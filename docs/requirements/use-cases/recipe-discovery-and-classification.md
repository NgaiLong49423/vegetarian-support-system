> **Document:** Use Case Specifications — M04
> **File:** `docs/requirements/use-cases/recipe-discovery-and-classification.md`
> **Version:** v2.2.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-27
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M04

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-08"></a>
## FR-08 — Tìm kiếm và lọc bài công thức đa tiêu chí

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08).

#### Shared Alternative Flow — Không tìm thấy kết quả
Áp dụng cho cả UC-08.1, UC-08.2 và UC-08.3: khi điều kiện hiện tại không trả về bài phù hợp, hệ thống thông báo không tìm thấy công thức và cung cấp tùy chọn đặt lại bộ lọc.

<a id="uc-08-1"></a>
### UC-08.1 — Tìm kiếm bài công thức theo từ khóa

#### Goal
Giúp người dùng tìm Recipe Post công khai bằng nội dung từ khóa.

#### Primary Actor
`Guest`, `Member` hoặc `Administrator`.

#### Trigger
Người dùng nhập từ khóa và gửi yêu cầu tìm kiếm.

#### Preconditions
Người dùng truy cập được trang Tìm kiếm/Khám phá; hệ thống chỉ trả Recipe Post đang công khai.

#### Main Flow
1. Người dùng nhập từ khóa về tên món, mô tả hoặc nguyên liệu.
2. Hệ thống tìm các Recipe Post công khai có nội dung phù hợp, phân trang kết quả và hiển thị cho người dùng.

#### Postconditions
Danh sách kết quả tìm kiếm được hiển thị; bài ẩn hoặc đã xóa không xuất hiện.

#### Traceability
- **Parent FR:** [FR-08](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08).
- **Relevant NFR:** [NFR-02](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02) (thời gian phản hồi tìm kiếm); [NFR-10](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10) (truy vấn an toàn).

#### Acceptance Coverage
- [AC-08.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08) — Tìm kiếm theo từ khóa.
- [AC-08.9](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08) — Xử lý trạng thái không có kết quả.

---

<a id="uc-08-2"></a>
### UC-08.2 — Lọc bài công thức theo nhiều tiêu chí

#### Goal
Giúp người dùng thu hẹp danh sách công thức theo nhu cầu ăn uống và thời gian chuẩn bị.

#### Primary Actor
`Guest`, `Member` hoặc `Administrator`.

#### Trigger
Người dùng chọn một hoặc nhiều tiêu chí trong bộ lọc.

#### Preconditions
Người dùng đang ở trang Tìm kiếm/Khám phá; các tùy chọn lọc chuẩn đã được cung cấp.

#### Main Flow
1. Người dùng chọn một hoặc nhiều tiêu chí:
   - Trường phái ăn chay: `Vegan`, `Lacto Vegetarian`, `Ovo Vegetarian`, hoặc `Lacto-Ovo Vegetarian`.
   - Thể loại món `dish_category`: món nước, món xào, món lẩu, món kho, món canh, món chiên, món hấp, món gỏi/salad, món cuốn, món nướng, hoặc món tráng miệng/chè.
   - Nguyên liệu và giới hạn thời gian nấu tối đa (≤ 15, ≤ 30, ≤ 60 phút hoặc trên 60 phút).
2. Hệ thống áp dụng điều kiện giao (AND) giữa các nhóm tiêu chí và chỉ truy vấn Recipe Post công khai.
3. Hệ thống hiển thị danh sách kết quả có phân trang.

#### Postconditions
Danh sách chỉ còn các bài công thức công khai thỏa mãn các tiêu chí đã chọn.

#### Traceability
- **Parent FR:** [FR-08](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08).
- **Relevant NFR:** [NFR-02](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02) (thời gian phản hồi); [NFR-10](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10) (truy vấn an toàn); [NFR-13](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13) (giao diện responsive).

#### Acceptance Coverage
- [AC-08.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08) — Lọc theo loại ăn chay.
- [AC-08.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08) — Lọc theo thể loại món.
- [AC-08.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08) — Lọc theo thời gian nấu.
- [AC-08.9](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08) — Trạng thái không có kết quả sau lọc.

---

<a id="uc-08-3"></a>
### UC-08.3 — Sắp xếp kết quả theo sáu chế độ

#### Goal
Cho phép người dùng sắp xếp kết quả tìm kiếm/khám phá bằng các chỉ số hoạt động hiện hành.

#### Primary Actor
`Guest`, `Member` hoặc `Administrator`.

#### Trigger
Người dùng chọn một chế độ sắp xếp trên danh sách công thức.

#### Preconditions
Danh sách Recipe Post công khai đang được hiển thị; người dùng có thể áp dụng tiêu chí lọc cùng lúc.

#### Main Flow
1. Người dùng chọn một trong sáu chế độ: Mới nhất; Được yêu thích nhất; Xem nhiều nhất; Nhiều bình luận nhất; Hoạt động sôi nổi nhất; hoặc Thịnh hành.
2. Hệ thống sắp xếp theo đúng tiêu chí tương ứng: thời điểm công khai giảm dần; `like_percentage` giảm dần rồi `like_count`; `view_count` theo khung 24 giờ/7 ngày/30 ngày/all-time; tổng bình luận giảm dần; điểm Most Active 7 ngày `views + 5 × comments + 10 × (likes + dislikes)` không giảm điểm vì tuổi bài; hoặc Trending theo tương tác 3 ngày kết hợp suy giảm theo thời gian/độ mới.
3. Hệ thống giữ các điều kiện lọc hiện có, trả kết quả phân trang và hiển thị nhãn tỷ lệ Like (`like_percentage`) hoặc “Mới” khi chưa có bình chọn nếu chế độ Most Liked được chọn.

#### Postconditions
Danh sách hiển thị theo chế độ được chọn bằng dữ liệu Like/Dislike và lượt xem hiện hành.

#### Traceability
- **Parent FR:** [FR-08](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08).
- **Relevant BR:** [BR-69](../srs/BUSINESS-RULES.md#br-69) (Like/Dislike và tỷ lệ % Like); [BR-70](../srs/BUSINESS-RULES.md#br-70) (lượt xem); [BR-71](../srs/BUSINESS-RULES.md#br-71) (Most Active); [BR-72](../srs/BUSINESS-RULES.md#br-72) (Trending).
- **Relevant NFR:** [NFR-02](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02) (thời gian phản hồi sắp xếp).

#### Acceptance Coverage
- [AC-08.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08) — Sắp xếp bài mới nhất.
- [AC-08.6](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08) — Sắp xếp theo Most Liked.
- [AC-08.7](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08) — Sắp xếp theo Most Viewed.
- [AC-08.8](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08) — Phân biệt Most Active và Trending.

---

<a id="fr-17"></a>
## FR-17 — Trình bày bài công thức dạng thẻ món trong Khám phá và liên kết lịch ăn

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-17).

<a id="uc-17-1"></a>
### UC-17.1 — Duyệt danh sách thẻ món công khai

#### Goal
Giúp người dùng khám phá Recipe Post công khai qua các thẻ món có thông tin nhất quán.

#### Primary Actor
`Guest` hoặc `Member`.

#### Trigger
Người dùng mở Trang chủ/Khám phá hoặc danh sách kết quả công thức.

#### Preconditions
Có các Recipe Post công khai; bài ẩn hoặc đã xóa không thuộc danh sách hiển thị.

#### Main Flow
1. Hệ thống lấy các Recipe Post đang `PUBLISHED` và kết xuất từng thẻ món.
2. Mỗi thẻ hiển thị ảnh bìa hoặc ảnh mặc định, tên món, tên hiển thị công khai của tác giả (không hiển thị email hoặc ID nội bộ), loại ăn chay, tổng thời gian, `like_percentage` (hoặc nhãn “Mới” khi chưa có bình chọn) và `view_count`.
3. Người dùng có thể duyệt danh sách và chọn thao tác lưu/thêm lịch hoặc mở chi tiết ở các UC tiếp theo.

#### Postconditions
Các thẻ món công khai được hiển thị với chỉ số Like/Dislike và lượt xem hiện hành.

#### Security Flow
- **SF-17.1 — Quyền riêng tư tác giả:** Thẻ chỉ hiển thị tên công khai, không để lộ email hoặc ID nội bộ (BR-18, NFR-08).

#### Traceability
- **Parent FR:** [FR-17](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-17).
- **Relevant BR:** [BR-18](../srs/BUSINESS-RULES.md#br-18) (thông tin tác giả công khai); [BR-20](../srs/BUSINESS-RULES.md#br-20) (ảnh mặc định khi không có ảnh); [BR-69](../srs/BUSINESS-RULES.md#br-69) (tỷ lệ Like); [BR-70](../srs/BUSINESS-RULES.md#br-70) (lượt xem).
- **Relevant NFR:** [NFR-02](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02) (thời gian tải); [NFR-08](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08) (quyền riêng tư dữ liệu cá nhân); [NFR-13](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13) (responsive tiếng Việt).

#### Acceptance Coverage
- [AC-17.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-17) — Hiển thị thông tin trên thẻ món.
- [AC-17.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-17) — Ảnh mặc định khi bài không có ảnh.

---

<a id="uc-17-2"></a>
### UC-17.2 — Lưu công thức hoặc thêm món vào Lịch ăn từ thẻ

#### Goal
Giúp Member thực hiện nhanh thao tác lưu công thức hoặc thêm món vào lịch mà không cần mở trang chi tiết trước.

#### Primary Actor
`Member`.

#### Trigger
Member nhấn icon Lưu hoặc Thêm vào lịch ăn trên thẻ món.

#### Preconditions
Member đã đăng nhập; Recipe Post đang công khai.

#### Main Flow
1. Khi Member chọn Lưu, hệ thống lưu Recipe Post vào danh sách đã lưu và cập nhật icon ngay trên thẻ, không cần chuyển trang.
2. Khi Member chọn Thêm vào lịch, hệ thống cho chọn ngày Thứ Hai–Chủ Nhật và một trong ba bữa Sáng/Trưa/Tối.
3. Member xác nhận; hệ thống thêm món vào ô bữa ăn được chọn và thông báo thành công.

#### Alternative Flows
- **AF-17.1 — Lưu từ thẻ món:** Hệ thống ghi nhận bài đã lưu và đổi trạng thái icon.
- **AF-17.2 — Thêm vào Lịch ăn:** Hệ thống cập nhật ô ngày/bữa Member đã chọn.
- **AF-17.3 — Guest nhấn thao tác thành viên:** Hệ thống yêu cầu đăng nhập và không tạo dữ liệu lưu/lịch.

#### Postconditions
Thao tác lưu hoặc cập nhật Meal Plan được phản ánh trong hồ sơ Member; Recipe Post nguồn không bị thay đổi.

#### Traceability
- **Parent FR:** [FR-17](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-17).
- **Related FR:** [FR-09](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-09) (Meal Plan); [FR-32](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-32) (Saved Recipe); [FR-33](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-33) (thêm món vào lịch).
- **Relevant BR:** [BR-05](../srs/BUSINESS-RULES.md#br-05) (Guest không có quyền thao tác); [BR-32](../srs/BUSINESS-RULES.md#br-32) (yêu cầu đăng nhập); [BR-33](../srs/BUSINESS-RULES.md#br-33) (thao tác lưu không gọi Gemini); [BR-36](../srs/BUSINESS-RULES.md#br-36) (ba bữa cố định).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (phân quyền).

#### Acceptance Coverage
- [AC-17.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-17) — Lưu công thức từ thẻ.
- [AC-17.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-17) — Thêm món vào Lịch ăn từ thẻ.

---

<a id="uc-17-3"></a>
### UC-17.3 — Mở trang chi tiết từ thẻ món

#### Goal
Cho phép người dùng đi từ thẻ món tới trang chi tiết của cùng Recipe Post.

#### Primary Actor
`Guest` hoặc `Member`.

#### Trigger
Người dùng nhấp vào phần thân thẻ món.

#### Preconditions
Thẻ món trỏ tới một Recipe Post công khai.

#### Main Flow
1. Hệ thống điều hướng tới trang chi tiết của Recipe Post đã chọn.
2. Trang chi tiết hiển thị dữ liệu của chính bài viết đó.

#### Exception Flows
- **EF-17.1 — Bài viết vừa bị ẩn hoặc xóa:** Hệ thống thông báo công thức không còn khả dụng thay vì gây lỗi máy chủ.

#### Postconditions
Người dùng xem trang chi tiết đúng bài viết, hoặc nhận thông báo không khả dụng khi trạng thái nguồn đã đổi.

#### Traceability
- **Parent FR:** [FR-17](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-17).
- **Related FR:** [FR-20](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-20) (trang chi tiết dùng cùng nguồn Recipe Post).
- **Relevant BR:** [BR-35](../srs/BUSINESS-RULES.md#br-35) (bài không khả dụng và tham chiếu/tombstone).
- **Relevant NFR:** [NFR-02](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02) (thời gian tải trang).

#### Acceptance Coverage
- [AC-17.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-17) — Điều hướng từ thẻ tới trang chi tiết.

---

<a id="fr-18"></a>
## FR-18 — Admin quản lý danh mục nguyên liệu chuẩn, đơn vị đo lường và bảng quy đổi

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18).

#### Shared Security Flow — SF-18.1 (Chỉ Administrator quản lý danh mục)
Chỉ Administrator được truy cập và thay đổi danh mục nguyên liệu, đơn vị, bảng quy đổi; API từ chối Member/Guest (NFR-09). Áp dụng cho cả ba UC.

#### Shared Alternative Flows
- **AF-18.1 — Chỉnh sửa mục hiện có:** Administrator sửa tên hoặc mô tả nguyên liệu/đơn vị; hệ thống phản ánh tên mới trên giao diện liên quan.
- **AF-18.2 — Ngừng sử dụng nguyên liệu hoặc đơn vị:** Administrator chuyển mục sang trạng thái ngừng dùng; mục không còn được chọn cho bài mới nhưng các Recipe Post cũ giữ liên kết lịch sử.

#### Shared Exception Flow — EF-18.1 (Không xóa cứng dữ liệu đang được tham chiếu)
Khi Admin cố gắng hard-delete nguyên liệu, đơn vị hoặc tỷ lệ quy đổi đang được bài công thức tham chiếu, hệ thống từ chối thao tác; sử dụng ngừng hoạt động theo phạm vi được phép thay cho xóa dữ liệu lịch sử.

<a id="uc-18-1"></a>
### UC-18.1 — Tìm kiếm và quản lý danh mục nguyên liệu chuẩn

#### Goal
Cho Administrator duy trì danh mục nguyên liệu chuẩn được dùng khi tạo Recipe Post.

#### Primary Actor
`Administrator`.

#### Trigger
Administrator mở trang quản lý danh mục nguyên liệu.

#### Preconditions
Administrator đã đăng nhập với quyền quản trị.

#### Main Flow
1. Hệ thống hiển thị danh sách nguyên liệu chuẩn và hỗ trợ tìm kiếm.
2. Administrator tạo mới hoặc chỉnh sửa tên tiếng Việt, tên tiếng Anh tùy chọn và nhóm nguyên liệu.
3. Administrator có thể ngừng sử dụng nguyên liệu; nguyên liệu ngừng sử dụng không được chọn cho liên kết mới nhưng giữ nguyên liên kết lịch sử.

#### Postconditions
Danh mục nguyên liệu được cập nhật để dùng trong các lựa chọn Recipe Post; dữ liệu đang được tham chiếu không bị xóa cứng.

#### Traceability
- **Parent FR:** [FR-18](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18).
- **Relevant BR:** [BR-53](../srs/BUSINESS-RULES.md#br-53) (không xóa nguyên liệu dinh dưỡng đang được tham chiếu).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (quyền quản trị).

#### Acceptance Coverage
- [AC-18.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18) — Chỉ Administrator được quản lý danh mục.
- [AC-18.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18) — Thêm nguyên liệu chuẩn hợp lệ.
- [AC-18.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18) — Chặn xóa vĩnh viễn nguyên liệu đang được bài viết tham chiếu.
- Coverage gap — Chưa có AC kiểm tra trực tiếp tìm kiếm, chỉnh sửa hoặc ngừng sử dụng nguyên liệu; [AC-18.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18) chỉ kiểm tra tạo mới, [AC-18.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18) kiểm tra từ chối xóa cứng.

---

<a id="uc-18-2"></a>
### UC-18.2 — Quản lý danh mục đơn vị đo lường `UNIT`

#### Goal
Cho Administrator quản lý các đơn vị đo lường chuẩn cùng thứ nguyên của chúng.

#### Primary Actor
`Administrator`.

#### Trigger
Administrator mở trang quản lý đơn vị đo lường.

#### Preconditions
Administrator đã đăng nhập với quyền quản trị.

#### Main Flow
1. Hệ thống hiển thị danh mục `UNIT`.
2. Administrator thêm hoặc chỉnh sửa tên, ký hiệu và thứ nguyên `MASS`, `VOLUME` hoặc `COUNT`.
3. Administrator có thể ngừng sử dụng đơn vị; các liên kết lịch sử được bảo toàn.

#### Postconditions
Danh mục đơn vị phản ánh thông tin và trạng thái được Administrator lưu.

#### Traceability
- **Parent FR:** [FR-18](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18).
- **Relevant BR:** [BR-73](../srs/BUSINESS-RULES.md#br-73) (đơn vị chuẩn và thứ nguyên đo lường).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (quyền quản trị).

#### Acceptance Coverage
- [AC-18.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18) — Chỉ Administrator được quản lý danh mục.
- `AC COVERAGE GAP` — Chưa có AC kiểm tra trực tiếp thao tác xem/thêm/chỉnh sửa danh mục `UNIT`; [AC-18.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18) kiểm tra cấu hình quy đổi ở UC-18.3, không thay thế kiểm chứng quản lý danh mục đơn vị.

---

<a id="uc-18-3"></a>
### UC-18.3 — Cấu hình quy đổi đơn vị theo nguyên liệu

#### Goal
Cho Administrator khai báo tỷ lệ quy đổi chính thức từ một đơn vị của một nguyên liệu sang gam.

#### Primary Actor
`Administrator`.

#### Trigger
Administrator mở trang cấu hình `INGREDIENT_UNIT_CONVERSION`.

#### Preconditions
Administrator đã đăng nhập; nguyên liệu và đơn vị cần cấu hình đã có trong danh mục.

#### Main Flow
1. Administrator chọn một nguyên liệu, một đơn vị và nhập số gam tương đương.
2. Hệ thống kiểm tra tỷ lệ phải lớn hơn 0 và không tạo trùng cặp `(ingredient_id, unit_id)`.
3. Khi hợp lệ, hệ thống lưu cấu hình để sử dụng cho tính toán dinh dưỡng và validation công thức theo FR-19/BR-73.

#### Postconditions
Tỷ lệ quy đổi gắn với đúng nguyên liệu/đơn vị được lưu và có thể được các luồng liên quan sử dụng.

#### Traceability
- **Parent FR:** [FR-18](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18).
- **Related FR:** [FR-19](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-19) (định lượng Recipe Post).
- **Relevant BR:** [BR-14](../srs/BUSINESS-RULES.md#br-14) (định lượng nguyên liệu); [BR-73](../srs/BUSINESS-RULES.md#br-73) (quy tắc bảng quy đổi).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (quyền quản trị); [NFR-10](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10) (validation dữ liệu).

#### Acceptance Coverage
- [AC-18.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18) — Chỉ Administrator được quản lý danh mục.
- [AC-18.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18) — Cấu hình tỷ lệ quy đổi nguyên liệu–đơn vị.
- Coverage gap — Chưa có AC cho việc từ chối tỷ lệ quy đổi không hợp lệ hoặc cặp nguyên liệu–đơn vị bị trùng; [AC-18.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18) chỉ kiểm tra lưu thành công.

---

<a id="fr-34"></a>
## FR-34 — AI gợi ý món và lập menu từ công thức công khai có sẵn

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-34).

#### Shared Exception Flow — Hồ sơ sở thích tối thiểu chưa hoàn tất
Trước khi gọi Gemini ở UC-34.1 hoặc UC-34.2, hệ thống kiểm tra đủ ba nhóm thông tin tối thiểu theo FR-31. Nếu thiếu, hệ thống chặn request, không gọi Gemini và hướng dẫn Member bổ sung hồ sơ (BR-31). UC-34.3 chỉ áp dụng preview đã được tạo, không gọi Gemini.

<a id="uc-34-1"></a>
### UC-34.1 — Yêu cầu AI gợi ý món theo nguyên liệu sẵn có

#### Goal
Giúp Member tìm công thức công khai phù hợp với nguyên liệu đang có và sở thích ăn uống.

#### Primary Actor
`Member`.

#### Supporting Actors
Google Gemini AI.

#### Trigger
Member chọn “AI gợi ý món ăn”.

#### Preconditions
Member đã đăng nhập, hoàn tất hồ sơ sở thích tối thiểu và có quyền dùng tính năng theo FR-10.

#### Main Flow
1. Member nhập danh sách nguyên liệu đang có.
2. Hệ thống lấy Recipe Post công khai có ít nhất một nguyên liệu được nhập, phù hợp trường phái ăn chay Member và loại trừ nguyên liệu gây dị ứng đã khai báo.
3. Hệ thống gửi danh sách ứng viên cùng prompt tới Gemini để chọn 3–5 món phù hợp.
4. Giao diện hiển thị ảnh, tên món, lý do gợi ý và liên kết về công thức nguồn; không áp dụng daily quota.

#### Postconditions
Kết quả gợi ý tham chiếu tới Recipe Post công khai thực tế; hệ thống không tạo Recipe Post mới.

#### Traceability
- **Parent FR:** [FR-34](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-34).
- **Related FR:** [FR-10](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-10) (Feature Entitlement); [FR-31](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-31) (hồ sơ tối thiểu).
- **Relevant BR:** [BR-31](../srs/BUSINESS-RULES.md#br-31) (thiếu thông tin thì chặn cá nhân hóa); [BR-38](../srs/BUSINESS-RULES.md#br-38) (AI chỉ dùng công thức công khai có sẵn).
- **Relevant NFR:** [NFR-03](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03) (thời gian phản hồi AI); [NFR-18](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-18) (provider fallback); [NFR-25](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-25) (chất lượng và tuân thủ nội dung AI).

#### Acceptance Coverage
- [AC-34.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-34) — Chỉ gợi ý Recipe Post công khai có link nguồn.
- [AC-34.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-34) — Chặn yêu cầu khi thiếu hồ sơ tối thiểu.
- Coverage gap — Chưa có AC kiểm tra kết quả gợi ý nguyên liệu tuân thủ trường phái ăn chay và dị ứng; [AC-34.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-34) kiểm tra thực đơn tuần, không phải UC-34.1.

---

<a id="uc-34-2"></a>
### UC-34.2 — Yêu cầu AI tạo preview thực đơn tuần

#### Goal
Cho Member Pro yêu cầu Gemini đề xuất thực đơn tuần dựa trên kho công thức công khai và hồ sơ ăn uống.

#### Primary Actor
`Member` thuộc gói Pro.

#### Supporting Actors
Google Gemini AI.

#### Trigger
Member chọn “AI lập thực đơn tuần mới”.

#### Preconditions
Member đã đăng nhập, hoàn tất hồ sơ sở thích tối thiểu và có quyền Pro theo FR-10.

#### Main Flow
1. Hệ thống lấy công thức công khai, lọc theo trường phái ăn chay và loại trừ dị ứng Member đã khai báo.
2. Hệ thống yêu cầu Gemini phân bổ các công thức ứng viên cho 7 ngày × 3 bữa, tránh lặp món liên tục.
3. Giao diện hiển thị cấu trúc thực đơn tuần được đề xuất ở chế độ Preview; chưa lưu Meal Plan và không áp dụng daily quota.
4. Member có thể xem xét preview rồi chuyển sang UC-34.3 để xác nhận áp dụng.

#### Alternative Flows
- Member không chấp nhận preview có thể hủy hoặc yêu cầu Gemini tạo phương án khác trong phạm vi gói hiện tại; Meal Plan hiện có không thay đổi.

#### Postconditions
Preview được hiển thị; chưa có thay đổi nào được ghi vào Meal Plan nếu Member chưa xác nhận.

#### Traceability
- **Parent FR:** [FR-34](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-34).
- **Related FR:** [FR-10](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-10) (Pro entitlement); [FR-31](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-31) (hồ sơ tối thiểu); [FR-09](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-09) (đích áp dụng sau xác nhận).
- **Relevant BR:** [BR-31](../srs/BUSINESS-RULES.md#br-31) (chặn khi thiếu hồ sơ); [BR-38](../srs/BUSINESS-RULES.md#br-38) (chỉ dùng công thức công khai).
- **Relevant NFR:** [NFR-03](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03); [NFR-18](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-18); [NFR-25](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-25).

#### Acceptance Coverage
- [AC-34.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-34) — Tuân thủ loại ăn chay và danh sách dị ứng.
- [AC-34.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-34) — Preview chưa làm thay đổi Lịch ăn khi chưa xác nhận.
- [AC-34.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-34) — Chặn yêu cầu khi thiếu hồ sơ tối thiểu.
- Coverage gap — Chưa có AC kiểm tra trực tiếp entitlement Pro cho UC-34.2; [AC-10.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-10) hiện nêu FR-36, còn [AC-10.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-10) chỉ kiểm tra quyền cho Member Pro nói chung.

---

<a id="uc-34-3"></a>
### UC-34.3 — Xác nhận áp dụng thực đơn AI vào Lịch ăn

#### Goal
Cho Member chủ động quyết định lưu preview AI vào một Lịch ăn tuần đã chọn.

#### Primary Actor
`Member`.

#### Trigger
Member nhấn “Xác nhận áp dụng vào Lịch ăn” trên preview đã xem.

#### Preconditions
Có preview thực đơn AI hợp lệ được tạo cho Member; Member chọn tuần đích.

#### Main Flow
1. Member xác nhận áp dụng preview vào tuần đã chọn.
2. Hệ thống ghi các món đề xuất vào Meal Plan của Member và thông báo áp dụng thành công.

#### Postconditions
Meal Plan được cập nhật chỉ sau hành động xác nhận chủ động của Member; Recipe Post nguồn không bị sửa.

#### Traceability
- **Parent FR:** [FR-34](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-34).
- **Related FR:** [FR-09](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-09) (lịch tuần); [FR-33](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-33) (thêm công thức vào bữa).
- **Relevant BR:** [BR-37](../srs/BUSINESS-RULES.md#br-37) (không tạo món trùng trong cùng bữa/ngày).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (quyền sở hữu Meal Plan).

#### Acceptance Coverage
- [AC-34.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-34) — Lưu thực đơn sau xác nhận chủ động.

---

<a id="fr-47"></a>
## FR-47 — Gợi ý bài công thức liên quan thông thường và tùy chọn Gemini

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-47).

<a id="uc-47-1"></a>
### UC-47.1 — Xem công thức liên quan thông thường

#### Goal
Giúp người dùng khám phá các Recipe Post có quan hệ nguyên liệu hoặc danh mục với công thức đang xem, không phụ thuộc dịch vụ AI ngoài.

#### Primary Actor
`Guest` hoặc `Member`.

#### Trigger
Người dùng mở trang chi tiết một Recipe Post công khai.

#### Preconditions
Recipe Post đang xem công khai; hệ thống có thể truy vấn kho công thức nội bộ.

#### Main Flow
1. Hệ thống tìm các Recipe Post công khai khác cùng danh mục hoặc có ít nhất một nguyên liệu chính trùng, ưu tiên cùng trường phái ăn chay.
2. Hệ thống hiển thị 4–6 công thức liên quan ở cuối trang chi tiết.
3. Luồng này không gọi API bên ngoài và không yêu cầu gói nâng cao.

#### Postconditions
Khối công thức liên quan thông thường được hiển thị cho người dùng.

#### Traceability
- **Parent FR:** [FR-47](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-47).
- **Relevant NFR:** [NFR-02](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02) (thời gian tải nội dung).

#### Acceptance Coverage
- [AC-47.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-47) — Hiển thị gợi ý thông thường từ dữ liệu nội bộ.

---

<a id="uc-47-2"></a>
### UC-47.2 — Yêu cầu Gemini gợi ý món ăn kèm hoặc biến tấu

#### Goal
Cho Member có entitlement Plus/Pro nhận gợi ý món ăn kèm hoặc biến tấu từ kho Recipe Post công khai.

#### Primary Actor
`Member` thuộc gói Plus hoặc Pro.

#### Supporting Actors
Google Gemini AI.

#### Trigger
Member nhấn “Nhờ AI gợi ý món ăn kèm / biến tấu”.

#### Preconditions
Member đang xem Recipe Post công khai và có quyền AI tương ứng theo FR-10.

#### Main Flow — AF-47.1 (Member yêu cầu AI gợi ý kết hợp món)
1. Hệ thống kiểm tra Feature Entitlement trước khi gọi Gemini.
2. Hệ thống lấy các Recipe Post công khai có thuộc tính phù hợp làm món ăn kèm/biến tấu.
3. Hệ thống gửi metadata bài hiện tại cùng danh sách ứng viên cho Gemini.
4. Hệ thống hiển thị kết quả gợi ý kèm giải thích ngắn trong khung chuyên biệt và ghi telemetry nếu có theo FR-11.

#### Exception Flows
- **EF-47.1 — Guest yêu cầu AI:** Hệ thống yêu cầu đăng nhập.
- **EF-47.2 — Member Free yêu cầu AI:** Hệ thống thông báo cần Plus/Pro và điều hướng tới trang gói dịch vụ.
- **EF-47.3 — Gemini lỗi hoặc timeout:** Hệ thống thông báo lỗi thân thiện và ghi error log theo BR-04.

#### Postconditions
Khi yêu cầu hợp lệ và Gemini thành công, kết quả xuất hiện trong khung gợi ý AI; không có Recipe Post mới được tạo bởi chatbot.

#### Traceability
- **Parent FR:** [FR-47](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-47).
- **Related FR:** [FR-10](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-10) (entitlement); [FR-11](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-11) (telemetry); [FR-13](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13) (thông tin gói).
- **Relevant BR:** [BR-02](../srs/BUSINESS-RULES.md#br-02) (quyền Plus/Pro); [BR-03](../srs/BUSINESS-RULES.md#br-03) (kiểm tra entitlement trước khi gọi); [BR-04](../srs/BUSINESS-RULES.md#br-04) (provider error/telemetry); [BR-05](../srs/BUSINESS-RULES.md#br-05) (Guest phải đăng nhập).
- **Relevant NFR:** [NFR-03](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03) (thời gian phản hồi AI); [NFR-18](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-18) (provider fallback).

#### Acceptance Coverage
- [AC-47.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-47) — Chỉ gọi AI sau thao tác chủ động và entitlement hợp lệ.
- [AC-47.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-47) — Chỉ đề xuất công thức có sẵn.
- [AC-47.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-47) — Xác thực quyền Plus/Pro trước khi gọi AI.

---
