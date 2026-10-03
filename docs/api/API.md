> **Document:** API Integration Guide
> **File:** `docs/api/API.md`
> **Version:** v0.5.0
> **Created:** 2026-09-20
> **Last Updated:** 2026-10-03
> **Status:** Active

# API Integration Guide

## 1. Mục đích và phạm vi

Tài liệu này hướng dẫn Frontend, Backend và tester tích hợp với API Mâm Xanh. Generated OpenAPI từ Spring Boot là runtime contract cho endpoint đã triển khai; trong giai đoạn migration, [OpenAPI YAML](openapi.yaml) là planned/reference contract cho endpoint chưa implement. Tài liệu này không lặp lại schema chi tiết.

Runtime API hiện có các endpoint đăng ký/xác minh email của `Authentication & Account` (FR-03) và hồ sơ dinh dưỡng tham khảo (FR-35). Các endpoint FR-03 chưa triển khai chỉ được mô tả trong planned/reference YAML; endpoint đã triển khai được xác nhận qua generated OpenAPI runtime.

Nhóm đã chấp nhận baseline API hiện có để phân rã và chuẩn bị triển khai FR-03. Các thông số còn mở ở mục 7 phải được owner đề xuất và Tech Lead duyệt trước khi triển khai phần phụ thuộc vào chúng. Trạng thái tài liệu `Active` không phải bằng chứng endpoint đã được triển khai hoặc chạy thành công.

## 2. Contract và công cụ

- Runtime OpenAPI được sinh từ Controller/DTO/annotations bằng springdoc: `/v3/api-docs` (JSON) và `/v3/api-docs.yaml` (YAML).
- Planned/reference contract trong migration: [`openapi.yaml`](openapi.yaml); các endpoint chỉ có tại đây chưa được xem là runtime API.
- Base path: `/api/v1`.
- Giao diện chính thức để team xem và thử API: `/scalar` trên cùng Backend host/port. Scalar tải generated contract từ `/v3/api-docs`.
- Swagger UI vẫn được giữ như giao diện tương thích trong giai đoạn đầu; Scalar là UI được khuyến nghị cho team.
- JSON property dùng `camelCase`.
- Timestamp biểu diễn instant dùng ISO-8601 UTC; calendar date dùng `YYYY-MM-DD` và không timezone-shift.
- Response lỗi dùng `application/problem+json` theo `ProblemDetail`, bổ sung `code` ổn định và `errors` cho lỗi theo field khi cần.

Trong migration, generated OpenAPI là bằng chứng runtime cho endpoint đã implement. Endpoint chưa xuất hiện trong spec runtime nhưng còn trong YAML chỉ là planned contract, không chứng minh endpoint đã tồn tại hoặc hoạt động. Khi migration hoàn tất, generated OpenAPI sẽ là nguồn contract duy nhất; YAML thủ công sẽ không tiếp tục làm authority song song.

Khi triển khai một endpoint đang có trong planned YAML, reviewer đối chiếu ý nghĩa contract của path/method, request/response, status và security với runtime spec và implementation. Docker Development hiện validate cấu trúc generated OpenAPI, không tự thực hiện semantic comparison giữa runtime JSON và YAML tham chiếu.

### 2.1 Scalar dành cho thành viên và tester

Mở `http://localhost:8080/scalar` khi Backend chạy local (bao gồm stack Docker Compose). Tại Scalar, thành viên có thể tìm operation, đọc request/response/schema/status/security, nhập Bearer/JWT token thủ công nếu generated runtime spec khai báo scheme phù hợp, và gửi request tới Backend. Không dùng JWT thật trong ảnh chụp, log hoặc tài liệu.

Scalar là giao diện xem và manual testing; request thử bằng Scalar không thay thế automated regression, authorization, integration hoặc acceptance tests. CI kiểm tra HTTP route và nội dung HTML entry point `/scalar` nhưng không chứng minh JavaScript đã tải/render, OpenAPI đã hiển thị trong browser, hoặc một API request đã chạy thành công. Runtime UI chỉ hiển thị auth schemes do generated OpenAPI khai báo; Bearer/JWT acceptance cần scheme và protected runtime endpoint thật.

## 3. Authentication flow

### 3.1 Access token

Đăng nhập email/password hoặc Google Login trả Stateless JWT Access Token trong JSON body (`AuthResponse`). Frontend gửi token ở các API được bảo vệ:

```http
Authorization: Bearer <access-token>
```

Frontend không ghi access token vào log. Thời lượng access token là thông số còn mở ở mục 7; owner FR-03 cần đề xuất giá trị để Tech Lead duyệt trước khi triển khai và kiểm thử phần phụ thuộc.

### 3.2 Client-side Logout

Hệ thống sử dụng Stateless JWT Access Token, không duy trì server-side session, cookie hay refresh token. Do đó không tồn tại endpoint `POST /auth/logout` trên máy chủ.

Khi người dùng chọn Đăng xuất, Frontend thực hiện:
1. Xóa Access Token khỏi nơi lưu trữ client.
2. Xóa thông tin người dùng trong AuthContext / state.
3. Điều hướng người dùng về trạng thái Guest hoặc màn hình Đăng nhập.

### 3.3 Candidate Follow-up: GET /auth/me

Endpoint `GET /auth/me` (tra cứu thông tin người dùng hiện tại từ token) là một ứng viên follow-up tiềm năng nhưng chưa thuộc planned contract hoặc runtime API. Việc thêm endpoint này sẽ được xem xét trong task riêng khi có yêu cầu cụ thể.

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

Mã `code` đã triển khai (Issue #5). Các mã của đăng nhập, Google Login và đặt lại mật khẩu được bổ sung khi các Issue tương ứng triển khai.

| `code` | Status | Khi nào |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Payload sai định dạng hoặc vi phạm validation; chi tiết theo field nằm trong `errors`. Mật khẩu yếu trả một phần tử `errors` cho mỗi tiêu chí còn thiếu. |
| `EMAIL_ALREADY_USED` | 409 | `POST /auth/register` với email đã có tài khoản (không phân biệt hoa/thường). |
| `VERIFICATION_TOKEN_INVALID` | 400 | `POST /auth/email-verifications` với mã không tồn tại, đã dùng, đã bị thay bằng mã mới hoặc hết hạn. |
| `RESEND_TOO_SOON` | 429 | `POST /auth/email-verifications/resend` trong vòng 60 giây kể từ email xác minh trước; kèm `Retry-After`. |
| `UNAUTHENTICATED` | 401 | Gọi endpoint cần đăng nhập mà không có thông tin xác thực hợp lệ. |
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

## 6. Nutrition Profile — FR-35

Ba endpoint runtime trong generated OpenAPI thao tác hồ sơ của Member hiện tại; client không truyền `userId`. Chúng yêu cầu authenticated Member principal. Tích hợp JWT thật phụ thuộc authentication contract chung; không dùng fake authentication trong production. Kết quả không được lưu thành lịch sử theo dõi.

- `GET /nutrition/profile` chỉ trả dữ liệu hồ sơ đã lưu; không trả BMI hoặc các chỉ tiêu.
- `PUT /nutrition/profile` nhận câu trả lời phạm vi hiện tại cùng ngày sinh, giới tính sinh học, chiều cao, cân nặng, mức vận động, mục tiêu chung và đồng thuận. Backend từ chối lưu nếu ngày sinh không hợp lệ, tuổi dưới 18/trên 120 hoặc có điều kiện loại trừ. Câu trả lời loại trừ chỉ dùng để kiểm tra yêu cầu và không được lưu lên hồ sơ.
- `POST /nutrition/profile/calculate` nhận xác nhận phạm vi hiện tại. Chỉ khi cả ba cờ đều `false` và hồ sơ lưu hợp lệ mới trả BMI cùng 8 thành phần dinh dưỡng (9 chỉ tiêu khi tính cả năng lượng). Phản hồi tính toán không được lưu và giao diện xóa kết quả khi đóng/tải lại trang hoặc thay đổi xác nhận.
- Response hiển thị số dạng xấp xỉ. Đây là tham khảo, không phải chẩn đoán/điều trị/kê đơn, tư vấn y tế, chứng nhận hay giám sát liên tục.
- Lỗi dùng `application/problem+json` và mã ổn định; trường hợp ngoài phạm vi trả `422 NUTRITION_PROFILE_OUT_OF_SCOPE`.

## 7. Thông số cần chốt trước khi triển khai phần phụ thuộc

| Quyết định | Trạng thái | Ảnh hưởng |
|---|---|---|
| Access-token lifetime | `TBD` | Đề xuất giá trị `expiresInSeconds` và cấu hình token TTL. |
| JWT Claims set | `TBD` | Token chứa `sub` (subject), `role`, claim định danh; không chứa `sid`; claim `jti` là TBD. |
| Frontend token storage | `TBD` | Frontend quyết định nơi lưu trữ Access Token an toàn (memory, storage...). |
| Candidate endpoint `GET /auth/me` | `PENDING` | Xem xét bổ sung sau nếu Frontend cần đồng bộ lại user profile. |
| Role freshness sau khi đổi role | `PENDING` | Cơ chế cập nhật role sau khi Admin nâng cấp vai trò trong lúc JWT cũ chưa hết hạn. |
| Public/development server URLs và CORS origins | `TBD` | Chốt theo môi trường thực tế trước khi cấu hình OpenAPI `servers` và CORS. |

Baseline API đã được nhóm chấp nhận; các mục `TBD` không tự có giá trị chỉ vì tài liệu chuyển sang `Active`. Owner FR-03 phân rã, đề xuất giá trị và cách kiểm thử; Tech Lead duyệt trước khi phần liên quan được coi là implementation-ready. Không suy diễn các giá trị này từ ví dụ hoặc cấu hình tạm.
