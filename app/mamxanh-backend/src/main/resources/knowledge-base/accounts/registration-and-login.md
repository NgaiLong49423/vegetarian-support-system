---
id: registration-and-login
title: Đăng ký, đăng nhập và bảo mật tài khoản
category: accounts
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-03
  - NFR-06
  - NFR-07
source_use_cases:
  - UC-03.1
  - UC-03.2
  - UC-03.3
  - UC-03.4
  - UC-03.5
  - UC-03.9
source_files:
  - app/mamxanh-frontend/src/pages/Auth.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/auth/controller/AuthController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Đăng ký, đăng nhập và bảo mật tài khoản

## Chức năng này dùng để làm gì?

Tài khoản Mâm Xanh là chìa khóa để bạn lưu trữ toàn bộ dữ liệu cá nhân (công thức yêu thích, thực đơn tuần, danh sách đi chợ, hồ sơ dinh dưỡng) và tham gia tương tác với cộng đồng ẩm thực chay.

## Các hình thức đăng ký và đăng nhập

### 1. Đăng ký bằng Email và Mật khẩu
1. Truy cập trang **Đăng ký** (`/dang-ky`).
2. Điền các thông tin:
   - **Tên hiển thị:** Tên bạn muốn xuất hiện khi bình luận hoặc chia sẻ trên Mâm Xanh.
   - **Địa chỉ Email:** Email thật để nhận liên kết xác thực tài khoản.
   - **Mật khẩu:** Tối thiểu 8 ký tự, bao gồm ít nhất một chữ hoa, một chữ thường, một chữ số và một ký tự đặc biệt.
3. Nhấn nút **Đăng ký**.
4. **Xác minh Email:** Hệ thống sẽ gửi một email xác minh kèm đường link kích hoạt. Hãy mở hộp thư (kiểm tra cả mục Spam/Thư rác nếu chưa thấy) và bấm vào liên kết để kích hoạt tài khoản.

### 2. Đăng nhập bằng Google
1. Trên màn hình **Đăng nhập** (`/dang-nhap`), nhấn nút **Tiếp tục với Google**.
2. Chọn tài khoản Google của bạn và cấp quyền.
3. Hệ thống sẽ tự động tạo tài khoản hoặc đăng nhập ngay lập tức mà không cần bước xác minh email qua liên kết.

### 3. Đăng xuất an toàn
- Để đăng xuất, bấm vào biểu tượng đại diện của bạn ở góc trên bên phải màn hình và chọn **Đăng xuất**.
- Phiên đăng nhập trên trình duyệt hiện tại sẽ được xóa ngay lập tức.

## Cơ chế bảo vệ tài khoản

1. **Khóa tạm thời khi nhập sai mật khẩu quá 5 lần:**
   - Để ngăn chặn các cuộc tấn công đoán mật khẩu, nếu bạn nhập sai mật khẩu **5 lần liên tiếp**, hệ thống sẽ tự động tạm khóa tài khoản trong vòng **10 phút**.
   - Trong thời gian này, bạn không thể đăng nhập kể cả khi nhập đúng mật khẩu. Hãy đợi hết thời gian khóa hoặc sử dụng tính năng **Quên mật khẩu** để đặt lại.
2. **Mật khẩu được mã hóa an toàn:**
   - Mật khẩu của bạn được mã hóa một chiều bằng thuật toán BCrypt trước khi lưu trữ. Ngay cả đội ngũ quản trị viên Mâm Xanh cũng không thể đọc được mật khẩu gốc của bạn.
3. **Phiên đăng nhập không lưu trữ trạng thái nguy hiểm:**
   - Hệ thống sử dụng mã xác thực bảo mật (JWT Access Token) và kiểm tra trạng thái tài khoản theo thời gian thực. Nếu tài khoản của bạn bị quản trị viên khóa do vi phạm chính sách, phiên làm việc sẽ chấm dứt ngay lập tức.
