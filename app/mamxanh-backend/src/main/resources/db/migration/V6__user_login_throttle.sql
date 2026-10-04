-- ============================================================================
-- Flyway Migration: V6__user_login_throttle.sql
-- Database Engine : Microsoft SQL Server
-- Project         : Mâm Xanh — Vegetarian Support System (SWP391)
-- Issue           : Refs #6 — FR-03-B password login and temporary login blocking
-- Date            : 2026-10-01
-- Source          : FR-03 (UC-03.4, AC-03.6–AC-03.9), NFR-07, database/README.md
--                   "Thiết kế mục tiêu bảng USER cho Auth", decisions Q13/Q24 and the
--                   PR #74 decision to name the block column login_blocked_until
-- ============================================================================
-- Change:
--   Brute-force protection per account, stored directly on USER (no LOGIN_THROTTLE
--   table, no IP throttling). failed_login_attempts counts consecutive wrong
--   passwords; the fifth sets login_blocked_until = now + 10 minutes. A successful
--   login resets both. This temporary block is independent of the administrative
--   account_status = 'LOCKED'.
--
--   Existing rows receive failed_login_attempts = 0 through the DEFAULT constraint.
--   Like V3, the CHECK constraint is declared in the same ALTER TABLE statement
--   because a batch cannot reference a column added earlier in the same batch.
--
-- IMPORTANT: Do NOT add USE or GO statements. Flyway parses via JDBC.
-- ============================================================================

ALTER TABLE [USER] ADD
    failed_login_attempts  INT           NOT NULL
        CONSTRAINT DF_USER_failed_login_attempts DEFAULT 0,
    login_blocked_until    DATETIME2(7)  NULL,
    CONSTRAINT CK_USER_failed_login_attempts_non_negative CHECK (failed_login_attempts >= 0);
