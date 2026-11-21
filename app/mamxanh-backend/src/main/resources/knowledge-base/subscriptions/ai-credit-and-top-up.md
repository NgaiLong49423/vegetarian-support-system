---
id: ai-credit-and-top-up
title: AI Credit và hướng dẫn nạp thêm ngân sách
category: subscriptions
audience:
  - customer
  - expert
source_requirements:
  - FR-63
  - FR-64
  - BR-77
source_files:
  - app/mamxanh-frontend/src/pages/AiCreditTopUp.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/subscription/service/AiTokenQuotaService.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/subscription/controller/SubscriptionController.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/admin/service/AdminAiConfigurationService.java
implementation_status: verified
last_verified: 2026-11-21
---

# AI Credit và hướng dẫn nạp thêm ngân sách

## Chức năng này dùng để làm gì?

Khi bạn đang ở gói Free hoặc Plus nhưng sử dụng nhiều tính năng AI đến mức dùng hết số token được cấp trong tháng, bạn không bắt buộc phải nâng cấp lên gói Pro đắt đỏ.

Mâm Xanh cung cấp ví **AI Credit** tại trang `/nap-ai-credit`. AI Credit là nguồn ngân sách dự phòng linh hoạt, giúp các cuộc trò chuyện và tác vụ gợi ý thực đơn của bạn tiếp tục diễn ra liền mạch mà không bị gián đoạn.

## Quy tắc quy đổi và thanh toán vượt mức (Overage)

1. **Tỷ lệ quy đổi và giới hạn nạp:**
   - Tỷ lệ VNĐ sang AI Credit, mức nạp tối thiểu và tối đa được quản trị viên cấu hình.
   - Xem số Credit dự kiến và giới hạn đang áp dụng trực tiếp tại trang nạp; không dựa vào con số trong ví dụ hoặc tài liệu cũ.
2. **AI Credit KHÔNG BAO GIỜ HẾT HẠN:**
   - Khác với hạn mức token của gói sẽ hết hạn vào cuối chu kỳ 30 ngày, **số dư AI Credit trong ví của bạn được lưu giữ vĩnh viễn** trong tài khoản cho đến khi bạn sử dụng hết.
3. **Tỷ lệ trừ phí khi vượt hạn mức gói (Overage Rate):**
   - Số Credit tiêu thụ cho token vượt quota được tính theo tỷ lệ do quản trị viên cấu hình. Tỷ lệ hiện tại được hiển thị trong phần cài đặt/quyền lợi AI; không thể khẳng định một mức cố định cho mọi thời điểm hay model.

## Thứ tự tiêu thụ token thông minh

Hệ thống luôn ưu tiên bảo vệ túi tiền của bạn theo thứ tự:
1. **Quota trước, Credit sau:** Hệ thống áp dụng quota của gói trước; AI Credit có thể được dùng cho phần vượt quota theo chính sách cấu hình hiện hành.
2. **Không để số dư âm (Bảo vệ tài khoản):**
   - Nếu quota và số dư Credit không đủ, yêu cầu AI có thể bị từ chối. Mức trừ cuối cùng phụ thuộc kết quả ghi nhận usage và cấu hình tại thời điểm xử lý.

## Các bước nạp AI Credit

1. Đăng nhập vào Mâm Xanh, vào trang **Nạp AI Credit** (`/nap-ai-credit`).
2. Nhập số tiền trong giới hạn được hiển thị tại trang nạp.
3. Màn hình sẽ hiển thị số Credit bạn sẽ nhận được tương ứng. Bấm nút **Thanh toán ngay**.
4. Hệ thống sẽ mở cổng thanh toán bảo mật **payOS** và hiển thị mã VietQR.
5. Mở ứng dụng ngân hàng của bạn, quét mã QR và xác nhận chuyển khoản đúng số tiền và nội dung.
6. Khi hệ thống xác nhận thanh toán, số AI Credit theo snapshot đơn hàng được cộng vào tài khoản.

## Những điều cần lưu ý

1. **Trạng thái thanh toán:** Nếu giao dịch chưa được xác nhận hoặc có vấn đề, kiểm tra trạng thái trong lịch sử giao dịch và làm theo hướng dẫn hỗ trợ đang hiển thị. Không chuyển khoản vào đơn đã hết hạn.
