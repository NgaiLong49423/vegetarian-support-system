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

-- FR-28 report queue examples: one report per status and one per decision except LOCK_AUTHOR,
-- so no demo account is locked. Times are relative to the load so the queue order stays realistic.
DECLARE @now DATETIME2(7) = SYSUTCDATETIME();
DECLARE @admin_id BIGINT;
SELECT @admin_id = user_id FROM [USER] WHERE email = 'demo-admin@mamxanh.local';

-- HIDE_RECIPE uses its own post so the hidden state never touches posts used by other demos or tests.
DECLARE @hidden_author_id BIGINT;
SELECT @hidden_author_id = user_id FROM [USER] WHERE email = 'demo-expert-02@mamxanh.local';
DECLARE @hidden_title NVARCHAR(120) = N'Salad bơ xoài đậu phộng';

INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category, vegetarian_type, difficulty,
    servings, prep_time_min, cook_time_min, status, published_at, created_at, updated_at)
SELECT @hidden_author_id, @hidden_title, N'Salad rau xanh và trái cây dễ làm cho bữa trưa nhẹ.',
    N'Rửa sạch xà lách và dưa leo, để ráo rồi cắt miếng vừa ăn. Cắt bơ và xoài thành khối nhỏ. Rang đậu phộng rồi giã dập. Trộn rau củ với chút nước cốt chanh, rắc đậu phộng lên trên. Đặt mua bộ sốt trộn salad trọn gói giá ưu đãi qua trang bán hàng của mình để nhận thêm quà tặng.',
    'SALAD', 'VEGAN', 'EASY', 2, 15, 0, 'PUBLISHED',
    DATEADD(DAY, -7, @now), DATEADD(DAY, -7, @now), DATEADD(DAY, -7, @now)
WHERE @hidden_author_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM [RECIPE_POST] p WHERE p.author_id = @hidden_author_id AND p.title = @hidden_title);

DECLARE @gram INT = (SELECT unit_id FROM [UNIT] WHERE name = N'gram');
DECLARE @hidden_ingredients TABLE (name NVARCHAR(120), quantity DECIMAL(10,2));
INSERT INTO @hidden_ingredients VALUES
 (N'Xà lách romaine', 80), (N'Dưa leo', 80), (N'Bơ', 150), (N'Xoài', 150), (N'Đậu phộng', 20);
INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
SELECT p.recipe_id, ingredient.ingredient_id, @gram, seed.quantity
FROM @hidden_ingredients seed
CROSS APPLY (SELECT TOP 1 i.ingredient_id FROM [INGREDIENT] i WHERE i.name = seed.name ORDER BY i.ingredient_id) ingredient
JOIN [RECIPE_POST] p ON p.author_id = @hidden_author_id AND p.title = @hidden_title
WHERE NOT EXISTS (SELECT 1 FROM [RECIPE_INGREDIENT] ri WHERE ri.recipe_id = p.recipe_id AND ri.ingredient_id = ingredient.ingredient_id);

DECLARE @report_seed TABLE (
    reporter_email VARCHAR(255), author_email VARCHAR(255), recipe_title NVARCHAR(120), reason_code VARCHAR(30),
    description NVARCHAR(500), status VARCHAR(20), decision VARCHAR(30), decision_reason NVARCHAR(1000),
    created_hours INT, changed_hours INT);
INSERT INTO @report_seed VALUES
 ('demo-customer-10@mamxanh.local', 'demo-expert@mamxanh.local', N'Nấm đùi gà kho tiêu', 'FOOD_SAFETY_HAZARD',
  N'Phần sơ chế chưa nhắc rửa sạch và để ráo nấm trước khi kho. Nhờ quản trị viên xem lại.', 'OPEN', NULL, NULL, -3, -3),
 ('demo-customer-11@mamxanh.local', 'demo-expert@mamxanh.local', N'Cơm chiên dứa đậu phộng', 'RECIPE_INFO_OR_DIET_LABEL',
  N'Bài ghi thuần chay nhưng chưa nói rõ nên dùng nước tương chay. Nhờ quản trị viên kiểm tra nhãn món.', 'IN_REVIEW', NULL, NULL, -26, -20),
 ('demo-customer-12@mamxanh.local', 'demo-expert@mamxanh.local', N'Phở cuốn nấm bào ngư', 'COPYRIGHT_VIOLATION',
  N'Mình thấy công thức này khá giống một bài đã đăng ở trang khác. Nhờ quản trị viên kiểm tra.', 'RESOLVED', 'NO_VIOLATION',
  N'Đã đối chiếu với nội dung được nêu và chưa thấy phần nào sao chép.', -96, -72),
 ('demo-customer-13@mamxanh.local', 'demo-expert@mamxanh.local', N'Súp đậu lăng rau củ', 'RECIPE_INFO_OR_DIET_LABEL',
  N'Bài chưa ghi đậu lăng cần ngâm trước hay không, mình làm theo thì đậu lâu chín.', 'RESOLVED', 'WARN_AUTHOR',
  N'Bài chưa ghi rõ có cần ngâm đậu lăng trước khi nấu hay không. Bạn vui lòng bổ sung để người đọc làm đúng.', -72, -48),
 ('demo-customer-14@mamxanh.local', 'demo-expert-02@mamxanh.local', N'Salad bơ xoài đậu phộng', 'SPAM_ADVERTISING',
  N'Cuối phần hướng dẫn có lời mời mua bộ sốt qua trang bán hàng, không liên quan đến cách làm món.', 'RESOLVED', 'HIDE_RECIPE',
  N'Phần hướng dẫn có lời mời mua hàng không liên quan đến công thức.', -48, -24);

-- handled_by is the Administrator who took the report (IN_REVIEW) and stays on RESOLVED reports.
-- handled_at is set only on resolution, as AdminReportService does.
INSERT INTO [REPORT] (reporter_id, recipe_id, comment_id, reason_code, description, status, decision, decision_reason,
    created_at, updated_at, handled_at, handled_by)
SELECT reporter.user_id, post.recipe_id, NULL, seed.reason_code, seed.description, seed.status, seed.decision, seed.decision_reason,
    DATEADD(HOUR, seed.created_hours, @now), DATEADD(HOUR, seed.changed_hours, @now),
    CASE WHEN seed.status = 'RESOLVED' THEN DATEADD(HOUR, seed.changed_hours, @now) END,
    CASE WHEN seed.status = 'OPEN' THEN NULL ELSE @admin_id END
FROM @report_seed seed
JOIN [USER] reporter ON reporter.email = seed.reporter_email
JOIN [USER] author ON author.email = seed.author_email
JOIN [RECIPE_POST] post ON post.author_id = author.user_id AND post.title = seed.recipe_title
WHERE @admin_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM [REPORT] existing
    WHERE existing.reporter_id = reporter.user_id AND existing.recipe_id = post.recipe_id);

-- The resolved HIDE_RECIPE report leaves its post hidden from public reads, as RecipeModerationService does.
UPDATE post SET status = 'HIDDEN', updated_at = report.handled_at
FROM [RECIPE_POST] post
JOIN [REPORT] report ON report.recipe_id = post.recipe_id AND report.decision = 'HIDE_RECIPE'
JOIN @report_seed seed ON seed.decision = 'HIDE_RECIPE' AND seed.description = report.description
WHERE post.author_id = @hidden_author_id AND post.title = @hidden_title AND post.status = 'PUBLISHED';

-- Notifications use the wording of ModerationNotificationService: the reporter gets a general result,
-- the author gets the Administrator reason for a sanction, and neither names the reporter (AC-49.4, BR-28).
DECLARE @resolved TABLE (report_id BIGINT, reporter_id BIGINT, author_id BIGINT, recipe_id BIGINT, recipe_title NVARCHAR(120),
    decision VARCHAR(30), decision_reason NVARCHAR(1000), handled_at DATETIME2(7));
INSERT INTO @resolved
SELECT report.report_id, report.reporter_id, post.author_id, post.recipe_id, post.title, report.decision, report.decision_reason, report.handled_at
FROM @report_seed seed
JOIN [USER] reporter ON reporter.email = seed.reporter_email
JOIN [REPORT] report ON report.reporter_id = reporter.user_id AND report.description = seed.description
    AND report.status = 'RESOLVED' AND report.decision = seed.decision
JOIN [RECIPE_POST] post ON post.recipe_id = report.recipe_id
WHERE seed.status = 'RESOLVED';

INSERT INTO [NOTIFICATION] (user_id, report_id, notification_type, title, message, target_path, is_read, created_at)
SELECT r.reporter_id, r.report_id, 'REPORT_RESULT', N'Báo cáo của bạn đã có kết quả',
    CONCAT(N'Quản trị viên đã xem xét báo cáo của bạn về bài “', r.recipe_title, N'” và ',
        CASE WHEN r.decision = 'NO_VIOLATION' THEN N'chưa thấy nội dung vi phạm' ELSE N'đã xử lý nội dung vi phạm' END,
        N'. Cảm ơn bạn đã góp ý.'),
    CONCAT('/cong-thuc/id/', r.recipe_id), 0, r.handled_at
FROM @resolved r
WHERE NOT EXISTS (SELECT 1 FROM [NOTIFICATION] existing
    WHERE existing.user_id = r.reporter_id AND existing.report_id = r.report_id AND existing.notification_type = 'REPORT_RESULT');

INSERT INTO [NOTIFICATION] (user_id, report_id, notification_type, title, message, target_path, is_read, created_at)
SELECT r.author_id, r.report_id, 'MODERATION_DECISION',
    CASE r.decision WHEN 'WARN_AUTHOR' THEN N'Bài công thức của bạn nhận được cảnh báo' ELSE N'Bài công thức của bạn đã bị ẩn' END,
    CASE r.decision
        WHEN 'WARN_AUTHOR' THEN CONCAT(N'Quản trị viên đã xem xét bài “', r.recipe_title, N'” và gửi cảnh báo đến bạn. Lý do: ', r.decision_reason)
        ELSE CONCAT(N'Quản trị viên đã ẩn bài “', r.recipe_title, N'” khỏi trang công khai. Lý do: ', r.decision_reason)
    END,
    '/ho-so/chuyen-gia', 0, r.handled_at
FROM @resolved r
WHERE r.decision IN ('WARN_AUTHOR', 'HIDE_RECIPE')
AND NOT EXISTS (SELECT 1 FROM [NOTIFICATION] existing
    WHERE existing.user_id = r.author_id AND existing.report_id = r.report_id AND existing.notification_type = 'MODERATION_DECISION');

COMMIT TRANSACTION;
