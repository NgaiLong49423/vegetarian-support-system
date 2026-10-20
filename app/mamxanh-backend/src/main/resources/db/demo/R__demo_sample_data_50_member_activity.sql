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

DECLARE @author_id BIGINT;
SELECT @author_id = user_id FROM [USER] WHERE email = 'demo-expert@mamxanh.local';
DECLARE @customer_id BIGINT;
SELECT @customer_id = user_id FROM [USER] WHERE email = 'demo-customer@mamxanh.local';
DECLARE @pepper BIGINT;
SELECT @pepper = ingredient_id FROM [INGREDIENT] WHERE name = N'Tiêu đen';
DECLARE @sample_week_start DATE = CONVERT(DATE, SYSUTCDATETIME());
DECLARE @sample_meal_plan_id BIGINT;
SET @sample_week_start = DATEADD(DAY,
    -(DATEDIFF(DAY, CONVERT(DATE, '19000101', 112), @sample_week_start) % 7), @sample_week_start);
IF NOT EXISTS (SELECT 1 FROM [MEAL_PLAN] WHERE user_id = @customer_id AND week_start_date = @sample_week_start)
    INSERT INTO [MEAL_PLAN] (user_id, week_start_date) VALUES (@customer_id, @sample_week_start);
SELECT @sample_meal_plan_id = meal_plan_id FROM [MEAL_PLAN]
WHERE user_id = @customer_id AND week_start_date = @sample_week_start;

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
INSERT INTO @recipe_titles VALUES
    (N'Đậu hũ sốt cà chua'), (N'Rau muống xào tỏi'),
    (N'Nấm đùi gà kho tiêu'), (N'Canh nấm hương cà rốt'), (N'Khoai lang hấp'), (N'Bông cải xanh xào nấm'), (N'Súp đậu lăng rau củ'),
    (N'Mì udon xào rau củ kiểu Nhật'), (N'Cơm chiên dứa đậu phộng'), (N'Phở cuốn nấm bào ngư'), (N'Bánh xèo nấm và giá đỗ'),
    (N'Cháo bí đỏ đậu xanh'), (N'Đậu hũ sốt nấm mỡ'), (N'Bánh mì nấm áp chảo'), (N'Bún chả giò chay rau thơm'),
    (N'Lẩu nấm cải thảo'), (N'Chả giò khoai môn đậu xanh'), (N'Cơm nắm rong biển mè trắng'), (N'Canh chua nấm đậu bắp'),
    (N'Mì Ý sốt cà tím nấm'), (N'Pudding hạt chia xoài'), (N'Rau ngót nấu nấm kim châm'), (N'Bí đỏ nướng mè trắng'),
    (N'Cơm gạo lứt đậu gà rau củ'), (N'Cháo yến mạch nấm mộc nhĩ'), (N'Salad quinoa bơ lựu'),
    (N'Đậu hũ áp chảo sốt hạt điều'), (N'Bún tempeh rau sống'), (N'Khoai tây nướng hạt bí'),
    (N'Súp đậu trắng cải xoăn'), (N'Bánh quinoa chuối dâu tây'), (N'Ớt chuông nhồi gạo lứt'),
    (N'Mì seitan xào măng tây'), (N'Canh kombu đậu hũ'), (N'Bí ngòi cuộn hạt điều'),
    (N'Súp lơ trắng nghiền hạnh nhân'), (N'Đậu đỏ hầm cà chua'), (N'Yến mạch qua đêm táo'),
    (N'Gỏi romaine dưa leo cam'), (N'Đậu nành hạt sốt cà tím'), (N'Bánh khoai lang hạt óc chó'),
    (N'Đậu gà nghiền bánh mì'), (N'Salad trái cây hạt chia'), (N'Canh rau dền mướp hương nấm rơm');

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

UPDATE post SET youtube_url = 'https://www.youtube.com/watch?v=7wtfhZwyrcc'
FROM [RECIPE_POST] post WHERE post.title = N'Mì udon xào rau củ kiểu Nhật';

UPDATE post SET youtube_url = 'https://www.youtube.com/watch?v=kXYiU_JCYtU'
FROM [RECIPE_POST] post WHERE post.title = N'Bánh xèo nấm và giá đỗ';

UPDATE post SET youtube_url = 'https://www.youtube.com/watch?v=fJ9rUzIMcZQ'
FROM [RECIPE_POST] post WHERE post.title = N'Đậu hũ sốt nấm mỡ';

UPDATE post SET youtube_url = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ'
FROM [RECIPE_POST] post WHERE post.title = N'Canh chua nấm đậu bắp';

UPDATE post SET youtube_url = 'https://www.youtube.com/watch?v=7wtfhZwyrcc'
FROM [RECIPE_POST] post WHERE post.title = N'Phở cuốn nấm bào ngư';

COMMIT TRANSACTION;
