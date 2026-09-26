> **Document:** Business Rules Specification
> **File:** `docs/requirements/srs/BUSINESS-RULES.md`
> **Version:** v2.1.0
> **Created:** 2026-09-14
> **Last Updated:** 2026-09-27
> **Status:** Active
> **Related Docs:** `docs/requirements/SRS.md`, `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md`, `docs/requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md`

# Business Rules

This document contains only requirements included in Requirements / Implementation Baseline v2.0.0. Stable identifiers are preserved; historical requirements remain in the archived v1 snapshot.

<a id="br-01"></a>
### BR-01 — Quyền sử dụng AI Chat cho Guest và Member Free

- **Mã quy tắc:** BR-01
- **Nội dung:** Guest và Member Free có quyền sử dụng tính năng AI Chatbot cơ bản (FR-51). Member Free được xác thực theo tài khoản; Guest được trải nghiệm trực tiếp không cần đăng nhập và được bảo vệ bằng cơ chế giới hạn tần suất kỹ thuật (Technical Rate Limiting) theo anonymous cookie/IP để chống spam. Hệ thống không áp dụng daily quota theo số lượt/ngày cho Guest và Member Free.

---

<a id="br-02"></a>
### BR-02 — Phân quyền tính năng AI cho gói Plus và Pro

- **Mã quy tắc:** BR-02
- **Nội dung:** Gói Plus và Pro mở quyền truy cập các tính năng AI nâng cao theo mô hình Feature-based Entitlement (không giới hạn số lượt request/ngày và không có chu kỳ reset 00:00). Gói Plus mở thêm tính năng AI hỗ trợ soạn nội dung Recipe Post (FR-21) và AI gợi ý biến tấu công thức (FR-47). Gói Pro mở toàn quyền tất cả tính năng AI của sản phẩm, bao gồm tính năng AI tự động lập thực đơn tuần 7 ngày (FR-36).

---

<a id="br-03"></a>
### BR-03 — Xác thực quyền tính năng trước khi gọi dịch vụ AI

- **Mã quy tắc:** BR-03
- **Nội dung:** Mọi yêu cầu gọi tính năng AI phải được Backend xác thực quyền tính năng (Feature Entitlement) dựa trên hạng gói Subscription hiện tại của tài khoản trước khi gửi yêu cầu tới dịch vụ Gemini; yêu cầu không đủ quyền bị chặn ngay tại server và trả về hướng dẫn nâng cấp gói thích hợp.

---

<a id="br-04"></a>
### BR-04 — Xử lý lỗi provider, timeout AI và lưu trữ telemetry

- **Mã quy tắc:** BR-04
- **Nội dung:** Lỗi provider hoặc timeout trước khi có phản hồi hợp lệ không được coi là hoàn tất thành công; hệ thống trả về thông báo lỗi thân thiện và ghi error log kỹ thuật. Dữ liệu đo lường kỹ thuật (telemetry token) do provider trả về chỉ dùng để đối soát chi phí vận hành, lưu trữ tối đa 90 ngày và TUYỆT ĐỐI KHÔNG lưu trữ nội dung câu hỏi thô (raw prompt).

---

<a id="br-05"></a>
### BR-05 — Giới hạn tính năng đối với Guest

- **Mã quy tắc:** BR-05
- **Nội dung:** Guest không có lịch sử AI, hồ sơ hoặc thực đơn cá nhân hóa.

---

<a id="br-06"></a>
### BR-06 — Bảo mật Gemini API Key

- **Mã quy tắc:** BR-06
- **Nội dung:** Gemini API key chỉ lưu ở backend/biến môi trường; frontend không được biết API key.

---

<a id="br-07"></a>
### BR-07 — Quyền đăng và công khai Recipe Post dành riêng cho Chuyên gia

- **Mã quy tắc:** BR-07
- **Nội dung:** Quyền tạo, sửa, xóa và công khai Recipe Post dành riêng cho Chuyên gia (`Role = EXPERT`) đã được Administrator phê duyệt qua đơn đăng ký theo format (`FR-05`); `Customer`, `Guest` và `Administrator` không có quyền tạo/đăng hoặc sửa nội dung bài công thức. Backend phải từ chối thao tác trái quyền; `Guest` chưa xác thực nhận `401 Unauthorized`, tài khoản đã xác thực nhưng không có quyền nhận `403 Forbidden`. Chuyên gia công khai bài trực tiếp khi đạt validation cấu trúc mà không phải qua duyệt từng bài. Quyền hậu kiểm của Administrator theo FR-06 không phải quyền tác giả.

---

<a id="br-09"></a>
### BR-09 — Ranh giới y tế và sức khỏe của câu trả lời AI

- **Mã quy tắc:** BR-09
- **Nội dung:** Câu trả lời AI phải nêu giới hạn hỗ trợ và không được trình bày như chẩn đoán/điều trị.

---

<a id="br-10"></a>
### BR-10 — Giới hạn định dạng video Phase 1

- **Mã quy tắc:** BR-10
- **Nội dung:** Phase 1 không nhận upload file video; bài công thức chỉ lưu link hoặc video ID của YouTube.

---

<a id="br-11"></a>
### BR-11 — Media công khai trực tiếp và hậu kiểm

- **Mã quy tắc:** BR-11
- **Nội dung:** Recipe Post hợp lệ có media do Member đã đăng nhập công khai trực tiếp; media vẫn thuộc chính sách nội dung và có thể bị báo cáo/xử lý sau đăng.

---

<a id="br-12"></a>
### BR-12 — Tính tùy chọn của liên kết nguyên liệu chuẩn

- **Mã quy tắc:** BR-12
- **Nội dung:** Liên kết nguyên liệu chuẩn là tùy chọn. Tên mới trên công thức không tự trở thành mục chuẩn; việc thiếu liên kết không tự chặn hoàn thiện hoặc công khai bài theo workflow chung.

---

<a id="br-13"></a>
### BR-13 — Xử lý nguyên liệu chưa nhận diện trong ràng buộc ăn uống

- **Mã quy tắc:** BR-13
- **Nội dung:** Nguyên liệu chưa nhận diện không mặc định thỏa mãn ràng buộc ăn uống; hệ thống không khẳng định món phù hợp với ràng buộc loại trừ khi chưa kiểm tra được thành phần.

---

<a id="br-14"></a>
### BR-14 — Quy tắc định lượng nguyên liệu số học và tổng hợp Shopping List an toàn theo đơn vị chuẩn

- **Mã quy tắc:** BR-14
- **Nội dung:** Bãi bỏ hoàn toàn khái niệm "vừa đủ" hoặc để trống định lượng. Mọi dòng nguyên liệu bắt buộc phải có số lượng số học dương (`quantity > 0`) và đơn vị đo lường hợp lệ thuộc danh mục `UNIT` (`MASS`, `VOLUME`, `COUNT`). Shopping List chỉ gom các dòng cùng `ingredientId`. Tự động quy đổi trong cùng thứ nguyên: `g ↔ kg` ($1\text{ kg} = 1.000\text{ g}$) và `ml ↔ l` ($1\text{ l} = 1.000\text{ ml}$). Quy đổi khác thứ nguyên (ví dụ `COUNT` dạng quả/củ hoặc `VOLUME` dạng muỗng/thìa/ml sang khối lượng `g`) BẮT BUỘC phải dựa trên bảng tỷ lệ quy đổi cụ thể của nguyên liệu đó trong `INGREDIENT_UNIT_CONVERSION`. Nếu nguyên liệu chưa có tỷ lệ quy đổi trong hệ thống, xem BR-73 về quy tắc chặn xuất bản; trên Shopping List nếu có các dòng không quy đổi được, hệ thống giữ nguyên các dòng riêng rẽ, tuyệt đối không suy diễn tùy tiện hay cộng dồn sai lệch.

---

<a id="br-15"></a>
### BR-15 — Đồng nhất chính sách bài viết tự soạn và bài có AI hỗ trợ

- **Mã quy tắc:** BR-15
- **Nội dung:** Bài tự viết và bài có AI hỗ trợ đều tuân theo cùng format/chính sách; AI chỉ hỗ trợ nội dung có thể chỉnh sửa trong biểu mẫu, không tự công khai hoặc lưu nháp bền vững.

---

<a id="br-16"></a>
### BR-16 — Không chặn công khai bài viết khi AI lỗi hoặc gói không hỗ trợ tính năng AI

- **Mã quy tắc:** BR-16
- **Nội dung:** Khi AI gặp sự cố kỹ thuật hoặc tài khoản Chuyên gia không thuộc gói có quyền sử dụng AI hỗ trợ soạn bài (FR-21), Chuyên gia vẫn toàn quyền tự viết, chỉnh sửa và công khai Recipe Post của mình nếu bài đạt đúng profile validation.

---

<a id="br-17"></a>
### BR-17 — Gắn quyền tác giả với tài khoản đăng bài

- **Mã quy tắc:** BR-17
- **Nội dung:** Tác giả được gắn với tài khoản Chuyên gia đăng bài; người đăng không được chọn tài khoản khác đứng tên. Việc AI hỗ trợ soạn nội dung không thay đổi tác giả; Administrator hậu kiểm không trở thành tác giả.

---

<a id="br-18"></a>
### BR-18 — Bảo vệ quyền riêng tư trong thông tin tác giả công khai

- **Mã quy tắc:** BR-18
- **Nội dung:** Thông tin tác giả công khai (được quản lý trực tiếp trong thực thể User) không tiết lộ email, mật khẩu hoặc hồ sơ dinh dưỡng/sức khỏe riêng tư; gắn tài khoản không được trình bày như xác minh danh tính thật hay chuyên môn.

---

<a id="br-19"></a>
### BR-19 — Điều kiện bắt buộc để công khai Recipe Post (hướng dẫn chế biến linh hoạt, 0..5 RECIPE_MEDIA, đơn vị quy đổi hợp lệ)

- **Mã quy tắc:** BR-19
- **Nội dung:** Recipe Post chỉ được công khai khi Chuyên gia/tác giả đã đăng nhập và đạt đúng profile validation bắt buộc:
  1. Tiêu đề (`title`): 3–120 ký tự.
  2. Thể loại món (`dish_category`): Bắt buộc chọn 1 giá trị chuẩn hóa thuộc danh mục thể loại món (`món nước`, `món xào`, `món lẩu`, `món kho`, `món canh`, `món chiên`, `món hấp`, `món gỏi / salad`, `món cuốn`, `món nướng`, `món tráng miệng / chè`). Không sử dụng bảng `CATEGORY` hay `RECIPE_CATEGORY` đa tầng động mà lưu trực tiếp thuộc tính chuẩn hóa trên Recipe Post.
  3. Nguyên liệu (`ingredients`): 1–50 dòng. Mỗi dòng bắt buộc: tên/liên kết nguyên liệu, định lượng số học dương (`quantity > 0`), và đơn vị đo lường hợp lệ thuộc `UNIT`. Cấm tuyệt đối giá trị phi số học ("vừa đủ").
  4. Ràng buộc quy đổi đơn vị: Nếu nguyên liệu sử dụng đơn vị cần quy đổi sang gam (để tính dinh dưỡng hoặc tổng hợp danh sách mua sắm) mà chưa có tỷ lệ quy đổi tương ứng trong `INGREDIENT_UNIT_CONVERSION` (ví dụ: dùng đơn vị đếm `COUNT` dạng quả/củ/trái hoặc đơn vị thể tích dạng muỗng/thìa/ml sang gam mà nguyên liệu chưa được cấu hình hệ số), hệ thống **BẮT BUỘC CHẶN XUẤT BẢN (Validation Error)**. Không cho phép công khai bài viết khi thiếu dữ liệu quy đổi (BR-73).
  5. Hướng dẫn chuẩn bị/chế biến: Người đăng **không bắt buộc phải viết từng bước nấu ăn** vào bài viết. Tác giả có thể nhập nội dung hướng dẫn chế biến dưới dạng văn bản tự do/tổng thể (`instructions`) từ 10 đến 5.000 ký tự hoặc chia theo bước tùy ý; hệ thống không ép buộc phải phân rã thành các bước độc lập hay quản lý theo chế độ step-to-step, giúp giảm rào cản nhập liệu.
  6. Khẩu phần (`serving`): 1–50.
  7. Thời gian: `prepTime` và `cookTime` mỗi giá trị 0–1.440 phút, tổng thời gian > 0 (`cookTime = 0` hợp lệ nếu `prepTime > 0`).
  8. Phân loại ăn chay: Chọn 1 trong 4 loại chuẩn (Vegan, Lacto Vegetarian, Ovo Vegetarian, Lacto-Ovo Vegetarian).
  9. Mô tả giới thiệu (`description`): Tối đa 2.000 ký tự (tùy chọn).
  10. Media: Tối đa 5 ảnh trên mỗi bài công thức (`RECIPE_MEDIA`), định dạng JPEG/PNG/WebP, dung lượng $\le 5$ MB/ảnh (tùy chọn). Nếu bài có từ 1 ảnh trở lên, **BẮT BUỘC phải có đúng 1 ảnh được đánh dấu làm ảnh bìa (`is_cover = true`)**. Đường dẫn YouTube (tối đa 1 link tùy chọn) được lưu trực tiếp trên Recipe Post.
- **Cơ chế thực thi và xử lý khi không đạt chuẩn (FE & BE):**
  - **Tầng Máy chủ (Backend — bắt buộc & quyết định):** Toàn bộ quy tắc thẩm định bắt buộc phải được thực thi độc lập tại Backend API (NFR-10). Máy chủ tuyệt đối không tin cậy dữ liệu máy khách; nếu request gửi lên có bất kỳ trường nào không đạt chuẩn (ví dụ: thiếu hướng dẫn chế biến, thiếu thể loại món `dish_category`, thiếu ảnh bìa khi có upload ảnh, hoặc nguyên liệu chưa có tỷ lệ quy đổi sang gam), máy chủ từ chối yêu cầu công khai/cập nhật và **tuyệt đối không tạo bất kỳ bản ghi dở dang nào vào cơ sở dữ liệu** (phù hợp với FR-24 là OUT_OF_SCOPE).
  - **Tầng Giao diện (Frontend — tối ưu trải nghiệm người dùng):** Giao diện thực hiện kiểm tra trước để phản hồi tức thì; khi dữ liệu không đạt chuẩn, giao diện giữ lại toàn bộ nội dung đã nhập trong biểu mẫu và hiển thị thông báo lỗi cụ thể để người dùng tiếp tục chỉnh sửa mà không bị mất dữ liệu. Việc giữ nội dung biểu mẫu là hành vi giao diện người dùng tạm thời, không tạo bất kỳ lưu nháp nào trên máy chủ.

---

<a id="br-20"></a>
### BR-20 — Tính tùy chọn của mô tả giới thiệu và ảnh đại diện (tối đa 5 ảnh, đúng 1 ảnh bìa)

- **Mã quy tắc:** BR-20
- **Nội dung:** Mô tả giới thiệu, thư viện ảnh (0..5 ảnh trong `RECIPE_MEDIA`) và đường dẫn YouTube (tối đa 1 link) là tùy chọn khi tạo và công khai bài công thức. Khi bài có từ 1 đến 5 ảnh, bắt buộc có đúng 1 ảnh được chọn làm ảnh bìa (cover image) để hiển thị trên thẻ món; bài không có ảnh nào (0 ảnh) sẽ dùng ảnh mặc định của hệ thống theo phân loại ăn chay. Thời gian nấu (`cookTime`) được phép bằng 0 đối với món không cần nấu nếu thời gian chuẩn bị (`prepTime`) lớn hơn 0 và tổng thời gian thỏa mãn quy định.

---

<a id="br-23"></a>
### BR-23 — Bản chất của báo cáo vi phạm từ người dùng

- **Mã quy tắc:** BR-23
- **Nội dung:** Báo cáo của người dùng là tín hiệu để xem xét, không phải kết luận vi phạm; báo cáo cơ bản phải hoạt động khi AI chưa được triển khai hoặc bị lỗi. Nếu AI gắn cờ được triển khai sau này, AI không tự quyết định xử lý Recipe Post.

---

<a id="br-24"></a>
### BR-24 — Xác thực tài khoản khi gửi báo cáo vi phạm

- **Mã quy tắc:** BR-24
- **Nội dung:** Hệ thống chỉ tiếp nhận báo cáo bài công thức từ tài khoản đã đăng nhập; Guest phải đăng nhập trước. Kiểm tra xác thực được thực hiện ở backend.

---

<a id="br-25"></a>
### BR-25 — Yêu cầu lý do và mô tả trong biểu mẫu báo cáo

- **Mã quy tắc:** BR-25
- **Nội dung:** Mỗi báo cáo phải có một lý do hợp lệ trong sáu nhóm đã chốt. Nếu chọn “Khác”, mô tả không được rỗng hoặc chỉ chứa khoảng trắng; không tạo báo cáo khi chưa thỏa điều kiện.

---

<a id="br-26"></a>
### BR-26 — Thẩm quyền xử lý báo cáo và áp dụng chế tài

- **Mã quy tắc:** BR-26
- **Nội dung:** Chỉ Admin quyết định thủ công việc cảnh báo, ẩn/xóa Recipe Post hoặc khóa/mở khóa tài khoản trong quy trình xử lý báo cáo; MVP không dùng ma trận chế tài số tự động và số lượng báo cáo không tự tạo chế tài. Báo cáo đã giải quyết bắt buộc có kết luận/lý do và audit/history phải được giữ.

---

<a id="br-27"></a>
### BR-27 — Quy tắc phục hồi bài công thức bị ẩn

- **Mã quy tắc:** BR-27
- **Nội dung:** Bài công thức bị ẩn không tự được công khai lại khi tác giả sửa hoặc khi báo cáo đóng; chỉ Admin được khôi phục bài như một quyết định hậu kiểm.

---

<a id="br-28"></a>
### BR-28 — Bảo mật danh tính người báo cáo và tính riêng tư của báo cáo

- **Mã quy tắc:** BR-28
- **Nội dung:** Báo cáo không công khai. Backend phải giới hạn quyền xem theo 3.13 và không cung cấp danh tính người báo cáo cho tác giả bài công thức bị báo cáo.

---

<a id="br-29"></a>
### BR-29 — Chống tạo báo cáo trùng lặp trên cùng một bài viết

- **Mã quy tắc:** BR-29
- **Nội dung:** Mỗi tài khoản chỉ có một báo cáo chưa giải quyết trên cùng bài công thức; thông tin mới được bổ sung vào báo cáo đang mở. Báo cáo lại sau khi giải quyết phải mô tả điểm mới.

---

<a id="br-30"></a>
### BR-30 — Ranh giới chức năng khi bỏ qua Onboarding

- **Mã quy tắc:** BR-30
- **Nội dung:** Bỏ qua Onboarding không khóa các chức năng không cá nhân hóa đã nêu tại 3.15; chỉ chặn yêu cầu AI cá nhân hóa khi thiếu dữ liệu tối thiểu.

---

<a id="br-31"></a>
### BR-31 — Không gọi AI khi chặn do thiếu hồ sơ

- **Mã quy tắc:** BR-31
- **Nội dung:** Khi chặn AI cá nhân hóa vì thiếu hồ sơ dinh dưỡng/sức khỏe tối thiểu, backend không gửi yêu cầu tới Gemini. Giá trị để trống không được tự hiểu là người dùng xác nhận “Không có”.

---

<a id="br-32"></a>
### BR-32 — Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn

- **Mã quy tắc:** BR-32
- **Nội dung:** Chỉ Member đã đăng nhập có dữ liệu Công thức đã lưu và Lịch ăn; Guest phải đăng nhập trước khi hệ thống tạo các dữ liệu này.

---

<a id="br-33"></a>
### BR-33 — Độc lập nghiệp vụ của thao tác lưu công thức

- **Mã quy tắc:** BR-33
- **Nội dung:** Lưu công thức là thao tác dữ liệu nội bộ độc lập, không gọi dịch vụ Gemini AI và không tự cập nhật Onboarding hoặc hồ sơ sở thích.

---

<a id="br-34"></a>
### BR-34 — Tính duy nhất của bản ghi lưu công thức

- **Mã quy tắc:** BR-34
- **Nội dung:** Mỗi Member chỉ có tối đa một bản ghi lưu cho cùng một bài công thức; yêu cầu lưu lặp không tạo dữ liệu trùng.

---

<a id="br-35"></a>
### BR-35 — Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn

- **Mã quy tắc:** BR-35
- **Nội dung:** Công thức đã lưu và mục lịch ăn có vòng đời độc lập: bỏ lưu không xóa mục lịch, xóa mục lịch không bỏ lưu. Khi Recipe Post bị hidden/deleted/unavailable, các tham chiếu Saved Recipe, Meal Plan và Shopping history hiện có được giữ dạng unavailable/tombstone, không cascade-delete; bài không còn được khám phá công khai hoặc dùng bởi AI.

---

<a id="br-36"></a>
### BR-36 — Quy tắc 3 loại bữa ăn cố định trong Lịch ăn MVP

- **Mã quy tắc:** BR-36
- **Nội dung:** Mỗi mục lịch ăn phải thuộc đúng một trong ba loại `Breakfast`, `Lunch` hoặc `Dinner`; MVP không có `Snack` hay loại bữa do người dùng tự tạo.

---

<a id="br-37"></a>
### BR-37 — Tính duy nhất của công thức trong cùng một bữa ăn ngày

- **Mã quy tắc:** BR-37
- **Nội dung:** Một bữa được có nhiều công thức và không có giới hạn cứng số món trong MVP. Với cùng Member, tổ hợp bài công thức–ngày–loại bữa phải duy nhất; yêu cầu thêm trùng không tạo mục lịch mới.

---

<a id="br-38"></a>
### BR-38 — AI không tự tạo công thức mới ngoài hệ thống

- **Mã quy tắc:** BR-38
- **Nội dung:** AI không tạo công thức mới để phục vụ gợi ý hoặc menu và chỉ dùng Recipe Post đang công khai, không hidden/deleted/unavailable. Nếu không có lựa chọn phù hợp, hệ thống phải nói rõ thay vì tạo nội dung không tồn tại.

---

<a id="br-39"></a>
### BR-39 — Vai trò tham khảo của chỉ số BMI

- **Mã quy tắc:** BR-39
- **Nội dung:** BMI hoặc mục tiêu cân nặng không được dùng một mình để tự kê calorie/macro target, đánh giá sức khỏe hoặc quyết định món phù hợp; BMI chỉ là chỉ số tham khảo trong hồ sơ rộng hơn. Goal-adjusted behavior nâng cao là stretch, không phải MVP acceptance.

---

<a id="br-40"></a>
### BR-40 — Điều kiện dữ liệu dinh dưỡng tin cậy cho AI menu

- **Mã quy tắc:** BR-40
- **Nội dung:** Menu và kiểm tra dinh dưỡng chỉ dùng số liệu tính từ gram nguyên liệu/khẩu phần với dữ liệu nội bộ có nguồn USDA/NIH; thiếu dữ liệu phải được công khai và không được AI tự ước lượng như dữ kiện.

---

<a id="br-41"></a>
### BR-41 — Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia

- **Mã quy tắc:** BR-41
- **Nội dung:** “Thấp hơn mức tham khảo” trong một ngày không được trình bày là chẩn đoán thiếu chất; mọi kết quả calorie/dưỡng chất là ước tính hỗ trợ lập kế hoạch và không thay thế chuyên gia.

---

<a id="br-42"></a>
### BR-42 — Đối tượng loại trừ khỏi tính toán nhu cầu dinh dưỡng MVP

- **Mã quy tắc:** BR-42
- **Nội dung:** MVP không cung cấp tính nhu cầu, AI menu dinh dưỡng hoặc kiểm tra menu ngày cho người dưới 18 tuổi, người mang thai/cho con bú hoặc người cần chế độ ăn điều trị; giới hạn này không được khóa chức năng thông thường của họ.

---

<a id="br-43"></a>
### BR-43 — Gắn số liệu dinh dưỡng với một khẩu phần

- **Mã quy tắc:** BR-43
- **Nội dung:** Số liệu dinh dưỡng trên trang công thức phải gắn với một khẩu phần; tỷ lệ tham chiếu chung không được trình bày như mục tiêu cá nhân. Kiểm tra menu ngày phải sử dụng số khẩu phần đã ghi nhận và mức tham khảo cá nhân.

---

<a id="br-44"></a>
### BR-44 — Quy tắc diễn giải mức tham khảo theo từng chỉ tiêu dinh dưỡng

- **Mã quy tắc:** BR-44
- **Nội dung:** Không dùng một tỷ lệ phần trăm hoặc một nhãn chung để thay thế ý nghĩa riêng của từng chỉ tiêu. Năng lượng và các chất đa lượng được so sánh với khoảng tham khảo đã chốt; vi chất được trình bày theo giá trị và mức tham khảo tương ứng. Kết quả thấp hơn hoặc cao hơn chỉ mô tả thực đơn dự kiến, không được diễn giải thành chẩn đoán thiếu/thừa chất của người dùng.

---

<a id="br-45"></a>
### BR-45 — Cấm hiển thị điểm tổng hợp hoặc nhãn đơn giản hóa

- **Mã quy tắc:** BR-45
- **Nội dung:** MVP không tạo điểm cân bằng dinh dưỡng tổng hợp và không hiển thị Glycemic Index/Glycemic Load. Hệ thống phải trình bày từng chỉ tiêu và không được che giấu dữ liệu thiếu bằng một nhãn “Tốt/Xấu”.

---

<a id="br-46"></a>
### BR-46 — Nguồn tính toán dinh dưỡng chính thức của công thức

- **Mã quy tắc:** BR-46
- **Nội dung:** Dinh dưỡng công thức phải được suy ra từ danh mục nguyên liệu dinh dưỡng nội bộ và định lượng đã khai báo. Bảng tổng do tác giả tự nhập hoặc con số do AI sinh ra không được coi là dữ liệu dinh dưỡng đã xác minh.

---

<a id="br-47"></a>
### BR-47 — Phân bổ dinh dưỡng theo số khẩu phần và cộng dồn ngày

- **Mã quy tắc:** BR-47
- **Nội dung:** Tổng dinh dưỡng toàn công thức được phân bổ theo số khẩu phần của công thức. Khi thêm vào Lịch ăn, mặc định là một khẩu phần nhưng Member được thay đổi số khẩu phần dự định ăn; tổng ngày phải cộng mọi mục thuộc cả ba bữa theo lượng này.

---

<a id="br-48"></a>
### BR-48 — Xử lý nguyên liệu thiếu số liệu dinh dưỡng và chặn công khai khi thiếu quy đổi đơn vị

- **Mã quy tắc:** BR-48
- **Nội dung:**
  - Dinh dưỡng món ăn được tính từ lượng khối lượng (gam) của từng nguyên liệu nhân với bảng giá trị dinh dưỡng trên 100g.
  - **Ràng buộc quy đổi đơn vị (Publish Gate):** Đơn vị đo lường của nguyên liệu bắt buộc phải quy đổi được ra gam. Nếu đơn vị nguyên liệu chưa có tỷ lệ quy đổi ra gam trong `INGREDIENT_UNIT_CONVERSION` (và không phải là đơn vị khối lượng `MASS` g/kg), hệ thống **BẮT BUỘC CHẶN CÔNG KHAI BÀI VIẾT (Validation Error)** theo BR-19 và BR-73, tuyệt đối không cho phép công khai khi thiếu tỷ lệ quy đổi.
  - **Dữ liệu dinh dưỡng trong catalog:** Trường hợp nguyên liệu đã có định lượng và quy đổi gam hợp lệ, nhưng bản thân nguyên liệu đó chưa được định nghĩa trong danh mục 9 chỉ tiêu dinh dưỡng của hệ thống: hệ thống vẫn cho phép công khai bài viết (BR-50), nhưng bài viết sẽ được gắn cờ hiển thị "Chưa đủ dữ liệu dinh dưỡng", không được AI đưa vào thực đơn dinh dưỡng chuẩn (BR-40) và tuyệt đối không được tự động gán các chỉ tiêu dinh dưỡng bằng 0.

---

<a id="br-49"></a>
### BR-49 — Không gọi API dinh dưỡng ngoài realtime và nguồn tham khảo

- **Mã quy tắc:** BR-49
- **Nội dung:** MVP không gọi API dinh dưỡng hoặc dùng AI để tự đối chiếu/tạo số liệu khi người dùng thao tác. Dữ liệu nội bộ phải giữ nguồn tham khảo; USDA FoodData Central là nguồn tham khảo chính cho bộ dữ liệu ban đầu.

---

<a id="br-50"></a>
### BR-50 — Cho phép công khai bài chứa nguyên liệu ngoài danh mục dinh dưỡng

- **Mã quy tắc:** BR-50
- **Nội dung:** Nguyên liệu ngoài danh mục dinh dưỡng không chặn công khai Recipe Post theo workflow chung. Bài có kết quả dinh dưỡng chưa đầy đủ không được sử dụng trong AI menu có mục tiêu dinh dưỡng.

---

<a id="br-51"></a>
### BR-51 — Thẩm quyền quản lý danh mục nguyên liệu dinh dưỡng

- **Mã quy tắc:** BR-51
- **Nội dung:** Chỉ Administrator được quản lý dữ liệu trong danh mục nguyên liệu dinh dưỡng. Tác giả công thức và AI không được trực tiếp tạo hoặc thay đổi các giá trị dinh dưỡng dùng làm dữ liệu tính toán chính thức.

---

<a id="br-52"></a>
### BR-52 — Yêu cầu đầy đủ 9 chỉ tiêu và nguồn trước khi kích hoạt

- **Mã quy tắc:** BR-52
- **Nội dung:** Ingredient có thể được lưu khi `nutrition_supported = 0` dù một hoặc nhiều chỉ tiêu dinh dưỡng chưa có dữ liệu; `NULL` biểu thị chưa biết và không được thay bằng `0`. Chỉ được bật `nutrition_supported = 1` để tính toán chính thức khi đủ cả chín giá trị trên 100 g và đầy đủ metadata nguồn tham chiếu (`source_name`, `source_url`, `reference_date`). Các ràng buộc lưu trữ hiện hành của database vẫn áp dụng.

---

<a id="br-53"></a>
### BR-53 — Cấm xóa vĩnh viễn nguyên liệu dinh dưỡng đã tham chiếu

- **Mã quy tắc:** BR-53
- **Nội dung:** MVP không xóa vĩnh viễn nguyên liệu dinh dưỡng đã được tham chiếu. Administrator dùng trạng thái ngừng hỗ trợ; dữ liệu ngừng hỗ trợ không được chọn cho liên kết mới nhưng vẫn phải giữ khả năng truy vết công thức đã sử dụng.

---

<a id="br-54"></a>
### BR-54 — Cấm nhập hàng loạt tự động vào danh mục dinh dưỡng

- **Mã quy tắc:** BR-54
- **Nội dung:** MVP không nhập hàng loạt, không tự gọi USDA và không cho AI tự điền dữ liệu vào danh mục dinh dưỡng.

---

<a id="br-58"></a>
### BR-58 — Độc lập giữa thao tác gửi báo cáo và quyết định xử lý

- **Mã quy tắc:** BR-58
- **Nội dung:** Gửi báo cáo và xử lý báo cáo là hai nghiệp vụ độc lập. Một báo cáo không tự ẩn/xóa Recipe Post hoặc khóa tài khoản nếu Admin chưa quyết định.

---

<a id="br-59"></a>
### BR-59 — Không có hàng đợi duyệt bài trước khi công khai

- **Mã quy tắc:** BR-59
- **Nội dung:** MVP không có hàng đợi duyệt từng Recipe Post. Bài hợp lệ của Chuyên gia đã đăng nhập được công khai trực tiếp khi tác giả chủ động công khai; Administrator chỉ hậu kiểm theo FR-06.

---

<a id="br-62"></a>
### BR-62 — Kiểm tra quyền tác giả và validation khi sửa bài công thức

- **Mã quy tắc:** BR-62
- **Nội dung:** Khi sửa Recipe Post đang công khai, backend phải kiểm tra tác giả và các trường bắt buộc. Bài bị Administrator ẩn không được tự công khai lại qua chức năng sửa.

---

<a id="br-64"></a>
### BR-64 — Quyền sửa và xóa bài công thức của chính tác giả

- **Mã quy tắc:** BR-64
- **Nội dung:** Chuyên gia đã đăng nhập chỉ được sửa/xóa Recipe Post do chính tài khoản đó đứng tên. Bài do tác giả xóa không còn công khai hoặc được AI gợi ý; bài đang bị Admin ẩn không được tác giả tự khôi phục bằng thao tác quản lý bài cá nhân.

---

<a id="br-66"></a>
### BR-66 — Quy tắc liên kết và phân quyền với reply bình luận

- **Mã quy tắc:** BR-66
- **Nội dung:** Mỗi reply phải thuộc cùng bài công thức với bình luận cha và độ sâu tối đa là 5. Khi cha bị xóa, hệ thống giữ tombstone và các reply hiện có. Chỉ chủ sở hữu được sửa/xóa bình luận của mình; quyền quản trị nội dung vi phạm của Administrator vẫn được áp dụng.

---

<a id="br-69"></a>
### BR-69 — Tương tác Like / Dislike và tỷ lệ hài lòng (% Like) của bài công thức

- **Mã quy tắc:** BR-69
- **Nội dung:**
  - Hệ thống áp dụng cơ chế bình chọn Thích / Không thích (Like / Dislike) độc quyền cho bài công thức (`RECIPE_REACTION`). Bình luận và phản hồi không hỗ trợ tính năng Like/Dislike (theo `FR-45` RETIRED).
  - **Quyền hạn tương tác:**
    - Guest (chưa đăng nhập): Chỉ có quyền xem tỷ lệ % Like và tổng số lượt bình chọn; khi Guest nhấp vào nút Like hoặc Dislike, hệ thống hiển thị thông báo yêu cầu đăng nhập (`BR-05`), tuyệt đối không ghi nhận phản hồi vào database.
    - Member đã đăng nhập (Customer hoặc Expert): Có quyền gửi phản hồi `LIKE` hoặc `DISLIKE` cho bài công thức công khai.
    - Tác giả bài viết: Bị cấm tự bình chọn (Like hoặc Dislike) cho bài công thức do chính mình sáng tác; hệ thống trả về lỗi nghiệp vụ nếu tác giả gửi yêu cầu phản hồi trên bài của mình.
  - **Cơ chế chuyển đổi và hủy phản hồi (Toggle & Switch):**
    - Mỗi Member chỉ có tối đa 1 phản hồi hiệu lực trên một bài công thức (`UNIQUE(user_id, recipe_id)`).
    - Nếu Member bấm vào cùng loại phản hồi đã chọn trước đó (ví dụ đang Like mà bấm lại nút Like), hệ thống hủy bỏ phản hồi (Toggle off / Remove reaction). Bản ghi phản hồi bị xóa khỏi trạng thái hiệu lực.
    - Nếu Member bấm vào loại phản hồi đối nghịch (ví dụ đang Like mà bấm nút Dislike), hệ thống tự động cập nhật trạng thái phản hồi sang `DISLIKE` (Switch reaction) và cập nhật lại bộ đếm.
  - **Công thức tính tỷ lệ % Like (Satisfaction Rate / Like Percentage):**
    $$\text{like\_percentage} = \begin{cases} \operatorname{round}\left(\dfrac{\text{total\_likes}}{\text{total\_likes} + \text{total\_dislikes}} \times 100\right), & \text{khi } (\text{total\_likes} + \text{total\_dislikes}) > 0 \\ \text{null / Chưa có dữ liệu}, & \text{khi } (\text{total\_likes} + \text{total\_dislikes}) = 0 \end{cases}$$
  - **Quy tắc hiển thị huy hiệu (Badge Display):**
    - Nếu bài viết có tổng số lượt bình chọn $(\text{total\_likes} + \text{total\_dislikes}) > 0$: Thẻ bài viết (Recipe Card) hiển thị huy hiệu tỷ lệ phần trăm kèm biểu tượng Like ở góc trên bên trái ảnh (ví dụ: `👍 98%`, `👍 96%`, `👍 100%`) theo phong cách Samsung Food. Trang chi tiết bài viết hiển thị thanh tỷ lệ phần trăm kèm số lượt Like và Dislike cụ thể.
    - Nếu bài viết chưa có lượt bình chọn nào $(\text{total\_likes} + \text{total\_dislikes} = 0)$: Thẻ bài viết hiển thị nhãn `Mới` (New) thay vì hiển thị `0%` để tránh gây hiểu lầm tiêu cực cho món ăn mới đăng.
  - **Quy tắc loại bỏ khi ẩn/xóa bài:** Khi bài viết bị ẩn hoặc bị xóa (do tác giả hoặc do Administrator xử lý vi phạm), toàn bộ lượt bình chọn của bài viết lập tức bị loại khỏi các bảng xếp hạng công khai.

---

<a id="br-70"></a>
### BR-70 — Ghi nhận lượt xem và khử trùng lặp theo phiên (RECIPE_VIEW)

- **Mã quy tắc:** BR-70
- **Nội dung:**
  - Hệ thống ghi nhận sự kiện xem bài công thức vào thực thể `RECIPE_VIEW` để phục vụ theo dõi lịch sử, thống kê phân tích và tính điểm xếp hạng.
  - **Cơ chế khử trùng lặp theo phiên (View Deduplication Window):** Nhằm ngăn chặn hành vi spam tải lại trang (F5/refresh) tạo lượt xem ảo, hệ thống áp dụng cửa sổ khử trùng lặp thời gian $T_{\text{dedup}} = 30\text{ phút}$:
    - Đối với Member đã đăng nhập: Căn cứ theo cặp định danh `(user_id, recipe_id)`. Các lượt truy cập lặp lại trong vòng 30 phút từ cùng một tài khoản trên cùng một bài chỉ tính là 1 lượt xem hợp lệ.
    - Đối với Guest (chưa đăng nhập): Căn cứ theo `(session_id / anonymous token / IP hash, recipe_id)`. Các lượt truy cập lặp lại trong vòng 30 phút từ cùng một phiên khách chỉ tính là 1 lượt xem hợp lệ.
  - Mỗi bản ghi `RECIPE_VIEW` lưu trữ timestamp chi tiết phục vụ tổng hợp số lượt xem theo các khung thời gian linh hoạt: 24 giờ qua, 7 ngày qua, 30 ngày qua và toàn thời gian (all-time).

---

<a id="br-71"></a>
### BR-71 — Thuật toán xếp hạng hoạt động sôi nổi nhất (Most Active)

- **Mã quy tắc:** BR-71
- **Nội dung:**
  - Chế độ "Most Active" (Hoạt động sôi nổi nhất) trên trang Khám phá đánh giá mức độ tương tác thực tế của cộng đồng đối với bài công thức trong vòng 7 ngày gần nhất ($t \in [\text{now} - 7\text{ ngày}, \text{now}]$).
  - Điểm hoạt động (Activity Score) được tính theo công thức:
    $$\text{Score}_{\text{active}} = w_v \cdot V_{7d} + w_c \cdot C_{7d} + w_r \cdot R_{7d}$$
    Trong đó:
    - $V_{7d}$: Tổng số lượt xem hợp lệ đã khử trùng lặp trong 7 ngày qua (từ `RECIPE_VIEW`).
    - $C_{7d}$: Tổng số bình luận và phản hồi hợp lệ tạo mới trong 7 ngày qua.
    - $R_{7d}$: Tổng số lượt tương tác phản hồi (Like / Dislike) hợp lệ tạo mới hoặc cập nhật trong 7 ngày qua (từ `RECIPE_REACTION`).
    - Trọng số chuẩn hóa mặc định: $w_v = 1$, $w_c = 5$, $w_r = 10$.
  - **Đặc trưng nghiệp vụ phân biệt với Trending:** Most Active đo lường tổng khối lượng tương tác thực tế gần đây thuần túy, KHÔNG áp dụng hệ số suy giảm thời gian đăng bài (freshness decay). Một bài công thức cũ đã đăng từ lâu nhưng có đợt tương tác thảo luận tăng vọt trong 7 ngày qua vẫn sẽ đạt thứ hạng cao nhất trên bảng Most Active.

---

<a id="br-72"></a>
### BR-72 — Thuật toán xếp hạng thịnh hành (Trending Ranking)

- **Mã quy tắc:** BR-72
- **Nội dung:**
  - Chế độ "Trending" (Thịnh hành) trên trang Khám phá xác định các bài viết đang thu hút sự chú ý nhanh chóng của cộng đồng bằng thuật toán xếp hạng phi AI kết hợp giữa tương tác gần đây và hệ số tươi mới của bài viết (freshness decay).
  - Thuật toán thịnh hành (Trending Algorithm):
    $$\text{Score}_{\text{trending}} = \frac{w_v \cdot V_{3d} + w_c \cdot C_{3d} + w_r \cdot R_{3d} + B_{\text{new}}}{(T_{\text{age\_hours}} + 2)^\gamma}$$
    Trong đó:
    - $V_{3d}, C_{3d}, R_{3d}$: Khối lượng tương tác hợp lệ trong 3 ngày (72 giờ) gần nhất (views từ `RECIPE_VIEW`, comments từ `COMMENT`, reactions từ `RECIPE_REACTION`).
    - $B_{\text{new}}$: Điểm thưởng khởi đầu dành cho bài viết mới công khai trong vòng 48 giờ đầu nhằm tạo cơ hội xuất hiện cho nội dung mới.
    - $T_{\text{age\_hours}}$: Tuổi của bài viết tính từ thời điểm công khai đến hiện tại (tính bằng giờ).
    - $\gamma$: Hệ số suy giảm trọng số theo thời gian (chuẩn hóa mặc định $\gamma = 1.5$).
  - Nhờ mẫu số $(T_{\text{age\_hours}} + 2)^\gamma$, điểm số của bài viết sẽ suy giảm dần theo thời gian, giúp ưu tiên các bài viết mới đăng có tốc độ gia tăng tương tác đột phá và ngăn chặn bài cũ chiếm giữ vị trí thịnh hành quá lâu.

---

<a id="br-73"></a>
### BR-73 — Chuẩn hóa đơn vị đo lường và chặn công khai khi thiếu quy đổi

- **Mã quy tắc:** BR-73
- **Nội dung:**
  - Hệ thống quản lý danh mục đơn vị đo lường chuẩn hóa `UNIT`, phân loại theo 3 thứ nguyên đo lường:
    1. Khối lượng (`MASS`): gam (g), kilogam (kg) với tỷ lệ quy đổi cố định $1\text{ kg} = 1.000\text{ g}$.
    2. Thể tích (`VOLUME`): mililit (ml), lít (l) với tỷ lệ quy đổi cố định $1\text{ l} = 1.000\text{ ml}$.
    3. Đếm số lượng (`COUNT`): quả, củ, trái, tép, bìa, gói, lát, muỗng canh, thìa cà phê,...
  - Bảng quy đổi nguyên liệu `INGREDIENT_UNIT_CONVERSION` lưu trữ tỷ lệ quy đổi cụ thể từ một đơn vị đo lường (thuộc `COUNT` hoặc `VOLUME`) sang khối lượng gam (`MASS` in grams) cho từng nguyên liệu cụ thể (ví dụ: 1 quả chuối tiêu $\approx 120\text{ g}$, 1 bìa đậu phụ $\approx 150\text{ g}$, 1 muỗng canh dầu thực vật $\approx 14\text{ g}$).
  - **Quy tắc chặn xuất bản nghiêm ngặt (Strict Validation Gate):** Khi Chuyên gia tạo hoặc chỉnh sửa bài công thức, nếu bất kỳ nguyên liệu nào sử dụng đơn vị đo lường mà nguyên liệu đó chưa được định nghĩa tỷ lệ quy đổi sang gam trong `INGREDIENT_UNIT_CONVERSION` (và không phải là đơn vị khối lượng `MASS` g/kg đã biết tỷ lệ), Backend **BẮT BUỘC TỪ CHỐI LƯU VÀ CHẶN CÔNG KHAI**, trả về lỗi validation: `"Đơn vị đo lường [Tên đơn vị] của nguyên liệu [Tên nguyên liệu] chưa có tỷ lệ quy đổi sang gam. Vui lòng chọn đơn vị khối lượng (gam/kg) hoặc liên hệ quản trị viên."`
  - Cấm hoàn toàn việc dùng chữ "vừa đủ" hoặc để trống định lượng số học. Mọi dòng nguyên liệu bắt buộc phải có số lượng số học dương (`quantity > 0`) và đơn vị đo lường hợp lệ.

---

<a id="br-74"></a>
### BR-74 — Quy trình xét duyệt đơn đăng ký Chuyên gia và chuyển đổi vai trò

- **Mã quy tắc:** BR-74
- **Nội dung:**
  - Người dùng có vai trò `CUSTOMER` được phép nộp đơn đăng ký Chuyên gia (`EXPERT_APPLICATION`) thông qua biểu mẫu format văn bản có cấu trúc (kinh nghiệm ẩm thực $\ge 20$ ký tự, trường phái chay, tóm tắt công thức sở trường $\ge 30$ ký tự, đúng một link HTTP/HTTPS tham khảo tùy chọn).
  - Đây là quy trình xét duyệt tư cách Chuyên gia và quyền đăng bài dựa trên thông tin tự khai, không phải xác minh danh tính/KYC hoặc xác thực bằng cấp. Hệ thống tuyệt đối không yêu cầu hoặc lưu trữ giấy tờ tùy thân, tệp chứng chỉ hay bằng cấp vật lý.
  - **Quy tắc chặn nộp trùng (Single Open Application Rule):** Mỗi tài khoản Customer chỉ được phép sở hữu tối đa một bản ghi đơn đăng ký ở trạng thái chờ duyệt (`PENDING`). Nếu gửi thêm đơn trong khi đơn cũ chưa được xử lý, Backend từ chối với mã lỗi `HTTP 409 Conflict`.
  - Customer được xem lịch sử các đơn của chính mình theo thứ tự mới nhất trước nhưng không được rút/hủy đơn `PENDING` trong MVP; không được xem đơn của tài khoản khác.
  - **Quy tắc phê duyệt và chuyển đổi vai trò (Instant Role Promotion):** Chỉ khi đơn vẫn `PENDING` và tài khoản nộp vẫn `ACTIVE` với vai trò `CUSTOMER`, Administrator mới được phê duyệt. Hệ thống cập nhật trạng thái đơn thành `APPROVED`, đồng thời cập nhật vai trò người dùng trong `USER` thành `EXPERT`, làm mới trạng thái phân quyền để mở khóa quyền tạo bài viết tại `FR-04` và gửi thông báo in-app.
  - **Quy tắc từ chối bắt buộc lý do (Mandatory Rejection Note):** Khi Administrator từ chối đơn, bắt buộc phải nhập lý do từ chối cụ thể (`admin_note` từ 10 đến 500 ký tự); trạng thái đơn chuyển thành `REJECTED`, vai trò tài khoản vẫn là `CUSTOMER`, thông báo in-app gửi kèm lý do; Customer được quyền nộp đơn mới sau khi đơn cũ bị từ chối.
  - **Quy tắc xử lý đồng thời:** Chỉ quyết định đầu tiên trên một đơn `PENDING` được ghi nhận. Mọi yêu cầu xử lý từ dữ liệu cũ sau khi trạng thái đã đổi phải trả `HTTP 409 Conflict`, không ghi đè kết quả, không cập nhật role và không gửi thông báo lần hai.

---

<a id="br-75"></a>
### BR-75 — Tính duy nhất, có hướng và quyền riêng tư của quan hệ theo dõi (`USER_FOLLOW`)

- **Mã quy tắc:** BR-75
- **Nội dung:**
  - `USER_FOLLOW` biểu diễn quan hệ có hướng từ `follower_user_id` đến `followed_user_id`; A → B không suy ra B → A.
  - Chỉ Member (`CUSTOMER` hoặc `EXPERT`) đã xác thực và có trạng thái tài khoản hoạt động được tạo/xóa quan hệ theo dõi của chính mình.
  - Cấm tự theo dõi: `follower_user_id <> followed_user_id`.
  - Mỗi cặp có hướng chỉ tồn tại tối đa một lần, được bảo vệ bằng khóa duy nhất hoặc khóa chính ghép trên `(follower_user_id, followed_user_id)`.
  - Follow và unfollow phải idempotent; request lặp không tạo bản ghi trùng hoặc làm sai bộ đếm.
  - Backend xác định `follower_user_id` từ phiên đăng nhập và kiểm tra tài khoản đích; client không được chỉ định người theo dõi thay cho tài khoản hiện tại.
  - Quan hệ theo dõi không cấp quyền xem dữ liệu riêng tư. Danh sách chỉ trả các trường hồ sơ công khai theo BR-18.
  - Tài khoản không hoạt động không nhận quan hệ mới và không xuất hiện trong danh sách công khai; chính sách xóa vật lý quan hệ khi xóa tài khoản được quyết định ở Physical ERD/migration theo chính sách vòng đời tài khoản.
  - `USER_FOLLOW` phải tham chiếu `USER` bằng hai vai trò riêng: người theo dõi (`follower`) và người được theo dõi (`followed`).

---

<a id="br-76"></a>
### BR-76 — Quy tắc so sánh hai công thức công khai theo một khẩu phần

- **Mã quy tắc:** BR-76
- **Nội dung:**
  - Mỗi phiên so sánh phải chứa đúng hai Recipe Post khác nhau và cả hai phải đang ở trạng thái `PUBLISHED`; bài bị ẩn, xóa hoặc không còn công khai không được đưa vào kết quả.
  - So sánh dinh dưỡng mặc định theo **một khẩu phần** của từng công thức. Giao diện phải hiển thị rõ số khẩu phần gốc của mỗi công thức để tránh hiểu sai rằng hai khẩu phần có cùng khối lượng thành phẩm.
  - Hệ thống hiển thị đúng chín chỉ tiêu MVP: Năng lượng, Chất đạm, Carbohydrate, Chất béo, Chất xơ, Canxi, Sắt, Vitamin B12 và Kẽm. Natri/muối không thuộc kết quả so sánh.
  - Chênh lệch dinh dưỡng được hiển thị bằng giá trị định lượng tuyệt đối theo cùng đơn vị; MVP không tính chênh lệch phần trăm, không tạo Health Score và không kết luận công thức nào tốt hơn hoặc lành mạnh hơn.
  - Danh sách nguyên liệu giữ số lượng và đơn vị nguyên bản của từng công thức. Hệ thống có thể nhóm tên nguyên liệu chuẩn giống nhau để đối chiếu nhưng không tự coi tên gần giống là cùng nguyên liệu và không tự quy đổi đơn vị khi chưa có quy tắc `INGREDIENT_UNIT_CONVERSION` hợp lệ.
  - Nếu một công thức hoặc chỉ tiêu thiếu dữ liệu, hệ thống hiển thị `Chưa đủ dữ liệu` đúng bên bị ảnh hưởng; giá trị chưa biết không được thay bằng `0` hoặc do AI suy đoán.
  - Guest và Member đều được sử dụng chức năng với dữ liệu công khai. MVP không lưu lịch sử so sánh và không xuất kết quả so sánh ra PDF.
