> **Document:** Software Requirements Specification
> **File:** `docs/requirements/SRS.md`
> **Version:** v0.44.0
> **Created:** 2026-09-11
> **Last Updated:** 2026-09-13
> **Status:** Draft
> **Related Docs:** `docs/requirements/product-direction.md`, `docs/requirements/ai-plan-decomposition.md`

# Software Requirements Specification

## 1. Mục đích và trạng thái tài liệu

Tài liệu này là khung SRS cho **Đề tài 03 — Ứng dụng hỗ trợ người ăn chay**. Nó gom những quyết định sản phẩm đã chốt và chia dự án thành các phần đủ nhỏ để viết Functional Requirements (FR), User Stories, Acceptance Criteria, ERD và Test Cases.

`Draft` nghĩa là tài liệu chưa phải nguồn yêu cầu cuối cùng để lập trình. Các mục ghi **Chưa chốt** chỉ là điểm cần quyết định, không phải yêu cầu đã được phê duyệt.

## 2. Mô tả sản phẩm

Định hướng được người dùng chốt ngày 11/09/2026: **Ứng dụng khám phá món ăn và lập thực đơn chay theo tuần, sử dụng Gemini AI để gợi ý món phù hợp từ các bài công thức đang công khai; người dùng chủ động chọn và thay đổi thực đơn.**

Luồng lõi: khai báo sở thích → khám phá món → nhận gợi ý AI → chọn món → lưu vào thực đơn tuần → chọn món thay thế khi cần. Bài công thức và tương tác cộng đồng cung cấp nội dung hỗ trợ luồng này. Hệ thống không có loại Blog tổng quát tách khỏi công thức.

Gợi ý món phải tham chiếu bài công thức đang công khai và không bị ẩn trong hệ thống. Hệ thống kiểm tra ràng buộc bắt buộc và kết quả AI; khi không đủ món phù hợp phải thông báo rõ. Người dùng quyết định lưu/thay món. Các trường dữ liệu để kiểm tra nguyên liệu và sở thích sẽ được phân rã riêng; việc bài được công khai không phải cam kết an toàn dị ứng hoặc xác nhận chuyên môn.

Onboarding Questionnaire sau đăng ký thu thập sở thích ăn uống phục vụ chọn món. Người dùng được bỏ qua Onboarding, nhưng phải hoàn thành ba nhóm thông tin tối thiểu trước khi dùng AI cá nhân hóa theo mục 3.15. Chi tiết từng lựa chọn vẫn được phân rã tại [bản actor và onboarding](actors-and-onboarding-draft.md). BMI thuộc Hồ sơ nhu cầu dinh dưỡng tại mục 3.18 và chỉ là chỉ số tham khảo, không tự quyết định nhu cầu calorie/dưỡng chất hoặc món phù hợp.

AI hỗ trợ hỏi đáp, tìm kiếm/gợi ý, soạn bản nháp bài công thức theo mẫu và có thể hỗ trợ moderation. AI không phải chuyên gia dinh dưỡng, không tự quyết định duyệt/xóa nội dung và không đưa chẩn đoán hoặc điều trị sức khỏe.

## 3. Ranh giới hệ thống

### 3.1 Trong phạm vi đã chốt

- Nội dung cộng đồng: bài công thức (`Recipe Post`), bình luận và danh mục. Đây là loại bài duy nhất; thuật ngữ “Blog” trong đề bài được giới hạn thành Blog công thức, không có Blog tổng quát hoặc bài viết tự do riêng.
- Mỗi bài công thức có dữ liệu món ăn có cấu trúc. Hướng dẫn nấu theo từng bước là phần tùy chọn; thiếu phần này không ngăn Member đã đăng nhập công khai bài.
- Media Phase 1: ảnh được upload lên Azure Blob Storage; bài công thức có thể chứa link YouTube và phát bằng embedded player trong app.
- Đăng bài công thức: Member đã đăng nhập tạo và công khai Recipe Post trực tiếp sau khi hoàn thành các trường bắt buộc; không có đơn xin quyền đăng hoặc duyệt trước từng bài.
- Kiểm duyệt sau đăng: người dùng báo cáo nội dung có vấn đề và Administrator xử lý báo cáo. AI quét/gắn cờ là hạng mục để sau (DEFERRED), không phải điều kiện nghiệm thu MVP.
- Người dùng: tìm kiếm/lọc, xem nội dung, tương tác cộng đồng và lập thực đơn tuần.
- AI Gemini: hỏi đáp/gợi ý với Guest Free, Free, Plus và Pro theo lượt gọi/ngày.
- Quản trị: quản lý người dùng, bài công thức, danh mục, video nhúng, bình luận và nội dung bị gắn cờ.

### 3.2 Ngoài phạm vi hiện tại

- Actor chuyên gia dinh dưỡng/thực phẩm độc lập và xác minh chứng chỉ.
- Chẩn đoán, điều trị, tư vấn sức khỏe chuyên nghiệp hoặc thay thế bác sĩ.
- Lưu nháp Recipe Post trên server và lịch sử hội thoại AI theo tài khoản.
- Tính nhu cầu dinh dưỡng, AI menu dinh dưỡng hoặc đánh giá menu cho người dưới 18 tuổi, người mang thai/cho con bú hay người cần chế độ ăn điều trị bệnh.
- Mobile/native app riêng. MVP là web responsive tiếng Việt; thanh toán gói AI thật thuộc phạm vi theo Q26, còn giá, cổng và chính sách chưa chốt.
- Tích hợp Apple Health, Google Fit, wearable hoặc lấy vị trí hiện tại bằng GPS/trình duyệt. Tìm nhà hàng chay quanh địa chỉ do người dùng nhập thuộc phạm vi tại mục 3.19.
- Nhận diện ảnh nguyên liệu, tóm tắt video, dự báo mùa/giá nguyên liệu.
- Upload/lưu file video trực tiếp trên Azure Blob Storage. Phase 1 chỉ lưu link hoặc YouTube video ID.
- Gói Max “vô hạn”. Nếu có sau này phải là Fair Use và có quota thực tế.
- `Queue` món chờ xếp lịch, shopping list và một module `Previous` riêng. Người dùng xem tuần trước bằng cách điều hướng lịch; kết quả AI gợi ý thuộc luồng AI hiện có.

### 3.3 Bài công thức và hướng tham khảo Samsung Food — cập nhật 12/09/2026

Samsung Food là sản phẩm tham khảo chính cho cách nối khám phá món, chi tiết công thức và Planner; không phải toàn bộ phạm vi cần sao chép. Ứng dụng có một lớp mạng xã hội thu nhỏ xoay quanh công thức: tác giả quản lý bài của mình và người dùng tương tác theo quyền. Tuy nhiên, luồng lõi của sản phẩm vẫn là khám phá món, AI gợi ý và lập thực đơn; không tự mở rộng sang đầy đủ tính năng của Facebook.

“Bình chọn” trong phạm vi MVP được hiểu là **Like/Upvote**, không phải chấm điểm 1–5 sao. Member đã đăng nhập được thích hoặc bỏ thích một bài công thức; mỗi Member chỉ đóng góp tối đa một lượt thích đang hiệu lực cho mỗi bài. Guest được xem tổng lượt thích nhưng phải đăng nhập trước khi thích. Like không làm thay đổi Saved Recipes, hồ sơ sở thích, Meal Planner hoặc hạn mức AI.

Bình luận phải hỗ trợ **reply lồng nhiều cấp**, không giới hạn thành danh sách phản hồi phẳng. Guest được đọc bình luận trên bài công thức công khai. Member đã đăng nhập được tạo bình luận cấp đầu, trả lời một bình luận hoặc một reply khác, đồng thời sửa/xóa bình luận của chính mình. Administrator được quản lý và xử lý bình luận vi phạm. Giới hạn độ sâu cụ thể, cách thu gọn cây và cách hiển thị nhánh khi bình luận cha bị xóa thuộc bước thiết kế sau, không làm thay đổi yêu cầu phải hỗ trợ nhiều cấp.

Bài công thức có năm nhóm dữ liệu riêng; trong đó bốn nhóm là bắt buộc trước khi công khai, còn hướng dẫn từng bước là tùy chọn:

| Nhóm dữ liệu | Mục đích |
| --- | --- |
| Nguyên liệu | Cho người đọc biết thành phần; làm đầu vào cho lọc và kiểm tra gợi ý AI. |
| Cách làm từng bước | Tùy chọn; tác giả bổ sung khi muốn hướng dẫn chi tiết cách thực hiện món ăn. |
| Khẩu phần | Cho biết công thức phục vụ bao nhiêu người/phần. |
| Thời gian nấu | Hỗ trợ người dùng chọn món phù hợp thời gian. |
| Loại ăn chay | Hỗ trợ phân loại và tìm món theo chế độ ăn. |

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

**Chưa chốt:** cách xử lý món chưa kiểm tra đủ thành phần trong gợi ý AI; cơ chế tên đồng nghĩa; danh sách/kiểm soát đơn vị; quy tắc số lượng hợp lệ; sửa/ngừng sử dụng nguyên liệu chuẩn đã được tham chiếu. Chưa thiết kế ERD hoặc workflow chuẩn hóa chi tiết.

### 3.5 Định lượng nguyên liệu — đã chốt 12/09/2026

- Mỗi dòng nguyên liệu hỗ trợ một trong hai cách khai báo: định lượng cụ thể bằng số lượng và đơn vị (ví dụ `200 g đậu hũ`), hoặc `vừa đủ` (ví dụ `muối — vừa đủ`).
- Với `vừa đủ`, không bắt buộc nhập số lượng hay đơn vị; hệ thống giữ rõ cách khai báo này, không dùng số giả như 0 để biểu diễn lượng chưa xác định.
- Hệ thống không tự quy đổi `vừa đủ` sang gram và không suy ra dinh dưỡng chính xác từ lượng chưa xác định. Chức năng dinh dưỡng tại 3.18 phải thể hiện dữ liệu thiếu này.
- Việc dùng `vừa đủ` không làm mất tên nguyên liệu hay liên kết danh mục nếu có; nguyên liệu vẫn thuộc thành phần công thức để kiểm tra ràng buộc ăn uống.

**Đã chốt đơn vị tại 3.20. Còn thiết kế:** định dạng số/phân số, giới hạn định lượng và xử lý thay đổi khẩu phần.

### 3.6 Một bài công thức, nhiều cách hiển thị và AI hỗ trợ soạn bài — cập nhật 12/09/2026

- Tác giả tạo một bài công thức có cấu trúc. Thẻ món ở Khám phá, trang chi tiết và tham chiếu trong thực đơn đều sử dụng cùng bài này; không yêu cầu tạo hai bài, một Blog tổng quát hoặc một website thứ hai.
- Nút “Xem công thức” mở trang chi tiết nội bộ với nội dung hướng dẫn đầy đủ. Không dùng liên kết website ngoài làm nơi bắt buộc đọc cách nấu; đề xuất đóng góp chỉ bằng dẫn nguồn ngoài chưa được chấp thuận.
- Biểu mẫu có các mục tên món, giới thiệu, nguyên liệu, khẩu phần, thời gian, loại ăn chay, hướng dẫn nấu từng bước và media theo chính sách hiện tại. Format quy định cấu trúc; tác giả vẫn được viết theo văn phong riêng. Hướng dẫn từng bước là tùy chọn. Điều kiện đầy đủ trước khi công khai nằm tại mục 3.9; validation chi tiết còn chờ chốt.
- Tác giả có thể tự viết hoặc chủ động yêu cầu Gemini hỗ trợ tạo nội dung có thể chỉnh sửa trong biểu mẫu từ thông tin đã cung cấp. Không bắt buộc dùng AI để tạo hoặc gửi bài.
- Tác giả xem, sửa và xác nhận nội dung AI trước khi công khai. AI không âm thầm thay nguyên liệu hoặc trình bày thông tin chưa biết như dữ kiện đã xác nhận.
- Member đã đăng nhập quản lý Recipe Post của chính mình theo quyền sở hữu nội dung thông thường của một mạng xã hội thu nhỏ: tạo, xem, sửa và xóa bài. Bài mới hoặc thay đổi hợp lệ được công khai trực tiếp; hệ thống kiểm tra tài khoản sở hữu bài và các trường bắt buộc.
- Khi tác giả xóa bài của mình, bài không còn hiển thị công khai và không được dùng cho kết quả tìm kiếm hoặc gợi ý AI. Tác giả không được dùng quyền sửa/xóa của mình để khôi phục bài đang bị Administrator ẩn do hậu kiểm.
- Khi tác giả bấm công khai, hệ thống kiểm tra đăng nhập, cấu trúc và các trường bắt buộc. Bài hợp lệ được công khai trực tiếp. AI không tự công khai bài. Đúng format không đồng nghĩa công thức đúng hoặc an toàn.
- Khi AI lỗi hoặc hết lượt, người dùng vẫn tiếp tục tự viết/chỉnh sửa bài. Quyền gọi AI soạn bài theo gói và cách tính lượt là quyết định riêng, không tự thay đổi hạn mức 5/15/50 đã chốt.

Luồng cấp cao: Member đã đăng nhập → tác giả nhập thông tin → tự viết hoặc yêu cầu AI hỗ trợ → tác giả rà soát/chỉnh sửa → hệ thống kiểm tra đăng nhập/format → công khai thẻ món và trang chi tiết từ cùng Recipe Post.

**Đã chốt phạm vi AI hỗ trợ nội dung trong biểu mẫu, quyền gói và tính lượt tại 3.20. Lưu nháp Recipe Post không thuộc phạm vi hiện tại. Không phân rã thêm CRUD tác giả ở bước chốt chức năng.

### 3.7 Hướng dẫn nấu theo bước tùy chọn — cập nhật 12/09/2026

- Bài công thức có thể không có hướng dẫn nấu. Nếu tác giả cung cấp hướng dẫn, phần này là danh sách các bước có thứ tự; mỗi bước chứa nội dung hướng dẫn bằng văn bản.
- Tác giả có thể thêm, sửa, xóa và đổi vị trí các bước trong trình soạn bài. Trang chi tiết hiển thị theo thứ tự đã lưu.
- Phần giới thiệu vẫn được viết tự do trong mục riêng, không tự được hiểu là danh sách hướng dẫn nấu.
- Khi AI hỗ trợ tạo hướng dẫn, kết quả phải theo cùng cấu trúc các bước để tác giả rà soát và chỉnh sửa từng bước trước khi công khai.
- Tác giả vẫn chịu trách nhiệm về các bước do AI hỗ trợ và phải tuân thủ chính sách nội dung. Khi sửa hướng dẫn trong bài đang công khai, thay đổi áp dụng theo cùng quy tắc chỉnh sửa bài tại mục 3.6.

Không có bước hướng dẫn vẫn được công khai nếu các trường bắt buộc khác đã đầy đủ. Khi không có hướng dẫn, trang chi tiết hiển thị rõ “Tác giả chưa cung cấp hướng dẫn từng bước”, không tạo dữ liệu giả. AI chỉ hỗ trợ tác giả soạn các bước khi được yêu cầu; AI gợi ý món cho người đọc không được trình bày hướng dẫn chưa có như nội dung do tác giả xác nhận. **Chưa chốt:** số bước tối đa, giới hạn độ dài mỗi bước và media riêng cho từng bước. Không mặc định bổ sung ảnh/video cho mỗi bước.

### 3.8 Tác giả và hồ sơ công khai — đã chốt 12/09/2026

- Đầu trang chi tiết bài công thức hiển thị tên công khai và avatar của tác giả, kèm liên kết tới hồ sơ công khai và các bài đã công khai của tác giả.
- Hệ thống gắn tác giả với tài khoản đăng bài, không dùng ô nhập tên tác giả tùy ý và không cho người đăng chọn tài khoản khác để đứng tên.
- Hồ sơ công khai không hiển thị email, thông tin đăng nhập hoặc dữ liệu hồ sơ ăn uống riêng tư. Có thêm giới thiệu ngắn, ngày tham gia và tổng Like nhận được trên bài công thức.
- Administrator xử lý báo cáo không trở thành tác giả. AI hỗ trợ viết không thay thế tác giả; người dùng vẫn rà soát và chịu trách nhiệm khi công khai bài.
- Gắn bài với tài khoản nhằm truy xuất người đứng tên nội dung, không chứng minh danh tính ngoài đời hoặc chuyên môn. Bản hiện tại chưa làm xác minh danh tính thật/bằng cấp và không cấp nhãn “đã xác minh danh tính” hay “chuyên gia”.
- Tham khảo giao diện đã quan sát trực tiếp trên Chrome: [Salads for Lunch — Creamy Tuna Macaroni Salad](https://www.salads4lunch.com/recipes/salad-recipes-for-lunch/classic-salads/creamy-tuna-macaroni-salad/) hiển thị ảnh/tên tác giả dẫn tới hồ sơ ở đầu bài và phần giới thiệu tác giả cuối bài. Đây là bằng chứng về cách hiển thị, không xác nhận quy trình kiểm chứng danh tính của website.

**Chưa chốt:** avatar mặc định, quy tắc đổi tên/avatar, xử lý hiển thị tác giả khi tài khoản bị khóa hoặc xóa và các trường giới thiệu bổ sung trên hồ sơ. Không tự thêm follow, nhắn tin hoặc xác minh chuyên gia.

### 3.9 Điều kiện tạo và công khai Recipe Post trực tiếp — cập nhật 13/09/2026

| Trường | Bắt buộc khi công khai | Ghi chú |
| --- | --- | --- |
| Tên món | Có | Nội dung do Member nhập. |
| Nguyên liệu | Có, ít nhất một | Có thể bao gồm nguyên liệu ngoài danh mục dinh dưỡng theo FR-40. |
| Khẩu phần | Có | Giá trị cụ thể để người xem hiểu công thức. |
| Loại ăn chay | Có | Chọn một trong bốn loại đã chốt tại 3.20. |
| Thời gian chuẩn bị/nấu | Có | Hiển thị cùng Recipe Post. |
| Tác giả | Có | Member đã đăng nhập tạo bài. |
| Mô tả, các bước nấu, ảnh và link YouTube | Không | Bước nấu không phải trường bắt buộc; ảnh/link là tùy chọn. |

- Member đã đăng nhập được tạo và công khai Recipe Post trực tiếp; không có đơn xin quyền đăng, trạng thái quyền đăng riêng, hoặc duyệt trước từng bài.
- Khi Member bấm công khai, hệ thống kiểm tra các trường bắt buộc. Nếu thiếu, hệ thống chỉ rõ trường cần bổ sung và không công khai bài.
- Recipe Post hợp lệ được công khai ngay. Sau đó, người dùng có thể báo cáo nội dung; Administrator xử lý báo cáo theo 3.11–3.14.
- AI chỉ hỗ trợ tác giả tạo nội dung có thể chỉnh sửa trong biểu mẫu; AI không tự công khai bài. Việc lưu nháp bền vững trên server không thuộc phạm vi hiện tại.
- Giới hạn chi tiết về độ dài, định dạng media, số lượng ảnh/link và quy tắc nội dung vẫn cần được phân rã trước khi triển khai.
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

**Chưa chốt:** giới hạn độ dài mô tả; biện pháp chống lạm dụng bổ sung; kênh và thời điểm thông báo kết quả; thời điểm AI rà soát và tiêu chí gắn cờ. Không mở rộng báo cáo sang bình luận/tài khoản. Luồng xử lý báo cáo và ẩn bài đã chốt tại 3.12; quyền xem tại 3.13.

### 3.12 Xử lý báo cáo và ẩn bài công thức — đã chốt 12/09/2026

| Trạng thái báo cáo | Ý nghĩa |
| --- | --- |
| Chờ xử lý | Báo cáo mới được gửi, chờ Administrator tiếp nhận. |
| Đang xem xét | Admin đã tiếp nhận và đang kiểm tra nội dung. |
| Đã giải quyết | Admin đã ghi kết luận và lý do xử lý: có vi phạm hoặc không đủ căn cứ. |

- Trạng thái báo cáo độc lập với trạng thái bài công thức. Gửi báo cáo hoặc có nhiều báo cáo không tự động ẩn bài.
- Nếu xác định có vấn đề, Admin có thể ẩn hoặc xóa bài công thức, hoặc khóa tài khoản theo mức độ vi phạm. Đây là quyền xử lý vi phạm của Admin, độc lập với quyền tác giả tự xóa bài của mình. Ngưỡng áp dụng từng chế tài còn chờ chốt; số lượng báo cáo không tự động tạo chế tài.
- Ẩn không phải xóa bài; bài ẩn không còn được xem công khai nhưng vẫn phục vụ tác giả chỉnh sửa và Admin xem xét. Xóa bài và khóa tài khoản là chế tài nghiêm trọng, phải có lý do.
- Nếu không đủ căn cứ, Admin đóng báo cáo với kết luận và lý do, không ẩn bài vì báo cáo đó. Nếu bài đang công khai thì tiếp tục công khai; việc đóng một báo cáo không tự khôi phục bài bị ẩn bởi quyết định khác.
- Bài bị ẩn không tự công khai khi tác giả sửa. Chỉ Administrator được khôi phục bài sau khi xử lý báo cáo; đây là quyết định hậu kiểm, không phải quy trình duyệt mọi bài mới.
- “Đã giải quyết” nghĩa là Admin đã kết luận/xử lý báo cáo, không đồng nghĩa bài đã được tác giả sửa xong hoặc đã được công khai lại.

**Chưa chốt:** tiêu chí ẩn/xóa bài hoặc khóa tài khoản; chi tiết chuyển trạng thái khi tác giả sửa bài bị ẩn; kênh/thời điểm thông báo; xử lý nhiều báo cáo; ảnh hưởng của bài bị ẩn/xóa đến thực đơn đã lưu và audit trail chi tiết.

### 3.13 Quyền xem báo cáo và bảo vệ người gửi — đã chốt 12/09/2026

| Người xem | Phạm vi được xem |
| --- | --- |
| Administrator | Báo cáo và tài khoản người gửi để xem xét, xử lý và kiểm tra lạm dụng. |
| Tác giả bài công thức bị báo cáo | Lý do, nội dung cần sửa và quyết định xử lý liên quan đến bài của mình; không thấy danh tính người báo cáo. |
| Người gửi báo cáo | Trạng thái và kết quả xử lý báo cáo của chính mình; không được xem báo cáo của tài khoản khác. |
| Công chúng | Báo cáo không được hiển thị công khai. |

- Hệ thống kiểm tra quyền xem ở backend theo vai trò và quan hệ với báo cáo/bài công thức, không chỉ ẩn nút hoặc trường trên giao diện.
- Nội dung gửi cho tác giả không kèm thông tin định danh người báo cáo. Không tự chuyển nguyên văn mô tả hoặc ghi chú nội bộ có thể làm lộ người gửi; thông tin phản hồi phải tuân theo phạm vi trong bảng.
- Quyền xem trạng thái/kết quả không đồng nghĩa đã chốt gửi email, push notification hoặc công khai ghi chú nội bộ của Admin.

**Chưa chốt:** màn hình tra cứu chi tiết, nội dung thông báo cụ thể, kênh và thời điểm thông báo, thời hạn lưu báo cáo và ghi chú nội bộ.

### 3.14 Báo cáo trùng và bổ sung thông tin — đã chốt 12/09/2026

- Mỗi tài khoản chỉ có một báo cáo chưa giải quyết trên cùng bài công thức (Chờ xử lý hoặc Đang xem xét).
- Khi báo cáo còn mở, người gửi được bổ sung mô tả vào báo cáo đó, không tạo báo cáo trùng.
- Sau khi báo cáo đã giải quyết, người dùng được báo cáo lại nếu phát hiện vấn đề mới và phải mô tả điểm mới, kể cả khi lý do không phải “Khác”.
- Nhiều tài khoản vẫn có thể báo cáo cùng bài công thức. Admin xem các báo cáo được nhóm theo bài; nhóm hiển thị không làm mất người gửi, trạng thái và kết quả của từng báo cáo.
- Backend kiểm tra quy tắc một báo cáo đang mở, kể cả khi nhận nhiều yêu cầu đồng thời.
- Chưa chốt giới hạn tần suất, độ dài mô tả bổ sung, lịch sử bổ sung và quy tắc xử lý hàng loạt.

### 3.15 Onboarding tùy chọn và điều kiện AI cá nhân hóa — đã chốt 12/09/2026

- Sau khi đăng ký, hệ thống mời Member thực hiện Onboarding Questionnaire và cho phép bỏ qua để vào ứng dụng.
- Member chưa hoàn thành Onboarding vẫn được xem/tìm món, tự thêm món vào thực đơn, đọc/bình luận/đăng bài công thức và dùng AI hỏi đáp thông thường theo hạn mức gói.
- Trước khi dùng AI gợi ý món hoặc tạo thực đơn cá nhân hóa, hồ sơ phải có ba nhóm thông tin tối thiểu: loại ăn chay; nguyên liệu cần tránh do dị ứng/kiêng; món hoặc nguyên liệu không thích.
- Với hai nhóm danh sách, người dùng phải được chủ động xác nhận “Không có” nếu không có mục cần khai báo. Bỏ trống không được tự hiểu là không có dị ứng/kiêng hoặc không có món không thích.
- Nếu thiếu thông tin tối thiểu, hệ thống không gọi Gemini cho yêu cầu cá nhân hóa và hướng người dùng tới đúng mục cần bổ sung; không trừ lượt AI.
- Sở thích mềm như ẩm thực yêu thích, thời gian nấu mong muốn và độ khó có thể để trống. AI chỉ cá nhân hóa theo dữ liệu thực sự đã được khai báo.
- Onboarding và hồ sơ sở thích có thể được cập nhật sau. Việc hoàn thành Onboarding không tạo cam kết y tế hoặc bảo đảm tuyệt đối về dị ứng.

**Chưa chốt:** danh sách loại ăn chay; cách nhập/chọn từng nhóm; phân biệt ràng buộc cứng và sở thích mềm trong thuật toán; cơ chế cập nhật hồ sơ. Hồ sơ nhu cầu dinh dưỡng và BMI được tách tại mục 3.18.

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

**Chưa chốt:** ghi chú và thứ tự món trong cùng bữa; giới hạn ngày trong quá khứ hoặc tương lai; hành vi khi bài công thức bị ẩn/xóa; thao tác sao chép cả tuần. Số khẩu phần dự định ăn đã chốt tại mục 3.18.

### 3.17 AI chỉ đề xuất công thức đã có và đang công khai — cập nhật 12/09/2026

- AI gợi ý món và lập menu chỉ được chọn từ các bài công thức đang công khai, không bị ẩn/xóa trong hệ thống.
- Gợi ý có thể dựa trên loại ăn chay, nguyên liệu cần tránh, món/nguyên liệu không thích, sở thích đã khai báo và nguyên liệu người dùng cho biết đang có.
- AI được tìm, xếp hạng, giải thích lý do phù hợp, sắp công thức vào menu và đề xuất công thức thay thế; AI không tạo món, công thức, danh sách nguyên liệu hoặc hướng dẫn nấu mới cho người dùng.
- Mỗi kết quả phải dẫn tới bài công thức tương ứng để người dùng kiểm tra trước khi lưu hoặc thêm vào lịch ăn.
- Khi không có công thức phù hợp, hệ thống thông báo không tìm thấy hoặc chỉ trả danh sách chưa đầy đủ kèm lý do; không bịa công thức để lấp kết quả.
- Bản menu AI chỉ là đề xuất. Người dùng chọn từng món hoặc xác nhận menu trước khi lịch ăn thay đổi.

**Chưa chốt:** nguyên liệu “đang có” được nhập cho từng lần yêu cầu hay quản lý thành kho cá nhân; mức độ bắt buộc phải dùng hết nguyên liệu; cách hiển thị nguyên liệu còn thiếu; quyền gói và số lượt cho một lần lập/chỉnh menu.

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

### 3.19 Tìm nhà hàng chay quanh địa chỉ nhập — đã chốt ranh giới 12/09/2026

- Chỉ Member đã đăng nhập được sử dụng chức năng tìm/đề xuất nhà hàng chay. Guest không được gửi yêu cầu tìm địa điểm; nếu chọn chức năng này thì được yêu cầu đăng nhập.
- Người dùng nhập hoặc chọn một địa chỉ/địa điểm Google Maps làm tâm tìm kiếm, ví dụ `KTX Khu B`. MVP không yêu cầu hoặc đọc GPS/vị trí hiện tại của thiết bị.
- Member chọn ngưỡng khoảng cách đường bộ **500 m, 1 km, 5 km hoặc 10 km**; không dùng đường thẳng thay cho đường bộ. Phương tiện tính đường để thiết kế.
- Hệ thống chuyển địa chỉ đã chọn thành tọa độ và tìm các địa điểm do Google Maps phân loại là `vegetarian_restaurant` hoặc `vegan_restaurant` trong khu vực tương ứng. Đây là tìm kiếm địa điểm, không phải tác vụ Gemini AI và không trừ lượt AI.
- Kết quả nhà hàng được hiển thị trong app để người dùng xem và lựa chọn. App phải ghi rõ dữ liệu địa điểm đến từ Google Maps; kết quả là dữ liệu phân loại của nhà cung cấp, không phải xác nhận độc lập của hệ thống rằng mọi món tại nhà hàng đều là món chay.
- Danh sách này là đề xuất nhà hàng chay theo địa chỉ và bán kính, độc lập với món hoặc bài công thức người dùng đã tìm kiếm. MVP không có chức năng suy ra hay đề xuất cửa hàng/nhà hàng dựa trên món đã tìm.
- Nhà hàng không phải nội dung do hệ thống quản lý. Administrator không tạo, sửa, xóa hoặc xác minh hồ sơ nhà hàng; ứng dụng chỉ hiển thị dữ liệu địa điểm do Google Maps Platform cung cấp. Việc có lưu tạm kết quả để tối ưu kỹ thuật hay không chưa làm thay đổi quyền sở hữu nguồn dữ liệu và sẽ được quyết định ở thiết kế.
- Khi địa chỉ không xác định được, không có kết quả, Google Maps Platform lỗi hoặc hết hạn mức, hệ thống phải thông báo đúng trạng thái thay vì tạo địa điểm giả.
- Google Maps Platform là phụ thuộc bên ngoài có cơ chế tính phí/hạn mức. Nhóm phải kiểm soát quota và không giả định dịch vụ luôn miễn phí. Google mô tả Geocoding dùng để chuyển địa chỉ thành tọa độ, Nearby Search tìm địa điểm theo vùng/bán kính và hỗ trợ loại `vegetarian_restaurant`/`vegan_restaurant`: [Geocoding](https://developers.google.com/maps/documentation/geocoding/geocoding), [Nearby Search](https://developers.google.com/maps/documentation/places/web-service/nearby-search), [Place Types](https://developers.google.com/maps/documentation/places/web-service/place-types), [Pricing](https://developers.google.com/maps/billing-and-pricing/pricing).

**Đã chốt:** ngưỡng đường bộ, danh sách + bản đồ, không lưu nhà hàng và mặc định 10 lượt/Member/ngày. Trường hiển thị, phương tiện và tích hợp dịch vụ để thiết kế.

**Lưu ý truy vết yêu cầu:** nguyên văn đề tài có ý “gợi ý cửa hàng thuần chay liên quan đến món ăn đã tìm kiếm”. Quyết định hiện tại chủ động thu hẹp thành đề xuất nhà hàng chay quanh địa chỉ, không liên kết với món đã tìm và không quản lý nhà hàng. Theo Q35, đề bài là định hướng đề xuất và nhóm chủ động chọn phạm vi này; không đặt điều kiện chờ giảng viên xác nhận. Giữ ghi chú để truy vết, không tự khôi phục chức năng đã loại.

### 3.20 Phạm vi chức năng sau bảng Q01–Q38 — xác nhận 12/09/2026

Mục này ghi quyết định mới nhất của người dùng và thay thế các đề xuất hoặc nhãn “chưa chốt” cũ về cùng vấn đề trong tài liệu phân rã. Chốt chức năng không có nghĩa đã chốt thiết kế hoặc đã triển khai.

| Câu | Quyết định đã xác nhận |
| --- | --- |
| Q01–Q04 | Web responsive; email/password và Google Login; xác minh email bắt buộc, quên/đặt lại mật khẩu. Hồ sơ công khai có tên, avatar, giới thiệu ngắn, bài đăng, ngày tham gia và tổng Like trên bài; không công khai email/hồ sơ ăn uống. |
| Q05–Q08 | Bốn loại ăn chay: Vegan, Lacto Vegetarian, Ovo Vegetarian, Lacto-Ovo Vegetarian. Nguyên liệu tránh/không thích chọn danh mục hoặc nhập tự do. Khẩu vị ẩm thực, độ khó và thời gian nấu tối đa tùy chọn. Được sửa hồ sơ, AI dùng dữ liệu mới ở yêu cầu tiếp theo; không tự sửa menu đã lưu. |
| Q09–Q12 | Admin tạo/sửa/ngừng dùng danh mục, một công thức có nhiều danh mục. Đơn vị g, kg, ml, l, tsp, tbsp, cup, piece hoặc “vừa đủ”; dinh dưỡng chỉ tính khi quy đổi gram có căn cứ. Lưu nháp Recipe Post không thuộc phạm vi hiện tại. AI hỗ trợ giới thiệu và bước nấu từ thông tin tác giả cung cấp, không thêm nguyên liệu hoặc tự công khai. |
| Q13–Q14 | Gợi ý bài liên quan thông thường theo danh mục/nguyên liệu không dùng Gemini; tùy chọn người dùng chủ động yêu cầu AI có tính lượt. Không tự gọi AI có tính lượt khi mở bài. Tìm/lọc từ khóa, loại ăn chay, danh mục, nguyên liệu, thời gian; sắp xếp mới nhất/nhiều Like. |
| Q15–Q18 | Bình luận/reply công khai ngay, có Like/Unlike và báo cáo. Một tài khoản tối đa một Like hiệu lực trên mỗi nội dung. Admin hậu kiểm bài và bình luận, có cảnh báo, ẩn/xóa, khóa/mở khóa tài khoản và ghi lý do. Thông báo trong app và email cho reply mới và quyết định xử lý nội dung; không thêm push. |
| Q19–Q20 | Free/Plus/Pro dùng chung chức năng AI, khác hạn mức 5/15/50 lượt/ngày. Điều kiện hồ sơ vẫn áp dụng cho chức năng cần dữ liệu đó. Guest chỉ hỏi đáp cơ bản 5 lượt/ngày. Mỗi yêu cầu AI thành công tính một lượt, kể cả menu tuần; lỗi không trừ lượt. |
| Q21–Q24 | AI đề xuất 7 ngày, sáng/trưa/tối, người dùng chọn/sửa/bỏ trước khi lưu. Nguyên liệu đang có nhập theo từng yêu cầu, không có kho nguyên liệu. Ưu tiên tận dụng, không bắt dùng hết, hiển thị phần thiếu. AI chỉ chọn công thức hiện có; chatbot giải thích thay thế nguyên liệu nhưng không tự sửa công thức/dinh dưỡng hoặc tạo công thức mới. |
| Q25–Q26 | Lịch sử hội thoại AI theo tài khoản không thuộc phạm vi hiện tại. Tích hợp thanh toán gói AI thật; chỉ kích hoạt sau xác minh giao dịch. Giá, cổng, chu kỳ, gia hạn và hoàn tiền còn TBD. |
| Q27–Q30 | Mục tiêu duy trì/giảm/tăng cân và tăng cơ; mục tiêu khác cần định nghĩa trước khi bổ sung. Khoảng 60 nguyên liệu có nguồn cho demo. Chỉ quy đổi gram bằng quy tắc có nguồn do hệ thống quản lý; thiếu dữ liệu không coi là 0. Admin sửa số liệu thì tính lại công thức/menu liên quan, ghi thay đổi và nguồn. |
| Q31 | Chỉ ghi nguồn trong SRS; nghiên cứu/chốt công thức và khoảng đánh giá khi triển khai chức năng dinh dưỡng. Không yêu cầu nghiên cứu công thức ngay để tiếp tục tài liệu. |
| Q32–Q34 | Ngưỡng khoảng cách đường bộ 500 m/1 km/5 km/10 km, không phải đường thẳng. Danh sách và bản đồ nhúng; không lưu nhà hàng yêu thích; hạn mức cấu hình mặc định 10 lượt tìm/Member/ngày, độc lập lượt AI. Phương tiện và tích hợp đường đi để thiết kế. |
| Q35 | Chỉ đề xuất nhà hàng quanh địa chỉ nhập, không liên kết món đã tìm hoặc quản lý hồ sơ nhà hàng. Nhóm tự chọn phạm vi từ đề bài mang tính đề xuất; không có điều kiện chờ giảng viên xác nhận. |
| Q36–Q38 | Giao diện tiếng Việt. AI gắn cờ là hạng mục DEFERRED, không phải nghiệm thu MVP. Ba actor người dùng: Guest, Member, Administrator; Contributor là quyền của Member. |

**Nguồn ghi nhận cho Q31:** [USDA FoodData Central](https://fdc.nal.usda.gov/) cho thành phần nguyên liệu; [USDA DRI Calculator](https://www.nal.usda.gov/human-nutrition-and-food-safety/dri-calculator) và [NIH DRI](https://ods.od.nih.gov/HealthInformation/nutrientrecommendations/) cho tham khảo nhu cầu dinh dưỡng. Ghi nguồn không đồng nghĩa chốt công thức hay tích hợp API runtime. Trước khi đưa tính toán thật vào sử dụng, phải chọn công thức/hệ số có căn cứ, giới hạn đối tượng và kiểm thử; không dùng số đoán hoặc để Gemini tự đặt mục tiêu.

## 4. Actors và quyền cấp cao

Bảng dưới là baseline cấp cao. Bản phân rã chi tiết và các quyền đã xác nhận nằm tại [Actors & Onboarding](actors-and-onboarding-draft.md); các gói Free/Plus/Pro là quyền sử dụng, không mặc định tạo role mới.

| Actor | Trạng thái | Mục tiêu chính | Không được làm |
| --- | --- | --- | --- |
| Guest | Chưa đăng nhập | Xem bài công thức công khai, tìm kiếm và dùng Guest Free AI | Đăng/bình luận, gửi báo cáo bài, xem lịch sử, lập thực đơn cá nhân hóa. |
| Member (Authorized User) | Đã đăng nhập | Tương tác cộng đồng, tạo/công khai Recipe Post trực tiếp, báo cáo bài có vấn đề, lưu công thức, quản lý lịch ăn, dùng AI theo gói | Tự duyệt nội dung hoặc quản trị người khác. |
| Administrator | Quản trị | Xử lý báo cáo bài; quản lý thành viên, bài công thức, bình luận, danh mục và danh mục nguyên liệu dinh dưỡng | Không duyệt quyền đăng hoặc từng bài trước khi công khai; không được để AI tự quyết định chế tài hoặc tự tạo số liệu dinh dưỡng. |
| Gemini AI | Dịch vụ ngoài hệ thống | Tạo phản hồi AI khi backend cho phép | Không có quyền hệ thống, không truy cập database trực tiếp, không tự công khai nội dung. |

## 5. Phân rã dự án theo module

| Mã module | Module | Giá trị | Phụ thuộc chính | Trạng thái phân rã |
| --- | --- | --- | --- | --- |
| M01 | Public Recipe Discovery | Khách tìm/xem bài công thức công khai, gồm bài có video YouTube được nhúng | Nội dung đã công khai | Cần viết user stories. |
| M02 | Identity & Access | Phân biệt Guest/User/Admin và bảo vệ thao tác | Tài khoản, role, session | Đã chốt email/password, Google Login, xác minh email và quên mật khẩu. |
| M03 | Recipe Contribution & Post-moderation | Member đã đăng nhập quản lý bài của mình và công khai trực tiếp; Member Like/Unlike và bình luận/reply nhiều cấp; Admin hậu kiểm nội dung theo báo cáo | M02, Azure Blob, quy trình báo cáo | Đã chốt CRUD bài, Like/Upvote và bình luận nhiều cấp; giới hạn nội dung, chi tiết hiển thị cây và chế tài còn cần phân rã. |
| M04 | Recipe Classification & Discovery | Phân loại, tìm và lọc bài công thức chay | M01, M03 | Đã chốt dữ liệu tại 3.3; cấu trúc và validation chi tiết còn cần phân rã. |
| M05 | Saved Recipes & Weekly Meal Planning | Lưu công thức để xem lại và xếp một hoặc nhiều công thức vào ngày cùng một trong ba bữa cố định bằng hai dữ liệu độc lập | M02, M04 | Đã chốt ranh giới Saved/Planner, loại bữa và quy tắc nhiều món/chống trùng; còn quy tắc lịch chi tiết. |
| M06 | Gemini AI Access & Usage | Guest/Free/Plus/Pro gọi AI và đo usage thật | M02 một phần, Gemini backend | Đã có hướng; cần user stories. |
| M07 | AI-assisted Moderation | AI rà soát/gắn cờ Recipe Post nhưng không tự áp dụng chế tài | M03, M06 | DEFERRED; không thuộc MVP hiện tại. |
| M08 | Subscription Administration | Hiển thị/đổi gói và quyền AI | M02, M06 | Thanh toán thật; giá, cổng và chính sách còn TBD. |
| M09 | Administration | Xử lý báo cáo, quản lý user/bài công thức/danh mục/bình luận và danh mục nguyên liệu dinh dưỡng | M02–M07, M10 | Không có hàng đợi duyệt từng bài hoặc duyệt quyền đăng; còn giới hạn nội dung, tiêu chí chống lạm dụng, chế tài, tác động dữ liệu cũ và audit trail. |
| M10 | Nutrition Profile & Daily Menu Check | Với Member đủ điều kiện, tính chín chỉ tiêu từ danh mục nguyên liệu dinh dưỡng nội bộ, quy đổi theo khẩu phần và đối chiếu tổng ba bữa với hồ sơ | M02, M04, M05 | Đã chốt đối tượng, chỉ tiêu, nguồn dữ liệu cấp MVP và cách cộng menu; còn dữ liệu khởi tạo, quy đổi và khoảng tham khảo chi tiết khi triển khai. |
| M11 | Nearby Vegetarian Restaurant Discovery | Member nhập địa chỉ và chọn 500 m/1 km/5 km/10 km để nhận đề xuất nhà hàng chay từ Google Maps; không dùng GPS, không liên kết với món đã tìm và không quản lý nhà hàng | M02, Google Maps Geocoding, Places/Nearby Search | Đã chốt ngưỡng đường bộ, danh sách + bản đồ, mặc định 10 lượt/Member/ngày. |

## 6. Thứ tự phân rã để viết SRS

Không viết ERD trước use case. Thứ tự cần giữ:

1. Chốt **User Journey** của M01–M06: mỗi actor bắt đầu ở đâu, làm gì, thành công/thất bại ra sao.
2. Viết **User Stories** theo journey; mỗi story chỉ mô tả một giá trị có thể demo.
3. Viết **Acceptance Criteria** dạng Given/When/Then cho từng story, gồm cả lỗi và quyền bị từ chối.
4. Chuyển các quy tắc lặp lại thành **Business Rules** có mã BR-xx.
5. Từ story và BR, xác định entity/thuộc tính/quan hệ để lập ERD; không thêm bảng không phục vụ yêu cầu.
6. Chuyển Acceptance Criteria thành test cases và liên kết traceability `FR → User Story → Test Case`.

## 7. Danh mục Functional Requirements cấp cao

Các FR dưới đây là khung đánh số. Chỉ mục mang trạng thái **Đã chốt** mới được dùng làm nền để viết chi tiết ngay.

| ID | Yêu cầu cấp cao | Module | Trạng thái |
| --- | --- | --- | --- |
| FR-01 | Hệ thống cho Guest xem và tìm kiếm nội dung đã công khai. | M01 | Đã chốt |
| FR-02 | Hệ thống cho Guest dùng Gemini AI theo Guest Free: tối đa 5 lượt text AI/ngày. | M06 | Đã chốt |
| FR-03 | Đăng ký/đăng nhập email/password và Google Login, xác minh email bắt buộc, quên/đặt lại mật khẩu. | M02 | Đã chốt |
| FR-04 | Hệ thống cho Member đã đăng nhập tạo, xem, sửa, xóa và công khai trực tiếp Recipe Post của chính mình; Member có thể upload ảnh và gắn link YouTube tùy chọn. Không có luồng gửi Blog tổng quát, đơn xin quyền đăng hoặc duyệt trước từng bài. | M02, M03 | ACTIVE — Đã chốt công khai trực tiếp 2026-09-13. |
| FR-05 | Cơ chế Member gửi đơn xin quyền đăng và Administrator duyệt/từ chối đơn trước khi đăng Recipe Post. | M02, M03, M09 | RETIRED — Removed from active scope 2026-09-13; retained only for requirement history. |
| FR-06 | Hệ thống cho Administrator xử lý báo cáo và quản lý thành viên, Recipe Post, bình luận, danh mục. Administrator không duyệt quyền đăng hoặc duyệt từng Recipe Post trước khi công khai. | M09 | ACTIVE — Đã chốt hậu kiểm theo báo cáo 2026-09-13. |
| FR-07 | Hệ thống cho Administrator tạo/cập nhật và phân loại bài công thức chay. | M04 | Đã chốt ở mức cao |
| FR-08 | Tìm/lọc theo từ khóa, loại ăn chay, danh mục, nguyên liệu, thời gian nấu; sắp xếp mới nhất/nhiều Like. | M04 | Đã chốt |
| FR-09 | Hệ thống cho Authorized User tạo và chỉnh lịch ăn tuần bằng một hoặc nhiều mục trong mỗi Bữa sáng/Bữa trưa/Bữa tối của từng ngày. | M05 | Đã chốt ở mức cao; thuộc tính mục lịch chi tiết chưa chốt |
| FR-10 | Hệ thống áp dụng hạn mức Gemini AI theo Guest Free/Free/Plus/Pro, kiểm tra trước khi gọi provider. | M06 | Đã chốt |
| FR-11 | Hệ thống lưu số lượt AI thành công và token usage metadata do Gemini trả về để đo usage thật. | M06 | Đã chốt |
| FR-12 | AI rà soát/gắn cờ Recipe Post nghi vấn và chỉ cung cấp tín hiệu cho Administrator; AI không tự áp dụng chế tài. | M07 | DEFERRED — Không thuộc MVP hiện tại; chỉ xem xét sau khi core hoàn thành. |
| FR-13 | Đăng ký gói AI qua thanh toán thật, xác minh giao dịch trước khi cấp quyền. | M08 | Đã chốt chức năng; giá/cổng/chính sách TBD |
| FR-14 | Hệ thống lưu ảnh bài công thức trên Azure Blob Storage và lưu metadata/tham chiếu trong SQL Server. | M03 | Đã chốt |
| FR-15 | Hệ thống nhúng YouTube player trong bài công thức từ link hợp lệ; hệ thống không upload hoặc sao chép file video YouTube. | M01, M03 | Đã chốt |
| FR-16 | Bài công thức có các mục dữ liệu riêng: nguyên liệu, khẩu phần, thời gian nấu và loại ăn chay; cách làm từng bước là tùy chọn. Tất cả bài dùng chung luồng đăng và duyệt. | M03, M04 | Đã chốt nhóm dữ liệu và tính tùy chọn của hướng dẫn; validation chi tiết chưa chốt |
| FR-17 | Hệ thống trình bày bài công thức đang công khai, không bị ẩn/xóa dưới dạng thẻ món trong Khám phá và nối thẻ/trang chi tiết với luồng lưu công thức hoặc thêm món vào lịch ăn. | M04, M05 | Đã chốt ở mức cao; tương tác chi tiết chưa chốt |
| FR-18 | Admin quản lý nguyên liệu và tạo/sửa/ngừng dùng danh mục món/công thức; một công thức có nhiều danh mục. | M04, M09 | Đã chốt chức năng |
| FR-19 | Người đăng bài công thức chọn nguyên liệu có sẵn hoặc nhập tên mới khi không tìm thấy, rồi khai báo định lượng cụ thể hoặc “vừa đủ”; mỗi dòng giữ tên hiển thị và liên kết danh mục chuẩn tùy chọn. | M03, M04 | Đã chốt nhập linh hoạt; validation chi tiết chưa chốt |
| FR-20 | Hệ thống cung cấp trang chi tiết và thẻ món từ cùng một bài công thức; thực đơn tham chiếu bài này, không tạo bài nguồn hoặc website riêng. | M01, M04, M05 | Đã chốt ở mức cao |
| FR-21 | AI hỗ trợ tạo phần giới thiệu hoặc bước nấu có thể chỉnh sửa từ thông tin tác giả cung cấp, không thêm nguyên liệu; tác giả tự kiểm tra và công khai. AI không tạo lưu nháp Recipe Post bền vững trên server. | M03, M06 | ACTIVE — Mọi gói Member đều có quyền theo quota; mỗi yêu cầu AI thành công dùng một lượt. |
| FR-22 | Trình soạn bài công thức cho tác giả thêm, sửa, xóa và đổi vị trí các bước nấu tùy chọn; nếu có, trang chi tiết hiển thị đúng thứ tự đã lưu. Hướng dẫn do AI hỗ trợ cũng dùng cấu trúc bước này. | M01, M03, M06 | Đã chốt tính tùy chọn, cấu trúc và thao tác; validation chi tiết chưa chốt |
| FR-23 | Hiển thị tác giả gắn tài khoản; hồ sơ có tên/avatar/giới thiệu ngắn/bài đăng/ngày tham gia/tổng Like trên bài. | M01, M02, M03 | Đã chốt |
| FR-24 | Lưu nháp Recipe Post chưa đầy đủ trên server. | M03 | OUT_OF_SCOPE — Deferred out of the current scope 2026-09-13. |
| FR-25 | Khi Member đã đăng nhập bấm công khai và validation đạt, hệ thống công khai Recipe Post ngay. Luồng hiện tại không có trạng thái Draft, Pending Review hoặc Rejected ở cấp bài. | M02, M03 | ACTIVE — Đã chốt công khai trực tiếp 2026-09-13. |
| FR-26 | Tài khoản đã đăng nhập được gửi báo cáo bài công thức có vấn đề; Guest được yêu cầu đăng nhập khi bấm “Báo cáo”. Administrator tiếp nhận, xem xét và xử lý báo cáo theo mục 3.12. | M02, M03, M09 | Đã chốt quyền đăng nhập và luồng xử lý cấp cao |
| FR-27 | Biểu mẫu báo cáo bài công thức cho chọn một trong sáu nhóm lý do tại 3.11 và nhập mô tả bổ sung; mô tả bắt buộc khi chọn “Khác”, tùy chọn với các lý do còn lại. | M03 | Đã chốt cấu trúc; giới hạn độ dài chưa chốt |
| FR-28 | Admin xử lý báo cáo qua Chờ xử lý, Đang xem xét, Đã giải quyết; ghi kết luận/lý do và có thể ẩn hoặc xóa Recipe Post, hoặc khóa tài khoản theo mức độ vi phạm. | M02, M03, M09 | ACTIVE — Chế tài chi tiết và kênh thông báo theo 3.20; cần phân rã trước thiết kế. |
| FR-29 | Hệ thống phân quyền xem báo cáo theo mục 3.13: Admin thấy tài khoản người gửi; tác giả thấy thông tin xử lý nhưng không thấy danh tính người báo cáo; người gửi xem trạng thái/kết quả báo cáo của mình; báo cáo không công khai. | M02, M03, M09 | Đã chốt phạm vi hiển thị; thông báo trong app và email theo 3.20 |
| FR-30 | Người gửi bổ sung mô tả cho báo cáo đang mở; hệ thống ngăn cùng tài khoản tạo báo cáo mở trùng trên một bài công thức. Admin xem báo cáo nhóm theo bài; báo cáo lại sau khi giải quyết cần mô tả vấn đề mới. | M03, M09 | Đã chốt quy tắc cấp cao |
| FR-31 | Member được bỏ qua Onboarding; trước khi dùng AI gợi ý món hoặc tạo thực đơn cá nhân hóa, hệ thống kiểm tra ba nhóm thông tin tối thiểu tại 3.15 và yêu cầu bổ sung nếu thiếu. | M02, M05, M06 | Đã chốt điều kiện cấp cao; lựa chọn chi tiết theo 3.20 |
| FR-32 | Hệ thống cho Member lưu/bỏ lưu bài công thức và xem, tìm/lọc danh sách công thức đã lưu; mỗi bài chỉ xuất hiện một lần trong danh sách của cùng Member. | M02, M04, M05 | Đã chốt |
| FR-33 | Hệ thống cho Member thêm bài công thức vào ngày và một trong ba loại Bữa sáng/Bữa trưa/Bữa tối, chuyển ngày/bữa, thay công thức và xóa mục khỏi lịch ăn. Mỗi bữa được có nhiều công thức nhưng không có cùng công thức trùng trong cùng ngày/bữa. | M02, M04, M05 | Đã chốt ở mức cao; thuộc tính mục lịch chi tiết chưa chốt |
| FR-34 | AI gợi ý món, lập menu và đề xuất thay thế chỉ từ bài công thức đang công khai, không bị ẩn/xóa, dựa trên hồ sơ và nguyên liệu người dùng cung cấp; mọi kết quả dẫn tới bài nguồn và chỉ được lưu sau khi người dùng xác nhận. | M04, M05, M06 | Đã chốt ranh giới; nguyên liệu theo từng yêu cầu, quyền gói theo 3.20 |
| FR-35 | Hệ thống cho Member đủ 18 tuổi, không mang thai/cho con bú và không cần chế độ ăn điều trị khai báo dữ liệu hồ sơ dinh dưỡng tại 3.18 và xem: (1) BMI chỉ để tham khảo; (2) mức tham khảo mỗi ngày của chín chỉ tiêu được ước tính từ hồ sơ rộng hơn BMI. Kết quả phải nêu rõ giới hạn ước tính và không phải chẩn đoán hoặc điều trị. | M02, M10 | ACTIVE — Đã chốt đối tượng, đầu ra và ranh giới; công thức/range theo Q31 cần nghiên cứu trước triển khai. |
| FR-36 | AI lập menu theo nhu cầu dinh dưỡng chỉ từ bài công thức đang công khai, không bị ẩn/xóa và có kết quả đủ tin cậy được tính từ nguyên liệu/định lượng, ưu tiên ràng buộc ăn uống và yêu cầu người dùng xác nhận trước khi lưu. | M04, M05, M06, M10 | Đã chốt ranh giới, chỉ tiêu và nguồn tính cấp nghiệp vụ; quyền gói đã chốt, khoảng mục tiêu nghiên cứu khi triển khai |
| FR-37 | Hệ thống cho Member khai báo số khẩu phần của từng món trong Lịch ăn và kiểm tra menu một ngày theo chín chỉ tiêu tại 3.18; kết quả cộng tất cả món trong ba bữa, hiển thị mức tham khảo cá nhân, tổng, chênh lệch, trạng thái và cảnh báo thiếu dữ liệu. AI được giải thích hoặc đề xuất món thay thế đã có. | M05, M06, M10 | Đã chốt chỉ tiêu, cách cộng menu và ranh giới trình bày; khoảng tham khảo chưa chốt |
| FR-38 | Trước chức năng dinh dưỡng, hệ thống yêu cầu Member xác nhận thuộc phạm vi hỗ trợ; người không đủ điều kiện được giải thích giới hạn và vẫn sử dụng các chức năng không-dinh-dưỡng. | M02, M10 | Đã chốt |
| FR-39 | Hệ thống tính tổng ước tính của chín chỉ tiêu cho công thức bằng cách cộng dữ liệu từ danh mục nguyên liệu dinh dưỡng nội bộ theo định lượng, sau đó quy đổi theo số khẩu phần đã khai báo; khi thiếu dữ liệu phải công khai phạm vi thiếu thay vì coi là 0 hoặc để AI đoán. | M03, M04, M10 | Đã chốt nguồn tính, nguồn dữ liệu cấp MVP và xử lý thiếu; dữ liệu khởi tạo/quy đổi chi tiết chưa chốt |
| FR-40 | Hệ thống cho Member đã đăng nhập công khai Recipe Post chứa nguyên liệu ngoài danh mục dinh dưỡng; nguyên liệu đó được đánh dấu chưa hỗ trợ tính dinh dưỡng, khiến kết quả không đầy đủ và bài không tham gia AI menu theo mục tiêu dinh dưỡng. | M03, M04, M06, M10 | ACTIVE — Đã chốt. |
| FR-41 | Hệ thống cho Administrator xem/tìm kiếm, thêm, sửa, bật lại hoặc ngừng hỗ trợ nguyên liệu dinh dưỡng; quản lý tên, chín giá trị trên 100 g, tên/đường dẫn nguồn, ngày tham khảo và xem các công thức đang sử dụng nguyên liệu. Chức năng này không cho AI tự tạo hoặc xác nhận số liệu. | M09, M10 | Đã chốt thao tác MVP; tính lại công thức/menu liên quan, ghi thay đổi và nguồn |
| FR-42 | Member tìm nhà hàng theo địa chỉ nhập và ngưỡng đường bộ 500 m/1 km/5 km/10 km, không GPS hoặc liên kết món đã tìm; mặc định 10 lượt/Member/ngày, không cho Guest gọi. | M02, M11 | Đã chốt; phương tiện/tích hợp TBD |
| FR-43 | Hiển thị danh sách và bản đồ nhúng từ Google, ghi nguồn và trạng thái lỗi; không quản lý hồ sơ hoặc lưu nhà hàng yêu thích. | M11 | Đã chốt |
| FR-44 | Member đã đăng nhập được sửa hoặc xóa Recipe Post đã công khai của chính mình. Thay đổi đạt validation được công khai mà không cần Administrator duyệt lại; bài do tác giả xóa không còn xuất hiện công khai hoặc trong gợi ý AI. Quy tắc riêng cho bài đang bị Admin ẩn vẫn áp dụng. | M02, M03 | ACTIVE — Đã chốt quyền quản lý bài cấp cao; không phân rã thêm. |
| FR-45 | Member Like/Unlike bài, bình luận hoặc reply, tối đa một Like hiệu lực mỗi nội dung; Guest chỉ xem tổng, không có đánh giá sao. | M01, M02, M03 | Đã chốt |
| FR-46 | Hệ thống hiển thị bình luận cho Guest và cho Member đã đăng nhập tạo bình luận, reply một bình luận/reply khác theo cấu trúc lồng nhiều cấp, đồng thời sửa/xóa bình luận của chính mình. Administrator được quản lý bình luận vi phạm. | M01, M02, M03, M09 | Đã chốt chức năng nhiều cấp; giới hạn độ sâu và hiển thị chi tiết chưa chốt |
| FR-47 | Gợi ý bài liên quan theo danh mục/nguyên liệu không AI; tùy chọn yêu cầu Gemini riêng có tính lượt. | M04, M06 | Đã chốt |
| FR-48 | Member báo cáo bình luận/reply; Admin hậu kiểm theo quyền riêng tư và nguyên tắc chống trùng báo cáo bài, thích ứng lý do theo loại nội dung. Bình luận/reply công khai ngay. | M03, M09 | Đã chốt |
| FR-49 | Hệ thống gửi thông báo trong app và email cho reply mới và quyết định xử lý nội dung được báo cáo. Quyền xem báo cáo và bảo vệ danh tính người báo cáo thuộc FR-29 và BR-28, không thuộc FR này. | M02, M03, M09 | ACTIVE — Đã tách trách nhiệm để không trùng FR-29, 2026-09-13. |
| FR-50 | Member xem/xóa lịch sử hội thoại AI riêng; Guest không có lịch sử tài khoản. | M02, M06 | OUT_OF_SCOPE — Deferred out of the current scope 2026-09-13. |
| FR-51 | Chatbot giải thích dinh dưỡng chay, BMI/calorie và thay thế nguyên liệu; không tự tạo/sửa công thức hoặc số liệu. | M06, M10 | Đã chốt |

## 8. Business Rules đã chốt

| ID | Quy tắc |
| --- | --- |
| BR-01 | Guest và tài khoản Free có tối đa 5 lượt text AI mỗi ngày theo giới hạn của app. |
| BR-02 | Plus có tối đa 15 lượt text AI/ngày; Pro có tối đa 50 lượt text AI/ngày. |
| BR-03 | Lượt AI chỉ tăng sau khi backend nhận phản hồi Gemini hợp lệ và lưu kết quả thành công. |
| BR-04 | Lỗi provider/timeout trước phản hồi hợp lệ không được trừ lượt, nhưng phải ghi event lỗi để đối soát. |
| BR-05 | Guest không có lịch sử AI, hồ sơ hoặc thực đơn cá nhân hóa. |
| BR-06 | Gemini API key chỉ lưu ở backend/biến môi trường; frontend không được biết API key. |
| BR-07 | Member đã đăng nhập được tạo và công khai Recipe Post trực tiếp sau khi bài đạt validation; không có trạng thái quyền đăng riêng hoặc Admin duyệt trước từng bài. |
| BR-08 | DEFERRED — Nếu AI gắn cờ được triển khai trong tương lai, AI chỉ hỗ trợ gắn cờ và không tự ẩn/xóa bài hoặc khóa tài khoản. |
| BR-09 | Câu trả lời AI phải nêu giới hạn hỗ trợ và không được trình bày như chẩn đoán/điều trị. |
| BR-10 | Phase 1 không nhận upload file video; bài công thức chỉ lưu link hoặc video ID của YouTube. |
| BR-11 | Recipe Post hợp lệ có media do Member đã đăng nhập công khai trực tiếp; media vẫn thuộc chính sách nội dung và có thể bị báo cáo/xử lý sau đăng. |
| BR-12 | Liên kết nguyên liệu chuẩn là tùy chọn. Tên mới trên công thức không tự trở thành mục chuẩn; việc thiếu liên kết không tự chặn hoàn thiện hoặc công khai bài theo workflow chung. |
| BR-13 | Nguyên liệu chưa nhận diện không mặc định thỏa mãn ràng buộc ăn uống; hệ thống không khẳng định món phù hợp với ràng buộc loại trừ khi chưa kiểm tra được thành phần. |
| BR-14 | Dòng nguyên liệu dùng “vừa đủ” không yêu cầu số lượng/đơn vị, không lưu số giả thay lượng chưa xác định và không tự quy đổi sang gram hoặc suy ra dinh dưỡng chính xác. |
| BR-15 | Bài tự viết và bài có AI hỗ trợ đều tuân theo cùng format/chính sách; AI chỉ hỗ trợ nội dung có thể chỉnh sửa trong biểu mẫu, không tự công khai hoặc lưu nháp bền vững. |
| BR-16 | AI lỗi hoặc hết lượt không được chặn Member đã đăng nhập tiếp tục tự viết/chỉnh sửa và công khai Recipe Post đáp ứng điều kiện. |
| BR-17 | Tác giả được gắn với tài khoản đăng bài; người đăng không được chọn tài khoản khác đứng tên. Việc Admin cấp quyền đăng hoặc AI hỗ trợ không thay đổi tác giả. |
| BR-18 | Hồ sơ tác giả công khai không tiết lộ email, thông tin đăng nhập hoặc hồ sơ ăn uống riêng tư; gắn tài khoản không được trình bày như xác minh danh tính thật hay chuyên môn. |
| BR-19 | Recipe Post chỉ được công khai khi Member đã đăng nhập và bài đủ các mục bắt buộc tại 3.9. Bài thiếu dữ liệu không được công khai và không được lưu nháp bền vững trong phạm vi hiện tại. Hướng dẫn từng bước không phải trường bắt buộc. |
| BR-20 | Giới thiệu, hướng dẫn từng bước và media không bắt buộc khi công khai bài công thức; thẻ món không có ảnh dùng ảnh mặc định. Thời gian nấu được bằng 0. |
| BR-21 | Cơ chế trạng thái đơn xin quyền đăng và kiểm tra quyền đăng trước khi tạo/công khai Recipe Post. | RETIRED — Removed from active scope 2026-09-13; retained only for requirement history. |
| BR-22 | Quy tắc Administrator phải nêu lý do khi từ chối đơn xin quyền đăng hoặc thu hồi quyền đăng. | RETIRED — Removed with the permission-application workflow 2026-09-13. |
| BR-23 | Báo cáo của người dùng là tín hiệu để xem xét, không phải kết luận vi phạm; báo cáo cơ bản phải hoạt động khi AI chưa được triển khai hoặc bị lỗi. Nếu AI gắn cờ được triển khai sau này, AI không tự quyết định xử lý Recipe Post. |
| BR-24 | Hệ thống chỉ tiếp nhận báo cáo bài công thức từ tài khoản đã đăng nhập; Guest phải đăng nhập trước. Kiểm tra xác thực được thực hiện ở backend. |
| BR-25 | Mỗi báo cáo phải có một lý do hợp lệ trong sáu nhóm đã chốt. Nếu chọn “Khác”, mô tả không được rỗng hoặc chỉ chứa khoảng trắng; không tạo báo cáo khi chưa thỏa điều kiện. |
| BR-26 | Chỉ Admin quyết định ẩn/xóa Recipe Post hoặc khóa tài khoản trong quy trình xử lý báo cáo; số lượng báo cáo không tự tạo chế tài. Quy tắc này không ngăn tác giả tự xóa bài của chính mình. Báo cáo đã giải quyết phải có kết luận và lý do. |
| BR-27 | Bài công thức bị ẩn không tự được công khai lại khi tác giả sửa hoặc khi báo cáo đóng; chỉ Admin được khôi phục bài như một quyết định hậu kiểm. |
| BR-28 | Báo cáo không công khai. Backend phải giới hạn quyền xem theo 3.13 và không cung cấp danh tính người báo cáo cho tác giả bài công thức bị báo cáo. |
| BR-29 | Mỗi tài khoản chỉ có một báo cáo chưa giải quyết trên cùng bài công thức; thông tin mới được bổ sung vào báo cáo đang mở. Báo cáo lại sau khi giải quyết phải mô tả điểm mới. |
| BR-30 | Bỏ qua Onboarding không khóa các chức năng không cá nhân hóa đã nêu tại 3.15; chỉ chặn yêu cầu AI cá nhân hóa khi thiếu dữ liệu tối thiểu. |
| BR-31 | Khi chặn AI cá nhân hóa vì thiếu hồ sơ, backend không gọi Gemini và không trừ lượt AI. Giá trị để trống không được tự hiểu là người dùng xác nhận “Không có”. |
| BR-32 | Chỉ Member đã đăng nhập có dữ liệu Công thức đã lưu và Lịch ăn; Guest phải đăng nhập trước khi hệ thống tạo các dữ liệu này. |
| BR-33 | Lưu công thức không gọi Gemini, không trừ lượt AI và không tự cập nhật Onboarding hoặc hồ sơ sở thích. |
| BR-34 | Mỗi Member chỉ có tối đa một bản ghi lưu cho cùng một bài công thức; yêu cầu lưu lặp không tạo dữ liệu trùng. |
| BR-35 | Công thức đã lưu và mục lịch ăn có vòng đời độc lập: bỏ lưu không xóa mục lịch, xóa mục lịch không bỏ lưu. Cùng một công thức được phép xuất hiện ở nhiều ngày/bữa. |
| BR-36 | Mỗi mục lịch ăn phải thuộc đúng một trong ba loại `Breakfast`, `Lunch` hoặc `Dinner`; MVP không có `Snack` hay loại bữa do người dùng tự tạo. |
| BR-37 | Một bữa được có nhiều công thức và không có giới hạn cứng số món trong MVP. Với cùng Member, tổ hợp bài công thức–ngày–loại bữa phải duy nhất; yêu cầu thêm trùng không tạo mục lịch mới. |
| BR-38 | AI không tạo công thức mới để phục vụ gợi ý hoặc menu. Nếu các công thức đang công khai, không bị ẩn/xóa không có lựa chọn phù hợp, hệ thống phải nói rõ thay vì tạo nội dung không tồn tại. |
| BR-39 | BMI không được dùng một mình để suy ra nhu cầu calorie/dưỡng chất, đánh giá sức khỏe hoặc quyết định món phù hợp; BMI chỉ là chỉ số tham khảo trong hồ sơ rộng hơn. |
| BR-40 | Menu theo dinh dưỡng và kiểm tra menu ngày chỉ dùng số liệu từ công thức có dữ liệu dinh dưỡng đủ và đáng tin cậy; thiếu dữ liệu phải được công khai và không được AI tự ước lượng như dữ kiện. |
| BR-41 | “Thấp hơn mức tham khảo” trong một ngày không được trình bày là chẩn đoán thiếu chất; mọi kết quả calorie/dưỡng chất là ước tính hỗ trợ lập kế hoạch và không thay thế chuyên gia. |
| BR-42 | MVP không cung cấp tính nhu cầu, AI menu dinh dưỡng hoặc kiểm tra menu ngày cho người dưới 18 tuổi, người mang thai/cho con bú hoặc người cần chế độ ăn điều trị; giới hạn này không được khóa chức năng thông thường của họ. |
| BR-43 | Số liệu dinh dưỡng trên trang công thức phải gắn với một khẩu phần; tỷ lệ tham chiếu chung không được trình bày như mục tiêu cá nhân. Kiểm tra menu ngày phải sử dụng số khẩu phần đã ghi nhận và mức tham khảo cá nhân. |
| BR-44 | Không áp dụng cùng một cách hiểu phần trăm cho mọi chỉ tiêu: natri được theo dõi theo giới hạn tối đa, còn trạng thái của năng lượng và chất đa lượng dựa trên khoảng tham khảo đã chốt. Không khuyến khích người dùng tăng natri chỉ để đạt 100%. |
| BR-45 | MVP không tạo điểm cân bằng dinh dưỡng tổng hợp và không hiển thị Glycemic Index/Glycemic Load. Hệ thống phải trình bày từng chỉ tiêu và không được che giấu dữ liệu thiếu bằng một nhãn “Tốt/Xấu”. |
| BR-46 | Dinh dưỡng công thức phải được suy ra từ danh mục nguyên liệu dinh dưỡng nội bộ và định lượng đã khai báo. Bảng tổng do tác giả tự nhập hoặc con số do AI sinh ra không được coi là dữ liệu dinh dưỡng đã xác minh. |
| BR-47 | Tổng dinh dưỡng toàn công thức được phân bổ theo số khẩu phần của công thức. Khi thêm vào Lịch ăn, mặc định là một khẩu phần nhưng Member được thay đổi số khẩu phần dự định ăn; tổng ngày phải cộng mọi mục thuộc cả ba bữa theo lượng này. |
| BR-48 | Nguyên liệu thiếu định lượng, dùng “vừa đủ”, không quy đổi được hoặc chưa có dữ liệu dinh dưỡng không được tự tính là 0. Hệ thống phải đánh dấu kết quả chưa đầy đủ và AI không được che giấu hoặc tự bù dữ liệu thiếu. |
| BR-49 | MVP không gọi API dinh dưỡng hoặc dùng AI để tự đối chiếu/tạo số liệu khi người dùng thao tác. Dữ liệu nội bộ phải giữ nguồn tham khảo; USDA FoodData Central là nguồn tham khảo chính cho bộ dữ liệu ban đầu. |
| BR-50 | Nguyên liệu ngoài danh mục dinh dưỡng không chặn công khai Recipe Post theo workflow chung. Bài có kết quả dinh dưỡng chưa đầy đủ không được sử dụng trong AI menu có mục tiêu dinh dưỡng. |
| BR-51 | Chỉ Administrator được quản lý dữ liệu trong danh mục nguyên liệu dinh dưỡng. Tác giả công thức và AI không được trực tiếp tạo hoặc thay đổi các giá trị dinh dưỡng dùng làm dữ liệu tính toán chính thức. |
| BR-52 | Mỗi nguyên liệu dinh dưỡng được thêm/sửa phải có đủ chín giá trị trên 100 g, tên nguồn, đường dẫn nguồn và ngày tham khảo hợp lệ trước khi được bật để tính toán chính thức. |
| BR-53 | MVP không xóa vĩnh viễn nguyên liệu dinh dưỡng đã được tham chiếu. Administrator dùng trạng thái ngừng hỗ trợ; dữ liệu ngừng hỗ trợ không được chọn cho liên kết mới nhưng vẫn phải giữ khả năng truy vết công thức đã sử dụng. |
| BR-54 | MVP không nhập hàng loạt, không tự gọi USDA và không cho AI tự điền dữ liệu vào danh mục dinh dưỡng. |
| BR-55 | Tìm nhà hàng dùng địa chỉ/địa điểm do người dùng chủ động nhập hoặc chọn; MVP không xin quyền, đọc hoặc lưu GPS/vị trí hiện tại của thiết bị. |
| BR-56 | Kết quả nhà hàng phải đến từ Google Maps Platform và không được Gemini tạo ra. Tìm nhà hàng không trừ lượt AI và app không được trình bày phân loại của Google như xác minh độc lập của hệ thống. |
| BR-57 | Khi địa chỉ không xác định được, không có kết quả hoặc dịch vụ Google lỗi/hết hạn mức, hệ thống phải thông báo đúng trạng thái và không tạo địa điểm thay thế không có nguồn. |
| BR-58 | Gửi báo cáo và xử lý báo cáo là hai nghiệp vụ độc lập. Một báo cáo không tự ẩn/xóa Recipe Post hoặc khóa tài khoản nếu Admin chưa quyết định. |
| BR-59 | MVP không có hàng đợi duyệt từng Recipe Post. Bài hợp lệ của Member đã đăng nhập được công khai trực tiếp khi tác giả chủ động công khai. |
| BR-60 | Quy tắc xác nhận chính sách, trách nhiệm và lý do đóng góp trước khi gửi đơn xin quyền đăng. | RETIRED — Removed with the permission-application workflow 2026-09-13. |
| BR-61 | Quy tắc mỗi Member có tối đa một đơn xin quyền đăng Chờ duyệt và được gửi lại sau khi bị từ chối. | RETIRED — Removed with the permission-application workflow 2026-09-13. |
| BR-62 | Khi sửa Recipe Post đang công khai, backend phải kiểm tra tác giả và các trường bắt buộc. Bài bị Administrator ẩn không được tự công khai lại qua chức năng sửa. |
| BR-63 | Đề xuất nhà hàng chỉ dựa trên địa chỉ/bán kính và dữ liệu Google Maps Platform, không dựa trên món đã tìm. Hệ thống không duy trì danh mục nhà hàng do Administrator quản lý và không được trình bày dữ liệu ngoài như dữ liệu đã được app xác minh. |
| BR-64 | Member đã đăng nhập chỉ được sửa/xóa Recipe Post do chính tài khoản đó đứng tên. Bài do tác giả xóa không còn công khai hoặc được AI gợi ý; bài đang bị Admin ẩn không được tác giả tự khôi phục bằng thao tác quản lý bài cá nhân. |
| BR-65 | Mỗi cặp Member–bài công thức chỉ có tối đa một Like đang hiệu lực. Thích lại không tạo lượt trùng; bỏ Like loại lượt của Member khỏi tổng. Like không tự lưu công thức, cập nhật sở thích, thêm món vào lịch hoặc trừ lượt AI. |
| BR-66 | Mỗi reply phải thuộc cùng bài công thức với bình luận cha và có thể tiếp tục nhận reply ở cấp sâu hơn. Chỉ chủ sở hữu được sửa/xóa bình luận của mình; quyền quản trị nội dung vi phạm của Administrator vẫn được áp dụng. |
| BR-67 | Backend chỉ gửi yêu cầu tìm nhà hàng tới Google Maps Platform sau khi xác thực Member. Yêu cầu của Guest bị chặn trước khi gọi dịch vụ ngoài và không tiêu thụ quota tìm kiếm của ứng dụng. |
| BR-68 | Bán kính tìm nhà hàng trong MVP chỉ nhận một trong bốn giá trị 500 m, 1 km, 5 km hoặc 10 km; giá trị khác bị từ chối trước khi gọi Google Maps Platform. |

## 9. Non-functional Requirements cần cụ thể hóa

| Nhóm | NFR cần viết trong SRS | Điều cần chốt/đo |
| --- | --- | --- |
| Security | Role-based access, backend-only Gemini key, input validation, rate limit | Cơ chế login, token/session và giới hạn upload ảnh. |
| Privacy | Giới hạn log prompt/dữ liệu hồ sơ; thông báo dữ liệu gửi Gemini | Thời hạn lưu và quyền xóa cho các log cần thiết còn phải chốt; không có lịch sử hội thoại AI theo tài khoản trong MVP. |
| Reliability | Xử lý timeout/429, không trừ lượt oan, idempotency cho request AI | Thời gian timeout/retry tối đa. |
| Performance | Tìm kiếm, danh sách, hàng đợi báo cáo và phản hồi AI có trạng thái loading/error | Mục tiêu thời gian phản hồi cần nhóm xác định. |
| Usability | Hiển thị gói, lượt còn lại, reset và lý do bị chặn | Web responsive tiếng Việt; tiêu chí khả dụng để thiết kế. |
| Auditability | Lưu moderation decision và AI usage event có requestId | Thời hạn lưu log và quyền xem log. |

## 10. Việc còn mở cho thiết kế/triển khai

Phạm vi Q01–Q38 đã xác nhận tại 3.20. Còn giá/cổng/chu kỳ/chính sách thanh toán; phương tiện và tích hợp khoảng cách đường bộ; validation, quyền riêng tư/thời hạn lưu, quy tắc email và hiển thị cây bình luận. Công thức/hệ số/khoảng dinh dưỡng nghiên cứu khi triển khai Q31, không cần nghiên cứu ngay để tiếp tục SRS.

## 11. Bước tiếp theo

Viết User Stories, Use Cases và Acceptance Criteria theo phạm vi đã chốt, sau đó thiết kế ERD/API/Test Cases. Không tự thêm chức năng hoặc coi các công thức dinh dưỡng chưa nghiên cứu là đã xác nhận.

## 12. Truy vết đề tài

| Yêu cầu đề tài | FR | Phạm vi nhóm chọn |
| --- | --- | --- |
| Quản lý thành viên/bài/bình luận/danh mục | FR-04, FR-06, FR-18, FR-28 | Công khai trực tiếp, hậu kiểm nội dung theo báo cáo |
| Bình luận/bình chọn/báo cáo/quản lý nội dung mình | FR-26–FR-30, FR-44–FR-46, FR-48 | Like không đánh giá sao, reply nhiều cấp |
| Thực đơn tuần theo nguyên liệu và BMI | FR-09, FR-31, FR-33–FR-39 | Hồ sơ rộng hơn BMI, AI chọn công thức có sẵn |
| Tìm/gợi ý nhà hàng chay | FR-42, FR-43 | Needs clarification — chờ giảng viên xác nhận phạm vi và tiêu chí nghiệm thu. |
| Bài liên quan công thức đã tìm | FR-47 | Thông thường hoặc tùy chọn AI |
| Chatbot dinh dưỡng, thay nguyên liệu, BMI/calorie | FR-51 | Không tạo công thức/số liệu |
| Giới hạn và đăng ký gói AI | FR-10, FR-11, FR-13 | 5/15/50, cùng chức năng; thanh toán thật |
| Guest tìm/xem video, Blog và thử AI | FR-01, FR-02, FR-15, FR-20 | Blog công thức, YouTube nhúng, AI cơ bản 5 lượt |

Đây là truy vết yêu cầu, không phải chứng cứ tính năng đã triển khai.