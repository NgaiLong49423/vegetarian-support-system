package tech.mamxanh.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;

/** An appeal in the Administrator queue; {@code recipe} is filled on the detail read only. */
public record AdminAppealResponse(
        RecipeAppealResponse appeal,
        AppealAuthor author,
        @Schema(description = "Nội dung bài đang ẩn; chỉ có ở API chi tiết.", nullable = true) RecipePostResponse recipe) {

    public record AppealAuthor(long id, String displayName, String email, String accountStatus) { }
}
