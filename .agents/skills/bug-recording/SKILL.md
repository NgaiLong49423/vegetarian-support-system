---
name: bug-recording
description: Record credible bugs discovered incidentally during repository work or during a user-requested bug audit. Use it to create one bug file per finding and maintain the bug metadata index without expanding task scope; read-only instructions override file recording, and GitHub Issue creation always requires explicit current-task authorization.
metadata:
  swp391-risk: "medium"
  swp391-source: "customized"
  swp391-version: "v1.2.0"
  swp391-created-date: "2026-09-20"
  swp391-last-updated-date: "2026-09-21"
---

# Bug Recording

## Purpose

Preserve a credible bug that an agent encounters during repository work so it is not forgotten. This skill records and reports evidence; it is not a bug lifecycle manager, automatic fixer, or automatic Issue manager.

## Modes

### Passive bug recording

This is the default cross-cutting mode. Do not scan proactively. When a credible bug signal appears during ordinary coding, review, testing, debugging, database, CI, requirement, API-contract, or implementation-documentation work:

1. Identify the expected behavior and the observed behavior.
2. Confirm that available evidence supports a meaningful defect rather than a transient tool or test problem.
3. Read `.agents/outputs/bugs/bugs-metadata.yaml` and search the referenced bug files briefly for an equivalent symptom and area.
4. If no equivalent record exists and local writes are allowed, allocate `next_id`, create one `.agents/outputs/bugs/BUG-xxx.md` file, add its summary to the metadata index, increment `next_id`, and update `last_updated`.
5. If an equivalent record exists, report `Matches existing BUG-xxx`; add genuinely new evidence to that bug file and update its metadata timestamp only when local writes are allowed.
6. Report the result to the user and continue the original task when it remains safe to do so.

An explicit `read-only`, `plan-only`, `review only`, `do not modify files`, or equivalent user instruction takes precedence. In that case, report the bug in the conversation and do not create or update bug output files unless the user later authorizes a write.

### Explicit bug audit

Use this mode only when the user explicitly asks to search or audit for bugs. Select checks that fit the requested scope, such as requirements, code review, build, JUnit, API/integration tests, Playwright, browser console/network, runtime logs, database, CI, or existing evidence.

Report the inspected scope, evidenced bugs and record IDs, evidence for each finding, unavailable checks, and areas not inspected. Do not infer that a merge or release requires an audit; wait for the user's request.

## Credibility gate

Record a bug when evidence reasonably establishes that expected behavior differs from actual behavior or that another meaningful project defect should be preserved.

Do not record ordinary editing mistakes, expected failing tests, an incorrect test or locator, a missing tool, a stopped development server, a transient network hiccup, an agent sandbox failure, an unsupported suspicion, or a warning that does not establish a defect.

Root cause, severity, reproduction steps, environment, requirement, Issue, and PR may remain `Unknown` when evidence does not establish them. Never invent missing information.

## Storage and status

Write bug-file titles, headings, explanations, and index titles/status descriptions in Vietnamese under this repository's language policy. Preserve IDs, YAML keys, paths, technical identifiers, and machine-readable status values. Unknown fields may be explained as `Chưa xác định` in prose; keep the index sentinel `Unknown`. Translation must preserve evidence and detection dates and must not imply that a bug is resolved.

Store each bug separately at `.agents/outputs/bugs/BUG-xxx.md`. Maintain `.agents/outputs/bugs/bugs-metadata.yaml` as the compact index; do not duplicate full evidence into the index.

Each metadata entry must contain `id`, `title`, `status`, `severity`, `area`, `file`, `created`, `updated`, and `related_issue`. Use `Unknown` or an empty YAML value when the corresponding fact is not established. `next_id` must always point to the next unused sequential number.

Use only these statuses:

- `RECORDED`: credible bug captured; no active investigation or verified resolution is recorded.
- `TRACKING`: the bug is being monitored or investigated but is not verified as resolved.
- `RESOLVED`: the bug file records both the resolution and verification evidence.

New bugs start as `RECORDED`. Do not infer `TRACKING` from elapsed time or `RESOLVED` from a code change alone. Keep the status identical in the bug file and metadata index.

Each bug file must contain its ID/title, status, area, severity, detection context, optional branch/commit and requirement/Issue/PR links, dates, symptom, expected behavior, actual behavior, evidence, reproduction, environment, root cause, resolution, and verification. In Vietnamese prose use `Chưa xác định` for unknown values and `Chưa bắt đầu` for an untouched resolution.

Observed branch and commit are optional context only. They must not drive a decision to fix, promote, classify, or create an Issue.

The v1 store uses sequential IDs. If concurrent work later causes collisions or repeated merge conflicts, report that limitation rather than inventing a new ID scheme.

## GitHub Issue human gate

A bug record is technical evidence; a GitHub Bug Issue is an official work item. They are not equivalent.

Never create a GitHub Bug Issue automatically. Creation requires an explicit current-task instruction naming the bug or approved set of bugs. When authorized:

1. Read the current `.github/ISSUE_TEMPLATE/bug_report.yml` and `CONTRIBUTING.md`.
2. Check existing Issues for a reasonable duplicate match.
3. Map verified record evidence to the current template without copying the template into this skill.
4. Stop and report when required information cannot be supplied truthfully.
5. Create only the authorized Issues and verify their real URLs and numbers.
6. Link the Issue in both the individual bug file and metadata index when local writes are allowed and the linkage is useful.

Do not assign an owner, set priority, close the Issue, fix the bug, commit, or push unless the user separately authorizes that action.

## Scope boundary

Recording a newly discovered bug does not authorize code changes, refactoring, regression tests, root-cause analysis, prevention rules, remediation, or expansion of the active task. If the user's active task already authorizes fixing that bug, the task—not this skill—provides the authority.
