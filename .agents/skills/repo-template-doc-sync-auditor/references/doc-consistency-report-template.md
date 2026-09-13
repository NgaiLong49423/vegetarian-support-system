# Documentation Consistency Report Template

Use the repository-defined report location when one exists.
Otherwise, propose a location rather than silently creating a new output structure.
A common fallback is:

```text
.agents/outputs/reports/DOC_CONSISTENCY_REPORT.md
```

## Template

```markdown
# Documentation Consistency Report

## Summary

- Repository:
- Audit date:
- Audit scope:
- Overall status:
- Critical issues:
- Major issues:
- Minor issues:
- Info notes:

## Overall Status

| Status | Meaning |
|---|---|
| Ready | Documentation is consistent enough for the requested next step |
| Needs Cleanup | Real issues exist, but the repository state remains understandable |
| Not Ready | Confirmed inconsistencies could cause wrong implementation, issue decomposition, or workflow |

## Files Reviewed

| File | Concern / Role | Lifecycle | Status | Notes |
|---|---|---|---|---|
| `...` | Requirements / overview / architecture / ... | Current / Draft / Historical / N/A | Reviewed / Missing / Skipped | |

## Findings

| ID | Severity | Type | Concern | Problem | Evidence | Recommended Action | Decision Needed |
|---|---|---|---|---|---|---|---|
| DOC-001 | Major | Structure Drift | Agent outputs | Current docs point to two different report paths | `AGENTS.md` vs adopted repo contract | Resolve authoritative path and update affected current docs | Yes |

## Cross-Document Checks

| Check | Result | Classification | Notes |
|---|---|---|---|
| Requirements vs product direction | Pass / Fail / Skipped | Contradiction / Different abstraction / N/A | |
| Requirements vs implementation | Pass / Gap / Skipped | Implementation gap / Contract drift / N/A | |
| Current docs vs archived baselines | Pass / Info / Skipped | Historical difference / Active confusion / N/A | |
| Governance sources | Pass / Fail / Skipped | Governance conflict / N/A | |
| Links and paths | Pass / Fail / Skipped | Broken link / Structure drift / N/A | |
| Metadata/version scope | Pass / Fail / Skipped | Metadata issue / Baseline mapping / N/A | |

## Requirement / Lifecycle Notes

| Item | Current Status | Evidence | Audit Interpretation | Action |
|---|---|---|---|---|
| `FR-...` | ACTIVE / DEFERRED / OUT_OF_SCOPE / RETIRED / project-specific | `...` | Current / non-current / uncertain | None / review / update traceability |

## Open Items / TBD Review

| File / Item | Open Item | Blocking? | Tracked? | Finding |
|---|---|---|---|---|
| `...` | `...` | Yes / No / Unknown | Yes / No | Valid TBD / stale / needs clarification |

## Suggested Fix Order

1. Critical source-of-truth or governance conflicts.
2. Requirement/contract contradictions that can cause wrong work.
3. Broken paths, links, and active/historical confusion.
4. Implementation gaps that affect the requested milestone.
5. Metadata/version/baseline mapping issues.
6. Minor cleanup.

## Files Suggested for Update

| File | Reason | Scope |
|---|---|---|
| `...` | `...` | Directly affected / Review only |

## Final Recommendation

State the recommendation for the user's requested next step, for example:

- Ready for coding.
- Ready for GitHub Issue decomposition.
- Ready for merge.
- Needs documentation cleanup first.
- Not ready until an authorized decision resolves the listed conflict(s).
```

## Final Response Summary

Summarize only the most important results and point to the report location when one was created or proposed.
