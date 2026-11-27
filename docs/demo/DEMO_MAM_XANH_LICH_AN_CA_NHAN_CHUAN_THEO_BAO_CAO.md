# KỊCH BẢN THUYẾT TRÌNH DEMO MÂM XANH — LẬP LỊCH ĂN CÁ NHÂN

> **Phiên bản hiệu chỉnh:** 21/11/2026<br>
> **Cơ sở:** `AI-MEAL-PLAN-REPORT.md` và `Bao_cao_cong_thuc_tinh_nhu_cau_dinh_duong_Mam_Xanh(1).md`, kết hợp 14 ảnh màn hình demo đã cung cấp.<br>
> **Mục tiêu:** Nói tự nhiên, chính xác với nghiệp vụ và cách Backend hiện đang xử lý; tránh khẳng định AI làm những việc thực tế do Backend thực hiện.<br>
> **Thời lượng dự kiến:** khoảng 10–12 phút với đầy đủ phần giải thích và thao tác; có thể rút ngắn bằng cách lướt qua các màn hình trùng nội dung.<br>
> **Quy ước:** Chỉ đọc các đoạn **🎤 Lời nói**. Các dòng **🖱 Thao tác** và **⚠ Lưu ý** dành riêng cho người thuyết trình.

---

## 1. Mở đầu — Giới thiệu hành trình lập lịch ăn

**🖱 Thao tác:** Mở Mâm Xanh, chuẩn bị ở trang **Sở thích ăn uống**.

**🎤 Lời nói:**

“Kính thưa quý thầy cô, tiếp theo em xin demo luồng **một người dùng tự lập và quản lý lịch ăn cá nhân** trên ứng dụng Mâm Xanh.

Điểm chính của luồng này là người dùng có thể **tự chọn món cho từng bữa**, hoặc **sử dụng AI để hỗ trợ đề xuất thực đơn cho tuần hiện tại**. Dù dùng cách nào, người dùng vẫn là người quyết định món nào xuất hiện trong lịch ăn của mình.

Để ứng dụng hiểu nhu cầu cá nhân trước khi xếp lịch, em xin giới thiệu hai phần thiết lập: **sở thích ăn uống** và **hồ sơ dinh dưỡng**. Sau đó, em sẽ demo hai cách thêm món và cuối cùng là chức năng theo dõi dinh dưỡng theo lịch.”

## 2. Thiết lập sở thích ăn uống

**🖱 Thao tác — Ảnh 01:** Lần lượt trỏ vào bốn kiểu ăn chay, danh sách nguyên liệu cần tránh, nguyên liệu không thích và sở thích bổ sung. Không cần nhập lại dữ liệu nếu đã chuẩn bị.

**🎤 Lời nói:**

“Đầu tiên là **sở thích ăn uống**. Phần này được chia thành bốn nhóm.

Nhóm thứ nhất là **kiểu ăn chay**, gồm bốn lựa chọn: **thuần chay, chay có sữa, chay có trứng, và chay có cả trứng lẫn sữa**. Ứng dụng so sánh lựa chọn này với kiểu ăn chay mà tác giả gắn cho công thức.

Nhóm thứ hai là **nguyên liệu cần tránh hoặc dị ứng**, ví dụ đậu phộng, gluten hoặc những nguyên liệu người dùng cần kiêng. Đây là thông tin quan trọng khi hệ thống kiểm tra mức độ phù hợp của món ăn.

Nhóm thứ ba là **món hoặc nguyên liệu không thích**. Khác với nhóm cần tránh, đây là sở thích cá nhân: một món có nguyên liệu không thích có thể bị giảm ưu tiên khi đề xuất, chứ không mặc nhiên bị loại bỏ.

Cuối cùng là nhóm **sở thích bổ sung**, để người dùng ghi lại khẩu vị ẩm thực, thời gian nấu tối đa và độ khó mong muốn.

Điểm quan trọng ở đây là ứng dụng **phân biệt ràng buộc cần tránh với sở thích không thích**. Sự khác biệt này sẽ thể hiện rõ khi em demo chức năng thêm món thủ công và AI đề xuất thực đơn.”

**⚠ Lưu ý kỹ thuật:** Trong luồng **AI tạo lịch hiện tại**, ba trường *khẩu vị ẩm thực, thời gian nấu tối đa và độ khó* đã được lưu/đọc ở hồ sơ nhưng **chưa được nối vào bước lọc hoặc xếp hạng của `AiMenuService`**. Vì vậy, chỉ nói chúng là thông tin được ghi nhận; **không khẳng định AI đang tối ưu thực đơn dựa trên ba trường này** (báo cáo AI, mục 3.2; BUG-116).

## 3. Thiết lập hồ sơ và các chỉ tiêu dinh dưỡng tham khảo

**🖱 Thao tác — Ảnh 02:** Chuyển sang **Thông tin hồ sơ**; lần lượt trỏ vào ngày sinh, giới tính sinh học, chiều cao, cân nặng, mức vận động và mục tiêu dinh dưỡng. Sau đó chỉ vào BMI, năng lượng mỗi ngày và bảng 9 chỉ tiêu bên phải.

**🎤 Lời nói:**

“Phần thiết lập thứ hai là **hồ sơ dinh dưỡng**, dành cho người dùng trong độ tuổi hệ thống hỗ trợ, **từ đủ 18 đến 120 tuổi**.

Người dùng khai báo **ngày sinh, giới tính sinh học, chiều cao, cân nặng và mức vận động**. Ngoài ra, có thể chọn thêm **mục tiêu dinh dưỡng cá nhân** để định hướng việc đề xuất thực đơn. Khi lưu hồ sơ, người dùng cũng cần xác nhận đồng ý sử dụng những thông tin đã khai báo để phục vụ tính toán dinh dưỡng.

Từ chiều cao và cân nặng, ứng dụng tính **BMI — chỉ số khối cơ thể**, để người dùng tham khảo. Còn **năng lượng cần thiết ước tính mỗi ngày** không được tính trực tiếp từ BMI, mà sử dụng phương trình **EER**, nghĩa là *nhu cầu năng lượng ước tính*, dựa trên **tuổi, giới tính sinh học, chiều cao, cân nặng và mức vận động**.

Ví dụ, với hồ sơ đang hiển thị là **nữ, 29 tuổi, cao 165 cm, nặng 60 kg, vận động vừa**, hệ thống đưa ra **BMI khoảng 22,0** và nhu cầu năng lượng khoảng **2.326 kcal mỗi ngày**.

Từ những thông tin đó, ứng dụng hiển thị **chín chỉ tiêu dinh dưỡng tham khảo**, gồm: **năng lượng; chất đạm; carbohydrate, tức chất bột đường; chất béo; chất xơ; canxi; sắt; vitamin B12; và kẽm**.

Ở đây, chất đạm, carbohydrate và chất béo được biểu diễn theo **khoảng tham chiếu** dựa trên năng lượng ngày; còn chất xơ và các vi chất có mức tham khảo theo những quy tắc tuổi, giới tính của hệ thống.

Đây là **các chỉ số ước tính phục vụ lập kế hoạch ăn uống**, không phải kết quả đo lượng thức ăn người dùng đã tiêu thụ và cũng không thay thế tư vấn y tế. Những dữ liệu này sẽ hỗ trợ bước lập thực đơn ở phía sau.”

**⚠ Lưu ý kỹ thuật:**

- BMI dựa trên chiều cao và cân nặng; **mức vận động không tham gia công thức BMI**.
- **Chín chỉ tiêu** không phải “chín vi chất”: có **1 năng lượng + 3 chất đa lượng + 1 chất xơ + 4 vi chất**.
- Báo cáo dinh dưỡng ghi nhận **18–120 tuổi**, bao gồm đúng 18 tuổi; tuổi 18 dùng nhánh EER riêng. Không nói “phải trên 18 tuổi”.
- Hồ sơ, sự đồng ý sử dụng dữ liệu và **xác nhận đủ điều kiện sử dụng hỗ trợ dinh dưỡng** là những bước cần có trước khi tạo thực đơn AI (báo cáo AI, mục 3.1 và 6).

## 4. Mở lịch ăn — Hai cách sắp xếp món

**🖱 Thao tác — Ảnh 03:** Chọn **Lịch ăn**; chỉ vào tuần đang xem, các ngày và ba khung bữa ăn.

**🎤 Lời nói:**

“Sau khi có sở thích và hồ sơ dinh dưỡng, em chuyển sang chức năng chính là **Lịch ăn**.

Ứng dụng tổ chức lịch theo **từng tuần, mỗi tuần bảy ngày**, và mỗi ngày có **ba bữa chính: sáng, trưa và tối**. Người dùng có thể mở từng ngày để xem, thêm hoặc điều chỉnh các món theo nhu cầu.

Mâm Xanh hỗ trợ **hai cách lập lịch**. Cách thứ nhất là **thêm món thủ công**, phù hợp khi người dùng đã có món muốn ăn. Cách thứ hai là **AI hỗ trợ tạo thực đơn**, phù hợp khi người dùng muốn được gợi ý cách phân bổ món trong phần còn lại của tuần.

Em sẽ demo cách thủ công trước.”

## 5. Cách 1 — Thêm món thủ công và kiểm tra cảnh báo

**🖱 Thao tác — Ảnh 04, 06, 07, 08, 09, 10:** Ở một ngày/bữa, nhấn **Thêm món** → tab **Đã lưu** hoặc **Tìm công thức**. Chọn **Cơm gạo lứt chiên trứng rau củ** để làm xuất hiện cảnh báo. Nếu cần, mở trang chi tiết để chỉ ba nhóm cảnh báo; cảnh báo kiểu chay dựa trên loại công thức, còn cần tránh và không thích chỉ nguyên liệu liên quan. Ảnh cũ có thể chưa phản ánh giao diện sau thay đổi.

**🎤 Lời nói:**

“Đầu tiên, em chọn **bữa sáng** và nhấn **Thêm món**. Tại đây, người dùng có thể chọn từ những **công thức đã lưu** hoặc tìm một công thức khác trong hệ thống. Ngoài ra, người dùng cũng có thể tìm món từ trang **Khám phá công thức**, nơi hiển thị các công thức được đăng công khai.

Để minh họa cơ chế cảnh báo, em thử chọn món **Cơm gạo lứt chiên trứng rau củ**.

Tài khoản demo đang chọn kiểu **thuần chay**, khai báo **gạo lứt là nguyên liệu cần tránh** và **cà rốt là nguyên liệu không thích**. Vì thế, hệ thống phát hiện ba vấn đề:

**Thứ nhất**, món chứa **gạo lứt**, trùng với danh sách cần tránh. **Thứ hai**, tác giả xếp công thức vào loại **chay có trứng**, không phù hợp với hồ sơ thuần chay; công thức có thể chứa trứng. **Thứ ba**, món có **cà rốt**, là nguyên liệu người dùng không thích.

Các cảnh báo này được hiển thị riêng. Trang chi tiết đánh dấu nguyên liệu cần tránh và không thích; cảnh báo kiểu chay nói về loại công thức, không chỉ đích danh nguyên liệu. Việc so sánh phụ thuộc vào loại mà tác giả chọn, nên không tự phát hiện công thức bị gắn sai loại.

Trong thao tác thêm thủ công, nếu người dùng vẫn muốn tiếp tục, ứng dụng yêu cầu **xác nhận đã đọc và hiểu cảnh báo** trước khi thêm món. Như vậy, hệ thống hỗ trợ người dùng nhận biết những điểm không phù hợp, thay vì âm thầm bỏ qua thông tin đã khai báo.

Em lưu ý rằng **cảnh báo này không chứng nhận một món an toàn cho người bị dị ứng**; người dùng vẫn phải tự kiểm tra thành phần thực tế và những nguy cơ liên quan.”

**🎤 Chuyển ý:**

“Vừa rồi là cách lập lịch khi người dùng đã biết mình muốn ăn gì. Tiếp theo, em sẽ demo cách **AI hỗ trợ đề xuất thực đơn**, để giảm công sức phải tự tìm và sắp xếp từng món.”

**⚠ Phân biệt bắt buộc khi thầy hỏi:** Ở **luồng thủ công**, giao diện có thể **hiển thị cảnh báo và yêu cầu xác nhận để tiếp tục**. Nhưng ở **luồng AI tạo thực đơn**, Backend **loại** ứng viên xung đột kiểu chay hoặc nguyên liệu cần tránh trước khi gọi Gemini. Không mô tả hai luồng là cùng một cơ chế kiểm soát.

## 6. Cách 2 — AI hỗ trợ tạo thực đơn tuần

### 6.1. Yêu cầu AI tạo thực đơn

**🖱 Thao tác — Ảnh 11:** Quay lại **Lịch ăn**, mở khu vực tạo thực đơn bằng AI. Nếu giao diện có lựa chọn chế độ, chỉ vào **Dựa trên hồ sơ** và **Theo mong muốn tuần này**; sau đó nhấn nút tạo thực đơn khi sẵn sàng.

**🎤 Lời nói:**

“Ở cách thứ hai, người dùng có thể yêu cầu AI hỗ trợ **đề xuất thực đơn cho những ngày còn lại của tuần hiện tại**.

Người dùng có thể sử dụng mục tiêu đã lưu trong hồ sơ; hoặc, khi chọn chế độ **theo mong muốn tuần này**, có thể nhập mục tiêu và nguyên liệu muốn ưu tiên hay cần tránh riêng cho lần đề xuất đó. Những lựa chọn riêng cho tuần này **không tự động thay đổi hồ sơ lâu dài**.

Khi người dùng nhấn **Tạo thực đơn**, hệ thống không để AI tự nghĩ ra món ăn mới. Thay vào đó, **Backend lấy tối đa 50 công thức đang công khai**, rồi kiểm tra kiểu ăn chay, các nguyên liệu cần tránh và độ đầy đủ, tin cậy của dữ liệu dinh dưỡng.

Những món vi phạm kiểu ăn chay hoặc nguyên liệu cần tránh được **loại khỏi danh sách ứng viên**. Những món chứa nguyên liệu người dùng **không thích** thì có thể vẫn được xem xét, nhưng sẽ **bị giảm ưu tiên**.

Sau đó, ứng dụng gửi danh sách công thức hợp lệ cùng thông tin và mục tiêu dinh dưỡng đến **Google Gemini** để AI **xếp hạng các công thức sẵn có**. Kết quả AI trả về chỉ là **danh sách mã công thức theo thứ tự ưu tiên**. Chính **Backend** mới là phần kiểm tra lại danh sách, tính và đối chiếu dinh dưỡng, rồi **sắp xếp món vào các ngày và từng bữa ăn**.

Vì vậy, đây là mô hình **AI hỗ trợ lựa chọn kết hợp thuật toán của Backend**, chứ không phải AI tự sáng tác món ăn hoặc tự quyết định tất cả.”

### 6.2. Xem trước, điều chỉnh và áp dụng

**🖱 Thao tác — Ảnh 11, 12:** Chỉ vào bữa sáng/trưa/tối, tổng kcal của ngày, các checkbox lựa chọn món, ngày đã qua; có thể bỏ chọn một món minh họa. Sau đó nhấn **Áp dụng vào Lịch ăn** và hoàn tất bước xác nhận thứ hai nếu muốn ghi lịch trong tài khoản demo.

**🎤 Lời nói:**

“Sau khi hệ thống xử lý xong, người dùng được xem **bản thực đơn đề xuất trước khi lưu**.

Về năng lượng, ứng dụng hướng đến tỷ lệ **25% cho bữa sáng, 37,5% cho bữa trưa và 37,5% cho bữa tối**. Tỷ lệ này có **dung sai khoảng 5 điểm phần trăm cho mỗi bữa**, bởi món ăn được chọn từ những công thức có sẵn, chứ không phải lúc nào cũng ghép được đúng tuyệt đối.

Bên cạnh kcal, Backend còn đối chiếu các **chỉ tiêu dinh dưỡng tham khảo** để lựa chọn cách phối hợp món phù hợp hơn. Tuy nhiên, đây là **mục tiêu tối ưu**, không phải cam kết rằng ngày nào cũng đạt đầy đủ cả chín chỉ tiêu.

Trên màn hình, người dùng nhìn thấy **món ăn theo từng bữa, tổng dinh dưỡng dự kiến theo ngày**, và có thể **bỏ chọn những món mình chưa ưng ý** hoặc yêu cầu tạo một bản thực đơn khác.

Một điểm quan trọng là **những ngày đã qua được giữ nguyên**, hệ thống chỉ đề xuất cho **ngày hiện tại đến hết Chủ Nhật của tuần đang xem**.

Khi hài lòng, người dùng nhấn **Áp dụng vào Lịch ăn** và **xác nhận thêm một lần nữa**. Chỉ ở bước này, các món đã chọn mới chính thức được ghi vào lịch. Những mục lịch từ hôm nay đến hết tuần được **thay bằng lựa chọn vừa xác nhận**, còn lịch của các ngày trước đó vẫn được giữ nguyên.

Như vậy, **AI đưa ra phương án; người dùng xem xét và xác nhận; Backend kiểm tra lại trước khi ghi lịch**.”

**⚠ Lưu ý kỹ thuật:**

- Chỉ nói **“ngày đã qua”** được giữ nguyên. Báo cáo code mô tả thao tác thay các mục **từ hôm nay trở đi**; không tự khẳng định mọi **bữa đã trôi qua trong riêng ngày hôm nay** đều được bảo vệ.
- Preview (**bản xem trước**) **chưa phải lịch đã lưu**. Chỉ sau khi xác nhận và nhận phản hồi thành công từ API áp dụng mới kết luận đã ghi lịch.
- Với chế độ AI trên **trang Lịch ăn**, Gemini chỉ **xếp hạng ID công thức**; bộ `AiMenuOptimizer` ở Backend mới ghép ngày/bữa.
- Chức năng này phụ thuộc vào điều kiện hồ sơ, sự đồng ý, trạng thái đủ điều kiện, quota và kết nối AI. Nếu thiếu cấu hình `GEMINI_API_KEY`, không có thực đơn giả được tự sinh ra (báo cáo AI, mục 5–6).

## 7. Theo dõi dinh dưỡng của lịch đã lập

**🖱 Thao tác — Ảnh 05, 13, 14:** Mở **Dinh dưỡng theo lịch ăn**. Cho xem tab **Theo ngày**, sau đó chuyển **So sánh & Thống kê 7 ngày**. Nếu cần, chỉ vào tab **Xuất báo cáo PDF**, nhưng không cần đi sâu nếu không có thời gian.

**🎤 Lời nói:**

“Sau khi đã lập lịch, người dùng có thể **kiểm tra lại chất lượng dinh dưỡng dự kiến của thực đơn**.

Ở tab **Theo ngày**, hệ thống cộng dinh dưỡng từ các công thức và khẩu phần đã được xếp vào lịch, rồi **so sánh với các mức tham khảo trong hồ sơ cá nhân**. Từ đó, người dùng biết ngày nào đang thấp hơn hoặc cao hơn các mức tham khảo và có thể quay lại thay đổi món.

Tiếp theo, ở tab **So sánh và Thống kê 7 ngày**, người dùng xem được số liệu theo từng ngày, **giá trị trung bình của tuần** và **tỷ lệ số ngày đạt mức tham khảo của từng chỉ tiêu**.

Ví dụ, nếu bảng cho thấy **vitamin B12** đang thấp hơn mức tham khảo trong nhiều ngày, người dùng có thêm cơ sở để xem xét và điều chỉnh kế hoạch ăn uống.

Em nhấn mạnh rằng những số liệu này được tính từ **món và khẩu phần đã lên lịch**, nên là **dinh dưỡng dự kiến**, không phải hệ thống ghi nhận người dùng đã thật sự ăn gì. Chức năng thống kê nhằm hỗ trợ **đánh giá và điều chỉnh kế hoạch**, chứ không phải chẩn đoán tình trạng sức khỏe.”

## 8. Kết thúc

**🖱 Thao tác:** Quay lại màn hình tổng quan lịch ăn trong tuần.

**🎤 Lời nói:**

“Như vậy, qua luồng demo vừa rồi, em đã trình bày hành trình một người dùng từ **khai báo sở thích**, **thiết lập hồ sơ dinh dưỡng**, **tự thêm món hoặc nhận đề xuất từ AI**, đến **xem thống kê và điều chỉnh lịch ăn**.

Giá trị chính mà Mâm Xanh hướng đến là **giúp người dùng ăn chay chủ động lập kế hoạch bữa ăn**, nhận biết những nguyên liệu không phù hợp với hồ sơ của mình, đồng thời có thêm thông tin dinh dưỡng tham khảo để lựa chọn món ăn.

**Phần demo luồng lập lịch ăn cá nhân của em xin kết thúc tại đây. Em xin cảm ơn quý thầy cô đã lắng nghe.**”

---

# PHỤ LỤC A — BẢNG ĐỐI CHIẾU 14 ẢNH VỚI KỊCH BẢN

| Ảnh | Nội dung màn hình | Phần nói/việc cần làm |
|---|---|---|
| 01 | Sở thích ăn uống | Mục 2: 4 kiểu chay, 2 nhóm nguyên liệu, sở thích bổ sung |
| 02 | Hồ sơ và chỉ số tham khảo | Mục 3: BMI, EER, 9 chỉ tiêu dinh dưỡng |
| 03 | Lịch ăn theo tuần | Mục 4: 7 ngày và 3 bữa chính mỗi ngày |
| 04 | Hộp thoại thêm món | Mục 5: tìm trong danh sách đã lưu hoặc công thức |
| 05 | Thống kê dinh dưỡng theo ngày (trạng thái ban đầu) | Có thể lướt qua; giải thích chung với mục 7 để tránh lặp |
| 06 | Khám phá công thức | Mục 5: nguồn món công khai và thao tác lưu/tìm món |
| 07 | Cảnh báo khi thêm món | Mục 5: thông báo xung đột và xác nhận nếu muốn tiếp tục |
| 08 | Trang chi tiết công thức với 3 cảnh báo | Mục 5: cần tránh, không phù hợp kiểu chay, không thích |
| 09 | Đầu trang chi tiết công thức | Minh họa bổ sung của ảnh 08; không cần dừng lâu |
| 10 | Danh sách nguyên liệu, dinh dưỡng mỗi khẩu phần | Mục 5: từng nguyên liệu được đánh dấu, số liệu khẩu phần |
| 11 | Bản xem trước do AI hỗ trợ tạo | Mục 6: món theo ngày/bữa; xem/bỏ chọn; dinh dưỡng dự kiến |
| 12 | Phần cuối bản xem trước và nút áp dụng | Mục 6: giữ ngày đã qua; xác nhận mới ghi lịch |
| 13 | Dinh dưỡng theo lịch ăn — Theo ngày | Mục 7: lượng dự kiến so với mức tham khảo |
| 14 | So sánh & Thống kê 7 ngày | Mục 7: giá trị từng ngày, trung bình tuần, tỷ lệ ngày đạt |

**Trình tự thao tác nên dùng khi demo:** 01 → 02 → 03 → 04 → 07 → 08 → 10 → 06 (nếu cần) → 11 → 12 → 13 → 14. Hai ảnh 05 và 09 có thể bỏ qua khi thời gian ngắn.

---

# PHỤ LỤC B — NHỮNG CHỖ ĐÃ SỬA SO VỚI LỜI THUYẾT TRÌNH BAN ĐẦU

| Cách nói ban đầu / dễ nói sai | Cách nói đúng theo hai báo cáo |
|---|---|
| “Người dùng phải lớn hơn 18 tuổi.” | **Từ đủ 18 đến 120 tuổi**; người đúng 18 tuổi có nhánh công thức EER riêng. |
| “Chiều cao, cân nặng, vận động ảnh hưởng đến BMI.” | BMI tính từ **chiều cao và cân nặng**. Tuổi, giới tính, chiều cao, cân nặng, vận động tham gia tính **EER**. |
| “Có 9 vi chất.” | Có **9 chỉ tiêu dinh dưỡng**: 1 năng lượng, 3 chất đa lượng, 1 chất xơ, 4 vi chất. |
| “AI lấy BMI tính ra kcal.” | Kcal ước tính theo **EER**; BMI là thông tin tham khảo, **không trực tiếp đặt/chỉnh mục tiêu kcal**. |
| “Các sở thích khác quyết định AI đề xuất món nấu dưới 30 phút, dễ làm.” | Các trường khẩu vị, thời gian, độ khó **đã được lưu nhưng chưa kết nối trực tiếp** với AI tạo lịch hiện tại. |
| “Món không thích sẽ không được AI đề xuất.” | **Nguyên liệu cần tránh/kiểu chay** là ràng buộc cứng; **không thích** là ưu tiên mềm. |
| “AI tự tạo và sắp xếp món ăn, tự tính vi chất.” | Gemini **xếp hạng ID** của công thức hợp lệ; Backend lọc, tính từ catalog và tối ưu ghép bữa. |
| “AI có thể chọn món tùy ý trên mạng.” | Hệ thống chỉ lấy tối đa **50 công thức công khai** từ catalog nội bộ để lập tập ứng viên. |
| “AI đảm bảo đủ kcal và 9 chỉ tiêu.” | Hệ thống **tối ưu hướng tới** mục tiêu, không bảo đảm đạt tuyệt đối; tỷ lệ bữa có dung sai **±5 điểm phần trăm**. |
| “AI không sửa các bữa đã qua trong ngày hôm nay.” | **Những ngày trước hôm nay** được giữ; khi áp dụng, dữ liệu **từ hôm nay trở đi** có thể được thay thế. |
| “AI vừa đề xuất là lịch đã lưu.” | Preview chưa lưu; người dùng **bỏ/chọn và xác nhận áp dụng** mới ghi vào lịch. |
| “Thống kê là những gì người dùng đã ăn.” | Là số liệu **dự kiến từ các món/khẩu phần đã lên lịch**, không phải lượng tiêu thụ thực tế. |

---

# PHỤ LỤC C — CÁC CÂU THẦY CÔ CÓ THỂ HỎI (TRẢ LỜI NGẮN)

### C1. BMI có phải là công thức tính kcal mỗi ngày không?

**Không.** BMI = cân nặng (kg) / chiều cao (m)². Nhu cầu kcal dùng **EER — nhu cầu năng lượng ước tính** theo tuổi, giới tính sinh học, chiều cao, cân nặng và mức vận động.

### C2. Vì sao ví dụ nữ 29 tuổi, cao 165 cm, nặng 60 kg, vận động vừa ra khoảng 2.326 kcal?

Theo phương trình cho nữ vận động vừa:<br>
`EER = 710,25 − 7,01 × 29 + 6,54 × 165 + 12,34 × 60 = 2.326,46 kcal/ngày.`<br>
Giá trị trên màn hình được làm tròn thành khoảng **2.326 kcal/ngày**.

### C3. Chín chỉ tiêu dinh dưỡng được tính như thế nào?

Năng lượng theo **EER**. Ba chất đa lượng theo tỷ lệ năng lượng: **đạm 10–35%**, **carbohydrate 45–65%**, **chất béo 20–35%**; quy đổi thành gram với 4/4/9 kcal mỗi gram tương ứng. Chất xơ, canxi, sắt, vitamin B12 và kẽm áp dụng những mức tham chiếu theo tuổi/giới tính hoặc mức cố định theo logic hiện tại.

### C4. Gemini đóng vai trò gì, khác gì so với Backend?

Backend **lọc công thức và tính dinh dưỡng** từ dữ liệu có sẵn. Gemini **xếp hạng mã công thức hợp lệ**. Backend **kiểm tra lại** và bộ tối ưu **ghép món vào bữa/ngày**. Gemini không tạo công thức mới, không tính số liệu dinh dưỡng cuối cùng và không trực tiếp ghi lịch.

### C5. Nếu người dùng không thích cà rốt, AI có loại mọi món chứa cà rốt không?

**Không nhất thiết.** Nguyên liệu **không thích** là **ưu tiên mềm**, làm món bị giảm ưu tiên. Kiểu ăn chay và nguyên liệu **cần tránh** mới là điều kiện lọc cứng trong luồng AI.

### C6. Tại sao có cảnh báo nhưng thao tác thủ công vẫn cho xác nhận thêm món?

Luồng **thêm thủ công** có bước thông báo, yêu cầu người dùng thừa nhận cảnh báo rồi tự quyết định có tiếp tục hay không. Luồng **AI tạo thực đơn** áp dụng lọc cứng trước để không đưa món vi phạm vào tập công thức AI xem xét. Cảnh báo dị ứng không đồng nghĩa với xác nhận an toàn y khoa.

### C7. AI có bảo đảm sáng đúng 25%, trưa tối mỗi bữa đúng 37,5% không?

**Không.** Đó là mục tiêu phân bổ năng lượng, có **dung sai ±5 điểm phần trăm cho từng bữa**. Do ứng dụng chọn từ công thức hữu hạn, có ngày vẫn chênh lệch mục tiêu hoặc chưa đạt đủ chỉ tiêu dinh dưỡng.

### C8. Khi nào thực đơn AI thật sự được ghi vào lịch?

Sau khi người dùng xem trước, chọn/bỏ món, nhấn **Áp dụng vào Lịch ăn**, xác nhận thêm một lần và Backend kiểm tra thành công. Lúc đó lịch từ **hôm nay đến hết tuần** được thay bằng các món đã chọn; ngày đã qua được giữ nguyên.

### C9. Có cần trả phí Plus/Pro để sử dụng AI tạo thực đơn không?

Theo **FR-36/BR-31 và code Service hiện hành được báo cáo đối chiếu**, chức năng không khóa cứng theo Plus/Pro. Tuy nhiên, vẫn có kiểm tra **quyền Member, hồ sơ hợp lệ, trạng thái đủ điều kiện và quota**. Báo cáo ghi nhận mô tả OpenAPI cũ không thống nhất ở điểm này (BUG-115).

### C10. Nếu không có API key hoặc Gemini không phản hồi thì sao?

Luồng này **không sinh thực đơn giả**. Thiếu `GEMINI_API_KEY` dẫn đến lỗi provider chưa được cấu hình; lỗi model hoặc dữ liệu trả về không hợp lệ cũng có thể khiến preview thất bại.

### C11. Tại sao gọi đây là số liệu dinh dưỡng dự kiến?

Vì Backend tính từ **định lượng nguyên liệu của các công thức đã lên lịch**, không có bằng chứng rằng người dùng thực sự nấu hoặc ăn đúng các khẩu phần đó.

---

# PHỤ LỤC D — CHECKLIST CẦN KIỂM TRA TRƯỚC BUỔI DEMO

- [ ] **Tài khoản:** Member đăng nhập được; hồ sơ dinh dưỡng đã lưu, đã đồng ý dùng dữ liệu và xác nhận đủ điều kiện hỗ trợ dinh dưỡng.
- [ ] **Sở thích:** Có sẵn kiểu thuần chay; gạo lứt trong nguyên liệu cần tránh; cà rốt trong danh sách không thích; món demo chứa trứng, gạo lứt, cà rốt.
- [ ] **Kiểu ăn & cảnh báo:** Thử trước hộp thoại cảnh báo 3 nhóm; kiểm tra nội dung xác nhận hoạt động và không dùng tài khoản thật có vấn đề dị ứng.
- [ ] **AI:** Kiểm tra API key/kết nối provider, quota, nguồn công thức công khai đủ dữ liệu; thử tạo preview **thật** trước buổi trình bày.
- [ ] **Phạm vi tuần:** Demo trên **tuần hiện tại**; những ngày đã qua hiển thị giữ nguyên và không được AI đề xuất mới.
- [ ] **Tài khoản dữ liệu:** Lưu ý **Áp dụng vào Lịch ăn** thay nội dung từ hôm nay đến cuối tuần; dùng dữ liệu demo có thể ghi đè.
- [ ] **Thống kê:** Chuẩn bị ngày có lịch để thấy số liệu; kiểm tra tab Theo ngày và Thống kê 7 ngày.
- [ ] **Hiển thị:** Khắc phục/tối thiểu giảm thanh cuộn ngang ở hộp thoại bằng độ zoom và kích thước màn hình thích hợp.
- [ ] **Đặc biệt — số liệu giữa ảnh chưa thống nhất:** Ảnh hồ sơ cho thấy mức năng lượng **≈ 2.326 kcal/ngày**, trong khi một số ảnh lịch/AI thống kê đang dùng mốc **≈ 2.502,61 kcal/ngày**. Có thể là các trạng thái dữ liệu hoặc lần tính khác nhau; **hai báo cáo không xác nhận nguyên nhân**. Trước khi demo, hãy mở cùng tài khoản, lưu/tính lại hồ sơ và đối chiếu các mốc hiển thị. Nếu chưa khớp, **không khẳng định các hình đang dùng chung một mục tiêu kcal**.
- [ ] **Lời nói:** Không dùng cụm “9 vi chất”, “BMI quyết định kcal”, “Gemini tự tính dinh dưỡng”, “AI bảo đảm đủ dưỡng chất” hoặc “AI đang lọc theo thời gian nấu/độ khó”.

---

# NGUỒN ĐỐI CHIẾU ĐỂ KIỂM CHỨNG NỘI DUNG

1. **`AI-MEAL-PLAN-REPORT.md`** — các mục 2, 3, 4, 6, 7, 8 và 10. Báo cáo đối chiếu code ngày 21/11/2026, mô tả việc lọc tối đa 50 công thức, Gemini xếp hạng ID, Backend tối ưu ghép bữa, quy trình preview/apply, các ràng buộc và giới hạn đã xác minh.
2. **`Bao_cao_cong_thuc_tinh_nhu_cau_dinh_duong_Mam_Xanh(1).md`** — các mục 1–8. Báo cáo mô tả EER, ví dụ 2.326,46 kcal, khoảng các chất đa lượng, chín chỉ tiêu dinh dưỡng và độ tuổi 18–120; lưu ý chính báo cáo này nói rằng nó **không thay thế kiểm tra trực tiếp mã nguồn hiện hành**.
3. **14 ảnh màn hình Mâm Xanh** trong yêu cầu demo ngày 21/11/2026 — dùng để đối chiếu tên nút, thứ tự màn hình, phần cảnh báo, preview và các tab thống kê.

**Phạm vi xác nhận:** Đây là kịch bản được chỉnh theo hai tài liệu đã gửi và các ảnh hiện có; **không phải kết quả chạy lại Backend/Gemini**. Các câu mô tả hoạt động code kế thừa mức kiểm chứng và giới hạn mà chính hai báo cáo đã nêu.
