---
id: account-and-login-issues
title: Khắc phục sự cố đăng ký, đăng nhập và tài khoản
category: troubleshooting
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-03
  - NFR-06
  - NFR-07
source_files:
  - app/mamxanh-frontend/src/pages/Auth.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/auth/controller/AuthController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Khắc phục sự cố đăng ký, đăng nhập và tài khoản

## Bảng tổng hợp các sự cố thường gặp

| Hiện tượng | Nguyên nhân có thể | Cách xử lý |
|---|---|---|
| **Không nhận được email xác minh khi đăng ký** | Thư bị phân loại nhầm vào Spam, hoặc gõ sai địa chỉ email | Kiểm tra thư mục Spam / Thư rác / Quảng cáo. Chờ 2-3 phút, nếu vẫn chưa nhận được thì bấm nút **Gửi lại email xác minh**. |
| **Báo lỗi "Tài khoản tạm thời bị khóa 10 phút"** | Nhập sai mật khẩu 5 lần liên tiếp (Cơ chế bảo vệ chống đoán mật khẩu) | Đợi đủ 10 phút để hệ thống tự động mở khóa. Sau đó dùng tính năng **Quên mật khẩu** để đổi mật khẩu mới an toàn. |
| **Báo lỗi "Email đã được sử dụng" khi đăng ký** | Bạn hoặc ai đó đã đăng ký tài khoản với email này trước đó | Dùng tính năng **Đăng nhập** hoặc bấm **Quên mật khẩu** để khôi phục quyền truy cập vào tài khoản cũ. |
| **Không đăng nhập được bằng nút Google** | Trình duyệt chặn pop-up đăng nhập, hoặc mất kết nối mạng | Bật cho phép cửa sổ bật lên (Pop-up) trên trình duyệt, thử dùng trình duyệt ẩn danh hoặc kiểm tra lại đường truyền Internet. |
| **Báo lỗi "Mật khẩu không thỏa mãn yêu cầu bảo mật"** | Mật khẩu quá ngắn hoặc thiếu các loại ký tự bắt buộc | Đặt mật khẩu dài từ 8 ký tự trở lên, gồm đủ: chữ in hoa (A-Z), chữ thường (a-z), chữ số (0-9) và ít nhất một ký tự đặc biệt (@, #, $, %, !...). |
| **Tài khoản bị khóa vĩnh viễn và không đăng nhập được** | Tài khoản bị Quản trị viên xử lý kỷ luật do vi phạm chính sách cộng đồng | Màn hình sẽ hiển thị lý do vi phạm. Vui lòng liên hệ với Ban quản trị qua email hỗ trợ để được giải đáp hoặc khiếu nại. |

## Hướng dẫn chi tiết từng trường hợp

### 1. Không nhận được email xác nhận hoặc đặt lại mật khẩu
- Đảm bảo bạn gõ chính xác từng ký tự của email (chú ý lỗi gõ tiếng Việt tự động thêm dấu vào email như `nguyen@gmai.com`).
- Một số nhà cung cấp dịch vụ thư (như Outlook, Yahoo) có thể có độ trễ chuyển tiếp thư từ 3-5 phút.
- Nếu bạn sử dụng tài khoản đăng nhập nhanh qua Google, bạn không cần tìm email xác nhận mật khẩu; chỉ cần bấm nút "Tiếp tục với Google".

### 2. Xử lý khi quên mật khẩu hoàn toàn
1. Mở trang Đăng nhập và nhấp vào **Quên mật khẩu?** (`/quen-mat-khau`).
2. Điền email của bạn và bấm **Gửi yêu cầu**.
3. Mở email, nhấp vào liên kết xác nhận và điền mật khẩu mới.
4. Lưu ý: Liên kết đặt lại mật khẩu chỉ có hiệu lực một lần duy nhất. Nếu liên kết báo hết hạn, hãy thực hiện lại các bước để nhận liên kết mới.
