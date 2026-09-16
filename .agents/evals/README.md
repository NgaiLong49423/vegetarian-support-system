# Agent Evaluations

These evaluations protect behavior that matters across skills and workflows.

## Evaluation Layers

- `skills/*/evals/evals.json` — behavior checks owned by one skill.
- `routing/routing-cases.json` — checks which specialized skill should be primary for overlapping requests.
- `workflows/*.json` — regression cases for workflow control behavior such as stop conditions, iteration bounds, and human-decision gates.
- `incidents/` — real failure records and the process for promoting failures into regression cases.

## When to Run

Run relevant evaluations when:

- a `SKILL.md` changes behavior;
- a workflow gate/order changes;
- a skill description changes and may alter routing;
- a production/project incident reveals a repeatable failure;
- the underlying model/runtime changes materially.

Do not require exact wording unless wording itself is the contract. Prefer behavioral assertions.

## Incident-to-Regression Loop

1. Capture the real failure as an incident.
2. Reproduce it with minimal context.
3. Classify the failure and identify root cause.
4. Add a regression evaluation that fails on the bad behavior.
5. Fix the smallest responsible asset.
6. Re-run the new case plus related existing cases.
7. Keep the regression case after the fix passes.

Normal task execution should not load all incident files.

## Runtime Evaluation vs Regression Evaluation

Do not confuse `.agents/evals/` with the runtime `workflows/evaluator-optimizer.md` loop:

- **runtime Evaluator–Optimizer** judges and improves the current task output;
- **regression evaluations** judge whether the agent assets themselves still produce the expected behavior after changes.

## Executed evidence

Definitions under skill `evals/`, routing, and workflow cases are expectations only. Actual run evidence belongs under `.agents/evals/runs/` and should record commit SHA, model/runtime, baseline type, assertion grading, trace evidence, and timing when available. Do not mark a gate passed only because a JSON test definition exists.
