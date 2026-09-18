import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  BadgeCheck,
  Bookmark,
  CalendarPlus,
  Check,
  ChevronRight,
  Clock,
  Flame,
  Heart,
  Leaf,
  Share2,
  ShoppingBasket,
  Star,
  Users,
  UtensilsCrossed,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { RecipeCard } from '../components/RecipeCard';
import { Badge, Button, Card, SectionHeading } from '../components/ui';
import { Modal } from '../components/Modal';
import { recipes } from '../data/mockData';

const nutritionRows = [
  { label: 'Năng lượng', value: '210 kcal', rdi: '11% RDI' },
  { label: 'Đạm thực vật', value: '14.2 g', rdi: '28% RDI' },
  { label: 'Carbohydrate', value: '18.5 g', rdi: '6% RDI' },
  { label: 'Chất béo tốt', value: '6.8 g', rdi: '9% RDI' },
  { label: 'Chất xơ (Fiber)', value: '4.2 g', rdi: '16% RDI' },
  { label: 'Natri (Sodium)', value: '420 mg', rdi: '18% RDI' },
  { label: 'Sắt hữu cơ', value: '2.8 mg', rdi: '22% RDI' },
  { label: 'Canxi (Calcium)', value: '160 mg', rdi: '16% RDI' },
];

export function RecipeDetail() {
  const { slug } = useParams();
  const recipe = recipes.find((r) => r.slug === slug) ?? recipes[0];
  const [saved, setSaved] = useState(false);
  const [checked, setChecked] = useState<Record<string, boolean>>({});
  const [toast, setToast] = useState<string | null>(null);
  const [planOpen, setPlanOpen] = useState(false);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2500);
  };

  const related = recipes.filter((r) => r.id !== recipe.id).slice(0, 4);
  const groupedIngredients = recipe.ingredients.reduce<Record<string, typeof recipe.ingredients>>((acc, ing) => {
    (acc[ing.group] ??= []).push(ing);
    return acc;
  }, {});

  return (
    <PageContainer className="py-8">
      {/* breadcrumb */}
      <nav className="mb-5 flex items-center gap-1.5 text-sm text-ink-muted">
        <Link to="/" className="hover:text-brand-600">Trang chủ</Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <Link to="/kham-pha" className="hover:text-brand-600">Khám phá</Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="font-medium text-ink-soft">{recipe.name}</span>
      </nav>

      {/* header */}
      <div className="mb-6 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="max-w-3xl">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <Badge tone="leaf" soft={false}><Leaf className="h-3.5 w-3.5" /> {recipe.diet}</Badge>
            <Badge tone="brand">{recipe.category}</Badge>
            <span className="inline-flex items-center gap-1 text-sm font-semibold text-ink">
              <Star className="h-4 w-4 fill-amber-400 text-amber-400" /> {recipe.rating}
              <span className="font-normal text-ink-muted">({recipe.reviews} đánh giá)</span>
            </span>
          </div>
          <h1 className="text-3xl font-extrabold leading-tight tracking-tight text-ink sm:text-4xl">
            {recipe.name}
          </h1>
          <div className="mt-4 flex items-center gap-3">
            <img src={recipe.author.avatar} alt="" className="h-11 w-11 rounded-full object-cover ring-2 ring-brand-100" />
            <div>
              <p className="flex items-center gap-1 font-semibold text-ink">
                {recipe.author.name}
                {recipe.author.verified && <BadgeCheck className="h-4 w-4 text-brand-600" />}
              </p>
              <p className="text-xs text-ink-muted">{recipe.author.bio ?? 'Thành viên cộng đồng Mâm Xanh'}</p>
            </div>
            <Button variant="outline" size="sm" className="ml-2">Theo dõi tác giả</Button>
          </div>
        </div>
        <div className="flex gap-2">
          <button onClick={() => setSaved((v) => !v)} className={`flex h-11 w-11 items-center justify-center rounded-xl border transition-colors ${saved ? 'border-brand-600 bg-brand-600 text-white' : 'border-brand-200 bg-white text-ink-soft hover:border-brand-300'}`}>
            <Heart className={`h-5 w-5 ${saved ? 'fill-current' : ''}`} />
          </button>
          <button className="flex h-11 w-11 items-center justify-center rounded-xl border border-brand-200 bg-white text-ink-soft transition-colors hover:border-brand-300">
            <Share2 className="h-5 w-5" />
          </button>
          <Button onClick={() => setPlanOpen(true)}>
            <CalendarPlus className="h-4 w-4" /> Thêm vào kế hoạch
          </Button>
        </div>
      </div>

      {/* stat row */}
      <div className="mb-8 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {[
          { icon: Clock, label: 'Chuẩn bị', value: `${recipe.prepTime} phút` },
          { icon: Flame, label: 'Nấu ăn', value: `${recipe.cookTime} phút` },
          { icon: Users, label: 'Khẩu phần', value: `${recipe.servings} người ăn` },
          { icon: UtensilsCrossed, label: 'Năng lượng', value: `${recipe.calories} kcal` },
        ].map((s) => (
          <div key={s.label} className="flex items-center gap-3 rounded-2xl border border-brand-100 bg-white p-4">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-100 text-brand-600">
              <s.icon className="h-5 w-5" />
            </span>
            <div>
              <p className="text-xs text-ink-muted">{s.label}</p>
              <p className="font-bold text-ink">{s.value}</p>
            </div>
          </div>
        ))}
      </div>

      {/* image + description */}
      <div className="mb-8 grid gap-6 lg:grid-cols-[1.2fr_1fr]">
        <div className="overflow-hidden rounded-2xl border border-brand-100">
          <img src={recipe.image} alt={recipe.name} className="h-full w-full object-cover" />
        </div>
        <Card className="flex flex-col p-6">
          <p className="mb-2 text-xs font-bold uppercase tracking-wider text-brand-600">Câu chuyện món ăn</p>
          <h2 className="mb-3 text-xl font-extrabold text-ink">Hương vị đất trời trong từng miếng đậu mềm mượt</h2>
          <p className="flex-1 leading-relaxed text-ink-soft">{recipe.description}</p>
          <div className="mt-5 flex flex-wrap gap-2">
            {recipe.tags.map((t) => (
              <span key={t} className="text-sm font-medium text-brand-600">{t}</span>
            ))}
          </div>
          <div className="mt-5 flex gap-2 border-t border-brand-50 pt-4">
            <Button variant="secondary" className="flex-1" onClick={() => setSaved((v) => !v)}>
              <Bookmark className="h-4 w-4" /> {saved ? 'Đã lưu' : 'Lưu lại'}
            </Button>
            <Button variant="outline" className="flex-1"><Share2 className="h-4 w-4" /> Chia sẻ</Button>
          </div>
        </Card>
      </div>

      {/* ingredients */}
      <Card className="mb-8 p-6">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="flex items-center gap-2 text-xl font-extrabold text-ink">
              <span className="text-brand-600">🥬</span> Nguyên liệu
            </h2>
            <p className="text-sm text-ink-muted">Tuỳ chỉnh theo khẩu phần · {recipe.servings} người ăn</p>
          </div>
          <Button
            onClick={() => showToast('Đã thêm nguyên liệu vào Danh sách đi chợ!')}
          >
            <ShoppingBasket className="h-4 w-4" /> Thêm tất cả vào Danh sách đi chợ
          </Button>
        </div>
        <div className="grid gap-x-8 gap-y-2 sm:grid-cols-2">
          {Object.entries(groupedIngredients).map(([group, items]) => (
            <div key={group} className="sm:contents">
              {items.map((ing) => (
                <label
                  key={ing.id}
                  className="flex cursor-pointer items-center gap-3 rounded-xl border border-brand-50 px-3 py-2.5 transition-colors hover:bg-brand-50/60"
                >
                  <button
                    type="button"
                    onClick={() => setChecked((c) => ({ ...c, [ing.id]: !c[ing.id] }))}
                    className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-md border transition-colors ${checked[ing.id] ? 'border-brand-600 bg-brand-600 text-white' : 'border-brand-300 bg-white'}`}
                  >
                    {checked[ing.id] && <Check className="h-3.5 w-3.5" />}
                  </button>
                  <span className={`flex-1 text-sm ${checked[ing.id] ? 'text-ink-muted line-through' : 'text-ink-soft'}`}>
                    {ing.name}
                    {ing.note && <span className="ml-1 text-xs text-ink-muted">· {ing.note}</span>}
                  </span>
                  <Badge tone="neutral">{ing.quantity}</Badge>
                </label>
              ))}
            </div>
          ))}
        </div>
      </Card>

      {/* steps */}
      <Card className="mb-8 p-6">
        <h2 className="mb-5 flex items-center gap-2 text-xl font-extrabold text-ink">
          <span className="text-brand-600">👩‍🍳</span> Các bước thực hiện
        </h2>
        <ol className="space-y-4">
          {recipe.steps.map((step, i) => (
            <li key={i} className="flex gap-4">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white">
                {i + 1}
              </span>
              <p className="pt-1 leading-relaxed text-ink-soft">{step}</p>
            </li>
          ))}
        </ol>
      </Card>

      {/* nutrition */}
      <Card className="mb-10 p-6">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-2">
          <h2 className="flex items-center gap-2 text-xl font-extrabold text-ink">
            <span className="text-brand-600">📊</span> Bảng dinh dưỡng khoa học 8 chỉ tiêu
          </h2>
          <Badge tone="leaf"><BadgeCheck className="h-3.5 w-3.5" /> Đạt chuẩn thực dưỡng USDA</Badge>
        </div>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          {nutritionRows.map((row) => (
            <div key={row.label} className="rounded-xl border border-brand-50 bg-brand-50/40 p-4">
              <p className="text-xs text-ink-muted">{row.label}</p>
              <p className="mt-0.5 text-lg font-extrabold text-ink">{row.value}</p>
              <p className="text-xs font-semibold text-brand-600">{row.rdi}</p>
            </div>
          ))}
        </div>
        <p className="mt-4 text-xs leading-relaxed text-ink-muted">
          * Chỉ số dinh dưỡng được tính toán dựa trên cơ sở dữ liệu Viện Dinh Dưỡng Quốc Gia Việt Nam &
          USDA FoodData Central. Kết quả có thể thay đổi tuỳ vào thương hiệu nguyên liệu bạn sử dụng.
        </p>
      </Card>

      <SectionHeading eyebrow="Có thể bạn thích" title="Công thức tương tự" />
      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {related.map((r) => (
          <RecipeCard key={r.id} recipe={r} />
        ))}
      </div>

      {/* add to plan modal */}
      <Modal open={planOpen} onClose={() => setPlanOpen(false)} title="Thêm vào kế hoạch bữa ăn">
        <p className="mb-4 text-sm text-ink-muted">Chọn ngày và buổi để thêm <strong className="text-ink">{recipe.name}</strong> vào thực đơn tuần của bạn.</p>
        <div className="mb-4 grid grid-cols-2 gap-3">
          <select className="rounded-xl border border-brand-200 bg-white px-3 py-2.5 text-sm outline-none focus:border-brand-400">
            {['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'].map((d) => <option key={d}>{d}</option>)}
          </select>
          <select className="rounded-xl border border-brand-200 bg-white px-3 py-2.5 text-sm outline-none focus:border-brand-400">
            {['Bữa sáng', 'Bữa trưa', 'Bữa tối'].map((d) => <option key={d}>{d}</option>)}
          </select>
        </div>
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={() => setPlanOpen(false)}>Huỷ</Button>
          <Button onClick={() => { setPlanOpen(false); showToast('Đã thêm món vào kế hoạch tuần!'); }}>Thêm vào kế hoạch</Button>
        </div>
      </Modal>

      {toast && (
        <div className="fixed bottom-6 left-1/2 z-50 flex -translate-x-1/2 items-center gap-2 rounded-full bg-ink px-5 py-3 text-sm font-semibold text-white shadow-xl">
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-leaf-500"><Check className="h-3.5 w-3.5" /></span>
          {toast}
        </div>
      )}
    </PageContainer>
  );
}
