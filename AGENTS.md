> **Document:** Agent Instructions  
> **File:** `AGENTS.md`  
> **Version:** v3.2.1
> **Created:** 2026-06-29  
> **Last Updated:** 2026-09-14
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
3. Before creating or moving maintained project documentation, consult `docs/README.md` for placement and registration.
4. Read applicable nested AGENTS.md instructions along the target path.
5. Load only the selected skill and the references needed for this task.
6. Make the change, verify it, and give the result in the conversation.

Do not recursively read the repository, all documentation, all skills, old logs or history as an onboarding step. Use scoped filename, heading or identifier searches first, then expand only when a dependency, conflict or test requires it. An explicit full audit can inspect the maintained document register, but does not authorize reading every unregistered artifact.

## Task-to-source routing

| Task | Read first | Expand only when relevant |
|---|---|---|
| Understand the product | README.md; PRD sections in docs/requirements/PRD.md | SRS headings for the requested capability |
| Implement/change business behavior | Relevant FR/BR/module in docs/requirements/SRS.md; target code/tests | Related SRS sections and supporting decomposition named by the document register |
| Frontend | app/frontend/README.md; target feature | Relevant SRS and backend API contract; technology baseline for dependency decisions |
| Backend | app/backend/README.md; target code/tests | Relevant SRS, API contract, database guide |
| Database | database/README.md; affected migrations/model | Relevant SRS and docs/diagrams/ERD/; empty SQL files are not an approved schema |
| Technologies/integration | Technology-stack or technology-baseline document registered in docs/README.md | Relevant SRS; do not turn provider selection into an unapproved model/architecture |
| Architecture or trust boundaries | docs/architecture/ARCHITECTURE.md | Relevant SRS and technology stack; do not invent packages, endpoints, tables, deployment topology or AI architecture |
| Testing or verification strategy | docs/testing/TEST-STRATEGY.md; relevant SRS requirements | Target code/tests and CONTRIBUTING.md completion/release rules; do not invent tests, commands or coverage thresholds |
| Development, setup or API guide | docs/README.md creation triggers; actual scaffold/contract evidence | Create a maintained guide only in an authorized documentation task after its trigger is satisfied |
| Git, review, release or teamwork | CONTRIBUTING.md | ADR-001 for branch/release rationale; ADR-002 for team responsibilities |
| Progress, owner, deadline or blocker | GitHub Issues and Projects, then linked PRs | CONTRIBUTING.md for status semantics; no local progress reports |
| Documentation/file placement | docs/README.md; target document | Only the listed authority and affected links |
| Agent routing/configuration | AGENTS.md; .agents/repo-contract.yml when needed | Selected skill or document-register entry |

Full paths for ADRs and supporting documents are registered in docs/README.md. The YAML contract is a machine-readable routing summary. Do not load it routinely when AGENTS.md already contains everything needed for the task. Read it when the selected skill or current task requires contract information not already available here.

## Skill selection

Skills live at `.agents/skills/<name>/SKILL.md`. For maintained local skills, the skill folder name and the `name` field in SKILL.md must match. Use the exact skill names registered below. A skill is a procedure, not a source of product requirements or permission to take external actions.

| When needed | Skill folder | Boundary |
|---|---|---|
| Write, review or restructure Markdown documentation | markdown-documentation | Owns shared documentation semantics, lifecycle, traceability and source-of-truth rules; load only the references needed for the task |
| Add/audit metadata or decide document versions | document-metadata-standardizer | Owns document metadata and versioning only; target maintained registered documents and preserve creation evidence |
| Cross-document consistency audit | repo-template-doc-sync-auditor | Use this project's maintained register and adopted contract; ignore skill packages and generated outputs by default |
| Decompose requirements or maintain/synchronize GitHub Issues | srs-to-github-issues | SRS owns requirement meaning and lifecycle; the skill owns requirement-to-Issue mapping, synchronization and Issue lifecycle actions |
| Maintain changelog or prepare release notes | changelog-automatic | Read CONTRIBUTING.md#changelog-format first; use verified evidence and do not infer release/PR/commit facts |

Use the smallest set that fits the request. Do not load all five skills for every task. When a specialized skill applies, let it own its specialized mechanics while `markdown-documentation` supplies shared documentation semantics. When a skill is unavailable, report it and apply the relevant repository rule directly; do not invent its contents.

### Requirement-to-Issue synchronization

All requirements in the maintained SRS are in Issue-management scope. The agent must not omit a requirement because it looks low priority, difficult, optional or inconvenient.

Before the first live Issue synchronization for a requirement, its lifecycle state must be explicitly confirmed by an authorized decision-maker using the repository vocabulary:

`DRAFT` / `ACTIVE` / `DEFERRED` / `OUT_OF_SCOPE` / `RETIRED`

The agent must never infer a lifecycle state from wording, code status, implementation difficulty, priority or personal judgment. If a requirement has no confirmed lifecycle state, stop Issue mutation for that requirement and ask for the state to be confirmed.

Once lifecycle is confirmed, use `srs-to-github-issues` to create, update, defer, reopen, close or otherwise reconcile the linked Issue as required by that lifecycle and the current SRS. Preserve stable requirement IDs and existing Issue history. Do not create duplicate implementation scope for both a parent FR and its child FRs.

If a linked Issue is already completed and the SRS later changes semantically, preserve the completed Issue as history. Create follow-up work when additional implementation is required instead of rewriting the completed Issue as if the new requirement had always existed.

Live GitHub mutations require authorization for the current task. Authorization may cover the full reconciliation operation; it does not need to be requested separately for every individual Issue action. If live mutation is not authorized, prepare the required synchronization and ask before applying it.

## Project overrides and generated artifacts

**Project overrides for every local skill:** the user request within the user's authorized scope, CONTRIBUTING.md for shared contribution rules, and this file for agent-specific behavior take precedence over generic skill examples. Repository governance and adopted project contracts define project-specific authority; a skill must not override them.

No automatic saved audit report, log, summary or progress file, regardless of changed-file count. Return findings in the conversation by default.

Maintained project documentation follows the document lifecycle and registration rules in `docs/README.md`. Scratch and generated working artifacts belong under `.agents/outputs/`, using either the selected skill's declared structure or a task-specific subdirectory. These outputs are not maintained project documentation unless an authorized decision explicitly promotes and registers them.

Metadata audits use the maintained register. `SKILL.md` retains YAML frontmatter. Do not add project-document metadata to skill packages, scratch files or generated outputs. Do not scan `.agents/skills/**` or `.agents/outputs/**` to discover supposed project requirements unless the task explicitly targets those locations.

## Context and authority boundaries

- Only documents registered in docs/README.md are maintained project documentation. A new file in a declared folder is not automatically authoritative.
- Ignore unregistered documents, ZIPs, scratch, outputs, generated/build folders and unrelated untracked files by default. Reading is allowed if the user names them or a concrete task dependency requires them; explain their evidence role without promoting them into the register.
- This reading rule is not a blanket .gitignore rule: relevant new source files, configs and tests remain reviewable. Never infer that an unregistered file is safe to delete.
- Inspect target files and applicable configuration even when not individually listed in the document register. Do not skip security or behavior checks to save tokens.
- SRS owns detailed business behavior and requirement lifecycle; PRD is the summary. Supporting notes and research cannot override SRS. Surface material contradictions before changing business meaning.
- GitHub Issues own implementation work tracking, progress and execution state. An Issue must not redefine the meaning of its source requirement.
- When an SRS requirement changes semantically or changes lifecycle, reconcile its linked Issue through `srs-to-github-issues`.
- Historical baselines and completed Issues are evidence of prior state. Do not rewrite them merely to match current requirements.

## Agent-specific execution and handoff

- Communicate with the user in Vietnamese. Follow the [Documentation Language Policy](docs/README.md#documentation-language-policy); use CONTRIBUTING.md for shared editing and verification rules.
- Repository access does not authorize an agent to commit, push, merge, tag, enable automation, mutate GitHub Issues/Projects or change GitHub settings. Perform external mutations only when the user authorizes them for the current task.
- Do not infer runnable commands or completed features from plans, empty workspaces or a draft document.
- Inspect the actual tool/test result before reporting success; explain unavailable verification without claiming it passed.
- When writing changelog entries, use the evidence procedure in the selected changelog skill. Do not infer PR numbers, commit status or dates from file names.
- Finish with the changes, verification and unresolved questions in the conversation; do not create an additional report file.
