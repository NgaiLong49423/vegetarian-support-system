package tech.mamxanh.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code PUT /api/v1/comments/{commentId}}: new content of the caller's own comment (AF-46.1). */
public record CommentUpdateRequest(
        @NotBlank(message = "Bình luận cần từ 1 đến 1.000 ký tự.")
        @Size(max = 1000, message = "Bình luận cần từ 1 đến 1.000 ký tự.")
        @Schema(description = "Văn bản thuần, 1–1.000 ký tự sau khi bỏ khoảng trắng đầu cuối.")
        String content) {

    public CommentUpdateRequest {
        content = content == null ? null : content.strip();
    }
}
