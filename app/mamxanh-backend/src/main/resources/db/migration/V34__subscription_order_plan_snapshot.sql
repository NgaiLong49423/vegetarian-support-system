ALTER TABLE PAYMENT_TRANSACTION
    ADD subscription_tier VARCHAR(10) NULL,
        token_quota_snapshot BIGINT NULL;

EXEC sys.sp_executesql N'
UPDATE payment
SET subscription_tier = COALESCE(subscription.tier,
        CASE payment.amount_vnd WHEN 49000 THEN ''PLUS'' WHEN 99000 THEN ''PRO'' END),
    token_quota_snapshot = COALESCE(subscription.token_quota,
        CASE payment.amount_vnd WHEN 49000 THEN 300000 WHEN 99000 THEN 600000 END)
FROM PAYMENT_TRANSACTION payment
LEFT JOIN SUBSCRIPTION subscription
    ON subscription.subscription_id = payment.subscription_id
    AND subscription.user_id = payment.user_id';

EXEC sys.sp_executesql N'
ALTER TABLE PAYMENT_TRANSACTION
    ADD CONSTRAINT CK_PAYMENT_token_quota_snapshot CHECK (
        token_quota_snapshot IS NULL OR token_quota_snapshot >= 0),
    CONSTRAINT CK_PAYMENT_subscription_tier_snapshot CHECK (
        subscription_tier IS NULL OR subscription_tier IN (''PLUS'', ''PRO''))';
