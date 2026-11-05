package tech.mamxanh.recipe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ExecutionMode;
import tech.mamxanh.recipe.dto.response.AiChatResponse.AssistantStep;
import tech.mamxanh.recipe.dto.response.AiChatResponse.PendingConfirmation;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates model output against actual tool execution results to prevent
 * hallucinated successes when tools failed, were skipped, or are awaiting confirmation.
 */
@Component
public class ExecutionTruthfulnessVerifier {

    private static final Pattern SUCCESS_CLAIM = Pattern.compile(
            "(?iuU)\\b(?:đã\\s+(?:tạo|lập|thêm|xóa|sửa|cập nhật|đổi|lưu|áp dụng)|thành công|hoàn tất|successfully)\\b");

    private static final Pattern FAILURE_ACKNOWLEDGEMENT = Pattern.compile(
            "(?iuU)\\b(?:không|chưa|chẳng|thất bại|không thể|gặp lỗi|lỗi)\\s+(?:thành công|hoàn tất|thực hiện|lưu|tạo|thêm|xóa|sửa|cập nhật|áp dụng)\\b");

    @Value("${mamxanh.ai.verifier.enabled:true}")
    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String verify(String modelAnswer, ExecutionMode mode, List<AssistantStep> steps, PendingConfirmation pendingConfirmation) {
        if (!enabled || steps == null || steps.isEmpty()) {
            return modelAnswer;
        }

        boolean hasFailed = steps.stream().anyMatch(s -> "FAILED".equals(s.status()) || "ERROR".equals(s.status()));
        boolean hasSkippedMode = steps.stream().anyMatch(s -> "SKIPPED_MODE".equals(s.status()));
        boolean hasAwaiting = pendingConfirmation != null || steps.stream().anyMatch(s -> "AWAITING_CONFIRMATION".equals(s.status()));
        boolean claimsSuccess = modelAnswer != null
                && SUCCESS_CLAIM.matcher(modelAnswer).find()
                && !FAILURE_ACKNOWLEDGEMENT.matcher(modelAnswer).find();

        // 1. Tool failed but model claimed success
        if (hasFailed && claimsSuccess) {
            var failedStep = steps.stream()
                    .filter(s -> "FAILED".equals(s.status()) || "ERROR".equals(s.status()))
                    .findFirst()
                    .orElse(steps.getFirst());
            var succeededSteps = steps.stream().filter(s -> "SUCCEEDED".equals(s.status())).toList();
            if (!succeededSteps.isEmpty()) {
                return String.format("Thao tác một phần: bước '%s' đã hoàn thành, nhưng bước '%s' không thành công (%s). Vui lòng thử lại.",
                        succeededSteps.getFirst().summary(), failedStep.tool(), failedStep.summary());
            }
            return String.format("Thao tác '%s' không thành công do lỗi: %s. Vui lòng kiểm tra lại yêu cầu.",
                    failedStep.tool(), failedStep.summary());
        }

        // 2. Awaiting confirmation but model claimed it was executed
        if (hasAwaiting && claimsSuccess) {
            String summary = pendingConfirmation != null ? pendingConfirmation.summary() : "thao tác";
            return String.format("Thao tác '%s' đang chờ bạn xác nhận. Hệ thống chưa thực hiện thay đổi nào trên tài khoản.", summary);
        }

        // 3. QA mode (read-only preview) but model claimed it wrote data
        if (mode == ExecutionMode.QA && hasSkippedMode && claimsSuccess) {
            return "Ở chế độ Hỏi đáp (QA), Mâm Xanh chỉ xem trước và không lưu dữ liệu vào hệ thống. Hãy chuyển sang chế độ Có xác nhận (CONFIRM) hoặc Tự động (AUTO) nếu bạn muốn thực hiện.";
        }

        return modelAnswer;
    }
}
