> **Document:** Use Case Specifications — M10
> **File:** `docs/requirements/use-cases/nutrition.md`
> **Version:** v2.0.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-26
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M10

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns the required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-35"></a>
## FR-35 — Khai báo hồ sơ dinh dưỡng và xem chỉ số tham khảo cá nhân

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản Member hợp lệ (FR-03).
  - Người dùng đã hoàn thành bước xác nhận phạm vi hỗ trợ và không thuộc đối tượng loại trừ (FR-38, BR-41, BR-42).
- **Kích hoạt (Trigger):**
  - Member truy cập màn hình "Hồ sơ dinh dưỡng cá nhân" hoặc chọn mục cập nhật chỉ số sức khỏe trong trang quản lý tài khoản.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member truy cập giao diện Hồ sơ dinh dưỡng; hệ thống hiển thị nổi bật Tuyên bố từ chối trách nhiệm y tế (Medical Safety Disclaimer theo BR-39, BR-41).
  - Bước 2: Member nhập các thông số: Năm sinh/Tuổi (phải $\ge 18$), Giới tính sinh học, Chiều cao (cm), Cân nặng (kg), Mức độ hoạt động thể chất (Ít vận động, Vận động nhẹ, Vận động vừa, Vận động nặng) và Mục tiêu dinh dưỡng chung (Duy trì cân nặng, Tăng cường sức khỏe, Hỗ trợ tập luyện).
  - Bước 3: Member nhấn "Lưu hồ sơ và Tính toán tham khảo".
  - Bước 4: Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào: Chiều cao từ 100 cm đến 250 cm, Cân nặng từ 30 kg đến 300 kg, Tuổi từ 18 đến 120.
  - Bước 5: Hệ thống tính toán chỉ số khối cơ thể: $\text{BMI} = \frac{\text{Cân nặng (kg)}}{(\text{Chiều cao (m)})^2}$, làm tròn 1 chữ số thập phân (BR-39).
  - Bước 6: Hệ thống xác định phân loại thể trạng tham khảo theo chuẩn WHO/USDA (Thiếu cân, Bình thường, Thừa cân, Béo phì) kèm văn bản cảnh báo rõ ràng rằng đây chỉ là chỉ số sàng lọc tham khảo, không đại diện cho tỷ lệ mỡ hay chẩn đoán sức khỏe cá nhân.
  - Bước 7: Hệ thống tính toán mức nhu cầu tham khảo hàng ngày cho 9 chỉ tiêu cốt lõi (Năng lượng, Đạm, Carb, Chất béo, Chất xơ, Canxi, Sắt, Vitamin B12, Kẽm) dựa trên công thức tham chiếu USDA/NIH và mức độ hoạt động.
  - Bước 8: Hệ thống lưu trữ hồ sơ dinh dưỡng của Member vào cơ sở dữ liệu và hiển thị bảng kết quả chỉ số tham khảo.
- **Luồng thay thế (Alternative Flows):**
  - *AF-35.1 (Cập nhật lại thông số):* Member có thể chỉnh sửa cân nặng hoặc mức độ vận động bất kỳ lúc nào. Hệ thống tự động tính toán lại BMI và 9 chỉ tiêu tham khảo tương ứng, cập nhật ngày sửa đổi gần nhất.
  - *AF-35.2 (Xem lại hồ sơ đã lưu):* Khi Member truy cập trang hồ sơ dinh dưỡng, nếu đã có dữ liệu trước đó, hệ thống tải dữ liệu đã lưu cùng ngày cập nhật và hiển thị đầy đủ thông số tham khảo.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-35.1 (Vi phạm điều kiện loại trừ y tế):* Nếu Member thay đổi thông tin xác nhận và khai báo thuộc nhóm đối tượng loại trừ (dưới 18 tuổi, đang mang thai, cho con bú hoặc mắc bệnh mạn tính theo BR-42), hệ thống lập tức khóa chức năng tính toán cá nhân hóa, hiển thị thông báo khuyến nghị tham vấn bác sĩ/chuyên gia dinh dưỡng và xóa/ẩn mục tiêu dinh dưỡng cá nhân.
  - *EF-35.2 (Thông số nhập không hợp lệ):* Nếu chiều cao hoặc cân nặng nằm ngoài ngưỡng cho phép, hệ thống hiển thị thông báo lỗi tại trường nhập liệu và không tiến hành tính toán.
  - *SF-35.1 (Bảo mật dữ liệu sức khỏe):* Dữ liệu hồ sơ dinh dưỡng và BMI là thông tin nhạy cảm; chỉ chính Member đó mới có quyền xem và sửa (RBAC theo NFR-08, NFR-09, NFR-20). Hệ thống không công khai các chỉ số này trên Author Card hay Profile công khai của Member.

#### 5. Hậu điều kiện (Postconditions)
- Hồ sơ dinh dưỡng của Member được cập nhật an toàn trong hệ thống.
- Bảng 9 chỉ tiêu dinh dưỡng tham khảo cá nhân sẵn sàng để làm mốc so sánh cho chức năng Lịch ăn ngày (FR-37) và gợi ý AI Menu (FR-36).

---

<a id="fr-36"></a>
## FR-36 — AI lập menu theo nhu cầu dinh dưỡng từ công thức tin cậy

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Member đã đăng nhập tài khoản hợp lệ (FR-03).
  - Member đã hoàn thành hồ sơ dinh dưỡng cá nhân (FR-35) và xác nhận phạm vi hỗ trợ (FR-38).
  - Member đã cấu hình tối thiểu trường phái ăn chay và dị ứng (FR-31).
  - Tài khoản Member có quyền dùng AI lập thực đơn theo gói Pro còn hiệu lực (FR-10, BR-02, BR-03).
- **Kích hoạt (Trigger):**
  - Member nhấn nút "Nhờ AI lập thực đơn dinh dưỡng" trên giao diện Lịch ăn tuần hoặc trang Dinh dưỡng cá nhân.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member chọn phạm vi lập menu (1 ngày hoặc 7 ngày trong tuần) và xác nhận các tiêu chí ưu tiên (giữ nguyên loại trừ dị ứng, ưu tiên trường phái ăn chay hiện tại).
  - Bước 2: Member nhấn "Tạo thực đơn bằng AI".
  - Bước 3: Backend kiểm tra quyền tính năng AI lập thực đơn theo gói Pro của tài khoản trước khi gọi Gemini (FR-10, BR-02, BR-03).
  - Bước 4: Hệ thống truy vấn kho công thức nội bộ để trích xuất tập hợp ứng viên (Candidate Pool) thỏa mãn các điều kiện ngặt nghèo:
    - Trạng thái công thức là đang công khai (`Public`).
    - Phù hợp với trường phái ăn chay và loại trừ hoàn toàn các thành phần dị ứng của Member (BR-30, BR-31).
    - Có 100% nguyên liệu đã được định lượng chuẩn hóa theo gram và ánh xạ đầy đủ 9 chỉ tiêu dinh dưỡng từ danh mục dinh dưỡng nội bộ (BR-40, BR-46). Các công thức chứa nguyên liệu chưa có dữ liệu dinh dưỡng bị loại bỏ hoàn toàn khỏi pool (BR-50).
  - Bước 5: Hệ thống gửi prompt kèm danh sách metadata của các công thức ứng viên (ID, tên món, 9 chỉ số dinh dưỡng/khẩu phần) và mục tiêu dinh dưỡng tham khảo của Member tới Google Gemini AI (NFR-04 $\le 8$ giây). Prompt bắt buộc yêu cầu AI chỉ được ghép nối các ID công thức có sẵn, tuyệt đối không tự tạo công thức mới (BR-38).
  - Bước 6: Google Gemini phản hồi cấu trúc menu được đề xuất phân bổ vào 3 bữa ăn cố định (Sáng, Trưa, Tối) kèm đoạn giải trình ngắn gọn lý do phân bổ.
  - Bước 7: Hệ thống xác thực kết quả AI hợp lệ; không trừ quota sử dụng theo lượt/ngày.
  - Bước 8: Hệ thống hiển thị bản xem trước (Preview) thực đơn được đề xuất kèm bảng tổng hợp 9 chỉ tiêu dinh dưỡng dự kiến và cảnh báo từ chối trách nhiệm y tế (BR-39, BR-41).
  - Bước 9: Member xem lại từng món, có thể loại bỏ hoặc giữ nguyên, sau đó nhấn "Lưu vào Lịch ăn".
  - Bước 10: Hệ thống ghi nhận các món được chọn vào các vị trí bữa tương ứng trong Lịch ăn tuần của Member (BR-35, BR-36, BR-37).
- **Luồng thay thế (Alternative Flows):**
  - *AF-36.1 (Kho công thức tin cậy không đủ đa dạng):* Nếu số lượng công thức đạt chuẩn dinh dưỡng tin cậy không đủ để lấp đầy 21 bữa ăn trong tuần mà không bị trùng lặp, hệ thống thông báo cho Member biết phạm vi kho công thức hiện tại và cho phép AI lặp lại công thức ở các ngày khác nhau hoặc đề xuất thực đơn cho số ngày ít hơn.
  - *AF-36.2 (Member hủy bỏ kết quả gợi ý):* Member có thể đóng cửa sổ xem trước mà không lưu vào Lịch ăn; hệ thống không tính quota theo lượt/ngày.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-36.1 (Không có quyền tính năng):* Nếu tài khoản không có gói Pro còn hiệu lực, Backend chặn trước khi gọi Gemini và hướng dẫn nâng cấp gói phù hợp (FR-10, BR-02, BR-03).
  - *EF-36.2 (Lỗi kết nối Gemini hoặc phản hồi quá 8 giây):* Nếu dịch vụ AI gặp sự cố hoặc timeout vượt quá 8 giây (NFR-04), hệ thống hủy yêu cầu và thông báo lỗi kỹ thuật thân thiện (BR-04); không có quota ngày để trừ.
  - *EF-36.3 (AI sinh công thức ngoài danh mục):* Nếu phản hồi của AI chứa ID không nằm trong Candidate Pool đã gửi, hệ thống tự động lọc bỏ các mục không hợp lệ trước khi hiển thị cho Member.

#### 5. Hậu điều kiện (Postconditions)
- Khi Member xác nhận lưu, các món ăn từ công thức công khai được thêm vào các bữa ăn trong Lịch ăn tuần của Member.
- Quyền tính năng theo gói không thay đổi sau yêu cầu; không áp dụng quota lượt/ngày.

---

<a id="fr-37"></a>
## FR-37 — Khai báo khẩu phần, kiểm tra dinh dưỡng menu ngày/tuần và xuất báo cáo PDF

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-37).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Member đã đăng nhập tài khoản hợp lệ (FR-03).
  - Khoảng thời gian được chọn trong Lịch ăn (ngày hoặc tuần) có ít nhất một món ăn được xếp vào bữa (Sáng, Trưa hoặc Tối) (FR-33).
- **Kích hoạt (Trigger):**
  - Member bấm nút "Kiểm tra dinh dưỡng ngày", "Phân tích tổng thể", hoặc "Xuất báo cáo PDF" trên giao diện Lịch ăn tuần / Theo dõi dinh dưỡng.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow — Kiểm tra dinh dưỡng ngày):**
  - Bước 1: Member mở xem chi tiết một ngày trong Lịch ăn tuần.
  - Bước 2: Tại mỗi món ăn đã xếp vào các bữa (Sáng, Trưa, Tối), hệ thống hiển thị số khẩu phần mặc định là 1. Member có thể điều chỉnh số khẩu phần (từ 0.5 đến 10.0, bước nhảy 0.5).
  - Bước 3: Member nhấn nút "Kiểm tra dinh dưỡng ngày".
  - Bước 4: Hệ thống duyệt qua tất cả các món ăn trong 3 bữa của ngày đó:
    - Với mỗi món, hệ thống lấy dữ liệu 9 chỉ tiêu trên 1 khẩu phần đã tính toán theo công thức chuẩn (FR-39).
    - Nhân giá trị dinh dưỡng của từng chỉ tiêu với số khẩu phần Member đã khai báo cho món đó (BR-47).
    - Nếu món có nguyên liệu chưa hỗ trợ tính dinh dưỡng, hệ thống ghi nhận cờ cảnh báo thiếu dữ liệu cho món đó (BR-48).
  - Bước 5: Hệ thống cộng dồn tổng giá trị của từng chỉ tiêu trong 9 chỉ tiêu cho toàn bộ các bữa ăn trong ngày:
    $$\text{Tổng ngày}(i) = \sum_{\text{món}} \left(\text{Chỉ tiêu}_i \text{ trên 1 khẩu phần} \times \text{Số khẩu phần}\right)$$
  - Bước 6: Nếu Member đã có hồ sơ dinh dưỡng tham khảo (FR-35), hệ thống so sánh Tổng ngày với Mức nhu cầu tham khảo cá nhân:
    - Tính chênh lệch định lượng ($\Delta = \text{Tổng ngày} - \text{Mức tham khảo}$).
    - Hiển thị thanh tiến trình trực quan biểu thị tỷ lệ % đạt được theo từng chỉ tiêu riêng biệt.
    - Tuyệt đối không tạo ra một "điểm số sức khỏe" (health score) tổng hợp hay gán nhãn "lành mạnh / không lành mạnh" đơn giản hóa (BR-45).
    - Hệ thống diễn giải từng chỉ tiêu theo mức tham khảo tương ứng, không dùng một tỷ lệ hoặc nhãn chung để thay thế ý nghĩa riêng của từng chỉ tiêu (BR-44).
  - Bước 7: Nếu có món ăn chứa nguyên liệu chưa có dữ liệu dinh dưỡng, hệ thống hiển thị thông báo cảnh báo màu vàng nổi bật: *"Một số món trong ngày chưa có dữ liệu dinh dưỡng đầy đủ; số liệu thực tế có thể cao hơn bảng tính toán"* kèm danh sách tên các món bị ảnh hưởng (BR-48).
  - Bước 8: Hệ thống hiển thị nhãn ghi chú minh bạch *"Dinh dưỡng dự kiến theo thực đơn"* và Tuyên bố từ chối trách nhiệm y tế (BR-39, BR-41).

- **Luồng phân tích tổng thể tuần (Sub-flow SBF-37.1 — Phân tích tổng thể tuần & So sánh 7 ngày):**
  - Bước 1: Member nhấn nút "So sánh & Thống kê 7 ngày" (hoặc "Phân tích tổng thể") tại màn hình Theo dõi dinh dưỡng.
  - Bước 2: Hệ thống truy xuất dữ liệu kế hoạch ăn của 7 ngày trong tuần được chọn (từ Thứ Hai đến Chủ Nhật).
  - Bước 3: Hệ thống kết xuất bảng ma trận so sánh trực quan đa chiều:
    - 7 cột tương ứng với 7 ngày trong tuần (Thứ Hai đến Chủ Nhật), hiển thị chi tiết số liệu của 9 chỉ tiêu cho từng ngày.
    - Cột "Trung bình/ngày" tính theo trung bình cộng 7 ngày:
      $$\text{Giá trị TB/ngày}(i) = \frac{\sum_{d=1}^{7} \text{Tổng ngày}_d(i)}{7}$$
    - Cột "Chuẩn tham khảo cá nhân (DRI)" và tỷ lệ % hoàn thành mục tiêu.
    - Thanh trạng thái / màu sắc trực quan (đạt / vượt / thiếu) cho từng ô chỉ tiêu theo ngày mà không tạo điểm số tổng hợp phán xét (BR-45).
  - Bước 4: Thống kê tỷ lệ ngày đạt chuẩn của từng chỉ tiêu (ví dụ: *6/7 ngày đạt nhu cầu Đạm*).
  - Bước 5: Phân tích các vi chất đặc thù ăn chay:
    - Phát hiện nguy cơ thiếu hụt tích lũy nếu chỉ tiêu quan trọng (như B12, Sắt non-heme, Canxi) thấp hơn ngưỡng khuyến nghị liên tiếp từ 3 ngày trở lên trong tuần.
    - Không phân tích Natri/muối vì chỉ tiêu này không thuộc bộ chín chỉ tiêu dinh dưỡng MVP.
  - Bước 6: Hiển thị kết quả phân tích trong giao diện chi tiết (Modal / Panel). Tuyệt đối không tạo ra một điểm số tổng hợp (Health Score) cho cả tuần (BR-45); hiển thị cảnh báo danh sách các ngày/món thiếu dữ liệu chuẩn (BR-48); hiển thị nhãn minh bạch "Dinh dưỡng dự kiến theo thực đơn tuần".

- **Luồng xuất báo cáo PDF (Sub-flow SBF-37.2 — Xuất báo cáo PDF tuần / ngày):**
  - Bước 1: Member nhấn nút "Xuất báo cáo PDF" (hỗ trợ nút xuất nhanh "Xuất PDF tuần này").
  - Bước 2: Hệ thống hiển thị hộp thoại chọn phạm vi xuất: "Báo cáo ngày hiện tại" hoặc "Báo cáo toàn bộ tuần này" (mặc định chọn Tuần hiện tại).
  - Bước 3: Hệ thống tổng hợp dữ liệu dinh dưỡng theo phạm vi được chọn (tính toán on-demand từ dữ liệu lịch ăn hiện hành, không truy vấn hay lưu trữ vào bảng báo cáo tĩnh).
  - Bước 4: Tạo tài liệu định dạng PDF chuẩn in A4: một trang đối với báo cáo ngày và hai trang đối với báo cáo tuần.
    - **Báo cáo ngày — 1 trang:** Logo/tiêu đề, thông tin Member và ngày được chọn, ba bữa cùng tên món/số khẩu phần dự kiến, bảng chín chỉ tiêu và mức tham khảo, cảnh báo thiếu dữ liệu, timestamp và tuyên bố miễn trừ y tế.
    - **Báo cáo tuần — 2 trang:**
      - **Trang 1 — Tổng quan & Bảng ma trận so sánh 7 ngày:**
        - Phần thông tin chung: Logo Mâm Xanh, Tiêu đề *"Báo Cáo Phân Tích Dinh Dưỡng Thực Đơn Tuần"*, Họ tên/Mã tài khoản Member, Thời điểm xuất dữ liệu (timestamp), Khoảng thời gian tuần (từ ngày... đến ngày...).
        - Thông tin thể trạng tham khảo (nếu có): Nhóm tuổi, giới tính, mức vận động, mức calorie mục tiêu (FR-35).
        - Bảng ma trận 9 chỉ tiêu x 7 ngày: hiển thị số liệu từng ngày (Thứ 2 đến Chủ nhật), cột trung bình tuần, cột chuẩn DRI cá nhân và tỷ lệ % đạt.
      - **Trang 2 — Chi tiết thực đơn, cảnh báo & miễn trừ y tế:**
        - Tóm tắt thực đơn tuần: Danh mục bữa ăn 3 bữa (Sáng, Trưa, Tối) từng ngày, tên món và số khẩu phần trong kỳ báo cáo.
        - Phân tích vi chất đặc thù & cảnh báo thiếu dữ liệu: Nhận diện vi chất đạt chuẩn, cảnh báo các món chứa nguyên liệu chưa có dữ liệu chuẩn (BR-48).
        - Tuyên bố miễn trừ y tế bắt buộc: In rõ ràng tuyên bố từ chối trách nhiệm y tế chuẩn theo BR-41.
  - Bước 5: Trình duyệt tải file PDF trực tiếp về thiết bị của Member.

- **Luồng thay thế (Alternative Flows):**
  - *AF-37.1 (Chưa khai báo hồ sơ dinh dưỡng cá nhân):* Nếu Member chưa khai báo hồ sơ dinh dưỡng theo FR-35, hệ thống vẫn hiển thị tổng 9 chỉ tiêu dinh dưỡng của thực đơn (theo ngày hoặc tuần), nhưng ở cột so sánh sẽ hiển thị giá trị khuyến nghị tiêu chuẩn của người trưởng thành theo USDA kèm liên kết gợi ý: *"Khai báo hồ sơ để xem chỉ số phù hợp với thể trạng cá nhân"*.
  - *AF-37.2 (Member nhờ AI phân tích chênh lệch):* Member bấm nút "Nhờ AI phân tích và đề xuất điều chỉnh". Hệ thống kiểm tra quyền dùng AI nâng cao của gói Pro (BR-02, BR-03). Nếu hợp lệ, hệ thống gửi bảng số liệu chênh lệch cho Gemini kèm yêu cầu đề xuất đổi một món trong bữa sang một công thức khác có sẵn trong hệ thống để cân bằng dinh dưỡng. AI trả về đề xuất; nếu Member đồng ý, hệ thống đổi món trong Lịch ăn.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-37.1 (Ngày hoặc tuần không có món ăn nào):* Nếu khoảng thời gian được chọn không có bất kỳ món ăn nào trong lịch ăn, các nút "Kiểm tra dinh dưỡng ngày", "So sánh & Thống kê 7 ngày" và "Xuất báo cáo PDF" bị vô hiệu hóa kèm thông báo *"Vui lòng thêm ít nhất một món ăn vào lịch ăn để phân tích/xuất báo cáo dinh dưỡng"*.
  - *EF-37.2 (Khẩu phần không hợp lệ):* Nếu người dùng nhập số khẩu phần nhỏ hơn 0.5 hoặc lớn hơn 10, hệ thống báo lỗi và khôi phục về giá trị hợp lệ gần nhất.
  - *EF-37.3 (Tạo PDF thất bại):* Nếu không thể tạo file PDF, hệ thống thông báo xuất báo cáo thất bại, không tạo file rỗng/hỏng và cho phép Member thử lại mà không thay đổi dữ liệu Lịch ăn.
  - *SF-37.1 (Kiểm soát quyền riêng tư):* Lịch ăn, kết quả kiểm tra dinh dưỡng và file PDF xuất ra thuộc quyền riêng tư của chính Member; tài khoản khác hoặc Guest không thể xem hoặc tải dữ liệu (RBAC theo NFR-08, NFR-09).

#### 5. Hậu điều kiện (Postconditions)
- Số khẩu phần đã điều chỉnh được lưu vào Lịch ăn ngày của Member.
- Kết quả kiểm tra 9 chỉ tiêu (ngày hoặc tuần) được hiển thị chi tiết, minh bạch đến từng chỉ tiêu thành phần.
- File báo cáo PDF được tạo on-demand và tải thành công về máy người dùng mà không tạo bản ghi dư thừa trong CSDL.

---

<a id="fr-38"></a>
## FR-38 — Xác nhận phạm vi hỗ trợ trước khi dùng chức năng dinh dưỡng

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-38).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member`: Người dùng đã đăng nhập có nhu cầu sử dụng các tính năng dinh dưỡng (thiết lập hồ sơ dinh dưỡng, kiểm tra menu ngày, nhận AI menu theo dinh dưỡng).
- **Secondary Actor / External System:**
  - `Cơ sở dữ liệu tham chiếu dinh dưỡng nội bộ`: Dữ liệu tham khảo dinh dưỡng được biên soạn chuẩn bị từ nguồn USDA FoodData Central và NIH DRI (không gọi runtime API bên ngoài theo BR-49).

#### 6. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:**
  - Người dùng đã đăng nhập với vai trò Member và tài khoản ở trạng thái `ACTIVE`.
- **Trigger:**
  - Member lần đầu tiên nhấp vào bất kỳ tính năng nào thuộc Module M10: menu "Hồ sơ dinh dưỡng" (FR-35), nút "Kiểm tra dinh dưỡng thực đơn ngày" (FR-37), hoặc yêu cầu "AI lập thực đơn theo nhu cầu dinh dưỡng" (FR-36).
  - Member chủ động truy cập mục Cài đặt dinh dưỡng để cập nhật lại điều kiện sức khỏe.

#### 7. Luồng sự kiện (Flow of Events)

##### A. Luồng Xác nhận đủ điều kiện sử dụng chức năng dinh dưỡng (UC-38.1)
1. **Main Flow (Xác nhận đủ điều kiện thành công):**
   - Bước 1: Member nhấp vào tính năng dinh dưỡng (ví dụ: "Hồ sơ dinh dưỡng").
   - Bước 2: Hệ thống kiểm tra trạng thái xác nhận dinh dưỡng của Member. Nếu chưa xác nhận, hệ thống yêu cầu hiển thị màn hình xác nhận.
   - Bước 3: Giao diện hiển thị màn hình "Xác nhận phạm vi hỗ trợ dinh dưỡng".
   - Bước 4: Màn hình trình bày rõ ràng:
     - Khối 1: Ba điều kiện bắt buộc (Từ đủ 18 tuổi; Không mang thai hoặc đang cho con bú; Không có bệnh lý yêu cầu chế độ ăn điều trị riêng) theo BR-42.
     - Khối 2: Tuyên bố ranh giới thông tin: Dữ liệu mang tính tham khảo lập kế hoạch theo chuẩn USDA/NIH, không phải chẩn đoán hay điều trị y khoa theo BR-41.
     - Khối 3: Hộp kiểm cam kết: *"Tôi xác nhận tôi từ đủ 18 tuổi trở lên, không mang thai/cho con bú, không có nhu cầu ăn kiêng điều trị bệnh, và hiểu rằng các khuyến nghị dinh dưỡng chỉ mang tính chất tham khảo."*
   - Bước 5: Member tích chọn hộp kiểm và nhấn nút "Xác nhận và tiếp tục".
   - Bước 6: Ứng dụng gửi yêu cầu xác nhận lên máy chủ hệ thống.
   - Bước 7: Hệ thống xác thực yêu cầu, lưu trạng thái xác nhận đủ điều kiện kèm mốc thời gian xác nhận vào hồ sơ tài khoản của Member.
   - Bước 8: Hệ thống phản hồi xác nhận thành công.
   - Bước 9: Giao diện chuyển hướng Member sang màn hình Khai báo Hồ sơ dinh dưỡng cá nhân (`FR-35`) để nhập các chỉ số tuổi, giới tính, chiều cao, cân nặng, mức vận động.
2. **Alternative Flow (Người dùng không thuộc đối tượng hỗ trợ hoặc từ chối cam kết):**
   - Bước 1: Tại màn hình xác nhận, Member tích chọn: *"Tôi không thuộc nhóm đối tượng trên"* (hoặc chọn "Hủy bỏ / Quay lại").
   - Bước 2: Hệ thống ghi nhận trạng thái chưa đủ điều kiện dinh dưỡng đối với tài khoản.
   - Bước 3: Hệ thống hiển thị thông báo giải thích rõ ràng và lịch sự:
     - *"Rất tiếc! Hệ thống Vegetarian Support Application hiện chỉ cung cấp tính toán dinh dưỡng mẫu cho người trưởng thành khỏe mạnh bình thường. Đối với người dưới 18 tuổi, phụ nữ mang thai/cho con bú hoặc người có bệnh lý nền, nhu cầu dinh dưỡng đòi hỏi phác đồ chuyên biệt từ bác sĩ chuyên khoa hoặc chuyên gia dinh dưỡng lâm sàng. Để đảm bảo an toàn tuyệt đối cho sức khỏe của bạn, hệ thống xin phép tạm dừng chức năng dinh dưỡng đối với tài khoản này."*
   - Bước 4: Hệ thống khóa quyền truy cập các chức năng dinh dưỡng (FR-35, FR-36, FR-37) đối với tài khoản.
   - Bước 5: Hệ thống điều hướng Member về trang Khám phá bài viết.
   - Bước 6: **Bảo toàn chức năng thông thường (BR-42):** Member VẪN SỬ DỤNG HOÀN TOÀN BÌNH THƯỜNG toàn bộ các chức năng khác của hệ thống: xem bài công thức, tìm kiếm, lọc theo nguyên liệu/loại ăn chay, lưu bài viết vào `Saved Recipes`, xếp lịch ăn 3 bữa thủ công trong `Meal Planner`, bình luận và hỏi đáp Chatbot AI kiến thức chay chung (FR-51); riêng Chuyên gia được tạo và công khai Recipe Post.

##### B. Luồng Xem lại tuyên bố miễn trừ y tế và phạm vi hỗ trợ (UC-38.2)
1. **Main Flow:**
   - Bước 1: Member truy cập mục Thông tin dinh dưỡng hoặc nhấp vào liên kết "Phạm vi hỗ trợ & Tuyên bố y tế" tại các trang dinh dưỡng.
   - Bước 2: Hệ thống hiển thị đầy đủ văn bản tuyên bố ranh giới thông tin tham khảo, cơ sở dữ liệu nguồn USDA/NIH, và 3 điều kiện áp dụng an toàn của hệ thống.

##### C. Luồng Cập nhật lại trạng thái điều kiện sức khỏe (UC-38.3)
1. **Main Flow:**
   - Bước 1: Member đã từng xác nhận đủ điều kiện trước đây, nay tình trạng sức khỏe thay đổi (ví dụ: đang mang thai hoặc phát hiện bệnh lý cần ăn kiêng).
   - Bước 2: Member vào mục "Cài đặt dinh dưỡng", chọn "Cập nhật điều kiện sức khỏe".
   - Bước 3: Member chuyển trạng thái sang không đủ điều kiện và xác nhận lưu.
   - Bước 4: Hệ thống ghi nhận trạng thái không đủ điều kiện dinh dưỡng đối với tài khoản.
   - Bước 5: Toàn bộ tính năng AI lập menu theo dinh dưỡng (FR-36) và kiểm tra 9 chỉ tiêu menu ngày (FR-37) lập tức dừng cung cấp cho tài khoản này; dữ liệu dinh dưỡng trước đó không bị xóa mất nhưng được đặt ở trạng thái không đánh giá; các chức năng không-dinh-dưỡng tiếp tục hoạt động bình thường.

##### D. Luồng An ninh — Kiểm soát truy cập chức năng dinh dưỡng ở tầng máy chủ
1. **Main Flow (Kiểm soát chặt chẽ phía máy chủ):**
   - Đối với mọi yêu cầu liên quan đến tính toán, phân tích hay lập kế hoạch dinh dưỡng, máy chủ bắt buộc kiểm tra cờ xác nhận đủ điều kiện của tài khoản.
   - Nếu cờ xác nhận chưa được thiết lập hoặc có giá trị không đủ điều kiện: máy chủ lập tức từ chối xử lý yêu cầu và phản hồi yêu cầu xác nhận điều kiện dinh dưỡng trước.

#### 8. Hậu điều kiện (Postconditions)
- Khi xác nhận đủ điều kiện thành công: Trạng thái xác nhận đủ điều kiện dinh dưỡng được lưu trữ gắn liền với tài khoản Member; mở khóa quyền truy cập vào màn hình khai báo Hồ sơ dinh dưỡng (`FR-35`).
- Khi từ chối hoặc không đủ điều kiện: Ghi nhận trạng thái chưa đủ điều kiện; khóa toàn bộ chức năng thuộc Module M10; toàn bộ các tính năng không-dinh-dưỡng (M01–M06, M08–M09) vẫn hoạt động nguyên vẹn.

---

<a id="fr-39"></a>
## FR-39 — Tính toán ước tính 9 chỉ tiêu dinh dưỡng cho công thức

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-39).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Bài công thức đã khai báo số khẩu phần (servings $\ge 1$) và có ít nhất một nguyên liệu hợp lệ (FR-16, FR-19).
- **Kích hoạt (Trigger):**
  - Tác giả nhấn lưu bài công thức, hoặc người dùng truy cập trang xem chi tiết Recipe Post (FR-18).

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Hệ thống đọc danh sách nguyên liệu của bài công thức kèm số lượng số dương và đơn vị đo chuẩn thuộc bảng `UNIT` (FR-19, BR-73).
  - Bước 2: Với mỗi nguyên liệu, hệ thống quy đổi định lượng sang đơn vị gram chuẩn:
    - Nếu đơn vị thuộc chiều `MASS`: $1\text{ g} = 1\text{ g}$; $1\text{ kg} = 1.000\text{ g}$.
    - Nếu đơn vị thuộc chiều `VOLUME` (ml, l) hoặc `COUNT` (quả, củ, bìa...): hệ thống sử dụng hệ số quy đổi ($conversion\_factor$) từ bảng `INGREDIENT_UNIT_CONVERSION` đối với đúng `ingredientId` và `unitId` đó:
      $$\text{Trọng lượng (g)} = \text{Số lượng} \times conversion\_factor$$
    - (Lưu ý: Nếu một nguyên liệu thiếu quy tắc chuyển đổi cần thiết, việc xuất bản đã bị chặn từ bước thẩm định FR-25, BR-14, BR-73).
  - Bước 3: Hệ thống truy vấn danh mục nguyên liệu dinh dưỡng nội bộ (`Nutrition Profile`) để ánh xạ từng nguyên liệu với bản ghi dinh dưỡng tương ứng (BR-46).
  - Bước 4: Với mỗi nguyên liệu đã được ánh xạ thành công, hệ thống tính toán giá trị của từng chỉ tiêu trong 9 chỉ tiêu theo trọng lượng gram thực tế đã quy đổi:
    $$\text{Giá trị chỉ tiêu}_i = \frac{\text{Trọng lượng (g)}}{100} \times \text{Chỉ số chuẩn trên 100g}_i$$
  - Bước 5: Hệ thống cộng tổng giá trị của từng chỉ tiêu cho tất cả các nguyên liệu đã ánh xạ thành công để ra Tổng dinh dưỡng toàn bài công thức.
  - Bước 6: Hệ thống chia tổng dinh dưỡng của toàn bộ công thức cho số khẩu phần (servings) đã khai báo để tính ra Dinh dưỡng ước tính trên 1 khẩu phần (BR-43):
    $$\text{Dinh dưỡng trên 1 khẩu phần}_i = \frac{\text{Tổng dinh dưỡng}_i}{\text{Số khẩu phần}}$$
  - Bước 7: Nếu 100% nguyên liệu đều có dữ liệu dinh dưỡng đầy đủ, hệ thống đánh dấu trạng thái dinh dưỡng của công thức là `Đầy đủ` (Eligible cho pool AI menu theo BR-40).
  - Bước 8: Hệ thống hiển thị bảng dinh dưỡng 9 chỉ tiêu trên trang chi tiết công thức kèm Tuyên bố từ chối trách nhiệm y tế (BR-39, BR-41).
- **Luồng thay thế (Alternative Flows):**
  - *AF-39.1 (Công thức chứa nguyên liệu chưa có dữ liệu dinh dưỡng):* Nếu có một hoặc nhiều nguyên liệu chưa được ánh xạ trong danh mục nội bộ:
    - Hệ thống vẫn tính tổng của các nguyên liệu đã có số liệu.
    - Hệ thống đánh dấu trạng thái dinh dưỡng của công thức là `Chưa đầy đủ` (Incomplete).
    - Trên bảng dinh dưỡng, hệ thống hiển thị nhãn cảnh báo màu vàng: *"Ước tính chưa đầy đủ: công thức có X nguyên liệu chưa hỗ trợ tính dinh dưỡng"* kèm danh sách tên các nguyên liệu đó (BR-48).
    - Hệ thống tuyệt đối không tự gán số liệu dinh dưỡng của nguyên liệu thiếu bằng 0 trong các phép so sánh nghiêm ngặt và không cho AI tự bịa số liệu (BR-48).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-39.1 (Không có nguyên liệu nào có dữ liệu dinh dưỡng):* Nếu toàn bộ nguyên liệu trong bài đều nằm ngoài danh mục nội bộ, hệ thống hiển thị thông báo: *"Công thức chưa hỗ trợ tính toán dinh dưỡng do chưa có dữ liệu nguyên liệu chuẩn"* thay vì hiển thị toàn bộ số 0.
  - *SF-39.1 (Cấm gọi API ngoài realtime):* Hệ thống xử lý tính toán 100% dựa trên cơ sở dữ liệu nội bộ đã được Administrator phê duyệt (BR-49, BR-51); không thực hiện cuộc gọi API ra ngoài mạng Internet trong lúc người dùng xem bài viết nhằm bảo vệ hiệu năng và tính ổn định (NFR-02, NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Bảng ước tính 9 chỉ tiêu dinh dưỡng của công thức được lưu kèm hoặc tính toán sẵn sàng hiển thị.
- Trạng thái dinh dưỡng (`Đầy đủ` hoặc `Chưa đầy đủ`) được xác định chính xác để phục vụ bộ lọc tìm kiếm (FR-08) và pool AI lập menu (FR-36).

---

<a id="fr-40"></a>
## FR-40 — Công khai Recipe Post chứa nguyên liệu ngoài danh mục dinh dưỡng

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-40).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Tác giả là Chuyên gia đã đăng nhập và hoàn thành các trường thông tin bắt buộc của bài công thức (FR-04, FR-16, FR-19).
  - Bài công thức có ít nhất một nguyên liệu mà tác giả nhập dạng chữ tự do chưa khớp với danh mục dinh dưỡng nội bộ.
- **Kích hoạt (Trigger):**
  - Tác giả nhấn nút "Đăng công thức" (Publish Recipe).

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả hoàn thành soạn thảo công thức; trong danh sách nguyên liệu có một số nguyên liệu tự do không tìm thấy trong danh mục dinh dưỡng nội bộ.
  - Bước 2: Tác giả nhấn "Đăng công thức".
  - Bước 3: Hệ thống thực hiện quy trình kiểm tra hợp lệ thông tin bài viết theo bộ quy tắc Recipe Validation Profile chuẩn của FR-16 (tiêu đề, khẩu phần, thời gian chuẩn bị và nấu, loại ăn chay; nguyên liệu theo FR-19; nội dung hướng dẫn chuẩn bị/chế biến từ 10 đến 5.000 ký tự theo FR-16, BR-19).
  - Bước 4: Kiểm tra hợp lệ thành công; hệ thống xác định có nguyên liệu chưa ánh xạ được với bảng dinh dưỡng nội bộ.
  - Bước 5: Hệ thống cho phép công khai trực tiếp bài viết lên nền tảng ngay lập tức mà không chặn và không đưa vào hàng đợi duyệt trước (BR-07, BR-50, BR-59).
  - Bước 6: Hệ thống đánh dấu trạng thái dinh dưỡng của bài viết là `Chưa đầy đủ` (Incomplete Nutrition Data) và đánh dấu cờ `Loại trừ khỏi AI Menu Dinh dưỡng` (BR-40).
  - Bước 7: Trên trang chi tiết công thức công khai, bên cạnh tên của nguyên liệu tự do đó, hệ thống hiển thị biểu tượng ghi chú nhỏ: *"Chưa hỗ trợ tính dinh dưỡng"* (BR-48).
  - Bước 8: Bảng ước tính dinh dưỡng của bài viết hiển thị giá trị cộng dồn của các nguyên liệu đã biết kèm dòng ghi chú cảnh báo minh bạch (FR-39).
- **Luồng thay thế (Alternative Flows):**
  - *AF-40.1 (Sau này Admin bổ sung nguyên liệu vào danh mục):* Khi Administrator bổ sung nguyên liệu tương ứng vào danh mục nội bộ (FR-41) và liên kết với nguyên liệu của bài viết, hệ thống tự động tính toán lại dinh dưỡng của bài; nếu 100% nguyên liệu đã được hỗ trợ, cờ cảnh báo được gỡ bỏ và bài viết tự động đủ điều kiện tham gia pool AI menu.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-40.1 (Tác giả bỏ trống tên hoặc định lượng nguyên liệu):* Nếu nguyên liệu không có tên hoặc không có định lượng (số lượng + đơn vị đo chuẩn hoặc "vừa đủ"), hệ thống chặn đăng theo quy tắc validation bắt buộc (FR-16, FR-19), không liên quan đến việc nguyên liệu có trong danh mục dinh dưỡng hay không.
  - *SF-40.1 (Ngăn chặn AI đưa bài thiếu số liệu vào menu dinh dưỡng):* Khi module AI Menu (FR-36) truy vấn danh sách công thức để lập thực đơn theo calo/macro, truy vấn cơ sở dữ liệu bắt buộc lọc bỏ các bài viết có cờ `Chưa đầy đủ dinh dưỡng` để đảm bảo an toàn cho người dùng (BR-40).

#### 5. Hậu điều kiện (Postconditions)
- Recipe Post được công khai ngay lập tức cho toàn bộ cộng đồng xem và tương tác.
- Nguyên liệu ngoài danh mục được hiển thị minh bạch trạng thái.
- Bài viết bị loại trừ khỏi pool gợi ý AI Menu theo mục tiêu dinh dưỡng.

---

<a id="fr-41"></a>
## FR-41 — Administrator quản lý danh mục nguyên liệu dinh dưỡng nội bộ

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-41).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập với tài khoản có vai trò Administrator (FR-03, NFR-09).
- **Kích hoạt (Trigger):**
  - Administrator truy cập phân hệ "Quản lý dinh dưỡng" -> "Danh mục nguyên liệu dinh dưỡng" trên thanh điều hướng quản trị.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Administrator truy cập danh sách nguyên liệu dinh dưỡng; hệ thống hiển thị danh sách dạng bảng phân trang gồm: Tên nguyên liệu, Năng lượng, Đạm, Carb, Chất béo, Nguồn tham chiếu (USDA/NIH), Ngày cập nhật, Trạng thái (Đang hỗ trợ / Đã ngừng hỗ trợ), và Số lượng công thức đang sử dụng.
  - Bước 2: Administrator nhấn nút "Thêm nguyên liệu mới".
  - Bước 3: Hệ thống hiển thị biểu mẫu yêu cầu nhập liệu:
    - Tên nguyên liệu tiếng Việt (chuẩn hóa, không trùng lặp) và tên tiếng Anh (tùy chọn).
    - Nhóm thực phẩm (Rau củ, Đậu & Chế phẩm, Ngũ cốc, Các loại hạt, Trái cây, Gia vị chay).
    - Giá trị của đầy đủ 9 chỉ tiêu cốt lõi trên 100g (Năng lượng kcal $\ge 0$, Protein g $\ge 0$, Carb g $\ge 0$, Fat g $\ge 0$, Fiber g $\ge 0$, Calcium mg $\ge 0$, Iron mg $\ge 0$, Vitamin B12 mcg $\ge 0$, Zinc mg $\ge 0$).
    - Nguồn dữ liệu tham chiếu (bắt buộc chọn USDA FoodData Central hoặc NIH/Viện Dinh Dưỡng).
    - Mã định danh hoặc URL tham chiếu nguồn (bắt buộc).
    - Ngày đối chiếu dữ liệu.
  - Bước 4: Administrator nhập đầy đủ dữ liệu và nhấn "Lưu nguyên liệu".
  - Bước 5: Hệ thống kiểm tra hợp lệ: Bắt buộc không được để trống bất kỳ chỉ tiêu nào trong 9 chỉ tiêu; không chấp nhận giá trị âm; tên không được trùng với nguyên liệu đang hoạt động; nguồn tham chiếu phải rõ ràng (BR-52).
  - Bước 6: Hệ thống lưu bản ghi mới vào cơ sở dữ liệu với trạng thái `Đang hỗ trợ` (Active), ghi nhận ID của Administrator tạo và thời gian tạo.
  - Bước 7: Hệ thống cập nhật lại danh sách và thông báo thêm mới thành công.
- **Luồng thay thế (Alternative Flows):**
  - *AF-41.1 (Chỉnh sửa nguyên liệu hiện có):* Administrator chọn một nguyên liệu và chỉnh sửa số liệu. Sau khi lưu hợp lệ, hệ thống cập nhật bản ghi và đánh dấu các công thức đang liên kết để tự động cập nhật lại dinh dưỡng trong chu kỳ tính toán tiếp theo.
  - *AF-41.2 (Ngừng hỗ trợ nguyên liệu):* Administrator chọn ngừng hỗ trợ (Deactivate) một nguyên liệu. Hệ thống chuyển trạng thái nguyên liệu sang `Ngừng hỗ trợ`. Các công thức cũ đã sử dụng nguyên liệu này vẫn giữ nguyên tham chiếu lịch sử, nhưng nguyên liệu sẽ không xuất hiện trong gợi ý chọn nguyên liệu mới cho người dùng (BR-53).
  - *AF-41.3 (Kích hoạt lại nguyên liệu):* Administrator có thể bật lại trạng thái `Đang hỗ trợ` cho một nguyên liệu đã bị ngừng hỗ trợ trước đó.
  - *AF-41.4 (Xem công thức đang liên kết):* Administrator nhấn vào số lượng công thức đang sử dụng để xem danh sách chi tiết các Recipe Post đang tham chiếu đến nguyên liệu này.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-41.1 (Cố gắng xóa vĩnh viễn nguyên liệu đã tham chiếu):* Nếu Administrator cố gắng thực hiện hành động xóa vĩnh viễn (hard delete) một nguyên liệu đã có ít nhất một bài công thức tham chiếu, hệ thống từ chối hành động, ngăn chặn xóa và hiển thị thông báo: *"Không thể xóa vĩnh viễn nguyên liệu đã được tham chiếu trong công thức. Vui lòng sử dụng tính năng Ngừng hỗ trợ"* (BR-53).
  - *EF-41.2 (Thiếu bất kỳ chỉ tiêu nào trong 9 chỉ tiêu):* Nếu biểu mẫu thiếu dù chỉ một giá trị chỉ tiêu dinh dưỡng hoặc thiếu nguồn trích dẫn, hệ thống chặn lưu và yêu cầu nhập đủ dữ liệu (BR-52).
  - *SF-41.1 (Chặn truy cập trái phép):* Người dùng không có quyền Administrator khi cố gắng gọi thao tác CRUD danh mục dinh dưỡng sẽ bị hệ thống từ chối với mã lỗi 403 Forbidden (NFR-09).
  - *SF-41.2 (Cấm tự động nhập hàng loạt và AI can thiệp):* Hệ thống không cung cấp API import tự động không qua kiểm duyệt và tuyệt đối không cấp quyền cho AI tự động sửa đổi hoặc chèn số liệu vào danh mục dinh dưỡng (BR-51, BR-54).

#### 5. Hậu điều kiện (Postconditions)
- Bản ghi nguyên liệu dinh dưỡng được thêm mới, cập nhật hoặc chuyển trạng thái an toàn trong danh mục nội bộ.
- Mọi thay đổi dữ liệu dinh dưỡng được lưu kèm vết kiểm toán (Audit Trail) gồm Administrator thực hiện và dấu thời gian.

---
