-- ============================================================================
-- Database Verification & Diagnostic Queries: queries.sql
-- Database Engine : Microsoft SQL Server 2019+
-- Project         : Mâm Xanh — Vegetarian Support System (SWP391)
-- Issue           : Refs #63 — Pha 2 Physical ERD & Schema
-- Author          : Trương Văn Khải
-- Date            : 2026-09-23
-- Source          : Logical ERD v1.0.0 + Data Dictionary v0.4.0 + schema.sql
-- ============================================================================
-- Purpose:
--   1. Schema Audit: Verify table counts, FK counts, indexes, and constraints.
--   2. Constraint Verification: Automated positive & negative test suites
--      proving that all database constraints, filtered unique indexes, and
--      cascade policies enforce business rules (BR-19, BR-20, BR-21, BR-23,
--      BR-29, BR-37, BR-53, BR-66, BR-69, BR-75) as designed.
--   3. Operational Queries: Practical queries demonstrating core queries
--      for recipes, nested comments, weekly meal plans, and audit logs.
--
-- Safety:
--   All test executions in Section 2 run inside an explicit TRANSACTION and
--   are ROLLED BACK at the end, leaving the database completely clean.
--
-- How to Run:
--   sqlcmd -S localhost\SQLEXPRESS -d MamXanhDB -i database/queries.sql
--   or execute directly in SQL Server Management Studio (SSMS) with F5.
-- ============================================================================

SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO
SET NOCOUNT ON;
GO


-- ============================================================================
-- PART 1: SCHEMA AUDIT & STATISTICAL QUERIES
-- ============================================================================

PRINT '====================================================================';
PRINT 'PART 1: SCHEMA AUDIT & OBJECT INVENTORY';
PRINT '====================================================================';

-- 1.1 Object Count Summary
-- Expected: 22 Tables, 38 FKs, 22 PKs, 5 UQ constraints, 41 Checks, 52 Defaults, 15 UNIT rows
SELECT 
    'Tables' AS ObjectType, COUNT(*) AS TotalCount, 22 AS ExpectedCount,
    CASE WHEN COUNT(*) = 22 THEN 'PASS' ELSE 'FAIL' END AS AuditStatus
FROM sys.tables WHERE is_ms_shipped = 0
UNION ALL
SELECT 'Foreign Keys', COUNT(*), 38, CASE WHEN COUNT(*) = 38 THEN 'PASS' ELSE 'FAIL' END
FROM sys.foreign_keys
UNION ALL
SELECT 'Primary Keys', COUNT(*), 22, CASE WHEN COUNT(*) = 22 THEN 'PASS' ELSE 'FAIL' END
FROM sys.key_constraints WHERE type = 'PK'
UNION ALL
SELECT 'Unique Constraints', COUNT(*), 5, CASE WHEN COUNT(*) = 5 THEN 'PASS' ELSE 'FAIL' END
FROM sys.key_constraints WHERE type = 'UQ'
UNION ALL
SELECT 'Check Constraints', COUNT(*), 41, CASE WHEN COUNT(*) = 41 THEN 'PASS' ELSE 'FAIL' END
FROM sys.check_constraints
UNION ALL
SELECT 'Default Constraints', COUNT(*), 52, CASE WHEN COUNT(*) = 52 THEN 'PASS' ELSE 'FAIL' END
FROM sys.default_constraints
UNION ALL
SELECT 'UNIT Seed Rows', COUNT(*), 15, CASE WHEN COUNT(*) = 15 THEN 'PASS' ELSE 'FAIL' END
FROM [UNIT];
GO

-- 1.2 Filtered Unique Indexes Audit
-- Expected: 5 filtered UNIQUE indexes + 2 filtered performance indexes = 7 filtered indexes
SELECT 
    OBJECT_NAME(i.object_id) AS TableName,
    i.name AS IndexName,
    i.is_unique AS IsUnique,
    i.filter_definition AS FilterCondition
FROM sys.indexes i
WHERE i.has_filter = 1
ORDER BY TableName, IndexName;
GO

-- 1.3 Foreign Key Cascade Policy Audit
-- Expected: Exactly 4 CASCADE, 1 SET NULL, 33 NO ACTION (solving Error 1785)
SELECT 
    fk.delete_referential_action_desc AS DeleteAction,
    COUNT(*) AS TotalCount,
    CASE 
        WHEN fk.delete_referential_action_desc = 'CASCADE' THEN '4 expected (MEDIA, INGREDIENT, ENTRY, ITEM)'
        WHEN fk.delete_referential_action_desc = 'SET_NULL' THEN '1 expected (RECIPE_VIEW.user_id)'
        WHEN fk.delete_referential_action_desc = 'NO_ACTION' THEN '33 expected (prevent Error 1785)'
        ELSE 'UNEXPECTED'
    END AS PolicyDescription
FROM sys.foreign_keys fk
GROUP BY fk.delete_referential_action_desc;
GO


-- ============================================================================
-- PART 2: AUTOMATED CONSTRAINT & INTEGRITY TEST SUITE
-- (Runs in isolated transaction with automatic rollback)
-- ============================================================================

PRINT '';
PRINT '====================================================================';
PRINT 'PART 2: BUSINESS CONSTRAINT VALIDATION TESTS (TRY / CATCH)';
PRINT '====================================================================';

BEGIN TRANSACTION;

BEGIN TRY
    -- ------------------------------------------------------------------------
    -- Setup Test Fixtures (Parent data needed for foreign key tests)
    -- ------------------------------------------------------------------------
    INSERT INTO [USER] (email, display_name, role, account_status)
    VALUES ('test_alice@test.local', N'Alice Customer', 'CUSTOMER', 'ACTIVE');
    DECLARE @AliceId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [USER] (email, display_name, role, account_status)
    VALUES ('test_bob@test.local', N'Bob Expert', 'EXPERT', 'ACTIVE');
    DECLARE @BobId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [USER] (email, display_name, role, account_status)
    VALUES ('test_charlie@test.local', N'Charlie Admin', 'ADMIN', 'ACTIVE');
    DECLARE @CharlieId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [INGREDIENT] (name, energy_kcal_100g, protein_g_100g, carbohydrate_g_100g,
                              total_fat_g_100g, fiber_g_100g, calcium_mg_100g, iron_mg_100g,
                              vitamin_b12_mcg_100g, zinc_mg_100g, source_name, reference_date)
    VALUES (N'Đậu phụ mơ test', 76.0, 8.0, 1.9, 4.8, 0.3, 350.0, 5.4, 0.0, 0.8, N'Viện Dinh Dưỡng', '2026-01-01');
    DECLARE @IngId BIGINT = SCOPE_IDENTITY();

    DECLARE @UnitGramId INT = (SELECT unit_id FROM [UNIT] WHERE code = 'g');

    INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category,
                               vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, status)
    VALUES (@BobId, N'Canh đậu hũ rong biển', N'Món canh thanh đạm',
            N'Bước 1: Rửa rong biển. Bước 2: Nấu nước dùng sôi. Bước 3: Thả đậu phụ và rong biển.',
            'SOUP', 'VEGAN', 'EASY', 4, 15, 20, 'PUBLISHED');
    DECLARE @RecipeId BIGINT = SCOPE_IDENTITY();

    -- ------------------------------------------------------------------------
    -- TC01: REPORT XOR Constraint (CK_REPORT_target_xor, BR-23)
    -- Must specify either recipe_id OR comment_id, NEVER both, NEVER neither.
    -- ------------------------------------------------------------------------
    -- Negative 1: Both provided
    BEGIN TRY
        INSERT INTO [REPORT] (reporter_id, recipe_id, comment_id, reason_code, description)
        VALUES (@AliceId, @RecipeId, 1, 'NON_VEGAN', N'Báo cáo lỗi cả 2');
        PRINT '  [FAIL] TC01a: Both recipe_id & comment_id should have been rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC01a: Rejected both recipe_id and comment_id (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- Negative 2: Neither provided
    BEGIN TRY
        INSERT INTO [REPORT] (reporter_id, recipe_id, comment_id, reason_code, description)
        VALUES (@AliceId, NULL, NULL, 'NON_VEGAN', N'Báo cáo rỗng cả 2');
        PRINT '  [FAIL] TC01b: NULL for both targets should have been rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC01b: Rejected empty targets (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- Positive: Only recipe_id provided
    INSERT INTO [REPORT] (reporter_id, recipe_id, comment_id, reason_code, description)
    VALUES (@AliceId, @RecipeId, NULL, 'NON_VEGAN', N'Món ăn có thành phần trứng');
    PRINT '  [PASS] TC01c: Accepted report targeting only recipe_id.';

    -- ------------------------------------------------------------------------
    -- TC02: Single Cover Image per Recipe (UQ_RECIPE_MEDIA_cover, BR-20)
    -- Filtered unique index: WHERE is_cover = 1
    -- ------------------------------------------------------------------------
    -- Insert 1st cover image -> SUCCESS
    INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
    VALUES (@RecipeId, 'https://storage.blob.core.windows.net/recipes/cover1.jpg', 'image/jpeg', 1, 1);

    -- Negative: Insert 2nd cover image for same recipe -> Must FAIL
    BEGIN TRY
        INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
        VALUES (@RecipeId, 'https://storage.blob.core.windows.net/recipes/cover2.jpg', 'image/jpeg', 2, 1);
        PRINT '  [FAIL] TC02: Duplicate cover image was not rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC02: Rejected 2nd cover image on same recipe (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- Positive: Non-cover image -> SUCCESS
    INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
    VALUES (@RecipeId, 'https://storage.blob.core.windows.net/recipes/detail1.jpg', 'image/jpeg', 2, 0);
    PRINT '  [PASS] TC02b: Accepted multiple non-cover images.';

    -- ------------------------------------------------------------------------
    -- TC03: Self-Follow Prevention (CK_USER_FOLLOW_no_self, BR-75)
    -- User cannot follow themselves
    -- ------------------------------------------------------------------------
    BEGIN TRY
        INSERT INTO [USER_FOLLOW] (follower_user_id, followed_user_id)
        VALUES (@AliceId, @AliceId);
        PRINT '  [FAIL] TC03: Self-follow was not rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC03: Rejected self-follow attempt (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- Positive: Follow another user -> SUCCESS
    INSERT INTO [USER_FOLLOW] (follower_user_id, followed_user_id)
    VALUES (@AliceId, @BobId);
    PRINT '  [PASS] TC03b: Accepted valid follow relationship.';

    -- ------------------------------------------------------------------------
    -- TC04: Single PENDING Expert Application per User (UQ_EXPERT_APP_pending, BR-21)
    -- Filtered unique index: WHERE status = 'PENDING'
    -- ------------------------------------------------------------------------
    INSERT INTO [EXPERT_APPLICATION] (user_id, bio_experience, vegetarian_type, sample_recipe_summary, status)
    VALUES (@AliceId, N'Kinh nghiệm 5 năm nấu chay', 'VEGAN', N'Công thức đậu phụ sốt cà', 'PENDING');

    -- Negative: 2nd PENDING application for same user -> Must FAIL
    BEGIN TRY
        INSERT INTO [EXPERT_APPLICATION] (user_id, bio_experience, vegetarian_type, sample_recipe_summary, status)
        VALUES (@AliceId, N'Đơn nộp lần 2', 'VEGAN', N'Công thức lẩu nấm', 'PENDING');
        PRINT '  [FAIL] TC04: 2nd PENDING application was not rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC04: Rejected 2nd PENDING application for same user (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC05: Fixed 11 Dish Categories (CK_RECIPE_POST_dish_category, Q1)
    -- Only approved technical codes allowed
    -- ------------------------------------------------------------------------
    BEGIN TRY
        INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category,
                                   vegetarian_type, difficulty, servings, prep_time_min, cook_time_min)
        VALUES (@BobId, N'Món chay lạ', N'Hướng dẫn nấu chi tiết trên 10 ký tự', 'FAST_FOOD',
                'VEGAN', 'EASY', 2, 10, 10);
        PRINT '  [FAIL] TC05: Unapproved category FAST_FOOD was not rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC05: Rejected invalid dish_category FAST_FOOD (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC06: Recipe Servings Range 1..50 (CK_RECIPE_POST_servings, BR-43)
    -- ------------------------------------------------------------------------
    BEGIN TRY
        INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category,
                                   vegetarian_type, difficulty, servings, prep_time_min, cook_time_min)
        VALUES (@BobId, N'Món số lượng 0', N'Hướng dẫn nấu chi tiết trên 10 ký tự', 'SOUP',
                'VEGAN', 'EASY', 0, 10, 10);
        PRINT '  [FAIL] TC06: Servings = 0 was not rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC06: Rejected servings = 0 (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC07: Non-negative Recipe Counters (CK_RECIPE_POST_like_count, Q2)
    -- ------------------------------------------------------------------------
    BEGIN TRY
        INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category,
                                   vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, like_count)
        VALUES (@BobId, N'Món like âm', N'Hướng dẫn nấu chi tiết trên 10 ký tự', 'SOUP',
                'VEGAN', 'EASY', 2, 10, 10, -5);
        PRINT '  [FAIL] TC07: Negative like_count was not rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC07: Rejected negative like_count (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC08: Comment Depth Limit 1..5 (CK_COMMENT_depth, BR-66)
    -- ------------------------------------------------------------------------
    BEGIN TRY
        INSERT INTO [COMMENT] (recipe_id, user_id, content, depth)
        VALUES (@RecipeId, @AliceId, N'Bình luận quá sâu', 6);
        PRINT '  [FAIL] TC08: Comment depth = 6 was not rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC08: Rejected comment depth = 6 (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC09: Duplicate Open Report on Same Recipe (UQ_REPORT_open_recipe, BR-29)
    -- ------------------------------------------------------------------------
    -- Alice already reported RecipeId in TC01c (status = PENDING)
    -- Duplicate report attempt by Alice on same recipe:
    BEGIN TRY
        INSERT INTO [REPORT] (reporter_id, recipe_id, comment_id, reason_code, description)
        VALUES (@AliceId, @RecipeId, NULL, 'SPAM_ADVERTISING', N'Báo cáo trùng');
        PRINT '  [FAIL] TC09: Duplicate open report on same recipe was not rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC09: Rejected duplicate open report by same reporter (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC10: Duplicate Recipe Slot in Meal Plan (UQ_MPE_unique_slot, BR-37)
    -- ------------------------------------------------------------------------
    INSERT INTO [MEAL_PLAN] (user_id, week_start_date)
    VALUES (@AliceId, '2026-09-28');
    DECLARE @MealPlanId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id, recipe_id, meal_date, meal_type, planned_servings)
    VALUES (@MealPlanId, @RecipeId, '2026-09-28', 'LUNCH', 2.0);

    -- Duplicate entry attempt for same recipe at same lunch:
    BEGIN TRY
        INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id, recipe_id, meal_date, meal_type, planned_servings)
        VALUES (@MealPlanId, @RecipeId, '2026-09-28', 'LUNCH', 1.0);
        PRINT '  [FAIL] TC10: Duplicate recipe in same meal slot was not rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC10: Rejected duplicate recipe in same meal slot (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC11: Payment Order Code Idempotency (UQ_PAYMENT_order_code, FR-13)
    -- ------------------------------------------------------------------------
    INSERT INTO [PAYMENT_TRANSACTION] (user_id, order_code, amount_vnd, status)
    VALUES (@AliceId, 'PAYOS_TXN_12345678', 49000, 'PAID');

    BEGIN TRY
        INSERT INTO [PAYMENT_TRANSACTION] (user_id, order_code, amount_vnd, status)
        VALUES (@AliceId, 'PAYOS_TXN_12345678', 49000, 'PAID');
        PRINT '  [FAIL] TC11: Duplicate order_code was not rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC11: Rejected duplicate payOS order_code (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC12: Health Metrics Validation (CK_USER_height_cm, CK_USER_weight_kg)
    -- ------------------------------------------------------------------------
    BEGIN TRY
        INSERT INTO [USER] (email, display_name, height_cm)
        VALUES ('test_short@test.local', N'Short User', 85.0);
        PRINT '  [FAIL] TC12a: Height < 100cm was not rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC12a: Rejected height < 100cm (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    BEGIN TRY
        INSERT INTO [USER] (email, display_name, weight_kg)
        VALUES ('test_light@test.local', N'Light User', 20.0);
        PRINT '  [FAIL] TC12b: Weight < 30kg was not rejected!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC12b: Rejected weight < 30kg (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC13: Cascade Deletion on Recipe Ownership (CASCADE)
    -- ------------------------------------------------------------------------
    INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category,
                               vegetarian_type, difficulty, servings, prep_time_min, cook_time_min)
    VALUES (@BobId, N'Món test cascade', N'Hướng dẫn nấu chi tiết trên 10 ký tự', 'SOUP',
            'VEGAN', 'EASY', 2, 10, 10);
    DECLARE @CascadeRecipeId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
    VALUES (@CascadeRecipeId, 'https://storage.blob.core.windows.net/recipes/cascade.jpg', 'image/jpeg', 1, 1);

    INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
    VALUES (@CascadeRecipeId, @IngId, @UnitGramId, 200.0);

    DELETE FROM [RECIPE_POST] WHERE recipe_id = @CascadeRecipeId;

    DECLARE @MediaCount INT = (SELECT COUNT(*) FROM [RECIPE_MEDIA] WHERE recipe_id = @CascadeRecipeId);
    DECLARE @IngCount INT = (SELECT COUNT(*) FROM [RECIPE_INGREDIENT] WHERE recipe_id = @CascadeRecipeId);

    IF @MediaCount = 0 AND @IngCount = 0
        PRINT '  [PASS] TC13: CASCADE deletion verified — media & ingredients deleted with recipe.';
    ELSE
        PRINT '  [FAIL] TC13: Child records remained after recipe deletion!';

    -- ------------------------------------------------------------------------
    -- TC13b: NO ACTION on Recipe when referenced by REPORT (Error 1785 prevention)
    -- ------------------------------------------------------------------------
    BEGIN TRY
        DELETE FROM [RECIPE_POST] WHERE recipe_id = @RecipeId;
        PRINT '  [FAIL] TC13b: Deleting recipe with open report should have been blocked!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC13b: Blocked recipe deletion with active report (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC14: SET NULL on Recipe View (SET NULL, Guest Support)
    -- ------------------------------------------------------------------------
    -- Create temporary recipe and view
    INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category,
                               vegetarian_type, difficulty, servings, prep_time_min, cook_time_min)
    VALUES (@BobId, N'Món xem thử', N'Hướng dẫn nấu trên 10 ký tự', 'ROLL', 'VEGAN', 'EASY', 2, 5, 5);
    DECLARE @TempRecipeId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [USER] (email, display_name) VALUES ('test_viewer@test.local', N'Temp Viewer');
    DECLARE @ViewerId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [RECIPE_VIEW] (recipe_id, user_id) VALUES (@TempRecipeId, @ViewerId);
    DECLARE @ViewId BIGINT = SCOPE_IDENTITY();

    DELETE FROM [USER] WHERE user_id = @ViewerId;

    DECLARE @ViewUserId BIGINT = (SELECT user_id FROM [RECIPE_VIEW] WHERE view_id = @ViewId);
    IF @ViewUserId IS NULL
        PRINT '  [PASS] TC14: SET NULL verified — view survives as anonymous Guest after user deletion.';
    ELSE
        PRINT '  [FAIL] TC14: user_id was not set to NULL upon user deletion!';

    -- ------------------------------------------------------------------------
    -- TC15: Prevent Permanent Deletion of Referenced Ingredient (BR-53, NO ACTION)
    -- ------------------------------------------------------------------------
    -- Assign ingredient to temp recipe
    INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
    VALUES (@TempRecipeId, @IngId, @UnitGramId, 100.0);

    BEGIN TRY
        DELETE FROM [INGREDIENT] WHERE ingredient_id = @IngId;
        PRINT '  [FAIL] TC15: Referenced ingredient deletion was not blocked!';
    END TRY
    BEGIN CATCH
        PRINT '  [PASS] TC15: Blocked permanent deletion of referenced ingredient (Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
    END CATCH;

END TRY
BEGIN CATCH
    PRINT '  [FATAL ERROR IN TEST SUITE]: ' + ERROR_MESSAGE();
END CATCH;

-- Always rollback test transactions to keep clean DB state
ROLLBACK TRANSACTION;
PRINT '  [CLEANUP] Transaction rolled back. Test database state preserved.';
GO


-- ============================================================================
-- PART 3: COMMON OPERATIONAL & REPORTING QUERIES
-- ============================================================================

PRINT '';
PRINT '====================================================================';
PRINT 'PART 3: SAMPLE OPERATIONAL QUERIES FOR APPLICATION LAYER';
PRINT '====================================================================';
GO

-- 3.1 Recipe Listing Query (with like_percentage calculation and pagination)
-- Business rule: like_percentage is computed on read, not stored (Q2, FR-17)
SELECT 
    r.recipe_id,
    r.title,
    r.dish_category,
    r.vegetarian_type,
    r.difficulty,
    r.servings,
    r.cook_time_min,
    r.like_count,
    r.dislike_count,
    r.view_count,
    CASE 
        WHEN (r.like_count + r.dislike_count) = 0 THEN NULL
        ELSE ROUND((CAST(r.like_count AS FLOAT) / (r.like_count + r.dislike_count)) * 100.0, 1)
    END AS like_percentage,
    u.display_name AS author_name,
    m.blob_url AS cover_image_url
FROM [RECIPE_POST] r
JOIN [USER] u ON r.author_id = u.user_id
LEFT JOIN [RECIPE_MEDIA] m ON r.recipe_id = m.recipe_id AND m.is_cover = 1
WHERE r.status = 'PUBLISHED'
ORDER BY r.published_at DESC;
GO

-- 3.2 Weekly Meal Plan Nutrition Aggregation Query (FR-09, FR-37)
-- Aggregates daily energy & macronutrients across all recipes in a meal plan
SELECT 
    mpe.meal_date,
    mpe.meal_type,
    r.title AS recipe_title,
    mpe.planned_servings,
    ROUND(SUM((i.energy_kcal_100g / 100.0) * (ri.quantity * u.base_factor) * (mpe.planned_servings / r.servings)), 1) AS total_energy_kcal,
    ROUND(SUM((i.protein_g_100g / 100.0) * (ri.quantity * u.base_factor) * (mpe.planned_servings / r.servings)), 1) AS total_protein_g
FROM [MEAL_PLAN_ENTRY] mpe
JOIN [RECIPE_POST] r ON mpe.recipe_id = r.recipe_id
JOIN [RECIPE_INGREDIENT] ri ON r.recipe_id = ri.recipe_id
JOIN [INGREDIENT] i ON ri.ingredient_id = i.ingredient_id
JOIN [UNIT] u ON ri.unit_id = u.unit_id
WHERE mpe.meal_plan_id = 1
GROUP BY mpe.meal_date, mpe.meal_type, r.title, mpe.planned_servings, r.servings
ORDER BY mpe.meal_date, 
    CASE mpe.meal_type WHEN 'BREAKFAST' THEN 1 WHEN 'LUNCH' THEN 2 WHEN 'DINNER' THEN 3 END;
GO

-- 3.3 Active Subscription Check Query (FR-10, FR-13, BR-02)
-- Used by Spring Security / Auth interceptor to verify AI feature access
SELECT 
    s.subscription_id,
    s.user_id,
    s.tier,
    s.status,
    s.starts_at,
    s.ends_at,
    CASE 
        WHEN s.status = 'ACTIVE' AND SYSUTCDATETIME() BETWEEN s.starts_at AND s.ends_at THEN 1
        ELSE 0 
    END AS is_currently_valid
FROM [SUBSCRIPTION] s
WHERE s.user_id = 1
  AND s.status = 'ACTIVE';
GO

PRINT '====================================================================';
PRINT 'END OF QUERIES EXECUTION';
PRINT '====================================================================';
GO
