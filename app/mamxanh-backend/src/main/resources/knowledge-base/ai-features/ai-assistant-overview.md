---
id: ai-assistant-overview
title: Tổng quan về Trợ lý AI Mâm Xanh
category: ai-features
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-02
  - FR-51
  - FR-66
  - BR-01
  - BR-03
  - BR-04
  - BR-06
  - BR-09
source_use_cases:
  - UC-02.1
  - UC-51.1
  - UC-51.2
source_files:
  - app/mamxanh-frontend/src/pages/AiChatPage.tsx
  - app/mamxanh-frontend/src/components/AiChatHost.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/service/AiChatService.java
implementation_status: verified
last_verified: 2026-11-01
---

# Tổng quan về Trợ lý AI Mâm Xanh

## Trợ lý AI Mâm Xanh là gì?

Trợ lý ảo AI của Mâm Xanh là tính năng thông minh được xây dựng nhằm đồng hành cùng bạn trong hành trình ẩm thực chay. Được tích hợp công nghệ mô hình ngôn ngữ lớn tiên tiến (Google Gemini) kết hợp cùng kho tri thức nội bộ được kiểm duyệt của Mâm Xanh, AI có khả năng giải đáp thắc mắc, tư vấn công thức nấu nướng, gợi ý món ăn theo khẩu vị và hỗ trợ điều phối các tính năng trên website.

Bạn có thể kích hoạt Trợ lý AI bất cứ lúc nào bằng cách nhấp vào biểu tượng **Trợ lý AI** nổi ở góc dưới bên phải màn hình hoặc truy cập trang `/hoi-dap-ai`.

## Những khả năng chính của AI

1. **Hỏi đáp ẩm thực chay chuyên sâu:**
   - Hướng dẫn cách sơ chế các loại nấm, đậu hạt để giữ trọn vị ngọt tự nhiên.
   - Gợi ý các phương pháp thay thế gia vị và nguyên liệu mặn sang chay (ví dụ: thay nước mắm bằng nước tương hoặc cốt dứa lên men, thay gelatin bằng bột rau câu agar).
   - Tư vấn kỹ thuật nấu các món kho, món xào, nước dùng lẩu thanh đạm.
2. **Khám phá và gợi ý món ăn có thật:**
   - Gợi ý các bài công thức đang có trên Mâm Xanh phù hợp với thời gian nấu hoặc nguyên liệu bạn đang có sẵn trong tủ lạnh.
   - Đưa ra đường dẫn và thẻ món ăn trực tiếp để bạn bấm vào xem chi tiết bài viết của Chuyên gia.
3. **Tư vấn theo bài công thức bạn đang xem:**
   - Khi bạn mở chat ngay trên một bài viết cụ thể, AI sẽ tự động đọc ngữ cảnh của bài đó để trả lời câu hỏi: *"Món này có thể thay nấm hương bằng nấm đùi gà được không?", "Nấu cho 8 người thì cần bao nhiêu gam nguyên liệu?"*...
4. **Hỗ trợ điều phối thao tác trên hệ thống:**
   - Điều hướng bạn đến các trang chức năng (như trang Lập thực đơn, Đi chợ, Cài đặt sở thích).
   - Đọc và hỗ trợ cập nhật danh sách mua sắm hoặc tùy chọn ăn uống khi có sự đồng ý của bạn.
5. **Hỗ trợ Chuyên gia biên soạn công thức:**
   - Giúp Chuyên gia viết đoạn giới thiệu truyền cảm hứng hoặc gợi ý khung hướng dẫn chế biến chi tiết.

## Các nguyên tắc an toàn cốt lõi của AI

Để bảo vệ người dùng, Trợ lý AI Mâm Xanh tuân thủ nghiêm ngặt các nguyên tắc sau:
1. **Không bịa đặt công thức không có thật:**
   - Khi bạn hỏi tìm món ăn trên Mâm Xanh, AI luôn ưu tiên chọn lọc từ các **Recipe Post công khai có thật** trên hệ thống do Chuyên gia sáng tác. AI không bao giờ tự ý bịa ra một món ăn giả rồi khẳng định rằng món đó đã được đăng trên nền tảng.
   - Nếu trong hệ thống chưa có món phù hợp với tiêu chí của bạn, AI sẽ thành thật thông báo và gợi ý các món tương tự gần nhất.
2. **Tuyệt đối không đưa ra chẩn đoán y tế hoặc tư vấn điều trị:**
   - AI chỉ cung cấp thông tin dinh dưỡng và mẹo nấu ăn phổ thông. AI **không phải là bác sĩ hay chuyên gia y tế**, không thể chẩn đoán bệnh, không kê đơn thực phẩm chữa bệnh hay lập chế độ ăn điều trị bệnh lý (như tiểu đường, suy thận, ung thư...). Khi gặp các câu hỏi dạng này, AI sẽ lịch sự từ chối và khuyên bạn tham vấn bác sĩ.
3. **Từ chối các nội dung độc hại, vi phạm đạo đức:**
   - Hệ thống được trang bị bộ lọc an toàn để từ chối các yêu cầu chứa nội dung bạo lực, xúc phạm, thông tin sai lệch hoặc cố tình phá vỡ quy chuẩn an toàn của hệ thống.
4. **Bảo vệ quyền riêng tư người dùng:**
   - Hệ thống không lưu trữ lịch sử câu hỏi riêng tư của bạn vĩnh viễn theo tài khoản. Các thông số kỹ thuật (lượng token tiêu thụ) chỉ được lưu ẩn danh phục vụ thống kê trong vòng 90 ngày và không chứa nội dung câu hỏi của bạn.

## Hạn mức sử dụng của từng nhóm người dùng

- **Khách vãng lai (Guest):** Được trải nghiệm AI cơ bản với hạn mức 20.000 token mỗi phiên trình duyệt và giới hạn tối đa 10 câu hỏi mỗi phút để chống spam tự động.
- **Thành viên gói Free:** Có 50.000 token miễn phí mỗi tháng dương lịch, sử dụng mô hình Gemini Flash-Lite tối ưu tốc độ.
- **Thành viên gói Plus & Pro:** Hạn mức 300.000 - 600.000 token/tháng, được quyền lựa chọn mô hình Gemini Flash hoặc Flash-Lite thông minh hơn và có thể nạp thêm AI Credit khi dùng vượt hạn mức.
