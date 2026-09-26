> **Document:** Use Case Specifications — M03
> **File:** `docs/requirements/use-cases/recipe-contribution-and-community.md`
> **Version:** v2.0.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-26
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M03

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns the required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-14"></a>
## FR-14 — Lưu trữ thư viện ảnh bài công thức (tối đa 5 ảnh, đúng 1 ảnh bìa) trên Azure Blob Storage

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-14).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản hợp lệ (FR-03).
  - Người dùng đang ở giao diện soạn thảo hoặc chỉnh sửa bài công thức nấu ăn.
- **Kích hoạt (Trigger):**
  - Tác giả chọn 1 hoặc nhiều tệp tin ảnh từ thiết bị để thêm vào bài công thức.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả chọn từ 1 đến 5 tệp ảnh từ máy tính hoặc thiết bị di động.
  - Bước 2: Hệ thống kiểm tra hợp lệ tại tầng ứng dụng: số lượng ảnh $\le 5$, dung lượng từng tệp tin $\le 5$ MB, MIME type thuộc danh sách cho phép (image/jpeg, image/png, image/webp) (NFR-10).
  - Bước 3: Hệ thống sinh tên tệp duy nhất (UUID + timestamp) để tránh xung đột tên tệp.
  - Bước 4: Hệ thống thực hiện truyền tải luồng dữ liệu (streaming upload) từng tệp tin lên vùng chứa (container) chuyên dụng trên Azure Blob Storage.
  - Bước 5: Azure Blob Storage xác nhận tải lên thành công và trả về Blob URL tương ứng.
  - Bước 6: Tác giả chọn 1 ảnh làm ảnh bìa (`is_cover = true`) và sắp xếp thứ tự hiển thị (`display_order` 1..N).
  - Bước 7: Hệ thống lưu các bản ghi tương ứng vào thực thể `RECIPE_MEDIA` liên kết với `recipe_id`.
  - Bước 8: Giao diện hiển thị bản xem trước thư viện ảnh với đánh dấu rõ ràng ảnh bìa.
- **Luồng quản lý vòng đời & Giải phóng tài nguyên (Resource Lifecycle & Cleanup Flow):**
  - *Thu hồi ảnh khi gỡ/thay thế ảnh:* Khi tác giả xóa bớt ảnh hoặc thay thế ảnh trong `RECIPE_MEDIA`, hệ thống cập nhật lại danh sách, kiểm tra bảo toàn đúng 1 ảnh bìa, và kích hoạt công việc xóa blob tương ứng trên Azure Blob Storage.
  - *Giải phóng tài nguyên khi xóa công thức:* Khi bài công thức bị xóa, hệ thống giải phóng toàn bộ các Blob thuộc `RECIPE_MEDIA` của bài viết theo chính sách dọn dẹp.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-14.1 (Vượt quá số lượng hoặc dung lượng ảnh cho phép):* Nếu tác giả chọn $> 5$ ảnh hoặc có ảnh $> 5$ MB, hệ thống từ chối nhận và hiển thị thông báo lỗi tương ứng.
  - *EF-14.2 (Thiếu ảnh bìa khi có ảnh):* Nếu bài viết có từ 1 đến 5 ảnh nhưng chưa chọn ảnh bìa (hoặc có $> 1$ ảnh bìa), hệ thống từ chối lưu và yêu cầu chọn đúng 1 ảnh bìa (BR-19, BR-20).
  - *EF-14.3 (Định dạng tệp không hợp lệ hoặc chứa mã độc):* Nếu tệp tin không đúng MIME type ảnh, hệ thống từ chối và ghi log cảnh báo an ninh (NFR-10).
  - *EF-14.4 (Lỗi kết nối Azure Blob Storage):* Thông báo lỗi tải ảnh thân thiện và cho phép thử lại mà không mất nội dung bài viết đang soạn thảo.
  - *SF-14.1 (Không lưu binary trong CSDL):* Toàn bộ tệp nhị phân lưu trữ độc quyền trên Azure Blob Storage; CSDL chỉ lưu trữ URL và metadata trong `RECIPE_MEDIA` (NFR-02).

#### 5. Hậu điều kiện (Postconditions)
- Tệp ảnh được lưu trữ an toàn trên Azure Blob Storage và bản ghi `RECIPE_MEDIA` được tạo/cập nhật chính xác.
- Đúng 1 ảnh được đánh dấu `is_cover = true` phục vụ hiển thị trên thẻ món ngoài trang Khám phá.

---

<a id="fr-16"></a>
## FR-16 — Cấu trúc dữ liệu bài công thức, hướng dẫn chế biến và validation profile

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-16).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản hợp lệ (FR-03).
- **Kích hoạt (Trigger):**
  - Tác giả nhấn nút "Công khai bài viết" hoặc "Lưu thay đổi" từ giao diện soạn thảo bài viết.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả nhấn "Công khai bài viết".
  - Bước 2: Hệ thống tiếp nhận toàn bộ dữ liệu bài viết và kích hoạt bộ kiểm tra hợp lệ:
    - Tiêu đề: kiểm tra độ dài nằm trong khoảng 3 đến 120 ký tự.
    - Thể loại món: kiểm tra chọn đúng 1 giá trị chuẩn hóa thuộc `dish_category`.
    - Nguyên liệu: kiểm tra số lượng dòng từ 1 đến 50, mỗi dòng có tên, định lượng số học dương và đơn vị chuẩn trong `UNIT` (FR-19).
    - Quy đổi đơn vị: kiểm tra mọi nguyên liệu có đơn vị quy đổi hợp lệ sang gam trong `INGREDIENT_UNIT_CONVERSION` (BR-73).
    - Hướng dẫn chế biến: kiểm tra nội dung hướng dẫn `instructions` từ 10 đến 5.000 ký tự sau khi cắt khoảng trắng đầu cuối (trim); không ép buộc phân rã thành các bước độc lập.
    - Khẩu phần: kiểm tra giá trị số nguyên từ 1 đến 50.
    - Thời gian: kiểm tra prep time $\ge 0$, cook time $\ge 0$, mỗi giá trị $\le 1.440$ phút và tổng thời gian $> 0$.
    - Phân loại ăn chay: kiểm tra thuộc 1 trong 4 loại chuẩn.
    - Mô tả: kiểm tra độ dài không vượt quá 2.000 ký tự (nếu có nhập).
    - Ảnh: kiểm tra số lượng từ 0 đến 5 ảnh (`RECIPE_MEDIA`); nếu có $\ge 1$ ảnh thì bắt buộc đúng 1 ảnh bìa (`is_cover = true`), định dạng JPEG/PNG/WebP, dung lượng $\le 5$ MB/ảnh (FR-14).
    - Video: kiểm tra tối đa 1 link YouTube hợp lệ (FR-15).
  - Bước 3: Toàn bộ tiêu chí validation đều thỏa mãn.
  - Bước 4: Hệ thống cho phép xuất bản trực tiếp bài công thức lên trạng thái công khai (`PUBLISHED`) ngay lập tức mà không cần Admin duyệt trước (BR-07, BR-59).
  - Bước 5: Hệ thống hiển thị thông báo thành công và điều hướng tác giả tới bài viết vừa đăng.
- **Luồng thay thế (Alternative Flows):**
  - *AF-16.1 (Công thức có thời gian nấu bằng 0):* Với các món salad trộn hoặc sinh tố, tác giả nhập cook time = 0 và prep time = 15 phút. Hệ thống xác nhận tổng thời gian là 15 phút $> 0$ và chấp nhận hợp lệ theo BR-20.
  - *AF-16.2 (Công thức không có ảnh tải lên):* Tác giả không upload ảnh nào (0 ảnh). Hệ thống tự động gán ảnh đại diện mặc định theo loại ăn chay của món ăn (BR-20).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-16.1 (Dữ liệu không thỏa mãn validation profile — Xử lý phía Frontend):* Khi người dùng nhấn nút đăng bài, nếu có bất kỳ trường nào vi phạm ngưỡng hợp lệ (ví dụ: thiếu hướng dẫn chế biến hoặc hướng dẫn $< 10$ hoặc $> 5.000$ ký tự, thiếu thể loại món `dish_category`, nguyên liệu chưa có tỷ lệ quy đổi sang gam, có ảnh nhưng không chọn ảnh bìa, hoặc khẩu phần $> 50$), giao diện người dùng chặn gửi yêu cầu không hợp lệ, giữ lại toàn bộ nội dung đã nhập trong biểu mẫu và hiển thị thông báo lỗi chi tiết để tác giả chỉnh sửa mà không bị mất dữ liệu đã nhập.
  - *SF-16.1 (Thẩm định độc lập bắt buộc tại Backend & Không lưu rác DB):* Toàn bộ quy tắc kiểm tra hợp lệ bắt buộc phải được thực thi độc lập và toàn diện tại tầng Backend của máy chủ theo NFR-10, tuyệt đối không phụ thuộc vào việc kiểm tra của Frontend. Nếu nhận yêu cầu có dữ liệu không đạt chuẩn, máy chủ độc lập từ chối yêu cầu và thông báo chi tiết lỗi; máy chủ TUYỆT ĐỐI KHÔNG ghi bất kỳ bản ghi bài viết hay tài nguyên dở dang nào vào cơ sở dữ liệu (Database), bảo đảm không phát sinh dữ liệu rác (phù hợp với FR-24 OUT_OF_SCOPE).

#### 5. Hậu điều kiện (Postconditions)
- Bài công thức đạt chuẩn được lưu trữ an toàn và xuất bản công khai trực tiếp.
- Dữ liệu chuẩn mực sẵn sàng cho các chức năng Thẻ món (FR-17), Chi tiết (FR-20), Tính dinh dưỡng (FR-39).

---

<a id="fr-19"></a>
## FR-19 — Nhập nguyên liệu linh hoạt và định lượng số học chuẩn

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-19).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Tác giả đang ở giao diện soạn thảo hoặc chỉnh sửa bài công thức (FR-04, FR-16).
- **Kích hoạt (Trigger):**
  - Tác giả tương tác với phần "Danh sách nguyên liệu" trong biểu mẫu soạn bài.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả mở phần nhập nguyên liệu của bài công thức. Mặc định có sẵn 1 dòng nguyên liệu trống.
  - Bước 2: Tác giả gõ tên nguyên liệu vào ô tìm kiếm nguyên liệu:
    - Hệ thống tự động gợi ý danh sách các nguyên liệu khớp từ Danh mục nguyên liệu chuẩn (FR-18).
    - Tác giả có thể nhấp chọn một nguyên liệu gợi ý (hệ thống ghi nhận liên kết `ingredientId` chuẩn).
    - Hoặc nếu không thấy, tác giả giữ nguyên tên tự do mình vừa gõ (hệ thống ghi nhận tên tự do, `ingredientId = null`).
  - Bước 3: Tác giả nhập định lượng số học dương (ví dụ: `200`, `1.5`) và chọn đơn vị đo từ danh mục `UNIT` (ví dụ: gam, ml, muỗng canh, quả, bìa). Hệ thống không hỗ trợ tùy chọn "vừa đủ".
  - Bước 4: Tác giả nhấn nút "Thêm nguyên liệu" để tạo thêm dòng mới (tối đa 50 dòng theo BR-19).
  - Bước 5: Khi lưu hoặc công khai bài viết, hệ thống kiểm tra: số dòng từ 1 đến 50, mọi dòng có `quantity > 0` và đơn vị đo hợp lệ; đồng thời kiểm tra tính khả dụng của tỷ lệ quy đổi sang gam trong `INGREDIENT_UNIT_CONVERSION` (BR-73).
  - Bước 6: Toàn bộ đạt chuẩn, hệ thống lưu trữ danh sách nguyên liệu và bảo toàn chuỗi tên hiển thị do tác giả nhập.
- **Luồng thay thế (Alternative Flows):**
  - *AF-19.1 (Xóa bớt hoặc đổi thứ tự dòng nguyên liệu):* Tác giả nhấn nút xóa dòng hoặc kéo thả để đổi thứ tự các nguyên liệu trong danh sách.
  - *AF-19.2 (Chỉnh sửa nguyên liệu đã lưu):* Tác giả sửa đổi tên hoặc định lượng của bất kỳ dòng nào khi cập nhật bài viết (FR-44).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-19.1 (Để trống nguyên liệu hoặc vượt quá 50 dòng):* Nếu tác giả không nhập dòng nguyên liệu nào (0 dòng) hoặc tạo quá 50 dòng nguyên liệu, hệ thống chặn lưu bài và hiển thị thông báo lỗi yêu cầu số dòng từ 1 đến 50 (BR-19).
  - *EF-19.2 (Dòng nguyên liệu thiếu số lượng, số lượng $\le 0$ hoặc thiếu đơn vị):* Nếu một dòng nguyên liệu có tên nhưng bỏ trống số lượng, nhập số $\le 0$, nhập chữ phi số học ("vừa đủ") hoặc bỏ trống đơn vị, hệ thống yêu cầu nhập số lượng số học dương và chọn đơn vị hợp lệ (BR-14).
  - *EF-19.3 (Chặn xuất bản khi thiếu tỷ lệ quy đổi sang gam):* Nếu tác giả dùng đơn vị đo lường (như quả, bìa, muỗng) mà nguyên liệu đó chưa được cấu hình tỷ lệ quy đổi về gam trong `INGREDIENT_UNIT_CONVERSION`, hệ thống từ chối công khai bài viết và hiển thị lỗi validation theo BR-19 và BR-73.
  - *SF-19.1 (Làm sạch chuỗi tên nguyên liệu tự do):* Tên nguyên liệu do tác giả tự do gõ được lọc và làm sạch mã độc (sanitize HTML) nhằm ngăn chặn tấn công XSS qua dữ liệu đầu vào (NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Danh sách nguyên liệu được lưu trữ chuẩn xác, sẵn sàng phục vụ hiển thị chi tiết (FR-20), tính toán dinh dưỡng (FR-39) và tổng hợp danh sách mua sắm (FR-54).

---

<a id="fr-21"></a>
## FR-21 — AI hỗ trợ tạo giới thiệu hoặc hướng dẫn chế biến không lưu nháp server

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-21).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản Chuyên gia hợp lệ (FR-03, FR-04).
  - Tài khoản Chuyên gia đang đăng ký gói Plus hoặc Pro còn hiệu lực (FR-10, BR-02, BR-03).
  - Tác giả đã nhập ít nhất Tên món ăn và danh sách Nguyên liệu trong biểu mẫu (FR-16, FR-19).
- **Kích hoạt (Trigger):**
  - Tác giả nhấn nút "Nhờ AI gợi ý giới thiệu" hoặc "Nhờ AI gợi ý hướng dẫn chế biến" trong trình soạn thảo.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả nhấn nút "Nhờ AI gợi ý hướng dẫn chế biến" (hoặc "gợi ý giới thiệu").
  - Bước 2: Hệ thống kiểm tra quyền tính năng (Feature Entitlement) của tài khoản (FR-10, BR-02, BR-03).
  - Bước 3: Hệ thống trích xuất thông tin tác giả đã nhập: Tên món ăn, Loại ăn chay, Thể loại món `dish_category`, Khẩu phần, và Danh sách tên các nguyên liệu kèm định lượng số dương (FR-19).
  - Bước 4: Hệ thống gửi prompt tới Google Gemini qua API bảo mật phía server (BR-06, đáp ứng NFR-03). Prompt bắt buộc yêu cầu:
    - Chỉ được sử dụng các nguyên liệu tác giả đã cung cấp.
    - Tuyệt đối không tự ý thêm nguyên liệu mới ngoài danh sách.
    - Định dạng kết quả trả về: Nếu gợi ý hướng dẫn chế biến, trả về văn bản hướng dẫn chi tiết mạch lạc (từ 10 đến 5.000 ký tự); nếu gợi ý giới thiệu, trả về đoạn văn bản súc tích $\le 2.000$ ký tự.
  - Bước 5: Google Gemini phản hồi nội dung đề xuất thành công trong thời gian quy định tại NFR-03.
  - Bước 6: Hệ thống ghi nhận dữ liệu đo lường kỹ thuật (telemetry nếu có theo BR-04) và không áp dụng quota tính theo lượt.
  - Bước 7: Hệ thống đưa nội dung AI sinh trực tiếp vào ô nhập liệu tương ứng trên trình soạn thảo giao diện phía client (điền vào ô `instructions` hoặc ô `description`) dưới dạng có thể chỉnh sửa hoàn toàn.
  - Bước 8: Tác giả tự do đọc lại, chỉnh sửa câu từ, bổ sung kinh nghiệm cá nhân cho phù hợp với thực tế chế biến của mình. AI tuyệt đối không tự động công khai bài viết.
  - Bước 9: Khi tác giả chủ động nhấn "Đăng công thức", bài viết được kiểm tra validation (bao gồm bắt buộc có hướng dẫn chế biến từ 10–5.000 ký tự theo BR-19) và xuất bản trực tiếp (BR-07, BR-19, BR-25).
  - Bước 10: Toàn bộ quá trình TUYỆT ĐỐI KHÔNG ghi bất kỳ bản ghi lưu nháp tạm thời nào vào cơ sở dữ liệu server (FR-24).
- **Luồng thay thế (Alternative Flows):**
  - *AF-21.1 (Tác giả không hài lòng với nội dung AI gợi ý):* Tác giả có thể nhấn nút "Xóa gợi ý" để quay về trạng thái trống hoặc tự gõ lại bằng tay.
  - *AF-21.2 (Tài khoản thuộc gói Free):* Nếu tài khoản chưa nâng cấp lên gói Plus hoặc Pro, hệ thống hiển thị thông báo hướng dẫn nâng cấp gói dịch vụ để mở khóa tính năng AI hỗ trợ soạn bài (FR-10, BR-02), đồng thời gợi ý tác giả tự nhập nội dung bằng tay để tiếp tục đăng bài bình thường (BR-16).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-21.1 (Lỗi kết nối dịch vụ AI hoặc timeout):* Nếu dịch vụ AI gặp sự cố hoặc quá thời gian phản hồi quy định tại NFR-03, hệ thống thông báo lỗi kỹ thuật thân thiện và giữ nguyên toàn bộ dữ liệu tác giả đã nhập trên form (BR-04, BR-16).
  - *SF-21.1 (Không lưu nháp server - Tuân thủ ranh giới FR-24):* Hệ thống không cung cấp chức năng lưu nháp trên máy chủ cho tính năng này; nếu tác giả rời khỏi biểu mẫu trước khi bấm công khai, dữ liệu dở dang không được bảo đảm lưu trữ bền vững trên máy chủ (FR-24).

#### 5. Hậu điều kiện (Postconditions)
- Nội dung gợi ý của AI được điền vào ô hướng dẫn chế biến hoặc giới thiệu trên client để tác giả toàn quyền kiểm soát.
- Dữ liệu đo lường kỹ thuật được ghi nhận phục vụ đối soát chi phí (nếu có theo BR-04).
- Cơ sở dữ liệu server không lưu trữ bất kỳ bản ghi nháp dở dang nào.

---

<a id="fr-27"></a>
## FR-27 — Biểu mẫu báo cáo bài công thức theo 6 nhóm lý do

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-27).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member` (Người báo cáo): Thao tác trên biểu mẫu báo cáo.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Member đã đăng nhập và đã kích hoạt biểu mẫu báo cáo từ bài công thức (FR-26).
- **Trigger:** Biểu mẫu báo cáo mở ra trên màn hình.

#### 6. Luồng sự kiện (Flow of Events)

##### A. Luồng Nhập và gửi biểu mẫu báo cáo (UC-27.1)
1. **Main Flow:**
   - Bước 1: Biểu mẫu hiển thị 6 nút chọn (radio button) tương ứng với 6 nhóm lý do trên.
   - Bước 2: Member chọn 1 trong 6 lý do.
   - Bước 3: Nếu Member chọn nhóm từ 1 đến 5: ô nhập "Mô tả bổ sung (tùy chọn)" cho phép để trống hoặc nhập văn bản tối đa 500 ký tự.
   - Bước 4: Nếu Member chọn nhóm 6 ("Khác"): ô nhập mô tả chuyển thành bắt buộc, hiển thị nhãn "(Bắt buộc)" kèm bộ đếm ký tự.
   - Bước 5: Member nhấn nút "Gửi báo cáo".
   - Bước 6: Hệ thống kiểm tra hợp lệ: lý do hợp lệ và mô tả đáp ứng quy định.
   - Bước 7: Dữ liệu được gửi tới máy chủ để hoàn tất tạo báo cáo (FR-26).
2. **Error Flows:**
   - *Chưa chọn lý do:* Member nhấn gửi mà chưa tích chọn lý do nào -> Hệ thống báo lỗi yêu cầu chọn 1 lý do.
   - *Chọn "Khác" nhưng bỏ trống mô tả:* Member chọn lý do "Khác" nhưng để trống hoặc nhập dưới 10 ký tự -> Hệ thống báo lỗi: "Vui lòng nhập mô tả chi tiết từ 10 đến 500 ký tự khi chọn lý do Khác."

#### 7. Hậu điều kiện (Postconditions)
- Báo cáo với lý do và mô tả chuẩn hóa được gửi thành công tới hệ thống tiếp nhận.

---

<a id="fr-30"></a>
## FR-30 — Bổ sung thông tin và ngăn trùng lặp báo cáo mở

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-30).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member` (Người gửi báo cáo): Bổ sung thông tin cho báo cáo đang mở.
  - `Administrator`: Xem danh sách báo cáo gom nhóm theo bài viết.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Member có một báo cáo đang ở trạng thái `OPEN` hoặc `IN_REVIEW`.
- **Trigger:** Member nhấn "Bổ sung thông tin" trong trang chi tiết báo cáo của mình, hoặc Admin mở chế độ xem nhóm báo cáo.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Bổ sung thông tin cho báo cáo đang mở (UC-30.1)
1. **Main Flow:**
   - Bước 1: Member truy cập báo cáo đang mở của mình trong "Báo cáo của tôi".
   - Bước 2: Member nhấn nút "Bổ sung thông tin".
   - Bước 3: Member nhập thêm văn bản mô tả (tối đa 500 ký tự) và nhấn "Lưu bổ sung".
   - Bước 4: Hệ thống nối thêm thông tin này vào nhật ký mô tả của báo cáo kèm mốc thời gian bổ sung.
   - Bước 5: Hệ thống thông báo cập nhật thông tin thành công.

##### B. Luồng Ngăn chặn tạo báo cáo mở trùng lặp
1. **Main Flow:**
   - Bước 1: Member nhấn nút "Báo cáo bài viết" trên bài công thức A.
   - Bước 2: Hệ thống kiểm tra cơ sở dữ liệu và phát hiện Member này đã có một báo cáo về bài viết A đang ở trạng thái `OPEN` hoặc `IN_REVIEW`.
   - Bước 3: Hệ thống không mở biểu mẫu tạo báo cáo mới, mà hiển thị thông báo: "Bạn đã có báo cáo đang được xem xét cho bài viết này. Bạn có muốn bổ sung thêm thông tin vào báo cáo hiện có không?" kèm nút bấm dẫn tới chức năng bổ sung thông tin.

##### C. Luồng Báo cáo lại sau khi giải quyết
1. **Main Flow:**
   - Bước 1: Nếu báo cáo trước đó của Member về bài viết A đã được Admin giải quyết (`RESOLVED`).
   - Bước 2: Sau đó, tác giả chỉnh sửa bài viết và phát sinh vi phạm mới, hoặc Member phát hiện vấn đề mới chưa được giải quyết.
   - Bước 3: Member có quyền gửi một báo cáo mới, nhưng bắt buộc phải nhập mô tả chi tiết giải thích vấn đề mới phát sinh (tối thiểu 10 ký tự).

##### D. Luồng Admin xem báo cáo gom nhóm theo bài (UC-30.2)
1. **Main Flow:**
   - Bước 1: Administrator mở mục "Báo cáo vi phạm" trên trang Quản trị.
   - Bước 2: Hệ thống hiển thị chế độ xem gom nhóm theo bài viết: mỗi dòng đại diện cho một bài viết bị báo cáo kèm số lượng phản ánh tích lũy (ví dụ: "Bài viết Canh Chua Đậu Phụ — 4 báo cáo đang mở").
   - Bước 3: Admin nhấp vào bài viết để mở danh sách chi tiết toàn bộ các báo cáo từ các người dùng khác nhau trên bài viết đó.

#### 6. Hậu điều kiện (Postconditions)
- Thông tin bổ sung được lưu thành công vào bản ghi báo cáo hiện có.
- Không phát sinh bản ghi trùng lặp trong hàng đợi quản trị.

---

<a id="fr-48"></a>
## FR-48 — Báo cáo bình luận, reply và Administrator hậu kiểm

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-48).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng muốn báo cáo đã đăng nhập tài khoản Member hợp lệ (FR-03, BR-24).
  - Bình luận hoặc reply mục tiêu đang hiển thị công khai.
- **Kích hoạt (Trigger):**
  - Member nhấn vào nút hoặc biểu tượng "Báo cáo vi phạm" (Cờ báo cáo) tại một bình luận cụ thể.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member nhấn nút "Báo cáo" tại một bình luận hoặc reply.
  - Bước 2: Hệ thống kiểm tra phiên đăng nhập (BR-24) và kiểm tra xem tài khoản này đã có báo cáo nào đang ở trạng thái `Pending` đối với bình luận này chưa (BR-29).
  - Bước 3: Hệ thống hiển thị biểu mẫu báo cáo gồm: Lựa chọn nhóm lý do vi phạm (Ngôn từ thù ghét/xúc phạm, Quảng cáo/Spam rác, Công kích cá nhân, Thông tin sai lệch) và Ô nhập mô tả chi tiết bổ sung (tối đa 500 ký tự) (BR-25).
  - Bước 4: Member chọn lý do, nhập mô tả và nhấn "Gửi báo cáo".
  - Bước 5: Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào (bắt buộc chọn lý do, mô tả không chứa mã độc).
  - Bước 6: Hệ thống tạo bản ghi báo cáo mới gắn liền với ID bình luận, lưu trữ an toàn ID người báo cáo (bảo mật nghiêm ngặt theo BR-28), và chuyển báo cáo vào hàng đợi kiểm duyệt với trạng thái ban đầu là `Pending` (BR-58).
  - Bước 7: Bình luận mục tiêu vẫn tiếp tục hiển thị bình thường cho cộng đồng xem (BR-23, BR-58); hệ thống thông báo gửi báo cáo thành công cho Member.
  - Bước 8: Administrator truy cập Trung tâm kiểm duyệt hậu kiểm (FR-06, FR-28), xem xét nội dung bình luận bị báo cáo và các lý do đi kèm.
  - Bước 9: Administrator đưa ra một trong các quyết định xử lý:
    - *Bác bỏ báo cáo (Dismiss):* Xác định bình luận không vi phạm; báo cáo chuyển sang `Dismissed`. Bình luận giữ nguyên hiển thị.
    - *Gỡ bỏ bình luận (Delete/Hide):* Xác định bình luận vi phạm; hệ thống ẩn/xóa nội dung bình luận đó (hiển thị tombstone nếu có reply con theo FR-46/BR-66), và chuyển báo cáo sang `Resolved`.
  - Bước 10: Hệ thống kích hoạt thông báo kết quả xử lý cho Member đã gửi báo cáo và thông báo chế tài cho tác giả của bình luận vi phạm (FR-49).
- **Luồng thay thế (Alternative Flows):**
  - *AF-48.1 (Nhiều Member báo cáo cùng một bình luận):* Nếu nhiều Member khác nhau báo cáo cùng một bình luận, hệ thống ghi nhận các lượt báo cáo riêng biệt và gom nhóm (grouping) dưới cùng một mục kiểm duyệt để Administrator dễ dàng đánh giá mức độ nghiêm trọng.
  - *AF-48.2 (Bổ sung mô tả khi đã có báo cáo mở):* Nếu Member bấm báo cáo lại bình luận mà mình đã gửi báo cáo đang chờ xử lý, hệ thống cho phép bổ sung thêm mô tả thay vì tạo bản ghi báo cáo trùng lặp (BR-29, FR-30).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-48.1 (Guest cố gắng báo cáo):* Hệ thống chặn mở biểu mẫu và yêu cầu đăng nhập (BR-05, BR-24).
  - *SF-48.1 (Bảo mật tuyệt đối danh tính người báo cáo):* Hệ thống tuyệt đối không hiển thị tên hoặc ID của người báo cáo cho tác giả bình luận hay bất kỳ người dùng thông thường nào khác (BR-28, FR-29). Thông tin này chỉ có Administrator mới có quyền xem xét phục vụ kiểm duyệt.
  - *SF-48.2 (Không tự động ẩn bình luận theo số lượng báo cáo):* Số lượng lượt báo cáo dù lớn đến đâu cũng chỉ là căn cứ ưu tiên trong danh sách của Administrator; hệ thống tuyệt đối không tự động ẩn hay xóa bình luận khi chưa có thao tác xác nhận của Administrator (BR-23, BR-58).

#### 5. Hậu điều kiện (Postconditions)
- Bản ghi báo cáo bình luận được ghi nhận và đưa vào hàng đợi kiểm duyệt của Administrator.
- Khi Administrator phê duyệt xử lý, trạng thái bình luận và kết quả báo cáo được cập nhật đồng bộ.

---
