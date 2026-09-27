# Current-Scope Eligibility for Issue Management

For this repository's Requirements / Implementation Baseline v2.0.0, the root `docs/requirements/SRS.md` registry is the authority for current requirement existence and scope. An FR present there belongs to current implementation scope. An FR absent there is non-current; historical lifecycle labels in `docs/archive/requirements/v1/` do not restore it to current scope.

## Mandatory Scope Rule

- Include every FR in the current root registry in the current requirement-to-Issue index.
- Do not create implementation Issues from archive-only requirements.
- Keep existing Issue mappings for non-current requirements as historical traceability.
- Before proposing closure, verify the FR-to-Issue mapping and confirm the Issue is unfinished. Completed work remains history.
- If root SRS and a detailed child disagree about whether an FR is current, root SRS wins; report the child-only entry and do not create scope from it.
- If the current registry or Issue mapping is ambiguous, stop only the affected action and request the missing authoritative evidence.

## Issue Treatment

| Requirement evidence | Index behavior | Default Issue treatment |
|---|---|---|
| FR present in current root SRS | Current | Maintain or create one traceable implementation/tracking Issue, without duplicating parent/child scope |
| FR absent from current root SRS, linked Issue unfinished | Historical / non-current | Preserve mapping; propose close-as-not-planned only when the current baseline and archive evidence confirm the scope change |
| FR absent from current root SRS, linked Issue completed | Historical / completed | Preserve Issue and mapping; do not rewrite or reopen solely due to the new baseline |
| FR present in current root SRS, linked Issue completed but new semantics require work | Current with completed history | Preserve completed Issue and propose a follow-up Issue for new work |

Execution readiness is separate from scope. Do not invent readiness or exclude a current FR because planning details remain open; report blocking ambiguities for that FR.

## Examples

```text
FR-01 | Current | linked open issue -> maintain/update
FR-02 | Current | no linked issue -> create candidate
FR-03 | Historical / non-current | linked completed issue -> preserve
FR-04 | Historical / non-current | linked unfinished issue -> close candidate after history verification
```
