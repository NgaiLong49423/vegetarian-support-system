> **Document:** Software Requirements Specification  
> **File:** `docs/requirements/SRS.md`  
> **Version:** v0.19.0  
> **Created:** 2026-09-11  
> **Last Updated:** 2026-09-12  
> **Status:** Draft  
> **Related Docs:** `00-de-tai-03-huong-di-dau-tien.md`, `01-phan-ra-goi-ai.md`  

# Software Requirements Specification

## 1. Mục đích và trạng thái tài liệu

Tài liệu này là khung SRS cho **Đề tài 03 — Ứng dụng hỗ trợ người ăn chay**. Nó gom những quyết định sản phẩm đã chốt và chia dự án thành các phần đủ nhỏ để viết Functional Requirements (FR), User Stories, Acceptance Criteria, ERD và Test Cases.

`Draft` nghĩa là tài liệu chưa phải nguồn yêu cầu cuối cùng để lập trình. Các mục ghi **Chưa chốt** chỉ là điểm cần quyết định, không phải yêu cầu đã được phê duyệt.

## 2. Mô tả sản phẩm

Định hướng được người dùng chốt ngày 11/09/2026: **Ứng dụng khám phá món ăn và lập thực đơn chay theo tuần, sử dụng Gemini AI để gợi ý món phù hợp từ các Blog công thức đã được kiểm duyệt; người dùng chủ động chọn và thay đổi thực đơn.**

Luồng lõi: khai báo sở thích → khám phá món → nhận gợi ý AI → chọn món → lưu vào thực đơn tuần → chọn món thay thế khi cần. Blog và cộng đồng cung cấp nội dung hỗ trợ luồng này. Người dùng dùng chung luồng tạo Blog Post để chia sẻ công thức.

Gợi ý món phải tham chiếu bài công thức đã duyệt trong hệ thống. Hệ thống kiểm tra ràng buộc bắt buộc và kết quả AI; khi không đủ món phù hợp phải thông báo rõ. Người dùng quyết định lưu/thay món. Các trường dữ liệu để kiểm tra nguyên liệu và sở thích sẽ được phân rã riêng; không suy ra cam kết an toàn dị ứng từ việc Admin đã duyệt bài.

Ý tưởng Onboarding Questionnaire sau đăng ký đã được ghi nhận: thu thập sở thích ăn uống phục vụ chọn món. Danh sách câu hỏi, bắt buộc hay cho bỏ qua và quyền theo gói còn chờ chốt tại [bản phân rã actor và onboarding](actors-and-onboarding-draft.md). BMI có trong nguồn đề bài nên vẫn cần làm rõ với giảng viên; chưa quyết định loại bỏ.

AI hỗ trợ hỏi đáp, tìm kiếm/gợi ý, soạn bản nháp Blog công thức theo mẫu và có thể hỗ trợ moderation. AI không phải chuyên gia dinh dưỡng, không tự quyết định duyệt/xóa nội dung và không đưa chẩn đoán hoặc điều trị sức khỏe.

## 3. Ranh giới hệ thống

### 3.1 Trong phạm vi đã chốt

- Nội dung cộng đồng: Blog Post, bình luận và danh mục. Một Blog Post có thể chia sẻ công thức chay, chứa ảnh và nhúng video YouTube.
- Chia sẻ công thức dùng cùng workflow tạo và kiểm duyệt Blog Post; khi chọn loại bài công thức, biểu mẫu có thêm các mục riêng: nguyên liệu, cách làm, khẩu phần, thời gian nấu và loại ăn chay. Không tạo chức năng xuất bản công thức độc lập.
- Media Phase 1: ảnh được upload lên Azure Blob Storage; Blog Post có thể chứa link YouTube và phát bằng embedded player trong app.
- Kiểm duyệt: nội dung người dùng đăng phải qua Administrator duyệt trước khi công khai. Người dùng có chức năng báo cáo Blog có vấn đề; AI rà soát/gắn cờ là chức năng nâng cao chỉ triển khai nếu còn thời gian.
- Người dùng: tìm kiếm/lọc, xem nội dung, tương tác cộng đồng và lập thực đơn tuần.
- AI Gemini: hỏi đáp/gợi ý với Guest Free, Free, Plus và Pro theo lượt gọi/ngày.
- Quản trị: quản lý người dùng, Blog Post (gồm bài công thức), danh mục, video, bình luận và nội dung bị gắn cờ.

### 3.2 Ngoài phạm vi hiện tại

- Actor chuyên gia dinh dưỡng/thực phẩm độc lập và xác minh chứng chỉ.
- Chẩn đoán, điều trị, tư vấn sức khỏe chuyên nghiệp hoặc thay thế bác sĩ.
- Thanh toán thật, hóa đơn, hoàn tiền hay tự động gia hạn thật.
- Tích hợp Apple Health, Google Fit, wearable hoặc vị trí.
- Nhận diện ảnh nguyên liệu, tóm tắt video, dự báo mùa/giá nguyên liệu.
- Upload/lưu file video trực tiếp trên Azure Blob Storage. Phase 1 chỉ lưu link hoặc YouTube video ID.
- Gói Max “vô hạn”. Nếu có sau này phải là Fair Use và có quota thực tế.

### 3.3 Blog công thức và hướng tham khảo Samsung Food — đã chốt 11/09/2026

Samsung Food là sản phẩm tham khảo chính cho cách nối khám phá món, chi tiết công thức và Planner; không phải toàn bộ phạm vi cần sao chép. Blog/cộng đồng vẫn hỗ trợ luồng lõi, không chuyển dự án thành mạng xã hội.

Blog loại công thức có năm nhóm dữ liệu riêng đã được chấp thuận:

| Nhóm dữ liệu | Mục đích |
| --- | --- |
| Nguyên liệu | Cho người đọc biết thành phần; làm đầu vào cho lọc và kiểm tra gợi ý AI. |
| Cách làm | Hướng dẫn thực hiện món ăn. |
| Khẩu phần | Cho biết công thức phục vụ bao nhiêu người/phần. |
| Thời gian nấu | Hỗ trợ người dùng chọn món phù hợp thời gian. |
| Loại ăn chay | Hỗ trợ phân loại và tìm món theo chế độ ăn. |

Trang Khám phá trình bày Blog công thức đã duyệt dưới dạng thẻ món; trang chi tiết nối tới luồng thêm món vào thực đơn. Đây là cách hiển thị và sử dụng cùng nội dung Blog, không phải một kho nội dung do người dùng xuất bản riêng.

Trường bắt buộc khi gửi duyệt được chốt tại mục 3.9. **Chưa chốt:** danh sách loại ăn chay, giới hạn giá trị và quy tắc chuẩn hóa chi tiết. Có trường nguyên liệu không đồng nghĩa hệ thống đã bảo đảm kiểm tra dị ứng chính xác. Chưa tự thêm import công thức từ URL, Queue, shopping list hoặc tính dinh dưỡng từ sản phẩm tham khảo.

### 3.4 Nhập nguyên liệu linh hoạt và chuẩn hóa — đã chốt 11/09/2026

Quyết định này thay thế yêu cầu bắt buộc mọi nguyên liệu phải có trong danh mục ở v0.6.0. Chuẩn hóa phục vụ tìm/lọc và AI, không phải điều kiện để người dùng hoàn thiện công thức.

- Administrator quản lý danh mục nguyên liệu chuẩn dùng chung.
- Ô nhập nguyên liệu ưu tiên gợi ý từ danh mục. Người đăng có thể chọn kết quả có sẵn hoặc dùng tên mới khi không tìm thấy, rồi khai báo lượng cho từng dòng theo mục 3.5.
- Mỗi dòng giữ tên hiển thị do người dùng nhập/chọn, thông tin lượng và liên kết nguyên liệu chuẩn nếu có. Liên kết này là tùy chọn; khi chuẩn hóa sau vẫn giữ tên đã nhập để hiển thị công thức.
- Ví dụ: chọn `Đậu hũ` có sẵn và nhập `200 g`; hoặc dùng tên mới `Nấm hầu thủ` và nhập `100 g` dù danh mục chưa có.
- Tên mới thuộc dòng nguyên liệu của công thức, không tự trở thành mục chuẩn dùng chung. Người đăng vẫn hoàn thiện và gửi Blog duyệt bình thường, không phải chờ Admin bổ sung danh mục hay qua cửa duyệt nguyên liệu riêng.
- Admin duyệt toàn bộ Blog theo workflow chung. Việc chưa liên kết danh mục không tự ngăn bài được duyệt/công khai; các tiêu chí kiểm duyệt nội dung vẫn áp dụng. Admin có thể chuẩn hóa/liên kết nguyên liệu sau.
- Tìm kiếm có thể sử dụng cả tên đã nhập và danh mục chuẩn. AI hỗ trợ đối chiếu tên chỉ là khả năng đề xuất, chưa chốt triển khai và không mặc định kết quả AI đúng.
- Không coi nguyên liệu chưa nhận diện là phù hợp với mọi chế độ ăn. Với gợi ý có ràng buộc loại trừ, hệ thống không tự khẳng định món phù hợp khi chưa kiểm tra được thành phần.

**Chưa chốt:** cách xử lý món chưa kiểm tra đủ thành phần trong gợi ý AI; cơ chế tên đồng nghĩa; danh sách/kiểm soát đơn vị; quy tắc số lượng hợp lệ; sửa/ngừng sử dụng nguyên liệu chuẩn đã được tham chiếu. Chưa thiết kế ERD hoặc workflow chuẩn hóa chi tiết.

### 3.5 Định lượng nguyên liệu — đã chốt 12/09/2026

- Mỗi dòng nguyên liệu hỗ trợ một trong hai cách khai báo: định lượng cụ thể bằng số lượng và đơn vị (ví dụ `200 g đậu hũ`), hoặc `vừa đủ` (ví dụ `muối — vừa đủ`).
- Với `vừa đủ`, không bắt buộc nhập số lượng hay đơn vị; hệ thống giữ rõ cách khai báo này, không dùng số giả như 0 để biểu diễn lượng chưa xác định.
- Hệ thống không tự quy đổi `vừa đủ` sang gram và không suy ra dinh dưỡng chính xác từ lượng chưa xác định. Quyết định này không bổ sung chức năng tính dinh dưỡng vào phạm vi hiện tại.
- Việc dùng `vừa đủ` không làm mất tên nguyên liệu hay liên kết danh mục nếu có; nguyên liệu vẫn thuộc thành phần công thức để kiểm tra ràng buộc ăn uống.

**Chưa chốt:** danh sách đơn vị, định dạng số/phân số, giới hạn định lượng và xử lý khi thay đổi khẩu phần.

### 3.6 Một Blog công thức, nhiều cách hiển thị và AI hỗ trợ soạn bài — đã chốt 12/09/2026

- Tác giả tạo một Blog công thức có cấu trúc. Thẻ món ở Khám phá, trang chi tiết và tham chiếu trong thực đơn đều sử dụng cùng bài này; không yêu cầu tạo hai bài hoặc một website thứ hai.
- Nút “Xem công thức” mở trang chi tiết nội bộ với nội dung hướng dẫn đầy đủ. Không dùng liên kết website ngoài làm nơi bắt buộc đọc cách nấu; đề xuất đóng góp chỉ bằng dẫn nguồn ngoài chưa được chấp thuận.
- Biểu mẫu có các mục tên món, giới thiệu, nguyên liệu, khẩu phần, thời gian, loại ăn chay, hướng dẫn nấu và media theo chính sách hiện tại. Format quy định cấu trúc; tác giả vẫn được viết theo văn phong riêng. Điều kiện đầy đủ khi gửi duyệt nằm tại mục 3.9; validation chi tiết còn chờ chốt.
- Tác giả có thể tự viết hoặc chủ động yêu cầu Gemini hỗ trợ soạn bản nháp theo mẫu từ thông tin đã cung cấp. Không bắt buộc dùng AI để tạo hoặc gửi bài.
- Tác giả xem, sửa và xác nhận nội dung AI trước khi dùng trong bài gửi duyệt. AI không âm thầm thay nguyên liệu hoặc trình bày thông tin chưa biết như dữ kiện đã xác nhận.
- Khi gửi duyệt, hệ thống kiểm tra cấu trúc và các trường bắt buộc đã được quy định; Administrator duyệt bài trước khi công khai. AI không tự gửi duyệt hoặc xuất bản. Đúng format không đồng nghĩa công thức đúng hoặc an toàn.
- Khi AI lỗi hoặc hết lượt, người dùng vẫn tiếp tục tự viết/chỉnh sửa bài. Quyền gọi AI soạn bài theo gói và cách tính lượt là quyết định riêng, không tự thay đổi hạn mức 5/15/50 đã chốt.

Luồng cấp cao: tác giả nhập thông tin → tự viết hoặc yêu cầu AI hỗ trợ → tác giả rà soát/chỉnh sửa → gửi duyệt và kiểm tra format → Admin duyệt → hiển thị thẻ món và trang chi tiết từ cùng bài công thức.

**Chưa chốt:** phạm vi AI được soạn/sửa từng mục; quyền theo gói và cách tính lượt AI soạn bài; cơ chế lưu nháp chi tiết (thủ công/tự động) và xử lý chỉnh sửa bài đã công khai.

### 3.7 Hướng dẫn nấu theo bước — đã chốt 12/09/2026

- Phần hướng dẫn nấu của Blog công thức là danh sách các bước có thứ tự; mỗi bước chứa nội dung hướng dẫn bằng văn bản.
- Tác giả có thể thêm, sửa, xóa và đổi vị trí các bước trong trình soạn bài. Trang chi tiết hiển thị theo thứ tự đã lưu.
- Phần giới thiệu vẫn được viết tự do trong mục riêng, không thay thế danh sách hướng dẫn nấu.
- Khi AI hỗ trợ tạo hướng dẫn, kết quả phải theo cùng cấu trúc các bước để tác giả rà soát và chỉnh sửa từng bước trước khi gửi duyệt.
- Quyền chỉnh sửa các bước không tự cho phép bỏ qua kiểm duyệt khi bài đã công khai; workflow chỉnh sửa bài đã công khai vẫn chờ chốt.

Khi gửi duyệt cần ít nhất một bước có nội dung theo mục 3.9. **Chưa chốt:** số bước tối đa, giới hạn độ dài mỗi bước và media riêng cho từng bước. Không mặc định bổ sung ảnh/video cho mỗi bước.

### 3.8 Tác giả và hồ sơ công khai — đã chốt 12/09/2026

- Đầu trang chi tiết Blog hiển thị tên công khai và avatar của tác giả, kèm liên kết tới hồ sơ công khai và các bài đã công khai của tác giả.
- Hệ thống gắn tác giả với tài khoản đăng bài, không dùng ô nhập tên tác giả tùy ý và không cho người đăng chọn tài khoản khác để đứng tên.
- Hồ sơ công khai không hiển thị email, thông tin đăng nhập hoặc dữ liệu hồ sơ ăn uống riêng tư. Các trường công khai bổ sung chưa được chốt.
- Administrator duyệt bài không trở thành tác giả. AI hỗ trợ viết không thay thế tác giả; người dùng vẫn rà soát và gửi bài.
- Gắn bài với tài khoản nhằm truy xuất người đứng tên nội dung, không chứng minh danh tính ngoài đời hoặc chuyên môn. Bản hiện tại chưa làm xác minh danh tính thật/bằng cấp và không cấp nhãn “đã xác minh danh tính” hay “chuyên gia”.
- Tham khảo giao diện đã quan sát trực tiếp trên Chrome: [Salads for Lunch — Creamy Tuna Macaroni Salad](https://www.salads4lunch.com/recipes/salad-recipes-for-lunch/classic-salads/creamy-tuna-macaroni-salad/) hiển thị ảnh/tên tác giả dẫn tới hồ sơ ở đầu bài và phần giới thiệu tác giả cuối bài. Đây là bằng chứng về cách hiển thị, không xác nhận quy trình kiểm chứng danh tính của website.

**Chưa chốt:** avatar mặc định, quy tắc đổi tên/avatar, xử lý hiển thị tác giả khi tài khoản bị khóa hoặc xóa và các trường giới thiệu bổ sung trên hồ sơ. Không tự thêm follow, nhắn tin hoặc xác minh chuyên gia.

### 3.9 Điều kiện lưu nháp và gửi duyệt Blog công thức — đã chốt 12/09/2026

| Nội dung | Điều kiện khi gửi duyệt |
| --- | --- |
| Tên món | Bắt buộc có nội dung. |
| Nguyên liệu | Có ít nhất một nguyên liệu; khai báo lượng cụ thể hoặc “vừa đủ” theo mục 3.5, không bắt buộc liên kết danh mục chuẩn. |
| Hướng dẫn | Có ít nhất một bước hướng dẫn có nội dung, giữ thứ tự đã lưu. |
| Khẩu phần | Bắt buộc khai báo. |
| Loại ăn chay | Bắt buộc khai báo; danh sách lựa chọn còn chờ chốt. |
| Thời gian chuẩn bị và thời gian nấu | Bắt buộc khai báo riêng; thời gian nấu được bằng 0 với món không cần nấu. |
| Tác giả | Hệ thống lấy từ tài khoản đăng bài, không yêu cầu nhập lại. |
| Giới thiệu, ảnh, video YouTube | Tùy chọn; khi không có ảnh, thẻ món dùng ảnh mặc định. Media được cung cấp vẫn phải tuân theo chính sách kiểm tra/kiểm duyệt hiện tại. |

- Người dùng được lưu nháp chưa đủ các thông tin bắt buộc và tiếp tục chỉnh sửa; chưa gửi nháp vào hàng đợi duyệt hoặc công khai.
- Chỉ kiểm tra tính đầy đủ theo bảng khi người dùng bấm “Gửi duyệt”. Nếu thiếu, hệ thống chỉ rõ mục cần bổ sung, không gửi duyệt và không làm mất nội dung đã nhập.
- Lưu nháp không bỏ qua kiểm tra quyền truy cập và an toàn dữ liệu. Đủ trường để gửi duyệt không đồng nghĩa bài được tự động phê duyệt.
- Bài tự viết và bài có AI hỗ trợ áp dụng cùng điều kiện này. Các điều kiện dành riêng cho công thức không tự áp dụng cho Blog không phải công thức.

**Chưa chốt:** giới hạn độ dài, giới hạn số lượng/khẩu phần/thời gian, danh sách đơn vị và loại ăn chay, cơ chế lưu nháp thủ công/tự động và sửa bài đã công khai. Luồng duyệt bài mới được chốt tại 3.10.

### 3.10 Duyệt Blog mới — đã chốt 12/09/2026

| Trạng thái | Ý nghĩa và thao tác |
| --- | --- |
| Draft — Nháp | Tác giả chỉnh sửa; bài chưa công khai. Khi đủ điều kiện, tác giả gửi sang Pending Review. |
| Pending Review — Chờ duyệt | Admin xem bài để duyệt hoặc từ chối. Tác giả muốn sửa phải rút bài về Draft trước. |
| Published — Đã công khai | Admin đã phê duyệt; bài được công khai. |
| Rejected — Bị từ chối | Admin phải ghi lý do; tác giả được chỉnh sửa và gửi lại khi đáp ứng điều kiện. |

- Nội dung chờ duyệt không được tác giả sửa trực tiếp. Nếu tác giả đã rút bài về nháp, Admin không được phê duyệt yêu cầu chờ duyệt cũ; hệ thống phải kiểm tra trạng thái hiện hành khi xử lý.
- Luồng này dành cho Blog mới, không tự quyết định workflow sửa bài đã công khai hoặc chính sách kiểm duyệt bình luận.

### 3.11 Báo cáo Blog và AI hỗ trợ kiểm duyệt — phân tách ưu tiên 12/09/2026

**Đã yêu cầu trong phạm vi cơ bản:** người dùng có thể báo cáo Blog có dấu hiệu có vấn đề để Administrator tiếp nhận, xem xét và xử lý. Có báo cáo không đồng nghĩa bài đã vi phạm.

**Quyền báo cáo đã chốt 12/09/2026:** chỉ tài khoản đã đăng nhập được gửi báo cáo Blog. Guest bấm “Báo cáo” được yêu cầu đăng nhập; hệ thống chưa tạo báo cáo khi chưa xác thực. Backend phải kiểm tra đăng nhập khi tiếp nhận, không chỉ giới hạn bằng giao diện. Đăng nhập giúp truy xuất báo cáo nhưng không tự thay thế các quy tắc chống lạm dụng.

**Nâng cao nếu còn thời gian:** AI rà soát Blog, phát hiện dấu hiệu bất thường và tạo cờ kèm thông tin hỗ trợ Admin xem xét. AI không tự phê duyệt, từ chối, ẩn hoặc xóa bài. Quy trình duyệt và tiếp nhận báo cáo của người dùng phải hoạt động khi chưa triển khai AI hoặc AI bị lỗi.

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

### 3.12 Xử lý báo cáo và ẩn Blog — đã chốt 12/09/2026

| Trạng thái báo cáo | Ý nghĩa |
| --- | --- |
| Chờ xử lý | Báo cáo mới được gửi, chờ Administrator tiếp nhận. |
| Đang xem xét | Admin đã tiếp nhận và đang kiểm tra nội dung. |
| Đã giải quyết | Admin đã ghi kết luận và lý do xử lý: có vi phạm hoặc không đủ căn cứ. |

- Trạng thái báo cáo độc lập với trạng thái Blog. Gửi báo cáo hoặc có nhiều báo cáo không tự động ẩn bài.
- Nếu xác định có vấn đề, Admin có thể ẩn Blog (`Hidden`) và yêu cầu tác giả sửa, gửi duyệt lại. Ẩn không phải xóa bài; bài không còn được xem công khai nhưng vẫn phục vụ chỉnh sửa và kiểm duyệt.
- Nếu không đủ căn cứ, Admin đóng báo cáo với kết luận và lý do, không ẩn bài vì báo cáo đó. Nếu bài đang công khai thì tiếp tục công khai; việc đóng một báo cáo không tự khôi phục bài bị ẩn bởi quyết định khác.
- Bài bị ẩn không tự công khai khi tác giả sửa hoặc gửi lại. Chỉ được công khai sau khi Admin duyệt lại; nếu chưa được duyệt, nội dung vẫn không công khai.
- “Đã giải quyết” nghĩa là Admin đã kết luận/xử lý báo cáo, không đồng nghĩa bài đã được tác giả sửa xong hoặc đã được công khai lại.

**Chưa chốt:** chi tiết chuyển trạng thái khi tác giả sửa bài bị ẩn, chỉnh sửa bài đang công khai, kênh và thời điểm thông báo kết quả, chi tiết xử lý nhiều báo cáo được nhóm theo Blog, ảnh hưởng của bài bị ẩn đến thực đơn đã lưu và audit trail chi tiết. Không tự bổ sung xóa vĩnh viễn hoặc khóa tài khoản theo báo cáo.

### 3.13 Quyền xem báo cáo và bảo vệ người gửi — đã chốt 12/09/2026

| Người xem | Phạm vi được xem |
| --- | --- |
| Administrator | Báo cáo và tài khoản người gửi để xem xét, xử lý và kiểm tra lạm dụng. |
| Tác giả Blog bị báo cáo | Lý do, nội dung cần sửa và quyết định xử lý liên quan đến bài của mình; không thấy danh tính người báo cáo. |
| Người gửi báo cáo | Trạng thái và kết quả xử lý báo cáo của chính mình; không được xem báo cáo của tài khoản khác. |
| Công chúng | Báo cáo không được hiển thị công khai. |

- Hệ thống kiểm tra quyền xem ở backend theo vai trò và quan hệ với báo cáo/Blog, không chỉ ẩn nút hoặc trường trên giao diện.
- Nội dung gửi cho tác giả không kèm thông tin định danh người báo cáo. Không tự chuyển nguyên văn mô tả hoặc ghi chú nội bộ có thể làm lộ người gửi; thông tin phản hồi phải tuân theo phạm vi trong bảng.
- Quyền xem trạng thái/kết quả không đồng nghĩa đã chốt gửi email, push notification hoặc công khai ghi chú nội bộ của Admin.

**Chưa chốt:** màn hình tra cứu chi tiết, nội dung thông báo cụ thể, kênh và thời điểm thông báo, thời hạn lưu báo cáo và ghi chú nội bộ.

### 3.14 Báo cáo trùng và bổ sung thông tin — đã chốt 12/09/2026

- Mỗi tài khoản chỉ có một báo cáo chưa giải quyết trên cùng Blog (Chờ xử lý hoặc Đang xem xét).
- Khi báo cáo còn mở, người gửi được bổ sung mô tả vào báo cáo đó, không tạo báo cáo trùng.
- Sau khi báo cáo đã giải quyết, người dùng được báo cáo lại nếu phát hiện vấn đề mới và phải mô tả điểm mới, kể cả khi lý do không phải “Khác”.
- Nhiều tài khoản vẫn có thể báo cáo cùng Blog. Admin xem các báo cáo được nhóm theo Blog; nhóm hiển thị không làm mất người gửi, trạng thái và kết quả của từng báo cáo.
- Backend kiểm tra quy tắc một báo cáo đang mở, kể cả khi nhận nhiều yêu cầu đồng thời.
- Chưa chốt giới hạn tần suất, độ dài mô tả bổ sung, lịch sử bổ sung và quy tắc xử lý hàng loạt.

## 4. Actors và quyền cấp cao

Bảng dưới là baseline cấp cao. Bản phân rã chi tiết và các quyền mới đang đề xuất nằm tại [Actors & Onboarding](actors-and-onboarding-draft.md); các gói Free/Plus/Pro là quyền sử dụng, không mặc định tạo role mới.

| Actor | Trạng thái | Mục tiêu chính | Không được làm |
| --- | --- | --- | --- |
| Guest | Chưa đăng nhập | Xem nội dung công khai, tìm kiếm và dùng Guest Free AI | Đăng/bình luận, gửi báo cáo Blog, xem lịch sử, lập thực đơn cá nhân hóa. |
| Authorized User | Đã đăng nhập | Tương tác cộng đồng, gửi nội dung, báo cáo Blog có vấn đề, quản lý thực đơn, dùng AI theo gói | Tự duyệt nội dung hoặc quản trị người khác. |
| Administrator | Quản trị | Duyệt/ẩn/xóa nội dung, quản lý Blog Post/danh mục/người dùng, xử lý nội dung AI gắn cờ | Không được để AI tự thay mình ra quyết định moderation. |
| Gemini AI | Dịch vụ ngoài hệ thống | Tạo phản hồi AI khi backend cho phép | Không có quyền hệ thống, không truy cập database trực tiếp, không tự công khai nội dung. |

## 5. Phân rã dự án theo module

| Mã module | Module | Giá trị | Phụ thuộc chính | Trạng thái phân rã |
| --- | --- | --- | --- | --- |
| M01 | Public Content Discovery | Khách tìm/xem Blog Post, gồm bài công thức và bài có video YouTube được nhúng | Nội dung đã công khai | Cần viết user stories. |
| M02 | Identity & Access | Phân biệt Guest/User/Admin và bảo vệ thao tác | Tài khoản, role, session | Cần chốt cơ chế đăng nhập. |
| M03 | Community Contribution & Moderation | Người dùng gửi Blog Post (có thể là bài công thức, ảnh Azure và link YouTube)/bình luận; Admin kiểm duyệt | M02, Azure Blob, quy trình trạng thái nội dung | Cần phân rã workflow duyệt. |
| M04 | Blog Recipe Classification & Discovery | Phân loại, tìm và lọc các Blog Post có nội dung công thức chay | M01, M03 | Đã chốt năm nhóm dữ liệu tại 3.3; cấu trúc và validation còn cần phân rã. |
| M05 | Weekly Meal Planning | Tạo thực đơn tuần theo mục tiêu cá nhân | M02, M04 | Cần chốt dữ liệu hồ sơ và bữa ăn. |
| M06 | Gemini AI Access & Usage | Guest/Free/Plus/Pro gọi AI và đo usage thật | M02 một phần, Gemini backend | Đã có hướng; cần user stories. |
| M07 | AI-assisted Moderation | AI rà soát/gắn cờ Blog, Admin quyết định | M03, M06 | Nâng cao nếu còn thời gian; thời điểm rà soát và tiêu chí cờ chưa chốt. |
| M08 | Subscription Administration | Hiển thị/đổi gói và quyền AI | M02, M06 | Bản đầu chỉ mô phỏng, không payment thật. |
| M09 | Administration | Quản lý user, Blog Post/danh mục, content và moderation queue | M02–M07 | Cần phân rã quyền và audit trail. |

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
| FR-03 | Hệ thống cho người dùng đăng ký/đăng nhập và duy trì role phù hợp. | M02 | Đã chốt ở mức cao; chi tiết auth chưa chốt |
| FR-04 | Hệ thống cho Authorized User gửi Blog Post (bao gồm bài chia sẻ công thức), upload ảnh và gắn link YouTube theo chính sách moderation. | M03 | Đã chốt ở mức cao |
| FR-05 | Hệ thống chỉ công khai nội dung người dùng sau khi Administrator duyệt. | M03 | Đã chốt |
| FR-06 | Hệ thống cho Administrator quản lý thành viên, Blog Post (gồm ảnh/link YouTube), bình luận và danh mục. | M09 | Đã chốt ở mức cao |
| FR-07 | Hệ thống cho Administrator tạo/cập nhật Blog Post và phân loại bài có nội dung công thức chay. | M04 | Đã chốt ở mức cao |
| FR-08 | Hệ thống cho người dùng tìm/lọc các Blog Post công thức đã công khai. | M04 | Đã chốt |
| FR-09 | Hệ thống cho Authorized User tạo và chỉnh thực đơn tuần. | M05 | Đã chốt ở mức cao |
| FR-10 | Hệ thống áp dụng hạn mức Gemini AI theo Guest Free/Free/Plus/Pro, kiểm tra trước khi gọi provider. | M06 | Đã chốt |
| FR-11 | Hệ thống lưu số lượt AI thành công và token usage metadata do Gemini trả về để đo usage thật. | M06 | Đã chốt |
| FR-12 | Hệ thống cho AI rà soát và gắn cờ Blog có dấu hiệu có vấn đề để Administrator xem xét; AI không tự quyết định moderation. | M07 | Nâng cao nếu còn thời gian |
| FR-13 | Hệ thống cho người dùng xem/đổi gói AI mô phỏng; thanh toán thật không thuộc bản đầu. | M08 | Đã chốt ở mức demo |
| FR-14 | Hệ thống lưu ảnh Blog Post trên Azure Blob Storage và lưu metadata/tham chiếu trong SQL Server. | M03 | Đã chốt |
| FR-15 | Hệ thống nhúng YouTube player trong Blog Post từ link hợp lệ; hệ thống không upload hoặc sao chép file video YouTube. | M01, M03 | Đã chốt |
| FR-16 | Blog loại công thức có các mục dữ liệu riêng: nguyên liệu, cách làm, khẩu phần, thời gian nấu và loại ăn chay, dùng chung luồng đăng và duyệt Blog. | M03, M04 | Đã chốt nhóm dữ liệu; validation chưa chốt |
| FR-17 | Hệ thống trình bày Blog công thức đã duyệt dưới dạng thẻ món trong Khám phá và nối trang chi tiết với luồng thêm món vào thực đơn. | M04, M05 | Đã chốt ở mức cao; tương tác chi tiết chưa chốt |
| FR-18 | Hệ thống cho Administrator quản lý danh mục nguyên liệu dùng chung cho Blog công thức. | M04, M09 | Đã chốt ở mức cao; vòng đời nguyên liệu chưa chốt |
| FR-19 | Người đăng Blog công thức chọn nguyên liệu có sẵn hoặc nhập tên mới khi không tìm thấy, rồi khai báo định lượng cụ thể hoặc “vừa đủ”; mỗi dòng giữ tên hiển thị và liên kết danh mục chuẩn tùy chọn. | M03, M04 | Đã chốt nhập linh hoạt; validation chi tiết chưa chốt |
| FR-20 | Hệ thống cung cấp trang chi tiết công thức nội bộ và thẻ món từ cùng một Blog công thức; thực đơn tham chiếu bài này, không tạo bài nguồn riêng. | M01, M04, M05 | Đã chốt ở mức cao |
| FR-21 | Tác giả tự viết hoặc yêu cầu Gemini hỗ trợ soạn bản nháp Blog công thức theo mẫu; tác giả rà soát, chỉnh sửa và xác nhận trước khi gửi Admin duyệt. | M03, M06 | Đã chốt ở mức cao; quyền gói, tính lượt và phạm vi soạn chi tiết chưa chốt |
| FR-22 | Trình soạn Blog công thức cho tác giả thêm, sửa, xóa và đổi vị trí các bước nấu; trang chi tiết hiển thị đúng thứ tự đã lưu. Hướng dẫn do AI hỗ trợ cũng dùng cấu trúc bước này. | M01, M03, M06 | Đã chốt cấu trúc và thao tác; validation chi tiết chưa chốt |
| FR-23 | Trang chi tiết Blog hiển thị tên công khai/avatar của tác giả gắn với tài khoản đăng bài và liên kết hồ sơ công khai có các bài đã công khai của tác giả. | M01, M02, M03 | Đã chốt ở mức cao; vòng đời hồ sơ và tài khoản chưa chốt |
| FR-24 | Tác giả được lưu nháp Blog công thức chưa đầy đủ; hệ thống kiểm tra các mục bắt buộc tại 3.9 khi gửi duyệt và chỉ rõ mục thiếu, giữ nội dung đã nhập nếu chưa gửi được. | M03 | Đã chốt điều kiện đầy đủ; validation và cơ chế lưu chi tiết chưa chốt |
| FR-25 | Blog mới đi qua Draft, Pending Review, Published hoặc Rejected theo mục 3.10; tác giả rút bài trước khi sửa nội dung chờ duyệt, được sửa/gửi lại bài bị từ chối. | M03, M09 | Đã chốt luồng bài mới |
| FR-26 | Tài khoản đã đăng nhập được gửi báo cáo Blog có vấn đề; Guest được yêu cầu đăng nhập khi bấm “Báo cáo”. Administrator tiếp nhận, xem xét và xử lý báo cáo theo mục 3.12. | M02, M03, M09 | Đã chốt quyền đăng nhập và luồng xử lý cấp cao |
| FR-27 | Biểu mẫu báo cáo Blog cho chọn một trong sáu nhóm lý do tại 3.11 và nhập mô tả bổ sung; mô tả bắt buộc khi chọn “Khác”, tùy chọn với các lý do còn lại. | M03 | Đã chốt cấu trúc; giới hạn độ dài chưa chốt |
| FR-28 | Admin xử lý báo cáo qua Chờ xử lý, Đang xem xét, Đã giải quyết; ghi kết luận và lý do. Admin có thể ẩn Blog có vấn đề và yêu cầu tác giả sửa/gửi duyệt lại. | M03, M09 | Đã chốt cấp cao; chi tiết chỉnh sửa và thông báo chưa chốt |
| FR-29 | Hệ thống phân quyền xem báo cáo theo mục 3.13: Admin thấy tài khoản người gửi; tác giả thấy thông tin xử lý nhưng không thấy danh tính người báo cáo; người gửi xem trạng thái/kết quả báo cáo của mình; báo cáo không công khai. | M02, M03, M09 | Đã chốt phạm vi hiển thị; kênh thông báo chưa chốt |
| FR-30 | Người gửi bổ sung mô tả cho báo cáo đang mở; hệ thống ngăn cùng tài khoản tạo báo cáo mở trùng trên một Blog. Admin xem báo cáo nhóm theo Blog; báo cáo lại sau khi giải quyết cần mô tả vấn đề mới. | M03, M09 | Đã chốt quy tắc cấp cao |

## 8. Business Rules đã chốt

| ID | Quy tắc |
| --- | --- |
| BR-01 | Guest và tài khoản Free có tối đa 5 lượt text AI mỗi ngày theo giới hạn của app. |
| BR-02 | Plus có tối đa 15 lượt text AI/ngày; Pro có tối đa 50 lượt text AI/ngày. |
| BR-03 | Lượt AI chỉ tăng sau khi backend nhận phản hồi Gemini hợp lệ và lưu kết quả thành công. |
| BR-04 | Lỗi provider/timeout trước phản hồi hợp lệ không được trừ lượt, nhưng phải ghi event lỗi để đối soát. |
| BR-05 | Guest không có lịch sử AI, hồ sơ hoặc thực đơn cá nhân hóa. |
| BR-06 | Gemini API key chỉ lưu ở backend/biến môi trường; frontend không được biết API key. |
| BR-07 | Nội dung do user gửi chỉ public sau khi Administrator phê duyệt. |
| BR-08 | AI chỉ hỗ trợ gắn cờ; Administrator quyết định duyệt/ẩn/xóa nội dung. |
| BR-09 | Câu trả lời AI phải nêu giới hạn hỗ trợ và không được trình bày như chẩn đoán/điều trị. |
| BR-10 | Phase 1 không nhận upload file video; Blog Post chỉ lưu link hoặc video ID của YouTube. |
| BR-11 | Blog Post có media vẫn phải qua Administrator phê duyệt trước khi công khai; Admin xem xét cả nội dung viết, ảnh và video được nhúng. |
| BR-12 | Liên kết nguyên liệu chuẩn là tùy chọn. Tên mới trên công thức không tự trở thành mục chuẩn; việc thiếu liên kết không tự chặn hoàn thiện, gửi duyệt hoặc công khai Blog theo workflow chung. |
| BR-13 | Nguyên liệu chưa nhận diện không mặc định thỏa mãn ràng buộc ăn uống; hệ thống không khẳng định món phù hợp với ràng buộc loại trừ khi chưa kiểm tra được thành phần. |
| BR-14 | Dòng nguyên liệu dùng “vừa đủ” không yêu cầu số lượng/đơn vị, không lưu số giả thay lượng chưa xác định và không tự quy đổi sang gram hoặc suy ra dinh dưỡng chính xác. |
| BR-15 | Bài tự viết và bài có AI hỗ trợ đều tuân theo cùng format và workflow kiểm duyệt Blog; AI chỉ hỗ trợ bản nháp, không tự gửi duyệt hoặc xuất bản. |
| BR-16 | AI lỗi hoặc hết lượt không được chặn tác giả tiếp tục tự viết/chỉnh sửa và gửi bài đáp ứng điều kiện kiểm duyệt. |
| BR-17 | Tác giả được gắn với tài khoản đăng bài; người đăng không được chọn tài khoản khác đứng tên. Việc Admin duyệt hoặc AI hỗ trợ không thay đổi tác giả. |
| BR-18 | Hồ sơ tác giả công khai không tiết lộ email, thông tin đăng nhập hoặc hồ sơ ăn uống riêng tư; gắn tài khoản không được trình bày như xác minh danh tính thật hay chuyên môn. |
| BR-19 | Blog công thức chỉ được gửi duyệt khi đủ các mục bắt buộc tại 3.9; nháp có thể chưa đầy đủ nhưng không được công khai hoặc đưa vào hàng đợi duyệt. |
| BR-20 | Giới thiệu và media không bắt buộc khi gửi Blog công thức duyệt; thẻ món không có ảnh dùng ảnh mặc định. Thời gian nấu được bằng 0. |
| BR-21 | Tác giả không sửa trực tiếp Blog đang Pending Review; phải rút về Draft. Admin chỉ xử lý yêu cầu còn ở trạng thái chờ duyệt hiện hành. |
| BR-22 | Admin phải cung cấp lý do khi từ chối Blog; tác giả được sửa và gửi lại khi đủ điều kiện. |
| BR-23 | Báo cáo của người dùng và cờ AI là tín hiệu để xem xét, không phải kết luận vi phạm; AI không tự quyết định xử lý Blog. Chức năng duyệt/báo cáo cơ bản không phụ thuộc AI. |
| BR-24 | Hệ thống chỉ tiếp nhận báo cáo Blog từ tài khoản đã đăng nhập; Guest phải đăng nhập trước. Kiểm tra xác thực được thực hiện ở backend. |
| BR-25 | Mỗi báo cáo phải có một lý do hợp lệ trong sáu nhóm đã chốt. Nếu chọn “Khác”, mô tả không được rỗng hoặc chỉ chứa khoảng trắng; không tạo báo cáo khi chưa thỏa điều kiện. |
| BR-26 | Chỉ Admin quyết định ẩn Blog trong quy trình xử lý báo cáo; số lượng báo cáo không tự làm bài bị ẩn. Báo cáo đã giải quyết phải có kết luận và lý do. |
| BR-27 | Blog bị ẩn phải được Admin duyệt lại trước khi công khai; sửa/gửi lại bài hoặc đóng báo cáo không tự khôi phục trạng thái công khai. |
| BR-28 | Báo cáo không công khai. Backend phải giới hạn quyền xem theo 3.13 và không cung cấp danh tính người báo cáo cho tác giả Blog bị báo cáo. |
| BR-29 | Mỗi tài khoản chỉ có một báo cáo chưa giải quyết trên cùng Blog; thông tin mới được bổ sung vào báo cáo đang mở. Báo cáo lại sau khi giải quyết phải mô tả điểm mới. |

## 9. Non-functional Requirements cần cụ thể hóa

| Nhóm | NFR cần viết trong SRS | Điều cần chốt/đo |
| --- | --- | --- |
| Security | Role-based access, backend-only Gemini key, input validation, rate limit | Cơ chế login, token/session và giới hạn upload ảnh. |
| Privacy | Giới hạn log prompt/dữ liệu hồ sơ; thông báo dữ liệu gửi Gemini | Thời hạn lưu lịch sử AI và quyền xóa. |
| Reliability | Xử lý timeout/429, không trừ lượt oan, idempotency cho request AI | Thời gian timeout/retry tối đa. |
| Performance | Tìm kiếm, danh sách, moderation queue và phản hồi AI có trạng thái loading/error | Mục tiêu thời gian phản hồi cần nhóm xác định. |
| Usability | Hiển thị gói, lượt còn lại, reset và lý do bị chặn | Thiết kế mobile/web chưa chốt. |
| Auditability | Lưu moderation decision và AI usage event có requestId | Thời hạn lưu log và quyền xem log. |

## 10. Điểm phải chốt trước ERD chi tiết

1. **Đã chốt:** người dùng chia sẻ công thức bằng Blog Post; không có chức năng “chia sẻ công thức” độc lập. Ảnh lưu trên Azure Blob; video Phase 1 chỉ là link YouTube được nhúng trong Blog Post, không upload file video.
2. Luồng bài mới đã chốt Draft/Pending Review/Published/Rejected; xử lý báo cáo và ẩn bài đã chốt cấp cao tại 3.12. Cần chốt sửa bài đã công khai, chi tiết sửa/gửi lại bài bị ẩn và moderation bình luận.
3. Đăng nhập dùng email/password, Google login hay cả hai?
4. “Thực đơn tuần” gồm những bữa nào và dữ liệu BMI/mục tiêu nào được phép thu thập?
5. Plus và Pro có được làm tính năng khác biệt như bảng ở `01-phan-ra-goi-ai.md` không, hay chỉ khác số lượt?
6. Bản nộp là web application hay cần mobile application?

## 11. Kế hoạch phân rã tiếp theo

Làm theo từng lát dọc có thể kiểm thử, thay vì hoàn thành hết một module rồi mới nối:

1. **Slice A — Guest Free AI:** `FR-01`, `FR-02`, `FR-10`, `FR-11`, `BR-01`, `BR-03`–`BR-06`.
2. **Slice B — User Free AI:** đăng ký/đăng nhập, 5 lượt/ngày theo user, lịch sử AI cơ bản.
3. **Slice C — Community moderation:** gửi bài → Pending Review → Admin duyệt/từ chối → public content.
4. **Slice D — Recipe Blog & meal plan:** Blog Post dạng công thức → tìm/lọc → thêm món vào thực đơn tuần.
5. **Slice E — Plus/Pro demo:** đổi gói mô phỏng → quyền AI khác biệt → kiểm tra hạn mức.
6. **Slice F — Admin & audit:** hàng đợi moderation, quản lý Blog Post/danh mục, audit usage/decision.

Thứ tự thảo luận hiện tại thay thế kế hoạch AI-first trước đây: chốt actor/quyền → chốt onboarding → chốt Blog công thức và dữ liệu món → chốt luồng tìm/gợi ý/lưu/thay món. Mỗi phần chỉ cập nhật thành yêu cầu đã chốt sau xác nhận của người dùng. Quyền cá nhân hóa theo gói trước đây cần được rà soát để không khóa luồng lõi ngoài ý muốn; chưa tự thay đổi hạn mức 5/15/50.
