> **Document:** Topic 03 Initial Product Direction
> **File:** `docs/requirements/product-direction.md`
> **Version:** v0.18.0
> **Created:** 2026-09-11
> **Last Updated:** 2026-09-13
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
| Administrator | Quản lý thành viên, Recipe Post, video, bình luận và danh mục món ăn; xử lý báo cáo nội dung. |
| Authorized User | Xem và tương tác với Recipe Post, bình luận, tìm kiếm/lọc món ăn-video; tạo menu tuần theo BMI/mục tiêu cá nhân; nhận gợi ý món, thay thế nguyên liệu, nơi ăn chay lân cận; tạo/công khai Recipe Post trực tiếp. |
| Unauthorized User | Tìm kiếm và xem bài viết/video; chatbot trả lời các câu hỏi cơ bản để khuyến khích đăng ký. |

Nguồn còn nêu các khả năng mở rộng dùng AI/tích hợp: nhận diện nguyên liệu từ ảnh, chatbot dinh dưỡng, tóm tắt video thành công thức, đối chiếu bữa ăn với Apple Health/Google Fit/Wearables, và dự báo/đề xuất theo mùa. Đây là **candidate scope**, chưa được coi là yêu cầu MVP.

### Định vị đã chốt

Ứng dụng được định vị là **ứng dụng khám phá món ăn và lập thực đơn chay theo tuần, sử dụng Gemini AI để gợi ý món phù hợp từ các bài công thức đang công khai; người dùng chủ động chọn và thay đổi thực đơn**. Bài công thức và tương tác cộng đồng là nguồn nội dung hỗ trợ. Định hướng này thay thế quyết định community-first trước đây.

Member đã đăng nhập tạo/công khai Recipe Post trực tiếp khi bài hợp lệ, không có đơn xin quyền đăng. Tác giả tạo/xem/sửa/xóa bài của mình như một lớp mạng xã hội thu nhỏ; Administrator không duyệt từng bài mà xử lý nội dung khi có báo cáo. AI là công cụ hỗ trợ, không tự áp dụng chế tài; AI quét/gắn cờ là DEFERRED. Nhóm chưa thêm actor `Expert` ở phiên bản đầu.

Người dùng chỉ có một loại bài để tạo là **bài công thức (`Recipe Post`)**. Đây chính là phạm vi Blog của ứng dụng; không có Blog tổng quát, bài viết tự do hoặc website chi tiết thứ hai. Hướng dẫn nấu từng bước thuộc bài công thức nhưng không bắt buộc.

## 2. Hướng đề xuất cho mục tiêu đầu tiên

### Mục tiêu 1 — Xác thực giá trị cốt lõi

Xây dựng một luồng hoàn chỉnh cho người dùng đã đăng nhập:

> **Khai báo sở thích → tìm/lọc món chay → AI gợi ý từ bài công thức đang công khai → người dùng chọn và lưu món vào thực đơn tuần → AI gợi ý thay thế khi cần.**

Tên ngắn: **Personalized vegetarian meal planning**.

### Vì sao chọn hướng này

- Nó trực tiếp phục vụ lý do người dùng chọn ứng dụng: quyết định “ăn gì” và lập kế hoạch bữa ăn chay.
- Nó tận dụng các chức năng đã ghi trong đề tài: công thức, danh mục, tìm/lọc, BMI/mục tiêu và menu tuần.
- Nó tạo được một luồng demo rõ ràng, liên kết Gemini với dữ liệu món có sẵn và quyết định của người dùng.
- Nội dung cộng đồng (bài công thức/video nhúng/bình luận) và quản trị vẫn quan trọng, nhưng là lớp hỗ trợ; nếu làm trước sẽ chưa chứng minh được giá trị “hỗ trợ ăn chay cá nhân hóa”.

## 3. Phạm vi tối thiểu của Mục tiêu 1

### Bao gồm

1. Người dùng đăng ký/đăng nhập ở mức cơ bản.
2. Onboarding Questionnaire để khai báo sở thích ăn uống; Hồ sơ nhu cầu dinh dưỡng cho xem BMI tham khảo và mức calorie/dưỡng chất ước tính từ nhiều yếu tố cá nhân, không chỉ BMI.
3. Danh mục và các Recipe Post chay đang công khai do Administrator hoặc Member đã đăng nhập tạo.
4. Tìm kiếm/lọc bài công thức theo loại món, nguyên liệu và một số điều kiện phù hợp hồ sơ.
5. Trang chi tiết bài công thức hiển thị nguyên liệu và thông tin dinh dưỡng **nếu có dữ liệu nguồn đáng tin cậy**; hướng dẫn từng bước chỉ hiển thị khi tác giả cung cấp.
6. Người dùng lưu/bỏ lưu bài công thức để xem lại; việc này độc lập với hồ sơ sở thích và không gọi AI.
7. Người dùng thêm, chuyển, thay hoặc xóa món trong Bữa sáng, Bữa trưa hoặc Bữa tối của từng ngày trong lịch ăn tuần. Một bữa được có nhiều món nhưng không có cùng công thức trùng trong cùng ngày/bữa. MVP không có Queue, Bữa phụ hoặc loại bữa tùy chỉnh.
8. Gemini chỉ gợi ý và sắp menu từ bài công thức đang công khai, không bị ẩn/xóa, dựa trên hồ sơ cùng nguyên liệu người dùng cho biết đang có; mỗi kết quả dẫn tới bài nguồn. AI không tạo công thức mới. Người dùng quyết định lưu/thay món; nếu không có lựa chọn phù hợp, hệ thống thông báo rõ.
9. Với Member đủ 18 tuổi, không mang thai/cho con bú và không cần chế độ ăn điều trị, AI menu dinh dưỡng chỉ dùng công thức có dữ liệu đáng tin cậy. Người dùng có thể kiểm tra một ngày theo mức tham khảo, tổng, chênh lệch và trạng thái; thiếu dữ liệu phải được nói rõ và không được diễn đạt thành chẩn đoán. Người ngoài phạm vi vẫn dùng các chức năng không-dinh-dưỡng.
10. Member đã đăng nhập tạo/xem/sửa/xóa và công khai Recipe Post trực tiếp khi bài hợp lệ, không qua hàng đợi duyệt riêng. Member đã đăng nhập được Like/Unlike, tối đa một Like trên mỗi bài; bình luận và reply phải hỗ trợ lồng nhiều cấp. MVP không có đánh giá 1–5 sao. Người dùng có thể báo cáo bài và Admin hậu kiểm/xử lý.
11. NEEDS CLARIFICATION — Chức năng tìm/gợi ý nhà hàng chay sẽ được nhóm hỏi giảng viên trước khi chốt actor, nguồn dữ liệu, đầu vào/đầu ra và tiêu chí nghiệm thu.

### Chưa bao gồm

- Nhận diện nguyên liệu từ ảnh (Computer Vision).
- Tư vấn dinh dưỡng chuyên nghiệp hoặc tự động tạo kế hoạch điều trị.
- Tóm tắt video thành công thức.
- Apple Health, Google Fit, wearable, đồng bộ calories hoặc cảnh báo sức khỏe.
- Lấy vị trí hiện tại bằng GPS/trình duyệt; tìm nhà hàng quanh địa chỉ nhập đã thuộc phạm vi.
- Dự báo món theo mùa/giá nguyên liệu.
- AI moderation không bắt buộc MVP; chỉ làm nếu core hoàn thành sớm. Các khả năng ngoài phạm vi đã chốt không tự thêm.

Các phần chưa bao gồm chỉ được đưa vào backlog sau khi nhóm xác nhận Mục tiêu 1 và đánh giá thời gian/rủi ro.

## 4. Tiêu chí hoàn thành đề xuất cho Mục tiêu 1

Một bản demo được xem là đạt khi có thể chứng minh bằng dữ liệu mẫu:

1. Administrator tạo bài mẫu; Member đã đăng nhập tạo/công khai Recipe Post trực tiếp và gán danh mục/nhãn phù hợp.
2. Người dùng có hồ sơ ăn chay và mục tiêu cá nhân.
3. Người dùng tìm được bài công thức phù hợp bằng tìm kiếm hoặc bộ lọc.
4. Người dùng xem được bài công thức, lưu vào danh sách Đã lưu hoặc thêm vào một ngày/bữa, kể cả khi tác giả không cung cấp hướng dẫn từng bước.
5. Danh sách Đã lưu không có công thức trùng và độc lập với lịch ăn.
6. Lịch ăn tuần hiển thị đúng món đã chọn và cho phép chuyển, xóa hoặc thay món.
7. Gợi ý Gemini dẫn về món thực sự đang công khai, không bị ẩn/xóa; người dùng chọn thay món và lịch ăn được lưu đúng. Kết quả vi phạm ràng buộc không được đưa vào danh sách đề xuất.

## 5. Phạm vi đã xác nhận qua Q01–Q38

[SRS mục 3.20](SRS.md) là bản tổng hợp quyết định ngày 12/09/2026: web responsive tiếng Việt; Guest/Member/Admin; bốn loại ăn chay; email/password và Google Login; Like/báo cáo bài và bình luận, reply nhiều cấp; thông báo trong app và email. Free/Plus/Pro cùng chức năng AI với 5/15/50 lượt/ngày, thanh toán thật. AI chọn công thức sẵn có, không tạo công thức mới; menu tuần do Member xác nhận.

Tìm/gợi ý nhà hàng chay đang chờ giảng viên xác nhận. Google Maps, danh sách/bản đồ, địa chỉ, khoảng cách, quota và liên kết với món đã tìm chỉ là phương án cũ, không phải phạm vi đã chốt.

Q31 chỉ ghi nguồn dinh dưỡng trong SRS ở thời điểm này. Nghiên cứu/chốt công thức, khoảng tham khảo và kiểm thử khi triển khai; không dùng Gemini tạo số liệu. Dữ liệu demo dự kiến khoảng 60 nguyên liệu có nguồn, Admin quản lý và hệ thống tính lại dữ liệu liên quan khi cập nhật.

## 6. Bước tiếp theo

Viết User Stories, Use Cases và Acceptance Criteria từ chức năng đã xác nhận, rồi mới thiết kế ERD/API/Test Cases. Giá/cổng/chính sách thanh toán, phương tiện tính đường và thông số kỹ thuật còn TBD; không mở lại câu hỏi phạm vi đã chốt.