---
id: ai-execution-modes
title: Ba chế độ thực thi của AI và bảo vệ quyền riêng tư
category: ai-features
audience:
  - customer
  - expert
source_requirements:
  - FR-66
  - BR-79
  - NFR-20
  - NFR-22
source_files:
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/service/AiAssistantExecutionService.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/service/AiAssistantToolRegistry.java
  - app/mamxanh-frontend/src/pages/AiChatPage.tsx
implementation_status: verified
last_verified: 2026-11-01
---

# Ba chế độ thực thi của AI và bảo vệ quyền riêng tư

## Chức năng này dùng để làm gì?

Trợ lý AI Mâm Xanh không chỉ trả lời văn bản thông thường mà còn có khả năng hỗ trợ bạn thực hiện các thao tác tiện ích trong ứng dụng (như mở trang chức năng, xem danh sách đi chợ, thêm món vào thực đơn, điều chỉnh sở thích ăn uống).

Để bạn luôn là người nắm quyền kiểm soát dữ liệu cá nhân cao nhất và không bao giờ bị bất ngờ bởi các thay đổi tự động ngoài ý muốn, Mâm Xanh thiết kế **3 Chế độ thực thi (Execution Modes)** và quy chuẩn **Đồng ý chia sẻ dữ liệu (Data Consent)** minh bạch.

## 3 Chế độ thực thi của AI

Bạn có thể dễ dàng chuyển đổi chế độ thực thi ngay tại thanh công cụ của khung chat:

### 1. Chế độ Hỏi đáp / Gợi ý (QA Mode)
- **Mục tiêu:** Chỉ đóng vai trò cung cấp thông tin, giải đáp kiến thức và đưa ra lời khuyên.
- **Hành vi:** AI có thể điều hướng bạn đến các trang thông tin hoặc điền nội dung vào biểu mẫu để bạn xem thử trên màn hình, nhưng **tuyệt đối không bao giờ gửi lệnh ghi hoặc lưu dữ liệu vào hệ thống**.
- **Phù hợp khi:** Bạn chỉ muốn tìm hiểu, hỏi han mà không muốn bất kỳ dữ liệu nào bị thay đổi.

### 2. Chế độ Có xác nhận (CONFIRM Mode) — *Chế độ mặc định*
- **Mục tiêu:** Cân bằng hoàn hảo giữa tính tiện lợi và sự an toàn.
- **Hành vi:** Khi bạn yêu cầu AI thực hiện một hành động thay đổi dữ liệu (ví dụ: *"Tạo danh sách đi chợ cho món này"* hoặc *"Cập nhật tên hiển thị của tôi thành Bếp Chay Xanh"*):
  1. AI sẽ lập một kế hoạch hành động và hiển thị một **Bản xem trước (Preview)** rõ ràng kèm hai nút bấm: **Đồng ý (Approve)** và **Từ chối (Reject)**.
  2. **Chỉ khi bạn nhấp vào nút "Đồng ý"**, hành động đó mới chính thức được thực hiện trên hệ thống.
  3. Nếu bạn bấm "Từ chối" hoặc không phản hồi trong 5 phút, kế hoạch sẽ tự động bị hủy bỏ.
- **Phù hợp khi:** Dành cho hầu hết mọi người dùng, đảm bảo bạn luôn biết chính xác điều gì sắp diễn ra trước khi dữ liệu được lưu.

### 3. Chế độ Tự động (AUTO Mode)
- **Mục tiêu:** Trải nghiệm rảnh tay tối đa cho người dùng thành thạo.
- **Hành vi:** AI được phép tự động thực thi các yêu cầu thay đổi dữ liệu an toàn ngay khi bạn ra lệnh rõ ràng (ví dụ: đánh dấu một món trong danh sách đi chợ đã mua), mà không cần thêm bước hỏi xác nhận trung gian.
- **Ranh giới an toàn đặc biệt (Hard Safeguards):** Kể cả khi bạn đang bật chế độ Tự động, **những thao tác có rủi ro cao hoặc không thể hoàn tác (như Xóa vĩnh viễn danh sách đi chợ, hoặc Đăng công thức mới của Chuyên gia) BẮT BUỘC LUÔN PHẢI CÓ XÁC NHẬN RÕ RÀNG TỪ BẠN**. AI không bao giờ được tự động xóa dữ liệu hay tự động đăng bài.

## Chính sách Đồng ý chia sẻ dữ liệu (Data Consent)

Khi bạn nhờ AI đọc hoặc chỉnh sửa dữ liệu cá nhân (hồ sơ, sở thích kiêng ăn, chỉ số dinh dưỡng, danh sách đi chợ):
1. **Hỏi ý kiến trước khi gửi:** Hệ thống sẽ giải thích rõ loại dữ liệu nào sắp được gửi sang mô hình AI để xử lý và xin sự đồng ý của bạn.
2. **Quyền thu hồi tức thì:** Bạn có thể thu hồi sự đồng ý bất cứ lúc nào trong phiên trò chuyện. Khi bạn thu hồi, AI sẽ lập tức ngừng việc truy cập vào dữ liệu đó.
3. **Tuyệt đối không lưu mật khẩu hay thông tin thanh toán:** AI chỉ hỗ trợ điều hướng bạn đến trang Thanh toán hoặc Đổi mật khẩu. Bạn tự mình nhập mật khẩu và thực hiện quét mã thanh toán, AI không có quyền can thiệp hay đọc các thông tin nhạy cảm này.
