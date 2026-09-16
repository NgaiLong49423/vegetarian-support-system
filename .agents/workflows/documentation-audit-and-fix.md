# Documentation Audit and Fix Workflow

## Purpose

Audit repository documentation for real cross-document drift, then apply the smallest authorized fixes without turning an audit into an uncontrolled rewrite.

## Use When

Use when the user asks whether documentation is consistent, ready for a milestone, or needs synchronization after a meaningful project change.

## Primary Skills

- `repo-template-doc-sync-auditor` — owns the audit and finding classification.
- `markdown-documentation` — owns semantic documentation edits when a finding is confirmed.
- `document-metadata-standardizer` — owns metadata/version corrections.
- `changelog-automatic` — owns changelog corrections when changelog evidence is involved.

## Procedure

### Step 1 — Scope the Audit

Determine the triggering concern (requirements, architecture, testing, workflow, database, labels, or agent policy) and use the registered `doc_sync_rules` when available.

Avoid full-repository inventory unless the task actually requires it.

### Step 2 — Audit First

Run `repo-template-doc-sync-auditor` in audit mode.

Each finding should distinguish at least:

- contradiction;
- omission;
- implementation gap;
- lifecycle/status mismatch;
- historical baseline difference;
- stale/broken reference.

Do not modify files during a review-only request.

### Step 3 — Establish Fix Authority

Before fixing each finding, identify the source of truth for that concern. If equally authoritative sources conflict, stop that finding and request a decision.

### Step 4 — Apply Specialized Fixes

When fixes are authorized:

- semantic documentation fix -> `markdown-documentation`;
- metadata/version fix -> `document-metadata-standardizer`;
- changelog mechanics/evidence fix -> `changelog-automatic`;
- requirement-linked Issue synchronization -> `srs-to-github-issues`, only if explicitly in scope and authorized.

### Step 5 — Evaluator–Optimizer Quality Loop

For important multi-document fixes, use `evaluator-optimizer.md`: treat confirmed audit findings as evaluation input, optimize only authorized/evidence-supported findings, and re-evaluate the changed concerns.

For a review-only request, do not invoke the optimizer.

Do not declare completion merely because files were edited.

## Completion Gate

Pass when all in-scope critical/major findings are either:

- fixed and verified; or
- explicitly accepted/deferred by an authorized decision with the reason recorded in the appropriate project artifact.

Minor/info findings may remain when they do not block the requested milestone.

## Output

Return:

1. audit scope;
2. findings by type/severity;
3. fixes applied;
4. verification results;
5. accepted/deferred findings;
6. remaining blockers.
