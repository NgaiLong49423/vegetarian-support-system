import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertCircle, Calendar, Check, Loader2, Sparkles, Wallet } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button, Card } from '../components/ui';
import { useAuth } from '../components/AuthContext';
import {
  subscriptionApi,
  type MySubscriptionResponse,
  type MyAiUsageResponse,
  type PlanResponse,
  type SubscriptionTier,
} from '../api/subscription';
import { formatVnd } from '../utils/date';
import { formatSubscriptionDate as formatDate } from '../utils/subscriptionDate';

export function AiPlans() {
  const navigate = useNavigate();
  const { account, isAuthenticated: isLoggedIn } = useAuth();
  const canUseAiCredit = isLoggedIn && (account?.role === 'CUSTOMER' || account?.role === 'EXPERT');

  const [plans, setPlans] = useState<PlanResponse[]>([]);
  const [plansError, setPlansError] = useState<string | null>(null);
  const [subscription, setSubscription] = useState<MySubscriptionResponse | null>(null);
  const [usage, setUsage] = useState<MyAiUsageResponse | null>(null);
  const [usageError, setUsageError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [subscribingTier, setSubscribingTier] = useState<SubscriptionTier | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [aiCreditBalance, setAiCreditBalance] = useState<number | null>(null);
  const [aiCreditLoading, setAiCreditLoading] = useState(false);
  const [aiCreditError, setAiCreditError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    async function loadData() {
      try {
        const fetchedPlans = await subscriptionApi.getPlans();
        if (active && fetchedPlans.length > 0) {
          setPlans(fetchedPlans);
          setPlansError(null);
        }
      } catch {
        if (active) setPlansError('Không tải được giá và quyền lợi hiện hành. Thử tải lại trang để tránh xem thông tin cũ.');
      }

      if (isLoggedIn) {
        try {
          const sub = await subscriptionApi.getMySubscription();
          if (active) {
            setSubscription(sub);
          }
        } catch {
          // If not authenticated or session expired, ignore
        }
        try {
          const currentUsage = await subscriptionApi.getMyAiUsage();
          if (active) setUsage(currentUsage);
        } catch (cause) {
          if (active) setUsageError(cause instanceof Error ? cause.message : 'Chưa tải được mức sử dụng AI.');
        }
      }

      if (canUseAiCredit) {
        if (active) {
          setAiCreditBalance(null);
          setAiCreditLoading(true);
        }
        try {
          const wallet = await subscriptionApi.getMyAiCredit();
          if (active) {
            setAiCreditBalance(wallet.balance);
            setAiCreditError(null);
          }
        } catch (cause) {
          if (active) setAiCreditError(cause instanceof Error ? cause.message : 'Chưa tải được số dư AI Credit.');
        } finally {
          if (active) setAiCreditLoading(false);
        }
      } else if (active) {
        setAiCreditBalance(null);
        setAiCreditError(null);
        setAiCreditLoading(false);
      }

      if (active) {
        setLoading(false);
      }
    }

    void loadData();

    return () => {
      active = false;
    };
  }, [canUseAiCredit, isLoggedIn]);

  const currentTier: SubscriptionTier =
    isLoggedIn && subscription?.status === 'ACTIVE' ? subscription.tier : 'FREE';
  const remainingQuotaPercent = usage && usage.quotaTokens > 0
    ? Math.round(Math.max(0, Math.min(100, usage.remainingTokens / usage.quotaTokens * 100)))
    : 0;
  const usedQuotaPercent = 100 - remainingQuotaPercent;

  async function handleSubscribe(tier: 'PLUS' | 'PRO') {
    if (!isLoggedIn) {
      navigate('/dang-nhap', { state: { returnTo: '/goi-ai' } });
      return;
    }

    setErrorMsg(null);
    setSubscribingTier(tier);

    try {
      const response = await subscriptionApi.createCheckout({ tier });
      let validCheckout = false;
      try {
        const url = new URL(response.checkoutUrl);
        validCheckout = url.protocol === 'https:' && url.hostname === 'pay.payos.vn'
          && !url.username && !url.password && (!url.port || url.port === '443');
      } catch { /* Invalid links must never send a member to a fake success page. */ }

      if (validCheckout) {
        window.location.href = response.checkoutUrl;
      } else {
        setErrorMsg('Không thể khởi tạo liên kết thanh toán. Vui lòng thử lại sau.');
        setSubscribingTier(null);
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Đã có lỗi xảy ra khi tạo giao dịch thanh toán.';
      setErrorMsg(message);
      setSubscribingTier(null);
    }
  }

  return (
    <PageContainer className="py-8">
      <nav className="mb-5 text-sm text-ink-muted">
        <Link to="/ho-so" className="hover:text-brand-600">
          Hồ sơ
        </Link>{' '}
        / Gói AI
      </nav>

      <div className="mb-8 text-center">
        <Sparkles className="mx-auto mb-3 h-8 w-8 text-brand-600" />
        <h1 className="text-3xl font-extrabold text-ink">Nâng cấp gói AI</h1>
        {!isLoggedIn ? (
          <p className="mt-2 text-sm font-semibold text-brand-700">
            Đăng nhập để xem gói hiện tại của bạn
          </p>
        ) : (
          <div className="mt-3 flex flex-wrap items-center justify-center gap-2 text-sm">
            <span className="text-ink-muted">Gói hiện tại của bạn:</span>
            {currentTier === 'FREE' ? (
              <Badge tone="neutral">Miễn phí</Badge>
            ) : (
              <Badge tone="brand">
                Gói {currentTier}
              </Badge>
            )}

            {currentTier !== 'FREE' && subscription?.endsAt && (
              <span className="inline-flex items-center gap-1 font-medium text-brand-700">
                <Calendar className="h-4 w-4" />
                Hết hạn ngày: {formatDate(subscription.endsAt)}
              </span>
            )}
          </div>
        )}

        <p className="mt-2 text-xs text-ink-muted">
          Gói quyết định hạn mức token và model Gemini; quyền dùng chức năng AI theo vai trò và quyền nghiệp vụ của tài khoản. Free tính theo tháng dương lịch, Plus/Pro theo tháng gói. Guest có 20.000 token dùng thử mỗi phiên. Plus/Pro thanh toán từng tháng qua payOS, không tự động gia hạn.
        </p>
      </div>

      {isLoggedIn && (
        <section aria-labelledby="ai-usage-heading" className="mb-8 w-full">
          <Card className="p-5 sm:p-6">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 id="ai-usage-heading" className="text-lg font-bold text-ink">Mức sử dụng token AI</h2>
                <p className="mt-1 text-sm text-ink-muted">
                  {usage ? `Gói ${usage.tier} · cấp lại ${formatDate(usage.resetsAt)}` : 'Đang tải hạn mức của kỳ hiện tại.'}
                </p>
              </div>
              {usage && <Badge tone={remainingQuotaPercent === 0 ? 'neutral' : 'brand'}>
                Còn {remainingQuotaPercent}%
              </Badge>}
            </div>
            {usage ? (
              <>
                <div className="mt-5 flex justify-between gap-3 text-sm">
                  <span className="font-semibold text-ink">Đã sử dụng {usedQuotaPercent}%</span>
                  <span className="text-ink-muted">Còn lại {remainingQuotaPercent}%</span>
                </div>
                <div className="mt-2 h-3 overflow-hidden rounded-full bg-brand-50" role="progressbar"
                  aria-label="Phần trăm hạn mức token còn lại trong kỳ hiện tại" aria-valuemin={0}
                  aria-valuemax={100} aria-valuenow={remainingQuotaPercent}>
                  <div className="h-full rounded-full bg-brand-600 transition-[width]"
                    style={{ width: `${remainingQuotaPercent}%` }} />
                </div>
              </>
            ) : usageError ? (
              <p role="alert" className="mt-4 text-sm text-red-700">{usageError}</p>
            ) : null}
          </Card>
        </section>
      )}

      {errorMsg && (
        <div role="alert" className="mx-auto mb-6 flex max-w-2xl items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
          <span>{errorMsg}</span>
        </div>
      )}

      <section aria-labelledby="ai-credit-top-up-heading" className="mb-8 w-full">
        <Card className="flex flex-col items-start justify-between gap-5 border-brand-200 p-5 sm:flex-row sm:items-center sm:p-6">
          <div className="flex items-start gap-3">
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700"><Wallet className="h-5 w-5" /></span>
            <div>
              <h2 id="ai-credit-top-up-heading" className="text-lg font-bold text-ink">Cần thêm AI Credit?</h2>
              <p className="mt-1 text-sm text-ink-muted">Xem số dư, chọn số tiền và thanh toán an toàn trên trang nạp AI Credit.</p>
              {canUseAiCredit && (
                <p className="mt-2 text-sm text-ink-muted" aria-live="polite">
                  Số dư AI Credit:{' '}
                  {aiCreditLoading ? <span>Đang tải…</span> : aiCreditError ? (
                    <span className="text-red-700">{aiCreditError}</span>
                  ) : (
                    <strong className="font-bold text-brand-700">
                      {(aiCreditBalance ?? 0).toLocaleString('vi-VN', { maximumFractionDigits: 9 })} Credit
                    </strong>
                  )}
                </p>
              )}
            </div>
          </div>
          <Link to={isLoggedIn ? '/nap-ai-credit' : '/dang-nhap'} state={!isLoggedIn ? { returnTo: '/nap-ai-credit' } : undefined}>
            <Button as="span" className="w-full sm:w-auto"><Wallet className="mr-2 h-4 w-4" />Nạp tiền</Button>
          </Link>
        </Card>
      </section>

      {loading ? (
        <div className="flex justify-center py-16">
          <Loader2 className="h-8 w-8 animate-spin text-brand-600" />
        </div>
      ) : plansError || plans.length === 0 ? (
        <div role="alert" className="mx-auto max-w-2xl rounded-xl border border-amber-200 bg-amber-50 p-4 text-center text-sm text-amber-900">
          {plansError ?? 'Chưa có cấu hình gói AI để hiển thị.'}
        </div>
      ) : (
        /* Current prices, token quotas, and model entitlements come from the Backend Admin configuration. */
        <div className="grid gap-6 lg:grid-cols-3">
          {plans.map((plan) => {
            const isCurrent = plan.tier === currentTier;
            const isProcessing = subscribingTier === plan.tier;
            const isPro = plan.tier === 'PRO';
            const tierClass = `ai-plan-card ai-plan-card--${plan.tier.toLowerCase()}`;

            return (
              <Card
                key={plan.tier}
                data-tier={plan.tier}
                className={`${tierClass} flex flex-col p-6`}
              >
                <div className="ai-plan-card__border" aria-hidden="true" />
                <div className="ai-plan-card__surface" aria-hidden="true" />

                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-extrabold text-brand-400">{plan.name}</h2>
                  {isCurrent && <Badge tone="leaf">Đang sử dụng</Badge>}
                  {!isCurrent && isPro && <Badge tone="brand">Khuyên dùng</Badge>}
                </div>

                <p className="mt-4 text-3xl font-extrabold text-ink">
                  {formatVnd(plan.priceVnd)}{' '}
                  <span className="text-sm font-medium text-ink-muted">VNĐ / tháng</span>
                </p>

                <p className="mt-2 text-xs text-ink-muted">{plan.description}</p>

                <hr className="ai-plan-card__line my-5" />

                <ul className="mb-6 flex-1 space-y-3">
                  {plan.features.map((feature, idx) => (
                    <li key={idx} className="flex items-center gap-2.5 text-sm text-ink-soft">
                      <span className="ai-plan-card__check shrink-0">
                        <Check className="ai-plan-card__check-svg" />
                      </span>
                      <span>{feature}</span>
                    </li>
                  ))}
                </ul>

                <div className="pt-2">
                  {!isLoggedIn ? (
                    plan.tier === 'FREE' ? (
                      <p className="rounded-full bg-brand-50 border border-brand-200 px-4 py-3 text-center text-sm font-semibold text-ink-muted">
                        Gói cơ bản miễn phí
                      </p>
                    ) : (
                      <button
                        disabled
                        title="Đăng nhập để nâng cấp gói"
                        className="w-full cursor-not-allowed rounded-full bg-brand-100 px-4 py-3 text-sm font-semibold text-ink-muted"
                      >
                        Thanh toán chưa khả dụng
                      </button>
                    )
                  ) : isCurrent ? (
                    <div className="rounded-full bg-brand-50 border border-brand-200 px-4 py-3 text-center text-sm font-semibold text-brand-600">
                      Gói hiện tại của bạn
                    </div>
                  ) : plan.tier === 'FREE' ? (
                    <div className="rounded-full bg-brand-50 border border-brand-200 px-4 py-3 text-center text-sm font-medium text-ink-muted">
                      Gói cơ bản mặc định
                    </div>
                  ) : (
                    <Button
                      variant={isPro ? 'primary' : 'outline'}
                      className={`ai-plan-cta w-full py-3 ${isPro ? 'ai-plan-cta--pro' : 'ai-plan-cta--plus'}`}
                      disabled={subscribingTier !== null}
                      onClick={() => handleSubscribe(plan.tier as 'PLUS' | 'PRO')}
                    >
                      {isProcessing ? (
                        <>
                          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          Đang tạo liên kết thanh toán...
                        </>
                      ) : !isLoggedIn ? (
                        'Đăng nhập để nâng cấp'
                      ) : (
                        `Nâng cấp lên ${plan.name}`
                      )}
                    </Button>
                  )}
                </div>
              </Card>
            );
          })}
        </div>
      )}

      <div className="mt-8 flex flex-col items-center justify-between gap-4 rounded-2xl border border-brand-100 bg-paper/90 p-4 sm:flex-row sm:px-6">
        <div className="text-sm text-ink-soft">
          <p className="font-semibold text-ink">Thanh toán an toàn qua cổng VietQR payOS</p>
          <p className="text-xs text-ink-muted">
            Quyền lợi PLUS / PRO được kích hoạt tự động ngay sau khi hoàn tất giao dịch.
          </p>
        </div>
        <Link
          to="/giao-dich"
          className="tx-history-card"
        >
          <div className="tx-left-side" aria-hidden="true">
            <div className="tx-stage">
              <div className="tx-card">
                <div className="tx-card-line" />
                <div className="tx-buttons" />
              </div>
              <div className="tx-post">
                <div className="tx-post-line" />
                <div className="tx-screen">
                  <div className="tx-dollar">$</div>
                </div>
                <div className="tx-numbers" />
                <div className="tx-numbers-line2" />
              </div>
            </div>
          </div>
          <div className="tx-right-side">
            <span className="tx-new">Xem lịch sử giao dịch</span>
            <svg
              viewBox="0 0 451.846 451.847"
              height={18}
              width={18}
              xmlns="http://www.w3.org/2000/svg"
              className="tx-arrow"
              aria-hidden="true"
            >
              <path
                fill="currentColor"
                d="M345.441 248.292L151.154 442.573c-12.359 12.365-32.397 12.365-44.75 0-12.354-12.354-12.354-32.391 0-44.744L278.318 225.92 106.409 54.017c-12.354-12.359-12.354-32.394 0-44.748 12.354-12.359 32.391-12.359 44.75 0l194.287 194.284c6.177 6.18 9.262 14.271 9.262 22.366 0 8.099-3.091 16.196-9.267 22.373z"
              />
            </svg>
          </div>
        </Link>
      </div>
    </PageContainer>
  );
}
