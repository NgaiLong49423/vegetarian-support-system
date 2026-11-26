package tech.mamxanh.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecipeAppealRequest(
        @Schema(description = "Lời khiếu nại, từ 10 đến 1000 ký tự sau khi bỏ khoảng trắng đầu và cuối.")
        @NotBlank(message = "Nhập lời khiếu nại từ 10 đến 1000 ký tự.")
        @Size(min = 10, max = 1000, message = "Nhập lời khiếu nại từ 10 đến 1000 ký tự.")
        String message) {
    public RecipeAppealRequest {
        if (message != null) message = message.trim();
    }
}
