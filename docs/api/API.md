> **Document:** API Integration Guide
> **File:** `docs/api/API.md`
> **Version:** v0.1.0
> **Created:** 2026-09-20
> **Last Updated:** 2026-09-20
> **Status:** Under Review

# API Integration Guide

## 1. Mục đích và phạm vi

Tài liệu này hướng dẫn Frontend, Backend và tester sử dụng contract API chung của Mâm Xanh. [OpenAPI contract](openapi.yaml) là Source of Truth cho path, HTTP method, parameter, request/response schema và status code chi tiết; tài liệu này không lặp lại toàn bộ contract.

Phiên bản đầu tiên chỉ bao phủ vertical slice `Authentication & Account` của [FR-03](../requirements/srs/FUNCTIONAL-REQUIREMENTS.md#fr-03). Các module khác được bổ sung khi FR tương ứng chuẩn bị triển khai và contract đã được Tech Lead review theo [CONTRIBUTING.md](../../CONTRIBUTING.md#ownership-ai-và-api-contract).

Contract hiện ở trạng thái `Under Review`; nó mô tả thiết kế API được đề xuất, không phải bằng chứng endpoint đã được triển khai hoặc chạy thành công.

## 2. Contract và công cụ

- Contract chi tiết: [`openapi.yaml`](openapi.yaml).
- Base path đề xuất: `/api/v1`.
- Runtime document dự kiến từ Springdoc: `/v3/api-docs` và `/v3/api-docs.yaml`.
- Swagger UI dự kiến: `/swagger-ui.html`.
- JSON property dùng `camelCase`.
- Timestamp biểu diễn instant dùng ISO-8601 UTC; calendar date dùng `YYYY-MM-DD` và không timezone-shift.
- Response lỗi dùng `application/problem+json` theo `ProblemDetail`, bổ sung `code` ổn định và `errors` cho lỗi theo field khi cần.

## 3. Authentication flow

### 3.1 Access token

Đăng nhập email/password hoặc Google Login trả JWT access token ngắn hạn trong JSON. Frontend gửi token ở các API được bảo vệ:

```http
Authorization: Bearer <access-token>
```

Frontend không lưu hoặc ghi access token vào log. Thời lượng access token chưa được SRS chốt và phải được quyết định trước khi triển khai.

### 3.2 Refresh session

Backend đặt rotating refresh token trong Secure HttpOnly Cookie. JavaScript không được đọc refresh token. Khi refresh thành công, Backend vô hiệu hóa token cũ, phát hành token mới và cập nhật phiên phía máy chủ.

Contract draft dùng tên cookie `refresh_token`. Tên cookie, thời lượng refresh token và cơ chế storage phía máy chủ cần Tech Lead duyệt trước implementation.

Frontend gọi refresh/logout với credential mode bật, ví dụ Axios `withCredentials: true`. Allowed origin phải được cấu hình tường minh; không dùng wildcard origin khi cho phép credential.

### 3.3 CSRF boundary

`POST /auth/refresh` và `POST /auth/logout` là state-changing endpoint dùng cookie do browser tự gửi. Trước implementation, team phải chọn và kiểm thử một cơ chế CSRF phù hợp với Spring Security. CORS không thay thế CSRF protection.

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
| `401 Unauthorized` | Credential không hợp lệ, hết hạn hoặc phiên đã bị thu hồi. |
| `403 Forbidden` | Tài khoản chưa xác minh hoặc bị Administrator khóa. |
| `409 Conflict` | Email đã được sử dụng. |
| `429 Too Many Requests` | Vượt rate limit; client đọc `Retry-After` khi có. |

## 5. Quy tắc bảo mật của Auth slice

- Không trả password, password hash, refresh token hoặc Google ID token trong response/log.
- Login sai dùng thông báo chung để tránh tiết lộ email có tồn tại.
- Password-reset request luôn trả thông điệp trung tính.
- Sau 5 lần đăng nhập sai liên tiếp, chặn theo account identifier và IP trong 10 phút; không đổi account status thành `LOCKED`.
- Reset password thành công thu hồi toàn bộ session đang hoạt động.
- Phát hiện refresh-token reuse thu hồi toàn bộ token family liên quan và xóa cookie.
- Google ID token phải được Backend xác minh chữ ký, issuer, audience và expiry trước khi tạo phiên.

## 6. Open items chặn implementation-ready

| Quyết định | Trạng thái | Ảnh hưởng |
|---|---|---|
| Access-token lifetime | `Needs clarification` | Giá trị `expiresInSeconds`, security test và refresh timing. |
| Refresh-token lifetime | `Needs clarification` | Cookie expiry và session retention. |
| Refresh cookie name | `Proposed: refresh_token` | Frontend credential flow và Spring Security configuration. |
| CSRF mechanism cho refresh/logout | `Needs clarification` | Security contract cho hai endpoint dùng cookie. |
| Public/development server URLs | `Needs clarification` | OpenAPI `servers` và CORS allowed origins. |

Các mục trên không thay đổi phạm vi FR-03, nhưng phải được Tech Lead duyệt trước khi contract Auth được coi là implementation-ready.
