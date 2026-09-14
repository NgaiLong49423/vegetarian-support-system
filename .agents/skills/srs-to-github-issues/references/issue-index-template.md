# ISSUE_INDEX.md Template

Create this generated working artifact at the repository-configured path.

Fallback:

```text
.agents/outputs/drafts/github-issues/ISSUE_INDEX.md
```

```md
# SRS ↔ GitHub Issue Index

## Summary
- Authoritative SRS Registry: `...` (e.g. `docs/requirements/SRS.md`)
- Detailed Requirements Specification: `...` (e.g. `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md` or monolithic SRS)
- Draft output directory: `.agents/outputs/drafts/github-issues/`
- Real GitHub mutations authorized: Yes / No
- Last reconciliation source revision: <verified commit/hash or Unknown>

## Source Authority
- Authoritative lifecycle registry: ... (e.g. `docs/requirements/SRS.md`)
- Detailed requirements source: ...
- Repository governance: ...
- Adopted repo contract: None / ...
- Unresolved source conflicts: None / ...

## Requirement ↔ Issue Registry

| Source ID | Lifecycle | Hierarchy Role | Draft(s) | GitHub Issue | Issue Disposition | Sync State | Notes |
|---|---|---|---|---|---|---|---|
| FR-01 | ACTIVE | Standalone | 001 | #12 | Open / Current | In Sync | ... |
| FR-02 | DEFERRED | Leaf | 002 | #13 | Open / Deferred | In Sync | ... |
| FR-03 | DRAFT | Parent | 003 | N/A | Planning only | Draft | ... |
| FR-04 | OUT_OF_SCOPE | Leaf | N/A | #14 | Closed / Not planned | In Sync | Historical link preserved |
| FR-05 | RETIRED | Standalone | N/A | #15 | Closed / Not planned | In Sync | Historical link preserved |
| FR-06 | <missing> | Leaf | N/A | N/A | Blocked | Lifecycle Required | Ask authorized decision-maker |

## Split / Group / Supersession Mapping

| Source ID | Related Issue(s) | Relationship | Reason |
|---|---|---|---|
| FR-10 | #20, #21 | Split | Two independently deliverable slices |
| FR-03 | #30 -> #31, #32 | Parent to children | Parent capability coordinates child FRs |

## Reconciliation Queue

| Source ID | Detected Change | Existing Issue | Required Action | Authorization State |
|---|---|---|---|---|
| FR-07 | ACTIVE -> DEFERRED | #17 | Mark deferred/backlog | Pending / Authorized |
| FR-08 | Semantic change after completion | #18 | Preserve #18; create follow-up | Pending / Authorized |
```

## Sync State Values

Use concise states such as:

```text
Draft
Lifecycle Required
Ready to Create
Created
In Sync
Update Required
State Change Required
Follow-up Required
Blocked
```

Repository conventions may replace these names.

## Index Rules

- This index is the synchronization registry, not the SRS source of truth.
- Every FR with explicit lifecycle must appear in the registry.
- Missing lifecycle is recorded as a blocker; do not guess it.
- Preserve old Issue numbers/URLs for historical traceability.
- Do not erase a mapping merely because the requirement becomes `OUT_OF_SCOPE` or `RETIRED`.
- Do not map both parent and child FRs to duplicate implementation scope; parent Issues are tracking/grouping work.
- When an already-completed Issue no longer represents new semantic work, keep it historical and link a follow-up Issue.
- Owner, estimates, priority, and dates may remain `TBD` when repository policy permits.
