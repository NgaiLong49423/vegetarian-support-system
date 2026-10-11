package tech.mamxanh.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tech.mamxanh.nutrition.entity.IngredientEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionId;
import tech.mamxanh.nutrition.repository.IngredientRepository;
import tech.mamxanh.nutrition.repository.IngredientUnitConversionRepository;

class RecipeNutritionServiceTest {
    private final IngredientRepository ingredients = mock(IngredientRepository.class);
    private final IngredientUnitConversionRepository conversions = mock(IngredientUnitConversionRepository.class);
    private final RecipeNutritionService service = new RecipeNutritionService(ingredients, conversions);

    @Test
    void convertsIngredientSpecificUnitsAndCalculatesPerServingAcrossNineNutrients() {
        IngredientEntity tofu = supportedIngredient(4L, "Đậu phụ mơ", "8");
        when(ingredients.findAllById(List.of(4L))).thenReturn(List.of(tofu));
        var conversionId = new IngredientUnitConversionId(4L, 9);
        when(conversions.findByIdIngredientIdInAndActiveTrue(java.util.Set.of(4L))).thenReturn(List.of(
                new IngredientUnitConversionEntity(conversionId, new BigDecimal("150"), false)));

        var result = service.calculate(List.of(new RecipeNutritionService.RecipeIngredient(4L, 9, "COUNT",
                BigDecimal.ONE, new BigDecimal("2"), "Đậu phụ mơ")), 2);

        assertTrue(result.complete());
        assertEquals(new BigDecimal("24.00"), result.total().get("PROTEIN_G"));
        assertEquals(new BigDecimal("12.00"), result.perServing().get("PROTEIN_G"));
        assertEquals(9, result.total().size());
    }

    @Test
    void doesNotTreatMissingConversionOrCustomIngredientAsZeroData() {
        IngredientEntity tofu = supportedIngredient(4L, "Đậu phụ mơ", "8");
        when(ingredients.findAllById(List.of(4L))).thenReturn(List.of(tofu));
        var missingConversion = service.calculate(List.of(new RecipeNutritionService.RecipeIngredient(4L, 9, "COUNT",
                BigDecimal.ONE, BigDecimal.ONE, "Đậu phụ mơ")), 1);
        var customIngredient = service.calculate(List.of(new RecipeNutritionService.RecipeIngredient(null, 9, "COUNT",
                BigDecimal.ONE, BigDecimal.ONE, "Rau gia vị tự chọn")), 1);

        assertFalse(missingConversion.complete());
        assertEquals(List.of("Đậu phụ mơ"), missingConversion.ingredientsMissingData());
        assertEquals(BigDecimal.ZERO.setScale(2), missingConversion.total().get("PROTEIN_G"));
        assertFalse(customIngredient.complete());
        assertEquals(List.of("Rau gia vị tự chọn"), customIngredient.ingredientsMissingData());
    }

    private IngredientEntity supportedIngredient(long id, String name, String protein) {
        IngredientEntity ingredient = new IngredientEntity(name, "Đạm thực vật", "USDA", "https://example.test", java.time.LocalDate.now());
        ReflectionTestUtils.setField(ingredient, "id", id);
        ReflectionTestUtils.setField(ingredient, "nutritionSupported", true);
        ReflectionTestUtils.setField(ingredient, "energyKcalPer100g", new BigDecimal("80"));
        ReflectionTestUtils.setField(ingredient, "proteinGramsPer100g", new BigDecimal(protein));
        ReflectionTestUtils.setField(ingredient, "carbohydrateGramsPer100g", new BigDecimal("3"));
        ReflectionTestUtils.setField(ingredient, "totalFatGramsPer100g", new BigDecimal("4"));
        ReflectionTestUtils.setField(ingredient, "fiberGramsPer100g", new BigDecimal("1"));
        ReflectionTestUtils.setField(ingredient, "calciumMgPer100g", new BigDecimal("20"));
        ReflectionTestUtils.setField(ingredient, "ironMgPer100g", new BigDecimal("2"));
        ReflectionTestUtils.setField(ingredient, "vitaminB12MicrogramsPer100g", new BigDecimal("0.2"));
        ReflectionTestUtils.setField(ingredient, "zincMgPer100g", new BigDecimal("1"));
        return ingredient;
    }
}
