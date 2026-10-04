import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertCircle, Calendar, Check, CreditCard, Loader2, Sparkles } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button, Card } from '../components/ui';
import { useDemoAccount } from '../components/DemoAccount';
import { demoAiPlan } from '../data/mockData';
import { hasAccessToken } from '../lib/apiClient';
import {
  subscriptionApi,
  type MySubscriptionResponse,
  type PlanResponse,
  type SubscriptionTier,
} from '../api/subscription';
import { formatDate, formatVnd } from '../utils/date';

const DEFAULT_PLANS: PlanResponse[] = [
  {
    tier: 'FREE',
    name: 'FREE',
    priceVnd: 0,
    billingCycle: 'Hàng tháng',
    description: 'Trải nghiệm các tính năng AI cơ bản',
    features: ['AI Chatbot cơ bản', 'Gợi ý món chay cơ bản'],
    autoRenew: false,
  },
  {
    tier: 'PLUS',
    name: 'PLUS',
    priceVnd: 49000,
    billingCycle: 'Hàng tháng',
    description: 'Dành cho người ăn chay muốn sáng tạo thực đơn',
    features: ['Toàn bộ quyền của FREE', 'AI hỗ trợ soạn bài viết và công thức', 'AI gợi ý biến tấu món chay sáng tạo'],
    autoRenew: false,
  },
  {
    tier: 'PRO',
    name: 'PRO',
    priceVnd: 99000,
    billingCycle: 'Hàng tháng',
    description: 'Dành cho người ăn chay chuyên sâu & tối ưu dinh dưỡng',
    features: [
      'Toàn bộ quyền của PLUS',
      'AI lập thực đơn tuần 7 ngày theo dinh dưỡng cá nhân',
      'Phân tích vi chất và tối ưu hoá calo chuyên sâu',
    ],
    autoRenew: false,
  },
];

export function AiPlans() {
  const navigate = useNavigate();
  const { active: isDemoActive } = useDemoAccount();
  const isLoggedIn = hasAccessToken();

  const [plans, setPlans] = useState<PlanResponse[]>(DEFAULT_PLANS);
  const [subscription, setSubscription] = useState<MySubscriptionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [subscribingTier, setSubscribingTier] = useState<SubscriptionTier | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    async function loadData() {
      try {
        const fetchedPlans = await subscriptionApi.getPlans();
        if (active && fetchedPlans.length > 0) {
          setPlans(fetchedPlans);
        }
      } catch {
        // Fall back to DEFAULT_PLANS if API is unreachable
      }

      if (isLoggedIn && !isDemoActive) {
        try {
          const sub = await subscriptionApi.getMySubscription();
          if (active) {
            setSubscription(sub);
          }
        } catch {
          // If not authenticated or session expired, ignore
        }
      }

      if (active) {
        setLoading(false);
      }
    }

    void loadData();

    return () => {
      active = false;
    };
  }, [isLoggedIn, isDemoActive]);

  const currentTier: SubscriptionTier =
    isLoggedIn && subscription?.status === 'ACTIVE' ? subscription.tier : 'FREE';

  async function handleSubscribe(tier: 'PLUS' | 'PRO') {
    if (!isLoggedIn) {
      navigate('/dang-nhap', { state: { returnTo: '/goi-ai' } });
      return;
    }

    setErrorMsg(null);
    setSubscribingTier(tier);

    try {
      const returnUrl = `${window.location.origin}/payment/success`;
      const cancelUrl = `${window.location.origin}/payment/cancel`;

      const response = await subscriptionApi.createCheckout({
        tier,
        returnUrl,
        cancelUrl,
      });

      if (response.checkoutUrl) {
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
        {!isLoggedIn || isDemoActive ? (
          <p className="mt-2 text-sm font-semibold text-brand-700">
            Gói hiện tại: {demoAiPlan} (demo)
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
          Giá theo tháng, thanh toán từng kỳ qua VietQR (payOS) và không tự động gia hạn.
        </p>
      </div>

      {errorMsg && (
        <div className="mx-auto mb-6 flex max-w-2xl items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
          <span>{errorMsg}</span>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-16">
          <Loader2 className="h-8 w-8 animate-spin text-brand-600" />
        </div>
      ) : (
        /* AC-13.1: Exactly 3 tiers: FREE 0 VNĐ/tháng, PLUS 49.000 VNĐ/tháng, PRO 99.000 VNĐ/tháng */
        <div className="grid gap-6 lg:grid-cols-3">
          {plans.map((plan) => {
            const isCurrent = plan.tier === currentTier;
            const isProcessing = subscribingTier === plan.tier;
            const isPro = plan.tier === 'PRO';

            return (
              <Card
                key={plan.tier}
                className={`flex flex-col p-6 transition-all ${
                  isPro ? 'border-brand-500 shadow-md ring-2 ring-brand-300' : ''
                }`}
              >
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-extrabold text-brand-700">{plan.name}</h2>
                  {isCurrent && <Badge tone="leaf">Đang sử dụng</Badge>}
                  {!isCurrent && isPro && <Badge tone="brand">Khuyên dùng</Badge>}
                </div>

                <p className="mt-4 text-3xl font-extrabold text-ink">
                  {formatVnd(plan.priceVnd)}{' '}
                  <span className="text-sm font-medium text-ink-muted">VNĐ / tháng</span>
                </p>

                <p className="mt-2 text-xs text-ink-muted">{plan.description}</p>

                <ul className="my-6 flex-1 space-y-3">
                  {plan.features.map((feature, idx) => (
                    <li key={idx} className="flex gap-2 text-sm text-ink-soft">
                      <Check className="h-4 w-4 shrink-0 text-leaf-600" />
                      <span>{feature}</span>
                    </li>
                  ))}
                </ul>

                <div className="pt-2">
                  {!isLoggedIn || isDemoActive ? (
                    plan.name === demoAiPlan ? (
                      <p className="rounded-xl bg-brand-50 px-4 py-3 text-center text-sm font-semibold text-brand-700">
                        Gói hiện tại (demo)
                      </p>
                    ) : (
                      <button
                        disabled
                        title="Chưa kết nối cổng thanh toán"
                        className="w-full cursor-not-allowed rounded-xl bg-brand-200 px-4 py-3 text-sm font-semibold text-ink-muted"
                      >
                        Thanh toán chưa khả dụng
                      </button>
                    )
                  ) : isCurrent ? (
                    <div className="rounded-xl bg-brand-50 px-4 py-3 text-center text-sm font-semibold text-brand-700">
                      Gói hiện tại của bạn
                    </div>
                  ) : plan.tier === 'FREE' ? (
                    <div className="rounded-xl bg-brand-50 px-4 py-3 text-center text-sm font-medium text-ink-muted">
                      Gói cơ bản mặc định
                    </div>
                  ) : (
                    <Button
                      variant={isPro ? 'primary' : 'outline'}
                      className="w-full py-3"
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

      <div className="mt-8 flex flex-col items-center justify-between gap-4 rounded-xl border border-brand-100 bg-brand-50/50 p-4 sm:flex-row">
        <div className="text-sm text-ink-soft">
          <p className="font-semibold text-ink">Thanh toán an toàn qua cổng VietQR payOS</p>
          <p className="text-xs text-ink-muted">
            Quyền lợi PLUS / PRO được kích hoạt tự động ngay sau khi hoàn tất giao dịch.
          </p>
        </div>
        <Link
          to="/giao-dich"
          className="inline-flex items-center gap-2 text-sm font-semibold text-brand-600 hover:text-brand-700"
        >
          <CreditCard className="h-4 w-4" /> Xem lịch sử giao dịch
        </Link>
      </div>
    </PageContainer>
  );
}
