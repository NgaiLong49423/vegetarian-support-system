import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, CheckCircle2, Eye, EyeOff, Mail } from 'lucide-react';
import { Logo } from '../components/Logo';
import { InteractiveLamp } from '../components/InteractiveLamp';
import { useAuth, type SessionNotice } from '../components/AuthContext';
import { GoogleSignIn } from '../components/GoogleSignIn';
import { fieldMessages, retryAfterSeconds, toProblem, type ProblemDetails } from '../lib/problem';
import { confirmPasswordReset, googleLogin, login, register, requestPasswordReset, resendVerificationEmail, verifyEmail, type AccountSummary } from '../services/authApi';
import { claimOnboardingInvitation } from '../services/dietaryPreferencesApi';
import { passwordProblems } from '../utils/password';

type Mode = 'login' | 'register' | 'forgot' | 'verify' | 'reset';
type Notice = { tone: 'success' | 'error'; text: string; link?: { to: string; label: string } };
type VerifyState = 'idle' | 'verifying' | 'verified' | 'failed';
type ResetState = 'ready' | 'unusable' | 'done';

const copy = {
  login: { title: 'Chào mừng bạn trở lại', subtitle: 'Tiếp tục hành trình ăn chay theo cách của bạn.', action: 'Đăng nhập' },
  register: { title: 'Bắt đầu cùng Mâm Xanh', subtitle: 'Tạo tài khoản để lưu món ngon và xây dựng thực đơn của riêng bạn.', action: 'Tạo tài khoản' },
  forgot: { title: 'Quên mật khẩu?', subtitle: 'Nhập email của bạn để yêu cầu hướng dẫn đặt lại mật khẩu.', action: 'Yêu cầu đặt lại mật khẩu' },
  verify: { title: 'Xác minh email của bạn', subtitle: 'Xác minh email là bước cần thiết trước khi đăng nhập bằng mật khẩu.', action: 'Yêu cầu gửi lại email' },
  reset: { title: 'Đặt mật khẩu mới', subtitle: 'Chọn mật khẩu mới cho tài khoản Mâm Xanh của bạn.', action: 'Lưu mật khẩu mới' },
};

// API field names follow the generated Backend runtime contract -> form field keys.
const apiFieldToForm: Record<string, string> = { displayName: 'name', email: 'email', password: 'password', newPassword: 'password', confirmPassword: 'confirm' };
const NETWORK_ERROR = 'Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại.';
const INVALID_LINK = 'Liên kết xác minh không hợp lệ hoặc đã hết hạn. Nhập email bên dưới để nhận liên kết mới.';
const INVALID_RESET_LINK: Notice = {
  tone: 'error',
  text: 'Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu liên kết mới.',
  link: { to: '/quen-mat-khau', label: 'Yêu cầu liên kết mới' },
};
const SESSION_NOTICES: Record<SessionNotice, string> = {
  timeout: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  expired: 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.',
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

const GOOGLE_RETRY = 'Không đăng nhập được với Google. Vui lòng thử lại.';

// UC-03.5: Google sign-in errors, also chosen by problem code.
function googleLoginError(problem: ProblemDetails | null): Notice {
  switch (problem?.code) {
    case 'GOOGLE_TOKEN_INVALID':
      return { tone: 'error', text: 'Không xác thực được tài khoản Google. Vui lòng thử đăng nhập Google lại.' };
    case 'ACCOUNT_LOCKED':
      return { tone: 'error', text: SESSION_NOTICES.locked };
    case 'GOOGLE_ACCOUNT_CONFLICT':
      return { tone: 'error', text: 'Email này đã được liên kết với một tài khoản Google khác. Hãy đăng nhập bằng tài khoản Google đó hoặc bằng email và mật khẩu.' };
    case 'GOOGLE_LOGIN_UNAVAILABLE':
      return { tone: 'error', text: 'Chưa thể đăng nhập bằng Google lúc này. Vui lòng thử lại sau hoặc đăng nhập bằng email.' };
    default:
      return { tone: 'error', text: problem?.detail ?? NETWORK_ERROR };
  }
}

/**
 * AC-31.10: the Backend shows the Onboarding questionnaire once, to a new Member who has not answered it;
 * everyone else lands on the home page. Failing to check the invitation never blocks the sign-in.
 */
async function landingAfterLogin(account: AccountSummary): Promise<string> {
  if (account.role === 'ADMIN') return '/trang-chu';
  try {
    return (await claimOnboardingInvitation()) ? '/khoi-tao-so-thich' : '/trang-chu';
  } catch {
    return '/trang-chu';
  }
}

function formErrors(problem: ProblemDetails): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const [field, message] of Object.entries(fieldMessages(problem))) errors[apiFieldToForm[field] ?? field] = message;
  return errors;
}

export function AuthPage({ mode }: { mode: Mode }) {
  const [isLampOn, setIsLampOn] = useState(true);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [visible, setVisible] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const location = useLocation();
  const sessionNotice = mode === 'login' ? (location.state as { sessionNotice?: SessionNotice } | null)?.sessionNotice : undefined;
  // UC-03.7: the reset token from the email link is kept in memory only; the effect below removes it from the URL.
  const [resetToken] = useState(() => mode === 'reset' ? new URLSearchParams(location.search).get('token') : null);
  const [notice, setNotice] = useState<Notice | null>(() => sessionNotice ? { tone: 'error', text: SESSION_NOTICES[sessionNotice] } : mode === 'reset' && !resetToken ? INVALID_RESET_LINK : null);
  const [submitting, setSubmitting] = useState(false);
  const [verifyState, setVerifyState] = useState<VerifyState>('idle');
  const [resetState, setResetState] = useState<ResetState>(resetToken ? 'ready' : 'unusable');
  const [searchParams, setSearchParams] = useSearchParams();
  const handledToken = useRef<string | null>(null);
  const { signIn } = useAuth();
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

  useEffect(() => {
    if (mode === 'reset' && searchParams.has('token')) setSearchParams({}, { replace: true });
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
      const session = await login({ email: email.trim(), password });
      signIn(session);
      navigate(await landingAfterLogin(session.account), { replace: true });
    } catch (error) {
      const problem = toProblem(error);
      if (problem?.code === 'VALIDATION_FAILED' && problem.errors?.length) setErrors(formErrors(problem));
      else setNotice(loginError(problem, retryAfterSeconds(error)));
    } finally {
      setPassword('');
      setSubmitting(false);
    }
  };

  const submitGoogle = async (idToken: string) => {
    setNotice(null);
    setSubmitting(true);
    try {
      const session = await googleLogin(idToken);
      signIn(session);
      navigate(await landingAfterLogin(session.account), { replace: true });
    } catch (error) {
      setNotice(googleLoginError(toProblem(error)));
    } finally {
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

  // UC-03.6: the API answers every valid email with the same neutral message.
  const submitForgot = async () => {
    setSubmitting(true);
    try {
      const result = await requestPasswordReset(email.trim());
      setNotice({ tone: 'success', text: result.message });
    } catch (error) {
      const problem = toProblem(error);
      if (problem?.code === 'VALIDATION_FAILED' && problem.errors?.length) setErrors(formErrors(problem));
      else setNotice({ tone: 'error', text: problem?.detail ?? NETWORK_ERROR });
    } finally {
      setSubmitting(false);
    }
  };

  const submitReset = async () => {
    if (!resetToken) return;
    setSubmitting(true);
    try {
      await confirmPasswordReset({ token: resetToken, newPassword: password, confirmPassword: confirm });
      setResetState('done');
    } catch (error) {
      const problem = toProblem(error);
      const badToken = problem?.code === 'PASSWORD_RESET_TOKEN_INVALID' || problem?.errors?.some(item => item.field === 'token');
      if (badToken || problem?.code === 'ACCOUNT_LOCKED') {
        setResetState('unusable');
        setNotice(badToken ? INVALID_RESET_LINK : { tone: 'error', text: SESSION_NOTICES.locked });
      } else if (problem?.code === 'NEW_PASSWORD_SAME_AS_CURRENT') setErrors({ password: 'Mật khẩu mới phải khác mật khẩu hiện tại.' });
      else if (problem?.code === 'VALIDATION_FAILED' && problem.errors?.length) setErrors(formErrors(problem));
      else setNotice({ tone: 'error', text: problem?.detail ?? NETWORK_ERROR });
    } finally {
      setPassword('');
      setConfirm('');
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
    if (mode === 'forgot') return void submitForgot();
    void submitReset();
  };
  const fieldClass =
    'mt-1 w-full rounded-xl border border-slate-700/80 bg-[#0c0e14]/80 px-3.5 py-2 sm:py-2.5 text-sm text-white placeholder:text-slate-500 outline-none transition focus:border-amber-400 focus:bg-[#0c0e14] focus:ring-2 focus:ring-amber-400/20';
  const error = (field: string) =>
    errors[field] && (
      <p id={`${field}-error`} className="mt-1 text-xs font-medium text-rose-400">
        {errors[field]}
      </p>
    );
  const showForm = !(mode === 'verify' && (verifyState === 'verifying' || verifyState === 'verified')) && !(mode === 'reset' && resetState !== 'ready');

  return (
    <div
      className={`relative h-screen h-dvh max-h-screen w-full overflow-hidden flex flex-col justify-between transition-colors duration-700 ease-in-out select-none ${
        isLampOn ? 'bg-[#10131a]' : 'bg-[#08090d]'
      }`}
      style={{ '--color-ink': '#f4f4f4' } as React.CSSProperties}
    >
      {/* Hiệu ứng ánh sáng vàng ấm lan tỏa khắp phòng khi đèn bật */}
      <div
        className={`pointer-events-none absolute inset-0 transition-opacity duration-700 ease-in-out ${
          isLampOn ? 'opacity-100' : 'opacity-0'
        }`}
        style={{
          background:
            'radial-gradient(ellipse 1100px 750px at 28% 46%, rgba(254, 240, 138, 0.18) 0%, rgba(245, 158, 11, 0.08) 36%, transparent 70%)',
        }}
      />
      <div
        className={`pointer-events-none absolute -right-24 top-1/2 -translate-y-1/2 h-[520px] w-[520px] rounded-full blur-[110px] transition-opacity duration-700 ${
          isLampOn ? 'bg-amber-500/[0.05]' : 'opacity-0'
        }`}
      />

      <header
        className={`relative z-10 flex h-14 w-full shrink-0 items-center justify-between px-5 sm:px-8 transition-opacity duration-500 ${
          isLampOn ? 'opacity-100' : 'opacity-30'
        }`}
      >
        <Logo />
        <Link
          to="/"
          aria-label="Về trang chủ"
          className="inline-flex items-center gap-2 rounded-xl border border-slate-700/60 bg-[#161922]/70 px-3.5 py-1.5 text-xs sm:text-sm font-semibold text-slate-200 backdrop-blur-md transition-all hover:border-amber-400/50 hover:bg-[#1f2330] hover:text-amber-300"
        >
          <ArrowLeft className="h-4 w-4" />
          <span className="hidden sm:inline">Về trang chủ</span>
        </Link>
      </header>

      <main className="relative z-10 flex flex-1 min-h-0 w-full items-start overflow-y-auto px-4 py-2 sm:px-6">
        <div className="mx-auto my-auto grid w-full max-w-5xl shrink-0 lg:grid-cols-2 lg:items-center lg:gap-10">
          {/* Cột cây đèn tương tác bên trái (desktop) */}
          <section className="relative hidden flex-col items-center justify-center scale-95 lg:flex xl:scale-100">
            <InteractiveLamp isOn={isLampOn} onToggle={() => setIsLampOn((prev) => !prev)} />
          </section>

          {/* Khung form đăng nhập / đăng ký / xác thực */}
          <div className="relative mx-auto w-full max-w-[420px] select-text">
            <section
              className={`relative min-w-0 rounded-2xl sm:rounded-3xl border p-5 sm:p-6 transition-all duration-700 ease-out ${
                isLampOn
                  ? 'border-amber-400/25 bg-[#141722]/85 text-white shadow-[0_20px_50px_rgba(0,0,0,0.6),0_0_35px_rgba(251,191,36,0.08),inset_0_1px_1px_rgba(255,255,255,0.1)] backdrop-blur-xl opacity-100 scale-100'
                  : 'border-white/5 bg-[#10121a]/70 opacity-15 brightness-50 blur-[1px] scale-[0.98] pointer-events-none select-none shadow-none'
              }`}
            >
              <div className="mb-2.5 flex items-center justify-between gap-2">
                <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-400/30 bg-amber-400/10 px-2.5 py-0.5 text-[11px] font-semibold uppercase tracking-wider text-amber-300">
                  <span className="h-1.5 w-1.5 rounded-full bg-amber-400 animate-pulse" />
                  {mode === 'register' ? 'Gia nhập cộng đồng' : 'Tài khoản Mâm Xanh'}
                </span>
              </div>

              <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-white">{details.title}</h1>
              <p className="mt-1 text-xs sm:text-sm leading-relaxed text-slate-300/90">{details.subtitle}</p>

              {(mode === 'login' || mode === 'register') && (
                <>
                  <GoogleSignIn
                    onCredential={(idToken) => void submitGoogle(idToken)}
                    onFailure={() => setNotice({ tone: 'error', text: GOOGLE_RETRY })}
                  />
                  <div className="my-2.5 sm:my-3 flex items-center gap-3 text-xs text-slate-400">
                    <span className="h-px flex-1 bg-slate-700/60" />
                    <span>hoặc sử dụng email</span>
                    <span className="h-px flex-1 bg-slate-700/60" />
                  </div>
                </>
              )}

              {mode === 'verify' && verifyState === 'verifying' && (
                <p role="status" className="mt-3 rounded-xl border border-amber-500/30 bg-amber-950/50 p-3 text-xs sm:text-sm leading-relaxed text-amber-200">
                  Đang xác minh email của bạn...
                </p>
              )}
              {mode === 'verify' && verifyState === 'verified' && (
                <div
                  role="status"
                  className="mt-3 rounded-xl border border-emerald-500/40 bg-emerald-950/60 p-3.5 text-xs sm:text-sm leading-relaxed text-emerald-200"
                >
                  <p className="flex items-center gap-2 font-semibold">
                    <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-400" />
                    Email của bạn đã được xác minh.
                  </p>
                  <p className="mt-1 text-slate-300">Bạn có thể đăng nhập bằng email và mật khẩu đã đăng ký.</p>
                  <Link
                    to="/dang-nhap"
                    className="mt-2.5 inline-flex items-center gap-1.5 font-bold text-amber-400 hover:text-amber-300 hover:underline"
                  >
                    Đến trang đăng nhập
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>
              )}
              {((mode === 'verify' && verifyState === 'idle') || (mode === 'reset' && resetState === 'ready')) && (
                <div className="mt-3 flex gap-2.5 rounded-xl border border-amber-500/30 bg-amber-950/40 p-3 text-xs sm:text-sm leading-relaxed text-amber-200">
                  <Mail className="mt-0.5 h-4 w-4 shrink-0 text-amber-400" />
                  <p>
                    {mode === 'verify'
                      ? 'Liên kết xác minh có hiệu lực 24 giờ. Bạn có thể yêu cầu gửi lại nếu liên kết hết hạn.'
                      : 'Liên kết đặt lại mật khẩu có hiệu lực 15 phút và chỉ dùng được một lần.'}
                  </p>
                </div>
              )}
              {mode === 'reset' && resetState === 'done' && (
                <div role="status" className="mt-3 rounded-xl border border-emerald-500/40 bg-emerald-950/60 p-3.5 text-xs sm:text-sm leading-relaxed text-emerald-200">
                  <p className="flex items-center gap-2 font-semibold text-emerald-400">
                    <CheckCircle2 className="h-5 w-5 shrink-0" />
                    Mật khẩu của bạn đã được cập nhật.
                  </p>
                  <p className="mt-1 text-slate-300">Bạn có thể đăng nhập bằng email và mật khẩu mới.</p>
                  <Link to="/dang-nhap" className="mt-2.5 inline-flex items-center gap-1.5 font-bold text-amber-400 hover:text-amber-300 hover:underline">
                    Đến trang đăng nhập
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>
              )}

              {showForm && (
                <form noValidate onSubmit={submit} className="mt-3 space-y-2.5 sm:space-y-3">
                  {mode === 'register' && (
                    <div>
                      <label htmlFor="auth-name" className="text-xs font-semibold text-slate-300">
                        Tên hiển thị
                      </label>
                      <input
                        id="auth-name"
                        autoComplete="nickname"
                        value={name}
                        maxLength={50}
                        onChange={(e) => setName(e.target.value)}
                        aria-invalid={!!errors.name}
                        aria-describedby={errors.name ? 'name-error' : undefined}
                        placeholder="Tên bạn muốn mọi người gọi"
                        className={fieldClass}
                      />
                      {error('name')}
                    </div>
                  )}
                  {mode !== 'reset' && (
                    <div>
                      <label htmlFor="auth-email" className="text-xs font-semibold text-slate-300">
                        Email
                      </label>
                      <input
                        id="auth-email"
                        type="email"
                        autoComplete="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        aria-invalid={!!errors.email}
                        aria-describedby={errors.email ? 'email-error' : undefined}
                        placeholder="ban@example.com"
                        className={fieldClass}
                      />
                      {error('email')}
                    </div>
                  )}
                  {needsPassword && (
                    <div>
                      <label htmlFor="auth-password" className="text-xs font-semibold text-slate-300">
                        {mode === 'reset' ? 'Mật khẩu mới' : 'Mật khẩu'}
                      </label>
                      <div className="relative">
                        <input
                          id="auth-password"
                          type={visible ? 'text' : 'password'}
                          autoComplete={newPassword ? 'new-password' : 'current-password'}
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          aria-invalid={!!errors.password}
                          aria-describedby={
                            errors.password
                              ? 'password-error'
                              : newPassword
                              ? 'password-hint'
                              : undefined
                          }
                          placeholder={newPassword ? 'Tối thiểu 8 ký tự' : 'Nhập mật khẩu của bạn'}
                          className={`${fieldClass} pr-10`}
                        />
                        <button
                          type="button"
                          aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                          aria-pressed={visible}
                          onClick={() => setVisible(!visible)}
                          className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-1.5 text-slate-400 hover:text-amber-300 hover:bg-white/5 transition-colors"
                        >
                          {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </button>
                      </div>
                      {error('password')}
                      {newPassword && (
                        <p id="password-hint" className="mt-1 text-[11px] leading-tight text-slate-400">
                          Dùng 8–64 ký tự, gồm ít nhất 1 chữ in hoa, 1 chữ thường và 1 chữ số, ký tự đặc biệt là tùy chọn.
                        </p>
                      )}
                    </div>
                  )}
                  {newPassword && (
                    <div>
                      <label htmlFor="auth-confirm" className="text-xs font-semibold text-slate-300">
                        Xác nhận mật khẩu
                      </label>
                      <input
                        id="auth-confirm"
                        type={visible ? 'text' : 'password'}
                        autoComplete="new-password"
                        value={confirm}
                        onChange={(e) => setConfirm(e.target.value)}
                        aria-invalid={!!errors.confirm}
                        aria-describedby={errors.confirm ? 'confirm-error' : undefined}
                        placeholder="Nhập lại mật khẩu"
                        className={fieldClass}
                      />
                      {error('confirm')}
                    </div>
                  )}
                  {mode === 'login' && (
                    <div className="text-right pt-0.5">
                      <Link
                        to="/quen-mat-khau"
                        className="text-xs font-semibold text-amber-400 hover:text-amber-300 hover:underline transition-colors"
                      >
                        Quên mật khẩu?
                      </Link>
                    </div>
                  )}
                  {Object.keys(errors).length > 0 && (
                    <p role="alert" className="text-xs font-medium text-rose-400">
                      Vui lòng kiểm tra các trường được đánh dấu.
                    </p>
                  )}
                  <button
                    type="submit"
                    disabled={submitting}
                    className="w-full mt-2 inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 via-amber-400 to-amber-500 py-2.5 text-sm sm:text-base font-bold text-slate-950 shadow-[0_4px_16px_rgba(245,158,11,0.35)] transition-all hover:brightness-105 active:scale-[0.99] disabled:opacity-50 disabled:pointer-events-none"
                  >
                    {submitting ? 'Đang gửi...' : details.action}
                    <ArrowRight className="h-4 w-4" />
                  </button>
                </form>
              )}

              {notice && (
                <div
                  role={notice.tone === 'error' ? 'alert' : 'status'}
                  className={`mt-3 rounded-xl border p-3 text-xs sm:text-sm leading-relaxed ${
                    notice.tone === 'error'
                      ? 'border-red-500/40 bg-red-950/60 text-red-200'
                      : 'border-emerald-500/40 bg-emerald-950/60 text-emerald-200'
                  }`}
                >
                  <p>{notice.text}</p>
                  {mode === 'register' && notice.tone === 'success' && (
                    <Link to="/xac-minh-email" className="mt-1.5 block font-semibold text-amber-300 underline">
                      Chưa nhận được email? Gửi lại email xác minh
                    </Link>
                  )}
                  {notice.link && (
                    <Link to={notice.link.to} className="mt-1.5 block font-semibold text-amber-300 underline">
                      {notice.link.label}
                    </Link>
                  )}
                </div>
              )}

              <div className="mt-3 border-t border-slate-700/60 pt-3 text-center text-xs sm:text-sm text-slate-300">
                {mode === 'login' ? (
                  <>
                    Chưa có tài khoản?{' '}
                    <Link to="/dang-ky" className="font-bold text-amber-400 hover:text-amber-300 hover:underline">
                      Đăng ký ngay
                    </Link>
                    <Link
                      to="/xac-minh-email"
                      className="mt-1.5 block text-xs text-slate-400 hover:text-slate-200 hover:underline"
                    >
                      Chưa nhận được email xác minh?
                    </Link>
                  </>
                ) : mode === 'register' ? (
                  <>
                    Đã có tài khoản?{' '}
                    <Link to="/dang-nhap" className="font-bold text-amber-400 hover:text-amber-300 hover:underline">
                      Đăng nhập
                    </Link>
                  </>
                ) : (
                  <Link to="/dang-nhap" className="font-semibold text-amber-400 hover:text-amber-300 hover:underline">
                    Quay lại đăng nhập
                  </Link>
                )}
              </div>
            </section>

            {/* Lớp phủ khi đèn tắt: nhấp vào để bật đèn lại */}
            {!isLampOn && (
              <button
                type="button"
                onClick={() => setIsLampOn(true)}
                aria-label="Bật đèn để đăng nhập"
                className="absolute inset-0 z-20 cursor-pointer rounded-2xl sm:rounded-3xl bg-black/50 backdrop-blur-[2px] transition-all hover:bg-black/40 flex items-center justify-center text-amber-200/90 text-sm font-medium p-4 text-center"
              >
                <span className="rounded-full bg-black/70 border border-amber-400/30 px-4 py-2 shadow-xl">
                  Nhấp vào đây hoặc kéo dây để bật đèn ✨
                </span>
              </button>
            )}
          </div>
        </div>
      </main>

      <footer className="relative z-10 flex h-7 shrink-0 items-center justify-center text-[11px] text-slate-500/70 select-none">
        <span>© 2026 Mâm Xanh · Ẩm thực thuần lành</span>
      </footer>
    </div>
  );
}
