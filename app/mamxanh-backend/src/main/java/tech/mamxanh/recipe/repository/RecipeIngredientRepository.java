package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.recipe.entity.RecipeIngredientEntity;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredientEntity, Long> {
    java.util.List<RecipeIngredientEntity> findAllByRecipeIdOrderByIdAsc(Long recipeId);
    java.util.List<RecipeIngredientEntity> findAllByRecipeIdInOrderByRecipeIdAscIdAsc(java.util.Collection<Long> recipeIds);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from RecipeIngredientEntity i where i.recipeId = :recipeId")
    void deleteAllByRecipeId(@org.springframework.data.repository.query.Param("recipeId") Long recipeId);
}
