package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.recipe.entity.RecipeReactionEntity;
import tech.mamxanh.recipe.entity.RecipeReactionId;

public interface RecipeReactionRepository extends JpaRepository<RecipeReactionEntity, RecipeReactionId> {
}
