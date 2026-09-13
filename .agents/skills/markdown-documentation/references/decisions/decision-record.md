# Decision Record Reference

Use this reference for important decisions whose rationale should remain understandable after the original discussion is forgotten.

## 1. When a Decision Record Is Worth Creating

A decision is a good candidate when one or more apply:

- multiple credible options exist;
- the choice affects multiple modules or team members;
- reversing it later would be costly;
- it has meaningful complexity, cost, security, performance, or maintainability trade-offs;
- it establishes an important long-lived constraint;
- the rationale is likely to be questioned later.

Examples may include JWT vs session, monorepo vs split repositories, database choice, storage approach, or whether AI calls go through the backend.

## 2. When Not to Create One

Do not create a Decision Record for routine implementation details, naming, small refactors, CSS changes, minor local dependencies, or temporary experiments that are not accepted.

If the repository does not already require a Decision Record for the case and the user did not explicitly request one, propose it first according to document-lifecycle rules.

## 3. Recommended Structure

```text
# ADR-XXX — Decision Title
Status: Proposed | Accepted | Superseded | Rejected

## Context
## Decision Drivers
## Considered Options
## Decision
## Rationale
## Consequences
## Related Artifacts
```

Use repository conventions when they define another format.

## 4. Preserve History

Do not rewrite an accepted historical decision to make a newer choice appear original.
When a significant decision changes, prefer a new Decision Record that supersedes the old one or follow repository policy.

## 5. Responsibility Boundary

- Decision Record -> why this significant choice was made.
- Architecture -> how the current system is organized.
- SRS -> what behavior/constraints are required.
- Changelog -> what changed.
- Research Note -> evidence/recommendation before acceptance.
