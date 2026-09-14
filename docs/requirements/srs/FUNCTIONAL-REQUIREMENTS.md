> **Document:** Functional Requirements Specification
> **File:** `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md`
> **Version:** v0.2.0
> **Created:** 2026-09-14
> **Last Updated:** 2026-09-14
> **Status:** Draft
> **Related Docs:** `docs/requirements/SRS.md`, `docs/requirements/srs/BUSINESS-RULES.md`, `docs/requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md`

# Functional Requirements Specification

## 1. Mục đích và thẩm quyền tài liệu

Tài liệu này là **Authoritative Detailed Specification** sở hữu các định nghĩa chi tiết cho toàn bộ Functional Requirements (`FR-01` đến `FR-56`) của hệ thống.

Khung đặc tả gốc và **Authoritative Registry** cho sự tồn tại của requirement, mã định danh ổn định (stable ID), phân bổ module cấp chỉ mục, và trạng thái vòng đời (lifecycle state) chính thức được duy trì tập trung tại `docs/requirements/SRS.md`.

Trạng thái hiển thị trong tài liệu này là giá trị dẫn xuất (derived) từ root registry `SRS.md`. Nếu phát sinh bất kỳ xung đột nào về trạng thái lifecycle, giá trị trong `SRS.md` luôn là chuẩn có thẩm quyền cao nhất; tài liệu này sẽ được đồng bộ hóa theo `SRS.md`.

Mỗi yêu cầu chức năng được gắn một thẻ stable anchor HTML cố định `<a id="fr-xx"></a>` đặt trước tiêu đề để đảm bảo tính bất biến của liên kết tham chiếu, không phụ thuộc vào việc tiêu đề yêu cầu có thể thay đổi hoặc được dịch nghĩa trong tương lai.

## 2. Quy tắc lifecycle trong baseline hiện tại

Vocabulary duy nhất dùng cho requirement lifecycle là `DRAFT`, `ACTIVE`, `DEFERRED`, `OUT_OF_SCOPE` và `RETIRED`. Các trạng thái derived dưới đây đã được đồng bộ từ root registry theo DEC-001–DEC-003; lifecycle và mức độ decomposition là hai chiều độc lập.

Không còn FR mang legacy/unknown lifecycle wording. Nội dung `DEFERRED`, `OUT_OF_SCOPE` và `RETIRED` được giữ để bảo toàn lịch sử nhưng không tạo MVP implementation scope.

## 3. Danh mục chi tiết Functional Requirements

<a id="fr-01"></a>
### FR-01 — Hệ thống cho Guest xem và tìm kiếm nội dung đã công khai

- **Mã yêu cầu:** FR-01
- **Module:** M01
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Guest xem và tìm kiếm nội dung đã công khai.

---

<a id="fr-02"></a>
### FR-02 — Hệ thống cho Guest dùng Gemini AI theo Guest Free

- **Mã yêu cầu:** FR-02
- **Module:** M06
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Guest dùng Gemini AI theo Guest Free: tối đa 5 request AI thành công/ngày, reset lúc 00:00 `Asia/Ho_Chi_Minh`; Guest được nhận diện bằng anonymous cookie kết hợp coarse IP rate limiting. Request lỗi không trừ lượt.

---

<a id="fr-03"></a>
### FR-03 — Đăng ký, đăng nhập và quản lý tài khoản cơ bản

- **Mã yêu cầu:** FR-03
- **Module:** M02
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Đăng ký/đăng nhập email/password và Google Login, xác minh email bắt buộc, quên/đặt lại mật khẩu. Sau 5 lần đăng nhập sai, hệ thống rate limit cả account identifier và IP trong 10 phút, không chuyển tài khoản thành trạng thái Admin `LOCKED`. Authentication baseline dùng access token ngắn hạn, rotating refresh token và refresh session/revocation phía server; logout thu hồi refresh session.

---

<a id="fr-04"></a>
### FR-04 — Member tạo và công khai trực tiếp Recipe Post của chính mình

- **Mã yêu cầu:** FR-04
- **Module:** M02, M03
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Member đã đăng nhập tạo, xem, sửa, xóa và công khai trực tiếp Recipe Post của chính mình khi đạt profile validation tại SRS 3.9; Member có thể upload tối đa 5 ảnh JPEG/PNG/WebP, mỗi ảnh tối đa 5 MB, và gắn tối đa một link YouTube. Không có luồng gửi Blog tổng quát, đơn xin quyền đăng hoặc duyệt trước từng bài.

---

<a id="fr-05"></a>
### FR-05 — Cơ chế xin quyền đăng và Administrator duyệt trước bài công thức

- **Mã yêu cầu:** FR-05
- **Module:** M02, M03, M09
- **Trạng thái (Derived):** RETIRED
- **Mô tả:** Cơ chế Member gửi đơn xin quyền đăng và Administrator duyệt/từ chối đơn trước khi đăng Recipe Post.

---

<a id="fr-06"></a>
### FR-06 — Administrator xử lý báo cáo và quản lý nội dung hậu kiểm

- **Mã yêu cầu:** FR-06
- **Module:** M09
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Administrator xử lý báo cáo và quản lý thành viên, Recipe Post, bình luận, danh mục. Administrator không duyệt quyền đăng hoặc duyệt từng Recipe Post trước khi công khai.

---

<a id="fr-07"></a>
### FR-07 — Administrator tạo, cập nhật và phân loại bài công thức chay

- **Mã yêu cầu:** FR-07
- **Module:** M04
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Administrator tạo/cập nhật và phân loại bài công thức chay.

---

<a id="fr-08"></a>
### FR-08 — Tìm kiếm và lọc bài công thức đa tiêu chí

- **Mã yêu cầu:** FR-08
- **Module:** M04
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Tìm/lọc theo từ khóa, loại ăn chay, danh mục, nguyên liệu, thời gian nấu; sắp xếp mới nhất/nhiều Like.

---

<a id="fr-09"></a>
### FR-09 — Authorized User tạo và chỉnh lịch ăn tuần

- **Mã yêu cầu:** FR-09
- **Module:** M05
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Authorized User tạo và chỉnh lịch ăn tuần bằng một hoặc nhiều mục trong mỗi Bữa sáng/Bữa trưa/Bữa tối của từng ngày. Nếu Recipe Post được tham chiếu trở thành hidden/deleted/unavailable, mục lịch được giữ bằng unavailable/tombstone thay vì bị cascade-delete.

---

<a id="fr-10"></a>
### FR-10 — Áp dụng hạn mức Gemini AI theo gói tài khoản

- **Mã yêu cầu:** FR-10
- **Module:** M06
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống áp dụng hạn mức 5/5/15/50 request AI thành công mỗi ngày cho Guest/Free/Plus/Pro, kiểm tra trước khi gọi provider và reset lúc 00:00 `Asia/Ho_Chi_Minh`; Member theo account, Guest theo anonymous cookie kết hợp coarse IP rate limiting.

---

<a id="fr-11"></a>
### FR-11 — Lưu lượt gọi AI thành công và token usage metadata

- **Mã yêu cầu:** FR-11
- **Module:** M06
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống lưu số lượt AI thành công và token usage metadata do Gemini trả về để đo usage thật. Telemetry không lưu raw prompt content, có thời hạn lưu 90 ngày; lỗi provider không trừ lượt.

---

<a id="fr-12"></a>
### FR-12 — AI rà soát và gắn cờ Recipe Post nghi vấn cho Administrator

- **Mã yêu cầu:** FR-12
- **Module:** M07
- **Trạng thái (Derived):** DEFERRED
- **Mô tả:** AI rà soát/gắn cờ Recipe Post nghi vấn và chỉ cung cấp tín hiệu cho Administrator; AI không tự áp dụng chế tài.

---

<a id="fr-13"></a>
### FR-13 — Đăng ký gói AI qua thanh toán thật

- **Mã yêu cầu:** FR-13
- **Module:** M08
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cung cấp ba gói tháng bằng VND: FREE 0 VND/tháng, PLUS 49,000 VND/tháng và PRO 99,000 VND/tháng. MVP không tự động gia hạn và không hoàn tiền một phần. Entitlement Plus/Pro chỉ kích hoạt sau thanh toán thành công đã được xác minh, hết hạn cuối kỳ đã trả; xử lý cùng một payment event/giao dịch lặp phải idempotent. Không có tier, annual plan, discount, coupon, trial hoặc promotional pricing khác.

---

<a id="fr-14"></a>
### FR-14 — Lưu trữ ảnh bài công thức trên Azure Blob Storage

- **Mã yêu cầu:** FR-14
- **Module:** M03
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống lưu ảnh bài công thức trên Azure Blob Storage và lưu metadata/tham chiếu trong SQL Server.

---

<a id="fr-15"></a>
### FR-15 — Nhúng trình phát YouTube trong bài công thức

- **Mã yêu cầu:** FR-15
- **Module:** M01, M03
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống nhúng YouTube player trong bài công thức từ link hợp lệ; hệ thống không upload hoặc sao chép file video YouTube.

---

<a id="fr-16"></a>
### FR-16 — Cấu trúc dữ liệu bài công thức và tính tùy chọn của hướng dẫn nấu

- **Mã yêu cầu:** FR-16
- **Module:** M03, M04
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Bài công thức có tên 3–120 ký tự, 1–50 nguyên liệu, khẩu phần 1–50, thời gian chuẩn bị và nấu mỗi giá trị 0–1.440 phút với tổng lớn hơn 0, loại ăn chay, mô tả tối đa 2.000 ký tự, tối đa 30 bước tùy chọn, tối đa 5 ảnh JPEG/PNG/WebP (5 MB/ảnh) và tối đa một link YouTube. Tất cả bài dùng chung luồng công khai trực tiếp và hậu kiểm.

---

<a id="fr-17"></a>
### FR-17 — Trình bày bài công thức dạng thẻ món trong Khám phá và liên kết lịch ăn

- **Mã yêu cầu:** FR-17
- **Module:** M04, M05
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống trình bày bài công thức đang công khai, không bị ẩn/xóa dưới dạng thẻ món trong Khám phá và nối thẻ/trang chi tiết với luồng lưu công thức hoặc thêm món vào lịch ăn.

---

<a id="fr-18"></a>
### FR-18 — Admin quản lý danh mục nguyên liệu và danh mục món ăn

- **Mã yêu cầu:** FR-18
- **Module:** M04, M09
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Admin quản lý nguyên liệu và tạo/sửa/ngừng dùng danh mục món/công thức; một công thức có nhiều danh mục.

---

<a id="fr-19"></a>
### FR-19 — Nhập nguyên liệu linh hoạt và định lượng

- **Mã yêu cầu:** FR-19
- **Module:** M03, M04
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Người đăng bài công thức chọn nguyên liệu có sẵn hoặc nhập tên mới khi không tìm thấy, rồi khai báo định lượng cụ thể hoặc “vừa đủ”; mỗi bài có 1–50 dòng nguyên liệu, mỗi dòng giữ tên hiển thị và liên kết danh mục chuẩn tùy chọn.

---

<a id="fr-20"></a>
### FR-20 — Thống nhất nguồn hiển thị thẻ món, chi tiết và thực đơn

- **Mã yêu cầu:** FR-20
- **Module:** M01, M04, M05
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cung cấp trang chi tiết và thẻ món từ cùng một bài công thức; thực đơn tham chiếu bài này, không tạo bài nguồn hoặc website riêng.

---

<a id="fr-21"></a>
### FR-21 — AI hỗ trợ tạo giới thiệu hoặc bước nấu không lưu nháp server

- **Mã yêu cầu:** FR-21
- **Module:** M03, M06
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** AI hỗ trợ tạo phần giới thiệu hoặc bước nấu có thể chỉnh sửa từ thông tin tác giả cung cấp, không thêm nguyên liệu; tác giả tự kiểm tra và công khai. AI không tạo lưu nháp Recipe Post bền vững trên server.

---

<a id="fr-22"></a>
### FR-22 — Thao tác chỉnh sửa và sắp xếp bước nấu tùy chọn

- **Mã yêu cầu:** FR-22
- **Module:** M01, M03, M06
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Trình soạn bài công thức cho tác giả thêm, sửa, xóa và đổi vị trí tối đa 30 bước nấu tùy chọn; nếu có, trang chi tiết hiển thị đúng thứ tự đã lưu. Hướng dẫn do AI hỗ trợ cũng dùng cấu trúc bước này.

---

<a id="fr-23"></a>
### FR-23 — Hiển thị thông tin tác giả gắn liền với tài khoản

- **Mã yêu cầu:** FR-23
- **Module:** M01, M02, M03
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hiển thị tác giả gắn tài khoản; hồ sơ có tên/avatar/giới thiệu ngắn/bài đăng/ngày tham gia/tổng Like trên bài.

---

<a id="fr-24"></a>
### FR-24 — Lưu nháp Recipe Post chưa đầy đủ trên server

- **Mã yêu cầu:** FR-24
- **Module:** M03
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Mô tả:** Lưu nháp Recipe Post chưa đầy đủ trên server.

---

<a id="fr-25"></a>
### FR-25 — Công khai Recipe Post ngay sau khi validation đạt

- **Mã yêu cầu:** FR-25
- **Module:** M02, M03
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Khi Member đã đăng nhập bấm công khai và toàn bộ validation tại SRS 3.9 đạt, hệ thống công khai Recipe Post ngay. Luồng hiện tại không có trạng thái Draft, Pending Review hoặc Rejected ở cấp bài.

---

<a id="fr-26"></a>
### FR-26 — Gửi báo cáo bài công thức có vấn đề

- **Mã yêu cầu:** FR-26
- **Module:** M02, M03, M09
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Tài khoản đã đăng nhập được gửi báo cáo bài công thức có vấn đề; Guest được yêu cầu đăng nhập khi bấm “Báo cáo”. Administrator tiếp nhận, xem xét và xử lý báo cáo theo mục 3.12.

---

<a id="fr-27"></a>
### FR-27 — Biểu mẫu báo cáo bài công thức theo 6 nhóm lý do

- **Mã yêu cầu:** FR-27
- **Module:** M03
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Biểu mẫu báo cáo bài công thức cho chọn một trong sáu nhóm lý do tại 3.11 và nhập mô tả bổ sung; mô tả bắt buộc khi chọn “Khác”, tùy chọn với các lý do còn lại.

---

<a id="fr-28"></a>
### FR-28 — Administrator xử lý báo cáo qua các trạng thái

- **Mã yêu cầu:** FR-28
- **Module:** M02, M03, M09
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Admin quyết định thủ công khi xử lý báo cáo qua Chờ xử lý, Đang xem xét, Đã giải quyết; bắt buộc ghi kết luận/lý do, giữ audit/history và có thể cảnh báo, ẩn/xóa Recipe Post hoặc khóa/mở khóa tài khoản. MVP không có ma trận chế tài số tự động.

---

<a id="fr-29"></a>
### FR-29 — Phân quyền hiển thị thông tin báo cáo

- **Mã yêu cầu:** FR-29
- **Module:** M02, M03, M09
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống phân quyền xem báo cáo theo mục 3.13: Admin thấy tài khoản người gửi; tác giả thấy thông tin xử lý nhưng không thấy danh tính người báo cáo; người gửi xem trạng thái/kết quả báo cáo của mình; báo cáo không công khai.

---

<a id="fr-30"></a>
### FR-30 — Bổ sung thông tin và ngăn trùng lặp báo cáo mở

- **Mã yêu cầu:** FR-30
- **Module:** M03, M09
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Người gửi bổ sung mô tả cho báo cáo đang mở; hệ thống ngăn cùng tài khoản tạo báo cáo mở trùng trên một bài công thức. Admin xem báo cáo nhóm theo bài; báo cáo lại sau khi giải quyết cần mô tả vấn đề mới.

---

<a id="fr-31"></a>
### FR-31 — Điều kiện thông tin tối thiểu trước khi dùng AI cá nhân hóa

- **Mã yêu cầu:** FR-31
- **Module:** M02, M05, M06
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Member được bỏ qua Onboarding; trước khi dùng AI gợi ý món hoặc tạo thực đơn cá nhân hóa, hệ thống kiểm tra ba nhóm thông tin tối thiểu tại 3.15 và yêu cầu bổ sung nếu thiếu.

---

<a id="fr-32"></a>
### FR-32 — Lưu, bỏ lưu và quản lý danh sách công thức đã lưu

- **Mã yêu cầu:** FR-32
- **Module:** M02, M04, M05
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Member lưu/bỏ lưu bài công thức và xem, tìm/lọc danh sách công thức đã lưu; mỗi bài chỉ xuất hiện một lần trong danh sách của cùng Member. Tham chiếu đã lưu được giữ bằng unavailable/tombstone nếu bài bị hidden/deleted/unavailable, nhưng bài không còn được khám phá công khai hoặc dùng bởi AI.

---

<a id="fr-33"></a>
### FR-33 — Thêm và quản lý bài công thức trong lịch ăn 3 bữa

- **Mã yêu cầu:** FR-33
- **Module:** M02, M04, M05
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Member thêm bài công thức vào ngày và một trong ba loại Bữa sáng/Bữa trưa/Bữa tối, chuyển ngày/bữa, thay công thức và xóa mục khỏi lịch ăn. Mỗi bữa được có nhiều công thức nhưng không có cùng công thức trùng trong cùng ngày/bữa; tham chiếu hiện có được giữ bằng unavailable/tombstone khi bài nguồn không còn khả dụng.

---

<a id="fr-34"></a>
### FR-34 — AI gợi ý món và lập menu từ công thức công khai có sẵn

- **Mã yêu cầu:** FR-34
- **Module:** M04, M05, M06
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** AI gợi ý món, lập menu và đề xuất thay thế chỉ từ Recipe Post đang công khai, không hidden/deleted/unavailable, dựa trên hồ sơ và nguyên liệu người dùng cung cấp; AI không bịa công thức, mọi kết quả dẫn tới bài nguồn và chỉ được lưu sau khi người dùng xác nhận.

---

<a id="fr-35"></a>
### FR-35 — Khai báo hồ sơ dinh dưỡng và xem chỉ số tham khảo cá nhân

- **Mã yêu cầu:** FR-35
- **Module:** M02, M10
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Member đủ 18 tuổi, không mang thai/cho con bú và không cần chế độ ăn điều trị khai báo dữ liệu hồ sơ dinh dưỡng tại 3.18 và xem BMI cùng thông tin dinh dưỡng cá nhân có tính tham khảo dựa trên nguồn USDA/NIH và dữ liệu rộng hơn BMI. Hệ thống không tự kê calorie/macro target chỉ từ BMI hoặc mục tiêu cân nặng; kết quả không phải chẩn đoán hay điều trị.

---

<a id="fr-36"></a>
### FR-36 — AI lập menu theo nhu cầu dinh dưỡng từ công thức tin cậy

- **Mã yêu cầu:** FR-36
- **Module:** M04, M05, M06, M10
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** AI lập menu tham khảo chỉ từ Recipe Post đang công khai và có dữ liệu đủ tin cậy được tính từ gram nguyên liệu/khẩu phần, ưu tiên ràng buộc ăn uống và yêu cầu người dùng xác nhận trước khi lưu. Goal-adjusted calorie/macro behavior chỉ là stretch, không phải MVP acceptance.

---

<a id="fr-37"></a>
### FR-37 — Khai báo khẩu phần trong Lịch ăn và kiểm tra menu ngày theo 9 chỉ tiêu

- **Mã yêu cầu:** FR-37
- **Module:** M05, M06, M10
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Member khai báo số khẩu phần của từng món trong Lịch ăn và kiểm tra menu một ngày theo chín chỉ tiêu tại 3.18; kết quả cộng tất cả món trong ba bữa, hiển thị mức tham khảo cá nhân, tổng, chênh lệch, trạng thái và cảnh báo thiếu dữ liệu. AI được giải thích hoặc đề xuất món thay thế đã có.

---

<a id="fr-38"></a>
### FR-38 — Xác nhận phạm vi hỗ trợ trước khi dùng chức năng dinh dưỡng

- **Mã yêu cầu:** FR-38
- **Module:** M02, M10
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Trước chức năng dinh dưỡng, hệ thống yêu cầu Member xác nhận thuộc phạm vi hỗ trợ; người không đủ điều kiện được giải thích giới hạn và vẫn sử dụng các chức năng không-dinh-dưỡng.

---

<a id="fr-39"></a>
### FR-39 — Tính toán ước tính 9 chỉ tiêu dinh dưỡng cho công thức

- **Mã yêu cầu:** FR-39
- **Module:** M03, M04, M10
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống tính tổng ước tính của chín chỉ tiêu cho công thức bằng cách cộng dữ liệu từ danh mục nguyên liệu dinh dưỡng nội bộ theo định lượng, sau đó quy đổi theo số khẩu phần đã khai báo; khi thiếu dữ liệu phải công khai phạm vi thiếu thay vì coi là 0 hoặc để AI đoán.

---

<a id="fr-40"></a>
### FR-40 — Công khai Recipe Post chứa nguyên liệu ngoài danh mục dinh dưỡng

- **Mã yêu cầu:** FR-40
- **Module:** M03, M04, M06, M10
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Member đã đăng nhập công khai Recipe Post chứa nguyên liệu ngoài danh mục dinh dưỡng; nguyên liệu đó được đánh dấu chưa hỗ trợ tính dinh dưỡng, khiến kết quả không đầy đủ và bài không tham gia AI menu theo mục tiêu dinh dưỡng.

---

<a id="fr-41"></a>
### FR-41 — Administrator quản lý danh mục nguyên liệu dinh dưỡng nội bộ

- **Mã yêu cầu:** FR-41
- **Module:** M09, M10
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Administrator xem/tìm kiếm, thêm, sửa, bật lại hoặc ngừng hỗ trợ nguyên liệu dinh dưỡng; quản lý tên, chín giá trị trên 100 g, tên/đường dẫn nguồn, ngày tham khảo và xem các công thức đang sử dụng nguyên liệu. Chức năng này không cho AI tự tạo hoặc xác nhận số liệu.

---

<a id="fr-42"></a>
### FR-42 — Member tìm nhà hàng chay theo địa chỉ nhập và bán kính đường bộ

- **Mã yêu cầu:** FR-42
- **Module:** M02, M11
- **Trạng thái (Derived):** DEFERRED
- **Mô tả:** Nội dung lịch sử của capability đã deferred: Member tìm nhà hàng theo địa chỉ nhập và ngưỡng đường bộ 500 m/1 km/5 km/10 km, không GPS hoặc liên kết món đã tìm; đề xuất cũ dùng 10 lượt/Member/ngày và không cho Guest gọi. Không triển khai hoặc tạo MVP Issue khi chưa có quyết định scope mới.

---

<a id="fr-43"></a>
### FR-43 — Hiển thị danh sách và bản đồ nhúng nhà hàng từ Google

- **Mã yêu cầu:** FR-43
- **Module:** M11
- **Trạng thái (Derived):** DEFERRED
- **Mô tả:** Nội dung lịch sử của capability đã deferred: hiển thị danh sách/bản đồ nhúng từ Google, ghi nguồn và trạng thái lỗi; không quản lý hồ sơ hoặc lưu nhà hàng yêu thích. Không triển khai hoặc tạo MVP Issue khi chưa có quyết định scope mới.

---

<a id="fr-44"></a>
### FR-44 — Member sửa hoặc xóa Recipe Post đã công khai của chính mình

- **Mã yêu cầu:** FR-44
- **Module:** M02, M03
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Member đã đăng nhập được sửa hoặc xóa Recipe Post đã công khai của chính mình. Thay đổi đạt profile validation tại SRS 3.9 được công khai không cần Admin duyệt lại; bài bị xóa không còn công khai hoặc dùng bởi AI, nhưng các tham chiếu Saved Recipe/Meal Plan/Shopping history hiện có được giữ dạng unavailable/tombstone. Quy tắc riêng cho bài đang bị Admin ẩn vẫn áp dụng.

---

<a id="fr-45"></a>
### FR-45 — Like và Unlike bài công thức, bình luận và phản hồi

- **Mã yêu cầu:** FR-45
- **Module:** M01, M02, M03
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Member Like/Unlike bài, bình luận hoặc reply, tối đa một Like hiệu lực mỗi nội dung; Guest chỉ xem tổng, không có đánh giá sao.

---

<a id="fr-46"></a>
### FR-46 — Hiển thị và quản lý bình luận, reply lồng nhiều cấp

- **Mã yêu cầu:** FR-46
- **Module:** M01, M02, M03, M09
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống hiển thị bình luận cho Guest và cho Member đã đăng nhập tạo bình luận/reply lồng tối đa 5 cấp, đồng thời sửa/xóa bình luận của chính mình. Khi cha bị xóa, hệ thống giữ tombstone và các reply hiện có. Administrator được quản lý bình luận vi phạm.

---

<a id="fr-47"></a>
### FR-47 — Gợi ý bài công thức liên quan thông thường và tùy chọn Gemini

- **Mã yêu cầu:** FR-47
- **Module:** M04, M06
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Gợi ý bài liên quan theo danh mục/nguyên liệu không AI; tùy chọn yêu cầu Gemini riêng có tính lượt.

---

<a id="fr-48"></a>
### FR-48 — Báo cáo bình luận, reply và Administrator hậu kiểm

- **Mã yêu cầu:** FR-48
- **Module:** M03, M09
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Member báo cáo bình luận/reply; Admin hậu kiểm theo quyền riêng tư và nguyên tắc chống trùng báo cáo bài, thích ứng lý do theo loại nội dung. Bình luận/reply công khai ngay.

---

<a id="fr-49"></a>
### FR-49 — Gửi thông báo trong app và email cho reply mới và kết quả báo cáo

- **Mã yêu cầu:** FR-49
- **Module:** M02, M03, M09
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** In-app notification được tạo theo business event reply/moderation. Email gửi bất đồng bộ/best-effort; lỗi email không rollback hành động gốc và có thể được log/retry. Email kết quả moderation phải được thử gửi; email reply có thể tuân theo preference hiện có. Quyền xem báo cáo và bảo vệ danh tính thuộc FR-29/BR-28.

---

<a id="fr-50"></a>
### FR-50 — Xem và xóa lịch sử hội thoại AI riêng theo tài khoản

- **Mã yêu cầu:** FR-50
- **Module:** M02, M06
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Mô tả:** Member xem/xóa lịch sử hội thoại AI riêng; Guest không có lịch sử tài khoản.

---

<a id="fr-51"></a>
### FR-51 — Chatbot giải thích dinh dưỡng chay, BMI/calorie và thay nguyên liệu

- **Mã yêu cầu:** FR-51
- **Module:** M06, M10
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Chatbot giải thích dinh dưỡng chay, BMI/calorie và thay thế nguyên liệu; không tự tạo/sửa công thức hoặc số liệu.

---

<a id="fr-52"></a>
### FR-52 — Tạo, sửa, xóa Blog văn xuôi và nhúng thẻ Recipe Post

- **Mã yêu cầu:** FR-52
- **Module:** M01, M03, M12
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Mô tả:** Hệ thống cho Member tạo/sửa/xóa bài Blog văn xuôi chia sẻ kinh nghiệm ăn chay và nhúng thẻ Recipe Post công khai; Guest xem bài blog; Admin kiểm duyệt.

---

<a id="fr-53"></a>
### FR-53 — Tạo và quản lý Shopping List checklist tương tác

- **Mã yêu cầu:** FR-53
- **Module:** M05
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Member tạo và quản lý Shopping List từ Meal Plan hoặc Recipe đã chọn; hỗ trợ tự thêm item vặt, chỉnh sửa/xóa item và tick đánh dấu đã mua dạng checklist tương tác.

---

<a id="fr-54"></a>
### FR-54 — Tự động tổng hợp nguyên liệu trùng an toàn trong Shopping List

- **Mã yêu cầu:** FR-54
- **Module:** M05
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống chỉ tổng hợp các dòng Shopping List khi cùng `ingredientId`; cho phép quy đổi an toàn `g ↔ kg` và `ml ↔ l`. Đơn vị khác chỉ gom khi giống hệt; không suy diễn mass ↔ volume hoặc `piece` ↔ mass/volume; `vừa đủ` không được cộng số. Dòng không tương thích được giữ riêng và phân nhóm theo danh mục có sẵn.

---

<a id="fr-55"></a>
### FR-55 — Sao chép clipboard và xuất file text Shopping List

- **Mã yêu cầu:** FR-55
- **Module:** M05
- **Trạng thái (Derived):** ACTIVE
- **Mô tả:** Hệ thống cho Member sao chép nhanh nội dung Shopping List vào clipboard hoặc xuất file văn bản .txt dạng checklist để lưu trữ hoặc chia sẻ nhanh.

---

<a id="fr-56"></a>
### FR-56 — Quản lý kho thực phẩm cá nhân và đề xuất món từ kho

- **Mã yêu cầu:** FR-56
- **Module:** M05, M06, M13
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Mô tả:** Hệ thống cho Member quản lý kho thực phẩm cá nhân (Pantry/Inventory); tự động tìm kiếm/đề xuất công thức phù hợp với nguyên liệu đang có và hỗ trợ AI gợi ý món mới từ nguyên liệu trong kho.
