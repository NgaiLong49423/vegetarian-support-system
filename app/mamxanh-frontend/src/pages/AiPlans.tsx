import { Link } from 'react-router-dom';
import { Check, CreditCard, Sparkles } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Card } from '../components/ui';
import { demoAiPlan } from '../data/mockData';

const plans = [
  { name: 'FREE', price: '0', features: ['AI Chatbot cơ bản', 'Gợi ý món cơ bản'] },
  { name: 'PLUS', price: '49.000', features: ['Quyền của FREE', 'AI hỗ trợ soạn bài', 'AI gợi ý biến tấu món'] },
  { name: 'PRO', price: '99.000', features: ['Quyền của PLUS', 'AI lập thực đơn tuần 7 ngày theo dinh dưỡng'] },
];

export function AiPlans() {
  return <PageContainer className="py-8">
    <nav className="mb-5 text-sm text-ink-muted"><Link to="/ho-so" className="hover:text-brand-600">Hồ sơ</Link> / Gói AI</nav>
    <div className="mb-8 text-center">
      <Sparkles className="mx-auto mb-3 h-8 w-8 text-brand-600" />
      <h1 className="text-3xl font-extrabold text-ink">Nâng cấp gói AI</h1>
      <p className="mt-2 text-sm font-semibold text-brand-700">Gói hiện tại: {demoAiPlan} (demo)</p>
      <p className="mt-2 text-ink-muted">Giá theo tháng, thanh toán từng kỳ và không tự động gia hạn.</p>
    </div>
    <div className="grid gap-5 lg:grid-cols-3">
      {plans.map((plan) => <Card key={plan.name} className={`flex flex-col p-6 ${plan.name === 'PRO' ? 'border-brand-400 ring-1 ring-brand-300' : ''}`}>
        <h2 className="text-xl font-extrabold text-brand-700">{plan.name}</h2>
        <p className="mt-4 text-3xl font-extrabold text-ink">{plan.price} <span className="text-sm font-medium text-ink-muted">VNĐ / tháng</span></p>
        <ul className="my-6 flex-1 space-y-3">
          {plan.features.map((feature) => <li key={feature} className="flex gap-2 text-sm text-ink-soft"><Check className="h-4 w-4 shrink-0 text-leaf-600" /> {feature}</li>)}
        </ul>
        {plan.name === demoAiPlan ? <p className="rounded-xl bg-brand-50 px-4 py-3 text-center text-sm font-semibold text-brand-700">Gói hiện tại (demo)</p>
          : <button disabled title="Chưa kết nối cổng thanh toán" className="cursor-not-allowed rounded-xl bg-brand-200 px-4 py-3 text-sm font-semibold text-ink-muted">Thanh toán chưa khả dụng</button>}
      </Card>)}
    </div>
    <p className="mt-6 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-ink-soft">
      Giao diện đang được chuẩn bị. Quyền PLUS/PRO chỉ được kích hoạt sau khi máy chủ xác minh thanh toán thật; hiện chưa thể mua gói từ trang này.
    </p>
    <Link to="/giao-dich" className="mt-5 inline-flex items-center gap-2 font-semibold text-brand-600 hover:text-brand-700"><CreditCard className="h-4 w-4" /> Xem lịch sử giao dịch</Link>
  </PageContainer>;
}
