package tech.mamxanh.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** {@code POST /api/v1/recipes/{recipeId}/comments}: a root comment, or a reply when {@code parentCommentId} is set. */
public record CommentCreateRequest(
        @NotBlank(message = "Bình luận cần từ 1 đến 1.000 ký tự.")
        @Size(max = 1000, message = "Bình luận cần từ 1 đến 1.000 ký tự.")
        @Schema(description = "Văn bản thuần, 1–1.000 ký tự sau khi bỏ khoảng trắng đầu cuối.")
        String content,

        @Positive
        @Schema(description = "Bình luận được trả lời; trả lời bình luận cấp 5 được gắn vào cha cấp 4 (AC-46.1).")
        Long parentCommentId) {

    public CommentCreateRequest {
        content = content == null ? null : content.strip();
    }
}
