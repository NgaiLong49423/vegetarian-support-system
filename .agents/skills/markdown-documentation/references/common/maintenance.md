# Documentation Maintenance Reference

Use this reference for targeted updates to existing documentation.

## 1. Read Before Editing

Read the target artifact and directly related authoritative sources needed to understand the change.
Preserve existing terminology, IDs, accepted decisions, and document responsibility unless the task explicitly changes them.

## 2. Editorial vs Semantic Change

An editorial change preserves project meaning. Examples:

- spelling and grammar;
- formatting;
- wording simplification;
- clarity improvements;
- terminology normalization that preserves the established concept.

A semantic change modifies meaning. Treat changes to any of the following as semantic unless clearly proven otherwise:

- actor or role;
- precondition or trigger;
- scope;
- obligation strength;
- business rule;
- exception;
- threshold or target;
- data requirement;
- expected outcome;
- acceptance behavior.

If uncertain whether meaning changes, treat the proposed edit as semantic and seek/confirm authorization.

## 3. Review vs Edit Boundary

Make the smallest coherent change.

When the task is targeted:

- edit items inside scope;
- edit directly affected artifacts only when necessary to preserve consistency;
- report unrelated findings separately;
- do not “clean up” unrelated files merely because they were discovered.

When the user explicitly requests a broad review-and-fix task, the edit scope may be broader.

## 4. Authorized Semantic Change Propagation

When a semantic change is authorized:

1. Identify directly affected authoritative artifacts.
2. Update artifacts whose required change is clear and necessary.
3. Preserve stable IDs unless a separate approved migration changes them.
4. Do not speculate about indirect effects.
5. Report uncertain or broader impacts for review.

Example: changing a meal recommendation rule may require updating an FR, related BR, a matching Use Case, and affected tests, but not an unrelated README section.

## 5. Stale Content

Update stale content only when it is directly affected by the requested change or when leaving it untouched would create an obvious contradiction.
Report other stale content instead of expanding scope silently.

## 6. Rename and Move

When the task renames or moves documentation, use the lifecycle reference and update confirmed links/references to the old path.
Do not combine path maintenance with unrelated content cleanup.

## 7. Uncertainty

Do not hide uncertainty through polished prose.
Use `TBD`, open items, or `Needs clarification` according to impact.
