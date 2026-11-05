---
id: ai-limits-and-errors
title: Xử lý sự cố và các trường hợp AI từ chối trả lời
category: troubleshooting
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-02
  - FR-51
  - BR-01
  - BR-02
  - BR-03
  - BR-04
  - BR-09
  - BR-31
source_files:
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/service/AiChatService.java
  - app/mamxanh-frontend/src/pages/AiChatPage.tsx
implementation_status: verified
last_verified: 2026-11-01
---

# Xử lý sự cố và các trường hợp AI từ chối trả lời

## Vì sao AI từ chối trả lời câu hỏi của bạn?

Trợ lý AI của Mâm Xanh được thiết kế với các quy tắc kiểm soát an toàn nghiêm ngặt. Khi câu hỏi vi phạm các ranh giới hỗ trợ, AI sẽ gửi phản hồi từ chối lịch sự. Dưới đây là các trường hợp điển hình:

### 1. Câu hỏi nằm ngoài phạm vi ẩm thực và chức năng Mâm Xanh (Off-topic)
- **Câu trả lời của AI:** *"Mình là trợ lý của Mâm Xanh, chỉ hỗ trợ về ẩm thực chay, công thức, nguyên liệu, cách chế biến và cách sử dụng các tính năng Mâm Xanh. Câu hỏi này nằm ngoài phạm vi đó nên mình không thể trả lời. Bạn hãy hỏi mình về món chay hoặc nấu ăn nhé."*
- **Nguyên nhân:** Bạn đang hỏi về các chủ đề không liên quan như bóng đá, công nghệ, chính trị, sửa xe, lập trình...
- **Cách xử lý:** Đặt các câu hỏi xoay quanh món chay, kỹ thuật nấu nướng, dinh dưỡng thực vật hoặc các tính năng của Mâm Xanh.

### 2. Yêu cầu chẩn đoán bệnh hoặc tư vấn điều trị y khoa (Medical Safety)
- **Câu trả lời của AI:** *"Mình không thể chẩn đoán, kê điều trị hoặc lập chế độ ăn điều trị bệnh. Bạn hãy trao đổi với bác sĩ hoặc chuyên gia dinh dưỡng y tế. Mình có thể hỗ trợ câu hỏi nấu ăn chay và thông tin dinh dưỡng phổ thông."*
- **Nguyên nhân:** Câu hỏi chứa nội dung hỏi chữa bệnh, trị liệu, hỏi dùng thuốc kết hợp ăn uống hoặc xây dựng phác đồ điều trị bệnh lý nghiêm trọng.
- **Cách xử lý:** Tham khảo ý kiến trực tiếp từ bác sĩ chuyên khoa hoặc chuyên gia dinh dưỡng y tế.

### 3. Nội dung nhạy cảm, độc hại hoặc cố tình phá vỡ quy chuẩn (Safety & Abuse)
- **Câu trả lời của AI:** *"Mình không thể hỗ trợ yêu cầu có nội dung nhạy cảm, gây hại hoặc nhằm vượt qua hay phá hoại giới hạn của Chatbox..."*
- **Nguyên nhân:** Yêu cầu chứa nội dung bạo lực, tự hại, ngôn từ thù địch, hoặc cố tình "jailbreak" hệ thống.

## Bảng tra cứu sự cố kỹ thuật của AI

| Hiện tượng | Nguyên nhân | Cách khắc phục |
|---|---|---|
| **Báo lỗi "Bạn đã dùng hết hạn mức token trong tháng" (Mã lỗi 429)** | Tài khoản gói Free (50.000 token) hoặc Plus/Pro đã tiêu thụ hết toàn bộ hạn mức token của chu kỳ hiện tại | Bạn có thể nạp thêm **AI Credit** tại `/nap-ai-credit` (chỉ từ 10.000đ) để tiếp tục chat ngay, hoặc nâng cấp lên gói cao hơn tại `/goi-ai`. |
| **Khách vãng lai báo lỗi "Bạn thao tác quá nhanh, vui lòng thử lại sau"** | Giới hạn kỹ thuật dành cho Khách (tối đa 10 câu hỏi/phút hoặc hết 20.000 token dùng thử) | Đợi 1-2 phút trước khi gửi câu hỏi tiếp theo, hoặc **Đăng ký tài khoản miễn phí** để nhận ngay 50.000 token mỗi tháng. |
| **AI thông báo: "Vui lòng hoàn thành sở thích ăn uống trước khi dùng tính năng này"** | Bạn đang yêu cầu AI gợi ý món cá nhân hóa nhưng tài khoản chưa khai báo thông tin trường phái ăn chay hoặc dị ứng | Truy cập trang **Hồ sơ > Sở thích ăn uống** (`/ho-so/so-thich-an-uong`) để điền 3 mục bắt buộc, sau đó quay lại yêu cầu AI. |
| **Báo lỗi "Dịch vụ AI tạm thời không khả dụng, vui lòng thử lại sau" (Mã lỗi 502/503)** | Kết nối giữa hệ thống Mâm Xanh và nhà cung cấp Google Gemini bị gián đoạn hoặc timeout | Chờ 1-2 phút và bấm gửi lại câu hỏi. Lượt gọi bị lỗi đường truyền này **hoàn toàn không bị trừ token hay Credit** trong tài khoản của bạn. |
| **Không thể bấm nút "Đồng ý" trên bản xem trước hành động của AI** | Kế hoạch hành động đã hết hạn hiệu lực (quá thời hạn an toàn 5 phút) | Nhập lại yêu cầu vào khung chat để AI tạo một bản xem trước kế hoạch mới. |
