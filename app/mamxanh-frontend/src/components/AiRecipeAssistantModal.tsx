import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { recipesApi, type RecipeAiDraftRequest, type RecipeAiDraftResponse } from '../api/recipes';
import { asApiError } from '../lib/apiClient';
import {
  Sparkles,
  ArrowDownToLine,
  Flame,
  Users,
  Utensils,
  BookOpen,
  Info,
  Clock,
  CheckCircle2,
  AlertTriangle,
  Lightbulb,
} from 'lucide-react';
import { Modal } from './Modal';
import { Button } from './ui';
import type { RecipeFormOptions } from '../api/recipes';

export type GeneratedRecipeData = RecipeAiDraftResponse;

interface AiRecipeAssistantModalProps {
  open: boolean;
  onClose: () => void;
  options: RecipeFormOptions | null;
  initialTitle?: string;
  initialIngredients?: string;
  initialServings?: number;
  onApplyRecipe: (recipe: GeneratedRecipeData) => void;
}

// Bảng phân bổ 3 buổi ăn chính + bữa phụ theo khuyến nghị dinh dưỡng (chuẩn 2.000 kcal/ngày)
export type MealSession = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK' | 'CUSTOM';

interface MealSessionConfig {
  session: MealSession;
  label: string;
  badgeText: string;
  defaultCalories: number;
  minCalories: number;
  maxCalories: number;
  percentageRatio: string;
  recommendedCategory: string;
  description: string;
}

const MEAL_SESSIONS: MealSessionConfig[] = [
  {
    session: 'BREAKFAST',
    label: 'Bữa sáng',
    badgeText: '🌅 20–25% Calo ngày',
    defaultCalories: 350,
    minCalories: 250,
    maxCalories: 500,
    percentageRatio: '20% - 25%',
    recommendedCategory: 'MAIN',
    description: 'Nạp năng lượng mở đầu ngày mới, ưu tiên protein dễ hấp thu, tinh bột lành và ít dầu mỡ.',
  },
  {
    session: 'LUNCH',
    label: 'Bữa trưa',
    badgeText: '☀️ 35–40% Calo ngày',
    defaultCalories: 600,
    minCalories: 450,
    maxCalories: 750,
    percentageRatio: '35% - 40%',
    recommendedCategory: 'MAIN',
    description: 'Bữa ăn chính duy trì hoạt động trong ngày, cân đối đạm thực vật, chất xơ và món mặn/xào đậm vị.',
  },
  {
    session: 'DINNER',
    label: 'Bữa tối',
    badgeText: '🌙 30–35% Calo ngày',
    defaultCalories: 450,
    minCalories: 350,
    maxCalories: 600,
    percentageRatio: '30% - 35%',
    recommendedCategory: 'SOUP',
    description: 'Bữa ăn phục hồi thanh nhẹ, giàu khoáng chất, hạn chế chiên rán nhiều dầu để ngủ ngon.',
  },
  {
    session: 'SNACK',
    label: 'Bữa phụ / Ăn nhẹ',
    badgeText: '🥪 5–10% Calo ngày',
    defaultCalories: 200,
    minCalories: 100,
    maxCalories: 300,
    percentageRatio: '5% - 10%',
    recommendedCategory: 'DESSERT',
    description: 'Bổ sung năng lượng giữa buổi, nhanh gọn, mát lành và thanh đạm.',
  },
  {
    session: 'CUSTOM',
    label: 'Tùy chỉnh tự do',
    badgeText: '⚙️ Chuyên gia định lượng',
    defaultCalories: 400,
    minCalories: 100,
    maxCalories: 1500,
    percentageRatio: 'Tùy ý',
    recommendedCategory: 'MAIN',
    description: 'Chuyên gia ẩm thực tự do thiết lập chỉ tiêu calo theo mục tiêu riêng.',
  },
];

const QUICK_TEMPLATES = [
  {
    name: 'Xào cà chua với trứng',
    meal: 'BREAKFAST' as MealSession,
    cal: 350,
    ingredients: 'cà chua, trứng gà, hành lá, dầu ăn, muối ăn',
  },
  {
    name: 'Đậu hũ sốt cà chua',
    meal: 'LUNCH' as MealSession,
    cal: 550,
    ingredients: 'đậu hũ, cà chua, hành boa rô, dầu ăn, hạt nêm, nước tương',
  },
  {
    name: 'Canh chua nấm đậu bắp',
    meal: 'DINNER' as MealSession,
    cal: 420,
    ingredients: 'đậu hũ, nấm rơm, cà chua, đậu bắp, ngò rí, hạt nêm',
  },
  {
    name: 'Nấm xào súp lơ ớt chuông',
    meal: 'LUNCH' as MealSession,
    cal: 580,
    ingredients: 'nấm đùi gà, súp lơ trắng, ớt chuông, cà rốt, dầu ăn, hạt nêm',
  },
];

export function AiRecipeAssistantModal({
  open,
  onClose,
  options: _options,
  onApplyRecipe,
  initialTitle,
  initialIngredients,
  initialServings,
}: AiRecipeAssistantModalProps) {
  const [selectedMeal, setSelectedMeal] = useState<MealSession>('BREAKFAST');
  const [dishIdea, setDishIdea] = useState('Xào cà chua với trứng');
  const [rawIngredients, setRawIngredients] = useState('cà chua, trứng gà, hành lá, dầu ăn, muối ăn');
  const [targetCalories, setTargetCalories] = useState(350);
  const [servings, setServings] = useState(2);
  const [cookingMethod, setCookingMethod] = useState('STIR_FRY');
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedResult, setGeneratedResult] = useState<GeneratedRecipeData | null>(null);

  const [error, setError] = useState<ReturnType<typeof asApiError> | null>(null);
  const requestVersion = useRef(0);
  useEffect(() => {
    requestVersion.current++;
    setIsGenerating(false);
    if (open) {
      if (initialTitle?.trim()) setDishIdea(initialTitle);
      if (initialIngredients?.trim()) setRawIngredients(initialIngredients);
      if (initialServings) setServings(initialServings);
      setGeneratedResult(null);
      setError(null);
    }
    return () => { requestVersion.current++; };
  }, [open, initialTitle, initialIngredients, initialServings]);
  useEffect(() => {
    setGeneratedResult(null);
    setError(null);
    requestVersion.current++;
  }, [dishIdea, rawIngredients, targetCalories, servings, cookingMethod, selectedMeal]);
  const handleClose = () => { requestVersion.current++; setIsGenerating(false); onClose(); };

  // Khi chọn buổi ăn, tự động đề xuất mức calo khuyến nghị tương ứng
  const handleSelectMeal = (session: MealSession) => {
    setSelectedMeal(session);
    const config = MEAL_SESSIONS.find((s) => s.session === session);
    if (config && session !== 'CUSTOM') {
      setTargetCalories(config.defaultCalories);
    }
  };

  // Chọn mẫu món nhanh
  const handleApplyTemplate = (tmpl: typeof QUICK_TEMPLATES[number]) => {
    setDishIdea(tmpl.name);
    setRawIngredients(tmpl.ingredients);
    setSelectedMeal(tmpl.meal);
    setTargetCalories(tmpl.cal);
    setCookingMethod(tmpl.name.startsWith('Canh') ? 'SOUP' : tmpl.name.includes('sốt') ? 'BRAISED' : 'STIR_FRY');
  };

  // Gemini proposes the preview; calories are recalculated from the Backend catalog.
  const generateRecipe = async () => {
    if (isGenerating) return;
    if (dishIdea.trim().length < 3 || dishIdea.trim().length > 120 || !rawIngredients.trim()
        || !Number.isInteger(servings) || servings < 1 || servings > 50
        || !Number.isInteger(targetCalories) || targetCalories < 100 || targetCalories > 1500) {
      setError(asApiError(new Error('Nhập tên món 3–120 ký tự, nguyên liệu, 1–50 khẩu phần và mục tiêu 100–1.500 kcal/phần.')));
      return;
    }
    const version = ++requestVersion.current;
    setError(null);
    setGeneratedResult(null);
    setIsGenerating(true);
    try {
      const result = await recipesApi.generateDraft({ title: dishIdea.trim(), rawIngredients,
        mealType: selectedMeal, targetCaloriesPerServing: targetCalories, servings,
        cookingMethod: cookingMethod as RecipeAiDraftRequest['cookingMethod'] });
      if (version === requestVersion.current) setGeneratedResult(result);
    } catch (cause) {
      if (version === requestVersion.current) setError(asApiError(cause));
    } finally {
      if (version === requestVersion.current) setIsGenerating(false);
    }
  };

  const handleUpdateQuantity = (index: number, newQty: number) => {
    if (!generatedResult) return;
    const safeQty = Math.max(0.01, Math.min(50000, Number.isFinite(newQty) ? Math.round(newQty * 100) / 100 : 0.01));
    const nextIngredients = [...generatedResult.ingredients];
    const targetItem = nextIngredients[index];
    if (!targetItem) return;
    const newCal = Math.round(safeQty * targetItem.energyKcalPer100g) / 100;

    nextIngredients[index] = {
      ...targetItem,
      quantity: safeQty,
      calories: newCal,
    };

    const newTotal = nextIngredients.reduce((sum, item) => sum + item.calories, 0);

    setGeneratedResult({
      ...generatedResult,
      ingredients: nextIngredients,
      totalCalories: Math.round(newTotal * 100) / 100,
      caloriesPerServing: Math.round(newTotal / generatedResult.servings * 100) / 100,
    });
  };

  // Cấu trúc JSON chuẩn hóa để chuyên gia có thể sao chép hoặc kiểm tra
  const jsonExportData = generatedResult
    ? {
        mon_an: generatedResult.title,
        buoi_an: MEAL_SESSIONS.find((s) => s.session === generatedResult.mealType)?.label ?? 'Bữa chính',
        khau_phan: generatedResult.servings,
        muc_tieu_calo_moi_phan: generatedResult.targetCaloriesPerServing,
        tong_calo_muc_tieu: generatedResult.targetCaloriesPerServing * generatedResult.servings,
        tong_calo_thuc_te: generatedResult.totalCalories,
        calo_trung_binh_moi_phan: Math.round(
          generatedResult.totalCalories / generatedResult.servings,
        ),
        do_lech_calo: Math.abs(
          generatedResult.totalCalories -
            generatedResult.targetCaloriesPerServing * generatedResult.servings,
        ),
        do_khop_calo_phan_tram: `${(
          100 -
          Math.min(
            100,
            (Math.abs(
              generatedResult.totalCalories -
                generatedResult.targetCaloriesPerServing * generatedResult.servings,
            ) /
              (generatedResult.targetCaloriesPerServing * generatedResult.servings)) *
              100,
          )
        ).toFixed(1)}%`,
        kieu_an_chay: generatedResult.vegetarianType,
        ingredients: generatedResult.ingredients.map((item) => ({
          name: item.name,
          quantity: item.quantity,
          unit: item.unitCode,
          calories: item.calories,
        })),
        nguyen_lieu: generatedResult.ingredients.map((item) => ({
          ten: item.name,
          dinh_luong: item.quantity,
          don_vi: item.unitCode,
          calo_uoc_tinh: item.calories,
        })),
      }
    : null;

  const handleApply = () => {
    if (generatedResult) {
      onApplyRecipe(generatedResult);
      handleClose();
    }
  };

  return (
    <Modal open={open} onClose={handleClose} title="Trợ lý AI đồng hành cùng Chuyên gia ẩm thực" size="lg" trapFocus>
      <div className="space-y-5">
        {/* Lời dẫn & Tuyên bố miễn trừ chuyên môn: Không khẳng định AI luôn đúng, tôn trọng chuyên môn chuyên gia */}
        <div className="flex items-start gap-2.5 rounded-xl border border-amber-300 bg-amber-50 p-3.5 text-xs leading-relaxed text-amber-950 dark:border-amber-700 dark:bg-amber-950/50 dark:text-amber-100">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400" />
          <div className="space-y-1">
            <p className="font-bold text-amber-950 dark:text-amber-200">
              Khuyến nghị tham khảo dinh dưỡng — Chuyên gia ẩm thực giữ quyền quyết định
            </p>
            <p className="text-amber-900 dark:text-amber-100/90">
              Trợ lý AI ước tính calo và đề xuất định lượng nguyên liệu dựa trên dữ liệu thành phần thực phẩm tiêu chuẩn (Viện Dinh dưỡng & USDA). AI không khẳng định công thức luôn chính xác 100% hay thay thế chỉ định y tế. Bạn luôn có toàn quyền điều chỉnh định lượng, gia vị và phương pháp chế biến theo phong cách và kinh nghiệm chuyên môn của mình.
            </p>
          </div>
        </div>

        <fieldset disabled={isGenerating} className="min-w-0 space-y-5 disabled:opacity-70">
        {/* Mẫu gợi ý nhanh 1-click để rút ngắn thao tác */}
        <div>
          <div className="mb-1.5 flex items-center justify-between">
            <span className="text-xs font-bold text-ink-soft flex items-center gap-1.5">
              <Lightbulb className="h-3.5 w-3.5 text-amber-500" /> Gợi ý nhanh 1-click (Rút ngắn thao tác):
            </span>
          </div>
          <div className="flex flex-wrap gap-2">
            {QUICK_TEMPLATES.map((tmpl, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => handleApplyTemplate(tmpl)}
                className={`rounded-lg border px-2.5 py-1 text-xs transition-all ${
                  dishIdea === tmpl.name
                    ? 'border-brand-500 bg-brand-50 font-bold text-brand-800 dark:border-amber-500 dark:bg-amber-950/60 dark:text-amber-200'
                    : 'border-brand-200 bg-paper text-ink-soft hover:border-brand-400 hover:text-ink dark:hover:border-amber-500'
                }`}
              >
                {tmpl.name} ({tmpl.cal} kcal)
              </button>
            ))}
          </div>
        </div>

        {/* Khối ràng buộc 1: Phân bổ theo 3 Buổi ăn chính trong ngày */}
        <div className="rounded-xl border border-brand-100 bg-raised/70 p-3.5 space-y-2">
          <label className="block text-xs font-bold uppercase tracking-wider text-ink flex items-center gap-1.5">
            <Clock className="h-3.5 w-3.5 text-brand-600" /> Phân bổ món ăn theo buổi trong ngày
          </label>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-5">
            {MEAL_SESSIONS.map((sessionItem) => {
              const active = selectedMeal === sessionItem.session;
              return (
                <button
                  key={sessionItem.session}
                  type="button"
                  onClick={() => handleSelectMeal(sessionItem.session)}
                  className={`flex flex-col items-center justify-center rounded-xl border p-2.5 text-center transition-all ${
                    active
                      ? 'border-brand-500 bg-paper shadow-sm ring-2 ring-brand-200 font-bold text-brand-900'
                      : 'border-brand-200 bg-paper text-ink-soft hover:border-brand-400 hover:bg-raised dark:hover:border-brand-400'
                  }`}
                >
                  <span className="text-xs">{sessionItem.badgeText.split(' ')[0]}</span>
                  <span className="mt-0.5 text-xs font-bold">{sessionItem.label}</span>
                  <span className="text-[10px] text-ink-muted">
                    {sessionItem.session === 'CUSTOM' ? 'Tự do' : `~${sessionItem.defaultCalories} kcal`}
                  </span>
                </button>
              );
            })}
          </div>
          <p className="text-[11px] text-ink-muted italic">
            * {MEAL_SESSIONS.find((s) => s.session === selectedMeal)?.description}
          </p>
        </div>

        {/* Khối ràng buộc 2: Thông tin món ăn & Nguyên liệu */}
        <div className="grid gap-3.5 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label htmlFor="ai-dish-idea" className="mb-1 block text-xs font-bold text-ink">
              Tên món ăn hoặc ý tưởng chế biến *
            </label>
            <input
              id="ai-dish-idea" maxLength={120}
              type="text"
              value={dishIdea}
              onChange={(e) => setDishIdea(e.target.value)}
              placeholder="VD: Xào cà chua với trứng, Đậu hũ sốt cà chua..."
              className="w-full rounded-xl border border-brand-200 bg-paper px-3.5 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
            />
          </div>

          <div className="sm:col-span-2">
            <label htmlFor="ai-raw-ingredients" className="mb-1 block text-xs font-bold text-ink">
              Nguyên liệu sẵn có (Quăng danh sách vào, ngăn cách bằng dấu phẩy) *
            </label>
            <textarea
              id="ai-raw-ingredients" maxLength={2000}
              rows={2}
              value={rawIngredients}
              onChange={(e) => setRawIngredients(e.target.value)}
              placeholder="VD: cà chua, trứng gà, hành lá, dầu thực vật, gia vị..."
              className="w-full rounded-xl border border-brand-200 bg-paper px-3.5 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100 resize-y"
            />
          </div>

          {/* Ràng buộc Calo mục tiêu */}
          <div>
            <label htmlFor="ai-target-calories" className="mb-1 block text-xs font-bold text-ink flex items-center gap-1.5">
              <Flame className="h-3.5 w-3.5 text-amber-500" /> Ràng buộc Calo mục tiêu / phần ăn
            </label>
            <div className="flex items-center gap-2">
              <input
                id="ai-target-calories"
                type="number"
                min={100}
                max={1500}
                step={25}
                value={targetCalories}
                onChange={(e) => {
                  setTargetCalories(Number(e.target.value));
                  setSelectedMeal('CUSTOM');
                }}
                className="w-full rounded-xl border border-brand-200 bg-paper px-3.5 py-2 text-sm font-semibold text-ink outline-none focus:border-brand-500"
              />
              <span className="text-xs text-ink-muted shrink-0 font-medium">kcal / người</span>
            </div>
          </div>

          {/* Ràng buộc Khẩu phần ăn */}
          <div>
            <label htmlFor="ai-servings" className="mb-1 block text-xs font-bold text-ink flex items-center gap-1.5">
              <Users className="h-3.5 w-3.5 text-brand-600" /> Ràng buộc Số khẩu phần ăn
            </label>
            <select
              id="ai-servings"
              value={servings}
              onChange={(e) => setServings(Number(e.target.value))}
              className="w-full rounded-xl border border-brand-200 bg-paper px-3.5 py-2 text-sm font-semibold text-ink outline-none focus:border-brand-500"
            >
              <option value={1}>1 người (Khẩu phần đơn)</option>
              <option value={2}>2 người (Gia đình nhỏ)</option>
              <option value={3}>3 người</option>
              <option value={4}>4 người (Gia đình tiêu chuẩn)</option>
              <option value={6}>6 người</option>
              {![1, 2, 3, 4, 6].includes(servings) && <option value={servings}>{servings} người</option>}
            </select>
          </div>

          {/* Ràng buộc Phương pháp chế biến */}
          <div>
            <label htmlFor="ai-cooking-method" className="mb-1 block text-xs font-bold text-ink">
              Phương pháp chế biến chính
            </label>
            <select
              id="ai-cooking-method"
              value={cookingMethod}
              onChange={(e) => setCookingMethod(e.target.value)}
              className="w-full rounded-xl border border-brand-200 bg-paper px-3.5 py-2 text-sm font-semibold text-ink outline-none focus:border-brand-500"
            >
              <option value="STIR_FRY">Xào / Áp chảo (cần dầu ăn nhẹ)</option>
              <option value="SOUP">Canh / Súp (ít chất béo)</option>
              <option value="BRAISED">Kho / Rim đậm vị</option>
              <option value="BOILED">Luộc / Hấp thanh đạm</option>
              <option value="SALAD">Salad / Gỏi thanh mát</option>
            </select>
          </div>
        </div>

        </fieldset>
        {error && <p role="alert" className="rounded-xl border border-amber-300 bg-amber-50 p-3 text-sm text-amber-950">{error.message}{error.code === 'AI_SUBSCRIPTION_REQUIRED' && <Link to="/goi-ai" className="ml-1 font-semibold underline">Xem các gói AI</Link>}</p>}
        {/* Nút kích hoạt AI phân tích & tối ưu calo */}
        <div className="flex justify-end pt-1">
          <Button
            type="button"
            onClick={generateRecipe}
            disabled={isGenerating || !rawIngredients.trim()}
            className="flex items-center gap-2 bg-gradient-to-r from-brand-600 to-amber-600 hover:from-brand-700 hover:to-amber-700 text-white font-bold px-4 py-2.5 rounded-xl shadow-sm"
          >
            <Sparkles className="h-4 w-4" />
            {isGenerating ? 'Đang giải bài toán định lượng calo...' : 'Phân tích nguyên liệu & gợi ý định lượng'}
          </Button>
        </div>

        {/* KẾT QUẢ GỢI Ý TỪ AI: Đảm bảo tỷ lệ đúng cao và làm nổi bật 2 thứ: Nguyên liệu & Định lượng */}
        {generatedResult && (
          <div className="space-y-4 rounded-2xl border border-brand-200 bg-paper p-4 sm:p-5 shadow-sm animate-fade-in">
            {/* Header kết quả */}
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-brand-100 pb-3">
              <div>
                <span className="text-xs font-bold uppercase tracking-wider text-brand-600">
                  Công thức đề xuất
                </span>
                <h3 className="text-base font-extrabold text-ink">{generatedResult.title}</h3>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-200">
                  <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400" />
                  Độ khớp calo: {jsonExportData?.do_khop_calo_phan_tram}
                </span>
                <span className="rounded-lg bg-amber-50 px-2.5 py-1 text-xs font-bold text-amber-800 dark:bg-amber-950/50 dark:text-amber-200">
                  {Math.round(generatedResult.totalCalories / generatedResult.servings)} kcal / phần
                </span>
                <span className="rounded-lg bg-brand-50 px-2.5 py-1 text-xs font-bold text-brand-700 dark:bg-brand-950/50 dark:text-brand-200">
                  {generatedResult.servings} người
                </span>
              </div>
            </div>

            {/* Trạng thái xác thực & phân định rõ ràng giữa AI và Chuyên gia */}
            <div className="flex flex-wrap items-center gap-2 rounded-xl bg-raised border border-brand-200/80 p-2.5 text-xs">
              <span className="inline-flex items-center gap-1 rounded-lg border border-blue-200 bg-blue-50 px-2.5 py-1 font-semibold text-blue-700 dark:border-blue-800 dark:bg-blue-950/50 dark:text-blue-200">
                <Sparkles className="h-3.5 w-3.5 text-blue-600 dark:text-blue-400" /> Gợi ý đề xuất bởi AI (Tham khảo)
              </span>
              <span className="inline-flex items-center gap-1 rounded-lg border border-amber-200 bg-amber-50 px-2.5 py-1 font-semibold text-amber-700 dark:border-amber-800 dark:bg-amber-950/50 dark:text-amber-200">
                <AlertTriangle className="h-3.5 w-3.5 text-amber-600 dark:text-amber-400" /> Chưa xác minh dinh dưỡng y khoa
              </span>
              <span className="inline-flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 font-semibold text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-200">
                <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400" /> Chờ chuyên gia xác nhận & nạp
              </span>
            </div>

            {/* BẢNG 2 THỨ CỐT LÕI: NGUYÊN LIỆU & ĐỊNH LƯỢNG */}
            <div>
              <div className="mb-2 flex items-center justify-between">
                <h4 className="text-xs font-extrabold uppercase text-ink flex items-center gap-1.5">
                  <Utensils className="h-4 w-4 text-brand-600" /> 2 Yếu tố cốt lõi: Nguyên liệu & Định lượng chuẩn xác
                </h4>
                <span className="text-[11px] text-ink-muted">
                  Tổng calo: <strong className="text-brand-700 font-bold">{generatedResult.totalCalories} kcal</strong> / Mục tiêu: {generatedResult.targetCaloriesPerServing * generatedResult.servings} kcal
                </span>
              </div>

              <div className="overflow-hidden rounded-xl border border-brand-200">
                <table className="w-full text-left text-xs">
                  <thead className="bg-brand-50/80 text-ink">
                    <tr>
                      <th className="p-3 font-bold">1. Nguyên liệu</th>
                      <th className="p-3 text-right font-bold text-brand-800">2. Định lượng (Điều chỉnh được)</th>
                      <th className="p-3 text-right font-medium text-ink-soft">Đơn vị</th>
                      <th className="p-3 text-right font-medium text-ink-soft">Năng lượng ước tính</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-brand-100 bg-paper">
                    {generatedResult.ingredients.map((item, idx) => (
                      <tr key={idx} className="hover:bg-brand-50/30 transition-colors">
                        <td className="p-3 font-bold text-ink flex items-center gap-1.5">
                          <span className="h-1.5 w-1.5 rounded-full bg-brand-500"></span>
                          {item.name}
                        </td>
                        <td className="p-3 text-right font-extrabold text-brand-700">
                          <div className="inline-flex items-center justify-end gap-1.5">
                            <input
                              type="number"
                              min={0.01}
                              max={50000}
                              step={0.01}
                              value={item.quantity}
                              onChange={(e) => handleUpdateQuantity(idx, Number(e.target.value))}
                              aria-label={`Định lượng ${item.name}`}
                              className="w-20 rounded-lg border border-brand-300 bg-paper px-2 py-1 text-right text-xs font-bold text-brand-800 outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-200"
                            />
                            <span className="text-[11px] font-medium text-ink-muted">{item.unitCode}</span>
                          </div>
                        </td>
                        <td className="p-3 text-right font-medium text-ink-soft">
                          {item.unitName}
                        </td>
                        <td className="p-3 text-right font-semibold text-amber-700" title={`Nguồn năng lượng: ${item.sourceName}`}>
                          {item.calories} kcal
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot className="border-t-2 border-brand-200 bg-brand-50/50 font-bold text-ink">
                    <tr>
                      <td colSpan={2} className="p-3">
                        Tổng năng lượng khẩu phần ({generatedResult.servings} người):
                      </td>
                      <td colSpan={2} className="p-3 text-right text-brand-800 font-extrabold text-sm">
                        {generatedResult.totalCalories} kcal ({Math.round(generatedResult.totalCalories / generatedResult.servings)} kcal / phần)
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>

            {/* Hướng dẫn chế biến chi tiết */}
            <div>
              <h4 className="mb-1.5 text-xs font-bold uppercase text-ink flex items-center gap-1.5">
                <BookOpen className="h-3.5 w-3.5 text-brand-600" /> Hướng dẫn chế biến từng bước
              </h4>
              <div className="rounded-xl bg-raised border border-brand-200/80 p-3.5 text-xs text-ink-soft whitespace-pre-line leading-relaxed font-sans">
                {generatedResult.instructions}
              </div>
            </div>

            {/* Nạp 1-click vào biểu mẫu */}
            <div className="flex items-center justify-end border-t border-brand-100 pt-3">
              <Button
                type="button"
                onClick={handleApply}
                className="flex items-center gap-1.5 bg-brand-700 hover:bg-brand-800 text-white font-bold shadow-sm"
              >
                <ArrowDownToLine className="h-4 w-4" /> Nạp vào biểu mẫu công thức
              </Button>
            </div>
          </div>
        )}

        <div className="flex justify-end pt-2">
          <Button type="button" variant="outline" onClick={handleClose} aria-label="Đóng và trở lại biểu mẫu">
            Đóng
          </Button>
        </div>
      </div>
    </Modal>
  );
}
