> **Document:** Activity Diagram Workspace Guide  
> **File:** `docs/diagrams/Activity/README.md`  
> **Version:** v1.0.0  
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-12  
> **Status:** Active  

# Activity Diagram - Sơ Đồ Hoạt Động

## Mục Đích

`Activity Diagram` (Sơ đồ hoạt động) mô tả trực quan luồng xử lý và các bước thực hiện của một chức năng nghiệp vụ cụ thể từ lúc bắt đầu cho tới khi kết thúc. Sơ đồ này giúp:
* Làm rõ quy trình hoạt động của các chức năng phức tạp trong hệ thống.
* Giúp các thành viên phát triển hiểu đúng logic nghiệp vụ để viết code chính xác.
* Tránh tình trạng hardcode (viết cố định giá trị trong code thay vì lấy từ dữ liệu/tham số đầu vào) dẫn đến luồng xử lý sai thực tế.

## Khi Nào Cần Vẽ

Bạn nên thiết kế sơ đồ hoạt động cho các chức năng có nhiều bước xử lý hoặc có rẽ nhánh logic phức tạp. Ví dụ:
* Đăng nhập / Đăng ký hệ thống.
* Đặt lịch hẹn (Booking) hoặc Tạo đơn hàng mới.
* Thanh toán (Checkout) trực tuyến.
* Quản lý dữ liệu lớn (CRUD) có ràng buộc vai trò phức tạp.

## Quy Tắc Đặt Tên File

Mã nguồn sơ đồ và hình ảnh xuất ra của Activity Diagram nên được đặt tên thống nhất và lưu trữ trong thư mục này theo định dạng:
```text
activity-[ten-chuc-nang-viet-lien-khong-dau].drawio
activity-[ten-chuc-nang-viet-lien-khong-dau].png
```

Ví dụ cụ thể:
* `activity-login.drawio` (file sơ đồ gốc vẽ trên Draw.io)
* `activity-create-booking.png` (ảnh xuất ra từ sơ đồ để nhúng vào tài liệu)

---

## Mẫu Mô Tả Sơ Đồ

Dưới đây là mẫu ghi chú kèm theo cho mỗi sơ đồ hoạt động được đưa vào tài liệu:

### Activity: Member thêm công thức vào lịch ăn

**Chức năng liên quan:** `FR-xx` trong SRS

**Vai trò người dùng (Actor):** Member

**Luồng xử lý chính (Main Flow):**
1. Member chọn bài công thức công khai và ngày/bữa muốn thêm.
2. Backend xác thực quyền, dữ liệu ngày/bữa và quy tắc chống trùng.
3. Hệ thống lưu mục lịch ăn và trả kết quả để giao diện cập nhật.

**Luồng thay thế (Alternative Flow):**
1. Nếu phiên hết hạn, hệ thống yêu cầu đăng nhập lại mà không tạo dữ liệu.
2. Nếu cùng công thức đã có trong ngày/bữa đó, backend từ chối theo Business Rule tương ứng.

**Ghi chú:** Gắn link tới FR/BR, Issue và test khi sơ đồ thật được tạo.
