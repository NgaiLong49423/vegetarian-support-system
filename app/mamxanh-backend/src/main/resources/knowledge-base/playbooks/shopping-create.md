---
id: shopping-create
title: Quy trình tạo danh sách mua sắm rỗng và quản lý trạng thái (FR-53.9)
category: playbooks
source_requirements:
  - FR-53
  - FR-66
implementation_status: verified
status: APPROVED
---

# Quy trình tạo danh sách mua sắm rỗng và quản lý trạng thái (FR-53.9)

Tài liệu này chuẩn hóa quy trình nghiệp vụ cho phép Trợ lý AI hỗ trợ người dùng tạo danh sách mua sắm rỗng và quản lý trạng thái theo FR-53.9, FR-66 và dịch vụ `ShoppingListService.createEmpty`.

## Thông tin bắt buộc và điều kiện hợp lệ của tên danh sách

Thành viên đang hoạt động (`CUSTOMER` và `EXPERT`) có quyền tạo danh sách mua sắm rỗng mới thuộc loại `MANUAL`.

- **Tên danh sách do người dùng cung cấp:** Tên danh sách bắt buộc phải do người dùng nêu rõ ràng trong yêu cầu. Nếu người dùng yêu cầu tạo danh sách nhưng chưa cung cấp tên hoặc tên không xác định được trong ngữ cảnh, Trợ lý AI bắt buộc phải hỏi lại để làm rõ; tuyệt đối không tự đặt tên mặc định thay cho người dùng.
- **Tiêu chuẩn dữ liệu:** Backend tự động chuẩn hóa khoảng trắng đầu cuối, từ chối các tên rỗng, tên chứa ký tự điều khiển hoặc tên dài vượt quá 200 ký tự.
- **Sàng lọc ý định:** Nếu người dùng chỉ đặt câu hỏi hướng dẫn cách làm, câu mang ý phủ định (ví dụ: "đừng tạo", "không tạo danh sách") hoặc câu giả định, hệ thống không kích hoạt thao tác ghi dữ liệu.

## Quy trình điều phối và thực thi công cụ Backend

Khi người dùng đã cấp consent hợp lệ và cung cấp tên danh sách hợp lệ:

1. **Phân nhánh theo chế độ thực thi:**
   - Ở chế độ `QA`: Hệ thống chỉ hiển thị bản xem trước (preview) tên danh sách dự kiến tạo, không kích hoạt công cụ ghi.
   - Ở chế độ `CONFIRM`: Hệ thống hiển thị bản xem trước và gửi yêu cầu xác nhận (`PendingConfirmation`), dừng lại chờ người dùng duyệt trước khi ghi.
   - Ở chế độ `AUTO` (hoặc sau khi người dùng chấp thuận yêu cầu ở `CONFIRM`): Hệ thống trực tiếp kích hoạt công cụ `create_shopping_list` thông qua `ShoppingListService.createEmpty`.
2. **Phản hồi và xác minh:**
   - Công cụ Backend trả về đầy đủ đối tượng danh sách mua sắm mới gồm mã định danh thật (`id`), tên chuẩn hóa (`name`), loại danh sách và danh sách mục rỗng.
   - Trợ lý AI sử dụng trực tiếp kết quả trả về từ Backend để thông báo hoàn tất cho người dùng.
   - Khi cần kiểm chứng tính toàn vẹn của việc lưu trữ trong cơ sở dữ liệu hoặc khi người dùng yêu cầu xem lại, AI có thể dùng chính mã `id` thật từ Backend để gọi tiếp công cụ đọc `get_shopping_list`. Trợ lý AI tuyệt đối không sử dụng ID suy đoán hoặc ID giả định.
