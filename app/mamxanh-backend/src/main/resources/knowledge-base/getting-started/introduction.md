---
id: introduction
title: Giới thiệu nền tảng ẩm thực chay Mâm Xanh
category: getting-started
audience:
  - guest
  - customer
  - expert
source_requirements: []
source_use_cases: []
source_files:
  - docs/requirements/PRD.md
  - docs/requirements/SRS.md
  - app/mamxanh-frontend/src/pages/Home.tsx
  - app/mamxanh-frontend/src/pages/LandingPage.tsx
implementation_status: verified
last_verified: 2026-11-01
---

# Giới thiệu nền tảng ẩm thực chay Mâm Xanh

## Mâm Xanh là gì?

**Mâm Xanh** (Vegetarian Support System) là nền tảng web chuyên nghiệp dành cho cộng đồng ẩm thực chay tại Việt Nam. Không chỉ là một nơi tổng hợp món ăn thông thường, Mâm Xanh được xây dựng như một **sân chơi của các Chuyên gia ẩm thực chay**, nơi chất lượng bài viết được bảo chứng thông qua quy trình xét duyệt nghiêm túc.

Mục tiêu cốt lõi của Mâm Xanh là:
1. **Chia sẻ công thức chuẩn mực:** Các bài công thức do chính Chuyên gia ẩm thực chay biên soạn, có định lượng rõ ràng, hướng dẫn trực quan và tỷ lệ quy đổi nguyên liệu khả toán.
2. **Khám phá và tương tác cộng đồng:** Người dùng có thể tìm kiếm, bình chọn Thích / Không thích (Like / Dislike) để phản ánh độ hài lòng khách quan, bình luận và theo dõi các đầu bếp chay uy tín.
3. **Lập kế hoạch bữa ăn và đi chợ thông minh:** Hỗ trợ lên lịch ăn tuần 3 bữa, theo dõi dinh dưỡng tham khảo và tự động gom danh sách mua sắm nguyên liệu tiện lợi.
4. **Hỗ trợ thông minh từ AI:** Trợ lý ảo AI Mâm Xanh hỗ trợ tìm kiếm món ăn, tư vấn nấu nướng và tối ưu thực đơn dựa trên tri thức ẩm thực nội bộ đã qua kiểm duyệt.

## Ai có thể sử dụng?

- **Khách vãng lai (Guest):** Bất kỳ ai ghé thăm trang web đều có thể tự do khám phá kho công thức chay phong phú, xem đánh giá của cộng đồng và trải nghiệm trợ lý AI cơ bản.
- **Thành viên đã đăng ký (Customer / Registered User):** Có không gian cá nhân để lưu công thức yêu thích, lập thực đơn tuần, tạo danh sách đi chợ, theo dõi dinh dưỡng, bình chọn đánh giá món ăn và trò chuyện với AI theo nhu cầu cá nhân.
- **Chuyên gia ẩm thực chay (Expert):** Những người có kiến thức và kinh nghiệm ẩm thực chay sâu sắc, được ban quản trị xét duyệt để trở thành tác giả công khai công thức trên nền tảng.

## Những giá trị nổi bật của Mâm Xanh

- **Nói "Không" với công thức thiếu căn cứ:** Để bảo vệ trải nghiệm của người nấu, Mâm Xanh không cho phép tài khoản thông thường đăng bài tự do. Chỉ Chuyên gia đã được xác minh mới có quyền đăng công thức công khai.
- **Định lượng số học chuẩn xác:** Mọi dòng nguyên liệu đều được ghi rõ khối lượng (gam, kg), thể tích (ml, thìa) hoặc số đếm (quả, củ). Nền tảng loại bỏ hoàn toàn các hướng dẫn mơ hồ như "nêm vừa đủ" hay "cho một ít".
- **Đánh giá Like / Dislike minh bạch:** Mâm Xanh áp dụng hệ thống bình chọn Thích / Không thích và hiển thị tỷ lệ % hài lòng (ví dụ: `👍 98%`) nổi bật trên thẻ món ăn, giúp bạn dễ dàng nhận biết món ăn được cộng đồng yêu thích nhất.
- **Tôn trọng ranh giới y tế:** Các tính năng dinh dưỡng và AI chỉ đóng vai trò tham khảo cho lối sống lành mạnh, không đưa ra chẩn đoán hay thay thế tư vấn y khoa của bác sĩ.

## Bắt đầu từ đâu?

1. **Khám phá món ngon:** Truy cập ngay trang **Khám phá** (`/kham-pha`) để tìm kiếm món chay theo thể loại, nguyên liệu hoặc chế độ ăn chay.
2. **Tạo tài khoản:** Đăng ký tài khoản miễn phí tại `/dang-ky` để mở khóa tính năng lưu món, lập lịch ăn và đi chợ.
3. **Trò chuyện cùng AI:** Bấm biểu tượng Trợ lý AI ở góc phải màn hình để được gợi ý món ăn phù hợp với sở thích của bạn ngay hôm nay.
