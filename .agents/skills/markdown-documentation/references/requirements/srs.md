# SRS Reference

Use this reference when creating, reviewing, or restructuring a Software Requirements Specification.

## 1. SRS Responsibility

SRS describes WHAT the system must do and the constraints under which it must operate.
It should not become the primary description of implementation architecture or the rationale for significant technical choices.

- SRS -> WHAT.
- Architecture -> HOW THE SYSTEM IS STRUCTURED.
- Decision Record -> WHY A SIGNIFICANT CHOICE WAS MADE.

## 2. Core Baseline Sections

A practical SRS should cover the following concerns, though repository headings may differ:

- Scope
- Actors / Roles
- Functional Requirements
- Business Rules
- Non-Functional Requirements
- Constraints / Dependencies
- Acceptance / Verification approach
- Open Items / TBDs

## 3. Conditional Sections

Add only when the project needs them:

- Data Requirements
- External Interfaces
- Third-party Integrations
- AI-specific constraints
- Other domain-specific requirement sections

Do not create empty headings for completeness theater.

## 4. Requirement Readiness

Use the graded readiness model from `requirements-writing.md`.
A baseline may contain `Ready with open items` requirements when open items are explicit and non-blocking.
`Needs clarification` items must not be treated as fully ready for affected design/test work.

## 5. BR Ownership

Define each BR once and reference its stable ID from FRs/Use Cases/tests as needed.
If BR content later becomes too large or independently maintained, propose a separate BR artifact according to document lifecycle rules.

## 6. Active vs Historical Requirement Set

The active SRS should describe the current accepted scope.
Requirements may remain visible with lifecycle states such as `DEFERRED`, `OUT_OF_SCOPE`, or `RETIRED` while they are still useful to the team.

When a later baseline cleanup removes retired items from the active SRS, historical traceability must remain recoverable and IDs must not be reused.

## 7. Modular SRS and Field-Specific Authority Pattern

When requirements grow large or need independent maintenance, the SRS may adopt a modular structure consisting of a **Root Specification (Registry)** and **Registered Child Requirement Artifacts** (e.g. dedicated files for FRs, BRs, and NFRs).

### Ownership and Field-Specific Authority

Authority is divided by concern and field rather than giving universal precedence to one file:

- **Root SRS (Authoritative Registry)** owns:
  - Requirement existence (a requirement exists in scope only if registered here);
  - Stable requirement identifiers (`FR-xx`, `BR-xx`, `NFR-xx`);
  - Index-level module allocation;
  - Authoritative requirement lifecycle state (`DRAFT`, `ACTIVE`, `DEFERRED`, `OUT_OF_SCOPE`, `RETIRED`, or explicit unconfirmed gate notation).
- **Child Requirement Artifacts (Authoritative Detailed Specifications)** own:
  - Detailed requirement statements;
  - Actors and triggers;
  - Preconditions and exceptions;
  - Business-rule details, examples, and logic;
  - Acceptance criteria (Given/When/Then, verification checks);
  - NFR metrics, measurable thresholds, and test conditions.

### Derived Lifecycle in Child Documents

Lifecycle status displayed inside child requirement documents is derived/secondary.
If a lifecycle conflict arises between root SRS and a child document:
1. Root SRS wins;
2. Auditor must flag the discrepancy as `Lifecycle Synchronization Drift`;
3. Child document must be synchronized to match root SRS;
4. The agent must not invent or silently choose a third state.

### Stable Requirement Anchors

To prevent anchor link breakage when requirement titles change or are translated, child requirement definitions must provide stable anchors based on the stable ID rather than the mutable title:

```html
<a id="fr-01"></a>
### FR-01 — Requirement Name
```

Root SRS index tables link directly to these anchors: ``docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md#fr-01``.

### Root Registry Schema

A recommended root index format provides a concise 5-column overview:

```text
ID | Short Name | Module | Lifecycle | Detail
```

### Anti-Invention Guardrail

Structural migration or modularization MUST NOT create new requirement semantics. If detailed fields (such as NFR metric, threshold, or test method) have not been established in project evidence, mark them as `TBD` or `OPEN`. Do not invent metrics or thresholds during modularization.

## 8. Review Questions

- Is scope clear?
- Are actors/roles consistent with FRs?
- Are FRs at appropriate abstraction levels?
- Are BRs defined once?
- In a modular SRS, are root index entries and detailed child definitions bidirectionally complete?
- Are lifecycle states in child documents synchronized with the root registry?
- Do all detailed requirement definitions expose stable ID anchors (`<a id="..."></a>`)?
- Are NFRs meaningful and verifiable without invented metrics?
- Are constraints/dependencies distinguished from implementation detail?
- Are blocking open items explicit?
- Does existing UC/Test traceability remain correct?
