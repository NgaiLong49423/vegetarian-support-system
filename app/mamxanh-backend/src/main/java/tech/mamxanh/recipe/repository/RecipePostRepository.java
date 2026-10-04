package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.recipe.entity.RecipePostEntity;

public interface RecipePostRepository extends JpaRepository<RecipePostEntity, Long> {
}
