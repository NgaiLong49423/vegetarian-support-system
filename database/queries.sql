-- ============================================================================
-- Database Verification & Diagnostic Queries: queries.sql
-- Database Engine : Microsoft SQL Server 2019+
-- Project         : Mâm Xanh — Vegetarian Support System (SWP391)
-- Issue           : Refs #63 — Pha 2 Physical ERD & Schema
-- Author          : Trương Văn Khải
-- Date            : 2026-09-25
-- Source          : Logical ERD v1.0.0 + Data Dictionary v0.6.0 + schema.sql
-- ============================================================================
-- Purpose:
--   1. Schema Audit: Verify table counts, FK counts, indexes, and constraints.
--   2. Constraint Verification: Automated positive & negative test suites
--      proving that all database constraints, filtered unique indexes, and
--      cascade policies enforce business rules as designed.
--      Preserves TC01..TC15 and adds TC16..TC37 for all newly implemented
--      constraints from Data Dictionary v0.6.0.
--      Every negative test verifies the EXACT constraint name in ERROR_MESSAGE().
--   3. Operational Queries: Practical queries demonstrating core queries
--      for recipes, nested comments, weekly meal plans, and subscriptions.
--
-- Safety:
--   All test executions in Section 2 run inside an explicit TRANSACTION and
--   are ROLLED BACK at the end, leaving the database completely clean.
--
-- How to Run:
--   sqlcmd -S localhost\SQLEXPRESS -E -I -d MamXanhDB -i database/queries.sql
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
-- Expected: 22 Tables, 38 FKs, 22 PKs, 9 UQ constraints, 57 Checks, 55 Defaults, 15 UNIT rows
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
SELECT 'Unique Constraints', COUNT(*), 9, CASE WHEN COUNT(*) = 9 THEN 'PASS' ELSE 'FAIL' END
FROM sys.key_constraints WHERE type = 'UQ'
UNION ALL
SELECT 'Check Constraints', COUNT(*), 57, CASE WHEN COUNT(*) = 57 THEN 'PASS' ELSE 'FAIL' END
FROM sys.check_constraints
UNION ALL
SELECT 'Default Constraints', COUNT(*), 55, CASE WHEN COUNT(*) = 55 THEN 'PASS' ELSE 'FAIL' END
FROM sys.default_constraints
UNION ALL
SELECT 'UNIT Seed Rows', COUNT(*), 15, CASE WHEN COUNT(*) = 15 THEN 'PASS' ELSE 'FAIL' END
FROM [UNIT];
GO

-- 1.2 Filtered Indexes Audit
-- Expected: 8 filtered UNIQUE indexes (1 google_subject + 7 Section 10) + 2 filtered performance indexes = 10 filtered indexes
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
        IF ERROR_MESSAGE() LIKE '%CK_REPORT_target_xor%'
            PRINT '  [PASS] TC01a: Rejected both targets (exact CK_REPORT_target_xor, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC01a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Negative 2: Neither provided
    BEGIN TRY
        INSERT INTO [REPORT] (reporter_id, recipe_id, comment_id, reason_code, description)
        VALUES (@AliceId, NULL, NULL, 'NON_VEGAN', N'Báo cáo rỗng cả 2');
        PRINT '  [FAIL] TC01b: NULL for both targets should have been rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_REPORT_target_xor%'
            PRINT '  [PASS] TC01b: Rejected empty targets (exact CK_REPORT_target_xor, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC01b: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: Only recipe_id provided
    INSERT INTO [REPORT] (reporter_id, recipe_id, comment_id, reason_code, description)
    VALUES (@AliceId, @RecipeId, NULL, 'NON_VEGAN', N'Món ăn có thành phần trứng');
    PRINT '  [PASS] TC01c: Accepted report targeting only recipe_id.';

    -- ------------------------------------------------------------------------
    -- TC02: Single Cover Image per Recipe (UQ_RECIPE_MEDIA_cover, BR-20)
    -- Filtered unique index: WHERE is_cover = 1
    -- ------------------------------------------------------------------------
    INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
    VALUES (@RecipeId, 'https://storage.blob.core.windows.net/recipes/cover1.jpg', 'image/jpeg', 1, 1);

    -- Negative: Insert 2nd cover image for same recipe -> Must FAIL
    BEGIN TRY
        INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover)
        VALUES (@RecipeId, 'https://storage.blob.core.windows.net/recipes/cover2.jpg', 'image/jpeg', 2, 1);
        PRINT '  [FAIL] TC02: Duplicate cover image was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%UQ_RECIPE_MEDIA_cover%'
            PRINT '  [PASS] TC02: Rejected 2nd cover image on same recipe (exact UQ_RECIPE_MEDIA_cover, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC02: Caught unexpected error: ' + ERROR_MESSAGE();
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
        IF ERROR_MESSAGE() LIKE '%CK_USER_FOLLOW_no_self%'
            PRINT '  [PASS] TC03: Rejected self-follow attempt (exact CK_USER_FOLLOW_no_self, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC03: Caught unexpected error: ' + ERROR_MESSAGE();
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
        IF ERROR_MESSAGE() LIKE '%UQ_EXPERT_APP_pending%'
            PRINT '  [PASS] TC04: Rejected 2nd PENDING application for same user (exact UQ_EXPERT_APP_pending, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC04: Caught unexpected error: ' + ERROR_MESSAGE();
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
        IF ERROR_MESSAGE() LIKE '%CK_RECIPE_POST_dish_category%'
            PRINT '  [PASS] TC05: Rejected invalid dish_category FAST_FOOD (exact CK_RECIPE_POST_dish_category, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC05: Caught unexpected error: ' + ERROR_MESSAGE();
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
        IF ERROR_MESSAGE() LIKE '%CK_RECIPE_POST_servings%'
            PRINT '  [PASS] TC06: Rejected servings = 0 (exact CK_RECIPE_POST_servings, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC06: Caught unexpected error: ' + ERROR_MESSAGE();
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
        IF ERROR_MESSAGE() LIKE '%CK_RECIPE_POST_like_count%'
            PRINT '  [PASS] TC07: Rejected negative like_count (exact CK_RECIPE_POST_like_count, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC07: Caught unexpected error: ' + ERROR_MESSAGE();
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
        IF ERROR_MESSAGE() LIKE '%CK_COMMENT_depth%' OR ERROR_MESSAGE() LIKE '%CK_COMMENT_root_reply%'
            PRINT '  [PASS] TC08: Rejected comment depth = 6 (exact CK_COMMENT constraint, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC08: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC09: Duplicate Open Report on Same Recipe (UQ_REPORT_open_recipe, BR-29)
    -- ------------------------------------------------------------------------
    -- Negative: Duplicate report attempt by Alice on same recipe:
    BEGIN TRY
        INSERT INTO [REPORT] (reporter_id, recipe_id, comment_id, reason_code, description)
        VALUES (@AliceId, @RecipeId, NULL, 'SPAM_ADVERTISING', N'Báo cáo trùng');
        PRINT '  [FAIL] TC09: Duplicate open report on same recipe was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%UQ_REPORT_open_recipe%'
            PRINT '  [PASS] TC09: Rejected duplicate open report (exact UQ_REPORT_open_recipe, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC09: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC10: Duplicate Recipe Slot in Meal Plan (UQ_MPE_unique_slot, BR-37)
    -- ------------------------------------------------------------------------
    INSERT INTO [MEAL_PLAN] (user_id, week_start_date)
    VALUES (@AliceId, '2026-09-28');
    DECLARE @MealPlanId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id, recipe_id, meal_date, meal_type, planned_servings)
    VALUES (@MealPlanId, @RecipeId, '2026-09-28', 'LUNCH', 2.0);

    -- Negative: Duplicate entry attempt for same recipe at same lunch:
    BEGIN TRY
        INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id, recipe_id, meal_date, meal_type, planned_servings)
        VALUES (@MealPlanId, @RecipeId, '2026-09-28', 'LUNCH', 1.0);
        PRINT '  [FAIL] TC10: Duplicate recipe in same meal slot was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%UQ_MPE_unique_slot%'
            PRINT '  [PASS] TC10: Rejected duplicate recipe in same meal slot (exact UQ_MPE_unique_slot, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC10: Caught unexpected error: ' + ERROR_MESSAGE();
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
        IF ERROR_MESSAGE() LIKE '%UQ_PAYMENT_order_code%'
            PRINT '  [PASS] TC11: Rejected duplicate payOS order_code (exact UQ_PAYMENT_order_code, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC11: Caught unexpected error: ' + ERROR_MESSAGE();
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
        IF ERROR_MESSAGE() LIKE '%CK_USER_height_cm%'
            PRINT '  [PASS] TC12a: Rejected height < 100cm (exact CK_USER_height_cm, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC12a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    BEGIN TRY
        INSERT INTO [USER] (email, display_name, weight_kg)
        VALUES ('test_light@test.local', N'Light User', 20.0);
        PRINT '  [FAIL] TC12b: Weight < 30kg was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_USER_weight_kg%'
            PRINT '  [PASS] TC12b: Rejected weight < 30kg (exact CK_USER_weight_kg, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC12b: Caught unexpected error: ' + ERROR_MESSAGE();
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
        IF ERROR_MESSAGE() LIKE '%FK_REPORT_RECIPE%'
            PRINT '  [PASS] TC13b: Blocked recipe deletion with active report (exact FK_REPORT_RECIPE, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC13b: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC14: SET NULL on Recipe View (SET NULL, Guest Support)
    -- ------------------------------------------------------------------------
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
    INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
    VALUES (@TempRecipeId, @IngId, @UnitGramId, 100.0);

    BEGIN TRY
        DELETE FROM [INGREDIENT] WHERE ingredient_id = @IngId;
        PRINT '  [FAIL] TC15: Referenced ingredient deletion was not blocked!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%FK_RECIPE_INGREDIENT_INGREDIENT%'
            PRINT '  [PASS] TC15: Blocked deletion of referenced ingredient (exact FK_RECIPE_INGREDIENT_INGREDIENT, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC15: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;


    -- ========================================================================
    -- TC16..TC35: NEW CONSTRAINT VERIFICATION (Data Dictionary v0.6.0)
    -- ========================================================================

    -- ------------------------------------------------------------------------
    -- TC16: INGREDIENT Nutrition Support Constraint (CK_INGREDIENT_nutrition_supported)
    -- When nutrition_supported = 1, all 9 nutrition columns and source_url must NOT be NULL.
    -- ------------------------------------------------------------------------
    -- Negative: nutrition_supported = 1 with NULL nutrition indicators
    BEGIN TRY
        INSERT INTO [INGREDIENT] (name, nutrition_supported, source_name, reference_date)
        VALUES (N'Nguyên liệu thiếu dinh dưỡng', 1, N'Viện Dinh Dưỡng', '2026-01-01');
        PRINT '  [FAIL] TC16a: Supported ingredient missing nutrition indicators was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_INGREDIENT_nutrition_supported%'
            PRINT '  [PASS] TC16a: Rejected nutrition_supported=1 with missing indicators (exact CK_INGREDIENT_nutrition_supported, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC16a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive 1: nutrition_supported = 0 with NULL nutrition indicators
    INSERT INTO [INGREDIENT] (name, nutrition_supported, source_name, reference_date)
    VALUES (N'Nguyên liệu thô chưa có dinh dưỡng', 0, N'Dân gian', '2026-01-01');
    PRINT '  [PASS] TC16b: Accepted nutrition_supported=0 with NULL indicators.';

    -- Positive 2: nutrition_supported = 1 with complete 9 indicators and source_url
    INSERT INTO [INGREDIENT] (name, energy_kcal_100g, protein_g_100g, carbohydrate_g_100g,
                              total_fat_g_100g, fiber_g_100g, calcium_mg_100g, iron_mg_100g,
                              vitamin_b12_mcg_100g, zinc_mg_100g, source_name, source_url,
                              reference_date, nutrition_supported)
    VALUES (N'Nấm hương chuẩn dinh dưỡng', 34.0, 2.2, 6.8, 0.5, 2.5, 2.0, 0.4, 0.0, 0.3,
            N'USDA FoodData', 'https://fdc.nal.usda.gov/12345', '2026-01-01', 1);
    PRINT '  [PASS] TC16c: Accepted nutrition_supported=1 with full 9 indicators and source_url.';

    -- ------------------------------------------------------------------------
    -- TC17: USER Date of Birth Validation (CK_USER_date_of_birth)
    -- 1900-01-01 <= date_of_birth <= today
    -- ------------------------------------------------------------------------
    -- Negative 1: Future date
    BEGIN TRY
        INSERT INTO [USER] (email, display_name, date_of_birth)
        VALUES ('test_future_dob@test.local', N'Future User', DATEADD(DAY, 1, CAST(GETDATE() AS DATE)));
        PRINT '  [FAIL] TC17a: Future date_of_birth was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_USER_date_of_birth%'
            PRINT '  [PASS] TC17a: Rejected future date_of_birth (exact CK_USER_date_of_birth, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC17a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Negative 2: Before 1900-01-01
    BEGIN TRY
        INSERT INTO [USER] (email, display_name, date_of_birth)
        VALUES ('test_ancient_dob@test.local', N'Ancient User', '1899-12-31');
        PRINT '  [FAIL] TC17b: Ancient date_of_birth was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_USER_date_of_birth%'
            PRINT '  [PASS] TC17b: Rejected date_of_birth < 1900-01-01 (exact CK_USER_date_of_birth, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC17b: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: Valid date_of_birth
    INSERT INTO [USER] (email, display_name, date_of_birth)
    VALUES ('test_valid_dob@test.local', N'Valid DOB User', '1995-05-15');
    PRINT '  [PASS] TC17c: Accepted valid date_of_birth (1995-05-15).';

    -- ------------------------------------------------------------------------
    -- TC18: USER Activity Level 4 Values (CK_USER_activity_level)
    -- EXTRA_ACTIVE dropped; allows SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE.
    -- ------------------------------------------------------------------------
    BEGIN TRY
        INSERT INTO [USER] (email, display_name, activity_level)
        VALUES ('test_extra_active@test.local', N'Extra Active User', 'EXTRA_ACTIVE');
        PRINT '  [FAIL] TC18a: Dropped EXTRA_ACTIVE level was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_USER_activity_level%'
            PRINT '  [PASS] TC18a: Rejected dropped EXTRA_ACTIVE level (exact CK_USER_activity_level, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC18a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: Valid activity_level
    INSERT INTO [USER] (email, display_name, activity_level)
    VALUES ('test_very_active@test.local', N'Very Active User', 'VERY_ACTIVE');
    PRINT '  [PASS] TC18b: Accepted valid activity_level VERY_ACTIVE.';

    -- ------------------------------------------------------------------------
    -- TC19: USER Nutrition Goal Constraint (CK_USER_nutrition_goal)
    -- Allows MAINTAIN_WEIGHT, IMPROVE_HEALTH, SUPPORT_TRAINING.
    -- ------------------------------------------------------------------------
    BEGIN TRY
        INSERT INTO [USER] (email, display_name, nutrition_goal)
        VALUES ('test_invalid_goal@test.local', N'Invalid Goal User', 'LOSE_WEIGHT_FAST');
        PRINT '  [FAIL] TC19a: Invalid nutrition_goal LOSE_WEIGHT_FAST was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_USER_nutrition_goal%'
            PRINT '  [PASS] TC19a: Rejected invalid nutrition_goal (exact CK_USER_nutrition_goal, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC19a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: Valid nutrition_goal
    INSERT INTO [USER] (email, display_name, nutrition_goal)
    VALUES ('test_valid_goal@test.local', N'Valid Goal User', 'IMPROVE_HEALTH');
    PRINT '  [PASS] TC19b: Accepted valid nutrition_goal IMPROVE_HEALTH.';

    -- ------------------------------------------------------------------------
    -- TC20: USER Onboarding Status Constraint & Default (CK_USER_onboarding_status, DF_USER_onboarding_status)
    -- ------------------------------------------------------------------------
    BEGIN TRY
        INSERT INTO [USER] (email, display_name, onboarding_status)
        VALUES ('test_invalid_onb@test.local', N'Invalid Onboarding', 'IN_PROGRESS');
        PRINT '  [FAIL] TC20a: Invalid onboarding_status was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_USER_onboarding_status%'
            PRINT '  [PASS] TC20a: Rejected invalid onboarding_status (exact CK_USER_onboarding_status, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC20a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: Verify default 'NOT_STARTED'
    INSERT INTO [USER] (email, display_name)
    VALUES ('test_default_onb@test.local', N'Default Onboarding User');
    DECLARE @DefaultOnbStatus VARCHAR(20) = (SELECT onboarding_status FROM [USER] WHERE email = 'test_default_onb@test.local');
    IF @DefaultOnbStatus = 'NOT_STARTED'
        PRINT '  [PASS] TC20b: Verified default onboarding_status = NOT_STARTED.';
    ELSE
        PRINT '  [FAIL] TC20b: Default onboarding_status expected NOT_STARTED, found: ' + ISNULL(@DefaultOnbStatus, 'NULL');

    -- ------------------------------------------------------------------------
    -- TC21: USER_INGREDIENT_PREFERENCE Type Constraint (CK_UIP_preference_type)
    -- ALLERGY dropped; only allows AVOID or DISLIKE.
    -- ------------------------------------------------------------------------
    BEGIN TRY
        INSERT INTO [USER_INGREDIENT_PREFERENCE] (user_id, ingredient_id, preference_type)
        VALUES (@AliceId, @IngId, 'ALLERGY');
        PRINT '  [FAIL] TC21a: Dropped preference_type ALLERGY was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_UIP_preference_type%'
            PRINT '  [PASS] TC21a: Rejected dropped preference_type ALLERGY (exact CK_UIP_preference_type, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC21a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: AVOID and DISLIKE accepted
    INSERT INTO [USER_INGREDIENT_PREFERENCE] (user_id, ingredient_id, preference_type)
    VALUES (@AliceId, @IngId, 'AVOID');
    PRINT '  [PASS] TC21b: Accepted valid preference_type AVOID.';

    -- ------------------------------------------------------------------------
    -- TC22: USER_INGREDIENT_PREFERENCE Target & Blank Checks (CK_UIP_target, CK_UIP_custom_name_not_blank)
    -- ------------------------------------------------------------------------
    -- Negative 1: Both NULL
    BEGIN TRY
        INSERT INTO [USER_INGREDIENT_PREFERENCE] (user_id, ingredient_id, custom_ingredient_name, preference_type)
        VALUES (@AliceId, NULL, NULL, 'DISLIKE');
        PRINT '  [FAIL] TC22a: Both ingredient_id & custom_name NULL was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_UIP_target%'
            PRINT '  [PASS] TC22a: Rejected both targets NULL (exact CK_UIP_target, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC22a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Negative 2: Whitespace-only custom name
    BEGIN TRY
        INSERT INTO [USER_INGREDIENT_PREFERENCE] (user_id, ingredient_id, custom_ingredient_name, preference_type)
        VALUES (@AliceId, NULL, N'   ' + NCHAR(9) + NCHAR(160), 'DISLIKE');
        PRINT '  [FAIL] TC22b: Whitespace-only custom_ingredient_name was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_UIP_custom_name_not_blank%'
            PRINT '  [PASS] TC22b: Rejected whitespace-only custom_name (exact CK_UIP_custom_name_not_blank, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC22b: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: Valid custom ingredient name
    INSERT INTO [USER_INGREDIENT_PREFERENCE] (user_id, ingredient_id, custom_ingredient_name, preference_type)
    VALUES (@AliceId, NULL, N'Rau cần tây dại', 'DISLIKE');
    PRINT '  [PASS] TC22c: Accepted valid custom_ingredient_name.';

    -- ------------------------------------------------------------------------
    -- TC23: USER_INGREDIENT_PREFERENCE Unique Constraints (UQ_UIP_user_ingredient, UQ_UIP_user_custom_name)
    -- ------------------------------------------------------------------------
    -- Negative 1: Duplicate standard ingredient preference for same user
    BEGIN TRY
        INSERT INTO [USER_INGREDIENT_PREFERENCE] (user_id, ingredient_id, preference_type)
        VALUES (@AliceId, @IngId, 'DISLIKE');
        PRINT '  [FAIL] TC23a: Duplicate preference for standard ingredient was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%UQ_UIP_user_ingredient%'
            PRINT '  [PASS] TC23a: Rejected duplicate preference for standard ingredient (exact UQ_UIP_user_ingredient, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC23a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Negative 2: Duplicate custom ingredient preference for same user
    BEGIN TRY
        INSERT INTO [USER_INGREDIENT_PREFERENCE] (user_id, ingredient_id, custom_ingredient_name, preference_type)
        VALUES (@AliceId, NULL, N'Rau cần tây dại', 'AVOID');
        PRINT '  [FAIL] TC23b: Duplicate preference for custom ingredient name was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%UQ_UIP_user_custom_name%'
            PRINT '  [PASS] TC23b: Rejected duplicate custom ingredient name (exact UQ_UIP_user_custom_name, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC23b: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC24: RECIPE_POST Total Cooking Time Constraint (CK_RECIPE_POST_total_time)
    -- prep_time_min + cook_time_min > 0
    -- ------------------------------------------------------------------------
    BEGIN TRY
        INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category,
                                   vegetarian_type, difficulty, servings, prep_time_min, cook_time_min)
        VALUES (@BobId, N'Món 0 phút', N'Hướng dẫn nấu chi tiết trên 10 ký tự', 'SOUP',
                'VEGAN', 'EASY', 2, 0, 0);
        PRINT '  [FAIL] TC24a: Total cooking time = 0 was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_RECIPE_POST_total_time%'
            PRINT '  [PASS] TC24a: Rejected prep + cook = 0 (exact CK_RECIPE_POST_total_time, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC24a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: prep=0, cook=10 -> total 10 > 0
    INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category,
                               vegetarian_type, difficulty, servings, prep_time_min, cook_time_min)
    VALUES (@BobId, N'Món nấu ngay', N'Hướng dẫn nấu chi tiết trên 10 ký tự', 'SOUP',
                'VEGAN', 'EASY', 2, 0, 10);
    PRINT '  [PASS] TC24b: Accepted prep=0, cook=10 (> 0).';

    -- ------------------------------------------------------------------------
    -- TC25: RECIPE_POST Instructions Length 10..5000 (CK_RECIPE_POST_instructions_len)
    -- ------------------------------------------------------------------------
    -- Negative 1: Length < 10
    BEGIN TRY
        INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category,
                                   vegetarian_type, difficulty, servings, prep_time_min, cook_time_min)
        VALUES (@BobId, N'Món ngắn', REPLICATE('A', 9), 'SOUP', 'VEGAN', 'EASY', 2, 5, 5);
        PRINT '  [FAIL] TC25a: Instructions < 10 chars was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_RECIPE_POST_instructions_len%'
            PRINT '  [PASS] TC25a: Rejected instructions < 10 chars (exact CK_RECIPE_POST_instructions_len, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC25a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Negative 2: Length > 5000
    BEGIN TRY
        INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category,
                                   vegetarian_type, difficulty, servings, prep_time_min, cook_time_min)
        VALUES (@BobId, N'Món siêu dài', REPLICATE(CAST(N'A' AS NVARCHAR(MAX)), 5001), 'SOUP', 'VEGAN', 'EASY', 2, 5, 5);
        PRINT '  [FAIL] TC25b: Instructions > 5000 chars was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_RECIPE_POST_instructions_len%'
            PRINT '  [PASS] TC25b: Rejected instructions > 5000 chars (exact CK_RECIPE_POST_instructions_len, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC25b: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC26: RECIPE_POST Status Check & Default (CK_RECIPE_POST_status, DF_RECIPE_POST_status)
    -- DRAFT dropped; allows PUBLISHED, HIDDEN, DELETED. Default = 'PUBLISHED'.
    -- ------------------------------------------------------------------------
    BEGIN TRY
        INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category,
                                   vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, status)
        VALUES (@BobId, N'Món draft', N'Hướng dẫn nấu chi tiết trên 10 ký tự', 'SOUP',
                'VEGAN', 'EASY', 2, 5, 5, 'DRAFT');
        PRINT '  [FAIL] TC26a: Dropped status DRAFT was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_RECIPE_POST_status%'
            PRINT '  [PASS] TC26a: Rejected status DRAFT (exact CK_RECIPE_POST_status, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC26a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: Default status = 'PUBLISHED'
    INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category,
                               vegetarian_type, difficulty, servings, prep_time_min, cook_time_min)
    VALUES (@BobId, N'Món default status', N'Hướng dẫn nấu chi tiết trên 10 ký tự', 'SOUP',
            'VEGAN', 'EASY', 2, 5, 5);
    DECLARE @DefaultRecipeStatus VARCHAR(20) = (SELECT status FROM [RECIPE_POST] WHERE title = N'Món default status');
    IF @DefaultRecipeStatus = 'PUBLISHED'
        PRINT '  [PASS] TC26b: Verified default recipe status = PUBLISHED.';
    ELSE
        PRINT '  [FAIL] TC26b: Default recipe status expected PUBLISHED, found: ' + ISNULL(@DefaultRecipeStatus, 'NULL');

    -- ------------------------------------------------------------------------
    -- TC27: RECIPE_MEDIA Display Order 1..5 Range (CK_RECIPE_MEDIA_display_order)
    -- ------------------------------------------------------------------------
    -- Negative 1: display_order = 0
    BEGIN TRY
        INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order)
        VALUES (@RecipeId, 'https://storage.blob.core.windows.net/recipes/order0.jpg', 'image/jpeg', 0);
        PRINT '  [FAIL] TC27a: display_order = 0 was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_RECIPE_MEDIA_display_order%'
            PRINT '  [PASS] TC27a: Rejected display_order = 0 (exact CK_RECIPE_MEDIA_display_order, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC27a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Negative 2: display_order = 6
    BEGIN TRY
        INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order)
        VALUES (@RecipeId, 'https://storage.blob.core.windows.net/recipes/order6.jpg', 'image/jpeg', 6);
        PRINT '  [FAIL] TC27b: display_order = 6 was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_RECIPE_MEDIA_display_order%'
            PRINT '  [PASS] TC27b: Rejected display_order = 6 (exact CK_RECIPE_MEDIA_display_order, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC27b: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC28: RECIPE_MEDIA Unique Display Order per Recipe (UQ_RECIPE_MEDIA_order)
    -- ------------------------------------------------------------------------
    INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order)
    VALUES (@RecipeId, 'https://storage.blob.core.windows.net/recipes/order3.jpg', 'image/jpeg', 3);

    BEGIN TRY
        INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order)
        VALUES (@RecipeId, 'https://storage.blob.core.windows.net/recipes/order3_dup.jpg', 'image/jpeg', 3);
        PRINT '  [FAIL] TC28: Duplicate display_order for same recipe was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%UQ_RECIPE_MEDIA_order%'
            PRINT '  [PASS] TC28: Rejected duplicate display_order (exact UQ_RECIPE_MEDIA_order, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC28: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC29: RECIPE_INGREDIENT & SHOPPING_LIST_ITEM Target and Blank Checks
    -- ------------------------------------------------------------------------
    -- Negative 1: RECIPE_INGREDIENT both NULL
    BEGIN TRY
        INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, custom_ingredient_name, unit_id, quantity)
        VALUES (@RecipeId, NULL, NULL, @UnitGramId, 100.0);
        PRINT '  [FAIL] TC29a: RECIPE_INGREDIENT both targets NULL was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_RECIPE_INGREDIENT_target%'
            PRINT '  [PASS] TC29a: Rejected RECIPE_INGREDIENT both targets NULL (exact CK_RECIPE_INGREDIENT_target, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC29a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Negative 2: RECIPE_INGREDIENT whitespace custom name
    BEGIN TRY
        INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, custom_ingredient_name, unit_id, quantity)
        VALUES (@RecipeId, NULL, N'   ', @UnitGramId, 100.0);
        PRINT '  [FAIL] TC29b: RECIPE_INGREDIENT whitespace custom name was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_RECIPE_INGREDIENT_custom_name_not_blank%'
            PRINT '  [PASS] TC29b: Rejected RECIPE_INGREDIENT whitespace name (exact CK_RECIPE_INGREDIENT_custom_name_not_blank, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC29b: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Setup Shopping List fixture
    INSERT INTO [SHOPPING_LIST] (user_id, name) VALUES (@AliceId, N'Đi chợ tuần này');
    DECLARE @ShopListId BIGINT = SCOPE_IDENTITY();

    -- Negative 3: SHOPPING_LIST_ITEM both NULL
    BEGIN TRY
        INSERT INTO [SHOPPING_LIST_ITEM] (shopping_list_id, ingredient_id, custom_ingredient_name, unit_id, quantity)
        VALUES (@ShopListId, NULL, NULL, @UnitGramId, 200.0);
        PRINT '  [FAIL] TC29c: SHOPPING_LIST_ITEM both targets NULL was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_SLI_target%'
            PRINT '  [PASS] TC29c: Rejected SHOPPING_LIST_ITEM both targets NULL (exact CK_SLI_target, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC29c: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Negative 4: SHOPPING_LIST_ITEM whitespace custom name
    BEGIN TRY
        INSERT INTO [SHOPPING_LIST_ITEM] (shopping_list_id, ingredient_id, custom_ingredient_name, unit_id, quantity)
        VALUES (@ShopListId, NULL, N'   ', @UnitGramId, 200.0);
        PRINT '  [FAIL] TC29d: SHOPPING_LIST_ITEM whitespace custom name was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_SLI_custom_name_not_blank%'
            PRINT '  [PASS] TC29d: Rejected SHOPPING_LIST_ITEM whitespace name (exact CK_SLI_custom_name_not_blank, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC29d: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC30: COMMENT Composite FK & Depth Step Integrity (FK_COMMENT_PARENT, CK_COMMENT_root_reply)
    -- ------------------------------------------------------------------------
    -- Setup Root Comment
    INSERT INTO [COMMENT] (recipe_id, user_id, parent_comment_id, parent_depth, content, depth)
    VALUES (@RecipeId, @AliceId, NULL, NULL, N'Bình luận gốc cấp 1', 1);
    DECLARE @RootCommentId BIGINT = SCOPE_IDENTITY();

    -- Setup Valid Reply (depth = 2, parent_depth = 1)
    INSERT INTO [COMMENT] (recipe_id, user_id, parent_comment_id, parent_depth, content, depth)
    VALUES (@RecipeId, @BobId, @RootCommentId, 1, N'Phản hồi cấp 2 hợp lệ', 2);
    DECLARE @Reply1Id BIGINT = SCOPE_IDENTITY();
    PRINT '  [PASS] TC30a: Accepted valid nested comment (depth 1 -> depth 2).';

    -- Negative 1: Cross-recipe reply (reply points to RootCommentId on RecipeId, but claims recipe_id = TempRecipeId)
    BEGIN TRY
        INSERT INTO [COMMENT] (recipe_id, user_id, parent_comment_id, parent_depth, content, depth)
        VALUES (@TempRecipeId, @AliceId, @RootCommentId, 1, N'Phản hồi chéo công thức', 2);
        PRINT '  [FAIL] TC30b: Cross-recipe reply was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%FK_COMMENT_PARENT%'
            PRINT '  [PASS] TC30b: Rejected cross-recipe reply (exact composite FK_COMMENT_PARENT, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC30b: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Negative 2: Depth jump (parent_depth = 1, but depth = 3 instead of 2)
    BEGIN TRY
        INSERT INTO [COMMENT] (recipe_id, user_id, parent_comment_id, parent_depth, content, depth)
        VALUES (@RecipeId, @AliceId, @RootCommentId, 1, N'Nhảy cóc depth', 3);
        PRINT '  [FAIL] TC30c: Depth jump was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_COMMENT_root_reply%'
            PRINT '  [PASS] TC30c: Rejected depth jump (exact CK_COMMENT_root_reply, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC30c: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Negative 3: Root comment with depth > 1
    BEGIN TRY
        INSERT INTO [COMMENT] (recipe_id, user_id, parent_comment_id, parent_depth, content, depth)
        VALUES (@RecipeId, @AliceId, NULL, NULL, N'Root sai depth', 2);
        PRINT '  [FAIL] TC30d: Root comment with depth > 1 was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_COMMENT_root_reply%'
            PRINT '  [PASS] TC30d: Rejected root comment with depth > 1 (exact CK_COMMENT_root_reply, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC30d: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC31: MEAL_PLAN Monday Week Start Check (CK_MEAL_PLAN_week_start_monday)
    -- ------------------------------------------------------------------------
    -- Negative: Tuesday 2026-09-29
    BEGIN TRY
        INSERT INTO [MEAL_PLAN] (user_id, week_start_date)
        VALUES (@BobId, '2026-09-29');
        PRINT '  [FAIL] TC31: Non-Monday week_start_date was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_MEAL_PLAN_week_start_monday%'
            PRINT '  [PASS] TC31: Rejected non-Monday week_start_date (exact CK_MEAL_PLAN_week_start_monday, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC31: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC32: MEAL_PLAN Unique Plan per User per Week (UQ_MEAL_PLAN_user_week)
    -- ------------------------------------------------------------------------
    -- Bob creates Monday 2026-10-05 plan -> SUCCESS
    INSERT INTO [MEAL_PLAN] (user_id, week_start_date)
    VALUES (@BobId, '2026-10-05');
    DECLARE @BobPlanId BIGINT = SCOPE_IDENTITY();

    -- Negative: Bob tries to create 2nd plan for same Monday 2026-10-05
    BEGIN TRY
        INSERT INTO [MEAL_PLAN] (user_id, week_start_date)
        VALUES (@BobId, '2026-10-05');
        PRINT '  [FAIL] TC32: Duplicate meal plan for same user and week was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%UQ_MEAL_PLAN_user_week%'
            PRINT '  [PASS] TC32: Rejected duplicate meal plan in same week (exact UQ_MEAL_PLAN_user_week, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC32: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- ------------------------------------------------------------------------
    -- TC33: MEAL_PLAN_ENTRY Composite FK (FK_MPE_MEAL_PLAN)
    -- meal_date must fall within the Monday–Sunday week of the parent meal plan.
    -- BobPlanId is week 2026-10-05 (Mon) to 2026-10-11 (Sun).
    -- ------------------------------------------------------------------------
    -- Negative: meal_date = 2026-10-12 (next Monday)
    BEGIN TRY
        INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id, recipe_id, meal_date, meal_type, planned_servings)
        VALUES (@BobPlanId, @RecipeId, '2026-10-12', 'BREAKFAST', 2.0);
        PRINT '  [FAIL] TC33a: meal_date outside parent plan week was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%FK_MPE_MEAL_PLAN%'
            PRINT '  [PASS] TC33a: Rejected meal_date outside plan week (exact composite FK_MPE_MEAL_PLAN, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC33a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: Sunday 2026-10-11 (last day of the week)
    INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id, recipe_id, meal_date, meal_type, planned_servings)
    VALUES (@BobPlanId, @RecipeId, '2026-10-11', 'DINNER', 2.0);
    PRINT '  [PASS] TC33b: Accepted meal_date on Sunday within the same week.';

    -- ------------------------------------------------------------------------
    -- TC34: SUBSCRIPTION Tier & Period Constraints (CK_SUBSCRIPTION_tier, CK_SUBSCRIPTION_period)
    -- ------------------------------------------------------------------------
    -- Negative 1: Dropped tier FREE
    BEGIN TRY
        INSERT INTO [SUBSCRIPTION] (user_id, tier, status, starts_at, ends_at)
        VALUES (@AliceId, 'FREE', 'ACTIVE', '2026-10-01 00:00:00', '2026-11-01 00:00:00');
        PRINT '  [FAIL] TC34a: Dropped tier FREE was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_SUBSCRIPTION_tier%'
            PRINT '  [PASS] TC34a: Rejected dropped tier FREE (exact CK_SUBSCRIPTION_tier, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC34a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Negative 2: ends_at <= starts_at
    BEGIN TRY
        INSERT INTO [SUBSCRIPTION] (user_id, tier, status, starts_at, ends_at)
        VALUES (@AliceId, 'PLUS', 'ACTIVE', '2026-10-01 00:00:00', '2026-10-01 00:00:00');
        PRINT '  [FAIL] TC34b: ends_at <= starts_at was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_SUBSCRIPTION_period%'
            PRINT '  [PASS] TC34b: Rejected ends_at <= starts_at (exact CK_SUBSCRIPTION_period, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC34b: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: Valid PLUS subscription
    INSERT INTO [SUBSCRIPTION] (user_id, tier, status, starts_at, ends_at)
    VALUES (@AliceId, 'PLUS', 'ACTIVE', '2026-10-01 00:00:00', '2026-11-01 00:00:00');
    PRINT '  [PASS] TC34c: Accepted valid PLUS subscription with ends_at > starts_at.';

    -- ------------------------------------------------------------------------
    -- TC35: Single ACTIVE Subscription per User (UQ_SUBSCRIPTION_active)
    -- Filtered unique index: WHERE status = 'ACTIVE'
    -- ------------------------------------------------------------------------
    -- Negative: 2nd ACTIVE subscription for Alice -> Must FAIL
    BEGIN TRY
        INSERT INTO [SUBSCRIPTION] (user_id, tier, status, starts_at, ends_at)
        VALUES (@AliceId, 'PRO', 'ACTIVE', '2026-11-01 00:00:00', '2026-12-01 00:00:00');
        PRINT '  [FAIL] TC35a: 2nd ACTIVE subscription for same user was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%UQ_SUBSCRIPTION_active%'
            PRINT '  [PASS] TC35a: Rejected 2nd ACTIVE subscription (exact UQ_SUBSCRIPTION_active, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC35a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: Can have EXPIRED subscription alongside ACTIVE
    INSERT INTO [SUBSCRIPTION] (user_id, tier, status, starts_at, ends_at)
    VALUES (@AliceId, 'PLUS', 'EXPIRED', '2026-09-01 00:00:00', '2026-10-01 00:00:00');
    PRINT '  [PASS] TC35b: Accepted EXPIRED subscription alongside ACTIVE.';

    -- ------------------------------------------------------------------------
    -- TC36: COMMENT Content Length Constraint (CK_COMMENT_content_len, 1..1000 chars)
    -- ------------------------------------------------------------------------
    -- Negative: Empty content (length = 0) -> Must FAIL
    BEGIN TRY
        INSERT INTO [COMMENT] (recipe_id, user_id, parent_comment_id, parent_depth, content, depth)
        VALUES (@RecipeId, @AliceId, NULL, NULL, '', 1);
        PRINT '  [FAIL] TC36a: Empty comment content was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_COMMENT_content_len%'
            PRINT '  [PASS] TC36a: Rejected empty comment content (exact CK_COMMENT_content_len, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC36a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: Valid comment within 1..1000 characters
    INSERT INTO [COMMENT] (recipe_id, user_id, parent_comment_id, parent_depth, content, depth)
    VALUES (@RecipeId, @AliceId, NULL, NULL, N'Bình luận hợp lệ thỏa mãn 1 đến 1000 ký tự.', 1);
    PRINT '  [PASS] TC36b: Accepted valid comment content (1..1000 chars).';

    -- ------------------------------------------------------------------------
    -- TC37: PAYMENT_TRANSACTION Positive Amount Constraint (CK_PAYMENT_amount, amount_vnd > 0)
    -- ------------------------------------------------------------------------
    -- Negative: amount_vnd = 0 (FREE tier does not generate transaction, Q9) -> Must FAIL
    BEGIN TRY
        INSERT INTO [PAYMENT_TRANSACTION] (user_id, order_code, amount_vnd, status)
        VALUES (@AliceId, 'PAYOS_ZERO_AMOUNT_TEST', 0, 'PENDING');
        PRINT '  [FAIL] TC37a: Zero payment amount was not rejected!';
    END TRY
    BEGIN CATCH
        IF ERROR_MESSAGE() LIKE '%CK_PAYMENT_amount%'
            PRINT '  [PASS] TC37a: Rejected zero payment amount (exact CK_PAYMENT_amount, Error ' + CAST(ERROR_NUMBER() AS VARCHAR) + ')';
        ELSE
            PRINT '  [FAIL] TC37a: Caught unexpected error: ' + ERROR_MESSAGE();
    END CATCH;

    -- Positive: Valid positive amount (99,000 VND for PRO)
    INSERT INTO [PAYMENT_TRANSACTION] (user_id, order_code, amount_vnd, status)
    VALUES (@AliceId, 'PAYOS_VALID_PRO_99K', 99000, 'PENDING');
    PRINT '  [PASS] TC37b: Accepted valid payment amount (99,000 VND > 0).';

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
