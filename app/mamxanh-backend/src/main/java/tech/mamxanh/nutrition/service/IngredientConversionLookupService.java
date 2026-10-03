package tech.mamxanh.nutrition.service;

import java.math.BigDecimal;
import java.util.Optional;

public interface IngredientConversionLookupService {
    Optional<BigDecimal> findActiveGramsPerUnit(long ingredientId, int unitId);
}
