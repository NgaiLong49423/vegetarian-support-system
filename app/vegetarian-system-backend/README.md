> **Document:** Backend Workspace Guide  
> **File:** `app/vegetarian-system-backend/README.md`  
> **Version:** v0.4.0
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-17
> **Status:** Active  

# Backend Workspace

Thư mục này dành cho REST API Java 21 + Spring Boot, build bằng Maven và truy cập Microsoft SQL Server qua Spring Data JPA/Hibernate theo [Technology Stack](../../docs/architecture/TECHNOLOGY-STACK.md).

## Trạng thái hiện tại

Backend đã được scaffold thành công với Java 21 và Spring Boot:
- Cấu hình Maven project độc lập và đính kèm Maven Wrapper (`mvnw`, `mvnw.cmd`).
- Khởi tạo ứng dụng chính `VegetarianSystemBackendApplication` tại package `com.vegetarian.system`.
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
- Luồng chuẩn là `React View -> Spring MVC Controller -> Service -> Repository -> Model/Entity -> Database`. Chi tiết package cụ thể chỉ được xác lập khi scaffold và phải tuân theo ranh giới này. AI architecture chi tiết vẫn chưa được chốt.
- SQL Server là source of truth cho dữ liệu nghiệp vụ. Flyway phải quản lý migration theo thứ tự, còn JPA/Hibernate không thay thế lịch sử migration.
- Authentication baseline dùng short-lived access token, rotating refresh token, refresh session/server-side revocation và logout revocation; role/ownership, validation và quota phải được thực thi ở backend.
- Subscription baseline dùng FREE 0, PLUS 49,000 và PRO 99,000 VND/tháng; verified activation, end-of-period expiry, no auto-renew/no partial refund và idempotent duplicate payment processing. Provider cụ thể còn chọn khi tích hợp.
- Gemini, Azure và payment credential chỉ đến từ cấu hình môi trường/secret store; không commit giá trị thật. Maps chỉ được cấu hình nếu M11 được kích hoạt lại bằng quyết định scope mới.
- API được mô tả bằng OpenAPI và trả lỗi nhất quán; không để frontend suy đoán business rule.

## Definition of Done cho thay đổi backend

- Business Rule và failure case liên quan có test phù hợp.
- `mvn test` và build/package chạy thành công trên commit hiện tại sau khi Maven project tồn tại.
- Không log password, token, SAS URL hoặc dữ liệu cá nhân nhạy cảm.
- Thay đổi schema có Flyway migration append-only, kiểm tra trên database sạch và cập nhật ERD/tài liệu.
- Thay đổi API có OpenAPI, validation, authorization và ví dụ lỗi tương ứng.
