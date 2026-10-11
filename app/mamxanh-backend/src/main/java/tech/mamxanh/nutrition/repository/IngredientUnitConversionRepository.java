package tech.mamxanh.nutrition.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionId;

public interface IngredientUnitConversionRepository extends JpaRepository<IngredientUnitConversionEntity, IngredientUnitConversionId> {
    Optional<IngredientUnitConversionEntity> findByIdAndActiveTrue(IngredientUnitConversionId id);
    List<IngredientUnitConversionEntity> findByIdIngredientId(Long ingredientId);
    List<IngredientUnitConversionEntity> findByIdIngredientIdInAndActiveTrue(Collection<Long> ingredientIds);
    boolean existsByIdUnitId(int unitId);

    @Modifying
    @Query(value = """
            INSERT INTO INGREDIENT_UNIT_CONVERSION
                (ingredient_id, unit_id, grams_per_unit, is_approximate, is_active)
            VALUES (:ingredientId, :unitId, :gramsPerUnit, :approximate, 1)
            """, nativeQuery = true)
    int insertNew(@Param("ingredientId") long ingredientId,
                  @Param("unitId") int unitId,
                  @Param("gramsPerUnit") BigDecimal gramsPerUnit,
                  @Param("approximate") boolean approximate);
}
