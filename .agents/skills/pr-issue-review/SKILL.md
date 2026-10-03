---
name: pr-issue-review
description: 'Review a Mâm Xanh implementation PR and its Issue across multiple rounds: trace SRS acceptance criteria to code and evidence, verify fixes and rebuttals, review SHA deltas, determine merge readiness, and continue Issue acceptance on main. Use for initial PR review, resumed review, developer rebuttal, or post-merge acceptance; not for implementing an Issue or a general repository audit.'
---

# PR / Issue Review

## Phạm vi và authority

Skill sở hữu phương pháp review; [review-pr-and-accept-issue](../../workflows/review-pr-and-accept-issue.md) sở hữu sequencing, checkout, live report và giao tiếp. Không tự sửa implementation để làm finding biến mất.

- Đọc phần liên quan trong `AGENTS.md`, `CONTRIBUTING.md`, `.agents/POLICY.md`, `.agents/repo-contract.yml`, `docs/README.md`, `docs/testing/TEST-STRATEGY.md`, PR template hiện hành. Ghi revision của rule; tooling chưa merge không tự thay authority trên develop của PR khác.
- SRS root xác định current requirement IDs; child SRS đã đăng ký sở hữu FR/BR/UC/AC chi tiết. Issue quản lý công việc, không định nghĩa lại SRS. Nêu mâu thuẫn và chặn đúng gate phụ thuộc; không biến thiếu dependency ngoài Issue thành implementation defect nếu scope hiện tại không chịu trách nhiệm.
- Đọc động coverage, commands, required checks, review/branch-sync gates, Definition of Done và Ruleset/check state. Không hard-code threshold thành authority mới. Không đọc được GitHub state thì ghi UNVERIFIED.
- Route Backend/API/database tới [mamxanh-backend-development](../mamxanh-backend-development/SKILL.md); Frontend/UI/Playwright tới [mamxanh-frontend-development](../mamxanh-frontend-development/SKILL.md). Các skill đó sở hữu kiểm tra kỹ thuật; không sao chép checklist vào đây.
- Reuse [bug-recording](../bug-recording/SKILL.md) cho bug có evidence theo policy; liên kết BUG-ID đã có thay vì tạo trùng. F-ID vẫn thuộc review PR. Không tự tạo Bug Issue, sửa bug hoặc mở rộng scope.
- Tuân thủ authorization và read-only của task. Skill không cấp quyền GitHub mutation, merge, close/reopen Issue, update Project hoặc resolve conversation. Agent được đề xuất GitHub Review action sau actual review nhưng chỉ được submit qua quy trình authorization chính xác trong Workflow.

## Scope, traceability và pass criteria

1. Xác nhận PR/Issue/base; target khác develop phải dùng gate tương ứng, không gán PASS develop cho release PR.
2. Initial review đọc PR diff từ merge-base tới exact HEAD, cùng base tip/integration assumptions. Trace `Issue → FR/BR/UC/AC → files/diff → tests/evidence`. Test/file tồn tại không chứng minh AC đạt.
3. Chọn gate theo scope/boundary thật: code, Backend, Frontend, API, migration, integration/provider, Docker Development, CI, human review, required conversations, branch sync. NOT_APPLICABLE phải có lý do.
4. Evidence ghi source SHA, môi trường, command/check URL, kết quả quan sát được và AC/gate chứng minh. Phân biệt developer evidence với reviewer tự tái hiện. Artifact thiếu provenance không được ghi PASS.

## Pre-Review Readiness

Trước actual code review, xác minh PR đạt Ready for Review theo authority hiện hành. Chỉ dùng Pre-Review Readiness `PASS` để bắt đầu actual review. Gate gồm:

- PR target/base đúng nhánh theo governance; xác minh current PR HEAD và latest `develop` SHA, thời điểm cần sync, và liệu baseline mới có ảnh hưởng hay không.
- Kiểm tra Git conflict và semantic/integration conflict có thể nhận diện từ delta với `develop` (ví dụ file giao nhau, API contract, cấu hình chung, thứ tự migration); preflight không thay thế actual code review.
- Xác minh evidence Compose local từ đúng PR HEAD khi CONTRIBUTING yêu cầu và Docker khả dụng. Nếu verification bắt buộc nhưng thiếu bằng chứng hợp lệ hoặc không thể thực hiện, readiness là `BLOCKED`.
- Required GitHub `Docker Development` chỉ đạt khi check hoàn tất `PASS` trên đúng current PR HEAD. `PENDING`, `MISSING`, `FAIL`, SHA khác hoặc không xác minh được đều làm `Pre-Review Readiness = BLOCKED`, không bắt đầu actual review. Local Compose evidence bắt buộc không được thay bằng check GitHub.

Nếu bất kỳ điều kiện nào chưa đạt, ghi snapshot, trạng thái và điều kiện unblock vào cặp report hiện có; ghi rõ actual review chưa bắt đầu và không tạo findings. Khi developer yêu cầu review lại sau `BLOCKED`, chạy lại toàn bộ gate từ đầu trên current HEAD và current `develop` baseline, không chỉ kiểm tra điều kiện từng bị block. Khi chuyển sang Issue Acceptance trên `main`, chỉ bắt đầu actual acceptance checks nếu report chứng minh PR liên kết đã qua Pre-Review Readiness trên HEAD được merge; nếu thiếu hoặc bị BLOCKED thì dừng actual acceptance review và báo điều kiện cần làm rõ.

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

## Ngôn ngữ, cảnh báo và vai trò con người

- Toàn bộ artifact review và PR comment/draft được tạo cho thành viên đọc phải dùng tiếng Việt làm ngôn ngữ chính. Giữ nguyên technical identifier, status, tên API/class/function, command, path, SHA, GitHub check và error message cần trích dẫn; phần giải thích quanh chúng vẫn viết tiếng Việt.
- Dòng đầu của REVIEW-PLAN.md và ACCEPTANCE-REVIEW.md phải là notice: “Tài liệu này do Agent tạo để hỗ trợ review; nội dung và bằng chứng quan trọng cần được con người kiểm tra, xác nhận trước khi dùng làm quyết định cuối cùng.”
- Mọi PR comment/draft phải đặt cảnh báo gần đầu: “Lưu ý: Đây là kết quả review do Agent hỗ trợ thực hiện. Agent có thể nhận định sai hoặc bỏ sót; không nên xem kết quả là kết luận tuyệt đối. Người được review và reviewer cần tự kiểm tra finding, evidence và kết quả kiểm thử trước khi xác nhận hoặc thay đổi. Quyết định cuối cùng thuộc về con người.”
- Tách bốn trạng thái: Agent assessment, Human confirmation, GitHub required approval và Merge authorization. Agent PASS không phải xác nhận của human, GitHub approval, hay quyền merge.
- Agent có thể đề xuất `APPROVE`, `REQUEST_CHANGES` hoặc GitHub Review `COMMENT` sau khi hoàn tất actual review và report. Không submit trước authorization rõ ràng của user sau khi xem preview đầy đủ. Authorization chỉ áp dụng cho đúng PR number, reviewed HEAD SHA, exact action và nội dung đã preview.
- Ngay trước submit, đọc lại current HEAD. Nếu khác reviewed HEAD hoặc preview thay đổi, authorization hết hiệu lực; không submit. Chạy review lại theo lifecycle trên snapshot mới và xin authorization mới. Submit đúng action được duyệt rồi xác minh remote result. `APPROVE` của Agent không phải Human confirmation hoặc required approval của human theo Ruleset, và không phải Merge authorization; không merge.
- Không tạo link giả đến local output. Trả đường dẫn tuyệt đối hai file trong chat; người dùng tự đính kèm chúng trong comment GitHub riêng. Agent không upload/publish report.

## Phân loại dependency và artifact

- Implementation defect: code trong scope hiện tại vi phạm AC/contract đã xác định; tạo finding có actual behavior và evidence gắn SHA.
- Outstanding dependency: capability thuộc project/Issue khác chưa có. Ghi dependency và tác động; gate phụ thuộc là BLOCKED nếu có trở ngại đã biết, UNVERIFIED nếu chưa thu được evidence. Không tự tạo finding blocking trừ khi Issue hiện tại giao trách nhiệm hoặc implementation vi phạm contract của dependency.
- Verification limitation: môi trường, provider, credential hoặc runtime không sẵn có. Ghi gate, boundary chưa kiểm tra và điều kiện unblock; không biến sự thiếu evidence thành defect.
- Phân loại artifact là acceptance scope, bắt buộc đồng bộ theo authority, informational hoặc explicitly deferred. Chỉ nhóm thuộc acceptance/authority mới có thể tác động gate; ghi căn cứ và lý do. Informational/deferred không tự block PR. Protected docs/diagrams vẫn read-only; không sửa khi không có authorization cụ thể.
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
| OPEN | Có evidence cho thấy vấn đề trong scope còn tồn tại; rebuttal/fix chưa xác minh |
| RESOLVED | Reviewer xác minh fix trên source/evidence xác định |
| WITHDRAWN | Reviewer xác minh finding cũ sai hoặc không còn hợp lệ; ghi lý do/evidence |

BLOCKED là trạng thái của gate/evidence, không phải finding mới. Thiếu dependency hoặc môi trường chỉ làm gate BLOCKED/UNVERIFIED. Finding cũ mang BLOCKED được giữ nguyên lịch sử rồi chuyển sang trạng thái phù hợp khi có evidence; không xóa transition.

- Fix: nối delta với F-ID, verify expected behavior và regression liên quan trước RESOLVED; commit message không thay kiểm tra.
- Rebuttal: kiểm tra claim bằng source/evidence/authority. Reviewer vẫn đúng: OPEN và giải thích phần rebuttal chưa đủ. Reviewer sai: WITHDRAWN và correction công khai theo quyền comment, không xóa finding.
- Tái diễn cùng vấn đề: reopen F-ID và thêm transition. Vấn đề khác: ID mới.
- Finding OPEN với Blocking YES ngăn PASS. Blocking NO cần lý do; không miễn required AC/gate.

## Hai verdict độc lập

**PR Readiness: PASS / FAIL / BLOCKED / UNVERIFIED**, gắn exact HEAD/base snapshot.

- PASS chỉ khi PR còn mở, không draft, hướng tới develop, có thể merge theo rule hiện hành và mọi required gate đạt (hoặc NOT_APPLICABLE có căn cứ): technical, human approval, required checks, required conversations, branch sync. Thiếu gate bắt buộc thì không PASS.
- FAIL khi required gate fail/finding blocking đã chứng minh; BLOCKED khi thiếu điều kiện đã biết (approval/check chưa xong, conflict, môi trường); UNVERIFIED khi chưa đánh giá được. Nếu cùng tồn tại, nêu tất cả và dùng FAIL trước BLOCKED trước UNVERIFIED.
- Đối chiếu Ruleset thực tế với policy repo; enforcement chưa bật không miễn policy. AI/advisory không thay human approval.
- Ghi riêng Human confirmation (PENDING / CONFIRMED / REJECTED), trạng thái GitHub required approval (PENDING / APPROVED / CHANGES_REQUESTED / NOT_REQUIRED / UNVERIFIED), và Merge authorization (NOT_AUTHORIZED / AUTHORIZED_FOR_THIS_ACTION). Không suy ra trạng thái nào từ Agent assessment; merge luôn cần authorization riêng và skill này không thực hiện merge. CI synthetic merge SHA phải map về đúng HEAD/base.
- Đọc lại HEAD/base/check/review state trước công bố; snapshot đổi thì giữ evidence cũ nhưng không PASS HEAD mới. Sau merge, giữ verdict lịch sử và chuyển Issue acceptance, không gán readiness mới cho PR đã đóng.

**Issue Acceptance: PENDING / PASS / FAIL / BLOCKED / UNVERIFIED**.

- Sau PR PASS/merge develop: PENDING. Lưu merge commit và release/main reference.
- Chỉ PASS khi toàn scope/AC hiện diện trên exact main commit (kể cả mapping squash/rebase), demo local sau merge đạt, Tech Lead/DoD xác nhận đầy đủ. Main delta liên quan phải đánh giá lại; PR evidence không tự chứng minh main.
- Main/demo fail: FAIL; dependency chặn: BLOCKED; thiếu thông tin verification: UNVERIFIED. Không giảm scope để đạt Done.
- PASS không tự đóng Issue/update Project. Work phát sinh sau Issue đã nghiệm thu theo follow-up Issue/PR trong CONTRIBUTING; không viết lại lịch sử hoàn thành.

## Kết quả

Mỗi PR có đúng hai live artifact local do workflow duy trì: REVIEW-PLAN.md và ACCEPTANCE-REVIEW.md. Comment tiếng Việt là summary, có Agent Review notice, verdict/state tách biệt và action cho developer; report bị ignore nên không tạo link local giả. Workflow trả đường dẫn file trong chat để người dùng tự đính kèm bằng comment riêng. Không thêm database/state machine, dashboard, QA framework, automation hoặc CI/CD.
