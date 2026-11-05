---
id: expert-registration
title: Quy trình đăng ký trở thành Chuyên gia ẩm thực chay
category: accounts
audience:
  - customer
source_requirements:
  - FR-05
  - BR-74
source_use_cases:
  - UC-05.1
  - UC-05.2
  - UC-05.3
  - UC-05.4
  - UC-05.5
source_files:
  - app/mamxanh-frontend/src/pages/ExpertApplication.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/expertapplication/controller/ExpertApplicationController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Quy trình đăng ký trở thành Chuyên gia ẩm thực chay

## Chức năng này dùng để làm gì?

Mâm Xanh là sân chơi công thức chuẩn mực do chính các Chuyên gia ẩm thực chay sáng tạo. Người dùng thông thường (Customer) không có quyền đăng bài trực tiếp, nhưng nếu bạn là đầu bếp chay, chuyên viên dinh dưỡng, blogger ẩm thực hoặc có kinh nghiệm nấu món chay phong phú, bạn có thể **nộp đơn đăng ký nâng cấp thành Chuyên gia (Expert)**.

Sau khi đơn của bạn được Quản trị viên (Administrator) xét duyệt thành công, tài khoản của bạn sẽ lập tức được thăng hạng thành `EXPERT` và mở khóa toàn bộ quyền đăng công thức công khai lên nền tảng.

## Điều kiện và biểu mẫu đăng ký

### Ai có thể nộp đơn?
- Mọi thành viên thông thường (`Customer`) có tài khoản đang hoạt động bình thường trên Mâm Xanh.
- Mỗi tài khoản chỉ được có **tối đa một đơn đăng ký ở trạng thái Chờ duyệt (`PENDING`)**. Bạn không thể gửi thêm đơn mới khi đơn trước đó chưa có kết quả.

### Nội dung biểu mẫu cần điền (tại `/dang-ky-chuyen-gia`)
1. **Kinh nghiệm ẩm thực chay (bắt buộc):**
   - Tối thiểu 20 ký tự.
   - Mô tả quá trình nấu ăn chay, kinh nghiệm làm bếp, các khóa học hoặc hoạt động ẩm thực bạn đã tham gia.
2. **Trường phái ẩm thực chay sở trường (bắt buộc):**
   - Chọn trường phái bạn am hiểu nhất (Thuần chay, Chay có sữa, Chay có trứng, Chay kết hợp...).
3. **Tóm tắt công thức hoặc phong cách món ăn sở trường (bắt buộc):**
   - Tối thiểu 30 ký tự.
   - Giới thiệu phong cách nấu, các món chay tâm đắc bạn dự định đóng góp cho cộng đồng.
4. **Đường dẫn tham khảo / Portfolio (tùy chọn):**
   - Đúng 1 liên kết hợp lệ (bắt đầu bằng `http://` hoặc `https://`) dẫn đến trang cá nhân, blog, kênh ẩm thực hoặc bài viết của bạn.
5. **Tài liệu chứng nhận chuyên môn (tùy chọn):**
   - 1 tệp đính kèm định dạng PDF, JPEG hoặc PNG, dung lượng tối đa 5 MB (ví dụ: chứng chỉ nghề bếp, bằng cấp dinh dưỡng, giấy khen ẩm thực).
   - *Lưu ý:* Đây là tài liệu phục vụ thẩm định nội bộ, Mâm Xanh không yêu cầu và không tiếp nhận giấy tờ tùy thân (CCCD/CMND).
6. **Ảnh thẻ chân dung của đơn (tùy chọn):**
   - 1 ảnh chân dung tỷ lệ khung hình 3:4 (JPEG, PNG hoặc WebP, tối đa 2 MB). Ảnh này chỉ lưu riêng cho hồ sơ thẩm định của Ban quản trị và **không tự động thay thế avatar công khai** của tài khoản bạn.

## Quy trình xét duyệt

1. **Gửi đơn:** Bạn điền biểu mẫu và nhấn **Nộp đơn đăng ký**. Trạng thái đơn lúc này là **Chờ duyệt** (`PENDING`).
2. **Thẩm định:** Ban quản trị xem xét nội dung kinh nghiệm, đối chiếu thông tin và tài liệu đính kèm.
3. **Kết quả xét duyệt:**
   - **Được phê duyệt (`APPROVED`):** Bạn sẽ nhận được thông báo chúc mừng trong ứng dụng. Vai trò tài khoản của bạn được chuyển thành `EXPERT` ngay lập tức, nút "Đăng công thức" sẽ xuất hiện trên thanh điều hướng.
   - **Bị từ chối (`REJECTED`):** Bạn sẽ nhận được thông báo kèm lý do từ chối cụ thể từ Ban quản trị (tối thiểu 10 ký tự, ví dụ: "Kinh nghiệm mô tả chưa đủ chi tiết", "Thiếu liên kết chứng minh hoạt động ẩm thực"...). Sau khi bị từ chối, bạn hoàn toàn có thể bổ sung thông tin và nộp đơn mới.

## Những điều cần lưu ý

1. **Tính bảo mật của tài liệu đính kèm:**
   - Các tệp tài liệu và ảnh thẻ bạn đính kèm trong đơn được lưu trữ an toàn và riêng tư trên hệ thống máy chủ đám mây, chỉ có Quản trị viên mới có quyền xem để thẩm định. Cộng đồng và các thành viên khác không thể xem hoặc tải các tệp này.
2. **Cấp quyền Chuyên gia trực tiếp:**
   - Trong một số trường hợp, Ban quản trị có thể trực tiếp mời và cấp quyền Chuyên gia cho một thành viên uy tín. Khi đó, nếu bạn đang có một đơn ở trạng thái chờ duyệt, đơn đó sẽ tự động chuyển sang trạng thái đã xử lý và bạn nhận được thông báo thăng hạng.
