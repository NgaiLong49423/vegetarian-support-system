# Decomposition Rules

## Coverage Principle

Every FR present in the current root SRS registry must appear in the Issue registry. Hierarchy determines the **kind of Issue**, not whether a current FR is omitted.

## Parent vs Leaf Functional Requirements

Example:

```text
FR-X — Parent capability
├── FR-X.1 — Independently deliverable slice A
├── FR-X.2 — Independently deliverable slice B
└── FR-X.3 — Independently deliverable slice C
```

Default mapping:

- `FR-X` -> parent/tracking Issue when real Issue tracking is enabled for the capability;
- each current child FR -> implementation Issue when its scope and hierarchy justify independent tracking.

The parent Issue coordinates and links child work. It must not duplicate the children's detailed implementation acceptance criteria.

A standalone current FR maps directly to its own Issue role.

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

## Current Scope and Decomposition

For this repository's v2 baseline, presence in the current root SRS registry defines current implementation scope. Archive-only requirements remain historical and must not generate new implementation work. Preserve existing Issue mappings for history; reconcile unfinished work through the approved preview gate.
