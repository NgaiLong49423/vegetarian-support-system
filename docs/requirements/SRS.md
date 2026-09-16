> **Document:** Software Requirements Specification
> **File:** `docs/requirements/SRS.md`
> **Version:** v1.0.0
> **Created:** 2026-09-11
> **Last Updated:** 2026-09-16
> **Status:** Active
> **Related Docs:** `docs/requirements/PRD.md`, `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md`, `docs/requirements/srs/BUSINESS-RULES.md`, `docs/requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md`, `docs/architecture/ARCHITECTURE.md`, `docs/testing/TEST-STRATEGY.md`

# Software Requirements Specification

## 1. Mục đích và trạng thái tài liệu

Tài liệu này là khung đặc tả gốc (Root Specification) và **Authoritative Registry** cho **Đề tài 03 — Ứng dụng hỗ trợ người ăn chay**. Trong cấu trúc Modular SRS, tài liệu này sở hữu sự tồn tại của requirement, mã định danh ổn định (stable ID), phân bổ module cấp chỉ mục, trạng thái lifecycle chính thức, phạm vi tổng thể, actors, ranh giới hệ thống và tổng quan truy vết.

Đặc tả chi tiết hành vi chức năng, quy tắc nghiệp vụ và yêu cầu phi chức năng được quản lý chuyên sâu tại các tài liệu:
- `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md` (FR chi tiết)
- `docs/requirements/srs/BUSINESS-RULES.md` (BR chi tiết)
- `docs/requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md` (NFR chi tiết)

`Active` nghĩa là Modular SRS thuộc Requirements Baseline v1.0.0 đã được chốt ngày 16/09/2026 và là nguồn chuẩn để lập kế hoạch triển khai. Document Status của tài liệu và Requirement Lifecycle của từng yêu cầu (`DRAFT`, `ACTIVE`, `DEFERRED`, `OUT_OF_SCOPE`, `RETIRED`) là hai khái niệm độc lập. Mọi thay đổi semantic sau baseline này phải được review, đánh giá ảnh hưởng tới FR/BR/NFR và tăng document version theo governance hiện hành. DEC-001–DEC-018, quyết định phạm vi M11 ngày 16/09/2026 và giá subscription là baseline nghiệp vụ hiện hành; các chi tiết kỹ thuật còn mở không được diễn giải thành quyết định sản phẩm mới.

## 2. Mô tả sản phẩm

Định hướng được người dùng chốt ngày 11/09/2026: **Ứng dụng khám phá món ăn và lập thực đơn chay theo tuần, sử dụng Gemini AI để gợi ý món phù hợp từ các bài công thức đang công khai; người dùng chủ động chọn và thay đổi thực đơn.**

Luồng lõi: khai báo sở thích → khám phá món → nhận gợi ý AI → chọn món → lưu vào thực đơn tuần → chọn món thay thế khi cần. Bài công thức và tương tác cộng đồng cung cấp nội dung hỗ trợ luồng này. Hệ thống không có loại Blog tổng quát tách khỏi công thức.

Gợi ý món phải tham chiếu bài công thức đang công khai và không bị ẩn trong hệ thống. Hệ thống kiểm tra ràng buộc bắt buộc và kết quả AI; khi không đủ món phù hợp phải thông báo rõ. Người dùng quyết định lưu/thay món. Các trường dữ liệu để kiểm tra nguyên liệu và sở thích sẽ được phân rã riêng; việc bài được công khai không phải cam kết an toàn dị ứng hoặc xác nhận chuyên môn.

Onboarding Questionnaire sau đăng ký thu thập sở thích ăn uống phục vụ chọn món. Người dùng được bỏ qua Onboarding, nhưng phải hoàn thành ba nhóm thông tin tối thiểu trước khi dùng AI cá nhân hóa theo mục 3.15. Các actor, ranh giới quyền và chi tiết Onboarding được duy trì trực tiếp trong SRS này. BMI thuộc Hồ sơ nhu cầu dinh dưỡng tại mục 3.18 và chỉ là chỉ số tham khảo, không tự quyết định nhu cầu calorie/dưỡng chất hoặc món phù hợp.

AI hỗ trợ hỏi đáp, tìm kiếm/gợi ý, soạn bản nháp bài công thức theo mẫu và có thể hỗ trợ moderation. AI không phải chuyên gia dinh dưỡng, không tự quyết định duyệt/xóa nội dung và không đưa chẩn đoán hoặc điều trị sức khỏe.

## 3. Ranh giới hệ thống

### 3.1 Trong phạm vi đã chốt

- Nội dung cộng đồng MVP: bài công thức (`Recipe Post`), bình luận và danh mục. Đây là loại bài cốt lõi duy nhất của MVP; mô hình Blog chia sẻ lối sống/kinh nghiệm độc lập có nhúng thẻ công thức (`Recipe Card`) theo Samsung Food đã được phân rã chi tiết tại mục 3.21 nhưng được chốt nằm ngoài phạm vi MVP ban đầu (`OUT_OF_SCOPE`).
- Mỗi bài công thức có dữ liệu món ăn có cấu trúc. Hướng dẫn chuẩn bị/chế biến bắt buộc phải có từ 1 đến 30 bước trước khi công khai theo mục 3.7 và 3.9; mỗi bước có nội dung văn bản không rỗng sau khi trim.
- Media Phase 1: ảnh được upload lên Azure Blob Storage; bài công thức có thể chứa link YouTube và phát bằng embedded player trong app.
- Đăng bài công thức: Member đã đăng nhập tạo và công khai Recipe Post trực tiếp sau khi hoàn thành các trường bắt buộc; không có đơn xin quyền đăng hoặc duyệt trước từng bài.
- Kiểm duyệt sau đăng: người dùng báo cáo nội dung có vấn đề và Administrator xử lý báo cáo. AI quét/gắn cờ là hạng mục để sau (DEFERRED), không phải điều kiện nghiệm thu MVP.
- Người dùng: tìm kiếm/lọc, xem nội dung, tương tác cộng đồng và lập thực đơn tuần.
- AI Gemini: hỏi đáp/gợi ý với Guest Free, Free, Plus và Pro theo lượt gọi/ngày.
- Quản trị: quản lý người dùng, bài công thức, danh mục, video nhúng, bình luận và nội dung bị gắn cờ.
- Danh sách mua sắm cơ bản (`Shopping List`): Member tạo danh sách từ Meal Plan hoặc Recipe; chỉ gom khi cùng `ingredientId`. Hệ thống được quy đổi an toàn `g ↔ kg` và `ml ↔ l`; các đơn vị khác chỉ gom khi giống hệt nhau; không suy diễn khối lượng ↔ thể tích hoặc `piece` ↔ khối lượng/thể tích; lượng `vừa đủ` không được cộng số. Danh sách được phân nhóm theo danh mục và cho phép thêm/sửa/xóa item thủ công, tick đã mua, copy clipboard, xuất `.txt`.

### 3.2 Ngoài phạm vi hiện tại

- Actor chuyên gia dinh dưỡng/thực phẩm độc lập và xác minh chứng chỉ.
- Chẩn đoán, điều trị, tư vấn sức khỏe chuyên nghiệp hoặc thay thế bác sĩ.
- Lưu nháp Recipe Post trên server và lịch sử hội thoại AI theo tài khoản.
- Tính nhu cầu dinh dưỡng, AI menu dinh dưỡng hoặc đánh giá menu cho người dưới 18 tuổi, người mang thai/cho con bú hay người cần chế độ ăn điều trị bệnh.
- Mobile/native app riêng. MVP là web responsive tiếng Việt; thanh toán gói AI thật thuộc phạm vi theo Q26 với giá/chính sách đã chốt, còn lựa chọn payment provider là quyết định kỹ thuật khi tích hợp.
- Chức năng Đăng Blog thật nhúng công thức (True Blog with Embedded Recipes): Được phân rã chi tiết tại mục 3.21 để định hướng mở rộng, nhưng nằm ngoài phạm vi MVP ban đầu (`OUT_OF_SCOPE`); chỉ phát triển khi toàn bộ các chức năng cốt lõi đã hoàn thành trọn vẹn.
- Chức năng Tìm kiếm nhà hàng chay quanh địa chỉ nhập (Module M11, FR-42, FR-43): `OUT_OF_SCOPE` theo quyết định phạm vi ngày 16/09/2026 và không thuộc baseline triển khai hiện tại; không tích hợp Apple Health, Google Fit, wearable hoặc lấy vị trí hiện tại bằng GPS/trình duyệt.
- Nhận diện ảnh nguyên liệu, tóm tắt video, dự báo mùa/giá nguyên liệu.
- Upload/lưu file video trực tiếp trên Azure Blob Storage. Phase 1 chỉ lưu link hoặc YouTube video ID.
- Gói Max “vô hạn”. Nếu có sau này phải là Fair Use và có quota thực tế.
- `Queue` món chờ xếp lịch, một module `Previous` riêng, chia sẻ Shopping List cho người khác, đồng bộ realtime, AI tự nhận diện nguyên liệu tương đương hoặc tự quy đổi đơn vị phức tạp, và xuất PDF phức tạp. Người dùng xem tuần trước bằng cách điều hướng lịch; kết quả AI gợi ý thuộc luồng AI hiện có.
- Chức năng Quản lý kho thực phẩm cá nhân (Pantry/Inventory) và đề xuất công thức từ kho: Được phân rã chi tiết tại mục 3.22 để định hướng tương lai, nhưng nằm ngoài phạm vi MVP ban đầu (`OUT_OF_SCOPE`); chỉ xem xét phát triển sau khi hệ thống hoàn thiện các luồng cốt lõi.

### 3.3 Bài công thức và hướng tham khảo Samsung Food — cập nhật 12/09/2026

Samsung Food là sản phẩm tham khảo chính cho cách nối khám phá món, chi tiết công thức và Planner; không phải toàn bộ phạm vi cần sao chép. Ứng dụng có một lớp mạng xã hội thu nhỏ xoay quanh công thức: tác giả quản lý bài của mình và người dùng tương tác theo quyền. Tuy nhiên, luồng lõi của sản phẩm vẫn là khám phá món, AI gợi ý và lập thực đơn; không tự mở rộng sang đầy đủ tính năng của Facebook.

“Bình chọn” trong phạm vi MVP được hiểu là **Like/Upvote**, không phải chấm điểm 1–5 sao. Member đã đăng nhập được thích hoặc bỏ thích một bài công thức; mỗi Member chỉ đóng góp tối đa một lượt thích đang hiệu lực cho mỗi bài. Guest được xem tổng lượt thích nhưng phải đăng nhập trước khi thích. Like không làm thay đổi Saved Recipes, hồ sơ sở thích, Meal Planner hoặc hạn mức AI.

Bình luận phải hỗ trợ **reply lồng tối đa 5 cấp**, không giới hạn thành danh sách phản hồi phẳng. Guest được đọc bình luận trên bài công thức công khai. Member đã đăng nhập được tạo bình luận cấp đầu, trả lời một bình luận hoặc một reply khác trong giới hạn độ sâu, đồng thời sửa/xóa bình luận của chính mình. Administrator được quản lý và xử lý bình luận vi phạm. Khi bình luận cha bị xóa, hệ thống giữ một tombstone để bảo toàn ngữ cảnh và các reply hiện có; cách thu gọn/hiển thị cây là chi tiết thiết kế, không được làm mất reply.

Bài công thức có năm nhóm dữ liệu cấu trúc bắt buộc trước khi công khai (mô tả giới thiệu và media là tùy chọn theo mục 3.9):

| Nhóm dữ liệu | Mục đích |
| --- | --- |
| Nguyên liệu | Bắt buộc (1–50 dòng); cho người đọc biết thành phần; làm đầu vào cho lọc và kiểm tra gợi ý AI. |
| Hướng dẫn chuẩn bị/chế biến | Bắt buộc (1–30 bước có nội dung không rỗng sau khi trim); hướng dẫn trình tự thực hiện món ăn. |
| Khẩu phần | Bắt buộc (1–50); cho biết công thức phục vụ bao nhiêu người/phần. |
| Thời gian (prep & cook) | Bắt buộc (tổng thời gian > 0; cookTime = 0 hợp lệ nếu prepTime > 0); hỗ trợ chọn món theo thời gian. |
| Loại ăn chay | Bắt buộc (chọn 1 trong 4 loại chuẩn); hỗ trợ phân loại và tìm món theo chế độ ăn. |

Trang Khám phá trình bày bài công thức đang công khai và không bị ẩn dưới dạng thẻ món; trang chi tiết nối tới luồng thêm món vào thực đơn. Thẻ món, trang chi tiết và mục tham chiếu trong thực đơn là các cách hiển thị của cùng một bài công thức, không phải các loại nội dung hoặc website riêng.

Trường bắt buộc trước khi công khai được chốt tại mục 3.9. Dữ liệu dinh dưỡng vẫn không bắt buộc để bài được công khai, nhưng bài thiếu dữ liệu dinh dưỡng đáng tin cậy không đủ điều kiện tham gia AI lập menu theo dinh dưỡng hoặc kiểm tra dinh dưỡng ngày tại mục 3.18. **Đã chốt:** Vegan, Lacto Vegetarian, Ovo Vegetarian và Lacto-Ovo Vegetarian. Giới hạn giá trị và chuẩn hóa chi tiết để bước thiết kế. Có trường nguyên liệu không đồng nghĩa hệ thống đã bảo đảm kiểm tra dị ứng chính xác. Chưa tự thêm import công thức từ URL, Queue hoặc shopping list.

### 3.4 Nhập nguyên liệu linh hoạt và chuẩn hóa — đã chốt 11/09/2026

Quyết định này thay thế yêu cầu bắt buộc mọi nguyên liệu phải có trong danh mục ở v0.6.0. Chuẩn hóa phục vụ tìm/lọc và AI, không phải điều kiện để người dùng hoàn thiện công thức.

- Administrator quản lý danh mục nguyên liệu chuẩn dùng chung.
- Ô nhập nguyên liệu ưu tiên gợi ý từ danh mục. Người đăng có thể chọn kết quả có sẵn hoặc dùng tên mới khi không tìm thấy, rồi khai báo lượng cho từng dòng theo mục 3.5.
- Mỗi dòng giữ tên hiển thị do người dùng nhập/chọn, thông tin lượng và liên kết nguyên liệu chuẩn nếu có. Liên kết này là tùy chọn; khi chuẩn hóa sau vẫn giữ tên đã nhập để hiển thị công thức.
- Ví dụ: chọn `Đậu hũ` có sẵn và nhập `200 g`; hoặc dùng tên mới `Nấm hầu thủ` và nhập `100 g` dù danh mục chưa có.
- Tên mới thuộc dòng nguyên liệu của công thức, không tự trở thành mục chuẩn dùng chung. Member đã đăng nhập vẫn hoàn thiện và công khai bài bình thường, không phải chờ Admin bổ sung danh mục hay qua cửa duyệt nguyên liệu riêng.
- Việc chưa liên kết danh mục không tự ngăn bài được công khai; tác giả vẫn chịu trách nhiệm tuân thủ chính sách nội dung. Admin có thể chuẩn hóa/liên kết nguyên liệu sau.
- Tìm kiếm có thể sử dụng cả tên đã nhập và danh mục chuẩn. AI hỗ trợ đối chiếu tên chỉ là khả năng đề xuất, chưa chốt triển khai và không mặc định kết quả AI đúng.
- Không coi nguyên liệu chưa nhận diện là phù hợp với mọi chế độ ăn. Với gợi ý có ràng buộc loại trừ, hệ thống không tự khẳng định món phù hợp khi chưa kiểm tra được thành phần.

**Còn phân rã/thiết kế:** cơ chế tên đồng nghĩa, biểu diễn định lượng, và workflow sửa/ngừng sử dụng nguyên liệu chuẩn đã được tham chiếu. AI không được coi nguyên liệu chưa kiểm tra là an toàn; danh sách đơn vị và validation Recipe Post đã chốt tại 3.20/3.9. Chưa thiết kế ERD hoặc workflow chuẩn hóa chi tiết.

### 3.5 Định lượng nguyên liệu — đã chốt 12/09/2026

- Mỗi dòng nguyên liệu hỗ trợ một trong hai cách khai báo: định lượng cụ thể bằng số lượng và đơn vị (ví dụ `200 g đậu hũ`), hoặc `vừa đủ` (ví dụ `muối — vừa đủ`).
- Với `vừa đủ`, không bắt buộc nhập số lượng hay đơn vị; hệ thống giữ rõ cách khai báo này, không dùng số giả như 0 để biểu diễn lượng chưa xác định.
- Hệ thống không tự quy đổi `vừa đủ` sang gram và không suy ra dinh dưỡng chính xác từ lượng chưa xác định. Chức năng dinh dưỡng tại 3.18 phải thể hiện dữ liệu thiếu này.
- Việc dùng `vừa đủ` không làm mất tên nguyên liệu hay liên kết danh mục nếu có; nguyên liệu vẫn thuộc thành phần công thức để kiểm tra ràng buộc ăn uống.

**Đã chốt đơn vị tại 3.20. Còn thiết kế:** định dạng số/phân số, giới hạn định lượng và xử lý thay đổi khẩu phần.

### 3.6 Một bài công thức, nhiều cách hiển thị và AI hỗ trợ soạn bài — cập nhật 12/09/2026

- Tác giả tạo một bài công thức có cấu trúc. Thẻ món ở Khám phá, trang chi tiết và tham chiếu trong thực đơn đều sử dụng cùng bài này; không yêu cầu tạo hai bài, một Blog tổng quát hoặc một website thứ hai.
- Nút “Xem công thức” mở trang chi tiết nội bộ với nội dung hướng dẫn đầy đủ. Không dùng liên kết website ngoài làm nơi bắt buộc đọc cách nấu; đề xuất đóng góp chỉ bằng dẫn nguồn ngoài chưa được chấp thuận.
- Biểu mẫu có các mục tên món, giới thiệu, nguyên liệu, khẩu phần, thời gian, loại ăn chay, hướng dẫn chuẩn bị/chế biến từng bước và media theo chính sách hiện tại. Format quy định cấu trúc; tác giả vẫn được viết theo văn phong riêng. Hướng dẫn chuẩn bị/chế biến bắt buộc có từ 1 đến 30 bước; mô tả giới thiệu và media là tùy chọn. Điều kiện đầy đủ và validation đã chốt tại mục 3.9.
- Tác giả có thể tự viết hoặc chủ động yêu cầu Gemini hỗ trợ tạo nội dung có thể chỉnh sửa trong biểu mẫu từ thông tin đã cung cấp. Không bắt buộc dùng AI để tạo hoặc gửi bài.
- Tác giả xem, sửa và xác nhận nội dung AI trước khi công khai. AI không âm thầm thay nguyên liệu hoặc trình bày thông tin chưa biết như dữ kiện đã xác nhận.
- Member đã đăng nhập quản lý Recipe Post của chính mình theo quyền sở hữu nội dung thông thường của một mạng xã hội thu nhỏ: tạo, xem, sửa và xóa bài. Bài mới hoặc thay đổi hợp lệ được công khai trực tiếp; hệ thống kiểm tra tài khoản sở hữu bài và các trường bắt buộc.
- Khi tác giả xóa bài của mình, bài không còn hiển thị công khai và không được dùng cho kết quả tìm kiếm hoặc gợi ý AI. Tác giả không được dùng quyền sửa/xóa của mình để khôi phục bài đang bị Administrator ẩn do hậu kiểm.
- Khi tác giả bấm công khai, hệ thống kiểm tra đăng nhập, cấu trúc và các trường bắt buộc. Bài hợp lệ được công khai trực tiếp. AI không tự công khai bài. Đúng format không đồng nghĩa công thức đúng hoặc an toàn.
- Khi AI lỗi hoặc hết lượt, người dùng vẫn tiếp tục tự viết/chỉnh sửa bài. Quyền gọi AI soạn bài theo gói và cách tính lượt là quyết định riêng, không tự thay đổi hạn mức 5/15/50 đã chốt.

Luồng cấp cao: Member đã đăng nhập → tác giả nhập thông tin → tự viết hoặc yêu cầu AI hỗ trợ → tác giả rà soát/chỉnh sửa → hệ thống kiểm tra đăng nhập/format → công khai thẻ món và trang chi tiết từ cùng Recipe Post.

**Đã chốt phạm vi AI hỗ trợ nội dung trong biểu mẫu, quyền gói và tính lượt tại 3.20. Lưu nháp Recipe Post không thuộc phạm vi hiện tại. Không phân rã thêm CRUD tác giả ở bước chốt chức năng.

### 3.7 Hướng dẫn chuẩn bị và chế biến — cập nhật 15/09/2026

- Recipe Post bắt buộc phải có từ 1 đến 30 bước hướng dẫn chuẩn bị/chế biến trước khi được công khai. Mỗi bước phải có nội dung văn bản không rỗng sau khi cắt khoảng trắng đầu cuối (trim).
- Phần hướng dẫn là danh sách các bước có thứ tự tuần tự; tác giả có thể thêm, sửa, xóa và thay đổi thứ tự các bước trong trình soạn bài. Trang chi tiết hiển thị các bước theo đúng thứ tự đã lưu.
- Phần mô tả giới thiệu được viết tự do trong mục riêng và mang tính tùy chọn, không thay thế cho danh sách bước hướng dẫn chuẩn bị/chế biến.
- `cookTime = 0` vẫn hợp lệ đối với các món không cần nấu (như salad trộn, nước sốt) nếu thời gian chuẩn bị lớn hơn 0 và các điều kiện thời gian hiện hành khác được đáp ứng.
- Khi AI hỗ trợ tạo hướng dẫn, kết quả phải được trả về dưới dạng các bước có thể chỉnh sửa trong biểu mẫu để tác giả rà soát và chỉnh sửa trước khi công khai. AI tuyệt đối không tự động công khai bài viết.
- Tác giả chịu trách nhiệm về toàn bộ nội dung các bước do mình tự soạn hoặc do AI hỗ trợ và phải tuân thủ chính sách nội dung. Khi sửa hướng dẫn trong bài đang công khai, thay đổi áp dụng theo cùng quy tắc chỉnh sửa bài tại mục 3.6.

### 3.8 Tác giả và hồ sơ công khai — đã chốt 12/09/2026

- Đầu trang chi tiết bài công thức hiển thị tên công khai và avatar của tác giả, kèm liên kết tới hồ sơ công khai và các bài đã công khai của tác giả.
- Hệ thống gắn tác giả với tài khoản đăng bài, không dùng ô nhập tên tác giả tùy ý và không cho người đăng chọn tài khoản khác để đứng tên.
- Hồ sơ công khai không hiển thị email, thông tin đăng nhập hoặc dữ liệu hồ sơ ăn uống riêng tư. Có thêm giới thiệu ngắn, ngày tham gia và tổng Like nhận được trên bài công thức.
- Administrator xử lý báo cáo không trở thành tác giả. AI hỗ trợ viết không thay thế tác giả; người dùng vẫn rà soát và chịu trách nhiệm khi công khai bài.
- Gắn bài với tài khoản nhằm truy xuất người đứng tên nội dung, không chứng minh danh tính ngoài đời hoặc chuyên môn. Bản hiện tại chưa làm xác minh danh tính thật/bằng cấp và không cấp nhãn “đã xác minh danh tính” hay “chuyên gia”.
- Tham khảo giao diện đã quan sát trực tiếp trên Chrome: [Salads for Lunch — Creamy Tuna Macaroni Salad](https://www.salads4lunch.com/recipes/salad-recipes-for-lunch/classic-salads/creamy-tuna-macaroni-salad/) hiển thị ảnh/tên tác giả dẫn tới hồ sơ ở đầu bài và phần giới thiệu tác giả cuối bài. Đây là bằng chứng về cách hiển thị, không xác nhận quy trình kiểm chứng danh tính của website.

**Còn phân rã/thiết kế:** avatar mặc định, quy tắc đổi tên/avatar và cách hiển thị tác giả khi tài khoản bị khóa/xóa. Không tự thêm trường hồ sơ, follow, nhắn tin hoặc xác minh chuyên gia ngoài baseline đã chốt.

### 3.9 Điều kiện tạo và công khai Recipe Post trực tiếp — cập nhật 15/09/2026

| Trường | Bắt buộc khi công khai | Ghi chú |
| --- | --- | --- |
| Tên món | Có | Từ 3 đến 120 ký tự. |
| Nguyên liệu | Có | Từ 1 đến 50 dòng; có thể bao gồm nguyên liệu ngoài danh mục dinh dưỡng theo FR-40. |
| Khẩu phần | Có | Số nguyên từ 1 đến 50. |
| Loại ăn chay | Có | Chọn một trong bốn loại đã chốt tại 3.20. |
| Thời gian chuẩn bị/nấu | Có | Mỗi giá trị từ 0 đến 1.440 phút; tổng thời gian chuẩn bị và nấu phải lớn hơn 0 (`cookTime = 0` hợp lệ nếu `prepTime > 0`). |
| Bước hướng dẫn chuẩn bị/chế biến | Có | Từ 1 đến 30 bước; mỗi bước có nội dung không rỗng sau khi trim; sắp xếp có thứ tự tuần tự. |
| Tác giả | Có | Member đã đăng nhập tạo bài. |
| Mô tả, ảnh và link YouTube | Không | Mô tả tối đa 2.000 ký tự; tối đa 5 ảnh JPEG/PNG/WebP, mỗi ảnh tối đa 5 MB; tối đa một link YouTube. |

- Member đã đăng nhập được tạo và công khai Recipe Post trực tiếp; không có đơn xin quyền đăng, trạng thái quyền đăng riêng, hoặc duyệt trước từng bài.
- Khi Member bấm công khai, hệ thống kiểm tra các trường bắt buộc. Nếu thiếu hoặc không thỏa mãn validation, hệ thống từ chối công khai và thông báo cụ thể trường cần bổ sung/chỉnh sửa.
- Recipe Post hợp lệ được công khai ngay. Sau đó, người dùng có thể báo cáo nội dung; Administrator xử lý báo cáo theo 3.11–3.14.
- AI chỉ hỗ trợ tác giả tạo nội dung có thể chỉnh sửa trong biểu mẫu; AI không tự công khai bài. Việc lưu nháp bền vững trên server không thuộc phạm vi hiện tại.
- Backend phải kiểm tra toàn bộ giới hạn trên trước khi công khai hoặc cập nhật bài; UI validation không thay thế server-side validation. Các quy tắc nội dung/moderation áp dụng độc lập với validation cấu trúc này.
### 3.10 Công khai trực tiếp và hậu kiểm Recipe Post — cập nhật 13/09/2026

- Member đã đăng nhập công khai Recipe Post trực tiếp khi bài hợp lệ; không có biểu mẫu, trạng thái hoặc quyết định duyệt quyền đăng.
- Administrator không phê duyệt trước từng bài. Vai trò của Administrator là xem xét báo cáo, ghi kết luận/lý do và áp dụng biện pháp quản trị theo chính sách.
- Báo cáo từ người dùng là tín hiệu để xem xét, không tự động kết luận bài vi phạm hay tự ẩn/xóa bài.
- AI quét/gắn cờ nội dung là **DEFERRED**. Nếu được triển khai sau này, AI chỉ cung cấp tín hiệu; không tự áp dụng biện pháp quản trị.
- Lưu nháp Recipe Post không thuộc phạm vi hiện tại.
### 3.11 Báo cáo bài công thức và AI hỗ trợ kiểm duyệt — phân tách ưu tiên 12/09/2026

**Đã yêu cầu trong phạm vi cơ bản:** người dùng có thể báo cáo bài công thức có dấu hiệu có vấn đề để Administrator tiếp nhận, xem xét và xử lý. Có báo cáo không đồng nghĩa bài đã vi phạm.

**Quyền báo cáo đã chốt 12/09/2026:** chỉ tài khoản đã đăng nhập được gửi báo cáo bài công thức. Guest bấm “Báo cáo” được yêu cầu đăng nhập; hệ thống chưa tạo báo cáo khi chưa xác thực. Backend phải kiểm tra đăng nhập khi tiếp nhận, không chỉ giới hạn bằng giao diện. Đăng nhập giúp truy xuất báo cáo nhưng không tự thay thế các quy tắc chống lạm dụng.

**Nâng cao nếu còn thời gian:** AI rà soát bài công thức, phát hiện dấu hiệu bất thường và tạo cờ kèm thông tin hỗ trợ. AI không tự ẩn, xóa bài hoặc khóa tài khoản. Quy trình báo cáo của người dùng phải hoạt động khi chưa triển khai AI hoặc AI bị lỗi.

Báo cáo do người dùng gửi và cờ do AI tạo cần phân biệt nguồn; không biến suy đoán AI thành kết luận vi phạm.

**Biểu mẫu báo cáo đã chốt 12/09/2026:** mỗi báo cáo chọn một lý do trong sáu nhóm sau:

1. Sai thông tin công thức hoặc nhãn ăn chay.
2. Nội dung có dấu hiệu nguy hiểm.
3. Spam, quảng cáo không phù hợp.
4. Nội dung xúc phạm hoặc không phù hợp.
5. Sao chép, nghi vi phạm bản quyền.
6. Khác.

Mô tả bổ sung là tùy chọn với năm nhóm đầu; chọn “Khác” phải có mô tả không rỗng (không chỉ gồm khoảng trắng). Hệ thống kiểm tra lý do hợp lệ và điều kiện mô tả trước khi tiếp nhận; nếu không đạt, chỉ rõ lỗi và không tạo báo cáo. Lý do do người dùng chọn là phản ánh cần xem xét, không phải kết luận vi phạm.

**Còn phân rã/thiết kế:** giới hạn độ dài mô tả và biện pháp chống lạm dụng bổ sung. Notification tuân theo DEC-010; AI rà soát/gắn cờ là `DEFERRED`. Không mở rộng báo cáo sang tài khoản; báo cáo bình luận/reply thuộc FR-48. Luồng xử lý đã chốt tại 3.12, quyền xem tại 3.13.

### 3.12 Xử lý báo cáo và ẩn bài công thức — đã chốt 12/09/2026

| Trạng thái báo cáo | Ý nghĩa |
| --- | --- |
| Chờ xử lý | Báo cáo mới được gửi, chờ Administrator tiếp nhận. |
| Đang xem xét | Admin đã tiếp nhận và đang kiểm tra nội dung. |
| Đã giải quyết | Admin đã ghi kết luận và lý do xử lý: có vi phạm hoặc không đủ căn cứ. |

- Trạng thái báo cáo độc lập với trạng thái bài công thức. Gửi báo cáo hoặc có nhiều báo cáo không tự động ẩn bài.
- Nếu xác định có vấn đề, Admin có thể cảnh báo, ẩn hoặc xóa bài công thức, hoặc khóa/mở khóa tài khoản theo mức độ vi phạm và phải ghi lý do. Đây là quyết định thủ công của Administrator, độc lập với quyền tác giả tự xóa bài của mình. MVP không dùng ma trận chế tài số tự động; số lượng báo cáo không tự động tạo chế tài.
- Ẩn không phải xóa bài; bài ẩn không còn được xem công khai nhưng vẫn phục vụ tác giả chỉnh sửa và Admin xem xét. Xóa bài và khóa tài khoản là chế tài nghiêm trọng, phải có lý do.
- Nếu không đủ căn cứ, Admin đóng báo cáo với kết luận và lý do, không ẩn bài vì báo cáo đó. Nếu bài đang công khai thì tiếp tục công khai; việc đóng một báo cáo không tự khôi phục bài bị ẩn bởi quyết định khác.
- Bài bị ẩn không tự công khai khi tác giả sửa. Chỉ Administrator được khôi phục bài sau khi xử lý báo cáo; đây là quyết định hậu kiểm, không phải quy trình duyệt mọi bài mới.
- “Đã giải quyết” nghĩa là Admin đã kết luận/xử lý báo cáo, không đồng nghĩa bài đã được tác giả sửa xong hoặc đã được công khai lại.

Mọi quyết định xử lý phải giữ audit/history và lý do. Bài bị ẩn/xóa/không khả dụng không còn được khám phá công khai hoặc dùng bởi AI, nhưng các tham chiếu cá nhân hiện có trong Saved Recipe, Meal Plan và lịch sử Shopping List được giữ dưới trạng thái unavailable/tombstone, không cascade-delete. Chi tiết UI và chính sách lưu audit dài hạn là thiết kế triển khai, không thay đổi các quy tắc nghiệp vụ này.

### 3.13 Quyền xem báo cáo và bảo vệ người gửi — đã chốt 12/09/2026

| Người xem | Phạm vi được xem |
| --- | --- |
| Administrator | Báo cáo và tài khoản người gửi để xem xét, xử lý và kiểm tra lạm dụng. |
| Tác giả bài công thức bị báo cáo | Lý do, nội dung cần sửa và quyết định xử lý liên quan đến bài của mình; không thấy danh tính người báo cáo. |
| Người gửi báo cáo | Trạng thái và kết quả xử lý báo cáo của chính mình; không được xem báo cáo của tài khoản khác. |
| Công chúng | Báo cáo không được hiển thị công khai. |

- Hệ thống kiểm tra quyền xem ở backend theo vai trò và quan hệ với báo cáo/bài công thức, không chỉ ẩn nút hoặc trường trên giao diện.
- Nội dung gửi cho tác giả không kèm thông tin định danh người báo cáo. Không tự chuyển nguyên văn mô tả hoặc ghi chú nội bộ có thể làm lộ người gửi; thông tin phản hồi phải tuân theo phạm vi trong bảng.
- Quyền xem trạng thái/kết quả không làm công khai ghi chú nội bộ. In-app notification theo business event; moderation-result email phải được thử gửi bất đồng bộ/best-effort và lỗi email không rollback quyết định xử lý. MVP không có push notification.

**Còn phân rã/thiết kế:** màn hình tra cứu, wording notification, retry detail, thời hạn lưu báo cáo và ghi chú nội bộ; các điểm này không thay đổi behavior DEC-010.

### 3.14 Báo cáo trùng và bổ sung thông tin — đã chốt 12/09/2026

- Mỗi tài khoản chỉ có một báo cáo chưa giải quyết trên cùng bài công thức (Chờ xử lý hoặc Đang xem xét).
- Khi báo cáo còn mở, người gửi được bổ sung mô tả vào báo cáo đó, không tạo báo cáo trùng.
- Sau khi báo cáo đã giải quyết, người dùng được báo cáo lại nếu phát hiện vấn đề mới và phải mô tả điểm mới, kể cả khi lý do không phải “Khác”.
- Nhiều tài khoản vẫn có thể báo cáo cùng bài công thức. Admin xem các báo cáo được nhóm theo bài; nhóm hiển thị không làm mất người gửi, trạng thái và kết quả của từng báo cáo.
- Backend kiểm tra quy tắc một báo cáo đang mở, kể cả khi nhận nhiều yêu cầu đồng thời.
- Còn phân rã kỹ thuật giới hạn tần suất/độ dài mô tả bổ sung, lịch sử bổ sung và thao tác xử lý hàng loạt; không có ma trận chế tài số tự động.

### 3.15 Onboarding tùy chọn và điều kiện AI cá nhân hóa — đã chốt 12/09/2026

- Sau khi đăng ký, hệ thống mời Member thực hiện Onboarding Questionnaire và cho phép bỏ qua để vào ứng dụng.
- Member chưa hoàn thành Onboarding vẫn được xem/tìm món, tự thêm món vào thực đơn, đọc/bình luận/đăng bài công thức và dùng AI hỏi đáp thông thường theo hạn mức gói.
- Trước khi dùng AI gợi ý món hoặc tạo thực đơn cá nhân hóa, hồ sơ phải có ba nhóm thông tin tối thiểu: loại ăn chay; nguyên liệu cần tránh do dị ứng/kiêng; món hoặc nguyên liệu không thích.
- Với hai nhóm danh sách, người dùng phải được chủ động xác nhận “Không có” nếu không có mục cần khai báo. Bỏ trống không được tự hiểu là không có dị ứng/kiêng hoặc không có món không thích.
- Nếu thiếu thông tin tối thiểu, hệ thống không gọi Gemini cho yêu cầu cá nhân hóa và hướng người dùng tới đúng mục cần bổ sung; không trừ lượt AI.
- Sở thích mềm như ẩm thực yêu thích, thời gian nấu mong muốn và độ khó có thể để trống. AI chỉ cá nhân hóa theo dữ liệu thực sự đã được khai báo.
- Onboarding và hồ sơ sở thích có thể được cập nhật sau. Việc hoàn thành Onboarding không tạo cam kết y tế hoặc bảo đảm tuyệt đối về dị ứng.

Danh sách bốn loại ăn chay, cách chọn/nhập các nhóm và quyền cập nhật hồ sơ đã chốt tại 3.20. Loại ăn chay và nguyên liệu cần tránh là ràng buộc cứng; khẩu vị, độ khó và thời gian là sở thích mềm tùy chọn. Chi tiết UI/thuật toán còn phân rã; hồ sơ dinh dưỡng và BMI được tách tại 3.18.

### 3.16 Công thức đã lưu và lịch ăn — đã chốt 12/09/2026

Hệ thống tách hai nghiệp vụ cá nhân, không dùng một trạng thái để biểu diễn cả hai:

| Nghiệp vụ | Mục đích | Dữ liệu khái niệm tối thiểu |
| --- | --- | --- |
| Công thức đã lưu (`Saved Recipe`) | Đánh dấu món người dùng muốn xem lại; không gắn ngày hoặc bữa ăn. | Tài khoản, bài công thức, thời điểm lưu. |
| Mục lịch ăn (`Meal Plan Entry`) | Ghi nhận dự định ăn một công thức vào ngày và bữa cụ thể. | Tài khoản, bài công thức, ngày ăn, loại bữa và số khẩu phần dự định ăn. |

**Công thức đã lưu:**

- Chỉ Member đã đăng nhập được lưu/bỏ lưu. Guest chọn “Lưu” được yêu cầu đăng nhập và hệ thống chưa tạo dữ liệu cá nhân trước khi xác thực.
- Member có thể lưu từ thẻ món hoặc trang chi tiết, xem danh sách Đã lưu, tìm/lọc trong danh sách và bỏ lưu.
- Mỗi cặp Member–bài công thức chỉ có tối đa một bản ghi đang lưu; thao tác lặp không tạo bản ghi trùng.
- Lưu công thức không gọi Gemini, không trừ lượt AI và không tự sửa Onboarding/hồ sơ sở thích. Đây chỉ là tín hiệu người dùng muốn xem lại, không phải ràng buộc ăn uống hay khẳng định món luôn phù hợp với họ.

**Lịch ăn:**

- Member có thể thêm bài công thức vào lịch từ trang chi tiết, danh sách Đã lưu, kết quả Gemini gợi ý hoặc màn hình Planner.
- Khi thêm, phải chọn ngày và một trong ba loại bữa cố định: **Bữa sáng (`Breakfast`)**, **Bữa trưa (`Lunch`)** hoặc **Bữa tối (`Dinner`)**.
- MVP không có Bữa phụ (`Snack`) và không cho người dùng tạo/đổi tên loại bữa tùy chỉnh. Tên hiển thị dùng tiếng Việt; giá trị tiếng Anh trong ngoặc là mã khái niệm để thống nhất tài liệu, chưa chốt cách lưu database.
- Mỗi bữa được chứa nhiều bài công thức để biểu diễn một bữa ăn có nhiều món. MVP không đặt giới hạn cứng số món trong một bữa.
- Cùng một bài công thức chỉ xuất hiện tối đa một lần trong cùng tài khoản, ngày và loại bữa. Nếu người dùng thêm lại đúng món vào đúng bữa đó, hệ thống thông báo món đã có và không tạo mục trùng.
- Quy tắc chống trùng không ngăn cùng công thức xuất hiện ở ngày khác hoặc loại bữa khác.
- Member có thể chuyển mục lịch sang ngày/bữa khác, thay bằng bài công thức khác hoặc xóa khỏi lịch.
- Cùng một bài công thức có thể xuất hiện ở nhiều ngày hoặc bữa khác nhau; bản ghi lịch ăn độc lập với bản ghi Đã lưu. Bỏ lưu không tự xóa món khỏi lịch, và xóa khỏi lịch không tự bỏ lưu công thức.
- MVP không có `Queue` món chờ xếp lịch. Mục `Previous` không được lưu như một loại dữ liệu riêng; người dùng chuyển lịch sang tuần trước để xem các mục theo ngày đã lưu.
- `For You` nếu được triển khai là điểm vào luồng Gemini gợi ý hiện có, không phải một hệ thống Planner hoặc kho công thức thứ hai.

Khi bài công thức bị ẩn/xóa/không khả dụng, Saved Recipe và Meal Plan Entry đã tồn tại được giữ dưới trạng thái unavailable/tombstone, không cascade-delete; bài không còn được khám phá công khai hoặc dùng cho gợi ý AI. Ghi chú, thứ tự món trong cùng bữa, giới hạn ngày và thao tác sao chép cả tuần là chi tiết thiết kế chưa quy định. Số khẩu phần dự định ăn đã chốt tại mục 3.18.

### 3.17 AI chỉ đề xuất công thức đã có và đang công khai — cập nhật 12/09/2026

- AI gợi ý món và lập menu chỉ được chọn từ các bài công thức đang công khai, không bị ẩn/xóa trong hệ thống.
- Gợi ý có thể dựa trên loại ăn chay, nguyên liệu cần tránh, món/nguyên liệu không thích, sở thích đã khai báo và nguyên liệu người dùng cho biết đang có.
- AI được tìm, xếp hạng, giải thích lý do phù hợp, sắp công thức vào menu và đề xuất công thức thay thế; AI không tạo món, công thức, danh sách nguyên liệu hoặc hướng dẫn nấu mới cho người dùng.
- Mỗi kết quả phải dẫn tới bài công thức tương ứng để người dùng kiểm tra trước khi lưu hoặc thêm vào lịch ăn.
- Khi không có công thức phù hợp, hệ thống thông báo không tìm thấy hoặc chỉ trả danh sách chưa đầy đủ kèm lý do; không bịa công thức để lấp kết quả.
- Bản menu AI chỉ là đề xuất. Người dùng chọn từng món hoặc xác nhận menu trước khi lịch ăn thay đổi.

Nguyên liệu “đang có” được nhập theo từng request; MVP không có pantry. AI ưu tiên tận dụng nhưng không bắt buộc dùng hết, phải hiển thị phần còn thiếu. Mọi gói Member dùng các khả năng AI theo quota 5/15/50 và mỗi request thành công, gồm tạo/chỉnh menu, tiêu thụ một lượt.

### 3.18 Hồ sơ nhu cầu dinh dưỡng và kiểm tra menu ngày — đã chốt ranh giới 12/09/2026

**Hồ sơ nhu cầu dinh dưỡng:**

- Phạm vi MVP của chức năng dinh dưỡng chỉ dành cho Member từ đủ 18 tuổi, không mang thai/cho con bú và không cần chế độ ăn điều trị bệnh.
- Trước khi sử dụng, người dùng phải xác nhận mình thuộc phạm vi hỗ trợ. Nếu không thuộc phạm vi, hệ thống giải thích giới hạn và không cung cấp mức nhu cầu, AI menu dinh dưỡng hoặc kiểm tra menu ngày; không đưa ra hướng dẫn điều trị thay thế.
- Việc không đủ điều kiện dùng chức năng dinh dưỡng không khóa các chức năng thông thường: xem/tìm công thức, Đã lưu, Lịch ăn thủ công và AI không-dinh-dưỡng theo quyền gói.
- Hệ thống cho người dùng xem BMI như một chỉ số tham khảo từ chiều cao và cân nặng; BMI không phải chẩn đoán và không được dùng một mình để quyết định nhu cầu calorie, dưỡng chất hoặc cấm/chọn món.
- Hệ thống cung cấp mức calorie và dưỡng chất tham khảo hằng ngày dựa trên hồ sơ cá nhân rộng hơn BMI, gồm các yếu tố cần thiết như tuổi, giới tính, chiều cao, cân nặng và mức vận động. Đối tượng theo giới hạn trên; nguồn tham khảo và lịch nghiên cứu công thức theo Q31 tại 3.20.
- AI sử dụng các mức tham khảo do hệ thống cung cấp để đề xuất và giải thích; AI không tự đặt ra mục tiêu calorie/dưỡng chất không có căn cứ.
- Kết quả phải ghi rõ đây là mức ước tính/tham khảo cho việc lập kế hoạch, không thay thế đánh giá của chuyên gia y tế hoặc dinh dưỡng.

**Chỉ tiêu dinh dưỡng MVP:**

- MVP sử dụng chín chỉ tiêu: **năng lượng (kcal), protein, carbohydrate, tổng chất béo, chất xơ, natri, sắt, canxi và vitamin B12**.
- MVP có một **danh mục nguyên liệu được hỗ trợ tính dinh dưỡng** lưu trong database của ứng dụng. Mỗi mục có giá trị của chín chỉ tiêu theo 100 g cùng thông tin nguồn tham khảo; USDA FoodData Central là nguồn tham khảo chính để nhóm chuẩn bị và kiểm chứng dữ liệu ban đầu.
- Administrator có chức năng quản lý danh mục nguyên liệu dinh dưỡng để bổ sung và duy trì dữ liệu được app hỗ trợ; đây không phải bộ dữ liệu chỉ được nhóm nạp cố định rồi không thể quản lý trong ứng dụng.
- Trong MVP, Administrator được xem/tìm kiếm, thêm, sửa, bật lại hoặc ngừng hỗ trợ một nguyên liệu; xem các công thức đang sử dụng nguyên liệu đó; và quản lý tên nguyên liệu, chín giá trị dinh dưỡng trên 100 g, tên/đường dẫn nguồn cùng ngày tham khảo.
- MVP không có xóa vĩnh viễn nguyên liệu dinh dưỡng, nhập dữ liệu hàng loạt, tự gọi USDA hoặc để AI tự điền số liệu. Hệ thống tính lại công thức/menu liên quan từ dữ liệu hiện hành và ghi nhận thay đổi, nguồn tham khảo.
- App không gọi USDA API trong lúc người dùng sử dụng và không dùng Gemini để tìm, đối chiếu hoặc tạo số liệu dinh dưỡng trong MVP. Việc tính toán chỉ sử dụng dữ liệu đã có trong danh mục nội bộ.
- Dữ liệu gốc để tính dinh dưỡng công thức là nguyên liệu đã liên kết với danh mục được hỗ trợ, định lượng và giá trị thành phần dinh dưỡng tương ứng. Hệ thống cộng dữ liệu của từng nguyên liệu để tạo tổng ước tính cho toàn bộ công thức; không lấy một bảng tổng do tác giả tự khai làm kết quả đã xác minh và AI không tự nghĩ ra số liệu còn thiếu.
- Trang chi tiết công thức cho phép xem tổng ước tính của toàn bộ công thức và phần quy đổi theo **mỗi khẩu phần** (`Nutrition per serving`). Giá trị mỗi khẩu phần được lấy từ tổng công thức chia theo số khẩu phần tác giả đã khai báo, không phải một nguồn dinh dưỡng độc lập.
- Mỗi chỉ tiêu hiển thị lượng, đơn vị và giải thích ngắn về ý nghĩa. Mọi tỷ lệ tham chiếu chung, nếu có, phải ghi rõ nguồn/phạm vi và không được trình bày như nhu cầu cá nhân.
- Năng lượng thể hiện lượng năng lượng khẩu phần cung cấp; protein hỗ trợ mô/cơ; carbohydrate là nguồn năng lượng chính; tổng chất béo cung cấp năng lượng và hỗ trợ hấp thu một số vitamin nhưng không tự phân biệt chất béo tốt/xấu; chất xơ hỗ trợ tiêu hóa và cảm giác no.
- Natri cần được giải thích theo hướng theo dõi giới hạn, không khuyến khích người dùng ăn thêm để đạt 100%. Sắt liên quan đến tạo hemoglobin và vận chuyển oxy; canxi liên quan đến xương, răng, cơ và thần kinh; vitamin B12 liên quan đến tế bào máu, thần kinh và DNA và đặc biệt cần được chú ý trong chế độ vegan.
- Người viết vẫn được nhập nguyên liệu ngoài danh mục dinh dưỡng và gửi bài theo workflow chung, không phải chờ danh mục được bổ sung. Nguyên liệu đó chỉ được đánh dấu **Chưa hỗ trợ tính dinh dưỡng**.
- Nếu nguyên liệu thiếu định lượng, không quy đổi được đơn vị, chưa thuộc danh mục được hỗ trợ hoặc một chỉ tiêu không có dữ liệu đáng tin cậy, giao diện hiển thị **Chưa đủ dữ liệu** và chỉ rõ phạm vi thiếu. Giá trị chưa biết không được coi là bằng 0; nguyên liệu ghi “vừa đủ” không được dùng để suy ra kết quả chính xác.
- MVP không có `Nutrition balance score`, Glycemic Index hoặc Glycemic Load. Các chỉ số này chỉ được xem xét ở Future Scope khi có nguồn dữ liệu và tiêu chí diễn giải đủ rõ.

**AI lập menu theo nhu cầu dinh dưỡng:**

- AI vẫn tuân theo mục 3.17: chỉ chọn bài công thức đang công khai, không bị ẩn/xóa và không tạo công thức mới.
- Chỉ những bài có kết quả dinh dưỡng được tính đầy đủ từ nguyên liệu thuộc danh mục được hỗ trợ, định lượng hợp lệ và số khẩu phần hợp lệ mới được dùng trong menu có mục tiêu dinh dưỡng. Bài có nguyên liệu chưa hỗ trợ vẫn được công khai nhưng không đủ điều kiện cho chức năng này.
- Ràng buộc loại ăn chay và nguyên liệu cần tránh được ưu tiên trước việc tối ưu calorie/dưỡng chất. AI không được chọn món vi phạm ràng buộc chỉ để đạt con số mục tiêu.
- Người dùng xem, thay món và xác nhận trước khi menu được lưu vào lịch ăn.

**Kiểm tra một ngày trong lịch ăn:**

- Người dùng có thể yêu cầu kiểm tra các món đã xếp trong một ngày so với mức calorie và dưỡng chất tham khảo của hồ sơ.
- Khi thêm công thức vào Lịch ăn, người dùng khai báo số khẩu phần dự định ăn; giá trị mặc định là một khẩu phần và người dùng có thể điều chỉnh.
- Kết quả hiển thị theo từng chỉ tiêu: mức tham khảo, tổng từ menu, chênh lệch và trạng thái **Thấp hơn mức tham khảo**, **Trong khoảng tham khảo**, **Cao hơn mức tham khảo** hoặc **Không đủ dữ liệu để đánh giá**.
- Dinh dưỡng của mỗi mục lịch bằng kết quả mỗi khẩu phần của công thức nhân với số khẩu phần dự định ăn. Tổng của ngày là tổng các mục đã xếp trong Bữa sáng, Bữa trưa và Bữa tối, rồi được so sánh với mức tham khảo cá nhân trong hồ sơ; không dùng một mức cố định như 2.000 kcal thay cho mọi người.
- Không dùng một điểm tổng hợp “Tốt/Xấu” để thay thế kết quả từng chỉ tiêu. Bản tóm tắt chỉ nêu số chỉ tiêu trong khoảng, thấp hơn, cao hơn hoặc chưa đủ dữ liệu.
- Cách diễn giải trạng thái phụ thuộc loại chỉ tiêu: natri được theo dõi chủ yếu theo giới hạn tối đa; năng lượng và các chất đa lượng được so với khoảng tham khảo; chỉ tiêu thấp hơn trong một ngày chỉ mô tả menu đã ghi nhận, không kết luận người dùng thiếu chất.
- Nếu một hoặc nhiều món thiếu dữ liệu dinh dưỡng hoặc chưa xác định được lượng ăn, hệ thống phải nêu rõ phạm vi dữ liệu thiếu và không trình bày tổng chưa đầy đủ như kết quả chính xác.
- Kết quả một ngày không được diễn đạt thành chẩn đoán “thiếu chất”, bệnh lý hoặc cam kết tăng/giảm cân. AI có thể giải thích chỉ tiêu và đề xuất thay món, nhưng món thay thế vẫn phải là công thức đang công khai có dữ liệu phù hợp.

Ranh giới này phù hợp với [WHO về BMI người trưởng thành](https://www.who.int/en/news-room/fact-sheets/detail/obesity-and-overweight), [CDC về BMI](https://www.cdc.gov/bmi/faq/index.html), [USDA DRI Calculator](https://www.nal.usda.gov/human-nutrition-and-food-safety/dri-calculator), [NIH về Dietary Reference Intakes](https://ods.od.nih.gov/HealthInformation/nutrientrecommendations/) và [FDA về Nutrition Facts theo khẩu phần và Daily Value](https://www.fda.gov/food/nutrition-facts-label/how-understand-and-use-nutrition-facts-label). Các nguồn cho thấy BMI là chỉ số sàng lọc, nhu cầu calorie/dưỡng chất phụ thuộc nhiều yếu tố cá nhân và số liệu dinh dưỡng phải gắn với khẩu phần; mức khuyến nghị không phải chẩn đoán cho từng người.

**Đã chốt:** khoảng 60 nguyên liệu, quy đổi có nguồn, tính lại dữ liệu liên quan và quyền gói tại 3.20. Công thức, khoảng đánh giá và ảnh hưởng sơ chế/nấu để nghiên cứu khi triển khai Q31. API dinh dưỡng, AI matching, GI/GL và đối tượng ngoài phạm vi vẫn không thuộc MVP.

### 3.19 Tìm nhà hàng chay quanh địa chỉ nhập — OUT_OF_SCOPE (cập nhật 16/09/2026)

Các mô tả dưới đây được giữ làm lịch sử của capability từng được đề xuất, không tạo nghĩa vụ thiết kế, dependency, implementation Issue hoặc test scope trong baseline hiện tại:

- Chỉ Member đã đăng nhập được sử dụng chức năng tìm/đề xuất nhà hàng chay. Guest không được gửi yêu cầu tìm địa điểm; nếu chọn chức năng này thì được yêu cầu đăng nhập.
- Người dùng nhập hoặc chọn một địa chỉ/địa điểm Google Maps làm tâm tìm kiếm, ví dụ `KTX Khu B`. MVP không yêu cầu hoặc đọc GPS/vị trí hiện tại của thiết bị.
- Member chọn ngưỡng khoảng cách đường bộ **500 m, 1 km, 5 km hoặc 10 km**; không dùng đường thẳng thay cho đường bộ. Phương tiện tính đường để thiết kế.
- Hệ thống chuyển địa chỉ đã chọn thành tọa độ và tìm các địa điểm do Google Maps phân loại là `vegetarian_restaurant` hoặc `vegan_restaurant` trong khu vực tương ứng. Đây là tìm kiếm địa điểm, không phải tác vụ Gemini AI và không trừ lượt AI.
- Kết quả nhà hàng được hiển thị trong app để người dùng xem và lựa chọn. App phải ghi rõ dữ liệu địa điểm đến từ Google Maps; kết quả là dữ liệu phân loại của nhà cung cấp, không phải xác nhận độc lập của hệ thống rằng mọi món tại nhà hàng đều là món chay.
- Danh sách này là đề xuất nhà hàng chay theo địa chỉ và bán kính, độc lập với món hoặc bài công thức người dùng đã tìm kiếm. MVP không có chức năng suy ra hay đề xuất cửa hàng/nhà hàng dựa trên món đã tìm.
- Nhà hàng không phải nội dung do hệ thống quản lý. Administrator không tạo, sửa, xóa hoặc xác minh hồ sơ nhà hàng; ứng dụng chỉ hiển thị dữ liệu địa điểm do Google Maps Platform cung cấp. Việc có lưu tạm kết quả để tối ưu kỹ thuật hay không chưa làm thay đổi quyền sở hữu nguồn dữ liệu và sẽ được quyết định ở thiết kế.
- Khi địa chỉ không xác định được, không có kết quả, Google Maps Platform lỗi hoặc hết hạn mức, hệ thống phải thông báo đúng trạng thái thay vì tạo địa điểm giả.
- Google Maps Platform là phụ thuộc bên ngoài có cơ chế tính phí/hạn mức. Nhóm phải kiểm soát quota và không giả định dịch vụ luôn miễn phí. Google mô tả Geocoding dùng để chuyển địa chỉ thành tọa độ, Nearby Search tìm địa điểm theo vùng/bán kính và hỗ trợ loại `vegetarian_restaurant`/`vegan_restaurant`: [Geocoding](https://developers.google.com/maps/documentation/geocoding/geocoding), [Nearby Search](https://developers.google.com/maps/documentation/places/web-service/nearby-search), [Place Types](https://developers.google.com/maps/documentation/places/web-service/place-types), [Pricing](https://developers.google.com/maps/billing-and-pricing/pricing).

**Trạng thái:** `OUT_OF_SCOPE` theo quyết định phạm vi ngày 16/09/2026 — toàn bộ Module M11, gồm FR-42, FR-43 và các BR liên quan đến Google Maps, không thuộc baseline triển khai hiện tại. Ứng dụng không có ý định quản lý hoặc xác minh dữ liệu nhà hàng bên ngoài; không chọn Google Maps làm dependency và không tạo implementation Issue cho capability này. FR-44 là quyền sửa/xóa Recipe Post thuộc M02/M03 và vẫn `ACTIVE`; không thuộc M11.

**Lưu ý truy vết yêu cầu:** nguyên văn đề tài có ý “gợi ý cửa hàng thuần chay liên quan đến món ăn đã tìm kiếm”. Nhóm từng đề xuất thu hẹp thành đề xuất nhà hàng chay quanh địa chỉ nhập theo bán kính đường bộ; quyết định hiện hành là giữ mô tả để bảo toàn lịch sử nhưng loại toàn bộ capability khỏi baseline triển khai. Nếu xem xét lại sau này, nhóm phải có quyết định scope mới và phân rã lại yêu cầu trước khi thiết kế hoặc tạo Issue.

### 3.20 Phạm vi chức năng sau bảng Q01–Q38 — xác nhận 12/09/2026

Mục này ghi quyết định mới nhất của người dùng và thay thế các đề xuất hoặc nhãn “chưa chốt” cũ về cùng vấn đề trong tài liệu phân rã. Chốt chức năng không có nghĩa đã chốt thiết kế hoặc đã triển khai.

| Câu | Quyết định đã xác nhận |
| --- | --- |
| Q01–Q04 | Web responsive; email/password và Google Login; xác minh email bắt buộc, quên/đặt lại mật khẩu. Sau 5 lần đăng nhập sai, rate limit theo cả account identifier và IP trong 10 phút, không đổi thành trạng thái tài khoản `LOCKED` do Admin. Authentication baseline dùng access token ngắn hạn, rotating refresh token, refresh session/revocation phía server; logout thu hồi refresh session. Hồ sơ công khai có tên, avatar, giới thiệu ngắn, bài đăng, ngày tham gia và tổng Like; không công khai email/hồ sơ ăn uống. |
| Q05–Q08 | Bốn loại ăn chay: Vegan, Lacto Vegetarian, Ovo Vegetarian, Lacto-Ovo Vegetarian. Nguyên liệu tránh/không thích chọn danh mục hoặc nhập tự do. Khẩu vị ẩm thực, độ khó và thời gian nấu tối đa tùy chọn. Được sửa hồ sơ, AI dùng dữ liệu mới ở yêu cầu tiếp theo; không tự sửa menu đã lưu. |
| Q09–Q12 | Admin tạo/sửa/ngừng dùng danh mục, một công thức có nhiều danh mục. Đơn vị g, kg, ml, l, tsp, tbsp, cup, piece hoặc “vừa đủ”; dinh dưỡng chỉ tính khi quy đổi gram có căn cứ. Recipe Post áp dụng đúng profile validation tại 3.9: tên 3–120 ký tự, 1–50 nguyên liệu, khẩu phần 1–50, mỗi thời gian 0–1.440 phút và tổng > 0 (`cookTime = 0` hợp lệ nếu `prepTime > 0`), từ 1 đến 30 bước hướng dẫn chuẩn bị/chế biến (nội dung không rỗng sau khi trim), mô tả tối đa 2.000 ký tự (tùy chọn), 5 ảnh JPEG/PNG/WebP tối đa 5 MB/ảnh (tùy chọn) và một link YouTube (tùy chọn). Lưu nháp không thuộc phạm vi hiện tại. |
| Q13–Q14 | Gợi ý bài liên quan thông thường theo danh mục/nguyên liệu không dùng Gemini; tùy chọn người dùng chủ động yêu cầu AI có tính lượt. Không tự gọi AI có tính lượt khi mở bài. Tìm/lọc từ khóa, loại ăn chay, danh mục, nguyên liệu, thời gian; sắp xếp mới nhất/nhiều Like. |
| Q15–Q18 | Bình luận/reply công khai ngay, tối đa 5 cấp; cha bị xóa trở thành tombstone và giữ replies. Một tài khoản tối đa một Like hiệu lực trên mỗi nội dung. Admin hậu kiểm thủ công, ghi lý do và audit/history; không có ma trận chế tài số tự động. In-app notification đi theo business event; email bất đồng bộ/best-effort, lỗi email không rollback hành động gốc; email kết quả moderation phải được thử gửi, email reply có thể theo preference. |
| Q19–Q20 | Free/Plus/Pro và Guest dùng chung AI Chatbot FR-51 (gồm General Context và Recipe Context đối với Recipe công khai), khác hạn mức 5/15/50 lượt thành công/ngày cho Member và tối đa 5 lượt/ngày cho Guest. Reset lúc 00:00 `Asia/Ho_Chi_Minh`; Member theo account; Guest theo anonymous cookie kết hợp coarse IP rate limiting; Guest không có lịch sử hội thoại theo tài khoản hoặc hồ sơ dinh dưỡng cá nhân. Chỉ request thành công mới trừ lượt; telemetry không lưu raw prompt và giữ 90 ngày. |
| Q21–Q24 | AI đề xuất 7 ngày, sáng/trưa/tối, người dùng chọn/sửa/bỏ trước khi lưu. Nguyên liệu đang có nhập theo từng yêu cầu, không có kho nguyên liệu. Ưu tiên tận dụng, không bắt dùng hết, hiển thị phần thiếu. AI chỉ chọn công thức hiện có; chatbot giải thích thay thế nguyên liệu nhưng không tự sửa công thức/dinh dưỡng hoặc tạo công thức mới. |
| Q25–Q26 | Lịch sử hội thoại AI theo tài khoản không thuộc phạm vi hiện tại. Subscription/payment `ACTIVE`: FREE 0 VND/tháng, PLUS 49,000 VND/tháng, PRO 99,000 VND/tháng; chỉ VND, chu kỳ tháng, không tự động gia hạn, không hoàn tiền một phần. Entitlement chỉ kích hoạt sau thanh toán được xác minh, hết hạn cuối kỳ đã trả; xử lý thanh toán trùng phải idempotent. Payment provider được chọn ở thiết kế tích hợp. |
| Q27–Q30 | Baseline MVP dùng nguồn tham khảo USDA/NIH, tính từ gram nguyên liệu và khẩu phần, rồi hiển thị thông tin tham khảo cá nhân; không tự kê mục tiêu calorie/macro chỉ dựa vào BMI hoặc mục tiêu cân nặng. Hành vi goal-adjusted nâng cao chỉ là stretch nếu còn thời gian, không phải MVP acceptance. Khoảng 60 nguyên liệu có nguồn cho demo; thiếu dữ liệu không coi là 0; thay đổi dữ liệu phải được tính lại và ghi nguồn. |
| Q31 | Chỉ ghi nguồn trong SRS; nghiên cứu/chốt công thức và khoảng đánh giá khi triển khai chức năng dinh dưỡng. Không yêu cầu nghiên cứu công thức ngay để tiếp tục tài liệu. |
| Q32–Q34 | `OUT_OF_SCOPE` — M11 và tích hợp Google Maps không thuộc baseline triển khai hiện tại; giữ nội dung để bảo toàn lịch sử, không tạo implementation Issue. |
| Q35 | `OUT_OF_SCOPE` — chức năng nhà hàng chay không thuộc baseline triển khai hiện tại. |
| Q36–Q38 | Giao diện tiếng Việt. AI gắn cờ là hạng mục DEFERRED, không phải nghiệm thu MVP. Ba actor người dùng: Guest, Member, Administrator; không có actor hoặc quyền Contributor riêng. |

### 3.21 Phân rã chức năng Đăng Blog cộng đồng nhúng công thức (Mô hình Samsung Food — Ngoài phạm vi MVP)

Nhằm chuẩn bị mở rộng hệ thống thành mạng xã hội ẩm thực chay thu nhỏ sau khi hoàn tất các chức năng MVP, chức năng Đăng Blog thật được phân rã chi tiết như sau:

- **Trang Blog độc lập:** Hệ thống có trang chuyên mục `/blog` riêng biệt với trang Khám phá công thức `/recipes`. Trang này hiển thị các bài viết chia sẻ phong cách sống, kiến thức dinh dưỡng, văn hóa chay hoặc cẩm nang theo các chủ đề: *All, Meal Plans, Recipes, Health, Ideas, News*.
- **Cấu trúc bài Blog (`Blog Post`):**
  - Tiêu đề (`title`), ảnh đại diện bài viết (`cover_image`), danh mục/thẻ (`category/tags`), tóm tắt ngắn (`summary`).
  - Nội dung chính (`content`): Dạng văn bản giàu định dạng (Rich Text / Markdown) hỗ trợ tiêu đề phụ, đoạn văn xuôi, danh sách và hình ảnh minh họa trong thân bài.
  - **Khối nhúng thẻ công thức (`Embedded Recipe Card`):** Trong bài blog, tác giả có thể gắn (tag) hoặc nhúng trực tiếp một hoặc nhiều bài `Recipe Post` công khai của hệ thống. Thẻ này hiển thị ảnh món, tên món, tác giả công thức, thời gian nấu, tóm tắt nguyên liệu và kèm theo các nút hành động nhanh:
    - Nút **"Xem công thức chi tiết"** (điều hướng sang trang công thức).
    - Nút **"Lưu công thức"** (thêm nhanh vào `Saved Recipes` của người đọc).
    - Nút **"Lập lịch ăn"** (mở popup thêm món vào `Meal Planner` tuần).
- **Phân quyền và tương tác:**
  - Guest: Đọc bài blog công khai, xem các công thức được nhúng.
  - Member: Soạn thảo, đăng tải, chỉnh sửa, xóa bài blog của chính mình; tìm kiếm và nhúng công thức công khai vào bài blog; Like và bình luận trao đổi trong bài blog.
  - Administrator: Quản lý danh mục blog; tiếp nhận báo cáo vi phạm và xử lý (cảnh báo, ẩn bài, xóa bài blog vi phạm).
- **Ranh giới thực hiện:** Chức năng này là `OUT_OF_SCOPE` trong baseline hiện tại. Nội dung được giữ để bảo toàn định hướng lịch sử, không tạo implementation Issue; muốn đưa lại vào scope phải có quyết định phạm vi và phân rã mới.

### 3.22 Phân rã chức năng Quản lý kho thực phẩm cá nhân (Pantry/Inventory) và đề xuất từ kho — Ngoài phạm vi MVP (`OUT_OF_SCOPE`)

Nhằm định hướng phát triển tính năng chống lãng phí thực phẩm và tận dụng nguyên liệu sẵn có trong gia đình sau khi hoàn thành MVP, chức năng Quản lý kho thực phẩm cá nhân (Pantry/Inventory) được phân rã chi tiết như sau:

- **Quản lý kho thực phẩm cá nhân của Member (`Personal Pantry`):**
  - Member có thể thêm, chỉnh sửa, xóa các loại thực phẩm, rau củ, đồ khô hoặc gia vị hiện có trong tủ lạnh/bếp nhà mình vào danh sách quản lý kho trên ứng dụng.
  - Thông tin mỗi mục thực phẩm gồm: Tên nguyên liệu (ưu tiên liên kết với danh mục nguyên liệu chuẩn), số lượng/khối lượng, đơn vị tính, và ngày hết hạn tùy chọn (`expiry_date`) để cảnh báo thực phẩm sắp hỏng.
- **Tìm kiếm và đề xuất công thức/bài viết từ kho thực phẩm (`Inventory-based Search`):**
  - Hệ thống hỗ trợ thuật toán so khớp nguyên liệu giữa kho cá nhân với cơ sở dữ liệu `Recipe Post` (hoặc bài blog) công khai của cộng đồng.
  - Phân loại kết quả tìm kiếm:
    - *Nấu được ngay (100% khớp):* Các món mà người dùng đã có đầy đủ toàn bộ nguyên liệu bắt buộc.
    - *Thiếu ít nguyên liệu:* Các món chỉ thiếu từ 1 đến 2 nguyên liệu; hệ thống hiển thị rõ các nguyên liệu còn thiếu và cung cấp nút bấm nhanh: *"Thêm nguyên liệu thiếu vào Shopping List"*.
- **AI sáng tạo công thức món chay mới từ nguyên liệu trong kho (`AI Pantry Recipe Generator`):**
  - Member có thể chọn một nhóm nguyên liệu đang có trong kho thực phẩm và yêu cầu AI (Gemini) gợi ý một công thức món chay mới sáng tạo, phù hợp với chế độ ăn chay đã đăng ký (Vegan, Lacto, Ovo, v.v.), giúp giải quyết thực phẩm tồn đọng mà không cần đi chợ.
- **Phân quyền và ranh giới:**
  - Guest: Không có quyền sử dụng kho thực phẩm cá nhân.
  - Member: Toàn quyền quản lý kho cá nhân của mình, tìm kiếm món phù hợp và gọi AI sáng tạo công thức theo hạn mức của gói.
  - Ranh giới thực hiện: Gắn nhãn **`OUT_OF_SCOPE` đối với MVP ban đầu**. Lý do: Yêu cầu quản lý trạng thái tồn kho phức tạp (trừ kho khi nấu, hạn sử dụng, cảnh báo hết hạn), thuật toán so khớp tập hợp nguyên liệu và đồng bộ trạng thái; việc này sẽ được phát triển ở giai đoạn nâng cao sau khi các luồng cốt lõi đã hoàn thiện.

**Nguồn ghi nhận cho Q31:** [USDA FoodData Central](https://fdc.nal.usda.gov/) cho thành phần nguyên liệu; [USDA DRI Calculator](https://www.nal.usda.gov/human-nutrition-and-food-safety/dri-calculator) và [NIH DRI](https://ods.od.nih.gov/HealthInformation/nutrientrecommendations/) cho tham khảo nhu cầu dinh dưỡng. Ghi nguồn không đồng nghĩa chốt công thức hay tích hợp API runtime. Trước khi đưa tính toán thật vào sử dụng, phải chọn công thức/hệ số có căn cứ, giới hạn đối tượng và kiểm thử; không dùng số đoán hoặc để Gemini tự đặt mục tiêu.

## 4. Actors và quyền cấp cao

Bảng dưới là baseline cấp cao; các gói Free/Plus/Pro là quyền sử dụng, không mặc định tạo role mới. Guest là trạng thái chưa xác thực, không phải tài khoản hoặc gói. Member vẫn giữ role Member dù bỏ qua Onboarding; chức năng AI cá nhân hóa kiểm tra dữ liệu tối thiểu thay vì tạo role mới.

| Actor | Trạng thái | Mục tiêu chính | Không được làm |
| --- | --- | --- | --- |
| Guest | Chưa đăng nhập | Xem bài công thức công khai, tìm kiếm và dùng Guest Free AI | Đăng/bình luận, gửi báo cáo bài, xem lịch sử, lập thực đơn cá nhân hóa. |
| Member (Authorized User) | Đã đăng nhập | Tương tác cộng đồng, tạo/công khai Recipe Post trực tiếp, báo cáo bài có vấn đề, lưu công thức, quản lý lịch ăn, dùng AI theo gói | Tự duyệt nội dung hoặc quản trị người khác. |
| Administrator | Quản trị | Xử lý báo cáo bài; quản lý thành viên, bài công thức, bình luận, danh mục và danh mục nguyên liệu dinh dưỡng | Không duyệt quyền đăng hoặc từng bài trước khi công khai; không được để AI tự quyết định chế tài hoặc tự tạo số liệu dinh dưỡng. |
| Gemini AI | Dịch vụ ngoài hệ thống | Tạo phản hồi AI khi backend cho phép | Không có quyền hệ thống, không truy cập database trực tiếp, không tự công khai nội dung. |

Các external/supporting actor dưới đây không phải tài khoản đăng nhập và không thay thế quyền của Guest, Member hoặc Administrator:

| Dịch vụ ngoài | Trách nhiệm được dùng trong phạm vi hiện tại | Ranh giới |
| --- | --- | --- |
| Azure Blob Storage | Lưu và phục vụ ảnh Recipe Post theo tham chiếu do ứng dụng quản lý | Không lưu video upload trong Phase 1; không tự quyết định quyền truy cập ứng dụng. |
| YouTube | Phát video nhúng từ link/video ID hợp lệ | Ứng dụng không upload, sao chép hoặc bảo đảm mọi video luôn cho phép nhúng. |
| Google Maps Platform | Phụ thuộc lịch sử của M11/FR-42/FR-43 đã `OUT_OF_SCOPE` | Không được chọn làm dependency của baseline hiện tại; ứng dụng không quản lý hoặc xác minh dữ liệu nhà hàng bên ngoài. |
| Google authentication và dịch vụ email | Hỗ trợ Google Login, xác minh email, đặt lại mật khẩu và thông báo đã xác nhận | Nhà cung cấp và chi tiết luồng triển khai chưa được SRS này tự suy diễn. |
| Payment provider | Xác minh thanh toán thật trước khi kích hoạt quyền Plus/Pro | Provider cụ thể là quyết định kỹ thuật; giá VND, chu kỳ tháng, không tự động gia hạn, hết hạn entitlement, không hoàn tiền một phần và idempotency đã chốt tại 3.20. |

## 5. Phân rã dự án theo module

| Mã module | Module | Giá trị | Phụ thuộc chính | Trạng thái phân rã |
| --- | --- | --- | --- | --- |
| M01 | Public Recipe Discovery | Khách tìm/xem bài công thức công khai, gồm bài có video YouTube được nhúng | Nội dung đã công khai | Cần viết user stories. |
| M02 | Identity & Access | Phân biệt Guest/User/Admin và bảo vệ thao tác | Tài khoản, role, session | Đã chốt email/password, Google Login, xác minh email và quên mật khẩu. |
| M03 | Recipe Contribution & Post-moderation | Member quản lý/công khai bài trực tiếp; Like/Unlike và reply tối đa 5 cấp; Admin hậu kiểm thủ công | M02, Azure Blob, quy trình báo cáo | Validation, tombstone, lý do và audit/history đã chốt; còn User Story/Use Case/flow/AC và chi tiết UI. |
| M04 | Recipe Classification & Discovery | Phân loại, tìm và lọc bài công thức chay | M01, M03 | Dữ liệu và profile validation đã chốt; còn User Story/Use Case/flow/AC. |
| M05 | Saved Recipes, Meal Planning & Shopping List | Lưu công thức để xem lại, xếp món vào lịch ăn 7 ngày ba bữa cố định và tạo/quản lý danh sách mua sắm nguyên liệu cơ bản | M02, M04 | Đã chốt ranh giới Saved/Planner/Shopping List; quy tắc gom an toàn và xuất .txt đã xác nhận. |
| M06 | Gemini AI Access & Usage | Guest/Free/Plus/Pro gọi AI và đo usage thật | M02 một phần, Gemini backend | Đã có hướng; cần user stories. |
| M07 | AI-assisted Moderation | AI rà soát/gắn cờ Recipe Post nhưng không tự áp dụng chế tài | M03, M06 | DEFERRED; không thuộc MVP hiện tại. |
| M08 | Subscription Administration | Hiển thị/đổi gói và quyền AI | M02, M06 | `ACTIVE`; FREE 0, PLUS 49,000, PRO 99,000 VND/tháng; payment provider còn chọn ở thiết kế. |
| M09 | Administration | Xử lý báo cáo, quản lý user/bài công thức/danh mục/bình luận và danh mục nguyên liệu dinh dưỡng | M02–M07, M10 | Không duyệt trước; quyết định thủ công có lý do, giữ audit/history; còn decomposition và thiết kế lưu trữ/UX. |
| M10 | Nutrition Profile & Daily Menu Check | Với Member đủ điều kiện, tính chín chỉ tiêu từ danh mục nguyên liệu dinh dưỡng nội bộ, quy đổi theo khẩu phần và đối chiếu tổng ba bữa với hồ sơ | M02, M04, M05 | Đã chốt đối tượng, chỉ tiêu, nguồn dữ liệu cấp MVP và cách cộng menu; còn dữ liệu khởi tạo, quy đổi và khoảng tham khảo chi tiết khi triển khai. |
| M11 | Nearby Vegetarian Restaurant Discovery | Capability lịch sử về tìm nhà hàng chay qua Google Maps; không thuộc baseline triển khai hiện tại | Không có dependency trong baseline hiện tại | `OUT_OF_SCOPE` — ứng dụng không quản lý dữ liệu nhà hàng bên ngoài; chỉ xem xét lại sau một quyết định scope và phân rã mới. |
| M12 | Community Blog & Recipe Embedding | Trang Blog độc lập cho Member đăng bài văn xuôi chia sẻ kinh nghiệm và nhúng thẻ Recipe Post công khai | M01, M03 | OUT_OF_SCOPE — Ngoài phạm vi MVP ban đầu; chỉ phát triển sau khi các module cốt lõi hoàn thành. |
| M13 | Personal Pantry & Inventory-based Recipe Recommendation | Quản lý kho thực phẩm cá nhân của Member, tìm kiếm công thức phù hợp từ nguyên liệu có sẵn và hỗ trợ AI sáng tạo món mới từ kho | M02, M03, M06 | OUT_OF_SCOPE — Ngoài phạm vi MVP ban đầu; ghi nhận định hướng sau khi hoàn thành các module cốt lõi. |

## 6. Thứ tự sử dụng baseline cho thiết kế và kiểm thử

Không thiết kế ERD hoặc API bằng giả định nằm ngoài requirement. Thứ tự cần giữ:

1. Dùng root registry để xác nhận requirement tồn tại, module và lifecycle chính thức.
2. Dùng FR/BR/NFR chi tiết cùng Use Case và Acceptance Criteria hiện có để xác định hành vi, lỗi, quyền và ràng buộc.
3. Bổ sung User Journey/User Story mapping khi cần cho lập kế hoạch Issue, nhưng không được thay đổi nghĩa của requirement nguồn.
4. Từ requirement và Business Rules, xác định entity/thuộc tính/quan hệ để lập ERD; không thêm bảng không phục vụ yêu cầu.
5. Thiết kế API contract từ các luồng đã chốt và giữ traceability tới FR/BR/NFR liên quan.
6. Chuyển Acceptance Criteria thành test cases và duy trì traceability `FR → Use Case/User Story → Test Case`.

## 7. Danh mục Functional Requirements và Authoritative Lifecycle Registry

Tài liệu này là **Authoritative Registry** cho sự tồn tại của requirement, mã định danh ổn định (stable ID), phân bổ module ở cấp chỉ mục, và trạng thái vòng đời (lifecycle state) chính thức. Đặc tả chi tiết từng FR (statements, actors, preconditions, exceptions, acceptance criteria) được quản lý có thẩm quyền tại [FUNCTIONAL-REQUIREMENTS.md](srs/FUNCTIONAL-REQUIREMENTS.md).

### 7.1 Quy tắc lifecycle trong baseline hiện tại

Vocabulary duy nhất dùng cho requirement lifecycle là `DRAFT`, `ACTIVE`, `DEFERRED`, `OUT_OF_SCOPE` và `RETIRED`. Theo DEC-001–003, các FR thuộc M01–M06 và M09–M10 cùng M08 subscription/payment là `ACTIVE`, trừ requirement đã được xác nhận `DEFERRED`, `OUT_OF_SCOPE` hoặc `RETIRED`. M11 là `OUT_OF_SCOPE` theo quyết định phạm vi ngày 16/09/2026. Registry dưới đây là baseline có thẩm quyền; lifecycle không biểu thị mức độ phân rã hay implementation readiness.

### 7.2 Functional Requirements Registry

| ID | Short Name | Module | Lifecycle | Detail |
| --- | --- | --- | --- | --- |
| FR-01 | Guest xem và tìm kiếm nội dung công khai | M01 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-01) |
| FR-02 | Quyền Guest trải nghiệm AI Chatbot chung theo Guest Free quota | M06 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-02) |
| FR-03 | Đăng ký, đăng nhập và quản lý tài khoản cơ bản | M02 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-03) |
| FR-04 | Member tạo và công khai trực tiếp Recipe Post | M02, M03 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-04) |
| FR-05 | Đơn xin quyền đăng và Administrator duyệt bài | M02, M03, M09 | RETIRED | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-05) |
| FR-06 | Administrator xử lý báo cáo và quản lý hậu kiểm | M09 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-06) |
| FR-07 | Administrator tạo, cập nhật và phân loại bài công thức | M04 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-07) |
| FR-08 | Tìm kiếm và lọc bài công thức đa tiêu chí | M04 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-08) |
| FR-09 | Authorized User tạo và chỉnh lịch ăn tuần | M05 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-09) |
| FR-10 | Áp dụng hạn mức Gemini AI theo gói tài khoản | M06 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-10) |
| FR-11 | Lưu lượt gọi AI thành công và token usage metadata | M06 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-11) |
| FR-12 | AI rà soát và gắn cờ Recipe Post nghi vấn | M07 | DEFERRED | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-12) |
| FR-13 | Đăng ký gói AI qua thanh toán thật | M08 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-13) |
| FR-14 | Lưu trữ ảnh bài công thức trên Azure Blob Storage | M03 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-14) |
| FR-15 | Nhúng trình phát YouTube trong bài công thức | M01, M03 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-15) |
| FR-16 | Cấu trúc dữ liệu bài công thức và tính bắt buộc của bước hướng dẫn chuẩn bị/chế biến | M03, M04 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-16) |
| FR-17 | Trình bày thẻ món trong Khám phá và liên kết lịch ăn | M04, M05 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-17) |
| FR-18 | Admin quản lý danh mục nguyên liệu và món ăn | M04, M09 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-18) |
| FR-19 | Nhập nguyên liệu linh hoạt và định lượng | M03, M04 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-19) |
| FR-20 | Thống nhất nguồn hiển thị thẻ món, chi tiết và thực đơn | M01, M04, M05 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-20) |
| FR-21 | AI hỗ trợ tạo giới thiệu hoặc bước hướng dẫn chuẩn bị/chế biến không lưu nháp server | M03, M06 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-21) |
| FR-22 | Thao tác chỉnh sửa và sắp xếp bước hướng dẫn chuẩn bị/chế biến | M01, M03, M06 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-22) |
| FR-23 | Hiển thị thông tin tác giả gắn liền với tài khoản | M01, M02, M03 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-23) |
| FR-24 | Lưu nháp Recipe Post chưa đầy đủ trên server | M03 | OUT_OF_SCOPE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-24) |
| FR-25 | Công khai Recipe Post ngay sau khi validation đạt | M02, M03 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-25) |
| FR-26 | Gửi báo cáo bài công thức có vấn đề | M02, M03, M09 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-26) |
| FR-27 | Biểu mẫu báo cáo bài công thức theo 6 nhóm lý do | M03 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-27) |
| FR-28 | Administrator xử lý báo cáo qua các trạng thái | M02, M03, M09 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-28) |
| FR-29 | Phân quyền hiển thị thông tin báo cáo | M02, M03, M09 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-29) |
| FR-30 | Bổ sung thông tin và ngăn trùng lặp báo cáo mở | M03, M09 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-30) |
| FR-31 | Điều kiện thông tin tối thiểu trước khi dùng AI cá nhân hóa | M02, M05, M06 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-31) |
| FR-32 | Lưu, bỏ lưu và quản lý danh sách công thức đã lưu | M02, M04, M05 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-32) |
| FR-33 | Thêm và quản lý bài công thức trong lịch ăn 3 bữa | M02, M04, M05 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-33) |
| FR-34 | AI gợi ý món và lập menu từ công thức công khai | M04, M05, M06 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-34) |
| FR-35 | Khai báo hồ sơ dinh dưỡng và xem chỉ số tham khảo cá nhân | M02, M10 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-35) |
| FR-36 | AI lập menu theo nhu cầu dinh dưỡng từ công thức tin cậy | M04, M05, M06, M10 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-36) |
| FR-37 | Khai báo khẩu phần và kiểm tra menu ngày theo 9 chỉ tiêu | M05, M06, M10 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-37) |
| FR-38 | Xác nhận phạm vi hỗ trợ trước khi dùng chức năng dinh dưỡng | M02, M10 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-38) |
| FR-39 | Tính toán ước tính 9 chỉ tiêu dinh dưỡng cho công thức | M03, M04, M10 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-39) |
| FR-40 | Công khai Recipe Post chứa nguyên liệu ngoài danh mục dinh dưỡng | M03, M04, M06, M10 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-40) |
| FR-41 | Administrator quản lý danh mục nguyên liệu dinh dưỡng | M09, M10 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-41) |
| FR-42 | Member tìm nhà hàng chay theo địa chỉ và bán kính đường bộ | M02, M11 | OUT_OF_SCOPE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-42) |
| FR-43 | Hiển thị danh sách và bản đồ nhúng nhà hàng từ Google | M11 | OUT_OF_SCOPE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-43) |
| FR-44 | Member sửa hoặc xóa Recipe Post đã công khai của chính mình | M02, M03 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-44) |
| FR-45 | Like và Unlike bài công thức, bình luận và phản hồi | M01, M02, M03 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-45) |
| FR-46 | Hiển thị và quản lý bình luận, reply lồng nhiều cấp | M01, M02, M03, M09 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-46) |
| FR-47 | Gợi ý bài công thức liên quan thông thường và tùy chọn Gemini | M04, M06 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-47) |
| FR-48 | Báo cáo bình luận, reply và Administrator hậu kiểm | M03, M09 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-48) |
| FR-49 | Gửi thông báo trong app và email cho reply mới và kết quả báo cáo | M02, M03, M09 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-49) |
| FR-50 | Xem và xóa lịch sử hội thoại AI riêng theo tài khoản | M02, M06 | OUT_OF_SCOPE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-50) |
| FR-51 | AI Chatbot hỗ trợ hỏi đáp ẩm thực chay theo ngữ cảnh | M06, M10 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-51) |
| FR-52 | Tạo, sửa, xóa Blog văn xuôi và nhúng thẻ Recipe Post | M01, M03, M12 | OUT_OF_SCOPE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-52) |
| FR-53 | Tạo và quản lý Shopping List checklist tương tác | M05 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-53) |
| FR-54 | Tự động tổng hợp nguyên liệu trùng an toàn trong Shopping List | M05 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-54) |
| FR-55 | Sao chép clipboard và xuất file text Shopping List | M05 | ACTIVE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-55) |
| FR-56 | Quản lý kho thực phẩm cá nhân và đề xuất món từ kho | M05, M06, M13 | OUT_OF_SCOPE | [Chi tiết](srs/FUNCTIONAL-REQUIREMENTS.md#fr-56) |

## 8. Business Rules và Authoritative Lifecycle Registry

Tài liệu này là **Authoritative Registry** cho sự tồn tại, mã ID và trạng thái lifecycle của Business Rules. Đặc tả chi tiết từng quy tắc nghiệp vụ được quản lý có thẩm quyền tại [BUSINESS-RULES.md](srs/BUSINESS-RULES.md).

### 8.1 Quy tắc lifecycle gate

Theo DEC-001–003, các BR hỗ trợ MVP thuộc M01–M06 và M09–M10 cùng M08 là `ACTIVE`, trừ quy tắc đã được xác nhận `DEFERRED`, `OUT_OF_SCOPE` hoặc `RETIRED`. Toàn bộ BR của M11 là `OUT_OF_SCOPE` theo quyết định phạm vi ngày 16/09/2026. Registry dưới đây là baseline lifecycle có thẩm quyền; mức độ chi tiết của BR được đánh giá riêng trong Phase 2.

### 8.2 Business Rules Registry

| ID | Short Name | Module | Lifecycle | Detail |
| --- | --- | --- | --- | --- |
| BR-01 | Hạn mức text AI cho Guest và tài khoản Free | M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-01) |
| BR-02 | Hạn mức text AI cho gói Plus và Pro | M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-02) |
| BR-03 | Điều kiện trừ hạn mức AI | M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-03) |
| BR-04 | Xử lý lỗi provider và timeout AI | M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-04) |
| BR-05 | Giới hạn tính năng đối với Guest | M01, M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-05) |
| BR-06 | Bảo mật Gemini API Key | M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-06) |
| BR-07 | Đăng và công khai Recipe Post trực tiếp | M02, M03 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-07) |
| BR-08 | Giới hạn vai trò AI gắn cờ Recipe Post | M07 | DEFERRED | [Chi tiết](srs/BUSINESS-RULES.md#br-08) |
| BR-09 | Ranh giới y tế và sức khỏe của câu trả lời AI | M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-09) |
| BR-10 | Giới hạn định dạng video Phase 1 | M01, M03 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-10) |
| BR-11 | Media công khai trực tiếp và hậu kiểm | M03 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-11) |
| BR-12 | Tính tùy chọn của liên kết nguyên liệu chuẩn | M03, M04 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-12) |
| BR-13 | Xử lý nguyên liệu chưa nhận diện trong ràng buộc ăn uống | M04, M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-13) |
| BR-14 | Quy tắc định lượng nguyên liệu và tổng hợp Shopping List an toàn | M03, M05, M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-14) |
| BR-15 | Đồng nhất chính sách bài viết tự soạn và bài có AI hỗ trợ | M03, M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-15) |
| BR-16 | Không chặn công khai bài viết khi AI lỗi hoặc hết hạn mức | M03, M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-16) |
| BR-17 | Gắn quyền tác giả với tài khoản đăng bài | M02, M03 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-17) |
| BR-18 | Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai | M01, M02 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-18) |
| BR-19 | Điều kiện bắt buộc để công khai Recipe Post | M03 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-19) |
| BR-20 | Tính tùy chọn của mô tả giới thiệu và ảnh đại diện | M03, M04 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-20) |
| BR-21 | Quy tắc đơn xin quyền đăng bài công thức | M02, M09 | RETIRED | [Chi tiết](srs/BUSINESS-RULES.md#br-21) |
| BR-22 | Lý do từ chối hoặc thu hồi quyền đăng bài | M09 | RETIRED | [Chi tiết](srs/BUSINESS-RULES.md#br-22) |
| BR-23 | Bản chất của báo cáo vi phạm từ người dùng | M03, M09 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-23) |
| BR-24 | Xác thực tài khoản khi gửi báo cáo vi phạm | M02, M03 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-24) |
| BR-25 | Yêu cầu lý do và mô tả trong biểu mẫu báo cáo | M03 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-25) |
| BR-26 | Thẩm quyền xử lý báo cáo và áp dụng chế tài | M02, M09 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-26) |
| BR-27 | Quy tắc phục hồi bài công thức bị ẩn | M03, M09 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-27) |
| BR-28 | Bảo mật danh tính người báo cáo và tính riêng tư của báo cáo | M03, M09 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-28) |
| BR-29 | Chống tạo báo cáo trùng lặp trên cùng một bài viết | M03, M09 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-29) |
| BR-30 | Ranh giới chức năng khi bỏ qua Onboarding | M02, M05 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-30) |
| BR-31 | Không gọi AI và không trừ hạn mức khi chặn do thiếu hồ sơ | M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-31) |
| BR-32 | Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn | M02, M05 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-32) |
| BR-33 | Thao tác lưu công thức không tiêu thụ hạn mức AI | M04, M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-33) |
| BR-34 | Tính duy nhất của bản ghi lưu công thức | M04 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-34) |
| BR-35 | Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn | M04, M05 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-35) |
| BR-36 | Quy tắc 3 loại bữa ăn cố định trong Lịch ăn MVP | M05 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-36) |
| BR-37 | Tính duy nhất của công thức trong cùng một bữa ăn ngày | M05 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-37) |
| BR-38 | AI không tự tạo công thức mới ngoài hệ thống | M04, M06 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-38) |
| BR-39 | Vai trò tham khảo của chỉ số BMI | M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-39) |
| BR-40 | Điều kiện dữ liệu dinh dưỡng tin cậy cho AI menu | M06, M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-40) |
| BR-41 | Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia | M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-41) |
| BR-42 | Đối tượng loại trừ khỏi tính toán nhu cầu dinh dưỡng MVP | M02, M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-42) |
| BR-43 | Gắn số liệu dinh dưỡng với một khẩu phần | M03, M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-43) |
| BR-44 | Quy tắc giải thích chỉ tiêu natri và năng lượng | M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-44) |
| BR-45 | Cấm hiển thị điểm tổng hợp hoặc nhãn đơn giản hóa | M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-45) |
| BR-46 | Nguồn tính toán dinh dưỡng chính thức của công thức | M03, M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-46) |
| BR-47 | Phân bổ dinh dưỡng theo số khẩu phần và cộng dồn ngày | M05, M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-47) |
| BR-48 | Xử lý nguyên liệu thiếu định lượng hoặc thiếu số liệu dinh dưỡng | M03, M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-48) |
| BR-49 | Không gọi API dinh dưỡng ngoài realtime và nguồn tham khảo | M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-49) |
| BR-50 | Cho phép công khai bài chứa nguyên liệu ngoài danh mục dinh dưỡng | M03, M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-50) |
| BR-51 | Thẩm quyền quản lý danh mục nguyên liệu dinh dưỡng | M09, M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-51) |
| BR-52 | Yêu cầu đầy đủ 9 chỉ tiêu và nguồn trước khi kích hoạt | M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-52) |
| BR-53 | Cấm xóa vĩnh viễn nguyên liệu dinh dưỡng đã tham chiếu | M09, M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-53) |
| BR-54 | Cấm nhập hàng loạt tự động vào danh mục dinh dưỡng | M10 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-54) |
| BR-55 | Không sử dụng GPS thiết bị khi tìm nhà hàng | M11 | OUT_OF_SCOPE | [Chi tiết](srs/BUSINESS-RULES.md#br-55) |
| BR-56 | Nguồn dữ liệu nhà hàng từ Google Maps Platform | M11 | OUT_OF_SCOPE | [Chi tiết](srs/BUSINESS-RULES.md#br-56) |
| BR-57 | Xử lý lỗi không tìm thấy địa chỉ hoặc vượt hạn mức Google | M11 | OUT_OF_SCOPE | [Chi tiết](srs/BUSINESS-RULES.md#br-57) |
| BR-58 | Độc lập giữa thao tác gửi báo cáo và quyết định xử lý | M03, M09 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-58) |
| BR-59 | Không có hàng đợi duyệt bài trước khi công khai | M02, M03 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-59) |
| BR-60 | Xác nhận trách nhiệm trước khi xin quyền đăng bài | M02 | RETIRED | [Chi tiết](srs/BUSINESS-RULES.md#br-60) |
| BR-61 | Giới hạn số đơn xin quyền đăng bài Chờ duyệt | M02, M09 | RETIRED | [Chi tiết](srs/BUSINESS-RULES.md#br-61) |
| BR-62 | Kiểm tra quyền tác giả và validation khi sửa bài công thức | M02, M03 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-62) |
| BR-63 | Đề xuất nhà hàng theo bán kính và không duy trì danh mục riêng | M11 | OUT_OF_SCOPE | [Chi tiết](srs/BUSINESS-RULES.md#br-63) |
| BR-64 | Quyền sửa và xóa bài công thức của chính tác giả | M02, M03 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-64) |
| BR-65 | Quy tắc mỗi Member tối đa một Like hiệu lực | M01, M02 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-65) |
| BR-66 | Quy tắc liên kết và phân quyền với reply bình luận | M01, M03 | ACTIVE | [Chi tiết](srs/BUSINESS-RULES.md#br-66) |
| BR-67 | Xác thực Member trước khi gọi Google Maps Platform | M02, M11 | OUT_OF_SCOPE | [Chi tiết](srs/BUSINESS-RULES.md#br-67) |
| BR-68 | 4 ngưỡng bán kính tìm nhà hàng cố định | M11 | OUT_OF_SCOPE | [Chi tiết](srs/BUSINESS-RULES.md#br-68) |

## 9. Non-functional Requirements Registry

Tài liệu này duy trì bảng chỉ mục tóm tắt các nhóm yêu cầu phi chức năng. Theo DEC-004, `NFR-01`–`NFR-25` là `ACTIVE`; `NFR-26` và `NFR-27` là `OUT_OF_SCOPE`. Lifecycle không có nghĩa mọi chi tiết kỹ thuật đã được thiết kế. Toàn bộ 27 NFR được duy trì chi tiết tại [NON-FUNCTIONAL-REQUIREMENTS.md](srs/NON-FUNCTIONAL-REQUIREMENTS.md).

| Nhóm | Mã NFR chi tiết | Trọng tâm yêu cầu | Tiêu chí đo lường chính | Chi tiết |
| --- | --- | --- | --- | --- |
| Performance | NFR-01 – NFR-05 | Phản hồi đăng nhập, tìm kiếm, Chatbot AI, tạo thực đơn tuần; chịu tải đồng thời | Login $\le 2$s (P95); Search $\le 3$s; Chatbot $\le 5$s (P90 $\le 7$s); Menu $\le 8$s; nghiệm thu 50 concurrent users, 100 là stretch; 500 CCU chỉ là design goal | [Chi tiết](srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-performance) |
| Security | NFR-06 – NFR-10 | Mã hóa mật khẩu, rate limit account identifier + IP 10 phút sau 5 lần sai, token rotation/revocation, bảo vệ dữ liệu sức khỏe, RBAC và OWASP Top 10 | 100% hash BCrypt/Argon2; HTTPS/TLS 1.2+; Backend 401/403; rotating refresh session có thể thu hồi | [Chi tiết](srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-security) |
| Usability | NFR-11 – NFR-15 | Đăng ký $\le 3$ bước, Chatbot trực quan, Responsive tiếng Việt 360px–1920px, tương thích đa trình duyệt và mobile | $\ge 90\%$ dùng Chatbot không cần trợ giúp; responsive đa thiết bị; tương thích 2 bản gần nhất Chrome, Firefox, Safari, Edge, Mobile | [Chi tiết](srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-usability) |
| Reliability | NFR-16 – NFR-19 | Transaction không mất dữ liệu đã commit, xử lý lỗi/timeout Gemini $\le 10$s không trừ lượt oan; uptime 99,5% và 1.000–10.000 users là design goals | Database rollback khi lỗi; fallback thân thiện theo BR-04; design goals không phải MVP release gates | [Chi tiết](srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-reliability) |
| Privacy | NFR-20 – NFR-22 | Consent dữ liệu cá nhân/sức khỏe, PCI-DSS không lưu số thẻ, không lưu account chat history hoặc raw prompt telemetry | Thông báo forwarding tới Gemini; telemetry retention 90 ngày | [Chi tiết](srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-privacy) |
| Auditability | NFR-23 – NFR-27 | OpenAPI, Modular Monolith dùng MVC/layered structure trong từng business module, AI quality theo curated evaluation set, 2 mục OUT_OF_SCOPE | Một Spring Boot deployable backend; $\ge 80\%$ curated cases đạt rule; NFR-26/NFR-27 ngoài MVP | [Chi tiết](srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-auditability) |

## 10. Việc còn mở cho thiết kế/triển khai

Phạm vi nghiệp vụ, lifecycle và Acceptance Criteria cấp FR của Requirements Baseline v1.0.0 đã được chốt. Các việc còn lại thuộc thiết kế/triển khai: chọn payment và email provider; chọn thời lượng access token, chính sách rotation chi tiết và storage mechanism trong baseline DEC-015-A; định dạng UI; công thức/hệ số/khoảng dinh dưỡng có nguồn; dữ liệu khởi tạo; timeout/retry kỹ thuật theo provider; ERD, API contract và test-case traceability. Các chi tiết này không phải quyết định phạm vi sản phẩm mới và không làm thay đổi trạng thái `Active` của baseline.

## 11. Bước tiếp theo

Dùng Use Cases và Acceptance Criteria hiện có để lập ERD, API contract và Test Cases; bổ sung User Story mapping khi cần cho Issue planning. Không tự thêm chức năng, không để artifact downstream định nghĩa lại requirement và không coi các công thức dinh dưỡng chưa nghiên cứu là đã xác nhận.

## 12. Truy vết đề tài

| Yêu cầu đề tài | FR | Phạm vi nhóm chọn |
| --- | --- | --- |
| Quản lý thành viên/bài/bình luận/danh mục | FR-04, FR-06, FR-18, FR-28 | Công khai trực tiếp, hậu kiểm nội dung theo báo cáo |
| Bình luận/bình chọn/báo cáo/quản lý nội dung mình | FR-26–FR-30, FR-44–FR-46, FR-48 | Like không đánh giá sao, reply nhiều cấp |
| Thực đơn tuần theo nguyên liệu và BMI | FR-09, FR-31, FR-33–FR-39 | Hồ sơ rộng hơn BMI, AI chọn công thức có sẵn |
| Tìm/gợi ý nhà hàng chay | FR-42, FR-43 | `OUT_OF_SCOPE`; giữ mô tả lịch sử nhưng không chọn Google Maps làm dependency hoặc tạo implementation scope trong baseline hiện tại. |
| Bài liên quan công thức đã tìm | FR-47 | Thông thường hoặc tùy chọn AI |
| AI Chatbot hỏi đáp ẩm thực chay theo ngữ cảnh | FR-51 | Không tạo công thức/số liệu ngoài hệ thống; hỗ trợ ngữ cảnh chung và ngữ cảnh bài công thức |
| Giới hạn và đăng ký gói AI | FR-10, FR-11, FR-13 | 5/15/50; FREE 0, PLUS 49,000, PRO 99,000 VND/tháng; thanh toán thật và entitlement sau xác minh |
| Guest tìm/xem video, Blog và thử AI | FR-01, FR-02, FR-15, FR-20 | Blog công thức, YouTube nhúng, dùng thử AI Chatbot chung FR-51 tối đa 5 lượt/ngày |

Đây là truy vết yêu cầu, không phải chứng cứ tính năng đã triển khai.
