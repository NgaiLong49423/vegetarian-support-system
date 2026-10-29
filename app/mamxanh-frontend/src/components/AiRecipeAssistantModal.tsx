import { useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, BookOpen, Lightbulb, LoaderCircle, Sparkles, Users, Utensils } from 'lucide-react';
import type { RecipeAiContentType, RecipeFormOptions } from '../api/recipes';
import { Modal } from './Modal';
import { Button } from './ui';

interface RecipeContext {
  title: string;
  dishCategory: string;
  vegetarianType: string;
  servings: string;
}

interface AiRecipeAssistantModalProps {
  open: boolean;
  onClose: () => void;
  context: RecipeContext;
  onContextChange: (patch: Partial<RecipeContext>) => void;
  options: RecipeFormOptions | null;
  ingredientFields: ReactNode;
  description: string;
  instructions: string;
  loading: RecipeAiContentType | null;
  error: { contentType: RecipeAiContentType; message: string; upgradeRequired?: boolean } | null;
  onGenerate: (contentType: RecipeAiContentType) => void;
}

const inputClass = 'w-full min-w-0 rounded-xl border border-brand-200 bg-raised px-3 py-2.5 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20';

/** Develop's form-oriented layout, with real FR-21 actions and catalog-backed ingredients. */
export function AiRecipeAssistantModal({ open, onClose, context, onContextChange, options,
  ingredientFields, description, instructions, loading, error, onGenerate }: AiRecipeAssistantModalProps) {
  const [contentType, setContentType] = useState<RecipeAiContentType>('DESCRIPTION');
  const actionLabel = contentType === 'DESCRIPTION' ? 'AI gợi ý viết mô tả' : 'AI gợi ý viết hướng dẫn chế biến';

  return (
    <Modal open={open} onClose={onClose} title="Trợ lý AI đồng hành cùng Chuyên gia ẩm thực" size="lg" trapFocus>
      <div className="space-y-5">
        <div className="flex items-start gap-3 rounded-xl border border-amber-300 bg-amber-50 p-4 text-sm text-amber-950">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          <div>
            <p className="font-bold">Khuyến nghị tham khảo – Chuyên gia ẩm thực giữ quyền quyết định</p>
            <p className="mt-1 leading-6">Trợ lý AI hỗ trợ viết nội dung từ nguyên liệu và định lượng bạn đã chọn. Bạn có toàn quyền kiểm tra, chỉnh sửa trước khi xuất bản. AI không tự thêm nguyên liệu hoặc thay đổi định lượng.</p>
          </div>
        </div>

        <fieldset disabled={loading !== null} className="min-w-0 space-y-4 disabled:opacity-70">
          <legend className="mb-3 flex items-center gap-2 text-sm font-bold text-ink">
            <Lightbulb className="h-4 w-4 text-brand-500" aria-hidden="true" /> Nội dung cần hỗ trợ
          </legend>
          <div className="grid grid-cols-2 gap-2">
            {(['DESCRIPTION', 'INSTRUCTIONS'] as const).map((type) => (
              <label key={type} className={`flex min-w-0 cursor-pointer items-center gap-2 rounded-xl border p-3 text-sm font-semibold transition-colors ${contentType === type ? 'border-brand-500 bg-brand-50 text-brand-700' : 'border-brand-100 bg-raised text-ink-soft hover:border-brand-300'}`}>
                <input type="radio" name="assistant-content-type" value={type} checked={contentType === type} onChange={() => setContentType(type)} className="accent-brand-600" />
                {type === 'DESCRIPTION' ? <BookOpen className="h-4 w-4 shrink-0" aria-hidden="true" /> : <Utensils className="h-4 w-4 shrink-0" aria-hidden="true" />}
                {type === 'DESCRIPTION' ? 'Mô tả món ăn' : 'Hướng dẫn chế biến'}
              </label>
            ))}
          </div>

          <div>
            <label htmlFor="assistant-title" className="mb-1 block text-sm font-semibold text-ink">Tên món hoặc ý tưởng chế biến *</label>
            <input id="assistant-title" value={context.title} onChange={(event) => onContextChange({ title: event.target.value })} maxLength={150} placeholder="Nhập tên món bạn muốn soạn" className={inputClass} />
            {!context.title.trim() && <p className="mt-1 text-xs text-ink-muted">Chưa nhập tên món</p>}
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="assistant-category" className="mb-1 block text-sm font-semibold text-ink">Thể loại món *</label>
              <select id="assistant-category" value={context.dishCategory} onChange={(event) => onContextChange({ dishCategory: event.target.value })} className={inputClass}>
                <option value="">Chọn thể loại món</option>
                {options?.dishCategories.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
              </select>
            </div>
            <div>
              <label htmlFor="assistant-vegetarian" className="mb-1 block text-sm font-semibold text-ink">Loại ăn chay *</label>
              <select id="assistant-vegetarian" value={context.vegetarianType} onChange={(event) => onContextChange({ vegetarianType: event.target.value })} className={inputClass}>
                <option value="">Chọn loại ăn chay</option>
                {options?.vegetarianTypes.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
              </select>
            </div>
          </div>

          <div>
            <label htmlFor="assistant-servings" className="mb-1 flex items-center gap-2 text-sm font-semibold text-ink"><Users className="h-4 w-4 text-brand-500" aria-hidden="true" /> Số khẩu phần ăn *</label>
            <input id="assistant-servings" type="number" min="1" max="50" step="1" value={context.servings} onChange={(event) => onContextChange({ servings: event.target.value })} className={inputClass} />
          </div>

          <section className="min-w-0 space-y-3 rounded-xl bg-raised p-3 sm:p-4" aria-label="Nguyên liệu trong trợ lý AI">
            <h4 className="text-sm font-bold text-ink">Nguyên liệu sẵn có và định lượng *</h4>
            {ingredientFields}
          </section>
        </fieldset>

        <div className="flex justify-end">
          <Button type="button" disabled={loading !== null} aria-label={actionLabel} aria-busy={loading !== null}
            onClick={() => onGenerate(contentType)} className="inline-flex w-full items-center justify-center gap-2 bg-gradient-to-r from-amber-500 to-orange-600 font-bold text-white hover:from-amber-600 hover:to-orange-700 sm:w-auto">
            {loading ? <LoaderCircle className="h-4 w-4 animate-spin" aria-hidden="true" /> : <Sparkles className="h-4 w-4" aria-hidden="true" />}
            {loading ? 'Đang soạn nội dung…' : actionLabel}
          </Button>
        </div>
        {loading && <p role="status" aria-live="polite" className="text-sm text-ink-muted">Đang chuẩn bị gợi ý nội dung, có thể mất vài giây...</p>}
        {error && <p role="alert" className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm leading-6 text-amber-950">
          {error.message}{error.upgradeRequired && <Link to="/goi-ai" className="ml-1 font-semibold underline">Xem các gói AI</Link>}
        </p>}

        {(description || instructions) && <section className="space-y-3 rounded-xl border border-brand-100 bg-raised p-4" aria-label="Nội dung AI trong biểu mẫu">
          <h4 className="font-bold text-ink">Nội dung trong biểu mẫu công thức</h4>
          {description && <div><p className="mb-1 text-sm font-semibold text-ink">Mô tả</p><p className="max-h-48 overflow-y-auto whitespace-pre-wrap break-words text-sm leading-6 text-ink-soft">{description}</p></div>}
          {instructions && <div><p className="mb-1 text-sm font-semibold text-ink">Hướng dẫn chế biến</p><p className="max-h-48 overflow-y-auto whitespace-pre-wrap break-words text-sm leading-6 text-ink-soft">{instructions}</p></div>}
          <p className="text-xs leading-5 text-ink-muted">Nội dung được điền vào biểu mẫu để bạn tiếp tục chỉnh sửa. Công thức chỉ được đăng khi bạn chủ động xuất bản.</p>
        </section>}
        <div className="flex justify-end border-t border-brand-100 pt-4">
          <Button type="button" variant="outline" aria-label="Đóng và trở lại biểu mẫu" onClick={onClose}>Đóng</Button>
        </div>
      </div>
    </Modal>
  );
}
