# Agent Engineering Assets

This directory contains repository-local assets used to guide, coordinate, and evaluate AI agents.

## Structure

```text
.agents/
├── POLICY.md            # Cross-skill safety, authority, and remote-mutation invariants
├── repo-contract.yml     # Repository-specific paths, authority model, and coordination policy
├── skills/               # Reusable capabilities and their detailed operating rules
├── workflows/            # Multi-skill procedures, transition gates, and runtime quality loops
├── scripts/              # Deterministic validation and portable eval-workspace helpers
└── evals/                # Acceptance/routing definitions, executed evidence, and incidents
```

## Responsibility Model

- **Skill**: explains how to perform one reusable capability correctly.
- **Workflow**: coordinates multiple skills in an ordered process and defines gates between steps.
- **Evaluation**: checks whether expected behavior is preserved after a skill/workflow change.
- **Evaluator–Optimizer workflow**: evaluates and improves the output of the current task through a bounded runtime loop.
- **Incident**: records a real agent failure and the evidence needed to prevent recurrence.

Do not duplicate a rule into every layer. Put the rule at the narrowest authoritative layer that can enforce it, then reference that layer from workflows or evaluations when needed.


## Authority and Safety

`.agents/POLICY.md` contains cross-skill invariants. `repo-contract.yml` separates user task intent from project-decision authority and non-overridable safety boundaries. Ordinary user requests do not implicitly authorize destructive Git or unpreviewed remote writes.

For GitHub synchronization, the default is: read/preflight → complete batch preview → one human approval for that exact batch → execute → verify.

## Runtime Portability

These assets are repository-local and vendor-neutral. They may be used by Antigravity, Codex, or another repository-capable agent client. Client-specific commands or hidden runtime behavior are not project authority. Each runtime/model must record its own behavioral-eval evidence because one client's results do not prove another client's behavior.

For a new or materially changed runtime, use `workflows/acceptance-evaluation.md` and the portable acceptance subset under `evals/acceptance/`.

## Loading Policy

Use progressive disclosure:

1. Start from repository governance and the current task.
2. Load only the skill(s) relevant to that task.
3. Load a workflow only when the task spans multiple capabilities or requires a transition gate.
4. Load incident history only when investigating failures or improving agent assets; normal tasks should not read every incident.
5. For important current-task outputs, use `workflows/evaluator-optimizer.md` when a bounded evaluate → optimize → re-evaluate loop is warranted.
6. Run the relevant regression evaluations after behavior-affecting changes to skills or workflows.

## Change Policy

When agent behavior fails in a repeatable way:

1. capture the incident;
2. reproduce and classify it;
3. identify the root cause;
4. create or update a regression evaluation;
5. change the smallest responsible skill/workflow/governance rule;
6. rerun the relevant evaluations;
7. keep the regression case after the fix passes.

An incident is historical evidence. A regression evaluation is the executable/repeatable expectation derived from that evidence.
