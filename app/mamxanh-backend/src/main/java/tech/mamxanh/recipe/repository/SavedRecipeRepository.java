package tech.mamxanh.recipe.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.recipe.entity.SavedRecipeEntity;
import tech.mamxanh.recipe.entity.SavedRecipeId;

public interface SavedRecipeRepository extends JpaRepository<SavedRecipeEntity, SavedRecipeId> {
    @Query(value = """
            SELECT sr.recipe_id AS recipeId,
                   CASE WHEN rp.status = 'PUBLISHED' THEN rp.title ELSE NULL END AS title,
                   CASE WHEN rp.status = 'PUBLISHED' THEN rp.description ELSE NULL END AS description,
                   CASE WHEN rp.status = 'PUBLISHED' THEN media.blob_url ELSE NULL END AS coverUrl,
                   CASE WHEN rp.status = 'PUBLISHED' THEN u.display_name ELSE NULL END AS authorName,
                   CAST(CASE WHEN rp.status = 'PUBLISHED' THEN 1 ELSE 0 END AS bit) AS available,
                   sr.saved_at AS savedAt
            FROM SAVED_RECIPE sr
            LEFT JOIN RECIPE_POST rp ON rp.recipe_id = sr.recipe_id
            LEFT JOIN [USER] u ON u.user_id = rp.author_id
            OUTER APPLY (
                SELECT TOP 1 rm.blob_url FROM RECIPE_MEDIA rm
                WHERE rm.recipe_id = rp.recipe_id AND rm.is_cover = 1
                ORDER BY rm.display_order ASC
            ) media
            WHERE sr.user_id = :userId
              AND (:keyword = N'' OR (rp.status = 'PUBLISHED'
                   AND (CHARINDEX(:keyword, rp.title) > 0 OR CHARINDEX(:keyword, rp.description) > 0)))
            ORDER BY sr.saved_at DESC, sr.recipe_id DESC
            """, countQuery = """
            SELECT COUNT(*)
            FROM SAVED_RECIPE sr
            LEFT JOIN RECIPE_POST rp ON rp.recipe_id = sr.recipe_id
            WHERE sr.user_id = :userId
              AND (:keyword = N'' OR (rp.status = 'PUBLISHED'
                   AND (CHARINDEX(:keyword, rp.title) > 0 OR CHARINDEX(:keyword, rp.description) > 0)))
            """, nativeQuery = true)
    Page<SavedRecipeProjection> findSavedRecipeProjections(@Param("userId") long userId,
            @Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query(value = """
            INSERT INTO SAVED_RECIPE (user_id, recipe_id, saved_at)
            SELECT :userId, :recipeId, SYSUTCDATETIME()
            WHERE NOT EXISTS (
                SELECT 1 FROM SAVED_RECIPE WITH (UPDLOCK, HOLDLOCK)
                WHERE user_id = :userId AND recipe_id = :recipeId
            )
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") long userId, @Param("recipeId") long recipeId);

    @Modifying
    @Query("delete from SavedRecipeEntity saved where saved.id.userId = :userId and saved.id.recipeId = :recipeId")
    int deleteSavedRecipe(@Param("userId") long userId, @Param("recipeId") long recipeId);

    interface SavedRecipeProjection {
        Long getRecipeId();
        String getTitle();
        String getDescription();
        String getCoverUrl();
        String getAuthorName();
        Boolean getAvailable();
        java.time.LocalDateTime getSavedAt();
    }
}
