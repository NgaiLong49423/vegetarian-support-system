import { useCallback, useEffect, useState } from 'react';
import { ArrowLeft, ArrowRight, AlertCircle, CheckCircle2, Loader2, RefreshCw, Wallet } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../components/AuthContext';
import { PageContainer } from '../components/Layout';
import { Button, Card } from '../components/ui';
import { subscriptionApi, type MyAiCreditResponse } from '../api/subscription';

export function AiCreditTopUp() {
  const { account, isAuthenticated } = useAuth();
  const canTopUp = isAuthenticated && (account?.role === 'CUSTOMER' || account?.role === 'EXPERT');
  const [credit, setCredit] = useState<MyAiCreditResponse | null>(null);
  const [amount, setAmount] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const loadCredit = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const currentCredit = await subscriptionApi.getMyAiCredit();
      setCredit(currentCredit);
      setAmount(current => current || String(currentCredit.minimumTopUpVnd));
    } catch (cause) {
      setLoadError(cause instanceof Error ? cause.message : 'Chưa tải được số dư AI Credit.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (canTopUp) void loadCredit();
  }, [canTopUp, loadCredit]);

  const parsedAmount = Number(amount);
  const isValidAmount = Boolean(credit && Number.isSafeInteger(parsedAmount)
    && parsedAmount >= credit.minimumTopUpVnd && parsedAmount <= credit.maximumTopUpVnd);
  const creditPreview = isValidAmount && credit
    ? parsedAmount * credit.creditsPer1000Vnd / 1000 : 0;

  async function handleTopUp() {
    if (!credit || !isValidAmount) return;
    setSubmitError(null);
    setSubmitting(true);
    try {
      const response = await subscriptionApi.createCreditCheckout(parsedAmount);
      let validCheckout = false;
      try {
        const url = new URL(response.checkoutUrl);
        validCheckout = url.protocol === 'https:' && url.hostname === 'pay.payos.vn'
          && !url.username && !url.password && (!url.port || url.port === '443');
      } catch { /* Invalid links must never send a member to a fake success page. */ }

      if (validCheckout) {
        window.location.href = response.checkoutUrl;
      } else {
        setSubmitError('Không thể khởi tạo liên kết nạp AI Credit. Vui lòng thử lại sau.');
        setSubmitting(false);
      }
    } catch (cause) {
      setSubmitError(cause instanceof Error ? cause.message : 'Không thể tạo giao dịch nạp AI Credit.');
      setSubmitting(false);
    }
  }

  return (
    <PageContainer className="py-8">
      <nav aria-label="Điều hướng trang" className="mb-5 text-sm text-ink-muted">
        <Link to="/goi-ai" className="inline-flex items-center gap-1 hover:text-brand-700">
          <ArrowLeft className="h-4 w-4" /> Gói AI
        </Link>
        <span aria-hidden="true"> / </span>
        <span aria-current="page" className="text-ink">Nạp AI Credit</span>
      </nav>

      <header className="mb-8 text-center">
        <span className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-brand-700">
          <Wallet className="h-6 w-6" />
        </span>
        <h1 className="text-3xl font-extrabold text-ink">Nạp AI Credit</h1>
        <p className="mx-auto mt-2 max-w-2xl text-sm text-ink-muted">
          Chọn số tiền cần nạp và kiểm tra lượng Credit nhận được trước khi thanh toán qua payOS.
        </p>
      </header>

      {!isAuthenticated ? (
        <Card className="mx-auto max-w-2xl p-6 text-center">
          <p className="text-ink">Đăng nhập để xem số dư và tạo giao dịch nạp AI Credit.</p>
          <Link to="/dang-nhap" state={{ returnTo: '/nap-ai-credit' }} className="mt-4 inline-block">
            <Button>Đăng nhập để tiếp tục<ArrowRight className="ml-2 h-4 w-4" /></Button>
          </Link>
        </Card>
      ) : !canTopUp ? (
        <Card className="mx-auto max-w-2xl p-6 text-center">
          <p role="alert" className="text-sm text-ink-muted">Tính năng nạp AI Credit dành cho tài khoản khách hàng và chuyên gia.</p>
          <Link to="/goi-ai" className="mt-4 inline-block"><Button variant="outline">Quay lại gói AI</Button></Link>
        </Card>
      ) : loading ? (
        <div role="status" className="flex justify-center py-16 text-brand-700">
          <Loader2 className="h-8 w-8 animate-spin" /><span className="sr-only">Đang tải số dư AI Credit</span>
        </div>
      ) : loadError ? (
        <Card className="mx-auto max-w-2xl p-6 text-center">
          <p role="alert" className="text-sm text-red-700">{loadError}</p>
          <Button className="mt-4" variant="outline" onClick={() => void loadCredit()}>
            <RefreshCw className="mr-2 h-4 w-4" />Thử lại
          </Button>
        </Card>
      ) : credit ? (
        <Card className="mx-auto max-w-5xl border-brand-200 p-5 sm:p-7">
          <div className="grid gap-7 md:grid-cols-2 md:items-center">
            <section aria-labelledby="ai-credit-balance-heading">
              <h2 id="ai-credit-balance-heading" className="text-lg font-bold text-ink">Số dư AI Credit</h2>
              <p className="mt-3 rounded-xl bg-raised px-4 py-3 text-sm text-ink-muted">
                Số dư hiện tại{' '}
                <strong className="text-lg text-brand-700">
                  {credit.balance.toLocaleString('vi-VN', { maximumFractionDigits: 9 })} Credit
                </strong>
              </p>
              <p className="mt-4 text-sm leading-6 text-ink-muted">
                AI Credit không hết hạn. Hệ thống dùng quota tháng trước; Credit được dùng khi quota đã hết và không thay đổi quyền chọn model.
              </p>
              <p className="mt-2 text-sm text-ink-muted">
                Tỷ lệ hiện tại: <strong className="text-ink">{credit.creditsPer1000Vnd.toLocaleString('vi-VN')} Credit / 1.000đ</strong>
              </p>
              <Link to="/giao-dich" className="mt-5 inline-flex items-center gap-2 text-sm font-semibold text-brand-700 hover:underline">
                Xem lịch sử giao dịch <ArrowRight className="h-4 w-4" />
              </Link>
            </section>

            <section aria-labelledby="ai-credit-amount-heading">
              <h2 id="ai-credit-amount-heading" className="text-base font-bold text-ink">Số tiền nạp (VND)</h2>
              <div className="mt-2 flex rounded-xl border border-brand-200 bg-paper focus-within:border-brand-500 focus-within:ring-2 focus-within:ring-brand-100">
                <input
                  id="ai-credit-top-up-amount"
                  className="min-w-0 flex-1 rounded-l-xl bg-transparent px-4 py-3 text-lg font-bold text-ink outline-none"
                  type="number"
                  inputMode="numeric"
                  min={credit.minimumTopUpVnd}
                  max={credit.maximumTopUpVnd}
                  step="1"
                  value={amount}
                  onChange={event => setAmount(event.target.value)}
                  aria-describedby="ai-credit-top-up-limits"
                />
                <span className="flex items-center border-l border-brand-100 px-4 font-semibold text-ink-muted">đ</span>
              </div>
              <p id="ai-credit-top-up-limits" className="mt-2 text-xs text-ink-muted">
                Từ {credit.minimumTopUpVnd.toLocaleString('vi-VN')}đ đến {credit.maximumTopUpVnd.toLocaleString('vi-VN')}đ mỗi giao dịch.
              </p>
              <div className="mt-4 flex items-center justify-between gap-3 rounded-xl bg-brand-50 px-4 py-3">
                <span className="text-sm text-ink-muted">Bạn nhận được</span>
                <strong className="text-lg font-extrabold text-brand-700">
                  {creditPreview.toLocaleString('vi-VN', { maximumFractionDigits: 9 })} Credit
                </strong>
              </div>
              {submitError && <p role="alert" className="mt-3 flex items-start gap-2 text-sm text-red-700"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />{submitError}</p>}
              <Button className="mt-4 w-full" onClick={() => void handleTopUp()} disabled={submitting || !isValidAmount}>
                {submitting
                  ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" />Đang tạo giao dịch…</>
                  : <>Nạp tiền qua payOS<ArrowRight className="ml-2 h-4 w-4" /></>}
              </Button>
              <p className="mt-3 flex items-start justify-center gap-2 text-center text-xs text-ink-muted">
                <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-brand-700" />
                Credit được cộng sau khi xác minh thanh toán. Đơn chờ hết hạn sau 15 phút.
              </p>
            </section>
          </div>
        </Card>
      ) : null}
    </PageContainer>
  );
}
