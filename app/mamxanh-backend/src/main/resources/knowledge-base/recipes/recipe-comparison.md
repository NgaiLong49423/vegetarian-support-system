---
id: recipe-comparison
title: So sánh hai công thức món chay
category: recipes
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-60
  - BR-76
source_use_cases:
  - UC-60.1
  - UC-60.2
  - UC-60.3
  - UC-60.4
  - UC-60.5
source_files:
  - app/mamxanh-frontend/src/pages/RecipeCompare.tsx
  - app/mamxanh-frontend/src/pages/RecipeComparePicker.tsx
  - app/mamxanh-frontend/src/lib/recipeComparison.ts
implementation_status: verified
last_verified: 2026-11-01
---

# So sánh hai công thức món chay

## Chức năng này dùng để làm gì?

Tính năng **So sánh công thức** (`/so-sanh`) cho phép bạn đặt hai bài công thức chay công khai cạnh nhau để đối chiếu trực tiếp về thông tin tổng quan, thời gian nấu, danh sách nguyên liệu và các chỉ tiêu dinh dưỡng.

Tính năng này đặc biệt hữu ích khi bạn phân vân giữa hai món ăn cho bữa tối, hoặc muốn chọn món ăn có hàm lượng đạm cao hơn hay ít calorie hơn.

## Ai có thể sử dụng?

- Cả Khách vãng lai (Guest), Thành viên thông thường (Customer) và Chuyên gia đều có thể sử dụng tự do tính năng so sánh với các bài công thức đang công khai.

## Cách sử dụng

### 1. Bắt đầu so sánh từ thanh điều hướng
1. Truy cập vào trang **So sánh** (`/so-sanh`).
2. Màn hình sẽ hiển thị 2 vị trí trống để bạn chọn món.
3. Bấm **Chọn món thứ nhất** để mở bảng tìm kiếm công thức (`/so-sanh/chon-mon`), gõ tên món hoặc chọn từ danh sách.
4. Bấm **Chọn món thứ hai** để chọn công thức đối chiếu còn lại.
5. Khi đã đủ hai món, bảng so sánh đối đầu chi tiết sẽ xuất hiện ngay lập tức.

### 2. Bắt đầu so sánh từ trang chi tiết bài viết
- Khi đang xem một công thức tại `/cong-thuc/:slug`, bạn có thể bấm nút **So sánh món này**. Hệ thống sẽ tự động gán món ăn này vào vị trí thứ nhất và mở danh sách để bạn chọn món thứ hai.

### 3. Thay đổi món trong bảng so sánh
- Bạn có thể bấm vào nút **Đổi món** ở đầu mỗi cột bất kỳ lúc nào để thay thế bằng một công thức khác.

## Nội dung được đối chiếu trong bảng so sánh

Bảng so sánh hiển thị trực quan các mục song song:
1. **Tổng quan món ăn:**
   - Ảnh bìa, tên món ăn, tên tác giả Chuyên gia.
   - Thể loại món ăn và Trường phái ăn chay.
   - Tỷ lệ % Like và tổng số lượt bình chọn.
   - Thời gian chuẩn bị và thời gian nấu.
   - Khẩu phần gốc do tác giả quy định.
2. **Đối chiếu dinh dưỡng trên một khẩu phần:**
   - Dinh dưỡng được tính toán theo **đúng 1 khẩu phần** của từng món ăn để đảm bảo sự tương đương.
   - So sánh 9 chỉ tiêu: Năng lượng (kcal), Chất đạm (g), Chất béo (g), Chất bột đường (g), Chất xơ (g), Canxi (mg), Sắt (mg), Vitamin B12 (mcg) và Kẽm (mg).
   - Hiển thị mức chênh lệch định lượng tuyệt đối giữa hai món ăn (ví dụ: Món A nhiều hơn Món B 4.5g đạm).
3. **Danh sách nguyên liệu:**
   - Hiển thị nguyên liệu kèm định lượng của từng món ăn để bạn dễ dàng kiểm tra xem nhà mình có sẵn nguyên liệu của món nào hơn.

## Những điều cần lưu ý

1. **Chỉ so sánh đúng hai công thức:**
   - Mỗi phiên so sánh chỉ áp dụng cho đúng hai bài công thức khác nhau. Cả hai bài viết bắt buộc phải đang ở trạng thái công khai (`PUBLISHED`). Bài bị ẩn hoặc xóa không thể đưa vào so sánh.
2. **Khẩu phần gốc có thể khác nhau:**
   - Hệ thống luôn hiển thị rõ khẩu phần gốc của từng bài (ví dụ Món A nấu cho 2 người, Món B nấu cho 4 người). Dù các con số dinh dưỡng được quy về 1 khẩu phần, kích thước một khẩu phần thành phẩm giữa các món có thể khác nhau.
3. **Không tự kết luận món nào "lành mạnh" hay "tốt hơn":**
   - Mâm Xanh chỉ cung cấp số liệu so sánh khách quan và định lượng, không tự đưa ra điểm số sức khỏe (Health Score) hay kết luận món nào tốt hơn món nào, vì nhu cầu dinh dưỡng của mỗi cá nhân là khác nhau.
4. **Xử lý khi thiếu dữ liệu dinh dưỡng:**
   - Nếu một trong hai công thức có nguyên liệu chưa có dữ liệu dinh dưỡng trong hệ thống, chỉ tiêu đó sẽ hiển thị nhãn **"Chưa đủ dữ liệu"**. Hệ thống không tự ý gán giá trị bằng 0 hay suy đoán số liệu.
5. **Không lưu lịch sử so sánh:**
   - Phiên so sánh diễn ra tức thì trên trình duyệt của bạn; hệ thống không lưu lại lịch sử các lần so sánh vào tài khoản cá nhân.
