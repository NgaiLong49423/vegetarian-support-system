---
id: nutrition-tracking
title: Theo dõi và phân tích dinh dưỡng thực đơn
category: meal-planning
audience:
  - customer
  - expert
source_requirements:
  - FR-35
  - FR-37
  - FR-38
  - FR-39
  - BR-39
  - BR-41
  - BR-42
  - BR-43
  - BR-44
  - BR-45
  - BR-47
source_use_cases:
  - UC-35.1
  - UC-35.2
  - UC-35.3
  - UC-37.1
  - UC-37.2
  - UC-37.3
  - UC-37.5
  - UC-38.1
  - UC-38.2
  - UC-38.3
  - UC-39.1
source_files:
  - app/mamxanh-frontend/src/pages/NutritionTracker.tsx
  - app/mamxanh-frontend/src/pages/NutritionProfile.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/nutrition/controller/NutritionController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Theo dõi và phân tích dinh dưỡng thực đơn

## Chức năng này dùng để làm gì?

Tính năng **Dinh dưỡng** (`/dinh-duong`) và **Hồ sơ dinh dưỡng** (`/ho-so/dinh-duong`) giúp bạn đối chiếu thực đơn ăn uống thực tế của mình với các mức khuyến nghị dinh dưỡng tham khảo hàng ngày.

Hệ thống tính toán tổng hợp từ các món ăn bạn đã xếp vào Lịch ăn tuần theo 9 chỉ tiêu dinh dưỡng quan trọng, giúp bạn biết chế độ ăn của mình đang đáp ứng năng lượng và vi chất ra sao.

## Ai có thể sử dụng? (Điều kiện tham gia bắt buộc)

Theo quy định an toàn y tế và sức khỏe (BR-42), chức năng dinh dưỡng cá nhân **chỉ dành cho người dùng đáp ứng đủ 3 điều kiện**:
1. **Từ đủ 18 tuổi trở lên.**
2. **Không đang mang thai hoặc cho con bú.**
3. **Không cần chế độ ăn kiêng điều trị bệnh lý đặc biệt.**

Trước khi mở tính năng dinh dưỡng lần đầu, hệ thống sẽ yêu cầu bạn xác nhận các điều kiện trên. Nếu bạn không thuộc nhóm đối tượng này, hệ thống sẽ tạm khóa chức năng dinh dưỡng cá nhân nhằm bảo đảm an toàn sức khỏe cho bạn. Các chức năng khác như tìm kiếm, lưu công thức hay đi chợ vẫn hoạt động hoàn toàn bình thường.

## 9 chỉ tiêu dinh dưỡng được theo dõi

Mâm Xanh theo dõi đúng 9 chỉ tiêu dinh dưỡng nền tảng dựa trên khuyến nghị từ Viện Dinh dưỡng và USDA:
1. **Năng lượng (Calorie - kcal):** Năng lượng cung cấp cho mọi hoạt động sống.
2. **Chất đạm (Protein - g):** Xây dựng và phục hồi mô cơ, đặc biệt quan trọng với người ăn chay.
3. **Chất bột đường (Carbohydrate - g):** Nguồn cung cấp năng lượng chính cho não bộ và cơ bắp.
4. **Tổng chất béo (Fat - g):** Dự trữ năng lượng và hỗ trợ hấp thu các vitamin tan trong dầu (A, D, E, K).
5. **Chất xơ (Fiber - g):** Hỗ trợ tiêu hóa, duy trì đường huyết và tạo cảm giác no lâu.
6. **Canxi (Calcium - mg):** Phát triển và bảo vệ xương, răng, hỗ trợ dẫn truyền thần kinh.
7. **Sắt (Iron - mg):** Tham gia cấu tạo hồng cầu và vận chuyển oxy.
8. **Vitamin B12 (mcg):** Hỗ trợ hệ thần kinh và tạo máu; đây là vi chất người ăn thuần chay cần đặc biệt lưu ý.
9. **Kẽm (Zinc - mg):** Củng cố hệ miễn dịch và thúc đẩy quá trình chuyển hóa.

*(Lưu ý: Chỉ số Natri/Muối không nằm trong phạm vi theo dõi hiện tại của nền tảng).*

## Chỉ số BMI và mức tham khảo cá nhân

Tại trang **Hồ sơ dinh dưỡng** (`/ho-so/dinh-duong`), bạn nhập các thông số: Chiều cao, Cân nặng, Độ tuổi, Giới tính sinh học và Mức độ vận động thể chất.

Hệ thống sẽ:
- Tính toán chỉ số khối cơ thể **BMI** và hiển thị phân loại thể trạng tham khảo (Thiếu cân, Bình thường, Thừa cân...).
- Ước tính **Nhu cầu Calorie và dưỡng chất tham khảo hàng ngày** dành riêng cho thể trạng của bạn.

## Cách xem phân tích dinh dưỡng thực đơn ngày

1. Truy cập trang **Dinh dưỡng** (`/dinh-duong`) hoặc bấm tab phân tích ngay trên **Kế hoạch bữa ăn**.
2. Chọn ngày bạn muốn kiểm tra trong tuần.
3. Hệ thống sẽ cộng dồn dinh dưỡng của tất cả các món ăn trong Bữa sáng, Bữa trưa và Bữa tối của ngày đó (đã nhân với số khẩu phần bạn dự định ăn).
4. Mỗi chỉ tiêu sẽ hiển thị mức đạt được so với mức tham khảo cá nhân qua 4 trạng thái rõ ràng:
   - **Trong khoảng tham khảo:** Chế độ ăn ngày hôm đó đáp ứng tốt mức khuyến nghị.
   - **Thấp hơn mức tham khảo:** Lượng nạp vào chưa đạt mức khuyến nghị ngày.
   - **Cao hơn mức tham khảo:** Lượng nạp vào vượt mức khuyến nghị ngày.
   - **Không đủ dữ liệu để đánh giá:** Có một hoặc nhiều món ăn chứa nguyên liệu chưa có dữ liệu dinh dưỡng trong từ điển.

## Những điều cần lưu ý

1. **Ranh giới y tế và sức khỏe (Health Disclaimer):**
   - Chỉ số BMI và các con số dinh dưỡng chỉ mang tính chất **ước tính tham khảo phục vụ lập kế hoạch ăn uống**.
   - Việc một ngày có chỉ tiêu "Thấp hơn mức tham khảo" chỉ phản ánh thực đơn dự kiến của ngày hôm đó, **tuyệt đối không phải là kết luận chẩn đoán bạn bị thiếu chất hay suy dinh dưỡng**.
   - Mâm Xanh không đưa ra chẩn đoán bệnh lý, chỉ định y khoa hay cam kết tăng/giảm cân.
2. **Không dùng nhãn đơn giản hóa "Tốt / Xấu":**
   - Nền tảng không chấm điểm "Health Score" tổng hợp hay dán nhãn "Thực đơn tốt / Thực đơn xấu". Bạn được cung cấp đầy đủ dữ liệu định lượng của từng chỉ tiêu để tự đánh giá một cách minh bạch.
3. **Dữ liệu thiếu không được coi là bằng 0:**
   - Nếu một món ăn có nguyên liệu chưa có số liệu dinh dưỡng, hệ thống sẽ cảnh báo rõ ràng thay vì tự ý coi lượng dinh dưỡng đó bằng 0.
