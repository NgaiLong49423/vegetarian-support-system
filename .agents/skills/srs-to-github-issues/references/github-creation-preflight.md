# GitHub Creation and Project Sync Preflight

Use this checklist before any real GitHub action.

## Before Creating Real GitHub Issues

- [ ] User explicitly requested real Issue creation.
- [ ] `ISSUE_INDEX.md` exists.
- [ ] Selected draft files are listed in `ISSUE_INDEX.md`.
- [ ] Every selected draft file exists.
- [ ] No unreferenced draft files remain in the final draft directory.
- [ ] No local `file:///` paths exist in drafts or index.
- [ ] Selected drafts are `Approved`, or the authorized decision-maker explicitly selected another state.
- [ ] Each selected source requirement is eligible for the requested Issue type.
- [ ] No selected implementation Issue comes from `DEFERRED`, `OUT_OF_SCOPE`, or `RETIRED` requirements unless the requirement state has been explicitly changed or the user requested a non-implementation tracking Issue.
- [ ] No blocking `Needs clarification` source is being created as an implementation-ready Issue.
- [ ] Parent capability FRs do not duplicate child implementation Issue scope.
- [ ] No duplicate real Issue already represents the same source scope when that can be determined confidently.
- [ ] Labels/types conform to repository configuration.
- [ ] Issue bodies include Source Trace.
- [ ] Full Issue bodies will not be printed in chat unless requested.

If any check fails, stop and report the failure.

## Before Syncing GitHub Project Metadata

- [ ] User explicitly requested Project sync.
- [ ] Project owner is known.
- [ ] Project number or Project ID is known.
- [ ] Field IDs are known.
- [ ] Option IDs are known for single-select fields.
- [ ] Issue item IDs are known.
- [ ] No field is guessed.
- [ ] Relationship sync is best-effort only.
- [ ] Relationship target Issue IDs can be resolved if relationship sync is attempted.

Do not sync if IDs cannot be determined confidently.

## Allowed Actions

Allowed only after explicit approval:

- create GitHub Issues from selected eligible drafts;
- create missing labels only when repository policy/user approval permits it;
- update `ISSUE_INDEX.md` after successful creation;
- sync verified GitHub Project fields.

## Forbidden Actions

Never do these as part of this skill unless separately and explicitly requested:

- Close Issues
- Delete Issues
- Delete labels
- Delete branches
- Force-push
- Modify source code
- Create branches
- Merge pull requests
