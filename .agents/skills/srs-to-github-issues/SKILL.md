---
name: srs-to-github-issues
description: 'Convert SRS requirements into traceable GitHub work items and keep linked Issues synchronized when the SRS changes. Use this skill to draft, create, reconcile, update, defer, reopen, or close requirement-linked Issues; maintain an Issue index; estimate work; assign repository-approved labels; and optionally sync verified GitHub Project metadata. Requirement lifecycle status must be explicit before Issue creation or reconciliation; never guess it.'
compatibility: >
  Draft and planning modes work without GitHub write access. Real GitHub synchronization requires an
  authenticated runtime with repository-scoped GitHub read/write capability and a human approval gate
  before the first remote write. No specific agent vendor or client is required.
metadata:
  swp391-risk: "critical"
  swp391-source: "self"
  swp391-source-type: "custom"
  swp391-version: "v2.0.1"
  swp391-created-date: "2026-06-27"
  swp391-last-updated-date: "2026-09-16"
---

# SRS to GitHub Issues

Turn the authoritative SRS into a traceable GitHub work-item model and keep that model synchronized as requirements evolve.

The SRS owns requirement meaning. GitHub Issues are a planning/execution mirror; they must not silently redefine the SRS.

Default behavior is safe: draft and plan locally unless the current task or adopted repository governance explicitly authorizes real GitHub mutations.

## When to Use

Use this skill when the user asks to:

- convert SRS functional requirements into GitHub Issue drafts or real Issues;
- create or refresh a requirement-to-Issue index;
- synchronize linked Issues after SRS edits;
- update Issue title/body/lifecycle labels when a requirement changes;
- defer, reopen, or close linked Issues when requirement lifecycle changes;
- split/group work while preserving requirement traceability;
- estimate story points, priority, dependencies, or planning metadata;
- sync verified GitHub Project metadata.

Do not use this skill as the primary tool for ordinary coding, PR review, CI debugging, or unrelated GitHub administration.

## Core Safety Rules

### 1. Never Invent Requirement State

Do not invent product behavior, technical choices, workflows, actors, labels, Project fields, dates, dependencies, relationships, lifecycle status, or readiness.

Every functional requirement managed by this skill must have an explicit lifecycle state from the authoritative requirement source or an explicit authorized decision.

Supported fallback vocabulary:

```text
DRAFT
ACTIVE
DEFERRED
OUT_OF_SCOPE
RETIRED
```

If the repository uses equivalent names, map by meaning rather than renaming them.

If a requirement has no explicit lifecycle state, **stop Issue generation/reconciliation for that requirement and ask the authorized decision-maker to choose the state**. Do not infer it from wording, file location, version number, implementation progress, or whether an Issue already exists.

### 2. SRS Requirements Participate in Issue Management

For an authoritative SRS, do not independently decide that a confirmed FR is "not worth tracking" merely because it seems small, difficult, or low priority.

Every FR with an explicit lifecycle state must appear in the managed requirement-to-Issue index.

Issue treatment is determined by the explicit lifecycle state and requirement hierarchy, not by agent preference.

This does not mean every SRS section becomes an implementation Issue. Actors, glossary entries, explanatory text, BRs, NFRs, constraints, and references are linked to relevant work unless repository policy or the user explicitly manages them as independent work items.

### 3. Preserve Requirement Meaning and Stable IDs

Issue decomposition may split or group implementation work, but it must not silently change accepted requirement semantics.

Preserve requirement identifiers exactly.

- Do not renumber requirements.
- Do not reuse retired IDs.
- Do not invent child IDs that do not exist in the SRS.
- Do not delete historical Issue links merely because a requirement changes lifecycle.

### 4. GitHub Mutations Require Previewed Batch Approval

Draft/index generation and GitHub reads are local/read-only preparation work.

Before any real GitHub write:

1. complete lifecycle, mapping, and repository preflight checks;
2. build one complete mutation preview grouped by `CREATE`, `UPDATE`, `CLOSE`, `REOPEN`, and `PROJECT_FIELD_UPDATE` as applicable;
3. show the affected requirement/Issue identifiers and the evidence/reason for every planned action;
4. stop before the first remote write and request one human approval for the complete previewed batch;
5. execute only the approved batch;
6. require a new preview/approval if targets or actions materially change;
7. verify remote state after execution and report partial failures.

A generic instruction such as "sync the docs" is not remote-write approval. An earlier approval does not authorize a materially changed batch. Approval never permits actions forbidden by `.agents/POLICY.md`.

## Source of Truth and Governance

Before planning or synchronization:

1. Follow applicable repository governance already supplied by the runtime.
2. If needed, inspect the applicable `AGENTS.md`.
3. Inspect `CONTRIBUTING.md` when Issue workflow, labels, branches, reviews, or team process matter.
4. If the repository explicitly adopts `.agents/repo-contract.yml` or another repository contract, use its configured paths and concern ownership.
5. Resolve authority by concern rather than by a universal file ranking.

Typical fallback ownership:

- detailed required behavior -> SRS / detailed requirements (monolithic or registered modular requirement set);
- business rules -> designated BR source;
- architecture -> architecture documentation;
- important decision rationale -> ADR / Decision Record;
- GitHub workflow -> repository governance / contribution policy;
- work-item state -> GitHub, but only as a mirror of requirement lifecycle and implementation progress.

When a Modular SRS Requirement Set is used:
- Root SRS is the Authoritative Registry for requirement existence, stable IDs, index-level module allocation, and authoritative lifecycle state.
- Registered child documents are Authoritative Detailed Specifications for detailed requirement statements, actors/triggers, preconditions, exceptions, and acceptance criteria.
- Requirement eligibility and lifecycle gate are resolved directly from the authoritative lifecycle registry in root SRS.
- Detailed requirement content is then hydrated from the corresponding registered child document via stable anchors (`#fr-xx`).
- If a lifecycle conflict occurs between root SRS and child document, root SRS wins; use the root SRS lifecycle and record a sync warning.

If equally authoritative requirement sources conflict, stop affected synchronization and report the conflict.

## Repository Inspection

Inspect only what is needed. Common candidates:

- `AGENTS.md`
- `CONTRIBUTING.md`
- `.agents/repo-contract.yml`
- Document Register in `docs/README.md` (to resolve registered modular SRS requirement sets)
- `docs/requirements/SRS.md` or root SRS
- Registered child requirement specifications (e.g. `docs/requirements/srs/*.md`)
- `docs/decisions/`
- `.github/ISSUE_TEMPLATE/*.yml`
- `.github/labels.yml`
- the repository's existing Issue index/mapping artifact

Fallback generated-working-artifact path when no stronger convention exists:

```text
.agents/outputs/drafts/github-issues/
```

Generated drafts/indexes are working artifacts, not authoritative project requirements.

## Mandatory Lifecycle Gate

Before creating drafts, real Issues, or reconciling existing Issues:

1. enumerate the requirements in the authoritative SRS registry (root SRS);
2. verify that each requirement has an explicit lifecycle state in the registry;
3. list missing/ambiguous states;
4. ask the authorized decision-maker to resolve them;
5. continue only for requirements whose lifecycle is explicit;
6. resolve detailed content (statements, acceptance criteria) from the registered child requirement documents.

Do not substitute a separate agent-inferred "eligibility" decision for this lifecycle gate.

Default lifecycle-to-Issue treatment:

- `DRAFT` -> keep in the index; planning/tracking Issue may exist, but do not present it as implementation-ready.
- `ACTIVE` -> maintain an open current implementation/tracking Issue.
- `DEFERRED` -> maintain a future/backlog Issue and mark it deferred according to repository conventions; do not place it in active implementation automatically.
- `OUT_OF_SCOPE` -> do not create new implementation work; if a linked open Issue exists, close it as not planned when authorized and preserve the link/history.
- `RETIRED` -> do not create new work; preserve historical mapping and close still-open linked work as not planned when authorized.

Read `references/requirement-eligibility.md` for lifecycle handling.

### Readiness Is Secondary

If the repository explicitly tracks `Ready`, `Ready with open items`, or `Needs clarification`, synchronize that information.

Do not invent readiness and do not use missing readiness as a reason to ignore an FR that already has an explicit lifecycle. Readiness controls whether an Issue is presented as implementation-ready, not whether the FR disappears from Issue management.

## Traceability Rule

Every requirement-derived work item must trace to the authoritative source ID/section. In a modular SRS set, trace to both the root SRS registry and the detailed specification anchor.

For SRS FRs, maintain at minimum:

```text
FR ID <-> Issue Index entry <-> GitHub Issue (when one exists)
```

Where UC/BR/NFR/Test artifacts exist, preserve real references. Do not fabricate artifacts to make traceability look complete.

## Managed Issue Index

`ISSUE_INDEX.md` is the local registry for requirement-to-Issue synchronization when the repository has not defined another registry.

It must record, at minimum:

- requirement ID;
- lifecycle;
- hierarchy role (`Parent/Capability`, `Leaf`, or `Standalone`);
- linked draft(s);
- linked GitHub Issue number/URL when available;
- Issue state/disposition;
- synchronization state;
- notes for split/group/superseded relationships.

Use `references/issue-index-template.md`.

## Operating Modes

### 1. Planning / Draft Mode

Default mode.

Create/update:

- `ISSUE_INDEX.md`;
- selected Issue draft files;
- traceability and reconciliation notes.

Do not mutate GitHub in this mode.

When regenerating:

- update existing drafts instead of duplicating them;
- preserve stable mappings where possible;
- remove or archive stale generated drafts according to repository convention;
- never delete historical GitHub mappings from the index merely because the current requirement is no longer active.

### 2. GitHub Creation Mode

Create real Issues only after preflight **and approval of the exact mutation batch preview**.

Rules:

- create from the managed index/drafts, not by scanning arbitrary Markdown;
- `ACTIVE` requirements create/open current work;
- `DEFERRED` requirements may create/open backlog work, clearly marked deferred;
- `DRAFT` requirements may create planning/tracking Issues only when the requested workflow wants draft tracking;
- `OUT_OF_SCOPE` and `RETIRED` requirements do not cause creation of new implementation Issues;
- parent/capability Issues must not duplicate child implementation scope;
- update the index immediately after successful creation.

Run `references/github-creation-preflight.md`.

### 3. Issue Reconciliation Mode

Use after SRS semantic, lifecycle, or hierarchy changes when linked Issues already exist or Issue sync is part of the requested workflow.

Reconcile by stable requirement ID, not by title matching alone.

Typical actions:

- title/wording changed -> update the managed Issue fields when meaning remains the same;
- semantic requirement change + open unfinished Issue -> update managed scope/acceptance content;
- semantic requirement change + already-completed Issue -> preserve the completed Issue as history and create/link a follow-up change Issue for new work unless repository policy explicitly prefers reopening;
- `ACTIVE -> DEFERRED` -> keep linked Issue as backlog/deferred according to repository policy;
- `DEFERRED -> ACTIVE` -> restore it to current work rather than creating a duplicate;
- `ACTIVE/DEFERRED -> OUT_OF_SCOPE` -> close linked unfinished Issue as not planned when authorized;
- `ACTIVE/DEFERRED -> RETIRED` -> close linked unfinished Issue as not planned when authorized;
- `OUT_OF_SCOPE/RETIRED -> ACTIVE` -> reopen the historical linked Issue when appropriate and not already completed, otherwise create a new follow-up Issue and preserve the old link;
- requirement split -> preserve original mapping as parent/superseded context and create child mappings;
- requirement removed from the active SRS without an explicit lifecycle/history decision -> do not guess; stop and ask before closing anything.

Never delete Issues or comments as part of reconciliation.

Read `references/issue-reconciliation.md`.

### 4. GitHub Project Sync Mode

Include Project-field mutations in the previewed batch. Explicit approval of that batch is required before mutation.

Verify repository owner/name, Project owner/number/ID, field IDs, option IDs, and Issue item IDs before mutation.

Do not guess Project IDs or option values.

Relationship sync is best-effort unless repository workflow makes it mandatory.

## Decomposition Strategy

The Issue model must cover every managed FR without duplicating implementation scope.

### Delivery-Decomposition Rule

A Functional Requirement represents required product behavior and must not automatically be treated as a single developer task. For each ACTIVE FR, determine whether the requirement is:
1. **Standalone delivery work**:
   - Small enough for one implementation owner;
   - Primarily one delivery surface (e.g. backend telemetry, background cron, pure UI presentation).
   - Maps to a single Implementation Issue/Task.
2. **Cross-surface feature work**:
   - Requires coordinated Frontend, Backend, external integration, or infrastructure work (e.g. Recipe creation, Meal planning, Shopping list).
   - Maintain one **Parent Feature Issue** as the requirement-facing source of end-to-end implementation scope, business rules, field constraints, and overall Acceptance Criteria.
   - Create child **Delivery Task Issues** (e.g., `[FE]`, `[BE]`, `[INT]`) only for the surfaces actually needed.
   - Every child Task MUST reference the parent FR and parent Feature Issue.
   - Child Tasks MUST NOT redefine business behavior or invent unapproved contracts.
   - Parent Feature completion requires all required child Tasks and end-to-end Acceptance Criteria to pass.
3. **Multi-story capability work** (e.g. FR-03 Identity & Access):
   - Slice vertically by User Story / Use Case first (e.g. Registration, Login, Google SSO, Password Reset) before creating delivery tasks. Avoid creating monolithic tasks for giant requirements.

### Frontend Task Rule

A Frontend child Issue MUST contain enough information for a Frontend developer to implement the user-facing behavior without reopening the SRS. It should include, when applicable:
- Target screens and routes (e.g. `/recipes/create`);
- Required UI components and layout;
- Displayed data and form fields;
- Client-side validation rules (exact bounds, formats);
- User interactions and flow;
- UI States: `initial`, `validation_error`, `submitting`, `loading`, `api_error`, `success`;
- API contracts or contract dependencies (endpoints, payload shape, expected responses);
- Authentication/authorization presentation behavior (redirecting guests, token handling);
- Responsive layout (Mobile vs Desktop) and accessibility constraints;
- Frontend-specific Acceptance Criteria.

It MUST NOT redefine server-side business rules, invent database schema, or fabricate unapproved backend APIs.

### Backend Task Rule

A Backend child Issue MUST contain enough information for a Backend developer to implement the server-side API and business logic without reopening the SRS. It should include, when applicable:
- REST API endpoint specification (HTTP Method, path, request/response DTO contracts);
- Server-side validation (Bean Validation, cross-field rules);
- Security, authentication, and authorization/ownership enforcement;
- Business logic and state transitions;
- Persistence behavior (referencing approved data models, not invented tables);
- External integration handling (Azure Blob, Brevo, payOS, Gemini);
- Error semantics and status code mappings;
- Backend unit and integration test expectations;
- Approved architectural and performance constraints.

Read `references/decomposition-rules.md`.

## Issue Body Synchronization

Follow repository Issue templates when present.

When no stronger repository format exists, use `references/issue-body-template.md`.

For Issues managed by this skill, use a clearly delimited managed block when compatible with the repository template:

```text
<!-- srs-sync:start -->
... requirement-derived managed content ...
<!-- srs-sync:end -->
```

Update only the managed requirement-derived content. Preserve human discussion, comments, and unrelated manual notes.

Do not overwrite a manually edited body blindly when the managed boundary is absent or ambiguous; report the conflict first.

## Labels, Priority, Estimates, Dates

Use repository/team conventions.

- Labels must come from repository configuration unless new labels are explicitly authorized.
- Do not invent Story Points, Priority, Start Date, or Target Date.
- Use `TBD` when allowed by repository policy.
- Use `references/sizing-priority-rules.md` only as fallback guidance when the project adopts it.

## Technical Constraint Rule

Include technical constraints/choices only when the authoritative project sources already define them.

Do not turn an implementation suggestion into a requirement-derived Issue fact.

### Source-Backed Implementation Rule

An implementation Issue may contain a concrete technical decision only when that decision is explicitly confirmed by an authoritative Architecture, Technology Stack, ADR, approved API contract, or approved data model.

If the source defines behavior but not implementation:
- describe the required behavior;
- do not invent framework versions, class names, table names, column types, endpoint shapes, persistence strategies, or provider response mappings.

If implementation remains open, mark it as `DESIGN_TBD` or reference the issue/artifact responsible for deciding it.

### Derived-Value Rule

Do not transform a business invariant or cross-field validation rule into a new persisted or request field unless an approved data model/API contract explicitly defines that field.

Example: `prepTime + cookTime > 0` does NOT imply a required `totalTime` request or database field.

### Self-Contained Issue Rule

An implementation Issue MUST contain enough authoritative behavioral information for an assigned developer and tester to understand the scope, expected flows, constraints, error behavior, and acceptance conditions without opening the source requirement documents. Linked source documents remain authoritative for traceability, conflict resolution, rationale, and deeper context; they MUST NOT be used as a substitute for missing execution-critical information in the Issue.

### Selective Materialization Rule

Only execution-critical information from source requirements is summarized into the Issue. Historical rationale, extensive requirement decomposition, governance metadata, and supporting context remain linked rather than duplicated.

## GitHub Creation / Reconciliation Preflight

Use `references/github-creation-preflight.md` before real mutations.

Preflight outcomes are classified:

- **Hard blocker** -> do not continue merely because the user says "continue"; resolve the missing authority/source/mapping first.
- **Overridable workflow warning** -> the authorized user may explicitly accept the risk and continue.

Missing explicit requirement lifecycle is a hard blocker for the affected requirement.

## Coordination With Documentation Skills

When `markdown-documentation` is available, it owns shared requirement semantics, lifecycle definitions, stable IDs, source-of-truth rules, and documentation impact analysis.

This skill owns GitHub work-item representation and synchronization.

When SRS content changes:

```text
markdown-documentation
        -> determine/record authorized requirement change
srs-to-github-issues
        -> reconcile linked work items from that source change
```

Issues never override the SRS solely because an Issue body differs.

## Token-Saving Workflow

For large SRS files, work by phase:

1. Inventory + lifecycle validation.
2. Update `ISSUE_INDEX.md` and reconciliation plan.
3. Generate/update selected drafts.
4. Perform authorized GitHub mutations.
5. Sync verified Project metadata if requested.
6. Report concise results and unresolved blockers.

Do not print full Issue bodies in chat unless requested.
