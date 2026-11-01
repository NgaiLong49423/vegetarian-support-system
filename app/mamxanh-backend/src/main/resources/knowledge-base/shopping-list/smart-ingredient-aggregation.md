---
id: smart-ingredient-aggregation
title: Cơ chế tự động tổng hợp nguyên liệu an toàn
category: shopping-list
audience:
  - customer
  - expert
source_requirements:
  - FR-54
  - BR-14
  - BR-73
source_use_cases:
  - UC-54.1
  - UC-54.2
source_files:
  - app/mamxanh-frontend/src/pages/ShoppingList.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/shopping/service/ShoppingListAggregationService.java
implementation_status: verified
last_verified: 2026-11-01
---

# Cơ chế tự động tổng hợp nguyên liệu an toàn

## Chức năng này dùng để làm gì?

Khi bạn tạo danh sách đi chợ từ thực đơn tuần gồm hàng chục món ăn khác nhau, nhiều món sẽ cùng sử dụng chung các nguyên liệu cơ bản (ví dụ: Thứ Hai cần 200g nấm rơm, Thứ Ba cần 300g nấm rơm, Thứ Sáu cần thêm 150g nấm rơm).

Nếu hệ thống liệt kê rời rạc từng dòng, bạn sẽ rất khó biết tổng cộng mình cần mua bao nhiêu. Tính năng **Tổng hợp nguyên liệu an toàn** của Mâm Xanh sẽ tự động gom các nguyên liệu giống nhau lại thành một dòng duy nhất với tổng số lượng đã được cộng dồn chính xác.

## Quy tắc tổng hợp theo 3 thứ nguyên đo lường

Hệ thống phân biệt rõ ràng 3 nhóm thứ nguyên đo lường (Dimension) và chỉ thực hiện cộng dồn khi an toàn:

### 1. Nhóm Khối lượng (MASS): `mg`, `g`, `kg`
- **Quy đổi tự động tuyệt đối an toàn:**
  - $1.000\text{ mg} = 1\text{ g}$
  - $1\text{ kg} = 1.000\text{ g}$
- **Ví dụ gom hàng:** Món A cần $300\text{ g}$ cà rốt, Món B cần $0.5\text{ kg}$ cà rốt $\rightarrow$ Hệ thống tự động quy về gam và hiển thị tổng cộng: **$800\text{ g}$ cà rốt** (hoặc $0.8\text{ kg}$).

### 2. Nhóm Thể tích (VOLUME): `ml`, `L`, `tsp`, `tbsp`, `cup`
- **Quy đổi chuẩn hóa theo dung tích:**
  - $1\text{ L} = 1.000\text{ ml}$
  - $1\text{ tsp}$ (thìa cà phê) $= 5\text{ ml}$
  - $1\text{ tbsp}$ (thìa canh) $= 15\text{ ml}$
  - $1\text{ cup}$ (cốc/chén) $= 240\text{ ml}$
- **Ví dụ gom hàng:** Nước tương cần $2\text{ tbsp}$ ($30\text{ ml}$) ở món kho và $100\text{ ml}$ ở món xào $\rightarrow$ Danh sách đi chợ hiển thị tổng cộng: **$130\text{ ml}$ nước tương**.

### 3. Nhóm Số đếm (COUNT): `quả`, `củ`, `miếng`
- Các đơn vị đếm chỉ được gom với nhau khi chúng có cùng đơn vị đếm cho cùng một nguyên liệu (ví dụ: $2\text{ bìa đậu phụ} + 3\text{ bìa đậu phụ} = 5\text{ bìa đậu phụ}$).
- Hệ thống **tuyệt đối không tự ý suy diễn** rằng 1 củ tương đương bao nhiêu quả hay bao nhiêu miếng nếu không cùng loại.

## Xử lý các dòng không thể quy đổi tương thích

### Trường hợp nào không thể gom tự động?
Ví dụ trong tuần bạn có một món ghi $2\text{ củ}$ cà rốt, và một món khác ghi $200\text{ g}$ cà rốt:
- Củ cà rốt có thể to hoặc nhỏ khác nhau tùy chợ. Nếu hệ thống chưa có hệ số quy đổi chuẩn giữa "củ" và "gam" cho loại cà rốt đó, việc ép buộc cộng gộp có thể làm sai lệch lượng nguyên liệu bạn cần mua.

### Giải pháp an toàn của Mâm Xanh:
- Hệ thống **tách riêng thành 2 dòng rõ ràng** trong danh sách đi chợ:
  - Dòng 1: Cà rốt — $200\text{ g}$
  - Dòng 2: Cà rốt — $2\text{ củ}$
- Đồng thời hệ thống hiển thị thông báo chú thích nhẹ để bạn nắm rõ. Bạn có thể tự mình điều chỉnh lại số lượng nếu muốn.

## Những điều cần lưu ý

1. **Chỉ gom cùng mã nguyên liệu chuẩn (`ingredientId`):**
   - Hệ thống chỉ cộng dồn khi hai nguyên liệu thực sự cùng trỏ tới một mục trong từ điển chuẩn của hệ thống (ví dụ: *Nấm đùi gà* không bao giờ bị gom nhầm với *Nấm rơm*).
2. **Không làm tròn về 0:**
   - Với những nguyên liệu cần số lượng nhỏ (như nhụy hoa nghệ tây, bột quế tính bằng miligram), hệ thống lưu trữ độ chính xác cao và không bao giờ tự ý làm tròn thành số 0.
3. **Chỉnh sửa sau khi gom:**
   - Sau khi danh sách đã được tạo tự động, bạn hoàn toàn có thể nhấn vào từng dòng để chỉnh sửa lại số lượng hoặc đổi đơn vị mua theo ý muốn thực tế khi đi chợ.
