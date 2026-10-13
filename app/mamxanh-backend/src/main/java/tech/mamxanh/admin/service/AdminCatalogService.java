package tech.mamxanh.admin.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.admin.dto.IngredientRecipeReferenceResponse;
import tech.mamxanh.admin.dto.IngredientCatalogOptionsResponse;
import tech.mamxanh.admin.dto.AdminIngredientResponse;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.nutrition.dto.request.CatalogStatusUpdateRequest;
import tech.mamxanh.nutrition.dto.request.ConversionSaveRequest;
import tech.mamxanh.nutrition.dto.request.IngredientNutritionRequest;
import tech.mamxanh.nutrition.dto.request.IngredientSaveRequest;
import tech.mamxanh.nutrition.dto.request.NutritionSupportUpdateRequest;
import tech.mamxanh.nutrition.dto.request.UnitSaveRequest;
import tech.mamxanh.nutrition.dto.response.ConversionResponse;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.nutrition.service.IngredientCatalogService;
import tech.mamxanh.recipe.service.RecipePostService;

/** Admin-facing facade for catalog operations spanning nutrition and recipe modules. */
@Service
public class AdminCatalogService {
    private final IngredientCatalogService catalog;
    private final RecipePostService recipes;

    public AdminCatalogService(IngredientCatalogService catalog, RecipePostService recipes) {
        this.catalog = catalog;
        this.recipes = recipes;
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> listIngredients(String query) { return catalog.listIngredients(query); }

    @Transactional(readOnly = true)
    public PageResponse<AdminIngredientResponse> searchIngredients(String query, String group, Boolean active,
            Boolean nutritionSupported, int page, int size) {
        PageResponse<IngredientResponse> results = catalog.searchIngredients(query, group, active,
                nutritionSupported, page, size);
        Map<Long, Long> usageCounts = recipes.countRecipesUsingIngredients(
                results.content().stream().map(IngredientResponse::id).toList());
        return new PageResponse<>(results.content().stream()
                .map(ingredient -> AdminIngredientResponse.from(ingredient,
                        usageCounts.getOrDefault(ingredient.id(), 0L)))
                .toList(), results.page(), results.size(), results.totalElements(), results.totalPages());
    }

    @Transactional(readOnly = true)
    public AdminIngredientResponse getIngredient(long id) {
        IngredientResponse ingredient = catalog.getIngredient(id);
        Map<Long, Long> usageCounts = recipes.countRecipesUsingIngredients(List.of(id));
        return AdminIngredientResponse.from(ingredient,
                usageCounts == null ? 0 : usageCounts.getOrDefault(id, 0L));
    }

    @Transactional(readOnly = true)
    public IngredientCatalogOptionsResponse ingredientOptions() {
        return new IngredientCatalogOptionsResponse(catalog.getAllowedGroups(), catalog.getAllowedSources());
    }

    @Transactional
    public IngredientResponse createIngredient(IngredientSaveRequest request) { return catalog.createIngredient(request); }

    @Transactional
    public IngredientResponse updateIngredient(long id, IngredientSaveRequest request) { return catalog.updateIngredient(id, request); }

    @Transactional
    public IngredientResponse updateNutrition(long id, IngredientNutritionRequest request) { return catalog.updateNutrition(id, request); }

    @Transactional
    public IngredientResponse setNutritionSupport(long id, NutritionSupportUpdateRequest request) {
        return catalog.setNutritionSupport(id, request);
    }

    @Transactional
    public IngredientResponse setIngredientStatus(long id, CatalogStatusUpdateRequest request) {
        return catalog.setIngredientStatus(id, request);
    }

    @Transactional
    public void deleteIngredient(long id) { catalog.getIngredient(id); catalog.rejectHardDelete("nguyên liệu"); }

    @Transactional(readOnly = true)
    public List<IngredientRecipeReferenceResponse> listRecipesUsingIngredient(long ingredientId) {
        catalog.getIngredient(ingredientId);
        return recipes.listRecipesUsingIngredient(ingredientId).stream()
                .map(recipe -> new IngredientRecipeReferenceResponse(recipe.recipeId(), recipe.title(),
                        recipe.status(), recipe.publishedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<IngredientRecipeReferenceResponse> searchRecipesUsingIngredient(
            long ingredientId, int page, int size) {
        catalog.getIngredient(ingredientId);
        PageResponse<RecipePostService.RecipeUsage> recipesPage = recipes.searchRecipesUsingIngredient(
                ingredientId, page, size);
        List<IngredientRecipeReferenceResponse> content = recipesPage.content().stream()
                .map(recipe -> new IngredientRecipeReferenceResponse(recipe.recipeId(), recipe.title(),
                        recipe.status(), recipe.publishedAt()))
                .collect(Collectors.toList());
        return new PageResponse<>(content, recipesPage.page(), recipesPage.size(),
                recipesPage.totalElements(), recipesPage.totalPages());
    }

    @Transactional(readOnly = true)
    public List<UnitResponse> listUnits() { return catalog.listUnits(); }
    @Transactional
    public UnitResponse createUnit(UnitSaveRequest request) { return catalog.createUnit(request); }
    @Transactional
    public UnitResponse updateUnit(int id, UnitSaveRequest request) { return catalog.updateUnit(id, request); }
    @Transactional
    public UnitResponse setUnitStatus(int id, CatalogStatusUpdateRequest request) { return catalog.setUnitStatus(id, request); }
    @Transactional
    public void deleteUnit(int id) { catalog.deleteUnit(id); }
    @Transactional(readOnly = true)
    public List<ConversionResponse> listConversions(Long ingredientId) { return catalog.listConversions(ingredientId); }
    @Transactional
    public ConversionResponse createConversion(long ingredientId, int unitId, ConversionSaveRequest request) {
        return catalog.createConversion(ingredientId, unitId, request);
    }
    @Transactional
    public ConversionResponse updateConversion(long ingredientId, int unitId, ConversionSaveRequest request) {
        return catalog.updateConversion(ingredientId, unitId, request);
    }
    @Transactional
    public ConversionResponse setConversionStatus(long ingredientId, int unitId, CatalogStatusUpdateRequest request) {
        return catalog.setConversionStatus(ingredientId, unitId, request);
    }
}
