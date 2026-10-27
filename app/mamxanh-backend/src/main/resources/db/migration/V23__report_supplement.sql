-- ============================================================================
-- Flyway Migration: V23__report_supplement.sql
-- Database Engine : Microsoft SQL Server
-- Project         : Mâm Xanh — Vegetarian Support System (SWP391)
-- Issue           : Refs #35 — FR-30 supplements to an open report
-- Date            : 2026-10-25
-- Source          : FR-30 AC-30.1, SRS 3.14, BR-29, question Q67
-- ============================================================================
-- Change:
--   REPORT_SUPPLEMENT keeps each piece of information the reporter adds to a
--   report that is still OPEN or IN_REVIEW (AC-30.1), with its own time. Rows
--   are append-only: the application never updates or deletes them, so the
--   original REPORT.description and every later supplement stay readable in
--   order for the Administrator. The limits (5 supplements, 10-500 characters)
--   are enforced by the Backend service.
--
--   FK NO ACTION: a report with supplements is never deleted through this
--   relationship.
--
-- IMPORTANT: Do NOT add USE or GO statements. Flyway parses via JDBC.
-- ============================================================================

CREATE TABLE [REPORT_SUPPLEMENT] (
    supplement_id  BIGINT          IDENTITY(1,1)  NOT NULL,
    report_id      BIGINT          NOT NULL,
    content        NVARCHAR(500)   NOT NULL,
    created_at     DATETIME2(7)    NOT NULL
        CONSTRAINT DF_REPORT_SUPPLEMENT_created_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_REPORT_SUPPLEMENT PRIMARY KEY (supplement_id),
    CONSTRAINT FK_REPORT_SUPPLEMENT_REPORT FOREIGN KEY (report_id)
        REFERENCES [REPORT](report_id) ON DELETE NO ACTION
);

CREATE NONCLUSTERED INDEX IX_REPORT_SUPPLEMENT_report
    ON [REPORT_SUPPLEMENT](report_id, created_at, supplement_id);
