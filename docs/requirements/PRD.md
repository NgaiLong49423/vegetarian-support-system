> **Document:** Product Requirements Document
> **File:** `docs/requirements/PRD.md`
> **Version:** v0.3.0
> **Created:** 2026-06-14
> **Last Updated:** 2026-09-13
> **Status:** Under Review
> **Related Docs:** `docs/requirements/SRS.md`, `README.md`

# Product Requirements Document

## 1. Mục đích

Vegetarian Support System là ứng dụng web phục vụ người ăn chay khám phá công thức, quản lý lựa chọn cá nhân, lập thực đơn tuần và nhận hỗ trợ AI có kiểm soát. Tài liệu này mô tả phạm vi sản phẩm cấp cao; [SRS](SRS.md) là nguồn chi tiết cho Functional Requirements, Business Rules và truy vết đề tài.

`Under Review` nghĩa là PRD và SRS chưa phải baseline ổn định để triển khai toàn bộ hoặc phân rã hàng loạt thành Issue. Không suy diễn các mục còn mở thành yêu cầu đã chốt.

## 2. Vấn đề sản phẩm

Người ăn chay thường phải tự tổng hợp công thức, sở thích, nguyên liệu cần tránh và kế hoạch bữa ăn từ nhiều nguồn. Dự án hướng tới một luồng thống nhất: khám phá nội dung có cấu trúc, nhận gợi ý từ các bài có sẵn, chủ động chọn món và quản lý lịch ăn. Hệ thống phải thể hiện rõ giới hạn của AI và dữ liệu dinh dưỡng, không đưa chẩn đoán hoặc thay thế chuyên gia y tế.

## 3. Người dùng

| Actor | Mục tiêu chính | Ranh giới chính |
|---|---|---|
| Guest | Khám phá nội dung công khai và thử AI cơ bản | Không có dữ liệu cá nhân lưu theo tài khoản; không Like, bình luận, đăng bài hoặc tìm nhà hàng |
| Member | Cá nhân hóa, lưu công thức, lập lịch ăn, tương tác, tạo/công khai Recipe Post trực tiếp và dùng AI theo gói | Chỉ quản lý dữ liệu và nội dung thuộc tài khoản của mình |
| Member có quyền đăng | — | RETIRED — không còn actor/phân quyền riêng sau khi bỏ workflow đơn xin quyền đăng ngày 2026-09-13 |
| Administrator | Quản lý tài khoản, danh mục, báo cáo và nội dung vi phạm | Quyết định quản trị phải có lý do và khả năng truy vết; không duyệt quyền đăng hoặc từng bài trước khi công khai |
| Dịch vụ ngoài | Gemini, Azure Blob Storage, Google Maps và dịch vụ thanh toán sẽ cung cấp năng lực tích hợp | Lỗi/quota của nhà cung cấp phải được xử lý minh bạch; không giả lập dữ liệu thành kết quả thật |

Free, Plus và Pro là hạng gói của Member, không phải actor hoặc role phân quyền.

## 4. Năng lực MVP

| Nhóm | Kết quả sản phẩm |
|---|---|
| Khám phá công thức | Tìm/lọc/xem bài công thức công khai có dữ liệu món ăn có cấu trúc và media được hỗ trợ |
| Cá nhân hóa | Thu thập sở thích/kiêng/dị ứng tối thiểu trước khi gọi AI cá nhân hóa; cho phép cập nhật sau |
| AI | Hỏi đáp, gợi ý công thức đang công khai, lập/thay menu và hỗ trợ nội dung có thể chỉnh sửa trong biểu mẫu trong hạn mức gói |
| Meal planning | Lưu công thức và quản lý lịch ăn theo ngày với ba loại bữa cố định; dữ liệu Saved và Planner độc lập |
| Dinh dưỡng | Tính từ danh mục nội bộ có nguồn, thể hiện kết quả thiếu dữ liệu; không chẩn đoán hoặc tự bịa số liệu |
| Cộng đồng | Like/Unlike, bình luận và reply nhiều cấp, báo cáo nội dung; không có đánh giá sao hoặc Blog tổng quát |
| Đăng bài và moderation | Member đã đăng nhập công khai Recipe Post hợp lệ trực tiếp; Administrator hậu kiểm theo báo cáo |
| Địa điểm | NEEDS CLARIFICATION — Chức năng tìm/gợi ý nhà hàng chay đang chờ giảng viên xác nhận; không chốt Google Maps, input hoặc ngưỡng. |
| Gói AI | Guest/Free/Plus/Pro có hạn mức; nâng cấp gói yêu cầu xác minh thanh toán thật. Lịch sử hội thoại AI theo tài khoản không thuộc MVP hiện tại. |

## 5. Ngoài phạm vi MVP

- Mobile app native, GPS/vị trí hiện tại, wearable và health platform.
- Chẩn đoán hoặc tư vấn điều trị; chức năng dinh dưỡng cho nhóm ngoài điều kiện an toàn đã ghi trong SRS.
- AI tạo công thức không có nguồn, AI tự tạo số liệu dinh dưỡng hoặc tự quyết định duyệt/xóa nội dung.
- Upload video trực tiếp, shopping list, queue món, Blog tổng quát, đánh giá 1–5 sao và quản lý hồ sơ nhà hàng nội bộ.
- Gói Max không giới hạn, nhận diện ảnh nguyên liệu, tóm tắt video và dự báo giá/mùa nguyên liệu.

## 6. Nguyên tắc chất lượng

- Backend thực thi authentication, authorization, validation, quota và ownership; frontend không phải lớp bảo vệ duy nhất.
- Secret và credential của Gemini, Azure, Maps hoặc thanh toán không được đưa vào frontend, source code hay Git.
- Kết quả AI và dịch vụ ngoài phải có trạng thái loading/error/quota rõ ràng, không trừ lượt oan khi request thất bại theo quy tắc SRS.
- Dữ liệu cá nhân, prompt và log moderation cần chính sách lưu/xóa trước khi triển khai. Lịch sử hội thoại AI theo tài khoản không thuộc MVP hiện tại.
- API phải được mô tả bằng OpenAPI; thay đổi hợp đồng, schema hoặc workflow cần được review và cập nhật tài liệu tương ứng.

Các ngưỡng hiệu năng, timeout/retry, retention và coverage vẫn cần cụ thể hóa thành NFR đo được.

## 7. Chỉ dấu thành công cho môn học

- Luồng demo cốt lõi chạy end-to-end trên dữ liệu kiểm thử và có bằng chứng test.
- Mỗi yêu cầu triển khai truy vết được từ SRS đến Issue, PR, test và phần demo.
- Nhóm 5 thành viên làm việc qua Issue/PR/review; đóng góp không được suy luận chỉ từ Story Points hoặc số commit.
- `main` luôn là bản đã qua release gate; lỗi hoặc giới hạn chưa xử lý được ghi nhận trung thực.

Chỉ số sản phẩm định lượng cho người dùng thật chưa được chốt; không tự đặt KPI tăng trưởng hoặc doanh thu trong giai đoạn này.

## 8. Phụ thuộc và quyết định còn mở

- Giá, cổng, chu kỳ và chính sách thanh toán.
- Gemini model và AI architecture.
- Công thức/hệ số dinh dưỡng cần nghiên cứu khi triển khai.
- Chi tiết khoảng cách đường bộ, email/in-app notification, validation, retention và hiển thị cây bình luận.
- Frontend state management, CSS/UI library và deployment.

Mọi thay đổi phạm vi phải cập nhật PRD/SRS, ghi lý do trong ADR hoặc Issue quyết định và bổ sung `CHANGELOG.md` khi có ý nghĩa.