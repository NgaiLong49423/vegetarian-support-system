> **Document:** SRS Product Decision Questionnaire  
> **File:** `docs/requirements/SRS-decision-questionnaire.md`  
> **Version:** v0.2.0  
> **Created:** 2026-09-12  
> **Last Updated:** 2026-09-12  
> **Status:** Under Review  
> **Related Docs:** `docs/requirements/SRS.md`, `docs/requirements/actors-and-onboarding-draft.md`, `01-phan-ra-goi-ai.md`  

# Phiếu chốt quyết định sản phẩm trước khi hoàn thiện SRS

## Xác nhận sau khi trả lời — 12/09/2026

Người dùng đã xác nhận toàn bộ câu trả lời và các cách diễn giải tại [SRS mục 3.20](SRS.md). Hai ý làm rõ dưới đây thay thế phần lựa chọn A tương ứng; giữ nguyên câu trả lời ban đầu để truy vết:

- **Q31:** hiện tại chỉ ghi nguồn tham khảo; nghiên cứu/chốt công thức dinh dưỡng khi triển khai, không phải việc cần làm ngay để hoàn tất tài liệu chức năng.
- **Q35:** đề tài là định hướng đề xuất, nhóm chủ động chọn phạm vi; không cần chờ giảng viên xác nhận việc bỏ gợi ý nhà hàng theo món đã tìm.
- **Q13:** gợi ý bài liên quan thông thường không AI, cùng tùy chọn người dùng chủ động gọi AI có tính lượt.
- **Q27:** duy trì/giảm/tăng cân và tăng cơ đã chốt; mục tiêu khác cần định nghĩa trước khi bổ sung.
- **Q37:** AI gắn cờ là mở rộng nếu core xong sớm, không phải nghiệm thu MVP bắt buộc.

## Cách trả lời

- Trả lời theo dạng `Q01-A, Q02-B, Q03: ý kiến riêng...` trong một lần.
- Phương án có ký hiệu **Khuyến nghị** là mặc định phù hợp nhất với MVP 10 tuần.
- Có thể bỏ trống câu chưa biết. Các câu bỏ trống sẽ được chốt theo phương án khuyến nghị, trừ khi phương án đó cần xác nhận của giảng viên hoặc nguồn chuyên môn.
- Phiếu này chỉ hỏi về phạm vi và hành vi sản phẩm. API, database schema, thuật toán, UI chi tiết và test case sẽ được phân rã sau khi SRS được chốt.

## A. Nền tảng và tài khoản

### Q01 — Nền tảng bàn giao

- **A — Khuyến nghị:** Web application responsive dùng tốt trên desktop và điện thoại; không làm mobile app native.
- B: Web application và mobile app native.

> Chốt a

### Q02 — Cách đăng nhập

- **A — Khuyến nghị:** Email + password bằng Spring Security/JWT/BCrypt; không tích hợp Google Login trong MVP.
- B: Email + password và Google Login.
- C: Chỉ Google Login.

> Chốt b

### Q03 — Khôi phục và bảo vệ tài khoản

- **A — Khuyến nghị:** Có đổi mật khẩu khi đã đăng nhập; quên mật khẩu qua liên kết có thời hạn gửi email; không bắt buộc xác minh email trước khi dùng app.
- B: Chỉ đăng ký/đăng nhập/đăng xuất/đổi mật khẩu; quên mật khẩu để Future Scope.
- C: Bắt buộc xác minh email và có quên mật khẩu.

> Chốt c
### Q04 — Hồ sơ công khai

- **A — Khuyến nghị:** Chỉ hiện tên công khai, avatar, giới thiệu ngắn và danh sách bài công khai.
- B: Thêm ngày tham gia và tổng Like nhận được.
- C: Ghi trường công khai khác do nhóm chỉ định.

> Chốt b

## B. Onboarding và hồ sơ sở thích

### Q05 — Các loại ăn chay cho người dùng chọn

- **A — Khuyến nghị:** Vegan, Lacto Vegetarian, Ovo Vegetarian và Lacto-Ovo Vegetarian.
- B: Chỉ Vegan và Vegetarian.
- C: Danh sách khác do nhóm cung cấp.

> Chốt a

### Q06 — Cách nhập dị ứng/kiêng và món không thích

- **A — Khuyến nghị:** Tìm/chọn từ danh mục nguyên liệu, đồng thời cho nhập tên tự do khi chưa có.
- B: Chỉ chọn từ danh mục có sẵn.
- C: Chỉ nhập văn bản tự do.

> Chốt a

### Q07 — Sở thích mềm dùng để ưu tiên món

- **A — Khuyến nghị:** Phong cách ẩm thực, độ khó và thời gian nấu tối đa; đều không bắt buộc.
- B: Chỉ phong cách ẩm thực.
- C: Không dùng sở thích mềm trong MVP.

> Chốt a

### Q08 — Cập nhật Onboarding

- **A — Khuyến nghị:** Member được sửa hồ sơ sở thích bất cứ lúc nào; lần gợi ý AI tiếp theo dùng dữ liệu mới.
- B: Chỉ được làm Onboarding một lần.

> Chốt a

## C. Bài công thức và khám phá món

### Q09 — Danh mục món ăn

- **A — Khuyến nghị:** Admin tạo/sửa/ngừng sử dụng danh mục; một công thức có thể thuộc nhiều danh mục.
- B: Mỗi công thức chỉ thuộc một danh mục.
- C: Dùng danh mục cố định trong code.

> Chốt a

### Q10 — Đơn vị định lượng được nhập trên công thức

- **A — Khuyến nghị:** Hỗ trợ `g`, `kg`, `ml`, `l`, `tsp`, `tbsp`, `cup`, `piece` và `vừa đủ`; chỉ tính dinh dưỡng khi quy đổi đáng tin cậy về gram.
- B: Chỉ cho nhập gram và milliliter để giảm phạm vi.
- C: Danh sách khác do nhóm cung cấp.

> Chốt a

### Q11 — Lưu nháp bài công thức

- **A — Khuyến nghị:** Nút “Lưu nháp” thủ công; không làm autosave trong MVP.
- B: Lưu nháp tự động.
- C: Có cả thủ công và tự động.

> Chốt a

### Q12 — Phạm vi AI hỗ trợ viết bài

- **A — Khuyến nghị:** AI chỉnh câu chữ phần giới thiệu và soạn các bước từ nguyên liệu/thông tin tác giả đã nhập; không tự thêm nguyên liệu hoặc tự công khai.
- B: AI được tạo nháp toàn bộ trường, tác giả tự kiểm tra.
- C: AI chỉ sửa chính tả và cách diễn đạt.

> Chốt a

### Q13 — Gợi ý các bài công thức liên quan

- **A — Khuyến nghị:** Giữ chức năng theo đề bài; dùng danh mục và nguyên liệu chung để hiển thị bài liên quan, không gọi Gemini.
- B: Dùng Gemini để chọn bài liên quan và trừ lượt AI.
- C: Bỏ khỏi MVP và xin giảng viên xác nhận.
> Chốt A và B

### Q14 — Bộ lọc Khám phá món

- **A — Khuyến nghị:** Từ khóa, loại ăn chay, danh mục, nguyên liệu và thời gian nấu; sắp xếp mới nhất hoặc nhiều Like.
- B: Chỉ từ khóa và danh mục.
- C: Bộ lọc khác do nhóm cung cấp.

> Chốt a

## D. Cộng đồng và kiểm duyệt

### Q15 — Công khai và báo cáo bình luận

- **A — Khuyến nghị:** Bình luận/reply công khai ngay; Member được báo cáo cả bài công thức lẫn bình luận; Admin hậu kiểm.
- B: Bình luận công khai ngay nhưng chỉ báo cáo được bài công thức.
- C: Mọi bình luận phải được Admin duyệt trước.

> Chốt a

### Q16 — Like cho bình luận

- **A — Khuyến nghị:** Không Like bình luận trong MVP; Like chỉ áp dụng cho bài công thức.
- B: Cho Like/Unlike cả bình luận và reply.

> Chốt B
### Q17 — Thông báo trong ứng dụng

- **A — Khuyến nghị:** Có thông báo trong app khi đơn xin quyền đăng được xử lý, có reply mới và nội dung của người dùng bị Admin xử lý; không gửi email/push trong MVP.
- B: Thêm thông báo email.
- C: Không có trung tâm thông báo; chỉ hiện kết quả tại từng màn hình.

> Chốt B

### Q18 — Các mức xử lý vi phạm của Admin

- **A — Khuyến nghị:** Cảnh báo, ẩn/xóa nội dung, thu hồi quyền đăng và khóa/mở khóa tài khoản; mọi quyết định phải có lý do.
- B: Chỉ xóa nội dung và khóa/mở khóa tài khoản.
- C: Danh sách khác do nhóm cung cấp.

> Chốt a

## E. Gemini AI, gói sử dụng và Meal Planner

### Q19 — Khác biệt Free/Plus/Pro

- **A — Khuyến nghị:** Member Free/Plus/Pro dùng cùng các chức năng AI; chỉ khác hạn mức 5/15/50 lượt mỗi ngày. Guest chỉ hỏi đáp cơ bản 5 lượt/ngày.
- B: Free chỉ hỏi đáp; Plus được lập menu cơ bản; Pro được lập/chỉnh menu cá nhân hóa.
- C: Cách chia khác do nhóm cung cấp.

> Chốt A

### Q20 — Cách tính lượt cho chức năng AI

- **A — Khuyến nghị:** Mỗi yêu cầu thành công của người dùng, kể cả tạo hoặc chỉnh menu, tính một lượt; lỗi trước kết quả hợp lệ không trừ lượt.
- B: Tạo menu tuần tốn nhiều lượt hơn hỏi đáp.

> Chốt a

### Q21 — Phạm vi một lần AI lập thực đơn

- **A — Khuyến nghị:** Tạo menu 7 ngày với Bữa sáng/Bữa trưa/Bữa tối; người dùng chọn, sửa hoặc bỏ từng món trước khi lưu.
- B: Người dùng chọn số ngày và những bữa cần tạo.
- C: AI chỉ gợi ý danh sách món, người dùng tự xếp lịch.

> Chốt a

### Q22 — Nguyên liệu người dùng “đang có”

- **A — Khuyến nghị:** Nhập danh sách cho từng lần yêu cầu AI; không xây kho nguyên liệu cá nhân trong MVP.
- B: Có Pantry/kho nguyên liệu được lưu theo tài khoản.

> Chốt A

### Q23 — Mức bắt buộc sử dụng nguyên liệu đang có

- **A — Khuyến nghị:** AI ưu tiên dùng nhưng không bắt buộc dùng hết; phải chỉ rõ món còn thiếu nguyên liệu nào.
- B: Chỉ được đề xuất món dùng hoàn toàn nguyên liệu đang có.
- C: Người dùng chọn “ưu tiên” hoặc “bắt buộc” cho từng yêu cầu.

> Chốt a

### Q24 — Thay thế nguyên liệu

- **A — Khuyến nghị:** Chatbot giải thích và gợi ý nguyên liệu thay thế; không tự sửa bài công thức, không tự thay dữ liệu dinh dưỡng và không tạo công thức mới.
- B: Cho người dùng tạo một bản công thức cá nhân đã thay nguyên liệu.
- C: Chỉ trả lời kiến thức chung, không hỗ trợ thay thế nguyên liệu.

> Chốt A

### Q25 — Lịch sử hội thoại AI

- **A — Khuyến nghị:** Member có lịch sử hội thoại; Guest không có. Member được xóa hội thoại của mình.
- B: Không lưu lịch sử cho bất kỳ ai.
- C: Chỉ Plus/Pro có lịch sử.

> Chốt a

### Q26 — Nâng cấp gói trong bản demo

- **A — Khuyến nghị:** Dùng quy trình thanh toán mô phỏng/sandbox để demo; không thu tiền thật.
- B: Admin gán gói cho tài khoản từ trang quản trị.
- C: Tích hợp thanh toán thật.

> Chốt c

## F. BMI và dữ liệu dinh dưỡng

### Q27 — Mục tiêu dinh dưỡng được hỗ trợ

- **A — Khuyến nghị:** Duy trì cân nặng, giảm cân và tăng cân; tất cả chỉ là mức tham khảo, không lập chế độ điều trị.
- B: Chỉ duy trì cân nặng trong MVP.
- C: Thêm tăng cơ hoặc mục tiêu khác do nhóm chỉ định.

> Chốt A, thêm tăng cơ, nhiều kế hoạch khác do nhóm tự thêm vào

### Q28 — Quy mô danh mục dinh dưỡng ban đầu

- **A — Khuyến nghị:** Khoảng 60 nguyên liệu chay thông dụng, nhập từ nguồn đã kiểm chứng và ưu tiên món demo của nhóm.
- B: Khoảng 30 nguyên liệu để giảm dữ liệu nhập.
- C: Từ 100 nguyên liệu trở lên.

>Chốt a

### Q29 — Điều kiện một nguyên liệu được tính dinh dưỡng

- **A — Khuyến nghị:** Chỉ tính khi có thể quy đổi lượng dùng về gram bằng dữ liệu/quy tắc đã được Admin quản lý; trường hợp không quy đổi được phải báo “chưa đủ dữ liệu”.
- B: Chỉ chấp nhận công thức dùng gram.
- C: Cho AI tự ước lượng lượng quy đổi.

>Chôt a

### Q30 — Khi Admin sửa dữ liệu dinh dưỡng

- **A — Khuyến nghị:** Hệ thống tính lại kết quả công thức/menu bằng dữ liệu mới và giữ nhật ký thay đổi nguồn.
- B: Công thức cũ giữ kết quả cũ; chỉ công thức mới dùng dữ liệu mới.
- C: Admin chọn tính lại từng công thức.

>Chốt a

### Q31 — Nguồn và công thức tính nhu cầu hằng ngày

- **A — Khuyến nghị:** Cho phép trợ lý tiếp tục nghiên cứu nguồn chính thức/primary source và đề xuất công thức có trích dẫn trước khi nhóm khóa SRS.
- B: Nhóm/giảng viên cung cấp công thức cố định.
- C: Chỉ hiển thị BMI, không tính mức calorie/dưỡng chất cá nhân.

> Chốt a

## G. Tìm nhà hàng chay

### Q32 — Cách hiểu bán kính

- **A — Khuyến nghị:** Khoảng cách đường thẳng quanh tọa độ địa chỉ; không gọi Directions/Routes API.
- B: Khoảng cách di chuyển theo đường bộ.

>Chốt b

### Q33 — Cách hiển thị kết quả

- **A — Khuyến nghị:** Danh sách gồm tên, địa chỉ, khoảng cách ước tính, rating/open status nếu Google có và nút mở Google Maps; sắp xếp gần nhất. Không nhúng bản đồ tương tác.
- B: Danh sách và bản đồ nhúng trong app.
- C: Chỉ tên, địa chỉ và liên kết Google Maps.

>chốt b

### Q34 — Lưu nhà hàng và giới hạn tìm kiếm

- **A — Khuyến nghị:** Không lưu nhà hàng yêu thích trong MVP; giới hạn cấu hình mặc định 10 lượt tìm/Member/ngày để kiểm soát quota.
- B: Có lưu nhà hàng yêu thích; vẫn giới hạn 10 lượt/ngày.
- C: Không lưu yêu thích và không đặt giới hạn phía ứng dụng.

>Chốt A

### Q35 — Khác biệt với yêu cầu đề tài gốc

- **A — Khuyến nghị:** Giữ quyết định hiện tại — chỉ tìm nhà hàng quanh địa chỉ, không gợi ý theo món đã tìm — và mang điểm khác biệt này hỏi giảng viên trước khi khóa baseline.
- B: Khôi phục chức năng gợi ý cửa hàng/nhà hàng liên quan đến món đã tìm để bám sát nguyên văn đề tài.

>Chốt a

## H. Ranh giới cuối của MVP

### Q36 — Ngôn ngữ giao diện

- **A — Khuyến nghị:** Chỉ tiếng Việt trong MVP.
- B: Tiếng Việt và tiếng Anh.
> Chốt a

### Q37 — AI hỗ trợ kiểm duyệt

- **A — Khuyến nghị:** Giữ ở Future Scope; MVP chỉ có báo cáo của người dùng và Admin hậu kiểm.
- B: Đưa AI gắn cờ bài vào MVP nếu hoàn thành core sớm.
>chốt B
### Q38 — Bộ actor cuối cùng

- **A — Khuyến nghị:** Chỉ Guest, Member và Administrator; `Contributor` là trạng thái/quyền của Member, không phải actor đăng nhập riêng.
- B: Tách Contributor thành actor riêng.
- C: Thêm Expert/Chuyên gia có quy trình xác minh.

>Chốt A

## Sau khi nhận câu trả lời

1. Ghi quyết định vào SRS và các tài liệu liên quan.
2. Tự chọn phương án khuyến nghị cho mọi câu bị bỏ trống. Q31 chỉ ghi nguồn ở giai đoạn này và nghiên cứu công thức khi triển khai; Q35 ghi phạm vi nhóm tự chọn, không có điều kiện mang đi xác nhận.
3. Lập bảng traceability từ yêu cầu đề tài gốc sang FR đã chốt.
4. Chỉ sau đó mới viết User Stories, Use Cases, Acceptance Criteria, ERD và Test Cases.
