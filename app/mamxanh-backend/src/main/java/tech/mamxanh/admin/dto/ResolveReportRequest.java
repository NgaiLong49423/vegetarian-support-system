package tech.mamxanh.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResolveReportRequest(
        @Schema(allowableValues = {"NO_VIOLATION", "WARN_AUTHOR", "HIDE_RECIPE", "LOCK_AUTHOR"})
        @NotBlank(message = "Chọn một kết luận xử lý.")
        @Pattern(regexp = "NO_VIOLATION|WARN_AUTHOR|HIDE_RECIPE|LOCK_AUTHOR", message = "Chọn một kết luận xử lý.")
        String decision,
        @Schema(description = "Lý do kết luận, từ 10 đến 500 ký tự sau khi bỏ khoảng trắng đầu và cuối.")
        @NotBlank(message = "Nhập lý do kết luận từ 10 đến 500 ký tự.")
        @Size(min = 10, max = 500, message = "Nhập lý do kết luận từ 10 đến 500 ký tự.")
        String reason) {
    public ResolveReportRequest {
        if (reason != null) reason = reason.trim();
    }
}
