> **Document:** System Architecture  
> **File:** `docs/architecture/ARCHITECTURE.md`  
> **Version:** v1.10.0  
> **Created:** 2026-09-13  
> **Last Updated:** 2026-09-23  
> **Status:** Active  
> **Related Docs:** `docs/requirements/SRS.md`, `docs/architecture/TECHNOLOGY-STACK.md`, `docs/diagrams/C4 Container Diagram/README.md`, `docs/diagrams/ERD/README.md`

# Architecture Document — Mâm Xanh

Tài liệu này xác định các ranh giới kiến trúc, thành phần runtime, luồng giao tiếp và các quyết định kỹ thuật cốt lõi của hệ thống **Mâm Xanh (Vegetarian Support System)**.

Chi tiết về lý do lựa chọn từng công nghệ, đánh giá trade-off và công cụ hỗ trợ được quản lý tại [TECHNOLOGY-STACK.md](TECHNOLOGY-STACK.md). Sơ đồ trực quan C4 Container Diagram được lưu trữ tại [C4 Container Diagram/](../diagrams/C4%20Container%20Diagram/README.md).

## 1. Mục tiêu và phạm vi kiến trúc

- Định hình các ranh giới đáng tin cậy giữa Frontend, Backend, Cơ sở dữ liệu và các Nhà cung cấp dịch vụ bên ngoài (External Providers).
- Xác định mô hình kiến trúc thực thi thống nhất cho nhóm phát triển 5 thành viên trong môn SWP391.
- Bảo đảm tính toàn vẹn dữ liệu, kiểm soát chi phí API đám mây và phòng ngừa các lỗ hổng bảo mật phổ biến.
- Giữ vững phạm vi MVP đã cam kết; không tự tiện mở rộng sang các công nghệ hoặc cấu trúc phức tạp chưa được phê duyệt.

## 2. Bối cảnh hệ thống (System Context)

Hệ thống cung cấp nền tảng web responsive phục vụ người ăn chay khám phá công thức, quản lý lịch ăn và nhận gợi ý thông minh:

```text
[ Người dùng / Trình duyệt Web ]
              │ (HTTPS / REST)
              ▼
[ Azure Static Web Apps (Frontend React + TS + Vite) ]
              │ (REST API / JSON / HttpOnly Cookie)
              ▼
[ Azure App Service (Backend Spring Boot + Java 21) ]
       │            │                  │
       │ (JDBC)     │ (Azure SDK)      │ (REST API / HTTPS)
       ▼            ▼                  ▼
[ Azure SQL ] [ Azure Blob ] [ External Providers ]
(Serverless)   (Images)        ├── Google Gemini 3.8 Flash (AI)
                               ├── Google Identity Services (GIS Login)
                               ├── Brevo (Transactional Email SMTP)
                               ├── payOS (VietQR Payment Gateway)
                               └── YouTube (Embed player)
```

Toàn bộ hệ thống được triển khai trên nền tảng **Microsoft Azure** nhằm đồng nhất môi trường, tối ưu chi phí vận hành cho nhóm và tận dụng gói Azure for Students.

## 3. Các thành phần runtime chính

| Thành phần | Nền tảng / Công nghệ | Trách nhiệm đã xác nhận | Ranh giới |
|---|---|---|---|
| Browser client | React, TypeScript, Vite (Azure Static Web Apps) | Hiển thị giao diện responsive, nhận input người dùng, xử lý trạng thái loading/error, nhúng YouTube player và nút Google Login | Không phải ranh giới tin cậy cho authorization, validation hoặc lưu secret |
| Spring Boot Backend | Java 21, Spring Boot (Azure App Service) | Xác thực request (JWT + Cookie Refresh Token), kiểm tra role/ownership, validate input, kiểm tra quyền tính năng AI (Feature Entitlement), áp dụng technical rate limit, điều phối nghiệp vụ và gọi external services | Quy tắc nghiệp vụ bắt buộc thực thi ở server-side kể cả khi UI đã ẩn thao tác |
| SQL Server | Microsoft SQL Server (Azure SQL Database Serverless) | Relational Source of Truth chính cho tài khoản, công thức, thực đơn, giao dịch thanh toán và tham chiếu media | Tự động pause khi không có request; cần kích hoạt trước các buổi demo |
| Media Storage | Azure Blob Storage | Lưu trữ tệp ảnh minh họa Recipe Post (0–5 ảnh qua thực thể `RECIPE_MEDIA`, JPEG/PNG/WebP $\le 5$ MB, đúng 1 ảnh bìa cover); SQL Server giữ URL tham chiếu, thứ tự hiển thị và cờ ảnh bìa | Phase 1 upload qua Backend kiểm duyệt; không upload file video |
| External integrations | Google Gemini, GIS, Brevo, payOS, YouTube | Cung cấp AI gợi ý, xác thực Google, gửi email kích hoạt/reset, thanh toán VietQR và phát video | Lỗi provider phải được xử lý minh bạch; không tính phí người dùng khi AI/email gặp sự cố |

### 3.1 Kiến trúc Backend

**Decision status:** `DEC-016 — A — APPROVED`.

**Backend Architecture: Modular Monolith using MVC/layered structure within each business module.**

Backend là một Spring Boot application duy nhất và được phát hành thành một deployable backend; dự án không tách các business capability thành microservice. Source code được tổ chức theo business capability, chẳng hạn `auth`, `recipe`, `mealplan`, `shopping`, `nutrition`, `subscription` và `admin`.

Bên trong mỗi business module phải áp dụng MVC/layered structure và thể hiện rõ tối thiểu các trách nhiệm `controller`, `service`, `repository`, `model`/`entity`, cùng `dto` khi cần.

Luồng chuẩn:

```text
React View
-> Spring MVC Controller
-> Service
-> Repository
-> Model/Entity
-> Database
```

## 4. Các luồng giao tiếp chính

### 4.1 Luồng request của ứng dụng

1. Browser gửi request REST/JSON tới Spring Boot Backend kèm Access Token (Bearer Header).
2. Backend thực hiện authentication qua Spring Security filter, kiểm tra authorization/ownership và validate dữ liệu đầu vào.
3. Backend truy cập SQL Server và gọi các external provider khi cần.
4. Backend trả kết quả JSON hoặc mã lỗi chuẩn HTTP rõ ràng để client hiển thị.

### 4.2 Luồng ảnh Recipe Post

```text
React client -> Spring Boot validation/authorization -> Azure Blob Storage (0-5 images)
                                           |
                                           -> SQL Server RECIPE_MEDIA (URL, display_order, is_cover)
```

Luồng ban đầu giữ authorization cho upload và kiểm tra file (JPEG/PNG/WebP $\le 5$ MB, số lượng 0–5 ảnh, đúng 1 ảnh bìa `is_cover = true`) tại Backend. Upload trực tiếp từ browser bằng SAS URL có scope hẹp là `Future option`.

### 4.3 Luồng YouTube

Recipe Post lưu link YouTube hoặc video ID được hỗ trợ. Browser nhúng YouTube iframe player; ứng dụng không upload, sao chép hoặc lưu trữ file video YouTube.

### 4.4 Luồng Gemini AI

```text
Browser request -> backend feature entitlement & rate limit checks -> AiClient (GeminiClient)
                                                                             |
                                                                             v
                                                                    Google Gen AI Java SDK
                                                                             |
                                                                             v
                                                                    gemini-3.8-flash
                                                                             |
            valid JSON response <- structured output validation
            failure / timeout   <- classified error, ghi log kỹ thuật, phản hồi thân thiện
```

- **SDK & Model:** Dùng Google Gen AI Java SDK chính thức (`com.google.genai:google-genai`), khóa model ID cố định là `gemini-3.8-flash`.
- **Abstraction:** Đóng gói qua interface `AiClient` để độc lập logic nghiệp vụ và hỗ trợ viết Mock Unit Test.
- **Resilience:** Cấu hình timeout và tối đa 1 lần retry cho transient errors (mạng, 429, 5xx). Lỗi external AI không làm gián đoạn các tính năng phi AI của ứng dụng.
- **Output:** Áp dụng Native Structured Outputs (JSON Schema) để nhận kết quả gợi ý món ăn/thực đơn có cấu trúc chặt chẽ.

### 4.5 Capability Google Maps (`OUT_OF_SCOPE`)

FR-42/FR-43, M11 và tích hợp Google Maps là `OUT_OF_SCOPE`. Ứng dụng không quản lý hoặc xác minh dữ liệu nhà hàng bên ngoài; Google Maps không phải dependency của baseline kiến trúc hiện tại.

### 4.6 Luồng thanh toán payOS

```text
Member chọn gói -> Backend tạo Payment record -> Gọi payOS REST API (RestClient)
                                                       |
                                                       v
Frontend hiển thị VietQR link <- Nhận checkoutUrl & QR từ payOS
      |
User thanh toán qua App Ngân hàng
      |
payOS gọi Webhook POST /api/webhooks/payos -> Backend verify chữ ký HMAC-SHA256
                                                  |
                     Đã SUCCESS? ──> YES: Bỏ qua (Idempotent HTTP 200)
                                 └──> NO: Cập nhật SUCCESS & Kích hoạt gói PLUS/PRO
```

- **Mô hình giá:** FREE 0, PLUS 49,000, PRO 99,000 VND/tháng; không auto-renew, không hoàn tiền từng phần.
- **Idempotency:** Bảng `Payment` quản lý `order_code` (UNIQUE). Webhook gửi lại nhiều lần chỉ kích hoạt subscription đúng 1 lần duy nhất.

### 4.7 Luồng xác thực Google Login (Google Identity Services)

```text
Guest click Google Sign-in -> Frontend Google Identity Services (GIS)
                                           |
                                           v
Backend nhận ID Token <- Trả Google ID Token (JWT) về React client
      |
Backend dùng GoogleIdTokenVerifier kiểm tra chữ ký, iss, aud, exp
      |
Trích xuất google_subject (sub), email, name, avatar
      |
Tìm hoặc tạo tài khoản Customer (ACTIVE, role = ROLE_CUSTOMER)
      |
Backend phát hành JWT Access Token + Rotating Refresh Token (HttpOnly Cookie)
```

- Lưu trữ `google_subject` (`sub`) làm khóa định danh tài khoản Google ổn định, phòng trường hợp tài khoản thay đổi email.

### 4.8 Luồng Email thông báo (Brevo SMTP)

- **Công nghệ:** Sử dụng `spring-boot-starter-mail` (`JavaMailSender`) kết nối tới Brevo SMTP Server.
- **Nghiệp vụ:** Gửi email kích hoạt tài khoản (hạn 24h), đặt lại mật khẩu (hạn 15 phút) và thông báo kiểm duyệt bài viết.
- **Xử lý nền:** Áp dụng `@Async` (kết hợp Virtual Threads của Java 21) để việc gửi email không làm nghẽn luồng xử lý HTTP request chính. Lỗi gửi email (nếu có) được ghi log và không rollback transaction nghiệp vụ.

## 5. Ranh giới tin cậy và bảo mật

### 5.1 Ranh giới tin cậy chung & Quản lý bí mật

- **Ranh giới không tin cậy:** Browser là môi trường không tin cậy. Mọi quyền hạn, phân quyền tính năng AI (Feature Entitlement), rate limit và validation phải được kiểm soát ở server-side.
- **Quản lý Secrets đa tầng:**
  - *Local Dev:* Biến môi trường hệ thống hoặc file `.env` (tuyệt đối không commit lên Git, duy trì file mẫu `.env.example`).
  - *CI/CD:* GitHub Environment Secrets trong GitHub Actions.
  - *Production:* Cấu hình trực tiếp trên Azure App Service Application Settings hoặc nạp qua Azure Key Vault reference.
- **Bảo mật Webhook:** Bắt buộc xác minh chữ ký số HMAC-SHA256 với `checksumKey` của payOS trước khi đọc payload webhook.
- **Bảo vệ phiên làm việc:** Access Token ngắn hạn, Rotating Refresh Token lưu trong Secure HttpOnly Cookie với cờ `SameSite=None; Secure`. Logout thu hồi phiên làm việc trên server-side.
- **Bảo mật Logging:** Logback/SLF4J tuyệt đối không ghi mật khẩu, token, API key, SAS URL, nội dung prompt cá nhân hoặc thông tin sức khỏe nhạy cảm.

### 5.2 Ranh giới Phân quyền RBAC & Quyền Tác giả Chuyên gia (RBAC & Expert Authorship Boundaries)

- **Mô hình 3 Vai trò (Three-Role RBAC):**
  - `ROLE_CUSTOMER`: Người dùng thông thường đã đăng ký tài khoản. Có quyền duyệt, tìm kiếm, lưu công thức (`SAVED_RECIPE`), bình chọn Like/Dislike bài công thức (`RECIPE_REACTION`, trừ bài do chính mình sáng tác), bình luận (`COMMENT`), lập kế hoạch bữa ăn (`MEAL_PLAN`), tạo danh sách đi chợ (`SHOPPING_LIST`), xuất file PDF công thức/thực đơn và báo cáo phân tích dinh dưỡng tuần kèm so sánh DRI cá nhân (FR-20, FR-33, FR-44), và nộp đơn đăng ký Chuyên gia (`EXPERT_APPLICATION`).
  - `ROLE_EXPERT`: Chuyên gia ẩm thực chay đã được Quản trị viên phê duyệt đơn. Kế thừa toàn bộ quyền của Customer, đồng thời sở hữu độc quyền quyền tạo, chỉnh sửa, xóa và tải ảnh cho các bài viết công thức (`RECIPE_POST`).
  - `ROLE_ADMIN`: Quản trị viên hệ thống. Quản lý tài khoản, danh mục nguyên liệu/đơn vị đo/bảng quy đổi chuẩn (FR-18), kiểm duyệt nội dung/báo cáo, và xét duyệt các đơn đăng ký Chuyên gia.

- **Bảo vệ Endpoint Sáng tạo Công thức (Recipe Post Creation/Mutation Enforcement):**
  - Các API tạo, sửa, xóa bài viết công thức và upload media liên quan:
    - `POST /api/v1/recipes`
    - `PUT /api/v1/recipes/{id}`
    - `DELETE /api/v1/recipes/{id}`
    - `POST /api/v1/recipes/{id}/media`
  - **Quy tắc bảo vệ:** Bắt buộc áp dụng `@PreAuthorize("hasRole('EXPERT')")` tại Spring MVC Controller và Service layer cho thao tác tạo/sửa nội dung Recipe Post (`BR-07`, `FR-04`, `FR-44`); quyền hậu kiểm của Admin được kiểm soát riêng theo `FR-06`.
  - **Xử lý vi phạm:** Nếu tài khoản mang vai trò `ROLE_CUSTOMER` cố tình gửi HTTP request đến các endpoint này (kể cả khi đã can thiệp qua DevTools/Postman), Backend lập tức chặn và trả về HTTP status `403 Forbidden` (`AC-04.6`). Phân quyền không bao giờ chỉ dựa vào việc ẩn nút bấm trên giao diện người dùng.

- **Bảo vệ Endpoint Xét duyệt Đơn Chuyên gia (Expert Application Workflow Protection):**
  - Nộp đơn: `POST /api/v1/expert-applications` dành cho `ROLE_CUSTOMER` (`@PreAuthorize("hasRole('CUSTOMER')")`). Backend kiểm tra ràng buộc không được có đơn `PENDING` nào đang chờ xử lý (`BR-74`, trả về `409 Conflict` nếu vi phạm).
  - Tra cứu và xử lý đơn:
    - `GET /api/v1/admin/expert-applications`
    - `PUT /api/v1/admin/expert-applications/{id}/approve`
    - `PUT /api/v1/admin/expert-applications/{id}/reject`
  - **Quy tắc bảo vệ:** Bắt buộc áp dụng `@PreAuthorize("hasRole('ADMIN')")`.
  - **Nguyên tử hóa giao dịch (Transactional Role Upgrade):** Khi Admin chấp thuận đơn (`approve`), Backend thực hiện cập nhật trạng thái đơn thành `APPROVED` và nâng cấp vai trò của người dùng từ `ROLE_CUSTOMER` lên `ROLE_EXPERT` trong cùng một Database Transaction duy nhất (`@Transactional`).

## 6. Ràng buộc kiến trúc

- Giữ ranh giới giữa React client, Spring Boot Backend và Microsoft SQL Server.
- Giữ relational database làm Source of Truth cho ứng dụng; dùng Flyway để quản lý thay đổi schema có thể thực thi sau khi Backend scaffold tồn tại.
- **Baseline mô hình dữ liệu quan hệ (Relational Schema Baseline):**
  - Conceptual ERD hiện có 22 thực thể (v1.8.0); `USER_FOLLOW` thuộc phạm vi bắt buộc theo FR-59/BR-75 `ACTIVE` và phải được kế thừa xuống Logical ERD, Physical ERD cùng schema triển khai.
  - Thực thể đánh giá chất lượng công thức được chuẩn hóa thành `RECIPE_REACTION` (lưu trữ phản hồi Like/Dislike với `reaction_type: LIKE | DISLIKE`, ràng buộc `UNIQUE(user_id, recipe_id)` để đảm bảo mỗi thành viên có tối đa 1 phản hồi hiệu lực trên một bài công thức theo `BR-69`, `FR-57`).
  - Không sử dụng các bảng phân loại động `CATEGORY` và `RECIPE_CATEGORY`; thể loại món ăn được chuẩn hóa trực tiếp thành trường thuộc tính `dish_category` trên thực thể bài công thức (`RECIPE_POST` theo BR-19).
  - Không duy trì bảng độc lập `RECIPE_STEP`; toàn bộ hướng dẫn chế biến được lưu trữ dưới dạng trường văn bản tự do linh hoạt `instructions` (10–5.000 ký tự) trên `RECIPE_POST` (FR-16, BR-19; FR-22 đã RETIRED theo Phương án B).
- Backend giữ cấu trúc Modular Monolith; MVC/layered structure bên trong từng module.
- Không tự ý thêm microservices, Kafka, Redis, Kubernetes hay vector database khi chưa có quyết định kiến trúc mới.
- **Kiến trúc tìm kiếm, sắp xếp và xếp hạng không dùng AI (Non-AI Ranking Architecture):**
  - Hệ thống hỗ trợ 6 chế độ khám phá/sắp xếp bài viết/công thức: Mới nhất (*Newest*), Được yêu thích nhất (*Most Liked / Highest Rated* theo tỷ lệ % Like giảm dần và tổng Like giảm dần), Xem nhiều nhất (*Most Viewed* - 24h/7d/30d/toàn thời gian), Bình luận nhiều nhất (*Most Commented*), Hoạt động sôi nổi nhất (*Most Active* - BR-71, tổng tương tác 7 ngày không phân rã), và Thịnh hành (*Trending* - BR-72, tương tác có phân rã thời gian theo công thức trọng số).
  - Tất cả các chế độ sắp xếp, lọc và tính điểm xếp hạng này được thực thi thuần túy thông qua truy vấn quan hệ SQL chuẩn (sử dụng Index tối ưu, Computed Columns hoặc Scheduled Aggregation trên Microsoft SQL Server) và xử lý logic tại tầng Backend Spring Boot.
  - Tuyệt đối **không sử dụng** AI, Machine Learning recommendation engines, vector database, Redis cache hay Kafka message broker cho tính năng khám phá và xếp hạng này trong phạm vi hiện tại.
  - Ghi nhận lượt xem (`RECIPE_VIEW`) áp dụng cơ chế chống trùng lặp (deduplication window 30 phút theo IP hash / Client session / Member ID) và cập nhật bộ đếm bất đồng bộ (`@Async` hoặc in-memory buffer định kỳ flush xuống DB) để không làm nghẽn luồng đọc công thức.

## 7. Các vấn đề kiến trúc và trạng thái quyết định

| Vấn đề | Trạng thái hiện tại | Giải pháp đã xác nhận / Ghi chú |
|---|---|---|
| Role-Based Authorization & Expert Authorship | **Confirmed** | Phân quyền 3 vai trò (`ROLE_CUSTOMER`, `ROLE_EXPERT`, `ROLE_ADMIN`); chỉ EXPERT được tạo/sửa recipe của mình; Customer nộp đơn văn bản, ADMIN phê duyệt đơn và hậu kiểm nội dung (`BR-07`, `BR-74`, `FR-06`) |
| AI Provider & Model | **Confirmed** | Google Gemini model `gemini-3.8-flash` qua Google Gen AI Java SDK, bọc qua `AiClient`, timeout + retry, structured output |
| Payment Provider | **Confirmed** | `payOS` (REST API qua Spring `RestClient` + Webhook HMAC-SHA256, xử lý idempotent theo `order_code`) |
| Email Service | **Confirmed** | `Brevo` qua Spring Boot Mail (SMTP), xử lý bất đồng bộ `@Async`, lỗi không rollback |
| Authentication Detail | **Confirmed** | Google Identity Services + `GoogleIdTokenVerifier` + Internal JWT & Rotating Refresh Cookie |
| Deployment Topology | **Confirmed** | Azure Stack: Static Web Apps (FE) + App Service (BE) + Azure SQL Serverless (DB) + Blob Storage (Media) |
| Monitoring & Observability | **Confirmed** | Spring Boot Actuator (`/actuator/health`) + Logback + Azure Application Insights Java Agent |
| QA & Testing Tooling | **Confirmed** | JUnit 5 + Mockito + JaCoCo + Codecov (CI reporting) + Testmail (Email E2E testing) + Requestly Pro (FE mocking) |
| Tích hợp Google Maps | `OUT_OF_SCOPE` | Không thuộc baseline kiến trúc; M11 giữ trong SRS để bảo toàn lịch sử |
| Upload trực tiếp lên Azure | Future option | Xem xét lại khi kích thước file/tải thực tế vượt quá năng lực xử lý của Backend |
| Module Blog cộng đồng | Out of MVP Scope | Được phân rã tại SRS 3.21; chỉ xem xét kiến trúc sau khi các module cốt lõi hoàn thành |
| Frontend State & Styling | TBD | Sẽ quyết định cùng React scaffold (React Context / Tailwind CSS / UI library) |
| Demo Domain Polish | Open Polish | Đăng ký 1 tên miền từ GitHub Student Pack (.tech hoặc Name.com) CNAME về Azure trước buổi demo |
| Azure SQL Cold Start | Operational Note | Kích hoạt database trước 5-10 phút trước khi thuyết trình để tránh độ trễ thức dậy của Serverless |
