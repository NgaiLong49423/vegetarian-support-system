# Hướng dẫn tạo MAMXANH_JWT_SECRET

`MAMXANH_JWT_SECRET` là khóa bí mật Backend dùng để ký và kiểm tra JWT access token khi người dùng đăng nhập. Bạn tự tạo khóa ngẫu nhiên trên máy, không cần lấy từ Google hoặc nhà cung cấp bên ngoài. Backend yêu cầu giá trị có ít nhất **32 byte theo UTF-8**; nếu thiếu hoặc quá ngắn, Backend sẽ không khởi động.

## 1. Tạo khóa trên Windows

Mở PowerShell và chạy lệnh sau (cần cài Node.js):

```powershell
node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"
```

Lệnh tạo 48 byte ngẫu nhiên và hiển thị chuỗi Base64. Sao chép toàn bộ chuỗi hiện ra; đây là giá trị bí mật của bạn. Không gửi chuỗi này vào chat, ảnh chụp màn hình hoặc tài liệu.

Nếu PowerShell báo không tìm thấy `node`, hãy cài Node.js theo hướng dẫn môi trường của nhóm, mở lại PowerShell rồi chạy lại lệnh.

## 2. Điền vào file cấu hình local

Mở file `app/mamxanh-backend/.env` và điền chuỗi vừa tạo sau dấu `=`:

```dotenv
MAMXANH_JWT_SECRET=<dan-chuoi-vua-tao-vao-day>
```

Thay cả phần `<dan-chuoi-vua-tao-vao-day>` bằng giá trị thật, không thêm dấu ngoặc nhọn, dấu ngoặc kép hoặc khoảng trắng. Lưu file. Dòng trên chỉ là ví dụ, không phải key sử dụng được.

Nếu chưa có `.env`, từ thư mục gốc repository chạy lệnh sau để tạo từ template mà không ghi đè file hiện có:

```powershell
if (-not (Test-Path app/mamxanh-backend/.env)) {
    Copy-Item app/mamxanh-backend/.env.example app/mamxanh-backend/.env
}
```

Sau đó mở `.env` để điền key và các biến khác cần thiết cho môi trường của bạn. Không điền giá trị thật vào `.env.example`; không commit hoặc chia sẻ file `.env`.

## 3. Cho Backend sử dụng key

### Chạy bằng Docker Compose

Từ thư mục gốc repository, chạy theo cấu hình dùng chung:

```powershell
$composeProject = 'mamxanh-dev'
docker compose -p $composeProject --env-file app/mamxanh-backend/.env up --build --detach --wait --wait-timeout 600
```

Compose đọc `.env` được chỉ định và truyền biến vào Backend. Cần cấu hình cả các biến bắt buộc khác như `MSSQL_SA_PASSWORD` và `MAMXANH_DEMO_PASSWORD`; xem [hướng dẫn Backend](../../app/mamxanh-backend/README.md) để biết đầy đủ bước chạy. Sau khi đổi key, chạy lại lệnh trên để Compose áp dụng cấu hình mới.

### Chạy trực tiếp bằng IntelliJ hoặc Maven

Spring Boot không tự động đọc file `.env`. Hãy cấp `MAMXANH_JWT_SECRET` cho **environment variables** của tiến trình Backend bằng cơ chế local bạn đang dùng, rồi khởi động lại Backend. Với IntelliJ, có thể dùng cơ chế nạp biến môi trường local phù hợp với cấu hình chạy của bạn; không lưu credential trong Run Configuration được chia sẻ. Với Maven, biến cần có trong PowerShell đã mở trước khi chạy Backend. Không đặt giá trị key trực tiếp trong lệnh, source code hoặc file được commit.

## 4. Kiểm tra và lưu ý

- Nếu thấy lỗi `MAMXANH_JWT_SECRET must be set to at least 32 bytes`, kiểm tra biến đã được truyền vào đúng tiến trình Backend và đã điền đầy đủ. Không in giá trị key để kiểm tra.
- Sau khi khởi động thành công, có thể mở `http://localhost:8080/v3/api-docs` để kiểm tra Backend phản hồi. Việc mở trang này không thay thế kiểm tra đăng nhập.
- Giữ cùng một key giữa các lần khởi động. Nếu đổi key, token cũ không còn xác thực được và người dùng cần đăng nhập lại.
- Nếu key bị lộ, hãy tạo key mới và cập nhật cấu hình môi trường phù hợp. Không dùng key thật trong ví dụ, log hoặc tài liệu.

Nguồn dự án: [Backend README](../../app/mamxanh-backend/README.md), [template biến môi trường](../../app/mamxanh-backend/.env.example) và [hướng dẫn Docker Compose](../../CONTRIBUTING.md#docker-development).
