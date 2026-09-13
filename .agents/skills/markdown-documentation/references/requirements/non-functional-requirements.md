# Non-Functional Requirements Reference

Use this reference for quality attributes and system-wide constraints.

## 1. Create Only Meaningful Categories

Do not create empty NFR sections merely to resemble a standards checklist.
Create categories only when the project has meaningful requirements in them.

During review, consider whether important needs are missing in areas such as:

- performance;
- security;
- usability/accessibility;
- reliability;
- maintainability;
- compatibility;
- scalability;
- privacy.

This is a review prompt, not a requirement that every project define every category.

## 2. Prefer Measurable and Verifiable NFRs

Avoid vague wording such as “fast”, “secure”, “user-friendly”, or “robust”.

Never invent thresholds, SLA/SLO values, coverage percentages, or response-time targets to make an NFR look professional.
If the target is not confirmed, use `TBD` or `Needs clarification` according to impact.

When a metric is confirmed, include relevant context such as workload, environment, percentile, success rate, or failure tolerance only when supported by the project.

## 3. Separate Quality Goals from Technology Choices

`Use Redis` is not a performance requirement.
`Use HTTPS` may be an accepted security constraint, but the security objective should remain clear.

## 4. Minimum Viable Security for Typical Web Projects

When relevant to the project, check for requirements/constraints involving:

- authentication;
- authorization/access control;
- password handling;
- sensitive data;
- secrets/API keys;
- input validation;
- basic API protection.

Do not create a full security framework unless the repository explicitly adopts one.

## 5. Review Questions

- Is this truly a quality/constraint rather than a function?
- Can the team verify it?
- Is the target confirmed rather than invented?
- Does the project actually need this category?
- Does the NFR duplicate an architecture decision or technology-stack entry?
