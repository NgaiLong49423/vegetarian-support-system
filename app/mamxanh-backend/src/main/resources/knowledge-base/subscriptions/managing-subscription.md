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
implementation_status: verified
last_verified: 2026-11-01
---

# Quản lý gói đăng ký và lịch sử giao dịch

## Chức năng này dùng để làm gì?

Trang **Lịch sử giao dịch** (`/giao-dich`) và trang thông tin **Gói của tôi** giúp bạn dễ dàng theo dõi:
- Gói dịch vụ hiện tại (Free, Plus hoặc Pro) và ngày hết hạn chu kỳ 30 ngày.
- Lượng token bạn đã tiêu thụ trong tháng và hạn mức còn lại.
- Số dư ví AI Credit hiện có.
- Lịch sử chi tiết của tất cả các lần mua gói nâng cấp, nạp tiền Credit hoặc quà tặng quyền lợi từ Ban quản trị.

## Cách xem trạng thái gói và hạn mức token

1. Truy cập vào trang **Gói dịch vụ AI** (`/goi-ai`) hoặc mở Cài đặt trong mục Hồ sơ.
2. Tại phần **Gói của bạn**, hệ thống hiển thị:
   - **Tên gói hiện tại:** Ví dụ *Gói Nâng Cao (Plus)*.
   - **Thanh đo lượng token:** Đã dùng bao nhiêu / Tổng số token của gói (ví dụ: `125.430 / 300.000 token`).
   - **Mốc thời gian:** Ngày bắt đầu và ngày kết thúc chu kỳ (ví dụ: *Hết hạn vào ngày 28/11/2026*).
   - **Số dư AI Credit:** Hiển thị số Credit khả dụng còn lại trong ví.

## Quy trình nâng cấp gói (Từ Free lên Plus/Pro, hoặc Plus lên Pro)

1. Tại trang `/goi-ai`, chọn gói dịch vụ bạn mong muốn (Plus 99.000đ hoặc Pro 199.000đ) và bấm **Nâng cấp ngay**.
2. Hệ thống chuyển sang màn hình thanh toán payOS hiển thị mã VietQR chuyển khoản ngân hàng.
3. Mở ứng dụng ngân hàng và quét mã để thanh toán.
4. Khi giao dịch hoàn tất, hệ thống tự động chuyển bạn về trang **Kết quả thanh toán** (`/payment/success`).
5. **Thời điểm áp dụng:**
   - Quyền lợi gói mới được kích hoạt ngay lập tức.
   - Chu kỳ 30 ngày mới sẽ được tính bắt đầu từ thời điểm bạn thanh toán thành công.
   - *(Lưu ý: Nếu bạn nâng cấp từ Plus lên Pro khi chu kỳ Plus cũ chưa hết hạn, gói Pro mới sẽ có hiệu lực ngay lập tức và bắt đầu 30 ngày mới, hệ thống không áp dụng hoàn tiền cho những ngày còn lại của gói Plus cũ).*

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
  - *Đang chờ (`PENDING`):* Đang chờ bạn quét mã QR (tối đa 15 phút).
  - *Hết hạn / Đã hủy (`CANCELLED` / `FAILED`):* Đơn bị hủy do quá thời gian 15 phút mà chưa nhận được tiền.

## Những điều cần lưu ý

1. **Hết hạn gói thì sao?**
   - Khi hết chu kỳ 30 ngày của gói Plus hoặc Pro, nếu bạn không mua tiếp, tài khoản của bạn sẽ **tự động chuyển về gói Miễn Phí (Free)** với 50.000 token mỗi tháng. Toàn bộ dữ liệu thực đơn, danh sách đi chợ và bài viết của bạn vẫn được giữ nguyên vẹn 100%.
2. **Không hỗ trợ hạ gói giữa chừng:**
   - Trong khi gói Pro đang còn thời hạn sử dụng, bạn không thể hạ xuống gói Plus cho đến khi chu kỳ Pro hiện tại kết thúc.
3. **Bảo mật giao dịch:**
   - Mâm Xanh kết nối thanh toán qua cổng payOS đã đăng ký với Ngân hàng Nhà nước; website không bao giờ lưu trữ số tài khoản, mã PIN hay thông tin thẻ ngân hàng của bạn.
