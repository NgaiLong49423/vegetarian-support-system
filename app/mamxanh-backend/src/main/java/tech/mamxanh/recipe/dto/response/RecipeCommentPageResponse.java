package tech.mamxanh.recipe.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** A page of root threads; {@code totalElements} counts visible threads. */
public record RecipeCommentPageResponse(
        List<CommentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        @Schema(description = "Số bình luận và reply chưa bị xóa của bài công thức.")
        long commentCount) {
}
