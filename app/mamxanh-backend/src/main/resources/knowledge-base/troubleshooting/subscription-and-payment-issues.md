---
id: subscription-and-payment-issues
title: Xử lý sự cố thanh toán gói dịch vụ và nạp AI Credit
category: troubleshooting
audience:
  - customer
  - expert
source_requirements:
  - FR-13
  - FR-64
  - BR-77
  - BR-78
source_files:
  - app/mamxanh-frontend/src/pages/PaymentResult.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/subscription/controller/PaymentWebhookController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Xử lý sự cố thanh toán gói dịch vụ và nạp AI Credit

## Bảng tra cứu các sự cố thanh toán thường gặp

| Sự cố | Nguyên nhân | Cách xử lý |
|---|---|---|
| **Đã chuyển khoản thành công nhưng tài khoản chưa được nâng cấp hoặc chưa nhận được Credit** | Webhook từ ngân hàng về cổng thanh toán payOS bị chậm trễ đường truyền từ 1-3 phút | Tải lại trang (F5) sau 1-2 phút hoặc kiểm tra trang **Lịch sử giao dịch** (`/giao-dich`). Nếu sau 5 phút vẫn chưa có, liên hệ hỗ trợ kèm mã giao dịch để được đối soát ngay. |
| **Màn hình báo "Mã thanh toán đã hết hạn (Quá 15 phút)"** | Bạn quét mã hoặc xác nhận chuyển khoản sau khi đồng hồ đếm ngược 15 phút kết thúc | Nếu bạn chưa chuyển tiền, hãy bấm **Tạo đơn hàng mới** để lấy mã QR mới. Nếu bạn đã lỡ chuyển tiền vào mã hết hạn, hệ thống sẽ đưa vào danh sách đối soát thủ công để cộng tiền cho bạn. |
| **Báo lỗi "Số tiền chuyển khoản không khớp"** | Bạn sửa lại số tiền hoặc quên không nhập đúng số tiền lẻ trên mã QR | Cổng thanh toán yêu cầu chuyển khoản **chính xác 100% số tiền** ghi trên đơn. Đơn chuyển sai số tiền sẽ cần Ban quản trị kiểm tra và hoàn tiền hoặc cộng thủ công. |
| **Đổi ý không muốn mua nữa sau khi đã mở mã QR** | Bạn bấm hủy đơn hoặc muốn quay lại | Đơn hàng `PENDING` sẽ tự động hết hạn và tự hủy sau 15 phút mà không phát sinh bất kỳ khoản phí nào. Tài khoản của bạn không bị ảnh hưởng. |
| **Quét mã QR nhưng ứng dụng ngân hàng báo lỗi** | Camera quét mờ hoặc ứng dụng ngân hàng đang bảo trì cổng VietQR | Sử dụng tính năng copy thông tin: Số tài khoản, Ngân hàng thụ hưởng, Số tiền và Nội dung chuyển khoản trên màn hình để chuyển khoản thủ công. |

## Quy định về hoàn tiền và đối soát giao dịch

1. **Quy định hoàn tiền:**
   - Theo chính sách dịch vụ của Mâm Xanh, các gói dịch vụ AI đã thanh toán thành công và kích hoạt chu kỳ sử dụng **không áp dụng chính sách hoàn trả một phần thời gian sử dụng**.
   - Số dư AI Credit đã nạp vào ví không hỗ trợ rút ngược về tài khoản ngân hàng nhưng được bảo lưu vĩnh viễn trong tài khoản để bạn sử dụng bất cứ lúc nào.
2. **Quy trình hỗ trợ đối soát thủ công:**
   - Nếu bạn gặp sự cố chuyển khoản muộn, sai nội dung hoặc tài khoản ngân hàng đã bị trừ tiền nhưng Mâm Xanh chưa cập nhật quyền lợi:
     1. Mở trang **Lịch sử giao dịch** (`/giao-dich`) và chụp lại màn hình mã đơn hàng (ví dụ: `TX-1042`).
     2. Chụp ảnh biên lai / sao kê chuyển tiền thành công từ ứng dụng ngân hàng của bạn.
     3. Gửi thông tin đến kênh hỗ trợ của Ban quản trị Mâm Xanh. Đội ngũ kỹ thuật sẽ đối chiếu với lịch sử ngân hàng và kích hoạt quyền lợi thủ công cho bạn trong vòng 24 giờ làm việc.
