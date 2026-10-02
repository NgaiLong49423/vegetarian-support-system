-- ============================================================================
-- Flyway Migration: V3__user_email_verification_token.sql
-- Database Engine : Microsoft SQL Server
-- Project         : Mâm Xanh — Vegetarian Support System (SWP391)
-- Issue           : Refs #5 — FR-03-A registration and email verification
-- Date            : 2026-09-28
-- Source          : FR-03 (UC-03.1–UC-03.3, AC-03.1–AC-03.5), database/README.md
--                   "Thiết kế mục tiêu bảng USER cho Auth", decisions Q13/Q15/Q16
-- ============================================================================
-- Change:
--   Store the current email-verification token directly on USER instead of a
--   separate token table (Q15/Q16). Only the SHA-256 hex digest of the token is
--   stored; the raw token exists only in the verification email link.
--   Issuing a new token overwrites the previous one, so at most one token per
--   account is valid. A successful verification clears both columns.
--
--   The filtered unique index supports lookup by token digest and allows many
--   NULLs (verified accounts have no token).
--
--   Flyway sends this script to SQL Server as one batch, and a batch cannot
--   reference a column added earlier in the same batch. The CHECK constraint is
--   therefore declared in the same ALTER TABLE statement, and the index is
--   created through EXEC so it is compiled only after the columns exist.
--
-- IMPORTANT: Do NOT add USE or GO statements. Flyway parses via JDBC.
-- ============================================================================

ALTER TABLE [USER] ADD
    email_verification_token       VARCHAR(64)    NULL,
    verification_token_expires_at  DATETIME2(7)   NULL,
    CONSTRAINT CK_USER_verification_token_pair CHECK (
        (email_verification_token IS NULL AND verification_token_expires_at IS NULL)
        OR (email_verification_token IS NOT NULL AND verification_token_expires_at IS NOT NULL)
    );

EXEC (N'CREATE UNIQUE NONCLUSTERED INDEX UQ_USER_email_verification_token
    ON [USER](email_verification_token)
    WHERE email_verification_token IS NOT NULL');
