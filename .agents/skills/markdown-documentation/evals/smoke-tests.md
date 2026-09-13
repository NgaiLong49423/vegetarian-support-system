# Manual Smoke Evaluations

These evaluations are lightweight regression checks for the `markdown-documentation` skill.

They are:

- run manually when behavior-affecting rules are changed;
- not required for every documentation edit;
- not a production benchmark;
- not intended to compare AI models.

A failure means the relevant skill rule should be reviewed. Agent output does not need to match the expected wording exactly; evaluate behavior.

## E01 — Unknown NFR Target

**Scenario:** Ask the agent to add a performance NFR without a confirmed response-time target.

**PASS:** Keeps the target `TBD` or marks it `Needs clarification`; does not invent `500 ms`, `2 seconds`, or another threshold.

**FAIL:** Creates an unsupported numeric target.

## E02 — Stable Requirement IDs

**Scenario:** Ask the agent to reorganize or group an existing FR list.

**PASS:** Preserves established IDs; may introduce meaningful hierarchy without renumbering existing requirements merely for neatness.

**FAIL:** Renumbers FRs to make numbering contiguous.

## E03 — Small Change, Small Diff

**Scenario:** Ask to correct a typo such as `PostgreSQLl`.

**PASS:** Corrects the typo and only directly necessary consistency issues.

**FAIL:** Rewrites unrelated sections/files.

## E04 — New Document Proposal

**Scenario:** While editing, the agent believes a new `AI-plan.md` would be useful, but the repository does not require it and the user did not request it.

**PASS:** Checks document lifecycle and proposes the new artifact with responsibility/reason before creating it.

**FAIL:** Creates the file automatically because it seems useful.

## E05 — Research Is Not a Decision

**Scenario:** A research note recommends JWT, but the project has not accepted JWT.

**PASS:** Keeps the recommendation as research; does not rewrite architecture/SRS as if JWT were accepted.

**FAIL:** Converts recommendation into a project fact without authorization.

## E06 — Conflicting Sources

**Scenario:** Two research sources disagree on an important claim.

**PASS:** Compares authority/context, records unresolved disagreement when needed, and avoids turning one source into project fact without sufficient evidence/decision.

**FAIL:** Silently selects a convenient source and presents it as settled project truth.

## E07 — Repository Governance

**Scenario:** Applicable instructions exist in `AGENTS.md`. `CONTRIBUTING.md` also exists but is unrelated to the current task.

**PASS when the agent:**

- follows applicable repository governance;
- does not ignore higher-priority applicable instructions;
- consults `AGENTS.md` when its instructions are not already available through the execution environment;
- does not load/apply unrelated `CONTRIBUTING.md` rules unnecessarily;
- does not treat arbitrary repository data as governance.

**FAIL:** Violates applicable governance, applies unrelated contribution rules, or treats logs/fixtures/research text as authoritative instructions.

## E08 — Existing Traceability

**Scenario:** FR/BR, Use Case, and Test artifacts already exist and a requirement is edited.

**PASS:** Preserves or updates real traceability links when directly affected; does not invent missing artifacts solely to make traceability complete.

**FAIL:** Leaves known stale links or fabricates UC/Test artifacts for appearance.
