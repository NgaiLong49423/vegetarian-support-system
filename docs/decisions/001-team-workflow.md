> **Document:** Team Workflow Decision 001  
> **File:** `docs/decisions/001-team-workflow.md`  
> **Version:** v2.0.0
> **Created:** 2026-09-07  
> **Last Updated:** 2026-09-19
> **Status:** Active  

# Quyết Định 001: Workflow Làm Việc Nhóm

## Bối cảnh và lịch sử

Workflow ban đầu được chọn khi chưa xác định Tech Lead: tích hợp trên `develop`, đưa bản demo theo tuần lên `main`. Baseline trước ngày 2026-09-18 yêu cầu approval ở `develop`, hai approval cho release, task tối đa 5 SP/4–5 ngày và dùng closing keywords khi merge `main`.

Ngày 2026-09-18, decision-maker xác nhận nhịp giao việc theo tuần, Tech Lead điều phối, cổng tích hợp nhẹ và cổng demo bắt buộc. Những quy tắc cũ nêu trên được thay thế; đây là lịch sử, không phải yêu cầu hiện hành.

## Quyết định hiện hành

[CONTRIBUTING.md](../../CONTRIBUTING.md#workflow-làm-việc-nhóm) là Source of Truth cho toàn bộ quy tắc thực thi: nhận việc, SP/deadline, PR, review, bug và nghiệm thu. ADR này ghi lý do, không sao chép checklist.

Giữ `branch -> develop -> main` và board năm trạng thái. Đầu tuần nhận việc; cuối tuần Tech Lead chọn phần ổn định đưa lên `main`, không yêu cầu xong cả module hoặc merge bằng mọi giá. `Done` cần kiểm tra demo local sau merge, không chỉ merge. Tạm thời chưa deploy/CD; Azure vẫn là định hướng công nghệ khi ứng dụng đủ ổn định.

## Các lựa chọn và lý do

- Merge `develop` là `Done`: đã xem xét và không chọn, vì board cần phản ánh phiên bản đủ điều kiện demo, bao gồm kiểm tra tích hợp.
- Hoàn tất cả module mới lên `main`: đã xem xét và không chọn; nhịp tuần theo phạm vi ổn định cho phép demo từng phần.
- Deploy tự động mỗi đợt thay đổi: tạm thời không chọn; ưu tiên local và kiểm tra trước khi nhóm triển khai Azure.
- Giữ approval bắt buộc ở cả hai nhánh: thay bằng review tùy chọn ở `develop`, approval độc lập và CI bắt buộc ở `main` để giảm thủ tục tích hợp nhưng giữ cổng demo.

## Hệ quả và giới hạn

Cổng `develop` nhẹ có thể để lỗi tích hợp xuất hiện ở nhánh này; Tech Lead và owners cần kiểm tra kỹ phạm vi trước khi đưa lên `main`. Required checks phải chạy trên commit mới nhất, và kiểm tra manual bổ sung cho các hành vi chưa tự động hóa. Không coi pipeline xanh là chứng minh toàn bộ nghiệp vụ đúng.

Cổng CI build/test đã chốt chưa đồng nghĩa được triển khai: xem trạng thái công cụ trong CONTRIBUTING.md. Không thay đổi pipeline, branch protection hoặc automation bằng việc cập nhật ADR.

## Nguồn liên quan

- [Quyết định về phối hợp nhóm](002-five-member-team-operating-agreement.md)
- [Nguồn và phân loại bằng chứng](WORKFLOW-SOURCES.md)

Bot nhắc deadline và thay đổi cấu hình GitHub cần quyết định/ủy quyền riêng khi thực hiện.
