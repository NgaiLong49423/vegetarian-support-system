-- Dữ liệu demo giả cho môi trường phát triển Docker.
-- Script có thể chạy lại an toàn: chỉ thêm fixture còn thiếu, không sửa dữ liệu người dùng.
-- Tài khoản tác giả demo không có mật khẩu và không dùng để đăng nhập.
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF NOT EXISTS (SELECT 1 FROM [USER] WHERE email = 'demo-expert@mamxanh.local')
BEGIN
    INSERT INTO [USER] (email, password_hash, display_name, role, email_verified)
    VALUES ('demo-expert@mamxanh.local', NULL, N'Chuyên gia mẫu', 'EXPERT', 0);
END;

DECLARE @author_id BIGINT;
SELECT @author_id = user_id FROM [USER] WHERE email = 'demo-expert@mamxanh.local';

IF NOT EXISTS (SELECT 1 FROM [INGREDIENT] WHERE name = N'Đậu hũ')
    INSERT INTO [INGREDIENT] (name, source_name, reference_date)
    VALUES (N'Đậu hũ', N'Dữ liệu demo nội bộ', '2026-10-02');

IF NOT EXISTS (SELECT 1 FROM [INGREDIENT] WHERE name = N'Cà chua')
    INSERT INTO [INGREDIENT] (name, source_name, reference_date)
    VALUES (N'Cà chua', N'Dữ liệu demo nội bộ', '2026-10-02');

IF NOT EXISTS (SELECT 1 FROM [INGREDIENT] WHERE name = N'Rau muống')
    INSERT INTO [INGREDIENT] (name, source_name, reference_date)
    VALUES (N'Rau muống', N'Dữ liệu demo nội bộ', '2026-10-02');

IF NOT EXISTS (SELECT 1 FROM [INGREDIENT] WHERE name = N'Tỏi')
    INSERT INTO [INGREDIENT] (name, source_name, reference_date)
    VALUES (N'Tỏi', N'Dữ liệu demo nội bộ', '2026-10-02');

IF NOT EXISTS (SELECT 1 FROM [INGREDIENT] WHERE name = N'Tiêu đen')
    INSERT INTO [INGREDIENT] (name, source_name, reference_date)
    VALUES (N'Tiêu đen', N'Dữ liệu demo nội bộ', '2026-10-02');

DECLARE @tofu BIGINT, @tomato BIGINT, @water_spinach BIGINT, @garlic BIGINT;
DECLARE @pepper BIGINT;
DECLARE @gram INT, @clove INT, @block INT, @fruit INT, @bunch INT, @teaspoon INT;
SELECT @tofu = ingredient_id FROM [INGREDIENT] WHERE name = N'Đậu hũ';
SELECT @tomato = ingredient_id FROM [INGREDIENT] WHERE name = N'Cà chua';
SELECT @water_spinach = ingredient_id FROM [INGREDIENT] WHERE name = N'Rau muống';
SELECT @garlic = ingredient_id FROM [INGREDIENT] WHERE name = N'Tỏi';
SELECT @pepper = ingredient_id FROM [INGREDIENT] WHERE name = N'Tiêu đen';
SELECT @gram = unit_id FROM [UNIT] WHERE code = N'g';
SELECT @clove = unit_id FROM [UNIT] WHERE code = N'nhánh';
SELECT @block = unit_id FROM [UNIT] WHERE code = N'bìa';
SELECT @fruit = unit_id FROM [UNIT] WHERE code = N'quả';
SELECT @bunch = unit_id FROM [UNIT] WHERE code = N'bó';
SELECT @teaspoon = unit_id FROM [UNIT] WHERE code = N'tsp';

-- Conversion fixtures are ingredient-specific and idempotent for shared Docker testing.
IF NOT EXISTS (SELECT 1 FROM [INGREDIENT_UNIT_CONVERSION] WHERE ingredient_id = @tofu AND unit_id = @block)
    INSERT INTO [INGREDIENT_UNIT_CONVERSION] (ingredient_id, unit_id, grams_per_unit, is_approximate)
    VALUES (@tofu, @block, 150, 1);

IF NOT EXISTS (SELECT 1 FROM [INGREDIENT_UNIT_CONVERSION] WHERE ingredient_id = @tomato AND unit_id = @fruit)
    INSERT INTO [INGREDIENT_UNIT_CONVERSION] (ingredient_id, unit_id, grams_per_unit, is_approximate)
    VALUES (@tomato, @fruit, 120, 1);

IF NOT EXISTS (SELECT 1 FROM [INGREDIENT_UNIT_CONVERSION] WHERE ingredient_id = @water_spinach AND unit_id = @bunch)
    INSERT INTO [INGREDIENT_UNIT_CONVERSION] (ingredient_id, unit_id, grams_per_unit, is_approximate)
    VALUES (@water_spinach, @bunch, 300, 1);

IF NOT EXISTS (SELECT 1 FROM [INGREDIENT_UNIT_CONVERSION] WHERE ingredient_id = @garlic AND unit_id = @clove)
    INSERT INTO [INGREDIENT_UNIT_CONVERSION] (ingredient_id, unit_id, grams_per_unit, is_approximate)
    VALUES (@garlic, @clove, 3, 1);

IF NOT EXISTS (SELECT 1 FROM [INGREDIENT_UNIT_CONVERSION] WHERE ingredient_id = @pepper AND unit_id = @teaspoon)
    INSERT INTO [INGREDIENT_UNIT_CONVERSION] (ingredient_id, unit_id, grams_per_unit, is_approximate)
    VALUES (@pepper, @teaspoon, 2, 1);

IF NOT EXISTS (
    SELECT 1 FROM [RECIPE_POST]
    WHERE author_id = @author_id AND title = N'Đậu hũ sốt cà chua'
)
BEGIN
    INSERT INTO [RECIPE_POST] (
        author_id, title, description, instructions, dish_category, vegetarian_type,
        difficulty, servings, prep_time_min, cook_time_min, published_at
    )
    VALUES (
        @author_id, N'Đậu hũ sốt cà chua', N'Món chay đơn giản cho bữa cơm gia đình.',
        N'Cắt đậu hũ thành miếng vừa ăn. Nấu cà chua với gia vị, cho đậu hũ vào rim đến khi thấm sốt.',
        'BRAISED', 'VEGAN', 'EASY', 2, 10, 15, SYSUTCDATETIME()
    );
END;

DECLARE @tofu_recipe BIGINT;
SELECT @tofu_recipe = recipe_id FROM [RECIPE_POST]
WHERE author_id = @author_id AND title = N'Đậu hũ sốt cà chua';

IF NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] WHERE recipe_id = @tofu_recipe)
BEGIN
    INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
    VALUES (@tofu_recipe, @tofu, @gram, 300), (@tofu_recipe, @tomato, @gram, 200);
END;

IF NOT EXISTS (
    SELECT 1 FROM [RECIPE_POST]
    WHERE author_id = @author_id AND title = N'Rau muống xào tỏi'
)
BEGIN
    INSERT INTO [RECIPE_POST] (
        author_id, title, description, instructions, dish_category, vegetarian_type,
        difficulty, servings, prep_time_min, cook_time_min, published_at
    )
    VALUES (
        @author_id, N'Rau muống xào tỏi', N'Món xào nhanh với rau xanh và tỏi.',
        N'Nhặt và rửa rau muống. Phi thơm tỏi, cho rau vào xào nhanh với gia vị đến khi vừa chín.',
        'STIR_FRY', 'VEGAN', 'EASY', 2, 5, 7, SYSUTCDATETIME()
    );
END;

DECLARE @spinach_recipe BIGINT;
SELECT @spinach_recipe = recipe_id FROM [RECIPE_POST]
WHERE author_id = @author_id AND title = N'Rau muống xào tỏi';

IF NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] WHERE recipe_id = @spinach_recipe)
BEGIN
    INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
    VALUES (@spinach_recipe, @water_spinach, @gram, 300), (@spinach_recipe, @garlic, @clove, 3);
END;

-- Issue #47: a second account's meal slot references the demo recipe. The account has no
-- password and cannot sign in; the fixture only provides repeatable database/API test data.
IF NOT EXISTS (SELECT 1 FROM [USER] WHERE email = 'demo-customer@mamxanh.local')
BEGIN
    INSERT INTO [USER] (email, password_hash, display_name, role, email_verified)
    VALUES ('demo-customer@mamxanh.local', NULL, N'Khách hàng mẫu', 'CUSTOMER', 0);
END;

DECLARE @customer_id BIGINT;
DECLARE @sample_week_start DATE = CONVERT(DATE, SYSUTCDATETIME());
DECLARE @sample_meal_plan_id BIGINT;
SELECT @customer_id = user_id FROM [USER] WHERE email = 'demo-customer@mamxanh.local';
SET @sample_week_start = DATEADD(DAY,
    -(DATEDIFF(DAY, CONVERT(DATE, '19000101', 112), @sample_week_start) % 7), @sample_week_start);

IF NOT EXISTS (SELECT 1 FROM [MEAL_PLAN] WHERE user_id = @customer_id AND week_start_date = @sample_week_start)
BEGIN
    INSERT INTO [MEAL_PLAN] (user_id, week_start_date) VALUES (@customer_id, @sample_week_start);
END;
SELECT @sample_meal_plan_id = meal_plan_id FROM [MEAL_PLAN]
WHERE user_id = @customer_id AND week_start_date = @sample_week_start;

IF NOT EXISTS (
    SELECT 1 FROM [MEAL_PLAN_ENTRY]
    WHERE meal_plan_id = @sample_meal_plan_id AND recipe_id = @tofu_recipe
        AND meal_date = DATEADD(DAY, 1, @sample_week_start) AND meal_type = 'LUNCH'
)
BEGIN
    INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id, recipe_id, meal_date, meal_type, planned_servings)
    VALUES (@sample_meal_plan_id, @tofu_recipe, DATEADD(DAY, 1, @sample_week_start), 'LUNCH', 2);
END;

COMMIT TRANSACTION;
