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

-- FR-30 supplements on the open Recipe Post report from seed 60, so its reporter sees them on the recipe page.
-- Like ReportSupplementService, rows are added only while the report is OPEN or IN_REVIEW and the original
-- description stays unchanged. Each supplement is placed after the report time, and the limit of five
-- supplements per report is never passed when a local database already has some.
DECLARE @supplement_seed TABLE (content NVARCHAR(500), minutes_after_report INT);
INSERT INTO @supplement_seed VALUES
 (N'Mình đọc lại thì phần mô tả ghi món chay nhưng chưa nói rõ bột bánh có dùng trứng hay không.', 60),
 (N'Bổ sung thêm: phần nước chấm trong bài chưa ghi dùng nước mắm chay hay nước tương.', 180);

INSERT INTO [REPORT_SUPPLEMENT] (report_id, content, created_at)
SELECT report.report_id, seed.content, DATEADD(MINUTE, seed.minutes_after_report, report.created_at)
FROM @supplement_seed seed
CROSS JOIN [REPORT] report
JOIN [USER] reporter ON reporter.user_id = report.reporter_id AND reporter.email = 'demo-customer-08@mamxanh.local'
JOIN [USER] author ON author.email = 'demo-expert@mamxanh.local'
JOIN [RECIPE_POST] post ON post.recipe_id = report.recipe_id AND post.author_id = author.user_id
    AND post.title = N'Bánh xèo nấm và giá đỗ'
WHERE report.status IN ('OPEN', 'IN_REVIEW')
AND (SELECT COUNT(*) FROM [REPORT_SUPPLEMENT] s WHERE s.report_id = report.report_id)
    + (SELECT COUNT(*) FROM @supplement_seed) <= 5
AND NOT EXISTS (SELECT 1 FROM [REPORT_SUPPLEMENT] existing
    WHERE existing.report_id = report.report_id AND existing.content = seed.content);

COMMIT TRANSACTION;
