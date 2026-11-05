---
id: shopping-list-actions
title: Thao tác khi đi chợ và xuất danh sách mua sắm
category: shopping-list
audience:
  - customer
  - expert
source_requirements:
  - FR-53
  - FR-55
source_use_cases:
  - UC-53.3
  - UC-55.1
  - UC-55.2
source_files:
  - app/mamxanh-frontend/src/pages/ShoppingList.tsx
  - app/mamxanh-frontend/src/components/ShoppingListExport.tsx
  - app/mamxanh-frontend/src/lib/shoppingListExport.ts
implementation_status: verified
last_verified: 2026-11-01
---

# Thao tác khi đi chợ và xuất danh sách mua sắm

## Chức năng này dùng để làm gì?

Khi bạn đang ở trong siêu thị hoặc ngoài chợ, giao diện Danh sách đi chợ (`/di-cho`) hoạt động như một ứng dụng ghi chú thông minh. Bạn có thể dễ dàng đánh dấu các món đã cho vào giỏ, thêm những đồ lặt vặt cần mua thêm, hoặc trích xuất danh sách để gửi cho người thân cùng đi chợ.

## Các thao tác tương tác chính

### 1. Đánh dấu mặt hàng đã mua (Checklist tương tác)
- Mỗi dòng nguyên liệu đều có một ô tích chọn tròn hoặc hình trái tim ở đầu dòng.
- Khi bạn đã nhặt món hàng đó vào giỏ, chỉ cần **nhấp vào ô tích**.
- Mặt hàng sẽ lập tức chuyển sang trạng thái **Đã mua** (chữ mờ đi và có gạch ngang), đồng thời tự động chuyển xuống nhóm cuối danh sách.
- Nếu bạn tích nhầm, chỉ cần bấm lại vào ô tích để đưa mặt hàng trở lại danh sách cần mua.

### 2. Thêm mặt hàng thủ công ngoài công thức
- Trong lúc đi chợ, nếu bạn cần mua thêm những thứ ngoài công thức món ăn (như *giấy ăn*, *dầu ăn*, *nước ngọt*, *trái cây tráng miệng*...):
  1. Bấm nút **+ Thêm mặt hàng** ở đầu danh sách.
  2. Nhập tên mặt hàng (tối đa 200 ký tự).
  3. Nhập số lượng và chọn đơn vị tính (chai, gói, kg, quả...).
  4. Bấm **Lưu**. Mặt hàng sẽ xuất hiện ngay trong danh sách đi chợ của bạn.

### 3. Chỉnh sửa hoặc Xóa từng mặt hàng
- **Sửa số lượng / đơn vị:** Nhấn vào biểu tượng cây bút cạnh món đồ để cập nhật lại số lượng bạn muốn mua.
- **Xóa một món:** Nhấn vào biểu tượng thùng rác cạnh dòng đó để xóa khỏi danh sách.

### 4. Dọn dẹp danh sách (Xóa nhanh các món đã mua)
- Sau khi đã nhặt xong một lượt, bạn có thể bấm nút **"Dọn dẹp các món đã mua"**.
- Toàn bộ các dòng đã được đánh dấu tích xanh sẽ được gỡ bỏ khỏi màn hình, chỉ để lại những mặt hàng bạn chưa tìm thấy, giúp danh sách luôn gọn gàng và dễ nhìn.

## Xuất và chia sẻ danh sách đi chợ

Mâm Xanh cung cấp 2 cách đơn giản để bạn mang danh sách đi chợ theo mình:

### 1. Sao chép vào Clipboard (Copy to Clipboard)
- Bấm nút **Sao chép** (biểu tượng Copy).
- Toàn bộ danh sách cần mua sẽ được định dạng ngắn gọn và lưu vào bộ nhớ tạm:
  ```text
  Danh sách đi chợ: Đi chợ cuối tuần
  - Nấm đùi gà: 500 g
  - Đậu hũ trắng: 3 bìa
  - Cà chua: 4 quả
  - Rau cải ngọt: 1 kg
  ```
- Bạn chỉ cần dán (`Paste`) vào Zalo, Messenger, SMS hoặc ứng dụng Ghi chú của điện thoại để gửi cho người thân hoặc xem offline.

### 2. Xuất thành tệp văn bản (.txt)
- Bấm nút **Xuất tệp text** (`.txt`).
- Một tệp tin văn bản sẽ được tải ngay về máy tính hoặc điện thoại của bạn để lưu trữ.

## Những điều cần lưu ý

1. **Thao tác an toàn, không lo mất dữ liệu:**
   - Mọi thay đổi (tích đã mua, đổi số lượng, thêm mặt hàng mới) đều được lưu trữ trực tiếp theo thời gian thực lên máy chủ. Bạn có thể mở danh sách trên máy tính ở nhà để lên kế hoạch, sau đó mở điện thoại ngoài chợ để vừa đi vừa tích chọn mà không bị mất dữ liệu.
2. **Không làm thay đổi công thức gốc:**
   - Việc bạn xóa bớt nguyên liệu hay đổi số lượng trong danh sách đi chợ chỉ có hiệu lực với đợt mua sắm lần đó, hoàn toàn không làm thay đổi bài công thức hay thực đơn tuần của bạn.
