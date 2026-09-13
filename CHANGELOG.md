> **Document:** Changelog  
> **File:** `CHANGELOG.md`  
> **Version:** v2.0.0  
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-13  
> **Status:** Active  

# Changelog

Notable project changes, grouped by date and topic. Writing rules are maintained in [CONTRIBUTING.md](CONTRIBUTING.md#changelog-format). Documentation decisions below describe scope, not implemented or deployed features.

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
