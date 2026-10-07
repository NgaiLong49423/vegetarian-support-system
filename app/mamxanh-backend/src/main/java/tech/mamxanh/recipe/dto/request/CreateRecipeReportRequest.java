package tech.mamxanh.recipe.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request data validated by FR-27 before the report submission flow handles it. */
@ValidRecipeReport
public record CreateRecipeReportRequest(
        @NotNull(message = "Vui lòng chọn một lý do báo cáo hợp lệ.") RecipeReportReasonCode reasonCode,
        @Size(max = 500, message = "Mô tả bổ sung không được vượt quá 500 ký tự.")
        @Pattern(regexp = "^[^<>]*$", message = "Mô tả chỉ nhận văn bản thuần, không nhận thẻ HTML.")
        String description) {

    public CreateRecipeReportRequest {
        if (description != null) {
            description = description.strip();
        }
    }
}
