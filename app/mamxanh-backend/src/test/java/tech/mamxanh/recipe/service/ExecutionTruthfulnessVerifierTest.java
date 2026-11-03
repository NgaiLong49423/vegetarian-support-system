package tech.mamxanh.recipe.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ExecutionMode;
import tech.mamxanh.recipe.dto.response.AiChatResponse.AssistantStep;
import tech.mamxanh.recipe.dto.response.AiChatResponse.PendingConfirmation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionTruthfulnessVerifierTest {

    private ExecutionTruthfulnessVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new ExecutionTruthfulnessVerifier();
    }

    @Test
    void whenToolFailsAndModelClaimsSuccess_rewritesToTruthfulFailureMessage() {
        List<AssistantStep> steps = List.of(
                new AssistantStep(1, "create_shopping_list", "FAILED", "Database connection timed out", Map.of())
        );

        String modelHallucination = "Tôi đã tạo thành công danh sách mua sắm cho bạn!";
        String verified = verifier.verify(modelHallucination, ExecutionMode.AUTO, steps, null);

        assertThat(verified).contains("không thành công do lỗi: Database connection timed out");
        assertThat(verified).doesNotContain("đã tạo thành công");
    }

    @Test
    void whenToolAwaitingConfirmationAndModelClaimsExecuted_rewritesToAwaitingConfirmationMessage() {
        PendingConfirmation pending = new PendingConfirmation(
                UUID.randomUUID(),
                "Thêm 3 món vào lịch ăn tuần này",
                Instant.now().plusSeconds(300),
                true,
                "add_meal_plan_slot"
        );
        List<AssistantStep> steps = List.of(
                new AssistantStep(1, "add_meal_plan_slot", "AWAITING_CONFIRMATION", "Cần xác nhận", Map.of())
        );

        String modelClaim = "Tôi đã thêm thành công 3 món vào lịch ăn của bạn!";
        String verified = verifier.verify(modelClaim, ExecutionMode.CONFIRM, steps, pending);

        assertThat(verified).contains("đang chờ bạn xác nhận");
        assertThat(verified).contains("Hệ thống chưa thực hiện thay đổi nào trên tài khoản");
    }

    @Test
    void whenQaModeAndModelClaimsWrite_rewritesToQaReadOnlyExplanation() {
        List<AssistantStep> steps = List.of(
                new AssistantStep(1, "create_shopping_list", "SKIPPED_MODE", "Chế độ QA không ghi dữ liệu", Map.of())
        );

        String modelClaim = "Tôi đã lưu thành công danh sách vào hệ thống.";
        String verified = verifier.verify(modelClaim, ExecutionMode.QA, steps, null);

        assertThat(verified).contains("Hỏi đáp (QA)");
        assertThat(verified).contains("CONFIRM");
        assertThat(verified).contains("AUTO");
    }

    @Test
    void whenPartialSuccessAndModelClaimsFullSuccess_rewritesToPartialSuccessMessage() {
        List<AssistantStep> steps = List.of(
                new AssistantStep(1, "search_public_recipes", "SUCCEEDED", "Tìm thấy 3 công thức đậu phụ", Map.of()),
                new AssistantStep(2, "add_meal_plan_slot", "FAILED", "Lỗi ràng buộc lịch ăn tuần", Map.of())
        );

        String modelClaim = "Tôi đã hoàn tất việc tìm kiếm và lên lịch ăn cho bạn!";
        String verified = verifier.verify(modelClaim, ExecutionMode.AUTO, steps, null);

        assertThat(verified).contains("Thao tác một phần");
        assertThat(verified).contains("Tìm thấy 3 công thức đậu phụ");
        assertThat(verified).contains("add_meal_plan_slot");
        assertThat(verified).contains("Lỗi ràng buộc lịch ăn tuần");
    }

    @Test
    void whenAllStepsSucceeded_preservesModelAnswerVerbatim() {
        List<AssistantStep> steps = List.of(
                new AssistantStep(1, "search_public_recipes", "SUCCEEDED", "Tìm thấy 2 công thức", Map.of())
        );

        String modelAnswer = "Dưới đây là 2 công thức món chay thanh đạm mà bạn có thể tham khảo.";
        String verified = verifier.verify(modelAnswer, ExecutionMode.AUTO, steps, null);

        assertThat(verified).isEqualTo(modelAnswer);
    }

    @Test
    void whenVerifierDisabled_preservesModelAnswerEvenIfClaimingSuccess() {
        verifier.setEnabled(false);
        List<AssistantStep> steps = List.of(
                new AssistantStep(1, "create_shopping_list", "FAILED", "Error", Map.of())
        );

        String hallucination = "Tôi đã tạo thành công danh sách.";
        String verified = verifier.verify(hallucination, ExecutionMode.AUTO, steps, null);

        assertThat(verified).isEqualTo(hallucination);
    }

    @Test
    void whenModelTruthfullyExplainsFailure_preservesModelAnswerVerbatim() {
        List<AssistantStep> steps = List.of(
                new AssistantStep(1, "add_meal_plan_slot", "FAILED", "Recipe not found", Map.of())
        );

        String truthfulModelAnswer = "Rất tiếc, thao tác thêm vào lịch ăn không thành công do công thức không tồn tại trên hệ thống.";
        String verified = verifier.verify(truthfulModelAnswer, ExecutionMode.AUTO, steps, null);

        assertThat(verified).isEqualTo(truthfulModelAnswer);
    }
}
