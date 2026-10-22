-- Repeatable demo fixtures for local development only.
-- Password hashes are initialized at runtime from MAMXANH_DEMO_PASSWORD and are never stored here.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

-- Shopping lists include recipe ingredients, a custom item, and mixed checklist states.
DECLARE @demo_shopping_lists TABLE (owner_email VARCHAR(200), name NVARCHAR(200), shopping_list_id BIGINT NULL);
INSERT INTO @demo_shopping_lists (owner_email, name) VALUES
 ('demo-customer@mamxanh.local', N'Đi chợ tuần này'),
 ('demo-customer-04@mamxanh.local', N'Nguyên liệu cuối tuần');

INSERT INTO [SHOPPING_LIST] (user_id, name)
SELECT member.user_id, seed.name
FROM @demo_shopping_lists seed JOIN [USER] member ON member.email = seed.owner_email
WHERE NOT EXISTS (SELECT 1 FROM [SHOPPING_LIST] existing
    WHERE existing.user_id = member.user_id AND existing.name = seed.name);

UPDATE seed SET shopping_list_id = existing.shopping_list_id
FROM @demo_shopping_lists seed
JOIN [USER] member ON member.email = seed.owner_email
JOIN [SHOPPING_LIST] existing ON existing.user_id = member.user_id AND existing.name = seed.name;

INSERT INTO [SHOPPING_LIST_ITEM] (shopping_list_id, ingredient_id, custom_ingredient_name, unit_id, quantity, is_bought)
SELECT lists.shopping_list_id, ingredient.ingredient_id, seed.custom_name, unit.unit_id, seed.quantity, seed.is_bought
FROM (VALUES
    (N'Đi chợ tuần này', N'Đậu hũ', CAST(NULL AS NVARCHAR(200)), N'bìa', CAST(2 AS DECIMAL(10,2)), CAST(0 AS BIT)),
    (N'Đi chợ tuần này', N'Cà chua', CAST(NULL AS NVARCHAR(200)), N'quả', CAST(4 AS DECIMAL(10,2)), CAST(0 AS BIT)),
    (N'Đi chợ tuần này', N'Tỏi', CAST(NULL AS NVARCHAR(200)), N'g', CAST(100 AS DECIMAL(10,2)), CAST(1 AS BIT)),
    (N'Đi chợ tuần này', CAST(NULL AS NVARCHAR(200)), N'Rau thơm theo mùa', N'bó', CAST(1 AS DECIMAL(10,2)), CAST(0 AS BIT)),
    (N'Nguyên liệu cuối tuần', N'Hạt sen', CAST(NULL AS NVARCHAR(200)), N'g', CAST(150 AS DECIMAL(10,2)), CAST(0 AS BIT)),
    (N'Nguyên liệu cuối tuần', N'Bí đao', CAST(NULL AS NVARCHAR(200)), N'trái', CAST(1 AS DECIMAL(10,2)), CAST(1 AS BIT)),
    (N'Nguyên liệu cuối tuần', N'Rau dền', CAST(NULL AS NVARCHAR(200)), N'g', CAST(200 AS DECIMAL(10,2)), CAST(0 AS BIT))
) seed(list_name, ingredient_name, custom_name, unit_code, quantity, is_bought)
JOIN @demo_shopping_lists lists ON lists.name = seed.list_name
JOIN [UNIT] unit ON unit.code = seed.unit_code
LEFT JOIN [INGREDIENT] ingredient ON ingredient.name = seed.ingredient_name
WHERE NOT EXISTS (
    SELECT 1 FROM [SHOPPING_LIST_ITEM] existing
    WHERE existing.shopping_list_id = lists.shopping_list_id
      AND ((seed.ingredient_name IS NOT NULL AND existing.ingredient_id = ingredient.ingredient_id)
        OR (seed.custom_name IS NOT NULL AND existing.custom_ingredient_name = seed.custom_name))
);

COMMIT TRANSACTION;
