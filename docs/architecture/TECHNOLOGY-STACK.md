> **Document:** Technology Stack  
> **File:** `docs/architecture/TECHNOLOGY-STACK.md`  
> **Version:** v1.3.0
> **Created:** 2026-09-13  
> **Last Updated:** 2026-09-16
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
| Axios | Gửi HTTP request từ client đến Backend | Confirmed | Cung cấp client API nhất quán và hỗ trợ xử lý dùng chung | Quy ước authentication/error vẫn cần API design được phê duyệt; native `fetch` vẫn là phương án khác nhưng không phải baseline đã chọn |

Frontend state management và CSS/UI library vẫn là `TBD`.

## 3. Backend và giao tiếp ứng dụng

| Công nghệ | Mục đích | Trạng thái | Lý do chọn / lợi ích chính | Trade-off hoặc chi tiết chưa giải quyết |
|---|---|---|---|---|
| Java 21 | Ngôn ngữ Backend và runtime baseline | Confirmed | Java LTS baseline phù hợp với công việc Spring của nhóm trong môn học | Mọi contributor và build environment phải dùng JDK tương thích |
| Spring Boot | Xây dựng ứng dụng Backend và các integration | Confirmed | Tích hợp ecosystem web, security, validation và persistence đã chọn | Version và tập module phải được chọn đồng bộ khi tạo Maven scaffold |
| Maven | Build Backend và quản lý dependency | Confirmed | Cung cấp cấu trúc dự án và dependency lifecycle có thể tái lập, quen thuộc với nhóm | Chưa có build command hoặc dependency version nào được xác minh trước khi `pom.xml` tồn tại |
| REST API qua JSON | Contract ứng dụng giữa browser và Backend | Confirmed | Ranh giới client/server rõ ràng và định dạng payload có khả năng tương tác | Endpoint path, error envelope, pagination và versioning chưa được định nghĩa |

**Backend Architecture: Modular Monolith using MVC/layered structure within each business module.** Baseline này dùng một Spring Boot application và một deployable backend, tổ chức module theo business capability; MVC/layered structure được áp dụng bên trong từng module và không bị thay thế bởi Modular Monolith. Cấu trúc trách nhiệm và luồng chuẩn được quy định tại [System Architecture](ARCHITECTURE.md#31-kiến-trúc-backend).

## 4. Persistence

| Công nghệ | Mục đích | Trạng thái | Lý do chọn / lợi ích chính | Trade-off hoặc chi tiết chưa giải quyết |
|---|---|---|---|---|
| Microsoft SQL Server | Relational datastore cốt lõi | Confirmed | Hỗ trợ relationship, constraint và transaction có cấu trúc cho account, content, plan và traceability | Hosting, backup, credential, environment và physical schema vẫn chưa chốt |
| Spring Data JPA | Lớp abstraction cho repository/data access | Confirmed | Giảm phần code persistence lặp lại trong Spring ecosystem | Query phức tạp và transaction boundary vẫn cần được thiết kế có chủ đích |
| Hibernate | JPA implementation và object-relational mapping | Confirmed | Cung cấp runtime integration cho JPA đã chọn | Tránh schema auto-update không kiểm soát; ORM mapping không thay thế database design |
| Flyway | Version hóa các thay đổi database có thể thực thi | Confirmed | Migration có thứ tự và review được giúp các environment đồng bộ | Khả năng tương thích với SQL Server phải được chọn cùng Spring Boot; migration đã áp dụng ở môi trường dùng chung không được tùy tiện viết lại |

Các file SQL hiện tại chỉ là placeholder, chưa phải schema được phê duyệt. Migration có thẩm quyền và có thể thực thi chỉ bắt đầu trong Backend sau khi scaffold tồn tại.

## 5. Authentication và Authorization

| Công nghệ | Mục đích | Trạng thái | Lý do chọn / lợi ích chính | Trade-off hoặc chi tiết chưa giải quyết |
|---|---|---|---|---|
| Spring Security | Framework authentication và authorization của Backend | Confirmed | Điểm tích hợp trung tâm để bảo vệ luồng request Spring | Filter, access rule và cách xử lý failure vẫn cần được thiết kế và kiểm thử |
| JWT access + rotating refresh token | Mang authentication claim và duy trì refresh session có thể thu hồi | Confirmed baseline | Short-lived access token cho REST; rotating refresh token với server-side session/revocation; logout thu hồi refresh session | Expiry cụ thể, storage và reuse-detection là chi tiết thiết kế; access-token-only chỉ là fallback sau một quyết định giảm scope mới |
| BCrypt | Hash và xác minh password | Confirmed | Cơ chế hash password một chiều có salt tích hợp với Spring Security | Work factor và xử lý input phải được chọn; không được ghi password hoặc hash vào log |
| Role-based authorization và ownership check | Thực thi quyền Guest/Member/Administrator và ownership của resource | Confirmed | Làm ranh giới role dễ giải thích và kiểm thử | Chỉ kiểm tra role là chưa đủ khi Member chỉ được thay đổi resource của chính mình |

Google Login là khả năng authentication đã được SRS xác nhận; Spring integration và luồng validation token cụ thể vẫn là chi tiết triển khai.

## 6. Media và external service

| Công nghệ/dịch vụ | Mục đích | Trạng thái | Lý do chọn / lợi ích chính | Trade-off hoặc chi tiết chưa giải quyết |
|---|---|---|---|---|
| Azure Blob Storage | Lưu ảnh Recipe Post trong khi SQL Server giữ reference | Confirmed | Shared cloud object storage giúp tránh lưu binary media trong relational database | Phụ thuộc network, access policy và chi phí; không đồng nghĩa chọn Azure để deploy toàn hệ thống |
| Upload Blob qua Backend | Xác thực, validate và upload tối đa 5 ảnh Recipe Post | Confirmed initial path | Tập trung kiểm soát truy cập và validation cho triển khai ban đầu | Chỉ JPEG/PNG/WebP, tối đa 5 MB/ảnh; timeout và consistency cleanup còn thiết kế |
| Upload trực tiếp bằng scoped SAS URL | Khả năng upload từ client tới Blob khi xuất hiện nhu cầu scaling đã được đo | Future option | Có thể giảm tải truyền file qua Backend | Tăng độ phức tạp về CORS, expiry, upload chưa hoàn tất và verification; chưa có ngưỡng áp dụng |
| YouTube embed | Phát video được liên kết trong Recipe Post mà không sao chép video | Confirmed | Dùng player của provider và tránh phải vận hành video pipeline | Khả năng embed phụ thuộc setting của video nguồn và hành vi của provider |
| Google Gemini | AI provider cho các khả năng được SRS định nghĩa | Confirmed at provider level | Cho nhóm một ranh giới provider thống nhất để đánh giá và tích hợp | Model, kiến trúc chi tiết, chất lượng, latency, quota và chi phí vẫn cần kiểm thử/chốt |
| Google Maps Platform | Phụ thuộc lịch sử từng được đề xuất cho FR-42/FR-43 | `OUT_OF_SCOPE` | M11 không phục vụ trực tiếp luồng meal-planning cốt lõi và ứng dụng không quản lý dữ liệu nhà hàng bên ngoài | Không chọn dependency hoặc tích hợp trong baseline hiện tại; chỉ xem xét lại sau quyết định scope và phân rã mới |
| Payment provider | Xác minh payment thật cho Plus/Pro trước khi kích hoạt entitlement | TBD implementation | Business baseline đã chốt FREE 0, PLUS 49,000, PRO 99,000 VND/tháng | Chỉ provider/webhook còn chọn; phải hỗ trợ verified activation, expiry, no auto-renew/no partial refund và idempotency |
| Email service | Verification, reset và notification bất đồng bộ/best-effort | TBD implementation | Email failure không rollback business action; moderation-result email phải được thử gửi | Provider, deliverability, template và retry detail chưa chốt |

Credential của provider phải nằm ở Backend và ngoài Source Control.

## 7. Chất lượng, API Documentation, validation và logging

| Công nghệ | Mục đích | Trạng thái | Lý do chọn / lợi ích chính | Trade-off hoặc chi tiết chưa giải quyết |
|---|---|---|---|---|
| JUnit 5 | Viết automated test cho Backend | Confirmed | Cung cấp assertion có tính lặp lại cho hành vi Java/Spring | Unit Test không tự chứng minh database, security hoặc provider integration hoạt động đúng |
| Mockito | Cô lập collaborator của Backend trong test | Confirmed | Cho phép kiểm tra nhanh các scenario thành công/thất bại có kiểm soát mà không luôn gọi hạ tầng thật | Mock có thể khác hành vi của provider và persistence thật |
| JaCoCo | Đo phần code Java được thực thi bởi test | Confirmed | Xác định vùng chưa được kiểm thử và hỗ trợ bằng chứng review | Chưa có Coverage threshold được phê duyệt; Coverage không chứng minh chất lượng assertion |
| OpenAPI | Mô tả REST contract đã được dự án áp dụng | Confirmed standard | Cung cấp contract có cấu trúc dùng chung cho Frontend, Backend và verification | Phải bám hành vi thật; chưa tạo API document trước khi có contract thật |
| Swagger UI | Hiển thị và thử OpenAPI description | Confirmed companion tool | Giúp developer và tester dễ tiếp cận contract | Lệnh gọi tương tác không thay thế automated verification |
| springdoc-openapi | Tích hợp việc sinh OpenAPI với Spring Boot | Confirmed | Giảm nội dung lặp giữa khai báo Backend và contract documentation cơ bản | Cần version tương thích cùng description/example được viết có chủ đích |
| Jakarta Bean Validation | Biểu diễn constraint cơ bản cho input Backend | Confirmed | Cung cấp validation sớm, nhất quán và tích hợp với Spring | Không thay thế authorization, database constraint hoặc Business Rule liên quan nhiều record |
| SLF4J + Logback | Logging facade và implementation của Backend | Confirmed | Cung cấp diagnostic logging nhất quán trong Spring ecosystem | Nơi lưu, retention và centralized observability vẫn là `TBD`; không được ghi secret hoặc dữ liệu nhạy cảm vào log |

Chiến lược verification cấp dự án nằm trong [Test Strategy](../testing/TEST-STRATEGY.md). Repository hiện chưa có test hoặc test command đã được xác minh.

## 8. Các lựa chọn chưa được giải quyết rõ ràng

- Gemini model/version và kiến trúc AI chi tiết.
- MapStruct hoặc cách mapping DTO khác.
- Quy ước/thư viện Frontend state management.
- CSS/UI library và design system.
- Provider cho payment và email; business behavior/price không còn TBD.
- Dependency version và compatibility matrix.
- Deployment provider/topology cho Frontend, Backend, SQL Server và hoạt động hỗ trợ.
- Access-token expiry/storage và refresh rotation/reuse implementation; timeout/retry theo provider; logging retention ngoài AI telemetry 90 ngày; mọi Coverage quality gate.

Lựa chọn IDE cá nhân như IntelliJ IDEA Ultimate hoặc JPA tooling là developer tool tùy chọn, không phải công nghệ cốt lõi của dự án.
