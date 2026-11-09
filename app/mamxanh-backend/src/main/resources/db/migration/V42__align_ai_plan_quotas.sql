-- Apply the approved AI quota policy without changing subscription periods,
-- usage ledger entries, credit balances, or existing payment snapshots.
UPDATE AI_PLAN_CONFIGURATION
SET token_quota = CASE plan_tier WHEN 'FREE' THEN 50000 WHEN 'PLUS' THEN 200000 WHEN 'PRO' THEN 500000 END,
    pending_token_quota = NULL,
    pending_effective_at = NULL,
    updated_at = SYSUTCDATETIME()
WHERE plan_tier IN ('FREE', 'PLUS', 'PRO');
