---
id: ai-meal-recommendations
title: AI gợi ý món ăn và tạo thực đơn tuần tự động
category: ai-features
audience:
  - customer
  - expert
source_requirements:
  - FR-34
  - FR-36
  - BR-31
  - BR-38
  - BR-40
source_use_cases:
  - UC-34.1
  - UC-34.2
  - UC-34.3
  - UC-36.1
  - UC-36.2
  - UC-36.3
source_files:
  - app/mamxanh-frontend/src/pages/AiRecipePlanner.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/controller/AiMenuController.java
implementation_status: verified
last_verified: 2026-11-01
---

# AI gợi ý món ăn và tạo thực đơn tuần tự động

## Chức năng này dùng để làm gì?

Việc lên thực đơn cho 21 bữa ăn trong một tuần (7 ngày $\times$ 3 bữa/ngày) vừa ngon miệng, vừa đổi món liên tục, lại vừa đảm bảo cân bằng dinh dưỡng là một thử thách tốn nhiều thời gian.

Tính năng **AI Lập thực đơn** (`/ke-hoach?view=for-you`) sử dụng sức mạnh tính toán của mô hình Gemini kết hợp với kho dữ liệu công thức chuẩn mực của Mâm Xanh để tự động tạo ra một bản xem trước (**Preview**) thực đơn 7 ngày hoàn chỉnh dành riêng cho bạn chỉ sau vài giây.

## Ai có thể sử dụng?

- **Thành viên đã đăng nhập (Customer và Expert):** Cần hoàn thành thông tin sở thích ăn uống tối thiểu (Trường phái ăn chay, nguyên liệu kiêng, món không thích) trước khi sử dụng.
- **Nếu sử dụng tính năng Thực đơn theo dinh dưỡng (FR-36):** Bắt buộc phải xác nhận đủ điều kiện tham gia (từ 18 tuổi, không mang thai, không điều trị bệnh) và đã khai báo Hồ sơ dinh dưỡng.

## Hai chế độ gợi ý thực đơn thông minh

### 1. Gợi ý thực đơn theo Sở thích & Nguyên liệu sẵn có
- Bạn nhập danh sách các nguyên liệu bạn đang có sẵn trong nhà (ví dụ: *nấm hương, đậu hũ, cà rốt, bí đỏ*...).
- Bạn chọn số ngày muốn lên thực đơn (1 ngày, 3 ngày hoặc cả tuần 7 ngày).
- AI sẽ ưu tiên tối đa các món sử dụng nguyên liệu bạn đang có để tránh lãng phí thực phẩm, đồng thời tự động loại trừ mọi thành phần bạn đã khai báo dị ứng hoặc không thích.

### 2. Gợi ý thực đơn tối ưu theo Chỉ tiêu Dinh dưỡng
- AI đọc mục tiêu calorie và 9 chỉ tiêu dinh dưỡng trong Hồ sơ dinh dưỡng cá nhân của bạn.
- Thuật toán phân bổ năng lượng chuẩn khoa học cho 3 bữa ăn chính trong ngày:
  - **Bữa sáng:** Hướng tới 25% tổng năng lượng ngày (khoảng 20% - 30%).
  - **Bữa trưa:** Hướng tới 37.5% tổng năng lượng ngày (khoảng 32.5% - 42.5%).
  - **Bữa tối:** Hướng tới 37.5% tổng năng lượng ngày (khoảng 32.5% - 42.5%).
- AI tự động tuyển chọn những bài công thức công khai có dữ liệu dinh dưỡng tin cậy nhất để phối hợp các món ăn, tối ưu lượng đạm thực vật, chất xơ và các vi chất thiết yếu.

## Quy trình: Xem trước $\rightarrow$ Tinh chỉnh $\rightarrow$ Xác nhận áp dụng

Để bảo đảm quyền tự quyết của người dùng, AI không bao giờ tự ý ghi đè vào lịch ăn cá nhân của bạn:

1. **Bước 1: Tạo bản xem trước (Preview):**
   - AI tạo ra bảng thực đơn dự kiến kèm lời giải thích lý do phối hợp món ăn (ví dụ: *"Bữa trưa Thứ Ba kết hợp Canh rong biển đậu hũ giúp bổ sung đạm và sắt, đi kèm Mướp xào giá đỗ thanh nhiệt..."*).
2. **Bước 2: Tự do điều chỉnh và đổi món:**
   - Bạn xem từng bữa ăn. Nếu có món nào không ưng ý, bạn có thể bấm nút **Đổi món khác** để AI đề xuất ngay một món chay tương đương thay thế.
3. **Bước 3: Xác nhận áp dụng vào Lịch ăn:**
   - Khi đã hoàn toàn hài lòng với thực đơn xem trước, bạn bấm nút **"Xác nhận áp dụng vào Lịch ăn tuần"**.
   - Lúc này, toàn bộ 21 món ăn mới chính thức được lưu vào Lịch ăn tuần (`Meal Plan`) của bạn, sẵn sàng để xuất ra Danh sách đi chợ.

## Những điều cần lưu ý

1. **Chỉ sử dụng công thức có thật:**
   - AI chỉ chọn lựa từ các bài viết đang công khai do Chuyên gia Mâm Xanh đăng tải. Mọi món ăn trong thực đơn đều có liên kết dẫn tới bài viết chi tiết để bạn xem cách nấu.
2. **Khi không đủ công thức đáp ứng hoàn hảo:**
   - Nếu số lượng công thức trong kho chưa đủ để đáp ứng trọn vẹn 100% tỷ lệ calo lý tưởng của bạn, AI sẽ trung thực nêu rõ độ chênh lệch thay vì khẳng định sai sự thật rằng thực đơn đã cân bằng hoàn hảo.
