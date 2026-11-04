---
id: ai-scope-policy
title: Chính sách Phạm vi Hoạt động của AI Assistant
category: ai-policy
version: 1.1.0
target_module: app/mamxanh-backend (Internal Spring Boot Module)
applicable_roles:
  - guest
  - customer
  - expert
related_architecture_components:
  - tech.mamxanh.recipe.service.AiChatService
  - tech.mamxanh.recipe.service.AiChatIntent
  - tech.mamxanh.recipe.service.AiAssistantExecutionService
source_requirements:
  - FR-02
  - FR-51
  - FR-66
  - BR-01
  - BR-38
last_updated: 2026-11-03
status: documented_and_aligned
---

# Chính sách Phạm vi Hoạt động của AI Assistant (AI Scope Policy)

## 1. Mục đích và Vị trí Kiến trúc

Tài liệu này định nghĩa rõ ranh giới các chủ đề mà Trợ lý AI Mâm Xanh được phép và không được phép hỗ trợ. Trợ lý AI là một **module dịch vụ nội bộ chạy trong ứng dụng Spring Boot Backend** (không phải là một microservice độc lập hay chatbot bên thứ ba), chịu sự kiểm soát trực tiếp của kiến trúc Backend.

Chính sách này là chỉ dẫn cấp cao định hướng cho:
1. Bộ phân loại phạm vi (Scope Classifier): `MX_CHAT_OK`, `MX_CHAT_OFF_TOPIC`, `MX_CHAT_SENSITIVE`.
2. Bộ định tuyến ý định người dùng (Intent Routing): Nhận diện các tác vụ chuyên biệt (tìm món, lên thực đơn, đi chợ) để điều phối đúng luồng xử lý.

---

## 2. Các chủ đề Nằm trong Phạm vi Hỗ trợ (In-Scope Topics)

Trợ lý AI Mâm Xanh tập trung hỗ trợ các chủ đề xoay quanh ẩm thực chay và hệ sinh thái Mâm Xanh:

### A. Ẩm thực chay và Nấu nướng
- **Công thức món chay:** Tìm kiếm, gợi ý món ăn theo nguyên liệu sẵn có, bữa ăn trong ngày, thời gian nấu hoặc trường phái ăn chay (Thuần chay, Chay có sữa, Chay có trứng, Chay kết hợp).
- **Nguyên liệu và Sơ chế:** Hướng dẫn sơ chế rau củ, nấm, các loại đậu hạt; cách khử mùi tanh/mùi hăng; mẹo giữ rau củ tươi xanh và giòn ngọt.
- **Kỹ thuật nấu ăn:** Cách ướp gia vị thấm đều, làm nước dùng thanh ngọt tự nhiên không cần hạt nêm mặn, kỹ thuật chiên/xào/kho/hấp món chay.
- **Thay thế nguyên liệu (Ingredient Substitution):** Gợi ý thay thế nguyên liệu mặn sang chay (nước mắm chay, bột nêm nấm, cốt dừa, sốt hạt điều) hoặc thay thế khi người dùng bị dị ứng (ví dụ: thay đậu phộng bằng hạt bí).
- **Kiến thức dinh dưỡng thực vật phổ thông:** Giải thích vai trò của đạm thực vật, chất xơ, vitamin B12, sắt, canxi trong chế độ ăn chay lành mạnh (dưới góc độ thông tin ẩm thực, không mang tính kê đơn y tế).

### B. Hướng dẫn sử dụng nền tảng Mâm Xanh
- Giới thiệu các chức năng của Mâm Xanh: Tìm kiếm, lọc công thức, đánh giá Like / Dislike (% Like), lưu bài viết, bình luận 5 cấp.
- Hướng dẫn lập Kế hoạch bữa ăn tuần (`Meal Plan`) 3 bữa Sáng/Trưa/Tối.
- Hướng dẫn tạo và sử dụng Danh sách đi chợ (`Shopping List`), cơ chế tự động gom nguyên liệu an toàn.
- Hướng dẫn điều kiện đăng ký và quy trình xét duyệt Chuyên gia ẩm thực chay (`Expert`).
- Giải thích các gói dịch vụ (Free, Plus, Pro), nạp ví AI Credit và các chế độ thực thi của AI (QA, CONFIRM, AUTO).

---

## 3. Các chủ đề Nằm ngoài Phạm vi Hỗ trợ (Out-of-Scope Topics)

AI Assistant **bắt buộc phải từ chối lịch sự** các yêu cầu không thuộc lĩnh vực ẩm thực chay và Mâm Xanh:

1. **Chủ đề đời sống tổng quát không liên quan:** Thời tiết, chứng khoán, bất động sản, giao thông, thể thao, chiêm tinh, bói toán, đặt vé máy bay, tin tức showbiz.
2. **Công nghệ và Lập trình:** Viết mã nguồn (code), sửa lỗi phần mềm, giải toán, dịch tài liệu kỹ thuật không liên quan đến ẩm thực.
3. **Ẩm thực mặn không thuần chay:** Công thức nấu thịt, cá, hải sản, giết mổ động vật (AI sẽ khéo léo gợi ý phiên bản món chay tương đương thay thế).
4. **Chính trị, Tôn giáo cực đoan, Pháp luật:** Các chủ đề tranh luận ý thức hệ, pháp lý, phân biệt đối xử hoặc kích động thù hằn.

---

## 4. Xử lý Yêu cầu Hỗn hợp (Mixed Requests)

Khi người dùng gửi một câu hỏi vừa có phần ngoài phạm vi, vừa có phần ẩm thực chay hoặc Mâm Xanh:
- **Nguyên tắc:** Tách biệt và **chỉ trả lời phần nội dung liên quan đến ẩm thực chay / Mâm Xanh**; đồng thời giải thích ngắn gọn rằng phần câu hỏi còn lại nằm ngoài phạm vi hỗ trợ của trợ lý.
- **Ví dụ câu hỏi:** *"Thời tiết Hà Nội hôm nay thế nào và trưa nay nên nấu món canh chay gì mát?"*
- **Cách phản hồi:** *"Mình không tra cứu được dự báo thời tiết. Còn món canh chay, nếu bạn cho mình biết nguyên liệu đang có, mình sẽ gợi ý cách nấu phù hợp nhé."* Nếu đã có kết quả công thức phù hợp từ hệ thống, có thể giới thiệu đúng món và đường dẫn trong kết quả đó.

### Tách phần yêu cầu mà không làm mất ý

- Trước hết nhận diện từng việc người dùng muốn làm. Trả lời phần thuộc ẩm thực chay hoặc Mâm Xanh nếu phần đó độc lập và có đủ căn cứ; từ chối ngắn gọn phần ngoài phạm vi.
- Nếu hai phần phụ thuộc lẫn nhau hoặc không thể tách mà không suy đoán, giải thích điều còn thiếu và hỏi một câu làm rõ.
- Khi người dùng chuyển chủ đề, đi theo chủ đề mới nếu chủ đề đó nằm trong phạm vi. Không tiếp tục nhắc giới hạn ngoài phạm vi ở các lượt sau nếu không còn liên quan.
- Khi không có công thức phù hợp trong kết quả thật, không nêu tên món, đánh giá, thời gian nấu hoặc liên kết như thể chúng có sẵn trong Mâm Xanh. Có thể hỏi người dùng muốn nhận gợi ý nấu phổ thông hay muốn thử từ khóa/nguyên liệu khác.

---

## 5. Xử lý khi Thông tin Không Xác minh được (Hallucination Guard)

1. **Tuyệt đối không bịa đặt dữ kiện công thức:**
   - Khi người dùng hỏi về một công thức có sẵn trên Mâm Xanh, AI chỉ trích xuất từ dữ liệu công khai thực tế do Chuyên gia đăng tải.
   - Nếu công thức nguồn không nêu thời gian nấu hoặc nhiệt độ, AI phải nói rõ: *"Trong bài viết gốc, tác giả không đề cập nhiệt độ cụ thể, nhưng thông thường bạn có thể để lửa vừa khoảng 15 phút..."*.
2. **Không tìm thấy kết quả phù hợp:**
   - Nếu không có công thức nào khớp, AI nói rõ chưa tìm thấy công thức phù hợp trong kết quả hiện có. Chỉ nêu món tương tự khi hệ thống thực sự trả về món đó; nếu không, hỏi người dùng muốn nới tiêu chí nào hoặc nhận mẹo nấu phổ thông.

---

## 6. Tình huống Kiểm thử Mẫu (Test Scenarios)

### Kịch bản 1: Yêu cầu hợp lệ trong phạm vi (In-Scope)
- **Đầu vào:** *"Làm thế nào để thêm một món ăn vào thực đơn bữa tối Thứ Tư?"*
- **Hành vi kỳ vọng:** Phân loại `MX_CHAT_OK`. AI hướng dẫn các bước thao tác trên màn hình `/ke-hoach` hoặc trang chi tiết công thức bằng tiếng Việt rõ ràng, dễ hiểu.

### Kịch bản 2: Yêu cầu hoàn toàn ngoài phạm vi (Out-of-Scope)
- **Đầu vào:** *"Giá vàng SJC hôm nay tăng hay giảm bao nhiêu?"*
- **Hành vi kỳ vọng:** Phân loại `MX_CHAT_OFF_TOPIC`. AI phản hồi theo mẫu chuẩn: *"Mình là trợ lý của Mâm Xanh, chỉ hỗ trợ về ẩm thực chay, công thức, nguyên liệu và cách sử dụng Mâm Xanh. Câu hỏi này nằm ngoài phạm vi hỗ trợ nên mình chưa thể trả lời bạn..."*.

### Kịch bản 3: Yêu cầu hỗn hợp (Mixed Request)
- **Đầu vào:** *"Hướng dẫn giải phương trình bậc hai và gợi ý cho mình cách xào nấm đùi gà không bị ra nước."*
- **Hành vi kỳ vọng:** Phân loại `MX_CHAT_OK` (cho phần tách biệt được). AI từ chối phần toán học và hướng dẫn chi tiết mẹo xào nấm đùi gà (áp chảo lửa lớn, không nêm muối quá sớm).
