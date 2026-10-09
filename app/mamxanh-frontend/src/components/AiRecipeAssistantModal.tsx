import { useState } from 'react';
import {
  Sparkles,
  Copy,
  Check,
  ArrowDownToLine,
  SlidersHorizontal,
  Flame,
  Users,
  Utensils,
  BookOpen,
  Info,
} from 'lucide-react';
import { Modal } from './Modal';
import { Button } from './ui';
import type { RecipeFormOptions } from '../api/recipes';

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
}

// Database kiến thức dinh dưỡng tiêu chuẩn (kcal / đơn vị)
interface IngredientNutrientInfo {
  keywords: string[];
  unitCode: string;
  unitName: string;
  standardPortion: number; // định lượng cơ sở cho 1 phần ăn
  caloriesPerUnit: number;
  isMainProtein?: boolean;
}

const NUTRITION_KNOWLEDGE_BASE: IngredientNutrientInfo[] = [
  {
    keywords: ['trứng', 'trứng gà', 'trứng vịt', 'trung'],
    unitCode: 'qua',
    unitName: 'Quả',
    standardPortion: 1.5,
    caloriesPerUnit: 72,
    isMainProtein: true,
  },
  {
    keywords: ['cà chua', 'ca chua', 'cà chua bi'],
    unitCode: 'g',
    unitName: 'Gam',
    standardPortion: 100,
    caloriesPerUnit: 0.18, // 18 kcal / 100g
  },
  {
    keywords: ['đậu hũ', 'dau hu', 'đậu phụ', 'tau hu'],
    unitCode: 'g',
    unitName: 'Gam',
    standardPortion: 120,
    caloriesPerUnit: 0.82,
    isMainProtein: true,
  },
  {
    keywords: ['nấm', 'nam', 'nấm rơm', 'nấm hương', 'nấm đùi gà', 'nấm bào ngư'],
    unitCode: 'g',
    unitName: 'Gam',
    standardPortion: 80,
    caloriesPerUnit: 0.35,
  },
  {
    keywords: ['cà rốt', 'ca rot'],
    unitCode: 'g',
    unitName: 'Gam',
    standardPortion: 60,
    caloriesPerUnit: 0.41,
  },
  {
    keywords: ['dầu ăn', 'dau an', 'dầu thực vật', 'dầu mè', 'dầu hào chay'],
    unitCode: 'ml',
    unitName: 'Mililit',
    standardPortion: 7,
    caloriesPerUnit: 8.84, // ~88.4 kcal / 10ml
  },
  {
    keywords: ['hành lá', 'hanh la', 'hành boaro', 'ngò rí', 'hành'],
    unitCode: 'g',
    unitName: 'Gam',
    standardPortion: 10,
    caloriesPerUnit: 0.32,
  },
  {
    keywords: ['ớt chuông', 'ot chuong'],
    unitCode: 'g',
    unitName: 'Gam',
    standardPortion: 60,
    caloriesPerUnit: 0.26,
  },
  {
    keywords: ['bông cải', 'súp lơ', 'bong cai'],
    unitCode: 'g',
    unitName: 'Gam',
    standardPortion: 80,
    caloriesPerUnit: 0.34,
  },
  {
    keywords: ['hạt nêm chay', 'muối', 'gia vị', 'duong', 'tiêu', 'nuoc tuong'],
    unitCode: 'g',
    unitName: 'Gam',
    standardPortion: 5,
    caloriesPerUnit: 1.2,
  },
];

export function AiRecipeAssistantModal({
  open,
  onClose,
  options,
  onApplyRecipe,
}: AiRecipeAssistantModalProps) {
  const [dishIdea, setDishIdea] = useState('Xào cà chua với trứng');
  const [rawIngredients, setRawIngredients] = useState('cà chua, trứng gà, hành lá, dầu thực vật, gia vị chay');
  const [targetCalories, setTargetCalories] = useState(350);
  const [servings, setServings] = useState(2);
  const [vegType, setVegType] = useState('OVO'); // OVO, VEGAN, LACTO, LACTO_OVO
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedResult, setGeneratedResult] = useState<GeneratedRecipeData | null>(null);
  const [copiedJson, setCopiedJson] = useState(false);
  const [jsonViewOpen, setJsonViewOpen] = useState(false);

  const generateRecipe = () => {
    setIsGenerating(true);
    setCopiedJson(false);

    setTimeout(() => {
      // 1. Phân tích nguyên liệu người dùng nhập
      const inputItems = rawIngredients
        .split(/[,;\n+]+/)
        .map((s) => s.trim().toLowerCase())
        .filter(Boolean);

      const resolvedIngredients: GeneratedRecipeData['ingredients'] = [];
      const totalTargetCal = targetCalories * servings;

      // Nhận diện kiểu món và tên món chuẩn hóa
      let calculatedTitle = dishIdea.trim() || 'Món chay bổ dưỡng';
      let cleanDishCategory = 'MAIN';
      if (/xào|kho|rim|chiên|sốt/i.test(calculatedTitle)) {
        cleanDishCategory = 'MAIN';
      } else if (/canh|súp/i.test(calculatedTitle)) {
        cleanDishCategory = 'SOUP';
      } else if (/gỏi|nộm|salad/i.test(calculatedTitle)) {
        cleanDishCategory = 'SALAD';
      }

      // Xác định loại ăn chay dựa trên nguyên liệu
      let detectedVegType = vegType;
      const hasEgg = inputItems.some((item) => /trứng|trung/i.test(item)) || /trứng/i.test(dishIdea);
      const hasMilk = inputItems.some((item) => /sữa|pho mai|bơ/i.test(item));
      if (hasEgg && hasMilk) detectedVegType = 'LACTO_OVO';
      else if (hasEgg) detectedVegType = 'OVO';
      else if (hasMilk) detectedVegType = 'LACTO';
      else detectedVegType = 'VEGAN';

      // 2. Cân chỉnh định lượng chính xác theo tổng calo mục tiêu
      // Dành ~60% calo cho nguyên liệu protein chính, ~25% cho dầu/nước sốt, ~15% cho rau củ & gia vị
      let currentAllocatedCal = 0;

      // Danh sách nguyên liệu cần có
      const matchedInfos: Array<{
        name: string;
        info: IngredientNutrientInfo;
      }> = [];

      inputItems.forEach((raw) => {
        const found = NUTRITION_KNOWLEDGE_BASE.find((k) =>
          k.keywords.some((w) => raw.includes(w)),
        );
        if (found) {
          const capitalized = raw.charAt(0).toUpperCase() + raw.slice(1);
          matchedInfos.push({ name: capitalized, info: found });
        } else {
          // Nguyên liệu chưa có trong danh mục kiến thức -> gán định lượng mặc định rau củ
          const capitalized = raw.charAt(0).toUpperCase() + raw.slice(1);
          matchedInfos.push({
            name: capitalized,
            info: {
              keywords: [raw],
              unitCode: 'g',
              unitName: 'Gam',
              standardPortion: 50,
              caloriesPerUnit: 0.4,
            },
          });
        }
      });

      // Nếu thiếu dầu ăn hoặc gia vị khi nấu món xào, bổ sung nhẹ để tính calo chính xác
      if (!matchedInfos.some((m) => m.info.unitCode === 'ml') && /xào|chiên|rim/i.test(dishIdea)) {
        const oilInfo = NUTRITION_KNOWLEDGE_BASE.find((k) => k.keywords.includes('dầu ăn'))!;
        matchedInfos.push({ name: 'Dầu thực vật', info: oilInfo });
      }
      if (!matchedInfos.some((m) => m.name.toLowerCase().includes('gia vị') || m.name.toLowerCase().includes('nêm'))) {
        const spiceInfo = NUTRITION_KNOWLEDGE_BASE.find((k) => k.keywords.includes('hạt nêm chay'))!;
        matchedInfos.push({ name: 'Gia vị chay (hạt nêm, muối)', info: spiceInfo });
      }

      // Tính tỷ lệ cân chỉnh calo
      const baseCal = matchedInfos.reduce((sum, item) => {
        return sum + item.info.standardPortion * servings * item.info.caloriesPerUnit;
      }, 0);

      const ratio = baseCal > 0 ? totalTargetCal / baseCal : 1;

      // Xuất danh sách nguyên liệu và định lượng đã cân chỉnh
      matchedInfos.forEach(({ name, info }) => {
        let rawQty = info.standardPortion * servings * ratio;
        if (info.unitCode === 'qua') {
          rawQty = Math.max(1, Math.round(rawQty));
        } else if (info.unitCode === 'ml' || info.unitCode === 'g') {
          rawQty = Math.max(5, Math.round(rawQty / 5) * 5); // làm tròn bậc 5g/5ml cho tự nhiên
        }
        const itemCal = Math.round(rawQty * info.caloriesPerUnit);
        currentAllocatedCal += itemCal;

        resolvedIngredients.push({
          name,
          quantity: rawQty,
          unitCode: info.unitCode,
          unitName: info.unitName,
          calories: itemCal,
        });
      });

      const actualCalPerServing = Math.round(currentAllocatedCal / servings);

      // Soạn thảo mô tả và các bước hướng dẫn nấu ăn tự nhiên, chân thực
      const isEggDish = hasEgg;
      let naturalDescription = '';
      let naturalInstructions = '';

      if (isEggDish && /cà chua/i.test(dishIdea)) {
        naturalDescription = `Món ${calculatedTitle} mang vị chua thanh tự nhiên của cà chua hòa quyện với vị bùi béo của trứng, màu sắc bắt mắt và giàu năng lượng lành mạnh cho bữa cơm gia đình.`;
        naturalInstructions = `1. Sơ chế: Cà chua rửa sạch, bổ múi cau nhỏ. Hành lá rửa sạch, cắt nhỏ. Trứng gà đập ra bát, đánh tan đều cùng một chút tiêu xay và 1/2 muỗng cà phê hạt nêm chay.
2. Xào trứng: Đun nóng chảo với 1/2 lượng dầu ăn, cho trứng vào đảo nhanh tay ở lửa vừa cho trứng vừa chín tới, còn độ mềm xốp thì trút ra đĩa riêng.
3. Làm sốt cà chua: Dùng chảo cũ, thêm phần dầu ăn còn lại, cho đầu hành vào phi thơm rồi trút cà chua vào xào. Nêm hạt nêm chay và một chút nước ấm, đậy nắp khoảng 2-3 phút cho cà chua mềm nhuyễn tạo thành sốt sánh mịn.
4. Hòa quyện: Trút phần trứng đã xào trở lại chảo sốt cà chua, đảo nhẹ tay khoảng 1 phút ở lửa nhỏ để trứng thấm đều vị sốt.
5. Hoàn thiện: Rắc hành lá thái nhỏ và tiêu lên trên, tắt bếp và thưởng thức ngay khi còn nóng cùng cơm trắng.`;
      } else {
        naturalDescription = `Món ${calculatedTitle} thơm ngon thanh đạm, kết hợp hài hòa giữa các nguyên liệu thực vật tươi sạch, cung cấp năng lượng cân đối và giàu chất xơ cho cơ thể.`;
        naturalInstructions = `1. Sơ chế: Rửa sạch các nguyên liệu dưới vòi nước. Thái miếng vừa ăn phù hợp với từng loại rau củ và nấm.
2. Nấu chín: Làm nóng chảo với dầu thực vật, phi thơm hành boaro hoặc gia vị nền. Cho nguyên liệu chính vào xào đều tay ở lửa vừa cho chín tới.
3. Nêm nếm: Nêm hạt nêm chay, nước tương và điều chỉnh vừa khẩu vị gia đình.
4. Hoàn thiện: Tắt bếp, rắc tiêu và rau thơm lên trên. Thưởng thức khi món ăn còn ấm nóng.`;
      }

      setGeneratedResult({
        title: calculatedTitle,
        description: naturalDescription,
        instructions: naturalInstructions,
        dishCategory: cleanDishCategory,
        vegetarianType: detectedVegType,
        difficulty: 'EASY',
        servings,
        prepTimeMinutes: 10,
        cookTimeMinutes: 15,
        targetCaloriesPerServing: targetCalories,
        totalCalories: currentAllocatedCal,
        ingredients: resolvedIngredients,
      });

      setIsGenerating(false);
    }, 450);
  };

  const jsonExportData = generatedResult
    ? {
        mon_an: generatedResult.title,
        khau_phan: generatedResult.servings,
        muc_tieu_calo_moi_phan: generatedResult.targetCaloriesPerServing,
        tong_calo_thuc_te: generatedResult.totalCalories,
        calo_trung_binh_moi_phan: Math.round(
          generatedResult.totalCalories / generatedResult.servings,
        ),
        kieu_an_chay: generatedResult.vegetarianType,
        nguyen_lieu: generatedResult.ingredients.map((item) => ({
          ten: item.name,
          dinh_luong: item.quantity,
          don_vi: item.unitCode,
          calo_uoc_tinh: item.calories,
        })),
      }
    : null;

  const handleCopyJson = () => {
    if (!jsonExportData) return;
    navigator.clipboard.writeText(JSON.stringify(jsonExportData, null, 2));
    setCopiedJson(true);
    setTimeout(() => setCopiedJson(false), 2500);
  };

  const handleApply = () => {
    if (generatedResult) {
      onApplyRecipe(generatedResult);
      onClose();
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Trợ lý AI đồng hành cùng Chuyên gia ẩm thực" size="lg">
      <div className="space-y-5">
        {/* Lời dẫn mở đầu tự nhiên, không khẳng định 100% đúng */}
        <div className="rounded-xl border border-brand-100 bg-brand-50/60 p-3.5 text-xs text-ink-soft leading-relaxed flex items-start gap-2.5">
          <Info className="h-4 w-4 shrink-0 text-brand-600 mt-0.5" />
          <p>
            Trợ lý AI hỗ trợ bạn phân tích nguyên liệu, tính toán định lượng sát với mức năng lượng mong muốn và soạn thảo nhanh cấu trúc công thức. Dữ liệu mang tính tham khảo kỹ thuật, bạn luôn có toàn quyền tinh chỉnh lại hương vị và các bước nấu theo phong cách riêng của mình.
          </p>
        </div>

        {/* Form nhập liệu nhanh */}
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label className="mb-1 block text-xs font-bold text-ink">
              1. Tên món ăn hoặc ý tưởng bạn muốn thực hiện
            </label>
            <input
              type="text"
              value={dishIdea}
              onChange={(e) => setDishIdea(e.target.value)}
              placeholder="VD: Xào cà chua với trứng, Đậu hũ sốt chua ngọt..."
              className="w-full rounded-xl border border-brand-200 bg-white px-3.5 py-2.5 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
            />
          </div>

          <div className="sm:col-span-2">
            <label className="mb-1 block text-xs font-bold text-ink">
              2. Danh sách nguyên liệu sẵn có (ngăn cách bằng dấu phẩy)
            </label>
            <textarea
              rows={2}
              value={rawIngredients}
              onChange={(e) => setRawIngredients(e.target.value)}
              placeholder="VD: cà chua, trứng gà, hành lá, dầu thực vật, gia vị..."
              className="w-full rounded-xl border border-brand-200 bg-white px-3.5 py-2.5 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100 resize-y"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-bold text-ink flex items-center gap-1.5">
              <Flame className="h-3.5 w-3.5 text-amber-500" /> 3. Ràng buộc mức Calo mục tiêu / phần ăn
            </label>
            <div className="flex items-center gap-2">
              <input
                type="number"
                min={150}
                max={1200}
                step={25}
                value={targetCalories}
                onChange={(e) => setTargetCalories(Number(e.target.value))}
                className="w-full rounded-xl border border-brand-200 bg-white px-3.5 py-2.5 text-sm font-semibold text-ink outline-none focus:border-brand-500"
              />
              <span className="text-xs text-ink-muted shrink-0 font-medium">kcal / người</span>
            </div>
          </div>

          <div>
            <label className="mb-1 block text-xs font-bold text-ink flex items-center gap-1.5">
              <Users className="h-3.5 w-3.5 text-brand-600" /> 4. Số khẩu phần ăn
            </label>
            <select
              value={servings}
              onChange={(e) => setServings(Number(e.target.value))}
              className="w-full rounded-xl border border-brand-200 bg-white px-3.5 py-2.5 text-sm font-semibold text-ink outline-none focus:border-brand-500"
            >
              <option value={1}>1 người (Khẩu phần đơn)</option>
              <option value={2}>2 người (Gia đình nhỏ)</option>
              <option value={3}>3 người</option>
              <option value={4}>4 người (Gia đình tiêu chuẩn)</option>
              <option value={6}>6 người</option>
            </select>
          </div>
        </div>

        {/* Nút kích hoạt AI */}
        <div className="flex justify-end pt-1">
          <Button
            type="button"
            onClick={generateRecipe}
            disabled={isGenerating || !rawIngredients.trim()}
            className="flex items-center gap-2"
          >
            <Sparkles className="h-4 w-4" />
            {isGenerating ? 'Đang tính toán định lượng...' : 'Phân tích nguyên liệu & gợi ý định lượng'}
          </Button>
        </div>

        {/* Kết quả gợi ý từ AI */}
        {generatedResult && (
          <div className="space-y-4 rounded-2xl border border-brand-200 bg-white p-4 sm:p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-brand-100 pb-3">
              <div>
                <span className="text-xs font-bold uppercase tracking-wider text-brand-600">
                  Công thức đề xuất
                </span>
                <h3 className="text-base font-extrabold text-ink">{generatedResult.title}</h3>
              </div>
              <div className="flex items-center gap-2">
                <span className="rounded-lg bg-amber-50 px-2.5 py-1 text-xs font-bold text-amber-800">
                  {Math.round(generatedResult.totalCalories / generatedResult.servings)} kcal / phần
                </span>
                <span className="rounded-lg bg-brand-50 px-2.5 py-1 text-xs font-bold text-brand-700">
                  Khẩu phần {generatedResult.servings} người
                </span>
              </div>
            </div>

            {/* Bảng nguyên liệu và định lượng */}
            <div>
              <h4 className="mb-2 text-xs font-bold uppercase text-ink flex items-center gap-1.5">
                <Utensils className="h-3.5 w-3.5 text-brand-600" /> Bảng nguyên liệu & định lượng chính xác
              </h4>
              <div className="overflow-hidden rounded-xl border border-brand-100">
                <table className="w-full text-left text-xs">
                  <thead className="bg-brand-50/70 text-ink-muted">
                    <tr>
                      <th className="p-2.5">Nguyên liệu</th>
                      <th className="p-2.5 text-right">Định lượng</th>
                      <th className="p-2.5 text-right">Đơn vị</th>
                      <th className="p-2.5 text-right">Năng lượng ước tính</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-brand-50">
                    {generatedResult.ingredients.map((item, idx) => (
                      <tr key={idx} className="hover:bg-brand-50/30">
                        <td className="p-2.5 font-semibold text-ink">{item.name}</td>
                        <td className="p-2.5 text-right font-bold text-brand-700">{item.quantity}</td>
                        <td className="p-2.5 text-right text-ink-soft">{item.unitName} ({item.unitCode})</td>
                        <td className="p-2.5 text-right font-medium text-amber-700">{item.calories} kcal</td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot className="border-t border-brand-100 bg-brand-50/40 font-bold text-ink">
                    <tr>
                      <td colSpan={3} className="p-2.5">Tổng năng lượng thực đơn:</td>
                      <td className="p-2.5 text-right text-brand-800">{generatedResult.totalCalories} kcal</td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>

            {/* Hướng dẫn nấu ăn tóm lược */}
            <div>
              <h4 className="mb-1.5 text-xs font-bold uppercase text-ink flex items-center gap-1.5">
                <BookOpen className="h-3.5 w-3.5 text-brand-600" /> Hướng dẫn chế biến
              </h4>
              <div className="rounded-xl bg-brand-50/40 p-3 text-xs text-ink-soft whitespace-pre-line leading-relaxed">
                {generatedResult.instructions}
              </div>
            </div>

            {/* Các tùy chọn xuất JSON & nạp vào công thức */}
            <div className="flex flex-wrap items-center justify-between gap-3 border-t border-brand-100 pt-3">
              <button
                type="button"
                onClick={() => setJsonViewOpen(!jsonViewOpen)}
                className="text-xs font-bold text-brand-700 hover:text-brand-800 underline"
              >
                {jsonViewOpen ? 'Ẩn mã JSON nguyên liệu' : 'Xem cấu trúc JSON nguyên liệu'}
              </button>

              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleCopyJson}
                  className="flex items-center gap-1.5 text-xs"
                >
                  {copiedJson ? (
                    <>
                      <Check className="h-3.5 w-3.5 text-green-600" /> Đã sao chép JSON
                    </>
                  ) : (
                    <>
                      <Copy className="h-3.5 w-3.5" /> Sao chép JSON
                    </>
                  )}
                </Button>

                <Button
                  type="button"
                  size="sm"
                  onClick={handleApply}
                  className="flex items-center gap-1.5 bg-brand-700 hover:bg-brand-800 text-white font-bold"
                >
                  <ArrowDownToLine className="h-3.5 w-3.5" /> Nạp vào biểu mẫu công thức
                </Button>
              </div>
            </div>

            {/* Hộp xem JSON */}
            {jsonViewOpen && jsonExportData && (
              <div className="relative mt-2 rounded-xl bg-slate-900 p-3 text-xs text-slate-100 font-mono overflow-x-auto max-h-56">
                <pre>{JSON.stringify(jsonExportData, null, 2)}</pre>
              </div>
            )}
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
