DECLARE @demo_expert_id BIGINT;
DECLARE @pro_quota BIGINT;

SELECT @demo_expert_id = user_id
FROM [USER]
WHERE email = 'demo-expert@mamxanh.local';

SELECT @pro_quota = token_quota
FROM AI_PLAN_CONFIGURATION
WHERE plan_tier = 'PRO';

IF @pro_quota IS NULL
    THROW 51086, 'AI plan configuration for PRO is required by the local demo seed.', 1;

IF @demo_expert_id IS NOT NULL
BEGIN
    -- The original annual demo entitlement predates quota snapshots and inherited the 0 default.
    -- Leave paid/admin-granted monthly subscriptions and any already configured snapshot untouched.
    UPDATE [SUBSCRIPTION]
    SET token_quota = @pro_quota,
        updated_at = SYSUTCDATETIME()
    WHERE user_id = @demo_expert_id
      AND tier = 'PRO'
      AND status = 'ACTIVE'
      AND token_quota = 0
      AND token_quota <> @pro_quota
      AND ends_at >= DATEADD(MONTH, 11, starts_at);
END;
