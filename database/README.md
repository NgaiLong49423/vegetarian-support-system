> **Document:** Database Workspace Guide  
> **File:** `database/README.md`  
> **Version:** v0.4.1  
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-25  
> **Status:** Under Review  

# Database Workspace

Database chính đã chốt là Microsoft SQL Server 2019. Lược đồ cơ sở dữ liệu đã được hoàn thiện đầy đủ trong khuôn khổ [Issue #63](https://github.com/NgaiLong49423/vegetarian-support-system/issues/63) với 22 bảng, 38 khóa ngoại (bao gồm 2 composite FKs: `COMMENT` và `MEAL_PLAN_ENTRY`), 57 ràng buộc CHECK, 55 ràng buộc DEFAULT, 55 index (22 PK, 9 UNIQUE constraint, 24 index tạo riêng; trong đó 10 filtered index gồm 8 unique và 2 không unique), seed 15 dòng đơn vị chuẩn trong bảng `UNIT`, cùng chính sách chống multiple cascade paths (lỗi SQL Server Error 1785).

## Quyền sở hữu dữ liệu

- Flyway migration trong backend (`app/mamxanh-backend/src/main/resources/db/migration/V1__baseline_schema.sql`) là lịch sử thay đổi schema có thẩm quyền và phải append-only sau khi đã chia sẻ.
- `database/schema.sql` là snapshot/manual bootstrap độc lập được đồng bộ có chủ đích từ Flyway baseline; dùng cho khởi tạo nhanh trên SSMS, Azure Data Studio hoặc `sqlcmd`.
- `database/sample-data.sql` chỉ chứa dữ liệu demo giả, không chứa tài khoản thật, credential hoặc dữ liệu cá nhân.
- `database/queries.sql` chứa kịch bản kiểm tra đối tượng, bộ test tự động xác minh các ràng buộc nghiệp vụ (positive/negative) có cơ chế rollback, và các truy vấn mẫu cho tầng ứng dụng; không thay thế automated integration tests.

## Quy trình thay đổi schema

1. Truy vết thay đổi đến SRS/Issue và xác nhận không mở rộng scope ngoài quyết định đã duyệt.
2. Thêm Flyway migration mới trong backend; không sửa migration đã được chia sẻ hoặc chạy ở môi trường chung.
3. Cập nhật entity/DTO/repository và test liên quan.
4. Kiểm tra migration trên database sạch và, khi phù hợp, đường nâng cấp từ baseline gần nhất.
5. Đồng bộ ERD, `database/schema.sql`, OpenAPI/SRS và `CHANGELOG.md` khi bị ảnh hưởng.
6. PR cần hai người kiểm tra theo ADR-002.

## Trạng thái thiết kế

- **Conceptual ERD:** 22 thực thể, 36 connector (Đã duyệt).
- **Logical ERD:** [logical-erd-v1.0.0.drawio](../docs/diagrams/ERD/logical-erd-v1.0.0.drawio) (22 bảng, 37 connector thể hiện 36 quan hệ; cập nhật lần cuối ở commit `827353e`).
- **Physical ERD:** [physical-erd-v1.0.0.drawio](../docs/diagrams/ERD/physical-erd-v1.0.0.drawio) & [physical-erd-v1.0.0.drawio.png](../docs/diagrams/ERD/physical-erd-v1.0.0.drawio.png) (22 bảng, 37 connector, 196 physical columns với đầy đủ kiểu dữ liệu, nullability, constraints, indexes).
- **Data Dictionary:** [data-dictionary.md](../docs/diagrams/ERD/data-dictionary.md) v0.7.1 (22 bảng, 196 cột physical, hoàn thành triển khai toàn bộ 33 mục đánh dấu sau review PR #66).
- **Schema & Migration:** [V1__baseline_schema.sql](../app/mamxanh-backend/src/main/resources/db/migration/V1__baseline_schema.sql) và [database/schema.sql](schema.sql) đã triển khai đầy đủ 22 tables, 38 FKs, 57 CHECK constraints, 55 DEFAULT constraints, 55 index (22 PK, 9 UNIQUE constraint, 24 index tạo riêng; trong đó 10 filtered index gồm 8 unique và 2 không unique), seed 15 dòng `UNIT`. Kiểm thử thành công 100% trên clean database Microsoft SQL Server 2019 thật (`.\SQLEXPRESS`).
- **Verification Tests:** [database/queries.sql](queries.sql) gồm 37 automated test cases (TC01–TC37, 70/70 test assertions PASS 100%) kiểm thử toàn bộ positive/negative business constraints và assert chính xác tên constraint trong `ERROR_MESSAGE()`.

## Hướng dẫn kiểm thử và thẩm định

Kiểm tra toàn bộ schema và chạy 37 test cases (TC01–TC37) bằng `sqlcmd`:

```powershell
# 1. Khởi tạo database kiểm thử sạch
sqlcmd -S .\SQLEXPRESS -E -Q "DROP DATABASE IF EXISTS MamXanhDB_Test; CREATE DATABASE MamXanhDB_Test;"

# 2. Thực thi schema DDL (hoặc V1__baseline_schema.sql)
sqlcmd -S .\SQLEXPRESS -E -d MamXanhDB_Test -i database/schema.sql

# 3. Chạy bộ kiểm thử ràng buộc nghiệp vụ (70/70 test assertions PASS)
sqlcmd -S .\SQLEXPRESS -E -d MamXanhDB_Test -i database/queries.sql
```

Xem [SRS](../docs/requirements/SRS.md), [ERD workspace](../docs/diagrams/ERD/README.md) và [Technology Stack](../docs/architecture/TECHNOLOGY-STACK.md).

