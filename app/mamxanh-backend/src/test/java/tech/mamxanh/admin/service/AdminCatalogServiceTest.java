package tech.mamxanh.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import tech.mamxanh.nutrition.service.IngredientCatalogService;
import tech.mamxanh.recipe.service.RecipePostService;

@ExtendWith(MockitoExtension.class)
class AdminCatalogServiceTest {
    @Mock private IngredientCatalogService catalog;
    @Mock private RecipePostService recipes;
    @InjectMocks private AdminCatalogService service;

    @Test
    void delegatesCatalogOperationsAndMapsIngredientRecipeReferences() {
        IngredientResponse ingredient = new IngredientResponse(7L, "Đậu phụ", "Đậu hạt",
                "USDA FoodData Central", "https://example.com/source", LocalDate.of(2026, 9, 1), false, true);
        List<IngredientResponse> ingredients = List.of(ingredient);
        PageResponse<IngredientResponse> page = PageResponse.of(ingredients, 0, 10, 1);
        IngredientSaveRequest ingredientRequest = new IngredientSaveRequest("Đậu phụ", "Đậu hạt",
                "USDA FoodData Central", "https://example.com/source", LocalDate.of(2026, 9, 1));
        IngredientNutritionRequest nutritionRequest = new IngredientNutritionRequest(
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        NutritionSupportUpdateRequest supportRequest = new NutritionSupportUpdateRequest(true);
        CatalogStatusUpdateRequest statusRequest = new CatalogStatusUpdateRequest(false);
        UnitSaveRequest unitRequest = new UnitSaveRequest("g", "gam", MeasurementDimension.MASS, BigDecimal.ONE);
        UnitResponse unit = new UnitResponse(2, "g", "gam", MeasurementDimension.MASS, BigDecimal.ONE, true);
        ConversionSaveRequest conversionRequest = new ConversionSaveRequest(BigDecimal.TEN, true);
        ConversionResponse conversion = new ConversionResponse(7L, 2, BigDecimal.TEN, true, true);

        when(catalog.listIngredients("đậu")).thenReturn(ingredients);
        when(catalog.searchIngredients("đậu", "Đậu hạt", true, false, 0, 10)).thenReturn(page);
        when(catalog.getIngredient(7L)).thenReturn(ingredient);
        when(catalog.createIngredient(ingredientRequest)).thenReturn(ingredient);
        when(catalog.updateIngredient(7L, ingredientRequest)).thenReturn(ingredient);
        when(catalog.updateNutrition(7L, nutritionRequest)).thenReturn(ingredient);
        when(catalog.setNutritionSupport(7L, supportRequest)).thenReturn(ingredient);
        when(catalog.setIngredientStatus(7L, statusRequest)).thenReturn(ingredient);
        when(recipes.listRecipesUsingIngredient(7L)).thenReturn(List.of(
                new RecipePostService.RecipeUsage(11L, "Đậu phụ sốt", "PUBLISHED", LocalDateTime.of(2026, 9, 2, 9, 0))));
        when(catalog.listUnits()).thenReturn(List.of(unit));
        when(catalog.createUnit(unitRequest)).thenReturn(unit);
        when(catalog.updateUnit(2, unitRequest)).thenReturn(unit);
        when(catalog.setUnitStatus(2, statusRequest)).thenReturn(unit);
        when(catalog.listConversions(7L)).thenReturn(List.of(conversion));
        when(catalog.createConversion(7L, 2, conversionRequest)).thenReturn(conversion);
        when(catalog.updateConversion(7L, 2, conversionRequest)).thenReturn(conversion);
        when(catalog.setConversionStatus(7L, 2, statusRequest)).thenReturn(conversion);

        assertThat(service.listIngredients("đậu")).containsExactly(ingredient);
        when(recipes.countRecipesUsingIngredients(List.of(7L))).thenReturn(Map.of(7L, 2L));
        var searchedIngredients = service.searchIngredients("đậu", "Đậu hạt", true, false, 0, 10);
        assertThat(searchedIngredients.content()).hasSize(1);
        assertThat(searchedIngredients.content().getFirst().recipeUsageCount()).isEqualTo(2);
        assertThat(searchedIngredients.totalElements()).isEqualTo(1);
        assertThat(service.getIngredient(7L).id()).isEqualTo(ingredient.id());
        assertThat(service.createIngredient(ingredientRequest)).isSameAs(ingredient);
        assertThat(service.updateIngredient(7L, ingredientRequest)).isSameAs(ingredient);
        assertThat(service.updateNutrition(7L, nutritionRequest)).isSameAs(ingredient);
        assertThat(service.setNutritionSupport(7L, supportRequest)).isSameAs(ingredient);
        assertThat(service.setIngredientStatus(7L, statusRequest)).isSameAs(ingredient);
        assertThat(service.listRecipesUsingIngredient(7L)).containsExactly(
                new tech.mamxanh.admin.dto.IngredientRecipeReferenceResponse(
                        11L, "Đậu phụ sốt", "PUBLISHED", LocalDateTime.of(2026, 9, 2, 9, 0)));
        assertThat(service.listUnits()).containsExactly(unit);
        assertThat(service.createUnit(unitRequest)).isSameAs(unit);
        assertThat(service.updateUnit(2, unitRequest)).isSameAs(unit);
        assertThat(service.setUnitStatus(2, statusRequest)).isSameAs(unit);
        assertThat(service.listConversions(7L)).containsExactly(conversion);
        assertThat(service.createConversion(7L, 2, conversionRequest)).isSameAs(conversion);
        assertThat(service.updateConversion(7L, 2, conversionRequest)).isSameAs(conversion);
        assertThat(service.setConversionStatus(7L, 2, statusRequest)).isSameAs(conversion);

        service.deleteIngredient(7L);
        service.deleteUnit(2);

        verify(catalog).rejectHardDelete("nguyên liệu");
        verify(catalog).deleteUnit(2);
    }

    @Test
    void returnsConfiguredIngredientOptions() {
        when(catalog.getAllowedGroups()).thenReturn(List.of("Rau củ", "Trái cây"));
        when(catalog.getAllowedSources()).thenReturn(List.of("USDA FoodData Central"));

        var options = service.ingredientOptions();

        assertThat(options.groups()).containsExactly("Rau củ", "Trái cây");
        assertThat(options.sources()).containsExactly("USDA FoodData Central");
    }
}
