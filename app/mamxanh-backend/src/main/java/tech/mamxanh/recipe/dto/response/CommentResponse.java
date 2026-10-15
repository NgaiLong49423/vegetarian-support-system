package tech.mamxanh.recipe.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** One FR-46 comment with its visible replies, oldest first. */
public record CommentResponse(
        long commentId,
        Long parentCommentId,
        @Schema(description = "Cấp lồng, từ 1 đến 5.")
        int depth,
        @Schema(description = "Văn bản thuần; Frontend hiển thị như text, không render HTML. Null khi đã xóa.")
        String content,
        @Schema(description = "Null khi bình luận đã xóa (tombstone).")
        Author author,
        @Schema(description = "Đã bị tác giả xóa nhưng còn reply nên vẫn giữ vị trí (AC-46.2).")
        boolean deleted,
        @Schema(description = "Nội dung đã được sửa sau khi đăng.")
        boolean edited,
        LocalDateTime createdAt,
        List<CommentResponse> replies) {

    /** Public profile fields only (BR-18). */
    public record Author(long userId, String displayName, String avatarUrl) { }
}
