package tech.mamxanh.recipe.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tech.mamxanh.recipe.entity.RecipeReactionType;

import java.math.BigDecimal;

public record RecipeReactionResponse(
        long recipeId,
        @Schema(description = "Lựa chọn của Member hiện tại; null khi chưa bình chọn.", nullable = true)
        RecipeReactionType currentReaction,
        long likes,
        long dislikes,
        long reactionCount,
        @Schema(description = "Tỷ lệ Like làm tròn thành số nguyên; null khi chưa có bình chọn.", nullable = true)
        BigDecimal likePercentage) {
}
