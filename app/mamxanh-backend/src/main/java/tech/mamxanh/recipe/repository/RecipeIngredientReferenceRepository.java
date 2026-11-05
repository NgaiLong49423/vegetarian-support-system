package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.recipe.entity.RecipeIngredientReferenceEntity;

import java.util.List;

public interface RecipeIngredientReferenceRepository extends JpaRepository<RecipeIngredientReferenceEntity, Long> {
    List<RecipeIngredientReferenceEntity> findTop30ByStatusAndNameContainingIgnoreCaseOrderByNameAsc(String status, String name);
    List<RecipeIngredientReferenceEntity> findAllByIdInAndStatus(List<Long> ids, String status);
}
