package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.recipe.entity.RecipeConversionEntity;
import tech.mamxanh.recipe.entity.RecipeConversionId;

public interface RecipeConversionRepository extends JpaRepository<RecipeConversionEntity, RecipeConversionId> {
    boolean existsByIngredientIdAndUnitIdAndActiveTrue(Long ingredientId, Integer unitId);
}
