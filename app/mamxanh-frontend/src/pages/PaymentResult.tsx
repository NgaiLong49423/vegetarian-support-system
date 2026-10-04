import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CheckCircle2, XCircle, ArrowRight, RefreshCw, CreditCard, Sparkles } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button, Card } from '../components/ui';
import { subscriptionApi, type MySubscriptionResponse } from '../api/subscription';
import { formatDate } from '../utils/date';
import { hasAccessToken } from '../lib/apiClient';

interface PaymentResultProps {
  mode: 'success' | 'cancel';
}

export function PaymentResult({ mode }: PaymentResultProps) {
  const [searchParams] = useSearchParams();
  const orderCode = searchParams.get('orderCode');
  const statusParam = searchParams.get('status');

  const [subscription, setSubscription] = useState<MySubscriptionResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function fetchLatestSubscription() {
      if (!hasAccessToken()) {
        setLoading(false);
        return;
      }
      try {
        const sub = await subscriptionApi.getMySubscription();
        if (active) {
          setSubscription(sub);
        }
      } catch {
        // Ignore error
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    fetchLatestSubscription();

    return () => {
      active = false;
    };
  }, []);

  const isSuccess = mode === 'success';

  return (
    <PageContainer className="py-12">
      <Card className="mx-auto max-w-lg p-8 text-center shadow-lg">
        {isSuccess ? (
          <>
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-leaf-100 text-leaf-600">
              <CheckCircle2 className="h-10 w-10" />
            </div>

            <h1 className="text-2xl font-extrabold text-ink">Thanh toán thành công!</h1>
            <p className="mt-2 text-sm text-ink-muted">
              Cảm ơn bạn đã đồng hành cùng Mâm Xanh. Gói dịch vụ AI của bạn đã được kích hoạt thành công.
            </p>

            {orderCode && (
              <p className="mt-2 text-xs font-medium text-ink-soft">
                Mã đơn hàng payOS: <span className="font-bold text-brand-700">#{orderCode}</span>
              </p>
            )}

            {/* Display activated subscription */}
            <div className="my-6 rounded-xl border border-brand-100 bg-brand-50/60 p-4 text-left">
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold text-ink">Trạng thái gói AI:</span>
                <Badge tone="leaf">
                  <Sparkles className="mr-1 h-3 w-3" />
                  {subscription?.tier ? `Gói ${subscription.tier}` : 'Đã kích hoạt'}
                </Badge>
              </div>

              {subscription?.endsAt && (
                <div className="mt-2 flex items-center justify-between text-xs text-ink-soft">
                  <span>Hạn sử dụng đến:</span>
                  <span className="font-bold text-brand-700">{formatDate(subscription.endsAt)}</span>
                </div>
              )}

              {subscription?.features && subscription.features.length > 0 && (
                <div className="mt-3 border-t border-brand-100 pt-3">
                  <p className="text-xs font-semibold text-ink-muted">Quyền lợi đã sẵn sàng sử dụng:</p>
                  <ul className="mt-1 space-y-1 text-xs text-ink-soft">
                    {subscription.features.map((f, i) => (
                      <li key={i} className="flex items-center gap-1.5">
                        <span className="text-leaf-600">✓</span> {f}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            <div className="flex flex-col gap-3 sm:flex-row">
              <Link to="/ho-so" className="flex-1">
                <Button variant="primary" className="w-full">
                  Xem hồ sơ <ArrowRight className="ml-1.5 h-4 w-4" />
                </Button>
              </Link>
              <Link to="/giao-dich" className="flex-1">
                <Button variant="outline" className="w-full">
                  <CreditCard className="mr-1.5 h-4 w-4" /> Lịch sử giao dịch
                </Button>
              </Link>
            </div>
          </>
        ) : (
          /* AC-13.6: Cancelled / Failed payment - retain existing tier */
          <>
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-amber-100 text-amber-600">
              <XCircle className="h-10 w-10" />
            </div>

            <h1 className="text-2xl font-extrabold text-ink">Giao dịch chưa hoàn tất</h1>
            <p className="mt-2 text-sm text-ink-muted">
              Giao dịch thanh toán đã bị hủy hoặc không thành công trên cổng thanh toán payOS.
            </p>

            {orderCode && (
              <p className="mt-2 text-xs font-medium text-ink-soft">
                Mã đơn hàng: <span className="font-semibold text-ink">#{orderCode}</span>
                {statusParam && <span className="ml-2 text-amber-700">({statusParam})</span>}
              </p>
            )}

            {/* AC-13.6: Retain existing subscription */}
            <div className="my-6 rounded-xl border border-amber-200 bg-amber-50/70 p-4 text-left">
              <p className="text-xs font-bold text-amber-800">Quyền lợi tài khoản của bạn được bảo toàn:</p>
              <p className="mt-1 text-xs text-amber-900">
                Gói hiện tại của bạn không bị ảnh hưởng và vẫn giữ nguyên quyền lợi đang có.
              </p>

              {subscription && (
                <div className="mt-3 flex items-center justify-between border-t border-amber-200/60 pt-2 text-xs">
                  <span className="font-medium text-ink">Gói hiện tại:</span>
                  <Badge tone={subscription.tier === 'FREE' ? 'neutral' : 'brand'}>
                    {subscription.tier === 'FREE' ? 'Miễn phí' : `Gói ${subscription.tier}`}
                  </Badge>
                </div>
              )}
            </div>

            <div className="flex flex-col gap-3 sm:flex-row">
              <Link to="/goi-ai" className="flex-1">
                <Button variant="primary" className="w-full">
                  <RefreshCw className="mr-1.5 h-4 w-4" /> Thử lại tại Bảng giá
                </Button>
              </Link>
              <Link to="/giao-dich" className="flex-1">
                <Button variant="outline" className="w-full">
                  <CreditCard className="mr-1.5 h-4 w-4" /> Xem lịch sử giao dịch
                </Button>
              </Link>
            </div>
          </>
        )}
      </Card>
    </PageContainer>
  );
}
