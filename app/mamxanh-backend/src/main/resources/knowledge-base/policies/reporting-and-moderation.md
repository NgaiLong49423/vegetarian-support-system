---
id: reporting-and-moderation
title: Báo cáo vi phạm và quy trình xử lý của Quản trị viên
category: policies
audience:
  - customer
  - expert
source_requirements:
  - FR-06
  - FR-26
  - FR-27
  - FR-28
  - FR-29
  - FR-30
  - FR-48
  - BR-23
  - BR-24
  - BR-25
  - BR-26
  - BR-27
  - BR-28
  - BR-29
  - BR-58
source_use_cases:
  - UC-06.1
  - UC-06.2
  - UC-26.1
  - UC-27.1
  - UC-28.1
  - UC-28.2
  - UC-29.1
  - UC-29.2
  - UC-30.1
  - UC-48.1
  - UC-48.2
source_files:
  - app/mamxanh-frontend/src/components/RecipeReportButton.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/admin/controller/RecipeReportController.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/admin/controller/AdminReportController.java
implementation_status: verified
last_verified: 2026-11-01
---

# Báo cáo vi phạm và quy trình xử lý của Quản trị viên

## Chức năng này dùng để làm gì?

Mâm Xanh trao quyền cho cộng đồng cùng tham gia giám sát chất lượng nội dung. Nếu bạn phát hiện một bài công thức hoặc một bình luận có dấu hiệu sai lệch, gây hại hoặc vi phạm quy chuẩn cộng đồng, bạn có thể gửi **Báo cáo vi phạm** để Quản trị viên (Administrator) kịp thời kiểm tra và xử lý.

## Ai có thể gửi báo cáo?

- **Chỉ thành viên đã đăng nhập (Customer hoặc Expert):** Mới có quyền gửi báo cáo.
- **Khách vãng lai (Guest):** Khi bấm nút Báo cáo, hệ thống sẽ yêu cầu bạn đăng nhập để ngăn chặn hành vi gửi báo cáo ẩn danh bừa bãi.

## 6 lý do báo cáo vi phạm bài công thức

Khi bấm nút **Báo cáo vi phạm** trên trang chi tiết công thức, bạn cần chọn 1 trong 6 lý do chuẩn hóa:

1. **Sai thông tin công thức hoặc nhãn ăn chay (`RECIPE_INFO_OR_DIET_LABEL`):** Món ăn gắn nhãn thuần chay nhưng lại chứa sữa/trứng, hoặc ghi sai thành phần nguyên liệu nghiêm trọng.
2. **Nguy cơ an toàn thực phẩm (`FOOD_SAFETY_HAZARD`):** Hướng dẫn kết hợp các nguyên liệu kỵ nhau gây độc tố, hướng dẫn nấu sống các loại đậu/nấm có độc hoặc không đảm bảo vệ sinh.
3. **Spam hoặc quảng cáo không phù hợp (`SPAM_ADVERTISING`):** Bài viết chèn link bán hàng, quảng cáo dịch vụ tài chính, nội dung rác không liên quan đến ẩm thực.
4. **Nội dung xúc phạm hoặc không phù hợp (`INAPPROPRIATE_CONTENT`):** Ngôn từ thô tục, hình ảnh phản cảm, bạo lực hoặc khiêu dâm.
5. **Sao chép hoặc nghi vi phạm bản quyền (`COPYRIGHT_VIOLATION`):** Bài viết sao chép hình ảnh hoặc nội dung độc quyền của tác giả khác mà không có nguồn gốc.
6. **Lý do khác (`OTHER`):** Các vấn đề phát sinh khác. *(Lưu ý: Khi chọn "Lý do khác", bạn bắt buộc phải nhập mô tả chi tiết từ 10 đến 500 ký tự giải thích rõ vấn đề).*

## Quy trình xử lý báo cáo của Quản trị viên

1. **Gửi báo cáo:** Báo cáo sau khi gửi sẽ có trạng thái **Chờ xử lý** (`OPEN`).
2. **Tiếp nhận & Xác minh:** Quản trị viên tiếp nhận và chuyển trạng thái sang **Đang xem xét** (`IN_REVIEW`). Quản trị viên đọc nội dung, đối chiếu bằng chứng độc lập, không dựa vào định kiến cá nhân.
3. **Quyết định hậu kiểm:**
   - **Báo cáo có căn cứ vi phạm:** Quản trị viên ghi rõ lý do và áp dụng biện pháp: gửi cảnh báo yêu cầu tác giả sửa đổi, **tạm ẩn bài viết** khỏi hiển thị công khai, hoặc xóa bài và khóa tài khoản vi phạm nếu nghiêm trọng.
   - **Báo cáo không đủ căn cứ:** Quản trị viên ghi nhận kết luận và đóng báo cáo (`RESOLVED`). Bài viết vẫn tiếp tục được hiển thị công khai bình thường.
4. **Thông báo kết quả:**
   - **Người gửi báo cáo:** Nhận được thông báo trong ứng dụng về kết quả xử lý của báo cáo do mình gửi.
   - **Tác giả bài viết bị báo cáo:** Nếu bài viết bị yêu cầu chỉnh sửa hoặc tạm ẩn, tác giả sẽ nhận được thông báo nêu rõ lý do và nội dung cần khắc phục.

## Những nguyên tắc bảo mật và công bằng quan trọng

1. **Bảo mật tuyệt đối danh tính người gửi báo cáo:**
   - Tác giả bài viết và cộng đồng **tuyệt đối không bao giờ biết ai là người đã gửi báo cáo**. Toàn bộ thông tin định danh của người báo cáo được bảo mật trong trang quản trị nội bộ.
2. **Báo cáo không tự động ẩn bài viết:**
   - Việc một bài viết bị gửi báo cáo (dù có nhiều người cùng báo cáo) **hoàn toàn không làm bài viết tự động bị ẩn hay xóa ngay lập tức**. Quyết định xử lý chỉ được đưa ra sau khi Quản trị viên là con người trực tiếp thẩm định.
3. **Chống gửi báo cáo trùng lặp:**
   - Mỗi tài khoản chỉ được có duy nhất **1 báo cáo đang mở** trên cùng một bài công thức. Nếu bạn có thêm thông tin mới, bạn có thể bổ sung vào báo cáo đang mở đó thay vì tạo báo cáo mới gây nghẽn hệ thống.
4. **Quy tắc phục hồi bài viết bị ẩn:**
   - Khi một bài viết đã bị Quản trị viên tạm ẩn, tác giả sau khi chỉnh sửa xong **không thể tự ý bấm công khai lại**, mà phải thông báo để Quản trị viên kiểm tra lại và duyệt gỡ ẩn.
