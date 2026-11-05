---
id: search-and-filter
title: Tìm kiếm và lọc công thức món chay
category: recipes
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-01
  - FR-08
  - FR-16
  - BR-13
source_use_cases:
  - UC-01.2
  - UC-08.1
  - UC-08.2
source_files:
  - app/mamxanh-frontend/src/pages/Explore.tsx
  - app/mamxanh-frontend/src/components/RecipeSearch.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/controller/RecipeController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Tìm kiếm và lọc công thức món chay

## Chức năng này dùng để làm gì?

Trang **Khám phá** (`/kham-pha`) của Mâm Xanh cung cấp bộ công cụ tìm kiếm và lọc đa tiêu chí, giúp bạn nhanh chóng tìm thấy những món chay phù hợp với nhu cầu của gia đình, thời gian nấu nướng hoặc chế độ ăn hiện tại.

## Ai có thể sử dụng?

- Mọi người dùng (bao gồm Khách vãng lai, Thành viên và Chuyên gia) đều có thể tìm kiếm và sử dụng toàn bộ các bộ lọc công thức công khai.

## Các cách tìm kiếm và lọc món ăn

### 1. Tìm kiếm theo từ khóa
- Nhập tên món ăn hoặc nguyên liệu chính vào ô tìm kiếm (ví dụ: *Đậu hũ sốt cà*, *Nấm đùi gà xào xả ớt*, *Canh chua chay*...).
- Hệ thống hỗ trợ tìm kiếm không dấu và có dấu, tự động gợi ý các món ăn khớp nhất.

### 2. Lọc theo Thể loại món ăn (`dish_category`)
Mâm Xanh phân loại món ăn theo các danh mục chuẩn hóa:
- **Món nước:** Phở, bún, miến, hủ tiếu chay...
- **Món xào:** Nấm xào, rau củ xào thập cẩm...
- **Món kho:** Đậu hũ kho nấm, củ cải kho tiêu...
- **Món canh:** Canh rong biển, canh chua, canh bí đỏ...
- **Món lẩu:** Lẩu nấm, lẩu thái chay, lẩu tiêu xanh...
- **Món chiên / nướng:** Chả giò chay, nấm chiên giòn, đậu hũ nướng giấy bạc...
- **Món hấp:** Bánh bao chay, nấm hấp sả...
- **Món gỏi / salad:** Gỏi ngó sen, salad rau mầm, gỏi nấm tuyết...
- **Món tráng miệng / chè:** Chè dưỡng nhan, chè đậu xanh, rau câu trái cây...

### 3. Lọc theo Trường phái ăn chay
- **Thuần chay (Vegan):** Hoàn toàn từ thực vật, không có trứng, sữa hay mật ong.
- **Chay có sữa (Lacto Vegetarian):** Có thể chứa sữa tươi, phô mai, bơ thực vật.
- **Chay có trứng (Ovo Vegetarian):** Có thể chứa trứng gà công nghiệp.
- **Chay có trứng và sữa (Lacto-Ovo Vegetarian):** Kết hợp cả trứng và sữa.

### 4. Lọc theo Nguyên liệu chính
- Bạn có thể chọn một hoặc nhiều nguyên liệu chuẩn (như *nấm đùi gà*, *đậu phụ*, *hạt sen*, *rong biển*) để lọc ra những công thức tận dụng tối đa các nguyên liệu đang có sẵn trong gian bếp của bạn.

### 5. Lọc theo Thời gian nấu và Khẩu phần
- **Thời gian chuẩn bị và chế biến:** Dưới 15 phút, 15 - 30 phút, 30 - 60 phút, hoặc trên 60 phút.
- **Khẩu phần:** 1-2 người, 3-4 người hoặc bữa tiệc gia đình từ 5 người trở lên.

## Những điều cần lưu ý

1. **Chỉ hiển thị các bài viết đang công khai:**
   - Kết quả tìm kiếm chỉ bao gồm các bài viết đã được Chuyên gia công khai và không bị ẩn hoặc xóa do vi phạm chính sách.
2. **Nguyên liệu chưa xác minh:**
   - Khi bạn lọc theo chế độ ăn kiêng, hệ thống sẽ kiểm tra nghiêm ngặt danh sách thành phần. Nếu một công thức chứa nguyên liệu chưa rõ thuộc tính, hệ thống sẽ không tự động khẳng định món ăn đó phù hợp với chế độ ăn kiêng nghiêm ngặt để đảm bảo an toàn cho bạn.
