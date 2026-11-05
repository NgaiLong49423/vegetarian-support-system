---
id: sorting-modes
title: 6 chế độ sắp xếp công thức trên Mâm Xanh
category: recipes
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-08
  - FR-57
  - FR-58
  - BR-69
  - BR-70
  - BR-71
  - BR-72
source_use_cases:
  - UC-08.3
source_files:
  - app/mamxanh-frontend/src/pages/Explore.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/repository/RecipeQueryRepository.java
implementation_status: verified
last_verified: 2026-11-01
---

# 6 chế độ sắp xếp công thức trên Mâm Xanh

## Chức năng này dùng để làm gì?

Tại trang **Khám phá** (`/kham-pha`), Mâm Xanh thiết kế 6 chế độ sắp xếp độc lập và minh bạch. Điều này giúp bạn dễ dàng tìm thấy công thức mới nhất, các món ăn được cộng đồng đánh giá cao nhất, hoặc những món chay đang tạo nên xu hướng thảo luận sôi nổi.

## Danh sách 6 chế độ sắp xếp

### 1. Mới nhất (Newest)
- **Tiêu chí:** Sắp xếp theo thời gian bài viết được công khai giảm dần (bài vừa đăng sẽ hiển thị trên cùng).
- **Phù hợp khi:** Bạn muốn cập nhật những ý tưởng sáng tạo và thực đơn mới nhất mỗi ngày từ các Chuyên gia ẩm thực chay.

### 2. Được yêu thích nhất (Most Liked)
- **Tiêu chí:** Sắp xếp theo **Tỷ lệ % Like** giảm dần.
- **Cách tính:** Tỷ lệ % Like được tính bằng công thức:
  $$\text{Tỷ lệ \% Like} = \frac{\text{Số lượt Like}}{\text{Số lượt Like} + \text{Số lượt Dislike}} \times 100\%$$
- **Điều kiện phụ:** Nếu hai bài có cùng tỷ lệ phần trăm hài lòng, bài viết có tổng số lượt Like lớn hơn sẽ được ưu tiên xếp trên.
- **Phù hợp khi:** Bạn muốn chọn những món ăn có chất lượng được số đông người nấu thực tế kiểm chứng và đánh giá cao.

### 3. Xem nhiều nhất (Most Viewed)
- **Tiêu chí:** Sắp xếp theo tổng số lượt xem hợp lệ đã qua cơ chế khử trùng lặp (View Deduplication).
- **Hỗ trợ khung thời gian:** Bạn có thể chọn xem nhiều nhất trong 24 giờ qua, 7 ngày qua, 30 ngày qua, hoặc toàn thời gian.
- **Phù hợp khi:** Bạn muốn biết những món ăn nào đang được nhiều người tìm đọc và quan tâm nhất hiện nay.

### 4. Nhiều bình luận nhất (Most Commented)
- **Tiêu chí:** Sắp xếp theo tổng số lượt bình luận và phản hồi trên bài viết.
- **Phù hợp khi:** Bạn muốn tham gia vào những chủ đề ẩm thực sôi nổi, nơi có nhiều câu hỏi, chia sẻ kinh nghiệm nấu và mẹo vặt gia giảm gia vị từ cộng đồng.

### 5. Hoạt động sôi nổi nhất (Most Active)
- **Tiêu chí:** Đo lường tổng khối lượng tương tác thực tế của cộng đồng trong **7 ngày gần nhất**.
- **Điểm hoạt động:** Kết hợp giữa số lượt xem gần đây, số bình luận mới và số lượt Like/Dislike mới trong 7 ngày qua:
  $$\text{Điểm hoạt động} = (\text{Lượt xem 7 ngày} \times 1) + (\text{Bình luận 7 ngày} \times 5) + (\text{Đánh giá 7 ngày} \times 10)$$
- **Điểm đặc biệt:** Chế độ này không quan tâm bài viết được đăng từ lâu hay mới đăng. Một công thức truyền thống dù đã đăng 1 năm trước nhưng tuần này có đợt nấu nhiều và bình luận sôi nổi vẫn sẽ vươn lên dẫn đầu.

### 6. Thịnh hành (Trending)
- **Tiêu chí:** Thuật toán thịnh hành kết hợp giữa **mức độ tương tác gia tăng trong 3 ngày gần đây** và **hệ số độ mới của bài viết (Freshness Decay)**.
- **Cách hoạt động:** Bài viết mới xuất bản (trong vòng 48 giờ) sẽ nhận được điểm thưởng khuyến khích; đồng thời theo thời gian, điểm số sẽ giảm dần để nhường chỗ cho những bài viết mới nổi.
- **Phù hợp khi:** Bạn muốn bắt kịp trào lưu những món chay "hot trend" đang được cộng đồng đón nhận nồng nhiệt nhất.

## Những điều cần lưu ý

1. **Khử trùng lặp lượt xem (Chống spam F5):**
   - Mâm Xanh áp dụng cửa sổ khử trùng lặp 30 phút. Nếu một người liên tục tải lại trang (F5) trong 30 phút, hệ thống chỉ tính là 1 lượt xem hợp lệ duy nhất, đảm bảo tính công bằng cho bảng xếp hạng.
2. **Bài viết mới đăng chưa có đánh giá:**
   - Những bài viết chưa có ai bình chọn Like hoặc Dislike sẽ hiển thị nhãn **"Mới"** trên thẻ ảnh thay vì hiển thị `0%` để không làm giảm giá trị của món ăn mới xuất bản.
