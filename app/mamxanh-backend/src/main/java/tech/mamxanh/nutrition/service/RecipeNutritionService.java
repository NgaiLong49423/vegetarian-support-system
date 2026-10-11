package tech.mamxanh.nutrition.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.nutrition.entity.IngredientEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionId;
import tech.mamxanh.nutrition.repository.IngredientRepository;
import tech.mamxanh.nutrition.repository.IngredientUnitConversionRepository;

/** Read-only calculation of recipe nutrition from the local ingredient catalog. */
@Service
public class RecipeNutritionService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final IngredientRepository ingredientRepository;
    private final IngredientUnitConversionRepository conversionRepository;

    public RecipeNutritionService(IngredientRepository ingredientRepository,
            IngredientUnitConversionRepository conversionRepository) {
        this.ingredientRepository = ingredientRepository;
        this.conversionRepository = conversionRepository;
    }

    @Transactional(readOnly = true)
    public NutritionSummary calculate(List<RecipeIngredient> recipeIngredients, int servings) {
        Map<Long, IngredientEntity> ingredients = ingredientRepository.findAllById(recipeIngredients.stream()
                .map(RecipeIngredient::ingredientId).filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(IngredientEntity::getId, ingredient -> ingredient));
        Set<Long> convertedIngredientIds = recipeIngredients.stream()
                .filter(line -> !"MASS".equals(line.unitDimension()))
                .map(RecipeIngredient::ingredientId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<IngredientUnitConversionId, IngredientUnitConversionEntity> conversions = convertedIngredientIds.isEmpty()
                ? Map.of()
                : conversionRepository.findByIdIngredientIdInAndActiveTrue(convertedIngredientIds).stream()
                        .collect(Collectors.toMap(IngredientUnitConversionEntity::getId, conversion -> conversion));
        EnumMap<Nutrient, BigDecimal> totals = new EnumMap<>(Nutrient.class);
        for (Nutrient nutrient : Nutrient.values()) totals.put(nutrient, BigDecimal.ZERO);
        List<String> missing = new ArrayList<>();
        for (RecipeIngredient line : recipeIngredients) {
            IngredientEntity ingredient = line.ingredientId() == null ? null : ingredients.get(line.ingredientId());
            if (ingredient == null || !ingredient.isNutritionSupported()) {
                missing.add(line.displayName());
                continue;
            }
            BigDecimal grams = grams(line, ingredient.getId(), conversions);
            if (grams == null) {
                missing.add(line.displayName());
                continue;
            }
            Map<Nutrient, BigDecimal> values = nutrientValues(ingredient);
            if (values.values().stream().anyMatch(Objects::isNull)) {
                missing.add(line.displayName());
                continue;
            }
            BigDecimal factor = grams.divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP);
            values.forEach((nutrient, value) -> totals.merge(nutrient, value.multiply(factor), BigDecimal::add));
        }
        boolean complete = missing.isEmpty();
        Map<Nutrient, BigDecimal> perServing = new EnumMap<>(Nutrient.class);
        totals.forEach((nutrient, total) -> perServing.put(nutrient,
                total.divide(BigDecimal.valueOf(servings), 4, RoundingMode.HALF_UP)));
        return new NutritionSummary(complete, missing.stream().distinct().toList(), values(totals), values(perServing));
    }

    private BigDecimal grams(RecipeIngredient line, long ingredientId,
            Map<IngredientUnitConversionId, IngredientUnitConversionEntity> conversions) {
        BigDecimal gramsPerUnit;
        if ("MASS".equals(line.unitDimension())) {
            gramsPerUnit = line.unitBaseFactor();
        } else {
            IngredientUnitConversionEntity conversion = conversions.get(new IngredientUnitConversionId(ingredientId, line.unitId()));
            gramsPerUnit = conversion == null ? null : conversion.getGramsPerUnit();
        }
        return gramsPerUnit == null ? null : line.quantity().multiply(gramsPerUnit);
    }

    private static Map<Nutrient, BigDecimal> nutrientValues(IngredientEntity ingredient) {
        EnumMap<Nutrient, BigDecimal> values = new EnumMap<>(Nutrient.class);
        values.put(Nutrient.ENERGY_KCAL, ingredient.getEnergyKcalPer100g());
        values.put(Nutrient.PROTEIN_G, ingredient.getProteinGramsPer100g());
        values.put(Nutrient.CARBOHYDRATE_G, ingredient.getCarbohydrateGramsPer100g());
        values.put(Nutrient.TOTAL_FAT_G, ingredient.getTotalFatGramsPer100g());
        values.put(Nutrient.FIBER_G, ingredient.getFiberGramsPer100g());
        values.put(Nutrient.CALCIUM_MG, ingredient.getCalciumMgPer100g());
        values.put(Nutrient.IRON_MG, ingredient.getIronMgPer100g());
        values.put(Nutrient.VITAMIN_B12_MCG, ingredient.getVitaminB12MicrogramsPer100g());
        values.put(Nutrient.ZINC_MG, ingredient.getZincMgPer100g());
        return values;
    }

    private static Map<String, BigDecimal> values(Map<Nutrient, BigDecimal> source) {
        return source.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().name(),
                entry -> entry.getValue().setScale(2, RoundingMode.HALF_UP)));
    }

    public record RecipeIngredient(Long ingredientId, Integer unitId, String unitDimension,
            BigDecimal unitBaseFactor, BigDecimal quantity, String displayName) { }

    public record NutritionSummary(boolean complete, List<String> ingredientsMissingData,
            Map<String, BigDecimal> total, Map<String, BigDecimal> perServing) { }

    private enum Nutrient { ENERGY_KCAL, PROTEIN_G, CARBOHYDRATE_G, TOTAL_FAT_G, FIBER_G,
        CALCIUM_MG, IRON_MG, VITAMIN_B12_MCG, ZINC_MG }
}
