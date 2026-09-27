> **Document:** Use Case Specifications — M05
> **File:** `docs/requirements/use-cases/meal-planning-and-shopping.md`
> **Version:** v2.1.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-27
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M05

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-09"></a>
## FR-09 — Authorized User tạo và chỉnh lịch ăn tuần

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-09).

#### Shared Security Flow — SF-09.1 (Kiểm soát quyền riêng tư lịch ăn)
Lịch ăn thuộc quyền riêng tư của Member sở hữu tài khoản; hệ thống không cho tài khoản khác xem hoặc chỉnh sửa (NFR-08, NFR-09). Quy tắc này áp dụng cho cả ba UC bên dưới.

<a id="uc-09-1"></a>
### UC-09.1 — Tạo mới hoặc mở xem Lịch ăn tuần

#### Goal
Cho Member tạo hoặc mở kế hoạch ăn cho tuần hiện tại hoặc tuần tiếp theo.

#### Primary Actor
`Member / Authorized User`.

#### Trigger
Member mở mục “Lịch ăn tuần” hoặc chọn tuần cần xem/lập kế hoạch.

#### Preconditions
Member đã đăng nhập; Guest không có quyền lưu lịch ăn.

#### Main Flow
1. Member mở Lịch ăn tuần; mặc định hệ thống tải tuần hiện tại.
2. Member có thể chọn tuần hiện tại hoặc một tuần trong tương lai.
3. Hệ thống hiển thị tuần được chọn gồm bảy ngày từ Thứ Hai đến Chủ Nhật, mỗi ngày có đúng ba bữa cố định: Sáng, Trưa, Tối; các mục đã lưu của tuần đó được tải nếu có.
4. Member xem kế hoạch hoặc tiếp tục sang UC-09.2 để thêm, đổi hay gỡ công thức trong một bữa.

#### Alternative Flows
- **EF-09.1 — Guest truy cập lịch ăn:** Hệ thống yêu cầu đăng nhập và không hiển thị dữ liệu lịch cá nhân.

#### Postconditions
Lịch tuần được hiển thị theo cấu trúc 7 ngày × 3 bữa; UC này không thay đổi bài Recipe Post nguồn.

#### Traceability
- **Parent FR:** [FR-09](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-09).
- **Relevant BR:** [BR-05](../srs/BUSINESS-RULES.md#br-05) (Guest không có quyền lưu); [BR-32](../srs/BUSINESS-RULES.md#br-32) (yêu cầu đăng nhập); [BR-36](../srs/BUSINESS-RULES.md#br-36) (cấu trúc ba bữa cố định).
- **Relevant NFR:** [NFR-08](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08) (quyền riêng tư dữ liệu cá nhân); [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (phân quyền); [NFR-13](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13) (giao diện responsive tiếng Việt).

---

<a id="uc-09-2"></a>
### UC-09.2 — Thêm, đổi hoặc gỡ công thức khỏi bữa ăn

#### Goal
Cho phép Member sắp xếp công thức vào từng bữa ăn trong lịch tuần cá nhân.

#### Primary Actor
`Member / Authorized User`.

#### Trigger
Member chọn “Thêm món”, “Đổi món” hoặc thao tác gỡ trên một ô bữa ăn.

#### Preconditions
Member đã đăng nhập và đang xem lịch tuần của chính mình.

#### Main Flow
1. Member chọn ô bữa ăn cần cập nhật.
2. Hệ thống cho phép chọn một Recipe Post công khai từ danh sách đã lưu hoặc tìm trong kho công thức công khai.
3. Member chọn công thức; hệ thống chặn nếu công thức đã có trong cùng bữa ăn của cùng ngày.
4. Khi thêm hợp lệ, hệ thống lưu liên kết món vào bữa được chọn với khẩu phần mặc định 1 và cập nhật thẻ món trên lịch.

#### Alternative Flows
- **AF-09.1 — Gỡ công thức:** Hệ thống gỡ liên kết khỏi bữa ăn, không xóa Recipe Post nguồn hoặc mục trong danh sách đã lưu.
- **AF-09.2 — Đổi công thức:** Member chọn công thức thay thế; hệ thống cập nhật vị trí bữa ăn sang món mới.

#### Postconditions
Vị trí bữa ăn được cập nhật theo thao tác của Member; danh sách đã lưu và Recipe Post gốc vẫn độc lập với lịch ăn.

#### Traceability
- **Parent FR:** [FR-09](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-09).
- **Relevant BR:** [BR-35](../srs/BUSINESS-RULES.md#br-35) (vòng đời độc lập giữa món đã lưu và lịch ăn); [BR-36](../srs/BUSINESS-RULES.md#br-36) (ba bữa cố định); [BR-37](../srs/BUSINESS-RULES.md#br-37) (không trùng công thức trong cùng bữa/ngày).
- **Relevant NFR:** [NFR-08](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08); [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09).

---

<a id="uc-09-3"></a>
### UC-09.3 — Xem Tombstone khi công thức trong lịch không còn khả dụng

#### Goal
Giúp Member nhận biết công thức đã lưu trong lịch bị tác giả xóa hoặc Admin ẩn mà không làm mất vị trí món trong kế hoạch.

#### Primary Actor
`Member / Authorized User`.

#### Trigger
Member mở lịch tuần có tham chiếu tới Recipe Post đã bị xóa hoặc ẩn.

#### Preconditions
Member đã đăng nhập và có một mục Meal Plan tham chiếu tới bài viết không còn khả dụng.

#### Main Flow
1. **AF-09.3 — Bài viết gốc bị xóa hoặc ẩn:** Khi Member mở lịch, hệ thống giữ nguyên ô có tham chiếu tới bài không còn khả dụng.
2. Hệ thống hiển thị trạng thái Tombstone “Công thức không còn khả dụng” và vô hiệu hóa liên kết xem chi tiết.

#### Postconditions
Member thấy được vị trí món không còn khả dụng; tham chiếu cá nhân trong lịch không bị cascade-delete.

#### Traceability
- **Parent FR:** [FR-09](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-09).
- **Relevant BR:** [BR-26](../srs/BUSINESS-RULES.md#br-26) (quyết định Admin ẩn nội dung); [BR-35](../srs/BUSINESS-RULES.md#br-35) (bảo toàn quan hệ tham chiếu trong lịch).
- **Relevant NFR:** [NFR-08](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08); [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09).

---

<a id="fr-53"></a>
## FR-53 — Tạo và quản lý Shopping List checklist tương tác

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-53).

#### Shared Security Flow — SF-53.2 (Quyền sở hữu Shopping List)
Chỉ Member sở hữu danh sách mới được xem hoặc thay đổi danh sách đó. Hệ thống áp dụng kiểm tra quyền sở hữu cho mọi thao tác thuộc UC-53.1–UC-53.3 (NFR-09).

<a id="uc-53-1"></a>
### UC-53.1 — Tạo Shopping List từ Lịch ăn hoặc công thức đã chọn

#### Goal
Giúp Member tạo checklist mua sắm từ một tuần/toàn bộ hoặc một phần lịch ăn, hoặc từ Recipe Post được chọn.

#### Primary Actor
`Member`.

#### Supporting Actors
Hệ thống quản lý Shopping List; hệ thống tổng hợp nguyên liệu theo [FR-54](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-54).

#### Trigger
Member chọn “Tạo danh sách mua sắm” từ Lịch ăn tuần hoặc “Thêm vào danh sách mua sắm” từ các Recipe Post đã chọn.

#### Preconditions
Member đã đăng nhập; Member đã chọn ít nhất một nguồn tạo danh sách.

#### Main Flow
1. Member chọn toàn bộ tuần/các ngày trong Meal Plan hoặc chọn một hay nhiều Recipe Post.
2. Member xác nhận tạo danh sách mua sắm.
3. Hệ thống trích xuất nguyên liệu theo số khẩu phần đã thiết lập và chuyển các dòng nguồn tới quy trình tổng hợp của FR-54.
4. Hệ thống tạo Shopping List và hiển thị checklist gồm tên nguyên liệu, định lượng, đơn vị, trạng thái checkbox và danh mục thực phẩm.

#### Alternative Flows
- **EF-53.1 — Meal Plan không có món:** Hệ thống thông báo chưa có món để tạo danh sách và hướng dẫn Member thêm món vào lịch trước.

#### Postconditions
Shopping List mới được lưu theo tài khoản Member; các mục đã tổng hợp sẵn sàng cho quản lý checklist và xuất dữ liệu.

#### Traceability
- **Parent FR:** [FR-53](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-53).
- **Related FR:** [FR-09](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-09) (nguồn Meal Plan); [FR-54](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-54) (tổng hợp nguyên liệu).
- **Relevant BR:** [BR-14](../srs/BUSINESS-RULES.md#br-14) (tổng hợp an toàn); [BR-32](../srs/BUSINESS-RULES.md#br-32) (yêu cầu đăng nhập).
- **Relevant NFR:** [NFR-08](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08); [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09); [NFR-13](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13).

---

<a id="uc-53-2"></a>
### UC-53.2 — Thêm, chỉnh sửa hoặc xóa mặt hàng thủ công

#### Goal
Cho phép Member bổ sung và quản lý mặt hàng mua sắm không được sinh từ Recipe Post.

#### Primary Actor
`Member`.

#### Trigger
Member chọn thêm mặt hàng thủ công, sửa một mục hoặc xóa một mục trong Shopping List.

#### Preconditions
Member đã đăng nhập và có quyền sở hữu danh sách đang thao tác.

#### Main Flow
1. Member chọn “Thêm mặt hàng khác” và nhập tên, số lượng/đơn vị cần mua; hoặc mở một mặt hàng hiện có để chỉnh sửa.
2. Hệ thống làm sạch dữ liệu đầu vào trước khi lưu và hiển thị.
3. Hệ thống thêm/cập nhật mục trong danh sách; thao tác xóa gỡ mục được chọn khỏi danh sách.

#### Alternative Flows
- **AF-53.2 — Chỉnh sửa hoặc xóa mục:** Member sửa số lượng/đơn vị hoặc xóa mặt hàng đã có trong danh sách.

#### Security Flows
- **SF-53.1 — Làm sạch đầu vào:** Tên mặt hàng thủ công được lọc mã độc XSS.

#### Postconditions
Mặt hàng được thêm, cập nhật hoặc gỡ khỏi danh sách của chính Member.

#### Traceability
- **Parent FR:** [FR-53](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-53).
- **Relevant BR:** [BR-32](../srs/BUSINESS-RULES.md#br-32) (danh sách thuộc thao tác Member đã đăng nhập).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (phân quyền); [NFR-10](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10) (xử lý XSS); [NFR-13](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13) (giao diện dùng trên thiết bị di động).

---

<a id="uc-53-3"></a>
### UC-53.3 — Đánh dấu hoặc bỏ đánh dấu mặt hàng đã mua

#### Goal
Cho phép Member theo dõi trạng thái mua hàng bằng checklist và lưu trạng thái đó bền vững.

#### Primary Actor
`Member`.

#### Trigger
Member chạm/click checkbox của một mặt hàng trong Shopping List.

#### Preconditions
Member đã đăng nhập và đang xem danh sách của mình.

#### Main Flow
1. Member đánh dấu một mục chưa mua; hệ thống chuyển mục sang `Đã mua`, hiển thị kiểu gạch ngang/màu xám và lưu trạng thái.
2. Nếu Member chạm lại mục đã mua, hệ thống chuyển trạng thái về `Chưa mua` và bỏ kiểu gạch ngang.

#### Alternative Flows
- **AF-53.1 — Bỏ đánh dấu đã mua:** Member chạm lại mục đã mua; hệ thống đưa mục về `Chưa mua`.
- **AF-53.3 — Xóa các mục đã mua:** Member chọn “Dọn dẹp danh sách” để gỡ nhanh các mặt hàng đã được đánh dấu mua.

#### Postconditions
Trạng thái checklist được lưu bền vững; nội dung Recipe Post nguồn không bị thay đổi.

#### Traceability
- **Parent FR:** [FR-53](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-53).
- **Relevant BR:** [BR-32](../srs/BUSINESS-RULES.md#br-32) (yêu cầu tài khoản Member).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (quyền sở hữu); [NFR-13](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13) (checklist tương tác/responsive).

---

<a id="fr-54"></a>
## FR-54 — Tự động tổng hợp nguyên liệu trùng an toàn trong Shopping List

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-54).

#### Shared system-triggered flow — Shopping List ingredient aggregation
Đây là cơ chế hệ thống được kích hoạt trong lúc tạo/cập nhật Shopping List theo FR-53; Member là người nhận kết quả, không thực hiện một thao tác tổng hợp riêng.

1. Hệ thống tiếp nhận các dòng nguyên liệu nguồn có định danh/nguyên liệu, số lượng và đơn vị.
2. Hệ thống chỉ gộp các dòng có cùng `ingredientId`; quy đổi an toàn `g/kg`, `ml/l`, cùng đơn vị đếm, hoặc khác chiều đo khi có quy tắc chính thức trong `INGREDIENT_UNIT_CONVERSION` cho nguyên liệu đó.
3. Nếu không có quy tắc quy đổi tương thích, hệ thống giữ các dòng riêng biệt, không tự suy diễn hệ số; các dòng sau xử lý được phân nhóm theo danh mục thực phẩm.
4. **AF-54.1 — Nguyên liệu tự do không có `ingredientId`:** Chỉ gộp khi tên hiển thị giống nhau không phân biệt hoa thường và cùng đơn vị; khác đơn vị thì giữ riêng.
5. **EF-54.1 — Định lượng không hợp lệ:** Dòng nguồn có số lượng không hợp lệ (`<= 0`) được bỏ qua và ghi nhận cảnh báo.
6. **SF-54.1 — Đảm bảo chính xác định lượng:** Phép tính dùng kiểu số có độ chính xác phù hợp để tránh sai số quy đổi.

#### Traceability — shared system behavior
- **Parent FR:** [FR-54](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-54).
- **Related FR:** [FR-53](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-53) (nguồn kích hoạt xử lý).
- **Relevant BR:** [BR-14](../srs/BUSINESS-RULES.md#br-14) (định lượng và gộp an toàn); [BR-73](../srs/BUSINESS-RULES.md#br-73) (chuẩn hóa và quy đổi đơn vị).
- **Relevant NFR:** [NFR-10](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10) (tính chính xác phép tính).

<a id="uc-54-1"></a>
### UC-54.1 — Xem nguyên liệu được tổng hợp theo đơn vị chuẩn

#### Goal
Giúp Member thấy tổng lượng nguyên liệu đã được gộp an toàn khi có thể quy đổi theo đơn vị chuẩn.

#### Primary Actor
`Member` (người nhận kết quả của xử lý hệ thống).

#### Trigger
Hệ thống xử lý các dòng nguyên liệu khi Shopping List được tạo/cập nhật theo FR-53.

#### Preconditions
Shopping List có các dòng nguyên liệu nguồn; quá trình tổng hợp dùng shared system-triggered flow ở FR-54.

#### Main Flow
1. Với các dòng cùng nguyên liệu và đơn vị/quy tắc quy đổi tương thích, hệ thống quy đổi và cộng dồn theo shared flow.
2. Hệ thống trả kết quả đã tổng hợp vào Shopping List để Member xem trong checklist.

#### Postconditions
Member thấy một dòng tổng hợp khi các dòng nguồn đáp ứng quy tắc gộp an toàn.

#### Traceability
- **Parent FR:** [FR-54](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-54).
- **Relevant BR:** [BR-14](../srs/BUSINESS-RULES.md#br-14); [BR-73](../srs/BUSINESS-RULES.md#br-73).
- **Relevant NFR:** [NFR-10](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10).

---

<a id="uc-54-2"></a>
### UC-54.2 — Xem riêng các dòng không có quy tắc quy đổi tương thích

#### Goal
Đảm bảo Member nhìn thấy riêng những lượng nguyên liệu không thể quy đổi an toàn thay vì nhận một tổng số suy diễn.

#### Primary Actor
`Member` (người nhận kết quả của xử lý hệ thống).

#### Trigger
Trong lúc hệ thống tổng hợp Shopping List, có các dòng cùng nguyên liệu nhưng đơn vị không tương thích và không có quy tắc chuyển đổi.

#### Preconditions
Shopping List có các dòng nguyên liệu cần tổng hợp; xử lý được kích hoạt từ luồng FR-53.

#### Main Flow
1. Hệ thống không cộng các dòng không có quy tắc quy đổi tương thích.
2. Hệ thống trả từng dòng riêng và phân nhóm theo danh mục để Member phân biệt rõ lượng cần mua.

#### Postconditions
Các dòng không tương thích vẫn tách riêng; hệ thống không tự gán tỷ lệ quy đổi.

#### Traceability
- **Parent FR:** [FR-54](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-54).
- **Relevant BR:** [BR-14](../srs/BUSINESS-RULES.md#br-14); [BR-73](../srs/BUSINESS-RULES.md#br-73).
- **Relevant NFR:** [NFR-10](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10).

---

<a id="fr-55"></a>
## FR-55 — Sao chép clipboard và xuất file text Shopping List

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-55).

<a id="uc-55-1"></a>
### UC-55.1 — Sao chép Shopping List vào Clipboard

#### Goal
Cho Member sao chép nhanh toàn bộ danh sách mua sắm thành nội dung checklist có cấu trúc.

#### Primary Actor
`Member` sở hữu Shopping List.

#### Trigger
Member nhấn “Sao chép vào Clipboard” trên thanh công cụ danh sách.

#### Preconditions
Member đã đăng nhập; danh sách của Member có ít nhất một mặt hàng.

#### Main Flow
1. Hệ thống định dạng danh sách thành văn bản UTF-8, phân nhóm theo danh mục; mỗi dòng có trạng thái `[ ]` hoặc `[x]`, tên nguyên liệu và định lượng tổng hợp.
2. Hệ thống ghi nội dung vào Clipboard và hiển thị thông báo sao chép thành công.

#### Exception Flows
- **EF-55.1 — Danh sách trống:** Nút sao chép bị vô hiệu hóa.
- **EF-55.2 — Clipboard bị trình duyệt chặn:** Hệ thống hiển thị văn bản và nút “Chọn tất cả” để Member tự sao chép.

#### Postconditions
Nội dung danh sách hiện tại có trong Clipboard; dữ liệu gốc không bị thay đổi.

#### Traceability
- **Parent FR:** [FR-55](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-55).
- **Relevant BR:** [BR-32](../srs/BUSINESS-RULES.md#br-32) (Shopping List cho Member đã đăng nhập).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (quyền sở hữu); [NFR-13](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13) (giao diện/tiếng Việt).

---

<a id="uc-55-2"></a>
### UC-55.2 — Xuất Shopping List thành tệp `.txt`

#### Goal
Cho Member tải danh sách mua sắm về thiết bị dưới dạng văn bản để lưu, gửi hoặc in.

#### Primary Actor
`Member` sở hữu Shopping List.

#### Trigger
Member nhấn “Xuất file .txt” trên thanh công cụ danh sách.

#### Preconditions
Member đã đăng nhập; danh sách có ít nhất một mặt hàng.

#### Main Flow
1. **AF-55.1 — Xuất file `.txt`:** Hệ thống kết xuất checklist UTF-8 theo danh mục, có ký hiệu `[ ]`/`[x]`, tên mặt hàng và định lượng.
2. Hệ thống tạo tệp `.txt` theo tên `Danh_sach_mua_sam_YYYYMMDD.txt` và tải xuống thiết bị Member.

#### Exception Flows
- **EF-55.1 — Danh sách trống:** Nút xuất tệp bị vô hiệu hóa.

#### Security Flows
- **SF-55.1 — Bảo vệ dữ liệu khi xuất:** Tệp chỉ chứa các mặt hàng mua sắm, không kèm dữ liệu nhạy cảm khác.

#### Postconditions
Member nhận tệp `.txt`; danh sách trong hệ thống không bị thay đổi và tệp chỉ chứa các mặt hàng mua sắm.

#### Traceability
- **Parent FR:** [FR-55](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-55).
- **Relevant BR:** [BR-32](../srs/BUSINESS-RULES.md#br-32) (Shopping List cho Member đã đăng nhập).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (quyền sở hữu); [NFR-13](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13) (hiển thị tiếng Việt).

---
