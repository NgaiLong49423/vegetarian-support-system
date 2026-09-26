> **Document:** Use Case Specifications — M08
> **File:** `docs/requirements/use-cases/subscription-and-payment.md`
> **Version:** v2.0.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-26
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M08

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns the required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-13"></a>
## FR-13 — Đăng ký gói AI qua thanh toán thật

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản Member hợp lệ (FR-03, BR-05).
- **Kích hoạt (Trigger):**
  - Member bấm nút "Nâng cấp gói" trên giao diện quản lý tài khoản hoặc từ thông báo hướng dẫn nâng cấp gói khi truy cập tính năng nâng cao.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member truy cập trang Bảng giá dịch vụ, xem bảng so sánh: Free (0 VNĐ, Chatbot & Gợi ý món cơ bản), Plus (49.000 VNĐ/tháng, thêm AI Soạn bài & Gợi ý biến tấu), Pro (99.000 VNĐ/tháng, toàn quyền gồm AI Lập thực đơn tuần 7 ngày).
  - Bước 2: Member chọn gói mong muốn (ví dụ gói Plus 49.000 VNĐ) và nhấn "Tiến hành thanh toán".
  - Bước 3: Hệ thống tạo một giao dịch thanh toán mới ở trạng thái `Pending` với mã giao dịch duy nhất (Transaction ID), lưu trữ số tiền (49.000 VNĐ), mã gói (PLUS), và ID tài khoản của Member.
  - Bước 4: Hệ thống tạo URL chuyển hướng an toàn kèm chữ ký số và điều hướng Member tới cổng thanh toán.
  - Bước 5: Member hoàn tất thanh toán trên giao diện cổng thanh toán.
  - Bước 6: Cổng thanh toán gửi thông báo kết quả giao dịch (Webhook / IPN) về máy chủ hệ thống kèm chữ ký số hợp lệ.
  - Bước 7: Hệ thống xác thực tính hợp lệ của chữ ký số, số tiền và trạng thái giao dịch:
    - Nếu chữ ký hợp lệ và giao dịch thành công: Hệ thống cập nhật trạng thái giao dịch sang `Success`.
    - Hệ thống kích hoạt quyền lợi (Entitlement) gói Plus cho Member: Mở khóa quyền sử dụng AI Soạn bài (FR-21) và Gợi ý biến tấu (FR-47) (BR-02), thiết lập thời hạn hiệu lực của quyền lợi tương ứng với chu kỳ tháng đã trả phí được xác thực.
  - Bước 8: Hệ thống gửi thông báo xác nhận thanh toán thành công trong ứng dụng và qua email cho Member.
  - Bước 9: Member được chuyển hướng về trang thông tin tài khoản hiển thị gói dịch vụ hiện tại là `Plus` kèm ngày hết hạn.
- **Luồng thay thế (Alternative Flows):**
  - *AF-13.1 (Member hủy thanh toán hoặc giao dịch thất bại):* Nếu Member bấm hủy hoặc thanh toán không thành công tại cổng thanh toán, giao dịch được ghi nhận trạng thái `Cancelled` hoặc `Failed`. Quyền lợi tài khoản của Member giữ nguyên ở mức hiện tại; hệ thống hiển thị thông báo thanh toán chưa hoàn tất và cho phép thử lại.
  - *AF-13.2 (Hết hạn chu kỳ đã trả phí - Graceful Expiration):* Khi chu kỳ tháng đã trả phí kết thúc, quyền lợi Plus hoặc Pro tự động hết hạn. Hệ thống tự động chuyển gói tài khoản về `Free` mà hoàn toàn không phát sinh thêm bất kỳ chi phí nào và không tự động trừ tiền gia hạn.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-13.1 (Xử lý thông báo thanh toán lặp - Idempotent Handling):* Nếu cổng thanh toán gửi nhiều lần thông báo IPN cho cùng một mã giao dịch đã xử lý thành công trước đó, hệ thống nhận diện mã giao dịch đã ở trạng thái `Success`, lập tức phản hồi xác nhận cho cổng thanh toán mà không cộng dồn thời hạn hay kích hoạt quyền lợi lần thứ hai.
  - *SF-13.1 (Phát hiện giả mạo dữ liệu thanh toán):* Nếu dữ liệu IPN/Callback có chữ ký số không khớp hoặc số tiền thanh toán không đúng với giá niêm yết của gói (ví dụ giả mạo số tiền 1.000 VNĐ thay vì 49.000 VNĐ), hệ thống từ chối kích hoạt quyền lợi, đánh dấu giao dịch `Tampered/Invalid` và ghi vết cảnh báo an ninh (NFR-10).
  - *SF-13.2 (Không lưu trữ thông tin thẻ ngân hàng):* Toàn bộ thao tác nhập thông tin thẻ/tài khoản ngân hàng diễn ra trực tiếp trên hạ tầng bảo mật của cổng thanh toán; hệ thống của dự án tuyệt đối không lưu trữ số thẻ tín dụng, mã CVV hay mật khẩu ngân hàng của người dùng (NFR-08, NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Quyền lợi tính năng AI của Member được kích hoạt đúng theo gói dịch vụ đã thanh toán.
- Bản ghi giao dịch được lưu vết kiểm toán đầy đủ và minh bạch.

---
