-- Local-only AI-estimated ingredients; all nine nutrition values are illustrative and unverified.
-- Values are per 100 g so demo recipes have no missing nutrition metrics.
-- No USDA identifiers or source URLs are assigned; estimates are labeled as unverified.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @new_ai_ingredients TABLE (
    name NVARCHAR(200) PRIMARY KEY,
    calcium_mg DECIMAL(10,2),
    iron_mg DECIMAL(10,2),
    zinc_mg DECIMAL(10,2)
);
INSERT INTO @new_ai_ingredients (name, calcium_mg, iron_mg, zinc_mg) VALUES
    (N'Cải cầu vồng', 51, 1.8, 0.36),
    (N'Cải Brussels', 42, 1.4, 0.42),
    (N'Bắp cải tím', 45, 0.8, 0.22),
    (N'Cải xoong', 120, 1.3, 0.2),
    (N'Rau diếp cá', 50, 2.2, 0.35),
    (N'Rau kinh giới', 180, 4.1, 0.9),
    (N'Rau thì là', 208, 6.5, 0.9),
    (N'Tần ô', 110, 2.1, 0.4),
    (N'Rau đay', 220, 3.1, 0.5),
    (N'Rau ngổ', 70, 2.0, 0.4),
    (N'Bông thiên lý', 40, 1.6, 0.3),
    (N'Đậu cove vàng', 37, 1.1, 0.25),
    (N'Đậu đũa', 50, 1.1, 0.4),
    (N'Đậu bắp tím', 80, 0.7, 0.6),
    (N'Mướp khía', 20, 0.4, 0.2),
    (N'Củ năng', 10, 0.3, 0.2),
    (N'Khoai mỡ', 25, 0.6, 0.3),
    (N'Củ cải đỏ', 25, 0.3, 0.2),
    (N'Củ cải vàng', 32, 0.3, 0.27),
    (N'Bắp chuối', 40, 0.6, 0.4),
    (N'Măng tre', 13, 1.1, 1.1),
    (N'Chuối xanh', 5, 0.3, 0.15),
    (N'Chuối sứ', 6, 0.3, 0.15),
    (N'Quýt', 37, 0.15, 0.07),
    (N'Bưởi', 22, 0.08, 0.07),
    (N'Kiwi', 34, 0.31, 0.14),
    (N'Dâu tằm', 39, 1.85, 0.12),
    (N'Mận hậu', 6, 0.17, 0.1),
    (N'Mơ', 13, 0.39, 0.2),
    (N'Chà là', 64, 0.9, 0.44),
    (N'Dừa nạo', 14, 2.43, 1.1),
    (N'Sầu riêng', 6, 0.43, 0.28),
    (N'Mít', 24, 0.23, 0.42),
    (N'Vải', 5, 0.31, 0.07),
    (N'Nhãn', 1, 0.13, 0.05),
    (N'Chôm chôm', 22, 0.35, 0.08),
    (N'Dưa lưới', 9, 0.21, 0.18),
    (N'Thanh trà', 20, 0.4, 0.15),
    (N'Kê vàng', 8, 3.0, 0.9),
    (N'Lúa mạch', 29, 3.6, 2.8),
    (N'Kiều mạch', 18, 2.2, 2.4),
    (N'Hạt dền', 159, 7.6, 2.9),
    (N'Bột yến mạch', 54, 4.7, 3.97),
    (N'Bột hạnh nhân', 269, 3.7, 3.1),
    (N'Bột bắp', 7, 0.5, 0.5),
    (N'Gạo basmati', 10, 1.5, 1.1),
    (N'Mì soba', 25, 2.5, 1.5),
    (N'Lá dứa', 20, 1.2, 0.4),
    (N'Lá chanh', 100, 3.5, 0.7),
    (N'Húng chanh', 210, 4.0, 0.8),
    (N'Trứng gà', 56, 1.75, 1.29);

DECLARE @new_ai_macros TABLE (
    name NVARCHAR(200) PRIMARY KEY,
    energy_kcal DECIMAL(10,2), protein_g DECIMAL(10,2), carbohydrate_g DECIMAL(10,2),
    total_fat_g DECIMAL(10,2), fiber_g DECIMAL(10,2), vitamin_b12_mcg DECIMAL(10,4)
);
INSERT INTO @new_ai_macros VALUES
    (N'Cải cầu vồng', 19, 1.8, 3.7, 0.2, 1.6, 0), (N'Cải Brussels', 43, 3.4, 9.0, 0.3, 3.8, 0),
    (N'Bắp cải tím', 31, 1.4, 7.4, 0.2, 2.1, 0), (N'Cải xoong', 11, 2.3, 1.3, 0.1, 0.5, 0),
    (N'Rau diếp cá', 32, 2.0, 6.0, 0.5, 2.0, 0), (N'Rau kinh giới', 44, 3.3, 9.0, 0.7, 4.0, 0),
    (N'Rau thì là', 43, 3.5, 7.0, 1.1, 2.1, 0), (N'Tần ô', 20, 1.8, 3.5, 0.3, 2.0, 0),
    (N'Rau đay', 34, 4.2, 5.7, 0.3, 2.0, 0), (N'Rau ngổ', 25, 2.0, 4.0, 0.3, 1.5, 0),
    (N'Bông thiên lý', 30, 3.0, 4.0, 0.2, 2.0, 0), (N'Đậu cove vàng', 31, 1.8, 7.0, 0.1, 3.0, 0),
    (N'Đậu đũa', 47, 2.8, 8.4, 0.4, 3.7, 0), (N'Đậu bắp tím', 33, 1.9, 7.5, 0.2, 3.2, 0),
    (N'Mướp khía', 20, 0.8, 4.4, 0.2, 1.0, 0), (N'Củ năng', 97, 1.4, 23.9, 0.1, 3.0, 0),
    (N'Khoai mỡ', 118, 1.5, 27.9, 0.2, 4.1, 0), (N'Củ cải đỏ', 43, 1.6, 9.6, 0.2, 2.8, 0),
    (N'Củ cải vàng', 41, 0.9, 9.6, 0.2, 2.8, 0), (N'Bắp chuối', 51, 1.6, 9.9, 0.6, 5.7, 0),
    (N'Măng tre', 27, 2.6, 5.2, 0.3, 2.2, 0), (N'Chuối xanh', 89, 1.1, 22.8, 0.3, 2.6, 0),
    (N'Chuối sứ', 89, 1.1, 22.8, 0.3, 2.6, 0), (N'Quýt', 53, 0.8, 13.3, 0.3, 1.8, 0),
    (N'Bưởi', 38, 0.8, 9.6, 0.0, 1.0, 0), (N'Kiwi', 61, 1.1, 14.7, 0.5, 3.0, 0),
    (N'Dâu tằm', 43, 1.4, 9.8, 0.4, 1.7, 0), (N'Mận hậu', 46, 0.7, 11.4, 0.3, 1.4, 0),
    (N'Mơ', 48, 1.4, 11.1, 0.4, 2.0, 0), (N'Chà là', 282, 2.5, 75.0, 0.4, 8.0, 0),
    (N'Dừa nạo', 354, 3.3, 15.2, 33.5, 9.0, 0), (N'Sầu riêng', 147, 1.5, 27.1, 5.3, 3.8, 0),
    (N'Mít', 95, 1.7, 23.3, 0.6, 1.5, 0), (N'Vải', 66, 0.8, 16.5, 0.4, 1.3, 0),
    (N'Nhãn', 60, 1.3, 15.1, 0.1, 1.1, 0), (N'Chôm chôm', 68, 0.9, 16.5, 0.2, 0.9, 0),
    (N'Dưa lưới', 34, 0.8, 8.2, 0.2, 0.9, 0), (N'Thanh trà', 40, 0.8, 10.0, 0.1, 1.5, 0),
    (N'Kê vàng', 378, 11.0, 72.8, 4.2, 8.5, 0), (N'Lúa mạch', 354, 12.5, 73.5, 2.3, 17.3, 0),
    (N'Kiều mạch', 343, 13.3, 71.5, 3.4, 10.0, 0), (N'Hạt dền', 371, 13.6, 65.3, 7.0, 6.7, 0),
    (N'Bột yến mạch', 404, 14.7, 65.7, 9.1, 6.5, 0), (N'Bột hạnh nhân', 571, 21.2, 18.7, 50.0, 10.4, 0),
    (N'Bột bắp', 361, 6.9, 76.9, 3.9, 7.3, 0), (N'Gạo basmati', 365, 7.1, 80.0, 0.7, 1.3, 0),
    (N'Mì soba', 336, 14.4, 74.0, 1.4, 3.3, 0), (N'Lá dứa', 40, 1.0, 8.0, 0.5, 2.0, 0),
    (N'Lá chanh', 60, 4.0, 10.0, 1.0, 3.0, 0), (N'Húng chanh', 45, 2.5, 8.0, 0.8, 3.0, 0),
    (N'Trứng gà', 143, 12.6, 0.7, 9.5, 0.0, 1.1);

-- Insert only missing names so pre-existing catalog rows are not overwritten.
INSERT INTO [INGREDIENT] (
    name, energy_kcal_100g, protein_g_100g, carbohydrate_g_100g, total_fat_g_100g,
    fiber_g_100g, calcium_mg_100g, iron_mg_100g, vitamin_b12_mcg_100g, zinc_mg_100g,
    source_name, source_url, reference_date, nutrition_supported
)
SELECT seed.name, macros.energy_kcal, macros.protein_g, macros.carbohydrate_g,
    macros.total_fat_g, macros.fiber_g, seed.calcium_mg, seed.iron_mg,
    macros.vitamin_b12_mcg, seed.zinc_mg,
    N'Ước tính AI - chưa xác minh', NULL, CONVERT(DATE, SYSUTCDATETIME()), 1
FROM @new_ai_ingredients seed
JOIN @new_ai_macros macros ON macros.name = seed.name
WHERE NOT EXISTS (SELECT 1 FROM [INGREDIENT] existing WHERE existing.name = seed.name);

-- Refresh only this fixture's own unverified rows when the repeatable changes.
UPDATE ingredient
SET energy_kcal_100g = macros.energy_kcal, protein_g_100g = macros.protein_g,
    carbohydrate_g_100g = macros.carbohydrate_g, total_fat_g_100g = macros.total_fat_g,
    fiber_g_100g = macros.fiber_g,
    calcium_mg_100g = seed.calcium_mg, iron_mg_100g = seed.iron_mg,
    vitamin_b12_mcg_100g = macros.vitamin_b12_mcg, zinc_mg_100g = seed.zinc_mg,
    source_url = NULL, nutrition_supported = 1, updated_at = SYSUTCDATETIME()
FROM [INGREDIENT] ingredient
JOIN @new_ai_ingredients seed ON seed.name = ingredient.name
JOIN @new_ai_macros macros ON macros.name = ingredient.name
WHERE ingredient.source_name IN (N'Ước tính AI - chưa xác minh', N'Ước tính AI demo - chưa xác minh',
    N'Dữ liệu demo nội bộ', N'Dữ liệu demo nội bộ - chưa xác minh dinh dưỡng')
  AND (ingredient.nutrition_supported = 0 OR ingredient.calcium_mg_100g IS NULL
       OR ingredient.iron_mg_100g IS NULL OR ingredient.zinc_mg_100g IS NULL
       OR ingredient.energy_kcal_100g IS NULL OR ingredient.protein_g_100g IS NULL
       OR ingredient.carbohydrate_g_100g IS NULL OR ingredient.total_fat_g_100g IS NULL
       OR ingredient.fiber_g_100g IS NULL OR ingredient.vitamin_b12_mcg_100g IS NULL);

COMMIT TRANSACTION;
