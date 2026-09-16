> **Document:** System Architecture  
> **File:** `docs/architecture/ARCHITECTURE.md`  
> **Version:** v1.3.0
> **Created:** 2026-09-13  
> **Last Updated:** 2026-09-16
> **Status:** Active  
> **Related Docs:** `docs/requirements/SRS.md`, `docs/architecture/TECHNOLOGY-STACK.md`

# System Architecture

## 1. Mục đích và ranh giới bằng chứng

Tài liệu này mô tả cấu trúc cấp cao đã được xác nhận của Vegetarian Support System. Tài liệu không khẳng định ứng dụng đã được triển khai: repository hiện chưa có scaffold Frontend/Backend, build manifest, database schema có thể thực thi, environment đã deploy hoặc luồng runtime đã được xác minh.

Hành vi nghiệp vụ chi tiết thuộc [SRS](../requirements/SRS.md). Các lựa chọn công nghệ và trade-off thuộc [Technology Stack](TECHNOLOGY-STACK.md). Tài liệu này chủ động không quy định package, class, table, endpoint path hoặc deployment provider.

## 2. Bối cảnh hệ thống

Hệ thống là web application responsive bằng tiếng Việt, được sử dụng qua browser. Tương tác của Guest, Member và Administrator đi qua ranh giới client công khai vào Backend ứng dụng. Backend chịu trách nhiệm thực thi quy tắc nghiệp vụ và làm trung gian cho toàn bộ persistence cũng như các lệnh gọi external service cần đặc quyền.

```text
Guest / Member / Administrator
             |
             v
       Browser + React client
             |
        REST/JSON over HTTPS
             |
             v
       Spring Boot backend
        |       |       |
        v       v       v
   SQL Server  Azure   External providers
               Blob    Gemini / YouTube / auth / email / payment
```

Sơ đồ trên thể hiện trách nhiệm, không phải deployment topology. Hosting, network layout và cách tách environment cụ thể vẫn là open item.

## 3. Các thành phần runtime chính

| Thành phần | Trách nhiệm đã xác nhận | Ranh giới |
|---|---|---|
| Browser client | Hiển thị React UI responsive, nhận input của người dùng, thể hiện trạng thái loading/error/quota và phát YouTube embed được hỗ trợ | Không phải ranh giới tin cậy cho authorization, validation hoặc lưu secret |
| Spring Boot Backend | Xác thực request, kiểm tra role và ownership, validate input, áp dụng quota AI, điều phối luồng nghiệp vụ, truy cập persistence và gọi provider cần đặc quyền | Quy tắc nghiệp vụ vẫn phải được thực thi ở server-side kể cả khi UI đã ẩn action không được phép |
| SQL Server | Relational Source of Truth chính cho record, relationship, state của ứng dụng và reference đến external media | Blob object và dữ liệu của third-party provider không được thay thế bằng record do ứng dụng sao chép hoặc bịa ra |
| Azure Blob Storage | Lưu ảnh Recipe Post; SQL Server giữ metadata/reference của ứng dụng | Phase 1 không upload file video; chọn Blob Storage không đồng nghĩa chọn Azure làm deployment provider cho ứng dụng |
| External integration | Cung cấp phản hồi AI, hỗ trợ authentication/email, video nhúng và xác minh payment | Lỗi provider, quota và chi tiết triển khai chưa xác nhận phải được thể hiện rõ, không được trình bày như dữ liệu ứng dụng đã xử lý thành công; Google Maps/M11 không thuộc baseline kiến trúc hiện tại |

### 3.1 Kiến trúc Backend

**Decision status:** `DEC-016 — A — APPROVED`.

**Backend Architecture: Modular Monolith using MVC/layered structure within each business module.**

Backend là một Spring Boot application duy nhất và được phát hành thành một deployable backend; dự án không tách các business capability thành microservice. Source code được tổ chức theo business capability, chẳng hạn `auth`, `recipe`, `mealplan`, `shopping`, `nutrition`, `subscription` và `admin`. Đây là các ví dụ định hướng cho ranh giới module, không phải danh sách package bắt buộc đã scaffold.

Bên trong mỗi business module phải áp dụng MVC/layered structure và thể hiện rõ tối thiểu các trách nhiệm `controller`, `service`, `repository`, `model`/`entity`, cùng `dto` khi cần. Modular Monolith xác định ranh giới nghiệp vụ; MVC/layered structure xác định cách phân tầng trách nhiệm bên trong từng ranh giới đó. Hai nguyên tắc được áp dụng đồng thời.

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

1. Browser gửi request REST/JSON tới Spring Boot Backend.
2. Backend thực hiện authentication, authorization, ownership check và kiểm tra input theo SRS.
3. Backend đọc hoặc thay đổi state trong SQL Server và gọi external provider khi cần.
4. Backend trả kết quả thành công hoặc lỗi validation, authorization, quota hay provider rõ ràng để client hiển thị.

OpenAPI tooling đã chọn sẽ mô tả contract thật sau khi endpoint tồn tại. Tài liệu Architecture này không tự tạo endpoint giả định.

### 4.2 Luồng ảnh Recipe Post

```text
React client -> Spring Boot validation/authorization -> Azure Blob Storage
                                           |
                                           -> SQL Server metadata/reference
```

Luồng ban đầu giữ authorization cho upload và kiểm tra file tại Backend. Upload trực tiếp từ browser bằng SAS URL có scope hẹp là `Future option`, chưa thuộc kiến trúc ban đầu đã xác nhận.

### 4.3 Luồng YouTube

Recipe Post lưu link YouTube hoặc video identifier được hỗ trợ. Browser nhúng YouTube player; ứng dụng không upload, sao chép hoặc transcode file video YouTube.

### 4.4 Luồng Gemini

```text
Browser request -> backend eligibility/quota checks -> Gemini
                                          |
                         valid response -> result + usage event
                         failure        -> classified error, no consumed user call
```

Backend quản lý Gemini credential, điều kiện hợp lệ của request, quota cấp ứng dụng, validation phản hồi, thống kê thành công/thất bại và usage telemetry. Gemini không kết nối trực tiếp SQL Server, không tự publish nội dung, không tạo dữ liệu dinh dưỡng chính thức và không tự áp dụng moderation action. Gemini model cụ thể và kiến trúc AI chi tiết vẫn là `TBD`.

### 4.5 Capability Google Maps (`OUT_OF_SCOPE`)

FR-42/FR-43, M11 và tích hợp Google Maps là `OUT_OF_SCOPE`. Ứng dụng không quản lý hoặc xác minh dữ liệu nhà hàng bên ngoài; Google Maps không phải dependency của baseline kiến trúc hiện tại. Mô tả cũ chỉ được giữ trong SRS để bảo toàn lịch sử và không tạo implementation hoặc test scope.

### 4.6 Luồng payment

MVP dùng VND và chu kỳ tháng: FREE 0, PLUS 49,000, PRO 99,000 VND/tháng. Kích hoạt Plus/Pro yêu cầu payment thành công được Backend xác minh; entitlement hết hạn cuối kỳ đã trả. Không auto-renew, không partial refund; duplicate payment event phải được xử lý idempotent. Provider, callback, data model và protocol cụ thể được chọn ở thiết kế tích hợp nhưng không được thay đổi các business rules này.

## 5. Ranh giới tin cậy và bảo mật

- Browser là ranh giới không tin cậy. Việc ẩn control không đáp ứng authorization.
- Xử lý password, kiểm tra role/ownership, quyết định quota và credential của provider thuộc Backend.
- Secret của Gemini, Azure, Google authentication, email và payment phải nằm ở server-side và ngoài Source Control.
- Authentication baseline dùng short-lived JWT access token, rotating refresh token và refresh session/server-side revocation; logout thu hồi refresh session. Thời lượng cụ thể, storage mechanism và rotation/reuse-detection implementation là chi tiết thiết kế. Access-token-only chỉ là fallback nếu có quyết định giảm scope mới, không đồng thời là baseline.
- Input từ browser hoặc provider phải được validate. Log không được làm lộ password, token, API key, SAS URL, toàn bộ prompt nhạy cảm hoặc dữ liệu nutrition profile riêng tư.
- Kết quả AI từ bên ngoài là output của provider. Không được âm thầm biến chúng thành dữ liệu dinh dưỡng đã xác minh, tư vấn y tế hoặc quyết định moderation.

## 6. Ràng buộc kiến trúc

- Giữ ranh giới giữa React client, Spring Boot Backend và SQL Server trừ khi có quyết định thay đổi đã được phê duyệt.
- Giữ relational database làm Source of Truth cho ứng dụng; dùng Flyway để quản lý thay đổi schema có thể thực thi sau khi Backend scaffold tồn tại.
- Các module và table trong database phải bám sát User Stories và Business Rules đã duyệt.
- Không suy diễn microservices, vector database, RAG, tool-calling, AI framework hoặc cloud deployment topology chỉ từ các provider đã chọn.

## 7. Các vấn đề kiến trúc còn mở

| Vấn đề | Trạng thái hiện tại | Điều kiện ra quyết định |
|---|---|---|
| Gemini model và thiết kế AI | TBD | Chọn sau khi đánh giá chất lượng, latency, quota và chi phí dựa trên requirement |
| Payment provider | TBD kỹ thuật | Chọn provider/callback phù hợp với giá VND, monthly billing, verified activation, expiry, no auto-renew/no partial refund và idempotency đã chốt |
| Token implementation detail | TBD kỹ thuật | Chọn expiry cụ thể, storage và rotation/reuse handling trong baseline access + rotating refresh + server revocation |
| Deployment topology | TBD | Chốt sau khi có scaffold FE/BE/database chạy được và nhu cầu environment thực tế |
| Chính sách timeout/retry theo provider | TBD kỹ thuật | Thiết kế có thể kiểm thử; email best-effort không rollback business action và AI failure không trừ quota |
| Tích hợp Google Maps (quán chay) | `OUT_OF_SCOPE` | Không thuộc baseline kiến trúc; chỉ xem xét lại sau quyết định scope và phân rã yêu cầu mới |
| Module Blog cộng đồng nhúng công thức | Out of MVP Scope | Được phân rã tại SRS 3.21; chỉ xem xét kiến trúc sau khi các module cốt lõi hoàn thành |
| Upload trực tiếp lên Azure | Future option | Chỉ xem xét lại khi số liệu về kích thước file/tải cho thấy luồng upload qua Backend không phù hợp |

Các lựa chọn dài hạn làm thay đổi đáng kể những ranh giới này nên được ghi lại theo quy trình Decision Record của repository. Quyết định về cách làm việc nhóm hiện nằm trong [ADR-001](../decisions/001-team-workflow.md) và [ADR-002](../decisions/002-five-member-team-operating-agreement.md); hai tài liệu này không định nghĩa kiến trúc runtime.
