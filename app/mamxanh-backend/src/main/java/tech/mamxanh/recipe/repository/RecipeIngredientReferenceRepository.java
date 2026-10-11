package tech.mamxanh.recipe.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.recipe.entity.RecipeIngredientReferenceEntity;

public interface RecipeIngredientReferenceRepository extends JpaRepository<RecipeIngredientReferenceEntity, Long> {
    List<RecipeIngredientReferenceEntity> findTop30ByStatusAndNameContainingIgnoreCaseOrderByNameAsc(String status, String name);
    List<RecipeIngredientReferenceEntity> findAllByIdInAndStatus(List<Long> ids, String status);
}
