package tech.mamxanh.nutrition.service;

import java.util.List;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.dto.response.UnitResponse;

/**
 * Read-only catalog boundary for recipe authoring. Only active records may be selected for new
 * recipe ingredient links; administrative screens use {@link IngredientCatalogService} instead.
 */
public interface IngredientCatalogLookupService {
    List<IngredientResponse> findActiveIngredients(String query);

    List<UnitResponse> findActiveUnits();
}
