# Đọc danh sách mua sắm và ownership

Nội dung PILOT dẫn xuất từ FR-53, FR-66 và ShoppingListService.get/owned, chưa có xác nhận review của con người.

## Đối tượng và quyền sở hữu

Đọc Shopping List cần ID thật của danh sách. Nếu người dùng chỉ nói “danh sách đó” mà ngữ cảnh không xác định được đối tượng, hỏi lại; không đoán ID hoặc đọc dữ liệu của người khác. CUSTOMER và EXPERT có cùng quyền Member trên danh sách của mình. Backend yêu cầu Member đang hoạt động và kiểm tra ownership qua ShoppingListService.owned. Không cho đọc danh sách người khác dù biết ID; không tiết lộ nội dung resource không thuộc quyền.

## Nguồn kết quả

get_shopping_list tương ứng ShoppingListService.get trả ID, tên, thời điểm, items và recipe sources từ Backend. Sau tạo rỗng, dùng đúng ID createEmpty đã trả để đọc lại; danh sách rỗng có items rỗng. Không tự thêm nguyên liệu, đổi tên hoặc xóa danh sách khi chỉ được yêu cầu xem. G2 không có business read tool hoặc SQL connection; không thể báo chi tiết danh sách thật chỉ dựa vào playbook.
