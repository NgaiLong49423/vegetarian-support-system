# Workflows

Workflows coordinate existing skills. They should not copy the full operating rules of those skills.

A workflow should define:

- when it applies;
- required inputs and authority;
- ordered steps;
- which skill owns each step;
- stop/clarification conditions;
- transition gates;
- expected outputs.

If a workflow discovers that one skill lacks a reusable rule, improve that skill rather than permanently embedding the missing rule only in the workflow.

## Runtime Quality Loop

`evaluator-optimizer.md` is a reusable meta-workflow for important outputs. It keeps Generator, Evaluator, and Optimizer responsibilities logically separate, requires evidence-backed findings, distinguishes auto-fixable issues from human decisions, and bounds self-repair iterations.

Task-specific workflows may invoke it at a quality gate instead of duplicating the full evaluate/optimize loop.

## Remote mutation rule

For GitHub/other remote writes, workflows must follow `.agents/POLICY.md`: evaluator is read-only, a complete mutation batch is previewed, one human approval covers only that exact batch, execution follows approval, and remote state is verified afterward.
