> **Document:** Use Case Specifications — M08
> **File:** `docs/requirements/use-cases/subscription-and-payment.md`
> **Version:** v2.2.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-27
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M08

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns the required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-13"></a>
## FR-13 — Đăng ký gói AI qua thanh toán thật

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13).

### Use Case navigation

- [UC-13.1 — Xem bảng giá, quyền lợi và so sánh các gói](#uc-13-1)
- [UC-13.2 — Khởi tạo đơn hàng và thanh toán nâng cấp](#uc-13-2)
- [UC-13.3 — Xem trạng thái gói hiện tại và ngày hết hạn](#uc-13-3)

Quy tắc hết hạn gói trả phí được giữ ở cấp FR-group dưới đây vì đây là sự kiện theo thời gian, không phải luồng do một thao tác xem hoặc thanh toán khởi phát.

#### Shared system-triggered flow — AF-13.2 (Hết hạn chu kỳ đã trả phí)
Khi chu kỳ tháng đã trả phí kết thúc, quyền lợi Plus hoặc Pro tự động hết hạn. Hệ thống tự động chuyển gói tài khoản về `Free`, không phát sinh thêm chi phí và không tự động trừ tiền gia hạn.

#### FR-level/system acceptance
- [AC-13.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13) — Hệ thống tự chuyển gói đã hết hạn về Free và không tự gia hạn; đây là sự kiện theo thời gian, không phải UC xem trạng thái.

---

<a id="uc-13-1"></a>
### UC-13.1 — Xem bảng giá, quyền lợi và so sánh các gói

#### Goal
Cho phép người dùng xem và so sánh giá cùng quyền lợi của các gói Free, Plus và Pro.

#### Primary Actor
`Member`.

#### Trigger
Người dùng mở trang Bảng giá dịch vụ.

#### Main Flow
1. Hệ thống hiển thị ba gói: Free (0 VNĐ/tháng), Plus (49.000 VNĐ/tháng) và Pro (99.000 VNĐ/tháng).
2. Hệ thống trình bày quyền lợi AI của từng gói theo bảng quyền lợi trong FR-13: Free có Chatbot và gợi ý món cơ bản; Plus bổ sung AI soạn bài và gợi ý biến tấu; Pro bổ sung AI lập thực đơn tuần 7 ngày.
3. Hệ thống không hiển thị gói năm, mã giảm giá, khuyến mãi hoặc gói dùng thử trong bảng giá Phase 1.

#### Postconditions
Người dùng đã xem được giá và quyền lợi để so sánh các gói; UC này chưa khởi tạo giao dịch thanh toán.

#### Traceability
- **Parent FR:** [FR-13](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13).
- **Relevant BR:** [BR-01](../srs/BUSINESS-RULES.md#br-01) (quyền AI cơ bản của Guest/Member Free); [BR-02](../srs/BUSINESS-RULES.md#br-02) (quyền tính năng AI theo gói Plus/Pro).
- **Relevant NFR:** [NFR-13](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13) (giao diện tiếng Việt, responsive).

#### Acceptance Coverage
- [AC-13.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13) — Hiển thị đúng bảng giá và quyền lợi các gói.

---

<a id="uc-13-2"></a>
### UC-13.2 — Khởi tạo đơn hàng và thanh toán nâng cấp

#### Goal
Cho phép Member mua gói Plus hoặc Pro và chỉ nhận quyền lợi trả phí sau khi thanh toán được xác minh thành công.

#### Primary Actor
`Member`.

#### Supporting Actors
Cổng thanh toán trực tuyến.

#### Trigger
Member đã đăng nhập nhấn “Nâng cấp gói” tại khu vực quản lý tài khoản hoặc từ thông báo nâng cấp khi truy cập tính năng nâng cao.

#### Preconditions
Member đã đăng nhập tài khoản hợp lệ; giao dịch được thực hiện cho một gói trả phí đang được niêm yết trong FR-13.

#### Main Flow
1. Member chọn Plus hoặc Pro và nhấn “Tiến hành thanh toán”.
2. Hệ thống tạo giao dịch mới với mã giao dịch duy nhất, trạng thái `Pending`, số tiền, mã gói và ID tài khoản Member.
3. Hệ thống tạo URL chuyển hướng an toàn có chữ ký số và chuyển Member tới cổng thanh toán.
4. Member hoàn tất thanh toán trên giao diện của cổng thanh toán.
5. Cổng thanh toán gửi kết quả giao dịch về máy chủ qua Webhook/IPN.
6. Hệ thống xác thực chữ ký số, số tiền và trạng thái giao dịch. Khi thông tin hợp lệ và giao dịch thành công, hệ thống chuyển giao dịch sang `Success` và kích hoạt quyền lợi của gói đã mua theo chu kỳ tháng được xác minh (BR-02).
7. Hệ thống gửi thông báo xác nhận thanh toán thành công trong ứng dụng và qua email cho Member.
8. Sau khi hoàn tất, Member được chuyển tới trang thông tin tài khoản; xem [UC-13.3](#uc-13-3) để biết thông tin trạng thái gói được hiển thị.

#### Alternative Flows
- **AF-13.1 — Member hủy thanh toán hoặc giao dịch thất bại:** Nếu Member hủy hoặc thanh toán không thành công tại cổng thanh toán, giao dịch được ghi nhận là `Cancelled` hoặc `Failed`; quyền lợi hiện tại được giữ nguyên, hệ thống thông báo thanh toán chưa hoàn tất và cho phép thử lại.

#### Exception Flows
- **EF-13.1 — Thông báo thanh toán lặp:** Nếu cổng thanh toán gửi lại IPN cho mã giao dịch đã xử lý thành công, hệ thống xác nhận đã xử lý mà không cộng dồn thời hạn hoặc kích hoạt quyền lợi lần nữa (idempotent handling).

#### Security Flows
- **SF-13.1 — Chữ ký hoặc số tiền không hợp lệ:** Hệ thống từ chối kích hoạt quyền lợi, đánh dấu giao dịch `Tampered/Invalid` và ghi vết cảnh báo an ninh nếu chữ ký không khớp hoặc số tiền sai với giá niêm yết.
- **SF-13.2 — Không lưu thông tin thẻ ngân hàng:** Việc nhập thông tin thẻ/tài khoản ngân hàng diễn ra trên hạ tầng của cổng thanh toán; hệ thống dự án không nhận, xử lý hoặc lưu số thẻ, CVV hay mật khẩu ngân hàng.

#### Postconditions
Với giao dịch thành công đã xác minh, giao dịch được ghi nhận `Success` và quyền lợi Member được kích hoạt theo gói/chu kỳ đã thanh toán. Giao dịch lặp không làm thay đổi thêm thời hạn hoặc quyền lợi.

#### Traceability
- **Parent FR:** [FR-13](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13).
- **Relevant BR:** [BR-02](../srs/BUSINESS-RULES.md#br-02) (quyền tính năng AI của các gói trả phí).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (phân quyền); [NFR-10](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10) (xác thực dữ liệu/IPN); [NFR-13](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13) (giao diện ứng dụng); [NFR-21](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-21) (bảo mật dữ liệu thanh toán).

#### Acceptance Coverage
- [AC-13.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13) — Kích hoạt quyền lợi sau khi xác minh thanh toán thành công.
- [AC-13.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13) — Xử lý callback/IPN lặp idempotently.
- [AC-13.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13) — Từ chối chữ ký hoặc số tiền không hợp lệ.
- [AC-13.6](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13) — Giữ quyền lợi hiện tại khi giao dịch bị hủy hoặc thất bại.

---

<a id="uc-13-3"></a>
### UC-13.3 — Xem trạng thái gói hiện tại và ngày hết hạn

#### Goal
Giúp Member biết gói hiện tại và thời điểm kết thúc chu kỳ đã thanh toán, nếu có.

#### Primary Actor
`Member`.

#### Trigger
Member yêu cầu xem thông tin gói hiện tại của tài khoản.

#### Preconditions
Member đã đăng nhập tài khoản hợp lệ.

#### Main Flow
1. Member yêu cầu xem thông tin gói hiện tại; sau luồng thanh toán thành công, Member cũng được chuyển tới trang thông tin tài khoản như đã mô tả ở UC-13.2.
2. Hệ thống hiển thị trạng thái gói hiện tại; với gói trả phí, hiển thị ngày hết hạn của chu kỳ đã thanh toán.

#### Postconditions
Member đã xem được trạng thái gói hiện tại và ngày hết hạn chu kỳ trả phí khi áp dụng.

#### Traceability
- **Parent FR:** [FR-13](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13).
- **Relevant BR:** [BR-02](../srs/BUSINESS-RULES.md#br-02) (quyền tính năng theo hạng gói).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (phân quyền truy cập); [NFR-13](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13) (giao diện tiếng Việt, responsive).

#### Acceptance Coverage
- [AC-13.7](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13) — Hiển thị trạng thái gói và ngày hết hạn chu kỳ đã thanh toán.

---
