# Consent, session, model và ngân sách

Nội dung PILOT dẫn xuất từ FR-66, BR-03/04, NFR-18/22; chưa được gắn nhãn con người review.

## Dữ liệu và phiên hội thoại

Personal data chỉ gửi Gemini sau consent theo loại dữ liệu, mục đích và bên nhận. Consent có hiệu lực trong hội thoại đến hết session hoặc thu hồi; xin lại khi đổi phạm vi. Session ngắn hạn hết hạn sau 30 phút không hoạt động; Cuộc trò chuyện mới xóa ngữ cảnh, consent và action chờ, không hoàn tác nghiệp vụ đã xong. Không tạo kho hội thoại dài hạn, không ghi raw prompt vào telemetry.

## Model, quota và lỗi

Backend kiểm tra quyền model và ngân sách trước mỗi lần gọi model phục vụ người dùng. Không reserve quota/Credit. Chỉ response sử dụng được với metadata hợp lệ mới trừ token thực tế đúng một lần; metadata thiếu ghi chưa ghi nhận, không giả số liệu. Lỗi/timeout do hệ thống chịu chi phí. Không Automatic Fallback: model thay thế sau lỗi chỉ khi người dùng đồng ý và model được phép theo gói, theo AC-66.8.

## Chi phí vận hành pilot

Indexing và query embedding là chi phí hệ thống, không trừ quota/Credit người dùng; theo dõi riêng operational usage, phân biệt provider metadata và dự toán. G2 dùng câu tổng hợp, tổng live budget tối đa 5 USD. Embedding lỗi dùng keyword_degraded có thông báo suy giảm, không đổi model/provider. Timeout tối đa 10 giây mỗi embedding/model call; toàn run ADK 30 giây, không tính thời gian chờ người dùng. Cache query vector chỉ trong RAM phiên 30 phút, không lưu query vào index. Build và startup không gọi embedding API.
