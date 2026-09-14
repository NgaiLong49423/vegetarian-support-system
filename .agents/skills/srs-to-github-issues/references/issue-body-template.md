# Issue Body Template

Use the repository's Issue template when one exists. Use this fallback only when no stronger format applies.

Issue bodies should be Vietnamese by default unless the user requests another language.

When compatible with the repository template, place requirement-derived content inside a managed block so future SRS synchronization can update it without overwriting human notes.

```md
# <Professional English Issue Title>

<!-- srs-sync:start -->
## Mục tiêu
<Mô tả kết quả theo requirement nguồn.>

## Source Trace
- Root SRS Registry: docs/requirements/SRS.md (or monolithic SRS.md)
- Detailed Specification: docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md#fr-xx (when modular)
- Requirement: FR-xx
- Requirement Lifecycle: ACTIVE / DEFERRED / DRAFT / ... (from authoritative SRS registry)
- Requirement Readiness: Ready / ... / Not used
- Hierarchy Role: Parent / Leaf / Standalone

## Phạm vi Requirement

### Trong phạm vi
- ...

### Ngoài phạm vi
- ...

## Acceptance Criteria
- [ ] ...

## Open Items
- None / ...

## Dependencies
- Parent: None / ...
- Blocked by: None / ...
- Blocking: None / ...

## Planning Metadata
- Type: ...
- Labels: ...
- Owner/Assignee: TBD
- Story Points: TBD
- Priority: TBD
- Start Date: TBD
- Target Date: TBD
<!-- srs-sync:end -->

## Human Notes
<!-- Preserve manual notes outside the managed block. -->
```

## Required Rules

- Source Trace is mandatory.
- Requirement Lifecycle must be explicit; never infer it.
- Acceptance Criteria must come from the requirement meaning and must not add new semantics.
- Parent/capability Issues must not repeat child implementation acceptance scope.
- `DEFERRED` Issues must be clearly marked as deferred/backlog according to repository convention.
- `DRAFT` tracking Issues must not be presented as implementation-ready.
- `OUT_OF_SCOPE` / `RETIRED` do not create new implementation work.
- Do not invent technology, dates, dependencies, labels, lifecycle, readiness, or relationships.
- Preserve human notes/comments when synchronizing.
- If the managed boundary is missing or ambiguous on an existing manually edited Issue, report the conflict before rewriting the body.
