# Evaluator–Optimizer Workflow

## Purpose

Provide a reusable quality-improvement loop for important agent outputs:

1. generate or modify an artifact;
2. evaluate it against explicit criteria and authoritative evidence;
3. return structured findings without silently rewriting the artifact;
4. optimize only the authorized, evidence-supported findings;
5. re-evaluate until the artifact passes, requires a human decision, or reaches the iteration limit.

This is a runtime workflow for improving the current task output. It is different from the regression evaluations under `.agents/evals/`, which test whether agent behavior remains correct after skills or workflows change.

## Use When

Use this workflow when the current task is high-impact, multi-artifact, or difficult to verify by inspection alone, including:

- requirement finalization or requirement reconciliation;
- cross-document consistency repair;
- architecture or design documentation with explicit constraints;
- GitHub Issue decomposition derived from accepted requirements;
- milestone/release documentation where incorrect output would propagate downstream.

Do not require this full loop for trivial, low-risk edits such as typo fixes, heading cleanup, or an isolated link correction unless the user explicitly requests strict verification.

## Roles

### Generator

Creates or modifies the candidate output using the task-specific skill/workflow and authoritative project sources.

### Evaluator

Inspects the candidate output against the evaluation contract. The evaluator reports findings and evidence; it does not silently change the candidate during the evaluation phase. For remote or baseline-changing tasks, the evaluator is strictly read-only with respect to the candidate and remote systems.

### Optimizer

Receives the candidate plus evaluator findings and applies only authorized, evidence-supported corrections. The optimizer must preserve unrelated accepted content.

The same underlying model may perform these roles sequentially, but the responsibilities must remain logically separated. For remote mutations, evaluation must finish before the approval/execution phase; an evaluator must not invoke the remote-write operation it is judging.

## Evaluation Contract

Before evaluation, identify the criteria that actually apply to the task. Do not invent criteria that are unrelated to the requested scope.

Typical criteria include:

- correctness against authoritative project sources;
- internal consistency;
- cross-document consistency when in scope;
- traceability to stable requirement/decision identifiers;
- compliance with repository governance and selected skill rules;
- absence of unsupported assumptions or invented decisions;
- completeness for the requested output contract;
- preservation of accepted history and unrelated content;
- authorization boundaries for local or remote mutations.

When a task-specific workflow defines stronger criteria, those criteria take precedence for that task.

## Finding Schema

Each non-trivial finding should contain enough structure for an optimizer or human reviewer to act safely:

```text
Finding ID: F-001
Severity: blocker | major | minor | info
Category: correctness | contradiction | omission | traceability | governance | metadata | authorization | other
Location: file/section/requirement/issue identifier
Evidence: authoritative source or observable repository state
Actual: what the candidate currently says/does
Expected: what the applicable contract requires
Disposition: AUTO_FIXABLE | HUMAN_DECISION_REQUIRED | ACCEPTABLE_AS_IS
Suggested correction: optional; must not invent product decisions
```

A finding without sufficient evidence must not be treated as an automatic correction instruction.

## Procedure

### Step 1 — Generate Candidate

Run the task-specific skill/workflow and produce a candidate result.

Record the authoritative inputs and the requested scope that the evaluator must use.

### Step 2 — Evaluate Without Mutating

Evaluate the candidate against the applicable contract.

During this step, do not modify the candidate, active baseline, or remote state. Remote reads may be used as evidence when authorized and task-relevant, but remote writes are prohibited during evaluation.

Classify each finding as:

- `AUTO_FIXABLE` — the correct change is unambiguous from authoritative evidence and already-authorized rules;
- `HUMAN_DECISION_REQUIRED` — resolving the finding requires choosing product meaning, policy, scope, priority, lifecycle, numeric targets, or between equally authoritative conflicting sources;
- `ACCEPTABLE_AS_IS` — an observed difference is intentional, historical, explicitly deferred, or outside the current scope.

Do not convert implementation gaps into documentation contradictions unless the applicable source-of-truth rules say they must already match.

### Step 3 — Decision Gate

If any blocker/major finding is `HUMAN_DECISION_REQUIRED` and it blocks safe optimization, stop with:

`BLOCKED_REQUIRES_DECISION`

Report the smallest decision the human/team must make. Do not let the optimizer guess.

If there are no actionable findings, return `PASS` without unnecessary rewrites.

### Step 4 — Optimize Authorized Findings

For `AUTO_FIXABLE` findings:

1. use the owning skill for the affected concern;
2. make the smallest coherent correction;
3. preserve unrelated accepted content;
4. preserve stable identifiers and historical evidence;
5. do not expand scope merely because nearby improvements are possible;
6. do not perform remote mutations unless the exact previewed batch has passed its required human approval gate;
7. if a planned remote batch changes materially after evaluation, return to preview/evaluation and obtain fresh approval.

### Step 5 — Execute Approved Remote Batch When Applicable

If optimization requires remote GitHub mutation, present the complete batch preview and stop for human approval before the first write. One approval may cover the entire previewed batch. Execute only the approved targets/actions; a materially changed batch requires new approval. Verify remote results and preserve partial-failure evidence.

For local-only tasks, continue directly to re-evaluation.

### Step 6 — Re-Evaluate

Evaluate the updated candidate again using the same applicable contract plus any new evidence produced by the authorized fix.

Do not mark a finding resolved only because a file was edited. Verify the expected behavior/state.

### Step 7 — Iteration Control

The default maximum is **3 optimization rounds** for one invocation of this workflow unless the parent workflow or user explicitly defines a different limit.

Track:

```text
iteration: 0..3
previous_findings: ...
current_findings: ...
```

Stop early on `PASS` or `BLOCKED_REQUIRES_DECISION`.

If the maximum number of optimization rounds is reached with unresolved blocker/major findings, stop with:

`BLOCKED_ITERATION_LIMIT`

Report the remaining findings and do not continue an unbounded self-repair loop.

## Status Model

Use one final status:

- `PASS` — no in-scope blocker/major actionable findings remain.
- `BLOCKED_REQUIRES_DECISION` — an authorized human/team decision is required before safe correction.
- `BLOCKED_ITERATION_LIMIT` — the allowed optimization rounds were exhausted with unresolved blocker/major findings.
- `BLOCKED_AUTHORIZATION` — the necessary correction requires an action not authorized by the current task/workflow.

Minor/info findings may remain when the parent workflow explicitly permits them.

## Output

Return a concise execution record:

1. candidate/artifacts evaluated;
2. evaluation criteria used;
3. findings by severity and disposition;
4. optimization actions applied;
5. number of optimization rounds used;
6. verification result;
7. final status;
8. unresolved decisions or authorization blockers.

## Non-Goals

This workflow does not:

- replace per-skill regression tests;
- make product or business decisions for the team;
- authorize GitHub or other remote mutations;
- require the agent to read all historical incidents;
- justify repeated rewrites when the candidate already passes.
