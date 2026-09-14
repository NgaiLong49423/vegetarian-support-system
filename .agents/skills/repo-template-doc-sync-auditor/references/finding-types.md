# Documentation Finding Types

Use this reference for detailed classification. A difference is not automatically a defect; first determine source ownership, lifecycle, project phase, and whether the two artifacts claim the same concern.

## 1. Structure / Path Drift

The repository structure described by an applicable current document conflicts with the actual current repository layout.

Examples:

```text
README says `.agents/reports/`, current governed path is `.agents/outputs/reports/`.
```

Do not report a historical archive merely because it uses an old path.

Typical severity:

- Critical if agents may read/write the wrong authoritative location.
- Major if contributors are likely to use the wrong path.
- Minor for a non-blocking stale tree/diagram.

## 2. Broken or Machine-Local Links

Examples:

```text
file:///d:/...
C:\Users\...
D:\Github-Projects\...
```

Prefer repository-relative links where appropriate.

## 3. Version / Baseline Mapping Issue

Different document versions are not a defect by default.

Possible scopes:

- document version;
- project/product version;
- baseline version tying multiple artifacts together.

Report only when the same scope conflicts or a required baseline relationship is ambiguous.

Defer version-bump policy to repository governance or the dedicated metadata skill.

## 4. Date / Metadata Inconsistency

Examples:

```text
Created date is later than Last Updated.
```

Do **not** infer that a changelog entry must exist on the same date as every documentation edit.
Only report changelog/date mismatch when project policy requires such traceability or when the changelog makes a contradictory release claim.

## 5. Requirement Contradiction

Report when authoritative sources that address the same behavior make incompatible claims.

Example:

```text
SRS: authenticated users may cancel their own booking.
Another authoritative requirement source: bookings cannot be cancelled.
```

Not automatically a contradiction:

```text
SRS defines cancellation.
README omits cancellation.
```

README may simply be a summary.

Similarly, PRD and SRS may differ in detail because they own different abstraction levels. Report only incompatible claims or violations of explicit repository expectations.

In a Modular SRS Requirement Set, a concise summary row in the root `SRS.md` index and a detailed requirement block in a registered child document (`FUNCTIONAL-REQUIREMENTS.md`) are complementary views of the same requirement, NOT duplicate or conflicting requirements. Detailed child documents are authoritative for detailed behaviors, preconditions, and acceptance criteria.

## 6. Requirement Lifecycle / Feature Status Drift

Report when current authoritative artifacts disagree about the status of the same feature or requirement.

Example:

```text
SRS: FR-19 is OUT_OF_SCOPE.
Current release plan: FR-19 is mandatory for this milestone.
```

### Lifecycle Synchronization Drift (Modular SRS)

In a Modular SRS Requirement Set, `SRS.md` is the Authoritative Registry for requirement lifecycle state. If the derived status displayed in a detailed child document differs from `SRS.md`:
- Report as `Lifecycle Synchronization Drift` (Major severity).
- The lifecycle value in `SRS.md` wins.
- The detailed child document must be synchronized to match `SRS.md`.
- The agent must not invent or silently adopt the child document's status.

Do not treat a historical baseline or archived issue as a current-status conflict.

Common statuses may include `DRAFT`, `ACTIVE`, `DEFERRED`, `OUT_OF_SCOPE`, and `RETIRED`, but repositories may use other terms.

## 7. Requirement-to-Implementation Gap

A valid requirement may not yet be implemented.

Example:

```text
SRS: LoyaltyPoint capability is ACTIVE.
Current schema: no supporting table yet.
```

Classify as an implementation gap when the requirement remains authoritative and implementation is incomplete.
Escalate to documentation contradiction only when current authoritative docs claim incompatible implemented/current facts.

If project phase or milestone expectations are unknown, report as `Info`/uncertain rather than declaring the requirement wrong.

## 8. Database Contract Drift

Report when current authoritative database artifacts conflict with each other or with an accepted implementation constraint.

Examples:

```text
Architecture/technology decision: PostgreSQL.
database/README: MySQL is the current database.
```

```text
Current migration defines `booking_status` values A/B/C.
Current API/database contract document requires X/Y/Z.
```

Do not require schema to already implement every future/deferred requirement.

## 9. GitHub Configuration Drift

Examples:

```text
Issue template applies label `feature`, but repository label configuration does not define it.
```

```text
CONTRIBUTING requires `feat/` branches, while current PR/issue templates instruct `feature/`.
```

## 10. Agent Governance / Contract Drift

Report when current recognized governance sources disagree or direct the agent to incompatible paths/workflows.

Example:

```text
AGENTS.md: reports -> `.agents/reports/`
Adopted repo contract: reports -> `.agents/outputs/reports/`
```

If both have equal authority and governance does not define precedence, report the conflict rather than choosing silently.

## 11. Placeholder / Open-Item Leakage

True template leakage examples:

```text
[Project Name]
[Tên Dự Án]
Mô tả ngắn về dự án
```

`TODO`/`TBD` are not automatically defects.

Report them when they are:

- leftover template text;
- stale/untracked;
- contradictory to a confirmed decision;
- blocking an artifact that is represented as ready/final;
- disallowed by the repository's milestone policy.

A deliberate `TBD` for genuinely unconfirmed information is valid documentation.

## 12. Language / Encoding Drift

Report when current governance requires one convention and current docs violate it in a way that affects usability, build/runtime expectations, or team workflow.

Minor mixed-language wording is usually a Minor/Info issue, not Major by default.

## 13. Historical / Active Confusion

Report when an archived/historical document is treated as current authority, or when active documents link to a retired baseline without making the historical role clear.

Do not report normal differences between historical and current baselines as contradictions.

## 14. Example / Real-Content Confusion

Report when example/template content is presented as if it were real project state.

Example:

```text
CHANGELOG contains an unmarked sample release mixed with real releases.
```

Prefer clear example labels or dedicated example locations.
