import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { subscriptionApi, type MySubscriptionResponse } from '../api/subscription';
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
import { assessRecipeDifficulty } from '../utils/difficultyCalculator';

export interface GeneratedRecipeData {
  title: string;
  description: string;
  instructions: string;
  dishCategory: string;
  vegetarianType: string;
  difficulty: string;
  servings: number;
  prepTimeMinutes: number;
  cookTimeMinutes: number;
  mealType: string;
  targetCaloriesPerServing: number;
  totalCalories: number;
  ingredients: Array<{
    name: string;
    quantity: number;
    unitCode: string;
    unitName: string;
    calories: number;
  }>;
}

interface AiRecipeAssistantModalProps {
  open: boolean;
  onClose: () => void;
  options: RecipeFormOptions | null;
  onApplyRecipe: (recipe: GeneratedRecipeData) => void;
  isPro?: boolean;
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

// Cơ sở tri thức dinh dưỡng nguyên liệu chay tiêu chuẩn (Viện Dinh dưỡng & USDA)
interface IngredientNutrientInfo {
  keywords: string[];
  unitCode: string;
  unitName: string;
  caloriesPerUnit: number;
  defaultPortionPerServing: number;
  minPortion: number;
  maxPortion: number;
  category: 'PROTEIN' | 'VEG' | 'FAT' | 'SEASONING' | 'CARB';
  isCountUnit?: boolean;
}

const NUTRITION_KNOWLEDGE_BASE: IngredientNutrientInfo[] = [
  // Nhóm Protein
  {
    keywords: ['trứng', 'trứng gà', 'trứng vịt', 'trung', 'trung ga', 'lòng đỏ', 'lòng trắng'],
    unitCode: 'qua',
    unitName: 'Quả',
    caloriesPerUnit: 72,
    defaultPortionPerServing: 1.5,
    minPortion: 1,
    maxPortion: 4,
    category: 'PROTEIN',
    isCountUnit: true,
  },
  {
    keywords: ['đậu hũ', 'dau hu', 'đậu phụ', 'tau hu', 'đậu hũ trắng', 'đậu hũ non', 'đậu hũ chiên'],
    unitCode: 'g',
    unitName: 'Gam',
    caloriesPerUnit: 0.82,
    defaultPortionPerServing: 120,
    minPortion: 50,
    maxPortion: 300,
    category: 'PROTEIN',
  },
  {
    keywords: ['nấm', 'nam', 'nấm rơm', 'nấm hương', 'nấm bào ngư', 'nấm đùi gà', 'nấm kim châm', 'nấm đông cô', 'nấm mỡ'],
    unitCode: 'g',
    unitName: 'Gam',
    caloriesPerUnit: 0.35,
    defaultPortionPerServing: 80,
    minPortion: 30,
    maxPortion: 200,
    category: 'PROTEIN',
  },
  // Nhóm Rau củ & Chất xơ
  {
    keywords: ['cà chua', 'ca chua', 'cà chua bi'],
    unitCode: 'g',
    unitName: 'Gam',
    caloriesPerUnit: 0.18,
    defaultPortionPerServing: 120,
    minPortion: 50,
    maxPortion: 300,
    category: 'VEG',
  },
  {
    keywords: ['cà rốt', 'ca rot'],
    unitCode: 'g',
    unitName: 'Gam',
    caloriesPerUnit: 0.41,
    defaultPortionPerServing: 60,
    minPortion: 20,
    maxPortion: 150,
    category: 'VEG',
  },
  {
    keywords: ['ớt chuông', 'ot chuong', 'ớt ngọt'],
    unitCode: 'g',
    unitName: 'Gam',
    caloriesPerUnit: 0.26,
    defaultPortionPerServing: 60,
    minPortion: 20,
    maxPortion: 150,
    category: 'VEG',
  },
  {
    keywords: ['bông cải', 'súp lơ', 'bong cai', 'bông cải xanh', 'bông cải trắng'],
    unitCode: 'g',
    unitName: 'Gam',
    caloriesPerUnit: 0.34,
    defaultPortionPerServing: 80,
    minPortion: 40,
    maxPortion: 200,
    category: 'VEG',
  },
  {
    keywords: ['cải thìa', 'cải ngọt', 'rau bina', 'rau muống', 'mồng tơi', 'cải bó xôi'],
    unitCode: 'g',
    unitName: 'Gam',
    caloriesPerUnit: 0.24,
    defaultPortionPerServing: 100,
    minPortion: 40,
    maxPortion: 250,
    category: 'VEG',
  },
  {
    keywords: ['đậu bắp', 'dau bap', 'đậu que', 'đậu cô ve'],
    unitCode: 'g',
    unitName: 'Gam',
    caloriesPerUnit: 0.31,
    defaultPortionPerServing: 70,
    minPortion: 30,
    maxPortion: 180,
    category: 'VEG',
  },
  {
    keywords: ['bắp ngọt', 'ngô ngọt', 'ngô'],
    unitCode: 'g',
    unitName: 'Gam',
    caloriesPerUnit: 0.86,
    defaultPortionPerServing: 50,
    minPortion: 20,
    maxPortion: 120,
    category: 'CARB',
  },
  {
    keywords: ['hành lá', 'hanh la', 'hành boaro', 'ngò rí', 'hành tây', 'mùi tàu', 'hành'],
    unitCode: 'g',
    unitName: 'Gam',
    caloriesPerUnit: 0.32,
    defaultPortionPerServing: 15,
    minPortion: 5,
    maxPortion: 40,
    category: 'VEG',
  },
  // Nhóm Dầu ăn & Chất béo
  {
    keywords: ['dầu ăn', 'dau an', 'dầu thực vật', 'dầu đậu nành', 'dầu mè', 'dầu olive'],
    unitCode: 'ml',
    unitName: 'Mililit',
    caloriesPerUnit: 8.84,
    defaultPortionPerServing: 10,
    minPortion: 5,
    maxPortion: 30,
    category: 'FAT',
  },
  // Nhóm Gia vị
  {
    keywords: ['hạt nêm chay', 'muối', 'gia vị', 'nước tương', 'tiêu', 'hạt nêm', 'duong', 'đường'],
    unitCode: 'g',
    unitName: 'Gam',
    caloriesPerUnit: 1.2,
    defaultPortionPerServing: 6,
    minPortion: 2,
    maxPortion: 15,
    category: 'SEASONING',
  },
];

// Mẫu công thức điền nhanh để rút ngắn thao tác cho chuyên gia
const QUICK_TEMPLATES = [
  {
    name: 'Xào cà chua với trứng',
    meal: 'BREAKFAST' as MealSession,
    cal: 350,
    ingredients: 'cà chua, trứng gà, hành lá, dầu thực vật, gia vị chay',
  },
  {
    name: 'Đậu hũ sốt cà chua',
    meal: 'LUNCH' as MealSession,
    cal: 550,
    ingredients: 'đậu hũ, cà chua, hành boaro, dầu ăn, hạt nêm chay, nước tương',
  },
  {
    name: 'Canh chua nấm đậu bắp',
    meal: 'DINNER' as MealSession,
    cal: 420,
    ingredients: 'đậu hũ, nấm rơm, cà chua, đậu bắp, ngò rí, hạt nêm chay',
  },
  {
    name: 'Nấm xào súp lơ ớt chuông',
    meal: 'LUNCH' as MealSession,
    cal: 580,
    ingredients: 'nấm đùi gà, súp lơ, ớt chuông, cà rốt, dầu mè, hạt nêm chay',
  },
];

export function AiRecipeAssistantModal({
  open,
  onClose,
  options: _options,
  onApplyRecipe,
  isPro,
}: AiRecipeAssistantModalProps) {
  const { account } = useAuth();
  const [internalSubscription, setInternalSubscription] = useState<MySubscriptionResponse | null>(null);
  const [entitlementError, setEntitlementError] = useState<string | null>(null);

  useEffect(() => {
    if (typeof isPro === 'boolean') return;
    let active = true;
    subscriptionApi.getMySubscription()
      .then((sub) => {
        if (active) setInternalSubscription(sub);
      })
      .catch(() => {
        if (active) setInternalSubscription(null);
      });
    return () => { active = false; };
  }, [isPro, open]);

  const isDemoPro = account?.email === 'demo-expert@mamxanh.local';
  const effectiveIsPro = typeof isPro === 'boolean'
    ? isPro
    : (internalSubscription
        ? internalSubscription.status === 'ACTIVE' && internalSubscription.tier === 'PRO'
        : isDemoPro);

  useEffect(() => {
    if (!open) {
      setEntitlementError(null);
    }
  }, [open]);

  useEffect(() => {
    if (!effectiveIsPro) {
      setGeneratedResult(null);
    }
  }, [effectiveIsPro]);

  const [selectedMeal, setSelectedMeal] = useState<MealSession>('BREAKFAST');
  const [dishIdea, setDishIdea] = useState('Xào cà chua với trứng');
  const [rawIngredients, setRawIngredients] = useState('cà chua, trứng gà, hành lá, dầu thực vật, gia vị chay');
  const [targetCalories, setTargetCalories] = useState(350);
  const [servings, setServings] = useState(2);
  const [cookingMethod, setCookingMethod] = useState('STIR_FRY');
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedResult, setGeneratedResult] = useState<GeneratedRecipeData | null>(null);

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
  };

  // Thuật toán AI phân giải nguyên liệu & tối ưu hóa calo chuẩn xác cao
  const generateRecipe = () => {
    if (!effectiveIsPro) {
      setEntitlementError('Tính năng Trợ lý AI phân tích nguyên liệu & gợi ý định lượng yêu cầu gói AI PRO còn hiệu lực. Hãy nâng cấp lên gói AI PRO để tiếp tục.');
      return;
    }
    setEntitlementError(null);
    setIsGenerating(true);

    setTimeout(() => {
      // 1. Phân tách danh sách nguyên liệu đầu vào
      const inputItems = rawIngredients
        .split(/[,;\n+]+/)
        .map((s) => s.trim().toLowerCase())
        .filter(Boolean);

      const targetTotalCal = targetCalories * servings;

      // 2. Nhận diện thể loại món
      let calculatedCategory = 'MAIN';
      if (/canh|súp|soup/i.test(dishIdea) || cookingMethod === 'SOUP') {
        calculatedCategory = 'SOUP';
      } else if (/gỏi|nộm|salad/i.test(dishIdea) || cookingMethod === 'SALAD') {
        calculatedCategory = 'SALAD';
      } else if (/chè|bánh|sinh tố|tráng miệng/i.test(dishIdea)) {
        calculatedCategory = 'DESSERT';
      }

      // 3. Nhận diện trường phái ăn chay
      let detectedVegType = 'VEGAN';
      const hasEgg = inputItems.some((item) => /trứng|trung/i.test(item)) || /trứng/i.test(dishIdea);
      const hasMilk = inputItems.some((item) => /sữa|pho mai|bơ|cream/i.test(item));
      if (hasEgg && hasMilk) detectedVegType = 'LACTO_OVO';
      else if (hasEgg) detectedVegType = 'OVO';
      else if (hasMilk) detectedVegType = 'LACTO';
      else detectedVegType = 'VEGAN';

      // 4. Ánh xạ nguyên liệu với Cơ sở tri thức dinh dưỡng
      interface MatchedItem {
        rawName: string;
        info: IngredientNutrientInfo;
      }
      const matchedItems: MatchedItem[] = [];

      inputItems.forEach((raw) => {
        const found = NUTRITION_KNOWLEDGE_BASE.find((k) =>
          k.keywords.some((w) => raw.includes(w)),
        );
        if (found) {
          const capitalized = raw.charAt(0).toUpperCase() + raw.slice(1);
          matchedItems.push({ rawName: capitalized, info: found });
        } else {
          // Nguyên liệu tùy biến từ người dùng: xếp loại rau củ chuẩn
          const capitalized = raw.charAt(0).toUpperCase() + raw.slice(1);
          matchedItems.push({
            rawName: capitalized,
            info: {
              keywords: [raw],
              unitCode: 'g',
              unitName: 'Gam',
              caloriesPerUnit: 0.38,
              defaultPortionPerServing: 60,
              minPortion: 20,
              maxPortion: 180,
              category: 'VEG',
            },
          });
        }
      });

      // Tự động bổ sung dầu thực vật và gia vị cơ bản nếu món xào/kho cần chất béo để hòa tan dưỡng chất
      const hasOil = matchedItems.some((m) => m.info.category === 'FAT');
      if (!hasOil && (cookingMethod === 'STIR_FRY' || /xào|chiên|rim|kho/i.test(dishIdea))) {
        const oilInfo = NUTRITION_KNOWLEDGE_BASE.find((k) => k.category === 'FAT')!;
        matchedItems.push({ rawName: 'Dầu thực vật', info: oilInfo });
      }
      const hasSeasoning = matchedItems.some((m) => m.info.category === 'SEASONING');
      if (!hasSeasoning) {
        const spiceInfo = NUTRITION_KNOWLEDGE_BASE.find((k) => k.category === 'SEASONING')!;
        matchedItems.push({ rawName: 'Gia vị chay (hạt nêm, muối, tiêu)', info: spiceInfo });
      }

      // 5. THUẬT TOÁN GIẢI TOÁN TỐI ƯU HÓA CALO (Error Minimization Solver)
      // Tách các nhóm: COUNT (Trứng), FAT (Dầu), PROTEIN (Đậu hũ), VEG (Cà chua/Rau), SEASONING (Gia vị)
      const countItems = matchedItems.filter((m) => m.info.isCountUnit);
      const fatItems = matchedItems.filter((m) => m.info.category === 'FAT');
      const proteinItems = matchedItems.filter((m) => m.info.category === 'PROTEIN' && !m.info.isCountUnit);
      const vegItems = matchedItems.filter((m) => m.info.category === 'VEG');
      const seasoningItems = matchedItems.filter((m) => m.info.category === 'SEASONING');

      // Kết quả tính toán nguyên liệu
      const computedQuantities: Map<string, number> = new Map();

      // Bước 5.1: Xử lý số lượng nguyên liệu đếm hạt/quả (VD: trứng gà)
      // Mỗi khẩu phần thường dùng 1 - 2 quả trứng
      let fixedCalCount = 0;
      countItems.forEach((c) => {
        // Cân đối số quả hợp lý theo tổng calo và số người
        let eggsCount = Math.round(servings * 1.5);
        if (targetCalories < 300) eggsCount = servings;
        else if (targetCalories > 500) eggsCount = servings * 2;
        eggsCount = Math.max(1, eggsCount);
        computedQuantities.set(c.rawName, eggsCount);
        fixedCalCount += eggsCount * c.info.caloriesPerUnit;
      });

      // Bước 5.2: Phân bổ gia vị cơ bản (muối, hạt nêm: ~5g/khẩu phần)
      let fixedCalSeasoning = 0;
      seasoningItems.forEach((s) => {
        const qty = Math.max(5, servings * 5);
        computedQuantities.set(s.rawName, qty);
        fixedCalSeasoning += qty * s.info.caloriesPerUnit;
      });

      // Lượng calo còn lại cần bù trừ
      const remainingCal = Math.max(50, targetTotalCal - fixedCalCount - fixedCalSeasoning);

      // Bước 5.3: Cân chỉnh chất béo (Dầu ăn) và Rau củ/Đậu hũ
      // Dầu ăn đóng vai trò cung cấp năng lượng và độ bóng mềm cho món ăn
      let idealOilMl = Math.round(servings * 8); // mặc định ~8ml/người
      if (cookingMethod === 'STIR_FRY' || /xào/i.test(dishIdea)) idealOilMl = Math.round(servings * 12);
      if (cookingMethod === 'SOUP') idealOilMl = Math.round(servings * 4);

      // Thử nghiệm các mức dầu ăn hợp lý (bội số 5ml) để tối thiểu sai số
      let bestDiff = Infinity;
      let bestOil = idealOilMl;
      let bestVegScale = 1;

      // Tính tổng năng lượng cơ sở của nhóm rau và đậu hũ
      const flexBaseCal =
        vegItems.reduce((acc, v) => acc + v.info.defaultPortionPerServing * servings * v.info.caloriesPerUnit, 0) +
        proteinItems.reduce((acc, p) => acc + p.info.defaultPortionPerServing * servings * p.info.caloriesPerUnit, 0);

      const baseVal = flexBaseCal > 0 ? flexBaseCal : 100;

      // Quét các mức dầu ăn khả dĩ (từ 5ml đến 40ml) để tìm tỷ lệ sai số thấp nhất
      for (let testOil = Math.max(5, servings * 5); testOil <= Math.min(60, servings * 25); testOil += 5) {
        const oilCal = fatItems.length > 0 ? testOil * fatItems[0].info.caloriesPerUnit : 0;
        const calForFlex = remainingCal - oilCal;
        if (calForFlex <= 0) continue;

        const scale = calForFlex / baseVal;
        // Kiểm tra xem tỉ lệ scale có trong ngưỡng tự nhiên (0.5x - 2.5x)
        if (scale >= 0.4 && scale <= 3.0) {
          // Tính tổng calo thực tế sau khi làm tròn bước 5g
          let simulatedCal = fixedCalCount + fixedCalSeasoning + oilCal;
          vegItems.forEach((v) => {
            const rawG = Math.max(10, Math.round((v.info.defaultPortionPerServing * servings * scale) / 5) * 5);
            simulatedCal += Math.round(rawG * v.info.caloriesPerUnit);
          });
          proteinItems.forEach((p) => {
            const rawG = Math.max(20, Math.round((p.info.defaultPortionPerServing * servings * scale) / 5) * 5);
            simulatedCal += Math.round(rawG * p.info.caloriesPerUnit);
          });

          const diff = Math.abs(simulatedCal - targetTotalCal);
          if (diff < bestDiff) {
            bestDiff = diff;
            bestOil = testOil;
            bestVegScale = scale;
          }
        }
      }

      // Bước 5.4: Gán định lượng chính xác
      fatItems.forEach((f) => {
        computedQuantities.set(f.rawName, bestOil);
      });

      vegItems.forEach((v) => {
        const g = Math.max(15, Math.round((v.info.defaultPortionPerServing * servings * bestVegScale) / 5) * 5);
        computedQuantities.set(v.rawName, g);
      });

      proteinItems.forEach((p) => {
        const g = Math.max(30, Math.round((p.info.defaultPortionPerServing * servings * bestVegScale) / 5) * 5);
        computedQuantities.set(p.rawName, g);
      });

      // Bước 5.5: Tổng hợp danh sách nguyên liệu và định lượng
      let finalTotalCal = 0;
      const finalIngredients: GeneratedRecipeData['ingredients'] = [];

      matchedItems.forEach(({ rawName, info }) => {
        const qty = computedQuantities.get(rawName) || 10;
        const cal = Math.round(qty * info.caloriesPerUnit);
        finalTotalCal += cal;

        finalIngredients.push({
          name: rawName,
          quantity: qty,
          unitCode: info.unitCode,
          unitName: info.unitName,
          calories: cal,
        });
      });

      // 6. Soạn nội dung hướng dẫn chế biến mạch lạc, thiết thực
      let naturalDescription = '';
      let naturalInstructions = '';

      if (hasEgg && /cà chua/i.test(dishIdea)) {
        naturalDescription = `Món ${dishIdea.trim()} thanh tao và giàu năng lượng, kết hợp hoàn hảo giữa vị chua dịu của cà chua và vị béo ngậy mềm mịn của trứng gà. Món ăn bổ sung protein dồi dào, cân đối calo lý tưởng cho bữa ăn gia đình.`;
        naturalInstructions = `1. Sơ chế nguyên liệu:
- Cà chua rửa sạch, cắt múi cau vừa ăn. Hành lá rửa sạch, phần gốc trắng đập dập băm nhỏ, phần lá xanh thái nhỏ.
- Đập trứng gà ra bát, nêm 1/2 muỗng cà phê hạt nêm chay và một ít tiêu xay, dùng đũa đánh tan đều cho sủi bọt nhẹ.

2. Chiên xào trứng lòng đào mềm xốp:
- Đặt chảo lên bếp, cho 1/2 lượng dầu thực vật vào đun nóng vừa.
- Đổ trứng vào chảo, chờ viền trứng hơi se lại thì dùng phới đảo nhẹ tay thành những mảng trứng lớn, vừa chín tới còn độ ẩm mềm xốp thì trút ra đĩa riêng (không đảo quá khô).

3. Làm sốt cà chua sền sệt:
- Dùng lại chảo, thêm lượng dầu ăn còn lại vào phi thơm gốc hành trắng.
- Cho toàn bộ cà chua vào xào đều tay ở lửa vừa. Nêm hạt nêm chay, nước tương và thêm khoảng 2-3 muỗng canh nước ấm.
- Đậy nắp chảo đun 2–3 phút cho cà chua chín mềm rục, dùng thìa dầm nhẹ để sốt sánh mịn tự nhiên.

4. Hòa quyện hương vị:
- Trút toàn bộ phần trứng đã xào vào chảo sốt cà chua, đảo nhẹ tay ở lửa nhỏ trong khoảng 1 phút để trứng ngấm đều nước sốt mà không bị nát.
- Rắc hành lá thái nhỏ và một chút tiêu xay lên trên rồi tắt bếp.

5. Thưởng thức:
- Dọn món ra đĩa sâu lòng, dùng nóng kèm cơm trắng dẻo hoặc bánh mì.`;
      } else {
        naturalDescription = `Món ${dishIdea.trim()} thanh nhẹ, kết hợp hài hòa các loại nguyên liệu thực vật tươi ngon, cung cấp lượng calo cân đối và dồi dào chất xơ tự nhiên cho cơ thể.`;
        naturalInstructions = `1. Sơ chế:
- Rửa sạch tất cả nguyên liệu dưới vòi nước chảy. Thái miếng vừa ăn phù hợp với từng loại rau củ và đậu nấm.

2. Chế biến:
- Làm nóng chảo với dầu thực vật, phi thơm hành boaro hoặc gia vị nền.
- Cho nguyên liệu chín chậm vào trước, đảo đều tay ở lửa vừa. Tiếp tục cho các nguyên liệu còn lại vào xào/nấu chín tới.

3. Nêm nếm:
- Nêm hạt nêm chay, nước tương vừa khẩu vị gia đình.

4. Hoàn thiện:
- Tắt bếp, rắc tiêu và rau thơm lên trên. Thưởng thức ngay khi món ăn còn nóng sốt.`;
      }

      setGeneratedResult({
        title: dishIdea.trim() || 'Món chay bổ dưỡng',
        description: naturalDescription,
        instructions: naturalInstructions,
        dishCategory: calculatedCategory,
        vegetarianType: detectedVegType,
        difficulty: assessRecipeDifficulty({
          ingredientCount: finalIngredients.length,
          prepTimeMinutes: 10,
          cookTimeMinutes: 15,
          instructions: naturalInstructions,
        }).code,
        servings,
        prepTimeMinutes: 10,
        cookTimeMinutes: 15,
        mealType: selectedMeal,
        targetCaloriesPerServing: targetCalories,
        totalCalories: finalTotalCal,
        ingredients: finalIngredients,
      });

      setIsGenerating(false);
    }, 450);
  };

  const handleUpdateQuantity = (index: number, newQty: number) => {
    if (!generatedResult) return;
    const safeQty = Math.max(1, Math.min(2000, Number.isNaN(newQty) ? 1 : newQty));
    const nextIngredients = [...generatedResult.ingredients];
    const targetItem = nextIngredients[index];
    if (!targetItem) return;

    const matched = NUTRITION_KNOWLEDGE_BASE.find((info) =>
      info.keywords.some((kw) => targetItem.name.toLowerCase().includes(kw)),
    );
    const calPerUnit = matched
      ? matched.caloriesPerUnit
      : targetItem.calories / (targetItem.quantity || 1);
    const newCal = Math.round(safeQty * calPerUnit);

    nextIngredients[index] = {
      ...targetItem,
      quantity: safeQty,
      calories: newCal,
    };

    const newTotal = nextIngredients.reduce((sum, item) => sum + item.calories, 0);

    setGeneratedResult({
      ...generatedResult,
      ingredients: nextIngredients,
      totalCalories: newTotal,
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
    if (!effectiveIsPro) {
      setEntitlementError('Tính năng Trợ lý AI phân tích nguyên liệu & gợi ý định lượng yêu cầu gói AI PRO còn hiệu lực. Hãy nâng cấp lên gói AI PRO để tiếp tục.');
      return;
    }
    if (generatedResult) {
      onApplyRecipe(generatedResult);
      onClose();
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Trợ lý AI đồng hành cùng Chuyên gia ẩm thực" size="lg">
      <div className="space-y-5">
        {!effectiveIsPro && (
          <div
            role="alert"
            className="flex items-start gap-3 rounded-2xl border border-amber-300 bg-amber-50 p-4 text-sm text-amber-950 shadow-sm dark:border-amber-700 dark:bg-amber-950/60 dark:text-amber-100"
          >
            <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400" />
            <div className="flex-1 space-y-1">
              <p className="font-bold text-amber-950 dark:text-amber-200">
                Tính năng Trợ lý AI yêu cầu gói AI PRO còn hiệu lực
              </p>
              <p className="text-xs text-amber-900/90 dark:text-amber-100/90">
                Trợ lý AI phân tích nguyên liệu, gợi ý định lượng chuẩn và tối ưu hóa calo là tính năng cao cấp dành riêng cho Chuyên gia sở hữu gói AI PRO. Tài khoản của bạn hiện chưa kích hoạt gói PRO.
              </p>
              <div className="pt-1.5">
                <Link
                  to="/goi-ai"
                  className="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-amber-500 to-orange-600 px-3.5 py-1.5 text-xs font-bold text-white shadow-md hover:from-amber-600 hover:to-orange-700 transition"
                >
                  <Sparkles className="h-3.5 w-3.5" />
                  Nâng cấp lên gói AI PRO
                </Link>
              </div>
            </div>
          </div>
        )}

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
              id="ai-dish-idea"
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
              id="ai-raw-ingredients"
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

        {/* Nút kích hoạt AI phân tích & tối ưu calo */}
        <div className="flex flex-col items-end gap-2 pt-1">
          {entitlementError && (
            <div role="alert" className="w-full flex items-center justify-between rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-800">
              <div className="flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 shrink-0 text-red-600" />
                <span>{entitlementError}</span>
              </div>
              <Link to="/goi-ai" className="font-bold underline text-red-900 hover:text-red-700 shrink-0 ml-2">
                Nâng cấp gói PRO
              </Link>
            </div>
          )}
          <Button
            type="button"
            onClick={() => {
              if (!effectiveIsPro) {
                setEntitlementError('Tính năng Trợ lý AI phân tích nguyên liệu & gợi ý định lượng yêu cầu gói AI PRO còn hiệu lực. Hãy nâng cấp lên gói AI PRO để tiếp tục.');
                return;
              }
              generateRecipe();
            }}
            disabled={isGenerating || !rawIngredients.trim() || !effectiveIsPro}
            className={`flex items-center gap-2 font-bold px-4 py-2.5 rounded-xl shadow-sm transition ${
              !effectiveIsPro
                ? 'bg-gray-300 text-gray-500 cursor-not-allowed dark:bg-gray-700 dark:text-gray-400'
                : 'bg-gradient-to-r from-brand-600 to-amber-600 hover:from-brand-700 hover:to-amber-700 text-white'
            }`}
          >
            <Sparkles className="h-4 w-4" />
            {!effectiveIsPro
              ? 'Yêu cầu gói AI PRO để phân tích'
              : isGenerating
                ? 'Đang giải bài toán định lượng calo...'
                : 'Phân tích nguyên liệu & gợi ý định lượng'}
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
              <div className="mb-2.5 space-y-1">
                <h4 className="text-xs font-extrabold uppercase text-ink flex items-center gap-1.5">
                  <Utensils className="h-4 w-4 text-brand-600" /> 2 Yếu tố cốt lõi: Nguyên liệu & Định lượng chuẩn xác
                </h4>
                <div className="text-[11px] text-ink-muted">
                  Tổng calo: <strong className="text-brand-700 font-bold">{generatedResult.totalCalories} kcal</strong> / Mục tiêu: {generatedResult.targetCaloriesPerServing * generatedResult.servings} kcal
                </div>
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
                              min={1}
                              max={2000}
                              value={item.quantity}
                              onChange={(e) => handleUpdateQuantity(idx, Number(e.target.value))}
                              aria-label={`Định lượng ${item.name}`}
                              className="w-20 rounded-lg border border-brand-300 bg-paper px-2 py-1 text-right text-xs font-bold text-brand-800 outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-200"
                            />
                            <span className="w-8 text-left text-[11px] font-medium text-ink-muted shrink-0">{item.unitCode}</span>
                          </div>
                        </td>
                        <td className="p-3 text-right font-medium text-ink-soft">
                          {item.unitName}
                        </td>
                        <td className="p-3 text-right font-semibold text-amber-700">
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
          <Button variant="outline" onClick={onClose}>
            Đóng
          </Button>
        </div>
      </div>
    </Modal>
  );
}
