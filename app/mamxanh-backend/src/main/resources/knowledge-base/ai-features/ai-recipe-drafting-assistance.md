---
id: ai-recipe-drafting-assistance
title: Dành cho Chuyên gia: AI hỗ trợ soạn bản nháp công thức
category: ai-features
audience:
  - expert
source_requirements:
  - FR-21
  - BR-15
  - BR-16
source_use_cases:
  - UC-21.1
  - UC-21.2
source_files:
  - app/mamxanh-frontend/src/components/RecipeAiAssistance.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/recipe/service/RecipeAiAssistanceService.java
implementation_status: verified
last_verified: 2026-11-01
---

# Dành cho Chuyên gia: AI hỗ trợ soạn bản nháp công thức

## Chức năng này dùng để làm gì?

Viết một bài công thức ẩm thực hoàn chỉnh đòi hỏi Chuyên gia phải đầu tư nhiều thời gian vào việc gõ câu từ mô tả, sắp xếp bố cục các bước chế biến và tìm cách diễn đạt cuốn hút.

Tính năng **AI Hỗ trợ soạn bài** được tích hợp ngay trong biểu mẫu **Đăng công thức** (`/dang-cong-thuc`) và **Chỉnh sửa công thức** (`/cong-thuc/:recipeId/chinh-sua`). AI đóng vai trò như một "thư ký ẩm thực", hỗ trợ bạn:
- Viết đoạn văn giới thiệu món ăn sinh động, truyền cảm hứng.
- Đề xuất khung dàn ý hướng dẫn chế biến mạch lạc dựa trên danh sách nguyên liệu và thời gian nấu mà bạn vừa nhập.

## Ai có thể sử dụng?

- **Chỉ tài khoản có vai trò Chuyên gia (Expert):** Được cấp quyền sử dụng tính năng này khi soạn bài viết.
- Chức năng sử dụng hạn mức token theo gói dịch vụ (Free/Plus/Pro) của Chuyên gia.

## Cách sử dụng trên giao diện biểu mẫu

### 1. Gợi ý đoạn mô tả giới thiệu món ăn
1. Trong biểu mẫu đăng công thức, sau khi bạn đã nhập Tên món ăn và Thể loại món.
2. Tại mục **Mô tả ngắn**, bấm vào nút **"Nhờ AI viết mô tả"** (có biểu tượng lấp lánh).
3. AI sẽ phân tích tên món, thể loại và tạo ra một đoạn giới thiệu ngắn (dưới 2.000 ký tự) mô tả hương vị, màu sắc và cảm xúc của món ăn.
4. Nội dung được điền trực tiếp vào ô văn bản. Bạn hoàn toàn có thể tự do chỉnh sửa, thêm bớt từ ngữ theo đúng văn phong riêng của mình.

### 2. Gợi ý hướng dẫn chuẩn bị và chế biến
1. Sau khi bạn đã chọn danh sách nguyên liệu định lượng và thời gian chuẩn bị/nấu.
2. Tại mục **Hướng dẫn chế biến**, bấm vào nút **"Nhờ AI gợi ý cách nấu"**.
3. AI sẽ gợi ý các công đoạn logic: từ khâu sơ chế nguyên liệu sạch sẽ, kỹ thuật ướp gia vị, thứ tự cho đồ vào xào/nấu, cho đến mẹo trình bày ra đĩa sao cho bắt mắt.
4. Bạn đọc lại toàn bộ nội dung, chỉnh sửa thời gian nấu hoặc bí quyết nêm nếm đặc trưng của bạn trước khi bấm công khai.

## Những điều cần lưu ý

1. **AI chỉ hỗ trợ soạn nội dung biểu mẫu, TUYỆT ĐỐI KHÔNG TỰ ĐỘNG CÔNG KHAI:**
   - AI chỉ điền văn bản vào ô nhập liệu trên trình duyệt của bạn để bạn xem trước và tinh chỉnh.
   - **Chỉ khi chính Chuyên gia bấm nút "Công khai bài viết"**, bài viết mới được hệ thống kiểm tra và xuất bản. AI không có quyền tự ý xuất bản bài viết của bạn.
2. **Trách nhiệm nội dung thuộc về tác giả:**
   - Dù bạn tự viết 100% hay sử dụng AI hỗ trợ gợi ý ý tưởng, bạn với tư cách là Chuyên gia đứng tên bài viết vẫn là người chịu trách nhiệm cuối cùng về tính chính xác, an toàn và hấp dẫn của món ăn.
3. **Không chặn đăng bài nếu AI lỗi hoặc hết token:**
   - Nếu kết nối AI gặp sự cố kỹ thuật hoặc tài khoản của bạn tạm thời hết hạn mức token trong tháng, bạn **vẫn toàn quyền tự tay viết và công khai bài bình thường** nếu biểu mẫu thỏa mãn đầy đủ các quy định chuẩn hóa của hệ thống.
