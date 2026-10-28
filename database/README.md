> **Document:** Database Workspace Guide  
> **File:** `database/README.md`  
> **Version:** v0.26.0<br>
> **Created:** 2026-06-14  
> **Last Updated:** 2026-10-26<br>
> **Status:** Active  

# Database Workspace

## Migration V18/V19 — Checklist và nguồn Recipe Post

`V18__shopping_manual_roll_unit.sql` bổ sung đơn vị `cuộn` thuộc `COUNT` cho AC-53.2, chỉ thêm nếu mã chưa tồn tại; không suy diễn tỷ lệ quy đổi. Khi tạo từ Meal Plan, Backend tổng hợp trước khi làm tròn tổng tới hai chữ số `HALF_UP`, trả cảnh báo nếu đổi lượng và từ chối lượng vượt giới hạn hoặc thành 0; không ghi một phần danh sách.

`V19__shopping_recipe_contributions.sql` triển khai AC-53.6 theo thiết kế nguồn đã duyệt trong #90: thêm `SHOPPING_LIST.list_kind`, unique index giới hạn một danh sách `PERSONAL` mỗi Member, và `SHOPPING_RECIPE_CONTRIBUTION` lưu ID công thức/nguyên liệu, lượng/đơn vị gốc, liên kết nullable tới item tổng hợp. Không sao chép nội dung bài. Các danh sách cũ mặc định `MEAL_PLAN`. Không cascade khi Recipe Post thay đổi trạng thái; khi xóa item, Service bỏ liên kết item và giữ đóng góp để chống thêm lại trùng. Xóa danh sách xóa các đóng góp của danh sách. Snapshot chứa cùng DDL; diagram giữ nguyên.

Database chính đã chốt là Microsoft SQL Server 2019. Migration V13 bổ sung mục tiêu hồ sơ dinh dưỡng tùy chọn cho FR-35/FR-36; V14 thêm các giá trị tham chiếu dinh dưỡng dùng chung cho FR-37. Ngày 17/10/2026, Backend `clean verify` đạt 891/891 test và đã khởi tạo SQL Server sạch, áp dụng đủ V1–V16 qua Testcontainers. Điều này không thay nghiệm thu riêng từng FR; các kết quả lịch sử ngày 10/10/2026 vẫn chỉ áp dụng cho baseline V1–V12.

## Migration V20 — Ảnh minh họa nguyên liệu

`V20__ingredient_reference_image.sql` thêm cột tùy chọn `INGREDIENT.image_url` để lưu URL ảnh do Admin quản lý. Migration này được đưa lên V20 sau khi đồng bộ với develop, nơi V18/V19 đã được dùng cho checklist đi chợ; không sửa lịch sử V18/V19.

## Migration V21 — Danh sách đi chợ tạo thủ công

`V21__shopping_manual_list_kind.sql` cho phép `SHOPPING_LIST.list_kind = 'MANUAL'` để lưu nhiều danh sách rỗng do Member tự tạo; giữ nguyên loại `MEAL_PLAN`, danh sách công thức `PERSONAL` và giới hạn một danh sách công thức mỗi Member. Không thêm bảng hoặc quan hệ. Snapshot chứa cùng constraint.

## Migration V22 — Người xử lý báo cáo

`V22__report_handling_by_admin.sql` (Issue #33, FR-28) thêm `REPORT.handled_by` kèm `FK_REPORT_HANDLED_BY` tới `USER` (`NO ACTION`) theo SRS 3.12, nơi báo cáo lưu `decision`, `decision_reason`, `handled_by`, `handled_at`. Cột được ghi khi Administrator tiếp nhận báo cáo (`IN_REVIEW`) và giữ nguyên khi `RESOLVED`. `CK_REPORT_decision` chỉ nhận `NO_VIOLATION`, `WARN_AUTHOR`, `HIDE_RECIPE`, `LOCK_AUTHOR` hoặc `NULL`; dữ liệu cũ chưa có kết luận nên vẫn hợp lệ. Hàng đợi lọc theo trạng thái dùng lại `IX_REPORT_status` của V1. Không thêm bảng lịch sử: báo cáo `RESOLVED` không còn API nào sửa được, nên chính bản ghi báo cáo là audit. Snapshot chứa cùng cột và constraint.

## Migration V24 — Tệp đính kèm đơn đăng ký Chuyên gia

`V24__expert_application_private_attachments.sql` thêm metadata khóa blob và MIME type cho chứng chỉ/văn bằng cùng ảnh chân dung riêng của từng đơn FR-05. Nội dung file nằm trong Azure Blob container riêng tư, không lưu trong SQL Server. Migration được đánh số V24 vì develop đã sử dụng V23 cho `REPORT_SUPPLEMENT`; snapshot chứa cùng các cột và constraint. Physical ERD/Data Dictionary trong `docs/diagrams/` là workspace được bảo vệ và không được cập nhật trong thay đổi này.

## Migration V25 — Lưu BMI hồ sơ dinh dưỡng

`V25__persist_nutrition_bmi.sql` bổ sung `USER.nutrition_bmi` và backfill một lần cho hồ sơ đã đồng ý lưu dữ liệu sức khỏe. Backend tính/lưu BMI khi hồ sơ mới hoặc chiều cao/cân nặng thay đổi và đọc lại giá trị đã lưu khi mở trang. `database/schema.sql` phản ánh cột mới.
Repeatable `R__demo_sample_data_65_report_moderation.sql` (chỉ profile `local`) thêm 5 báo cáo bài công thức để hàng đợi `/admin/bao-cao` có dữ liệu: 1 `OPEN`, 1 `IN_REVIEW` do `demo-admin@mamxanh.local` tiếp nhận và 3 `RESOLVED` với `NO_VIOLATION`, `WARN_AUTHOR`, `HIDE_RECIPE`. Báo cáo đã giải quyết có thông báo `REPORT_RESULT`/`MODERATION_DECISION` cùng câu chữ với `ModerationNotificationService`, không nêu người báo cáo. Kết luận `HIDE_RECIPE` dùng bài riêng của `demo-expert-02@mamxanh.local` nên các bài mẫu khác vẫn công khai. Seed không có `LOCK_AUTHOR` nên không tài khoản demo nào bị khóa. Seed kiểm tra tồn tại trước khi thêm nên chạy lại không nhân bản dữ liệu.

## Migration V23 — Bổ sung thông tin cho báo cáo đang mở

`V23__report_supplement.sql` (Issue #35, FR-30) tạo `REPORT_SUPPLEMENT` (`supplement_id`, `report_id` FK `NO ACTION` tới `REPORT`, `content NVARCHAR(500)`, `created_at`) cùng index `IX_REPORT_SUPPLEMENT_report` theo báo cáo và thời gian. Mỗi lần người gửi bổ sung vào báo cáo `OPEN`/`IN_REVIEW` là một dòng mới (AC-30.1), ứng dụng không sửa hay xóa dòng nào, `REPORT.description` giữ mô tả gốc. Giới hạn 5 lần và 10–500 ký tự do Backend kiểm (SRS 3.14 để lại cho thiết kế triển khai). Bảng mới đã nêu trong câu hỏi Q67 gửi Tech Lead. Snapshot chứa cùng bảng và index.

Repeatable `R__demo_sample_data_66_report_supplements.sql` (chỉ profile `local`) thêm 2 lần bổ sung cho báo cáo `OPEN` của `demo-customer-08@mamxanh.local` ở seed 60, để người gửi thấy các lần bổ sung trên trang công thức. Seed chỉ thêm khi báo cáo còn `OPEN`/`IN_REVIEW`, giữ nguyên mô tả gốc và kiểm tra tồn tại trước khi thêm nên chạy lại không nhân bản dữ liệu.

Repeatable `R__demo_sample_data_67_report_groups.sql` (chỉ profile `local`, AC-30.4) thêm báo cáo của `demo-customer-15@mamxanh.local` (`OPEN`) và `demo-customer-16@mamxanh.local` (`IN_REVIEW`, do `demo-admin@mamxanh.local` tiếp nhận) cho cùng bài "Bánh xèo nấm và giá đỗ" của seed 60, để trang `/admin/bao-cao` có một nhóm 3 báo cáo từ 3 tài khoản. Mỗi tài khoản chỉ được thêm khi chưa có báo cáo nào trên bài, nên chạy lại không tạo dòng mới và không vi phạm quy tắc một báo cáo đang mở.

## Quyền sở hữu dữ liệu

- Flyway migration trong backend (`app/mamxanh-backend/src/main/resources/db/migration/`) là lịch sử schema có thẩm quyền và append-only sau khi chia sẻ. Baseline hiện gồm V1–V25; V13 mở rộng `nutrition_goal`, V14 tạo `NUTRITION_DAILY_VALUE`, V16 cho phép hỗ trợ từng phần, V17 chấp nhận nguồn AI trong tính toán một phần, V18 thêm đơn vị `cuộn`, V19 lưu loại danh sách và đóng góp Recipe Post, V20 thêm URL ảnh minh họa tùy chọn cho nguyên liệu, V21 cho phép loại danh sách `MANUAL`, V22 thêm người xử lý báo cáo, V23 tạo `REPORT_SUPPLEMENT`, V24 thêm tham chiếu attachment riêng tư vào đơn đăng ký Chuyên gia và V25 lưu BMI hồ sơ dinh dưỡng. `database/schema.sql` phản ánh trạng thái sau toàn bộ migration; Physical ERD do người phụ trách sơ đồ cập nhật riêng.
- `database/schema.sql` là snapshot/manual bootstrap độc lập, được đồng bộ có chủ đích với trạng thái sau khi chạy toàn bộ Flyway migration; dùng cho khởi tạo nhanh trên SSMS, Azure Data Studio hoặc `sqlcmd`.
- Flyway repeatable migrations trong `app/mamxanh-backend/src/main/resources/db/demo/` nạp fixture demo khi profile `local` chạy (IntelliJ hoặc Docker Compose), gồm tài khoản, lịch ăn, công thức, nguyên liệu, tương tác cộng đồng và danh sách mua sắm. `MAMXANH_DEMO_PASSWORD` được lấy từ `.env` local và BCrypt hóa lúc Backend khởi động; không lưu password/hash trong SQL migration hoặc Git.
- Profile `local` dùng thêm location `classpath:db/demo`; `test` và production chỉ chạy migration schema. Repeatable demo chạy theo thứ tự catalog, ước tính AI, recipe và hoạt động mẫu. Fixture AI estimate điền đủ chín chỉ tiêu trên 100 g bằng giá trị minh họa chưa xác minh để công thức demo không còn thiếu chỉ tiêu; số 0 cũng là ước tính (ví dụ B12 trong thực phẩm thực vật), không phải xác nhận xét nghiệm. Dữ liệu nhập khác vẫn có thể thiếu trường; khi đó giá trị `NULL` được bỏ khỏi phép tính và giao diện cảnh báo kết quả chưa đầy đủ. Seed kiểm tra tồn tại trước khi thêm nên chạy lại không nhân bản fixture.
- `database/queries.sql` chứa kịch bản kiểm tra đối tượng, bộ test tự động xác minh các ràng buộc nghiệp vụ (positive/negative) có cơ chế rollback, và các truy vấn mẫu cho tầng ứng dụng; không thay thế automated integration tests.

## Gia vị, định lượng và ảnh minh họa demo

- Repeatable `R__demo_sample_data_46_recipe_seasonings.sql` cập nhật đúng 54 món mẫu của `demo-expert@mamxanh.local` theo danh sách tiêu đề: bổ sung gia vị riêng cho từng món, đồng bộ hướng dẫn và định lượng cho hai phần ăn. Tất cả nguyên liệu trong các bài này dùng gram; form tạo/sửa vẫn hỗ trợ các đơn vị đã có.
- Seed kiểm tra đủ món/nguyên liệu trước khi ghi, cập nhật từng cặp bài–nguyên liệu và không xóa nguyên liệu bổ sung sẵn có. Đơn vị cũ được đổi bằng `UNIT.base_factor` cho khối lượng hoặc `INGREDIENT_UNIT_CONVERSION` riêng theo nguyên liệu; thiếu hệ số thì dừng transaction. Không áp dụng quy tắc 1 ml = 1 g cho mọi chất lỏng.
- Gạo/hạt/đậu khô được cân trước khi nấu; trứng tính phần bỏ vỏ, rau củ tính phần ăn được. Nước ghi trong công thức là lượng giữ trong món; nước ngâm/rửa, nước hấp và nước luộc bỏ đi không được tính là nguyên liệu tiêu thụ. Định lượng là công thức minh họa, không phải chỉ định dinh dưỡng cá nhân.
- Repeatable `R__demo_sample_data_12_ingredient_images.sql` ánh xạ 209 nguyên liệu seed sang ảnh AI riêng tại Frontend `public/ingredients/*.webp`. Chỉ điền `image_url` trống; ảnh có sẵn hoặc ảnh Admin tải lên được giữ nguyên. Ảnh minh họa không đại diện thương hiệu hoặc xác nhận giá trị dinh dưỡng.
- Repeatable `R__demo_sample_data_13_ingredient_groups.sql` chuẩn hóa nhóm cho cùng 209 nguyên liệu demo dựa trên loại thực phẩm; chỉ cập nhật `ingredient_group`, không thay đổi dinh dưỡng, ảnh, quy đổi hoặc nguyên liệu ngoài danh sách demo.
- Sau khi stack Compose chuẩn chạy với profile `local`, chạy `./scripts/verify-demo-ingredient-fixtures.ps1` từ repository root. Script kiểm tra asset WebP và chạy hai seed hai lần trong transaction có rollback: kiểm tra định lượng gram, dinh dưỡng đủ chín chỉ tiêu, không trùng nguyên liệu, bảo toàn bài ngoài fixture và ảnh có sẵn. Không reset database hoặc SQL volume. Có thể truyền `-ComposeProject` nếu stack dùng project name khác.

## Khôi phục database local khi Flyway history không tương thích

- Với database phát triển local `MamXanhDB`, Flyway history phải khớp với các migration hiện hành trong branch đang chạy. Nếu cùng version nhưng khác ý nghĩa (ví dụ database ghi V3 là `ingredient group and unit validation` trong khi code hiện tại dùng V3 cho email verification), đây là hai baseline khác nhau; **không chạy `flyway repair`** để ép checksum khớp và không sửa migration đã chia sẻ.
- Khi chạy Backend local, profile mặc định `local,local-reset` drop và tạo lại database mỗi lần khởi động theo [Backend Workspace Guide](../app/mamxanh-backend/README.md#tùy-chọn-reset-schema-mỗi-lần-chạy-local). Profile chỉ chạy trên SQL Server loopback, database `MamXanhDB`, và yêu cầu tài khoản có quyền tạo/xóa database; sau khi tạo database mới, Flyway áp dụng toàn bộ migration hiện hành. Toàn bộ database và dữ liệu cũ bị xóa mỗi lần chạy. Compose đặt profile `local` rõ ràng nên giữ database container.
- Nếu cần reset toàn database do database vật lý hoặc cấu hình bị hỏng, chỉ làm khi chủ sở hữu database xác nhận dữ liệu local có thể xóa: dừng Backend, xác minh `.env` trỏ tới đúng SQL Server local và đúng `MamXanhDB`, rồi xóa/tạo lại **chính database `MamXanhDB`**. Không tự tạo database tên khác để né migration drift. Database khác hoặc remote/shared database không thuộc quy trình reset local này.
- Nếu dữ liệu cần giữ, không drop database và không sửa `flyway_schema_history` thủ công; dừng lại để sao lưu và chọn phương án phục hồi/migration phù hợp với baseline đã xác nhận.
- Sau khi tạo lại, giữ database name `MamXanhDB`, collation `SQL_Latin1_General_CP1_CI_AS` và compatibility level `150` (SQL Server 2019), sau đó khởi động Backend để Flyway áp dụng các migration hiện hành. Xác minh toàn bộ migration thành công trước khi báo Backend sẵn sàng.

Ví dụ reset chỉ dành cho database local đã được xác nhận là có thể xóa (chạy trên đúng SQL Server instance):

```sql
USE [master];
GO
IF DB_ID(N'MamXanhDB') IS NOT NULL
BEGIN
    ALTER DATABASE [MamXanhDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [MamXanhDB];
END;
GO
CREATE DATABASE [MamXanhDB] COLLATE SQL_Latin1_General_CP1_CI_AS;
GO
ALTER DATABASE [MamXanhDB] SET COMPATIBILITY_LEVEL = 150;
GO
```

## Quy trình thay đổi schema & Engineering Autonomy Policy

Chính sách quản trị schema tuân thủ trực tiếp [Engineering Autonomy Policy](../CONTRIBUTING.md#engineering-autonomy-policy):

1. **Phân định thẩm quyền thay đổi:**
   - **Thay đổi nhỏ/cục bộ (Minor/Local Schema Evolution — Autonomous):** Việc thêm các cột cục bộ trên bảng hiện có (như bổ sung các cột phục vụ auth/security trên bảng `USER`), điều chỉnh nullability/default tương thích, thêm index, hoặc thêm ràng buộc CHECK/FK nhằm triển khai một mối quan hệ (relationship) đã được requirement hoặc schema baseline chấp thuận trong khuôn khổ implementation issue đã phân công **không** cần phải tổ chức biểu quyết toàn nhóm (3/5 vote). Developer hoặc AI coding agent được chủ động tạo Flyway migration mới, cập nhật entity, repository, tests, và PR sẽ được review theo quy trình code review thông thường.
   - **Thay đổi lớn/cấp hệ thống (Major/System-wide Changes — Decision Required):** Chỉ áp dụng Decision Issue và biểu quyết nhóm 3/5 đối với: thêm bảng/thực thể mới, xóa bảng, split/merge bảng, tạo mới hoặc thay đổi relationship/cardinality so với baseline, thay đổi kiểu dữ liệu phá vỡ tính tương thích, hoặc thay đổi hệ quản trị cơ sở dữ liệu.
2. **Thiết kế mục tiêu bảng `USER` cho Auth (FR-03 Target Design):**
   - Không tạo 3 bảng độc lập (`REFRESH_TOKEN`, `LOGIN_THROTTLE`, `ACCOUNT_TOKEN`).
   - Toàn bộ trường phục vụ xác minh email, đặt lại mật khẩu và rate limit được lưu trữ trực tiếp trên bảng `USER`:
     - Xác minh email: `email_verification_token` (VARCHAR), `verification_token_expires_at` (DATETIME2).
     - Đặt lại mật khẩu (V11, Issue #9): `password_reset_token` (VARCHAR(64), SHA-256 hex digest của token, filtered unique index), `reset_token_expires_at` (DATETIME2, hạn 15 phút).
     - Brute-force rate limit: `failed_login_attempts` (INT DEFAULT 0), `login_blocked_until` (DATETIME2 NULL). Không thêm `last_failed_login_at` vì Acceptance Criteria không dùng cột này (Tech Lead chốt Q29 ngày 03/10/2026).
     - Metadata rate limit email đặt lại mật khẩu (Q27, V11): `password_reset_sent_at` (cooldown 60 giây), `password_reset_window_started_at` và `password_reset_window_count` (INT DEFAULT 0) cho tối đa 5 email trong khung 1 giờ/tài khoản.
   - Developer (Tony) sẽ viết Flyway migration mới trong các Issue thực thi (#5, #6, #9) và cập nhật snapshot `database/schema.sql`.
   - **Ranh giới Diagram Artifact Protection:** Thư mục `docs/diagrams/ERD/` (Physical ERD, Logical ERD) là presentation workspace do con người duy trì và được bảo vệ theo `AGENTS.md`. Việc thay đổi schema hoặc migration **tuyệt đối không tự động cấp quyền sửa hoặc regenerate ERD diagrams** cho coding agent trừ khi có task riêng được ủy quyền tường minh.
3. **Quy trình thực hiện migration:**
   - Truy vết thay đổi đến SRS/Issue và xác nhận không mở rộng scope ngoài quyết định đã duyệt.
   - Thêm Flyway migration mới ở phiên bản tiếp theo khả dụng (hiện là `V26__...`); không sửa migration đã được chia sẻ.
   - Cập nhật entity/DTO/repository và test liên quan.
   - Cập nhật snapshot `database/schema.sql`.
   - Kiểm tra migration trên database sạch và kiểm thử nâng cấp.
   - PR cần review theo ADR-002.

## Trạng thái thiết kế

- **Conceptual ERD:** 22 thực thể, 36 connector (Đã duyệt).
- **Logical ERD:** [logical-erd-v1.0.0.drawio](../docs/diagrams/ERD/logical-erd-v1.0.0.drawio) (22 bảng, 37 connector thể hiện 36 quan hệ; cập nhật lần cuối ở commit `827353e`).
- **Physical ERD:** [physical-erd-v1.0.0.drawio](../docs/diagrams/ERD/physical-erd-v1.0.0.drawio) & [physical-erd-v1.0.0.drawio.png](../docs/diagrams/ERD/physical-erd-v1.0.0.drawio.png) (22 bảng, 37 connector, 196 physical columns với đầy đủ kiểu dữ liệu, nullability, constraints, indexes).
- **Data Dictionary:** [data-dictionary.md](../docs/diagrams/ERD/data-dictionary.md) v0.7.2 (22 bảng, 196 cột physical, hoàn thành triển khai toàn bộ 33 mục đánh dấu sau review PR #66). Theo quyết định của Tech Lead ngày 01/10/2026, các tài liệu trong `docs/diagrams/` (ERD, Data Dictionary) là baseline tham khảo và chỉ được đồng bộ ở giai đoạn viết tài liệu nộp; trạng thái schema hiện hành lấy theo Flyway migration và `database/schema.sql`.
- **Schema & Migration:** Lịch sử hiện hành gồm V1–V25; V13 mở rộng mục tiêu FR-35, V14 thêm giá trị tham chiếu cho FR-37, V16/V17 hỗ trợ dinh dưỡng từng phần và nguồn AI; V18 thêm đơn vị `cuộn`, V19 lưu loại danh sách và đóng góp Recipe Post, V20 thêm URL ảnh minh họa tùy chọn cho nguyên liệu, V21 thêm loại `MANUAL`, V22 thêm `REPORT.handled_by`, V23 tạo `REPORT_SUPPLEMENT`, V24 thêm attachment riêng tư FR-05 và V25 lưu BMI FR-35. `database/schema.sql` là snapshot thủ công sau toàn bộ migration. SQL Server Testcontainers đã áp dụng V1–V19 ngày 21/10/2026 trên baseline lúc đó; V20 và V21 chưa được xác minh trên SQL Server trong lượt triển khai này. Ngày 25/10/2026, `AdminReportIntegrationTest` (Issue #33) chạy trên SQL Server Testcontainers mới nên đã áp dụng V1–V22; V23–V25 chưa được xác minh trên SQL Server.
- **Migration V7 (Issue #36, FR-31):** [V7__user_onboarding_invitation.sql](../app/mamxanh-backend/src/main/resources/db/migration/V7__user_onboarding_invitation.sql) thêm cột `USER.onboarding_invited_at DATETIME2(7) NULL`: thời điểm lời mời Onboarding đã hiển thị, `NULL` là chưa mời. Backend chỉ mời khi `onboarding_status = NOT_STARTED` và cột còn `NULL`, nên Member bỏ dở questionnaire không bị mời lại (AC-31.10). Tài khoản tồn tại trước migration được ghi thời điểm chạy migration nên không bị hỏi tự động; `onboarding_status` giữ nguyên, nên `SKIPPED` vẫn chỉ có nghĩa là Member đã bấm "Bỏ qua".
- **Migration V8 (Issue #68, FR-05):** [V8__expert_application_notifications.sql](../app/mamxanh-backend/src/main/resources/db/migration/V8__expert_application_notifications.sql) thêm `NOTIFICATION.target_path` và hai index phục vụ truy vấn lịch sử/xét duyệt đơn Chuyên gia. Cần xác minh migration này cùng snapshot trên database sạch trước khi dùng kết quả kiểm tra V1–V7 làm bằng chứng cho baseline đã đồng bộ.
- **Migration V11 (Issue #9, FR-03-E):** [V11__user_password_reset.sql](../app/mamxanh-backend/src/main/resources/db/migration/V11__user_password_reset.sql) thêm năm cột trên `USER`: `password_reset_token`, `reset_token_expires_at`, `password_reset_sent_at`, `password_reset_window_started_at`, `password_reset_window_count`; hai CHECK (`CK_USER_password_reset_token_pair` giữ token và hạn cùng có hoặc cùng `NULL`, `CK_USER_password_reset_window_count_non_negative`), một DEFAULT và filtered unique index `UQ_USER_password_reset_token`. Token mới ghi đè token cũ; đặt lại thành công xóa token nhưng giữ khung rate limit.
- **Verification Tests:** [database/queries.sql](queries.sql) có 41 test cases (TC01–TC41), gồm các assertion cho giới hạn đăng nhập và lời mời Onboarding. Kết quả 80/80 ngày 05/10/2026 được xác minh trên V1–V7; chưa bao gồm migration V8.

## Hướng dẫn kiểm thử và thẩm định

Kiểm tra toàn bộ schema và chạy 41 test cases (TC01–TC41) bằng `sqlcmd`; xác nhận kết quả thực tế trên database sạch:

```powershell
# 1. Khởi tạo database kiểm thử sạch
sqlcmd -S .\SQLEXPRESS -E -Q "DROP DATABASE IF EXISTS MamXanhDB_Test; CREATE DATABASE MamXanhDB_Test;"

# 2. Thực thi schema DDL (hoặc chạy lần lượt V1–V25 trong db/migration với cờ -I,
#    vì filtered index cần QUOTED_IDENTIFIER ON)
sqlcmd -S .\SQLEXPRESS -E -d MamXanhDB_Test -i database/schema.sql

# 3. Chạy bộ kiểm thử ràng buộc nghiệp vụ và xác nhận kết quả thực tế
sqlcmd -S .\SQLEXPRESS -E -d MamXanhDB_Test -i database/queries.sql
```

Xem [SRS](../docs/requirements/SRS.md), [ERD workspace](../docs/diagrams/ERD/README.md) và [Technology Stack](../docs/architecture/TECHNOLOGY-STACK.md).
