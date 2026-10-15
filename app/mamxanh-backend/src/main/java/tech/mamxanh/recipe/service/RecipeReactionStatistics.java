package tech.mamxanh.recipe.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** BR-69: the same displayed percentage for reaction, browse and detail responses. */
public final class RecipeReactionStatistics {
    private RecipeReactionStatistics() { }

    public static BigDecimal likePercentage(long likes, long dislikes) {
        BigDecimal total = BigDecimal.valueOf(likes).add(BigDecimal.valueOf(dislikes));
        return total.signum() == 0 ? null : BigDecimal.valueOf(likes)
                .multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP);
    }
}
