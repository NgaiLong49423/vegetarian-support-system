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

## 7. Review Questions

- Is scope clear?
- Are actors/roles consistent with FRs?
- Are FRs at appropriate abstraction levels?
- Are BRs defined once?
- Are NFRs meaningful and verifiable?
- Are constraints/dependencies distinguished from implementation detail?
- Are blocking open items explicit?
- Does existing UC/Test traceability remain correct?
