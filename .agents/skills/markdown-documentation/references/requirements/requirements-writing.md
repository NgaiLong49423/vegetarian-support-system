# Requirements Writing Reference

Use this reference when creating, editing, decomposing, reviewing, or retiring functional requirements.

## Contents

1. What an FR should express
2. FR hierarchy and abstraction
3. Atomicity and WHAT vs HOW
4. Requirement readiness
5. Traceability
6. Lifecycle and stable IDs
7. Editorial vs semantic requirement changes
8. Review questions

## 1. What an FR Should Express

A Functional Requirement describes behavior the system must provide or enforce.
It should identify the behavior clearly enough to understand and verify without embedding unnecessary implementation detail.

## 2. FR Hierarchy and Abstraction

Use hierarchy by meaning, not by a fixed child-count rule.

- A top-level FR may represent a meaningful business capability.
- Child FRs may represent independently traceable/testable behaviors within that capability.
- Do not create a new peer-level FR merely because another detail appeared.
- Do not create artificial parent FRs only to make numbering look organized.

Example:

```text
FR-03 — Recipe Management
├── FR-03.1 — Create Recipe
├── FR-03.2 — Save Draft
├── FR-03.3 — Publish Recipe
└── FR-03.4 — Edit Recipe
```

A standalone capability such as `FR-08 — Reset Password` does not require a fake parent when none is useful.

## 3. Atomicity and WHAT vs HOW

A leaf requirement should be specific enough to verify independently.
Avoid combining unrelated behaviors with multiple obligations in one statement.

Describe what the system must do, not internal implementation, unless a technology or design choice is an accepted constraint.

Avoid embedding Java class names, package paths, repository methods, or framework calls in FRs unless the repository explicitly treats them as requirement constraints.

## 4. Requirement Readiness

Use a graded readiness check instead of a hard pass/fail gate.

### `Ready`

Core behavior is sufficiently clear to proceed to design/test.

### `Ready with open items`

Core behavior is usable, but non-blocking details remain open. Record them explicitly.

### `Needs clarification`

An unresolved issue could materially change design, code, or tests.

Review these dimensions:

- Completeness — important behavior/error paths missing?
- Consistency — conflicts with FR/BR/NFR/accepted decisions?
- Feasibility — realistically implementable within known constraints?
- Verifiability — can the team test/evaluate it?
- Traceability — related actor/use case/test link available when those artifacts exist?
- Ambiguity — could reasonable readers interpret it differently?

Do not require documents to be “perfect” before work can proceed; classify open items by impact.

## 5. Traceability

Keep traceability minimal and useful.

When corresponding artifacts already exist:

```text
Requirement / Rule
       ↓
Use Case / Feature
       ↓
Test Case
```

- If a Use Case exists, relevant FR/BR links should be preserved.
- If test cases exist, relevant FR/BR/NFR verification links should be preserved.
- Do not create fake Use Cases or tests solely to make traceability look complete.

## 6. Lifecycle and Stable IDs

Use repository lifecycle conventions; otherwise see `references/common/document-lifecycle.md`. Do not infer or bulk-assign lifecycle states without repository evidence or an explicit authorized decision.

Accepted identifiers are persistent:

- do not renumber to close gaps;
- do not reuse retired IDs;
- prefer status changes before physical removal;
- perform impact analysis before removing accepted requirements from an active baseline.

## 7. Editorial vs Semantic Requirement Changes

Editorial changes may improve grammar or clarity only when meaning is preserved.

Treat changes to actor, precondition, trigger, scope, obligation, exception, threshold, data requirement,
business rule, expected outcome, or acceptance behavior as semantic.

Baselined semantic changes require authorization according to repository governance.

## 8. Review Questions

- Is the requirement supported by confirmed project information?
- Is it at the correct abstraction level?
- Is it independently verifiable at its leaf level?
- Does it describe WHAT rather than unapproved HOW?
- Is wording unambiguous enough for design and testing?
- Is its readiness status justified?
- Are related BR/UC/Test IDs real when referenced?
- Has an accepted ID been preserved?
