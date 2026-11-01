---
id: expert-recipe-creation
title: Dành cho Chuyên gia: Hướng dẫn đăng và chỉnh sửa công thức
category: recipes
audience:
  - expert
source_requirements:
  - FR-04
  - FR-14
  - FR-15
  - FR-16
  - FR-19
  - FR-25
  - FR-44
  - BR-07
  - BR-10
  - BR-11
  - BR-12
  - BR-14
  - BR-19
  - BR-20
  - BR-73
source_use_cases:
  - UC-04.1
  - UC-04.2
  - UC-04.3
  - UC-14.1
  - UC-14.2
  - UC-16.1
  - UC-19.1
  - UC-25.1
  - UC-44.1
source_files:
  - app/mamxanh-frontend/src/pages/CreateRecipe.tsx
  - app/mamxanh-frontend/src/pages/EditRecipe.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/controller/RecipePostController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Dành cho Chuyên gia: Hướng dẫn đăng và chỉnh sửa công thức

## Chức năng này dùng để làm gì?

Đây là tính năng độc quyền dành riêng cho tài khoản có vai trò **Chuyên gia ẩm thực chay (Expert)** trên Mâm Xanh. Chuyên gia có toàn quyền biên soạn, đăng tải trực tiếp, chỉnh sửa và quản lý các bài viết công thức chay chuẩn mực nhằm chia sẻ kinh nghiệm cho cộng đồng.

## Ai có thể sử dụng?

- **Chỉ tài khoản có vai trò `EXPERT`:** Được mở khóa nút **Đăng công thức** trên thanh điều hướng và truy cập trang `/dang-cong-thuc` hoặc chỉnh sửa bài viết của mình tại `/cong-thuc/:recipeId/chinh-sua`.
- Người dùng thông thường (`Customer`) và Khách (`Guest`) không thể truy cập giao diện này và sẽ bị máy chủ từ chối nếu cố tình gửi yêu cầu đăng bài.

## Quy chuẩn bắt buộc khi biên soạn bài công thức

Để bảo đảm tính chuyên nghiệp, mọi bài công thức trước khi bấm **Công khai** bắt buộc phải thỏa mãn 8 điều kiện sau:

1. **Tiêu đề món ăn (`title`):** Từ 3 đến 120 ký tự (ví dụ: *Nấm mối kho nước dừa xiêm*).
2. **Thể loại món ăn (`dish_category`):** Chọn đúng 1 thể loại chuẩn hóa trong danh mục (Món nước, Món xào, Món lẩu, Món kho, Món canh, Món chiên, Món hấp, Món gỏi / salad, Món cuốn, Món nướng, Món tráng miệng...).
3. **Danh sách nguyên liệu (từ 1 đến 50 dòng):**
   - **Bắt buộc chọn từ Danh mục chuẩn:** Chuyên gia tìm kiếm và chọn nguyên liệu đã có sẵn trong danh mục chuẩn của hệ thống, không tự ý gõ tên tự do. Nếu nguyên liệu đặc thù chưa có, vui lòng liên hệ Ban quản trị để bổ sung vào danh mục chung.
   - **Bắt buộc định lượng số học dương (`quantity > 0`):** Nghiêm cấm các từ ngữ ước lượng như "vừa đủ", "một ít", "tùy thích".
   - **Đơn vị đo lường hợp lệ:** Chọn đơn vị thuộc danh mục chuẩn:
     - Khối lượng: `mg`, `g`, `kg` (quy đổi cố định tự động).
     - Thể tích: `ml`, `L`, `tsp` (thìa cà phê - 5ml), `tbsp` (thìa canh - 15ml), `cup` (chén/cốc - 240ml).
     - Đếm: `quả`, `củ`, `miếng` (chỉ khả dụng nếu nguyên liệu đó đã được Quản trị viên cấu hình tỷ lệ quy đổi sang gam).
   - **Quy tắc chặn xuất bản (Publish Gate):** Nếu nguyên liệu bạn chọn sử dụng đơn vị đếm hoặc thể tích nhưng hệ thống chưa có hệ số quy đổi sang gam, hệ thống sẽ báo lỗi và chặn xuất bản. Bạn chỉ cần đổi đơn vị sang gam (`g`) để xuất bản bài viết thành công.
4. **Khẩu phần (`servings`):** Nhập số phần ăn từ 1 đến 50 người.
5. **Trường phái ăn chay:** Chọn 1 trong 4 loại chuẩn (Thuần chay, Chay có sữa, Chay có trứng, Chay có trứng và sữa).
6. **Thời gian chế biến:**
   - Thời gian chuẩn bị (`prepTime`) và thời gian nấu (`cookTime`): từ 0 đến 1.440 phút.
   - Tổng thời gian phải lớn hơn 0. Riêng các món không cần nấu qua lửa (như salad, gỏi, nước sốt), bạn có thể để `cookTime = 0` nếu `prepTime > 0`.
7. **Hướng dẫn chuẩn bị và chế biến (`instructions`):**
   - Nội dung hướng dẫn dạng văn bản từ 10 đến 5.000 ký tự.
   - **Không ép buộc chia bước:** Mâm Xanh tôn trọng văn phong của Chuyên gia, bạn có thể viết văn bản tự do, chia đoạn theo ý thích mà không bị ép buộc phải tách thành từng bước nhỏ cứng nhắc.
8. **Thư viện ảnh và Ảnh bìa (`RECIPE_MEDIA`):**
   - Tối đa 5 ảnh trên một bài viết (định dạng JPEG, PNG, WebP, dung lượng $\le 5$ MB/ảnh).
   - **Nếu bài có ảnh:** Bắt buộc phải bấm chọn đúng **1 ảnh làm Ảnh bìa (Cover Image)** để hiển thị ngoài trang chủ.
   - **Nếu bài không có ảnh nào (0 ảnh):** Hệ thống sẽ tự động gán ảnh minh họa mặc định theo loại ăn chay của món.
9. **Mô tả ngắn & Video YouTube (Tùy chọn):**
   - Mô tả ngắn giới thiệu món ăn tối đa 2.000 ký tự.
   - Đường dẫn video YouTube hướng dẫn nấu (tối đa 1 link). Hệ thống hỗ trợ phát video nhúng trực tiếp trên bài viết.

## Cơ chế công khai trực tiếp (Không chờ duyệt từng bài)

- Khác với nhiều nền tảng phải chờ kiểm duyệt nội dung hàng ngày, tại Mâm Xanh, **chỉ cần bài viết của Chuyên gia thỏa mãn đầy đủ các quy chuẩn trên, bài viết sẽ được CÔNG KHAI NGAY LẬP TỨC**.
- Ban quản trị đặt trọn niềm tin vào năng lực chuyên môn của Chuyên gia và chỉ áp dụng biện pháp hậu kiểm khi có báo cáo vi phạm từ cộng đồng.

## Chỉnh sửa hoặc Xóa bài viết của chính mình

- Bạn có toàn quyền truy cập trang quản lý bài viết của mình, bấm **Chỉnh sửa** để cập nhật lại gia vị hoặc mẹo nấu.
- Khi xóa bài viết, bài sẽ rút khỏi trạng thái công khai và không còn xuất hiện trong kết quả tìm kiếm hay gợi ý của AI.
- *Lưu ý quan trọng:* Nếu bài viết của bạn đang bị Quản trị viên tạm ẩn do có báo cáo vi phạm chờ xử lý, bạn không thể tự ý công khai lại bài viết qua giao diện cá nhân mà cần phối hợp giải quyết theo kết luận hậu kiểm của Ban quản trị.
