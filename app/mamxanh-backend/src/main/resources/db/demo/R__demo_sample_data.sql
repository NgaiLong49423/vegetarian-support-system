-- Repeatable demo fixtures for local development only. Password hashes are
-- initialized at runtime from MAMXANH_DEMO_PASSWORD and are never stored here.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF NOT EXISTS (SELECT 1 FROM [USER] WHERE email = 'demo-expert@mamxanh.local')
BEGIN
    INSERT INTO [USER] (email, password_hash, display_name, role, email_verified)
    VALUES ('demo-expert@mamxanh.local', NULL, N'Nguyễn Tuệ Tâm', 'EXPERT', 0);
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

IF NOT EXISTS (SELECT 1 FROM [RECIPE_POST] WHERE author_id = @author_id AND title = N'Đậu hũ sốt cà chua')
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
    INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
    VALUES (@tofu_recipe, @tofu, @gram, 300), (@tofu_recipe, @tomato, @gram, 200);

IF NOT EXISTS (SELECT 1 FROM [RECIPE_POST] WHERE author_id = @author_id AND title = N'Rau muống xào tỏi')
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
    INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
    VALUES (@spinach_recipe, @water_spinach, @gram, 300), (@spinach_recipe, @garlic, @clove, 3);

IF NOT EXISTS (SELECT 1 FROM [USER] WHERE email = 'demo-customer@mamxanh.local')
BEGIN
    INSERT INTO [USER] (email, password_hash, display_name, role, email_verified)
    VALUES ('demo-customer@mamxanh.local', NULL, N'Nguyễn Minh An', 'CUSTOMER', 0);
END;

IF NOT EXISTS (SELECT 1 FROM [USER] WHERE email = 'demo-new-member@mamxanh.local')
    INSERT INTO [USER] (email, display_name, role, email_verified)
    VALUES ('demo-new-member@mamxanh.local', N'Trần Mai Anh', 'CUSTOMER', 1);
IF NOT EXISTS (SELECT 1 FROM [USER] WHERE email = 'demo-applicant@mamxanh.local')
    INSERT INTO [USER] (email, display_name, role, email_verified)
    VALUES ('demo-applicant@mamxanh.local', N'Lê Thanh Sơn', 'CUSTOMER', 1);
IF NOT EXISTS (SELECT 1 FROM [USER] WHERE email = 'demo-admin@mamxanh.local')
    INSERT INTO [USER] (email, display_name, role, email_verified)
    VALUES ('demo-admin@mamxanh.local', N'Lê Hoàng Nam', 'ADMIN', 1);

-- Additional fixed local demo identities; credentials are injected at runtime.
INSERT INTO [USER] (email, display_name, role, email_verified)
SELECT seed.email, seed.display_name, seed.role, 1
FROM (VALUES
    ('demo-customer-04@mamxanh.local', N'Phạm Gia Hân', 'CUSTOMER'),
    ('demo-customer-05@mamxanh.local', N'Võ Minh Khang', 'CUSTOMER'),
    ('demo-customer-06@mamxanh.local', N'Đặng Ngọc Mai', 'CUSTOMER'),
    ('demo-customer-07@mamxanh.local', N'Bùi Quốc Bảo', 'CUSTOMER'),
    ('demo-customer-08@mamxanh.local', N'Ngô Thảo Vy', 'CUSTOMER'),
    ('demo-customer-09@mamxanh.local', N'Đỗ Nhật Minh', 'CUSTOMER'),
    ('demo-customer-10@mamxanh.local', N'Hoàng Khánh Linh', 'CUSTOMER'),
    ('demo-customer-11@mamxanh.local', N'Phan Anh Tuấn', 'CUSTOMER'),
    ('demo-customer-12@mamxanh.local', N'Vũ Hải Yến', 'CUSTOMER'),
    ('demo-customer-13@mamxanh.local', N'Đinh Đức Anh', 'CUSTOMER'),
    ('demo-customer-14@mamxanh.local', N'Đoàn Mỹ Duyên', 'CUSTOMER'),
    ('demo-customer-15@mamxanh.local', N'Phạm Hoàng Long', 'CUSTOMER'),
    ('demo-customer-16@mamxanh.local', N'Trịnh Bảo Ngọc', 'CUSTOMER'),
    ('demo-customer-17@mamxanh.local', N'Lý Thành Đạt', 'CUSTOMER'),
    ('demo-customer-18@mamxanh.local', N'Nguyễn Hà My', 'CUSTOMER'),
    ('demo-customer-19@mamxanh.local', N'Trần Gia Bảo', 'CUSTOMER'),
    ('demo-customer-20@mamxanh.local', N'Phan Ngọc Trâm', 'CUSTOMER'),
    ('demo-expert-02@mamxanh.local', N'Võ Thanh Trúc', 'EXPERT'),
    ('demo-expert-03@mamxanh.local', N'Nguyễn Hoàng Phúc', 'EXPERT'),
    ('demo-expert-04@mamxanh.local', N'Lê Bảo Châu', 'EXPERT')
) seed(email, display_name, role)
WHERE NOT EXISTS (SELECT 1 FROM [USER] existing WHERE existing.email = seed.email);

UPDATE [USER]
SET email_verified = 1, account_status = 'ACTIVE', updated_at = SYSUTCDATETIME()
WHERE email IN ('demo-expert@mamxanh.local', 'demo-customer@mamxanh.local',
                'demo-new-member@mamxanh.local', 'demo-applicant@mamxanh.local',
                'demo-admin@mamxanh.local', 'demo-customer-04@mamxanh.local',
                'demo-customer-05@mamxanh.local', 'demo-customer-06@mamxanh.local',
                'demo-customer-07@mamxanh.local', 'demo-customer-08@mamxanh.local',
                'demo-customer-09@mamxanh.local', 'demo-customer-10@mamxanh.local',
                'demo-customer-11@mamxanh.local', 'demo-customer-12@mamxanh.local',
                'demo-customer-13@mamxanh.local', 'demo-customer-14@mamxanh.local',
                'demo-customer-15@mamxanh.local', 'demo-customer-16@mamxanh.local',
                'demo-customer-17@mamxanh.local', 'demo-customer-18@mamxanh.local',
                'demo-customer-19@mamxanh.local', 'demo-customer-20@mamxanh.local',
                'demo-expert-02@mamxanh.local', 'demo-expert-03@mamxanh.local',
                'demo-expert-04@mamxanh.local');

-- Keep the local Expert eligible to demonstrate FR-21; this fixture is never loaded outside profile local.
IF NOT EXISTS (
    SELECT 1 FROM [SUBSCRIPTION]
    WHERE user_id = @author_id AND tier = 'PRO' AND status = 'ACTIVE'
      AND starts_at <= SYSUTCDATETIME() AND ends_at > SYSUTCDATETIME()
)
BEGIN
    DECLARE @demo_subscription_start DATETIME2(7) = SYSUTCDATETIME();
    UPDATE [SUBSCRIPTION]
    SET tier = 'PRO', starts_at = @demo_subscription_start,
        ends_at = DATEADD(YEAR, 1, @demo_subscription_start), updated_at = @demo_subscription_start
    WHERE user_id = @author_id AND status = 'ACTIVE';

    IF @@ROWCOUNT = 0
        INSERT INTO [SUBSCRIPTION] (user_id, tier, status, starts_at, ends_at)
        VALUES (@author_id, 'PRO', 'ACTIVE', @demo_subscription_start,
                DATEADD(YEAR, 1, @demo_subscription_start));
END;

DECLARE @customer_id BIGINT;
DECLARE @sample_week_start DATE = CONVERT(DATE, SYSUTCDATETIME());
DECLARE @sample_meal_plan_id BIGINT;
SELECT @customer_id = user_id FROM [USER] WHERE email = 'demo-customer@mamxanh.local';
UPDATE [USER]
SET vegetarian_type = 'VEGAN', cuisine_preference = N'Món Việt', preferred_difficulty = 'EASY',
    max_cooking_time_min = 45, date_of_birth = DATEADD(YEAR, -25, CONVERT(DATE, GETDATE())),
    biological_sex = 'FEMALE', height_cm = 160, weight_kg = 55,
    activity_level = 'MODERATELY_ACTIVE', nutrition_goal = 'IMPROVE_HEALTH',
    onboarding_status = 'COMPLETED', avoid_none_confirmed = 0, dislike_none_confirmed = 1,
    health_data_consent = 1, health_data_consent_at = COALESCE(health_data_consent_at, SYSUTCDATETIME()),
    updated_at = SYSUTCDATETIME()
WHERE user_id = @customer_id;
SET @sample_week_start = DATEADD(DAY,
    -(DATEDIFF(DAY, CONVERT(DATE, '19000101', 112), @sample_week_start) % 7), @sample_week_start);

IF NOT EXISTS (SELECT 1 FROM [MEAL_PLAN] WHERE user_id = @customer_id AND week_start_date = @sample_week_start)
    INSERT INTO [MEAL_PLAN] (user_id, week_start_date) VALUES (@customer_id, @sample_week_start);

SELECT @sample_meal_plan_id = meal_plan_id FROM [MEAL_PLAN]
WHERE user_id = @customer_id AND week_start_date = @sample_week_start;

IF NOT EXISTS (
    SELECT 1 FROM [MEAL_PLAN_ENTRY]
    WHERE meal_plan_id = @sample_meal_plan_id AND recipe_id = @tofu_recipe
        AND meal_date = DATEADD(DAY, 1, @sample_week_start) AND meal_type = 'LUNCH'
)
    INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id, recipe_id, meal_date, meal_type, planned_servings)
    VALUES (@sample_meal_plan_id, @tofu_recipe, DATEADD(DAY, 1, @sample_week_start), 'LUNCH', 2);

COMMIT TRANSACTION;
