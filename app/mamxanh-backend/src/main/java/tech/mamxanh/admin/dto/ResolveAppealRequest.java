package tech.mamxanh.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResolveAppealRequest(
        @Schema(allowableValues = {"RESTORE", "KEEP_HIDDEN", "ALLOW_EDIT"})
        @NotBlank(message = "Chọn một kết luận cho khiếu nại.")
        @Pattern(regexp = "RESTORE|KEEP_HIDDEN|ALLOW_EDIT", message = "Chọn một kết luận cho khiếu nại.")
        String decision,
        @Schema(description = "Lý do kết luận, từ 10 đến 500 ký tự sau khi bỏ khoảng trắng đầu và cuối.")
        @NotBlank(message = "Nhập lý do kết luận từ 10 đến 500 ký tự.")
        @Size(min = 10, max = 500, message = "Nhập lý do kết luận từ 10 đến 500 ký tự.")
        String reason) {
    public ResolveAppealRequest {
        if (reason != null) reason = reason.trim();
    }
}
