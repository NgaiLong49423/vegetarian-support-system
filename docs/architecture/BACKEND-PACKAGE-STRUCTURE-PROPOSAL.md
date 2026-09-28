> **Document:** Backend Package Structure Specification
> **File:** `docs/architecture/BACKEND-PACKAGE-STRUCTURE-PROPOSAL.md`
> **Version:** v1.0.0
> **Created:** 2026-09-20
> **Last Updated:** 2026-09-28
> **Status:** Active
> **Related Docs:** `docs/architecture/ARCHITECTURE.md`, `docs/architecture/TECHNOLOGY-STACK.md`, `app/mamxanh-backend/README.md`

# Quy chuẩn cấu trúc package Backend (Backend Package Structure Specification)

## 1. Mục đích và phạm vi áp dụng

Tài liệu này xác lập quy chuẩn cấu trúc package và tổ chức mã nguồn chính thức cho Backend Mâm Xanh. Đây là **quy chuẩn kiến trúc đã được phê duyệt (`Status: Active`)**, cụ thể hóa mô hình **Modular Monolith** kết hợp MVC/layered structure theo [System Architecture](ARCHITECTURE.md#31-kiến-trúc-backend) và [Technology Stack](TECHNOLOGY-STACK.md).

Quy chuẩn này là kim chỉ nam bắt buộc cho tất cả thành viên khi triển khai các vertical slice. Mã nguồn sẽ được tạo lập dần theo từng tính năng/Issue thực tế; nhóm không tạo trước các package rỗng hoặc abstraction khi chưa có nhu cầu sử dụng thực tế.

## 2. Nguyên tắc tổ chức cốt lõi

- **Ưu tiên Business Capability:** Phân chia package cấp cao nhất theo nghiệp vụ (`auth`, `recipe`, `mealplan`, `shopping`, `nutrition`, `subscription`, `admin`), sau đó mới phân chia layer bên trong từng module.
- **Áp dụng MVC/Layered bên trong module:** Mỗi module tự đóng gói các thành phần trách nhiệm: `controller`, `service`, `repository`, `entity`, `dto`, và `mapper` (nếu cần).
- **Ranh giới liên module nghiêm ngặt (Inter-module Boundary):** Module A tuyệt đối không inject hoặc truy cập `Repository` của Module B. Phối hợp nghiệp vụ liên module bắt buộc phải thông qua public `Service` của module đích.
- **Cách ly hợp đồng giao tiếp (DTO vs Entity):** DTO request/response là hợp đồng dữ liệu ở biên HTTP; không trả trực tiếp JPA Entity ra cho client/Frontend.
- **Tập trung nghiệp vụ tại Service:** Toàn bộ business rules, điều phối transaction (`@Transactional`), kiểm tra quyền (authorization) và quyền sở hữu (ownership) được xử lý tại tầng Service.
- **Cô lập tích hợp dịch vụ ngoài (`integration`):** Các SDK và client gọi dịch vụ bên ngoài (Gemini AI, Azure Blob, payOS, Brevo, Google GIS) được đóng gói riêng biệt, không để lộ chi tiết hạ tầng vào business service.
- **Tiêu chuẩn dùng chung (`common`):** Chỉ đưa vào `common` những thành phần hạ tầng/tiện ích dùng chung đã có consumer thực tế (exception handling tập trung, envelope response, cross-cutting configs).

## 3. Cấu trúc package quy chuẩn

```text
src/main/java/tech/mamxanh/
├── MamXanhApplication.java
├── common/
│   ├── config/              # SecurityConfig, CorsConfig, OpenApiConfig, AsyncConfig
│   ├── exception/           # GlobalExceptionHandler, AppException, ErrorCode
│   ├── response/            # ApiResponse<T>, PageResponse<T>
│   └── validation/          # Custom validators / annotations dùng chung
├── auth/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── mapper/              # (Tùy chọn) Mapping Auth DTO <-> Entity
│   └── security/            # JwtTokenProvider, JwtAuthFilter, GoogleTokenVerifier
├── recipe/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   └── mapper/
├── mealplan/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│       ├── request/
│       └── response/
├── shopping/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│       ├── request/
│       └── response/
├── nutrition/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│       ├── request/
│       └── response/
├── subscription/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│       ├── request/
│       └── response/
├── admin/
│   ├── controller/
│   ├── service/
│   └── dto/
│       ├── request/
│       └── response/
└── integration/
    ├── ai/                  # Google Gen AI Java SDK (Gemini 3.8 Flash), AiClient
    ├── storage/             # Azure Blob Storage Client (Upload & quản lý ảnh)
    ├── payment/             # payOS VietQR Client, Webhook signature verification
    ├── email/               # Brevo SMTP / JavaMailSender
    └── google/              # Google Identity Services verification helper
```

*Lưu ý:* Các subpackage được tạo khi có vertical slice tương ứng. Module `admin` đóng vai trò điều phối quản trị, tái sử dụng service của các module khác và không nhất thiết phải tạo lại Entity trùng lặp.

## 4. Trách nhiệm của từng layer và chuẩn thành phần

### 4.1 Phân định trách nhiệm các layer

| Package / Layer | Trách nhiệm chính | Ràng buộc kiến trúc |
|---|---|---|
| `controller` | Tiếp nhận HTTP request, validate format ở biên (`@Valid`), ủy quyền xử lý cho Service và đóng gói trả về `ApiResponse<T>`. | Không chứa business logic; không gọi trực tiếp `Repository`. |
| `service` | Điều phối use case, transaction (`@Transactional`), thực thi Business Rules, kiểm tra authorization/ownership và tích hợp liên module. | Không phụ thuộc vào servlet request/response của HTTP. |
| `repository` | Thao tác dữ liệu với SQL Server thông qua Spring Data JPA (`JpaRepository`). | Chỉ được truy cập bởi Service của chính module sở hữu. |
| `entity` | Ánh xạ các bảng cơ sở dữ liệu (`@Entity`). | Không dùng làm response contract cho REST API; hạn chế logic phức tạp. |
| `dto/request` | Biểu diễn dữ liệu Client gửi lên API kèm các annotation validation (`@NotBlank`, `@Min`, `@Pattern`...). | Bất biến (immutable) hoặc dùng Lombok `@Data`/`@Getter`. |
| `dto/response` | Biểu diễn payload dữ liệu Backend trả về cho Client. | Che giấu các trường nhạy cảm (như mật khẩu, hash token). |
| `mapper` | Chuyển đổi dữ liệu hai chiều giữa JPA Entity và DTO. | Có thể dùng MapStruct hoặc method mapping thủ công với `@Builder`. |
| `security` | Cấu hình xác thực, giải mã JWT, filter bảo mật, nạp UserDetails. | Tập trung trong `auth/security/` để quản lý Identity & Access. |
| `integration` | Đóng gói chi tiết kỹ thuật khi kết nối các dịch vụ ngoài (Gemini, Blob, payOS, Brevo). | Cung cấp interface rõ ràng (`AiClient`, `PaymentGateway`) để dễ mock khi kiểm thử. |
| `common` | Chứa cấu hình hạ tầng và tiện ích dùng chung toàn ứng dụng. | Không biến thành nơi chứa các helper nghiệp vụ hỗn tạp. |

### 4.2 Chuẩn hóa phản hồi API và xử lý ngoại lệ (`common/`)

Để đảm bảo Frontend nhận payload nhất quán, Backend chuẩn hóa cấu trúc:

1. **Chuẩn Envelope phản hồi (`common/response/ApiResponse.java`):**
   ```json
   {
     "success": true,
     "message": "Thực hiện thành công",
     "data": { ... },
     "timestamp": "2026-09-28T14:55:00Z"
   }
   ```
2. **Xử lý ngoại lệ tập trung (`common/exception/GlobalExceptionHandler.java`):**
   - Sử dụng `@RestControllerAdvice` để bắt và chuyển đổi toàn bộ ngoại lệ thành JSON format chuẩn.
   - Bắt các lỗi validation biên (`MethodArgumentNotValidException`) và trả về chi tiết field lỗi.
   - Định nghĩa `AppException` kế thừa `RuntimeException` nhận `ErrorCode` (enum định nghĩa mã lỗi nghiệp vụ, HTTP status tương ứng và message mặc định).

## 5. Cấu trúc Resources và Test

### 5.1 Resources cấu hình và Migration

```text
src/main/resources/
├── application.properties               # Cấu hình chung cho ứng dụng
├── application-local.properties         # Cấu hình máy cá nhân (chứa credentials, Git ignored)
└── db/
    └── migration/                       # Lịch sử migration Flyway append-only
        ├── V1__baseline_schema.sql      # Schema khởi tạo cơ sở dữ liệu
        ├── V2__unit_code_unicode.sql    # Migration bổ sung Unicode cho đơn vị
        └── ...                          # Các migration tiếp theo theo thứ tự version
```

### 5.2 Kiểm thử tự động (Unit & Integration Tests)

Cấu trúc thư mục test phản chiếu chính xác package của source code để đảm bảo tính đồng bộ:

```text
src/test/java/tech/mamxanh/
├── common/
├── auth/
├── recipe/
├── mealplan/
├── shopping/
├── nutrition/
├── subscription/
└── integration/
```

Mỗi test class kiểm tra đúng trách nhiệm: Unit Test cho Service (dùng Mockito mock Repository/Client ngoài), WebMvcTest cho Controller, hoặc DataJpaTest cho các query phức tạp.

## 6. Quy tắc phụ thuộc và ranh giới liên module

1. **Cấm gọi chéo Repository:** Module A tuyệt đối không được inject `Repository` của Module B. Ví dụ: `MealPlanService` cần thông tin món ăn bắt buộc phải gọi `RecipeService.getRecipeById(...)`, không được inject `RecipeRepository`.
2. **Controller chỉ gọi Service:** Controller không được gọi trực tiếp `Repository` hoặc Service của module khác ngoài module mà nó đại diện.
3. **Không rò rỉ JPA Entity ra API:** Toàn bộ dữ liệu trả về client phải đi qua DTO.
4. **Không tạo abstraction dư thừa:** Không ép buộc tạo interface cho mọi Service nếu chỉ có duy nhất một implementation và không có nhu cầu thay thế runtime.
5. **Không tạo package rỗng:** Chỉ tạo package khi bắt tay vào triển khai vertical slice tương ứng.

## 7. Quy trình áp dụng theo từng Vertical Slice

Khi bắt đầu một FR, thành viên tạo đúng các package và class cần thiết cho lát cắt đó. Ví dụ lát cắt Authentication (`auth`):

```text
auth/
├── controller/AuthController.java
├── service/AuthService.java
├── repository/UserRepository.java
├── entity/User.java
├── dto/request/LoginRequest.java
├── dto/request/RegisterRequest.java
├── dto/response/AuthResponse.java
└── security/JwtTokenProvider.java
```

Mọi chi tiết class, field, validation và endpoint phải bám sát [OpenAPI Contract](../api/openapi.yaml), [SRS Requirements](../requirements/SRS.md) và [Data Dictionary](../diagrams/ERD/data-dictionary.md).

## 8. Quản lý vòng đời và duy trì quy chuẩn

- Tài liệu này là **Active Baseline** có thẩm quyền về cấu trúc package Backend của dự án.
- Mọi điều chỉnh, tái cấu trúc hoặc phát sinh module mới trong quá trình code phải được thống nhất với Tech Lead và cập nhật lại vào tài liệu này.
- Khi một module hoặc vertical slice mới được đưa vào `main`, cấu trúc thực tế trong mã nguồn phải tuân thủ nghiêm ngặt các ranh giới và quy tắc phụ thuộc đã nêu tại đây.

