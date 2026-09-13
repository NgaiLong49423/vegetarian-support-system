> **Document:** Actor and Onboarding Decomposition
> **File:** `docs/requirements/actors-and-onboarding-draft.md`
> **Version:** v0.3.0
> **Created:** 2026-09-12
> **Last Updated:** 2026-09-13
> **Status:** Under Review
> **Related Docs:** `docs/requirements/SRS.md`

# Actor, quyền hạn và Onboarding Questionnaire

Trạng thái: chức năng Q01–Q38 đã được người dùng xác nhận ngày 12/09/2026; thiết kế chi tiết còn tiếp tục. Xem [SRS mục 3.20](SRS.md).

## 1. Actor con người đã chốt

| Actor | Chức năng | Quyết định của actor | Ranh giới |
| --- | --- | --- | --- |
| Guest | Khám phá/tìm và đọc bài công thức công khai; đọc bình luận và xem tổng Like; xem ảnh/video nhúng; hỏi AI Free 5 lượt/ngày; đăng ký/đăng nhập | Chọn nội dung xem, câu hỏi và có tạo tài khoản không | Chưa có hồ sơ/thực đơn lưu theo tài khoản; không đăng bài/bình luận/Like hoặc tìm nhà hàng chay |
| Member (Authorized User) | Quyền xem như Guest; trả lời/sửa onboarding; tìm món; lưu/bỏ lưu công thức; thêm/chuyển/thay/xóa món trong lịch ăn; xem hồ sơ nhu cầu dinh dưỡng và kiểm tra menu ngày; nhận gợi ý theo quyền gói; tạo/xem/sửa/xóa và công khai Recipe Post trực tiếp; Like/Unlike; tạo/sửa/xóa bình luận của mình và reply lồng nhiều cấp; chức năng tìm/gợi ý nhà hàng chay đang chờ giảng viên xác nhận | Khai báo sở thích; chọn món/menu; chịu trách nhiệm và quản lý bài/bình luận của mình; Like/Unlike; gửi báo cáo khi phát hiện vấn đề | Không quản lý bài/bình luận của tài khoản khác; không tự khôi phục nội dung bị Admin ẩn |
| Administrator | Xử lý báo cáo bài và bình luận; quản lý tài khoản, Recipe Post, bình luận, danh mục và dữ liệu dinh dưỡng; theo dõi lỗi/usage AI và quản lý gói AI và trạng thái đăng ký | Xem xét báo cáo có vi phạm không; áp dụng ẩn/xóa bài hoặc khóa tài khoản theo chính sách và ghi lý do | Không duyệt quyền đăng hoặc từng bài/lần sửa trước khi công khai; không quản lý/xác minh nhà hàng Google Maps; không tự sửa sở thích/thực đơn riêng của Member; quyền truy cập dữ liệu riêng không mặc định được cấp |

Đã chốt ba actor con người. `Contributor` là quyền/trạng thái đăng bài được Admin cấp cho Member sau khi duyệt đơn, không phải actor đăng nhập độc lập. Đơn bắt buộc có xác nhận chính sách, lý do đóng góp, xác nhận trách nhiệm và hiểu chế tài; MVP không yêu cầu chứng chỉ/chuyên môn. Người có quyền này tự công khai bài hợp lệ; Admin hậu kiểm theo báo cáo, không duyệt từng bài. Nếu đơn bị từ chối, Admin phải ghi lý do và Member được sửa/gửi lại; mỗi tài khoản chỉ có một đơn đang chờ và MVP không có thời gian chờ cố định. Chưa cần Expert, Moderator hoặc Super Admin riêng. Bài công thức là loại bài duy nhất; hướng dẫn từng bước là tùy chọn. Guest là trạng thái chưa xác thực, còn Free/Plus/Pro là hạng gói chứ không phải role. Like/Upvote đã chốt thay cho đánh giá sao; bình luận phải hỗ trợ reply lồng nhiều cấp. Giới hạn nội dung đơn, tiêu chí chống lạm dụng, chi tiết hiển thị cây bình luận và chế tài còn cần xác nhận.

Đã chốt: Công thức đã lưu chỉ là thư viện xem lại, không phải dữ liệu Onboarding; Lịch ăn là các mục gắn ngày và một trong ba loại Bữa sáng/Bữa trưa/Bữa tối. Hai dữ liệu độc lập, chỉ thuộc Member đã đăng nhập và không có Queue, Bữa phụ hoặc loại bữa tùy chỉnh trong MVP. Mỗi bữa được có nhiều công thức, không giới hạn cứng nhưng không được thêm trùng cùng công thức trong cùng ngày/bữa.

## 2. Actor hệ thống bên ngoài

| Dịch vụ | Vai trò hỗ trợ use case |
| --- | --- |
| Gemini API | Tạo phản hồi/gợi ý từ dữ liệu ứng dụng cung cấp; ứng dụng kiểm tra kết quả trước khi cho chọn |
| Azure Blob Storage | Lưu và phục vụ ảnh theo quyền truy cập do ứng dụng quản lý |
| YouTube | Phát video nhúng trong bài công thức |
| Google Maps Platform | Candidate only — không là phụ thuộc đã chốt cho đến khi giảng viên xác nhận chức năng tìm/gợi ý nhà hàng chay. |

Đây là supporting actors khi vẽ ranh giới hệ thống; không phải tài khoản đăng nhập. SQL Server, bộ đếm lượt và bộ lọc nghiệp vụ là thành phần nội bộ. Payment provider thuộc phạm vi thanh toán thật; nhà cung cấp chưa chốt. Google Login và dịch vụ email hỗ trợ xác thực/thông báo.

## 3. Onboarding Questionnaire — chính sách đã chốt, trường đang phân rã

Mục đích: thiết lập hồ sơ sở thích sau đăng ký. Đây không phải bài kiểm tra chấm điểm hoặc đánh giá sức khỏe. Một người vừa đăng ký vẫn là Member, với trạng thái hồ sơ chưa hoàn tất.

| Nội dung hỏi | Ví dụ | Mục đích |
| --- | --- | --- |
| Kiểu ăn chay | Vegan, Lacto Vegetarian, Ovo Vegetarian, Lacto-Ovo Vegetarian | Xác định chế độ cần đáp ứng; bốn lựa chọn đã chốt |
| Nguyên liệu cần tránh do dị ứng/kiêng | Đậu phộng | Tách ràng buộc cần loại trừ khỏi sở thích; không xem thiếu khai báo là xác nhận an toàn |
| Nguyên liệu hoặc món không thích | Nấm | Tránh đề xuất trái sở thích; cần ánh xạ món và nguyên liệu rõ ràng |
| Phong cách món ưa thích | Món Việt | Ưu tiên lựa chọn phù hợp |
| Mức độ/thời gian nấu mong muốn | Dễ nấu, khoảng 30 phút | Giúp xếp thứ tự món thực tế có thể nấu |

Ba nhóm tối thiểu trước AI cá nhân hóa đã chốt: kiểu ăn chay; nguyên liệu cần tránh do dị ứng/kiêng; món hoặc nguyên liệu không thích. Hai nhóm danh sách phải cho người dùng xác nhận “Không có”; bỏ trống không đồng nghĩa không có. Phong cách món, độ khó và thời gian nấu là sở thích mềm, được để trống. Hồ sơ nhu cầu dinh dưỡng là chức năng riêng: BMI chỉ tham khảo; mức calorie/dưỡng chất dựa trên hồ sơ rộng hơn và chỉ đánh giá menu khi đủ dữ liệu đáng tin cậy. Phần dinh dưỡng MVP chỉ dành cho Member đủ 18 tuổi, không mang thai/cho con bú và không cần chế độ ăn điều trị; người ngoài phạm vi vẫn dùng các chức năng thông thường. Chín chỉ tiêu MVP đã chốt trong SRS; mục tiêu duy trì/giảm/tăng cân và tăng cơ đã chốt; công thức/khoảng tham khảo nghiên cứu khi triển khai Q31.

Luồng đã chốt ở mức cao: đăng ký thành công → giới thiệu lợi ích Onboarding → trả lời hoặc bỏ qua → vào ứng dụng. Người bỏ qua vẫn dùng chức năng không cá nhân hóa; trước AI gợi ý món/tạo thực đơn cá nhân hóa, hệ thống kiểm tra ba nhóm tối thiểu và điều hướng tới mục còn thiếu. AI hỏi đáp thông thường vẫn dùng theo hạn mức. Hồ sơ có thể cập nhật sau.

## 4. Quyền bổ sung đã xác nhận

- Member chọn nguyên liệu tránh/không thích từ danh mục hoặc nhập tự do; sửa hồ sơ thì yêu cầu AI tiếp theo dùng dữ liệu mới.
- Free/Plus/Pro cùng chức năng AI, khác 5/15/50 lượt/ngày. Guest chỉ hỏi đáp cơ bản 5 lượt/ngày.
- Member Like/Unlike và báo cáo cả bài, bình luận/reply; bình luận công khai ngay. Lịch sử hội thoại AI theo tài khoản không thuộc MVP hiện tại.
- Thông báo trong app và email cho quyết định đơn, reply mới và xử lý nội dung. Admin được cảnh báo, ẩn/xóa, thu hồi quyền, khóa/mở khóa, phải ghi lý do.
- Nhà hàng: danh sách + bản đồ, ngưỡng đường bộ, mặc định 10 lượt/Member/ngày, không lưu yêu thích.
- Thanh toán AI thật; cổng/giá/chính sách chưa chốt. AI gắn cờ chỉ làm nếu core xong sớm.
- Tiếp theo viết User Stories/Use Cases/Acceptance Criteria; không cần hỏi lại bộ actor hoặc quyền đã xác nhận.