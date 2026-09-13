# Decomposition Rules

## Coverage Principle

Every FR with explicit lifecycle must appear in the Issue registry. Hierarchy determines the **kind of Issue**, not whether the FR silently disappears.

## Parent vs Leaf Functional Requirements

Example:

```text
FR-03 — Recipe Management
├── FR-03.1 — Create Recipe
├── FR-03.2 — Save Draft
└── FR-03.3 — Publish Recipe
```

Default mapping:

- `FR-03` -> parent/tracking Issue when real Issue tracking is enabled for the capability;
- `FR-03.1` -> implementation Issue when lifecycle permits;
- `FR-03.2` -> implementation Issue when lifecycle permits;
- `FR-03.3` -> implementation Issue when lifecycle permits.

The parent Issue coordinates and links child work. It must not duplicate the children's detailed implementation acceptance criteria.

A standalone FR maps directly to its own Issue role according to lifecycle.

## Split One Leaf Requirement When

Split a leaf requirement into multiple delivery Issues only when necessary, for example when:

- it contains multiple independently deliverable/testable slices;
- repository/team planning rules require smaller work units;
- dependencies need separate delivery;
- uncertainty/risk benefits from separate investigation;
- one Issue would hide important traceability.

All split Issues must retain the same FR Source Trace, and `ISSUE_INDEX.md` must record the split.

Do not invent child FR IDs merely to support Issue splitting.

## Group Requirements When

Group multiple FRs into one implementation Issue only when:

- they are strongly coupled;
- they are delivered in the same workflow;
- grouping does not obscure independent acceptance behavior;
- the registry still shows every source FR explicitly.

A grouped Issue must list all source FR IDs.

## Vertical Slice Rule

Prefer meaningful, testable behavior over code-layer task fragmentation.

Avoid default splits such as:

```text
Create DAO
Create Service
Create Controller
Create UI
```

when the requirement can be represented as a vertical behavior slice.

## Lifecycle and Decomposition

- `ACTIVE` -> current implementation/tracking work.
- `DEFERRED` -> backlog/future work; decomposition may exist, but do not activate it automatically.
- `DRAFT` -> planning representation only unless the project intentionally tracks draft work.
- `OUT_OF_SCOPE` -> preserve registry/history; no new implementation decomposition.
- `RETIRED` -> preserve registry/history; no new work.

Missing lifecycle is a hard blocker; never decompose by guessing.
