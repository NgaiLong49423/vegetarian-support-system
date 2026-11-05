---
id: profile-management
title: Quản lý thông tin hồ sơ và cài đặt tài khoản
category: accounts
audience:
  - customer
  - expert
source_requirements:
  - FR-03
  - FR-23
  - BR-18
source_use_cases:
  - UC-23.2
  - UC-23.3
source_files:
  - app/mamxanh-frontend/src/pages/Profile.tsx
  - app/mamxanh-frontend/src/pages/ProfileSettingsPage.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/auth/controller/MemberProfileController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Quản lý thông tin hồ sơ và cài đặt tài khoản

## Chức năng này dùng để làm gì?

Trang hồ sơ cá nhân (`/ho-so`) và cài đặt hồ sơ (`/ho-so/cai-dat`) giúp bạn quản lý các thông tin đại diện của mình trên Mâm Xanh, xem lại các hoạt động cá nhân và điều chỉnh thiết lập tài khoản.

## Thông tin nào hiển thị công khai và thông tin nào được bảo mật?

### Thông tin công khai (Mọi người có thể thấy):
- **Tên hiển thị:** Tên xuất hiện trên các bài viết, bình luận và phản hồi.
- **Ảnh đại diện (Avatar):** Hình ảnh đại diện cho tài khoản.
- **Tiểu sử ngắn (Bio):** Giới thiệu ngắn về bản thân hoặc trường phái ăn chay yêu thích.
- **Huy hiệu vai trò:** Nếu bạn là Chuyên gia, hồ sơ sẽ có huy hiệu `Chuyên gia` nổi bật.
- **Số lượng Người theo dõi (Followers) & Đang theo dõi (Following).**
- **Đối với Chuyên gia:** Danh sách các công thức chay đã được công khai, tổng lượt thích (% Like) và tổng lượt xem của tất cả các bài viết.

### Thông tin riêng tư (Chỉ bạn và hệ thống biết, tuyệt đối không công khai):
- Địa chỉ email và mật khẩu tài khoản.
- Hồ sơ sở thích ăn uống cá nhân (các món ghét, dị ứng).
- Dữ liệu dinh dưỡng cá nhân (chiều cao, cân nặng, chỉ số BMI, calorie mục tiêu).
- Nội dung thực đơn tuần cá nhân (`Meal Plan`) và danh sách đi chợ (`Shopping List`).
- Các công thức bạn đã bấm lưu (`Saved Recipes`).

## Cách chỉnh sửa hồ sơ

1. Đăng nhập vào Mâm Xanh, bấm vào avatar ở thanh điều hướng trên cùng và chọn **Hồ sơ cá nhân** (`/ho-so`).
2. Nhấn nút **Chỉnh sửa hồ sơ** hoặc vào trực tiếp `/ho-so/cai-dat`.
3. Bạn có thể thay đổi:
   - **Tên hiển thị:** Từ 2 đến 50 ký tự.
   - **Tiểu sử ngắn:** Tối đa 250 ký tự chia sẻ về bản thân.
   - **Ảnh đại diện:** Tải ảnh từ thiết bị lên hệ thống (định dạng ảnh JPEG, PNG dung lượng tối đa quy định).
4. Nhấn **Lưu thay đổi** để cập nhật thông tin.

## Xem hồ sơ của thành viên hoặc Chuyên gia khác

- Khi nhấp vào tên tác giả của một bài công thức hoặc tìm kiếm trong Danh bạ chuyên gia (`/chuyen-gia`), bạn sẽ được chuyển đến trang hồ sơ công khai của họ (`/thanh-vien/:userId`).
- Tại đây, bạn có thể xem các công thức do họ sáng tác và nhấn nút **Theo dõi** (`Follow`) để cập nhật những bài viết mới từ họ.
