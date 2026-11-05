---
id: password-recovery
title: Quên mật khẩu và khôi phục tài khoản
category: accounts
audience:
  - customer
  - expert
source_requirements:
  - FR-03
  - NFR-06
source_use_cases:
  - UC-03.6
  - UC-03.7
source_files:
  - app/mamxanh-frontend/src/pages/Auth.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/auth/service/PasswordResetEmailNotifier.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/auth/controller/AuthController.java
implementation_status: verified
last_verified: 2026-11-03
---

# Quên mật khẩu và khôi phục tài khoản

## Chức năng này dùng để làm gì?

Trong trường hợp bạn quên mật khẩu hoặc không thể đăng nhập vào tài khoản Mâm Xanh, bạn có thể tự khôi phục mật khẩu một cách an toàn thông qua email đã đăng ký.

## Các bước lấy lại mật khẩu

### Bước 1: Gửi yêu cầu đặt lại mật khẩu
1. Trên trang **Đăng nhập** (`/dang-nhap`), nhấn vào liên kết **Quên mật khẩu?** hoặc truy cập trực tiếp `/quen-mat-khau`.
2. Nhập chính xác địa chỉ email bạn đã sử dụng để đăng ký tài khoản Mâm Xanh.
3. Nhấn nút **Gửi yêu cầu**.
4. Màn hình sẽ thông báo: "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn." (Thông báo này được thiết kế nhằm bảo vệ quyền riêng tư, tránh lộ thông tin email có tồn tại hay không).

### Bước 2: Nhận email và mở liên kết đặt lại
1. Mở hòm thư của bạn và tìm thư có tiêu đề gửi từ Mâm Xanh.
2. Trong thư sẽ có một liên kết đặt lại mật khẩu an toàn.
3. Nhấp vào liên kết (hoặc sao chép dán vào trình duyệt). Đường dẫn sẽ đưa bạn đến trang `/dat-lai-mat-khau`.

### Bước 3: Thiết lập mật khẩu mới
1. Nhập mật khẩu mới thỏa mãn yêu cầu bảo mật:
   - Tối thiểu 8 ký tự.
   - Có ít nhất một chữ in hoa, một chữ in thường, một chữ số và một ký tự đặc biệt.
2. Nhập lại mật khẩu mới để xác nhận.
3. Nhấn **Cập nhật mật khẩu**.
4. Khi nhận được thông báo thành công, bạn có thể quay lại trang Đăng nhập và sử dụng mật khẩu mới ngay lập tức.

## Những điều cần lưu ý

1. **Thời hạn hiệu lực của liên kết:**
   - Liên kết đặt lại mật khẩu chỉ dùng được một lần và có hiệu lực 15 phút theo cấu hình hiện tại. Nếu liên kết đã hết hạn, đã được sử dụng hoặc đã bị thay thế bởi yêu cầu mới hơn, bạn hãy gửi lại yêu cầu đặt lại mật khẩu.
2. **Không nhận được email:**
   - Kiểm tra kỹ thư mục Spam, Thư rác hoặc Quảng cáo.
   - Kiểm tra xem bạn có gõ sai địa chỉ email khi yêu cầu hay không.
3. **Tài khoản đăng nhập bằng Google:**
   - Nếu bạn tạo tài khoản bằng nút "Tiếp tục với Google", bạn không có mật khẩu riêng trên Mâm Xanh. Bạn chỉ cần nhấn nút "Tiếp tục với Google" để đăng nhập trực tiếp.

## Nếu liên kết không hoạt động

Nếu trang báo liên kết không hợp lệ hoặc đã hết hạn, hãy quay lại trang **Quên mật khẩu?** và gửi yêu cầu mới. Mỗi yêu cầu mới thay thế liên kết trước đó, nên hãy mở liên kết mới nhất trong email. Nếu chưa thấy thư, kiểm tra thư rác và xác nhận bạn đã nhập đúng địa chỉ email; thông báo trên màn hình không xác nhận email có tài khoản hay không.
