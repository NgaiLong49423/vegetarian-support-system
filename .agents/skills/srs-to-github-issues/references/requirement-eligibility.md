# Requirement Eligibility for Issue Generation

Use this reference when the source requirements define lifecycle or readiness states, or when issue generation depends on whether a requirement is ready for implementation.

## Lifecycle Eligibility

| Requirement status | Default issue-generation behavior |
|---|---|
| `DRAFT` | Do not create an implementation-ready Issue by default. Keep as planning/Needs Review if useful. |
| `ACTIVE` | Eligible, subject to readiness and scope checks. |
| `DEFERRED` | Do not create current implementation work by default. |
| `OUT_OF_SCOPE` | Exclude from current implementation work. |
| `RETIRED` | Exclude from current implementation work. Never treat it as a new active requirement. |

These status names are project conventions, not universal standards. If the repository uses different status names, map them by meaning rather than renaming them.

## Readiness Eligibility

| Readiness | Default behavior |
|---|---|
| `Ready` | Eligible for implementation decomposition. |
| `Ready with open items` | Eligible only when the open items are explicitly non-blocking for the proposed Issue. Record relevant open items. |
| `Needs clarification` | Do not mark implementation work as Approved. Keep the source in `Needs Review` until blocking ambiguity is resolved. |

## Unknown Status or Readiness

Do not invent a status.

When the source does not define lifecycle/readiness metadata:

1. Use explicit source wording and repository governance.
2. Determine whether the requirement is clearly current and implementation-ready.
3. If uncertainty would change whether an Issue should be created, record `Needs Review` and ask for clarification.

## Traceability Without Work Creation

Excluded requirements may still appear in the planning index so the decomposition is auditable.

Example:

```text
FR-19 | OUT_OF_SCOPE | No implementation Issue | Excluded from current scope
FR-20 | ACTIVE / Ready | Issue 007 | Eligible
FR-21 | ACTIVE / Needs clarification | No approved Issue | Blocking ambiguity
```

Do not create fake implementation work just to make every source ID map to an Issue.
