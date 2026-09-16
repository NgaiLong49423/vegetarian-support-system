# Requirement Finalization Workflow

## Purpose

Move the current requirement set from reviewable draft content to a team-confirmed baseline that is safe for downstream design, testing, and Issue decomposition.

This workflow coordinates existing skills; it does not redefine requirement semantics.

## Use When

Use when the team wants to finish/chốt the current FR, BR, and NFR set before creating implementation work items or proceeding to design.

Do not use this workflow to invent product decisions on behalf of the team.

## Primary Skills

- `markdown-documentation` — requirement content, ambiguity handling, source-of-truth, stable IDs, lifecycle semantics.
- `document-metadata-standardizer` — metadata/version updates after approved semantic edits.
- `repo-template-doc-sync-auditor` — cross-document consistency audit and authorized fixes.
- `srs-to-github-issues` — optional downstream handoff after the finalization gate passes.

## Preconditions

1. Identify applicable repository governance and the authoritative requirement set.
2. If the repository uses a modular SRS, resolve the root registry and registered detailed requirement documents.
3. Do not treat research notes, generated drafts, Issues, or implementation code as authority for requirement meaning unless repository policy explicitly says so.

## Procedure

### Step 1 — Review Current Requirements

Use `markdown-documentation` to review only the current authoritative requirement set.

Find decision-blocking issues such as:

- ambiguous behavior;
- missing actor/trigger/precondition when required for implementation understanding;
- contradictory FR/BR/NFR statements;
- unsupported numeric NFR targets;
- unclear lifecycle state;
- unresolved external dependency or scope boundary;
- acceptance criteria that cannot be objectively checked.

Do not rewrite unclear items by guessing.

### Step 2 — Produce Decision Questions

Group only the unresolved items that require an authorized team/user decision.

For each question, preserve the affected stable requirement ID and explain what downstream decision is blocked.

### Step 3 — Apply Confirmed Decisions

After decisions are supplied, use `markdown-documentation` to update the authoritative requirement source with the smallest coherent change.

Rules owned by the skill still apply, including stable-ID preservation and protection of accepted/baselined history.

### Step 4 — Update Metadata

If repository policy requires metadata/version changes after the semantic edit, use `document-metadata-standardizer`.

Do not invent a version bump outside the repository's policy.

### Step 5 — Cross-Document Audit

Use `repo-template-doc-sync-auditor` in audit mode against the changed concerns and the registered sync targets from `repo-contract.yml`.

Classify contradictions separately from harmless omissions and implementation gaps.

### Step 6 — Fix Authorized Drift

If the user/task authorizes fixes, apply only findings supported by authoritative evidence. Re-run the affected audit checks after fixes.

### Step 7 — Evaluator–Optimizer Quality Gate

For a baseline/finalization request, run `evaluator-optimizer.md` against the candidate finalized requirement set using the finalization criteria below. Auto-fix only evidence-supported findings. Any unresolved product/requirement decision must return `BLOCKED_REQUIRES_DECISION`.

### Step 8 — Finalization Gate

The workflow passes only when:

- no decision-blocking ambiguity remains in the selected scope;
- lifecycle states required by repository policy are explicit;
- stable IDs are preserved;
- authoritative requirement documents do not contradict each other;
- required metadata is valid;
- directly affected registered documentation has no unresolved major/critical consistency finding.

A known `TBD` may remain only when repository policy permits it and it does not block the next intended activity.

## Optional Downstream Handoff — GitHub Issues

Only after the finalization gate passes, and only when requested/authorized, invoke `srs-to-github-issues`.

That skill performs its own lifecycle gate and GitHub mutation authorization. Finalization does not itself authorize remote Issue creation.

## Stop Conditions

Stop and ask for a decision when:

- equally authoritative requirement sources conflict;
- a lifecycle state required for the next step is missing;
- a numeric target or business rule would have to be invented;
- a requested change would overwrite an accepted decision without authorization.

## Output

Report:

1. resolved requirement decisions;
2. remaining open decisions, if any;
3. files changed;
4. finalization gate result: `PASS` or `BLOCKED`;
5. whether Issue decomposition is safe to start.
