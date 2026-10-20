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

-- Threaded comments and reply notifications for the local community demo.
DECLARE @demo_comment_roots TABLE (
    recipe_title NVARCHAR(120), author_email VARCHAR(200), content NVARCHAR(1000), comment_id BIGINT NULL
);
INSERT INTO @demo_comment_roots (recipe_title, author_email, content) VALUES
 (N'Đậu hũ sốt cà chua', 'demo-customer-04@mamxanh.local', N'Mình đã làm món này cho bữa tối, phần sốt cà chua rất dễ ăn.'),
 (N'Salad trái cây hạt chia', 'demo-customer-05@mamxanh.local', N'Có thể chuẩn bị trái cây trước và thêm hạt chia ngay trước khi dùng.'),
 (N'Canh rau dền mướp hương nấm rơm', 'demo-customer-06@mamxanh.local', N'Món canh này hợp với bữa cơm gia đình, mình sẽ thử cuối tuần.'),
 (N'Mì udon xào rau củ kiểu Nhật', 'demo-customer-07@mamxanh.local', N'Mình thường cho rau vào sau cùng để giữ độ giòn.');

INSERT INTO [COMMENT] (recipe_id, user_id, parent_comment_id, parent_depth, content, depth, is_deleted, created_at, updated_at)
SELECT post.recipe_id, member.user_id, NULL, NULL, seed.content, 1, 0,
    DATEADD(DAY, -2, SYSUTCDATETIME()), DATEADD(DAY, -2, SYSUTCDATETIME())
FROM @demo_comment_roots seed
JOIN [RECIPE_POST] post ON post.author_id = @author_id AND post.title = seed.recipe_title AND post.status = 'PUBLISHED'
JOIN [USER] member ON member.email = seed.author_email
WHERE NOT EXISTS (
    SELECT 1 FROM [COMMENT] existing
    WHERE existing.recipe_id = post.recipe_id AND existing.user_id = member.user_id
      AND existing.parent_comment_id IS NULL AND existing.content = seed.content
);

UPDATE seed SET comment_id = existing.comment_id
FROM @demo_comment_roots seed
JOIN [RECIPE_POST] post ON post.author_id = @author_id AND post.title = seed.recipe_title
JOIN [USER] member ON member.email = seed.author_email
JOIN [COMMENT] existing ON existing.recipe_id = post.recipe_id AND existing.user_id = member.user_id
    AND existing.parent_comment_id IS NULL AND existing.content = seed.content;

DECLARE @demo_comment_replies TABLE (
    recipe_title NVARCHAR(120), root_author_email VARCHAR(200), root_content NVARCHAR(1000),
    reply_author_email VARCHAR(200), content NVARCHAR(1000), comment_id BIGINT NULL
);
INSERT INTO @demo_comment_replies (recipe_title, root_author_email, root_content, reply_author_email, content) VALUES
 (N'Đậu hũ sốt cà chua', 'demo-customer-04@mamxanh.local', N'Mình đã làm món này cho bữa tối, phần sốt cà chua rất dễ ăn.',
  'demo-expert@mamxanh.local', N'Cảm ơn bạn đã thử công thức! Có thể thêm chút tiêu đen nếu thích vị cay nhẹ.'),
 (N'Salad trái cây hạt chia', 'demo-customer-05@mamxanh.local', N'Có thể chuẩn bị trái cây trước và thêm hạt chia ngay trước khi dùng.',
  'demo-customer@mamxanh.local', N'Cảm ơn mẹo hay, mình sẽ thử bảo quản trái cây trong hộp kín.'),
 (N'Canh rau dền mướp hương nấm rơm', 'demo-customer-06@mamxanh.local', N'Món canh này hợp với bữa cơm gia đình, mình sẽ thử cuối tuần.',
  'demo-expert@mamxanh.local', N'Bạn có thể cho rau dền vào sau cùng để rau vừa chín tới.');

INSERT INTO [COMMENT] (recipe_id, user_id, parent_comment_id, parent_depth, content, depth, is_deleted, created_at, updated_at)
SELECT post.recipe_id, replier.user_id, root.comment_id, 1, seed.content, 2, 0,
    DATEADD(DAY, -1, SYSUTCDATETIME()), DATEADD(DAY, -1, SYSUTCDATETIME())
FROM @demo_comment_replies seed
JOIN @demo_comment_roots root ON root.recipe_title = seed.recipe_title
    AND root.author_email = seed.root_author_email AND root.content = seed.root_content
JOIN [RECIPE_POST] post ON post.author_id = @author_id AND post.title = seed.recipe_title
JOIN [USER] replier ON replier.email = seed.reply_author_email
WHERE NOT EXISTS (
    SELECT 1 FROM [COMMENT] existing
    WHERE existing.recipe_id = post.recipe_id AND existing.user_id = replier.user_id
      AND existing.parent_comment_id = root.comment_id AND existing.content = seed.content
);

UPDATE seed SET comment_id = existing.comment_id
FROM @demo_comment_replies seed
JOIN @demo_comment_roots root ON root.recipe_title = seed.recipe_title
    AND root.author_email = seed.root_author_email AND root.content = seed.root_content
JOIN [USER] replier ON replier.email = seed.reply_author_email
JOIN [COMMENT] existing ON existing.parent_comment_id = root.comment_id
    AND existing.user_id = replier.user_id AND existing.content = seed.content;

-- A saved-recipe collection with varied entries for two demo members.
INSERT INTO [SAVED_RECIPE] (user_id, recipe_id, saved_at)
SELECT member.user_id, post.recipe_id, DATEADD(DAY, -3, SYSUTCDATETIME())
FROM (VALUES
    ('demo-customer@mamxanh.local', N'Đậu hũ sốt cà chua'),
    ('demo-customer@mamxanh.local', N'Canh rau dền mướp hương nấm rơm'),
    ('demo-customer-04@mamxanh.local', N'Salad trái cây hạt chia'),
    ('demo-customer-04@mamxanh.local', N'Mì udon xào rau củ kiểu Nhật')
) seed(email, recipe_title)
JOIN [USER] member ON member.email = seed.email
JOIN [RECIPE_POST] post ON post.author_id = @author_id AND post.title = seed.recipe_title
WHERE NOT EXISTS (SELECT 1 FROM [SAVED_RECIPE] saved
    WHERE saved.user_id = member.user_id AND saved.recipe_id = post.recipe_id);

-- Moderation queue examples use only the current report status and reason values.
INSERT INTO [REPORT] (reporter_id, recipe_id, comment_id, reason_code, description, status, created_at, updated_at)
SELECT reporter.user_id, post.recipe_id, NULL, 'RECIPE_INFO_OR_DIET_LABEL',
    N'Nhờ quản trị viên kiểm tra nhãn món chay trong phần mô tả.', 'OPEN',
    DATEADD(HOUR, -5, SYSUTCDATETIME()), DATEADD(HOUR, -5, SYSUTCDATETIME())
FROM [USER] reporter
JOIN [RECIPE_POST] post ON post.author_id = @author_id AND post.title = N'Bánh xèo nấm và giá đỗ'
WHERE reporter.email = 'demo-customer-08@mamxanh.local'
AND NOT EXISTS (SELECT 1 FROM [REPORT] existing
    WHERE existing.reporter_id = reporter.user_id AND existing.recipe_id = post.recipe_id
      AND existing.status IN ('OPEN', 'IN_REVIEW'));

INSERT INTO [REPORT] (reporter_id, recipe_id, comment_id, reason_code, description, status, created_at, updated_at)
SELECT reporter.user_id, NULL, reply.comment_id, 'SPAM_ADVERTISING',
    N'Nhờ quản trị viên xem lại nội dung bình luận này.', 'IN_REVIEW',
    DATEADD(HOUR, -8, SYSUTCDATETIME()), DATEADD(HOUR, -8, SYSUTCDATETIME())
FROM @demo_comment_replies reply
JOIN [USER] reporter ON reporter.email = 'demo-customer-09@mamxanh.local'
WHERE reply.recipe_title = N'Salad trái cây hạt chia'
AND NOT EXISTS (SELECT 1 FROM [REPORT] existing
    WHERE existing.reporter_id = reporter.user_id AND existing.comment_id = reply.comment_id
      AND existing.status IN ('OPEN', 'IN_REVIEW'));

-- Reply notifications keep report authors anonymous and link only to the reply itself.
INSERT INTO [NOTIFICATION] (user_id, comment_id, notification_type, title, message, is_read, created_at, target_path)
SELECT root_member.user_id, reply.comment_id, 'COMMENT_REPLY', N'Có phản hồi mới',
    N'Một thành viên đã phản hồi bình luận của bạn.', 0, DATEADD(HOUR, -1, SYSUTCDATETIME()),
    CONCAT('/cong-thuc/id/', post.recipe_id, '#comments')
FROM @demo_comment_replies reply
JOIN @demo_comment_roots root ON root.recipe_title = reply.recipe_title
    AND root.author_email = reply.root_author_email AND root.content = reply.root_content
JOIN [USER] root_member ON root_member.email = root.author_email
JOIN [RECIPE_POST] post ON post.author_id = @author_id AND post.title = reply.recipe_title
WHERE NOT EXISTS (SELECT 1 FROM [NOTIFICATION] existing
    WHERE existing.user_id = root_member.user_id AND existing.comment_id = reply.comment_id
      AND existing.notification_type = 'COMMENT_REPLY');

COMMIT TRANSACTION;
