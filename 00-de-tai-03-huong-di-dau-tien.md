> **Document:** Topic 03 Initial Product Direction  
> **File:** `00-de-tai-03-huong-di-dau-tien.md`  
> **Version:** v0.1.0  
> **Created:** 2026-09-11  
> **Last Updated:** 2026-09-11  
> **Status:** Under Review  

# Đề tài 03 — Ứng dụng hỗ trợ người ăn chay

## Trạng thái

- Ngày khởi tạo: 11/09/2026
- Nguồn ghi nhận: ảnh bảng phân công do nhóm cung cấp.
- Trạng thái quyết định: **định hướng khám phá món và lập thực đơn có AI đã chốt ngày 11/09/2026**. Các chi tiết chưa xác nhận tiếp tục là đề xuất.
- Ranh giới: tài liệu này chỉ chốt hướng của mục tiêu đầu tiên; chưa phân rã database, UI, API, backlog hay công nghệ.

## 1. Đề tài đã xác nhận từ nguồn hiện có

**Đề tài số 3: Ứng dụng hỗ trợ người ăn chay.**

Các vai trò xuất hiện trong nguồn:

| Vai trò | Nhu cầu/chức năng được nêu |
| --- | --- |
| Administrator | Quản lý thành viên, bài blog, video, bình luận; quản lý danh mục món ăn; tạo công thức theo loại và theo thành phần; theo dõi/duyệt nội dung bị AI gắn cờ. |
| Authorized User | Xem và tương tác với bài viết, bình luận, tìm kiếm/lọc món ăn-video; tạo menu tuần theo BMI/mục tiêu cá nhân; nhận gợi ý món, thay thế nguyên liệu, nơi ăn chay lân cận; đăng blog/video/bình luận. |
| Unauthorized User | Tìm kiếm và xem bài viết/video; chatbot trả lời các câu hỏi cơ bản để khuyến khích đăng ký. |

Nguồn còn nêu các khả năng mở rộng dùng AI/tích hợp: nhận diện nguyên liệu từ ảnh, chatbot dinh dưỡng, tóm tắt video thành công thức, đối chiếu bữa ăn với Apple Health/Google Fit/Wearables, và dự báo/đề xuất theo mùa. Đây là **candidate scope**, chưa được coi là yêu cầu MVP.

### Định vị đã chốt

Ứng dụng được định vị là **ứng dụng khám phá món ăn và lập thực đơn chay theo tuần, sử dụng Gemini AI để gợi ý món phù hợp từ các Blog công thức đã được kiểm duyệt; người dùng chủ động chọn và thay đổi thực đơn**. Blog và cộng đồng là nguồn nội dung hỗ trợ. Định hướng này thay thế quyết định community-first trước đây.

AI là công cụ hỗ trợ, không thay thế Administrator: AI có thể gợi ý/tìm kiếm và gắn cờ nội dung; Administrator là bên ra quyết định duyệt, ẩn hoặc xử lý nội dung. Nhóm chưa thêm actor `Expert` ở phiên bản đầu.

Người dùng chỉ có một chức năng tạo nội dung là đăng Blog Post. “Chia sẻ công thức” là một dạng nội dung của Blog Post và dùng chung quy trình kiểm duyệt; ứng dụng không xây một luồng đăng/chia sẻ công thức độc lập.

## 2. Hướng đề xuất cho mục tiêu đầu tiên

### Mục tiêu 1 — Xác thực giá trị cốt lõi

Xây dựng một luồng hoàn chỉnh cho người dùng đã đăng nhập:

> **Khai báo sở thích → tìm/lọc món chay → AI gợi ý từ Blog công thức đã duyệt → người dùng chọn và lưu món vào thực đơn tuần → AI gợi ý thay thế khi cần.**

Tên ngắn: **Personalized vegetarian meal planning**.

### Vì sao chọn hướng này

- Nó trực tiếp phục vụ lý do người dùng chọn ứng dụng: quyết định “ăn gì” và lập kế hoạch bữa ăn chay.
- Nó tận dụng các chức năng đã ghi trong đề tài: công thức, danh mục, tìm/lọc, BMI/mục tiêu và menu tuần.
- Nó tạo được một luồng demo rõ ràng, liên kết Gemini với dữ liệu món có sẵn và quyết định của người dùng.
- Nội dung cộng đồng (blog/video/bình luận) và quản trị vẫn quan trọng, nhưng là lớp hỗ trợ; nếu làm trước sẽ chưa chứng minh được giá trị “hỗ trợ ăn chay cá nhân hóa”.

## 3. Phạm vi tối thiểu của Mục tiêu 1

### Bao gồm

1. Người dùng đăng ký/đăng nhập ở mức cơ bản.
2. Onboarding Questionnaire để khai báo sở thích ăn uống; trường cụ thể và chính sách bỏ qua chờ chốt. BMI có trong đề nguồn nên cần xác minh trước khi quyết định giữ/bỏ hoặc thu thập dữ liệu.
3. Danh mục và các Blog Post dạng công thức chay mẫu do Administrator tạo hoặc phê duyệt.
4. Tìm kiếm/lọc Blog Post dạng công thức theo loại món, nguyên liệu và một số điều kiện phù hợp hồ sơ.
5. Trang chi tiết Blog Post công thức hiển thị nguyên liệu, hướng dẫn và thông tin dinh dưỡng **nếu có dữ liệu nguồn đáng tin cậy**.
6. Người dùng thêm/xóa món từ Blog Post công thức vào các ngày/bữa trong thực đơn tuần.
7. Gemini gợi ý món từ nội dung đã duyệt, kèm tham chiếu tới Blog công thức. Hệ thống kiểm tra ràng buộc bắt buộc và kết quả; người dùng quyết định lưu/thay món. Nếu không có lựa chọn phù hợp, thông báo rõ.

### Chưa bao gồm

- Nhận diện nguyên liệu từ ảnh (Computer Vision).
- Tư vấn dinh dưỡng chuyên nghiệp hoặc tự động tạo kế hoạch điều trị.
- Tóm tắt video thành công thức.
- Apple Health, Google Fit, wearable, đồng bộ calories hoặc cảnh báo sức khỏe.
- Gợi ý địa điểm ăn chay theo vị trí.
- Dự báo món theo mùa/giá nguyên liệu.
- AI moderation, recommendation nâng cao, blog/video/bình luận đầy đủ.

Các phần chưa bao gồm chỉ được đưa vào backlog sau khi nhóm xác nhận Mục tiêu 1 và đánh giá thời gian/rủi ro.

## 4. Tiêu chí hoàn thành đề xuất cho Mục tiêu 1

Một bản demo được xem là đạt khi có thể chứng minh bằng dữ liệu mẫu:

1. Administrator tạo/cập nhật hoặc phê duyệt một Blog Post công thức chay và gán danh mục/nhãn phù hợp.
2. Người dùng có hồ sơ ăn chay và mục tiêu cá nhân.
3. Người dùng tìm được Blog Post công thức phù hợp bằng tìm kiếm hoặc bộ lọc.
4. Người dùng xem được Blog Post công thức và thêm món vào một bữa trong tuần.
5. Thực đơn tuần hiển thị lại đúng món đã chọn, và có thể xóa/thay thế món.
6. Gợi ý Gemini dẫn về món thực sự có trong hệ thống và đã duyệt; người dùng chọn thay món và thực đơn được lưu đúng. Kết quả vi phạm ràng buộc không được đưa vào danh sách đề xuất.

## 5. Những quyết định cần nhóm chốt trước khi phân rã tiếp

| Quyết định | Lựa chọn đề xuất | Lý do |
| --- | --- | --- |
| Đối tượng ưu tiên | Người dùng đã đăng nhập, đang cần lên thực đơn chay | Tạo giá trị cốt lõi rõ nhất. |
| Loại ăn chay ở phiên bản đầu | Vegetarian và Vegan; các biến thể khác ghi nhận sau | Giảm độ phức tạp của dữ liệu và quy tắc. |
| Cá nhân hóa | Gemini gợi ý trên dữ liệu đã duyệt, kết hợp kiểm tra ràng buộc | AI gắn với luồng chọn món; không giao toàn bộ kiểm tra cho mô hình. |
| Dữ liệu dinh dưỡng | Dữ liệu mẫu có nguồn/ghi rõ giới hạn | Tránh đưa ra tư vấn sức khỏe thiếu căn cứ. |
| AI | Gợi ý và thay món là một phần luồng lõi đã chốt | Các AI khác không tự động nằm trong MVP. |
| Quyền dùng AI | Guest Trial, Free và các gói nâng cao theo lượt gọi/ngày; xem `01-phan-ra-goi-ai.md` | Tạo ranh giới thương mại minh bạch, đo được usage thực tế, nhưng chưa quyết định giá hay cổng thanh toán. |

## 6. Điểm cần xác minh từ nhóm/giảng viên

- Các mục AI trong ảnh là yêu cầu bắt buộc, hay là đề xuất tính năng để chọn theo năng lực nhóm?
- “Ăn chay” có cần hỗ trợ chế độ cụ thể nào ngoài Vegetarian/Vegan không?
- Dự án phải có mobile app hay web application được chấp nhận?
- Có tiêu chí chấm điểm, công nghệ bắt buộc, mốc tiến độ, hoặc định dạng SRS/User Story chưa?

## Bước tiếp theo sau khi xác nhận

Phân rã actor và quyền trước, sau đó chốt Onboarding Questionnaire, dữ liệu Blog công thức và luồng chọn/lưu/thay món. Mỗi quyết định được người dùng xác nhận riêng trước khi đưa vào SRS. Xem `docs/requirements/actors-and-onboarding-draft.md`.
