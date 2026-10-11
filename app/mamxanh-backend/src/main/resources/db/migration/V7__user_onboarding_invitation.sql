-- ============================================================================
-- Flyway Migration: V7__user_onboarding_invitation.sql
-- Database Engine : Microsoft SQL Server
-- Project         : Mâm Xanh — Vegetarian Support System (SWP391)
-- Issue           : Refs #36 — FR-31 Onboarding, dietary preferences and the
--                   personalized-AI gate
-- Date            : 2026-10-05
-- Source          : FR-31 (UC-31.1, UC-31.2, AC-31.10), PR #92 review F001
-- ============================================================================
-- Change:
--   AC-31.10 invites a NEW account to the Onboarding questionnaire only once,
--   after its first successful sign-in. onboarding_status alone cannot express
--   this: a Member who leaves the questionnaire without "Hoàn tất" or "Bỏ qua"
--   stays NOT_STARTED. onboarding_invited_at records when the invitation was
--   shown; NULL means it has not been shown yet. The Backend shows it only while
--   onboarding_status = 'NOT_STARTED' and this column is NULL.
--
--   Accounts that already exist are never asked automatically (AC-31.10), so
--   they count as already invited at migration time. Their onboarding_status is
--   left unchanged: SKIPPED keeps meaning "the Member pressed Bỏ qua", and they
--   can still complete the profile from Settings. Accounts created afterwards
--   start with NULL and are invited once.
--
--   Flyway sends this script to SQL Server as one batch, and a batch cannot
--   reference a column added earlier in the same batch (see V3), so the UPDATE
--   runs through EXEC and is compiled only after the column exists.
--
-- IMPORTANT: Do NOT add USE or GO statements. Flyway parses via JDBC.
-- ============================================================================

ALTER TABLE [USER] ADD
    onboarding_invited_at  DATETIME2(7)  NULL;

EXEC (N'UPDATE [USER]
    SET onboarding_invited_at = SYSUTCDATETIME(),
        updated_at = SYSUTCDATETIME()');
