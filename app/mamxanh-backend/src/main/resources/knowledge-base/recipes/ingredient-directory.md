---
id: ingredient-directory
title: Tra cứu danh mục nguyên liệu và thông tin dinh dưỡng
category: recipes
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-18
  - FR-61
  - FR-62
source_files:
  - app/mamxanh-frontend/src/pages/IngredientDirectoryPage.tsx
  - app/mamxanh-frontend/src/pages/IngredientDetailPage.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/nutrition/controller/PublicIngredientBrowseController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Tra cứu danh mục nguyên liệu và thông tin dinh dưỡng

## Chức năng này dùng để làm gì?

Trang **Danh mục nguyên liệu** (`/danh-muc-nguyen-lieu`) là từ điển thành phần ẩm thực chay chính thức của Mâm Xanh. Tại đây, bạn có thể tra cứu nguồn gốc, đặc tính dinh dưỡng trên 100g của từng loại rau củ, nấm, đậu hạt và khám phá tất cả các công thức đang sử dụng nguyên liệu đó.

## Ai có thể sử dụng?

- Mọi người dùng (Khách vãng lai, Thành viên và Chuyên gia) đều có quyền duyệt, tìm kiếm và xem chi tiết dinh dưỡng của các nguyên liệu đang hoạt động trên hệ thống.

## Cách sử dụng

### 1. Duyệt và tìm kiếm nguyên liệu
1. Truy cập vào trang `/danh-muc-nguyen-lieu`.
2. Bạn có thể duyệt theo từng nhóm nguyên liệu:
   - **Rau củ & Quả:** Cà chua, cà tím, nấm hương, nấm đùi gà, ngô ngọt, cà rốt...
   - **Các loại Đậu & Hạt:** Đậu nành, đậu xanh, hạt sen, hạnh nhân, hạt chia...
   - **Chế phẩm thực vật:** Đậu phụ, váng đậu (tàu hũ ky), sữa đậu nành...
   - **Gia vị & Khác:** Tương bần, dầu mè, nước tương, rong biển...
3. Gõ tên nguyên liệu vào ô tìm kiếm để lọc nhanh.

### 2. Xem chi tiết thông tin và dinh dưỡng nguyên liệu
- Nhấp vào một thẻ nguyên liệu để mở trang chi tiết (`/nguyen-lieu/:ingredientId`).
- Trang chi tiết cung cấp:
  - Tên tiếng Việt và hình ảnh nhận diện thực phẩm.
  - Bảng thành phần dinh dưỡng tham khảo trên **100g nguyên liệu**: Năng lượng (kcal), Chất đạm, Chất béo, Carbohydrate, Chất xơ, Canxi, Sắt, Vitamin B12 và Kẽm.
  - Nguồn tham khảo dữ liệu dinh dưỡng (ví dụ: Viện Dinh dưỡng, USDA...).
  - Các đơn vị đo lường tương thích và hệ số quy đổi khả dụng.

### 3. Tìm các công thức sử dụng nguyên liệu đã chọn
- Ngay trên trang chi tiết nguyên liệu, bạn sẽ thấy danh sách **Các món ngon sử dụng nguyên liệu này**.
- Bạn cũng có thể chọn tích nhiều nguyên liệu cùng lúc ở danh mục rồi bấm **"Tìm món nấu từ nguyên liệu đã chọn"** để hệ thống chuyển sang trang Khám phá với các bộ lọc tương ứng.

## Những điều cần lưu ý

1. **Từ điển dùng chung chuẩn hóa:**
   - Danh mục nguyên liệu do Ban quản trị Mâm Xanh quản lý tập trung. Đây là nguồn dữ liệu gốc duy nhất để các Chuyên gia lựa chọn khi đăng bài công thức và để hệ thống tính toán dinh dưỡng, đi chợ chính xác.
2. **Nguyên liệu chưa có số liệu dinh dưỡng:**
   - Một số loại rau thơm hoặc nguyên liệu bản địa đặc thù có thể chưa được cập nhật đầy đủ 9 chỉ tiêu dinh dưỡng. Khi đó, hệ thống sẽ hiển thị nhãn "Chưa đủ dữ liệu dinh dưỡng". Món ăn sử dụng nguyên liệu này vẫn được công khai bình thường nhưng sẽ tạm thời không tham gia vào tính năng tự động tối ưu thực đơn theo chỉ tiêu năng lượng của AI.
