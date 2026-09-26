> **Document:** Technology Stack  
> **File:** `docs/architecture/TECHNOLOGY-STACK.md`  
> **Version:** v1.6.0
> **Created:** 2026-09-13  
> **Last Updated:** 2026-09-25
> **Status:** Active  
> **Related Docs:** `docs/architecture/ARCHITECTURE.md`, `docs/requirements/SRS.md`, `docs/testing/TEST-STRATEGY.md`

# Technology Stack

## 1. Mục đích và quy ước trạng thái

Tài liệu này ghi lại các công nghệ đã được chọn cho dự án, vai trò của chúng và các trade-off quan trọng. Đây là Technology Stack baseline, không phải bằng chứng rằng dependency, scaffold, integration hoặc environment đã tồn tại.

| Trạng thái | Ý nghĩa |
|---|---|
| `Confirmed` | Đã được chọn cho dự án với trách nhiệm được nêu |
| `Future option` | Phương án đã được ghi nhận nhưng chưa thuộc implementation baseline ban đầu |
| `TBD` | Chưa có lựa chọn được phê duyệt; các ví dụ không được coi là dependency |

Hành vi nghiệp vụ chi tiết vẫn thuộc [SRS](../requirements/SRS.md). Ranh giới runtime cấp cao nằm trong [System Architecture](ARCHITECTURE.md).

## 2. Frontend

| Công nghệ | Mục đích | Trạng thái | Lý do chọn / lợi ích chính | Trade-off hoặc chi tiết chưa giải quyết |
|---|---|---|---|---|
| React | Xây dựng UI trên browser từ các component tái sử dụng | Confirmed | Phù hợp với ứng dụng tương tác và responsive; giúp tách UI thành component | React không tự quyết định routing, state management, styling hoặc quy ước component |
| TypeScript | Cung cấp source Frontend có kiểu dữ liệu | Confirmed | Làm contract rõ hơn và phát hiện nhiều lỗi phổ biến trước runtime | Type phải được duy trì và không tự validate dữ liệu runtime không tin cậy |
| Vite | Cung cấp tooling để phát triển và build Frontend | Confirmed | Cho phản hồi local nhanh và toolchain tập trung cho React/TypeScript | Script, plugin và cách xử lý environment cụ thể phải chờ scaffold thật |
| npm | Quản lý package và script của Frontend | Confirmed | Cung cấp package workflow tiêu chuẩn cho Frontend stack đã chọn | Lockfile và dependency update phải được review; chưa khẳng định command nào trước khi `package.json` tồn tại |
| Axios | Gửi HTTP request từ client đến Backend | Confirmed | Cung cấp client API nhất quán và hỗ trợ xử lý interceptor dùng chung (refresh token) | Cần cấu hình `withCredentials: true` để gửi HttpOnly cookie xuyên domain |
| Requestly Pro | Mock REST API, giả lập network delay và test các kịch bản lỗi biên | Confirmed Developer Tooling | Cho phép Frontend dev song song khi Backend chưa hoàn thiện; hỗ trợ sẵn từ GitHub Student Pack | Chạy phía client/trình duyệt, không deploy lên production |

Frontend state management (ngoài React Context) và CSS/UI library vẫn là `TBD`.

## 3. Backend và giao tiếp ứng dụng

| Công nghệ | Mục đích | Trạng thái | Lý do chọn / lợi ích chính | Trade-off hoặc chi tiết chưa giải quyết |
|---|---|---|---|---|
| Java 21 | Ngôn ngữ Backend và runtime baseline | Confirmed | Java LTS baseline, hỗ trợ Virtual Threads phục vụ tác vụ I/O bất đồng bộ | Mọi contributor và build environment phải dùng JDK tương thích |
| Spring Boot | Xây dựng ứng dụng Backend và các integration | Confirmed | Tích hợp ecosystem web, security, validation và persistence đã chọn | Version và tập module phải được chọn đồng bộ khi tạo Maven scaffold |
| Maven | Build Backend và quản lý dependency | Confirmed | Cung cấp cấu trúc dự án và dependency lifecycle có thể tái lập, quen thuộc với nhóm | Đã gắn kèm Maven Wrapper (`mvnw`, `mvnw.cmd`) |
| REST API qua JSON | Contract ứng dụng giữa browser và Backend | Confirmed | Ranh giới client/server rõ ràng và định dạng payload có khả năng tương tác | Endpoint path, error envelope, pagination và versioning theo chuẩn REST |
| Spring Boot Actuator | Cung cấp health check (`/actuator/health`) và metric vận hành | Confirmed | Tích hợp sẵn trong Spring Boot, kết nối trực tiếp với Azure App Service health probe | Cần bảo vệ các endpoint nhạy cảm, chỉ mở công khai `/health` |

**Backend Architecture: Modular Monolith using MVC/layered structure within each business module.** Baseline này dùng một Spring Boot application và một deployable backend, tổ chức module theo business capability; MVC/layered structure được áp dụng bên trong từng module và không bị thay thế bởi Modular Monolith. Cấu trúc trách nhiệm và luồng chuẩn được quy định tại [System Architecture](ARCHITECTURE.md#31-kiến-trúc-backend).

## 4. Persistence

| Công nghệ | Mục đích | Trạng thái | Lý do chọn / lợi ích chính | Trade-off hoặc chi tiết chưa giải quyết |
|---|---|---|---|---|
| Microsoft SQL Server | Relational datastore cốt lõi | Confirmed | Hỗ trợ relationship, constraint và transaction có cấu trúc cho account, content, plan và traceability | Triển khai trên Azure SQL Database Serverless; cần lưu ý độ trễ cold start khi database tự động pause |
| Spring Data JPA | Lớp abstraction cho repository/data access | Confirmed | Giảm phần code persistence lặp lại trong Spring ecosystem | Query phức tạp và transaction boundary vẫn cần được thiết kế có chủ đích |
| Hibernate | JPA implementation và object-relational mapping | Confirmed | Cung cấp runtime integration cho JPA đã chọn | Tránh schema auto-update không kiểm soát; ORM mapping không thay thế database design |
| Flyway | Version hóa các thay đổi database có thể thực thi | Confirmed | Migration có thứ tự và review được giúp các environment đồng bộ | Migration đã áp dụng ở môi trường dùng chung không được tùy tiện viết lại |

Các file SQL hiện tại chỉ là placeholder, chưa phải schema được phê duyệt. Migration có thẩm quyền và có thể thực thi chỉ bắt đầu trong Backend sau khi scaffold tồn tại.

## 5. Authentication và Authorization

| Công nghệ | Mục đích | Trạng thái | Lý do chọn / lợi ích chính | Trade-off hoặc chi tiết chưa giải quyết |
|---|---|---|---|---|
| Spring Security | Framework authentication và authorization của Backend | Confirmed | Điểm tích hợp trung tâm để bảo vệ luồng request Spring | Filter, access rule và cách xử lý failure được phân tầng chặt chẽ |
| JWT access + rotating refresh token | Mang authentication claim và duy trì refresh session có thể thu hồi | Confirmed baseline | Short-lived access token cho REST; rotating refresh token với server-side session/revocation qua HttpOnly Cookie | Cookie cross-domain cần `SameSite=None; Secure` và `Access-Control-Allow-Credentials: true` |
| Google Identity Services (GIS) | Xác thực tài khoản Google 1-click phía Frontend | Confirmed | Trả về Google ID Token an toàn, giảm độ phức tạp so với Authorization Code flow | Phụ thuộc thư viện Google Identity phía client (dùng `@react-oauth/google`) |
| Google API Client (`GoogleIdTokenVerifier`) | Xác thực Google ID Token tại Backend | Confirmed | Kiểm tra chữ ký số, issuer, audience và expiry với Google JWKS chính thức; trích xuất `google_subject` (`sub`) làm khóa định danh | Thêm dependency `com.google.api-client:google-api-client` vào `pom.xml` |
| BCrypt | Hash và xác minh password | Confirmed | Cơ chế hash password một chiều có salt tích hợp với Spring Security | Work factor và xử lý input phải được chọn; không được ghi password hoặc hash vào log |
| Role-based authorization và ownership check | Thực thi quyền Guest/Member/Administrator và ownership của resource | Confirmed | Làm ranh giới role dễ giải thích và kiểm thử | Chỉ kiểm tra role là chưa đủ khi Member chỉ được thay đổi resource của chính mình |

## 6. Media, AI và external service

| Công nghệ/dịch vụ | Mục đích | Trạng thái | Lý do chọn / lợi ích chính | Trade-off hoặc chi tiết chưa giải quyết |
|---|---|---|---|---|
| Azure Blob Storage | Lưu ảnh Recipe Post trong khi SQL Server giữ reference | Confirmed | Shared cloud object storage giúp tránh lưu binary media trong relational database | Giới hạn tối đa 5 ảnh/bài viết, chỉ nhận JPEG/PNG/WebP $\le 5$ MB |
| Upload Blob qua Backend | Xác thực, validate và upload ảnh Recipe Post | Confirmed initial path | Tập trung kiểm soát truy cập và validation cho triển khai ban đầu | Cần cấu hình timeout và dọn dẹp file rác khi tạo bài viết thất bại |
| Upload trực tiếp bằng scoped SAS URL | Khả năng upload từ client tới Blob khi xuất hiện nhu cầu scaling đã được đo | Future option | Có thể giảm tải truyền file qua Backend | Tăng độ phức tạp về CORS, expiry, upload chưa hoàn tất và verification; chưa có ngưỡng áp dụng |
| YouTube embed | Phát video được liên kết trong Recipe Post mà không sao chép video | Confirmed | Dùng player của provider và tránh phải vận hành video pipeline | Khả năng embed phụ thuộc setting của video nguồn và hành vi của provider |
| Google Gemini (`gemini-3.8-flash`) | AI model phục vụ gợi ý món ăn, thực đơn và hỗ trợ giải đáp | Confirmed model | Model Flash mới, tốc độ phản hồi nhanh, hỗ trợ reasoning và native JSON Structured Outputs | Khóa cứng model ID `gemini-3.8-flash`, không dùng alias `latest` để đảm bảo tính ổn định |
| Google Gen AI Java SDK (`com.google.genai:google-genai`) | SDK chính thức gọi Gemini API từ Backend | Confirmed integration | Thuần Java 21, không cần dựng Python microservice hoặc dùng framework AI nặng | Bọc qua interface `AiClient` để dễ dàng mock trong Unit/Integration Test |
| Google Maps Platform | Phụ thuộc lịch sử từng được đề xuất cho FR-42/FR-43 | `OUT_OF_SCOPE` | M11 không phục vụ trực tiếp luồng meal-planning cốt lõi và ứng dụng không quản lý dữ liệu nhà hàng bên ngoài | Không chọn dependency hoặc tích hợp trong baseline hiện tại |
| payOS | Cổng thanh toán VietQR cho gói PLUS (49.000) và PRO (99.000) | Confirmed provider | Hỗ trợ thanh toán VND qua VietQR, link thanh toán, webhook tức thì và sandbox miễn phí | Tích hợp qua Spring `RestClient`; webhook yêu cầu verify chữ ký HMAC-SHA256 và xử lý idempotent theo `order_code` |
| Brevo (Sendinblue) | Transactional email service cho verify account, reset password và thông báo | Confirmed provider | Gói miễn phí 300 email/ngày, hỗ trợ SMTP chuẩn; tích hợp qua `spring-boot-starter-mail` | Xử lý bất đồng bộ qua `@Async`; lỗi email không rollback transaction nghiệp vụ |

Credential của provider phải nằm ở Backend và ngoài Source Control.

## 7. Chất lượng, API Documentation, validation, logging và observability

| Công nghệ | Mục đích | Trạng thái | Lý do chọn / lợi ích chính | Trade-off hoặc chi tiết chưa giải quyết |
|---|---|---|---|---|
| JUnit 5 | Viết automated test cho Backend | Confirmed | Cung cấp assertion có tính lặp lại cho hành vi Java/Spring | Unit Test không tự chứng minh database, security hoặc provider integration hoạt động đúng |
| Mockito | Cô lập collaborator của Backend trong test | Confirmed | Cho phép kiểm tra nhanh các scenario thành công/thất bại có kiểm soát mà không luôn gọi hạ tầng thật | Mock `AiClient`, `JavaMailSender`, `payOSClient` |
| JaCoCo | Đo phần code Java được thực thi bởi test | Confirmed | Tạo báo cáo coverage XML cho CI pipeline | Cần cấu hình exclude cho các DTO/Entity/Config không chứa logic |
| Codecov | Báo cáo và hiển thị trực quan coverage trên GitHub PR | Confirmed QA Tooling | Tự động phân tích và comment tỷ lệ coverage vào PR; có sẵn từ GitHub Student Pack | Cấu hình qua GitHub Action `codecov/codecov-action` |
| Testmail | Hộp thư ảo phục vụ kiểm thử tự động email | Confirmed QA Tooling | Cung cấp vô hạn địa chỉ email test qua API, không tốn quota Brevo thật; hỗ trợ từ GitHub Student Pack | Dành cho môi trường kiểm thử/staging |
| OpenAPI | Mô tả REST contract đã được dự án áp dụng | Confirmed standard | Cung cấp contract có cấu trúc dùng chung cho Frontend, Backend và verification | Phải bám hành vi thật; chưa tạo API document trước khi có contract thật |
| Swagger UI | Hiển thị và thử OpenAPI description | Confirmed companion tool | Giúp developer và tester dễ tiếp cận contract | Lệnh gọi tương tác không thay thế automated verification |
| springdoc-openapi | Tích hợp việc sinh OpenAPI với Spring Boot | Confirmed | Giảm nội dung lặp giữa khai báo Backend và contract documentation cơ bản | Đã có sẵn trong `pom.xml` |
| Jakarta Bean Validation | Biểu diễn constraint cơ bản cho input Backend | Confirmed | Cung cấp validation sớm, nhất quán và tích hợp với Spring | Không thay thế authorization hoặc Business Rule phức tạp |
| SLF4J + Logback | Logging facade và implementation của Backend | Confirmed | Cung cấp diagnostic logging nhất quán trong Spring ecosystem | Tuyệt đối không ghi secret, mật khẩu hoặc dữ liệu cá nhân vào log |
| Azure Application Insights | Giám sát hiệu năng và lỗi ứng dụng trên production | Confirmed Observability | Tích hợp qua App Service Java Agent / OpenTelemetry, theo dõi response time, dependency calls và exceptions | Không yêu cầu dựng server Prometheus/Grafana hay ELK riêng |

## 8. Docker cho môi trường phát triển

| Thành phần | Mục đích | Trạng thái | Lý do chọn / lợi ích chính | Trade-off hoặc chi tiết chưa giải quyết |
|---|---|---|---|---|
| Docker Engine | Chạy môi trường Frontend và Backend có thể tái lập trên máy thành viên | Confirmed Developer Tooling | Giảm chênh lệch phiên bản runtime và công cụ giữa các máy; contributor chỉ cần Docker để chạy image đã định nghĩa | Không thay thế việc quản lý dependency/lockfile và vẫn cần rebuild image khi dependency thay đổi |
| Frontend Dockerfile | Định nghĩa môi trường phát triển/build của React, TypeScript và Vite tại `app/mamxanh-frontend/Dockerfile` | Confirmed placement | Giữ cấu hình Frontend gần source và cho phép thay đổi độc lập với Backend | Dùng để đồng bộ local development; không mặc định yêu cầu nền tảng hosting Frontend phải chạy container |
| Backend Dockerfile | Định nghĩa môi trường build/chạy Java 21, Maven và Spring Boot tại `app/mamxanh-backend/Dockerfile` | Confirmed placement | Giữ cấu hình Backend gần source và cho phép thay đổi độc lập với Frontend | Chưa xác nhận Docker image này là production deployment artifact |

## 9. Hạ tầng triển khai và quản lý Secret (Deployment & Secrets)

| Thành phần | Công nghệ / Nền tảng | Trạng thái | Ghi chú và Ràng buộc |
|---|---|---|---|
| Frontend Hosting | Vercel | Confirmed | Host ứng dụng React build bằng Vite, cung cấp CDN và HTTPS; baseline hosting không tự xác nhận Git auto-deploy hoặc CI/CD đã được cấu hình |
| Backend Hosting | Azure App Service (Java 21 SE) | Confirmed | Chạy ứng dụng Spring Boot JAR độc lập; hỗ trợ Auto-healing, Managed TLS và tích hợp Application Insights |
| Database Hosting | Azure SQL Database (Serverless) | Confirmed | Microsoft SQL Server đám mây; tự động pause khi không hoạt động để tối ưu chi phí (cần kích hoạt trước khi demo) |
| Media Storage | Azure Blob Storage | Confirmed | Lưu trữ ảnh bài đăng công thức nấu ăn |
| Custom Domain | Domain `.tech` | Confirmed Polish | Có thể cấu hình custom domain cho Vercel; tên miền cụ thể và DNS record sẽ được chốt khi chuẩn bị live demo |
| Secrets - Local | Environment Variables / `.env` | Confirmed | Biến môi trường hệ thống; cam kết không commit file `.env`, duy trì `.env.example` mẫu |
| Secrets - CI/CD | GitHub Environment Secrets | Confirmed | Quản lý token triển khai, Sonar/Codecov token và build secrets trong GitHub Actions |
| Secrets - Production | Azure App Service App Settings / Key Vault | Confirmed | Cấu hình trực tiếp trên Azure Portal hoặc nạp qua Azure Key Vault reference |

## 10. Các lựa chọn chưa được giải quyết rõ ràng (Open Items)

- Quy ước/thư viện Frontend state management (nếu vượt quá React Context).
- CSS/UI library và design system (Tailwind CSS, MUI, Ant Design hoặc Shadcn).
- MapStruct hoặc phương pháp mapping Entity $\leftrightarrow$ DTO.
- Tên miền cụ thể được chọn (.tech hay Name.com) khi bước vào giai đoạn demo.
- Điều chỉnh thời gian auto-pause của Azure SQL Database Serverless trong tuần báo cáo đồ án.
