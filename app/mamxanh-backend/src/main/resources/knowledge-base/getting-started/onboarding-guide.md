---
id: onboarding-guide
title: Khởi tạo sở thích ăn uống ban đầu (Onboarding)
category: getting-started
audience:
  - customer
  - expert
source_requirements:
  - FR-31
  - BR-30
  - BR-31
source_use_cases:
  - UC-31.1
  - UC-31.2
  - UC-31.3
source_files:
  - app/mamxanh-frontend/src/pages/OnboardingPage.tsx
  - app/mamxanh-frontend/src/pages/DietaryPreferencesPage.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/nutrition/controller/DietaryPreferenceController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Khởi tạo sở thích ăn uống ban đầu (Onboarding)

## Chức năng này dùng để làm gì?

Sau khi đăng ký tài khoản thành công, Mâm Xanh mời bạn hoàn thành bảng khảo sát ban đầu (**Onboarding Questionnaire**). Mục đích là thu thập thông tin về trường phái ăn chay, các nguyên liệu bạn cần kiêng/dị ứng và những món bạn không thích.

Những dữ liệu này giúp hệ thống và Trợ lý AI:
- Gợi ý món ăn chuẩn xác theo đúng chế độ ăn chay của bạn.
- Tự động loại trừ các nguyên liệu có nguy cơ dị ứng khi đề xuất thực đơn.
- Ưu tiên món ăn phù hợp với khẩu vị cá nhân.

## Ai có thể sử dụng?

- Thành viên mới vừa hoàn tất đăng ký tài khoản hoặc đăng nhập Google lần đầu.
- Mọi thành viên đã đăng ký đều có thể xem lại hoặc cập nhật thông tin này bất kỳ lúc nào trong trang **Hồ sơ > Sở thích ăn uống** (`/ho-so/so-thich-an-uong`).

## Các bước thực hiện

### 1. Khi vừa đăng ký tài khoản
1. Sau khi xác minh email hoặc đăng nhập lần đầu, màn hình **Khởi tạo sở thích** (`/khoi-tao-so-thich`) sẽ xuất hiện.
2. **Bước 1: Chọn trường phái ăn chay (bắt buộc):**
   - *Thuần chay (Vegan):* Không sử dụng bất kỳ sản phẩm nào từ động vật (bao gồm sữa, trứng, mật ong).
   - *Chay có sữa (Lacto Vegetarian):* Sử dụng sản phẩm từ sữa, không ăn trứng.
   - *Chay có trứng (Ovo Vegetarian):* Sử dụng trứng, không dùng sản phẩm từ sữa.
   - *Chay có trứng và sữa (Lacto-Ovo Vegetarian):* Sử dụng cả trứng và sữa.
3. **Bước 2: Khai báo nguyên liệu kiêng / dị ứng (bắt buộc):**
   - Chọn các nguyên liệu bạn bắt buộc phải tránh (ví dụ: đậu phộng, mè, đậu nành, gluten...).
   - Nếu bạn không kiêng nguyên liệu nào, hãy chủ động tích chọn **"Tôi không có dị ứng / kiêng ăn"**.
4. **Bước 3: Khai báo món hoặc nguyên liệu không thích (bắt buộc):**
   - Chọn những món/nguyên liệu không hợp khẩu vị của bạn (ví dụ: khổ qua, ngò rí, ớt cay...).
   - Nếu bạn ăn được tất cả các món chay, hãy chủ động chọn **"Tôi không có món nào ghét"**.
5. Nhấn **Hoàn tất** để lưu hồ sơ.

### 2. Có thể bỏ qua Onboarding không?
- Bạn hoàn toàn có thể nhấn nút **"Bỏ qua" (Skip)** ở góc trên để vào ngay trang chủ.
- Khi bạn đã chọn bỏ qua, hệ thống sẽ ghi nhận và **không tự động hiển thị lại lời mời Onboarding** ở các lần đăng nhập tiếp theo.

## Những điều cần lưu ý

1. **Điều kiện để sử dụng AI gợi ý cá nhân hóa:**
   - Nếu bạn bỏ qua Onboarding, bạn vẫn có thể xem công thức, tìm kiếm, tự thêm món vào lịch ăn và trò chuyện hỏi đáp thông thường với AI.
   - Tuy nhiên, trước khi dùng các chức năng **AI gợi ý món theo sở thích** hoặc **AI lập thực đơn tuần cá nhân hóa**, hệ thống bắt buộc bạn phải hoàn thành đầy đủ 3 nhóm thông tin tối thiểu kể trên. Nếu thiếu thông tin, AI sẽ nhắc bạn vào bổ sung sở thích trước khi tiếp tục.
2. **Không mặc định để trống là "Không có":**
   - Để tránh rủi ro dị ứng, hệ thống không tự hiểu việc bỏ trống là bạn không kiêng gì. Bạn phải chủ động xác nhận "Không có" nếu không có mục kiêng.
3. **Cập nhật sở thích bất cứ lúc nào:**
   - Bạn có thể vào **Tài khoản > Cài đặt > Sở thích ăn uống** (`/ho-so/so-thich-an-uong`) để thay đổi lại các lựa chọn này bất cứ khi nào khẩu vị hoặc chế độ ăn của bạn thay đổi.

## Câu hỏi thường gặp

### Khai báo dị ứng ở đây có bảo đảm an toàn 100% khi nấu không?
Hệ thống sử dụng thông tin dị ứng để loại bỏ các công thức có chứa nguyên liệu đó khỏi danh sách AI gợi ý. Tuy nhiên, việc công khai bài viết không phải là bảo đảm y tế tuyệt đối. Bạn luôn cần đọc kỹ danh sách nguyên liệu của từng bài viết trước khi thực hiện để đảm bảo an toàn cho sức khỏe của mình.
