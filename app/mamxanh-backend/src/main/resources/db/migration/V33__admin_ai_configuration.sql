CREATE TABLE AI_PLAN_CONFIGURATION (
    plan_tier VARCHAR(10) NOT NULL,
    price_vnd INT NOT NULL,
    token_quota BIGINT NOT NULL,
    pending_token_quota BIGINT NULL,
    pending_effective_at DATETIME2(7) NULL,
    updated_at DATETIME2(7) NOT NULL CONSTRAINT DF_AI_PLAN_CONFIGURATION_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_AI_PLAN_CONFIGURATION PRIMARY KEY (plan_tier),
    CONSTRAINT CK_AI_PLAN_CONFIGURATION_tier CHECK (plan_tier IN ('FREE', 'PLUS', 'PRO')),
    CONSTRAINT CK_AI_PLAN_CONFIGURATION_price CHECK (price_vnd >= 0),
    CONSTRAINT CK_AI_PLAN_CONFIGURATION_quota CHECK (
        token_quota >= 0 AND (pending_token_quota IS NULL OR pending_token_quota >= 0)),
    CONSTRAINT CK_AI_PLAN_CONFIGURATION_pending CHECK (
        (pending_token_quota IS NULL AND pending_effective_at IS NULL) OR
        (pending_token_quota IS NOT NULL AND pending_effective_at IS NOT NULL))
);

INSERT INTO AI_PLAN_CONFIGURATION (plan_tier, price_vnd, token_quota)
VALUES ('FREE', 0, 50000), ('PLUS', 99000, 300000), ('PRO', 199000, 600000);

CREATE TABLE AI_GEMINI_MODEL_CONFIGURATION (
    model_id VARCHAR(128) NOT NULL,
    display_name NVARCHAR(120) NOT NULL,
    enabled BIT NOT NULL,
    free_allowed BIT NOT NULL,
    free_default BIT NOT NULL,
    plus_allowed BIT NOT NULL,
    plus_default BIT NOT NULL,
    pro_allowed BIT NOT NULL,
    pro_default BIT NOT NULL,
    updated_at DATETIME2(7) NOT NULL CONSTRAINT DF_AI_GEMINI_MODEL_CONFIGURATION_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_AI_GEMINI_MODEL_CONFIGURATION PRIMARY KEY (model_id),
    CONSTRAINT CK_AI_GEMINI_MODEL_CONFIGURATION_id CHECK (model_id LIKE 'gemini-%'),
    CONSTRAINT CK_AI_GEMINI_MODEL_CONFIGURATION_default_access CHECK (
        (free_default = 0 OR (enabled = 1 AND free_allowed = 1)) AND
        (plus_default = 0 OR (enabled = 1 AND plus_allowed = 1)) AND
        (pro_default = 0 OR (enabled = 1 AND pro_allowed = 1)))
);

INSERT INTO AI_GEMINI_MODEL_CONFIGURATION
    (model_id, display_name, enabled, free_allowed, free_default, plus_allowed, plus_default, pro_allowed, pro_default)
VALUES
    ('gemini-3.5-flash-lite', N'Gemini 3.5 Flash-Lite', 1, 1, 1, 1, 0, 1, 0),
    ('gemini-3.8-flash', N'Gemini 3.8 Flash', 1, 0, 0, 1, 1, 1, 1);

CREATE TABLE AI_CREDIT_CONFIGURATION (
    configuration_id TINYINT NOT NULL,
    credits_per_1000_vnd DECIMAL(19,6) NOT NULL,
    credits_per_1000_overage_tokens BIGINT NOT NULL,
    minimum_top_up_vnd INT NOT NULL,
    maximum_top_up_vnd INT NOT NULL,
    updated_at DATETIME2(7) NOT NULL CONSTRAINT DF_AI_CREDIT_CONFIGURATION_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_AI_CREDIT_CONFIGURATION PRIMARY KEY (configuration_id),
    CONSTRAINT CK_AI_CREDIT_CONFIGURATION_singleton CHECK (configuration_id = 1),
    CONSTRAINT CK_AI_CREDIT_CONFIGURATION_positive CHECK (
        credits_per_1000_vnd > 0 AND credits_per_1000_overage_tokens > 0 AND minimum_top_up_vnd > 0),
    CONSTRAINT CK_AI_CREDIT_CONFIGURATION_top_up_range CHECK (maximum_top_up_vnd >= minimum_top_up_vnd)
);

INSERT INTO AI_CREDIT_CONFIGURATION
    (configuration_id, credits_per_1000_vnd, credits_per_1000_overage_tokens, minimum_top_up_vnd, maximum_top_up_vnd)
VALUES (1, 1000, 400, 10000, 500000);
