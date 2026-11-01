-- FR-11 experimental branch: structural approval is pending the team's vote.
-- Technical accounting only; no prompt/response text, quota or account relationship.
CREATE TABLE AI_USAGE_TELEMETRY (
    event_id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id BIGINT NULL,
    anonymous_session_id VARCHAR(36) NULL,
    feature VARCHAR(10) NOT NULL,
    model VARCHAR(80) NOT NULL,
    service_tier VARCHAR(30) NOT NULL,
    occurred_at DATETIME2(7) NOT NULL,
    prompt_tokens BIGINT NOT NULL,
    response_tokens BIGINT NOT NULL,
    total_tokens BIGINT NOT NULL,
    thinking_tokens BIGINT NULL,
    cached_tokens BIGINT NULL,
    tool_use_prompt_tokens BIGINT NULL,
    usage_status VARCHAR(20) NOT NULL,
    pricing_version VARCHAR(40) NULL,
    estimated_cost_usd DECIMAL(24,12) NULL,
    CONSTRAINT CK_AI_TELEMETRY_actor CHECK (
        (user_id IS NOT NULL AND anonymous_session_id IS NULL) OR
        (user_id IS NULL AND anonymous_session_id IS NOT NULL)),
    CONSTRAINT CK_AI_TELEMETRY_feature CHECK (feature IN ('FR-21','FR-34','FR-36','FR-37','FR-47','FR-51')),
    CONSTRAINT CK_AI_TELEMETRY_usage CHECK (usage_status IN ('COMPLETE','MISSING','PARTIAL','INVALID')),
    CONSTRAINT CK_AI_TELEMETRY_tokens CHECK (prompt_tokens >= 0 AND response_tokens >= 0 AND total_tokens >= 0
        AND (thinking_tokens IS NULL OR thinking_tokens >= 0)
        AND (cached_tokens IS NULL OR cached_tokens >= 0)
        AND (tool_use_prompt_tokens IS NULL OR tool_use_prompt_tokens >= 0)),
    CONSTRAINT CK_AI_TELEMETRY_cost CHECK (
        (estimated_cost_usd IS NULL AND pricing_version IS NULL) OR
        (estimated_cost_usd >= 0 AND pricing_version IS NOT NULL))
);
CREATE INDEX IX_AI_TELEMETRY_time ON AI_USAGE_TELEMETRY(occurred_at);
CREATE INDEX IX_AI_TELEMETRY_feature_time ON AI_USAGE_TELEMETRY(feature, occurred_at);
