> **Document:** Backend Workspace Guide  
> **File:** `app/mamxanh-backend/README.md`  
> **Version:** v0.7.0
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-25
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

## Yêu cầu để chạy ứng dụng

Chọn một trong hai môi trường:

- **Chạy trực tiếp:** JDK 21. Maven không cần cài riêng vì repository có Maven Wrapper.
- **Chạy bằng Docker:** Docker Desktop đang hoạt động. Không cần cài Java/Maven trực tiếp trên máy.

Cả hai cách đều cần một Microsoft SQL Server có database `MamXanhDB` và schema phù hợp. Repository hiện chưa có bộ Flyway migration hoàn chỉnh để tự tạo toàn bộ schema từ database rỗng; vì vậy Backend có thể compile thành công nhưng không thể khởi động đầy đủ nếu database chưa sẵn sàng.

Không commit username, password, connection string thật hoặc file cấu hình local chứa credential.

Khi mở toàn hệ thống, khởi động theo thứ tự:

1. Microsoft SQL Server và database `MamXanhDB`.
2. Backend tại port `8080`.
3. [Frontend](../mamxanh-frontend/README.md) tại port `5173`.

## Cách 1 — Chạy trực tiếp bằng Java 21

### 1. Xác minh Java

```powershell
java -version
javac -version
```

Cả hai lệnh phải hiển thị Java 21. Có `java` nhưng không có `javac` nghĩa là máy chưa dùng đầy đủ JDK.

### 2. Chuẩn bị cấu hình database local

Sao chép file mẫu thành file cấu hình local:

```powershell
Copy-Item `
  'src/main/resources/application-local.properties.example' `
  'src/main/resources/application-local.properties'
```

Mở `application-local.properties` và thay ba giá trị mẫu bằng cấu hình SQL Server trên máy của bạn:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=MamXanhDB;encrypt=true;trustServerCertificate=true
spring.datasource.username=YOUR_LOCAL_DB_USERNAME
spring.datasource.password=YOUR_LOCAL_DB_PASSWORD
```

File chứa credential chỉ dùng trên máy cá nhân và không được commit.

### 3. Compile và chạy

Mở PowerShell tại `app/mamxanh-backend`:

```powershell
.\mvnw.cmd clean test-compile
.\mvnw.cmd spring-boot:run
```

Backend dùng port mặc định `8080`. Dấu hiệu khởi động thành công là log Spring Boot có dòng `Started MamXanhApplication` và không có lỗi kết nối SQL Server/Flyway/Hibernate. Dừng bằng `Ctrl+C`.

Sau lần tải dependency đầu tiên, các lần mở dự án tiếp theo chỉ cần bảo đảm SQL Server đang chạy rồi dùng:

```powershell
.\mvnw.cmd spring-boot:run
```

### Chạy bằng nút Run trong IntelliJ IDEA

1. Mở thư mục `app/mamxanh-backend` bằng IntelliJ IDEA.
2. Chọn **Project SDK = Java 21** và chờ Maven import dependency xong.
3. Tạo `application-local.properties` theo bước cấu hình database ở trên.
4. Mở `src/main/java/tech/mamxanh/MamXanhApplication.java`.
5. Bấm nút Run cạnh hàm `main`.

IntelliJ vẫn sử dụng cùng cấu hình Spring profile `local`; nút Run không thay thế yêu cầu SQL Server phải sẵn sàng.

## Cách 2 — Chạy bằng Docker

Dockerfile chỉ đóng gói Backend; SQL Server vẫn chạy bên ngoài container theo quyết định hiện tại.

### 1. Build image

Mở PowerShell tại `app/mamxanh-backend`:

```powershell
docker build -t mamxanh-backend-dev .
```

### 2. Cấu hình database cho phiên Terminal hiện tại

Nếu SQL Server chạy trên máy Windows, container phải kết nối qua `host.docker.internal` thay vì `localhost`:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://host.docker.internal:1433;databaseName=MamXanhDB;encrypt=true;trustServerCertificate=true'
$env:SPRING_DATASOURCE_USERNAME = 'YOUR_LOCAL_DB_USERNAME'
$env:SPRING_DATASOURCE_PASSWORD = 'YOUR_LOCAL_DB_PASSWORD'
```

Không ghi giá trị thật vào README, Dockerfile hoặc source control.

### 3. Chạy container

```powershell
docker run --rm --name mamxanh-backend `
  -p 8080:8080 `
  -e SPRING_DATASOURCE_URL `
  -e SPRING_DATASOURCE_USERNAME `
  -e SPRING_DATASOURCE_PASSWORD `
  mamxanh-backend-dev
```

Dừng bằng `Ctrl+C`. Container tự xóa vì dùng `--rm`. Khi source hoặc `pom.xml` thay đổi, build lại image trước khi chạy.

Xóa credential khỏi phiên PowerShell sau khi hoàn tất:

```powershell
Remove-Item Env:SPRING_DATASOURCE_URL
Remove-Item Env:SPRING_DATASOURCE_USERNAME
Remove-Item Env:SPRING_DATASOURCE_PASSWORD
```

## Kiểm tra và xử lý lỗi thường gặp

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `java` đúng nhưng `javac` không tồn tại | Máy đang dùng JRE hoặc `PATH` trỏ sai | Cài/chọn JDK 21 và mở Terminal mới. |
| `Login failed for user` | Sai username/password hoặc SQL Login chưa được bật | Kiểm tra credential và chế độ authentication của SQL Server. |
| `Connection refused` hoặc timeout | SQL Server chưa chạy, sai port hoặc firewall chặn | Xác minh SQL Server lắng nghe TCP `1433`; trong Docker dùng `host.docker.internal`. |
| Hibernate báo thiếu bảng | Database chưa có schema mà `ddl-auto=validate` không tự tạo bảng | Khởi tạo schema/migration được dự án phê duyệt; không đổi sang `ddl-auto=update` để né lỗi. |
| Flyway validation lỗi | Migration history và source migration không khớp | Không sửa migration đã áp dụng; đối chiếu đúng database/environment trước khi tiếp tục. |
| Port `8080` đã được sử dụng | Backend/container khác đang chạy | Dừng tiến trình cũ rồi chạy lại. |
| Docker không nhận lệnh | Docker Desktop chưa cài, chưa chạy hoặc chưa có trong `PATH` | Mở Docker Desktop và xác minh bằng `docker version`. |

## Ranh giới kiến trúc hiện tại

- SRS là nguồn nghiệp vụ; Technology Stack là nguồn lựa chọn công nghệ; System Architecture là nguồn cho cấu trúc backend đã duyệt. **Backend Architecture: Modular Monolith using MVC/layered structure within each business module.**
- Backend gồm một Spring Boot application và một deployable backend, không tách microservices. Source code được chia theo business capability như `auth`, `recipe`, `mealplan`, `shopping`, `nutrition`, `subscription` và `admin`; bên trong mỗi module phải phân tách tối thiểu `controller`, `service`, `repository`, `model`/`entity`, cùng `dto` khi cần.
- Baseline production đã chốt triển khai Backend dưới dạng Spring Boot JAR trên **Azure App Service (Java 21 SE)**. Dockerfile trong thư mục này dùng để đồng bộ môi trường development; Docker image chưa được chọn làm production deployment artifact.
- Frontend được host riêng trên **Vercel** và gọi Backend qua HTTPS/REST API. Azure SQL Database Serverless là relational source of truth; Azure Blob Storage lưu media của Recipe Post.
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
