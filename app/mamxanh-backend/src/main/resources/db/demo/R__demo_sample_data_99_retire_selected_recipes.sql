-- Retire selected local demo and AI-test recipe posts from the visible recipe feed.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @demo_expert_id BIGINT;
DECLARE @demo_expert_02_id BIGINT;
SELECT @demo_expert_id = user_id
FROM [USER]
WHERE email = 'demo-expert@mamxanh.local';
SELECT @demo_expert_02_id = user_id
FROM [USER]
WHERE email = 'demo-expert-02@mamxanh.local';

IF @demo_expert_id IS NOT NULL
BEGIN
    UPDATE [RECIPE_POST]
    SET status = 'DELETED', updated_at = SYSUTCDATETIME()
    WHERE author_id = @demo_expert_id
      AND title IN (N'Salad cải bó xôi bơ lựu', N'Chè kê vàng chà là dừa')
      AND status <> 'DELETED';
END;

IF @demo_expert_02_id IS NOT NULL
BEGIN
    UPDATE [RECIPE_POST]
    SET status = 'DELETED', updated_at = SYSUTCDATETIME()
    WHERE author_id = @demo_expert_02_id
      AND title = N'Bánh khoai tím dẻo kiểm thử AI mw4cfigl'
      AND status <> 'DELETED';
END;

COMMIT TRANSACTION;