> **Document:** Five-Member Team Operating Agreement  
> **File:** `docs/decisions/002-five-member-team-operating-agreement.md`  
> **Version:** v2.1.0
> **Created:** 2026-09-12  
> **Last Updated:** 2026-09-28
> **Status:** Active  
> **Related Docs:** `CONTRIBUTING.md`, `docs/decisions/001-team-workflow.md`  

# Quyết Định 002: Quy Ước Vận Hành Nhóm 5 Thành Viên

## Bối cảnh và lịch sử

Baseline ngày 2026-09-12 dùng reviewer/backup cố định cho mỗi Issue, Release Coordinator luân phiên, mặc định một Issue `In Progress` mỗi người, approval bắt buộc ở `develop` và hai approval ở `main`.

Ngày 2026-09-18, decision-maker xác nhận Tech Lead là người điều phối đưa code lên `main`; lượng Issue giao dựa trên SP và khả năng thực tế; reviewer chọn khi cần. Những cơ chế cũ nêu trên không còn là quy tắc hiện hành. Không áp đặt lịch reviewer hoặc chức danh FE cố định.

## Quyết định và lý do

[CONTRIBUTING.md](../../CONTRIBUTING.md#workflow-làm-việc-nhóm) sở hữu quy định owner, deadline, SP, review, bug và nghiệm thu. Tài liệu này giữ lý do phối hợp, không xác lập thêm checklist merge.

- Một owner chịu trách nhiệm toàn bộ scope đã nhận, kể cả FE do AI thực hiện; AI không thay trách nhiệm xác minh của thành viên.
- Tech Lead chọn phạm vi, mở PR vào `main`, duyệt API contract, điều phối reviewer, xử lý blocker/trễ hạn và tổ chức nghiệm thu local.
- Approval ở `main` phải đến từ người khác tác giả; code do reviewer viết cần được người khác kiểm tra. Tech Lead không thay thế kiểm tra độc lập.
- Dùng SP để chọn lượng việc phù hợp đầu tuần; người nhận theo Issue, không cắt scope để vừa người. Báo trễ sớm để hỗ trợ nhưng vẫn ghi nhận cam kết ban đầu.
- Bug được giao sau xác nhận nguyên nhân; người phụ trách code liên quan là mặc định, không dùng việc giao sửa làm kết luận quy lỗi cá nhân.

## Hệ quả

Không cần thành viên FE riêng; owner phụ trách BE kiểm tra nhu cầu giao diện và dùng contract đã duyệt để hướng dẫn AI. Khả năng phối hợp và bằng chứng kiểm tra quan trọng hơn số lượng SP hoặc output AI. Nguy cơ Tech Lead thành nút thắt được giảm bằng phản hồi reviewer và báo blocker sớm theo CONTRIBUTING.md.

## Quyền quyết định ngoài workflow đã chốt

Quy tắc về thay đổi requirement, schema, kiến trúc hoặc core dependency tiếp tục phân định rõ phạm vi theo [Engineering Autonomy Policy](../../CONTRIBUTING.md#engineering-autonomy-policy):
- **Yêu cầu Decision Issue & biểu quyết 3/5 thành viên:** Chỉ áp dụng cho các thay đổi lớn cấp hệ thống (structural redesign): thêm bảng/thực thể mới, xóa bảng, split/merge bảng, tạo mới hoặc thay đổi relationship/cardinality so với baseline, thay đổi core technology stack hoặc framework cốt lõi, breaking API changes, và thay đổi ý nghĩa nghiệp vụ của SRS.
- **Quyền tự chủ kỹ thuật (Engineering Autonomy):** Developer và AI coding agent có quyền tự chủ thực hiện các thay đổi implementation cục bộ trong phạm vi Issue được giao: bổ sung cột, thêm ràng buộc CHECK hoặc Foreign Key để triển khai quan hệ đã được baseline phê duyệt, thêm auxiliary dependency (thư viện test/helper), và thiết kế endpoint/DTO non-breaking (đồng bộ tài liệu API trong cùng PR). Các thay đổi này không cần mở Decision Issue hay biểu quyết 3/5.
- **Bảo vệ Diagram Artifacts:** Thư mục `docs/diagrams/` là presentation workspace do con người duy trì; thay đổi code/schema/API không tự động cấp quyền sửa hoặc regenerate diagram cho coding agent.

Workflow hiện hành được cập nhật theo quyết định trực tiếp ngày 2026-09-18. Các thay đổi chính sách tiếp theo cần quyết định có thẩm quyền được ghi nhận; không dùng phiếu bầu để bỏ qua yêu cầu môn học, bảo mật hoặc quality gate.

## Nhịp phối hợp và bằng chứng

Nhóm refinement đầu tuần, cập nhật khi có blocker/nguy cơ trễ/thay đổi quan trọng và xem lại kết quả cuối tuần để điều chỉnh lượng việc. Issue/Project quản lý assignments, dates, status và blockers; PR giữ bằng chứng review/test. Không tạo báo cáo tiến độ cá nhân hoặc tuần trong repository.

Đóng góp gồm ownership, commit/PR, review có nội dung, test, tài liệu, nghiên cứu và demo. Rà soát quyết định khi thành viên/yêu cầu môn học thay đổi hoặc workflow gây tắc nghẽn lặp lại.

## Nguồn liên quan

- [ADR-001](001-team-workflow.md)
- [Document Register](../README.md)
- [Agent Entry Point](../../AGENTS.md)
