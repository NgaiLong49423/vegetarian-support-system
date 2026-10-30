SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @expert_id BIGINT;
DECLARE @customer_id BIGINT;
DECLARE @new_member_id BIGINT;
DECLARE @customer_04_id BIGINT;
DECLARE @customer_05_id BIGINT;
DECLARE @customer_06_id BIGINT;

SELECT @expert_id = user_id FROM [USER] WHERE email = 'demo-expert@mamxanh.local';
SELECT @customer_id = user_id FROM [USER] WHERE email = 'demo-customer@mamxanh.local';
SELECT @new_member_id = user_id FROM [USER] WHERE email = 'demo-new-member@mamxanh.local';
SELECT @customer_04_id = user_id FROM [USER] WHERE email = 'demo-customer-04@mamxanh.local';
SELECT @customer_05_id = user_id FROM [USER] WHERE email = 'demo-customer-05@mamxanh.local';
SELECT @customer_06_id = user_id FROM [USER] WHERE email = 'demo-customer-06@mamxanh.local';

-- Subscriptions
DECLARE @expert_sub_id BIGINT;
DECLARE @customer_sub_id BIGINT;
DECLARE @customer_06_sub_id BIGINT;

SELECT @expert_sub_id = subscription_id FROM [SUBSCRIPTION] WHERE user_id = @expert_id AND status = 'ACTIVE';

IF @customer_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM [SUBSCRIPTION] WHERE user_id = @customer_id AND status = 'ACTIVE')
BEGIN
    INSERT INTO [SUBSCRIPTION] (user_id, tier, status, starts_at, ends_at)
    VALUES (@customer_id, 'PLUS', 'ACTIVE', DATEADD(MINUTE, -30, SYSUTCDATETIME()), DATEADD(MONTH, 1, SYSUTCDATETIME()));
END
SELECT @customer_sub_id = subscription_id FROM [SUBSCRIPTION] WHERE user_id = @customer_id AND status = 'ACTIVE';

IF @customer_06_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM [SUBSCRIPTION] WHERE user_id = @customer_06_id AND status = 'ACTIVE')
BEGIN
    INSERT INTO [SUBSCRIPTION] (user_id, tier, status, starts_at, ends_at)
    VALUES (@customer_06_id, 'PRO', 'ACTIVE', DATEADD(MINUTE, -20, SYSUTCDATETIME()), DATEADD(MONTH, 1, SYSUTCDATETIME()));
END
SELECT @customer_06_sub_id = subscription_id FROM [SUBSCRIPTION] WHERE user_id = @customer_06_id AND status = 'ACTIVE';

-- 1. TX-1 for demo-customer (CANCELLED PLUS)
IF @customer_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM [PAYMENT_TRANSACTION] WHERE order_code = '7790172734618463')
BEGIN
    INSERT INTO [PAYMENT_TRANSACTION] (user_id, subscription_id, order_code, amount_vnd, status, created_at, paid_at)
    VALUES (@customer_id, NULL, '7790172734618463', 49000, 'CANCELLED', DATEADD(MINUTE, -35, SYSUTCDATETIME()), NULL);
END

-- 2. TX-2 for demo-customer (PAID PLUS)
IF @customer_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM [PAYMENT_TRANSACTION] WHERE order_code = '755744502266367')
BEGIN
    INSERT INTO [PAYMENT_TRANSACTION] (user_id, subscription_id, order_code, amount_vnd, status, created_at, paid_at)
    VALUES (@customer_id, @customer_sub_id, '755744502266367', 49000, 'PAID', DATEADD(MINUTE, -30, SYSUTCDATETIME()), DATEADD(MINUTE, -29, SYSUTCDATETIME()));
END

-- 3. Expert PRO Subscription Payment (PAID PRO)
IF @expert_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM [PAYMENT_TRANSACTION] WHERE order_code = '89102450123401')
BEGIN
    INSERT INTO [PAYMENT_TRANSACTION] (user_id, subscription_id, order_code, amount_vnd, status, created_at, paid_at)
    VALUES (@expert_id, @expert_sub_id, '89102450123401', 99000, 'PAID', DATEADD(DAY, -10, SYSUTCDATETIME()), DATEADD(DAY, -10, SYSUTCDATETIME()));
END

-- 4. demo-new-member PENDING PLUS
IF @new_member_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM [PAYMENT_TRANSACTION] WHERE order_code = '7819203847192834')
BEGIN
    INSERT INTO [PAYMENT_TRANSACTION] (user_id, subscription_id, order_code, amount_vnd, status, created_at, paid_at)
    VALUES (@new_member_id, NULL, '7819203847192834', 49000, 'PENDING', DATEADD(MINUTE, -15, SYSUTCDATETIME()), NULL);
END

-- 5. demo-customer-04 FAILED PRO
IF @customer_04_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM [PAYMENT_TRANSACTION] WHERE order_code = '7629384710293847')
BEGIN
    INSERT INTO [PAYMENT_TRANSACTION] (user_id, subscription_id, order_code, amount_vnd, status, created_at, paid_at)
    VALUES (@customer_04_id, NULL, '7629384710293847', 99000, 'FAILED', DATEADD(HOUR, -2, SYSUTCDATETIME()), NULL);
END

-- 6. demo-customer-05 CANCELLED PRO
IF @customer_05_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM [PAYMENT_TRANSACTION] WHERE order_code = '7918273645019283')
BEGIN
    INSERT INTO [PAYMENT_TRANSACTION] (user_id, subscription_id, order_code, amount_vnd, status, created_at, paid_at)
    VALUES (@customer_05_id, NULL, '7918273645019283', 99000, 'CANCELLED', DATEADD(HOUR, -1, SYSUTCDATETIME()), NULL);
END

-- 7. demo-customer-06 FAILED PRO (1st attempt)
IF @customer_06_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM [PAYMENT_TRANSACTION] WHERE order_code = '7491028374619283')
BEGIN
    INSERT INTO [PAYMENT_TRANSACTION] (user_id, subscription_id, order_code, amount_vnd, status, created_at, paid_at)
    VALUES (@customer_06_id, NULL, '7491028374619283', 99000, 'FAILED', DATEADD(MINUTE, -25, SYSUTCDATETIME()), NULL);
END

-- 8. demo-customer-06 PAID PRO (2nd attempt)
IF @customer_06_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM [PAYMENT_TRANSACTION] WHERE order_code = '7591028374619284')
BEGIN
    INSERT INTO [PAYMENT_TRANSACTION] (user_id, subscription_id, order_code, amount_vnd, status, created_at, paid_at)
    VALUES (@customer_06_id, @customer_06_sub_id, '7591028374619284', 99000, 'PAID', DATEADD(MINUTE, -20, SYSUTCDATETIME()), DATEADD(MINUTE, -19, SYSUTCDATETIME()));
END

COMMIT TRANSACTION;
