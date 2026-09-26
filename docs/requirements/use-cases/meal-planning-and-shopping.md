> **Document:** Use Case Specifications — M05
> **File:** `docs/requirements/use-cases/meal-planning-and-shopping.md`
> **Version:** v2.0.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-26
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M05

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns the required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-09"></a>
## FR-09 — Authorized User tạo và chỉnh lịch ăn tuần

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-09).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản Member hợp lệ (FR-03, BR-32).
- **Kích hoạt (Trigger):**
  - Member truy cập vào mục "Lịch ăn tuần" (Meal Planner) từ thanh điều hướng chính.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member mở trang Lịch ăn tuần. Hệ thống tải dữ liệu lịch ăn của tuần hiện tại (mặc định từ Thứ Hai đến Chủ Nhật).
  - Bước 2: Tại mỗi ngày, hệ thống trình bày lưới 3 ô bữa ăn cố định: Sáng, Trưa, Tối (BR-36).
  - Bước 3: Member nhấn nút "Thêm món" tại một bữa ăn cụ thể.
  - Bước 4: Hệ thống hiển thị hộp thoại chọn món từ Danh sách công thức đã lưu (FR-32) hoặc tìm kiếm nhanh từ kho công thức công khai (FR-08, FR-20).
  - Bước 5: Member chọn một công thức công khai hợp lệ.
  - Bước 6: Hệ thống kiểm tra tính duy nhất: Một công thức không được trùng lặp trong cùng một bữa ăn của cùng một ngày (BR-37).
  - Bước 7: Hệ thống ghi nhận liên kết món ăn vào bữa ăn của ngày được chọn với số khẩu phần mặc định là 1 (FR-37).
  - Bước 8: Giao diện cập nhật thẻ món ăn vào ô bữa ăn tương ứng ngay lập tức.
- **Luồng thay thế (Alternative Flows):**
  - *AF-09.1 (Gỡ món khỏi bữa ăn):* Member nhấn biểu tượng xóa/gỡ món trên thẻ món trong bữa ăn. Hệ thống gỡ liên kết món ăn khỏi bữa ăn đó mà hoàn toàn không ảnh hưởng đến bài viết gốc hay danh sách công thức đã lưu (BR-35).
  - *AF-09.2 (Đổi món ăn):* Member nhấn nút "Đổi món", chọn một công thức khác thay thế. Hệ thống cập nhật món mới vào vị trí bữa ăn.
  - *AF-09.3 (Xử lý bài viết gốc bị xóa hoặc ẩn - Tombstone):* Khi Member xem lịch ăn của tuần có chứa bài công thức đã bị tác giả xóa (FR-44) hoặc bị Administrator ẩn do vi phạm (BR-26), hệ thống giữ nguyên vị trí ô lịch đó nhưng hiển thị thẻ xám ghi rõ: *"Công thức không còn khả dụng"* và vô hiệu hóa link xem chi tiết (BR-35).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-09.1 (Guest truy cập lịch ăn):* Nếu người dùng chưa đăng nhập cố gắng truy cập trang Lịch ăn, hệ thống điều hướng đến trang Đăng nhập và hiển thị thông báo yêu cầu đăng nhập (BR-05, BR-32).
  - *SF-09.1 (Kiểm soát quyền riêng tư lịch ăn):* Lịch ăn tuần thuộc quyền riêng tư của tài khoản Member; hệ thống áp dụng phân quyền RBAC nghiêm ngặt, tuyệt đối không cho phép người dùng khác xem hoặc chỉnh sửa lịch ăn của tài khoản (NFR-08, NFR-09).

#### 5. Hậu điều kiện (Postconditions)
- Kế hoạch ăn uống trong tuần của Member được lưu trữ an toàn trong cơ sở dữ liệu.
- Dữ liệu lịch ăn sẵn sàng làm đầu vào cho chức năng Kiểm tra dinh dưỡng ngày (FR-37) và Tạo danh sách mua sắm (FR-53).

---

<a id="fr-53"></a>
## FR-53 — Tạo và quản lý Shopping List checklist tương tác

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-53).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản Member hợp lệ (FR-03, BR-32).
- **Kích hoạt (Trigger):**
  - Member nhấn nút "Tạo danh sách mua sắm" trên trang Lịch ăn tuần hoặc chọn "Thêm vào danh sách mua sắm" từ các bài công thức.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member chọn nguồn tạo danh sách mua sắm: chọn phạm vi ngày trong Lịch ăn tuần (FR-09) hoặc chọn các bài công thức cụ thể.
  - Bước 2: Member nhấn "Tạo danh sách mua sắm".
  - Bước 3: Hệ thống trích xuất toàn bộ nguyên liệu từ các bài công thức tương ứng theo số khẩu phần đã thiết lập trong lịch (FR-37).
  - Bước 4: Hệ thống áp dụng quy tắc gộp nguyên liệu tự động an toàn (FR-54) để gom nhóm các nguyên liệu trùng lặp.
  - Bước 5: Hệ thống tạo danh sách mua sắm mới và hiển thị trên giao diện dạng Checklist tương tác:
    - Mỗi mục hàng hiển thị: Tên nguyên liệu, Định lượng tổng hợp, Đơn vị đo, Hộp kiểm (Checkbox) trạng thái, và Danh mục thực phẩm.
  - Bước 6: Member nhấn nút "Thêm mặt hàng khác" để tự gõ thêm các đồ cần mua phát sinh (ví dụ: nước rửa bát, dầu ăn chay, giấy ăn); hệ thống bổ sung ngay vào danh sách.
  - Bước 7: Trong lúc đi chợ, Member chạm/click vào hộp kiểm bên cạnh món hàng đã mua; hệ thống chuyển trạng thái mục đó sang `Đã mua` (gạch ngang văn bản nhẹ nhàng và đổi màu xám) và lưu trạng thái vào cơ sở dữ liệu.
- **Luồng thay thế (Alternative Flows):**
  - *AF-53.1 (Bỏ đánh dấu đã mua):* Member chạm lại vào mục đã mua; hệ thống bỏ gạch ngang và chuyển trạng thái về `Chưa mua`.
  - *AF-53.2 (Chỉnh sửa hoặc xóa mục):* Member nhấn nút sửa để đổi số lượng/đơn vị, hoặc nhấn nút xóa để gỡ một mặt hàng ra khỏi danh sách mua sắm.
  - *AF-53.3 (Xóa các mục đã mua):* Member nhấn nút "Dọn dẹp danh sách" để xóa nhanh toàn bộ các mặt hàng đã được tick hoàn thành.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-53.1 (Lịch ăn trống không có món nào):* Nếu tuần được chọn chưa có bất kỳ món ăn nào, hệ thống thông báo: *"Chưa có món ăn trong lịch ăn để tạo danh sách mua sắm"* và hướng dẫn thêm món vào lịch ăn trước (FR-09).
  - *SF-53.1 (Làm sạch đầu vào mục thủ công):* Tên các mặt hàng do Member tự nhập thủ công bắt buộc được lọc sạch mã độc XSS trước khi lưu và hiển thị (NFR-10).
  - *SF-53.2 (Kiểm soát phân quyền danh sách cá nhân):* Danh sách mua sắm thuộc quyền sở hữu riêng của Member; tài khoản khác không có quyền xem hoặc chỉnh sửa (NFR-08, NFR-09).

#### 5. Hậu điều kiện (Postconditions)
- Danh sách mua sắm được lưu trữ và cập nhật trạng thái checklist bền vững.
- Dữ liệu sẵn sàng phục vụ tính năng Sao chép Clipboard và Xuất file văn bản (FR-55).

---

<a id="fr-54"></a>
## FR-54 — Tự động tổng hợp nguyên liệu trùng an toàn trong Shopping List

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-54).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Hệ thống tiếp nhận tập hợp các nguyên liệu cần mua từ Lịch ăn tuần hoặc các bài công thức được chọn (FR-53).
- **Kích hoạt (Trigger):**
  - Tiến trình tạo hoặc cập nhật Danh sách mua sắm được kích hoạt.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Hệ thống đọc toàn bộ danh sách các dòng nguyên liệu nguồn kèm `ingredientId`, số lượng số dương, đơn vị đo chuẩn (`unitId`) và danh mục thực phẩm.
  - Bước 2: Hệ thống gom các dòng có cùng `ingredientId` thành từng nhóm xử lý riêng biệt.
  - Bước 3: Trong mỗi nhóm nguyên liệu cùng `ingredientId`:
    - *Trường hợp 1 (Cùng đơn vị khối lượng g / kg):* Hệ thống quy đổi toàn bộ về gam ($1\text{kg} = 1.000\text{g}$), cộng dồn tổng số gam. Nếu tổng $\ge 1.000\text{g}$, hệ thống quy đổi hiển thị thành dạng kilôgam (ví dụ: $1.250\text{g} \rightarrow 1,25\text{kg}$).
    - *Trường hợp 2 (Cùng đơn vị thể tích ml / l):* Hệ thống quy đổi toàn bộ về mililít ($1\text{l} = 1.000\text{ml}$), cộng dồn tổng số ml. Nếu tổng $\ge 1.000\text{ml}$, hệ thống quy đổi hiển thị thành lít (ví dụ: $1.500\text{ml} \rightarrow 1,5\text{l}$).
    - *Trường hợp 3 (Cùng đơn vị đếm):* Nếu các dòng có cùng đơn vị đếm (như "quả", "củ", "bìa"), hệ thống cộng dồn phần số lượng theo đơn vị đó.
    - *Trường hợp 4 (Khác chiều đo lường nhưng có quy tắc chuyển đổi trong INGREDIENT_UNIT_CONVERSION):* Hệ thống sử dụng hệ số quy đổi để đưa về đơn vị gram chuẩn và cộng dồn vào tổng số gam của nguyên liệu đó.
  - Bước 4: Nếu trong nhóm xuất hiện các dòng có đơn vị không tương thích nhau và KHÔNG có quy tắc quy đổi trong `INGREDIENT_UNIT_CONVERSION`:
    - Hệ thống KHÔNG tự ý suy diễn hoặc gán hệ số ước tính (BR-14).
    - Hệ thống giữ tách biệt thành các dòng riêng biệt (ví dụ: "Nấm rơm: 200g" và "Nấm rơm: 1 khay").
  - Bước 5: Hệ thống phân bổ các dòng nguyên liệu sau khi tổng hợp vào các Danh mục thực phẩm tương ứng (Rau củ, Đậu hạt, Gia vị...).
  - Bước 6: Trả về kết quả danh sách mua sắm đã tổng hợp hoàn tất cho giao diện hiển thị (FR-53).
- **Luồng thay thế (Alternative Flows):**
  - *AF-54.1 (Nguyên liệu tự do không có ingredientId):* Đối với các nguyên liệu do tác giả tự gõ không liên kết chuẩn (`ingredientId = null` theo FR-19), hệ thống chỉ gom các dòng có chuỗi tên hiển thị giống hệt nhau (không phân biệt hoa thường) và có cùng đơn vị đo lường; nếu khác đơn vị, giữ tách dòng độc lập.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-54.1 (Định lượng không hợp lệ hoặc số âm):* Nếu phát hiện dòng nguyên liệu nguồn có số lượng $\le 0$, hệ thống bỏ qua phần số âm và ghi nhận cảnh báo dữ liệu.
  - *SF-54.1 (Đảm bảo tính chính xác định lượng):* Thuật toán sử dụng kiểu dữ liệu số thực chính xác cao để tính toán cộng dồn, tránh sai số làm tròn khi quy đổi đơn vị (NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Danh sách mua sắm được tối ưu hóa số lượng dòng, hiển thị trực quan và an toàn.
- Người dùng nắm bắt chính xác khối lượng cần mua mà không bị nhầm lẫn định lượng.

---

<a id="fr-55"></a>
## FR-55 — Sao chép clipboard và xuất file text Shopping List

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-55).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Member đã đăng nhập và danh sách mua sắm hiện tại có ít nhất một mặt hàng (FR-53).
- **Kích hoạt (Trigger):**
  - Member nhấn nút "Sao chép vào Clipboard" hoặc "Xuất file .txt" trên thanh công cụ của trang Danh sách mua sắm.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow - Sao chép Clipboard):**
  - Bước 1: Member nhấn nút "Sao chép vào Clipboard" trên giao diện Danh sách mua sắm.
  - Bước 2: Hệ thống định dạng toàn bộ các mặt hàng trong danh sách thành một chuỗi văn bản UTF-8 chuẩn mực:
    - Dòng tiêu đề: `DANH SÁCH ĐI CHỢ CHAY - [Ngày tạo / Tuần]`
    - Phân chia theo từng Danh mục thực phẩm:
      ```text
      --- RAU CỦ & NẤM ---
      [ ] Nấm rơm: 300g
      [x] Cà rốt: 2 củ
      --- ĐẬU & CHẾ PHẨM ---
      [ ] Đậu phụ mơ: 4 bìa
      ```
  - Bước 3: Hệ thống ghi chuỗi văn bản vào bộ nhớ tạm (Clipboard API) của thiết bị người dùng.
  - Bước 4: Giao diện hiển thị thông báo nổi (toast message): *"Đã sao chép danh sách mua sắm vào khay nhớ tạm!"*.
- **Luồng thay thế (Alternative Flows - Xuất file .txt):**
  - *AF-55.1 (Tải xuống file .txt):*
    - Bước 1: Member nhấn nút "Xuất file .txt".
    - Bước 2: Hệ thống đóng gói chuỗi văn bản checklist có cấu trúc ở trên thành tệp tin có phần mở rộng `.txt` với bộ mã ký tự UTF-8 (bảo đảm hiển thị đúng tiếng Việt có dấu trên mọi hệ điều hành).
    - Bước 3: Đặt tên tệp tin tự động theo quy chuẩn: `Danh_sach_mua_sam_YYYYMMDD.txt`.
    - Bước 4: Kích hoạt luồng tải xuống (download stream) trực tiếp về thiết bị của Member.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-55.1 (Danh sách mua sắm trống):* Nếu danh sách mua sắm chưa có mặt hàng nào, các nút sao chép và xuất file bị vô hiệu hóa kèm tooltip: *"Danh sách mua sắm đang trống"*.
  - *EF-55.2 (Trình duyệt chặn quyền Clipboard):* Nếu trình duyệt không cấp quyền truy cập Clipboard, hệ thống hiển thị hộp thoại chứa toàn bộ văn bản và nút "Chọn tất cả" để người dùng sao chép thủ công.
  - *SF-55.1 (Bảo vệ dữ liệu khi xuất file):* File văn bản xuất ra không chứa bất kỳ thông tin nhạy cảm nào ngoài danh sách các mặt hàng đi chợ (NFR-08).

#### 5. Hậu điều kiện (Postconditions)
- Dữ liệu danh sách mua sắm được sao chép vào Clipboard hoặc lưu thành tệp `.txt` an toàn trên thiết bị của Member.
- Dữ liệu gốc trong hệ thống không bị thay đổi.

---
