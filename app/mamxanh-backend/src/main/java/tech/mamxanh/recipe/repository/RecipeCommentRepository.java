package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.recipe.entity.RecipeCommentEntity;

import java.util.Collection;
import java.util.List;

/**
 * FR-46 thread queries. A root thread is visible while at least one of its comments is not deleted;
 * a fully deleted thread disappears (AF-46.3).
 */
public interface RecipeCommentRepository extends JpaRepository<RecipeCommentEntity, Long> {

    long countByRecipeIdAndDeletedFalse(Long recipeId);

    @Query(value = """
            WITH thread AS (
                SELECT c.comment_id, c.comment_id AS root_id, c.is_deleted
                FROM [COMMENT] c
                WHERE c.recipe_id = :recipeId AND c.parent_comment_id IS NULL
                UNION ALL
                SELECT child.comment_id, thread.root_id, child.is_deleted
                FROM [COMMENT] child JOIN thread ON child.parent_comment_id = thread.comment_id
            )
            SELECT root.comment_id
            FROM [COMMENT] root
            WHERE root.comment_id IN (SELECT root_id FROM thread WHERE is_deleted = 0)
            ORDER BY root.created_at DESC, root.comment_id DESC
            OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
            """, nativeQuery = true)
    List<Long> findVisibleRootIds(@Param("recipeId") long recipeId, @Param("offset") long offset,
            @Param("size") int size);

    @Query(value = """
            WITH thread AS (
                SELECT c.comment_id, c.comment_id AS root_id, c.is_deleted
                FROM [COMMENT] c
                WHERE c.recipe_id = :recipeId AND c.parent_comment_id IS NULL
                UNION ALL
                SELECT child.comment_id, thread.root_id, child.is_deleted
                FROM [COMMENT] child JOIN thread ON child.parent_comment_id = thread.comment_id
            )
            SELECT COUNT(DISTINCT root_id) FROM thread WHERE is_deleted = 0
            """, nativeQuery = true)
    long countVisibleRoots(@Param("recipeId") long recipeId);

    /** Every comment of the given root threads; at most five levels deep. */
    @Query(value = """
            WITH thread AS (
                SELECT c.* FROM [COMMENT] c WHERE c.comment_id IN (:rootIds)
                UNION ALL
                SELECT child.* FROM [COMMENT] child JOIN thread ON child.parent_comment_id = thread.comment_id
            )
            SELECT * FROM thread
            """, nativeQuery = true)
    List<RecipeCommentEntity> findThreads(@Param("rootIds") Collection<Long> rootIds);
}
