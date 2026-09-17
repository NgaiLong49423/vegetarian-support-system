> **Document:** Business Rules Specification
> **File:** `docs/requirements/srs/BUSINESS-RULES.md`
> **Version:** v1.2.0
> **Created:** 2026-09-14
> **Last Updated:** 2026-09-17
> **Status:** Active
> **Related Docs:** `docs/requirements/SRS.md`, `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md`, `docs/requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md`

# Business Rules Specification

## 1. Mục đích và thẩm quyền tài liệu

Tài liệu này là **Authoritative Detailed Specification** sở hữu các định nghĩa chi tiết cho toàn bộ Business Rules (`BR-01` đến `BR-68`) của Requirements Baseline v1.0.0.

Khung đặc tả gốc và **Authoritative Registry** cho sự tồn tại của quy tắc, mã định danh ổn định (stable ID), và trạng thái vòng đời (lifecycle state) chính thức được duy trì tập trung tại `docs/requirements/SRS.md`.

Trạng thái hiển thị trong tài liệu này là giá trị dẫn xuất (derived) từ root registry `SRS.md`. Nếu phát sinh bất kỳ xung đột nào về trạng thái lifecycle, giá trị trong `SRS.md` luôn là chuẩn có thẩm quyền cao nhất; tài liệu này sẽ được đồng bộ hóa theo `SRS.md`.

Mỗi quy tắc nghiệp vụ được gắn một thẻ stable anchor HTML cố định `<a id="br-xx"></a>` đặt trước tiêu đề để đảm bảo tính bất biến của liên kết tham chiếu, không phụ thuộc vào việc tiêu đề quy tắc có thể thay đổi hoặc được dịch nghĩa trong tương lai.

## 2. Quy tắc lifecycle trong baseline hiện tại

Các trạng thái derived dưới đây được đồng bộ từ root registry, bao gồm quyết định phạm vi M11 ngày 16/09/2026. Không còn BR mang legacy/unknown lifecycle wording. Nội dung `DEFERRED`, `OUT_OF_SCOPE` và `RETIRED` được giữ để bảo toàn lịch sử và không tạo implementation scope hiện tại.

## 3. Danh mục chi tiết Business Rules

<a id="br-01"></a>
### BR-01 — Quyền sử dụng AI Chat cho Guest và Member Free

- **Mã quy tắc:** BR-01
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Guest và Member Free có quyền sử dụng tính năng AI Chatbot cơ bản (FR-51). Member Free được xác thực theo tài khoản; Guest được trải nghiệm trực tiếp không cần đăng nhập và được bảo vệ bằng cơ chế giới hạn tần suất kỹ thuật (Technical Rate Limiting) theo anonymous cookie/IP để chống spam. Hệ thống không áp dụng daily quota theo số lượt/ngày cho Guest và Member Free.

---

<a id="br-02"></a>
### BR-02 — Phân quyền tính năng AI cho gói Plus và Pro

- **Mã quy tắc:** BR-02
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Gói Plus và Pro mở quyền truy cập các tính năng AI nâng cao theo mô hình Feature-based Entitlement (không giới hạn số lượt request/ngày và không có chu kỳ reset 00:00). Gói Plus mở thêm tính năng AI hỗ trợ soạn nội dung Recipe Post (FR-21) và AI gợi ý biến tấu công thức (FR-47). Gói Pro mở toàn quyền tất cả tính năng AI của sản phẩm, bao gồm tính năng AI tự động lập thực đơn tuần 7 ngày (FR-36).

---

<a id="br-03"></a>
### BR-03 — Xác thực quyền tính năng trước khi gọi dịch vụ AI

- **Mã quy tắc:** BR-03
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Mọi yêu cầu gọi tính năng AI phải được Backend xác thực quyền tính năng (Feature Entitlement) dựa trên hạng gói Subscription hiện tại của tài khoản trước khi gửi yêu cầu tới dịch vụ Gemini; yêu cầu không đủ quyền bị chặn ngay tại server và trả về hướng dẫn nâng cấp gói thích hợp.

---

<a id="br-04"></a>
### BR-04 — Xử lý lỗi provider, timeout AI và lưu trữ telemetry

- **Mã quy tắc:** BR-04
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Lỗi provider hoặc timeout trước khi có phản hồi hợp lệ không được coi là hoàn tất thành công; hệ thống trả về thông báo lỗi thân thiện và ghi error log kỹ thuật. Dữ liệu đo lường kỹ thuật (telemetry token) do provider trả về chỉ dùng để đối soát chi phí vận hành, lưu trữ tối đa 90 ngày và TUYỆT ĐỐI KHÔNG lưu trữ nội dung câu hỏi thô (raw prompt).

---

<a id="br-05"></a>
### BR-05 — Giới hạn tính năng đối với Guest

- **Mã quy tắc:** BR-05
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Guest không có lịch sử AI, hồ sơ hoặc thực đơn cá nhân hóa.

---

<a id="br-06"></a>
### BR-06 — Bảo mật Gemini API Key

- **Mã quy tắc:** BR-06
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Gemini API key chỉ lưu ở backend/biến môi trường; frontend không được biết API key.

---

<a id="br-07"></a>
### BR-07 — Đăng và công khai Recipe Post trực tiếp

- **Mã quy tắc:** BR-07
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Member đã đăng nhập được tạo và công khai Recipe Post trực tiếp sau khi bài đạt validation; không có trạng thái quyền đăng riêng hoặc Admin duyệt trước từng bài.

---

<a id="br-08"></a>
### BR-08 — Giới hạn vai trò AI gắn cờ Recipe Post

- **Mã quy tắc:** BR-08
- **Trạng thái (Derived):** DEFERRED
- **Nội dung:** Nếu AI gắn cờ được triển khai trong tương lai, AI chỉ hỗ trợ gắn cờ và không tự ẩn/xóa bài hoặc khóa tài khoản.

---

<a id="br-09"></a>
### BR-09 — Ranh giới y tế và sức khỏe của câu trả lời AI

- **Mã quy tắc:** BR-09
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Câu trả lời AI phải nêu giới hạn hỗ trợ và không được trình bày như chẩn đoán/điều trị.

---

<a id="br-10"></a>
### BR-10 — Giới hạn định dạng video Phase 1

- **Mã quy tắc:** BR-10
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Phase 1 không nhận upload file video; bài công thức chỉ lưu link hoặc video ID của YouTube.

---

<a id="br-11"></a>
### BR-11 — Media công khai trực tiếp và hậu kiểm

- **Mã quy tắc:** BR-11
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Recipe Post hợp lệ có media do Member đã đăng nhập công khai trực tiếp; media vẫn thuộc chính sách nội dung và có thể bị báo cáo/xử lý sau đăng.

---

<a id="br-12"></a>
### BR-12 — Tính tùy chọn của liên kết nguyên liệu chuẩn

- **Mã quy tắc:** BR-12
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Liên kết nguyên liệu chuẩn là tùy chọn. Tên mới trên công thức không tự trở thành mục chuẩn; việc thiếu liên kết không tự chặn hoàn thiện hoặc công khai bài theo workflow chung.

---

<a id="br-13"></a>
### BR-13 — Xử lý nguyên liệu chưa nhận diện trong ràng buộc ăn uống

- **Mã quy tắc:** BR-13
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Nguyên liệu chưa nhận diện không mặc định thỏa mãn ràng buộc ăn uống; hệ thống không khẳng định món phù hợp với ràng buộc loại trừ khi chưa kiểm tra được thành phần.

---

<a id="br-14"></a>
### BR-14 — Quy tắc định lượng và tổng hợp Shopping List an toàn

- **Mã quy tắc:** BR-14
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Dòng nguyên liệu dùng “vừa đủ” không yêu cầu số lượng/đơn vị, không lưu số giả và không được cộng số. Shopping List chỉ gom các dòng cùng `ingredientId`; cho phép quy đổi `g ↔ kg` và `ml ↔ l`; đơn vị khác chỉ gom khi giống hệt. Không suy diễn mass ↔ volume hoặc `piece` ↔ mass/volume.

---

<a id="br-15"></a>
### BR-15 — Đồng nhất chính sách bài viết tự soạn và bài có AI hỗ trợ

- **Mã quy tắc:** BR-15
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Bài tự viết và bài có AI hỗ trợ đều tuân theo cùng format/chính sách; AI chỉ hỗ trợ nội dung có thể chỉnh sửa trong biểu mẫu, không tự công khai hoặc lưu nháp bền vững.

---

<a id="br-16"></a>
### BR-16 — Không chặn công khai bài viết khi AI lỗi hoặc gói không hỗ trợ tính năng AI

- **Mã quy tắc:** BR-16
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Khi AI gặp sự cố kỹ thuật hoặc tài khoản không thuộc gói có quyền sử dụng AI hỗ trợ soạn bài (FR-21), Member vẫn toàn quyền tự viết, chỉnh sửa và công khai Recipe Post bình thường nếu bài đạt đúng profile validation.

---

<a id="br-17"></a>
### BR-17 — Gắn quyền tác giả với tài khoản đăng bài

- **Mã quy tắc:** BR-17
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Tác giả được gắn với tài khoản Member đăng bài; người đăng không được chọn tài khoản khác đứng tên. Việc AI hỗ trợ soạn nội dung không thay đổi tác giả.

---

<a id="br-18"></a>
### BR-18 — Bảo vệ quyền riêng tư trong thông tin tác giả công khai

- **Mã quy tắc:** BR-18
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Thông tin tác giả công khai (được quản lý trực tiếp trong thực thể User) không tiết lộ email, mật khẩu hoặc hồ sơ dinh dưỡng/sức khỏe riêng tư; gắn tài khoản không được trình bày như xác minh danh tính thật hay chuyên môn.

---

<a id="br-19"></a>
### BR-19 — Điều kiện bắt buộc để công khai Recipe Post

- **Mã quy tắc:** BR-19
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Recipe Post chỉ được công khai khi Member đã đăng nhập và đạt đúng profile: title 3–120 ký tự; 1–50 nguyên liệu; serving 1–50; prep và cook time mỗi giá trị 0–1.440 phút, tổng > 0 (`cookTime = 0` hợp lệ nếu `prepTime > 0`); bắt buộc có nội dung hướng dẫn chuẩn bị/chế biến (`instructions`) không rỗng sau khi cắt khoảng trắng đầu cuối (trim), độ dài từ 10 đến 5.000 ký tự (dạng văn bản tự do); description tối đa 2.000 ký tự (tùy chọn); tối đa 1 ảnh đại diện JPEG/PNG/WebP dung lượng $\le 5$ MB (tùy chọn); tối đa một YouTube link (tùy chọn). Bài không đạt validation không được công khai và không được lưu nháp bền vững trên máy chủ trong phạm vi hiện tại.
- **Cơ chế thực thi và xử lý khi không đạt chuẩn (FE & BE):**
  - **Tầng Máy chủ (Backend — bắt buộc & quyết định):** Toàn bộ quy tắc thẩm định bắt buộc phải được thực thi độc lập tại Backend API (NFR-10). Máy chủ tuyệt đối không tin cậy dữ liệu máy khách; nếu request gửi lên có bất kỳ trường nào không đạt chuẩn (ví dụ: thiếu hướng dẫn chế biến hoặc nội dung hướng dẫn rỗng sau khi trim), máy chủ từ chối yêu cầu công khai/cập nhật và **tuyệt đối không tạo bất kỳ bản ghi dở dang nào vào cơ sở dữ liệu** (phù hợp với FR-24 là OUT_OF_SCOPE).
  - **Tầng Giao diện (Frontend — tối ưu trải nghiệm người dùng):** Giao diện thực hiện kiểm tra trước để phản hồi tức thì; khi dữ liệu không đạt chuẩn, giao diện giữ lại toàn bộ nội dung đã nhập trong biểu mẫu và hiển thị thông báo lỗi cụ thể để người dùng tiếp tục chỉnh sửa mà không bị mất dữ liệu. Việc giữ nội dung biểu mẫu là hành vi giao diện người dùng tạm thời, không tạo bất kỳ lưu nháp nào trên máy chủ.

---

<a id="br-20"></a>
### BR-20 — Tính tùy chọn của mô tả giới thiệu và ảnh đại diện

- **Mã quy tắc:** BR-20
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Mô tả giới thiệu, ảnh đại diện (tối đa 1 ảnh) và đường dẫn YouTube (tối đa 1 link) là tùy chọn khi tạo và công khai bài công thức; bài không có ảnh dùng ảnh mặc định theo phân loại ăn chay. Thời gian nấu (`cookTime`) được phép bằng 0 đối với món không cần nấu nếu thời gian chuẩn bị (`prepTime`) lớn hơn 0 và tổng thời gian thỏa mãn quy định.

---

<a id="br-21"></a>
### BR-21 — Quy tắc đơn xin quyền đăng bài công thức

- **Mã quy tắc:** BR-21
- **Trạng thái (Derived):** RETIRED
- **Nội dung:** Cơ chế trạng thái đơn xin quyền đăng và kiểm tra quyền đăng trước khi tạo/công khai Recipe Post.

---

<a id="br-22"></a>
### BR-22 — Lý do từ chối hoặc thu hồi quyền đăng bài

- **Mã quy tắc:** BR-22
- **Trạng thái (Derived):** RETIRED
- **Nội dung:** Quy tắc Administrator phải nêu lý do khi từ chối đơn xin quyền đăng hoặc thu hồi quyền đăng.

---

<a id="br-23"></a>
### BR-23 — Bản chất của báo cáo vi phạm từ người dùng

- **Mã quy tắc:** BR-23
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Báo cáo của người dùng là tín hiệu để xem xét, không phải kết luận vi phạm; báo cáo cơ bản phải hoạt động khi AI chưa được triển khai hoặc bị lỗi. Nếu AI gắn cờ được triển khai sau này, AI không tự quyết định xử lý Recipe Post.

---

<a id="br-24"></a>
### BR-24 — Xác thực tài khoản khi gửi báo cáo vi phạm

- **Mã quy tắc:** BR-24
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Hệ thống chỉ tiếp nhận báo cáo bài công thức từ tài khoản đã đăng nhập; Guest phải đăng nhập trước. Kiểm tra xác thực được thực hiện ở backend.

---

<a id="br-25"></a>
### BR-25 — Yêu cầu lý do và mô tả trong biểu mẫu báo cáo

- **Mã quy tắc:** BR-25
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Mỗi báo cáo phải có một lý do hợp lệ trong sáu nhóm đã chốt. Nếu chọn “Khác”, mô tả không được rỗng hoặc chỉ chứa khoảng trắng; không tạo báo cáo khi chưa thỏa điều kiện.

---

<a id="br-26"></a>
### BR-26 — Thẩm quyền xử lý báo cáo và áp dụng chế tài

- **Mã quy tắc:** BR-26
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Chỉ Admin quyết định thủ công việc cảnh báo, ẩn/xóa Recipe Post hoặc khóa/mở khóa tài khoản trong quy trình xử lý báo cáo; MVP không dùng ma trận chế tài số tự động và số lượng báo cáo không tự tạo chế tài. Báo cáo đã giải quyết bắt buộc có kết luận/lý do và audit/history phải được giữ.

---

<a id="br-27"></a>
### BR-27 — Quy tắc phục hồi bài công thức bị ẩn

- **Mã quy tắc:** BR-27
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Bài công thức bị ẩn không tự được công khai lại khi tác giả sửa hoặc khi báo cáo đóng; chỉ Admin được khôi phục bài như một quyết định hậu kiểm.

---

<a id="br-28"></a>
### BR-28 — Bảo mật danh tính người báo cáo và tính riêng tư của báo cáo

- **Mã quy tắc:** BR-28
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Báo cáo không công khai. Backend phải giới hạn quyền xem theo 3.13 và không cung cấp danh tính người báo cáo cho tác giả bài công thức bị báo cáo.

---

<a id="br-29"></a>
### BR-29 — Chống tạo báo cáo trùng lặp trên cùng một bài viết

- **Mã quy tắc:** BR-29
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Mỗi tài khoản chỉ có một báo cáo chưa giải quyết trên cùng bài công thức; thông tin mới được bổ sung vào báo cáo đang mở. Báo cáo lại sau khi giải quyết phải mô tả điểm mới.

---

<a id="br-30"></a>
### BR-30 — Ranh giới chức năng khi bỏ qua Onboarding

- **Mã quy tắc:** BR-30
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Bỏ qua Onboarding không khóa các chức năng không cá nhân hóa đã nêu tại 3.15; chỉ chặn yêu cầu AI cá nhân hóa khi thiếu dữ liệu tối thiểu.

---

<a id="br-31"></a>
### BR-31 — Không gọi AI khi chặn do thiếu hồ sơ

- **Mã quy tắc:** BR-31
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Khi chặn AI cá nhân hóa vì thiếu hồ sơ dinh dưỡng/sức khỏe tối thiểu, backend không gửi yêu cầu tới Gemini. Giá trị để trống không được tự hiểu là người dùng xác nhận “Không có”.

---

<a id="br-32"></a>
### BR-32 — Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn

- **Mã quy tắc:** BR-32
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Chỉ Member đã đăng nhập có dữ liệu Công thức đã lưu và Lịch ăn; Guest phải đăng nhập trước khi hệ thống tạo các dữ liệu này.

---

<a id="br-33"></a>
### BR-33 — Độc lập nghiệp vụ của thao tác lưu công thức

- **Mã quy tắc:** BR-33
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Lưu công thức là thao tác dữ liệu nội bộ độc lập, không gọi dịch vụ Gemini AI và không tự cập nhật Onboarding hoặc hồ sơ sở thích.

---

<a id="br-34"></a>
### BR-34 — Tính duy nhất của bản ghi lưu công thức

- **Mã quy tắc:** BR-34
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Mỗi Member chỉ có tối đa một bản ghi lưu cho cùng một bài công thức; yêu cầu lưu lặp không tạo dữ liệu trùng.

---

<a id="br-35"></a>
### BR-35 — Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn

- **Mã quy tắc:** BR-35
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Công thức đã lưu và mục lịch ăn có vòng đời độc lập: bỏ lưu không xóa mục lịch, xóa mục lịch không bỏ lưu. Khi Recipe Post bị hidden/deleted/unavailable, các tham chiếu Saved Recipe, Meal Plan và Shopping history hiện có được giữ dạng unavailable/tombstone, không cascade-delete; bài không còn được khám phá công khai hoặc dùng bởi AI.

---

<a id="br-36"></a>
### BR-36 — Quy tắc 3 loại bữa ăn cố định trong Lịch ăn MVP

- **Mã quy tắc:** BR-36
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Mỗi mục lịch ăn phải thuộc đúng một trong ba loại `Breakfast`, `Lunch` hoặc `Dinner`; MVP không có `Snack` hay loại bữa do người dùng tự tạo.

---

<a id="br-37"></a>
### BR-37 — Tính duy nhất của công thức trong cùng một bữa ăn ngày

- **Mã quy tắc:** BR-37
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Một bữa được có nhiều công thức và không có giới hạn cứng số món trong MVP. Với cùng Member, tổ hợp bài công thức–ngày–loại bữa phải duy nhất; yêu cầu thêm trùng không tạo mục lịch mới.

---

<a id="br-38"></a>
### BR-38 — AI không tự tạo công thức mới ngoài hệ thống

- **Mã quy tắc:** BR-38
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** AI không tạo công thức mới để phục vụ gợi ý hoặc menu và chỉ dùng Recipe Post đang công khai, không hidden/deleted/unavailable. Nếu không có lựa chọn phù hợp, hệ thống phải nói rõ thay vì tạo nội dung không tồn tại.

---

<a id="br-39"></a>
### BR-39 — Vai trò tham khảo của chỉ số BMI

- **Mã quy tắc:** BR-39
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** BMI hoặc mục tiêu cân nặng không được dùng một mình để tự kê calorie/macro target, đánh giá sức khỏe hoặc quyết định món phù hợp; BMI chỉ là chỉ số tham khảo trong hồ sơ rộng hơn. Goal-adjusted behavior nâng cao là stretch, không phải MVP acceptance.

---

<a id="br-40"></a>
### BR-40 — Điều kiện dữ liệu dinh dưỡng tin cậy cho AI menu

- **Mã quy tắc:** BR-40
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Menu và kiểm tra dinh dưỡng chỉ dùng số liệu tính từ gram nguyên liệu/khẩu phần với dữ liệu nội bộ có nguồn USDA/NIH; thiếu dữ liệu phải được công khai và không được AI tự ước lượng như dữ kiện.

---

<a id="br-41"></a>
### BR-41 — Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia

- **Mã quy tắc:** BR-41
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** “Thấp hơn mức tham khảo” trong một ngày không được trình bày là chẩn đoán thiếu chất; mọi kết quả calorie/dưỡng chất là ước tính hỗ trợ lập kế hoạch và không thay thế chuyên gia.

---

<a id="br-42"></a>
### BR-42 — Đối tượng loại trừ khỏi tính toán nhu cầu dinh dưỡng MVP

- **Mã quy tắc:** BR-42
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** MVP không cung cấp tính nhu cầu, AI menu dinh dưỡng hoặc kiểm tra menu ngày cho người dưới 18 tuổi, người mang thai/cho con bú hoặc người cần chế độ ăn điều trị; giới hạn này không được khóa chức năng thông thường của họ.

---

<a id="br-43"></a>
### BR-43 — Gắn số liệu dinh dưỡng với một khẩu phần

- **Mã quy tắc:** BR-43
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Số liệu dinh dưỡng trên trang công thức phải gắn với một khẩu phần; tỷ lệ tham chiếu chung không được trình bày như mục tiêu cá nhân. Kiểm tra menu ngày phải sử dụng số khẩu phần đã ghi nhận và mức tham khảo cá nhân.

---

<a id="br-44"></a>
### BR-44 — Quy tắc giải thích chỉ tiêu natri và năng lượng

- **Mã quy tắc:** BR-44
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Không áp dụng cùng một cách hiểu phần trăm cho mọi chỉ tiêu: natri được theo dõi theo giới hạn tối đa, còn trạng thái của năng lượng và chất đa lượng dựa trên khoảng tham khảo đã chốt. Không khuyến khích người dùng tăng natri chỉ để đạt 100%.

---

<a id="br-45"></a>
### BR-45 — Cấm hiển thị điểm tổng hợp hoặc nhãn đơn giản hóa

- **Mã quy tắc:** BR-45
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** MVP không tạo điểm cân bằng dinh dưỡng tổng hợp và không hiển thị Glycemic Index/Glycemic Load. Hệ thống phải trình bày từng chỉ tiêu và không được che giấu dữ liệu thiếu bằng một nhãn “Tốt/Xấu”.

---

<a id="br-46"></a>
### BR-46 — Nguồn tính toán dinh dưỡng chính thức của công thức

- **Mã quy tắc:** BR-46
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Dinh dưỡng công thức phải được suy ra từ danh mục nguyên liệu dinh dưỡng nội bộ và định lượng đã khai báo. Bảng tổng do tác giả tự nhập hoặc con số do AI sinh ra không được coi là dữ liệu dinh dưỡng đã xác minh.

---

<a id="br-47"></a>
### BR-47 — Phân bổ dinh dưỡng theo số khẩu phần và cộng dồn ngày

- **Mã quy tắc:** BR-47
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Tổng dinh dưỡng toàn công thức được phân bổ theo số khẩu phần của công thức. Khi thêm vào Lịch ăn, mặc định là một khẩu phần nhưng Member được thay đổi số khẩu phần dự định ăn; tổng ngày phải cộng mọi mục thuộc cả ba bữa theo lượng này.

---

<a id="br-48"></a>
### BR-48 — Xử lý nguyên liệu thiếu định lượng hoặc thiếu số liệu dinh dưỡng

- **Mã quy tắc:** BR-48
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Nguyên liệu thiếu định lượng, dùng “vừa đủ”, không quy đổi được hoặc chưa có dữ liệu dinh dưỡng không được tự tính là 0. Hệ thống phải đánh dấu kết quả chưa đầy đủ và AI không được che giấu hoặc tự bù dữ liệu thiếu.

---

<a id="br-49"></a>
### BR-49 — Không gọi API dinh dưỡng ngoài realtime và nguồn tham khảo

- **Mã quy tắc:** BR-49
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** MVP không gọi API dinh dưỡng hoặc dùng AI để tự đối chiếu/tạo số liệu khi người dùng thao tác. Dữ liệu nội bộ phải giữ nguồn tham khảo; USDA FoodData Central là nguồn tham khảo chính cho bộ dữ liệu ban đầu.

---

<a id="br-50"></a>
### BR-50 — Cho phép công khai bài chứa nguyên liệu ngoài danh mục dinh dưỡng

- **Mã quy tắc:** BR-50
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Nguyên liệu ngoài danh mục dinh dưỡng không chặn công khai Recipe Post theo workflow chung. Bài có kết quả dinh dưỡng chưa đầy đủ không được sử dụng trong AI menu có mục tiêu dinh dưỡng.

---

<a id="br-51"></a>
### BR-51 — Thẩm quyền quản lý danh mục nguyên liệu dinh dưỡng

- **Mã quy tắc:** BR-51
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Chỉ Administrator được quản lý dữ liệu trong danh mục nguyên liệu dinh dưỡng. Tác giả công thức và AI không được trực tiếp tạo hoặc thay đổi các giá trị dinh dưỡng dùng làm dữ liệu tính toán chính thức.

---

<a id="br-52"></a>
### BR-52 — Yêu cầu đầy đủ 9 chỉ tiêu và nguồn trước khi kích hoạt

- **Mã quy tắc:** BR-52
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Mỗi nguyên liệu dinh dưỡng được thêm/sửa phải có đủ chín giá trị trên 100 g, tên nguồn, đường dẫn nguồn và ngày tham khảo hợp lệ trước khi được bật để tính toán chính thức.

---

<a id="br-53"></a>
### BR-53 — Cấm xóa vĩnh viễn nguyên liệu dinh dưỡng đã tham chiếu

- **Mã quy tắc:** BR-53
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** MVP không xóa vĩnh viễn nguyên liệu dinh dưỡng đã được tham chiếu. Administrator dùng trạng thái ngừng hỗ trợ; dữ liệu ngừng hỗ trợ không được chọn cho liên kết mới nhưng vẫn phải giữ khả năng truy vết công thức đã sử dụng.

---

<a id="br-54"></a>
### BR-54 — Cấm nhập hàng loạt tự động vào danh mục dinh dưỡng

- **Mã quy tắc:** BR-54
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** MVP không nhập hàng loạt, không tự gọi USDA và không cho AI tự điền dữ liệu vào danh mục dinh dưỡng.

---

<a id="br-55"></a>
### BR-55 — Không sử dụng GPS thiết bị khi tìm nhà hàng

- **Mã quy tắc:** BR-55
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Nội dung:** Ràng buộc lịch sử: nếu M11 được đưa lại vào scope bằng một quyết định và phân rã mới, chức năng dùng địa chỉ/địa điểm người dùng chủ động nhập/chọn và không xin quyền, đọc hoặc lưu GPS/vị trí hiện tại của thiết bị.

---

<a id="br-56"></a>
### BR-56 — Nguồn dữ liệu nhà hàng từ Google Maps Platform

- **Mã quy tắc:** BR-56
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Nội dung:** Ràng buộc lịch sử: đề xuất cũ yêu cầu kết quả nhà hàng đến từ Google Maps Platform, không do Gemini tạo ra, không trừ lượt AI và không được trình bày phân loại của Google như xác minh độc lập của hệ thống.

---

<a id="br-57"></a>
### BR-57 — Xử lý lỗi không tìm thấy địa chỉ hoặc vượt hạn mức Google

- **Mã quy tắc:** BR-57
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Nội dung:** Ràng buộc lịch sử: đề xuất cũ yêu cầu thông báo đúng trạng thái khi địa chỉ không xác định được, không có kết quả hoặc dịch vụ Google lỗi/hết hạn mức, đồng thời không tạo địa điểm thay thế không có nguồn.

---

<a id="br-58"></a>
### BR-58 — Độc lập giữa thao tác gửi báo cáo và quyết định xử lý

- **Mã quy tắc:** BR-58
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Gửi báo cáo và xử lý báo cáo là hai nghiệp vụ độc lập. Một báo cáo không tự ẩn/xóa Recipe Post hoặc khóa tài khoản nếu Admin chưa quyết định.

---

<a id="br-59"></a>
### BR-59 — Không có hàng đợi duyệt bài trước khi công khai

- **Mã quy tắc:** BR-59
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** MVP không có hàng đợi duyệt từng Recipe Post. Bài hợp lệ của Member đã đăng nhập được công khai trực tiếp khi tác giả chủ động công khai.

---

<a id="br-60"></a>
### BR-60 — Xác nhận trách nhiệm trước khi xin quyền đăng bài

- **Mã quy tắc:** BR-60
- **Trạng thái (Derived):** RETIRED
- **Nội dung:** Quy tắc xác nhận chính sách, trách nhiệm và lý do đóng góp trước khi gửi đơn xin quyền đăng.

---

<a id="br-61"></a>
### BR-61 — Giới hạn số đơn xin quyền đăng bài Chờ duyệt

- **Mã quy tắc:** BR-61
- **Trạng thái (Derived):** RETIRED
- **Nội dung:** Quy tắc mỗi Member có tối đa một đơn xin quyền đăng Chờ duyệt và được gửi lại sau khi bị từ chối.

---

<a id="br-62"></a>
### BR-62 — Kiểm tra quyền tác giả và validation khi sửa bài công thức

- **Mã quy tắc:** BR-62
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Khi sửa Recipe Post đang công khai, backend phải kiểm tra tác giả và các trường bắt buộc. Bài bị Administrator ẩn không được tự công khai lại qua chức năng sửa.

---

<a id="br-63"></a>
### BR-63 — Đề xuất nhà hàng theo bán kính và không duy trì danh mục riêng

- **Mã quy tắc:** BR-63
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Nội dung:** Ràng buộc lịch sử: đề xuất cũ chỉ dựa trên địa chỉ/bán kính và dữ liệu Google Maps Platform, không dựa trên món đã tìm. Hệ thống không duy trì danh mục nhà hàng do Administrator quản lý và không được trình bày dữ liệu ngoài như dữ liệu đã được app xác minh.

---

<a id="br-64"></a>
### BR-64 — Quyền sửa và xóa bài công thức của chính tác giả

- **Mã quy tắc:** BR-64
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Member đã đăng nhập chỉ được sửa/xóa Recipe Post do chính tài khoản đó đứng tên. Bài do tác giả xóa không còn công khai hoặc được AI gợi ý; bài đang bị Admin ẩn không được tác giả tự khôi phục bằng thao tác quản lý bài cá nhân.

---

<a id="br-65"></a>
### BR-65 — [RETIRED] Quy tắc mỗi Member tối đa một Like hiệu lực

- **Mã quy tắc:** BR-65
- **Trạng thái (Derived):** RETIRED
- **Nội dung:** *(Quy tắc đã bãi bỏ)* Trước đây quy định mỗi cặp Member–bài công thức chỉ có tối đa một Like đang hiệu lực. Theo quyết định tinh gọn baseline sản phẩm ngày 17/09/2026, toàn bộ tính năng Like (Recipe Like, Comment Like, Reply Like) đã bị loại bỏ hoàn toàn khỏi hệ thống để tập trung vào giá trị cốt lõi (nấu ăn, thực đơn dinh dưỡng, đi chợ). Quy tắc này không còn hiệu lực thực thi và không tạo implementation scope.

---

<a id="br-66"></a>
### BR-66 — Quy tắc liên kết và phân quyền với reply bình luận

- **Mã quy tắc:** BR-66
- **Trạng thái (Derived):** ACTIVE
- **Nội dung:** Mỗi reply phải thuộc cùng bài công thức với bình luận cha và độ sâu tối đa là 5. Khi cha bị xóa, hệ thống giữ tombstone và các reply hiện có. Chỉ chủ sở hữu được sửa/xóa bình luận của mình; quyền quản trị nội dung vi phạm của Administrator vẫn được áp dụng.

---

<a id="br-67"></a>
### BR-67 — Xác thực Member trước khi gọi Google Maps Platform

- **Mã quy tắc:** BR-67
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Nội dung:** Ràng buộc lịch sử: đề xuất cũ chỉ cho Backend gửi yêu cầu tìm nhà hàng tới Google Maps Platform sau khi xác thực Member; yêu cầu của Guest bị chặn trước khi gọi dịch vụ ngoài và không tiêu thụ quota tìm kiếm của ứng dụng.

---

<a id="br-68"></a>
### BR-68 — 4 ngưỡng bán kính tìm nhà hàng cố định

- **Mã quy tắc:** BR-68
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Nội dung:** Ràng buộc lịch sử: nếu M11 được đưa lại vào scope bằng một quyết định và phân rã mới, đề xuất cũ chỉ nhận bán kính 500 m, 1 km, 5 km hoặc 10 km; giá trị khác bị từ chối trước khi gọi Google Maps Platform.
