# Development Guide Reference

Use this reference for day-to-day technical development workflow.

## 1. Responsibility

`DEVELOPMENT.md` explains how developers work with the system technically after local setup succeeds.

Typical content may include:

- high-level repository/project structure;
- backend/frontend development commands;
- how to run tests;
- how to build artifacts;
- database migration workflow;
- local debugging practices;
- development-only tools;
- confirmed conventions for adding modules/features when such conventions exist.

## 2. Boundary with CONTRIBUTING.md

`CONTRIBUTING.md` owns collaboration and contribution workflow, such as:

- branch strategy;
- commit/PR conventions;
- review/merge rules;
- issue workflow;
- contributor approval expectations.

`DEVELOPMENT.md` owns technical workflow, such as:

- `mvn test`;
- how migrations are run;
- how services are started/debugged;
- where major code areas live.

If an item touches both, choose one source of truth and link to it from the other instead of duplicating the full rule.

## 3. Grounding

Do not invent commands, package names, directory structure, or tool choices.
Confirm them from the repository or explicit task context.

## 4. Keep It Operational

Prefer actionable, maintained instructions over broad software-engineering theory.
