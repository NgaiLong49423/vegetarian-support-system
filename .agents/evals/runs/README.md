# Executed Evaluation Evidence

This directory stores **results from actual eval executions**. Files under skill `evals/` and routing definitions describe expectations; they do not prove the agent passed them.

Do not fabricate evidence. Create a run directory only after an eval is actually executed.

Hidden working structure (outside the evaluated workspace):

```text
<external-harness-root>/
└── <run-id>/
    ├── manifest.json
    ├── agent-prompt.txt
    ├── hidden/
    │   ├── case.json
    │   └── assertions.json
    └── evidence/
            ├── with_skill/
            │   ├── output.md
            │   ├── git-diff.patch
            │   ├── trace.json
            │   ├── grading.json
            │   └── timing.json
            └── without_skill/
                ├── output.md
                ├── trace.json
                ├── grading.json
                └── timing.json
```

For comparison against a prior skill version, `without_skill/` may instead be replaced by or supplemented with `old_skill/`; record the baseline type in `manifest.json`.

## Minimum Manifest

Record:

- commit SHA;
- model/runtime;
- skill hashes/versions when relevant;
- evaluation date;
- cases executed;
- baseline type;
- pass/fail summary;
- known failures;
- whether traces were inspected.

## Grading

Grade concrete observable assertions. A PASS should identify evidence in the output, diff, trace, or remote state.

Important invariants such as stable-ID preservation, no invented project facts, and no unauthorized remote mutation require 100% pass for the acceptance cases that test them.

## Clean Context

With-skill and baseline runs should start from equivalent clean task state. Do not let one run inherit hidden state from the other.

## Multi-runtime evidence

A run proves behavior only for the runtime/model recorded in its manifest. When the team uses multiple agent clients, keep separate run IDs for each accepted runtime/model combination. Do not assume an Antigravity run proves Codex behavior, or vice versa.

## Portable helper

Use the repository helper only to prepare/finalize evidence structure and clean local SUT repositories; it does not call a model:

```text
python .agents/scripts/eval-run.py init --case A01 --runtime <client> --model <model> --baseline without_skill
```

This creates separate roots for the evaluator harness and tested SUT. The tested runtime must receive only one printed SUT path plus the text from `agent-prompt.txt`. After actual execution and evaluator grading, finalize with:

```text
python .agents/scripts/eval-run.py finalize --harness <external-harness-run-path>
```

Finalization refuses to promote incomplete evidence. Leakage produces invalid evidence rather than a PASS.

A completed `grading.json` must contain at least:

```json
{
  "pass": true,
  "assertions": [
    {"assertion": "observable requirement", "pass": true}
  ],
  "evidence": [
    "file/diff/trace/output location that supports the grade"
  ],
  "notes": ""
}
```

If a runtime does not expose token counts or detailed traces, record the field as unavailable rather than inventing data.
