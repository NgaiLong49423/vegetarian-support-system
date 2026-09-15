> **Document:** Changelog
> **File:** `CHANGELOG.md`
> **Version:** v2.12.0
> **Created:** 2026-06-14
> **Last Updated:** 2026-09-15
> **Status:** Active

# Changelog

Notable project changes, grouped by date and topic. Writing rules are maintained in [CONTRIBUTING.md](CONTRIBUTING.md#changelog-format). Documentation decisions below describe scope, not implemented or deployed features.

## 2026-09-15 — Unified AI Chatbot Model & Recipe Instruction Step Baseline Alignment

**Status:** Working tree — not committed.

**Scope:** Unify AI Chatbot into a single multi-capability assistant under FR-51 per approved Product Decision, align AI Chatbot performance with NFR-03, expand NFR-25 AI quality and compliance evaluation, clarify BR-51 authority boundaries, enforce mandatory 1–30 cooking steps across Recipe Post requirements, and update Root SRS Draft status definition.

### Added

- Unified multi-capability AI Chatbot specification in `FR-51` with context-driven execution across General Culinary & Nutrition Context (`UC-51.1`) and Recipe Context (`UC-51.2`), supporting culinary Q&A, nutrition/calorie explanations, ingredient substitutions, and step-by-step cooking clarifications without fragmentation.

### Changed

- Re-scoped `FR-02` to strictly govern Guest trial quota (5 queries/day) and authentication boundary while delegating conversational behavior to `FR-51`.
- Normalized AI Chatbot performance authority to `NFR-03` ($\le 5$s normal response, P90 $\le 7$s under high load/concurrency); eliminated stale $\le 4$s timeouts and decoupled database query performance (`NFR-07`) from AI response time.
- Expanded `NFR-25` title to `Đánh giá chất lượng và tuân thủ của nội dung AI` and widened scope/threshold ($\ge 80\%$) to cover all AI-generated outputs (recommendations, related recipes, and Chatbot) for source boundaries, dietary/restriction constraints, non-fabrication, and safety rules.
- Enforced mandatory 1–30 instruction steps for Recipe Post across `SRS 3.1`, `3.3`, `3.6`, `BR-19`, `BR-20`, `FR-16`, `FR-21`, and `FR-22`; eliminated stale optional-step wording and removed unapproved per-step separate illustration images from `FR-22`.
- Updated definition of `Status: Draft` in `docs/requirements/SRS.md` to reflect that Phase-2 decomposition is complete and the baseline is pending Project Owner/team review and freeze, explicitly decoupling Document Status from Requirement Lifecycle.

### Fixed

- Fixed semantic misuse of `BR-51` in `FR-51`: stated functional non-mutation of user business data directly within `FR-51` and restricted `BR-51` citations strictly to its authoritative domain (AI cannot create or modify official nutrition catalog values).
- Removed implementation leakage (HTTP status codes, auto-scroll UI, client form state mechanics) from requirements prose.

## 2026-09-15 — Cross-Document Corrective Pass & Dual-Layer Validation Alignment

**Status:** Committed — 5b1abfe.

**Scope:** Resolve remaining cross-document inconsistencies identified during full Phase-2 requirements audit, enforce Owner-only access for private user data, align authentication with HttpOnly refresh cookies, clean up system mechanism Use Cases, and clarify dual-layer validation without server-side drafts.

### Added

- Dual-layer validation execution and draft boundary specifications in `BR-19`, `FR-16`, and `FR-25`, explicitly separating mandatory server-side rejection (`HTTP 400 Bad Request` with no DB persistence) from client-side in-memory form state retention for inline user correction.
- Detailed architecture rationale in `FR-24` (`OUT_OF_SCOPE`) documenting why incomplete server drafts and orphan asset persistence are excluded from the MVP.

### Changed

- Aligned authentication flows in `FR-03` with `TECHNOLOGY-STACK.md`: Refresh Token is issued, rotated, and expired exclusively via Secure HttpOnly SameSite Cookie inaccessible to client-side JavaScript; server manages session family revocation upon logout and reuse detection.
- Enforced strict `Owner-only by default` authorization in `FR-23` for all personal private resources (nutrition profile, health/BMI metrics, dietary preferences, saved recipes, meal plans, shopping lists); eliminated generic `isOwner || isAdmin` bypass logic and restricted Administrator access to minimal resource-specific administrative fields (`accountId`, email, account status, moderation logs) authorized by BR/NFR.
- Restricted cooking step reordering and editing in `FR-21` and `AC-23.10` to the recipe author per `BR-64`, keeping Administrator actions confined to independent content moderation (hide/remove).
- Reclassified automated internal background actions (`UC-11.1` AI token telemetry recording, `UC-11.3` 90-day retention purge, `UC-14.3` image resource lifecycle cleanup) from standalone Use Cases into System Behaviors and Cleanup Flows in `FR-11` and `FR-14`, preserving 100% of testable Acceptance Criteria (`AC-11.1`, `AC-11.4`, `AC-14.3`).
- Standardized Primary Actor in `FR-54` as `Member` (shopper needing consolidated ingredients) with the aggregation engine designated as a supporting system mechanism.
- Reclassified technical implementation specifics to non-blocking categories: domestic payment gateway to `TECHNICAL_DESIGN_TBD` and exact Gemini model variant to `TECHNICAL_SPIKE_REQUIRED`, with all Phase-1 business boundaries locked.
- Clarified external Google Maps dependency in `TEST-STRATEGY.md` with deferred module condition `(nếu M11 được kích hoạt sau này)`.

### Fixed

- Fixed contradictory wording in `FR-03` that previously suggested tokens were returned in application payloads and cleared by client-side storage manipulation.
- Fixed ambiguous administrative permissions in `FR-21` and `FR-23` that risked leaking member private personal data or allowing admin direct modification of author cooking steps.

## 2026-09-14 — Phase-2 Functional Requirement Semantic Repair & Full Decomposition

**Status:** Committed — 8db98d6.

**Scope:** Complete Phase-2 requirement decomposition and full semantic repair across all 56 Functional Requirements in `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md`. Restored authoritative Phase-1 business decisions, quotas, pricing, validation profiles, and security boundaries while preserving deep decomposition for all 48 ACTIVE requirements.

### Added

- Full Phase-2 decomposition for all 48 ACTIVE Functional Requirements (`FR-01` to `FR-04`, `FR-06` to `FR-11`, `FR-13` to `FR-23`, `FR-25` to `FR-41`, `FR-44` to `FR-49`, `FR-51`, `FR-53` to `FR-55`) with 131 Use Cases, detailed main/alternative/exception event flows, permission rules, and 248 atomic Given/When/Then Acceptance Criteria.
- Comprehensive semantic traceability mapping linking each active FR to corresponding Business Rules (`BR-01`–`BR-68`) and Non-Functional Requirements (`NFR-01`–`NFR-27`).
- Three-tier credential and data visibility boundary in `FR-23` separating Public Profile, Private Account/Profile Data, and Security / Credential Internals under least-privilege principles.
- Single Source of Truth (SSOT) delegation for Recipe Validation Profile in `FR-16`, referenced consistently by `FR-25`, `FR-40`, and `FR-44`.

### Changed

- Restored authoritative AI quotas across all tiers: Guest 5/day, Free Member 5/day, Plus Member 15/day, Pro Member 50/day; reset at 00:00 `Asia/Ho_Chi_Minh` (GMT+7); provider failures do not consume quota.
- Aligned subscription and payment terms in `FR-13`: Free 0 VND, Plus 49,000 VND/month, Pro 99,000 VND/month; monthly prepaid cycle, no auto-renewal, no partial refund, verified activation, and idempotent payment processing.
- Relocated misplaced decomposition content to authoritative owners: Comments to `FR-46`, Likes/Favorites to `FR-45`, Chatbot to `FR-51`, Recipe Card/Details to `FR-17`/`FR-20`, and Shopping List Export to `FR-55`.
- Normalized login rate-limiting behavior in `FR-03`: 10-minute temporary rate limit after 5 consecutive failures enforced across both account identifier and source IP without converting the account to administrator `LOCKED` state.
- Standardized password hashing terminology to BCrypt (minimum work factor 10) in `FR-03`.
- Preserved frozen historical scope without implementation scope for the 8 non-ACTIVE requirements: `FR-05` (RETIRED), `FR-12` (DEFERRED), `FR-24` (OUT_OF_SCOPE), `FR-42` (DEFERRED), `FR-43` (DEFERRED), `FR-50` (OUT_OF_SCOPE), `FR-52` (OUT_OF_SCOPE), and `FR-56` (OUT_OF_SCOPE).

### Fixed

- Removed all unauthorized/drifted terms, including fabricated "Premium" plans, 100-request quotas, and obsolete subscription prices (59k/149k/499k VND).
- Fixed misleading Free AI quota call-to-action in `FR-02` that incorrectly implied Free tier offered 15 requests/day.
- Fixed mandatory cooking step contradictions in `FR-07`, `FR-25`, `FR-40`, and `FR-44`, ensuring cooking steps remain optional (0 to 30 steps) per `FR-16` and SRS 3.9.
- Eliminated obsolete validation limits (5–100 title characters, 1–20 servings) in `FR-25`.
- Repaired corrupted LaTeX formatting control characters (`\frac`, `\times`, `\rightarrow`) across all requirement blocks.

## 2026-09-14 — Phase-1 Decision Baseline Synchronization

**Status:** Committed — `ad9e004` (verified documentation checkpoint).

**Scope:** Đồng bộ corrective baseline DEC-001–DEC-018 trên requirement set và tài liệu bị ảnh hưởng; giữ stable IDs/lịch sử, không tạo GitHub Issues hoặc bắt đầu Phase-2 decomposition.

### Added

- Ghi nhận baseline thương mại MVP: FREE 0, PLUS 49,000 và PRO 99,000 VND/tháng; monthly, no auto-renew, no partial refund, verified activation, end-of-period expiry và idempotent duplicate processing.
- Ghi nhận baseline authentication dùng short-lived access token, rotating refresh token, refresh-session/server-side revocation và logout revocation.
- Giữ `DEC-016 — A — APPROVED`: **Backend Architecture: Modular Monolith using MVC/layered structure within each business module.**

### Changed

- Chuẩn hóa lifecycle 56 FR, 68 BR và 27 NFR; chuyển M11 cùng FR/BR liên quan sang `DEFERRED`, giữ các mục `OUT_OF_SCOPE`/`RETIRED`.
- Đồng bộ Recipe Post validation, Shopping List unit conversion, tombstone/reference preservation, manual moderation, comment depth, notification, AI quota/telemetry, nutrition, subscription, security, NFR performance và curated AI evaluation.
- Đồng bộ SRS/FR/BR/NFR, PRD, README, Architecture, Technology Stack và Test Strategy; không thay đổi code, schema, API hoặc GitHub work item.

### Fixed

- Loại bỏ legacy/unknown lifecycle wording và các xung đột còn mô tả M11 là MVP/Pending Review, giá subscription/JWT lifecycle là chưa quyết định, hoặc NFR design goals là MVP release gates.

## 2026-09-14 — NFR Decomposition & ISO/IEC 25010 Quality Categorization

**Status:** Working tree — not committed.

**Scope:** Chuẩn hóa và phân rã chi tiết 27 yêu cầu phi chức năng (NFR-01 đến NFR-27) vào 6 nhóm danh mục cốt lõi của dự án theo chuẩn ISO/IEC 25010; tích hợp tiêu chí đo lường định lượng và ranh giới kiến trúc đã xác nhận.

### Added

- Phân rã 27 yêu cầu phi chức năng chi tiết (`NFR-01` đến `NFR-27`) vào `docs/requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md` (v0.2.0) với đầy đủ Metric, Threshold và Verification Method, gắn thẻ stable anchor cố định `<a id="nfr-xx"></a>`.
- Bổ sung ranh giới phân loại và loại trừ rõ ràng:
  - Ghi nhận `NFR-05` với mục tiêu thiết kế 500 CCU và ngưỡng nghiệm thu thực tế đồ án 50–100 CCU.
  - Ghi nhận khuyến nghị `NFR-08` không mã hóa cột Database AES-256 đối với chỉ số sức khỏe/BMI để tránh phức tạp hóa database.
  - Đánh dấu trạng thái `OUT_OF_SCOPE` cho `NFR-26` (AI rà soát/gắn cờ nội dung) và `NFR-27` (Cá nhân hóa nội dung kiểu TikTok For You theo dõi lịch sử).
  - Loại bỏ hoàn toàn đề xuất AI microservice và quota gọi chatbot khỏi bộ tiêu chuẩn NFR.

### Changed

- Nâng cấp `docs/requirements/SRS.md` lên `v0.49.0`: Cập nhật bảng tổng hợp Mục 9 liên kết trực tiếp tới 6 nhóm chất lượng và 27 mã NFR chi tiết.

### Fixed

- None.

## 2026-09-14 — Modular SRS Architecture & Field-Specific Authority Model

**Status:** Working tree — not committed.

**Scope:** Tái cấu trúc tài liệu đặc tả yêu cầu phần mềm (SRS) sang mô hình Modular SRS đa tệp, phân tách `SRS.md` gốc thành sổ bộ chỉ mục có thẩm quyền (authoritative registry) và 3 tài liệu đặc tả chi tiết chuyên biệt (`FUNCTIONAL-REQUIREMENTS.md`, `BUSINESS-RULES.md`, `NON-FUNCTIONAL-REQUIREMENTS.md`).

### Added

- Thư mục `docs/requirements/srs/` chứa 3 tài liệu đặc tả chi tiết:
  - `FUNCTIONAL-REQUIREMENTS.md`: Nắm giữ thẩm quyền định nghĩa chi tiết cho 56 Functional Requirements (`FR-01` đến `FR-56`), gắn thẻ stable HTML anchor `<a id="fr-xx"></a>`, giữ nguyên wording và lifecycle gate notes từ baseline.
  - `BUSINESS-RULES.md`: Nắm giữ thẩm quyền định nghĩa chi tiết cho 68 Business Rules (`BR-01` đến `BR-68`), gắn thẻ stable HTML anchor `<a id="br-xx"></a>`.
  - `NON-FUNCTIONAL-REQUIREMENTS.md`: Nắm giữ thẩm quyền định nghĩa chi tiết cho 6 nhóm NFR (Security, Privacy, Reliability, Performance, Usability, Auditability), gắn thẻ category anchor `<a id="nfr-category"></a>`, bảo lưu các trường chưa chốt dạng `TBD`/`OPEN` mà không tự phát minh mã số `NFR-xx`.
- Bổ sung loại phát hiện `Lifecycle Synchronization Drift` vào kỹ năng `repo-template-doc-sync-auditor`.

### Changed

- Cập nhật `docs/requirements/SRS.md` lên `v0.48.0`: Chuyển đổi Mục 7 và 8 sang bảng chỉ mục 5 cột Option A (`ID | Short Name | Module | Lifecycle | Detail`) đóng vai trò Authoritative Registry cho sự tồn tại, mã định danh, phân bổ module và trạng thái lifecycle; Mục 9 chuyển thành bảng tổng hợp NFR liên kết chi tiết.
- Cập nhật quy tắc quản trị tài liệu và agent trong `AGENTS.md` (v3.3.0), `docs/README.md` (v3.2.0) và `.agents/repo-contract.yml` ghi nhận cấu trúc Modular SRS, phân định ranh giới thẩm quyền theo trường dữ liệu (Field-Specific Authority Model) và quy trình đọc phạm vi hẹp (2-step scoped reading).
- Đồng bộ các kỹ năng nội bộ (`markdown-documentation`, `srs-to-github-issues`, `repo-template-doc-sync-auditor`) để hỗ trợ Modular SRS, 2-phase issue resolution và kiểm toán tính nhất quán 2 chiều.

### Fixed

- None.

## 2026-09-14 — Personal Pantry & Inventory-based Recommendation Scope

**Status:** Working tree — not committed.

**Scope:** Phân rã chi tiết chức năng Quản lý kho thực phẩm cá nhân (Pantry/Inventory), tìm kiếm công thức/bài viết từ kho và AI sáng tạo công thức mới từ nguyên liệu có sẵn; xác định trạng thái ngoài phạm vi MVP ban đầu (`OUT_OF_SCOPE`).

### Added

- Mục 3.22 trong SRS phân rã quản lý kho thực phẩm cá nhân của Member (`Personal Pantry`), tìm kiếm công thức theo mức độ khớp nguyên liệu (100% hoặc thiếu ít kèm gợi ý thêm vào Shopping List) và tính năng AI Gemini sáng tạo công thức món chay mới từ nguyên liệu kho.
- Module M13 (`Personal Pantry & Inventory-based Recipe Recommendation`) và yêu cầu chức năng FR-56 với trạng thái `OUT_OF_SCOPE` cho MVP ban đầu.

### Changed

- Đồng bộ mục ngoài phạm vi trong PRD Mục 5 và SRS Mục 3.2 làm rõ phân rã M13/FR-56 cho tương lai.

### Fixed

- None.

## 2026-09-14 — Basic Shopping List MVP Adoption

**Status:** Working tree — not committed.

**Scope:** Đưa chức năng Shopping List cơ bản từ ngoài phạm vi vào MVP chính thức, tích hợp dưới Module M05 với 3 yêu cầu nghiệp vụ (FR-53, FR-54, FR-55) và quy tắc gom nguyên liệu an toàn.

### Added

- Yêu cầu chức năng FR-53 (quản lý Shopping List, thêm item vặt, tick đã mua), FR-54 (gom nguyên liệu an toàn khi cùng ID và đơn vị, phân nhóm quầy hàng) và FR-55 (copy clipboard, xuất file .txt) vào SRS Mục 7.2 với trạng thái `ACTIVE`.

### Changed

- Tích hợp Shopping List vào Module M05 (đổi tên thành `Saved Recipes, Meal Planning & Shopping List`).
- Bỏ shopping list khỏi danh sách ngoài phạm vi trong SRS Mục 3.2 và PRD Mục 5; làm rõ các tính năng nâng cao (kho pantry, chia sẻ nhiều người, đồng bộ realtime, AI tự quy đổi đơn vị, xuất PDF) nằm ngoài phạm vi MVP.

### Fixed

- None.

## 2026-09-14 — Restaurant Discovery Review and True Blog Scope

**Status:** Working tree — not committed.

**Scope:** Chuyển chức năng tìm kiếm nhà hàng chay sang diện chờ giảng viên review lại; phân rã chi tiết mô hình Đăng Blog thật nhúng thẻ công thức theo Samsung Food và xác định ngoài phạm vi MVP ban đầu.

### Added

- Mục 3.21 trong SRS phân rã thực thể Blog Post, định dạng Rich Text văn xuôi, khối nhúng Embedded Recipe Card và tương tác lưu/xếp lịch món ăn từ bài blog.
- Yêu cầu chức năng FR-52 và Module M12 ghi nhận năng lực Đăng Blog với trạng thái `OUT_OF_SCOPE` cho MVP ban đầu.

### Changed

- Chuyển Module M11, FR-42, FR-43 và tích hợp Google Maps Platform sang trạng thái `PENDING_REVIEW` (chờ giảng viên hướng dẫn review).
- Cập nhật PRD, System Architecture và Technology Stack đồng bộ trạng thái chờ xem xét của Google Maps và xác định ranh giới Blog ngoài MVP.

### Fixed

- None.

## 2026-09-14 — Documentation Language Policy

**Status:** Working tree — not committed.

**Scope:** Thiết lập tiếng Việt làm ngôn ngữ văn xuôi chính cho tài liệu phát triển nội bộ, đồng thời giữ các technical term tiếng Anh cần thiết và chỉ dùng hoàn toàn tiếng Anh cho tài liệu được phân loại rõ là academic submission.

### Added

- Chính sách ngôn ngữ tài liệu có thẩm quyền trong maintained document register.

### Changed

- Liên kết AGENTS.md và CONTRIBUTING.md tới policy có thẩm quyền mà không sao chép toàn bộ nội dung.
- Chuẩn hóa Architecture, Technology Stack, Test Strategy và tài liệu bằng chứng workflow sang văn xuôi tiếng Việt, đồng thời giữ nguyên ý nghĩa kỹ thuật và các English term đã được xác lập.

### Fixed

- Loại bỏ cách hiểu trước đây rằng tài liệu kỹ thuật hoặc governance phải mặc định viết bằng tiếng Anh chỉ vì loại tài liệu có tên tiếng Anh.

## 2026-09-13 — Controlled Documentation Baseline Migration

**Status:** Working tree — not committed.

**Scope:** Separate product requirements, software requirements, architecture, technology selection and verification strategy into maintained sources with explicit ownership, while preserving existing requirement meaning and holding unresolved lifecycle assignments for decision.

### Added

- Maintained System Architecture, Technology Stack and Test Strategy documents with explicit evidence limits and unresolved implementation choices.
- Evidence-based creation triggers for future Development, Setup and API documents.

### Changed

- Migrated current product rationale, actor/onboarding context, AI boundaries and technology decisions into their authoritative destinations.
- Updated repository navigation, agent routing and the machine-readable contract to use the new architecture, technology and testing paths.
- Retired and deleted the four superseded migration sources after their unique current information and active references had been migrated and deletion was explicitly approved.

### Fixed

- Corrected stale summary and research statements that treated Google Maps restaurant discovery as unconfirmed or described the retired permission-application workflow as current.
- Added an explicit lifecycle decision gate so legacy phrases such as “Đã chốt” are not silently converted to `ACTIVE`.

## 2026-09-13 — Recipe Publishing and AI Scope Simplification

**Status:** Working tree — not committed.

**Scope:** Align the requirements baseline with the team's confirmed simplification of Recipe Post publishing and deferred AI features.

### Added

- None.

### Changed

- Members publish valid Recipe Posts directly; the permission-application workflow is retired and Administrator moderation is report-driven.
- Marked persistent Recipe Post drafts and per-account AI chat history as out of scope for the current MVP.
- Marked AI content scanning/flagging as deferred rather than an MVP acceptance requirement.
- Clarified FR-35's eligibility, input/output, and non-diagnostic boundary; separated FR-49 notifications from report-identity privacy in FR-29/BR-28.

### Fixed

- Removed conflicting summary references to pre-publication permission approval and persistent drafts in the directly affected requirement documents.

**Related PR:** None.
## 2026-09-13 — Changelog Format and Contribution Authority

**Status:** Working tree — not committed.

**Scope:** Standardize the English project history and separate shared contribution rules from AI-specific instructions.

### Added

- A dated, topic-based changelog format and reference template in CONTRIBUTING.md.

### Changed

- Converted the previous accumulating history into dated topics with Added, Changed and Fixed sections, explicit status and scope.
- Made CONTRIBUTING.md the canonical contribution workflow; AGENTS.md now directs agents there before repository changes and retains agent-specific routing, context and authorization instructions.
- Moved shared editing guidance out of AGENTS.md; aligned the repository contract and changelog skill with the same authority.
- Removed changelog tutorials and illustrative release history from this file; preserved actual historical decisions and distinguished commit checkpoints from uncommitted work.

### Fixed

- Resolved the mismatch between the requested daily format and the changelog skill's generic Unreleased format.

## 2026-09-12 — Repository Layout and Agent Onboarding

**Status:** Working tree — not committed.

**Scope:** Establish predictable document placement and targeted onboarding for new agents.

### Added

- A maintained document register with placement rules, document purposes and read triggers.
- Task-based source and skill routing, including boundaries for unregistered files.

### Changed

- Moved product-direction and AI-plan decomposition notes from the root into requirements documentation.
- Kept GitHub Issues/Projects as the progress source and linked PRs as review/test evidence.
- Removed completed SRS questionnaires, the superseded SRS template and obsolete local audit reports; retained product decisions in SRS section 3.20.
- Returned audit findings in the conversation unless an export is requested; stopped automatic report generation and registration of scratch artifacts.

### Fixed

- Removed ambiguous placement and automatic-reading expectations for unregistered documents.

## 2026-09-12 — Project Documentation and Five-Member Team Setup

**Status:** Working tree — not committed.

**Scope:** Adapt repository guidance to the Vegetarian Support System and the five-member SWP391 team.

### Added

- ADR-002 documenting owner/reviewer responsibilities, work-in-progress limits, review, decision-making and contribution evidence.
- Consistency and metadata audits; their initially generated local reports were subsequently removed during the layout cleanup above.

### Changed

- Personalized README, PRD and frontend/backend/database workspace guides while distinguishing documentation plans from the unimplemented application.
- Standardized maintained-document metadata and aligned app/ paths, .agents/skills/ paths and issue-number branch naming.

### Fixed

- Replaced machine-specific file links and removed actor/template descriptions that contradicted the current SRS.

## 2026-09-12 — Consolidated Product Scope and Traceability

**Status:** Committed — `ccd6def` (verified documentation checkpoint).

**Scope:** Record the confirmed questionnaire decisions and align supporting product documents.

### Added

- A 38-question SRS decision questionnaire with recommended choices, used during scope clarification and later removed after decisions were consolidated.
- FR-47 through FR-51 and the associated traceability table in SRS v0.43.0.

### Changed

- Consolidated Q01–Q38: Free/Plus/Pro share the same functions; real payments, comment reports/Likes, email and in-app notifications, and road-distance map results are included in the documented scope.
- Recorded sources for Q31 while deferring formula research to implementation; treated Q35 as team-selected scope without an instructor-confirmation prerequisite.
- Synchronized questionnaire answers, actors, product direction, AI plans and benchmark documents.

### Fixed

- None.

## 2026-09-12 — Recipe Publishing and Community Decisions

**Status:** Committed — `ccd6def` (verified documentation checkpoint).

**Scope:** Preserve the recipe and moderation decisions recorded across SRS revisions.

### Added

- Duplicate-report prevention in SRS v0.19.0.

### Changed

- SRS v0.21.0 replaced general blogs with a single Recipe Post content type and made step-by-step cooking instructions optional.
- SRS v0.34.0 replaced per-recipe approval with publishing-permission applications: authorized Members publish valid posts directly; Administrators moderate reported content and record reasons for sanctions. AI sources became public posts that are not hidden or deleted.
- SRS v0.35.0 required policy acknowledgement, contribution intent, responsibility and sanction awareness in applications, without professional certificates or experience.
- SRS v0.36.0 allowed rejected applications to be revised and resubmitted with rejection reasons, limited each Member to one pending application, and required reasons for abuse restrictions without a fixed MVP cooldown.
- SRS v0.37.0 allowed published recipes to be edited without reapproval; v0.38.0 clarified authors' create/read/update/delete rights and prevented authors from restoring Administrator-hidden posts.
- SRS v0.39.0 defined one reversible Like/Upvote per Member per post, Guest visibility of totals and no five-star rating system.
- SRS v0.40.0 defined nested replies, Guest reading, Members managing their own comments/replies, and Administrator moderation.

### Fixed

- None.

## 2026-09-12 — Onboarding, AI Selection and Meal Planning

**Status:** Committed — `ccd6def` (verified documentation checkpoint).

**Scope:** Clarify personalization prerequisites and the relationship between saved recipes, AI and meal plans.

### Added

- None.

### Changed

- SRS v0.20.0 made onboarding skippable but required three minimum profile groups before personalized AI recipe suggestions or menu generation.
- SRS v0.22.0 separated saved recipes from meal planning, kept personal data Member-only and excluded Queue from MVP.
- SRS v0.23.0 fixed the meal types to breakfast, lunch and dinner, without snacks or custom meal types.
- SRS v0.24.0 allowed multiple recipes per meal without a hard cap, while preventing duplicate Member/date/meal/recipe entries.
- SRS v0.25.0 restricted AI to source recipes and required source links instead of invented recipes. Its original approved-post wording was superseded by the public, non-hidden/non-deleted source rule in v0.34.0.

### Fixed

- None.

## 2026-09-12 — Nutrition Scope and Catalogue Ownership

**Status:** Committed — `ccd6def` (verified documentation checkpoint).

**Scope:** Define nutrition reference behavior and its supported users and data sources.

### Added

- None.

### Changed

- SRS v0.26.0 treated BMI as a reference within the broader nutrition profile and added nutrition-based AI menus and daily status/difference checks without diagnosis.
- SRS v0.27.0 limited nutrition-needs, AI nutrition menus and nutrition checks to Members aged 18 or over who are not pregnant, breastfeeding or requiring therapeutic diets; ordinary features remain available.
- SRS v0.28.0 selected nine MVP nutrition metrics with per-serving presentation and personal daily references, excluding an overall balance score and Glycemic Index/Glycemic Load.
- SRS v0.29.0 based recipe nutrition on ingredient quantities, used servings to allocate recipe totals and planned intake, and summed all recipes across the day's three meals.
- SRS v0.30.0 selected a fixed database nutrition catalogue with USDA FoodData Central as the primary reference, without runtime API calls or AI matching. Unsupported ingredients do not block posting but make nutrition results incomplete.
- SRS v0.31.0 assigned catalogue ownership to Administrators; v0.32.0 defined search, create, edit, enable/disable, related-recipe inspection and source recording, excluding permanent deletion, bulk import, USDA calls and AI autofill.

### Fixed

- None.

## 2026-09-12 — Address-Based Restaurant Discovery

**Status:** Committed — `ccd6def` (verified documentation checkpoint).

**Scope:** Align the restaurant-discovery requirements and technology baseline.

### Added

- None.

### Changed

- SRS v0.33.0 included vegetarian-restaurant discovery around an entered address through Google Maps Platform, not GPS or Gemini, and documented Geocoding/Places dependencies, failure states and open UX/cost decisions.
- SRS v0.37.0 made discovery independent of searched recipes and excluded restaurant-profile management.
- SRS v0.41.0 restricted discovery to authenticated Members and rejected Guest requests before consuming external quota.
- SRS v0.42.0 limited radii to 500 m, 1 km, 5 km and 10 km, rejecting other values before external calls.
- Technology Stack v2.0.0 replaced v1.0.0: entered addresses and Google Geocoding/Places replaced Browser Geolocation and an internal SQL Server restaurant/store catalogue.

### Fixed

- None.

## 2026-09-12 — Topic 03 Import and Workflow Checkpoint

**Status:** Committed — `86bf864` (verified documentation checkpoint).

**Scope:** Preserve imported project documentation and previously undated workflow history. For the workflow items, this is the verified repository checkpoint date, not a claimed original change date.

### Added

- Imported five Topic 03 decomposition documents and retained the previous SRS template for reference at that time; the template was later removed during the documented cleanup.
- ADR-001 and WORKFLOW-SOURCES to distinguish platform behavior, framework guidance, complementary practices and team conventions.
- Definition of Ready for the transition from Planning to In Progress.

### Changed

- Documented develop/main delivery, 4–5-day tasks, PR review, release checklists and Story Points for workload planning.
- Defined Target Date as technical completion and feature merge into develop; Review includes the wait for release to main without treating that wait as owner lateness.
- Linked README and CONTRIBUTING to the five-status workflow and standardized agent, skill and output paths under .agents/.
- Aligned Issue tooling with Story Points 1, 2, 3, 5 and 8, removed Size, and required 8-point Issues to be split before assignment.
- Aligned six Project Types with primary labels and Issue Forms; classified NFR as primary Task plus secondary NFR label.
- Reduced Issue bodies to seven sections, separated Draft State from Project Status, and allowed owner/dates to remain TBD until work starts.
- Defined the develop merge gate around traceability, Acceptance Criteria, verification evidence, review and relevant documentation/database updates.
- Distinguished Refs links on feature PRs from Closes links on release PRs.
- Defined the main release gate: scope, build/tests, integrated demo flows, database, documentation, two approvals and no branch-protection bypass.
- Added post-merge smoke testing, reopening affected Issues, creating Bugs and stopping further releases pending a revert/hotfix decision after failure.

### Fixed

- None.
