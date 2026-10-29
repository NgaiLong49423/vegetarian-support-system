package tech.mamxanh.nutrition.service;

import java.util.List;
import java.util.Collection;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.nutrition.dto.response.IngredientUnitOptionResponse;

/**
 * Read-only catalog boundary for recipe authoring. Only active records may be selected for new
 * recipe ingredient links; administrative screens use {@link IngredientCatalogService} instead.
 */
public interface IngredientCatalogLookupService {
    List<IngredientResponse> findActiveIngredients(String query);

    List<IngredientResponse> findActiveIngredientsByIds(Collection<Long> ids);

    List<UnitResponse> findActiveUnits();

    /** Resolve existing references, including catalog entries subsequently deactivated. */
    List<IngredientResponse> findIngredientReferences(Collection<Long> ids);

    List<UnitResponse> findUnitReferences(Collection<Integer> ids);

    List<IngredientUnitOptionResponse> findActiveIngredientUnits(long ingredientId);
}
