> **Document:** Use Case Specifications — M06
> **File:** `docs/requirements/use-cases/ai-and-personalization.md`
> **Version:** v2.0.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-26
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M06

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns the required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-02"></a>
## FR-02 — Quyền Guest trải nghiệm AI Chatbot chung có giới hạn tần suất kỹ thuật

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-02).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Guest`: Người dùng chưa xác thực trải nghiệm tương tác với AI Chatbot chung (FR-51).
- **Secondary Actor / External System:**
  - `Google Gemini AI`: Dịch vụ trí tuệ nhân tạo xử lý và phản hồi câu hỏi thông qua FR-51.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Thiết bị của Guest có thể kết nối mạng và hỗ trợ cookie trình duyệt; tần suất gửi yêu cầu của Guest không vượt quá ngưỡng rate limit kỹ thuật (BR-01).
- **Trigger:** Guest mở cửa sổ AI Chatbot chung hoặc nhấn nút "Hỏi AI về công thức này" khi đang xem bài công thức công khai (FR-20) và gửi câu hỏi.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Dùng thử Chatbot AI cho Guest (UC-02.1)
1. **Main Flow:**
   - Bước 1: Guest mở hộp thoại AI Chatbot FR-51 trên trang web (ở ngữ cảnh chung hoặc ngữ cảnh Recipe Post công khai đang xem).
   - Bước 2: Hệ thống kiểm tra anonymous cookie của trình duyệt (nếu chưa có, máy chủ tạo cookie định danh ẩn danh mới) kết hợp kiểm tra địa chỉ IP để áp dụng rate limiting kỹ thuật (BR-01).
   - Bước 3: Hệ thống xác nhận tần suất gửi yêu cầu của Guest nằm trong ngưỡng an toàn (ví dụ: $< 10$ request / phút).
   - Bước 4: Guest gửi câu hỏi. Hệ thống kiểm soát tính hợp lệ của đầu vào và chuyển yêu cầu tới quy trình xử lý hội thoại của FR-51.
   - Bước 5: Dịch vụ AI phản hồi kết quả hợp lệ đáp ứng thời gian quy định tại NFR-03.
   - Bước 6: Hệ thống hiển thị câu trả lời cho Guest kèm lời gợi ý đăng ký tài khoản Member để sử dụng đầy đủ các tính năng lập thực đơn và lưu trữ yêu thích.
2. **Alternative Flow (Vượt ngưỡng Rate Limit kỹ thuật):**
   - Khi Guest gửi quá nhiều yêu cầu trong thời gian ngắn (vượt ngưỡng rate limit kỹ thuật): hệ thống tạm khóa tiếp nhận request từ IP/cookie đó, trả về mã HTTP 429 và thông báo thân thiện: *"Bạn đang thao tác quá nhanh. Vui lòng thử lại sau giây lát hoặc đăng ký tài khoản để có trải nghiệm tốt hơn!"*.
3. **Exception Flow (Gián đoạn dịch vụ AI bên ngoài hoặc timeout):**
   - Nếu dịch vụ AI gặp sự cố kỹ thuật hoặc quá thời gian phản hồi theo quy định -> Hệ thống hiển thị thông báo lỗi thân thiện và ghi log kỹ thuật (BR-04, NFR-18).

#### 6. Hậu điều kiện (Postconditions)
- Guest nhận được câu trả lời từ AI hoặc thông báo lỗi/giới hạn tần suất thích hợp.
- Dữ liệu đo lường kỹ thuật (telemetry token) được ghi nhận để đối soát chi phí (FR-11).

---

<a id="fr-10"></a>
## FR-10 — Phân quyền tính năng AI theo gói tài khoản (Feature-based AI Entitlement)

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-10).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng gửi yêu cầu sử dụng một tính năng AI cụ thể (FR-21, FR-34, FR-36, FR-47, FR-51).
- **Kích hoạt (Trigger):**
  - Hệ thống tiếp nhận yêu cầu gọi tính năng AI tại Backend Controller/Gateway trước khi kích hoạt logic xử lý nghiệp vụ AI.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Người dùng thao tác gọi một tính năng AI trên giao diện.
  - Bước 2: Backend xác định danh tính và hạng gói Subscription hiện tại của người dùng:
    - Nếu là Guest: Chỉ cho phép gọi AI Chatbot FR-51 (kiểm tra Rate Limiting kỹ thuật theo BR-01).
    - Nếu là Member: Xác định hạng gói (`FREE`, `PLUS`, `PRO`) từ thông tin Subscription có hiệu lực.
  - Bước 3: Backend đối chiếu tính năng được gọi với ma trận phân quyền (Feature Entitlement):
    - Yêu cầu AI Chatbot (FR-51) hoặc Gợi ý món cơ bản (FR-34): Cho phép với mọi Member (kể cả Free) và Guest (với FR-51).
    - Yêu cầu AI Soạn bài (FR-21) hoặc Gợi ý biến tấu (FR-47): Yêu cầu tối thiểu gói `PLUS` hoặc `PRO`.
    - Yêu cầu AI Lập thực đơn tuần 7 ngày (FR-36): Bắt buộc gói `PRO`.
  - Bước 4: Nếu tài khoản đủ quyền lợi tính năng, hệ thống chuẩn bị ngữ cảnh an toàn và chuyển tiếp yêu cầu tới dịch vụ Google Gemini AI.
  - Bước 5: Sau khi Gemini phản hồi hợp lệ thành công, hệ thống ghi nhận telemetry đo lường kỹ thuật để đối soát chi phí (FR-11) và trả kết quả cho người dùng.
- **Luồng thay thế (Alternative Flows):**
  - *AF-10.1 (Yêu cầu tính năng vượt quá hạng gói):*
    - Khi tài khoản Free cố gắng gọi tính năng của gói Plus/Pro, hoặc tài khoản Plus cố gắng gọi tính năng của gói Pro:
    - Hệ thống chặn yêu cầu ngay tại Backend, trả về mã lỗi HTTP 403 Forbidden kèm thông báo: *"Tính năng này chỉ dành cho thành viên gói [Plus/Pro]. Vui lòng nâng cấp gói để trải nghiệm!"* kèm đường dẫn điều hướng tới trang Đăng ký gói dịch vụ (FR-13).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-10.1 (Lỗi kết nối Gemini hoặc timeout):* Nếu yêu cầu gọi AI gặp lỗi từ nhà cung cấp hoặc quá thời gian chờ, hệ thống trả về thông báo lỗi thân thiện và ghi error log kỹ thuật (BR-04, NFR-18).
  - *SF-10.1 (Bảo mật kiểm tra quyền tại Backend):* Toàn bộ quy trình xác thực quyền tính năng bắt buộc phải thực thi tại Backend API (NFR-10); không cho phép client vượt qua kiểm tra bằng cách chỉnh sửa giao diện người dùng.

#### 5. Hậu điều kiện (Postconditions)
- Yêu cầu AI được thực thi chính xác nếu đủ quyền tính năng, hoặc bị từ chối an toàn nếu không đủ quyền.
- Không phát sinh chi phí gọi Gemini API đối với các yêu cầu không đủ thẩm quyền.

---

<a id="fr-11"></a>
## FR-11 — Ghi nhận dữ liệu đo lường kỹ thuật (Telemetry) sử dụng AI

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-11).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Một request gọi Google Gemini API hoàn tất thành công và trả về mã trạng thái HTTP 200 kèm payload kết quả hợp lệ.
- **Kích hoạt (Trigger):**
  - Module AI client nhận được phản hồi thành công từ Google Gemini API, hoặc Administrator truy cập trang thống kê tiêu thụ token.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng nghiệp vụ Actor — Administrator xem thống kê tiêu thụ (UC-11.2):**
  - Bước 1: Administrator truy cập trang Thống kê kỹ thuật trên giao diện quản trị (M06, M09).
  - Bước 2: Hệ thống truy vấn dữ liệu telemetry tổng hợp, hiển thị biểu đồ và bảng dữ liệu tổng lượng token tiêu thụ theo ngày, tuần, tháng và phân bổ theo từng tính năng (Chatbot, Lập thực đơn, Gợi ý công thức).
  - Bước 3: Administrator xem xét số liệu và chi phí ước tính phục vụ lập kế hoạch vận hành.
- **Hành vi hệ thống — Ghi nhận siêu dữ liệu telemetry khi gọi AI thành công:**
  - Bước 1: Google Gemini API trả về phản hồi hợp lệ cho backend.
  - Bước 2: Hệ thống trích xuất siêu dữ liệu sử dụng token (`usageMetadata`) từ đối tượng phản hồi của Gemini, gồm: `promptTokenCount`, `candidatesTokenCount`, và `totalTokenCount`.
  - Bước 3: Hệ thống chuẩn bị bản ghi đo lường (Telemetry Record) gồm: Mã tài khoản người dùng (hoặc Anonymous Session ID đối với Guest), Loại tính năng được gọi (Menu, Chatbot, Authoring), Dấu thời gian (Timestamp UTC), và Số lượng token tiêu thụ.
  - Bước 4: Hệ thống TUYỆT ĐỐI LOẠI BỎ toàn bộ nội dung văn bản câu hỏi thô (raw prompt content) và câu trả lời thô khỏi bản ghi telemetry (BR-04).
  - Bước 5: Hệ thống ghi bản ghi đo lường vào bảng nhật ký kỹ thuật trong cơ sở dữ liệu.
  - Bước 6: Trả kết quả nghiệp vụ về cho người dùng bình thường.
- **Luồng thay thế (Alternative Flows):**
  - *AF-11.1 (Gemini không trả về trường usageMetadata):* Nếu phản hồi từ Gemini thành công nhưng thiếu khối thông tin token, hệ thống vẫn ghi nhận bản ghi với số token = 0 và đánh dấu cờ kiểm toán kỹ thuật để không làm gián đoạn trải nghiệm người dùng.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-11.1 (Gọi AI thất bại hoặc timeout):* Nếu request tới Gemini bị lỗi mạng, timeout hoặc trả về mã lỗi 4xx/5xx, hệ thống không ghi nhận bản ghi đo lường thành công mà chỉ ghi nhật ký lỗi (Error Log) riêng biệt để phục vụ khắc phục sự cố (BR-04).
  - *SF-11.1 (Bảo mật quyền riêng tư - Không lưu Prompt):* Các quy tắc kiểm tra tự động và mã nguồn bảo đảm trường nội dung prompt không bao giờ được đưa vào bảng telemetry, ngăn chặn rò rỉ dữ liệu cá nhân nhạy cảm (NFR-08, NFR-20).
  - *SF-11.2 (Chính sách thanh lọc dữ liệu định kỳ 90 ngày - Retention & Cleanup Behavior):* Một tiến trình ngầm (Background Job) chạy định kỳ hàng tuần tự động xóa các bản ghi telemetry có dấu thời gian cũ hơn 90 ngày (BR-04).

#### 5. Hậu điều kiện (Postconditions)
- Siêu dữ liệu đo lường token được lưu trữ an toàn, phục vụ đối soát.
- Không có bất kỳ nội dung văn bản câu hỏi thô nào của người dùng bị lưu giữ trong bảng đo lường kỹ thuật.

---

<a id="fr-51"></a>
## FR-51 — AI Chatbot hỗ trợ hỏi đáp ẩm thực chay theo ngữ cảnh

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-51).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Guest: Thiết bị hỗ trợ cookie; tuân thủ giới hạn tần suất kỹ thuật (FR-02, BR-01).
  - Member: Đã đăng nhập tài khoản Member hợp lệ ở trạng thái hoạt động (FR-03, FR-10, BR-01); đã xác nhận phạm vi hỗ trợ dinh dưỡng trước khi hỏi về chỉ số cá nhân (FR-38, BR-41).
  - Đối với ngữ cảnh bài công thức: Bài công thức đang ở trạng thái công khai (`PUBLISHED`) mà người dùng có quyền xem (FR-20).
- **Kích hoạt (Trigger):**
  - Người dùng mở khung Chatbot AI từ thanh công cụ/menu chính; HOẶC
  - Người dùng nhấn nút "Hỏi AI về công thức này" trên trang chi tiết công thức (Recipe Detail View).

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Người dùng kích hoạt AI Chatbot (từ giao diện chung hoặc từ nút "Hỏi AI về công thức này" tại Recipe Detail View). Giao diện hiển thị rõ thông báo từ chối trách nhiệm y tế chuẩn: *"Trợ lý AI cung cấp thông tin tham khảo tổng quát, không đưa ra chẩn đoán hay thay thế tư vấn y khoa"* (BR-09, BR-41). Nếu mở từ bài công thức, giao diện hiển thị huy hiệu gắn ngữ cảnh công thức kèm tiêu đề món.
  - Bước 2: Người dùng nhập câu hỏi và nhấn gửi.
  - Bước 3: Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào và kiểm tra quyền truy cập hoặc giới hạn tần suất kỹ thuật (Guest theo FR-02/BR-01, Member theo FR-10/BR-01/BR-02).
  - Bước 4: Hệ thống chuẩn bị dữ liệu ngữ cảnh an toàn cho cuộc gọi AI:
    - Thiết lập các chỉ dẫn an toàn bắt buộc: tuân thủ đúng `dietaryType` của context/người dùng và các giới hạn `allergiesAndRestrictions`; không gợi ý nguyên liệu vi phạm; từ chối chẩn đoán/kê đơn y khoa (BR-09, BR-41); cấm tự bịa đặt số liệu dinh dưỡng (NFR-25); nhấn mạnh tính tham khảo của BMI/calorie (BR-39); yêu cầu phân biệt nội dung AI với dữ liệu gốc của tác giả; cấm tự ý sửa đổi dữ liệu hệ thống.
    - Đóng gói dữ liệu ngữ cảnh:
      - Nếu ở ngữ cảnh Recipe: truyền tiêu đề, thể loại món (`dish_category`), loại ăn chay, khẩu phần, thời gian, danh sách nguyên liệu & định lượng (FR-19), hướng dẫn thực hiện chi tiết (`instructions` theo FR-16), và dữ liệu dinh dưỡng sẵn có của bài công thức hiện tại (FR-39).
      - Nếu ở ngữ cảnh chung và Member đã đăng nhập: có thể tích hợp thông tin hồ sơ dinh dưỡng tham khảo của Member (FR-35) khi người dùng hỏi về chỉ số cá nhân.
  - Bước 5: Hệ thống gửi yêu cầu tới dịch vụ AI qua Backend an toàn (BR-06; không để lộ thông tin bảo mật hay khóa truy cập ra client).
  - Bước 6: Dịch vụ AI phản hồi kết quả hợp lệ đáp ứng thời gian quy định tại [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03).
  - Bước 7: Hệ thống ghi nhận dữ liệu đo lường kỹ thuật (telemetry nếu có theo BR-04) và TUYỆT ĐỐI KHÔNG lưu trữ nội dung câu hỏi thô (FR-11, BR-04, NFR-22).
  - Bước 8: Giao diện hiển thị câu trả lời dạng văn bản định dạng rõ ràng cho người dùng, phân biệt rõ lời AI với nội dung gốc của tác giả (và tùy chọn đóng/chuyển đổi ngữ cảnh nếu đang ở ngữ cảnh Recipe).
- **Luồng thay thế (Alternative Flows):**
  - *AF-51.1 (Đóng hoặc chuyển đổi ngữ cảnh công thức):* Khi đang ở ngữ cảnh Recipe Post, người dùng có thể đóng ngữ cảnh công thức để chuyển khung chat về ngữ cảnh chung, hoặc chuyển sang xem bài công thức khác để nhận ngữ cảnh công thức mới mà không cần mở lại cửa sổ chat.
  - *AF-51.2 (Vượt giới hạn tần suất kỹ thuật — Guest):* Khi Guest gửi request vượt quá tần suất kỹ thuật cho phép trong 1 phút, hệ thống tạm dừng nhận câu hỏi, hiển thị thông báo thao tác quá nhanh và gợi ý đăng ký/đăng nhập tài khoản Member (FR-02, BR-01, BR-05).
  - *AF-51.3 (Yêu cầu tính năng AI nâng cao khi ở gói Free):* Khi Member thuộc gói Free yêu cầu các tính năng AI nâng cao (như hỗ trợ soạn bài hoặc lập thực đơn tuần), hệ thống hiển thị thông báo hướng dẫn nâng cấp gói Plus/Pro thích hợp (FR-10, BR-02, BR-03).
  - *AF-51.4 (Member hỏi BMI cá nhân nhưng chưa khai báo hồ sơ dinh dưỡng):* Nếu Member hỏi về BMI cá nhân mà chưa hoàn thành khai báo tại FR-35, Chatbot giải thích công thức tính BMI chuẩn tham khảo và hiển thị liên kết dẫn tới trang Hồ sơ dinh dưỡng để Member tự tính toán.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-51.1 (Yêu cầu chẩn đoán bệnh tật hoặc kê đơn điều trị y khoa):* Nếu câu hỏi chứa yêu cầu chẩn đoán triệu chứng hoặc chữa bệnh qua ăn chay, Chatbot tuân thủ ranh giới an toàn, lịch sự từ chối đưa ra kết luận bệnh lý hay phác đồ điều trị, và hướng dẫn người dùng tham vấn ý kiến bác sĩ chuyên khoa hoặc chuyên gia y tế (SRS 3.15, BR-09, BR-41).
  - *EF-51.2 (Lỗi kết nối dịch vụ AI hoặc quá thời gian phản hồi):* Nếu dịch vụ AI gặp sự cố kết nối, lỗi kỹ thuật hoặc quá thời gian phản hồi quy định tại NFR-03 (NFR-18), hệ thống hiển thị thông báo sự cố kỹ thuật thân thiện và ghi error log kỹ thuật (BR-04).
  - *EF-51.3 (Yêu cầu AI chỉnh sửa dữ liệu hệ thống hoặc dữ liệu dinh dưỡng):* Nếu người dùng yêu cầu Chatbot sửa đổi bài viết, thêm món vào Lịch ăn, sửa hồ sơ cá nhân hay thay đổi giá trị trong danh mục dinh dưỡng, Chatbot giải thích rõ rằng AI chỉ đóng vai trò tư vấn thông tin trong hội thoại, không có thẩm quyền sửa đổi dữ liệu người dùng và không được phép can thiệp vào danh mục dinh dưỡng chính thức (BR-51).
  - *SF-51.1 (Bảo vệ thông tin xác thực backend và phòng chống lạm dụng prompt):* Khóa truy cập dịch vụ AI được lưu trữ và kiểm soát bảo mật tại backend (BR-06); hệ thống kiểm duyệt và làm sạch dữ liệu đầu vào nhằm ngăn chặn các hành vi tấn công vượt rào an toàn (Prompt Injection) (NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Người dùng nhận được câu trả lời an toàn, phù hợp ngữ cảnh và dễ hiểu.
- Dữ liệu đo lường kỹ thuật (telemetry) được lưu trữ ẩn danh không chứa nội dung câu hỏi thô theo thời hạn tối đa 90 ngày (FR-11, BR-04, NFR-22).
- Không có bất kỳ thay đổi nào xảy ra đối với cơ sở dữ liệu bài viết, thực đơn tuần hay hồ sơ của người dùng.

---
