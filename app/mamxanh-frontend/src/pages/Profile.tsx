import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Award, BadgeCheck, Bookmark, Heart, Leaf, Plus, Settings, UtensilsCrossed } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { RecipeCard } from '../components/RecipeCard';
import { Badge, Button, Card, EmptyState } from '../components/ui';
import { useDemoAccount } from '../components/DemoAccount';
import { currentUser, demoAiPlan, recipes } from '../data/mockData';
import { getSavedRecipeSlugs, subscribeSaved } from '../lib/savedRecipes';
import { useAuth } from '../components/AuthContext';

const tabs = ['Công thức đã lưu', 'Món yêu thích', 'Sở thích ăn chay', 'Tùy chọn'] as const;
type Tab = (typeof tabs)[number];

const preferences = [
  { label: 'Chế độ ăn chay', value: 'Thuần Chay (Vegan)' },
  { label: 'Dị ứng thực phẩm', value: 'Đậu phộng' },
  { label: 'Mục tiêu năng lượng', value: '1.850 kcal / ngày' },
  { label: 'Khẩu phần mặc định', value: '4 người ăn' },
];

const toggles = [
  { label: 'Nhắc nhở lên kế hoạch bữa ăn hàng tuần', on: true },
  { label: 'Gợi ý món chay theo mùa từ Gemini AI', on: true },
  { label: 'Cảnh báo khi thiếu hụt vi chất (B12, Sắt)', on: true },
  { label: 'Nhận bản tin cộng đồng Mâm Xanh', on: false },
];

export function Profile() {
  const { role, applicationStatus } = useDemoAccount();
  const { account } = useAuth();
  const [tab, setTab] = useState<Tab>('Công thức đã lưu');
  const [savedSlugs, setSavedSlugs] = useState(getSavedRecipeSlugs);
  const favorites = recipes.slice(2, 5);
  const [switches, setSwitches] = useState(toggles);

  useEffect(() => subscribeSaved((list) => setSavedSlugs(list.map((recipe) => recipe.slug))), []);

  const saved = recipes.filter((recipe) => savedSlugs.includes(recipe.slug));

  return (
    <PageContainer className="py-8">
      {/* header banner */}
      <Card className="mb-6 overflow-hidden">
        <div className="h-28 bg-gradient-to-r from-brand-500 via-brand-400 to-amber-400" />
        <div className="flex flex-col gap-4 p-6 sm:flex-row sm:items-end">
          <img src={currentUser.avatar} alt="" className="-mt-16 h-24 w-24 rounded-2xl object-cover ring-4 ring-white" />
          <div className="flex-1">
            <h1 className="flex items-center gap-2 text-2xl font-extrabold text-ink">
              {currentUser.name} <BadgeCheck className="h-5 w-5 text-brand-600" />
            </h1>
            <p className="text-sm text-ink-muted">{currentUser.bio}</p>
            <div className="mt-2 flex flex-wrap gap-2">
              <Badge tone="leaf"><Leaf className="h-3 w-3" /> Thuần Chay</Badge>
              <Badge tone={role === 'EXPERT' ? 'leaf' : role === 'ADMIN' ? 'brand' : 'neutral'}>
                {role === 'EXPERT' ? 'Chuyên gia ẩm thực' : role === 'ADMIN' ? 'Quản trị viên' : 'Thành viên'}
              </Badge>
              <Badge tone="brand">Gói AI {demoAiPlan} (demo)</Badge>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            {role === 'EXPERT' && (
              <Link to="/dang-cong-thuc" className="inline-flex items-center gap-1.5 rounded-xl bg-leaf-600 px-4 py-2 text-sm font-bold text-white shadow-sm hover:bg-leaf-700">
                <Plus className="h-4 w-4" /> Đăng công thức
              </Link>
            )}
            {account?.role === 'EXPERT' && (
              <Link to="/ho-so/chuyen-gia-demo" className="inline-flex items-center gap-1.5 rounded-xl border border-brand-200 px-4 py-2 text-sm font-bold text-brand-700 hover:bg-brand-50">
                Bài công thức của tôi
              </Link>
            )}
            {role === 'ADMIN' && (
              <Link to="/admin/xet-duyet-chuyen-gia" className="inline-flex items-center gap-1.5 rounded-xl bg-amber-600 px-4 py-2 text-sm font-bold text-white shadow-sm hover:bg-amber-700">
                Xét duyệt Chuyên gia
              </Link>
            )}
            {role === 'CUSTOMER' && (
              <Link to="/dang-ky-chuyen-gia" className="inline-flex items-center gap-1.5 rounded-xl border border-leaf-300 bg-leaf-50 px-4 py-2 text-sm font-bold text-leaf-800 hover:bg-leaf-100">
                <Award className="h-4 w-4 text-leaf-600" /> Đăng ký Chuyên gia
              </Link>
            )}
            <Button variant="outline"><Settings className="h-4 w-4" /> Chỉnh sửa</Button>
          </div>
        </div>

        {/* Customer CTA Banner if not expert */}
        {role === 'CUSTOMER' && (
          <div className="border-t border-brand-50 bg-leaf-50/50 p-4 sm:px-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-sm font-bold text-ink">Bạn có đam mê và bí quyết nấu món chay ngon?</p>
                <p className="text-xs text-ink-muted">
                  Đăng ký trở thành Chuyên gia ẩm thực để chia sẻ công thức độc quyền với cộng đồng Mâm Xanh. Không yêu cầu bằng cấp hay giấy tờ vật lý.
                </p>
              </div>
              <Link
                to="/dang-ky-chuyen-gia"
                className="rounded-xl bg-leaf-600 px-4 py-2 text-xs font-bold text-white shadow-sm hover:bg-leaf-700"
              >
                {applicationStatus === 'PENDING' ? 'Xem trạng thái đơn đang chờ' : 'Nộp đơn xét duyệt ngay'}
              </Link>
            </div>
          </div>
        )}
      </Card>

      {/* tabs */}
      <div className="mb-6 flex flex-wrap gap-2 border-b border-brand-100">
        {tabs.map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`-mb-px border-b-2 px-4 py-2.5 text-sm font-semibold transition-colors ${
              tab === t ? 'border-brand-600 text-brand-600' : 'border-transparent text-ink-muted hover:text-ink-soft'
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      {tab === 'Công thức đã lưu' && (
        saved.length ? (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {saved.map((r) => <RecipeCard key={r.id} recipe={r} />)}
          </div>
        ) : (
          <EmptyState icon="🔖" title="Chưa có công thức đã lưu" description="Lưu công thức bạn yêu thích để xem lại tại đây." />
        )
      )}

      {tab === 'Món yêu thích' && (
        favorites.length ? (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {favorites.map((r) => <RecipeCard key={r.id} recipe={r} />)}
          </div>
        ) : (
          <EmptyState icon="❤️" title="Chưa có món yêu thích" description="Nhấn biểu tượng trái tim trên các công thức để lưu vào đây." />
        )
      )}

      {tab === 'Sở thích ăn chay' && (
        <Card className="max-w-2xl p-6">
          <h3 className="mb-4 flex items-center gap-2 font-bold text-ink"><UtensilsCrossed className="h-5 w-5 text-brand-600" /> Thông tin cá nhân & sở thích</h3>
          <dl className="divide-y divide-brand-50">
            {preferences.map((p) => (
              <div key={p.label} className="flex items-center justify-between py-3">
                <dt className="text-sm text-ink-muted">{p.label}</dt>
                <dd className="text-sm font-semibold text-ink">{p.value}</dd>
              </div>
            ))}
          </dl>
        </Card>
      )}

      {tab === 'Tùy chọn' && (
        <Card className="max-w-2xl p-6">
          <h3 className="mb-4 font-bold text-ink">Tùy chọn thông báo cơ bản</h3>
          <div className="space-y-1">
            {switches.map((s, i) => (
              <label key={s.label} className="flex cursor-pointer items-center justify-between rounded-xl px-2 py-3 hover:bg-brand-50/60">
                <span className="text-sm text-ink-soft">{s.label}</span>
                <button
                  type="button"
                  onClick={() => setSwitches((prev) => prev.map((x, j) => (j === i ? { ...x, on: !x.on } : x)))}
                  className={`relative h-6 w-11 rounded-full transition-colors ${s.on ? 'bg-brand-600' : 'bg-brand-200'}`}
                >
                  <span className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform ${s.on ? 'left-[22px]' : 'left-0.5'}`} />
                </button>
              </label>
            ))}
          </div>
        </Card>
      )}

      <div className="mt-8 flex items-center gap-2 text-sm text-ink-muted">
        <Bookmark className="h-4 w-4" /> Dữ liệu được lưu cục bộ trên thiết bị cho phiên bản demo.
      </div>
    </PageContainer>
  );
}
