-- ============================================================================
-- Flyway Migration: V10__user_password_reset.sql
-- Database Engine : Microsoft SQL Server
-- Project         : Mâm Xanh — Vegetarian Support System (SWP391)
-- Issue           : Refs #9 — FR-03-E password reset
-- Date            : 2026-10-10
-- Source          : FR-03 (UC-03.6, UC-03.7, AC-03.14), database/README.md
--                   "Thiết kế mục tiêu bảng USER cho Auth", decisions Q13/Q27
-- ============================================================================
-- Change:
--   Store the current password-reset token on USER (Q13): only the SHA-256 hex
--   digest of the token and its 15-minute expiry. A new token overwrites the
--   previous one; a successful reset clears both columns.
--
--   Minimal reset-email rate-limit metadata (Q27): the time of the last reset
--   email (60-second cooldown) and a one-hour window with its start time and
--   the number of reset emails sent in it (at most 5 per account).
--
--   The filtered unique index supports lookup by token digest and allows many
--   NULLs. Constraints are declared in the same ALTER TABLE statement and the
--   index is created through EXEC, because Flyway sends the script as one batch
--   and a batch cannot reference columns added earlier in the same batch.
--
-- IMPORTANT: Do NOT add USE or GO statements. Flyway parses via JDBC.
-- ============================================================================

ALTER TABLE [USER] ADD
    password_reset_token              VARCHAR(64)    NULL,
    reset_token_expires_at            DATETIME2(7)   NULL,
    password_reset_sent_at            DATETIME2(7)   NULL,
    password_reset_window_started_at  DATETIME2(7)   NULL,
    password_reset_window_count       INT            NOT NULL
        CONSTRAINT DF_USER_password_reset_window_count DEFAULT 0,
    CONSTRAINT CK_USER_password_reset_token_pair CHECK (
        (password_reset_token IS NULL AND reset_token_expires_at IS NULL)
        OR (password_reset_token IS NOT NULL AND reset_token_expires_at IS NOT NULL)
    ),
    CONSTRAINT CK_USER_password_reset_window_count_non_negative CHECK (password_reset_window_count >= 0);

EXEC (N'CREATE UNIQUE NONCLUSTERED INDEX UQ_USER_password_reset_token
    ON [USER](password_reset_token)
    WHERE password_reset_token IS NOT NULL');
