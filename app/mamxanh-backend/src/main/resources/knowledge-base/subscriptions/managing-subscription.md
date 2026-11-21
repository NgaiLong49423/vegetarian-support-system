---
id: managing-subscription
title: Quản lý gói đăng ký và lịch sử giao dịch
category: subscriptions
audience:
  - customer
  - expert
source_requirements:
  - FR-13
  - FR-63
  - BR-02
  - BR-78
source_use_cases:
  - UC-13.2
  - UC-13.3
source_files:
  - app/mamxanh-frontend/src/pages/TransactionHistory.tsx
  - app/mamxanh-frontend/src/pages/PaymentResult.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/subscription/service/SubscriptionService.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/admin/service/AdminAiConfigurationService.java
implementation_status: verified
last_verified: 2026-11-21
---

# Quản lý gói đăng ký và lịch sử giao dịch

## Chức năng này dùng để làm gì?

Trang **Lịch sử giao dịch** (`/giao-dich`) và trang thông tin **Gói của tôi** giúp bạn dễ dàng theo dõi:
- Gói dịch vụ hiện tại (Free, Plus hoặc Pro) và thời hạn chu kỳ đang áp dụng.
- Lượng token bạn đã tiêu thụ trong tháng và hạn mức còn lại.
- Số dư ví AI Credit hiện có.
- Lịch sử chi tiết của tất cả các lần mua gói nâng cấp, nạp tiền Credit hoặc quà tặng quyền lợi từ Ban quản trị.

## Cách xem trạng thái gói và hạn mức token

1. Truy cập vào trang **Gói dịch vụ AI** (`/goi-ai`) hoặc mở Cài đặt trong mục Hồ sơ.
2. Tại phần **Gói của bạn**, hệ thống hiển thị giá, quota và chu kỳ theo cấu hình hiện hành:
   - **Tên gói hiện tại:** Ví dụ *Gói Nâng Cao (Plus)*.
   - **Thanh đo lượng token:** Đã dùng bao nhiêu / Tổng số token của gói.
   - **Mốc thời gian:** Ngày bắt đầu và ngày kết thúc chu kỳ.
   - **Số dư AI Credit:** Hiển thị số Credit khả dụng còn lại trong ví.

## Quy trình nâng cấp gói (Từ Free lên Plus/Pro, hoặc Plus lên Pro)

1. Tại trang `/goi-ai`, xem giá và quyền lợi hiện hành, chọn Plus hoặc Pro rồi bấm **Nâng cấp ngay**.
2. Hệ thống chuyển sang màn hình thanh toán payOS hiển thị mã VietQR chuyển khoản ngân hàng.
3. Mở ứng dụng ngân hàng và quét mã để thanh toán.
4. Khi giao dịch hoàn tất, hệ thống tự động chuyển bạn về trang **Kết quả thanh toán** (`/payment/success`).
5. **Thời điểm áp dụng:**
   - Quyền lợi và thời điểm hiệu lực phụ thuộc loại giao dịch và chính sách hiện hành; xem chi tiết trên trang gói và kết quả thanh toán.

## Tra cứu lịch sử giao dịch

Tại trang `/giao-dich`, bạn có thể kiểm tra danh sách đầy đủ tất cả các giao dịch đã phát sinh:
- **Mã giao dịch (Mã đơn hàng):** Dãy số định danh để bạn dễ dàng tra cứu hoặc đối soát khi cần hỗ trợ.
- **Loại giao dịch:**
  - *Mua gói dịch vụ:* Nâng cấp gói Plus hoặc Pro.
  - *Nạp Credit:* Nạp tiền vào ví AI Credit.
  - *Quà tặng từ Admin:* Các lần Ban quản trị tặng gói trải nghiệm hoặc cộng Credit thưởng.
- **Số tiền thanh toán (VNĐ):** Số tiền thực tế đã giao dịch qua cổng payOS.
- **Trạng thái:**
  - *Thành công (`PAID`):* Tiền đã vào tài khoản và quyền lợi đã kích hoạt.
  - *Đang chờ (`PENDING`):* Cổng thanh toán chưa xác nhận giao dịch.
  - *Hết hạn / Đã hủy (`CANCELLED` / `FAILED`):* Xem trạng thái và hướng dẫn trên chi tiết giao dịch.

## Những điều cần lưu ý

1. **Hết hạn gói thì sao?**
   - Khi gói trả phí hết hạn, quyền lợi tài khoản tuân theo chu kỳ và cấu hình gói hiện hành. Xem quota Free hiện tại trong trang **Gói dịch vụ AI**.
2. **Không hỗ trợ hạ gói giữa chừng:**
   - Trong khi gói Pro đang còn thời hạn sử dụng, bạn không thể hạ xuống gói Plus cho đến khi chu kỳ Pro hiện tại kết thúc.
3. **Bảo mật giao dịch:**
   - Thực hiện thanh toán theo hướng dẫn của cổng thanh toán được hiển thị trong phiên giao dịch; không chia sẻ mã PIN hoặc mật khẩu ngân hàng.
