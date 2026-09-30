> **Document:** API Integration Guide
> **File:** `docs/api/API.md`
> **Version:** v0.3.1
> **Created:** 2026-09-20
> **Last Updated:** 2026-09-30
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

Frontend không ghi access token vào log. Thời lượng access token là thông số còn mở ở mục 6; owner FR-03 cần đề xuất giá trị để Tech Lead duyệt trước khi triển khai và kiểm thử phần phụ thuộc.

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
| Access-token lifetime | `TBD` | Đề xuất giá trị `expiresInSeconds` và cấu hình token TTL. |
| JWT Claims set | `TBD` | Token chứa `sub` (subject), `role`, claim định danh; không chứa `sid`; claim `jti` là TBD. |
| Frontend token storage | `TBD` | Frontend quyết định nơi lưu trữ Access Token an toàn (memory, storage...). |
| Candidate endpoint `GET /auth/me` | `PENDING` | Xem xét bổ sung sau nếu Frontend cần đồng bộ lại user profile. |
| Role freshness sau khi đổi role | `PENDING` | Cơ chế cập nhật role sau khi Admin nâng cấp vai trò trong lúc JWT cũ chưa hết hạn. |
| Public/development server URLs và CORS origins | `TBD` | Chốt theo môi trường thực tế trước khi cấu hình OpenAPI `servers` và CORS. |

Baseline API đã được nhóm chấp nhận; các mục `TBD` không tự có giá trị chỉ vì tài liệu chuyển sang `Active`. Owner FR-03 phân rã, đề xuất giá trị và cách kiểm thử; Tech Lead duyệt trước khi phần liên quan được coi là implementation-ready. Không suy diễn các giá trị này từ ví dụ hoặc cấu hình tạm.
