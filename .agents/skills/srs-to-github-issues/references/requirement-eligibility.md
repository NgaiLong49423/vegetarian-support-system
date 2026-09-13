# Requirement Lifecycle for Issue Management

Use this reference to decide how an explicitly confirmed requirement lifecycle state affects GitHub work-item management.

## Mandatory Rule: No Lifecycle Guessing

Every FR managed by this skill must have an explicit lifecycle state in the authoritative source or an explicit authorized decision.

Fallback vocabulary:

```text
DRAFT
ACTIVE
DEFERRED
OUT_OF_SCOPE
RETIRED
```

If the project uses equivalent names, map by meaning.

If status is missing or ambiguous:

1. record the FR as `Lifecycle unresolved` in the local planning result;
2. do not create, close, reopen, or materially update a real Issue for that FR;
3. ask the authorized decision-maker to choose the lifecycle state;
4. continue after the state is explicit.

Do not infer lifecycle from implementation progress, position in the SRS, GitHub Issue state, version number, or model judgment.

## Lifecycle-to-Issue Behavior

| Requirement status | Index behavior | Default real-Issue behavior |
|---|---|---|
| `DRAFT` | Always tracked | Planning/tracking Issue only when the workflow intentionally tracks drafts; never present as implementation-ready by default |
| `ACTIVE` | Always tracked | Open current implementation/tracking Issue |
| `DEFERRED` | Always tracked | Keep/create backlog Issue marked deferred; do not place in active implementation automatically |
| `OUT_OF_SCOPE` | Always tracked for history | Do not create new implementation work; close linked unfinished Issue as not planned when authorized |
| `RETIRED` | Always tracked for history | Do not create new work; preserve historical mapping and close linked unfinished Issue as not planned when authorized |

`OUT_OF_SCOPE` and `RETIRED` do not mean delete the Issue mapping.

## SRS Means the FR Is Managed

The agent must not independently exclude an FR from management because it seems trivial, difficult, low-priority, or inconvenient.

Every FR with explicit lifecycle belongs in the index.

Hierarchy controls **Issue role**, not whether the FR disappears:

- parent/capability FR -> parent/tracking Issue where useful;
- leaf/standalone FR -> implementation Issue when lifecycle permits current/future work.

## Readiness

If the project explicitly tracks readiness:

| Readiness | Effect |
|---|---|
| `Ready` | May be presented as implementation-ready |
| `Ready with open items` | May proceed when open items are non-blocking and recorded |
| `Needs clarification` | Keep the Issue/FR tracked, but do not present it as ready for implementation until the blocking ambiguity is resolved |

If the project does not track readiness, do not invent it.

Readiness does not replace lifecycle and does not make an FR disappear from the index.

## Examples

```text
FR-01 | ACTIVE      | current implementation Issue
FR-02 | DEFERRED    | backlog/deferred Issue
FR-03 | DRAFT       | tracked; planning Issue only if draft tracking is enabled
FR-04 | OUT_OF_SCOPE| historical mapping; no new implementation Issue
FR-05 | RETIRED     | historical mapping; no new work
FR-06 | <missing>   | HARD BLOCKER: ask authorized decision-maker
```
