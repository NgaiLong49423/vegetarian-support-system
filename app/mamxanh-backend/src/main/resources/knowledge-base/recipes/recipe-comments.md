---
id: recipe-comments
title: Thảo luận, bình luận và phản hồi nhiều cấp
category: recipes
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-46
  - BR-66
source_use_cases:
  - UC-46.1
  - UC-46.2
  - UC-46.3
  - UC-46.4
  - UC-46.5
source_files:
  - app/mamxanh-frontend/src/components/RecipeComments.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/controller/RecipeCommentController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Thảo luận, bình luận và phản hồi nhiều cấp

## Chức năng này dùng để làm gì?

Mục bình luận bên dưới mỗi bài công thức là không gian để người nấu giao lưu, đặt câu hỏi cho tác giả Chuyên gia, chia sẻ cảm nhận sau khi nấu thử hoặc góp ý các mẹo biến tấu gia vị độc đáo.

## Ai có thể sử dụng?

- **Khách vãng lai (Guest):** Có quyền đọc toàn bộ các bình luận và phản hồi công khai của mọi người. Để viết bình luận hoặc trả lời, bạn cần đăng nhập tài khoản.
- **Thành viên đã đăng nhập (Customer và Expert):** Có quyền viết bình luận mới, trả lời (reply) bình luận của người khác, và chỉnh sửa hoặc xóa bình luận của chính mình.

## Cách sử dụng

### 1. Viết bình luận gốc (Bình luận cấp 1)
1. Kéo xuống phần **Bình luận** ở cuối trang chi tiết công thức.
2. Nhập nội dung thảo luận vào khung soạn thảo (từ 1 đến 1.000 ký tự).
3. Nhấn nút **Gửi bình luận**. Bình luận của bạn sẽ xuất hiện ngay lập tức kèm tên hiển thị và thời gian đăng.

### 2. Trả lời một bình luận (Reply)
1. Bên dưới bình luận bạn muốn trao đổi, nhấn nút **Trả lời** (`Reply`).
2. Khung soạn thảo phản hồi sẽ mở ra ngay bên dưới.
3. Nhập nội dung câu trả lời và nhấn **Gửi**.
4. Phản hồi của bạn sẽ thụt lề và hiển thị lồng ghép bên dưới bình luận gốc.

### 3. Chỉnh sửa hoặc Xóa bình luận của bạn
- Bên cạnh bình luận do chính bạn viết, bấm vào biểu tượng dấu 3 chấm hoặc nút **Chỉnh sửa / Xóa**:
  - **Chỉnh sửa:** Cập nhật lại câu từ và bấm **Lưu**. Bình luận sẽ hiển thị nhãn "(đã chỉnh sửa)".
  - **Xóa:** Hệ thống sẽ hỏi xác nhận trước khi xóa.

## Những điều cần lưu ý

1. **Hỗ trợ phản hồi lồng tối đa 5 cấp:**
   - Để cuộc thảo luận diễn ra có trật tự và ngữ cảnh rõ ràng, Mâm Xanh hỗ trợ cấu trúc trả lời lồng nhau tối đa **5 cấp**. Khi chuỗi thảo luận đã đạt đến cấp thứ 5, các phản hồi tiếp theo sẽ được chuyển về cùng cấp cuối cùng này để tránh làm vỡ giao diện trên điện thoại.
2. **Cơ chế bảo toàn ngữ cảnh khi bình luận cha bị xóa (Tombstone):**
   - Nếu bạn xóa một bình luận của mình mà bên dưới đã có nhiều người khác trả lời, hệ thống sẽ thay thế nội dung bình luận của bạn bằng nhãn *"Bình luận này đã bị xóa bởi người dùng"* (Tombstone). Điều này giúp giữ nguyên mạch đối thoại của những người trả lời bên dưới mà không làm đứt đoạn câu chuyện.
3. **Không áp dụng Like / Dislike cho bình luận:**
   - Để giữ không khí thảo luận lành mạnh, tập trung vào trao đổi chuyên môn nấu nướng và tránh gây áp lực đánh giá tiêu cực giữa các thành viên, Mâm Xanh **hoàn toàn không hỗ trợ tính năng Like hoặc Dislike cho bình luận và phản hồi**. Tính năng Like/Dislike chỉ áp dụng duy nhất cho bài công thức.
4. **Quy tắc ứng xử và kiểm duyệt:**
   - Vui lòng sử dụng ngôn từ lịch sự, tôn trọng các trường phái ăn chay khác nhau. Nếu phát hiện bình luận xúc phạm, quảng cáo rác hoặc nội dung không phù hợp, các thành viên có thể bấm **Báo cáo** để Quản trị viên can thiệp xử lý.
