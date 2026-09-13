---
name: srs-to-github-issues
description: Convert PRD, SRS, product specs, requirement documents, or planning documents into traceable GitHub Issue drafts, optionally create approved GitHub Issues, and optionally sync GitHub Project metadata. Use this when the user asks to decompose requirements into issues, create GitHub issue drafts, estimate story points, assign labels, or prepare project-ready work items.
risk: critical
source: self
source_type: custom
version: v1.6.0
created_date: 2026-06-27
last_updated_date: 2026-09-13
---

# SRS to GitHub Issues

Convert confirmed requirements into professional, traceable GitHub Issue drafts without changing requirement meaning or inventing implementation scope.

Default behavior is safe: create draft files only. Do not create real GitHub Issues, update GitHub Projects, create branches, close issues, or modify source code unless the user explicitly asks.

## When to Use

Use this skill when the user asks to:

- Convert SRS, PRD, specs, requirements, user stories, or planning docs into GitHub Issues.
- Split implementation-ready requirements into work items.
- Create issue drafts with traceability, acceptance criteria, labels, priority, story points, and relationships.
- Create real GitHub Issues from approved drafts.
- Sync issue metadata into a GitHub Project.
- Estimate issue story points or generate an issue index.

Do not use for ordinary coding, bug fixing, PR review, or CI debugging unless the user specifically asks to create or manage GitHub Issues from requirements.

## Core Safety Rules

### No Invention

Do not invent product behavior, technical choices, workflows, actors, labels, Project fields, dates, dependencies, requirement status, readiness, or relationships.

If information is missing, use `TBD`, `Unknown`, `Needs Review`, or ask the authorized decision-maker when the missing information blocks decomposition.

### Preserve Requirement Meaning

Issue decomposition may split or group implementation work, but it must not silently change the semantics of an accepted requirement.

Do not change actors, conditions, business rules, obligations, exceptions, thresholds, expected outcomes, or acceptance behavior merely to make an Issue easier to implement.

### Stable Requirement IDs

Preserve requirement identifiers exactly as defined by the authoritative source.

- Do not renumber requirements.
- Do not reuse retired requirement IDs.
- Do not invent child IDs that do not exist in the source requirement set.

## Source of Truth and Governance

Before drafting:

1. Follow applicable repository governance that is already available to the execution environment.
2. If governance is not already available and `AGENTS.md` exists, inspect it before making repository-specific assumptions.
3. Inspect `CONTRIBUTING.md` when the task depends on contribution workflow, issue policy, labels, branch conventions, review rules, or team process.
4. If `.agents/repo-contract.yml` or another repository contract is explicitly adopted by the repository, follow it for configured paths and document authority.
5. Determine authority by concern rather than using a universal file ranking.

Typical concern ownership when the repository does not define a stronger rule:

- Product goals / high-level direction -> PRD or equivalent product document.
- Detailed required system behavior -> SRS / detailed requirements.
- Business rules -> authoritative BR section or BR document.
- Architecture structure -> architecture documentation.
- Important technical decisions and rationale -> ADR / decision records.
- GitHub workflow and contribution policy -> repository governance / contribution docs.

If two authoritative sources for the same concern conflict, do not silently choose one. Report the conflict and stop decomposition of the affected scope until it is clarified.

Do not infer document authority or readiness solely from a version prefix such as `v0.x.x` unless repository policy defines that convention.

## Repository Inspection

Inspect only files relevant to the task. Common candidates include:

- `AGENTS.md`
- `CONTRIBUTING.md`
- `.agents/repo-contract.yml`
- `README.md`
- `PRD.md`
- `SRS.md`
- `requirements.md`
- `SPEC.md`
- `docs/requirements/`
- `docs/decisions/`
- `.github/ISSUE_TEMPLATE/*.yml`
- `.github/labels.yml`

Do not hard-code a specific decision filename when repository governance can discover the actual source.

If the repo uses the standard template and no stronger path convention exists, prefer:

- `docs/requirements/SRS.md`
- `docs/requirements/PRD.md`
- `.agents/outputs/drafts/github-issues/`
- `.agents/outputs/reports/`

## Requirement Eligibility

Before converting a requirement into an implementation Issue, determine its lifecycle and readiness when those states exist in the source or repository convention.

Default interpretation:

- `ACTIVE` + `Ready` -> eligible for implementation decomposition.
- `ACTIVE` + `Ready with open items` -> eligible only when open items are explicitly non-blocking.
- `ACTIVE` + `Needs clarification` -> not implementation-ready; record as `Needs Review` rather than creating an approved implementation Issue.
- `DRAFT` -> not implementation-ready by default.
- `DEFERRED` -> do not create current implementation work by default.
- `OUT_OF_SCOPE` -> do not create current implementation work.
- `RETIRED` -> do not create current implementation work.

Do not invent lifecycle or readiness states if the project does not use them. In that case, infer only what the source explicitly supports and flag uncertainty when it materially affects issue creation.

Detailed guidance: `references/requirement-eligibility.md`.

## Traceability Rule

Every requirement-derived Issue must trace back to at least one authoritative source reference: FR, NFR, UC, business rule, product goal, requirement section, or spec heading.

If an implementation Issue cannot be traced to a source document, do not create it as a requirement-derived Issue.

Traceability must not create fake artifacts. If no Use Case or Test Case exists, do not invent one merely to make the Issue look complete.

## Operating Modes

### Draft Mode

Default mode. Create Markdown issue drafts only.

Default output:

- `.agents/outputs/drafts/github-issues/ISSUE_INDEX.md`
- numbered issue draft files such as `001-module-short-title.md`

Draft mode must not create real issues, update Projects, create labels, create branches, commit changes, or modify code unless explicitly asked.

When regenerating drafts:

- Clean or archive stale draft files according to repository convention.
- Ensure every draft file listed in `ISSUE_INDEX.md` exists.
- Ensure every `.md` draft file in the final draft directory is referenced by `ISSUE_INDEX.md`.
- Do not leave stale, duplicate, or unreferenced drafts.
- Do not create duplicate implementation drafts for both a parent capability FR and its implementable child FRs.

### GitHub Creation Mode

Explicit approval required.

Rules:

- Create issues only from drafts listed in `ISSUE_INDEX.md`.
- Do not scan every `.md` file in the draft directory.
- Default: create only items with `Draft State = Approved`.
- If the user specifies IDs/ranges, create only those drafts.
- If the user says create all drafts, create all eligible listed drafts.
- Do not create implementation Issues from `DEFERRED`, `OUT_OF_SCOPE`, `RETIRED`, unresolved `DRAFT`, or blocking `Needs clarification` sources unless the authorized decision-maker explicitly changes the requirement state or asks for a non-implementation tracking Issue.
- After creation, update `ISSUE_INDEX.md` with issue number, URL, Draft State `Created`, and date if available.

Run `references/github-creation-preflight.md` before creating.

If a preflight check fails, stop and report it. Do not create partial issues unless the user explicitly approves continuing.

### GitHub Project Sync Mode

Explicit approval required.

Before syncing, verify repository owner/name, Project owner/number/ID, field IDs, option IDs, and issue item IDs.

Do not sync if required IDs cannot be determined confidently.

Normal fields to sync when available:

- Type
- Story Points
- Priority
- Start date
- Target date

Relationship sync is optional and best-effort only. Do not treat relationship sync failure as full workflow failure.

## Decomposition Strategy

Prefer implementation-ready leaf requirements rather than blindly applying one Issue to every FR identifier.

Default rule:

- A top-level FR that represents a business capability normally groups work and provides traceability context.
- An implementable child/leaf FR normally maps to one implementation Issue when its behavior is independently testable and ready.
- Do not create a parent FR implementation Issue that duplicates the implementation scope already covered by child FR Issues.

Allowed exceptions:

- Split one leaf FR if it is too large, risky, uncertain, or contains multiple independently testable implementation slices.
- Group multiple small leaf FRs only when they are strongly coupled, implemented in the same workflow, and grouping does not hide meaningful traceability.
- A standalone FR with no child hierarchy may map directly to one implementation Issue.

Prefer vertical slices: each implementation Issue should represent meaningful, testable behavior.

Detailed guidance: `references/decomposition-rules.md`.

## Epic and Relationship Rules

Use parent/epic Issues only when grouping adds planning value.

A parent capability may become an Epic or parent Issue when it coordinates multiple implementation work items, but do not create an Epic merely because an arbitrary child-count threshold was reached.

Every draft Issue must include relationship fields:

- Parent
- Blocked by
- Blocking

Relationship sync to GitHub Project is optional and best-effort only.

Never guess relationship field IDs or target issue IDs.

## Definition of Ready

`Draft State` in `ISSUE_INDEX.md` describes the issue-generation lifecycle. It is not the GitHub Project `Status`.

Before an Issue moves from planning into active implementation, verify the repository's Definition of Ready when one exists. At minimum, ensure:

- The objective and Source Trace are clear.
- The source requirement is eligible for implementation.
- Scope is sufficiently clear and Acceptance Criteria are testable.
- Blocking requirement ambiguity is resolved.
- Dependencies and blockers are identified when known.

Owner, estimates, priority, and dates follow repository/team policy. Do not invent them merely to satisfy a generic template.

If this repository explicitly uses the sizing/timebox conventions in `references/sizing-priority-rules.md`, apply them. Otherwise treat those values as optional guidance rather than universal rules.

## Story Points, Priority, Dates

Use repository/team conventions when defined.

If the repository uses the default sizing guidance in this skill, use:

- `references/sizing-priority-rules.md`

Dates default to `TBD`. Do not invent dates. Fill dates only when the user or repository schedule provides them.

## Labels and Templates

Before assigning labels:

1. Read `.github/labels.yml` if present.
2. Use only labels defined there unless the user explicitly allows new labels.
3. Follow repository issue types when defined.
4. If no repository-specific type system exists, the skill may use its default issue types from `references/issue-body-template.md`.
5. Secondary labels must not replace the primary issue type when the repository requires one.

Before drafting, inspect `.github/ISSUE_TEMPLATE/*.yml` when present.

Required issue body sections are defined in:

- `references/issue-body-template.md`

## Title and Branch Rules

Follow repository naming conventions when defined.

Fallback recommendation:

- Issue title: clear professional English.
- Issue body: Vietnamese by default unless the user requests another language.
- Suggested branch: `<prefix>/<short-kebab-case-title>`.

Do not create branches. Only suggest branch names.

## Technical Constraint Rule

Include implementation constraints, technical choices, algorithms, storage mechanisms, architecture decisions, APIs, or data structures only when authoritative source documents explicitly define them.

Do not add new technical solutions as facts.

If a useful technical suggestion is not in the source documents, keep it out of the requirement-derived Issue draft and ask the user before finalizing.

## Token-Saving Workflow

For large documents, do not perform planning, draft generation, real issue creation, and Project sync in one pass unless explicitly asked.

Use phases:

1. Planning: create `ISSUE_INDEX.md`, traceability table, proposed issue list, estimates, exclusions, and dependencies.
2. Draft Generation: create selected full Issue draft files after approval.
3. GitHub Creation: create real Issues only from eligible drafts listed in `ISSUE_INDEX.md`.
4. Project Sync: sync normal Project fields after Issues exist.
5. Final Report: concise result table only.

Do not print full Issue bodies in chat unless the user asks.

## Coordination With Documentation Skills

When a general documentation-governance skill is available, defer shared documentation semantics to it, including:

- requirement lifecycle meaning;
- stable ID behavior;
- source-of-truth resolution;
- requirement readiness semantics;
- impact analysis for requirement changes.

This skill owns the mechanics of turning eligible source requirements into GitHub work items. It must not redefine project requirements merely to make Issue generation easier.
