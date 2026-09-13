> **Document:** Database Workspace Guide  
> **File:** `database/README.md`  
> **Version:** v0.1.1
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-13
> **Status:** Under Review  

# Database Workspace

Database chính đã chốt là Microsoft SQL Server. Repository hiện có ba file SQL rỗng (`schema.sql`, `sample-data.sql`, `queries.sql`); chúng chưa mô tả schema đã duyệt và không phải bằng chứng database có thể khởi tạo.

## Quyền sở hữu dữ liệu

- Khi backend được scaffold, Flyway migration trong backend là lịch sử thay đổi schema có thẩm quyền và phải append-only sau khi đã chia sẻ.
- `database/schema.sql` chỉ nên là snapshot/manual bootstrap được sinh hoặc đồng bộ có chủ đích; không được âm thầm đi trước hoặc mâu thuẫn với Flyway.
- `database/sample-data.sql` chỉ chứa dữ liệu demo giả, không chứa tài khoản thật, credential hoặc dữ liệu cá nhân.
- `database/queries.sql` dành cho truy vấn kiểm tra có giải thích; không thay thế automated integration tests.

## Quy trình thay đổi schema

1. Truy vết thay đổi đến SRS/Issue và xác nhận không mở rộng scope ngoài quyết định đã duyệt.
2. Thêm Flyway migration mới trong backend; không sửa migration đã được chia sẻ hoặc chạy ở môi trường chung.
3. Cập nhật entity/DTO/repository và test liên quan.
4. Kiểm tra migration trên database sạch và, khi phù hợp, đường nâng cấp từ baseline gần nhất.
5. Đồng bộ ERD, `database/schema.sql`, OpenAPI/SRS và `CHANGELOG.md` khi bị ảnh hưởng.
6. PR cần hai người kiểm tra theo ADR-002.

## Trạng thái thiết kế

ERD và danh sách migration chưa tồn tại. Không tự suy ra bảng chỉ từ danh sách dữ liệu gợi ý trong SRS; cần hoàn thiện data model và review trước khi coi schema là baseline.

Xem [SRS](../docs/requirements/SRS.md), [ERD workspace](../docs/diagrams/ERD/README.md) và [Technology Stack](../docs/architecture/TECHNOLOGY-STACK.md).
