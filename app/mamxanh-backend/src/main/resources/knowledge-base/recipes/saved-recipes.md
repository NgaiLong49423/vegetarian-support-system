---
id: saved-recipes
title: Lưu và quản lý danh sách công thức yêu thích
category: recipes
audience:
  - customer
  - expert
source_requirements:
  - FR-32
  - BR-32
  - BR-33
  - BR-34
  - BR-35
source_use_cases:
  - UC-32.1
  - UC-32.2
  - UC-32.3
source_files:
  - app/mamxanh-frontend/src/components/SaveRecipeButton.tsx
  - app/mamxanh-frontend/src/components/SavedRecipesContext.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/controller/SavedRecipeController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Lưu và quản lý danh sách công thức yêu thích

## Chức năng này dùng để làm gì?

Tính năng **Công thức đã lưu (`Saved Recipe`)** giúp bạn tạo một "sổ tay ẩm thực" riêng trên Mâm Xanh. Khi bắt gặp những món chay hấp dẫn hoặc muốn nấu thử vào dịp cuối tuần, bạn có thể bấm lưu lại để mở ra nấu bất cứ khi nào mà không mất công tìm kiếm lại.

## Ai có thể sử dụng?

- **Thành viên đã đăng nhập (Customer và Expert):** Toàn quyền lưu, xem danh sách và bỏ lưu công thức.
- **Khách vãng lai (Guest):** Khi bấm nút Lưu (biểu tượng trái tim), hệ thống sẽ mở thông báo yêu cầu đăng nhập trước để tạo sổ tay cá nhân.

## Cách sử dụng

### 1. Cách lưu một công thức
Bạn có thể lưu công thức từ hai vị trí:
- **Từ thẻ món ăn (Recipe Card):** Nhấn vào biểu tượng **Trái tim** ở góc trên thẻ món khi đang lướt trang Chủ hoặc trang Khám phá.
- **Từ trang chi tiết công thức:** Nhấn nút **Lưu công thức** (có biểu tượng trái tim) bên cạnh tiêu đề món ăn.
Khi được lưu thành công, biểu tượng trái tim sẽ chuyển sang màu cam/đỏ nổi bật.

### 2. Xem danh sách công thức đã lưu
1. Đăng nhập vào Mâm Xanh, vào mục **Hồ sơ cá nhân** (`/ho-so`).
2. Chọn tab **Công thức đã lưu** (hoặc mở menu nhanh trên thanh điều hướng).
3. Danh sách hiển thị toàn bộ các món bạn đã lưu kèm ảnh bìa, thời gian nấu, khẩu phần và tỷ lệ % hài lòng.
4. Bạn có thể tìm kiếm theo tên món ăn ngay trong danh sách đã lưu này.

### 3. Bỏ lưu công thức
- Bạn chỉ cần bấm lại vào biểu tượng Trái tim đang sáng ở thẻ món hoặc trang chi tiết. Trạng thái lưu sẽ lập tức được hủy bỏ và món ăn được rút khỏi sổ tay của bạn.

## Những điều cần lưu ý

1. **Độc lập hoàn toàn với Lịch ăn tuần (`Meal Plan`):**
   - Lưu một công thức chỉ có nghĩa là bạn đánh dấu để đọc lại sau này, **hoàn toàn không làm ảnh hưởng đến Lịch ăn tuần**.
   - Việc bạn xóa một món ăn khỏi Lịch ăn tuần không làm mất món đó trong danh sách Đã lưu. Ngược lại, nếu bạn bỏ lưu một công thức, món ăn đó vẫn được giữ nguyên trong các bữa ăn bạn đã lên lịch từ trước.
2. **Không gọi AI và không thay đổi sở thích ăn uống:**
   - Hành động bấm lưu chỉ là thao tác dữ liệu cá nhân đơn thuần; nó không tự động gọi AI và không tự động thay đổi các cài đặt kiêng ăn hay dị ứng của bạn.
3. **Mỗi món chỉ lưu tối đa 1 lần:**
   - Mỗi tài khoản chỉ có tối đa một bản ghi lưu cho cùng một bài công thức. Việc bạn vô tình bấm lưu nhiều lần không tạo ra các mục trùng lặp.
4. **Xử lý khi bài viết gốc bị ẩn hoặc xóa:**
   - Nếu Chuyên gia xóa bài công thức hoặc bài viết bị Ban quản trị tạm ẩn do hậu kiểm vi phạm, mục lưu đó trong sổ tay của bạn sẽ hiển thị trạng thái "Công thức hiện không khả dụng" để bạn nắm rõ, chứ hệ thống không tự động xóa mất dấu vết gây bất ngờ cho bạn.
