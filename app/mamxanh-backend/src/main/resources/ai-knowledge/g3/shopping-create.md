# Tạo danh sách mua sắm rỗng và đọc lại G3 FR-53.9

Nội dung PILOT G3 dẫn xuất từ FR-53.9 và ShoppingListService.createEmpty, chưa có xác nhận review của con người. Đây là hướng dẫn nghiệp vụ, không phải schema tool; code sở hữu schema tham số.

## Thông tin bắt buộc và điều kiện tên danh sách

Member đang hoạt động (`CUSTOMER` và `EXPERT`) có thể tạo danh sách mua sắm rỗng loại `MANUAL`. Tên danh sách bắt buộc phải do người dùng cung cấp rõ ràng. Nếu người dùng yêu cầu tạo danh sách nhưng chưa nêu tên hoặc tên không xác định được, Assistant phải hỏi lại người dùng muốn đặt tên gì; tuyệt đối không tự đặt tên mặc định. Backend chuẩn hóa khoảng trắng, từ chối tên rỗng, ký tự điều khiển hoặc tên dài quá 200 ký tự. Nếu người dùng chỉ đặt câu hỏi cách làm, câu phủ định (ví dụ nói không tạo, đừng tạo) hoặc câu giả định thì không được thực thi tạo danh sách.

## Luồng thực thi tạo rồi đọc lại bằng ADK

Khi đã có consent hợp lệ và tên danh sách hợp lệ: ở chế độ `QA`, chỉ trình bày preview tên danh sách mà không gọi tool ghi; ở chế độ `CONFIRM`, yêu cầu xác nhận preview trước khi ghi; ở chế độ `AUTO` (hoặc sau khi người dùng chấp thuận xác nhận ở `CONFIRM`), gọi `create_shopping_list` qua `ShoppingListService.createEmpty`. Sau khi `create_shopping_list` trả về kết quả thành công kèm `id` thật của danh sách vừa tạo, ADK dùng chính `id` thật đó để gọi tiếp `get_shopping_list` (`ShoppingListService.get`) nhằm đọc lại và xác nhận danh sách rỗng vừa được lưu trong cơ sở dữ liệu. Chỉ báo cáo thành công khi kết quả từ Backend xác nhận; không bịa ID và không thêm nguyên liệu ngoài phạm vi G3.
