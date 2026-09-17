> **Document:** System Architecture  
> **File:** `docs/architecture/ARCHITECTURE.md`  
> **Version:** v1.4.0
> **Created:** 2026-09-13  
> **Last Updated:** 2026-09-17
> **Status:** Active  
> **Related Docs:** `docs/requirements/SRS.md`, `docs/architecture/TECHNOLOGY-STACK.md`

# System Architecture

## 1. Mục đích và ranh giới bằng chứng

Tài liệu này mô tả cấu trúc cấp cao đã được xác nhận của **Mâm Xanh** (Vegetarian Support System). Tài liệu phản ánh baseline kiến trúc đã chốt cho pha triển khai, bao gồm các thành phần runtime, luồng giao tiếp với external service và topology đám mây.

Hành vi nghiệp vụ chi tiết thuộc [SRS](../requirements/SRS.md). Các lựa chọn công nghệ, thư viện và trade-off thuộc [Technology Stack](TECHNOLOGY-STACK.md).

## 2. Bối cảnh hệ thống

Hệ thống là web application responsive bằng tiếng Việt, được sử dụng qua browser. Tương tác của Guest, Member và Administrator đi qua ranh giới client công khai vào Backend ứng dụng. Backend chịu trách nhiệm thực thi quy tắc nghiệp vụ và làm trung gian cho toàn bộ persistence cũng như các lệnh gọi external service cần đặc quyền.

```text
Guest / Member / Administrator
             |
             v
   Browser + React client (Azure Static Web Apps)
             |
        REST/JSON over HTTPS (CORS / Linked Backends)
             |
             v
   Spring Boot backend (Azure App Service)
    |          |             |
    |          |             +--> Azure Application Insights (Observability)
    |          |
    v          v
SQL Server   Azure Blob      External Providers
(Azure SQL   (Media Images)  ├── Google Gemini (gemini-3.8-flash)
Serverless)                  ├── Google Identity Services (Google Login)
                             ├── Brevo (Transactional Email SMTP)
                             ├── payOS (VietQR Payment REST & Webhook)
                             └── YouTube (Embed player)
```

Toàn bộ hệ thống được triển khai trên nền tảng **Microsoft Azure** nhằm đồng nhất môi trường, tối ưu chi phí vận hành cho nhóm và tận dụng gói Azure for Students.

## 3. Các thành phần runtime chính

| Thành phần | Nền tảng / Công nghệ | Trách nhiệm đã xác nhận | Ranh giới |
|---|---|---|---|
| Browser client | React, TypeScript, Vite (Azure Static Web Apps) | Hiển thị giao diện responsive, nhận input người dùng, xử lý trạng thái loading/error/quota, nhúng YouTube player và nút Google Login | Không phải ranh giới tin cậy cho authorization, validation hoặc lưu secret |
| Spring Boot Backend | Java 21, Spring Boot (Azure App Service) | Xác thực request (JWT + Cookie Refresh Token), kiểm tra role/ownership, validate input, áp dụng quota AI, điều phối nghiệp vụ và gọi external services | Quy tắc nghiệp vụ bắt buộc thực thi ở server-side kể cả khi UI đã ẩn thao tác |
| SQL Server | Microsoft SQL Server (Azure SQL Database Serverless) | Relational Source of Truth chính cho tài khoản, công thức, thực đơn, giao dịch thanh toán và tham chiếu media | Tự động pause khi không có request; cần kích hoạt trước các buổi demo |
| Media Storage | Azure Blob Storage | Lưu trữ tệp ảnh Recipe Post (tối đa 5 ảnh, JPEG/PNG/WebP $\le 5$ MB); SQL Server giữ URL tham chiếu | Phase 1 upload qua Backend kiểm duyệt; không upload file video |
| External integrations | Google Gemini, GIS, Brevo, payOS, YouTube | Cung cấp AI gợi ý, xác thực Google, gửi email kích hoạt/reset, thanh toán VietQR và phát video | Lỗi provider phải được xử lý minh bạch; không trừ quota người dùng khi AI/email gặp sự cố |

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
React client -> Spring Boot validation/authorization -> Azure Blob Storage
                                           |
                                           -> SQL Server metadata/reference
```

Luồng ban đầu giữ authorization cho upload và kiểm tra file tại Backend. Upload trực tiếp từ browser bằng SAS URL có scope hẹp là `Future option`.

### 4.3 Luồng YouTube

Recipe Post lưu link YouTube hoặc video ID được hỗ trợ. Browser nhúng YouTube iframe player; ứng dụng không upload, sao chép hoặc lưu trữ file video YouTube.

### 4.4 Luồng Gemini AI

```text
Browser request -> backend eligibility/quota checks -> AiClient (GeminiClient)
                                                          |
                                                          v
                                                 Google Gen AI Java SDK
                                                          |
                                                          v
                                                 gemini-3.8-flash
                                                          |
        valid JSON response <- structured output validation
        failure / timeout   <- classified error, KHÔNG trừ quota người dùng
```

- **SDK & Model:** Dùng Google Gen AI Java SDK chính thức (`com.google.genai:google-genai`), khóa model ID cố định là `gemini-3.8-flash`.
- **Abstraction:** Đóng gói qua interface `AiClient` để độc lập logic nghiệp vụ và hỗ trợ viết Mock Unit Test.
- **Resilience:** Cấu hình timeout và tối đa 1 lần retry cho transient errors (mạng, 429, 5xx). Lỗi external AI không làm trừ lượt quota của người dùng.
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
Tìm hoặc tạo tài khoản Member (ACTIVE)
      |
Backend phát hành JWT Access Token + Rotating Refresh Token (HttpOnly Cookie)
```

- Lưu trữ `google_subject` (`sub`) làm khóa định danh tài khoản Google ổn định, phòng trường hợp tài khoản thay đổi email.

### 4.8 Luồng Email thông báo (Brevo SMTP)

- **Công nghệ:** Sử dụng `spring-boot-starter-mail` (`JavaMailSender`) kết nối tới Brevo SMTP Server.
- **Nghiệp vụ:** Gửi email kích hoạt tài khoản (hạn 24h), đặt lại mật khẩu (hạn 15 phút) và thông báo kiểm duyệt bài viết.
- **Xử lý nền:** Áp dụng `@Async` (kết hợp Virtual Threads của Java 21) để việc gửi email không làm nghẽn luồng xử lý HTTP request chính. Lỗi gửi email (nếu có) được ghi log và không rollback transaction nghiệp vụ.

## 5. Ranh giới tin cậy và bảo mật

- **Ranh giới không tin cậy:** Browser là môi trường không tin cậy. Mọi quyền hạn, quota và validation phải được kiểm soát ở server-side.
- **Quản lý Secrets đa tầng:**
  - *Local Dev:* Biến môi trường hệ thống hoặc file `.env` (tuyệt đối không commit lên Git, duy trì file mẫu `.env.example`).
  - *CI/CD:* GitHub Environment Secrets trong GitHub Actions.
  - *Production:* Cấu hình trực tiếp trên Azure App Service Application Settings hoặc nạp qua Azure Key Vault reference.
- **Bảo mật Webhook:** Bắt buộc xác minh chữ ký số HMAC-SHA256 với `checksumKey` của payOS trước khi đọc payload webhook.
- **Bảo vệ phiên làm việc:** Access Token ngắn hạn, Rotating Refresh Token lưu trong Secure HttpOnly Cookie với cờ `SameSite=None; Secure`. Logout thu hồi phiên làm việc trên server-side.
- **Bảo mật Logging:** Logback/SLF4J tuyệt đối không ghi mật khẩu, token, API key, SAS URL, nội dung prompt cá nhân hoặc thông tin sức khỏe nhạy cảm.

## 6. Ràng buộc kiến trúc

- Giữ ranh giới giữa React client, Spring Boot Backend và Microsoft SQL Server.
- Giữ relational database làm Source of Truth cho ứng dụng; dùng Flyway để quản lý thay đổi schema có thể thực thi sau khi Backend scaffold tồn tại.
- Backend giữ cấu trúc Modular Monolith; MVC/layered structure bên trong từng module.
- Không tự ý thêm microservices, Kafka, Redis, Kubernetes hay vector database khi chưa có quyết định kiến trúc mới.

## 7. Các vấn đề kiến trúc và trạng thái quyết định

| Vấn đề | Trạng thái hiện tại | Giải pháp đã xác nhận / Ghi chú |
|---|---|---|
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
