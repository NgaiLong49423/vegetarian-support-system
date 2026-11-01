package tech.mamxanh.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.recipe.entity.RecipeUnitReferenceEntity;

import java.util.List;

public interface RecipeUnitReferenceRepository extends JpaRepository<RecipeUnitReferenceEntity, Integer> {
    List<RecipeUnitReferenceEntity> findAllByOrderByNameAsc();
}
