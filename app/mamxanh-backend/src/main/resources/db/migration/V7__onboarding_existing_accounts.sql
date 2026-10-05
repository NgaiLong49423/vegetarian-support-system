-- ============================================================================
-- V7 — FR-31 (Issue #36): do not invite accounts that already exist
-- ============================================================================
-- AC-31.10: the Onboarding invitation is shown only after the first successful
-- sign-in of a NEW account; existing accounts are never asked automatically.
-- Every account created before this migration still has the column default
-- NOT_STARTED, so it is marked SKIPPED: the invitation is closed, the profile
-- stays incomplete, and the Member can still complete it from Settings.
-- Accounts created afterwards keep the default NOT_STARTED and are invited once.
-- Data-only change: no column, constraint or index changes.
UPDATE [USER]
SET onboarding_status = 'SKIPPED',
    updated_at = SYSUTCDATETIME()
WHERE onboarding_status = 'NOT_STARTED';
