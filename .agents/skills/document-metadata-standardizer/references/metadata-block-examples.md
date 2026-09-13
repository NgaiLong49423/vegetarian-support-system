# Metadata Block Examples

This reference provides examples for repositories that adopt the quote-block document metadata convention.

Use repository policy first. These examples are defaults, not universal schemas.

## General Rules

- Put the metadata block before the first main heading when the repository uses this convention.
- Use repository-relative paths only.
- Do not use local absolute paths such as `D:\...` or `file:///d:/...`.
- Do not force all documents to share the same document version.
- Do not infer that `v0.x.x` means draft unless the repository defines that convention.
- Keep document-level `Status` separate from requirement lifecycle states inside the document.
- Do not fabricate `Created`, `Project Version`, `Baseline`, or ownership values.

## README.md

```markdown
> **Document:** Project README  
> **File:** `README.md`  
> **Version:** v1.0.0  
> **Created:** 2026-06-29  
> **Last Updated:** 2026-06-29  
> **Status:** Active  

# Project Name
```

## SRS.md

```markdown
> **Document:** Software Requirements Specification  
> **File:** `docs/requirements/SRS.md`  
> **Version:** v1.0.0  
> **Created:** 2026-06-29  
> **Last Updated:** 2026-06-29  
> **Status:** Active  

# Software Requirements Specification
```

The document can be `Active` while individual requirements use lifecycle states such as:

```text
FR-19: OUT_OF_SCOPE
FR-23: DEFERRED
```

These are different status scopes.

## Pre-Stable SRS (Only When Repository Uses v0 Draft Convention)

```markdown
> **Document:** Software Requirements Specification  
> **File:** `docs/requirements/SRS.md`  
> **Version:** v0.3.0  
> **Created:** 2026-06-29  
> **Last Updated:** 2026-06-29  
> **Status:** Under Review  

# Software Requirements Specification
```

Do not infer this meaning from `v0.3.0` unless repository policy defines `v0.x.x` as pre-stable.

## PRD.md

```markdown
> **Document:** Product Requirements Document  
> **File:** `docs/requirements/PRD.md`  
> **Version:** v1.0.0  
> **Created:** 2026-06-29  
> **Last Updated:** 2026-06-29  
> **Status:** Active  

# Product Requirements Document
```

PRD and SRS may have different document versions.

## CHANGELOG.md

```markdown
> **Document:** Changelog  
> **File:** `CHANGELOG.md`  
> **Version:** v1.0.0  
> **Created:** 2026-06-29  
> **Last Updated:** 2026-06-29  
> **Status:** Active  

# Changelog
```

The changelog document version is separate from software release versions listed inside the file.

## CONTRIBUTING.md

```markdown
> **Document:** Contribution Guide  
> **File:** `CONTRIBUTING.md`  
> **Version:** v1.0.0  
> **Created:** 2026-06-29  
> **Last Updated:** 2026-06-29  
> **Status:** Active  

# Contributing Guide
```

## AGENTS.md

```markdown
> **Document:** Agent Instructions  
> **File:** `AGENTS.md`  
> **Version:** v1.0.0  
> **Created:** 2026-06-29  
> **Last Updated:** 2026-06-29  
> **Status:** Active  

# Agent Instructions
```

Do not copy full requirements or workflow documents into AGENTS.md merely to make metadata self-contained.

## Database README

```markdown
> **Document:** Database Documentation  
> **File:** `database/README.md`  
> **Version:** v1.0.0  
> **Created:** 2026-06-29  
> **Last Updated:** 2026-06-29  
> **Status:** Active  

# Database Documentation
```

Do not add this Markdown metadata block to `.sql` files unless explicitly requested.

## Skill Document

`SKILL.md` is different. Use its YAML frontmatter rather than the generic quote block:

```markdown
---
name: example-skill
description: ...
version: v1.0.0
---
```

Do not prepend the generic document metadata block above YAML frontmatter.

By default, this skill should not bulk-standardize `.agents/skills/**` internals.

## Template Document

```markdown
> **Document:** README Template  
> **File:** `docs/templates/README_TEMPLATE.md`  
> **Version:** v1.0.0  
> **Created:** 2026-06-29  
> **Last Updated:** 2026-06-29  
> **Status:** Template  

# README Template
```

## Archived Document

```markdown
> **Document:** Software Requirements Specification Archive  
> **File:** `docs/archive/requirements/SRS-v1.0.0.md`  
> **Version:** v1.0.0  
> **Created:** 2026-06-29  
> **Last Updated:** 2026-06-29  
> **Status:** Archived  

# Software Requirements Specification Archive
```

Archived documents are historical evidence, not current source of truth, and should not be rewritten to match active documentation.

## Deprecated Document

```markdown
> **Document:** Legacy Project Requirements  
> **File:** `docs/requirements/legacy-project-requirements.md`  
> **Version:** v1.2.0  
> **Created:** 2026-06-20  
> **Last Updated:** 2026-06-29  
> **Status:** Deprecated  

# Legacy Project Requirements
```

## Missing Created Date

Do not manufacture a date:

```text
Created: current date
```

is **not** a valid fallback when the document existed before today.

Preferred workflow:

1. inspect Git/file/changelog history;
2. use a date only when evidence supports it;
3. otherwise report `Created` as unresolved and request confirmation when the field is required.

## Optional Fields Example

```markdown
> **Document:** Software Requirements Specification  
> **File:** `docs/requirements/SRS.md`  
> **Version:** v1.1.0  
> **Created:** 2026-06-27  
> **Last Updated:** 2026-09-13  
> **Status:** Active  
> **Owner:** Group 6  
> **Related Docs:** `README.md`, `docs/requirements/PRD.md`  
> **Project Version:** v0.2.0  
> **Baseline:** Project Baseline v0.2.0  
```

Only use optional values when they are already confirmed by repository/project evidence.
