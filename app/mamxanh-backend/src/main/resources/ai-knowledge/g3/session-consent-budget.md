# Consent Shopping List, session, model và ngân sách G3

Nội dung PILOT G3 dẫn xuất từ FR-66, BR-03/04, BR-77 và NFR-18/22; chưa được gắn nhãn con người review.

## Consent dữ liệu Shopping List và phiên hội thoại

Dữ liệu thuộc phạm vi Shopping List (nội dung yêu cầu liên quan, tên danh sách, ID danh sách và kết quả đọc danh sách) yêu cầu consent riêng của người dùng trước khi gửi tới Google Gemini để tìm hướng dẫn hoặc điều phối, bao gồm cả bước nhúng câu hỏi (query embedding) nếu câu hỏi chứa dữ liệu thuộc phạm vi cần consent. Consent tách biệt hoàn toàn với bước xác nhận tạo danh sách (`CONFIRM`). Khi thu hồi consent, hệ thống chặn các lần gửi tiếp theo, hủy ngay pending action đang chờ và xóa ngữ cảnh nhạy cảm khỏi session. Pilot G3 không đọc hồ sơ cá nhân hay dữ liệu sức khỏe. Session hết hạn sau 30 phút không hoạt động; mỗi session cố định một engine và chỉ chạy tối đa một lượt tại một thời điểm. Nếu session đã hết hạn hoặc mất sau khi khởi động lại, Backend từ chối thao tác cũ và không tự khôi phục hay tạo lại từ session ID do client gửi.

## Model, quota, AI Credit và dừng trước bước ghi

Backend kiểm tra tài khoản đang hoạt động, model được phép theo gói và ngân sách trước từng lần gọi Gemini. Snapshot lúc bắt đầu call ghi nhận chu kỳ, hạn mức quota, model và tỷ lệ overage; không tính lại chu kỳ theo thời điểm hoàn tất. Hệ thống dùng quota tháng trước rồi mới trừ AI Credit theo số thập phân chính xác (tỷ lệ pilot 1.000 token = 400 Credit, tức 0,4 Credit/token) trong cùng một giao dịch cơ sở dữ liệu với deduplication theo call ID. Nếu tiêu thụ vượt số dư, hệ thống chỉ trừ tối đa số dư hiện có, phần vượt do hệ thống chịu và số dư không bao giờ âm. Response lỗi hoặc thiếu/sai metadata không bị trừ quota hay Credit. Ngay trước bước ghi `create_shopping_list`, Backend kiểm tra lại ngân sách: nếu cả quota và Credit đều đã cạn thì dừng ngay trước bước ghi; thao tác đang chờ phải được duyệt lại sau khi bổ sung ngân sách.

## Chi phí vận hành và giới hạn thời gian

Indexing và query embedding là chi phí vận hành hệ thống, không trừ quota hay Credit của người dùng. Mỗi lần gọi model có hạn thời gian 10 giây, gồm tối đa một lần retry cùng model khi gặp lỗi 502/503/504; không tự đổi model. Query embedding có timeout 10 giây, không tự retry; lỗi chuyển keyword retrieval có thông báo suy giảm, không đổi embedding model/provider; toàn bộ một lượt chạy ADK tối đa 6 lần gọi model trong thời hạn 30 giây.
