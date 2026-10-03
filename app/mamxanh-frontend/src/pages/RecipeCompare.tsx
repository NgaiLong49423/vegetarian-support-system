import { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { ArrowLeftRight, Info, ThumbsUp } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Card } from '../components/ui';
import { recipes } from '../data/mockData';

// Standard 9 nutrients according to SRS FR-60 & FR-37
const compareNutrients = [
  { key: 'energy', label: 'Năng lượng', unit: 'kcal' },
  { key: 'protein', label: 'Đạm thực vật', unit: 'g' },
  { key: 'fat', label: 'Chất béo tốt', unit: 'g' },
  { key: 'carbs', label: 'Carbohydrate', unit: 'g' },
  { key: 'fiber', label: 'Chất xơ', unit: 'g' },
  { key: 'sodium', label: 'Natri', unit: 'mg' },
  { key: 'iron', label: 'Sắt hữu cơ', unit: 'mg' },
  { key: 'calcium', label: 'Canxi thực vật', unit: 'mg' },
  { key: 'vitaminB12', label: 'Vitamin B12 & vi lượng', unit: 'mcg' },
];

// Simulated nutrient lookup for recipes
const recipeNutrientMock: Record<string, Record<string, number | null>> = {
  r1: {
    energy: 210,
    protein: 14.2,
    fat: 6.8,
    carbs: 18.5,
    fiber: 4.2,
    sodium: 420,
    iron: 2.8,
    calcium: 160,
    vitaminB12: null,
  },
  r2: {
    energy: 320,
    protein: 11.5,
    fat: 14.2,
    carbs: 34.0,
    fiber: 7.5,
    sodium: 180,
    iron: 3.1,
    calcium: 95,
    vitaminB12: null,
  },
  r3: {
    energy: 280,
    protein: 9.8,
    fat: 3.5,
    carbs: 48.0,
    fiber: 3.8,
    sodium: 680,
    iron: 2.2,
    calcium: 80,
    vitaminB12: null,
  },
  r4: {
    energy: 190,
    protein: 8.5,
    fat: 5.2,
    carbs: 26.0,
    fiber: 3.1,
    sodium: 310,
    iron: 1.8,
    calcium: 65,
    vitaminB12: null,
  },
};

export function RecipeComparePage() {
  const [params, setParams] = useSearchParams();
  const leftSlug = params.get('left') || recipes[0]?.slug;
  const rightSlug = params.get('right') || recipes[1]?.slug;

  const leftRecipe = recipes.find((r) => r.slug === leftSlug) || recipes[0];
  const [selectedRightSlug, setSelectedRightSlug] = useState(
      rightSlug && rightSlug !== leftRecipe.slug ? rightSlug : recipes[1]?.slug,
  );
  const rightRecipe = recipes.find((r) => r.slug === selectedRightSlug) || recipes[1];

  const handleLeftChange = (slug: string) => {
    setParams({ left: slug, right: rightRecipe.slug });
  };

  const handleRightChange = (slug: string) => {
    setSelectedRightSlug(slug);
    setParams({ left: leftRecipe.slug, right: slug });
  };

  const leftNutrients = recipeNutrientMock[leftRecipe.id] || {
    energy: leftRecipe.calories,
    protein: 12,
    fat: 6,
    carbs: 25,
    fiber: 4,
    sodium: 300,
    iron: 2,
    calcium: 100,
    vitaminB12: null,
  };
  const rightNutrients = recipeNutrientMock[rightRecipe.id] || {
    energy: rightRecipe.calories,
    protein: 10,
    fat: 8,
    carbs: 28,
    fiber: 5,
    sodium: 250,
    iron: 2.5,
    calcium: 90,
    vitaminB12: null,
  };

  return (
      <PageContainer className="py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <Badge tone="leaf">FR-60</Badge>
                <Badge tone="neutral">Frontend Integration (#64 / #70)</Badge>
              </div>
              <h1 className="mt-2 text-3xl font-extrabold text-ink sm:text-4xl">
                So sánh hai công thức công khai
              </h1>
              <p className="mt-1 text-sm text-ink-muted">
                Đối chiếu hai Recipe Post theo khẩu phần dự kiến. Ký hiệu ≈ biểu thị số liệu ước tính;
                giao diện không đưa ra Health Score hay kết luận món nào tốt hơn.
              </p>
            </div>
          </div>
        </div>

        {/* Selectors Bar */}
        <Card className="mb-8 p-6">
          <div className="grid items-center gap-4 md:grid-cols-[1fr_auto_1fr]">
            {/* Left Picker */}
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-ink-muted">
                Công thức 1
              </label>
              <select
                  value={leftRecipe.slug}
                  onChange={(e) => handleLeftChange(e.target.value)}
                  className="mt-1.5 w-full rounded-xl border border-brand-200 bg-white p-3 text-sm font-bold text-ink outline-none focus:border-brand-500"
              >
                {recipes.map((r) => (
                    <option key={r.id} value={r.slug} disabled={r.slug === rightRecipe.slug}>
                      {r.name} ({r.diet})
                    </option>
                ))}
              </select>
            </div>

            {/* Versus Icon */}
            <div className="flex justify-center">
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-100 font-extrabold text-brand-700 shadow-inner">
              <ArrowLeftRight className="h-5 w-5" />
            </span>
            </div>

            {/* Right Picker */}
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-ink-muted">
                Công thức 2
              </label>
              <select
                  value={rightRecipe.slug}
                  onChange={(e) => handleRightChange(e.target.value)}
                  className="mt-1.5 w-full rounded-xl border border-brand-200 bg-white p-3 text-sm font-bold text-ink outline-none focus:border-brand-500"
              >
                {recipes.map((r) => (
                    <option key={r.id} value={r.slug} disabled={r.slug === leftRecipe.slug}>
                      {r.name} ({r.diet})
                    </option>
                ))}
              </select>
            </div>
          </div>
        </Card>

        {/* Overview Cards Side-by-Side */}
        <div className="mb-8 grid gap-6 md:grid-cols-2">
          {/* Left Overview */}
          <Card className="p-6">
            <div className="aspect-video w-full overflow-hidden rounded-xl">
              <img
                  src={leftRecipe.image}
                  alt={leftRecipe.name}
                  className="h-full w-full object-cover"
              />
            </div>
            <div className="mt-4 flex items-center justify-between">
              <Badge tone="leaf">{leftRecipe.diet}</Badge>
              {leftRecipe.likePercentage !== undefined ? (
                  <span className="flex items-center gap-1 text-xs font-bold text-leaf-700">
                <ThumbsUp className="h-3.5 w-3.5" /> {leftRecipe.likePercentage}% Like
              </span>
              ) : (
                  <Badge tone="brand">Mới</Badge>
              )}
            </div>
            <h2 className="mt-2 text-xl font-bold text-ink">{leftRecipe.name}</h2>
            <p className="mt-1 text-xs text-ink-muted">Bởi {leftRecipe.author.name}</p>

            <div className="mt-4 grid grid-cols-3 gap-2 rounded-xl bg-brand-50/50 p-3 text-center text-xs">
              <div>
                <span className="text-ink-muted">Thời gian</span>
                <p className="font-bold text-ink">≈ {leftRecipe.prepTime + leftRecipe.cookTime}p</p>
              </div>
              <div>
                <span className="text-ink-muted">Khẩu phần</span>
                <p className="font-bold text-ink">≈ {leftRecipe.servings} người</p>
              </div>
              <div>
                <span className="text-ink-muted">Năng lượng</span>
                <p className="font-bold text-ink">≈ {leftRecipe.calories} kcal</p>
              </div>
            </div>
          </Card>

          {/* Right Overview */}
          <Card className="p-6">
            <div className="aspect-video w-full overflow-hidden rounded-xl">
              <img
                  src={rightRecipe.image}
                  alt={rightRecipe.name}
                  className="h-full w-full object-cover"
              />
            </div>
            <div className="mt-4 flex items-center justify-between">
              <Badge tone="leaf">{rightRecipe.diet}</Badge>
              {rightRecipe.likePercentage !== undefined ? (
                  <span className="flex items-center gap-1 text-xs font-bold text-leaf-700">
                <ThumbsUp className="h-3.5 w-3.5" /> {rightRecipe.likePercentage}% Like
              </span>
              ) : (
                  <Badge tone="brand">Mới</Badge>
              )}
            </div>
            <h2 className="mt-2 text-xl font-bold text-ink">{rightRecipe.name}</h2>
            <p className="mt-1 text-xs text-ink-muted">Bởi {rightRecipe.author.name}</p>

            <div className="mt-4 grid grid-cols-3 gap-2 rounded-xl bg-brand-50/50 p-3 text-center text-xs">
              <div>
                <span className="text-ink-muted">Thời gian</span>
                <p className="font-bold text-ink">≈ {rightRecipe.prepTime + rightRecipe.cookTime}p</p>
              </div>
              <div>
                <span className="text-ink-muted">Khẩu phần</span>
                <p className="font-bold text-ink">≈ {rightRecipe.servings} người</p>
              </div>
              <div>
                <span className="text-ink-muted">Năng lượng</span>
                <p className="font-bold text-ink">≈ {rightRecipe.calories} kcal</p>
              </div>
            </div>
          </Card>
        </div>

        {/* Comparison: Ingredients Table */}
        <Card className="mb-8 p-6">
          <h3 className="mb-4 text-lg font-bold text-ink">Đối chiếu nguyên liệu</h3>
          <p className="mb-4 text-xs text-ink-muted">
            Nguyên liệu được giữ nguyên định lượng và đơn vị gốc theo công thức của tác giả. Không tự ý
            quy đổi hoặc gộp chung.
          </p>

          <div className="grid gap-6 md:grid-cols-2">
            {/* Left Ingredients */}
            <div>
              <h4 className="mb-3 text-sm font-bold text-leaf-700">{leftRecipe.name}</h4>
              <div className="divide-y divide-brand-50">
                {leftRecipe.ingredients.map((ing) => (
                    <div
                        key={ing.id}
                        className="flex items-center justify-between py-2 text-xs"
                    >
                      <span className="text-ink-soft">{ing.name}</span>
                      <span className="font-semibold text-ink">{ing.quantity}</span>
                    </div>
                ))}
              </div>
            </div>

            {/* Right Ingredients */}
            <div>
              <h4 className="mb-3 text-sm font-bold text-leaf-700">{rightRecipe.name}</h4>
              <div className="divide-y divide-brand-50">
                {rightRecipe.ingredients.map((ing) => (
                    <div
                        key={ing.id}
                        className="flex items-center justify-between py-2 text-xs"
                    >
                      <span className="text-ink-soft">{ing.name}</span>
                      <span className="font-semibold text-ink">{ing.quantity}</span>
                    </div>
                ))}
              </div>
            </div>
          </div>
        </Card>

        {/* Comparison: Nutrition Table (9 metrics) */}
        <Card className="mb-8 p-6">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
            <div>
              <h3 className="text-lg font-bold text-ink">Bảng so sánh 9 chỉ tiêu dinh dưỡng</h3>
              <p className="text-xs text-ink-muted">
                Tính trên 1 khẩu phần dự kiến. Dữ liệu chưa xác định ghi rõ "Chưa đủ dữ liệu", không
                coi là 0. Tất cả số liệu mang tính xấp xỉ (≈).
              </p>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
              <tr className="border-b border-brand-100 text-xs font-bold uppercase text-ink-muted">
                <th className="py-3 pr-4">Chỉ tiêu dinh dưỡng</th>
                <th className="py-3 px-4">{leftRecipe.name}</th>
                <th className="py-3 px-4">{rightRecipe.name}</th>
                <th className="py-3 pl-4">Chênh lệch tham khảo</th>
              </tr>
              </thead>
              <tbody className="divide-y divide-brand-50">
              {compareNutrients.map((n) => {
                const valLeft = leftNutrients[n.key];
                const valRight = rightNutrients[n.key];
                const hasBoth =
                    valLeft !== null &&
                    valLeft !== undefined &&
                    valRight !== null &&
                    valRight !== undefined;
                const diff = hasBoth ? (valLeft - valRight).toFixed(1) : null;

                return (
                    <tr key={n.key} className="hover:bg-brand-50/30">
                      <td className="py-3 pr-4 font-medium text-ink-soft">{n.label}</td>
                      <td className="py-3 px-4">
                        {valLeft !== null && valLeft !== undefined ? (
                            <span className="font-bold text-ink">
                          ≈ {valLeft} {n.unit}
                        </span>
                        ) : (
                            <span className="rounded bg-brand-100 px-2 py-0.5 text-xs font-semibold text-ink-muted">
                          Chưa đủ dữ liệu
                        </span>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        {valRight !== null && valRight !== undefined ? (
                            <span className="font-bold text-ink">
                          ≈ {valRight} {n.unit}
                        </span>
                        ) : (
                            <span className="rounded bg-brand-100 px-2 py-0.5 text-xs font-semibold text-ink-muted">
                          Chưa đủ dữ liệu
                        </span>
                        )}
                      </td>
                      <td className="py-3 pl-4 text-xs font-semibold">
                        {hasBoth && diff !== null ? (
                            Number(diff) > 0 ? (
                                <span className="text-amber-700">
                            +≈ {diff} {n.unit}
                          </span>
                            ) : Number(diff) < 0 ? (
                                <span className="text-leaf-700">
                            -≈ {Math.abs(Number(diff))} {n.unit}
                          </span>
                            ) : (
                                <span className="text-ink-muted">Tương đương</span>
                            )
                        ) : (
                            <span className="text-ink-muted">—</span>
                        )}
                      </td>
                    </tr>
                );
              })}
              </tbody>
            </table>
          </div>

          <div className="mt-6 rounded-xl border border-brand-100 bg-brand-50/50 p-4 text-xs leading-relaxed text-ink-muted">
            <div className="flex items-start gap-2">
              <Info className="mt-0.5 h-4 w-4 shrink-0 text-brand-600" />
              <div>
                <strong>Tuyên bố miễn trừ trách nhiệm (FR-60 / BR-08):</strong>
                <p className="mt-1">
                  Bảng so sánh mang tính chất tham khảo thông tin ẩm thực, không phải công cụ chẩn đoán
                  y tế hoặc phân định chất lượng món ăn. Mâm Xanh không tính toán điểm Health Score và
                  không kết luận công thức nào "lành mạnh hơn".
                </p>
              </div>
            </div>
          </div>
        </Card>

        {/* Back to explore */}
        <div className="flex justify-center">
          <Link
              to="/kham-pha"
              className="text-sm font-semibold text-brand-600 hover:text-brand-700 hover:underline"
          >
            ← Quay lại khám phá công thức
          </Link>
        </div>
      </PageContainer>
  );
}