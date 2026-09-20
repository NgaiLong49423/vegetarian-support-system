> **Document:** Backend Package Structure Proposal
> **File:** `docs/architecture/BACKEND-PACKAGE-STRUCTURE-PROPOSAL.md`
> **Version:** v0.1.0
> **Created:** 2026-09-20
> **Last Updated:** 2026-09-20
> **Status:** Draft
> **Related Docs:** `docs/architecture/ARCHITECTURE.md`, `app/mamxanh-backend/README.md`

# Đề xuất cấu trúc package Backend

## 1. Mục đích và giới hạn

Tài liệu này ghi lại cấu trúc package dự kiến cho Backend Mâm Xanh để nhóm có điểm tham chiếu khi bắt đầu triển khai các vertical slice. Đây là **đề xuất đang ở trạng thái Draft**, không phải bằng chứng rằng các package, class hoặc module đã tồn tại trong source code.

Kiến trúc có thẩm quyền hiện tại vẫn là [System Architecture](ARCHITECTURE.md): một Spring Boot application theo **Modular Monolith**, tổ chức theo business capability và áp dụng MVC/layered structure bên trong từng module. Tài liệu này chỉ cụ thể hóa một phương án package ban đầu trong ranh giới đó.

Cấu trúc thực tế sẽ được hình thành dần theo Issue, Functional Requirement, Business Rule và code đã triển khai. Khi implementation cho thấy một cách phân chia khác phù hợp hơn, nhóm phải cập nhật đề xuất thay vì ép code tuân theo các thư mục rỗng hoặc abstraction chưa cần thiết.

## 2. Nguyên tắc tổ chức đề xuất

- Chia package theo business capability trước, sau đó chia layer bên trong từng module.
- Ưu tiên một vertical slice hoàn chỉnh từ API đến database thay vì tạo trước toàn bộ package cho mọi module.
- Giữ luồng phụ thuộc thông thường: `Controller -> Service -> Repository -> Entity -> Database`.
- DTO request/response là contract ở biên HTTP; không trả trực tiếp JPA Entity cho Frontend.
- Business rule, authorization và ownership check thuộc Backend, chủ yếu được điều phối ở Service.
- Chỉ đưa thành phần vào `common` khi có nhu cầu dùng chung đã được chứng minh ở nhiều module.
- External provider được cô lập khỏi Controller và business logic chính.

## 3. Cấu trúc package dự kiến

```text
src/main/java/tech/mamxanh/
├── MamXanhApplication.java
├── common/
│   ├── config/
│   ├── exception/
│   ├── response/
│   └── validation/
├── auth/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   └── security/
├── recipe/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│       ├── request/
│       └── response/
├── mealplan/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── shopping/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── nutrition/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── subscription/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── admin/
│   ├── controller/
│   ├── service/
│   └── dto/
└── integration/
    ├── ai/
    ├── storage/
    ├── payment/
    └── email/
```

Tên module trong cây trên là định hướng ban đầu dựa trên các capability đã được kiến trúc hiện tại nêu rõ. Chúng không tạo requirement mới và không buộc mọi module phải có đủ tất cả subpackage. Ví dụ, `admin` có thể điều phối use case quản trị nhưng không cần sao chép Entity thuộc `auth`, `recipe` hoặc `nutrition`.

## 4. Trách nhiệm dự kiến của từng layer

| Package | Trách nhiệm chính |
|---|---|
| `controller` | Nhận HTTP request, kiểm tra dữ liệu đầu vào ở biên, gọi Service và trả HTTP response. |
| `service` | Điều phối use case, transaction, business rule, authorization và ownership check. |
| `repository` | Truy cập Microsoft SQL Server qua Spring Data JPA. |
| `entity` | Ánh xạ mô hình lưu trữ bằng JPA; không đóng vai trò response contract. |
| `dto/request` | Biểu diễn dữ liệu Client gửi vào API. |
| `dto/response` | Biểu diễn dữ liệu Backend công khai qua API. |
| `security` | Chứa các thành phần authentication/authorization gắn với Identity & Access. |
| `integration` | Bao bọc giao tiếp với Gemini, Azure Blob Storage, payOS và Brevo theo baseline đã chốt. |
| `common` | Chứa cấu hình và cơ chế dùng chung đã có consumer thực tế; không phải nơi chứa mọi helper. |

`mapper` chỉ nên được thêm trong một module khi việc chuyển đổi Entity và DTO đủ phức tạp. Đề xuất này chưa chọn hoặc yêu cầu thêm thư viện mapping.

## 5. Resources và test dự kiến

```text
src/main/resources/
├── application.properties
├── application-local.properties
└── db/
    └── migration/
        ├── V1__initial_schema.sql
        └── ...
```

`application-local.properties` là cấu hình cá nhân và không được Git theo dõi. Flyway migration là lịch sử schema executable, append-only sau khi được áp dụng; Hibernate không thay thế migration history.

Test nên phản chiếu package của source code thay vì tạo một cấu trúc phân loại không liên quan:

```text
src/test/java/tech/mamxanh/
├── auth/
├── recipe/
├── mealplan/
├── shopping/
├── nutrition/
└── subscription/
```

Cấu trúc test cụ thể chỉ được tạo khi có behavior và test case thực tế.

## 6. Quy tắc phụ thuộc ban đầu

- Controller không gọi Repository trực tiếp.
- Module không truy cập tùy tiện Repository nội bộ của module khác; ưu tiên đi qua Service/use-case boundary khi cần phối hợp nghiệp vụ.
- Không đưa business logic vào Controller, DTO, Entity callback hoặc class tiện ích chung.
- Không tạo interface cho mọi Service nếu chưa có nhiều implementation hoặc một boundary cần thay thế rõ ràng.
- Không bổ sung Clean Architecture, Hexagonal Architecture, CQRS, event bus hoặc microservice chỉ để hoàn thiện cấu trúc thư mục.
- Không tạo package rỗng cho toàn bộ capability trước khi bắt đầu implementation tương ứng.

## 7. Cách áp dụng dần

Khi bắt đầu một FR, nhóm tạo đúng các package và class cần cho vertical slice đó. Ví dụ, một slice Authentication có thể bắt đầu với:

```text
auth/
├── controller/AuthController.java
├── service/AuthService.java
├── repository/UserRepository.java
├── entity/User.java
├── dto/request/LoginRequest.java
└── dto/response/AuthResponse.java
```

Ví dụ chỉ minh họa cách tổ chức; nó không xác nhận tên endpoint, field, Entity hoặc class đã được duyệt. Những chi tiết đó phải xuất phát từ SRS, API contract và schema áp dụng cho FR đang triển khai.

## 8. Điều kiện cập nhật trạng thái tài liệu

Tài liệu tiếp tục ở trạng thái `Draft` trong khi cấu trúc mới là đề xuất. Sau khi có ít nhất một vertical slice Backend được triển khai và kiểm chứng, nhóm cần đối chiếu:

1. Package thực tế có giữ đúng ranh giới module và layer hay không.
2. Có package đề xuất nào không cần thiết hoặc thiếu trách nhiệm thực tế hay không.
3. Dependency giữa các module có rõ ràng và kiểm thử được hay không.
4. Tài liệu có phản ánh source code thay vì mô tả cấu trúc lý tưởng chưa tồn tại hay không.

Chỉ sau bước đối chiếu và quyết định của nhóm, tài liệu mới được cập nhật để phản ánh cấu trúc thực tế hoặc được hợp nhất vào tài liệu Backend phù hợp. Việc đổi trạng thái tài liệu không tự thay đổi kiến trúc đã duyệt trong `ARCHITECTURE.md`.
