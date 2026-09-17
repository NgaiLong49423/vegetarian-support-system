> **Document:** Product Requirements Document — Mâm Xanh
> **File:** `docs/requirements/PRD.md`
> **Version:** v1.0.1
> **Created:** 2026-06-14
> **Last Updated:** 2026-09-17
> **Status:** Active
> **Related Docs:** `docs/requirements/SRS.md`, `README.md`

# Product Requirements Document — Mâm Xanh

## 1. Mục đích

**Mâm Xanh** (Vegetarian Support System) là ứng dụng web phục vụ người ăn chay khám phá công thức, quản lý lựa chọn cá nhân, lập thực đơn tuần và nhận hỗ trợ AI có kiểm soát. Tài liệu này mô tả phạm vi sản phẩm cấp cao; [SRS](SRS.md) là nguồn chi tiết cho Functional Requirements, Business Rules và truy vết đề tài.

`Active` nghĩa là PRD thuộc Requirements Baseline v1.0.0 đã được chốt ngày 16/09/2026. [SRS](SRS.md) và các tài liệu con sở hữu requirement chi tiết cùng lifecycle chính thức; các chi tiết thiết kế/triển khai còn mở không được suy diễn thành yêu cầu sản phẩm mới. Mọi thay đổi semantic sau baseline này phải được review, đánh giá ảnh hưởng và version theo governance hiện hành.

## 2. Vấn đề sản phẩm

Người ăn chay thường phải tự tổng hợp công thức, sở thích, nguyên liệu cần tránh và kế hoạch bữa ăn từ nhiều nguồn. Dự án hướng tới một luồng thống nhất: khám phá nội dung có cấu trúc, nhận gợi ý từ các bài có sẵn, chủ động chọn món và quản lý lịch ăn. Hệ thống phải thể hiện rõ giới hạn của AI và dữ liệu dinh dưỡng, không đưa chẩn đoán hoặc thay thế chuyên gia y tế.

Giá trị cốt lõi được định vị là **personalized vegetarian meal planning**: khai báo sở thích → tìm/lọc món chay → AI gợi ý từ Recipe Post đang công khai → người dùng chọn và lưu món vào thực đơn tuần → nhận gợi ý thay thế khi cần. Nội dung cộng đồng, media và tương tác tạo nguồn nội dung hỗ trợ; chúng không thay thế luồng khám phá và lập kế hoạch bữa ăn.

Sản phẩm MVP tập trung vào nội dung `Recipe Post`; mô hình Blog độc lập nhúng công thức (Samsung Food style) được phân rã để chuẩn bị cho giai đoạn sau khi các chức năng cốt lõi hoàn thành. AI hỗ trợ quyết định và soạn nội dung có thể chỉnh sửa; người dùng vẫn chịu trách nhiệm lựa chọn, xác nhận và công khai.

## 3. Người dùng

| Actor | Mục tiêu chính | Ranh giới chính |
|---|---|---|
| Guest | Khám phá nội dung công khai và thử AI cơ bản | Không có dữ liệu cá nhân lưu theo tài khoản; không Like, bình luận hoặc đăng bài |
| Member | Cá nhân hóa, lưu công thức, lập lịch ăn, tương tác, tạo/công khai Recipe Post trực tiếp và dùng AI theo gói | Chỉ quản lý dữ liệu và nội dung thuộc tài khoản của mình |
| Member có quyền đăng | — | RETIRED — không còn actor/phân quyền riêng sau khi bỏ workflow đơn xin quyền đăng ngày 2026-09-13 |
| Administrator | Quản lý tài khoản, danh mục, báo cáo và nội dung vi phạm | Quyết định quản trị phải có lý do và khả năng truy vết; không duyệt quyền đăng hoặc từng bài trước khi công khai |
| Dịch vụ ngoài | Gemini, Azure Blob Storage, YouTube, Google authentication/email và dịch vụ thanh toán hỗ trợ năng lực MVP | Lỗi/quota của nhà cung cấp phải được xử lý minh bạch; không giả lập dữ liệu thành kết quả thật. Google Maps không thuộc baseline hiện tại. |

Free, Plus và Pro là hạng gói của Member, không phải actor hoặc role phân quyền.

## 4. Năng lực MVP

| Nhóm | Kết quả sản phẩm |
|---|---|
| Khám phá công thức | Tìm/lọc/xem bài công thức công khai có dữ liệu món ăn có cấu trúc và media được hỗ trợ |
| Cá nhân hóa | Thu thập sở thích/kiêng/dị ứng tối thiểu trước khi gọi AI cá nhân hóa; cho phép cập nhật sau |
| AI | Hỏi đáp, gợi ý công thức đang công khai, lập/thay menu và hỗ trợ nội dung có thể chỉnh sửa trong biểu mẫu trong hạn mức gói |
| Meal planning & Shopping | Lưu công thức, quản lý lịch ăn 7 ngày ba bữa và Shopping List; chỉ gom cùng ingredient ID, quy đổi an toàn g↔kg và ml↔l, đơn vị khác phải giống hệt, không suy diễn mass↔volume/piece và không cộng số cho “vừa đủ”. |
| Dinh dưỡng | Tính từ gram nguyên liệu/khẩu phần và danh mục có nguồn USDA/NIH, thể hiện dữ liệu thiếu; BMI chỉ tham khảo, không tự kê calorie/macro target từ BMI hoặc weight goal |
| Cộng đồng | Like/Unlike, bình luận/reply tối đa 5 cấp, cha bị xóa thành tombstone, báo cáo nội dung; không có đánh giá sao |
| Đăng bài và moderation | Member đã đăng nhập công khai Recipe Post hợp lệ trực tiếp; Administrator hậu kiểm theo báo cáo |
| Địa điểm | `OUT_OF_SCOPE` — M11/Google Maps không thuộc baseline triển khai hiện tại; ứng dụng không quản lý, xác minh hoặc cam kết tích hợp dữ liệu nhà hàng bên ngoài. |
| Gói AI | Guest/Free/Plus/Pro có hạn mức 5/5/15/50 request thành công/ngày. FREE 0, PLUS 49,000, PRO 99,000 VND/tháng; không auto-renew/partial refund; entitlement sau xác minh và hết hạn cuối kỳ; xử lý trùng idempotent. |

## 5. Ngoài phạm vi MVP

- Chức năng Đăng Blog thật nhúng thẻ công thức (True Blog with Embedded Recipes theo mô hình Samsung Food) — đã được phân rã chi tiết để chuẩn bị mở rộng sau này, nhưng nằm ngoài phạm vi MVP ban đầu (`OUT_OF_SCOPE`); chỉ phát triển khi toàn bộ các chức năng cốt lõi đã hoàn thành.
- Quản lý kho thực phẩm cá nhân (Pantry/Inventory) và đề xuất công thức/bài viết từ kho hoặc nhờ AI sáng tạo món mới từ kho: Đã phân rã định hướng (`FR-56`), nằm ngoài phạm vi MVP ban đầu (`OUT_OF_SCOPE`).
- Mobile app native, GPS/vị trí hiện tại, wearable và health platform.
- Chẩn đoán hoặc tư vấn điều trị; chức năng dinh dưỡng cho nhóm ngoài điều kiện an toàn đã ghi trong SRS.
- AI tạo công thức không có nguồn, AI tự tạo số liệu dinh dưỡng hoặc tự quyết định duyệt/xóa nội dung.
- Upload video trực tiếp, queue món, đánh giá 1–5 sao, tìm/đề xuất hoặc quản lý hồ sơ nhà hàng qua Google Maps, chia sẻ Shopping List nhiều tài khoản, đồng bộ realtime, AI tự nhận diện nguyên liệu tương đương hoặc tự quy đổi đơn vị phức tạp, và xuất PDF phức tạp.
- Gói Max không giới hạn, nhận diện ảnh nguyên liệu, tóm tắt video và dự báo giá/mùa nguyên liệu.

## 6. Nguyên tắc chất lượng

- Backend thực thi authentication, authorization, validation, quota và ownership; frontend không phải lớp bảo vệ duy nhất.
- Secret và credential của Gemini, Azure, Maps hoặc thanh toán không được đưa vào frontend, source code hay Git.
- Kết quả AI và dịch vụ ngoài phải có trạng thái loading/error/quota rõ ràng, không trừ lượt oan khi request thất bại theo quy tắc SRS.
- Không lưu lịch sử hội thoại AI theo account; telemetry không giữ raw prompt và được lưu 90 ngày. Audit/history moderation phải được bảo toàn theo policy triển khai.
- Khi endpoint hoặc hợp đồng API thật tồn tại, OpenAPI phải phản ánh hành vi, quyền và lỗi thực tế; thay đổi hợp đồng, schema hoặc workflow cần được review và cập nhật tài liệu tương ứng.

Ngưỡng nghiệm thu tải là 50 concurrent users; 100 là stretch, còn 500 CCU, 99,5% monthly uptime và 1.000–10.000 users là design goals. Timeout/retry theo provider và coverage gate tiếp tục được cụ thể hóa ở thiết kế/test.

## 7. Chỉ dấu thành công cho môn học

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
