---
id: ai-meal-recommendations
title: AI tạo thực đơn dinh dưỡng từ công thức có sẵn
category: ai-features
audience:
  - customer
  - expert
source_requirements:
  - FR-35
  - FR-36
  - FR-38
  - FR-63
  - BR-31
  - BR-38
  - BR-39
  - BR-40
  - BR-41
source_use_cases:
  - UC-36.1
  - UC-36.2
  - UC-36.3
source_files:
  - app/mamxanh-frontend/src/pages/MealPlanWeekEditor.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/controller/AiMenuController.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuOptimizer.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/integration/ai/AdkAiClient.java
implementation_status: verified
last_verified: 2026-11-21
---

# AI tạo thực đơn dinh dưỡng từ công thức có sẵn

## Chức năng này làm gì?

Trong trang Lịch ăn, thành viên có thể yêu cầu AI tạo bản xem trước thực đơn cho các ngày từ hôm nay đến hết Chủ Nhật của tuần hiện tại. Mỗi ngày có ba bữa cố định: sáng, trưa và tối. Mỗi bữa có thể gồm nhiều công thức, mỗi công thức được tính một khẩu phần trong preview.

AI không viết công thức mới. Thực đơn chỉ dùng công thức đã công khai trên Mâm Xanh và đủ dữ liệu để Backend tính dinh dưỡng. Ngày đã qua không được đề xuất; khi áp dụng, các bữa của ngày đã qua được giữ nguyên.

## Ai có thể dùng?

Member đã đăng nhập, có hồ sơ dinh dưỡng hợp lệ và đáp ứng điều kiện eligibility dinh dưỡng. Gói dịch vụ không phải điều kiện nghiệp vụ của FR-36; model được phép dùng và hạn mức AI vẫn có thể ảnh hưởng khả năng hoàn tất yêu cầu.

Hồ sơ sở thích ăn uống không cần hoàn tất toàn bộ chỉ để tạo menu FR-36. Tuy vậy, mọi ràng buộc đã khai báo vẫn được áp dụng. Nếu có dị ứng hoặc nguyên liệu cần tránh, người dùng phải khai báo để Backend loại các món có nguyên liệu đó. Dữ liệu chưa khai báo không được hiểu là xác nhận “không dị ứng”; hệ thống cũng không thể tự biết dị ứng chưa được cung cấp.

## Hai cách cá nhân hóa

- **Dựa trên hồ sơ:** Dùng mục tiêu dinh dưỡng chung đã lưu (nếu có), chỉ số hồ sơ và ràng buộc ăn uống đã khai báo. Mục tiêu hoặc sở thích chưa trả lời vẫn được xem là chưa khai báo, không tự suy đoán.
- **Theo mong muốn tuần này:** Có thể gửi mục tiêu riêng, nguyên liệu muốn ưu tiên và nguyên liệu muốn tránh cho lần tạo hiện tại. Đây là dữ liệu request-only, không cập nhật hồ sơ. Nguyên liệu muốn có chỉ là ưu tiên mềm; nguyên liệu cần tránh là điều kiện loại trừ cứng.

## Backend và Gemini phối hợp ra sao?

1. Backend kiểm tra Member, hồ sơ dinh dưỡng/eligibility, hạn mức AI, tuần được yêu cầu và các ràng buộc ăn uống.
2. Backend lấy công thức công khai, loại món vi phạm loại ăn chay hoặc nguyên liệu dị ứng/cần tránh, rồi loại món không đủ dữ liệu định lượng và dinh dưỡng tin cậy. Nếu không còn ứng viên phù hợp, hệ thống báo chưa đủ công thức thay vì tự tạo món.
3. Backend gửi Gemini thông tin của các ứng viên hợp lệ, gồm ID, tên, nguyên liệu và dữ liệu dinh dưỡng theo khẩu phần. Gemini trả về thứ tự ưu tiên các ID trong danh sách được cung cấp; Gemini không được tự thêm công thức hoặc số liệu dinh dưỡng.
4. Backend xác thực lại ID và ràng buộc sau khi Gemini phản hồi. Bộ tối ưu nội bộ dùng thứ hạng của Gemini cùng dữ liệu dinh dưỡng tin cậy để ghép món vào các ngày và bữa.
5. Backend trả preview để người dùng xem. Preview không ghi vào Lịch ăn.

Trong Chatbox, AI có thể điều phối các công cụ lấy ứng viên và tạo preview. Các công cụ vẫn dùng ứng viên được Backend kiểm tra và không cho phép AI bịa ID món. Đây là luồng hội thoại riêng; dữ liệu động và quyền truy cập vẫn phải do Backend xác minh.

## Mục tiêu phân bổ năng lượng

Hệ thống hướng tới phân bổ năng lượng ngày như sau:

| Bữa | Tỷ lệ mục tiêu | Khoảng tham khảo theo FR-36 |
|---|---:|---:|
| Sáng | 25% | 20%–30% |
| Trưa | 37,5% | 32,5%–42,5% |
| Tối | 37,5% | 32,5%–42,5% |

Đây là mục tiêu tối ưu, không phải cam kết mọi bữa sẽ đạt đúng tỷ lệ. Kết quả phụ thuộc các công thức phù hợp có trong kho và dữ liệu dinh dưỡng hiện có. Mục tiêu tuần chỉ giúp ưu tiên món; không làm thay đổi mục tiêu dinh dưỡng đã lưu, không phải phác đồ điều trị và không cam kết thay đổi cân nặng.

## Xem, chỉnh và áp dụng preview

1. Chọn chế độ cá nhân hóa và nhập các tùy chọn nếu muốn.
2. Nhấn **Tạo thực đơn tuần**. Các món trong preview được chọn sẵn; người dùng có thể bỏ chọn hoặc chọn lại trước khi lưu. Có thể tạo một preview khác.
3. Xem tên món, bữa/ngày và dinh dưỡng dự kiến. Nếu hài lòng, xác nhận áp dụng.
4. Backend kiểm tra lại quyền, ngày/bữa, trạng thái công khai, dữ liệu dinh dưỡng và ràng buộc món. Nếu dữ liệu không còn hợp lệ, thao tác bị từ chối; nếu hợp lệ, các món được lưu vào Lịch ăn của Member.

Thông tin preview có thể được giữ trong session của trình duyệt để tiếp tục xem sau khi tải lại cùng phiên. Vì vậy, preview đang hiển thị không nhất thiết là một lần gọi Gemini vừa xảy ra.

## Giới hạn và an toàn

- Chỉ công thức đang công khai, phù hợp ràng buộc và có dữ liệu dinh dưỡng đầy đủ mới được dùng.
- Nguyên liệu dị ứng/cần tránh đã khai báo là ràng buộc cứng; sở thích không thích là ưu tiên mềm.
- Con số dinh dưỡng là giá trị dự kiến tính từ công thức và khẩu phần trong hệ thống, không phải đo lường bữa ăn thực tế.
- BMI và mục tiêu cân nặng không được dùng để tự suy ra thâm hụt/thặng dư calorie.
- Thực đơn có tính chất tham khảo, không chẩn đoán hoặc điều trị bệnh. Khi không có đủ dữ liệu, hệ thống không được tự gán số liệu dinh dưỡng.
