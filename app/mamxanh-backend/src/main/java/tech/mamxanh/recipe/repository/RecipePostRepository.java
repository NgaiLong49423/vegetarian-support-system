package tech.mamxanh.recipe.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.recipe.entity.RecipePostEntity;

import java.util.List;

public interface RecipePostRepository extends JpaRepository<RecipePostEntity, Long> {
    java.util.Optional<RecipePostEntity> findByIdAndStatus(Long id, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecipePostEntity r where r.id = :id")
    java.util.Optional<RecipePostEntity> lockById(@Param("id") long id);

    /**
     * BR-27: an Administrator answered the appeal on this post's latest hide decision with ALLOW_EDIT. An older
     * hide decision's appeal never unlocks editing, so a later hide always starts locked.
     */
    @Query(value = "SELECT CASE WHEN (SELECT TOP 1 appeal_status FROM [REPORT] WHERE recipe_id = :id "
            + "AND decision = 'HIDE_RECIPE' ORDER BY handled_at DESC, report_id DESC) = 'EDIT_ALLOWED' "
            + "THEN 1 ELSE 0 END", nativeQuery = true)
    int editAllowedByAppeal(@Param("id") long id);

    Page<RecipePostEntity> findAllByStatusAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
            String status, String title, Pageable pageable);

    Page<RecipePostEntity> findAllByAuthorIdAndStatusInOrderByUpdatedAtDesc(
            Long authorId, List<String> statuses, Pageable pageable);

    @Query("select recipe from RecipePostEntity recipe where exists "
            + "(select ingredient.id from RecipeIngredientEntity ingredient "
            + "where ingredient.recipeId = recipe.id and ingredient.ingredientId = :ingredientId)")
    Page<RecipePostEntity> findAllUsingIngredient(@Param("ingredientId") long ingredientId, Pageable pageable);

    @Query("select case when count(recipe) > 0 then true else false end from RecipePostEntity recipe "
            + "where recipe.status = 'PUBLISHED' and exists "
            + "(select ingredient.id from RecipeIngredientEntity ingredient "
            + "where ingredient.recipeId = recipe.id and ingredient.ingredientId = :ingredientId)")
    boolean existsPublishedUsingIngredient(@Param("ingredientId") long ingredientId);

}
