# Document Lifecycle Reference

Use this reference when deciding whether to create, split, retire, archive, baseline, remove, rename, or move documentation, or when accepted requirement status changes.

## Contents

1. Update before creating
2. Creating a new document
3. Requirement lifecycle states
4. Requirement removal and impact analysis
5. Historical baselines and archive behavior
6. Broad baseline migration
7. Retired identifiers
8. Rename and move

## 1. Update Before Creating

Before creating a new document, determine whether an existing authoritative document already owns the information.

Prefer updating an existing document when the new information has the same purpose, audience, source of truth, and lifecycle.

## 2. Creating a New Document

A new document is justified when one or more of the following is true:

- Repository convention requires a separate artifact.
- The information has a distinct responsibility or audience.
- The information has an independent lifecycle or approval state.
- Keeping it in the current artifact would mix concerns or make maintenance materially harder.
- The artifact is a historical record that should remain stable, such as an accepted Decision Record.

When the user has not explicitly requested a new file and repository convention does not require it, propose the new artifact first. Explain:

- proposed path/name;
- responsibility;
- why the existing artifact is insufficient;
- expected references or ownership.

Create it only after approval by the appropriate decision authority.

## 3. Requirement Lifecycle States

Unless repository conventions define equivalent states, use this small vocabulary:

- `DRAFT` — being developed and not yet accepted.
- `ACTIVE` — accepted and in the current scope.
- `DEFERRED` — accepted but postponed to a later iteration/version.
- `OUT_OF_SCOPE` — not in the current scope; may be reconsidered later.
- `RETIRED` — previously accepted but intentionally no longer used.

When moving an accepted requirement out of `ACTIVE`, record a reason. If a related Decision Record or change record exists, reference it.

Do not treat `OUT_OF_SCOPE` or `DEFERRED` as physical deletion.

## 4. Requirement Removal and Impact Analysis

Accepted requirement IDs are persistent.

When an accepted requirement leaves the current scope:

1. Prefer a lifecycle status change first.
2. Do not automatically delete the requirement or its related artifacts.
3. Before removing it from the active baseline, perform impact analysis on directly related artifacts.
4. Evaluate each affected artifact independently:
   - keep it if it remains valid;
   - update it if only part is affected;
   - retire it if its purpose no longer exists;
   - remove it from the active baseline only when appropriate.
5. Update traceability so active artifacts do not contain unexplained stale references.
6. Preserve enough history to determine what the requirement meant, why it changed, when it changed, and how relationships were resolved.
7. Require approval according to repository governance before removal from an accepted baseline.

Typical impact targets include BRs, FRs, use cases, tests, architecture, interfaces/API contracts, issues, and traceability records when they exist.

This is controlled change propagation, not cascading delete.

## 5. Historical Baselines and Archive Behavior

Historical storage follows repository convention.

Do not hard-code a requirement that every repository must use `docs/archive/`.
If no convention exists and a historical baseline needs to be preserved, the agent may propose a simple archive/baseline location, such as `docs/archive/`, but must not create a new archival structure without approval.

Historical baselines:

- preserve the project state at the time they were accepted;
- are historical evidence, not the current source of truth;
- should not be rewritten to match current documentation;
- should remain recoverable through the repository's chosen history/baseline mechanism.

Removing an item from the active SRS is not the same as erasing it from project history.

## 6. Broad Baseline Migration

Do not create a new baseline based on a hard numeric threshold such as “five changed FRs.”
Use impact instead.

Propose a baseline migration when changes materially alter one or more of:

- project scope;
- requirement hierarchy/structure;
- a large connected set of FR/BR/UC/Test artifacts;
- traceability relationships;
- the active document set;
- terminology or organization enough that the new baseline is meaningfully different from the old one.

A normal small edit, one new FR, one deferred FR, or one corrected reference does not automatically require a new baseline.

For a broad migration:

1. Perform impact analysis.
2. Propose the migration and historical-preservation plan.
3. Obtain approval from the repository's authorized decision-maker.
4. Preserve the previous baseline.
5. Update current authoritative artifacts.
6. Update traceability.
7. Defer version metadata/bump rules to repository policy or the dedicated metadata skill.

## 7. Retired Identifiers

- Gaps in IDs are acceptable.
- Never renumber IDs merely to close gaps.
- Never reuse a retired ID for a different requirement.
- New requirements receive new IDs according to repository convention.
- Repository-wide ID migration requires explicit approval and complete confirmed-reference updates.

Example:

```text
FR-18
FR-20
FR-21
```

is preferable to renumbering `FR-20 -> FR-19` after `FR-19` was retired.

## 8. Rename and Move

Treat rename/move as a change with link/reference impact.

When a file is renamed or moved:

1. Identify confirmed internal links and references to the old path.
2. Update affected references.
3. Check documentation indexes, README, governance references, and traceability when relevant.
4. Report risk if external tooling or workflows may depend on the old path.
5. Do not rewrite unrelated content while updating references.
