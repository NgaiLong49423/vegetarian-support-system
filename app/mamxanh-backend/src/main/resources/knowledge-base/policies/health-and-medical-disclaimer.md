---
id: health-and-medical-disclaimer
title: Tuyên bố miễn trừ trách nhiệm y tế và dinh dưỡng
category: policies
audience:
  - guest
  - customer
  - expert
source_requirements:
  - BR-09
  - BR-39
  - BR-41
  - BR-42
source_use_cases:
  - UC-38.1
  - UC-38.2
source_files:
  - docs/requirements/srs/BUSINESS-RULES.md
  - docs/requirements/SRS.md
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/service/AiChatService.java
implementation_status: verified
last_verified: 2026-11-01
---

# Tuyên bố miễn trừ trách nhiệm y tế và dinh dưỡng

## 1. Mục đích của tài liệu

Mâm Xanh luôn đặt sự an toàn và sức khỏe của người dùng lên hàng đầu. Tài liệu này xác lập ranh giới rõ ràng giữa **thông tin tham khảo lối sống ẩm thực** và **tư vấn y tế chuyên nghiệp**, giúp người dùng hiểu đúng bản chất của các tính năng trên website.

## 2. Ranh giới thông tin dinh dưỡng

1. **Tính chất tham khảo:**
   - Mọi số liệu dinh dưỡng (bao gồm năng lượng calorie, chất đạm, chất béo, chất bột đường, chất xơ và các vi chất như canxi, sắt, vitamin B12, kẽm) hiển thị trên bài viết công thức, bảng theo dõi dinh dưỡng và báo cáo xuất file đều là **ước tính lý thuyết phục vụ việc lập kế hoạch ăn uống**.
   - Các giá trị này được tính toán dựa trên dữ liệu thành phần dinh dưỡng trung bình của 100g nguyên liệu có nguồn gốc từ Viện Dinh dưỡng Quốc gia và USDA. Quá trình sơ chế thực tế, nguồn gốc rau củ, nhiệt độ nấu và thời gian chế biến có thể làm thay đổi hàm lượng vi chất thực tế trong món ăn.
2. **Không phải chẩn đoán y khoa:**
   - Việc bảng phân tích dinh dưỡng thực đơn hiển thị một chỉ tiêu *"Thấp hơn mức tham khảo"* chỉ đơn thuần mô tả kế hoạch bữa ăn ngày hôm đó của bạn, **tuyệt đối không phải là kết luận hay chẩn đoán rằng bạn đang bị thiếu hụt vi chất hay suy dinh dưỡng**.
   - Nền tảng không bao giờ dán nhãn "Thực đơn xấu" hay "Món ăn độc hại" một cách đơn giản hóa.
3. **Chỉ số BMI chỉ là chỉ số sàng lọc thể trạng:**
   - Chỉ số khối cơ thể (BMI) tính từ chiều cao và cân nặng của bạn chỉ đóng vai trò tham khảo thể trạng theo khuyến nghị của Tổ chức Y tế Thế giới (WHO). Chỉ số này không phản ánh tỷ lệ mỡ cơ thể, khối lượng cơ bắp hay tình trạng bệnh lý, và không thể thay thế việc khám sức khỏe định kỳ.

## 3. Ranh giới của Trợ lý AI

1. **AI không phải là bác sĩ hay chuyên gia dinh dưỡng lâm sàng:**
   - Trợ lý AI của Mâm Xanh được huấn luyện để trả lời về công thức, mẹo nấu nướng, cách sơ chế nguyên liệu và điều phối thực đơn.
   - AI **tuyệt đối không đưa ra lời khuyên chẩn đoán bệnh, không kê đơn thực phẩm chữa bệnh, không tư vấn sử dụng thuốc và không xây dựng phác đồ ăn kiêng điều trị bệnh lý**.
2. **Từ chối các câu hỏi y khoa đặc thù:**
   - Khi nhận được câu hỏi liên quan đến điều trị bệnh (ví dụ: *"Bị suy thận độ 3 có ăn được nấm này không?", "Món này có trị được ung thư không?", "Người bệnh tiểu đường thai kỳ nên uống gì để hạ đường huyết?"*), AI sẽ từ chối trả lời và hướng dẫn bạn tham khảo ý kiến trực tiếp từ bác sĩ chuyên khoa hoặc chuyên gia dinh dưỡng lâm sàng.

## 4. Đối tượng cần lưu ý đặc biệt

Tính năng phân tích nhu cầu dinh dưỡng cá nhân trên Mâm Xanh chỉ phục vụ người trưởng thành (từ đủ 18 tuổi trở lên) có sức khỏe bình thường. Nếu bạn thuộc các nhóm đối tượng sau:
- Trẻ em và thanh thiếu niên dưới 18 tuổi (giai đoạn đang phát triển thể chất đặc thù).
- Phụ nữ đang mang thai hoặc đang trong thời kỳ cho con bú.
- Người đang mắc các bệnh lý mạn tính cần chế độ dinh dưỡng y học chuyên biệt (tiểu đường, bệnh thận, bệnh gan, tim mạch...).

$\rightarrow$ **Bạn vui lòng tham khảo ý kiến của bác sĩ hoặc chuyên viên y tế có chuyên môn trước khi áp dụng bất kỳ thực đơn ăn chay nào dài hạn.**
