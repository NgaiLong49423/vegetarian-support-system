package tech.mamxanh.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import tech.mamxanh.admin.entity.AppealStatus;
import tech.mamxanh.admin.entity.RecipeReportEntity;

/**
 * The author's view of the hide decision on their Recipe Post and of their appeal. It never carries the
 * reporter or the report description (BR-28).
 */
public record RecipeAppealResponse(
        long reportId,
        long recipeId,
        String recipeTitle,
        @Schema(description = "Lý do Quản trị viên ẩn bài.") String hideReason,
        LocalDateTime hiddenAt,
        @Schema(description = "Trạng thái khiếu nại; null khi tác giả chưa khiếu nại.", nullable = true)
        AppealStatus appealStatus,
        String appealMessage,
        LocalDateTime appealedAt,
        LocalDateTime resubmittedAt,
        @Schema(description = "Lý do của kết luận khiếu nại gần nhất.", nullable = true) String adminReason,
        LocalDateTime decidedAt) {

    public static RecipeAppealResponse of(RecipeReportEntity report, String recipeTitle) {
        return new RecipeAppealResponse(report.getId(), report.getRecipeId(), recipeTitle, report.getDecisionReason(),
                report.getHandledAt(), report.getAppealStatus(), report.getAppealMessage(), report.getAppealedAt(),
                report.getAppealResubmittedAt(), report.getAppealDecisionReason(), report.getAppealHandledAt());
    }
}
