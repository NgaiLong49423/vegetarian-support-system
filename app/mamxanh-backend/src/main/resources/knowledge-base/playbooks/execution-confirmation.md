---
id: execution-confirmation
title: Chế độ thực thi (Execution Modes) và quy trình xác nhận hành động
category: playbooks
source_requirements:
  - FR-66
  - BR-79
implementation_status: verified
status: APPROVED
---

# Chế độ thực thi (Execution Modes) và quy trình xác nhận hành động

Tài liệu này chuẩn hóa cơ chế điều phối chế độ thực thi và quy trình xác nhận hành động của AI Agent Mâm Xanh theo FR-66 và BR-79. Backend luôn giữ quyền kiểm soát tối cao, độc lập xác minh vai trò (role), trạng thái tài khoản hoạt động, quyền sở hữu dữ liệu (ownership), consent người dùng, hạn mức sử dụng và tính hợp lệ của dữ liệu trước mọi thao tác lưu trữ.

## Ba chế độ thực thi của Trợ lý AI

1. **Hỏi đáp / Tư vấn (`QA`):** Chế độ chỉ đọc. AI chỉ tra cứu tri thức, trả lời câu hỏi hướng dẫn hoặc hiển thị bản xem trước (preview) của tham số dự kiến thực hiện. Tuyệt đối không kích hoạt các công cụ làm thay đổi dữ liệu (mutation tools) và không ghi dữ liệu xuống Backend trong chế độ này.
2. **Có xác nhận (`CONFIRM` - Mặc định cho các tác vụ thay đổi dữ liệu):** Khi người dùng cung cấp đầy đủ thông tin hợp lệ để thực hiện một hành động ghi (ví dụ: tạo danh sách mua sắm), hệ thống lập yêu cầu xác nhận chờ (`PendingConfirmation`), hiển thị chi tiết bản xem trước các tham số sẽ thực thi và chờ người dùng duyệt rõ ràng trước khi kích hoạt công cụ ghi Backend.
3. **Tự động (`AUTO`):** Chỉ tự động kích hoạt công cụ ghi khi người dùng đã chủ động bật chế độ AUTO, yêu cầu được nêu rõ ràng kèm đầy đủ tham số hợp lệ, và toàn bộ điều kiện kiểm tra (consent, quota, quyền) đều thỏa mãn. Chế độ `AUTO` tuyệt đối không tự động thực thi các câu mang ý phủ định (ví dụ: "đừng tạo", "không lưu"), câu giả định hoặc câu hỏi thăm dò cách làm.

## Quy tắc hiệu lực xác nhận và xử lý lỗi từng phần

- **Thời hạn xác nhận:** Mỗi yêu cầu xác nhận (`PendingConfirmation`) có thời hạn hiệu lực tối đa 5 phút (theo BR-79), gắn liền với định danh người dùng, phiên hội thoại, lệnh gọi gốc, các tham số đã chuẩn hóa và chế độ thực thi. Nếu người dùng thay đổi tham số hoặc thay đổi chế độ thực thi, yêu cầu xác nhận cũ lập tức bị vô hiệu hóa và không thể tái sử dụng.
- **Xử lý phản hồi công cụ:** Hệ thống ưu tiên sử dụng phản hồi đầy đủ từ công cụ Backend để thông báo kết quả cho người dùng. Thao tác đọc lại sau khi ghi chỉ được thực hiện khi quy trình nghiệp vụ yêu cầu kiểm chứng tính toàn vẹn hoặc người dùng chủ động yêu cầu xem lại.
- **Xử lý lỗi từng phần (Partial Failure):** Đối với các chuỗi thao tác nhiều bước: nếu bước ghi dữ liệu đã commit thành công vào cơ sở dữ liệu nhưng bước tiếp theo gặp sự cố (như lỗi kết nối, timeout hoặc lỗi model), Backend giữ nguyên trạng thái thành công và mã định danh của thực thể đã tạo, ghi nhận trạng thái hoàn tất một phần (`PARTIAL_FAILURE`), thông báo rõ ràng cho người dùng, và tuyệt đối không chạy lại bước ghi trước đó để tránh ghi trùng lặp dữ liệu.

## Giao tiếp với người dùng trong luồng thực thi

- Trước khi xin xác nhận, tóm tắt bằng lời dễ hiểu việc sắp làm và các thông tin quan trọng mà người dùng đã cung cấp. Chỉ hiển thị bản xem trước do hệ thống hỗ trợ; không tự thêm trường hoặc tự sửa giá trị mà chưa được người dùng đồng ý.
- Câu hỏi xác nhận phải nêu rõ hành động và cho người dùng lựa chọn đồng ý hoặc hủy. Không coi sự im lặng, đổi chủ đề, câu hỏi “được không?” hay đồng ý với một phần khác là chấp thuận.
- Nếu dữ liệu bắt buộc còn thiếu, hỏi bổ sung trước khi tạo bản xác nhận. Nếu người dùng sửa tham số hoặc đổi chế độ, giải thích ngắn rằng cần xem lại bản xác nhận mới; không dùng lại xác nhận cũ.
- Sau khi có kết quả, báo đúng trạng thái: đã tạo/cập nhật thành công, chưa thực hiện vì đang chờ xác nhận, đã hủy, thất bại, hoặc hoàn tất một phần. Không dùng mã lỗi hay tên trạng thái nội bộ trong câu trả lời người dùng.
- Khi thất bại, nêu phần người dùng có thể làm tiếp theo nếu hệ thống cung cấp căn cứ; không khẳng định thao tác thất bại nếu Backend đã xác nhận một bước ghi thành công.
