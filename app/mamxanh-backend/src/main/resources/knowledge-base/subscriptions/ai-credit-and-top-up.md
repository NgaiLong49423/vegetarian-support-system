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
implementation_status: verified
last_verified: 2026-11-01
---

# AI Credit và hướng dẫn nạp thêm ngân sách

## Chức năng này dùng để làm gì?

Khi bạn đang ở gói Free hoặc Plus nhưng sử dụng nhiều tính năng AI đến mức dùng hết số token được cấp trong tháng, bạn không bắt buộc phải nâng cấp lên gói Pro đắt đỏ.

Mâm Xanh cung cấp ví **AI Credit** tại trang `/nap-ai-credit`. AI Credit là nguồn ngân sách dự phòng linh hoạt, giúp các cuộc trò chuyện và tác vụ gợi ý thực đơn của bạn tiếp tục diễn ra liền mạch mà không bị gián đoạn.

## Quy tắc quy đổi và thanh toán vượt mức (Overage)

1. **Tỷ lệ nạp tiền:**
   $$\text{1.000 VNĐ} = \text{1.000 AI Credit}$$
   - Mức nạp tối thiểu mỗi lần: **10.000 VNĐ** (tương đương 10.000 Credit).
   - Mức nạp tối đa mỗi lần: **500.000 VNĐ** (tương đương 500.000 Credit).
2. **AI Credit KHÔNG BAO GIỜ HẾT HẠN:**
   - Khác với hạn mức token của gói sẽ hết hạn vào cuối chu kỳ 30 ngày, **số dư AI Credit trong ví của bạn được lưu giữ vĩnh viễn** trong tài khoản cho đến khi bạn sử dụng hết.
3. **Tỷ lệ trừ phí khi vượt hạn mức gói (Overage Rate):**
   $$\text{1.000 token vượt mức} = \text{400 AI Credit} \quad (\text{tương đương } 0.4\text{ Credit} / \text{token})$$
   - Mức phí được tính chính xác theo số lượng token thực tế bạn sử dụng, có hỗ trợ số lẻ thập phân, không làm tròn lên từng block gây thiệt thòi cho bạn.
   - Cùng một tỷ lệ ưu đãi này được áp dụng cho mọi mô hình Gemini (cả Flash và Flash-Lite).

## Thứ tự tiêu thụ token thông minh

Hệ thống luôn ưu tiên bảo vệ túi tiền của bạn theo thứ tự:
1. **Dùng hết token miễn phí của gói trước:** Hệ thống chỉ bắt đầu trừ AI Credit sau khi toàn bộ token hàng tháng của gói Free/Plus/Pro đã được dùng hết.
2. **Không để số dư âm (Bảo vệ tài khoản):**
   - Nếu bạn còn số dư Credit nhỏ hơn chi phí của lượt gọi AI vừa thực hiện, hệ thống chỉ trừ tối đa số Credit hiện có của bạn về 0, phần chi phí vượt trội còn lại do hệ thống Mâm Xanh chịu; ví của bạn **tuyệt đối không bao giờ bị âm tiền**.
   - Sau khi cả token gói và ví Credit đều cạn, hệ thống sẽ tạm dừng lượt gọi AI tiếp theo và hiển thị thông báo mời bạn nạp thêm.

## Các bước nạp AI Credit

1. Đăng nhập vào Mâm Xanh, vào trang **Nạp AI Credit** (`/nap-ai-credit`).
2. Chọn số tiền bạn muốn nạp (ví dụ: 20.000đ, 50.000đ, 100.000đ hoặc nhập số tiền tùy chọn từ 10.000đ đến 500.000đ).
3. Màn hình sẽ hiển thị số Credit bạn sẽ nhận được tương ứng. Bấm nút **Thanh toán ngay**.
4. Hệ thống sẽ mở cổng thanh toán bảo mật **payOS** và hiển thị mã VietQR.
5. Mở ứng dụng ngân hàng của bạn, quét mã QR và xác nhận chuyển khoản đúng số tiền và nội dung.
6. Ngay sau khi ngân hàng xử lý thành công, số dư AI Credit sẽ được cộng tức thì vào tài khoản của bạn.

## Những điều cần lưu ý

1. **Thời hạn hiệu lực của đơn nạp (15 phút):**
   - Mỗi mã QR thanh toán có thời hạn hiệu lực trong vòng **15 phút**. Nếu bạn chưa quét mã sau 15 phút, đơn hàng sẽ tự động hết hạn để bảo vệ giao dịch. Vui lòng tạo đơn mới thay vì chuyển khoản vào mã QR đã hết hạn.
2. **Nếu chuyển khoản muộn sau khi đơn hết hạn:**
   - Hệ thống sẽ ghi nhận giao dịch vào trạng thái "Chờ đối soát". Bạn chỉ cần liên hệ Ban quản trị hoặc bấm nút Yêu cầu kiểm tra, đội ngũ hỗ trợ sẽ đối soát và cộng đủ số Credit cho bạn.
