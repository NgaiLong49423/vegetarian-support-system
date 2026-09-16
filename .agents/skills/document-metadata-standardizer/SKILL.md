---
name: document-metadata-standardizer
description: 'Use this skill when the user asks to add, update, audit, or standardize metadata at the top of Markdown documentation files, including document version, created date, last updated date, status, and version bump decisions after document edits.'
metadata:
  swp391-risk: "low"
  swp391-source: "self"
  swp391-version: "v1.2.1"
  swp391-created-date: "2026-06-29"
  swp391-last-updated-date: "2026-09-16"
---

# Document Metadata Standardizer

Standardize metadata for repository documentation files.

This skill owns **document metadata and document-version mechanics only**. It does not decide whether a document should exist, whether a requirement is correct, or whether a project decision should change. Those concerns belong to the repository's documentation/lifecycle/governance rules.

## When to Use

Use this skill for:

- Adding or standardizing metadata in Markdown documentation.
- Auditing documentation for missing or invalid metadata.
- Updating metadata after an intentional document edit.
- Recommending document-version bumps based on the impact of that edit.
- Adding standardized metadata when a new Markdown documentation file has already been created or approved.
- Preparing metadata before documentation audit, issue decomposition, merge, or release when the repository requires it.

Do **not** use this skill to decide that a new document should be created. Follow the repository's document-lifecycle/governance rules first.

Typical target files include:

```text
README.md
CONTRIBUTING.md
AGENTS.md
docs/**/*.md
database/**/*.md
.github/**/*.md
```

Repository conventions may expand or narrow this set.

Do not apply the Markdown quote-block metadata format to agent-skill package internals or generated outputs by default.

Ignore by default:

```text
.git/ node_modules/ target/ build/ dist/ out/
.agents/skills/**
.agents/**/SKILL.md
.agents/**/references/**
.agents/**/assets/**
.agents/**/evals/**
.agents/outputs/**
*.class *.jar *.zip *.rar *.png *.jpg *.pdf
*.sql *.java *.xml *.yml *.yaml
```

`SKILL.md` files use their own YAML frontmatter format. Do not insert the generic Markdown metadata block above YAML frontmatter.

## Responsibility Boundary

When another documentation skill or repository rule is available:

- **Document lifecycle / content / traceability** decides what the document means, whether it should exist, and whether content changes are authorized.
- **This skill** decides how document metadata should represent the approved/current document state.
- **Changelog tooling** owns changelog-entry mechanics when a dedicated changelog skill exists.

If the content/lifecycle skill reports that a change is semantic, baseline-affecting, archived, or newly approved, use that information as evidence for metadata/version decisions. Do not independently reinterpret the underlying requirement or project decision.

## Reference Router

Read references only when the current metadata task needs them:

- For concrete metadata block examples, read `references/metadata-block-examples.md`.
- For deciding or reviewing a document-version bump, read `references/version-bump-rules.md`.
- For producing a structured metadata audit report, read `references/metadata-audit-report-template.md`.

Do not load all reference files for a simple metadata edit.

## Required Metadata

For repository documents that adopt this metadata convention, use this block before the first heading:

```markdown
> **Document:** Document Name  
> **File:** `relative/path/to/file.md`  
> **Version:** v1.0.0  
> **Created:** YYYY-MM-DD  
> **Last Updated:** YYYY-MM-DD  
> **Status:** Active  
```

Required fields:

| Field | Rule |
|---|---|
| Document | Human-readable name; no unresolved template placeholders |
| File | Repository-relative path only; never `D:\...` or `file:///...` |
| Version | Document version in the repository's adopted format; default fallback is `vMAJOR.MINOR.PATCH` |
| Created | Actual creation date when supported by evidence; never fabricate it |
| Last Updated | Latest intentional document edit date |
| Status | Document-level status defined by repository policy; fallback values are `Draft`, `Under Review`, `Active`, `Deprecated`, `Archived`, `Template` |

Optional fields may be added only when useful and supported by project evidence:

```markdown
> **Owner:** Team/Person  
> **Related Docs:** `README.md`, `CHANGELOG.md`  
> **Project Version:** v0.2.0  
> **Baseline:** Project Baseline v0.2.0  
```

Do not invent `Owner`, `Project Version`, or `Baseline`.

## Document Status Is Not Requirement Lifecycle Status

Do not confuse document-level metadata with lifecycle status inside the document.

Example:

```text
SRS.md
Document Status: Active

FR-19 Status: OUT_OF_SCOPE
FR-23 Status: DEFERRED
```

This is valid. The SRS document can be an active/current source while individual requirements have their own lifecycle states.

Requirement lifecycle states such as `DRAFT`, `ACTIVE`, `DEFERRED`, `OUT_OF_SCOPE`, and `RETIRED` are **not** values for the document `Status` field unless the repository explicitly defines otherwise.

## Version Scope Rules

Always distinguish:

- **Document Version**: version of one file.
- **Project Version**: version of the software/project.
- **Baseline Version**: version or identifier of a coordinated set of approved artifacts.

Different documents may have different document versions.

Valid:

```text
PRD.md: v1.0.0
SRS.md: v1.0.2
```

Do not report different document versions as an issue unless the repository requires synchronized versions or the documents conflict on the same version scope.

## Repository Version Policy First

Use the repository's documented metadata/version policy when one exists.

Do not assume universally that:

- `v0.x.x` always means draft;
- `v1.0.0` always means first approval;
- every requirement removal is automatically MAJOR;
- every database or architecture change is automatically MAJOR.

If the repository explicitly adopts those conventions, enforce them. Otherwise use the fallback impact-based rules below and clearly label the result as a recommendation.

## Fallback Draft Convention

When the repository **does** use `v0.x.x` to represent pre-stable documents:

- Prefer `Draft` or `Under Review` document status.
- Do not automatically promote to `v1.0.0`.
- Promotion requires approval according to repository governance.
- A `v0.x.x` document marked `Active` is a warning only when that combination conflicts with the repository's adopted version policy.

Do not infer stability from a version number when no such convention exists.

## Impact-Based Version Bump Rules

Version bumps describe the impact of a document change, not merely the technical category of the underlying project change.

Read `references/version-bump-rules.md` when the bump is ambiguous or material.

### No Bump

Use no bump when the document content did not intentionally change, for example:

- read/review only;
- Git metadata only;
- archived baseline left untouched;
- generated artifact where manual document versioning does not apply.

### PATCH

Use PATCH for editorial/non-semantic changes that preserve meaning and contract, such as:

- spelling/grammar fixes;
- formatting cleanup;
- broken-link repair;
- metadata correction;
- terminology normalization that does not change meaning;
- small wording clarification with no semantic change.

### MINOR

Use MINOR for meaningful, compatible document evolution that does not require a new incompatible baseline, such as:

- adding an approved section;
- adding a compatible requirement or scenario;
- adding traceability or examples that change maintained content but do not invalidate the prior document contract;
- a localized lifecycle/status change whose impact does not materially restructure the active baseline.

### MAJOR

Use MAJOR when the **document contract/baseline itself** changes incompatibly or is broadly restructured, for example:

- authorized baseline migration with substantial scope change;
- replacing a canonical document/source-of-truth model;
- broad requirement restructuring that invalidates many references;
- large semantic rewrite where consumers of the previous document can no longer safely rely on its structure or meaning;
- replacement/deprecation of a canonical document by a new authoritative artifact.

A requirement removal, database change, architecture change, or technology change is **not automatically MAJOR**. Evaluate how that change affects this document and the repository's baseline/version policy.

## Date Rules

- Preserve `Created` when it is supported by evidence.
- If `Created` is missing, first check Git/file history, changelog evidence, repository history, or another authoritative record.
- Do **not** substitute the current date merely because the true creation date is unknown.
- If no reliable creation date can be established, report `Created` as unresolved and ask for confirmation when the repository requires the field.
- Use the current intentional edit date for `Last Updated`, unless the user/repository provides another authoritative date.
- Normalize confirmed dates to `YYYY-MM-DD` when this metadata convention is used.
- Ensure a confirmed `Created` date is not after `Last Updated`.

## Archive and Baseline Rules

Historical storage follows repository convention.

When a document is an archived historical baseline:

- represent it as `Archived` if that matches repository policy;
- do not rewrite it to match current documentation;
- do not treat it as the current source of truth;
- do not bump its version merely because the active document changed.

If a new active baseline is approved, metadata/version changes must reflect the approved baseline decision; this skill must not create or approve the baseline by itself.

## Audit Workflow

1. Identify the target documentation files and the repository's metadata/version policy.
2. Exclude skill-package internals, generated outputs, archives, or other paths that the policy does not cover.
3. Check required metadata fields.
4. Validate path, version format, date format, document status, placeholders, and local paths according to the adopted policy.
5. Distinguish document status from requirement lifecycle status.
6. Determine whether the document content actually changed and classify the impact as `No bump`, `PATCH`, `MINOR`, or `MAJOR` using repository policy first, fallback rules second.
7. Report unresolved creation dates or ambiguous version decisions rather than inventing values.
8. Apply only minimal metadata fixes when the user asked to fix.

## Audit Output

When auditing, produce:

```markdown
# Document Metadata Report

## Summary
- Files reviewed:
- Missing metadata:
- Invalid metadata:
- Version updates needed:
- Date issues:
- Policy ambiguities:
- Overall status:

## Findings
| File | Issue | Severity | Recommended Fix |
|---|---|---|---|

## Version Decisions
| File | Current Version | Recommended Version | Reason |
|---|---|---|---|
```

Severity:

| Severity | Use When |
|---|---|
| Critical | Metadata can make an agent/contributor use the wrong source of truth or historical baseline |
| Major | Metadata can materially confuse version/baseline tracking |
| Minor | Small metadata/date/format problem |
| Info | Optional improvement or policy clarification |

## Fix Rules

When fixing metadata:

- Make minimal edits.
- Do not rewrite document content unless explicitly authorized by the documentation/content workflow.
- Do not decide to create, archive, replace, or delete a document on this skill's authority alone.
- Do not change `Created` unless evidence shows the existing value is missing or wrong.
- Never invent `Created` from the current date when the original date is unknown.
- Update `Last Updated` when an intentional edit covered by the repository policy occurs.
- Apply version changes according to repository policy first and impact-based fallback rules second.
- Do not force document versions to match across files.
- Do not invent `Project Version`, `Baseline`, or approval state.
- Do not mark a template/example/archive as `Active` unless repository policy explicitly uses that meaning.
- Use repository-relative paths only.
- Preserve existing language and heading style.
- Keep document status separate from requirement lifecycle status.

## Completion Criteria

The task is complete when:

- metadata required by repository policy is present or unresolved fields are explicitly reported;
- metadata format is valid;
- version decisions are evidence-based and impact-based;
- `Created` is preserved or honestly reported as unknown rather than fabricated;
- `Last Updated` reflects the relevant intentional edit;
- document status is not confused with requirement lifecycle status;
- archived baselines are not rewritten or treated as current sources;
- no document version is forced to match another document without repository policy requiring it.

## Final Response Format

Audit mode:

```text
Metadata audit completed.
Files reviewed: X
Missing metadata: X
Invalid metadata: X
Version decisions: X
Unresolved metadata: X
```

Fix mode:

```text
Metadata standardization completed.
Updated files:
- ...

Version changes:
- ...

Unresolved items:
- ...
```
