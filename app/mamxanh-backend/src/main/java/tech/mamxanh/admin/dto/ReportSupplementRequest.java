package tech.mamxanh.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportSupplementRequest(
        @Schema(description = "Thông tin bổ sung, từ 10 đến 500 ký tự sau khi bỏ khoảng trắng đầu và cuối.")
        @NotBlank(message = "Nhập nội dung bổ sung từ 10 đến 500 ký tự.")
        @Size(min = 10, max = 500, message = "Nhập nội dung bổ sung từ 10 đến 500 ký tự.")
        String content) {
    public ReportSupplementRequest {
        if (content != null) content = content.trim();
    }
}
