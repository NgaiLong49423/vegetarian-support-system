> **Document:** Product Requirements Document — Mâm Xanh
> **File:** `docs/requirements/PRD.md`
> **Version:** v2.8.0
> **Created:** 2026-06-14
> **Last Updated:** 2026-10-30
> **Status:** Active
> **Related Docs:** `docs/requirements/SRS.md`, `README.md`

# Product Requirements Document — Mâm Xanh

## 1. Mục đích

**Mâm Xanh** (Vegetarian Support System) là nền tảng web ẩm thực chay chuyên nghiệp, đóng vai trò là **sân chơi của các Chuyên gia ẩm thực chay** chia sẻ công thức chuẩn mực, đồng thời phục vụ người ăn chay khám phá món ăn, quản lý lựa chọn cá nhân, lập thực đơn ngày/tuần, xuất file công thức và báo cáo dinh dưỡng, đồng thời nhận hỗ trợ AI có kiểm soát. Tài liệu này mô tả phạm vi sản phẩm cấp cao; [SRS](SRS.md) là nguồn chi tiết cho Functional Requirements, Business Rules và truy vết đề tài.

PRD này tóm tắt Requirements / Implementation Baseline v2.0.0. [SRS](SRS.md) là registry của scope hiện hành; các tài liệu SRS con sở hữu đặc tả chi tiết và Acceptance Criteria. Các tài liệu trong `docs/archive/requirements/v1/` chỉ lưu lịch sử, không xác định scope hiện hành.

## 2. Vấn đề sản phẩm

Người ăn chay thường gặp khó khăn khi tìm kiếm các công thức chuẩn xác, an toàn và cân bằng dinh dưỡng từ nguồn uy tín. Mâm Xanh định vị là **sân chơi chia sẻ công thức của các Chuyên gia ẩm thực chay**, nơi chất lượng bài viết được bảo chứng thông qua quy trình xét duyệt tư cách chuyên môn nghiêm túc, loại bỏ hoàn toàn tình trạng bài rác và công thức thiếu căn cứ.

Giá trị cốt lõi được định vị là **chuyên nghiệp hóa nội dung ẩm thực chay & personalized meal planning**: Chuyên gia sáng tạo và chia sẻ công thức chuẩn mực → Người dùng (Customer) khám phá, bình chọn Like/Dislike, xem tỷ lệ hài lòng % Like, bình luận → Người dùng lưu công thức vào kế hoạch bữa ăn ngày/tuần → Xuất file công thức và báo cáo PDF dinh dưỡng có kèm thực đơn dự kiến. Người dùng thông thường không có quyền tự do đăng bài nhằm bảo vệ tính toàn vẹn chuyên môn của nền tảng, nhưng có thể nộp đơn theo format để được Admin xem xét nâng cấp thành Chuyên gia.

Sản phẩm MVP quản lý nội dung `Recipe Post` với hướng dẫn chuẩn bị/chế biến linh hoạt (người đăng không bắt buộc phải viết từng bước nấu ăn vào bài viết, nhập văn bản hướng dẫn `instructions` 10–5.000 ký tự theo BR-19), chọn 1 thể loại món ăn trong danh mục chuẩn hóa (`dish_category`), hỗ trợ thư viện ảnh tới 5 ảnh (`RECIPE_MEDIA`) với đúng 1 ảnh được chọn làm ảnh bìa (cover), tối đa một YouTube URL; mọi nguyên liệu đều được định lượng bằng số và đơn vị chuẩn có khả năng quy đổi; người dùng có thể khám phá món qua 6 chế độ sắp xếp (Mới nhất, Được yêu thích nhất theo % Like, Xem nhiều nhất, Nhiều bình luận nhất, Hoạt động sôi nổi nhất và Thịnh hành); Customer được đánh giá chất lượng món qua cơ chế Like / Dislike, tính tỷ lệ % hài lòng hiển thị huy hiệu `👍 {like_percentage}%` trước mặt thẻ bài viết (tương tự Samsung Food) và xuất dữ liệu công thức ra file text/PDF; báo cáo dinh dưỡng ngày/tuần xuất PDF kèm thực đơn dự kiến; mô hình Blog độc lập nhúng công thức (Samsung Food style) được phân rã để chuẩn bị cho giai đoạn sau khi các chức năng cốt lõi hoàn thành.

## 3. Người dùng

| Actor | Mục tiêu chính | Ranh giới chính |
|---|---|---|
| Guest | Khám phá nội dung công khai, xem tỷ lệ % Like và tổng số đánh giá, trải nghiệm AI Chatbot cơ bản (có technical rate limit) | Không có dữ liệu cá nhân lưu theo tài khoản; không bình luận, không Like/Dislike, không đăng bài, không lưu công thức; Guest không phải persistent entity |
| Customer (Người dùng thường) | Khám phá, bình chọn Like / Dislike bài công thức, bình luận/reply, lưu công thức (`Saved Recipe`), lập lịch ăn ngày/tuần (`Meal Plan`), so sánh thống kê dinh dưỡng tuần, xuất file công thức và báo cáo PDF dinh dưỡng, nộp đơn đăng ký làm Chuyên gia theo format | **Tuyệt đối không có quyền tạo hoặc công khai bài công thức**; chỉ quản lý dữ liệu cá nhân của mình |
| Chuyên gia ẩm thực chay (`EXPERT`) | Sáng tạo, chỉnh sửa, xóa và công khai trực tiếp các bài công thức chay chuẩn mực; chia sẻ kinh nghiệm ẩm thực chay cho cộng đồng | **Chủ thể duy nhất được quyền đăng bài** khi được Admin cấp role qua duyệt đơn hoặc quản lý trực tiếp; không bắt buộc viết từng bước nấu ăn; tuân thủ quy tắc định lượng số học và tỷ lệ quy đổi gam; không tự Like/Dislike bài của mình |
| Administrator | Quản lý tài khoản thật (tìm kiếm, lọc, phân trang, xem lịch sử, khóa/mở khóa, đổi role có lý do/audit), từ điển nguyên liệu, đơn vị đo lường và bảng quy đổi, xét duyệt đơn (`FR-05`), hậu kiểm báo cáo (`FR-06`) | ADMIN chỉ xem, không bị khóa/đổi role từ trang này; chỉ chuyển CUSTOMER/EXPERT; cấp Expert trực tiếp không cần đơn đã duyệt, đơn PENDING bị kết thúc có lý do và thông báo; xử lý vi phạm ghi trên Report; không sửa bài công thức hoặc quản lý bảng danh mục động |
| Dịch vụ ngoài | Gemini, Azure Blob Storage, YouTube, Google authentication/email và dịch vụ thanh toán hỗ trợ năng lực MVP | Lỗi provider phải được xử lý minh bạch; không giả lập dữ liệu thành kết quả thật. Google Maps không thuộc baseline hiện tại. |

Free, Plus và Pro là hạng gói dịch vụ (Subscription Tier) của Customer/Expert, xác định hạn mức token và model Gemini được chọn; quyền dùng chức năng AI vẫn theo role, ownership, eligibility và quyền nghiệp vụ gốc.

## 4. Năng lực cốt lõi MVP

| Nhóm | Kết quả sản phẩm |
|---|---|
| Khám phá công thức | Tìm/lọc/xem bài công thức công khai của Chuyên gia theo 6 chế độ sắp xếp độc lập (Newest, Most Liked theo tỷ lệ % Like, Most Viewed 24h/7d/30d/all-time, Most Commented, Most Active theo tương tác gần nhất 7 ngày, Trending theo tương tác + độ tươi mới); hiển thị dữ liệu món ăn có cấu trúc (hướng dẫn chế biến chi tiết, thể loại món ăn chuẩn hóa, nguyên liệu định lượng chuẩn, thời gian, khẩu phần), bộ sưu tập tối đa 5 ảnh (`RECIPE_MEDIA`) và 1 video YouTube; hiển thị huy hiệu `👍 {like_percentage}%` nổi bật trên ảnh thẻ món ăn |
| Danh bạ và nguyên liệu | Duyệt/tìm Chuyên gia đang hoạt động, xem thông tin công khai và follow theo FR-59; hiển thị toàn bộ nguyên liệu đang hoạt động dưới dạng nhãn chọn nhiều để mở Khám phá theo các nguyên liệu đã chọn. Không tạo taxonomy món ăn hoặc danh mục nguyên liệu mới. |
| Thẩm định Chuyên gia | Customer nộp đơn theo format văn bản (`EXPERT_APPLICATION`: kinh nghiệm ẩm thực, trường phái chay, tóm tắt công thức và link tham khảo) và có thể đính kèm tài liệu chứng chỉ/văn bằng chuyên môn cùng ảnh thẻ chân dung khung 3:4 riêng cho đơn; Administrator xem hoặc tải tệp để thẩm định rồi phê duyệt hoặc từ chối kèm lý do; khi duyệt, tài khoản được thăng cấp `EXPERT` (`FR-05`). Admin cũng cấp role Expert trực tiếp trong FR-06; đơn PENDING khi có sẽ chuyển CANCELLED và gửi notification. Tệp chỉ Admin truy cập được; ảnh thẻ không cập nhật avatar hồ sơ công khai. |
| Đăng bài Chuyên gia | Chỉ tài khoản có vai trò `Chuyên gia` được cấp qua FR-05 hoặc FR-06 mới được tạo và công khai Recipe Post trực tiếp (hướng dẫn chế biến 10–5.000 ký tự linh hoạt, chọn 1 thể loại món ăn `dish_category`, người đăng không bắt buộc phải viết từng bước nấu ăn, tối đa 5 ảnh với đúng 1 ảnh cover, 1 YouTube URL, nguyên liệu bắt buộc số lượng > 0 và đơn vị có công thức quy đổi hợp lệ); Administrator hậu kiểm sau đăng dựa trên báo cáo vi phạm |
| Cá nhân hóa | Thu thập sở thích/kiêng/dị ứng tối thiểu trước khi gọi AI cá nhân hóa; lưu trực tiếp trong hồ sơ User và User Ingredient Preference |
| AI Assistant | Guest/Member dùng chức năng AI theo quyền nghiệp vụ hiện có; mức token và Gemini model phụ thuộc gói. Có các mode Hỏi đáp/Gợi ý, Có xác nhận, Tự động; mặc định Có xác nhận. RAG MVP dùng dữ liệu Mâm Xanh và tài liệu nội bộ được duyệt. |
| Meal planning & Shopping | Lưu công thức vào Saved Recipe, quản lý lịch ăn ngày/tuần ba bữa và Shopping List; chuẩn hóa đơn vị qua `UNIT` và `INGREDIENT_UNIT_CONVERSION` theo 3 chiều (`MASS`, `VOLUME`, `COUNT`), gom nguyên liệu cùng chiều an toàn; cấm hoàn toàn giá trị phi số học như “vừa đủ”. |
| Xuất file (Export) & Báo cáo | Hỗ trợ Customer xuất dữ liệu công thức chi tiết (.txt/.pdf), báo cáo dinh dưỡng ngày hoặc tuần (.pdf) kèm thực đơn dự kiến, và Shopping List (.txt) để tiện theo dõi, nấu nướng và mua sắm |
| Dinh dưỡng | Tính từ gram nguyên liệu/khẩu phần và danh mục có nguồn USDA/NIH; 100% nguyên liệu công khai đều có đơn vị đo lường và tỷ lệ quy đổi xác định (chặn xuất bản nếu thiếu quy đổi); BMI chỉ tham khảo, không tự kê calorie/macro target từ BMI hoặc weight goal |
| Cộng đồng | Bình luận/reply tối đa 5 cấp, cha bị xóa thành tombstone, lưu công thức (Saved Recipe), báo cáo nội dung; đánh giá chất lượng công thức bằng bình chọn Like / Dislike dành cho Customer/Expert đã đăng nhập (`RECIPE_REACTION`, Guest chỉ có quyền xem); hiển thị tỷ lệ % hài lòng `👍 {like_percentage}%` trên thẻ món ăn (tương tự Samsung Food); lưu vết lượt xem chi tiết có khử trùng lặp (`RECIPE_VIEW`); không áp dụng Like cho bình luận/reply |
| Địa điểm | Không có chức năng tìm hoặc quản lý nhà hàng; Google Maps không phải dependency hiện hành. |
| Gói AI và Credit | Quota pilot Guest 20.000 token/session, Free 50.000/tháng, Plus 300.000/tháng (99.000đ), Pro 600.000/tháng (199.000đ); Free có giá 0đ. Model pilot mặc định: Guest theo Free/Flash-Lite, Plus/Pro chọn Flash-Lite hoặc Flash. Admin cấu hình quota, giá Plus/Pro, Gemini catalog/access/default theo tier và tỷ lệ; mọi model tier có thể đổi theo cấu hình Admin. 1.000đ = 1.000 Credit; 1.000 token overage = 400 Credit; Credit không hết hạn. |

## 5. Ngoài phạm vi MVP

- Bảng quản lý danh mục động (`CATEGORY`, `RECIPE_CATEGORY`); mỗi Recipe Post chọn 1 thể loại món ăn trong danh mục chuẩn hóa (`dish_category`).
- Hướng dẫn chế biến tách thành các bản ghi bước độc lập; hướng dẫn được lưu thành trường văn bản `instructions` trên `Recipe Post`.
- Customer không có quyền tự đăng bài công thức; tài khoản phải được cấp role `EXPERT` qua FR-05 hoặc FR-06.
- Đăng Blog độc lập nhúng Recipe Post và quản lý kho thực phẩm cá nhân.
- Like/Dislike cho bình luận hoặc reply; Like/Dislike hiện chỉ áp dụng cho Recipe Post.
- Lưu vết lịch sử nhiều lần xử lý kiểm duyệt (`moderation_action`) độc lập; kết quả xử lý lưu trực tiếp tại `Report`.
- Daily request quota cho AI và bộ đếm lượt sử dụng theo ngày.
- Mobile app native, GPS/vị trí hiện tại, wearable và health platform.
- Chẩn đoán hoặc tư vấn điều trị; chức năng dinh dưỡng cho nhóm ngoài điều kiện an toàn đã ghi trong SRS.
- AI tạo công thức không có nguồn, AI tự tạo số liệu dinh dưỡng hoặc tự quyết định duyệt/xóa nội dung.
- Upload video trực tiếp, queue món, tìm/đề xuất hoặc quản lý hồ sơ nhà hàng qua Google Maps, chia sẻ Shopping List nhiều tài khoản, đồng bộ realtime, AI tự nhận diện nguyên liệu tương đương hoặc tự quy đổi đơn vị phức tạp.
- Gói Max không giới hạn, nhận diện ảnh nguyên liệu, tóm tắt video và dự báo giá/mùa nguyên liệu.

## 6. Nguyên tắc chất lượng

- Backend thực thi authentication, authorization, validation, ownership và model/quota checks; Frontend không phải lớp bảo vệ duy nhất. Gói AI không cấp thêm quyền nghiệp vụ hoặc khóa riêng capability.
- Secret và credential của Gemini, Azure, Maps hoặc thanh toán không được đưa vào frontend, source code hay Git.
- Kết quả AI và dịch vụ ngoài phải có trạng thái loading/error rõ ràng, có rate limit kỹ thuật chống spam; khi request thất bại do lỗi mạng/provider, hệ thống phản hồi thân thiện và ghi log kỹ thuật.
- Không lưu lịch sử hội thoại AI theo account; telemetry chỉ lưu lượng token tiêu thụ và không giữ raw prompt, lưu trữ trong 90 ngày. Báo cáo kiểm duyệt và quyết định xử lý được lưu trữ trực tiếp trên Report.
- Khi endpoint hoặc hợp đồng API thật tồn tại, OpenAPI phải phản ánh hành vi, quyền và lỗi thực tế; thay đổi hợp đồng, schema hoặc workflow cần được review và cập nhật tài liệu tương ứng.

Ngưỡng nghiệm thu tải là 50 concurrent users; 100 là stretch, còn 500 CCU, 99,5% monthly uptime và 1.000–10.000 users là design goals. Timeout/retry theo provider và coverage gate tiếp tục được cụ thể hóa ở thiết kế/test.
- Luồng demo cốt lõi chạy end-to-end trên dữ liệu kiểm thử và có bằng chứng test.
- Mỗi yêu cầu triển khai truy vết được từ SRS đến Issue, PR, test và phần demo.
- Nhóm 5 thành viên làm việc qua Issue/PR/review; đóng góp không được suy luận chỉ từ Story Points hoặc số commit.
- `main` luôn là bản đã qua release gate; lỗi hoặc giới hạn chưa xử lý được ghi nhận trung thực.

Chỉ số sản phẩm định lượng cho người dùng thật chưa được chốt; không tự đặt KPI tăng trưởng hoặc doanh thu trong giai đoạn này.

## 8. Phụ thuộc và quyết định còn mở

- Công thức/hệ số dinh dưỡng cần nghiên cứu khi triển khai.
- Chi tiết timeout/retry, error mapping và test fixture cho Gemini, payOS và Brevo; các provider này đã được chọn trong Technology Stack.
- Thời lượng access token, claim tối thiểu và vị trí lưu token phía Frontend trong baseline stateless JWT Access Token (đăng xuất phía client, kiểm tra tức thời trạng thái tài khoản `account_status`).
- Frontend state management, CSS/UI library và deployment.

Mọi thay đổi phạm vi phải cập nhật PRD/SRS, ghi lý do trong ADR hoặc Issue quyết định và bổ sung `CHANGELOG.md` khi có ý nghĩa.
