> **Document:** Repository Layout and Document Register  
> **File:** `docs/README.md`  
> **Version:** v2.0.0  
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-12  
> **Status:** Active  

# Repository Layout and Document Register

This is the maintained placement policy for all five team members and every agent. Read AGENTS.md for task routing. GitHub Projects manages progress; this repository stores code, requirements, durable decisions and verification assets.

## Where a new file belongs

| Location | Allowed purpose | Examples and boundaries |
|---|---|---|
| Repository root | Entry points and root-level tooling only | README.md, AGENTS.md, CONTRIBUTING.md, CHANGELOG.md, LICENSE, .gitignore; new build/tool configs require a real root-level consumer |
| app/frontend/ | Frontend source, tests, assets and configuration | React component, feature test, package.json after scaffolding; place files by the app's established feature structure |
| app/backend/ | Backend source, tests, configuration and migrations | Java classes/tests, pom.xml, Flyway migrations after scaffolding |
| database/ | Database usage guide, deliberate SQL snapshot, demo seed and diagnostic queries | schema.sql remains empty until designed; Flyway owns executable migration history when implemented |
| docs/requirements/ | Maintained product requirements and supporting decomposition | PRD, SRS, actor/onboarding and AI plan notes; no completed interview worksheets |
| docs/decisions/ | Durable project/workflow/technology decisions | Numbered ADR with context, decision, consequences and unresolved points |
| docs/diagrams/Activity/ | Maintained activity diagram source and exports | Name by feature; link to the relevant SRS identifiers |
| docs/diagrams/UseCase/ | Maintained use-case diagram source and exports | Name by module; do not invent a second requirement source |
| docs/diagrams/ERD/ | Data-model diagram source and exports | Sync with approved model/migrations |
| docs/research/ | Reusable, source-backed findings | Record research date and applicability; research does not approve a requirement |
| .github/ISSUE_TEMPLATE/ | Issue form configuration | Intake forms; not copies of live Issues |
| .github/ | GitHub configuration and PR template | labels.yml, pull_request_template.md; workflows only when authorized |
| .agents/skills/ | Reusable agent procedures and their own references/templates | Each skill lives in one folder with SKILL.md; no product requirements here |
| .agents/repo-contract.yml | Machine-readable routing and maintenance contract | Keep aligned with this register and AGENTS.md |
| .agents/outputs/<task>/ | Explicitly requested temporary deliverables only | Requested draft Issue bodies or audit export; never automatic, never authoritative |
| OS temporary directory | Disposable verification artifacts and local backups | Keep these out of project documentation |

Backend source subdirectories, migration location and frontend feature structure must follow the actual scaffold once it exists; this policy does not create a new application architecture.

Examples: a recipe form belongs in app/frontend/, its API service in app/backend/, an AI quota rule in SRS (with supporting detail in ai-plan-decomposition.md if needed), and a decision about a payment provider in docs/decisions/. Task status belongs on its GitHub Issue/Project item.

## Maintained document register

Only entries below are maintained documentation. Read entries by task, not as a mandatory reading list.

| Path | Function and authority | Read when |
|---|---|---|
| ../AGENTS.md | Portable agent entry point, read scope and skill routing | Starting a new agent session |
| ../README.md | Product overview and implementation state | General onboarding |
| ../CONTRIBUTING.md | Operational Git/Issue/PR/release rules | Contributing or reviewing changes |
| ../CHANGELOG.md | Historical changes, not current progress | Updating notable changes or investigating history |
| README.md | File placement and document registry | Creating/moving files or routing documentation |
| requirements/PRD.md | High-level product summary, Under Review | Product intent and scope |
| requirements/SRS.md | Detailed requirements and confirmed decisions, Draft | Related business work; Q01-Q38 decisions remain in section 3.20 |
| requirements/actors-and-onboarding-draft.md | Supporting actor/profile decomposition | Actor/onboarding changes |
| requirements/product-direction.md | Supporting origin and product direction, Under Review | Need original topic context; SRS wins on current scope |
| requirements/ai-plan-decomposition.md | Supporting AI plan/usage decomposition, Under Review | Quota, AI access, plans and integration details |
| decisions/SWP-Technology-Stack-v2.0.0.txt | Selected technologies and explicit TBDs | Technical choices |
| decisions/001-team-workflow.md | Branch/release decision and rationale | Workflow changes |
| decisions/002-five-member-team-operating-agreement.md | Team responsibilities and coordination | Team process |
| decisions/WORKFLOW-SOURCES.md | Evidence behind workflow conventions | Reconsidering a workflow decision |
| research/similar-products-benchmark.md | Dated external research, non-authoritative | Relevant product comparison |
| diagrams/Activity/README.md | Activity diagram conventions | Creating/updating activity diagrams |
| diagrams/UseCase/README.md | Use-case diagram conventions | Creating/updating use-case diagrams |
| diagrams/ERD/README.md | ERD documentation conventions | Creating/updating the data model |
| ../app/frontend/README.md | Frontend setup/state and contribution guidance | Frontend work |
| ../app/backend/README.md | Backend setup/state and contribution guidance | Backend work |
| ../database/README.md | SQL/Flyway ownership and database state | Database work |
| ../.github/pull_request_template.md | PR evidence and release checklist template | Opening or reviewing a PR |

Paths above are relative to docs/. Skill procedures are registered separately in AGENTS.md and are not onboarding documents. Generated output, scratch files and unknown files are excluded.

## Creating, moving and retiring documents

1. Check whether an existing registered document owns the information. Update that document instead of creating a duplicate.
2. Choose an allowed folder. If none fits, propose the concrete location and purpose before adding a new maintained category; do not use root as a fallback.
3. For a new durable document, add metadata and a register entry with purpose, authority and read trigger in the same change. Use a descriptive kebab-case filename; numbered prefixes are reserved for ordered ADRs.
4. Update incoming relative links and the File metadata when moving a document. Preserve Created and increment its document version appropriately.
5. When a worksheet/draft has fulfilled its purpose, retain confirmed decisions in the authoritative document, remove navigation/Related Docs references and delete only with user authorization. Historical changelog entries may mention retired paths without making them reading targets.
6. An unregistered file is ignored for documentation discovery, not automatically deleted, rewritten, catalogued or metadata-standardized.
7. Return audit results in chat. Do not recreate reports directories because an old skill example mentions them. Explicitly requested exports go under .agents/outputs/<task>/ and never enter this register.

## Progress and evidence

GitHub Issues/Projects own assignments, estimates, dates, status and blockers. Linked PRs own review and verification evidence. No per-member reports, weekly report files or parallel local progress tracker are required. Long-lived technical decisions extracted from discussion belong in ADRs; progress snapshots do not.
