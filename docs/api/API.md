> **Document:** API Integration Guide
> **File:** `docs/api/API.md`
> **Version:** v0.4.0
> **Created:** 2026-09-20
> **Last Updated:** 2026-10-01
> **Status:** Active

# API Integration Guide

## 1. Mục đích và phạm vi

Tài liệu này hướng dẫn Frontend, Backend và tester sử dụng contract API chung của Mâm Xanh. [OpenAPI contract](openapi.yaml) là Source of Truth cho path, HTTP method, parameter, request/response schema và status code chi tiết; tài liệu này không lặp lại toàn bộ contract.

Phiên bản đầu tiên chỉ bao phủ vertical slice `Authentication & Account` của [FR-03](../requirements/srs/FUNCTIONAL-REQUIREMENTS.md#fr-03). Các module khác được bổ sung khi FR tương ứng chuẩn bị triển khai và contract đã được Tech Lead review theo [CONTRIBUTING.md](../../CONTRIBUTING.md#ownership-ai-và-api-contract).

Nhóm đã chấp nhận baseline API hiện có để phân rã và chuẩn bị triển khai FR-03. Các thông số còn mở ở mục 6 phải được owner đề xuất và Tech Lead duyệt trước khi triển khai phần phụ thuộc vào chúng. Trạng thái tài liệu `Active` không phải bằng chứng endpoint đã được triển khai hoặc chạy thành công.

## 2. Contract và công cụ

- Contract chi tiết: [`openapi.yaml`](openapi.yaml).
- Base path: `/api/v1`.
- Runtime document dự kiến từ Springdoc: `/v3/api-docs` và `/v3/api-docs.yaml`.
- Swagger UI dự kiến: `/swagger-ui.html`.
- JSON property dùng `camelCase`.
- Timestamp biểu diễn instant dùng ISO-8601 UTC; calendar date dùng `YYYY-MM-DD` và không timezone-shift.
- Response lỗi dùng `application/problem+json` theo `ProblemDetail`, bổ sung `code` ổn định và `errors` cho lỗi theo field khi cần.

## 3. Authentication flow

### 3.1 Access token

Đăng nhập email/password hoặc Google Login trả Stateless JWT Access Token trong JSON body (`AuthResponse`). Frontend gửi token ở các API được bảo vệ:

```http
Authorization: Bearer <access-token>
```

Frontend không ghi access token vào log. Token ký HS256 bằng `MAMXANH_JWT_SECRET`, chứa `iss`, `sub` (`USER.user_id`), `role`, `iat`, `exp`; không có `sid` hay refresh token. Thời lượng đọc từ `mamxanh.auth.access-token-ttl` (cấu hình tạm `15m`, `expiresInSeconds = 900`); giá trị chính thức vẫn là thông số mở ở mục 6.

Mỗi request mang Bearer token đều được Backend đọc lại `USER`: token sai chữ ký, sai issuer, hết hạn hoặc của tài khoản không còn tồn tại trả `401 UNAUTHENTICATED`; tài khoản đang `LOCKED` trả `403 ACCOUNT_LOCKED` ngay cả khi token còn hạn. Khi nhận `401`, Frontend xóa token và đưa người dùng về trang Đăng nhập.

### 3.2 Client-side Logout

Hệ thống sử dụng Stateless JWT Access Token, không duy trì server-side session, cookie hay refresh token. Do đó không tồn tại endpoint `POST /auth/logout` trên máy chủ.

Khi người dùng chọn Đăng xuất, Frontend thực hiện:
1. Xóa Access Token khỏi nơi lưu trữ client.
2. Xóa thông tin người dùng trong AuthContext / state.
3. Điều hướng người dùng về trạng thái Guest hoặc màn hình Đăng nhập.

### 3.3 Candidate Follow-up: GET /auth/me

Endpoint `GET /auth/me` (tra cứu thông tin người dùng hiện tại từ token) là một ứng viên follow-up tiềm năng nhưng chưa thuộc contract chính thức trong OpenAPI `openapi.yaml`. Việc thêm endpoint này sẽ được xem xét trong task riêng khi có yêu cầu cụ thể.

## 4. Error convention

Frontend xử lý theo HTTP status và `code`, không parse câu chữ trong `detail`.

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request contains invalid data.",
  "instance": "/api/v1/auth/register",
  "code": "VALIDATION_FAILED",
  "errors": [
    {
      "field": "email",
      "message": "Email must be valid."
    }
  ]
}
```

Quy ước status chính:

| Status | Ý nghĩa |
|---|---|
| `200 OK` | Request thành công và có response body. |
| `201 Created` | Tài khoản đã được tạo. |
| `202 Accepted` | Yêu cầu email đã được tiếp nhận; không xác nhận email có tồn tại. |
| `204 No Content` | Thao tác thành công và không cần response body. |
| `400 Bad Request` | Payload, token xác minh hoặc token đặt lại mật khẩu không hợp lệ. |
| `401 Unauthorized` | Credential không hợp lệ hoặc token hết hạn. |
| `403 Forbidden` | Tài khoản chưa xác minh (`EMAIL_NOT_VERIFIED`) hoặc bị Administrator khóa (`ACCOUNT_LOCKED`). |
| `409 Conflict` | Email đã được sử dụng hoặc đã liên kết với tài khoản Google khác (`GOOGLE_ACCOUNT_CONFLICT`). |
| `429 Too Many Requests` | Chỉ áp dụng khi contract của endpoint quy định `429` (ví dụ login, resend verification hoặc AI rate limiting); client đọc `Retry-After` khi có. Password-reset request không trả `429`. |

Mã `code` đã triển khai (Issue #5, #6). Các mã của Google Login và đặt lại mật khẩu được bổ sung khi các Issue tương ứng triển khai.

| `code` | Status | Khi nào |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Payload sai định dạng hoặc vi phạm validation; chi tiết theo field nằm trong `errors`. Mật khẩu yếu trả một phần tử `errors` cho mỗi tiêu chí còn thiếu. |
| `EMAIL_ALREADY_USED` | 409 | `POST /auth/register` với email đã có tài khoản (không phân biệt hoa/thường). |
| `VERIFICATION_TOKEN_INVALID` | 400 | `POST /auth/email-verifications` với mã không tồn tại, đã dùng, đã bị thay bằng mã mới hoặc hết hạn. |
| `RESEND_TOO_SOON` | 429 | `POST /auth/email-verifications/resend` trong vòng 60 giây kể từ email xác minh trước; kèm `Retry-After`. |
| `INVALID_CREDENTIALS` | 401 | `POST /auth/login` với email không tồn tại, sai mật khẩu hoặc tài khoản chỉ đăng nhập Google (không có mật khẩu); mọi trường hợp cùng `detail` trung tính "Email hoặc mật khẩu không chính xác." |
| `EMAIL_NOT_VERIFIED` | 403 | `POST /auth/login` đúng mật khẩu nhưng email chưa xác minh. |
| `ACCOUNT_LOCKED` | 403 | `POST /auth/login` đúng mật khẩu nhưng tài khoản bị Administrator khóa; hoặc request mang Bearer token hợp lệ của tài khoản đang `LOCKED`. |
| `LOGIN_TEMPORARILY_BLOCKED` | 429 | Lần sai mật khẩu thứ 5 liên tiếp và mọi lần thử `POST /auth/login` trong 10 phút sau đó (kiểm tra trước khi so mật khẩu); kèm `Retry-After` là số giây còn lại. Không đổi `account_status`. |
| `UNAUTHENTICATED` | 401 | Gọi endpoint cần đăng nhập mà không có Bearer token, hoặc token sai chữ ký, sai issuer, hết hạn hay thuộc tài khoản không còn tồn tại. |
| `ACCESS_DENIED` | 403 | Đã xác thực nhưng không đủ quyền. |
| `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE` | 404, 405, 415 | Lỗi định tuyến/định dạng do framework phát hiện. |
| `INTERNAL_ERROR` | 500 | Lỗi không mong đợi; `detail` không chứa thông tin nội bộ. |

`POST /auth/email-verifications/resend` trả cùng một phản hồi `202` cho email không tồn tại và email đã xác minh; hai trường hợp này không gửi email.

## 5. Quy tắc bảo mật của Auth slice

- Không trả password, password hash hoặc Google ID token trong response/log.
- Login sai dùng thông báo chung để tránh tiết lộ email có tồn tại.
- Password-reset request luôn trả cùng HTTP 202 trung tính; silent rate limiting vẫn enforce cooldown 60 giây và tối đa 5 email/giờ/tài khoản. Khi vượt limit hoặc email không tồn tại, không gửi email; response không tiết lộ account existence hay trạng thái rate limit.
- Sau 5 lần đăng nhập sai liên tiếp, rate limit tạm thời 10 phút ở cấp tài khoản (lưu trên bảng `USER`); không rate limit IP; không đổi account status thành `LOCKED`.
- Giới hạn độ dài mật khẩu: 8–64 ký tự, tối đa 72 bytes UTF-8 (chuẩn BCrypt).
- Đăng xuất xử lý hoàn toàn phía client (không gọi backend API).
- Google ID token phải được Backend xác minh chữ ký, issuer, audience và expiry trước khi phát hành token.

## 6. Thông số cần chốt trước khi triển khai phần phụ thuộc

| Quyết định | Trạng thái | Ảnh hưởng |
|---|---|---|
| Access-token lifetime | `TBD` | Backend đọc `mamxanh.auth.access-token-ttl`; Issue #6 cấu hình tạm `15m` trong lúc chờ Tech Lead chốt (Q30, đề xuất 60 phút vì không có refresh token). |
| JWT Claims set | `TBD` | Issue #6 phát hành `iss`, `sub` (`USER.user_id`), `role`, `iat`, `exp`; không chứa `sid`; chưa có `jti` (chờ xác nhận Q31). |
| Frontend token storage | `TBD` | Frontend quyết định nơi lưu trữ Access Token an toàn (memory, storage...). |
| Candidate endpoint `GET /auth/me` | `PENDING` | Xem xét bổ sung sau nếu Frontend cần đồng bộ lại user profile. |
| Role freshness sau khi đổi role | `PENDING` | Issue #6 lấy quyền từ `USER.role` trong lần đọc `USER` bắt buộc ở mỗi request, nên role mới có hiệu lực ngay; claim `role` chỉ để Frontend hiển thị. Chờ Tech Lead xác nhận (Q32). |
| Public/development server URLs và CORS origins | `TBD` | Chốt theo môi trường thực tế trước khi cấu hình OpenAPI `servers` và CORS. |

Baseline API đã được nhóm chấp nhận; các mục `TBD` không tự có giá trị chỉ vì tài liệu chuyển sang `Active`. Owner FR-03 phân rã, đề xuất giá trị và cách kiểm thử; Tech Lead duyệt trước khi phần liên quan được coi là implementation-ready. Không suy diễn các giá trị này từ ví dụ hoặc cấu hình tạm.
