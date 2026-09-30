> **Document:** Agent Instructions
> **File:** `AGENTS.md`
> **Version:** v3.14.0
> **Created:** 2026-06-29
> **Last Updated:** 2026-09-30
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
| Implement/change business behavior | Registered SRS root plus applicable authoritative FR/BR/NFR documents; target code/tests | Related SRS sections and supporting decomposition named by the document register |
| Frontend | app/mamxanh-frontend/README.md; target feature | Relevant SRS and backend API contract; technology baseline for dependency decisions |
| Backend | app/mamxanh-backend/README.md; docs/architecture/BACKEND-PACKAGE-STRUCTURE-PROPOSAL.md (mandatory package structure); target code/tests | Relevant SRS, API contract, database guide |
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
| Frontend implementation/review/validation for Mâm Xanh | mamxanh-frontend-development | Project-local React/Vite/Tailwind guidance; preserves existing UI identity and does not replace `implement-fr-issue` |
| Backend implementation/review/validation for Mâm Xanh | mamxanh-backend-development | Project-local Spring/Maven/JPA/Flyway/API guidance; preserves the modular monolith and does not replace `implement-fr-issue` |
| Write, review or restructure Markdown documentation | markdown-documentation | Owns shared documentation semantics, authority, traceability and source-of-truth rules; load only the references needed for the task |
| Add/audit metadata or decide document versions | document-metadata-standardizer | Owns document metadata and versioning only; target maintained registered documents and preserve creation evidence |
| Cross-document consistency audit | repo-template-doc-sync-auditor | Use this project's maintained register and adopted contract; ignore skill packages and generated outputs by default |
| Decompose requirements or maintain/synchronize GitHub Issues | srs-to-github-issues | Current SRS presence defines implementation scope; the skill owns stable-ID requirement-to-Issue mapping, synchronization and Issue execution-state actions |
| Maintain changelog or prepare release notes | changelog-automatic | Read CONTRIBUTING.md#changelog-format first; use verified evidence and do not infer release/PR/commit facts |
| Record a credible bug discovered during repository work, or conduct a user-requested bug audit | bug-recording | Records evidence without expanding task scope; read-only instructions override file writes, and GitHub Issue creation requires explicit current-task authorization |

Use the smallest set that fits the request. Do not load all registered skills for every task. When a specialized skill applies, let it own its specialized mechanics while `markdown-documentation` supplies shared documentation semantics. When a skill is unavailable, report it and apply the relevant repository rule directly; do not invent its contents.

### Cross-cutting bug recording

Các bản ghi `.agents/outputs/bugs/BUG-xxx.md` phải dùng tiếng Việt cho tiêu đề, heading và phần giải thích để thành viên nhóm dễ đọc. Tiêu đề và mô tả trong metadata index cũng dùng tiếng Việt; giữ nguyên ID, YAML key, path, mã trạng thái (`RECORDED`, `TRACKING`, `RESOLVED`) và identifier kỹ thuật. Việc dịch không thay đổi bằng chứng, ngày phát hiện hoặc tự đánh dấu lỗi đã được sửa.

During any repository task, remain alert for credible evidence that expected behavior differs from actual behavior or that another meaningful project defect exists. Do not proactively scan for bugs unless the user explicitly requests a bug audit. When a credible bug is encountered incidentally, use `bug-recording`, create one file per bug under `.agents/outputs/bugs/`, update `bugs-metadata.yaml`, report it, and continue the active task only when safe.

An explicit `read-only`, `plan-only`, `review only`, `do not modify files`, or equivalent instruction takes precedence: report the finding in the conversation and do not update bug outputs. If an equivalent record already exists, do not allocate a new ID; report `Matches existing BUG-xxx` and add new evidence only when local writes are allowed.

A bug record is not a GitHub Bug Issue. Never create an Issue automatically. Creation requires an explicit current-task instruction from the user and must use the current `.github/ISSUE_TEMPLATE/bug_report.yml` plus `CONTRIBUTING.md`. Recording a bug does not authorize fixing it, adding a regression test, assigning an owner, setting priority, committing, pushing, or otherwise expanding the current task.

For browser-visible verification, distinguish exploratory browser control from persistent Playwright tests in `app/mamxanh-frontend/tests/e2e/`. Do not call a frontend-only or mock-backed smoke test full end-to-end evidence for Backend or database behavior.

### Current requirement-to-Issue synchronization

Requirements present in the current SRS registry belong to the current implementation scope and Issue management. Do not omit a current requirement because it looks low priority, difficult, optional or inconvenient. Historical requirements belong in the frozen archive and are not current implementation scope.

Use `srs-to-github-issues` to map the current registry to Issues by stable ID. When a requirement is removed from the current baseline, inspect its linked Issue and implementation evidence: preserve completed work as history; prepare a close-as-not-planned candidate for unfinished work when the approved scope decision supports it. Do not infer completion, delete history, or create duplicate implementation scope for both a parent FR and child FRs.

If a linked Issue is already completed and the SRS later changes semantically, preserve the completed Issue as history. Create follow-up work when additional implementation is required instead of rewriting the completed Issue as if the new requirement had always existed.

Live GitHub mutations require authorization for the current task. Authorization may cover the full reconciliation operation; it does not need to be requested separately for every individual Issue action. If live mutation is not authorized, prepare the required synchronization and ask before applying it.

## Workflow selection

| Situation | Workflow |
|---|---|
| Triển khai FR từ GitHub Issue đã được giao | `implement-fr-issue.md` |
| Requirements còn mơ hồ cần chốt | `requirement-finalization.md` |
| Requirement semantics or current-scope membership changed | `requirement-change-reconciliation.md` |
| Audit toàn docs rồi sửa finding rõ ràng | `documentation-audit-and-fix.md` |
| Important multi-artifact output needs bounded quality loop | `evaluator-optimizer.md` |
| Validate agent assets/runtime behavior | `acceptance-evaluation.md` |

## Project overrides and generated artifacts

**Project overrides for every local skill:** the user request within the user's authorized scope, CONTRIBUTING.md for shared contribution rules, and this file for agent-specific behavior take precedence over generic skill examples. Repository governance and adopted project contracts define project-specific authority; a skill must not override them.

No automatic saved audit report, log, summary or progress file, regardless of changed-file count. Return findings in the conversation by default.

Maintained project documentation follows the document lifecycle and registration rules in `docs/README.md`. Scratch and generated working artifacts belong under `.agents/outputs/`, using either the selected skill's declared structure or a task-specific subdirectory. These outputs are not maintained project documentation unless an authorized decision explicitly promotes and registers them. The tracked `.agents/outputs/bugs/` subtree is the approved exception for cross-task bug records and their metadata index; it remains agent output rather than product or requirement authority.

Metadata audits use the maintained register. `SKILL.md` retains YAML frontmatter. Do not add project-document metadata to skill packages, scratch files or generated outputs. Do not scan `.agents/skills/**` or `.agents/outputs/**` to discover supposed project requirements unless the task explicitly targets those locations.

### Diagram artifact protection

`docs/diagrams/` is a human-maintained presentation workspace. Agents may read its contents when they are relevant, but must not create, edit, rename, delete, regenerate, export, or otherwise modify any file in that subtree by default. A write is allowed only when the user gives explicit authorization in the current task that names the diagram work and affected artifact(s); a database, documentation, or synchronization task alone is not sufficient authorization.

Engineering Autonomy does not override protected artifact boundaries. `docs/diagrams/` remains read-only for agents unless the current user request explicitly authorizes diagram work and identifies the affected artifact(s). Local schema autonomy, migration changes, or implementation tasks never grant permission to edit or regenerate diagram files.

## Environment Configuration & Secret Management Policy

Agents MUST classify configuration before deciding whether to externalize it:

- **Secret configuration** (for example passwords, API keys, access or refresh tokens, JWT signing secrets, OAuth client secrets, cloud-storage keys, private keys, and service credentials) MUST use environment variables plus an approved, provider-agnostic secret-management mechanism. Never hard-code or commit real secret values.
- **Environment-dependent configuration** that is not secret but legitimately changes between environments (for example database/service URLs, frontend/backend origins, deployment ports, or external endpoints) SHOULD use environment variables.
- **Stable application configuration** that is not secret and has no evidence of varying between environments MUST remain directly in application configuration. Agents MUST NOT externalize configuration merely for the sake of externalizing it; `spring.application.name` and `spring.jpa.open-in-view` are examples unless repository evidence establishes environment-specific behavior.

### Secret disclosure and rotation reporting

Real secret values MUST NOT appear in source code, tracked configuration, `.env.example`, documentation, test fixtures, scripts, comments, or agent reports. If repository evidence indicates that a real credential has been committed or otherwise exposed through tracked history, report `ROTATION REQUIRED` with the credential type, affected file/location, and reason for rotation without reproducing its value. Do not automatically revoke or rotate credentials, rewrite Git history, force-push, or purge history; each requires separate explicit authorization.

### `.env` and environment-variable contracts

`.env` and `.env.*` files containing real local values or secrets MUST remain untracked and ignored. `.env.example` may be tracked only as the developer environment-variable contract and MUST contain no real credential. Each component-local `.env.example` MUST contain every variable name from its matching `.env`; when a variable is added, removed, or renamed in `.env`, update the matching `.env.example` in the same change with a safe placeholder and any necessary usage guidance, never the real value. Before changing `.gitignore`, inspect existing `.env*` files and tracked templates so intentional safe templates remain available. Do not create multiple example files with the same purpose without repository evidence.

### Configuration placement and ownership

Place configuration with the component that consumes it. Repository-root environment files are reserved for genuine repository-wide tooling; they MUST NOT become a catch-all for application runtime values. Backend runtime configuration and secrets belong under `app/mamxanh-backend/`, while frontend environment files belong under `app/mamxanh-frontend/` and may contain only client-safe public values. Before adding or moving a variable, identify its runtime consumer and place it in that component's configuration scope.

### Trackable configuration files

A configuration file is not automatically local or untrackable merely because part of its runtime value differs by environment. Agents MUST keep a configuration file trackable when it can safely contain stable non-secret settings and environment-variable placeholders for secret or environment-dependent values. Only real secret and local runtime values belong outside Git. When configuration work is authorized, replace such values with placeholders supported by the established runtime mechanism before tracking the file; do not replace stable non-secret settings with placeholders without evidence. Never unignore or commit an existing local configuration file before inspecting it for real values and sanitizing it where needed.

### Spring Boot runtime environment loading

Agents MUST NOT assume that Spring Boot automatically loads `.env`. Before changing runtime configuration loading, inspect the established mechanism, including IntelliJ Run Configurations, operating-system environment variables, Docker or Docker Compose, CI/CD, existing configuration loaders, and Spring profiles. Do not add a dotenv dependency, configuration framework, bootstrap mechanism, or other runtime dependency solely to make `.env` work without repository evidence or explicit user approval.

### Frontend public environment variables

Every client-exposed environment variable is public after frontend build. For Vite, this includes `VITE_*` by default and every additional prefix configured through `envPrefix`. Secrets MUST NOT be exposed through client-exposed variables; backend services perform operations that require secrets.

### Configuration duplication

`.env.example` is an environment-variable contract, not a replacement for meaningful Spring profile configuration. Do not delete `application-local.properties`, `application-local.properties.example`, `application-*.properties`, or similar profile files merely because `.env.example` exists. Inspect their contents first: retain files with meaningful profile behavior, and only propose removal or consolidation when a file merely duplicates environment-variable placeholders without an independent purpose.

This policy does not authorize an unrelated runtime-configuration refactor, application-configuration changes, dependency additions, source-of-truth changes, or edits to protected diagram artifacts.

### Repository-local Codex MCP

- Repository MCP configuration MUST live in `.codex/config.toml` when the MCP is required only for this project.
- Agents MUST NOT modify the global `~/.codex/config.toml` for project-scoped MCP work.
- Agents MUST NOT store API keys or tokens directly in `.codex/config.toml`; use a local ignored `.env` file and the repository launcher at `scripts/codex.ps1`.
- `.env` MUST NOT be committed. `.env.example` MAY be committed only with variable names and safe empty placeholders.
- Agents MUST NOT create persistent Windows User/System environment variables for project secrets unless the user explicitly requests it.
- When the project needs Codex MCP credentials, contributors should start Codex with `./scripts/codex.ps1` so credentials are limited to that process and its child processes.

## Context and authority boundaries

- Only documents registered in docs/README.md are maintained project documentation. A new file in a declared folder is not automatically authoritative.
- Ignore unregistered documents, ZIPs, scratch, outputs, generated/build folders and unrelated untracked files by default. Reading is allowed if the user names them or a concrete task dependency requires them; explain their evidence role without promoting them into the register.
- This reading rule is not a blanket .gitignore rule: relevant new source files, configs and tests remain reviewable. Never infer that an unregistered file is safe to delete.
- In Requirements / Implementation Baseline v2, root SRS (`docs/requirements/SRS.md`) is the authoritative registry for current requirement presence, stable IDs and module allocation. Presence means current implementation scope; absence means the requirement is not in the current baseline. Registered child documents (`docs/requirements/srs/`) own detailed statements, triggers, preconditions, exceptions, domain rules and canonical acceptance criteria. PRD is the high-level summary. Supporting notes and research cannot override SRS. Surface material contradictions before changing business meaning.
- Scoped reading procedure: when implementing or verifying a requirement, read root SRS to verify that its stable ID is present in the current baseline; then open only the specific detailed definitions in `docs/requirements/srs/` relevant to the task (via stable anchor `#fr-xx`) and related BR/NFR anchors rather than loading all child requirement documents.
- GitHub Issues own implementation work tracking, progress and execution state. An Issue must not redefine the meaning of its source requirement.
- When an SRS requirement changes semantically or enters/leaves the current registry, reconcile its linked Issue through `srs-to-github-issues`.
- Historical baselines and completed Issues are evidence of prior state. Do not rewrite them merely to match current requirements.
- **Engineering autonomy for coding agents:** Agents have engineering autonomy for local implementation choices within the assigned Issue: adding required columns or approved baseline foreign keys/constraints to existing tables via append-only Flyway migrations, adding auxiliary dependencies (test/helpers) with active usage, and designing non-breaking REST endpoints/DTOs synchronized in `docs/api/API.md` and `docs/api/openapi.yaml` within the same work item/PR before merge.
- Core technologies follow the `Confirmed` baseline in `docs/architecture/TECHNOLOGY-STACK.md`; agents must not swap them or hard-code technologies/versions not confirmed by authority documents.
- Coding agents **must not** pause, ask for confirmation, or demand a Decision Issue / 3/5 team vote for these local implementation details.
- Coding agents **must** surface a decision requirement only when crossing structural boundaries: new tables/entities, table removal/merge/split, new or changed relationships/cardinality, core technology stack swaps, breaking API changes, or changing SRS business meaning.
- Engineering autonomy does not grant write permission to `docs/diagrams/`. For complete governance details, see [CONTRIBUTING.md#engineering-autonomy-policy](CONTRIBUTING.md#engineering-autonomy-policy).

## Agent-specific execution and handoff

- Communicate with the user in Vietnamese. Follow the [Documentation Language Policy](docs/README.md#documentation-language-policy); internal development documentation uses Vietnamese prose by default, while `CHANGELOG.md` and every Git commit message must use English. Use CONTRIBUTING.md for shared editing and verification rules.
- Repository access does not authorize an agent to commit, push, merge, tag, enable automation, mutate GitHub Issues/Projects or change GitHub settings. Perform external mutations only when the user authorizes them for the current task.
- Do not infer runnable commands or completed features from plans, empty workspaces or a draft document.
- Inspect the actual tool/test result before reporting success; explain unavailable verification without claiming it passed.
- When writing changelog entries, use the evidence procedure in the selected changelog skill. Do not infer PR numbers, commit status or dates from file names.
- Finish with the changes, verification and unresolved questions in the conversation; do not create an additional report file.
