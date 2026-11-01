---
id: shopping-list-overview
title: Tạo và quản lý danh sách đi chợ
category: shopping-list
audience:
  - customer
  - expert
source_requirements:
  - FR-53
  - BR-14
source_use_cases:
  - UC-53.1
  - UC-53.2
source_files:
  - app/mamxanh-frontend/src/pages/ShoppingList.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/shopping/controller/ShoppingListController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Tạo và quản lý danh sách đi chợ

## Chức năng này dùng để làm gì?

Trang **Danh sách đi chợ** (`/di-cho`) là người bạn đồng hành đắc lực mỗi khi bạn chuẩn bị ra chợ hoặc siêu thị mua nguyên liệu nấu ăn chay. Hệ thống giúp bạn tổ chức các mặt hàng cần mua thành các danh sách checklist tương tác, đánh dấu những món đã mua và chia nhóm nguyên liệu rõ ràng.

## Ai có thể sử dụng?

- **Thành viên đã đăng nhập (Customer và Expert):** Sở hữu các danh sách mua sắm riêng trên tài khoản của mình.
- **Khách vãng lai (Guest):** Cần đăng nhập để lưu trữ và quản lý danh sách đi chợ.

## 3 cách tạo danh sách đi chợ trên Mâm Xanh

### 1. Tạo tự động từ Thực đơn tuần (`Meal Plan`)
- **Cách tiện lợi nhất:** Sau khi bạn đã lên lịch ăn tuần, bấm nút **"Tạo danh sách đi chợ từ lịch ăn"** trên trang Kế hoạch bữa ăn (`/ke-hoach`).
- Bạn có thể chọn:
  - Đi chợ cho **toàn bộ cả tuần** (tất cả các ngày từ Thứ Hai đến Chủ Nhật).
  - Hoặc chọn đi chợ cho **một số ngày cụ thể** (ví dụ chỉ đi chợ cho Thứ Hai đến Thứ Tư).
- Hệ thống sẽ tự động quét mọi món ăn trong các ngày đã chọn, tính toán theo số khẩu phần bạn dự định ăn và tự động gom các nguyên liệu giống nhau lại thành một danh sách duy nhất.

### 2. Tạo từ một bài công thức cụ thể
- Khi đang xem một món ăn ngon tại `/cong-thuc/:slug`, bấm nút **"Thêm vào đi chợ"**.
- Bạn có thể chọn tạo một danh sách mua sắm mới tinh cho món này, hoặc chọn thêm nguyên liệu của món này vào một danh sách đi chợ bạn đã có từ trước.
- Bạn có thể điều chỉnh số khẩu phần muốn mua (ví dụ công thức gốc cho 2 người, nhưng bạn muốn mua đủ cho 6 người ăn).

### 3. Tạo danh sách trống thủ công
- Tại trang `/di-cho`, bấm nút **"Tạo danh sách mới"**.
- Đặt tên cho danh sách (ví dụ: *Đi chợ cuối tuần*, *Mua gia vị làm tiệc chay*...).
- Sau đó, bạn có thể tự do bấm **+ Thêm mặt hàng** để thêm các món đồ tùy thích.

## Quản lý nhiều danh sách đi chợ

- Bạn có thể sở hữu nhiều danh sách đi chợ cùng lúc cho các dịp khác nhau.
- **Đổi tên danh sách:** Bấm biểu tượng cây bút để đổi tên danh sách (tối đa 200 ký tự).
- **Xóa danh sách:** Bấm biểu tượng thùng rác để xóa bỏ danh sách khi đã mua sắm xong. Việc xóa danh sách đi chợ hoàn toàn **không ảnh hưởng gì đến Thực đơn tuần hay bài công thức gốc**.

## Những điều cần lưu ý

1. **Số lượng nguyên liệu luôn là số học:**
   - Mọi mặt hàng được tạo từ công thức hoặc lịch ăn đều có số lượng cụ thể và đơn vị rõ ràng (ví dụ: `500 g nấm rơm`, `2 bìa đậu hũ`), giúp bạn không bao giờ phải băn khoăn mua bao nhiêu là đủ.
2. **Quyền riêng tư tuyệt đối:**
   - Danh sách đi chợ là dữ liệu riêng tư của cá nhân bạn, không hiển thị công khai cho bất kỳ ai khác trên hệ thống.
