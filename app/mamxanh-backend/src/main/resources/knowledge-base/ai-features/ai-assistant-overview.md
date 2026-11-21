---
id: ai-assistant-overview
title: Trợ lý AI Mâm Xanh: khả năng và giới hạn
category: ai-features
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-02
  - FR-21
  - FR-34
  - FR-36
  - FR-51
  - FR-66
  - BR-01
  - BR-03
  - BR-04
  - BR-06
  - BR-09
  - BR-38
source_files:
  - app/mamxanh-frontend/src/pages/AiChatPage.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/service/AiChatService.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/service/AiAssistantExecutionService.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/service/AiAssistantToolRegistry.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java
implementation_status: verified
last_verified: 2026-11-21
---

# Trợ lý AI Mâm Xanh: khả năng và giới hạn

## Trợ lý hỗ trợ những gì?

Trợ lý AI của Mâm Xanh hỗ trợ các câu hỏi về món chay, nguyên liệu, công thức công khai và cách sử dụng các chức năng trong ứng dụng. Tùy yêu cầu và quyền của tài khoản, trợ lý có thể tìm công thức công khai, giải thích cách nấu dựa trên bài nguồn, hướng dẫn mở đúng trang hoặc điều phối một số thao tác qua công cụ Backend.

Thành viên cũng có thể yêu cầu tạo preview thực đơn dinh dưỡng. Chức năng lập thực đơn dùng công thức công khai đủ dữ liệu dinh dưỡng và ràng buộc ăn uống; xem [AI tạo thực đơn dinh dưỡng từ công thức có sẵn](ai-meal-recommendations.md).

## AI dùng dữ liệu như thế nào?

Kho tri thức trong ứng dụng chứa thông tin nghiệp vụ tĩnh như hướng dẫn sử dụng và chính sách. Dữ liệu thay đổi theo tài khoản, như hồ sơ, sở thích, lịch ăn hoặc danh sách đi chợ, phải được lấy bằng công cụ Backend có kiểm tra quyền và điều kiện nghiệp vụ. Trợ lý không được đoán dữ liệu riêng của bạn từ tri thức tĩnh.

Khi cần dùng dữ liệu hồ sơ, hệ thống áp dụng quy tắc đồng ý phù hợp với chức năng. Hãy xem nội dung xác nhận trong giao diện trước khi đồng ý chia sẻ dữ liệu cho lượt xử lý đó.

## Kết quả AI có thể làm gì?

- Hỏi đáp và gợi ý chỉ cung cấp câu trả lời hoặc bản xem trước; không tự ghi dữ liệu nghiệp vụ.
- Hành động ghi dữ liệu phải đi qua Backend để kiểm tra danh tính, quyền sở hữu, tính hợp lệ và xác nhận theo chức năng.
- Với thực đơn AI, preview không sửa Lịch ăn. Người dùng xem và xác nhận áp dụng trước khi các món được lưu.
- Khi AI gợi ý công thức trên hệ thống, món phải trỏ về Recipe Post công khai có thật. Nếu không tìm được món phù hợp, trợ lý cần nói rõ thay vì bịa món hoặc ID công thức.

## Giới hạn và an toàn

- Trợ lý không chẩn đoán bệnh, kê đơn hoặc lập chế độ ăn điều trị. Thông tin dinh dưỡng trong app là tham khảo, không thay thế chuyên gia.
- Nếu thiếu dữ liệu bắt buộc của một tính năng, Trợ lý phải giải thích dữ liệu còn thiếu và không tự giả định.
- Dị ứng và nguyên liệu tránh chỉ được áp dụng khi người dùng đã khai báo hoặc cung cấp trong yêu cầu. Ô trống không đồng nghĩa với “không dị ứng”.
- Không khẳng định một thao tác đã hoàn tất nếu Backend chưa trả về kết quả thành công.
- Nếu Gemini hoặc dịch vụ AI không khả dụng, hệ thống có thể yêu cầu thử lại; không được trình bày phản hồi mô phỏng như kết quả Gemini thật.

## Hạn mức và gói dịch vụ

Hạn mức token, Credit, model được phép và tính năng phụ thuộc cấu hình hiện hành của tài khoản và hệ thống. Các mức giá hoặc hạn mức có thể thay đổi; hãy xem trang Gói dịch vụ AI để biết thông tin đang được ứng dụng hiển thị. Trợ lý không nên tự đưa ra số dư, quota còn lại, model đang chọn hoặc giao dịch của một tài khoản nếu chưa truy vấn dữ liệu hiện hành bằng công cụ phù hợp.
