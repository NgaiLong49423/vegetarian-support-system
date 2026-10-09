package tech.mamxanh.recipe.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.recipe.entity.SavedRecipeEntity;
import tech.mamxanh.recipe.entity.SavedRecipeId;

public interface SavedRecipeRepository extends JpaRepository<SavedRecipeEntity, SavedRecipeId> {
    @Query(value = """
            SELECT sr.recipe_id AS recipeId,
                   CASE WHEN rp.status = 'PUBLISHED' THEN rp.title ELSE NULL END AS title,
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
            ORDER BY sr.saved_at DESC, sr.recipe_id DESC
            """, countQuery = "SELECT COUNT(*) FROM SAVED_RECIPE WHERE user_id = :userId", nativeQuery = true)
    Page<SavedRecipeProjection> findSavedRecipeProjections(@Param("userId") long userId, Pageable pageable);

    interface SavedRecipeProjection {
        Long getRecipeId();
        String getTitle();
        String getCoverUrl();
        String getAuthorName();
        Boolean getAvailable();
        java.time.LocalDateTime getSavedAt();
    }
}
