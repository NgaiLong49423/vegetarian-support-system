# Version Bump Rules

This reference explains how to recommend document-version changes when the repository does not already define a stronger version policy.

## Contents

- Core principle
- Version scopes
- Repository policy first
- No bump / PATCH / MINOR / MAJOR
- Requirement and lifecycle examples
- Baseline migration
- Ambiguous cases

## Core Principle

**Version the impact on the document, not the label of the underlying technical change.**

The same project event can produce different version impacts on different documents.

Example:

```text
Database technology changes from SQL Server to PostgreSQL.

ARCHITECTURE.md
→ may be MAJOR if the architecture contract is broadly replaced.

README.md
→ may be MINOR if only the stack/setup summary changes.

CONTRIBUTING.md
→ may be No bump if unaffected.
```

Do not use rules such as `database change = MAJOR everywhere`.

## Version Scopes

Always distinguish:

| Version Type | Meaning | Must Match Across Docs? |
|---|---|---|
| Document Version | Version of one document | No |
| Project Version | Software/project version | Only if project policy says so |
| Baseline Version | Identifier/version of coordinated approved artifacts | Must map clearly when used |

Different documents may evolve independently.

## Repository Policy First

If the repository has an explicit metadata/version policy, follow it.

The rules below are fallback guidance only.

Do not infer that `v0.x.x` means draft unless the repository adopts that convention.

If the repository does use that convention, promotion to a stable version requires approval according to repository governance.

## No Version Bump

No bump when there was no intentional maintained-document change.

Examples:

- read/review only;
- Git metadata only;
- active docs changed but archived snapshot did not;
- generated output where manual versioning does not apply.

## PATCH — Editorial / Non-Semantic Change

Use PATCH when meaning and maintained contract stay the same.

Examples:

| Change | Example |
|---|---|
| Typo/grammar | Fix misspelled heading |
| Formatting | Fix table alignment |
| Metadata correction | Correct repository-relative path |
| Broken link | Fix link target |
| Terminology normalization | Rename wording without changing meaning |
| Small clarification | Rephrase an accepted requirement without semantic change |

PATCH must not introduce a new actor, condition, obligation, threshold, business rule, expected outcome, or other semantic behavior.

## MINOR — Compatible Semantic Evolution

Use MINOR when the document gains or changes meaningful content but remains compatible enough that the prior structure/contract is still usable.

Examples:

- add an approved section;
- add a compatible requirement or scenario;
- add a role/actor that extends rather than replaces the previous model;
- add traceability details;
- change one requirement lifecycle status when the active baseline remains structurally intact;
- add an API/setup/development convention without replacing the existing workflow;
- add documentation metadata to an existing document when the repository intentionally adopts the metadata standard and the change is more than a correction.

A semantic change is not automatically MAJOR.

## MAJOR — Incompatible Document/Baseline Change

Use MAJOR when consumers of the previous document can no longer safely rely on its prior authoritative meaning or structure.

Examples:

- approved baseline migration after broad scope removal/restructure;
- replacing one canonical requirements source with another;
- broad FR hierarchy migration that invalidates many references;
- replacing the documented architecture model in a way that materially changes system organization;
- replacing contribution/release workflow so the old workflow is no longer valid;
- replacing/deprecating a canonical document with a new authoritative artifact.

MAJOR should be justified by impact, not by a keyword such as `database`, `architecture`, or `requirement removal`.

## Requirement and Lifecycle Examples

### Example A — Local lifecycle change

```text
FR-19: ACTIVE -> OUT_OF_SCOPE
```

If SRS structure remains intact and the document simply records the approved lifecycle change:

```text
Likely: MINOR
```

unless the repository policy says otherwise.

### Example B — Broad scope cleanup

```text
- retire an entire capability;
- restructure many FRs;
- update many BR/UC/test references;
- produce a cleaned new active baseline.
```

This is a strong **MAJOR candidate** because the SRS baseline contract is materially reorganized.

### Example C — Editorial rewrite only

```text
"User can create recipe when logged in"
->
"An authenticated user can create a recipe"
```

If meaning is confirmed unchanged:

```text
PATCH
```

### Example D — Semantic requirement change

```text
"Authenticated users can create recipes"
->
"All users can create recipes"
```

This is semantic. It may be MINOR or MAJOR depending on impact to the document/baseline and repository policy. Do not classify it as PATCH.

## Baseline Migration

A documentation/content lifecycle workflow decides whether a baseline migration should occur.

This metadata skill only records the approved result.

When an authorized baseline migration is approved:

1. preserve the historical baseline using the repository's chosen mechanism;
2. keep archived snapshots unchanged;
3. update the active document metadata;
4. recommend the version bump according to repository policy and impact;
5. do not invent a `Baseline` value if none has been defined.

## Draft / Pre-Stable Conventions

Only apply `v0.x.x = draft/under review` when repository policy explicitly adopts it.

When adopted:

- do not auto-promote to `v1.0.0`;
- require governance approval for promotion;
- warn on `v0.x.x + Active` only if the policy defines that as inconsistent.

## Ambiguous Cases

When version impact is ambiguous:

1. identify the semantic impact first;
2. check whether a baseline or source-of-truth relationship changed;
3. check repository version policy;
4. recommend the smallest bump that faithfully represents the impact;
5. explain uncertainty;
6. ask for approval/clarification when the decision changes baseline identity or stability.

Do not choose MAJOR merely to be safe. Do not choose PATCH when meaning changed.
