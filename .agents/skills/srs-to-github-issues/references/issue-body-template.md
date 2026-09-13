# Issue Body Template

Use this template for requirement-derived GitHub Issue drafts when the repository does not provide a stronger Issue template.

Issue bodies should be Vietnamese by default unless the user asks for another language.

```md
# <Professional English Issue Title>

## Mục tiêu
Mô tả kết quả có thể kiểm tra sau khi Issue hoàn thành.

## Source Trace
- PRD/Product source: ...
- SRS/Spec: ...
- FR/NFR/UC/Business Rule: ...
- Source Lifecycle: ...
- Source Readiness: ...

## Phạm vi

### Trong phạm vi
- ...

### Ngoài phạm vi
- ...

## Acceptance Criteria
- [ ] ...
- [ ] ...
- [ ] ...

## Open Items
- None / ...

## Dependencies
- Parent: None
- Blocked by: None
- Blocking: None

## Project Metadata
- Type: ...
- Labels: ...
- Owner/Assignee: TBD
- Story Points: TBD
- Estimation Reason: TBD / ...
- Priority: TBD
- Priority Reason: TBD / ...
- Start Date: TBD
- Target Date: TBD

## Suggested Branch
`feature/example-branch-name`
```

## Required Rules

- Follow repository Issue templates and type systems when present.
- Fallback title should be clear professional English.
- Body should be Vietnamese by default unless the user requests another language.
- Source Trace is mandatory for requirement-derived Issues.
- Acceptance Criteria must be testable and must not introduce new requirement semantics.
- Do not invent technology, behavior, dates, dependencies, relationships, lifecycle, or readiness. Use `TBD`, `Unknown`, `None`, or `Needs Review` when appropriate.
- If source lifecycle/readiness is available, include it so reviewers can verify Issue eligibility.
- `Ready with open items` Issues must list the relevant non-blocking open items.
- `Needs clarification` requirements must not be presented as implementation-ready Approved Issues.
- Labels must come from repository configuration when present.
- Owner, Story Points, Priority, Start Date, and Target Date may remain `TBD` unless repository policy requires them earlier.
- Dependencies should record Parent, Blocked by, and Blocking when the repository workflow uses those relations.
- Suggested branch names are recommendations only; this skill does not create branches.
