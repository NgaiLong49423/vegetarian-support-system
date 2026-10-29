ALTER TABLE [SHOPPING_RECIPE_CONTRIBUTION] DROP CONSTRAINT [CK_SRC_source_kind];
GO

-- Keep an incomplete recipe occurrence hidden, but make its surviving item amounts editable.
;WITH IncompleteSources AS (
    SELECT shopping_list_id, source_id
    FROM [SHOPPING_RECIPE_CONTRIBUTION]
    WHERE source_kind = 'RECIPE'
    GROUP BY shopping_list_id, source_id
    HAVING MIN(expected_ingredient_count) IS NULL
        OR MIN(expected_ingredient_count) <> MAX(expected_ingredient_count)
        OR COUNT_BIG(*) <> MAX(expected_ingredient_count)
        OR SUM(CASE WHEN shopping_list_item_id IS NULL THEN 1 ELSE 0 END) > 0
)
UPDATE contribution
SET source_kind = 'UNKNOWN'
FROM [SHOPPING_RECIPE_CONTRIBUTION] contribution
JOIN IncompleteSources incomplete
    ON incomplete.shopping_list_id = contribution.shopping_list_id
    AND incomplete.source_id = contribution.source_id
WHERE contribution.source_kind = 'RECIPE'
    AND contribution.shopping_list_item_id IS NOT NULL;
GO

ALTER TABLE [SHOPPING_RECIPE_CONTRIBUTION] ADD CONSTRAINT [CK_SRC_source_kind] CHECK (
    (source_kind = 'RECIPE' AND recipe_id IS NOT NULL AND ingredient_id IS NOT NULL
        AND (expected_ingredient_count IS NULL OR expected_ingredient_count > 0))
    OR (source_kind = 'MANUAL' AND recipe_id IS NULL AND expected_ingredient_count IS NULL)
    OR (source_kind = 'UNKNOWN' AND ingredient_id IS NOT NULL
        AND (expected_ingredient_count IS NULL OR expected_ingredient_count > 0))
);
GO
