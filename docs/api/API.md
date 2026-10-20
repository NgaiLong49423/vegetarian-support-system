> **Document:** API Integration Guide
> **File:** `docs/api/API.md`
> **Version:** v0.31.0
> **Created:** 2026-09-20
> **Last Updated:** 2026-10-20
> **Status:** Active

# API Integration Guide

## 1. Mục đích và phạm vi

Tài liệu này hướng dẫn Frontend, Backend và tester tích hợp với API Mâm Xanh. Generated OpenAPI từ Spring Boot là runtime contract. File [`openapi.yaml`](openapi.yaml) là bản xuất được commit từ Springdoc để tiện tra cứu và phải khớp với runtime; không sửa contract endpoint thủ công trong file này.

Backend source trong branch gồm đăng ký, xác minh email, đăng nhập bằng mật khẩu và Google, đặt lại mật khẩu (FR-03), tìm kiếm/lọc RecipePost công khai (FR-08), danh mục quản trị nguyên liệu/đơn vị/quy đổi (FR-18), xác nhận eligibility dinh dưỡng (FR-38) và hồ sơ dinh dưỡng tham khảo (FR-35), đọc chi tiết RecipePost và Meal Plan (FR-20), AI gợi ý và menu tuần công thức (FR-34), AI preview/thay tuần Meal Plan theo dinh dưỡng (FR-36), chatbot Member theo ngữ cảnh chung hoặc RecipePost công khai (FR-51), danh sách RecipePost đã lưu (FR-20/FR-32), nộp báo cáo Recipe Post (FR-26/27), bình chọn (FR-57), lượt xem và thống kê (FR-58), theo dõi thành viên (FR-59), và bình luận lồng nhiều cấp (FR-46). Runtime spec là nguồn contract; YAML trong repo được sinh từ cùng runtime endpoint.

Nhóm đã chấp nhận baseline API hiện có để phân rã và chuẩn bị triển khai FR-03. Các thông số còn mở ở mục 8 phải được owner đề xuất và Tech Lead duyệt trước khi triển khai phần phụ thuộc vào chúng. Trạng thái tài liệu `Active` không phải bằng chứng endpoint đã được triển khai hoặc chạy thành công.

## 2. Contract và công cụ

- Runtime OpenAPI được sinh từ Controller/DTO/annotations bằng springdoc: `/v3/api-docs` (JSON) và `/v3/api-docs.yaml` (YAML).
- Bản OpenAPI YAML được commit tại [`openapi.yaml`](openapi.yaml) để tra cứu và tích hợp; CI so sánh file này với YAML runtime.
- Khi Backend đang chạy tại `http://localhost:8080`, tạo/cập nhật snapshot bằng `./scripts/export-openapi.ps1`. Script tải trực tiếp `/v3/api-docs.yaml` và thay file chỉ sau khi nhận được tài liệu OpenAPI hợp lệ.
- Operation cần đăng nhập khai báo `bearerAuth`; operation công khai thì không. Response lỗi dùng `application/problem+json` theo `ProblemDetail`; operation nhận dữ liệu khai báo `400`, operation cần đăng nhập khai báo `401`/`403`, và lỗi riêng được khai báo theo mã Backend thực sự trả về.
- `OpenApiContractIntegrationTest` kiểm tra security và response lỗi trong generated contract so với quy tắc runtime.
- Base path: `/api/v1`.
- Giao diện chính thức để team xem và thử API: `/scalar` trên cùng Backend host/port. Scalar tải generated contract từ `/v3/api-docs`.
- Swagger UI vẫn được giữ như giao diện tương thích trong giai đoạn đầu; Scalar là UI được khuyến nghị cho team.
- JSON property dùng `camelCase`.
- Timestamp biểu diễn instant dùng ISO-8601 UTC; calendar date dùng `YYYY-MM-DD` và không timezone-shift.
- Response lỗi dùng `application/problem+json` theo `ProblemDetail`, bổ sung `code` ổn định và `errors` cho lỗi theo field khi cần.

Sau khi thay đổi Controller, DTO hoặc OpenAPI annotations, chạy script xuất YAML rồi đưa bản cập nhật vào cùng thay đổi. Docker Development tải YAML runtime, validate bằng Scalar CLI, so sánh với file được commit và báo lỗi nếu snapshot chưa cập nhật. Artifact của CI vẫn cung cấp runtime spec của lần chạy để kiểm tra khi cần.

### 2.1 Scalar dành cho thành viên và tester

Mở `http://localhost:8080/scalar` khi Backend chạy local (bao gồm stack Docker Compose). Tại Scalar, thành viên có thể tìm operation, đọc request/response/schema/status/security, nhập Bearer/JWT token thủ công nếu generated runtime spec khai báo scheme phù hợp, và gửi request tới Backend. Không dùng JWT thật trong ảnh chụp, log hoặc tài liệu.

Scalar là giao diện xem và manual testing; request thử bằng Scalar không thay thế automated regression, authorization, integration hoặc acceptance tests. CI kiểm tra HTTP route và nội dung HTML entry point `/scalar` nhưng không chứng minh JavaScript đã tải/render, OpenAPI đã hiển thị trong browser, hoặc một API request đã chạy thành công. Runtime UI chỉ hiển thị auth schemes do generated OpenAPI khai báo; Bearer/JWT acceptance cần scheme và protected runtime endpoint thật.

## 3. Authentication flow

### 3.1 Access token

Đăng nhập email/password (`POST /auth/login`) và đăng nhập Google (`POST /auth/google`, mục 3.4) cùng trả Stateless JWT Access Token trong JSON body (`AuthResponse`). Frontend gửi token ở các API được bảo vệ:

```http
Authorization: Bearer <access-token>
```

Frontend không ghi access token vào log. Login phát JWT ký HS256 bằng secret cấu hình qua `MAMXANH_JWT_SECRET`; token chứa `iss`, `sub` (`USER.user_id`), `role`, `iat`, `exp`, không có `sid` hoặc refresh token. TTL là 60 phút (`expiresInSeconds = 3600`). Frontend lưu phiên trong `sessionStorage` của tab hiện tại; phiên bị xóa khi tab đóng và không đồng bộ giữa các tab. Request mang Bearer token được Backend kiểm tra tài khoản hiện hành; token không hợp lệ/hết hạn hoặc user không còn tồn tại trả `401`, tài khoản bị quản trị khóa trả `403 ACCOUNT_LOCKED`. Frontend xóa phiên khi nhận các lỗi kết thúc phiên này.

### 3.2 Client-side Logout

Hệ thống sử dụng Stateless JWT Access Token, không duy trì server-side session, cookie hay refresh token. Do đó không tồn tại endpoint `POST /auth/logout` trên máy chủ.

Khi người dùng chọn Đăng xuất, Frontend xóa phiên cục bộ và điều hướng về Đăng nhập; không gọi Backend. Frontend thực hiện:
1. Xóa Access Token khỏi nơi lưu trữ client.
2. Xóa thông tin người dùng trong AuthContext / state.
3. Điều hướng người dùng về trạng thái Guest hoặc màn hình Đăng nhập.

### 3.3 Candidate Follow-up: GET /auth/me

Endpoint `GET /auth/me` (tra cứu thông tin người dùng hiện tại từ token) là một ứng viên follow-up tiềm năng nhưng chưa thuộc planned contract hoặc runtime API. Việc thêm endpoint này sẽ được xem xét trong task riêng khi có yêu cầu cụ thể.

### 3.4 Google Login — FR-03-D

Frontend dùng Google Identity Services (`@react-oauth/google`) để lấy Google ID Token rồi gửi `POST /auth/google` với body `{ "idToken": "<Google ID Token>" }` (tối đa 4096 ký tự). Endpoint công khai, không cần Bearer token; response thành công là `200` với `AuthResponse` như đăng nhập mật khẩu. Không có refresh token hay session máy chủ.

Backend xác minh ID Token bằng `GoogleIdTokenVerifier` (`com.google.api-client:google-api-client`): chữ ký RS256 theo chứng chỉ Google công bố, `exp`, issuer `accounts.google.com`/`https://accounts.google.com` và audience bằng `MAMXANH_GOOGLE_CLIENT_ID`. Việc xác minh chạy trước transaction database. Email, tên và ảnh chỉ lấy từ claim đã xác minh; client không gửi các giá trị này.

Tài khoản được xử lý theo thứ tự (Q26, Q39–Q42):

1. Google `email_verified = false`, token không hợp lệ hoặc email Google không lưu được (không phải ASCII hợp lệ, dài hơn 255 ký tự): `401 GOOGLE_TOKEN_INVALID`, không tạo hay liên kết tài khoản.
2. Tìm theo `USER.google_subject` (`sub`) trước. Tìm thấy: tài khoản `LOCKED` trả `403 ACCOUNT_LOCKED`, còn lại đăng nhập; tên và ảnh không bị ghi đè.
3. Không thấy theo `google_subject` thì tìm theo email đã chuẩn hóa (trim, chữ thường):
   - Không có tài khoản: tạo Member mới `CUSTOMER`, `ACTIVE`, `email_verified = true`, không có mật khẩu; `display_name` và `avatar_url` lấy từ Google. Tên được trim, cắt còn 50 ký tự; nếu rỗng hoặc dưới 3 ký tự thì dùng phần trước `@` của email khi phần đó dài 3–50 ký tự, nếu không thì dùng `Thành viên Mâm Xanh`. Ảnh dài hơn 2048 ký tự bị bỏ qua.
   - Tài khoản `LOCKED`: `403 ACCOUNT_LOCKED`, không liên kết.
   - Email đã liên kết với `google_subject` khác: `409 GOOGLE_ACCOUNT_CONFLICT`.
   - Tài khoản mật khẩu chưa xác minh email: liên kết `google_subject`, đặt `email_verified = true`, xóa `password_hash` và mã xác minh email.
   - Tài khoản mật khẩu đã xác minh email: liên kết `google_subject` và giữ nguyên mật khẩu; người dùng đăng nhập được bằng cả hai cách.
4. Chặn tạm thời do sai mật khẩu (`login_blocked_until`) không ngăn Google Login, và Google Login thành công không đặt lại bộ đếm sai mật khẩu.

Không tải được chứng chỉ Google trả `503 GOOGLE_LOGIN_UNAVAILABLE`. Khi `MAMXANH_GOOGLE_CLIENT_ID` để trống, Backend từ chối mọi token (`401 GOOGLE_TOKEN_INVALID`) và Frontend không hiện nút Google (thiếu `VITE_GOOGLE_CLIENT_ID`). Client ID là giá trị công khai; Backend và Frontend phải dùng cùng một Client ID, và Google Cloud Console phải khai báo origin của Frontend (ví dụ `http://localhost:5173`).

Sau khi Google Login thành công, Frontend gọi `POST /nutrition/dietary-preferences/onboarding/invitation` như sau đăng nhập mật khẩu (mục 9).

### 3.5 Đặt lại mật khẩu — FR-03-E

Hai endpoint công khai, không cần Bearer token (UC-03.6, UC-03.7, AC-03.14, Q27):

- `POST /auth/password-resets` nhận `{ "email": "..." }` và luôn trả `202` với `{ "message": "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn." }`. Chỉ email sai định dạng trả `400 VALIDATION_FAILED`. Backend chỉ gửi email khi tài khoản tồn tại, đang `ACTIVE`, đã qua 60 giây kể từ email đặt lại trước và chưa gửi đủ 5 email trong khung 1 giờ của tài khoản đó; email không tồn tại, tài khoản `LOCKED` và yêu cầu vượt giới hạn đều nhận cùng phản hồi nhưng không có email, không có `429`. Không giới hạn theo IP. Tài khoản chưa xác minh email vẫn nhận email đặt lại.
- Email chứa liên kết `{MAMXANH_FRONTEND_BASE_URL}/dat-lai-mat-khau?token=<token>`. Token là 256 bit ngẫu nhiên (`SecureRandom`); `USER` chỉ lưu SHA-256 digest và hạn 15 phút. Token mới ghi đè token cũ nên chỉ liên kết mới nhất dùng được. Email được gửi bất đồng bộ sau khi transaction commit.
- `POST /auth/password-resets/confirm` nhận `{ "token": "...", "newPassword": "...", "confirmPassword": "..." }`. Mật khẩu mới theo cùng quy tắc đăng ký (8–64 ký tự, tối đa 72 bytes UTF-8, có chữ in hoa, chữ thường và chữ số) và phải khớp `confirmPassword`. Thành công trả `204`: lưu BCrypt hash mới, xóa token, đặt `email_verified = true` nếu email chưa xác minh và kết thúc khóa tạm do nhập sai mật khẩu (NFR-07). Tài khoản chỉ đăng nhập Google (chưa có mật khẩu) có thể đặt mật khẩu theo cách này và giữ liên kết Google (Q47).
- Lỗi của bước xác nhận: token không tồn tại, đã dùng, đã bị thay hoặc hết hạn trả `400 PASSWORD_RESET_TOKEN_INVALID`; mật khẩu mới trùng mật khẩu hiện tại trả `400 NEW_PASSWORD_SAME_AS_CURRENT` và token vẫn dùng được (Q48); tài khoản bị khóa sau khi nhận email trả `403 ACCOUNT_LOCKED`. Hai yêu cầu xác nhận đồng thời với cùng token chỉ có một yêu cầu thành công.
- Hệ thống không có session máy chủ nên không có phiên nào cần thu hồi; access token đã phát trước đó vẫn hợp lệ đến khi hết hạn (tối đa 60 phút, Q30).

Frontend: `/quen-mat-khau` gửi email và hiển thị nguyên văn `message` trung tính; `/dat-lai-mat-khau` đọc `token` từ URL, giữ trong bộ nhớ và xóa khỏi thanh địa chỉ trước khi người dùng nhập mật khẩu mới.

### 3.6 Định danh lượt xem công thức — FR-58

Luồng FR-58 dùng `POST /api/v1/recipes/{recipeId}/views`. Gửi sự kiện sau khi nội dung chi tiết công thức công khai tải thành công; GET chi tiết và GET danh sách không tự tạo sự kiện. Schema/status chi tiết lấy từ generated OpenAPI. Transaction/Flyway và persistence phải được kiểm chứng bằng test SQL Server/Compose; kiểm thử với mock không thay bằng chứng này.

- Member gửi Bearer như các API hiện có; Backend lấy `user_id` từ JWT đã xác minh, không nhận danh tính tài khoản hoặc thời gian từ body. Guest không gửi Bearer và phải gửi `anonymousViewerId` dạng UUID chuẩn; Backend lưu SHA-256 của UUID đã chuẩn hóa chữ thường.
- Frontend giữ UUID của Guest qua reload bằng session cookie `mamxanh.recipe-viewer` trên chính origin Frontend (`Path=/`, `SameSite=Lax`, `Secure` khi HTTPS), rồi gửi trong JSON body. Đây là định danh ẩn danh, không phải auth cookie; không cần bật credentials xuyên origin hoặc sửa mô hình đăng nhập JWT. Khi cookie bị chặn, client báo lỗi và không gửi sự kiện bằng UUID tạm thay đổi qua reload.
- Trang chi tiết ở cả `/cong-thuc/id/:recipeId` và đường dẫn ID số `/cong-thuc/:slug` gửi sự kiện sau khi GET thành công và nội dung đã render. Lỗi GET hoặc phản hồi GET đến sau khi rời trang không ghi nhận lượt xem. Frontend dùng `statistics.viewCount` làm số ban đầu và `viewCount` trong response ghi nhận làm số mới, kể cả `counted=false`; không tự cộng số. Trang chủ và Khám phá dùng tổng `viewCount` từ danh sách. Lỗi ghi nhận vẫn cho phép đọc công thức và thử lại với cùng định danh; phản hồi muộn của trang trước không cập nhật trang hiện tại. Các luồng này đã được nối trong source; kiểm thử trình duyệt dùng API giả lập, chưa thay thế bằng chứng tích hợp SQL Server/Compose.
- Bearer sai/hết hạn không được hạ xuống Guest; tài khoản bị khóa giữ lỗi xác thực hiện hành. Không có loại trừ lượt xem của tác giả như quy tắc Like/Dislike.
- Response trực tiếp gồm `recipeId`, `counted`, `viewCount`, có `Cache-Control: no-store`. Request trùng vẫn thành công với `counted=false`; retry cùng định danh không làm tăng lượt đếm trong cửa sổ.
- Cửa sổ dựa trên lượt hợp lệ gần nhất: không quá 30 phút (kể cả đúng mốc 30 phút) thì bỏ qua; hơn 30 phút thì ghi mới. Request bị bỏ qua không cập nhật timestamp. Thời gian dùng Clock UTC của Backend.
- Sự kiện và bộ đếm được ghi trong cùng transaction. Khi lấy khóa người xem–công thức thất bại, API trả `503 RECIPE_VIEW_UNAVAILABLE`; client có thể thử lại cùng định danh. Không tăng số trên UI nếu chưa có phản hồi thành công; lỗi ghi nhận lượt xem không nên ngăn đọc nội dung công thức.
- Tổng công khai không tiết lộ định danh hoặc lịch sử người xem. Xóa cookie/đổi phiên tạo định danh Guest khác; khử trùng lặp theo phiên không phải cơ chế chống bot hoàn chỉnh.

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
| `401 Unauthorized` | Credential, access token hoặc Google ID Token không hợp lệ hay đã hết hạn. |
| `403 Forbidden` | Tài khoản chưa xác minh (`EMAIL_NOT_VERIFIED`), bị Administrator khóa (`ACCOUNT_LOCKED`) hoặc không phải Member khi dùng hồ sơ sở thích hay hồ sơ cá nhân (`MEMBER_ACCESS_REQUIRED`). |
| `409 Conflict` | Email đã được sử dụng, đã liên kết với tài khoản Google khác (`GOOGLE_ACCOUNT_CONFLICT`), hoặc hồ sơ sở thích chưa đủ để gọi AI cá nhân hóa (`DIETARY_PROFILE_INCOMPLETE`). |
| `429 Too Many Requests` | Chỉ áp dụng khi contract của endpoint quy định `429` (ví dụ login, resend verification hoặc AI rate limiting); client đọc `Retry-After` khi có. Password-reset request không trả `429`. |
| `503 Service Unavailable` | Dịch vụ phụ thuộc tạm thời không dùng được, ví dụ không tải được chứng chỉ Google (`GOOGLE_LOGIN_UNAVAILABLE`). |

Mã `code` đã triển khai (Issue #5, #6, #8, #9, #29, #36).

| `code` | Status | Khi nào |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Payload sai định dạng hoặc vi phạm validation; chi tiết theo field nằm trong `errors`. Mật khẩu yếu trả một phần tử `errors` cho mỗi tiêu chí còn thiếu. |
| `EMAIL_ALREADY_USED` | 409 | `POST /auth/register` với email đã có tài khoản (không phân biệt hoa/thường). |
| `VERIFICATION_TOKEN_INVALID` | 400 | `POST /auth/email-verifications` với mã không tồn tại, đã dùng, đã bị thay bằng mã mới hoặc hết hạn. |
| `RESEND_TOO_SOON` | 429 | `POST /auth/email-verifications/resend` trong vòng 60 giây kể từ email xác minh trước; kèm `Retry-After`. |
| `PASSWORD_RESET_TOKEN_INVALID` | 400 | `POST /auth/password-resets/confirm` với mã không tồn tại, đã dùng, đã bị thay bằng mã mới hoặc hết hạn (15 phút). |
| `NEW_PASSWORD_SAME_AS_CURRENT` | 400 | `POST /auth/password-resets/confirm` với mật khẩu mới trùng mật khẩu hiện tại; token chưa bị tiêu thụ. |
| `INVALID_CREDENTIALS` | 401 | `POST /auth/login` với email không tồn tại, sai mật khẩu hoặc tài khoản chỉ đăng nhập Google (không có mật khẩu); mọi trường hợp cùng `detail` trung tính "Email hoặc mật khẩu không chính xác." |
| `EMAIL_NOT_VERIFIED` | 403 | `POST /auth/login` đúng mật khẩu nhưng email chưa xác minh. |
| `ACCOUNT_LOCKED` | 403 | `POST /auth/login` đúng mật khẩu nhưng tài khoản bị Administrator khóa; `POST /auth/google` cho tài khoản đang `LOCKED`; `POST /auth/password-resets/confirm` với token hợp lệ của tài khoản đang `LOCKED`; hoặc request mang Bearer token hợp lệ của tài khoản đang `LOCKED`. |
| `LOGIN_TEMPORARILY_BLOCKED` | 429 | Lần sai mật khẩu thứ 5 liên tiếp và mọi lần thử `POST /auth/login` trong 10 phút sau đó (kiểm tra trước khi so mật khẩu); kèm `Retry-After` là số giây còn lại. Không đổi `account_status` và không áp dụng cho `POST /auth/google`. |
| `GOOGLE_TOKEN_INVALID` | 401 | `POST /auth/google` với ID Token sai định dạng, sai chữ ký, hết hạn, sai issuer/audience, thiếu `sub`/email, email Google chưa xác minh hoặc không lưu được; cũng trả khi Backend chưa cấu hình `MAMXANH_GOOGLE_CLIENT_ID`. |
| `GOOGLE_ACCOUNT_CONFLICT` | 409 | `POST /auth/google` khi email đã liên kết với một tài khoản Google khác. |
| `GOOGLE_LOGIN_UNAVAILABLE` | 503 | `POST /auth/google` khi Backend không tải được chứng chỉ ký của Google. |
| `MEMBER_ACCESS_REQUIRED` | 403 | Tài khoản Administrator gọi endpoint hồ sơ sở thích ăn uống (FR-31) hoặc hồ sơ cá nhân `/me/profile` (FR-23); chỉ Member (`CUSTOMER`, `EXPERT`) có các hồ sơ này. |
| `MEMBER_PROFILE_NOT_FOUND` | 404 | `GET /members/{userId}` hoặc `/members/{userId}/recipes` với ID không tồn tại hoặc thuộc tài khoản Administrator (FR-23, Q55). |
| `FILE_TOO_LARGE` | 413 | Ảnh đại diện vượt 2 MB, hoặc tệp tải lên vượt giới hạn 5 MB của máy chủ. |
| `UNSUPPORTED_IMAGE_TYPE` | 400 | Tệp ảnh không phải JPEG, PNG hoặc WebP; với ảnh đại diện, nội dung tệp (chữ ký đầu tệp) phải khớp `Content-Type`. |
| `INGREDIENT_PREFERENCE_CONFLICT` | 400 | `PUT /nutrition/dietary-preferences` có cùng một tên (không phân biệt hoa/thường) ở cả danh sách cần tránh và danh sách không thích. |
| `DIETARY_PROFILE_INCOMPLETE` | 409 | Cổng AI cá nhân hóa (BR-31) chặn yêu cầu vì hồ sơ thiếu nhóm thông tin tối thiểu; body có thêm `missing` (`VEGETARIAN_TYPE`, `AVOID_INGREDIENTS`, `DISLIKED_INGREDIENTS`). |
| `UNAUTHENTICATED` | 401 | Gọi endpoint cần đăng nhập mà không có Bearer token, hoặc token sai chữ ký, sai issuer, hết hạn hay thuộc tài khoản không còn tồn tại. |
| `ACCESS_DENIED` | 403 | Đã xác thực nhưng không đủ quyền. |
| `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE` | 404, 405, 415 | Lỗi định tuyến/định dạng do framework phát hiện. |
| `INTERNAL_ERROR` | 500 | Lỗi không mong đợi; `detail` không chứa thông tin nội bộ. |

`POST /auth/email-verifications/resend` trả cùng một phản hồi `202` cho email không tồn tại và email đã xác minh; hai trường hợp này không gửi email.

## 5. Quy tắc bảo mật của Auth slice

- Không trả password, password hash hoặc Google ID token trong response/log.
- Login sai dùng thông báo chung để tránh tiết lộ email có tồn tại.
- Quy tắc trên không che giấu hoàn toàn trạng thái khóa tạm: tài khoản đang bị chặn trả `429 LOGIN_TEMPORARILY_BLOCKED`, còn email không tồn tại/sai mật khẩu trả `401 INVALID_CREDENTIALS`. Đây là rủi ro account-enumeration đã được owner chấp nhận cho Issue #6.
- Password-reset request luôn trả cùng HTTP 202 trung tính; silent rate limiting vẫn enforce cooldown 60 giây và tối đa 5 email/giờ/tài khoản. Khi vượt limit, email không tồn tại hoặc tài khoản `LOCKED`, không gửi email; response không tiết lộ account existence hay trạng thái rate limit.
- Sau 5 lần đăng nhập sai liên tiếp, rate limit tạm thời 10 phút ở cấp tài khoản (lưu trên bảng `USER`); không rate limit IP; không đổi account status thành `LOCKED`.
- Giới hạn độ dài mật khẩu: 8–64 ký tự, tối đa 72 bytes UTF-8 (chuẩn BCrypt).
- Đăng xuất xử lý hoàn toàn phía client (không gọi backend API).
- Google ID token phải được Backend xác minh chữ ký, issuer, audience và expiry trước khi phát hành token; tài khoản được định danh bằng `google_subject`, không bằng email hay tên do client gửi.
- Liên kết Google vào tài khoản mật khẩu chưa xác minh sẽ xóa mật khẩu đó, vì mật khẩu có thể do người không sở hữu hộp thư đặt trước (Q26).

## 6. Nutrition eligibility — FR-38 và Nutrition Profile — FR-35

Eligibility và hồ sơ dinh dưỡng là hai thao tác Backend riêng. Client không truyền `userId`; các endpoint yêu cầu authenticated Member principal. Tích hợp JWT thật phụ thuộc authentication contract chung; không dùng fake authentication trong production.

- `GET /nutrition/profile/eligibility` trả trạng thái hiện tại (`NOT_CONFIRMED`, `ELIGIBLE`, `INELIGIBLE`) và `confirmedAt`. Trạng thái ban đầu là `NOT_CONFIRMED`, timestamp `null`.
- `PUT /nutrition/profile/eligibility` nhận trạng thái xác nhận `ELIGIBLE` hoặc `INELIGIBLE`. Chỉ request xác nhận chủ động mới cập nhật Database; Backend ghi trạng thái và thời điểm xác nhận thành công gần nhất trong cùng giao dịch. `NOT_CONFIRMED` không phải trạng thái được gửi để xác nhận. Lỗi validation/Backend không cập nhật giá trị cũ. Endpoint này tách biệt với lưu hồ sơ và `health_data_consent_at`; không có draft/autosave hoặc lịch sử xác nhận.
- `GET /nutrition/profile`, `PUT /nutrition/profile` và `POST /nutrition/profile/calculate` yêu cầu `ELIGIBLE`. `NOT_CONFIRMED` và `INELIGIBLE` bị chặn ở Backend trước khi đọc, lưu hay tính hồ sơ cá nhân; `INELIGIBLE` vẫn được gọi API eligibility để xác nhận lại. Khi trở lại `ELIGIBLE`, quyền truy cập được khôi phục theo phân quyền hiện hành, không tự động tính lại dữ liệu cũ.
- Guard trả `403 NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED` cho `NOT_CONFIRMED` và `403 NUTRITION_ELIGIBILITY_INELIGIBLE` cho `INELIGIBLE`.
- `PUT /nutrition/profile` lưu hồ sơ FR-35 và đồng thuận xử lý dữ liệu sức khỏe theo hợp đồng FR-35. `nutritionGoal` có thể là `null`; các giá trị được hỗ trợ gồm `MAINTAIN_WEIGHT`, `LOSE_WEIGHT`, `GAIN_WEIGHT`, `BUILD_MUSCLE`, `IMPROVE_HEALTH` và `SUPPORT_TRAINING`. Việc này không tự xác nhận hoặc đổi trạng thái eligibility.
- `POST /nutrition/profile/calculate` trả BMI cùng 8 thành phần dinh dưỡng (9 chỉ tiêu khi tính cả năng lượng) khi hồ sơ lưu hợp lệ. Phản hồi tính toán không được lưu thành lịch sử theo dõi.
- FR-36 dùng trạng thái `ELIGIBLE` đã xác nhận trước khi tạo hoặc áp dụng menu. Nếu chưa xác nhận, API trả `403 NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED`; người dùng có thể xác nhận chủ động qua `PUT /nutrition/profile/eligibility` trước khi thử lại.
- Guard dinh dưỡng cá nhân FR-37 cần được tích hợp tại API do FR đó sở hữu; chỉ endpoint đã tồn tại và được bảo vệ mới có bằng chứng nghiệm thu.
- Response hiển thị số dạng xấp xỉ. Đây là tham khảo, không phải chẩn đoán/điều trị/kê đơn, tư vấn y tế, chứng nhận hay giám sát liên tục.
- Lỗi dùng `application/problem+json` và mã ổn định; trường hợp ngoài phạm vi trả `422 NUTRITION_PROFILE_OUT_OF_SCOPE`.

## 7. Recipe detail, Meal Plan references and saved recipes — FR-20

Các endpoint detail và Meal Plan dưới đây trả projection đọc từ `RECIPE_POST` và bảng liên quan hiện có. Saved Recipes đọc và thay đổi tham chiếu riêng của tài khoản hiện tại trong `SAVED_RECIPE`, không tạo bản sao recipe. Generated `/v3/api-docs` là contract chi tiết của runtime.

| Acceptance criterion / integration | Runtime API hoặc owner | Hành vi |
|---|---|---|
| AC-20.1 — detail từ nguồn RecipePost duy nhất | `GET /api/v1/recipes/{recipeId}` (public) | Trả dữ liệu recipe hiện tại, hồ sơ tác giả công khai, nguyên liệu định lượng, media theo thứ tự, YouTube URL, 9 chỉ tiêu dinh dưỡng ước tính trên toàn công thức và mỗi khẩu phần kèm cảnh báo thiếu dữ liệu, thống kê Like/Dislike và lượt xem. Bài không công khai hoặc không tồn tại trả `404`. |
| AC-20.2 — cập nhật mới nhất ở detail và Meal Plan | `GET /api/v1/recipes/{recipeId}` và `GET /api/v1/meal-plans?weekStartDate=YYYY-MM-DD` (có xác thực) | Mỗi request đọc RecipePost và nguyên liệu hiện tại; không lưu snapshot recipe trùng lặp. |
| AC-20.3 — tombstone an toàn trong Meal Plan | `GET /api/v1/meal-plans?weekStartDate=YYYY-MM-DD` (có xác thực) | Tham chiếu không khả dụng vẫn ở đúng ô, kèm `recipeDeleted` và `unavailableMessage` chung `Công thức không còn khả dụng`; nội dung recipe bị lược bỏ. |
| UC-32.1/32.2 — lưu và bỏ lưu | `PUT /api/v1/saved-recipes/{recipeId}` và `DELETE /api/v1/saved-recipes/{recipeId}` (có xác thực) | Chỉ tài khoản Member hiện tại được xử lý. Lưu chỉ chấp nhận công thức công khai; gọi lặp không tạo mục trùng. Bỏ lưu chỉ xóa tham chiếu của tài khoản hiện tại và an toàn khi gọi lặp. |
| UC-32.3 — danh sách, tìm kiếm và phân trang công thức đã lưu | `GET /api/v1/saved-recipes?keyword=&page=0&size=20` (có xác thực) | Chỉ trả tham chiếu của tài khoản hiện tại, sắp xếp mới lưu gần đây trước. `size` giới hạn 1–50; `keyword` tối đa 120 ký tự, tìm trong tên và mô tả. Mục không khả dụng vẫn được giữ an toàn với `available=false`, thông báo chung và không có nội dung recipe, tác giả hay media. |
| AC-20.4 — export PDF/TXT | Frontend | Backend trả dữ liệu detail cần cho export. Frontend tạo TXT và PDF khổ A4; API này không tạo file. |
| AC-20.5 — tải toàn trang trong hai giây | Frontend xác minh acceptance | Backend trả đủ instructions trong cùng response detail. Frontend cần đo thời gian tải và render toàn trang trên môi trường mục tiêu. |
| AC-40.1/40.2 — công khai khi thiếu dữ liệu dinh dưỡng (FR-40, BR-50) | `POST /api/v1/recipes`, `GET /api/v1/recipes/{recipeId}` | Nguyên liệu trong catalog nhưng chưa bật tính không chặn công khai (nguyên liệu ngoài catalog vẫn bị từ chối theo BR-12). `nutritionSupported` biểu thị có tham gia tính phần dữ liệu hiện có; `nutritionComplete` và `ingredientsWithoutNutrition` cảnh báo chỉ tiêu còn thiếu. Các chỉ tiêu `NULL` không được xem là 0; số liệu AI-generated được giải thích là ước tính chưa xác minh. |
| AC-40.3 — loại khỏi ứng viên AI menu dinh dưỡng (BR-40, BR-50) | `POST /api/v1/meal-plans/ai-menu/previews`, `PUT /api/v1/meal-plans/ai-menu` | Pool ứng viên FR-36 (dùng cho prompt Gemini và cho bước kiểm lại khi áp dụng) chỉ gồm Recipe Post `PUBLISHED` có dinh dưỡng đầy đủ, cùng phép tính với detail. Áp dụng menu chứa bài thiếu dữ liệu trả `404 RECIPE_NOT_FOUND`, Lịch ăn giữ nguyên. Không có endpoint mới. |

Các key trong `nutrition.total` và `nutrition.perServing` lần lượt là `ENERGY_KCAL` (kcal), `PROTEIN_G` (g), `CARBOHYDRATE_G` (g), `TOTAL_FAT_G` (g), `FIBER_G` (g), `CALCIUM_MG` (mg), `IRON_MG` (mg), `VITAMIN_B12_MCG` (mcg) và `ZINC_MG` (mg). Nếu chưa có lượt bình chọn, `statistics.reactionCount` bằng `0` và `statistics.likePercentage` là `null`; Frontend hiển thị nhãn “Mới” theo BR-69.

Nutrition chỉ dùng danh mục nội bộ và quy đổi gram hiện có. `nutrition.complete=false` cùng `ingredientsMissingData` chỉ rõ nguyên liệu tùy chỉnh/thiếu dữ liệu hoặc thiếu quy đổi; không ngầm coi dữ liệu chưa biết là giá trị đo bằng không. Đây là số liệu tham khảo, không phải tư vấn y tế. Không gọi dịch vụ dinh dưỡng ngoài.

### 7.1. Tạo hoặc mở lịch tuần — FR-09

- `GET /api/v1/meal-plans?weekStartDate=YYYY-MM-DD` chỉ đọc lịch của Member hiện tại. Khi chưa có lịch, trả tuần với `entries=[]` và không tạo bản ghi.
- `PUT /api/v1/meal-plans?weekStartDate=YYYY-MM-DD` tạo lịch nếu chưa có, hoặc mở lại lịch đã có, trả `200` cùng `MealPlanWeekResponse`. Không cần request body. Gọi lặp giữ nguyên lịch và các món đã lưu.
- Cả hai thao tác yêu cầu JWT Bearer và `weekStartDate` theo định dạng ISO, là ngày Thứ Hai. Chủ lịch được lấy từ tài khoản đã xác thực; client không được chọn chủ lịch bằng `userId`. Guest/token không hợp lệ trả `401`; Admin hoặc tài khoản bị khóa trả `403`; ngày thiếu, sai định dạng hoặc không phải Thứ Hai trả `400 VALIDATION_FAILED`.
- Mỗi Member chỉ có một lịch cho mỗi tuần theo unique constraint hiện có. Thao tác tạo dùng transaction và khóa phạm vi khóa SQL Server để xử lý yêu cầu tạo đồng thời. Không gọi Gemini, không tạo món mặc định và không thay đổi danh sách công thức đã lưu.
- Contract chi tiết lấy từ generated `/v3/api-docs`; kiểm thử SQL Server và Compose cần được xác minh riêng với kiểm thử MVC dùng mock service.

### 7.2. Thêm, thay, chuyển và gỡ món — FR-09/FR-33

| Thao tác | API | Request và kết quả |
|---|---|---|
| Thêm món | `POST /api/v1/meal-plans/{weekStartDate}/entries` | JSON gồm `mealDate`, `mealType` (`BREAKFAST`, `LUNCH`, `DINNER`) và `recipeId` (số nguyên ≥1). Trả `201` cùng projection `MealPlanWeekResponse.Entry`; khẩu phần mặc định 1. Tạo tuần nếu chưa có. |
| Thay công thức | `PUT /api/v1/meal-plans/{weekStartDate}/entries/{entryId}` | JSON gồm `recipeId` thay thế. Trả `200` cùng projection của mục món; giữ `entryId`, ngày, loại bữa và khẩu phần hiện tại. |
| Chỉnh số phần cần nấu | `PATCH /api/v1/meal-plans/{weekStartDate}/entries/{entryId}/servings` | JSON gồm `plannedServings` (số nguyên 1–9.999). Trả `200` cùng projection đã lưu; giữ nguyên công thức, ngày và bữa. |
| Gỡ món | `DELETE /api/v1/meal-plans/{weekStartDate}/entries/{entryId}` | Trả `204`, không có response body. Chỉ xóa mục lịch; công thức nguồn và mục đã lưu không bị thay đổi. |
| Chuyển món | `PATCH /api/v1/meal-plans/{weekStartDate}/entries/{entryId}/position` | JSON gồm `mealDate` và `mealType` đích. Trả `200` cùng projection đã chuyển; giữ ID, công thức và khẩu phần, kể cả giá trị lẻ cũ. Chỉ chuyển trong cùng tuần; ngoài tuần trả `400`, trùng công thức ở bữa đích trả `409 MEAL_PLAN_RECIPE_DUPLICATE`. |

- `weekStartDate` phải là Thứ Hai; toàn bộ tuần phải nằm trong phạm vi ngày SQL Server (năm 1–9999). Ngày ăn phải nằm trong bảy ngày của tuần đã chọn; dữ liệu không hợp lệ trả `400 VALIDATION_FAILED`.
- Backend xác định chủ lịch từ tài khoản đã xác thực. Mục không tồn tại trong tuần của chủ tài khoản, kể cả ID thuộc người khác hoặc tuần khác, trả `403 ACCESS_DENIED`; không truy xuất hoặc sửa mục theo ID đơn lẻ.
- Chỉnh số phần dùng cùng transaction/khóa tuần và kiểm tra chủ sở hữu như thao tác thay món. Thiếu giá trị, số nhỏ hơn 1 hoặc lớn hơn 9.999, hay có phần thập phân khác 0 trả `400 VALIDATION_FAILED`; cột database vẫn là `DECIMAL(5,1)` để giữ dữ liệu cũ, không thay đổi schema. API ghi khẩu phần mới chỉ nhận số nguyên. Mục có công thức không còn khả dụng vẫn sửa được số phần, nhưng response giữ nhãn trung tính và không trả nội dung recipe bị ẩn/xóa.
- Trong luồng lập lịch hiện tại theo yêu cầu người dùng, `plannedServings` là số phần cần nấu (có thể cho cả gia đình). Dữ liệu này phục vụ tỷ lệ nguyên liệu đi chợ: lượng gốc × `plannedServings` ÷ số phần gốc của công thức. Bảng dinh dưỡng ngày/tuần trên Frontend thống kê riêng **1 người, mỗi món 1 phần**, không nhân theo lượng nấu cho gia đình. BR-47 hiện vẫn mô tả khẩu phần dự định ăn; hai ý nghĩa cần được nhóm phân biệt khi triển khai thống kê lượng đã ăn. Việc có API chỉnh số phần không xác nhận chức năng tổng hợp danh sách đi chợ đã triển khai.
- Một bữa chứa nhiều công thức khác nhau. Thêm hoặc thay thành công thức đã có trong cùng ngày/bữa trả `409 MEAL_PLAN_RECIPE_DUPLICATE`; thay lại cùng công thức của chính mục đó không bị xem là trùng mục khác.
- Chỉ công thức `PUBLISHED` được thêm hoặc dùng để thay thế; nguồn thiếu/ẩn/xóa/nháp trả `404 RECIPE_NOT_FOUND`. Kiểm tra trạng thái nguồn và ghi lịch nằm trong cùng transaction; các thay đổi của cùng lịch tuần được tuần tự hóa bằng khóa ở database và vẫn có unique constraint bảo vệ.
- Tombstone hiện có được thay hoặc gỡ như mục thông thường. Gỡ không yêu cầu công thức nguồn còn công khai; đọc lịch vẫn trả nhãn chung `Công thức không còn khả dụng` và lược bỏ nội dung bài không công khai.
- Frontend dùng dữ liệu response sau thêm/thay/chuyển và bỏ mục sau `204`; khi request thất bại giữ dữ liệu đang hiển thị và cho phép thử lại. Chuyển món khóa lịch tuần trước kiểm tra đích và cập nhật cùng một mục trong transaction; không xóa rồi thêm lại. Các thao tác này không gọi Gemini.

### 7.3. Xuất lịch tuần — FR-33

- `GET /api/v1/meal-plans/{weekStartDate}/export` trả snapshot JSON riêng cho tài khoản Member đang xác thực, kèm `Cache-Control: no-store`. Không nhận ID chủ lịch. Guest nhận `401`, Admin hoặc tài khoản khóa nhận `403`; ngày sai định dạng/không phải Thứ Hai nhận `400`. Tuần chưa tạo trả danh sách trống và không ghi database.
- Snapshot gồm `weekStartDate` và các mục `mealDate`, `mealType`, `title`, `plannedServings`, `energyKcal`, `available`; generated OpenAPI là contract chi tiết. Năng lượng lấy từ Backend tính theo nguyên liệu/quy đổi nội bộ, bằng kcal mỗi phần × khẩu phần đã lưu. Đây là lượng của các phần trong lịch, không phải bảng dinh dưỡng một người hoặc so sánh DRI. Dữ liệu thiếu giữ `energyKcal=null`, không thay bằng 0.
- Công thức không khả dụng chỉ có ngày/bữa và nhãn trung tính; khẩu phần/năng lượng là `null`, không trả ID, ảnh hoặc nội dung bài nguồn. Công thức bị ẩn giữa lúc đọc lịch và lấy chi tiết cũng được chuyển sang nhãn không khả dụng. Lỗi khác làm request thất bại để người dùng thử lại, không tải file có dữ liệu suy đoán.
- Frontend tạo file từ snapshot mới lấy khi người dùng chủ động xuất: đủ bảy ngày, ba bữa mỗi ngày, mọi món trong bữa và trạng thái bữa trống. TXT là UTF-8; PDF tải trực tiếp, khổ A4 và phân trang khi nhiều món, dùng Noto Sans đi kèm để hiển thị tiếng Việt. Không xuất bảng chín chỉ tiêu, DRI hoặc Shopping List.

## 8. Thông số cần chốt trước khi triển khai phần phụ thuộc

| Quyết định | Trạng thái | Ảnh hưởng |
|---|---|---|
| Access-token lifetime | `Confirmed (Q30)` | JWT HS256 có hiệu lực 60 phút; MVP/demo chấp nhận access token không bị thu hồi trước hạn. |
| JWT Claims set | `Confirmed (Q31)` | Dùng `iss`, `sub` (`USER.user_id`), `role`, `iat`, `exp`; không dùng `jti`. |
| Frontend token storage | `Confirmed (Q33)` | Lưu phiên trong `sessionStorage`; không đồng bộ giữa các tab. |
| Candidate endpoint `GET /auth/me` | `PENDING` | Xem xét bổ sung sau nếu Frontend cần đồng bộ lại user profile. |
| Role freshness sau khi đổi role | `Confirmed (Q32)` | Backend lấy quyền từ `USER.role` khi xác thực từng request; claim `role` chỉ phục vụ hiển thị. |
| Public/development server URLs và CORS origins | `TBD` | Chốt theo môi trường thực tế trước khi cấu hình OpenAPI `servers` và CORS. |

Baseline API đã được nhóm chấp nhận; các mục `TBD` không tự có giá trị chỉ vì tài liệu chuyển sang `Active`. Owner FR-03 phân rã, đề xuất giá trị và cách kiểm thử; Tech Lead duyệt trước khi phần liên quan được coi là implementation-ready. Không suy diễn các giá trị này từ ví dụ hoặc cấu hình tạm.

## 9. Sở thích ăn uống và Onboarding — FR-31

Năm endpoint runtime thao tác hồ sơ của Member đang đăng nhập; client không truyền `userId`. Generated OpenAPI khai báo security scheme `bearerAuth` (HTTP bearer, JWT) cho cả năm operation; client gửi access token của `POST /auth/login` trong header `Authorization: Bearer`. Guest nhận `401 UNAUTHENTICATED`, Administrator nhận `403 MEMBER_ACCESS_REQUIRED`. Dữ liệu này riêng tư: không có endpoint nào trả sở thích của người khác.

- `GET /nutrition/dietary-preferences` trả `vegetarianType`, `avoid` và `dislike` (mỗi danh sách gồm `noneConfirmed` và `items` với `ingredientId`, `name`), ba sở thích tùy chọn, `onboardingStatus` và `aiPersonalization` (`eligible`, `missing`).
- `PUT /nutrition/dietary-preferences` lưu toàn bộ hồ sơ và chuyển `onboardingStatus` sang `COMPLETED` (dùng cho cả "Hoàn tất" Onboarding và "Lưu thay đổi" trong Cài đặt).
  - `vegetarianType` bắt buộc, một trong `VEGAN`, `LACTO`, `OVO`, `LACTO_OVO`.
  - `avoid` và `dislike` bắt buộc: có ít nhất một mục trong `items` hoặc `noneConfirmed = true`. Danh sách rỗng mà không xác nhận trả `400 VALIDATION_FAILED` với `errors[].field` là `avoid` hoặc `dislike` (BR-31). Thiếu `noneConfirmed` được hiểu là chưa xác nhận.
  - Mỗi danh sách tối đa 30 mục, mỗi tên 1–200 ký tự. Backend bỏ khoảng trắng thừa, gộp tên trùng (không phân biệt hoa/thường) và liên kết `ingredientId` khi tên trùng một nguyên liệu chuẩn đang hoạt động. Khi danh sách có mục, `noneConfirmed` được đưa về `false`.
  - Một tên ở cả hai danh sách trả `400 INGREDIENT_PREFERENCE_CONFLICT`.
  - Tùy chọn: `cuisinePreference` (tối đa 200 ký tự), `maxCookingTimeMinutes` (1–1440), `preferredDifficulty` (`EASY`, `MEDIUM`, `HARD`; giao diện hiển thị `HARD` là "Nâng cao").
  - Cập nhật hồ sơ không sửa Meal Plan đã lưu; chỉ các yêu cầu AI sau đó dùng dữ liệu mới.
- `POST /nutrition/dietary-preferences/onboarding/skip` trả `204`, chuyển `NOT_STARTED` sang `SKIPPED`; gọi lại hoặc gọi khi đã `COMPLETED` không đổi trạng thái.
- `POST /nutrition/dietary-preferences/onboarding/invitation` trả `200` với `{ "show": true | false }`. Chỉ trả `true` khi `onboardingStatus = NOT_STARTED` và lời mời chưa từng hiển thị; khi trả `true`, Backend ghi thời điểm vào `USER.onboarding_invited_at`. Backend khóa dòng `USER` của tài khoản trong lúc kiểm tra, nên các request song song chỉ có một request nhận `true`.
- `GET /nutrition/dietary-preferences/ingredient-suggestions?query=` trả tối đa 10 nguyên liệu chuẩn đang hoạt động (`id`, `name`, `ingredientGroup`); query rỗng trả danh sách rỗng.

Luồng Onboarding (AC-31.10): sau mỗi lần đăng nhập thành công của Member, Frontend gọi `POST .../onboarding/invitation` và chỉ chuyển tới trang Onboarding khi `show = true`. Vì vậy lời mời chỉ hiện một lần, kể cả khi Member rời questionnaire mà chưa "Hoàn tất" hay "Bỏ qua" (trạng thái vẫn `NOT_STARTED`). Trang Onboarding và trang Sở thích ăn uống trong Cài đặt vẫn mở thủ công được. Tài khoản tồn tại trước migration V7 được ghi `onboarding_invited_at` nên không bị hỏi và giữ nguyên `onboardingStatus`; tài khoản tạo sau đó bắt đầu ở `NOT_STARTED` với `onboarding_invited_at = NULL`. Luồng đăng nhập khác (ví dụ Google Login, Issue #8) cần gọi endpoint này sau khi đăng nhập thành công.

Cổng AI cá nhân hóa (AC-31.4–AC-31.6): endpoint AI gợi ý món hoặc tạo thực đơn tuần phải gọi `DietaryPreferenceService.requirePersonalizedAiEligible(userId)` trước mọi xử lý khác. Khi thiếu thông tin, request dừng với `409 DIETARY_PROFILE_INCOMPLETE` kèm `missing`; không gọi Gemini và không ghi Meal Plan. Cổng chỉ kiểm tra dữ liệu hồ sơ, không dựa vào `onboardingStatus`.

## 10. Tìm kiếm công thức công khai — FR-08

**Tích hợp thống kê FR-58:** `viewCount` trên danh sách và `statistics.viewCount` trên chi tiết là tổng số sự kiện xem đã lưu, không đổi ý nghĩa theo `viewPeriod`. Tham số `viewPeriod` chỉ chọn số lượt dùng để xếp hạng Most Viewed; card vẫn hiển thị tổng toàn thời gian.

Các khoảng là khoảng trượt theo Clock UTC của Backend, không phải ngày/tuần/tháng lịch: 24 giờ, 168 giờ, 720 giờ, hoặc lịch sử tới hiện tại. Mốc bắt đầu và kết thúc đều được tính; sự kiện sau hiện tại không tăng điểm Most Viewed. Nếu bằng số lượt trong khoảng, ưu tiên `published_at` mới hơn rồi `recipe_id` tăng dần để phân trang ổn định.

Most Active dùng tương tác 7 ngày; Trending dùng tương tác 72 giờ và suy giảm theo tuổi bài cùng bonus 48 giờ theo policy hiện có. Các truy vấn này dùng sự kiện `RECIPE_VIEW` của luồng ghi nhận mới, không lấy số do Frontend gửi. Kiểm chứng thứ tự, ranh giới thời gian và liên kết với luồng ghi nhận dùng các test SQL Server; test mapping với mock không chứng minh thứ tự truy vấn thực tế.

`GET /api/v1/recipes` là API đọc công khai có phân trang. Generated OpenAPI runtime là contract chi tiết. Endpoint nhận `keyword`, `page` (bắt đầu từ 0), `size` (1–50), `sort` (`NEWEST`, `MOST_LIKED`, `MOST_VIEWED`, `MOST_COMMENTED`, `MOST_ACTIVE` hoặc `TRENDING`) và `viewPeriod` (`ALL_TIME`, `LAST_24_HOURS`, `LAST_7_DAYS` hoặc `LAST_30_DAYS`).

Bộ lọc tùy chọn gồm `vegetarianType` (`VEGAN`, `LACTO`, `OVO`, `LACTO_OVO`), `dishCategory` (mã do API form-options trả về), `ingredientIds` lặp lại (tối đa 20 ID dương) và `maxTotalTimeMinutes` (số nguyên dương). Mọi tiêu chí đã gửi kết hợp theo AND. Nhiều `ingredientIds` cũng kết hợp theo AND: mỗi công thức phải chứa đủ mọi nguyên liệu chuẩn đã chọn. Tổng thời gian bằng thời gian chuẩn bị cộng thời gian nấu. Chỉ bài công thức công khai được trả về.

```http
GET /api/v1/recipes?keyword=canh&vegetarianType=VEGAN&dishCategory=SOUP&ingredientIds=12&ingredientIds=29&maxTotalTimeMinutes=30&sort=TRENDING&page=0&size=12
```

Dùng `GET /api/v1/recipes/form-options` để lấy mã/nhãn loại ăn chay và thể loại món hiện hành; dùng `GET /api/v1/recipes/ingredient-options?query=...` để tìm trong danh mục nguyên liệu đang hoạt động. Frontend không hard-code danh mục tùy chọn. Bộ lọc hoặc tham số phân trang ngoài giới hạn runtime trả `400` theo ProblemDetail chuẩn với `code` ổn định và lỗi field nếu có. Response danh sách giữ cấu trúc `items`, `page`, `size`, `totalElements`, `totalPages` hiện tại, đồng thời có `likes`, `dislikes`, `likePercentage` (`null` khi chưa có lượt bình chọn) và `viewCount` để hiển thị theo sort.

## 11. AI hỗ trợ soạn Recipe Post — FR-21

`POST /api/v1/recipes/ai-assistance` yêu cầu Bearer token của Expert đang hoạt động và gói Plus/Pro còn hiệu lực. Backend xác thực entitlement trước khi gọi Gemini. Generated OpenAPI runtime là hợp đồng chi tiết cho request/response và các lỗi.

Request chọn `contentType` là `DESCRIPTION` hoặc `INSTRUCTIONS`, gửi tiêu đề, mã danh mục, loại ăn chay, khẩu phần và các dòng nguyên liệu gồm `ingredientId`, `unitId`, `quantity`. Backend đối chiếu ingredient ID với danh mục đang hoạt động, yêu cầu Gemini trả các bước có cấu trúc và chỉ chấp nhận ingredient ID trong request. Nội dung không hợp lệ hoặc nhắc tên nguyên liệu có trong danh mục nhưng không được chọn bị từ chối.

Response trả `contentType` và `content` để Frontend điền trực tiếp vào trường có thể chỉnh sửa. API không tạo RecipePost hoặc bản nháp; chỉ thao tác đăng/lưu hiện có mới ghi công thức sau khi tác giả chủ động xác nhận. Free, hết hạn hoặc thiếu subscription nhận `403 AI_SUBSCRIPTION_REQUIRED` trước khi provider được gọi. Gemini thiếu cấu hình/lỗi nhận `503 AI_PROVIDER_UNAVAILABLE`; kết quả sai định dạng/giới hạn nhận `502 AI_RESPONSE_INVALID`. Các lỗi không ngăn tác giả tự nhập và đăng theo validation hiện hành.

Backend gọi model cố định `gemini-3.8-flash` qua Google Gen AI Java SDK; khóa chỉ được đọc từ `GEMINI_API_KEY` ở Backend. Không gửi API key xuống Frontend. Không ghi prompt thô vào log.

## 12. Hồ sơ thành viên — FR-23

Generated OpenAPI runtime là contract chi tiết; mục này ghi quy ước dùng chung (quyết định Q54–Q58).

- `GET /api/v1/members/{userId}` (công khai) trả `MemberProfileResponse`: `userId`, `displayName`, `avatarUrl` (null thì Frontend hiện ảnh mặc định), `bio` (null khi trống) và `joinedMonth` dạng `yyyy-MM` tính theo giờ Việt Nam từ `USER.created_at` (UTC). Không trả email, role, trạng thái tài khoản hay dữ liệu riêng tư (AC-23.2, BR-18). Hồ sơ của Member bị `LOCKED` vẫn công khai; ID không tồn tại hoặc của Administrator trả `404 MEMBER_PROFILE_NOT_FOUND` (Q55).
- `GET /api/v1/members/{userId}/recipes?page=0&size=12` (công khai) trả cùng dạng phân trang với `GET /api/v1/recipes`, chỉ gồm bài `PUBLISHED` của thành viên, mới nhất trước; `size` 1–50 (Q56).
- `GET /api/v1/me/profile` và `PUT /api/v1/me/profile` (cần Bearer token) luôn thao tác trên tài khoản trong phiên; không nhận `userId` (AC-23.8). Body `PUT`: `displayName` (bỏ khoảng trắng đầu cuối, 3–50 ký tự, không có ký tự điều khiển) và `bio` (bỏ khoảng trắng đầu cuối, tối đa 500 ký tự, rỗng lưu `NULL`) (Q58). Lỗi validation trả `400 VALIDATION_FAILED` kèm `errors[].field`. Tên mới hiện ngay trên hồ sơ và mọi bài của tác giả vì tác giả được đọc từ tài khoản hiện tại.
- `POST /api/v1/me/profile/avatar` (cần Bearer token, `multipart/form-data`, trường `file`) nhận JPEG, PNG hoặc WebP tối đa 2 MB; Backend đọc chữ ký đầu tệp và yêu cầu khớp `Content-Type` (AC-23.5, AC-23.7). Ảnh lưu qua `StorageClient` của FR-14 trong thư mục `avatars/` (Q54); ảnh đại diện cũ do hệ thống lưu bị xóa sau khi lưu thành công. Response là `MemberProfileResponse` mới.
- Administrator gọi `/me/profile` hoặc `/me/profile/avatar` nhận `403 MEMBER_ACCESS_REQUIRED`.
- Giới hạn multipart của máy chủ là 5 MB/tệp và 6 MB/request để ảnh công thức (≤ 5 MB) và ảnh đại diện (≤ 2 MB) tới được bước kiểm của service; tệp vượt giới hạn này trả `413 FILE_TOO_LARGE`.

## 13. Công thức liên quan — FR-47

Generated OpenAPI runtime là contract chi tiết. Hai API đều trả `{ "items": [...] }`, trong đó mỗi item dùng cùng DTO công thức công khai với `GET /api/v1/recipes`; thứ tự đã được Backend xác định, Frontend không xếp lại hoặc thay bằng dữ liệu mẫu.

- `GET /api/v1/recipes/{recipeId}/related?limit=6` công khai, không kiểm tra subscription và không gọi Gemini. `recipeId` dương; `limit` mặc định 6, từ 1 đến 12. Bài nguồn phải `PUBLISHED`; nguồn không khả dụng trả `404 RECIPE_NOT_FOUND`. Danh sách không chứa bài nguồn, bài ẩn/xóa hoặc ID trùng; ít ứng viên thì trả ít item, không có ứng viên trả `200` với `items: []`.
- Xếp hạng theo [FR-47](../requirements/srs/FUNCTIONAL-REQUIREMENTS.md#fr-47): khớp một phần, một điểm cho mỗi tiêu chí cùng thể loại/cùng loại chay/trùng nguyên liệu chính; tiếp theo là số nguyên liệu chuẩn trùng, ngày công khai và ID. Nguyên liệu chính có khối lượng gram lớn nhất; thiếu quy đổi hoặc nguyên liệu tùy chỉnh không có ID chuẩn thì bỏ tiêu chí nguyên liệu chính của công thức đó.
- `POST /api/v1/recipes/{recipeId}/related/ai?limit=6` không cần body; yêu cầu Bearer token của Customer/Expert đang hoạt động với Plus/Pro còn hiệu lực. Backend kiểm tra quyền trước khi gọi provider. Guest nhận `401`; Free/hết hạn nhận `403 AI_SUBSCRIPTION_REQUIRED`; Administrator không có quyền Member. Không áp quota lượt/ngày.
- Gemini nhận tối đa 50 ứng viên công khai cùng loại chay với nguồn, phân bổ theo thể loại, và chỉ trả ID. Backend từ chối cấu trúc sai, quá giới hạn hoặc ID ngoài tập bằng `502 AI_RESPONSE_INVALID`; loại ID trùng và đọc lại trạng thái/nội dung hiện tại sau khi provider trả về. Bài bị ẩn/xóa hoặc đổi sang loại chay khác không được gửi xuống FE. Không có ứng viên thì trả rỗng và không gọi Gemini.
- Lỗi cấu hình/provider/timeout trả `503 AI_PROVIDER_UNAVAILABLE`; API chỉ đọc, không tạo RecipePost hoặc ghi Lịch ăn. FE chỉ gọi POST sau thao tác chủ động, giữ gợi ý thường khi AI lỗi và bỏ phản hồi cũ khi chuyển bài. Khóa vẫn dùng `GEMINI_API_KEY` ở Backend và cơ chế Compose hiện có.
- Tích hợp FR-11: `AiUsageEvent` mang `userId`, `feature=FR-47`, `occurredAt` và các token count thực tế từ provider; thiếu metadata giữ `null`, không suy đoán bằng 0. Event chỉ phát sau khi phản hồi hợp lệ. Consumer lưu trữ tối đa 90 ngày/báo cáo do Issue #17 triển khai; FR-47 chưa cung cấp consumer lưu trữ. Không đưa prompt, nội dung công thức hoặc API key vào event/log.

## 14. Gợi ý món và menu tuần AI — FR-34

Các API yêu cầu Bearer token của Member đang hoạt động và hồ sơ FR-31 đủ ba nhóm thông tin. Backend lọc ứng viên công khai theo loại ăn chay trong hồ sơ và nguyên liệu cần tránh trước khi gửi catalog rút gọn tới Gemini. Lựa chọn Gemini phải thuộc ID trong catalog; Backend tải lại các bài công khai hiện hành trước khi trả dữ liệu. Không gửi nội dung hồ sơ riêng tư hoặc prompt người dùng vào telemetry/log.

| Thao tác | API | Quyền và kết quả |
|---|---|---|
| Gợi ý 3–5 món theo nguyên liệu | `POST /api/v1/recipes/ai/recommendations` | Body `{ "ingredientIds": [12, 29] }`; nguyên liệu được lấy từ `GET /api/v1/recipes/ingredient-options?query=...`. Trả `{ "items": [{ "recipe": RecipePostResponse, "reason": "..." }] }`. Công thức khớp nhiều nguyên liệu đã chọn được ưu tiên. Free Member được gọi theo FR-10. |
| Tạo menu tuần xem trước | `POST /api/v1/recipes/ai/weekly-menu` | Cùng body `ingredientIds`; yêu cầu Pro còn hiệu lực. Trả 21 `slots`, theo thứ tự ngày 0–6 và sáng/trưa/tối; mỗi mục gồm `dayOffset`, `mealType`, `recipeId`, thông tin thẻ món và lý do. Có thể lặp công thức nếu catalog ít, nhưng hai vị trí liên tiếp không dùng cùng ID. API chỉ tạo preview trong response, không lưu Meal Plan. |
| Xác nhận thêm menu vào Lịch ăn | `POST /api/v1/meal-plans/{weekStartDate}/ai-menu` | Yêu cầu Pro còn hiệu lực, Bearer token và ngày Thứ Hai. Body gồm đúng 21 mục `slots`, mỗi mục `{ "dayOffset": 0, "mealType": "BREAKFAST", "recipeId": 123 }`. Backend giữ các mục hiện có và thêm menu trong một transaction. Trả tuần đã cập nhật. |

API gợi ý và preview trả `409 DIETARY_PROFILE_INCOMPLETE` khi hồ sơ chưa đủ; weekly preview và thao tác xác nhận trả `403 AI_SUBSCRIPTION_REQUIRED` nếu tài khoản không có Pro còn hiệu lực. Không đủ ứng viên để tạo menu trả `409 AI_MENU_UNAVAILABLE`; lỗi provider trả `503 AI_PROVIDER_UNAVAILABLE`; phản hồi AI sai hoặc chứa ID ngoài catalog trả `502 AI_RESPONSE_INVALID`. Khi xác nhận, công thức đã ẩn/xóa trả `404 RECIPE_NOT_FOUND`; nếu cùng recipe đã có trong đúng ngày/bữa thì trả `409 MEAL_PLAN_RECIPE_DUPLICATE`. Hai trường hợp đều rollback toàn bộ, không ghi một phần. Chỉ gọi API xác nhận sau khi Member chủ động bấm áp dụng.

Mỗi phản hồi AI hợp lệ phát `AiUsageEvent` với `feature=FR-34` và token count thực tế do Gemini cung cấp. Issue #17 tiếp tục sở hữu consumer lưu trữ/báo cáo telemetry. Không có bảng preview mới và không tạo công thức mới.

## 15. AI lập thực đơn dinh dưỡng — FR-36

Generated `/v3/api-docs` là hợp đồng runtime chi tiết cho hai endpoint dưới đây. Cả hai yêu cầu Bearer token của Member đang hoạt động, gói Pro còn hiệu lực, trạng thái dinh dưỡng `ELIGIBLE` và hồ sơ nhân trắc học FR-35 hợp lệ. FR-31 onboarding không bắt buộc; ràng buộc an toàn đã khai báo vẫn được áp dụng.

- `POST /api/v1/meal-plans/ai-menu/previews` nhận `weekStartDate` (Thứ Hai), `mode` (`PROFILE` hoặc `THIS_WEEK`), `goal` tùy chọn, `preferredIngredientIds` và `avoidedIngredientIds` tùy chọn. PROFILE dùng mục tiêu FR-35 đã lưu; THIS_WEEK dùng câu trả lời chỉ trong request hiện tại. Response có 21 món theo thứ tự ngày/bữa, cover, 9 giá trị dinh dưỡng mỗi khẩu phần và lý do chọn. Preview không được lưu và không sửa Meal Plan.
- Backend chỉ chọn tối đa 50 công thức `PUBLISHED`, có mọi nguyên liệu ánh xạ gram và đủ 9 chỉ tiêu dinh dưỡng. Dị ứng/nguyên liệu cần tránh từ FR-31 và `avoidedIngredientIds` của tuần này là điều kiện loại trừ cứng trước khi gọi Gemini; mọi công thức chứa một trong các nguyên liệu đó không thuộc pool. `preferredIngredientIds` chỉ là ưu tiên mềm trong số công thức còn hợp lệ và không thể ghi đè điều kiện tránh. Gemini chỉ trả ID; Backend xác minh ID, tính lại dinh dưỡng và từ chối kết quả sai, ngoài pool hoặc lặp cùng món ở hai bữa liền kề. Pool có thể lặp công thức khác vị trí liền kề nếu không đủ 21 món khác nhau.
- `PUT /api/v1/meal-plans/ai-menu` nhận `weekStartDate`, mode và các giá trị tuần cần để tái xác minh, cùng danh sách 1–21 ô được chọn (`mealDate`, `mealType`, `recipeId`). Khi gọi API, Backend tái kiểm tra Pro, eligibility, hồ sơ, công thức công khai, pool dinh dưỡng và ràng buộc an toàn; trong một transaction, thay toàn bộ entries của tuần bằng các ô đã chọn. Bất kỳ lỗi nào rollback và giữ nguyên tuần cũ. FE phải cảnh báo rõ rằng tuần cũ sẽ bị thay trước khi gửi PUT.
- Mục tiêu chỉ điều hướng lựa chọn món, không làm thay đổi mục tiêu số FR-35, không tự suy ra thâm hụt/thặng dư năng lượng và không đảm bảo kết quả sức khỏe. Dữ liệu câu trả lời tuần không được lưu/tái sử dụng. Không gửi số đo nhân trắc học hoặc mục tiêu cá nhân chi tiết vào telemetry; không ghi prompt hay câu trả lời cá nhân vào log.
- Nếu chưa xác nhận trạng thái dinh dưỡng, API trả `403 NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED`; nếu người dùng đã xác nhận không đủ điều kiện, trả `403 NUTRITION_ELIGIBILITY_INELIGIBLE`. Hồ sơ nhân trắc học chưa lưu trả `409 NUTRITION_PROFILE_REQUIRED`.

## 16. Chatbot hỏi đáp theo ngữ cảnh — FR-51

`POST /api/v1/recipes/ai/chat` yêu cầu Bearer token của Member đang hoạt động. Endpoint nhận `contextType` (`GENERAL` hoặc `RECIPE`), `question` (tối đa 1.000 ký tự), tối đa 8 lượt `history` gần nhất (`speaker`: `USER` hoặc `ASSISTANT`, mỗi nội dung tối đa 1.000 ký tự), và `recipeId` khi dùng ngữ cảnh `RECIPE`. `GENERAL` không nhận `recipeId`; `RECIPE` bắt buộc có ID dương.

Ví dụ body cho ngữ cảnh công thức:

```json
{
  "contextType": "RECIPE",
  "recipeId": 123,
  "question": "Có thể thay nấm bằng nguyên liệu nào?",
  "history": []
}
```

Backend tải RecipePost hiện hành theo ID và chỉ dùng bài đang công khai; không nhận nội dung công thức do Frontend gửi. Context gồm nội dung công thức, nguyên liệu/định lượng, dinh dưỡng hiện có và ràng buộc ăn chay/nguyên liệu cần tránh của Member nếu đã khai báo. Khi câu hỏi yêu cầu dữ liệu cá nhân về BMI hoặc năng lượng, chatbot không tự tải hồ sơ sức khỏe vào prompt; Member cần dùng luồng hồ sơ dinh dưỡng đã có.

Response gồm `contextType`, `recipeId` (null với ngữ cảnh chung), `answer` và `recommendations` (mảng rỗng nếu không tìm công thức). Khi người dùng tìm món/công thức, Backend tải tối đa 50 Recipe Post công khai, lọc trường phái ăn chay và nguyên liệu cần tránh đã khai báo rồi gửi danh sách giới hạn cho Gemini; Gemini chỉ được chọn tối đa 4 ID trong danh sách. Backend xác minh ID và trạng thái công khai lần nữa trước khi trả các thẻ bài viết gồm `id`, `title`, `description`, `coverUrl`, `totalTimeMinutes`, `difficulty` và `reason`. FE dùng `id` để mở trang `/cong-thuc/id/{id}`; không dùng URL do Gemini tạo. Nếu không tìm được bài phù hợp, `recommendations` rỗng và `answer` nói rõ không có kết quả. Nội dung công thức mới hoặc biến tấu chỉ được trả trong `answer` khi người dùng yêu cầu rõ; đây là bản nháp AI trong hội thoại, không lưu hoặc đăng.

Ví dụ phần response cho yêu cầu tìm món:

```json
{
  "contextType": "GENERAL",
  "recipeId": null,
  "answer": "Mình tìm được một số bài công thức phù hợp:",
  "recommendations": [
    {
      "id": 123,
      "title": "Đậu hũ sốt cà chua",
      "description": "Món chay dễ làm cho bữa cơm gia đình.",
      "coverUrl": "https://storage.example/recipe-cover.jpg",
      "totalTimeMinutes": 25,
      "difficulty": "EASY",
      "reason": "Cách làm đơn giản và thời gian chuẩn bị ngắn."
    }
  ]
}
```

Gemini phải phân biệt dữ kiện ghi trong RecipePost với gợi ý/ước tính của AI, không bịa thông tin còn thiếu, tôn trọng loại ăn chay và không chẩn đoán hoặc kê phác đồ điều trị. API chỉ đọc; không lưu lịch sử hội thoại hay thay đổi dữ liệu nghiệp vụ. Frontend giữ tối đa lịch sử cần gửi trong state của phiên hiện tại. Backend chỉ phát telemetry token `AiUsageEvent` sau phản hồi hợp lệ; không đưa câu hỏi, lịch sử, context công thức hoặc câu trả lời vào telemetry/log.

Lỗi request trả `400`; bài không tồn tại hoặc không còn công khai trả `404`; phản hồi Gemini sai/thiếu trả `502 AI_RESPONSE_INVALID`; Gemini lỗi hoặc timeout trả `503 AI_PROVIDER_UNAVAILABLE`. Backend giới hạn thời gian chờ Gemini theo NFR-18. Guest chưa được bật trên endpoint này; chỉ mở khi giới hạn tần suất Guest thuộc FR-02 có mặt trên baseline tích hợp. Generated `/v3/api-docs` là contract chi tiết cho request, response, validation và security.

## 17. Theo dõi thành viên — FR-59

Generated OpenAPI runtime là contract chi tiết; mục này ghi quy ước dùng chung. Quan hệ có hướng lưu trong `USER_FOLLOW` (có từ V1), người theo dõi luôn là tài khoản trong phiên (BR-75).

- `GET /api/v1/members/{userId}/follow` (công khai) trả `FollowStatusResponse`: `userId`, `following` (người xem có đang theo dõi không; Guest luôn `false`), `followerCount`, `followingCount`. Hai bộ đếm chỉ tính tài khoản `ACTIVE` (AC-59.7). Hồ sơ Member bị `LOCKED` vẫn đọc được (Q55).
- `PUT /api/v1/members/{userId}/follow` và `DELETE /api/v1/members/{userId}/follow` (cần Bearer token) đều idempotent và trả `FollowStatusResponse` mới: gọi lặp không tạo bản ghi trùng hay làm sai bộ đếm (AC-59.4, AC-59.5). Chỉ Customer/Expert đang hoạt động được gọi; Administrator nhận `403 MEMBER_ACCESS_REQUIRED`, tự theo dõi nhận `403 FOLLOW_SELF_NOT_ALLOWED`, theo dõi Member `LOCKED` nhận `409 FOLLOW_TARGET_INACTIVE`.
- `GET /api/v1/members/{userId}/followers` và `GET /api/v1/members/{userId}/following` (cần Bearer token) trả `PageResponse` gồm `userId`, `displayName`, `avatarUrl`; không có email hay dữ liệu đăng nhập (AC-59.6). Chỉ gồm tài khoản `ACTIVE`, mới theo dõi gần đây trước; `page` từ 0, `size` 1–50 (mặc định 20).
## 18. Bình luận và reply lồng nhiều cấp — FR-46

Generated OpenAPI runtime là contract chi tiết; mục này ghi quy ước dùng chung. Dữ liệu lưu trong `COMMENT` (có từ V1); `CK_COMMENT_depth` và `FK_COMMENT_PARENT` giữ cây tối đa 5 cấp và reply cùng bài với cha (BR-66).

- `GET /api/v1/recipes/{recipeId}/comments?page=0&size=10` (công khai) chỉ cho bài `PUBLISHED` (khác thì `404 RECIPE_NOT_FOUND`). Trả `RecipeCommentPageResponse`: `content` là các luồng gốc, mới nhất trước; mỗi `CommentResponse` có `commentId`, `parentCommentId`, `depth`, `content`, `author` (`userId`, `displayName`, `avatarUrl`), `deleted`, `edited`, `createdAt` và `replies` (cũ trước, đúng thứ tự hội thoại). `totalElements` đếm luồng gốc còn hiển thị, `commentCount` đếm bình luận và reply chưa xóa; `size` 1–50.
- Bình luận đã xóa còn reply hiển thị được trả về với `deleted = true`, `content` và `author` là `null` (tombstone, AC-46.2); đã xóa và không còn reply nào hiển thị thì không trả về (AF-46.3).
- `POST /api/v1/recipes/{recipeId}/comments` (cần Bearer token) body `{ "content", "parentCommentId"? }`, trả `201` với bình luận mới. Chỉ Customer/Expert đang hoạt động; Administrator nhận `403 MEMBER_ACCESS_REQUIRED`. `content` bỏ khoảng trắng đầu cuối, 1–1.000 ký tự, sai thì `400 VALIDATION_FAILED` với `errors[].field = content`. Trả lời bình luận cấp 5 được gắn vào cha cấp 4 nên vẫn ở cấp 5 (AC-46.1); Frontend chèn `@Tên` vào đầu nội dung.
- `PUT /api/v1/comments/{commentId}` body `{ "content" }` và `DELETE /api/v1/comments/{commentId}` (cần Bearer token): chỉ chủ bình luận; người khác nhận `403 COMMENT_EDIT_NOT_ALLOWED`. Sửa thì `edited = true`. Xóa trả `204`, gọi lặp an toàn, luôn là xóa mềm (`is_deleted`) vì `REPORT`/`NOTIFICATION` có thể tham chiếu bình luận.
- Nội dung là văn bản thuần: Backend lưu nguyên văn, Frontend hiển thị như text nên mã HTML/JavaScript không được thực thi (AC-46.3).
