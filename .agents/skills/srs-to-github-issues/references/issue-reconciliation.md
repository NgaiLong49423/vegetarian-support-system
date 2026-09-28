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

## Current-Scope Changes

For this repository's v2 baseline, current scope is determined by presence in the root SRS registry, not by lifecycle labels.

### FR enters the current registry

- Check for a historical Issue mapping by stable FR ID.
- Reopen an appropriate unfinished Issue when it still represents the current work; otherwise create a new Issue candidate.
- Preserve any completed historical Issue and do not rewrite its scope.

### FR leaves the current registry

- Preserve the historical mapping and verify the linked Issue state.
- For unfinished work, propose closing as not planned only when the current baseline/archive evidence confirms the scope change.
- Preserve completed Issues as history; do not rewrite, delete, or reopen them solely because the FR left current scope.

## Requirement Split

When an FR becomes a parent with new child FRs:

- retain the parent mapping as tracking/context;
- create child mappings for the new stable child IDs;
- avoid duplicate implementation acceptance criteria in the parent;
- preserve old Issue history.

## Requirement Merge or Supersession

Do not delete old Issues.

Record supersession in the index and propose closing unfinished superseded Issues only when the current baseline/history evidence supports the scope change.

## Requirement Missing From Current SRS

Absence from the current root registry means non-current scope in this repository's v2 baseline, but does not automatically authorize a remote write. Verify the Issue mapping, archive/history, completion state and approval preview before proposing a close. If this evidence is ambiguous, stop the affected action and ask for direction.

## Human Content Protection

Never delete comments.

When the Issue body contains a managed block, update only that block.

When no safe managed boundary exists and human-written body content would be overwritten, stop and present the proposed change instead of destructive replacement.
