---
id: ai-chat-cooking-guidance
title: Hỏi đáp ẩm thực chay và công thức cùng AI
category: ai-features
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-34
  - FR-51
  - BR-09
  - BR-38
  - BR-41
source_use_cases:
  - UC-34.1
  - UC-34.4
  - UC-51.1
  - UC-51.2
source_files:
  - app/mamxanh-frontend/src/pages/AiChatPage.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/service/AiChatService.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/service/AiCookingGuideService.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/controller/AiChatController.java
implementation_status: verified
last_verified: 2026-11-21
---

# Hỏi đáp ẩm thực chay và công thức cùng AI

## Trợ lý có thể hỗ trợ gì?

Bạn có thể hỏi bằng ngôn ngữ tự nhiên về cách nấu món chay, sơ chế nguyên liệu, thay đổi kỹ thuật nấu và cách dùng Mâm Xanh. Khi đang xem một công thức công khai, bạn có thể hỏi về chính bài viết đó để trợ lý dùng tên món, nguyên liệu và hướng dẫn nguồn làm ngữ cảnh.

Khi bạn muốn tìm món có thể nấu từ nguyên liệu sẵn có, Trợ lý có thể tìm và gợi ý các công thức công khai trên Mâm Xanh. Kết quả cần dẫn tới bài công thức thật trong hệ thống. Nếu không tìm thấy món phù hợp, Trợ lý phải nói rõ; không được bịa tên món hoặc khẳng định công thức do AI tự tạo là bài đã đăng trên Mâm Xanh.

## Câu hỏi mẫu

- “Mình có đậu phụ, nấm rơm và cà chua; trên Mâm Xanh có món nào dùng các nguyên liệu này không?”
- “Trong công thức này, bước xào nấm cần lưu ý điều gì?”
- “Mình dị ứng đậu phộng; công thức nào không có nguyên liệu này?”
- “Chỉ mình cách sơ chế nấm đùi gà để xào ít ra nước.”

## Ranh giới với dữ liệu hồ sơ và dị ứng

Trợ lý chỉ biết dị ứng, loại ăn chay và sở thích nếu các dữ liệu đó đã được khai báo hoặc cung cấp trong yêu cầu. Không được xem ô trống là xác nhận “không dị ứng”. Với bất kỳ dị ứng nào, hãy đọc lại thành phần công thức trước khi nấu; dữ liệu trong bài không bảo đảm kiểm soát nhiễm chéo.

## Hạn mức và lịch sử hội thoại

Quota, model và tốc độ sử dụng AI tùy cấu hình hiện hành của tài khoản và hệ thống. Hãy xem trang Gói AI/tài khoản để biết thông tin cụ thể; tài liệu này không cố định số token hoặc số câu hỏi.

Ngữ cảnh hội thoại được quản lý theo phiên. Trợ lý không nên hứa giữ toàn bộ lịch sử vô thời hạn hoặc đưa ra số lượt ghi nhớ cố định nếu thông tin đó chưa được xác nhận trong giao diện.

## An toàn dinh dưỡng

Trợ lý có thể chia sẻ kiến thức nấu ăn và dinh dưỡng phổ thông. Trợ lý không chẩn đoán, kê đơn hoặc lập chế độ ăn điều trị bệnh. Nếu câu hỏi cần đánh giá y khoa, hãy trao đổi với bác sĩ hoặc chuyên gia dinh dưỡng y tế.
