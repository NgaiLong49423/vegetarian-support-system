package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.recipe.entity.RecipeAuthorReferenceEntity;

public interface RecipeAuthorReferenceRepository extends JpaRepository<RecipeAuthorReferenceEntity, Long> {
}
