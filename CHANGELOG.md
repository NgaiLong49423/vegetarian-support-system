> **Document:** Changelog
> **File:** `CHANGELOG.md`
> **Version:** v2.127.0
> **Created:** 2026-06-14
> **Last Updated:** 2026-10-30
> **Status:** Active

# Changelog

Notable project changes, grouped by date and topic. Writing rules are maintained in [CONTRIBUTING.md](CONTRIBUTING.md#changelog-format). Documentation decisions below describe scope, not implemented or deployed features.

## 2026-10-30 — Restore Meal Plan Focus After Moving an Entry

**Status:** Committed — `4b143d74`.

**Scope:** Keep keyboard focus in a usable location when a meal-plan dialog closes after its trigger becomes hidden.

### Added

- None.

### Changed

- None.

### Fixed

- Return focus to the meal-plan heading when the dialog's original trigger is still in the DOM but no longer visible.

## 2026-10-30 — Align CI Checks for Admin Account Management

**Status:** Committed — `3ec1ed3c`.

**Scope:** Keep the generated API contract and Frontend coverage test aligned with the Admin account-management page.

### Added

- None.

### Changed

- Refresh the OpenAPI snapshot from the runtime contract generated for the pull request commit.
- Update the Admin page coverage assertion to match the current heading.

### Fixed

- None.

## 2026-10-30 — Implement Backend-Backed Admin Account Management

**Status:** Committed — `8632da92`.

**Scope:** Replace the account-management preview with protected SQL Server-backed account lookup and administration.

### Added

- Add paginated account search, global counts, minimal account details, and an append-only account audit history for Administrators.
- Add reason-required account status and CUSTOMER/EXPERT role changes. Direct Expert grants close any pending application and notify its applicant in the same transaction.
- Add Frontend coverage for API-backed account search, role/status changes, and read-only Administrator rows.

### Changed

- Remove sample accounts, account export, and the preview banner from the account-management page; retain the separate transaction preview.
- Clarify FR-05/FR-06 and related business rules for direct Expert grants and cancellation of pending applications.

### Fixed

- None.

## 2026-10-29 — Let Recipe AI Suggest Additional Ingredients

**Status:** Committed — `540998c5d2ca8c8b7765d1efc328ed629ae81dcd`.

**Scope:** Align FR-21, the API guide and recipe drafting with author-reviewed AI suggestions instead of rejecting generated text that mentions an ingredient outside the form's selected list.

### Added

- None.

### Changed

- Ask recipe drafting AI to prioritize entered ingredients while allowing suitable additional suggestions for the author to review and edit before posting.
- Remove Backend rejection based only on generated ingredient mentions; keep existing response-format, entitlement, and publication checks. Published recipes remain subject to current post-moderation.

### Fixed

- None.

## 2026-10-29 — Add Nguyen Hoang Phuc Dessert Demo Collection

**Status:** Committed — `a430530b0a8e91ca40e3cdd525fea1734f3221f6`.

**Scope:** Expand the local Expert demo with a detailed vegetarian collection of desserts, drinks and baked sweets.

### Added

- Add a local-only repeatable seed for 15 recipe posts with professional descriptions, step-by-step instructions, gram quantities and catalog ingredients prioritized for lower usage in existing demo fixtures.
- Generate and attach one distinct food illustration per post, served from Frontend public assets.

### Changed

- Document the new Expert demo collection and local seed behavior.

### Fixed

- None.

## 2026-10-28 — Remove Semicolons from the Recipe Comparison Text ([PR #195](https://github.com/NgaiLong49423/vegetarian-support-system/pull/195))

**Status:** Committed — `f7abbe11`.

**Scope:** Apply the lecturer's feedback that text shown on the website does not use semicolons to the FR-60 recipe comparison page.

### Added

- Add a Playwright check that the text on the comparison page contains no semicolon.

### Changed

- Split the three comparison notes joined by a semicolon (ingredient alignment, nutrition difference and estimate disclaimer) into separate sentences without changing their meaning.

### Fixed

- None.

## 2026-10-28 — Track Recipe Sources and Item Provenance in Shopping Lists

**Status:** Committed — `2d04630c3cef07fb79e1a5fda3d2c0575c60a8f1`.

**Scope:** Show recipes contributing to each shopping list and let Members adjust combined servings without changing Meal Plan entries.

### Added

- Store per-occurrence recipe source, serving count, and optional meal date/type; show a horizontal recipe carousel with one image, title, and combined serving total per recipe. Open the recipe detail from a card, adjust the combined servings alongside its ingredients, and return to the refreshed shopping list after saving.
- Record each manually added contribution separately and require a selected catalog result before assigning a supported ingredient identity. Incomplete recipes are omitted from the recipe carousel; removing an aggregated ingredient removes all its list contributions while preserving recipe history as incomplete.

### Changed

- Group repeated recipe appearances into one card without displaying meal dates/types; distribute adjusted total servings proportionally across saved occurrences and recalculate their ingredient contributions. The recipe detail supports shopping-list context without changing Meal Plan behavior. Replace always-visible per-item source lines with a circular details control and a grouped recipe-contribution dialog showing the combined recipe servings.
- Require Members to confirm a shopping-list item addition or quantity change before saving, with a confirmation that names the item and describes the quantity increase or decrease when conversion is safe; manual contribution edits also confirm the named item and the amount change. Let Members edit the total amount of a recipe-linked ingredient while preserving a recipe-required minimum; show an immediate inline warning and disable saving below that minimum without displaying a routine amount breakdown. The Backend also rejects totals below the minimum. Keep the recipe unit fixed while editing that total; manual-only items remain editable. Deleting a whole item removes both recipe and manual amounts. Add catalog search and limit supported items to units returned for the selected ingredient; free-text items remain custom and have no catalog image.
- Classify surviving sibling contributions from an incomplete recipe occurrence as `UNKNOWN`. Keep them in the list total and item detail, but exclude them from the recipe carousel and required minimum. When reducing a total, consume `UNKNOWN` before Member-added amounts; increases remain Member-added. Deleting the item itself removes its own contributions. Extend FR-53 and add Flyway V29 for this lifecycle.
- Consolidate all `UNKNOWN` contributions for one ingredient into a single total row in its detail dialog. Refresh the full shopping checklist after deleting an item so recipe cards update immediately without a page reload.
- Add a three-dot menu to each shopping-list recipe card for quick serving changes and removing all occurrences of that recipe from the current list. Confirmations preview affected ingredient amounts; removal recalculates shared rows and deletes rows that no longer have any source, without changing the blog post or meal plan.
- Extend FR-53 and FR-54 acceptance criteria for recipe provenance and quantity recalculation. Existing lists without recoverable source data remain unchanged.
- Remove the duplicate serving-count input from the meal-plan recipe panel so the serving control appears only once.

### Fixed

- None.

## 2026-10-28 — Improve AI Meal-Planning Preview Readability

**Status:** Committed — `2d04630c3cef07fb79e1a5fda3d2c0575c60a8f1`.

**Scope:** Put the current and upcoming dates before past dates in the nutrition-based AI menu preview.

### Changed

- Order the preview with today and future dates first, then show one “NGÀY ĐÃ QUA” divider before past dates. Applying the preview still uses each meal's original date and selection.
- Improve contrast for the AI recipe assistant's labels and controls in dark mode.

### Fixed

- None.

## 2026-10-28 — Seed the Demo Expert Nutrition Profile

**Status:** Committed — `2d04630c3cef07fb79e1a5fda3d2c0575c60a8f1`.

**Scope:** Populate profile fields for `demo-expert@mamxanh.local` in the local demonstration database.

### Added

- Add an idempotent local demo-profile fixture that fills the demo Expert's missing nutrition-profile fields without overwriting an already completed profile; the fixture does not describe a real person.

### Fixed

- None.

## 2026-10-27 — Add Ingredient Unit Conversion to Public Detail

**Status:** Committed — `c4794dba98b0a3d9e0a150ee08e8c7611ece3a67`.

**Scope:** Let Guests and Members convert between units supported by the ingredient they are viewing.

### Added

- Add a two-way ingredient unit converter using per-ingredient gram factors, with retry and estimated-value guidance.

### Changed

- Extend FR-62 and its API integration guidance with the public unit conversion behavior.

### Fixed

- None.

## 2026-10-27 — Standardize Ingredient Recipe Units

**Status:** Committed — `c4794dba98b0a3d9e0a150ee08e8c7611ece3a67`.

**Scope:** Add per-ingredient support for standardized recipe units and precise shopping aggregation.

### Added

- Add the `mg` unit, per-ingredient unit options, and grouped volume conversion management.

### Changed

- Limit new recipe unit choices to supported standard units, let administrators enter ingredient conversion factors directly without source metadata, and preserve sub-gram shopping quantities to six decimal places.

### Fixed

- Prevent legacy units and unsupported conversions from being selected for new recipe ingredients.

## 2026-10-27 — Fix AI Chat Questions About the Current Recipe

**Status:** Committed — `b294a28ebc1b2a8e1fce65f00b5dec3561b43325`.

**Scope:** Let members ask ordinary questions about the recipe currently open without entering recipe discovery mode.

### Added

- Add regression tests for current-recipe questions and explicit requests to find another recipe.

### Changed

- Exclude questions that refer to the current recipe from catalog discovery unless the member explicitly requests another recipe or dish.

### Fixed

- Return direct answers to current-recipe questions instead of the generic invalid AI response error.

## 2026-10-27 — Add Recipe Context to AI Chat

**Status:** Committed — `dac525d5`.

**Scope:** Help members start a focused conversation about the recipe they are viewing.

### Added

- Show the recipe title, a contextual welcome message and recipe-specific question suggestions when opening AI chat from a recipe detail page.
- Embed the current recipe article in the chat with its cover image, author, description, servings, preparation details, full ingredient list and instructions.

### Changed

- Pass the recipe article preview through the frontend chat context while keeping the existing recipe ID request contract authoritative for AI responses.

### Fixed

- Replace the empty, generic recipe-chat state with a useful opening prompt.

## 2026-10-27 — Limit Nutrition AI Menus to the Current Week ([PR #193](https://github.com/NgaiLong49423/vegetarian-support-system/pull/193))

**Status:** Committed — `cc719355`.

**Scope:** Keep FR-36 proposals relevant to the remaining days of the current week and preserve entries for days that have passed.

### Added

- Compose multiple trusted, one-serving recipes per meal to pursue the member's daily energy shares and nutrition targets.
- Mark past days in the preview and show daily nutrient totals with each meal's kcal share.

### Changed

- Generate suggestions from today through Sunday only; reject applying meals outside that range and preserve past meal-plan entries.
- Show nutrition totals without classifying nutrients as deficient or excessive.

### Fixed

- None.
## 2026-10-26 — Accept Today's Vietnam Date as an Ingredient Reference Date (Issue #159)

**Status:** Committed — `ed9cd29`.

**Scope:** Fix BUG-059 in the FR-41 ingredient catalog: from 0:00 to 6:59 in Vietnam, adding an ingredient with the default reference date failed with `400` because the browser counted the day in Vietnam time while the Backend in a UTC container was still on the previous day.

### Added

- Add SQL Server integration tests that move the application clock to 00:30 in Vietnam, including a check that the validation clock follows the application clock, and Playwright tests that open the add form in UTC, America/Los_Angeles and Asia/Ho_Chi_Minh with a fixed clock.

### Changed

- Judge Bean Validation date rules such as `@PastOrPresent` by the application `Clock` in Vietnam time instead of the server clock; this covers the ingredient `referenceDate` and the nutrition profile `dateOfBirth`.

### Fixed

- Accept today's Vietnam date as the ingredient `referenceDate` at any hour, default the add form to the Vietnam date in every browser time zone, and explain a future date in Vietnamese.
## 2026-10-26 — Rank and Search Reported Posts on the Moderation Page (Issue #12)

**Status:** Committed — `c24ae67`, `f15680e`.

**Scope:** Deliver the Recipe Post side of FR-06 on the grouped report board of Issue #35: Administrators rank reported posts by report count, see the most common reason of each post and find a post by its title or author (UC-06.1, US-06.1). Hiding a post and locking its author stay the FR-28 decisions already on `/admin/bao-cao` (AC-06.2, AC-06.3). Comment reports wait for FR-48, as proposed in question Q71 on Issue #12.

### Added

- Add `query` (title, author name or author email, at most 200 characters, LIKE wildcards matched literally) and `sort` (`NEWEST` or `MOST_REPORTED`) to `GET /api/v1/admin/reports/groups`, and `topReasonCode`/`topReasonCount` to `AdminReportGroupResponse`; a tie goes to the reason reported most recently.
- Add a search box and a sort select to the "Theo bài viết" view of `/admin/bao-cao`, and the most common reason with its count to each post card.
- Add SQL Server integration tests for the ranking, the most common reason, the search and its validation, and for a new post that publishes at once while other reports are open or in review (AC-06.4), plus Playwright tests for the search, the sort and the reason line.

### Changed

- Rename the page and its admin menu entry to "Hậu kiểm nội dung", and document the new parameters in `docs/api/API.md` section 21 and the Backend and Frontend READMEs.
## 2026-10-26 — Persist Nutrition Profile BMI

**Status:** Committed — `d28503ca`.

**Scope:** Keep each Member's saved BMI available when reopening their private nutrition profile.

### Added

- Add Flyway migration `V25__persist_nutrition_bmi.sql` and update the SQL Server schema snapshot to store BMI with the existing `USER` nutrition profile.
- Backfill BMI once for existing profiles that consented to health-data storage.

### Changed

- Calculate and store BMI when a Member first saves or changes height/weight; return the saved BMI with current reference values when the profile is reopened.
- Update FR-35 acceptance criteria and UC-35.2 to specify reading saved BMI and refreshing it only after height or weight changes.

### Fixed

- Keep the BMI panel populated after refreshing or revisiting the nutrition profile.

## 2026-10-26 — Group Recipe Reports by Post for Administrators (Issue #35)

**Status:** Committed — `fdcbf11`, `9972d22`.

**Scope:** Deliver AC-30.4 of FR-30 on the FR-28 queue of Issue #33: Administrators see the reports on one Recipe Post as one group with its report count, then open each report with its reporter, status, decision and supplements (SRS 3.14).

### Added

- Add `GET /api/v1/admin/reports/groups` with one group per reported Recipe Post: the report count for the status filter, the count in each status, the newest matching report time and the author. Pages count posts, and the post with the newest matching report comes first.
- Add the optional `recipeId` filter to `GET /api/v1/admin/reports` and `supplements` (oldest first) to `AdminReportResponse`, loaded in one query per page.
- Open `/admin/bao-cao` grouped by post with one "Xem N báo cáo" button per post, keep the single-report queue under "Từng báo cáo" (`?xem=bao-cao`), and list the supplements in the report detail dialog.
- Add the local-only demo seed `R__demo_sample_data_67_report_groups.sql` with two more reports on the post of seed 60, so the board shows a group of three reports from three accounts.
- Add SQL Server integration tests for groups, the status filter, paging, the recipe filter, supplements and access, and Playwright tests for the grouped board and the supplements in the detail dialog.

### Changed

- Open the FR-28 Playwright and real-stack specs on the single-report view, and document the endpoints in `docs/api/API.md` sections 21 and 22, the seed in `database/README.md` and the board in the Backend and Frontend READMEs.

### Fixed

- Point the FR-30 line of the Backend README to `docs/api/API.md` section 22, which the develop sync in `d34c59b` left at section 21.

## 2026-10-25 — Let Reporters Add Information to an Open Recipe Report (Issue #35)

**Status:** Committed — `785f81f`, `87fffea`, `86a06ad`.

**Scope:** Complete the Member side of FR-30 with the proposals of questions Q67 and Q68: reporters keep adding information to the report that is still open instead of filing another one, and the real recipe page can now send reports (AC-30.1, AC-30.2, SRS 3.14). The Administrator view of supplements and of reports grouped by recipe (AC-30.4) builds on the FR-28 queue of Issue #33 and is not part of this entry.

### Added

- Add `GET /api/v1/me/reports/{reportId}` so the reporter reads their own report with its supplements, and `POST /api/v1/me/reports/{reportId}/supplements` to add 10–500 characters while the report is `OPEN` or `IN_REVIEW`, at most five times; other accounts get `403 ACCESS_DENIED`.
- Add migration `V23__report_supplement.sql` with the append-only `REPORT_SUPPLEMENT` table and the matching `database/schema.sql` snapshot.
- Add the local-only demo seed `R__demo_sample_data_66_report_supplements.sql` with two supplements on the open demo report from seed 60, so its reporter sees them on the recipe page.
- Add the "Báo cáo công thức" action and dialog to the real recipe page: six reasons, description rules, a sign-in prompt for Guests, and on `409 REPORT_ALREADY_OPEN` the open report with its supplements and a form to add more.
- Add SQL Server integration tests for supplements, ownership, the limit under concurrent requests and the resolved state, Playwright tests for the dialog and a real-stack test from a duplicate report to a stored supplement.

### Changed

- Document the endpoints and error codes in `docs/api/API.md` section 22, the migration in `database/README.md` and the flow in the Backend and Frontend READMEs.
- Keep one report form on the recipe page after syncing PR #189: `RecipeReportButton` and `reportsApi` stay, the unused `recipesApi.submitReport` is removed, and the PR #189 Playwright spec checks the shared dialog.

### Fixed

- None.

## 2026-10-25 — Let Administrators Handle Recipe Post Reports (Issue #33)

**Status:** Committed — `5c53627`, `fab3605`.

**Scope:** Deliver FR-28 for Recipe Post reports: Administrators take a report into review, record one decision with a mandatory reason and apply the chosen sanction, while the report row keeps who decided what and when (SRS 3.12, BR-26, NFR-08). Comment reports (FR-48), account unlock and the moderation-outcome email stay out of this change.

### Added

- Add `GET /api/v1/admin/reports`, `GET /api/v1/admin/reports/status-counts`, `GET /api/v1/admin/reports/{reportId}`, `POST /api/v1/admin/reports/{reportId}/claim` and `POST /api/v1/admin/reports/{reportId}/resolve` for Administrators only, returning reporter and author accounts for review (AC-29.3).
- Move an `OPEN` report to `IN_REVIEW` under the claiming Administrator with a row lock, so concurrent claims give one `200` and one `409 REPORT_STATUS_CONFLICT` (AC-28.1).
- Resolve only a report held by the caller with `NO_VIOLATION`, `WARN_AUTHOR`, `HIDE_RECIPE` or `LOCK_AUTHOR` and a 10–500 character reason; hiding sets the post to `HIDDEN` and removes it from public reads and search, locking sets the author to `LOCKED` so existing tokens get `403 ACCOUNT_LOCKED` (AC-28.2–AC-28.5).
- Write in-app notifications on resolution: a general result for the reporter and, for a sanction, the Administrator reason for the author, never containing the reporter name, email or report text (AC-49.4, BR-28).
- Add migration `V22__report_handling_by_admin.sql` with `REPORT.handled_by` (FK to `USER`) and `CK_REPORT_decision`, and the matching `database/schema.sql` snapshot.
- Add the local-only demo seed `R__demo_sample_data_65_report_moderation.sql`: one `OPEN` report, one `IN_REVIEW` report held by the demo Administrator and three `RESOLVED` reports (`NO_VIOLATION`, `WARN_AUTHOR`, and `HIDE_RECIPE` on a dedicated post) with the matching in-app notifications, without locking any demo account.
- Replace the `/admin/bao-cao` preview with a page backed by these endpoints: status filters with counts, claim, detail, decision form with client-side checks before sending, and view-only display for reports held by another Administrator or already resolved.
- Add SQL Server integration tests for each Acceptance Criterion, permissions, concurrent claims and immutable resolved reports, Playwright tests for the page and a real-stack test from a submitted report to the hidden post.

### Changed

- Remove the report module and its tests from the Admin preview, which now covers accounts and transactions only.
- Document the endpoints and error codes in `docs/api/API.md` section 21, the migration in `database/README.md` and the flow in the Backend and Frontend READMEs.

### Fixed

- None.

## 2026-10-24 — Notify Members of Replies In-App and by Email (Issue #51)

**Status:** Committed — `a0157f9`, `1386e6e`, `1338052`, `c1237af`, `9294dcd`.

**Scope:** Deliver the FR-49 parts that do not depend on the report-handling workflow (Issue #33): reply notifications (AC-49.1), best-effort reply emails (AC-49.2, AC-49.3), the notification center and read state (UC-49.1, UC-49.2) and the reply email preference (UC-49.4). Moderation-outcome notifications (AC-49.4) wait for Issue #33.

### Added

- Create a `COMMENT_REPLY` notification for the author of the answered comment in the same transaction as the reply; root comments and replies to one's own comment create none, and a reply to a level-5 comment notifies that comment's author although it is stored under the level-4 parent.
- Send the reply email after commit on an async thread when `USER.reply_email_enabled` is on and the account is active and verified; a delivery failure is logged without the address and keeps the reply and its notification.
- Add owner-scoped endpoints `GET /api/v1/me/notifications`, `GET /api/v1/me/notifications/unread-count`, `PUT /api/v1/me/notifications/{notificationId}/read`, `PUT /api/v1/me/notifications/read-all` and `GET`/`PUT /api/v1/me/notification-preferences`, with `404 NOTIFICATION_NOT_FOUND` for another user's notification.
- Add the header bell with the unread count, the `/thong-bao` notification center with an unread filter, paging and "mark all read", and the reply email switch in profile settings.
- Add SQL Server integration tests for every Acceptance Criterion above and Playwright tests for the bell, center, deep link and preference.

### Changed

- Give each recipe comment a `comment-{id}` anchor; a notification link loads further comment pages until the reply is shown and focuses it.
- Document the endpoints in `docs/api/API.md` section 23 and the Backend and Frontend READMEs; no migration is needed because `USER.reply_email_enabled` exists since V1.
- Split the email notification note in profile settings into two sentences instead of joining them with a semicolon.
- Split the closing sentence of the reply email in two instead of joining them with a semicolon, and check the email text in the reply email test.

### Fixed

- None.

## 2026-10-24 — Refine Shopping List Workflow ([PR #185](https://github.com/NgaiLong49423/vegetarian-support-system/pull/185))

**Status:** Committed — `c4822f9`.

**Scope:** Make shopping-list creation, item management and review more consistent across recipe, meal-plan and shopping-list screens.

### Added

- Allow selecting recipe ingredients and adding manual items in the shopping-list dialog, with duplicate-name checks and numbered names for same-day lists.
- Add shopping-list export actions and a compact menu for list-level actions.

### Changed

- Use the creation date for meal-plan shopping lists, order lists newest first and remove internal list codes from the interface.
- Refine shopping-list layout, colors, controls and spacing across the page; use icon-only edit and delete actions for items.

### Fixed

- Normalize whitespace when aggregating manual item names so equivalent names are combined consistently.

## 2026-10-24 — Deduplicate Recipe Ingredient Selections

**Status:** Committed — `95a05ee`.

**Scope:** Keep recipe-to-shopping-list additions valid when one ingredient appears in multiple recipe lines with different units.

### Added

- Add Frontend and Backend regression coverage for repeated ingredient IDs, selected ingredients, and manual-only additions.

### Changed

- Submit each selected ingredient ID once while preserving every matching recipe line and unit in the shopping list.

### Fixed

- Prevent duplicate selected IDs from causing a validation error when adding a recipe to a shopping list.

## 2026-10-23 — Remove Recipe Card Status Badges

**Status:** Committed — `c0c1aff`.

**Scope:** Remove the “Mới” and Like percentage badges from recipe imagery and rating headings.

### Added

- None.

### Changed

- Remove the recipe status and Like percentage badge from public recipe cards and the detail-page reaction heading.
- Keep recipe voting controls, accessible Like ratio progress, and reaction counts.

### Fixed

- None.

## 2026-10-23 — Center Ingredient Directory Content

**Status:** Committed — `c0c1aff`.

**Scope:** Align the ingredient directory header and catalog within one centered content column.

### Added

- None.

### Changed

- Center the directory content wrapper so the heading, search controls and ingredient grid share the same alignment.

### Fixed

- Correct the left-heavy layout caused by a narrow catalog section aligned to the page container edge.

## 2026-10-23 — Show All Core Nutrients in Daily Summary

**Status:** Committed — `c0c1aff`.

**Scope:** Show all nine core metrics in a clearer hierarchy on the nutrition tracker.

### Added

- Show fiber, calcium, iron, vitamin B12 and zinc alongside energy, protein, carbohydrate and fat.

### Changed

- Keep energy, protein, carbohydrate and fat as primary cards; group fiber and four micronutrients in a compact section.
- Align primary metric values, reference amounts and progress bars in a consistent card layout.

### Fixed

- None.

## 2026-10-23 — Distinguish Meal Labels in Nutrition Tracker

**Status:** Committed — `c0c1aff`.

**Scope:** Improve the visual distinction between meal labels and energy distribution on the nutrition tracker.

### Added

- Add an amber badge tone for meal labels.

### Changed

- Use matching amber, green and brand-orange colors for breakfast, lunch and dinner labels, energy-bar segments and legend dots.

### Fixed

- None.

## 2026-10-23 — Restore Recipe Images in Nutrition Tracker

**Status:** Committed — `c0c1aff`.

**Scope:** Show scheduled recipe cover images on the daily meal cards in nutrition tracking.

### Added

- Load cover URLs from the existing weekly meal-plan response and match them to nutrition entries.

### Changed

- Render each available recipe cover above its nutrition summary while keeping the utensil placeholder for missing or failed images.

### Fixed

- Replace the permanent image placeholder on scheduled recipe cards when a cover image is available.

## 2026-10-23 — Move Recipe Comparison into Card Menu

**Status:** Committed — `c0c1aff`.

**Scope:** Keep recipe-card actions compact by placing comparison in the existing overflow menu.

### Added

- Add a comparison action to the recipe-card three-dot menu.

### Changed

- Remove the standalone comparison link below public recipe cards.

### Fixed

- None.

## 2026-10-23 — Apply Ingredient Filters from Catalog

**Status:** Committed — `c0c1aff`.

**Scope:** Carry selected ingredient filters from the public ingredient catalog and detail page into recipe discovery.

### Added

- Initialize recipe discovery with ingredient IDs and names passed in the URL.

### Changed

- Keep ingredient selection in the URL as filters are changed and clear those parameters when resetting the search.

### Fixed

- Apply the selected ingredient IDs to recipe discovery requests after navigating from ingredient pages.

## 2026-10-23 — Fix Recipe Ingredient Editing

**Status:** Committed — `c0c1aff`.

**Scope:** Ensure recipe editing can resolve existing ingredients beyond the initial reference-data result limit.

### Added

- None.

### Changed

- Search the existing ingredient reference-data endpoint for recipe ingredients missing from the initial options and merge the valid units into the editor.

### Fixed

- Stop presenting ingredients omitted by the initial result cap as unsupported; clarify the warning for ingredients that remain unavailable after lookup.

## 2026-10-23 — Correct Demo Ingredient Groups

**Status:** Committed — `c0c1aff`.

**Scope:** Replace the default or inconsistent groups on seeded demo ingredients with food-appropriate categories.

### Added

- Add an idempotent local demo seed that assigns consistent groups to all 209 illustrated ingredients.

### Changed

- Document the ingredient-group fixture and its limited effect on demo catalog group labels.

### Fixed

- Categorize vegetables, mushrooms, fruit, grains, legumes, nuts and seeds, seasonings, dairy and eggs, seaweed, plant protein, and prepared vegetarian foods under appropriate groups.

## 2026-10-23 — Fix Integration Regressions After Develop Synchronization ([PR #162](https://github.com/NgaiLong49423/vegetarian-support-system/pull/162))

**Status:** Committed — `4968e40`.

**Scope:** Restore discovery badges and safe shopping-list ingredient selection after integrating the current baseline.

### Added

- Cover ingredient subsets, manual-only shopping items, duplicate ingredient selections, serving scaling, and atomic rollback through HTTP and SQL integration tests.
- Add browser regressions for selecting one ingredient represented by multiple units from discovery and saved recipes.

### Changed

- Align shopping browser assertions with the current ingredient-selection and manual-item request contract.

### Fixed

- Restore the Like percentage or New badge on discovery cards required by FR-17.
- Deduplicate initially selected ingredient IDs so recipes using multiple units do not produce rejected shopping-list requests.
- Preserve ingredient filters when navigating from the ingredient directory or detail page into discovery.

## 2026-10-23 — Improve Ingredient and Shopping Workflows

**Status:** Committed — `f58b0e1`.

**Scope:** Let Members inspect ingredients and add selected recipe items to shopping lists without leaving recipe details, while enabling administrators to manage ingredient images.

### Added

- Add ingredient image upload in the Admin catalog and link ingredient names from recipe details to ingredient detail pages.
- Add a shopping-list action to recipe detail pages and allow Members to select recipe ingredients or add manual items before submitting.

### Changed

- Extend shopping-list API requests to carry selected recipe ingredients and manual items, and update the API and requirement documentation.

### Fixed

- None.

## 2026-10-23 — Keep Recipe Search Ingredients in Sync with the URL (Issue #70)

**Status:** Committed — `2e03550`, `e42b65f`.

**Scope:** Finish the FR-62 ingredient links in the shared `RecipeSearch` module used by Explore and the FR-60 picker: PR #185 reads `ingredientId`/`ingredientName` from the URL, but removed or reset ingredients stayed in the URL and came back on the next URL change or reload. Also keep the FR-60 picker within a phone-width page.

### Added

- Add Playwright coverage for an ingredient preset on the picker, removing it before another filter changes, resetting all filters, and a picker card with a long dish category label on a 360 px wide screen.

### Changed

- None.

### Fixed

- Write the selected ingredients back to the URL when one is added or removed, so a removed preset no longer comes back when the page is reloaded or reopened from the browser history.
- Remove `ingredientId`/`ingredientName` from the URL when the filters are reset.
- Keep picker result cards inside a phone-width page when a recipe has a long category label such as "Món tráng miệng / chè": the result grid uses one shrinkable column below the `sm` breakpoint (AC-60.8).

## 2026-10-23 — Fix AI Recipe Guidance Generation ([PR #181](https://github.com/NgaiLong49423/vegetarian-support-system/pull/181))

**Status:** Committed — `3926690`.

**Scope:** Prevent incomplete or falsely rejected AI-generated recipe descriptions and cooking instructions.

### Added

- Add regression coverage for Gemini output truncation and ingredient-name validation in cooking instructions.

### Changed

- Reserve an 8,192-token response budget for recipe text generation while keeping Gemini's default reasoning and a 60-second provider timeout.
- Share Vietnamese-aware ingredient mention validation between Expert recipe authoring and Member cooking guidance.

### Fixed

- Reject truncated Gemini recipe output explicitly and allow selected compound ingredients, rinsing water, sauce, and dicing instructions without mistaking them for added ingredients.

## 2026-10-23 — Detailed AI Cooking Guides ([PR #181](https://github.com/NgaiLong49423/vegetarian-support-system/pull/181))

**Status:** Committed — `adfbcd9`.

**Scope:** Extend AI cooking guidance for recipe authors and Members following suggested recipes.

### Added

- Add a source-linked cooking guide for Members with 6–10 ordered preparation, cooking, and plating steps.

### Changed

- Expand the Expert recipe-writing assistant to produce detailed, editable cooking steps and label estimated time or temperature.
- Update FR-34 requirements, use cases, and API documentation for the new guide.

### Fixed

- None.

## 2026-10-23 — Bound FR-36 Gemini Preview Latency

**Status:** Committed — `af1127a`.

**Scope:** Keep AI-generated nutrition-menu previews within the provider and client timeout budget.

### Added

- Add a regression assertion for the shared Gemini provider timeout.

### Changed

- Set the shared Gemini provider request timeout to seven seconds and the FR-36 preview request timeout to eight seconds.

### Fixed

- None.

## 2026-10-23 — Fix AI Weekly Menu Submission Without Ingredient Filters (FR-34)

**Status:** Committed — `764e121`.

**Scope:** Ensure the weekly-menu preview action works when Members leave optional ingredient filters empty.

### Added

- None.

### Changed

- Synchronize the generated OpenAPI snapshot with the Backend contract produced by CI for the PR commit.

### Fixed

- Allow weekly-menu preview requests with no selected ingredients while keeping ingredient selection required for single-recipe recommendations.
- Wait for the weekly-menu API request in the E2E scenario before asserting its preview.

## 2026-10-22 — Define Ingredient Deactivation Guard (Issue #176) ([PR #177](https://github.com/NgaiLong49423/vegetarian-support-system/pull/177))

**Status:** Committed — `acafeacf`.

**Scope:** Update FR-18 and UC-18.1 so an ingredient referenced by a currently published recipe cannot be deactivated.

### Added

- Define rejection, preserved active state, and administrator feedback while any published recipe references the ingredient.

### Changed

- Allow deactivation once no published recipe currently references the ingredient; preserve existing inactive records and historical links.
- Limit the rule to ingredients; measurement units remain outside Issue #176.

### Fixed

- None.

## 2026-10-22 — Allow AI Weekly Menus Without Ingredient Filters (FR-34)

**Scope:** Let eligible Pro Members preview an AI-generated weekly menu from the safe public recipe catalog without first selecting ingredients, while preserving optional ingredient filtering.

### Changed

- Make the weekly-menu preview ingredient filter optional; keep ingredient selection required for single-recipe recommendations.
- Allow weekly-menu candidates from the full profile-safe public catalog when no ingredients are selected.
- Update the API guide and weekly-menu planner flow for the optional filter.

## 2026-10-22 — Create and Manage Shopping Lists (FR-53) ([PR #175](https://github.com/NgaiLong49423/vegetarian-support-system/pull/175))

**Status:** Committed — `47dc13a`.

**Scope:** Let Members create and manage shopping lists, then add one recipe at a time to a selected or newly created list.

### Added

- Add owner-protected API operations to rename and delete a shopping list, with confirmation and fallback navigation in the Frontend.
- Add a named empty-list flow at `/di-cho` and a `MANUAL` list kind that is independent from meal plans and recipe sources.
- Add per-recipe shopping actions from recipe cards, with destination selection, serving adjustment and an atomic create-and-add API operation.
- Show catalog ingredient illustrations in shopping checklist rows, with a clear missing-image state for items without an available illustration.
- Remove multi-recipe selection and the `POST /from-recipes` API contract.
- Delete list contribution records and checklist items while preserving meal plans and source recipes.

### Changed

- Extend FR-53 acceptance criteria and document standalone list creation, list-level management and per-recipe additions.

### Fixed

- None.

## 2026-10-22 — Hand Off Shopping List Clipboard and Text Export (FR-55)

**Status:** Committed — `28a4324` (original export implementation).

**Scope:** Prepare Issue #55 on its own branch, inheriting the explicitly required FR-53 checklist platform from PR #171; integration and acceptance remain pending.

### Added

- Copy the complete selected checklist with food groups, saved quantities and units, and bought/unbought markers; offer a keyboard-accessible manual fallback when clipboard access fails.
- Download the same content as UTF-8 TXT with a BOM and the export-date filename, keeping planned meal dates separately inside the text.
- Cover clipboard, downloads, saved state, filters, empty lists, access denial, failure recovery and meal-date headers in browser tests.

### Changed

- Connect the export toolbar to the current owned checklist without changing its aggregation, persistence or source-recipe data.

### Fixed

- None.


## 2026-10-22 — Enrich Demo Recipe Ingredients and Illustrations ([PR #175](https://github.com/NgaiLong49423/vegetarian-support-system/pull/175))

**Status:** Committed — `47dc13a`.

**Scope:** Improve local Flyway demo recipe data and ingredient illustrations.

### Added

- Add per-recipe seasonings and synchronized cooking instructions for 54 named demo recipes, with all ingredient quantities expressed in grams for two servings.
- Add individual AI-generated ingredient illustrations and a missing-image mapping seed that preserves existing image URLs.
- Add a Compose-based fixture verification script for repeated execution, gram quantities, nutrition completeness, asset presence and preservation of unrelated recipes and uploaded images.

### Changed

- Convert legacy demo units using mass factors or ingredient-specific conversions and stop when a reliable conversion is unavailable.
- Document dry ingredient weights and retained recipe water separately from discarded preparation water.

### Fixed

- Persist the existing zinc estimates for 85 demo ingredients instead of leaving their zinc columns null (BUG-054).
- Include stored demo spice estimates in nutrition calculations under the current BR-52 policy while retaining unverified AI provenance (BUG-055).

## 2026-10-22 — Align Public Profile Recipe Cards with Discovery ([PR #175](https://github.com/NgaiLong49423/vegetarian-support-system/pull/175))

**Status:** Committed — `47dc13a`.

**Scope:** Unify published recipe cards on public member and expert profiles with recipe discovery.

### Added

- None.

### Changed

- Reuse the shared recipe card with landscape covers, author details, cooking metadata, reaction percentage, view counts and save/meal-plan actions.
- Adapt profile recipe grids from one to four columns while preserving pagination and author interaction totals.

### Fixed

- None.

## 2026-10-21 — Add Public Ingredient Details ([PR #175](https://github.com/NgaiLong49423/vegetarian-support-system/pull/175))

**Status:** Committed — `25b113f`.

**Scope:** Let users inspect active ingredient details and available nutrition data.

### Added

- Add a public detail endpoint and a responsive ingredient detail page with per-100 g nutrition values.
- Display published recipe articles containing the ingredient directly on its detail page, with recipe diet labels, sorting and load-more pagination.
- Open ingredient details from directory names/images and recipe ingredient names.
- Show missing nutrition values as unavailable and keep recipe discovery filters from the directory and detail page.

### Changed

- Expand FR-62 to cover public ingredient details, images and nutrition while excluding administrative provenance and status fields.
- Synchronize the discovery use case, component guides and public API integration conventions with the ingredient detail flow.

### Fixed

- None.

## 2026-10-21 — Add Ingredient Reference Images ([PR #175](https://github.com/NgaiLong49423/vegetarian-support-system/pull/175))

**Status:** Committed — `25b113f`.

**Scope:** Let administrators attach and manage an illustrative image for each ingredient in the catalog.

### Added

- Upload optional JPEG, PNG or WebP ingredient images up to 2 MB and persist their URLs with ingredient records.
- Preview selected images in the admin editor and prefer saved images on ingredient cards, with existing image mappings as fallback.
- Add the nullable `INGREDIENT.image_url` migration as V20 after integrating the V18/V19 checklist migrations, and synchronize the database schema snapshot.

### Changed

- None.

### Fixed

- None.

## 2026-10-21 — Integrate Shopping Checklist Management (FR-53) ([PR #171](https://github.com/NgaiLong49423/vegetarian-support-system/pull/171))

**Status:** Committed — `daac445`. Original implementation: `83f1938`; develop synchronization: `34cf4e6`.

**Scope:** Prepare the owned shopping checklist and selected Recipe Post flow for a separate Issue #53 handoff; retain the explicit dependency on the unmerged FR-54 engine.

### Added

- Create persisted checklists from planned meal dates or selected public recipes, with manual items, editing, bought-state updates, deletion and ownership checks.
- Preserve original recipe contributions and unavailable source links without changing saved quantities when a recipe changes.

### Changed

- Move unmerged shopping migrations to V18/V19 while preserving develop's nutrition V16/V17.
- Adapt browser checks to the current discovery, profile-statistics and partial-nutrition baseline.

### Fixed

- Remove the unresolved dependency on the separately committed FR-55 export component so FR-53 can build independently.
- Use explicit calendar-value comparisons and manage shopping-dialog focus through refs while preserving keyboard access to add, edit and cancel actions.
## 2026-10-22 — Select Public Recipes Directly in Meal Plans (Issue #154)

**Status:** Committed — `b5c8bdb`.

**Scope:** Complete the FR-09 / FR-33 recipe picker flow using the existing public search and meal-plan APIs.

### Added

- Switch between saved recipes and public recipe search without leaving the selected meal; search and paginate both sources.
- Cover empty saved collections, public add/replace, unavailable or duplicate choices, failure recovery, late responses and keyboard access in browser tests.

### Changed

- Keep saved recipes as the default source and preserve day, meal and servings when selecting a public replacement.
- Extend the Compose integration scenario to verify adding a public recipe that is not saved, without inserting a saved-recipe row.

### Fixed

- Remove the requirement to leave the planner and save a recipe before selecting it for a meal; meal-plan operations do not modify the saved collection.


## 2026-10-21 — Restore CI Compatibility with Public Discovery ([PR #149](https://github.com/NgaiLong49423/vegetarian-support-system/pull/149))

**Status:** Committed — `51b706b`.

**Scope:** Resolve integration failures exposed while validating the payment fixes against the current develop baseline.

### Added

- Verify that public recipe statistics remain accessible to guests while private subscription APIs require authentication.

### Changed

- Align browser fixtures and navigation checks with the public statistics API and Explore dropdown; wait for both ingredient selections before checking the discovery request.

### Fixed

- Permit guest reads of the documented public recipe-statistics endpoint.
- Preserve Vietnamese characters when binding ingredient search patterns on SQL Server.

## 2026-10-21 — Expert Application Attachments

**Status:** Committed — `fbaf8e4`.

**Scope:** Extend FR-05 so applicants can attach a certificate and an application-only 3:4 portrait, with private storage and Admin-only retrieval.

### Added

- Add optional certificate and application-only 3:4 portrait uploads with server-side type, signature, and size validation; store attachment metadata through Flyway V24 after develop used V23 for report supplements.
- Add an Admin-only attachment download endpoint backed by a private Azure Blob Storage container.

### Changed

- Update FR-05, BR-74, NFR-10, architecture, database guidance, and the application form to describe the approved attachment behavior.
- Focus the first-time application view on the submission form and group optional uploads into a compact section.
- Identify the application portrait as a 3:4 photo, add an explicit Admin download action, and improve the review dialog's hierarchy, attachment cards, status, and responsive scrolling.
- Keep the existing JSON submission endpoint and public account avatar flow unchanged.

### Fixed

- Bind Azure storage configuration through its canonical constructor so the Backend can start with the new private-container property.

## 2026-10-20 — Remove Standalone Meal Plan Export (Issue #152)

**Status:** Committed — `5ac1614`.

**Scope:** Remove the standalone weekly meal-plan export from the active requirements and implementation while preserving the FR-37 nutrition report PDF with its planned-meal list.

### Added

- None.

### Changed

- Remove the standalone meal-plan export use case and acceptance criterion from FR-33 and its linked interaction flow; remove its Frontend controls, file-generation helper, Backend endpoint, service, and response DTO.
- Remove the standalone export dependency and obsolete UI, Backend, and SQL integration checks.
- Remove the obsolete endpoint description from the API guide; the generated OpenAPI snapshot still requires regeneration from a running Backend.
- Clarify that recipe exports, FR-37 nutrition report PDFs, and Shopping List text export remain in scope.

### Fixed

- None.

## 2026-10-20 — Correct Subscription Time Display and Transaction Expiry ([PR #149](https://github.com/NgaiLong49423/vegetarian-support-system/pull/149))

**Status:** Committed — `1d81141`.

**Scope:** Follow-up payment verification fixes for Issue #142 / FR-13.

### Added

- Show the expiry of the subscription purchased by each paid transaction in transaction history; cancelled or unlinked orders have no expiry.
- Add timezone, midnight rollover, historical subscription mapping and API serialization regression checks.

### Changed

None.

### Fixed

- Use UTC consistently for checkout creation and payment activation, and include the UTC offset in subscription API timestamps.
- Display subscription and payment dates in Vietnam time regardless of the browser timezone.

## 2026-10-20 — Expand Local Demo Data ([PR #166](https://github.com/NgaiLong49423/vegetarian-support-system/pull/166))

**Status:** Committed — `fa44e59`.

**Scope:** Expand local demo coverage across ingredients, community interactions, and shopping while allowing flexible nutrition provenance and clearly labeling unverified estimates.

### Added

- Allow freely entered nutrition sources, including `Ước tính AI - chưa xác minh`, in the catalog.
- Seed recipe comments and replies, saved recipes, shopping lists and checklist items, moderation reports, and reply notifications for demo accounts.
- Add 51 local demo ingredients, including chicken egg, with all nine AI-estimated nutrition values per 100 g; keep the estimates explicitly unverified.
- Add ten more local recipes across vegan meals and an ovo-vegetarian breakfast, using the expanded ingredient catalog.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Replace the seed's `DEMO` source type with `AI_ESTIMATE`, retain unverified provenance, and include saved estimates in partial nutrition calculations without treating missing fields as zero.
- Remove the source-type and all-nine-values database restrictions for supported rows; require at least one stored nutrient and keep missing values as `NULL`.
- Make the new community and shopping fixtures repeatable by checking their stable account, recipe, and item keys before insertion.

### Fixed

- Seed core recipe ingredients before applying catalog nutrition so supported entries such as sweet potato are enriched correctly.

- Fill the remaining macro, fiber and B12 estimates for all AI-estimated demo ingredients so those fixtures no longer have missing nutrition metrics.

## 2026-10-19 — Store Recipe Images in Azure (Issue #158) ([PR #162](https://github.com/NgaiLong49423/vegetarian-support-system/pull/162))

**Status:** Committed — `c3a5155`.

**Scope:** Fix misleading upload success when Azure storage is missing or unavailable ([Issue #158](https://github.com/NgaiLong49423/vegetarian-support-system/issues/158)).

### Added

- Add unavailable-storage tests, browser upload-state regression checks, and an opt-in Azure/Compose integration test.

### Changed

- Forward Backend Azure configuration through the root Compose stack and document the image storage setup.
- Regenerate OpenAPI to describe upload success as HTTP 201 and storage failure as HTTP 503.

### Fixed

- Reject uploads with `MEDIA_STORAGE_UNAVAILABLE` instead of returning fabricated storage URLs; sanitize storage failure diagnostics.
- Label failed previews, count only successful uploads, and block publication while uploads are pending or failed.

## 2026-10-19 — Expert and Ingredient Discovery (FR-61/FR-62) and Social Recipe Profiles ([PR #163](https://github.com/NgaiLong49423/vegetarian-support-system/pull/163))

**Status:** Committed — `3106850`.

**Scope:** Add public expert discovery, ingredient-group browsing, Explore entry points and per-post engagement counts on public profiles.

### Added

- Add a searchable, paginated active-expert directory with public profile fields and follow actions backed by existing follow APIs.
- Add a searchable catalog of active ingredients presented as selectable labels for recipe discovery.
- Add four generated, locally hosted portraits for the seeded Expert demo accounts.
- Add backend read endpoints for expert directory and public ingredient catalog data without schema changes.
- Present public profile recipes as a social-style image grid with each post's API-backed view and like counts.
- Show profile-wide totals for views and likes across all published recipes, independent of the current recipe page.

### Changed

- Expand the Explore navigation menu on desktop and mobile to include recipes, ingredient groups and experts; keep community accessible through the existing Home entry point.
- Align the Explore dropdown trigger with the shared orange navigation hover indicator.
- Replace ingredient group cards and per-ingredient rows with a single multi-select label list that preserves the recipe filter's all-selected-ingredients rule.
- Add FR-61 and FR-62 to the current requirements registry and detailed specifications.
- Add a public recipe-statistics endpoint that aggregates likes and views across every published recipe by author; no database migration is required.

### Fixed

- None.

## 2026-10-19 — Refresh Landing Previews and Scroll Motion ([PR #157](https://github.com/NgaiLong49423/vegetarian-support-system/pull/157))

**Status:** Committed — `0f1ce32`.

**Scope:** Update the public Landing presentation alongside the account, calendar and application refinements retained from PR #156.

### Added

- Add replayable scroll reveals with reduced-motion and missing-observer fallbacks.
- Add browser checks for illustration assets, scroll effects and responsive navigation, and upload Landing screenshots with the existing CI UI evidence.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Replace captured application previews with decorative HTML/CSS illustrations using existing local food images.
- Simplify the header and retain section navigation in the footer; preserve application destinations and account-specific collection links.

### Fixed

- Align the Landing tests and workspace guide with the current preview and navigation design.

## 2026-10-18 — Refine Account, Calendar and Admin Interfaces ([PR #156](https://github.com/NgaiLong49423/vegetarian-support-system/pull/156))

**Status:** Committed — `2e6ec94`.

**Scope:** Refine the Frontend presentation and reconcile it with the current develop integration baseline.

### Added

- Add interactive local Admin previews for account roles/locks, report moderation and transaction details, explicitly labeled as sample data without Backend writes.
- Add browser coverage for preview interactions and calendar keyboard navigation across months.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Refresh account forms, shared navigation, recipe details, profile pages and the animated meal calendar while retaining reduced-motion support.
- Require Guest sign-in for meal planning and shopping; keep explicit local previews behind `?demo=true`.

### Fixed

- Preserve API-backed recipe comments, reactions, views and meal-plan serving updates after synchronizing develop.
- Keep tall registration forms scrollable, retain ingredient JSON preview/copy, and use the actual vegetarian-type label in recipe details.

## 2026-10-19 — Point Duplicate Recipe Reports to the Open Report and Require New Details After Resolution (Issue #35)

**Status:** Committed — `03ae27a`.

**Scope:** Deliver the FR-30 rules that do not depend on the open supplement-storage and report-form decisions (Q67, Q68): a duplicate report now tells the client which open report to continue, and a report filed after an earlier one was resolved must describe the new problem (SRS 3.14, AC-30.2, AC-30.3).

### Added

- Add `reportId` of the reporter's open report to the `409 REPORT_ALREADY_OPEN` problem body of `POST /api/v1/recipes/{recipeId}/reports`.
- Add SQL Server integration tests for duplicate reports, four concurrent submissions by the same member (one `201`, three `409` with the created `reportId`), re-reporting after `RESOLVED`, and a first report with a non-`OTHER` reason and no description.
- Record BUG-065: moving a meal-plan entry sometimes leaves keyboard focus on the page body instead of the "Lịch ăn của bạn" heading (FR-33, AC-33.4; 6 of 28 isolated runs of `meal-plan-management.spec.ts:120` failed on `develop` `0e98048`).

### Changed

- Require a 10–500 character description for every reason when the member's previous report on the same recipe is `RESOLVED`; otherwise return `400 VALIDATION_FAILED` with `field = description`.
- Document report submission, the duplicate rule and the re-report rule in `docs/api/API.md` section 22 and the Backend README.

### Fixed

- None.

## 2026-10-18 — Repair CI Contract and Recipe-Card Checks ([PR #148](https://github.com/NgaiLong49423/vegetarian-support-system/pull/148))

**Status:** Committed — `a74f7c2`.

**Scope:** Synchronize the generated Backend contract for Explore discovery and correct recipe-card E2E checks that failed in CI.

### Added

- None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Regenerate `docs/api/openapi.yaml` from the running Backend OpenAPI endpoint.

### Fixed

- Disambiguate default-artwork accessible-name checks and click visible meal-slot labels in Playwright.
- Expect two successful meal additions when the first of three selected slots returns a simulated conflict.
## 2026-10-18 — Send Chatbot Questions with Enter

**Status:** Committed — `aad73a1`.

**Scope:** Fix keyboard submission in the AI Chatbot so users can send questions without leaving the keyboard.

### Added

- Add browser regression coverage for Enter submission, Shift+Enter multiline input and empty questions.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Use Enter to submit a Chatbot question while preserving Shift+Enter for a newline.

### Fixed

- Prevent Enter from leaving a non-empty Chatbot question unsent.

## 2026-10-17 — Add Multiweek Meal Plan Actions (Issue #23) ([PR #151](https://github.com/NgaiLong49423/vegetarian-support-system/pull/151))

**Status:** Committed — `02bf185`.

**Scope:** Improve recipe-card actions and let users schedule a recipe across multiple meal slots and weeks.

### Added

- Add recipe-card actions and a multi-select meal-plan dialog that retains selections while navigating between weeks.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Submit selected meal slots to their corresponding calendar weeks and retain failed selections for retry.

### Fixed

- None.

## 2026-10-17 — Remove Recipe Counts from Explore Pagination

**Status:** Committed — `24fd7cd`.

**Scope:** Remove the total-count query and total-page display from the Explore recipe browse flow while preserving count-based responses for Home and author profiles.

### Added

- Add a discovery browse response with `hasNext` pagination for Explore.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Use previous/next navigation in Explore without displaying total recipe or page counts.

### Fixed

- Avoid running the total-count query for Explore recipe browsing.

## 2026-10-17 — Add Safe Shopping Ingredient Aggregation ([PR #150](https://github.com/NgaiLong49423/vegetarian-support-system/pull/150), Issue #54)

**Status:** Committed — `748947f` (implementation); PR #150 is open and has not been merged or accepted.

**Scope:** Implement and test the internal FR-54 aggregation engine independently of the unmerged FR-53 checklist work. Creating/persisting lists, Frontend integration and main acceptance remain outstanding.

### Added

- Aggregate catalog lines by ingredient identity, normalize g/kg and ml/L without early rounding, preserve count units, and use only active ingredient-specific official rules for other conversions. Keep incompatible rows separate and group results by catalog category.
- Merge manual rows only by case-insensitive trimmed name and identical unit, keep them separate from catalog ingredients, and return source-indexed warnings for invalid rows or conflicting conversion rules.
- Add unit coverage for AC-54.1–54.5 and edge cases, plus integration tests against real SQL Server catalog and conversion data.

### Changed

- Extend the nutrition service boundary with batch reference and eligible conversion lookups. Historical inactive references remain readable while inactive conversion rules, ingredients or units cannot authorize cross-unit aggregation.
- Document the internal calculation contract and the remaining FR-53/#90 integration boundary. No HTTP API, schema relationship, migration or technology change is introduced.

### Fixed

- None.
## 2026-10-17 — Verify Paid Subscription Activation ([PR #149](https://github.com/NgaiLong49423/vegetarian-support-system/pull/149))

**Status:** Committed — `39ccd9d`.

**Scope:** FR-13 / Issue #142: replace false payment success with verified payOS checkout, order reconciliation and subscription activation.

### Added

- Add a private per-order result endpoint with server-side payOS reconciliation, ownership checks and non-cacheable responses.
- Add payment settlement, signature, concurrency and browser regression tests for delayed activation, provider failures and forged success URLs.

### Changed

- Pass Backend payOS configuration through the canonical Compose stack and document checkout/webhook setup.
- Generate the OpenAPI snapshot from the updated Backend runtime and align subscription types with its responses.
- Serialize verified settlement by account and order, preserving atomic activation and idempotent webhook processing.
- Run shared-SQL integration tests sequentially, synchronize recipe-view checks with completed search responses, and use the canonical Backend host for the OpenAPI healthcheck.

### Fixed

- Missing payOS credentials no longer create a fake success checkout or accept mock webhook signatures.
- Payment result pages confirm the specific order before claiming activation, with bounded polling and retry on pending or failed verification.
- Display subscription features and transaction tiers from Backend responses.

## 2026-10-16 — Expand Local Demo Fixtures ([PR #141](https://github.com/NgaiLong49423/vegetarian-support-system/pull/141))

**Status:** Committed — `9b6c1fe`.

**Scope:** Expand the local demo experience across accounts, meal plans, recipe content and community activity.

### Added

- Add 17 Customer accounts, 3 Expert accounts, 20 recipes, 30 catalogue ingredients, four weeks of meals for every Customer, and repeatable reaction, view and follow fixtures.
- Assign the shared local demo password to all 25 seeded accounts through the local-only credential initializer.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Complete the profiles of all 20 demo Customers, including current BMI inputs and nutrition consent; convert the two former onboarding personas into full demo Customers.
- Update Backend and Database guides with the expanded local fixture and its account roles.

### Fixed

- Keep recipe counters synchronized with seeded reactions and views while rerunning the local seed without duplicating its fixtures.

## 2026-10-15 — Synchronize Recipe Views with Develop

**Status:** Committed — `e532d43`.

**Scope:** Reconcile FR-58 / Issue #57 / PR #123 with the integration baseline, preserving shared recipe, meal-plan, authentication and CI behavior.

### Added

- None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Preserve the multi-window Most Viewed filters and record Guest/Member views without changing recipe detail or browse contracts.
- Keep migration V15 and preserve the FR-58 bug records as BUG-033, BUG-034 and BUG-035; BUG-030–032 are already assigned on develop.
- Regenerate the committed OpenAPI snapshot from the synchronized Backend runtime.

### Fixed

- Resolve Git, migration and contract conflicts against the current develop baseline, including commit `9114dcf`.
- Document ProblemDetail error schemas for view-recording responses instead of the success DTO (BUG-035).
- Adapt meal-plan browser fixtures and scope feedback assertions for concurrent view-recording messages.

## 2026-10-13 — Add Member Following (Issue #69) ([PR #128](https://github.com/NgaiLong49423/vegetarian-support-system/pull/128))

**Status:** Committed — `81ea5ec` (Backend), `8cd7bfc` (Frontend and documentation).

**Scope:** Implement UC-59.1–UC-59.4 so members can follow and unfollow other members from the public profile, see follower and following counts, and browse both lists, following AC-59.1–AC-59.7 and BR-75.

### Added

- Add `GET /api/v1/members/{userId}/follow` (public follow summary with the viewer's state), idempotent `PUT`/`DELETE` on the same path, and paginated `GET /api/v1/members/{userId}/followers` and `/following` for signed-in users. The follower always comes from the session; only active customers and experts may follow; counts and lists include only active accounts.
- Add the `FOLLOW_SELF_NOT_ALLOWED` (403) and `FOLLOW_TARGET_INACTIVE` (409) error codes.
- Add follow counts, the follow button and the follower/following lists to the public profile page `/thanh-vien/:userId`.
- Add SQL Server integration tests (including parallel follow requests) and Playwright tests for the follow flows.
- Record BUG-032 under `.agents/outputs/bugs/`.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Document the follow API and error codes in `API.md` section 17 and the Backend/Frontend READMEs.

### Fixed

- None.

## 2026-10-15 — Align Recipe Reactions with Generated OpenAPI Verification ([PR #119](https://github.com/NgaiLong49423/vegetarian-support-system/pull/119))

**Status:** Committed — `b715b9b`.

**Scope:** Synchronize FR-57 after the generated OpenAPI snapshot policy was integrated into develop.

### Added

- None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Regenerate the OpenAPI YAML from the current Backend runtime, including the recipe reaction endpoints, for the shared-stack contract comparison gate.

### Fixed

- Reconcile the shared recipe detail fixture and scope browser status/error assertions to the relevant feature after reaction controls were integrated.

## 2026-10-15 — Synchronize Recipe Reactions with the Integration Baseline ([PR #119](https://github.com/NgaiLong49423/vegetarian-support-system/pull/119))

**Status:** Committed — `ef1fb5a`.

**Scope:** Integrate Issue #56 with develop while preserving recipe discovery, saving, nutrition and meal-plan flows.

### Added

- None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Display the FR-57 satisfaction badge through the shared PublicRecipeCard used by Home and Explore, preserving the current card layout and save controls.
- Integrate live reaction controls into the current recipe detail layout alongside existing recipe, nutrition and export actions.
- Preserve colliding historical bug records by remapping unmerged FR-57 BUG-017/018/019 to BUG-030/031/028 with explicit provenance.

### Fixed

- Resolve overlapping recipe UI and documentation changes without restoring outdated pages or duplicating reaction totals.

## 2026-10-15 — Generate and Verify the OpenAPI Snapshot

**Status:** Committed — `188c3c03b3f5b021e4d7ff337bb5c54432a59126`.

**Scope:** Keep the committed OpenAPI YAML aligned with the Springdoc contract served by the Backend.

### Added

- Add a PowerShell command to export `/v3/api-docs.yaml` into `docs/api/openapi.yaml`.
- Make Docker Development CI validate and compare the generated runtime YAML with the committed snapshot.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Treat Springdoc as the API contract authority and the repository YAML as its generated, checked-in snapshot.
- Update API documentation and agent guidance with the export and synchronization workflow.

### Fixed

- Permit unauthenticated access to Springdoc's `/v3/api-docs.yaml` endpoint so the export script and Docker Development CI can retrieve the generated YAML.

## 2026-10-15 — Connect Nutrition Tracking and Improve PDF Reports

**Status:** Committed — `6c576ae`.

**Scope:** Continue Issue #42 / FR-37 by using real meal-plan data in both nutrition views and making daily and weekly PDF reports readable for longer menus.

### Added

- Connect the standalone nutrition tracker to the Member's selected meal-plan day or week, including partial nutrient values and missing-data warnings.
- Render daily and weekly reports as structured tables, with landscape weekly pages and repeated headers when content continues onto additional pages.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Use the same meal-plan analysis and PDF endpoints in the tracker and meal-plan nutrition dialog.
- Preserve Backend problem details returned by binary PDF requests so the interface can show the actual failure reason.

### Fixed

- Request PDF media explicitly, preventing the API client's JSON `Accept` header from causing HTTP 406.
- Disable report export for an empty selected day or week and guide the Member to add a meal.

## 2026-10-15 — Recommend Published Recipes in AI Chat ([PR #137](https://github.com/NgaiLong49423/vegetarian-support-system/pull/137))

**Status:** Committed — c3e3ed30827c9c9f260e8eaf45950817c91bbd19.

**Scope:** Extend FR-51 chat with real public recipe recommendations and explicitly requested, non-persistent recipe drafts.

### Added

- Return recipe cards selected from current public Backend posts when users ask to find or recommend recipes; link each card to its real recipe page.
- Allow recipe creation or adaptation text only after an explicit request, without saving or publishing it.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Filter recipe candidates by the Member's declared vegetarian type and avoided ingredients before asking Gemini to select public post IDs.
- Synchronize the FR-51, BR-38, UC-51 and chat response documentation with the catalog-first behavior.

### Fixed

- Match Vietnamese recipe creation and adaptation requests against the same accent-normalized phrases used by the chat intent detector.

## 2026-10-14 — Add Contextual AI Chat for Members ([PR #137](https://github.com/NgaiLong49423/vegetarian-support-system/pull/137))

**Status:** Committed — f984505f64662bb1ae77aedbd5f9a8d47dc537df.

**Scope:** Add the Member chatbot slice for Issue #52 while keeping public Guest access closed until FR-02 rate limiting is integrated.

### Added

- Add a Backend chat endpoint for general vegetarian-cooking questions and current public RecipePost context, with ephemeral request history and token-only usage telemetry.
- Add a shared Frontend chat page with general and recipe-specific entry points, loading/error states, AI labeling and privacy guidance.
- Document the FR-51 request and response contract in the API integration guide.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Limit Gemini request timeouts to 10 seconds to follow NFR-18.
- Require an active Member session for chatbot requests until the FR-02 Guest rate limiter is available.

### Fixed

- None.

## 2026-10-14 — Add Meal Plan Nutrition Reports

**Status:** Committed — `46b6945`.

**Scope:** Issue #42 / FR-37, catalog-based meal-plan nutrition analysis, AI recommendations and PDF export.

### Added

- Add day and seven-day nutrition APIs, FDA Daily Value references, Pro recipe suggestions and on-demand A4 PDF reports.
- Show catalog-based nutrient summaries, missing-data warnings, saved planned servings and the medical information disclaimer in the Member meal planner.
- Add coverage for fractional servings, PDF routes and nutrition-reference migration behavior.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Allow planned servings from 0.5 to 10 in half-serving increments and calculate nutrients from the saved quantity.
- Keep AI provider errors separate from nutrition results and require Member confirmation before applying a suggested recipe.
- Upgrade Apache PDFBox to 3.0.8 for PDF generation.

### Fixed

- Count empty days as zero in weekly nutrition averages and divide by all seven days.

## 2026-10-14 — Synchronize FR-33 with the AI Menu Integration Baseline ([PR #135](https://github.com/NgaiLong49423/vegetarian-support-system/pull/135))

**Status:** Committed — `5b553ab`.

**Scope:** Synchronize Issue #38 with develop after the AI menu implementation was integrated.

### Added

- None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Preserve both manual entry movement/export and the integrated AI menu controllers, DTOs, UI modes and regression tests when resolving overlapping edits.

### Fixed

- Download Git LFS assets in Frontend and Docker Development CI checkouts so PDF export receives the bundled Vietnamese font rather than an LFS pointer (BUG-025).
- Update existing browser fixtures for the integrated nutrition eligibility confirmation, ingredient search and goal label while retaining authorization, payload and out-of-scope assertions.

## 2026-10-14 — Move and Export Weekly Meal Plans (FR-33) ([PR #135](https://github.com/NgaiLong49423/vegetarian-support-system/pull/135))

**Status:** Committed — `f4eeb26`.

**Scope:** Complete local meal-plan management for Issue #38 using the existing weekly plan and recipe references.

### Added

- Move an owned meal-plan entry between days and meals within the same week while retaining its identity and saved servings; reject duplicate destinations without changing the source.
- Export the authenticated Member's current schedule to UTF-8 TXT and paginated A4 PDF with embedded Vietnamese font, seven days, three meals, saved servings and calculated energy. Keep missing nutrition explicit and unavailable recipes neutral.
- Add controller/generated OpenAPI, service, browser and SQL integration checks for movement, export, ownership and unavailable content.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Extend the existing meal planner with destination selection and downloadable exports; calculate export energy from Backend per-serving nutrition and the saved serving quantity.
- Document the private export snapshot and movement contract in the API integration guide.
- Exclude generated Frontend coverage artifacts from development image build contexts.

### Fixed

- Adapt the Compose meal-plan browser test to the existing collapsed day layout and saved-recipe picker.

## 2026-10-14 — Refine Weekly Nutrition Menu Preferences

**Status:** Committed — `876a08e`.

**Scope:** Clarify weekly menu preference inputs and make excluded ingredients take precedence over preferred ingredients.

### Added

- Provide separate ingredient search and selection fields for ingredients to include and ingredients to exclude.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Rename the weekly goal prompt and remove the balancing and training goals from the weekly menu selector.
- Specify excluded weekly ingredients as hard candidate filters; preferred ingredients only influence selection among recipes that pass those filters.
- Synchronize FR-36, UC-36.1, and the API integration guide with the exclusion precedence.

### Fixed

- None.

## 2026-10-14 — Handle Nutrition Eligibility for AI Menus

**Status:** Committed — `876a08e`.

**Scope:** Return the correct eligibility response from AI menu endpoints and guide Members to the required confirmation.

### Added

- Show a link to the nutrition profile when menu generation is blocked because eligibility has not been confirmed.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Route nutrition-profile exceptions from Meal Plan endpoints through the shared ProblemDetail handler with their original status and error code.
- Document the eligibility response and confirmation endpoint for FR-36.

### Fixed

- Prevent an unconfirmed eligibility state from being reported as an internal server error.

## 2026-10-14 — Integrate AI Recipe Planning and Nutrition Menus

**Status:** Implementation complete; local checks recorded, Docker and GitHub gates pending.

**Scope:** Combine FR-34 recipe recommendations and weekly menu planning with FR-36 nutrition-aware weekly menus in the Meal Plan experience.

### Added

- Keep ingredient-based recommendations and standard weekly menus sourced from public Recipe Posts, alongside nutrition-aware weekly previews based on complete nutrition data.
- Add profile-based and one-time weekly preference modes for nutrition menus, with editable previews and explicit confirmation before replacing the target week.
- Preserve separate authorization, dietary constraints, candidate pools, and apply semantics for FR-34 and FR-36.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Integrate the three AI planning modes into the Meal Plan's “Dành cho bạn” area while keeping manual meal planning available separately.
- Document the combined API contracts and clarify nutrition-menu replacement and rollback behavior.

### Fixed

- None.

## 2026-10-13 — Compare Two Public Recipes ([PR #132](https://github.com/NgaiLong49423/vegetarian-support-system/pull/132))

**Status:** Committed — `5749ba8`, `c11e882`, `8dc7f2f`, `ffb3eae`, `a2b410b`, `c666280`.

**Scope:** Complete FR-60 (AC-60.1–AC-60.8, BR-76) in the Frontend: compare two different published recipes by overview, ingredients and the nine nutrients per serving, using the existing public recipe detail and search APIs only.

### Added

- Add the real `/so-sanh?a={recipeId}&b={recipeId}` comparison page showing the first and second recipe side by side, replacement of either recipe, removal of recipes that are no longer public, and retry on load errors.
- Add the separate `/so-sanh/chon-mon` picker page: "Chọn công thức thứ hai" (or "Chọn công thức đầu tiên") opens it with the Explore keyword search, filters, sort and paging, leaves out the recipe already chosen on the other side, lets each candidate open its recipe detail in a new tab ("Xem chi tiết", PR #132 review) so the picker keeps its search, and returns to the comparison with both recipes.
- Line up ingredients only by `ingredient_id`, keep each recipe's original quantity and unit, and flag rows whose units differ instead of converting them.
- Show the nine nutrients per serving with the original servings of each recipe and the absolute difference (first recipe − second recipe); a recipe with incomplete nutrition shows "Chưa đủ dữ liệu" and no difference.
- Add "So sánh" entry points on recipe cards, both recipe detail routes and the Member saved recipe list.
- Add Playwright tests for every acceptance criterion and the picker, and a real-stack test that uses the two seeded demo Expert recipes, chooses the second one on the picker page, checks the per-serving values against the Backend and checks that comparing only sends `GET` requests.
- Record BUG-059: adding an ingredient between 00:00 and 06:59 Vietnam time is rejected because the default reference date is checked against the Backend's UTC clock; found by the admin catalog real-stack test and not fixed in this PR.

### Changed

- Remove the prototype banner, mock data and prototype Playwright test of `/so-sanh`; the prototype detail page links to the empty comparison page.
- Move the Explore search and filter state, filter panel, sort, active filter chips and paging into a shared `RecipeSearch` module so the picker reuses them; Explore behaves as before.
- Name the two sides "Công thức đầu tiên" and "Công thức thứ hai" instead of positions A and B, as requested in the PR #132 review.
- Target recipe card links by their heading in the member profile Playwright test and the Explore real-stack test, since each card now also has a "So sánh" link with the recipe title.
- Document FR-60 in `API.md` section 7 and the Frontend README.

### Fixed

- None.

## 2026-10-13 — Add Threaded Recipe Comments (Issue #48) ([PR #129](https://github.com/NgaiLong49423/vegetarian-support-system/pull/129))

**Status:** Committed — `ede6d10` (comments), `ecc5a58` (canonical recipe page), `e2f8be8` (documentation).

**Scope:** Implement UC-46.1–UC-46.5 so guests read the comment threads of a published recipe and members comment, reply up to five levels, and edit or delete their own comments, following AC-46.1–AC-46.4 and BR-66. Administrator moderation (UC-46.6) and the banned-word filter wait for decisions Q60 and Q61.

### Added

- Add `GET /api/v1/recipes/{recipeId}/comments` (public, paginated root threads with nested replies and a comment count), `POST` on the same path to comment or reply, and `PUT`/`DELETE /api/v1/comments/{commentId}` for the owner. A reply to a level-5 comment joins its level-4 parent; a deleted comment with visible replies stays as a tombstone.
- Add the `COMMENT_NOT_FOUND` (404) and `COMMENT_EDIT_NOT_ALLOWED` (403) error codes.
- Add SQL Server integration tests and Playwright tests for the comment flows.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Replace the session-only comment preview with the API-backed comment section on both recipe detail pages; the slug-based demo recipe pages no longer show comments.
- Document the comment API and error codes in `API.md` section 18 and the Backend/Frontend READMEs.

### Fixed

- None.

## 2026-10-13 — Align Generated OpenAPI Security and Error Responses (Issue #114) ([PR #130](https://github.com/NgaiLong49423/vegetarian-support-system/pull/130))

**Status:** Committed — `a504714`, `fa04b45`.

**Scope:** Make generated `/v3/api-docs` match Backend security and error behavior, and keep API guidance aligned with the current generated-contract workflow.

### Added

- Add `OpenApiContractIntegrationTest` to verify documented security and error responses against runtime behavior.
- Add a shared `ProblemDetail` schema and document `400`, `401`, `403`, and operation-specific error responses where applicable.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Declare `bearerAuth` on the 31 protected operations that were missing it while keeping public operations unauthenticated.
- Declare operation-specific errors for the affected catalog, Expert application, subscription, recipe publishing, and authentication operations.
- Document the generated OpenAPI snapshot and its CI validation alongside the API contract rules.

### Fixed

- Fix `/v3/api-docs.yaml` returning `401` so the snapshot exporter and API consumers can retrieve it without authentication.

## 2026-10-13 — Publish Recipes With Incomplete Nutrition ([PR #131](https://github.com/NgaiLong49423/vegetarian-support-system/pull/131))

**Status:** Committed — `db03caf`, `1bfb109`, `1525b12`.

**Scope:** Complete FR-40 (AC-40.1–AC-40.3, BR-40, BR-50): recipes that use catalog ingredients without nutrition data stay publishable, each such ingredient is labelled on the recipe page, and such recipes are kept out of the nutrition AI menu candidate pool.

### Added

- Add the "Chưa hỗ trợ tính dinh dưỡng" label next to each ingredient without nutrition data on both recipe detail pages.
- Add an SQL Server integration test showing that applying a nutrition AI menu (`PUT /api/v1/meal-plans/ai-menu`, FR-36) with a published recipe that uses an ingredient without nutrition data returns `404 RECIPE_NOT_FOUND` and leaves the meal plan untouched (AC-40.3), and Playwright tests for the label.
- Record BUG-026: generated OpenAPI merges same-named nested recipe DTOs, so recipe `ingredients[]`/`media[]` schemas differ from the real responses.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Document FR-40 in `API.md` section 7 and the Backend/Frontend READMEs; type `nutritionSupported` on recipe detail ingredients in the Frontend API client.
- Rely on the FR-36 candidate filter merged in PR #134 (`AiMenuService` keeps only published recipes whose nutrition calculation is complete) for AC-40.3 instead of adding a second candidate service.

### Fixed

- None.

## 2026-10-13 — Focus Meal Planning on Individual Days ([PR #133](https://github.com/NgaiLong49423/vegetarian-support-system/pull/133))

**Status:** Committed — `2f85f34`.

**Scope:** Make the Member meal planner easier to use one day at a time while retaining weekly persistence.

### Added

- Show day and week nutrition summaries for all nine supported indicators, for one person eating one serving of each scheduled dish, independently of family cooking portions, with per-day energy totals and explicit missing-data states.
- Let Members open a planned recipe and save integer cooking servings from its detail page to the exact day/meal entry through an authenticated ownership-checked PATCH endpoint; keep the week view read-only for quantities and preserve existing database data.
- Add browser checks for saved-recipe pagination, day selection, single-serving nutrition despite family cooking portions, missing nutrition, and retry behavior.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Display seven collapsible day rows and three vertically stacked meals for the selected day, with a compact calendar on mobile.
- Limit the recipe picker to the Member's saved recipes with server-side search and pagination.
- Adapt the existing visual theme using a Google Stitch reference inspired by Samsung Food.

### Fixed

- None.

## 2026-10-13 — Expand Local Recipe Demo Fixtures ([PR #127](https://github.com/NgaiLong49423/vegetarian-support-system/pull/127))

**Status:** Committed — `c5ac7d0`.

**Scope:** Enrich the IntelliJ `local` profile with recipe, ingredient and nutrition examples for classroom demonstrations.

### Added

- Add 40 catalog ingredients and 16 illustrated recipe posts to the repeatable local seed.
- Seed per-100-gram nutrition values for all 51 demo ingredients, labeling synthetic examples as internal demo data and retaining USDA FoodData Central identifiers for sourced rows.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Add cover images to the seven existing local recipe examples so every seeded recipe has a cover.

### Fixed

- None.

## 2026-10-13 — Add Weekly Meal Planning ([PR #126](https://github.com/NgaiLong49423/vegetarian-support-system/pull/126))

**Status:** Committed — `ed72a4f`.

**Scope:** Issue #15 / FR-09, private weekly plans and recipe entries across three daily meals.

### Added

- Add idempotent week creation and authenticated add, replace and remove operations with owner checks, default servings of one, date validation and duplicate-recipe conflicts.
- Support multiple recipes per meal, published-recipe selection from public and saved lists, and replacement or removal of unavailable recipe references.
- Add service, MVC/OpenAPI, SQL Server concurrency and rollback tests, plus a real Compose browser flow with scoped fixtures and SQL assertions.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Replace the Member's read-only page with a responsive seven-day, three-meal editor while preserving the existing Guest demonstration.
- Document generated API contracts and add an optional Playwright worker limit for memory-constrained local verification without changing coverage gates.
- Publish responsive meal-planner screenshots as a CI artifact for review.

### Fixed

- Align SQL integration fixtures with the existing recipe status constraint and remove fixture Saved records before deleting their referenced recipes.
- Describe nullable meal-plan response fields in generated OpenAPI so unavailable recipe references and optional metadata match runtime JSON.

## 2026-10-12 — Introduce the Public Product Landing Page ([PR #120](https://github.com/NgaiLong49423/vegetarian-support-system/pull/120))

**Status:** Committed — `2f21e04`.

**Scope:** Introduce the agreed product story at the public root while preserving the existing application and authentication flow.

### Added

- Add a responsive, fixed-palette Landing Page for recipe discovery, saved recipes, meal planning, shopping, reference nutrition, AI assistance and expert contributions.
- Adapt reviewed Google Stitch desktop and mobile proposals into repository-native React and CSS, with actual application screenshots and accessible section navigation.
- Add browser coverage for public and authenticated visitors, mobile navigation, keyboard access, fixed appearance and Backend-independent rendering.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Move the existing application Home to `/trang-chu` and update application navigation, login destinations and affected regression tests.
- Open the public application Home from the primary Landing start actions so Guest visitors can enter without signing in or registering.
- Replace recreated preview controls with compressed screenshots captured from the application and its Compose demo data, including a dedicated mobile Home image.
- Load application routes, theme and saved-recipe state separately from the Landing Page.
- Align public metadata and the Frontend guide with the product story and route separation; preserve the existing preview indexing policy.

### Fixed

- None.

## 2026-10-12 — Add Related Recipe Recommendations ([PR #125](https://github.com/NgaiLong49423/vegetarian-support-system/pull/125))

**Status:** Committed — `81a3b5e`.

**Scope:** Issue #49 / FR-47, public related recipes and explicit subscription-gated Gemini selection.

### Added

- Add bounded SQL Server similarity ranking and public related-recipe APIs, with an optional Gemini selection restricted to current public recipe IDs.
- Show database-backed recipe cards and an explicit AI action on the recipe detail page, with retry, empty and upgrade states.
- Expose provider token metadata through an FR-11 handoff event without recording prompts or generated content.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Define principal ingredients by the largest converted gram weight and clarify partial attribute matching and deterministic ordering in FR-47 and UC-47.
- Document the runtime integration contract, role checks and remaining telemetry-consumer responsibility.

### Fixed

- Disambiguate the existing publish-flow test's description textbox from its AI action by using an exact textbox locator.
- Align the gallery publishing test's upload fixture with the current runtime API envelope.

## 2026-10-12 — Complete Ingredient Catalog Management Experience ([PR #124](https://github.com/NgaiLong49423/vegetarian-support-system/pull/124))

**Status:** Committed — `4a8bc0e`.

**Scope:** Complete the remaining Issue #46 catalog workflows and keep the administration page focused on nutrition ingredients.

### Added

- Add configured ingredient group and source choices, detailed nutrition viewing, recipe usage counts, and paginated recipe references.
- Search ingredients as the administrator types and show key nutrition values in the catalog list.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Remove unrelated unit and conversion management screens from the Issue #46 page while retaining their existing Backend endpoints.
- Allow an ingredient name to be reused after its previous catalog record becomes inactive.

### Fixed

- None.

## 2026-10-12 — Manage Ingredient Nutrition Catalog ([PR #124](https://github.com/NgaiLong49423/vegetarian-support-system/pull/124))

**Status:** Committed — `6be47a6`.

**Scope:** Implement Backend support for administrator-managed ingredient nutrition data and expose supported nutrient details with public recipe data.

### Added

- Add paginated and filtered administrator ingredient search, ingredient detail and recipe-reference endpoints, nutrition-value updates, and nutrition-support controls.
- Include ingredient group and supported per-100-gram nutrient values in public recipe details without exposing source metadata.
- Validate nutrition sources against application configuration and require complete values and source metadata before enabling nutrition calculations.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Keep catalog writes restricted to administrators and preserve existing ingredient, unit, and conversion endpoints.
- Reject hard-delete requests and retain soft status changes for catalog records.

### Fixed

- None.

## 2026-10-11 — Add AI Assistance to Recipe Authoring ([PR #122](https://github.com/NgaiLong49423/vegetarian-support-system/pull/122))

**Status:** Committed — `cfad92c`.

**Scope:** Let eligible Experts generate editable recipe descriptions and instructions without saving a server-side draft.

### Added

- Add a subscription-gated recipe-writing API backed by Gemini and integrate editable suggestions into the create and edit forms.
- Document the FR-21 API integration flow and its authorization and failure responses.
- Seed an active one-year Pro subscription for the local demo Expert so the AI authoring flow can be exercised.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Pass the optional `GEMINI_API_KEY` from the local environment into the Backend container at runtime and document the Docker configuration.

### Fixed

- Reject generated recipe instructions that reference unselected catalog ingredients or invalid ingredient IDs.

## 2026-10-11 — Preserve Theme During Browser Reload Verification

**Status:** Committed — `20b3a16`.

**Scope:** Keep the theme persistence browser test's setup from clearing its saved value during reload.

### Added

- None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Initialize the browser test's theme storage only once per test page session.

### Fixed

- Allow the theme persistence test to verify the saved light theme after reload.

## 2026-10-11 — Add Persistent Light and Dark Themes

**Status:** Committed — `cbef32a`.

**Scope:** Let users switch the application appearance and keep the selected theme between visits.

### Added

- Add light and dark theme controls to the main menu and persist the selection in browser storage.
- Add browser coverage for switching themes, applying light color tokens and restoring the selection after reload.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Apply the selected semantic color palette and native form control scheme throughout the application.
- Update the Frontend guide to describe both themes and the appearance setting.

### Fixed

- None.

## 2026-10-11 — Resolve Sonar Reliability Findings ([#118](https://github.com/NgaiLong49423/vegetarian-support-system/pull/118))

**Status:** Committed — `93e3172`.

**Scope:** Address Frontend reliability and accessibility findings reported by SonarCloud for the pull request.

### Added

- Cover native expert-application dialog visibility and Escape dismissal in the Playwright workflow.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Use the browser's native modal dialog behavior for expert-application details and keep keyboard focus on recipe links instead of their scroll container.

### Fixed

- Explicitly mark the handled Home data-loading promise chain as intentionally asynchronous.

## 2026-10-11 — Preserve Explore Filters During URL Synchronization

**Status:** Committed — `2664eea`.

**Scope:** Prevent URL synchronization from overwriting newer Explore filter selections while keeping browser navigation synchronized.

### Added

- None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- None.

### Fixed

- Keep selected Explore filters in the recipe search request when several filters are changed in succession.

## 2026-10-11 — Cover API Recipe Detail Actions in Playwright

**Status:** Committed — `a97bb58`.

**Scope:** Exercise the live API recipe-detail page's key guest interactions in the Frontend coverage suite.

### Added

- Cover serving adjustments, ingredient check-off, image selection, YouTube embedding, link sharing, TXT download, print/PDF handoff, and total nutrition display in a browser regression test.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- None.

### Fixed

- None.

## 2026-10-11 — Allow Standalone Pull Requests Without an Issue ([#118](https://github.com/NgaiLong49423/vegetarian-support-system/pull/118))

**Status:** Committed — `94bae2f`.

**Scope:** Clarify that Issue references are required for PRs implementing an assigned GitHub Issue, while independent work can explain why it has no Issue.

### Added

- Document `Refs: N/A` with a scope reason for standalone work and align the branch naming guidance and PR template.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Keep one implementation PR per assigned Issue and prohibit using `N/A` to bypass or split an existing Issue.

### Fixed

- None.

## 2026-10-11 — Show Onboarding Questions One at a Time

**Status:** Committed — `6ad21e5`.

**Scope:** Present the existing dietary-preference questionnaire as six sequential questions without changing Backend validation or persistence rules.

### Added

- Add question progress, Back/Continue navigation, retained draft answers and field-error navigation to the affected question.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Show one Onboarding question at a time, save only after final completion, and allow skipping the entire questionnaire from every step without sending a partial preference profile.
- Keep the full Settings form and explicit allergy/dislike confirmations, and document the new UI flow.

### Fixed

- None.

## 2026-10-11 — Add Dark Food Workspace and Saved Recipe Integration

**Status:** Committed — `6ad21e5`.

**Scope:** Adapt the Google Stitch layout to existing Mâm Xanh routes using a dark theme and connect saved recipes to the implemented Backend contract.

### Added

- Add public recipe save/unsave controls and a private, searchable, paginated saved-recipe page using the existing API, with Guest sign-in prompts and safe unavailable entries.
- Add a month calendar and week navigation to the existing read-only Meal Plan API.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Replace the oversized navigation and warm Explore layout with a compact dark header, photo cards, responsive filter rail, three-column Home, profile tabs and shopping checklist.
- Load Home collections and filter shortcuts from Backend data, preserve existing role controls, and label prototype screens explicitly.
- Synchronize the Frontend guide and browser regression checks with the implemented UI and API behavior.

### Fixed

- Use the neutral unavailable-recipe message required by AC-20.3 instead of asserting an author deletion in Meal Plan slots.

## 2026-10-11 — Complete API-Driven Recipe Detail and Stitch MCP Guidance

**Status:** Committed — `189b327`.

**Scope:** Complete the Frontend recipe detail experience using the existing Backend API and document the project-local Google Stitch MCP setup for agents.

### Added

- Build the recipe detail page from live API data, including ordered images, author and statistics, scaled ingredient quantities, nutrition, safe YouTube embedding, TXT download, and A4 print/PDF output.
- Document the repository-local Stitch MCP credential and startup diagnostics without exposing the local key.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Expand the Frontend recipe detail API type to match the Backend response.
## 2026-10-11 — Verify Recipe Reactions on Docker Compose (Issue #56)

**Status:** Committed — `b9d8455`.

**Scope:** Complete the previously blocked local Backend and shared-stack verification for FR-57. Remote CI, Sonar, CodeQL and post-merge acceptance remain unverified.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Pass all 319 Backend tests, including ten SQL Server reaction integration tests, with no failures, errors or skipped tests. Pass the strict JaCoCo BUNDLE gate: Lines 93.8%, Branches 82.3%, Methods 90.5%, Instructions 94.1%.
- Build and start the canonical root Compose stack using isolated project `mamxanh-fr57`; verify Frontend, Backend and SQL Server healthy and database initialization exit code 0. Verify HTTP 200 for Frontend, generated OpenAPI and the Scalar route, and generated GET/PUT/DELETE reaction operations. Scalar route smoke is not browser-rendering evidence.
- Pass 13 real browser/HTTP/SQL reaction checks on Compose, covering all five FR-57 acceptance criteria, and the existing real-stack login/throttle browser test. Clean reaction fixtures and stop the stack without removing its named volumes.
- Align the Backend environment template with existing local variable names using empty placeholders; no real configuration values are copied into tracked files.

## 2026-10-11 — Verify Recipe Reaction Acceptance and Input Validation (Issue #56)

**Status:** Committed — `b9d8455`.

**Scope:** Verify FR-57 local functional acceptance and required quality gates. Full integration acceptance remains pending.

### Added

- Add seven Controller regression cases covering invalid IDs/payloads, successful response mapping and private cache policy.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Verify 51 focused Backend tests, 45 real local HTTP/SQL checks and 13 real browser/HTTP/SQL checks after the validation fix, including all five FR-57 acceptance criteria. Clean up disposable fixtures.
- Attempt full Backend verification: 175 of 312 tests pass, while 137 Testcontainers tests fail to initialize without Docker. Supplemental partial coverage remains below every strict Backend gate: Lines 62.9%, Branches 60.1%, Methods 59.2%, Instructions 67.0%.
- Correct a management-page test fixture to use the public detail API shape. Record the workflow's outdated coverage guidance separately as BUG-028; preserve the current strict gates.
- Pass all 104 Frontend browser tests and the strict coverage gate on the final rerun: Lines 92.35%, Statements 89.13%, Functions 84.71%, Branches 83.91%. The first run failed one test because of the incorrect fixture. Compose, Sonar and CodeQL verification for these uncommitted changes remain pending.

### Fixed

- Return 400 instead of 500 for nonpositive recipe IDs on all three reaction operations by using MVC parameter validation; verify the resolution in BUG-030.

## 2026-10-11 — Integrate Recipe Reaction Frontend (Issue #56)

**Status:** Committed — `b9d8455`.

**Scope:** Complete the FR-57 Frontend stage on API-backed recipe detail, discovery and home cards. Final integration acceptance remains pending.

### Added

- Add Like/Dislike controls with authenticated choice loading, server-confirmed counts, percentage bar and selected-button cancellation through DELETE.
- Add an upper-left percentage badge on public recipe cards, displaying New for no votes and zero percent for all Dislikes.
- Add browser tests for adding, switching, cancelling and restoring reactions; guest login, author/Admin restrictions, failed requests, pending-write locking, keyboard operation and responsive layout.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Model author and statistics fields from generated OpenAPI and reuse the existing authenticated API client for reaction operations.
- Update workspace guides and API fixtures; preserve current counts on write failure and offer retry when the current choice cannot be loaded.
- Verify 27 browser/regression checks, followed by 14 focused checks on the final implementation, and a real local Guest discovery/detail browser smoke. Member browser writes against the real Backend, Compose integration and full coverage gates remain unverified.

### Fixed

None.

## 2026-10-11 — Align Recipe Reaction Statistics (Issue #56)

**Status:** Committed — `b9d8455`.

**Scope:** Complete FR-57 Backend statistics consistency across reaction, public browse and recipe detail responses. Frontend integration remains pending.

### Added

- Add a shared BR-69 percentage calculation and regression checks for rounding, no votes and all-dislike cases.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Use the same whole-number percentage in all three response paths while preserving response field types and the existing ranking algorithm.
- Verify 44 unit/regression tests and 45 real local HTTP/SQL checks, including hidden/deleted exclusion from public rankings. Docker-backed integration and full coverage gates remain unverified.

### Fixed

- Correct browse/detail responses that returned 90.91 instead of the required 91 for ten Likes and one Dislike; record the resolution in BUG-031.

## 2026-10-09 — Add Recipe Reaction Backend (Issue #56)

**Status:** Committed — `b9d8455`.

**Scope:** Implement the Backend stage of FR-57 using the existing reaction schema. This checkpoint preceded the statistics alignment recorded on October 11; Frontend integration remains pending.

### Added

- Add authenticated GET, PUT and DELETE operations for the current Member's recipe reaction, with aggregate counts, rounded percentages, private cache policy and generated OpenAPI documentation.
- Enforce author self-vote rejection and published-recipe checks; serialize reaction mutations through the existing recipe lock, preserving idempotent set/remove behavior.
- Add focused unit and SQL Server integration tests. Local verification passed 35 unit/regression tests and 25 HTTP/SQL checks; the Testcontainers run could not start without Docker.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Document the reaction contract, verification limits and pending Frontend/read-statistics work in the Backend workspace guide.

### Fixed

None.

## 2026-10-10 — Add Password Reset (Issue #9) ([PR #113](https://github.com/NgaiLong49423/vegetarian-support-system/pull/113))

**Status:** Committed — `8794862`.

**Scope:** Implement UC-03.6 and UC-03.7 so members can request a reset link by email and set a new password, following AC-03.14 and decisions Q13, Q27, Q47 and Q48.

### Added

- Add `POST /api/v1/auth/password-resets`, which always returns the same neutral `202` message and emails a 15-minute single-use link only to an existing active account outside the 60-second cooldown and under five emails per hour; unknown emails, locked accounts and rate-limited requests get no email and no `429`.
- Add `POST /api/v1/auth/password-resets/confirm`, which stores the new BCrypt hash, consumes the token and returns `204`; it also verifies an unverified email, ends a temporary login block and lets a Google-only account set a password.
- Add `PASSWORD_RESET_TOKEN_INVALID` (400) for unknown, used, replaced or expired tokens and `NEW_PASSWORD_SAME_AS_CURRENT` (400), and return `ACCOUNT_LOCKED` (403) when the account was locked after the email was sent.
- Add Flyway migration `V11__user_password_reset.sql` with the token digest, its expiry and the reset-email rate-limit columns on `USER`, plus two checks, one default and a filtered unique index on the token digest.
- Connect the forgot-password and reset-password pages to the API; the reset page keeps the token in memory and removes it from the address bar.
- Add unit, SQL Server integration (cooldown, hourly limit, token replacement and expiry, parallel requests, 72-byte passwords, log redaction) and Playwright tests.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Remove the implemented password-reset endpoints and their schemas from the planned `openapi.yaml`, and document the flow and new error codes in `API.md`.
- Synchronize `database/schema.sql`, the `database/queries.sql` expected counts and `database/README.md` with V11.

### Fixed

- None.
## 2026-10-11 — Add Public Author Profiles (Issue #29) ([PR #116](https://github.com/NgaiLong49423/vegetarian-support-system/pull/116))

**Status:** Committed — `20399b3`.

**Scope:** Implement UC-23.1–UC-23.3 so readers see who wrote a Recipe Post and can open the author's public profile, and members can edit their name, short bio and avatar, following AC-23.1–AC-23.10 and decisions Q54–Q58.

### Added

- Add `GET /api/v1/members/{userId}` (public profile: name, avatar, bio and the join month in Vietnam time, without email, role or private data) and `GET /api/v1/members/{userId}/recipes` (the member's published posts, newest first, size 1–50). A locked member keeps a public profile; an unknown id or an Administrator returns `404 MEMBER_PROFILE_NOT_FOUND`.
- Add `GET`/`PUT /api/v1/me/profile` for the signed-in member (name trimmed, 3–50 characters, no control characters; bio trimmed, at most 500 characters, empty stored as `NULL`) and `POST /api/v1/me/profile/avatar` (JPEG, PNG or WebP up to 2 MB, checked by file signature, stored through the FR-14 `StorageClient` under `avatars/`); an Administrator gets `403 MEMBER_ACCESS_REQUIRED`.
- Add the author block (avatar, linked name, publish date) to the recipe detail page, the public profile page `/thanh-vien/:userId`, the profile settings page `/ho-so/cai-dat` and a header link to it.
- Add SQL Server integration tests for the profile API, owner-only private data (AC-23.8), session-bound authorship (AC-23.9) and Administrator edit refusal (AC-23.10), plus Playwright tests for the new pages.
- Record BUG-019 and BUG-020 under `.agents/outputs/bugs/`.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Map the `USER` profile columns through a separate `MemberProfileEntity` in the `auth` module, keeping `User` as the identity view.
- Let `StorageClient` upload into a named folder; recipe images still go to `recipes/`.
- Show the member's published recipes on the profile page with the shared `PublicRecipeCard` used by Explore.
- Document the profile API and the `MEMBER_PROFILE_NOT_FOUND`, `FILE_TOO_LARGE` and `UNSUPPORTED_IMAGE_TYPE` codes in `API.md`.

### Fixed

- Fix BUG-019: set the servlet multipart limit to 5 MB per file and 6 MB per request (the 1 MB default rejected recipe images allowed by AC-14.1), and return `FILE_TOO_LARGE` instead of `INTERNAL_ERROR` for `413`.

## 2026-10-11 — Close Expert Recipe Publishing Gaps (Issue #11) ([PR #115](https://github.com/NgaiLong49423/vegetarian-support-system/pull/115))

**Status:** Committed — `3e20875`.

**Scope:** Complete the FR-04 acceptance gaps found while reviewing Issue #11 as the FR-04 coordination Issue (decisions Q49 and Q50): only the Expert author may change a Recipe Post's images, an Administrator is not an author, and a Guest who opens the create page is sent to the login page.

### Added

- Add `RecipeMediaAuthorizationIntegrationTest` (SQL Server Testcontainers) for image upload, replacement and removal by the author, another Expert, an Administrator, a missing recipe and a hidden recipe.
- Record BUG-021 and BUG-018 under `.agents/outputs/bugs/`.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Restrict `POST /api/v1/recipes/media/upload`, `PUT /api/v1/recipes/{recipeId}/media` and `DELETE /api/v1/recipes/{recipeId}/media` to `ROLE_EXPERT`; an Administrator now gets `403` (AC-04.7).
- Make the image replacement and removal endpoints use the same author check as Recipe Post editing (`RecipePostService.lockOwnEditableRecipe`): another Expert gets `403 RECIPE_EDIT_NOT_ALLOWED`, a hidden post `403 RECIPE_HIDDEN` and a missing post `404 RECIPE_NOT_FOUND`.
- Send a Guest who opens `/dang-cong-thuc` straight to `/dang-nhap` (AC-04.5); Customers and Administrators still see the access notice.
- Remove the Administrator-as-author wording from FR-04 and FR-16 in `FUNCTIONAL-REQUIREMENTS.md` so they match AC-04.7 and decision Q50.

### Fixed

- Fix BUG-021: another Expert or an Administrator could replace or delete the images of a Recipe Post they did not write, including the stored blobs.
- Fix BUG-018: replacing the images of a Recipe Post that already had images always failed with `500` because the new rows were inserted before the old ones were deleted.

## 2026-10-10 — Add Google Login and Account Linking (Issue #8) ([PR #111](https://github.com/NgaiLong49423/vegetarian-support-system/pull/111))

**Status:** Committed — `519ecbb`.

**Scope:** Implement UC-03.5 so members can sign in with Google Identity Services, following decisions Q25, Q26 and Q39–Q42.

### Added

- Add `POST /api/v1/auth/google`, which verifies the Google ID Token with Google API Client's `GoogleIdTokenVerifier` (signature, expiry, issuer and the `MAMXANH_GOOGLE_CLIENT_ID` audience) and returns the same stateless `AuthResponse` as password login.
- Find accounts by Google ID before email; create an active, verified customer from the Google profile, link verified password accounts while keeping their password, and link unverified password accounts while removing their password.
- Reject unverified Google emails and invalid tokens with `GOOGLE_TOKEN_INVALID` (401), locked accounts with `ACCOUNT_LOCKED` (403), emails linked to another Google ID with `GOOGLE_ACCOUNT_CONFLICT` (409) and unavailable Google certificates with `GOOGLE_LOGIN_UNAVAILABLE` (503).
- Add the Google Identity Services button (`@react-oauth/google`) to the login and registration pages, with the post-login Onboarding invitation and code-based error messages.
- Add `MAMXANH_GOOGLE_CLIENT_ID` and `VITE_GOOGLE_CLIENT_ID` to the environment templates and Docker Compose.
- Add unit, SQL Server integration and Playwright tests that never call Google: a self-signed certificate checks the verifier, and a stub script replaces Google Identity Services.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Let a temporary password-login block not stop Google login, without resetting the failed-attempt counter.
- Remove the implemented Google endpoint and its schemas from the planned `openapi.yaml`, and document the flow, linking rules and new error codes in `API.md`.

### Fixed

- None.

## 2026-10-09 — Manage Saved Recipes in Backend (Issue #37) ([PR #110](https://github.com/NgaiLong49423/vegetarian-support-system/pull/110))

**Status:** Committed — `6f155e4`.

**Scope:** Implement the Backend portion of FR-32 without changing Frontend source.

### Added

- Add authenticated idempotent save and unsave operations for public recipes.
- Add saved-recipe search by title or description while preserving private ownership, stable pagination, and safe unavailable tombstones.
- Add SQL Server integration coverage for persistence, authorization, search, pagination, tombstones, validation, and generated OpenAPI.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Update the API integration guide to describe the runtime Saved Recipes read and write operations.

### Fixed

None.

## 2026-10-08 — Add Public Recipe Search Filters and Explore Controls (Issue #14) ([PR #108](https://github.com/NgaiLong49423/vegetarian-support-system/pull/108))

**Status:** Committed — `60a3788`.

**Scope:** Complete the public recipe discovery flow for FR-08 with database-backed filters and sorting controls.

### Added

- Add parameterized Backend filters for vegetarian type, dish category, all selected ingredients, and maximum total preparation-plus-cooking time.
- Connect Explore filter options, ingredient lookup, pagination and six sorting modes to the public Backend API; add real SQL Server integration coverage.
- Document FR-08 filter semantics and the runtime API integration contract.

## 2026-10-07 — Add Recipe Detail and Saved Recipe Read Projections (Issue #89)

**Status:** Committed — `3ca211f`.

**Scope:** Prepare Backend read APIs for the Frontend's FR-20 recipe detail, Meal Plan, saved recipe and export integrations.

### Added

- Extend public recipe detail with public author, ordered media, nine local nutrition estimates with missing-data warnings, and reaction/view statistics.
- Add authenticated, paginated read-only saved recipe projections scoped to the current account, with safe unavailable-item responses.
- Add focused unit coverage for ingredient-specific unit conversion, per-serving nutrition, and missing nutrition data.
- Document the runtime API and map AC-20.1–AC-20.5 to Backend or Frontend ownership.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Normalize unavailable Meal Plan references to the generic `Công thức không còn khả dụng` message while retaining the existing deletion indicator.
- Return out-of-range Saved Recipes pagination as a standard validation problem and describe the bounds in OpenAPI.

## 2026-10-06 — Submit Recipe Post Reports

**Status:** Committed — `03648fe`.

**Scope:** Implement member submission of reports for published recipe posts and align the report contract with the current requirements baseline.

**Related PR:** [#103](https://github.com/NgaiLong49423/vegetarian-support-system/pull/103).

### Added

- Add authenticated report submission with reason and description validation, private receipts, and duplicate detection for open or in-review reports.
- Add Flyway V9 to preserve and migrate existing report values to the current status and reason taxonomy.
- Add SQL Server integration coverage for report storage, authorization, validation, duplicates and recipe visibility.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Align the root SRS, FR-26/27, BR-25, database snapshot and API integration guide with the approved six report reasons and description limits for the Backend contract.

### Fixed

- Correct the report reason taxonomy mismatch between the root SRS and FR-27.

## 2026-10-06 — Use Backend Data in Local Demo Flows

**Status:** Uncommitted.

**Scope:** Prepare a local demo that uses the real Backend and SQL Server for Frontend-connected flows while identifying UI-only prototypes.

### Added

- Add repeatable local fixtures for verified customer, new-member, applicant, expert and admin accounts; broader ingredient/recipe data; customer profile/preferences; an expert review application; and a complete weekly meal plan.
- Generate BCrypt demo-account passwords at local Backend startup from an ignored `.env` value; keep credentials and password hashes out of Git.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Replace Frontend role-switching/demo authentication and mock recipe listings with real session/API-backed views for connected flows.
- Keep screens without Backend endpoints as explicitly identified UI prototypes; document the local demo accounts, seed scope and reset behavior.
- Configure the local profile to include demo migrations and make Docker Compose wait for the Flyway-backed Backend before starting Frontend.
- Remove the separate SQLCMD sample-data service from Docker Compose.

### Fixed

None.

## 2026-10-05 — Enforce Strict Coverage Gates for Every FE/BE Metric

**Status:** Committed — 517ae10.

**Scope:** Make every frontend and backend coverage counter a strict, visible CI gate and preserve reports when coverage fails.

### Added

- Add exact-count strict coverage checkers and boundary regression tests for Frontend Istanbul/NYC and Backend JaCoCo counters.
- Report all four metrics, covered/total counts, coverage, required threshold and gate status in CI summaries.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Require Frontend Lines, Statements, Functions and Branches, and Backend JaCoCo BUNDLE Lines, Branches, Methods and Instructions to each exceed 80%; exactly 80% fails.
- Collect coverage summaries and artifacts after a gate failure when reports are available.

### Fixed

- Correct stale coverage policy references that described a 60% Frontend threshold or a Backend line-only gate.

## 2026-10-05 — Configure IntelliJ Development Runs and Docker SQL Port

**Status:** Committed — 2e21e79.

**Scope:** Support running the root Compose development stack from IntelliJ alongside a Windows SQL Server installation, without committing local credentials.

### Added

- Add shared IntelliJ Run configurations for Backend, Frontend, their local compound run, and the complete Docker Compose stack using the Backend env file and image builds.
- Record BUG-011: the observed IntelliJ Services Stop command omits the env file; document the verified Compose stop workaround.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Publish Docker SQL Server on loopback port 1434 while keeping its internal port 1433; document the connection port for a Backend running directly on the host.

### Fixed

- Avoid the Docker SQL startup port conflict with Windows SQL Server listening on port 1433. Local verification confirmed healthy SQL, Backend and Frontend, successful initialization services, HTTP 200 at the Frontend and generated OpenAPI endpoints, and preserved SQL volume after stopping with the env file.

## 2026-10-05 — Implement Onboarding and the Personalized AI Eligibility Gate (FR-31, Issue #36) ([PR #92](https://github.com/NgaiLong49423/vegetarian-support-system/pull/92))

**Status:** Committed — 98f5dd9, fc13b75, ab661ef, 7e20917, 06aa22f, b8dcd6a, dc0f6f8, 434add6.

**Scope:** Implement UC-31.1–UC-31.3 and AC-31.1–AC-31.10: a one-time Onboarding questionnaire for new Members, Skip without locking regular features, viewing and updating the private dietary-preference profile, and the BR-31 gate that must run before any personalized AI request.

### Added

- Add `GET`/`PUT /api/v1/nutrition/dietary-preferences`, `POST /api/v1/nutrition/dietary-preferences/onboarding/skip`, `POST /api/v1/nutrition/dietary-preferences/onboarding/invitation` and `GET /api/v1/nutrition/dietary-preferences/ingredient-suggestions` for the signed-in Member; administrators receive `403 MEMBER_ACCESS_REQUIRED`.
- Add a nutrition-owned entity for the dietary-preference and Onboarding columns of `USER` (decision Q18, `@DynamicUpdate`) and an entity for `USER_INGREDIENT_PREFERENCE`; names matching an active standard ingredient are linked to it.
- Add `DietaryPreferenceService.requirePersonalizedAiEligible`, which stops a request with `409 DIETARY_PROFILE_INCOMPLETE` and a `missing` list before any Gemini call or Meal Plan write.
- Add the Flyway migration `V7__user_onboarding_invitation.sql` with the column `USER.onboarding_invited_at`: the invitation endpoint returns `show = true` only once, for a `NOT_STARTED` account that has not been shown it, under a `USER` row lock. Accounts that existed before V7 are recorded as already invited and keep their `onboarding_status` (AC-31.10).
- Add the `MEMBER_ACCESS_REQUIRED`, `INGREDIENT_PREFERENCE_CONFLICT` and `DIETARY_PROFILE_INCOMPLETE` error codes, and let application errors carry extra problem properties.
- Add the `bearerAuth` HTTP bearer security scheme (`OpenApiConfig`) to the generated OpenAPI and require it on the dietary-preference operations.
- Add the Frontend Onboarding page `/khoi-tao-so-thich`, the Settings page `/ho-so/so-thich-an-uong` with the missing-information notice, and the redirect to Onboarding when the invitation endpoint returns `show = true` after sign-in.
- Add SQL Server integration tests for AC-31.1–AC-31.10 (including the invitation shown once, parallel claims and the V6→V7 upgrade), validation, access, privacy and the generated OpenAPI, a problem-detail unit test, `TC41` in `database/queries.sql`, and API-stubbed Playwright scenarios for the Onboarding and Settings flows.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Point the "Sở thích ăn uống" menu item to the new Settings page, stub the invitation call in the existing login browser tests, record the invitation as already shown for the real-stack login test account, and document the endpoints, rules and error codes in `docs/api/API.md`, `database/schema.sql`, `database/README.md` and both app READMEs.

### Fixed

- Stop inviting a Member again after the questionnaire was left without Skip or Complete, and stop marking existing accounts as `SKIPPED` (PR #92 review F001).
- Publish `minimum: 1` for `maxCookingTimeMinutes`, matching the validator that rejects `0` (PR #92 review F002).
- Declare bearer security for the dietary-preference operations in the generated OpenAPI (PR #92 review F003).

## 2026-10-05 — Record Password Login Follow-Up Database Checks (FR-03-B, Issue #6)

**Status:** Committed — 7757560.

**Scope:** Apply the Tech Lead decision Q29 to the database guide and replace the unverified post-sync notes with results measured on a clean SQL Server 2019 database after V1–V6.

### Added

None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Remove `last_failed_login_at` from the FR-03 target design in `database/README.md`, because no Acceptance Criterion uses it.
- Record that `database/schema.sql` matches a database built by V1–V6 with 0 differences in columns, CHECK, DEFAULT, index and FK definitions, and that `database/queries.sql` passes 78/78 assertions on both databases.

### Fixed

None.

## 2026-10-04 — Implement Expert Application Review (Issue #68) ([PR #86](https://github.com/NgaiLong49423/vegetarian-support-system/pull/86))

**Status:** Committed — ad6764d.

**Scope:** Add the Customer submission/history and Admin review workflows for expert applications, including the atomic role promotion and in-app decision notice.

### Added

- Add the Expert Application and Notification backend services, runtime API endpoints, SQL Server migration V8, and API-backed Frontend workflow.
- Add request-validation, service/controller, SQL Server repository, promotion-eligibility and competing-decision concurrency coverage.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Extend the package specification and workspace guides for the implemented modules and migration.
- Replace the FR-05 demo workflow with authenticated API submission, history, review, conflict recovery and role display.

### Fixed

- Read UTC timestamps from injected clock instants across expert promotion and decision notifications so UTC-only test clocks work through the full review transaction.

## 2026-10-04 — Recipe Post Frontend Management (Issue #47)

**Status:** Committed — `c144b85`.

**Scope:** Add the Frontend experience for Experts to edit and delete their own public Recipe Posts from the expert profile.

### Added

- Add the recipe API client, expert profile recipe list, edit form, separate delete action, confirmation dialog and success feedback.
- Add Playwright scenarios for the edit and delete flows using mocked API responses.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Return to the expert profile after saving and remove a deleted recipe from that list.
- Keep recipe management actions on the expert profile instead of the public recipe detail page.

### Fixed

- None.

## 2026-10-04 — Prepare Recipe Ingredient Entry for Issue #25

**Status:** Committed — f8a90152dcf5ccf88a96f4120d36295ef6bb6af5.

**Scope:** Prototype ingredient quantity input and add repeatable Docker conversion samples while the Recipe Post API was pending Issues #11/#22.

### Added

- Validate positive ingredient quantities and the then-agreed 100g minimum/step for direct `g`/`kg` input.
- Add ingredient-specific conversion fixtures for tofu, tomato, water spinach, garlic, and black pepper.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Prototype 1–50 ingredient rows with free-text names and local-only publish feedback before the Recipe Post API was available.

### Fixed

- Remove the demo's misleading successful-publish message before API integration.

## 2026-10-04 — Correct GitHub CLI Authentication Checks in Codex

**Status:** Working tree — not committed.

**Scope:** Prevent Codex from reporting a failed GitHub CLI login solely because its Windows sandbox cannot access the user's keyring or network.

### Added

None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Require an authorized out-of-sandbox authentication check and separate keyring, network, and credential failures before asking a user to sign in again.

### Fixed

None.

## 2026-10-04 — Fix Docker Development JWT Configuration

**Status:** Committed — 7371bf6.

**Scope:** Restore the Docker Development CI gate by passing the required JWT signing secret to the Backend under its configured environment-variable name.

### Added

None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Align the Docker Compose variable and contributor setup instructions with the Backend's `MAMXANH_JWT_SECRET` configuration.

### Fixed

- Generate and pass `MAMXANH_JWT_SECRET` in the Docker Development workflow so the Backend starts during the integration smoke test.
- Preserve SQL client output in Playwright failures so integration query errors are actionable in CI logs.
- Enable `QUOTED_IDENTIFIER` for test SQL sessions that update tables with filtered indexes.
- Suppress `sqlcmd` row-count output so integration assertions parse only query results.
- Target the password input uniquely when the visible-password control shares its accessible label.

## 2026-10-04 — Complete Password Login Integration Evidence (Issue #6)

**Status:** Committed — cfb7f9c.

**Scope:** Verify password login through the real Frontend, Backend and SQL Server Compose stack, and align the implementation and integration guidance with approved authentication decisions.

### Added

- Add a Docker Development Playwright integration test that verifies browser login, failed-login persistence, temporary blocking and successful counter reset against SQL Server.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Use 60-minute JWT access tokens and tab-scoped `sessionStorage`; document the accepted temporary account-enumeration risk.
- Require the JWT signing secret in the Compose Backend and generate a disposable secret in CI.
- Remove password login from the planned OpenAPI reference now that it is implemented at runtime.

### Fixed

- Reuse the shared SQL Server integration-test context for the ingredient catalog database tests so required JWT configuration is present.

## 2026-10-03 — Complete FR-18 Review Fixes ([PR #75](https://github.com/NgaiLong49423/vegetarian-support-system/pull/75))

**Status:** Committed — f2e571df269d8522764b7b776b1ff5d18eb2a363.

**Scope:** Synchronize the catalog-management feature with the develop baseline, preserve ordered schema migrations, and address verified FR-18 review findings.

### Added

- Add database-side insert-only handling for concurrent duplicate ingredient-unit conversion requests.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Align conversion and unit numeric validation with their SQL Server decimal precision and scale.
- Reuse the shared Frontend API client and expose Bearer-token injection for the pending login integration.
- Move the ingredient-group migration to V5 after the email-verification V3 and nutrition-consent V4 migrations.

### Fixed

- Preserve the complete coverage HTML reports, Authentication contract, and catalog form context during status changes.
- Keep the conversion ingredient lookup independent from filtered search results and use the local calendar date for reference-date defaults.
- Reject removal of a required source URL before updating nutrition-supported ingredients.

## 2026-10-04 — Synchronize Password Login with the Develop Baseline (FR-03-B, Issue #6)

**Status:** Committed — 998e404.

**Scope:** Resolve the authentication feature branch conflicts with the current `develop` baseline while preserving the nutrition and ingredient-catalog schema changes.

### Added

None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Move the account login-throttle migration to V6 after the existing V4 nutrition-consent and V5 ingredient-validation migrations.
- Integrate real authenticated account state with the role-aware demo navigation, shared API client and nutrition requests.
- Align the schema snapshot, verification query and API/database guides with the merged baseline.

### Fixed

- Keep demo role switching limited to demo sessions and clear demo state when a real user signs in or leaves the account.

## 2026-10-03 — Recipe Post Backend API and Data Handling (Issue #47)

**Status:** Committed — `06e7b0d`.

**Scope:** Add the Backend API and persistence behavior needed to edit and soft-delete an Expert's own public Recipe Post.

### Added

- Add public Recipe Post detail/search and reference-data APIs, plus author-only edit and soft-delete endpoints.
- Add Backend service tests for ownership, direct publication after update, hidden recipes, soft deletion and validation.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Resolve the active Expert from the authenticated server principal; do not accept an author ID from the client.
- Block authors from opening or editing a recipe hidden by an Administrator.

### Fixed

- Preserve Recipe Post rows as tombstones so existing Meal Plan foreign-key references remain intact and exclude deleted recipes from public search.

## 2026-10-03 — Fix Docker Sample Data Seed ([PR #79](https://github.com/NgaiLong49423/vegetarian-support-system/pull/79))

**Status:** Committed — `7107e5e`.

**Scope:** Make the SQL Server 2019 development seed compatible with filtered indexes and capture the container logs when the Docker gate fails.

### Added

- Print Compose service status and Backend/seed logs before CI cleanup when Docker Development fails.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Enable `QUOTED_IDENTIFIER` before inserting sample data.

### Fixed

- Resolve SQL Server error 1934 when the sample seed inserts into tables with filtered indexes.

## 2026-10-02 — Fix Docker Development and Scalar Security Gates ([PR #81](https://github.com/NgaiLong49423/vegetarian-support-system/pull/81))

**Status:** Committed — `0360577`.

**Scope:** Resolve the PR's Docker Development failure and the Sonar security rating finding without weakening validation.

### Added

- Add Subresource Integrity (SRI) and anonymous CORS mode to the version-pinned Scalar UI asset.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Pin the Scalar CLI workflow runtime to Node.js `24.21.0`, meeting the CLI's Node.js `>=24` requirement.

### Fixed

- Resolve the Docker Development Node.js engine mismatch and the Sonar finding for the external Scalar script.

## 2026-10-02 — Pin and Harden the Docker Development Stack ([PR #81](https://github.com/NgaiLong49423/vegetarian-support-system/pull/81))

**Status:** Committed — `9a2ae5e`.

**Scope:** Make SQL Server and Backend integration tests consume one verified image, restrict local port exposure, and document safe Compose project/volume lifecycles.

### Added

- Pin the MCR tag `2019-CU32-GDR11-ubuntu-20.04` with verified repository manifest digest `sha256:ef0b8db33970ecd01bed49c3a84a1d083c435a9891718df619298b67b352e74a` in Compose and Testcontainers; bind development ports to loopback.
- Add a Compose one-off `npm ci` workflow for refreshing the Frontend dependency volume and document consistent project-name use, SQL-only reset, and full-volume reset boundaries.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Use `/opt/mssql-tools18/bin/sqlcmd` consistently and pass the local SQL password through `SQLCMDPASSWORD` rather than a command-line argument.
- Keep the `Docker Development` context and shared stack while naming its Compose project consistently for startup and cleanup.

### Fixed

- Replace floating SQL Server `2019-latest` references in Compose and Testcontainers with the same verified manifest-pinned image.

## 2026-10-02 — Add Scalar API Reference and Runtime OpenAPI Validation

**Status:** Committed — 596443c.

**Scope:** Make Scalar the team's local API reference UI and validate the generated runtime contract through the existing Docker Development gate.

### Added

- Serve a version-pinned Scalar browser UI at Backend `/scalar`, retain Swagger UI compatibility, and apply the existing public API-documentation access policy.
- Capture and validate generated `/v3/api-docs` in Docker Development, upload a run-specific artifact, and smoke-test only the Scalar route/HTML shell.
- Add WebMvc/security tests for the public Scalar route and document separate browser acceptance for actual rendering and manual API requests.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Establish generated OpenAPI as runtime contract; retain the manual YAML as planned/reference contract during migration and update agent lookup/governance rules.
- Pin Docker Development Node.js and Scalar CLI versions; align the documented required CI context with the existing gate.
- Identify Scalar as the official human-facing API documentation/manual testing UI without treating it as automated regression evidence.

### Fixed

- Correct stale machine-readable API documentation and required-CI-context rules that conflicted with the active repository policy.

## 2026-10-02 — Clarify One-Time Onboarding for New Members (FR-31, Issue #36)

**Status:** Committed — 101b218.

**Scope:** Specify when the optional onboarding questionnaire is shown and how members can return to it after skipping.

### Added

- Add acceptance coverage for one-time onboarding invitations, no automatic re-prompt after Skip, and manual access through Settings.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Clarify that only newly registered Members are invited after their first successful authentication; existing accounts are not prompted automatically.
- Clarify that skipping preserves normal features and does not prevent later completion from Settings.

### Fixed

None.

## 2026-10-02 — Standardize the Docker Development Stack and Add a CI Gate

**Status:** Committed — 2ff0254.

**Scope:** Provide one Docker Compose development environment for Frontend, Backend and SQL Server, and require a GitHub Actions build/smoke-test check before merging changes into the integration baseline.

### Added

- Add root `docker-compose.yml` with health-ordered SQL Server, database initialization, Backend and Frontend services, plus persistent local database/dependency volumes.
- Add the `Docker Development` GitHub Actions job to build and smoke-test the shared stack using an ephemeral SQL Server password.
- Document the shared Compose setup, PowerShell startup/verification/shutdown commands, local-secret handling, volume safety, and the rules for members and agents in CONTRIBUTING.md, AGENTS.md and component guides.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Define root Compose as the canonical FE/BE/SQL Server integration-test entry point; retain per-app Dockerfiles for component builds without duplicating CI gates.
- Add `Docker Development` to the documented required validation contexts and the PR synchronization checklist, and distinguish its smoke coverage from feature/acceptance tests.

### Fixed

None.

## 2026-10-02 — Add Reproducible Docker Compose Development Environment ([PR #79](https://github.com/NgaiLong49423/vegetarian-support-system/pull/79))

**Status:** Committed — c2bac47.

**Scope:** Provide one repeatable local environment for Frontend, Backend and SQL Server 2019 with persistent local development data.

### Added

- Add Docker Compose services for SQL Server, database creation, Backend, sample data and Frontend, plus a guarded command to reset the local database volume.
- Add deterministic fictional recipe fixtures that are safe to apply repeatedly and a data-only demo author without login credentials.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Route the Frontend API proxy to the Backend service name in Compose while retaining the localhost default for direct development.
- Document per-machine Docker credentials, startup, persistence and reset behavior.

### Fixed

- Populate the previously empty sample-data script so a clean Docker database receives the documented fixtures.

## 2026-10-02 — Force Overlapping Requests in the Concurrent Resend Test (FR-03-A, Issue #5) ([PR #74](https://github.com/NgaiLong49423/vegetarian-support-system/pull/74))

**Status:** Committed — 95fd0ef.

**Scope:** Address review finding F-03: the concurrent resend regression test did not prove that the requests overlapped, so it could pass even without the row lock.

### Added

None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Hold the `USER` row lock in a test transaction while five resends start, and release it only after SQL Server reports all five requests waiting on the lock. The test now passes with the lock (1×202, 4×429, one email) and fails without it (5×202).

### Fixed

None.

## 2026-10-01 — Implement Nutrition Profile Reference Flow

**Status:** Committed — db6b690.

**Scope:** Implement the FR-35 nutrition profile vertical slice within the existing Member profile model, while preserving the application's non-clinical, informational boundary.

### Added

- Add authenticated nutrition profile read, save and eligibility-confirmed calculation endpoints, with the matching OpenAPI contract and a consent timestamp migration.
- Add a responsive green Frontend flow for self-reported profile data, eligibility confirmation, approximate BMI, energy and eight nutrient targets, source links, and the required informational disclaimer.
- Add unit coverage for reference calculations, consent, saved-profile behavior, and rejection of unauthenticated or out-of-scope access.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Keep calculated results out of profile reads and saves; return results only after a separate eligibility-confirmed calculation request.
- Record API integration guidance for the FR-35 endpoints and the pending real JWT integration boundary.
- Remove the feature-specific E2E runner and tests while retaining the nutrition functionality and the repository's general Playwright setup, as requested.

### Fixed

- Prevent previously calculated results from reappearing after reload or after eligibility answers change.
- Record the manual SQL confirmation of current consent columns and constraint as schema-state evidence only; fresh-database Flyway execution and authenticated end-to-end acceptance remain unverified.

## 2026-10-01 — Serialize Verification Email Resends (FR-03-A, Issue #5) ([PR #74](https://github.com/NgaiLong49423/vegetarian-support-system/pull/74))

**Status:** Committed — 9ba491d, fcc74a8.

**Scope:** Address the PR #74 review: concurrent resend requests could all pass the 60-second cooldown, several boundaries were untested, and the pull request must leave `docs/diagrams/` unchanged.

### Added

- Add regression and boundary tests: five concurrent resends after the cooldown accept exactly one request, keep only its token and send one email; a resend at 59 seconds is rejected and at exactly 60 seconds is accepted; display names of 3 and 50 characters are accepted and 51 are rejected.
- Assert that integration tests started through `MamXanhApplication.main` use only the `test` profile, the SQL Server Testcontainers database and the applied Flyway history.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Revert the Data Dictionary edits made earlier in this pull request (the two `USER` columns, decisions Q13, Q15 and Q16, and the `google_subject` trace correction). Per the Tech Lead decision on 2026-10-01, files under `docs/diagrams/` stay a reference baseline until the final documentation phase; `database/README.md` now says so.

### Fixed

- Lock the `USER` row while checking the resend cooldown and replacing the verification token, so concurrent requests are serialized and only the accepted one publishes an email event.

## 2026-10-01 — Implement Password Login and Temporary Login Blocking (FR-03-B, Issue #6)

**Status:** Committed — d9b6ef4, f3deae8, e5da120, 3057480.

**Scope:** Implement UC-03.4 (AC-03.6–AC-03.9, NFR-07), client-side logout (AC-03.13) and the account-status check of NFR-09 end to end: password login that issues a Stateless JWT access token, a ten-minute per-account block after five consecutive wrong passwords, validation of Bearer tokens on every protected request, and the Frontend session that uses them.

### Added

- Add `POST /api/v1/auth/login` returning `AuthResponse` (HS256 access token, `tokenType`, `expiresInSeconds`, `AccountSummary`) with neutral `INVALID_CREDENTIALS` answers for unknown emails, wrong passwords and password-less Google accounts, and `EMAIL_NOT_VERIFIED` or `ACCOUNT_LOCKED` only after a correct password.
- Add Flyway migration `V4__user_login_throttle.sql` with `USER.failed_login_attempts` and `USER.login_blocked_until`; the fifth consecutive wrong password blocks login for ten minutes with `429 LOGIN_TEMPORARILY_BLOCKED` and `Retry-After`, checked before the password and without changing `account_status`.
- Add Spring Security OAuth2 Resource Server validation of Bearer tokens (signature, issuer, expiry against the application clock) and re-read `USER` on every authenticated request so a `LOCKED` account is rejected immediately with `403 ACCOUNT_LOCKED`.
- Add the required `MAMXANH_JWT_SECRET` setting (at least 32 bytes, never printed) and configurable `mamxanh.auth.access-token-ttl`, `max-failed-login-attempts` and `login-block-duration` properties.
- Add unit and SQL Server integration tests for AC-03.6–AC-03.9, concurrent wrong passwords, other accounts staying unaffected, expired, tampered, foreign-signed and orphan tokens, immediate administrative lock and log redaction, plus TC40 in `database/queries.sql`.
- Add the Frontend login flow: `POST /auth/login` from the login page, messages chosen by problem `code` (neutral invalid credentials, unverified email with a resend link, administrative lock, temporary block with the remaining minutes), and a session kept in `localStorage` behind a single storage function while the storage decision is pending.
- Add an `AuthContext` with client-side logout, automatic logout when the token expires, cross-tab synchronization, and an Axios interceptor that sends `Authorization: Bearer` and ends the session on `401` or `403 ACCOUNT_LOCKED`.
- Add API-stubbed Playwright scenarios for login, logout without a server call, login error codes, expired stored sessions and token expiry.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Lock the `USER` row during a login attempt so parallel requests cannot bypass the failure count.
- Replace the `DemoAccount` context with `AuthContext`; the explicit demo preview remains a separate, token-free mode, and the header shows the real account name, email and a logout action for real sessions.
- Synchronize `database/schema.sql` with V4 (200 columns, 59 CHECK and 56 DEFAULT constraints) and document the login error codes, token claims and pending token parameters in `docs/api/API.md` and `docs/api/openapi.yaml`.

### Fixed

None.

## 2026-10-01 — Compute the Verification Resend Cooldown with Time Zone-Aware Values (FR-03-A, Issue #5) ([PR #74](https://github.com/NgaiLong49423/vegetarian-support-system/pull/74))

**Status:** Committed — 528d966.

**Scope:** Resolve the SonarQube Cloud reliability issue (rule `java:S8700`) that failed the Quality Gate on PR #74.

### Added

None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
None.

### Fixed

- Compute the `Retry-After` duration for `RESEND_TOO_SOON` from values bound to the application clock zone instead of plain `LocalDateTime` values, so the result stays correct if the clock zone ever observes daylight saving time.

## 2026-10-01 — Synchronize Email Registration with the Develop Baseline (FR-03-A, Issue #5) ([PR #74](https://github.com/NgaiLong49423/vegetarian-support-system/pull/74))

**Status:** Committed — 7f80186.

**Scope:** Merge the current `develop` integration baseline into the FR-03-A branch and adapt the registration work to the new environment-configuration policy and coverage gates.

### Added

- Add the FR-03 mail, CORS and frontend-link variables to `app/mamxanh-backend/.env.example`, matching the `.env` import used by the `local` profile.
- Add a unit test proving that verification emails are skipped when the SMTP host is blank.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Resolve mail settings through explicit `SPRING_MAIL_*` placeholders so values also load from the local `.env` file, and treat a blank SMTP host as email not configured.
- Start the shared integration-test context through `MamXanhApplication.main`, combining the entry-point check from `develop` with the SQL Server Testcontainers context.
- Use the coverage-collecting Playwright fixture in the registration and email-verification browser tests.
- Remove `application-local.properties.example` following `develop` and move its optional Brevo settings into `.env.example`.

### Fixed

None.

## 2026-09-30 — Enforce Coverage Gates and Separate Sonar Validation

**Status:** Committed — 38e0226.

**Scope:** Enforce the approved Frontend and Backend coverage policies, validate demo UI behavior, and document the required-check setup for develop integration.

### Added

- Add meaningful browser tests for recipe search, shopping list editing/export, recipe draft rows, community article rating, profile preferences, and nutrition day navigation.
- Add a JaCoCo BUNDLE LINE COVEREDRATIO check at 0.80 and exercise the scaffold application entry point in its context test.
- Define the external Ruleset setup for required Frontend, Backend, Sonar and relevant CodeQL checks, keeping Kody/Gemini review advisory.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Replace baseline-derived Frontend thresholds with 60% for lines, statements, functions and branches; any metric below its threshold fails the command.
- Run Frontend/Backend jobs independently and transfer LCOV, JaCoCo XML and Java bytecode through artifacts to a separate Sonar job that waits for its Quality Gate.
- Align testing, contributor, tooling and agent documentation; record the distinction between repository workflow configuration and GitHub enforcement awaiting separate approval.

### Fixed

- Wait for the recovery form before entering email in the navigation test.
- Correct stale documentation claiming that build/test CI or coverage gates were not configured.

## 2026-09-30 — Add Reproducible Playwright E2E Coverage Gate

**Status:** Committed — 5c20a71.

**Scope:** Add a repository-managed Playwright and Istanbul/NYC coverage workflow for the frontend, CI and SonarQube.

### Added

- Add portable npm coverage scripts, shared E2E coverage collection, HTML/LCOV/text reports, and baseline-derived thresholds of 59% lines, 54% statements, 42% functions, and 53% branches.
- Run the Chromium coverage gate in CI and pass its LCOV artifact to the SonarQube scan after backend JaCoCo generation.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Document the normal E2E and E2E coverage commands in the Frontend testing guide.

### Fixed

None.

## 2026-09-30 — Align Authentication Documentation for Q27 and Login Blocking Field

**Status:** Committed — a9c5f0f.

**Scope:** Align active authentication documentation with the approved password-reset privacy behavior and temporary login-block field name.

### Added

None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Clarify that password-reset rate limits suppress email delivery while the request retains the same neutral `202 Accepted` response.
- Use `login_blocked_until` as the canonical temporary login-block field name.

### Fixed

- Remove active Use Case wording that described password-reset rate limiting as returning `429`.

## 2026-09-30 — Define Develop Integration Baseline and PR Synchronization

**Status:** Committed — a9c5f0f.

**Scope:** Establish contributor and coding-agent documentation for concurrent feature integration against `develop`.

### Added

- Define merged `develop` content as the current integration baseline and document synchronization, conflict review, shared-infrastructure reuse, Flyway collision handling, and scope-appropriate re-validation.
- Align the agent entry point, machine-readable repository contract, implementation workflow, and `develop` PR checklist with the contribution policy.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
None.

### Fixed

None.

## 2026-09-28 — Implement Email Registration and Verification (FR-03-A, Issue #5) ([PR #74](https://github.com/NgaiLong49423/vegetarian-support-system/pull/74))

**Status:** Committed — 87076f9, efc4f75, 4915a96, c7b36bb.

**Scope:** Implement UC-03.1–UC-03.3 (AC-03.1–AC-03.5) end to end under the stateless JWT baseline and decisions Q13–Q18: account registration, email verification and verification-email resend in the Backend and Frontend, plus the shared error, security and integration-test foundation that later FR-03 issues and other modules reuse.

### Added

- Add `POST /api/v1/auth/register`, `/auth/email-verifications` and `/auth/email-verifications/resend` following `docs/api/openapi.yaml`: accounts are created `ACTIVE` with `email_verified = false`, passwords are hashed with BCrypt (work factor 12), and verification links use single-use 256-bit tokens valid for 24 hours with a 60-second resend cooldown.
- Add Flyway migration `V3__user_email_verification_token.sql` storing only the SHA-256 digest and expiry of the current verification token on `USER`, with a filtered unique index and a pairing check constraint.
- Add a shared FR-03 password rule (8–64 characters, at most 72 UTF-8 bytes, uppercase, lowercase and digit) that reports each unmet criterion, and mirror it in the Frontend.
- Add `application/problem+json` error handling with stable `code` values and field `errors`, a stateless Spring Security baseline with ProblemDetail 401/403 responses, and a CORS allowlist configured by `MAMXANH_CORS_ALLOWED_ORIGINS`.
- Add best-effort verification emails through `spring-boot-starter-mail` (Brevo SMTP), sent asynchronously after commit and skipped with a warning when SMTP is not configured.
- Add Testcontainers SQL Server integration tests covering AC-03.1–AC-03.5, CORS, 401 responses and the absence of passwords and tokens in logs.
- Add the Frontend API client (Axios), registration and email-verification integration, and `.env.example` for `VITE_API_BASE_URL`.
- Add test case TC39 to `database/queries.sql` for the new token pairing check and the unique token-digest index.
- Add the implemented error codes to `docs/api/API.md` and the matching response descriptions to `docs/api/openapi.yaml`.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Run the application-context test against SQL Server through Testcontainers instead of disabling the DataSource and Flyway; `./mvnw verify` now requires a running Docker daemon.
- Replace the demo-only registration Playwright scenario with API-stubbed registration and verification scenarios, labelled as Frontend-only evidence.
- Document mail, CORS and frontend-link environment variables and the Docker requirement in the Backend README, the API base URL in the Frontend README, and migration V3 in the database guide.
- Synchronize `database/schema.sql` with V3 (22 tables, 198 columns, 58 CHECK constraints, 56 indexes) and record the two `USER` columns and decisions Q13, Q15 and Q16 in the Data Dictionary.

### Fixed

- Replace the Frontend registration hint that suggested special characters with the approved FR-03 password rule.
- Correct the Data Dictionary trace for `USER.google_subject` from UC-03.8 to UC-03.5.

## 2026-09-27 — Finalize Requirements Baseline v2 and Prepare Issue Reconciliation ([PR #72](https://github.com/NgaiLong49423/vegetarian-support-system/pull/72))

**Status:** Committed — 30c5d0a.

**Scope:** Stabilize the maintained requirements baseline for implementation planning while preserving prior requirements as history. GitHub Issue reconciliation is prepared separately and remains pending user approval.

### Added

- Add current Use Case specifications with Acceptance Coverage mappings to canonical Acceptance Criteria, preserving stable requirement and Use Case IDs.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Define current implementation scope by presence in the Requirements Baseline v2 registry; retain the v1 requirements baseline in the archive.
- Align current requirements and contracts with Like/Dislike reactions, feature-based AI entitlements, and the implemented SQL Server schema/Flyway baseline.
- Align account verification and API role documentation with the approved account-state and concrete-role models.

### Fixed

- Correct stale account verification wording and Recipe View traceability; preserve Like/Dislike requirements instead of restoring historical star-rating behavior.
- Prepare current and historical GitHub Issue reconciliation for review without changing any remote Issue.

## 2026-09-27 — Fix Unit Code Encoding and Conceptual ERD Connectors After the PR #66 Merge

**Status:** Committed — 3917ead.

**Scope:** Post-merge verification of PR #66 on `develop` found that Vietnamese unit codes lost their diacritics and that the conceptual ERD update merged through PR #67 had re-attached or detached several relationship connectors. Fix both on a branch from `develop` without editing the already shared V1 migration, and restore the chronology and accuracy of the PR #66 changelog entries.

### Added

- Add Flyway migration `V2__unit_code_unicode.sql` that widens `UNIT.code` from `VARCHAR(20)` to `NVARCHAR(20)` and restores the `quả`, `củ` and `miếng` codes that V1 stored as `qu?`, `c?` and `mi?ng` under the default `SQL_Latin1_General_CP1_CI_AS` collation.
- Add test case TC38 to `database/queries.sql`, asserting that Vietnamese unit codes are stored without loss and that a duplicate code is rejected by `UQ_UNIT_code`.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Synchronize `database/schema.sql` with the V1 plus V2 state, using `NVARCHAR(20)` and Unicode seed literals for `UNIT.code`.
- Update the `UNIT.code` type label on the physical ERD to `NVARCHAR(20) NOT NULL`, and re-export the conceptual and physical ERD images from their updated sources.
- Update `docs/diagrams/ERD/data-dictionary.md` to `v0.7.2`, `database/README.md` to `v0.4.2` and `docs/diagrams/ERD/README.md` to `v1.13.2` for the V2 migration, the 38-case test suite and the current data dictionary version.
- Reorder the changelog entries of 2026-09-23 to 2026-09-25 into reverse chronological order, record the verified PR #66 commits, and add the missing logical ERD and data dictionary entries.

### Fixed

- Fix the conceptual ERD connectors changed by PR #67: attach `owns` from `USER` instead of `EXPERT_APPLICATION` to `MEAL_PLAN`, attach `reacts` from `USER` instead of `RECIPE_VIEW` to `RECIPE_REACTION`, bind both ends of `comment_trigger` to `COMMENT` and `NOTIFICATION`, remove a stray `SAVED_RECIPE` self-loop, and align the `reacts` and `receives_reactions` labels with the relationship matrix, restoring 36 connectors for 36 relationships.
- Correct the PR #66 entry, which still reported an uncommitted working tree and an inaccurate count of 182 custom indexes.

## 2026-09-25 — Finalize Database Modeling, Physical ERD, Schema Constraints, and Integrity Test Suite ([PR #66](https://github.com/NgaiLong49423/vegetarian-support-system/pull/66))

**Status:** Committed — f4b9cd2, 9be5058 and de893df; merged into `develop` through PR #66 as 1acbb8a.

**Scope:** Complete Phase 2 database modeling for Issue #63 addressing Tech Lead review rounds 1 and 2 on PR #66: finalize SQL Server 2019 baseline migration and bootstrap schema with all business check/unique/composite constraints, resolve Draw.io container table layout formatting for physical ERD, update physical data dictionary, and expand the verification test suite to 37 test cases.

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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Update `V1__baseline_schema.sql` and `database/schema.sql`:
  - Make all 9 `INGREDIENT` nutrition columns nullable (`NULL`) while preserving default `nutrition_supported = 0` (Q5).
  - Change `COMMENT.content` from `NVARCHAR(2000)` to `NVARCHAR(1000)` and add constraint `CK_COMMENT_content_len` (FR-46).
  - Tighten `PAYMENT_TRANSACTION.amount_vnd` check constraint `CK_PAYMENT_amount` from `>= 0` to `> 0` because free tier generates no transaction (Q9).
- Update `docs/diagrams/ERD/physical-erd-v1.0.0.drawio` and exported image `physical-erd-v1.0.0.drawio.png`: format all 22 tables and 196 physical columns with exact data types, nullability, primary/foreign/unique/check constraints, default values, and index indicators (Review Round 1 Point 1); synchronize 37 Crow's Foot connectors from Logical ERD v1.0.0; resolve container table layout styling to ensure correct row ordering and visible headers; synchronize `COMMENT.content` to `NVARCHAR(1000)` and `CK`.
- Update `docs/diagrams/ERD/data-dictionary.md` to version `v0.7.0`: remove all pending `⏳` markers for implemented items, update Section 1 status, document Phase 2 completion in Section 8, and record `CK_COMMENT_content_len` and `CK_PAYMENT_amount` (> 0) in Sections 4.10, 4.22, and 5.
- Update `database/README.md` to version `v0.4.0` with object counts, the 37-case test suite and `sqlcmd` execution instructions.
- Update `docs/diagrams/ERD/README.md` to version `v1.13.0` with physical metrics on line 16.

### Fixed

- Fix Draw.io table row ordering and header occlusion for `COMMENT` and `MEAL_PLAN_ENTRY` by positioning added physical columns sequentially in table geometry.
- Fix negative length assertion test in `database/queries.sql` to be UTF-8 encoding agnostic across CLI environments.
- Correct the index counts published in `database/README.md`, `docs/diagrams/ERD/README.md` and the data dictionary to the measured 55 indexes (22 primary keys, 9 unique constraints and 24 standalone indexes, of which 10 are filtered: 8 unique and 2 non-unique), and align data dictionary version references to `v0.7.1` (de893df).

## 2026-09-25 — Confirm Vercel and Azure Deployment Baseline

**Status:** Committed — d1c2082.

**Scope:** Record the approved hosting split across maintained architecture, setup, workflow, and overview documentation without claiming that deployment automation or the complete production system already exists.

### Added

- None.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Configure the Frontend container for Vite development on port 5173 with file watching and no browser launch inside the container.
- Configure the Backend container for Java 21 and Maven Wrapper development on port 8080 while keeping database credentials outside the image.
- Keep Docker as local development tooling with no shared root Dockerfile, database container, Docker Compose setup, or production deployment change.
- Align the C4 workspace guide with the four current architecture areas, external providers, request/response flows, Vercel deployment view, and known diagram corrections still required before approval.

### Fixed

- None.

## 2026-09-24 — Apply PR #66 Review Decisions to the Logical ERD and Data Dictionary ([PR #66](https://github.com/NgaiLong49423/vegetarian-support-system/pull/66))

**Status:** Committed — 2b5d25d and 827353e.

**Scope:** Record the Tech Lead decisions Q5 to Q12 from the PR #66 review in the logical model and the data dictionary so that phase 2 can implement them, and repair the logical ERD connectors found during the relationship audit.

### Added

- Add `date_of_birth`, `nutrition_goal`, `onboarding_status`, `avoid_none_confirmed` and `dislike_none_confirmed` to `USER` in the logical ERD and the data dictionary (Q7, Q12).
- Add the missing relationship 17 connector between `RECIPE_POST` and `SAVED_RECIPE` to the logical ERD.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Record decisions Q5 to Q12: nullable nutrition indicators, `reviewed_by` without a conceptual connector, date of birth with the 18 to 120 age check in the service layer, technical columns with composite foreign keys for cross-row rules, paid-only subscriptions with at most one active subscription, `AVOID` and `DISLIKE` preference types, and four activity levels.
- Specify the pending round 1 and round 2 constraints in the data dictionary with pending markers and a phase 2 handover list.
- Flag the open conflicts between Q5 and `FR-41`/`AC-41.2`, and between Q7 and the birth-year wording of `FR-35`, until the SRS is updated.
- Remove the superseded `logical-erd-v0.1.0.drawio` draft and its exported image.

### Fixed

- Re-attach nine logical ERD connectors whose ends were not bound to a table, and align two relationship labels with the relationship matrix.

## 2026-09-23 — Implement Physical ERD, SQL Server Baseline Migration, and Data Integrity Test Suite

**Status:** Committed — ae61073.

**Scope:** Complete Phase 2 database modeling for Issue #63, delivering the physical ERD, Flyway baseline schema migration, standalone bootstrap script, comprehensive physical data dictionary, and automated integrity validation query suite for Microsoft SQL Server 2019.

### Added

- Add Flyway baseline migration `V1__baseline_schema.sql` under `app/mamxanh-backend/src/main/resources/db/migration/` establishing 22 core tables, 38 foreign keys, 21 indexes (including 5 filtered unique indexes), 41 check constraints, 52 default constraints, and initial seed data for 15 standard measurement units without JDBC-incompatible batch separators.
- Add standalone bootstrap script `database/schema.sql` with `GO` batch delimiters and ANSI settings (`SET ANSI_NULLS ON;`, `SET QUOTED_IDENTIFIER ON;`) for SSMS and `sqlcmd`.
- Add `docs/diagrams/ERD/physical-erd-v1.0.0.drawio` defining the physical ERD with exact SQL Server 2019 data types, primary keys, foreign keys, and 36 entity relationship connectors in Crow's Foot notation, along with exported diagram image `docs/diagrams/ERD/physical-erd-v1.0.0.drawio.png`.
- Add comprehensive diagnostic and automated test suite in `database/queries.sql` featuring schema catalog auditing, 15 transactional positive and negative test cases verifying key constraints and cascade path behaviors, and core operational queries.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Update `docs/diagrams/ERD/data-dictionary.md` to version `v0.4.0`, completing all nine physical specification columns across 22 tables (178 entity columns), documenting the resolution of SQL Server multiple cascade path Error 1785, and checking off Phase 2 delivery milestones.
- Update `database/README.md` to version `v0.2.0` with step-by-step guidance for running the Flyway baseline migration, executing `schema.sql`, and running the automated test suite.
- Update `docs/diagrams/ERD/README.md` to version `v1.11.0` linking the newly created Physical ERD and updating workspace status.

### Fixed

- Eliminate multiple cascade path conflict (SQL Server Error 1785) across interrelated entities (`COMMENT`, `USER_FOLLOW`, `REPORT`) by restricting `ON DELETE CASCADE` strictly to four parent-child ownership relationships, applying `SET NULL` for guest recipe views, and setting the remaining 33 foreign keys to `NO ACTION`.

## 2026-09-23 — Add Logical ERD v1.0.0 and Data Dictionary Baseline ([PR #66](https://github.com/NgaiLong49423/vegetarian-support-system/pull/66))

**Status:** Committed — cb404f7.

**Scope:** Rebuild the logical ERD on the confirmed 22-entity conceptual baseline and add the data dictionary and traceability matrix that phase 2 uses as its specification.

### Added

- Add `docs/diagrams/ERD/logical-erd-v1.0.0.drawio` with 22 tables, including `EXPERT_APPLICATION` and `USER_FOLLOW` with separate `follower_user_id` and `followed_user_id` roles.
- Add `docs/diagrams/ERD/data-dictionary.md` covering every column of the 22 tables with logical types, relationships, FR/BR/UC/AC traceability and constraints that a connector cannot express.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Remove `CATEGORY`, `RECIPE_CATEGORY` and `RECIPE_STEP`; add `instructions`, `dish_category` and the like, dislike and view counters to `RECIPE_POST`; replace sodium with zinc in `INGREDIENT`.
- Record decisions Q1 (eleven fixed `dish_category` codes) and Q2 (stored interaction counters as a documented third normal form exception).

### Fixed

- None.

## 2026-09-23 — Align Social, Recipe Comparison, Nutrition, and Expert Requirements

**Status:** Committed — 6e49cd9.

**Scope:** Align the conceptual data model and requirement semantics for required social following, public two-recipe comparison, nutrition analysis/PDF reporting, and expert applications without claiming implementation or creating GitHub Issues.

### Added

- Add `FR-59` and `BR-75` for directed Member follow relationships and represent `USER_FOLLOW` in the 22-entity Conceptual ERD.
- Add active `FR-60` and `BR-76` for comparing exactly two distinct public recipes by overview data, ingredients, and nine per-serving nutrition indicators.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
- Synchronized the out-of-scope sections in PRD Section 5 and SRS Section 3.2 to clarify the future M13/FR-56 decomposition.

### Fixed

- None.

## 2026-09-14 — Basic Shopping List MVP Adoption

**Status:** Working tree — not committed.

**Scope:** Move the basic Shopping List capability into the official MVP under Module M05 with three functional requirements (`FR-53`, `FR-54`, `FR-55`) and safe ingredient-aggregation rules.

### Added

- Added FR-53 (Shopping List management, ad-hoc items, purchased checkbox), FR-54 (safe aggregation for matching ingredient IDs and units, grouped by store section), and FR-55 (clipboard copy and `.txt` export) to SRS Section 7.2 with lifecycle `ACTIVE`.

### Changed

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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

- Split the local-only demo repeatable into ordered catalog, recipe, member-activity, community-moderation, and shopping-list files.
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
