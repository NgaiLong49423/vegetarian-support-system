> **Document:** API Integration Guide
> **File:** `docs/api/API.md`
> **Version:** v0.11.0
> **Created:** 2026-09-20
> **Last Updated:** 2026-10-10
> **Status:** Active

# API Integration Guide

## 1. Mục đích và phạm vi

Tài liệu này hướng dẫn Frontend, Backend và tester tích hợp với API Mâm Xanh. Generated OpenAPI từ Spring Boot là runtime contract cho endpoint đã triển khai; trong giai đoạn migration, [OpenAPI YAML](openapi.yaml) là planned/reference contract cho endpoint chưa implement. Tài liệu này không lặp lại schema chi tiết.

Backend source trong branch gồm đăng ký, xác minh email, đăng nhập bằng mật khẩu và bằng Google (FR-03), danh mục quản trị nguyên liệu/đơn vị/quy đổi (FR-18), xác nhận eligibility dinh dưỡng (FR-38) và hồ sơ dinh dưỡng tham khảo (FR-35), đọc chi tiết RecipePost và Meal Plan (FR-20), danh sách RecipePost đã lưu (FR-20/FR-32), và nộp báo cáo Recipe Post (FR-26/27). Generated OpenAPI runtime là contract cho các endpoint đã triển khai; planned/reference YAML chỉ giữ password recovery chưa có trong runtime.

Nhóm đã chấp nhận baseline API hiện có để phân rã và chuẩn bị triển khai FR-03. Các thông số còn mở ở mục 8 phải được owner đề xuất và Tech Lead duyệt trước khi triển khai phần phụ thuộc vào chúng. Trạng thái tài liệu `Active` không phải bằng chứng endpoint đã được triển khai hoặc chạy thành công.

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
| `403 Forbidden` | Tài khoản chưa xác minh (`EMAIL_NOT_VERIFIED`), bị Administrator khóa (`ACCOUNT_LOCKED`) hoặc không phải Member khi dùng hồ sơ sở thích (`MEMBER_ACCESS_REQUIRED`). |
| `409 Conflict` | Email đã được sử dụng, đã liên kết với tài khoản Google khác (`GOOGLE_ACCOUNT_CONFLICT`), hoặc hồ sơ sở thích chưa đủ để gọi AI cá nhân hóa (`DIETARY_PROFILE_INCOMPLETE`). |
| `429 Too Many Requests` | Chỉ áp dụng khi contract của endpoint quy định `429` (ví dụ login, resend verification hoặc AI rate limiting); client đọc `Retry-After` khi có. Password-reset request không trả `429`. |
| `503 Service Unavailable` | Dịch vụ phụ thuộc tạm thời không dùng được, ví dụ không tải được chứng chỉ Google (`GOOGLE_LOGIN_UNAVAILABLE`). |

Mã `code` đã triển khai (Issue #5, #6, #8, #36). Các mã của đặt lại mật khẩu được bổ sung khi Issue tương ứng triển khai.

| `code` | Status | Khi nào |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Payload sai định dạng hoặc vi phạm validation; chi tiết theo field nằm trong `errors`. Mật khẩu yếu trả một phần tử `errors` cho mỗi tiêu chí còn thiếu. |
| `EMAIL_ALREADY_USED` | 409 | `POST /auth/register` với email đã có tài khoản (không phân biệt hoa/thường). |
| `VERIFICATION_TOKEN_INVALID` | 400 | `POST /auth/email-verifications` với mã không tồn tại, đã dùng, đã bị thay bằng mã mới hoặc hết hạn. |
| `RESEND_TOO_SOON` | 429 | `POST /auth/email-verifications/resend` trong vòng 60 giây kể từ email xác minh trước; kèm `Retry-After`. |
| `INVALID_CREDENTIALS` | 401 | `POST /auth/login` với email không tồn tại, sai mật khẩu hoặc tài khoản chỉ đăng nhập Google (không có mật khẩu); mọi trường hợp cùng `detail` trung tính "Email hoặc mật khẩu không chính xác." |
| `EMAIL_NOT_VERIFIED` | 403 | `POST /auth/login` đúng mật khẩu nhưng email chưa xác minh. |
| `ACCOUNT_LOCKED` | 403 | `POST /auth/login` đúng mật khẩu nhưng tài khoản bị Administrator khóa; `POST /auth/google` cho tài khoản đang `LOCKED`; hoặc request mang Bearer token hợp lệ của tài khoản đang `LOCKED`. |
| `LOGIN_TEMPORARILY_BLOCKED` | 429 | Lần sai mật khẩu thứ 5 liên tiếp và mọi lần thử `POST /auth/login` trong 10 phút sau đó (kiểm tra trước khi so mật khẩu); kèm `Retry-After` là số giây còn lại. Không đổi `account_status` và không áp dụng cho `POST /auth/google`. |
| `GOOGLE_TOKEN_INVALID` | 401 | `POST /auth/google` với ID Token sai định dạng, sai chữ ký, hết hạn, sai issuer/audience, thiếu `sub`/email, email Google chưa xác minh hoặc không lưu được; cũng trả khi Backend chưa cấu hình `MAMXANH_GOOGLE_CLIENT_ID`. |
| `GOOGLE_ACCOUNT_CONFLICT` | 409 | `POST /auth/google` khi email đã liên kết với một tài khoản Google khác. |
| `GOOGLE_LOGIN_UNAVAILABLE` | 503 | `POST /auth/google` khi Backend không tải được chứng chỉ ký của Google. |
| `MEMBER_ACCESS_REQUIRED` | 403 | Tài khoản Administrator gọi endpoint hồ sơ sở thích ăn uống (FR-31); chỉ Member (`CUSTOMER`, `EXPERT`) có hồ sơ này. |
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
- Password-reset request luôn trả cùng HTTP 202 trung tính; silent rate limiting vẫn enforce cooldown 60 giây và tối đa 5 email/giờ/tài khoản. Khi vượt limit hoặc email không tồn tại, không gửi email; response không tiết lộ account existence hay trạng thái rate limit.
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
- `PUT /nutrition/profile` lưu hồ sơ FR-35 và đồng thuận xử lý dữ liệu sức khỏe theo hợp đồng FR-35. Việc này không tự xác nhận hoặc đổi trạng thái eligibility.
- `POST /nutrition/profile/calculate` trả BMI cùng 8 thành phần dinh dưỡng (9 chỉ tiêu khi tính cả năng lượng) khi hồ sơ lưu hợp lệ. Phản hồi tính toán không được lưu thành lịch sử theo dõi.
- Guard dùng chung cho endpoint dinh dưỡng cá nhân FR-36/FR-37 cần được tích hợp tại các API do các FR đó sở hữu; chỉ endpoint đã tồn tại và được bảo vệ mới có bằng chứng nghiệm thu.
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

Các key trong `nutrition.total` và `nutrition.perServing` lần lượt là `ENERGY_KCAL` (kcal), `PROTEIN_G` (g), `CARBOHYDRATE_G` (g), `TOTAL_FAT_G` (g), `FIBER_G` (g), `CALCIUM_MG` (mg), `IRON_MG` (mg), `VITAMIN_B12_MCG` (mcg) và `ZINC_MG` (mg). Nếu chưa có lượt bình chọn, `statistics.reactionCount` bằng `0` và `statistics.likePercentage` là `null`; Frontend hiển thị nhãn “Mới” theo BR-69.

Nutrition chỉ dùng danh mục nội bộ và quy đổi gram hiện có. `nutrition.complete=false` cùng `ingredientsMissingData` chỉ rõ nguyên liệu tùy chỉnh/thiếu dữ liệu hoặc thiếu quy đổi; không ngầm coi dữ liệu chưa biết là giá trị đo bằng không. Đây là số liệu tham khảo, không phải tư vấn y tế. Không gọi dịch vụ dinh dưỡng ngoài.

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
