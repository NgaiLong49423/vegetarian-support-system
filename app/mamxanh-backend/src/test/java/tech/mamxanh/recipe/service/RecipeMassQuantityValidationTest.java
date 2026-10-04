package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecipeMassQuantityValidationTest {
    @Test
    void acceptsOnlyWholeGramMultiplesOfOneHundred() {
        for (String quantity : List.of("100", "200", "500")) {
            assertThat(RecipeService.massQuantityError(new BigDecimal(quantity), "g")).isNull();
        }
    }

    @Test
    void rejectsGramQuantitiesBelowBetweenOrFractionalToTheHundredGramMultiple() {
        for (String quantity : List.of("80", "120", "150", "100.5")) {
            assertThat(RecipeService.massQuantityError(new BigDecimal(quantity), "g"))
                    .contains("bội số của 100 g");
        }
    }

    @Test
    void acceptsDecimalKilogramsOnlyWhenConvertedGramsAreMultiplesOfOneHundred() {
        for (String quantity : List.of("0.1", "0.5", "1.5")) {
            assertThat(RecipeService.massQuantityError(new BigDecimal(quantity), "kg")).isNull();
        }
        for (String quantity : List.of("0.15", "1.55")) {
            assertThat(RecipeService.massQuantityError(new BigDecimal(quantity), "kg"))
                    .contains("chia hết cho 100 g");
        }
    }

    @Test
    void leavesOtherUnitsToTheirIngredientConversionRule() {
        assertThat(RecipeService.massQuantityError(new BigDecimal("2"), "quả")).isNull();
        assertThat(RecipeService.massQuantityError(new BigDecimal("3"), "bó")).isNull();
    }
}
