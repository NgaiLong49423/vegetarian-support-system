---
id: weekly-meal-planner
title: Lập kế hoạch thực đơn tuần 3 bữa
category: meal-planning
audience:
  - customer
  - expert
source_requirements:
  - FR-09
  - FR-33
  - BR-32
  - BR-35
  - BR-36
  - BR-37
source_use_cases:
  - UC-09.1
  - UC-09.2
  - UC-09.3
  - UC-33.1
  - UC-33.2
  - UC-33.3
source_files:
  - app/mamxanh-frontend/src/pages/MealPlanner.tsx
  - app/mamxanh-frontend/src/components/WeekCalendar.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/controller/MealPlanController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Lập kế hoạch thực đơn tuần 3 bữa

## Chức năng này dùng để làm gì?

Trang **Kế hoạch bữa ăn** (`/ke-hoach`) giúp bạn tổ chức chế độ ăn uống khoa học cho bản thân và gia đình. Bạn có thể lên lịch trước các món chay cho từng ngày trong tuần từ Thứ Hai đến Chủ Nhật, chia rõ theo 3 bữa ăn chính trong ngày.

Việc lên thực đơn tuần giúp bạn:
- Tiết kiệm thời gian mỗi ngày không phải đắn đo "Hôm nay ăn gì?".
- Cân bằng dinh dưỡng và thay đổi món ăn đa dạng.
- Tự động kết nối với **Danh sách đi chợ** để mua đúng và đủ nguyên liệu cho cả tuần.

## Ai có thể sử dụng?

- **Thành viên đã đăng nhập (Customer và Expert):** Có không gian lịch ăn tuần cá nhân được lưu trữ bền vững trên tài khoản.
- **Khách vãng lai (Guest):** Cần đăng nhập để sử dụng tính năng này.

## Cấu trúc lịch ăn tuần

Mâm Xanh áp dụng cấu trúc lịch ăn chuẩn hóa:
- **Chu kỳ tuần:** 7 ngày (bắt đầu từ Thứ Hai và kết thúc vào Chủ Nhật). Bạn có thể dùng mũi tên điều hướng để chuyển qua lại giữa tuần hiện tại, tuần trước hoặc các tuần tương lai.
- **3 bữa ăn cố định mỗi ngày:**
  1. **Bữa sáng (Breakfast)**
  2. **Bữa trưa (Lunch)**
  3. **Bữa tối (Dinner)**
  *(Lưu ý: Hệ thống hiện tại không hỗ trợ bữa phụ Snack hoặc tạo loại bữa tùy chỉnh để giữ sự tinh gọn).*
- **Mỗi bữa có thể có nhiều món:** Bạn có thể thêm 1 hoặc nhiều món vào cùng một bữa (ví dụ Bữa trưa gồm: Canh chua chay + Đậu hũ kho nấm + Rau củ luộc).

## Cách sử dụng

### 1. Thêm món vào lịch ăn
- **Cách 1: Từ trang chi tiết công thức (`/cong-thuc/:slug`)**
  1. Bấm nút **Thêm vào lịch ăn**.
  2. Một hộp thoại sẽ hiện ra, bạn chọn **Ngày ăn** (trong tuần mong muốn) và **Loại bữa** (Sáng, Trưa hoặc Tối).
  3. Chọn **Số khẩu phần dự định ăn** (mặc định là 1 phần, bạn có thể điều chỉnh tùy theo số người ăn).
  4. Bấm **Xác nhận thêm**.
- **Cách 2: Trực tiếp từ trang Lịch ăn (`/ke-hoach`)**
  1. Tại ô của bữa ăn mong muốn (ví dụ Thứ Tư - Bữa tối), bấm vào nút dấu cộng `+ Thêm món`.
  2. Tìm kiếm món ăn trong kho công thức hoặc chọn từ danh sách **Công thức đã lưu** của bạn.
  3. Bấm chọn món để đưa vào bữa ăn.

### 2. Điều chỉnh món trong lịch
- **Đổi số khẩu phần:** Bấm vào biểu tượng số khẩu phần trên món ăn để tăng hoặc giảm.
- **Chuyển bữa / Chuyển ngày:** Kéo thả hoặc bấm vào menu món ăn, chọn chuyển sang bữa khác hoặc ngày khác.
- **Xóa món khỏi lịch:** Bấm vào biểu tượng thùng rác hoặc nút gỡ bỏ trên món ăn. Món ăn sẽ được xóa khỏi lịch ngày hôm đó (nhưng công thức gốc và danh sách Đã lưu của bạn vẫn hoàn toàn nguyên vẹn).

## Những điều cần lưu ý

1. **Quy tắc chống thêm trùng trong cùng một bữa:**
   - Trong cùng một ngày và cùng một loại bữa, một công thức chỉ được thêm tối đa 1 lần. Nếu bạn thêm lại đúng món đó vào cùng bữa, hệ thống sẽ nhắc món đã có trong bữa. Bạn hoàn toàn có thể thêm lại món ăn đó vào bữa khác hoặc ngày khác trong tuần.
2. **Độc lập với Công thức đã lưu (`Saved Recipes`):**
   - Thêm món vào lịch ăn không làm tự động bấm lưu món vào sổ tay, và gỡ món khỏi lịch ăn không xóa món đó khỏi danh sách Đã lưu của bạn.
3. **Khi bài công thức gốc bị ẩn hoặc xóa:**
   - Nếu công thức bị tác giả xóa hoặc bị Ban quản trị ẩn do vi phạm, vị trí của món ăn trong lịch ăn của bạn sẽ hiển thị nhãn "Món ăn hiện không khả dụng" (Tombstone) để bạn chủ động chọn món chay khác thay thế, không làm xáo trộn cấu trúc lịch ăn của bạn.
