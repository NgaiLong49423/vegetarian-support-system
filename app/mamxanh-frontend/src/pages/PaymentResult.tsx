import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CheckCircle2, XCircle, RefreshCw, CreditCard, Clock3, AlertCircle } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button, Card } from '../components/ui';
import { subscriptionApi, type PaymentResultResponse } from '../api/subscription';
import { useAuth } from '../components/AuthContext';
import { formatDate } from '../utils/date';

const MAX_CHECKS = 10;
const CHECK_INTERVAL_MS = 3000;

type ResultState = 'checking' | 'pending' | 'unconfirmed' | 'success' | 'paid' | 'cancelled' | 'error';

export function PaymentResult({ mode }: { mode: 'success' | 'cancel' }) {
  const [params] = useSearchParams();
  const { account } = useAuth();
  const orderCode = params.get('orderCode') ?? '';
  return <PaymentResultDetails key={`${account?.id ?? 'guest'}:${orderCode}:${mode}`} orderCode={orderCode} signedIn={!!account} />;
}

function PaymentResultDetails({ orderCode, signedIn }: { orderCode: string; signedIn: boolean }) {
  const validOrder = /^[1-9][0-9]{0,15}$/.test(orderCode) && Number(orderCode) <= Number.MAX_SAFE_INTEGER;
  const [result, setResult] = useState<PaymentResultResponse | null>(null);
  const [state, setState] = useState<ResultState>('checking');
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    if (!signedIn || !validOrder) return;
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout> | undefined;
    let attempts = 0;
    setState('checking');
    setError('');
    async function check() {
      attempts++;
      try {
        const response = await subscriptionApi.getPaymentResult(orderCode, controller.signal);
        if (controller.signal.aborted) return;
        if (response.orderCode !== orderCode) throw new Error('Không xác minh được đúng đơn thanh toán.');
        setResult(response);
        if (response.status === 'PAID') {
          setState(response.activated ? 'success' : 'paid');
        } else if (response.status === 'CANCELLED' || response.status === 'FAILED') {
          setState('cancelled');
        } else if (attempts >= MAX_CHECKS) {
          setState('unconfirmed');
        } else {
          setState('pending');
          timer = setTimeout(() => void check(), CHECK_INTERVAL_MS);
        }
      } catch (err) {
        if (controller.signal.aborted) return;
        setError(err instanceof Error ? err.message : 'Chưa thể kiểm tra kết quả thanh toán.');
        setState('error');
      }
    }
    void check();
    return () => { controller.abort(); if (timer) clearTimeout(timer); };
  }, [orderCode, signedIn, validOrder, retry]);

  const success = state === 'success';
  const busy = state === 'checking' || state === 'pending';
  const title = !signedIn ? 'Đăng nhập để kiểm tra thanh toán'
    : !validOrder ? 'Thiếu hoặc sai mã đơn thanh toán'
    : success ? 'Thanh toán thành công!'
    : state === 'paid' ? 'Giao dịch đã được thanh toán'
    : state === 'cancelled' ? 'Giao dịch chưa hoàn tất'
    : state === 'error' ? 'Chưa thể xác minh thanh toán'
    : state === 'unconfirmed' ? 'Chưa nhận được xác nhận thanh toán'
    : 'Đang xác minh thanh toán';
  const subscription = result?.subscription;

  return <PageContainer className="py-12">
    <Card className="mx-auto max-w-lg p-6 text-center sm:p-8">
      <div className={`mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full ${success ? 'bg-leaf-100 text-leaf-600' : 'bg-raised text-brand-600'}`} aria-hidden="true">
        {success ? <CheckCircle2 className="h-10 w-10" /> : state === 'cancelled' ? <XCircle className="h-10 w-10" /> : state === 'error' ? <AlertCircle className="h-10 w-10" /> : <Clock3 className="h-10 w-10" />}
      </div>
      <div role="status" aria-live="polite" aria-busy={signedIn && validOrder && busy}>
        <h1 className="text-2xl font-extrabold text-ink">{title}</h1>
        <p className="mt-3 text-sm leading-6 text-ink-muted">
          {!signedIn ? 'Vui lòng đăng nhập bằng tài khoản đã tạo đơn để xem kết quả.'
            : !validOrder ? 'Mở lịch sử giao dịch để kiểm tra đơn của bạn. Đường dẫn này chưa xác nhận thanh toán.'
            : success ? `Gói ${result?.tier} của đơn này đã được kích hoạt sau khi xác minh thanh toán.`
            : state === 'paid' ? 'Thanh toán của đơn này đã được xác nhận. Gói của đơn hiện không còn hiệu lực; xem gói đang sử dụng bên dưới.'
            : state === 'cancelled' ? 'Đơn đã bị hủy, hết hạn hoặc thất bại theo kết quả máy chủ. Quyền lợi tài khoản của bạn được bảo toàn.'
            : state === 'error' ? error
            : state === 'unconfirmed' ? 'Chưa có xác nhận từ cổng thanh toán. Bạn có thể bấm Kiểm tra lại sau ít phút. Nếu đã chuyển tiền, vui lòng không thanh toán lại khi chưa xác minh kết quả.'
            : 'Hệ thống đang kiểm tra kết quả từ cổng thanh toán. Quyền lợi hiện tại được giữ nguyên. Nếu đã chuyển tiền, vui lòng không thanh toán lại khi chưa xác minh kết quả.'}
        </p>
      </div>
      {signedIn && validOrder && <p className="mt-3 break-all text-xs text-ink-soft">Mã đơn hàng payOS: <strong>#{orderCode}</strong></p>}
      {subscription && <div className="my-6 rounded-xl border border-brand-100 bg-raised p-4 text-left">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className="text-sm font-semibold text-ink">Gói hiện tại:</span>
          <Badge tone={subscription.tier === 'FREE' ? 'neutral' : 'brand'}>Gói {subscription.tier}</Badge>
        </div>
        {subscription.endsAt && <p className="mt-3 text-xs text-ink-soft">Hạn sử dụng đến: <strong>{formatDate(subscription.endsAt)}</strong></p>}
        <ul className="mt-3 space-y-2 text-xs text-ink-soft">{subscription.unlockedFeatures.map(feature => <li key={feature}>✓ {feature}</li>)}</ul>
      </div>}
      <div className="mt-6 flex flex-col gap-3">
        {!signedIn ? <Link to="/dang-nhap" state={{ returnTo: `/payment/success?orderCode=${encodeURIComponent(orderCode)}` }}><Button className="w-full">Đăng nhập</Button></Link>
          : validOrder && (state === 'error' || state === 'unconfirmed') && <Button variant="outline" onClick={() => setRetry(value => value + 1)}><RefreshCw className="mr-2 h-4 w-4" />Kiểm tra lại</Button>}
        {signedIn && <Link to="/giao-dich"><Button variant="outline" className="w-full"><CreditCard className="mr-2 h-4 w-4" />Lịch sử giao dịch</Button></Link>}
        {signedIn && <Link to="/ho-so" className="text-sm font-semibold text-brand-600">Xem hồ sơ</Link>}
        <Link to={success ? '/goi-ai' : '/ho-so'} className="text-sm font-semibold text-brand-600">{success ? 'Xem gói và quyền lợi' : 'Về hồ sơ'}</Link>
      </div>
    </Card>
  </PageContainer>;
}
