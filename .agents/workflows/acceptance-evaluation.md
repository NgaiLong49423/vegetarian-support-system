# Acceptance Evaluation Workflow

## Purpose

Use this workflow before adopting a new agent-skill baseline, after a material behavior change, or when validating the same `.agents` package on a new agent runtime/model.

This workflow is vendor-neutral. It does not assume Antigravity, Codex, or any other specific client.

## Inputs

- repository snapshot/commit;
- runtime/client name;
- model identifier when available;
- `.agents/evals/acceptance/acceptance-cases.json`;
- `.agents/evals/routing/routing-cases.json`;
- baseline type: `without_skill` or `old_skill`.

Case definitions, expected assertions, grading templates, manifests, and evidence are evaluator-only. They must remain in a harness root outside the tested workspace. Configure the tested runtime so its only filesystem workspace is the prepared SUT path.

## Procedure

### 1. Deterministic validation

Run:

```text
python .agents/scripts/validate-agent-assets.py .agents
```

Resolve FAIL findings before behavioral acceptance. Security review warnings on executable scripts require human review before acceptance.

### 2. Prepare an isolated harness and SUT

Do not use the authoritative working tree as disposable test data. The helper creates a clean local Git repository for the system under test and a physically separate harness directory. The SUT excludes `.agents/evals/`, `.agents/scripts/`, `.agents/outputs/`, and skill-local `evals/` directories.

Optional helper:

```text
python .agents/scripts/eval-run.py init --case A01 --runtime <client> --model <model> --baseline without_skill
```

The helper prints the external harness path, opaque SUT path(s), and evaluator-held `agent-prompt.txt`. Give the tested agent only the prompt text and the selected SUT workspace. Do not mount or open the harness root in the tested runtime.

### 3. Routing evaluation

For every routing case, run the same prompt three times in clean contexts and record the observed primary skill. Apply the thresholds defined in `.agents/evals/routing/README.md`.

### 4. WITH-skill acceptance run

Execute each acceptance case with the normal repository governance and applicable skill available.

Capture, when the runtime exposes them:

- output;
- changed-file diff or planned mutation preview;
- tool/decision trace;
- duration/token information;
- assertion grading with evidence.

Write captured output, trace, timing, and evaluator grading only under the external harness `evidence/<mode>/` directory. Never place hidden assertions or grading inside the SUT.

### 5. Baseline run

Run the same case from an equivalent clean state without the target skill, or against the previously accepted skill version. Do not emulate a baseline by keeping the same hidden context and merely telling the model to ignore the skill.

### 6. Grade

Grade observable assertions only. Critical safety/authority assertions require 100% pass.

A grader must not invent evidence that the runtime did not expose.

### 7. Failure handling

If a case fails:

`capture → reproduce → classify → smallest responsible fix → add/retain regression eval → rerun`

Record a real incident only when an actual failure occurred. Do not create synthetic incident history for hypothetical tests.

### 8. Finalize evidence

Only after cases have actually run, place evidence under `.agents/evals/runs/<run-id>/`. Finalization reads each SUT Git diff, output, trace, and completed grading. It rejects incomplete evidence and marks detected evaluator-artifact access as `evidence_validity = INVALID`, `reason = evaluation_leakage`.

Optional helper:

```text
python .agents/scripts/eval-run.py finalize --harness <external-harness-run-path>
```

Finalization must fail if required grading evidence is missing.

## Acceptance Gate

The agent-skill baseline is accepted for the evaluated runtime when:

- deterministic validation passes;
- repository contract paths resolve;
- all critical acceptance assertions pass;
- routing thresholds pass;
- no unreviewed executable-script security warning remains;
- WITH-skill behavior is not worse than the selected baseline on the behavior the skill claims to improve;
- evidence is stored with runtime/model and commit/snapshot identity.

Remote GitHub mutation remains blocked until the critical authorization cases have passed on the runtime that will perform the mutation.
