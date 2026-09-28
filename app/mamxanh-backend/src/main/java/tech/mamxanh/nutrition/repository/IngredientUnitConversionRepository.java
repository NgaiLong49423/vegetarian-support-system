package tech.mamxanh.nutrition.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionId;

public interface IngredientUnitConversionRepository extends JpaRepository<IngredientUnitConversionEntity, IngredientUnitConversionId> {
    Optional<IngredientUnitConversionEntity> findByIdAndActiveTrue(IngredientUnitConversionId id);
    List<IngredientUnitConversionEntity> findByIdIngredientId(Long ingredientId);
}
