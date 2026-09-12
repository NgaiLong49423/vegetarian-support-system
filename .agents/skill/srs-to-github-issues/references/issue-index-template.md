# ISSUE_INDEX.md Template

Create this file at:

```text
.agents/outputs/drafts/github-issues/ISSUE_INDEX.md
```

If the repo contract defines another path, use the repo contract path.

```md
# GitHub Issue Draft Index

## Summary
- Source documents:
  - ...
- Draft output directory: `.agents/outputs/drafts/github-issues/`
- Mode: Draft only
- Real GitHub issues created: No
- GitHub Project synced: No

## Source Hierarchy
1. SRS / detailed requirements:
2. PRD / product overview:
3. Use case docs:
4. Architecture/design notes:
5. README/high-level docs:

## Traceability Table

| Source ID | Source Section | Proposed Issue(s) | Notes |
|---|---|---|---|
| FR-01 | ... | 001 | ... |

## Module: <Module Name>

| No | Draft File | Title | Type | Owner | Story Points | Priority | Source Trace | Dependencies | Parent | Labels | Suggested Branch | Draft State | GitHub Issue |
|---|---|---|---|---|---:|---|---|---|---|---|---|---|---|
| 001 | 001-module-short-title.md | [Module][FR-xx] Title | ✨ Feature | TBD | 5 | High | FR-xx | None | None | ✨ Feature | feature/example | Draft | N/A |

## Grouping / Splitting Decisions

| Source | Decision | Reason |
|---|---|---|
| FR-01 | Split into 001, 002 | Too large for one implementation issue |

## Review Notes

- Items requiring user confirmation:
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

`Draft State` tracks the issue-generation lifecycle. It is not the GitHub Project `Status`. A created issue may be `Approved` or `Created` in this index while remaining in Project `Backlog` or `Planning`.

## Index Rules

- `ISSUE_INDEX.md` is the single source of truth for real issue creation.
- Every listed draft file must exist.
- Every `.md` draft file in the final draft directory must be listed.
- Do not create real GitHub Issues from files not listed in the index.
- Owner may remain `TBD` during drafting and initial creation, but must be assigned before the Project item moves to `In Progress`.
