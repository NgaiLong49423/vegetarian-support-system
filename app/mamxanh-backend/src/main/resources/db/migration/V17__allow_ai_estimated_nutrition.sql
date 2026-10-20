-- Nutrition provenance is descriptive metadata; AI estimates may participate in partial calculations.
-- Require at least one stored nutrient when nutrition support is enabled.
ALTER TABLE [INGREDIENT] DROP CONSTRAINT [CK_INGREDIENT_nutrition_supported];

ALTER TABLE [INGREDIENT] ADD CONSTRAINT [CK_INGREDIENT_nutrition_supported] CHECK (
    nutrition_supported = 0 OR (
        energy_kcal_100g IS NOT NULL
        OR protein_g_100g IS NOT NULL
        OR carbohydrate_g_100g IS NOT NULL
        OR total_fat_g_100g IS NOT NULL
        OR fiber_g_100g IS NOT NULL
        OR calcium_mg_100g IS NOT NULL
        OR iron_mg_100g IS NOT NULL
        OR vitamin_b12_mcg_100g IS NOT NULL
        OR zinc_mg_100g IS NOT NULL
    )
);
