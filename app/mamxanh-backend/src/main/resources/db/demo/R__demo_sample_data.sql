-- Repeatable demo fixtures for local development only. Password hashes are
-- initialized at runtime from MAMXANH_DEMO_PASSWORD and are never stored here.
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
    VALUES ('demo-customer@mamxanh.local', NULL, N'Khách hàng mẫu', 'CUSTOMER', 0);
END;

IF NOT EXISTS (SELECT 1 FROM [USER] WHERE email = 'demo-new-member@mamxanh.local')
    INSERT INTO [USER] (email, display_name, role, email_verified)
    VALUES ('demo-new-member@mamxanh.local', N'Thành viên mới', 'CUSTOMER', 1);
IF NOT EXISTS (SELECT 1 FROM [USER] WHERE email = 'demo-applicant@mamxanh.local')
    INSERT INTO [USER] (email, display_name, role, email_verified)
    VALUES ('demo-applicant@mamxanh.local', N'Ứng viên Chuyên gia', 'CUSTOMER', 1);
IF NOT EXISTS (SELECT 1 FROM [USER] WHERE email = 'demo-admin@mamxanh.local')
    INSERT INTO [USER] (email, display_name, role, email_verified)
    VALUES ('demo-admin@mamxanh.local', N'Quản trị viên mẫu', 'ADMIN', 1);

UPDATE [USER]
SET email_verified = 1, account_status = 'ACTIVE', updated_at = SYSUTCDATETIME()
WHERE email IN ('demo-expert@mamxanh.local', 'demo-customer@mamxanh.local',
                'demo-new-member@mamxanh.local', 'demo-applicant@mamxanh.local',
                'demo-admin@mamxanh.local');

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

-- Additional ingredient choices for the real recipe composer and admin catalog.
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

DECLARE @mushroom BIGINT, @shiitake BIGINT, @carrot BIGINT, @sweet_potato BIGINT, @broccoli BIGINT, @lentil BIGINT;
SELECT @mushroom = ingredient_id FROM [INGREDIENT] WHERE name = N'Nấm đùi gà';
SELECT @shiitake = ingredient_id FROM [INGREDIENT] WHERE name = N'Nấm hương';
SELECT @carrot = ingredient_id FROM [INGREDIENT] WHERE name = N'Cà rốt';
SELECT @sweet_potato = ingredient_id FROM [INGREDIENT] WHERE name = N'Khoai lang';
SELECT @broccoli = ingredient_id FROM [INGREDIENT] WHERE name = N'Bông cải xanh';
SELECT @lentil = ingredient_id FROM [INGREDIENT] WHERE name = N'Đậu lăng';

DECLARE @recipe_seed TABLE (title NVARCHAR(120), description NVARCHAR(2000), instructions NVARCHAR(MAX), category VARCHAR(20), difficulty VARCHAR(10), prep INT, cook INT);
INSERT INTO @recipe_seed VALUES
 (N'Nấm đùi gà kho tiêu', N'Nấm kho đậm vị, dùng cùng cơm nóng.', N'Cắt nấm thành lát. Phi thơm tiêu và gia vị, cho nấm vào kho nhỏ lửa đến khi thấm.', 'BRAISED', 'EASY', 10, 20),
 (N'Canh nấm hương cà rốt', N'Canh rau củ thanh nhẹ cho bữa cơm gia đình.', N'Ngâm nấm hương và cắt cà rốt. Đun mềm rau củ trong nước dùng rau, nêm vừa ăn.', 'SOUP', 'EASY', 10, 20),
 (N'Khoai lang hấp', N'Món phụ đơn giản từ khoai lang.', N'Rửa sạch khoai lang, cắt khúc vừa ăn và hấp đến khi mềm.', 'STEAMED', 'EASY', 5, 20),
 (N'Bông cải xanh xào nấm', N'Rau xanh xào nhanh cùng nấm.', N'Rửa sạch bông cải và nấm. Xào nấm trước, thêm bông cải và đảo đến khi vừa chín.', 'STIR_FRY', 'EASY', 10, 10),
 (N'Súp đậu lăng rau củ', N'Súp đậu lăng ấm bụng với rau củ.', N'Rửa đậu lăng. Nấu cùng cà rốt và gia vị đến khi đậu mềm, khuấy đều trước khi dùng.', 'SOUP', 'MEDIUM', 10, 35);

INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category, vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, published_at)
SELECT @author_id, s.title, s.description, s.instructions, s.category, 'VEGAN', s.difficulty, 2, s.prep, s.cook, SYSUTCDATETIME()
FROM @recipe_seed s
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_POST] p WHERE p.author_id = @author_id AND p.title = s.title);

DECLARE @recipe_ingredients TABLE (title NVARCHAR(120), ingredient_id BIGINT, unit_id INT, quantity DECIMAL(10,2));
INSERT INTO @recipe_ingredients VALUES
 (N'Nấm đùi gà kho tiêu', @mushroom, @gram, 250),
 (N'Canh nấm hương cà rốt', @shiitake, @gram, 30), (N'Canh nấm hương cà rốt', @carrot, @gram, 100),
 (N'Khoai lang hấp', @sweet_potato, @gram, 300),
 (N'Bông cải xanh xào nấm', @broccoli, @gram, 200), (N'Bông cải xanh xào nấm', @mushroom, @gram, 100),
 (N'Súp đậu lăng rau củ', @lentil, @gram, 150), (N'Súp đậu lăng rau củ', @carrot, @gram, 80);
INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT p.recipe_id, i.ingredient_id, i.unit_id, i.quantity
FROM @recipe_ingredients i JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = i.title
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri WHERE ri.recipe_id = p.recipe_id);

IF NOT EXISTS (SELECT 1 FROM [USER_INGREDIENT_PREFERENCE] WHERE user_id = @customer_id AND ingredient_id = @pepper AND preference_type = 'AVOID')
    INSERT INTO [USER_INGREDIENT_PREFERENCE] (user_id, ingredient_id, preference_type) VALUES (@customer_id, @pepper, 'AVOID');

IF NOT EXISTS (SELECT 1 FROM [EXPERT_APPLICATION] WHERE user_id = @customer_id)
    INSERT INTO [EXPERT_APPLICATION] (user_id, bio_experience, vegetarian_type, sample_recipe_summary)
    VALUES (@customer_id, N'Nấu món chay tại nhà nhiều năm và muốn chia sẻ kinh nghiệm.', 'VEGAN', N'Đậu hũ sốt cà chua và các món rau củ theo mùa.');

-- Seven-day plan with breakfast, lunch and dinner examples, refreshed idempotently.
DECLARE @plan_recipes TABLE (day_offset INT, meal_type VARCHAR(10), title NVARCHAR(120));
INSERT INTO @plan_recipes VALUES
 (0,'BREAKFAST',N'Khoai lang hấp'), (0,'LUNCH',N'Đậu hũ sốt cà chua'), (0,'DINNER',N'Canh nấm hương cà rốt'),
 (1,'BREAKFAST',N'Khoai lang hấp'), (1,'LUNCH',N'Rau muống xào tỏi'), (1,'DINNER',N'Nấm đùi gà kho tiêu'),
 (2,'BREAKFAST',N'Canh nấm hương cà rốt'), (2,'LUNCH',N'Bông cải xanh xào nấm'), (2,'DINNER',N'Súp đậu lăng rau củ'),
 (3,'BREAKFAST',N'Khoai lang hấp'), (3,'LUNCH',N'Đậu hũ sốt cà chua'), (3,'DINNER',N'Bông cải xanh xào nấm'),
 (4,'BREAKFAST',N'Súp đậu lăng rau củ'), (4,'LUNCH',N'Nấm đùi gà kho tiêu'), (4,'DINNER',N'Canh nấm hương cà rốt'),
 (5,'BREAKFAST',N'Khoai lang hấp'), (5,'LUNCH',N'Rau muống xào tỏi'), (5,'DINNER',N'Súp đậu lăng rau củ'),
 (6,'BREAKFAST',N'Canh nấm hương cà rốt'), (6,'LUNCH',N'Bông cải xanh xào nấm'), (6,'DINNER',N'Đậu hũ sốt cà chua');
INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id, recipe_id, meal_date, meal_type, planned_servings)
SELECT @sample_meal_plan_id, p.recipe_id, DATEADD(DAY, seed.day_offset, @sample_week_start), seed.meal_type, 2
FROM @plan_recipes seed JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = seed.title
WHERE NOT EXISTS (SELECT 1 FROM [MEAL_PLAN_ENTRY] e WHERE e.meal_plan_id = @sample_meal_plan_id
    AND e.recipe_id = p.recipe_id AND e.meal_date = DATEADD(DAY, seed.day_offset, @sample_week_start) AND e.meal_type = seed.meal_type);

COMMIT TRANSACTION;
