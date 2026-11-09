-- The one-time policy change applies to existing cycles without rewriting their
-- subscription snapshots. Later activations continue to use their own snapshot.
ALTER TABLE AI_PLAN_CONFIGURATION ADD paid_quota_policy_cutover_at DATETIME2(7) NULL;

-- Run in a dynamic batch so SQL Server compiles this statement after the
-- preceding ALTER TABLE has added the column.
EXEC sp_executesql N'UPDATE AI_PLAN_CONFIGURATION
SET paid_quota_policy_cutover_at = SYSUTCDATETIME()
WHERE plan_tier = ''PLUS'';';
