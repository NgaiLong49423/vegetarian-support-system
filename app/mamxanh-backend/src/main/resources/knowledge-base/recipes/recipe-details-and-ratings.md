---
id: recipe-details-and-ratings
title: Xem chi tiết công thức và đánh giá Like / Dislike
category: recipes
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-16
  - FR-17
  - FR-20
  - FR-57
  - FR-58
  - BR-19
  - BR-69
  - BR-70
source_use_cases:
  - UC-01.3
  - UC-20.1
  - UC-57.1
  - UC-57.2
  - UC-57.3
  - UC-57.4
  - UC-58.3
source_files:
  - app/mamxanh-frontend/src/pages/RecipeDetail.tsx
  - app/mamxanh-frontend/src/components/RecipeReactions.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/controller/RecipeReactionController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Xem chi tiết công thức và đánh giá Like / Dislike

## Chức năng này dùng để làm gì?

Trang chi tiết công thức (`/cong-thuc/:slug`) là nơi trình bày đầy đủ dữ liệu món ăn được chuẩn hóa của Chuyên gia: từ nguyên liệu định lượng số học, hướng dẫn chuẩn bị và chế biến, video minh họa YouTube, cho đến bảng phân tích dinh dưỡng trên một khẩu phần.

Đồng thời, đây cũng là nơi cộng đồng đánh giá chất lượng món ăn thông qua cơ chế bình chọn **Like / Dislike**.

## Cấu trúc nội dung của một bài công thức

1. **Thông tin tác giả:** Hiển thị tên Chuyên gia, ảnh đại diện, huy hiệu xác nhận và nút Theo dõi (`Follow`).
2. **Hình ảnh & Media:**
   - Bộ sưu tập từ 1 đến 5 ảnh chụp thực tế quá trình nấu hoặc thành phẩm, với ảnh bìa nổi bật.
   - Trình phát video YouTube nhúng trực tiếp (nếu tác giả có cung cấp video hướng dẫn).
3. **Chỉ số tổng quan:**
   - Thời gian chuẩn bị (`prepTime`) và thời gian nấu (`cookTime`).
   - Khẩu phần chuẩn (ví dụ: 2 người, 4 người).
   - Thể loại món ăn chuẩn hóa và Trường phái ăn chay.
4. **Bảng định lượng nguyên liệu:**
   - Danh sách 100% nguyên liệu có số lượng số học và đơn vị chuẩn có thể quy đổi (gam, ml, quả...).
   - Nút liên kết nhanh để thêm nguyên liệu này vào **Danh sách đi chợ** hoặc xem công thức tương tự.
5. **Hướng dẫn chế biến (`instructions`):**
   - Văn bản hướng dẫn mạch lạc, trực quan từ 10 đến 5.000 ký tự do chính tác giả biên soạn, chia sẻ chi tiết cách sơ chế, các bước nấu và mẹo nêm nếm.
6. **Bảng dinh dưỡng ước tính trên một khẩu phần:**
   - 9 chỉ tiêu: Năng lượng (kcal), Chất đạm (Protein), Chất bột đường (Carb), Chất béo, Chất xơ, Canxi, Sắt, Vitamin B12 và Kẽm.

## Cơ chế bình chọn Like / Dislike

Thay vì thang điểm 1-5 sao chung chung, Mâm Xanh áp dụng hệ thống bình chọn **Thích (Like) / Không thích (Dislike)** tương tự Samsung Food nhằm phản ánh tỷ lệ hài lòng thực tế của người nấu.

### 1. Quyền bình chọn
- **Thành viên đã đăng nhập (Customer hoặc Expert):** Có quyền gửi 1 phản hồi Like hoặc Dislike trên mỗi bài công thức.
- **Khách vãng lai (Guest):** Chỉ có thể xem tỷ lệ phần trăm và tổng số lượt bình chọn; khi bấm Like/Dislike, hệ thống sẽ mở thông báo yêu cầu bạn đăng nhập.
- **Tác giả bài viết:** **Tuyệt đối không được tự Like hoặc Dislike bài viết của chính mình** để giữ tính khách quan.

### 2. Thao tác bình chọn và đổi ý
- **Bấm Like:** Ghi nhận 1 lượt Thích cho bài viết.
- **Bấm Dislike:** Ghi nhận 1 lượt Không thích.
- **Hủy bình chọn:** Nếu bạn đang Like mà bấm lại nút Like lần nữa, hệ thống sẽ rút lại lượt bình chọn của bạn.
- **Đổi ý (Switch):** Nếu bạn đang Like mà bấm sang Dislike (hoặc ngược lại), hệ thống sẽ tự động chuyển trạng thái phản hồi và cập nhật lại tỷ lệ phần trăm mà không tạo bản ghi trùng.

### 3. Hiển thị huy hiệu tỷ lệ hài lòng
- Thẻ bài viết tại trang chủ và khám phá hiển thị huy hiệu ở góc trên ảnh: ví dụ `👍 98%` hoặc `👍 92%`.
- Trang chi tiết hiển thị thanh tỷ lệ phần trăm hài lòng kèm số lượt Like và Dislike cụ thể.
- Nếu bài viết mới đăng và chưa có ai bình chọn, hệ thống hiển thị nhãn `Mới` thay vì `0%`.

## Các nút thao tác nhanh trên trang công thức

- **Lưu công thức (Biểu tượng trái tim):** Đánh dấu bài viết vào danh sách Công thức đã lưu để mở lại khi cần.
- **Thêm vào lịch ăn:** Chọn ngày và bữa (Sáng/Trưa/Tối) để đưa món ăn vào Thực đơn tuần của bạn.
- **Thêm vào đi chợ:** Chuyển toàn bộ nguyên liệu của món ăn sang Danh sách mua sắm với số khẩu phần bạn mong muốn.
- **So sánh món ăn:** Chọn món ăn này để so sánh dinh dưỡng đối đầu với một món chay khác.
- **Xuất file:** Tải công thức về máy dưới dạng tệp văn bản `.txt` hoặc tệp `.pdf`.
- **Báo cáo vi phạm:** Gửi phản ánh đến Ban quản trị nếu phát hiện công thức có nội dung sai lệch hoặc nguy cơ mất an toàn thực phẩm.
