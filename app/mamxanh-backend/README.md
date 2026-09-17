> **Document:** Backend Workspace Guide  
> **File:** `app/mamxanh-backend/README.md`  
> **Version:** v0.5.1
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-17
> **Status:** Active  

# Backend Workspace

Thư mục này dành cho REST API Java 21 + Spring Boot, build bằng Maven và truy cập Microsoft SQL Server qua Spring Data JPA/Hibernate theo [Technology Stack](../../docs/architecture/TECHNOLOGY-STACK.md).

## Trạng thái hiện tại

Backend đã được scaffold thành công với Java 21 và Spring Boot:
- Cấu hình Maven project độc lập và đính kèm Maven Wrapper (`mvnw`, `mvnw.cmd`).
- Khởi tạo ứng dụng chính `MamXanhApplication` tại package `tech.mamxanh`.
- Cấu hình sẵn các dependency cốt lõi:
  - **Data / Persistence:** `spring-boot-starter-data-jpa`, `mssql-jdbc` (SQL Server driver), Flyway migration (`spring-boot-starter-flyway`, `flyway-sqlserver`).
  - **Security:** `spring-boot-starter-security`.
  - **Web / Validation:** `spring-boot-starter-webmvc`, `spring-boot-starter-validation`.
  - **Documentation:** `springdoc-openapi-starter-webmvc-ui` (Swagger UI & OpenAPI 3).
  - **Productivity & Testing:** Lombok, starter test dependencies (`data-jpa-test`, `flyway-test`, `security-test`, `validation-test`, `webmvc-test`).

### Lệnh chạy và kiểm tra xác minh

```bash
# Windows
.\mvnw.cmd clean test-compile

# Linux / macOS
./mvnw clean test-compile
```

## Ranh giới kiến trúc hiện tại

- SRS là nguồn nghiệp vụ; Technology Stack là nguồn lựa chọn công nghệ; System Architecture là nguồn cho cấu trúc backend đã duyệt. **Backend Architecture: Modular Monolith using MVC/layered structure within each business module.**
- Backend gồm một Spring Boot application và một deployable backend, không tách microservices. Source code được chia theo business capability như `auth`, `recipe`, `mealplan`, `shopping`, `nutrition`, `subscription` và `admin`; bên trong mỗi module phải phân tách tối thiểu `controller`, `service`, `repository`, `model`/`entity`, cùng `dto` khi cần.
- Luồng chuẩn là `React View -> Spring MVC Controller -> Service -> Repository -> Model/Entity -> Database`. Chi tiết package cụ thể chỉ được xác lập khi scaffold và phải tuân theo ranh giới này. AI architecture sử dụng Google Gen AI Java SDK (`com.google.genai:google-genai`) với model `gemini-3.8-flash`, bọc qua `AiClient` interface abstraction; cấu hình timeout + retry cho lỗi tạm thời và không trừ quota khi AI gặp sự cố.
- SQL Server là source of truth cho dữ liệu nghiệp vụ (triển khai trên Azure SQL Database Serverless). Flyway phải quản lý migration theo thứ tự, còn JPA/Hibernate không thay thế lịch sử migration.
- Authentication baseline dùng Google Identity Services (`GoogleIdTokenVerifier` ở Backend xác thực ID Token), short-lived JWT access token, rotating refresh token trong HttpOnly Cookie, refresh session/server-side revocation và logout revocation; role/ownership, validation và quota phải được thực thi ở backend.
- Subscription baseline dùng FREE 0, PLUS 49,000 và PRO 99,000 VND/tháng; verified activation, end-of-period expiry, no auto-renew/no partial refund và idempotent duplicate payment processing. Cổng thanh toán đã chốt là **payOS** (REST API qua Spring `RestClient` + Webhook HMAC-SHA256).
- Gemini, Azure, Brevo SMTP và payOS credential chỉ đến từ cấu hình môi trường/secret store (local `.env`, GitHub Secrets, Azure App Service Settings); không commit giá trị thật. Maps chỉ được cấu hình nếu M11 được kích hoạt lại bằng quyết định scope mới.
- API được mô tả bằng OpenAPI và trả lỗi nhất quán; không để frontend suy đoán business rule.

## Definition of Done cho thay đổi backend

- Business Rule và failure case liên quan có test phù hợp.
- `mvn test` và build/package chạy thành công trên commit hiện tại sau khi Maven project tồn tại.
- Không log password, token, SAS URL hoặc dữ liệu cá nhân nhạy cảm.
- Thay đổi schema có Flyway migration append-only, kiểm tra trên database sạch và cập nhật ERD/tài liệu.
- Thay đổi API có OpenAPI, validation, authorization và ví dụ lỗi tương ứng.
