ALTER TABLE [SHOPPING_RECIPE_CONTRIBUTION]
    ADD source_id UNIQUEIDENTIFIER NULL,
        servings DECIMAL(4,1) NULL,
        meal_plan_entry_id BIGINT NULL,
        meal_date DATE NULL,
        meal_type VARCHAR(20) NULL;
GO

-- Keep existing recipe contributions grouped by list and recipe; historical serving counts are unknown.
DECLARE @SourceGroups TABLE (shopping_list_id BIGINT, recipe_id BIGINT, source_id UNIQUEIDENTIFIER);
INSERT INTO @SourceGroups (shopping_list_id, recipe_id, source_id)
SELECT shopping_list_id, recipe_id, NEWID()
FROM [SHOPPING_RECIPE_CONTRIBUTION]
WHERE source_id IS NULL
GROUP BY shopping_list_id, recipe_id;
UPDATE c SET source_id = g.source_id
FROM [SHOPPING_RECIPE_CONTRIBUTION] c
JOIN @SourceGroups g ON g.shopping_list_id = c.shopping_list_id AND g.recipe_id = c.recipe_id
WHERE c.source_id IS NULL;
ALTER TABLE [SHOPPING_RECIPE_CONTRIBUTION] ALTER COLUMN source_id UNIQUEIDENTIFIER NOT NULL;
CREATE INDEX IX_SRC_LIST_SOURCE ON [SHOPPING_RECIPE_CONTRIBUTION](shopping_list_id, source_id);
