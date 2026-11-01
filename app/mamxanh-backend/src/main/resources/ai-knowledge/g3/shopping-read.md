# Đọc danh sách mua sắm và ownership G3

Nội dung PILOT G3 dẫn xuất từ FR-53, FR-66 và ShoppingListService.get/owned, chưa có xác nhận review của con người.

## Đối tượng và quyền sở hữu

Công cụ `get_shopping_list` cần mã danh sách (`listId` là số nguyên dương) thật của danh sách mua sắm thuộc quyền sở hữu của chính người dùng đang đăng nhập. Trong luồng tạo danh sách mới của G3, `listId` được lấy trực tiếp từ kết quả trả về của bước `create_shopping_list` vừa hoàn tất. Nếu người dùng yêu cầu đọc một danh sách sẵn có nhưng không nêu rõ `listId` và ngữ cảnh phiên không xác định được đối tượng, Assistant phải hỏi lại để làm rõ; không tự đoán `listId`.

## Kiểm tra quyền và bảo mật dữ liệu

`CUSTOMER` và `EXPERT` có cùng quyền Member trên các danh sách mua sắm của chính mình. Backend kiểm tra tài khoản đang hoạt động và kiểm tra quyền sở hữu qua `ShoppingListService.get`. Một tài khoản tuyệt đối không đọc được danh sách mua sắm của tài khoản khác dù truyền đúng `listId`; Backend trả lỗi không tìm thấy và không tiết lộ sự tồn tại hay nội dung của danh sách không thuộc quyền sở hữu. Danh sách mới tạo bằng `create_shopping_list` khi đọc lại qua `get_shopping_list` sẽ trả về đúng `id`, `name`, thời điểm tạo/cập nhật và danh sách `items` rỗng.
