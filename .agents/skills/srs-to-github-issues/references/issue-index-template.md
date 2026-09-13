# ISSUE_INDEX.md Template

Create this file at the repository-configured draft path.

Fallback path when no stronger convention exists:

```text
.agents/outputs/drafts/github-issues/ISSUE_INDEX.md
```

```md
# GitHub Issue Draft Index

## Summary
- Source documents:
  - ...
- Draft output directory: `.agents/outputs/drafts/github-issues/`
- Mode: Draft only
- Real GitHub issues created: No
- GitHub Project synced: No

## Source Authority
- Product direction source: ...
- Detailed requirement source: ...
- Repository governance: ...
- Unresolved source conflicts: None / ...

## Requirement Disposition / Traceability

| Source ID | Lifecycle | Readiness | Proposed Issue(s) | Disposition / Notes |
|---|---|---|---|---|
| FR-01 | ACTIVE | Ready | 001 | Eligible leaf FR |
| FR-02 | DEFERRED | N/A | None | Excluded from current implementation |
| FR-03 | ACTIVE | Needs clarification | None | Blocking ambiguity; Needs Review |

## Module: <Module Name>

| No | Draft File | Title | Type | Owner | Story Points | Priority | Source Trace | Dependencies | Parent | Labels | Suggested Branch | Draft State | GitHub Issue |
|---|---|---|---|---|---:|---|---|---|---|---|---|---|---|
| 001 | 001-module-short-title.md | [Module][FR-xx] Title | Feature | TBD | TBD | TBD | FR-xx | None | None | ... | feature/example | Draft | N/A |

## Grouping / Splitting Decisions

| Source | Decision | Reason |
|---|---|---|
| FR-01 | Split into 001, 002 | Independent testable slices |
| FR-03 | Parent only | Child FRs own implementation scope |

## Review Notes

- Items requiring clarification:
  - ...
- Requirements excluded by lifecycle:
  - ...
```

## Allowed Draft States

```text
Draft
Needs Review
Approved
Created
Synced
```

Default Draft State: `Draft`.

`Draft State` tracks the issue-generation lifecycle. It is not the GitHub Project `Status`.

## Index Rules

- `ISSUE_INDEX.md` is the single source of truth for real Issue creation from this skill's generated drafts.
- Every listed draft file must exist.
- Every `.md` draft file in the final draft directory must be listed.
- Do not create real GitHub Issues from files not listed in the index.
- Record excluded or blocked source requirements in the disposition table when useful for auditability; do not create fake Issue files for them.
- Parent capability FRs must not duplicate child implementation Issue scope.
- Owner, estimates, priority, and dates follow repository/team policy and may remain `TBD` when not yet decided.
