package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.recipe.entity.RecipeMediaEntity;

public interface RecipeMediaRepository extends JpaRepository<RecipeMediaEntity, Long> {
    java.util.List<RecipeMediaEntity> findAllByRecipeIdOrderByDisplayOrderAsc(Long recipeId);
}
