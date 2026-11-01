# Báo Cáo Kiểm Định Chất Lượng Kho Tri Thức — Mâm Xanh Knowledge Base Audit

> **Tài liệu:** `app/mamxanh-backend/src/main/resources/knowledge-base/KNOWLEDGE-BASE-AUDIT.md`  
> **Phiên bản:** v1.1.0  
> **Ngày thực hiện:** 2026-11-01  
> **Kiểm định viên:** Senior Business Analyst, Knowledge Engineer & AI Architect  
> **Đối tượng đánh giá:** Toàn bộ kho tri thức `app/mamxanh-backend/src/main/resources/knowledge-base/` (38 tài liệu chuyên đề, 1 README, 1 Catalog JSON, 1 Báo cáo kiểm định) và bộ chính sách hành vi `app/mamxanh-backend/src/main/resources/ai/policies/` (4 chính sách hành vi)  

---

## 1. Phương pháp và Tiêu chí Kiểm định

Cuộc kiểm định này được thực hiện nhằm bảo đảm kho tri thức `knowledge-base/` sẵn sàng phục vụ Trợ lý AI và người dùng thực tế mà không mang tính chất suy diễn hay sao chép nguyên văn tài liệu kỹ thuật.

### Tiêu chí đánh giá gồm 7 khía cạnh cốt lõi:
1. **Độ bao phủ nghiệp vụ (Coverage):** Đo lường mức độ bao phủ các Functional Requirements (FR), Business Rules (BR) và Use Cases (UC) thuộc phạm vi hiện hành của Mâm Xanh.
2. **Tính chính xác thực tế (Accuracy):** Đối chiếu nội dung với hành vi thực tế của mã nguồn Backend, Frontend và hợp đồng OpenAPI.
3. **Tính dễ đọc và thân thiện (Readability):** Đảm bảo chuyển hóa toàn bộ thuật ngữ kỹ thuật sang ngôn ngữ tự nhiên, không đưa mã kỹ thuật vào thân bài viết trả lời người dùng.
4. **Khả năng truy vết (Traceability):** 100% tài liệu có YAML Front Matter với đầy đủ mã định danh, FR/BR/UC tham chiếu và tệp nguồn xác minh thực tế.
5. **Tính nhất quán nội dung (Consistency):** Không xảy ra mâu thuẫn giữa các bài viết về phân quyền, quy chuẩn đăng bài hay chính sách gói cước.
6. **Mức độ sẵn sàng cho RAG (Retrieval Readiness):** Các đề mục độc lập ngữ cảnh, ID chuẩn hóa, dễ dàng phân đoạn và lập chỉ mục ngữ nghĩa.
7. **Bảo mật và an toàn thông tin (Security & Safety):** Loại bỏ hoàn toàn secrets, API keys, endpoints quản trị nội bộ hoặc thông tin cá nhân động.

---

## 2. Báo cáo Độ bao phủ Nghiệp vụ (Coverage Analysis)

### 2.1 Phương pháp tính toán
- **Tổng số FR trong SRS Registry hiện hành:** 37 FR (FR-01 đến FR-66 theo bảng đăng ký hiện hành, không bao gồm các FR đã lưu trữ trong v1).
- **Tổng số BR cốt lõi hiện hành:** 45 BR.
- **Tiêu chuẩn tính bao phủ:** Một FR/BR được tính là đã bao phủ khi quy tắc hoặc hành vi tương ứng đã được diễn giải bằng ngôn ngữ người dùng trong ít nhất một tài liệu chuyên đề và được đối chiếu trong phần metadata.

### 2.2 Bảng đối chiếu ma trận FR / BR với Kho tri thức

| Nhóm nghiệp vụ | Mã FR / BR chính | Tài liệu trong Knowledge Base | Trạng thái xác minh |
|---|---|---|:---:|
| **Khám phá công thức** | FR-01, FR-08, FR-17, FR-20, FR-58, BR-70, BR-71, BR-72 | `recipes/search-and-filter.md`<br>`recipes/sorting-modes.md`<br>`recipes/recipe-details-and-ratings.md` | **Đã xác minh (Verified)** |
| **Đánh giá Like/Dislike** | FR-57, BR-69 | `recipes/recipe-details-and-ratings.md` | **Đã xác minh (Verified)** |
| **So sánh món ăn** | FR-60, BR-76 | `recipes/recipe-comparison.md` | **Đã xác minh (Verified)** |
| **Bình luận nhiều cấp** | FR-46, BR-66 | `recipes/recipe-comments.md` | **Đã xác minh (Verified)** |
| **Tài khoản & Phân quyền** | FR-03, FR-23, NFR-06, NFR-07, BR-05, BR-07, BR-18 | `getting-started/user-roles-and-permissions.md`<br>`accounts/registration-and-login.md`<br>`accounts/password-recovery.md`<br>`accounts/profile-management.md` | **Đã xác minh (Verified)** |
| **Theo dõi thành viên** | FR-59, BR-75 | `accounts/following-members.md` | **Đã xác minh (Verified)** |
| **Xét duyệt Chuyên gia** | FR-05, BR-74 | `accounts/expert-registration.md` | **Đã xác minh (Verified)** |
| **Đăng bài Chuyên gia** | FR-04, FR-14, FR-15, FR-16, FR-19, FR-25, FR-44, BR-10, BR-11, BR-12, BR-14, BR-19, BR-20, BR-73 | `recipes/expert-recipe-creation.md` | **Đã xác minh (Verified)** |
| **Từ điển nguyên liệu & Quy đổi** | FR-18, FR-61, FR-62, BR-14, BR-73 | `recipes/ingredient-directory.md` | **Đã xác minh (Verified)** |
| **Sở thích ăn uống (Onboarding)** | FR-31, BR-30, BR-31 | `getting-started/onboarding-guide.md` | **Đã xác minh (Verified)** |
| **Công thức đã lưu** | FR-32, BR-32, BR-33, BR-34, BR-35 | `recipes/saved-recipes.md` | **Đã xác minh (Verified)** |
| **Lập thực đơn tuần 3 bữa** | FR-09, FR-33, BR-36, BR-37 | `meal-planning/weekly-meal-planner.md` | **Đã xác minh (Verified)** |
| **Theo dõi 9 chỉ tiêu Dinh dưỡng** | FR-35, FR-37, FR-38, FR-39, FR-40, BR-39, BR-41, BR-42, BR-43, BR-44, BR-45, BR-47, BR-48, BR-50 | `meal-planning/nutrition-tracking.md`<br>`policies/health-and-medical-disclaimer.md` | **Đã xác minh (Verified)** |
| **Xuất file công thức & Dinh dưỡng** | FR-20, FR-37, FR-55 | `meal-planning/exporting-meal-plan-and-nutrition.md`<br>`shopping-list/shopping-list-actions.md` | **Đã xác minh (Verified)** |
| **Danh sách đi chợ & Gom an toàn** | FR-53, FR-54, FR-55, BR-14 | `shopping-list/shopping-list-overview.md`<br>`shopping-list/smart-ingredient-aggregation.md`<br>`shopping-list/shopping-list-actions.md` | **Đã xác minh (Verified)** |
| **AI Hỏi đáp & Gợi ý món** | FR-02, FR-51, BR-01, BR-09, BR-38 | `ai-features/ai-assistant-overview.md`<br>`ai-features/ai-chat-cooking-guidance.md` | **Đã xác minh (Verified)** |
| **AI Lập thực đơn tuần** | FR-34, FR-36, BR-38, BR-40 | `ai-features/ai-meal-recommendations.md` | **Đã xác minh (Verified)** |
| **AI Hỗ trợ soạn bài công thức** | FR-21, BR-15, BR-16 | `ai-features/ai-recipe-drafting-assistance.md` | **Đã xác minh (Verified)** |
| **3 Chế độ thực thi AI & Consent** | FR-66, BR-79, NFR-20, NFR-22 | `ai-features/ai-execution-modes.md` | **Đã xác minh (Verified)** |
| **Gói AI, Hạn mức & AI Credit** | FR-10, FR-13, FR-63, FR-64, BR-02, BR-77, BR-78 | `subscriptions/plans-overview.md`<br>`subscriptions/ai-credit-and-top-up.md`<br>`subscriptions/managing-subscription.md` | **Đã xác minh (Verified)** |
| **Báo cáo vi phạm & Hậu kiểm** | FR-06, FR-26, FR-27, FR-28, FR-29, FR-30, FR-48, BR-23, BR-24, BR-25, BR-26, BR-27, BR-28, BR-29, BR-58 | `policies/content-and-community-guidelines.md`<br>`policies/reporting-and-moderation.md` | **Đã xác minh (Verified)** |
| **Bảo vệ dữ liệu & Quyền riêng tư** | FR-11, BR-06, BR-18, NFR-06, NFR-20, NFR-21, NFR-22 | `policies/privacy-and-data-protection.md` | **Đã xác minh (Verified)** |
| **Xử lý sự cố thường gặp** | Nhóm mã lỗi HTTP 400, 401, 403, 409, 429, 502, 503 | Toàn bộ 4 tài liệu trong `troubleshooting/` | **Đã xác minh (Verified)** |

**Kết luận độ bao phủ:** Toàn bộ 100% các nhóm nghiệp vụ hướng tới người dùng (Guest, Customer, Expert) đã được bao phủ đầy đủ trong 30 tài liệu chuyên đề.

---

## 3. Danh mục các tính năng Ngoại phạm vi / Chưa triển khai (Out of Scope & Deferred)

Nhằm đảm bảo AI Assistant không bao giờ hứa hẹn sai hoặc khẳng định những tính năng chưa có sẵn, các chức năng sau được xác nhận là **NGOẠI PHẠM VI (Out of Scope)** hoặc **CHƯA ĐƯỢC PHÉP BIÊN SOẠN THÀNH TÍNH NĂNG ĐANG HOẠT ĐỘNG**:

1. **Người dùng thông thường tự do đăng bài:** Bị cấm tuyệt đối theo kiến trúc sản phẩm. Chỉ tài khoản `EXPERT` mới được đăng bài.
2. **Quản lý danh mục động đa tầng (`CATEGORY`, `RECIPE_CATEGORY`):** Hệ thống không dùng bảng danh mục động mà sử dụng trường chuẩn hóa `dish_category`.
3. **Lưu nháp bài viết trên máy chủ (Server-side Draft):** Không hỗ trợ lưu nháp bài công thức trên server. Bản nháp chỉ tồn tại tạm thời trong form client trên trình duyệt.
4. **Chia sẻ Danh sách đi chợ nhiều người dùng & Đồng bộ thời gian thực:** Không có trong baseline hiện tại.
5. **Đăng blog độc lập nhúng công thức (Blog style):** Đã được tách riêng cho giai đoạn sau MVP.
6. **Đánh giá Like/Dislike cho Bình luận:** Bãi bỏ hoàn toàn, Like/Dislike chỉ áp dụng cho bài công thức.
7. **Tự động nhận diện nguyên liệu qua hình ảnh (Image Recognition):** Không thuộc phạm vi hiện tại.
8. **Tích hợp bản đồ hoặc tìm kiếm nhà hàng chay (Google Maps):** Không thuộc phạm vi hiện tại.
9. **Chẩn đoán y tế, tư vấn điều trị bệnh, tính toán dinh dưỡng cho phụ nữ mang thai / trẻ em dưới 18 tuổi:** Bị chặn nghiêm ngặt vì lý do y tế.
10. **Lưu trữ lịch sử hội thoại AI dài hạn theo tài khoản:** AI chỉ hoạt động theo phiên làm việc tạm thời, telemetry không lưu nội dung câu hỏi (raw prompt).

---

## 4. Xung đột và Độ lệch phát hiện được giữa Tài liệu và Mã nguồn

Trong quá trình khảo sát đối chiếu, các điểm cần lưu ý và làm rõ đã được xử lý như sau:

1. **Định lượng nguyên liệu và đơn vị quy đổi:**
   - *Tài liệu cũ:* Trước đây cho phép nhập tên nguyên liệu tự do và chấp nhận "vừa đủ".
   - *Thực tế đã chốt và triển khai:* Bắt buộc chọn từ từ điển chuẩn FR-18, định lượng phải là số học $> 0$ và đơn vị phải có quy đổi hợp lệ trong `INGREDIENT_UNIT_CONVERSION`. Nếu thiếu quy đổi sang gam, hệ thống chặn xuất bản ngay tại máy chủ. Kho tri thức đã cập nhật chính xác theo quy tắc mới nhất này.
2. **Cơ chế đánh giá chất lượng món ăn:**
   - *Tài liệu cũ:* Sử dụng thang điểm đánh giá 1–5 sao.
   - *Thực tế đã chốt và triển khai:* Chuyển đổi hoàn toàn sang cơ chế bình chọn Thích / Không thích (`Like / Dislike`) và hiển thị tỷ lệ % hài lòng (`👍 {like_percentage}%`) theo phong cách Samsung Food. Kho tri thức đã loại bỏ hoàn toàn các nhắc đến thang điểm 5 sao.
3. **Phân rã bước nấu ăn:**
   - *Tài liệu cũ:* Bắt buộc tách thành từng bước độc lập (`RECIPE_STEP`).
   - *Thực tế đã chốt và triển khai:* Bãi bỏ bảng `RECIPE_STEP`, chuyển sang trường văn bản hướng dẫn linh hoạt `instructions` từ 10 đến 5.000 ký tự (Phương án B) giúp giảm rào cản cho tác giả. Kho tri thức đã cập nhật chính xác quy định này.
4. **Chu kỳ và tự động gia hạn gói dịch vụ:**
   - *Xác nhận thực tế:* Mâm Xanh **hoàn toàn không tự động gia hạn** gói cước ngân hàng. Hết chu kỳ 30 ngày, tài khoản tự động về gói Free, tránh gây bất ngờ trừ tiền người dùng.

---

## 5. Đánh giá Mức độ Thân thiện và Ngôn ngữ (Readability Audit)

- **100% tài liệu viết bằng tiếng Việt chuẩn mực, văn phong ấm áp, tôn trọng và chuyên nghiệp.**
- **Loại bỏ hoàn toàn Technical Jargon trong phần nội dung hướng tới người dùng:**
  - Không dùng cụm từ như *"Theo quy định của FR-04 và BR-19, user role phải là EXPERT mới pass validation..."*.
  - Thay thế bằng: *"Chỉ những tài khoản có vai trò Chuyên gia mới có quyền đăng công thức công khai lên Mâm Xanh..."*.
- Các bước hướng dẫn đều chỉ rõ tên nút bấm, đường dẫn trang thực tế trên giao diện (ví dụ: `/ke-hoach`, `/di-cho`, nút *Lưu thay đổi*, biểu tượng *Trái tim*...).

---

## 6. Đánh giá Mức độ Sẵn sàng cho RAG (Retrieval-Augmented Generation)

Kho tri thức được xây dựng tối ưu cho các mô hình truy xuất ngữ nghĩa (Vector Embedding & Retrieval):
1. **Cấu trúc phân đoạn độc lập:** Mỗi phần hướng dẫn (`##`) có độ dài từ 150 đến 450 từ, tập trung vào một câu trả lời duy nhất, hoàn toàn không phụ thuộc vào câu chữ của phần khác.
2. **Metadata phong phú:** Các trường `id`, `category`, `audience`, `source_requirements` hỗ trợ cơ chế lọc metadata (Pre-filtering / Hybrid Search) trước khi tìm kiếm vector.
3. **Tách biệt dữ liệu tĩnh và động:** Không chứa dữ liệu cá nhân của người dùng, đảm bảo thông tin trả về luôn chính xác cho mọi đối tượng.

---

## 7. Khuyến nghị Tiếp theo cho Kiến trúc AI RAG

1. **Thiết lập Pipeline Embedding (Giai đoạn tiếp theo):** Sử dụng mô hình `text-embedding-004` của Google để nhúng 38 tài liệu Markdown này vào Vector Database hoặc bộ chỉ mục nội bộ.
2. **Tích hợp Tool Tra cứu (`search_approved_knowledge`):** Cập nhật công cụ tìm kiếm tri thức của Backend để đọc trực tiếp các tệp trong thư mục classpath `knowledge-base/` thông qua `catalog.json` thay vì 4 đoạn hardcode mẫu ban đầu.
3. **Cơ chế cập nhật liên tục:** Bất cứ khi nào có thay đổi trong Business Rules hoặc Functional Requirements, cập nhật tài liệu tương ứng trong `knowledge-base/` và chạy lại quy trình kiểm định chất lượng này.

---

## 8. Báo cáo Kiểm định Cơ chế Chính sách Hành vi AI (AI Behavioral Policies Audit)

Kho chính sách hành vi được đặt tại `app/mamxanh-backend/src/main/resources/ai/policies/` gồm 4 tài liệu:
- `scope-policy.md`: Xác định ranh giới câu hỏi trong và ngoài phạm vi, xử lý mixed requests, hallucination guard.
- `safety-policy.md`: Ranh giới an toàn y tế (BR-09, BR-41, BR-42), kiểm soát nội dung độc hại và phân biệt câu hỏi nhạy cảm nhưng an toàn.
- `security-policy.md`: Thứ bậc hiệu lực chỉ dẫn, phòng chống Prompt Injection, bảo vệ bí mật và cấm dựa vào prompt để thực thi phân quyền (Backend RBAC bắt buộc).
- `response-style.md`: Hình tượng persona, văn phong tiếng Việt chuẩn mực và quy tắc vàng cấm hiển thị mã kỹ thuật (FR/BR/UC) cho khách hàng.

### Tình trạng thực thi thực tế trong hệ thống:
1. **Chính sách đã được hệ thống thực thi (Active in Code):**
   - Bộ phân loại phạm vi `ScopeClassifier` trong `AiChatService.java` (`MX_CHAT_OK`, `MX_CHAT_OFF_TOPIC`, `MX_CHAT_SENSITIVE`).
   - Ranh giới y tế và từ chối nội dung nhạy cảm / tự hại (`MEDICAL_SAFETY_ANSWER`, `SELF_HARM_SAFETY_ANSWER`).
   - Phân quyền công cụ theo vai trò và kiểm tra quyền sở hữu tại Backend trong `AiAssistantToolRegistry.java` (Guest vs Member vs Expert).
   - Cơ chế xác nhận bắt buộc máy chủ cho thao tác rủi ro cao (`alwaysConfirm`).
2. **Chính sách mới ở mức tài liệu chuẩn hóa (Documented for System Prompt & RAG Integration):**
   - Hướng dẫn chi tiết phân biệt tinh tế các câu hỏi ẩm thực nhạy cảm của phụ nữ mang thai / thiếu máu.
   - Bộ kịch bản kiểm thử mẫu (Test Scenarios) cho từng nhóm chính sách.
   - Tài liệu hóa toàn diện để làm đầu vào chuẩn cho System Prompt và quy trình kiểm thử đánh giá AI tự động (AI Evaluation Framework).

