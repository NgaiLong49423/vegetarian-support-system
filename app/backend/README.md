> **Document:** Backend Workspace Guide  
> **File:** `app/backend/README.md`  
> **Version:** v0.1.1
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-13
> **Status:** Under Review  

# Backend Workspace

Thư mục này dành cho REST API Java 21 + Spring Boot, build bằng Maven và truy cập Microsoft SQL Server qua Spring Data JPA/Hibernate theo [Technology Stack](../../docs/architecture/TECHNOLOGY-STACK.md).

## Trạng thái hiện tại

Backend chưa được scaffold: chưa có `pom.xml`, source code, migration hoặc test. Vì vậy chưa có lệnh Maven hay endpoint nào được xác minh trong repository.

## Ranh giới kiến trúc hiện tại

- SRS là nguồn nghiệp vụ; Technology Stack là nguồn lựa chọn công nghệ. Monolith/module layout, package structure và AI architecture chưa được chốt.
- SQL Server là source of truth cho dữ liệu nghiệp vụ. Flyway phải quản lý migration theo thứ tự, còn JPA/Hibernate không thay thế lịch sử migration.
- Authentication, role/ownership, validation, quota và access tới dịch vụ ngoài phải được thực thi ở backend.
- Gemini, Azure, Maps và payment credential chỉ đến từ cấu hình môi trường/secret store; không commit giá trị thật.
- API được mô tả bằng OpenAPI và trả lỗi nhất quán; không để frontend suy đoán business rule.

## Definition of Done cho thay đổi backend

- Business Rule và failure case liên quan có test phù hợp.
- `mvn test` và build/package chạy thành công trên commit hiện tại sau khi Maven project tồn tại.
- Không log password, token, SAS URL hoặc dữ liệu cá nhân nhạy cảm.
- Thay đổi schema có Flyway migration append-only, kiểm tra trên database sạch và cập nhật ERD/tài liệu.
- Thay đổi API có OpenAPI, validation, authorization và ví dụ lỗi tương ứng.

Sau khi scaffold, bổ sung JDK/Maven yêu cầu, cấu hình local an toàn, profile test, lệnh chạy và các kiểm tra đã thực thi thật.
