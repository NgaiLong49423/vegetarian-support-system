---
name: pr-issue-review
description: 'Review a Mâm Xanh implementation PR and its Issue across multiple rounds: trace SRS acceptance criteria to code and evidence, verify fixes and rebuttals, review SHA deltas, determine merge readiness, and continue Issue acceptance on main. Use for initial PR review, resumed review, developer rebuttal, or post-merge acceptance; not for implementing an Issue or a general repository audit.'
---

# PR / Issue Review

## Phạm vi và authority

Skill sở hữu phương pháp review; [review-pr-and-accept-issue](../../workflows/review-pr-and-accept-issue.md) sở hữu sequencing, checkout, live report và giao tiếp. Không tự sửa implementation để làm finding biến mất.

- Đọc phần liên quan trong `AGENTS.md`, `CONTRIBUTING.md`, `.agents/POLICY.md`, `.agents/repo-contract.yml`, `docs/README.md`, `docs/testing/TEST-STRATEGY.md`, PR template hiện hành. Ghi revision của rule; tooling chưa merge không tự thay authority trên develop của PR khác.
- SRS root xác định current requirement IDs; child SRS đã đăng ký sở hữu FR/BR/UC/AC chi tiết. Issue quản lý công việc, không định nghĩa lại SRS. Nêu mâu thuẫn và chặn phần phụ thuộc, không tự chọn nghĩa tiện cho implementation.
- Đọc động coverage, commands, required checks, review/branch-sync gates, Definition of Done và Ruleset/check state. Không hard-code threshold thành authority mới. Không đọc được GitHub state thì ghi UNVERIFIED.
- Route Backend/API/database tới [mamxanh-backend-development](../mamxanh-backend-development/SKILL.md); Frontend/UI/Playwright tới [mamxanh-frontend-development](../mamxanh-frontend-development/SKILL.md). Các skill đó sở hữu kiểm tra kỹ thuật; không sao chép checklist vào đây.
- Reuse [bug-recording](../bug-recording/SKILL.md) cho bug có evidence theo policy; liên kết BUG-ID đã có thay vì tạo trùng. F-ID vẫn thuộc review PR. Không tự tạo Bug Issue, sửa bug hoặc mở rộng scope.
- Tuân thủ authorization và read-only của task. Skill không cấp quyền GitHub mutation, merge, close/reopen Issue, update Project, resolve conversation hoặc submit approval review.

## Scope, traceability và pass criteria

1. Xác nhận PR/Issue/base; target khác develop phải dùng gate tương ứng, không gán PASS develop cho release PR.
2. Initial review đọc PR diff từ merge-base tới exact HEAD, cùng base tip/integration assumptions. Trace `Issue → FR/BR/UC/AC → files/diff → tests/evidence`. Test/file tồn tại không chứng minh AC đạt.
3. Chọn gate theo scope/boundary thật: code, Backend, Frontend, API, migration, integration/provider, Docker Development, CI, human review, required conversations, branch sync. NOT_APPLICABLE phải có lý do.
4. Evidence ghi source SHA, môi trường, command/check URL, kết quả quan sát được và AC/gate chứng minh. Phân biệt developer evidence với reviewer tự tái hiện. Artifact thiếu provenance không được ghi PASS.

## Evidence theo boundary

| Loại | Chứng minh được | Không tự chứng minh |
|---|---|---|
| Mock test / frontend-only Playwright | Logic/UI với dependency giả hoặc phạm vi FE | Backend, database, provider thật |
| Docker Development smoke | Stack build/start/health và smoke assertions đã chạy | Business AC end-to-end |
| Integration | Các runtime boundary thật đã đi xuyên, với assertions cụ thể | Toàn bộ business flow nếu chỉ test một đoạn |
| Business E2E | Luồng AC từ điểm vào tới kết quả qua đủ boundary | AC/runtime khác chưa kiểm tra |

- AC có thể cần FE→BE, FE→BE→SQL Server hoặc thêm provider. Không mặc định API tính toán cần database. Ghi real/mock cho từng boundary; test hybrid chỉ chứng minh đoạn thật đã chạy.
- Endpoint đã implement: tra generated `/v3/api-docs` hoặc generated artifact cùng commit trước Controller/DTO. `docs/api/openapi.yaml` chỉ là planned contract cho endpoint chưa implement; báo runtime contract conflict.
- Khi cần stack tích hợp mà root `docker-compose.yml` hỗ trợ, ưu tiên stack đó theo CONTRIBUTING. Ghi môi trường thật; Compose project name riêng không tự tránh xung đột host port.
- Provider live/sandbox chỉ cần khi AC cần chứng minh kết nối thật; không gửi email/tạo dữ liệu trên tài khoản thật ngoài authorization. Mock không thay thế evidence đó.
- CI fail do assertion/code là FAIL; hạ tầng/provider/Docker không khả dụng là BLOCKED; chưa chạy/không rõ kết quả là UNVERIFIED. Giữ kết quả độc lập đã có và ghi unblock action. Skip/cancel/pending/missing check không phải PASS. Không hạ threshold hoặc bỏ check để vượt gate.

## Delta re-review và evidence còn hiệu lực

- Đọc report trước mỗi round. `last reviewed HEAD..current HEAD` là diff giữa hai tree, không chỉ danh sách commit. Xét cả base/develop delta, AC/rule, rebuttal và evidence mới ngay cả khi HEAD không đổi.
- Review finding được sửa/phản bác, code delta và dependency bị ảnh hưởng: caller/callee, shared auth/config, DTO/client, schema/migration, dependency/lockfile, tests, runtime boundary. Không audit lại toàn repo.
- Gate PASS không bị ảnh hưởng giữ kết quả và SHA gốc, thêm lý do carry-forward cho HEAD mới. Không đổi SHA evidence như thể đã chạy lại.
- Reopen khi delta ảnh hưởng source/contract/data/dependency/giả định của gate, base mới đổi tích hợp, AC/rule đổi, evidence không còn đại diện hoặc check mới fail. Dùng UNVERIFIED khi cần chạy lại, BLOCKED khi có trở ngại; giữ lịch sử PASS.
- HEAD mới luôn vô hiệu overall PR Readiness PASS cũ cho HEAD mới. Technical evidence không ảnh hưởng có thể reuse sau impact analysis; checks/review/conversations/sync phải xác nhận lại theo gate hiện hành.
- Rebase/force-push: so sánh tree SHA cũ/mới và PR patch ở baseline mới khi có; không dựa ancestry/commit count để kết luận không đổi. Nếu mất baseline hoặc impact không đáng tin cậy, ghi lý do và review lại PR scope mất evidence. Không reset history.

## Finding lifecycle

Mỗi finding có ID tăng dần F-001, F-002… trong PR, không tái sử dụng/xóa. Ghi vấn đề, expected, actual, evidence/SHA, AC/gate, location nếu có, Blocking: YES/NO, status và transition theo round. Không thêm severity/score.

| Status | Điều kiện |
|---|---|
| OPEN | Vấn đề còn tồn tại; claim đã sửa chưa đủ evidence vẫn OPEN |
| RESOLVED | Reviewer xác minh fix trên source/evidence xác định |
| WITHDRAWN | Reviewer xác minh finding cũ sai/không còn hợp lệ; ghi reason/evidence |
| BLOCKED | Không thể xác minh vì dependency/environment/evidence; ghi điều kiện tiếp tục |

- Fix: nối delta với F-ID, verify expected behavior và regression liên quan trước RESOLVED; commit message không thay kiểm tra.
- Rebuttal: kiểm tra claim bằng source/evidence/authority. Reviewer vẫn đúng: OPEN và giải thích phần rebuttal chưa đủ. Reviewer sai: WITHDRAWN và correction công khai theo quyền comment, không xóa finding.
- Tái diễn cùng vấn đề: reopen F-ID và thêm transition. Vấn đề khác: ID mới. BLOCKED có thể chuyển OPEN/RESOLVED/WITHDRAWN khi có evidence.
- Blocking OPEN/BLOCKED ngăn PASS. Nonblocking cần lý do; Blocking NO không miễn required AC/gate.

## Hai verdict độc lập

**PR Readiness: PASS / FAIL / BLOCKED / UNVERIFIED**, gắn exact HEAD/base snapshot.

- PASS chỉ khi PR còn mở, không draft, hướng tới develop, có thể merge theo rule hiện hành và mọi required gate đạt (hoặc NOT_APPLICABLE có căn cứ): technical, human approval, required checks, required conversations, branch sync.
- FAIL khi required gate fail/finding blocking đã chứng minh; BLOCKED khi thiếu điều kiện đã biết (approval/check chưa xong, conflict, môi trường); UNVERIFIED khi chưa đánh giá được. Nếu cùng tồn tại, nêu tất cả và dùng FAIL trước BLOCKED trước UNVERIFIED.
- Đối chiếu Ruleset thực tế với policy repo; enforcement chưa bật không miễn policy. AI/advisory không thay human approval. CI synthetic merge SHA phải map về đúng HEAD/base.
- Đọc lại HEAD/base/check/review state trước công bố; snapshot đổi thì giữ evidence cũ nhưng không PASS HEAD mới. Sau merge, giữ verdict lịch sử và chuyển Issue acceptance, không gán readiness mới cho PR đã đóng.

**Issue Acceptance: PENDING / PASS / FAIL / BLOCKED / UNVERIFIED**.

- Sau PR PASS/merge develop: PENDING. Lưu merge commit và release/main reference.
- Chỉ PASS khi toàn scope/AC hiện diện trên exact main commit (kể cả mapping squash/rebase), demo local sau merge đạt, Tech Lead/DoD xác nhận đầy đủ. Main delta liên quan phải đánh giá lại; PR evidence không tự chứng minh main.
- Main/demo fail: FAIL; dependency chặn: BLOCKED; thiếu thông tin verification: UNVERIFIED. Không giảm scope để đạt Done.
- PASS không tự đóng Issue/update Project. Work phát sinh sau Issue đã nghiệm thu theo follow-up Issue/PR trong CONTRIBUTING; không viết lại lịch sử hoàn thành.

## Kết quả

Một live report theo Workflow, không report mỗi round. Comment tự chứa finding/action cho developer vì report local bị ignore. Không thêm database/state machine, dashboard, QA framework, automation hoặc CI/CD.
