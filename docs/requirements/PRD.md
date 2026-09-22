> **Document:** Product Requirements Document — Mâm Xanh
> **File:** `docs/requirements/PRD.md`
> **Version:** v1.6.0
> **Created:** 2026-06-14
> **Last Updated:** 2026-09-22
> **Status:** Active
> **Related Docs:** `docs/requirements/SRS.md`, `README.md`

# Product Requirements Document — Mâm Xanh

## 1. Mục đích

**Mâm Xanh** (Vegetarian Support System) là nền tảng web ẩm thực chay chuyên nghiệp, đóng vai trò là **sân chơi của các Chuyên gia ẩm thực chay** chia sẻ công thức chuẩn mực, đồng thời phục vụ người ăn chay khám phá món ăn, quản lý lựa chọn cá nhân, lập thực đơn ngày/tuần, xuất file công thức/thực đơn và nhận hỗ trợ AI có kiểm soát. Tài liệu này mô tả phạm vi sản phẩm cấp cao; [SRS](SRS.md) là nguồn chi tiết cho Functional Requirements, Business Rules và truy vết đề tài.

`Active` nghĩa là PRD thuộc Requirements Baseline v1.0.0 đã được chốt ngày 16/09/2026, cập nhật v1.3.0 ngày 18/09/2026, v1.4.0 ngày 22/09/2026, v1.5.0 ngày 22/09/2026 và cập nhật v1.6.0 ngày 22/09/2026 (chuyển đổi cơ chế đánh giá chất lượng bài công thức từ thang điểm 1–5 sao sang hệ thống bình chọn Thích / Không thích (Like / Dislike), tính toán tỷ lệ % hài lòng `👍 {like_percentage}%` trên tổng số lượt bình chọn hiển thị nổi bật trên thẻ món ăn Recipe Card theo phong cách Samsung Food; không áp dụng Like cho bình luận/reply). [SRS](SRS.md) và các tài liệu con sở hữu requirement chi tiết cùng lifecycle chính thức.

## 2. Vấn đề sản phẩm

Người ăn chay thường gặp khó khăn khi tìm kiếm các công thức chuẩn xác, an toàn và cân bằng dinh dưỡng từ nguồn uy tín. Mâm Xanh định vị là **sân chơi chia sẻ công thức của các Chuyên gia ẩm thực chay**, nơi chất lượng bài viết được bảo chứng thông qua quy trình xét duyệt tư cách chuyên môn nghiêm túc, loại bỏ hoàn toàn tình trạng bài rác và công thức thiếu căn cứ.

Giá trị cốt lõi được định vị là **chuyên nghiệp hóa nội dung ẩm thực chay & personalized meal planning**: Chuyên gia sáng tạo và chia sẻ công thức chuẩn mực → Người dùng (Customer) khám phá, bình chọn Like/Dislike, xem tỷ lệ hài lòng % Like, bình luận → Người dùng lưu công thức vào kế hoạch bữa ăn ngày/tuần → Xuất file công thức/thực đơn phục vụ nấu nướng và đi chợ. Người dùng thông thường không có quyền tự do đăng bài nhằm bảo vệ tính toàn vẹn chuyên môn của nền tảng, nhưng có thể nộp đơn theo format để được Admin xem xét nâng cấp thành Chuyên gia.

Sản phẩm MVP quản lý nội dung `Recipe Post` với hướng dẫn chuẩn bị/chế biến linh hoạt (người đăng không bắt buộc phải viết từng bước nấu ăn vào bài viết, nhập văn bản hướng dẫn `instructions` 10–5.000 ký tự theo BR-19), chọn 1 thể loại món ăn trong danh mục chuẩn hóa (`dish_category`), hỗ trợ thư viện ảnh tới 5 ảnh (`RECIPE_MEDIA`) với đúng 1 ảnh được chọn làm ảnh bìa (cover), tối đa một YouTube URL; mọi nguyên liệu đều được định lượng bằng số và đơn vị chuẩn có khả năng quy đổi; người dùng có thể khám phá món qua 6 chế độ sắp xếp (Mới nhất, Được yêu thích nhất theo % Like, Xem nhiều nhất, Nhiều bình luận nhất, Hoạt động sôi nổi nhất và Thịnh hành); Customer được đánh giá chất lượng món qua cơ chế Like / Dislike, tính tỷ lệ % hài lòng hiển thị huy hiệu `👍 {like_percentage}%` trước mặt thẻ bài viết (tương tự Samsung Food) và xuất dữ liệu công thức/thực đơn ra file text/PDF; mô hình Blog độc lập nhúng công thức (Samsung Food style) được phân rã để chuẩn bị cho giai đoạn sau khi các chức năng cốt lõi hoàn thành.

## 3. Người dùng

| Actor | Mục tiêu chính | Ranh giới chính |
|---|---|---|
| Guest | Khám phá nội dung công khai, xem tỷ lệ % Like và tổng số đánh giá, trải nghiệm AI Chatbot cơ bản (có technical rate limit) | Không có dữ liệu cá nhân lưu theo tài khoản; không bình luận, không Like/Dislike, không đăng bài, không lưu công thức; Guest không phải persistent entity |
| Customer (Người dùng thường) | Khám phá, bình chọn Like / Dislike bài công thức, bình luận/reply, lưu công thức (`Saved Recipe`), lập lịch ăn ngày/tuần (`Meal Plan`), so sánh thống kê dinh dưỡng tuần, xuất file công thức/thực đơn/báo cáo PDF, nộp đơn đăng ký làm Chuyên gia theo format | **Tuyệt đối không có quyền tạo hoặc công khai bài công thức**; chỉ quản lý dữ liệu cá nhân của mình |
| Chuyên gia ẩm thực chay (`EXPERT`) | Sáng tạo, chỉnh sửa, xóa và công khai trực tiếp các bài công thức chay chuẩn mực; chia sẻ kinh nghiệm ẩm thực chay cho cộng đồng | **Chủ thể duy nhất được quyền đăng bài** sau khi đơn đăng ký được duyệt; không bắt buộc viết từng bước nấu ăn; tuân thủ quy tắc định lượng số học và tỷ lệ quy đổi gam; không tự Like/Dislike bài của mình |
| Administrator | Quản lý tài khoản, từ điển nguyên liệu, đơn vị đo lường và bảng quy đổi, xét duyệt đơn đăng ký Chuyên gia (`FR-05`), xử lý báo cáo vi phạm (`FR-06`) | Quyết định phê duyệt chuyên gia dựa trên format văn bản; xử lý vi phạm ghi trực tiếp trên bản ghi Report; không can thiệp sửa nội dung công thức của chuyên gia; không quản lý bảng danh mục động |
| Dịch vụ ngoài | Gemini, Azure Blob Storage, YouTube, Google authentication/email và dịch vụ thanh toán hỗ trợ năng lực MVP | Lỗi provider phải được xử lý minh bạch; không giả lập dữ liệu thành kết quả thật. Google Maps không thuộc baseline hiện tại. |

Free, Plus và Pro là hạng gói dịch vụ (Subscription Tier) của Customer/Expert xác định quyền sử dụng tính năng AI, không phải actor hoặc role phân quyền.

## 4. Năng lực cốt lõi MVP

| Nhóm | Kết quả sản phẩm |
|---|---|
| Khám phá công thức | Tìm/lọc/xem bài công thức công khai của Chuyên gia theo 6 chế độ sắp xếp độc lập (Newest, Most Liked / Highest Rated theo tỷ lệ % Like, Most Viewed 24h/7d/30d/all-time, Most Commented, Most Active theo tương tác gần nhất 7 ngày, Trending theo tương tác + độ tươi mới); hiển thị dữ liệu món ăn có cấu trúc (hướng dẫn chế biến chi tiết, thể loại món ăn chuẩn hóa, nguyên liệu định lượng chuẩn, thời gian, khẩu phần), bộ sưu tập tối đa 5 ảnh (`RECIPE_MEDIA`) và 1 video YouTube; hiển thị huy hiệu `👍 {like_percentage}%` nổi bật trên ảnh thẻ món ăn |
| Thẩm định Chuyên gia | Customer nộp đơn xin cấp quyền Chuyên gia theo format văn bản (`EXPERT_APPLICATION`: kinh nghiệm ẩm thực chay, trường phái chay, tóm tắt công thức mẫu, link tham khảo); Administrator thẩm định và phê duyệt hoặc từ chối kèm lý do; tài khoản được thăng cấp `EXPERT` ngay khi duyệt (`FR-05` ACTIVE) |
| Đăng bài Chuyên gia | Chỉ tài khoản có vai trò `Chuyên gia` đã được phê duyệt mới được tạo và công khai Recipe Post trực tiếp (hướng dẫn chế biến 10–5.000 ký tự linh hoạt, chọn 1 thể loại món ăn `dish_category`, người đăng không bắt buộc phải viết từng bước nấu ăn, tối đa 5 ảnh với đúng 1 ảnh cover, 1 YouTube URL, nguyên liệu bắt buộc số lượng > 0 và đơn vị có công thức quy đổi hợp lệ); Administrator hậu kiểm sau đăng dựa trên báo cáo vi phạm |
| Cá nhân hóa | Thu thập sở thích/kiêng/dị ứng tối thiểu trước khi gọi AI cá nhân hóa; lưu trực tiếp trong hồ sơ User và User Ingredient Preference |
| AI (Feature-based) | Phân quyền theo gói Subscription: Free (AI Chatbot cơ bản), Plus (AI soạn bài giới thiệu/hướng dẫn + AI gợi ý biến tấu món), Pro (Toàn quyền gồm AI Lập thực đơn tuần tự động 7 ngày); Guest được dùng AI Chatbot cơ bản với technical rate limit |
| Meal planning & Shopping | Lưu công thức vào Saved Recipe, quản lý lịch ăn ngày/tuần ba bữa và Shopping List; chuẩn hóa đơn vị qua `UNIT` và `INGREDIENT_UNIT_CONVERSION` theo 3 chiều (`MASS`, `VOLUME`, `COUNT`), gom nguyên liệu cùng chiều an toàn; cấm hoàn toàn giá trị phi số học như “vừa đủ”. |
| Xuất file (Export) & Báo cáo | Hỗ trợ Customer xuất dữ liệu công thức chi tiết (.txt/.pdf), kế hoạch thực đơn tuần (.txt/.pdf), báo cáo phân tích dinh dưỡng tuần có thống kê và so sánh 7 ngày ra file PDF chuẩn A4, và Shopping List ra file văn bản (.txt) để tiện theo dõi, nấu nướng và mua sắm |
| Dinh dưỡng | Tính từ gram nguyên liệu/khẩu phần và danh mục có nguồn USDA/NIH; 100% nguyên liệu công khai đều có đơn vị đo lường và tỷ lệ quy đổi xác định (chặn xuất bản nếu thiếu quy đổi); BMI chỉ tham khảo, không tự kê calorie/macro target từ BMI hoặc weight goal |
| Cộng đồng | Bình luận/reply tối đa 5 cấp, cha bị xóa thành tombstone, lưu công thức (Saved Recipe), báo cáo nội dung; đánh giá chất lượng công thức bằng bình chọn Like / Dislike dành cho Customer/Expert đã đăng nhập (`RECIPE_REACTION`, Guest chỉ có quyền xem); hiển thị tỷ lệ % hài lòng `👍 {like_percentage}%` trên thẻ món ăn (tương tự Samsung Food); lưu vết lượt xem chi tiết có khử trùng lặp (`RECIPE_VIEW`); không áp dụng Like cho bình luận/reply |
| Địa điểm | `OUT_OF_SCOPE` — M11/Google Maps không thuộc baseline triển khai hiện tại; ứng dụng không quản lý, xác minh hoặc cam kết tích hợp dữ liệu nhà hàng bên ngoài. |
| Gói AI | Phân quyền tính năng theo gói: FREE 0 VNĐ/tháng, PLUS 49.000 VNĐ/tháng, PRO 99.000 VNĐ/tháng; không auto-renew/partial refund; entitlement kích hoạt sau xác minh thanh toán và hết hạn cuối kỳ; xử lý trùng idempotent; không quản lý quota đếm số lượt/ngày. |

## 5. Ngoài phạm vi MVP

- Bảng quản lý danh mục động (`CATEGORY`, `RECIPE_CATEGORY`); mỗi Recipe Post chọn 1 thể loại món ăn trong danh mục chuẩn hóa (`dish_category`).
- Quản lý chế độ nấu ăn từng bước và bảng lưu trữ các bước độc lập (`RECIPE_STEP`); hướng dẫn chế biến được lưu trữ dưới dạng văn bản tự do/tổng thể `instructions` (10–5.000 ký tự) trực tiếp trên `Recipe Post` (Phương án B).
- Quản lý tệp chứng chỉ, văn bằng hoặc bằng cấp vật lý trong quy trình xét duyệt Chuyên gia (ứng dụng chỉ thẩm định dựa trên format văn bản có cấu trúc do người dùng khai báo).
- Người dùng thông thường (Customer) tự do đăng tải bài công thức (chỉ dành riêng cho Chuyên gia đã được phê duyệt).
- Chức năng Đăng Blog thật nhúng thẻ công thức (True Blog with Embedded Recipes theo mô hình Samsung Food) — đã được phân rã chi tiết để chuẩn bị mở rộng sau này, nhưng nằm ngoài phạm vi MVP ban đầu (`OUT_OF_SCOPE`); chỉ phát triển khi toàn bộ các chức năng cốt lõi đã hoàn thành.
- Quản lý kho thực phẩm cá nhân (Pantry/Inventory) và đề xuất công thức/bài viết từ kho hoặc nhờ AI sáng tạo món mới từ kho: Đã phân rã định hướng (`FR-56`), nằm ngoài phạm vi MVP ban đầu (`OUT_OF_SCOPE`).
- Tính năng Thích (Like) đối với Bình luận (`Comment Like`) và Phản hồi (`Reply Like`) (đã loại bỏ triệt để khỏi MVP để giữ môi trường thảo luận tinh gọn; tính năng Like/Dislike chỉ áp dụng độc quyền cho Bài công thức để đánh giá chất lượng món ăn thay cho thang điểm sao).
- Lưu vết lịch sử nhiều lần xử lý kiểm duyệt (`moderation_action`) độc lập; kết quả xử lý lưu trực tiếp tại `Report`.
- Quản lý quota sử dụng AI theo số lượng request/ngày (5/15/50) và thực thể `ai_usage_record`.
- Mobile app native, GPS/vị trí hiện tại, wearable và health platform.
- Chẩn đoán hoặc tư vấn điều trị; chức năng dinh dưỡng cho nhóm ngoài điều kiện an toàn đã ghi trong SRS.
- AI tạo công thức không có nguồn, AI tự tạo số liệu dinh dưỡng hoặc tự quyết định duyệt/xóa nội dung.
- Upload video trực tiếp, queue món, tìm/đề xuất hoặc quản lý hồ sơ nhà hàng qua Google Maps, chia sẻ Shopping List nhiều tài khoản, đồng bộ realtime, AI tự nhận diện nguyên liệu tương đương hoặc tự quy đổi đơn vị phức tạp.
- Gói Max không giới hạn, nhận diện ảnh nguyên liệu, tóm tắt video và dự báo giá/mùa nguyên liệu.

## 6. Nguyên tắc chất lượng

- Backend thực thi authentication, authorization, validation, feature entitlement và ownership; frontend không phải lớp bảo vệ duy nhất.
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

- Gemini model và AI architecture.
- Công thức/hệ số dinh dưỡng cần nghiên cứu khi triển khai.
- Provider cho payment/email và chi tiết kỹ thuật webhook/retry; giá/chính sách subscription đã chốt.
- Thời lượng access token và storage/rotation implementation trong baseline refresh-session đã chốt.
- Frontend state management, CSS/UI library và deployment.

Mọi thay đổi phạm vi phải cập nhật PRD/SRS, ghi lý do trong ADR hoặc Issue quyết định và bổ sung `CHANGELOG.md` khi có ý nghĩa.
