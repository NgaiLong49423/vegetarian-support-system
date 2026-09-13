# Decomposition Rules

## Default Strategy

Prefer one implementation Issue per **implementation-ready leaf requirement**, not one Issue per every FR identifier.

Every requirement-derived Issue must have Source Trace.

## Parent vs Leaf Functional Requirements

When requirements use hierarchy:

```text
FR-03 — Recipe Management
├── FR-03.1 — Create Recipe
├── FR-03.2 — Save Draft
└── FR-03.3 — Publish Recipe
```

Treat `FR-03` primarily as a business capability / grouping context when its child FRs contain the independently testable implementation behavior.

Normally:

- `FR-03` -> parent/epic context when useful;
- `FR-03.1` -> implementation Issue;
- `FR-03.2` -> implementation Issue;
- `FR-03.3` -> implementation Issue.

Do not also create a duplicate implementation Issue for `FR-03` that repeats the scope of its child Issues.

A standalone FR with no child hierarchy may map directly to an implementation Issue.

## Split One Leaf Requirement When

Split an implementable leaf requirement when:

- It is too large for one implementation Issue under repository/team planning rules.
- It contains multiple independent workflows.
- It has important dependencies that need separate delivery.
- It has high uncertainty or risk that benefits from separate investigation/work.
- It contains separate testable behavior slices.

If the repository uses the default sizing guidance, an `8 SP` estimate is a planning signal to split before assignment. Do not impose that rule when the repository uses another sizing model.

## Group Requirements When

Group multiple leaf requirements only when:

- They are small.
- They are strongly coupled.
- They are implemented in the same workflow.
- Splitting would create artificial work items.
- Grouping does not hide independent acceptance behavior or traceability.

Do not group many FRs into one Issue only for convenience.

## Vertical Slice Rule

Prefer vertical slices.

A good Issue should represent meaningful, testable behavior.

Avoid splitting only by code layer unless the source document or workflow requires it.

Bad split:

```text
Create DAO
Create Service
Create Controller
Create UI
```

Better split:

```text
Implement Booking Creation With Window Validation
```

## Epic / Parent Rules

Create or propose an Epic/parent Issue when:

- a meaningful capability coordinates multiple implementation work items;
- a parent requirement has several child requirements whose work benefits from shared planning context;
- a large requirement must be split into independently testable Issues and grouping helps navigation.

Do not create an Epic solely because an arbitrary numeric child threshold is reached.

Do not create real Epic/parent Issues unless the user approves when real GitHub creation is requested.
