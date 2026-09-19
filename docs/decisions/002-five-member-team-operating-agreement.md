> **Document:** Five-Member Team Operating Agreement  
> **File:** `docs/decisions/002-five-member-team-operating-agreement.md`  
> **Version:** v2.0.0
> **Created:** 2026-09-12  
> **Last Updated:** 2026-09-19
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

Quy tắc đã có về thay đổi requirement, schema, kiến trúc hoặc core dependency vẫn giữ: nêu phương án/ảnh hưởng trong Decision Issue hoặc ADR phù hợp và cần ít nhất 3/5 thành viên đồng ý. Tech Lead duyệt contract triển khai trong scope đã xác nhận; việc duyệt không tự cho phép thay đổi business meaning, schema hoặc kiến trúc ngoài scope.

Workflow hiện hành được cập nhật theo quyết định trực tiếp ngày 2026-09-18. Các thay đổi chính sách tiếp theo cần quyết định có thẩm quyền được ghi nhận; không dùng phiếu bầu để bỏ qua yêu cầu môn học, bảo mật hoặc quality gate.

## Nhịp phối hợp và bằng chứng

Nhóm refinement đầu tuần, cập nhật khi có blocker/nguy cơ trễ/thay đổi quan trọng và xem lại kết quả cuối tuần để điều chỉnh lượng việc. Issue/Project quản lý assignments, dates, status và blockers; PR giữ bằng chứng review/test. Không tạo báo cáo tiến độ cá nhân hoặc tuần trong repository.

Đóng góp gồm ownership, commit/PR, review có nội dung, test, tài liệu, nghiên cứu và demo. Rà soát quyết định khi thành viên/yêu cầu môn học thay đổi hoặc workflow gây tắc nghẽn lặp lại.

## Nguồn liên quan

- [ADR-001](001-team-workflow.md)
- [Document Register](../README.md)
- [Agent Entry Point](../../AGENTS.md)
