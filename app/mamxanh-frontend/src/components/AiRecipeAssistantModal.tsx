import type { RecipeAiContentType } from '../api/recipes';
import { Modal } from './Modal';
import { RecipeAiAssistance } from './RecipeAiAssistance';
import { Button } from './ui';

interface AiRecipeAssistantModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  description: string;
  instructions: string;
  loading: RecipeAiContentType | null;
  error: { contentType: RecipeAiContentType; message: string; upgradeRequired?: boolean } | null;
  onGenerate: (contentType: RecipeAiContentType) => void;
}

/** FR-21 uses the selected ingredients and the same server-guarded actions as inline assistance. */
export function AiRecipeAssistantModal({ open, onClose, title, description, instructions,
  loading, error, onGenerate }: AiRecipeAssistantModalProps) {
  return (
    <Modal open={open} onClose={onClose} title="Trợ lý AI hỗ trợ soạn nội dung" size="lg">
      <div className="space-y-5">
        <p className="text-sm leading-6 text-ink-soft">
          Nhập tên món, danh mục, loại chay, khẩu phần và chọn nguyên liệu trong biểu mẫu trước khi yêu cầu AI.
          AI viết từ các nguyên liệu bạn đã chọn, giữ nguyên định lượng và không tự thêm nguyên liệu.
          Tính năng dành cho Chuyên gia có gói Plus hoặc Pro còn hiệu lực.
        </p>
        <p className="rounded-lg bg-brand-50 p-3 text-sm text-ink">
          <strong>Món đang soạn:</strong> {title.trim() || 'Chưa nhập tên món'}
        </p>
        {(['DESCRIPTION', 'INSTRUCTIONS'] as const).map((contentType) => (
          <section key={contentType} className="min-w-0 space-y-2">
            <h4 className="font-semibold text-ink">{contentType === 'DESCRIPTION' ? 'Mô tả' : 'Hướng dẫn chế biến'}</h4>
            <RecipeAiAssistance
              contentType={contentType}
              loading={loading}
              error={error?.contentType === contentType ? error.message : ''}
              upgradeRequired={error?.contentType === contentType && error.upgradeRequired}
              onGenerate={onGenerate}
            />
            {(contentType === 'DESCRIPTION' ? description : instructions) && (
              <p className="max-h-48 overflow-y-auto whitespace-pre-wrap break-words rounded-lg border border-brand-100 bg-raised p-3 text-sm leading-6 text-ink-soft">
                {contentType === 'DESCRIPTION' ? description : instructions}
              </p>
            )}
          </section>
        ))}
        <p className="text-sm leading-6 text-ink-muted">
          Nội dung được điền vào biểu mẫu để bạn kiểm tra và chỉnh sửa. Câu trả lời AI chỉ mang tính tham khảo;
          công thức chỉ được đăng khi bạn chủ động xuất bản.
        </p>
        <div className="flex justify-end">
          <Button type="button" variant="outline" onClick={onClose}>Trở lại biểu mẫu</Button>
        </div>
      </div>
    </Modal>
  );
}
