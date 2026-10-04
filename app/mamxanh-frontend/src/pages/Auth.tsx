import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, CheckCircle2, Eye, EyeOff, Leaf, Mail, ShieldCheck, Utensils } from 'lucide-react';
import { Logo } from '../components/Logo';
import { Button } from '../components/ui';
import { useAuth, type SessionNotice } from '../components/AuthContext';
import { useDemoAccount } from '../components/DemoAccount';
import { fieldMessages, retryAfterSeconds, toProblem, type ProblemDetails } from '../lib/problem';
import { login, register, resendVerificationEmail, verifyEmail } from '../services/authApi';
import { passwordProblems } from '../utils/password';

type Mode = 'login' | 'register' | 'forgot' | 'verify' | 'reset';
type Notice = { tone: 'success' | 'error'; text: string; link?: { to: string; label: string } };
type VerifyState = 'idle' | 'verifying' | 'verified' | 'failed';

const copy = {
  login: { title: 'Chào mừng bạn trở lại', subtitle: 'Tiếp tục hành trình ăn chay theo cách của bạn.', action: 'Đăng nhập' },
  register: { title: 'Bắt đầu cùng Mâm Xanh', subtitle: 'Tạo tài khoản để lưu món ngon và xây dựng thực đơn của riêng bạn.', action: 'Tạo tài khoản' },
  forgot: { title: 'Quên mật khẩu?', subtitle: 'Nhập email của bạn để yêu cầu hướng dẫn đặt lại mật khẩu.', action: 'Yêu cầu đặt lại mật khẩu' },
  verify: { title: 'Xác minh email của bạn', subtitle: 'Xác minh email là bước cần thiết trước khi đăng nhập bằng mật khẩu.', action: 'Yêu cầu gửi lại email' },
  reset: { title: 'Đặt mật khẩu mới', subtitle: 'Chọn mật khẩu mới cho tài khoản Mâm Xanh của bạn.', action: 'Lưu mật khẩu mới' },
};

// API field names follow the generated Backend runtime contract -> form field keys.
const apiFieldToForm: Record<string, string> = { displayName: 'name', email: 'email', password: 'password', confirmPassword: 'confirm' };
const NETWORK_ERROR = 'Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại.';
const INVALID_LINK = 'Liên kết xác minh không hợp lệ hoặc đã hết hạn. Nhập email bên dưới để nhận liên kết mới.';
const SESSION_NOTICES: Record<SessionNotice, string> = {
  timeout: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  expired: 'Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại.',
  locked: 'Tài khoản đã bị quản trị viên khóa. Vui lòng liên hệ quản trị viên.',
};

// UC-03.4: messages follow the problem code (API.md section 4), never the detail text.
function loginError(problem: ProblemDetails | null, retryAfter: number | null): Notice {
  switch (problem?.code) {
    case 'INVALID_CREDENTIALS':
      return { tone: 'error', text: 'Email hoặc mật khẩu không chính xác.' };
    case 'EMAIL_NOT_VERIFIED':
      return { tone: 'error', text: 'Tài khoản chưa xác minh email. Vui lòng mở liên kết xác minh trong hộp thư trước khi đăng nhập.', link: { to: '/xac-minh-email', label: 'Gửi lại email xác minh' } };
    case 'ACCOUNT_LOCKED':
      return { tone: 'error', text: SESSION_NOTICES.locked };
    case 'LOGIN_TEMPORARILY_BLOCKED':
      return { tone: 'error', text: retryAfter ? `Bạn đã nhập sai mật khẩu nhiều lần liên tiếp. Vui lòng thử lại sau ${Math.ceil(retryAfter / 60)} phút.` : problem.detail ?? NETWORK_ERROR };
    default:
      return { tone: 'error', text: problem?.detail ?? NETWORK_ERROR };
  }
}

function formErrors(problem: ProblemDetails): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const [field, message] of Object.entries(fieldMessages(problem))) errors[apiFieldToForm[field] ?? field] = message;
  return errors;
}

export function AuthPage({ mode }: { mode: Mode }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [visible, setVisible] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const location = useLocation();
  const sessionNotice = mode === 'login' ? (location.state as { sessionNotice?: SessionNotice } | null)?.sessionNotice : undefined;
  const [notice, setNotice] = useState<Notice | null>(() => sessionNotice ? { tone: 'error', text: SESSION_NOTICES[sessionNotice] } : null);
  const [submitting, setSubmitting] = useState(false);
  const [verifyState, setVerifyState] = useState<VerifyState>('idle');
  const [searchParams, setSearchParams] = useSearchParams();
  const handledToken = useRef<string | null>(null);
  const { signIn, enterDemo } = useAuth();
  const { setActive } = useDemoAccount();
  const navigate = useNavigate();
  const details = copy[mode];
  const newPassword = mode === 'register' || mode === 'reset';
  const needsPassword = mode === 'login' || newPassword;

  // UC-03.2: the email link opens /xac-minh-email?token=...; submit it once, then drop it from the URL.
  useEffect(() => {
    if (mode !== 'verify') return;
    const token = searchParams.get('token');
    if (!token || handledToken.current === token) return;
    handledToken.current = token;
    setSearchParams({}, { replace: true });
    setVerifyState('verifying');
    setNotice(null);
    verifyEmail(token)
      .then(() => setVerifyState('verified'))
      .catch((error: unknown) => {
        const problem = toProblem(error);
        setVerifyState('failed');
        setNotice({ tone: 'error', text: problem?.code === 'VERIFICATION_TOKEN_INVALID' ? INVALID_LINK : problem?.detail ?? NETWORK_ERROR });
      });
  }, [mode, searchParams, setSearchParams]);

  const submitRegistration = async () => {
    setSubmitting(true);
    try {
      const result = await register({ displayName: name.trim(), email: email.trim(), password, confirmPassword: confirm });
      setPassword('');
      setConfirm('');
      setNotice({ tone: 'success', text: result.message });
    } catch (error) {
      const problem = toProblem(error);
      if (problem?.code === 'EMAIL_ALREADY_USED') setErrors({ email: 'Email này đã được sử dụng. Vui lòng đăng nhập hoặc dùng email khác.' });
      else if (problem?.code === 'VALIDATION_FAILED' && problem.errors?.length) setErrors(formErrors(problem));
      else setNotice({ tone: 'error', text: problem?.detail ?? NETWORK_ERROR });
    } finally {
      setSubmitting(false);
    }
  };

  const submitLogin = async () => {
    setSubmitting(true);
    try {
      signIn(await login({ email: email.trim(), password }));
      setActive(false);
      navigate('/', { replace: true });
    } catch (error) {
      const problem = toProblem(error);
      if (problem?.code === 'VALIDATION_FAILED' && problem.errors?.length) setErrors(formErrors(problem));
      else setNotice(loginError(problem, retryAfterSeconds(error)));
    } finally {
      setPassword('');
      setSubmitting(false);
    }
  };

  const submitResend = async () => {
    setSubmitting(true);
    try {
      const result = await resendVerificationEmail(email.trim());
      setNotice({ tone: 'success', text: result.message });
    } catch (error) {
      const problem = toProblem(error);
      const seconds = retryAfterSeconds(error);
      if (problem?.code === 'RESEND_TOO_SOON') setNotice({ tone: 'error', text: seconds ? `Bạn vừa yêu cầu gửi email xác minh. Vui lòng thử lại sau ${seconds} giây.` : problem.detail ?? NETWORK_ERROR });
      else if (problem?.code === 'VALIDATION_FAILED' && problem.errors?.length) setErrors(formErrors(problem));
      else setNotice({ tone: 'error', text: problem?.detail ?? NETWORK_ERROR });
    } finally {
      setSubmitting(false);
    }
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const next: Record<string, string> = {};
    if (mode === 'register' && (name.trim().length < 3 || name.trim().length > 50)) next.name = 'Tên hiển thị cần từ 3 đến 50 ký tự.';
    if (mode !== 'reset' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) next.email = 'Vui lòng nhập địa chỉ email hợp lệ.';
    if (needsPassword && !password) next.password = 'Vui lòng nhập mật khẩu.';
    else if (newPassword) {
      const problems = passwordProblems(password);
      if (problems.length) next.password = problems.join(' ');
    }
    if (newPassword && password !== confirm) next.confirm = 'Mật khẩu xác nhận chưa khớp.';
    setErrors(next);
    setNotice(null);
    if (Object.keys(next).length) return;
    if (mode === 'register') return void submitRegistration();
    if (mode === 'verify') return void submitResend();
    if (mode === 'login') return void submitLogin();
    // Forgot/reset password (#9) are not connected to the backend yet.
    setPassword('');
    setConfirm('');
    setNotice({ tone: 'success', text: mode === 'reset'
        ? 'Đã kiểm tra biểu mẫu. Chưa xác minh liên kết hoặc thay đổi mật khẩu tài khoản.'
        : 'Đã kiểm tra định dạng email. Bản demo chưa gửi email và không kiểm tra địa chỉ này có tài khoản hay chưa.' });
  };
  const fieldClass = 'mt-2 w-full rounded-xl border border-brand-200 bg-white px-4 py-3 text-sm text-ink outline-none transition focus:border-leaf-600 focus:ring-2 focus:ring-leaf-100';
  const error = (field: string) => errors[field] && <p id={`${field}-error`} className="mt-1.5 text-xs text-red-700">{errors[field]}</p>;
  const showForm = !(mode === 'verify' && (verifyState === 'verifying' || verifyState === 'verified'));
  const demoNote = mode === 'register' || mode === 'login'
    ? `${mode === 'login' ? 'Đăng nhập' : 'Đăng ký và xác minh email'} được gửi tới máy chủ · Đăng nhập Google chưa được kết nối.`
    : mode === 'verify' ? '' : 'Bản demo giao diện · Chưa kết nối xác thực, gửi email hoặc lưu thông tin tài khoản.';

  return <div className="min-h-screen bg-cream">
    <header className="mx-auto flex max-w-7xl items-center justify-between gap-3 px-5 py-6 sm:px-8">
      <Logo />
      <Link to="/" aria-label="Về trang chủ" className="inline-flex items-center gap-2 text-sm font-semibold text-ink-soft hover:text-leaf-700"><ArrowLeft className="h-4 w-4" /><span className="hidden sm:inline">Về trang chủ</span></Link>
    </header>
    <main className="mx-auto grid max-w-6xl gap-8 px-5 pb-12 pt-3 lg:grid-cols-[1fr_1.05fr] lg:items-center lg:gap-16 lg:py-10">
      <section className="relative hidden overflow-hidden rounded-[2rem] bg-leaf-700 px-9 py-12 text-white lg:block">
        <div className="absolute -right-16 -top-16 h-64 w-64 rounded-full border-[40px] border-white/5" />
        <span className="relative inline-flex items-center gap-2 rounded-full border border-white/25 px-3 py-1.5 text-xs font-semibold"><Leaf className="h-4 w-4" /> ĂN CHAY THEO CÁCH CỦA BẠN</span>
        <h2 className="relative mt-8 text-4xl font-extrabold leading-tight">Một bữa ăn xanh.<br /><span className="text-brand-200">Một ngày an lành.</span></h2>
        <p className="mt-5 max-w-sm text-sm leading-7 text-white/80">Từ công thức yêu thích đến thực đơn mỗi ngày, Mâm Xanh đồng hành cùng bạn trên hành trình ăn chay.</p>
        <div className="mt-10 space-y-5">
          {[{ Icon: Utensils, title: 'Món ngon luôn ở gần', text: 'Khám phá và lưu công thức chay bạn yêu thích.' }, { Icon: Leaf, title: 'Bữa ăn có kế hoạch', text: 'Sắp xếp thực đơn và chuẩn bị nguyên liệu dễ dàng.' }, { Icon: ShieldCheck, title: 'Hiểu thêm về bản thân', text: 'Theo dõi hồ sơ dinh dưỡng và chỉ số tham khảo.' }].map(({ Icon, title, text }) => <div key={title} className="flex gap-3"><span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white/10"><Icon className="h-5 w-5" /></span><div><h3 className="text-sm font-bold">{title}</h3><p className="mt-1 text-xs leading-5 text-white/75">{text}</p></div></div>)}
        </div>
        <p className="mt-12 border-t border-white/15 pt-5 text-xs text-white/65">Mâm Xanh · Ẩm thực thuần lành</p>
      </section>
      <section className="min-w-0 rounded-3xl border border-brand-100 bg-white p-6 shadow-xl shadow-brand-900/5 sm:p-9">
        <span className="text-xs font-bold uppercase tracking-widest text-leaf-700">{mode === 'register' ? 'Gia nhập cộng đồng' : 'Tài khoản Mâm Xanh'}</span>
        <h1 className="mt-3 text-3xl font-extrabold tracking-tight text-ink">{details.title}</h1>
        <p className="mt-3 text-sm leading-6 text-ink-muted">{details.subtitle}</p>
        {(mode === 'login' || mode === 'register') && <>
          <button type="button" aria-describedby="auth-demo-note" onClick={() => setNotice({ tone: 'success', text: 'Google Login chưa được kết nối trong bản demo. Chưa có tài khoản hoặc phiên đăng nhập được tạo.' })} className="mt-6 flex w-full items-center justify-center gap-3 rounded-xl border border-brand-200 py-3 text-sm font-semibold text-ink hover:bg-brand-50"><span aria-hidden="true" className="text-lg font-bold text-blue-600">G</span>Tiếp tục với Google</button>
          <div className="my-5 flex items-center gap-3 text-xs text-ink-muted"><span className="h-px flex-1 bg-brand-100" />hoặc sử dụng email<span className="h-px flex-1 bg-brand-100" /></div>
        </>}
        {mode === 'verify' && verifyState === 'verifying' && <p role="status" className="mt-5 rounded-xl bg-leaf-50 p-4 text-sm leading-6 text-leaf-800">Đang xác minh email của bạn...</p>}
        {mode === 'verify' && verifyState === 'verified' && <div role="status" className="mt-5 rounded-xl border border-leaf-100 bg-leaf-50 p-4 text-sm leading-6 text-leaf-800">
          <p className="flex items-center gap-2 font-semibold"><CheckCircle2 className="h-5 w-5 shrink-0" />Email của bạn đã được xác minh.</p>
          <p className="mt-1">Bạn có thể đăng nhập bằng email và mật khẩu đã đăng ký.</p>
          <Link to="/dang-nhap" className="mt-3 inline-flex items-center gap-2 font-semibold text-brand-700 hover:underline">Đến trang đăng nhập<ArrowRight className="h-4 w-4" /></Link>
        </div>}
        {((mode === 'verify' && verifyState === 'idle') || mode === 'reset') && <div className="mt-5 flex gap-3 rounded-xl bg-leaf-50 p-4 text-sm leading-6 text-leaf-800"><Mail className="mt-1 h-5 w-5 shrink-0" /><p>{mode === 'verify' ? 'Liên kết xác minh có hiệu lực 24 giờ. Bạn có thể yêu cầu gửi lại nếu liên kết hết hạn.' : 'Cần liên kết đặt lại mật khẩu hợp lệ từ email (hiệu lực 15 phút). Bản demo chỉ cho xem biểu mẫu, chưa xác minh liên kết.'}</p></div>}
        {showForm && <form noValidate onSubmit={submit} className="mt-5 space-y-4">
          {mode === 'register' && <div><label htmlFor="auth-name" className="text-sm font-semibold text-ink">Tên hiển thị</label><input id="auth-name" autoComplete="nickname" value={name} maxLength={50} onChange={e => setName(e.target.value)} aria-invalid={!!errors.name} aria-describedby={errors.name ? 'name-error' : undefined} placeholder="Tên bạn muốn mọi người gọi" className={fieldClass} />{error('name')}</div>}
          {mode !== 'reset' && <div><label htmlFor="auth-email" className="text-sm font-semibold text-ink">Email</label><input id="auth-email" type="email" autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} aria-invalid={!!errors.email} aria-describedby={errors.email ? 'email-error' : undefined} placeholder="ban@example.com" className={fieldClass} />{error('email')}</div>}
          {needsPassword && <div><label htmlFor="auth-password" className="text-sm font-semibold text-ink">{mode === 'reset' ? 'Mật khẩu mới' : 'Mật khẩu'}</label><div className="relative"><input id="auth-password" type={visible ? 'text' : 'password'} autoComplete={newPassword ? 'new-password' : 'current-password'} value={password} onChange={e => setPassword(e.target.value)} aria-invalid={!!errors.password} aria-describedby={errors.password ? 'password-error' : newPassword ? 'password-hint' : undefined} placeholder={newPassword ? 'Tối thiểu 8 ký tự' : 'Nhập mật khẩu của bạn'} className={`${fieldClass} pr-12`} /><button type="button" aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} aria-pressed={visible} onClick={() => setVisible(!visible)} className="absolute right-2 top-4 rounded-lg p-2 text-ink-muted hover:bg-brand-50">{visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}</button></div>{error('password')}{newPassword && <p id="password-hint" className="mt-2 text-xs text-ink-muted">Dùng 8–64 ký tự, gồm ít nhất 1 chữ in hoa, 1 chữ thường và 1 chữ số; ký tự đặc biệt là tùy chọn.</p>}</div>}
          {newPassword && <div><label htmlFor="auth-confirm" className="text-sm font-semibold text-ink">Xác nhận mật khẩu</label><input id="auth-confirm" type={visible ? 'text' : 'password'} autoComplete="new-password" value={confirm} onChange={e => setConfirm(e.target.value)} aria-invalid={!!errors.confirm} aria-describedby={errors.confirm ? 'confirm-error' : undefined} placeholder="Nhập lại mật khẩu" className={fieldClass} />{error('confirm')}</div>}
          {mode === 'login' && <div className="text-right"><Link to="/quen-mat-khau" className="text-sm font-semibold text-brand-700 hover:underline">Quên mật khẩu?</Link></div>}
          {Object.keys(errors).length > 0 && <p role="alert" className="text-sm text-red-700">Vui lòng kiểm tra các trường được đánh dấu.</p>}
          <Button type="submit" disabled={submitting} className="w-full py-3">{submitting ? 'Đang gửi...' : details.action}<ArrowRight className="h-4 w-4" /></Button>
        </form>}
        {notice && <div role={notice.tone === 'error' ? 'alert' : 'status'} className={`mt-4 rounded-xl border p-4 text-sm leading-6 ${notice.tone === 'error' ? 'border-red-200 bg-red-50 text-red-800' : 'border-leaf-100 bg-leaf-50 text-leaf-800'}`}>{notice.text}{mode === 'register' && notice.tone === 'success' && <Link to="/xac-minh-email" className="mt-2 block font-semibold underline">Chưa nhận được email? Gửi lại email xác minh</Link>}{notice.link && <Link to={notice.link.to} className="mt-2 block font-semibold underline">{notice.link.label}</Link>}</div>}
        {demoNote && <p id="auth-demo-note" className="mt-5 text-xs leading-5 text-ink-muted">{demoNote}</p>}
        <div className="mt-6 border-t border-brand-100 pt-5 text-center text-sm text-ink-soft">
          {mode === 'login' ? <>Chưa có tài khoản? <Link to="/dang-ky" className="font-bold text-brand-700 hover:underline">Đăng ký ngay</Link><Link to="/xac-minh-email" className="mt-3 block text-xs text-ink-muted hover:underline">Chưa nhận được email xác minh?</Link></> : mode === 'register' ? <>Đã có tài khoản? <Link to="/dang-nhap" className="font-bold text-brand-700 hover:underline">Đăng nhập</Link></> : <Link to="/dang-nhap" className="font-semibold text-brand-700 hover:underline">Quay lại đăng nhập</Link>}
        </div>
        {mode === 'login' && <button type="button" onClick={() => { enterDemo(); setActive(true); navigate('/'); }} className="mt-5 w-full rounded-xl bg-brand-50 px-4 py-3 text-xs font-semibold text-ink-soft hover:bg-brand-100">Khám phá tài khoản demo</button>}
      </section>
    </main>
  </div>;
}
