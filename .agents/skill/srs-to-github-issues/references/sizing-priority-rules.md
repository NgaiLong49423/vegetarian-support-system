# Story Points, Priority, and Date Rules

## Story Points

Story Points estimate effort, uncertainty, and risk. They are not hours.

| Points | Meaning |
|---:|---|
| 1 | Very small, clear, and low-risk work |
| 2 | Small work with limited rules or edge cases |
| 3 | Medium work with several rules or integration points |
| 5 | Complex work that still fits within 4–5 calendar days |
| 8 | Too large or uncertain for assignment; must be split first |

An implementation Issue ready for assignment must be at most `5 SP`. Use `8 SP` only during planning to signal mandatory decomposition.

Each issue must include a short estimation reason.

Example:

```text
Story Points: 5
Estimation Reason: Medium scope, but higher effort because it affects validation, persistence, and business rule consistency.
```

## Priority

Priority must consider:

- Dependency order
- Core user workflow importance
- Risk if delayed
- Whether other issues are blocked by it
- Whether it is required before testing/integration

Allowed priority values should come from repository Project config or labels.

If `.github/labels.yml` exists, map priority only to labels defined there.

Do not invent priority labels.

## Dates

Default:

```text
Start Date: TBD
Target Date: TBD
```

Do not invent dates.

Only fill dates when the user provides a schedule.

`Target Date` is the deadline for the owner to pass the technical-completion gate and merge the feature PR into `develop`. It is not the `main` release date. After that merge, an Issue may stay in `Review` awaiting release without being overdue; an open PR or unresolved change request remains subject to the Target Date.

Examples:

```text
start from 2026-06-27
split this into 7 days
foundation issues on day 1
target all booking issues by Friday
```
