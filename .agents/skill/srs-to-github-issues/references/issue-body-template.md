# Issue Body Template

Use this template for requirement-derived GitHub Issue drafts.

Issue bodies should be Vietnamese by default unless the user asks for another language.

```md
# <Professional English Issue Title>

## Mục tiêu
Mô tả kết quả có thể kiểm tra sau khi Issue hoàn thành.

## Source Trace
- PRD: ...
- SRS/Spec: ...
- FR/NFR/UC/Business Rule: ...

## Phạm vi

### Trong phạm vi
- ...

### Ngoài phạm vi
- ...

## Acceptance Criteria
- [ ] ...
- [ ] ...
- [ ] ...

## Dependencies
- Parent: None
- Blocked by: None
- Blocking: None

## Project Metadata
- Type: ...
- Labels: ...
- Owner/Assignee: TBD
- Story Points: ...
- Estimation Reason: ...
- Priority: ...
- Priority Reason: ...
- Start Date: TBD
- Target Date: TBD

## Suggested Branch
`feature/example-branch-name`

```

## Required Rules

- Title must be professional English.
- Body should be Vietnamese by default.
- Source Trace is mandatory.
- Acceptance Criteria must be testable.
- Do not invent technology, behavior, dates, dependencies, or relationships. Use `TBD` or `None` when appropriate.
- Labels must come from `.github/labels.yml` when present.
- Type must be exactly one of: `🐛 Bug`, `✨ Feature`, `📋 Task`, `♻️ Refactor`, `📝 Docs`, or `🔍 Research`.
- Labels must include the exact label matching the selected Type; secondary labels are optional.
- Owner, Start Date, and Target Date may remain `TBD` while the draft or real Issue is in `Backlog` or `Planning`.
- Target Date means the expected technical-completion and feature-PR merge date for `develop`, not the release date for `main`.
- Before moving to `In Progress`, the Issue must satisfy the repository's Definition of Ready.
- Dependencies must record Parent, Blocked by, and Blocking even when their values are `None`.
- An implementation Issue must be no more than `5 SP` before assignment; `8 SP` means it must be split.
- Suggested branch must be English kebab-case.
