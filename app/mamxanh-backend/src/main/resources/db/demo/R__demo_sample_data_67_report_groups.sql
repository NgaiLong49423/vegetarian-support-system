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

-- FR-30 AC-30.4: two more Members report the post that already has the open report of seed 60, so the
-- Administrator board shows one group with three reports from three accounts. A Member gets a report here
-- only when they have none on this post yet, which also keeps one unresolved report per account (BR-29).
DECLARE @now DATETIME2(7) = SYSUTCDATETIME();
DECLARE @admin_id BIGINT = (SELECT user_id FROM [USER] WHERE email = 'demo-admin@mamxanh.local');
DECLARE @group_seed TABLE (
    reporter_email VARCHAR(255), reason_code VARCHAR(30), description NVARCHAR(500), status VARCHAR(20),
    created_hours INT, changed_hours INT);
INSERT INTO @group_seed VALUES
 ('demo-customer-15@mamxanh.local', 'FOOD_SAFETY_HAZARD',
  N'Bài hướng dẫn đổ bột khi chảo còn nguội, bánh dễ dính và sống bột. Nhờ quản trị viên xem lại bước này.', 'OPEN', -5, -5),
 ('demo-customer-16@mamxanh.local', 'RECIPE_INFO_OR_DIET_LABEL',
  N'Phần nguyên liệu có đậu xanh nhưng các bước làm không dùng tới. Nhờ quản trị viên kiểm tra lại bài.', 'IN_REVIEW', -10, -6);

INSERT INTO [REPORT] (reporter_id, recipe_id, comment_id, reason_code, description, status, created_at, updated_at, handled_by)
SELECT reporter.user_id, post.recipe_id, NULL, seed.reason_code, seed.description, seed.status,
    DATEADD(HOUR, seed.created_hours, @now), DATEADD(HOUR, seed.changed_hours, @now),
    CASE WHEN seed.status = 'OPEN' THEN NULL ELSE @admin_id END
FROM @group_seed seed
JOIN [USER] reporter ON reporter.email = seed.reporter_email
JOIN [USER] author ON author.email = 'demo-expert@mamxanh.local'
JOIN [RECIPE_POST] post ON post.author_id = author.user_id AND post.title = N'Bánh xèo nấm và giá đỗ'
WHERE @admin_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM [REPORT] existing
    WHERE existing.reporter_id = reporter.user_id AND existing.recipe_id = post.recipe_id);

COMMIT TRANSACTION;
