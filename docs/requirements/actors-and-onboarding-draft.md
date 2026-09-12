# Actor, quyền hạn và Onboarding Questionnaire

Trạng thái: bản đề xuất để thảo luận ngày 11/09/2026. Product Vision đã chốt trong SRS; bảng quyền dưới đây chưa được coi là phê duyệt toàn bộ.

## 1. Actor con người đề xuất

| Actor | Chức năng | Quyết định của actor | Ranh giới |
| --- | --- | --- | --- |
| Guest | Khám phá/tìm món và đọc Blog công khai; xem ảnh/video nhúng; hỏi AI Free 5 lượt/ngày; đăng ký/đăng nhập | Chọn nội dung xem, câu hỏi và có tạo tài khoản không | Chưa có hồ sơ/thực đơn lưu theo tài khoản; không đăng bài/bình luận |
| Member (Authorized User) | Quyền xem như Guest; trả lời/sửa onboarding; tìm món; nhận gợi ý theo quyền gói; thêm/thay/xóa món trong thực đơn riêng; gửi và theo dõi bài Blog của mình; bình luận/vote/lưu bài | Khai báo sở thích; chọn món lưu và món thay thế; gửi bài để duyệt | Không duyệt bài hoặc sửa dữ liệu người khác; quyền AI nâng cao theo gói cần chốt lại |
| Administrator | Quản lý tài khoản, danh mục; kiểm tra Blog và media; duyệt/từ chối kèm lý do; xử lý nội dung vi phạm; theo dõi lỗi/usage AI và quản lý gói demo | Bài nào công khai, lý do từ chối/ẩn; tài khoản cần xử lý | Không tự sửa sở thích/thực đơn riêng của Member; quyền truy cập dữ liệu riêng không mặc định được cấp |

Đề xuất dùng ba actor con người. Người đăng bài công thức cũng là Member; chưa cần Contributor, Expert, Moderator hoặc Super Admin riêng. Guest là trạng thái chưa xác thực, không nhất thiết là role lưu trong database. Free/Plus/Pro là hạng gói của Member, không phải ba role quản trị khác nhau. Chi tiết quyền Admin, vote/save và trạng thái bình luận còn cần xác nhận.

## 2. Actor hệ thống bên ngoài

| Dịch vụ | Vai trò hỗ trợ use case |
| --- | --- |
| Gemini API | Tạo phản hồi/gợi ý từ dữ liệu ứng dụng cung cấp; ứng dụng kiểm tra kết quả trước khi cho chọn |
| Azure Blob Storage | Lưu và phục vụ ảnh theo quyền truy cập do ứng dụng quản lý |
| YouTube | Phát video nhúng trong Blog Post |

Đây là supporting actors khi vẽ ranh giới hệ thống; không phải tài khoản đăng nhập. SQL Server, bộ đếm lượt và bộ lọc nghiệp vụ là thành phần nội bộ. Payment provider chưa thuộc phạm vi demo hiện tại.

## 3. Onboarding Questionnaire — đề xuất

Mục đích: thiết lập hồ sơ sở thích sau đăng ký. Đây không phải bài kiểm tra chấm điểm hoặc đánh giá sức khỏe. Một người vừa đăng ký vẫn là Member, với trạng thái hồ sơ chưa hoàn tất.

| Nội dung hỏi | Ví dụ | Mục đích |
| --- | --- | --- |
| Kiểu ăn chay | Vegan; vegetarian với thông tin có ăn trứng/sữa | Xác định chế độ cần đáp ứng; danh sách cụ thể chờ chốt |
| Nguyên liệu cần tránh do dị ứng/kiêng | Đậu phộng | Tách ràng buộc cần loại trừ khỏi sở thích; không xem thiếu khai báo là xác nhận an toàn |
| Nguyên liệu hoặc món không thích | Nấm | Tránh đề xuất trái sở thích; cần ánh xạ món và nguyên liệu rõ ràng |
| Phong cách món ưa thích | Món Việt | Ưu tiên lựa chọn phù hợp |
| Mức độ/thời gian nấu mong muốn | Dễ nấu, khoảng 30 phút | Giúp xếp thứ tự món thực tế có thể nấu |

Các trường trên chỉ là đề xuất, chưa phải schema hoặc trường bắt buộc. BMI, mục tiêu cân nặng và calorie sẽ được quyết định riêng sau khi làm rõ yêu cầu nguồn.

Luồng đề xuất: đăng ký thành công → giới thiệu lợi ích onboarding → trả lời hoặc bỏ qua → xem lại thông tin → lưu → khám phá món. Có mục sửa sở thích về sau. Nếu cho bỏ qua, người dùng vẫn khám phá và lập thực đơn thủ công; trước khi cá nhân hóa, app yêu cầu thông tin tối thiểu còn thiếu. Chính sách bỏ qua chưa chốt.

## 4. Các quyết định cần chốt tuần tự

1. Ba actor con người Guest/Member/Administrator và supporting actors như trên có đủ không?
2. Onboarding bắt buộc hay cho bỏ qua? Đề xuất cho bỏ qua và bổ sung trước khi cá nhân hóa.
3. Chốt từng trường onboarding, cách trả lời và ràng buộc cứng/mềm.
4. Chốt quyền AI của Member Free: gợi ý món có được dùng trong 5 lượt/ngày hay chỉ hỏi đáp tổng quát? Tài liệu gói trước đây hạn chế cá nhân hóa, cần thống nhất với lõi sản phẩm mới.
5. Chốt quyền tạo/sửa bài, bình luận và phạm vi quản trị ở các phiên phân rã tiếp theo.

Không tự áp dụng toàn bộ bảng quyền hoặc chuyển sang quyết định số 2 trước khi người dùng chốt quyết định số 1.
