ALTER TABLE PAYMENT_TRANSACTION
    ADD product_type VARCHAR(20) NOT NULL
            CONSTRAINT DF_PAYMENT_product_type DEFAULT 'SUBSCRIPTION',
        credit_amount_snapshot DECIMAL(38,9) NULL,
        credits_per_1000_vnd_snapshot DECIMAL(19,6) NULL,
        expires_at DATETIME2(7) NULL;

ALTER TABLE PAYMENT_TRANSACTION DROP CONSTRAINT CK_PAYMENT_status;

EXEC(N'ALTER TABLE PAYMENT_TRANSACTION
    ADD CONSTRAINT CK_PAYMENT_status CHECK (
        status IN (''PENDING'', ''PAID'', ''FAILED'', ''CANCELLED'', ''NEEDS_RECONCILIATION''))');

EXEC(N'ALTER TABLE PAYMENT_TRANSACTION
    ADD CONSTRAINT CK_PAYMENT_product_type CHECK (
        product_type IN (''SUBSCRIPTION'', ''AI_CREDIT''))');

EXEC(N'ALTER TABLE PAYMENT_TRANSACTION
    ADD CONSTRAINT CK_PAYMENT_credit_snapshot CHECK (
        (product_type = ''SUBSCRIPTION'' AND credit_amount_snapshot IS NULL
            AND credits_per_1000_vnd_snapshot IS NULL)
        OR (product_type = ''AI_CREDIT'' AND subscription_tier IS NULL
            AND token_quota_snapshot IS NULL AND credit_amount_snapshot > 0
            AND credits_per_1000_vnd_snapshot > 0 AND expires_at IS NOT NULL))');
