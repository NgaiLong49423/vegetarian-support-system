> **Document:** Agent Instructions  
> **File:** `AGENTS.md`  
> **Version:** v3.0.0  
> **Created:** 2026-06-29  
> **Last Updated:** 2026-09-13  
> **Status:** Active  

# Agent Entry Point

## Start here

This file is the portable entry point for any new coding agent. If your client does not load AGENTS.md automatically, ask it to read this file before working. No knowledge of previous conversations is required.

## Contribution guidelines

Before making repository changes, agents must read and follow the contribution guidelines defined in [CONTRIBUTING.md](CONTRIBUTING.md). CONTRIBUTING.md is the source of truth for the repository contribution workflow, including changelog format. AGENTS.md defines only additional instructions specific to AI agents.

On later tasks in the same session, reread the relevant contribution sections if the file changed or the task needs a rule not yet loaded. If a local skill gives a different contribution rule or changelog format, follow CONTRIBUTING.md and report the conflict.

## Reading procedure

1. Identify the user request and inspect `git status --short` before edits.
2. Choose the relevant row below. Read the target file and the necessary sections of its sources.
3. Before creating/moving a file, consult `docs/README.md` for placement and registration.
4. Read applicable nested AGENTS.md instructions along the target path.
5. Load only the selected skill and the references needed for this task.
6. Make the change, verify it, and give the result in the conversation.

Do not recursively read the repository, all documentation, all skills, old logs or history as an onboarding step. Use scoped filename/heading/identifier searches first, then expand only when a dependency, conflict or test requires it. An explicit full audit can inspect the maintained document register, but does not authorize reading every unregistered artifact.

## Task-to-source routing

| Task | Read first | Expand only when relevant |
|---|---|---|
| Understand the product | README.md; PRD sections in docs/requirements/PRD.md | SRS headings for the requested capability |
| Implement/change business behavior | Relevant FR/BR/module in docs/requirements/SRS.md; target code/tests | Related SRS sections and supporting decomposition named by the document register |
| Frontend | app/frontend/README.md; target feature | Relevant SRS and backend API contract; technology baseline for dependency decisions |
| Backend | app/backend/README.md; target code/tests | Relevant SRS, API contract, database guide |
| Database | database/README.md; affected migrations/model | Relevant SRS and docs/diagrams/ERD/; empty SQL files are not an approved schema |
| Technologies/integration | docs/decisions/SWP-Technology-Stack-v2.0.0.txt | Relevant SRS; do not turn provider selection into an unapproved model/architecture |
| Git, review, release or teamwork | CONTRIBUTING.md | ADR-001 for branch/release rationale; ADR-002 for team responsibilities |
| Progress, owner, deadline or blocker | GitHub Issues and Projects, then linked PRs | CONTRIBUTING.md for status semantics; no local progress reports |
| Documentation/file placement | docs/README.md; target document | Only the listed authority and affected links |
| Agent routing/configuration | AGENTS.md; .agents/repo-contract.yml | Selected skill or document-register entry |

Full paths for ADRs and supporting documents are registered in docs/README.md. The YAML contract is a machine-readable routing summary; ordinary tasks do not need to read it in addition to this file.

## Skill selection

Skills live at `.agents/skills/<folder>/SKILL.md`. Folder names and frontmatter names may differ; use these exact paths. A skill is a procedure, not a source of product requirements or permission to take external actions.

| When needed | Skill folder | Boundary |
|---|---|---|
| Write or restructure Markdown content | markdown-documentation-skill | Load relevant maintenance/standardization references, not all references |
| Add/audit metadata or decide document versions | document-metadata-standardizer | Target maintained registered documents; preserve creation evidence and draft status |
| Cross-document consistency audit | repo-template-doc-sync-auditor | Use this project's register, not its example template tree |
| Decompose requirements or prepare/create Issues | srs-to-github-issues | Load selected source requirements, labels and necessary templates; live creation requires user authorization |
| Maintain changelog or prepare release notes | changelog-automatic | Read CONTRIBUTING.md#changelog-format first; the skill implements that format |

Use the smallest set that fits the request. Do not load all five skills for every task. When a skill is unavailable, report it and apply the relevant repository rule directly; do not invent its contents.

**Project overrides for every local skill:** the user request, CONTRIBUTING.md for shared contribution rules, and this file for agent-specific behavior take precedence over generic skill examples. No automatic saved audit report, log, summary or progress file, regardless of changed-file count. Return findings in the conversation by default. Read/write scratch artifacts only for an explicitly requested deliverable, under `.agents/outputs/<task>/`; these are not maintained project documentation. Metadata audits use the maintained register; SKILL.md retains YAML frontmatter. Do not add metadata to scratch files or scan them to discover supposed project requirements.

## Context and authority boundaries

- Only documents registered in docs/README.md are maintained project documentation. A new file in a declared folder is not automatically authoritative.
- Ignore unregistered documents, ZIPs, scratch, outputs, generated/build folders and unrelated untracked files by default. Reading is allowed if the user names them or a concrete task dependency requires them; explain their evidence role without promoting them into the register.
- This reading rule is not a blanket .gitignore rule: relevant new source files, configs and tests remain reviewable. Never infer that an unregistered file is safe to delete.
- Inspect target files and applicable configuration even when not individually listed in the document register. Do not skip security/behavior checks to save tokens.
- SRS owns detailed business behavior; PRD is the summary. Supporting notes and research cannot override SRS. Surface material contradictions before changing business meaning.

## Agent-specific execution and handoff

- Communicate with the user in Vietnamese. Consult CONTRIBUTING.md for document language and shared editing/verification rules.
- Repository access does not authorize an agent to commit, push, merge, tag, enable automation or change GitHub settings. Perform these actions only when the user requests them.
- Do not infer runnable commands or completed features from plans, empty workspaces or a draft document.
- Inspect the actual tool/test result before reporting success; explain unavailable verification without claiming it passed.
- When writing changelog entries, use the evidence procedure in the selected changelog skill. Do not infer PR numbers, commit status or dates from file names.
- Finish with the changes, verification and unresolved questions in the conversation; do not create an additional report file.
