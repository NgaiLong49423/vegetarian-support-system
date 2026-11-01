---
id: privacy-and-data-protection
title: Chính sách bảo vệ dữ liệu và quyền riêng tư
category: policies
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-11
  - BR-06
  - BR-18
  - BR-28
  - BR-79
  - NFR-06
  - NFR-07
  - NFR-20
  - NFR-21
  - NFR-22
source_files:
  - docs/requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md
  - docs/requirements/srs/BUSINESS-RULES.md
  - app/mamxanh-backend/src/main/java/tech/mamxanh/auth/security/JwtConfig.java
implementation_status: verified
last_verified: 2026-11-01
---

# Chính sách bảo vệ dữ liệu và quyền riêng tư

## 1. Cam kết quyền riêng tư của Mâm Xanh

Mâm Xanh cam kết tôn trọng và bảo vệ tối đa quyền riêng tư của mọi người dùng. Chúng tôi chỉ thu thập và xử lý những thông tin thực sự cần thiết để cung cấp trải nghiệm ẩm thực chay cá nhân hóa tốt nhất cho bạn, đồng thời áp dụng các tiêu chuẩn an ninh mạng hiện đại để bảo vệ dữ liệu đó.

## 2. Dữ liệu chúng tôi thu thập và cách sử dụng

1. **Thông tin tài khoản cơ bản:**
   - Email, tên hiển thị và mật khẩu đã mã hóa một chiều (BCrypt). Email chỉ dùng để xác thực đăng nhập, khôi phục mật khẩu và gửi thông báo quan trọng khi có sự đồng ý của bạn.
2. **Sở thích ăn uống và Dữ liệu dinh dưỡng cá nhân:**
   - Trường phái ăn chay, danh sách nguyên liệu kiêng/dị ứng, chiều cao, cân nặng, độ tuổi.
   - **Mục đích duy nhất:** Phục vụ việc lọc công thức, đề xuất món ăn an toàn và tính toán nhu cầu calorie tham khảo trên tài khoản của bạn.
   - **Quyền riêng tư:** Dữ liệu này là hoàn toàn riêng tư, **không bao giờ hiển thị công khai** trên trang cá nhân của bạn.
3. **Dữ liệu hoạt động trên nền tảng:**
   - Lịch ăn tuần, danh sách đi chợ, các bài công thức đã lưu. Đây là tài sản cá nhân của bạn và chỉ bạn mới có quyền truy cập hoặc chỉnh sửa.
4. **Thông tin giao dịch:**
   - Khi bạn đăng ký gói Plus/Pro hoặc nạp AI Credit, giao dịch được thực hiện trực tiếp qua cổng thanh toán bảo mật **payOS**. Mâm Xanh **tuyệt đối không lưu trữ số thẻ ngân hàng, mã PIN hay mật khẩu ngân hàng của bạn**.

## 3. Chính sách chia sẻ dữ liệu với mô hình Trợ lý AI (Google Gemini)

Khi bạn sử dụng tính năng Trợ lý AI:
1. **Minh bạch và có sự đồng ý (Data Consent):**
   - Chỉ khi bạn yêu cầu các tính năng cần đến dữ liệu cá nhân (ví dụ: *"Gợi ý món phù hợp với dị ứng của tôi"*), hệ thống mới chuyển dữ liệu liên quan (loại ăn chay, nguyên liệu kiêng) sang mô hình Gemini để xử lý câu trả lời.
   - Bạn có quyền kiểm soát và thu hồi sự đồng ý chia sẻ bất cứ lúc nào trong phiên chat.
2. **Không lưu trữ nội dung câu hỏi vĩnh viễn (Telemetry Privacy):**
   - Mâm Xanh **không lưu trữ vĩnh viễn nội dung tin nhắn hay câu hỏi (Raw Prompt) của bạn theo tài khoản**.
   - Dữ liệu đo lường kỹ thuật (Telemetry) chỉ ghi nhận số lượng token tiêu thụ phục vụ việc tính phí và vận hành hệ thống, được lưu trữ trong tối đa **90 ngày** rồi tự động xóa sạch.

## 4. Bảo vệ an toàn kỹ thuật (Security Standards)

- **Mã hóa đường truyền:** Toàn bộ dữ liệu trao đổi giữa trình duyệt của bạn và máy chủ Mâm Xanh đều được mã hóa bằng giao thức HTTPS/TLS 1.2+ an toàn.
- **Xác thực bảo mật:** Sử dụng mã phiên stateless JWT Bearer token với cơ chế kiểm tra tức thời trạng thái tài khoản.
- **Chống tấn công tự động:** Giới hạn tốc độ gửi yêu cầu (Rate Limiting) và tự động tạm khóa tài khoản 10 phút nếu nhập sai mật khẩu 5 lần liên tiếp.
- **Bảo mật mã khóa AI:** Toàn bộ khóa API của Google Gemini được lưu trữ bí mật tại máy chủ后端, hoàn toàn không xuất hiện trên giao diện hay trình duyệt của người dùng.
