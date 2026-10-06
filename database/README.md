> **Document:** Database Workspace Guide  
> **File:** `database/README.md`  
> **Version:** v0.15.0<br>
> **Created:** 2026-06-14  
> **Last Updated:** 2026-10-06<br>
> **Status:** Active  

# Database Workspace

Database chính đã chốt là Microsoft SQL Server 2019. Sau khi đồng bộ, thứ tự migration hiện hành là V1–V8; cần xác minh migration V8 cùng `database/schema.sql` và `database/queries.sql` trên database sạch.

## Quyền sở hữu dữ liệu

- Flyway migration trong backend (`app/mamxanh-backend/src/main/resources/db/migration/`) là lịch sử schema có thẩm quyền và append-only sau khi chia sẻ. Baseline hiện gồm V1–V8, bao gồm V7 cho lời mời Onboarding và V8 cho notification target/index của Expert Application. `database/schema.sql` phải phản ánh trạng thái sau toàn bộ migration; Physical ERD do người phụ trách sơ đồ cập nhật riêng.
- `database/schema.sql` là snapshot/manual bootstrap độc lập, được đồng bộ có chủ đích với trạng thái sau khi chạy toàn bộ Flyway migration; dùng cho khởi tạo nhanh trên SSMS, Azure Data Studio hoặc `sqlcmd`.
- Flyway repeatable migration `app/mamxanh-backend/src/main/resources/db/demo/R__demo_sample_data.sql` nạp fixture demo khi profile `local` chạy (IntelliJ hoặc Docker Compose). Năm tài khoản đã xác minh phục vụ demo Auth, onboarding, hồ sơ/sở thích, công thức, lịch ăn và duyệt Chuyên gia/Admin. `MAMXANH_DEMO_PASSWORD` được lấy từ `.env` local và BCrypt hóa lúc Backend khởi động; không lưu password/hash trong SQL migration hoặc Git.
- Profile `local` dùng thêm location `classpath:db/demo`; các profile `test` và production chỉ chạy location migration schema `classpath:db/migration`. Seed dùng khóa xác định/kiểm tra tồn tại trước khi thêm nên Flyway chạy lại không nhân bản fixture.
- `database/queries.sql` chứa kịch bản kiểm tra đối tượng, bộ test tự động xác minh các ràng buộc nghiệp vụ (positive/negative) có cơ chế rollback, và các truy vấn mẫu cho tầng ứng dụng; không thay thế automated integration tests.

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
     - Đặt lại mật khẩu: `password_reset_token` (VARCHAR), `reset_token_expires_at` (DATETIME2).
     - Brute-force rate limit: `failed_login_attempts` (INT DEFAULT 0), `login_blocked_until` (DATETIME2 NULL). Không thêm `last_failed_login_at` vì Acceptance Criteria không dùng cột này (Tech Lead chốt Q29 ngày 03/10/2026).
     - Metadata rate limit email (Q27): cho phép bổ sung các trường tối thiểu trên `USER` nếu cần theo dõi 60s cooldown và tối đa 5 email/giờ/tài khoản.
   - Developer (Tony) sẽ viết Flyway migration mới trong các Issue thực thi (#5, #6, #9) và cập nhật snapshot `database/schema.sql`.
   - **Ranh giới Diagram Artifact Protection:** Thư mục `docs/diagrams/ERD/` (Physical ERD, Logical ERD) là presentation workspace do con người duy trì và được bảo vệ theo `AGENTS.md`. Việc thay đổi schema hoặc migration **tuyệt đối không tự động cấp quyền sửa hoặc regenerate ERD diagrams** cho coding agent trừ khi có task riêng được ủy quyền tường minh.
3. **Quy trình thực hiện migration:**
   - Truy vết thay đổi đến SRS/Issue và xác nhận không mở rộng scope ngoài quyết định đã duyệt.
   - Thêm Flyway migration mới ở phiên bản tiếp theo khả dụng (hiện là `V9__...`); không sửa migration đã được chia sẻ.
   - Cập nhật entity/DTO/repository và test liên quan.
   - Cập nhật snapshot `database/schema.sql`.
   - Kiểm tra migration trên database sạch và kiểm thử nâng cấp.
   - PR cần review theo ADR-002.

## Trạng thái thiết kế

- **Conceptual ERD:** 22 thực thể, 36 connector (Đã duyệt).
- **Logical ERD:** [logical-erd-v1.0.0.drawio](../docs/diagrams/ERD/logical-erd-v1.0.0.drawio) (22 bảng, 37 connector thể hiện 36 quan hệ; cập nhật lần cuối ở commit `827353e`).
- **Physical ERD:** [physical-erd-v1.0.0.drawio](../docs/diagrams/ERD/physical-erd-v1.0.0.drawio) & [physical-erd-v1.0.0.drawio.png](../docs/diagrams/ERD/physical-erd-v1.0.0.drawio.png) (22 bảng, 37 connector, 196 physical columns với đầy đủ kiểu dữ liệu, nullability, constraints, indexes).
- **Data Dictionary:** [data-dictionary.md](../docs/diagrams/ERD/data-dictionary.md) v0.7.2 (22 bảng, 196 cột physical, hoàn thành triển khai toàn bộ 33 mục đánh dấu sau review PR #66). Theo quyết định của Tech Lead ngày 01/10/2026, các tài liệu trong `docs/diagrams/` (ERD, Data Dictionary) là baseline tham khảo và chỉ được đồng bộ ở giai đoạn viết tài liệu nộp; trạng thái schema hiện hành lấy theo Flyway migration và `database/schema.sql`.
- **Schema & Migration:** Lịch sử hiện hành gồm V1 baseline, V2 Unicode cho `UNIT.code`, V3 xác minh email, V4 consent FR-35, V5 ingredient group/unit validation FR-18, V6 login throttle, V7 lời mời Onboarding (Issue #36) và V8 notification target cùng index Expert Application (Issue #68). `database/schema.sql` là snapshot thủ công sau toàn bộ migration; kết quả đối chiếu ngày 05/10/2026 chỉ áp dụng cho V1–V7, trước khi thêm V8.
- **Migration V7 (Issue #36, FR-31):** [V7__user_onboarding_invitation.sql](../app/mamxanh-backend/src/main/resources/db/migration/V7__user_onboarding_invitation.sql) thêm cột `USER.onboarding_invited_at DATETIME2(7) NULL`: thời điểm lời mời Onboarding đã hiển thị, `NULL` là chưa mời. Backend chỉ mời khi `onboarding_status = NOT_STARTED` và cột còn `NULL`, nên Member bỏ dở questionnaire không bị mời lại (AC-31.10). Tài khoản tồn tại trước migration được ghi thời điểm chạy migration nên không bị hỏi tự động; `onboarding_status` giữ nguyên, nên `SKIPPED` vẫn chỉ có nghĩa là Member đã bấm "Bỏ qua".
- **Migration V8 (Issue #68, FR-05):** [V8__expert_application_notifications.sql](../app/mamxanh-backend/src/main/resources/db/migration/V8__expert_application_notifications.sql) thêm `NOTIFICATION.target_path` và hai index phục vụ truy vấn lịch sử/xét duyệt đơn Chuyên gia. Cần xác minh migration này cùng snapshot trên database sạch trước khi dùng kết quả kiểm tra V1–V7 làm bằng chứng cho baseline đã đồng bộ.
- **Verification Tests:** [database/queries.sql](queries.sql) có 41 test cases (TC01–TC41), gồm các assertion cho giới hạn đăng nhập và lời mời Onboarding. Kết quả 80/80 ngày 05/10/2026 được xác minh trên V1–V7; chưa bao gồm migration V8.

## Hướng dẫn kiểm thử và thẩm định

Kiểm tra toàn bộ schema và chạy 41 test cases (TC01–TC41) bằng `sqlcmd`; xác nhận kết quả thực tế trên database sạch:

```powershell
# 1. Khởi tạo database kiểm thử sạch
sqlcmd -S .\SQLEXPRESS -E -Q "DROP DATABASE IF EXISTS MamXanhDB_Test; CREATE DATABASE MamXanhDB_Test;"

# 2. Thực thi schema DDL (hoặc chạy lần lượt V1–V8 trong db/migration với cờ -I,
#    vì filtered index cần QUOTED_IDENTIFIER ON)
sqlcmd -S .\SQLEXPRESS -E -d MamXanhDB_Test -i database/schema.sql

# 3. Chạy bộ kiểm thử ràng buộc nghiệp vụ và xác nhận kết quả thực tế
sqlcmd -S .\SQLEXPRESS -E -d MamXanhDB_Test -i database/queries.sql
```

Xem [SRS](../docs/requirements/SRS.md), [ERD workspace](../docs/diagrams/ERD/README.md) và [Technology Stack](../docs/architecture/TECHNOLOGY-STACK.md).
