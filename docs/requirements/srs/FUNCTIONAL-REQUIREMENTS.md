> **Document:** Functional Requirements Specification
> **File:** `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md`
> **Version:** v0.5.0
> **Created:** 2026-09-14
> **Last Updated:** 2026-09-15
> **Status:** Draft
> **Related Docs:** `docs/requirements/SRS.md`, `docs/requirements/srs/BUSINESS-RULES.md`, `docs/requirements/srs/NON-FUNCTIONAL-REQUIREMENTS.md`

# Functional Requirements Specification

## 1. Mục đích và thẩm quyền tài liệu

Tài liệu này là **Authoritative Detailed Specification** sở hữu các định nghĩa chi tiết cho các Functional Requirements (`FR-01` đến `FR-56`) của hệ thống. Các yêu cầu ACTIVE được phân rã chi tiết dần qua từng đợt (progressively decomposed) trong giai đoạn xác định yêu cầu hiện tại.

Khung đặc tả gốc và **Authoritative Registry** cho sự tồn tại của requirement, mã định danh ổn định (stable ID), phân bổ module cấp chỉ mục, và trạng thái vòng đời (lifecycle state) chính thức được duy trì tập trung tại `docs/requirements/SRS.md`.

Trạng thái hiển thị trong tài liệu này là giá trị dẫn xuất (derived) từ root registry `SRS.md`. Nếu phát sinh bất kỳ xung đột nào về trạng thái lifecycle, giá trị trong `SRS.md` luôn là chuẩn có thẩm quyền cao nhất; tài liệu này sẽ được đồng bộ hóa theo `SRS.md`.

Mỗi yêu cầu chức năng được gắn một thẻ stable anchor HTML cố định `<a id="fr-xx"></a>` đặt trước tiêu đề để đảm bảo tính bất biến của liên kết tham chiếu, không phụ thuộc vào việc tiêu đề yêu cầu có thể thay đổi hoặc được dịch nghĩa trong tương lai.

## 2. Quy tắc lifecycle trong baseline hiện tại

Vocabulary duy nhất dùng cho requirement lifecycle là `DRAFT`, `ACTIVE`, `DEFERRED`, `OUT_OF_SCOPE` và `RETIRED`. Các trạng thái derived dưới đây đã được đồng bộ từ root registry theo DEC-001–DEC-003; lifecycle và mức độ decomposition là hai chiều độc lập.

Không còn FR mang legacy/unknown lifecycle wording. Nội dung `DEFERRED`, `OUT_OF_SCOPE` và `RETIRED` được giữ để bảo toàn lịch sử nhưng không tạo MVP implementation scope.

## 3. Danh mục chi tiết Functional Requirements

<a id="fr-01"></a>
### FR-01 — Hệ thống cho Guest xem và tìm kiếm nội dung đã công khai

- **Mã yêu cầu:** FR-01
- **Module:** M01 (Guest & Public Browsing)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống cho phép người dùng chưa đăng nhập (Guest) duyệt xem danh sách, tìm kiếm và xem chi tiết toàn bộ các bài công thức chay đã công khai; không yêu cầu đăng nhập đối với việc đọc nội dung; khi Guest bấm vào các tác vụ cá nhân hóa hoặc tương tác thành viên thì hệ thống yêu cầu đăng nhập.

#### 1. Mục đích
Tạo phễu tiếp cận mở rộng nhằm giới thiệu kiến thức ẩm thực chay và lan tỏa phong cách sống tích cực tới cộng đồng; cho phép người dùng trải nghiệm giá trị cốt lõi của nền tảng trước khi đăng ký tài khoản; đồng thời thiết lập ranh giới phân quyền rõ ràng giữa việc xem nội dung công khai và việc tương tác cộng đồng/cá nhân hóa.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Guest`: Người dùng vãng lai chưa xác thực truy cập ứng dụng để tìm và xem công thức nấu ăn.
- **Secondary Actor / External System:**
  - `Hệ thống phân phối nội dung công khai`: Đảm bảo tải dữ liệu nhanh chóng và an toàn.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-01.1`: Duyệt danh sách bài công thức công khai (Browse public recipes).
  - `UC-01.2`: Tìm kiếm công thức cơ bản theo từ khóa (Search public recipes by keyword).
  - `UC-01.3`: Xem chi tiết bài công thức công khai (View public recipe details).
- **User Stories:**
  - `US-01.1`: Là một Guest, tôi muốn lướt xem các món chay hấp dẫn trên trang chủ để tìm cảm hứng nấu ăn hàng ngày mà không bị bắt buộc phải tạo tài khoản trước.
  - `US-01.2`: Là một Guest, tôi muốn nhập từ khóa để tìm nhanh món ăn tôi đang quan tâm nhằm tham khảo nguyên liệu và cách làm.
  - `US-01.3`: Là một Guest, khi tôi bấm vào nút Thích hoặc Lưu bài, tôi muốn hệ thống giải thích rõ ràng đây là tính năng dành cho thành viên và hướng dẫn tôi đăng nhập/đăng ký.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Bài công thức đang ở trạng thái công khai (`PUBLISHED`) và không bị ẩn hoặc xóa do vi phạm quy tắc cộng đồng.
- **Trigger:** Guest truy cập vào trang web, sử dụng ô tìm kiếm, hoặc nhấp vào một bài công thức từ danh sách.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Duyệt danh sách công thức công khai (UC-01.1)
1. **Main Flow:**
   - Bước 1: Guest truy cập trang chủ hoặc trang Khám phá.
   - Bước 2: Hệ thống truy xuất và hiển thị danh sách các bài công thức công khai mới nhất kèm phân trang. Mỗi thẻ công thức hiển thị: ảnh đại diện món ăn, tiêu đề, loại ăn chay chuẩn, thời gian nấu, và thông tin tác giả.
   - Bước 3: Guest có thể cuộn trang hoặc bấm chuyển trang để xem thêm các công thức khác.

##### B. Luồng Tìm kiếm công thức cơ bản (UC-01.2)
1. **Main Flow:**
   - Bước 1: Guest nhập từ khóa vào thanh tìm kiếm (ví dụ: "nấm kho", "đậu phụ") và nhấn Enter hoặc biểu tượng tìm kiếm.
   - Bước 2: Hệ thống tìm kiếm trên tiêu đề và mô tả ngắn của các bài công thức công khai.
   - Bước 3: Hệ thống trả về danh sách kết quả phù hợp với thời gian phản hồi nhanh ($\le 1.5$ giây theo NFR-02).
2. **Alternative Flow (Không tìm thấy kết quả):**
   - Nếu không có bài viết nào khớp với từ khóa, hệ thống hiển thị thông báo "Không tìm thấy công thức nào phù hợp với từ khóa của bạn" và gợi ý các từ khóa thịnh hành hoặc danh mục phổ biến.

##### C. Luồng Xem chi tiết bài công thức (UC-01.3)
1. **Main Flow:**
   - Bước 1: Guest nhấp vào một thẻ bài viết từ danh sách.
   - Bước 2: Hệ thống hiển thị toàn bộ nội dung chi tiết của bài viết: tiêu đề, ảnh, video hướng dẫn (nếu có), khẩu phần, thời gian nấu, nguyên liệu theo định lượng, các bước nấu, thông tin tác giả và tổng số lượt Thích.
2. **Security & Exception Flow (Yêu cầu đăng nhập khi thực hiện hành động thành viên):**
   - Bước 1: Khi đang ở trang chi tiết bài viết hoặc danh sách, Guest nhấp vào các nút tương tác: "Thích", "Bình luận", "Lưu công thức", hoặc "Thêm vào lịch ăn".
   - Bước 2: Hệ thống nhận diện trạng thái chưa đăng nhập của Guest.
   - Bước 3: Hệ thống hiển thị hộp thoại thông báo: "Vui lòng đăng nhập hoặc tạo tài khoản để thực hiện chức năng này" kèm nút bấm điều hướng tới trang Đăng nhập / Đăng ký (BR-05, BR-32).

#### 6. Hậu điều kiện (Postconditions)
- Nội dung công thức công khai được hiển thị đầy đủ cho Guest.
- Không phát sinh phiên đăng nhập người dùng hay thay đổi dữ liệu trên máy chủ.

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Guest được phép xem toàn bộ các bài công thức ở trạng thái `PUBLISHED`.
- Hệ thống tuyệt đối không hiển thị cho Guest các bài viết đang ở trạng thái nháp (nếu có sau này), bài bị Quản trị viên ẩn do vi phạm, hoặc bài đã xóa.
- Không cho phép Guest tạo, chỉnh sửa, xóa bài viết, like, gửi bình luận hay lưu dữ liệu cá nhân hóa (BR-05).

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian phản hồi tìm kiếm công thức $\le 1.5$ giây (P95).
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn truy cập trái phép.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa thiết bị.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-01.1 — Guest duyệt danh sách bài công thức công khai thành công:**
  - **Given:** Guest truy cập trang Khám phá mà không đăng nhập.
  - **When:** Trang web tải hoàn tất.
  - **Then:** Hệ thống hiển thị danh sách các bài công thức đang công khai kèm tiêu đề, ảnh, loại ăn chay và tác giả; có hỗ trợ phân trang.

- **AC-01.2 — Guest tìm kiếm bài công thức theo từ khóa thành công:**
  - **Given:** Guest đang ở thanh tìm kiếm của ứng dụng.
  - **When:** Guest nhập từ khóa "canh chua" và gửi yêu cầu tìm kiếm.
  - **Then:** Hệ thống phản hồi danh sách các bài công thức chứa từ khóa "canh chua" với thời gian phản hồi $\le 1.5$ giây (P95).

- **AC-01.3 — Guest xem đầy đủ nội dung chi tiết bài công thức:**
  - **Given:** Một bài công thức hợp lệ đang ở trạng thái công khai (`PUBLISHED`).
  - **When:** Guest nhấp vào xem chi tiết bài công thức đó.
  - **Then:** Hệ thống hiển thị toàn bộ nội dung bao gồm tiêu đề, nguyên liệu, định lượng, các bước nấu, ảnh và thông tin tác giả.

- **AC-01.4 — Không hiển thị bài viết vi phạm hoặc bài không công khai cho Guest:**
  - **Given:** Bài công thức đang bị Quản trị viên ẩn do vi phạm hoặc không tồn tại.
  - **When:** Guest cố tình truy cập bài viết đó qua đường dẫn trực tiếp.
  - **Then:** Hệ thống từ chối hiển thị và thông báo bài viết không khả dụng hoặc không tìm thấy.

- **AC-01.5 — Yêu cầu đăng nhập khi Guest thực hiện thao tác thành viên:**
  - **Given:** Guest đang xem một bài công thức công khai.
  - **When:** Guest nhấn nút "Lưu bài viết" hoặc "Thích bài viết".
  - **Then:** Hệ thống không thực hiện lưu hay tăng like, mà hiển thị thông báo yêu cầu đăng nhập và cung cấp liên kết chuyển đến màn hình Đăng nhập (BR-05, BR-32).

---

<a id="fr-02"></a>
### FR-02 — Quyền Guest trải nghiệm AI Chatbot chung theo Guest Free quota

- **Mã yêu cầu:** FR-02
- **Module:** M06 (AI Assistant & Personalization)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống quản lý quyền và trải nghiệm của Guest khi tương tác với AI Chatbot chung của hệ thống ([FR-51](FUNCTIONAL-REQUIREMENTS.md#fr-51)) theo hạn mức Guest Free:
  - **Hạn mức:** Tối đa 5 request AI thành công mỗi ngày, tự động đặt lại về 0 vào lúc 00:00 `Asia/Ho_Chi_Minh` (BR-01).
  - **Nhận diện & Phòng ngừa lạm dụng:** Guest được nhận diện bằng anonymous cookie kết hợp coarse IP protection theo quy tắc của BR-01.
  - **Quy tắc trừ hạn mức:** Chỉ request AI hoàn tất thành công mới tiêu thụ quota; request lỗi mạng, gián đoạn kết nối hoặc timeout từ phía AI provider tuyệt đối không trừ lượt (BR-03, BR-04).
  - **Ngữ cảnh sử dụng:** Guest được trải nghiệm AI Chatbot FR-51 ở cả Ngữ cảnh chung (General Context) và Ngữ cảnh bài công thức (Recipe Context theo [FR-20](FUNCTIONAL-REQUIREMENTS.md#fr-20)) đối với các Recipe Post đang công khai, dùng chung hạn ngạch 5 lượt thành công/ngày.
  - **Giới hạn trải nghiệm:** Guest không có lịch sử hội thoại lưu theo tài khoản và không có hồ sơ dinh dưỡng cá nhân (BR-05).
  - **Toàn bộ năng lực và luồng tương tác:** Hành vi hội thoại, áp dụng chỉ dẫn an toàn và phản hồi câu hỏi do [FR-51](FUNCTIONAL-REQUIREMENTS.md#fr-51) sở hữu và điều phối; thời gian phản hồi của AI Chatbot phải đáp ứng [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03).

#### 1. Mục đích
Thiết lập chính sách phân quyền và hạn mức trải nghiệm cho người dùng vãng lai trên cùng một AI Chatbot duy nhất của hệ thống (FR-51); chứng minh năng lực hỗ trợ thông minh của nền tảng nhằm thu hút người dùng đăng ký tài khoản thành viên chính thức; đồng thời kiểm soát chi phí vận hành API trí tuệ nhân tạo và ngăn ngừa hành vi lạm dụng tự động.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Guest`: Người dùng chưa xác thực trải nghiệm tương tác với AI Chatbot chung (FR-51).
- **Secondary Actor / External System:**
  - `Google Gemini AI`: Dịch vụ trí tuệ nhân tạo xử lý và phản hồi câu hỏi thông qua FR-51.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-02.1`: Trải nghiệm dùng thử AI Chatbot chung cho Guest theo hạn mức Guest Free (Try unified AI chatbot as guest).
- **User Stories:**
  - `US-02.1`: Là một Guest đang tìm hiểu về ăn chay hoặc xem một bài công thức công khai, tôi muốn hỏi thử AI vài câu (về nấu nướng chung hoặc về món ăn đang xem) để trải nghiệm chất lượng hỗ trợ trước khi quyết định đăng ký tài khoản.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Thiết bị của Guest có hỗ trợ cookie trình duyệt; số lượt AI thành công trong ngày của định danh Guest chưa vượt quá 5 lượt (BR-01).
- **Trigger:** Guest mở cửa sổ AI Chatbot chung hoặc nhấn nút "Hỏi AI về công thức này" khi đang xem bài công thức công khai (FR-20) và gửi câu hỏi.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Dùng thử Chatbot AI cho Guest (UC-02.1)
1. **Main Flow:**
   - Bước 1: Guest mở hộp thoại AI Chatbot FR-51 trên trang web (ở ngữ cảnh chung hoặc ngữ cảnh Recipe Post công khai đang xem).
   - Bước 2: Hệ thống kiểm tra anonymous cookie của trình duyệt (nếu chưa có, máy chủ tạo cookie định danh ẩn danh mới) kết hợp kiểm tra địa chỉ IP thô theo BR-01.
   - Bước 3: Hệ thống xác nhận số lượt gọi AI thành công trong ngày của định danh này đang $< 5$ (ví dụ: đang ở lượt thứ 2/5).
   - Bước 4: Guest gửi câu hỏi. Hệ thống kiểm soát tính hợp lệ của đầu vào và chuyển yêu cầu tới quy trình xử lý hội thoại của FR-51.
   - Bước 5: Dịch vụ AI phản hồi kết quả hợp lệ đáp ứng thời gian quy định tại NFR-03.
   - Bước 6: Hệ thống tăng bộ đếm lượt thành công trong ngày lên 1 (ví dụ lên 3/5 theo BR-03), hiển thị câu trả lời và thông báo số lượt dùng thử còn lại ("Bạn còn 2/5 lượt dùng thử hôm nay").
2. **Alternative Flow (Hết lượt dùng thử 5/5):**
   - Khi bộ đếm đã đạt 5/5 lượt thành công trong ngày: hệ thống khóa ô nhập câu hỏi, không gửi yêu cầu tới dịch vụ AI, hiển thị thông báo: "Bạn đã sử dụng hết 5 lượt dùng thử AI hôm nay. Hãy đăng ký tài khoản thành viên để lưu lịch sử, quản lý lịch ăn và nâng cấp gói Plus hoặc Pro!" kèm nút Đăng ký tài khoản (BR-01, BR-05).
3. **Exception Flow (Gián đoạn dịch vụ AI bên ngoài hoặc timeout):**
   - Nếu dịch vụ AI gặp sự cố kỹ thuật hoặc quá thời gian phản hồi theo quy định -> Hệ thống hiển thị thông báo lỗi thân thiện và **TUYỆT ĐỐI KHÔNG TRỪ LƯỢT DÙNG THỬ** của Guest (BR-03, BR-04, NFR-18).

#### 6. Hậu điều kiện (Postconditions)
- Số lượt gọi AI thành công của định danh Guest được ghi nhận chính xác theo BR-01 và BR-03.
- Bộ đếm tự động đặt lại về 0 vào lúc 00:00 `Asia/Ho_Chi_Minh`.

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Nhận diện Guest qua anonymous cookie kết hợp coarse IP protection theo BR-01 nhằm ngăn ngừa hành vi lạm dụng tự động.
- Dữ liệu đầu vào của câu hỏi được kiểm soát và làm sạch theo tiêu chuẩn an toàn bảo mật (NFR-10).

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Hạn mức text AI cho Guest và tài khoản Free (5 lượt/ngày, anonymous cookie và coarse IP).
  - [BR-03](BUSINESS-RULES.md#br-03): Điều kiện trừ hạn mức AI (chỉ trừ khi thành công).
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider và timeout AI (không trừ lượt).
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03): Thời gian phản hồi của AI Chatbot.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến và kiểm soát đầu vào.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt.
  - [NFR-18](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-18): Cơ chế dự phòng khi dịch vụ Gemini AI bị lỗi hoặc timeout.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-02.1 — Guest dùng thử AI thành công khi còn lượt:**
  - **Given:** Guest chưa sử dụng hết 5 lượt AI trong ngày.
  - **When:** Guest gửi câu hỏi hợp lệ tới AI Chatbot FR-51 (ở ngữ cảnh chung hoặc ngữ cảnh Recipe Post đang xem).
  - **Then:** Hệ thống xử lý qua FR-51, phản hồi câu trả lời thành công đáp ứng thời gian quy định tại NFR-03 và tăng bộ đếm thêm 1 lượt (BR-01, BR-03).

- **AC-02.2 — Chặn gọi AI và không trừ lượt khi Guest đã đạt 5 lượt/ngày:**
  - **Given:** Guest đã sử dụng đủ 5 lượt AI thành công trong ngày.
  - **When:** Guest gửi thêm câu hỏi thứ 6.
  - **Then:** Hệ thống không gửi yêu cầu tới dịch vụ AI, giữ nguyên số lượt và hiển thị thông báo hết hạn mức kèm liên kết Đăng ký tài khoản (BR-01, BR-05).

- **AC-02.3 — Không trừ lượt dùng thử khi dịch vụ AI gặp sự cố hoặc timeout:**
  - **Given:** Guest đang có số dư lượt là 3/5.
  - **When:** Guest gửi câu hỏi nhưng dịch vụ AI gặp lỗi kết nối hoặc quá thời gian phản hồi quy định.
  - **Then:** Hệ thống thông báo lỗi kỹ thuật và số lượt sử dụng của Guest vẫn giữ nguyên là 3/5 (BR-03, BR-04, NFR-18).

- **AC-02.4 — Tự động đặt lại bộ đếm lúc 00:00 Asia/Ho_Chi_Minh:**
  - **Given:** Guest đã dùng hết 5/5 lượt trong ngày hôm nay.
  - **When:** Thời gian chuyển sang 00:00 ngày hôm sau theo giờ `Asia/Ho_Chi_Minh`.
  - **Then:** Bộ đếm tự động đặt lại về 0/5, cho phép Guest tiếp tục dùng thử (BR-01).

---

<a id="fr-03"></a>
### FR-03 — Đăng ký, đăng nhập và quản lý tài khoản cơ bản

- **Mã yêu cầu:** FR-03
- **Module:** M02 (Identity & Access)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống cung cấp cơ chế đăng ký và đăng nhập bằng email/mật khẩu hoặc Google Login, xác minh email bắt buộc, quên/đặt lại mật khẩu, làm mới phiên xác thực an toàn bằng rotating refresh token có server-side revocation, đăng xuất thu hồi session, và phòng vệ brute-force bằng rate limiting 10 phút sau 5 lần thất bại liên tiếp (không khóa vĩnh viễn tài khoản sang trạng thái Admin `LOCKED`).

#### 1. Mục đích
Cung cấp giải pháp định danh, xác thực an toàn và quản lý vòng đời tài khoản/phiên đăng nhập cho người dùng hệ thống (Guest, Member, Administrator); đảm bảo tính bảo mật của thông tin định danh, phòng chống các tấn công brute-force và credential stuffing, đồng thời duy trì trải nghiệm người dùng liền mạch qua cơ chế xoay vòng token có kiểm soát.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Guest`: Người dùng chưa xác thực thực hiện đăng ký tài khoản mới, xác minh email, đăng nhập hoặc yêu cầu đặt lại mật khẩu.
  - `Member` / `Administrator`: Người dùng đã đăng nhập thực hiện các thao tác trong phiên làm việc, làm mới phiên tự động hoặc đăng xuất khỏi hệ thống.
- **Secondary Actor / External System:**
  - `Google Identity Services (OAuth2/OIDC)`: Cung cấp dịch vụ xác thực tài khoản Google tập trung.
  - `Dịch vụ Email (SMTP/Email Provider)`: Chịu trách nhiệm gửi email xác minh và email đặt lại mật khẩu theo cơ chế bất đồng bộ / best-effort.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-03.1`: Đăng ký tài khoản mới bằng email và mật khẩu (Register with email and password).
  - `UC-03.2`: Xác minh địa chỉ email qua liên kết kích hoạt (Verify email address).
  - `UC-03.3`: Yêu cầu gửi lại email xác minh (Resend verification email).
  - `UC-03.4`: Đăng nhập bằng email và mật khẩu (Login with email and password). *(Bao gồm luồng xử lý phòng vệ brute-force rate limiting)*
  - `UC-03.5`: Đăng nhập 1-click bằng tài khoản Google (Login with Google OAuth2/OIDC).
  - `UC-03.6`: Yêu cầu gửi liên kết đặt lại mật khẩu qua email (Request password reset).
  - `UC-03.7`: Thiết lập mật khẩu mới từ liên kết hợp lệ (Reset password).
  - `UC-03.8`: Làm mới phiên xác thực qua Secure HttpOnly Cookie (Refresh authenticated session via Secure HttpOnly Cookie).
  - `UC-03.9`: Đăng xuất và thu hồi phiên làm việc trên máy chủ (Logout and terminate session).
  *(Ghi chú: UC-03.10 trước đây về giới hạn tần suất đăng nhập sai không còn là Use Case độc lập vì không phải mục tiêu của Actor; hành vi này được chuẩn hóa thành Luồng rẽ nhánh / Luồng bảo mật thuộc UC-03.4).*
- **User Stories:**
  - `US-03.1`: Là một người dùng mới, tôi muốn đăng ký tài khoản nhanh chóng bằng email và mật khẩu để có thể lưu trữ thông tin cá nhân và cá nhân hóa chế độ ăn chay.
  - `US-03.2`: Là một người dùng, tôi muốn đăng nhập linh hoạt bằng tài khoản hệ thống hoặc tài khoản Google để truy cập thuận tiện trên các thiết bị.
  - `US-03.3`: Là một thành viên đang sử dụng ứng dụng, tôi muốn phiên đăng nhập được duy trì tự động và an toàn mà không phải nhập lại mật khẩu thường xuyên.
  - `US-03.4`: Là một thành viên, tôi muốn khi đăng xuất thì phiên làm việc trên thiết bị bị hủy hoàn toàn trên máy chủ để bảo đảm an toàn cho tài khoản.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:**
  - Đối với đăng ký: Địa chỉ email chưa được liên kết với một tài khoản đang hoạt động trong hệ thống.
  - Đối với đăng nhập bằng email/mật khẩu: Tài khoản đã được tạo và email đã được xác minh thành công.
  - Đối với Google Login: Người dùng sở hữu tài khoản Google hợp lệ.
  - Đối với làm mới phiên: Thiết bị gửi kèm refresh token hợp lệ, còn thời hạn và chưa bị thu hồi trên máy chủ.
- **Trigger:** Người dùng gửi yêu cầu Đăng ký, Đăng nhập, Quên mật khẩu, Đăng xuất trên giao diện, hoặc ứng dụng gửi yêu cầu làm mới phiên khi access token chuẩn bị hết hạn.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Đăng ký & Xác minh Email (UC-03.1, UC-03.2, UC-03.3)
1. **Main Flow (Đăng ký thành công):**
   - Bước 1: Guest truy cập trang Đăng ký, nhập thông tin: Tên hiển thị (3–50 ký tự), Email hợp lệ, Mật khẩu (tối thiểu 8 ký tự, đáp ứng yêu cầu độ phức tạp), Xác nhận mật khẩu.
   - Bước 2: Guest nhấn "Đăng ký". Ứng dụng gửi yêu cầu đăng ký tới máy chủ.
   - Bước 3: Hệ thống kiểm tra tính hợp lệ của dữ liệu, xác nhận email chưa tồn tại, băm mật khẩu bằng thuật toán an toàn (BCrypt theo NFR-06), tạo tài khoản ở trạng thái chưa xác minh (`UNVERIFIED`).
   - Bước 4: Hệ thống tạo mã xác minh ngẫu nhiên có thời hạn (24 giờ) gắn với tài khoản.
   - Bước 5: Hệ thống kích hoạt gửi email xác minh bất đồng bộ tới email người dùng kèm đường dẫn kích hoạt chứa mã xác minh.
   - Bước 6: Hệ thống phản hồi thông báo đăng ký thành công và nhắc nhở người dùng kiểm tra hộp thư.
   - Bước 7: Người dùng nhấp vào liên kết xác minh trong email.
   - Bước 8: Hệ thống kiểm tra mã xác minh hợp lệ và còn hạn; cập nhật trạng thái tài khoản thành hoạt động (`ACTIVE`) và vô hiệu hóa mã xác minh đó.
   - Bước 9: Giao diện hiển thị thông báo xác minh thành công và điều hướng người dùng tới Onboarding Questionnaire hoặc màn hình Đăng nhập.
2. **Alternative Flows:**
   - *Gửi lại email xác minh (UC-03.3):* Nếu người dùng chưa nhận được email hoặc mã xác minh hết hạn, người dùng có thể yêu cầu gửi lại email xác minh. Hệ thống tạo mã mới, hủy mã cũ và gửi lại email (áp dụng giới hạn tần suất gửi tối thiểu 60 giây/lần để tránh spam).
3. **Error Flows:**
   - *Email đã tồn tại:* Nếu email đã được đăng ký trong hệ thống, hệ thống từ chối yêu cầu và phản hồi thông báo lỗi tương ứng.
   - *Mật khẩu không đạt độ phức tạp:* Hệ thống từ chối yêu cầu và thông báo chi tiết tiêu chí mật khẩu chưa đạt chuẩn.
   - *Mã xác minh không hợp lệ hoặc đã hết hạn:* Hệ thống từ chối kích hoạt và cung cấp tùy chọn gửi lại email xác minh mới.

##### B. Luồng Đăng nhập bằng Email/Mật khẩu & Phòng vệ Brute-force (UC-03.4)
1. **Main Flow (Đăng nhập thành công):**
   - Bước 1: Guest nhập email và mật khẩu tại màn hình Đăng nhập, nhấn "Đăng nhập".
   - Bước 2: Hệ thống kiểm tra cơ chế giới hạn thử sai (rate limit) đối với định danh tài khoản và địa chỉ IP nguồn. Nếu chưa chạm ngưỡng giới hạn, tiếp tục xử lý.
   - Bước 3: Hệ thống tìm kiếm thông tin tài khoản theo email.
   - Bước 4: Hệ thống so khớp mật khẩu qua thuật toán băm an toàn (BCrypt). Mật khẩu khớp chính xác.
   - Bước 5: Hệ thống kiểm tra trạng thái tài khoản: tài khoản đã xác minh email và không ở trạng thái bị khóa quản trị (`LOCKED`).
   - Bước 6: Hệ thống xóa bộ đếm thử sai liên quan đến tài khoản và IP nguồn về 0.
   - Bước 7: Hệ thống tạo Access Token ngắn hạn và Rotating Refresh Token; thiết lập Refresh Token vào Secure HttpOnly Cookie (ngăn chặn JavaScript phía máy khách truy cập trực tiếp).
   - Bước 8: Hệ thống ghi nhận và lưu trữ phiên làm việc được theo dõi phía máy chủ (server-side session tracking).
   - Bước 9: Hệ thống phản hồi đăng nhập thành công kèm Access Token cho ứng dụng; giao diện chuyển sang trạng thái đã đăng nhập.
2. **Alternative & Security Flow (Phòng vệ Brute-force Rate Limiting theo cả Account Identifier và Source IP):**
   - *Đăng nhập sai từ lần 1 đến lần 4:* Mật khẩu không khớp -> Hệ thống tăng bộ đếm thất bại đối với định danh tài khoản và địa chỉ IP nguồn, từ chối xác thực kèm thông báo an toàn chung: "Email hoặc mật khẩu không chính xác".
   - *Đăng nhập sai liên tiếp lần thứ 5:* Khi ghi nhận 5 lần đăng nhập thất bại liên tiếp liên quan đến định danh tài khoản hoặc phát xuất từ địa chỉ IP nguồn, hệ thống tự động kích hoạt cơ chế bảo vệ tạm thời (Temporary Rate Limit) trong đúng 10 phút theo cả hai chiều độc lập:
     - **Bảo vệ theo định danh tài khoản (Account Identifier):** Các nỗ lực đăng nhập tiếp theo nhắm vào tài khoản đó bị tạm chặn trong 10 phút để phòng chống tấn công dò quét mật khẩu (brute-force);
     - **Bảo vệ theo địa chỉ IP nguồn (Source IP):** Các nỗ lực đăng nhập tiếp theo phát xuất từ địa chỉ IP đó bị tạm chặn trong 10 phút để phòng chống tấn công rà quét diện rộng (credential stuffing).
   - *Yêu cầu đăng nhập trong 10 phút bị rate limit:* Mọi yêu cầu đăng nhập liên quan đến tài khoản đang bị bảo vệ hoặc gửi từ IP nguồn đang bị hạn chế đều bị từ chối ngay tại cổng tiếp nhận với thông báo: *"Bạn đã đăng nhập sai quá số lần quy định. Vui lòng thử lại sau 10 phút."*, hoàn toàn không truy vấn kiểm tra mật khẩu trong cơ sở dữ liệu (NFR-07).
   - *Hết thời hạn 10 phút:* Cơ chế bảo vệ tạm thời tự động hết hiệu lực; người dùng có thể tiếp tục đăng nhập bình thường mà KHÔNG cần Quản trị viên can thiệp.
   - *Ranh giới bảo mật cốt lõi:* Hệ thống TUYỆT ĐỐI KHÔNG chuyển trạng thái tài khoản sang trạng thái khóa quản trị (`LOCKED`) trong cơ sở dữ liệu khi bị rate limit (trạng thái `LOCKED` chỉ do Quản trị viên áp dụng thủ công sau hậu kiểm theo BR-26).
3. **Error Flows:**
   - *Tài khoản chưa xác minh email:* Hệ thống từ chối đăng nhập, thông báo tài khoản chưa kích hoạt và cung cấp liên kết gửi lại email xác minh.
   - *Tài khoản bị Quản trị viên khóa (`LOCKED`):* Hệ thống từ chối đăng nhập và thông báo tài khoản đang bị khóa theo quyết định quản trị.

##### C. Luồng Đăng nhập bằng Google (UC-03.5)
1. **Main Flow:**
   - Bước 1: Guest nhấn nút "Đăng nhập với Google" (Google One Tap hoặc nút đăng nhập Google).
   - Bước 2: Người dùng xác thực tài khoản trên giao diện Google và đồng ý chia sẻ thông tin cơ bản (email, tên, ảnh đại diện).
   - Bước 3: Ứng dụng nhận mã xác thực từ Google và gửi yêu cầu xác thực tới máy chủ hệ thống.
   - Bước 4: Máy chủ hệ thống xác thực tính hợp lệ của token với dịch vụ Google Identity.
   - Bước 5: Hệ thống trích xuất email, tên và ảnh đại diện từ dữ liệu xác thực Google.
   - Bước 6: Nếu email chưa tồn tại trong hệ thống, hệ thống tự động tạo tài khoản Member mới với email này ở trạng thái đã xác minh (`ACTIVE`) và thiết lập ảnh đại diện từ Google. Nếu email đã tồn tại, hệ thống liên kết định danh Google với tài khoản đó.
   - Bước 7: Hệ thống tạo Access Token ngắn hạn, thiết lập Rotating Refresh Token qua Secure HttpOnly Cookie, lưu trữ và theo dõi phiên làm việc phía máy chủ và hoàn tất đăng nhập thành công.

##### D. Luồng Quên & Đặt lại mật khẩu (UC-03.6, UC-03.7)
1. **Main Flow:**
   - Bước 1: Guest truy cập chức năng Quên mật khẩu, nhập email đã đăng ký và gửi yêu cầu.
   - Bước 2: Hệ thống kiểm tra email trong hệ thống. Nếu tài khoản tồn tại, tạo mã đặt lại mật khẩu ngẫu nhiên an toàn có thời hạn ngắn (15 phút).
   - Bước 3: Hệ thống gửi email chứa liên kết đặt lại mật khẩu an toàn theo cơ chế bất đồng bộ.
   - Bước 4: Hệ thống luôn phản hồi thông điệp trung tính: "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn" (nhằm phòng chống tấn công dò quét sự tồn tại của email người dùng).
   - Bước 5: Người dùng nhấp vào liên kết trong email, giao diện hiển thị biểu mẫu thiết lập mật khẩu mới.
   - Bước 6: Người dùng gửi mật khẩu mới kèm mã xác thực. Hệ thống kiểm tra mã hợp lệ và còn hạn; băm mật khẩu mới bằng BCrypt, cập nhật mật khẩu tài khoản, vô hiệu hóa mã đặt lại mật khẩu đã sử dụng, đồng thời lập tức thu hồi toàn bộ các phiên đăng nhập đang hoạt động của tài khoản trên máy chủ.
   - Bước 7: Giao diện thông báo đổi mật khẩu thành công và điều hướng tới màn hình Đăng nhập.
2. **Error Flows:**
   - *Mã đặt lại mật khẩu hết hạn hoặc không hợp lệ:* Hệ thống từ chối yêu cầu và thông báo người dùng khởi tạo lại quy trình quên mật khẩu.

##### E. Luồng Làm mới phiên xác thực & Đăng xuất (UC-03.8, UC-03.9)
1. **Main Flow Làm mới phiên (Token Rotation):**
   - Bước 1: Khi Access Token hết hạn, ứng dụng gửi yêu cầu làm mới phiên tới máy chủ (Refresh Token được trình duyệt tự động gửi kèm qua Secure HttpOnly Cookie mà JavaScript không truy cập trực tiếp).
   - Bước 2: Hệ thống kiểm tra tính hợp lệ của Refresh Token từ cookie và đối chiếu với danh sách phiên đang hoạt động trên máy chủ.
   - Bước 3: Nếu Refresh Token hợp lệ và khớp với phiên đang hoạt động:
     - Hệ thống tạo Access Token mới.
     - Hệ thống tạo Refresh Token mới (xoay vòng token).
     - Hệ thống cập nhật phiên làm việc máy chủ với Refresh Token mới, vô hiệu hóa Refresh Token cũ.
     - Hệ thống thiết lập Secure HttpOnly Cookie mới chứa Refresh Token mới và trả về Access Token mới cho ứng dụng.
2. **Luồng An ninh — Phát hiện tái sử dụng Token (Token Reuse Detection):**
   - Nếu hệ thống nhận được một Refresh Token đã từng bị thay thế trước đó (dấu hiệu token bị rò rỉ hoặc bị đánh cắp phiên):
     - Hệ thống lập tức thu hồi và hủy toàn bộ các phiên làm việc thuộc nhóm phiên liên quan (token family) của tài khoản trên máy chủ.
     - Hệ thống gửi phản hồi xóa/hết hạn Secure HttpOnly Cookie của Refresh Token và từ chối yêu cầu xác thực, buộc người dùng phải đăng nhập lại từ đầu trên mọi thiết bị.
3. **Main Flow Đăng xuất (UC-03.9):**
   - Bước 1: Người dùng nhấn "Đăng xuất". Ứng dụng gửi yêu cầu đăng xuất tới máy chủ.
   - Bước 2: Hệ thống xác thực yêu cầu, lập tức thu hồi và hủy vĩnh viễn phiên làm việc tương ứng trên máy chủ (server-side session revocation).
   - Bước 3: Hệ thống gửi phản hồi chỉ thị xóa/hết hạn Secure HttpOnly Cookie của Refresh Token về trình duyệt; ứng dụng xóa bỏ Access Token và trạng thái xác thực cục bộ phía máy khách.
   - Bước 4: Hệ thống xác nhận đăng xuất thành công; người dùng trở về trạng thái Guest.

#### 6. Hậu điều kiện (Postconditions)
- Sau khi đăng ký: Bản ghi tài khoản mới được tạo ở trạng thái chưa xác minh (`UNVERIFIED`); email xác minh được gửi đi.
- Sau khi xác minh email: Trạng thái tài khoản chuyển thành hoạt động (`ACTIVE`); email được ghi nhận đã xác thực.
- Sau khi đăng nhập: Access Token ngắn hạn được cấp phát, Refresh Token được thiết lập qua Secure HttpOnly Cookie; phiên làm việc được lưu trữ và theo dõi phía máy chủ; bộ đếm thử sai được đặt lại về 0.
- Sau khi bị rate limit: Cơ chế bảo vệ theo định danh tài khoản và IP nguồn tạm dừng tiếp nhận đăng nhập trong 10 phút; trạng thái tài khoản trong cơ sở dữ liệu tuyệt đối không bị chuyển sang `LOCKED`.
- Sau khi đăng xuất: Phiên làm việc tương ứng bị thu hồi vĩnh viễn trên máy chủ; cookie Refresh Token bị xóa/hết hạn; trạng thái xác thực phía máy khách được xóa; token cũ không thể sử dụng để làm mới phiên.

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Khách vãng lai (Guest) chỉ được tiếp cận các chức năng: đăng ký, đăng nhập, đăng nhập Google, xác minh email, gửi lại email xác minh, yêu cầu đặt lại mật khẩu, thiết lập mật khẩu mới.
- Thành viên (Member) và Quản trị viên (Administrator) được tiếp cận chức năng làm mới phiên xác thực và đăng xuất.
- Ràng buộc kỹ thuật được phê duyệt:
  - Mật khẩu người dùng phải được băm một chiều an toàn bằng thuật toán BCrypt với work factor tối thiểu 10 (NFR-06); tuyệt đối không lưu trữ mật khẩu dạng rõ (plaintext) hoặc mã hóa hai chiều.
  - Cơ chế xác thực sử dụng JWT Access Token ngắn hạn kết hợp Rotating Refresh Token truyền qua Secure HttpOnly Cookie (không để JavaScript truy cập trực tiếp Refresh Token) và cơ chế thu hồi phía máy chủ (server-side revocation) (NFR-09).
  - Cơ chế phòng vệ brute-force áp dụng quy tắc 5 lần đăng nhập thất bại liên tiếp thì rate limit trong 10 phút, độc lập hoàn toàn với trạng thái khóa tài khoản quản trị `LOCKED` (NFR-07).
  - Tích hợp đăng nhập bên thứ ba sử dụng chuẩn OAuth2/OIDC của Google Identity Services.
  - Không để lộ secret key, API key, thông tin lỗi nội bộ hoặc token trong log hệ thống.

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest (Guest không có dữ liệu cá nhân hóa, phải đăng nhập để dùng tính năng thành viên).
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp (yêu cầu tài khoản Member đã xác thực).
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-01](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-01): Thời gian phản hồi xử lý Đăng nhập/Đăng xuất $\le 2$ giây (P95).
  - [NFR-06](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-06): Mã hóa mật khẩu 100% bằng BCrypt, 0% plaintext.
  - [NFR-07](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-07): Giới hạn 5 lần đăng nhập sai liên tiếp, rate limit tài khoản + IP trong 10 phút, không chuyển sang `LOCKED`.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái phép, JWT access ngắn hạn + rotating refresh token qua Secure HttpOnly Cookie với server-side revocation, logout thu hồi phiên.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống lỗ hổng OWASP Top 10.
  - [NFR-11](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-11): Giao diện đăng ký $\le 3$ bước / $\le 3$ click; hỗ trợ Google Login 1-click.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-03.1 — Đăng ký thành công bằng email và mật khẩu hợp lệ:**
  - **Given:** Guest đang ở trang Đăng ký và email nhập vào chưa từng tồn tại trong hệ thống.
  - **When:** Guest nhập email hợp lệ, mật khẩu đạt chuẩn (ít nhất 8 ký tự, có chữ hoa, chữ thường và số/ký tự đặc biệt), xác nhận mật khẩu khớp, và nhấn "Đăng ký".
  - **Then:** Hệ thống tạo tài khoản mới ở trạng thái `UNVERIFIED`, băm mật khẩu bằng BCrypt, gửi email xác minh chứa liên kết kích hoạt, và thông báo yêu cầu kiểm tra email.

- **AC-03.2 — Đăng ký từ chối email đã tồn tại trong hệ thống:**
  - **Given:** Guest đang ở trang Đăng ký.
  - **When:** Guest nhập địa chỉ email đã được đăng ký bởi một tài khoản đang hoạt động và nhấn "Đăng ký".
  - **Then:** Hệ thống từ chối đăng ký, không tạo thêm tài khoản, và hiển thị thông báo lỗi email đã được sử dụng.

- **AC-03.3 — Đăng ký từ chối mật khẩu không đạt chuẩn độ phức tạp:**
  - **Given:** Guest đang ở trang Đăng ký với địa chỉ email hợp lệ và chưa tồn tại.
  - **When:** Guest nhập mật khẩu không đạt yêu cầu (dưới 8 ký tự hoặc thiếu thành phần chữ hoa/chữ thường/số) và nhấn "Đăng ký".
  - **Then:** Hệ thống từ chối đăng ký, không tạo tài khoản, và hiển thị thông báo lỗi cụ thể về tiêu chí mật khẩu chưa thỏa mãn.

- **AC-03.4 — Xác minh email thành công kích hoạt tài khoản:**
  - **Given:** Tài khoản đang ở trạng thái `UNVERIFIED` với mã xác minh hợp lệ còn thời hạn.
  - **When:** Người dùng nhấp vào liên kết xác minh từ email gửi tới.
  - **Then:** Hệ thống kiểm tra mã hợp lệ, cập nhật trạng thái tài khoản thành `ACTIVE`, vô hiệu hóa mã xác minh, và hiển thị thông báo kích hoạt thành công.

- **AC-03.5 — Xác minh email thất bại do mã không hợp lệ hoặc hết hạn:**
  - **Given:** Người dùng mở liên kết xác minh có mã xác thực đã hết hạn hoặc không tồn tại.
  - **When:** Yêu cầu xác minh gửi tới hệ thống.
  - **Then:** Hệ thống từ chối kích hoạt, thông báo mã xác thực không hợp lệ/hết hạn, và hiển thị tùy chọn yêu cầu gửi lại email xác minh mới.

- **AC-03.6 — Đăng nhập thành công với thông tin chính xác:**
  - **Given:** Tài khoản đang ở trạng thái `ACTIVE` và đã xác minh email.
  - **When:** Người dùng nhập đúng email và mật khẩu tại trang Đăng nhập và nhấn "Đăng nhập".
  - **Then:** Hệ thống phản hồi thành công trong vòng $\le 2$ giây (P95 theo NFR-01), cấp phát Access Token ngắn hạn, thiết lập Refresh Token qua Secure HttpOnly Cookie, ghi nhận phiên máy chủ, xóa bộ đếm đăng nhập sai về 0, và chuyển trạng thái sang đã đăng nhập.

- **AC-03.7 — Đăng nhập thất bại do sai mật khẩu và ghi nhận số lần sai:**
  - **Given:** Người dùng nhập đúng email nhưng sai mật khẩu và số lần sai trước đó $< 4$.
  - **When:** Người dùng nhấn "Đăng nhập".
  - **Then:** Hệ thống tăng bộ đếm thất bại đối với định danh tài khoản và địa chỉ IP nguồn, từ chối xác thực kèm thông báo "Email hoặc mật khẩu không chính xác", và không cấp phát token.

- **AC-03.8 — Kích hoạt rate limit sau 5 lần đăng nhập sai liên tiếp theo tài khoản và IP:**
  - **Given:** Đã có 4 lần đăng nhập sai liên tiếp đối với một tài khoản hoặc từ một địa chỉ IP.
  - **When:** Người dùng tiếp tục thực hiện lần đăng nhập sai thứ 5.
  - **Then:** Hệ thống kích hoạt cơ chế bảo vệ tạm thời trong đúng 10 phút (chặn theo cả định danh tài khoản và theo địa chỉ IP), từ chối xử lý kèm thông báo thử lại sau 10 phút; trạng thái tài khoản trong cơ sở dữ liệu KHÔNG bị chuyển thành `LOCKED`.

- **AC-03.9 — Phục hồi đăng nhập tự động sau khi hết 10 phút rate limit:**
  - **Given:** Tài khoản hoặc địa chỉ IP đang bị rate limit 10 phút do đã đăng nhập sai 5 lần liên tiếp.
  - **When:** Thời gian 10 phút đã trôi qua và người dùng thực hiện đăng nhập lại với mật khẩu chính xác.
  - **Then:** Hệ thống tiếp nhận yêu cầu, xác thực thành công, xóa bộ đếm rate limit về 0, cấp phát token và đăng nhập thành công mà không cần Quản trị viên can thiệp mở khóa.

- **AC-03.10 — Đăng nhập 1-click bằng tài khoản Google:**
  - **Given:** Guest chọn đăng nhập với Google và hoàn tất xác thực trên Google OAuth2/OIDC.
  - **When:** Dữ liệu xác thực Google hợp lệ được gửi tới máy chủ hệ thống.
  - **Then:** Hệ thống xác thực tính hợp lệ với Google; nếu tài khoản chưa có thì tự động tạo Member mới với trạng thái email đã xác minh, thiết lập Refresh Token qua Secure HttpOnly Cookie, cấp phát Access Token phiên làm việc và hoàn tất đăng nhập thành công.

- **AC-03.11 — Xoay vòng Refresh Token thành công khi làm mới phiên:**
  - **Given:** Member sở hữu Access Token đã hết hạn và Refresh Token hợp lệ còn thời hạn được ghi nhận trên máy chủ (được trình duyệt gửi qua Secure HttpOnly Cookie).
  - **When:** Ứng dụng gửi yêu cầu làm mới phiên tới máy chủ.
  - **Then:** Hệ thống sinh ra một Access Token mới và một Refresh Token mới thay thế, cập nhật phiên máy chủ, vô hiệu hóa Refresh Token cũ, thiết lập Refresh Token mới vào Secure HttpOnly Cookie và trả về Access Token mới cho ứng dụng mà không để lộ Refresh Token cho mã nguồn JavaScript phía máy khách.

- **AC-03.12 — Thu hồi phiên khi phát hiện tái sử dụng Refresh Token cũ:**
  - **Given:** Một Refresh Token cũ đã từng bị xoay vòng thay thế.
  - **When:** Yêu cầu làm mới phiên gửi lại Refresh Token cũ đó tới hệ thống.
  - **Then:** Hệ thống phát hiện hành vi tái sử dụng token, lập tức hủy toàn bộ phiên làm việc thuộc nhóm phiên liên quan của tài khoản trên máy chủ, gửi chỉ thị xóa cookie Refresh Token, từ chối xác thực, buộc tài khoản phải đăng nhập lại từ đầu.

- **AC-03.13 — Đăng xuất thu hồi phiên làm việc trên máy chủ và xóa cookie:**
  - **Given:** Member đang trong phiên đăng nhập hợp lệ trên hệ thống.
  - **When:** Member nhấn "Đăng xuất".
  - **Then:** Hệ thống thu hồi và hủy bản ghi phiên làm việc tương ứng trên máy chủ, gửi phản hồi chỉ thị xóa/hết hạn Secure HttpOnly Cookie của Refresh Token; ứng dụng xóa bỏ Access Token và trạng thái xác thực cục bộ; token đã thu hồi không thể tái sử dụng để làm mới phiên.

- **AC-03.14 — Đặt lại mật khẩu thành công thu hồi các phiên đăng nhập cũ:**
  - **Given:** Người dùng có mã đặt lại mật khẩu hợp lệ còn thời hạn trong 15 phút.
  - **When:** Người dùng nhập mật khẩu mới đạt chuẩn độ phức tạp và gửi yêu cầu đặt lại mật khẩu.
  - **Then:** Hệ thống băm mật khẩu mới bằng BCrypt, cập nhật tài khoản, hủy mã đặt lại mật khẩu, đồng thời thu hồi toàn bộ các phiên làm việc hiện có của tài khoản trên máy chủ; người dùng có thể đăng nhập bằng mật khẩu mới.

---

<a id="fr-04"></a>
### FR-04 — Member tạo và công khai trực tiếp Recipe Post của chính mình

- **Mã yêu cầu:** FR-04
- **Module:** M02 (Identity & Access), M03 (Community Recipes & Social)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống cho phép Member đã đăng nhập tạo, xem, chỉnh sửa, xóa và công khai trực tiếp Recipe Post của chính mình khi đạt các tiêu chuẩn kiểm tra tính hợp lệ; Member có thể upload tối đa 5 ảnh và gắn tối đa 1 link YouTube; không có luồng gửi Blog tổng quát, đơn xin quyền đăng hoặc duyệt trước từng bài.

#### 1. Mục đích
Trao quyền tự chủ sáng tạo nội dung cho thành viên cộng đồng; tạo điều kiện để người dùng chia sẻ công thức món chay nhanh chóng, thuận tiện mà không gặp rào cản hành chính duyệt bài trước; đồng thời đảm bảo quyền sở hữu bất biến của tác giả đối với nội dung do mình tạo ra.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member` (Tác giả): Người dùng đã đăng nhập tạo, sửa, xóa và công khai bài viết.
- **Secondary Actor / External System:**
  - `Dịch vụ lưu trữ tệp đám mây (Azure Blob Storage)`: Lưu trữ ảnh minh họa bài viết.
  - `Administrator`: Quản trị viên xử lý báo cáo vi phạm sau khi bài đã công khai (hậu kiểm).

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-04.1`: Tạo và công khai bài công thức cá nhân (Create and publish personal recipe post).
  - `UC-04.2`: Chỉnh sửa bài công thức của chính mình (Edit owned recipe post).
  - `UC-04.3`: Xóa bài công thức của chính mình (Delete owned recipe post).
- **User Stories:**
  - `US-04.1`: Là một thành viên yêu nấu ăn, tôi muốn tạo và chia sẻ ngay công thức món chay mới của mình lên cộng đồng mà không phải gửi đơn xin duyệt từng bài.
  - `US-04.2`: Là tác giả bài viết, tôi muốn có thể chỉnh sửa lại định lượng gia vị hoặc cập nhật ảnh món ăn bất kỳ lúc nào để hoàn thiện bài viết.
  - `US-04.3`: Là tác giả bài viết, tôi muốn có thể xóa bài công thức của mình khi không còn muốn chia sẻ nữa.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Người dùng đã đăng nhập với tài khoản Member ở trạng thái hoạt động (`ACTIVE`) và đã xác minh email. Đối với sửa/xóa bài: người dùng phải là chính chủ tác giả của bài viết (`authenticatedUser.id == recipe.authorId`).
- **Trigger:** Member nhấn nút "Đăng công thức" trên thanh điều hướng, hoặc nhấn "Chỉnh sửa" / "Xóa" trên trang chi tiết bài viết của mình.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Tạo và công khai bài công thức trực tiếp (UC-04.1)
1. **Main Flow:**
   - Bước 1: Member nhấn "Đăng công thức". Giao diện hiển thị biểu mẫu tạo bài viết.
   - Bước 2: Member nhập thông tin bắt buộc (cấu trúc và validation theo FR-16, nguyên liệu theo FR-19, các bước hướng dẫn chuẩn bị/chế biến theo FR-22, tải ảnh lên Azure Blob Storage theo FR-14 và gắn link YouTube theo FR-15).
   - Bước 3: Member nhấn "Công khai bài viết".
   - Bước 4: Hệ thống thực thi kiểm tra tính hợp lệ toàn bộ dữ liệu (validation rules theo BR-07 / SRS 3.9). Toàn bộ dữ liệu đạt chuẩn.
   - Bước 5: Hệ thống tự động gán mã định danh tác giả từ phiên đăng nhập (BR-17), lưu bài viết ở trạng thái công khai (`PUBLISHED`), và phản hồi thành công.
   - Bước 6: Bài viết lập tức hiển thị trên trang cá nhân của tác giả, trang chủ và kết quả tìm kiếm.

##### B. Luồng Chỉnh sửa bài công thức của chính mình (UC-04.2)
1. **Main Flow:**
   - Bước 1: Tác giả mở bài công thức do mình sở hữu và nhấn "Chỉnh sửa bài viết".
   - Bước 2: Giao diện hiển thị biểu mẫu với toàn bộ thông tin hiện tại của bài viết.
   - Bước 3: Tác giả thay đổi thông tin cần cập nhật và nhấn "Lưu thay đổi".
   - Bước 4: Hệ thống kiểm tra quyền sở hữu của người gọi ở tầng máy chủ (Ownership Check theo BR-64).
   - Bước 5: Hệ thống kiểm tra tính hợp lệ của dữ liệu mới, cập nhật bài viết trong cơ sở dữ liệu và phản hồi thành công.

##### C. Luồng Xóa bài công thức của chính mình (UC-04.3)
1. **Main Flow:**
   - Bước 1: Tác giả mở bài công thức do mình sở hữu và nhấn "Xóa bài viết".
   - Bước 2: Hệ thống hiển thị hộp thoại xác nhận an toàn: "Bạn có chắc chắn muốn xóa bài công thức này? Thao tác này không thể hoàn tác."
   - Bước 3: Tác giả xác nhận đồng ý xóa.
   - Bước 4: Hệ thống kiểm tra quyền sở hữu tác giả ở tầng máy chủ.
   - Bước 5: Hệ thống đánh dấu trạng thái bài viết là không khả dụng/đã xóa (tombstone pattern); gỡ bài viết khỏi danh sách công khai, kết quả tìm kiếm và các gợi ý AI; giữ lại bản ghi tham chiếu cho các Lịch ăn hoặc Danh sách đã lưu của người dùng khác ở trạng thái không khả dụng (BR-32, BR-33, BR-64).

##### D. Luồng An ninh & Xử lý ngoại lệ (Security Flow)
1. **Main Flow:**
   - Nếu một người dùng khác (không phải tác giả và không phải Quản trị viên) cố tình gửi yêu cầu sửa hoặc xóa bài viết tới máy chủ:
   - Máy chủ so khớp định danh người gọi với định danh tác giả của bài viết.
   - Phát hiện không trùng khớp -> Máy chủ lập tức từ chối yêu cầu và phản hồi mã lỗi HTTP 403 Forbidden.

#### 6. Hậu điều kiện (Postconditions)
- Bài viết mới được công khai trực tiếp ngay trên hệ thống (`PUBLISHED`).
- Khi sửa bài: Thông tin mới được cập nhật đồng bộ trên toàn hệ thống.
- Khi xóa bài: Bài viết không còn khả dụng công khai; các mục tham chiếu trong Saved Recipes và Meal Planner chuyển sang trạng thái đã xóa.

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Guest không có quyền tạo, sửa hoặc xóa bài công thức (BR-05).
- Tác giả chỉ có quyền chỉnh sửa hoặc xóa các bài viết do chính mình làm tác giả (BR-64, NFR-09).
- Không áp dụng cơ chế phê duyệt trước (pre-moderation) từ Quản trị viên; toàn bộ việc kiểm soát nội dung thực hiện theo cơ chế hậu kiểm (post-moderation) dựa trên báo cáo vi phạm cộng đồng (FR-06, BR-07).

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản đăng bài.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập và xử lý bài đã lưu.
  - [BR-33](BUSINESS-RULES.md#br-33): Xử lý tham chiếu công thức trong Lịch ăn.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và truyền tải an toàn.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn truy cập trái quyền.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-04.1 — Member công khai trực tiếp Recipe Post thành công khi dữ liệu hợp lệ:**
  - **Given:** Member đã đăng nhập với tài khoản hoạt động và hoàn thành biểu mẫu tạo công thức với đầy đủ thông tin hợp lệ.
  - **When:** Member nhấn "Công khai bài viết".
  - **Then:** Hệ thống lưu bài viết ở trạng thái `PUBLISHED` ngay lập tức mà không qua xét duyệt trước, gắn đúng định danh tác giả, và hiển thị bài viết trên trang công khai (BR-07, BR-17).

- **AC-04.2 — Tác giả chỉnh sửa thành công bài viết của chính mình:**
  - **Given:** Member là tác giả của bài công thức đang công khai.
  - **When:** Tác giả thay đổi thông tin mô tả và nhấn "Lưu thay đổi".
  - **Then:** Hệ thống xác thực quyền tác giả, cập nhật nội dung mới vào cơ sở dữ liệu và phản ánh ngay lập tức trên bài viết.

- **AC-04.3 — Tác giả xóa thành công bài viết của chính mình kèm xác nhận an toàn:**
  - **Given:** Member đang ở trang bài viết do chính mình làm tác giả.
  - **When:** Tác giả nhấn "Xóa bài viết" và nhấn xác nhận trên hộp thoại cảnh báo.
  - **Then:** Hệ thống gỡ bài viết khỏi danh sách công khai và tìm kiếm, đánh dấu trạng thái bài viết không khả dụng.

- **AC-04.4 — Chặn người dùng khác sửa hoặc xóa bài viết không thuộc quyền sở hữu:**
  - **Given:** Bài công thức thuộc tác giả A.
  - **When:** Người dùng B (Member khác, không phải Admin) gửi yêu cầu sửa hoặc xóa bài viết đó.
  - **Then:** Máy chủ kiểm tra quyền sở hữu, từ chối thao tác và trả về mã lỗi HTTP 403 Forbidden (BR-64).

- **AC-04.5 — Chặn Guest tạo bài công thức:**
  - **Given:** Người dùng chưa đăng nhập (Guest).
  - **When:** Guest cố tình mở trang tạo bài viết hoặc gửi yêu cầu tạo bài công thức.
  - **Then:** Hệ thống chuyển hướng người dùng tới trang Đăng nhập và không cho phép tạo bài (BR-05).

---

<a id="fr-05"></a>
### FR-05 — Cơ chế xin quyền đăng và Administrator duyệt trước bài công thức

- **Mã yêu cầu:** FR-05
- **Module:** M02, M03, M09
- **Trạng thái (Derived):** RETIRED
- **Mô tả:** Cơ chế Member gửi đơn xin quyền đăng và Administrator duyệt/từ chối đơn trước khi đăng Recipe Post.

---

<a id="fr-06"></a>
### FR-06 — Administrator xử lý báo cáo và quản lý nội dung hậu kiểm

- **Mã yêu cầu:** FR-06
- **Module:** M09 (Administration & Moderation)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống cho phép Administrator tiếp nhận, quản lý và xử lý báo cáo vi phạm đối với thành viên, Recipe Post, bình luận và danh mục; Administrator không duyệt quyền đăng hoặc duyệt từng Recipe Post trước khi công khai; toàn bộ việc kiểm soát nội dung tuân thủ cơ chế hậu kiểm (post-moderation) dựa trên phản ánh của cộng đồng.

#### 1. Mục đích
Cung cấp trung tâm điều hành hậu kiểm nội dung toàn diện cho Quản trị viên; đảm bảo môi trường cộng đồng trong sạch, an toàn, tuân thủ pháp luật và giữ gìn chuẩn mực ẩm thực chay chân chính mà không gây ách tắc cho việc chia sẻ nội dung tự do của thành viên.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Administrator`: Quản trị viên hệ thống có toàn quyền thực hiện các chế tài hậu kiểm.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-06.1`: Quản lý danh sách nội dung bị báo cáo hậu kiểm (Manage reported content dashboard).
  - `UC-06.2`: Thực hiện hành động hậu kiểm đối với nội dung hoặc tài khoản vi phạm (Execute moderation actions on content or user).
- **User Stories:**
  - `US-06.1`: Là một Administrator, tôi muốn có bảng tổng hợp trực quan các bài viết và bình luận bị báo cáo nhiều nhất để ưu tiên xử lý nhanh các trường hợp vi phạm nghiêm trọng.
  - `US-06.2`: Là một Administrator, tôi muốn áp dụng chế tài ẩn bài hoặc khóa tài khoản kèm lý do rõ ràng để bảo đảm tính minh bạch và công bằng.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Người dùng đã đăng nhập với vai trò Administrator (`Role = ADMIN`).
- **Trigger:** Administrator truy cập khu vực Quản lý hậu kiểm (Moderation Portal) trên trang Quản trị.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Quản lý bảng điều khiển hậu kiểm (UC-06.1)
1. **Main Flow:**
   - Bước 1: Administrator mở mục "Hậu kiểm nội dung".
   - Bước 2: Hệ thống hiển thị danh sách các nội dung bị báo cáo (Recipe Post, bình luận) kèm số lượng báo cáo tích lũy và lý do báo cáo phổ biến nhất.
   - Bước 3: Administrator có thể lọc theo loại nội dung, mức độ ưu tiên hoặc tìm kiếm theo tiêu đề bài viết / tên tài khoản.

##### B. Luồng Thực hiện chế tài hậu kiểm (UC-06.2)
1. **Main Flow:**
   - Bước 1: Administrator chọn một mục nội dung cần xử lý và xem chi tiết phản ánh từ cộng đồng.
   - Bước 2: Administrator đưa ra quyết định chế tài thủ công:
     - *Ẩn nội dung vi phạm:* Bài viết hoặc bình luận bị chuyển trạng thái ẩn vi phạm, không còn xuất hiện công khai trên hệ thống.
     - *Cảnh cáo tác giả:* Gửi thông báo cảnh cáo bằng văn bản tới hộp thư hệ thống của tác giả vi phạm.
     - *Khóa tài khoản vi phạm:* Chuyển trạng thái tài khoản sang `LOCKED` nếu tái phạm hoặc vi phạm nghiêm trọng theo BR-26.
     - *Bác bỏ báo cáo:* Đóng báo cáo và giữ nguyên trạng thái nội dung nếu nội dung không vi phạm quy tắc.
   - Bước 3: Administrator bắt buộc nhập kết luận giải thích lý do xử lý.
   - Bước 4: Hệ thống lưu vết kiểm toán, cập nhật trạng thái nội dung/tài khoản và phản hồi thành công.

#### 6. Hậu điều kiện (Postconditions)
- Trạng thái của nội dung hoặc tài khoản bị chế tài được cập nhật ngay lập tức.
- Quyết định xử lý được lưu trữ vĩnh viễn trong lịch sử kiểm toán của hệ thống.

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Chỉ tài khoản có vai trò Administrator mới có quyền truy cập trung tâm hậu kiểm và áp dụng chế tài (NFR-09).
- Tuyệt đối không áp dụng cơ chế phê duyệt trước (pre-moderation) từng bài viết trước khi đăng (BR-07).

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-06](BUSINESS-RULES.md#br-06): Quản lý nội dung công thức và kiểm duyệt hậu kiểm.
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-26](BUSINESS-RULES.md#br-26): Các hình thức chế tài khi xử lý vi phạm.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và toàn vẹn lịch sử kiểm toán.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-06.1 — Administrator xem bảng điều khiển hậu kiểm đầy đủ thông tin:**
  - **Given:** Administrator đang đăng nhập và mở trang Hậu kiểm nội dung.
  - **When:** Bảng điều khiển tải hoàn tất.
  - **Then:** Hệ thống hiển thị danh sách các bài viết và bình luận bị báo cáo kèm số lượt báo cáo, người bị báo cáo và trạng thái xử lý.

- **AC-06.2 — Administrator ẩn thành công bài viết vi phạm kèm thông báo lý do:**
  - **Given:** Bài công thức đang bị nhiều người dùng báo cáo vi phạm nội dung không chay.
  - **When:** Administrator chọn chế tài "Ẩn bài viết" và nhập lý do kết luận.
  - **Then:** Bài viết lập tức bị gỡ khỏi trang công khai và kết quả tìm kiếm, tác giả nhận được thông báo giải thích vi phạm.

- **AC-06.3 — Administrator khóa tài khoản vi phạm nghiêm trọng sang trạng thái LOCKED:**
  - **Given:** Thành viên tái phạm nhiều lần việc đăng nội dung độc hại.
  - **When:** Administrator chọn chế tài "Khóa tài khoản" và xác nhận lưu.
  - **Then:** Trạng thái tài khoản chuyển thành `LOCKED`, vô hiệu hóa các phiên đăng nhập của tài khoản đó (BR-26).

- **AC-06.4 — Bảo toàn cơ chế công khai trực tiếp cho các bài viết thông thường:**
  - **Given:** Quá trình hậu kiểm đang diễn ra song song trên hệ thống.
  - **When:** Các thành viên khác tạo và công khai bài công thức hợp lệ mới.
  - **Then:** Các bài viết mới vẫn được công khai ngay lập tức mà không bị nghẽn hay chờ duyệt trước (BR-07).

---

<a id="fr-07"></a>
### FR-07 — Administrator tạo, cập nhật và phân loại bài công thức chay

- **Mã yêu cầu:** FR-07
- **Module:** M04 (Recipe Catalog & Search)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống cho phép Administrator tạo mới, cập nhật nội dung và phân loại các bài công thức chay chính thức của hệ thống (System Recipes) vào các danh mục và loại ăn chay chuẩn; đảm bảo kho nội dung nòng cốt đạt chuẩn chất lượng cho nền tảng.

#### 1. Mục đích
Xây dựng và duy trì kho công thức ẩm thực chay chuẩn mực, đa dạng, đáng tin cậy phục vụ người dùng mới và làm dữ liệu tham chiếu nền tảng cho các tính năng gợi ý thực đơn của AI; đồng thời hỗ trợ quản trị viên tổ chức cấu trúc phân loại danh mục khoa học.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Administrator`: Quản trị viên hệ thống có toàn quyền quản lý nội dung công thức chính thức.
- **Secondary Actor / External System:**
  - `Dịch vụ lưu trữ tệp đám mây (Azure Blob Storage)`: Lưu trữ ảnh bài viết chính thức.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-07.1`: Tạo bài công thức chuẩn của hệ thống (Create official system recipe).
  - `UC-07.2`: Cập nhật bài công thức chuẩn (Update official recipe).
  - `UC-07.3`: Phân loại bài công thức vào danh mục và loại ăn chay (Categorize official recipe).
- **User Stories:**
  - `US-07.1`: Là một Administrator, tôi muốn tạo các bài công thức chuẩn mực với định lượng nguyên liệu chính xác để làm giàu kho nội dung chất lượng cao của nền tảng.
  - `US-07.2`: Là một Administrator, tôi muốn cập nhật nội dung hoặc điều chỉnh phân loại danh mục của công thức khi cần thiết để người dùng dễ dàng tìm kiếm.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Người dùng đã đăng nhập với vai trò Administrator (`Role = ADMIN`).
- **Trigger:** Administrator truy cập khu vực Quản lý công thức trong trang Quản trị và thực hiện tạo mới hoặc chỉnh sửa.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Tạo bài công thức chuẩn (UC-07.1, UC-07.3)
1. **Main Flow:**
   - Bước 1: Administrator vào mục "Quản lý công thức hệ thống" và nhấn "Tạo công thức mới".
   - Bước 2: Administrator nhập thông tin: Tiêu đề, mô tả ngắn, số khẩu phần, thời gian chuẩn bị và nấu, độ khó; chọn đúng 1 trong 4 loại ăn chay chuẩn (Vegan, Lacto, Ovo, Lacto-Ovo); chọn danh mục món ăn (Món kho, Món xào, Món canh, v.v.).
   - Bước 3: Administrator thêm danh sách nguyên liệu kèm định lượng và đơn vị đo chuẩn; thêm các bước nấu tuần tự và tải ảnh minh họa.
   - Bước 4: Administrator nhấn "Công khai bài viết".
   - Bước 5: Hệ thống kiểm tra tính hợp lệ của toàn bộ thông tin bắt buộc, lưu bài viết vào cơ sở dữ liệu với nhãn công thức hệ thống, và phản hồi thành công.
2. **Error Flows:**
   - *Thiếu thông tin bắt buộc:* Nếu vi phạm bộ quy tắc Recipe Validation Profile chuẩn theo FR-16 (ví dụ: thiếu tiêu đề, thiếu nguyên liệu, thiếu bước hướng dẫn chuẩn bị/chế biến, chưa chọn loại ăn chay), hệ thống từ chối lưu và hiển thị thông báo lỗi chi tiết cho từng trường.

##### B. Luồng Cập nhật bài công thức chuẩn (UC-07.2)
1. **Main Flow:**
   - Bước 1: Administrator chọn một bài công thức hệ thống cần cập nhật và nhấn "Chỉnh sửa".
   - Bước 2: Hệ thống tải thông tin hiện tại của bài viết lên biểu mẫu chỉnh sửa.
   - Bước 3: Administrator thay đổi nội dung, điều chỉnh nguyên liệu, đổi danh mục hoặc loại ăn chay, và nhấn "Lưu thay đổi".
   - Bước 4: Hệ thống xác thực dữ liệu, cập nhật bản ghi trong cơ sở dữ liệu và phản hồi thành công.

##### C. Luồng An ninh & Kiểm soát phân quyền (Security Flow)
1. **Main Flow:**
   - Khi có bất kỳ yêu cầu tạo hoặc sửa bài công thức hệ thống được gửi tới máy chủ, máy chủ kiểm tra vai trò của người gọi.
   - Nếu người gọi không có vai trò Administrator (ví dụ là Guest hoặc Member), máy chủ lập tức từ chối và phản hồi lỗi không có quyền truy cập (HTTP 403 Forbidden).

#### 6. Hậu điều kiện (Postconditions)
- Bài công thức chuẩn được lưu trữ thành công và hiển thị công khai cho toàn bộ người dùng.
- Thông tin phân loại danh mục và loại ăn chay của bài viết được cập nhật đồng bộ trong bộ lọc tìm kiếm.

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Chỉ tài khoản có vai trò Administrator mới có quyền truy cập các chức năng tạo và sửa bài công thức hệ thống (NFR-09).
- Dữ liệu hình ảnh được lưu trữ an toàn trên dịch vụ lưu trữ tệp đám mây Azure Blob Storage.
- Công thức do Admin tạo được gắn nhãn định danh tác giả hệ thống theo BR-17.

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản đăng bài.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và truyền tải an toàn.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền trả về 403.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-07.1 — Administrator tạo bài công thức chuẩn thành công với đầy đủ thông tin:**
  - **Given:** Administrator đang đăng nhập và ở màn hình Tạo bài công thức hệ thống.
  - **When:** Administrator nhập đầy đủ tiêu đề, chọn loại ăn chay `Vegan`, chọn danh mục, nhập nguyên liệu, các bước nấu và nhấn "Công khai bài viết".
  - **Then:** Hệ thống lưu bài viết thành công, gắn nhãn tác giả hệ thống, và hiển thị bài viết trên trang công khai.

- **AC-07.2 — Administrator cập nhật thành công bài công thức chuẩn đã tồn tại:**
  - **Given:** Một bài công thức chuẩn của hệ thống đang tồn tại.
  - **When:** Administrator chỉnh sửa lại tiêu đề và thời gian nấu của bài viết, rồi nhấn "Lưu thay đổi".
  - **Then:** Hệ thống cập nhật các thông tin mới vào cơ sở dữ liệu và phản ánh ngay lập tức trên giao diện công khai.

- **AC-07.3 — Phân loại chính xác bài công thức theo loại ăn chay và danh mục:**
  - **Given:** Administrator đang tạo hoặc chỉnh sửa bài công thức chuẩn.
  - **When:** Administrator chọn loại ăn chay `Lacto Vegetarian` và danh mục "Món kho".
  - **Then:** Hệ thống ghi nhận đúng các nhãn phân loại, cho phép người dùng lọc chính xác bài viết này theo các tiêu chí trên tại trang tìm kiếm.

- **AC-07.4 — Chặn người dùng không có vai trò Admin truy cập chức năng quản trị công thức:**
  - **Given:** Người dùng đang đăng nhập với vai trò Member thông thường (không phải Admin).
  - **When:** Người dùng cố tình gửi yêu cầu tạo hoặc chỉnh sửa bài công thức hệ thống tới máy chủ.
  - **Then:** Máy chủ kiểm tra quyền hạn, từ chối xử lý và phản hồi mã lỗi HTTP 403 Forbidden.

---

<a id="fr-08"></a>
### FR-08 — Tìm kiếm và lọc bài công thức đa tiêu chí

- **Mã yêu cầu:** FR-08
- **Module:** M04 (Recipe Catalog & Search)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống cung cấp công cụ tìm kiếm và bộ lọc đa tiêu chí (từ khóa, loại ăn chay chuẩn, danh mục món ăn, nguyên liệu, thời gian nấu tối đa) kết hợp sắp xếp kết quả (mới nhất, nhiều lượt Thích nhất) cho toàn bộ người dùng (Guest, Member, Administrator).

#### 1. Mục đích
Giúp người dùng nhanh chóng tìm thấy các bài công thức chay phù hợp với nhu cầu ăn uống cụ thể, sở thích cá nhân, nguyên liệu sẵn có trong gia đình hoặc quỹ thời gian nấu nướng; tối ưu hóa trải nghiệm khám phá ẩm thực chay trên nền tảng.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Guest` / `Member` / `Administrator`: Mọi người dùng có nhu cầu tìm kiếm và chọn lọc công thức món ăn.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-08.1`: Tìm kiếm bài công thức theo từ khóa (Search recipes by keyword).
  - `UC-08.2`: Lọc bài công thức theo đa tiêu chí (Filter recipes by multiple criteria).
  - `UC-08.3`: Sắp xếp danh sách kết quả tìm kiếm (Sort recipe search results).
- **User Stories:**
  - `US-08.1`: Là một người ăn chay thuần (Vegan), tôi muốn lọc các công thức theo nhãn "Thuần chay" để không phải tự kiểm tra thủ công xem món ăn có chứa sữa hay trứng không.
  - `US-08.2`: Là một người bận rộn, tôi muốn lọc món ăn có thời gian nấu dưới 30 phút và sắp xếp theo số lượt Thích cao nhất để nhanh chóng chọn được món ngon và tiện lợi.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Các bài công thức đang ở trạng thái công khai (`PUBLISHED`) và không bị ẩn do vi phạm.
- **Trigger:** Người dùng nhập từ khóa vào thanh tìm kiếm, chọn các tiêu chí lọc trong bảng bộ lọc, hoặc thay đổi tùy chọn sắp xếp.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Tìm kiếm và Lọc đa tiêu chí (UC-08.1, UC-08.2, UC-08.3)
1. **Main Flow:**
   - Bước 1: Người dùng truy cập trang Tìm kiếm / Khám phá công thức.
   - Bước 2: Người dùng có thể kết hợp các thao tác:
     - Nhập từ khóa tìm kiếm (tìm theo tên món, mô tả hoặc nguyên liệu).
     - Chọn 1 trong 4 loại ăn chay chuẩn: `Vegan`, `Lacto Vegetarian`, `Ovo Vegetarian`, `Lacto-Ovo Vegetarian`.
     - Chọn danh mục món ăn (ví dụ: Món canh, Món kho, Món xào, Món lẩu, v.v.).
     - Chọn giới hạn thời gian nấu tối đa ($\le 15$ phút, $\le 30$ phút, $\le 60$ phút, hoặc trên 60 phút).
     - Chọn tiêu chí sắp xếp: "Mới nhất" (thời gian đăng giảm dần) hoặc "Nhiều lượt Thích nhất" (tổng Like giảm dần).
   - Bước 3: Người dùng nhấn "Tìm kiếm" hoặc áp dụng bộ lọc.
   - Bước 4: Hệ thống thực thi truy vấn kết hợp các điều kiện lọc theo phép giao (AND logic giữa các nhóm tiêu chí khác nhau), chỉ lấy các bài công thức đang ở trạng thái `PUBLISHED`.
   - Bước 5: Hệ thống phản hồi danh sách kết quả kèm thông tin phân trang với thời gian phản hồi $\le 1.5$ giây (P95 theo NFR-02).
2. **Alternative Flow (Không tìm thấy kết quả phù hợp):**
   - Nếu không có bài viết nào thỏa mãn đồng thời tất cả các tiêu chí đã chọn, hệ thống hiển thị thông báo "Không tìm thấy công thức nào phù hợp" kèm nút bấm "Đặt lại bộ lọc" để người dùng dễ dàng thử lại.

#### 6. Hậu điều kiện (Postconditions)
- Danh sách các bài công thức phù hợp được hiển thị trực quan cho người dùng.
- Không hiển thị bất kỳ bài viết nào đang ở trạng thái bị ẩn hoặc đã xóa.

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Tính năng tìm kiếm và lọc là dịch vụ công khai mở cho mọi người dùng (Guest, Member, Admin).
- Truy vấn tìm kiếm bắt buộc phải được xử lý an toàn, sử dụng parameterized query hoặc ORM an toàn để phòng chống tấn công SQL Injection (NFR-10).
- Bảo vệ hiệu năng hệ thống: Áp dụng giới hạn số lượng kết quả trên mỗi trang (phân trang chuẩn 12–24 bài/trang) để tránh quá tải bộ nhớ.

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian phản hồi tìm kiếm công thức $\le 1.5$ giây (P95).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống lỗ hổng OWASP Top 10 (SQL Injection).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt, thích ứng đa thiết bị.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-08.1 — Tìm kiếm theo từ khóa phản hồi nhanh chóng:**
  - **Given:** Người dùng đang ở trang Tìm kiếm công thức.
  - **When:** Người dùng nhập từ khóa "đậu phụ sốt cà" và thực hiện tìm kiếm.
  - **Then:** Hệ thống trả về danh sách các công thức có tên hoặc mô tả chứa từ khóa với thời gian đáp ứng $\le 1.5$ giây (P95).

- **AC-08.2 — Lọc chính xác theo loại ăn chay chuẩn:**
  - **Given:** Kho dữ liệu có nhiều bài công thức với các loại ăn chay khác nhau.
  - **When:** Người dùng chọn bộ lọc loại ăn chay là `Vegan`.
  - **Then:** Danh sách kết quả chỉ chứa 100% các bài viết được gắn nhãn `Vegan`, loại trừ hoàn toàn các món ăn có sữa hoặc trứng.

- **AC-08.3 — Lọc chính xác theo danh mục món ăn:**
  - **Given:** Người dùng đang ở màn hình Khám phá công thức.
  - **When:** Người dùng chọn danh mục "Món canh".
  - **Then:** Hệ thống chỉ hiển thị các bài công thức thuộc danh mục "Món canh".

- **AC-08.4 — Lọc chính xác theo thời gian nấu tối đa:**
  - **Given:** Người dùng đang thiết lập bộ lọc tìm kiếm.
  - **When:** Người dùng chọn tiêu chí thời gian nấu "$\le 30$ phút".
  - **Then:** Toàn bộ các công thức trong kết quả đều có tổng thời gian nấu $\le 30$ phút.

- **AC-08.5 — Sắp xếp kết quả theo bài đăng mới nhất:**
  - **Given:** Có nhiều bài công thức thỏa mãn điều kiện lọc.
  - **When:** Người dùng chọn sắp xếp theo "Mới nhất".
  - **Then:** Kết quả được hiển thị theo thứ tự thời gian công khai giảm dần.

- **AC-08.6 — Sắp xếp kết quả theo số lượt Thích cao nhất:**
  - **Given:** Có nhiều bài công thức thỏa mãn điều kiện lọc.
  - **When:** Người dùng chọn sắp xếp theo "Nhiều lượt Thích nhất".
  - **Then:** Kết quả được hiển thị theo thứ tự tổng số lượt Thích giảm dần.

- **AC-08.7 — Xử lý khi không có kết quả tìm kiếm phù hợp:**
  - **Given:** Người dùng kết hợp các tiêu chí lọc quá hẹp mà không có công thức nào thỏa mãn.
  - **When:** Hệ thống thực thi tìm kiếm.
  - **Then:** Giao diện hiển thị thông báo "Không tìm thấy công thức phù hợp" kèm tùy chọn đặt lại bộ lọc.

---

<a id="fr-09"></a>
### FR-09 — Authorized User tạo và chỉnh lịch ăn tuần

- **Mã yêu cầu:** FR-09
- **Module:** M05
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp tính năng lập và chỉnh sửa kế hoạch ăn uống theo tuần dành cho Authorized User (Member đã đăng nhập); hỗ trợ xếp các bài công thức nấu ăn vào cấu trúc 3 bữa ăn cố định mỗi ngày (Bữa sáng, Bữa trưa, Bữa tối) cho 7 ngày trong tuần; khi bài công thức được tham chiếu bị tác giả xóa hoặc bị Administrator ẩn, hệ thống bảo toàn mục lịch ăn ở trạng thái không khả dụng (Tombstone) thay vì bị xóa liên đới (cascade-delete).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Người dùng đã đăng nhập tài khoản Member (FR-03).
  - Cấu trúc lịch ăn: 7 ngày trong tuần (Thứ Hai đến Chủ Nhật), mỗi ngày chia đúng 3 bữa: Sáng, Trưa, Tối (BR-36).
  - Không áp dụng: Guest không có quyền lưu lịch ăn (BR-05, BR-32).
- **Phân loại Actor:**
  - Primary Actor: `Member / Authorized User` (người dùng quản lý lịch ăn cá nhân).
  - Supporting Actor: `Hệ thống quản lý Lịch ăn tuần`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-09.1`: Tạo mới hoặc mở xem Lịch ăn cho tuần hiện tại hoặc các tuần tiếp theo.
  - `UC-09.2`: Thêm, đổi hoặc gỡ bỏ bài công thức trong từng bữa ăn (Sáng, Trưa, Tối) của các ngày trong tuần.
  - `UC-09.3`: Xem thông báo trạng thái Tombstone khi món ăn đã lưu trong lịch bị xóa hoặc tạm ẩn.
- **User Stories:**
  - *Là một người ăn chay bận rộn*, tôi muốn lên trước thực đơn 3 bữa cho cả tuần để chủ động chuẩn bị nguyên liệu và duy trì chế độ ăn chay đều đặn mà không phải nghĩ hôm nay ăn gì.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản Member hợp lệ (FR-03, BR-32).
- **Kích hoạt (Trigger):**
  - Member truy cập vào mục "Lịch ăn tuần" (Meal Planner) từ thanh điều hướng chính.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member mở trang Lịch ăn tuần. Hệ thống tải dữ liệu lịch ăn của tuần hiện tại (mặc định từ Thứ Hai đến Chủ Nhật).
  - Bước 2: Tại mỗi ngày, hệ thống trình bày lưới 3 ô bữa ăn cố định: Sáng, Trưa, Tối (BR-36).
  - Bước 3: Member nhấn nút "Thêm món" tại một bữa ăn cụ thể.
  - Bước 4: Hệ thống hiển thị hộp thoại chọn món từ Danh sách công thức đã lưu (FR-32) hoặc tìm kiếm nhanh từ kho công thức công khai (FR-08, FR-20).
  - Bước 5: Member chọn một công thức công khai hợp lệ.
  - Bước 6: Hệ thống kiểm tra tính duy nhất: Một công thức không được trùng lặp trong cùng một bữa ăn của cùng một ngày (BR-37).
  - Bước 7: Hệ thống ghi nhận liên kết món ăn vào bữa ăn của ngày được chọn với số khẩu phần mặc định là 1 (FR-37).
  - Bước 8: Giao diện cập nhật thẻ món ăn vào ô bữa ăn tương ứng ngay lập tức.
- **Luồng thay thế (Alternative Flows):**
  - *AF-09.1 (Gỡ món khỏi bữa ăn):* Member nhấn biểu tượng xóa/gỡ món trên thẻ món trong bữa ăn. Hệ thống gỡ liên kết món ăn khỏi bữa ăn đó mà hoàn toàn không ảnh hưởng đến bài viết gốc hay danh sách công thức đã lưu (BR-35).
  - *AF-09.2 (Đổi món ăn):* Member nhấn nút "Đổi món", chọn một công thức khác thay thế. Hệ thống cập nhật món mới vào vị trí bữa ăn.
  - *AF-09.3 (Xử lý bài viết gốc bị xóa hoặc ẩn - Tombstone):* Khi Member xem lịch ăn của tuần có chứa bài công thức đã bị tác giả xóa (FR-44) hoặc bị Administrator ẩn do vi phạm (BR-26), hệ thống giữ nguyên vị trí ô lịch đó nhưng hiển thị thẻ xám ghi rõ: *"Công thức không còn khả dụng"* và vô hiệu hóa link xem chi tiết (BR-35).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-09.1 (Guest truy cập lịch ăn):* Nếu người dùng chưa đăng nhập cố gắng truy cập trang Lịch ăn, hệ thống điều hướng đến trang Đăng nhập và hiển thị thông báo yêu cầu đăng nhập (BR-05, BR-32).
  - *SF-09.1 (Kiểm soát quyền riêng tư lịch ăn):* Lịch ăn tuần thuộc quyền riêng tư của tài khoản Member; hệ thống áp dụng phân quyền RBAC nghiêm ngặt, tuyệt đối không cho phép người dùng khác xem hoặc chỉnh sửa lịch ăn của tài khoản (NFR-08, NFR-09).

#### 5. Hậu điều kiện (Postconditions)
- Kế hoạch ăn uống trong tuần của Member được lưu trữ an toàn trong cơ sở dữ liệu.
- Dữ liệu lịch ăn sẵn sàng làm đầu vào cho chức năng Kiểm tra dinh dưỡng ngày (FR-37) và Tạo danh sách mua sắm (FR-53).

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Độc quyền cho Authorized User (Member sở hữu tài khoản).
- **Ràng buộc nghiệp vụ:** Cố định 3 bữa/ngày trong MVP (BR-36); thao tác trên lịch ăn hoàn toàn độc lập với danh sách công thức đã lưu (BR-35) và không tiêu tốn hạn mức AI (BR-33).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Lịch ăn.
  - [BR-33](BUSINESS-RULES.md#br-33): Thao tác lưu công thức không tiêu thụ hạn mức AI.
  - [BR-35](BUSINESS-RULES.md#br-35): Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn.
  - [BR-36](BUSINESS-RULES.md#br-36): Quy tắc 3 loại bữa ăn cố định trong Lịch ăn MVP.
  - [BR-37](BUSINESS-RULES.md#br-37): Tính duy nhất của công thức trong cùng một bữa ăn ngày.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-09.1 (Cấu trúc lịch tuần 7 ngày và 3 bữa cố định):**
  - *Given* Member đã đăng nhập truy cập trang Lịch ăn tuần,
  - *When* giao diện lịch ăn hiển thị,
  - *Then* hệ thống trình bày lưới 7 ngày trong tuần với đúng 3 bữa ăn cố định (Sáng, Trưa, Tối) cho mỗi ngày.
- **AC-09.2 (Thêm công thức vào bữa ăn thành công):**
  - *Given* Member chọn một công thức công khai để thêm vào Bữa trưa Thứ Ba,
  - *When* công thức chưa có trong Bữa trưa ngày hôm đó,
  - *Then* hệ thống ghi nhận món ăn vào Bữa trưa Thứ Ba và hiển thị thẻ món thành công.
- **AC-09.3 (Ngăn chặn trùng lặp công thức trong cùng một bữa ăn):**
  - *Given* Bữa trưa Thứ Ba đã có món "Canh chua chay",
  - *When* Member cố gắng thêm tiếp món "Canh chua chay" vào chính Bữa trưa Thứ Ba đó,
  - *Then* hệ thống từ chối thêm và hiển thị thông báo món ăn đã có trong bữa này.
- **AC-09.4 (Bảo toàn Tombstone khi bài viết gốc bị xóa):**
  - *Given* một món ăn trong lịch ăn có bài công thức gốc bị tác giả xóa,
  - *When* Member mở xem Lịch ăn tuần,
  - *Then* ô lịch ăn vẫn giữ nguyên vị trí, hiển thị nhãn "Công thức không còn khả dụng" và không gây lỗi sập trang.
- **AC-09.5 (Bảo vệ quyền riêng tư lịch ăn):**
  - *Given* Member A đăng nhập vào hệ thống,
  - *When* Member A cố gắng truy xuất lịch ăn của Member B,
  - *Then* hệ thống từ chối truy cập và phản hồi mã lỗi phân quyền.

---

<a id="fr-10"></a>
### FR-10 — Áp dụng hạn mức Gemini AI theo gói tài khoản

- **Mã yêu cầu:** FR-10
- **Module:** M06
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống kiểm soát và áp dụng hạn mức sử dụng dịch vụ Google Gemini AI theo các gói tài khoản đã được phê duyệt trong Phase 1: Guest Free (5 lượt/ngày), Free Member (5 lượt/ngày), Plus Member (15 lượt/ngày), và Pro Member (50 lượt/ngày); hạn mức được tính dựa trên số lượng request AI thành công; chu kỳ làm mới (reset) cố định vào 00:00 hàng ngày theo múi giờ `Asia/Ho_Chi_Minh`; kiểm tra số dư hạn mức trước khi gọi provider; thông báo cảnh báo và hướng dẫn nâng cấp khi người dùng đạt giới hạn.
- **Phạm vi nghiệp vụ:**
  - Hạn mức theo gói:
    | Gói tài khoản | Hạn mức AI thành công | Định danh theo dõi | Cơ chế bảo vệ |
    |---|---|---|---|
    | **Guest Free** | **5 request / ngày** | Anonymous Cookie + Coarse IP | Rate limit IP, câu hỏi $\le 200$ ký tự |
    | **Free Member** | **5 request / ngày** | Tài khoản Member (Account ID) | Reset 00:00 `Asia/Ho_Chi_Minh` |
    | **Plus Member** | **15 request / ngày** | Tài khoản Member (Account ID) | Reset 00:00 `Asia/Ho_Chi_Minh` |
    | **Pro Member** | **50 request / ngày** | Tài khoản Member (Account ID) | Reset 00:00 `Asia/Ho_Chi_Minh` |
  - Nguyên tắc trừ quota: Chỉ trừ lượt khi backend nhận phản hồi hợp lệ từ Gemini (BR-03); request lỗi mạng hoặc timeout không tiêu thụ quota (BR-04).
- **Phân loại Actor:**
  - Primary Actor: `Guest`, `Member` (Free, Plus, Pro).
  - Supporting Actor: `Hệ thống kiểm soát Quota và Rate Limiting`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-10.1`: Xem số dư hạn mức AI khả dụng trong ngày và thời điểm làm mới tiếp theo.
  - `UC-10.2`: Nhận cảnh báo khi đạt giới hạn hạn mức AI trong ngày kèm hướng dẫn nâng cấp gói hoặc thời điểm reset.
- **User Stories:**
  - *Là một người dùng*, tôi muốn biết rõ số lượt sử dụng AI còn lại trong ngày của gói tài khoản của mình để có kế hoạch tra cứu hợp lý và biết khi nào hạn mức sẽ được làm mới.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng gửi yêu cầu sử dụng bất kỳ tính năng AI nào (AI Chatbot FR-51, gợi ý thực đơn, hỗ trợ soạn bài).
- **Kích hoạt (Trigger):**
  - Hệ thống tiếp nhận yêu cầu gọi AI tại tầng Gateway/Controller trước khi chuyển tiếp tới dịch vụ Gemini.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Người dùng gửi yêu cầu gọi AI.
  - Bước 2: Hệ thống xác định danh tính và hạng gói của người dùng:
    - Nếu là Guest: Lấy Anonymous Cookie kết hợp dải mạng IP (BR-01).
    - Nếu là Member: Lấy Account ID và hạng gói hiện tại (Free, Plus hoặc Pro) (BR-01, BR-02).
  - Bước 3: Hệ thống truy vấn bộ đếm số lượt AI thành công đã dùng trong ngày (tính từ 00:00 `Asia/Ho_Chi_Minh` gần nhất).
  - Bước 4: Hệ thống so sánh số lượt đã dùng với định mức của gói:
    - Guest: $\text{đã dùng} < 5$.
    - Free: $\text{đã dùng} < 5$.
    - Plus: $\text{đã dùng} < 15$.
    - Pro: $\text{đã dùng} < 50$.
  - Bước 5: Nếu còn hạn mức, hệ thống cho phép yêu cầu đi tiếp tới module tích hợp Google Gemini AI.
  - Bước 6: Sau khi Gemini phản hồi hợp lệ thành công, hệ thống tăng bộ đếm số lượt thành công lên 1 đơn vị (BR-03).
  - Bước 7: Trả về kết quả cho người dùng kèm số dư hạn mức còn lại trong ngày.
- **Luồng thay thế (Alternative Flows):**
  - *AF-10.1 (Đạt giới hạn hạn mức trong ngày):*
    - Khi số lượt đã dùng bằng định mức của gói, hệ thống chặn gửi yêu cầu tới Gemini.
    - Hệ thống phản hồi thông báo đạt hạn mức:
      - Đối với Guest: *"Bạn đã dùng hết 5 lượt AI hôm nay. Vui lòng đăng ký tài khoản hoặc quay lại sau 00:00."*
      - Đối với Free Member: *"Bạn đã sử dụng hết 5 lượt AI hôm nay. Vui lòng nâng cấp lên gói Plus (15 lượt) hoặc Pro (50 lượt), hoặc quay lại sau 00:00."* (kèm nút dẫn tới trang Đăng ký gói FR-13).
      - Đối với Plus/Pro: *"Bạn đã sử dụng hết hạn mức AI trong ngày. Hạn mức sẽ được làm mới lúc 00:00."*
  - *AF-10.2 (Làm mới hạn mức lúc nửa đêm):*
    - Vào đúng 00:00 theo múi giờ `Asia/Ho_Chi_Minh`, bộ đếm số lượt gọi AI của tất cả tài khoản tự động được đặt lại về 0 cho ngày mới.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-10.1 (Lỗi kết nối Gemini hoặc timeout):* Nếu yêu cầu gọi AI gặp lỗi từ nhà cung cấp hoặc quá thời gian chờ, hệ thống không tăng bộ đếm và không trừ hạn mức của người dùng (BR-04).
  - *SF-10.1 (Chống spam và lạm dụng qua IP):* Đối với Guest, hệ thống áp dụng cơ chế coarse IP rate limiting để ngăn chặn việc xóa cookie liên tục nhằm vượt qua hạn mức 5 lượt/ngày (BR-01, NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Số lượt gọi AI thành công được ghi nhận chính xác theo tài khoản hoặc cookie định danh.
- Không phát sinh chi phí gọi provider khi tài khoản đã hết hạn mức.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Áp dụng tự động cho mọi đối tượng người dùng hệ thống.
- **Ràng buộc kinh doanh:** Không cấp quyền vô hạn định; bảo vệ ngân sách API vận hành hệ thống theo đúng chính sách Phase 1.

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Hạn mức text AI cho Guest và tài khoản Free (5 lượt/ngày).
  - [BR-02](BUSINESS-RULES.md#br-02): Hạn mức text AI cho gói Plus (15 lượt) và Pro (50 lượt).
  - [BR-03](BUSINESS-RULES.md#br-03): Điều kiện trừ hạn mức AI (chỉ trừ khi thành công).
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider và timeout AI (không trừ quota).
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-06](BUSINESS-RULES.md#br-06): Bảo mật Gemini API Key.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03): Thời gian phản hồi của tính năng AI Chatbot.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống lạm dụng và lỗ hổng bảo mật.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-10.1 (Hạn mức Guest và Free đúng 5 lượt/ngày):**
  - *Given* một người dùng Guest hoặc tài khoản Member Free có 0 lượt dùng trong ngày,
  - *When* thực hiện thành công 5 request AI trong cùng một ngày,
  - *Then* hệ thống cho phép thực hiện đủ 5 lượt và chặn request thứ 6 kèm thông báo đạt giới hạn.
- **AC-10.2 (Hạn mức gói Plus đúng 15 lượt/ngày):**
  - *Given* một tài khoản Member thuộc gói Plus,
  - *When* tài khoản thực hiện thành công 15 request AI trong ngày,
  - *Then* hệ thống ghi nhận đúng 15 lượt và từ chối request thứ 16 kèm thông báo đạt giới hạn.
- **AC-10.3 (Hạn mức gói Pro đúng 50 lượt/ngày):**
  - *Given* một tài khoản Member thuộc gói Pro,
  - *When* tài khoản thực hiện thành công 50 request AI trong ngày,
  - *Then* hệ thống ghi nhận đúng 50 lượt và từ chối request thứ 51 kèm thông báo đạt giới hạn.
- **AC-10.4 (Tự động reset hạn mức lúc 00:00 Asia/Ho_Chi_Minh):**
  - *Given* tài khoản đã sử dụng hết hạn mức trong ngày,
  - *When* đồng hồ hệ thống chuyển sang 00:00 theo múi giờ `Asia/Ho_Chi_Minh`,
  - *Then* bộ đếm số lượt đã dùng của tài khoản được đặt lại về 0 và tài khoản có thể tiếp tục sử dụng AI.
- **AC-10.5 (Không trừ hạn mức khi Gemini gặp lỗi hoặc timeout):**
  - *Given* tài khoản Free còn 2 lượt sử dụng AI,
  - *When* gửi yêu cầu gọi AI nhưng dịch vụ AI gặp sự cố kỹ thuật hoặc quá thời gian phản hồi quy định tại NFR-03,
  - *Then* hệ thống hiển thị thông báo lỗi kỹ thuật và số dư hạn mức của tài khoản vẫn giữ nguyên là 2 lượt.
- **AC-10.6 (Chặn trước khi gọi provider khi hết quota):**
  - *Given* tài khoản đã hết hạn mức trong ngày,
  - *When* người dùng bấm gửi yêu cầu AI,
  - *Then* hệ thống chặn ngay tại tầng kiểm tra nội bộ mà không tạo bất kỳ HTTP request nào tới Google Gemini API.

---

<a id="fr-11"></a>
### FR-11 — Lưu lượt gọi AI thành công và token usage metadata

- **Mã yêu cầu:** FR-11
- **Module:** M06
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống thu thập và lưu trữ dữ liệu đo lường kỹ thuật (AI Usage Telemetry & Accounting) cho mọi lượt gọi Gemini AI thành công, bao gồm số lượt gọi và siêu dữ liệu lượng token tiêu thụ (prompt tokens, response tokens, total tokens) do nhà cung cấp Gemini trả về nhằm đo lường mức độ sử dụng tài nguyên thực tế và đối soát chi phí vận hành; hệ thống TUYỆT ĐỐI KHÔNG lưu trữ nội dung câu hỏi thô (raw prompt content) nhằm bảo vệ quyền riêng tư người dùng; thời hạn lưu trữ dữ liệu đo lường là 90 ngày; các lỗi từ nhà cung cấp không tiêu thụ quota và không được ghi nhận là lượt dùng thành công.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi request gọi Gemini AI thành công từ các tính năng: AI Chatbot theo ngữ cảnh (FR-51), AI gợi ý thực đơn (FR-34, FR-36), AI hỗ trợ tạo bài (FR-21), Gợi ý món biến tấu (FR-47).
  - Bảo mật dữ liệu: Tuyệt đối cấm lưu trữ nội dung văn bản câu hỏi của người dùng trong bảng dữ liệu đo lường (BR-04, NFR-08, NFR-20).
  - Vòng đời lưu trữ: Lưu trữ tối đa 90 ngày (BR-04); tự động dọn dẹp các bản ghi quá hạn.
- **Phân loại Actor:**
  - Primary Actor: `Administrator` (người dùng quản trị xem báo cáo thống kê mức độ tiêu thụ token và chi phí AI).
  - Supporting System / Mechanism: `Hệ thống đo lường và ghi vết AI (AI Telemetry Subsystem)` (tiến trình ghi nhận dữ liệu kỹ thuật và dọn dẹp ngầm).

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-11.2`: Administrator xem bảng thống kê lượng tiêu thụ token và chi phí ước tính theo chu kỳ (View AI token consumption and cost statistics).
  *(Ghi chú: UC-11.1 trước đây về ghi nhận telemetry sau lượt gọi AI và UC-11.3 về tự động thanh lọc dữ liệu sau 90 ngày không còn là các Use Case độc lập vì đây là các cơ chế kỹ thuật ngầm của hệ thống chứ không phải mục tiêu của Actor; hai hành vi này được chuẩn hóa tương ứng thành Cơ chế ghi nhận telemetry hệ thống sau lượt gọi AI và Luồng dọn dẹp dữ liệu ngầm theo chu kỳ).*
- **User Stories:**
  - *Là một Administrator quản trị hệ thống*, tôi muốn theo dõi tổng số token tiêu thụ thực tế của từng tính năng AI mà không lưu trữ nội dung riêng tư của người dùng, để tôi có thể ước tính chi phí API hàng tháng và tối ưu hóa hệ thống.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Một request gọi Google Gemini API hoàn tất thành công và trả về mã trạng thái HTTP 200 kèm payload kết quả hợp lệ.
- **Kích hoạt (Trigger):**
  - Module AI client nhận được phản hồi thành công từ Google Gemini API, hoặc Administrator truy cập trang thống kê tiêu thụ token.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng nghiệp vụ Actor — Administrator xem thống kê tiêu thụ (UC-11.2):**
  - Bước 1: Administrator truy cập trang Thống kê kỹ thuật trên giao diện quản trị (M06, M09).
  - Bước 2: Hệ thống truy vấn dữ liệu telemetry tổng hợp, hiển thị biểu đồ và bảng dữ liệu tổng lượng token tiêu thụ theo ngày, tuần, tháng và phân bổ theo từng tính năng (Chatbot, Lập thực đơn, Gợi ý công thức).
  - Bước 3: Administrator xem xét số liệu và chi phí ước tính phục vụ lập kế hoạch vận hành.
- **Hành vi hệ thống — Ghi nhận siêu dữ liệu telemetry khi gọi AI thành công:**
  - Bước 1: Google Gemini API trả về phản hồi hợp lệ cho backend.
  - Bước 2: Hệ thống trích xuất siêu dữ liệu sử dụng token (`usageMetadata`) từ đối tượng phản hồi của Gemini, gồm: `promptTokenCount`, `candidatesTokenCount`, và `totalTokenCount`.
  - Bước 3: Hệ thống chuẩn bị bản ghi đo lường (Telemetry Record) gồm: Mã tài khoản người dùng (hoặc Anonymous Session ID đối với Guest), Loại tính năng được gọi (Menu, Chatbot, Authoring), Dấu thời gian (Timestamp UTC), và Số lượng token tiêu thụ.
  - Bước 4: Hệ thống TUYỆT ĐỐI LOẠI BỎ toàn bộ nội dung văn bản câu hỏi thô (raw prompt content) và câu trả lời thô khỏi bản ghi telemetry (BR-04).
  - Bước 5: Hệ thống ghi bản ghi đo lường vào bảng nhật ký kỹ thuật trong cơ sở dữ liệu.
  - Bước 6: Trả kết quả nghiệp vụ về cho người dùng bình thường.
- **Luồng thay thế (Alternative Flows):**
  - *AF-11.1 (Gemini không trả về trường usageMetadata):* Nếu phản hồi từ Gemini thành công nhưng thiếu khối thông tin token, hệ thống vẫn ghi nhận bản ghi với số token = 0 và đánh dấu cờ kiểm toán kỹ thuật để không làm gián đoạn trải nghiệm người dùng.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-11.1 (Gọi AI thất bại hoặc timeout):* Nếu request tới Gemini bị lỗi mạng, timeout hoặc trả về mã lỗi 4xx/5xx, hệ thống không ghi nhận bản ghi đo lường thành công, không trừ quota, mà chỉ ghi nhật ký lỗi (Error Log) riêng biệt để phục vụ khắc phục sự cố (BR-03, BR-04).
  - *SF-11.1 (Bảo mật quyền riêng tư - Không lưu Prompt):* Các quy tắc kiểm tra tự động và mã nguồn bảo đảm trường nội dung prompt không bao giờ được đưa vào bảng telemetry, ngăn chặn rò rỉ dữ liệu cá nhân nhạy cảm (NFR-08, NFR-20).
  - *SF-11.2 (Chính sách thanh lọc dữ liệu định kỳ 90 ngày - Retention & Cleanup Behavior):* Một tiến trình ngầm (Background Job) chạy định kỳ hàng tuần tự động xóa các bản ghi telemetry có dấu thời gian cũ hơn 90 ngày (BR-04).

#### 5. Hậu điều kiện (Postconditions)
- Siêu dữ liệu đo lường token được lưu trữ an toàn, phục vụ đối soát.
- Không có bất kỳ nội dung văn bản câu hỏi thô nào của người dùng bị lưu giữ trong bảng đo lường kỹ thuật.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Dữ liệu đo lường kỹ thuật chỉ có Administrator mới có quyền xem báo cáo tổng hợp. Người dùng thông thường không có quyền truy cập.
- **Ràng buộc an toàn:** Tuân thủ thời hạn lưu trữ 90 ngày và cấm lưu trữ raw prompt (BR-04).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-03](BUSINESS-RULES.md#br-03): Điều kiện trừ hạn mức AI.
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider, timeout và thời hạn lưu telemetry 90 ngày không lưu raw prompt.
  - [BR-06](BUSINESS-RULES.md#br-06): Bảo mật Gemini API Key.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-05](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-05): Khả năng chịu tải đồng thời của hệ thống.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.
  - [NFR-20](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20): Cơ chế bảo vệ dữ liệu sức khỏe cá nhân và ranh giới thông tin.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-11.1 (Ghi nhận đầy đủ token metadata khi gọi AI thành công):**
  - *Given* một request gọi Gemini AI hoàn tất thành công với thông số tiêu thụ 150 prompt tokens và 300 response tokens,
  - *When* hệ thống ghi nhận dữ liệu đo lường,
  - *Then* bản ghi telemetry lưu trữ chính xác số lượng 150 prompt tokens, 300 response tokens và 450 total tokens kèm ID tính năng và dấu thời gian.
- **AC-11.2 (Tuyệt đối không lưu raw prompt content trong bảng telemetry):**
  - *Given* người dùng gửi câu hỏi chứa thông tin cá nhân tới AI,
  - *When* bản ghi đo lường được lưu vào cơ sở dữ liệu,
  - *Then* trường dữ liệu lưu trữ chỉ chứa số lượng token và siêu dữ liệu, hoàn toàn không chứa chuỗi ký tự văn bản câu hỏi thô của người dùng.
- **AC-11.3 (Không ghi nhận thành công khi gọi provider lỗi):**
  - *Given* một request gọi Gemini AI bị timeout hoặc gặp lỗi từ máy chủ Google,
  - *When* hệ thống xử lý ngoại lệ,
  - *Then* hệ thống không tạo bản ghi đo lường AI thành công và không trừ hạn mức quota của người dùng.
- **AC-11.4 (Tự động thanh lọc dữ liệu quá 90 ngày):**
  - *Given* các bản ghi đo lường AI có dấu thời gian được tạo cách đây 91 ngày,
  - *When* tiến trình dọn dẹp định kỳ thực thi,
  - *Then* các bản ghi cũ hơn 90 ngày được xóa khỏi cơ sở dữ liệu telemetry.
- **AC-11.5 (Phân quyền bảo mật dữ liệu đo lường):**
  - *Given* một người dùng Member thông thường cố gắng truy cập dữ liệu đo lường token AI,
  - *When* hệ thống kiểm tra quyền hạn,
  - *Then* hệ thống từ chối truy cập và chỉ cho phép tài khoản Administrator xem báo cáo tổng hợp.

---

<a id="fr-12"></a>
### FR-12 — AI rà soát và gắn cờ Recipe Post nghi vấn cho Administrator

- **Mã yêu cầu:** FR-12
- **Module:** M07
- **Trạng thái (Derived):** DEFERRED
- **Mô tả:** AI rà soát/gắn cờ Recipe Post nghi vấn và chỉ cung cấp tín hiệu cho Administrator; AI không tự áp dụng chế tài.

---

<a id="fr-13"></a>
### FR-13 — Đăng ký gói AI qua thanh toán thật

- **Mã yêu cầu:** FR-13
- **Module:** M08
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp tính năng đăng ký và thanh toán trực tuyến gói dịch vụ AI cho Member thông qua cổng thanh toán thật: hỗ trợ 3 gói dịch vụ theo tháng thanh toán bằng đồng Việt Nam (VNĐ): FREE (0 VNĐ/tháng), PLUS (49.000 VNĐ/tháng) và PRO (99.000 VNĐ/tháng); trong phạm vi MVP hệ thống không tự động gia hạn (No auto-renewal) và không áp dụng chính sách hoàn tiền một phần (No partial refund); quyền lợi hạn mức AI của gói trả phí CHỈ ĐƯỢC KÍCH HOẠT sau khi giao dịch thanh toán thành công đã được xác minh toàn vẹn; quyền lợi tự động hết hạn khi kết thúc chu kỳ tháng đã trả phí (verified monthly paid period); việc xử lý các sự kiện thanh toán lặp (duplicate callback/IPN) phải bảo đảm tính idempotent; không hỗ trợ gói năm, mã giảm giá, khuyến mãi hay dùng thử.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập tài khoản hợp lệ (FR-03).
  - Bảng định giá và quyền lợi chính thức Phase 1:
    | Gói dịch vụ | Mức giá niêm yết | Hạn mức AI thành công | Thời hạn chu kỳ | Gia hạn tự động |
    |---|---|---|---|---|
    | **FREE** | **0 VNĐ / tháng** | 5 request / ngày | Không giới hạn | Không |
    | **PLUS** | **49.000 VNĐ / tháng** | 15 request / ngày | Chu kỳ tháng (Monthly) | **Không (Thanh toán từng kỳ)** |
    | **PRO** | **99.000 VNĐ / tháng** | 50 request / ngày | Chu kỳ tháng (Monthly) | **Không (Thanh toán từng kỳ)** |
  - Tiền tệ: Đồng Việt Nam (VNĐ) duy nhất.
  - Không hỗ trợ: Gói năm, coupon, giảm giá, trial, hoàn tiền một phần.
- **Phân loại Actor:**
  - Primary Actor: `Member` (người đăng ký mua gói).
  - Supporting Actor: `Cổng thanh toán trực tuyến`, `Hệ thống quản lý Subscription & Entitlement`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-13.1`: Xem bảng giá, quyền lợi và so sánh các gói dịch vụ (Free, Plus, Pro).
  - `UC-13.2`: Khởi tạo đơn hàng và thanh toán nâng cấp gói dịch vụ (Plus 49.000 VNĐ hoặc Pro 99.000 VNĐ).
  - `UC-13.3`: Xem thông tin trạng thái gói dịch vụ hiện tại và ngày hết hạn chu kỳ đã trả phí.
- **User Stories:**
  - *Là một người dùng thường xuyên cần AI lên thực đơn*, tôi muốn nâng cấp lên gói Plus 49.000 VNĐ/tháng một cách an toàn và minh bạch để có 15 lượt gọi AI mỗi ngày mà không bị tự động trừ tiền âm thầm vào tháng sau.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản Member hợp lệ (FR-03, BR-05).
- **Kích hoạt (Trigger):**
  - Member bấm nút "Nâng cấp gói" trên giao diện quản lý tài khoản hoặc từ thông báo hết hạn mức AI.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member truy cập trang Bảng giá dịch vụ, xem bảng so sánh: Free (0 VNĐ, 5 lượt/ngày), Plus (49.000 VNĐ/tháng, 15 lượt/ngày), Pro (99.000 VNĐ/tháng, 50 lượt/ngày).
  - Bước 2: Member chọn gói mong muốn (ví dụ gói Plus 49.000 VNĐ) và nhấn "Tiến hành thanh toán".
  - Bước 3: Hệ thống tạo một giao dịch thanh toán mới ở trạng thái `Pending` với mã giao dịch duy nhất (Transaction ID), lưu trữ số tiền (49.000 VNĐ), mã gói (PLUS), và ID tài khoản của Member.
  - Bước 4: Hệ thống tạo URL chuyển hướng an toàn kèm chữ ký số và điều hướng Member tới cổng thanh toán.
  - Bước 5: Member hoàn tất thanh toán trên giao diện cổng thanh toán.
  - Bước 6: Cổng thanh toán gửi thông báo kết quả giao dịch (Webhook / IPN) về máy chủ hệ thống kèm chữ ký số hợp lệ.
  - Bước 7: Hệ thống xác thực tính hợp lệ của chữ ký số, số tiền và trạng thái giao dịch:
    - Nếu chữ ký hợp lệ và giao dịch thành công: Hệ thống cập nhật trạng thái giao dịch sang `Success`.
    - Hệ thống kích hoạt quyền lợi (Entitlement) gói Plus cho Member: Nâng hạn mức AI lên 15 request/ngày (BR-02), thiết lập thời hạn hiệu lực của quyền lợi tương ứng với chu kỳ tháng đã trả phí được xác thực.
  - Bước 8: Hệ thống gửi thông báo xác nhận thanh toán thành công trong ứng dụng và qua email cho Member.
  - Bước 9: Member được chuyển hướng về trang thông tin tài khoản hiển thị gói dịch vụ hiện tại là `Plus` kèm ngày hết hạn.
- **Luồng thay thế (Alternative Flows):**
  - *AF-13.1 (Member hủy thanh toán hoặc giao dịch thất bại):* Nếu Member bấm hủy hoặc thanh toán không thành công tại cổng thanh toán, giao dịch được ghi nhận trạng thái `Cancelled` hoặc `Failed`. Quyền lợi tài khoản của Member giữ nguyên ở mức hiện tại; hệ thống hiển thị thông báo thanh toán chưa hoàn tất và cho phép thử lại.
  - *AF-13.2 (Hết hạn chu kỳ đã trả phí - Graceful Expiration):* Khi chu kỳ tháng đã trả phí kết thúc, quyền lợi Plus hoặc Pro tự động hết hạn. Hệ thống tự động chuyển gói tài khoản về `Free` (hạn mức 5 request/ngày) mà hoàn toàn không phát sinh thêm bất kỳ chi phí nào và không tự động trừ tiền gia hạn.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-13.1 (Xử lý thông báo thanh toán lặp - Idempotent Handling):* Nếu cổng thanh toán gửi nhiều lần thông báo IPN cho cùng một mã giao dịch đã xử lý thành công trước đó, hệ thống nhận diện mã giao dịch đã ở trạng thái `Success`, lập tức phản hồi xác nhận cho cổng thanh toán mà không cộng dồn thời hạn hay kích hoạt quyền lợi lần thứ hai.
  - *SF-13.1 (Phát hiện giả mạo dữ liệu thanh toán):* Nếu dữ liệu IPN/Callback có chữ ký số không khớp hoặc số tiền thanh toán không đúng với giá niêm yết của gói (ví dụ giả mạo số tiền 1.000 VNĐ thay vì 49.000 VNĐ), hệ thống từ chối kích hoạt quyền lợi, đánh dấu giao dịch `Tampered/Invalid` và ghi vết cảnh báo an ninh (NFR-10).
  - *SF-13.2 (Không lưu trữ thông tin thẻ ngân hàng):* Toàn bộ thao tác nhập thông tin thẻ/tài khoản ngân hàng diễn ra trực tiếp trên hạ tầng bảo mật của cổng thanh toán; hệ thống của dự án tuyệt đối không lưu trữ số thẻ tín dụng, mã CVV hay mật khẩu ngân hàng của người dùng (NFR-08, NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Quyền lợi hạn mức AI của Member được kích hoạt đúng theo gói dịch vụ đã thanh toán.
- Bản ghi giao dịch được lưu vết kiểm toán đầy đủ và minh bạch.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member đã đăng nhập mới có quyền thực hiện giao dịch thanh toán nâng cấp.
- **Ràng buộc Phase 1:** Không tự động gia hạn; không hoàn tiền một phần; chỉ VND; đúng mức giá 49.000 VNĐ (Plus) và 99.000 VNĐ (Pro); không có mã giảm giá hay gói năm.

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Hạn mức text AI cho tài khoản Free (0 VNĐ/tháng).
  - [BR-02](BUSINESS-RULES.md#br-02): Hạn mức text AI cho gói Plus (49.000 VNĐ) và Pro (99.000 VNĐ).
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-05](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-05): Khả năng chịu tải đồng thời của hệ thống.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật (chống giả mạo chữ ký IPN, chống IDOR).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-13.1 (Bảng giá hiển thị đúng chuẩn Phase 1):**
  - *Given* người dùng truy cập trang bảng giá dịch vụ,
  - *When* trang hiển thị,
  - *Then* hệ thống trình bày chính xác 3 gói: FREE 0 VNĐ/tháng, PLUS 49.000 VNĐ/tháng, PRO 99.000 VNĐ/tháng; không hiển thị gói năm, mã giảm giá hay gói dùng thử.
- **AC-13.2 (Kích hoạt quyền lợi chỉ sau khi thanh toán thành công):**
  - *Given* Member thanh toán gói Plus 49.000 VNĐ,
  - *When* hệ thống nhận được thông báo IPN thành công có chữ ký hợp lệ từ cổng thanh toán,
  - *Then* trạng thái tài khoản chuyển sang gói Plus với hạn mức 15 lượt AI/ngày tương ứng với chu kỳ tháng đã thanh toán.
- **AC-13.3 (Xử lý giao dịch lặp đảm bảo tính Idempotent):**
  - *Given* một giao dịch đã được hệ thống xử lý kích hoạt thành công,
  - *When* cổng thanh toán gửi lại bản tin IPN thứ hai cho cùng mã giao dịch đó,
  - *Then* hệ thống xác nhận xử lý thành công mà không cộng thêm thời hạn chu kỳ hay trừ tiền lặp.
- **AC-13.4 (Hết hạn chu kỳ chuyển về Free không tự gia hạn):**
  - *Given* gói Plus của Member đã hết chu kỳ tháng đã trả phí,
  - *When* thời điểm hết hạn kết thúc,
  - *Then* hệ thống tự động chuyển tài khoản về gói Free (5 lượt/ngày) và không thực hiện bất kỳ lệnh trừ tiền tự động nào.
- **AC-13.5 (Từ chối yêu cầu có chữ ký hoặc số tiền giả mạo):**
  - *Given* một bản tin thanh toán gửi về máy chủ có chữ ký số không hợp lệ hoặc số tiền sai lệch với giá gói,
  - *When* hệ thống xác thực dữ liệu giao dịch,
  - *Then* hệ thống từ chối kích hoạt quyền lợi và ghi log cảnh báo an ninh.

---

<a id="fr-14"></a>
### FR-14 — Lưu trữ ảnh bài công thức trên Azure Blob Storage

- **Mã yêu cầu:** FR-14
- **Module:** M03
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống cung cấp cơ chế lưu trữ tập trung toàn bộ các tệp tin hình ảnh của bài công thức nấu ăn (ảnh đại diện món ăn và ảnh minh họa từng bước nấu) trên dịch vụ đám mây Azure Blob Storage; lưu trữ siêu dữ liệu (metadata: URI truy cập an toàn, tên tệp gốc, dung lượng, loại MIME, quan hệ khóa ngoại với bài công thức hoặc bước nấu) trong cơ sở dữ liệu hệ thống; áp dụng cơ chế xác thực định dạng và dung lượng trước khi tải lên; thu hồi hoặc giải phóng tài nguyên ảnh khi công thức hoặc bước nấu bị xóa hoặc thay đổi; tuyệt đối không lưu tệp nhị phân ảnh trực tiếp trong cơ sở dữ liệu quan hệ.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi hình ảnh được tải lên khi tạo hoặc chỉnh sửa bài công thức bởi Member (FR-04, FR-21, FR-44) và Administrator (FR-07).
  - Định dạng hỗ trợ: JPEG, PNG, WebP; dung lượng tối đa $\le 5$ MB trên mỗi tệp tin (NFR-10).
- **Phân loại Actor:**
  - Primary Actor: `Member / Administrator` (tác giả tải ảnh lên).
  - Supporting Actor: `Azure Blob Storage` (dịch vụ lưu trữ đám mây).

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-14.1`: Tải tệp tin ảnh đại diện hoặc ảnh bước nấu lên Azure Blob Storage và nhận liên kết truy cập an toàn (Upload recipe images to Azure Blob Storage).
  - `UC-14.2`: Hiển thị hình ảnh từ Azure Blob Storage với tốc độ cao trên giao diện chi tiết công thức (Display recipe images from Azure Blob Storage).
  *(Ghi chú: UC-14.3 trước đây về tự động thu hồi/xóa tài nguyên ảnh không còn là Use Case độc lập vì đây là cơ chế quản lý vòng đời tài nguyên ngầm của hệ thống chứ không phải mục tiêu của Actor; hành vi này được chuẩn hóa thành Luồng quản lý vòng đời & giải phóng tài nguyên ảnh (Resource Lifecycle & Cleanup Flow) và Tiêu chí nghiệm thu tương ứng).*
- **User Stories:**
  - *Là một người chia sẻ công thức nấu ăn*, tôi muốn tải lên những bức ảnh chụp món ăn sắc nét mà không lo trang bị chậm hay lỗi máy chủ, để bài nấu ăn của tôi trông sinh động và hấp dẫn người xem.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản hợp lệ (FR-03).
  - Người dùng đang ở giao diện soạn thảo hoặc chỉnh sửa bài công thức nấu ăn.
- **Kích hoạt (Trigger):**
  - Tác giả chọn tệp tin ảnh từ thiết bị để làm ảnh đại diện hoặc ảnh minh họa bước nấu.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả chọn tệp ảnh từ máy tính hoặc thiết bị di động.
  - Bước 2: Hệ thống kiểm tra hợp lệ tại tầng ứng dụng: dung lượng tệp tin $\le 5$ MB, phần mở rộng và MIME type thuộc danh sách cho phép (image/jpeg, image/png, image/webp) (NFR-10).
  - Bước 3: Hệ thống sinh tên tệp duy nhất (sử dụng chuỗi định danh ngẫu nhiên UUID kết hợp timestamp) để tránh xung đột tên tệp trên bộ lưu trữ.
  - Bước 4: Hệ thống thực hiện truyền tải luồng dữ liệu (streaming upload) tệp tin lên vùng chứa (container) chuyên dụng trên Azure Blob Storage.
  - Bước 5: Azure Blob Storage xác nhận tải lên thành công và trả về URL định danh duy nhất (Blob URL) của tệp tin.
  - Bước 6: Hệ thống lưu bản ghi siêu dữ liệu của ảnh (Blob URL, kích thước, định dạng, quan hệ tham chiếu với bài viết/bước nấu) vào cơ sở dữ liệu.
  - Bước 7: Giao diện hiển thị bản xem trước (preview) hình ảnh cho tác giả ngay lập tức.
- **Luồng quản lý vòng đời & Giải phóng tài nguyên (Resource Lifecycle & Cleanup Flow):**
  - *Thu hồi ảnh khi thay thế ảnh mới:* Khi tác giả chọn tải ảnh khác thay cho ảnh hiện tại, hệ thống tải ảnh mới lên Azure Blob Storage, cập nhật liên kết mới trong cơ sở dữ liệu và đánh dấu bản ghi ảnh cũ vào hàng đợi thu hồi tài nguyên (cleanup job).
  - *Giải phóng tài nguyên khi xóa công thức/bước nấu:* Khi bài công thức bị xóa hoặc bước nấu bị xóa, hệ thống cập nhật trạng thái tham chiếu và giải phóng các Blob tương ứng trên Azure theo chính sách dọn dẹp tài nguyên.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-14.1 (Tệp tin vượt quá dung lượng cho phép):* Nếu tệp ảnh $> 5$ MB, hệ thống từ chối nhận tệp và hiển thị thông báo: *"Dung lượng ảnh không được vượt quá 5 MB"*.
  - *EF-14.2 (Định dạng tệp không hợp lệ hoặc chứa mã độc):* Nếu tệp tin không đúng định dạng ảnh hoặc có phần mở rộng bị cấm (như tệp thực thi), hệ thống từ chối tải lên và ghi log cảnh báo an ninh (NFR-10).
  - *EF-14.3 (Lỗi kết nối tới Azure Blob Storage):* Nếu dịch vụ lưu trữ đám mây phản hồi lỗi hoặc timeout, hệ thống thông báo lỗi tải ảnh thân thiện và cho phép người dùng thử lại mà không làm mất nội dung văn bản đang soạn thảo.
  - *SF-14.1 (Không lưu binary trong cơ sở dữ liệu):* Toàn bộ tệp nhị phân của hình ảnh được lưu trữ độc quyền trên Azure Blob Storage; cơ sở dữ liệu chỉ lưu trữ URL và metadata, tối ưu hóa kích thước cơ sở dữ liệu và tốc độ truy vấn (NFR-02).

#### 5. Hậu điều kiện (Postconditions)
- Tệp ảnh được lưu trữ an toàn và sẵn sàng phục vụ hiển thị công khai qua CDN/Azure Blob URL.
- Siêu dữ liệu ảnh được liên kết chính xác với Recipe Post tương ứng.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member chỉ được tải ảnh lên cho công thức của chính mình; Administrator có quyền tải ảnh cho công thức chính thức của hệ thống.
- **Ràng buộc lưu trữ:** Hình ảnh được công khai trực tiếp cùng bài viết không qua duyệt trước media (BR-07, BR-11); nếu vi phạm chuẩn mực sẽ bị xử lý qua cơ chế hậu kiểm (FR-06, FR-28).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-11](BUSINESS-RULES.md#br-11): Media công khai trực tiếp và hậu kiểm.
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của ảnh đại diện.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian tải trang hiển thị chi tiết bài viết $\le 2$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống lỗ hổng bảo mật (kiểm soát loại tệp upload, chống tải mã độc).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-14.1 (Giới hạn dung lượng và định dạng ảnh):**
  - *Given* tác giả chọn tệp tin ảnh có dung lượng lớn hơn 5 MB hoặc tệp tin không phải định dạng JPEG/PNG/WebP,
  - *When* tác giả bắt đầu tải lên,
  - *Then* hệ thống từ chối tải tệp tin và hiển thị thông báo lỗi yêu cầu định dạng hợp lệ và dung lượng $\le 5$ MB.
- **AC-14.2 (Lưu trữ tệp thành công trên Azure Blob Storage):**
  - *Given* tác giả tải lên tệp ảnh hợp lệ 2 MB định dạng PNG,
  - *When* quá trình tải lên hoàn tất,
  - *Then* tệp tin được lưu trên Azure Blob Storage với định danh duy nhất và hệ thống nhận về URL truy cập công khai hợp lệ.
- **AC-14.3 (Không lưu tệp nhị phân trong cơ sở dữ liệu quan hệ):**
  - *Given* một hình ảnh bài công thức được lưu thành công,
  - *When* kiểm tra bản ghi tương ứng trong cơ sở dữ liệu hệ thống,
  - *Then* bản ghi chỉ chứa đường dẫn URL và siêu dữ liệu (dung lượng, định dạng, ngày tạo), hoàn toàn không chứa dữ liệu nhị phân (binary blob).
- **AC-14.4 (Hiển thị ảnh tối ưu trên giao diện bài viết):**
  - *Given* bài công thức có ảnh lưu trên Azure Blob Storage,
  - *When* người dùng xem chi tiết công thức trên trình duyệt,
  - *Then* ảnh được tải trực tiếp từ Azure Blob Storage và hiển thị chính xác trong thời gian tải trang không vượt quá 2 giây.

---

<a id="fr-15"></a>
### FR-15 — Nhúng trình phát YouTube trong bài công thức

- **Mã yêu cầu:** FR-15
- **Module:** M01, M03
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống cho phép tác giả bài công thức (Member hoặc Administrator) gắn tối đa một đường link hoặc video ID hợp lệ của YouTube vào bài Recipe Post khi tạo hoặc chỉnh sửa; hệ thống kiểm tra tính hợp lệ của liên kết, trích xuất mã định danh video YouTube và nhúng trình phát video (YouTube IFrame Player) an toàn trên trang chi tiết công thức để phục vụ người xem; hệ thống TUYỆT ĐỐI KHÔNG nhận tải lên, sao chép hoặc lưu trữ tệp tin video nhị phân của YouTube trên máy chủ hay bộ lưu trữ đám mây (BR-10); video là thành phần hoàn toàn tùy chọn (BR-20).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi bài công thức được tạo hoặc chỉnh sửa bởi Member (FR-04, FR-16, FR-44) và Administrator (FR-07).
  - Nền tảng hỗ trợ: YouTube duy nhất trong Phase 1 (BR-10).
  - Giới hạn: Tối đa **1 liên kết video YouTube** cho mỗi bài công thức (BR-19).
- **Phân loại Actor:**
  - Primary Actor: `Member / Administrator` (tác giả gắn link video), `Guest / Member` (người xem video trên bài viết).
  - Supporting Actor: `YouTube IFrame API / Player`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-15.1`: Gắn hoặc cập nhật liên kết video YouTube khi soạn thảo bài công thức.
  - `UC-15.2`: Xem video hướng dẫn nấu ăn được nhúng trực tiếp trên trang chi tiết Recipe Post.
- **User Stories:**
  - *Là một người nấu ăn*, tôi muốn xem video YouTube quay lại các công đoạn nấu ăn ngay trong bài viết để dễ dàng hình dung kỹ thuật chế biến món ăn mà không phải chuyển sang ứng dụng khác.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Tác giả đang ở giao diện tạo hoặc chỉnh sửa bài công thức (FR-04, FR-16).
- **Kích hoạt (Trigger):**
  - Tác giả dán đường dẫn YouTube vào ô nhập liệu "Video hướng dẫn (YouTube)" và lưu bài viết.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả dán đường dẫn URL video YouTube vào trường nhập liệu.
  - Bước 2: Hệ thống kiểm tra định dạng URL: hỗ trợ các định dạng chuẩn của YouTube (ví dụ: `https://www.youtube.com/watch?v=VIDEO_ID`, `https://youtu.be/VIDEO_ID`, `https://www.youtube.com/embed/VIDEO_ID`).
  - Bước 3: Hệ thống trích xuất chuỗi Video ID gồm 11 ký tự an toàn.
  - Bước 4: Giao diện hiển thị khung xem trước (Preview) video YouTube nhúng để tác giả xác nhận đúng video.
  - Bước 5: Khi bài công thức được lưu và công khai, hệ thống lưu mã Video ID/URL tham chiếu vào cơ sở dữ liệu (tuyệt đối không tải tệp video về máy chủ theo BR-10).
  - Bước 6: Khi người dùng truy cập trang chi tiết công thức (FR-20), hệ thống kết xuất trình phát IFrame YouTube bảo đảm tiêu chuẩn an toàn (sandbox, allow-scripts, allow-same-origin) cho phép người xem bấm phát video trực tiếp.
- **Luồng thay thế (Alternative Flows):**
  - *AF-15.1 (Không gắn video):* Tác giả để trống ô video YouTube. Hệ thống công khai bài viết bình thường và không hiển thị khung video trên trang chi tiết (BR-20).
  - *AF-15.2 (Xóa hoặc đổi link video):* Tác giả xóa link hoặc dán link YouTube khác khi chỉnh sửa bài viết (FR-44). Hệ thống cập nhật Video ID mới vào bài viết.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-15.1 (URL không phải từ YouTube hoặc không hợp lệ):* Nếu tác giả nhập đường dẫn từ nền tảng video khác (như TikTok, Facebook, Vimeo) hoặc URL không đúng cấu trúc YouTube, hệ thống báo lỗi: *"Chỉ hỗ trợ liên kết video từ YouTube hợp lệ"* và yêu cầu sửa lại (BR-10).
  - *SF-15.1 (Phòng chống nhúng mã độc qua URL):* Hệ thống làm sạch và trích xuất nghiêm ngặt chỉ duy nhất mã Video ID chữ-số, không nhúng trực tiếp chuỗi URL tùy ý của người dùng vào thuộc tính thẻ IFrame nhằm chống tấn công XSS (NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Video YouTube được nhúng thành công và phát mượt mà trên trang chi tiết công thức.
- Cơ sở dữ liệu chỉ lưu trữ chuỗi tham chiếu Video ID nhẹ nhàng.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Mọi tác giả bài viết đều có quyền gắn link YouTube; mọi người xem (Guest và Member) đều có quyền xem video.
- **Ràng buộc kiến trúc:** Nghiêm cấm nhận file upload video; chỉ nhúng YouTube (BR-10).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-10](BUSINESS-RULES.md#br-10): Giới hạn định dạng video Phase 1 (chỉ YouTube, không upload file video).
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post (tối đa một link YouTube).
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của mô tả giới thiệu và ảnh đại diện.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian tải trang hiển thị chi tiết bài viết $\le 2$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến (chống XSS qua URL).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-15.1 (Trích xuất Video ID từ đường link YouTube hợp lệ):**
  - *Given* tác giả nhập đường dẫn YouTube hợp lệ dạng `https://youtu.be/dQw4w9WgXcQ`,
  - *When* hệ thống kiểm tra dữ liệu đầu vào,
  - *Then* hệ thống trích xuất chính xác Video ID là `dQw4w9WgXcQ` và hiển thị khung xem trước.
- **AC-15.2 (Từ chối liên kết video ngoài YouTube):**
  - *Given* tác giả dán đường dẫn video từ nền tảng khác (ví dụ `https://vimeo.com/12345`),
  - *When* hệ thống kiểm tra định dạng URL,
  - *Then* hệ thống từ chối liên kết và hiển thị thông báo lỗi chỉ hỗ trợ YouTube.
- **AC-15.3 (Giới hạn tối đa một liên kết YouTube duy nhất):**
  - *Given* biểu mẫu soạn thảo bài công thức,
  - *When* tác giả nhập dữ liệu,
  - *Then* hệ thống chỉ cung cấp duy nhất 1 trường nhập link YouTube cho toàn bộ bài viết.
- **AC-15.4 (Tuyệt đối không lưu trữ file video nhị phân):**
  - *Given* một bài công thức có gắn video YouTube được lưu trữ,
  - *When* kiểm tra dữ liệu lưu trữ phía máy chủ và đám mây,
  - *Then* hệ thống chỉ lưu chuỗi tham chiếu Video ID, hoàn toàn không lưu trữ tệp tin video mp4/mkv nào.
- **AC-15.5 (Tính tùy chọn của video YouTube):**
  - *Given* tác giả không nhập link YouTube nào vào bài viết,
  - *When* tác giả nhấn công khai bài viết đạt chuẩn,
  - *Then* hệ thống công khai bài viết thành công mà không báo lỗi thiếu video.

---

<a id="fr-16"></a>
### FR-16 — Cấu trúc dữ liệu bài công thức và tính bắt buộc của bước hướng dẫn chuẩn bị/chế biến

- **Mã yêu cầu:** FR-16
- **Module:** M03, M04
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Thiết lập cấu trúc dữ liệu chuẩn mực và bộ quy tắc kiểm tra hợp lệ (Recipe Validation Profile) bắt buộc đối với mọi Recipe Post trước khi được công khai trong hệ thống theo SRS 3.9 và BR-19: tiêu đề từ 3 đến 120 ký tự; số lượng nguyên liệu từ 1 đến 50 dòng; số khẩu phần từ 1 đến 50; thời gian chuẩn bị và nấu mỗi giá trị từ 0 đến 1.440 phút với tổng thời gian lớn hơn 0 (thời gian nấu được phép bằng 0 theo BR-20 khi thời gian chuẩn bị lớn hơn 0); loại ăn chay bắt buộc; mô tả bài viết tối đa 2.000 ký tự (tùy chọn theo BR-20); các bước hướng dẫn chuẩn bị/chế biến bắt buộc có từ 1 đến 30 bước (BR-19), mỗi bước có nội dung không rỗng sau khi cắt khoảng trắng đầu cuối (trim); tối đa 5 hình ảnh JPEG/PNG/WebP dung lượng $\le 5$ MB/ảnh (tùy chọn theo BR-20); tối đa một link YouTube (tùy chọn theo BR-10, BR-20); tất cả bài công thức đều áp dụng thống nhất mô hình xuất bản trực tiếp và hậu kiểm (BR-07, BR-59).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi bài Recipe Post do Member (FR-04) hoặc Administrator (FR-07) tạo và chỉnh sửa (FR-44).
  - Validation Profile chính thức:
    | Trường dữ liệu | Ràng buộc giá trị hợp lệ | Bắt buộc / Tùy chọn |
    |---|---|---|
    | **Tiêu đề (Title)** | **3 – 120 ký tự** | Bắt buộc |
    | **Danh sách nguyên liệu** | **1 – 50 dòng nguyên liệu** | Bắt buộc |
    | **Khẩu phần (Serving size)** | **1 – 50 khẩu phần** | Bắt buộc |
    | **Thời gian chuẩn bị (Prep time)** | **0 – 1.440 phút** | Bắt buộc |
    | **Thời gian nấu (Cook time)** | **0 – 1.440 phút** (được bằng 0 khi prep time > 0) | Bắt buộc |
    | **Tổng thời gian** | **Prep time + Cook time > 0** | Bắt buộc |
    | **Trường phái ăn chay** | Chọn 1 trong 4 loại chuẩn (Vegan, Lacto, Ovo, Lacto-ovo) | Bắt buộc |
    | **Mô tả món ăn (Description)** | **Tối đa 2.000 ký tự** | Tùy chọn (BR-20) |
    | **Bước hướng dẫn chuẩn bị/chế biến** | **1 – 30 bước**, nội dung mỗi bước không rỗng sau khi trim | Bắt buộc (BR-19) |
    | **Hình ảnh minh họa** | **0 – 5 ảnh**, JPEG/PNG/WebP, $\le 5$ MB/ảnh | Tùy chọn (BR-20) |
    | **Video YouTube** | **0 – 1 link YouTube** hợp lệ | Tùy chọn (BR-10, BR-20) |
- **Phân loại Actor:**
  - Primary Actor: `Member`, `Administrator` (tác giả bài viết).
  - Supporting Actor: `Hệ thống kiểm tra tính hợp lệ dữ liệu (Validation Subsystem)`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-16.1`: Soạn thảo và xác thực bài công thức tuân thủ đầy đủ cấu trúc dữ liệu chuẩn của hệ thống (bao gồm 1–30 bước hướng dẫn chuẩn bị/chế biến bắt buộc).
  - `UC-16.2`: Công khai bài công thức không có ảnh đại diện (sử dụng ảnh mặc định) hoặc không có mô tả giới thiệu.
- **User Stories:**
  - *Là một người nấu ăn*, tôi muốn hệ thống có quy định rõ ràng về các thông tin cần nhập để bài viết của tôi đầy đủ và chuẩn xác, bảo đảm có ít nhất một bước hướng dẫn rõ ràng để người khác thực hiện được, đồng thời linh hoạt cho phép tôi chia sẻ các món không cần nấu nhiệt (thời gian nấu = 0) hoặc không bắt buộc phải tải ảnh lên.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản hợp lệ (FR-03).
- **Kích hoạt (Trigger):**
  - Tác giả nhấn nút "Đăng công thức" hoặc "Lưu thay đổi" từ giao diện soạn thảo bài viết.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả nhấn "Đăng công thức".
  - Bước 2: Hệ thống tiếp nhận toàn bộ dữ liệu bài viết và kích hoạt bộ kiểm tra hợp lệ:
    - Tiêu đề: kiểm tra độ dài nằm trong khoảng 3 đến 120 ký tự.
    - Nguyên liệu: kiểm tra số lượng dòng từ 1 đến 50, mỗi dòng có tên và định lượng hợp lệ (FR-19).
    - Khẩu phần: kiểm tra giá trị số nguyên từ 1 đến 50.
    - Thời gian: kiểm tra prep time $\ge 0$, cook time $\ge 0$, mỗi giá trị $\le 1.440$ phút và tổng thời gian $> 0$.
    - Phân loại ăn chay: kiểm tra thuộc danh mục hợp lệ.
    - Mô tả: kiểm tra độ dài không vượt quá 2.000 ký tự (nếu có nhập).
    - Bước hướng dẫn chuẩn bị/chế biến: kiểm tra số lượng bước từ 1 đến 30 bước, mỗi bước có nội dung không rỗng sau khi cắt khoảng trắng đầu cuối (trim) (FR-22, BR-19).
    - Ảnh: kiểm tra số lượng $\le 5$, định dạng JPEG/PNG/WebP, dung lượng mỗi ảnh $\le 5$ MB (FR-14).
    - Video: kiểm tra tối đa 1 link YouTube hợp lệ (FR-15).
  - Bước 3: Toàn bộ tiêu chí validation đều thỏa mãn.
  - Bước 4: Hệ thống cho phép xuất bản trực tiếp bài công thức lên trạng thái công khai (`Public`) ngay lập tức mà không cần Admin duyệt trước (BR-07, BR-25, BR-59).
  - Bước 5: Hệ thống hiển thị thông báo thành công và điều hướng tác giả tới bài viết vừa đăng.
- **Luồng thay thế (Alternative Flows):**
  - *AF-16.1 (Công thức có thời gian nấu bằng 0):* Với các món salad trộn hoặc nước chấm, tác giả nhập cook time = 0 và prep time = 15 phút. Hệ thống xác nhận tổng thời gian là 15 phút $> 0$ và chấp nhận hợp lệ theo BR-20.
  - *AF-16.2 (Công thức không có ảnh tải lên):* Tác giả không upload ảnh nào (0 ảnh). Hệ thống tự động gán ảnh đại diện mặc định theo loại ăn chay của món ăn (BR-20).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-16.1 (Dữ liệu không thỏa mãn validation profile — Xử lý phía Frontend):* Khi người dùng nhấn nút đăng bài, nếu có bất kỳ trường nào vi phạm ngưỡng hợp lệ (ví dụ: tiêu đề $< 3$ hoặc $> 120$ ký tự, khẩu phần $> 50$, tổng thời gian $= 0$, chưa có dòng nguyên liệu nào hoặc vượt quá 50 nguyên liệu, chưa có bước hướng dẫn nào hoặc vượt quá 30 bước, hoặc bước hướng dẫn có nội dung rỗng sau khi trim), giao diện người dùng chặn gửi yêu cầu không hợp lệ, giữ lại toàn bộ nội dung đã nhập trong biểu mẫu và hiển thị thông báo lỗi chi tiết để tác giả chỉnh sửa mà không bị mất dữ liệu đã nhập.
  - *SF-16.1 (Thẩm định độc lập bắt buộc tại Backend & Không lưu rác DB):* Toàn bộ quy tắc kiểm tra hợp lệ bắt buộc phải được thực thi độc lập và toàn diện tại tầng Backend của máy chủ theo NFR-10, tuyệt đối không phụ thuộc vào việc kiểm tra của Frontend. Nếu nhận yêu cầu có dữ liệu không đạt chuẩn, máy chủ độc lập từ chối yêu cầu và thông báo chi tiết lỗi; máy chủ TUYỆT ĐỐI KHÔNG ghi bất kỳ bản ghi bài viết hay tài nguyên dở dang nào vào cơ sở dữ liệu (Database), bảo đảm không phát sinh dữ liệu rác (phù hợp với FR-24 OUT_OF_SCOPE).

#### 5. Hậu điều kiện (Postconditions)
- Bài công thức đạt chuẩn được lưu trữ an toàn và xuất bản công khai trực tiếp.
- Dữ liệu chuẩn mực sẵn sàng cho các chức năng Thẻ món (FR-17), Chi tiết (FR-20), Tính dinh dưỡng (FR-39).

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Áp dụng bình đẳng cho mọi bài viết do Member hoặc Administrator tạo.
- **Ràng buộc nghiệp vụ:** Đúng bộ tham số đã phê duyệt; bắt buộc có 1–30 bước hướng dẫn chuẩn bị/chế biến với nội dung không rỗng sau khi trim (BR-19); mô tả và ảnh mang tính tùy chọn (BR-20).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-10](BUSINESS-RULES.md#br-10): Giới hạn định dạng video Phase 1.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post.
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của mô tả giới thiệu và ảnh đại diện.
  - [BR-59](BUSINESS-RULES.md#br-59): Không có hàng đợi duyệt bài trước khi công khai.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-16.1 (Kiểm tra độ dài tiêu đề 3–120 ký tự):**
  - *Given* tác giả nhập tiêu đề bài công thức có độ dài 2 ký tự hoặc 121 ký tự,
  - *When* nhấn đăng bài,
  - *Then* hệ thống từ chối xuất bản và hiển thị thông báo lỗi yêu cầu tiêu đề từ 3 đến 120 ký tự.
- **AC-16.2 (Kiểm tra giới hạn số nguyên liệu 1–50 và khẩu phần 1–50):**
  - *Given* bài viết có 0 dòng nguyên liệu hoặc có 51 dòng nguyên liệu,
  - *When* tác giả gửi yêu cầu đăng bài,
  - *Then* hệ thống ngăn chặn xuất bản và thông báo số dòng nguyên liệu phải từ 1 đến 50.
- **AC-16.3 (Kiểm tra tổng thời gian prep + cook lớn hơn 0):**
  - *Given* tác giả nhập thời gian chuẩn bị là 0 phút và thời gian nấu là 0 phút,
  - *When* tác giả nhấn đăng bài,
  - *Then* hệ thống từ chối xuất bản và thông báo tổng thời gian chuẩn bị và nấu phải lớn hơn 0 phút.
- **AC-16.4 (Cho phép thời gian nấu bằng 0 khi thời gian chuẩn bị lớn hơn 0):**
  - *Given* món salad có thời gian chuẩn bị 10 phút và thời gian nấu 0 phút,
  - *When* tác giả gửi yêu cầu đăng bài,
  - *Then* hệ thống chấp nhận hợp lệ và cho phép xuất bản bài viết.
- **AC-16.5 (Bắt buộc có từ 1 đến 30 bước hướng dẫn chuẩn bị/chế biến không rỗng):**
  - *Given* bài công thức chưa có bước hướng dẫn nào (0 bước) hoặc có bước chỉ chứa khoảng trắng,
  - *When* tác giả gửi yêu cầu đăng bài,
  - *Then* hệ thống từ chối xuất bản và hiển thị thông báo lỗi yêu cầu phải có ít nhất 1 bước hướng dẫn chuẩn bị/chế biến với nội dung hợp lệ (tối đa 30 bước).
- **AC-16.6 (Giới hạn mô tả tối đa 2.000 ký tự và tối đa 30 bước hướng dẫn):**
  - *Given* tác giả nhập mô tả dài hơn 2.000 ký tự hoặc tạo 31 bước hướng dẫn,
  - *When* tác giả nhấn lưu,
  - *Then* hệ thống chặn lưu và yêu cầu mô tả $\le 2.000$ ký tự và số bước hướng dẫn $\le 30$ bước.
- **AC-16.7 (Cho phép công khai không có ảnh đại diện và không có mô tả):**
  - *Given* bài công thức không có mô tả và không tải ảnh nào lên nhưng có đầy đủ nguyên liệu và bước hướng dẫn chuẩn bị/chế biến hợp lệ,
  - *When* tác giả nhấn đăng bài,
  - *Then* hệ thống công khai bài viết thành công, hiển thị danh sách nguyên liệu, các bước hướng dẫn và gán ảnh mặc định cho món ăn theo BR-20.

---

<a id="fr-17"></a>
### FR-17 — Trình bày bài công thức dạng thẻ món trong Khám phá và liên kết lịch ăn

- **Mã yêu cầu:** FR-17
- **Module:** M04, M05
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống trình bày các bài công thức đang công khai (không bị ẩn hoặc xóa) dưới dạng Thẻ món ăn (Recipe Card) trực quan và đồng nhất trong giao diện Khám phá (Trang chủ, duyệt danh mục, kết quả tìm kiếm và lọc tại FR-01, FR-08); tích hợp trực tiếp trên thẻ các nút hành động nhanh để kết nối thẻ món với luồng lưu công thức yêu thích (FR-32) hoặc thêm món vào Lịch ăn tuần 3 bữa (FR-09, FR-33); nhấp vào thẻ điều hướng liền mạch tới trang chi tiết công thức (FR-20).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi người dùng xem giao diện khám phá (Guest và Member).
  - Thành phần hiển thị trên thẻ món: Ảnh đại diện (hoặc ảnh mặc định nếu bài không có ảnh theo BR-20), Tên món ăn, Thẻ tác giả (Author Card thu nhỏ theo FR-23, BR-18), Phân loại trường phái ăn chay, Tổng thời gian (prep + cook), và Tổng số lượt Like (FR-45).
- **Phân loại Actor:**
  - Primary Actor: `Guest`, `Member` (người duyệt khám phá món ăn).
  - Supporting Actor: `Hệ thống hiển thị và quản lý thẻ món`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-17.1`: Duyệt xem danh sách các thẻ món ăn công khai trên giao diện Khám phá.
  - `UC-17.2`: Thực hiện thao tác lưu nhanh công thức hoặc thêm món vào Lịch ăn tuần trực tiếp từ thẻ món.
  - `UC-17.3`: Nhấp vào thẻ món để xem trang chi tiết đầy đủ của bài công thức.
- **User Stories:**
  - *Là một người tìm kiếm ý tưởng ăn chay*, tôi muốn lướt xem các thẻ món ăn hấp dẫn với đầy đủ thông tin cơ bản về thời gian và loại chay, đồng thời có thể bấm lưu lại hoặc thêm ngay vào thực đơn tuần mà không cần phải mở từng bài viết.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng truy cập trang Khám phá, Trang chủ hoặc thực hiện tìm kiếm/lọc công thức.
- **Kích hoạt (Trigger):**
  - Hệ thống tải và kết xuất danh sách bài viết dưới dạng lưới các thẻ món.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Người dùng mở giao diện Khám phá.
  - Bước 2: Hệ thống truy vấn cơ sở dữ liệu lấy danh sách các Recipe Post có trạng thái công khai (`Public`), loại trừ các bài đang bị ẩn hoặc xóa (FR-20).
  - Bước 3: Với mỗi bài viết, hệ thống kết xuất một Thẻ món ăn (Recipe Card) chuẩn gồm:
    - Ảnh đại diện từ Azure Blob Storage (FR-14); nếu bài không có ảnh, hiển thị ảnh mặc định chuyên dụng theo loại ăn chay (BR-20).
    - Tên món ăn (tiêu đề).
    - Tên hiển thị của tác giả (liên kết tới Author Card theo FR-23, bảo vệ quyền riêng tư theo BR-18).
    - Nhãn phân loại ăn chay (Vegan, Lacto, Ovo, Lacto-ovo).
    - Tổng thời gian thực hiện (phút).
    - Bộ đếm tổng lượt Like (FR-45).
    - Nút icon "Lưu công thức" (Trái tim / Bookmark).
    - Nút icon "Thêm vào lịch ăn" (Cuốn lịch).
  - Bước 4: Danh sách thẻ món hiển thị hoàn tất trong thời gian tải trang $\le 2$ giây (NFR-02).
  - Bước 5: Người dùng có thể nhấp vào thân thẻ để mở trang xem chi tiết công thức (FR-20).
- **Luồng thay thế (Alternative Flows):**
  - *AF-17.1 (Lưu công thức từ thẻ món):* Member nhấn icon Bookmark trên thẻ món. Hệ thống lưu công thức vào danh sách Đã lưu của Member (FR-32) và chuyển đổi icon sang trạng thái đã lưu ngay tức thì mà không chuyển trang.
  - *AF-17.2 (Thêm món vào lịch ăn từ thẻ món):* Member nhấn icon Cuốn lịch trên thẻ món. Hệ thống hiển thị modal nhanh cho phép chọn Ngày trong tuần (Thứ Hai đến Chủ Nhật) và Bữa ăn (Sáng, Trưa, Tối). Member xác nhận, hệ thống thêm món vào Lịch ăn tuần (FR-09, FR-33) và thông báo thành công.
  - *AF-17.3 (Guest nhấn nút lưu hoặc thêm lịch ăn):* Nếu người dùng là Guest nhấn icon Lưu hoặc Thêm lịch ăn, hệ thống hiển thị thông báo yêu cầu đăng nhập (BR-05, BR-32).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-17.1 (Bài viết vừa bị ẩn hoặc xóa):* Nếu trong lúc người dùng duyệt thẻ món mà bài viết gốc vừa bị Admin ẩn hoặc tác giả xóa, khi người dùng nhấp vào thẻ, hệ thống hiển thị thông báo: *"Công thức này không còn khả dụng"* thay vì báo lỗi 500 (BR-35).
  - *SF-17.1 (Bảo mật quyền riêng tư tác giả trên thẻ):* Thông tin tác giả hiển thị trên thẻ món chỉ bao gồm tên hiển thị (display name) công khai, tuyệt đối không lộ địa chỉ email hay ID nội bộ của tác giả (BR-18, NFR-08).

#### 5. Hậu điều kiện (Postconditions)
- Các thẻ món hiển thị chuẩn xác và hấp dẫn trên giao diện.
- Các hành động lưu hoặc thêm vào lịch ăn được cập nhật đồng bộ vào hồ sơ Member.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Xem thẻ món mở cho toàn bộ Guest và Member. Thao tác lưu và thêm lịch ăn chỉ dành cho Member đã đăng nhập (BR-05).
- **Ràng buộc giao diện:** Tối ưu hóa hiển thị thẻ món trên thiết bị di động (NFR-13).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của ảnh đại diện (dùng ảnh mặc định khi thiếu).
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn.
  - [BR-33](BUSINESS-RULES.md#br-33): Thao tác lưu công thức không tiêu thụ hạn mức AI.
  - [BR-35](BUSINESS-RULES.md#br-35): Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn.
  - [BR-36](BUSINESS-RULES.md#br-36): Quy tắc 3 loại bữa ăn cố định trong Lịch ăn MVP.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian tải trang hiển thị chi tiết bài viết $\le 2$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-17.1 (Hiển thị đầy đủ thông tin trên thẻ món):**
  - *Given* một bài công thức đang ở trạng thái công khai hợp lệ,
  - *When* hệ thống hiển thị danh sách Khám phá,
  - *Then* thẻ món thể hiện rõ ảnh đại diện, tiêu đề món ăn, tên tác giả công khai, loại ăn chay, tổng thời gian và số lượt Like.
- **AC-17.2 (Gán ảnh mặc định khi bài viết không có ảnh):**
  - *Given* bài công thức công khai không có hình ảnh đính kèm,
  - *When* thẻ món được hiển thị,
  - *Then* hệ thống tự động hiển thị hình ảnh minh họa mặc định tương ứng với loại ăn chay của bài viết.
- **AC-17.3 (Lưu công thức nhanh từ thẻ món):**
  - *Given* Member đã đăng nhập nhấn nút Lưu trên thẻ món,
  - *When* thao tác thành công,
  - *Then* bài công thức được đưa vào danh sách Đã lưu của Member và icon chuyển sang trạng thái đã lưu mà không tải lại trang.
- **AC-17.4 (Thêm món vào Lịch ăn từ thẻ món):**
  - *Given* Member đã đăng nhập nhấn nút Thêm vào lịch ăn trên thẻ món,
  - *When* Member chọn Bữa tối Thứ Tư và xác nhận,
  - *Then* món ăn được thêm thành công vào Bữa tối Thứ Tư trong Lịch ăn tuần của Member.
- **AC-17.5 (Điều hướng chính xác tới trang chi tiết công thức):**
  - *Given* người dùng nhấn vào thân thẻ món,
  - *When* sự kiện click diễn ra,
  - *Then* hệ thống điều hướng chính xác tới trang chi tiết đầy đủ của bài công thức đó.

---

<a id="fr-18"></a>
### FR-18 — Admin quản lý danh mục nguyên liệu và danh mục món ăn

- **Mã yêu cầu:** FR-18
- **Module:** M04, M09
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp giao diện quản trị chuyên biệt dành cho Administrator để quản lý hai danh mục phân loại nền tảng của hệ thống: Danh mục nguyên liệu chuẩn (Standard Ingredient Catalog) và Danh mục món ăn / công thức (Recipe Categories); Administrator có thẩm quyền xem, tìm kiếm, tạo mới, chỉnh sửa thông tin và ngừng sử dụng (soft-disable) các danh mục; một bài công thức có thể được gắn một hoặc nhiều danh mục món ăn; hệ thống nghiêm cấm xóa vĩnh viễn (hard delete) các danh mục đang được tham chiếu trong các bài công thức công khai.
- **Phạm vi nghiệp vụ:**
  - Quản lý danh mục nguyên liệu: Tên nguyên liệu chuẩn tiếng Việt, tên tiếng Anh (tùy chọn), nhóm nguyên liệu (Rau củ, Nấm, Đậu hạt, Gia vị, v.v.).
  - Quản lý danh mục món: Tên danh mục (Món khai vị, Món chính, Món canh, Món kho chay, Món lẩu, Món tráng miệng...), mô tả, thứ tự hiển thị, trạng thái hoạt động.
  - Phân quyền: Độc quyền cho vai trò `Administrator` (M09).
- **Phân loại Actor:**
  - Primary Actor: `Administrator`.
  - Supporting Actor: `Hệ thống quản lý Danh mục phân loại`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-18.1`: Tìm kiếm, xem danh sách và quản lý danh mục nguyên liệu chuẩn của hệ thống.
  - `UC-18.2`: Thêm mới, chỉnh sửa hoặc chuyển trạng thái ngừng sử dụng danh mục món ăn.
  - `UC-18.3`: Phân loại và gán nhiều danh mục món ăn cho một bài công thức nấu ăn.
- **User Stories:**
  - *Là một Administrator*, tôi muốn dễ dàng cập nhật danh mục nguyên liệu và các thể loại món ăn chay mới theo xu hướng ẩm thực, để các thành viên có thể phân loại bài viết chuẩn xác và người xem dễ dàng tìm kiếm món ăn theo nhu cầu.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập với tài khoản có vai trò Administrator (FR-03, NFR-09).
- **Kích hoạt (Trigger):**
  - Administrator truy cập phân hệ "Quản lý danh mục" trên trang quản trị hệ thống.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Administrator mở trang Quản lý danh mục món ăn.
  - Bước 2: Hệ thống hiển thị bảng danh sách các danh mục gồm: Tên danh mục, Mô tả, Số lượng bài công thức đang thuộc danh mục, Thứ tự hiển thị, và Trạng thái (`Đang dùng` / `Ngừng dùng`).
  - Bước 3: Administrator nhấn "Thêm danh mục món".
  - Bước 4: Hệ thống hiển thị biểu mẫu nhập: Tên danh mục (3–50 ký tự, không trùng lặp), Mô tả ngắn, và Thứ tự ưu tiên.
  - Bước 5: Administrator nhập thông tin hợp lệ và nhấn "Lưu danh mục".
  - Bước 6: Hệ thống kiểm tra trùng lặp tên; lưu danh mục mới vào cơ sở dữ liệu với trạng thái `Đang dùng`.
  - Bước 7: Danh mục mới xuất hiện ngay lập tức trong bộ lọc tìm kiếm (FR-08) và biểu mẫu đăng bài của người dùng (FR-16).
- **Luồng thay thế (Alternative Flows):**
  - *AF-18.1 (Chỉnh sửa danh mục hiện có):* Administrator sửa tên hoặc mô tả của danh mục. Hệ thống cập nhật và tự động phản ánh tên mới trên tất cả các bài viết đang thuộc danh mục đó.
  - *AF-18.2 (Ngừng sử dụng danh mục):* Administrator chọn chuyển trạng thái danh mục sang `Ngừng dùng` (Soft-disable). Danh mục này không còn xuất hiện trong danh sách lựa chọn khi tạo bài viết mới, nhưng các bài công thức cũ đã gắn danh mục này vẫn giữ nguyên liên kết lịch sử.
  - *AF-18.3 (Gán nhiều danh mục cho bài viết):* Khi tác giả soạn bài công thức, hệ thống cho phép chọn đồng thời nhiều danh mục (ví dụ: vừa là "Món chính" vừa là "Món kho chay").
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-18.1 (Cố gắng xóa cứng danh mục đang được sử dụng):* Nếu Administrator cố gắng thực hiện hành động xóa vĩnh viễn (hard delete) một danh mục đang có ít nhất một bài công thức liên kết, hệ thống từ chối xóa và hiển thị thông báo: *"Không thể xóa danh mục đang có bài công thức tham chiếu. Vui lòng chọn Ngừng sử dụng."* (BR-64).
  - *SF-18.1 (Chặn truy cập trái phép phân hệ danh mục):* Người dùng không có vai trò Administrator (Member hoặc Guest) khi truy cập các API quản lý danh mục sẽ bị từ chối ngay với mã lỗi 403 Forbidden (NFR-09).

#### 5. Hậu điều kiện (Postconditions)
- Bản ghi danh mục nguyên liệu hoặc danh mục món ăn được lưu trữ hoặc cập nhật an toàn.
- Hệ thống danh mục phân loại luôn giữ được tính toàn vẹn dữ liệu.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Độc quyền cho Administrator.
- **Ràng buộc an toàn:** Cấm xóa cứng các bản ghi đang được tham chiếu trong công thức (BR-64).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-06](BUSINESS-RULES.md#br-06): Bảo mật hệ thống và phân quyền quản trị.
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản đăng bài.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-18.1 (Phân quyền quản lý danh mục chỉ dành cho Administrator):**
  - *Given* người dùng đăng nhập tài khoản Member thông thường,
  - *When* cố gắng truy cập trang hoặc gọi API quản lý danh mục nguyên liệu/món ăn,
  - *Then* hệ thống từ chối thực hiện và phản hồi mã lỗi 403 Forbidden.
- **AC-18.2 (Thêm mới danh mục món ăn hợp lệ):**
  - *Given* Administrator nhập tên danh mục món ăn mới hợp lệ chưa từng tồn tại,
  - *When* nhấn lưu danh mục,
  - *Then* hệ thống tạo mới danh mục thành công và hiển thị trong danh sách lựa chọn phân loại.
- **AC-18.3 (Hỗ trợ một bài công thức gắn nhiều danh mục):**
  - *Given* tác giả soạn bài viết món "Nấm đùi gà kho tiêu",
  - *When* tác giả chọn cả hai danh mục "Món kho chay" và "Món chính",
  - *Then* hệ thống lưu trữ thành công cả hai liên kết danh mục cho bài viết đó.
- **AC-18.4 (Ngăn chặn xóa vĩnh viễn danh mục đang có bài viết liên kết):**
  - *Given* danh mục "Món canh chay" đang có 10 bài công thức công khai liên kết,
  - *When* Administrator gửi yêu cầu xóa vĩnh viễn danh mục này,
  - *Then* hệ thống từ chối xóa, giữ nguyên dữ liệu và yêu cầu chuyển sang trạng thái ngừng sử dụng.

---

<a id="fr-19"></a>
### FR-19 — Nhập nguyên liệu linh hoạt và định lượng

- **Mã yêu cầu:** FR-19
- **Module:** M03, M04
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp cơ chế nhập danh sách nguyên liệu linh hoạt và trực quan khi Member soạn thảo bài công thức nấu ăn: mỗi bài công thức có từ 1 đến 50 dòng nguyên liệu (BR-19); tác giả có thể tìm kiếm và chọn nguyên liệu có sẵn từ danh mục chuẩn (FR-18) hoặc tự do nhập tên nguyên liệu mới khi không tìm thấy trong danh mục (BR-12, BR-50); với mỗi dòng nguyên liệu, tác giả có thể khai báo định lượng cụ thể (số lượng + đơn vị đo như g, kg, ml, l, muỗng, quả...) hoặc chọn mức định lượng không số học "vừa đủ" (to taste / as needed); hệ thống luôn lưu trữ và bảo toàn tên hiển thị nguyên liệu do tác giả nhập kết hợp với liên kết tham chiếu tùy chọn tới nguyên liệu chuẩn (BR-12).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi bài Recipe Post tạo mới hoặc chỉnh sửa trong hệ thống (FR-04, FR-16, FR-44).
  - Số lượng: Tối thiểu 1 dòng nguyên liệu, tối đa 50 dòng nguyên liệu (BR-19).
  - Không bắt buộc: Liên kết nguyên liệu chuẩn là tùy chọn; nguyên liệu tự do ngoài danh mục vẫn được lưu và công khai bình thường (BR-12, BR-50).
- **Phân loại Actor:**
  - Primary Actor: `Member`, `Administrator` (tác giả bài viết).
  - Supporting Actor: `Hệ thống gợi ý và chuẩn hóa nguyên liệu`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-19.1`: Tìm kiếm và chọn nguyên liệu có sẵn từ danh mục chuẩn khi soạn công thức.
  - `UC-19.2`: Tự do nhập tên nguyên liệu mới chưa có trong danh mục và khai báo định lượng cụ thể hoặc "vừa đủ".
  - `UC-19.3`: Quản lý danh sách 1–50 dòng nguyên liệu trong bài công thức (thêm, sửa, xóa, sắp xếp dòng).
- **User Stories:**
  - *Là một người nấu ăn sáng tạo*, tôi muốn nhập nhanh các nguyên liệu quen thuộc từ danh mục gợi ý và cũng có thể gõ các loại rau rừng đặc sản địa phương chưa có sẵn, đồng thời ghi "muối vừa đủ" cho những gia vị nêm nếm linh hoạt.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Tác giả đang ở giao diện soạn thảo hoặc chỉnh sửa bài công thức (FR-04, FR-16).
- **Kích hoạt (Trigger):**
  - Tác giả tương tác với phần "Danh sách nguyên liệu" trong biểu mẫu soạn bài.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả mở phần nhập nguyên liệu của bài công thức. Mặc định có sẵn 1 dòng nguyên liệu trống.
  - Bước 2: Tác giả gõ tên nguyên liệu vào ô tìm kiếm nguyên liệu:
    - Hệ thống tự động gợi ý danh sách các nguyên liệu khớp từ Danh mục nguyên liệu chuẩn (FR-18).
    - Tác giả có thể nhấp chọn một nguyên liệu gợi ý (hệ thống ghi nhận liên kết `ingredientId` chuẩn).
    - Hoặc nếu không thấy, tác giả giữ nguyên tên tự do mình vừa gõ (hệ thống ghi nhận tên tự do, `ingredientId = null`).
  - Bước 3: Tác giả chọn hình thức khai báo định lượng:
    - *Định lượng số:* Nhập số lượng (ví dụ: 200) và chọn/nhập đơn vị đo (ví dụ: gam, ml, muỗng canh, quả).
    - *Định lượng "vừa đủ":* Đánh dấu vào tùy chọn "Vừa đủ" (hệ thống lưu định lượng dạng chữ "vừa đủ", không có số).
  - Bước 4: Tác giả nhấn nút "Thêm nguyên liệu" để tạo thêm dòng mới (tối đa 50 dòng theo BR-19).
  - Bước 5: Khi lưu bài viết, hệ thống kiểm tra số lượng dòng nguyên liệu nằm trong khoảng 1 đến 50 dòng, tất cả các dòng đều có tên và định lượng hợp lệ.
  - Bước 6: Hệ thống lưu trữ danh sách nguyên liệu và bảo toàn chuỗi tên hiển thị do tác giả nhập.
- **Luồng thay thế (Alternative Flows):**
  - *AF-19.1 (Xóa bớt hoặc đổi thứ tự dòng nguyên liệu):* Tác giả nhấn nút xóa dòng hoặc kéo thả để đổi thứ tự các nguyên liệu trong danh sách.
  - *AF-19.2 (Chỉnh sửa nguyên liệu đã lưu):* Tác giả sửa đổi tên hoặc định lượng của bất kỳ dòng nào khi cập nhật bài viết (FR-44).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-19.1 (Để trống nguyên liệu hoặc vượt quá 50 dòng):* Nếu tác giả không nhập dòng nguyên liệu nào (0 dòng) hoặc tạo quá 50 dòng nguyên liệu, hệ thống chặn lưu bài và hiển thị thông báo lỗi yêu cầu số dòng từ 1 đến 50 (BR-19).
  - *EF-19.2 (Dòng nguyên liệu thiếu tên hoặc thiếu định lượng):* Nếu một dòng nguyên liệu có tên nhưng bỏ trống cả số lượng lẫn tùy chọn "vừa đủ", hệ thống yêu cầu bổ sung định lượng.
  - *SF-19.1 (Làm sạch chuỗi tên nguyên liệu tự do):* Tên nguyên liệu do tác giả tự do gõ được lọc và làm sạch mã độc (sanitize HTML) nhằm ngăn chặn tấn công XSS qua dữ liệu đầu vào (NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Danh sách nguyên liệu được lưu trữ chuẩn xác, sẵn sàng phục vụ hiển thị chi tiết (FR-20), tính toán dinh dưỡng (FR-39) và tổng hợp danh sách mua sắm (FR-54).

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member và Administrator đều có quyền nhập nguyên liệu linh hoạt cho bài viết của mình.
- **Ràng buộc nghiệp vụ:** Không ép buộc phải liên kết nguyên liệu chuẩn (BR-12); cho phép định lượng "vừa đủ"; đúng giới hạn 1–50 dòng (BR-19).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-12](BUSINESS-RULES.md#br-12): Tính tùy chọn của liên kết nguyên liệu chuẩn.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post (1–50 nguyên liệu).
  - [BR-48](BUSINESS-RULES.md#br-48): Xử lý nguyên liệu thiếu định lượng hoặc thiếu số liệu dinh dưỡng.
  - [BR-49](BUSINESS-RULES.md#br-49): Không gọi API dinh dưỡng ngoài realtime và nguồn tham khảo.
  - [BR-50](BUSINESS-RULES.md#br-50): Cho phép công khai bài chứa nguyên liệu ngoài danh mục dinh dưỡng.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến (chống XSS trong tên nguyên liệu).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-19.1 (Hỗ trợ chọn nguyên liệu từ danh mục hoặc gõ tên tự do):**
  - *Given* tác giả đang soạn thảo nguyên liệu bài công thức,
  - *When* tác giả gõ một tên nguyên liệu không có trong danh mục chuẩn,
  - *Then* hệ thống vẫn chấp nhận tên nguyên liệu tự do đó và không báo lỗi chặn nhập.
- **AC-19.2 (Hỗ trợ định lượng có số và định lượng 'vừa đủ'):**
  - *Given* tác giả nhập dòng nguyên liệu "Muối tiêu",
  - *When* tác giả tích chọn định lượng "Vừa đủ",
  - *Then* hệ thống lưu trữ nguyên liệu thành công với định lượng là chuỗi "vừa đủ" mà không bắt buộc nhập số.
- **AC-19.3 (Ràng buộc số dòng nguyên liệu từ 1 đến 50 dòng):**
  - *Given* tác giả tạo bài viết có 0 dòng nguyên liệu hoặc có 51 dòng nguyên liệu,
  - *When* tác giả gửi yêu cầu lưu bài viết,
  - *Then* hệ thống từ chối lưu và hiển thị thông báo số lượng nguyên liệu phải từ 1 đến 50 dòng.
- **AC-19.4 (Bảo toàn tên hiển thị và liên kết chuẩn tùy chọn):**
  - *Given* tác giả chọn nguyên liệu chuẩn "Đậu phụ" nhưng gõ tên hiển thị là "Đậu phụ Làng Mơ",
  - *When* bài viết được lưu thành công,
  - *Then* hệ thống lưu giữ chuỗi "Đậu phụ Làng Mơ" để hiển thị cho người đọc kèm mã tham chiếu chuẩn tới "Đậu phụ".

---

<a id="fr-20"></a>
### FR-20 — Thống nhất nguồn hiển thị thẻ món, chi tiết và thực đơn

- **Mã yêu cầu:** FR-20
- **Module:** M01, M04, M05
- **Trạng thái (Derived):** ACTIVE
- **Điểm tích hợp AI Chatbot:** Trang chi tiết công thức (Recipe Detail) có thể cung cấp Recipe Post hiện tại làm context cho AI Chatbot [FR-51](FUNCTIONAL-REQUIREMENTS.md#fr-51) khi người dùng chủ động chọn chức năng hỏi AI về công thức đang xem. Toàn bộ luồng tương tác hội thoại, phân quyền và trừ hạn ngạch thuộc phạm vi sở hữu của FR-51 (áp dụng cho cả Member và Guest theo [FR-02](FUNCTIONAL-REQUIREMENTS.md#fr-02)).

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Áp dụng kiến trúc nguồn dữ liệu duy nhất (Single Source of Truth - SSOT) cho toàn bộ các bài công thức nấu ăn trong hệ thống: trang chi tiết bài công thức (Recipe Detail View) và thẻ món ăn (Recipe Card theo FR-17) được kết xuất trực tiếp từ cùng một bản thể Recipe Post duy nhất; các phân hệ Lịch ăn tuần (Meal Plan - FR-09, FR-33) và Danh sách mua sắm (Shopping List - FR-53) tham chiếu trực tiếp đến bản thể này bằng khóa ngoại; hệ thống TUYỆT ĐỐI KHÔNG sao chép (duplicate) dữ liệu bài viết hay tạo các bài nguồn/website riêng biệt; khi bài viết được chỉnh sửa, toàn bộ các điểm hiển thị đều được cập nhật nhất quán; khi bài viết bị xóa hoặc ẩn, các tham chiếu lịch sử được giữ an toàn dưới dạng bản ghi không khả dụng (Tombstone) (BR-35, BR-64).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Toàn bộ Recipe Post công khai trong hệ thống.
  - Điểm kết nối dữ liệu: Thẻ món Khám phá (FR-17), Trang chi tiết công thức, Danh sách công thức đã lưu (FR-32), Lịch ăn tuần 3 bữa (FR-09, FR-33), Danh sách mua sắm (FR-53).
  - Không áp dụng: Hệ thống không tạo bài viết ảo hay nhân bản dữ liệu để phục vụ menu.
- **Phân loại Actor:**
  - Primary Actor: `Guest`, `Member` (người xem nội dung), `Member` (tác giả chỉnh sửa/xóa bài).
  - Supporting Actor: `Hệ thống quản lý dữ liệu hợp nhất (Data SSOT Subsystem)`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-20.1`: Xem trang chi tiết đầy đủ của bài công thức được kết xuất từ nguồn dữ liệu duy nhất.
  - `UC-20.2`: Xem các tham chiếu công thức trong Lịch ăn và Danh sách đã lưu tự động cập nhật đồng bộ khi bài nguồn thay đổi.
  - `UC-20.3`: Xem hiển thị trạng thái Tombstone an toàn trong Lịch ăn khi bài công thức nguồn bị xóa hoặc tạm ẩn.
- **User Stories:**
  - *Là một người sử dụng hệ thống*, tôi muốn khi tác giả cập nhật công thức nấu ăn thì các món tôi đã lưu trong lịch ăn cũng tự động cập nhật theo, và nếu bài viết có bị xóa thì lịch ăn của tôi vẫn còn ghi nhớ tên món chứ không bị mất trắng.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Bài công thức đã được tạo và lưu trữ trong cơ sở dữ liệu hệ thống (FR-04, FR-07).
- **Kích hoạt (Trigger):**
  - Người dùng truy cập trang chi tiết công thức, xem thẻ món, hoặc mở Lịch ăn tuần.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Người dùng nhấp vào một thẻ món trong trang Khám phá hoặc trong Lịch ăn tuần.
  - Bước 2: Hệ thống truy vấn trực tiếp bản thể Recipe Post từ cơ sở dữ liệu dựa trên mã định danh công thức duy nhất (`recipeId`).
  - Bước 3: Hệ thống kết xuất Trang chi tiết công thức (Recipe Detail View) gồm đầy đủ các khối dữ liệu:
    - Tiêu đề, Tác giả (FR-23), Phân loại ăn chay, Ngày đăng/cập nhật.
    - Thời gian chuẩn bị, Thời gian nấu, Khẩu phần (FR-16).
    - Ảnh đại diện từ Azure Blob Storage (FR-14) và Video YouTube nhúng (FR-15).
    - Mô tả giới thiệu món ăn.
    - Danh sách 1–50 nguyên liệu kèm định lượng (FR-19).
    - Bảng ước tính 9 chỉ tiêu dinh dưỡng trên 1 khẩu phần (FR-39).
    - Các bước hướng dẫn chuẩn bị/chế biến tuần tự (FR-22).
    - Khu vực bình luận và thảo luận cộng đồng (FR-46).
  - Bước 4: Trang hiển thị hoàn tất trong thời gian $\le 2$ giây (NFR-02).
- **Luồng thay thế (Alternative Flows):**
  - *AF-20.1 (Tác giả cập nhật bài công thức):* Khi tác giả chỉnh sửa bài công thức (FR-44), hệ thống cập nhật trực tiếp trên bản ghi duy nhất đó. Ngay lập tức, trang chi tiết, thẻ món trên Khám phá và các mục trong Lịch ăn của mọi người dùng đều phản ánh thông tin mới mà không cần thao tác đồng bộ phụ nào khác.
  - *AF-20.2 (Bài công thức bị xóa hoặc ẩn - Cơ chế Tombstone):* Khi bài công thức bị xóa bởi tác giả (FR-44) hoặc bị Administrator ẩn (BR-26):
    - Trạng thái bài viết chuyển sang `DELETED` hoặc `HIDDEN`.
    - Bài viết lập tức biến mất khỏi trang Khám phá, Tìm kiếm và pool gợi ý AI.
    - Tại các ô Lịch ăn tuần (FR-09) và Danh sách đã lưu (FR-32) của người dùng khác, hệ thống giữ nguyên liên kết nhưng hiển thị dạng Tombstone: *"Công thức không còn khả dụng"* và vô hiệu hóa link mở chi tiết (BR-35, BR-64).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-20.1 (Truy cập trực tiếp bài viết không tồn tại hoặc đã bị xóa):* Nếu người dùng truy cập trực tiếp qua URL của bài viết đã bị xóa hoàn toàn, hệ thống phản hồi trang thông báo lỗi 404 thân thiện nêu rõ nội dung không còn khả dụng.
  - *SF-20.1 (Ngăn chặn nhân bản dữ liệu bài viết):* Kiến trúc cơ sở dữ liệu sử dụng khóa ngoại chặt chẽ trỏ về bảng công thức gốc, ngăn ngừa việc nhân bản dữ liệu gây bất nhất quán dữ liệu giữa các phân hệ (NFR-08).

#### 5. Hậu điều kiện (Postconditions)
- Dữ liệu bài công thức luôn bảo đảm tính toàn vẹn và nhất quán trên toàn hệ thống.
- Trải nghiệm người dùng không bị gián đoạn khi dữ liệu gốc biến động.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Mọi người dùng đều có quyền xem chi tiết công thức công khai.
- **Ràng buộc kiến trúc:** Single Source of Truth; không tạo bản sao độc lập; áp dụng Tombstone pattern (BR-35, BR-64).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của mô tả giới thiệu và ảnh đại diện.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn.
  - [BR-33](BUSINESS-RULES.md#br-33): Thao tác lưu công thức không tiêu thụ hạn mức AI.
  - [BR-35](BUSINESS-RULES.md#br-35): Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn (Tombstone).
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian tải trang hiển thị chi tiết bài viết $\le 2$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-20.1 (Hiển thị trang chi tiết công thức từ nguồn dữ liệu duy nhất):**
  - *Given* bài công thức công khai tồn tại trong hệ thống,
  - *When* người dùng mở xem trang chi tiết,
  - *Then* hệ thống kết xuất đầy đủ thông tin chuẩn (tiêu đề, nguyên liệu, khẩu phần, thời gian, dinh dưỡng, bước hướng dẫn chuẩn bị/chế biến, ảnh, video) từ bản thể dữ liệu duy nhất.
- **AC-20.2 (Đồng bộ tức thì khi tác giả chỉnh sửa bài viết):**
  - *Given* tác giả vừa cập nhật thành công tiêu đề và định lượng của bài công thức,
  - *When* một người dùng khác mở xem bài viết đó hoặc xem trong Lịch ăn tuần,
  - *Then* thông tin mới cập nhật được phản ánh chính xác ngay lập tức mà không cần đồng bộ thủ công.
- **AC-20.3 (Bảo toàn Tombstone trong Lịch ăn khi bài nguồn bị xóa):**
  - *Given* một bài công thức đang được lưu trong Lịch ăn của Member B,
  - *When* tác giả Member A thực hiện xóa bài công thức đó,
  - *Then* ô tương ứng trong Lịch ăn của Member B hiển thị trạng thái "Công thức không còn khả dụng" và không gây lỗi sập trang.
- **AC-20.4 (Thời gian tải trang chi tiết đáp ứng chuẩn NFR):**
  - *Given* bài công thức có đầy đủ 5 ảnh và 30 bước hướng dẫn,
  - *When* người dùng truy cập trang chi tiết,
  - *Then* toàn bộ nội dung được tải và hiển thị hoàn tất trong thời gian không vượt quá 2 giây.

---

<a id="fr-21"></a>
### FR-21 — AI hỗ trợ tạo giới thiệu hoặc bước hướng dẫn chuẩn bị/chế biến không lưu nháp server

- **Mã yêu cầu:** FR-21
- **Module:** M03, M06
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp tính năng trợ lý AI tương tác thông minh hỗ trợ Member trong quá trình tạo hoặc chỉnh sửa bài công thức nấu ăn: AI hỗ trợ gợi ý đoạn văn bản giới thiệu món ăn hấp dẫn hoặc đề xuất các bước hướng dẫn chuẩn bị/chế biến tuần tự dựa trên thông tin tác giả đã cung cấp (tên món, loại ăn chay, danh sách nguyên liệu và khẩu phần); AI tuyệt đối không tự ý thêm bất kỳ nguyên liệu mới nào ngoài danh sách tác giả đã nhập; kết quả do AI sinh ra được đưa trực tiếp vào các ô nhập liệu của biểu mẫu ở phía client ở dạng có thể chỉnh sửa tự do và chỉ được công khai khi người dùng chủ động xem xét, xác nhận (BR-15); hệ thống TUYỆT ĐỐI KHÔNG tự động công khai, không tự động lưu trữ bản nháp bền vững trên server (No Persistent Server Draft theo FR-24); gọi AI tiêu tốn 1 lượt hạn mức AI của Member (BR-03); nếu AI gặp sự cố hoặc tài khoản hết hạn mức, tác giả vẫn có toàn quyền tiếp tục tự viết và xuất bản bài viết bình thường (BR-16).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập và còn hạn mức AI trong ngày (FR-03, FR-10, BR-01, BR-02).
  - Khả năng hỗ trợ:
    1. Gợi ý đoạn giới thiệu món ăn (Description $\le 2.000$ ký tự).
    2. Đề xuất các bước hướng dẫn chuẩn bị/chế biến (Cooking Steps $\le 30$ bước, tuân thủ cấu trúc bước của FR-22).
  - Không hỗ trợ: AI không tự thêm nguyên liệu; không tự động công khai; không lưu server draft (FR-24).
- **Phân loại Actor:**
  - Primary Actor: `Member` (tác giả bài viết).
  - Supporting Actor: `Google Gemini AI` (trợ lý sinh nội dung), `Hệ thống kiểm soát hạn mức và bảo mật`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-21.1`: Yêu cầu AI gợi ý đoạn văn bản giới thiệu món ăn dựa trên nguyên liệu và tên món đã nhập.
  - `UC-21.2`: Yêu cầu AI đề xuất các bước hướng dẫn chuẩn bị/chế biến tuần tự dựa trên danh sách nguyên liệu.
- **User Stories:**
  - *Là một người thích nấu ăn nhưng ngại viết văn*, tôi muốn nhờ AI gợi ý giúp một đoạn mô tả món ăn hấp dẫn và các bước hướng dẫn chuẩn bị/chế biến cơ bản từ những nguyên liệu tôi đã chọn, để tôi có thể chỉnh sửa lại cho đúng bí quyết của mình và chủ động xác nhận đăng bài nhanh chóng hơn.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản Member hợp lệ (FR-03).
  - Tài khoản Member còn số dư hạn mức AI khả dụng trong ngày (FR-10, BR-01, BR-02).
  - Tác giả đã nhập ít nhất Tên món ăn và danh sách Nguyên liệu trong biểu mẫu (FR-16, FR-19).
- **Kích hoạt (Trigger):**
  - Tác giả nhấn nút "Nhờ AI gợi ý giới thiệu" hoặc "Nhờ AI gợi ý bước hướng dẫn" trong trình soạn thảo.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả nhấn nút "Nhờ AI gợi ý bước hướng dẫn" (hoặc "gợi ý giới thiệu").
  - Bước 2: Hệ thống kiểm tra số dư hạn mức AI của Member (FR-10, BR-01, BR-02, BR-03).
  - Bước 3: Hệ thống trích xuất thông tin tác giả đã nhập: Tên món ăn, Loại ăn chay, Khẩu phần, và Danh sách tên các nguyên liệu kèm định lượng (FR-19).
  - Bước 4: Hệ thống gửi prompt tới Google Gemini qua API bảo mật phía server (BR-06, đáp ứng NFR-03). Prompt bắt buộc yêu cầu:
    - Chỉ được sử dụng các nguyên liệu tác giả đã cung cấp.
    - Tuyệt đối không tự ý thêm nguyên liệu mới ngoài danh sách.
    - Định dạng kết quả trả về dưới dạng danh sách các bước rõ ràng hoặc đoạn văn bản giới thiệu súc tích.
  - Bước 5: Google Gemini phản hồi nội dung đề xuất thành công trong thời gian quy định tại NFR-03.
  - Bước 6: Hệ thống trừ 1 lượt hạn mức AI của Member sau khi nhận phản hồi hợp lệ (BR-03).
  - Bước 7: Hệ thống đưa nội dung AI sinh trực tiếp vào các ô nhập liệu tương ứng trên trình soạn thảo giao diện phía client dưới dạng có thể chỉnh sửa hoàn toàn.
  - Bước 8: Tác giả tự do đọc lại, chỉnh sửa câu từ, thêm bớt hoặc sắp xếp lại các bước hướng dẫn cho phù hợp với thực tế chế biến của mình. AI tuyệt đối không tự động công khai bài viết.
  - Bước 9: Khi tác giả chủ động nhấn "Đăng công thức", bài viết được kiểm tra validation (bao gồm bắt buộc có 1–30 bước với nội dung không rỗng sau khi trim theo BR-19) và xuất bản trực tiếp (BR-07, BR-19, BR-25).
  - Bước 10: Toàn bộ quá trình TUYỆT ĐỐI KHÔNG ghi bất kỳ bản ghi lưu nháp tạm thời nào vào cơ sở dữ liệu server (FR-24).
- **Luồng thay thế (Alternative Flows):**
  - *AF-21.1 (Tác giả không hài lòng với nội dung AI gợi ý):* Tác giả có thể nhấn nút "Xóa gợi ý" để quay về trạng thái trống hoặc tự gõ lại bằng tay. Hạn mức đã trừ không được hoàn lại vì dịch vụ AI đã xử lý thành công (BR-03).
  - *AF-21.2 (Tác giả hết hạn mức AI trong ngày):* Nếu tài khoản đã hết hạn mức AI, hệ thống hiển thị thông báo đã đạt giới hạn (FR-10) và gợi ý tác giả tự nhập nội dung bằng tay để tiếp tục đăng bài bình thường (BR-16).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-21.1 (Lỗi kết nối dịch vụ AI hoặc timeout):* Nếu dịch vụ AI gặp sự cố hoặc quá thời gian phản hồi quy định tại NFR-03, hệ thống thông báo lỗi kỹ thuật thân thiện, không trừ hạn mức của Member và giữ nguyên toàn bộ dữ liệu tác giả đã nhập trên form (BR-04, BR-16).
  - *SF-21.1 (Không lưu nháp server - Tuân thủ ranh giới FR-24):* Hệ thống không cung cấp chức năng lưu nháp trên máy chủ cho tính năng này; nếu tác giả rời khỏi biểu mẫu trước khi bấm công khai, dữ liệu dở dang không được bảo đảm lưu trữ bền vững trên máy chủ (FR-24).

#### 5. Hậu điều kiện (Postconditions)
- Nội dung gợi ý của AI được điền vào trình soạn thảo client dưới dạng có thể chỉnh sửa để tác giả toàn quyền kiểm soát.
- Hạn mức AI trong ngày của Member giảm đi 1 lượt khi hoàn tất thành công.
- Cơ sở dữ liệu server không lưu trữ bất kỳ bản ghi nháp dở dang nào.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chỉ Member đã đăng nhập và còn hạn mức AI mới được sử dụng.
- **Ràng buộc an toàn:** AI không tự thêm nguyên liệu; kết quả AI phải ở dạng chỉnh sửa được và AI không được tự ý công khai; không lưu nháp server (FR-24); không chặn đăng bài khi AI lỗi (BR-16).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Hạn mức text AI cho tài khoản Free.
  - [BR-02](BUSINESS-RULES.md#br-02): Hạn mức text AI cho gói Plus và Pro.
  - [BR-03](BUSINESS-RULES.md#br-03): Điều kiện trừ hạn mức AI.
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider và timeout AI.
  - [BR-06](BUSINESS-RULES.md#br-06): Bảo mật API Key của AI Provider.
  - [BR-15](BUSINESS-RULES.md#br-15): Đồng nhất chính sách bài viết tự soạn và bài có AI hỗ trợ.
  - [BR-16](BUSINESS-RULES.md#br-16): Không chặn công khai bài viết khi AI lỗi hoặc hết hạn mức.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03): Thời gian phản hồi của tính năng AI Chatbot.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-21.1 (AI sinh nội dung không tự ý thêm nguyên liệu mới):**
  - *Given* tác giả khai báo danh sách nguyên liệu chỉ gồm "Đậu phụ, Cà chua, Hành boa-rô",
  - *When* AI sinh các bước hướng dẫn chuẩn bị/chế biến,
  - *Then* toàn bộ các bước chỉ sử dụng các nguyên liệu đã khai báo và tuyệt đối không xuất hiện nguyên liệu mới (như nấm, ớt chuông...).
- **AC-21.2 (Đưa nội dung AI sinh vào trình soạn thảo client dưới dạng có thể chỉnh sửa):**
  - *Given* AI phản hồi thành công đoạn giới thiệu hoặc các bước hướng dẫn,
  - *When* dữ liệu trả về client,
  - *Then* nội dung được tự động điền vào các ô nhập liệu tương ứng trên form để tác giả có thể chỉnh sửa trực tiếp và chỉ được công khai khi tác giả chủ động bấm đăng.
- **AC-21.3 (Tuyệt đối không lưu trữ bản nháp trên server):**
  - *Given* tác giả sử dụng AI hỗ trợ soạn bài nhưng chưa nhấn nút "Đăng công thức",
  - *When* kiểm tra cơ sở dữ liệu hệ thống trên máy chủ,
  - *Then* không có bất kỳ bản ghi bài viết nháp nào được lưu trên server.
- **AC-21.4 (Không chặn xuất bản bài viết khi AI lỗi hoặc hết lượt):**
  - *Given* tài khoản Member đã hết hạn mức AI hoặc dịch vụ AI gặp sự cố,
  - *When* tác giả tự gõ nội dung vào biểu mẫu và nhấn đăng bài đạt chuẩn,
  - *Then* hệ thống công khai bài viết thành công mà không có bất kỳ trở ngại nào.
- **AC-21.5 (Trừ hạn mức chính xác 1 lượt khi thành công):**
  - *Given* Member có 5 lượt AI khả dụng,
  - *When* yêu cầu hỗ trợ tạo bài hoàn tất thành công đáp ứng thời gian quy định tại NFR-03,
  - *Then* số dư hạn mức AI của Member giảm đi chính xác 1 lượt (còn 4 lượt).

---

<a id="fr-22"></a>
### FR-22 — Thao tác chỉnh sửa và sắp xếp bước hướng dẫn chuẩn bị/chế biến

- **Mã yêu cầu:** FR-22
- **Module:** M01, M03, M06
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Trình soạn thảo bài công thức cung cấp chức năng cho phép tác giả thêm mới, chỉnh sửa nội dung, xóa bỏ và thay đổi thứ tự (sắp xếp lại vị trí) của các bước hướng dẫn chuẩn bị/chế biến; bài công thức bắt buộc phải có từ 1 đến 30 bước hướng dẫn trước khi được công khai (BR-19); mỗi bước phải có nội dung không rỗng sau khi cắt khoảng trắng đầu cuối (trim); khi bài viết được công khai hoặc hiển thị chi tiết, hệ thống trình bày các bước theo đúng thứ tự số học tuần tự đã lưu ($1, 2, 3...$); cấu trúc bước này được dùng chung đồng bộ cho cả các bước do tác giả tự nhập lẫn các bước do AI hỗ trợ đề xuất (FR-21).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi bài Recipe Post do Member hoặc Administrator tạo/sửa (FR-04, FR-07, FR-16, FR-44).
  - Giới hạn số bước: Từ 1 đến tối đa **30 bước hướng dẫn** (BR-19).
  - Tính bắt buộc: Bắt buộc có ít nhất 1 bước với nội dung không rỗng sau khi trim (BR-19).
- **Phân loại Actor:**
  - Primary Actor: `Member`, `Administrator` (tác giả bài viết).
  - Supporting Actor: `Hệ thống soạn thảo và sắp xếp bước hướng dẫn`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-22.1`: Thêm mới, chỉnh sửa nội dung hoặc xóa bỏ bước hướng dẫn chuẩn bị/chế biến trong bài công thức (1–30 bước).
  - `UC-22.2`: Thay đổi vị trí thứ tự thực hiện giữa các bước hướng dẫn trong bài công thức.
- **User Stories:**
  - *Là một tác giả công thức*, tôi muốn dễ dàng thêm bớt, chỉnh sửa và đổi thứ tự các bước hướng dẫn chuẩn bị/chế biến để các công đoạn được logic và người đọc dễ thực hiện theo nhất.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Tác giả đang ở giao diện tạo hoặc chỉnh sửa bài công thức (FR-04, FR-16).
- **Kích hoạt (Trigger):**
  - Tác giả tương tác với phần "Các bước thực hiện" trong trình soạn thảo bài viết.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả nhấn nút "Thêm bước". Hệ thống tạo một ô nhập liệu bước hướng dẫn mới với số thứ tự kế tiếp (ví dụ Bước 1).
  - Bước 2: Tác giả nhập mô tả chi tiết công đoạn thực hiện của bước đó (nội dung không được rỗng sau khi trim).
  - Bước 3: Tác giả tiếp tục thêm các bước tiếp theo (tối đa không quá 30 bước theo BR-19).
  - Bước 4: Để điều chỉnh trình tự, tác giả sử dụng thao tác di chuyển vị trí giữa các bước (ví dụ: nút lên/xuống) tới vị trí mong muốn.
  - Bước 5: Hệ thống tự động cập nhật lại số thứ tự tuần tự ($1, 2, 3...$) của toàn bộ các bước trên giao diện.
  - Bước 6: Khi tác giả nhấn lưu bài viết, hệ thống kiểm tra số bước trong khoảng 1–30 và nội dung từng bước không rỗng sau khi trim, sau đó lưu trữ danh sách các bước kèm chỉ số thứ tự tuần tự vào cơ sở dữ liệu.
  - Bước 7: Trang xem chi tiết công thức (FR-20) hiển thị các bước hướng dẫn theo đúng thứ tự đã lưu.
- **Luồng thay thế (Alternative Flows):**
  - *AF-22.1 (Xóa bớt một bước hướng dẫn):* Tác giả nhấn xóa tại một bước trung gian (ví dụ Bước 2 trong 4 bước). Hệ thống xóa bước đó và tự động đánh số lại các bước còn lại ($1, 2, 3$) một cách liền mạch.
  - *AF-22.2 (Nhận các bước do AI hỗ trợ đề xuất):* Khi tác giả sử dụng tính năng AI hỗ trợ từ FR-21, hệ thống chuyển các bước đề xuất vào danh sách các ô nhập liệu bước này để tác giả tiếp tục chỉnh sửa, sắp xếp lại và chủ động xác nhận trước khi đăng.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-22.1 (Số lượng bước không hợp lệ hoặc bước rỗng):* Khi danh sách chưa có bước nào (0 bước), vượt quá 30 bước, hoặc có bước chỉ chứa khoảng trắng khi nhấn lưu/công khai, hệ thống từ chối lưu và thông báo lỗi yêu cầu từ 1 đến 30 bước với nội dung hợp lệ (BR-19).
  - *SF-22.1 (Làm sạch nội dung mô tả bước):* Nội dung văn bản của từng bước hướng dẫn bắt buộc được làm sạch mã độc (sanitize HTML) để phòng chống lỗ hổng tấn công XSS (NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Các bước hướng dẫn chuẩn bị/chế biến được lưu trữ bền vững kèm chỉ số thứ tự chuẩn xác.
- Trang chi tiết hiển thị đúng hướng dẫn từng bước trực quan.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chỉ chính tác giả sở hữu bài viết mới có quyền chỉnh sửa và sắp xếp các bước của bài viết đó (BR-64, NFR-09). Administrator xử lý bài vi phạm theo quy trình kiểm duyệt độc lập (ẩn/gỡ bài), không trực tiếp chỉnh sửa nội dung bài của tác giả.
- **Ràng buộc nghiệp vụ:** Bắt buộc có từ 1 đến 30 bước; mỗi bước có nội dung không rỗng sau khi trim (BR-19).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post (1–30 bước hướng dẫn chuẩn bị/chế biến).
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của mô tả giới thiệu và ảnh đại diện.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến (chống XSS trong nội dung bước hướng dẫn).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-22.1 (Giới hạn số lượng từ 1 đến 30 bước hướng dẫn):**
  - *Given* bài công thức chưa có bước hướng dẫn nào (0 bước) hoặc tác giả cố gắng tạo thêm bước thứ 31,
  - *When* tác giả gửi yêu cầu đăng bài,
  - *Then* hệ thống ngăn chặn và thông báo số bước hướng dẫn phải từ 1 đến 30 bước.
- **AC-22.2 (Sắp xếp lại thứ tự các bước hướng dẫn chính xác):**
  - *Given* bài công thức có 3 bước hướng dẫn theo thứ tự A, B, C,
  - *When* tác giả di chuyển bước C lên vị trí đầu tiên,
  - *Then* hệ thống cập nhật lại thứ tự thành C (Bước 1), A (Bước 2), B (Bước 3) và lưu lại chuẩn xác.
- **AC-22.3 (Tự động đánh số lại khi xóa bước trung gian):**
  - *Given* bài công thức đang có 3 bước hướng dẫn được đánh số 1, 2, 3,
  - *When* tác giả xóa bước số 2,
  - *Then* bước số 3 cũ tự động chuyển thành bước số 2 và danh sách còn đúng 2 bước tuần tự.
- **AC-22.4 (Bắt buộc nội dung bước không rỗng sau khi trim):**
  - *Given* tác giả nhập một bước chỉ gồm các ký tự khoảng trắng hoặc để trống nội dung,
  - *When* tác giả nhấn đăng công thức,
  - *Then* hệ thống từ chối xuất bản và hiển thị thông báo lỗi yêu cầu nhập nội dung cụ thể cho từng bước hướng dẫn.

---

<a id="fr-23"></a>
### FR-23 — Hiển thị thông tin tác giả gắn liền với tài khoản

- **Mã yêu cầu:** FR-23
- **Module:** M01, M02, M03
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống định danh tác giả gắn chặt với tài khoản đã đăng ký; hiển thị thông tin tác giả trên đầu bài công thức và trang hồ sơ công khai gồm tên, avatar, giới thiệu ngắn, bài đăng, ngày tham gia và tổng Like; phân định nghiêm ngặt quyền xem dữ liệu công khai và dữ liệu riêng tư; bảo vệ quyền sở hữu nội dung và ngăn chặn lộ lọt thông tin nhạy cảm.

#### 1. Mục đích
Xác lập mối liên kết định danh bất biến giữa nội dung bài công thức (`Recipe Post`) với tài khoản thành viên (`Member`) đã tạo ra bài viết; cung cấp không gian hồ sơ công khai để cộng đồng nhận diện tác giả và đánh giá uy tín qua tổng lượt thích; đồng thời thiết lập ranh giới bảo mật nghiêm ngặt nhằm bảo vệ thông tin riêng tư (email, hồ sơ ăn uống, dinh dưỡng, lịch ăn) của người dùng.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Guest` / `Member`: Người xem duyệt xem bài công thức và trang hồ sơ công khai của tác giả.
  - `Member` (Tác giả / Chủ tài khoản): Người dùng xem và cập nhật thông tin hồ sơ của chính mình.
  - `Administrator`: Quản trị viên xem xét thông tin tài khoản phục vụ hậu kiểm nội dung vi phạm.
- **Secondary Actor / External System:**
  - `Dịch vụ lưu trữ tệp đám mây (Azure Blob Storage)`: Lưu trữ và phân phối tệp ảnh đại diện người dùng.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-23.1`: Hiển thị thông tin tác giả trên bài công thức (Display author card on Recipe Post).
  - `UC-23.2`: Xem trang hồ sơ công khai của thành viên (View public user profile).
  - `UC-23.3`: Xem và chỉnh sửa hồ sơ cá nhân của chính mình (View and edit own profile).
  *(Ghi chú: UC-23.4 trước đây về kiểm soát ranh giới dữ liệu riêng tư không còn là Use Case độc lập vì đây là cơ chế an ninh hệ thống chứ không phải mục tiêu của Actor; hành vi này được chuẩn hóa thành Quy tắc phân quyền & bảo mật, Luồng an ninh/ngoại lệ, và Tiêu chí nghiệm thu).*
- **User Stories:**
  - `US-23.1`: Là một người đọc, tôi muốn thấy tên, ảnh đại diện và liên kết hồ sơ của tác giả ở đầu bài công thức để biết ai là người sáng tạo món ăn.
  - `US-23.2`: Là một người đọc, tôi muốn xem trang hồ sơ công khai của tác giả để biết ngày tham gia, giới thiệu ngắn, tổng Like nhận được và danh sách các món ăn khác của tác giả.
  - `US-23.3`: Là một thành viên, tôi muốn cập nhật tên hiển thị, ảnh đại diện và phần giới thiệu ngắn để cá nhân hóa hình ảnh của mình trong cộng đồng.
  - `US-23.4`: Là một thành viên, tôi muốn địa chỉ email, thông tin xác thực, hồ sơ dị ứng/ăn chay riêng tư và dữ liệu dinh dưỡng/lịch ăn của tôi được bảo mật tuyệt đối, không bị lộ ra ngoài trang công khai.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:**
  - Để xem thông tin tác giả / hồ sơ công khai: Bài công thức đang ở trạng thái công khai (`PUBLISHED`) hoặc tài khoản người dùng đang tồn tại trong hệ thống.
  - Để chỉnh sửa hồ sơ cá nhân: Người dùng đã đăng nhập với vai trò Member và là chủ sở hữu của tài khoản đó (`Resource Owner`).
- **Trigger:** Người dùng mở trang chi tiết bài công thức, nhấp vào liên kết hồ sơ tác giả, hoặc truy cập vào trang quản lý hồ sơ cá nhân.

#### 5. Ranh giới dữ liệu công khai và dữ liệu riêng tư (Data Visibility Boundary)

| Nhóm dữ liệu | Phạm vi hiển thị | Danh mục trường dữ liệu | Ràng buộc bảo mật & hiển thị |
|---|---|---|---|
| **Hồ sơ công khai (Public Profile)** | Công khai cho toàn bộ người dùng (Guest, Member, Admin) | - Tên hiển thị (3–50 ký tự)<br>- Ảnh đại diện (avatar)<br>- Giới thiệu ngắn (bio, tối đa 500 ký tự)<br>- Thời điểm tham gia (định dạng Tháng/Năm)<br>- Danh sách Recipe Post đang công khai<br>- Tổng lượt Thích nhận được | - Nếu chưa có avatar: hiển thị ảnh mặc định hệ thống.<br>- Nếu bio trống: cho phép để trống/ẩn.<br>- Tuyệt đối KHÔNG gán nhãn "Chuyên gia", "Bác sĩ" hoặc "Đã xác minh danh tính ngoài đời" (BR-18).<br>- Không hiển thị bài viết đang bị ẩn do vi phạm. |
| **Dữ liệu tài khoản & hồ sơ riêng tư (Private Account & Profile Data)** | Mặc định độc quyền chủ sở hữu (`Owner-only by default`); Administrator chỉ được tiếp cận các thông tin định danh và quản trị tối thiểu được BR/NFR cho phép rõ ràng | - Địa chỉ email đăng ký<br>- Trạng thái xác minh email<br>- Trạng thái tài khoản (`ACTIVE`, `LOCKED`)<br>- Hồ sơ sở thích ăn chay (FR-31)<br>- Hồ sơ dinh dưỡng & chỉ số sức khỏe (FR-35, FR-38)<br>- Công thức đã lưu (FR-32)<br>- Lịch ăn tuần (FR-09, FR-33)<br>- Danh sách mua sắm (FR-53)<br>- Báo cáo vi phạm đã gửi (FR-26) | - Máy chủ bắt buộc kiểm tra quyền sở hữu (`Ownership Check`): Dữ liệu cá nhân hóa (hồ sơ dinh dưỡng, chỉ số sức khỏe/BMI, sở thích ăn uống, công thức đã lưu, lịch ăn, danh sách mua sắm) thuộc quyền ĐỘC QUYỀN của chính chủ tài khoản (`Resource Owner`).<br>- Administrator TUYỆT ĐỐI KHÔNG tự động có quyền truy cập dữ liệu cá nhân hóa (nghiêm cấm quy tắc generic `isOwner || isAdmin`).<br>- Quyền của Administrator là riêng biệt theo từng tài nguyên (resource-specific) và giới hạn ở mức tối thiểu cần thiết để thực thi nhiệm vụ quản trị (account ID, email quản trị, trạng thái tài khoản, lịch sử kiểm duyệt/báo cáo) theo BR/NFR.<br>- Tuyệt đối không trả các trường riêng tư trong giao diện hay dữ liệu public profile. |
| **Dữ liệu an ninh / Thông tin định danh nhạy cảm nội bộ (Security / Credential Internals)** | Xử lý nội bộ độc quyền bởi các thành phần an ninh/xác thực tin cậy của hệ thống (System Security Components Only) | - Mật khẩu / Password hash<br>- Refresh tokens / Refresh session material<br>- Active session credentials / tokens<br>- Password reset token<br>- Email verification token<br>- Authentication secrets / API keys | - **Tuyệt đối KHÔNG BAO GIỜ** để lộ qua Public Profile.<br>- **Tuyệt đối KHÔNG BAO GIỜ** trả về qua Member UI/API thông thường.<br>- **Tuyệt đối KHÔNG BAO GIỜ** trả về qua Administrator UI/API thông thường.<br>- Administrator không được phép truy cập hoặc xem các bí mật xác thực này chỉ vì có vai trò Admin (tuân thủ nguyên tắc đặc quyền tối thiểu - Least Privilege).<br>- Chỉ được xử lý bởi các thành phần an ninh/xác thực tin cậy của hệ thống khi có yêu cầu kỹ thuật hợp lệ. |

#### 6. Luồng sự kiện (Flow of Events)

##### A. Luồng Hiển thị thông tin tác giả trên bài công thức (UC-23.1)
1. **Main Flow:**
   - Bước 1: Người dùng (Guest hoặc Member) mở trang chi tiết bài công thức hợp lệ đang công khai.
   - Bước 2: Tại đầu trang chi tiết, hệ thống hiển thị khối thông tin tác giả: ảnh đại diện, tên hiển thị công khai, thời điểm công khai bài viết, và tổng số lượt Thích của bài viết đó.
   - Bước 3: Người dùng có thể nhấp vào ảnh đại diện hoặc tên tác giả để điều hướng sang trang Hồ sơ công khai của tác giả.

##### B. Luồng Xem trang hồ sơ công khai của thành viên (UC-23.2)
1. **Main Flow:**
   - Bước 1: Người dùng truy cập đường dẫn hồ sơ công khai của một thành viên.
   - Bước 2: Ứng dụng gửi yêu cầu lấy thông tin hồ sơ công khai tới máy chủ.
   - Bước 3: Hệ thống truy xuất dữ liệu, tổng hợp các thông tin công khai: tên hiển thị, ảnh đại diện, giới thiệu ngắn, thời điểm tham gia, danh sách các bài công thức đang công khai của thành viên, và tính tổng số lượt Thích nhận được trên các bài viết đó.
   - Bước 4: Hệ thống phản hồi dữ liệu chỉ chứa thông tin công khai; tuyệt đối không chứa bất kỳ trường dữ liệu riêng tư nào.
   - Bước 5: Giao diện hiển thị trang hồ sơ công khai của tác giả kèm danh sách các bài công thức.
2. **Alternative Flows:**
   - *Tác giả chưa có bài viết nào:* Danh sách bài viết hiển thị trạng thái rỗng ("Tác giả chưa có bài công thức nào được công khai").
   - *Tác giả chưa nhận được Thích:* Tổng số lượt Thích hiển thị giá trị 0.
   - *Tác giả chưa tải ảnh đại diện:* Giao diện hiển thị ảnh đại diện mặc định chuẩn của hệ thống.
3. **Error Flows:**
   - *Tài khoản không tồn tại:* Nếu mã định danh người dùng không tồn tại trong hệ thống, hệ thống hiển thị thông báo "Không tìm thấy hồ sơ người dùng".

##### C. Luồng Xem và chỉnh sửa hồ sơ cá nhân của chính mình (UC-23.3)
1. **Main Flow:**
   - Bước 1: Member đã đăng nhập truy cập trang Cài đặt tài khoản / Quản lý hồ sơ cá nhân.
   - Bước 2: Hệ thống xác thực phiên đăng nhập, lấy thông tin cá nhân của chính Member đó và hiển thị lên biểu mẫu.
   - Bước 3: Member có thể chỉnh sửa: Tên hiển thị (3–50 ký tự), Giới thiệu ngắn ($\le 500$ ký tự), hoặc tải lên tệp ảnh đại diện mới (định dạng JPEG/PNG/WebP, dung lượng $\le 2$ MB).
   - Bước 4: Member nhấn "Lưu thay đổi".
   - Bước 5: Hệ thống kiểm tra tính hợp lệ của dữ liệu, tải ảnh lên dịch vụ lưu trữ đám mây (Azure Blob Storage) nếu có thay đổi ảnh, cập nhật thông tin trong cơ sở dữ liệu và phản hồi thành công.
   - Bước 6: Thông tin mới được cập nhật đồng bộ trên trang cá nhân và khối tác giả trên các bài công thức đã đăng của thành viên.
2. **Error Flows:**
   - *Tên hiển thị không hợp lệ:* Tên dưới 3 ký tự, vượt quá 50 ký tự hoặc chứa ký tự bị cấm -> Hệ thống từ chối cập nhật và hiển thị thông báo lỗi.
   - *Tệp ảnh không hợp lệ:* Tệp không đúng định dạng ảnh cho phép hoặc vượt quá dung lượng quy định -> Hệ thống từ chối và hiển thị thông báo lỗi.

##### D. Luồng An ninh — Kiểm soát quyền sở hữu và bảo mật ranh giới riêng tư
1. **Main Flow (Ngăn chặn truy cập trái phép dữ liệu cá nhân):**
   - Bước 1: Người dùng A (hoặc Quản trị viên) gửi yêu cầu truy xuất tài nguyên riêng tư của người dùng B (ví dụ: công thức đã lưu, lịch ăn tuần, danh sách mua sắm, sở thích ăn chay, hồ sơ dinh dưỡng và chỉ số sức khỏe).
   - Bước 2: Máy chủ trích xuất định danh và vai trò người gọi từ phiên xác thực hợp lệ.
   - Bước 3: Máy chủ kiểm tra quyền sở hữu tài nguyên theo nguyên tắc: Tài nguyên cá nhân riêng tư mặc định thuộc quyền ĐỘC QUYỀN của chính chủ sở hữu (`Owner-only by default`). Administrator KHÔNG tự động có quyền truy cập các dữ liệu cá nhân hóa này (không áp dụng quy tắc gộp `isOwner || isAdmin`). Quyền của Administrator chỉ áp dụng riêng cho các thông tin định danh và quản trị tối thiểu được BR/NFR cho phép rõ ràng (như trạng thái tài khoản, email phục vụ quản trị, báo cáo vi phạm).
   - Bước 4: Phát hiện người gọi không phải là chính chủ sở hữu tài nguyên (Resource Owner), máy chủ lập tức từ chối yêu cầu và phản hồi mã lỗi HTTP 403 Forbidden, bảo đảm an toàn dữ liệu riêng tư tuyệt đối.
2. **Main Flow (Gán quyền tác giả bất biến khi tạo bài công thức):**
   - Khi Member tạo hoặc công khai bài công thức (`FR-04`, `FR-25`), hệ thống tự động gán mã định danh của thành viên đang đăng nhập làm tác giả duy nhất của bài viết.
   - Người đăng TUYỆT ĐỐI KHÔNG được chọn tài khoản khác hoặc gửi tham số tác giả tùy ý từ máy khách (BR-17).
   - Tác giả chỉ có quyền sửa hoặc xóa bài viết do chính mình làm tác giả theo BR-64.

#### 7. Hậu điều kiện (Postconditions)
- Khi bài viết được công khai: Tác giả được gắn cố định với tài khoản người tạo; thông tin tác giả có thể xem được từ trang chi tiết bài và liên kết tới hồ sơ công khai.
- Khi tác giả cập nhật hồ sơ cá nhân: Tên hiển thị và ảnh đại diện mới được cập nhật đồng bộ trên trang cá nhân và các bài viết công khai của tác giả.
- Dữ liệu riêng tư của người dùng được bảo vệ an toàn ở tầng máy chủ, không bị lộ lọt qua các giao diện hay dịch vụ công khai.

#### 8. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Mọi người dùng (Guest, Member, Admin) đều có quyền xem thông tin công khai của tác giả và trang hồ sơ công khai của thành viên.
- Quy tắc phân quyền tài nguyên riêng tư: Mặc định là ĐỘC QUYỀN CHỦ SỞ HỮU (`Owner-only by default`). Chỉ chính chủ sở hữu tài khoản (`Resource Owner`) mới có quyền xem, truy xuất và chỉnh sửa dữ liệu cá nhân hóa của mình (hồ sơ dinh dưỡng, chỉ số BMI/sức khỏe, sở thích ăn uống, công thức đã lưu, lịch ăn tuần, danh sách mua sắm).
- Administrator tuyệt đối KHÔNG tự động được cấp quyền truy cập vào các dữ liệu cá nhân hóa trên (nghiêm cấm sử dụng logic phân quyền generic `isOwner || isAdmin`).
- Quyền của Administrator là quyền hạn riêng biệt theo từng tài nguyên (resource-specific) và phải được quy định rõ ràng trong các BR/NFR: Administrator chỉ được tiếp cận các thông tin quản trị tối thiểu phục vụ nhiệm vụ (account ID, email quản trị, trạng thái tài khoản, thông tin báo cáo/kiểm duyệt vi phạm).
- Tách biệt rõ ràng ranh giới Security / Credential Internals (mật khẩu băm, token bí mật): các thông tin này tuyệt đối không trả về qua bất kỳ UI/API người dùng hay quản trị viên nào, tuân thủ nguyên tắc đặc quyền tối thiểu (Least Privilege).
- Máy chủ bắt buộc thực thi kiểm tra quyền sở hữu (Ownership Check) ở tầng nghiệp vụ máy chủ trước khi truy xuất hoặc thay đổi bất kỳ tài nguyên cá nhân nào; việc ẩn nút trên giao diện máy khách không thay thế được kiểm tra bảo mật phía máy chủ (NFR-08, NFR-09).
- Không cấp nhãn chuyên gia, bác sĩ dinh dưỡng hoặc cam kết xác minh danh tính thật trên hồ sơ công khai (BR-18).
- Ràng buộc kỹ thuật được phê duyệt: Lưu trữ ảnh đại diện trên dịch vụ Azure Blob Storage; bảo vệ kênh truyền qua HTTPS/TLS 1.2+ (NFR-08).

#### 9. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản đăng bài (không được chọn tài khoản khác, AI không thay thế tác giả).
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai (không lộ email, thông tin đăng nhập, hồ sơ ăn uống/sức khỏe riêng tư, không nhãn chuyên gia).
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân & chỉ số sức khỏe, HTTPS/TLS 1.2+, kiểm tra quyền sở hữu ở Backend.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn truy cập trái quyền.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt, tương thích đa độ phân giải.

#### 10. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-23.1 — Hiển thị đầy đủ thông tin tác giả trên trang bài công thức:**
  - **Given:** Một Recipe Post hợp lệ đang ở trạng thái công khai được tạo bởi tác giả `NguyenVanA`.
  - **When:** Người dùng (Guest hoặc Member) truy cập vào trang chi tiết bài công thức đó.
  - **Then:** Đầu trang hiển thị rõ ràng tên hiển thị `NguyenVanA`, ảnh đại diện (hoặc ảnh mặc định), ngày đăng bài, và liên kết dẫn tới trang hồ sơ công khai của tác giả.

- **AC-23.2 — Xem trang hồ sơ công khai chỉ chứa thông tin được phép:**
  - **Given:** Người dùng truy cập trang hồ sơ công khai của tác giả `NguyenVanA`.
  - **When:** Trang hồ sơ tải hoàn tất.
  - **Then:** Giao diện hiển thị: tên hiển thị, ảnh đại diện, giới thiệu ngắn (nếu có), thời điểm tham gia (Tháng/Năm), tổng số lượt Thích nhận được, và danh sách các bài công thức đang công khai của tác giả; tuyệt đối KHÔNG có email, mật khẩu/password hash, token xác thực, hồ sơ dị ứng, dữ liệu dinh dưỡng, lịch ăn hay danh sách đã lưu.

- **AC-23.3 — Hiển thị ảnh đại diện mặc định khi tác giả chưa tải ảnh lên:**
  - **Given:** Thành viên `NguyenVanB` có tài khoản nhưng chưa tải ảnh đại diện cá nhân lên hệ thống.
  - **When:** Người dùng xem bài viết hoặc trang hồ sơ công khai của `NguyenVanB`.
  - **Then:** Hệ thống hiển thị ảnh đại diện mặc định chuẩn của ứng dụng, không bị lỗi hiển thị hình ảnh.

- **AC-23.4 — Tác giả cập nhật thành công tên hiển thị và giới thiệu ngắn:**
  - **Given:** Member đang đăng nhập vào tài khoản của chính mình và truy cập màn hình Cài đặt hồ sơ.
  - **When:** Member cập nhật tên hiển thị hợp lệ (3–50 ký tự) và đoạn giới thiệu ngắn, rồi nhấn "Lưu thay đổi".
  - **Then:** Hệ thống xác thực quyền sở hữu, cập nhật dữ liệu vào cơ sở dữ liệu, phản hồi thành công; thông tin mới lập tức được phản ánh trên trang hồ sơ và các bài viết của tác giả.

- **AC-23.5 — Tác giả tải lên ảnh đại diện mới hợp lệ thành công:**
  - **Given:** Member đang ở màn hình Cài đặt hồ sơ của chính mình.
  - **When:** Member chọn một tệp ảnh đại diện hợp lệ (JPEG/PNG/WebP, dung lượng $\le 2$ MB) và lưu thay đổi.
  - **Then:** Hệ thống lưu tệp ảnh lên dịch vụ lưu trữ đám mây, cập nhật liên kết ảnh đại diện của tài khoản, và hiển thị ảnh mới trên giao diện.

- **AC-23.6 — Từ chối cập nhật hồ sơ khi tên hiển thị không hợp lệ:**
  - **Given:** Member đang ở màn hình Cài đặt hồ sơ.
  - **When:** Member nhập tên hiển thị dưới 3 ký tự hoặc vượt quá 50 ký tự và nhấn "Lưu thay đổi".
  - **Then:** Hệ thống từ chối cập nhật, giữ nguyên tên cũ, và hiển thị thông báo lỗi yêu cầu độ dài tên từ 3 đến 50 ký tự.

- **AC-23.7 — Từ chối cập nhật hồ sơ khi tệp ảnh không đúng định dạng hoặc quá dung lượng:**
  - **Given:** Member đang ở màn hình Cài đặt hồ sơ.
  - **When:** Member chọn tệp không phải định dạng ảnh cho phép hoặc có dung lượng vượt quá 2 MB và nhấn lưu.
  - **Then:** Hệ thống từ chối tiếp nhận tệp, không cập nhật ảnh đại diện, và hiển thị thông báo lỗi tương ứng.

- **AC-23.8 — Chặn truy cập trái phép vào dữ liệu cá nhân riêng tư (Nguyên tắc Owner-only mặc định):**
  - **Given:** Người dùng A (bao gồm cả tài khoản có vai trò Quản trị viên - Administrator) không phải là chủ sở hữu của tài khoản B.
  - **When:** Người dùng A gửi yêu cầu truy xuất dữ liệu cá nhân riêng tư của tài khoản B (chỉ số sức khỏe/BMI, hồ sơ dinh dưỡng, sở thích ăn chay, công thức đã lưu, lịch ăn tuần, danh sách mua sắm).
  - **Then:** Máy chủ kiểm tra quyền sở hữu theo nguyên tắc Owner-only mặc định, từ chối yêu cầu với mã phản hồi HTTP 403 Forbidden và không tiết lộ bất kỳ dữ liệu riêng tư nào của tài khoản B (nghiêm cấm áp dụng logic gộp `isOwner || isAdmin`; quyền của Administrator là quyền hạn riêng biệt theo từng tài nguyên và chỉ áp dụng cho dữ liệu quản trị tối thiểu được BR/NFR cho phép rõ ràng).

- **AC-23.9 — Gán quyền tác giả tự động và chống mạo danh khi tạo bài:**
  - **Given:** Member `UserX` đang đăng nhập với phiên xác thực hợp lệ.
  - **When:** `UserX` gửi yêu cầu tạo Recipe Post mới có kèm thông tin giả mạo mã tác giả của tài khoản khác.
  - **Then:** Hệ thống bỏ qua thông tin tác giả gửi lên, tự động trích xuất định danh thật của `UserX` từ phiên xác thực và gắn làm tác giả bài viết; bài viết được tạo thuộc về đúng tài khoản `UserX`.

- **AC-23.10 — Chặn sửa hoặc xóa bài công thức không thuộc quyền sở hữu:**
  - **Given:** Bài công thức thuộc quyền sở hữu của tác giả A.
  - **When:** Người dùng B (không phải tác giả A) gửi yêu cầu chỉnh sửa hoặc xóa bài viết đó thông qua luồng quản lý bài cá nhân.
  - **Then:** Hệ thống kiểm tra quyền tác giả (BR-64), từ chối thao tác với mã lỗi HTTP 403 Forbidden và giữ nguyên trạng thái bài viết; Administrator chỉ xử lý bài vi phạm qua quy trình kiểm duyệt độc lập (ẩn/gỡ vi phạm), không tự ý chỉnh sửa nội dung bài của tác giả.

---

<a id="fr-24"></a>
### FR-24 — Lưu nháp Recipe Post chưa đầy đủ trên server

- **Mã yêu cầu:** FR-24
- **Module:** M03
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Mô tả:** Hệ thống không hỗ trợ lưu trữ trạng thái bản nháp (`Draft`) của Recipe Post trên máy chủ hay cơ sở dữ liệu:
  - **Ranh giới máy chủ:** Máy chủ chỉ tiếp nhận và lưu trữ bài viết khi toàn bộ dữ liệu đạt chuẩn validation đầy đủ để công khai ngay (`PUBLISHED` theo FR-25). Bất kỳ yêu cầu nào chứa dữ liệu dở dang hoặc vi phạm BR-19 đều bị máy chủ từ chối, không tạo bản ghi nháp rác và không lưu tệp media mồ côi trên dịch vụ lưu trữ.
  - **Hành vi phía giao diện người dùng:** Khi bài viết chưa đạt chuẩn hoặc người dùng đang soạn thảo dở dang, nội dung chỉ tồn tại tạm thời trên biểu mẫu của giao diện để người dùng tiếp tục hoàn thiện. Nếu người dùng chủ động đóng trang hoặc thoát phiên làm việc mà chưa công khai thành công, nội dung chưa hoàn tất đó không được bảo đảm lưu trữ bền vững trên máy chủ.

---

<a id="fr-25"></a>
### FR-25 — Công khai Recipe Post ngay sau khi validation đạt

- **Mã yêu cầu:** FR-25
- **Module:** M02 (Identity & Access), M03 (Community Recipes & Social)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Khi Member đã đăng nhập nhấn "Công khai bài viết" và toàn bộ các điều kiện kiểm tra tính hợp lệ (validation rules theo SRS 3.9) đều thỏa mãn, hệ thống lập tức chuyển trạng thái bài viết thành công khai (`PUBLISHED`) và hiển thị ngay trên toàn hệ thống; không áp dụng luồng duyệt trước từng bài hay trạng thái Pending Review ở cấp bài viết (BR-07).

#### 1. Mục đích
Triển khai cơ chế xuất bản bài viết công khai trực tiếp (Direct Publishing); giảm thiểu tối đa độ trễ trong việc chia sẻ công thức; tạo sự hứng khởi cho người dùng tham gia đóng góp nội dung; đồng thời phân định ranh giới rõ ràng giữa việc xuất bản nội dung và việc kiểm soát vi phạm qua cơ chế hậu kiểm (BR-07, FR-06).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member` (Tác giả): Người dùng thực hiện công khai bài viết.
- **Secondary Actor / External System:**
  - `Hệ thống kiểm tra tính hợp lệ (Validation Engine)`: Tự động thẩm định dữ liệu bài viết trước khi xuất bản.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-25.1`: Xuất bản trực tiếp bài công thức sau khi kiểm tra hợp lệ (Publish recipe post directly).
- **User Stories:**
  - `US-25.1`: Là một thành viên vừa nấu xong món ngon và ghi lại công thức, tôi muốn bài viết của mình xuất hiện ngay trên trang khám phá để cộng đồng có thể xem ngay mà không phải chờ Quản trị viên duyệt thủ công từng bài.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Người dùng đã đăng nhập với tài khoản Member hoạt động (`ACTIVE`) và đã hoàn thiện nội dung bài công thức trên biểu mẫu.
- **Trigger:** Tác giả nhấn nút "Công khai bài viết".

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Xuất bản trực tiếp khi đạt validation (UC-25.1)
1. **Main Flow:**
   - Bước 1: Member nhấn nút "Công khai bài viết" trên giao diện soạn thảo công thức.
   - Bước 2: Hệ thống kích hoạt quy trình thẩm định tính hợp lệ toàn diện ở tầng máy chủ (Server-side Validation) đối chiếu trực tiếp với bộ quy tắc Recipe Validation Profile chuẩn được định nghĩa tại FR-16 và các Business Rules liên quan:
     - Thẩm định cấu trúc và độ dài trường theo FR-16: tiêu đề (3–120 ký tự), khẩu phần (1–50), thời gian chuẩn bị và nấu (mỗi giá trị 0–1.440 phút, tổng thời gian $> 0$), mô tả bài viết ($\le 2.000$ ký tự), chọn đúng 1 trong 4 loại ăn chay chuẩn (BR-07, BR-19).
     - Thẩm định nguyên liệu theo FR-19: danh sách từ 1 đến 50 dòng, có tên nguyên liệu và định lượng hợp lệ (số lượng + đơn vị đo chuẩn hoặc "vừa đủ").
     - Thẩm định các bước hướng dẫn chuẩn bị/chế biến theo FR-22: bắt buộc có từ 1 đến 30 bước, mỗi bước có nội dung không rỗng sau khi trim (BR-19).
     - Thẩm định tệp ảnh và video theo FR-14 và FR-15: tối đa 5 ảnh JPEG/PNG/WebP ($\le 5$ MB/ảnh) và tối đa 1 liên kết YouTube hợp lệ.
     - Xác thực quyền tác giả: Tác giả được trích xuất tự động và gắn cố định từ phiên đăng nhập hợp lệ của Member theo FR-23 và BR-17.
   - Bước 3: Toàn bộ các điều kiện đều thỏa mãn.
   - Bước 4: Hệ thống cập nhật trạng thái bài viết thành `PUBLISHED`, lưu thời điểm công khai (`publishedAt`), và phản hồi thành công.
   - Bước 5: Bài viết xuất hiện ngay lập tức trên trang chủ, trang khám phá, kết quả tìm kiếm và trang hồ sơ cá nhân của tác giả.
   - Bước 6: Giao diện chuyển hướng tác giả đến trang chi tiết bài viết vừa xuất bản kèm thông báo chúc mừng.
2. **Exception Flow (Kiểm tra dữ liệu không đạt):**
   - Bước 1: Khi phát hiện dữ liệu vi phạm bộ quy tắc Recipe Validation Profile (ví dụ: tiêu đề dưới 3 ký tự, chưa có dòng nguyên liệu nào, chưa có bước hướng dẫn nào hoặc bước rỗng, tổng thời gian bằng 0):
   - Bước 2 (Xử lý máy chủ): Máy chủ thực thi thẩm định độc lập, lập tức từ chối yêu cầu xuất bản không hợp lệ và dừng quy trình, hoàn toàn không tạo bản ghi nào trong cơ sở dữ liệu ở bất kỳ trạng thái nào (kể cả nháp hay công khai).
   - Bước 3 (Xử lý giao diện): Giao diện giữ nguyên nội dung tác giả đã nhập, không làm mất dữ liệu biểu mẫu và hiển thị thông báo lỗi cụ thể tương ứng với trường không hợp lệ để tác giả tiếp tục hoàn thiện.

#### 6. Hậu điều kiện (Postconditions)
- Bài viết được chuyển sang trạng thái `PUBLISHED` và có thể tiếp cận công khai bởi toàn bộ người dùng (Guest, Member, Admin).
- Không tạo ra bất kỳ trạng thái trung gian nào như `Draft`, `Pending Review`, hoặc `Waiting for Approval` ở cấp bài viết (BR-07).

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Chỉ Member đã xác thực mới có quyền xuất bản bài công thức (BR-05, BR-07, NFR-09).
- Hệ thống tuyệt đối không cho phép client gửi trường `status` tùy ý; trạng thái `PUBLISHED` chỉ do máy chủ thiết lập sau khi toàn bộ điều kiện kiểm tra đã vượt qua thành công.

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản đăng bài.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post.
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của mô tả giới thiệu và ảnh đại diện.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và toàn vẹn giao dịch.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-25.1 — Công khai bài viết thành công ngay lập tức khi validation đạt:**
  - **Given:** Member đã nhập đầy đủ và chính xác toàn bộ các trường bắt buộc của bài viết thỏa mãn Recipe Validation Profile của FR-16.
  - **When:** Member nhấn "Công khai bài viết".
  - **Then:** Hệ thống chuyển trạng thái bài viết thành `PUBLISHED` ngay lập tức mà không cần phê duyệt trước (BR-07), gán đúng tác giả (BR-17) và chuyển hướng tới trang chi tiết bài viết.

- **AC-25.2 — Bài viết vừa công khai xuất hiện ngay trong kết quả tìm kiếm:**
  - **Given:** Bài công thức vừa được xuất bản thành công với tiêu đề "Nấm đùi gà kho tiêu".
  - **When:** Người dùng khác tìm kiếm từ khóa "Nấm đùi gà kho tiêu".
  - **Then:** Bài viết xuất hiện ngay trong kết quả tìm kiếm công khai.

- **AC-25.3 — Chặn xuất bản và hiển thị lỗi cụ thể khi có trường vi phạm validation:**
  - **Given:** Member đang ở biểu mẫu công thức nhưng bỏ trống danh sách nguyên liệu.
  - **When:** Member nhấn "Công khai bài viết".
  - **Then:** Hệ thống từ chối xuất bản, giữ nguyên dữ liệu đã nhập và hiển thị thông báo lỗi yêu cầu thêm nguyên liệu.

- **AC-25.4 — Gán quyền tác giả bất biến từ phiên đăng nhập khi công khai bài:**
  - **Given:** Member `UserA` đang đăng nhập với phiên xác thực hợp lệ.
  - **When:** `UserA` công khai một bài công thức mới.
  - **Then:** Bài viết được gán cố định tác giả là `UserA`; không có bất kỳ cách nào để gán sang tài khoản khác từ client (BR-17).

---

<a id="fr-26"></a>
### FR-26 — Gửi báo cáo bài công thức có vấn đề

- **Mã yêu cầu:** FR-26
- **Module:** M02 (Identity & Access), M03 (Community Recipes & Social), M09 (Administration & Moderation)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống cho phép tài khoản đã đăng nhập gửi báo cáo bài công thức có vấn đề; Guest được yêu cầu đăng nhập khi bấm "Báo cáo" (BR-24); Administrator tiếp nhận, xem xét và xử lý báo cáo theo quy trình hậu kiểm; ngăn chặn cùng một tài khoản tạo báo cáo mở trùng lặp trên cùng một bài viết (FR-30).

#### 1. Mục đích
Thiết lập cơ chế giám sát cộng đồng (crowdsourced moderation) nhằm phát hiện kịp thời các nội dung bài viết vi phạm tiêu chuẩn món chay, gây nguy hiểm an toàn thực phẩm, phản cảm hoặc vi phạm bản quyền; cung cấp thông tin đầu vào chuẩn xác phục vụ quy trình xử lý hậu kiểm của Quản trị viên (FR-06, BR-26).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member` (Người báo cáo): Gửi phản ánh về bài công thức có vấn đề.
  - `Guest`: Người dùng chưa đăng nhập được yêu cầu xác thực.
- **Secondary Actor / External System:**
  - `Administrator`: Quản trị viên tiếp nhận và xử lý báo cáo.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-26.1`: Gửi báo cáo bài công thức có vấn đề (Report inappropriate recipe post).
- **User Stories:**
  - `US-26.1`: Là một thành viên, khi tôi thấy một bài viết ghi "Ăn chay" nhưng lại hướng dẫn dùng nước mắm cá hoặc mỡ lợn, tôi muốn gửi báo cáo để Ban quản trị xử lý nhằm bảo vệ người ăn chay khác.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Bài công thức đang ở trạng thái công khai (`PUBLISHED`). Người gửi báo cáo đã đăng nhập với tài khoản Member hoạt động (`ACTIVE`).
- **Trigger:** Member nhấn nút "Báo cáo bài viết" trên trang chi tiết bài công thức.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Gửi báo cáo bài công thức (UC-26.1)
1. **Main Flow:**
   - Bước 1: Member xem bài viết công thức và nhấn nút "Báo cáo bài viết".
   - Bước 2: Hệ thống kiểm tra xem tài khoản này đã có báo cáo nào đang mở (`OPEN` hoặc `IN_REVIEW`) đối với bài viết này chưa (FR-30). Chưa có báo cáo mở nào.
   - Bước 3: Giao diện hiển thị biểu mẫu báo cáo bài công thức theo 6 nhóm lý do chuẩn (FR-27).
   - Bước 4: Member chọn lý do phù hợp và nhập mô tả chi tiết (nếu cần), rồi nhấn "Gửi báo cáo".
   - Bước 5: Máy chủ kiểm tra dữ liệu hợp lệ, lưu bản ghi báo cáo mới ở trạng thái Chờ xử lý (`OPEN`), gắn với ID bài viết và ID người báo cáo.
   - Bước 6: Hệ thống phản hồi thông báo tiếp nhận thành công; bài viết vẫn tiếp tục duy trì trạng thái công khai bình thường cho đến khi Admin có quyết định xử lý (BR-26).
2. **Exception Flow (Trùng lặp báo cáo đang mở):**
   - Nếu Member đã có báo cáo đang mở trên bài viết này, hệ thống thông báo: "Bạn đã có một báo cáo đang chờ xử lý cho bài viết này" và chuyển hướng người dùng sang giao diện bổ sung thông tin (FR-30).
3. **Security Flow (Guest bấm báo cáo):**
   - Nếu Guest nhấn "Báo cáo bài viết", hệ thống yêu cầu đăng nhập trước khi báo cáo (BR-24).

#### 6. Hậu điều kiện (Postconditions)
- Bản ghi báo cáo mới được lưu trữ trong hệ thống ở trạng thái `OPEN`.
- Bài viết bị báo cáo vẫn duy trì hiển thị cho đến khi Quản trị viên ra phán quyết chế tài.

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Bắt buộc xác thực tài khoản Member khi gửi báo cáo; tuyệt đối không chấp nhận báo cáo ẩn danh từ Guest nhằm chống quấy rối hoặc spam giả mạo (BR-24, NFR-09).
- Danh tính của người báo cáo được bảo mật nghiêm ngặt và không bao giờ tiết lộ cho tác giả bài viết bị báo cáo (FR-29, BR-18).

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
  - [BR-26](BUSINESS-RULES.md#br-26): Các hình thức chế tài khi xử lý vi phạm.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân & truyền tải an toàn.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-26.1 — Member gửi báo cáo bài công thức thành công:**
  - **Given:** Member đang đăng nhập và chưa từng có báo cáo đang mở trên bài viết A.
  - **When:** Member chọn lý do báo cáo hợp lệ và nhấn "Gửi báo cáo".
  - **Then:** Hệ thống lưu báo cáo ở trạng thái `OPEN` và phản hồi thông báo đã tiếp nhận thành công.

- **AC-26.2 — Ngăn chặn tạo báo cáo mới trùng lặp từ cùng tài khoản:**
  - **Given:** Member đã có một báo cáo ở trạng thái `OPEN` đối với bài viết A.
  - **When:** Member cố tình gửi thêm một báo cáo mới cho bài viết A.
  - **Then:** Hệ thống chặn tạo mới và thông báo tài khoản đã có báo cáo đang chờ xử lý (FR-30).

- **AC-26.3 — Yêu cầu đăng nhập khi Guest nhấn nút Báo cáo bài viết:**
  - **Given:** Người dùng chưa đăng nhập (Guest) xem bài viết.
  - **When:** Guest nhấn nút "Báo cáo bài viết".
  - **Then:** Hệ thống hiển thị hộp thoại yêu cầu đăng nhập và không mở biểu mẫu báo cáo (BR-24).

- **AC-26.4 — Duy trì hiển thị bài viết bình thường khi chưa có quyết định của Admin:**
  - **Given:** Bài công thức A vừa nhận thêm một báo cáo mới.
  - **When:** Người dùng khác truy cập vào bài công thức A.
  - **Then:** Bài viết A vẫn hiển thị bình thường, không bị tự động ẩn trước khi Admin xem xét (BR-26).

---

<a id="fr-27"></a>
### FR-27 — Biểu mẫu báo cáo bài công thức theo 6 nhóm lý do

- **Mã yêu cầu:** FR-27
- **Module:** M03 (Community Recipes & Social)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Biểu mẫu báo cáo bài công thức cho phép chọn đúng một trong sáu nhóm lý do chuẩn hóa tại SRS 3.11 và nhập mô tả bổ sung; mô tả bổ sung là bắt buộc khi chọn nhóm lý do "Khác" (10–500 ký tự) và là tùy chọn đối với các nhóm lý do còn lại.

#### 1. Mục đích
Chuẩn hóa dữ liệu đầu vào của các báo cáo vi phạm nội dung; giúp người báo cáo dễ dàng chỉ rõ bản chất vi phạm; đồng thời hỗ trợ Quản trị viên nhanh chóng phân loại, đánh giá mức độ khẩn cấp (như nguy cơ an toàn thực phẩm) và xử lý chính xác theo quy chuẩn nền tảng.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member` (Người báo cáo): Thao tác trên biểu mẫu báo cáo.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-27.1`: Chọn lý do và gửi biểu mẫu báo cáo bài công thức (Submit recipe report form).
- **User Stories:**
  - `US-27.1`: Là một người báo cáo, tôi muốn chọn đúng lý do "Không phải món chay" từ danh sách có sẵn để Ban quản trị hiểu ngay vấn đề mà tôi không cần phải gõ giải thích dài dòng nếu không cần thiết.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Member đã đăng nhập và đã kích hoạt biểu mẫu báo cáo từ bài công thức (FR-26).
- **Trigger:** Biểu mẫu báo cáo mở ra trên màn hình.

#### 5. Cấu trúc 6 nhóm lý do báo cáo chuẩn hóa (SRS 3.11)

| Nhóm lý do | Mã phân loại | Tính chất mô tả bổ sung | Mô tả chi tiết phạm vi vi phạm |
|---|---|---|---|
| **1. Không phải món chay** | `NON_VEGAN` | Tùy chọn (Optional) | Công thức chứa thịt, cá, hải sản, mỡ động vật hoặc các thành phần không thuần chay. |
| **2. Nguy cơ an toàn thực phẩm** | `FOOD_SAFETY_HAZARD` | Tùy chọn (Optional) | Hướng dẫn kết hợp nguyên liệu gây ngộ độc, liều lượng nguy hiểm hoặc phương pháp nấu có hại cho sức khỏe. |
| **3. Nội dung phản cảm / Bạo lực** | `INAPPROPRIATE_CONTENT` | Tùy chọn (Optional) | Hình ảnh ghê rợn, phản cảm, ngôn từ bạo lực hoặc vi phạm thuần phong mỹ tục. |
| **4. Vi phạm bản quyền / Sao chép** | `COPYRIGHT_VIOLATION` | Tùy chọn (Optional) | Sao chép nguyên văn công thức, ảnh hoặc nội dung thuộc quyền sở hữu của người khác mà không xin phép. |
| **5. Spam / Quảng cáo thương mại** | `SPAM_ADVERTISING` | Tùy chọn (Optional) | Chèn liên kết bán hàng, quảng cáo thương mại không liên quan, nội dung rác hoặc lừa đảo. |
| **6. Khác** | `OTHER` | **Bắt buộc (Mandatory)** | Các vấn đề khác; **bắt buộc nhập từ 10 đến 500 ký tự** để giải thích rõ lý do báo cáo. |

#### 6. Luồng sự kiện (Flow of Events)

##### A. Luồng Nhập và gửi biểu mẫu báo cáo (UC-27.1)
1. **Main Flow:**
   - Bước 1: Biểu mẫu hiển thị 6 nút chọn (radio button) tương ứng với 6 nhóm lý do trên.
   - Bước 2: Member chọn 1 trong 6 lý do.
   - Bước 3: Nếu Member chọn nhóm từ 1 đến 5: ô nhập "Mô tả bổ sung (tùy chọn)" cho phép để trống hoặc nhập văn bản tối đa 500 ký tự.
   - Bước 4: Nếu Member chọn nhóm 6 ("Khác"): ô nhập mô tả chuyển thành bắt buộc, hiển thị nhãn "(Bắt buộc)" kèm bộ đếm ký tự.
   - Bước 5: Member nhấn nút "Gửi báo cáo".
   - Bước 6: Hệ thống kiểm tra hợp lệ: lý do hợp lệ và mô tả đáp ứng quy định.
   - Bước 7: Dữ liệu được gửi tới máy chủ để hoàn tất tạo báo cáo (FR-26).
2. **Error Flows:**
   - *Chưa chọn lý do:* Member nhấn gửi mà chưa tích chọn lý do nào -> Hệ thống báo lỗi yêu cầu chọn 1 lý do.
   - *Chọn "Khác" nhưng bỏ trống mô tả:* Member chọn lý do "Khác" nhưng để trống hoặc nhập dưới 10 ký tự -> Hệ thống báo lỗi: "Vui lòng nhập mô tả chi tiết từ 10 đến 500 ký tự khi chọn lý do Khác."

#### 7. Hậu điều kiện (Postconditions)
- Báo cáo với lý do và mô tả chuẩn hóa được gửi thành công tới hệ thống tiếp nhận.

#### 8. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Làm sạch dữ liệu mô tả chống các tấn công chèn mã độc XSS (NFR-10).
- Nhóm lý do gửi lên máy chủ phải khớp chính xác với 1 trong 6 mã phân loại chuẩn hóa.

#### 9. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống lỗ hổng OWASP Top 10 (XSS).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt.

#### 10. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-27.1 — Hiển thị đầy đủ 6 nhóm lý do chuẩn hóa:**
  - **Given:** Member mở biểu mẫu báo cáo bài viết.
  - **When:** Biểu mẫu tải hoàn tất.
  - **Then:** Giao diện hiển thị đúng và đủ 6 nhóm lý do theo quy định tại SRS 3.11.

- **AC-27.2 — Gửi thành công báo cáo với mô tả để trống cho 5 nhóm lý do đầu:**
  - **Given:** Member chọn nhóm lý do "Không phải món chay".
  - **When:** Member để trống ô mô tả bổ sung và nhấn "Gửi báo cáo".
  - **Then:** Hệ thống xác nhận hợp lệ và gửi báo cáo thành công.

- **AC-27.3 — Bắt buộc nhập mô tả chi tiết từ 10 đến 500 ký tự khi chọn lý do Khác:**
  - **Given:** Member chọn nhóm lý do "Khác".
  - **When:** Member nhập mô tả "Bài viết này hướng dẫn dùng mật ong mà người ăn thuần chay không dùng được" (75 ký tự) và nhấn gửi.
  - **Then:** Hệ thống ghi nhận báo cáo hợp lệ và gửi thành công.

- **AC-27.4 — Từ chối báo cáo lý do Khác khi mô tả dưới 10 ký tự:**
  - **Given:** Member chọn nhóm lý do "Khác".
  - **When:** Member nhập mô tả "Sai món" (7 ký tự) hoặc để trống và nhấn gửi.
  - **Then:** Hệ thống từ chối và hiển thị thông báo lỗi yêu cầu mô tả tối thiểu 10 ký tự.

---

<a id="fr-28"></a>
### FR-28 — Administrator xử lý báo cáo qua các trạng thái

- **Mã yêu cầu:** FR-28
- **Module:** M02 (Identity & Access), M03 (Community Recipes & Social), M09 (Administration & Moderation)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Administrator quyết định thủ công khi xử lý báo cáo qua các trạng thái: Chờ xử lý (`OPEN`), Đang xem xét (`IN_REVIEW`), Đã giải quyết (`RESOLVED`); bắt buộc ghi kết luận và lý do xử lý; duy trì lịch sử kiểm toán (audit history); có thể cảnh báo tác giả, ẩn/xóa Recipe Post hoặc khóa/mở khóa tài khoản (BR-26); MVP không có ma trận chế tài số tự động.

#### 1. Mục đích
Thiết lập quy trình nghiệp vụ rõ ràng, chuẩn hóa và có trách nhiệm giải trình cho Quản trị viên trong việc xử lý các phản ánh vi phạm; đảm bảo mọi phán quyết chế tài đều được lưu vết minh bạch, đồng thời tránh xung đột khi nhiều quản trị viên cùng thao tác xử lý nội dung.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Administrator`: Người thực hiện tiếp nhận, xem xét và ra quyết định giải quyết báo cáo.

#### 3. Vòng đời trạng thái báo cáo vi phạm
Hệ thống quản lý trạng thái báo cáo theo luồng tuần tự:
```
[OPEN] (Chờ xử lý) ──> [IN_REVIEW] (Đang xem xét) ──> [RESOLVED] (Đã giải quyết)
```
- `OPEN` (Chờ xử lý): Báo cáo mới được gửi từ người dùng, chưa có Admin nào tiếp nhận.
- `IN_REVIEW` (Đang xem xét): Một Admin đã tiếp nhận và đang thẩm tra bài viết; giúp các Admin khác không xử lý trùng.
- `RESOLVED` (Đã giải quyết): Admin đã ra quyết định kết luận (bác bỏ hoặc áp dụng chế tài cụ thể).

#### 4. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-28.1`: Xem danh sách và tiếp nhận xử lý báo cáo (Review report queue).
  - `UC-28.2`: Ra quyết định xử lý báo cáo và áp dụng chế tài (Resolve report and apply sanctions).
- **User Stories:**
  - `US-28.1`: Là một Administrator, tôi muốn chuyển báo cáo sang trạng thái "Đang xem xét" khi bắt đầu thẩm tra để đồng nghiệp biết và không thao tác trùng.
  - `US-28.2`: Là một Administrator, khi quyết định ẩn bài viết vi phạm, tôi muốn hệ thống yêu cầu tôi nhập lý do kết luận rõ ràng để làm căn cứ thông báo cho tác giả và phục vụ kiểm tra lại sau này.

#### 5. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Người dùng đã đăng nhập với quyền Administrator.
- **Trigger:** Administrator mở danh sách báo cáo vi phạm trong trang Quản trị.

#### 6. Luồng sự kiện (Flow of Events)

##### A. Luồng Tiếp nhận xem xét báo cáo (UC-28.1)
1. **Main Flow:**
   - Bước 1: Administrator vào danh sách báo cáo ở trạng thái `OPEN`.
   - Bước 2: Administrator chọn một báo cáo cần xử lý và nhấn "Tiếp nhận xem xét".
   - Bước 3: Hệ thống chuyển trạng thái báo cáo sang `IN_REVIEW`, ghi nhận định danh Admin đang phụ trách xem xét.
   - Bước 4: Administrator xem nội dung bài viết, nguyên liệu, hình ảnh và danh sách các lý do báo cáo được gửi tới.

##### B. Luồng Ra quyết định xử lý và áp dụng chế tài (UC-28.2)
1. **Main Flow (Nội dung không vi phạm):**
   - Bước 1: Administrator thẩm định bài viết và xác định bài viết tuân thủ đúng quy chuẩn (ví dụ: món ăn thuần chay chuẩn, người báo cáo hiểu nhầm nguyên liệu).
   - Bước 2: Administrator chọn kết luận "Không vi phạm quy tắc", nhập ghi chú giải thích và nhấn "Hoàn tất xử lý".
   - Bước 3: Hệ thống chuyển trạng thái báo cáo sang `RESOLVED`, giữ nguyên bài viết hiển thị công khai bình thường.
2. **Main Flow (Nội dung có vi phạm và áp dụng chế tài):**
   - Bước 1: Administrator xác định bài viết vi phạm quy định (ví dụ: chứa mỡ động vật hoặc hình ảnh phản cảm).
   - Bước 2: Administrator chọn chế tài xử lý thủ công (BR-26):
     - *Cảnh cáo tác giả:* Gửi thông báo cảnh cáo yêu cầu chỉnh sửa bài viết.
     - *Ẩn Recipe Post:* Chuyển bài viết sang trạng thái ẩn vi phạm, không cho hiển thị công khai.
     - *Xóa Recipe Post:* Đánh dấu xóa bài viết.
     - *Khóa tài khoản tác giả:* Chuyển trạng thái tài khoản sang `LOCKED` (áp dụng khi vi phạm nghiêm trọng hoặc tái phạm nhiều lần).
   - Bước 3: Administrator **bắt buộc nhập lý do kết luận xử lý** (tối thiểu 10 ký tự).
   - Bước 4: Hệ thống thực thi chế tài đã chọn, lưu vết kiểm toán (Admin thực hiện, thời gian, quyết định, lý do), và chuyển trạng thái báo cáo sang `RESOLVED`.
   - Bước 5: Hệ thống gửi thông báo giải thích kết quả tới tác giả bài viết (FR-29).
3. **Error Flow:**
   - Administrator nhấn hoàn tất xử lý vi phạm mà không nhập lý do kết luận -> Hệ thống từ chối lưu và yêu cầu bắt buộc nhập lý do.

#### 7. Hậu điều kiện (Postconditions)
- Trạng thái báo cáo chuyển thành `RESOLVED`.
- Quyết định chế tài được áp dụng ngay lập tức đối với bài viết hoặc tài khoản liên quan.
- Toàn bộ thông tin xử lý được ghi nhận vào nhật ký kiểm toán hệ thống.

#### 8. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Chỉ tài khoản có vai trò Administrator mới có quyền truy cập và thay đổi trạng thái xử lý báo cáo (NFR-09).
- Bắt buộc lưu vết kiểm toán (audit trail) không thể xóa sửa đối với mọi quyết định xử lý của Admin (NFR-08).
- Không áp dụng chế tài tự động bằng thuật toán trong phiên bản MVP; mọi quyết định đều do Quản trị viên con người đánh giá thủ công (BR-26).

#### 9. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
  - [BR-26](BUSINESS-RULES.md#br-26): Các hình thức chế tài khi xử lý vi phạm.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và toàn vẹn lịch sử kiểm toán.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### 10. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-28.1 — Tiếp nhận và chuyển trạng thái báo cáo sang IN_REVIEW:**
  - **Given:** Administrator đang ở danh sách báo cáo có mục ở trạng thái `OPEN`.
  - **When:** Administrator nhấn "Tiếp nhận xem xét".
  - **Then:** Báo cáo chuyển sang trạng thái `IN_REVIEW` kèm tên Admin đang xử lý.

- **AC-28.2 — Giải quyết báo cáo với kết luận không vi phạm:**
  - **Given:** Administrator đang xem xét báo cáo ở trạng thái `IN_REVIEW`.
  - **When:** Administrator chọn kết luận "Không vi phạm", nhập lý do giải thích và hoàn tất.
  - **Then:** Báo cáo chuyển sang `RESOLVED`, bài viết tiếp tục hiển thị bình thường.

- **AC-28.3 — Áp dụng chế tài ẩn bài viết và bắt buộc nhập lý do:**
  - **Given:** Administrator đang xem xét báo cáo bài viết có vi phạm.
  - **When:** Administrator chọn chế tài "Ẩn bài viết", nhập lý do vi phạm và hoàn tất.
  - **Then:** Báo cáo chuyển sang `RESOLVED`, bài viết lập tức bị ẩn khỏi giao diện công khai và tìm kiếm (BR-26).

- **AC-28.4 — Áp dụng chế tài khóa tài khoản tác giả vi phạm sang trạng thái LOCKED:**
  - **Given:** Tác giả cố tình đăng nội dung độc hại nhiều lần.
  - **When:** Administrator chọn chế tài "Khóa tài khoản" và nhập lý do kết luận.
  - **Then:** Hệ thống chuyển trạng thái tài khoản tác giả sang `LOCKED`, thu hồi các phiên đăng nhập hiện có của tài khoản đó (BR-26).

- **AC-28.5 — Từ chối hoàn tất giải quyết nếu để trống lý do kết luận:**
  - **Given:** Administrator chọn chế tài xử lý vi phạm.
  - **When:** Administrator để trống ô lý do kết luận và nhấn "Hoàn tất xử lý".
  - **Then:** Hệ thống từ chối lưu và hiển thị thông báo yêu cầu nhập lý do kết luận.

---

<a id="fr-29"></a>
### FR-29 — Phân quyền hiển thị thông tin báo cáo

- **Mã yêu cầu:** FR-29
- **Module:** M02 (Identity & Access), M03 (Community Recipes & Social), M09 (Administration & Moderation)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống phân quyền hiển thị thông tin báo cáo theo mục 3.13: Administrator thấy đầy đủ thông tin tài khoản người gửi và người bị báo cáo; tác giả thấy thông tin xử lý nhưng không thấy danh tính người báo cáo; người gửi xem trạng thái và kết quả báo cáo của mình; báo cáo vi phạm không được hiển thị công khai cho bên thứ ba.

#### 1. Mục đích
Bảo vệ an toàn và quyền riêng tư cho người dùng tham gia đóng góp phản ánh vi phạm; ngăn chặn triệt để các hành vi đe dọa, quấy rối hoặc trả đũa cá nhân giữa các thành viên cộng đồng; đồng thời đảm bảo tính minh bạch đối với kết quả xử lý của Ban quản trị.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Administrator`: Xem toàn bộ thông tin phục vụ quản trị.
  - `Member` (Người gửi báo cáo): Xem trạng thái báo cáo của mình.
  - `Member` (Tác giả bài viết bị xử lý): Xem thông báo kết quả chế tài.

#### 3. Bảng Ma trận phân quyền hiển thị thông tin báo cáo (SRS 3.13)

| Trường thông tin báo cáo | Administrator | Người gửi báo cáo | Tác giả bài viết bị báo cáo | Người dùng khác / Guest |
|---|---|---|---|---|
| **Danh tính người gửi báo cáo** | Có (Hiển thị đầy đủ) | Có (Xem của chính mình) | **KHÔNG (Ẩn tuyệt đối)** | **KHÔNG** |
| **Nội dung bài viết bị báo cáo** | Có | Có | Có | Có (nếu bài chưa bị ẩn) |
| **Lý do và mô tả báo cáo đã gửi**| Có | Có | Có (nội dung kết luận tóm tắt) | **KHÔNG** |
| **Trạng thái xử lý (`OPEN`,...)**| Có | Có | Có (khi có quyết định) | **KHÔNG** |
| **Ghi chú kết luận của Admin** | Có | Có (kết luận chung) | Có (lý do chế tài) | **KHÔNG** |

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Bản ghi báo cáo vi phạm đang tồn tại trong hệ thống. Người dùng đã đăng nhập với vai trò tương ứng.
- **Trigger:** Người dùng truy cập mục "Báo cáo của tôi", tác giả mở thông báo hệ thống, hoặc Admin mở danh sách quản trị.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Người gửi xem trạng thái báo cáo của mình (UC-29.1)
1. **Main Flow:**
   - Bước 1: Member vào trang "Lịch sử báo cáo của tôi".
   - Bước 2: Hệ thống truy xuất các báo cáo do chính `authenticatedUser.id` tạo ra.
   - Bước 3: Giao diện hiển thị danh sách gồm: tên bài viết, ngày gửi, nhóm lý do, trạng thái hiện tại (`Chờ xử lý`, `Đang xem xét`, `Đã giải quyết`) và ghi chú kết luận của Admin (nếu đã xong).

##### B. Luồng Tác giả nhận thông báo kết quả xử lý (UC-29.2)
1. **Main Flow:**
   - Bước 1: Khi bài viết bị Admin áp dụng chế tài ẩn hoặc cảnh cáo, hệ thống tự động gửi thông báo đến tác giả bài viết.
   - Bước 2: Tác giả mở thông báo, thấy nội dung: bài viết bị xử lý, biện pháp chế tài, điều khoản vi phạm và ghi chú kết luận của Admin.
   - Bước 3: **Ranh giới bảo mật:** Hệ thống TUYỆT ĐỐI KHÔNG hiển thị tên, ảnh đại diện, ID hay bất kỳ thông tin nào về người đã gửi báo cáo.

##### C. Luồng An ninh & Chặn truy cập trái phép
1. **Main Flow:**
   - Người dùng A cố tình truy cập vào thông tin báo cáo do người dùng B gửi:
   - Máy chủ kiểm tra quyền sở hữu (`report.reporterId == authenticatedUser.id` hoặc `Role == ADMIN`).
   - Phát hiện không khớp -> Máy chủ từ chối và phản hồi HTTP 403 Forbidden, không trả về bất kỳ dữ liệu báo cáo nào.

#### 6. Hậu điều kiện (Postconditions)
- Thông tin báo cáo được hiển thị đúng người, đúng quyền, bảo vệ tối đa quyền riêng tư của các bên liên quan.

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Việc ẩn danh tính người báo cáo đối với tác giả phải được thực thi ở tầng máy chủ (Data Transfer Object không chứa trường `reporterInfo` khi trả về cho tác giả) (NFR-08, NFR-09).
- Báo cáo vi phạm là dữ liệu riêng tư nhạy cảm, tuyệt đối không tạo endpoint public hay để lộ ra bên ngoài (NFR-08).

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân & truyền tải an toàn HTTPS.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-29.1 — Người gửi báo cáo xem được tiến độ báo cáo của chính mình:**
  - **Given:** Member A đã gửi báo cáo về bài viết X.
  - **When:** Member A vào trang Lịch sử báo cáo của tôi.
  - **Then:** Hệ thống hiển thị đúng bài viết X, lý do báo cáo và trạng thái hiện tại của báo cáo đó.

- **AC-29.2 — Tác giả bài viết không thấy danh tính người gửi báo cáo:**
  - **Given:** Bài viết của tác giả B bị ẩn do vi phạm sau báo cáo của Member A.
  - **When:** Tác giả B mở thông báo xử lý vi phạm.
  - **Then:** Giao diện hiển thị lý do vi phạm và kết luận của Admin, tuyệt đối không có tên hay thông tin của Member A.

- **AC-29.3 — Administrator xem được đầy đủ danh tính người báo cáo và tác giả:**
  - **Given:** Administrator đang xem chi tiết một báo cáo vi phạm.
  - **When:** Trang báo cáo tải hoàn tất.
  - **Then:** Administrator thấy rõ ràng thông tin tài khoản người báo cáo và tài khoản tác giả bị báo cáo.

- **AC-29.4 — Chặn người dùng bên ngoài truy cập thông tin báo cáo:**
  - **Given:** Người dùng C không phải người báo cáo, không phải tác giả và không phải Admin.
  - **When:** Người dùng C cố tình truy cập vào liên kết xem chi tiết báo cáo vi phạm.
  - **Then:** Máy chủ từ chối yêu cầu và phản hồi mã lỗi HTTP 403 Forbidden.

---

<a id="fr-30"></a>
### FR-30 — Bổ sung thông tin và ngăn trùng lặp báo cáo mở

- **Mã yêu cầu:** FR-30
- **Module:** M03 (Community Recipes & Social), M09 (Administration & Moderation)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Người gửi được bổ sung mô tả cho báo cáo đang mở; hệ thống ngăn cùng tài khoản tạo báo cáo mở trùng trên một bài công thức; Admin xem báo cáo nhóm theo bài; báo cáo lại sau khi giải quyết cần mô tả vấn đề mới.

#### 1. Mục đích
Tối ưu hóa quy trình tiếp nhận thông tin phản ánh; chống hành vi spam gửi nhiều báo cáo trùng lặp gây quá tải hệ thống quản trị; hỗ trợ Quản trị viên theo dõi bức tranh toàn cảnh về toàn bộ các phản ánh của cộng đồng trên từng bài viết cụ thể.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member` (Người gửi báo cáo): Bổ sung thông tin cho báo cáo đang mở.
  - `Administrator`: Xem danh sách báo cáo gom nhóm theo bài viết.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-30.1`: Bổ sung thông tin cho báo cáo đang mở (Append information to open report).
  - `UC-30.2`: Xem danh sách báo cáo được nhóm theo bài viết (View reports grouped by recipe).
- **User Stories:**
  - `US-30.1`: Là một người đã gửi báo cáo, khi tôi phát hiện thêm bằng chứng vi phạm, tôi muốn bổ sung thêm vào báo cáo cũ đang chờ duyệt mà không cần tạo một báo cáo mới.
  - `US-30.2`: Là một Administrator, tôi muốn các báo cáo về cùng một bài viết được gom lại một chỗ để tôi nắm được toàn bộ phản ánh trước khi ra quyết định.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Member có một báo cáo đang ở trạng thái `OPEN` hoặc `IN_REVIEW`.
- **Trigger:** Member nhấn "Bổ sung thông tin" trong trang chi tiết báo cáo của mình, hoặc Admin mở chế độ xem nhóm báo cáo.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Bổ sung thông tin cho báo cáo đang mở (UC-30.1)
1. **Main Flow:**
   - Bước 1: Member truy cập báo cáo đang mở của mình trong "Báo cáo của tôi".
   - Bước 2: Member nhấn nút "Bổ sung thông tin".
   - Bước 3: Member nhập thêm văn bản mô tả (tối đa 500 ký tự) và nhấn "Lưu bổ sung".
   - Bước 4: Hệ thống nối thêm thông tin này vào nhật ký mô tả của báo cáo kèm mốc thời gian bổ sung.
   - Bước 5: Hệ thống thông báo cập nhật thông tin thành công.

##### B. Luồng Ngăn chặn tạo báo cáo mở trùng lặp
1. **Main Flow:**
   - Bước 1: Member nhấn nút "Báo cáo bài viết" trên bài công thức A.
   - Bước 2: Hệ thống kiểm tra cơ sở dữ liệu và phát hiện Member này đã có một báo cáo về bài viết A đang ở trạng thái `OPEN` hoặc `IN_REVIEW`.
   - Bước 3: Hệ thống không mở biểu mẫu tạo báo cáo mới, mà hiển thị thông báo: "Bạn đã có báo cáo đang được xem xét cho bài viết này. Bạn có muốn bổ sung thêm thông tin vào báo cáo hiện có không?" kèm nút bấm dẫn tới chức năng bổ sung thông tin.

##### C. Luồng Báo cáo lại sau khi giải quyết
1. **Main Flow:**
   - Bước 1: Nếu báo cáo trước đó của Member về bài viết A đã được Admin giải quyết (`RESOLVED`).
   - Bước 2: Sau đó, tác giả chỉnh sửa bài viết và phát sinh vi phạm mới, hoặc Member phát hiện vấn đề mới chưa được giải quyết.
   - Bước 3: Member có quyền gửi một báo cáo mới, nhưng bắt buộc phải nhập mô tả chi tiết giải thích vấn đề mới phát sinh (tối thiểu 10 ký tự).

##### D. Luồng Admin xem báo cáo gom nhóm theo bài (UC-30.2)
1. **Main Flow:**
   - Bước 1: Administrator mở mục "Báo cáo vi phạm" trên trang Quản trị.
   - Bước 2: Hệ thống hiển thị chế độ xem gom nhóm theo bài viết: mỗi dòng đại diện cho một bài viết bị báo cáo kèm số lượng phản ánh tích lũy (ví dụ: "Bài viết Canh Chua Đậu Phụ — 4 báo cáo đang mở").
   - Bước 3: Admin nhấp vào bài viết để mở danh sách chi tiết toàn bộ các báo cáo từ các người dùng khác nhau trên bài viết đó.

#### 6. Hậu điều kiện (Postconditions)
- Thông tin bổ sung được lưu thành công vào bản ghi báo cáo hiện có.
- Không phát sinh bản ghi trùng lặp trong hàng đợi quản trị.

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Kiểm tra tính duy nhất của báo cáo đang mở theo cặp `(reporterId, recipeId, status IN ['OPEN', 'IN_REVIEW'])` ở tầng cơ sở dữ liệu / nghiệp vụ máy chủ (NFR-08).
- Chỉ người tạo báo cáo mới có quyền bổ sung thông tin cho báo cáo đó (NFR-09).

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
  - [BR-26](BUSINESS-RULES.md#br-26): Các hình thức chế tài khi xử lý vi phạm.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và toàn vẹn giao dịch.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-30.1 — Bổ sung mô tả vào báo cáo đang mở thành công:**
  - **Given:** Member có báo cáo đang ở trạng thái `OPEN`.
  - **When:** Member nhập thêm nội dung bổ sung và nhấn lưu.
  - **Then:** Hệ thống lưu thêm nội dung mới vào báo cáo kèm mốc thời gian và phản hồi thành công.

- **AC-30.2 — Chặn tạo báo cáo mới trùng lặp khi báo cáo trước đang mở:**
  - **Given:** Member đã có báo cáo đang ở trạng thái `OPEN` trên bài viết A.
  - **When:** Member cố tình gửi thêm báo cáo mới cho bài viết A.
  - **Then:** Hệ thống từ chối tạo mới và điều hướng Member đến báo cáo hiện có để bổ sung thông tin.

- **AC-30.3 — Cho phép gửi báo cáo mới sau khi báo cáo trước đã giải quyết:**
  - **Given:** Báo cáo trước của Member trên bài viết A đã chuyển sang trạng thái `RESOLVED`.
  - **When:** Member gửi một báo cáo mới kèm mô tả vấn đề phát sinh mới.
  - **Then:** Hệ thống tiếp nhận và tạo bản ghi báo cáo mới thành công.

- **AC-30.4 — Administrator xem danh sách báo cáo được gom nhóm theo bài viết:**
  - **Given:** Bài viết A nhận được 3 báo cáo từ 3 thành viên khác nhau.
  - **When:** Administrator mở bảng quản lý báo cáo.
  - **Then:** Hệ thống hiển thị bài viết A thành 1 nhóm duy nhất với chỉ số "3 báo cáo", cho phép xem chi tiết từng phản ánh.

---

<a id="fr-31"></a>
### FR-31 — Điều kiện thông tin tối thiểu trước khi dùng AI cá nhân hóa

- **Mã yêu cầu:** FR-31
- **Module:** M02, M05, M06
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống cung cấp bảng câu hỏi Onboarding tùy chọn cho Member mới; cho phép bỏ qua mà không khóa các tính năng thông thường; quản lý hồ sơ sở thích ăn uống; bắt buộc hoàn thành ba nhóm thông tin tối thiểu (loại ăn chay, nguyên liệu cần tránh do dị ứng/kiêng, món hoặc nguyên liệu không thích) trước khi gọi AI gợi ý món hoặc tạo thực đơn cá nhân hóa; chặn gọi Gemini và không trừ hạn mức AI khi thiếu thông tin.

#### 1. Mục đích
Thu thập và chuẩn hóa dữ liệu sở thích, thói quen và các ràng buộc ăn kiêng của thành viên nhằm phục vụ các tính năng gợi ý món ăn và lập thực đơn cá nhân hóa của AI; bảo đảm quyền tự do trải nghiệm của người dùng qua cơ chế Onboarding tùy chọn (cho phép bỏ qua); đồng thời thiết lập cổng kiểm soát dữ liệu đầu vào nghiêm ngặt để đảm bảo an toàn dị ứng, ngăn ngừa AI gợi ý sai lệch và tránh lãng phí hạn mức gọi AI của người dùng.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member`: Người dùng đã đăng nhập tham gia Onboarding, xem và cập nhật hồ sơ sở thích ăn uống cá nhân.
- **Secondary Actor / External System:**
  - `Dịch vụ AI (Google Gemini)`: Tiếp nhận dữ liệu hồ sơ sở thích ăn uống hợp lệ làm đầu vào (constraints/context) để thực hiện gợi ý món ăn và lập thực đơn cá nhân hóa.

#### 3. Cấu trúc hồ sơ sở thích ăn uống (Dietary Preferences Profile)

| Nhóm thông tin | Tính chất ràng buộc | Quy tắc giá trị & Validation |
|---|---|---|
| **1. Loại ăn chay (`dietaryType`)** | Ràng buộc cứng, bắt buộc phải chọn | Bắt buộc chọn đúng **1 trong 4 loại** đã được chuẩn hóa (SRS 3.20):<br>- `Vegan` (Ăn chay thuần — không thịt, cá, trứng, sữa, mật ong)<br>- `Lacto Vegetarian` (Ăn chay có dùng sữa và chế phẩm từ sữa)<br>- `Ovo Vegetarian` (Ăn chay có dùng trứng)<br>- `Lacto-Ovo Vegetarian` (Ăn chay có dùng cả trứng và sữa) |
| **2. Nguyên liệu cần tránh (`allergiesAndRestrictions`)** | Ràng buộc cứng, bắt buộc phải xác nhận | - Danh sách nguyên liệu cần tránh (chọn từ danh mục nguyên liệu chuẩn hoặc nhập văn bản tự do, ví dụ: đậu phộng, gluten, nấm, đậu nành,...).<br>- **HOẶC** người dùng phải chủ động tích chọn xác nhận: `"Không có"`.<br>- Không được để trạng thái chưa xác nhận (trống/unspecified). Bỏ trống KHÔNG được tự hiểu là không có dị ứng (BR-31). |
| **3. Món hoặc nguyên liệu không thích (`dislikedIngredients`)** | Ràng buộc cứng, bắt buộc phải xác nhận | - Danh sách món hoặc nguyên liệu không thích (chọn từ danh mục chuẩn hoặc nhập tự do, ví dụ: mướp đắng, rau mùi,...).<br>- **HOẶC** người dùng phải chủ động tích chọn xác nhận: `"Không có"`.<br>- Không được để trạng thái chưa xác nhận (trống/unspecified). Bỏ trống KHÔNG được tự hiểu là không có món không thích (BR-31). |
| **4. Khẩu vị ẩm thực (`cuisinePreference`)** | Sở thích mềm, tùy chọn | Tùy chọn khẩu vị ưa thích (ví dụ: món chay truyền thống Việt Nam, món chay phong cách Thái, Âu,...). Cho phép để trống. |
| **5. Thời gian nấu tối đa (`maxCookingTimeMinutes`)** | Sở thích mềm, tùy chọn | Số nguyên dương biểu diễn số phút mong muốn (ví dụ: $\le 30$ phút, $\le 60$ phút). Cho phép để trống. |
| **6. Độ khó nấu ăn (`cookingDifficulty`)** | Sở thích mềm, tùy chọn | Chọn một trong các mức: `Easy` (Dễ), `Medium` (Trung bình), `Advanced` (Nâng cao). Cho phép để trống. |

#### 4. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-31.1`: Khảo sát sở thích ban đầu qua Onboarding Questionnaire (Initial Onboarding Questionnaire).
  - `UC-31.2`: Bỏ qua Onboarding (Skip Onboarding).
  - `UC-31.3`: Xem và cập nhật hồ sơ sở thích ăn uống cá nhân (View and update dietary preferences).
  *(Ghi chú: UC-31.4 trước đây về kiểm tra điều kiện thông tin tối thiểu không còn là Use Case độc lập vì đây là cổng kiểm soát logic/bảo vệ an toàn của hệ thống khi thực hiện chức năng AI cá nhân hóa chứ không phải mục tiêu của Actor; hành vi này được chuẩn hóa thành Cổng kiểm soát nghiệp vụ, Luồng rẽ nhánh an toàn, và Tiêu chí nghiệm thu).*
- **User Stories:**
  - `US-31.1`: Là một thành viên mới, tôi muốn tham gia bảng câu hỏi Onboarding sau khi đăng ký để khai báo chế độ ăn chay, dị ứng và món không thích, giúp AI hiểu nhu cầu của tôi.
  - `US-31.2`: Là một thành viên mới, tôi muốn có nút "Bỏ qua" để vào ngay ứng dụng khám phá công thức mà không bị ép buộc điền thông tin ngay lập tức.
  - `US-31.3`: Là một thành viên chưa hoàn thành Onboarding, tôi muốn vẫn sử dụng được toàn bộ các tính năng thông thường (tìm kiếm, xem bài, lưu công thức, lập lịch ăn thủ công, đăng bài, bình luận, hỏi đáp AI cơ bản) mà không bị khóa tài khoản.
  - `US-31.4`: Là một thành viên, tôi muốn vào phần Cài đặt tài khoản để cập nhật lại loại ăn chay hoặc danh sách dị ứng bất cứ khi nào có thay đổi.
  - `US-31.5`: Là một thành viên, khi tôi yêu cầu AI gợi ý món hoặc tạo thực đơn cá nhân hóa mà chưa hoàn tất 3 thông tin tối thiểu, tôi muốn hệ thống giải thích rõ lý do, hướng dẫn tôi điền thông tin và tuyệt đối không trừ lượt AI của tôi.

#### 5. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:**
  - Người dùng đã đăng nhập với vai trò Member và tài khoản ở trạng thái `ACTIVE`.
- **Trigger:**
  - Ngay sau khi xác minh email / đăng nhập lần đầu (hiển thị Onboarding).
  - Member chủ động truy cập mục "Sở thích ăn uống" trong màn hình Cài đặt cá nhân.
  - Member nhấn vào tính năng AI cá nhân hóa: yêu cầu gợi ý món ăn theo sở thích hoặc tạo thực đơn tuần theo sở thích.

#### 6. Luồng sự kiện (Flow of Events)

##### A. Luồng Khảo sát Onboarding ban đầu (UC-31.1, UC-31.2)
1. **Main Flow (Hoàn thành Onboarding):**
   - Bước 1: Sau khi hoàn tất xác minh email hoặc đăng nhập Google lần đầu, hệ thống chuyển hướng Member tới màn hình Onboarding Questionnaire.
   - Bước 2: Màn hình hiển thị lần lượt các câu hỏi:
     - Câu 1: Chọn 1 trong 4 loại ăn chay (bắt buộc).
     - Câu 2: Khai báo nguyên liệu cần tránh/dị ứng (chọn từ gợi ý hoặc nhập tự do, hoặc tích chọn "Tôi không có dị ứng/kiêng cử").
     - Câu 3: Khai báo món/nguyên liệu không thích (chọn gợi ý, nhập tự do, hoặc tích chọn "Tôi không có món không thích").
     - Câu 4 (Tùy chọn): Khẩu vị ẩm thực, thời gian nấu tối đa, độ khó mong muốn.
   - Bước 3: Member điền đầy đủ và nhấn "Hoàn tất".
   - Bước 4: Ứng dụng gửi dữ liệu hồ sơ lên máy chủ hệ thống.
   - Bước 5: Hệ thống kiểm tra tính hợp lệ của 3 nhóm thông tin tối thiểu, lưu trữ hồ sơ, ghi nhận trạng thái tài khoản đã hoàn thành Onboarding.
   - Bước 6: Hệ thống phản hồi thành công; giao diện hiển thị thông báo hoàn tất và điều hướng Member vào trang Khám phá.
2. **Alternative Flow — Bỏ qua Onboarding (UC-31.2):**
   - Bước 1: Tại màn hình Onboarding, Member nhấn nút "Bỏ qua" (Skip).
   - Bước 2: Hệ thống ghi nhận trạng thái tài khoản chưa hoàn tất Onboarding và chuyển thẳng Member vào trang Khám phá.
   - Bước 3: **Ranh giới chức năng khi bỏ qua Onboarding (BR-30):**
     - Member VẪN ĐƯỢC: Duyệt, tìm kiếm và lọc các bài công thức công khai; Đọc và gửi bình luận, Like bài viết; Tạo, chỉnh sửa và công khai Recipe Post của chính mình; Lưu bài công thức vào `Saved Recipes`; Tự thêm bài công thức vào `Meal Planner` 3 bữa theo cách thủ công; Sử dụng Chatbot AI hỏi đáp kiến thức chay chung trong hạn mức gói.
     - Member CHỈ BỊ CHẶN khi gọi các chức năng AI cá nhân hóa (gợi ý món ăn riêng, lập thực đơn tuần tự động).

##### B. Luồng Xem và Cập nhật hồ sơ sở thích ăn uống (UC-31.3)
1. **Main Flow:**
   - Bước 1: Member truy cập mục "Sở thích ăn uống" trong Cài đặt tài khoản.
   - Bước 2: Hệ thống truy xuất và hiển thị thông tin sở thích hiện tại của Member.
   - Bước 3: Giao diện hiển thị các trường dữ liệu đang lưu. Member có thể thay đổi loại ăn chay, thêm/bớt nguyên liệu kiêng, thêm/bớt món không thích hoặc các sở thích mềm.
   - Bước 4: Member nhấn "Lưu thay đổi".
   - Bước 5: Hệ thống kiểm tra tính hợp lệ (đảm bảo đủ 3 nhóm thông tin tối thiểu theo quy tắc), lưu dữ liệu mới và phản hồi thành công.
   - Bước 6: **Hiệu lực dữ liệu:**
     - Các yêu cầu gọi AI cá nhân hóa tiếp theo sẽ lập tức áp dụng dữ liệu hồ sơ mới nhất này.
     - Việc cập nhật hồ sơ sở thích TUYỆT ĐỐI KHÔNG tự động thay đổi, tính toán lại hoặc ghi đè lên các thực đơn tuần đã được lưu trước đó trong Lịch ăn (SRS 3.20).
2. **Error Flows:**
   - *Vi phạm quy tắc dữ liệu:* Nếu Member bỏ chọn loại ăn chay mà không chọn loại mới, hoặc xóa hết danh sách dị ứng/món không thích mà không tích xác nhận "Không có", hệ thống từ chối lưu và hiển thị thông báo lỗi cụ thể cho từng trường dữ liệu.

##### C. Luồng Cổng kiểm soát dữ liệu trước khi gọi AI cá nhân hóa
1. **Main Flow (Đủ điều kiện thông tin):**
   - Bước 1: Member nhấn yêu cầu tính năng AI cá nhân hóa (gợi ý món hoặc tạo thực đơn tuần).
   - Bước 2: Hệ thống tiếp nhận yêu cầu và kiểm tra dữ liệu hồ sơ sở thích của Member.
   - Bước 3: Hệ thống xác nhận: Đã chọn loại ăn chay hợp lệ VÀ (có danh sách dị ứng hoặc đã tích xác nhận "Không có") VÀ (có danh sách món không thích hoặc đã tích xác nhận "Không có").
   - Bước 4: Điều kiện tối thiểu thỏa mãn -> Hệ thống tiếp tục kiểm tra hạn mức sử dụng AI, đưa hồ sơ sở thích vào ngữ cảnh xử lý và gửi yêu cầu tới dịch vụ Google Gemini.
2. **Luồng Rẽ nhánh An toàn (Thiếu thông tin tối thiểu — Chặn an toàn & Không trừ lượt AI):**
   - Bước 1: Member (chưa hoàn thành Onboarding hoặc hồ sơ chưa đầy đủ) yêu cầu tính năng AI cá nhân hóa.
   - Bước 2: Hệ thống kiểm tra hồ sơ sở thích và phát hiện thiếu ít nhất một trong ba nhóm thông tin tối thiểu.
   - Bước 3: Hệ thống **NGAY LẬP TỨC DỪNG XỬ LÝ**:
     - **TUYỆT ĐỐI KHÔNG GỌI DỊCH VỤ GOOGLE GEMINI** (BR-31).
     - **TUYỆT ĐỐI KHÔNG TRỪ HẠN MỨC GỌI AI** của tài khoản (BR-31).
   - Bước 4: Hệ thống phản hồi từ chối kèm danh sách các trường thông tin còn thiếu.
   - Bước 5: Giao diện hiển thị thông báo thân thiện: "Bạn cần hoàn tất 3 thông tin cơ bản về chế độ ăn chay (Loại ăn chay, Nguyên liệu dị ứng/kiêng, Món không thích) để AI có thể gợi ý chính xác và an toàn." kèm nút bấm "Cập nhật hồ sơ ngay".
   - Bước 6: Member nhấp vào nút và được chuyển trực tiếp tới màn hình bổ sung các trường còn thiếu.

#### 7. Hậu điều kiện (Postconditions)
- Khi hoàn thành Onboarding hoặc cập nhật hồ sơ: Hồ sơ sở thích được lưu trữ gắn với tài khoản Member; ghi nhận trạng thái đã hoàn tất Onboarding.
- Khi gọi AI thành công: Yêu cầu AI cá nhân hóa sử dụng đúng các ràng buộc sở thích mới nhất.
- Khi bị chặn do thiếu hồ sơ: Hạn mức gọi AI giữ nguyên 100%; không có yêu cầu nào gửi tới dịch vụ AI bên ngoài; không có dữ liệu sai lệch nào được ghi nhận vào Lịch ăn.

#### 8. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Khách vãng lai (Guest) không có hồ sơ cá nhân và không thể gọi AI cá nhân hóa (BR-05).
- Chỉ Member đã đăng nhập mới có quyền tạo và chỉnh sửa hồ sơ sở thích của chính mình.
- Dữ liệu sở thích ăn uống và dị ứng là thông tin riêng tư, được bảo vệ theo NFR-08 và NFR-20; không bao giờ được hiển thị trong hồ sơ công khai hay tiết lộ cho người dùng khác.
- Việc hoàn thành Onboarding và khai báo dị ứng là hỗ trợ cá nhân hóa ẩm thực, không cấu thành cam kết an toàn y tế tuyệt đối hay thay thế kiểm tra an toàn thực phẩm ngoài đời thực (SRS 3.15).
- Ràng buộc kỹ thuật được phê duyệt: Tích hợp Google Gemini cho các chức năng AI; kiểm soát hạn mức gọi AI nghiêm ngặt.

#### 9. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest (Guest không có hồ sơ cá nhân hóa).
  - [BR-30](BUSINESS-RULES.md#br-30): Ranh giới chức năng khi bỏ qua Onboarding (không khóa các chức năng không cá nhân hóa).
  - [BR-31](BUSINESS-RULES.md#br-31): Không gọi AI và không trừ hạn mức khi chặn do thiếu hồ sơ; để trống không tự hiểu là "Không có".
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-04](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-04): Thời gian tạo thực đơn tuần theo hồ sơ và ràng buộc ăn uống $\le 8$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân & sở thích ăn uống, HTTPS/TLS 1.2+, kiểm soát quyền sở hữu.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt, thích ứng đa thiết bị.
  - [NFR-20](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20): Cơ chế đồng ý (Consent) và bảo vệ dữ liệu cá nhân thói quen ăn uống, dị ứng; người dùng có quyền tự xem và sửa.

#### 10. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-31.1 — Hoàn thành Onboarding với danh sách nguyên liệu cụ thể:**
  - **Given:** Member vừa đăng ký tài khoản mới và đang ở màn hình Onboarding Questionnaire.
  - **When:** Member chọn loại ăn chay `Vegan`, chọn dị ứng là `Đậu phộng`, chọn món không thích là `Mướp đắng`, rồi nhấn "Hoàn tất".
  - **Then:** Hệ thống xác thực dữ liệu hợp lệ, lưu trữ hồ sơ, ghi nhận trạng thái đã hoàn thành Onboarding và điều hướng vào trang Khám phá.

- **AC-31.2 — Hoàn thành Onboarding với tùy chọn xác nhận "Không có":**
  - **Given:** Member đang ở màn hình Onboarding Questionnaire.
  - **When:** Member chọn loại ăn chay `Lacto-Ovo Vegetarian`, tích chọn xác nhận "Tôi không có dị ứng/kiêng cử", và tích chọn "Tôi không có món không thích", rồi nhấn "Hoàn tất".
  - **Then:** Hệ thống ghi nhận trạng thái xác nhận không có dị ứng và không có món không thích, lưu hồ sơ thành công và kích hoạt trạng thái đủ điều kiện sử dụng AI cá nhân hóa.

- **AC-31.3 — Bỏ qua Onboarding không làm khóa các tính năng thông thường:**
  - **Given:** Member mới đang ở màn hình Onboarding.
  - **When:** Member nhấn nút "Bỏ qua" (Skip).
  - **Then:** Hệ thống ghi nhận trạng thái chưa hoàn tất Onboarding; Member vẫn có thể thực hiện tìm kiếm bài viết, lưu công thức, xếp lịch ăn thủ công, Like, bình luận và tạo Recipe Post bình thường mà không bị ngăn cản (BR-30).

- **AC-31.4 — Chặn gọi AI cá nhân hóa khi chưa chọn loại ăn chay:**
  - **Given:** Member chưa chọn loại ăn chay trong hồ sơ sở thích và còn lượt sử dụng AI.
  - **When:** Member nhấn yêu cầu "AI gợi ý món cho tôi" hoặc "AI tạo thực đơn tuần".
  - **Then:** Hệ thống kiểm tra và chặn yêu cầu; tuyệt đối KHÔNG gửi yêu cầu tới dịch vụ AI; hạn mức AI của Member giữ nguyên; hệ thống hiển thị thông báo yêu cầu chọn loại ăn chay trước (BR-31).

- **AC-31.5 — Chặn gọi AI cá nhân hóa khi chưa xác nhận thông tin dị ứng/kiêng cử:**
  - **Given:** Member đã chọn loại ăn chay nhưng chưa khai báo danh sách dị ứng và cũng chưa tích xác nhận "Không có dị ứng/kiêng cử".
  - **When:** Member nhấn yêu cầu chức năng AI cá nhân hóa.
  - **Then:** Hệ thống kiểm tra và chặn yêu cầu; tuyệt đối KHÔNG gửi yêu cầu tới dịch vụ AI; hạn mức AI giữ nguyên; hệ thống thông báo yêu cầu xác nhận thông tin dị ứng trước (BR-31).

- **AC-31.6 — Chặn gọi AI cá nhân hóa khi chưa xác nhận món hoặc nguyên liệu không thích:**
  - **Given:** Member đã chọn loại ăn chay và xác nhận dị ứng, nhưng chưa khai báo món không thích và chưa tích xác nhận "Không có món không thích".
  - **When:** Member nhấn yêu cầu chức năng AI cá nhân hóa.
  - **Then:** Hệ thống kiểm tra và chặn yêu cầu; tuyệt đối KHÔNG gửi yêu cầu tới dịch vụ AI; hạn mức AI giữ nguyên; hệ thống thông báo yêu cầu xác nhận thông tin món không thích trước (BR-31).

- **AC-31.7 — Từ chối lưu hồ sơ khi để trống trường dị ứng mà không tích xác nhận "Không có":**
  - **Given:** Member đang ở màn hình Cập nhật sở thích ăn uống.
  - **When:** Member xóa toàn bộ danh sách dị ứng nhưng KHÔNG tích chọn ô "Không có" và nhấn "Lưu thay đổi".
  - **Then:** Hệ thống từ chối lưu, yêu cầu chọn ít nhất một nguyên liệu hoặc tích xác nhận không có dị ứng/kiêng cử (BR-31).

- **AC-31.8 — Cập nhật hồ sơ có hiệu lực ngay với AI nhưng không sửa đổi Meal Plan cũ:**
  - **Given:** Member có Lịch ăn tuần trước đã lưu khi đang ăn chay kiểu `Lacto Vegetarian`.
  - **When:** Member vào Cài đặt đổi loại ăn chay sang `Vegan` và lưu thành công.
  - **Then:** Yêu cầu gọi AI lập thực đơn tiếp theo sẽ tuân thủ nghiêm ngặt chuẩn `Vegan`; toàn bộ các mục Lịch ăn của tuần trước vẫn được giữ nguyên không bị tự động sửa đổi hoặc xóa bỏ (SRS 3.20).

- **AC-31.9 — Bảo vệ tính riêng tư của dữ liệu sở thích:**
  - **Given:** Member A đã khai báo danh sách dị ứng và sở thích ăn uống cá nhân.
  - **When:** Người dùng khác xem trang hồ sơ công khai của Member A.
  - **Then:** Trang hồ sơ công khai tuyệt đối không chứa thông tin về loại ăn chay, danh sách dị ứng hay món không thích của Member A.

---

<a id="fr-32"></a>
### FR-32 — Lưu, bỏ lưu và quản lý danh sách công thức đã lưu

- **Mã yêu cầu:** FR-32
- **Module:** M02 (Identity & Access), M04 (Recipe Catalog & Search), M05 (Meal Planning & Shopping List)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống cho Member lưu/bỏ lưu bài công thức và xem, tìm/lọc danh sách công thức đã lưu; mỗi bài chỉ xuất hiện một lần trong danh sách của cùng Member (idempotent); tham chiếu đã lưu được giữ bằng unavailable/tombstone nếu bài bị ẩn hoặc xóa vi phạm, nhưng bài không còn được khám phá công khai hoặc dùng bởi AI.

#### 1. Mục đích
Hỗ trợ thành viên lưu trữ nhanh các bài công thức yêu thích thành một sổ tay nấu ăn cá nhân; giúp người dùng dễ dàng tìm lại và nấu lại các món ăn tâm đắc mà không phải tìm kiếm lại từ đầu; đồng thời cung cấp danh mục món ăn sẵn sàng để đưa nhanh vào Lịch ăn tuần.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member`: Người dùng đã đăng nhập thực hiện lưu, bỏ lưu và quản lý danh sách.
  - `Guest`: Người dùng chưa đăng nhập được yêu cầu đăng nhập khi muốn lưu bài.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-32.1`: Lưu bài công thức vào danh sách đã lưu (Save recipe to saved list).
  - `UC-32.2`: Bỏ lưu bài công thức khỏi danh sách (Unsave recipe).
  - `UC-32.3`: Xem và tìm kiếm trong danh sách công thức đã lưu (View and search saved recipes).
- **User Stories:**
  - `US-32.1`: Là một thành viên đang lướt xem công thức, tôi muốn nhấn Lưu bài để gom các món ngon vào danh sách riêng để cuối tuần nấu thử.
  - `US-32.2`: Là một thành viên có nhiều công thức đã lưu, tôi muốn tìm kiếm nhanh theo tên món trong danh sách của mình.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Bài công thức đang ở trạng thái công khai (`PUBLISHED`). Đối với thao tác lưu/bỏ lưu: người dùng đã đăng nhập với tài khoản Member.
- **Trigger:** Member nhấn nút "Lưu bài viết" trên thẻ hoặc trang chi tiết bài công thức, hoặc truy cập trang "Công thức đã lưu".

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Lưu bài công thức (UC-32.1)
1. **Main Flow:**
   - Bước 1: Member đang xem một bài công thức mà tài khoản chưa lưu.
   - Bước 2: Member nhấn nút "Lưu bài viết".
   - Bước 3: Hệ thống kiểm tra xem bài viết đã có trong danh sách lưu của Member chưa. Chưa có.
   - Bước 4: Hệ thống ghi nhận bài viết vào danh sách công thức đã lưu của Member và lưu mốc thời gian lưu.
   - Bước 5: Nút trên giao diện chuyển thành "Đã lưu" kèm thông báo thành công ngắn.
2. **Alternative Flow (Thao tác lặp lại - Idempotency):**
   - Nếu client gửi lại yêu cầu lưu cho bài viết đã được lưu trước đó, hệ thống giữ nguyên trạng thái thành công, bảo đảm mỗi bài chỉ xuất hiện tối đa 1 lần duy nhất trong danh sách đã lưu của cùng một Member (BR-32).

##### B. Luồng Bỏ lưu bài công thức (UC-32.2)
1. **Main Flow:**
   - Bước 1: Member nhấn nút "Đã lưu" trên trang bài viết, hoặc nhấn nút "Bỏ lưu" trong trang danh sách đã lưu.
   - Bước 2: Hệ thống gỡ bài viết khỏi danh sách công thức đã lưu của Member đó.
   - Bước 3: Giao diện cập nhật trạng thái nút về "Lưu bài viết" và gỡ thẻ bài viết khỏi danh sách hiển thị.

##### C. Luồng Xem và Tìm kiếm trong danh sách đã lưu (UC-32.3)
1. **Main Flow:**
   - Bước 1: Member mở trang "Công thức đã lưu" (Saved Recipes).
   - Bước 2: Hệ thống truy xuất danh sách bài viết đã lưu của Member đó và hiển thị dạng danh sách/lưới kèm phân trang.
   - Bước 3: Member có thể nhập từ khóa vào ô tìm kiếm nội bộ để lọc nhanh các món đã lưu theo tên món hoặc nguyên liệu.
2. **Alternative Flow (Xử lý bài viết nguồn không còn khả dụng):**
   - Nếu một bài viết đã lưu bị tác giả xóa hoặc bị Quản trị viên ẩn do vi phạm quy tắc:
   - Bản ghi tham chiếu trong danh sách đã lưu được giữ lại dưới dạng trạng thái không khả dụng (tombstone pattern theo BR-32).
   - Thẻ bài viết hiển thị nhãn: "Công thức này không còn khả dụng (đã bị gỡ bỏ hoặc ẩn vi phạm)".
   - Người dùng không thể nhấn vào để xem chi tiết bài; bài viết này không tham gia vào gợi ý AI hay tính toán dinh dưỡng, nhưng người dùng vẫn có thể bấm nút "Bỏ lưu" để dọn dẹp danh sách của mình.

##### D. Security Flow (Guest bấm lưu bài)
1. **Main Flow:**
   - Guest nhấn "Lưu bài viết" -> Hệ thống hiển thị hộp thoại yêu cầu đăng nhập kèm liên kết chuyển đến trang Đăng nhập (BR-05, BR-32).

#### 6. Hậu điều kiện (Postconditions)
- Trạng thái lưu/bỏ lưu được cập nhật chính xác cho tài khoản Member.
- Danh sách công thức đã lưu là dữ liệu riêng tư, không hiển thị trên hồ sơ công khai (FR-23).

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Guest không có quyền lưu hay sở hữu danh sách công thức đã lưu (BR-05, BR-32).
- Máy chủ bắt buộc kiểm tra quyền sở hữu tài nguyên (Ownership Check) ở tầng nghiệp vụ; không người dùng nào được xem hay sửa danh sách đã lưu của người khác (NFR-08, NFR-09).

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu riêng tư cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-32.1 — Member lưu bài công thức thành công:**
  - **Given:** Member đang xem một bài công thức công khai chưa lưu.
  - **When:** Member nhấn nút "Lưu bài viết".
  - **Then:** Hệ thống ghi nhận bài viết vào danh sách đã lưu của Member, nút chuyển sang trạng thái "Đã lưu".

- **AC-32.2 — Đảm bảo tính duy nhất của bài viết trong danh sách đã lưu:**
  - **Given:** Bài công thức A đã có trong danh sách lưu của Member.
  - **When:** Có yêu cầu lưu bài A gửi tới hệ thống từ cùng tài khoản.
  - **Then:** Hệ thống chỉ duy trì đúng 1 bản ghi bài A trong danh sách lưu của Member đó (BR-32).

- **AC-32.3 — Member bỏ lưu bài công thức thành công:**
  - **Given:** Bài công thức A đang ở trạng thái đã lưu.
  - **When:** Member nhấn nút "Bỏ lưu".
  - **Then:** Hệ thống xóa bài A khỏi danh sách đã lưu của Member và chuyển nút về trạng thái chưa lưu.

- **AC-32.4 — Tìm kiếm theo từ khóa trong danh sách đã lưu chính xác:**
  - **Given:** Member có 10 bài công thức đã lưu trong sổ tay.
  - **When:** Member nhập từ khóa "canh chua" vào ô tìm kiếm của danh sách đã lưu.
  - **Then:** Hệ thống chỉ hiển thị các bài viết đã lưu có tên hoặc mô tả chứa từ khóa "canh chua".

- **AC-32.5 — Hiển thị nhãn không khả dụng khi bài viết nguồn bị xóa hoặc ẩn:**
  - **Given:** Bài công thức B đã lưu trước đó nay bị Quản trị viên ẩn do vi phạm.
  - **When:** Member mở danh sách công thức đã lưu.
  - **Then:** Bài B vẫn xuất hiện nhưng hiển thị nhãn "Công thức không còn khả dụng", không cho phép mở chi tiết và cho phép bấm bỏ lưu (BR-32).

- **AC-32.6 — Chặn Guest lưu bài công thức và yêu cầu đăng nhập:**
  - **Given:** Người dùng chưa đăng nhập (Guest) xem bài viết.
  - **When:** Guest nhấn nút "Lưu bài viết".
  - **Then:** Hệ thống hiển thị hộp thoại yêu cầu đăng nhập và không thực hiện thao tác lưu (BR-05, BR-32).

---

<a id="fr-33"></a>
### FR-33 — Thêm và quản lý bài công thức trong lịch ăn 3 bữa

- **Mã yêu cầu:** FR-33
- **Module:** M02 (Identity & Access), M04 (Recipe Catalog & Search), M05 (Meal Planning & Shopping List)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Hệ thống cho phép Member thêm bài công thức vào ngày và một trong ba loại Bữa sáng / Bữa trưa / Bữa tối, chuyển ngày/bữa, thay công thức và xóa mục khỏi lịch ăn; mỗi bữa được có nhiều công thức nhưng không có cùng công thức trùng trong cùng ngày/bữa; tham chiếu hiện có được giữ bằng unavailable/tombstone khi bài nguồn không còn khả dụng.

#### 1. Mục đích
Cung cấp công cụ thao tác linh hoạt, chi tiết và trực quan để thành viên tự do thiết kế từng bữa ăn cụ thể trong Lịch ăn tuần; bảo đảm dữ liệu bữa ăn chính xác, không bị trùng lặp công thức trong cùng một bữa; làm cơ sở vững chắc cho chức năng kiểm tra dinh dưỡng ngày (FR-37) và tổng hợp danh sách đi chợ tự động (FR-53).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member`: Người dùng đã đăng nhập thao tác sắp xếp món ăn vào các bữa.

#### 3. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-33.1`: Thêm bài công thức vào bữa ăn cụ thể trong lịch (Add recipe to meal slot).
  - `UC-33.2`: Chuyển món ăn sang bữa hoặc ngày khác (Move recipe to another meal slot).
  - `UC-33.3`: Xóa món ăn khỏi bữa trong Lịch ăn (Remove recipe from meal slot).
- **User Stories:**
  - `US-33.1`: Là một thành viên, tôi muốn thêm món Canh nấm vào Bữa trưa Thứ Ba để chuẩn bị thực đơn cho gia đình.
  - `US-33.2`: Là một thành viên, tôi muốn có thể chuyển món ăn từ Bữa trưa sang Bữa tối khi tôi bận không kịp nấu bữa trưa.
  - `US-33.3`: Là một thành viên, tôi muốn thêm cả món xào và món canh vào cùng một Bữa trưa mà không bị giới hạn chỉ 1 món.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Bài công thức đang ở trạng thái công khai (`PUBLISHED`) hoặc trong danh sách đã lưu của Member. Member đã đăng nhập với tài khoản hoạt động (`ACTIVE`).
- **Trigger:** Member nhấn nút "Thêm món vào bữa" trên Lịch ăn, hoặc nhấn nút "Thêm vào lịch ăn" trên trang chi tiết bài công thức.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Thêm bài công thức vào bữa ăn (UC-33.1)
1. **Main Flow:**
   - Bước 1: Member chọn ngày cụ thể (ví dụ: Thứ Tư) và loại bữa (ví dụ: Bữa trưa) trên Lịch ăn tuần.
   - Bước 2: Giao diện hiển thị hộp thoại chọn công thức: Member có thể chọn từ "Công thức đã lưu" hoặc tìm kiếm trong kho công thức công khai.
   - Bước 3: Member chọn bài công thức và nhấn "Thêm vào bữa".
   - Bước 4: Hệ thống kiểm tra quy tắc trùng lặp: bài công thức này chưa tồn tại trong cùng một bữa và cùng một ngày đó (`(userId, weekDate, mealType, recipeId)` chưa tồn tại theo BR-33).
   - Bước 5: Hệ thống lưu mục món ăn vào bữa tương ứng, mặc định số khẩu phần bằng khẩu phần gốc của bài viết.
   - Bước 6: Thẻ món ăn xuất hiện ngay trong ô Bữa trưa Thứ Tư của Lịch ăn tuần.
2. **Alternative Flow (Nhiều món trong một bữa):**
   - Member tiếp tục nhấn thêm món khác (ví dụ: thêm món Xào vào cùng Bữa trưa Thứ Tư). Hệ thống ghi nhận thành công, cho phép một bữa có nhiều món ăn khác nhau (BR-33).
3. **Error Flow (Trùng công thức trong cùng bữa):**
   - Nếu Member cố tình chọn lại chính bài công thức đã có trong Bữa trưa Thứ Tư -> Hệ thống từ chối và thông báo: "Món ăn này đã có trong Bữa trưa Thứ Tư. Bạn không thể thêm trùng món trong cùng một bữa." (BR-33).

##### B. Luồng Chuyển món ăn sang bữa hoặc ngày khác (UC-33.2)
1. **Main Flow:**
   - Bước 1: Tại thẻ món ăn trong một bữa, Member chọn tùy chọn "Chuyển bữa / Chuyển ngày" (hoặc kéo thả thẻ món ăn sang ô bữa khác).
   - Bước 2: Member chọn ngày đích và bữa đích (ví dụ: chuyển từ Trưa Thứ Tư sang Tối Thứ Tư).
   - Bước 3: Hệ thống kiểm tra trùng lặp tại vị trí đích. Nếu vị trí đích chưa có món này, hệ thống cập nhật vị trí mới cho món ăn và phản hồi thành công.
2. **Error Flow:**
   - Nếu vị trí đích đã có sẵn món ăn đó -> Hệ thống cảnh báo món đã tồn tại ở vị trí đích và không thực hiện chuyển.

##### C. Luồng Xóa món ăn khỏi bữa (UC-33.3)
1. **Main Flow:**
   - Bước 1: Member nhấn biểu tượng xóa (dấu X hoặc thùng rác) tại thẻ món ăn trong bữa.
   - Bước 2: Hệ thống gỡ bỏ món ăn đó khỏi bữa ăn tương ứng và cập nhật lại giao diện.

##### D. Luồng Xử lý khi bài viết nguồn không còn khả dụng
1. **Main Flow:**
   - Nếu một bài công thức đã được xếp vào Lịch ăn nhưng sau đó bị tác giả xóa hoặc bị Admin ẩn vi phạm:
   - Mục món ăn trong Lịch ăn được giữ nguyên vị trí nhưng chuyển sang trạng thái "Không còn khả dụng" (tombstone pattern theo BR-33).
   - Thẻ hiển thị nhãn: "Món ăn đã bị gỡ bỏ hoặc ẩn vi phạm" kèm nút "Thay thế món khác" hoặc "Xóa khỏi bữa".
   - Món ăn này không còn tham gia vào tính toán dinh dưỡng (FR-37) hay danh sách mua sắm (FR-53).

#### 6. Hậu điều kiện (Postconditions)
- Cấu trúc các món ăn trong từng bữa được cập nhật chính xác.
- Bảo đảm tuyệt đối không có 2 bản ghi cùng bài công thức trong cùng một bữa của một ngày.

#### 7. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Dữ liệu các món trong Lịch ăn là thông tin riêng tư của Member (FR-23).
- Toàn bộ các thao tác thêm, chuyển, xóa đều bắt buộc kiểm tra quyền sở hữu ở tầng máy chủ (NFR-08, NFR-09).

#### 8. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Lịch ăn.
  - [BR-33](BUSINESS-RULES.md#br-33): Xử lý tham chiếu công thức trong Lịch ăn.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và toàn vẹn giao dịch.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt.

#### 9. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-33.1 — Thêm bài công thức vào bữa ăn cụ thể thành công:**
  - **Given:** Member đang ở giao diện Lịch ăn tuần.
  - **When:** Member chọn món "Canh chua nấm" đưa vào Bữa trưa Thứ Ba.
  - **Then:** Hệ thống lưu thành công món ăn vào Bữa trưa Thứ Ba và hiển thị thẻ món trên bảng lịch.

- **AC-33.2 — Cho phép thêm nhiều món ăn khác nhau trong cùng một bữa:**
  - **Given:** Bữa trưa Thứ Ba đã có món "Canh chua nấm".
  - **When:** Member thêm tiếp món "Đậu phụ kho sả" vào Bữa trưa Thứ Ba.
  - **Then:** Hệ thống ghi nhận thành công cả 2 món ăn trong cùng một Bữa trưa Thứ Ba (BR-33).

- **AC-33.3 — Từ chối thêm cùng bài công thức trùng lặp trong cùng một bữa:**
  - **Given:** Bữa trưa Thứ Ba đã có món "Canh chua nấm".
  - **When:** Member cố tình thêm lại món "Canh chua nấm" vào Bữa trưa Thứ Ba.
  - **Then:** Hệ thống từ chối thao tác và hiển thị thông báo món ăn đã tồn tại trong bữa ăn này (BR-33).

- **AC-33.4 — Chuyển thành công món ăn sang bữa hoặc ngày khác:**
  - **Given:** Món "Đậu phụ kho sả" đang ở Bữa trưa Thứ Ba.
  - **When:** Member thực hiện chuyển món sang Bữa tối Thứ Ba.
  - **Then:** Món ăn được dời sang Bữa tối Thứ Ba và không còn ở Bữa trưa Thứ Ba.

- **AC-33.5 — Xóa thành công món ăn khỏi bữa ăn:**
  - **Given:** Món ăn đang có trong một bữa của Lịch ăn.
  - **When:** Member nhấn biểu tượng xóa tại món ăn đó.
  - **Then:** Món ăn bị gỡ bỏ khỏi bữa ăn tương ứng và biến mất khỏi giao diện.

- **AC-33.6 — Giữ bản ghi ở trạng thái không khả dụng khi bài nguồn bị gỡ bỏ:**
  - **Given:** Bài công thức trong Lịch ăn tuần bị tác giả xóa bài gốc.
  - **When:** Member mở lại Lịch ăn tuần.
  - **Then:** Vị trí bữa ăn hiển thị nhãn "Món ăn đã bị gỡ bỏ hoặc ẩn vi phạm" kèm tùy chọn thay thế hoặc xóa bỏ (BR-33).

---

<a id="fr-34"></a>
### FR-34 — AI gợi ý món và lập menu từ công thức công khai có sẵn

- **Mã yêu cầu:** FR-34
- **Module:** M04 (Recipe Catalog & Search), M05 (Meal Planning & Shopping List), M06 (AI Assistant & Personalization)
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** AI gợi ý món, lập menu và đề xuất thay thế chỉ từ Recipe Post đang công khai (`PUBLISHED`), không hidden/deleted/unavailable, dựa trên hồ sơ sở thích (FR-31) và nguyên liệu người dùng cung cấp; AI không bịa công thức, mọi kết quả dẫn tới bài nguồn và chỉ được lưu sau khi người dùng xác nhận (SRS 3.17, BR-31, BR-34).

#### 1. Mục đích
Tận dụng năng lực phân tích thông minh của AI để giải quyết bài toán gợi ý món ăn phù hợp với nguyên liệu thực tế và sở thích riêng của từng gia đình; bảo đảm 100% các món ăn được gợi ý đều là các công thức thực tế, có thể nấu được ngay từ kho công thức cộng đồng mà không bị ảo giác sinh ra công thức giả mạo.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member`: Người dùng đã đăng nhập yêu cầu AI gợi ý món hoặc tạo thực đơn tuần.
- **Secondary Actor / External System:**
  - `Google Gemini AI`: Tiếp nhận danh sách bài công thức ứng viên và thực hiện lựa chọn, xếp hạng thực đơn.

#### 3. Ranh giới an toàn của tính năng AI gợi ý món (SRS 3.17, BR-34)
- **Nguyên tắc "Không bịa công thức" (No Hallucination):** AI tuyệt đối không tự sinh ra tên món ăn hay cách nấu không có trong cơ sở dữ liệu. Mọi món ăn do AI đề xuất bắt buộc phải là một Recipe Post đang tồn tại hợp lệ ở trạng thái `PUBLISHED`.
- **Nguyên tắc dẫn nguồn bài viết:** Mỗi món ăn được gợi ý phải đi kèm đường dẫn xem chi tiết công thức gốc của tác giả trong hệ thống.
- **Nguyên tắc xác nhận trước khi lưu:** Kết quả gợi ý thực đơn tuần của AI chỉ mang tính đề xuất; hệ thống TUYỆT ĐỐI KHÔNG tự động ghi đè vào Lịch ăn của Member nếu người dùng chưa bấm nút "Xác nhận áp dụng vào Lịch ăn" (BR-34).

#### 4. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-34.1`: Yêu cầu AI gợi ý món ăn theo nguyên liệu sẵn có (Request AI recipe suggestions by ingredients).
  - `UC-34.2`: Yêu cầu AI lập thực đơn tuần từ kho công thức (Request AI weekly meal plan generation).
  - `UC-34.3`: Xác nhận lưu thực đơn AI đề xuất vào Lịch ăn (Confirm and apply AI meal plan).
- **User Stories:**
  - `US-34.1`: Là một thành viên đang có sẵn nấm và đậu phụ, tôi muốn nhờ AI tìm giúp các món ngon nấu được từ hai nguyên liệu này từ kho bài viết của cộng đồng.
  - `US-34.2`: Là một thành viên bận rộn, tôi muốn AI lên giúp tôi thực đơn 7 ngày hợp khẩu vị, tôi xem qua thấy hài lòng rồi mới bấm lưu vào Lịch ăn của mình.

#### 5. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Member đã đăng nhập và **bắt buộc đã hoàn tất 3 nhóm thông tin tối thiểu** trong hồ sơ sở thích ăn uống theo FR-31 (loại ăn chay, xác nhận dị ứng, xác nhận món không thích); tài khoản còn hạn mức gọi AI trong ngày theo FR-10.
- **Trigger:** Member nhấn nút "AI gợi ý món" hoặc "AI tạo thực đơn tuần".

#### 6. Luồng sự kiện (Flow of Events)

##### A. Luồng AI gợi ý món ăn theo nguyên liệu sẵn có (UC-34.1)
1. **Main Flow:**
   - Bước 1: Member mở tính năng "AI gợi ý món ăn".
   - Bước 2: Member nhập danh sách nguyên liệu đang có (ví dụ: "đậu phụ, cà chua, nấm đùi gà").
   - Bước 3: Hệ thống kiểm tra điều kiện hồ sơ sở thích tối thiểu (FR-31) và hạn mức AI còn lại (FR-10). Đủ điều kiện.
   - Bước 4: Hệ thống truy vấn cơ sở dữ liệu nội bộ để lấy danh sách các Recipe Post đang công khai (`PUBLISHED`) có chứa ít nhất một trong các nguyên liệu trên và phù hợp với loại ăn chay của Member, loại trừ các món chứa chất dị ứng của Member.
   - Bước 5: Hệ thống gửi danh sách ứng viên bài viết này cùng prompt tới Google Gemini để chọn lọc 3–5 món ăn kết hợp nguyên liệu hài hòa nhất.
   - Bước 6: Google Gemini trả về danh sách các bài viết được chọn.
   - Bước 7: Giao diện hiển thị danh sách các món ăn kèm ảnh, tên món, lý do gợi ý và liên kết dẫn thẳng tới bài công thức nguồn. Trừ 1 lượt AI thành công.

##### B. Luồng AI lập thực đơn tuần và Xác nhận lưu (UC-34.2, UC-34.3)
1. **Main Flow:**
   - Bước 1: Member nhấn nút "AI lập thực đơn tuần mới".
   - Bước 2: Hệ thống kiểm tra điều kiện hồ sơ tối thiểu (FR-31) và hạn mức AI (FR-10). Đủ điều kiện.
   - Bước 3: Hệ thống truy vấn kho bài công thức công khai, lọc theo loại ăn chay chuẩn của Member và loại trừ các dị ứng đã khai báo.
   - Bước 4: Hệ thống đưa danh sách bài viết ứng viên vào ngữ cảnh và yêu cầu Gemini phân bổ vào 7 ngày $\times$ 3 bữa sao cho thực đơn phong phú, không lặp món liên tục.
   - Bước 5: Gemini trả về cấu trúc thực đơn tuần đề xuất gồm các ID bài viết công khai.
   - Bước 6: Giao diện hiển thị **Bản xem trước thực đơn tuần do AI đề xuất** (Preview Mode). Trừ 1 lượt AI thành công.
   - Bước 7: Member xem xét thực đơn. Nếu ưng ý, Member nhấn nút "Xác nhận áp dụng vào Lịch ăn" (UC-34.3).
   - Bước 8: Hệ thống lưu các món ăn này vào Lịch ăn tuần được chọn của Member (FR-09, FR-33) và thông báo áp dụng thành công.
2. **Alternative Flow (Không áp dụng thực đơn):**
   - Nếu Member không hài lòng với thực đơn đề xuất, Member có thể bấm "Hủy bỏ" hoặc yêu cầu AI tạo lại phương án khác (nếu còn lượt AI). Lịch ăn hiện tại của Member giữ nguyên vẹn không bị thay đổi.
3. **Exception Flow (Chặn an toàn khi thiếu hồ sơ tối thiểu):**
   - Nếu Member chưa hoàn thành 3 thông tin tối thiểu trong hồ sơ sở thích -> Hệ thống lập tức chặn yêu cầu, tuyệt đối không gọi Gemini, không trừ hạn mức AI, và hiển thị thông báo hướng dẫn bổ sung hồ sơ theo quy định tại FR-31 (BR-31).

#### 7. Hậu điều kiện (Postconditions)
- Các món ăn gợi ý luôn có bài công thức nguồn thực tế tương ứng.
- Thực đơn tuần chỉ được ghi vào cơ sở dữ liệu Lịch ăn sau khi có hành động xác nhận chủ động từ Member (BR-34).

#### 8. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Guest không được phép sử dụng AI cá nhân hóa gợi ý món (BR-05).
- Ràng buộc chặt chẽ trong System Prompt và kiểm tra chéo ở Backend: chỉ chấp nhận ID bài viết hợp lệ đang ở trạng thái `PUBLISHED` từ cơ sở dữ liệu, loại bỏ bất kỳ món ăn nào không có trong hệ thống (NFR-08, NFR-10).

#### 9. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-30](BUSINESS-RULES.md#br-30): Ranh giới chức năng khi bỏ qua Onboarding.
  - [BR-31](BUSINESS-RULES.md#br-31): Không gọi AI và không trừ hạn mức khi thiếu hồ sơ.
  - [BR-34](BUSINESS-RULES.md#br-34): AI gợi ý món từ công thức có sẵn và xác nhận trước khi lưu.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-04](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-04): Thời gian tạo thực đơn tuần theo hồ sơ $\le 8$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và toàn vẹn giao dịch.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### 10. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-34.1 — AI chỉ gợi ý các món ăn có công thức nguồn công khai thực tế:**
  - **Given:** Member yêu cầu AI gợi ý món từ nguyên liệu sẵn có.
  - **When:** AI trả về kết quả gợi ý.
  - **Then:** 100% các món ăn được đề xuất đều gắn liền với Recipe Post đang công khai (`PUBLISHED`) và có đường dẫn hợp lệ dẫn tới bài viết đó (BR-34).

- **AC-34.2 — AI tuân thủ nghiêm ngặt loại ăn chay và danh sách dị ứng:**
  - **Given:** Member khai báo loại ăn chay `Vegan` và dị ứng `Đậu phộng` tại FR-31.
  - **When:** AI tạo thực đơn gợi ý.
  - **Then:** Toàn bộ các món ăn được chọn tuyệt đối không chứa trứng, sữa, mật ong hay đậu phộng.

- **AC-34.3 — Không tự động lưu thực đơn vào Lịch ăn khi chưa được người dùng xác nhận:**
  - **Given:** AI vừa tạo xong bản xem trước thực đơn tuần đề xuất.
  - **When:** Member đóng màn hình xem trước hoặc chưa bấm nút xác nhận.
  - **Then:** Lịch ăn tuần của Member giữ nguyên trạng thái cũ, không bị ghi đè hay thay đổi dữ liệu (BR-34).

- **AC-34.4 — Lưu thành công thực đơn AI vào Lịch ăn sau khi người dùng xác nhận:**
  - **Given:** Member đang ở màn hình xem trước thực đơn tuần do AI đề xuất.
  - **When:** Member nhấn nút "Xác nhận áp dụng vào Lịch ăn".
  - **Then:** Hệ thống lưu toàn bộ các món ăn vào Lịch ăn tuần của Member và hiển thị thông báo thành công.

- **AC-34.5 — Chặn an toàn và không trừ hạn mức khi thiếu hồ sơ sở thích:**
  - **Given:** Member chưa hoàn thành 3 thông tin tối thiểu trong hồ sơ sở thích theo FR-31.
  - **When:** Member nhấn yêu cầu AI gợi ý món.
  - **Then:** Hệ thống dừng xử lý ngay, không gửi request tới Gemini, không trừ hạn mức và hiển thị hướng dẫn bổ sung hồ sơ (BR-31).

---

<a id="fr-35"></a>
### FR-35 — Khai báo hồ sơ dinh dưỡng và xem chỉ số tham khảo cá nhân

- **Mã yêu cầu:** FR-35
- **Module:** M02, M10
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cho phép Member đã đăng nhập và đủ điều kiện sức khỏe khai báo/cập nhật thông số nhân trắc học (tuổi, giới tính, chiều cao, cân nặng, mức độ vận động, mục tiêu dinh dưỡng cá nhân); tính toán chỉ số BMI tham khảo và hiển thị nhu cầu năng lượng/dinh dưỡng ước tính dựa trên dữ liệu chuẩn USDA/NIH; bắt buộc hiển thị tuyên bố từ chối trách nhiệm y tế (Medical Safety Disclaimer); hệ thống tuyệt đối không tự chẩn đoán, kê đơn hay áp đặt chế độ ăn điều trị.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập, từ đủ 18 tuổi, không mang thai, không cho con bú và không có bệnh lý mạn tính cần chế độ ăn điều trị đặc biệt (SRS 3.18, BR-41, BR-42).
  - Không áp dụng: Guest (BR-05), người dùng dưới 18 tuổi, phụ nữ có thai/cho con bú, người cần can thiệp y tế (BR-42).
- **Phân loại Actor:**
  - Primary Actor: `Member` (đã đăng nhập tài khoản hợp lệ).
  - Supporting Actor: `Hệ thống tính toán dinh dưỡng nội bộ` (tham chiếu dữ liệu USDA/NIH).

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-35.1`: Khai báo và cập nhật thông số hồ sơ dinh dưỡng cá nhân.
  - `UC-35.2`: Xem chỉ số BMI và phân loại thể trạng tham khảo kèm thông điệp từ chối trách nhiệm y tế.
  - `UC-35.3`: Xem bảng nhu cầu 9 chỉ tiêu dinh dưỡng hàng ngày tham khảo.
- **User Stories:**
  - *Là một Member ăn chay*, tôi muốn khai báo chiều cao, cân nặng và mức độ vận động để hệ thống cung cấp các chỉ số tham khảo phù hợp với thể trạng của tôi, giúp tôi theo dõi dinh dưỡng một cách an toàn mà không bị nhầm lẫn với chẩn đoán y khoa.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản Member hợp lệ (FR-03).
  - Người dùng đã hoàn thành bước xác nhận phạm vi hỗ trợ và không thuộc đối tượng loại trừ (FR-38, BR-41, BR-42).
- **Kích hoạt (Trigger):**
  - Member truy cập màn hình "Hồ sơ dinh dưỡng cá nhân" hoặc chọn mục cập nhật chỉ số sức khỏe trong trang quản lý tài khoản.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member truy cập giao diện Hồ sơ dinh dưỡng; hệ thống hiển thị nổi bật Tuyên bố từ chối trách nhiệm y tế (Medical Safety Disclaimer theo BR-39, BR-41).
  - Bước 2: Member nhập các thông số: Năm sinh/Tuổi (phải $\ge 18$), Giới tính sinh học, Chiều cao (cm), Cân nặng (kg), Mức độ hoạt động thể chất (Ít vận động, Vận động nhẹ, Vận động vừa, Vận động nặng) và Mục tiêu dinh dưỡng chung (Duy trì cân nặng, Tăng cường sức khỏe, Hỗ trợ tập luyện).
  - Bước 3: Member nhấn "Lưu hồ sơ và Tính toán tham khảo".
  - Bước 4: Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào: Chiều cao từ 100 cm đến 250 cm, Cân nặng từ 30 kg đến 300 kg, Tuổi từ 18 đến 120.
  - Bước 5: Hệ thống tính toán chỉ số khối cơ thể: $\text{BMI} = \frac{\text{Cân nặng (kg)}}{(\text{Chiều cao (m)})^2}$, làm tròn 1 chữ số thập phân (BR-39).
  - Bước 6: Hệ thống xác định phân loại thể trạng tham khảo theo chuẩn WHO/USDA (Thiếu cân, Bình thường, Thừa cân, Béo phì) kèm văn bản cảnh báo rõ ràng rằng đây chỉ là chỉ số sàng lọc tham khảo, không đại diện cho tỷ lệ mỡ hay chẩn đoán sức khỏe cá nhân.
  - Bước 7: Hệ thống tính toán mức nhu cầu tham khảo hàng ngày cho 9 chỉ tiêu cốt lõi (Năng lượng, Đạm, Carb, Chất béo, Chất xơ, Canxi, Sắt, Vitamin B12, Kẽm) dựa trên công thức tham chiếu USDA/NIH và mức độ hoạt động.
  - Bước 8: Hệ thống lưu trữ hồ sơ dinh dưỡng của Member vào cơ sở dữ liệu và hiển thị bảng kết quả chỉ số tham khảo.
- **Luồng thay thế (Alternative Flows):**
  - *AF-35.1 (Cập nhật lại thông số):* Member có thể chỉnh sửa cân nặng hoặc mức độ vận động bất kỳ lúc nào. Hệ thống tự động tính toán lại BMI và 9 chỉ tiêu tham khảo tương ứng, cập nhật ngày sửa đổi gần nhất.
  - *AF-35.2 (Xem lại hồ sơ đã lưu):* Khi Member truy cập trang hồ sơ dinh dưỡng, nếu đã có dữ liệu trước đó, hệ thống tải dữ liệu đã lưu cùng ngày cập nhật và hiển thị đầy đủ thông số tham khảo.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-35.1 (Vi phạm điều kiện loại trừ y tế):* Nếu Member thay đổi thông tin xác nhận và khai báo thuộc nhóm đối tượng loại trừ (dưới 18 tuổi, đang mang thai, cho con bú hoặc mắc bệnh mạn tính theo BR-42), hệ thống lập tức khóa chức năng tính toán cá nhân hóa, hiển thị thông báo khuyến nghị tham vấn bác sĩ/chuyên gia dinh dưỡng và xóa/ẩn mục tiêu dinh dưỡng cá nhân.
  - *EF-35.2 (Thông số nhập không hợp lệ):* Nếu chiều cao hoặc cân nặng nằm ngoài ngưỡng cho phép, hệ thống hiển thị thông báo lỗi tại trường nhập liệu và không tiến hành tính toán.
  - *SF-35.1 (Bảo mật dữ liệu sức khỏe):* Dữ liệu hồ sơ dinh dưỡng và BMI là thông tin nhạy cảm; chỉ chính Member đó mới có quyền xem và sửa (RBAC theo NFR-08, NFR-09, NFR-20). Hệ thống không công khai các chỉ số này trên Author Card hay Profile công khai của Member.

#### 5. Hậu điều kiện (Postconditions)
- Hồ sơ dinh dưỡng của Member được cập nhật an toàn trong hệ thống.
- Bảng 9 chỉ tiêu dinh dưỡng tham khảo cá nhân sẵn sàng để làm mốc so sánh cho chức năng Lịch ăn ngày (FR-37) và gợi ý AI Menu (FR-36).

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chỉ Member sở hữu tài khoản mới có quyền xem và cập nhật hồ sơ dinh dưỡng của mình. Guest và Member khác không có quyền truy cập.
- **Ràng buộc an toàn:** Tuyệt đối không quy đổi BMI thành chế độ ăn cắt giảm calorie cực đoan; hệ thống không cung cấp chức năng kê đơn thuốc hay thực phẩm chức năng.

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest (yêu cầu đăng nhập).
  - [BR-39](BUSINESS-RULES.md#br-39): Vai trò tham khảo của chỉ số BMI và thông điệp từ chối trách nhiệm y tế.
  - [BR-41](BUSINESS-RULES.md#br-41): Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia y tế.
  - [BR-42](BUSINESS-RULES.md#br-42): Đối tượng loại trừ khỏi tính toán nhu cầu dinh dưỡng MVP.
  - [BR-43](BUSINESS-RULES.md#br-43): Gắn số liệu dinh dưỡng với một khẩu phần.
  - [BR-48](BUSINESS-RULES.md#br-48): Quyền khai báo và lưu trữ hồ sơ dinh dưỡng của Member.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân và chỉ số sức khỏe.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.
  - [NFR-20](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20): Cơ chế đồng ý (Consent) và bảo vệ dữ liệu sức khỏe cá nhân.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-35.1 (Hiển thị Disclaimer y tế):**
  - *Given* Member đã đăng nhập và truy cập giao diện Hồ sơ dinh dưỡng,
  - *When* trang được hiển thị,
  - *Then* hệ thống trình bày nổi bật văn bản Tuyên bố từ chối trách nhiệm y tế nêu rõ số liệu chỉ mang tính chất tham khảo dựa trên chuẩn USDA/NIH và không thay thế tư vấn y khoa chuyên nghiệp.
- **AC-35.2 (Chặn đối tượng loại trừ y tế):**
  - *Given* người dùng khai báo đang mang thai, cho con bú, dưới 18 tuổi hoặc có bệnh lý điều trị,
  - *When* người dùng gửi yêu cầu lưu hồ sơ,
  - *Then* hệ thống từ chối tính toán nhu cầu dinh dưỡng cá nhân hóa và hiển thị thông báo hướng dẫn tham vấn ý kiến bác sĩ chuyên khoa.
- **AC-35.3 (Xác thực dữ liệu nhập liệu nhân trắc học):**
  - *Given* Member nhập thông số chiều cao hoặc cân nặng ngoài ngưỡng sinh lý hợp lệ ($100 \le \text{chiều cao} \le 250$ cm, $30 \le \text{cân nặng} \le 300$ kg),
  - *When* Member nhấn nút lưu hồ sơ,
  - *Then* hệ thống ngăn chặn việc lưu và hiển thị thông báo lỗi cụ thể tại từng ô nhập liệu tương ứng.
- **AC-35.4 (Tính toán BMI tham khảo chính xác):**
  - *Given* Member nhập chiều cao 170 cm và cân nặng 65 kg hợp lệ,
  - *When* hệ thống xử lý tính toán,
  - *Then* chỉ số BMI được hiển thị chính xác là 22.5 kèm phân loại thể trạng "Bình thường" và ghi chú đây là chỉ số sàng lọc tham khảo.
- **AC-35.5 (Tính toán bảng 9 chỉ tiêu tham khảo):**
  - *Given* Member có thông số nhân trắc học và mức độ vận động hợp lệ đã được lưu,
  - *When* hệ thống hiển thị hồ sơ dinh dưỡng,
  - *Then* bảng kết quả trình bày đầy đủ định lượng tham khảo hàng ngày cho 9 chỉ tiêu cốt lõi: Năng lượng (kcal), Chất đạm (g), Carbohydrate (g), Chất béo (g), Chất xơ (g), Canxi (mg), Sắt (mg), Vitamin B12 (mcg), Kẽm (mg).
- **AC-35.6 (Bảo mật dữ liệu dinh dưỡng cá nhân):**
  - *Given* một người dùng khác hoặc Guest xem trang hồ sơ công khai của Member,
  - *When* trang hồ sơ công khai được tải,
  - *Then* các thông số chiều cao, cân nặng, BMI và bảng dinh dưỡng cá nhân hoàn toàn không xuất hiện trên giao diện công khai.

---

<a id="fr-36"></a>
### FR-36 — AI lập menu theo nhu cầu dinh dưỡng từ công thức tin cậy

- **Mã yêu cầu:** FR-36
- **Module:** M04, M05, M06, M10
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp tính năng trợ lý AI tạo thực đơn gợi ý theo tuần hoặc theo ngày dựa trên hồ sơ dinh dưỡng tham khảo cá nhân và các ràng buộc ăn uống của Member; AI chỉ được phép lựa chọn từ các bài Recipe Post đang công khai trong hệ thống có dữ liệu định lượng dinh dưỡng tin cậy (100% nguyên liệu đã được ánh xạ gram và có số liệu dinh dưỡng); AI không được tự bịa công thức mới ngoài cơ sở dữ liệu; người dùng phải xác nhận trước khi lưu các món được gợi ý vào Lịch ăn tuần.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập, đã hoàn tất hồ sơ dinh dưỡng (FR-35), đã hoàn thành onboarding sở thích/ràng buộc ăn chay (FR-31) và còn hạn mức AI hợp lệ (FR-10).
  - Giới hạn: Quá trình tạo thực đơn chỉ mang tính tham khảo và hỗ trợ lên kế hoạch ăn uống; việc tối ưu hóa vi chất chuyên sâu theo bệnh lý nằm ngoài phạm vi MVP.
- **Phân loại Actor:**
  - Primary Actor: `Member` (đã đăng nhập và đủ điều kiện).
  - Supporting Actor: `Google Gemini AI` (đóng vai trò tổng hợp và đề xuất món dựa trên prompt giới hạn và kho dữ liệu công thức tin cậy).

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-36.1`: Yêu cầu AI tạo thực đơn tham khảo theo hồ sơ dinh dưỡng và ràng buộc ăn chay.
  - `UC-36.2`: Xem danh sách món được đề xuất kèm giải trình dinh dưỡng và cảnh báo tham khảo.
  - `UC-36.3`: Xác nhận áp dụng thực đơn gợi ý vào các bữa ăn trong Lịch ăn tuần.
- **User Stories:**
  - *Là một Member bận rộn*, tôi muốn nhờ AI đề xuất thực đơn các món chay cho tuần tới dựa trên nhu cầu dinh dưỡng của tôi và chỉ chọn từ những công thức đã có sẵn công thức nấu chuẩn, giúp tôi tiết kiệm thời gian lên thực đơn mà vẫn đảm bảo bữa ăn phong phú.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Member đã đăng nhập tài khoản hợp lệ (FR-03).
  - Member đã hoàn thành hồ sơ dinh dưỡng cá nhân (FR-35) và xác nhận phạm vi hỗ trợ (FR-38).
  - Member đã cấu hình tối thiểu trường phái ăn chay và dị ứng (FR-31).
  - Tài khoản Member còn số dư hạn mức AI khả dụng trong ngày (FR-10, BR-01, BR-02).
- **Kích hoạt (Trigger):**
  - Member nhấn nút "Nhờ AI lập thực đơn dinh dưỡng" trên giao diện Lịch ăn tuần hoặc trang Dinh dưỡng cá nhân.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member chọn phạm vi lập menu (1 ngày hoặc 7 ngày trong tuần) và xác nhận các tiêu chí ưu tiên (giữ nguyên loại trừ dị ứng, ưu tiên trường phái ăn chay hiện tại).
  - Bước 2: Member nhấn "Tạo thực đơn bằng AI".
  - Bước 3: Hệ thống kiểm tra số dư hạn mức AI của tài khoản (BR-01, BR-02, BR-03).
  - Bước 4: Hệ thống truy vấn kho công thức nội bộ để trích xuất tập hợp ứng viên (Candidate Pool) thỏa mãn các điều kiện ngặt nghèo:
    - Trạng thái công thức là đang công khai (`Public`).
    - Phù hợp với trường phái ăn chay và loại trừ hoàn toàn các thành phần dị ứng của Member (BR-30, BR-31).
    - Có 100% nguyên liệu đã được định lượng chuẩn hóa theo gram và ánh xạ đầy đủ 9 chỉ tiêu dinh dưỡng từ danh mục dinh dưỡng nội bộ (BR-40, BR-46). Các công thức chứa nguyên liệu chưa có dữ liệu dinh dưỡng bị loại bỏ hoàn toàn khỏi pool (BR-50).
  - Bước 5: Hệ thống gửi prompt kèm danh sách metadata của các công thức ứng viên (ID, tên món, 9 chỉ số dinh dưỡng/khẩu phần) và mục tiêu dinh dưỡng tham khảo của Member tới Google Gemini AI (NFR-04 $\le 8$ giây). Prompt bắt buộc yêu cầu AI chỉ được ghép nối các ID công thức có sẵn, tuyệt đối không tự tạo công thức mới (BR-38).
  - Bước 6: Google Gemini phản hồi cấu trúc menu được đề xuất phân bổ vào 3 bữa ăn cố định (Sáng, Trưa, Tối) kèm đoạn giải trình ngắn gọn lý do phân bổ.
  - Bước 7: Hệ thống trừ 1 lượt hạn mức AI của Member sau khi nhận phản hồi hợp lệ (BR-03).
  - Bước 8: Hệ thống hiển thị bản xem trước (Preview) thực đơn được đề xuất kèm bảng tổng hợp 9 chỉ tiêu dinh dưỡng dự kiến và cảnh báo từ chối trách nhiệm y tế (BR-39, BR-41).
  - Bước 9: Member xem lại từng món, có thể loại bỏ hoặc giữ nguyên, sau đó nhấn "Lưu vào Lịch ăn".
  - Bước 10: Hệ thống ghi nhận các món được chọn vào các vị trí bữa tương ứng trong Lịch ăn tuần của Member (BR-35, BR-36, BR-37).
- **Luồng thay thế (Alternative Flows):**
  - *AF-36.1 (Kho công thức tin cậy không đủ đa dạng):* Nếu số lượng công thức đạt chuẩn dinh dưỡng tin cậy không đủ để lấp đầy 21 bữa ăn trong tuần mà không bị trùng lặp, hệ thống thông báo cho Member biết phạm vi kho công thức hiện tại và cho phép AI lặp lại công thức ở các ngày khác nhau hoặc đề xuất thực đơn cho số ngày ít hơn.
  - *AF-36.2 (Member hủy bỏ kết quả gợi ý):* Member có thể đóng cửa sổ xem trước mà không lưu vào Lịch ăn. Hạn mức AI đã trừ vẫn giữ nguyên (vì dịch vụ tính toán AI đã hoàn tất thành công theo BR-03).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-36.1 (Hết hạn mức AI):* Nếu tài khoản đã hết hạn mức trong ngày, hệ thống chặn gửi yêu cầu, hiển thị thông báo đạt ngưỡng hạn mức và gợi ý nâng cấp tài khoản hoặc quay lại vào ngày hôm sau (FR-10, BR-01, BR-02).
  - *EF-36.2 (Lỗi kết nối Gemini hoặc phản hồi quá 8 giây):* Nếu dịch vụ AI gặp sự cố hoặc timeout vượt quá 8 giây (NFR-04), hệ thống hủy yêu cầu, không trừ hạn mức AI của Member và thông báo lỗi kỹ thuật thân thiện (BR-04).
  - *EF-36.3 (AI sinh công thức ngoài danh mục):* Nếu phản hồi của AI chứa ID không nằm trong Candidate Pool đã gửi, hệ thống tự động lọc bỏ các mục không hợp lệ trước khi hiển thị cho Member.

#### 5. Hậu điều kiện (Postconditions)
- Khi Member xác nhận lưu, các món ăn từ công thức công khai được thêm vào các bữa ăn trong Lịch ăn tuần của Member.
- Hạn mức AI trong ngày của Member giảm đi 1 lượt.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member đã đăng nhập mới có quyền yêu cầu tạo thực đơn AI. Guest không có quyền này (BR-05).
- **Ràng buộc nghiệp vụ:** AI chỉ đóng vai trò ghép nối công thức có sẵn; hệ thống cấm tuyệt đối việc tạo công thức ảo hoặc ghi đè số liệu dinh dưỡng không qua kiểm chứng (BR-38, BR-40, BR-41).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Hạn mức text AI cho tài khoản Free.
  - [BR-02](BUSINESS-RULES.md#br-02): Hạn mức text AI cho gói Plus và Pro.
  - [BR-03](BUSINESS-RULES.md#br-03): Điều kiện trừ hạn mức AI.
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider và timeout AI.
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-30](BUSINESS-RULES.md#br-30): Ranh giới chức năng khi bỏ qua Onboarding.
  - [BR-31](BUSINESS-RULES.md#br-31): Không gọi AI và không trừ hạn mức khi thiếu hồ sơ.
  - [BR-35](BUSINESS-RULES.md#br-35): Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn.
  - [BR-36](BUSINESS-RULES.md#br-36): Quy tắc 3 loại bữa ăn cố định trong Lịch ăn MVP.
  - [BR-37](BUSINESS-RULES.md#br-37): Tính duy nhất của công thức trong cùng một bữa ăn ngày.
  - [BR-38](BUSINESS-RULES.md#br-38): AI không tự tạo công thức mới ngoài hệ thống.
  - [BR-40](BUSINESS-RULES.md#br-40): Điều kiện dữ liệu dinh dưỡng tin cậy cho AI menu.
  - [BR-41](BUSINESS-RULES.md#br-41): Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia.
  - [BR-50](BUSINESS-RULES.md#br-50): Cho phép công khai bài chứa nguyên liệu ngoài danh mục dinh dưỡng (nhưng bị loại khỏi pool AI menu).
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-04](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-04): Thời gian tạo thực đơn tuần theo hồ sơ $\le 8$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân và chỉ số sức khỏe.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-20](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20): Cơ chế bảo vệ dữ liệu sức khỏe và ranh giới thông tin.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-36.1 (Chỉ chọn công thức từ pool tin cậy):**
  - *Given* Member yêu cầu AI tạo thực đơn theo dinh dưỡng,
  - *When* hệ thống tổng hợp tập hợp ứng viên gửi cho AI,
  - *Then* tất cả các công thức trong tập hợp đều phải có trạng thái Public và 100% nguyên liệu đã được định lượng chuẩn hóa kèm dữ liệu dinh dưỡng đầy đủ (không chứa nguyên liệu chưa hỗ trợ tính dinh dưỡng).
- **AC-36.2 (Cấm tự bịa công thức ngoài hệ thống):**
  - *Given* AI phản hồi kết quả thực đơn,
  - *When* hệ thống kiểm tra các món ăn được đề xuất,
  - *Then* tất cả các món trong thực đơn phải khớp với ID công thức đang công khai hợp lệ trong hệ thống; mọi món không tồn tại trong kho nội bộ đều bị loại bỏ ngay lập tức.
- **AC-36.3 (Tôn trọng dị ứng và trường phái ăn chay):**
  - *Given* hồ sơ Member có khai báo trường phái Lacto-ovo vegetarian và dị ứng đậu phộng,
  - *When* AI tạo thực đơn tuần,
  - *Then* 100% các món được gợi ý phải tuân thủ chuẩn Lacto-ovo và hoàn toàn không chứa thành phần đậu phộng.
- **AC-36.4 (Xác nhận của Member trước khi ghi đè/lưu Lịch ăn):**
  - *Given* AI đã sinh bản xem trước thực đơn thành công,
  - *When* Member chưa nhấn nút "Lưu vào Lịch ăn",
  - *Then* hệ thống không tự động thay đổi bất kỳ mục nào trong Lịch ăn tuần hiện tại của Member.
- **AC-36.5 (Trừ hạn mức AI chính xác):**
  - *Given* Member còn 10 lượt sử dụng AI trong ngày,
  - *When* yêu cầu tạo thực đơn AI hoàn thành và hiển thị kết quả thành công,
  - *Then* số dư hạn mức AI của Member giảm đi 1 lượt (còn 9 lượt).
- **AC-36.6 (Không trừ hạn mức khi xảy ra sự cố kỹ thuật):**
  - *Given* Member gửi yêu cầu tạo thực đơn AI,
  - *When* dịch vụ AI gặp lỗi kết nối hoặc thời gian phản hồi vượt quá 8 giây,
  - *Then* hệ thống thông báo lỗi tới người dùng và số dư hạn mức AI của Member được giữ nguyên không bị trừ.

---

<a id="fr-37"></a>
### FR-37 — Khai báo khẩu phần trong Lịch ăn và kiểm tra menu ngày theo 9 chỉ tiêu

- **Mã yêu cầu:** FR-37
- **Module:** M05, M06, M10
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp chức năng cho phép Member tùy chỉnh số khẩu phần ăn cho từng món trong Lịch ăn ngày (Sáng, Trưa, Tối); hệ thống tự động tính toán tổng dinh dưỡng của cả ngày theo 9 chỉ tiêu cốt lõi dựa trên định lượng nguyên liệu và số khẩu phần; so sánh với mức nhu cầu tham khảo cá nhân (nếu đã khai báo hồ sơ) để hiển thị trạng thái đạt, chưa đạt hoặc vượt mức kèm chênh lệch định lượng; hiển thị cảnh báo minh bạch nếu có món ăn chứa nguyên liệu chưa có dữ liệu dinh dưỡng; nghiêm cấm hiển thị điểm tổng hợp (health score) hoặc nhãn phán xét đơn giản hóa; tùy chọn cho phép AI giải thích chênh lệch và đề xuất món thay thế từ kho công thức có sẵn.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập và quản lý Lịch ăn cá nhân (FR-09, FR-33).
  - Không áp dụng: Guest không có quyền lưu lịch ăn (BR-05, BR-32).
- **Phân loại Actor:**
  - Primary Actor: `Member` (đã đăng nhập).
  - Supporting Actor: `Hệ thống tính toán dinh dưỡng nội bộ`, `Google Gemini AI` (tùy chọn khi Member yêu cầu giải thích).

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-37.1`: Khai báo và điều chỉnh số lượng khẩu phần cho từng món ăn trong Lịch ăn ngày.
  - `UC-37.2`: Xem bảng tổng hợp dinh dưỡng ngày theo 9 chỉ tiêu cốt lõi so sánh với mức tham khảo cá nhân.
  - `UC-37.3`: Xem cảnh báo chi tiết các món ăn bị thiếu dữ liệu dinh dưỡng trong ngày.
  - `UC-37.4`: Yêu cầu AI phân tích chênh lệch dinh dưỡng và gợi ý món điều chỉnh từ kho công thức có sẵn.
- **User Stories:**
  - *Là một Member đang theo dõi chế độ ăn uống*, tôi muốn điều chỉnh số khẩu phần ăn thực tế và kiểm tra tổng dinh dưỡng trong ngày so với mức khuyến nghị tham khảo, để tôi biết ngày hôm đó đã cân bằng năng lượng, đạm và vi chất hay chưa.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Member đã đăng nhập tài khoản hợp lệ (FR-03).
  - Ngày được chọn trong Lịch ăn tuần có ít nhất một món ăn được xếp vào bữa (Sáng, Trưa hoặc Tối) (FR-33).
- **Kích hoạt (Trigger):**
  - Member bấm chọn tính năng "Kiểm tra dinh dưỡng ngày" trên giao diện Lịch ăn tuần.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member mở xem chi tiết một ngày trong Lịch ăn tuần.
  - Bước 2: Tại mỗi món ăn đã xếp vào các bữa (Sáng, Trưa, Tối), hệ thống hiển thị số khẩu phần mặc định là 1. Member có thể điều chỉnh số khẩu phần (từ 0.5 đến 10.0, bước nhảy 0.5).
  - Bước 3: Member nhấn nút "Kiểm tra dinh dưỡng ngày".
  - Bước 4: Hệ thống duyệt qua tất cả các món ăn trong 3 bữa của ngày đó:
    - Với mỗi món, hệ thống lấy dữ liệu 9 chỉ tiêu trên 1 khẩu phần đã tính toán theo công thức chuẩn (FR-39).
    - Nhân giá trị dinh dưỡng của từng chỉ tiêu với số khẩu phần Member đã khai báo cho món đó (BR-47).
    - Nếu món có nguyên liệu chưa hỗ trợ tính dinh dưỡng, hệ thống ghi nhận cờ cảnh báo thiếu dữ liệu cho món đó (BR-48).
  - Bước 5: Hệ thống cộng dồn tổng giá trị của từng chỉ tiêu trong 9 chỉ tiêu cho toàn bộ các bữa ăn trong ngày:
    $$\text{Tổng ngày}(i) = \sum_{\text{món}} \left(\text{Chỉ tiêu}_i \text{ trên 1 khẩu phần} \times \text{Số khẩu phần}
ight)$$
  - Bước 6: Nếu Member đã có hồ sơ dinh dưỡng tham khảo (FR-35), hệ thống so sánh Tổng ngày với Mức nhu cầu tham khảo cá nhân:
    - Tính chênh lệch định lượng ($\Delta = \text{Tổng ngày} - \text{Mức tham khảo}$).
    - Hiển thị thanh tiến trình trực quan biểu thị tỷ lệ % đạt được theo từng chỉ tiêu riêng biệt.
    - Tuyệt đối không tạo ra một "điểm số sức khỏe" (health score) tổng hợp hay gán nhãn "lành mạnh / không lành mạnh" đơn giản hóa (BR-45).
    - Với Natri và Năng lượng, hệ thống giải thích rõ ràng ngưỡng khuyến nghị tối đa theo hướng dẫn dinh dưỡng (BR-44).
  - Bước 7: Nếu có món ăn chứa nguyên liệu chưa có dữ liệu dinh dưỡng, hệ thống hiển thị thông báo cảnh báo màu vàng nổi bật: *"Một số món trong ngày chưa có dữ liệu dinh dưỡng đầy đủ; số liệu thực tế có thể cao hơn bảng tính toán"* kèm danh sách tên các món bị ảnh hưởng (BR-48).
  - Bước 8: Hệ thống hiển thị Tuyên bố từ chối trách nhiệm y tế (BR-39, BR-41).
- **Luồng thay thế (Alternative Flows):**
  - *AF-37.1 (Chưa khai báo hồ sơ dinh dưỡng cá nhân):* Nếu Member chưa khai báo hồ sơ dinh dưỡng theo FR-35, hệ thống vẫn hiển thị tổng 9 chỉ tiêu dinh dưỡng thực tế của cả ngày, nhưng ở cột so sánh sẽ hiển thị giá trị khuyến nghị tiêu chuẩn của người trưởng thành theo USDA kèm liên kết gợi ý: *"Khai báo hồ sơ để xem chỉ số phù hợp với thể trạng cá nhân"*.
  - *AF-37.2 (Member nhờ AI phân tích chênh lệch):* Member bấm nút "Nhờ AI phân tích và đề xuất điều chỉnh". Hệ thống kiểm tra hạn mức AI (BR-01, BR-02, BR-03). Nếu hợp lệ, hệ thống gửi bảng số liệu chênh lệch cho Gemini kèm yêu cầu đề xuất đổi một món trong bữa sang một công thức khác có sẵn trong hệ thống để cân bằng dinh dưỡng. AI trả về đề xuất; nếu Member đồng ý, hệ thống đổi món trong Lịch ăn.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-37.1 (Ngày không có món ăn nào):* Nếu cả 3 bữa trong ngày đều trống, nút "Kiểm tra dinh dưỡng ngày" bị vô hiệu hóa kèm thông báo *"Vui lòng thêm ít nhất một món ăn vào lịch ăn để kiểm tra dinh dưỡng"*.
  - *EF-37.2 (Khẩu phần không hợp lệ):* Nếu người dùng nhập số khẩu phần nhỏ hơn 0.5 hoặc lớn hơn 10, hệ thống báo lỗi và khôi phục về giá trị hợp lệ gần nhất.
  - *SF-37.1 (Kiểm soát quyền riêng tư):* Lịch ăn và kết quả kiểm tra dinh dưỡng thuộc quyền riêng tư của chính Member; tài khoản khác hoặc Guest không thể xem (RBAC theo NFR-08, NFR-09).

#### 5. Hậu điều kiện (Postconditions)
- Số khẩu phần đã điều chỉnh được lưu vào Lịch ăn ngày của Member.
- Kết quả kiểm tra 9 chỉ tiêu được hiển thị chi tiết, minh bạch đến từng chỉ tiêu thành phần.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chỉ Member sở hữu Lịch ăn mới có quyền điều chỉnh khẩu phần và xem bảng kiểm tra dinh dưỡng.
- **Ràng buộc an toàn:** Không được phép tổng hợp thành một chỉ số điểm số duy nhất để đánh giá một ngày ăn (BR-45); luôn gắn kèm cảnh báo khi dữ liệu chưa trọn vẹn (BR-48).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Lịch ăn.
  - [BR-36](BUSINESS-RULES.md#br-36): Quy tắc 3 loại bữa ăn cố định trong Lịch ăn MVP.
  - [BR-37](BUSINESS-RULES.md#br-37): Tính duy nhất của công thức trong cùng một bữa ăn ngày.
  - [BR-39](BUSINESS-RULES.md#br-39): Vai trò tham khảo của chỉ số BMI.
  - [BR-40](BUSINESS-RULES.md#br-40): Điều kiện dữ liệu dinh dưỡng tin cậy cho AI menu.
  - [BR-41](BUSINESS-RULES.md#br-41): Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia.
  - [BR-43](BUSINESS-RULES.md#br-43): Gắn số liệu dinh dưỡng với một khẩu phần.
  - [BR-44](BUSINESS-RULES.md#br-44): Quy tắc giải thích chỉ tiêu natri và năng lượng.
  - [BR-45](BUSINESS-RULES.md#br-45): Cấm hiển thị điểm tổng hợp hoặc nhãn đơn giản hóa.
  - [BR-46](BUSINESS-RULES.md#br-46): Nguồn tính toán dinh dưỡng chính thức của công thức.
  - [BR-47](BUSINESS-RULES.md#br-47): Phân bổ dinh dưỡng theo số khẩu phần và cộng dồn ngày.
  - [BR-48](BUSINESS-RULES.md#br-48): Xử lý nguyên liệu thiếu định lượng hoặc thiếu số liệu dinh dưỡng.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân và chỉ số sức khỏe.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.
  - [NFR-20](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20): Cơ chế bảo vệ dữ liệu sức khỏe cá nhân.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-37.1 (Điều chỉnh khẩu phần ăn hợp lệ):**
  - *Given* Member đang ở giao diện chi tiết ngày trong Lịch ăn,
  - *When* Member nhập số khẩu phần là 1.5 cho món ăn đã chọn,
  - *Then* hệ thống cập nhật giá trị khẩu phần thành 1.5 và tính toán lại định lượng dinh dưỡng tương ứng của món đó.
- **AC-37.2 (Cộng dồn chính xác 9 chỉ tiêu theo khẩu phần):**
  - *Given* thực đơn ngày gồm Món A (1 khẩu phần, 200 kcal, 10g protein) và Món B (2 khẩu phần, mỗi khẩu phần 150 kcal, 5g protein),
  - *When* hệ thống thực hiện kiểm tra dinh dưỡng ngày,
  - *Then* tổng năng lượng của ngày được tính đúng là 500 kcal ($200 \times 1 + 150 \times 2$) và tổng protein là 20g ($10 \times 1 + 5 \times 2$).
- **AC-37.3 (Hiển thị chi tiết từng chỉ tiêu, cấm điểm tổng hợp):**
  - *Given* bảng kết quả kiểm tra dinh dưỡng ngày được kết xuất,
  - *When* hệ thống hiển thị cho người dùng,
  - *Then* hệ thống trình bày chi tiết từng chỉ số riêng biệt trong 9 chỉ tiêu (kèm chênh lệch định lượng so với mức tham khảo) và tuyệt đối không hiển thị một con số điểm tổng hợp sức khỏe (Health Score) hay gán nhãn "ngày ăn lành mạnh / không lành mạnh".
- **AC-37.4 (Cảnh báo minh bạch khi có món thiếu dữ liệu dinh dưỡng):**
  - *Given* thực đơn ngày chứa ít nhất một món ăn có nguyên liệu được đánh dấu "Chưa hỗ trợ tính dinh dưỡng",
  - *When* người dùng xem bảng kết quả kiểm tra dinh dưỡng,
  - *Then* hệ thống hiển thị thông báo cảnh báo rõ ràng nêu rõ số liệu dinh dưỡng của ngày chưa đầy đủ do có món ăn chứa nguyên liệu chưa có dữ liệu chuẩn.
- **AC-37.5 (Không cho AI tự bịa số liệu khi giải thích):**
  - *Given* Member yêu cầu AI phân tích chênh lệch dinh dưỡng của ngày,
  - *When* AI phản hồi phân tích và đề xuất điều chỉnh,
  - *Then* AI chỉ được sử dụng các số liệu thực tế đã tính toán và đề xuất món thay thế từ các công thức có sẵn trong hệ thống, không được tự ý bịa số liệu dinh dưỡng mới.

---

<a id="fr-38"></a>
### FR-38 — Xác nhận phạm vi hỗ trợ trước khi dùng chức năng dinh dưỡng

- **Mã yêu cầu:** FR-38
- **Module:** M02, M10
- **Trạng thái (Derived):** ACTIVE
- **Tóm tắt yêu cầu:** Trước khi truy cập hoặc sử dụng các chức năng dinh dưỡng (hồ sơ dinh dưỡng cá nhân, AI menu theo dinh dưỡng, kiểm tra menu ngày), hệ thống bắt buộc Member xác nhận thuộc phạm vi đối tượng được hỗ trợ trong MVP (từ đủ 18 tuổi, không mang thai/cho con bú, không cần chế độ ăn điều trị bệnh); người không đủ điều kiện được giải thích rõ ranh giới an toàn y tế, bị từ chối chức năng dinh dưỡng nhưng vẫn được sử dụng bình thường toàn bộ các chức năng không-dinh-dưỡng khác.

#### 1. Mục đích
Thiết lập cổng kiểm soát an toàn y tế và sức khỏe bắt buộc trước khi cung cấp các chức năng liên quan đến dữ liệu dinh dưỡng; yêu cầu Member chủ động xác nhận (consent) mình thuộc nhóm đối tượng áp dụng an toàn của MVP; giải thích minh bạch bản chất thông tin dinh dưỡng là ước tính tham khảo lập kế hoạch; đồng thời đảm bảo việc không đủ điều kiện dinh dưỡng không gây phân biệt đối xử hoặc khóa các chức năng thông thường của người dùng.

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member`: Người dùng đã đăng nhập có nhu cầu sử dụng các tính năng dinh dưỡng (thiết lập hồ sơ dinh dưỡng, kiểm tra menu ngày, nhận AI menu theo dinh dưỡng).
- **Secondary Actor / External System:**
  - `Cơ sở dữ liệu tham chiếu dinh dưỡng nội bộ`: Dữ liệu tham khảo dinh dưỡng được biên soạn chuẩn bị từ nguồn USDA FoodData Central và NIH DRI (không gọi runtime API bên ngoài theo BR-49).

#### 3. Tiêu chí đối tượng hỗ trợ hợp lệ trong MVP (Eligible Target Audience)
Theo quy định an toàn tại [BR-42](BUSINESS-RULES.md#br-42), chức năng dinh dưỡng MVP **CHỈ ÁP DỤNG** cho người dùng đồng thời thỏa mãn cả ba điều kiện:
1. **Độ tuổi:** Người trưởng thành từ đủ 18 tuổi trở lên.
2. **Tình trạng thai kỳ & nuôi con:** Không đang trong giai đoạn mang thai hoặc cho con bú.
3. **Tình trạng bệnh lý:** Không mắc các bệnh lý đòi hỏi chế độ ăn điều trị chuyên biệt theo chỉ định của bác sĩ (ví dụ: suy thận mạn tính, đái tháo đường phụ thuộc insulin, bệnh gan giai đoạn nặng, gout cấp tính, suy tim, rối loạn chuyển hóa nặng,...).

#### 4. Ranh giới thông tin y tế và vai trò của chỉ số BMI
- **Ranh giới tham khảo (BR-41):** Mọi số liệu dinh dưỡng, ước tính năng lượng (kcal), 9 chỉ tiêu dưỡng chất và kết quả kiểm tra menu ngày trong hệ thống chỉ là thông tin ước tính hỗ trợ lập kế hoạch ăn uống cá nhân dựa trên các hướng dẫn dinh dưỡng tham khảo của USDA/NIH; hệ thống KHÔNG đưa ra chẩn đoán y khoa, không thay thế bác sĩ chuyên khoa hoặc chuyên gia dinh dưỡng lâm sàng, và không đưa ra chế độ ăn điều trị bệnh.
- **Vai trò tham khảo của BMI (BR-39):** Chỉ số BMI (Body Mass Index) chỉ được tính toán và hiển thị như một chỉ số sàng lọc tham khảo trong hồ sơ cá nhân rộng hơn (gồm tuổi, giới tính, chiều cao, cân nặng, mức vận động); BMI hoặc mục tiêu cân nặng TUYỆT ĐỐI KHÔNG được sử dụng độc lập để tự động sinh ra các mục tiêu calorie hay macro mang tính áp đặt/kê đơn y tế.

#### 5. Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-38.1`: Xác nhận đủ điều kiện và đồng ý sử dụng chức năng dinh dưỡng (Confirm nutrition eligibility and consent). *(Bao gồm luồng rẽ nhánh xử lý khi người dùng không thuộc đối tượng hỗ trợ hoặc từ chối cam kết)*
  - `UC-38.2`: Xem lại tuyên bố miễn trừ y tế và phạm vi hỗ trợ dinh dưỡng (View nutrition disclaimer and eligibility scope).
  - `UC-38.3`: Cập nhật lại trạng thái điều kiện sức khỏe dinh dưỡng (Update nutrition eligibility status).
  *(Ghi chú: Xử lý trường hợp người dùng không thuộc đối tượng hỗ trợ không còn là Use Case độc lập vì đây không phải là mục tiêu chủ động của Actor mà là Luồng rẽ nhánh an toàn thuộc UC-38.1).*
- **User Stories:**
  - `US-38.1`: Là một thành viên muốn theo dõi dinh dưỡng món ăn, tôi muốn thấy rõ điều kiện sử dụng và ranh giới y tế trước khi khai báo dữ liệu sức khỏe để có thể đưa ra quyết định đồng ý phù hợp.
  - `US-38.2`: Là một thành viên đủ điều kiện (từ 18 tuổi, không mang thai/cho con bú, không ăn kiêng bệnh lý), tôi muốn xác nhận nhanh chóng để bắt đầu thiết lập hồ sơ dinh dưỡng và nhận phân tích thực đơn.
  - `US-38.3`: Là một thành viên không thuộc nhóm hỗ trợ (dưới 18 tuổi, đang mang thai, hoặc có bệnh lý điều trị), tôi muốn hệ thống giải thích rõ lý do an toàn sức khỏe và vẫn cho phép tôi sử dụng bình thường các tính năng khác của ứng dụng.
  - `US-38.4`: Là một thành viên có tình trạng sức khỏe thay đổi (ví dụ: bắt đầu mang thai), tôi muốn có thể cập nhật lại xác nhận này để hệ thống tạm dừng các khuyến nghị dinh dưỡng không còn an toàn.

#### 6. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:**
  - Người dùng đã đăng nhập với vai trò Member và tài khoản ở trạng thái `ACTIVE`.
- **Trigger:**
  - Member lần đầu tiên nhấp vào bất kỳ tính năng nào thuộc Module M10: menu "Hồ sơ dinh dưỡng" (FR-35), nút "Kiểm tra dinh dưỡng thực đơn ngày" (FR-37), hoặc yêu cầu "AI lập thực đơn theo nhu cầu dinh dưỡng" (FR-36).
  - Member chủ động truy cập mục Cài đặt dinh dưỡng để cập nhật lại điều kiện sức khỏe.

#### 7. Luồng sự kiện (Flow of Events)

##### A. Luồng Xác nhận đủ điều kiện sử dụng chức năng dinh dưỡng (UC-38.1)
1. **Main Flow (Xác nhận đủ điều kiện thành công):**
   - Bước 1: Member nhấp vào tính năng dinh dưỡng (ví dụ: "Hồ sơ dinh dưỡng").
   - Bước 2: Hệ thống kiểm tra trạng thái xác nhận dinh dưỡng của Member. Nếu chưa xác nhận, hệ thống yêu cầu hiển thị màn hình xác nhận.
   - Bước 3: Giao diện hiển thị màn hình "Xác nhận phạm vi hỗ trợ dinh dưỡng".
   - Bước 4: Màn hình trình bày rõ ràng:
     - Khối 1: Ba điều kiện bắt buộc (Từ đủ 18 tuổi; Không mang thai hoặc đang cho con bú; Không có bệnh lý yêu cầu chế độ ăn điều trị riêng) theo BR-42.
     - Khối 2: Tuyên bố ranh giới thông tin: Dữ liệu mang tính tham khảo lập kế hoạch theo chuẩn USDA/NIH, không phải chẩn đoán hay điều trị y khoa theo BR-41.
     - Khối 3: Hộp kiểm cam kết: *"Tôi xác nhận tôi từ đủ 18 tuổi trở lên, không mang thai/cho con bú, không có nhu cầu ăn kiêng điều trị bệnh, và hiểu rằng các khuyến nghị dinh dưỡng chỉ mang tính chất tham khảo."*
   - Bước 5: Member tích chọn hộp kiểm và nhấn nút "Xác nhận và tiếp tục".
   - Bước 6: Ứng dụng gửi yêu cầu xác nhận lên máy chủ hệ thống.
   - Bước 7: Hệ thống xác thực yêu cầu, lưu trạng thái xác nhận đủ điều kiện kèm mốc thời gian xác nhận vào hồ sơ tài khoản của Member.
   - Bước 8: Hệ thống phản hồi xác nhận thành công.
   - Bước 9: Giao diện chuyển hướng Member sang màn hình Khai báo Hồ sơ dinh dưỡng cá nhân (`FR-35`) để nhập các chỉ số tuổi, giới tính, chiều cao, cân nặng, mức vận động.
2. **Alternative Flow (Người dùng không thuộc đối tượng hỗ trợ hoặc từ chối cam kết):**
   - Bước 1: Tại màn hình xác nhận, Member tích chọn: *"Tôi không thuộc nhóm đối tượng trên"* (hoặc chọn "Hủy bỏ / Quay lại").
   - Bước 2: Hệ thống ghi nhận trạng thái chưa đủ điều kiện dinh dưỡng đối với tài khoản.
   - Bước 3: Hệ thống hiển thị thông báo giải thích rõ ràng và lịch sự:
     - *"Rất tiếc! Hệ thống Vegetarian Support Application hiện chỉ cung cấp tính toán dinh dưỡng mẫu cho người trưởng thành khỏe mạnh bình thường. Đối với người dưới 18 tuổi, phụ nữ mang thai/cho con bú hoặc người có bệnh lý nền, nhu cầu dinh dưỡng đòi hỏi phác đồ chuyên biệt từ bác sĩ chuyên khoa hoặc chuyên gia dinh dưỡng lâm sàng. Để đảm bảo an toàn tuyệt đối cho sức khỏe của bạn, hệ thống xin phép tạm dừng chức năng dinh dưỡng đối với tài khoản này."*
   - Bước 4: Hệ thống khóa quyền truy cập các chức năng dinh dưỡng (FR-35, FR-36, FR-37) đối với tài khoản.
   - Bước 5: Hệ thống điều hướng Member về trang Khám phá bài viết.
   - Bước 6: **Bảo toàn chức năng thông thường (BR-42):** Member VẪN SỬ DỤNG HOÀN TOÀN BÌNH THƯỜNG toàn bộ các chức năng khác của hệ thống: xem bài công thức, tìm kiếm, lọc theo nguyên liệu/loại ăn chay, lưu bài viết vào `Saved Recipes`, xếp lịch ăn 3 bữa thủ công trong `Meal Planner`, tạo và công khai Recipe Post, Like, bình luận và hỏi đáp Chatbot AI kiến thức chay chung trong hạn mức gói.

##### B. Luồng Xem lại tuyên bố miễn trừ y tế và phạm vi hỗ trợ (UC-38.2)
1. **Main Flow:**
   - Bước 1: Member truy cập mục Thông tin dinh dưỡng hoặc nhấp vào liên kết "Phạm vi hỗ trợ & Tuyên bố y tế" tại các trang dinh dưỡng.
   - Bước 2: Hệ thống hiển thị đầy đủ văn bản tuyên bố ranh giới thông tin tham khảo, cơ sở dữ liệu nguồn USDA/NIH, và 3 điều kiện áp dụng an toàn của hệ thống.

##### C. Luồng Cập nhật lại trạng thái điều kiện sức khỏe (UC-38.3)
1. **Main Flow:**
   - Bước 1: Member đã từng xác nhận đủ điều kiện trước đây, nay tình trạng sức khỏe thay đổi (ví dụ: đang mang thai hoặc phát hiện bệnh lý cần ăn kiêng).
   - Bước 2: Member vào mục "Cài đặt dinh dưỡng", chọn "Cập nhật điều kiện sức khỏe".
   - Bước 3: Member chuyển trạng thái sang không đủ điều kiện và xác nhận lưu.
   - Bước 4: Hệ thống ghi nhận trạng thái không đủ điều kiện dinh dưỡng đối với tài khoản.
   - Bước 5: Toàn bộ tính năng AI lập menu theo dinh dưỡng (FR-36) và kiểm tra 9 chỉ tiêu menu ngày (FR-37) lập tức dừng cung cấp cho tài khoản này; dữ liệu dinh dưỡng trước đó không bị xóa mất nhưng được đặt ở trạng thái không đánh giá; các chức năng không-dinh-dưỡng tiếp tục hoạt động bình thường.

##### D. Luồng An ninh — Kiểm soát truy cập chức năng dinh dưỡng ở tầng máy chủ
1. **Main Flow (Kiểm soát chặt chẽ phía máy chủ):**
   - Đối với mọi yêu cầu liên quan đến tính toán, phân tích hay lập kế hoạch dinh dưỡng, máy chủ bắt buộc kiểm tra cờ xác nhận đủ điều kiện của tài khoản.
   - Nếu cờ xác nhận chưa được thiết lập hoặc có giá trị không đủ điều kiện: máy chủ lập tức từ chối xử lý yêu cầu và phản hồi yêu cầu xác nhận điều kiện dinh dưỡng trước.

#### 8. Hậu điều kiện (Postconditions)
- Khi xác nhận đủ điều kiện thành công: Trạng thái xác nhận đủ điều kiện dinh dưỡng được lưu trữ gắn liền với tài khoản Member; mở khóa quyền truy cập vào màn hình khai báo Hồ sơ dinh dưỡng (`FR-35`).
- Khi từ chối hoặc không đủ điều kiện: Ghi nhận trạng thái chưa đủ điều kiện; khóa toàn bộ chức năng thuộc Module M10; toàn bộ các tính năng không-dinh-dưỡng (M01–M06, M08–M09) vẫn hoạt động nguyên vẹn.

#### 9. Quy tắc phân quyền và bảo mật (Permissions & Security)
- Khách chưa đăng nhập (Guest) không có quyền truy cập chức năng dinh dưỡng (BR-05).
- Việc xác nhận đủ điều kiện là bắt buộc đối với mọi Member trước khi hệ thống lưu trữ hoặc xử lý bất kỳ dữ liệu nhân trắc học hay phân tích dinh dưỡng nào (NFR-20).
- Máy chủ bắt buộc kiểm tra điều kiện xác nhận dinh dưỡng ở tầng nghiệp vụ đối với toàn bộ các chức năng thuộc Module M10. Nếu chưa xác nhận, từ chối xử lý và yêu cầu hoàn thành xác nhận.
- Ràng buộc kỹ thuật được phê duyệt: Dữ liệu dinh dưỡng dựa trên cơ sở dữ liệu tham chiếu nội bộ chuẩn bị từ nguồn USDA FoodData Central và NIH DRI; tuyệt đối không gọi runtime API bên thứ ba khi tính toán dinh dưỡng (BR-49).

#### 10. Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-39](BUSINESS-RULES.md#br-39): Vai trò tham khảo của chỉ số BMI (không tự kê calorie/macro từ BMI hoặc mục tiêu cân nặng).
  - [BR-41](BUSINESS-RULES.md#br-41): Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia y tế.
  - [BR-42](BUSINESS-RULES.md#br-42): Đối tượng loại trừ khỏi tính toán nhu cầu dinh dưỡng MVP (người dưới 18 tuổi, mang thai/cho con bú, bệnh lý điều trị; không khóa chức năng thông thường).
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân & chỉ số sức khỏe, HTTPS/TLS 1.2+, kiểm tra quyền sở hữu.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.
  - [NFR-20](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20): Cơ chế đồng ý (Consent) trước khi thu thập/xử lý dữ liệu sức khỏe, ranh giới thông tin rõ ràng.

#### 11. Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-38.1 — Bắt buộc hiển thị xác nhận khi truy cập chức năng dinh dưỡng lần đầu:**
  - **Given:** Member đã đăng nhập nhưng chưa từng thực hiện xác nhận phạm vi hỗ trợ dinh dưỡng.
  - **When:** Member nhấn vào tính năng "Hồ sơ dinh dưỡng" hoặc "Kiểm tra dinh dưỡng thực đơn ngày".
  - **Then:** Hệ thống không cho phép truy cập trực tiếp vào màn hình chỉ số, mà bắt buộc hiển thị màn hình "Xác nhận phạm vi hỗ trợ dinh dưỡng" với đầy đủ 3 điều kiện loại trừ và tuyên bố ranh giới y tế tham khảo (BR-41, BR-42).

- **AC-38.2 — Xác nhận đủ điều kiện thành công mở khóa tính năng dinh dưỡng:**
  - **Given:** Member đang ở màn hình xác nhận phạm vi hỗ trợ dinh dưỡng.
  - **When:** Member tích chọn ô cam kết đáp ứng đủ 3 tiêu chí (từ 18 tuổi, không mang thai/cho con bú, không ăn kiêng bệnh lý) và nhấn "Xác nhận và tiếp tục".
  - **Then:** Hệ thống lưu trạng thái xác nhận đủ điều kiện thành công kèm thời điểm xác nhận, phản hồi thành công, và giao diện chuyển tiếp Member sang màn hình Khai báo Hồ sơ dinh dưỡng cá nhân (FR-35).

- **AC-38.3 — Xử lý an toàn khi người dùng xác nhận không đủ điều kiện:**
  - **Given:** Member đang ở màn hình xác nhận phạm vi hỗ trợ dinh dưỡng.
  - **When:** Member chọn "Tôi không thuộc nhóm đối tượng trên" hoặc từ chối cam kết.
  - **Then:** Hệ thống hiển thị thông báo từ chối lịch sự nêu rõ lý do an toàn y tế, khóa quyền truy cập chức năng dinh dưỡng, và điều hướng Member về trang Khám phá bài viết.

- **AC-38.4 — Bảo toàn 100% tính năng không-dinh-dưỡng cho người không đủ điều kiện:**
  - **Given:** Member đã được ghi nhận trạng thái không đủ điều kiện dinh dưỡng.
  - **When:** Member thực hiện các thao tác: tìm kiếm món chay, xem chi tiết bài công thức, lưu công thức vào `Saved Recipes`, thêm công thức vào Lịch ăn thủ công, tạo và công khai Recipe Post, Like và bình luận.
  - **Then:** Toàn bộ các thao tác trên được hệ thống xử lý thành công 100% mà không gặp bất kỳ thông báo lỗi hay sự hạn chế nào liên quan đến dinh dưỡng (BR-42).

- **AC-38.5 — Chặn ở tầng kiểm soát máy chủ đối với các chức năng dinh dưỡng khi chưa xác nhận:**
  - **Given:** Member chưa thực hiện xác nhận phạm vi hỗ trợ dinh dưỡng.
  - **When:** Ứng dụng của Member (hoặc công cụ gửi yêu cầu trực tiếp) gửi yêu cầu truy xuất hồ sơ dinh dưỡng hoặc kiểm tra dinh dưỡng thực đơn ngày.
  - **Then:** Máy chủ kiểm tra điều kiện, từ chối xử lý yêu cầu và phản hồi thông báo yêu cầu xác nhận điều kiện dinh dưỡng trước.

- **AC-38.6 — Cập nhật điều kiện sức khỏe sang không đủ điều kiện tạm dừng tính năng dinh dưỡng:**
  - **Given:** Member đã từng xác nhận đủ điều kiện dinh dưỡng trước đây.
  - **When:** Member vào Cài đặt dinh dưỡng cập nhật trạng thái sức khỏe sang không đủ điều kiện (ví dụ: đang mang thai) và lưu thay đổi.
  - **Then:** Hệ thống ghi nhận trạng thái mới, lập tức tạm dừng các tính năng gợi ý thực đơn dinh dưỡng và kiểm tra 9 chỉ tiêu ngày đối với tài khoản, đồng thời bảo toàn nguyên vẹn dữ liệu lịch sử đã lưu trước đó.

- **AC-38.7 — Khẳng định ranh giới tham khảo và không tự kê đơn y tế từ BMI:**
  - **Given:** Member đã hoàn tất xác nhận FR-38 và nhập chiều cao, cân nặng tại FR-35 có kết quả BMI tương ứng mức thừa cân.
  - **When:** Hệ thống tính toán và hiển thị kết quả.
  - **Then:** Giao diện hiển thị chỉ số BMI kèm nhãn tham khảo; hệ thống tuyệt đối KHÔNG tự động đưa ra các con số calorie hay macro mang tính áp đặt/kê đơn điều trị y tế (BR-39).

---

<a id="fr-39"></a>
### FR-39 — Tính toán ước tính 9 chỉ tiêu dinh dưỡng cho công thức

- **Mã yêu cầu:** FR-39
- **Module:** M03, M04, M10
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống tự động tính toán tổng ước tính của 9 chỉ tiêu dinh dưỡng cốt lõi cho một bài công thức nấu ăn bằng cách đối chiếu và cộng dồn dữ liệu từ danh mục nguyên liệu dinh dưỡng nội bộ (tham chiếu USDA/NIH) theo trọng lượng gram thực tế của từng nguyên liệu; sau đó quy đổi ra định lượng trên 1 khẩu phần dựa trên số khẩu phần mà tác giả đã khai báo; khi có nguyên liệu thiếu định lượng hoặc chưa có trong danh mục dinh dưỡng nội bộ, hệ thống phải công khai rõ ràng phạm vi thiếu dữ liệu, tuyệt đối không được tự gán giá trị bằng 0 và không cho phép AI tự suy đoán số liệu; không gọi API dinh dưỡng ngoài theo thời gian thực (realtime external API).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Tất cả các bài Recipe Post được tạo hoặc chỉnh sửa trong hệ thống (FR-04, FR-07, FR-16, FR-19, FR-44).
  - 9 chỉ tiêu cốt lõi: Năng lượng (kcal), Chất đạm (g), Carbohydrate (g), Chất béo (g), Chất xơ (g), Canxi (mg), Sắt (mg), Vitamin B12 (mcg), Kẽm (mg) (SRS 3.18, BR-40, BR-46).
- **Phân loại Actor:**
  - Primary Actor: `Member / Tác giả bài viết` (khi tạo/sửa công thức), `Guest / Member` (khi xem chi tiết công thức).
  - Supporting Actor: `Cơ sở dữ liệu dinh dưỡng nội bộ` (lưu trữ 9 chỉ số chuẩn trên 100g).

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-39.1`: Xem bảng ước tính 9 chỉ tiêu dinh dưỡng của công thức (tính trên 1 khẩu phần và trên toàn bộ công thức).
  - `UC-39.2`: Xem cảnh báo danh sách các nguyên liệu chưa hỗ trợ tính dinh dưỡng trong công thức (nếu có).
- **User Stories:**
  - *Là một người tìm kiếm công thức nấu ăn chay*, tôi muốn biết rõ lượng dinh dưỡng ước tính trong mỗi phần ăn để cân đối khẩu phần cho bản thân, đồng thời biết rõ nguyên liệu nào chưa có dữ liệu dinh dưỡng để không bị lầm tưởng con số là chính xác tuyệt đối 100%.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Bài công thức đã khai báo số khẩu phần (servings $\ge 1$) và có ít nhất một nguyên liệu hợp lệ (FR-16, FR-19).
- **Kích hoạt (Trigger):**
  - Tác giả nhấn lưu bài công thức, hoặc người dùng truy cập trang xem chi tiết Recipe Post (FR-18).

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Hệ thống đọc danh sách nguyên liệu của bài công thức kèm số lượng và đơn vị đo chuẩn (gram/ml theo BR-49).
  - Bước 2: Hệ thống truy vấn danh mục nguyên liệu dinh dưỡng nội bộ để ánh xạ từng nguyên liệu với bản ghi dinh dưỡng tương ứng (BR-46).
  - Bước 3: Với mỗi nguyên liệu đã được ánh xạ thành công, hệ thống tính toán giá trị của từng chỉ tiêu trong 9 chỉ tiêu theo trọng lượng thực tế:
    $$\text{Giá trị chỉ tiêu}_i = \frac{\text{Trọng lượng (g)}}{100} \times \text{Chỉ số chuẩn trên 100g}_i$$
  - Bước 4: Hệ thống cộng tổng giá trị của từng chỉ tiêu cho tất cả các nguyên liệu đã ánh xạ thành công để ra Tổng dinh dưỡng toàn bài công thức.
  - Bước 5: Hệ thống chia tổng dinh dưỡng của toàn bộ công thức cho số khẩu phần (servings) đã khai báo để tính ra Dinh dưỡng ước tính trên 1 khẩu phần (BR-43):
    $$\text{Dinh dưỡng trên 1 khẩu phần}_i = \frac{\text{Tổng dinh dưỡng}_i}{\text{Số khẩu phần}}$$
  - Bước 6: Nếu 100% nguyên liệu đều có dữ liệu dinh dưỡng đầy đủ, hệ thống đánh dấu trạng thái dinh dưỡng của công thức là `Đầy đủ` (Eligible cho pool AI menu theo BR-40).
  - Bước 7: Hệ thống hiển thị bảng dinh dưỡng 9 chỉ tiêu trên trang chi tiết công thức kèm Tuyên bố từ chối trách nhiệm y tế (BR-39, BR-41).
- **Luồng thay thế (Alternative Flows):**
  - *AF-39.1 (Công thức chứa nguyên liệu chưa có dữ liệu dinh dưỡng):* Nếu có một hoặc nhiều nguyên liệu chưa được ánh xạ trong danh mục nội bộ:
    - Hệ thống vẫn tính tổng của các nguyên liệu đã có số liệu.
    - Hệ thống đánh dấu trạng thái dinh dưỡng của công thức là `Chưa đầy đủ` (Incomplete).
    - Trên bảng dinh dưỡng, hệ thống hiển thị nhãn cảnh báo màu vàng: *"Ước tính chưa đầy đủ: công thức có X nguyên liệu chưa hỗ trợ tính dinh dưỡng"* kèm danh sách tên các nguyên liệu đó (BR-48).
    - Hệ thống tuyệt đối không tự gán số liệu dinh dưỡng của nguyên liệu thiếu bằng 0 trong các phép so sánh nghiêm ngặt và không cho AI tự bịa số liệu (BR-48).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-39.1 (Không có nguyên liệu nào có dữ liệu dinh dưỡng):* Nếu toàn bộ nguyên liệu trong bài đều nằm ngoài danh mục nội bộ, hệ thống hiển thị thông báo: *"Công thức chưa hỗ trợ tính toán dinh dưỡng do chưa có dữ liệu nguyên liệu chuẩn"* thay vì hiển thị toàn bộ số 0.
  - *SF-39.1 (Cấm gọi API ngoài realtime):* Hệ thống xử lý tính toán 100% dựa trên cơ sở dữ liệu nội bộ đã được Administrator phê duyệt (BR-49, BR-51); không thực hiện cuộc gọi API ra ngoài mạng Internet trong lúc người dùng xem bài viết nhằm bảo vệ hiệu năng và tính ổn định (NFR-02, NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Bảng ước tính 9 chỉ tiêu dinh dưỡng của công thức được lưu kèm hoặc tính toán sẵn sàng hiển thị.
- Trạng thái dinh dưỡng (`Đầy đủ` hoặc `Chưa đầy đủ`) được xác định chính xác để phục vụ bộ lọc tìm kiếm (FR-08) và pool AI lập menu (FR-36).

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Mọi người dùng (Guest, Member, Admin) đều có quyền xem bảng dinh dưỡng ước tính trên các công thức công khai.
- **Ràng buộc nghiệp vụ:** Không hiển thị điểm số tổng hợp (BR-45); luôn ghi chú rõ ràng nguồn gốc dữ liệu tham khảo USDA/NIH và từ chối trách nhiệm y tế (BR-39, BR-41).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-39](BUSINESS-RULES.md#br-39): Vai trò tham khảo của chỉ số BMI.
  - [BR-40](BUSINESS-RULES.md#br-40): Điều kiện dữ liệu dinh dưỡng tin cậy cho AI menu.
  - [BR-41](BUSINESS-RULES.md#br-41): Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia.
  - [BR-43](BUSINESS-RULES.md#br-43): Gắn số liệu dinh dưỡng với một khẩu phần.
  - [BR-44](BUSINESS-RULES.md#br-44): Quy tắc giải thích chỉ tiêu natri và năng lượng.
  - [BR-45](BUSINESS-RULES.md#br-45): Cấm hiển thị điểm tổng hợp hoặc nhãn đơn giản hóa.
  - [BR-46](BUSINESS-RULES.md#br-46): Nguồn tính toán dinh dưỡng chính thức của công thức.
  - [BR-48](BUSINESS-RULES.md#br-48): Xử lý nguyên liệu thiếu định lượng hoặc thiếu số liệu dinh dưỡng.
  - [BR-49](BUSINESS-RULES.md#br-49): Không gọi API dinh dưỡng ngoài realtime và nguồn tham khảo.
  - [BR-50](BUSINESS-RULES.md#br-50): Cho phép công khai bài chứa nguyên liệu ngoài danh mục dinh dưỡng.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian tải trang hiển thị chi tiết bài viết $\le 2$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Bảo vệ hệ thống khỏi lỗ hổng (không gọi API ngoài không kiểm soát).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-39.1 (Tính toán chính xác theo gram nguyên liệu và khẩu phần):**
  - *Given* công thức có khai báo 2 khẩu phần và nguyên liệu gồm 200g Đậu phụ mơ (có 8g protein / 100g trong danh mục nội bộ),
  - *When* hệ thống thực hiện tính toán dinh dưỡng cho công thức,
  - *Then* lượng protein trên 1 khẩu phần được hiển thị chính xác là 8g ($\frac{200 \times 8}{100} \div 2$).
- **AC-39.2 (Đầy đủ 9 chỉ tiêu cốt lõi):**
  - *Given* công thức có các nguyên liệu đã được ánh xạ đầy đủ,
  - *When* người dùng xem bảng dinh dưỡng công thức,
  - *Then* hệ thống hiển thị chính xác và đầy đủ 9 chỉ tiêu: Năng lượng (kcal), Chất đạm (g), Carbohydrate (g), Chất béo (g), Chất xơ (g), Canxi (mg), Sắt (mg), Vitamin B12 (mcg), Kẽm (mg).
- **AC-39.3 (Minh bạch khi có nguyên liệu thiếu số liệu):**
  - *Given* công thức chứa một nguyên liệu tự do không có trong danh mục dinh dưỡng nội bộ,
  - *When* bảng dinh dưỡng công thức được hiển thị,
  - *Then* hệ thống gắn nhãn cảnh báo "Ước tính chưa đầy đủ", liệt kê rõ tên nguyên liệu chưa hỗ trợ tính toán và không được gán ngầm giá trị của nguyên liệu đó bằng 0.
- **AC-39.4 (Cấm hiển thị điểm tổng hợp sức khỏe):**
  - *Given* kết quả tính toán dinh dưỡng công thức được kết xuất,
  - *When* hiển thị trên giao diện người dùng,
  - *Then* hệ thống tuyệt đối không hiển thị bất kỳ nhãn điểm tổng hợp (như "Điểm dinh dưỡng: 85/100") hay nhãn phân loại đơn giản ("Tốt / Xấu").
- **AC-39.5 (Hoạt động hoàn toàn bằng dữ liệu nội bộ, không gọi API ngoài):**
  - *Given* người dùng truy cập xem chi tiết công thức,
  - *When* hệ thống tải và tính toán dữ liệu dinh dưỡng,
  - *Then* toàn bộ dữ liệu được truy xuất trực tiếp từ cơ sở dữ liệu nội bộ mà không tạo bất kỳ HTTP request nào ra dịch vụ dinh dưỡng bên ngoài.

---

<a id="fr-40"></a>
### FR-40 — Công khai Recipe Post chứa nguyên liệu ngoài danh mục dinh dưỡng

- **Mã yêu cầu:** FR-40
- **Module:** M03, M04, M06, M10
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cho phép Member đã đăng nhập công khai trực tiếp bài công thức nấu ăn (Recipe Post) ngay cả khi bài viết có chứa các nguyên liệu tự do nằm ngoài danh mục nguyên liệu dinh dưỡng nội bộ của hệ thống; hệ thống không được chặn quyền xuất bản chỉ vì lý do nguyên liệu chưa có dữ liệu dinh dưỡng; các nguyên liệu này được gắn cờ hiển thị "Chưa hỗ trợ tính dinh dưỡng" trên giao diện bài viết; bài công thức này được hiển thị rõ cảnh báo kết quả dinh dưỡng chưa đầy đủ và bị loại trừ khỏi tập hợp ứng viên cho chức năng AI lập thực đơn theo nhu cầu dinh dưỡng (FR-36).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member tạo và xuất bản công thức trực tiếp (FR-04, FR-25).
  - Phân định ranh giới: Xuất bản bài viết cộng đồng không bị phụ thuộc vào tiến độ cập nhật danh mục dinh dưỡng của Administrator (BR-07, BR-50).
- **Phân loại Actor:**
  - Primary Actor: `Member` (tác giả bài viết).
  - Secondary Actor: `Guest / Member` (người xem công thức).
  - Supporting Actor: `Hệ thống phân loại và gắn cờ dinh dưỡng`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-40.1`: Công khai trực tiếp Recipe Post có chứa nguyên liệu ngoài danh mục dinh dưỡng nội bộ.
  - `UC-40.2`: Xem chỉ dẫn và cảnh báo về nguyên liệu chưa hỗ trợ tính dinh dưỡng trên trang chi tiết công thức.
- **User Stories:**
  - *Là một người nấu ăn sáng tạo*, tôi muốn chia sẻ công thức sử dụng một loại gia vị hoặc rau rừng đặc sản địa phương chưa có trong danh mục chuẩn, để công thức của tôi vẫn được chia sẻ cho cộng đồng mà không bị hệ thống từ chối xuất bản.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Tác giả là Member đã đăng nhập và hoàn thành các trường thông tin bắt buộc của bài công thức (FR-04, FR-16, FR-19).
  - Bài công thức có ít nhất một nguyên liệu mà tác giả nhập dạng chữ tự do chưa khớp với danh mục dinh dưỡng nội bộ.
- **Kích hoạt (Trigger):**
  - Tác giả nhấn nút "Đăng công thức" (Publish Recipe).

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả hoàn thành soạn thảo công thức; trong danh sách nguyên liệu có một số nguyên liệu tự do không tìm thấy trong danh mục dinh dưỡng nội bộ.
  - Bước 2: Tác giả nhấn "Đăng công thức".
  - Bước 3: Hệ thống thực hiện quy trình kiểm tra hợp lệ thông tin bài viết theo bộ quy tắc Recipe Validation Profile chuẩn của FR-16 (tiêu đề, khẩu phần, thời gian chuẩn bị và nấu, loại ăn chay; nguyên liệu theo FR-19; các bước hướng dẫn chuẩn bị/chế biến bắt buộc từ 1 đến 30 bước theo FR-22, BR-19).
  - Bước 4: Kiểm tra hợp lệ thành công; hệ thống xác định có nguyên liệu chưa ánh xạ được với bảng dinh dưỡng nội bộ.
  - Bước 5: Hệ thống cho phép công khai trực tiếp bài viết lên nền tảng ngay lập tức mà không chặn và không đưa vào hàng đợi duyệt trước (BR-07, BR-50, BR-59).
  - Bước 6: Hệ thống đánh dấu trạng thái dinh dưỡng của bài viết là `Chưa đầy đủ` (Incomplete Nutrition Data) và đánh dấu cờ `Loại trừ khỏi AI Menu Dinh dưỡng` (BR-40).
  - Bước 7: Trên trang chi tiết công thức công khai, bên cạnh tên của nguyên liệu tự do đó, hệ thống hiển thị biểu tượng ghi chú nhỏ: *"Chưa hỗ trợ tính dinh dưỡng"* (BR-48).
  - Bước 8: Bảng ước tính dinh dưỡng của bài viết hiển thị giá trị cộng dồn của các nguyên liệu đã biết kèm dòng ghi chú cảnh báo minh bạch (FR-39).
- **Luồng thay thế (Alternative Flows):**
  - *AF-40.1 (Sau này Admin bổ sung nguyên liệu vào danh mục):* Khi Administrator bổ sung nguyên liệu tương ứng vào danh mục nội bộ (FR-41) và liên kết với nguyên liệu của bài viết, hệ thống tự động tính toán lại dinh dưỡng của bài; nếu 100% nguyên liệu đã được hỗ trợ, cờ cảnh báo được gỡ bỏ và bài viết tự động đủ điều kiện tham gia pool AI menu.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-40.1 (Tác giả bỏ trống tên hoặc định lượng nguyên liệu):* Nếu nguyên liệu không có tên hoặc không có định lượng (số lượng + đơn vị đo chuẩn hoặc "vừa đủ"), hệ thống chặn đăng theo quy tắc validation bắt buộc (FR-16, FR-19), không liên quan đến việc nguyên liệu có trong danh mục dinh dưỡng hay không.
  - *SF-40.1 (Ngăn chặn AI đưa bài thiếu số liệu vào menu dinh dưỡng):* Khi module AI Menu (FR-36) truy vấn danh sách công thức để lập thực đơn theo calo/macro, truy vấn cơ sở dữ liệu bắt buộc lọc bỏ các bài viết có cờ `Chưa đầy đủ dinh dưỡng` để đảm bảo an toàn cho người dùng (BR-40).

#### 5. Hậu điều kiện (Postconditions)
- Recipe Post được công khai ngay lập tức cho toàn bộ cộng đồng xem và tương tác.
- Nguyên liệu ngoài danh mục được hiển thị minh bạch trạng thái.
- Bài viết bị loại trừ khỏi pool gợi ý AI Menu theo mục tiêu dinh dưỡng.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member có toàn quyền công khai bài viết của mình mà không cần phê duyệt trước của Admin (BR-07, BR-59).
- **Ràng buộc nghiệp vụ:** Không được vì thiếu dữ liệu dinh dưỡng mà ngăn cản quyền chia sẻ công thức cộng đồng (BR-50).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post.
  - [BR-40](BUSINESS-RULES.md#br-40): Điều kiện dữ liệu dinh dưỡng tin cậy cho AI menu.
  - [BR-46](BUSINESS-RULES.md#br-46): Nguồn tính toán dinh dưỡng chính thức của công thức.
  - [BR-48](BUSINESS-RULES.md#br-48): Xử lý nguyên liệu thiếu định lượng hoặc thiếu số liệu dinh dưỡng.
  - [BR-50](BUSINESS-RULES.md#br-50): Cho phép công khai bài chứa nguyên liệu ngoài danh mục dinh dưỡng.
  - [BR-59](BUSINESS-RULES.md#br-59): Không có hàng đợi duyệt bài trước khi công khai.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-40.1 (Không chặn xuất bản công thức chứa nguyên liệu mới):**
  - *Given* Member đăng bài công thức hợp lệ có chứa nguyên liệu "Nấm tràm Phú Quốc" chưa có trong danh mục dinh dưỡng nội bộ,
  - *When* Member nhấn nút "Đăng công thức",
  - *Then* hệ thống xuất bản bài công thức thành công ở trạng thái công khai ngay lập tức mà không hiển thị lỗi chặn xuất bản.
- **AC-40.2 (Hiển thị nhãn chưa hỗ trợ tính dinh dưỡng cho nguyên liệu):**
  - *Given* bài công thức đã công khai có chứa nguyên liệu ngoài danh mục dinh dưỡng,
  - *When* người dùng xem danh sách nguyên liệu trên trang chi tiết công thức,
  - *Then* cạnh tên nguyên liệu ngoài danh mục có gắn nhãn hoặc chú thích rõ ràng "Chưa hỗ trợ tính dinh dưỡng".
- **AC-40.3 (Loại trừ bài viết khỏi pool gợi ý AI Menu dinh dưỡng):**
  - *Given* bài công thức được gắn trạng thái "Chưa đầy đủ dữ liệu dinh dưỡng",
  - *When* hệ thống thực hiện truy vấn trích xuất ứng viên cho chức năng AI lập thực đơn theo dinh dưỡng (FR-36),
  - *Then* bài công thức này bị loại hoàn toàn khỏi kết quả truy vấn và không bao giờ xuất hiện trong gợi ý thực đơn dinh dưỡng của AI.

---

<a id="fr-41"></a>
### FR-41 — Administrator quản lý danh mục nguyên liệu dinh dưỡng nội bộ

- **Mã yêu cầu:** FR-41
- **Module:** M09, M10
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp giao diện quản trị chuyên biệt cho phép Administrator xem, tìm kiếm, lọc, thêm mới, cập nhật, ngừng hỗ trợ (deactivate/soft-delete) và kích hoạt lại các bản ghi trong danh mục nguyên liệu dinh dưỡng nội bộ; mỗi bản ghi bắt buộc chứa tên chuẩn hóa, giá trị đầy đủ của 9 chỉ tiêu dinh dưỡng cốt lõi tính trên 100g, nguồn dữ liệu tham chiếu chính thức (USDA FoodData Central / Viện Dinh dưỡng Quốc gia - NIH), đường dẫn/tài liệu tham khảo và ngày cập nhật; cho phép Administrator xem danh sách các Recipe Post đang liên kết với nguyên liệu; nghiêm cấm xóa vĩnh viễn (hard delete) nguyên liệu đã được tham chiếu trong bất kỳ công thức nào; nghiêm cấm nhập hàng loạt tự động (auto bulk-import) hoặc để AI tự động tạo/sửa/xác nhận số liệu dinh dưỡng.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Người dùng có vai trò `Administrator` (M09, M10).
  - Bảo đảm tính toàn vẹn và thẩm quyền: Chỉ con người có thẩm quyền quản trị mới được điều chỉnh dữ liệu dinh dưỡng cốt lõi (BR-51, BR-52, BR-54).
- **Phân loại Actor:**
  - Primary Actor: `Administrator`.
  - Supporting Actor: `Hệ thống quản lý dữ liệu dinh dưỡng nội bộ`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-41.1`: Tìm kiếm, xem chi tiết và lọc danh mục nguyên liệu dinh dưỡng nội bộ.
  - `UC-41.2`: Thêm mới nguyên liệu dinh dưỡng với đủ 9 chỉ tiêu và thông tin nguồn tham chiếu.
  - `UC-41.3`: Chỉnh sửa thông số dinh dưỡng và thông tin nguồn của nguyên liệu hiện có.
  - `UC-41.4`: Chuyển trạng thái ngừng hỗ trợ (Deactivate) hoặc kích hoạt lại nguyên liệu dinh dưỡng.
  - `UC-41.5`: Xem danh sách các bài công thức đang sử dụng nguyên liệu dinh dưỡng.
- **User Stories:**
  - *Là một Administrator*, tôi muốn duy trì và cập nhật danh mục nguyên liệu dinh dưỡng chuẩn xác từ nguồn USDA/NIH có trích dẫn nguồn minh bạch, để hệ thống có cơ sở dữ liệu dinh dưỡng tin cậy phục vụ tính toán khẩu phần cho người dùng ăn chay.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập với tài khoản có vai trò Administrator (FR-03, NFR-09).
- **Kích hoạt (Trigger):**
  - Administrator truy cập phân hệ "Quản lý dinh dưỡng" -> "Danh mục nguyên liệu dinh dưỡng" trên thanh điều hướng quản trị.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Administrator truy cập danh sách nguyên liệu dinh dưỡng; hệ thống hiển thị danh sách dạng bảng phân trang gồm: Tên nguyên liệu, Năng lượng, Đạm, Carb, Chất béo, Nguồn tham chiếu (USDA/NIH), Ngày cập nhật, Trạng thái (Đang hỗ trợ / Đã ngừng hỗ trợ), và Số lượng công thức đang sử dụng.
  - Bước 2: Administrator nhấn nút "Thêm nguyên liệu mới".
  - Bước 3: Hệ thống hiển thị biểu mẫu yêu cầu nhập liệu:
    - Tên nguyên liệu tiếng Việt (chuẩn hóa, không trùng lặp) và tên tiếng Anh (tùy chọn).
    - Nhóm thực phẩm (Rau củ, Đậu & Chế phẩm, Ngũ cốc, Các loại hạt, Trái cây, Gia vị chay).
    - Giá trị của đầy đủ 9 chỉ tiêu cốt lõi trên 100g (Năng lượng kcal $\ge 0$, Protein g $\ge 0$, Carb g $\ge 0$, Fat g $\ge 0$, Fiber g $\ge 0$, Calcium mg $\ge 0$, Iron mg $\ge 0$, Vitamin B12 mcg $\ge 0$, Zinc mg $\ge 0$).
    - Nguồn dữ liệu tham chiếu (bắt buộc chọn USDA FoodData Central hoặc NIH/Viện Dinh Dưỡng).
    - Mã định danh hoặc URL tham chiếu nguồn (bắt buộc).
    - Ngày đối chiếu dữ liệu.
  - Bước 4: Administrator nhập đầy đủ dữ liệu và nhấn "Lưu nguyên liệu".
  - Bước 5: Hệ thống kiểm tra hợp lệ: Bắt buộc không được để trống bất kỳ chỉ tiêu nào trong 9 chỉ tiêu; không chấp nhận giá trị âm; tên không được trùng với nguyên liệu đang hoạt động; nguồn tham chiếu phải rõ ràng (BR-52).
  - Bước 6: Hệ thống lưu bản ghi mới vào cơ sở dữ liệu với trạng thái `Đang hỗ trợ` (Active), ghi nhận ID của Administrator tạo và thời gian tạo.
  - Bước 7: Hệ thống cập nhật lại danh sách và thông báo thêm mới thành công.
- **Luồng thay thế (Alternative Flows):**
  - *AF-41.1 (Chỉnh sửa nguyên liệu hiện có):* Administrator chọn một nguyên liệu và chỉnh sửa số liệu. Sau khi lưu hợp lệ, hệ thống cập nhật bản ghi và đánh dấu các công thức đang liên kết để tự động cập nhật lại dinh dưỡng trong chu kỳ tính toán tiếp theo.
  - *AF-41.2 (Ngừng hỗ trợ nguyên liệu):* Administrator chọn ngừng hỗ trợ (Deactivate) một nguyên liệu. Hệ thống chuyển trạng thái nguyên liệu sang `Ngừng hỗ trợ`. Các công thức cũ đã sử dụng nguyên liệu này vẫn giữ nguyên tham chiếu lịch sử, nhưng nguyên liệu sẽ không xuất hiện trong gợi ý chọn nguyên liệu mới cho người dùng (BR-53).
  - *AF-41.3 (Kích hoạt lại nguyên liệu):* Administrator có thể bật lại trạng thái `Đang hỗ trợ` cho một nguyên liệu đã bị ngừng hỗ trợ trước đó.
  - *AF-41.4 (Xem công thức đang liên kết):* Administrator nhấn vào số lượng công thức đang sử dụng để xem danh sách chi tiết các Recipe Post đang tham chiếu đến nguyên liệu này.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-41.1 (Cố gắng xóa vĩnh viễn nguyên liệu đã tham chiếu):* Nếu Administrator cố gắng thực hiện hành động xóa vĩnh viễn (hard delete) một nguyên liệu đã có ít nhất một bài công thức tham chiếu, hệ thống từ chối hành động, ngăn chặn xóa và hiển thị thông báo: *"Không thể xóa vĩnh viễn nguyên liệu đã được tham chiếu trong công thức. Vui lòng sử dụng tính năng Ngừng hỗ trợ"* (BR-53).
  - *EF-41.2 (Thiếu bất kỳ chỉ tiêu nào trong 9 chỉ tiêu):* Nếu biểu mẫu thiếu dù chỉ một giá trị chỉ tiêu dinh dưỡng hoặc thiếu nguồn trích dẫn, hệ thống chặn lưu và yêu cầu nhập đủ dữ liệu (BR-52).
  - *SF-41.1 (Chặn truy cập trái phép):* Người dùng không có quyền Administrator khi cố gắng gọi thao tác CRUD danh mục dinh dưỡng sẽ bị hệ thống từ chối với mã lỗi 403 Forbidden (NFR-09).
  - *SF-41.2 (Cấm tự động nhập hàng loạt và AI can thiệp):* Hệ thống không cung cấp API import tự động không qua kiểm duyệt và tuyệt đối không cấp quyền cho AI tự động sửa đổi hoặc chèn số liệu vào danh mục dinh dưỡng (BR-51, BR-54).

#### 5. Hậu điều kiện (Postconditions)
- Bản ghi nguyên liệu dinh dưỡng được thêm mới, cập nhật hoặc chuyển trạng thái an toàn trong danh mục nội bộ.
- Mọi thay đổi dữ liệu dinh dưỡng được lưu kèm vết kiểm toán (Audit Trail) gồm Administrator thực hiện và dấu thời gian.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Độc quyền cho vai trò `Administrator` (M09, M10). Member thông thường và Guest hoàn toàn không có quyền can thiệp.
- **Ràng buộc an toàn:** Tuyệt đối cấm AI tự động cập nhật hoặc xác thực dữ liệu dinh dưỡng; cấm xóa cứng dữ liệu đã tham chiếu (BR-51, BR-53, BR-54).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-46](BUSINESS-RULES.md#br-46): Nguồn tính toán dinh dưỡng chính thức của công thức.
  - [BR-49](BUSINESS-RULES.md#br-49): Không gọi API dinh dưỡng ngoài realtime và nguồn tham khảo.
  - [BR-51](BUSINESS-RULES.md#br-51): Thẩm quyền quản lý danh mục nguyên liệu dinh dưỡng (chỉ Administrator).
  - [BR-52](BUSINESS-RULES.md#br-52): Yêu cầu đầy đủ 9 chỉ tiêu và nguồn trước khi kích hoạt.
  - [BR-53](BUSINESS-RULES.md#br-53): Cấm xóa vĩnh viễn nguyên liệu dinh dưỡng đã tham chiếu.
  - [BR-54](BUSINESS-RULES.md#br-54): Cấm nhập hàng loạt tự động vào danh mục dinh dưỡng.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật (chặn can thiệp trái phép).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-41.1 (Phân quyền quản trị danh mục dinh dưỡng):**
  - *Given* người dùng không có vai trò Administrator (Member hoặc Guest),
  - *When* người dùng cố gắng truy cập hoặc thực hiện thao tác trên danh mục nguyên liệu dinh dưỡng,
  - *Then* hệ thống từ chối truy cập và trả về thông báo lỗi không có quyền.
- **AC-41.2 (Bắt buộc đủ 9 chỉ tiêu và nguồn dữ liệu):**
  - *Given* Administrator tạo mới hoặc cập nhật nguyên liệu dinh dưỡng,
  - *When* có bất kỳ chỉ tiêu nào trong 9 chỉ tiêu bị bỏ trống hoặc thiếu thông tin nguồn tham chiếu,
  - *Then* hệ thống ngăn chặn việc lưu và hiển thị thông báo yêu cầu nhập đầy đủ 9 chỉ tiêu và nguồn gốc trích dẫn.
- **AC-41.3 (Cấm xóa vĩnh viễn nguyên liệu đã tham chiếu):**
  - *Given* một nguyên liệu dinh dưỡng đang được liên kết trong ít nhất một bài Recipe Post,
  - *When* Administrator thực hiện thao tác xóa vĩnh viễn nguyên liệu đó,
  - *Then* hệ thống từ chối xóa, giữ nguyên toàn vẹn dữ liệu tham chiếu và yêu cầu chuyển sang thao tác ngừng hỗ trợ.
- **AC-41.4 (Chuyển trạng thái ngừng hỗ trợ an toàn):**
  - *Given* một nguyên liệu dinh dưỡng đang ở trạng thái hoạt động,
  - *When* Administrator xác nhận chuyển trạng thái sang "Ngừng hỗ trợ",
  - *Then* nguyên liệu không còn xuất hiện trong danh sách gợi ý nhập nguyên liệu mới cho người dùng nhưng các bài viết cũ vẫn giữ nguyên liên kết lịch sử.
- **AC-41.5 (Cấm AI tự động tạo hoặc sửa số liệu dinh dưỡng):**
  - *Given* hệ thống nhận được request từ các module AI hoặc tiến trình nền tự động,
  - *When* request cố gắng chỉnh sửa bảng dữ liệu nguyên liệu dinh dưỡng,
  - *Then* hệ thống từ chối xử lý và ghi log cảnh báo an ninh.

---

<a id="fr-42"></a>
### FR-42 — Member tìm nhà hàng chay theo địa chỉ nhập và bán kính đường bộ

- **Mã yêu cầu:** FR-42
- **Module:** M02, M11
- **Trạng thái (Derived):** DEFERRED
- **Mô tả:** Nội dung lịch sử của capability đã deferred: Member tìm nhà hàng theo địa chỉ nhập và ngưỡng đường bộ 500 m/1 km/5 km/10 km, không GPS hoặc liên kết món đã tìm; đề xuất cũ dùng 10 lượt/Member/ngày và không cho Guest gọi. Không triển khai hoặc tạo MVP Issue khi chưa có quyết định scope mới.

---

<a id="fr-43"></a>
### FR-43 — Hiển thị danh sách và bản đồ nhúng nhà hàng từ Google

- **Mã yêu cầu:** FR-43
- **Module:** M11
- **Trạng thái (Derived):** DEFERRED
- **Mô tả:** Nội dung lịch sử của capability đã deferred: hiển thị danh sách/bản đồ nhúng từ Google, ghi nguồn và trạng thái lỗi; không quản lý hồ sơ hoặc lưu nhà hàng yêu thích. Không triển khai hoặc tạo MVP Issue khi chưa có quyết định scope mới.

---

<a id="fr-44"></a>
### FR-44 — Member sửa hoặc xóa Recipe Post đã công khai của chính mình

- **Mã yêu cầu:** FR-44
- **Module:** M02, M03
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cho phép Member đã đăng nhập chỉnh sửa nội dung hoặc xóa bỏ bài công thức nấu ăn đã công khai do chính mình tạo ra; khi chỉnh sửa, các thay đổi phải vượt qua toàn bộ quy tắc kiểm tra hợp lệ Recipe Validation Profile chuẩn theo FR-16 (tiêu đề, khẩu phần, thời gian, loại ăn chay, danh mục, nguyên liệu theo FR-19, các bước hướng dẫn chuẩn bị/chế biến theo FR-22, BR-19) và được cập nhật công khai ngay lập tức mà không cần Administrator phê duyệt lại; trường hợp bài viết đang bị Administrator tạm ẩn do vi phạm (BR-27), Member không được tự ý sửa để mở lại công khai; khi xóa bài công thức, hệ thống chuyển trạng thái bài viết sang xóa mềm (Tombstone), bài viết không còn xuất hiện trên danh mục công khai hay tìm kiếm, nhưng các tham chiếu lịch sử trong Lịch ăn tuần (Meal Plan), Công thức đã lưu (Saved Recipe) và Danh sách mua sắm hiện có của người dùng khác được bảo toàn ở trạng thái hiển thị "Nội dung không còn khả dụng" để tránh phá vỡ tính toàn vẹn dữ liệu.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member sở hữu bài công thức (`Author`).
  - Phân quyền: Nghiêm cấm sửa hoặc xóa bài của tác giả khác (RBAC, chống IDOR theo NFR-09, NFR-10).
- **Phân loại Actor:**
  - Primary Actor: `Member` (tác giả bài viết).
  - Supporting Actor: `Hệ thống kiểm tra tính toàn vẹn dữ liệu`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-44.1`: Chỉnh sửa nội dung bài công thức đã công khai của chính mình.
  - `UC-44.2`: Xóa bài công thức đã công khai của chính mình kèm cơ chế bảo toàn tham chiếu (Tombstone).
- **User Stories:**
  - *Là một tác giả công thức*, tôi muốn cập nhật lại định lượng gia vị cho chính xác hơn sau khi nhận được phản hồi của bạn đọc, hoặc xóa bài viết khi không còn muốn chia sẻ mà không làm hỏng lịch ăn của những người đã lưu bài trước đó.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản Member và là tác giả sở hữu bài công thức (BR-17, BR-64).
  - Bài công thức đang ở trạng thái công khai (`Public`) và không nằm trong trạng thái bị Administrator khóa/ẩn vi phạm (BR-27).
- **Kích hoạt (Trigger):**
  - Tác giả nhấn nút "Chỉnh sửa công thức" hoặc "Xóa công thức" trên giao diện bài viết hoặc trang quản lý bài viết cá nhân.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow - Chỉnh sửa công thức):**
  - Bước 1: Tác giả nhấn "Chỉnh sửa công thức". Hệ thống kiểm tra quyền tác giả (BR-62, BR-64); nếu trùng khớp, hiển thị biểu mẫu chỉnh sửa với toàn bộ dữ liệu hiện tại của bài viết.
  - Bước 2: Tác giả sửa đổi các thông tin (tiêu đề, thời gian nấu, khẩu phần, danh sách nguyên liệu, các bước hướng dẫn chuẩn bị/chế biến, ảnh).
  - Bước 3: Tác giả nhấn "Lưu thay đổi".
  - Bước 4: Hệ thống thực hiện kiểm tra hợp lệ toàn bộ các tiêu chí validation bắt buộc theo FR-16 và BR-19.
  - Bước 5: Kiểm tra thành công, hệ thống cập nhật nội dung bài viết vào cơ sở dữ liệu, tự động tính toán lại bảng ước tính 9 chỉ tiêu dinh dưỡng (FR-39), và xuất bản trực tiếp các thay đổi lên trang công khai ngay lập tức mà không qua kiểm duyệt trước của Admin (BR-07, BR-59, BR-62).
  - Bước 6: Hệ thống hiển thị thông báo cập nhật thành công cho tác giả.
- **Luồng thay thế (Alternative Flows - Xóa công thức):**
  - *AF-44.1 (Quy trình xóa công thức):*
    - Bước 1: Tác giả nhấn "Xóa công thức".
    - Bước 2: Hệ thống hiển thị hộp thoại xác nhận cảnh báo: *"Hành động này sẽ gỡ bỏ công thức khỏi toàn bộ danh sách công khai và tìm kiếm. Các người dùng đã lưu công thức vào Lịch ăn sẽ thấy nội dung ở dạng không còn khả dụng. Bạn có chắc chắn muốn xóa?"*.
    - Bước 3: Tác giả xác nhận xóa.
    - Bước 4: Hệ thống cập nhật trạng thái bài công thức sang `DELETED` (Soft Delete / Tombstone) (BR-35, BR-64).
    - Bước 5: Hệ thống loại bỏ bài viết khỏi chỉ mục tìm kiếm và khỏi candidate pool của AI (BR-38, BR-40).
    - Bước 6: Tại các bản ghi Lịch ăn hoặc Công thức đã lưu của người dùng khác, hệ thống giữ nguyên liên kết khóa ngoại nhưng hiển thị thẻ tombstone: *"Công thức này đã bị xóa bởi tác giả"* và vô hiệu hóa liên kết xem chi tiết (FR-32, FR-33).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-44.1 (Bài viết đang bị Admin ẩn do vi phạm):* Nếu bài công thức đang ở trạng thái `HIDDEN` do quyết định xử lý báo cáo của Admin (BR-26, BR-27), tác giả không được tự ý sửa để chuyển trạng thái về công khai. Hệ thống khóa chức năng xuất bản trực tiếp và hiển thị thông báo hướng dẫn liên hệ Admin để phục hồi bài viết.
  - *SF-44.1 (Ngăn chặn sửa/xóa trái phép - IDOR):* Nếu người dùng cố gắng gửi request sửa hoặc xóa ID bài viết mà mình không phải là tác giả, hệ thống từ chối ngay lập tức với mã lỗi 403 Forbidden, ghi vết kiểm toán an ninh (NFR-09, NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Nội dung mới của bài viết được cập nhật công khai ngay lập tức khi sửa.
- Bài viết bị xóa biến mất khỏi kết quả tìm kiếm và danh mục công khai; các tham chiếu lịch sử hiển thị an toàn dưới dạng Tombstone.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chỉ chính tác giả sở hữu bài công thức hoặc Administrator mới có quyền sửa/xóa bài viết.
- **Ràng buộc an toàn:** Sửa bài công khai trực tiếp ngay (BR-07, BR-62); xóa bài bảo toàn tính toàn vẹn dữ liệu qua cơ chế Tombstone (BR-35, BR-64).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản đăng bài.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-27](BUSINESS-RULES.md#br-27): Quy tắc phục hồi bài công thức bị ẩn.
  - [BR-35](BUSINESS-RULES.md#br-35): Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn.
  - [BR-59](BUSINESS-RULES.md#br-59): Không có hàng đợi duyệt bài trước khi công khai.
  - [BR-62](BUSINESS-RULES.md#br-62): Kiểm tra quyền tác giả và validation khi sửa bài công thức.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến (chống IDOR).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-44.1 (Quyền tác giả khi chỉnh sửa bài viết):**
  - *Given* Member đăng nhập không phải là tác giả của bài công thức,
  - *When* Member cố gắng truy cập trang chỉnh sửa hoặc gửi yêu cầu cập nhật bài công thức,
  - *Then* hệ thống từ chối thực thi và hiển thị thông báo lỗi không có quyền chỉnh sửa.
- **AC-44.2 (Công khai trực tiếp thay đổi sau validation):**
  - *Given* chính tác giả chỉnh sửa và gửi nội dung cập nhật thỏa mãn đầy đủ điều kiện hợp lệ bắt buộc,
  - *When* tác giả nhấn lưu thay đổi,
  - *Then* các thay đổi được cập nhật và hiển thị công khai ngay lập tức mà không cần Admin phê duyệt lại.
- **AC-44.3 (Khóa tự sửa bài đang bị Admin ẩn vi phạm):**
  - *Given* bài công thức đang ở trạng thái bị Admin ẩn do vi phạm báo cáo,
  - *When* tác giả cố gắng chỉnh sửa và công khai lại bài viết,
  - *Then* hệ thống từ chối cho phép công khai và hiển thị thông báo yêu cầu thực hiện thủ tục phục hồi qua quản trị viên.
- **AC-44.4 (Xóa công thức và bảo toàn tham chiếu Tombstone):**
  - *Given* bài công thức đã được lưu trong Lịch ăn tuần của một người dùng khác,
  - *When* tác giả xác nhận xóa bài công thức đó,
  - *Then* bài viết chuyển sang trạng thái đã xóa, biến mất khỏi tìm kiếm công khai, và ô tương ứng trong Lịch ăn của người dùng kia hiển thị thông báo "Công thức này đã bị xóa bởi tác giả" mà không gây lỗi trang.

---

<a id="fr-45"></a>
### FR-45 — Like và Unlike bài công thức, bình luận và phản hồi

- **Mã yêu cầu:** FR-45
- **Module:** M01, M02, M03
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp tính năng Thích (Like) và Bỏ thích (Unlike) trên các nội dung công khai trong hệ thống, bao gồm bài công thức (Recipe Post), bình luận (Comment) và phản hồi bình luận (Reply); mỗi tài khoản Member chỉ được duy trì tối đa một lượt Like có hiệu lực duy nhất trên một nội dung cụ thể tại một thời điểm; thao tác lặp lại cùng một hành động được xử lý an toàn (idempotent); Guest chỉ được xem tổng số lượt Like công khai mà không thể thực hiện thao tác Like; hệ thống hoàn toàn không áp dụng cơ chế đánh giá sao (star rating) trong phạm vi MVP; cập nhật bộ đếm hiển thị nhất quán.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi bài Recipe Post công khai, mọi bình luận và reply công khai.
  - Phân quyền: Member đã đăng nhập được Like/Unlike; Guest chỉ xem (BR-05, BR-65).
- **Phân loại Actor:**
  - Primary Actor: `Member` (thực hiện Like/Unlike).
  - Secondary Actor: `Guest` (xem tổng lượt Like).
  - Supporting Actor: `Hệ thống quản lý tương tác và đếm lượt thích`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-45.1`: Thích (Like) một bài công thức, bình luận hoặc phản hồi bình luận.
  - `UC-45.2`: Bỏ thích (Unlike) nội dung đã thích trước đó.
  - `UC-45.3`: Xem tổng số lượt thích trên từng bài viết, bình luận và phản hồi.
- **User Stories:**
  - *Là một độc giả yêu thích nấu ăn*, tôi muốn bấm nút Thích trên các công thức ngon và các bình luận hữu ích để bày tỏ sự ủng hộ của tôi đối với tác giả.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng muốn Like/Unlike đã đăng nhập tài khoản Member hợp lệ (FR-03).
  - Nội dung mục tiêu (bài viết, bình luận hoặc reply) đang ở trạng thái công khai hợp lệ.
- **Kích hoạt (Trigger):**
  - Member nhấn vào biểu tượng Trái tim hoặc nút "Thích" tại bài viết, bình luận hoặc reply.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow - Thao tác Like):**
  - Bước 1: Member nhấn biểu tượng "Thích" trên một bài công thức hoặc bình luận.
  - Bước 2: Hệ thống kiểm tra phiên đăng nhập của Member (BR-05).
  - Bước 3: Hệ thống kiểm tra trong cơ sở dữ liệu xem Member đã có bản ghi Like trên nội dung này chưa (BR-65).
  - Bước 4: Nếu chưa có bản ghi Like, hệ thống ghi nhận một bản ghi Like mới gắn liền với ID tài khoản của Member và ID nội dung mục tiêu.
  - Bước 5: Hệ thống tăng bộ đếm tổng số Like của nội dung mục tiêu lên 1 đơn vị.
  - Bước 6: Giao diện người dùng chuyển đổi trạng thái biểu tượng Trái tim sang trạng thái "Đã thích" (Active/Filled) và cập nhật số lượng hiển thị ngay lập tức mà không cần tải lại toàn bộ trang.
- **Luồng thay thế (Alternative Flows - Thao tác Unlike):**
  - *AF-45.1 (Bỏ thích):*
    - Bước 1: Member nhấn vào biểu tượng Trái tim đang ở trạng thái "Đã thích".
    - Bước 2: Hệ thống xác định Member đã có bản ghi Like hợp lệ trên nội dung này.
    - Bước 3: Hệ thống xóa hoặc hủy kích hoạt bản ghi Like của Member đối với nội dung này (BR-65).
    - Bước 4: Hệ thống giảm bộ đếm tổng số Like của nội dung đi 1 đơn vị (bảo đảm bộ đếm không âm).
    - Bước 5: Giao diện chuyển biểu tượng Trái tim về trạng thái chưa thích (Outline) và cập nhật lại số lượng hiển thị.
  - *AF-45.2 (Guest xem lượt Like):* Guest truy cập trang chỉ nhìn thấy tổng số lượt Like công khai; nếu Guest bấm vào nút Thích, hệ thống hiển thị thông báo gợi ý đăng nhập (BR-05).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-45.1 (Nội dung mục tiêu đã bị xóa hoặc ẩn):* Nếu nội dung vừa bị xóa hoặc ẩn bởi Admin trong lúc Member bấm Like, hệ thống thông báo nội dung không còn khả dụng và không tăng bộ đếm.
  - *SF-45.1 (Chống spam và thao túng số Like):* Áp dụng ràng buộc duy nhất (Unique Constraint) trong cơ sở dữ liệu trên cặp `(AccountId, TargetType, TargetId)` để bảo đảm không một tài khoản nào có thể tạo nhiều hơn 1 lượt Like có hiệu lực trên cùng một nội dung (BR-65, NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Trạng thái Like/Unlike của Member được lưu trữ chính xác và bền vững.
- Tổng số lượt thích của nội dung được phản ánh chính xác cho toàn bộ cộng đồng xem.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Mọi Member đã đăng nhập đều có quyền Like/Unlike. Guest chỉ có quyền xem bộ đếm.
- **Ràng buộc thiết kế:** Hệ thống không hỗ trợ đánh giá sao 1-5 sao trong phạm vi MVP; chỉ duy nhất cơ chế Like nhị phân (Yes/No).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-65](BUSINESS-RULES.md#br-65): Quy tắc mỗi Member tối đa một Like hiệu lực.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến (chống race condition và spam click).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-45.1 (Yêu cầu đăng nhập khi Like):**
  - *Given* người dùng chưa đăng nhập (Guest) nhấn nút Like trên một bài công thức hoặc bình luận,
  - *When* sự kiện click diễn ra,
  - *Then* hệ thống không tăng số lượt Like và hiển thị thông báo yêu cầu đăng nhập để thích nội dung.
- **AC-45.2 (Thích thành công và tăng bộ đếm):**
  - *Given* Member đã đăng nhập và chưa từng Like bài công thức có 10 lượt Like,
  - *When* Member nhấn nút Like,
  - *Then* hệ thống ghi nhận lượt Like của Member, biểu tượng chuyển sang trạng thái đã thích và bộ đếm hiển thị tăng thành 11.
- **AC-45.3 (Bỏ thích thành công và giảm bộ đếm):**
  - *Given* Member đang ở trạng thái đã Like một bài viết có 11 lượt Like,
  - *When* Member nhấn lại nút Like để bỏ thích,
  - *Then* hệ thống hủy lượt Like của Member, biểu tượng trở về trạng thái chưa thích và bộ đếm giảm xuống còn 10.
- **AC-45.4 (Giới hạn tối đa một Like hiệu lực duy nhất):**
  - *Given* Member gửi liên tiếp nhiều request Like trên cùng một bình luận,
  - *When* hệ thống xử lý các request,
  - *Then* chỉ duy nhất 1 bản ghi Like được duy trì hợp lệ và bộ đếm không bị tăng sai lệch.
- **AC-45.5 (Cấm tính năng chấm điểm sao trong MVP):**
  - *Given* người dùng xem giao diện chi tiết công thức hoặc danh sách công thức,
  - *When* giao diện hiển thị các chỉ số tương tác,
  - *Then* chỉ có bộ đếm Like nhị phân được hiển thị, hoàn toàn không xuất hiện thang điểm hay đánh giá sao (Star Rating).

---

<a id="fr-46"></a>
### FR-46 — Hiển thị và quản lý bình luận, reply lồng nhiều cấp

- **Mã yêu cầu:** FR-46
- **Module:** M01, M02, M03, M09
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp chức năng bình luận cộng đồng đa cấp trên bài công thức nấu ăn; cho phép Guest xem toàn bộ danh sách bình luận công khai; cho phép Member đã đăng nhập tạo bình luận gốc mới, gửi câu trả lời (Reply) lồng nhau theo cấu trúc cây phân cấp tối đa 5 cấp; Member có quyền chỉnh sửa hoặc xóa bình luận của chính mình; khi bình luận cha bị xóa nhưng vẫn có bình luận con bên dưới, hệ thống giữ nguyên vị trí phân cấp và hiển thị bản ghi thay thế (Tombstone: *"Bình luận này đã bị xóa bởi người dùng"*) để bảo toàn tính toàn vẹn của chuỗi thảo luận; Administrator có thẩm quyền quản trị ẩn hoặc xóa các bình luận vi phạm quy chuẩn cộng đồng.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi bài Recipe Post ở trạng thái công khai (`Public`).
  - Giới hạn phân cấp: Cấu trúc lồng nhau tối đa là 5 cấp (Depth $\le 5$ theo BR-66); mọi phản hồi ở cấp 5 sẽ trở thành câu trả lời đồng cấp với cấp 5 hoặc chỉ đích danh tài khoản được phản hồi.
- **Phân loại Actor:**
  - Primary Actor: `Member` (tạo, sửa, xóa bình luận/reply của chính mình).
  - Secondary Actor: `Guest` (xem bình luận công khai).
  - Administrative Actor: `Administrator` (kiểm duyệt và xử lý bình luận vi phạm).

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-46.1`: Xem danh sách bình luận và chuỗi phản hồi lồng nhau trên bài công thức.
  - `UC-46.2`: Tạo bình luận gốc mới trên bài công thức công khai.
  - `UC-46.3`: Phản hồi (Reply) bình luận hoặc reply của người khác trong giới hạn 5 cấp.
  - `UC-46.4`: Chỉnh sửa nội dung bình luận/reply của chính mình.
  - `UC-46.5`: Xóa bình luận/reply của chính mình kèm cơ chế bảo toàn Tombstone nếu có phản hồi con.
  - `UC-46.6`: Administrator xử lý gỡ bỏ bình luận vi phạm chuẩn mực cộng đồng.
- **User Stories:**
  - *Là một người nấu ăn*, tôi muốn đặt câu hỏi dưới bài công thức và trao đổi qua lại với tác giả hoặc những người khác để học hỏi kinh nghiệm nấu nướng thực tế.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng muốn bình luận/reply đã đăng nhập tài khoản Member hợp lệ (FR-03).
  - Bài công thức đang ở trạng thái công khai (`Public`).
- **Kích hoạt (Trigger):**
  - Member nhập nội dung vào ô bình luận và nhấn "Gửi bình luận" hoặc nhấn "Trả lời" dưới một bình luận cụ thể.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow - Tạo bình luận/reply):**
  - Bước 1: Member xem bài viết, cuộn xuống phần bình luận và nhập nội dung (độ dài từ 1 đến 1.000 ký tự).
  - Bước 2: Nếu là phản hồi cho bình luận khác, Member nhấn nút "Trả lời"; hệ thống xác định ID bình luận cha và độ sâu phân cấp (depth level).
  - Bước 3: Member nhấn "Gửi bình luận".
  - Bước 4: Hệ thống thực hiện kiểm tra độ sâu phân cấp: nếu độ sâu $< 5$, gán `parentId` là bình luận được chọn; nếu độ sâu đã đạt cấp 5, gán `parentId` bằng cha của cấp 5 kèm nhãn đề cập (@mention) để giữ độ sâu tối đa không vượt quá 5 cấp (BR-66).
  - Bước 5: Hệ thống kiểm tra bộ lọc từ ngữ cấm tự động; nếu nội dung hợp lệ, bình luận được lưu vào cơ sở dữ liệu và xuất bản công khai trực tiếp ngay mà không qua duyệt trước (BR-07, BR-59).
  - Bước 6: Bình luận mới xuất hiện ngay trên giao diện theo đúng cấu trúc cây phân cấp; hệ thống kích hoạt sự kiện tạo thông báo cho tác giả bài viết hoặc người được reply (FR-49).
- **Luồng thay thế (Alternative Flows):**
  - *AF-46.1 (Chỉnh sửa bình luận của chính mình):* Member chọn nút "Chỉnh sửa" trên bình luận của mình. Hệ thống kiểm tra quyền sở hữu (BR-64). Member sửa nội dung và lưu lại. Hệ thống cập nhật nội dung mới kèm nhãn nhỏ "(Đã chỉnh sửa)" trên giao diện.
  - *AF-46.2 (Xóa bình luận có phản hồi con):* Member chọn "Xóa bình luận". Nếu bình luận đó đã có các reply con bên dưới, hệ thống không xóa hẳn bản ghi mà cập nhật cờ `isDeleted = true` và thay thế nội dung hiển thị thành văn bản Tombstone: *"Bình luận này đã bị xóa bởi người dùng"* (BR-66). Các reply con bên dưới vẫn được hiển thị bình thường.
  - *AF-46.3 (Xóa bình luận lá không có phản hồi con):* Nếu bình luận không có bất kỳ reply con nào bên dưới, hệ thống có thể gỡ bỏ hoàn toàn bản ghi khỏi giao diện.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-46.1 (Bình luận rỗng hoặc vượt quá 1.000 ký tự):* Hệ thống chặn gửi và hiển thị thông báo lỗi yêu cầu nội dung hợp lệ.
  - *SF-46.1 (Lọc mã độc XSS):* Toàn bộ nội dung bình luận bắt buộc phải được làm sạch (HTML sanitize / encode) trước khi hiển thị nhằm phòng chống triệt để các cuộc tấn công Cross-Site Scripting (NFR-10).
  - *SF-46.2 (Chặn sửa/xóa bình luận của người khác):* Chỉ tác giả sở hữu bình luận hoặc Administrator mới có quyền sửa/xóa; request trái phép bị từ chối với mã lỗi 403 Forbidden (NFR-09, NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Bình luận/reply mới được liên kết chuẩn xác trong cây phân cấp và hiển thị công khai.
- Cây thảo luận bảo toàn tính liên tục ngay cả khi có bình luận trung gian bị xóa.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member đăng bình luận trực tiếp; chỉ chính tác giả mới được sửa/xóa bình luận của mình; Admin có quyền kiểm duyệt hậu kiểm.
- **Ràng buộc cấu trúc:** Tuyệt đối không để độ sâu phân cấp vượt quá 5 cấp nhằm bảo vệ bố cục giao diện responsive trên thiết bị di động (BR-66, NFR-13).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản đăng bài.
  - [BR-59](BUSINESS-RULES.md#br-59): Không có hàng đợi duyệt bài trước khi công khai.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
  - [BR-66](BUSINESS-RULES.md#br-66): Quy tắc liên kết và phân quyền với reply bình luận.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật (chống XSS và IDOR).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-46.1 (Giới hạn độ sâu phân cấp lồng tối đa 5 cấp):**
  - *Given* một chuỗi phản hồi bình luận đang ở cấp độ sâu thứ 5,
  - *When* Member gửi câu trả lời cho bình luận cấp 5 đó,
  - *Then* hệ thống giữ nguyên cấu trúc ở cấp độ 5 và không tạo thêm cấp thụt dòng thứ 6 trên giao diện.
- **AC-46.2 (Cơ chế Tombstone khi xóa bình luận cha có con):**
  - *Given* một bình luận cha đang có 2 bình luận con phản hồi bên dưới,
  - *When* tác giả của bình luận cha thực hiện thao tác xóa bình luận,
  - *Then* nội dung bình luận cha được chuyển thành thông báo "Bình luận này đã bị xóa bởi người dùng" và 2 bình luận con vẫn được hiển thị bình thường.
- **AC-46.3 (Công khai trực tiếp và kiểm soát mã độc XSS):**
  - *Given* Member nhập bình luận có chứa các thẻ mã độc HTML/JavaScript như `<script>alert(1)</script>`,
  - *When* bình luận được gửi và lưu thành công,
  - *Then* hệ thống làm sạch mã độc (sanitize) và hiển thị dưới dạng văn bản an toàn mà không thực thi mã độc.
- **AC-46.4 (Chặn sửa/xóa trái phép bình luận người khác):**
  - *Given* Member A cố gắng gửi request sửa hoặc xóa bình luận do Member B viết,
  - *When* hệ thống kiểm tra quyền hạn,
  - *Then* hệ thống từ chối yêu cầu và giữ nguyên nội dung bình luận của Member B.

---

<a id="fr-47"></a>
### FR-47 — Gợi ý bài công thức liên quan thông thường và tùy chọn Gemini

- **Mã yêu cầu:** FR-47
- **Module:** M04, M06
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp tính năng gợi ý các bài công thức nấu ăn liên quan ở chân trang chi tiết Recipe Post; mặc định hệ thống hiển thị danh sách gợi ý liên quan thông thường dựa trên tiêu chí đối khớp thuộc tính tĩnh (cùng danh mục món ăn, trùng khớp ít nhất một nguyên liệu chính, cùng trường phái ăn chay) mà hoàn toàn không sử dụng trí tuệ nhân tạo (AI) và không tiêu tốn hạn mức; đồng thời cung cấp nút tùy chọn chuyên biệt "Nhờ Trợ lý AI gợi ý món ăn kết hợp hoặc biến tấu" sử dụng Google Gemini AI; chỉ khi Member chủ động bấm vào nút tùy chọn AI này, hệ thống mới gửi yêu cầu tới Gemini và trừ hạn mức sử dụng AI theo quy định (BR-03); các gợi ý của AI cũng chỉ được chọn lọc từ các Recipe Post đang công khai trong hệ thống, không tự bịa công thức mới.
- **Phạm vi nghiệp vụ:**
  - Gợi ý thông thường: Phục vụ cho mọi đối tượng người dùng (bao gồm Guest và Member) hoàn toàn miễn phí (BR-05).
  - Tùy chọn gọi AI: Độc quyền cho Member đã đăng nhập và còn hạn mức sử dụng AI hợp lệ (FR-10, BR-01, BR-02, BR-03).
- **Phân loại Actor:**
  - Primary Actor: `Guest / Member` (xem gợi ý thông thường), `Member` (chủ động yêu cầu AI gợi ý).
  - Supporting Actor: `Hệ thống truy vấn công thức nội bộ`, `Google Gemini AI` (khi được gọi).

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-47.1`: Xem danh sách các bài công thức liên quan thông thường dựa trên danh mục và nguyên liệu.
  - `UC-47.2`: Yêu cầu Trợ lý AI phân tích và đề xuất món ăn biến tấu hoặc kết hợp phù hợp từ kho công thức có sẵn.
- **User Stories:**
  - *Là một người tìm kiếm món ăn*, tôi muốn xem nhanh các món ăn tương tự hoặc cùng nguyên liệu ở cuối bài để có thêm sự lựa chọn, và khi cần ý tưởng độc đáo thì có thể nhờ AI gợi ý kết hợp món ăn kèm hoàn hảo.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Với gợi ý thông thường: Người dùng đang xem chi tiết một Recipe Post công khai hợp lệ (FR-18).
  - Với tùy chọn AI: Người dùng đã đăng nhập tài khoản Member và còn hạn mức AI trong ngày (FR-10, BR-03).
- **Kích hoạt (Trigger):**
  - Gợi ý thông thường: Tự động kích hoạt khi tải trang chi tiết bài viết.
  - Tùy chọn AI: Member chủ động bấm nút "Nhờ AI gợi ý món ăn kèm / biến tấu".

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow - Gợi ý thông thường không AI):**
  - Bước 1: Người dùng (Guest hoặc Member) truy cập trang chi tiết một bài công thức.
  - Bước 2: Hệ thống truy vấn cơ sở dữ liệu nội bộ tìm các bài công thức công khai khác có cùng danh mục hoặc có ít nhất một nguyên liệu chính trùng khớp, ưu tiên các bài có cùng trường phái ăn chay.
  - Bước 3: Hệ thống trả về danh sách từ 4 đến 6 bài công thức liên quan thông thường (thời gian tải trang $\le 2$ giây theo NFR-02).
  - Bước 4: Khối "Công thức liên quan" được hiển thị ở cuối bài viết mà hoàn toàn không gọi API bên ngoài và không trừ bất kỳ hạn mức AI nào (BR-03).
- **Luồng thay thế (Alternative Flows - Member chủ động gọi AI):**
  - *AF-47.1 (Yêu cầu AI gợi ý kết hợp món):*
    - Bước 1: Member bấm nút "Nhờ AI gợi ý món ăn kèm / biến tấu".
    - Bước 2: Hệ thống kiểm tra số dư hạn mức AI của Member (BR-01, BR-02, BR-03).
    - Bước 3: Hệ thống trích xuất danh sách các Recipe Post công khai trong hệ thống có thuộc tính phù hợp làm món ăn kèm hoặc biến tấu.
    - Bước 4: Hệ thống gửi prompt kèm metadata của bài hiện tại và danh sách ứng viên tới Google Gemini AI (đáp ứng NFR-03).
    - Bước 5: Gemini phản hồi danh sách gợi ý kèm lời giải thích ngắn gọn lý do kết hợp hương vị.
    - Bước 6: Hệ thống trừ 1 lượt hạn mức AI của Member và hiển thị kết quả gợi ý AI trong một khung chuyên biệt nổi bật (BR-03).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-47.1 (Guest bấm nút gọi AI):* Hệ thống hiển thị hộp thoại thông báo yêu cầu đăng nhập để sử dụng tính năng Trợ lý AI (BR-05).
  - *EF-47.2 (Member hết hạn mức AI):* Hệ thống hiển thị thông báo đã đạt giới hạn gọi AI trong ngày kèm gợi ý nâng cấp gói hoặc chờ làm mới vào 00:00 (FR-10, BR-01, BR-02).
  - *EF-47.3 (Lỗi kết nối Gemini hoặc timeout):* Nếu gọi AI gặp sự cố kỹ thuật hoặc quá thời gian chờ, hệ thống không trừ hạn mức của Member và thông báo thử lại sau (BR-04).

#### 5. Hậu điều kiện (Postconditions)
- Khối gợi ý thông thường hiển thị đầy đủ và nhanh chóng.
- Khi gọi AI thành công, kết quả đề xuất xuất hiện và hạn mức AI của Member giảm đi 1 lượt.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Gợi ý thông thường mở cho tất cả mọi người (Guest và Member). Gợi ý AI chỉ dành cho Member đã đăng nhập.
- **Ràng buộc kỹ thuật:** Tách biệt rạch ròi giữa thuật toán lọc cơ sở dữ liệu thông thường và cuộc gọi AI; cấm tự động gọi AI khi chỉ xem trang để tránh lãng phí chi phí và hạn mức (BR-03).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Hạn mức text AI cho tài khoản Free.
  - [BR-02](BUSINESS-RULES.md#br-02): Hạn mức text AI cho gói Plus và Pro.
  - [BR-03](BUSINESS-RULES.md#br-03): Điều kiện trừ hạn mức AI.
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider và timeout AI.
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-38](BUSINESS-RULES.md#br-38): AI không tự tạo công thức mới ngoài hệ thống.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian tải trang hiển thị chi tiết bài viết $\le 2$ giây.
  - [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03): Thời gian phản hồi của tính năng AI Chatbot.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-47.1 (Hiển thị gợi ý thông thường không tiêu tốn hạn mức):**
  - *Given* người dùng truy cập trang chi tiết bài công thức nấu ăn,
  - *When* khối công thức liên quan thông thường được hiển thị,
  - *Then* hệ thống hiển thị danh sách bài viết từ cơ sở dữ liệu nội bộ mà không tạo request tới dịch vụ AI và không trừ hạn mức của người dùng.
- **AC-47.2 (Chỉ gọi AI khi có thao tác bấm chủ động của Member):**
  - *Given* Member đang ở trang chi tiết bài công thức,
  - *When* Member chủ động nhấn nút "Nhờ AI gợi ý món ăn kèm / biến tấu",
  - *Then* hệ thống mới bắt đầu kết nối tới Google Gemini và chỉ trừ 1 lượt hạn mức khi kết quả trả về thành công.
- **AC-47.3 (AI chỉ đề xuất công thức có sẵn trong hệ thống):**
  - *Given* Member yêu cầu AI gợi ý món ăn kèm,
  - *When* kết quả gợi ý được trả về từ AI,
  - *Then* tất cả các món ăn được đề xuất đều có liên kết hợp lệ tới các Recipe Post đang công khai trong hệ thống, không chứa công thức giả định không tồn tại.
- **AC-47.4 (Bảo vệ hạn mức khi AI gặp sự cố):**
  - *Given* Member nhấn nút gọi AI nhưng dịch vụ gặp lỗi kết nối hoặc quá thời gian phản hồi quy định tại NFR-03,
  - *When* hệ thống thông báo lỗi kỹ thuật,
  - *Then* số dư hạn mức AI của Member được giữ nguyên không bị trừ.

---

<a id="fr-48"></a>
### FR-48 — Báo cáo bình luận, reply và Administrator hậu kiểm

- **Mã yêu cầu:** FR-48
- **Module:** M03, M09
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp cơ chế cho phép Member đã đăng nhập gửi báo cáo vi phạm đối với các bình luận (Comment) hoặc phản hồi (Reply) trên bài công thức nấu ăn khi phát hiện nội dung vi phạm chuẩn mực cộng đồng; các lý do báo cáo được phân loại rõ ràng (ngôn từ thù ghét/xúc phạm, quảng cáo/spam, công kích cá nhân, thông tin sai lệch); bình luận và reply vẫn được công khai trực tiếp ngay khi tạo và hệ thống xử lý qua quy trình hậu kiểm (post-moderation); Administrator tiếp nhận báo cáo tại Trung tâm kiểm duyệt để thẩm định và ra quyết định xử lý; áp dụng nghiêm ngặt nguyên tắc bảo mật tuyệt đối danh tính người báo cáo (BR-28) và cơ chế chống gửi báo cáo trùng lặp đang mở trên cùng một nội dung (BR-29); việc gửi báo cáo hoàn toàn độc lập với quyết định xử lý cuối cùng của Administrator (BR-58).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi bình luận và reply công khai trên hệ thống.
  - Phân quyền: Chỉ Member đã đăng nhập mới được gửi báo cáo (BR-24); Administrator có thẩm quyền duy nhất để xử lý báo cáo (BR-26).
- **Phân loại Actor:**
  - Primary Actor: `Member` (người gửi báo cáo vi phạm).
  - Administrative Actor: `Administrator` (người thẩm định và xử lý hậu kiểm).

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-48.1`: Gửi báo cáo vi phạm bình luận hoặc phản hồi với lý do và mô tả chi tiết.
  - `UC-48.2`: Administrator xem danh sách, thẩm định và đưa ra quyết định xử lý báo cáo bình luận.
- **User Stories:**
  - *Là một thành viên cộng đồng văn minh*, tôi muốn báo cáo những bình luận có lời lẽ xúc phạm hoặc quảng cáo rác để Ban quản trị xem xét và gỡ bỏ, giữ cho môi trường trao đổi ẩm thực luôn tích cực và an toàn.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng muốn báo cáo đã đăng nhập tài khoản Member hợp lệ (FR-03, BR-24).
  - Bình luận hoặc reply mục tiêu đang hiển thị công khai.
- **Kích hoạt (Trigger):**
  - Member nhấn vào nút hoặc biểu tượng "Báo cáo vi phạm" (Cờ báo cáo) tại một bình luận cụ thể.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member nhấn nút "Báo cáo" tại một bình luận hoặc reply.
  - Bước 2: Hệ thống kiểm tra phiên đăng nhập (BR-24) và kiểm tra xem tài khoản này đã có báo cáo nào đang ở trạng thái `Pending` đối với bình luận này chưa (BR-29).
  - Bước 3: Hệ thống hiển thị biểu mẫu báo cáo gồm: Lựa chọn nhóm lý do vi phạm (Ngôn từ thù ghét/xúc phạm, Quảng cáo/Spam rác, Công kích cá nhân, Thông tin sai lệch) và Ô nhập mô tả chi tiết bổ sung (tối đa 500 ký tự) (BR-25).
  - Bước 4: Member chọn lý do, nhập mô tả và nhấn "Gửi báo cáo".
  - Bước 5: Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào (bắt buộc chọn lý do, mô tả không chứa mã độc).
  - Bước 6: Hệ thống tạo bản ghi báo cáo mới gắn liền với ID bình luận, lưu trữ an toàn ID người báo cáo (bảo mật nghiêm ngặt theo BR-28), và chuyển báo cáo vào hàng đợi kiểm duyệt với trạng thái ban đầu là `Pending` (BR-58).
  - Bước 7: Bình luận mục tiêu vẫn tiếp tục hiển thị bình thường cho cộng đồng xem (BR-23, BR-58); hệ thống thông báo gửi báo cáo thành công cho Member.
  - Bước 8: Administrator truy cập Trung tâm kiểm duyệt hậu kiểm (FR-06, FR-28), xem xét nội dung bình luận bị báo cáo và các lý do đi kèm.
  - Bước 9: Administrator đưa ra một trong các quyết định xử lý:
    - *Bác bỏ báo cáo (Dismiss):* Xác định bình luận không vi phạm; báo cáo chuyển sang `Dismissed`. Bình luận giữ nguyên hiển thị.
    - *Gỡ bỏ bình luận (Delete/Hide):* Xác định bình luận vi phạm; hệ thống ẩn/xóa nội dung bình luận đó (hiển thị tombstone nếu có reply con theo FR-46/BR-66), và chuyển báo cáo sang `Resolved`.
  - Bước 10: Hệ thống kích hoạt thông báo kết quả xử lý cho Member đã gửi báo cáo và thông báo chế tài cho tác giả của bình luận vi phạm (FR-49).
- **Luồng thay thế (Alternative Flows):**
  - *AF-48.1 (Nhiều Member báo cáo cùng một bình luận):* Nếu nhiều Member khác nhau báo cáo cùng một bình luận, hệ thống ghi nhận các lượt báo cáo riêng biệt và gom nhóm (grouping) dưới cùng một mục kiểm duyệt để Administrator dễ dàng đánh giá mức độ nghiêm trọng.
  - *AF-48.2 (Bổ sung mô tả khi đã có báo cáo mở):* Nếu Member bấm báo cáo lại bình luận mà mình đã gửi báo cáo đang chờ xử lý, hệ thống cho phép bổ sung thêm mô tả thay vì tạo bản ghi báo cáo trùng lặp (BR-29, FR-30).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-48.1 (Guest cố gắng báo cáo):* Hệ thống chặn mở biểu mẫu và yêu cầu đăng nhập (BR-05, BR-24).
  - *SF-48.1 (Bảo mật tuyệt đối danh tính người báo cáo):* Hệ thống tuyệt đối không hiển thị tên hoặc ID của người báo cáo cho tác giả bình luận hay bất kỳ người dùng thông thường nào khác (BR-28, FR-29). Thông tin này chỉ có Administrator mới có quyền xem xét phục vụ kiểm duyệt.
  - *SF-48.2 (Không tự động ẩn bình luận theo số lượng báo cáo):* Số lượng lượt báo cáo dù lớn đến đâu cũng chỉ là căn cứ ưu tiên trong danh sách của Administrator; hệ thống tuyệt đối không tự động ẩn hay xóa bình luận khi chưa có thao tác xác nhận của Administrator (BR-23, BR-58).

#### 5. Hậu điều kiện (Postconditions)
- Bản ghi báo cáo bình luận được ghi nhận và đưa vào hàng đợi kiểm duyệt của Administrator.
- Khi Administrator phê duyệt xử lý, trạng thái bình luận và kết quả báo cáo được cập nhật đồng bộ.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Mọi Member đã đăng nhập đều có quyền gửi báo cáo. Quyền thẩm định và ra quyết định chế tài thuộc về Administrator (BR-26).
- **Ràng buộc nghiệp vụ:** Không tự ý xóa nội dung trước khi kiểm duyệt (BR-23, BR-58); bảo mật danh tính người báo cáo (BR-28).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-23](BUSINESS-RULES.md#br-23): Bản chất của báo cáo vi phạm từ người dùng.
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
  - [BR-25](BUSINESS-RULES.md#br-25): Yêu cầu lý do và mô tả trong biểu mẫu báo cáo.
  - [BR-26](BUSINESS-RULES.md#br-26): Thẩm quyền xử lý báo cáo và áp dụng chế tài.
  - [BR-28](BUSINESS-RULES.md#br-28): Bảo mật danh tính người báo cáo và tính riêng tư của báo cáo.
  - [BR-29](BUSINESS-RULES.md#br-29): Chống tạo báo cáo trùng lặp trên cùng một nội dung.
  - [BR-58](BUSINESS-RULES.md#br-58): Độc lập giữa thao tác gửi báo cáo và quyết định xử lý.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-48.1 (Yêu cầu đăng nhập khi báo cáo bình luận):**
  - *Given* người dùng chưa đăng nhập nhấn nút báo cáo bình luận,
  - *When* hành động được kích hoạt,
  - *Then* hệ thống ngăn chặn việc mở biểu mẫu báo cáo và hiển thị thông báo yêu cầu đăng nhập.
- **AC-48.2 (Bắt buộc chọn nhóm lý do vi phạm):**
  - *Given* Member mở biểu mẫu báo cáo bình luận,
  - *When* Member nhấn gửi báo cáo mà chưa chọn bất kỳ lý do vi phạm nào,
  - *Then* hệ thống ngăn chặn việc gửi và hiển thị thông báo yêu cầu chọn lý do.
- **AC-48.3 (Bảo mật danh tính người báo cáo):**
  - *Given* Member A gửi báo cáo bình luận của Member B,
  - *When* Member B xem thông báo hoặc chi tiết bình luận,
  - *Then* Member B hoàn toàn không nhìn thấy bất kỳ thông tin nào liên quan đến danh tính của Member A.
- **AC-48.4 (Chống trùng lặp báo cáo đang mở):**
  - *Given* Member đã có một báo cáo trạng thái Pending trên bình luận C,
  - *When* Member đó nhấn báo cáo lại bình luận C,
  - *Then* hệ thống không tạo bản ghi báo cáo mới mà hiển thị giao diện cho phép cập nhật bổ sung mô tả vào báo cáo hiện có.
- **AC-48.5 (Không tự động ẩn bình luận trước khi Admin quyết định):**
  - *Given* một bình luận nhận được 10 lượt báo cáo từ các Member khác nhau,
  - *When* Administrator chưa thực hiện thao tác xử lý,
  - *Then* bình luận đó vẫn tiếp tục hiển thị bình thường trên giao diện bài viết.

---

<a id="fr-49"></a>
### FR-49 — Gửi thông báo trong app và email cho reply mới và kết quả báo cáo

- **Mã yêu cầu:** FR-49
- **Module:** M02, M03, M09
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống tự động tạo và gửi thông báo trong ứng dụng (In-app Notification) cho Member khi có các sự kiện nghiệp vụ liên quan: có người phản hồi (Reply) bình luận của họ, hoặc có thông báo kết quả xử lý từ Administrator đối với báo cáo vi phạm do họ gửi hay đối với nội dung của chính họ bị xử lý vi phạm; đồng thời hỗ trợ gửi email thông báo bất đồng bộ theo cơ chế nỗ lực tối đa (asynchronous / best-effort) dựa trên tùy chọn nhận thông báo của người dùng; sự cố phát sinh từ dịch vụ email tuyệt đối không được gây lỗi hoặc rollback nghiệp vụ gốc và phải được ghi nhật ký (log) để xử lý độc lập; thông báo kết quả kiểm duyệt luôn được ưu tiên gửi; đảm bảo tính bảo mật danh tính người báo cáo trong mọi nội dung thông báo (FR-29, BR-28).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi Member đã đăng ký tài khoản hợp lệ (FR-03).
  - Loại sự kiện kích hoạt:
    1. Có phản hồi mới trên bình luận của Member (Reply event).
    2. Có kết quả xử lý báo cáo từ Administrator (Moderation outcome event).
  - Phân luồng kỹ thuật: In-app notification được ghi trực tiếp vào cơ sở dữ liệu; Email notification được đưa vào hàng đợi gửi ngầm không đồng bộ (Asynchronous Queue).
- **Phân loại Actor:**
  - Primary Actor: `Member` (người nhận thông báo).
  - Supporting Actor: `Hệ thống thông báo nội bộ`, `Dịch vụ gửi email (SMTP/Email Service Provider)`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-49.1`: Nhận và xem danh sách thông báo sự kiện trong ứng dụng (In-app Notification Center).
  - `UC-49.2`: Đánh dấu thông báo là đã đọc hoặc đánh dấu đọc tất cả.
  - `UC-49.3`: Nhận email thông báo bất đồng bộ về phản hồi mới hoặc kết quả kiểm duyệt.
  - `UC-49.4`: Cấu hình tùy chọn bật/tắt nhận email thông báo trong trang quản lý tài khoản.
- **User Stories:**
  - *Là một thành viên tích cực*, tôi muốn nhận được thông báo ngay khi có ai đó trả lời bình luận của tôi để tôi có thể kịp thời phản hồi, và tôi muốn nhận email khi báo cáo vi phạm của tôi đã được Ban quản trị xử lý để biết kết quả.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người nhận là Member có tài khoản hoạt động và có địa chỉ email đã xác thực (FR-03).
- **Kích hoạt (Trigger):**
  - Sự kiện 1: Có người dùng khác tạo thành công một câu trả lời (Reply) dưới bình luận của Member (FR-46).
  - Sự kiện 2: Administrator hoàn tất thẩm định và cập nhật quyết định xử lý báo cáo vi phạm liên quan đến Member (FR-06, FR-28, FR-48).

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Sự kiện nghiệp vụ phát sinh (có Reply mới hoặc có kết quả xử lý kiểm duyệt).
  - Bước 2: Hệ thống trích xuất thông tin người nhận mục tiêu và nội dung sự kiện.
  - Bước 3: Hệ thống tạo một bản ghi Thông báo trong ứng dụng (In-app Notification) gắn với tài khoản người nhận, gồm: Tiêu đề, Tóm tắt nội dung, Liên kết điều hướng tới nội dung liên quan, Trạng thái (`Chưa đọc`), và Dấu thời gian.
  - Bước 4: Biểu tượng thông báo (Quả chuông) trên giao diện của Member tăng số đếm chưa đọc và hiển thị chấm đỏ (badge).
  - Bước 5: Hệ thống kiểm tra cấu hình tùy chọn nhận email của Member và loại sự kiện:
    - Nếu là sự kiện kết quả kiểm duyệt: Bắt buộc gửi email thông báo (ưu tiên cao).
    - Nếu là sự kiện reply mới: Kiểm tra tùy chọn người dùng có bật "Nhận email khi có phản hồi" hay không.
  - Bước 6: Nếu thỏa mãn điều kiện gửi email, hệ thống đẩy công việc (Job) gửi email vào hàng đợi bất đồng bộ (Background Task Queue).
  - Bước 7: Tiến trình ngầm thực hiện kết nối tới dịch vụ email và gửi thư đến địa chỉ email đã đăng ký của Member.
  - Bước 8: Ghi nhận trạng thái gửi thành công vào nhật ký hệ thống.
- **Luồng thay thế (Alternative Flows):**
  - *AF-49.1 (Người dùng đọc thông báo):* Member nhấn vào biểu tượng Quả chuông, xem danh sách thông báo và nhấn vào một thông báo cụ thể. Hệ thống chuyển trạng thái thông báo thành `Đã đọc` và điều hướng người dùng tới bài viết/bình luận tương ứng.
  - *AF-49.2 (Đánh dấu đọc tất cả):* Member bấm nút "Đánh dấu đã đọc tất cả". Hệ thống cập nhật toàn bộ thông báo chưa đọc của Member thành đã đọc và xóa số đếm trên Quả chuông.
  - *AF-49.3 (Member tắt nhận email reply):* Nếu Member đã cấu hình tắt nhận email đối với các phản hồi bình luận, hệ thống bỏ qua Bước 6 đối với sự kiện reply, chỉ tạo thông báo trong ứng dụng.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-49.1 (Lỗi gửi email hoặc dịch vụ email quá tải):* Nếu dịch vụ gửi email phản hồi lỗi hoặc timeout, tiến trình ngầm ghi log lỗi và có thể thử lại (retry) tối đa 3 lần. Tuyệt đối không làm ảnh hưởng hay rollback hành động bình luận hoặc quyết định kiểm duyệt gốc của hệ thống.
  - *SF-49.1 (Bảo vệ danh tính người báo cáo trong email/thông báo):* Khi gửi thông báo về kết quả xử lý báo cáo, nội dung thông báo gửi cho tác giả bị báo cáo tuyệt đối không chứa tên tài khoản, email hay bất kỳ thông tin nào của người đã gửi báo cáo (BR-28, FR-29).

#### 5. Hậu điều kiện (Postconditions)
- Bản ghi thông báo trong ứng dụng được lưu trữ an toàn và hiển thị trên giao diện của người nhận.
- Email thông báo được gửi đi thành công hoặc được ghi log để theo dõi mà không làm gián đoạn luồng nghiệp vụ chính.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member chỉ có quyền xem và thao tác trên danh sách thông báo của chính tài khoản mình (RBAC theo NFR-09).
- **Ràng buộc kỹ thuật:** Email gửi bất đồng bộ (asynchronous / best-effort); bảo mật danh tính người báo cáo là bắt buộc (BR-28).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-28](BUSINESS-RULES.md#br-28): Bảo mật danh tính người báo cáo và tính riêng tư của báo cáo.
  - [BR-66](BUSINESS-RULES.md#br-66): Quy tắc liên kết và phân quyền với reply bình luận.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-49.1 (Tạo thông báo trong app khi có reply mới):**
  - *Given* Member B gửi một reply dưới bình luận của Member A,
  - *When* hành động tạo reply hoàn tất thành công,
  - *Then* hệ thống tạo ngay một bản ghi thông báo trong ứng dụng cho Member A và tăng số đếm chưa đọc trên biểu tượng Quả chuông của Member A.
- **AC-49.2 (Gửi email thông báo bất đồng bộ):**
  - *Given* Member A bật tùy chọn nhận email cho reply mới và Member B gửi câu trả lời,
  - *When* hệ thống ghi nhận sự kiện,
  - *Then* email thông báo được đưa vào hàng đợi gửi ngầm bất đồng bộ mà không làm chậm thời gian phản hồi giao diện của Member B.
- **AC-49.3 (Không rollback nghiệp vụ khi gửi email thất bại):**
  - *Given* máy chủ email gặp sự cố không thể kết nối,
  - *When* sự kiện reply mới hoặc kết quả kiểm duyệt diễn ra,
  - *Then* thao tác đăng reply hoặc cập nhật trạng thái kiểm duyệt vẫn thành công bình thường và hệ thống ghi log cảnh báo lỗi gửi email.
- **AC-49.4 (Bảo mật danh tính người báo cáo trong thông báo kiểm duyệt):**
  - *Given* Administrator xử lý gỡ bỏ bài viết hoặc bình luận vi phạm của Member X theo báo cáo của Member Y,
  - *When* hệ thống gửi thông báo kết quả kiểm duyệt cho Member X,
  - *Then* nội dung thông báo hoàn toàn không chứa bất kỳ dấu vết hay thông tin định danh nào của Member Y.

---

<a id="fr-50"></a>
### FR-50 — Xem và xóa lịch sử hội thoại AI riêng theo tài khoản

- **Mã yêu cầu:** FR-50
- **Module:** M02, M06
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Mô tả:** Member xem/xóa lịch sử hội thoại AI riêng; Guest không có lịch sử tài khoản.

---

<a id="fr-51"></a>
### FR-51 — AI Chatbot hỗ trợ hỏi đáp ẩm thực chay theo ngữ cảnh

- **Mã yêu cầu:** FR-51
- **Module:** M06 (AI Assistant & Personalization), M10 (Nutrition & Dietetics)
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp **MỘT AI Chatbot duy nhất dành cho người dùng cuối** (Unified End-User AI Chatbot), tích hợp Google Gemini AI, hỗ trợ đa năng lực/hỏi đáp tùy theo câu hỏi và ngữ cảnh người dùng đang tương tác. Hệ thống không tạo nhiều chatbot riêng biệt (như Cooking Bot, Recipe Bot, Nutrition Bot, Ingredient Bot, BMI Bot); thay vào đó, cùng một AI Chatbot sẽ hỗ trợ linh hoạt dựa trên hai ngữ cảnh hoạt động chính:
  1. *Ngữ cảnh chung (General Context):* Người dùng mở chatbot từ giao diện chung của hệ thống để hỏi đáp về lối sống ăn chay, kỹ thuật nấu ăn và chế biến món chay, gợi ý nguyên liệu thay thế phù hợp với trường phái ăn chay, giải thích kiến thức dinh dưỡng thực vật, và giải thích ý nghĩa tham khảo của chỉ số BMI cùng mức năng lượng calorie (dựa trên hồ sơ dinh dưỡng nếu Member đã khai báo tại FR-35).
  2. *Ngữ cảnh bài công thức (Recipe Context):* Khi người dùng đang xem một bài công thức nấu ăn cụ thể (trang Recipe Detail theo FR-20) và chủ động chọn chức năng "Hỏi AI về công thức này", hệ thống chuyển dữ liệu bài công thức hiện tại làm ngữ cảnh trực tiếp cho Chatbot FR-51. Ngữ cảnh bao gồm các dữ liệu Recipe hợp lệ: tiêu đề, loại ăn chay, khẩu phần, thời gian chuẩn bị và nấu, danh sách nguyên liệu và định lượng (FR-19), 1–30 bước hướng dẫn chuẩn bị/chế biến (FR-22), và dữ liệu dinh dưỡng khả dụng (FR-39). Người dùng có thể đặt các câu hỏi gắn liền với món ăn đó, ví dụ: *"Bước 3 nghĩa là gì và xào khoảng bao lâu?"*, *"Không có dầu mè thì thay bằng gì trong món này?"*, *"Giải thích cách làm món này chi tiết hơn cho người mới nấu"*, *"Món này có bao nhiêu calorie theo dữ liệu hiện có?"*.
- **Người dùng và Hạn ngạch (Quota):**
  - Cả `Guest` và `Member` sử dụng **CÙNG MỘT AI Chatbot** này; không tạo bot riêng biệt theo phân quyền người dùng.
  - Sự khác biệt duy nhất nằm ở cơ chế xác thực, hạn ngạch (quota) và mức độ cá nhân hóa:
    - *Guest:* Tối đa 5 request AI thành công/ngày (theo FR-02, BR-01), định danh qua anonymous cookie kết hợp coarse IP rate limiting theo BR-01; được phép hỏi cả ngữ cảnh chung và ngữ cảnh bài công thức công khai; không có lịch sử hội thoại lưu theo tài khoản và không có hồ sơ dinh dưỡng cá nhân (BR-05).
    - *Member Free:* Tối đa 5 request AI thành công/ngày (theo tài khoản, BR-01).
    - *Member Plus:* Tối đa 15 request AI thành công/ngày (theo tài khoản, BR-02).
    - *Member Pro:* Tối đa 50 request AI thành công/ngày (theo tài khoản, BR-02).
    - Toàn bộ hạn ngạch tự động làm mới vào lúc 00:00 `Asia/Ho_Chi_Minh` hàng ngày (BR-01, BR-02).
    - Chỉ request AI hoàn tất thành công mới tiêu thụ 1 lượt hạn ngạch; lỗi provider hoặc timeout tuyệt đối không trừ quota (BR-03, BR-04, NFR-18).
  - Thời gian phản hồi: đáp ứng [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03).
- **Ranh giới an toàn, nguồn dữ liệu và toàn vẹn hệ thống (Safety & Source Boundaries):**
  - *Ranh giới y tế tuyệt đối:* Chatbot không phải chuyên gia y tế, không đưa ra chẩn đoán bệnh lý hay phác đồ ăn uống điều trị y khoa (SRS 3.15, BR-09, BR-41). Mọi thông tin chỉ mang tính tham khảo tổng quát.
  - *Tính tham khảo của BMI & Calorie:* Chỉ số BMI và mức phân bổ calorie chỉ là thông tin ước tính hỗ trợ tham khảo trong hồ sơ dinh dưỡng rộng hơn (BR-39, BR-41), không phải chỉ định lâm sàng.
  - *Tôn trọng trường phái ăn chay và giới hạn của người dùng:* AI Chatbot phải tuân thủ nghiêm ngặt trường phái ăn chay (`dietaryType`: Vegan, Lacto-Vegetarian, Ovo-Vegetarian, Lacto-Ovo Vegetarian) của bài công thức đang xem hoặc của người dùng yêu cầu/khai báo, cùng các giới hạn kiêng kỵ và dị ứng (`allergiesAndRestrictions`); tuyệt đối không gợi ý các nguyên liệu vi phạm `dietaryType` tương ứng.
  - *Không bịa đặt dữ liệu (Non-fabrication):* Không tự ý bịa đặt số liệu dinh dưỡng hoặc khẳng định các dữ kiện không có căn cứ khoa học (NFR-25). Khi bài công thức không có thông tin cụ thể (ví dụ: tác giả không ghi số phút hay mức nhiệt của bước nấu), AI phải nêu rõ công thức gốc không đề cập thông tin này trước khi đưa ra ước tính mang tính gợi ý bổ trợ, và phân biệt rõ ước tính đó với dữ kiện gốc của tác giả.
  - *Minh bạch nguồn phát ngôn:* Nội dung giải đáp của AI phải được gắn nhãn nhận diện AI rõ ràng, tách biệt hoàn toàn với nội dung gốc do tác giả bài viết biên soạn.
  - *Toàn vẹn dữ liệu hệ thống:* AI Chatbot chỉ đóng vai trò hỏi đáp/tư vấn và không được tự thực hiện thao tác ghi hoặc thay đổi dữ liệu nghiệp vụ của người dùng (ví dụ: không tự sửa Recipe Post, không tự thêm/xóa món trong Meal Plan, không tự thay đổi hồ sơ người dùng). Riêng đối với dữ liệu dinh dưỡng chính thức, AI tuyệt đối không được trực tiếp tạo hoặc thay đổi giá trị trong danh mục nguyên liệu dinh dưỡng theo [BR-51](BUSINESS-RULES.md#br-51).
  - *Ranh giới với các workflow AI khác:* Các chức năng AI có mục tiêu nghiệp vụ khác nhau được duy trì ở các FR độc lập tương ứng và KHÔNG bị gộp vào FR-51:
    - AI hỗ trợ tác giả soạn bài công thức (gợi ý mô tả, gợi ý bước làm, kết quả editable, người dùng chủ động xác nhận, không tự publish) -> thuộc [FR-21](FUNCTIONAL-REQUIREMENTS.md#fr-21).
    - AI gợi ý món và lập thực đơn tuần -> thuộc [FR-34](FUNCTIONAL-REQUIREMENTS.md#fr-34) và [FR-36](FUNCTIONAL-REQUIREMENTS.md#fr-36).
    - AI rà soát và gắn cờ nội dung vi phạm -> thuộc [FR-12](FUNCTIONAL-REQUIREMENTS.md#fr-12) (DEFERRED).
    - Gợi ý bài viết liên quan thông thường hoặc tùy chọn Gemini -> thuộc [FR-47](FUNCTIONAL-REQUIREMENTS.md#fr-47).
- **Phân loại Actor:**
  - Primary Actor: `Guest` (người dùng chưa xác thực trải nghiệm theo FR-02), `Member` (người dùng đã đăng nhập theo FR-10).
  - Supporting Actor: `Google Gemini AI` (mô hình ngôn ngữ lớn hỗ trợ xử lý ngôn ngữ tự nhiên), `Hệ thống kiểm soát hạn mức và bảo mật`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-51.1 — Hỏi đáp AI trong ngữ cảnh chung`: Người dùng đặt câu hỏi với AI Chatbot từ giao diện chung của hệ thống. Các năng lực (capabilities) được hỗ trợ bao gồm:
    - Hỏi đáp về lối sống ăn chay, kiến thức ẩm thực và kỹ thuật chế biến món chay căn bản.
    - Giải thích ý nghĩa tham khảo của chỉ số BMI và mức phân bổ calorie theo ranh giới an toàn (kết hợp hồ sơ dinh dưỡng nếu Member đã khai báo tại FR-35).
    - Giải thích cách sử dụng, xử lý hoặc thay thế nguyên liệu mà người dùng chủ động hỏi trong phạm vi tư vấn ẩm thực; không tạo Recipe mới ngoài phạm vi đã chốt.
  - `UC-51.2 — Hỏi đáp AI theo Recipe Post đang xem`: Người dùng đang xem một bài công thức cụ thể và chọn "Hỏi AI về công thức này". Các năng lực (capabilities) được hỗ trợ bao gồm:
    - Giải thích chi tiết bước chuẩn bị/chế biến trong công thức đang xem (kỹ thuật nấu, lưu ý khi làm, ước tính tham khảo khi công thức chưa nêu rõ).
    - Gợi ý thay thế nguyên liệu phù hợp với trường phái ăn chay của món ăn hoặc theo yêu cầu của người dùng (Vegan, Lacto, Ovo, Lacto-Ovo).
    - Giải thích thành phần nguyên liệu, thuật ngữ nấu ăn và dữ liệu dinh dưỡng sẵn có trong bài viết.
- **User Stories:**
  - `US-51.1` *(Hỏi đáp chung)*: Là một người tìm hiểu hoặc thực hành ăn chay, tôi muốn hỏi Chatbot về kỹ thuật nấu ăn và kiến thức dinh dưỡng thực vật căn bản để có chế độ ăn lành mạnh và tự tin vào bếp.
  - `US-51.2` *(Ngữ cảnh công thức)*: Là người đang xem một bài công thức nấu ăn cụ thể, tôi muốn chọn "Hỏi AI về công thức này" để được AI giải đáp nhanh các thắc mắc về món ăn đó (giải thích bước làm, gợi ý thay thế nguyên liệu) dựa trên đúng nguyên liệu và dữ liệu của bài viết mà không phải tự gõ lại thông tin.
  - `US-51.3` *(Dinh dưỡng & BMI an toàn)*: Là một người theo dõi sức khỏe, tôi muốn hỏi AI để hiểu rõ ý nghĩa tham khảo của chỉ số BMI cá nhân và cách phân bổ calorie của bữa ăn chay mà không tiếp nhận các chỉ định y khoa sai lệch.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Guest: Thiết bị hỗ trợ cookie; số lượt AI thành công trong ngày $< 5$ (FR-02, BR-01).
  - Member: Đã đăng nhập tài khoản Member hợp lệ (FR-03); số lượt AI thành công trong ngày còn khả dụng theo gói tài khoản (Free 5, Plus 15, Pro 50 theo FR-10, BR-01, BR-02); đã xác nhận phạm vi hỗ trợ dinh dưỡng trước khi hỏi về chỉ số cá nhân (FR-38, BR-41).
  - Đối với ngữ cảnh bài công thức: Bài công thức đang ở trạng thái công khai (`PUBLISHED`) mà người dùng có quyền xem (FR-20).
- **Kích hoạt (Trigger):**
  - Người dùng mở khung Chatbot AI từ thanh công cụ/menu chính; HOẶC
  - Người dùng nhấn nút "Hỏi AI về công thức này" trên trang chi tiết công thức (Recipe Detail View).

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Người dùng kích hoạt AI Chatbot (từ giao diện chung hoặc từ nút "Hỏi AI về công thức này" tại Recipe Detail View). Giao diện hiển thị rõ thông báo từ chối trách nhiệm y tế chuẩn: *"Trợ lý AI cung cấp thông tin tham khảo tổng quát, không đưa ra chẩn đoán hay thay thế tư vấn y khoa"* (BR-09, BR-41). Nếu mở từ bài công thức, giao diện hiển thị huy hiệu gắn ngữ cảnh công thức kèm tiêu đề món.
  - Bước 2: Người dùng nhập câu hỏi và nhấn gửi.
  - Bước 3: Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào và kiểm tra số dư hạn ngạch khả dụng (Guest theo FR-02/BR-01, Member theo FR-10/BR-01/BR-02).
  - Bước 4: Hệ thống chuẩn bị dữ liệu ngữ cảnh an toàn cho cuộc gọi AI:
    - Thiết lập các chỉ dẫn an toàn bắt buộc: tuân thủ đúng `dietaryType` của context/người dùng và các giới hạn `allergiesAndRestrictions`; không gợi ý nguyên liệu vi phạm; từ chối chẩn đoán/kê đơn y khoa (BR-09, BR-41); cấm tự bịa đặt số liệu dinh dưỡng (NFR-25); nhấn mạnh tính tham khảo của BMI/calorie (BR-39); yêu cầu phân biệt nội dung AI với dữ liệu gốc của tác giả; cấm tự ý sửa đổi dữ liệu hệ thống.
    - Đóng gói dữ liệu ngữ cảnh:
      - Nếu ở ngữ cảnh Recipe: truyền tiêu đề, loại ăn chay, khẩu phần, thời gian, danh sách nguyên liệu & định lượng (FR-19), 1–30 bước hướng dẫn (FR-22), và dữ liệu dinh dưỡng sẵn có của bài công thức hiện tại (FR-39).
      - Nếu ở ngữ cảnh chung và Member đã đăng nhập: có thể tích hợp thông tin hồ sơ dinh dưỡng tham khảo của Member (FR-35) khi người dùng hỏi về chỉ số cá nhân.
  - Bước 5: Hệ thống gửi yêu cầu tới dịch vụ AI qua Backend an toàn (BR-06; không để lộ thông tin bảo mật hay khóa truy cập ra client).
  - Bước 6: Dịch vụ AI phản hồi kết quả hợp lệ đáp ứng thời gian quy định tại [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03).
  - Bước 7: Hệ thống ghi nhận lượt gọi thành công, tăng bộ đếm số lượt đã dùng trong ngày thêm 1 (BR-03) và ghi nhận dữ liệu kỹ thuật không lưu trữ nội dung câu hỏi thô (FR-11, BR-04, NFR-22).
  - Bước 8: Giao diện hiển thị câu trả lời dạng văn bản định dạng rõ ràng cho người dùng, phân biệt rõ lời AI với nội dung gốc của tác giả, hiển thị số lượt còn lại trong ngày (và tùy chọn đóng/chuyển đổi ngữ cảnh nếu đang ở ngữ cảnh Recipe).
- **Luồng thay thế (Alternative Flows):**
  - *AF-51.1 (Đóng hoặc chuyển đổi ngữ cảnh công thức):* Khi đang ở ngữ cảnh Recipe Post, người dùng có thể đóng ngữ cảnh công thức để chuyển khung chat về ngữ cảnh chung, hoặc chuyển sang xem bài công thức khác để nhận ngữ cảnh công thức mới mà không cần mở lại cửa sổ chat.
  - *AF-51.2 (Hết hạn mức AI trong ngày — Guest):* Khi Guest đạt 5/5 lượt thành công trong ngày, hệ thống khóa khung nhập câu hỏi, hiển thị thông báo hết hạn ngạch ngày và hiển thị nút Đăng ký tài khoản (FR-02, BR-01, BR-05).
  - *AF-51.3 (Hết hạn mức AI trong ngày — Member):* Khi Member đạt giới hạn theo gói (Free 5, Plus 15, Pro 50), hệ thống hiển thị thông báo đã đạt giới hạn ngày kèm hướng dẫn nâng cấp gói hoặc thông báo thời điểm làm mới lúc 00:00 `Asia/Ho_Chi_Minh` (FR-10, BR-01, BR-02).
  - *AF-51.4 (Member hỏi BMI cá nhân nhưng chưa khai báo hồ sơ dinh dưỡng):* Nếu Member hỏi về BMI cá nhân mà chưa hoàn thành khai báo tại FR-35, Chatbot giải thích công thức tính BMI chuẩn tham khảo và hiển thị liên kết dẫn tới trang Hồ sơ dinh dưỡng để Member tự tính toán.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-51.1 (Yêu cầu chẩn đoán bệnh tật hoặc kê đơn điều trị y khoa):* Nếu câu hỏi chứa yêu cầu chẩn đoán triệu chứng hoặc chữa bệnh qua ăn chay, Chatbot tuân thủ ranh giới an toàn, lịch sự từ chối đưa ra kết luận bệnh lý hay phác đồ điều trị, và hướng dẫn người dùng tham vấn ý kiến bác sĩ chuyên khoa hoặc chuyên gia y tế (SRS 3.15, BR-09, BR-41).
  - *EF-51.2 (Lỗi kết nối dịch vụ AI hoặc quá thời gian phản hồi):* Nếu dịch vụ AI gặp sự cố kết nối, lỗi kỹ thuật hoặc quá thời gian phản hồi quy định tại NFR-03 (NFR-18), hệ thống hiển thị thông báo sự cố kỹ thuật thân thiện và **TUYỆT ĐỐI KHÔNG TRỪ HẠN MỨC** của người dùng (BR-04).
  - *EF-51.3 (Yêu cầu AI chỉnh sửa dữ liệu hệ thống hoặc dữ liệu dinh dưỡng):* Nếu người dùng yêu cầu Chatbot sửa đổi bài viết, thêm món vào Lịch ăn, sửa hồ sơ cá nhân hay thay đổi giá trị trong danh mục dinh dưỡng, Chatbot giải thích rõ rằng AI chỉ đóng vai trò tư vấn thông tin trong hội thoại, không có thẩm quyền sửa đổi dữ liệu người dùng và không được phép can thiệp vào danh mục dinh dưỡng chính thức (BR-51).
  - *SF-51.1 (Bảo vệ thông tin xác thực backend và phòng chống lạm dụng prompt):* Khóa truy cập dịch vụ AI được lưu trữ và kiểm soát bảo mật tại backend (BR-06); hệ thống kiểm duyệt và làm sạch dữ liệu đầu vào nhằm ngăn chặn các hành vi tấn công vượt rào an toàn (Prompt Injection) (NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Người dùng nhận được câu trả lời an toàn, phù hợp ngữ cảnh và dễ hiểu.
- Bộ đếm số lượt AI thành công trong ngày của Guest (theo cookie/IP theo BR-01) hoặc Member (theo tài khoản theo BR-01, BR-02) tăng thêm 1 lượt (BR-03).
- Dữ liệu đo lường kỹ thuật (telemetry) được lưu trữ ẩn danh không chứa nội dung câu hỏi thô theo thời hạn 90 ngày (FR-11, BR-04, NFR-22).
- Không có bất kỳ thay đổi nào xảy ra đối với cơ sở dữ liệu bài viết, thực đơn tuần hay hồ sơ của người dùng.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Cả Guest (theo FR-02) và Member (theo FR-10) đều có quyền sử dụng cùng một AI Chatbot này; mỗi nhóm tuân thủ hạn ngạch và ranh giới xác thực riêng.
- **Ràng buộc an toàn & Toàn vẹn:** Ranh giới y tế tuyệt đối (BR-09, BR-41); cấm bịa đặt thông tin (NFR-25); AI chỉ tư vấn và không tự thực hiện thao tác ghi/sửa dữ liệu nghiệp vụ của người dùng; tuyệt đối không tạo hoặc thay đổi giá trị trong danh mục dinh dưỡng chính thức (BR-51); tuân thủ đúng `dietaryType` và `allergiesAndRestrictions`; tách biệt rõ nhãn AI với nội dung của tác giả.

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Hạn mức text AI cho Guest và tài khoản Free (5 lượt/ngày).
  - [BR-02](BUSINESS-RULES.md#br-02): Hạn mức text AI cho gói Plus và Pro (15 và 50 lượt/ngày).
  - [BR-03](BUSINESS-RULES.md#br-03): Điều kiện trừ hạn mức AI (chỉ trừ khi thành công).
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider và timeout AI (không trừ lượt khi lỗi).
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest (không lưu lịch sử theo tài khoản).
  - [BR-06](BUSINESS-RULES.md#br-06): Bảo mật Gemini API Key.
  - [BR-09](BUSINESS-RULES.md#br-09): Ranh giới y tế và sức khỏe của câu trả lời AI.
  - [BR-39](BUSINESS-RULES.md#br-39): Vai trò tham khảo của chỉ số BMI.
  - [BR-41](BUSINESS-RULES.md#br-41): Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia.
  - [BR-51](BUSINESS-RULES.md#br-51): Thẩm quyền quản lý danh mục nguyên liệu dinh dưỡng (AI không được tạo/sửa giá trị dinh dưỡng chính thức).
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03): Thời gian phản hồi của tính năng AI Chatbot.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.
  - [NFR-12](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-12): Giao diện Chatbot AI trực quan, dễ sử dụng.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.
  - [NFR-18](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-18): Cơ chế dự phòng khi dịch vụ AI bị lỗi hoặc timeout.
  - [NFR-20](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20): Cơ chế bảo vệ dữ liệu sức khỏe cá nhân và ranh giới thông tin.
  - [NFR-22](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-22): Giới hạn lưu trữ Prompt và Lịch sử trò chuyện AI.
  - [NFR-25](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-25): Đánh giá chất lượng và tuân thủ của nội dung AI.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-51.1 (Trả lời câu hỏi theo ngữ cảnh Recipe Post đang xem):**
  - *Given* người dùng (Guest hoặc Member còn hạn ngạch) đang xem bài công thức công khai "Đậu hũ sốt cà chua" (khẩu phần 2 người, 4 bước làm, thời gian nấu 25 phút) và bấm "Hỏi AI về công thức này",
  - *When* người dùng gửi câu hỏi "Món này nấu mất bao lâu và cần những gia vị gì?",
  - *Then* Chatbot trả lời chính xác dựa trên dữ liệu ngữ cảnh của bài (25 phút, danh sách gia vị trong bài) và hiển thị nhãn AI phân biệt với nội dung của tác giả.
- **AC-51.2 (Giải thích chi tiết bước chuẩn bị/chế biến và minh bạch thông tin không có trong bài):**
  - *Given* người dùng đang ở ngữ cảnh bài công thức có bước "Chiên nấm đùi gà vàng đều" mà tác giả không ghi rõ mức lửa hoặc số phút cụ thể,
  - *When* người dùng hỏi "Bước chiên nấm này nên để lửa mức nào và trong bao lâu?",
  - *Then* Chatbot nêu rõ công thức gốc của tác giả không chỉ định thời gian hay mức lửa cụ thể, đồng thời cung cấp gợi ý ước tính mang tính tham khảo kỹ thuật (ví dụ: nên để lửa vừa, quan sát nấm xém vàng 2 mặt khoảng 3–5 phút) và phân biệt rõ đây là tư vấn bổ trợ của AI chứ không phải dữ kiện gốc của tác giả.
- **AC-51.3 (Gợi ý nguyên liệu thay thế phù hợp với trường phái ăn chay):**
  - *Given* người dùng hỏi giải pháp thay thế nguyên liệu trong một món ăn chay (ví dụ: thay thế sữa bò trong công thức Lacto-Vegetarian sang lựa chọn thuần chay Vegan, hoặc thay thế trứng trong bánh ngọt chay),
  - *When* Chatbot phân tích và phản hồi,
  - *Then* câu trả lời đề xuất các lựa chọn thực vật phù hợp với đúng trường phái ăn chay yêu cầu (ví dụ: sữa đậu nành, nước luộc đậu gà aquafaba thay thế trứng) kèm tỷ lệ quy đổi và lưu ý kỹ thuật, tuyệt đối không đề xuất nguyên liệu vi phạm dietaryType đã chỉ định hoặc danh mục nguyên liệu kiêng kỵ của người dùng.
- **AC-51.4 (Từ chối chẩn đoán và phác đồ điều trị y khoa):**
  - *Given* người dùng đặt câu hỏi yêu cầu Chatbot chẩn đoán triệu chứng bệnh hoặc hướng dẫn chế độ ăn điều trị dứt điểm bệnh lý,
  - *When* Chatbot phân tích câu hỏi,
  - *Then* hệ thống từ chối đưa ra kết luận chẩn đoán hoặc phác đồ điều trị y khoa và hiển thị khuyến nghị người dùng tham vấn ý kiến bác sĩ chuyên khoa hoặc chuyên gia dinh dưỡng y tế (BR-09, BR-41).
- **AC-51.5 (Nhất quán một AI Chatbot duy nhất cho cả Guest và Member):**
  - *Given* Guest và Member cùng trải nghiệm đặt câu hỏi cho AI Chatbot,
  - *When* hệ thống tiếp nhận và xử lý request,
  - *Then* cả hai đều tương tác với cùng một mô hình AI Chatbot duy nhất (FR-51); Guest được kiểm soát 5 lượt/ngày qua cookie/IP (FR-02, BR-01) và Member được kiểm soát theo tài khoản (5/15/50 theo FR-10, BR-01, BR-02); chỉ request thành công mới trừ 1 lượt (BR-03).
- **AC-51.6 (Chatbot không tự ý sửa đổi Recipe Post hay dữ liệu hệ thống):**
  - *Given* người dùng trong phiên chat yêu cầu "Hãy sửa bước 3 của bài viết này ngắn lại" hoặc "Thêm món này vào lịch ăn của tôi",
  - *When* Chatbot xử lý,
  - *Then* Chatbot từ chối thao tác ghi dữ liệu, nêu rõ AI chỉ đóng vai trò tư vấn thông tin và hướng dẫn người dùng tự thao tác nếu họ là tác giả bài viết hoặc chủ sở hữu lịch ăn.
- **AC-51.7 (Bảo vệ hạn mức khi dịch vụ AI gặp sự cố hoặc quá thời gian phản hồi):**
  - *Given* người dùng gửi câu hỏi nhưng dịch vụ AI bên ngoài gặp lỗi kết nối hoặc quá thời gian phản hồi quy định tại NFR-03,
  - *When* hệ thống xử lý ngoại lệ,
  - *Then* hệ thống hiển thị thông báo lỗi kỹ thuật thân thiện và số dư hạn mức AI của người dùng hoàn toàn không bị trừ (BR-04, NFR-18).

---

<a id="fr-52"></a>
### FR-52 — Tạo, sửa, xóa Blog văn xuôi và nhúng thẻ Recipe Post

- **Mã yêu cầu:** FR-52
- **Module:** M01, M03, M12
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Mô tả:** Hệ thống cho Member tạo/sửa/xóa bài Blog văn xuôi chia sẻ kinh nghiệm ăn chay và nhúng thẻ Recipe Post công khai; Guest xem bài blog; Admin kiểm duyệt.

---

<a id="fr-53"></a>
### FR-53 — Tạo và quản lý Shopping List checklist tương tác

- **Mã yêu cầu:** FR-53
- **Module:** M05
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống cho phép Member đã đăng nhập tạo và quản lý Danh sách mua sắm (Shopping List) phục vụ đi chợ hoặc mua sắm nguyên liệu; nguồn dữ liệu tạo danh sách có thể được sinh tự động từ Lịch ăn tuần (Meal Plan - FR-09) hoặc từ một hay nhiều bài công thức (Recipe Post) do người dùng chọn lọc; hỗ trợ người dùng tự thêm các mặt hàng mua sắm thủ công (custom items), chỉnh sửa thông tin số lượng/đơn vị hoặc xóa bớt các mục không cần thiết; hiển thị danh sách dưới dạng checklist tương tác cho phép đánh dấu (tick) đã mua hoặc bỏ đánh dấu; lưu trữ bền vững trạng thái checklist theo tài khoản người dùng (BR-32).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập tài khoản hợp lệ (FR-03, BR-05, BR-32).
  - Nguồn tạo:
    1. Sinh từ Lịch ăn tuần (toàn bộ tuần hoặc các ngày được tích chọn trong FR-09).
    2. Sinh từ các bài Recipe Post cụ thể được chọn trong Khám phá (FR-17) hoặc Công thức đã lưu (FR-32).
  - Tính năng quản lý tương tác: Thêm item thủ công ngoài công thức, sửa/xóa item, tick checklist đánh dấu đã mua/chưa mua.
- **Phân loại Actor:**
  - Primary Actor: `Member` (người tạo và quản lý danh sách mua sắm).
  - Supporting Actor: `Hệ thống quản lý Danh sách mua sắm`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-53.1`: Tạo mới danh sách mua sắm từ Lịch ăn tuần hoặc từ các công thức được chọn.
  - `UC-53.2`: Thêm mục hàng hóa mua sắm thủ công ngoài công thức, chỉnh sửa hoặc xóa mục.
  - `UC-53.3`: Đánh dấu đã mua hoặc bỏ đánh dấu các mục dạng checklist tương tác trên thiết bị.
- **User Stories:**
  - *Là một người nội trợ*, tôi muốn tạo nhanh danh sách đi chợ từ thực đơn tuần đã lên kế hoạch, có thể tự thêm các đồ gia dụng lặt vặt và tick chọn từng món đã bỏ vào giỏ hàng ngay trên điện thoại khi đi chợ.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản Member hợp lệ (FR-03, BR-32).
- **Kích hoạt (Trigger):**
  - Member nhấn nút "Tạo danh sách mua sắm" trên trang Lịch ăn tuần hoặc chọn "Thêm vào danh sách mua sắm" từ các bài công thức.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Member chọn nguồn tạo danh sách mua sắm: chọn phạm vi ngày trong Lịch ăn tuần (FR-09) hoặc chọn các bài công thức cụ thể.
  - Bước 2: Member nhấn "Tạo danh sách mua sắm".
  - Bước 3: Hệ thống trích xuất toàn bộ nguyên liệu từ các bài công thức tương ứng theo số khẩu phần đã thiết lập trong lịch (FR-37).
  - Bước 4: Hệ thống áp dụng quy tắc gộp nguyên liệu tự động an toàn (FR-54) để gom nhóm các nguyên liệu trùng lặp.
  - Bước 5: Hệ thống tạo danh sách mua sắm mới và hiển thị trên giao diện dạng Checklist tương tác:
    - Mỗi mục hàng hiển thị: Tên nguyên liệu, Định lượng tổng hợp, Đơn vị đo, Hộp kiểm (Checkbox) trạng thái, và Danh mục thực phẩm.
  - Bước 6: Member nhấn nút "Thêm mặt hàng khác" để tự gõ thêm các đồ cần mua phát sinh (ví dụ: nước rửa bát, dầu ăn chay, giấy ăn); hệ thống bổ sung ngay vào danh sách.
  - Bước 7: Trong lúc đi chợ, Member chạm/click vào hộp kiểm bên cạnh món hàng đã mua; hệ thống chuyển trạng thái mục đó sang `Đã mua` (gạch ngang văn bản nhẹ nhàng và đổi màu xám) và lưu trạng thái vào cơ sở dữ liệu.
- **Luồng thay thế (Alternative Flows):**
  - *AF-53.1 (Bỏ đánh dấu đã mua):* Member chạm lại vào mục đã mua; hệ thống bỏ gạch ngang và chuyển trạng thái về `Chưa mua`.
  - *AF-53.2 (Chỉnh sửa hoặc xóa mục):* Member nhấn nút sửa để đổi số lượng/đơn vị, hoặc nhấn nút xóa để gỡ một mặt hàng ra khỏi danh sách mua sắm.
  - *AF-53.3 (Xóa các mục đã mua):* Member nhấn nút "Dọn dẹp danh sách" để xóa nhanh toàn bộ các mặt hàng đã được tick hoàn thành.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-53.1 (Lịch ăn trống không có món nào):* Nếu tuần được chọn chưa có bất kỳ món ăn nào, hệ thống thông báo: *"Chưa có món ăn trong lịch ăn để tạo danh sách mua sắm"* và hướng dẫn thêm món vào lịch ăn trước (FR-09).
  - *SF-53.1 (Làm sạch đầu vào mục thủ công):* Tên các mặt hàng do Member tự nhập thủ công bắt buộc được lọc sạch mã độc XSS trước khi lưu và hiển thị (NFR-10).
  - *SF-53.2 (Kiểm soát phân quyền danh sách cá nhân):* Danh sách mua sắm thuộc quyền sở hữu riêng của Member; tài khoản khác không có quyền xem hoặc chỉnh sửa (NFR-08, NFR-09).

#### 5. Hậu điều kiện (Postconditions)
- Danh sách mua sắm được lưu trữ và cập nhật trạng thái checklist bền vững.
- Dữ liệu sẵn sàng phục vụ tính năng Sao chép Clipboard và Xuất file văn bản (FR-55).

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Dành riêng cho Member đã đăng nhập (BR-05, BR-32).
- **Ràng buộc kỹ thuật:** Checklist tương tác cập nhật mượt mà; tối ưu hóa thao tác một tay trên màn hình điện thoại khi đi chợ (NFR-13).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-14](BUSINESS-RULES.md#br-14): Quy tắc định lượng và tổng hợp Shopping List an toàn.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Lịch ăn và Shopping List.
  - [BR-33](BUSINESS-RULES.md#br-33): Thao tác lưu công thức không tiêu thụ hạn mức AI.
  - [BR-35](BUSINESS-RULES.md#br-35): Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến (chống XSS).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt, tối ưu trên thiết bị di động.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-53.1 (Tạo danh sách mua sắm từ Lịch ăn tuần thành công):**
  - *Given* Lịch ăn tuần của Member có các món ăn đã được xếp vào bữa,
  - *When* Member nhấn nút tạo danh sách mua sắm,
  - *Then* hệ thống trích xuất đầy đủ các nguyên liệu cần thiết và hiển thị bảng checklist mua sắm hoàn chỉnh.
- **AC-53.2 (Thêm mặt hàng thủ công ngoài công thức):**
  - *Given* Member đang xem danh sách mua sắm,
  - *When* Member nhập thêm mặt hàng "Khăn giấy lau bếp" số lượng "2 cuộn" và nhấn thêm,
  - *Then* mặt hàng mới xuất hiện ngay trong danh sách mua sắm với trạng thái chưa mua.
- **AC-53.3 (Chuyển đổi trạng thái checklist đã mua/chưa mua tương tác):**
  - *Given* một mục hàng đang ở trạng thái chưa mua trong danh sách,
  - *When* Member chạm vào hộp kiểm của mục đó,
  - *Then* mục hàng chuyển sang trạng thái đã mua (gạch ngang chữ) và lưu lại trạng thái bền vững.
- **AC-53.4 (Chỉnh sửa và xóa mục hàng trong danh sách):**
  - *Given* một mục hàng có sẵn trong danh sách mua sắm,
  - *When* Member thực hiện sửa số lượng hoặc nhấn xóa mục đó,
  - *Then* hệ thống cập nhật đúng số lượng mới hoặc gỡ bỏ hoàn toàn mục đó khỏi danh sách.
- **AC-53.5 (Yêu cầu đăng nhập đối với danh sách mua sắm):**
  - *Given* người dùng là Guest chưa đăng nhập,
  - *When* cố gắng truy cập tính năng danh sách mua sắm,
  - *Then* hệ thống chặn truy cập và hiển thị thông báo yêu cầu đăng nhập.

---

<a id="fr-54"></a>
### FR-54 — Tự động tổng hợp nguyên liệu trùng an toàn trong Shopping List

- **Mã yêu cầu:** FR-54
- **Module:** M05
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống tự động tính toán và tổng hợp (gộp) các dòng nguyên liệu trùng lặp khi tạo danh sách mua sắm theo bộ quy tắc định lượng an toàn tuyệt đối: hệ thống CHỈ tổng hợp các dòng nguyên liệu khi chúng có cùng mã định danh nguyên liệu chuẩn (`ingredientId`) (BR-14, BR-49); cho phép quy đổi an toàn giữa gam và kilôgam ($g \leftrightarrow kg$) và giữa mililít và lít ($ml \leftrightarrow l$); các đơn vị đo khác chỉ được gom lại khi tên đơn vị giống hệt nhau; TUYỆT ĐỐI KHÔNG suy diễn giữa khối lượng và thể tích (mass $\leftrightarrow$ volume) hoặc giữa số lượng cái/quả/củ và khối lượng (piece $\leftrightarrow$ mass/volume); định lượng ghi "vừa đủ" (to taste) giữ nguyên dạng văn bản phi số lượng, không cộng dồn số học; các dòng nguyên liệu không tương thích đơn vị được giữ riêng biệt và phân nhóm theo danh mục thực phẩm có sẵn.
- **Phạm vi nghiệp vụ:**
  - Quy tắc tổng hợp nguyên liệu:
    1. **Quy tắc cùng bản thể:** Bắt buộc có cùng `ingredientId`.
    2. **Quy tắc quy đổi khối lượng:** $1.000\text{g} = 1\text{kg}$ (tự động cộng dồn và đưa về đơn vị phù hợp).
    3. **Quy tắc quy đổi thể tích:** $1.000\text{ml} = 1\text{l}$ (tự động cộng dồn và đưa về đơn vị phù hợp).
    4. **Quy tắc đơn vị đếm được/định lượng khác:** Chỉ gộp khi trùng khớp hoàn toàn tên đơn vị (ví dụ: *2 quả* + *3 quả* = *5 quả*).
    5. **Cấm tuyệt đối suy diễn bất đối xứng:**
       - Không quy đổi $g \leftrightarrow ml$ (khối lượng $\leftrightarrow$ thể tích).
       - Không quy đổi quả/củ/bìa sang gram hay ml.
       - "Vừa đủ" giữ nguyên chuỗi văn bản, không gán số 0, không cộng dồn.
    6. **Phân nhóm hiển thị:** Các dòng không tương thích được giữ thành các mục độc lập và hiển thị gom nhóm theo Danh mục thực phẩm (Rau củ, Nấm, Đậu & Chế phẩm, Ngũ cốc, Gia vị...).
- **Phân loại Actor:**
  - Primary Actor: `Member` (người cần danh sách nguyên liệu tổng hợp chính xác để đi chợ).
  - Supporting Mechanism / System: `Hệ thống tính toán và tổng hợp Shopping List` (cơ chế tính toán và gom gộp tự động).

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-54.1`: Xem danh sách nguyên liệu mua sắm được tổng hợp tự động an toàn theo đúng đơn vị đo chuẩn.
  - `UC-54.2`: Xem các dòng nguyên liệu có đơn vị không tương thích được tách dòng minh bạch và phân nhóm theo danh mục.
- **User Stories:**
  - *Là một người đi chợ*, tôi muốn danh sách mua sắm tự động cộng dồn 300g đậu phụ ở món này với 200g đậu phụ ở món khác thành 500g đậu phụ, nhưng không được tự ý đổi "2 quả cà chua" sang gram để tôi mua đúng số lượng thực tế cần dùng.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Hệ thống tiếp nhận tập hợp các nguyên liệu cần mua từ Lịch ăn tuần hoặc các bài công thức được chọn (FR-53).
- **Kích hoạt (Trigger):**
  - Tiến trình tạo hoặc cập nhật Danh sách mua sắm được kích hoạt.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Hệ thống đọc toàn bộ danh sách các dòng nguyên liệu nguồn kèm `ingredientId`, số lượng, đơn vị đo và danh mục thực phẩm.
  - Bước 2: Hệ thống gom các dòng có cùng `ingredientId` thành từng nhóm xử lý riêng biệt.
  - Bước 3: Trong mỗi nhóm nguyên liệu cùng `ingredientId`:
    - *Trường hợp 1 (Đơn vị khối lượng g / kg):* Hệ thống quy đổi toàn bộ về gam ($1\text{kg} = 1.000\text{g}$), cộng dồn tổng số gam. Nếu tổng $\ge 1.000\text{g}$, hệ thống quy đổi hiển thị thành dạng kilôgam (ví dụ: $1.250\text{g} \rightarrow 1,25\text{kg}$).
    - *Trường hợp 2 (Đơn vị thể tích ml / l):* Hệ thống quy đổi toàn bộ về mililít ($1\text{l} = 1.000\text{ml}$), cộng dồn tổng số ml. Nếu tổng $\ge 1.000\text{ml}$, hệ thống quy đổi hiển thị thành lít (ví dụ: $1.500\text{ml} \rightarrow 1,5\text{l}$).
    - *Trường hợp 3 (Đơn vị cùng loại khác):* Nếu các dòng có cùng tên đơn vị đo (như "quả", "bìa", "muỗng cà phê"), hệ thống cộng dồn phần số lượng theo đơn vị đó.
  - Bước 4: Nếu trong nhóm xuất hiện các dòng có đơn vị không tương thích nhau (ví dụ: một bài dùng "200g nấm rơm", một bài khác ghi "1 chén nấm rơm"):
    - Hệ thống KHÔNG tự ý quy đổi mass $\leftrightarrow$ volume (BR-14).
    - Hệ thống giữ tách biệt thành 2 dòng riêng biệt: "Nấm rơm: 200g" và "Nấm rơm: 1 chén".
  - Bước 5: Nếu dòng nguyên liệu có định lượng "vừa đủ":
    - Hệ thống không cộng dồn số học.
    - Giữ một dòng hiển thị riêng: "Tên nguyên liệu: vừa đủ".
  - Bước 6: Hệ thống phân bổ các dòng nguyên liệu sau khi tổng hợp vào các Danh mục thực phẩm tương ứng (Rau củ, Đậu hạt, Gia vị...).
  - Bước 7: Trả về kết quả danh sách mua sắm đã tổng hợp hoàn tất cho giao diện hiển thị (FR-53).
- **Luồng thay thế (Alternative Flows):**
  - *AF-54.1 (Nguyên liệu tự do không có ingredientId):* Đối với các nguyên liệu do tác giả tự gõ không liên kết chuẩn (`ingredientId = null` theo FR-19), hệ thống chỉ gom các dòng có chuỗi tên hiển thị giống hệt nhau (không phân biệt hoa thường) và có cùng đơn vị đo lường; nếu khác đơn vị, giữ tách dòng độc lập.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-54.1 (Định lượng không hợp lệ hoặc số âm):* Nếu phát hiện dòng nguyên liệu nguồn có số lượng $\le 0$, hệ thống bỏ qua phần số âm và ghi nhận cảnh báo dữ liệu.
  - *SF-54.1 (Đảm bảo tính chính xác định lượng):* Thuật toán sử dụng kiểu dữ liệu số thực chính xác cao để tính toán cộng dồn, tránh sai số làm tròn khi quy đổi đơn vị (NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Danh sách mua sắm được tối ưu hóa số lượng dòng, hiển thị trực quan và an toàn.
- Người dùng nắm bắt chính xác khối lượng cần mua mà không bị nhầm lẫn định lượng.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Hệ thống thực thi tự động; phục vụ cho Member sở hữu danh sách mua sắm.
- **Ràng buộc an toàn:** Tuyệt đối cấm suy diễn mass $\leftrightarrow$ volume hoặc piece $\leftrightarrow$ mass/volume (BR-14).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-14](BUSINESS-RULES.md#br-14): Quy tắc định lượng và tổng hợp Shopping List an toàn.
  - [BR-48](BUSINESS-RULES.md#br-48): Xử lý nguyên liệu thiếu định lượng hoặc thiếu số liệu dinh dưỡng.
  - [BR-49](BUSINESS-RULES.md#br-49): Không gọi API dinh dưỡng ngoài realtime và nguồn tham khảo.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗi tính toán và lỗ hổng phần mềm.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-54.1 (Quy đổi và tổng hợp an toàn gam sang kilôgam):**
  - *Given* danh sách có Món 1 dùng 600g Đậu phụ và Món 2 dùng 800g Đậu phụ (cùng `ingredientId`),
  - *When* hệ thống thực hiện tổng hợp danh sách mua sắm,
  - *Then* hệ thống gộp thành một dòng duy nhất "Đậu phụ: 1,4 kg" (hoặc "1.400 g").
- **AC-54.2 (Quy đổi và tổng hợp an toàn mililít sang lít):**
  - *Given* danh sách có Món 1 dùng 500ml Nước cốt dừa và Món 2 dùng 750ml Nước cốt dừa,
  - *When* hệ thống thực hiện tổng hợp,
  - *Then* hệ thống gộp thành một dòng duy nhất "Nước cốt dừa: 1,25 l" (hoặc "1.250 ml").
- **AC-54.3 (Cấm tự ý quy đổi giữa khối lượng và thể tích):**
  - *Given* danh sách có Món A dùng "300g Nước tương" và Món B dùng "200ml Nước tương",
  - *When* hệ thống thực hiện tổng hợp,
  - *Then* hệ thống giữ thành 2 dòng riêng biệt: "Nước tương: 300g" và "Nước tương: 200ml" mà không tự ý cộng dồn thành 500g hay 500ml.
- **AC-54.4 (Cấm tự ý quy đổi số lượng cái/quả sang khối lượng):**
  - *Given* danh sách có Món 1 dùng "2 quả Cà chua" và Món 2 dùng "300g Cà chua",
  - *When* hệ thống thực hiện tổng hợp,
  - *Then* hệ thống giữ thành 2 dòng riêng biệt: "Cà chua: 2 quả" và "Cà chua: 300g".
- **AC-54.5 (Giữ nguyên định lượng 'vừa đủ' không cộng số):**
  - *Given* các món ăn đều có nguyên liệu "Tiêu đen: vừa đủ",
  - *When* hệ thống thực hiện tổng hợp danh sách mua sắm,
  - *Then* hệ thống chỉ hiển thị một dòng "Tiêu đen: vừa đủ" và không gán bất kỳ giá trị số học nào.

---

<a id="fr-55"></a>
### FR-55 — Sao chép clipboard và xuất file text Shopping List

- **Mã yêu cầu:** FR-55
- **Module:** M05
- **Trạng thái (Derived):** ACTIVE

#### 1. Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp hai chức năng kết xuất và chia sẻ dữ liệu nhanh cho Danh sách mua sắm (Shopping List) dành cho Member:
  1. **Sao chép vào Clipboard (Copy to Clipboard):** Cho phép người dùng nhấn một nút để sao chép toàn bộ danh sách mua sắm hiện tại vào khay nhớ tạm của thiết bị dưới dạng văn bản có cấu trúc phân nhóm rõ ràng, kèm trạng thái `[ ]` chưa mua hoặc `[x]` đã mua; hiển thị thông báo phản hồi thao tác thành công.
  2. **Xuất tệp văn bản thuần túy (Export as `.txt` Checklist):** Cho phép người dùng tải về tệp tin định dạng văn bản `.txt` chứa toàn bộ danh sách mua sắm dạng checklist để lưu trữ offline, gửi qua tin nhắn, hoặc in ấn ra giấy khi đi chợ.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập và đang quản lý danh sách mua sắm (FR-03, FR-53).
  - Định dạng kết xuất: Văn bản UTF-8 rõ ràng, phân nhóm theo danh mục thực phẩm (Rau củ, Nấm, Đậu hạt, Gia vị...), mỗi dòng có ký hiệu trạng thái checklist (`[ ]` hoặc `[x]`), tên nguyên liệu và định lượng tổng hợp.
- **Phân loại Actor:**
  - Primary Actor: `Member` (người xuất hoặc sao chép danh sách mua sắm).
  - Supporting Actor: `Hệ thống kết xuất dữ liệu văn bản`.

#### 2. Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-55.1`: Sao chép nhanh toàn bộ nội dung danh sách mua sắm vào Clipboard thiết bị.
  - `UC-55.2`: Xuất và tải xuống tệp tin văn bản thuần túy `.txt` dạng checklist mua sắm.
- **User Stories:**
  - *Là một người nội trợ*, tôi muốn sao chép nhanh danh sách đi chợ vào Zalo gửi cho người thân hoặc tải về file text để in ra giấy khi đi siêu thị mà không cần phải mở ứng dụng liên tục.

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Member đã đăng nhập và danh sách mua sắm hiện tại có ít nhất một mặt hàng (FR-53).
- **Kích hoạt (Trigger):**
  - Member nhấn nút "Sao chép vào Clipboard" hoặc "Xuất file .txt" trên thanh công cụ của trang Danh sách mua sắm.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow - Sao chép Clipboard):**
  - Bước 1: Member nhấn nút "Sao chép vào Clipboard" trên giao diện Danh sách mua sắm.
  - Bước 2: Hệ thống định dạng toàn bộ các mặt hàng trong danh sách thành một chuỗi văn bản UTF-8 chuẩn mực:
    - Dòng tiêu đề: `DANH SÁCH ĐI CHỢ CHAY - [Ngày tạo / Tuần]`
    - Phân chia theo từng Danh mục thực phẩm:
      ```text
      --- RAU CỦ & NẤM ---
      [ ] Nấm rơm: 300g
      [x] Cà rốt: 2 củ
      --- ĐẬU & CHẾ PHẨM ---
      [ ] Đậu phụ mơ: 4 bìa
      ```
  - Bước 3: Hệ thống ghi chuỗi văn bản vào bộ nhớ tạm (Clipboard API) của thiết bị người dùng.
  - Bước 4: Giao diện hiển thị thông báo nổi (toast message): *"Đã sao chép danh sách mua sắm vào khay nhớ tạm!"*.
- **Luồng thay thế (Alternative Flows - Xuất file .txt):**
  - *AF-55.1 (Tải xuống file .txt):*
    - Bước 1: Member nhấn nút "Xuất file .txt".
    - Bước 2: Hệ thống đóng gói chuỗi văn bản checklist có cấu trúc ở trên thành tệp tin có phần mở rộng `.txt` với bộ mã ký tự UTF-8 (bảo đảm hiển thị đúng tiếng Việt có dấu trên mọi hệ điều hành).
    - Bước 3: Đặt tên tệp tin tự động theo quy chuẩn: `Danh_sach_mua_sam_YYYYMMDD.txt`.
    - Bước 4: Kích hoạt luồng tải xuống (download stream) trực tiếp về thiết bị của Member.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-55.1 (Danh sách mua sắm trống):* Nếu danh sách mua sắm chưa có mặt hàng nào, các nút sao chép và xuất file bị vô hiệu hóa kèm tooltip: *"Danh sách mua sắm đang trống"*.
  - *EF-55.2 (Trình duyệt chặn quyền Clipboard):* Nếu trình duyệt không cấp quyền truy cập Clipboard, hệ thống hiển thị hộp thoại chứa toàn bộ văn bản và nút "Chọn tất cả" để người dùng sao chép thủ công.
  - *SF-55.1 (Bảo vệ dữ liệu khi xuất file):* File văn bản xuất ra không chứa bất kỳ thông tin nhạy cảm nào ngoài danh sách các mặt hàng đi chợ (NFR-08).

#### 5. Hậu điều kiện (Postconditions)
- Dữ liệu danh sách mua sắm được sao chép vào Clipboard hoặc lưu thành tệp `.txt` an toàn trên thiết bị của Member.
- Dữ liệu gốc trong hệ thống không bị thay đổi.

#### 6. Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member sở hữu danh sách mua sắm mới có quyền xuất dữ liệu.
- **Ràng buộc kỹ thuật:** Định dạng văn bản UTF-8 chuẩn, hiển thị tốt tiếng Việt; thể hiện rõ ký hiệu `[ ]` và `[x]` (NFR-13).

#### 7. Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Shopping List.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### 8. Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-55.1 (Sao chép toàn bộ danh sách vào Clipboard thành công):**
  - *Given* danh sách mua sắm của Member có chứa 5 mặt hàng,
  - *When* Member nhấn nút "Sao chép vào Clipboard",
  - *Then* toàn bộ danh sách được ghi thành công vào Clipboard của thiết bị và hiển thị thông báo thành công.
- **AC-55.2 (Xuất và tải về tệp tin .txt định dạng checklist chuẩn):**
  - *Given* danh sách mua sắm có các mục thuộc danh mục Rau củ và Đậu hạt,
  - *When* Member nhấn nút "Xuất file .txt",
  - *Then* trình duyệt tải xuống tệp tin `.txt` mã hóa UTF-8 phân chia theo danh mục kèm ký hiệu `[ ]` và `[x]`.
- **AC-55.3 (Thể hiện rõ trạng thái đã mua và chưa mua trong file xuất):**
  - *Given* mặt hàng A ở trạng thái chưa mua và mặt hàng B ở trạng thái đã mua,
  - *When* xuất danh sách sang clipboard hoặc file text,
  - *Then* mặt hàng A hiển thị với tiền tố `[ ]` và mặt hàng B hiển thị với tiền tố `[x]`.
- **AC-55.4 (Vô hiệu hóa chức năng khi danh sách trống):**
  - *Given* danh sách mua sắm hiện tại không có mặt hàng nào,
  - *When* Member xem thanh công cụ,
  - *Then* các nút sao chép clipboard và xuất file .txt bị vô hiệu hóa.

---

<a id="fr-56"></a>
### FR-56 — Quản lý kho thực phẩm cá nhân và đề xuất món từ kho

- **Mã yêu cầu:** FR-56
- **Module:** M05, M06, M13
- **Trạng thái (Derived):** OUT_OF_SCOPE
- **Mô tả:** Hệ thống cho Member quản lý kho thực phẩm cá nhân (Pantry/Inventory); tự động tìm kiếm/đề xuất công thức phù hợp với nguyên liệu đang có và hỗ trợ AI gợi ý món mới từ nguyên liệu trong kho.
