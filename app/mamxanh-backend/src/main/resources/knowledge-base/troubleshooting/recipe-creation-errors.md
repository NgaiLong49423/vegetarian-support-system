---
id: recipe-creation-errors
title: Khắc phục lỗi khi đăng hoặc chỉnh sửa công thức
category: troubleshooting
audience:
  - expert
source_requirements:
  - FR-04
  - FR-16
  - FR-19
  - BR-12
  - BR-14
  - BR-19
  - BR-20
  - BR-73
source_files:
  - app/mamxanh-frontend/src/pages/CreateRecipe.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/controller/RecipePostController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Khắc phục lỗi khi đăng hoặc chỉnh sửa công thức

## Dành cho: Chuyên gia ẩm thực chay (Expert)

Khi Chuyên gia biên soạn bài công thức tại `/dang-cong-thuc` hoặc chỉnh sửa bài viết tại `/cong-thuc/:recipeId/chinh-sua`, hệ thống sẽ áp dụng quy trình kiểm tra tự động nghiêm ngặt nhằm bảo đảm 100% bài viết công khai đều đạt chuẩn chất lượng.

Nếu bài viết chưa đạt yêu cầu, giao diện sẽ giữ nguyên nội dung bạn đã nhập và hiển thị thông báo lỗi cụ thể. Dưới đây là cách khắc phục nhanh các lỗi thường gặp:

## Bảng tra cứu lỗi và cách xử lý

| Thông báo lỗi | Nguyên nhân | Cách khắc phục |
|---|---|---|
| **"Nguyên liệu này chưa có tỷ lệ quy đổi sang gam"** *(Lỗi phổ biến nhất)* | Bạn chọn đơn vị đếm (quả, củ, miếng) hoặc thể tích (thìa, cốc) cho một nguyên liệu mà hệ thống chưa có hệ số quy đổi khối lượng tương ứng | Đổi đơn vị của dòng nguyên liệu đó sang đơn vị khối lượng trực tiếp như **gam (`g`)** hoặc **kilôgam (`kg`)**. Sau đó bấm xuất bản lại sẽ thành công ngay. |
| **"Vui lòng chọn 1 ảnh làm ảnh bìa"** | Bài viết có tải lên ảnh (từ 1 đến 5 ảnh) nhưng bạn chưa tích chọn ảnh nào làm ảnh đại diện chính | Rê chuột hoặc chạm vào bức ảnh đẹp nhất bạn muốn hiển thị ngoài trang chủ, bấm vào nút **"Chọn làm ảnh bìa"** (ảnh sẽ xuất hiện viền sáng hoặc huy hiệu Bìa). |
| **"Định lượng nguyên liệu phải là số lớn hơn 0"** | Có một dòng nguyên liệu bị để trống số lượng hoặc bạn nhập chữ thay vì nhập số | Điền số lượng cụ thể lớn hơn 0 (ví dụ: `200`, `1.5`, `3`). Tuyệt đối không để trống hoặc gõ chữ "vừa đủ". |
| **"Nguyên liệu không tồn tại trong danh mục chuẩn"** | Bạn nhập tay tên nguyên liệu tự do không có liên kết với từ điển | Bấm vào ô tìm kiếm nguyên liệu, gõ từ khóa và chọn đúng tên nguyên liệu hiển thị trong danh sách gợi ý của hệ thống. |
| **"Hướng dẫn chế biến phải từ 10 đến 5.000 ký tự"** | Ô hướng dẫn nấu quá ngắn (dưới 10 ký tự) hoặc quá dài | Bổ sung chi tiết các bước sơ chế và nấu nướng tối thiểu 10 ký tự, hoặc rút gọn văn bản nếu vượt quá 5.000 ký tự. |
| **"Tổng thời gian nấu và chuẩn bị phải lớn hơn 0"** | Cả hai ô Thời gian chuẩn bị (`prepTime`) và Thời gian nấu (`cookTime`) đều đang để số 0 | Nếu món không cần nấu lửa (như salad), hãy nhập thời gian chuẩn bị rau củ (ví dụ: `15 phút`). Tổng hai số bắt buộc phải lớn hơn 0. |
| **"Ảnh tải lên vượt quá dung lượng cho phép (5 MB)"** | Tệp ảnh có kích thước tệp quá lớn | Nén ảnh hoặc giảm độ phân giải xuống dưới 5 MB trước khi tải lên (hỗ trợ định dạng JPEG, PNG, WebP). |
| **"Đường dẫn YouTube không hợp lệ"** | Đường dẫn video không đúng định dạng chuẩn của YouTube | Chỉ dán đường dẫn dạng `https://www.youtube.com/watch?v=...` hoặc `https://youtu.be/...`. |

## Vì sao tài khoản thông thường không thấy nút Đăng công thức?

- Nếu bạn đăng nhập vào Mâm Xanh nhưng không nhìn thấy nút "Đăng công thức" trên thanh điều hướng:
  - **Lý do:** Tài khoản của bạn đang có vai trò là Thành viên thông thường (`Customer`).
  - **Cách xử lý:** Mâm Xanh chỉ cấp quyền đăng bài cho Chuyên gia. Bạn vui lòng truy cập trang **Đăng ký Chuyên gia** (`/dang-ky-chuyen-gia`) để nộp hồ sơ xét duyệt nâng cấp tài khoản.
