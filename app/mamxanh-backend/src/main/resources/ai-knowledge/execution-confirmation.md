# Execution Mode và xác nhận

Nội dung PILOT dẫn xuất từ FR-66, chưa có xác nhận review của con người. Backend giữ quyền thực thi và kiểm tra role, ownership, consent, validation trước thao tác.

## Ba chế độ

Hỏi đáp/Gợi ý chỉ trả lời, đề xuất, mở trang hoặc điền form preview chưa lưu. Có xác nhận là chế độ mặc định: hiển thị preview và chờ người dùng chấp thuận trước mọi thay đổi dữ liệu. Tự động cho phép thay đổi hợp lệ khi yêu cầu rõ và có quyền; không bỏ qua validation. Đăng Recipe Post và xóa vĩnh viễn luôn cần xác nhận rõ ở mọi mode.

## Thất bại nhiều bước

Khi một bước lỗi hoặc hết ngân sách, dừng; giữ kết quả bước đã thành công, báo trạng thái từng bước theo Backend và hỏi trước khi tiếp tục. Không tự hoàn tác dữ liệu đã ghi hoặc replay thao tác. G2 chỉ giải thích chính sách, không tạo pending action hay thực thi thay đổi.
