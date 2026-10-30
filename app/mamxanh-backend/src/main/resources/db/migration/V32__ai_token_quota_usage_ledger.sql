ALTER TABLE [SUBSCRIPTION]
    ADD token_quota BIGINT NOT NULL CONSTRAINT DF_SUBSCRIPTION_token_quota DEFAULT 0;

EXEC sys.sp_executesql N'UPDATE [SUBSCRIPTION]
SET token_quota = CASE tier WHEN ''PLUS'' THEN 300000 WHEN ''PRO'' THEN 600000 ELSE 0 END';

EXEC sys.sp_executesql N'ALTER TABLE [SUBSCRIPTION]
    ADD CONSTRAINT CK_SUBSCRIPTION_token_quota CHECK (token_quota >= 0)';

CREATE TABLE AI_TOKEN_USAGE_LEDGER (
    event_id VARCHAR(36) NOT NULL,
    user_id BIGINT NULL,
    anonymous_session_id VARCHAR(36) NULL,
    feature VARCHAR(10) NOT NULL,
    model VARCHAR(80) NULL,
    usage_status VARCHAR(20) NOT NULL,
    token_count BIGINT NULL,
    quota_limit BIGINT NOT NULL,
    period_start DATETIME2(7) NOT NULL,
    period_end DATETIME2(7) NOT NULL,
    occurred_at DATETIME2(7) NOT NULL,
    expires_at DATETIME2(7) NOT NULL,
    CONSTRAINT PK_AI_TOKEN_USAGE_LEDGER PRIMARY KEY (event_id),
    CONSTRAINT FK_AI_TOKEN_USAGE_LEDGER_USER FOREIGN KEY (user_id)
        REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_AI_TOKEN_USAGE_LEDGER_actor CHECK (
        (user_id IS NOT NULL AND anonymous_session_id IS NULL) OR
        (user_id IS NULL AND anonymous_session_id IS NOT NULL)),
    CONSTRAINT CK_AI_TOKEN_USAGE_LEDGER_status CHECK (
        usage_status IN ('RECORDED', 'MISSING', 'INVALID')),
    CONSTRAINT CK_AI_TOKEN_USAGE_LEDGER_tokens CHECK (
        (token_count IS NULL OR token_count >= 0) AND quota_limit >= 0 AND period_end > period_start)
);

CREATE INDEX IX_AI_TOKEN_USAGE_LEDGER_user_period
    ON AI_TOKEN_USAGE_LEDGER(user_id, period_start, period_end)
    INCLUDE (token_count, usage_status);
CREATE INDEX IX_AI_TOKEN_USAGE_LEDGER_guest
    ON AI_TOKEN_USAGE_LEDGER(anonymous_session_id)
    INCLUDE (token_count, usage_status);
CREATE INDEX IX_AI_TOKEN_USAGE_LEDGER_expiry
    ON AI_TOKEN_USAGE_LEDGER(expires_at);
