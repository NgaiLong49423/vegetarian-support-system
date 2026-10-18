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

-- Local Expert portraits are served by the Frontend; preserve any avatar uploaded to storage.
UPDATE target
SET avatar_url = seed.avatar_url, updated_at = SYSUTCDATETIME()
FROM [USER] target
INNER JOIN (VALUES
    ('demo-expert@mamxanh.local', '/avatars/experts/nguyen-tue-tam.jpg'),
    ('demo-expert-02@mamxanh.local', '/avatars/experts/vo-thanh-truc.jpg'),
    ('demo-expert-03@mamxanh.local', '/avatars/experts/nguyen-hoang-phuc.jpg'),
    ('demo-expert-04@mamxanh.local', '/avatars/experts/le-bao-chau.jpg')
) seed(email, avatar_url) ON seed.email = target.email
WHERE (target.avatar_url IS NULL OR target.avatar_url LIKE '/avatars/experts/%')
  AND (target.avatar_url IS NULL OR target.avatar_url <> seed.avatar_url);

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

-- Local-only cover images use the Frontend's public demo assets so the recipe
-- cards can render without depending on a developer-specific Azure account.
DECLARE @recipe_media_seed TABLE (title NVARCHAR(120), blob_url VARCHAR(2048));
INSERT INTO @recipe_media_seed VALUES
 (N'Đậu hũ sốt cà chua', '/assets/95427.png'),
 (N'Rau muống xào tỏi', '/assets/cddd4.png'),
 (N'Nấm đùi gà kho tiêu', '/assets/40c2c.png'),
 (N'Canh nấm hương cà rốt', '/assets/40053.png'),
 (N'Khoai lang hấp', '/assets/8ef48.png'),
 (N'Bông cải xanh xào nấm', '/assets/09723.png'),
 (N'Súp đậu lăng rau củ', '/assets/5a53c.png');

INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
SELECT p.recipe_id, s.blob_url, 'image/png', 1, 1
FROM @recipe_media_seed s
JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = s.title
WHERE NOT EXISTS (
    SELECT 1 FROM [RECIPE_MEDIA] rm WHERE rm.recipe_id = p.recipe_id
);

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
 (N'Mì udon'), (N'Riềng'), (N'Bột năng'), (N'Rau ngót'), (N'Cải thảo');

INSERT INTO [INGREDIENT] (name, source_name, reference_date)
SELECT seed.name, N'Dữ liệu demo nội bộ', CONVERT(DATE, SYSUTCDATETIME())
FROM @showcase_ingredients seed
WHERE NOT EXISTS (SELECT 1 FROM [INGREDIENT] i WHERE i.name = seed.name);

-- Values are per 100 g. USDA-backed rows use their FoodData Central IDs;
-- rows marked DEMO are illustrative and must not be treated as nutrition guidance.
DECLARE @demo_nutrition TABLE (
    ingredient_name NVARCHAR(200), energy_kcal DECIMAL(10,2), protein_g DECIMAL(10,2),
    carbohydrate_g DECIMAL(10,2), total_fat_g DECIMAL(10,2), fiber_g DECIMAL(10,2),
    calcium_mg DECIMAL(10,2), iron_mg DECIMAL(10,2), vitamin_b12_mcg DECIMAL(10,4),
    zinc_mg DECIMAL(10,2), fdc_id BIGINT, source_type VARCHAR(10)
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
 (N'Rau muống', 19, 2.6, 3.1, 0.2, 2.1, 77, 1.7, 0, 0.4, NULL, 'DEMO'),
 (N'Nấm đùi gà', 35, 3.3, 6.2, 0.4, 2.3, 3, 1.3, 0, 0.8, NULL, 'DEMO'),
 (N'Nấm hương', 34, 2.2, 6.8, 0.5, 2.5, 2, 0.4, 0, 1.0, NULL, 'DEMO'),
 (N'Mì udon', 127, 3.4, 25.2, 0.6, 1.0, 10, 0.6, 0, 0.5, NULL, 'DEMO'),
 (N'Riềng', 71, 1.5, 15.0, 0.9, 2.0, 34, 0.9, 0, 0.3, NULL, 'DEMO'),
 (N'Bột năng', 358, 0.2, 88.7, 0.0, 0.9, 20, 1.6, 0, 0.1, NULL, 'DEMO'),
 (N'Rau ngót', 35, 5.0, 6.0, 0.4, 2.5, 230, 3.0, 0, 1.0, NULL, 'DEMO'),
 (N'Cải thảo', 16, 1.2, 3.2, 0.2, 1.2, 40, 0.5, 0, 0.2, NULL, 'DEMO');

UPDATE i
SET energy_kcal_100g = seed.energy_kcal, protein_g_100g = seed.protein_g,
    carbohydrate_g_100g = seed.carbohydrate_g, total_fat_g_100g = seed.total_fat_g,
    fiber_g_100g = seed.fiber_g, calcium_mg_100g = seed.calcium_mg,
    iron_mg_100g = seed.iron_mg, vitamin_b12_mcg_100g = seed.vitamin_b12_mcg,
    zinc_mg_100g = seed.zinc_mg,
    source_name = CASE WHEN seed.source_type = 'DEMO' THEN N'Dữ liệu demo nội bộ' ELSE N'USDA FoodData Central' END,
    source_url = CASE WHEN seed.source_type = 'DEMO'
        THEN 'https://example.com/demo-nutrition-data'
        ELSE CONCAT('https://fdc.nal.usda.gov/food-details/', seed.fdc_id, '/nutrients') END,
    reference_date = '2026-10-13', nutrition_supported = 1, updated_at = SYSUTCDATETIME()
FROM [INGREDIENT] i
JOIN @demo_nutrition seed ON seed.ingredient_name = i.name
WHERE i.source_name = N'Dữ liệu demo nội bộ';

DECLARE @showcase_recipes TABLE (
    title NVARCHAR(120), description NVARCHAR(2000), instructions NVARCHAR(MAX),
    category VARCHAR(20), difficulty VARCHAR(10), prep INT, cook INT, image_url VARCHAR(2048));
INSERT INTO @showcase_recipes VALUES
 (N'Mì udon xào rau củ kiểu Nhật', N'Mì udon dai mềm xào nấm, cải thìa và đậu Hà Lan trong sốt tương gừng.', N'Luộc mì udon rồi để ráo. Xào thơm gừng, cho nấm và rau củ vào đảo nhanh. Thêm mì cùng nước tương, đảo đều trên lửa lớn rồi rắc hành lá.', 'STIR_FRY', 'EASY', 15, 12, 'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=1200'),
 (N'Cơm chiên dứa đậu phộng', N'Cơm chiên vàng thơm với dứa chua ngọt, đậu phộng rang và rau củ.', N'Xào hành tím, thêm đậu Hà Lan và ngô hạt. Cho cơm nguội vào đảo tơi, nêm nước tương. Trộn dứa và đậu phộng rang ở cuối để giữ độ giòn.', 'STIR_FRY', 'EASY', 15, 15, 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200'),
 (N'Phở cuốn nấm bào ngư', N'Bánh phở cuốn mềm ôm nấm bào ngư áp chảo và rau sống tươi mát.', N'Xé nấm bào ngư, áp chảo với nước tương và gừng. Trải bánh phở, thêm cải thìa, giá đỗ và nấm rồi cuốn chắc tay. Dùng với nước chấm chanh ớt.', 'ROLL', 'MEDIUM', 20, 12, 'https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=1200'),
 (N'Bánh xèo nấm và giá đỗ', N'Bánh xèo vỏ mỏng giòn, nhân nấm, giá đỗ và cải bắp.', N'Pha bột gạo với nước và bột nghệ. Xào thơm nấm, đổ bột vào chảo nóng rồi thêm giá đỗ và bắp cải. Đậy nắp đến khi viền bánh giòn rồi gập đôi.', 'FRIED', 'MEDIUM', 20, 20, 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=1200'),
 (N'Cháo bí đỏ đậu xanh', N'Cháo bí đỏ sánh mịn nấu cùng đậu xanh bùi, thơm nhẹ gừng.', N'Nấu đậu xanh cùng gạo đến khi mềm. Thêm bí đỏ cắt khối và gừng, nấu nhỏ lửa đến khi bí mềm. Khuấy đều, nêm vừa ăn và rắc hành lá.', 'SOUP', 'EASY', 10, 35, 'https://images.unsplash.com/photo-1511690743698-d9d85f2fbf38?w=1200'),
 (N'Đậu hũ sốt nấm mỡ', N'Đậu hũ mềm phủ sốt nấm mỡ sánh nhẹ và hành lá.', N'Xào thơm hành tím, cho nấm và nước tương vào. Hòa bột năng với nước lạnh rồi khuấy vào sốt cho sánh. Áp chảo đậu hũ, rưới sốt nấm và dùng nóng.', 'BRAISED', 'EASY', 12, 12, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=1200'),
 (N'Bánh mì nấm áp chảo', N'Bánh mì nguyên cám kẹp nấm nướng, cà tím và rau xanh.', N'Ướp nấm và cà tím với nước tương, tiêu và ít dầu. Áp chảo đến khi vàng thơm. Nướng nóng bánh mì, kẹp cùng cải thìa và vắt chút chanh trước khi ăn.', 'GRILLED', 'EASY', 12, 12, 'https://images.unsplash.com/photo-1547592180-85f173990554?w=1200'),
 (N'Bún chả giò chay rau thơm', N'Tô bún tươi ăn cùng chả giò chay giòn và rau xanh mát.', N'Chiên hoặc nướng chả giò đến khi vàng giòn. Xếp bánh phở, cải thìa, giá đỗ và rau thơm vào tô. Thêm chả giò cắt khúc, rắc đậu phộng và chan nước chấm chanh.', 'NOODLE_SOUP', 'MEDIUM', 20, 15, 'https://images.unsplash.com/photo-1555126634-323283e090fa?w=1200'),
 (N'Lẩu nấm cải thảo', N'Nồi lẩu thanh ngọt với nấm kim châm, nấm bào ngư và cải thảo.', N'Đun sôi nước dùng rau củ với gừng và riềng. Nêm vừa ăn, lần lượt cho cải thảo, nấm và đậu hũ vào nồi. Dùng nóng cùng bánh phở.', 'HOT_POT', 'EASY', 15, 25, 'https://images.unsplash.com/photo-1570368336224-455084c1fb31?w=1200'),
 (N'Chả giò khoai môn đậu xanh', N'Chả giò vàng giòn với nhân khoai môn, đậu xanh và nấm.', N'Ngâm mềm đậu xanh và nấm. Trộn với khoai môn bào và bắp cải thái sợi. Cuốn nhân trong bánh tráng, áp chảo hoặc chiên ít dầu đến khi vàng đều.', 'FRIED', 'MEDIUM', 25, 20, 'https://images.unsplash.com/photo-1589442305595-62647c1514f9?w=1200'),
 (N'Cơm nắm rong biển mè trắng', N'Cơm nắm nhỏ gọn trộn rong biển, mè trắng và đậu Hà Lan.', N'Nấu chín đậu Hà Lan. Trộn cơm nóng với rong biển vụn, mè rang và chút nước tương. Nắm thành viên nhỏ khi cơm còn ấm, dùng kèm dưa leo.', 'STEAMED', 'EASY', 15, 20, 'https://images.unsplash.com/photo-1651629993507-d20a811a746d?w=1200'),
 (N'Canh chua nấm đậu bắp', N'Canh chua thanh nhẹ nấu nấm, đậu bắp và me chua.', N'Đun nước với me chua rồi lọc bỏ hạt. Thêm cà chua, nấm và đậu bắp, nấu vừa chín tới. Nêm cân vị, rắc ngò rí trước khi dùng.', 'SOUP', 'EASY', 12, 18, 'https://images.unsplash.com/photo-1675092789086-4bd2b93ffc69?w=1200'),
 (N'Mì Ý sốt cà tím nấm', N'Mì Ý phủ sốt cà tím, nấm và cà chua nấu thơm.', N'Luộc mì Ý đến độ dai vừa. Áp chảo cà tím và nấm, thêm cà chua cùng ít nước luộc mì rồi nấu thành sốt. Trộn mì vào chảo, nêm tiêu và nước tương.', 'STIR_FRY', 'MEDIUM', 15, 20, 'https://images.unsplash.com/photo-1758293121435-396ed31ebcf4?w=1200'),
 (N'Pudding hạt chia xoài', N'Món tráng miệng mát lạnh với hạt chia, xoài chín và sữa đậu nành.', N'Khuấy hạt chia vào sữa đậu nành, để lạnh ít nhất 3 giờ cho nở. Cắt xoài thành hạt lựu, xếp lên pudding trước khi dùng.', 'DESSERT', 'EASY', 10, 0, 'https://images.unsplash.com/photo-1505576633757-0ac1084af824?w=1200'),
 (N'Rau ngót nấu nấm kim châm', N'Canh rau ngót xanh mềm nấu cùng nấm kim châm và đậu hũ.', N'Vò nhẹ rau ngót, cắt bỏ gốc nấm. Đun nước sôi, cho đậu hũ vào trước, thêm rau ngót và nấm. Nấu vừa chín, nêm nước tương và tiêu.', 'SOUP', 'EASY', 10, 12, 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200'),
 (N'Bí đỏ nướng mè trắng', N'Bí đỏ nướng mềm ngọt phủ mè trắng rang và sốt chanh gừng.', N'Cắt bí đỏ thành miếng vừa ăn, trộn với gừng băm và chút dầu. Nướng đến khi mềm cạnh vàng. Rắc mè rang, thêm nước cốt chanh và dùng nóng.', 'GRILLED', 'EASY', 10, 25, 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=1200');

INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category, vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, published_at)
SELECT @author_id, r.title, r.description, r.instructions, r.category, 'VEGAN', r.difficulty, 2, r.prep, r.cook, SYSUTCDATETIME()
FROM @showcase_recipes r
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_POST] p WHERE p.author_id = @author_id AND p.title = r.title);

DECLARE @showcase_recipe_ingredients TABLE (title NVARCHAR(120), ingredient_name NVARCHAR(200), quantity DECIMAL(10,2));
INSERT INTO @showcase_recipe_ingredients VALUES
 (N'Mì udon xào rau củ kiểu Nhật', N'Mì udon', 200), (N'Mì udon xào rau củ kiểu Nhật', N'Nấm kim châm', 100), (N'Mì udon xào rau củ kiểu Nhật', N'Rau cải thìa', 120), (N'Mì udon xào rau củ kiểu Nhật', N'Đậu Hà Lan', 60),
 (N'Cơm chiên dứa đậu phộng', N'Dứa', 120), (N'Cơm chiên dứa đậu phộng', N'Đậu phộng', 30), (N'Cơm chiên dứa đậu phộng', N'Đậu Hà Lan', 60), (N'Cơm chiên dứa đậu phộng', N'Ngô hạt', 60),
 (N'Phở cuốn nấm bào ngư', N'Bánh phở', 200), (N'Phở cuốn nấm bào ngư', N'Nấm bào ngư', 150), (N'Phở cuốn nấm bào ngư', N'Giá đỗ', 80), (N'Phở cuốn nấm bào ngư', N'Rau cải thìa', 80),
 (N'Bánh xèo nấm và giá đỗ', N'Bột gạo', 150), (N'Bánh xèo nấm và giá đỗ', N'Bột nghệ', 3), (N'Bánh xèo nấm và giá đỗ', N'Nấm mỡ', 100), (N'Bánh xèo nấm và giá đỗ', N'Giá đỗ', 80),
 (N'Cháo bí đỏ đậu xanh', N'Bí đỏ', 200), (N'Cháo bí đỏ đậu xanh', N'Đậu xanh', 80), (N'Cháo bí đỏ đậu xanh', N'Gừng', 5), (N'Cháo bí đỏ đậu xanh', N'Hành lá', 10),
 (N'Đậu hũ sốt nấm mỡ', N'Đậu hũ', 200), (N'Đậu hũ sốt nấm mỡ', N'Nấm mỡ', 120), (N'Đậu hũ sốt nấm mỡ', N'Bột năng', 8), (N'Đậu hũ sốt nấm mỡ', N'Hành tím', 15),
 (N'Bánh mì nấm áp chảo', N'Bánh mì nguyên cám', 120), (N'Bánh mì nấm áp chảo', N'Nấm kim châm', 100), (N'Bánh mì nấm áp chảo', N'Cà tím', 100), (N'Bánh mì nấm áp chảo', N'Chanh', 10),
 (N'Bún chả giò chay rau thơm', N'Bánh phở', 200), (N'Bún chả giò chay rau thơm', N'Chả giò chay', 150), (N'Bún chả giò chay rau thơm', N'Rau cải thìa', 80), (N'Bún chả giò chay rau thơm', N'Đậu phộng', 20),
 (N'Lẩu nấm cải thảo', N'Nấm kim châm', 100), (N'Lẩu nấm cải thảo', N'Nấm bào ngư', 150), (N'Lẩu nấm cải thảo', N'Cải thảo', 150), (N'Lẩu nấm cải thảo', N'Riềng', 10),
 (N'Chả giò khoai môn đậu xanh', N'Khoai môn', 150), (N'Chả giò khoai môn đậu xanh', N'Đậu xanh', 60), (N'Chả giò khoai môn đậu xanh', N'Bắp cải', 80), (N'Chả giò khoai môn đậu xanh', N'Bột gạo', 30),
 (N'Cơm nắm rong biển mè trắng', N'Rong biển', 5), (N'Cơm nắm rong biển mè trắng', N'Mè trắng', 10), (N'Cơm nắm rong biển mè trắng', N'Đậu Hà Lan', 50), (N'Cơm nắm rong biển mè trắng', N'Nước tương', 10),
 (N'Canh chua nấm đậu bắp', N'Đậu bắp', 100), (N'Canh chua nấm đậu bắp', N'Me chua', 15), (N'Canh chua nấm đậu bắp', N'Nấm mỡ', 100), (N'Canh chua nấm đậu bắp', N'Ngò rí', 10),
 (N'Mì Ý sốt cà tím nấm', N'Mì Ý', 180), (N'Mì Ý sốt cà tím nấm', N'Cà tím', 120), (N'Mì Ý sốt cà tím nấm', N'Nấm mỡ', 100), (N'Mì Ý sốt cà tím nấm', N'Cà chua', 150),
 (N'Pudding hạt chia xoài', N'Hạt chia', 25), (N'Pudding hạt chia xoài', N'Xoài', 150), (N'Pudding hạt chia xoài', N'Sữa đậu nành', 200), (N'Pudding hạt chia xoài', N'Chanh', 5),
 (N'Rau ngót nấu nấm kim châm', N'Rau ngót', 150), (N'Rau ngót nấu nấm kim châm', N'Nấm kim châm', 100), (N'Rau ngót nấu nấm kim châm', N'Đậu hũ', 100), (N'Rau ngót nấu nấm kim châm', N'Nước tương', 10),
 (N'Bí đỏ nướng mè trắng', N'Bí đỏ', 250), (N'Bí đỏ nướng mè trắng', N'Mè trắng', 10), (N'Bí đỏ nướng mè trắng', N'Gừng', 5), (N'Bí đỏ nướng mè trắng', N'Chanh', 10);
INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT p.recipe_id, i.ingredient_id, @gram, seed.quantity
FROM @showcase_recipe_ingredients seed
JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = seed.title
JOIN [INGREDIENT] i ON i.name = seed.ingredient_name
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri WHERE ri.recipe_id = p.recipe_id);

-- Add a cover image to every pre-existing and newly added demo recipe.
INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
SELECT p.recipe_id, seed.image_url, 'image/jpeg', 1, 1
FROM (VALUES
    (N'Đậu hũ sốt cà chua', 'https://images.unsplash.com/photo-1589442305595-62647c1514f9?w=1200'),
    (N'Rau muống xào tỏi', 'https://images.unsplash.com/photo-1505576633757-0ac1084af824?w=1200'),
    (N'Nấm đùi gà kho tiêu', 'https://images.unsplash.com/photo-1555126634-323283e090fa?w=1200'),
    (N'Canh nấm hương cà rốt', 'https://images.unsplash.com/photo-1675092789086-4bd2b93ffc69?w=1200'),
    (N'Khoai lang hấp', 'https://images.unsplash.com/photo-1570368336224-455084c1fb31?w=1200'),
    (N'Bông cải xanh xào nấm', 'https://images.unsplash.com/photo-1651629993507-d20a811a746d?w=1200'),
    (N'Súp đậu lăng rau củ', 'https://images.unsplash.com/photo-1547592180-85f173990554?w=1200')
) seed(title, image_url)
JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = seed.title
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_MEDIA] m WHERE m.recipe_id = p.recipe_id);

-- 20 additional recipes and 30 unique catalogue ingredients for local demos.
DECLARE @extra_ingredients TABLE (name NVARCHAR(200) PRIMARY KEY);
INSERT INTO @extra_ingredients (name) VALUES
    (N'Gạo lứt'), (N'Yến mạch cán'), (N'Hạt quinoa'), (N'Hạt điều'), (N'Hạnh nhân'),
    (N'Óc chó'), (N'Hạt bí'), (N'Hạt hướng dương'), (N'Đậu nành hạt'), (N'Đậu đỏ'),
    (N'Đậu gà'), (N'Đậu trắng'), (N'Tempeh'), (N'Seitan'), (N'Tảo bẹ kombu'),
    (N'Nấm mộc nhĩ'), (N'Măng tây'), (N'Bí ngòi'), (N'Súp lơ trắng'), (N'Cải xoăn'),
    (N'Xà lách romaine'), (N'Dưa leo'), (N'Ớt chuông'), (N'Bơ'), (N'Lựu'),
    (N'Táo'), (N'Chuối'), (N'Dâu tây'), (N'Cam'), (N'Khoai tây');

INSERT INTO [INGREDIENT] (name, source_name, reference_date, nutrition_supported)
SELECT seed.name, N'Dữ liệu demo nội bộ - chưa xác minh dinh dưỡng', CONVERT(DATE, SYSUTCDATETIME()), 0
FROM @extra_ingredients seed
WHERE NOT EXISTS (SELECT 1 FROM [INGREDIENT] i WHERE i.name = seed.name);

DECLARE @extra_recipes TABLE (
    title NVARCHAR(120) PRIMARY KEY, description NVARCHAR(2000), instructions NVARCHAR(MAX),
    category VARCHAR(20), difficulty VARCHAR(10), prep INT, cook INT, image_url VARCHAR(2048));
INSERT INTO @extra_recipes VALUES
 (N'Cơm gạo lứt đậu gà rau củ',N'Cơm gạo lứt ăn cùng đậu gà và rau củ áp chảo.',N'Nấu chín gạo lứt và đậu gà đã ngâm mềm. Áp chảo ớt chuông cùng bí ngòi, nêm nước tương. Xếp cơm, đậu và rau củ ra đĩa, rắc hạt hướng dương.','STIR_FRY','EASY',15,30,'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200'),
 (N'Cháo yến mạch nấm mộc nhĩ',N'Cháo yến mạch nóng với nấm mộc nhĩ và cà rốt.',N'Ngâm nấm mộc nhĩ rồi thái sợi. Đun yến mạch với nước đến khi sánh, thêm nấm và cà rốt thái nhỏ. Nấu chín mềm, nêm vừa ăn và dùng nóng.','SOUP','EASY',10,15,'https://images.unsplash.com/photo-1511690743698-d9d85f2fbf38?w=1200'),
 (N'Salad quinoa bơ lựu',N'Salad quinoa trộn bơ, lựu và xà lách romaine.',N'Nấu chín quinoa rồi để nguội. Cắt bơ và dưa leo, tách hạt lựu. Trộn cùng xà lách romaine, nước cốt cam và chút muối ngay trước khi dùng.','SALAD','EASY',15,20,'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200'),
 (N'Đậu hũ áp chảo sốt hạt điều',N'Đậu hũ vàng nhẹ phủ sốt hạt điều béo mịn.',N'Ngâm hạt điều rồi xay với nước thành sốt. Áp chảo đậu hũ đến vàng các mặt, đun sốt với tỏi và nước tương. Rưới sốt lên đậu hũ và rắc hạnh nhân lát.','BRAISED','MEDIUM',15,15,'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=1200'),
 (N'Bún tempeh rau sống',N'Bánh phở ăn kèm tempeh áp chảo và rau sống tươi.',N'Cắt tempeh thành lát, ướp nước tương rồi áp chảo vàng. Xếp bánh phở, xà lách romaine, dưa leo và tempeh vào tô. Rắc đậu phộng và dùng cùng nước chấm chanh.','NOODLE_SOUP','EASY',15,12,'https://images.unsplash.com/photo-1555126634-323283e090fa?w=1200'),
 (N'Khoai tây nướng hạt bí',N'Khoai tây nướng thơm với hạt bí rang.',N'Cắt khoai tây thành miếng vừa, trộn với dầu và tiêu. Nướng đến khi mềm ruột và vàng cạnh. Rang hạt bí riêng rồi rắc lên khoai trước khi dùng.','GRILLED','EASY',10,30,'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=1200'),
 (N'Súp đậu trắng cải xoăn',N'Súp đậu trắng nấu cùng cải xoăn và cà chua.',N'Ngâm và nấu mềm đậu trắng. Thêm cà chua cùng nước dùng rau củ, nấu liu riu. Cho cải xoăn vào cuối, nấu vừa mềm rồi nêm gia vị.','SOUP','MEDIUM',15,35,'https://images.unsplash.com/photo-1547592180-85f173990554?w=1200'),
 (N'Bánh quinoa chuối dâu tây',N'Bánh áp chảo từ quinoa, chuối và dâu tây.',N'Nghiền chuối, trộn với quinoa đã nấu và yến mạch cán. Áp chảo từng phần nhỏ đến khi hai mặt vàng. Dùng cùng dâu tây cắt lát.','FRIED','MEDIUM',15,15,'https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=1200'),
 (N'Ớt chuông nhồi gạo lứt',N'Ớt chuông nướng với nhân gạo lứt và đậu đỏ.',N'Nấu chín gạo lứt và đậu đỏ. Trộn cùng cà chua thái hạt lựu, cho vào ớt chuông bỏ hạt. Nướng đến khi ớt mềm và phần nhân nóng đều.','GRILLED','MEDIUM',20,30,'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=1200'),
 (N'Mì seitan xào măng tây',N'Mì xào seitan và măng tây giòn nhẹ.',N'Luộc mì vừa chín. Cắt seitan và măng tây thành miếng, xào nhanh với tỏi. Thêm mì, nước tương và chút nước luộc mì, đảo đều rồi dùng nóng.','STIR_FRY','MEDIUM',15,15,'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=1200'),
 (N'Canh kombu đậu hũ',N'Canh thanh vị nấu từ kombu, đậu hũ và nấm.',N'Ngâm kombu trong nước lạnh rồi đun nhỏ lửa để lấy nước dùng, vớt kombu ra. Thêm nấm và đậu hũ, nấu sôi nhẹ, nêm nước tương trước khi dùng.','SOUP','EASY',10,20,'https://images.unsplash.com/photo-1675092789086-4bd2b93ffc69?w=1200'),
 (N'Bí ngòi cuộn hạt điều',N'Bí ngòi cuộn sốt hạt điều và cà chua.',N'Thái bí ngòi thành lát mỏng và áp chảo nhanh. Xay hạt điều đã ngâm cùng nước và tỏi thành sốt. Phết sốt lên lát bí, cuộn lại và dùng với cà chua.','ROLL','MEDIUM',20,10,'https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=1200'),
 (N'Súp lơ trắng nghiền hạnh nhân',N'Súp lơ trắng hấp mềm nghiền cùng hạnh nhân.',N'Hấp súp lơ trắng đến khi mềm. Xay hoặc nghiền cùng hạnh nhân ngâm và nước ấm đến độ mịn mong muốn. Nêm tiêu, dùng nóng với hạt bí rang.','STEAMED','EASY',10,15,'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200'),
 (N'Đậu đỏ hầm cà chua',N'Đậu đỏ hầm mềm với cà chua và khoai tây.',N'Ngâm đậu đỏ qua đêm rồi nấu mềm. Thêm khoai tây và cà chua cắt miếng, hầm đến khi rau củ chín. Nêm gia vị và rắc hành lá.','BRAISED','MEDIUM',15,45,'https://images.unsplash.com/photo-1547592180-85f173990554?w=1200'),
 (N'Yến mạch qua đêm táo',N'Yến mạch ngâm lạnh với táo và hạnh nhân.',N'Trộn yến mạch với sữa đậu nành, đậy kín và để lạnh qua đêm. Cắt táo nhỏ, rắc hạnh nhân lên trên ngay trước khi ăn.','DESSERT','EASY',10,5,'https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=1200'),
 (N'Gỏi romaine dưa leo cam',N'Gỏi rau giòn với dưa leo và cam tươi.',N'Rửa và xé xà lách romaine. Cắt dưa leo, tách múi cam rồi trộn nhẹ. Rắc hạt hướng dương và dùng ngay.', 'SALAD','EASY',15,5,'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200'),
 (N'Đậu nành hạt sốt cà tím',N'Đậu nành hạt nấu mềm trong sốt cà tím.',N'Ngâm và nấu mềm đậu nành hạt. Áp chảo cà tím với tỏi, thêm cà chua và đun thành sốt. Trộn đậu vào, nấu thêm vài phút rồi nêm vừa ăn.','BRAISED','MEDIUM',15,35,'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=1200'),
 (N'Bánh khoai lang hạt óc chó',N'Bánh khoai lang nghiền trộn óc chó và yến mạch.',N'Hấp khoai lang rồi nghiền nhuyễn. Trộn với yến mạch và óc chó băm, tạo miếng dẹt. Áp chảo lửa nhỏ đến khi hai mặt vàng và bánh nóng đều.','FRIED','MEDIUM',15,20,'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=1200'),
 (N'Đậu gà nghiền bánh mì',N'Bánh mì nguyên cám kẹp đậu gà nghiền và rau xanh.',N'Nghiền đậu gà đã nấu chín với nước cốt chanh và tiêu. Phết lên bánh mì, thêm xà lách romaine, dưa leo và cà chua. Dùng ngay sau khi kẹp.','ROLL','EASY',15,5,'https://images.unsplash.com/photo-1547592180-85f173990554?w=1200'),
 (N'Salad trái cây hạt chia',N'Trái cây tươi trộn hạt chia và hạnh nhân.',N'Cắt chuối, táo, dâu tây và cam thành miếng vừa ăn. Trộn nhẹ với hạt chia đã ngâm nở, rắc hạnh nhân lát và dùng lạnh.','DESSERT','EASY',15,5,'https://images.unsplash.com/photo-1505576633757-0ac1084af824?w=1200');

INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category, vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, published_at)
SELECT @author_id, seed.title, seed.description, seed.instructions, seed.category, 'VEGAN', seed.difficulty, 2, seed.prep, seed.cook, SYSUTCDATETIME()
FROM @extra_recipes seed
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_POST] p WHERE p.author_id = @author_id AND p.title = seed.title);

DECLARE @extra_recipe_ingredients TABLE (title NVARCHAR(120), ingredient_name NVARCHAR(200), quantity DECIMAL(10,2));
INSERT INTO @extra_recipe_ingredients VALUES
 (N'Cơm gạo lứt đậu gà rau củ',N'Gạo lứt',180),(N'Cơm gạo lứt đậu gà rau củ',N'Đậu gà',100),(N'Cơm gạo lứt đậu gà rau củ',N'Ớt chuông',80),(N'Cơm gạo lứt đậu gà rau củ',N'Bí ngòi',80),(N'Cơm gạo lứt đậu gà rau củ',N'Hạt hướng dương',10),
 (N'Cháo yến mạch nấm mộc nhĩ',N'Yến mạch cán',80),(N'Cháo yến mạch nấm mộc nhĩ',N'Nấm mộc nhĩ',25),(N'Cháo yến mạch nấm mộc nhĩ',N'Cà rốt',60),
 (N'Salad quinoa bơ lựu',N'Hạt quinoa',100),(N'Salad quinoa bơ lựu',N'Bơ',80),(N'Salad quinoa bơ lựu',N'Lựu',50),(N'Salad quinoa bơ lựu',N'Xà lách romaine',80),(N'Salad quinoa bơ lựu',N'Dưa leo',50),(N'Salad quinoa bơ lựu',N'Cam',20),
 (N'Đậu hũ áp chảo sốt hạt điều',N'Đậu hũ',200),(N'Đậu hũ áp chảo sốt hạt điều',N'Hạt điều',35),(N'Đậu hũ áp chảo sốt hạt điều',N'Hạnh nhân',10),
 (N'Bún tempeh rau sống',N'Tempeh',150),(N'Bún tempeh rau sống',N'Bánh phở',150),(N'Bún tempeh rau sống',N'Dưa leo',80),(N'Bún tempeh rau sống',N'Xà lách romaine',70),(N'Bún tempeh rau sống',N'Đậu phộng',15),
 (N'Khoai tây nướng hạt bí',N'Khoai tây',250),(N'Khoai tây nướng hạt bí',N'Hạt bí',15),
 (N'Súp đậu trắng cải xoăn',N'Đậu trắng',100),(N'Súp đậu trắng cải xoăn',N'Cải xoăn',80),(N'Súp đậu trắng cải xoăn',N'Cà chua',100),
 (N'Bánh quinoa chuối dâu tây',N'Hạt quinoa',80),(N'Bánh quinoa chuối dâu tây',N'Chuối',100),(N'Bánh quinoa chuối dâu tây',N'Dâu tây',80),(N'Bánh quinoa chuối dâu tây',N'Yến mạch cán',40),
 (N'Ớt chuông nhồi gạo lứt',N'Ớt chuông',160),(N'Ớt chuông nhồi gạo lứt',N'Gạo lứt',100),(N'Ớt chuông nhồi gạo lứt',N'Đậu đỏ',80),(N'Ớt chuông nhồi gạo lứt',N'Cà chua',60),
 (N'Mì seitan xào măng tây',N'Seitan',120),(N'Mì seitan xào măng tây',N'Măng tây',100),(N'Mì seitan xào măng tây',N'Mì Ý',120),
 (N'Canh kombu đậu hũ',N'Tảo bẹ kombu',5),(N'Canh kombu đậu hũ',N'Đậu hũ',120),(N'Canh kombu đậu hũ',N'Nấm hương',20),
 (N'Bí ngòi cuộn hạt điều',N'Bí ngòi',160),(N'Bí ngòi cuộn hạt điều',N'Hạt điều',30),(N'Bí ngòi cuộn hạt điều',N'Cà chua',80),
 (N'Súp lơ trắng nghiền hạnh nhân',N'Súp lơ trắng',200),(N'Súp lơ trắng nghiền hạnh nhân',N'Hạnh nhân',20),(N'Súp lơ trắng nghiền hạnh nhân',N'Hạt bí',10),
 (N'Đậu đỏ hầm cà chua',N'Đậu đỏ',100),(N'Đậu đỏ hầm cà chua',N'Khoai tây',100),(N'Đậu đỏ hầm cà chua',N'Cà chua',120),
 (N'Yến mạch qua đêm táo',N'Yến mạch cán',60),(N'Yến mạch qua đêm táo',N'Táo',100),(N'Yến mạch qua đêm táo',N'Hạnh nhân',10),(N'Yến mạch qua đêm táo',N'Sữa đậu nành',150),
 (N'Gỏi romaine dưa leo cam',N'Xà lách romaine',100),(N'Gỏi romaine dưa leo cam',N'Dưa leo',80),(N'Gỏi romaine dưa leo cam',N'Cam',80),(N'Gỏi romaine dưa leo cam',N'Hạt hướng dương',10),
 (N'Đậu nành hạt sốt cà tím',N'Đậu nành hạt',100),(N'Đậu nành hạt sốt cà tím',N'Cà tím',120),(N'Đậu nành hạt sốt cà tím',N'Cà chua',100),
 (N'Bánh khoai lang hạt óc chó',N'Khoai lang',180),(N'Bánh khoai lang hạt óc chó',N'Óc chó',20),(N'Bánh khoai lang hạt óc chó',N'Yến mạch cán',30),
 (N'Đậu gà nghiền bánh mì',N'Đậu gà',100),(N'Đậu gà nghiền bánh mì',N'Bánh mì nguyên cám',100),(N'Đậu gà nghiền bánh mì',N'Xà lách romaine',50),(N'Đậu gà nghiền bánh mì',N'Dưa leo',50),(N'Đậu gà nghiền bánh mì',N'Cà chua',30),
 (N'Salad trái cây hạt chia',N'Chuối',60),(N'Salad trái cây hạt chia',N'Táo',60),(N'Salad trái cây hạt chia',N'Dâu tây',60),(N'Salad trái cây hạt chia',N'Cam',60),(N'Salad trái cây hạt chia',N'Hạt chia',10),(N'Salad trái cây hạt chia',N'Hạnh nhân',10);

INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT p.recipe_id, i.ingredient_id, @gram, seed.quantity
FROM @extra_recipe_ingredients seed
JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = seed.title
JOIN [INGREDIENT] i ON i.name = seed.ingredient_name
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri WHERE ri.recipe_id = p.recipe_id AND ri.ingredient_id = i.ingredient_id);

INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
SELECT p.recipe_id, seed.image_url, 'image/jpeg', 1, 1
FROM @extra_recipes seed JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = seed.title
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_MEDIA] m WHERE m.recipe_id = p.recipe_id);
INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
SELECT p.recipe_id, seed.image_url, 'image/jpeg', 1, 1
FROM @showcase_recipes seed
JOIN [RECIPE_POST] p ON p.author_id = @author_id AND p.title = seed.title
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_MEDIA] m WHERE m.recipe_id = p.recipe_id);

DECLARE @demo_customers TABLE (user_id BIGINT PRIMARY KEY, ordinal INT);
INSERT INTO @demo_customers (user_id, ordinal)
SELECT user_id, CONVERT(INT, ROW_NUMBER() OVER (ORDER BY email))
FROM [USER] WHERE role = 'CUSTOMER' AND email IN (
    'demo-customer@mamxanh.local','demo-new-member@mamxanh.local','demo-applicant@mamxanh.local',
    'demo-customer-04@mamxanh.local','demo-customer-05@mamxanh.local','demo-customer-06@mamxanh.local',
    'demo-customer-07@mamxanh.local','demo-customer-08@mamxanh.local','demo-customer-09@mamxanh.local',
    'demo-customer-10@mamxanh.local','demo-customer-11@mamxanh.local','demo-customer-12@mamxanh.local',
    'demo-customer-13@mamxanh.local','demo-customer-14@mamxanh.local','demo-customer-15@mamxanh.local',
    'demo-customer-16@mamxanh.local','demo-customer-17@mamxanh.local','demo-customer-18@mamxanh.local',
    'demo-customer-19@mamxanh.local','demo-customer-20@mamxanh.local');

UPDATE u SET vegetarian_type = CASE WHEN c.ordinal % 4 = 0 THEN 'LACTO_OVO' ELSE 'VEGAN' END,
    cuisine_preference = CASE c.ordinal % 4 WHEN 0 THEN N'Món Việt, món Nhật' WHEN 1 THEN N'Món Việt'
        WHEN 2 THEN N'Món Việt, món Thái' ELSE N'Món Việt, món Hàn' END,
    preferred_difficulty = CASE WHEN c.ordinal % 3 = 0 THEN 'MEDIUM' ELSE 'EASY' END,
    max_cooking_time_min = CASE WHEN c.ordinal % 2 = 0 THEN 45 ELSE 30 END,
    date_of_birth = DATEADD(YEAR, -(20 + c.ordinal % 25), CONVERT(DATE, GETDATE())),
    biological_sex = CASE WHEN c.ordinal % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
    height_cm = 150 + (c.ordinal % 21), weight_kg = 48 + (c.ordinal % 26),
    activity_level = CASE c.ordinal % 4 WHEN 0 THEN 'VERY_ACTIVE' WHEN 1 THEN 'LIGHTLY_ACTIVE'
        WHEN 2 THEN 'MODERATELY_ACTIVE' ELSE 'SEDENTARY' END,
    nutrition_goal = CASE c.ordinal % 3 WHEN 0 THEN 'MAINTAIN_WEIGHT' WHEN 1 THEN 'IMPROVE_HEALTH'
        ELSE 'SUPPORT_TRAINING' END,
    nutrition_scope_confirmed = 1, onboarding_status = 'COMPLETED',
    avoid_none_confirmed = CASE WHEN u.email = 'demo-customer@mamxanh.local' THEN 0 ELSE 1 END,
    dislike_none_confirmed = 1, health_data_consent = 1,
    health_data_consent_at = COALESCE(u.health_data_consent_at, SYSUTCDATETIME()), updated_at = SYSUTCDATETIME()
FROM [USER] u JOIN @demo_customers c ON c.user_id = u.user_id;

DECLARE @current_monday DATE = DATEADD(DAY,
    -(DATEDIFF(DAY, CONVERT(DATE, '19000101', 112), CONVERT(DATE, GETDATE())) % 7), CONVERT(DATE, GETDATE()));
DECLARE @week_offsets TABLE (week_offset INT PRIMARY KEY);
INSERT INTO @week_offsets VALUES (0),(1),(2),(3);
INSERT INTO [MEAL_PLAN] (user_id, week_start_date)
SELECT c.user_id, DATEADD(DAY, -7 * w.week_offset, @current_monday)
FROM @demo_customers c CROSS JOIN @week_offsets w
WHERE NOT EXISTS (SELECT 1 FROM [MEAL_PLAN] mp WHERE mp.user_id = c.user_id
    AND mp.week_start_date = DATEADD(DAY, -7 * w.week_offset, @current_monday));

-- Rebuild only fixture-owned entries for these demo users and four weeks.
DELETE entry FROM [MEAL_PLAN_ENTRY] entry
JOIN [MEAL_PLAN] mp ON mp.meal_plan_id = entry.meal_plan_id
JOIN @demo_customers c ON c.user_id = mp.user_id
WHERE mp.week_start_date BETWEEN DATEADD(DAY, -21, @current_monday) AND @current_monday;

DECLARE @recipe_roster TABLE (recipe_id BIGINT PRIMARY KEY, ordinal INT);
DECLARE @recipe_titles TABLE (title NVARCHAR(120) PRIMARY KEY);
INSERT INTO @recipe_titles VALUES (N'Đậu hũ sốt cà chua'), (N'Rau muống xào tỏi');
INSERT INTO @recipe_titles SELECT title FROM @recipe_seed;
INSERT INTO @recipe_titles SELECT title FROM @showcase_recipes;
INSERT INTO @recipe_titles SELECT title FROM @extra_recipes;
UPDATE [RECIPE_POST] SET status = 'PUBLISHED',
    published_at = COALESCE(published_at, SYSUTCDATETIME()), updated_at = SYSUTCDATETIME()
WHERE author_id = @author_id AND title IN (SELECT title FROM @recipe_titles);
INSERT INTO @recipe_roster (recipe_id, ordinal)
SELECT recipe_id, CONVERT(INT, ROW_NUMBER() OVER (ORDER BY recipe_id))
FROM [RECIPE_POST] WHERE author_id = @author_id AND status = 'PUBLISHED'
  AND title IN (SELECT title FROM @recipe_titles);
DECLARE @recipe_total INT = (SELECT COUNT(*) FROM @recipe_roster);

;WITH slots AS (
    SELECT day_no, meal_type FROM (VALUES (0,'BREAKFAST'),(0,'LUNCH'),(0,'DINNER'),
        (1,'BREAKFAST'),(1,'LUNCH'),(1,'DINNER'),(2,'BREAKFAST'),(2,'LUNCH'),(2,'DINNER'),
        (3,'BREAKFAST'),(3,'LUNCH'),(3,'DINNER'),(4,'BREAKFAST'),(4,'LUNCH'),(4,'DINNER'),
        (5,'BREAKFAST'),(5,'LUNCH'),(5,'DINNER'),(6,'BREAKFAST'),(6,'LUNCH'),(6,'DINNER')) v(day_no,meal_type)
), plans AS (
    SELECT mp.meal_plan_id, c.ordinal customer_no, w.week_offset, mp.week_start_date
    FROM @demo_customers c JOIN [MEAL_PLAN] mp ON mp.user_id = c.user_id
    JOIN @week_offsets w ON mp.week_start_date = DATEADD(DAY, -7 * w.week_offset, @current_monday)
), assignments AS (
    SELECT p.meal_plan_id, p.week_start_date, s.day_no, s.meal_type,
        ((p.customer_no * 7 + p.week_offset * 21 + s.day_no * 3
            + CASE s.meal_type WHEN 'BREAKFAST' THEN 0 WHEN 'LUNCH' THEN 1 ELSE 2 END - 1) % @recipe_total) + 1 recipe_ordinal
    FROM plans p CROSS JOIN slots s
)
INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id, recipe_id, meal_date, meal_type, planned_servings)
SELECT a.meal_plan_id, r.recipe_id, DATEADD(DAY, a.day_no, a.week_start_date), a.meal_type, 2
FROM assignments a JOIN @recipe_roster r ON r.ordinal = a.recipe_ordinal;

DECLARE @reaction_targets TABLE (recipe_id BIGINT PRIMARY KEY, reaction_count INT, likes INT,
    dislikes INT, view_count INT);
INSERT INTO @reaction_targets
SELECT recipe_id,
    CASE WHEN ordinal <= 9 THEN 20 WHEN ordinal <= 22 THEN 15 ELSE 12 END,
    CASE WHEN ordinal <= 9 THEN 18 WHEN ordinal <= 22 THEN 12 ELSE 8 END,
    CASE WHEN ordinal <= 9 THEN 2 WHEN ordinal <= 22 THEN 3 ELSE 4 END,
    CASE WHEN ordinal <= 9 THEN 60 WHEN ordinal <= 22 THEN 35 ELSE 15 END
FROM @recipe_roster;

DELETE reaction FROM [RECIPE_REACTION] reaction
JOIN [USER] demo ON demo.user_id = reaction.user_id AND demo.email LIKE 'demo-%@mamxanh.local'
JOIN @reaction_targets target ON target.recipe_id = reaction.recipe_id;
;WITH demo_voters AS (
    SELECT user_id, CONVERT(INT, ROW_NUMBER() OVER (ORDER BY email)) ordinal
    FROM [USER] WHERE role IN ('CUSTOMER','EXPERT') AND account_status = 'ACTIVE'
      AND email LIKE 'demo-%@mamxanh.local' AND user_id <> @author_id
)
INSERT INTO [RECIPE_REACTION] (user_id, recipe_id, reaction_type)
SELECT voter.user_id, target.recipe_id,
    CASE WHEN voter.ordinal <= target.likes THEN 'LIKE' ELSE 'DISLIKE' END
FROM @reaction_targets target JOIN demo_voters voter ON voter.ordinal <= target.reaction_count;

DELETE follows FROM [USER_FOLLOW] follows
WHERE follower_user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'demo-%@mamxanh.local')
   OR followed_user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'demo-%@mamxanh.local');
;WITH members AS (
    SELECT user_id, CONVERT(INT, ROW_NUMBER() OVER (ORDER BY email)) ordinal, COUNT(*) OVER () member_count
    FROM [USER] WHERE role IN ('CUSTOMER','EXPERT') AND account_status = 'ACTIVE'
      AND email LIKE 'demo-%@mamxanh.local'
), desired AS (
    SELECT follower.user_id follower_user_id,
        ((follower.ordinal - 1 + step.follow_offset) % follower.member_count) + 1 followed_ordinal
    FROM members follower CROSS JOIN (VALUES (1),(5),(9),(13)) step(follow_offset)
), resolved AS (
    SELECT d.follower_user_id, followed.user_id followed_user_id
    FROM desired d JOIN members followed ON followed.ordinal = d.followed_ordinal
)
INSERT INTO [USER_FOLLOW] (follower_user_id, followed_user_id)
SELECT r.follower_user_id, r.followed_user_id FROM resolved r
WHERE r.follower_user_id <> r.followed_user_id
AND NOT EXISTS (SELECT 1 FROM [USER_FOLLOW] f WHERE f.follower_user_id = r.follower_user_id
    AND f.followed_user_id = r.followed_user_id);

DELETE FROM [RECIPE_VIEW] WHERE anonymous_viewer_hash LIKE 'demo-fixture-%';
;WITH numbers AS (
    SELECT TOP (60) CONVERT(INT, ROW_NUMBER() OVER (ORDER BY (SELECT NULL))) ordinal
    FROM sys.all_objects a CROSS JOIN sys.all_objects b
)
INSERT INTO [RECIPE_VIEW] (recipe_id, anonymous_viewer_hash, viewed_at)
SELECT target.recipe_id, CONCAT('demo-fixture-', target.recipe_id, '-', number.ordinal),
    DATEADD(MINUTE, -(CASE number.ordinal % 4 WHEN 0 THEN number.ordinal % 60
        WHEN 1 THEN 1440 + number.ordinal % 10080 WHEN 2 THEN 10080 + number.ordinal % 43200
        ELSE 43200 + number.ordinal * 120 END), SYSUTCDATETIME())
FROM @reaction_targets target JOIN numbers number ON number.ordinal <= target.view_count;

UPDATE post SET like_count = totals.likes, dislike_count = totals.dislikes,
    view_count = COALESCE(views.total_views, 0), updated_at = SYSUTCDATETIME()
FROM [RECIPE_POST] post
JOIN (SELECT reaction.recipe_id,
    SUM(CASE WHEN reaction.reaction_type = 'LIKE' THEN 1 ELSE 0 END) likes,
    SUM(CASE WHEN reaction.reaction_type = 'DISLIKE' THEN 1 ELSE 0 END) dislikes
    FROM [RECIPE_REACTION] reaction GROUP BY reaction.recipe_id) totals ON totals.recipe_id = post.recipe_id
LEFT JOIN (SELECT recipe_id, COUNT(*) total_views FROM [RECIPE_VIEW] GROUP BY recipe_id) views
    ON views.recipe_id = post.recipe_id
WHERE post.author_id = @author_id AND post.title IN (SELECT title FROM @recipe_titles);

COMMIT TRANSACTION;
