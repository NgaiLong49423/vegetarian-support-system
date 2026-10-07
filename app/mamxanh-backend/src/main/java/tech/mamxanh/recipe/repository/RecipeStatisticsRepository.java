package tech.mamxanh.recipe.repository;

import java.util.Optional;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.recipe.entity.RecipePostEntity;

/** Read-only aggregate over the existing interaction tables. */
public interface RecipeStatisticsRepository extends Repository<RecipePostEntity, Long> {
    @Query(value = """
            SELECT
              (SELECT COUNT_BIG(*) FROM RECIPE_REACTION rr WHERE rr.recipe_id = rp.recipe_id AND rr.reaction_type = 'LIKE') AS likes,
              (SELECT COUNT_BIG(*) FROM RECIPE_REACTION rr WHERE rr.recipe_id = rp.recipe_id AND rr.reaction_type = 'DISLIKE') AS dislikes,
              (SELECT COUNT_BIG(*) FROM RECIPE_REACTION rr WHERE rr.recipe_id = rp.recipe_id) AS reactionCount,
              (SELECT COUNT_BIG(*) FROM RECIPE_VIEW rv WHERE rv.recipe_id = rp.recipe_id) AS viewCount
            FROM RECIPE_POST rp
            WHERE rp.recipe_id = :recipeId
            """, nativeQuery = true)
    Optional<StatisticsProjection> findStatistics(@Param("recipeId") long recipeId);

    interface StatisticsProjection {
        Long getLikes();
        Long getDislikes();
        Long getReactionCount();
        Long getViewCount();
    }
}
