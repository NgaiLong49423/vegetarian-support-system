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

-- Additional catalog items for the local lecturer demo.
DECLARE @showcase_ingredients TABLE (name NVARCHAR(200) PRIMARY KEY);
INSERT INTO @showcase_ingredients (name) VALUES
 (N'Mì Ý'), (N'Bánh phở'), (N'Bánh mì nguyên cám'), (N'Bột gạo'), (N'Bột nghệ'),
 (N'Nấm kim châm'), (N'Nấm bào ngư'), (N'Nấm mỡ'), (N'Đậu bắp'), (N'Dứa'),
 (N'Bắp cải'), (N'Giá đỗ'), (N'Hành lá'), (N'Hành tím'), (N'Gừng'),
 (N'Me chua'), (N'Đậu phộng'), (N'Mè trắng'), (N'Rong biển'), (N'Hạt chia'),
 (N'Xoài'), (N'Sữa đậu nành'), (N'Chả giò chay'), (N'Khoai môn'), (N'Rau cải thìa'),
 (N'Ngò rí'), (N'Nước tương'), (N'Chanh'), (N'Bột mì'), (N'Đậu xanh'),
 (N'Bí đỏ'), (N'Cà tím'), (N'Ớt đỏ'), (N'Đậu Hà Lan'), (N'Ngô hạt'),
 (N'Mì udon'), (N'Riềng'), (N'Bột năng'), (N'Rau ngót'), (N'Cải thảo'),
 (N'Quế'), (N'Hoa hồi'), (N'Đinh hương'), (N'Thảo quả'),
 (N'Ớt bột'), (N'Bột cà ri'), (N'Hạt thì là'), (N'Hạt mùi'), (N'Dầu ăn'),
 (N'Ớt'), (N'Nước'), (N'Đường ăn'), (N'Muối ăn'), (N'Hạt nêm'),
 (N'Phô mai mozzarella'), (N'Phô mai cheddar'), (N'Phô mai parmesan'),
 (N'Phô mai cream cheese'), (N'Phô mai cottage'), (N'Phô mai xanh'),
 (N'Phô mai chay đậu nành'), (N'Phô mai chay hạt điều');

INSERT INTO [INGREDIENT] (name, source_name, reference_date)
SELECT seed.name, N'Dữ liệu demo nội bộ', CONVERT(DATE, SYSUTCDATETIME())
FROM @showcase_ingredients seed
WHERE NOT EXISTS (SELECT 1 FROM [INGREDIENT] i WHERE i.name = seed.name);

-- Core recipe ingredients must exist before the nutrition catalog is enriched.
-- Core recipe ingredients and additional choices for the composer and admin catalog.
IF NOT EXISTS (SELECT 1 FROM [INGREDIENT] WHERE name = N'Nấm đùi gà')
    INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (N'Nấm đùi gà', N'Dữ liệu demo nội bộ', '2026-10-06');
IF NOT EXISTS (SELECT 1 FROM [INGREDIENT] WHERE name = N'Nấm hương')
    INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (N'Nấm hương', N'Dữ liệu demo nội bộ', '2026-10-06');
IF NOT EXISTS (SELECT 1 FROM [INGREDIENT] WHERE name = N'Cà rốt')
    INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (N'Cà rốt', N'Dữ liệu demo nội bộ', '2026-10-06');
IF NOT EXISTS (SELECT 1 FROM [INGREDIENT] WHERE name = N'Khoai lang')
    INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (N'Khoai lang', N'Dữ liệu demo nội bộ', '2026-10-06');
IF NOT EXISTS (SELECT 1 FROM [INGREDIENT] WHERE name = N'Bông cải xanh')
    INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (N'Bông cải xanh', N'Dữ liệu demo nội bộ', '2026-10-06');
IF NOT EXISTS (SELECT 1 FROM [INGREDIENT] WHERE name = N'Đậu lăng')
    INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (N'Đậu lăng', N'Dữ liệu demo nội bộ', '2026-10-06');

-- Values are per 100 g. Only verified USDA-backed rows are loaded as
-- nutrition-supported data; illustrative demo values remain unsupported.
DECLARE @demo_nutrition TABLE (
    ingredient_name NVARCHAR(200), energy_kcal DECIMAL(10,2), protein_g DECIMAL(10,2),
    carbohydrate_g DECIMAL(10,2), total_fat_g DECIMAL(10,2), fiber_g DECIMAL(10,2),
    calcium_mg DECIMAL(10,2), iron_mg DECIMAL(10,2), vitamin_b12_mcg DECIMAL(10,4),
    zinc_mg DECIMAL(10,2), fdc_id BIGINT, source_type VARCHAR(20)
);
INSERT INTO @demo_nutrition VALUES
 (N'Đậu hũ', 78, 9.04, 2.85, 4.17, 0.9, 201, 1.61, 0, 0.83, 172448, 'SR'),
 (N'Cà chua', 18, 0.88, 3.89, 0.2, 1.2, 10, 0.27, 0, 0.17, 170457, 'SR'),
 (N'Tỏi', 149, 6.36, 33.06, 0.5, 2.1, 181, 1.7, 0, 1.16, 169230, 'SR'),
 (N'Tiêu đen', 251, 10.39, 63.95, 3.26, 25.3, 443, 9.71, 0, 1.19, 170931, 'SR'),
 (N'Cà rốt', 41, 0.93, 9.58, 0.24, 2.8, 33, 0.3, 0, 0.24, 170393, 'SR'),
 (N'Khoai lang', 86, 1.57, 20.12, 0.05, 3, 30, 0.61, 0, 0.3, 168482, 'SR'),
 (N'Bông cải xanh', 34, 2.82, 6.64, 0.37, 2.6, 47, 0.73, 0, 0.41, 170379, 'SR'),
 (N'Đậu lăng', 352, 24.63, 63.35, 1.06, 10.7, 35, 6.51, 0, 3.27, 172420, 'SR'),
 (N'Mì Ý', 371, 13.04, 74.67, 1.51, 3.2, 21, 3.3, 0, 1.41, 169736, 'SR'),
 (N'Bánh phở', 364, 5.95, 80.18, 0.56, 1.6, 18, 0.7, 0, 0.74, 169742, 'SR'),
 (N'Bánh mì nguyên cám', 252, 12.45, 42.71, 3.5, 6, 161, 2.47, 0, 1.77, 172688, 'SR'),
 (N'Bột gạo', 366, 5.95, 80.13, 1.42, 2.4, 10, 0.35, 0, 0.8, 169714, 'SR'),
 (N'Bột nghệ', 312, 9.68, 67.14, 3.25, 22.7, 168, 55, 0, 4.5, 172231, 'SR'),
 (N'Nấm kim châm', 37, 2.66, 7.81, 0.29, 2.7, 0, 1.15, 0, 0.65, 169382, 'SR'),
 (N'Nấm bào ngư', 33, 3.31, 6.09, 0.41, 2.3, 3, 1.33, 0, 0.77, 168580, 'SR'),
 (N'Nấm mỡ', 22, 3.09, 3.26, 0.34, 1, 3, 0.5, 0.04, 0.52, 169251, 'SR'),
 (N'Đậu bắp', 33, 1.93, 7.45, 0.19, 3.2, 82, 0.62, 0, 0.58, 169260, 'SR'),
 (N'Dứa', 50, 0.54, 13.12, 0.12, 1.4, 13, 0.29, 0, 0.12, 169124, 'SR'),
 (N'Bắp cải', 25, 1.28, 5.8, 0.1, 2.5, 40, 0.47, 0, 0.18, 169975, 'SR'),
 (N'Giá đỗ', 30, 3.04, 5.94, 0.18, 1.8, 13, 0.91, 0, 0.41, 169957, 'SR'),
 (N'Hành lá', 32, 1.83, 7.34, 0.19, 2.6, 72, 1.48, 0, 0.39, 170005, 'SR'),
 (N'Hành tím', 72, 2.5, 16.8, 0.1, 3.2, 37, 1.2, 0, 0.4, 170499, 'SR'),
 (N'Gừng', 80, 1.82, 17.77, 0.75, 2, 16, 0.6, 0, 0.34, 169231, 'SR'),
 (N'Me chua', 239, 2.8, 62.5, 0.6, 5.1, 74, 2.8, 0, 0.1, 167763, 'SR'),
 (N'Đậu phộng', 567, 25.8, 16.13, 49.24, 8.5, 92, 4.58, 0, 3.27, 172430, 'SR'),
 (N'Mè trắng', 573, 17.73, 23.45, 49.67, 11.8, 975, 14.55, 0, 7.75, 170150, 'SR'),
 (N'Rong biển', 298, 31.8, 52.4, 4.01, 5.6, 372, 25, 0, 3.9, 2709988, 'FNDDS'),
 (N'Hạt chia', 486, 16.54, 42.12, 30.74, 34.4, 631, 7.72, 0, 4.58, 170554, 'SR'),
 (N'Xoài', 60, 0.82, 14.98, 0.38, 1.6, 11, 0.16, 0, 0.09, 169910, 'SR'),
 (N'Sữa đậu nành', 54, 3.27, 6.28, 1.75, 0.6, 25, 0.64, 0, 0.12, 172446, 'SR'),
 (N'Chả giò chay', 270, 5.62, 30, 14.1, 2.3, 47, 1.86, 0, 0.42, 2708700, 'FNDDS'),
 (N'Khoai môn', 112, 1.5, 26.46, 0.2, 4.1, 43, 0.55, 0, 0.23, 169308, 'SR'),
 (N'Rau cải thìa', 13, 1.5, 2.18, 0.2, 1, 105, 0.8, 0, 0.19, 170390, 'SR'),
 (N'Ngò rí', 23, 2.13, 3.67, 0.52, 2.8, 67, 1.77, 0, 0.5, 169997, 'SR'),
 (N'Nước tương', 53, 8.14, 4.93, 0.57, 0.8, 33, 1.45, 0, 0.87, 174277, 'SR'),
 (N'Chanh', 29, 1.1, 9.32, 0.3, 2.8, 26, 0.6, 0, 0.06, 167746, 'SR'),
 (N'Bột mì', 364, 10.33, 76.31, 0.98, 2.7, 15, 4.64, 0, 0.7, 168894, 'SR'),
 (N'Đậu xanh', 347, 23.86, 62.62, 1.15, 16.3, 132, 6.74, 0, 2.68, 174256, 'SR'),
 (N'Bí đỏ', 26, 1, 6.5, 0.1, 0.5, 21, 0.8, 0, 0.32, 168448, 'SR'),
 (N'Cà tím', 25, 0.98, 5.88, 0.18, 3, 9, 0.23, 0, 0.16, 169228, 'SR'),
 (N'Ớt đỏ', 40, 1.87, 8.81, 0.44, 1.5, 14, 1.03, 0, 0.26, 170106, 'SR'),
 (N'Đậu Hà Lan', 81, 5.42, 14.45, 0.4, 5.7, 25, 1.47, 0, 1.24, 170419, 'SR'),
 (N'Ngô hạt', 86, 3.27, 18.7, 1.35, 2, 2, 0.52, 0, 0.46, 169998, 'SR'),
 (N'Quế', 247, 4.0, 80.6, 1.2, 53.1, 1002, 8.3, 0, 1.8, NULL, 'AI_ESTIMATE'),
 (N'Hoa hồi', 337, 17.6, 50.0, 15.9, 14.6, 646, 37.0, 0, 5.3, NULL, 'AI_ESTIMATE'),
 (N'Đinh hương', 274, 6.0, 65.5, 13.0, 33.9, 632, 11.8, 0, 2.3, NULL, 'AI_ESTIMATE'),
 (N'Thảo quả', 311, 10.0, 68.0, 6.7, 28.0, 383, 14.0, 0, 7.5, NULL, 'AI_ESTIMATE'),
 (N'Ớt bột', 282, 14.1, 49.7, 13.8, 34.8, 330, 17.0, 0, 4.3, NULL, 'AI_ESTIMATE'),
 (N'Bột cà ri', 325, 14.0, 56.0, 14.0, 33.0, 478, 19.0, 0, 4.7, NULL, 'AI_ESTIMATE'),
 (N'Hạt thì là', 375, 17.8, 44.2, 22.3, 10.5, 931, 66.4, 0, 4.8, NULL, 'AI_ESTIMATE'),
 (N'Hạt mùi', 298, 12.4, 55.0, 17.8, 41.9, 709, 16.3, 0, 4.7, NULL, 'AI_ESTIMATE'),
 (N'Dầu ăn', 884, 0, 0, 100, 0, 0, 0, 0, 0, NULL, 'AI_ESTIMATE'),
 (N'Ớt', 40, 1.87, 8.81, 0.44, 1.5, 14, 1.03, 0, 0.26, NULL, 'AI_ESTIMATE'),
 (N'Nước', 0, 0, 0, 0, 0, 0, 0, 0, 0, NULL, 'AI_ESTIMATE'),
 (N'Đường ăn', 387, 0, 100, 0, 0, 0, 0, 0, 0, NULL, 'AI_ESTIMATE'),
 (N'Muối ăn', 0, 0, 0, 0, 0, 24, 0.33, 0, 0.1, 173468, 'SR'),
 (N'Hạt nêm', 300, 5, 35, 15, 0, 40, 1.5, 0, 0.6, NULL, 'AI_ESTIMATE'),
 (N'Phô mai mozzarella', 280, 28.0, 3.1, 17.1, 0, 731, 0.4, 2.3, 3.6, NULL, 'AI_ESTIMATE'),
 (N'Phô mai cheddar', 403, 22.9, 3.4, 33.3, 0, 710, 0.14, 1.1, 3.6, NULL, 'AI_ESTIMATE'),
 (N'Phô mai parmesan', 431, 38.0, 4.1, 29.0, 0, 1184, 0.82, 1.2, 2.75, NULL, 'AI_ESTIMATE'),
 (N'Phô mai cream cheese', 342, 5.9, 4.1, 34.2, 0, 98, 0.38, 0.25, 0.51, NULL, 'AI_ESTIMATE'),
 (N'Phô mai cottage', 98, 11.1, 3.4, 4.3, 0, 83, 0.07, 0.43, 0.4, NULL, 'AI_ESTIMATE'),
 (N'Phô mai xanh', 353, 21.4, 2.3, 28.7, 0, 528, 0.31, 0, 2.66, NULL, 'AI_ESTIMATE'),
 (N'Phô mai chay đậu nành', 290, 20.0, 8.0, 21.0, 2.0, 350, 3.5, 0, 2.5, NULL, 'AI_ESTIMATE'),
 (N'Phô mai chay hạt điều', 330, 10.0, 14.0, 27.0, 2.0, 60, 2.5, 0, 2.0, NULL, 'AI_ESTIMATE'),
 (N'Rau muống', 19, 2.6, 3.1, 0.2, 2.1, 77, 1.7, 0, 0.4, NULL, 'AI_ESTIMATE'),
 (N'Nấm đùi gà', 35, 3.3, 6.2, 0.4, 2.3, 3, 1.3, 0, 0.8, NULL, 'AI_ESTIMATE'),
 (N'Nấm hương', 34, 2.2, 6.8, 0.5, 2.5, 2, 0.4, 0, 1.0, NULL, 'AI_ESTIMATE'),
 (N'Mì udon', 127, 3.4, 25.2, 0.6, 1.0, 10, 0.6, 0, 0.5, NULL, 'AI_ESTIMATE'),
 (N'Riềng', 71, 1.5, 15.0, 0.9, 2.0, 34, 0.9, 0, 0.3, NULL, 'AI_ESTIMATE'),
 (N'Bột năng', 358, 0.2, 88.7, 0.0, 0.9, 20, 1.6, 0, 0.1, NULL, 'AI_ESTIMATE'),
 (N'Rau ngót', 35, 5.0, 6.0, 0.4, 2.5, 230, 3.0, 0, 1.0, NULL, 'AI_ESTIMATE'),
 (N'Cải thảo', 16, 1.2, 3.2, 0.2, 1.2, 40, 0.5, 0, 0.2, NULL, 'AI_ESTIMATE');

UPDATE i
SET energy_kcal_100g = seed.energy_kcal, protein_g_100g = seed.protein_g,
    carbohydrate_g_100g = seed.carbohydrate_g, total_fat_g_100g = seed.total_fat_g,
    fiber_g_100g = seed.fiber_g, calcium_mg_100g = seed.calcium_mg,
    iron_mg_100g = seed.iron_mg, vitamin_b12_mcg_100g = seed.vitamin_b12_mcg,
    zinc_mg_100g = seed.zinc_mg,
    ingredient_group = CASE
        WHEN seed.ingredient_name IN
            (N'Quế', N'Hoa hồi', N'Đinh hương', N'Thảo quả', N'Ớt bột', N'Bột cà ri', N'Hạt thì là', N'Hạt mùi', N'Dầu ăn', N'Ớt', N'Tiêu đen', N'Đường ăn', N'Muối ăn', N'Hạt nêm')
            THEN N'Gia vị chay'
        WHEN seed.ingredient_name = N'Phô mai chay đậu nành' THEN N'Đậu & Chế phẩm'
        WHEN seed.ingredient_name = N'Phô mai chay hạt điều' THEN N'Các loại hạt'
        ELSE i.ingredient_group END,
    source_name = CASE WHEN seed.source_type = 'AI_ESTIMATE' THEN N'Ước tính AI - chưa xác minh'
        WHEN seed.source_type = 'FNDDS' THEN N'USDA FoodData Central (FNDDS)'
        ELSE N'USDA FoodData Central (Standard Reference)' END,
    source_url = CASE WHEN seed.source_type = 'AI_ESTIMATE' THEN NULL
        ELSE CONCAT('https://fdc.nal.usda.gov/food-details/', seed.fdc_id, '/nutrients') END,
    reference_date = '2026-10-13',
    -- BR-52 / V17: stored AI estimates can participate, with their source label retained.
    nutrition_supported = 1,
    updated_at = SYSUTCDATETIME()
FROM [INGREDIENT] i
JOIN @demo_nutrition seed ON seed.ingredient_name = i.name
WHERE i.source_name IN (N'Dữ liệu demo nội bộ', N'Ước tính AI demo - chưa xác minh', N'Ước tính AI - chưa xác minh');

-- Keep the existing pepper fixture in the matching spice group on repeatable reruns.
UPDATE [INGREDIENT]
SET ingredient_group = N'Gia vị chay', updated_at = SYSUTCDATETIME()
WHERE name = N'Tiêu đen'
  AND ingredient_group = N'Khác'
  AND source_name = N'USDA FoodData Central (Standard Reference)';

-- 21 additional recipes and 85 unique catalogue ingredients for local demos.
DECLARE @extra_ingredients TABLE (name NVARCHAR(200) PRIMARY KEY);
INSERT INTO @extra_ingredients (name) VALUES
    (N'Gạo lứt'), (N'Yến mạch cán'), (N'Hạt quinoa'), (N'Hạt điều'), (N'Hạnh nhân'),
    (N'Óc chó'), (N'Hạt bí'), (N'Hạt hướng dương'), (N'Đậu nành hạt'), (N'Đậu đỏ'),
    (N'Đậu gà'), (N'Đậu trắng'), (N'Tempeh'), (N'Seitan'), (N'Tảo bẹ kombu'),
    (N'Nấm mộc nhĩ'), (N'Măng tây'), (N'Bí ngòi'), (N'Súp lơ trắng'), (N'Cải xoăn'),
    (N'Xà lách romaine'), (N'Dưa leo'), (N'Ớt chuông'), (N'Bơ'), (N'Lựu'),
    (N'Táo'), (N'Chuối'), (N'Dâu tây'), (N'Cam'), (N'Khoai tây'),
    (N'Mướp hương'), (N'Rau dền'), (N'Nấm rơm'), (N'Hạt sen'), (N'Bí đao'),
    (N'Gạo tẻ'), (N'Gạo nếp'), (N'Hạt lanh'), (N'Mè đen'), (N'Hạt phỉ'),
    (N'Hạt dẻ cười'), (N'Hạt mắc ca'), (N'Hạt dẻ'), (N'Đậu đen'), (N'Đậu nành non'),
    (N'Đậu lăng đỏ'), (N'Đậu ván'), (N'Đậu ngự'), (N'Đậu phụ non'), (N'Đậu hũ ky'),
    (N'Chao'), (N'Tương miso'), (N'Bắp non'), (N'Su su'), (N'Củ cải trắng'),
    (N'Củ dền'), (N'Củ sen'), (N'Củ sắn'), (N'Khoai sọ'), (N'Rau mồng tơi'),
    (N'Cải bó xôi'), (N'Cải bẹ xanh'), (N'Cải ngọt'), (N'Bầu'), (N'Khổ qua'),
    (N'Dọc mùng'), (N'Cần tây'), (N'Cần ta'), (N'Rau má'), (N'Rau lang'),
    (N'Lá lốt'), (N'Húng quế'), (N'Húng lủi'), (N'Tía tô'), (N'Sả'),
    (N'Hành tây'), (N'Hành boa rô'), (N'Nghệ tươi'), (N'Chanh dây'), (N'Ổi'),
    (N'Lê'), (N'Nho'), (N'Thanh long'), (N'Đu đủ'), (N'Dưa hấu');

INSERT INTO [INGREDIENT] (name, source_name, reference_date, nutrition_supported)
SELECT seed.name, N'Dữ liệu demo nội bộ - chưa xác minh dinh dưỡng', CONVERT(DATE, SYSUTCDATETIME()), 0
FROM @extra_ingredients seed
WHERE NOT EXISTS (SELECT 1 FROM [INGREDIENT] i WHERE i.name = seed.name);

-- AI-inferred nutrition estimates per 100 g for local demo only.
-- Micronutrients and macros are illustrative and unverified.
DECLARE @ai_demo_micronutrients TABLE (
    ingredient_name NVARCHAR(200) PRIMARY KEY,
    calcium_mg DECIMAL(10,2), iron_mg DECIMAL(10,2), zinc_mg DECIMAL(10,2)
);
INSERT INTO @ai_demo_micronutrients VALUES
 (N'Gạo lứt', 23, 1.47, 1.8), (N'Yến mạch cán', 54, 4.72, 3.97),
 (N'Hạt quinoa', 17, 4.57, 3.1), (N'Hạt điều', 37, 6.68, 5.78),
 (N'Hạnh nhân', 269, 3.71, 3.12), (N'Óc chó', 98, 2.91, 3.09),
 (N'Hạt bí', 46, 8.82, 7.64), (N'Hạt hướng dương', 78, 5.25, 5.0),
 (N'Đậu nành hạt', 277, 15.7, 4.9), (N'Đậu đỏ', 66, 5.0, 2.9),
 (N'Đậu gà', 105, 6.2, 3.4), (N'Đậu trắng', 240, 5.0, 3.6),
 (N'Tempeh', 111, 2.7, 1.1), (N'Seitan', 14, 5.2, 1.6),
 (N'Tảo bẹ kombu', 168, 2.85, 1.23), (N'Nấm mộc nhĩ', 159, 5.9, 1.1),
 (N'Măng tây', 24, 2.14, 0.54), (N'Bí ngòi', 16, 0.37, 0.32),
 (N'Súp lơ trắng', 22, 0.42, 0.27), (N'Cải xoăn', 254, 1.47, 0.39),
 (N'Xà lách romaine', 33, 0.97, 0.23), (N'Dưa leo', 16, 0.28, 0.2),
 (N'Ớt chuông', 7, 0.43, 0.25), (N'Bơ', 12, 0.55, 0.64),
 (N'Lựu', 10, 0.3, 0.35), (N'Táo', 6, 0.12, 0.04),
 (N'Chuối', 5, 0.26, 0.15), (N'Dâu tây', 16, 0.41, 0.14),
 (N'Cam', 40, 0.1, 0.07), (N'Khoai tây', 12, 0.81, 0.3),
 (N'Mướp hương', 20, 0.36, 0.07), (N'Rau dền', 215, 2.32, 0.9),
 (N'Nấm rơm', 10, 1.0, 0.6), (N'Hạt sen', 44, 1.4, 2.8),
 (N'Bí đao', 19, 0.4, 0.61),
 (N'Gạo tẻ', 10, 0.8, 1.1), (N'Gạo nếp', 10, 0.8, 1.2),
 (N'Hạt lanh', 255, 5.7, 4.3), (N'Mè đen', 975, 14.6, 7.8),
 (N'Hạt phỉ', 114, 4.7, 2.5), (N'Hạt dẻ cười', 105, 3.9, 2.2),
 (N'Hạt mắc ca', 85, 3.7, 1.3), (N'Hạt dẻ', 27, 0.9, 0.5),
 (N'Đậu đen', 123, 5.0, 3.7), (N'Đậu nành non', 63, 2.3, 1.4),
 (N'Đậu lăng đỏ', 35, 6.5, 3.3), (N'Đậu ván', 60, 3.0, 1.0),
 (N'Đậu ngự', 54, 2.5, 0.9), (N'Đậu phụ non', 111, 1.5, 0.8),
 (N'Đậu hũ ky', 201, 5.6, 3.3), (N'Chao', 30, 1.2, 0.3),
 (N'Tương miso', 57, 2.5, 0.4), (N'Bắp non', 2, 0.5, 0.5),
 (N'Su su', 17, 0.4, 0.7), (N'Củ cải trắng', 27, 0.4, 0.3),
 (N'Củ dền', 16, 0.8, 0.4), (N'Củ sen', 45, 1.2, 0.4),
 (N'Củ sắn', 16, 0.3, 0.3), (N'Khoai sọ', 43, 0.6, 0.3),
 (N'Rau mồng tơi', 109, 1.2, 0.4), (N'Cải bó xôi', 99, 2.7, 0.5),
 (N'Cải bẹ xanh', 115, 1.6, 0.3), (N'Cải ngọt', 105, 1.5, 0.3),
 (N'Bầu', 26, 0.3, 0.2), (N'Khổ qua', 19, 0.4, 0.8),
 (N'Dọc mùng', 44, 0.9, 0.2), (N'Cần tây', 40, 0.2, 0.1),
 (N'Cần ta', 40, 1.0, 0.3), (N'Rau má', 48, 3.0, 0.4),
 (N'Rau lang', 99, 1.2, 0.3), (N'Lá lốt', 200, 3.0, 0.5),
 (N'Húng quế', 177, 3.2, 0.8), (N'Húng lủi', 243, 5.1, 1.1),
 (N'Tía tô', 230, 3.0, 1.1), (N'Sả', 65, 2.0, 0.1),
 (N'Hành tây', 23, 0.2, 0.2), (N'Hành boa rô', 59, 2.1, 0.1),
 (N'Nghệ tươi', 12, 2.8, 0.3), (N'Chanh dây', 12, 1.6, 0.1),
 (N'Ổi', 18, 0.3, 0.2), (N'Lê', 9, 0.2, 0.1),
 (N'Nho', 10, 0.4, 0.1), (N'Thanh long', 8, 1.9, 0.1),
 (N'Đu đủ', 20, 0.3, 0.1), (N'Dưa hấu', 7, 0.2, 0.1);

DECLARE @ai_demo_macros TABLE (
    ingredient_name NVARCHAR(200) PRIMARY KEY,
    energy_kcal DECIMAL(10,2), protein_g DECIMAL(10,2), carbohydrate_g DECIMAL(10,2),
    total_fat_g DECIMAL(10,2), fiber_g DECIMAL(10,2), vitamin_b12_mcg DECIMAL(10,4)
);
INSERT INTO @ai_demo_macros VALUES
 (N'Gạo lứt', 367, 7.5, 76.2, 3.2, 3.6, 0), (N'Yến mạch cán', 389, 16.9, 66.3, 6.9, 10.6, 0),
 (N'Hạt quinoa', 368, 14.1, 64.2, 6.1, 7.0, 0), (N'Hạt điều', 553, 18.2, 30.2, 43.9, 3.3, 0),
 (N'Hạnh nhân', 579, 21.2, 21.6, 49.9, 12.5, 0), (N'Óc chó', 654, 15.2, 13.7, 65.2, 6.7, 0),
 (N'Hạt bí', 559, 30.2, 10.7, 49.1, 6.0, 0), (N'Hạt hướng dương', 584, 20.8, 20.0, 51.5, 8.6, 0),
 (N'Đậu nành hạt', 446, 36.5, 30.2, 19.9, 9.3, 0), (N'Đậu đỏ', 329, 19.9, 62.9, 0.5, 12.7, 0),
 (N'Đậu gà', 364, 19.3, 60.7, 6.0, 17.4, 0), (N'Đậu trắng', 337, 21.1, 62.3, 1.6, 15.2, 0),
 (N'Tempeh', 193, 20.3, 7.6, 10.8, 1.4, 0), (N'Seitan', 141, 24.0, 7.0, 2.0, 0.6, 0),
 (N'Tảo bẹ kombu', 43, 1.7, 9.6, 0.6, 1.3, 0), (N'Nấm mộc nhĩ', 284, 9.3, 73.0, 0.7, 70.0, 0),
 (N'Măng tây', 20, 2.2, 3.9, 0.1, 2.1, 0), (N'Bí ngòi', 17, 1.2, 3.1, 0.3, 1.0, 0),
 (N'Súp lơ trắng', 25, 1.9, 5.0, 0.3, 2.0, 0), (N'Cải xoăn', 35, 2.9, 4.4, 1.5, 4.1, 0),
 (N'Xà lách romaine', 17, 1.2, 3.3, 0.3, 2.1, 0), (N'Dưa leo', 15, 0.7, 3.6, 0.1, 0.5, 0),
 (N'Ớt chuông', 31, 1.0, 6.0, 0.3, 2.1, 0), (N'Bơ', 160, 2.0, 8.5, 14.7, 6.7, 0),
 (N'Lựu', 83, 1.7, 18.7, 1.2, 4.0, 0), (N'Táo', 52, 0.3, 13.8, 0.2, 2.4, 0),
 (N'Chuối', 89, 1.1, 22.8, 0.3, 2.6, 0), (N'Dâu tây', 32, 0.7, 7.7, 0.3, 2.0, 0),
 (N'Cam', 47, 0.9, 11.8, 0.1, 2.4, 0), (N'Khoai tây', 77, 2.0, 17.5, 0.1, 2.2, 0),
 (N'Mướp hương', 20, 0.8, 4.4, 0.2, 1.0, 0), (N'Rau dền', 23, 2.5, 4.0, 0.3, 2.1, 0),
 (N'Nấm rơm', 28, 3.7, 4.4, 0.3, 2.0, 0), (N'Hạt sen', 332, 15.4, 64.5, 2.0, 8.0, 0),
 (N'Bí đao', 13, 0.4, 3.0, 0.2, 0.5, 0), (N'Gạo tẻ', 365, 7.1, 80.0, 0.7, 1.3, 0),
 (N'Gạo nếp', 370, 6.8, 81.7, 0.6, 1.0, 0), (N'Hạt lanh', 534, 18.3, 28.9, 42.2, 27.3, 0),
 (N'Mè đen', 573, 17.7, 23.5, 49.7, 11.8, 0), (N'Hạt phỉ', 628, 15.0, 16.7, 60.8, 9.7, 0),
 (N'Hạt dẻ cười', 560, 20.2, 27.2, 45.3, 10.6, 0), (N'Hạt mắc ca', 718, 7.9, 13.8, 75.8, 8.6, 0),
 (N'Hạt dẻ', 213, 2.4, 45.5, 2.3, 8.1, 0), (N'Đậu đen', 341, 21.6, 62.4, 1.4, 15.5, 0),
 (N'Đậu nành non', 121, 12.0, 8.9, 5.2, 5.2, 0), (N'Đậu lăng đỏ', 358, 23.9, 63.1, 1.1, 10.8, 0),
 (N'Đậu ván', 343, 21.5, 60.0, 1.5, 10.0, 0), (N'Đậu ngự', 338, 21.5, 62.0, 0.7, 15.0, 0),
 (N'Đậu phụ non', 55, 4.8, 2.7, 2.7, 0.3, 0), (N'Đậu hũ ky', 477, 50.0, 18.8, 26.0, 3.0, 0),
 (N'Chao', 116, 8.0, 9.0, 5.0, 1.0, 0), (N'Tương miso', 199, 12.0, 26.0, 6.0, 5.0, 0),
 (N'Bắp non', 26, 1.0, 5.8, 0.2, 2.0, 0), (N'Su su', 19, 0.8, 4.5, 0.1, 1.7, 0),
 (N'Củ cải trắng', 18, 0.6, 4.1, 0.1, 1.6, 0), (N'Củ dền', 43, 1.6, 9.6, 0.2, 2.8, 0),
 (N'Củ sen', 74, 2.6, 17.2, 0.1, 4.9, 0), (N'Củ sắn', 160, 1.4, 38.1, 0.3, 1.8, 0),
 (N'Khoai sọ', 112, 1.5, 26.5, 0.2, 4.1, 0), (N'Rau mồng tơi', 19, 1.8, 3.4, 0.3, 2.1, 0),
 (N'Cải bó xôi', 23, 2.9, 3.6, 0.4, 2.2, 0), (N'Cải bẹ xanh', 27, 2.9, 4.7, 0.4, 3.2, 0),
 (N'Cải ngọt', 20, 1.6, 3.2, 0.2, 1.8, 0), (N'Bầu', 14, 0.6, 3.4, 0.0, 0.5, 0),
 (N'Khổ qua', 17, 1.0, 3.7, 0.2, 2.8, 0), (N'Dọc mùng', 16, 0.7, 3.0, 0.1, 1.0, 0),
 (N'Cần tây', 14, 0.7, 3.0, 0.2, 1.6, 0), (N'Cần ta', 20, 1.5, 3.5, 0.2, 1.5, 0),
 (N'Rau má', 20, 2.0, 3.3, 0.2, 2.0, 0), (N'Rau lang', 35, 2.5, 6.0, 0.3, 2.5, 0),
 (N'Lá lốt', 30, 2.0, 5.0, 0.5, 2.0, 0), (N'Húng quế', 23, 3.2, 2.7, 0.6, 1.6, 0),
 (N'Húng lủi', 44, 3.3, 8.4, 0.7, 6.8, 0), (N'Tía tô', 37, 3.0, 7.0, 0.5, 3.0, 0),
 (N'Sả', 99, 1.8, 25.3, 0.5, 0.0, 0), (N'Hành tây', 40, 1.1, 9.3, 0.1, 1.7, 0),
 (N'Hành boa rô', 61, 1.5, 14.2, 0.3, 1.8, 0), (N'Nghệ tươi', 73, 1.8, 15.0, 0.7, 2.0, 0),
 (N'Chanh dây', 97, 2.2, 23.4, 0.7, 10.4, 0), (N'Ổi', 68, 2.6, 14.3, 1.0, 5.4, 0),
 (N'Lê', 57, 0.4, 15.2, 0.1, 3.1, 0), (N'Nho', 69, 0.7, 18.1, 0.2, 0.9, 0),
 (N'Thanh long', 57, 1.2, 13.2, 0.0, 3.0, 0), (N'Đu đủ', 43, 0.5, 10.8, 0.3, 1.7, 0),
 (N'Dưa hấu', 30, 0.6, 7.6, 0.2, 0.4, 0);

UPDATE i
SET energy_kcal_100g = macros.energy_kcal, protein_g_100g = macros.protein_g,
    carbohydrate_g_100g = macros.carbohydrate_g, total_fat_g_100g = macros.total_fat_g,
    fiber_g_100g = macros.fiber_g, calcium_mg_100g = seed.calcium_mg,
    iron_mg_100g = seed.iron_mg, zinc_mg_100g = seed.zinc_mg,
    vitamin_b12_mcg_100g = macros.vitamin_b12_mcg,
    source_name = N'Ước tính AI - chưa xác minh', source_url = NULL,
    reference_date = '2026-10-20', nutrition_supported = 1,
    updated_at = SYSUTCDATETIME()
FROM [INGREDIENT] i
JOIN @ai_demo_micronutrients seed ON seed.ingredient_name = i.name
JOIN @ai_demo_macros macros ON macros.ingredient_name = i.name
WHERE i.source_name IN (N'Dữ liệu demo nội bộ - chưa xác minh dinh dưỡng', N'Ước tính AI demo - chưa xác minh', N'Ước tính AI - chưa xác minh');

COMMIT TRANSACTION;
