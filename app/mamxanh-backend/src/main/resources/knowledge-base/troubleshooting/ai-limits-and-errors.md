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
  - app/mamxanh-backend/src/main/java/tech/mamxanh/subscription/service/AiTokenQuotaService.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java
implementation_status: verified
last_verified: 2026-11-21
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
| **Thông báo đã hết hạn mức AI** | Hạn mức token khả dụng của kỳ hiện tại đã hết và tài khoản không còn Credit khả dụng | Xem thông tin gói, hạn mức và Credit đang hiển thị trong trang Gói AI/tài khoản. Nếu có Credit hoặc quota mới, thử lại sau khi trạng thái được cập nhật. Các con số phụ thuộc cấu hình hiện hành, không nên suy ra từ bảng hạn mức cố định trong tài liệu cũ. |
| **Yêu cầu AI chưa thể tạo thực đơn dinh dưỡng** | Có thể thiếu hồ sơ dinh dưỡng hợp lệ, chưa đủ điều kiện eligibility, không còn công thức phù hợp đủ dữ liệu dinh dưỡng, hết quota hoặc Gemini không khả dụng | Đọc thông báo cụ thể trên trang. Bổ sung hồ sơ/eligibility nếu được yêu cầu; nếu kho không đủ món thì thử tiêu chí khác hoặc tạo thực đơn thủ công; nếu provider lỗi thì thử lại sau. |
| **Trợ lý hỏi về sở thích hoặc dị ứng chưa khai báo** | Trợ lý không thể tự suy ra loại ăn chay, dị ứng hay nguyên liệu cần tránh từ ô trống | Cập nhật thông tin trong **Hồ sơ > Sở thích ăn uống** hoặc nêu ràng buộc trong yêu cầu nếu tính năng cho phép. Với thực đơn dinh dưỡng, các ràng buộc đã khai báo được áp dụng; onboarding đầy đủ không phải điều kiện riêng của FR-36. |
| **Báo lỗi dịch vụ AI không khả dụng** | Gemini hoặc kết nối Backend tới provider có thể gặp lỗi hoặc timeout | Thử lại sau. Không khẳng định request đã được Gemini xử lý thành công nếu ứng dụng trả lỗi. Trạng thái tính quota/credit phụ thuộc điểm lỗi và dữ liệu usage được hệ thống ghi nhận; không thể cam kết mọi lỗi đều miễn phí nếu chưa xem giao dịch usage. |
| **Không thể bấm nút "Đồng ý" trên bản xem trước hành động của AI** | Kế hoạch hành động đã hết hạn hiệu lực (quá thời hạn an toàn 5 phút) | Nhập lại yêu cầu vào khung chat để AI tạo một bản xem trước kế hoạch mới. |
