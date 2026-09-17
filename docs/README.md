> **Document:** Repository Layout and Document Register  
> **File:** `docs/README.md`  
> **Version:** v3.6.0
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-17
> **Status:** Active  

# Repository Layout and Document Register

This is the maintained placement policy for all five team members and every agent. Read AGENTS.md for task routing. GitHub Projects manages progress; this repository stores code, requirements, durable decisions and verification assets.

## Documentation Language Policy

Phần văn xuôi và nội dung giải thích trong **tài liệu phát triển nội bộ** phải được viết chủ yếu bằng tiếng Việt để cả nhóm có thể đọc, rà soát và duy trì thống nhất.

Hai ngoại lệ bắt buộc phải dùng **tiếng Anh**:

- Toàn bộ nội dung trong `CHANGELOG.md`, gồm heading, `Status`, `Scope`, `Added`, `Changed`, `Fixed` và mô tả từng mục.
- Toàn bộ Git commit message, gồm subject/description, body và footer do người đóng góp tự viết. Conventional Commit `type`, `scope` và các token chuẩn như `BREAKING CHANGE`, `Closes` hoặc `Refs` giữ đúng cú pháp kỹ thuật.

Không trộn câu tiếng Việt vào changelog entry hoặc commit message. Nếu phát hiện changelog entry lịch sử chưa phải tiếng Anh, phải chuẩn hóa entry đó bằng editorial translation, giữ nguyên ngày, trạng thái, ID, version, liên kết và ý nghĩa lịch sử.

Giữ các technical term đã được sử dụng phổ biến bằng tiếng Anh khi dịch sang tiếng Việt làm giảm độ chính xác hoặc gây khó khăn khi đối chiếu với code, công cụ và official documentation. Các ví dụ gồm `Backend`, `Frontend`, `Pull Request`, `Issue`, `Acceptance Criteria`, `Definition of Done`, `Test Strategy`, `Integration Test`, `Coverage`, `Mock`, `REST API`, `OpenAPI`, `JWT`, `Source of Truth`, các giá trị lifecycle status và các thuật ngữ tương tự. Việc giữ thuật ngữ tiếng Anh không có nghĩa toàn bộ câu hoặc phần giải thích phải viết bằng tiếng Anh.

Tài liệu nộp chính thức cho trường hoặc giảng viên chỉ được viết hoàn toàn bằng tiếng Anh khi tài liệu đó được project decision-maker **phân loại rõ là academic submission**. Việc phân loại phải được ghi trong metadata, document register hoặc một quyết định dự án có thẩm quyền; không được suy diễn chỉ từ tên file hoặc loại tài liệu.

Các tên loại tài liệu như SRS, Architecture, Test Strategy hoặc API Documentation không tự tạo yêu cầu viết bằng tiếng Anh. Nếu chưa được phân loại là academic submission, chúng tuân theo chính sách tài liệu phát triển nội bộ ở trên.

Tên công nghệ, identifier, code, command, path, lifecycle status, tên nguồn/tài liệu bên ngoài và đoạn trích dẫn có thể giữ nguyên ngôn ngữ gốc khi cần bảo toàn khả năng truy vết hoặc đối chiếu. Khi chỉnh sửa, phải giữ nguyên ý nghĩa, decision status và thuật ngữ đã được project xác lập.

Phần này là Source of Truth cho ngôn ngữ tài liệu, `CHANGELOG.md` và Git commit message của repository. Các file governance khác chỉ được dẫn tới đây và nêu ngắn gọn quy tắc thực thi, không lặp lại toàn bộ policy.

## Where a new file belongs

| Location | Allowed purpose | Examples and boundaries |
|---|---|---|
| Repository root | Entry points and root-level tooling only | README.md, AGENTS.md, CONTRIBUTING.md, CHANGELOG.md, LICENSE, .gitignore; new build/tool configs require a real root-level consumer |
| app/frontend/ | Frontend source, tests, assets and configuration | React component, feature test, package.json after scaffolding; place files by the app's established feature structure |
| app/mamxanh-backend/ | Backend source, tests, configuration and migrations | Java classes/tests, pom.xml, Flyway migrations after scaffolding |
| database/ | Database usage guide, deliberate SQL snapshot, demo seed and diagnostic queries | schema.sql remains empty until designed; Flyway owns executable migration history when implemented |
| docs/requirements/ | Maintained product and software requirements | PRD owns high-level product intent; root SRS owns scope, context, index, and lifecycle registry; docs/requirements/srs/ owns detailed FR, BR, and NFR specifications |
| docs/architecture/ | Current high-level system structure and selected technology baseline | ARCHITECTURE owns runtime boundaries; TECHNOLOGY-STACK owns technology purpose, rationale, trade-offs and TBD choices |
| docs/testing/ | Project-level verification strategy | Strategy and quality evidence policy, not a test-case catalog or claim that tests exist |
| docs/decisions/ | Durable project/workflow decisions and rationale | Numbered ADR with context, decision, consequences and unresolved points |
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

Examples: a recipe form belongs in app/frontend/, its API service in app/mamxanh-backend/, an AI quota rule in SRS, an integration boundary in ARCHITECTURE, and a durable provider-selection rationale in docs/decisions/. Task status belongs on its GitHub Issue/Project item.

## Maintained document register

Only entries below are maintained documentation. Read entries by task, not as a mandatory reading list.

| Path | Function and authority | Read when |
|---|---|---|
| ../AGENTS.md | Portable agent entry point, read scope and skill routing | Starting a new agent session |
| ../README.md | Product overview and implementation state | General onboarding |
| ../CONTRIBUTING.md | Operational Git/Issue/PR/release rules | Contributing or reviewing changes |
| ../CHANGELOG.md | Historical changes, not current progress | Updating notable changes or investigating history |
| README.md | File placement and document registry | Creating/moving files or routing documentation |
| requirements/PRD.md | High-level product summary, Requirements Baseline v1.0.0, Active | Product intent and scope |
| requirements/SRS.md | Root software requirements specification, scope, actors, system context, requirement indexes, cross-cutting information, and authoritative lifecycle registry, Requirements Baseline v1.0.0, Active | Scoping, routing, identifying requirements, verifying lifecycle state, or reading system context |
| requirements/srs/FUNCTIONAL-REQUIREMENTS.md | Authoritative detailed FR definitions, triggers, preconditions, exceptions, and acceptance criteria (derived lifecycle), Requirements Baseline v1.0.0, Active | Implementing or verifying functional behavior and acceptance criteria |
| requirements/srs/BUSINESS-RULES.md | Authoritative detailed BR definitions, rationale, constraints, and business logic (derived lifecycle), Requirements Baseline v1.0.0, Active | Implementing or verifying business rules and constraints |
| requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md | Authoritative detailed NFR definitions, measurable targets, quality constraints, and verification criteria, Requirements Baseline v1.0.0, Active | Architecture, performance, security, reliability, or quality assurance work |
| architecture/ARCHITECTURE.md | High-level runtime parts, boundaries, communication paths, trust boundaries and architectural constraints | Architecture or cross-component integration work |
| architecture/TECHNOLOGY-STACK.md | Selected technologies, purpose, rationale, benefits, trade-offs and unresolved choices | Dependency or technology decisions |
| testing/TEST-STRATEGY.md | Project-level test levels, evidence boundaries, traceability, coverage interpretation and completion relationship | Test planning, quality gates or verification design |
| decisions/001-team-workflow.md | Branch/release decision and rationale | Workflow changes |
| decisions/002-five-member-team-operating-agreement.md | Team responsibilities and coordination | Team process |
| decisions/WORKFLOW-SOURCES.md | Evidence behind workflow conventions | Reconsidering a workflow decision |
| research/similar-products-benchmark.md | Dated external research, non-authoritative | Relevant product comparison |
| diagrams/Activity/README.md | Activity diagram conventions | Creating/updating activity diagrams |
| diagrams/UseCase/README.md | Use-case diagram conventions | Creating/updating use-case diagrams |
| diagrams/ERD/README.md | ERD documentation conventions | Creating/updating the data model |
| ../app/frontend/README.md | Frontend setup/state and contribution guidance | Frontend work |
| ../app/mamxanh-backend/README.md | Backend setup/state and contribution guidance | Backend work |
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

## Evidence-triggered future documents

Do not create empty documents to complete a checklist. A future authorized documentation task may create and register:

- `development/DEVELOPMENT.md` only after real frontend/backend scaffolds and verified day-to-day development, build, test and migration commands exist.
- `setup/SETUP.md` only after a clean checkout can be configured, started and verified end-to-end with tested steps.
- `api/API.md` only after the project adopts an OpenAPI (or equivalent real) contract or implements endpoints with shared authentication, error and API conventions that can be verified.

Until each trigger is satisfied, workspace READMEs and implementation artifacts may record verified local facts without pretending that a maintained cross-project guide exists.

## Progress and evidence

GitHub Issues/Projects own assignments, estimates, dates, status and blockers. Linked PRs own review and verification evidence. No per-member reports, weekly report files or parallel local progress tracker are required. Long-lived technical decisions extracted from discussion belong in ADRs; progress snapshots do not.
