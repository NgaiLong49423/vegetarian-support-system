---
id: following-members
title: Theo dõi Chuyên gia và thành viên khác
category: accounts
audience:
  - customer
  - expert
source_requirements:
  - FR-59
  - BR-75
source_use_cases:
  - UC-59.1
  - UC-59.2
  - UC-59.3
  - UC-59.4
source_files:
  - app/mamxanh-frontend/src/pages/MemberProfilePage.tsx
  - app/mamxanh-frontend/src/pages/ExpertDirectoryPage.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/auth/controller/MemberFollowController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Theo dõi Chuyên gia và thành viên khác

## Chức năng này dùng để làm gì?

Tính năng Theo dõi (`Follow`) cho phép bạn kết nối và theo sát các Chuyên gia ẩm thực chay hoặc những thành viên cùng chung niềm đam mê ăn chay trên nền tảng Mâm Xanh.

## Ai có thể sử dụng?

- **Thành viên đã đăng nhập (Customer và Expert):** Có quyền nhấn Theo dõi hoặc Bỏ theo dõi bất kỳ thành viên nào đang hoạt động.
- **Khách vãng lai (Guest):** Chỉ có thể xem số lượng người theo dõi trên trang cá nhân; khi bấm nút Theo dõi, hệ thống sẽ yêu cầu bạn đăng nhập.

## Cách sử dụng

### 1. Theo dõi một Chuyên gia hoặc thành viên
1. Mở trang hồ sơ của thành viên bạn muốn theo dõi (qua liên kết tác giả ở bài viết, hoặc qua trang **Danh bạ Chuyên gia** `/chuyen-gia`).
2. Nhấn nút **Theo dõi** (màu xanh hoặc có biểu tượng dấu cộng).
3. Nút sẽ chuyển sang trạng thái **Đang theo dõi**, đồng thời số lượng người theo dõi của thành viên đó sẽ tự động tăng thêm 1.

### 2. Bỏ theo dõi (Unfollow)
1. Truy cập lại trang cá nhân của người bạn đang theo dõi.
2. Nhấn vào nút **Đang theo dõi**.
3. Hệ thống sẽ hủy trạng thái theo dõi và nút chuyển lại thành **Theo dõi**.

### 3. Xem danh sách Đang theo dõi và Người theo dõi
1. Truy cập vào trang cá nhân của bạn tại `/ho-so`.
2. Bấm vào chỉ số **Đang theo dõi** (`Following`) hoặc **Người theo dõi** (`Followers`) để xem danh sách chi tiết các tài khoản liên quan.

## Những điều cần lưu ý

1. **Quan hệ một chiều (Có hướng):**
   - Khi bạn theo dõi một ai đó, điều này không có nghĩa là người đó tự động theo dõi lại bạn.
2. **Không thể tự theo dõi chính mình:**
   - Hệ thống chặn hành vi tự bấm theo dõi tài khoản của bản thân.
3. **Bảo vệ quyền riêng tư:**
   - Việc theo dõi chỉ giúp bạn dễ dàng tìm kiếm và xem các nội dung công khai của người đó. Việc theo dõi không cấp cho bạn quyền xem các thông tin nhạy cảm của họ (như thực đơn tuần riêng, danh sách đi chợ, dị ứng hoặc số liệu dinh dưỡng cá nhân).
4. **Không có tài khoản bị khóa trong danh sách:**
   - Nếu một tài khoản bị tạm khóa hoặc ngừng hoạt động, họ sẽ không xuất hiện trong danh sách theo dõi công khai.
