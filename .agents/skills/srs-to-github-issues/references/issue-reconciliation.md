# Issue Reconciliation After SRS Changes

Use this reference whenever requirement-linked Issues already exist and the SRS changes.

## Principle

The SRS is the requirement source of truth. The Issue is a managed execution representation.

Reconcile by stable requirement ID and stored Issue mapping, not by title similarity alone.

## Change Classification

### Editorial SRS Change

If meaning is unchanged:

- update managed Issue wording only when useful for consistency;
- do not change Issue state solely because of editorial wording;
- do not create follow-up work.

### Semantic SRS Change — Open/Unfinished Issue

When authorized requirement meaning changes and the linked Issue is still unfinished:

- update the managed objective/scope/acceptance content;
- preserve discussion/comments;
- update labels/dependencies only when supported by repository policy/evidence;
- keep the same Issue number when it still represents the same work identity.

### Semantic SRS Change — Completed Issue

A completed Issue is historical evidence of work performed under the earlier requirement state.

Do not rewrite it as if the new requirement had always existed.

Default:

1. preserve the completed Issue;
2. create a follow-up change Issue for new implementation work;
3. link the follow-up to the same FR and to the completed Issue;
4. record both in `ISSUE_INDEX.md`.

Repository policy may explicitly choose reopening instead.

## Lifecycle Transitions

### `DRAFT -> ACTIVE`

Promote/create the current implementation Issue after authorization and preflight.

### `ACTIVE -> DEFERRED`

Keep the linked Issue, mark it deferred/backlog, and remove it from active implementation status according to repository workflow.

### `DEFERRED -> ACTIVE`

Reactivate the existing linked unfinished Issue rather than creating a duplicate.

### `ACTIVE/DEFERRED -> OUT_OF_SCOPE`

For an unfinished linked Issue, close as not planned when authorized. Preserve the mapping and reason.

### `ACTIVE/DEFERRED -> RETIRED`

For unfinished linked work, close as not planned when authorized. Preserve historical mapping.

### `OUT_OF_SCOPE/RETIRED -> ACTIVE`

- reopen the linked Issue if it is the correct unfinished historical work item;
- if the old Issue was already completed or no longer represents the new scope, create a follow-up and preserve the old mapping.

## Requirement Split

When an FR becomes a parent with new child FRs:

- retain the parent mapping as tracking/context;
- create child mappings for the new stable child IDs;
- avoid duplicate implementation acceptance criteria in the parent;
- preserve old Issue history.

## Requirement Merge or Supersession

Do not delete old Issues.

Record supersession in the index and close unfinished superseded Issues only when the requirement lifecycle/decision authorizes it.

## Requirement Missing From Current SRS

Absence alone is not enough to infer `OUT_OF_SCOPE` or `RETIRED`.

If a previously mapped FR disappears without an explicit lifecycle/history decision:

- do not close/delete/reassign its Issue;
- inspect authoritative history/governance if available;
- otherwise ask the authorized decision-maker.

## Human Content Protection

Never delete comments.

When the Issue body contains a managed block, update only that block.

When no safe managed boundary exists and human-written body content would be overwritten, stop and present the proposed change instead of destructive replacement.
