> **Document:** Frontend Workspace Guide (Mâm Xanh)  
> **File:** `app/mamxanh-frontend/README.md`  
> **Version:** v1.47.0
> **Created:** 2026-09-18  
> **Last Updated:** 2026-10-25
> **Status:** Active  

# Mâm Xanh Frontend

### Sao chép và xuất TXT đi chợ — FR-55 / Issue #55

- Toolbar `/di-cho` sao chép hoặc tải toàn bộ danh sách đang chọn, kể cả những mục bị ẩn bởi bộ lọc; giữ nhóm, số lượng, đơn vị và ký hiệu `[ ]`/`[x]` đã lưu. Không đưa thông tin tài khoản hoặc provenance công thức vào nội dung xuất.
- Tệp UTF-8 có BOM dùng tên `Danh_sach_mua_sam_YYYYMMDD.txt` theo ngày xuất. Nội dung có riêng ngày xuất và ngày thực đơn lấy từ nhãn danh sách: một ngày, khoảng ngày hoặc các ngày đã chọn. Danh sách cá nhân từ công thức được ghi rõ không gắn ngày thực đơn; nhãn cũ thiếu ngày không tự suy diễn từ thời gian tạo.
- Danh sách trống hoặc đang lưu vô hiệu hóa thao tác xuất. Khi clipboard không được hỗ trợ/bị từ chối, hộp thoại cho phép chọn và sao chép thủ công; lỗi tải TXT cho phép thử lại. Thao tác xuất không sửa checklist hoặc gọi AI.
- Phụ thuộc nền tảng FR-53 / PR #171; chỉ bàn giao phần export của #55, chưa xác nhận nghiệm thu trên `main`.

### Checklist đi chợ — FR-53 / Issue #53

- `/di-cho` đọc và lưu checklist thật theo tài khoản; Guest cần đăng nhập, Administrator không có thao tác cá nhân. Member có thể đổi tên hoặc xóa mọi danh sách của mình; xóa danh sách yêu cầu xác nhận, xóa checklist cùng nguồn đóng góp nhưng giữ Lịch ăn và công thức nguồn. Thêm mặt hàng thủ công, sửa lượng/đơn vị, tick/bỏ tick, xóa từng mục hoặc xóa mục đã mua đều dùng API; lỗi giữ dữ liệu để thử lại.
- Member có thể tạo danh sách rỗng bằng tên tự chọn ngay tại `/di-cho`, không cần Lịch ăn hoặc công thức; checklist mở sẵn để thêm mặt hàng thủ công. Loại `MANUAL` được phân biệt với danh sách Meal Plan và danh sách công thức `PERSONAL`.
- Từ Lịch ăn có thể tạo theo cả tuần hoặc các ngày chọn; nhãn danh sách thể hiện ngày thực đơn. Từ menu dấu ba chấm trên từng thẻ ở Khám phá/Đã lưu hoặc nút trên trang chi tiết công thức, Member chọn một danh sách của mình (kể cả danh sách từ Lịch ăn) hoặc tạo danh sách có tên mới ngay trong hộp thoại; có thể chọn/bỏ chọn từng nguyên liệu, thêm mặt hàng thủ công và chỉnh khẩu phần 1–50. Thêm thành công ngay trên trang chi tiết giữ Member ở lại bài viết và cho liên kết mở danh sách; từ menu thẻ, hộp thoại vẫn mở trang danh sách sau khi thêm. Guest được yêu cầu đăng nhập, Admin không có thao tác.
- Nguồn công thức giữ lượng/đơn vị lúc thêm, hiển thị nội dung hiện hành hoặc unavailable khi bài không còn công khai. Thêm lại công thức không cộng trùng. Hành vi theo AC-53.6 và thiết kế nguồn đã chốt trong #90.
- Dòng nguyên liệu trong checklist dùng `imageUrl` từ danh mục nguyên liệu để hiển thị ảnh minh họa; nguyên liệu thủ công, chưa có ảnh hoặc ảnh tải lỗi hiển thị trạng thái “Chưa có ảnh”.
- Bản dựng FR-53 không phụ thuộc component export chưa bàn giao của FR-55; clipboard/TXT sẽ được nối ở PR riêng. Backend FR-53 phụ thuộc engine FR-54 / PR #150 chưa merge.

### Danh bạ Chuyên gia và chọn nguyên liệu — FR-61 / FR-62

- Menu Khám phá mở các route Công thức, Danh mục nguyên liệu và Chuyên gia trên desktop/mobile. Cộng đồng vẫn được mở từ nút “Xem cộng đồng” trên Trang chủ; route và deep links hiện có được giữ nguyên.
- `/chuyen-gia` đọc danh sách Expert đang hoạt động từ `GET /api/v1/experts`, hỗ trợ tìm/phân trang, mở hồ sơ hiện có và follow/unfollow qua API FR-59. Guest được chuyển tới đăng nhập; Admin chỉ duyệt.
- `/danh-muc-nguyen-lieu` tải nguyên liệu đang hoạt động từ `GET /api/v1/ingredients`, hiển thị ảnh/nhãn thiếu ảnh, cho xem chi tiết tại `/nguyen-lieu/:ingredientId` qua `GET /api/v1/ingredients/{ingredientId}` và giữ luồng chọn nhiều để mở Khám phá. Chi tiết hiện giá trị dinh dưỡng có sẵn trên 100 g; giá trị `null` hiện là chưa có dữ liệu.
- Chi tiết phân biệt nguyên liệu không tồn tại (`404`, có đường về danh mục) với lỗi kết nối/dịch vụ (có Thử lại), có trạng thái thiếu toàn bộ dinh dưỡng và giữ tối đa bốn chữ số thập phân cho B12. Ảnh trống hoặc tải lỗi hiện nhãn chưa có ảnh. Các kiểm tra UI dùng API intercept nằm trong `ingredient-directory.spec.ts` và `ingredient-detail.spec.ts`; chúng không xác nhận SQL Server runtime.
- Trang chi tiết có lưới bài công thức chứa đúng nguyên liệu từ API discovery: nhãn lọc loại ăn chay chuẩn, sắp xếp mới nhất/được yêu thích, Xem thêm và mở từng bài. Tên/ảnh nguyên liệu trong danh mục và tên nguyên liệu ở chi tiết công thức đều mở trang nguyên liệu. Nhãn là bộ lọc loại công thức, chưa phải phân loại độ phù hợp chế độ ăn riêng của nguyên liệu.
- Bốn Expert demo trong profile `local` dùng ảnh chân dung tổng hợp được host trong `public/avatars/experts/`; avatar người dùng tải lên được giữ nguyên.
- Danh mục demo có 209 ảnh nguyên liệu AI riêng trong `public/ingredients/`, được ánh xạ qua `INGREDIENT.image_url` khi URL còn trống. Asset WebP dùng chung cho danh mục, chi tiết nguyên liệu và danh sách nguyên liệu trong bài công thức; ảnh Admin tải lên được ưu tiên giữ nguyên. Bộ 54 món demo bổ sung gia vị theo món và định lượng gram cho hai phần ăn.
- Màn hình dùng dữ liệu Backend, có trạng thái loading/error/empty; không dùng mock directory hay count tĩnh.
- Hồ sơ công khai `/thanh-vien/:userId` dùng chung `PublicRecipeCard` với trang Công thức: ảnh ngang 4:3, tác giả, tỷ lệ thích, số nguyên liệu, thời gian nấu, khẩu phần, loại ăn chay/thể loại, lượt xem và thao tác lưu/thêm lịch ăn. Lưới thích ứng từ một đến bốn cột và giữ phân trang. Phần đầu hồ sơ hiển thị tổng lượt xem/lượt thích của tất cả bài công khai qua `GET /api/v1/members/{userId}/recipe-statistics`, không chỉ trang đang xem.

Thư mục mã nguồn giao diện ứng dụng Mâm Xanh được xây dựng bằng **React 19**, **TypeScript**, **Vite 8**, **Tailwind CSS v4** và xuất phát từ thiết kế Figma Make.

Dự án đã được cấu hình tối ưu để mở, chỉnh sửa, chạy và gỡ lỗi trực tiếp trên **IntelliJ IDEA** (hoặc WebStorm/VS Code).

### Ảnh công thức — FR-14 / fix #158

- Chỉ tính ảnh đã nhận URL HTTPS từ upload thành công vào số ảnh đã tải. Khi lỗi, ảnh xem trước có nhãn **Chưa tải lên**, thông báo lỗi và hướng dẫn xóa/chọn lại. Form chặn xuất bản khi còn ảnh lỗi hoặc đang upload.
- Kiểm thử UI có mock: `npx playwright test tests/e2e/recipe-media.spec.ts`. Kiểm thử Azure thật là opt-in: trên stack root Compose đã cấu hình Azure, đặt `MAMXANH_E2E_LIVE_AZURE=1` và `MAMXANX_E2E_COMPOSE_PROJECT` đúng project rồi chạy `npm run test:e2e:integration -- tests/integration/recipe-media-real-stack.spec.ts --workers=1`. Test dùng tài khoản/nguyên liệu fixture, tắt trace để không lưu phiên đăng nhập; xóa ảnh Azure và công thức test trong cleanup.

### Thẻ công thức và thao tác lịch ăn — FR-17 / Issue #23

- Thẻ công thức công khai hiển thị ảnh bìa hoặc ảnh mặc định theo loại ăn chay, thông tin tác giả, tỷ lệ Like, thời gian và lượt xem. Nút ba chấm mở thao tác thêm công thức vào lịch; nút lưu nhanh vẫn hoạt động độc lập.
- Thành viên có thể chọn nhiều tổ hợp ngày/bữa trong cùng tuần hoặc nhiều tuần. Chuyển tuần không xóa lựa chọn; khi xác nhận, mỗi tổ hợp được gửi đến API của đúng tuần. Nếu một số lượt thêm thất bại, lựa chọn lỗi còn được giữ để thử lại.
- Guest được dẫn tới đăng nhập trước khi thêm lịch; Administrator không có thao tác cá nhân trên thẻ.
- Kiểm thử Playwright mô phỏng API để bao phủ thẻ, ảnh mặc định, lưu nhanh, lựa chọn qua nhiều tuần và lỗi từng lượt thêm. Kiểm thử này không xác nhận tích hợp Backend/SQL Server thật.

### Bình chọn công thức — FR-57 / Issue #56

- Trang chi tiết API `/cong-thuc/id/:recipeId` gọi API phản hồi cá nhân để tải lựa chọn hiện tại; dùng PUT để thêm/đổi Like hoặc Dislike và DELETE khi bấm lại nút đã chọn. Số lượt, thanh tỷ lệ và phần trăm được cập nhật từ response thành công của Backend. Lỗi ghi giữ nguyên dữ liệu, quá trình tải/ghi khóa thao tác để tránh gửi trùng.
- Guest xem thống kê công khai và được yêu cầu đăng nhập khi bình chọn; tác giả và Admin không được thao tác. Backend vẫn quyết định quyền cuối cùng. Lỗi đọc lựa chọn có nút tải lại; bài không còn công khai hoặc Backend từ chối quyền sẽ chặn thao tác tiếp.
- Thẻ công thức API trên Trang chủ và Khám phá không phủ nhãn trạng thái hoặc tỷ lệ Like lên ảnh. Trang chi tiết vẫn giữ thanh tỷ lệ có nhãn truy cập và số lượt Like/Dislike; luồng bình chọn không đổi.
- Kiểm thử Playwright của tính năng sử dụng API mô phỏng để xác minh hành vi trình duyệt; chưa thay thế bằng chứng tích hợp Frontend–Backend–SQL Server trong Compose. Các route công thức demo dùng slug vẫn là luồng minh họa riêng.
- Xác minh giai đoạn 4 (11/10/2026): TypeScript và production build pass; 27 kiểm thử trình duyệt/hồi quy pass và lượt xác nhận cuối 14 kiểm thử pass (9 bình chọn, 5 khám phá công khai). Đã mở luồng Guest với Backend/SQL local thật, kiểm tra yêu cầu đăng nhập không ghi bình chọn và xem ảnh desktop/mobile. Chưa xác minh thao tác ghi của Member qua trình duyệt với Backend thật, Compose hoặc toàn bộ coverage gate.
- Xác minh giai đoạn 5 (11/10/2026): 13 kiểm tra trình duyệt–HTTP–SQL local thật pass, gồm Member thêm/đổi/hủy, lưu lựa chọn qua tải lại, huy hiệu 91% và 0%, Guest không ghi dữ liệu, tác giả/Admin bị chặn, 401 và hết phiên client, bài bị ẩn/xóa. Các context dùng token do Backend cấp thật; fixture riêng đã được dọn. Bằng chứng này bổ sung cho UI mock, không thay cổng tích hợp Compose khi Docker chưa khả dụng.
- Lượt cuối `npm run test:e2e:coverage` đạt 104/104 kiểm thử và strict gate >80% cho cả bốn metric: Lines 92,35%, Statements 89,13%, Functions 84,71%, Branches 83,91%. Lượt đầu có một kiểm thử lỗi do fixture dùng nhầm shape API quản lý cho API chi tiết công khai; đã sửa fixture và chạy lại toàn bộ. Tại thời điểm lượt này, Backend/Compose còn bị chặn bởi Docker.
- Xác minh tiếp giai đoạn 5 (11/10/2026): canonical root Compose project `mamxanh-fr57` build/chạy thành công với FE/BE/SQL healthy. 13 kiểm tra FR-57 bằng trình duyệt/API/SQL thật trên Compose và 1 kiểm thử đăng nhập/throttle thật pass; fixture phản hồi được dọn. Backend đạt 319/319 test và cả bốn JaCoCo metric >80%. Stack được dừng bằng `down`, giữ named volumes; CI/Sonar/CodeQL và nghiệm thu sau merge chưa xác minh.

## Giao diện demo và giới hạn hiện tại

### Landing công khai và Home ứng dụng

- `/` là Landing giới thiệu sản phẩm và tầm nhìn Mâm Xanh cho cả Guest và người đã đăng nhập. Trang dùng palette kem/xanh cố định, không có thao tác đổi theme và không tải dữ liệu công thức hoặc dữ liệu cá nhân từ Backend để render.
- `/trang-chu` giữ Home ứng dụng hiện có. Logo và mục Trang chủ trong ứng dụng dẫn về route này; đăng nhập email/Google thành công cũng mở `/trang-chu`, trừ trường hợp Backend mời onboarding. Hoàn tất hoặc bỏ qua onboarding vẫn mở `/kham-pha`. Các deep link chức năng và phân quyền giữ nguyên.
- Nút Bắt đầu ở header và Bắt đầu khám phá trong Hero/cuối Landing mở `/trang-chu` cho Guest mà không yêu cầu đăng nhập hoặc đăng ký. Đăng nhập là lựa chọn riêng; việc tạo tài khoản chỉ được đề xuất tại chức năng lưu công thức theo tài khoản.
- Landing giới thiệu công thức, lưu món, lịch ăn, Shopping List, dinh dưỡng, hỗ trợ AI và nội dung từ Chuyên gia theo phạm vi sản phẩm đã chốt. Theo quyết định nội dung của nhóm, không gắn nhãn tiến độ trên Landing; các đoạn giới thiệu không phải bằng chứng nghiệm thu những chức năng chưa kết nối. Giới hạn implementation tiếp tục được mô tả trong các mục dưới đây.
- Preview trên Landing là minh họa tĩnh dựng bằng HTML/CSS và ảnh món ăn có sẵn trong `public/assets/`, gồm món ăn, bộ sưu tập, lịch ăn, đi chợ, dinh dưỡng và hỗ trợ AI. Các khối minh họa không có control tương tác và được ẩn khỏi cây truy cập; số liệu mẫu không phải dữ liệu cá nhân hoặc kết quả AI trực tiếp. Landing không gọi Backend để tải những preview này; CTA mở các route ứng dụng hiện có.
- Header giữ logo, đăng nhập và nút Bắt đầu; liên kết các mục giới thiệu nằm ở footer và gợi ý cuộn sau Hero. Ảnh dưới màn hình đầu được lazy-load; bố cục đáp ứng desktop/mobile và giữ palette kem/xanh cố định.
- Hiệu ứng xuất hiện theo cuộn dùng `IntersectionObserver`, có thể chạy lại khi quay về một mục. Thiết lập giảm chuyển động hoặc trình duyệt thiếu observer giữ nội dung hiển thị đầy đủ. Test trình duyệt xác minh ảnh tải được, điều hướng, bàn phím, ba vai trò đăng nhập và các kích thước 360/768/1440 px; đây là kiểm thử Frontend, không phải bằng chứng Backend/SQL.
- Google Stitch hỗ trợ proposal desktop/mobile trong project `4319306096093270564`. Bố cục và palette được chuyển thủ công sang React/CSS của repo; bỏ script CDN, font/icon phụ, asset logo mới và các khả năng ngoài SRS mà generator tự thêm. Samsung Food chỉ được dùng làm tham khảo cách kể câu chuyện sản phẩm.
- Application routes được tải theo chunk riêng, gồm theme, danh sách đã lưu và các hiệu ứng của ứng dụng. Landing không tải toàn bộ màn hình ứng dụng ngay từ đầu; khi vào ứng dụng, lựa chọn theme đã lưu vẫn được áp dụng.
- Metadata mô tả/OG được sinh từ `.figma/make/site.json`, ngôn ngữ HTML là `vi`, title Landing được quản lý bằng React. Cấu hình preview `noindex` hiện có vẫn được giữ; bổ sung metadata không đồng nghĩa trang đã được deploy hoặc mở indexing.

Bản demo Vercel hiện tại: [Mâm Xanh trên Vercel](https://mamxanh-frontend.vercel.app/) vẫn là bản UI cũ dùng dữ liệu mẫu và chưa có đăng nhập/phân quyền thật. Bản chạy local dưới đây dùng Backend và SQL Server thật cho các luồng đã tích hợp; thay đổi hiện tại chưa được deploy lên Vercel.

### Giao diện sáng/tối và các luồng đã kết nối

- Bố cục nền tối mặc định tham khảo các ảnh Samsung Food qua Google Stitch, sau đó chuyển thủ công sang component React của Mâm Xanh: header gọn, tab gạch chân, thẻ ảnh dọc, Home ba cột, lịch tháng chọn tuần và danh sách đi chợ dạng checklist. Mục Cài đặt giao diện trong menu cho phép đổi sang nền sáng hoặc tối; lựa chọn được lưu trên trình duyệt. Cả hai chủ đề dùng chung token màu để áp dụng nhất quán trên các trang và thành phần native. Giữ logo, route, quyền và API của dự án; không nhập dữ liệu tài khoản hoặc công thức mẫu từ code Stitch.
- Home tải hai nhóm `NEWEST` và `MOST_LIKED` từ `GET /recipes`; loại ăn chay lấy từ `form-options`. Liên kết từ Home mở Explore với query tương ứng. Explore giữ lọc AND, tìm kiếm, sáu chế độ sắp xếp và phân trang từ API, có bộ lọc thu gọn trên điện thoại.
- Nút lưu trên thẻ công thức công khai và trang chi tiết gọi `PUT /saved-recipes/{recipeId}`; bỏ lưu gọi `DELETE` cùng đường dẫn. Guest được mời đăng nhập, Administrator không truy vấn danh sách riêng của Member. Trạng thái nút chỉ đổi sau khi Backend xác nhận; lỗi có thể thử lại.
- `/ho-so` hiển thị tài khoản đang đăng nhập và danh sách `GET /saved-recipes` riêng, tìm kiếm và phân trang qua Backend. Response dùng `content` theo generated OpenAPI. Recipe không khả dụng vẫn có mục bỏ lưu an toàn, không lộ nội dung hoặc liên kết chi tiết. Cache dùng ID tham chiếu và được xóa khi đổi tài khoản; không dùng danh sách yêu thích hard-code hoặc localStorage làm nguồn dữ liệu.
- Member chọn tuần qua lịch tháng hoặc nút trước/sau trên `/ke-hoach`; mỗi tuần đọc `GET /meal-plans?weekStartDate=...`. Bảy ngày trình bày thành danh sách dọc, mặc định mở hôm nay (hoặc Thứ Hai khi chuyển sang tuần khác); chọn ngày mới mở ba bữa Sáng/Trưa/Tối xếp dọc. Trên điện thoại, lịch tháng mở qua nút “Chọn ngày trong lịch”. `MealPlanWeekEditor` hỗ trợ lưu tuần trống, thêm nhiều món trong mỗi bữa từ **công thức đã lưu hoặc kho công thức công khai**, thay và gỡ món theo các API FR-09 trong [API Guide](../../docs/api/API.md#72-thêm-thay-và-gỡ-món--fr-09). Chỉ cập nhật lịch sau response thành công; lỗi giữ dữ liệu hiện tại, request đến muộn khi đổi tuần bị bỏ qua.
- Mục bị xóa/ẩn hiển thị thông báo trung tính “Công thức không còn khả dụng”, không có link mở chi tiết và vẫn cho thay/gỡ. Hộp chọn món mặc định ở nguồn Đã lưu (`GET /saved-recipes`); Member có thể chuyển sang Tìm công thức (`GET /recipes`) ngay trong hộp thoại thêm/thay món. Hai nguồn hỗ trợ tìm kiếm và phân trang 12 món/trang, thao tác bàn phím và chặn lựa chọn đã có trong bữa hoặc không còn khả dụng; Backend vẫn kiểm tra lại quyền sở hữu và trùng lặp. Khi Đã lưu trống, nút Tìm công thức công khai chuyển nguồn tại chỗ; không cần lưu món trước và không tự thêm vào Đã lưu. Chuyển nguồn đặt lại tìm kiếm/trang và bỏ qua phản hồi nguồn cũ đến muộn. Bỏ lưu công thức không tự gỡ món đã nằm trong lịch.
- Nút dinh dưỡng ngày/tuần mở bảng chín chỉ số, thống kê **cho 1 người, mỗi món 1 phần ăn** bằng cách cộng `nutrition.perServing` từ `GET /recipes/{recipeId}` cho các món trong phạm vi đã chọn. Chỉ tải khi mở bảng, gộp ID công thức trùng và giới hạn tối đa bốn request đồng thời; không gọi dịch vụ dinh dưỡng ngoài. Backend chia dinh dưỡng toàn công thức cho số phần ăn gốc để trả `perServing`; Frontend không chia lần nữa và không nhân theo số phần nấu cho cả nhà (`plannedServings`). Tổng tuần cộng các ngày đã lên lịch cho 1 người, không phải trung bình ngày. Bảng tuần có thêm năng lượng từng ngày cho 1 người. Món thiếu dữ liệu hoặc không còn khả dụng được liệt kê rõ; tổng chưa xác định ghi “Chưa đủ dữ liệu”, không tự thay bằng 0. Đây là dinh dưỡng **đã lên kế hoạch**, không phải dữ liệu đã ăn thực tế. Kiểm thử Playwright của màn này dùng API giả lập, không chứng minh tính bền vững hoặc transaction trên SQL Server thật.
- Trang lịch chỉ hiển thị số phần đã lưu. Bấm công thức từ lịch mở trang chi tiết với context `mealPlanWeek`, `mealPlanEntryId` và `mealDate`; Frontend đọc lại lịch riêng và kiểm tra entry thuộc đúng công thức trước khi cho chỉnh. Nút tăng/giảm đổi từng **1 phần nguyên**, nút “Lưu khẩu phần vào lịch ăn” gọi `PATCH /meal-plans/{weekStartDate}/entries/{entryId}/servings` và chỉ sửa món của ngày/bữa đã bấm. API chấp nhận số nguyên 1–9.999; dữ liệu lẻ cũ được cảnh báo, không tự ghi đè. Lỗi giữ lượng đã lưu và cho thử lại. Quay về lịch giữ tuần/ngày và tải lại số phần từ Backend. Mở công thức thông thường chỉ thay định lượng đang xem, không tự ghi lịch; liên kết chia sẻ không mang context lịch riêng. Đây là đầu vào để tính nguyên liệu đi chợ, còn bảng dinh dưỡng vẫn dành cho 1 người, mỗi món 1 phần.
- Member có thể chuyển món giữa bảy ngày và ba bữa của cùng tuần bằng hộp chọn ngày/bữa đích; giữ số phần, chặn công thức trùng và giữ dữ liệu cũ khi request thất bại. Chuyển thành công mở ngày đích; nguồn không khả dụng giữ nhãn trung tính.
- Đi chợ, Cộng đồng, lịch ăn Guest và các màn hình chưa tích hợp vẫn là prototype; banner nêu rõ thao tác chưa lưu vào tài khoản. Shopping List và tính năng theo dõi tác giả/nhóm chưa được bổ sung bởi luồng FR-33.
- Kiểm tra local bằng trình duyệt với API thật đã đối chiếu bộ lọc, Home, chuyển tuần; lưu/bỏ lưu được đối chiếu trực tiếp với SQL Server và khôi phục mục kiểm thử. Bằng chứng này dành cho các luồng được nêu, không xác nhận nghiệm thu toàn bộ FR hoặc gate GitHub Actions.

### Đăng ký, đăng nhập và khôi phục tài khoản (UI FR-03)

- **Bảng hỏi Onboarding từng bước:** `/khoi-tao-so-thich` hiển thị lần lượt 6 câu (loại ăn chay, nguyên liệu cần tránh, món không thích, khẩu vị, thời gian nấu, độ khó), có tiến độ và Quay lại/Tiếp tục. Câu trả lời được giữ trong state khi đổi bước; chỉ gửi `PUT /nutrition/dietary-preferences` khi nhấn Hoàn tất ở bước cuối. Ba câu đầu vẫn phải xác nhận để lưu, ba câu sau có thể để trống. Bỏ qua luôn kết thúc toàn bộ bảng hỏi qua API skip và mở Explore; không gửi bản nháp hoặc tự xác nhận “không có”. Sau này bổ sung trong Sở thích ăn uống; trang chỉnh sửa này vẫn dùng biểu mẫu đầy đủ. Lỗi trường từ Backend đưa về đúng câu để sửa, không xóa câu trả lời đang nhập.
- Header mặc định dành cho Guest có Đăng nhập/Đăng ký. Các trang `/dang-nhap`, `/dang-ky`, `/quen-mat-khau`, `/xac-minh-email`, `/dat-lai-mat-khau` dùng bố cục responsive riêng cùng nhận diện dự án.
- **Đăng ký và xác minh email đã gọi Backend thật (Issue #5):** form đăng ký kiểm tra tên 3–50 ký tự, email và mật khẩu 8–64 ký tự (tối đa 72 byte UTF-8) có chữ in hoa, chữ thường, chữ số, rồi gửi `POST /auth/register`. Liên kết trong email mở `/xac-minh-email?token=...`; trang gửi mã một lần, xóa mã khỏi thanh địa chỉ và báo kết quả. Mã sai/hết hạn cho phép yêu cầu gửi lại email; gửi lại trong 60 giây hiển thị số giây chờ từ header `Retry-After`.
- **Đăng nhập bằng email/mật khẩu đã gọi Backend thật (Issue #6):** gửi `POST /auth/login`; thành công thì lưu phiên (access token, thời điểm hết hạn, `account`) trong `sessionStorage` key `mamxanh.auth` của tab hiện tại và hiển thị tên tài khoản thật ở header. Lỗi hiển thị theo `code`: sai email/mật khẩu (thông báo trung tính), email chưa xác minh (kèm liên kết gửi lại email), tài khoản bị khóa, và khóa đăng nhập tạm 10 phút (số phút lấy từ `Retry-After`). Mật khẩu luôn được xóa khỏi state sau mỗi lần gửi.
- Nơi lưu token nằm trong một hàm duy nhất (`src/lib/authStorage.ts`). Axios tự gắn `Authorization: Bearer` cho request cần đăng nhập (không gắn cho `/auth/*`); Backend trả `401` hoặc `403 ACCOUNT_LOCKED` thì Frontend xóa phiên và đưa về trang Đăng nhập. Phiên tự kết thúc khi token hết hạn (không có refresh token), bị xóa khi tab đóng và không đồng bộ giữa các tab.
- Docker Development chạy thêm kiểm thử Playwright tích hợp: trình duyệt đăng nhập qua Frontend/Backend đang chạy và truy vấn SQL Server Compose để xác minh bộ đếm login sai, khóa lần thứ năm và reset sau khi hết hạn.
- **Onboarding và Sở thích ăn uống đã gọi Backend thật (Issue #36):** sau khi đăng nhập, Frontend gọi `POST /nutrition/dietary-preferences/onboarding/invitation`; chỉ khi Backend trả `show = true` (lần đầu của tài khoản mới chưa trả lời) Member mới được chuyển tới `/khoi-tao-so-thich` để chọn loại ăn chay, nguyên liệu cần tránh, món không thích (hoặc xác nhận "Không có") và sở thích tùy chọn. Rời trang mà chưa trả lời hoặc bấm "Bỏ qua" (ghi nhận `SKIPPED`) thì các lần đăng nhập sau không hỏi lại; trang vẫn mở thủ công được. Trang `/ho-so/so-thich-an-uong` (menu "Sở thích ăn uống") cho xem, cập nhật hồ sơ và cho biết còn thiếu thông tin nào trước khi dùng AI cá nhân hóa. Gợi ý tên lấy từ danh mục nguyên liệu chuẩn đang hoạt động.
- **Đăng nhập Google đã gọi Backend thật (Issue #8):** trang Đăng nhập và Đăng ký hiện nút "Tiếp tục với Google" của Google Identity Services (`@react-oauth/google`) khi có `VITE_GOOGLE_CLIENT_ID`; script Google chỉ được tải trên hai trang này. Google trả ID Token, Frontend gửi `POST /auth/google` rồi lưu phiên và kiểm tra lời mời Onboarding như khi đăng nhập bằng mật khẩu. Lỗi hiển thị theo `code`: token Google không hợp lệ (`GOOGLE_TOKEN_INVALID`), tài khoản bị khóa (`ACCOUNT_LOCKED`), email đã liên kết Google khác (`GOOGLE_ACCOUNT_CONFLICT`) và Google tạm không khả dụng (`GOOGLE_LOGIN_UNAVAILABLE`). Thiếu Client ID thì thay nút bằng thông báo; script Google không tải được thì vẫn đăng nhập bằng email được.
- **Hồ sơ tác giả đã gọi Backend thật (Issue #29):** trang chi tiết `/cong-thuc/id/:recipeId` hiện ảnh đại diện, tên tác giả (liên kết tới `/thanh-vien/:userId`) và ngày đăng; `/thanh-vien/:userId` là hồ sơ công khai (tên, ảnh, giới thiệu, tháng tham gia, bài đã đăng có phân trang); `/ho-so/cai-dat` cho Member sửa tên, giới thiệu ngắn và ảnh đại diện. Không có ảnh hoặc ảnh lỗi thì hiện ảnh mặc định chữ cái đầu như header.
- **Nguyên liệu chưa có dữ liệu dinh dưỡng (Issue #45):** trang chi tiết công thức gắn nhãn "Chưa hỗ trợ tính dinh dưỡng" cạnh từng nguyên liệu Backend trả `nutritionSupported = false`, cùng thông báo dinh dưỡng chưa đầy đủ đã có.
- **Theo dõi thành viên đã gọi Backend thật (Issue #69):** trang `/thanh-vien/:userId` hiện số người theo dõi và số tài khoản đang theo dõi; Member bấm "Theo dõi"/"Đang theo dõi" để theo dõi hoặc bỏ theo dõi, bộ đếm lấy từ phản hồi của Backend. Guest được dẫn tới đăng nhập; hồ sơ của chính mình và tài khoản Administrator không có nút. Bấm vào bộ đếm để xem danh sách (cần đăng nhập, "Xem thêm" theo trang). Lỗi như theo dõi tài khoản đang bị khóa hiện nguyên văn thông báo của Backend.
- **Bình luận đã gọi Backend thật (Issue #48):** trang chi tiết `/cong-thuc/id/:recipeId` (và `/cong-thuc/:id`) hiện các luồng bình luận lồng tối đa 5 cấp, "Xem thêm bình luận" theo trang. Member viết, trả lời, sửa, xóa bình luận của mình (xác nhận trước khi xóa); trả lời bình luận cấp 5 tự chèn `@Tên`. Bình luận đã xóa còn reply hiện "Bình luận này đã bị xóa bởi người dùng". Guest được mời đăng nhập; Administrator chỉ xem. Nội dung hiển thị như văn bản, không render HTML.
- **Thông báo reply (Issue #51, FR-49):** khi đăng nhập, header có chuông `Thông báo` với số chưa đọc (`GET /api/v1/me/notifications/unread-count`, tải lại khi đổi trang, khi quay lại tab, mỗi 60 giây và sau mỗi thao tác đọc). `/thong-bao` liệt kê thông báo mới nhất trước, lọc "Chưa đọc", phân trang 20 mục, "Đánh dấu tất cả đã đọc"; bấm một thông báo thì đánh dấu đã đọc rồi mở `targetPath`. Trang chi tiết công thức nhận `#comment-{id}`, tự tải thêm trang bình luận tới khi thấy bình luận đó rồi đưa focus tới. `/ho-so/cai-dat#email-thong-bao` có công tắc nhận email khi có người trả lời bình luận.
- **Đăng xuất** chỉ xóa token và trạng thái đăng nhập trên thiết bị, không gọi Backend (AC-03.13).
- **Quên và đặt lại mật khẩu đã gọi Backend thật (Issue #9):** `/quen-mat-khau` gửi `POST /auth/password-resets` và hiển thị nguyên văn thông báo trung tính của Backend. Liên kết trong email mở `/dat-lai-mat-khau?token=...`; Frontend giữ token trong bộ nhớ, xóa nó khỏi thanh địa chỉ, kiểm tra quy tắc mật khẩu rồi gửi `POST /auth/password-resets/confirm`. Liên kết hỏng, đã dùng hoặc hết hạn (`PASSWORD_RESET_TOKEN_INVALID`) ẩn biểu mẫu và dẫn tới yêu cầu liên kết mới; mật khẩu trùng mật khẩu hiện tại (`NEW_PASSWORD_SAME_AS_CURRENT`) báo ngay dưới ô mật khẩu.
- Không còn chế độ đổi vai trò/tài khoản giả trên Frontend. Dùng tài khoản local do Backend seed tạo sẵn; danh sách email và cách đặt chung mật khẩu nằm trong [Backend Workspace Guide](../mamxanh-backend/README.md#tài-khoản-demo-local). Mỗi vai trò phải đăng nhập bằng tài khoản riêng; quyền được Backend xác thực.
- Các luồng Backend hiện hiển thị từ API/SQL thật: khám phá và chi tiết công thức; tạo/sửa/quản lý công thức; lịch ăn tuần; hồ sơ dinh dưỡng; sở thích/onboarding; nộp và duyệt đơn Chuyên gia; quản lý danh mục Ingredient dinh dưỡng Admin gồm tìm/lọc, xem chi tiết, thêm/sửa nguồn và chỉ tiêu, đổi trạng thái, xem Recipe Post tham chiếu. Trang khám phá gửi từ khóa, bộ lọc loại ăn chay/thể loại/nguyên liệu/tổng thời gian, phân trang và chế độ sắp xếp tới API công khai; tùy chọn chuẩn và nguyên liệu được tải từ Backend, mọi điều kiện lọc kết hợp bằng AND.
- **So sánh hai công thức (Issue #70):** `/so-sanh?a={recipeId}&b={recipeId}` đối chiếu hai công thức công khai khác nhau: thông tin tổng quan, nguyên liệu (chỉ đặt cùng hàng khi cùng nguyên liệu trong danh mục, giữ nguyên số lượng và đơn vị) và chín chỉ tiêu dinh dưỡng trên một khẩu phần kèm chênh lệch (công thức đầu tiên − công thức thứ hai). Bắt đầu từ nút "So sánh" trên thẻ công thức, trang chi tiết hoặc danh sách đã lưu; nút "Chọn công thức thứ hai" (hoặc "Đổi công thức") mở trang chọn riêng `/so-sanh/chon-mon` dùng chung ô tìm kiếm, bộ lọc, sắp xếp và phân trang với trang Khám phá (`src/components/RecipeSearch.tsx`), loại món đang ở bên còn lại và chọn xong quay về `/so-sanh?a=&b=`. Dữ liệu đọc từ `GET /recipes/{recipeId}` và `GET /recipes/discovery`; công thức thiếu dữ liệu dinh dưỡng hiện "Chưa đủ dữ liệu", không tính chênh lệch. Trang không ghi dữ liệu, không lưu lịch sử và không xuất PDF.
- **Xử lý báo cáo vi phạm (Issue #33):** `/admin/bao-cao` đọc hàng đợi báo cáo Recipe Post từ `GET /admin/reports` và số lượng từ `GET /admin/reports/status-counts`, lọc theo Chờ xử lý, Đang xem xét, Đã giải quyết. Administrator bấm "Tiếp nhận xem xét" (`POST /admin/reports/{id}/claim`), xem người báo cáo, tác giả và nội dung báo cáo, rồi chọn một trong bốn kết luận (không vi phạm, cảnh cáo tác giả, ẩn bài viết, khóa tài khoản tác giả) với lý do 10–500 ký tự trước khi "Hoàn tất xử lý" (`POST /admin/reports/{id}/resolve`). Thiếu kết luận hoặc lý do thì không gửi request. Báo cáo do Administrator khác giữ hoặc đã giải quyết chỉ xem được. Thời gian hiển thị theo giờ Việt Nam.
- Lỗi từ API được đọc theo HTTP status và `code` của ProblemDetail ([API Guide](../../docs/api/API.md) mục 4), không phân tích câu chữ trong `detail`.

### Các chức năng demo khác

Giao diện tài khoản dùng đèn tương tác và vùng biểu mẫu cuộn được khi nội dung cao hơn màn hình. Lịch ăn có hiệu ứng chuyển tháng, điều hướng bằng phím mũi tên và hỗ trợ thiết lập giảm chuyển động của hệ điều hành. Các trang chi tiết công thức vẫn dùng API lượt xem, bình chọn và bình luận sau khi đổi bố cục.

Guest mở `/ke-hoach` hoặc `/di-cho` được mời đăng nhập; bản xem thử local chỉ mở bằng `?demo=true`. Hai module Admin tài khoản/phân quyền và giao dịch có nhãn “Giao diện minh họa”: tìm/lọc, xem chi tiết và thay đổi dữ liệu mẫu chỉ tồn tại trong bộ nhớ, tải lại trang sẽ khôi phục. Các thao tác này chưa ghi Backend và không chứng minh các FR quản trị đã hoàn thành.

| Khu vực | Người dùng có thể thử | Giới hạn hiện tại |
|---|---|---|
| Trang chủ và thanh điều hướng | Nền tối, header gọn, hai nhóm công thức API và menu responsive | Preview cộng đồng vẫn được ghi nhãn mẫu; không có API nhóm hoặc theo dõi tác giả |
| Bình luận công thức | Viết, trả lời, sửa và xóa bình luận; tối đa 5 cấp; xóa bình luận cha vẫn giữ trả lời | Guest đọc công khai; Customer/Expert đăng nhập mới được thao tác; Backend lưu bình luận và trả lời |
| Khẩu phần tại công thức | Chọn 1–50 phần; nguyên liệu = lượng gốc / số phần gốc × số phần muốn nấu; từ trang công thức mở hộp thoại thêm nguyên liệu vào Shopping List và điều chỉnh riêng khẩu phần cần mua | Khi xác nhận, Backend scale nguyên liệu theo khẩu phần đã chọn rồi tổng hợp vào Shopping List cá nhân; công thức đã được thêm không bị cộng trùng |
| Kế hoạch bữa ăn Guest | Đăng nhập để lưu lịch; `?demo=true` mở bản mẫu với 0,5–10 phần cho từng món, bước 0,5 | Bản mẫu chỉ cập nhật UI; Member dùng API lịch tuần, thêm món với khẩu phần mặc định 1 |
| Báo cáo công thức | Mở nút Báo cáo, chọn 1 trong 6 lý do; lý do Khác cần mô tả 10–500 ký tự | Nút chỉ kiểm tra biểu mẫu, chưa gửi cho quản trị viên |
| Dinh dưỡng công thức prototype tại `/cong-thuc/:slug` | Đọc số liệu minh họa cho **1 khẩu phần** | 8 chỉ tiêu tĩnh, xem BUG-002; trang thật `/cong-thuc/id/:id` dùng 9 chỉ tiêu và trạng thái dữ liệu từ Backend |
| Theo dõi dinh dưỡng (`/dinh-duong`) | Đọc lịch ăn Member qua `GET /meal-plans/{weekStartDate}/nutrition`; đổi ngày/tuần cập nhật số liệu; xuất báo cáo PDF theo đúng phạm vi qua `/nutrition/report` | Cần đăng nhập Member và xác nhận phạm vi hỗ trợ dinh dưỡng; số liệu là khẩu phần dự kiến. Danh sách mua sắm, cộng đồng/bình luận/báo cáo, gói AI và giao dịch vẫn là các màn hình minh họa nếu không được liệt kê riêng là đã nối API |

Các trang chưa có API được giữ làm prototype để minh họa phạm vi giao diện; chúng được ghi nhãn demo và không đại diện cho dữ liệu đã lưu trên Backend. Theo dõi dinh dưỡng là trang đọc API thật và dùng cùng phân tích/xuất PDF với Lịch ăn.

Các màn hình này chuẩn bị trải nghiệm cho FR-13, FR-20, FR-26/27, FR-35/38, FR-37/39 và FR-46; không xác nhận đã hoàn thành toàn bộ Acceptance Criteria của các FR. SRS vẫn là nguồn yêu cầu chính thức.

## Vercel cho Frontend

- Vercel là nền tảng hosting đã chốt cho Frontend production. Backend được triển khai riêng trên Azure App Service; Frontend gọi Backend qua HTTPS/REST API.
- Bản UI hiện tại được triển khai thủ công từ thư mục Frontend. Vercel gắn nhãn môi trường `Production` cho link demo, nhưng bản này vẫn dùng dữ liệu mẫu và chưa phải bằng chứng toàn hệ thống đã được triển khai.
- `vercel.json` chuyển các đường dẫn SPA về `index.html`, giúp mở trực tiếp hoặc tải lại trang con bằng React Router.
- Build dùng `npm run build`, đầu ra `dist/`. `.vercel/` là thông tin liên kết tài khoản/project local và được bỏ qua trong Git.
- Chưa thiết lập Vercel Git auto-deploy hoặc deployment workflow. GitHub test CI chạy verification; push code không tự cập nhật link Vercel, mỗi lần cập nhật demo vẫn cần deploy thủ công.
- Dockerfile Frontend dùng để đồng bộ môi trường development giữa các thành viên, không dùng để deploy Frontend lên Vercel.

---

## Thanh toán xác minh — FR-13 / Issue #142

- Bấm nâng cấp PLUS/PRO gửi đúng `tier` đến Backend, khóa thao tác trong lúc tạo đơn và chỉ chuyển tới checkout HTTPS của payOS. Thiếu cấu hình/lỗi tạo đơn hiển thị lỗi tại bảng giá; không tự dẫn tới trang thành công.
- `/payment/success` và `/payment/cancel` đều đọc kết quả đơn từ API riêng tư theo `orderCode`. `status=PAID`, `cancel` hoặc `mock=true` trên URL không chứng minh giao dịch đã thành công. Chỉ báo kích hoạt khi Backend xác nhận đơn `PAID` và `activated=true`.
- Trang kiểm tra tối đa 10 lần, cách nhau 3 giây sau mỗi response còn chờ; khi hết lượt chỉ thông báo chưa nhận xác nhận và có nút “Kiểm tra lại”, không yêu cầu trả tiền lần nữa. Lỗi API dừng polling và cho thử lại. Request/timer được hủy khi rời trang, đổi đơn hoặc đổi tài khoản.
- Đơn hủy/thất bại giữ quyền lợi hiện tại. Đơn lịch sử đã thanh toán nhưng subscription không còn hiệu lực hiển thị đã thanh toán và gói hiện tại, không quảng cáo vừa kích hoạt. Quyền lợi đọc theo `unlockedFeatures`, cùng tên trong generated OpenAPI; lịch sử đọc gói do Backend trả.
- Chưa đăng nhập thì mời đăng nhập; mã đơn sai/thiếu không gửi request. Kiểm thử browser API mô phỏng nằm trong `subscription.spec.ts` và `payment-verification.spec.ts`; không thay thế bằng chứng trả tiền thật hoặc tích hợp Compose.

## 1. Yêu cầu môi trường

- **Node.js**: Phiên bản `>= 20.x` (khuyến nghị `v24.x` hoặc `v22.x LTS`).
- **Package Manager**: `npm` (tiêu chuẩn của dự án).
- **IDE**: IntelliJ IDEA (Ultimate hoặc Community với Node.js/Web plugin) / WebStorm.

Nếu chạy bằng Docker, thành viên chỉ cần Docker Desktop đang hoạt động; không cần cài Node.js trực tiếp trên máy.

## Khởi động nhanh

Nếu cần mở cả hệ thống, dùng hai cửa sổ Terminal và khởi động theo thứ tự:

1. Microsoft SQL Server và database `MamXanhDB`.
2. [Backend](../mamxanh-backend/README.md) tại port `8080`.
3. Frontend tại port `5173`.

Frontend gọi Backend tại `VITE_API_BASE_URL`. Khi chạy trực tiếp, `/api/v1` được Vite proxy tới `http://localhost:8080`; trong Docker Compose, proxy dùng service `backend`. Để đổi base path, sao chép `.env.example` thành `.env.local` (đã được Git bỏ qua) rồi sửa giá trị; không đặt secret trong biến `VITE_` vì chúng nằm trong bundle công khai. Backend phải cho phép origin của Frontend qua `MAMXANH_CORS_ALLOWED_ORIGINS`.

Đăng nhập Google cần `VITE_GOOGLE_CLIENT_ID` (Client ID công khai, cùng giá trị với `MAMXANH_GOOGLE_CLIENT_ID` của Backend) và origin của Frontend (ví dụ `http://localhost:5173`) phải có trong mục Authorized JavaScript origins của OAuth client trên Google Cloud Console. Biến được gắn vào bundle lúc build. Bản build kiểm thử E2E (`npm run test:e2e:coverage`) tự đặt một Client ID giả và thay Google Identity Services bằng script stub, nên test không gọi Google; khi tự build rồi chạy Playwright, đặt `VITE_GOOGLE_CLIENT_ID` trước lệnh build.

Chỉ các luồng được liệt kê là đã kết nối mới là bằng chứng hiển thị dữ liệu Backend; các UI demo khác không chứng minh Backend hoặc database đã kết nối.

### Cách 1 — Chạy trực tiếp bằng Node.js

Mở Terminal tại `app/mamxanh-frontend` và chạy:

```powershell
npm ci
npm run dev
```

Mở <http://localhost:5173>. Vite theo dõi source và tự cập nhật trình duyệt khi code thay đổi.

Sau lần cài đầu tiên, các lần mở dự án tiếp theo chỉ cần:

```powershell
npm run dev
```

Luôn dùng `npm ci` khi cài mới từ `package-lock.json` hoặc khi dependency bị lệch. Không chia sẻ thư mục `node_modules` giữa các thành viên.

### Cách 2 — Chạy bằng Docker

Để chạy đồng bộ toàn bộ ứng dụng, dùng Docker Compose từ root repository theo hướng dẫn tại [Backend README](../mamxanh-backend/README.md#cách-2--chạy-bằng-docker). Cách chạy riêng Frontend bên dưới chỉ dành cho debug component; không thay thế kiểm thử tích hợp hoặc Docker Development gate. Quy tắc chung do [CONTRIBUTING.md](../../CONTRIBUTING.md#docker-development) quản lý.

Đảm bảo Docker Desktop đã khởi động, sau đó mở PowerShell tại `app/mamxanh-frontend`:

```powershell
docker build -t mamxanh-frontend-dev .

docker run --rm --name mamxanh-frontend `
  -p 127.0.0.1:5173:5173 `
  --mount "type=bind,source=$($PWD.Path),target=/workspace" `
  --mount "type=volume,source=mamxanh-frontend-node-modules,target=/workspace/node_modules" `
  mamxanh-frontend-dev
```

Mở <http://localhost:5173>. Bind mount đồng bộ source vào container; named volume giữ `node_modules` Linux tách khỏi máy Windows để tránh lỗi dependency native khác hệ điều hành.

Dừng bằng `Ctrl+C`. Container tự xóa vì dùng `--rm`; volume dependency được giữ lại để lần chạy sau nhanh hơn.

Với stack Compose chuẩn, khi `package.json` hoặc `package-lock.json` thay đổi, mở PowerShell tại root repository rồi refresh đúng named volume theo lockfile bằng one-off command (xem [hướng dẫn chuẩn](../../CONTRIBUTING.md#docker-development)); dùng cùng `-p <project>` với stack đang chạy:

```powershell
$composeProject = 'mamxanh-dev'
docker compose -p $composeProject --env-file app/mamxanh-backend/.env run --rm --no-deps frontend npm ci
```

Lệnh này không xóa hay reset SQL volume. Với lệnh standalone `docker run` ở trên, volume riêng tên `mamxanh-frontend-node-modules` không được Compose quản lý: dừng container, xóa riêng volume đó bằng `docker volume rm mamxanh-frontend-node-modules`, build lại image rồi chạy lại để khởi tạo dependency từ `package-lock.json` mới. Không dùng lệnh này để reset dữ liệu SQL của stack Compose.

Để chạy cả Frontend, Backend và SQL Server trong Compose chuẩn, dùng phần **Docker Compose** trong [Backend README](../mamxanh-backend/README.md). Compose nạp seed mẫu sau Flyway và giữ dữ liệu qua `down`/`up`; reset riêng SQL volume được thực hiện bằng script có xác nhận.

### Kiểm tra trước khi bàn giao code

```powershell
npm run lint
npm run build
```

Nếu dùng Docker và không cài Node.js trên máy, chạy kiểm tra trong image:

```powershell
docker run --rm mamxanh-frontend-dev npm run lint
docker run --rm mamxanh-frontend-dev npm run build
```

### Lỗi khởi động thường gặp

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `npm` không được nhận diện | Chưa cài Node.js hoặc Terminal chưa nạp lại `PATH` | Cài Node.js 22 LTS rồi mở Terminal mới, hoặc dùng Docker. |
| Port `5173` đã được sử dụng | Một Vite/container khác đang chạy | Dừng tiến trình cũ; không tự đổi port nếu nhóm đang dùng URL chuẩn `5173`. |
| Dependency/native binary lỗi sau khi đổi máy | `node_modules` được sao chép từ máy hoặc hệ điều hành khác hoặc dependency volume cũ sau khi lockfile đổi | Cài bằng `npm ci`; với Compose dùng one-off `run --rm --no-deps frontend npm ci`; chỉ troubleshooting mới xóa riêng Frontend dependency volume. |
| Docker không nhận lệnh | Docker Desktop chưa cài, chưa chạy hoặc chưa có trong `PATH` | Mở Docker Desktop và xác minh bằng `docker version`. |

---

## 2. Cách mở và chạy trong IntelliJ IDEA

### Bước 1: Mở dự án trong IntelliJ IDEA
1. Khởi động **IntelliJ IDEA**.
2. Chọn **File** -> **Open...** (hoặc **Open Project** ở màn hình Welcome).
3. Trỏ tới thư mục:
   ```
   D:\Semester 5\SWP391\vegetarian-support-system\app\mamxanh-frontend
   ```
4. Chọn **Trust Project** nếu có thông báo tin cậy dự án.

### Bước 2: Chạy ứng dụng bằng nút Run (Play) trên thanh công cụ
Dự án đã được cấu hình sẵn các **Run Configurations** nằm trong `.idea/runConfigurations/` và `.run/`:
- **`dev`**: Khởi chạy dev server Vite (tự động mở trình duyệt tại `http://localhost:5173`).
- **`build`**: Đóng gói production build vào thư mục `dist/`.
- **`preview`**: Chạy preview bản build tại `http://localhost:4173`.

> **Thao tác**: Trên thanh công cụ trên cùng góc phải, chọn configuration **`dev`** và bấm nút **Play (▶️)** (hoặc `Shift + F10`). Trình duyệt sẽ tự động mở trang web Mâm Xanh.

### Bước 3: Sử dụng cửa sổ công cụ `npm` trong IntelliJ
Nếu bạn muốn chạy các lệnh nhanh từ giao diện đồ họa:
1. Vào menu **View** -> **Tool Windows** -> **npm** (hoặc click icon `npm` ở thanh bên).
2. Danh sách các script sẽ hiển thị: `dev`, `build`, `preview`, `lint`, `format`.
3. Nhấp đúp (Double-click) vào `dev` để chạy dev server.

---

## 3. Các lệnh dòng lệnh (Terminal)

Nếu sử dụng Terminal trong IntelliJ (`Alt + F12`) hoặc Command Prompt/PowerShell:

```bash
# 1. Cài đặt thư viện (nếu clone mới)
npm install

# 2. Khởi chạy môi trường phát triển (Hot reload)
npm run dev

# 3. Kiểm tra lỗi kiểu TypeScript
npm run lint

# 4. Đóng gói cho Production
npm run build

# 5. Xem trước bản đóng gói Production
npm run preview

# 6. Chạy Frontend browser smoke test bằng Playwright
# Lệnh này tự build trước khi khởi động preview server trên 127.0.0.1:4173.
npm run test:e2e

# 7. Mở Playwright UI hoặc xem HTML report của lần chạy gần nhất
npm run test:e2e:ui
npm run test:e2e:report
```

---

## 4. Cấu trúc thư mục mã nguồn

```
app/mamxanh-frontend/
├── .idea/                 # Cấu hình dự án IntelliJ IDEA (Modules, Run Configurations, Code Styles)
├── .run/                  # Shared Run Configurations (dev, build, preview)
├── .figma/                # Dữ liệu xuất và cấu hình từ Figma Make
├── public/                # Tài nguyên tĩnh (ảnh, favicon, robots)
├── tests/e2e/             # Playwright browser smoke và E2E tests có thể chạy lại
├── src/
│   ├── components/        # Các UI component dùng chung (Layout, Header, Footer, v.v.)
│   ├── pages/             # Các trang nghiệp vụ:
│   │   ├── Home.tsx             # Trang chủ giới thiệu
│   │   ├── Explore.tsx          # Khám phá công thức chay
│   │   ├── RecipeDetail.tsx     # Chi tiết công thức nấu ăn
│   │   ├── CreateRecipe.tsx     # Tạo và chia sẻ công thức mới
│   │   ├── MealPlanner.tsx      # Lập kế hoạch thực đơn
│   │   ├── ShoppingList.tsx     # Danh sách đi chợ thông minh
│   │   ├── NutritionTracker.tsx # Nhật ký theo dõi dinh dưỡng
│   │   ├── Community.tsx        # Diễn đàn cộng đồng
│   │   ├── PostDetail.tsx       # Chi tiết bài viết cộng đồng
│   │   ├── NutritionProfile.tsx # Hồ sơ BMI tham khảo
│   │   ├── AiPlans.tsx          # Gói AI và trạng thái gói demo
│   │   ├── TransactionHistory.tsx # Lịch sử giao dịch chưa kết nối API
│   │   └── Profile.tsx          # Hồ sơ cá nhân
│   ├── data/              # Dữ liệu mẫu (mock data cho UI)
│   ├── types.ts           # Kiểu dữ liệu TypeScript
│   ├── App.tsx            # Cấu hình định tuyến React Router
│   ├── main.tsx           # Entry point React 19
│   └── index.css          # Tailwind CSS styles
├── package.json           # Khai báo thư viện và npm scripts
├── playwright.config.ts   # Base URL, preview web server, Chromium và test evidence
├── tsconfig.json          # Cấu hình TypeScript compiler và alias path (@/*)
└── vite.config.ts         # Cấu hình build Vite (server port 5173, alias, plugins)
```

---

## 5. Các cấu hình đã được tối ưu cho IntelliJ IDEA

1. **Quản lý tài nguyên & Hiệu năng**:
   - `src/` được đánh dấu là **Source Root** giúp IntelliJ nhận diện auto-import và path alias `@/` chính xác.
   - `node_modules/`, `dist/`, `.figma/`, `out/` đã được **Exclude** khỏi quá trình Indexing, giúp IntelliJ không bị đơ giật hay chiếm dụng bộ nhớ RAM.
2. **Cổng Dev Server**:
   - Đã chuyển từ cổng mặc định của Figma (`8443` trên `0.0.0.0`) sang cổng tiêu chuẩn `5173` trên `localhost`.
   - Tự động mở trình duyệt khi ấn Run (`open: true`).
3. **Tiêu chuẩn mã nguồn**:
   - Tương thích 100% Vite 8 native ESM và TypeScript 5.7.
   - Xóa bỏ các cảnh báo deprecated của Vite liên quan đến import JSON và `__dirname`.

---

## 6. Playwright browser testing

Playwright có hai cách sử dụng khác nhau:

- Browser control của agent dùng cho exploratory verification, kiểm tra console/network và thu thập bằng chứng trong một task cụ thể. Cách này không tự tạo regression test.
- `@playwright/test` chạy các test có thể lặp lại trong `tests/e2e/` bằng `npm run test:e2e`.

Lần đầu chạy trên một máy mới, cài Chromium runtime bằng:

```bash
npx playwright install chromium
```

`npm run test:e2e` tự gọi `npm run build`, sau đó Playwright khởi động Vite preview tại `http://127.0.0.1:4173`, chờ URL sẵn sàng rồi chạy Chromium. Port được giữ cố định và không tái sử dụng một server có sẵn để tránh kiểm thử nhầm ứng dụng.

Để chạy E2E cùng coverage hard gate, dùng `npm run test:e2e:coverage`. Playwright + Istanbul/NYC làm sạch coverage cũ, tạo build instrumentation riêng, chạy cùng suite E2E rồi xuất text summary, HTML, `coverage/lcov.info` và summary theo từng metric. **Lines, Statements, Functions và Branches đều phải >80%**; đúng 80% vẫn fail. Checker so sánh số đếm covered/total chính xác, không quyết định bằng phần trăm đã làm tròn. `npm run test:e2e:coverage:gate-tests` chạy regression tests cho ngưỡng strict. Nếu browser test fail thì command vẫn fail dù coverage đạt. Không hạ threshold theo baseline, exclude production source hoặc thêm test vô nghĩa để pass. Cài Chromium lần đầu bằng `npx playwright install chromium`; GitHub Actions cài thêm Linux dependencies.

Job `Sonar` chỉ đọc LCOV/JaCoCo XML sau khi tải artifacts từ Frontend/Backend; Sonar không tạo coverage. Policy yêu cầu các required checks pass trước merge vào `develop`, còn technical merge block phải được cấu hình ở [Ruleset](../../CONTRIBUTING.md#develop-required-checks). Tests hiện tại kiểm chứng demo UI/browser behavior, không chứng minh Backend/database E2E.

HTML report được tạo trong `playwright-report/`; screenshot và trace lỗi nằm trong `test-results/`. Hai thư mục này là generated evidence và không được commit mặc định.

Trên máy ít RAM, có thể giới hạn số Playwright workers bằng biến môi trường của tiến trình `PLAYWRIGHT_WORKERS`, ví dụ đặt `$env:PLAYWRIGHT_WORKERS = '2'` rồi chạy `npm.cmd run test:e2e:coverage` trong PowerShell. Khi không đặt biến, Playwright giữ mức mặc định. Tùy chọn này chỉ điều chỉnh số test chạy đồng thời, không đổi timeout, danh sách test hay coverage hard gate.

Playwright hiện kiểm tra các tương tác UI prototype bằng API giả, các luồng API-backed với response được intercept, và tích hợp đăng nhập thật qua Docker Development trên Compose. Test file `backend-demo-flows.spec.ts` dùng `page.route` để kiểm tra session, dữ liệu công thức, loading, lỗi và empty state; đây là kiểm thử Frontend, không chứng minh SQL seed hoặc xác thực Backend. Chỉ Docker Development chạy trình duyệt qua Backend/SQL Server thật và có thể làm bằng chứng tích hợp FE–BE.

`tests/integration/meal-plan-real-stack.spec.ts` kiểm tra FR-09/FR-33 trên cùng stack Compose: đăng nhập, thêm, reload, chặn trùng, chặn sửa/gỡ/chuyển mục của tài khoản khác, chuyển món và giữ khẩu phần, chặn chuyển ra ngoài tuần, đối chiếu năng lượng theo Ingredient/quy đổi thật, tải PDF/TXT, bảo vệ snapshot riêng và thay/gỡ tombstone. Test đối chiếu SQL rằng công thức nguồn cùng mục đã lưu vẫn còn; tự tạo ID/email riêng và dọn đúng dữ liệu của mình. File được suite `test:e2e:integration` nhận diện; sự hiện diện hoặc việc liệt kê test không chứng minh test đã pass. Cần chạy Docker Development trên commit của nhánh để có bằng chứng thực thi.
