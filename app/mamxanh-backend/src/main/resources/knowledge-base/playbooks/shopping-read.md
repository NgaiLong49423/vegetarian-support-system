---
id: shopping-read
title: Quy trình tra cứu danh sách mua sắm và bảo vệ quyền sở hữu (FR-53.1 / FR-53.2)
category: playbooks
source_requirements:
  - FR-53
  - FR-66
implementation_status: verified
status: APPROVED
---

# Quy trình tra cứu danh sách mua sắm và bảo vệ quyền sở hữu (FR-53.1 / FR-53.2)

Tài liệu này chuẩn hóa quy trình tra cứu danh sách mua sắm và chính sách bảo vệ quyền sở hữu dữ liệu người dùng của Trợ lý AI theo FR-53.1, FR-53.2, FR-66 và `ShoppingListService.get`.

## Đối tượng tra cứu và nguyên tắc định danh thực thể

Công cụ `get_shopping_list` cho phép đọc chi tiết thông tin danh sách mua sắm bằng mã định danh danh sách (`listId` là số nguyên dương hợp lệ).

- **Nguồn gốc mã danh sách:** Trong luồng tạo danh sách mới, `listId` được lấy trực tiếp từ phản hồi thành công của công cụ `create_shopping_list`. Khi người dùng yêu cầu tra cứu một danh sách đã tồn tại từ trước, người dùng cần cung cấp mã danh sách cụ thể.
- **Làm rõ khi thiếu thông tin:** Nếu yêu cầu tra cứu không nêu rõ mã danh sách và ngữ cảnh hội thoại chưa ghi nhận thực thể cụ thể, Trợ lý AI bắt buộc phải hỏi lại để làm rõ; tuyệt đối không tự đoán hoặc tạo ID ngẫu nhiên.

## Kiểm tra quyền hạn và bảo mật quyền sở hữu (Ownership)

- **Quyền hạn đồng nhất:** Thành viên (`CUSTOMER`) và Chuyên gia ẩm thực (`EXPERT`) có quyền hạn đồng nhất đối với các danh sách mua sắm do chính tài khoản của mình tạo và sở hữu.
- **Bảo vệ quyền sở hữu dữ liệu:** Backend kiểm tra trạng thái hoạt động của tài khoản và đối chiếu quyền sở hữu nghiêm ngặt trong `ShoppingListService.get`. Một tài khoản tuyệt đối không thể đọc hay can thiệp vào danh sách mua sắm của tài khoản khác dù cung cấp đúng `listId`. Khi phát hiện truy cập trái phép hoặc không đúng chủ sở hữu, Backend trả về lỗi không tìm thấy và tuyệt đối không tiết lộ bất kỳ thông tin nào về sự tồn tại hay nội dung của danh sách đó.
- **Tính chính xác của dữ liệu:** Kết quả tra cứu trả về phản ánh chính xác trạng thái trong cơ sở dữ liệu gồm mã danh sách (`id`), tên danh sách (`name`), các mốc thời gian và danh sách nguyên liệu (`items`). Trợ lý AI tóm tắt hoặc hiển thị trung thực nội dung này tới người dùng, không tự ý thêm bớt các nguyên liệu không có trong dữ liệu thật.
