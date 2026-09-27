-- ============================================================================
-- Flyway Migration: V2__unit_code_unicode.sql
-- Database Engine : Microsoft SQL Server
-- Project         : Mâm Xanh — Vegetarian Support System (SWP391)
-- Issue           : Refs #63 — post-merge fix for PR #66
-- Date            : 2026-09-27
-- Source          : Data Dictionary 4.19 (UNIT.code), FR-18, BR-73
-- ============================================================================
-- Problem:
--   V1 created UNIT.code as VARCHAR(20). Under the default collation
--   SQL_Latin1_General_CP1_CI_AS (code page 1252, also the Azure SQL default),
--   characters outside code page 1252 are lost on insert:
--     'quả' is stored as 'qu?', 'củ' as 'c?', 'miếng' as 'mi?ng'.
--   Lookups such as WHERE code = N'quả' then return no row.
--
-- Fix:
--   1. Widen UNIT.code to NVARCHAR(20).
--   2. Restore the COUNT unit codes from UNIT.name (NVARCHAR, stored correctly
--      by V1). For these seed units the code is identical to the name.
--      Rows that are already correct are not touched, so this migration is
--      also safe on databases whose collation preserved the characters.
--
--   V1 is not edited: it is already shared on develop, and database/README.md
--   requires migrations to be append-only once shared.
--
-- IMPORTANT: Do NOT add USE or GO statements. Flyway parses via JDBC.
-- ============================================================================

ALTER TABLE [UNIT] DROP CONSTRAINT UQ_UNIT_code;

ALTER TABLE [UNIT] ALTER COLUMN code NVARCHAR(20) NOT NULL;

UPDATE [UNIT]
SET code = name
WHERE dimension = 'COUNT'
  AND name IN (N'quả', N'củ', N'bìa', N'lá', N'nhánh', N'trái', N'miếng', N'bó')
  AND code <> name;

ALTER TABLE [UNIT] ADD CONSTRAINT UQ_UNIT_code UNIQUE (code);
