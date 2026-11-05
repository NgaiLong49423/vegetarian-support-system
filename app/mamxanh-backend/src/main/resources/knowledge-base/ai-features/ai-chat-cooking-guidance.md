---
id: ai-chat-cooking-guidance
title: Hỏi đáp ẩm thực chay và tư vấn nấu nướng cùng AI
category: ai-features
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-51
  - BR-38
  - BR-09
source_use_cases:
  - UC-51.1
  - UC-51.2
source_files:
  - app/mamxanh-frontend/src/pages/AiChatPage.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/service/AiCookingGuideService.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/controller/AiChatController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Hỏi đáp ẩm thực chay và tư vấn nấu nướng cùng AI

## Chức năng này dùng để làm gì?

Khung chat thông minh của Mâm Xanh là trợ thủ nhà bếp 24/7 của bạn. Bạn có thể trò chuyện tự nhiên bằng tiếng Việt để hỏi bất kỳ điều gì liên quan đến nấu ăn chay: từ cách khử mùi hôi của nấm, cách ướp đậu hũ thấm vị, cho đến việc tìm kiếm món chay thanh đạm đãi tiệc gia đình.

Hệ thống hỗ trợ 2 ngữ cảnh hỏi đáp chính:
1. **Hỏi đáp chung (General Chat):** Thảo luận về kiến thức ẩm thực, kỹ thuật nấu ăn và tìm kiếm món chay trên toàn nền tảng.
2. **Hỏi đáp theo bài viết đang xem (Recipe-Context Chat):** Tư vấn chuyên sâu ngay trên công thức của Chuyên gia mà bạn đang mở trên màn hình.

## Ai có thể sử dụng?

- **Khách vãng lai (Guest):** Trải nghiệm hỏi đáp tự do trong hạn mức 20.000 token của phiên.
- **Thành viên đã đăng ký (Customer và Expert):** Trò chuyện không giới hạn số câu hỏi, áp dụng hạn mức token theo gói dịch vụ tháng và được ghi nhận sở thích ăn uống cá nhân vào câu trả lời.

## Các tình huống ứng dụng thực tế

### 1. Tìm kiếm và gợi ý món ăn
- *"Tủ lạnh nhà mình có đậu phụ, nấm rơm và cà chua thì nấu được món chay gì ngon?"*
- *"Gợi ý cho mình 3 món canh chay thanh mát cho ngày hè oi bức."*
- *"Mình muốn tìm các món chay chế biến nhanh dưới 20 phút cho bữa sáng."*
$\rightarrow$ AI sẽ phân tích yêu cầu, tra cứu trong kho công thức công khai của Mâm Xanh và gửi kèm thẻ món ăn thực tế để bạn bấm vào xem cách nấu ngay.

### 2. Thay thế và gia giảm nguyên liệu
- *"Món kho này có thể thay đường tinh luyện bằng đường phèn hoặc táo đỏ được không?"*
- *"Mình dị ứng đậu phộng thì nên dùng hạt gì để tạo độ béo ngậy cho món cà ri chay?"*
- *"Công thức gốc nấu cho 4 người ăn, nếu mình nấu cho 10 người thì liều lượng gia vị nên gia giảm thế nào?"*

### 3. Kỹ thuật chế biến và xử lý nguyên liệu
- *"Làm sao để chiên đậu hũ vàng giòn bên ngoài nhưng bên trong vẫn mềm mịn và không bị ngập dầu?"*
- *"Cách sơ chế nấm đùi gà để không bị ra nước khi xào?"*
- *"Nấu nước dùng phở chay từ những loại củ quả nào để có vị ngọt thanh tự nhiên nhất?"*

### 4. Hỏi đáp theo bài viết cụ thể
- Khi bạn đang ở trang bài viết `/cong-thuc/:slug`, bấm vào nút **Hỏi AI về món này**.
- Cửa sổ chat sẽ tự động tải dữ liệu của món ăn đó (nguyên liệu, thời gian, hướng dẫn của tác giả) làm ngữ cảnh. Mọi câu hỏi của bạn sẽ được AI đối chiếu trực tiếp với bài viết để trả lời chuẩn xác nhất.

## Những điều cần lưu ý

1. **AI chỉ soạn công thức nháp trong chat khi bạn yêu cầu rõ:**
   - Nếu bạn nhờ AI *"Hãy sáng tạo một công thức sốt chấm chay mới cho riêng tôi"*, AI sẽ trả lời dưới dạng nội dung văn bản trong khung chat để bạn tham khảo. Nội dung này **hoàn toàn là gợi ý trao đổi trong chat**, không tự động được đăng thành bài viết công khai trên website.
2. **Khi công thức gốc thiếu thông tin:**
   - Nếu một bài công thức của tác giả không ghi rõ thời gian nướng hoặc nhiệt độ lò, AI sẽ trung thực nêu rõ rằng công thức gốc không đề cập điều này trước khi đưa ra mức ước tính tham khảo để bạn thử nghiệm.
3. **Giới hạn số lượt hội thoại:**
   - Để tối ưu hóa hiệu năng, AI ghi nhớ tối đa 8 lượt hỏi đáp gần nhất trong phiên trò chuyện hiện tại để hiểu ngữ cảnh tiếp theo. Khi chuyển sang một chủ đề hoàn toàn mới, bạn nên bấm nút làm mới cuộc trò chuyện.
