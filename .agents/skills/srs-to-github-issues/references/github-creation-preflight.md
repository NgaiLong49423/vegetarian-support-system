# GitHub Creation and Reconciliation Preflight

Use this checklist before any real GitHub mutation.

## Hard Blockers

A hard blocker must be resolved before the affected mutation. A simple "continue anyway" does not override missing authority, missing source state, or an ambiguous target.

- [ ] Real GitHub mutation is authorized by the current task or adopted repository workflow.
- [ ] Repository owner/name is known for the target repository.
- [ ] Every affected FR has an explicit lifecycle state from the authoritative SRS registry or authorized decision, and no unresolved lifecycle conflict exists between root registry and detailed specification.
- [ ] The target Issue mapping is known, or creation of a new Issue is clearly intended.
- [ ] No conflicting authoritative requirement sources remain unresolved for the affected scope.
- [ ] Parent/capability tracking scope does not duplicate child implementation scope.
- [ ] No requirement ID is being reused for a different meaning.
- [ ] A requirement removed from the active SRS is not being treated as retired/out-of-scope without explicit lifecycle/history evidence.

If any hard blocker fails, stop the affected mutation and report exactly what must be resolved.

## Creation Checks

- [ ] `ISSUE_INDEX.md` or repository-equivalent mapping exists.
- [ ] Selected draft files, when used, are listed in the index.
- [ ] Every selected draft file exists.
- [ ] No local `file:///` paths exist in content to be posted.
- [ ] Source Trace is present.
- [ ] Lifecycle-to-Issue behavior matches `references/requirement-eligibility.md`.
- [ ] Existing linked Issue was checked to avoid duplicate creation.
- [ ] Labels/types conform to repository configuration or are explicitly authorized.
- [ ] Full Issue bodies will not be printed in chat unless requested.

## Reconciliation Checks

- [ ] Existing Issue was resolved by stable source mapping, preferably FR ID + stored Issue link/number.
- [ ] Current SRS content was compared with the Issue's managed requirement-derived content.
- [ ] Human comments and unrelated manual notes will be preserved.
- [ ] Closed-completed Issues will not be rewritten as if their historical completed scope had always been different.
- [ ] Semantic changes to completed work will use a follow-up Issue unless repository policy explicitly prefers reopening.
- [ ] `OUT_OF_SCOPE` / `RETIRED` transitions will not delete Issues or comments.
- [ ] Re-activation will reuse/reopen an appropriate unfinished historical Issue rather than create a duplicate when possible.

## Overridable Workflow Warnings

These may be accepted by an authorized user when repository policy permits:

- optional Story Points are `TBD`;
- optional Priority is `TBD`;
- optional dates are `TBD`;
- relationship metadata cannot be synchronized although the core Issue can be safely created/updated;
- optional Project metadata is unavailable while core Issue synchronization can proceed.

Record the accepted warning; do not silently pretend the missing metadata was synchronized.

## GitHub Project Sync Checks

Before Project-field mutation:

- [ ] Project owner is known.
- [ ] Project number or Project ID is known.
- [ ] Field IDs are known.
- [ ] Option IDs are known for single-select fields.
- [ ] Issue item IDs are known.
- [ ] No field or option is guessed.

Project-sync ID failures block only the Project mutation when the core Issue operation is otherwise valid.

## Never Do Automatically

- delete Issues;
- delete comments;
- rewrite human discussion history;
- force-push;
- delete branches;
- merge pull requests;
- modify source code merely to satisfy an Issue;
- create branches unless separately requested;
- infer requirement lifecycle from GitHub state.
