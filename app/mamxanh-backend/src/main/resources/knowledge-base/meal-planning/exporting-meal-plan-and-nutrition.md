---
id: exporting-meal-plan-and-nutrition
title: Xuất công thức và báo cáo dinh dưỡng ra file TXT / PDF
category: meal-planning
audience:
  - customer
  - expert
source_requirements:
  - FR-20
  - FR-37
source_use_cases:
  - UC-20.4
  - UC-37.6
source_files:
  - app/mamxanh-frontend/src/pages/MealPlanner.tsx
  - app/mamxanh-frontend/src/pages/NutritionTracker.tsx
  - app/mamxanh-frontend/src/pages/RecipeDetail.tsx
implementation_status: verified
last_verified: 2026-11-01
---

# Xuất công thức và báo cáo dinh dưỡng ra file TXT / PDF

## Chức năng này dùng để làm gì?

Mâm Xanh hỗ trợ bạn trích xuất các dữ liệu nấu ăn và kế hoạch dinh dưỡng của mình ra các tệp tin tiện lợi (`.txt` hoặc `.pdf`). Tính năng này giúp bạn:
- In ra giấy để dán lên tủ lạnh hoặc mang vào bếp khi nấu nướng mà không lo bẩn điện thoại.
- Lưu trữ offline trên máy tính hoặc điện thoại khi không có kết nối mạng.
- Gửi kế hoạch ăn uống cho người thân hoặc đầu bếp gia đình.

## Các loại tệp có thể xuất

### 1. Xuất chi tiết bài công thức (.txt hoặc .pdf)
- **Vị trí thực hiện:** Trên trang chi tiết món ăn (`/cong-thuc/:slug`).
- **Thao tác:** Nhấn nút **Xuất file** ở thanh công cụ bên cạnh món ăn và chọn định dạng:
  - **Tệp văn bản thuần (`.txt`):** Nhẹ, dễ mở bằng bất kỳ ứng dụng ghi chú nào. Chứa tiêu đề, tác giả, khẩu phần, thời gian, toàn bộ danh sách nguyên liệu định lượng và hướng dẫn chế biến.
  - **Tệp tài liệu in ấn (`.pdf`):** Trình bày đẹp mắt, có bố cục trang chuẩn, kèm hình ảnh món ăn, bảng nguyên liệu và các chỉ số dinh dưỡng tham khảo để in ra giấy.

### 2. Xuất báo cáo dinh dưỡng ngày hoặc tuần (.pdf)
- **Vị trí thực hiện:** Trên trang **Theo dõi dinh dưỡng** (`/dinh-duong`).
- **Thao tác:** Chọn ngày hoặc tuần bạn muốn tổng hợp, sau đó nhấn **Xuất báo cáo PDF**.
- **Nội dung trong báo cáo:**
  - Danh sách thực đơn dự kiến theo từng bữa (Sáng, Trưa, Tối) và số khẩu phần ăn.
  - Bảng tổng hợp đối chiếu 9 chỉ tiêu dinh dưỡng thực tế đạt được so với mức khuyến nghị tham khảo cá nhân.
  - Biểu đồ phân bổ năng lượng trực quan giữa 3 bữa ăn.
  - Phần cảnh báo minh bạch nếu có món ăn thiếu số liệu dinh dưỡng.
  - Lời nhắc miễn trừ y tế về tính chất tham khảo của báo cáo.

### 3. Xuất danh sách đi chợ (.txt)
- **Vị trí thực hiện:** Trên trang **Danh sách đi chợ** (`/di-cho`).
- **Thao tác:** Bấm nút **Xuất tệp text** hoặc **Sao chép vào Clipboard** để lấy danh sách các món cần mua và gửi qua Zalo, Messenger hoặc ứng dụng ghi chú.

## Những điều cần lưu ý

1. **Khẩu phần được tính toán chính xác theo thực đơn:**
   - Khi bạn xuất báo cáo dinh dưỡng, hệ thống sử dụng chính xác số khẩu phần bạn đã khai báo trên Lịch ăn để nhân số liệu, đảm bảo con số trên báo cáo phản ánh đúng lượng thức ăn dự kiến của bạn.
2. **Quyền riêng tư:**
   - Các tệp PDF báo cáo dinh dưỡng chứa thông tin sức khỏe cá nhân của bạn. Tệp được tạo trực tiếp trên trình duyệt hoặc máy chủ an toàn và tải thẳng về thiết bị của bạn, không được công khai cho người khác.
