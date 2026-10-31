# Tạo danh sách mua sắm rỗng FR-53.9

Nội dung PILOT dẫn xuất từ FR-53.9 và ShoppingListService.createEmpty, chưa có xác nhận review của con người. Đây là hướng dẫn nghiệp vụ, không phải schema tool; code sở hữu schema tham số.

## Thông tin bắt buộc và quyền

Member đăng nhập có thể tạo Shopping List rỗng; CUSTOMER và EXPERT đều được phép. Phải có tên danh sách do người dùng cung cấp. Nếu thiếu tên, hỏi muốn đặt tên gì; không tự đặt. Backend chuẩn hóa tên, đổi khoảng trắng NBSP, không nhận rỗng, ký tự điều khiển hoặc tên quá 200 ký tự. Không cần Meal Plan hay Recipe trước khi tạo danh sách MANUAL rỗng; không tự thêm nguyên liệu.

## Luồng tạo rồi đọc lại

Pilot nghiệp vụ G3 dự kiến: làm rõ tên, hiển thị preview và xác nhận trước ghi, gọi tool create_shopping_list tương ứng ShoppingListService.createEmpty với tên hợp lệ. Chỉ sau kết quả tạo thành công mới lấy ID thật Backend trả về và dùng get_shopping_list đọc lại. Không bịa ID, không báo thành công khi Backend lỗi. G2 không cung cấp hai business tools này và không thực sự tạo danh sách; chỉ giải thích quy trình.
