> **Document:** Use Case Specifications — M04
> **File:** `docs/requirements/use-cases/recipe-discovery-and-classification.md`
> **Version:** v2.0.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-26
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M04

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns the required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-08"></a>
## FR-08 — Tìm kiếm và lọc bài công thức đa tiêu chí

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-08).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Guest` / `Member` / `Administrator`: Mọi người dùng có nhu cầu tìm kiếm và chọn lọc công thức món ăn.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Các bài công thức đang ở trạng thái công khai (`PUBLISHED`) và không bị ẩn do vi phạm.
- **Trigger:** Người dùng nhập từ khóa vào thanh tìm kiếm, chọn các tiêu chí lọc trong bảng bộ lọc, hoặc thay đổi tùy chọn sắp xếp.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Tìm kiếm và Lọc đa tiêu chí (UC-08.1, UC-08.2, UC-08.3)
1. **Main Flow:**
   - Bước 1: Người dùng truy cập trang Tìm kiếm / Khám phá công thức.
   - Bước 2: Người dùng có thể kết hợp các thao tác:
     - Nhập từ khóa tìm kiếm (tìm theo tên món, mô tả hoặc nguyên liệu).
     - Chọn 1 trong 4 loại ăn chay chuẩn: `Vegan`, `Lacto Vegetarian`, `Ovo Vegetarian`, `Lacto-Ovo Vegetarian`.
     - Chọn thể loại món ăn chuẩn hóa (`dish_category`: món nước, món xào, món lẩu, món kho, món canh, món chiên, món hấp, món gỏi / salad, món cuốn, món nướng, món tráng miệng / chè).
     - Chọn giới hạn thời gian nấu tối đa ($\le 15$ phút, $\le 30$ phút, $\le 60$ phút, hoặc trên 60 phút).
     - Chọn 1 trong 6 chế độ sắp xếp độc lập:
       1. **Mới nhất (Newest):** Sắp xếp theo ngày giờ công khai giảm dần (mặc định).
       2. **Được yêu thích nhất (Most Liked / Highest Rated):** Sắp xếp theo tỷ lệ % Like giảm dần, kèm điều kiện phụ theo tổng lượt Like (`likes_count` giảm dần); hiển thị huy hiệu tỷ lệ % Like Samsung Food (`👍 {like_percentage}%`) hoặc nhãn "Mới" nếu chưa có lượt bình chọn (BR-69).
       3. **Xem nhiều nhất (Most Viewed):** Sắp xếp theo số lượt xem hợp lệ từ `RECIPE_VIEW` (hỗ trợ chọn khung 24h, 7 ngày, 30 ngày, all-time) (BR-70).
       4. **Nhiều bình luận nhất (Most Commented):** Sắp xếp theo tổng số bình luận hợp lệ giảm dần.
       5. **Hoạt động sôi nổi nhất (Most Active):** Sắp xếp theo tổng điểm tương tác 7 ngày qua ($\text{views} + 5 \times \text{comments} + 10 \times (\text{likes} + \text{dislikes})$) không phân biệt bài cũ hay mới (BR-71).
       6. **Thịnh hành (Trending):** Sắp xếp theo thuật toán suy giảm thời gian kết hợp tương tác 3 ngày và độ tuổi bài viết (BR-72).
   - Bước 3: Người dùng nhấn "Tìm kiếm" hoặc áp dụng bộ lọc.
   - Bước 4: Hệ thống thực thi truy vấn kết hợp các điều kiện lọc theo phép giao (AND logic giữa các nhóm tiêu chí khác nhau), chỉ lấy các bài công thức đang ở trạng thái `PUBLISHED`.
   - Bước 5: Hệ thống phản hồi danh sách kết quả kèm thông tin phân trang với thời gian phản hồi $\le 1.5$ giây (P95 theo NFR-02).
2. **Alternative Flow (Không tìm thấy kết quả phù hợp):**
   - Nếu không có bài viết nào thỏa mãn đồng thời tất cả các tiêu chí đã chọn, hệ thống hiển thị thông báo "Không tìm thấy công thức nào phù hợp" kèm nút bấm "Đặt lại bộ lọc" để người dùng dễ dàng thử lại.

#### 6. Hậu điều kiện (Postconditions)
- Danh sách các bài công thức phù hợp được hiển thị trực quan cho người dùng.
- Không hiển thị bất kỳ bài viết nào đang ở trạng thái bị ẩn hoặc đã xóa.

---

<a id="fr-17"></a>
## FR-17 — Trình bày bài công thức dạng thẻ món trong Khám phá và liên kết lịch ăn

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-17).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng truy cập trang Khám phá, Trang chủ hoặc thực hiện tìm kiếm/lọc công thức.
- **Kích hoạt (Trigger):**
  - Hệ thống tải và kết xuất danh sách bài viết dưới dạng lưới các thẻ món.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Người dùng mở giao diện Khám phá.
  - Bước 2: Hệ thống truy vấn cơ sở dữ liệu lấy danh sách các Recipe Post có trạng thái công khai (`PUBLISHED`), loại trừ các bài đang bị ẩn hoặc xóa (FR-20).
  - Bước 3: Với mỗi bài viết, hệ thống kết xuất một Thẻ món ăn (Recipe Card) chuẩn gồm:
    - Ảnh bìa từ `RECIPE_MEDIA` (ảnh có `is_cover = true` theo FR-14); nếu bài không có ảnh, hiển thị ảnh mặc định chuyên dụng theo loại ăn chay (BR-20).
    - Tên món ăn (tiêu đề).
    - Tên hiển thị của tác giả (liên kết tới Author Card theo FR-23, bảo vệ quyền riêng tư theo BR-18).
    - Nhãn phân loại ăn chay (Vegan, Lacto, Ovo, Lacto-Ovo).
    - Tổng thời gian thực hiện (phút).
    - Huy hiệu tỷ lệ phần trăm Like (`👍 {like_percentage}%`, ví dụ `👍 98%`, `👍 96%`) đặt nổi bật ở góc trên bên trái ảnh bìa theo phong cách Samsung Food; nếu bài chưa có lượt bình chọn nào thì hiển thị nhãn `Mới` (theo BR-69).
    - Tổng số lượt xem hợp lệ (`view_count`) theo BR-70.
    - Nút icon "Lưu công thức" (Bookmark).
    - Nút icon "Thêm vào lịch ăn" (Cuốn lịch).
  - Bước 4: Danh sách thẻ món hiển thị hoàn tất trong thời gian tải trang $\le 2$ giây (NFR-02).
  - Bước 5: Người dùng có thể nhấp vào thân thẻ để mở trang xem chi tiết công thức (FR-20).
- **Luồng thay thế (Alternative Flows):**
  - *AF-17.1 (Lưu công thức từ thẻ món):* Member nhấn icon Bookmark trên thẻ món. Hệ thống lưu công thức vào danh sách Đã lưu của Member (FR-32) và chuyển đổi icon sang trạng thái đã lưu ngay tức thì mà không chuyển trang.
  - *AF-17.2 (Thêm món vào lịch ăn từ thẻ món):* Member nhấn icon Cuốn lịch trên thẻ món. Hệ thống hiển thị modal nhanh cho phép chọn Ngày trong tuần (Thứ Hai đến Chủ Nhật) và Bữa ăn (Sáng, Trưa, Tối). Member xác nhận, hệ thống thêm món vào Lịch ăn tuần (FR-09, FR-33) và thông báo thành công.
  - *AF-17.3 (Guest nhấn nút lưu hoặc thêm lịch ăn):* Nếu người dùng là Guest nhấn icon Lưu hoặc Thêm lịch ăn, hệ thống hiển thị thông báo yêu cầu đăng nhập (BR-05, BR-32).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-17.1 (Bài viết vừa bị ẩn hoặc xóa):* Nếu trong lúc người dùng duyệt thẻ món mà bài viết gốc vừa bị Admin ẩn hoặc tác giả xóa, khi người dùng nhấp vào thẻ, hệ thống hiển thị thông báo: *"Công thức này không còn khả dụng"* thay vì báo lỗi 500 (BR-35).
  - *SF-17.1 (Bảo mật quyền riêng tư tác giả trên thẻ):* Thông tin tác giả hiển thị trên thẻ món chỉ bao gồm tên hiển thị (display name) công khai, tuyệt đối không lộ địa chỉ email hay ID nội bộ của tác giả (BR-18, NFR-08).

#### 5. Hậu điều kiện (Postconditions)
- Các thẻ món hiển thị chuẩn xác và hấp dẫn trên giao diện với đầy đủ chỉ số huy hiệu % Like và lượt xem.
- Các hành động lưu hoặc thêm vào lịch ăn được cập nhật đồng bộ vào hồ sơ Member.

---

<a id="fr-18"></a>
## FR-18 — Admin quản lý danh mục nguyên liệu chuẩn, đơn vị đo lường và bảng quy đổi

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-18).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập với tài khoản có vai trò Administrator (FR-03, NFR-09).
- **Kích hoạt (Trigger):**
  - Administrator truy cập phân hệ "Quản lý danh mục nguyên liệu & quy đổi" trên trang quản trị hệ thống.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Administrator mở trang Quản lý nguyên liệu chuẩn hoặc Quản lý đơn vị & quy đổi.
  - Bước 2: Hệ thống hiển thị bảng danh sách các mục quản lý.
  - Bước 3: Administrator thêm/sửa thông tin đơn vị đo lường `UNIT` (tên, thứ nguyên `MASS`/`VOLUME`/`COUNT`) hoặc cấu hình tỷ lệ quy đổi trong `INGREDIENT_UNIT_CONVERSION` (chọn nguyên liệu, chọn đơn vị, nhập số gam tương đương).
  - Bước 4: Hệ thống kiểm tra dữ liệu hợp lệ: tỷ lệ quy đổi $> 0$, không trùng lặp cặp `(ingredient_id, unit_id)`.
  - Bước 5: Administrator nhấn "Lưu". Hệ thống ghi nhận vào cơ sở dữ liệu và kích hoạt trạng thái sử dụng ngay lập tức cho các bài viết.
- **Luồng thay thế (Alternative Flows):**
  - *AF-18.1 (Chỉnh sửa nguyên liệu/đơn vị hiện có):* Administrator sửa tên hoặc mô tả của nguyên liệu/đơn vị. Hệ thống cập nhật và tự động phản ánh tên mới trên các giao diện liên quan.
  - *AF-18.2 (Ngừng sử dụng nguyên liệu hoặc đơn vị):* Administrator chọn chuyển trạng thái sang `Ngừng dùng` (Soft-disable). Mục này không còn xuất hiện trong danh sách lựa chọn khi tạo bài viết mới, nhưng các bài công thức cũ đã gắn vẫn giữ nguyên liên kết lịch sử.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-18.1 (Cố gắng xóa cứng nguyên liệu hoặc đơn vị đang được sử dụng):* Nếu Administrator cố gắng xóa vĩnh viễn (hard delete) một nguyên liệu, đơn vị hoặc tỷ lệ quy đổi đang có bài công thức tham chiếu, hệ thống từ chối xóa và hiển thị thông báo: *"Không thể xóa dữ liệu đang có bài công thức tham chiếu. Vui lòng chọn Ngừng sử dụng."* (BR-53, BR-64).
  - *SF-18.1 (Chặn truy cập trái phép phân hệ danh mục):* Người dùng không có vai trò Administrator (Member hoặc Guest) khi truy cập các API quản lý danh mục sẽ bị từ chối ngay với mã lỗi 403 Forbidden (NFR-09).

#### 5. Hậu điều kiện (Postconditions)
- Bản ghi danh mục nguyên liệu, đơn vị `UNIT` và bảng quy đổi `INGREDIENT_UNIT_CONVERSION` được lưu trữ an toàn.
- Hệ thống danh mục phân loại luôn giữ được tính toàn vẹn dữ liệu.

---

<a id="fr-34"></a>
## FR-34 — AI gợi ý món và lập menu từ công thức công khai có sẵn

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-34).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member`: Người dùng đã đăng nhập yêu cầu AI gợi ý món hoặc tạo thực đơn tuần.
- **Secondary Actor / External System:**
  - `Google Gemini AI`: Tiếp nhận danh sách bài công thức ứng viên và thực hiện lựa chọn, xếp hạng thực đơn.

#### 5. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Member đã đăng nhập và **bắt buộc đã hoàn tất 3 nhóm thông tin tối thiểu** trong hồ sơ sở thích ăn uống theo FR-31 (loại ăn chay, xác nhận dị ứng, xác nhận món không thích); Backend xác thực quyền tính năng theo gói tại FR-10. Tạo thực đơn tuần bằng AI yêu cầu gói Pro.
- **Trigger:** Member nhấn nút "AI gợi ý món" hoặc "AI tạo thực đơn tuần".

#### 6. Luồng sự kiện (Flow of Events)

##### A. Luồng AI gợi ý món ăn theo nguyên liệu sẵn có (UC-34.1)
1. **Main Flow:**
   - Bước 1: Member mở tính năng "AI gợi ý món ăn".
   - Bước 2: Member nhập danh sách nguyên liệu đang có (ví dụ: "đậu phụ, cà chua, nấm đùi gà").
   - Bước 3: Hệ thống kiểm tra điều kiện hồ sơ sở thích tối thiểu (FR-31) và quyền sử dụng tính năng (FR-10). Đủ điều kiện.
   - Bước 4: Hệ thống truy vấn cơ sở dữ liệu nội bộ để lấy danh sách các Recipe Post đang công khai (`PUBLISHED`) có chứa ít nhất một trong các nguyên liệu trên và phù hợp với loại ăn chay của Member, loại trừ các món chứa chất dị ứng của Member.
   - Bước 5: Hệ thống gửi danh sách ứng viên bài viết này cùng prompt tới Google Gemini để chọn lọc 3–5 món ăn kết hợp nguyên liệu hài hòa nhất.
   - Bước 6: Google Gemini trả về danh sách các bài viết được chọn.
   - Bước 7: Giao diện hiển thị danh sách các món ăn kèm ảnh, tên món, lý do gợi ý và liên kết dẫn thẳng tới bài công thức nguồn; không trừ quota theo lượt/ngày.

##### B. Luồng AI lập thực đơn tuần và Xác nhận lưu (UC-34.2, UC-34.3)
1. **Main Flow:**
   - Bước 1: Member nhấn nút "AI lập thực đơn tuần mới".
   - Bước 2: Hệ thống kiểm tra điều kiện hồ sơ tối thiểu (FR-31) và quyền gói Pro đối với AI lập thực đơn tuần (FR-10). Đủ điều kiện.
   - Bước 3: Hệ thống truy vấn kho bài công thức công khai, lọc theo loại ăn chay chuẩn của Member và loại trừ các dị ứng đã khai báo.
   - Bước 4: Hệ thống đưa danh sách bài viết ứng viên vào ngữ cảnh và yêu cầu Gemini phân bổ vào 7 ngày $\times$ 3 bữa sao cho thực đơn phong phú, không lặp món liên tục.
   - Bước 5: Gemini trả về cấu trúc thực đơn tuần đề xuất gồm các ID bài viết công khai.
   - Bước 6: Giao diện hiển thị **Bản xem trước thực đơn tuần do AI đề xuất** (Preview Mode); không trừ quota theo lượt/ngày.
   - Bước 7: Member xem xét thực đơn. Nếu ưng ý, Member nhấn nút "Xác nhận áp dụng vào Lịch ăn" (UC-34.3).
   - Bước 8: Hệ thống lưu các món ăn này vào Lịch ăn tuần được chọn của Member (FR-09, FR-33) và thông báo áp dụng thành công.
2. **Alternative Flow (Không áp dụng thực đơn):**
   - Nếu Member không hài lòng với thực đơn đề xuất, Member có thể bấm "Hủy bỏ" hoặc yêu cầu AI tạo lại phương án khác trong phạm vi quyền gói hiện tại. Lịch ăn hiện tại của Member giữ nguyên vẹn không bị thay đổi.
3. **Exception Flow (Chặn an toàn khi thiếu hồ sơ tối thiểu):**
   - Nếu Member chưa hoàn thành 3 thông tin tối thiểu trong hồ sơ sở thích -> Hệ thống lập tức chặn yêu cầu, tuyệt đối không gọi Gemini, và hiển thị thông báo hướng dẫn bổ sung hồ sơ theo quy định tại FR-31 (BR-31).

#### 7. Hậu điều kiện (Postconditions)
- Các món ăn gợi ý luôn có bài công thức nguồn thực tế tương ứng.
- Thực đơn tuần chỉ được ghi vào cơ sở dữ liệu Lịch ăn sau khi có hành động xác nhận chủ động từ Member (BR-34).

---

<a id="fr-47"></a>
## FR-47 — Gợi ý bài công thức liên quan thông thường và tùy chọn Gemini

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-47).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Với gợi ý thông thường: Người dùng đang xem chi tiết một Recipe Post công khai hợp lệ (FR-18).
  - Với tùy chọn AI: Người dùng đã đăng nhập tài khoản Member và đang sử dụng gói Plus hoặc Pro còn hiệu lực (FR-10, BR-02, BR-03).
- **Kích hoạt (Trigger):**
  - Gợi ý thông thường: Tự động kích hoạt khi tải trang chi tiết bài viết.
  - Tùy chọn AI: Member chủ động bấm nút "Nhờ AI gợi ý món ăn kèm / biến tấu".

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow - Gợi ý thông thường không AI):**
  - Bước 1: Người dùng (Guest hoặc Member) truy cập trang chi tiết một bài công thức.
  - Bước 2: Hệ thống truy vấn cơ sở dữ liệu nội bộ tìm các bài công thức công khai khác có cùng danh mục hoặc có ít nhất một nguyên liệu chính trùng khớp, ưu tiên các bài có cùng trường phái ăn chay.
  - Bước 3: Hệ thống trả về danh sách từ 4 đến 6 bài công thức liên quan thông thường (thời gian tải trang $\le 2$ giây theo NFR-02).
  - Bước 4: Khối "Công thức liên quan" được hiển thị ở cuối bài viết mà hoàn toàn không gọi API bên ngoài và không yêu cầu gói dịch vụ nâng cao (BR-03).
- **Luồng thay thế (Alternative Flows - Member chủ động gọi AI):**
  - *AF-47.1 (Yêu cầu AI gợi ý kết hợp món):*
    - Bước 1: Member bấm nút "Nhờ AI gợi ý món ăn kèm / biến tấu".
    - Bước 2: Hệ thống kiểm tra quyền tính năng (Feature Entitlement) của tài khoản Member (FR-10, BR-02, BR-03).
    - Bước 3: Hệ thống trích xuất danh sách các Recipe Post công khai trong hệ thống có thuộc tính phù hợp làm món ăn kèm hoặc biến tấu.
    - Bước 4: Hệ thống gửi prompt kèm metadata của bài hiện tại và danh sách ứng viên tới Google Gemini AI (đáp ứng NFR-03).
    - Bước 5: Gemini phản hồi danh sách gợi ý kèm lời giải thích ngắn gọn lý do kết hợp hương vị.
    - Bước 6: Hệ thống ghi nhận dữ liệu đo lường kỹ thuật (telemetry nếu có theo BR-04) và hiển thị kết quả gợi ý AI trong một khung chuyên biệt nổi bật (BR-03).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-47.1 (Guest bấm nút gọi AI):* Hệ thống hiển thị hộp thoại thông báo yêu cầu đăng nhập để sử dụng tính năng Trợ lý AI (BR-05).
  - *EF-47.2 (Tài khoản thuộc gói Free):* Hệ thống hiển thị thông báo tính năng AI gợi ý món ăn kèm/biến tấu yêu cầu gói Plus hoặc Pro kèm nút bấm điều hướng đến trang Đăng ký gói dịch vụ (FR-10, FR-13, BR-02).
  - *EF-47.3 (Lỗi kết nối Gemini hoặc timeout):* Nếu gọi AI gặp sự cố kỹ thuật hoặc quá thời gian chờ, hệ thống hiển thị thông báo lỗi thân thiện và ghi error log kỹ thuật (BR-04).

#### 5. Hậu điều kiện (Postconditions)
- Khối gợi ý thông thường hiển thị đầy đủ và nhanh chóng.
- Khi gọi AI thành công, kết quả đề xuất xuất hiện trong khung gợi ý chuyên biệt.

---
