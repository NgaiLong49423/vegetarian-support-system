> **Document:** Database Workspace Guide  
> **File:** `database/README.md`  
> **Version:** v0.7.1<br>
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-30<br>
> **Status:** Active  

# Database Workspace

Database chính đã chốt là Microsoft SQL Server 2019. Lược đồ cơ sở dữ liệu có 22 bảng, 38 khóa ngoại (bao gồm 2 composite FKs: `COMMENT` và `MEAL_PLAN_ENTRY`), 58 ràng buộc CHECK, 56 ràng buộc DEFAULT, 55 index (22 PK, 9 UNIQUE constraint, 24 index tạo riêng; trong đó 10 filtered index gồm 8 unique và 2 không unique), seed 15 dòng đơn vị chuẩn trong bảng `UNIT`, cùng chính sách chống multiple cascade paths (lỗi SQL Server Error 1785).

## Quyền sở hữu dữ liệu

- Flyway migration trong backend (`app/mamxanh-backend/src/main/resources/db/migration/`, hiện gồm `V1__baseline_schema.sql`, `V2__unit_code_unicode.sql` và `V3__ingredient_group_and_unit_validation.sql`) là lịch sử thay đổi schema có thẩm quyền và phải append-only sau khi đã chia sẻ.
- `database/schema.sql` là snapshot/manual bootstrap độc lập, được đồng bộ có chủ đích với trạng thái sau khi chạy toàn bộ Flyway migration; dùng cho khởi tạo nhanh trên SSMS, Azure Data Studio hoặc `sqlcmd`.
- `database/sample-data.sql` chỉ chứa dữ liệu demo giả, không chứa tài khoản thật, credential hoặc dữ liệu cá nhân.
- `database/queries.sql` chứa kịch bản kiểm tra đối tượng, bộ test tự động xác minh các ràng buộc nghiệp vụ (positive/negative) có cơ chế rollback, và các truy vấn mẫu cho tầng ứng dụng; không thay thế automated integration tests.

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
     - Brute-force rate limit: `failed_login_attempts` (INT DEFAULT 0), `login_blocked_until` (DATETIME2 NULL), `last_failed_login_at` (DATETIME2 NULL).
     - Metadata rate limit email (Q27): cho phép bổ sung các trường tối thiểu trên `USER` nếu cần theo dõi 60s cooldown và tối đa 5 email/giờ/tài khoản.
   - Developer (Tony) sẽ viết Flyway migration mới trong các Issue thực thi (#5, #6, #9) và cập nhật snapshot `database/schema.sql`.
   - **Ranh giới Diagram Artifact Protection:** Thư mục `docs/diagrams/ERD/` (Physical ERD, Logical ERD) là presentation workspace do con người duy trì và được bảo vệ theo `AGENTS.md`. Việc thay đổi schema hoặc migration **tuyệt đối không tự động cấp quyền sửa hoặc regenerate ERD diagrams** cho coding agent trừ khi có task riêng được ủy quyền tường minh.
3. **Quy trình thực hiện migration:**
   - Truy vết thay đổi đến SRS/Issue và xác nhận không mở rộng scope ngoài quyết định đã duyệt.
   - Thêm Flyway migration mới trong backend (`V3__...`); không sửa migration đã được chia sẻ.
   - Cập nhật entity/DTO/repository và test liên quan.
   - Cập nhật snapshot `database/schema.sql`.
   - Kiểm tra migration trên database sạch và kiểm thử nâng cấp.
   - PR cần review theo ADR-002.

## Trạng thái thiết kế

- **Conceptual ERD:** 22 thực thể, 36 connector (Đã duyệt).
- **Logical ERD:** [logical-erd-v1.0.0.drawio](../docs/diagrams/ERD/logical-erd-v1.0.0.drawio) (22 bảng, 37 connector thể hiện 36 quan hệ; cập nhật lần cuối ở commit `827353e`).
- **Physical ERD:** [physical-erd-v1.0.0.drawio](../docs/diagrams/ERD/physical-erd-v1.0.0.drawio) & [physical-erd-v1.0.0.drawio.png](../docs/diagrams/ERD/physical-erd-v1.0.0.drawio.png) (22 bảng, 37 connector, 196 physical columns với đầy đủ kiểu dữ liệu, nullability, constraints, indexes).
- **Data Dictionary:** [data-dictionary.md](../docs/diagrams/ERD/data-dictionary.md) v0.7.2 (22 bảng, 196 cột physical, hoàn thành triển khai toàn bộ 33 mục đánh dấu sau review PR #66).
- **Schema & Migration:** [V1__baseline_schema.sql](../app/mamxanh-backend/src/main/resources/db/migration/V1__baseline_schema.sql), [V2__unit_code_unicode.sql](../app/mamxanh-backend/src/main/resources/db/migration/V2__unit_code_unicode.sql) (đổi `UNIT.code` sang `NVARCHAR(20)` và khôi phục các mã `quả`, `củ`, `miếng` bị mất dấu), [V3__ingredient_group_and_unit_validation.sql](../app/mamxanh-backend/src/main/resources/db/migration/V3__ingredient_group_and_unit_validation.sql) (thêm `INGREDIENT.ingredient_group` và ràng buộc `UNIT.base_factor > 0`) và [database/schema.sql](schema.sql) là snapshot đồng bộ với toàn bộ migration. Kiểm thử baseline V1/V2 trên clean database Microsoft SQL Server 2019 thật (`.\SQLEXPRESS`) đã được ghi nhận; V3 cần được kiểm tra trên database sạch trước nghiệm thu Issue #24.
- **Verification Tests:** [database/queries.sql](queries.sql) gồm 38 automated test cases (TC01–TC38, 72/72 test assertions PASS 100%) kiểm thử toàn bộ positive/negative business constraints và assert chính xác tên constraint trong `ERROR_MESSAGE()`.

## Hướng dẫn kiểm thử và thẩm định

Kiểm tra toàn bộ schema và chạy 38 test cases (TC01–TC38) bằng `sqlcmd`:

```powershell
# 1. Khởi tạo database kiểm thử sạch
sqlcmd -S .\SQLEXPRESS -E -Q "DROP DATABASE IF EXISTS MamXanhDB_Test; CREATE DATABASE MamXanhDB_Test;"

# 2. Thực thi schema DDL (hoặc chạy lần lượt V1__baseline_schema.sql, V2__unit_code_unicode.sql rồi V3__ingredient_group_and_unit_validation.sql)
sqlcmd -S .\SQLEXPRESS -E -d MamXanhDB_Test -i database/schema.sql

# 3. Chạy bộ kiểm thử ràng buộc nghiệp vụ (72/72 test assertions PASS)
sqlcmd -S .\SQLEXPRESS -E -d MamXanhDB_Test -i database/queries.sql
```

Xem [SRS](../docs/requirements/SRS.md), [ERD workspace](../docs/diagrams/ERD/README.md) và [Technology Stack](../docs/architecture/TECHNOLOGY-STACK.md).

