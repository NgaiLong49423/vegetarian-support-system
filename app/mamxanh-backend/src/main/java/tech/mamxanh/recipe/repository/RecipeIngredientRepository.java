package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.recipe.entity.RecipeIngredientEntity;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredientEntity, Long> {
    java.util.List<RecipeIngredientEntity> findAllByRecipeIdOrderByIdAsc(Long recipeId);
}
