# Execution Mode và xác nhận G3

Nội dung PILOT G3 dẫn xuất từ FR-66 và BR-79, chưa có xác nhận review của con người. Backend giữ quyền thực thi và kiểm tra role, trạng thái tài khoản hoạt động, ownership, consent dữ liệu Shopping List, ngân sách và validation trước mọi thao tác.

## Ba chế độ thực thi trong G3

Hỏi đáp/Gợi ý (`QA`) chỉ trả lời hướng dẫn hoặc hiển thị preview tên danh sách dự kiến tạo; tuyệt đối không gọi `create_shopping_list` và không ghi dữ liệu xuống Backend. Có xác nhận (`CONFIRM`) là chế độ mặc định: khi người dùng đã cung cấp tên danh sách hợp lệ, hệ thống tạo yêu cầu xác nhận hiển thị rõ tên danh sách dự kiến tạo và chờ người dùng bấm xác nhận trước khi thực thi ghi. Tự động (`AUTO`) chỉ tự động gọi `create_shopping_list` rồi `get_shopping_list` khi yêu cầu tạo danh sách được nêu rõ ràng, có đầy đủ tên hợp lệ và các guard quyền/consent/ngân sách đều thỏa mãn; chế độ `AUTO` không thực thi các câu phủ định, giả định hoặc câu chỉ hỏi cách làm.

## Quy tắc hiệu lực xác nhận và thất bại nhiều bước

Mỗi xác nhận (`PendingConfirmation`) hết hạn sau 5 phút, gắn chặt với chủ sở hữu, session, lệnh gọi gốc, tên danh sách đã chuẩn hóa và Execution Mode. Nếu người dùng đổi tên danh sách hoặc đổi Execution Mode thì xác nhận cũ mất hiệu lực ngay và không thể dùng lại. Khi thực thi chuỗi tạo danh sách rồi đọc lại: nếu bước `create_shopping_list` đã commit thành công nhưng bước `get_shopping_list` hoặc bước gọi model kế tiếp gặp lỗi, Backend giữ nguyên ID danh sách đã tạo và trạng thái tạo thành công, báo kết quả một phần (`PARTIAL_FAILURE`) theo từng bước và tuyệt đối không chạy lại lệnh tạo để tránh tạo trùng danh sách.
