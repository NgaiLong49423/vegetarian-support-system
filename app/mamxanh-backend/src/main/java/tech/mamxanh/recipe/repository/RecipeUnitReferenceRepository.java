package tech.mamxanh.recipe.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.recipe.entity.RecipeUnitReferenceEntity;

public interface RecipeUnitReferenceRepository extends JpaRepository<RecipeUnitReferenceEntity, Integer> {
    List<RecipeUnitReferenceEntity> findAllByOrderByNameAsc();
}
