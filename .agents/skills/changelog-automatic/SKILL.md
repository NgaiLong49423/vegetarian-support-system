---
name: changelog-automatic
description: Maintain a repository's changelog from verified project evidence and repository-specific changelog policy. Preserve chronology, avoid fabricated release or PR information, and record only meaningful release- or milestone-relevant changes.
risk: medium
source: customized
version: v2.1.1
created_date: 2026-06-29
last_updated_date: 2026-09-13
---

# Changelog Maintenance

## Purpose

Use this skill to create, update, normalize, or review changelog entries.

This skill owns **changelog mechanics and evidence handling**:

- locating the repository's changelog policy;
- identifying the correct changelog entry or section;
- verifying the evidence behind an entry;
- preserving dates and chronology;
- avoiding duplicate entries;
- recording commit or pull-request references only when verified;
- keeping changelog wording aligned with the repository's established format.

It does **not** replace requirements, decision records, architecture documentation,
Git history, release tooling, or document-metadata policy.

## Trigger

Use this skill when the task includes any of the following:

- updating `CHANGELOG.md` or the repository's equivalent changelog;
- converting or cleaning historical changelog entries;
- preparing changelog content for a release or milestone;
- reviewing whether existing changelog entries agree with verified project evidence;
- consolidating duplicate changelog entries for the same coherent change.

Do not load this skill for unrelated source-code or documentation edits merely because
files changed. A file change is not automatically changelog-worthy.

## Authority and repository policy

Follow authority in this order:

1. an explicit instruction from the user within the user's authorized scope, or an authorized project decision;
2. applicable repository governance such as `AGENTS.md`;
3. the repository's adopted changelog policy, when one exists;
4. the established format of the current changelog;
5. this skill's generic defaults.

Do not hard-code `CONTRIBUTING.md` as the authority for every repository.
Consult it when repository governance requires it or when it contains the applicable
changelog/contribution policy.

A repository may instead define changelog behavior in another governance file,
a repository contract, release documentation, or the changelog itself.

Do not replace an adopted repository format with a generic convention such as
Keep a Changelog unless the project explicitly adopts that convention.

## Ownership boundaries

### Shared documentation semantics

A general documentation skill may determine whether a documentation or requirement
change is meaningful, semantic, release-relevant, or part of a baseline migration.
This skill then owns how that approved change is represented in the changelog.

Do not use the changelog to redefine requirements, architecture, business rules,
or decision rationale.

### Document metadata

Document metadata and document-version decisions belong to the repository's metadata
procedure or a dedicated metadata-standardization skill when available.

This skill may detect that changelog metadata appears stale, but it must not invent a
metadata version bump independently.

### Git and release actions

This skill may inspect evidence needed to write an entry, but it does not authorize:

- commits;
- tags;
- pull-request creation or modification;
- merges;
- releases;
- deployment;
- release automation.

Those actions require separate authorization and tooling.

## What belongs in a changelog

Prefer changes that matter to a release, milestone, project behavior, supported
workflow, compatibility contract, or meaningful project documentation baseline.

Typical candidates include:

- a user-visible feature or capability added, changed, deferred, or removed;
- a meaningful bug fix;
- an API or compatibility change;
- a major architecture or dependency decision with release impact;
- a significant requirement-baseline change;
- a meaningful security or reliability correction;
- a migration that affects how the project is built, run, configured, or used.

Normally exclude low-level noise such as:

- variable renames;
- formatting-only edits;
- typo fixes;
- internal refactors with no meaningful external or milestone effect;
- routine metadata synchronization;
- every individual commit in a larger coherent change.

Repository policy overrides these defaults.

## Evidence procedure

1. **Identify the applicable policy and target entry.**
   Determine the changelog file, expected language, ordering, categories, date style,
   and release/milestone structure from repository authority.

2. **Define the coherent change.**
   Identify the single user-visible, release-relevant, or milestone-relevant topic
   being documented. Do not split one coherent change into unnecessary duplicate entries.

3. **Inspect relevant evidence only.**
   Use the smallest sufficient evidence set, such as:
   - current working-tree differences;
   - file-specific Git history;
   - verified commits;
   - verified pull requests;
   - accepted decision records;
   - accepted requirement or baseline changes;
   - explicit user-confirmed project changes.

   Avoid scanning unrelated repository history.

4. **Distinguish work state from evidence state.**
   Uncommitted work, a draft document, or a proposed requirement is not proof that a
   change was merged, released, deployed, or accepted.

5. **Verify references before recording them.**
   Use commit hashes or pull-request references only when they are actually verified.
   Never fabricate, guess, reuse example references, or copy references from another project.

6. **Handle dates conservatively.**
   Preserve historical dates already supported by evidence.
   Do not replace an unknown historical date with the current date merely because the
   entry is being edited now.

   If the repository requires a date and the relevant historical date cannot be verified,
   report the uncertainty or use the repository's approved unknown-date convention.

7. **Update rather than duplicate.**
   If an existing entry already represents the same coherent work, update that entry
   instead of creating a second version of the same history.

8. **Write in the adopted format.**
   Match the repository's category names, ordering, heading hierarchy, terminology,
   language, and release/milestone conventions.

9. **Check consistency.**
   Verify:
   - chronological order;
   - category placement;
   - evidence support;
   - release/milestone naming;
   - absence of invented PR/commit/release data;
   - absence of duplicate entries;
   - consistency with the current source of truth for the changed concern.

10. **Report unresolved facts instead of inventing them.**
    If a required date, release, PR, commit, or acceptance state cannot be verified,
    surface it explicitly.

## Requirement and baseline changes

Requirement lifecycle changes are not automatically changelog entries.

For example:

```text
FR-19 ACTIVE -> OUT_OF_SCOPE
```

may be worth recording when it materially changes the current release or accepted
project scope, but a routine status cleanup with no milestone significance may not be.

A broad baseline migration, retirement of a major capability, or coordinated change
across requirements, use cases, tests, or architecture is usually a stronger changelog
candidate when the repository tracks such changes.

Do not use the changelog as the historical baseline itself. Historical baselines and
archived documentation remain separate evidence.

## Changelog vs other artifacts

Use each artifact for its own concern:

```text
SRS / requirements      -> what the system is required to do
Decision Record / ADR   -> why an important decision was made
Architecture            -> how the system is structured
Git history             -> commit-level implementation history
CHANGELOG               -> meaningful changes by release or milestone
```

Do not copy entire requirements, ADR rationale, or Git commit logs into the changelog.
Reference the authoritative artifact when more detail is needed.

## Historical conversion

When normalizing an existing changelog:

- preserve meaningful historical information;
- preserve evidenced dates;
- preserve verified references;
- do not rewrite old entries to match current project behavior;
- do not treat example blocks as project history;
- do not silently erase decisions merely because the current project has changed;
- distinguish historical state from the current source of truth.

Editorial normalization is allowed when meaning remains unchanged.
Semantic rewriting of historical entries requires explicit authorization.

## Review checklist

Before finishing a changelog task, confirm that:

- the repository's actual changelog policy was followed;
- the change is appropriate for the changelog at the requested scope;
- evidence supports the claims made;
- uncommitted, merged, released, and deployed states were not conflated;
- dates and references were not invented;
- duplicate entries were avoided;
- historical entries were not rewritten semantically without authorization;
- document metadata was delegated to the applicable metadata procedure;
- no commit, tag, PR, merge, or release action was performed without authorization.

## Limits

Apply the user-requested scope and repository governance.

Do not:

- invent project facts;
- fabricate dates, release numbers, commit hashes, or pull-request IDs;
- infer deployment or release merely from source changes;
- create a second changelog format when the repository already has one;
- use changelog entries as a substitute for requirements or decision records;
- execute release actions merely because the changelog describes a release;
- expose secrets or unrelated sensitive repository content while gathering evidence.
