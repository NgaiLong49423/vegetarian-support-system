> **Document:** Non-Functional Requirements Specification
> **File:** `docs/requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md`
> **Version:** v1.0.0
> **Created:** 2026-09-14
> **Last Updated:** 2026-09-16
> **Status:** Active
> **Related Docs:** `docs/requirements/SRS.md`, `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md`, `docs/requirements/srs/BUSINESS-RULES.md`

# Non-Functional Requirements Specification

## 1. Mục đích và thẩm quyền tài liệu

Tài liệu này là **Authoritative Detailed Specification** sở hữu các định nghĩa chi tiết cho các yêu cầu phi chức năng (Non-Functional Requirements) và các ràng buộc chất lượng toàn hệ thống của Requirements Baseline v1.0.0 theo mô hình ISO/IEC 25010.

Khung đặc tả gốc, sự tồn tại của NFR và lifecycle chính thức được duy trì tập trung tại `docs/requirements/SRS.md`. Trạng thái trong tài liệu này là giá trị dẫn xuất; nếu có xung đột, root registry là nguồn chuẩn và tài liệu này phải được đồng bộ theo.

Mỗi yêu cầu phi chức năng được gắn thẻ stable anchor HTML cố định (`<a id="nfr-xx"></a>`) cùng với các stable anchor danh mục cấp cao (`<a id="nfr-category"></a>`) đặt trước từng nhóm để bảo đảm tính toàn vẹn và bất biến của liên kết tham chiếu.

## 2. Nguyên tắc phân loại và ranh giới kiến trúc

1. **Mô hình phân loại:** Toàn bộ các yêu cầu phi chức năng được chuẩn hóa vào **6 nhóm danh mục cốt lõi**:
   - **Performance** (Hiệu năng và khả năng đáp ứng)
   - **Security** (Bảo mật và kiểm soát truy cập)
   - **Usability** (Khả năng sử dụng và tương thích giao diện)
   - **Reliability** (Độ tin cậy, tính sẵn sàng và khả năng chịu lỗi)
   - **Privacy** (Quyền riêng tư, bảo vệ dữ liệu và tuân thủ pháp lý)
   - **Auditability & Maintainability** (Khả năng kiểm toán, giám sát và bảo trì)
2. **Ranh giới kiến trúc đã xác nhận:**
   - **Backend Architecture: Modular Monolith using MVC/layered structure within each business module.** Backend là một Spring Boot application và một deployable backend; các business capability không được tách thành microservice. Modular Monolith và MVC/layered structure được áp dụng đồng thời, không thay thế nhau.
   - Tính năng AI kiểm duyệt nội dung (`FR-12`, `BR-08`) và cơ chế đề xuất theo dõi hành vi kiểu TikTok For You nằm ngoài phạm vi (`OUT_OF_SCOPE`) của phiên bản MVP ban đầu.
   - Các hạn mức sử dụng (như quota gọi chatbot theo gói tài khoản) là quy tắc nghiệp vụ thuộc `FR-02`/`BR-01`, không nằm trong tài liệu NFR này.

## 3. Danh mục chi tiết Non-Functional Requirements

---

<a id="nfr-performance"></a>
### Nhóm 1: Performance (Hiệu năng và Trải nghiệm phản hồi)

<a id="nfr-01"></a>
#### NFR-01 — Thời gian phản hồi khi đăng nhập / đăng xuất

- **Mã yêu cầu:** NFR-01
- **Nhóm chất lượng:** Performance
- **Mô tả yêu cầu:** Thời gian phản hồi của hệ thống khi người dùng thực hiện thao tác đăng nhập hoặc đăng xuất tài khoản.
- **Nghiệp vụ liên quan:** `FR-03` (Đăng ký, đăng nhập và quản lý tài khoản cơ bản).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Metric:* Thời gian phản hồi của API Login/Logout (Response Time).
  - *Threshold:* $\le 2$ giây cho 95% số lượt request (P95) trong điều kiện tải bình thường.
- **Phương pháp kiểm chứng (Verification Method):** Kiểm thử hiệu năng (Performance/Load Test) bằng JMeter hoặc K6 trên môi trường staging.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-02"></a>
#### NFR-02 — Thời gian tìm kiếm và hiển thị bài viết công thức

- **Mã yêu cầu:** NFR-02
- **Nhóm chất lượng:** Performance
- **Mô tả yêu cầu:** Thời gian hệ thống thực hiện tìm kiếm, lọc và trả về danh sách bài viết công thức nấu ăn món chay.
- **Nghiệp vụ liên quan:** `FR-01`, `FR-08` (Tìm kiếm và lọc bài công thức đa tiêu chí).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Metric:* Thời gian xử lý truy vấn tìm kiếm và render danh sách trên client.
  - *Threshold:* $\le 3$ giây với cơ sở dữ liệu thử nghiệm của đồ án.
- **Phương pháp kiểm chứng (Verification Method):** Kiểm thử tự động API query và kiểm tra thời gian tải trang qua Chrome DevTools/Lighthouse.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-03"></a>
#### NFR-03 — Thời gian phản hồi của trợ lý Chatbot AI

- **Mã yêu cầu:** NFR-03
- **Nhóm chất lượng:** Performance
- **Mô tả yêu cầu:** Thời gian phản hồi của trợ lý ảo Gemini AI khi người dùng đặt câu hỏi tư vấn dinh dưỡng hoặc món chay.
- **Nghiệp vụ liên quan:** `FR-02`, `FR-51` (AI Chatbot hỗ trợ hỏi đáp ẩm thực chay theo ngữ cảnh).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Metric:* Thời gian từ khi gửi câu hỏi đến khi nhận toàn bộ hoặc token đầu tiên của câu trả lời.
  - *Threshold:* $\le 5$ giây cho điều kiện mạng thông thường; phân vị 90% (P90) $\le 7$ giây khi tải mạng cao. Giao diện người dùng bắt buộc hiển thị trạng thái đang xử lý (loading indicator hoặc stream phản hồi) ngay lập tức.
- **Phương pháp kiểm chứng (Verification Method):** Đo thời gian round-trip request từ client qua Spring Boot Backend tới Google Gemini API.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-04"></a>
#### NFR-04 — Thời gian tạo thực đơn tuần theo hồ sơ và ràng buộc ăn uống

- **Mã yêu cầu:** NFR-04
- **Nhóm chất lượng:** Performance
- **Mô tả yêu cầu:** Thời gian hệ thống tổng hợp và trả về đề xuất thực đơn 7 ngày dựa trên hồ sơ, ràng buộc ăn uống và Recipe Post đủ điều kiện. BMI chỉ là thông tin tham khảo, không phải căn cứ duy nhất để kê mục tiêu calorie/macro.
- **Nghiệp vụ liên quan:** `FR-09`, `FR-31`, `FR-33`–`FR-39` (Thực đơn tuần và dinh dưỡng).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Metric:* Thời gian xử lý thuật toán chọn công thức và phản hồi từ Backend.
  - *Threshold:* $\le 8$ giây để trả về trọn vẹn thực đơn tuần đề xuất. Giao diện hiển thị loading rõ ràng.
- **Phương pháp kiểm chứng (Verification Method):** Kiểm thử tích hợp đo thời gian thực thi của endpoint tạo thực đơn với các hồ sơ mẫu.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-05"></a>
#### NFR-05 — Khả năng chịu tải đồng thời của hệ thống

- **Mã yêu cầu:** NFR-05
- **Nhóm chất lượng:** Performance
- **Mô tả yêu cầu:** Khả năng phục vụ đồng thời nhiều người dùng truy cập và tương tác mà không bị sập hay suy giảm trải nghiệm nghiêm trọng.
- **Nghiệp vụ liên quan:** Toàn hệ thống.
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Mục tiêu thiết kế dài hạn:* 500 CCU là design goal, không phải MVP release gate.
  - *Ngưỡng nghiệm thu MVP:* Đạt 50 concurrent users trên môi trường kiểm thử/staging mà không phát sinh lỗi hệ thống hoặc suy giảm hiệu năng quá 20%.
  - *Stretch target:* 100 concurrent users nếu nhóm đủ thời gian; không bắt buộc để nghiệm thu MVP.
- **Phương pháp kiểm chứng (Verification Method):** Chạy JMeter với ramp-up đến 50 virtual users cho nghiệm thu; có thể chạy thêm kịch bản 100 users để đánh giá stretch target.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-security"></a>
### Nhóm 2: Security (Bảo mật và Kiểm soát truy cập)

<a id="nfr-06"></a>
#### NFR-06 — Mã hóa mật khẩu người dùng khi lưu trữ

- **Mã yêu cầu:** NFR-06
- **Nhóm chất lượng:** Security
- **Mô tả yêu cầu:** Mật khẩu của mọi tài khoản người dùng phải được băm an toàn trước khi lưu vào cơ sở dữ liệu.
- **Nghiệp vụ liên quan:** `FR-03` (Đăng ký, đăng nhập và quản lý tài khoản cơ bản).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Metric:* Thuật toán băm mật khẩu và cơ chế salt.
  - *Threshold:* 100% mật khẩu được băm bằng BCrypt (với work factor thích hợp) hoặc Argon2; tuyệt đối 0% mật khẩu lưu dưới dạng văn bản thô (plaintext) hay mã hóa đối xứng có thể giải mã.
- **Phương pháp kiểm chứng (Verification Method):** Kiểm tra mã nguồn cấu hình Spring Security `PasswordEncoder` và kiểm tra dữ liệu trực tiếp trong database table `Users`/`Accounts`.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-07"></a>
#### NFR-07 — Giới hạn số lần đăng nhập sai liên tiếp

- **Mã yêu cầu:** NFR-07
- **Nhóm chất lượng:** Security
- **Mô tả yêu cầu:** Cơ chế phòng vệ chống brute-force bằng temporary rate limiting sau ngưỡng đăng nhập sai; không chuyển tài khoản sang trạng thái Admin `LOCKED`.
- **Nghiệp vụ liên quan:** `FR-03` (Đăng ký, đăng nhập và quản lý tài khoản cơ bản).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Metric:* Số lần thất bại cho phép và thời gian khóa tạm thời.
  - *Threshold:* Sau 5 lần nhập sai liên tiếp, rate limit đồng thời theo account identifier và IP trong 10 phút.
- **Phương pháp kiểm chứng (Verification Method):** Kiểm thử tự động ngưỡng 5 lần sai, xác nhận cả account identifier và IP bị rate limit 10 phút, rồi xác nhận đăng nhập hợp lệ hoạt động lại sau thời hạn mà không cần Admin mở khóa.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-08"></a>
#### NFR-08 — Bảo vệ dữ liệu cá nhân và chỉ số sức khỏe (BMI)

- **Mã yêu cầu:** NFR-08
- **Nhóm chất lượng:** Security & Privacy
- **Mô tả yêu cầu:** Bảo đảm an toàn cho các dữ liệu cá nhân, thông tin nhân trắc học (chiều cao, cân nặng, chỉ số BMI) và hồ sơ ăn uống của người dùng.
- **Nghiệp vụ liên quan:** `FR-09`, `FR-31` (Hồ sơ sức khỏe và thực đơn cá nhân).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Truyền tải:* 100% dữ liệu truyền qua giao thức an toàn HTTPS/TLS 1.2+.
  - *Kiểm soát truy cập:* Dữ liệu sức khỏe chỉ được truy cập bởi chính chủ tài khoản (ownership check) hoặc Administrator qua phân quyền chặt chẽ ở Backend.
  - *Đề xuất kiến trúc:* **Khuyến nghị không áp dụng mã hóa cột AES-256 ở mức cơ sở dữ liệu** đối với chỉ số BMI/chiều cao/cân nặng để tránh làm tăng độ phức tạp database và overhead xử lý không cần thiết cho phạm vi MVP của đồ án. Việc bảo vệ được bảo đảm ở tầng ứng dụng (Spring Security) và mã hóa đường truyền.
- **Phương pháp kiểm chứng (Verification Method):** Code review chính sách kiểm tra quyền sở hữu (`@PreAuthorize` / ownership verification) và kiểm tra chứng chỉ SSL/TLS trên API endpoint.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-09"></a>
#### NFR-09 — Phân quyền truy cập chức năng theo vai trò (RBAC)

- **Mã yêu cầu:** NFR-09
- **Nhóm chất lượng:** Security
- **Mô tả yêu cầu:** Thực thi nghiêm ngặt phân quyền vai trò (Role-Based Access Control) giữa Guest, Member và Administrator.
- **Nghiệp vụ liên quan:** `FR-03`, `FR-04`, `FR-06`, `FR-07` và toàn bộ các nghiệp vụ quản trị.
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Metric:* Tỉ lệ chặn truy cập trái phép ở tầng Backend.
  - *Threshold:* 100% endpoint quản trị chỉ dành cho Administrator. Mọi thao tác truy cập trái quyền hoặc không có token hợp lệ đều bị chặn ở Backend và trả về mã lỗi HTTP 401 Unauthorized hoặc 403 Forbidden.
  - *Token baseline:* Access token ngắn hạn; rotating refresh token gắn với refresh session có thể thu hồi phía server; logout thu hồi refresh session. Access-token-only không phải baseline đang hoạt động.
- **Phương pháp kiểm chứng (Verification Method):** Kiểm thử tự động RBAC, refresh rotation/reuse handling, server-side revocation và logout; request dùng token/session không còn hợp lệ phải bị chặn với HTTP 401/403 phù hợp.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-10"></a>
#### NFR-10 — Phòng chống các lỗ hổng bảo mật phổ biến (OWASP Top 10)

- **Mã yêu cầu:** NFR-10
- **Nhóm chất lượng:** Security
- **Mô tả yêu cầu:** Bảo vệ hệ thống trước các nguy cơ tấn công bảo mật web phổ biến từ dữ liệu đầu vào của người dùng.
- **Nghiệp vụ liên quan:** Toàn hệ thống (đặc biệt là chức năng đăng bài, bình luận, báo cáo).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Threshold:* Không tồn tại các lỗ hổng SQL Injection (sử dụng ORM/Hibernate parameterized queries), Cross-Site Scripting (XSS - sanitize input/escape HTML trên React) và Cross-Site Request Forgery (CSRF).
- **Phương pháp kiểm chứng (Verification Method):** Sử dụng công cụ quét bảo mật tĩnh (SAST/SonarQube) hoặc quét động (OWASP ZAP) trong quy trình kiểm thử chất lượng.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-usability"></a>
### Nhóm 3: Usability (Khả năng sử dụng và Tương thích giao diện)

<a id="nfr-11"></a>
#### NFR-11 — Giao diện đăng ký tài khoản đơn giản, trực quan

- **Mã yêu cầu:** NFR-11
- **Nhóm chất lượng:** Usability
- **Mô tả yêu cầu:** Thiết kế quy trình đăng ký tài khoản nhanh chóng, thân thiện, không gây quá tải thông tin cho người dùng mới.
- **Nghiệp vụ liên quan:** `FR-03` (Đăng ký, đăng nhập và quản lý tài khoản cơ bản).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Metric:* Số bước và số thao tác click để hoàn tất tạo tài khoản.
  - *Threshold:* Hoàn thành đăng ký trong $\le 3$ bước / $\le 3$ lượt click chuyển màn hình (hỗ trợ cả đăng ký form chuẩn và Google Login 1-click).
- **Phương pháp kiểm chứng (Verification Method):** Đánh giá trải nghiệm người dùng (UX walk-through) trên giao diện đã scaffold.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-12"></a>
#### NFR-12 — Giao diện Chatbot AI trực quan, dễ sử dụng

- **Mã yêu cầu:** NFR-12
- **Nhóm chất lượng:** Usability
- **Mô tả yêu cầu:** Màn hình trò chuyện với AI được thiết kế thân thiện, dễ nắm bắt cách đặt câu hỏi mà không cần hướng dẫn sử dụng.
- **Nghiệp vụ liên quan:** `FR-02`, `FR-51` (AI Chatbot hỗ trợ hỏi đáp ẩm thực chay theo ngữ cảnh).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Threshold:* $\ge 90\%$ người dùng thử nghiệm hoàn thành 1 lượt hỏi-đáp thành công trong lần đầu tiên mà không cần trợ giúp hoặc tài liệu hướng dẫn.
- **Phương pháp kiểm chứng (Verification Method):** Thử nghiệm chấp nhận người dùng (User Acceptance Testing) trên nhóm người dùng mẫu.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-13"></a>
#### NFR-13 — Giao diện Responsive tiếng Việt trên đa kích thước màn hình

- **Mã yêu cầu:** NFR-13
- **Nhóm chất lượng:** Usability
- **Mô tả yêu cầu:** Giao diện ứng dụng hoàn toàn bằng tiếng Việt, tự động thích ứng với nhiều kích thước màn hình từ điện thoại đến máy tính bàn.
- **Nghiệp vụ liên quan:** Toàn hệ thống.
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Threshold:* Hiển thị đúng bố cục, không vỡ layout hoặc tràn màn hình ngang trên các độ phân giải từ 360px (mobile) đến 1920px (desktop full HD). Nội dung văn bản hiển thị chuẩn tiếng Việt có dấu.
- **Phương pháp kiểm chứng (Verification Method):** Kiểm tra hiển thị qua chế độ Device Mode trên Chrome DevTools và kiểm thử thực tế trên thiết bị vật lý.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-14"></a>
#### NFR-14 — Tương thích các trình duyệt web phổ biến

- **Mã yêu cầu:** NFR-14
- **Nhóm chất lượng:** Usability & Compatibility
- **Mô tả yêu cầu:** Ứng dụng web hoạt động ổn định và nhất quán trên các trình duyệt web hiện đại.
- **Nghiệp vụ liên quan:** Toàn hệ thống.
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Threshold:* Tương thích đầy đủ giao diện và tính năng trên 2 phiên bản chính thức gần nhất của Google Chrome, Mozilla Firefox, Apple Safari và Microsoft Edge.
- **Phương pháp kiểm chứng (Verification Method):** Cross-browser testing trên các trình duyệt mục tiêu.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-15"></a>
#### NFR-15 — Tương thích trình duyệt trên thiết bị di động

- **Mã yêu cầu:** NFR-15
- **Nhóm chất lượng:** Usability & Compatibility
- **Mô tả yêu cầu:** Đảm bảo người dùng truy cập web qua trình duyệt di động có trải nghiệm mượt mà và thao tác đầy đủ chức năng.
- **Nghiệp vụ liên quan:** Toàn hệ thống.
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Threshold:* Hiển thị và thao tác đầy đủ các chức năng xem, tìm kiếm, đăng bài, bình luận trên trình duyệt Safari (iOS) và Chrome (Android).
- **Phương pháp kiểm chứng (Verification Method):** Kiểm thử trên thiết bị di động iOS và Android thực tế hoặc trình giả lập mobile.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-reliability"></a>
### Nhóm 4: Reliability (Độ tin cậy, Tính sẵn sàng và Khả năng chịu lỗi)

<a id="nfr-16"></a>
#### NFR-16 — Tỉ lệ hoạt động liên tục của hệ thống (Uptime)

- **Mã yêu cầu:** NFR-16
- **Nhóm chất lượng:** Reliability & Availability
- **Mô tả yêu cầu:** Mức độ sẵn sàng phục vụ của hệ thống trong chu kỳ vận hành.
- **Nghiệp vụ liên quan:** Toàn hệ thống.
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Design goal:* Uptime $\ge 99.5\%$ mỗi tháng trong môi trường vận hành sản phẩm, không tính bảo trì định kỳ có thông báo trước; đây không phải MVP release gate.
- **Phương pháp kiểm chứng (Verification Method):** Review kiến trúc và kế hoạch monitoring; nếu có môi trường vận hành đủ thời gian thì thu thập health-check/uptime evidence, nhưng không chặn nghiệm thu MVP vì chưa đủ cửa sổ một tháng.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-17"></a>
#### NFR-17 — Toàn vẹn dữ liệu giao dịch cơ sở dữ liệu

- **Mã yêu cầu:** NFR-17
- **Nhóm chất lượng:** Reliability
- **Mô tả yêu cầu:** Đảm bảo tính toàn vẹn (ACID) của dữ liệu khi xảy ra sự cố đột ngột trong lúc lưu bài viết, cập nhật hồ sơ hoặc chỉnh sửa thực đơn.
- **Nghiệp vụ liên quan:** `FR-04` (Tạo Recipe Post), `FR-09` (Chỉnh lịch ăn tuần), `FR-53` (Shopping List).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Threshold:* 100% thao tác thay đổi dữ liệu phức tạp được bao bọc trong Database Transaction (`@Transactional`); $0\%$ mất mát dữ liệu đã được backend xác nhận lưu thành công.
- **Phương pháp kiểm chứng (Verification Method):** Kiểm thử tích hợp mô phỏng lỗi ngắt quãng giữa chừng khi lưu bài viết nhiều bước và kiểm tra rollback tự động của database.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-18"></a>
#### NFR-18 — Cơ chế dự phòng khi dịch vụ Gemini AI bị lỗi hoặc timeout

- **Mã yêu cầu:** NFR-18
- **Nhóm chất lượng:** Reliability
- **Mô tả yêu cầu:** Xử lý lỗi linh hoạt khi dịch vụ Google Gemini bên ngoài gặp sự cố mạng, timeout hoặc trả về lỗi HTTP 429/500.
- **Nghiệp vụ liên quan:** `FR-02`, `FR-10`, `FR-51`, `BR-04`.
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Threshold:* Thiết lập timeout tối đa 10 giây; khi vượt ngưỡng hoặc lỗi provider, hệ thống trả về thông báo lỗi thân thiện, cho phép người dùng thử lại và tuyệt đối **không trừ hạn mức sử dụng AI** của người dùng theo quy tắc `BR-04`.
- **Phương pháp kiểm chứng (Verification Method):** Kiểm thử mô phỏng mock lỗi timeout hoặc lỗi 429 từ Gemini API và kiểm tra số dư hạn mức của tài khoản không bị thay đổi.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-19"></a>
#### NFR-19 — Khả năng mở rộng số lượng người dùng (Scalability)

- **Mã yêu cầu:** NFR-19
- **Nhóm chất lượng:** Reliability & Scalability
- **Mô tả yêu cầu:** Kiến trúc ứng dụng sẵn sàng mở rộng quy mô khi lượng người dùng đăng ký gia tăng mà không đòi hỏi thiết kế lại hệ thống cốt lõi.
- **Nghiệp vụ liên quan:** Toàn hệ thống.
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Design goal:* Kiến trúc Modular Monolith và SQL Server hướng tới khả năng mở rộng từ 1.000 lên 10.000 Authorized User; đây không phải MVP release gate và không phải cam kết tải đồng thời.
- **Phương pháp kiểm chứng (Verification Method):** Review kiến trúc, stateless boundary phù hợp, connection-pool plan và benchmark dữ liệu mẫu khi khả thi; không yêu cầu chứng minh 10.000 user để nghiệm thu MVP.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-privacy"></a>
### Nhóm 5: Privacy (Quyền riêng tư, Bảo vệ dữ liệu và Tuân thủ)

<a id="nfr-20"></a>
#### NFR-20 — Bảo vệ dữ liệu sức khỏe cá nhân và cơ chế đồng ý (Consent)

- **Mã yêu cầu:** NFR-20
- **Nhóm chất lượng:** Privacy & Compliance
- **Mô tả yêu cầu:** Tuân thủ các nguyên tắc bảo vệ dữ liệu cá nhân khi thu thập thông tin thói quen ăn uống, dị ứng và chỉ số sức khỏe của người dùng.
- **Nghiệp vụ liên quan:** `FR-09`, `FR-31` (Hồ sơ cá nhân và thông tin ăn chay).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Threshold:* Hiển thị rõ điều khoản bảo mật và yêu cầu người dùng xác nhận đồng ý (consent) trước khi lưu thông tin sức khỏe/chế độ ăn; cung cấp quyền cho người dùng tự xem và chỉnh sửa thông tin của mình.
- **Phương pháp kiểm chứng (Verification Method):** Kiểm tra giao diện Onboarding và màn hình quản lý hồ sơ cá nhân có hiển thị thông báo đồng ý điều khoản.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-21"></a>
#### NFR-21 — Bảo mật thông tin thanh toán khi đăng ký gói AI

- **Mã yêu cầu:** NFR-21
- **Nhóm chất lượng:** Privacy & Compliance
- **Mô tả yêu cầu:** Đảm bảo an toàn tuyệt đối cho thông tin thẻ tín dụng/tài khoản thanh toán khi người dùng mua gói Plus hoặc Pro.
- **Nghiệp vụ liên quan:** `FR-13` (Đăng ký gói AI qua thanh toán thật).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Threshold:* Tuân thủ tiêu chuẩn bảo mật thanh toán PCI-DSS thông qua việc ủy quyền xử lý cho cổng thanh toán bên thứ ba hợp chuẩn; Backend của hệ thống **tuyệt đối không nhận, không xử lý và không lưu trữ số thẻ ngân hàng hoặc mã CVV**.
- **Phương pháp kiểm chứng (Verification Method):** Kiểm tra mã nguồn API thanh toán để xác nhận payload không chứa trường dữ liệu nhạy cảm của thẻ.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-22"></a>
#### NFR-22 — Giới hạn lưu trữ Prompt và Lịch sử trò chuyện AI

- **Mã yêu cầu:** NFR-22
- **Nhóm chất lượng:** Privacy
- **Mô tả yêu cầu:** Bảo vệ sự riêng tư của các cuộc trò chuyện tư vấn dinh dưỡng giữa người dùng và AI.
- **Nghiệp vụ liên quan:** `FR-02`, `FR-51` (Chatbot AI).
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Threshold:* MVP không lưu lịch sử hội thoại AI theo account; telemetry không giữ raw prompt content và được xóa sau 90 ngày. Người dùng được thông báo câu hỏi sẽ được chuyển tới Google Gemini API để xử lý.
- **Phương pháp kiểm chứng (Verification Method):** Kiểm tra schema, log/telemetry configuration và retention job để xác nhận không có raw prompt, không có account chat history và dữ liệu telemetry quá 90 ngày được loại bỏ.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-auditability"></a>
### Nhóm 6: Auditability & Maintainability (Khả năng kiểm toán, Giám sát và Bảo trì)

<a id="nfr-23"></a>
#### NFR-23 — Mã nguồn tuân thủ Convention và Tài liệu hóa API

- **Mã yêu cầu:** NFR-23
- **Nhóm chất lượng:** Maintainability
- **Mô tả yêu cầu:** Đảm bảo chất lượng mã nguồn dễ đọc, dễ bảo trì và các giao diện lập trình được mô tả đầy đủ.
- **Nghiệp vụ liên quan:** Toàn hệ thống.
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Threshold:* 100% endpoint REST API công khai có tài liệu tương tác Swagger/OpenAPI; mã nguồn Frontend và Backend tuân thủ quy chuẩn định dạng và quy tắc đóng góp đã ban hành tại [CONTRIBUTING.md](../../../CONTRIBUTING.md).
- **Phương pháp kiểm chứng (Verification Method):** Kiểm tra tài liệu Swagger UI được sinh tự động và kiểm tra qua quy trình code review.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-24"></a>
#### NFR-24 — Modular Monolith với MVC/layered structure theo business module

- **Mã yêu cầu:** NFR-24
- **Nhóm chất lượng:** Maintainability
- **Mô tả yêu cầu:** Backend phải là một Spring Boot application duy nhất, được phát hành thành một deployable backend theo kiến trúc Modular Monolith. Source code phải được chia theo business capability như `auth`, `recipe`, `mealplan`, `shopping`, `nutrition`, `subscription` và `admin`; bên trong mỗi module phải áp dụng MVC/layered structure.
- **Nghiệp vụ liên quan:** Toàn bộ business module của backend, bao gồm các capability liên quan đến `FR-06`, `FR-07` và các yêu cầu nghiệp vụ khác.
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Architecture threshold:* Có đúng một Spring Boot application và một deployable backend; không có business capability nào được tách thành microservice.
  - *Module threshold:* Mỗi business capability được tổ chức thành module/package có ranh giới rõ ràng.
  - *Layering threshold:* Mỗi module thể hiện rõ tối thiểu các trách nhiệm `controller`, `service`, `repository`, `model`/`entity`, cùng `dto` khi cần.
  - *Flow threshold:* Luồng xử lý chuẩn là `React View -> Spring MVC Controller -> Service -> Repository -> Model/Entity -> Database`.
  - *Regression threshold:* Việc sửa đổi hoặc bảo trì chức năng trong một module không gây lỗi hồi quy làm ảnh hưởng tới hoạt động của module khác; kiểm chứng qua automated test.
- **Phương pháp kiểm chứng (Verification Method):** Review package structure và dependency flow khi backend được scaffold; chạy test của module bị ảnh hưởng và Regression Test Suite sau mỗi thay đổi liên module.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-25"></a>
#### NFR-25 — Đánh giá chất lượng và tuân thủ của nội dung AI

- **Mã yêu cầu:** NFR-25
- **Nhóm chất lượng:** Auditability & AI Quality
- **Mô tả yêu cầu:** Đo mức tuân thủ của các phản hồi và gợi ý do AI tạo ra trong các nghiệp vụ liên quan đối với source boundaries, dietary/restriction constraints, non-fabrication và safety/business constraints.
- **Nghiệp vụ liên quan:** `FR-09`, `FR-47`, `FR-51`.
- **Tiêu chí đo lường (Acceptance Criteria / Metric / Threshold):**
  - *Threshold:* Ít nhất 80% case trong curated evaluation set phải đồng thời thỏa các quy tắc được áp dụng cho case về source boundaries, dietary/restriction constraints, không bịa nội dung (non-fabrication) và safety/business constraints.
- **Phương pháp kiểm chứng (Verification Method):** Chạy curated evaluation set có expected constraints và chấm pass/fail theo evidence. Không yêu cầu in-product satisfaction survey chỉ để đáp ứng NFR này; exact Gemini model/version được chọn qua technical evaluation sau.
- **Trạng thái:** `ACTIVE`

---

<a id="nfr-26"></a>
#### NFR-26 — (Ngoài phạm vi) Xử lý và lưu log bài viết bị AI gắn cờ vi phạm

- **Mã yêu cầu:** NFR-26
- **Nhóm chất lượng:** Auditability & Moderation
- **Mô tả yêu cầu:** Lưu vết và xử lý nội dung bị AI rà soát và gắn cờ vi phạm trong vòng 24 giờ.
- **Nghiệp vụ liên quan:** `FR-12`, `BR-08`.
- **Trạng thái:** `OUT_OF_SCOPE`
- **Lý do loại trừ:** Tính năng "AI rà soát và gắn cờ Recipe Post nghi vấn" (`FR-12`) và giới hạn vai trò của AI kiểm duyệt (`BR-08`) đã chính thức bị hoãn (`DEFERRED`) khỏi phạm vi phiên bản MVP ban đầu. Hệ thống hiện tại chỉ áp dụng cơ chế hậu kiểm dựa trên báo cáo vi phạm trực tiếp từ người dùng (`FR-06`). NFR này được giữ lại dưới dạng tham chiếu lịch sử và không áp dụng kiểm thử trong MVP.

---

<a id="nfr-27"></a>
#### NFR-27 — (Ngoài phạm vi) Cá nhân hóa nội dung kiểu "For You" của TikTok dựa trên lịch sử tìm kiếm

- **Mã yêu cầu:** NFR-27
- **Nhóm chất lượng:** AI Personalization
- **Mô tả yêu cầu:** Tự động ghi nhận lịch sử tìm kiếm và chủ đề người dùng quan tâm để xây dựng thuật toán máy học gợi ý nội dung tương tự cơ chế "For You" của TikTok.
- **Nghiệp vụ liên quan:** Gợi ý bài viết và cá nhân hóa nâng cao.
- **Trạng thái:** `OUT_OF_SCOPE`
- **Lý do loại trừ:** Hệ thống Vegetarian Support Application không phát triển mô hình Machine Learning đề xuất theo hành vi. Tính năng gợi ý món liên quan trong MVP (`FR-47`) chỉ áp dụng tìm kiếm theo danh mục/thẻ tag có sẵn hoặc tùy chọn từ khóa qua Gemini. Đồng thời, chính sách quyền riêng tư của MVP cam kết không lưu vết hành vi tìm kiếm cá nhân của người dùng. NFR này hoàn toàn nằm ngoài phạm vi dự án.
