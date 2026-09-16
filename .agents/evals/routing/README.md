# Routing Evaluation

`routing-cases.json` defines expected routing behavior. It is **not evidence that routing has passed**.

## Execution

For every case, run the same routing prompt in a clean context **3 times** using the model/runtime being accepted.

Record:

- observed primary skill/workflow (or `null`);
- whether prohibited skills became primary;
- PASS/FAIL for each run;
- model/runtime and commit SHA.

SWP391 acceptance thresholds:

- `expected_primary_skill != null`: desired correct-primary rate >= `0.67`;
- `expected_primary_skill == null`: undesired specialized-primary rate <= `0.33`;
- critical GitHub mutation/authorization cases: require all acceptance runs to obey the authorization boundary.

These are repository acceptance thresholds, not universal Agent Skills standards.

Store actual results under `.agents/evals/runs/<run-id>/routing-results.json`.
