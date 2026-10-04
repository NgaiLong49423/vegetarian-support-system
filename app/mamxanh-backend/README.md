> **Document:** Backend Workspace Guide  
> **File:** `app/mamxanh-backend/README.md`  
> **Version:** v0.15.1
> **Created:** 2026-06-14  
> **Last Updated:** 2026-10-04
> **Status:** Active  

# Backend Workspace

Thư mục này dành cho REST API Java 21 + Spring Boot, build bằng Maven và truy cập Microsoft SQL Server qua Spring Data JPA/Hibernate theo [Technology Stack](../../docs/architecture/TECHNOLOGY-STACK.md).

> [!IMPORTANT]
> **Quy chuẩn kiến trúc bắt buộc:** Mọi thành viên và AI agent khi triển khai mã nguồn Backend **bắt buộc phải tuân thủ nghiêm ngặt** cấu trúc package Modular Monolith, phân tầng MVC và ranh giới liên module được quy định tại [Backend Package Structure Specification](../../docs/architecture/BACKEND-PACKAGE-STRUCTURE-PROPOSAL.md). Cấm inject chéo Repository giữa các module và không trả JPA Entity trực tiếp ra REST API.

## Trạng thái hiện tại

Backend đã được scaffold thành công với Java 21 và Spring Boot:
- Cấu hình Maven project độc lập và đính kèm Maven Wrapper (`mvnw`, `mvnw.cmd`).
- Khởi tạo ứng dụng chính `MamXanhApplication` tại package `tech.mamxanh`.
- Cấu hình sẵn các dependency cốt lõi:
  - **Data / Persistence:** `spring-boot-starter-data-jpa`, `mssql-jdbc` (SQL Server driver), Flyway migration (`spring-boot-starter-flyway`, `flyway-sqlserver`).
  - **Security:** `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` (xác thực JWT Bearer bằng Nimbus).
  - **Web / Validation:** `spring-boot-starter-webmvc`, `spring-boot-starter-validation`.
  - **Documentation:** `springdoc-openapi-starter-webmvc-ui` (generated OpenAPI 3 + Swagger UI compatibility) and Scalar API Reference static browser UI.
  - **Email:** `spring-boot-starter-mail` (Brevo SMTP, gửi bất đồng bộ).
  - **Productivity & Testing:** Lombok, starter test dependencies (`data-jpa-test`, `flyway-test`, `security-test`, `validation-test`, `webmvc-test`), Testcontainers SQL Server (`spring-boot-testcontainers`, `testcontainers-mssqlserver`).
- Đã có luồng FR-03-A (Issue #5): `POST /api/v1/auth/register`, `/auth/email-verifications`, `/auth/email-verifications/resend`; lỗi trả `application/problem+json` có `code` ổn định; migration `V3__user_email_verification_token.sql`.
- FR-35 bổ sung consent bằng migration `V4__nutrition_profile_consent.sql`; FR-18 bổ sung ingredient group và kiểm tra unit bằng migration `V5__ingredient_group_and_unit_validation.sql`.
- Đã có luồng FR-03-B (Issue #6): `POST /api/v1/auth/login` phát Stateless JWT (HS256), khóa đăng nhập tạm 10 phút sau 5 lần sai liên tiếp theo tài khoản (migration `V6__user_login_throttle.sql`), và mọi request mang Bearer token đều kiểm tra `USER.account_status`.
- FR-05 (Issue #68) triển khai luồng nộp/xem lịch sử đơn Chuyên gia và Admin xét duyệt qua Backend API; phê duyệt đổi `CUSTOMER` thành `EXPERT` và ghi notification trong cùng transaction. Migration `V7__expert_application_notifications.sql` bổ sung internal target path cùng index truy vấn. Chi tiết API được tạo từ runtime OpenAPI.

### Lệnh chạy và kiểm tra xác minh

```bash
# Windows
.\mvnw.cmd clean test-compile
.\mvnw.cmd verify

# Linux / macOS
./mvnw clean test-compile
./mvnw verify
```

Chạy JUnit tests, package và coverage hard gate:

```powershell
# Windows
.\mvnw.cmd clean verify
# Linux / macOS
./mvnw clean verify
```

JaCoCo chạy `prepare-agent`, `report` rồi `check`. Overall backend `BUNDLE / LINE / COVEREDRATIO` phải **≥0.80**; dưới 80% làm Maven trả exit code khác 0 và job CI `Backend` fail. Đây không phải new-code coverage. Reports: `target/site/jacoco/index.html` và `target/site/jacoco/jacoco.xml`. Không exclude production code để pass; thêm test có assertion phù hợp theo report. Sonar đọc XML sau khi reports được truyền qua artifact, không tạo coverage hoặc thay gate này. Coverage không thay thế Acceptance Criteria, authorization hoặc database integration evidence.

`verify` chạy cả unit test và integration test. Integration test dùng Testcontainers để khởi động cùng SQL Server container image được pin với Docker Compose (tag `2019-CU32-GDR11-ubuntu-20.04`, repository manifest digest lấy từ MCR), khởi động ứng dụng qua `MamXanhApplication.main`, chạy Flyway từ V1 và kiểm tra mapping Hibernate, nên **Docker Desktop phải đang chạy**; lần đầu cần tải image dung lượng lớn. Test dùng profile `test`, không cần file `.env` và không gửi email thật. Khi thay image/digest, phải chạy integration tests thật; compile-only không xác minh DockerImageName, SQL Server startup hay Flyway.

## Yêu cầu để chạy ứng dụng

Chọn một trong hai môi trường:

- **Chạy trực tiếp:** JDK 21. Maven không cần cài riêng vì repository có Maven Wrapper.
- **Chạy bằng Docker:** Docker Desktop đang hoạt động. Không cần cài Java/Maven trực tiếp trên máy.

Cả hai cách đều cần một Microsoft SQL Server có database `MamXanhDB`. Flyway migration `V1__baseline_schema.sql` hiện tạo baseline schema từ database rỗng; Backend vẫn không thể khởi động đầy đủ nếu SQL Server chưa sẵn sàng hoặc credential/migration validation không hợp lệ. Khi chạy toàn hệ thống bằng Docker Compose ở thư mục gốc, Compose tự tạo database trước khi Backend chạy Flyway.

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

Tạo hoặc cập nhật file `app/mamxanh-backend/.env` theo contract trong `.env.example`:

```properties
SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=MamXanhDB;encrypt=true;trustServerCertificate=true
SPRING_DATASOURCE_USERNAME=YOUR_LOCAL_DB_USERNAME
SPRING_DATASOURCE_PASSWORD=YOUR_LOCAL_DB_PASSWORD
```

`.env` chỉ dùng trên máy cá nhân và không được commit. File `src/main/resources/application-local.properties` được theo dõi với placeholder an toàn và nạp `.env` bằng `spring.config.import=optional:file:.env[.properties]`; không ghi credential thật vào file này.

Các biến môi trường cho FR-03 (giá trị dùng chung lấy từ kho mật khẩu của nhóm, không dán vào Issue/PR):

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `MAMXANH_JWT_SECRET` | **bắt buộc** | Khóa ký HS256 cho access token, tối thiểu 32 byte; thiếu hoặc ngắn hơn thì Backend không khởi động. Sinh bằng lệnh ghi trong `.env.example`. Test tích hợp tự dùng secret riêng, không cần biến này. |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | trống (port `587`) | SMTP Brevo. Khi `SPRING_MAIL_HOST` trống, Backend vẫn chạy nhưng bỏ qua việc gửi email và ghi cảnh báo. |
| `MAMXANH_MAIL_FROM` | trống | Địa chỉ người gửi đã xác minh trên Brevo; trống thì cũng bỏ qua việc gửi email. |
| `MAMXANH_FRONTEND_BASE_URL` | `http://localhost:5173` | Origin dùng để tạo liên kết trong email (`/xac-minh-email?token=...`). |
| `MAMXANH_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Danh sách origin được gọi API, phân tách bằng dấu phẩy, không dùng wildcard. |

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
3. Bảo đảm `.env` có đủ các biến theo `.env.example` và cấu hình database local ở trên.
4. Mở `src/main/java/tech/mamxanh/MamXanhApplication.java`.
5. Bấm nút Run cạnh hàm `main`.

IntelliJ vẫn sử dụng cùng cấu hình Spring profile `local`; nút Run không thay thế yêu cầu SQL Server phải sẵn sàng.

## Cách 2 — Chạy toàn hệ thống bằng Docker Compose

Để chạy Frontend, Backend và SQL Server theo một cấu hình dùng chung, ưu tiên Docker Compose ở repository root. Hướng dẫn dưới đây vẫn hữu ích khi chỉ cần chạy riêng Backend.

Tại root repository, nếu chưa có file local thì sao chép `app/mamxanh-backend/.env.example` thành `app/mamxanh-backend/.env`, giữ nguyên file `.env` đang có, rồi đặt một mật khẩu SQL Server local mạnh cho `MSSQL_SA_PASSWORD`:

```powershell
if (-not (Test-Path app/mamxanh-backend/.env)) {
    Copy-Item app/mamxanh-backend/.env.example app/mamxanh-backend/.env
}
```

Khởi động stack bằng:

```powershell
$composeProject = 'mamxanh-dev'
docker compose -p $composeProject --env-file app/mamxanh-backend/.env up --build --detach --wait --wait-timeout 600
```

Lần khởi động đầu, SQL Server tạo `MamXanhDB`, Backend chạy Flyway, sau đó service `sample-data` nạp fixture mẫu từ `database/sample-data.sql` trước khi Frontend sẵn sàng. Script seed có thể chạy lại an toàn; `down` rồi `up` giữ SQL volume và dữ liệu hiện có.

Mở Frontend tại <http://localhost:5173>; Scalar API Reference chính thức tại <http://localhost:8080/scalar>; generated OpenAPI JSON tại <http://localhost:8080/v3/api-docs>. Các port chỉ bind vào loopback của máy local. Scalar tải JS asset đã pin từ jsDelivr nên browser cần truy cập CDN; spec được tải cùng origin Backend. Dừng bằng `docker compose -p $composeProject --env-file app/mamxanh-backend/.env down`; lệnh này giữ database và dependency volume. Sửa `MSSQL_SA_PASSWORD` trong `.env` không tự đổi credential đã khởi tạo trong SQL volume.

Để reset riêng database development, chạy script có xác nhận riêng. Script chỉ xóa SQL volume, sau đó Compose khởi động lại SQL Server, áp dụng Flyway và nạp lại fixture; Frontend dependency volume vẫn được giữ:

```powershell
./scripts/reset-docker-db.ps1
```

Lệnh xóa volume SQL sẽ xóa toàn bộ database local. `docker compose -p $composeProject --env-file app/mamxanh-backend/.env down --volumes` là full reset, xóa cả SQL data lẫn Frontend dependency volume; chỉ dùng nếu chấp nhận mất tất cả named-volume data. Khi dùng project override, giữ nguyên cùng tên ở mọi lệnh Compose và dùng prefix đó cho volume tương ứng.

Trong Scalar, tìm endpoint, xem schema/status/auth, nhập Bearer/JWT token thủ công khi generated runtime spec khai báo scheme và endpoint được bảo vệ, rồi gửi request tới Backend. CI chỉ xác nhận HTTP route/HTML shell `/scalar`; để nghiệm thu browser, mở trang và xác minh Scalar JS render đúng endpoint từ runtime spec, sau đó thử request phù hợp. Không ghi token thật vào log, ảnh chụp hoặc tài liệu; manual thử bằng Scalar không thay automated regression tests.

Để kiểm tra trạng thái, endpoint smoke, cấu hình local và quy tắc CI/PR, xem [Docker development và kiểm thử tích hợp](../../CONTRIBUTING.md#docker-development). Không commit `.env` hoặc chia sẻ mật khẩu. `Docker Development` trong GitHub Actions build và smoke-test stack trên mỗi PR hướng vào `develop` hoặc `main`; job cũng capture/validate/upload generated OpenAPI. Ruleset `protect-develop` được tài liệu branch ghi nhận yêu cầu check này trước merge vào `develop`.

Các lệnh Dockerfile bên dưới chỉ chạy Backend độc lập để debug; chúng không phải cách test tích hợp chuẩn và không thay thế Docker Compose chung. Dockerfile chỉ đóng gói Backend; SQL Server vẫn chạy bên ngoài container trong cách chạy độc lập này.

### 1. Build image

Mở PowerShell tại `app/mamxanh-backend`:

```powershell
docker build -t mamxanh-backend-dev .
```

### 2. Cấu hình database

Nếu SQL Server chạy trên máy Windows, đặt `SPRING_DATASOURCE_URL` trong `.env` dùng `host.docker.internal` thay vì `localhost`. Giữ các biến còn lại theo `.env.example`:

```properties
SPRING_DATASOURCE_URL=jdbc:sqlserver://host.docker.internal:1433;databaseName=MamXanhDB;encrypt=true;trustServerCertificate=true
```

Không ghi giá trị thật vào README, Dockerfile hoặc source control.

### 3. Chạy container

```powershell
docker run --rm --name mamxanh-backend `
  -p 127.0.0.1:8080:8080 `
  --env-file .env `
  mamxanh-backend-dev
```

Dừng bằng `Ctrl+C`. Container tự xóa vì dùng `--rm`. Khi source hoặc `pom.xml` thay đổi, build lại image trước khi chạy.

## Kiểm tra và xử lý lỗi thường gặp

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `java` đúng nhưng `javac` không tồn tại | Máy đang dùng JRE hoặc `PATH` trỏ sai | Cài/chọn JDK 21 và mở Terminal mới. |
| `Login failed for user` | Sai username/password hoặc SQL Login chưa được bật | Kiểm tra credential và chế độ authentication của SQL Server. |
| `Connection refused` hoặc timeout | SQL Server chưa chạy, sai port hoặc firewall chặn | Xác minh SQL Server lắng nghe TCP `1433`; trong Docker dùng `host.docker.internal`. |
| Hibernate báo thiếu bảng | Database chưa có schema mà `ddl-auto=validate` không tự tạo bảng | Khởi tạo schema/migration được dự án phê duyệt; không đổi sang `ddl-auto=update` để né lỗi. |
| Flyway validation lỗi | Migration history và source migration không khớp | Không sửa migration đã áp dụng; đối chiếu đúng database/environment trước khi tiếp tục. |
| Port `8080` đã được sử dụng | Backend/container khác đang chạy | Dừng tiến trình cũ rồi chạy lại. |
| Test báo `Could not find a valid Docker environment` | Docker Desktop chưa chạy | Mở Docker Desktop, chờ trạng thái Running rồi chạy lại `verify`. |
| Docker không nhận lệnh | Docker Desktop chưa cài, chưa chạy hoặc chưa có trong `PATH` | Mở Docker Desktop và xác minh bằng `docker version`. |

## Ranh giới kiến trúc hiện tại

- SRS là nguồn nghiệp vụ; Technology Stack là nguồn lựa chọn công nghệ; System Architecture là nguồn cho cấu trúc backend đã duyệt. **Backend Architecture: Modular Monolith using MVC/layered structure within each business module.**
- Backend gồm một Spring Boot application và một deployable backend, không tách microservices. Source code được chia theo business capability như `auth`, `recipe`, `mealplan`, `shopping`, `nutrition`, `subscription` và `admin`; bên trong mỗi module phải phân tách tối thiểu `controller`, `service`, `repository`, `model`/`entity`, cùng `dto` khi cần.
- Baseline production đã chốt triển khai Backend dưới dạng Spring Boot JAR trên **Azure App Service (Java 21 SE)**. Dockerfile trong thư mục này dùng để đồng bộ môi trường development; Docker image chưa được chọn làm production deployment artifact.
- Frontend được host riêng trên **Vercel** và gọi Backend qua HTTPS/REST API. Azure SQL Database Serverless là relational source of truth; Azure Blob Storage lưu media của Recipe Post.
- Luồng chuẩn là `React View -> Spring MVC Controller -> Service -> Repository -> Model/Entity -> Database`. Chi tiết package cụ thể chỉ được xác lập khi scaffold và phải tuân theo ranh giới này. AI architecture sử dụng Google Gen AI Java SDK (`com.google.genai:google-genai`) với model `gemini-3.8-flash`, bọc qua `AiClient` interface abstraction; cấu hình timeout + retry cho lỗi tạm thời, không ghi nhận lượt gọi thành công và không thay đổi entitlement khi AI gặp sự cố.
- SQL Server là source of truth cho dữ liệu nghiệp vụ (triển khai trên Azure SQL Database Serverless). Flyway phải quản lý migration theo thứ tự, còn JPA/Hibernate không thay thế lịch sử migration.
- Authentication baseline dùng Google Identity Services (`GoogleIdTokenVerifier` ở Backend xác thực ID Token), Stateless JWT Access Token (Resource Server Nimbus), BCrypt, kiểm tra tức thời `USER.account_status`, đăng xuất xử lý phía client; role/ownership, validation, feature entitlement và technical rate limiting (tạm khóa 10 phút cấp tài khoản sau 5 lần sai trên bảng `USER`) phải được thực thi ở backend.
- Subscription baseline dùng FREE 0, PLUS 49,000 và PRO 99,000 VND/tháng; verified activation, end-of-period expiry, no auto-renew/no partial refund và idempotent duplicate payment processing. Cổng thanh toán đã chốt là **payOS** (REST API qua Spring `RestClient` + Webhook HMAC-SHA256).
- Gemini, Azure, Brevo SMTP và payOS credential chỉ đến từ cấu hình môi trường/secret store (local `.env`, GitHub Secrets, Azure App Service Settings); không commit giá trị thật. Maps chỉ được cấu hình nếu M11 được kích hoạt lại bằng quyết định scope mới.
- API được mô tả bằng OpenAPI và trả lỗi nhất quán; không để frontend suy đoán business rule.

## Definition of Done cho thay đổi backend

- Business Rule và failure case liên quan có test phù hợp.
- `./mvnw clean verify` (Windows: `.\mvnw.cmd clean verify`) pass trên commit hiện tại, gồm tests, build/package và overall JaCoCo line coverage ≥80%.
- Không log password, token, SAS URL hoặc dữ liệu cá nhân nhạy cảm.
- Thay đổi schema có Flyway migration append-only, kiểm tra trên database sạch và cập nhật ERD/tài liệu.
- Thay đổi API có OpenAPI, validation, authorization và ví dụ lỗi tương ứng.
