-- ============================================================================
-- Database Schema Snapshot: schema.sql
-- Database Engine : Microsoft SQL Server 2019+
-- Project         : Mâm Xanh — Vegetarian Support System (SWP391)
-- Issue           : Refs #63 — Pha 2 Physical ERD & Schema
-- Author          : Trương Văn Khải
-- Date            : 2026-09-25
-- Source          : Logical ERD v1.0.0 + Data Dictionary v0.6.0
--                   (Nguyễn Hải Dương — Pha 1, commit 827353e)
-- Synchronized with: V1__baseline_schema.sql (Flyway baseline)
-- ============================================================================
-- This file is the manual bootstrap / schema snapshot for local development,
-- testing on clean databases, or SSMS / Azure Data Studio / sqlcmd execution.
--
-- Authority rule (database/README.md):
--   Flyway migration (app/mamxanh-backend/src/main/resources/db/migration/)
--   is the authoritative schema migration history.
--   This file is a deliberate snapshot and must stay synchronized with Flyway.
--
-- Quick Start (SSMS / Azure Data Studio / sqlcmd):
--   1. Create database if it does not already exist:
--        CREATE DATABASE [MamXanhDB];
--        GO
--   2. Switch to the database context:
--        USE [MamXanhDB];
--        GO
--   3. Execute this script.
--
-- CASCADE POLICY (SQL Server error 1785 — multiple cascade paths):
--   CASCADE only on true ownership: RECIPE_POST → MEDIA/INGREDIENT,
--   MEAL_PLAN → ENTRY, SHOPPING_LIST → ITEM.
--   SET NULL on RECIPE_VIEW.user_id (Guest support).
--   NO ACTION on everything else — application layer handles deletion.
-- ============================================================================

SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO


-- ============================================================================
-- SECTION 1: Reference / Dictionary Tables
-- ============================================================================

-- 1.1 UNIT — Measurement unit dictionary (MASS / VOLUME / COUNT)
-- Source: Data Dictionary 4.19, FR-18, BR-73
CREATE TABLE [UNIT] (
    unit_id       INT            IDENTITY(1,1)  NOT NULL,
    code          VARCHAR(20)    NOT NULL,
    name          NVARCHAR(50)   NOT NULL,
    dimension     VARCHAR(10)    NOT NULL,
    base_factor   DECIMAL(18,6)  NOT NULL,
    is_active     BIT            NOT NULL
        CONSTRAINT DF_UNIT_is_active DEFAULT 1,

    CONSTRAINT PK_UNIT PRIMARY KEY (unit_id),
    CONSTRAINT UQ_UNIT_code UNIQUE (code),
    CONSTRAINT CK_UNIT_dimension CHECK (dimension IN ('MASS', 'VOLUME', 'COUNT'))
);
GO

-- 1.2 INGREDIENT — Standard ingredient dictionary with 9 nutrition indicators
-- Source: Data Dictionary 4.18, FR-39/41, BR-44/49/51/52/53
-- Nutrition indicators are NULLable: NULL = no data yet; 0 = true measured zero.
-- nutrition_supported = 1 requires all 9 indicators and source_url NOT NULL (Q5, BR-52).
CREATE TABLE [INGREDIENT] (
    ingredient_id         BIGINT         IDENTITY(1,1)  NOT NULL,
    name                  NVARCHAR(200)  NOT NULL,
    energy_kcal_100g      DECIMAL(10,2)  NULL,
    protein_g_100g        DECIMAL(10,2)  NULL,
    carbohydrate_g_100g   DECIMAL(10,2)  NULL,
    total_fat_g_100g      DECIMAL(10,2)  NULL,
    fiber_g_100g          DECIMAL(10,2)  NULL,
    calcium_mg_100g       DECIMAL(10,2)  NULL,
    iron_mg_100g          DECIMAL(10,2)  NULL,
    vitamin_b12_mcg_100g  DECIMAL(10,4)  NULL,
    zinc_mg_100g          DECIMAL(10,2)  NULL,
    source_name           NVARCHAR(200)  NOT NULL,
    source_url            VARCHAR(2048)  NULL,
    reference_date        DATE           NOT NULL,
    nutrition_supported   BIT            NOT NULL
        CONSTRAINT DF_INGREDIENT_nutrition_supported DEFAULT 0,
    status                VARCHAR(20)    NOT NULL
        CONSTRAINT DF_INGREDIENT_status DEFAULT 'ACTIVE',
    created_at            DATETIME2(7)   NOT NULL
        CONSTRAINT DF_INGREDIENT_created_at DEFAULT SYSUTCDATETIME(),
    updated_at            DATETIME2(7)   NOT NULL
        CONSTRAINT DF_INGREDIENT_updated_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_INGREDIENT PRIMARY KEY (ingredient_id),
    CONSTRAINT UQ_INGREDIENT_name UNIQUE (name),
    CONSTRAINT CK_INGREDIENT_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT CK_INGREDIENT_nutrition_supported CHECK (
        nutrition_supported = 0 OR (
            energy_kcal_100g IS NOT NULL
            AND protein_g_100g IS NOT NULL
            AND carbohydrate_g_100g IS NOT NULL
            AND total_fat_g_100g IS NOT NULL
            AND fiber_g_100g IS NOT NULL
            AND calcium_mg_100g IS NOT NULL
            AND iron_mg_100g IS NOT NULL
            AND vitamin_b12_mcg_100g IS NOT NULL
            AND zinc_mg_100g IS NOT NULL
            AND source_url IS NOT NULL
        )
    )
);
GO


-- ============================================================================
-- SECTION 2: Core User Table
-- ============================================================================

-- 2.1 USER — Account, role, profile, health metrics, onboarding (merged User Profile)
-- Source: Data Dictionary 4.1, FR-03/23/31/35/38/49, BR-17/18/24/26/30/39/42, Q7, Q12
-- NOTE: [USER] requires brackets — reserved word in SQL Server.
CREATE TABLE [USER] (
    user_id                    BIGINT         IDENTITY(1,1)  NOT NULL,
    email                      VARCHAR(255)   NOT NULL,
    password_hash              VARCHAR(255)   NULL,
    google_subject             VARCHAR(255)   NULL,
    display_name               NVARCHAR(100)  NOT NULL,
    avatar_url                 VARCHAR(2048)  NULL,
    bio                        NVARCHAR(500)  NULL,
    role                       VARCHAR(20)    NOT NULL
        CONSTRAINT DF_USER_role DEFAULT 'CUSTOMER',
    account_status             VARCHAR(20)    NOT NULL
        CONSTRAINT DF_USER_account_status DEFAULT 'ACTIVE',
    email_verified             BIT            NOT NULL
        CONSTRAINT DF_USER_email_verified DEFAULT 0,
    vegetarian_type            VARCHAR(20)    NULL,
    cuisine_preference         NVARCHAR(200)  NULL,
    preferred_difficulty       VARCHAR(20)    NULL,
    max_cooking_time_min       INT            NULL,
    biological_sex             VARCHAR(10)    NULL,
    height_cm                  DECIMAL(5,1)   NULL,
    weight_kg                  DECIMAL(5,1)   NULL,
    activity_level             VARCHAR(30)    NULL,
    pregnant                   BIT            NOT NULL
        CONSTRAINT DF_USER_pregnant DEFAULT 0,
    breastfeeding              BIT            NOT NULL
        CONSTRAINT DF_USER_breastfeeding DEFAULT 0,
    therapeutic_diet_required  BIT            NOT NULL
        CONSTRAINT DF_USER_therapeutic DEFAULT 0,
    nutrition_scope_confirmed  BIT            NOT NULL
        CONSTRAINT DF_USER_nutrition_scope DEFAULT 0,
    reply_email_enabled        BIT            NOT NULL
        CONSTRAINT DF_USER_reply_email DEFAULT 1,
    date_of_birth              DATE           NULL,
    nutrition_goal             VARCHAR(20)    NULL,
    onboarding_status          VARCHAR(20)    NOT NULL
        CONSTRAINT DF_USER_onboarding_status DEFAULT 'NOT_STARTED',
    avoid_none_confirmed       BIT            NOT NULL
        CONSTRAINT DF_USER_avoid_none_confirmed DEFAULT 0,
    dislike_none_confirmed     BIT            NOT NULL
        CONSTRAINT DF_USER_dislike_none_confirmed DEFAULT 0,
    created_at                 DATETIME2(7)   NOT NULL
        CONSTRAINT DF_USER_created_at DEFAULT SYSUTCDATETIME(),
    updated_at                 DATETIME2(7)   NOT NULL
        CONSTRAINT DF_USER_updated_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_USER PRIMARY KEY (user_id),
    CONSTRAINT UQ_USER_email UNIQUE (email),
    CONSTRAINT CK_USER_role CHECK (role IN ('CUSTOMER', 'EXPERT', 'ADMIN')),
    CONSTRAINT CK_USER_account_status CHECK (account_status IN ('ACTIVE', 'LOCKED')),
    CONSTRAINT CK_USER_vegetarian_type CHECK (
        vegetarian_type IN ('VEGAN', 'LACTO', 'OVO', 'LACTO_OVO')
    ),
    CONSTRAINT CK_USER_preferred_difficulty CHECK (
        preferred_difficulty IN ('EASY', 'MEDIUM', 'HARD')
    ),
    CONSTRAINT CK_USER_biological_sex CHECK (biological_sex IN ('MALE', 'FEMALE')),
    CONSTRAINT CK_USER_height_cm CHECK (height_cm BETWEEN 100.0 AND 250.0),
    CONSTRAINT CK_USER_weight_kg CHECK (weight_kg BETWEEN 30.0 AND 300.0),
    CONSTRAINT CK_USER_activity_level CHECK (
        activity_level IN ('SEDENTARY', 'LIGHTLY_ACTIVE', 'MODERATELY_ACTIVE', 'VERY_ACTIVE')
    ),
    CONSTRAINT CK_USER_date_of_birth CHECK (
        date_of_birth >= '1900-01-01' AND date_of_birth <= CAST(GETDATE() AS DATE)
    ),
    CONSTRAINT CK_USER_nutrition_goal CHECK (
        nutrition_goal IN ('MAINTAIN_WEIGHT', 'IMPROVE_HEALTH', 'SUPPORT_TRAINING')
    ),
    CONSTRAINT CK_USER_onboarding_status CHECK (
        onboarding_status IN ('NOT_STARTED', 'SKIPPED', 'COMPLETED')
    )
);
GO

-- google_subject: allow multiple NULLs (standard UNIQUE only allows one NULL)
CREATE UNIQUE NONCLUSTERED INDEX UQ_USER_google_subject
    ON [USER](google_subject)
    WHERE google_subject IS NOT NULL;
GO


-- ============================================================================
-- SECTION 3: User Relationship Tables
-- ============================================================================

-- 3.1 USER_FOLLOW — Directed follow relationship between Members
-- Source: Data Dictionary 4.2, FR-59, BR-75
-- Composite PK prevents duplicate pairs; CHECK prevents self-follow.
-- Both FKs → USER: NO ACTION (error 1785)
CREATE TABLE [USER_FOLLOW] (
    follower_user_id  BIGINT        NOT NULL,
    followed_user_id  BIGINT        NOT NULL,
    created_at        DATETIME2(7)  NOT NULL
        CONSTRAINT DF_USER_FOLLOW_created_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_USER_FOLLOW PRIMARY KEY (follower_user_id, followed_user_id),
    CONSTRAINT FK_USER_FOLLOW_follower FOREIGN KEY (follower_user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_USER_FOLLOW_followed FOREIGN KEY (followed_user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_USER_FOLLOW_no_self CHECK (follower_user_id <> followed_user_id)
);
GO

-- 3.2 USER_INGREDIENT_PREFERENCE — Avoid / Dislike declarations (Q11, FR-31, BR-13/30)
-- preference_type: 'AVOID' (gộp ALLERGY) or 'DISLIKE'.
-- Must declare standard ingredient_id OR custom_ingredient_name (or both).
-- Custom name must not be blank or whitespace-only.
-- Filtered unique indexes enforce max 1 preference per ingredient per user (Q11b).
CREATE TABLE [USER_INGREDIENT_PREFERENCE] (
    preference_id           BIGINT         IDENTITY(1,1)  NOT NULL,
    user_id                 BIGINT         NOT NULL,
    ingredient_id           BIGINT         NULL,
    custom_ingredient_name  NVARCHAR(200)  NULL,
    preference_type         VARCHAR(20)    NOT NULL,
    created_at              DATETIME2(7)   NOT NULL
        CONSTRAINT DF_UIP_created_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_USER_INGREDIENT_PREFERENCE PRIMARY KEY (preference_id),
    CONSTRAINT FK_UIP_USER FOREIGN KEY (user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_UIP_INGREDIENT FOREIGN KEY (ingredient_id)
        REFERENCES [INGREDIENT](ingredient_id) ON DELETE NO ACTION,
    CONSTRAINT CK_UIP_preference_type CHECK (
        preference_type IN ('AVOID', 'DISLIKE')
    ),
    CONSTRAINT CK_UIP_target CHECK (
        ingredient_id IS NOT NULL OR custom_ingredient_name IS NOT NULL
    ),
    CONSTRAINT CK_UIP_custom_name_not_blank CHECK (
        custom_ingredient_name IS NULL
        OR LEN(TRIM(NCHAR(9)+NCHAR(10)+NCHAR(13)+NCHAR(32)+NCHAR(160) FROM custom_ingredient_name)) > 0
    )
);
GO

-- 3.3 EXPERT_APPLICATION — Expert role application by Customer
-- Source: Data Dictionary 4.4, FR-05, BR-21/22/26
-- vegetarian_type has 5 values (adds MACROBIOTIC for "Chay thực dưỡng dưỡng sinh")
-- Both user_id and reviewed_by → USER: NO ACTION (error 1785)
CREATE TABLE [EXPERT_APPLICATION] (
    application_id         BIGINT          IDENTITY(1,1)  NOT NULL,
    user_id                BIGINT          NOT NULL,
    bio_experience         NVARCHAR(MAX)   NOT NULL,
    vegetarian_type        VARCHAR(30)     NOT NULL,
    sample_recipe_summary  NVARCHAR(MAX)   NOT NULL,
    portfolio_url          VARCHAR(2048)   NULL,
    status                 VARCHAR(20)     NOT NULL
        CONSTRAINT DF_EXPERT_APP_status DEFAULT 'PENDING',
    admin_note             NVARCHAR(1000)  NULL,
    reviewed_by            BIGINT          NULL,
    reviewed_at            DATETIME2(7)    NULL,
    created_at             DATETIME2(7)    NOT NULL
        CONSTRAINT DF_EXPERT_APP_created_at DEFAULT SYSUTCDATETIME(),
    updated_at             DATETIME2(7)    NOT NULL
        CONSTRAINT DF_EXPERT_APP_updated_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_EXPERT_APPLICATION PRIMARY KEY (application_id),
    CONSTRAINT FK_EXPERT_APP_USER FOREIGN KEY (user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_EXPERT_APP_REVIEWER FOREIGN KEY (reviewed_by)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_EXPERT_APP_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED')
    ),
    CONSTRAINT CK_EXPERT_APP_vegetarian_type CHECK (
        vegetarian_type IN ('VEGAN', 'LACTO', 'OVO', 'LACTO_OVO', 'MACROBIOTIC')
    )
);
GO


-- ============================================================================
-- SECTION 4: Recipe Core Tables
-- ============================================================================

-- 4.1 RECIPE_POST — Recipe article authored by Expert
-- Source: Data Dictionary 4.5, FR-04/07/08/16/17/25/28/44/57/58, BR-07/10/19/20/27/43/62/69-72
-- Default status: 'PUBLISHED' (no server-side drafts; FE holds unposted state).
-- Status values: 'PUBLISHED', 'HIDDEN', 'DELETED' (tombstone soft-delete).
-- instructions: 10..5000 chars (CK_RECIPE_POST_instructions_len).
-- total time: prep + cook > 0 (CK_RECIPE_POST_total_time).
CREATE TABLE [RECIPE_POST] (
    recipe_id       BIGINT          IDENTITY(1,1)  NOT NULL,
    author_id       BIGINT          NOT NULL,
    title           NVARCHAR(120)   NOT NULL,
    description     NVARCHAR(2000)  NULL,
    instructions    NVARCHAR(MAX)   NOT NULL,
    dish_category   VARCHAR(20)     NOT NULL,
    vegetarian_type VARCHAR(20)     NOT NULL,
    difficulty      VARCHAR(10)     NOT NULL,
    servings        INT             NOT NULL,
    prep_time_min   INT             NOT NULL,
    cook_time_min   INT             NOT NULL,
    youtube_url     VARCHAR(2048)   NULL,
    status          VARCHAR(20)     NOT NULL
        CONSTRAINT DF_RECIPE_POST_status DEFAULT 'PUBLISHED',
    published_at    DATETIME2(7)    NULL,
    created_at      DATETIME2(7)    NOT NULL
        CONSTRAINT DF_RECIPE_POST_created_at DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(7)    NOT NULL
        CONSTRAINT DF_RECIPE_POST_updated_at DEFAULT SYSUTCDATETIME(),
    like_count      INT             NOT NULL
        CONSTRAINT DF_RECIPE_POST_like_count DEFAULT 0,
    dislike_count   INT             NOT NULL
        CONSTRAINT DF_RECIPE_POST_dislike_count DEFAULT 0,
    view_count      INT             NOT NULL
        CONSTRAINT DF_RECIPE_POST_view_count DEFAULT 0,

    CONSTRAINT PK_RECIPE_POST PRIMARY KEY (recipe_id),
    CONSTRAINT FK_RECIPE_POST_USER FOREIGN KEY (author_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_RECIPE_POST_title_len CHECK (LEN(title) >= 3),
    CONSTRAINT CK_RECIPE_POST_instructions_len CHECK (LEN(instructions) BETWEEN 10 AND 5000),
    CONSTRAINT CK_RECIPE_POST_dish_category CHECK (
        dish_category IN ('NOODLE_SOUP', 'STIR_FRY', 'HOT_POT', 'BRAISED',
                          'SOUP', 'FRIED', 'STEAMED', 'SALAD', 'ROLL',
                          'GRILLED', 'DESSERT')
    ),
    CONSTRAINT CK_RECIPE_POST_vegetarian_type CHECK (
        vegetarian_type IN ('VEGAN', 'LACTO', 'OVO', 'LACTO_OVO')
    ),
    CONSTRAINT CK_RECIPE_POST_difficulty CHECK (
        difficulty IN ('EASY', 'MEDIUM', 'HARD')
    ),
    CONSTRAINT CK_RECIPE_POST_servings CHECK (servings BETWEEN 1 AND 50),
    CONSTRAINT CK_RECIPE_POST_prep_time CHECK (prep_time_min BETWEEN 0 AND 1440),
    CONSTRAINT CK_RECIPE_POST_cook_time CHECK (cook_time_min BETWEEN 0 AND 1440),
    CONSTRAINT CK_RECIPE_POST_total_time CHECK (prep_time_min + cook_time_min > 0),
    CONSTRAINT CK_RECIPE_POST_status CHECK (
        status IN ('PUBLISHED', 'HIDDEN', 'DELETED')
    ),
    CONSTRAINT CK_RECIPE_POST_like_count CHECK (like_count >= 0),
    CONSTRAINT CK_RECIPE_POST_dislike_count CHECK (dislike_count >= 0),
    CONSTRAINT CK_RECIPE_POST_view_count CHECK (view_count >= 0)
);
GO

-- 4.2 RECIPE_MEDIA — 0..5 images on Azure Blob Storage per recipe
-- Source: Data Dictionary 4.6, FR-14/17, BR-11/20
-- UQ_RECIPE_MEDIA_order + display_order BETWEEN 1 AND 5 limits to at most 5 images.
-- CASCADE: media belongs to recipe — delete recipe deletes all its media.
CREATE TABLE [RECIPE_MEDIA] (
    media_id       BIGINT         IDENTITY(1,1)  NOT NULL,
    recipe_id      BIGINT         NOT NULL,
    blob_url       VARCHAR(2048)  NOT NULL,
    mime_type      VARCHAR(20)    NOT NULL,
    display_order  INT            NOT NULL,
    is_cover       BIT            NOT NULL
        CONSTRAINT DF_RECIPE_MEDIA_is_cover DEFAULT 0,

    CONSTRAINT PK_RECIPE_MEDIA PRIMARY KEY (media_id),
    CONSTRAINT FK_RECIPE_MEDIA_RECIPE_POST FOREIGN KEY (recipe_id)
        REFERENCES [RECIPE_POST](recipe_id) ON DELETE CASCADE,
    CONSTRAINT UQ_RECIPE_MEDIA_order UNIQUE (recipe_id, display_order),
    CONSTRAINT CK_RECIPE_MEDIA_display_order CHECK (display_order BETWEEN 1 AND 5),
    CONSTRAINT CK_RECIPE_MEDIA_mime_type CHECK (
        mime_type IN ('image/jpeg', 'image/png', 'image/webp')
    )
);
GO

-- 4.3 RECIPE_INGREDIENT — Ingredient line with positive quantity and standard unit
-- Source: Data Dictionary 4.7, FR-16/19, BR-12/13/14/19/73
-- Must have ingredient_id OR custom_ingredient_name (or both).
-- Custom name must not be blank or whitespace-only.
-- CASCADE: ingredients belong to recipe — delete recipe deletes its ingredient list.
CREATE TABLE [RECIPE_INGREDIENT] (
    recipe_ingredient_id   BIGINT         IDENTITY(1,1)  NOT NULL,
    recipe_id              BIGINT         NOT NULL,
    ingredient_id          BIGINT         NULL,
    unit_id                INT            NOT NULL,
    custom_ingredient_name NVARCHAR(200)  NULL,
    quantity               DECIMAL(10,2)  NOT NULL,

    CONSTRAINT PK_RECIPE_INGREDIENT PRIMARY KEY (recipe_ingredient_id),
    CONSTRAINT FK_RECIPE_INGREDIENT_RECIPE FOREIGN KEY (recipe_id)
        REFERENCES [RECIPE_POST](recipe_id) ON DELETE CASCADE,
    CONSTRAINT FK_RECIPE_INGREDIENT_INGREDIENT FOREIGN KEY (ingredient_id)
        REFERENCES [INGREDIENT](ingredient_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RECIPE_INGREDIENT_UNIT FOREIGN KEY (unit_id)
        REFERENCES [UNIT](unit_id) ON DELETE NO ACTION,
    CONSTRAINT CK_RECIPE_INGREDIENT_quantity CHECK (quantity > 0),
    CONSTRAINT CK_RECIPE_INGREDIENT_target CHECK (
        ingredient_id IS NOT NULL OR custom_ingredient_name IS NOT NULL
    ),
    CONSTRAINT CK_RECIPE_INGREDIENT_custom_name_not_blank CHECK (
        custom_ingredient_name IS NULL
        OR LEN(TRIM(NCHAR(9)+NCHAR(10)+NCHAR(13)+NCHAR(32)+NCHAR(160) FROM custom_ingredient_name)) > 0
    )
);
GO


-- ============================================================================
-- SECTION 5: Interaction Tables
-- ============================================================================

-- 5.1 RECIPE_REACTION — Like / Dislike vote (1 per user per recipe)
-- Source: Data Dictionary 4.8, FR-57, BR-69
-- Composite PK enforces one vote per user per recipe.
-- "Author cannot self-vote" — enforced at service layer (constraint #7).
CREATE TABLE [RECIPE_REACTION] (
    user_id        BIGINT        NOT NULL,
    recipe_id      BIGINT        NOT NULL,
    reaction_type  VARCHAR(10)   NOT NULL,
    created_at     DATETIME2(7)  NOT NULL
        CONSTRAINT DF_RECIPE_REACTION_created_at DEFAULT SYSUTCDATETIME(),
    updated_at     DATETIME2(7)  NOT NULL
        CONSTRAINT DF_RECIPE_REACTION_updated_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_RECIPE_REACTION PRIMARY KEY (user_id, recipe_id),
    CONSTRAINT FK_RECIPE_REACTION_USER FOREIGN KEY (user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RECIPE_REACTION_RECIPE FOREIGN KEY (recipe_id)
        REFERENCES [RECIPE_POST](recipe_id) ON DELETE NO ACTION,
    CONSTRAINT CK_RECIPE_REACTION_type CHECK (
        reaction_type IN ('LIKE', 'DISLIKE')
    )
);
GO

-- 5.2 RECIPE_VIEW — View event for deduplication (30-min window) and statistics
-- Source: Data Dictionary 4.9, FR-58, BR-70
-- user_id SET NULL: when user is deleted, view record survives as anonymous
CREATE TABLE [RECIPE_VIEW] (
    view_id                BIGINT         IDENTITY(1,1)  NOT NULL,
    recipe_id              BIGINT         NOT NULL,
    user_id                BIGINT         NULL,
    anonymous_viewer_hash  VARCHAR(128)   NULL,
    viewed_at              DATETIME2(7)   NOT NULL
        CONSTRAINT DF_RECIPE_VIEW_viewed_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_RECIPE_VIEW PRIMARY KEY (view_id),
    CONSTRAINT FK_RECIPE_VIEW_RECIPE FOREIGN KEY (recipe_id)
        REFERENCES [RECIPE_POST](recipe_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RECIPE_VIEW_USER FOREIGN KEY (user_id)
        REFERENCES [USER](user_id) ON DELETE SET NULL
);
GO

-- 5.3 COMMENT — Nested comments (self-referencing, max depth 5, Q8, BR-66)
-- UQ_COMMENT_id_recipe_depth enables composite FK guaranteeing replies share
-- same recipe_id and parent_depth = parent.depth.
-- CK_COMMENT_root_reply guarantees root comments have depth=1 and child comments
-- have depth = parent_depth + 1.
CREATE TABLE [COMMENT] (
    comment_id         BIGINT          IDENTITY(1,1)  NOT NULL,
    recipe_id          BIGINT          NOT NULL,
    user_id            BIGINT          NOT NULL,
    parent_comment_id  BIGINT          NULL,
    parent_depth       INT             NULL,
    content            NVARCHAR(1000)  NOT NULL,
    depth              INT             NOT NULL
        CONSTRAINT DF_COMMENT_depth DEFAULT 1,
    is_deleted         BIT             NOT NULL
        CONSTRAINT DF_COMMENT_is_deleted DEFAULT 0,
    created_at         DATETIME2(7)    NOT NULL
        CONSTRAINT DF_COMMENT_created_at DEFAULT SYSUTCDATETIME(),
    updated_at         DATETIME2(7)    NOT NULL
        CONSTRAINT DF_COMMENT_updated_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_COMMENT PRIMARY KEY (comment_id),
    CONSTRAINT UQ_COMMENT_id_recipe_depth UNIQUE (comment_id, recipe_id, depth),
    CONSTRAINT FK_COMMENT_RECIPE FOREIGN KEY (recipe_id)
        REFERENCES [RECIPE_POST](recipe_id) ON DELETE NO ACTION,
    CONSTRAINT FK_COMMENT_USER FOREIGN KEY (user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_COMMENT_PARENT FOREIGN KEY (parent_comment_id, recipe_id, parent_depth)
        REFERENCES [COMMENT](comment_id, recipe_id, depth) ON DELETE NO ACTION,
    CONSTRAINT CK_COMMENT_content_len CHECK (LEN(content) BETWEEN 1 AND 1000),
    CONSTRAINT CK_COMMENT_depth CHECK (depth BETWEEN 1 AND 5),
    CONSTRAINT CK_COMMENT_root_reply CHECK (
        (parent_comment_id IS NULL AND parent_depth IS NULL AND depth = 1)
        OR (parent_comment_id IS NOT NULL AND parent_depth IS NOT NULL AND depth = parent_depth + 1)
    )
);
GO

-- 5.4 SAVED_RECIPE — Bookmark (composite PK = one save per user per recipe)
-- Source: Data Dictionary 4.13, FR-32, BR-32/33/34
CREATE TABLE [SAVED_RECIPE] (
    user_id    BIGINT        NOT NULL,
    recipe_id  BIGINT        NOT NULL,
    saved_at   DATETIME2(7)  NOT NULL
        CONSTRAINT DF_SAVED_RECIPE_saved_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_SAVED_RECIPE PRIMARY KEY (user_id, recipe_id),
    CONSTRAINT FK_SAVED_RECIPE_USER FOREIGN KEY (user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_SAVED_RECIPE_RECIPE FOREIGN KEY (recipe_id)
        REFERENCES [RECIPE_POST](recipe_id) ON DELETE NO ACTION
);
GO


-- ============================================================================
-- SECTION 6: Moderation & Notification
-- ============================================================================

-- 6.1 REPORT — Content violation report (merged Moderation Action)
-- Source: Data Dictionary 4.11, FR-26/27/28/48, BR-23/24/25/26/28/29
-- XOR CHECK: exactly one of (recipe_id, comment_id) must be NOT NULL
-- All FKs NO ACTION (error 1785: multiple paths via USER, RECIPE_POST, COMMENT)
CREATE TABLE [REPORT] (
    report_id        BIGINT          IDENTITY(1,1)  NOT NULL,
    reporter_id      BIGINT          NOT NULL,
    recipe_id        BIGINT          NULL,
    comment_id       BIGINT          NULL,
    reason_code      VARCHAR(30)     NOT NULL,
    description      NVARCHAR(500)   NULL,
    status           VARCHAR(20)     NOT NULL
        CONSTRAINT DF_REPORT_status DEFAULT 'PENDING',
    decision         VARCHAR(30)     NULL,
    decision_reason  NVARCHAR(1000)  NULL,
    created_at       DATETIME2(7)    NOT NULL
        CONSTRAINT DF_REPORT_created_at DEFAULT SYSUTCDATETIME(),
    updated_at       DATETIME2(7)    NOT NULL
        CONSTRAINT DF_REPORT_updated_at DEFAULT SYSUTCDATETIME(),
    handled_at       DATETIME2(7)    NULL,

    CONSTRAINT PK_REPORT PRIMARY KEY (report_id),
    CONSTRAINT FK_REPORT_USER FOREIGN KEY (reporter_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_REPORT_RECIPE FOREIGN KEY (recipe_id)
        REFERENCES [RECIPE_POST](recipe_id) ON DELETE NO ACTION,
    CONSTRAINT FK_REPORT_COMMENT FOREIGN KEY (comment_id)
        REFERENCES [COMMENT](comment_id) ON DELETE NO ACTION,
    CONSTRAINT CK_REPORT_target_xor CHECK (
        (recipe_id IS NOT NULL AND comment_id IS NULL)
        OR (recipe_id IS NULL AND comment_id IS NOT NULL)
    ),
    CONSTRAINT CK_REPORT_reason_code CHECK (
        reason_code IN ('NON_VEGAN', 'FOOD_SAFETY_HAZARD',
                        'INAPPROPRIATE_CONTENT', 'COPYRIGHT_VIOLATION',
                        'SPAM_ADVERTISING', 'OTHER')
    ),
    CONSTRAINT CK_REPORT_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'RESOLVED', 'REJECTED')
    )
);
GO

-- 6.2 NOTIFICATION — In-app notification to user
-- Source: Data Dictionary 4.12, FR-49
CREATE TABLE [NOTIFICATION] (
    notification_id    BIGINT          IDENTITY(1,1)  NOT NULL,
    user_id            BIGINT          NOT NULL,
    comment_id         BIGINT          NULL,
    report_id          BIGINT          NULL,
    notification_type  VARCHAR(30)     NOT NULL,
    title              NVARCHAR(200)   NOT NULL,
    message            NVARCHAR(1000)  NOT NULL,
    is_read            BIT             NOT NULL
        CONSTRAINT DF_NOTIFICATION_is_read DEFAULT 0,
    created_at         DATETIME2(7)    NOT NULL
        CONSTRAINT DF_NOTIFICATION_created_at DEFAULT SYSUTCDATETIME(),
    read_at            DATETIME2(7)    NULL,

    CONSTRAINT PK_NOTIFICATION PRIMARY KEY (user_id, notification_id),
    CONSTRAINT FK_NOTIFICATION_USER FOREIGN KEY (user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_NOTIFICATION_COMMENT FOREIGN KEY (comment_id)
        REFERENCES [COMMENT](comment_id) ON DELETE NO ACTION,
    CONSTRAINT FK_NOTIFICATION_REPORT FOREIGN KEY (report_id)
        REFERENCES [REPORT](report_id) ON DELETE NO ACTION
);
GO


-- ============================================================================
-- SECTION 7: Meal Planning & Shopping
-- ============================================================================

-- 7.1 MEAL_PLAN — Weekly meal plan (Mon–Sun, 3 meals/day, Q3, FR-09, BR-32/36)
-- week_start_date must be Monday (DATEDIFF % 7 = 0 from 1900-01-01, independent of DATEFIRST).
-- At most 1 meal plan per user per week (UQ_MEAL_PLAN_user_week).
-- UQ_MEAL_PLAN_id_week target of composite FK from MEAL_PLAN_ENTRY.
CREATE TABLE [MEAL_PLAN] (
    meal_plan_id     BIGINT        IDENTITY(1,1)  NOT NULL,
    user_id          BIGINT        NOT NULL,
    week_start_date  DATE          NOT NULL,
    created_at       DATETIME2(7)  NOT NULL
        CONSTRAINT DF_MEAL_PLAN_created_at DEFAULT SYSUTCDATETIME(),
    updated_at       DATETIME2(7)  NOT NULL
        CONSTRAINT DF_MEAL_PLAN_updated_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_MEAL_PLAN PRIMARY KEY (meal_plan_id),
    CONSTRAINT FK_MEAL_PLAN_USER FOREIGN KEY (user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT UQ_MEAL_PLAN_user_week UNIQUE (user_id, week_start_date),
    CONSTRAINT UQ_MEAL_PLAN_id_week UNIQUE (meal_plan_id, week_start_date),
    CONSTRAINT CK_MEAL_PLAN_week_start_monday CHECK (
        DATEDIFF(DAY, CONVERT(DATE, '19000101', 112), week_start_date) % 7 = 0
    )
);
GO

-- 7.2 MEAL_PLAN_ENTRY — Recipe assigned to a specific day and meal slot
-- Source: Data Dictionary 4.15, FR-33/37, BR-35/36/37/47, Q8
-- Computed column meal_week_start calculates the Monday for meal_date.
-- FK_MPE_MEAL_PLAN composite FK to (meal_plan_id, week_start_date) strictly enforces
-- meal_date must fall within the 7 days of the parent meal plan's week.
-- CASCADE: delete meal plan deletes all its entries.
CREATE TABLE [MEAL_PLAN_ENTRY] (
    meal_plan_entry_id  BIGINT        IDENTITY(1,1)  NOT NULL,
    meal_plan_id        BIGINT        NOT NULL,
    recipe_id           BIGINT        NOT NULL,
    meal_date           DATE          NOT NULL,
    meal_week_start     AS CONVERT(DATE, DATEADD(DAY, -(DATEDIFF(DAY, CONVERT(DATE, '19000101', 112), meal_date) % 7), meal_date)) PERSISTED NOT NULL,
    meal_type           VARCHAR(10)   NOT NULL,
    planned_servings    DECIMAL(5,1)  NOT NULL,

    CONSTRAINT PK_MEAL_PLAN_ENTRY PRIMARY KEY (meal_plan_entry_id),
    CONSTRAINT FK_MPE_MEAL_PLAN FOREIGN KEY (meal_plan_id, meal_week_start)
        REFERENCES [MEAL_PLAN](meal_plan_id, week_start_date) ON DELETE CASCADE,
    CONSTRAINT FK_MPE_RECIPE FOREIGN KEY (recipe_id)
        REFERENCES [RECIPE_POST](recipe_id) ON DELETE NO ACTION,
    CONSTRAINT CK_MPE_meal_type CHECK (
        meal_type IN ('BREAKFAST', 'LUNCH', 'DINNER')
    ),
    CONSTRAINT CK_MPE_planned_servings CHECK (planned_servings > 0),
    CONSTRAINT UQ_MPE_unique_slot UNIQUE (
        meal_plan_id, meal_date, meal_type, recipe_id
    )
);
GO

-- 7.3 SHOPPING_LIST — Shopping list (from meal plan, recipe, or manual)
-- Source: Data Dictionary 4.16, FR-53, BR-32
CREATE TABLE [SHOPPING_LIST] (
    shopping_list_id  BIGINT          IDENTITY(1,1)  NOT NULL,
    user_id           BIGINT          NOT NULL,
    name              NVARCHAR(200)   NOT NULL,
    created_at        DATETIME2(7)    NOT NULL
        CONSTRAINT DF_SHOPPING_LIST_created_at DEFAULT SYSUTCDATETIME(),
    updated_at        DATETIME2(7)    NOT NULL
        CONSTRAINT DF_SHOPPING_LIST_updated_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_SHOPPING_LIST PRIMARY KEY (shopping_list_id),
    CONSTRAINT FK_SHOPPING_LIST_USER FOREIGN KEY (user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION
);
GO

-- 7.4 SHOPPING_LIST_ITEM — Individual item to buy
-- Source: Data Dictionary 4.17, FR-53/54, BR-12/14/73
-- Must have ingredient_id OR custom_ingredient_name (or both).
-- Custom name must not be blank or whitespace-only.
-- CASCADE: items belong to list — delete list deletes all items.
CREATE TABLE [SHOPPING_LIST_ITEM] (
    shopping_list_item_id  BIGINT         IDENTITY(1,1)  NOT NULL,
    shopping_list_id       BIGINT         NOT NULL,
    ingredient_id          BIGINT         NULL,
    unit_id                INT            NOT NULL,
    custom_ingredient_name NVARCHAR(200)  NULL,
    quantity               DECIMAL(10,2)  NOT NULL,
    is_bought              BIT            NOT NULL
        CONSTRAINT DF_SLI_is_bought DEFAULT 0,

    CONSTRAINT PK_SHOPPING_LIST_ITEM PRIMARY KEY (shopping_list_item_id),
    CONSTRAINT FK_SLI_SHOPPING_LIST FOREIGN KEY (shopping_list_id)
        REFERENCES [SHOPPING_LIST](shopping_list_id) ON DELETE CASCADE,
    CONSTRAINT FK_SLI_INGREDIENT FOREIGN KEY (ingredient_id)
        REFERENCES [INGREDIENT](ingredient_id) ON DELETE NO ACTION,
    CONSTRAINT FK_SLI_UNIT FOREIGN KEY (unit_id)
        REFERENCES [UNIT](unit_id) ON DELETE NO ACTION,
    CONSTRAINT CK_SLI_quantity CHECK (quantity > 0),
    CONSTRAINT CK_SLI_target CHECK (
        ingredient_id IS NOT NULL OR custom_ingredient_name IS NOT NULL
    ),
    CONSTRAINT CK_SLI_custom_name_not_blank CHECK (
        custom_ingredient_name IS NULL
        OR LEN(TRIM(NCHAR(9)+NCHAR(10)+NCHAR(13)+NCHAR(32)+NCHAR(160) FROM custom_ingredient_name)) > 0
    )
);
GO


-- ============================================================================
-- SECTION 8: Ingredient Unit Conversion
-- ============================================================================

-- 8.1 INGREDIENT_UNIT_CONVERSION — Per-ingredient conversion to grams
-- Source: Data Dictionary 4.20, FR-18, BR-48/73
-- Composite PK: one conversion rule per (ingredient, unit) pair
CREATE TABLE [INGREDIENT_UNIT_CONVERSION] (
    ingredient_id   BIGINT         NOT NULL,
    unit_id         INT            NOT NULL,
    grams_per_unit  DECIMAL(10,2)  NOT NULL,
    is_approximate  BIT            NOT NULL
        CONSTRAINT DF_IUC_is_approximate DEFAULT 0,
    is_active       BIT            NOT NULL
        CONSTRAINT DF_IUC_is_active DEFAULT 1,

    CONSTRAINT PK_INGREDIENT_UNIT_CONVERSION PRIMARY KEY (ingredient_id, unit_id),
    CONSTRAINT FK_IUC_INGREDIENT FOREIGN KEY (ingredient_id)
        REFERENCES [INGREDIENT](ingredient_id) ON DELETE NO ACTION,
    CONSTRAINT FK_IUC_UNIT FOREIGN KEY (unit_id)
        REFERENCES [UNIT](unit_id) ON DELETE NO ACTION,
    CONSTRAINT CK_IUC_grams_per_unit CHECK (grams_per_unit > 0)
);
GO


-- ============================================================================
-- SECTION 9: Subscription & Payment
-- ============================================================================

-- 9.1 SUBSCRIPTION — Membership tier (PLUS / PRO) with validity period (Q9, Q10)
-- Tier values: 'PLUS', 'PRO' (FREE tier is default entitlement, not stored in DB).
-- ends_at must be strictly greater than starts_at.
-- UQ_SUBSCRIPTION_active filtered unique index enforces max 1 ACTIVE subscription per user.
CREATE TABLE [SUBSCRIPTION] (
    subscription_id  BIGINT        IDENTITY(1,1)  NOT NULL,
    user_id          BIGINT        NOT NULL,
    tier             VARCHAR(10)   NOT NULL,
    status           VARCHAR(20)   NOT NULL
        CONSTRAINT DF_SUBSCRIPTION_status DEFAULT 'ACTIVE',
    starts_at        DATETIME2(7)  NOT NULL,
    ends_at          DATETIME2(7)  NOT NULL,
    created_at       DATETIME2(7)  NOT NULL
        CONSTRAINT DF_SUBSCRIPTION_created_at DEFAULT SYSUTCDATETIME(),
    updated_at       DATETIME2(7)  NOT NULL
        CONSTRAINT DF_SUBSCRIPTION_updated_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_SUBSCRIPTION PRIMARY KEY (subscription_id),
    CONSTRAINT FK_SUBSCRIPTION_USER FOREIGN KEY (user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_SUBSCRIPTION_tier CHECK (tier IN ('PLUS', 'PRO')),
    CONSTRAINT CK_SUBSCRIPTION_status CHECK (
        status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')
    ),
    CONSTRAINT CK_SUBSCRIPTION_period CHECK (ends_at > starts_at)
);
GO

-- 9.2 PAYMENT_TRANSACTION — payOS payment to activate subscription
-- Source: Data Dictionary 4.22, FR-13, BR-03
-- order_code UNIQUE: idempotency mechanism for payOS webhook (constraint #12)
CREATE TABLE [PAYMENT_TRANSACTION] (
    payment_transaction_id  BIGINT        IDENTITY(1,1)  NOT NULL,
    user_id                 BIGINT        NOT NULL,
    subscription_id         BIGINT        NULL,
    order_code              VARCHAR(100)  NOT NULL,
    amount_vnd              INT           NOT NULL,
    status                  VARCHAR(20)   NOT NULL
        CONSTRAINT DF_PAYMENT_status DEFAULT 'PENDING',
    created_at              DATETIME2(7)  NOT NULL
        CONSTRAINT DF_PAYMENT_created_at DEFAULT SYSUTCDATETIME(),
    paid_at                 DATETIME2(7)  NULL,

    CONSTRAINT PK_PAYMENT_TRANSACTION PRIMARY KEY (payment_transaction_id),
    CONSTRAINT FK_PAYMENT_USER FOREIGN KEY (user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_PAYMENT_SUBSCRIPTION FOREIGN KEY (subscription_id)
        REFERENCES [SUBSCRIPTION](subscription_id) ON DELETE NO ACTION,
    CONSTRAINT UQ_PAYMENT_order_code UNIQUE (order_code),
    CONSTRAINT CK_PAYMENT_status CHECK (
        status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT CK_PAYMENT_amount CHECK (amount_vnd > 0)
);
GO


-- ============================================================================
-- SECTION 10: Filtered Unique Indexes
-- (Business constraints that cannot be expressed by standard UNIQUE)
-- ============================================================================

-- Constraint #2: Exactly one cover image per recipe (BR-20)
CREATE UNIQUE NONCLUSTERED INDEX UQ_RECIPE_MEDIA_cover
    ON [RECIPE_MEDIA](recipe_id)
    WHERE is_cover = 1;
GO

-- Constraint #10: At most one PENDING expert application per user (BR-21)
CREATE UNIQUE NONCLUSTERED INDEX UQ_EXPERT_APP_pending
    ON [EXPERT_APPLICATION](user_id)
    WHERE status = 'PENDING';
GO

-- Constraint #15: Prevent duplicate open reports on same recipe by same reporter (BR-29)
CREATE UNIQUE NONCLUSTERED INDEX UQ_REPORT_open_recipe
    ON [REPORT](reporter_id, recipe_id)
    WHERE recipe_id IS NOT NULL
      AND status IN ('PENDING', 'PROCESSING');
GO

-- Constraint #15: Prevent duplicate open reports on same comment by same reporter (BR-29)
CREATE UNIQUE NONCLUSTERED INDEX UQ_REPORT_open_comment
    ON [REPORT](reporter_id, comment_id)
    WHERE comment_id IS NOT NULL
      AND status IN ('PENDING', 'PROCESSING');
GO

-- Constraint #26: At most one preference type per standard ingredient per user (Q11b)
CREATE UNIQUE NONCLUSTERED INDEX UQ_UIP_user_ingredient
    ON [USER_INGREDIENT_PREFERENCE](user_id, ingredient_id)
    WHERE ingredient_id IS NOT NULL;
GO

-- Constraint #26: At most one preference type per custom ingredient name per user (Q11b)
CREATE UNIQUE NONCLUSTERED INDEX UQ_UIP_user_custom_name
    ON [USER_INGREDIENT_PREFERENCE](user_id, custom_ingredient_name)
    WHERE ingredient_id IS NULL AND custom_ingredient_name IS NOT NULL;
GO

-- Constraint #27: At most one ACTIVE subscription per user (Q10)
CREATE UNIQUE NONCLUSTERED INDEX UQ_SUBSCRIPTION_active
    ON [SUBSCRIPTION](user_id)
    WHERE status = 'ACTIVE';
GO


-- ============================================================================
-- SECTION 11: Performance Indexes
-- ============================================================================

-- Recipe queries
CREATE NONCLUSTERED INDEX IX_RECIPE_POST_author
    ON [RECIPE_POST](author_id);
GO
CREATE NONCLUSTERED INDEX IX_RECIPE_POST_status_published
    ON [RECIPE_POST](status, published_at DESC);
GO
CREATE NONCLUSTERED INDEX IX_RECIPE_POST_dish_category
    ON [RECIPE_POST](dish_category);
GO

-- Comment tree traversal
CREATE NONCLUSTERED INDEX IX_COMMENT_recipe
    ON [COMMENT](recipe_id, created_at);
GO
CREATE NONCLUSTERED INDEX IX_COMMENT_parent
    ON [COMMENT](parent_comment_id)
    WHERE parent_comment_id IS NOT NULL;
GO

-- Notification inbox (unread first, newest first)
CREATE NONCLUSTERED INDEX IX_NOTIFICATION_user_inbox
    ON [NOTIFICATION](user_id, is_read, created_at DESC);
GO

-- View statistics by recipe and time window
CREATE NONCLUSTERED INDEX IX_RECIPE_VIEW_recipe_time
    ON [RECIPE_VIEW](recipe_id, viewed_at);
GO
CREATE NONCLUSTERED INDEX IX_RECIPE_VIEW_user
    ON [RECIPE_VIEW](user_id)
    WHERE user_id IS NOT NULL;
GO

-- Reaction lookup by recipe
CREATE NONCLUSTERED INDEX IX_RECIPE_REACTION_recipe
    ON [RECIPE_REACTION](recipe_id);
GO

-- Report moderation queue
CREATE NONCLUSTERED INDEX IX_REPORT_status
    ON [REPORT](status, created_at);
GO

-- Follow: "who follows me?" query
CREATE NONCLUSTERED INDEX IX_USER_FOLLOW_followed
    ON [USER_FOLLOW](followed_user_id);
GO

-- Subscription active lookup
CREATE NONCLUSTERED INDEX IX_SUBSCRIPTION_user
    ON [SUBSCRIPTION](user_id, status);
GO

-- Meal plan and shopping list owner lookup
CREATE NONCLUSTERED INDEX IX_MEAL_PLAN_user
    ON [MEAL_PLAN](user_id);
GO
CREATE NONCLUSTERED INDEX IX_SHOPPING_LIST_user
    ON [SHOPPING_LIST](user_id);
GO

-- Ingredient preference lookup
CREATE NONCLUSTERED INDEX IX_UIP_user
    ON [USER_INGREDIENT_PREFERENCE](user_id);
GO

-- Expert application lookup by user
CREATE NONCLUSTERED INDEX IX_EXPERT_APP_user
    ON [EXPERT_APPLICATION](user_id);
GO


-- ============================================================================
-- SECTION 12: Reference Data Seed — UNIT
-- (Mandatory reference data, NOT demo data)
-- ============================================================================

INSERT INTO [UNIT] (code, name, dimension, base_factor) VALUES
    ('g',       N'gram',            'MASS',    1.0),
    ('kg',      N'kilogram',        'MASS',    1000.0),
    ('ml',      N'mililít',         'VOLUME',  1.0),
    ('L',       N'lít',             'VOLUME',  1000.0),
    ('tbsp',    N'muỗng canh',      'VOLUME',  15.0),
    ('tsp',     N'muỗng cà phê',    'VOLUME',  5.0),
    ('cup',     N'chén/cốc',        'VOLUME',  240.0),
    ('quả',     N'quả',             'COUNT',   1.0),
    ('củ',      N'củ',              'COUNT',   1.0),
    ('bìa',     N'bìa',             'COUNT',   1.0),
    ('lá',      N'lá',              'COUNT',   1.0),
    ('nhánh',   N'nhánh',           'COUNT',   1.0),
    ('trái',    N'trái',            'COUNT',   1.0),
    ('miếng',   N'miếng',           'COUNT',   1.0),
    ('bó',      N'bó',              'COUNT',   1.0);
GO


-- ============================================================================
-- END OF SCHEMA SNAPSHOT
-- ============================================================================
-- Summary:
--   22 tables created
--   38 foreign keys defined (including 2 composite FKs: COMMENT, MEAL_PLAN_ENTRY)
--    4 composite primary keys (USER_FOLLOW, RECIPE_REACTION, SAVED_RECIPE,
--      INGREDIENT_UNIT_CONVERSION)
--    7 filtered unique indexes (cover image, pending application,
--      open report on recipe, open report on comment, UIP user_ingredient,
--      UIP user_custom_name, active subscription)
--    1 filtered unique index on USER.google_subject
--   15 UNIT reference data rows seeded
--
-- Cascade policy:
--   CASCADE  : RECIPE_POST → MEDIA, RECIPE_POST → INGREDIENT,
--              MEAL_PLAN → ENTRY (via composite FK), SHOPPING_LIST → ITEM
--   SET NULL : RECIPE_VIEW.user_id
--   NO ACTION: all other 33 foreign keys
-- ============================================================================
