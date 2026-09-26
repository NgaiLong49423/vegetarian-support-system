> **Document:** Changelog
> **File:** `CHANGELOG.md`
> **Version:** v2.32.0
> **Created:** 2026-06-14
> **Last Updated:** 2026-09-25
> **Status:** Active

# Changelog

Notable project changes, grouped by date and topic. Writing rules are maintained in [CONTRIBUTING.md](CONTRIBUTING.md#changelog-format). Documentation decisions below describe scope, not implemented or deployed features.

## 2026-09-25 — Finalize Database Modeling, Physical ERD, Schema Constraints, and Integrity Test Suite ([PR #66](https://github.com/NgaiLong49423/vegetarian-support-system/pull/66))

**Status:** Uncommitted working tree — Refs #63.

**Scope:** Complete Phase 2 database modeling for Issue #63 addressing Tech Lead review rounds 1 and 2 on PR #66: finalize SQL Server 2019 baseline migration and bootstrap schema with all business check/unique/composite constraints, resolve Draw.io container table layout formatting for physical ERD, update physical data dictionary, and expand verification test suite to 35 test cases.

### Added

- Add composite foreign key constraints enforcing relational hierarchy:
  - `FK_COMMENT_PARENT` on `(parent_comment_id, recipe_id, parent_depth)` referencing `COMMENT(comment_id, recipe_id, depth)` ensuring replies belong to the same recipe and child depth equals parent depth plus 1.
  - `FK_MPE_MEAL_PLAN` on `(meal_plan_id, meal_week_start)` referencing `MEAL_PLAN(meal_plan_id, week_start_date)` via persisted computed column `meal_week_start` guaranteeing `meal_date` strictly falls within the target Monday-to-Sunday planning week.
- Add strict business CHECK and UNIQUE constraints:
  - `CK_INGREDIENT_nutrition_supported`: requires all 9 nutrition indicators and `source_url` to be NOT NULL when `nutrition_supported = 1` (BR-52, Q5).
  - `CK_RECIPE_POST_total_time`: enforces `prep_time_min + cook_time_min > 0` (BR-19, FR-16).
  - `CK_RECIPE_POST_instructions_len`: enforces trimmed length between 10 and 5,000 characters (FR-16, AC-16.5).
  - `CK_RECIPE_POST_status` and `DF_RECIPE_POST_status`: restricts statuses to `PUBLISHED`, `HIDDEN`, `DELETED` with default `PUBLISHED` (FR-24).
  - `CK_RECIPE_MEDIA_display_order` (1..5) and `UQ_RECIPE_MEDIA_order` (`recipe_id`, `display_order`): restricts recipe media to at most 5 images (BR-20, FR-14).
  - `CK_*_target` and `CK_*_custom_name_not_blank` on `USER_INGREDIENT_PREFERENCE`, `RECIPE_INGREDIENT`, and `SHOPPING_LIST_ITEM`: requires at least one of `ingredient_id` or `custom_ingredient_name`, and rejects whitespace-only custom names (BR-12).
  - `UQ_UIP_user_ingredient` and `UQ_UIP_user_custom_name`: guarantees exactly one active preference type per ingredient per user (FR-31, Q11).
  - `CK_MEAL_PLAN_week_start_monday` and `UQ_MEAL_PLAN_user_week`: enforces Monday start date (`DATEDIFF(day, '1900-01-01', week_start_date) % 7 = 0`) and one meal plan per user per week (FR-09, Q3).
  - `CK_SUBSCRIPTION_tier` (`PLUS`, `PRO`), `CK_SUBSCRIPTION_period` (`ends_at > starts_at`), and `UQ_SUBSCRIPTION_active`: restricts paid tiers and prevents concurrent active subscriptions for the same user (FR-13, Q9, Q10).
  - `CK_USER_date_of_birth` (<= current date and >= 1900-01-01), `CK_USER_activity_level` (4 active levels), `CK_USER_nutrition_goal` (3 active goals), and `CK_USER_onboarding_status` with `DF_USER_onboarding_status` ('NOT_STARTED') (FR-35, Q7, Q12).
  - `CK_COMMENT_content_len`: enforces comment content length between 1 and 1,000 characters (FR-46, EF-46.1).
- Add 22 new test cases (TC16–TC37) to `database/queries.sql` asserting exact constraint names in `ERROR_MESSAGE()`, including TC36 for comment content length and TC37 for positive transaction amount, bringing the test suite to 37 test cases (70/70 test assertions PASS 100%).

### Changed

- Update `V1__baseline_schema.sql` and `database/schema.sql`:
  - Make all 9 `INGREDIENT` nutrition columns nullable (`NULL`) while preserving default `nutrition_supported = 0` (Q5).
  - Change `COMMENT.content` from `NVARCHAR(2000)` to `NVARCHAR(1000)` and add constraint `CK_COMMENT_content_len` (FR-46).
  - Tighten `PAYMENT_TRANSACTION.amount_vnd` check constraint `CK_PAYMENT_amount` from `>= 0` to `> 0` because free tier generates no transaction (Q9).
- Update `docs/diagrams/ERD/physical-erd-v1.0.0.drawio` and exported image `physical-erd-v1.0.0.drawio.png`: format all 22 tables and 196 physical columns with exact data types, nullability, primary/foreign/unique/check constraints, default values, and index indicators (Review Round 1 Point 1); synchronize 37 Crow's Foot connectors from Logical ERD v1.0.0; resolve container table layout styling to ensure correct row ordering and visible headers; synchronize `COMMENT.content` to `NVARCHAR(1000)` and `CK`.
- Update `docs/diagrams/ERD/data-dictionary.md` to version `v0.7.0`: remove all pending `⏳` markers for implemented items, update Section 1 status, document Phase 2 completion in Section 8, and record `CK_COMMENT_content_len` and `CK_PAYMENT_amount` (> 0) in Sections 4.10, 4.22, and 5.
- Update `database/README.md` to version `v0.4.0` with verified object counts (22 tables, 38 FKs, 57 checks, 55 defaults, 10 filtered indexes, 182 custom indexes, 37 test cases) and `sqlcmd` execution instructions.
- Update `docs/diagrams/ERD/README.md` to version `v1.13.0` line 16 with verified physical metrics (38 FKs, 57 check constraints, 55 default constraints, 10 filtered unique indexes, 182 custom nonclustered indexes).

### Fixed

- Fix Draw.io table row ordering and header occlusion for `COMMENT` and `MEAL_PLAN_ENTRY` by positioning added physical columns sequentially in table geometry.
- Fix negative length assertion test in `database/queries.sql` to be UTF-8 encoding agnostic across CLI environments.

## 2026-09-23 — Implement Physical ERD, SQL Server Baseline Migration, and Data Integrity Test Suite

**Status:** Committed — ae61073.

**Scope:** Complete Phase 2 database modeling for Issue #63, delivering the physical ERD, Flyway baseline schema migration, standalone bootstrap script, comprehensive physical data dictionary, and automated integrity validation query suite for Microsoft SQL Server 2019.

### Added

- Add Flyway baseline migration `V1__baseline_schema.sql` under `app/mamxanh-backend/src/main/resources/db/migration/` establishing 22 core tables, 38 foreign keys, 21 indexes (including 5 filtered unique indexes), 41 check constraints, 52 default constraints, and initial seed data for 15 standard measurement units without JDBC-incompatible batch separators.
- Add standalone bootstrap script `database/schema.sql` with `GO` batch delimiters and ANSI settings (`SET ANSI_NULLS ON;`, `SET QUOTED_IDENTIFIER ON;`) for SSMS and `sqlcmd`.
- Add `docs/diagrams/ERD/physical-erd-v1.0.0.drawio` defining the physical ERD with exact SQL Server 2019 data types, primary keys, foreign keys, and 36 entity relationship connectors in Crow's Foot notation, along with exported diagram image `docs/diagrams/ERD/physical-erd-v1.0.0.drawio.png`.
- Add comprehensive diagnostic and automated test suite in `database/queries.sql` featuring schema catalog auditing, 15 transactional positive and negative test cases verifying key constraints and cascade path behaviors, and core operational queries.

### Changed

- Update `docs/diagrams/ERD/data-dictionary.md` to version `v0.4.0`, completing all nine physical specification columns across 22 tables (178 entity columns), documenting the resolution of SQL Server multiple cascade path Error 1785, and checking off Phase 2 delivery milestones.
- Update `database/README.md` to version `v0.2.0` with step-by-step guidance for running the Flyway baseline migration, executing `schema.sql`, and running the automated test suite.
- Update `docs/diagrams/ERD/README.md` to version `v1.11.0` linking the newly created Physical ERD and updating workspace status.

### Fixed

- Eliminate multiple cascade path conflict (SQL Server Error 1785) across interrelated entities (`COMMENT`, `USER_FOLLOW`, `REPORT`) by restricting `ON DELETE CASCADE` strictly to four parent-child ownership relationships, applying `SET NULL` for guest recipe views, and setting the remaining 33 foreign keys to `NO ACTION`.
## 2026-09-25 — Confirm Vercel and Azure Deployment Baseline

**Status:** Committed — d1c2082.

**Scope:** Record the approved hosting split across maintained architecture, setup, workflow, and overview documentation without claiming that deployment automation or the complete production system already exists.

### Added

- None.

### Changed

- Confirm Vercel as the Frontend hosting platform and Azure App Service with Java 21 SE as the Backend hosting platform.
- Retain Azure SQL Database Serverless and Azure Blob Storage as the database and media services.
- Point the planned `.tech` custom domain to the Vercel-hosted Frontend instead of Azure Static Web Apps.
- Clarify that the current Vercel demo remains manually deployed and that Git auto-deploy or CI/CD has not been configured.
- Clarify that the application-level Dockerfiles standardize local development and are not the selected production deployment artifacts.
- Remove the resolved Vercel-versus-Azure Static Web Apps inconsistency from the C4 review findings and refresh the maintained document register.

### Fixed

- None.

## 2026-09-24 — Add Per-Application Docker Setup and Align C4 Guidance

**Status:** Committed — 43206ac.

**Scope:** Add independent Docker development environments and reproducible run instructions for the Frontend and Backend, then align the maintained C4 workspace guide with the updated diagram while preserving unresolved deployment-baseline differences for review.

### Added

- Add independent `Dockerfile` and `.dockerignore` files inside the Frontend and Backend application directories.
- Add direct and Docker-based startup, verification, database configuration, IntelliJ, and troubleshooting guidance to both application workspace guides.

### Changed

- Configure the Frontend container for Vite development on port 5173 with file watching and no browser launch inside the container.
- Configure the Backend container for Java 21 and Maven Wrapper development on port 8080 while keeping database credentials outside the image.
- Keep Docker as local development tooling with no shared root Dockerfile, database container, Docker Compose setup, or production deployment change.
- Align the C4 workspace guide with the four current architecture areas, external providers, request/response flows, Vercel deployment view, and known diagram corrections still required before approval.

### Fixed

- None.
## 2026-09-23 — Align Social, Recipe Comparison, Nutrition, and Expert Requirements

**Status:** Committed — 6e49cd9.

**Scope:** Align the conceptual data model and requirement semantics for required social following, public two-recipe comparison, nutrition analysis/PDF reporting, and expert applications without claiming implementation or creating GitHub Issues.

### Added

- Add `FR-59` and `BR-75` for directed Member follow relationships and represent `USER_FOLLOW` in the 22-entity Conceptual ERD.
- Add active `FR-60` and `BR-76` for comparing exactly two distinct public recipes by overview data, ingredients, and nine per-serving nutrition indicators.

### Changed

- Confirm `FR-59` and `BR-75` as `ACTIVE`, making `USER_FOLLOW` a required capability that must be inherited by the Logical ERD, Physical ERD, and implementation schema.
- Synchronize the document register, architecture baseline, C4 workspace guidance, and ERD workspace guidance with the active 22-entity conceptual model.
- Keep the nutrition MVP at nine indicators excluding sodium, and define the daily one-page and weekly two-page nutrition PDF contents and failure behavior.
- Clarify expert applications as text-based expertise and publishing-permission review, including applicant history, active-account revalidation, authorization refresh, and concurrent-decision handling.

### Fixed

- Resolve the mismatch between the root nutrition indicator list and the detailed nutrition requirements by using zinc consistently instead of sodium.

## 2026-09-21 — Expand the Frontend UI Demo and Document Manual Vercel Hosting ([PR #62](https://github.com/NgaiLong49423/vegetarian-support-system/pull/62))

**Status:** Committed — cf3911b370ef1c0f697901164565e70c9ffacb51.

**Scope:** Prepare a teacher-facing UI demonstration with browser-local interactions and explicit mock-data boundaries; this does not complete Backend integration or SRS acceptance.

### Added

- Add threaded recipe comments with reply, edit, delete, and retained replies after parent deletion.
- Add recipe ingredient scaling from original servings, meal-plan serving selection, and a six-reason recipe report form with local validation.
- Add nutrition-profile/BMI, AI plan, and transaction-history screens; paid-plan checkout remains disabled and transaction history has no fabricated records.
- Add seven browser tests alongside the existing smoke test and a Vercel SPA rewrite for direct route access.
- Record the manual Vercel UI demo and its limits in the Frontend guide without adding GitHub CI/CD or changing the Azure architecture baseline.

### Changed

- Move BMI, AI upgrade, and transaction-history navigation into account menus; display the current mock FREE plan beside the avatar.
- Label recipe nutrition explicitly as sample data for one serving, with ingredient scaling independent of the static panel.
- Require Vietnamese bug-record prose and index descriptions; translate BUG-001 through BUG-003 and retain their unresolved status.
- Synchronize project and contribution guidance with the manually hosted demo.

### Fixed

- Keep desktop navigation labels on one line, balance the home hero across two lines, and reuse the project logo for the app and favicon.

## 2026-09-20 — Align Project Overview and Local Database Configuration

**Status:** Committed — 14d6b0b9d370921494f2059e51862d7ee7065d57.

**Scope:** Align the root project overview with the actual Frontend, Backend, documentation, testing, and local SQL Server scaffolds while keeping credentials outside Git.

### Added

- Add a tracked local SQL Server configuration example and activate the local Spring profile by default for development.
- Add verified Backend and Frontend build/test commands to the root project overview.

### Changed

- Expand the repository tree and maintained-source links to reflect the current API, Backend proposal, Playwright, and application workspaces.
- Update the current implementation summary and MVP interaction wording without claiming that draft API or package proposals are implemented.

### Fixed

- Remove stale statements that the Frontend, build manifests, tests, and remote `develop` branch do not exist.
- Remove the retired Like interaction from the root MVP summary.

## 2026-09-20 — Document the Draft Backend Package Structure

**Status:** Committed — 14d6b0b9d370921494f2059e51862d7ee7065d57.

**Scope:** Record an incremental package-level proposal within the approved Backend modular-monolith boundary without claiming that the proposed modules, packages, or classes are implemented.

### Added

- Add a registered draft proposal covering package-by-business-capability organization, internal layer responsibilities, dependency boundaries, resources, tests, and incremental adoption criteria.

### Changed

- Extend the maintained document register with the proposal and its read trigger.

### Fixed

- None.

## 2026-09-20 — Add Bug Recording and Playwright Foundations

**Status:** Committed — 14d6b0b9d370921494f2059e51862d7ee7065d57.

**Scope:** Add a bounded, human-gated bug-recording procedure and a local Chromium browser-smoke foundation without introducing automatic fixes, Issue creation, CI integration, or claims of full frontend-backend E2E coverage.

### Added

- Add the `bug-recording` skill with passive recording, user-requested audit, read-only precedence, lightweight duplicate handling, and explicit authorization before GitHub Bug Issue creation.
- Add a tracked agent-output bug store with one file per bug and a compact metadata index for sequential IDs and `RECORDED`, `TRACKING`, or `RESOLVED` status; unknown evidence fields remain valid and GitHub continues to own work tracking.
- Add Playwright configuration and a repeatable Frontend browser smoke test with a fixed preview URL, Chromium, failure evidence, and an HTML report.

### Changed

- Add self-contained Frontend E2E scripts, including an automatic production build before Playwright starts the preview server.
- Align agent routing, documentation placement, Frontend guidance, and the test strategy with the real `app/mamxanh-frontend/` path and the selected Playwright boundary.

### Fixed

- Correct stale `app/frontend/` documentation routes that no longer matched the current Frontend scaffold.

## 2026-09-20 — Start the Authentication API Contract

**Status:** Committed — 14d6b0b9d370921494f2059e51862d7ee7065d57.

**Scope:** Establish the first under-review OpenAPI vertical slice for FR-03 and a lightweight integration guide without claiming that the documented endpoints are implemented.

### Added

- Add an OpenAPI 3.1.1 contract for registration, email verification, email/password and Google login, password reset, refresh-token rotation, reuse handling, and logout.
- Add a concise API integration guide for the shared base path, access/refresh-token flow, RFC 9457-style errors, security boundaries, and unresolved decisions that block implementation readiness.

### Changed

- Register `docs/api/API.md` and `docs/api/openapi.yaml` as maintained API documentation, with OpenAPI as the endpoint-level Source of Truth.

### Fixed

- None.

## 2026-09-19 — Confirm Weekly Workflow and Local Demo Gates

**Status:** Committed.

**Scope:** Apply the decision-maker's confirmed weekly workflow, keep CONTRIBUTING.md as the operational source, and align decision rationale and intake templates. Work began on September 18 and was completed on September 19 (Asia/Saigon).

### Added

- Require the project PR template and Bug Report form for human and agent submissions, including equivalent structured bodies through CLI/API; template changes require Tech Lead approval.
- Document owner accountability for AI-assisted frontend work, API contract approval before integration, immediate deadline-risk reporting, bug triage, and local verification before marking work Done.

### Changed

- Make review/approval and GitHub Actions optional for develop integration; require independent approval, current build/test checks, integration evidence, and post-merge local demo verification for main.
- Assign main PR coordination and acceptance to the Tech Lead; plan weekly workloads using relative SP without trimming accepted scope or counting parent and child estimates twice.
- Retain ADR-001/002 decision history while moving operational rules to CONTRIBUTING.md; align the workflow evidence register, project overview, test strategy, PR template, and existing bug form.
- Defer deployment/CD until the team is ready for Azure deployment. No GitHub settings, workflow code, live Issues, or Issue drafts were changed.

### Fixed

- Use Refs for main PRs and close Issues only after acceptance, avoiding automatic closure before post-merge verification.
- Explicitly distinguish the approved CI build/test gate from the existing release-source Action, which only checks the source branch.

## 2026-09-18 — Restore Steps and Media, Add Rating, View Tracking, Unit Conversion, and Multi-Mode Discovery

**Status:** Committed.

**Scope:** Realign the entire project documentation suite (PRD, SRS, Functional Requirements, Business Rules, Non-Functional Requirements, ERD Documentation, Architecture, and Test Strategy) to reflect newly confirmed business decisions: restore sequential cooking steps (`RECIPE_STEP`, 1–30 steps, reorderable, reactivating `FR-22`), restore recipe media gallery (`RECIPE_MEDIA`, 0–5 images with exactly 1 cover image), add recipe rating (`RECIPE_RATING`, 1–5 stars, authenticated Member only, 1 per recipe, editable, author excluded, Guest view-only, `FR-57`, `BR-69`), add recipe view tracking (`RECIPE_VIEW`, 30-minute deduplication window, 24h/7d/30d/all-time stats, `FR-58`, `BR-70`), define 6 discovery/sorting modes (Newest, Highest Rated, Most Viewed, Most Commented, Most Active `BR-71` via 7-day raw interactions, and Trending `BR-72` via freshness decay, non-AI based), standardize unit and conversion management (`UNIT`, `INGREDIENT_UNIT_CONVERSION`, `BR-73`) with strict elimination of qualitative terms like "vừa đủ" and a mandatory validation gate blocking publish if required conversion to gram is missing, and expand conceptual ERD baseline from 17 to exactly 23 entities.

### Added

- Added `FR-57` ("Đánh giá công thức nấu ăn bằng thang điểm sao (Recipe Rating)"): allows authenticated Members to rate recipes (1–5 stars), updates rating aggregates (`average_rating`, `rating_count`), prevents author self-rating, limits to 1 rating per user per recipe (editable), and grants Guests view-only access.
- Added `FR-58` ("Ghi nhận và tổng hợp lượt xem công thức (Recipe View Tracking)"): logs recipe view events with a 30-minute deduplication window per viewer session/IP/Member, updates view count asynchronously, and aggregates stats across 24h, 7 days, 30 days, and all-time.
- Added Business Rules:
  - `BR-69` ("Quy tắc đánh giá và tính điểm xếp hạng công thức (Recipe Rating)"): 1–5 integer stars, 1 rating per user/recipe, author self-rating prohibited, guest view-only.
  - `BR-70` ("Quy tắc ghi nhận lượt xem và chống trùng lặp (Recipe View Tracking & Deduplication)"): 30-minute deduplication window per viewer identity, asynchronous counter flush.
  - `BR-71` ("Quy tắc xếp hạng công thức hoạt động sôi nổi nhất (Most Active Recipe Ranking)"): calculated from 7-day raw interactions ($V_{7d} + 5 \times C_{7d} + 10 \times R_{7d}$) without freshness time decay.
  - `BR-72` ("Quy tắc xếp hạng công thức thịnh hành (Trending Recipe Ranking)"): calculated by scoring recent interactions scaled by a freshness decay factor ($\text{Score} / (T_{\text{age\_hours}} + 2)^\gamma$).
  - `BR-73` ("Quy tắc chuẩn hóa đơn vị đo lường và cổng kiểm định quy đổi nguyên liệu (Unit & Conversion Gate)"): mandates positive numeric quantities ($>0$), eliminates "vừa đủ", standardizes dimensions (`MASS`, `VOLUME`, `COUNT`), and strictly blocks publishing if inter-dimension conversion to gram is missing in `INGREDIENT_UNIT_CONVERSION`.
- Added Non-AI Ranking Architecture constraint to `docs/architecture/ARCHITECTURE.md` (Section 6) establishing that all 6 discovery/sorting modes and view tracking run via relational SQL queries and scheduled aggregations without AI, recommendation engines, vector databases, or Redis/Kafka.
- Added testing priorities in `docs/testing/TEST-STRATEGY.md` (Section 4) covering step reordering, 0–5 media with single cover validation, unit conversion publish gate rejection, rating authorization and anti-tampering, view deduplication window, and Most Active vs. Trending score calculations.

### Changed

- Updated `docs/requirements/PRD.md` to v1.3.0: expanded core entity baseline to 23 entities, added recipe steps (1–30), media gallery (0–5 images with 1 cover), ratings, view tracking, 6 discovery/sorting modes, and ingredient unit conversion gate.
- Updated `docs/requirements/SRS.md` to v1.3.0:
  - Expanded conceptual baseline to 23 entities in Section 3.3.
  - Updated Section 3.1, 3.4, 3.5, 3.6, 3.7, 3.9, and Question Baseline Section 3.20 (Q01, Q05, Q06, Q07, Q16, Q18, Q20, Q23, Q25).
  - Updated Section 5 Module descriptions (M01, M03, M06).
  - Updated Section 7.2 Functional Requirements Registry (reactivated FR-22; added FR-57, FR-58).
  - Updated Section 8.2 Business Rules Registry (updated BR-14, BR-19, BR-20, BR-48; added BR-69, BR-70, BR-71, BR-72, BR-73).
  - Updated Section 12 Traceability Matrix to map all 23 entities to requirements.
- Updated `docs/requirements/srs/BUSINESS-RULES.md` to v1.3.0: updated BR-14, BR-19, BR-20, BR-48 and added full specifications for BR-69 through BR-73.
- Updated `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md` to v1.3.0:
  - Updated `FR-01` to include recipe rating view-only, view count, 6 discovery modes, cover image, and sequential steps.
  - Updated `FR-04`, `FR-14`, `FR-16`, `FR-17`, `FR-18`, `FR-19`, `FR-20`, `FR-21`, `FR-25`, `FR-39`, `FR-44`, `FR-51`, `FR-54` to incorporate 1–30 steps, 0–5 media with 1 cover, numeric quantities, unit conversion publish validation gate, and rating/view presentation.
  - Reactivated `FR-22` ("Thao tác chỉnh sửa và sắp xếp bước hướng dẫn chuẩn bị/chế biến") from `RETIRED` to `ACTIVE` with full specification for step CRUD and drag-and-drop / numeric reordering.
  - Appended detailed technical specifications for `FR-57` and `FR-58`.
- Updated `docs/requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md` to v1.2.0: updated NFR-02 (indexing for 6 sorting modes), NFR-05 (handling high write throughput for recipe views under 50 CCU), and NFR-10 (security boundaries for steps, media, rating, and view tracking).
- Updated `docs/diagrams/ERD/README.md` to v1.4.0: expanded conceptual data model from 17 to 23 entities (`RECIPE_STEP`, `RECIPE_MEDIA`, `RECIPE_RATING`, `RECIPE_VIEW`, `UNIT`, `INGREDIENT_UNIT_CONVERSION`), removing `recipe_step` and `recipe_media` from the removed entity catalog, and documented the full 37-relationship and cardinality matrix synchronized with `conceptual-erd-v1.0.0.drawio`.
- Updated `docs/diagrams/UseCase/README.md` to v1.1.0: embedded and linked the system-wide use case diagram `usecase-vegetarian-support-application.drawio.png`.
- Updated `docs/architecture/ARCHITECTURE.md` to v1.6.0: updated media storage boundaries (0–5 images, Azure Blob Storage, SQL Server `RECIPE_MEDIA`) and added Non-AI Ranking Architecture.
- Updated `docs/testing/TEST-STRATEGY.md` to v1.4.0: aligned test priorities with step reordering, media constraints, unit conversion validation gate, rating constraints, view deduplication, and discovery mode ranking.

### Reactivated

- Reactivated `FR-22` ("Thao tác chỉnh sửa và sắp xếp bước hướng dẫn chuẩn bị/chế biến") from `RETIRED` back to `ACTIVE` lifecycle state following the team's decision to restore structured, sequential recipe cooking steps (1–30 steps) with drag-and-drop and order manipulation.

## 2026-09-18 — Add C4 Container Guide and Standardize Conceptual ERD Documentation

**Status:** Committed — 5accbaa.

**Scope:** Document the runtime system architecture in `docs/diagrams/C4 Container Diagram/README.md` (v1.0.0) explaining the C4 Container model (Web Application, Spring Boot REST API, SQL Server Database, and Azure Blob Storage) and external cloud services (Gemini, YouTube, Google GIS); refactor `docs/diagrams/ERD/README.md` (v1.2.0) to focus on the project purpose and architectural boundary of the 17-entity conceptual data model; and register the C4 diagram workspace in `docs/README.md` (v3.7.0).

### Added

- Added `docs/diagrams/C4 Container Diagram/README.md` (v1.0.0) providing a comprehensive Vietnamese guide for the C4 Container diagram (Level 2), detailing actors (Guest, Member, Administrator), internal containers, external services, protocol matrices, and trust boundaries.
- Embedded `conceptual-erd-v1.0.0.drawio.png` and linked `conceptual-erd-v1.0.0.drawio` in `docs/diagrams/ERD/README.md`.
- Registered `docs/diagrams/C4 Container Diagram/` in `docs/README.md` (v3.7.0) document register.

### Changed

- Updated `docs/diagrams/ERD/README.md` to v1.2.0, clarifying the role and purpose of the Conceptual ERD in guiding physical database implementation without conjecturing internal relationship details, while synchronizing file naming conventions to `conceptual-erd-v[version]`.
- Updated `docs/README.md` to v3.7.0 registering the maintained C4 Container documentation workspace.

## 2026-09-17 — Realign Conceptual Baseline to 17 Entities and Adopt Feature-based AI Entitlements

**Status:** Committed — 5accbaa.

**Scope:** Realign the entire project documentation suite (PRD, SRS, Functional Requirements, Business Rules, Non-Functional Requirements, Architecture, Test Strategy, and ERD documentation) according to 10 confirmed business decisions: establishing a 17-entity conceptual baseline, replacing daily AI request quotas (5/15/50) with Feature-based Entitlements, applying technical rate limiting for Guest AI chat, consolidating recipe instructions into a single free-form field (retiring FR-22), limiting Recipe Post media to 1 cover image + 0..1 YouTube URL, dropping moderation action entities into direct fields on Report, merging user profile into User, and completely eliminating Like and Unlike capabilities across recipes and comments (retiring FR-45 and BR-65).

### Added

- Documented the authoritative 17 Conceptual Entities baseline in `docs/diagrams/ERD/README.md` (v1.1.0) and `docs/requirements/PRD.md` (v1.2.0): User, User Ingredient Preference, Recipe Post, Category, Recipe Category, Ingredient, Recipe Ingredient, Saved Recipe, Comment, Report, Meal Plan, Meal Plan Entry, Shopping List, Shopping List Item, Subscription, Payment Transaction, Notification.
- Defined explicit Feature-based AI Entitlement tiers in `docs/requirements/SRS.md` and `docs/requirements/srs/BUSINESS-RULES.md`:
  - FREE (0 VND/month): AI Chatbot (`FR-51`) + Basic ingredient-based Recipe Suggestions (`FR-34`).
  - PLUS (49,000 VND/month): FREE rights + AI Recipe Authoring Assistant (`FR-21`) + AI Recipe Variation Suggestion (`FR-47`).
  - PRO (99,000 VND/month): PLUS rights + Automated 7-Day Meal Planner (`FR-36`).
- Documented Technical Rate Limiting (e.g. 10 requests/minute via anonymous cookie + IP) for Guest AI chat interactions in `FR-02`, `FR-51`, `BR-01`, and `NFR-09`.

### Changed

- Updated `docs/requirements/PRD.md` (v1.2.0) to reflect the 17 core entities baseline, single cover image per recipe, feature-based AI entitlements, and removal of all Like interactions.
- Updated `docs/diagrams/ERD/README.md` (v1.1.0) to register the exact 17 conceptual entities and explicitly catalog removed/merged entities (`recipe_step`, `recipe_media`, `recipe_like`, `comment_like`, `moderation_action`, `user_profile`, `ai_usage_record`).
- Updated `docs/requirements/SRS.md` (v1.2.0) across Core Entities (Section 3.3), AI and Media constraints (Sections 3.6, 3.8, 3.9, 3.12, 3.17), Question Baseline table Q01–Q26 (Section 3.20), Functional Modules M03/M06/M08 (Section 5), Functional Requirements Registry (Section 7.2), Business Rules Registry (Section 8.2), and Traceability Matrix (Section 12).
- Updated `docs/requirements/srs/BUSINESS-RULES.md` (v1.2.0):
  - Refactored `BR-01` (Guest/Free AI Chat access & rate limit), `BR-02` (Plus/Pro AI feature entitlement), `BR-03` (Server-side entitlement verification), `BR-04` (Provider error handling and technical telemetry logging), `BR-16` (Authoring flow when AI unavailable or unentitled), `BR-19` (Single cover image & free-form instructions), and `BR-31` (Safety check without quota reference).
  - Formally retired `BR-65` ("Tương tác Like/Unlike đối với Recipe Post, Comment và Reply").
- Updated `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md` (v1.2.0):
  - Updated `FR-01`, `FR-08`, `FR-17`, `FR-23`, `FR-31`, and `FR-38` to completely remove Like buttons, counters, and sorting by most liked.
  - Formally retired `FR-45` ("Like và Unlike bài công thức, bình luận và phản hồi") with permanent anchor `<a id="fr-45"></a>` and full historical description.
  - Updated `FR-04`, `FR-14`, `FR-16`, `FR-20`, and `FR-25` to constrain recipe illustrations to at most 1 cover image (`cover_image_url`) + 0..1 YouTube URL.
  - Updated `FR-02`, `FR-10`, `FR-11`, `FR-13`, `FR-21`, `FR-47`, and `FR-51` to eliminate daily quotas (5/15/50) and 00:00 resets, implementing Feature-based Entitlements and technical telemetry accounting.
- Updated `docs/architecture/ARCHITECTURE.md` (v1.5.0) to reflect the 17-entity conceptual baseline, feature entitlement gating, rate limiting, and single media storage.
- Updated `docs/testing/TEST-STRATEGY.md` (v1.3.0) to align test scenarios with feature-based entitlements, single cover image validation, and removal of like tests.
- Updated `docs/requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md` (v1.1.0) to align technical boundaries and error handling with feature-based entitlement.

### Fixed

- Eliminated stale documentation references across requirements and test artifacts regarding daily request quotas (5/15/50), 00:00 quota resets, multiple recipe photos, and like/unlike interactions.

## 2026-09-17 — Retire FR-22 and Adopt Free-form Recipe Instructions Context

**Status:** Committed — c1929e1.

**Scope:** Refactor Recipe Post functional specifications and business rules across the Modular SRS baseline (SRS.md, BUSINESS-RULES.md, FUNCTIONAL-REQUIREMENTS.md, PRD.md, and ERD guide) to replace the 1–30 cooking steps array and step reordering model with a single free-form instruction context field (`instructions`, 10–5,000 characters, markdown/plain text), officially retiring `FR-22`.

### Changed

- Updated `docs/requirements/SRS.md` to v1.1.0:
  - Redefined Recipe Post cooking instructions from 1–30 sequential steps to a single free-form context field (`instructions`, 10–5,000 characters, trimmed non-blank) in Section 3.7, Section 3.9, and Section 3.20.
  - Renamed `FR-16` to "Biểu mẫu tạo Recipe Post kết hợp thông tin món ăn có cấu trúc và trường nội dung/hướng dẫn tự do" in the requirements registry.
  - Renamed `FR-21` to "Hỗ trợ AI gợi ý và điền nội dung hướng dẫn chế biến tự do trực tiếp vào biểu mẫu tạo/chỉnh sửa Recipe Post".
  - Marked `FR-22` as `RETIRED` in Section 7.2 registry.
- Updated `docs/requirements/srs/BUSINESS-RULES.md` to v1.1.0:
  - Updated `BR-19` to mandate the single `instructions` field (10–5,000 characters, non-blank after trim) and removed the 1–30 step count constraint.
- Updated `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md` to v1.1.0:
  - Updated `FR-04`, `FR-07`, `FR-16`, `FR-20`, `FR-21`, `FR-25`, `FR-40`, `FR-44`, and `FR-51` to replace multi-step management, step arrays, and reordering with the single free-form `instructions` textarea and payload.
  - Formally retired `FR-22` with a detailed Retirement Rationale and historical behavior preserved.
  - Updated `FR-51` Recipe Context generation for Gemini to pass the full `instructions` text rather than a structured step array.
- Updated `docs/requirements/PRD.md` to v1.1.0:
  - Clarified that MVP Recipe Post combines structured metadata with a single free-form instruction field (10–5,000 characters) without sequential step decomposition.
- Updated `docs/diagrams/ERD/README.md` to v1.0.1:
  - Added `instructions` (`NVARCHAR(MAX)`, NOT NULL) to the sample `recipe_post` entity description, removing the need for a separate `recipe_step` table.

### Retired

- Retired `FR-22` ("Thao tác chỉnh sửa và sắp xếp bước hướng dẫn chuẩn bị/chế biến") as recipe instructions are now authored as a continuous free-form context block, eliminating explicit step reordering, moving, and discrete step editing workflows.

## 2026-09-17 — Finalize Technical Stack Baseline and Adopt Mâm Xanh Brand

**Status:** Committed — 2bbf52b.

**Scope:** Finalize the concrete technical stack baseline (Google Identity Services, Brevo SMTP, payOS, Gemini 3.8 Flash, Azure Cloud, GitHub Student Pack tools) and officially rebrand the application to "Mâm Xanh" (Vegetarian Support System) with updated backend artifact coordinates and documentation.

### Added

- Added official Mâm Xanh brand logo to `image/logo.png`.
- Added C4 Container Diagram source model and rendered asset under `docs/diagrams/C4 Container Diagram/`.
- Finalized integration specifications in `docs/architecture/TECHNOLOGY-STACK.md` and `docs/architecture/ARCHITECTURE.md` (v1.4.0):
  - Google Login: Google Identity Services (GIS) on frontend, Google ID Token verification via `GoogleIdTokenVerifier` in backend.
  - Transactional Email: Brevo SMTP via `spring-boot-starter-mail`.
  - Payment Gateway: payOS REST API with VietQR and HMAC-SHA256 signature verification for webhook callbacks.
  - AI Integration: Google Gemini `gemini-3.8-flash` via official Google Gen AI Java SDK, wrapped in backend `AiClient` abstraction with circuit breaker, timeout, and retry handling.
  - Deployment Topology: Azure Static Web Apps (frontend), Azure App Service (backend), Azure SQL Database Serverless, and Azure Blob Storage.
  - Observability: Spring Boot Actuator and Azure Application Insights.
  - GitHub Student Developer Pack developer tooling: Codecov (test coverage reporting), Testmail (automation mailbox testing), Requestly Pro (API mocking/interception), and custom `.tech` domain.

### Changed

- Adopted "Mâm Xanh" brand name across project documentation, including `README.md`, `docs/requirements/PRD.md`, and `docs/requirements/SRS.md`.
- Renamed backend module from `app/vegetarian-system-backend/` to `app/mamxanh-backend/` with Maven coordinates `tech.mamxanh:mamxanh-backend`.
- Refactored backend package structure to `tech.mamxanh`, renaming the main Spring Boot application to `tech.mamxanh.MamXanhApplication`.
- Updated backend documentation in `app/mamxanh-backend/README.md` and repository registers in `docs/README.md` and `AGENTS.md` to reflect the new backend module path.

### Fixed

- Resolved test suite package mismatch and excluded unconfigured `DataSourceAutoConfiguration` and `FlywayAutoConfiguration` for smoke test suite in `MamXanhApplicationTests`.
- Removed stale draw.io backup file artifacts from `docs/diagrams/C4 Container Diagram/`.

## 2026-09-17 — Initial Backend Scaffold and Workflow Setup

**Status:** Committed — 7880900.

**Scope:** Initialize the Java 21 and Spring Boot backend workspace under `app/vegetarian-system-backend/`, add GitHub Actions release source verification, and update workspace documentation and repository registers.

### Added

- Scaffolded the backend application under `app/vegetarian-system-backend/` using Spring Boot 4.1.1, Java 21, and Maven Wrapper (`mvnw`, `mvnw.cmd`).
- Configured backend dependencies for Spring Data JPA, SQL Server driver (`mssql-jdbc`), Flyway migrations (`spring-boot-starter-flyway`, `flyway-sqlserver`), Spring Security, Bean Validation, Spring Web MVC, Springdoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`), and Lombok.
- Added GitHub Actions workflow `.github/workflows/release-source.yml` to enforce that pull requests targeting `main` originate only from `develop`.

### Changed

- Updated Backend Workspace Guide (`app/vegetarian-system-backend/README.md`) with scaffold status, dependency stack, and verified compilation commands.
- Updated repository layout and document registers in `docs/README.md` and `AGENTS.md` to reference `app/vegetarian-system-backend/`.

### Fixed

- None.

## 2026-09-16 — Harden Promptfoo Acceptance Checks

**Status:** Working tree — not committed.

**Scope:** Correct evaluator safety checks and evidence semantics while preserving external isolated SUT workspaces and existing business requirements.

### Added

- Added focused regression coverage for child-process GitHub CLI interception, authorization artifacts, HTTPS probes, executable scanning, and secret-read observability.
- Added a reproducible dependency manifest and lockfile pinned to Promptfoo 0.123.0.

### Changed

- Split HTTPS and ICMP preflight probes, require execution artifacts, and provide a preflight-only runner option that cannot start real acceptance cases.
- Expand static executable scanning across agent scripts, evaluation code, and skill scripts while excluding generated dependencies.
- Report A10 secret exposure separately from unobservable secret reads; make the results viewer optional.

### Fixed

- Pass the SUT-first PATH explicitly to the real provider and fail closed when child-shell GitHub CLI resolution is not the local stub.
- Fail A05/A06 on blocked remote-write attempts; grade A07 against exact approved operations and consistent mutation history instead of unconditional success.

## 2026-09-16 — Standardize English for Changelog and Commit Messages

**Status:** Working tree — not committed.

**Scope:** Establish English as the mandatory language for all new or edited `CHANGELOG.md` content and all Git commit messages while retaining Vietnamese as the default prose language for other internal development documentation.

### Added

- Added the canonical English-only exception for changelog entries and Git commit subjects, bodies, and contributor-authored footers to the Documentation Language Policy.
- Added concise enforcement guidance and English Conventional Commit examples to `CONTRIBUTING.md` and agent instructions to `AGENTS.md`.

### Changed

- Converted the current working-tree changelog entries dated 2026-09-16 to English so they comply with the new policy.

### Fixed

- Removed Vietnamese commit-message examples that conflicted with the new repository rule.

## 2026-09-16 — Finalize Requirements Baseline v1.0.0

**Status:** Working tree — not committed.

**Scope:** Standardize and promote the PRD and Modular SRS document set to the active Requirements Baseline v1.0.0, including synchronized metadata, document registration, and downstream design and verification guidance.

### Added

- Defined post-baseline change control: semantic changes require review, FR/BR/NFR impact analysis, and an appropriate document-version update.
- Clarified how the approved baseline feeds ERD/API design and traceable Test Cases derived from Acceptance Criteria.

### Changed

- Promoted `PRD.md`, root `SRS.md`, `FUNCTIONAL-REQUIREMENTS.md`, `BUSINESS-RULES.md`, and `NON-FUNCTIONAL-REQUIREMENTS.md` to `v1.0.0` with document status `Active`.
- Synchronized Requirements Baseline v1.0.0 status in the repository document register and project README.
- Classified unresolved payment/email providers, token details, UI choices, nutrition reference data, and timeout/retry policies as non-blocking design or implementation work.

### Fixed

- Removed stale wording that claimed FR-level Acceptance Criteria remained incomplete even though every active FR already has Acceptance Criteria.
- Standardized M12 to the single lifecycle state `OUT_OF_SCOPE`, removing ambiguous “`OUT_OF_SCOPE` or `DEFERRED`” wording.

## 2026-09-16 — Remove Google Maps Restaurant Discovery from the Implementation Baseline

**Status:** Working tree — not committed.

**Scope:** Reclassify the entire M11 restaurant-discovery capability from `DEFERRED` to `OUT_OF_SCOPE` because it does not serve the core meal-planning flow and the application does not intend to manage or verify external restaurant data.

### Added

- Added a re-entry gate requiring a new scope decision and requirement decomposition before M11 can be reconsidered.

### Changed

- Reclassified `FR-42`, `FR-43`, `BR-55`–`BR-57`, `BR-63`, `BR-67`, and `BR-68` as `OUT_OF_SCOPE` while preserving stable IDs and historical descriptions.
- Removed Google Maps from the current dependency, architecture, and test baselines across README, PRD, SRS, Architecture, Technology Stack, and Test Strategy.

### Fixed

- Replaced stale wording that implied a later M11 implementation commitment with an explicit no-implementation baseline boundary.

## 2026-09-15 — Unified AI Chatbot Model & Recipe Instruction Step Baseline Alignment

**Status:** Working tree — not committed.

**Scope:** Unify AI Chatbot into a single multi-capability assistant under FR-51 per approved Product Decision, align AI Chatbot performance with NFR-03, expand NFR-25 AI quality and compliance evaluation, clarify BR-51 authority boundaries, enforce mandatory 1–30 cooking steps across Recipe Post requirements, and update Root SRS Draft status definition.

### Added

- Unified multi-capability AI Chatbot specification in `FR-51` with context-driven execution across General Culinary & Nutrition Context (`UC-51.1`) and Recipe Context (`UC-51.2`), supporting culinary Q&A, nutrition/calorie explanations, ingredient substitutions, and step-by-step cooking clarifications without fragmentation.

### Changed

- Re-scoped `FR-02` to strictly govern Guest trial quota (5 queries/day) and authentication boundary while delegating conversational behavior to `FR-51`.
- Normalized AI Chatbot performance authority to `NFR-03` ($\le 5$s normal response, P90 $\le 7$s under high load/concurrency); eliminated stale $\le 4$s timeouts and decoupled database query performance (`NFR-07`) from AI response time.
- Expanded `NFR-25` to cover AI content quality and compliance, and widened its scope/threshold ($\ge 80\%$) to cover all AI-generated outputs (recommendations, related recipes, and Chatbot) for source boundaries, dietary/restriction constraints, non-fabrication, and safety rules.
- Enforced mandatory 1–30 instruction steps for Recipe Post across `SRS 3.1`, `3.3`, `3.6`, `BR-19`, `BR-20`, `FR-16`, `FR-21`, and `FR-22`; eliminated stale optional-step wording and removed unapproved per-step separate illustration images from `FR-22`.
- Updated definition of `Status: Draft` in `docs/requirements/SRS.md` to reflect that Phase-2 decomposition is complete and the baseline is pending Project Owner/team review and freeze, explicitly decoupling Document Status from Requirement Lifecycle.

### Fixed

- Fixed semantic misuse of `BR-51` in `FR-51`: stated functional non-mutation of user business data directly within `FR-51` and restricted `BR-51` citations strictly to its authoritative domain (AI cannot create or modify official nutrition catalog values).
- Removed implementation leakage (HTTP status codes, auto-scroll UI, client form state mechanics) from requirements prose.

## 2026-09-15 — Cross-Document Corrective Pass & Dual-Layer Validation Alignment

**Status:** Committed — 5b1abfe.

**Scope:** Resolve remaining cross-document inconsistencies identified during full Phase-2 requirements audit, enforce Owner-only access for private user data, align authentication with HttpOnly refresh cookies, clean up system mechanism Use Cases, and clarify dual-layer validation without server-side drafts.

### Added

- Dual-layer validation execution and draft boundary specifications in `BR-19`, `FR-16`, and `FR-25`, explicitly separating mandatory server-side rejection (`HTTP 400 Bad Request` with no DB persistence) from client-side in-memory form state retention for inline user correction.
- Detailed architecture rationale in `FR-24` (`OUT_OF_SCOPE`) documenting why incomplete server drafts and orphan asset persistence are excluded from the MVP.

### Changed

- Aligned authentication flows in `FR-03` with `TECHNOLOGY-STACK.md`: Refresh Token is issued, rotated, and expired exclusively via Secure HttpOnly SameSite Cookie inaccessible to client-side JavaScript; server manages session family revocation upon logout and reuse detection.
- Enforced strict `Owner-only by default` authorization in `FR-23` for all personal private resources (nutrition profile, health/BMI metrics, dietary preferences, saved recipes, meal plans, shopping lists); eliminated generic `isOwner || isAdmin` bypass logic and restricted Administrator access to minimal resource-specific administrative fields (`accountId`, email, account status, moderation logs) authorized by BR/NFR.
- Restricted cooking step reordering and editing in `FR-21` and `AC-23.10` to the recipe author per `BR-64`, keeping Administrator actions confined to independent content moderation (hide/remove).
- Reclassified automated internal background actions (`UC-11.1` AI token telemetry recording, `UC-11.3` 90-day retention purge, `UC-14.3` image resource lifecycle cleanup) from standalone Use Cases into System Behaviors and Cleanup Flows in `FR-11` and `FR-14`, preserving 100% of testable Acceptance Criteria (`AC-11.1`, `AC-11.4`, `AC-14.3`).
- Standardized Primary Actor in `FR-54` as `Member` (shopper needing consolidated ingredients) with the aggregation engine designated as a supporting system mechanism.
- Reclassified technical implementation specifics to non-blocking categories: domestic payment gateway to `TECHNICAL_DESIGN_TBD` and exact Gemini model variant to `TECHNICAL_SPIKE_REQUIRED`, with all Phase-1 business boundaries locked.
- Clarified the external Google Maps dependency in `TEST-STRATEGY.md` with the deferred-module condition that it applied only if M11 was activated later.

### Fixed

- Fixed contradictory wording in `FR-03` that previously suggested tokens were returned in application payloads and cleared by client-side storage manipulation.
- Fixed ambiguous administrative permissions in `FR-21` and `FR-23` that risked leaking member private personal data or allowing admin direct modification of author cooking steps.

## 2026-09-14 — Phase-2 Functional Requirement Semantic Repair & Full Decomposition

**Status:** Committed — 8db98d6.

**Scope:** Complete Phase-2 requirement decomposition and full semantic repair across all 56 Functional Requirements in `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md`. Restored authoritative Phase-1 business decisions, quotas, pricing, validation profiles, and security boundaries while preserving deep decomposition for all 48 ACTIVE requirements.

### Added

- Full Phase-2 decomposition for all 48 ACTIVE Functional Requirements (`FR-01` to `FR-04`, `FR-06` to `FR-11`, `FR-13` to `FR-23`, `FR-25` to `FR-41`, `FR-44` to `FR-49`, `FR-51`, `FR-53` to `FR-55`) with 131 Use Cases, detailed main/alternative/exception event flows, permission rules, and 248 atomic Given/When/Then Acceptance Criteria.
- Comprehensive semantic traceability mapping linking each active FR to corresponding Business Rules (`BR-01`–`BR-68`) and Non-Functional Requirements (`NFR-01`–`NFR-27`).
- Three-tier credential and data visibility boundary in `FR-23` separating Public Profile, Private Account/Profile Data, and Security / Credential Internals under least-privilege principles.
- Single Source of Truth (SSOT) delegation for Recipe Validation Profile in `FR-16`, referenced consistently by `FR-25`, `FR-40`, and `FR-44`.

### Changed

- Restored authoritative AI quotas across all tiers: Guest 5/day, Free Member 5/day, Plus Member 15/day, Pro Member 50/day; reset at 00:00 `Asia/Ho_Chi_Minh` (GMT+7); provider failures do not consume quota.
- Aligned subscription and payment terms in `FR-13`: Free 0 VND, Plus 49,000 VND/month, Pro 99,000 VND/month; monthly prepaid cycle, no auto-renewal, no partial refund, verified activation, and idempotent payment processing.
- Relocated misplaced decomposition content to authoritative owners: Comments to `FR-46`, Likes/Favorites to `FR-45`, Chatbot to `FR-51`, Recipe Card/Details to `FR-17`/`FR-20`, and Shopping List Export to `FR-55`.
- Normalized login rate-limiting behavior in `FR-03`: 10-minute temporary rate limit after 5 consecutive failures enforced across both account identifier and source IP without converting the account to administrator `LOCKED` state.
- Standardized password hashing terminology to BCrypt (minimum work factor 10) in `FR-03`.
- Preserved frozen historical scope without implementation scope for the 8 non-ACTIVE requirements: `FR-05` (RETIRED), `FR-12` (DEFERRED), `FR-24` (OUT_OF_SCOPE), `FR-42` (DEFERRED), `FR-43` (DEFERRED), `FR-50` (OUT_OF_SCOPE), `FR-52` (OUT_OF_SCOPE), and `FR-56` (OUT_OF_SCOPE).

### Fixed

- Removed all unauthorized/drifted terms, including fabricated "Premium" plans, 100-request quotas, and obsolete subscription prices (59k/149k/499k VND).
- Fixed misleading Free AI quota call-to-action in `FR-02` that incorrectly implied Free tier offered 15 requests/day.
- Fixed mandatory cooking step contradictions in `FR-07`, `FR-25`, `FR-40`, and `FR-44`, ensuring cooking steps remain optional (0 to 30 steps) per `FR-16` and SRS 3.9.
- Eliminated obsolete validation limits (5–100 title characters, 1–20 servings) in `FR-25`.
- Repaired corrupted LaTeX formatting control characters (`\frac`, `\times`, `\rightarrow`) across all requirement blocks.

## 2026-09-14 — Phase-1 Decision Baseline Synchronization

**Status:** Committed — `ad9e004` (verified documentation checkpoint).

**Scope:** Synchronize the DEC-001–DEC-018 corrective baseline across the requirement set and affected documentation while preserving stable IDs and history, without creating GitHub Issues or starting Phase-2 decomposition.

### Added

- Recorded the MVP commercial baseline: FREE 0, PLUS 49,000, and PRO 99,000 VND/month; monthly billing, no automatic renewal, no partial refund, verified activation, end-of-period expiry, and idempotent duplicate processing.
- Recorded the authentication baseline using short-lived access tokens, rotating refresh tokens, refresh-session/server-side revocation, and logout revocation.
- Preserved `DEC-016 — A — APPROVED`: **Backend Architecture: Modular Monolith using MVC/layered structure within each business module.**

### Changed

- Standardized lifecycle states for 56 FRs, 68 BRs, and 27 NFRs; moved M11 and its related FRs/BRs to `DEFERRED` while preserving `OUT_OF_SCOPE` and `RETIRED` items.
- Synchronized Recipe Post validation, Shopping List unit conversion, tombstone/reference preservation, manual moderation, comment depth, notifications, AI quota/telemetry, nutrition, subscriptions, security, NFR performance, and curated AI evaluation.
- Synchronized SRS/FR/BR/NFR, PRD, README, Architecture, Technology Stack, and Test Strategy without changing code, schema, API, or GitHub work items.

### Fixed

- Removed legacy or unknown lifecycle wording and conflicts that still described M11 as MVP/Pending Review, subscription pricing or JWT lifecycle as undecided, or NFR design goals as MVP release gates.

## 2026-09-14 — NFR Decomposition & ISO/IEC 25010 Quality Categorization

**Status:** Working tree — not committed.

**Scope:** Standardize and decompose 27 non-functional requirements (`NFR-01` through `NFR-27`) into six core project quality categories based on ISO/IEC 25010, including quantitative criteria and confirmed architecture boundaries.

### Added

- Decomposed all 27 non-functional requirements (`NFR-01` through `NFR-27`) in `docs/requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md` (v0.2.0), including Metric, Threshold, Verification Method, and stable `<a id="nfr-xx"></a>` anchors.
- Added explicit classification and exclusion boundaries:
  - Recorded `NFR-05` with a 500-CCU design goal and a practical project acceptance range of 50–100 CCU.
  - Recorded the `NFR-08` recommendation not to apply AES-256 database-column encryption to health/BMI indicators to avoid unnecessary database complexity.
  - Marked `NFR-26` (AI content review/flagging) and `NFR-27` (TikTok For You-style behavioral personalization) as `OUT_OF_SCOPE`.
  - Removed the proposed AI microservice and chatbot-call quota from the NFR set.

### Changed

- Upgraded `docs/requirements/SRS.md` to `v0.49.0` and updated the Section 9 summary table to link directly to six quality groups and all 27 detailed NFR identifiers.

### Fixed

- None.

## 2026-09-14 — Modular SRS Architecture & Field-Specific Authority Model

**Status:** Working tree — not committed.

**Scope:** Restructure the Software Requirements Specification into a multi-file Modular SRS, separating the root `SRS.md` authoritative registry from three specialized detailed specifications: `FUNCTIONAL-REQUIREMENTS.md`, `BUSINESS-RULES.md`, and `NON-FUNCTIONAL-REQUIREMENTS.md`.

### Added

- Added three detailed specifications under `docs/requirements/srs/`:
  - `FUNCTIONAL-REQUIREMENTS.md`: authoritative detailed definitions for 56 Functional Requirements (`FR-01` through `FR-56`), stable `<a id="fr-xx"></a>` anchors, and preserved baseline wording and lifecycle-gate notes.
  - `BUSINESS-RULES.md`: authoritative detailed definitions for 68 Business Rules (`BR-01` through `BR-68`) with stable `<a id="br-xx"></a>` anchors.
  - `NON-FUNCTIONAL-REQUIREMENTS.md`: authoritative detailed definitions for six NFR categories (Security, Privacy, Reliability, Performance, Usability, Auditability), category anchors such as `<a id="nfr-category"></a>`, and preserved `TBD`/`OPEN` fields without inventing `NFR-xx` identifiers.
- Added the `Lifecycle Synchronization Drift` finding type to the `repo-template-doc-sync-auditor` skill.

### Changed

- Upgraded `docs/requirements/SRS.md` to `v0.48.0`; converted Sections 7 and 8 to five-column Option A indexes (`ID | Short Name | Module | Lifecycle | Detail`) acting as the Authoritative Registry for existence, identifiers, module allocation, and lifecycle, and converted Section 9 into a linked NFR summary.
- Updated documentation and agent governance in `AGENTS.md` (v3.3.0), `docs/README.md` (v3.2.0), and `.agents/repo-contract.yml` to recognize the Modular SRS, Field-Specific Authority Model, and two-step scoped-reading procedure.
- Synchronized the internal `markdown-documentation`, `srs-to-github-issues`, and `repo-template-doc-sync-auditor` skills for Modular SRS support, two-phase issue resolution, and bidirectional consistency audits.

### Fixed

- None.

## 2026-09-14 — Personal Pantry & Inventory-based Recommendation Scope

**Status:** Working tree — not committed.

**Scope:** Decompose Personal Pantry/Inventory management, pantry-based recipe or post discovery, and AI-assisted recipe creation from available ingredients; classify the capability as `OUT_OF_SCOPE` for the initial MVP.

### Added

- Added SRS Section 3.22 decomposition for Member Personal Pantry management, ingredient-match recipe discovery (100% match or a small number of missing items with Shopping List suggestions), and Gemini-assisted vegetarian recipe creation from pantry ingredients.
- Added Module M13 (`Personal Pantry & Inventory-based Recipe Recommendation`) and FR-56 with lifecycle `OUT_OF_SCOPE` for the initial MVP.

### Changed

- Synchronized the out-of-scope sections in PRD Section 5 and SRS Section 3.2 to clarify the future M13/FR-56 decomposition.

### Fixed

- None.

## 2026-09-14 — Basic Shopping List MVP Adoption

**Status:** Working tree — not committed.

**Scope:** Move the basic Shopping List capability into the official MVP under Module M05 with three functional requirements (`FR-53`, `FR-54`, `FR-55`) and safe ingredient-aggregation rules.

### Added

- Added FR-53 (Shopping List management, ad-hoc items, purchased checkbox), FR-54 (safe aggregation for matching ingredient IDs and units, grouped by store section), and FR-55 (clipboard copy and `.txt` export) to SRS Section 7.2 with lifecycle `ACTIVE`.

### Changed

- Integrated Shopping List into Module M05 and renamed the module to `Saved Recipes, Meal Planning & Shopping List`.
- Removed Shopping List from the out-of-scope lists in SRS Section 3.2 and PRD Section 5; clarified that advanced pantry, multi-user sharing, real-time synchronization, AI unit conversion, and PDF export remain outside the MVP.

### Fixed

- None.

## 2026-09-14 — Restaurant Discovery Review and True Blog Scope

**Status:** Working tree — not committed.

**Scope:** Move vegetarian restaurant discovery to lecturer review, decompose a Samsung Food-style long-form Blog with embedded Recipe Cards, and classify the Blog capability outside the initial MVP.

### Added

- Added SRS Section 3.21 decomposition for the Blog Post entity, long-form Rich Text content, Embedded Recipe Cards, and save/schedule interactions from a blog post.
- Added FR-52 and Module M12 for the Blog capability with lifecycle `OUT_OF_SCOPE` for the initial MVP.

### Changed

- Moved Module M11, FR-42, FR-43, and Google Maps Platform integration to `PENDING_REVIEW` pending lecturer guidance.
- Updated the PRD, System Architecture, and Technology Stack to synchronize the Google Maps review state and define the Blog boundary outside the MVP.

### Fixed

- None.

## 2026-09-14 — Documentation Language Policy

**Status:** Working tree — not committed.

**Scope:** Establish Vietnamese as the primary prose language for internal development documentation, preserve necessary English technical terms, and require fully English content only for documents explicitly classified as academic submissions.

### Added

- Added the authoritative documentation-language policy to the maintained document register.

### Changed

- Linked `AGENTS.md` and `CONTRIBUTING.md` to the authoritative policy without duplicating the full content.
- Standardized Architecture, Technology Stack, Test Strategy, and workflow-evidence documentation to Vietnamese prose while preserving technical meaning and established English terms.

### Fixed

- Removed the previous assumption that technical or governance documents default to English solely because their document type has an English name.

## 2026-09-13 — Controlled Documentation Baseline Migration

**Status:** Working tree — not committed.

**Scope:** Separate product requirements, software requirements, architecture, technology selection and verification strategy into maintained sources with explicit ownership, while preserving existing requirement meaning and holding unresolved lifecycle assignments for decision.

### Added

- Maintained System Architecture, Technology Stack and Test Strategy documents with explicit evidence limits and unresolved implementation choices.
- Evidence-based creation triggers for future Development, Setup and API documents.

### Changed

- Migrated current product rationale, actor/onboarding context, AI boundaries and technology decisions into their authoritative destinations.
- Updated repository navigation, agent routing and the machine-readable contract to use the new architecture, technology and testing paths.
- Retired and deleted the four superseded migration sources after their unique current information and active references had been migrated and deletion was explicitly approved.

### Fixed

- Corrected stale summary and research statements that treated Google Maps restaurant discovery as unconfirmed or described the retired permission-application workflow as current.
- Added an explicit lifecycle decision gate so legacy phrases indicating a finalized decision are not silently converted to `ACTIVE`.

## 2026-09-13 — Recipe Publishing and AI Scope Simplification

**Status:** Working tree — not committed.

**Scope:** Align the requirements baseline with the team's confirmed simplification of Recipe Post publishing and deferred AI features.

### Added

- None.

### Changed

- Members publish valid Recipe Posts directly; the permission-application workflow is retired and Administrator moderation is report-driven.
- Marked persistent Recipe Post drafts and per-account AI chat history as out of scope for the current MVP.
- Marked AI content scanning/flagging as deferred rather than an MVP acceptance requirement.
- Clarified FR-35's eligibility, input/output, and non-diagnostic boundary; separated FR-49 notifications from report-identity privacy in FR-29/BR-28.

### Fixed

- Removed conflicting summary references to pre-publication permission approval and persistent drafts in the directly affected requirement documents.

**Related PR:** None.
## 2026-09-13 — Changelog Format and Contribution Authority

**Status:** Working tree — not committed.

**Scope:** Standardize the English project history and separate shared contribution rules from AI-specific instructions.

### Added

- A dated, topic-based changelog format and reference template in CONTRIBUTING.md.

### Changed

- Converted the previous accumulating history into dated topics with Added, Changed and Fixed sections, explicit status and scope.
- Made CONTRIBUTING.md the canonical contribution workflow; AGENTS.md now directs agents there before repository changes and retains agent-specific routing, context and authorization instructions.
- Moved shared editing guidance out of AGENTS.md; aligned the repository contract and changelog skill with the same authority.
- Removed changelog tutorials and illustrative release history from this file; preserved actual historical decisions and distinguished commit checkpoints from uncommitted work.

### Fixed

- Resolved the mismatch between the requested daily format and the changelog skill's generic Unreleased format.

## 2026-09-12 — Repository Layout and Agent Onboarding

**Status:** Working tree — not committed.

**Scope:** Establish predictable document placement and targeted onboarding for new agents.

### Added

- A maintained document register with placement rules, document purposes and read triggers.
- Task-based source and skill routing, including boundaries for unregistered files.

### Changed

- Moved product-direction and AI-plan decomposition notes from the root into requirements documentation.
- Kept GitHub Issues/Projects as the progress source and linked PRs as review/test evidence.
- Removed completed SRS questionnaires, the superseded SRS template and obsolete local audit reports; retained product decisions in SRS section 3.20.
- Returned audit findings in the conversation unless an export is requested; stopped automatic report generation and registration of scratch artifacts.

### Fixed

- Removed ambiguous placement and automatic-reading expectations for unregistered documents.

## 2026-09-12 — Project Documentation and Five-Member Team Setup

**Status:** Working tree — not committed.

**Scope:** Adapt repository guidance to the Vegetarian Support System and the five-member SWP391 team.

### Added

- ADR-002 documenting owner/reviewer responsibilities, work-in-progress limits, review, decision-making and contribution evidence.
- Consistency and metadata audits; their initially generated local reports were subsequently removed during the layout cleanup above.

### Changed

- Personalized README, PRD and frontend/backend/database workspace guides while distinguishing documentation plans from the unimplemented application.
- Standardized maintained-document metadata and aligned app/ paths, .agents/skills/ paths and issue-number branch naming.

### Fixed

- Replaced machine-specific file links and removed actor/template descriptions that contradicted the current SRS.

## 2026-09-12 — Consolidated Product Scope and Traceability

**Status:** Committed — `ccd6def` (verified documentation checkpoint).

**Scope:** Record the confirmed questionnaire decisions and align supporting product documents.

### Added

- A 38-question SRS decision questionnaire with recommended choices, used during scope clarification and later removed after decisions were consolidated.
- FR-47 through FR-51 and the associated traceability table in SRS v0.43.0.

### Changed

- Consolidated Q01–Q38: Free/Plus/Pro share the same functions; real payments, comment reports/Likes, email and in-app notifications, and road-distance map results are included in the documented scope.
- Recorded sources for Q31 while deferring formula research to implementation; treated Q35 as team-selected scope without an instructor-confirmation prerequisite.
- Synchronized questionnaire answers, actors, product direction, AI plans and benchmark documents.

### Fixed

- None.

## 2026-09-12 — Recipe Publishing and Community Decisions

**Status:** Committed — `ccd6def` (verified documentation checkpoint).

**Scope:** Preserve the recipe and moderation decisions recorded across SRS revisions.

### Added

- Duplicate-report prevention in SRS v0.19.0.

### Changed

- SRS v0.21.0 replaced general blogs with a single Recipe Post content type and made step-by-step cooking instructions optional.
- SRS v0.34.0 replaced per-recipe approval with publishing-permission applications: authorized Members publish valid posts directly; Administrators moderate reported content and record reasons for sanctions. AI sources became public posts that are not hidden or deleted.
- SRS v0.35.0 required policy acknowledgement, contribution intent, responsibility and sanction awareness in applications, without professional certificates or experience.
- SRS v0.36.0 allowed rejected applications to be revised and resubmitted with rejection reasons, limited each Member to one pending application, and required reasons for abuse restrictions without a fixed MVP cooldown.
- SRS v0.37.0 allowed published recipes to be edited without reapproval; v0.38.0 clarified authors' create/read/update/delete rights and prevented authors from restoring Administrator-hidden posts.
- SRS v0.39.0 defined one reversible Like/Upvote per Member per post, Guest visibility of totals and no five-star rating system.
- SRS v0.40.0 defined nested replies, Guest reading, Members managing their own comments/replies, and Administrator moderation.

### Fixed

- None.

## 2026-09-12 — Onboarding, AI Selection and Meal Planning

**Status:** Committed — `ccd6def` (verified documentation checkpoint).

**Scope:** Clarify personalization prerequisites and the relationship between saved recipes, AI and meal plans.

### Added

- None.

### Changed

- SRS v0.20.0 made onboarding skippable but required three minimum profile groups before personalized AI recipe suggestions or menu generation.
- SRS v0.22.0 separated saved recipes from meal planning, kept personal data Member-only and excluded Queue from MVP.
- SRS v0.23.0 fixed the meal types to breakfast, lunch and dinner, without snacks or custom meal types.
- SRS v0.24.0 allowed multiple recipes per meal without a hard cap, while preventing duplicate Member/date/meal/recipe entries.
- SRS v0.25.0 restricted AI to source recipes and required source links instead of invented recipes. Its original approved-post wording was superseded by the public, non-hidden/non-deleted source rule in v0.34.0.

### Fixed

- None.

## 2026-09-12 — Nutrition Scope and Catalogue Ownership

**Status:** Committed — `ccd6def` (verified documentation checkpoint).

**Scope:** Define nutrition reference behavior and its supported users and data sources.

### Added

- None.

### Changed

- SRS v0.26.0 treated BMI as a reference within the broader nutrition profile and added nutrition-based AI menus and daily status/difference checks without diagnosis.
- SRS v0.27.0 limited nutrition-needs, AI nutrition menus and nutrition checks to Members aged 18 or over who are not pregnant, breastfeeding or requiring therapeutic diets; ordinary features remain available.
- SRS v0.28.0 selected nine MVP nutrition metrics with per-serving presentation and personal daily references, excluding an overall balance score and Glycemic Index/Glycemic Load.
- SRS v0.29.0 based recipe nutrition on ingredient quantities, used servings to allocate recipe totals and planned intake, and summed all recipes across the day's three meals.
- SRS v0.30.0 selected a fixed database nutrition catalogue with USDA FoodData Central as the primary reference, without runtime API calls or AI matching. Unsupported ingredients do not block posting but make nutrition results incomplete.
- SRS v0.31.0 assigned catalogue ownership to Administrators; v0.32.0 defined search, create, edit, enable/disable, related-recipe inspection and source recording, excluding permanent deletion, bulk import, USDA calls and AI autofill.

### Fixed

- None.

## 2026-09-12 — Address-Based Restaurant Discovery

**Status:** Committed — `ccd6def` (verified documentation checkpoint).

**Scope:** Align the restaurant-discovery requirements and technology baseline.

### Added

- None.

### Changed

- SRS v0.33.0 included vegetarian-restaurant discovery around an entered address through Google Maps Platform, not GPS or Gemini, and documented Geocoding/Places dependencies, failure states and open UX/cost decisions.
- SRS v0.37.0 made discovery independent of searched recipes and excluded restaurant-profile management.
- SRS v0.41.0 restricted discovery to authenticated Members and rejected Guest requests before consuming external quota.
- SRS v0.42.0 limited radii to 500 m, 1 km, 5 km and 10 km, rejecting other values before external calls.
- Technology Stack v2.0.0 replaced v1.0.0: entered addresses and Google Geocoding/Places replaced Browser Geolocation and an internal SQL Server restaurant/store catalogue.

### Fixed

- None.

## 2026-09-12 — Topic 03 Import and Workflow Checkpoint

**Status:** Committed — `86bf864` (verified documentation checkpoint).

**Scope:** Preserve imported project documentation and previously undated workflow history. For the workflow items, this is the verified repository checkpoint date, not a claimed original change date.

### Added

- Imported five Topic 03 decomposition documents and retained the previous SRS template for reference at that time; the template was later removed during the documented cleanup.
- ADR-001 and WORKFLOW-SOURCES to distinguish platform behavior, framework guidance, complementary practices and team conventions.
- Definition of Ready for the transition from Planning to In Progress.

### Changed

- Documented develop/main delivery, 4–5-day tasks, PR review, release checklists and Story Points for workload planning.
- Defined Target Date as technical completion and feature merge into develop; Review includes the wait for release to main without treating that wait as owner lateness.
- Linked README and CONTRIBUTING to the five-status workflow and standardized agent, skill and output paths under .agents/.
- Aligned Issue tooling with Story Points 1, 2, 3, 5 and 8, removed Size, and required 8-point Issues to be split before assignment.
- Aligned six Project Types with primary labels and Issue Forms; classified NFR as primary Task plus secondary NFR label.
- Reduced Issue bodies to seven sections, separated Draft State from Project Status, and allowed owner/dates to remain TBD until work starts.
- Defined the develop merge gate around traceability, Acceptance Criteria, verification evidence, review and relevant documentation/database updates.
- Distinguished Refs links on feature PRs from Closes links on release PRs.
- Defined the main release gate: scope, build/tests, integrated demo flows, database, documentation, two approvals and no branch-protection bypass.
- Added post-merge smoke testing, reopening affected Issues, creating Bugs and stopping further releases pending a revert/hotfix decision after failure.

### Fixed

- None.
