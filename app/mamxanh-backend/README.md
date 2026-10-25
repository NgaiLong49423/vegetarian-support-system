> **Document:** Backend Workspace Guide  
> **File:** `app/mamxanh-backend/README.md`  
> **Version:** v0.41.0
> **Created:** 2026-06-14  
> **Last Updated:** 2026-10-25
> **Status:** Active  

# Backend Workspace

## Tổng hợp nguyên liệu — FR-54 / Issue #54

`shopping/service/ShoppingAggregationService.aggregate(List<ShoppingAggregationLine>)` là biên tính toán nội bộ cho FR-53. Đầu vào gồm `ingredientId` (bắt buộc đối với dòng công thức; `null` chỉ dành cho mặt hàng thủ công), `customName`, `quantity` đã được caller tính theo khẩu phần, và `unitId`. Kết quả `ShoppingAggregationResult` chứa các nhóm danh mục, các dòng tổng hợp và cảnh báo có chỉ số dòng nguồn. Service không ghi Shopping List và không gọi AI/API ngoài.

- Chỉ gộp cùng `ingredientId`; tên/nhóm lấy từ catalog. Tự quy đổi đúng hai cặp chuẩn `g/kg`, `ml/L`, trả đơn vị cơ sở khi có trong catalog. Cùng đơn vị đếm giữ nguyên đơn vị; không suy diễn rằng mọi đơn vị `COUNT` tương đương nhau.
- Khác nhóm đơn vị chỉ gộp được qua `INGREDIENT_UNIT_CONVERSION` đúng nguyên liệu, với cả nguyên liệu, đơn vị và tỷ lệ còn hoạt động. Một tỷ lệ ml hoặc L có thể dùng qua quan hệ chuẩn 1 L = 1000 ml; các tỷ lệ đang mâu thuẫn khiến nhóm đó giữ riêng và có cảnh báo. Muỗng/thìa không tự đổi sang ml chỉ dựa vào `base_factor`.
- Mặt hàng thủ công chỉ gộp tên giống nhau không phân biệt hoa thường sau trim và cùng `unitId`; không gộp với dòng catalog hoặc tự đổi g/kg cho mặt hàng thủ công. Dòng không tương thích được giữ riêng với `incompatibleUnits=true`; kết quả phân nhóm theo danh mục, mặt hàng thủ công thuộc `Nhóm khác`.
- Phép cộng/nhân/chia chuẩn dùng `BigDecimal`, không làm tròn hoặc ghi về precision của SQL ở bước tổng hợp. FR-53 phải xử lý precision khi lưu để không biến lượng nhỏ thành 0. Dòng thiếu số lượng, số lượng <= 0, nguyên liệu/đơn vị không tồn tại hoặc tên thủ công không hợp lệ được bỏ qua kèm cảnh báo; đầu vào rỗng trả kết quả rỗng, thiếu toàn bộ danh sách trả `VALIDATION_FAILED`.
- Catalog và quy đổi được đọc theo batch qua public service của module `nutrition`; tham chiếu lịch sử ngừng sử dụng vẫn có tên/đơn vị để hiển thị, nhưng không dùng tỷ lệ đã ngừng sử dụng để gộp khác dimension.

**Ranh giới tích hợp:** engine thuộc FR-54 / PR #150, chưa phải baseline `develop`. Nhánh FR-53 kế thừa engine để triển khai caller bên dưới; PR #150 phải được tích hợp trước khi FR-53 sẵn sàng merge. Engine không tự xử lý đăng nhập, quyền sở hữu hoặc lưu checklist.

## Checklist mua sắm — FR-53 / Issue #53

- `/api/v1/shopping-lists` cung cấp danh sách theo tài khoản, đọc checklist, đổi tên/xóa danh sách, thêm/sửa/xóa item, đặt trạng thái đã mua và xóa các item đã mua. Backend lấy chủ sở hữu từ phiên đăng nhập; Guest/Admin/tài khoản khóa và truy cập danh sách của người khác bị chặn. Xóa danh sách xóa provenance trước item, không xóa Meal Plan hoặc Recipe Post nguồn.
- `POST /api/v1/shopping-lists` tạo checklist rỗng loại `MANUAL` với tên Member nhập; danh sách độc lập với Meal Plan và danh sách công thức `PERSONAL`, trả `201` và mở được ngay để thêm mặt hàng thủ công. Migration V21 mở rộng constraint loại danh sách; snapshot schema được đồng bộ.
- `POST /from-meal-plan` tạo checklist độc lập từ cả tuần hoặc các ngày đã chọn, tính lượng theo khẩu phần dự kiến, tổng hợp qua FR-54 rồi lưu transaction. Tên danh sách thể hiện ngày thực đơn, không ngụ ý ngày mua nguyên liệu.
- `POST /{listId}/recipes` thêm các nguyên liệu được chọn của một công thức công khai cùng các mặt hàng thủ công tùy chọn vào danh sách Member; `POST /from-recipe` tạo danh sách `MANUAL` có tên và lưu các mục trong cùng transaction. Bỏ `selectedIngredientIds` để chọn toàn bộ nguyên liệu; hỗ trợ khẩu phần 1–50, kiểm tra quyền sở hữu và không cộng trùng đóng góp công thức. Sửa/ẩn/xóa bài không tự thay đổi lượng hoặc trạng thái checklist; nguồn được đọc theo nội dung hiện hành hoặc hiển thị unavailable, theo thiết kế đã chốt trong #90.
- Migration shopping chưa merge được chuyển sang V18 (đơn vị `cuộn`) và V19 (loại danh sách/đóng góp Recipe Post), giữ nguyên V16/V17 dinh dưỡng của develop. Không sửa Flyway history, reset database dùng chung hoặc thay đổi diagram.
- Clipboard/TXT thuộc FR-55, được bàn giao riêng. Scalar và generated OpenAPI mô tả hợp đồng shopping hiện hành.

Kiểm thử tập trung: `.\mvnw.cmd "-Dtest=ShoppingAggregationServiceTest,ShoppingAggregationIntegrationTest,IngredientCatalogServiceTest" test`. Unit tests bao phủ AC-54.1–54.5 và trường hợp biên; integration tests dùng catalog/quy đổi SQL Server Testcontainers thật, fixture rollback. Không mô tả các test này là kiểm thử UI hoặc toàn bộ luồng tạo danh sách.

Kiểm chứng ngày 2026-10-17 trên nhánh FR-54: 67/67 test tập trung đạt; toàn Backend `clean verify` đạt 881/881 test, không failure/error/skip. JaCoCo BUNDLE: Lines 94,87%, Branches 82,97%, Methods 93,42%, Instructions 94,31%; cả bốn đều vượt cổng `>80%`. Đây là bằng chứng local của bộ tổng hợp và baseline Backend, không thay thế required CI/review hoặc nghiệm thu sau merge.

Thư mục này dành cho REST API Java 21 + Spring Boot, build bằng Maven và truy cập Microsoft SQL Server qua Spring Data JPA/Hibernate theo [Technology Stack](../../docs/architecture/TECHNOLOGY-STACK.md).

> [!IMPORTANT]
> **Quy chuẩn kiến trúc bắt buộc:** Mọi thành viên và AI agent khi triển khai mã nguồn Backend **bắt buộc phải tuân thủ nghiêm ngặt** cấu trúc package Modular Monolith, phân tầng MVC và ranh giới liên module được quy định tại [Backend Package Structure Specification](../../docs/architecture/BACKEND-PACKAGE-STRUCTURE-PROPOSAL.md). Cấm inject chéo Repository giữa các module và không trả JPA Entity trực tiếp ra REST API.

### Cấu hình ảnh Azure — FR-14 / fix #158

- Điền `AZURE_STORAGE_CONNECTION_STRING` và `AZURE_STORAGE_CONTAINER_NAME` trong `.env` Backend đã được Git ignore. Root Compose truyền hai biến vào container; sau khi thay `.env`, chạy lại `docker compose -p <project> --env-file app/mamxanh-backend/.env up --build -d --wait` tại root với đúng tên project đang dùng. Không đưa connection string vào Frontend.
- Ảnh công khai cần container cho phép đọc Blob. Khi thiếu cấu hình hoặc Azure không khả dụng, Backend vẫn phục vụ tính năng khác nhưng upload ảnh trả `503 MEDIA_STORAGE_UNAVAILABLE`, không tạo URL giả. URL mock của ảnh cũ không có dữ liệu để khôi phục; người dùng cần tải lại ảnh.
- Chạy test storage/media theo lệnh Maven bên dưới; kiểm tra Azure thật qua test opt-in được mô tả trong [Frontend README](../mamxanh-frontend/README.md). Storage dùng chung cho avatar nên avatar cũng cần Azure được cấu hình đúng.

### Backend bình chọn công thức — FR-57 / Issue #56

- Module `recipe` bổ sung `GET`, `PUT`, `DELETE /api/v1/recipes/{recipeId}/reaction`, yêu cầu Bearer token của Customer/Expert đang hoạt động. Response trả `recipeId`, `currentReaction`, `likes`, `dislikes`, `reactionCount`, `likePercentage` và header `Cache-Control: no-store`; generated OpenAPI tại `/v3/api-docs` là contract chi tiết.
- PUT nhận `{"reactionType":"LIKE"}` hoặc `DISLIKE`, đặt/chuyển lựa chọn; gửi lại cùng lựa chọn không hủy vote và không thay `updated_at`. DELETE hủy lựa chọn, trả thống kê mới; gửi DELETE lặp không thay đổi dữ liệu. Frontend cần dùng DELETE khi Member bấm lại nút đang chọn để đáp ứng toggle off.
- Tái sử dụng `RECIPE_REACTION`, không thêm bảng/migration. Mutation khóa bản ghi Recipe Post trong transaction trước khi đọc/ghi reaction; mỗi Member chỉ có một phản hồi trên một bài. Tác giả tự bình chọn trả `403 RECIPE_SELF_REACTION_NOT_ALLOWED`; bài không công khai trả `404 RECIPE_NOT_FOUND`. Guest không được dùng API phản hồi cá nhân, nhưng vẫn xem thống kê qua API công thức công khai.
- API phản hồi, danh sách và chi tiết dùng chung `RecipeReactionStatistics`: tỷ lệ Like làm tròn thành số nguyên (10 Like/1 Dislike → 91%), `null` khi chưa có vote, `0` khi có vote nhưng toàn Dislike. Kiểu dữ liệu response và cách xếp hạng hiện có được giữ nguyên. Giai đoạn 4 đã tích hợp UI phản hồi tại trang chi tiết API và huy hiệu tại thẻ Trang chủ/Khám phá; xem [Frontend Workspace Guide](../mamxanh-frontend/README.md). Chưa xác nhận toàn bộ FR-57 đã nghiệm thu.
- Kiểm tra giai đoạn 2: 35 unit/regression tests pass; 25 kiểm tra HTTP/SQL Server local thật pass. Integration test `RecipeReactionIntegrationTest` đã được bổ sung nhưng lượt chạy Testcontainers không khởi động được vì máy chưa có Docker. Chưa xác minh full Compose, toàn bộ coverage gate hoặc UI/browser.
- Kiểm tra giai đoạn 3 (11/10/2026): 44 unit/regression tests và 45 kiểm tra HTTP/SQL Server local thật pass, gồm thống kê nhất quán giữa ba API và loại bài HIDDEN/DELETED khỏi MOST_LIKED/MOST_ACTIVE/TRENDING. Fixture riêng được dọn sau kiểm tra. Testcontainers/Compose vẫn chưa có bằng chứng pass do Docker chưa khả dụng.
- Kiểm tra giai đoạn 5 (11/10/2026): sau sửa validation ID, 51 kiểm thử Backend liên quan pass, 45 kiểm tra HTTP/SQL và 13 kiểm tra trình duyệt–HTTP–SQL thật pass; đáp ứng AC-57.1–AC-57.5 ở môi trường local. Sửa `recipeId` 0/-1 trả 500 thành `400 VALIDATION_FAILED` bằng validation MVC hiện hành, thêm 7 test Controller (BUG-030). Fixture riêng được dọn, không bật `local-reset`.
- Lượt full `clean verify` đầu tiên: 312 test, 175 pass, 137 lỗi khởi tạo Testcontainers do không có Docker; không được coi là build pass. Báo cáo JaCoCo sau các lượt kiểm tra bổ sung: Lines 62,9%, Branches 60,1%, Methods 59,2%, Instructions 67,0%; strict gate >80% fail trên coverage từng phần. Không hạ threshold hoặc loại production source.
- Xác minh tiếp giai đoạn 5 (11/10/2026), sau khi Docker khả dụng: `clean verify` đạt 319/319 test, không failure/error/skipped, gồm 10 test `RecipeReactionIntegrationTest` trên SQL Server container. Strict JaCoCo BUNDLE gate >80% pass: Lines 93,8%, Branches 82,3%, Methods 90,5%, Instructions 94,1%.
- Root Compose với project riêng `mamxanh-fr57` build/chạy thành công; FE/BE/SQL đều healthy và database-init thoát 0. FE, generated OpenAPI và route Scalar trả HTTP 200; OpenAPI có đủ GET/PUT/DELETE phản hồi. 13 kiểm tra trình duyệt–HTTP–SQL FR-57 và 1 test đăng nhập/throttle thật trên Compose pass. Fixture phản hồi được dọn; `down` giữ named volumes. Route smoke chưa chứng minh Scalar render/gửi request trong trình duyệt. CI, Sonar/CodeQL trên thay đổi này và nghiệm thu sau merge vẫn chưa xác minh.

## Trạng thái hiện tại

Backend đã được scaffold thành công với Java 21 và Spring Boot:
- Cấu hình Maven project độc lập và đính kèm Maven Wrapper (`mvnw`, `mvnw.cmd`).
- Khởi tạo ứng dụng chính `MamXanhApplication` tại package `tech.mamxanh`.
- Cấu hình sẵn các dependency cốt lõi:
  - **Data / Persistence:** `spring-boot-starter-data-jpa`, `mssql-jdbc` (SQL Server driver), Flyway migration (`spring-boot-starter-flyway`, `flyway-sqlserver`).
  - **Security:** `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` (xác thực JWT Bearer bằng Nimbus).
  - **Web / Validation:** `spring-boot-starter-webmvc`, `spring-boot-starter-validation`.
  - **Documentation:** `springdoc-openapi-starter-webmvc-ui` (generated OpenAPI 3 + Swagger UI compatibility) and Scalar API Reference static browser UI.
  - **Email:** `spring-boot-starter-mail` (Brevo SMTP, gửi bất đồng bộ).
  - **Productivity & Testing:** Lombok, starter test dependencies (`data-jpa-test`, `flyway-test`, `security-test`, `validation-test`, `webmvc-test`), Testcontainers SQL Server (`spring-boot-testcontainers`, `testcontainers-mssqlserver`).
- Đã có luồng FR-03-A (Issue #5): `POST /api/v1/auth/register`, `/auth/email-verifications`, `/auth/email-verifications/resend`; lỗi trả `application/problem+json` có `code` ổn định; migration `V3__user_email_verification_token.sql`.
- FR-35 bổ sung consent bằng migration `V4__nutrition_profile_consent.sql`; FR-18 bổ sung ingredient group và kiểm tra unit bằng migration `V5__ingredient_group_and_unit_validation.sql`.
- Đã có luồng FR-03-B (Issue #6): `POST /api/v1/auth/login` phát Stateless JWT (HS256), khóa đăng nhập tạm 10 phút sau 5 lần sai liên tiếp theo tài khoản (migration `V6__user_login_throttle.sql`), và mọi request mang Bearer token đều kiểm tra `USER.account_status`.
- Đã có luồng FR-03-D (Issue #8): `POST /api/v1/auth/google` xác minh Google ID Token bằng `GoogleIdTokenVerifier` (`com.google.api-client:google-api-client`, audience `MAMXANH_GOOGLE_CLIENT_ID`), tìm tài khoản theo `google_subject` rồi theo email, tạo hoặc liên kết tài khoản theo [API Guide mục 3.4](../../docs/api/API.md) và phát cùng `AuthResponse` như đăng nhập mật khẩu. Không có migration mới: cột và unique index `google_subject` đã có từ baseline. Test không gọi Google: test tích hợp giả lập `GoogleTokenVerifier`, còn test adapter dùng chứng chỉ tự ký phục vụ qua transport giả.
- Đã có luồng FR-03-E (Issue #9): `POST /api/v1/auth/password-resets` luôn trả `202` trung tính và chỉ gửi email cho tài khoản `ACTIVE` ngoài cooldown 60 giây, tối đa 5 email/giờ/tài khoản; `POST /api/v1/auth/password-resets/confirm` đặt mật khẩu mới bằng token 15 phút (chỉ lưu SHA-256) và trả `204`. Migration `V11__user_password_reset.sql` thêm token và metadata rate limit trên `USER`; quy tắc chi tiết ở [API Guide mục 3.5](../../docs/api/API.md). Thời hạn và giới hạn cấu hình bằng `mamxanh.auth.password-reset.*` trong `application.properties`.
- FR-05 (Issue #68) triển khai luồng nộp/xem lịch sử đơn Chuyên gia và Admin xét duyệt qua Backend API; phê duyệt đổi `CUSTOMER` thành `EXPERT` và ghi notification trong cùng transaction. Migration `V8__expert_application_notifications.sql` bổ sung internal target path cùng index truy vấn. Chi tiết API được tạo từ runtime OpenAPI.
- Đã có FR-31 (Issue #36) trong module `nutrition`: `GET`/`PUT /api/v1/nutrition/dietary-preferences`, `POST /api/v1/nutrition/dietary-preferences/onboarding/skip`, `POST /api/v1/nutrition/dietary-preferences/onboarding/invitation` (lời mời Onboarding chỉ hiện một lần, AC-31.10) và `GET /api/v1/nutrition/dietary-preferences/ingredient-suggestions`; entity riêng ánh xạ các cột sở thích của `USER` (quyết định Q18) và bảng `USER_INGREDIENT_PREFERENCE`; migration `V7__user_onboarding_invitation.sql` thêm cột `USER.onboarding_invited_at`. Generated OpenAPI khai báo security scheme `bearerAuth` (`common/config/OpenApiConfig`); controller cần đăng nhập gắn `@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)`. Endpoint AI cá nhân hóa sau này phải gọi `DietaryPreferenceService.requirePersonalizedAiEligible(userId)` trước khi gọi Gemini (BR-31).
- FR-26/27 thêm `POST /api/v1/recipes/{recipeId}/reports` cho Member gửi báo cáo bài công khai; báo cáo lưu `OPEN`, xác thực và chống trùng ở Backend, migration V9 đồng bộ trạng thái/mã lý do với SRS. Luồng bổ sung thông tin thuộc FR-30 chưa có trong Issue này.
- FR-28 (Issue #33) thêm hàng đợi Administrator dưới `/api/v1/admin/reports`: danh sách theo trạng thái, số lượng từng trạng thái, chi tiết, tiếp nhận (`OPEN` → `IN_REVIEW`) và giải quyết với kết luận `NO_VIOLATION`, `WARN_AUTHOR`, `HIDE_RECIPE` hoặc `LOCK_AUTHOR` kèm lý do bắt buộc. Chỉ Administrator đang giữ báo cáo được ghi kết luận, báo cáo `RESOLVED` không sửa được. Ẩn bài đi qua `RecipeModerationService` (module `recipe`), khóa tài khoản qua `AccountModerationService` (module `auth`), thông báo trong app cho người báo cáo và tác giả qua `ModerationNotificationService` (module `notification`) và không chứa danh tính người báo cáo. Migration `V22__report_handling_by_admin.sql` thêm `REPORT.handled_by` và CHECK cho `decision`. Quy tắc chi tiết ở [API Guide mục 21](../../docs/api/API.md).
- Đã có FR-23 (Issue #29): `GET /api/v1/members/{userId}` và `GET /api/v1/members/{userId}/recipes` công khai; `GET`/`PUT /api/v1/me/profile` và `POST /api/v1/me/profile/avatar` cho Member sửa tên, giới thiệu ngắn và ảnh đại diện (JPEG/PNG/WebP ≤ 2 MB, kiểm chữ ký tệp, lưu qua `StorageClient` trong thư mục `avatars/`). Không có migration: `USER.bio`, `display_name`, `avatar_url` có từ V1. Quy tắc chi tiết ở [API Guide mục 11](../../docs/api/API.md). Giới hạn multipart `spring.servlet.multipart.*` là 5 MB/tệp, 6 MB/request.
- FR-40 (Issue #45): Recipe Post dùng nguyên liệu catalog chưa có hồ sơ dinh dưỡng vẫn công khai được (BR-50); AI menu dinh dưỡng (FR-36, `AiMenuService`) chỉ lấy ứng viên `PUBLISHED` có dinh dưỡng đầy đủ, nên bài loại này không vào pool và áp dụng menu chứa bài đó bị từ chối (AC-40.3, BR-40; test `IncompleteNutritionAiMenuIntegrationTest`).
- Đã có FR-59 (Issue #69) trong module `auth`: `GET /api/v1/members/{userId}/follow` công khai (bộ đếm và trạng thái theo dõi của người xem), `PUT`/`DELETE` cùng đường dẫn để theo dõi/bỏ theo dõi (idempotent, khóa phạm vi chống ghi trùng khi gọi song song) và `GET /api/v1/members/{userId}/followers`, `/following` có phân trang cho tài khoản đã đăng nhập. Không có migration: `USER_FOLLOW` có từ V1. Quy tắc chi tiết ở [API Guide mục 17](../../docs/api/API.md).
- FR-61/FR-62 có `GET /api/v1/experts` (danh bạ Expert đang hoạt động, tìm kiếm/phân trang, follower count và trạng thái following) cùng `GET /api/v1/ingredients/groups`, `GET /api/v1/ingredients` và `GET /api/v1/ingredients/{ingredientId}`. Chi tiết nguyên liệu `ACTIVE` trả ảnh minh họa và chín chỉ tiêu dinh dưỡng trên 100 g; `NULL` là chưa biết, không chuyển thành 0. Nguyên liệu ngừng dùng và không tồn tại đều trả `404 INGREDIENT_NOT_FOUND`; API công khai không trả nguồn tham khảo, ngày tham khảo, trạng thái hoặc cờ hỗ trợ dinh dưỡng. Tái sử dụng catalog hiện có; xem [quy ước tích hợp FR-62](../../docs/api/API.md#20-danh-mục-và-chi-tiết-nguyên-liệu-công-khai--fr-62). OpenAPI snapshot được export từ runtime sau khi API thay đổi.
- FR-23 bổ sung `GET /api/v1/members/{userId}/recipe-statistics`, tổng hợp số bài công khai, lượt LIKE và lượt xem của toàn bộ Recipe Post `PUBLISHED` của một Member; tái sử dụng `RECIPE_POST`, `RECIPE_REACTION` và `RECIPE_VIEW`, không thêm migration.
- Đã có FR-46 (Issue #48) trong module `recipe`: `GET /api/v1/recipes/{recipeId}/comments` công khai (luồng gốc phân trang, reply lồng tối đa 5 cấp, tombstone), `POST` cùng đường dẫn để bình luận/trả lời, `PUT`/`DELETE /api/v1/comments/{commentId}` cho chủ bình luận. Không có migration: `COMMENT` có từ V1. Quy tắc chi tiết ở [API Guide mục 18](../../docs/api/API.md).
- Đã có FR-49 (Issue #51, phần reply) trong module `notification`: reply vào bình luận của Member khác tạo thông báo `COMMENT_REPLY` trong cùng transaction; email reply gửi sau commit trên luồng `@Async` theo `USER.reply_email_enabled` (có sẵn từ V1, mặc định bật) và lỗi gửi chỉ ghi log. API của chính tài khoản: `GET /api/v1/me/notifications`, `GET /me/notifications/unread-count`, `PUT /me/notifications/{notificationId}/read`, `PUT /me/notifications/read-all`, `GET`/`PUT /me/notification-preferences`; ID của người khác → `404 NOTIFICATION_NOT_FOUND`. Không có migration. Thông báo kết quả xử lý báo cáo (AC-49.4) do luồng FR-28 (Issue #33) tạo. Chi tiết ở [API Guide mục 22](../../docs/api/API.md).

### Lệnh chạy và kiểm tra xác minh

```bash
# Windows
.\mvnw.cmd clean test-compile
.\mvnw.cmd verify

# Linux / macOS
./mvnw clean test-compile
./mvnw verify
```

Chạy JUnit tests, package và coverage hard gate:

```powershell
# Windows
.\mvnw.cmd clean verify
# Linux / macOS
./mvnw clean verify
```

JaCoCo chạy `prepare-agent`, `report` rồi kiểm tra aggregate `BUNDLE` cho **LINE, BRANCH, METHOD và INSTRUCTION**; cả bốn metric phải **>80%** (đúng 80% vẫn fail). JaCoCo không đo Statements; Methods và Instructions là counter riêng. Checker đọc số đếm trong XML và so sánh chính xác, đồng thời ghi từng metric, `covered/total`, tỷ lệ và trạng thái vào `target/site/jacoco/strict-coverage-summary.md`. `JaCoCoCoverageGateTest` kiểm tra các biên của gate. Đây không phải new-code coverage. Reports: `target/site/jacoco/index.html` và `target/site/jacoco/jacoco.xml`. Không exclude production code để pass; thêm test có assertion phù hợp theo report. Sonar đọc XML sau khi reports được truyền qua artifact, không tạo coverage hoặc thay gate này. Coverage không thay thế Acceptance Criteria, authorization hoặc database integration evidence.

`verify` chạy cả unit test và integration test. Integration test dùng Testcontainers để khởi động cùng SQL Server container image được pin với Docker Compose (tag `2019-CU32-GDR11-ubuntu-20.04`, repository manifest digest lấy từ MCR), khởi động ứng dụng qua `MamXanhApplication.main`, chạy Flyway từ V1 và kiểm tra mapping Hibernate, nên **Docker Desktop phải đang chạy**; lần đầu cần tải image dung lượng lớn. Test dùng profile `test`, không cần file `.env` và không gửi email thật. Khi thay image/digest, phải chạy integration tests thật; compile-only không xác minh DockerImageName, SQL Server startup hay Flyway.

## Yêu cầu để chạy ứng dụng

Chọn một trong hai môi trường:

- **Chạy trực tiếp:** JDK 21. Maven không cần cài riêng vì repository có Maven Wrapper.
- **Chạy bằng Docker:** Docker Desktop đang hoạt động. Không cần cài Java/Maven trực tiếp trên máy.

Cả hai cách đều cần một Microsoft SQL Server có database `MamXanhDB`. Flyway migration `V1__baseline_schema.sql` hiện tạo baseline schema từ database rỗng; Backend vẫn không thể khởi động đầy đủ nếu SQL Server chưa sẵn sàng hoặc credential/migration validation không hợp lệ. Khi chạy toàn hệ thống bằng Docker Compose ở thư mục gốc, Compose tự tạo database trước khi Backend chạy Flyway.

Không commit username, password, connection string thật hoặc file cấu hình local chứa credential.

Khi mở toàn hệ thống, khởi động theo thứ tự:

1. Microsoft SQL Server và database `MamXanhDB`.
2. Backend tại port `8080`.
3. [Frontend](../mamxanh-frontend/README.md) tại port `5173`.

## Cách 1 — Chạy trực tiếp bằng Java 21

### 1. Xác minh Java

```powershell
java -version
javac -version
```

Cả hai lệnh phải hiển thị Java 21. Có `java` nhưng không có `javac` nghĩa là máy chưa dùng đầy đủ JDK.

### 2. Chuẩn bị cấu hình database local

Tạo hoặc cập nhật file `app/mamxanh-backend/.env` theo contract trong `.env.example`:

```properties
SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=MamXanhDB;encrypt=true;trustServerCertificate=true
SPRING_DATASOURCE_USERNAME=YOUR_LOCAL_DB_USERNAME
SPRING_DATASOURCE_PASSWORD=YOUR_LOCAL_DB_PASSWORD
```

`.env` chỉ dùng trên máy cá nhân và không được commit. File `src/main/resources/application-local.properties` được theo dõi với placeholder an toàn và nạp `.env` bằng `spring.config.import=optional:file:.env[.properties]`; không ghi credential thật vào file này.

Trước khi chạy profile `local`, đặt `MAMXANH_DEMO_PASSWORD` trong `.env` theo mục [Tài khoản demo local](#tài-khoản-demo-local). Các giá trị trong `.env.example` chỉ là placeholder, không phải credential dùng được.

Các biến môi trường cho xác thực, email và tích hợp AI (giá trị dùng chung lấy từ kho mật khẩu của nhóm, không dán vào Issue/PR):

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `MAMXANH_JWT_SECRET` | **bắt buộc** | Khóa ký HS256 cho access token, tối thiểu 32 byte; thiếu hoặc ngắn hơn thì Backend không khởi động. Sinh bằng lệnh ghi trong `.env.example`. Test tích hợp tự dùng secret riêng, không cần biến này. |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | trống (port `587`) | SMTP Brevo. Khi `SPRING_MAIL_HOST` trống, Backend vẫn chạy nhưng bỏ qua việc gửi email và ghi cảnh báo. |
| `MAMXANH_MAIL_FROM` | trống | Địa chỉ người gửi đã xác minh trên Brevo; trống thì cũng bỏ qua việc gửi email. |
| `MAMXANH_FRONTEND_BASE_URL` | `http://localhost:5173` | Origin dùng để tạo liên kết trong email (`/xac-minh-email?token=...`, `/dat-lai-mat-khau?token=...`). |
| `MAMXANH_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Danh sách origin được gọi API, phân tách bằng dấu phẩy, không dùng wildcard. |
| `MAMXANH_DEMO_PASSWORD` | **bắt buộc khi dùng profile `local`** | Mật khẩu local chung cho tài khoản demo; 12–72 byte UTF-8. Backend tạo BCrypt hash khi khởi động; không đưa mật khẩu vào Git hoặc tài liệu. |
| `MAMXANH_GOOGLE_CLIENT_ID` | trống | OAuth Client ID (công khai) mà mọi Google ID Token phải nhắm tới; cùng giá trị với `VITE_GOOGLE_CLIENT_ID` của Frontend. Trống thì Google Login từ chối mọi token (`401 GOOGLE_TOKEN_INVALID`) và Backend ghi cảnh báo khi khởi động. |
| `GEMINI_API_KEY` | trống | Khóa Gemini dùng lúc chạy Backend cho FR-21. Để trống thì Backend vẫn khởi động nhưng không thể gọi AI. Compose chuyển biến này từ `.env` vào container; không đưa khóa vào Docker image hoặc Git. |

### Tài khoản demo local

Danh sách 54 công thức mẫu có seed gia vị/định lượng riêng cho hai phần ăn, tất cả nguyên liệu dùng gram và hướng dẫn được đồng bộ. Seed ảnh ánh xạ 209 nguyên liệu sang WebP do Frontend phục vụ, chỉ điền URL trống để giữ ảnh Admin đã tải lên. Quy tắc định lượng và lệnh kiểm chứng chạy lại seed nằm trong [Database Workspace Guide](../../database/README.md#gia-vị-định-lượng-và-ảnh-minh-họa-demo). Chạy profile `local` hoặc Compose để cập nhật có giữ dữ liệu; không cần bật `local-reset`.

Khi Flyway nạp seed trong profile `local`, Backend tạo 25 tài khoản đã xác minh email: 20 Customer (`demo-customer@mamxanh.local`, `demo-new-member@mamxanh.local`, `demo-applicant@mamxanh.local` và `demo-customer-04` đến `demo-customer-20`), 4 Expert (`demo-expert@mamxanh.local` và `demo-expert-02` đến `demo-expert-04`) cùng `demo-admin@mamxanh.local`. Bốn Expert demo có ảnh chân dung tổng hợp do Frontend host trong `public/avatars/experts/`; seed chỉ gán avatar cục bộ khi hồ sơ chưa có avatar hoặc vẫn dùng avatar demo, không ghi đè ảnh đã tải lên storage. Tất cả Customer có hồ sơ hoàn chỉnh, BMI hiện tại từ chiều cao/cân nặng và bốn tuần lịch ăn; hai tài khoản onboarding cũ nay cũng là Customer hoàn chỉnh. Seed có thêm công thức, nguyên liệu chưa xác minh dinh dưỡng và hoạt động Like/Dislike/View/Follow để trình diễn các bảng xếp hạng. Tài khoản Expert đầu tiên quản lý bộ công thức hiện có và có gói Pro demo hiệu lực một năm tính từ lần seed thiết lập subscription. Đặt `MAMXANH_DEMO_PASSWORD` trong file `.env` local; một mật khẩu được BCrypt hóa riêng khi ứng dụng khởi động và gán cho cả 25 tài khoản.

Seed repeatable chỉ dùng với profile `local`. Trong IntelliJ/Maven, profile `local,local-reset` mặc định xóa và dựng lại `MamXanhDB` trước khi chạy migration; Docker Compose giữ volume/database khi `down`/`up`, và chạy lại seed an toàn. Reset SQL volume Compose sẽ dựng lại toàn bộ schema và fixture. Không dùng tài khoản hoặc mật khẩu demo ngoài máy local.

### 3. Compile và chạy

Mở PowerShell tại `app/mamxanh-backend`:

```powershell
.\mvnw.cmd clean test-compile
.\mvnw.cmd spring-boot:run
```

Backend dùng port mặc định `8080`. Dấu hiệu khởi động thành công là log Spring Boot có dòng `Started MamXanhApplication` và không có lỗi kết nối SQL Server/Flyway/Hibernate. Dừng bằng `Ctrl+C`.

Sau lần tải dependency đầu tiên, các lần mở dự án tiếp theo chỉ cần bảo đảm SQL Server đang chạy rồi dùng:

```powershell
.\mvnw.cmd spring-boot:run
```

### Chạy bằng nút Run trong IntelliJ IDEA

1. Mở thư mục `app/mamxanh-backend` bằng IntelliJ IDEA.
2. Chọn **Project SDK = Java 21** và chờ Maven import dependency xong.
3. Bảo đảm `.env` có đủ các biến theo `.env.example` và cấu hình database local ở trên.
4. Mở `src/main/java/tech/mamxanh/MamXanhApplication.java`.
5. Bấm nút Run cạnh hàm `main`.

IntelliJ vẫn sử dụng cùng cấu hình Spring profile `local`; nút Run không thay thế yêu cầu SQL Server phải sẵn sàng.

### Tùy chọn reset schema mỗi lần chạy local

Mặc định khi chạy local, Backend bật profile phá hủy dữ liệu `local-reset` cùng với `local`. Mỗi lần khởi động, Backend xác minh JDBC URL trỏ tới SQL Server loopback, kết nối đúng database `MamXanhDB`, và tài khoản có quyền tạo/xóa database; sau đó app kết nối `master`, drop rồi tạo lại database `MamXanhDB`, và chạy toàn bộ migration hiện hành. Database này bị xóa hoàn toàn mỗi lần chạy; không dùng profile này với database dùng chung, staging hoặc production. Docker Compose truyền profile `local` tường minh nên không reset database container.

- **IntelliJ IDEA:** Để trống **Active profiles** để dùng mặc định `local,local-reset`. Nếu cấu hình đã đặt profile tường minh, đặt thành `local,local-reset` để reset mỗi lần chạy. Muốn giữ database, đặt thành `local`. Không lưu credential trong Run Configuration.
- **Maven (PowerShell):** `.\mvnw.cmd spring-boot:run` dùng mặc định reset. Đặt `$env:SPRING_PROFILES_ACTIVE='local'` trước lệnh để giữ database.
- Việc reset drop database vật lý, nên mọi bảng và dữ liệu trong `MamXanhDB` bị xóa. Database được tạo lại với collation `SQL_Latin1_General_CP1_CI_AS` và compatibility level `150`. Nếu preflight không xác nhận đúng local server, database hoặc quyền SQL Server, ứng dụng dừng trước khi drop.

## Cách 2 — Chạy toàn hệ thống bằng Docker Compose

Để chạy Frontend, Backend và SQL Server theo một cấu hình dùng chung, ưu tiên Docker Compose ở repository root. Hướng dẫn dưới đây vẫn hữu ích khi chỉ cần chạy riêng Backend.

Tại root repository, nếu chưa có file local thì sao chép `app/mamxanh-backend/.env.example` thành `app/mamxanh-backend/.env`, giữ nguyên file `.env` đang có, rồi đặt một mật khẩu SQL Server local mạnh cho `MSSQL_SA_PASSWORD`:

```powershell
if (-not (Test-Path app/mamxanh-backend/.env)) {
    Copy-Item app/mamxanh-backend/.env.example app/mamxanh-backend/.env
}
```

Khởi động stack bằng:

```powershell
$composeProject = 'mamxanh-dev'
docker compose -p $composeProject --env-file app/mamxanh-backend/.env up --build --detach --wait --wait-timeout 600
```

Khi chạy với profile `local`, Flyway áp dụng migration schema và repeatable demo seed `db/demo/R__demo_sample_data.sql` trước khi Backend báo healthy. Cơ chế này áp dụng cho IntelliJ và Docker Compose; Compose chỉ khởi động Frontend sau khi Backend khỏe. Seed có thể chạy lại an toàn; `down` rồi `up` giữ SQL volume và dữ liệu hiện có.

Mở Frontend tại <http://localhost:5173>; Scalar API Reference chính thức tại <http://localhost:8080/scalar>; generated OpenAPI JSON tại <http://localhost:8080/v3/api-docs>. Các port chỉ bind vào loopback của máy local. Scalar tải JS asset đã pin từ jsDelivr nên browser cần truy cập CDN; spec được tải cùng origin Backend. Dừng bằng `docker compose -p $composeProject --env-file app/mamxanh-backend/.env down`; lệnh này giữ database và dependency volume. Sửa `MSSQL_SA_PASSWORD` trong `.env` không tự đổi credential đã khởi tạo trong SQL volume.

Để reset riêng database development, chạy script có xác nhận riêng. Script chỉ xóa SQL volume, sau đó Compose khởi động lại SQL Server và Flyway dựng lại schema cùng fixture; Frontend dependency volume vẫn được giữ:

```powershell
./scripts/reset-docker-db.ps1
```

Lệnh xóa volume SQL sẽ xóa toàn bộ database local. `docker compose -p $composeProject --env-file app/mamxanh-backend/.env down --volumes` là full reset, xóa cả SQL data lẫn Frontend dependency volume; chỉ dùng nếu chấp nhận mất tất cả named-volume data. Khi dùng project override, giữ nguyên cùng tên ở mọi lệnh Compose và dùng prefix đó cho volume tương ứng.

Trong Scalar, tìm endpoint, xem schema/status/auth, nhập Bearer/JWT token thủ công khi generated runtime spec khai báo scheme và endpoint được bảo vệ, rồi gửi request tới Backend. CI chỉ xác nhận HTTP route/HTML shell `/scalar`; để nghiệm thu browser, mở trang và xác minh Scalar JS render đúng endpoint từ runtime spec, sau đó thử request phù hợp. Không ghi token thật vào log, ảnh chụp hoặc tài liệu; manual thử bằng Scalar không thay automated regression tests.

Để kiểm tra trạng thái, endpoint smoke, cấu hình local và quy tắc CI/PR, xem [Docker development và kiểm thử tích hợp](../../CONTRIBUTING.md#docker-development). Không commit `.env` hoặc chia sẻ mật khẩu. `Docker Development` trong GitHub Actions build và smoke-test stack trên mỗi PR hướng vào `develop` hoặc `main`; job cũng capture/validate/upload generated OpenAPI. Ruleset `protect-develop` được tài liệu branch ghi nhận yêu cầu check này trước merge vào `develop`.

Các lệnh Dockerfile bên dưới chỉ chạy Backend độc lập để debug; chúng không phải cách test tích hợp chuẩn và không thay thế Docker Compose chung. Dockerfile chỉ đóng gói Backend; SQL Server vẫn chạy bên ngoài container trong cách chạy độc lập này.

### 1. Build image

Mở PowerShell tại `app/mamxanh-backend`:

```powershell
docker build -t mamxanh-backend-dev .
```

### 2. Cấu hình database

Nếu SQL Server chạy trên máy Windows, đặt `SPRING_DATASOURCE_URL` trong `.env` dùng `host.docker.internal` thay vì `localhost`. Giữ các biến còn lại theo `.env.example`:

```properties
SPRING_DATASOURCE_URL=jdbc:sqlserver://host.docker.internal:1433;databaseName=MamXanhDB;encrypt=true;trustServerCertificate=true
```

Không ghi giá trị thật vào README, Dockerfile hoặc source control.

### 3. Chạy container

```powershell
docker run --rm --name mamxanh-backend `
  -p 127.0.0.1:8080:8080 `
  --env-file .env `
  mamxanh-backend-dev
```

Dừng bằng `Ctrl+C`. Container tự xóa vì dùng `--rm`. Khi source hoặc `pom.xml` thay đổi, build lại image trước khi chạy.

## Kiểm tra và xử lý lỗi thường gặp

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `java` đúng nhưng `javac` không tồn tại | Máy đang dùng JRE hoặc `PATH` trỏ sai | Cài/chọn JDK 21 và mở Terminal mới. |
| `Login failed for user` | Sai username/password hoặc SQL Login chưa được bật | Kiểm tra credential và chế độ authentication của SQL Server. |
| `Connection refused` hoặc timeout | SQL Server chưa chạy, sai port hoặc firewall chặn | Xác minh SQL Server lắng nghe TCP `1433`; trong Docker dùng `host.docker.internal`. |
| Hibernate báo thiếu bảng | Database chưa có schema mà `ddl-auto=validate` không tự tạo bảng | Khởi tạo schema/migration được dự án phê duyệt; không đổi sang `ddl-auto=update` để né lỗi. |
| Flyway validation lỗi | Migration history và source migration không khớp | Không sửa migration đã áp dụng; đối chiếu đúng database/environment trước khi tiếp tục. |
| Port `8080` đã được sử dụng | Backend/container khác đang chạy | Dừng tiến trình cũ rồi chạy lại. |
| Test báo `Could not find a valid Docker environment` | Docker Desktop chưa chạy | Mở Docker Desktop, chờ trạng thái Running rồi chạy lại `verify`. |
| Docker không nhận lệnh | Docker Desktop chưa cài, chưa chạy hoặc chưa có trong `PATH` | Mở Docker Desktop và xác minh bằng `docker version`. |

## Ranh giới kiến trúc hiện tại

- SRS là nguồn nghiệp vụ; Technology Stack là nguồn lựa chọn công nghệ; System Architecture là nguồn cho cấu trúc backend đã duyệt. **Backend Architecture: Modular Monolith using MVC/layered structure within each business module.**
- Backend gồm một Spring Boot application và một deployable backend, không tách microservices. Source code được chia theo business capability như `auth`, `recipe`, `mealplan`, `shopping`, `nutrition`, `subscription` và `admin`; bên trong mỗi module phải phân tách tối thiểu `controller`, `service`, `repository`, `model`/`entity`, cùng `dto` khi cần.
- Baseline production đã chốt triển khai Backend dưới dạng Spring Boot JAR trên **Azure App Service (Java 21 SE)**. Dockerfile trong thư mục này dùng để đồng bộ môi trường development; Docker image chưa được chọn làm production deployment artifact.
- Frontend được host riêng trên **Vercel** và gọi Backend qua HTTPS/REST API. Azure SQL Database Serverless là relational source of truth; Azure Blob Storage lưu media của Recipe Post.
- Luồng chuẩn là `React View -> Spring MVC Controller -> Service -> Repository -> Model/Entity -> Database`. Chi tiết package cụ thể chỉ được xác lập khi scaffold và phải tuân theo ranh giới này. AI architecture sử dụng Google Gen AI Java SDK (`com.google.genai:google-genai`) với model `gemini-3.8-flash`, bọc qua `AiClient` interface abstraction; cấu hình timeout + retry cho lỗi tạm thời, không ghi nhận lượt gọi thành công và không thay đổi entitlement khi AI gặp sự cố.
- SQL Server là source of truth cho dữ liệu nghiệp vụ (triển khai trên Azure SQL Database Serverless). Flyway phải quản lý migration theo thứ tự, còn JPA/Hibernate không thay thế lịch sử migration.
- Authentication baseline dùng Google Identity Services (`GoogleIdTokenVerifier` ở Backend xác thực ID Token), Stateless JWT Access Token (Resource Server Nimbus), BCrypt, kiểm tra tức thời `USER.account_status`, đăng xuất xử lý phía client; role/ownership, validation, feature entitlement và technical rate limiting (tạm khóa 10 phút cấp tài khoản sau 5 lần sai trên bảng `USER`) phải được thực thi ở backend.
- Subscription baseline dùng FREE 0, PLUS 49,000 và PRO 99,000 VND/tháng; verified activation, end-of-period expiry, no auto-renew/no partial refund và idempotent duplicate payment processing. Cổng thanh toán đã chốt là **payOS** (REST API qua Spring `RestClient` + Webhook HMAC-SHA256).
- Gemini, Azure, Brevo SMTP và payOS credential chỉ đến từ cấu hình môi trường/secret store (local `.env`, GitHub Secrets, Azure App Service Settings); không commit giá trị thật. Maps chỉ được cấu hình nếu M11 được kích hoạt lại bằng quyết định scope mới.
- API được mô tả bằng OpenAPI và trả lỗi nhất quán; không để frontend suy đoán business rule.

## Thanh toán xác minh — FR-13 / Issue #142

- `POST /api/v1/subscriptions/checkout` chỉ trả URL HTTPS thuộc `pay.payos.vn` sau khi payOS trả dữ liệu có chữ ký hợp lệ, đúng mã đơn/số tiền, trạng thái `PENDING` và có QR/link thanh toán. Thiếu cấu hình hoặc provider không khả dụng trả `503 PAYMENT_UNAVAILABLE`; dữ liệu provider không xác minh được trả `502 PAYMENT_RESPONSE_INVALID`. Không tạo checkout giả hoặc chấp nhận chữ ký thử trong runtime.
- `GET /api/v1/subscriptions/transactions/{orderCode}` yêu cầu Member đang hoạt động, chỉ đọc đơn của chính tài khoản; đơn không tồn tại hoặc của người khác cùng trả 404. Response `Cache-Control: no-store` gồm `orderCode` dạng chuỗi, gói đã mua, số tiền, trạng thái đơn, `paidAt`, `activated` và subscription hiện tại. Đơn `PENDING` được đối soát server-to-server với payOS; không tin trạng thái trên URL trình duyệt.
- Webhook và đối soát dùng chung `PaymentSettlementService`: khóa tài khoản rồi khóa đơn, kiểm tra số tiền, hết hiệu lực bản ghi ACTIVE cũ trước khi thêm bản ghi mới, cập nhật `PAID`/subscription trong cùng transaction. Webhook lặp không đổi thời hạn hoặc `paidAt`; probe có chữ ký hợp lệ nhưng không thuộc đơn nội bộ được ACK mà không cấp quyền. Chỉ dữ liệu webhook được ký mới quyết định kết quả; envelope hoặc query params không cấp quyền.
- Khi chạy Compose, điền ba credential payOS trong `app/mamxanh-backend/.env`: `MAMXANH_PAYOS_CLIENT_ID`, `MAMXANH_PAYOS_API_KEY`, `MAMXANH_PAYOS_CHECKSUM_KEY`; file Compose truyền các biến này vào Backend. Không đưa credential vào Frontend hoặc Git. Để trống vẫn khởi động ứng dụng nhưng chức năng tạo/xác minh thanh toán chưa xử lý được sẽ báo 503.
- Cấu hình webhook trên kênh payOS bằng URL HTTPS công khai kết thúc `/api/v1/payments/webhook`. Local cần HTTPS tunnel trỏ về Backend; `localhost` không phải webhook công khai. `MAMXANH_PAYOS_RETURN_URL` và `MAMXANH_PAYOS_CANCEL_URL` trỏ tới đúng origin Frontend mà người dùng đăng nhập, không thay thế webhook. Tạo lại container Backend sau khi đổi biến môi trường bằng cùng Compose project; không cần xóa SQL volume.
- Adapter REST có thời gian chờ hữu hạn; lỗi mạng, chữ ký sai hoặc thiếu tiền không được kích hoạt gói. Kiểm thử dùng provider có kiểm soát, không thực hiện chuyển tiền thật. `PaymentVerificationIntegrationTest` kiểm tra locking, ownership, hủy/đối soát và giữ đúng một subscription ACTIVE trên SQL Server qua Testcontainers.

## Definition of Done cho thay đổi backend

- Business Rule và failure case liên quan có test phù hợp.
- `./mvnw clean verify` (Windows: `.\mvnw.cmd clean verify`) pass trên commit hiện tại, gồm tests, build/package và JaCoCo BUNDLE Lines, Branches, Methods, Instructions đều >80%.
- Không log password, token, SAS URL hoặc dữ liệu cá nhân nhạy cảm.
- Thay đổi schema có Flyway migration append-only, kiểm tra trên database sạch và cập nhật ERD/tài liệu.
- Thay đổi API có OpenAPI, validation, authorization và ví dụ lỗi tương ứng.
