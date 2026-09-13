# Metadata Audit Report Template

Use this template when auditing document metadata.

Default output path only when the repository already uses it:

```text
.agents/outputs/reports/DOCUMENT_METADATA_REPORT.md
```

Otherwise return the report in the current task or propose a destination instead of creating a new reporting structure automatically.

## Report Template

```markdown
# Document Metadata Report

## Summary

- Repository:
- Audit date:
- Files reviewed:
- Files with complete metadata:
- Files missing metadata:
- Files with invalid metadata:
- Version updates needed:
- Date issues:
- Policy ambiguities:
- Unresolved fields:
- Overall status:

## Overall Status

| Status | Meaning |
|---|---|
| Ready | Metadata is complete enough for normal repository work |
| Needs Cleanup | Some metadata issues should be fixed |
| Not Ready | Metadata could cause wrong source-of-truth/baseline behavior |

## Files Reviewed

| File | Role | Metadata Status | Notes |
|---|---|---|---|
| `README.md` | Project overview | Complete / Missing / Invalid | |
| `docs/requirements/SRS.md` | Requirements | Complete / Missing / Invalid | |
| `CHANGELOG.md` | Version history | Complete / Missing / Invalid | |
| `CONTRIBUTING.md` | Contribution rules | Complete / Missing / Invalid | |
| `AGENTS.md` | Agent governance | Complete / Missing / Invalid | |

## Findings

| ID | Severity | File | Problem | Evidence | Recommended Fix |
|---|---|---|---|---|---|
| META-001 | Major | `docs/requirements/SRS.md` | Missing Version | Metadata block has no Version field | Add version according to repository policy |
| META-002 | Minor | `README.md` | Invalid date format | Created is `29/6` | Normalize after confirming the actual date |
| META-003 | Major | `docs/requirements/SRS.md` | Version/status policy conflict | Repository defines v0 as draft but file is `v0.3.0` + `Active` | Ask whether status or version policy should change |
| META-004 | Critical | `docs/archive/SRS-v1.0.0.md` | Historical baseline represented as current | Archive convention says archived snapshots are not active | Correct document status without rewriting historical content |
| META-005 | Major | `docs/requirements/SRS.md` | Created date unknown | No reliable creation evidence found | Leave unresolved; do not use current date as a substitute |

## Version Decisions

| File | Current Version | Recommended Version | Decision | Reason |
|---|---|---|---|---|
| `README.md` | v1.0.0 | v1.0.1 | PATCH | Metadata path correction only |
| `docs/requirements/SRS.md` | v1.2.0 | v1.3.0 | MINOR | Approved localized lifecycle/content update; no baseline migration |
| `docs/requirements/SRS.md` | v1.9.0 | v2.0.0 | MAJOR candidate | Approved broad baseline migration/restructure |

## Date Checks

| File | Created | Last Updated | Result | Notes |
|---|---|---|---|---|
| `README.md` | 2026-06-29 | 2026-09-13 | OK | |
| `docs/requirements/SRS.md` | Unknown | 2026-09-13 | Unresolved | Do not fabricate Created |

## Document Status vs Requirement Lifecycle

| File | Document Status | Internal Lifecycle Example | Result |
|---|---|---|---|
| `docs/requirements/SRS.md` | Active | `FR-19: OUT_OF_SCOPE` | Valid; scopes are different |

## Missing / Unresolved Metadata

| File | Field | State | Next Action |
|---|---|---|---|
| `docs/requirements/SRS.md` | Created | Unresolved | Check Git/history or ask authorized maintainer |

## Recommended Fix Order

1. Fix metadata that can make current vs historical source-of-truth ambiguous.
2. Resolve policy conflicts affecting version/status interpretation.
3. Add missing required metadata when evidence exists.
4. Normalize paths and date formats.
5. Leave unknown values unresolved rather than inventing them.

## Final Recommendation

Choose the statement that matches the actual task:

- Metadata is ready for normal repository work.
- Metadata should be cleaned before documentation audit.
- Metadata should be cleaned before issue decomposition.
- Metadata should be cleaned before merge/release.
- Metadata has unresolved fields that need maintainer confirmation.
```

## Severity Guide

### Critical

Metadata can make contributors or agents use the wrong current/historical source of truth.

Examples:

- two documents both claim the same canonical role without governance resolving it;
- historical baseline is presented as current;
- metadata points to the wrong canonical artifact.

### Major

Metadata can materially confuse version/baseline tracking.

Examples:

- required `Version` or document `Status` missing from a canonical artifact;
- version/status combination violates the repository's explicit policy;
- `Created` was fabricated from the current date despite an older document history.

### Minor

Small metadata issue.

Examples:

- confirmed date uses inconsistent format;
- path uses non-canonical separators but is still understandable;
- optional field formatting differs.

### Info

Optional improvement or policy clarification.

## Final Response Format

After metadata audit:

```text
Metadata audit completed.
Status: Ready / Needs Cleanup / Not Ready
Files reviewed: X
Missing metadata: X
Invalid metadata: X
Version decisions needed: X
Unresolved fields: X
```

After metadata fixes:

```text
Metadata standardization completed.
Updated files:
- ...

Version changes:
- ...

Unresolved items:
- ...
```
