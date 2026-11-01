---
id: session-consent-budget
title: Quản lý phiên hội thoại, consent dữ liệu và hạn mức AI
category: playbooks
source_requirements:
  - FR-66
  - BR-03
  - BR-04
  - BR-77
  - NFR-18
  - NFR-22
implementation_status: verified
status: APPROVED
---

# Quản lý phiên hội thoại, consent dữ liệu và hạn mức AI

Tài liệu này chuẩn hóa quy trình quản lý phiên hội thoại, cơ chế đồng thuận dữ liệu người dùng (consent) và chính sách kiểm soát hạn mức sử dụng AI trên Mâm Xanh theo FR-66, BR-03/04, BR-77, NFR-18 và NFR-22.

## Quản lý phiên hội thoại và phân tách phạm vi consent

Dữ liệu riêng tư của người dùng thuộc các phân hệ nghiệp vụ cụ thể (như danh sách mua sắm, thông tin dinh dưỡng hoặc thực đơn cá nhân) đòi hỏi sự đồng thuận rõ ràng (consent) của người dùng trước khi gửi tới mô hình AI để xử lý hoặc tạo vector truy xuất (query embedding).

- **Phân tách trách nhiệm:** Consent dữ liệu hoàn toàn độc lập với bước xác nhận hành động ghi (`CONFIRM`). Consent cho phép AI đọc và phân tích dữ liệu; xác nhận cho phép AI thực thi ghi thay đổi.
- **Thu hồi consent:** Khi người dùng thu hồi consent, hệ thống lập tức chặn các lần gửi dữ liệu tiếp theo, hủy bỏ ngay các yêu cầu xác nhận hành động (`PendingConfirmation`) đang chờ duyệt trong phạm vi đó và dọn dẹp ngữ cảnh dữ liệu nhạy cảm khỏi phiên hội thoại.
- **Vòng đời phiên hội thoại (Session Lifecycle):** Phiên hội thoại AI tự động hết hạn sau 30 phút không hoạt động (idle timeout). Mỗi phiên được gán cố định với một engine điều phối và xử lý tuần tự từng lượt hội thoại. Khi phiên đã hết hạn hoặc bị hủy sau khi hệ thống khởi động lại, Backend từ chối tiếp tục các thao tác dở dang và không tự ý khôi phục phiên từ session ID do client gửi lên.

## Kiểm soát hạn mức, quota và AI Credit

Backend kiểm tra trạng thái hoạt động của tài khoản, danh sách mô hình được phép theo gói thành viên và số dư khả dụng trước từng lượt gọi mô hình AI.

- **Snapshot hạn mức:** Trạng thái hạn mức được ghi nhận snapshot ngay tại thời điểm bắt đầu lượt gọi (chu kỳ, model, tỷ lệ khấu trừ) để đảm bảo tính nhất quán của chu kỳ tính phí, không phụ thuộc vào thời điểm lượt gọi hoàn tất.
- **Thứ tự khấu trừ:** Hệ thống ưu tiên trừ quota được cấp định kỳ trong tháng trước; khi quota cạn kiệt, hệ thống tiếp tục khấu trừ AI Credit theo cấu hình chính sách của hệ thống. Mọi giao dịch khấu trừ credit được thực hiện trong giao dịch cơ sở dữ liệu an toàn kèm cơ chế khử trùng lặp (deduplication) theo mã định danh lượt gọi (call ID). Số dư credit không bao giờ âm; các lượt gọi thất bại do lỗi hệ thống hoặc phản hồi sai định dạng sẽ không bị trừ quota hay credit.
- **Dừng an toàn trước thao tác ghi:** Ngay trước khi kích hoạt công cụ ghi làm thay đổi dữ liệu, Backend tái kiểm tra số dư hạn mức. Nếu cả quota và credit đều đã cạn kiệt, hệ thống dừng an toàn ngay trước bước ghi và thông báo cho người dùng bổ sung hạn mức.

## Vận hành hệ thống và giới hạn thời gian (Timeouts)

- **Chi phí vận hành hệ thống:** Thao tác tạo chỉ mục tri thức (indexing) và tra cứu tri thức tĩnh phục vụ hỗ trợ người dùng là chi phí vận hành của nền tảng, không tính vào hạn mức hay credit cá nhân của người dùng.
- **Thời gian chờ và thử lại (Timeouts & Retries):** Mỗi lần gọi mô hình AI có thời hạn tối đa 10 giây, hỗ trợ tối đa 1 lần thử lại (retry) khi gặp lỗi kết nối tạm thời (502/503/504). Lượt tạo vector truy xuất (query embedding) có thời hạn 10 giây; khi embedding gặp sự cố, hệ thống tự động suy thoái an toàn sang tìm kiếm theo từ khóa (Keyword Fallback) kèm đánh dấu trạng thái suy thoái (`degraded`), đảm bảo dịch vụ không bị gián đoạn hoàn toàn.
