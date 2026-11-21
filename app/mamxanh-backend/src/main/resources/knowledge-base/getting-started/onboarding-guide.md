---
id: onboarding-guide
title: Khởi tạo và cập nhật sở thích ăn uống
category: getting-started
audience:
  - customer
  - expert
source_requirements:
  - FR-31
  - FR-36
  - BR-30
  - BR-31
  - BR-38
source_use_cases:
  - UC-31.1
  - UC-31.2
  - UC-31.3
  - UC-36.1
source_files:
  - app/mamxanh-frontend/src/pages/OnboardingPage.tsx
  - app/mamxanh-frontend/src/pages/DietaryPreferencesPage.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/nutrition/controller/DietaryPreferenceController.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java
implementation_status: verified
last_verified: 2026-11-21
---

# Khởi tạo và cập nhật sở thích ăn uống

## Onboarding dùng để làm gì?

Sau khi đăng ký, Mâm Xanh có thể mời bạn khai báo trường phái ăn chay, nguyên liệu dị ứng/cần tránh và món hoặc nguyên liệu không thích. Bạn cũng có thể cập nhật các lựa chọn này trong trang **Sở thích ăn uống**.

Các lựa chọn đã khai báo giúp hệ thống lọc công thức và cá nhân hóa gợi ý. Nếu không hoàn thành hoặc bỏ qua onboarding, lời mời không tự xuất hiện lại ở lần đăng nhập sau; bạn vẫn có thể vào phần hồ sơ để bổ sung thông tin.

## Bỏ trống không đồng nghĩa với “không có”

Giá trị chưa khai báo được giữ là chưa biết. Trợ lý không được kết luận rằng bạn không dị ứng, không kiêng hoặc ăn được mọi loại thực phẩm chỉ vì hồ sơ để trống.

Nếu bạn có dị ứng hoặc nguyên liệu cần tránh, hãy khai báo chính xác trong sở thích ăn uống. Backend dùng các ràng buộc đã khai báo để loại công thức không phù hợp trước khi tạo gợi ý hoặc thực đơn. Hệ thống không thể tự phát hiện một dị ứng mà bạn chưa cung cấp; hãy kiểm tra danh sách nguyên liệu của công thức trước khi nấu.

## Onboarding có bắt buộc để tạo thực đơn AI không?

Không cần hoàn tất toàn bộ bảng hỏi sở thích chỉ để dùng chức năng FR-36 tạo thực đơn theo dinh dưỡng. Chức năng đó vẫn yêu cầu Member đăng nhập, hồ sơ dinh dưỡng hợp lệ và đủ điều kiện dinh dưỡng. Các sở thích và nguyên liệu tránh đã khai báo sẽ được áp dụng; các mục chưa khai báo không được suy đoán.

Một số tính năng gợi ý cá nhân hóa khác có thể có điều kiện riêng. Nếu một yêu cầu cần dữ liệu hồ sơ mà hệ thống chưa có, Trợ lý cần nói rõ dữ liệu nào còn thiếu thay vì tự giả định.

## Câu hỏi thường gặp

### Nếu tôi khai báo dị ứng, hệ thống có loại món chứa nguyên liệu đó không?

Backend loại các công thức có nguyên liệu dị ứng/cần tránh đã khai báo khỏi tập ứng viên của thực đơn AI. Đây là kiểm tra theo dữ liệu nguyên liệu của công thức trong Mâm Xanh, không phải bảo đảm y tế tuyệt đối về nhiễm chéo hoặc thành phần không được ghi trong bài. Hãy luôn kiểm tra bài công thức và nhãn thực phẩm thực tế.

### Nếu tôi không bị dị ứng thì sao?

Bạn có thể khai báo rõ không có nguyên liệu cần tránh nếu biểu mẫu cung cấp lựa chọn đó. Đừng để Trợ lý tự suy ra câu trả lời này từ một ô trống.
