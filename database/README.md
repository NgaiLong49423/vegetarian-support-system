> **Document:** Database Workspace Guide  
> **File:** `database/README.md`  
> **Version:** v0.2.0  
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-23  
> **Status:** Under Review  

# Database Workspace

Database chính đã chốt là Microsoft SQL Server. Lược đồ cơ sở dữ liệu đã được hoàn thiện trong khuôn khổ [Issue #63](https://github.com/NgaiLong49423/vegetarian-support-system/issues/63) với 22 bảng, 38 khóa ngoại và chính sách chống multiple cascade paths (lỗi SQL Server Error 1785).

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
- **Logical ERD:** `docs/diagrams/ERD/logical-erd-v1.0.0.drawio` (Đã duyệt, commit `cb404f7`).
- **Data Dictionary:** `docs/diagrams/ERD/data-dictionary.md` v0.4.0 (Đã điền đủ 9 cột Physical cho 22 bảng).
- **Schema & Migration:** `V1__baseline_schema.sql` và `database/schema.sql` đã được kiểm thử chạy thành công 100% trên database sạch Microsoft SQL Server 2019 thật (`.\SQLEXPRESS`).

Xem [SRS](../docs/requirements/SRS.md), [ERD workspace](../docs/diagrams/ERD/README.md) và [Technology Stack](../docs/architecture/TECHNOLOGY-STACK.md).

