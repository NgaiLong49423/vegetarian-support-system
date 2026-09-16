# Requirement Change Reconciliation Workflow

## Purpose

Reconcile an accepted requirement change across authoritative documentation, dependent documentation, and requirement-linked GitHub work without losing traceability or historical identity.

## Use When

Use after an existing FR/BR/NFR is semantically changed, deferred, moved out of scope, retired, reactivated, split, or otherwise changes in a way that can affect linked artifacts.

## Primary Skills

- `markdown-documentation`
- `document-metadata-standardizer`
- `repo-template-doc-sync-auditor`
- `srs-to-github-issues`
- `changelog-automatic` when the accepted change is changelog-worthy under repository policy

## Invariants

Throughout this workflow:

- preserve established requirement IDs;
- do not renumber remaining requirements to fill gaps;
- do not reuse retired/out-of-scope IDs for new requirements;
- preserve historical Issue mappings;
- do not infer lifecycle transitions from implementation progress;
- do not mutate GitHub unless authorized by the current task or adopted workflow.

## Procedure

### Step 1 — Identify the Authoritative Change

Confirm:

- affected requirement ID(s);
- previous accepted state/meaning;
- newly authorized state/meaning;
- whether the change is semantic, lifecycle-only, editorial, or structural.

If the new state is not authorized or explicit, stop.

### Step 2 — Update Authoritative Requirement Sources

Use `markdown-documentation` to apply the accepted change to the authoritative requirement source(s).

For a modular SRS, preserve the root registry's ownership of requirement existence/stable ID/lifecycle and the registered child document's ownership of detailed specification, when that is the repository's adopted model.

### Step 3 — Update Metadata if Required

Use `document-metadata-standardizer` after semantic edits when repository policy requires version/date/status updates.

### Step 4 — Audit Dependent Documentation

Use `repo-template-doc-sync-auditor` against the concern-specific sync targets in `repo-contract.yml`.

Do not treat an implementation gap as a documentation contradiction.

### Step 5 — Reconcile GitHub Work

If requirement-linked Issues exist or Issue synchronization is requested, use `srs-to-github-issues` in reconciliation mode.

Reconcile by stable requirement ID, not title similarity alone.

Examples are governed by that skill, including `ACTIVE -> DEFERRED`, `ACTIVE -> OUT_OF_SCOPE`, reactivation, completed historical work, and requirement splits.

### Step 6 — Evaluate Changelog Impact

Use `changelog-automatic` only when the accepted change is meaningful under repository changelog policy. A lifecycle change is not automatically changelog-worthy.

### Step 7 — Evaluator–Optimizer Quality Gate

Run `evaluator-optimizer.md` on the reconciled state when the change affects multiple authoritative/dependent artifacts or requirement-linked GitHub work. Use the invariants in this workflow as mandatory evaluation criteria.

### Step 8 — Verification Gate

Pass only when:

- authoritative requirement sources agree on the new state/meaning;
- no stable identifier was renumbered or reused;
- directly affected documentation is consistent or explicitly records an approved open item;
- Issue mappings preserve history and match explicit lifecycle;
- no remote mutation was performed without authorization.

## Output

Report:

1. affected requirement IDs;
2. semantic/lifecycle changes applied;
3. dependent documents reconciled;
4. Issue reconciliation actions or planned actions;
5. changelog action, if any;
6. unresolved blockers.
