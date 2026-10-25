-- ============================================================================
-- Flyway Migration: V22__report_handling_by_admin.sql
-- Database Engine : Microsoft SQL Server
-- Project         : Mâm Xanh — Vegetarian Support System (SWP391)
-- Issue           : Refs #33 — FR-28 Administrator handles reports by status
-- Date            : 2026-10-25
-- Source          : SRS 3.12 (Report keeps decision, decision_reason, handled_by,
--                   handled_at), FR-28 AC-28.1–AC-28.5, BR-26, question Q63
-- ============================================================================
-- Change:
--   REPORT.handled_by records the Administrator who took the report into review
--   (AC-28.1) and stays set once the report is RESOLVED, so every decision keeps
--   who made it. FK NO ACTION: an account that handled reports is never deleted
--   through this relationship.
--
--   CK_REPORT_decision limits stored decisions to the outcomes the Administrator
--   can choose: no violation, warn the author, hide the Recipe Post, lock the
--   author account. Existing rows have no decision, so the check applies only to
--   new decisions.
--
--   The queue filtered by status keeps using IX_REPORT_status from V1.
--
-- IMPORTANT: Do NOT add USE or GO statements. Flyway parses via JDBC.
-- ============================================================================

ALTER TABLE [REPORT] ADD
    handled_by  BIGINT  NULL
        CONSTRAINT FK_REPORT_HANDLED_BY FOREIGN KEY REFERENCES [USER](user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_REPORT_decision CHECK (
        decision IS NULL
        OR decision IN ('NO_VIOLATION', 'WARN_AUTHOR', 'HIDE_RECIPE', 'LOCK_AUTHOR')
    );
