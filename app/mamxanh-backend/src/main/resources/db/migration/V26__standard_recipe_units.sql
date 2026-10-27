-- Preserve legacy unit rows for historical recipes and shopping items. New recipe choices
-- are restricted by the application to the standardized codes.
IF NOT EXISTS (SELECT 1 FROM [UNIT] WHERE code = N'mg')
    INSERT INTO [UNIT] (code, name, dimension, base_factor, is_active)
    VALUES (N'mg', N'miligram', N'MASS', 0.001, 1);
UPDATE [UNIT] SET name = N'miligram', dimension = N'MASS', base_factor = 0.001 WHERE code = N'mg';

UPDATE [UNIT] SET base_factor = 1.0 WHERE code = N'g' AND dimension = N'MASS';
UPDATE [UNIT] SET base_factor = 1000.0 WHERE code = N'kg' AND dimension = N'MASS';
UPDATE [UNIT] SET base_factor = 1.0 WHERE code = N'ml' AND dimension = N'VOLUME';
UPDATE [UNIT] SET base_factor = 1000.0 WHERE code = N'L' AND dimension = N'VOLUME';
UPDATE [UNIT] SET base_factor = 5.0 WHERE code = N'tsp' AND dimension = N'VOLUME';
UPDATE [UNIT] SET base_factor = 15.0 WHERE code = N'tbsp' AND dimension = N'VOLUME';
UPDATE [UNIT] SET base_factor = 240.0 WHERE code = N'cup' AND dimension = N'VOLUME';

-- Disable the entire active volume group for ingredients whose existing rates disagree.
-- Admins can resolve the base g/ml value and use the grouped endpoint to restore all five.
;WITH ConflictingVolumeIngredients AS (
    SELECT c.ingredient_id
    FROM [INGREDIENT_UNIT_CONVERSION] c
    JOIN [UNIT] u ON u.unit_id = c.unit_id
    WHERE c.is_active = 1 AND u.code IN (N'ml', N'L', N'tsp', N'tbsp', N'cup')
    GROUP BY c.ingredient_id
    HAVING COUNT(DISTINCT CAST(c.grams_per_unit / NULLIF(u.base_factor, 0) AS DECIMAL(18,9))) > 1
)
UPDATE c SET is_active = 0
FROM [INGREDIENT_UNIT_CONVERSION] c
JOIN [UNIT] u ON u.unit_id = c.unit_id
JOIN ConflictingVolumeIngredients conflict ON conflict.ingredient_id = c.ingredient_id
WHERE u.code IN (N'ml', N'L', N'tsp', N'tbsp', N'cup');

ALTER TABLE [INGREDIENT_UNIT_CONVERSION] ALTER COLUMN grams_per_unit DECIMAL(18,9) NOT NULL;

ALTER TABLE [SHOPPING_LIST_ITEM] ALTER COLUMN quantity DECIMAL(18,6) NOT NULL;
ALTER TABLE [SHOPPING_RECIPE_CONTRIBUTION] ALTER COLUMN quantity DECIMAL(18,6) NOT NULL;
