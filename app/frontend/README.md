> **Document:** Frontend Workspace Guide  
> **File:** `app/frontend/README.md`  
> **Version:** v0.1.0  
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-12  
> **Status:** Under Review  

# Frontend Workspace

Thư mục này dành cho ứng dụng web React + TypeScript dùng Vite, npm và Axios theo [Technology Stack](../../docs/decisions/SWP-Technology-Stack-v2.0.0.txt).

## Trạng thái hiện tại

Frontend chưa được scaffold: chưa có `package.json`, source code hoặc test. Vì vậy chưa có lệnh `npm install`, `npm run dev`, `npm test` hay `npm run build` nào được xác minh trong repository.

## Hợp đồng khi khởi tạo

- Không chọn thêm state-management hoặc CSS/UI library trước khi nhóm ghi quyết định; hai mục này vẫn là `TBD`.
- API base URL và secret phải đến từ cấu hình môi trường. Không đưa key Gemini, Azure, Maps hoặc payment vào frontend.
- Kiểu TypeScript phải bám OpenAPI/DTO đã thống nhất; interface phía client không tự chứng minh JSON runtime hợp lệ.
- Tổ chức code theo feature hoặc module nhất quán, tránh tạo tầng trừu tượng khi chưa có nhu cầu thực tế.
- Mỗi trạng thái gọi API cần có loading, empty, error và authorization behavior phù hợp.

## Definition of Done cho thay đổi frontend

- Acceptance Criteria và actor/permission đã được kiểm tra.
- Type check, lint, test và production build đã chạy bằng lệnh thật được khai báo trong `package.json`.
- Không log token hoặc dữ liệu cá nhân nhạy cảm.
- Giao diện responsive tiếng Việt và xử lý lỗi/quota dịch vụ ngoài rõ ràng.
- Nếu API, cấu hình hoặc cách chạy thay đổi, cập nhật OpenAPI và README liên quan.

Sau khi scaffold, thay phần trạng thái bằng yêu cầu Node/npm, biến môi trường mẫu không chứa secret, lệnh chạy và kết quả kiểm chứng.
