# Story Points, Priority, and Date Rules

Repository/team planning conventions override these fallback guidelines.

Do not invent estimates, priorities, dates, or policy thresholds merely because this reference contains examples.

## Story Points

Story Points estimate effort, uncertainty, and risk. They are not hours.

If the repository explicitly adopts the fallback Fibonacci-like scale below, use it:

| Points | Meaning |
|---:|---|
| 1 | Very small, clear, and low-risk work |
| 2 | Small work with limited rules or edge cases |
| 3 | Medium work with several rules or integration points |
| 5 | Complex work that still fits the team's normal implementation window |
| 8 | Large or uncertain work; consider decomposition before assignment |

Do not enforce `5 SP` as a universal maximum unless team/repository policy says so.

Each non-TBD estimate should include a short reason.

Example:

```text
Story Points: 5
Estimation Reason: Medium scope, but higher effort because it affects validation, persistence, and business rule consistency.
```

## Priority

Priority should consider, when applicable:

- Dependency order
- Core user workflow importance
- Risk if delayed
- Whether other Issues are blocked by it
- Whether it is required before testing/integration

Allowed priority values should come from repository Project configuration or labels.

Do not invent priority labels.

## Dates

Default:

```text
Start Date: TBD
Target Date: TBD
```

Do not invent dates.

Only fill dates when the user, project schedule, or repository planning policy provides them.

Interpret the meaning of `Target Date` according to repository/team workflow rather than assuming it always means merge-to-`develop` or release-to-`main`.
