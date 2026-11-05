package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.recipe.entity.RecipePostEntity;

import java.util.Optional;

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

    @Query(value = """
            SELECT rp.recipe_id AS recipeId,
              (SELECT COUNT_BIG(*) FROM RECIPE_REACTION rr WHERE rr.recipe_id = rp.recipe_id AND rr.reaction_type = 'LIKE') AS likes,
              (SELECT COUNT_BIG(*) FROM RECIPE_REACTION rr WHERE rr.recipe_id = rp.recipe_id AND rr.reaction_type = 'DISLIKE') AS dislikes,
              (SELECT COUNT_BIG(*) FROM RECIPE_VIEW rv WHERE rv.recipe_id = rp.recipe_id) AS viewCount,
              (SELECT COUNT_BIG(*) FROM RECIPE_REACTION rr WHERE rr.recipe_id = rp.recipe_id) AS reactionCount
            FROM RECIPE_POST rp WHERE rp.recipe_id IN (:recipeIds)
            """, nativeQuery = true)
    java.util.List<RecipeStatisticsProjection> findAllStatistics(@Param("recipeIds") java.util.Collection<Long> recipeIds);

    @Query(value = """
            WITH reaction_totals AS (
              SELECT recipe_id, COUNT_BIG(*) AS totalLikes
              FROM RECIPE_REACTION
              WHERE reaction_type = 'LIKE'
              GROUP BY recipe_id
            ), view_totals AS (
              SELECT recipe_id, COUNT_BIG(*) AS totalViews
              FROM RECIPE_VIEW
              GROUP BY recipe_id
            )
            SELECT COUNT_BIG(*) AS recipeCount,
              COALESCE(SUM(COALESCE(rt.totalLikes, 0)), 0) AS totalLikes,
              COALESCE(SUM(COALESCE(vt.totalViews, 0)), 0) AS totalViews
            FROM RECIPE_POST rp
            LEFT JOIN reaction_totals rt ON rt.recipe_id = rp.recipe_id
            LEFT JOIN view_totals vt ON vt.recipe_id = rp.recipe_id
            WHERE rp.author_id = :authorId AND rp.status = 'PUBLISHED'
            """, nativeQuery = true)
    AuthorStatisticsProjection findPublishedAuthorStatistics(@Param("authorId") long authorId);

    interface AuthorStatisticsProjection {
        Long getRecipeCount();
        Long getTotalLikes();
        Long getTotalViews();
    }

    interface RecipeStatisticsProjection extends StatisticsProjection {
        Long getRecipeId();
    }

    interface StatisticsProjection {
        Long getLikes();
        Long getDislikes();
        Long getReactionCount();
        Long getViewCount();
    }
}
