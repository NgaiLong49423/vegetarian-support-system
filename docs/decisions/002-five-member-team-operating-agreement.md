> **Document:** Five-Member Team Operating Agreement  
> **File:** `docs/decisions/002-five-member-team-operating-agreement.md`  
> **Version:** v1.1.0  
> **Created:** 2026-09-12  
> **Last Updated:** 2026-09-12  
> **Status:** Active  
> **Related Docs:** `CONTRIBUTING.md`, `docs/decisions/001-team-workflow.md`  

# Quyết Định 002: Quy Ước Vận Hành Nhóm 5 Thành Viên

## Mục tiêu

Thiết lập cách phối hợp đủ rõ cho nhóm SWP391 gồm 5 người, giảm phụ thuộc vào nhắn tin riêng và bảo đảm mỗi thay đổi có owner, reviewer, bằng chứng kiểm tra và người hỗ trợ khi cần.

## 1. Trách nhiệm, không phải chức danh cố định

- Nhóm có đúng 5 thành viên; tên và GitHub username được quản lý trong GitHub Team/Project sau khi xác nhận.
- Mỗi Issue có đúng một `Owner` chịu trách nhiệm tiến độ và một `Reviewer/Backup` biết đủ ngữ cảnh để hỗ trợ. Reviewer không thay owner thực hiện toàn bộ task.
- Mỗi tuần chỉ định một `Release Coordinator` trong 5 người. Trách nhiệm này luân phiên: kiểm tra board, gom release scope và điều phối release PR; không có quyền bỏ qua review hoặc quality gate.
- Product/scope, frontend, backend, database/integration và quality/documentation là các vùng trách nhiệm cần được phủ, nhưng một người có thể phụ trách nhiều vùng và phải có ít nhất một người backup cho vùng rủi ro cao.

## 2. Giới hạn công việc và cập nhật tiến độ

- Mỗi thành viên mặc định chỉ có một Issue ở `In Progress`. Nhận thêm việc chỉ khi task hiện tại bị blocker có ghi nhận hoặc nhóm đồng ý rõ.
- Owner cập nhật Issue khi bắt đầu, khi có thay đổi phạm vi, khi bị blocker và trước Target Date. Cập nhật phải nêu: phần đã xong, phần còn lại, bằng chứng hiện có và trợ giúp cần thiết.
- Blocker phải được ghi trên Issue trong ngày phát hiện và gắn label `⛔ Blocked`; không chỉ báo qua tin nhắn riêng.
- Nếu owner không thể tiếp tục, nhóm reassign công khai trên Issue và ghi phần bàn giao; không kết luận “không làm” chỉ vì thiếu dữ liệu GitHub.

## 3. Review và quyền merge

- Tác giả không tự approve PR của mình. PR vào `develop` cần tối thiểu một approval từ thành viên khác.
- Thay đổi authentication/authorization, database schema/migration, API dùng chung, cấu hình secret/cloud hoặc cấu trúc chung cần hai người kiểm tra, trong đó ít nhất một người hiểu vùng bị ảnh hưởng.
- Release PR vào `main` cần hai approval và toàn bộ release checklist trong `CONTRIBUTING.md`.
- Reviewer kiểm tra scope, Acceptance Criteria, failure cases, security, test evidence và tài liệu liên quan; không chỉ kiểm tra format.
- Yêu cầu sửa phải cụ thể và có căn cứ. Cuộc thảo luận quan trọng được resolve sau khi người nêu yêu cầu xác nhận hoặc khi nhóm ghi quyết định thay thế.

## 4. Ra quyết định

| Loại quyết định | Cách xử lý |
|---|---|
| Thực hiện trong scope Issue đã duyệt | Owner quyết định chi tiết triển khai, reviewer kiểm tra |
| Thay đổi yêu cầu, public API, schema, kiến trúc hoặc core dependency | Tạo Decision Issue/ADR, nêu phương án và ảnh hưởng; cần ít nhất 3/5 thành viên đồng ý |
| Thay đổi workflow/Definition of Done | Cập nhật ADR và `CONTRIBUTING.md`; cần ít nhất 3/5 thành viên đồng ý |
| Xử lý blocker khẩn cấp trước demo | Release Coordinator triệu tập quyết định; vẫn phải ghi lại Issue/ADR và không bỏ qua bảo mật |
| Không đạt đồng thuận | Giữ baseline hiện tại, thu thập thêm bằng chứng hoặc xin quyết định từ giảng viên/mentor khi thuộc yêu cầu môn học |

Không dùng phiếu bầu để hợp thức hóa thay đổi trái yêu cầu môn học, làm lộ secret hoặc bỏ qua kiểm tra bắt buộc.

## 5. Nhịp phối hợp tối thiểu

- Trước mỗi chu kỳ: refinement ngắn để xác nhận Source Trace, Acceptance Criteria, dependency, owner, reviewer, Story Points và Target Date.
- Trong chu kỳ: cập nhật bất đồng bộ trên Issue; nội dung ảnh hưởng quyết định không chỉ tồn tại trong chat.
- Trước release: kiểm tra board, build/test trên commit mới nhất, demo flow, database/documentation và danh sách Issue đóng.
- Sau demo/release: retrospective ngắn với ba câu hỏi: điều gì hiệu quả, điều gì gây chậm/sai, thay đổi quy ước nào cho chu kỳ sau.

Thời gian họp cụ thể do nhóm thống nhất theo lịch học; tài liệu này không tự đặt giờ hoặc kênh liên lạc.

## 6. Bằng chứng đóng góp

Đóng góp được nhìn qua Issue ownership, commit/PR, review có nội dung, test, tài liệu, nghiên cứu có nguồn và phần demo. Story Points hỗ trợ lập kế hoạch, không phải điểm cá nhân. AI-generated output phải được người thực hiện kiểm tra; PR ghi rõ AI đã hỗ trợ phần nào và bằng chứng xác minh khi có ảnh hưởng đáng kể.

## 7. Điều kiện rà soát lại

Rà soát thỏa thuận khi thành viên thay đổi, yêu cầu môn học thay đổi, workflow gây tắc nghẽn lặp lại hoặc repository đã có CI/branch protection khác với giả định hiện tại.

## 8. Progress Tracking and Repository Placement

GitHub Issues/Projects are the source of truth for ownership, dates, status and blockers; linked PRs carry verification and review evidence. Members do not write individual or weekly progress reports in the repository. Capture durable technical decisions in ADRs only when useful.

Use [the document register](../README.md) to place new files. New agent sessions start with [AGENTS.md](../../AGENTS.md). Unregistered temporary files are excluded from default reading and metadata audits.
