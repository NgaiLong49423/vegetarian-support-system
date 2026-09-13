# Test Strategy Reference

Use this reference to document how the project plans to verify software quality.

## 1. Strategy, Not Test Cases

A Test Strategy describes approach, scope, levels, ownership, tooling boundaries, test data/environment,
coverage interpretation, and Definition of Done. It is not a catalog of individual test cases.

## 2. Core Test Levels for a Typical SWP Web Project

Plan for these core concerns at the strategy level when applicable:

- Unit testing for business/domain logic.
- Integration testing for important component/database/external-adapter interactions.
- API/backend verification for REST behavior.
- Requirement-to-test traceability when requirement/test artifacts already exist.

## 3. Optional Test Levels by Risk

Use when they provide real value:

- frontend/component testing;
- end-to-end testing for critical user flows;
- performance testing;
- security testing;
- exploratory/manual testing.

Do not require every level for every feature.

## 4. Strategy Questions

A useful strategy should answer, at minimum:

- What will be tested?
- At which test level?
- Which tools are used when already chosen?
- Who owns testing according to project governance?
- What test data/environment is needed?
- Which requirements are verified when traceability artifacts exist?
- What conditions define feature completion?

## 5. Coverage

Coverage is a quality signal, not proof of correctness.

- Do not invent a default threshold such as 80% or 90%.
- If the course/project/team has defined a target, record that target and its source.
- If no target exists, report actual coverage without claiming it is sufficient.
- Prioritize meaningful business logic and important branches over percentage chasing.

`100% coverage` does not imply `100% correctness`.

## 6. Definition of Done

A minimal reusable DoD may include:

- related requirement/acceptance behavior is sufficiently clear;
- implementation builds/runs as expected for the project;
- required tests at appropriate levels pass;
- no unresolved blocker/critical defect remains for the feature;
- directly affected documentation is updated when necessary;
- requirement/test traceability is preserved when those artifacts exist.

Do not automatically require a particular coverage percentage, E2E test, performance test, security review,
or production deployment unless repository governance requires it.

## 7. External Services and AI

When the system depends on external APIs or AI models, document how tests will avoid unstable/expensive calls when appropriate.
Possible techniques include mocks, stubs, controlled fixtures, or limited integration tests, but do not prescribe a technique the project has not adopted.
