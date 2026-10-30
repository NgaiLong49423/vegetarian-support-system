ALTER TABLE [SHOPPING_RECIPE_CONTRIBUTION]
    ADD source_kind VARCHAR(16) NOT NULL
        CONSTRAINT DF_SRC_source_kind DEFAULT 'RECIPE',
        expected_ingredient_count INT NULL;
GO

-- Snapshot each recipe occurrence's required ingredient count, including unselected lines.
WITH RecipeIngredientCounts AS (
    SELECT recipe_id, COUNT_BIG(*) AS ingredient_count
    FROM [RECIPE_INGREDIENT]
    GROUP BY recipe_id
)
UPDATE contribution
SET expected_ingredient_count = CONVERT(INT, counts.ingredient_count)
FROM [SHOPPING_RECIPE_CONTRIBUTION] contribution
JOIN RecipeIngredientCounts counts ON counts.recipe_id = contribution.recipe_id;
GO

-- Legacy sources without a saved serving count do not have enough provenance to assert completeness.
UPDATE [SHOPPING_RECIPE_CONTRIBUTION]
SET expected_ingredient_count = NULL
WHERE servings IS NULL;
GO

ALTER TABLE [SHOPPING_RECIPE_CONTRIBUTION] ALTER COLUMN recipe_id BIGINT NULL;
ALTER TABLE [SHOPPING_RECIPE_CONTRIBUTION] ALTER COLUMN ingredient_id BIGINT NULL;
ALTER TABLE [SHOPPING_RECIPE_CONTRIBUTION] ADD CONSTRAINT CK_SRC_source_kind CHECK (
    (source_kind = 'RECIPE' AND recipe_id IS NOT NULL AND ingredient_id IS NOT NULL
        AND (expected_ingredient_count IS NULL OR expected_ingredient_count > 0))
    OR (source_kind = 'MANUAL' AND recipe_id IS NULL AND expected_ingredient_count IS NULL)
);
GO
