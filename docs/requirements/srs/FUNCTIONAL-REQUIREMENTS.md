> **Document:** Functional Requirements
> **File:** `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md`
> **Version:** v2.45.0
> **Created:** 2026-09-14
> **Last Updated:** 2026-10-29
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Functional Requirements

This document defines the functional requirements in the current implementation baseline. Each requirement preserves its stable FR ID and acceptance criteria. Detailed actor-system flows are maintained in the linked Use Case Specifications. Related Business Rules and NFRs remain separately authoritative.

## Functional Requirements

<a id="fr-01"></a>
### FR-01 — Hệ thống cho Guest xem và tìm kiếm nội dung đã công khai

- **Mã yêu cầu:** FR-01
- **Module:** M01 (Guest & Public Browsing)
- **Tóm tắt yêu cầu:** Hệ thống cho phép người dùng chưa đăng nhập (Guest) duyệt xem danh sách, tìm kiếm và xem chi tiết toàn bộ các bài công thức chay đã công khai; không yêu cầu đăng nhập đối với việc đọc nội dung; khi Guest bấm vào các tác vụ cá nhân hóa hoặc tương tác thành viên thì hệ thống yêu cầu đăng nhập.

- **Use Case detail:** [Open interaction flows](../use-cases/discovery-and-public-content.md#fr-01).

#### Mục đích
Tạo phễu tiếp cận mở rộng nhằm giới thiệu kiến thức ẩm thực chay và lan tỏa phong cách sống tích cực tới cộng đồng; cho phép người dùng trải nghiệm giá trị cốt lõi của nền tảng trước khi đăng ký tài khoản; đồng thời thiết lập ranh giới phân quyền rõ ràng giữa việc xem nội dung công khai và việc tương tác cộng đồng/cá nhân hóa.

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-01.1`: Duyệt danh sách bài công thức công khai (Browse public recipes).
  - `UC-01.2`: Tìm kiếm công thức cơ bản theo từ khóa (Search public recipes by keyword).
  - `UC-01.3`: Xem chi tiết bài công thức công khai (View public recipe details).
- **User Stories:**
  - `US-01.1`: Là một Guest, tôi muốn lướt xem các món chay hấp dẫn trên trang chủ để tìm cảm hứng nấu ăn hàng ngày mà không bị bắt buộc phải tạo tài khoản trước.
  - `US-01.2`: Là một Guest, tôi muốn nhập từ khóa để tìm nhanh món ăn tôi đang quan tâm nhằm tham khảo nguyên liệu và cách làm.
  - `US-01.3`: Là một Guest, khi tôi bấm vào nút Lưu bài hoặc Bình luận, tôi muốn hệ thống giải thích rõ ràng đây là tính năng dành cho thành viên và hướng dẫn tôi đăng nhập/đăng ký.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Guest được phép xem toàn bộ các bài công thức ở trạng thái `PUBLISHED`.
- Hệ thống tuyệt đối không hiển thị cho Guest các bài viết đang ở trạng thái nháp (nếu có sau này), bài bị Quản trị viên ẩn do vi phạm, hoặc bài đã xóa.
- Guest được xem tỷ lệ % Like (`👍 {like_percentage}%`) và tổng số lượt bình chọn (`RECIPE_REACTION`), nhưng tuyệt đối không được gửi phản hồi Like hoặc Dislike (BR-69).
- Không cho phép Guest tạo, chỉnh sửa, xóa bài viết, gửi phản hồi Like/Dislike, gửi bình luận hay lưu dữ liệu cá nhân hóa (BR-05, BR-69).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn.
  - [BR-69](BUSINESS-RULES.md#br-69): Tương tác Like / Dislike và tỷ lệ hài lòng (% Like) của bài công thức (Guest chỉ xem, không được bình chọn).
  - [BR-70](BUSINESS-RULES.md#br-70): Ghi nhận lượt xem và khử trùng lặp theo phiên (`RECIPE_VIEW`).
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian phản hồi tìm kiếm công thức $\le 1.5$ giây (P95).
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn truy cập trái phép.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa thiết bị.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-01.1 — Guest duyệt danh sách bài công thức công khai thành công:**
  - **Given:** Guest truy cập trang Khám phá mà không đăng nhập.
  - **When:** Trang web tải hoàn tất.
  - **Then:** Hệ thống hiển thị danh sách các bài công thức đang công khai kèm tiêu đề, ảnh bìa (`RECIPE_MEDIA`), loại ăn chay, tỷ lệ % Like (`👍 {like_percentage}%` theo phong cách Samsung Food hoặc nhãn "Mới" nếu chưa có bình chọn), lượt xem (`RECIPE_VIEW`) và tác giả; có hỗ trợ phân trang và chọn 6 chế độ sắp xếp.

- **AC-01.2 — Guest tìm kiếm bài công thức theo từ khóa thành công:**
  - **Given:** Guest đang ở thanh tìm kiếm của ứng dụng.
  - **When:** Guest nhập từ khóa "canh chua" và gửi yêu cầu tìm kiếm.
  - **Then:** Hệ thống phản hồi danh sách các bài công thức chứa từ khóa "canh chua" với thời gian phản hồi $\le 1.5$ giây (P95).

- **AC-01.3 — Guest xem đầy đủ nội dung chi tiết bài công thức:**
  - **Given:** Một bài công thức hợp lệ đang ở trạng thái công khai (`PUBLISHED`).
  - **When:** Guest nhấp vào xem chi tiết bài công thức đó.
  - **Then:** Hệ thống hiển thị toàn bộ nội dung bao gồm tiêu đề, nguyên liệu định lượng chuẩn, hướng dẫn thực hiện chi tiết (`instructions`), tối đa 5 ảnh (`RECIPE_MEDIA`) kèm 1 ảnh bìa, video YouTube nhúng, tỷ lệ % Like và tổng số lượt Like/Dislike (`RECIPE_REACTION`), số lượt xem (`RECIPE_VIEW`) và thông tin tác giả; đồng thời ghi nhận 1 lượt xem nếu hợp lệ theo BR-70.

- **AC-01.4 — Không hiển thị bài viết vi phạm hoặc bài không công khai cho Guest:**
  - **Given:** Bài công thức đang bị Quản trị viên ẩn do vi phạm hoặc không tồn tại.
  - **When:** Guest cố tình truy cập bài viết đó qua đường dẫn trực tiếp.
  - **Then:** Hệ thống từ chối hiển thị và thông báo bài viết không khả dụng hoặc không tìm thấy.

- **AC-01.5 — Yêu cầu đăng nhập khi Guest thực hiện thao tác thành viên (kể cả bình chọn Like/Dislike):**
  - **Given:** Guest đang xem một bài công thức công khai.
  - **When:** Guest nhấn nút "Like", "Dislike", "Lưu bài viết" hoặc "Bình luận".
  - **Then:** Hệ thống không thực hiện lưu, bình chọn hay gửi bình luận, mà hiển thị thông báo yêu cầu đăng nhập và cung cấp liên kết chuyển đến màn hình Đăng nhập (BR-05, BR-32, BR-69).

---

---

<a id="fr-02"></a>
### FR-02 — Quyền Guest trải nghiệm AI Chatbot chung có giới hạn tần suất kỹ thuật

- **Mã yêu cầu:** FR-02
- **Module:** M06 (AI Assistant & Personalization)
- **Tóm tắt yêu cầu:** Hệ thống quản lý quyền và trải nghiệm của Guest khi tương tác với AI Chatbot chung của hệ thống ([FR-51](FUNCTIONAL-REQUIREMENTS.md#fr-51)) theo cơ chế bảo vệ tần suất kỹ thuật (Technical Rate Limiting):
  - **Quyền sử dụng:** Guest được trải nghiệm trực tiếp AI Chatbot cơ bản (FR-51) ở cả Ngữ cảnh chung (General Context) và Ngữ cảnh bài công thức (Recipe Context theo [FR-20](FUNCTIONAL-REQUIREMENTS.md#fr-20)) đối với các Recipe Post đang công khai mà không bắt buộc đăng nhập (BR-01).
  - **Giới hạn tần suất kỹ thuật (Rate Limiting):** Áp dụng giới hạn tần suất kỹ thuật (ví dụ: tối đa 10 request / phút theo anonymous cookie kết hợp IP) để chống spam và ngăn chặn cạn kiệt tài nguyên máy chủ; không áp dụng quota tính theo số lượt/ngày và không có chu kỳ reset 00:00 (BR-01).
  - **Giới hạn trải nghiệm:** Guest không có lịch sử hội thoại lưu theo tài khoản và không có hồ sơ dinh dưỡng cá nhân (BR-05). Guest không phải là một thực thể trong cơ sở dữ liệu.
  - **Toàn bộ năng lực và luồng tương tác:** Hành vi hội thoại, áp dụng chỉ dẫn an toàn và phản hồi câu hỏi do [FR-51](FUNCTIONAL-REQUIREMENTS.md#fr-51) sở hữu và điều phối; thời gian phản hồi của AI Chatbot phải đáp ứng [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03).

- **Use Case detail:** [Open interaction flows](../use-cases/ai-and-personalization.md#fr-02).

#### Mục đích
Cho phép người dùng vãng lai (Guest) trải nghiệm trực tiếp năng lực hỗ trợ thông minh của AI Chatbot (FR-51) trên nền tảng Mâm Xanh; chứng minh giá trị thực tế nhằm thu hút người dùng đăng ký tài khoản thành viên chính thức; đồng thời kiểm soát chi phí hạ tầng và ngăn ngừa hành vi tấn công từ chối dịch vụ thông qua cơ chế giới hạn tần suất kỹ thuật.

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-02.1`: Trải nghiệm dùng thử AI Chatbot chung cho Guest có giới hạn tần suất kỹ thuật (Try unified AI chatbot as guest).
- **User Stories:**
  - `US-02.1`: Là một Guest đang tìm hiểu về ăn chay hoặc xem một bài công thức công khai, tôi muốn hỏi thử AI (về kỹ thuật nấu nướng chung hoặc về món ăn đang xem) để trải nghiệm chất lượng hỗ trợ trước khi quyết định đăng ký tài khoản.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Nhận diện Guest qua anonymous cookie kết hợp coarse IP protection theo BR-01 nhằm ngăn ngừa hành vi spam bot và lạm dụng API.
- Dữ liệu đầu vào của câu hỏi được kiểm soát và làm sạch theo tiêu chuẩn an toàn bảo mật (NFR-10).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Quyền sử dụng AI Chat cho Guest và Member Free (bảo vệ bằng Technical Rate Limiting).
  - [BR-03](BUSINESS-RULES.md#br-03): Xác thực quyền tính năng trước khi gọi dịch vụ AI.
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider, timeout AI và lưu trữ telemetry.
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03): Thời gian phản hồi của AI Chatbot.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến và kiểm soát đầu vào.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt.
  - [NFR-18](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-18): Cơ chế dự phòng khi dịch vụ Gemini AI bị lỗi hoặc timeout.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-02.1 — Guest dùng thử AI thành công trong ngưỡng tần suất:**
  - **Given:** Guest gửi yêu cầu tới AI Chatbot và chưa vượt ngưỡng rate limiting kỹ thuật.
  - **When:** Guest gửi câu hỏi hợp lệ tới AI Chatbot FR-51 (ở ngữ cảnh chung hoặc ngữ cảnh Recipe Post đang xem).
  - **Then:** Hệ thống xử lý qua FR-51 và phản hồi câu trả lời thành công đáp ứng thời gian quy định tại NFR-03.

- **AC-02.2 — Áp dụng Rate Limit kỹ thuật khi Guest gửi request quá nhanh:**
  - **Given:** Guest gửi liên tiếp nhiều request vượt quá ngưỡng kỹ thuật cho phép (ví dụ $> 10$ req/phút).
  - **When:** Hệ thống nhận request tiếp theo từ IP/cookie đó,
  - **Then:** Hệ thống trả về mã lỗi HTTP 429 và thông báo tạm dừng thao tác thay vì chuyển tiếp tới dịch vụ AI (BR-01).

- **AC-02.3 — Xử lý lỗi thân thiện khi dịch vụ AI gặp sự cố hoặc timeout:**
  - **Given:** Guest gửi câu hỏi hợp lệ tới AI Chatbot,
  - **When:** Dịch vụ AI gặp lỗi kết nối hoặc quá thời gian phản hồi quy định,
  - **Then:** Hệ thống thông báo lỗi kỹ thuật thân thiện và không gây treo ứng dụng (BR-04, NFR-18).

- **AC-02.4 — Gợi ý đăng ký tài khoản thành viên:**
  - **Given:** Guest đang trải nghiệm AI Chatbot,
  - **When:** Hệ thống hiển thị câu trả lời hoặc thông báo giới hạn,
  - **Then:** Giao diện kèm theo liên kết hướng dẫn Đăng ký tài khoản để lưu trữ thực đơn và quản lý công thức (BR-05).

---

---

<a id="fr-03"></a>
### FR-03 — Đăng ký, đăng nhập và quản lý tài khoản cơ bản

- **Mã yêu cầu:** FR-03
- **Module:** M02 (Identity & Access)
- **Tóm tắt yêu cầu:** Hệ thống cung cấp cơ chế đăng ký và đăng nhập bằng email/mật khẩu hoặc Google Login, xác minh email bắt buộc, quên/đặt lại mật khẩu, cấp stateless JWT Access Token, đăng xuất an toàn phía client, và phòng vệ brute-force bằng rate limiting 10 phút sau 5 lần thất bại liên tiếp (không khóa vĩnh viễn tài khoản sang trạng thái Admin `LOCKED`).

- **Use Case detail:** [Open interaction flows](../use-cases/identity-and-access.md#fr-03).

#### Mục đích
Cung cấp giải pháp định danh, xác thực an toàn và quản lý vòng đời tài khoản cho người dùng hệ thống (Guest, Member, Administrator); đảm bảo tính bảo mật của thông tin định danh, phòng chống các tấn công brute-force cấp tài khoản, kiểm tra trạng thái tài khoản thời gian thực và hỗ trợ đăng xuất an toàn phía client.

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-03.1`: Đăng ký tài khoản mới bằng email và mật khẩu (Register with email and password).
  - `UC-03.2`: Xác minh địa chỉ email qua liên kết xác minh (Verify email address).
  - `UC-03.3`: Yêu cầu gửi lại email xác minh (Resend verification email).
  - `UC-03.4`: Đăng nhập bằng email và mật khẩu (Login with email and password). *(Bao gồm luồng xử lý phòng vệ brute-force rate limiting theo tài khoản)*
  - `UC-03.5`: Đăng nhập 1-click bằng tài khoản Google (Login with Google OAuth2/OIDC).
  - `UC-03.6`: Yêu cầu gửi liên kết đặt lại mật khẩu qua email (Request password reset).
  - `UC-03.7`: Thiết lập mật khẩu mới từ liên kết hợp lệ (Reset password).
  - `UC-03.8`: *[SUPERSEDED / RETIRED]* Làm mới phiên xác thực (Refresh token out of scope).
  - `UC-03.9`: Đăng xuất phía client (Client-side logout and state cleanup).
  *(Ghi chú: UC-03.10 trước đây về giới hạn tần suất đăng nhập sai không còn là Use Case độc lập vì không phải mục tiêu của Actor; hành vi này được chuẩn hóa thành Luồng rẽ nhánh / Luồng bảo mật thuộc UC-03.4).*
- **User Stories:**
  - `US-03.1`: Là một người dùng mới, tôi muốn đăng ký tài khoản nhanh chóng bằng email và mật khẩu để có thể lưu trữ thông tin cá nhân và cá nhân hóa chế độ ăn chay.
  - `US-03.2`: Là một người dùng, tôi muốn đăng nhập linh hoạt bằng tài khoản hệ thống hoặc tài khoản Google để truy cập thuận tiện trên các thiết bị.
  - `US-03.3`: Là một thành viên đang sử dụng ứng dụng, tôi muốn phiên làm việc được xác thực an toàn qua Access Token mà không làm tăng độ phức tạp hệ thống.
  - `US-03.4`: Là một thành viên, tôi muốn khi đăng xuất thì ứng dụng xóa toàn bộ thông tin đăng nhập trên thiết bị để đưa về trạng thái Guest an toàn.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Khách vãng lai (Guest) chỉ được tiếp cận các chức năng: đăng ký, đăng nhập, đăng nhập Google, xác minh email, gửi lại email xác minh, yêu cầu đặt lại mật khẩu, thiết lập mật khẩu mới.
- Thành viên (Member) và Quản trị viên (Administrator) được tiếp cận chức năng đăng xuất phía client.
- Mô hình trạng thái tài khoản tách biệt với xác minh email: `account_status` chỉ gồm `ACTIVE` và `LOCKED`; đăng ký tạo tài khoản `ACTIVE` với `email_verified = false`, xác minh email chỉ đổi `email_verified` thành `true`, và đăng nhập yêu cầu `account_status = ACTIVE` cùng `email_verified = true`. Khóa quản trị `LOCKED` độc lập với trạng thái xác minh email.
- Ràng buộc kỹ thuật được phê duyệt:
  - Mật khẩu người dùng phải được băm một chiều an toàn bằng thuật toán BCrypt với work factor tối thiểu 10 (NFR-06); tuyệt đối không lưu trữ mật khẩu dạng rõ (plaintext) hoặc mã hóa hai chiều.
  - Cơ chế xác thực sử dụng Stateless JWT Access Token, phân quyền RBAC và kiểm tra tức thời trạng thái tài khoản `account_status` trên mọi authenticated request; đăng xuất xử lý phía client; không dùng refresh token hay server-side session (NFR-09).
  - Cơ chế phòng vệ brute-force áp dụng quy tắc 5 lần đăng nhập thất bại liên tiếp thì khóa tạm thời tài khoản trong 10 phút trên `USER`, độc lập hoàn toàn với trạng thái khóa tài khoản quản trị `LOCKED` (NFR-07).
  - Tích hợp đăng nhập bên thứ ba sử dụng chuẩn OAuth2/OIDC của Google Identity Services.
  - Không để lộ secret key, API key, thông tin lỗi nội bộ hoặc token trong log hệ thống.

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest (Guest không có dữ liệu cá nhân hóa, phải đăng nhập để dùng tính năng thành viên).
  - [BR-07](BUSINESS-RULES.md#br-07): Quyền đăng và công khai Recipe Post dành riêng cho Chuyên gia (Role = EXPERT).
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-01](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-01): Thời gian phản hồi xử lý Đăng nhập $\le 2$ giây (P95).
  - [NFR-06](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-06): Mã hóa mật khẩu 100% bằng BCrypt, 0% plaintext.
  - [NFR-07](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-07): Giới hạn 5 lần đăng nhập sai liên tiếp, khóa tài khoản tạm thời 10 phút trên `USER`, không chuyển sang `LOCKED`.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái phép, stateless JWT access token, kiểm tra tức thời `account_status`, client-side logout.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống lỗ hổng OWASP Top 10.
  - [NFR-11](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-11): Giao diện đăng ký $\le 3$ bước / $\le 3$ click; hỗ trợ Google Login 1-click.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-03.1 — Đăng ký thành công bằng email và mật khẩu hợp lệ:**
  - **Given:** Guest đang ở trang Đăng ký và email nhập vào chưa từng tồn tại trong hệ thống.
  - **When:** Guest nhập email hợp lệ, mật khẩu đạt chuẩn (tối thiểu 8 ký tự, bao gồm ít nhất 1 chữ cái viết hoa, 1 chữ cái viết thường và 1 chữ số; ký tự đặc biệt là tùy chọn), xác nhận mật khẩu khớp, và nhấn "Đăng ký".
  - **Then:** Hệ thống tạo tài khoản mới với `account_status = ACTIVE` và `email_verified = false`, băm mật khẩu bằng BCrypt, gửi email xác minh chứa liên kết xác minh, và thông báo yêu cầu kiểm tra email.

- **AC-03.2 — Đăng ký từ chối email đã tồn tại trong hệ thống:**
  - **Given:** Guest đang ở trang Đăng ký.
  - **When:** Guest nhập địa chỉ email đã được đăng ký bởi một tài khoản đang hoạt động và nhấn "Đăng ký".
  - **Then:** Hệ thống từ chối đăng ký, không tạo thêm tài khoản, và hiển thị thông báo lỗi email đã được sử dụng.

- **AC-03.3 — Đăng ký từ chối mật khẩu không đạt chuẩn độ phức tạp:**
  - **Given:** Guest đang ở trang Đăng ký với địa chỉ email hợp lệ và chưa tồn tại.
  - **When:** Guest nhập mật khẩu không đạt yêu cầu (dưới 8 ký tự hoặc thiếu ít nhất một trong các thành phần: chữ cái viết hoa, chữ cái viết thường, chữ số) và nhấn "Đăng ký".
  - **Then:** Hệ thống từ chối đăng ký, không tạo tài khoản, và hiển thị thông báo lỗi cụ thể về tiêu chí mật khẩu chưa thỏa mãn.

- **AC-03.4 — Xác minh email thành công cập nhật trạng thái xác minh:**
  - **Given:** Tài khoản có `email_verified = false` và mã xác minh hợp lệ còn thời hạn.
  - **When:** Người dùng nhấp vào liên kết xác minh từ email gửi tới.
  - **Then:** Hệ thống kiểm tra mã hợp lệ, cập nhật `email_verified = true`, giữ nguyên `account_status`, vô hiệu hóa mã xác minh và hiển thị thông báo xác minh thành công.

- **AC-03.5 — Xác minh email thất bại do mã không hợp lệ hoặc hết hạn:**
  - **Given:** Người dùng mở liên kết xác minh có mã xác thực đã hết hạn hoặc không tồn tại.
  - **When:** Yêu cầu xác minh gửi tới hệ thống.
  - **Then:** Hệ thống từ chối xác minh email, thông báo mã xác thực không hợp lệ/hết hạn, và hiển thị tùy chọn yêu cầu gửi lại email xác minh mới.

- **AC-03.6 — Đăng nhập thành công với thông tin chính xác:**
  - **Given:** Tài khoản đang ở trạng thái `ACTIVE` và đã xác minh email (`email_verified = true`).
  - **When:** Người dùng nhập đúng email và mật khẩu tại trang Đăng nhập và nhấn "Đăng nhập".
  - **Then:** Hệ thống phản hồi thành công trong vòng $\le 2$ giây (P95 theo NFR-01), cấp phát JWT Access Token, đặt lại bộ đếm đăng nhập sai về 0 (`USER.failed_login_attempts = 0`, `USER.login_blocked_until = NULL`), và chuyển trạng thái sang đã đăng nhập.

- **AC-03.7 — Đăng nhập thất bại do sai mật khẩu và ghi nhận số lần sai:**
  - **Given:** Người dùng nhập đúng email nhưng sai mật khẩu và số lần sai trước đó $< 4$.
  - **When:** Người dùng nhấn "Đăng nhập".
  - **Then:** Hệ thống tăng bộ đếm thất bại đối với tài khoản (`USER.failed_login_attempts`), từ chối xác thực kèm thông báo trung tính "Email hoặc mật khẩu không chính xác", và không cấp phát token.

- **AC-03.8 — Kích hoạt khóa tạm thời sau 5 lần đăng nhập sai liên tiếp theo tài khoản:**
  - **Given:** Đã có 4 lần đăng nhập sai liên tiếp đối với một tài khoản.
  - **When:** Người dùng tiếp tục thực hiện lần đăng nhập sai thứ 5.
  - **Then:** Hệ thống kích hoạt cơ chế bảo vệ tạm thời trong đúng 10 phút trên tài khoản (`USER.login_blocked_until = now + 10m`), từ chối xử lý kèm thông báo thử lại sau 10 phút; trạng thái tài khoản `account_status` trong cơ sở dữ liệu KHÔNG bị chuyển thành `LOCKED`.

- **AC-03.9 — Phục hồi đăng nhập tự động sau khi hết 10 phút rate limit:**
  - **Given:** Tài khoản đang bị khóa tạm thời 10 phút do đã đăng nhập sai 5 lần liên tiếp.
  - **When:** Thời gian 10 phút đã trôi qua và người dùng thực hiện đăng nhập lại với mật khẩu chính xác.
  - **Then:** Hệ thống tiếp nhận yêu cầu, xác thực thành công, xóa bộ đếm đăng nhập sai về 0, cấp phát JWT Access Token và hoàn tất đăng nhập mà không cần Quản trị viên can thiệp mở khóa.

- **AC-03.10 — Đăng nhập 1-click bằng tài khoản Google:**
  - **Given:** Guest chọn đăng nhập với Google và hoàn tất xác thực trên Google OAuth2/OIDC.
  - **When:** Dữ liệu xác thực Google ID Token hợp lệ được gửi tới máy chủ hệ thống.
  - **Then:** Hệ thống xác thực tính hợp lệ với Google; nếu tài khoản chưa có thì tự động tạo Member mới với `account_status = ACTIVE` và `email_verified = true`, cấp phát JWT Access Token và hoàn tất đăng nhập thành công.

- **AC-03.11 — [SUPERSEDED / OUT OF SCOPE] Xoay vòng Refresh Token:**
  - Yêu cầu xoay vòng Refresh Token qua Secure HttpOnly Cookie đã bị bãi bỏ khỏi phạm vi hiện hành theo quyết định tinh giản baseline xác thực của Project Owner. Hệ thống sử dụng Stateless JWT Access Token.

- **AC-03.12 — [SUPERSEDED / OUT OF SCOPE] Thu hồi phiên khi phát hiện tái sử dụng Refresh Token:**
  - Yêu cầu phát hiện tái sử dụng Refresh Token và thu hồi nhóm phiên máy chủ đã bị bãi bỏ khỏi phạm vi hiện hành cùng với việc loại bỏ Refresh Token.

- **AC-03.13 — Đăng xuất an toàn phía máy khách (Client-side Logout):**
  - **Given:** Member hoặc Administrator đang trong trạng thái đăng nhập trên ứng dụng client.
  - **When:** Người dùng nhấn "Đăng xuất".
  - **Then:** Ứng dụng client xóa bỏ Access Token và làm sạch trạng thái xác thực cục bộ (AuthContext), đưa người dùng về trạng thái Guest / trang Đăng nhập; không gọi endpoint thu hồi phiên máy chủ; không tồn tại `POST /auth/logout` trong baseline API mới.

- **AC-03.14 — Đặt lại mật khẩu thành công:**
  - **Given:** Người dùng có mã đặt lại mật khẩu hợp lệ còn thời hạn trong 15 phút.
  - **When:** Người dùng nhập mật khẩu mới đạt chuẩn độ phức tạp và gửi yêu cầu đặt lại mật khẩu.
  - **Then:** Hệ thống băm mật khẩu mới bằng BCrypt, cập nhật tài khoản, xóa/null mã đặt lại mật khẩu; token cũ hết hiệu lực theo `exp`; người dùng có thể đăng nhập bằng mật khẩu mới.

---

---

<a id="fr-04"></a>
### FR-04 — Chuyên gia tạo và công khai trực tiếp Recipe Post

- **Mã yêu cầu:** FR-04
- **Module:** M02 (Identity & Access), M03 (Community Recipes & Social)
- **Tóm tắt yêu cầu:** Hệ thống cho phép tài khoản có vai trò Chuyên gia (`Role = EXPERT`) đã được phê duyệt tạo, xem, chỉnh sửa, xóa và công khai trực tiếp Recipe Post của chính mình khi đạt các tiêu chuẩn kiểm tra tính hợp lệ; người đăng **không bắt buộc phải viết từng bước nấu ăn** vào bài viết mà có thể nhập hướng dẫn chế biến dạng văn bản tổng thể (10–5.000 ký tự) hoặc phân đoạn tùy ý; upload tối đa 5 ảnh minh họa (`RECIPE_MEDIA`, $\le 5$ MB/ảnh) kèm đúng 1 ảnh bìa, và gắn tối đa 1 link YouTube; hệ thống bắt buộc các nguyên liệu phải có định lượng số học và tỷ lệ quy đổi hợp lệ sang gam (BR-73); `Customer`, `Guest` và `Administrator` không có quyền tạo/đăng hoặc sửa nội dung bài công thức.

- **Use Case detail:** [Open interaction flows](../use-cases/identity-and-access.md#fr-04).

#### Mục đích
Thiết lập Mâm Xanh thành sân chơi ẩm thực chay chuyên nghiệp, nơi các công thức được sáng tạo và chia sẻ bởi các Chuyên gia ẩm thực uy tín; đảm bảo chất lượng, tính chuẩn mực và an toàn của các bài viết công thức; đồng thời trao quyền tự chủ quản lý vòng đời bài viết cho chính Chuyên gia tác giả.

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-04.1`: Chuyên gia tạo và công khai bài công thức cá nhân (Create and publish expert recipe post).
  - `UC-04.2`: Chuyên gia chỉnh sửa bài công thức của chính mình (Edit owned recipe post).
  - `UC-04.3`: Chuyên gia xóa bài công thức của chính mình (Delete owned recipe post).
- **User Stories:**
  - `US-04.1`: Là một Chuyên gia ẩm thực chay, tôi muốn biên soạn và công khai công thức tâm huyết của mình lên cộng đồng với đầy đủ định lượng và các bước chi tiết mà không phải chờ duyệt từng bài.
  - `US-04.2`: Là tác giả Chuyên gia, tôi muốn có thể cập nhật ảnh món ăn hoặc điều chỉnh định lượng gia vị bất kỳ lúc nào để hoàn thiện bài viết.
  - `US-04.3`: Là tác giả Chuyên gia, tôi muốn có thể xóa bài công thức của mình khi không còn phù hợp.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Guest và Customer tuyệt đối không có quyền tạo, chỉnh sửa hoặc xóa bài công thức (BR-05, BR-07).
- Chỉ tài khoản có vai trò `Role = EXPERT` mới có quyền tạo bài viết mới; Administrator không phải tác giả và chỉ xử lý Recipe Post qua thao tác quản trị/hậu kiểm (AC-04.7, FR-06).
- Chuyên gia tác giả chỉ có quyền chỉnh sửa hoặc xóa các bài viết do chính mình làm tác giả (BR-64, NFR-09).
- Không áp dụng cơ chế phê duyệt trước (pre-moderation) từng bài viết của Chuyên gia; chất lượng bài được bảo đảm thông qua quy trình tiền kiểm cấp quyền Chuyên gia (FR-05) và cơ chế hậu kiểm dựa trên báo cáo vi phạm cộng đồng (FR-06).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post dành riêng cho Chuyên gia.
  - [BR-14](BUSINESS-RULES.md#br-14): Định lượng nguyên liệu số học theo đơn vị chuẩn.
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản Chuyên gia đăng bài.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post.
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của mô tả và ảnh đại diện (0..5 ảnh, đúng 1 cover).
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập và xử lý bài đã lưu.
  - [BR-33](BUSINESS-RULES.md#br-33): Xử lý tham chiếu công thức trong Lịch ăn.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
  - [BR-73](BUSINESS-RULES.md#br-73): Chuẩn hóa đơn vị đo lường và chặn công khai khi thiếu quy đổi.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và truyền tải an toàn.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC chặt chẽ ở tầng Backend.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Validation dữ liệu chặt chẽ ở tầng Backend.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-04.1 — Chuyên gia công khai trực tiếp Recipe Post thành công khi dữ liệu hợp lệ:**
  - **Given:** Chuyên gia đã đăng nhập với tài khoản có `Role = EXPERT` đang hoạt động và hoàn thành biểu mẫu tạo công thức với đầy đủ thông tin hợp lệ: hướng dẫn chế biến 10–5.000 ký tự (không bắt buộc viết từng bước), 1–50 nguyên liệu có định lượng số học và tỷ lệ quy đổi gam, 0–5 ảnh `RECIPE_MEDIA` (với đúng 1 ảnh bìa nếu upload ảnh).
  - **When:** Chuyên gia nhấn "Công khai bài viết".
  - **Then:** Hệ thống lưu bài viết ở trạng thái `PUBLISHED` ngay lập tức mà không qua xét duyệt trước từng bài, gắn đúng định danh tác giả Chuyên gia, và hiển thị bài viết trên trang công khai (BR-07, BR-17, BR-19).

- **AC-04.2 — Tác giả Chuyên gia chỉnh sửa thành công bài viết của chính mình:**
  - **Given:** Chuyên gia là tác giả của bài công thức đang công khai.
  - **When:** Tác giả thay đổi thông tin mô tả và nhấn "Lưu thay đổi".
  - **Then:** Hệ thống xác thực quyền tác giả Chuyên gia, cập nhật nội dung mới vào cơ sở dữ liệu và phản ánh ngay lập tức trên bài viết.

- **AC-04.3 — Tác giả Chuyên gia xóa thành công bài viết của chính mình kèm xác nhận an toàn:**
  - **Given:** Chuyên gia đang ở trang bài viết do chính mình làm tác giả.
  - **When:** Tác giả nhấn "Xóa bài viết" và nhấn xác nhận trên hộp thoại cảnh báo.
  - **Then:** Hệ thống gỡ bài viết khỏi danh sách công khai và tìm kiếm, đánh dấu trạng thái bài viết không khả dụng.

- **AC-04.4 — Chặn người dùng khác sửa hoặc xóa bài viết không thuộc quyền sở hữu:**
  - **Given:** Bài công thức thuộc tác giả Chuyên gia A.
  - **When:** Người dùng B (Chuyên gia khác hoặc Customer, không phải Admin) gửi yêu cầu sửa hoặc xóa bài viết đó.
  - **Then:** Máy chủ kiểm tra quyền sở hữu, từ chối thao tác và trả về mã lỗi HTTP 403 Forbidden (BR-64).

- **AC-04.5 — Chặn Guest tạo bài công thức:**
  - **Given:** Người dùng chưa đăng nhập (Guest).
  - **When:** Guest cố tình mở trang tạo bài viết hoặc gửi yêu cầu tạo bài công thức.
  - **Then:** Hệ thống chuyển hướng người dùng tới trang Đăng nhập và không cho phép tạo bài (BR-05).

- **AC-04.6 — Chặn Customer thông thường tạo bài công thức:**
  - **Given:** Người dùng thông thường đã đăng nhập với vai trò `CUSTOMER`.
  - **When:** Customer gửi yêu cầu tạo bài viết (`POST /api/v1/recipes`) hoặc cố tình truy cập đường dẫn tạo bài.
  - **Then:** Giao diện ẩn nút đăng bài và hiển thị lời mời đăng ký Chuyên gia; Backend từ chối request với mã lỗi HTTP 403 Forbidden và thông báo yêu cầu quyền Chuyên gia.

- **AC-04.7 — Chặn Administrator tạo hoặc sửa nội dung bài công thức:**
  - **Given:** Administrator đã đăng nhập và không có vai trò `EXPERT`.
  - **When:** Administrator gửi yêu cầu tạo hoặc sửa nội dung Recipe Post.
  - **Then:** Backend từ chối với HTTP 403 Forbidden; quyền hậu kiểm theo FR-06 vẫn được áp dụng qua thao tác quản trị riêng.

---

---

<a id="fr-05"></a>
### FR-05 — Cơ chế nộp đơn đăng ký Chuyên gia theo format và Administrator phê duyệt cấp quyền

- **Mã yêu cầu:** FR-05
- **Module:** M02 (Identity & Access), M03 (Community Recipes & Social), M09 (Administration & Moderation)
- **Tóm tắt yêu cầu:** Hệ thống cho phép người dùng thông thường (`Customer`) có nguyện vọng chia sẻ công thức nộp đơn đăng ký Chuyên gia theo format văn bản có cấu trúc (`EXPERT_APPLICATION`) và tùy chọn đính kèm tối đa một tài liệu chứng chỉ/văn bằng chuyên môn cùng một ảnh thẻ chân dung khung 3:4 riêng của đơn; đây là quy trình xét duyệt **tư cách Chuyên gia và quyền đăng bài**, không phải xác minh danh tính/KYC. Hệ thống không yêu cầu giấy tờ tùy thân. Tệp đính kèm được lưu riêng tư và chỉ Administrator có quyền tải/xem khi thẩm định; ảnh không cập nhật avatar hồ sơ công khai. Administrator ghi nhận xét thẩm định và quyết định phê duyệt (`APPROVED`) hoặc từ chối (`REJECTED`) kèm lý do; khi được phê duyệt, tài khoản được thăng cấp tức thời lên vai trò `EXPERT`; nếu bị từ chối, người dùng có thể nộp lại đơn mới.

- **Use Case detail:** [Open interaction flows](../use-cases/identity-and-access.md#fr-05).

#### Mục đích
Thiết lập quy trình thẩm định tư cách Chuyên gia minh bạch, chặt chẽ nhưng gọn nhẹ, không tạo rào cản thủ tục hành chính phức tạp về bằng cấp vật lý; bảo đảm chỉ những người dùng có kinh nghiệm, kiến thức và cam kết ẩm thực chay chân chính mới được cấp quyền đăng bài; đồng thời tạo ra công cụ quản trị quy trình xét duyệt hoàn chỉnh cho Administrator.

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-05.1`: Nộp đơn đăng ký Chuyên gia theo format văn bản (Submit expert application form).
  - `UC-05.2`: Thẩm định danh sách đơn đăng ký Chuyên gia (Review pending expert applications).
  - `UC-05.3`: Phê duyệt đơn và cấp quyền Chuyên gia (Approve application and grant Expert role).
  - `UC-05.4`: Từ chối đơn đăng ký kèm lý do giải thích (Reject application with admin reason).
  - `UC-05.5`: Customer xem lịch sử và trạng thái các đơn đăng ký của chính mình.
- **User Stories:**
  - `US-05.1`: Là một người nấu chay có kinh nghiệm, tôi muốn điền bản giới thiệu theo format chuẩn về kinh nghiệm ẩm thực của mình để xin cấp quyền Chuyên gia mà không cần phải chụp/nộp bằng cấp rườm rà.
  - `US-05.2`: Là một Administrator, tôi muốn xem danh sách các đơn đăng ký Chuyên gia đang chờ duyệt với đầy đủ thông tin tóm tắt để thẩm định nhanh chóng và công bằng.
  - `US-05.3`: Là một Administrator, khi tôi phê duyệt một đơn hợp lệ, tôi muốn hệ thống tự động chuyển vai trò tài khoản đó sang Chuyên gia và gửi thông báo chúc mừng tới họ.
  - `US-05.4`: Là một Customer bị từ chối đơn, tôi muốn đọc được lý do cụ thể từ Admin để có thể hoàn thiện thông tin và nộp lại đơn mới.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Guest không được nộp đơn; hệ thống yêu cầu đăng nhập trước khi tiếp cận biểu mẫu (BR-05).
- Customer chỉ được xem trạng thái và nội dung đơn đăng ký của chính mình (NFR-09).
- Chỉ Administrator có quyền xem toàn bộ danh sách đơn và thực hiện phê duyệt/từ chối (NFR-09).
- Không yêu cầu tải lên giấy tờ tùy thân. Customer có thể tự nguyện gửi tối đa một tài liệu chứng chỉ/văn bằng chuyên môn (PDF/JPEG/PNG, tối đa 5 MB) và một ảnh thẻ chân dung khung 3:4 riêng của đơn (JPEG/PNG/WebP, tối đa 2 MB). Backend xác minh định dạng thực tế và dung lượng; từ chối tệp không hợp lệ và không tạo đơn dở dang.
- Tệp được lưu trong vùng Azure Blob riêng tư; không phát URL công khai. Chỉ Administrator được phép xem hoặc tải nội dung tệp qua Backend trong phạm vi thẩm định. Ảnh thẻ 3:4 đính kèm thuộc đơn và không thay đổi `USER.avatar_url` hoặc hồ sơ công khai FR-23.
- Quy trình không được mô tả là xác minh danh tính/KYC hoặc xác thực bằng cấp của Customer.

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post dành riêng cho Chuyên gia.
  - [BR-74](BUSINESS-RULES.md#br-74): Quy trình xét duyệt đơn đăng ký Chuyên gia và chuyển đổi vai trò.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC và bảo vệ endpoint quản trị.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Validation dữ liệu văn bản và tệp đính kèm chặt chẽ ở tầng Backend.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-05.1 — Customer nộp đơn đăng ký Chuyên gia hợp lệ thành công:**
  - **Given:** Customer đã đăng nhập, tài khoản `ACTIVE`, chưa phải là Chuyên gia và không có đơn nào đang ở trạng thái `PENDING`.
  - **When:** Customer nhập đầy đủ thông tin format hợp lệ (kinh nghiệm $\ge 20$ ký tự, chọn trường phái chay, tóm tắt công thức mẫu $\ge 30$ ký tự) và nhấn "Gửi đơn đăng ký".
  - **Then:** Hệ thống tạo bản ghi `EXPERT_APPLICATION` mới ở trạng thái `PENDING`, liên kết với tài khoản người dùng, hiển thị thông báo gửi thành công và chuyển giao diện sang trạng thái chờ duyệt.

- **AC-05.2 — Chặn nộp đơn trùng lặp khi đang có đơn chờ duyệt:**
  - **Given:** Customer đang có một đơn đăng ký ở trạng thái `PENDING`.
  - **When:** Customer gửi yêu cầu tạo thêm đơn đăng ký mới.
  - **Then:** Hệ thống từ chối tiếp nhận, không tạo thêm bản ghi, trả về mã lỗi HTTP 409 Conflict và thông báo người dùng đang có đơn chờ xử lý.

- **AC-05.3 — Administrator phê duyệt đơn và hệ thống tự động thăng cấp Chuyên gia:**
  - **Given:** Administrator đang xem chi tiết một đơn đăng ký ở trạng thái `PENDING`.
  - **When:** Administrator nhấn "Phê duyệt" và xác nhận.
  - **Then:** Trong cùng một transaction, hệ thống cập nhật trạng thái đơn thành `APPROVED`, ghi nhận thời gian và người duyệt, lập tức chuyển đổi vai trò tài khoản tương ứng thành `EXPERT`, và ghi thông báo in-app chúc mừng tới người dùng. Nếu một bước thất bại, toàn bộ thay đổi được rollback. Người dùng ngay sau đó có thể truy cập chức năng tạo bài viết (FR-04).

- **AC-05.4 — Administrator từ chối đơn bắt buộc nhập lý do:**
  - **Given:** Administrator đang xem chi tiết một đơn đăng ký ở trạng thái `PENDING`.
  - **When:** Administrator nhấn "Từ chối" nhưng bỏ trống ô lý do từ chối.
  - **Then:** Hệ thống chặn thao tác, yêu cầu nhập lý do từ chối cụ thể (tối thiểu 10 ký tự); khi Administrator nhập lý do hợp lệ và xác nhận, trong cùng một transaction hệ thống cập nhật đơn thành `REJECTED` và ghi thông báo in-app kèm lý do tới người dùng.

- **AC-05.5 — Customer nộp lại đơn đăng ký mới sau khi bị từ chối:**
  - **Given:** Customer có đơn đăng ký trước đó ở trạng thái `REJECTED`.
  - **When:** Customer mở lại biểu mẫu đăng ký, điều chỉnh nội dung format theo góp ý và nhấn "Gửi đơn đăng ký".
  - **Then:** Hệ thống tiếp nhận thành công, tạo bản ghi `EXPERT_APPLICATION` mới ở trạng thái `PENDING`, lưu trữ độc lập với bản ghi đã bị từ chối trước đó.

- **AC-05.6 — Customer xem lịch sử đơn của chính mình:**
  - **Given:** Customer đã từng gửi ít nhất một đơn đăng ký Chuyên gia.
  - **When:** Customer mở mục lịch sử đăng ký Chuyên gia.
  - **Then:** Hệ thống hiển thị các đơn của đúng tài khoản theo thứ tự mới nhất trước, gồm ngày gửi, trạng thái và kết quả xử lý; không hiển thị đơn của người dùng khác.

- **AC-05.7 — Chặn xử lý đồng thời hoặc xử lý lại đơn:**
  - **Given:** Hai Administrator cùng mở một đơn `PENDING` và người thứ nhất đã xử lý thành công.
  - **When:** Administrator thứ hai gửi quyết định cho cùng đơn từ dữ liệu cũ.
  - **Then:** Hệ thống trả `HTTP 409 Conflict`, giữ nguyên quyết định đầu tiên và không cập nhật role hoặc gửi thông báo lần hai.

- **AC-05.8 — Chặn duyệt khi tài khoản không còn đủ điều kiện:**
  - **Given:** Đơn còn `PENDING` nhưng tài khoản nộp đã không còn `ACTIVE` hoặc không còn vai trò `CUSTOMER`.
  - **When:** Administrator cố phê duyệt đơn.
  - **Then:** Hệ thống trả `HTTP 409 Conflict`, rollback quyết định, không cấp role `EXPERT` hoặc ghi notification, và yêu cầu Administrator tải lại trạng thái hiện hành.

- **AC-05.9 — Customer gửi tệp đính kèm riêng cho đơn:**
  - **Given:** Customer đủ điều kiện nộp đơn; tài liệu và ảnh đính kèm là tùy chọn.
- **When:** Customer gửi tối đa một tài liệu chứng chỉ/văn bằng chuyên môn dạng PDF/JPEG/PNG không quá 5 MB và một ảnh thẻ chân dung khung 3:4 dạng JPEG/PNG/WebP không quá 2 MB.
- **Then:** Hệ thống xác minh chữ ký nội dung khớp với định dạng khai báo, lưu tệp riêng tư gắn với đúng đơn; ảnh thẻ dùng khung chân dung 3:4 và không cập nhật avatar tài khoản. Tệp thiếu, rỗng, sai định dạng hoặc quá giới hạn bị từ chối với lỗi dễ hiểu và không tạo đơn hoặc tệp mồ côi.

- **AC-05.10 — Chỉ Administrator truy cập tệp của đơn để thẩm định:**
  - **Given:** Đơn có tệp đính kèm.
  - **When:** Administrator đã xác thực mở chi tiết đơn hoặc yêu cầu tải tệp; Customer, Expert, Guest hoặc người dùng khác thử truy cập cùng tài nguyên.
- **Then:** Administrator có thể xem/tải tài liệu chứng chỉ và ảnh thẻ 3:4 qua endpoint có xác thực; các role khác bị chặn ở Backend. API không trả URL Blob công khai và tệp không hiển thị trong hồ sơ/avatar công khai.

- **AC-05.11 — Nhãn và hướng dẫn Portfolio URL:**
  - **Then:** Form hiển thị nhãn “Đường dẫn portfolio”; hướng dẫn “Không bắt buộc. Chấp nhận HTTP/HTTPS, tối đa 500 ký tự.” nằm riêng bên dưới, không có dấu ngoặc.

---

---

<a id="fr-06"></a>
### FR-06 — Administrator xử lý báo cáo và quản lý nội dung hậu kiểm

- **Mã yêu cầu:** FR-06
- **Module:** M09 (Administration & Moderation)
- **Tóm tắt yêu cầu:** Hệ thống cho phép Administrator tiếp nhận, quản lý và xử lý báo cáo vi phạm đối với thành viên, Recipe Post, bình luận và danh mục; Administrator không duyệt quyền đăng hoặc duyệt từng Recipe Post trước khi công khai; toàn bộ việc kiểm soát nội dung tuân thủ cơ chế hậu kiểm (post-moderation) dựa trên phản ánh của cộng đồng.

- **Use Case detail:** [Open interaction flows](../use-cases/administration.md#fr-06).

#### Mục đích
Cung cấp trung tâm điều hành hậu kiểm nội dung toàn diện cho Quản trị viên; đảm bảo môi trường cộng đồng trong sạch, an toàn, tuân thủ pháp luật và giữ gìn chuẩn mực ẩm thực chay chân chính mà không gây ách tắc cho việc chia sẻ nội dung tự do của thành viên.

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-06.1`: Quản lý danh sách nội dung bị báo cáo hậu kiểm (Manage reported content dashboard).
  - `UC-06.2`: Thực hiện hành động hậu kiểm đối với nội dung hoặc tài khoản vi phạm (Execute moderation actions on content or user).
- **User Stories:**
  - `US-06.1`: Là một Administrator, tôi muốn có bảng tổng hợp trực quan các bài viết và bình luận bị báo cáo nhiều nhất để ưu tiên xử lý nhanh các trường hợp vi phạm nghiêm trọng.
  - `US-06.2`: Là một Administrator, tôi muốn áp dụng chế tài ẩn bài hoặc khóa tài khoản kèm lý do rõ ràng để bảo đảm tính minh bạch và công bằng.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Chỉ tài khoản có vai trò Administrator mới có quyền truy cập trung tâm hậu kiểm và áp dụng chế tài (NFR-09).
- Tuyệt đối không áp dụng cơ chế phê duyệt trước (pre-moderation) từng bài viết trước khi đăng (BR-07).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-06](BUSINESS-RULES.md#br-06): Quản lý nội dung công thức và kiểm duyệt hậu kiểm.
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-26](BUSINESS-RULES.md#br-26): Các hình thức chế tài khi xử lý vi phạm.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và toàn vẹn lịch sử kiểm toán.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

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

---

<a id="fr-08"></a>
### FR-08 — Tìm kiếm và lọc bài công thức đa tiêu chí

- **Mã yêu cầu:** FR-08
- **Module:** M04 (Recipe Catalog & Search)
- **Tóm tắt yêu cầu:** Hệ thống cung cấp công cụ tìm kiếm và bộ lọc đa tiêu chí (từ khóa, loại ăn chay chuẩn, thể loại món chuẩn hóa `dish_category`, nguyên liệu, thời gian nấu tối đa) kết hợp 6 chế độ sắp xếp độc lập: Mới nhất (Newest), Được yêu thích nhất (Most Liked theo tỷ lệ % Like và tổng lượt Like `like_count`, BR-69), Xem nhiều nhất (Most Viewed theo 24h/7d/30d/all-time), Nhiều bình luận nhất (Most Commented), Hoạt động sôi nổi nhất (Most Active theo tương tác 7 ngày gần nhất BR-71), và Thịnh hành (Trending theo tương tác gần đây + freshness decay BR-72) cho toàn bộ người dùng (Guest, Member, Administrator).

- **Use Case detail:** [Open interaction flows](../use-cases/recipe-discovery-and-classification.md#fr-08).

#### Mục đích
Giúp người dùng nhanh chóng tìm thấy các bài công thức chay phù hợp với nhu cầu ăn uống cụ thể, sở thích cá nhân, nguyên liệu sẵn có trong gia đình hoặc quỹ thời gian nấu nướng; tối ưu hóa trải nghiệm khám phá ẩm thực chay trên nền tảng qua các tiêu chí lọc chính xác và bảng xếp hạng đa dạng.

#### Ngữ nghĩa lọc
- Từ khóa, loại ăn chay, thể loại món, nguyên liệu và giới hạn thời gian được áp dụng đồng thời theo điều kiện AND.
- Khi chọn nhiều nguyên liệu, một Recipe Post chỉ khớp nếu có đủ tất cả nguyên liệu chuẩn đã chọn.
- Tổng thời gian dùng để so sánh với giới hạn là thời gian chuẩn bị cộng thời gian nấu (`prep_time_min + cook_time_min`). Bộ lọc chỉ trả Recipe Post công khai và không làm thay đổi chế độ sắp xếp đã chọn.

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-08.1`: Tìm kiếm bài công thức theo từ khóa (Search recipes by keyword).
  - `UC-08.2`: Lọc bài công thức theo đa tiêu chí (Filter recipes by multiple criteria).
  - `UC-08.3`: Sắp xếp danh sách kết quả tìm kiếm theo 6 chế độ độc lập (Sort recipe search results).
- **User Stories:**
  - `US-08.1`: Là một người ăn chay thuần (Vegan), tôi muốn lọc các công thức theo nhãn "Thuần chay" để không phải tự kiểm tra thủ công xem món ăn có chứa sữa hay trứng không.
  - `US-08.2`: Là một người bận rộn, tôi muốn lọc món ăn có thời gian nấu dưới 30 phút và sắp xếp theo mới nhất để nhanh chóng chọn được món ngon và tiện lợi.
  - `US-08.3`: Là một người dùng muốn tìm món ngon được cộng đồng kiểm chứng, tôi muốn sắp xếp theo "Được yêu thích nhất" hoặc "Xem nhiều nhất" để chọn các công thức có tỷ lệ % Like cao và uy tín nhất.
  - `US-08.4`: Là một người dùng yêu thích xu hướng, tôi muốn sắp xếp theo "Thịnh hành" hoặc "Hoạt động sôi nổi nhất" để cập nhật các món chay đang được quan tâm thảo luận nhiều nhất tuần này.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Tính năng tìm kiếm và lọc là dịch vụ công khai mở cho mọi người dùng (Guest, Member, Admin).
- Truy vấn tìm kiếm bắt buộc phải được xử lý an toàn, sử dụng parameterized query hoặc ORM an toàn để phòng chống tấn công SQL Injection (NFR-10).
- Bảo vệ hiệu năng hệ thống: Áp dụng giới hạn số lượng kết quả trên mỗi trang (phân trang chuẩn 12–24 bài/trang) để tránh quá tải bộ nhớ.

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-69](BUSINESS-RULES.md#br-69): Tương tác Like / Dislike và tỷ lệ hài lòng (% Like) của bài công thức.
  - [BR-70](BUSINESS-RULES.md#br-70): Ghi nhận lượt xem và khử trùng lặp theo phiên (`RECIPE_VIEW`).
  - [BR-71](BUSINESS-RULES.md#br-71): Thuật toán xếp hạng hoạt động sôi nổi nhất (Most Active).
  - [BR-72](BUSINESS-RULES.md#br-72): Thuật toán xếp hạng thịnh hành (Trending Ranking).
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian phản hồi tìm kiếm và sắp xếp công thức $\le 1.5$ giây (P95).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống lỗ hổng OWASP Top 10 (SQL Injection).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt, thích ứng đa thiết bị.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-08.1 — Tìm kiếm theo từ khóa phản hồi nhanh chóng:**
  - **Given:** Người dùng đang ở trang Tìm kiếm công thức.
  - **When:** Người dùng nhập từ khóa "đậu phụ sốt cà" và thực hiện tìm kiếm.
  - **Then:** Hệ thống trả về danh sách các công thức có tên hoặc mô tả chứa từ khóa với thời gian đáp ứng $\le 1.5$ giây (P95).

- **AC-08.2 — Lọc chính xác theo loại ăn chay chuẩn:**
  - **Given:** Kho dữ liệu có nhiều bài công thức với các loại ăn chay khác nhau.
  - **When:** Người dùng chọn bộ lọc loại ăn chay là `Vegan`.
  - **Then:** Danh sách kết quả chỉ chứa 100% các bài viết được gắn nhãn `Vegan`, loại trừ hoàn toàn các món ăn có sữa hoặc trứng.

- **AC-08.3 — Lọc chính xác theo thể loại món (dish_category):**
  - **Given:** Người dùng đang ở màn hình Khám phá công thức.
  - **When:** Người dùng chọn thể loại món "món canh".
  - **Then:** Hệ thống chỉ hiển thị các bài công thức có `dish_category = "món canh"`.

- **AC-08.4 — Lọc chính xác theo thời gian nấu tối đa:**
  - **Given:** Người dùng đang thiết lập bộ lọc tìm kiếm.
  - **When:** Người dùng chọn tiêu chí thời gian nấu "$\le 30$ phút".
  - **Then:** Toàn bộ các công thức trong kết quả đều có tổng thời gian nấu $\le 30$ phút.

- **AC-08.5 — Sắp xếp kết quả theo bài đăng mới nhất:**
  - **Given:** Có nhiều bài công thức thỏa mãn điều kiện lọc.
  - **When:** Người dùng chọn sắp xếp theo "Mới nhất".
  - **Then:** Kết quả được hiển thị theo thứ tự thời gian công khai giảm dần.

- **AC-08.6 — Sắp xếp theo Được yêu thích nhất (Most Liked):**
  - **Given:** Các bài công thức có tỷ lệ % Like và tổng số lượt bình chọn khác nhau.
  - **When:** Người dùng chọn chế độ sắp xếp "Được yêu thích nhất".
  - **Then:** Kết quả sắp xếp theo tỷ lệ % Like giảm dần, kèm điều kiện phụ theo tổng lượt Like (`like_count` giảm dần); trên giao diện hiển thị rõ huy hiệu tỷ lệ % Like (`👍 {like_percentage}%`) hoặc nhãn "Mới" nếu chưa có bình chọn (BR-69).

- **AC-08.7 — Sắp xếp theo Xem nhiều nhất (Most Viewed):**
  - **Given:** Các bài công thức có dữ liệu lượt xem hợp lệ trong `RECIPE_VIEW`.
  - **When:** Người dùng chọn sắp xếp "Xem nhiều nhất" và chọn khung thời gian (ví dụ 7 ngày qua).
  - **Then:** Kết quả sắp xếp theo số lượt xem hợp lệ trong 7 ngày giảm dần (BR-70).

- **AC-08.8 — Phân biệt chính xác giữa Hoạt động sôi nổi nhất (Most Active) và Thịnh hành (Trending):**
  - **Given:** Bài A là công thức đăng 6 tháng trước nhưng có lượng tương tác rất cao trong 7 ngày qua (viral lại); Bài B là công thức vừa đăng 12 giờ trước với tốc độ tăng tương tác nhanh.
  - **When:** Người dùng lần lượt chuyển đổi giữa chế độ "Hoạt động sôi nổi nhất" và "Thịnh hành".
  - **Then:**
    - Ở chế độ "Hoạt động sôi nổi nhất": Bài A xếp trên Bài B do tổng khối lượng tương tác 7 ngày lớn hơn và không bị suy giảm thời gian (BR-71).
    - Ở chế độ "Thịnh hành": Bài B xếp trên Bài A do Bài B có độ tươi mới cao (mẫu số suy giảm thời gian nhỏ) và được cộng điểm khởi đầu bài mới (BR-72).

- **AC-08.9 — Xử lý khi không có kết quả tìm kiếm phù hợp:**
  - **Given:** Người dùng kết hợp các tiêu chí lọc quá hẹp mà không có công thức nào thỏa mãn.
  - **When:** Hệ thống thực thi tìm kiếm.
  - **Then:** Giao diện hiển thị thông báo "Không tìm thấy công thức phù hợp" kèm tùy chọn đặt lại bộ lọc.

---

---

<a id="fr-09"></a>
### FR-09 — Authorized User tạo và chỉnh lịch ăn tuần

- **Mã yêu cầu:** FR-09
- **Module:** M05

- **Use Case detail:** [Open interaction flows](../use-cases/meal-planning-and-shopping.md#fr-09).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp tính năng lập và chỉnh sửa kế hoạch ăn uống theo tuần dành cho Authorized User (Member đã đăng nhập); hỗ trợ xếp các bài công thức nấu ăn vào cấu trúc 3 bữa ăn cố định mỗi ngày (Bữa sáng, Bữa trưa, Bữa tối) cho 7 ngày trong tuần; khi bài công thức được tham chiếu bị tác giả xóa hoặc bị Administrator ẩn, hệ thống bảo toàn mục lịch ăn ở trạng thái không khả dụng (Tombstone) thay vì bị xóa liên đới (cascade-delete).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Người dùng đã đăng nhập tài khoản Member (FR-03).
  - Cấu trúc lịch ăn: 7 ngày trong tuần (Thứ Hai đến Chủ Nhật), mỗi ngày chia đúng 3 bữa: Sáng, Trưa, Tối (BR-36).
  - Không áp dụng: Guest không có quyền lưu lịch ăn (BR-05, BR-32).
- **Phân loại Actor:**
  - Primary Actor: `Member / Authorized User` (người dùng quản lý lịch ăn cá nhân).
  - Supporting Actor: `Hệ thống quản lý Lịch ăn tuần`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-09.1`: Tạo mới hoặc mở xem Lịch ăn cho tuần hiện tại hoặc các tuần tiếp theo.
  - `UC-09.2`: Thêm, đổi hoặc gỡ bỏ bài công thức trong từng bữa ăn (Sáng, Trưa, Tối) của các ngày trong tuần.
  - `UC-09.3`: Xem thông báo trạng thái Tombstone khi món ăn đã lưu trong lịch bị xóa hoặc tạm ẩn.
- **User Stories:**
  - *Là một người ăn chay bận rộn*, tôi muốn lên trước thực đơn 3 bữa cho cả tuần để chủ động chuẩn bị nguyên liệu và duy trì chế độ ăn chay đều đặn mà không phải nghĩ hôm nay ăn gì.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Độc quyền cho Authorized User (Member sở hữu tài khoản).
- **Ràng buộc nghiệp vụ:** Cố định 3 bữa/ngày trong MVP (BR-36); thao tác trên lịch ăn hoàn toàn độc lập với danh sách công thức đã lưu (BR-35) và không gọi Gemini (BR-33).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Lịch ăn.
  - [BR-33](BUSINESS-RULES.md#br-33): Thao tác lưu công thức không gọi Gemini.
  - [BR-35](BUSINESS-RULES.md#br-35): Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn.
  - [BR-36](BUSINESS-RULES.md#br-36): Quy tắc 3 loại bữa ăn cố định trong Lịch ăn MVP.
  - [BR-37](BUSINESS-RULES.md#br-37): Tính duy nhất của công thức trong cùng một bữa ăn ngày.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
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
- **AC-09.6 (Thay công thức trong một bữa ăn):**
  - *Given* một bữa ăn trong lịch tuần đã có công thức,
  - *When* Member chọn một công thức công khai khác để thay thế,
  - *Then* hệ thống cập nhật món trong đúng vị trí bữa ăn đã chọn.
- **AC-09.7 (Gỡ công thức khỏi một bữa ăn):**
  - *Given* một bữa ăn trong lịch tuần đã có công thức,
  - *When* Member gỡ công thức khỏi bữa đó,
  - *Then* hệ thống xóa liên kết khỏi bữa ăn nhưng không xóa Recipe Post nguồn hoặc mục đã lưu.

---

---

<a id="fr-10"></a>
### FR-10 — Phân quyền tính năng AI theo gói tài khoản (Feature-based AI Entitlement)

- **Mã yêu cầu:** FR-10
- **Module:** M06

- **Use Case detail:** [Open interaction flows](../use-cases/ai-and-personalization.md#fr-10).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống kiểm soát và phân quyền sử dụng các tính năng Google Gemini AI theo các gói dịch vụ (Feature-based Entitlement), loại bỏ hoàn toàn cơ chế hạn mức theo số lượt/ngày (5/15/50) và chu kỳ reset 00:00; quyền sử dụng được xác thực độc lập tại Backend trước khi gọi AI provider; khi người dùng yêu cầu tính năng vượt quá thẩm quyền của gói hiện tại, hệ thống chặn tại tầng máy chủ và hiển thị thông báo hướng dẫn nâng cấp gói tương ứng (kết nối FR-13).
- **Phạm vi nghiệp vụ:**
  - Ma trận phân quyền tính năng AI theo gói:
    | Gói tài khoản | Mức phí | Quyền tính năng AI (Feature Entitlement) | Cơ chế kiểm soát |
    |---|---|---|---|
    | **Guest Free** | 0 VNĐ | AI Chatbot hỏi đáp ẩm thực chay cơ bản (FR-51) | Giới hạn tần suất kỹ thuật (Rate Limiting theo cookie/IP, BR-01) |
    | **Free Member** | 0 VNĐ | AI Chatbot cơ bản (FR-51) + Gợi ý món theo nguyên liệu (`UC-34.1` thuộc FR-34) | Xác thực tài khoản Member; không giới hạn số lượt/ngày |
    | **Plus Member** | 49.000 VNĐ / tháng | Toàn bộ quyền gói Free + AI lập thực đơn tuần thông thường (`UC-34.2`/`UC-34.3` thuộc FR-34) + AI lập thực đơn theo dinh dưỡng (FR-36) | Xác thực gói Subscription hiệu lực; không giới hạn số lượt/ngày |
    | **Pro Member** | 99.000 VNĐ / tháng | Toàn bộ quyền gói Plus + AI hỗ trợ soạn bài Recipe Post cho Chuyên gia (FR-21) + AI gợi ý món liên quan hoặc biến tấu (FR-47) + AI đề xuất điều chỉnh dinh dưỡng (FR-37) | Toàn quyền tất cả tính năng AI sản phẩm; không giới hạn số lượt/ngày |
  - Ma trận quyền hiện hành áp dụng cho cả gói đã mua còn hiệu lực. Không đổi giá, mã gói, thời hạn đã thanh toán hoặc tự chuyển hạng Subscription. AI soạn bài vẫn yêu cầu vai trò Chuyên gia. Quyền gói không thay thế các điều kiện hồ sơ và eligibility.
  - Nguyên tắc kiểm soát: Backend bắt buộc xác thực Feature Entitlement trước khi gửi yêu cầu tới dịch vụ Gemini (BR-03); yêu cầu không đủ quyền bị từ chối ngay tại server và tuyệt đối không phát sinh request tới Gemini API.
- **Phân loại Actor:**
  - Primary Actor: `Guest`, `Member` (Free, Plus, Pro).
  - Supporting Actor: `Hệ thống phân quyền tính năng AI (Feature Entitlement Guard)`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-10.1`: Xác thực quyền sử dụng tính năng AI trước khi kích hoạt (Verify AI Feature Entitlement).
  - `UC-10.2`: Nhận thông báo hướng dẫn nâng cấp gói khi truy cập tính năng AI vượt quá quyền hạn của gói hiện tại.
- **User Stories:**
  - *Là một người dùng gói Free*, tôi muốn sử dụng AI Chatbot và tìm gợi ý món chay nhanh chóng, đồng thời nhận hướng dẫn rõ ràng về quyền lợi nâng cấp gói khi muốn dùng tính năng AI tạo thực đơn tuần hoặc hỗ trợ viết bài.
  - *Là một người dùng gói Plus hoặc Pro*, tôi muốn sử dụng không giới hạn các tính năng AI trong phạm vi gói của mình mà không bị ngắt quãng bởi giới hạn số lượt mỗi ngày.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Áp dụng tự động cho mọi đối tượng người dùng hệ thống dựa trên Subscription hiện tại.
- **Ràng buộc kinh doanh:** Phân quyền theo gói tính năng (Feature-based Entitlement); không áp dụng daily quota theo số lượt.

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Quyền sử dụng AI Chat cho Guest và Member Free.
  - [BR-02](BUSINESS-RULES.md#br-02): Phân quyền tính năng AI cho gói Plus và Pro.
  - [BR-03](BUSINESS-RULES.md#br-03): Xác thực quyền tính năng trước khi gọi dịch vụ AI.
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider, timeout AI và lưu trữ telemetry.
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-06](BUSINESS-RULES.md#br-06): Bảo mật Gemini API Key.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03): Thời gian phản hồi của tính năng AI Chatbot.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống lạm dụng và lỗ hổng bảo mật.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-10.1 (Quyền sử dụng AI cho Guest và Member Free):**
  - *Given* người dùng là Guest hoặc Member có gói Free,
  - *When* gọi tính năng AI Chatbot (FR-51) hoặc Gợi ý công thức theo nguyên liệu (UC-34.1 thuộc FR-34 đối với Member),
  - *Then* hệ thống xác thực hợp lệ và cho phép thực thi yêu cầu mà không giới hạn theo số lượt/ngày.
- **AC-10.2 (Chặn Member Free khi gọi tính năng gói Plus):**
  - *Given* Member thuộc gói Free gọi tính năng AI lập thực đơn tuần (UC-34.2/UC-34.3, FR-36),
  - *When* Backend tiếp nhận yêu cầu,
  - *Then* hệ thống từ chối yêu cầu với HTTP 403 và hiển thị thông báo hướng dẫn nâng cấp lên gói Plus.
- **AC-10.3 (Quyền sử dụng AI cho gói Plus):**
  - *Given* Member có gói Plus đang hiệu lực,
  - *When* gọi tính năng AI lập thực đơn tuần (UC-34.2/UC-34.3, FR-36),
  - *Then* hệ thống xác thực thành công và gửi yêu cầu tới Gemini AI.
- **AC-10.4 (Chặn Member Free và Plus khi gọi tính năng gói Pro):**
  - *Given* Member thuộc gói Free hoặc Plus gọi tính năng AI soạn bài (FR-21, đồng thời phải có vai trò Chuyên gia) hoặc AI gợi ý món liên quan/biến tấu (FR-47) hoặc AI đề xuất điều chỉnh dinh dưỡng (FR-37),
  - *When* Backend tiếp nhận yêu cầu,
  - *Then* hệ thống từ chối yêu cầu với HTTP 403 và hiển thị thông báo hướng dẫn nâng cấp lên gói Pro.
- **AC-10.5 (Quyền toàn diện cho gói Pro):**
  - *Given* Member có gói Pro đang hiệu lực,
  - *When* gọi bất kỳ tính năng AI nào của hệ thống (gồm cả FR-36),
  - *Then* hệ thống xác thực thành công và cho phép thực thi đầy đủ.
- **AC-10.6 (Chặn tại Backend trước khi gọi provider ngoài):**
  - *Given* tài khoản không đủ quyền sử dụng tính năng AI yêu cầu,
  - *When* người dùng gửi request,
  - *Then* hệ thống chặn ngay tại tầng Backend Guard mà tuyệt đối không gửi bất kỳ HTTP request nào tới Google Gemini API (BR-03).
- **AC-10.7 (Chặn gói Free khi yêu cầu thực đơn tuần FR-34):**
  - *Given* Member đang thuộc gói Free,
  - *When* Member yêu cầu tạo thực đơn tuần thông thường theo `UC-34.2`,
  - *Then* hệ thống từ chối yêu cầu và hướng dẫn nâng cấp lên gói Plus.

---

---

<a id="fr-11"></a>
### FR-11 — Ghi nhận dữ liệu đo lường kỹ thuật (Telemetry) sử dụng AI

- **Mã yêu cầu:** FR-11
- **Module:** M06

- **Use Case detail:** [Open interaction flows](../use-cases/ai-and-personalization.md#fr-11).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống thu thập và lưu trữ dữ liệu đo lường kỹ thuật (AI Usage Telemetry & Accounting) cho mọi lượt gọi Gemini AI thành công, bao gồm siêu dữ liệu lượng token tiêu thụ (prompt tokens, response tokens, total tokens) do nhà cung cấp Gemini trả về nhằm đo lường mức độ sử dụng tài nguyên thực tế và đối soát chi phí vận hành; đây là dữ liệu log kỹ thuật, không phải thực thể quản lý hạn ngạch nghiệp vụ người dùng (bỏ thực thể `ai_usage_record`); hệ thống TUYỆT ĐỐI KHÔNG lưu trữ nội dung câu hỏi thô (raw prompt content) nhằm bảo vệ quyền riêng tư người dùng; thời hạn lưu trữ dữ liệu đo lường là 90 ngày; các lỗi từ nhà cung cấp không được ghi nhận là lượt thành công.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi request gọi Gemini AI thành công từ các tính năng: AI Chatbot theo ngữ cảnh (FR-51), AI gợi ý thực đơn (FR-34, FR-36), AI hỗ trợ tạo bài (FR-21), Gợi ý món biến tấu (FR-47).
  - Bảo mật dữ liệu: Tuyệt đối cấm lưu trữ nội dung văn bản câu hỏi của người dùng trong bảng dữ liệu đo lường (BR-04, NFR-08, NFR-20).
  - Vòng đời lưu trữ: Lưu trữ tối đa 90 ngày (BR-04); tự động dọn dẹp các bản ghi quá hạn.
- **Phân loại Actor:**
  - Primary Actor: `Administrator` (người dùng quản trị xem báo cáo thống kê mức độ tiêu thụ token và chi phí AI).
  - Supporting System / Mechanism: `Hệ thống đo lường và ghi vết AI (AI Telemetry Subsystem)` (tiến trình ghi nhận dữ liệu kỹ thuật và dọn dẹp ngầm).

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-11.2`: Administrator xem bảng thống kê lượng tiêu thụ token và chi phí ước tính theo chu kỳ (View AI token consumption and cost statistics).
  *(Ghi chú: UC-11.1 trước đây về ghi nhận telemetry sau lượt gọi AI và UC-11.3 về tự động thanh lọc dữ liệu sau 90 ngày không còn là các Use Case độc lập vì đây là các cơ chế kỹ thuật ngầm của hệ thống chứ không phải mục tiêu của Actor; hai hành vi này được chuẩn hóa tương ứng thành Cơ chế ghi nhận telemetry hệ thống sau lượt gọi AI và Luồng dọn dẹp dữ liệu ngầm theo chu kỳ).*
- **User Stories:**
  - *Là một Administrator quản trị hệ thống*, tôi muốn theo dõi tổng số token tiêu thụ thực tế của từng tính năng AI mà không lưu trữ nội dung riêng tư của người dùng, để tôi có thể ước tính chi phí API hàng tháng và tối ưu hóa hệ thống.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Dữ liệu đo lường kỹ thuật chỉ có Administrator mới có quyền xem báo cáo tổng hợp. Người dùng thông thường không có quyền truy cập.
- **Ràng buộc an toàn:** Tuân thủ thời hạn lưu trữ 90 ngày và cấm lưu trữ raw prompt (BR-04).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-03](BUSINESS-RULES.md#br-03): Xác thực quyền tính năng trước khi gọi dịch vụ AI.
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider, timeout AI và lưu trữ telemetry.
  - [BR-06](BUSINESS-RULES.md#br-06): Bảo mật Gemini API Key.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-05](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-05): Khả năng chịu tải đồng thời của hệ thống.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.
  - [NFR-20](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20): Cơ chế bảo vệ dữ liệu sức khỏe cá nhân và ranh giới thông tin.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
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
  - *Then* hệ thống không tạo bản ghi đo lường AI thành công và ghi log lỗi kỹ thuật.
- **AC-11.4 (Tự động thanh lọc dữ liệu quá 90 ngày):**
  - *Given* các bản ghi đo lường AI có dấu thời gian được tạo cách đây 91 ngày,
  - *When* tiến trình dọn dẹp định kỳ thực thi,
  - *Then* các bản ghi cũ hơn 90 ngày được xóa khỏi cơ sở dữ liệu telemetry.
- **AC-11.5 (Phân quyền bảo mật dữ liệu đo lường):**
  - *Given* một người dùng Member thông thường cố gắng truy cập dữ liệu đo lường token AI,
  - *When* hệ thống kiểm tra quyền hạn,
  - *Then* hệ thống từ chối truy cập và chỉ cho phép tài khoản Administrator xem báo cáo tổng hợp.

---

---

<a id="fr-13"></a>
### FR-13 — Đăng ký gói AI qua thanh toán thật

- **Mã yêu cầu:** FR-13
- **Module:** M08

- **Use Case detail:** [Open interaction flows](../use-cases/subscription-and-payment.md#fr-13).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp tính năng đăng ký và thanh toán trực tuyến gói dịch vụ AI cho Member thông qua cổng thanh toán thật: hỗ trợ 3 gói dịch vụ theo tháng thanh toán bằng đồng Việt Nam (VNĐ): FREE (0 VNĐ/tháng), PLUS (49.000 VNĐ/tháng) và PRO (99.000 VNĐ/tháng); phân quyền sử dụng theo mô hình Feature-based Entitlement (không giới hạn số lượt/ngày); trong phạm vi MVP hệ thống không tự động gia hạn (No auto-renewal) và không áp dụng chính sách hoàn tiền một phần (No partial refund); quyền lợi tính năng AI của gói trả phí CHỈ ĐƯỢC KÍCH HOẠT sau khi giao dịch thanh toán thành công đã được xác minh toàn vẹn; quyền lợi tự động hết hạn khi kết thúc chu kỳ tháng đã trả phí (verified monthly paid period); việc xử lý các sự kiện thanh toán lặp (duplicate callback/IPN) phải bảo đảm tính idempotent; không hỗ trợ gói năm, mã giảm giá, khuyến mãi hay dùng thử.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập tài khoản hợp lệ (FR-03).
  - Bảng định giá và quyền lợi tính năng chính thức Phase 1:
    | Gói dịch vụ | Mức giá niêm yết | Quyền tính năng AI (Feature Entitlement) | Thời hạn chu kỳ | Gia hạn tự động |
    |---|---|---|---|---|
    | **FREE** | **0 VNĐ / tháng** | AI Chatbot cơ bản (FR-51) + Gợi ý món theo nguyên liệu (`UC-34.1` thuộc FR-34) | Không thời hạn | Không |
    | **PLUS** | **49.000 VNĐ / tháng** | Quyền gói Free + AI lập thực đơn tuần (UC-34.2/UC-34.3, FR-36) | Chu kỳ tháng (Monthly) | **Không (Thanh toán từng kỳ)** |
    | **PRO** | **99.000 VNĐ / tháng** | Toàn bộ quyền gói Plus + AI soạn bài cho Chuyên gia (FR-21) + AI gợi ý món liên quan hoặc biến tấu (FR-47) + AI đề xuất điều chỉnh dinh dưỡng (FR-37) | Chu kỳ tháng (Monthly) | **Không (Thanh toán từng kỳ)** |
  - Tiền tệ: Đồng Việt Nam (VNĐ) duy nhất.
  - Không hỗ trợ: Gói năm, coupon, giảm giá, trial, hoàn tiền một phần.
- **Phân loại Actor:**
  - Primary Actor: `Member` (người đăng ký mua gói).
  - Supporting Actor: `Cổng thanh toán trực tuyến`, `Hệ thống quản lý Subscription & Entitlement`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-13.1`: Xem bảng giá, quyền lợi và so sánh các gói dịch vụ (Free, Plus, Pro).
  - `UC-13.2`: Khởi tạo đơn hàng và thanh toán nâng cấp gói dịch vụ (Plus 49.000 VNĐ hoặc Pro 99.000 VNĐ).
  - `UC-13.3`: Xem thông tin trạng thái gói dịch vụ hiện tại và ngày hết hạn chu kỳ đã trả phí.
- **User Stories:**
  - *Là một người dùng thường xuyên cần AI lên thực đơn*, tôi muốn nâng cấp lên gói Plus 49.000 VNĐ/tháng một cách an toàn và minh bạch để mở khóa các tính năng AI lập thực đơn tuần và hỗ trợ dinh dưỡng mà không bị tự động trừ tiền âm thầm vào tháng sau.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member đã đăng nhập mới có quyền thực hiện giao dịch thanh toán nâng cấp.
- **Ràng buộc Phase 1:** Không tự động gia hạn; không hoàn tiền một phần; chỉ VND; đúng mức giá 49.000 VNĐ (Plus) và 99.000 VNĐ (Pro); không có mã giảm giá hay gói năm.

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Quyền sử dụng AI Chat cho Guest và Member Free.
  - [BR-02](BUSINESS-RULES.md#br-02): Phân quyền tính năng AI cho gói Plus và Pro.
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-05](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-05): Khả năng chịu tải đồng thời của hệ thống.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật (chống giả mạo chữ ký IPN, chống IDOR).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-13.1 (Bảng giá hiển thị đúng chuẩn Phase 1):**
  - *Given* người dùng truy cập trang bảng giá dịch vụ,
  - *When* trang hiển thị,
  - *Then* hệ thống trình bày chính xác 3 gói: FREE 0 VNĐ/tháng, PLUS 49.000 VNĐ/tháng, PRO 99.000 VNĐ/tháng; không hiển thị gói năm, mã giảm giá hay gói dùng thử.
- **AC-13.2 (Kích hoạt quyền lợi chỉ sau khi thanh toán thành công):**
  - *Given* Member thanh toán gói Plus 49.000 VNĐ,
  - *When* hệ thống nhận được thông báo IPN thành công có chữ ký hợp lệ từ cổng thanh toán,
  - *Then* trạng thái tài khoản chuyển sang gói Plus với quyền lập thực đơn tuần (UC-34.2/UC-34.3, FR-36) tương ứng với chu kỳ tháng đã thanh toán.
- **AC-13.3 (Xử lý giao dịch lặp đảm bảo tính Idempotent):**
  - *Given* một giao dịch đã được hệ thống xử lý kích hoạt thành công,
  - *When* cổng thanh toán gửi lại bản tin IPN thứ hai cho cùng mã giao dịch đó,
  - *Then* hệ thống xác nhận xử lý thành công mà không cộng thêm thời hạn chu kỳ hay trừ tiền lặp.
- **AC-13.4 (Hết hạn chu kỳ chuyển về Free không tự gia hạn):**
  - *Given* gói Plus của Member đã hết chu kỳ tháng đã trả phí,
  - *When* thời điểm hết hạn kết thúc,
  - *Then* hệ thống tự động chuyển tài khoản về gói Free và không thực hiện bất kỳ lệnh trừ tiền tự động nào.
- **AC-13.5 (Từ chối yêu cầu có chữ ký hoặc số tiền giả mạo):**
  - *Given* một bản tin thanh toán gửi về máy chủ có chữ ký số không hợp lệ hoặc số tiền sai lệch với giá gói,
  - *When* hệ thống xác thực dữ liệu giao dịch,
  - *Then* hệ thống từ chối kích hoạt quyền lợi và ghi log cảnh báo an ninh.
- **AC-13.6 (Giữ nguyên quyền lợi hiện tại khi giao dịch bị hủy hoặc thất bại):**
  - *Given* Member đang có quyền lợi gói hiện tại và giao dịch nâng cấp bị hủy hoặc thất bại,
  - *When* hệ thống ghi nhận kết quả giao dịch,
  - *Then* hệ thống giữ nguyên quyền lợi hiện tại, không kích hoạt gói mới và thông báo giao dịch chưa hoàn tất.
- **AC-13.7 (Hiển thị trạng thái gói và ngày hết hạn):**
  - *Given* Member đã đăng nhập và mở trang thông tin tài khoản,
  - *When* hệ thống hiển thị thông tin Subscription,
  - *Then* Member xem được trạng thái gói hiện tại và, nếu là gói trả phí, ngày hết hạn của chu kỳ đã thanh toán.

---

---

<a id="fr-14"></a>
### FR-14 — Lưu trữ thư viện ảnh bài công thức (tối đa 5 ảnh, đúng 1 ảnh bìa) trên Azure Blob Storage

- **Mã yêu cầu:** FR-14
- **Module:** M03

- **Use Case detail:** [Open interaction flows](../use-cases/recipe-contribution-and-community.md#fr-14).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống cung cấp cơ chế lưu trữ thư viện ảnh của bài công thức nấu ăn thông qua thực thể `RECIPE_MEDIA` trên dịch vụ đám mây Azure Blob Storage; mỗi bài công thức có tối đa 5 ảnh minh họa (tùy chọn theo BR-20); nếu bài có từ 1 đến 5 ảnh thì **BẮT BUỘC phải có đúng 1 ảnh được chọn làm ảnh bìa (`is_cover = true`)**; mỗi ảnh có thứ tự hiển thị (`display_order`); áp dụng kiểm tra định dạng và dung lượng ($\le 5$ MB/ảnh, JPEG/PNG/WebP) trước khi tải lên; giải phóng tài nguyên ảnh khi bài công thức bị xóa hoặc khi tác giả gỡ ảnh; tuyệt đối không lưu tệp nhị phân trực tiếp trong cơ sở dữ liệu quan hệ.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Thư viện ảnh minh họa được Chuyên gia tải lên khi tạo hoặc chỉnh sửa bài công thức của chính mình (FR-04, FR-21, FR-44).
  - Giới hạn: Tối đa **5 ảnh minh họa** cho mỗi Recipe Post trong `RECIPE_MEDIA`.
  - Quy tắc ảnh bìa: Khi bài có $\ge 1$ ảnh, phải có đúng 1 ảnh mang cờ `is_cover = true`.
  - Định dạng hỗ trợ: JPEG, PNG, WebP; dung lượng tối đa $\le 5$ MB trên mỗi tệp tin (NFR-10).
- **Phân loại Actor:**
  - Primary Actor: `Chuyên gia (Role = EXPERT)` (tác giả tải ảnh lên).
  - Supporting Actor: `Azure Blob Storage` (dịch vụ lưu trữ đám mây).

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-14.1`: Tải tệp tin ảnh lên Azure Blob Storage và tạo bản ghi `RECIPE_MEDIA` (Upload recipe images to Azure Blob Storage).
  - `UC-14.2`: Chọn ảnh bìa và sắp xếp thứ tự hiển thị ảnh minh họa (Set cover image and arrange display order).
  - `UC-14.3`: Hiển thị thư viện ảnh từ Azure Blob Storage với tốc độ cao trên giao diện thẻ món và chi tiết công thức (Display recipe media gallery).
- **User Stories:**
  - *Là một người chia sẻ công thức nấu ăn*, tôi muốn tải lên tối đa 5 bức ảnh chụp từng công đoạn và thành phẩm món ăn, chọn bức ảnh đẹp nhất làm ảnh bìa để bài viết của tôi trông sinh động, bắt mắt và thu hút cộng đồng.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chuyên gia chỉ được tải/xóa ảnh của công thức do chính mình sở hữu; Administrator có quyền quản lý ảnh của công thức hệ thống.
- **Ràng buộc:** Media công khai trực tiếp cùng bài viết, không qua duyệt trước media (BR-07, BR-11); nếu vi phạm chuẩn mực sẽ bị xử lý qua cơ chế hậu kiểm (FR-06, FR-28).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Quyền đăng và công khai Recipe Post dành riêng cho Chuyên gia (Role = EXPERT).
  - [BR-11](BUSINESS-RULES.md#br-11): Media công khai trực tiếp và hậu kiểm.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post (0..5 ảnh, đúng 1 ảnh bìa nếu có ảnh).
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của ảnh đại diện (0..5 ảnh, đúng 1 cover; bài 0 ảnh dùng ảnh mặc định).
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian tải trang hiển thị chi tiết bài viết $\le 2$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Kiểm soát upload tệp tin an toàn.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive hiển thị ảnh linh hoạt.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-14.1 (Giới hạn số lượng, dung lượng và định dạng ảnh):**
  - *Given* tác giả chọn tệp tin ảnh $> 5$ MB, hoặc chọn $> 5$ ảnh, hoặc tệp tin không phải JPEG/PNG/WebP,
  - *When* tác giả tải lên,
  - *Then* hệ thống từ chối tải tệp tin và thông báo lỗi rõ ràng.
- **AC-14.2 (Lưu trữ thư viện tối đa 5 ảnh vào RECIPE_MEDIA thành công):**
  - *Given* tác giả tải lên từ 1 đến 5 ảnh hợp lệ ($\le 5$ MB/ảnh, định dạng JPEG/PNG/WebP),
  - *When* quá trình tải lên hoàn tất,
  - *Then* các tệp tin được lưu trên Azure Blob Storage và hệ thống tạo tương ứng 1–5 bản ghi trong thực thể `RECIPE_MEDIA` gắn với `recipe_id`.
- **AC-14.3 (Ràng buộc bắt buộc đúng 1 ảnh bìa khi có ảnh):**
  - *Given* bài công thức có tải lên từ 1 đến 5 ảnh trong `RECIPE_MEDIA`,
  - *When* tác giả nhấn công khai bài viết,
  - *Then* hệ thống kiểm tra và chỉ cho phép công khai khi có đúng 1 ảnh mang cờ `is_cover = true`; từ chối công khai nếu 0 hoặc $> 1$ ảnh bìa (BR-19).
- **AC-14.4 (Không lưu tệp nhị phân trong cơ sở dữ liệu quan hệ):**
  - *Given* hình ảnh bài công thức được lưu thành công,
  - *When* kiểm tra bảng `RECIPE_MEDIA` trong cơ sở dữ liệu hệ thống,
  - *Then* bản ghi chỉ chứa Blob URL và metadata (thứ tự hiển thị, cờ cover), hoàn toàn không chứa dữ liệu nhị phân.

---

---

<a id="fr-15"></a>
### FR-15 — Nhúng trình phát YouTube trong bài công thức

- **Mã yêu cầu:** FR-15
- **Module:** M01, M03

- **Use Case detail:** [Open interaction flows](../use-cases/discovery-and-public-content.md#fr-15).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống cho phép tác giả bài công thức (Chuyên gia) gắn tối đa một đường link hoặc video ID hợp lệ của YouTube vào bài Recipe Post khi tạo hoặc chỉnh sửa; hệ thống kiểm tra tính hợp lệ của liên kết, trích xuất mã định danh video YouTube và nhúng trình phát video (YouTube IFrame Player) an toàn trên trang chi tiết công thức để phục vụ người xem; hệ thống TUYỆT ĐỐI KHÔNG nhận tải lên, sao chép hoặc lưu trữ tệp tin video nhị phân của YouTube trên máy chủ hay bộ lưu trữ đám mây (BR-10); video là thành phần hoàn toàn tùy chọn (BR-20).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi bài công thức được Chuyên gia tạo hoặc chỉnh sửa (FR-04, FR-16, FR-44).
  - Nền tảng hỗ trợ: YouTube duy nhất trong Phase 1 (BR-10).
  - Giới hạn: Tối đa **1 liên kết video YouTube** cho mỗi bài công thức (BR-19).
- **Phân loại Actor:**
  - Primary Actor: `Chuyên gia` (tác giả gắn link video), `Guest / Member` (người xem video trên bài viết).
  - Supporting Actor: `YouTube IFrame API / Player`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-15.1`: Gắn hoặc cập nhật liên kết video YouTube khi soạn thảo bài công thức.
  - `UC-15.2`: Xem video hướng dẫn nấu ăn được nhúng trực tiếp trên trang chi tiết Recipe Post.
- **User Stories:**
  - *Là một người nấu ăn*, tôi muốn xem video YouTube quay lại các công đoạn nấu ăn ngay trong bài viết để dễ dàng hình dung kỹ thuật chế biến món ăn mà không phải chuyển sang ứng dụng khác.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Mọi tác giả bài viết đều có quyền gắn link YouTube; mọi người xem (Guest và Member) đều có quyền xem video.
- **Ràng buộc kiến trúc:** Nghiêm cấm nhận file upload video; chỉ nhúng YouTube (BR-10).

#### Ma trận truy vết (Traceability Matrix)
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

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
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

---

<a id="fr-16"></a>
### FR-16 — Cấu trúc dữ liệu bài công thức, hướng dẫn chế biến và validation profile

- **Mã yêu cầu:** FR-16
- **Module:** M03, M04

- **Use Case detail:** [Open interaction flows](../use-cases/recipe-contribution-and-community.md#fr-16).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Thiết lập cấu trúc dữ liệu chuẩn mực và bộ quy tắc kiểm tra hợp lệ (Recipe Validation Profile) bắt buộc đối với mọi Recipe Post trước khi được công khai trong hệ thống theo SRS 3.9, BR-19 và BR-73: tiêu đề từ 3 đến 120 ký tự; thể loại món ăn bắt buộc chọn 1 giá trị chuẩn hóa thuộc `dish_category`; số lượng nguyên liệu từ 1 đến 50 dòng và mỗi dòng bắt buộc tham chiếu nguyên liệu trong danh mục chuẩn theo FR-19/BR-12, có định lượng số học dương và tỷ lệ quy đổi hợp lệ sang gam (cấm "vừa đủ"); số khẩu phần từ 1 đến 50; thời gian chuẩn bị và nấu mỗi giá trị từ 0 đến 1.440 phút với tổng thời gian lớn hơn 0 (thời gian nấu được phép bằng 0 theo BR-20 khi thời gian chuẩn bị lớn hơn 0); loại ăn chay bắt buộc thuộc 4 loại chuẩn; mô tả bài viết tối đa 2.000 ký tự (tùy chọn theo BR-20); hướng dẫn chế biến dạng văn bản từ 10 đến 5.000 ký tự sau khi trim (người đăng không bắt buộc phải viết từng bước nấu ăn vào bài viết, có thể viết đoạn văn tự do hoặc phân bước tùy ý theo BR-19); thư viện ảnh tối đa 5 ảnh JPEG/PNG/WebP dung lượng $\le 5$ MB/ảnh (`RECIPE_MEDIA`), nếu có ảnh bắt buộc đúng 1 ảnh bìa (`is_cover = true`) (tùy chọn theo BR-19, BR-20); tối đa một link YouTube (tùy chọn theo BR-10, BR-20); tất cả bài công thức đều áp dụng thống nhất mô hình xuất bản trực tiếp và hậu kiểm (BR-07, BR-59).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi bài Recipe Post do Chuyên gia tạo hoặc chỉnh sửa (FR-04, FR-44).
  - Validation Profile chính thức:
    | Trường dữ liệu | Ràng buộc giá trị hợp lệ | Bắt buộc / Tùy chọn |
    |---|---|---|
    | **Tiêu đề (Title)** | **3 – 120 ký tự** | Bắt buộc |
    | **Thể loại món (`dish_category`)** | Chọn đúng 1 trong danh mục chuẩn: `món nước`, `món xào`, `món lẩu`, `món kho`, `món canh`, `món chiên`, `món hấp`, `món gỏi / salad`, `món cuốn`, `món nướng`, `món tráng miệng / chè` | Bắt buộc (BR-19) |
    | **Danh sách nguyên liệu** | **1 – 50 dòng nguyên liệu**; mỗi dòng bắt buộc tham chiếu mục catalog FR-18 (BR-12), có định lượng số học dương (`quantity > 0`), đơn vị thuộc `UNIT`, cấm "vừa đủ" | Bắt buộc (BR-12, BR-14, BR-73) |
    | **Quy đổi đơn vị nguyên liệu** | Đơn vị bắt buộc có tỷ lệ quy đổi sang gam trong `INGREDIENT_UNIT_CONVERSION` (hoặc là `MASS` g/kg); nếu thiếu **chặn xuất bản** | Bắt buộc (BR-19, BR-73) |
    | **Hướng dẫn chế biến (`instructions`)** | Văn bản hướng dẫn từ **10 – 5.000 ký tự** không rỗng sau khi trim; **không bắt buộc phải viết theo từng bước nấu ăn** (có thể nhập tự do hoặc chia đoạn tùy ý) | Bắt buộc (BR-19) |
    | **Khẩu phần (Serving size)** | **1 – 50 khẩu phần** | Bắt buộc |
    | **Thời gian chuẩn bị (Prep time)** | **0 – 1.440 phút** | Bắt buộc |
    | **Thời gian nấu (Cook time)** | **0 – 1.440 phút** (được bằng 0 khi prep time > 0) | Bắt buộc |
    | **Tổng thời gian** | **Prep time + Cook time > 0** | Bắt buộc |
    | **Trường phái ăn chay** | Chọn 1 trong 4 loại chuẩn (Vegan, Lacto, Ovo, Lacto-Ovo) | Bắt buộc |
    | **Mô tả món ăn (Description)** | **Tối đa 2.000 ký tự** | Tùy chọn (BR-20) |
    | **Thư viện ảnh (`RECIPE_MEDIA`)** | **0 – 5 ảnh**, JPEG/PNG/WebP, $\le 5$ MB/ảnh; khi có $\ge 1$ ảnh thì **bắt buộc đúng 1 ảnh bìa** (`is_cover = true`) | Tùy chọn (BR-19, BR-20, FR-14) |
    | **Video YouTube** | **0 – 1 link YouTube** hợp lệ | Tùy chọn (BR-10, BR-20, FR-15) |
- **Phân loại Actor:**
  - Primary Actor: `Chuyên gia (Role = EXPERT)` (tác giả bài viết).
  - Supporting Actor: `Hệ thống kiểm tra tính hợp lệ dữ liệu (Validation Subsystem)`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-16.1`: Soạn thảo và xác thực bài công thức tuân thủ cấu trúc dữ liệu, hướng dẫn chế biến và tỷ lệ quy đổi đơn vị bắt buộc.
  - `UC-16.2`: Công khai bài công thức không có ảnh minh họa (sử dụng ảnh mặc định) hoặc không có mô tả giới thiệu.
- **User Stories:**
  - *Là một người nấu ăn*, tôi muốn nhập hướng dẫn chế biến linh hoạt và rõ ràng để người đọc dễ dàng làm theo, chọn thể loại món phù hợp, đồng thời hệ thống kiểm tra các đơn vị đo lường có quy đổi được không để tính toán dinh dưỡng chính xác.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Áp dụng bình đẳng cho mọi bài viết do Chuyên gia tạo; Administrator không phải tác giả (AC-04.7).
- **Ràng buộc nghiệp vụ:** Đúng bộ tham số đã phê duyệt; bắt buộc có hướng dẫn chế biến (`instructions` từ 10 đến 5.000 ký tự); thể loại món `dish_category` hợp lệ; nguyên liệu có tỷ lệ quy đổi hợp lệ sang gam (BR-73); thư viện ảnh tối đa 5 ảnh với đúng 1 ảnh bìa (BR-19, BR-20).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Quyền đăng và công khai Recipe Post dành riêng cho Chuyên gia (Role = EXPERT).
  - [BR-10](BUSINESS-RULES.md#br-10): Giới hạn định dạng video Phase 1.
  - [BR-14](BUSINESS-RULES.md#br-14): Định lượng nguyên liệu số học theo đơn vị chuẩn.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post (hướng dẫn chế biến linh hoạt, 0..5 ảnh với 1 ảnh bìa, tỷ lệ quy đổi đơn vị).
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của mô tả giới thiệu và ảnh đại diện.
  - [BR-59](BUSINESS-RULES.md#br-59): Không có hàng đợi duyệt bài trước khi công khai.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
  - [BR-73](BUSINESS-RULES.md#br-73): Chuẩn hóa đơn vị đo lường và chặn công khai khi thiếu quy đổi.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến (Validation tầng Backend).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
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
- **AC-16.5 (Bắt buộc có hướng dẫn chế biến instructions từ 10 đến 5.000 ký tự):**
  - *Given* bài công thức chưa có nội dung hướng dẫn chế biến, hoặc nội dung $< 10$ ký tự, hoặc vượt quá 5.000 ký tự sau khi trim,
  - *When* tác giả gửi yêu cầu đăng bài,
  - *Then* hệ thống từ chối xuất bản và hiển thị thông báo lỗi yêu cầu độ dài hướng dẫn chế biến từ 10 đến 5.000 ký tự (BR-19).
- **AC-16.6 (Chặn xuất bản nghiêm ngặt khi nguyên liệu thiếu tỷ lệ quy đổi sang gam):**
  - *Given* tác giả chọn nguyên liệu dùng đơn vị "quả" hoặc "muỗng canh" nhưng nguyên liệu đó chưa được định nghĩa tỷ lệ quy đổi sang gam trong `INGREDIENT_UNIT_CONVERSION`,
  - *When* tác giả gửi yêu cầu đăng bài,
  - *Then* hệ thống từ chối xuất bản và hiển thị lỗi validation yêu cầu chọn đơn vị khối lượng (gam/kg) hoặc liên hệ quản trị viên (BR-19, BR-73).
- **AC-16.7 (Ràng buộc đúng 1 ảnh bìa khi có 1–5 ảnh trong RECIPE_MEDIA):**
  - *Given* bài công thức có tải lên 3 ảnh nhưng tác giả chưa chọn ảnh bìa nào hoặc chọn 2 ảnh bìa,
  - *When* tác giả gửi yêu cầu đăng bài,
  - *Then* hệ thống từ chối xuất bản và thông báo yêu cầu chọn đúng 1 ảnh bìa (BR-19).
- **AC-16.8 (Cho phép công khai không có ảnh và không có mô tả):**
  - *Given* bài công thức không có mô tả và không tải ảnh nào lên nhưng có đầy đủ nguyên liệu hợp lệ, thể loại món hợp lệ và hướng dẫn chế biến hợp lệ (10–5.000 ký tự),
  - *When* tác giả nhấn đăng bài,
  - *Then* hệ thống công khai bài viết thành công, hiển thị đầy đủ nội dung hướng dẫn chế biến và gán ảnh mặc định cho món ăn theo BR-20.

---

---

<a id="fr-17"></a>
### FR-17 — Trình bày bài công thức dạng thẻ món trong Khám phá và liên kết lịch ăn

- **Mã yêu cầu:** FR-17
- **Module:** M04, M05

- **Use Case detail:** [Open interaction flows](../use-cases/recipe-discovery-and-classification.md#fr-17).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống trình bày các bài công thức đang công khai (không bị ẩn hoặc xóa) dưới dạng Thẻ món ăn (Recipe Card) trực quan và đồng nhất trong giao diện Khám phá (Trang chủ, duyệt danh mục, kết quả tìm kiếm và lọc tại FR-01, FR-08); hiển thị ảnh bìa từ `RECIPE_MEDIA` (hoặc ảnh mặc định nếu bài không có ảnh theo BR-20), tên món, tác giả, loại ăn chay, thời gian nấu, huy hiệu tỷ lệ % Like theo phong cách Samsung Food (`👍 {like_percentage}%` hoặc nhãn `Mới` nếu chưa có lượt bình chọn, theo BR-69), và tổng lượt xem (`RECIPE_VIEW` theo BR-70); tích hợp trực tiếp trên thẻ các nút hành động nhanh để kết nối thẻ món với luồng lưu công thức yêu thích (FR-32) hoặc thêm món vào Lịch ăn tuần 3 bữa (FR-09, FR-33); nhấp vào thẻ điều hướng liền mạch tới trang chi tiết công thức (FR-20).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi người dùng xem giao diện khám phá (Guest và Member).
  - Thành phần hiển thị trên thẻ món: Ảnh bìa từ `RECIPE_MEDIA` (hoặc ảnh mặc định), Tên món ăn, Thẻ tác giả (Author Card thu nhỏ theo FR-23, BR-18), Phân loại trường phái ăn chay, Tổng thời gian (prep + cook), Huy hiệu tỷ lệ phần trăm Like (`👍 {like_percentage}%` đặt nổi bật ở góc trên bên trái ảnh theo Samsung Food, hoặc nhãn `Mới`), và Tổng số lượt xem (`view_count`).
- **Phân loại Actor:**
  - Primary Actor: `Guest`, `Member` (người duyệt khám phá món ăn).
  - Supporting Actor: `Hệ thống hiển thị và quản lý thẻ món`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-17.1`: Duyệt xem danh sách các thẻ món ăn công khai trên giao diện Khám phá.
  - `UC-17.2`: Thực hiện thao tác lưu nhanh công thức hoặc thêm món vào Lịch ăn tuần trực tiếp từ thẻ món.
  - `UC-17.3`: Nhấp vào thẻ món để xem trang chi tiết đầy đủ của bài công thức.
- **User Stories:**
  - *Là một người tìm kiếm ý tưởng ăn chay*, tôi muốn lướt xem các thẻ món ăn hấp dẫn với đầy đủ thông tin về ảnh bìa, thời gian, loại chay, tỷ lệ % Like nổi bật kiểu Samsung Food và số lượt xem, đồng thời có thể bấm lưu lại hoặc thêm ngay vào thực đơn tuần mà không cần phải mở từng bài viết.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Xem thẻ món mở cho toàn bộ Guest và Member. Thao tác lưu và thêm lịch ăn chỉ dành cho Member đã đăng nhập (BR-05).
- **Ràng buộc giao diện:** Tối ưu hóa hiển thị thẻ món trên thiết bị di động (NFR-13).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong thông tin tác giả công khai.
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của ảnh đại diện (dùng ảnh mặc định khi thiếu).
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn.
  - [BR-33](BUSINESS-RULES.md#br-33): Độc lập nghiệp vụ của thao tác lưu công thức.
  - [BR-35](BUSINESS-RULES.md#br-35): Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn.
  - [BR-36](BUSINESS-RULES.md#br-36): Quy tắc 3 loại bữa ăn cố định trong Lịch ăn MVP.
  - [BR-69](BUSINESS-RULES.md#br-69): Tương tác Like / Dislike và tỷ lệ hài lòng (% Like) của bài công thức.
  - [BR-70](BUSINESS-RULES.md#br-70): Ghi nhận lượt xem và khử trùng lặp theo phiên (`RECIPE_VIEW`).
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian tải trang hiển thị chi tiết bài viết $\le 2$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-17.1 (Hiển thị đầy đủ thông tin trên thẻ món):**
  - *Given* một bài công thức đang ở trạng thái công khai hợp lệ,
  - *When* hệ thống hiển thị danh sách Khám phá,
  - *Then* thẻ món thể hiện rõ ảnh bìa từ `RECIPE_MEDIA` (hoặc ảnh mặc định), tiêu đề món ăn, tên tác giả công khai, loại ăn chay, tổng thời gian, huy hiệu tỷ lệ % Like (`👍 {like_percentage}%` hoặc nhãn `Mới` theo BR-69), và tổng lượt xem `view_count`. Like/Dislike chỉ áp dụng cho bài công thức, không áp dụng cho bình luận.
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

---

<a id="fr-18"></a>
### FR-18 — Admin quản lý danh mục nguyên liệu chuẩn, đơn vị đo lường và bảng quy đổi

- **Mã yêu cầu:** FR-18
- **Module:** M04, M09

- **Use Case detail:** [Open interaction flows](../use-cases/recipe-discovery-and-classification.md#fr-18).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp giao diện quản trị chuyên biệt dành cho Administrator để quản lý các danh mục phân loại dữ liệu nền tảng của hệ thống: Danh mục nguyên liệu chuẩn (Standard Ingredient Catalog), Danh mục đơn vị đo lường chuẩn (`UNIT`), và Bảng quy đổi đơn vị theo nguyên liệu (`INGREDIENT_UNIT_CONVERSION`) (hệ thống không sử dụng bảng động `CATEGORY` / `RECIPE_CATEGORY`, thể loại món ăn được chuẩn hóa trực tiếp thành thuộc tính `dish_category` trên Recipe Post); Administrator có thẩm quyền xem, tìm kiếm, tạo mới, chỉnh sửa thông tin, cấu hình tỷ lệ quy đổi sang gam và ngừng sử dụng (soft-disable); hệ thống nghiêm cấm xóa vĩnh viễn (hard delete) các nguyên liệu hoặc đơn vị đang được tham chiếu trong các bài công thức công khai.
- **Phạm vi nghiệp vụ:**
  - Quản lý danh mục nguyên liệu: Tên nguyên liệu chuẩn bằng tiếng Việt và nhóm nguyên liệu (Rau củ, Nấm, Đậu hạt, Gia vị, v.v.). Ứng dụng MVP không quản lý tên tiếng Anh hoặc đa ngôn ngữ.
  - Quản lý bộ đơn vị công thức chuẩn: `mg`, `g`, `kg`; `ml`, `L`, `tsp`, `tbsp`, `cup`; `quả`, `củ`, `miếng`. Hệ số khối lượng là cố định; đơn vị thể tích và đếm chỉ xuất hiện khi có quy đổi active cho đúng nguyên liệu. Đơn vị legacy tiếp tục đọc được nhưng không chọn cho công thức mới.
  - Quản lý tỷ lệ quy đổi (`INGREDIENT_UNIT_CONVERSION`): Admin nhập gram trên một đơn vị đếm hoặc gram trên 1 ml cho nguyên liệu; hệ thống đồng bộ các hệ số `L/tsp/tbsp/cup` theo 1000/5/15/240 ml. Không yêu cầu khai báo nguồn; Admin có thể đánh dấu hệ số xấp xỉ. Ngừng/khôi phục là soft-disable; không hard-delete.
  - Thể loại món (`dish_category`): Được quản lý theo danh mục giá trị chuẩn hóa thuộc hệ thống (món nước, món xào, món lẩu, món kho, món canh, món chiên, món hấp, món gỏi / salad, món cuốn, món nướng, món tráng miệng / chè), không yêu cầu bảo trì bảng phân cấp động.
  - Phân quyền: Độc quyền cho vai trò `Administrator` (M09).
- **Phân loại Actor:**
  - Primary Actor: `Administrator`.
  - Supporting Actor: `Hệ thống quản lý Danh mục phân loại`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-18.1`: Tìm kiếm, xem danh sách và quản lý danh mục nguyên liệu chuẩn của hệ thống.
  - `UC-18.2`: Quản lý danh mục đơn vị đo lường `UNIT` và phân loại thứ nguyên (`MASS`, `VOLUME`, `COUNT`).
  - `UC-18.3`: Quản lý cấu hình bảng quy đổi đơn vị theo nguyên liệu `INGREDIENT_UNIT_CONVERSION`.
- **User Stories:**
  - *Là một Administrator*, tôi muốn dễ dàng cập nhật danh mục nguyên liệu, đơn vị đo lường và tỷ lệ quy đổi sang gam cho từng nguyên liệu, để các thành viên có thể chọn đơn vị đa dạng khi đăng bài mà vẫn đảm bảo tính toán dinh dưỡng chính xác.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Độc quyền cho Administrator.
- **Ràng buộc an toàn:** Cấm xóa cứng các bản ghi đang được tham chiếu trong công thức (BR-53, BR-64).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-06](BUSINESS-RULES.md#br-06): Bảo mật hệ thống và phân quyền quản trị.
  - [BR-07](BUSINESS-RULES.md#br-07): Quyền đăng và công khai Recipe Post dành riêng cho Chuyên gia (Role = EXPERT).
  - [BR-14](BUSINESS-RULES.md#br-14): Định lượng nguyên liệu số học theo đơn vị chuẩn.
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản đăng bài.
  - [BR-53](BUSINESS-RULES.md#br-53): Cấm xóa vĩnh viễn nguyên liệu/danh mục đã tham chiếu.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
  - [BR-73](BUSINESS-RULES.md#br-73): Chuẩn hóa đơn vị đo lường và chặn công khai khi thiếu quy đổi.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-18.1 (Phân quyền quản lý danh mục chỉ dành cho Administrator):**
  - *Given* người dùng đăng nhập tài khoản Member thông thường,
  - *When* cố gắng truy cập trang hoặc gọi API quản lý danh mục/đơn vị/bảng quy đổi,
  - *Then* hệ thống từ chối thực hiện và phản hồi mã lỗi 403 Forbidden.
- **AC-18.2 (Thêm mới nguyên liệu chuẩn hợp lệ):**
  - *Given* Administrator nhập tên nguyên liệu chuẩn mới hợp lệ chưa từng tồn tại,
  - *When* nhấn lưu nguyên liệu,
  - *Then* hệ thống tạo mới nguyên liệu thành công và hiển thị trong danh sách lựa chọn nguyên liệu khi tạo bài viết.
- **AC-18.3 (Cấu hình tỷ lệ quy đổi nguyên liệu - đơn vị thành công):**
  - *Given* Administrator chọn nguyên liệu "Chuối tây", chọn đơn vị "quả" (thứ nguyên `COUNT`), và nhập tỷ lệ quy đổi là 120 gam,
  - *When* nhấn lưu cấu hình quy đổi,
  - *Then* hệ thống ghi nhận vào bảng `INGREDIENT_UNIT_CONVERSION` thành công và cho phép các công thức chứa "Chuối tây" dùng đơn vị "quả" vượt qua validation xuất bản (BR-73).
- **AC-18.4 (Ngăn chặn xóa vĩnh viễn nguyên liệu/đơn vị đang có bài viết liên kết):**
  - *Given* nguyên liệu "Đậu phụ mơ" đang có 10 bài công thức công khai liên kết,
  - *When* Administrator gửi yêu cầu xóa vĩnh viễn nguyên liệu này,
  - *Then* hệ thống từ chối xóa, giữ nguyên dữ liệu và yêu cầu chuyển sang trạng thái ngừng sử dụng.
- **AC-18.5 (Tìm kiếm danh mục nguyên liệu chuẩn):**
  - *Given* danh mục có các nguyên liệu chuẩn,
  - *When* Administrator tìm kiếm trong danh mục,
  - *Then* hệ thống hiển thị các nguyên liệu phù hợp với nội dung tìm kiếm.
- **AC-18.6 (Chỉnh sửa thông tin nguyên liệu chuẩn):**
  - *Given* Administrator chỉnh sửa tên nguyên liệu tiếng Việt hoặc nhóm của một nguyên liệu,
  - *When* Administrator lưu thay đổi,
  - *Then* danh mục hiển thị thông tin đã cập nhật.
- **AC-18.7 (Ngừng sử dụng nguyên liệu nhưng giữ liên kết lịch sử):**
  - *Given* nguyên liệu chuẩn đang được ít nhất một Recipe Post ở trạng thái `PUBLISHED` tham chiếu,
  - *When* Administrator yêu cầu chuyển nguyên liệu từ `ACTIVE` sang `INACTIVE`,
  - *Then* hệ thống từ chối thao tác, giữ nguyên trạng thái `ACTIVE` và thông báo rằng nguyên liệu đang được công thức công khai sử dụng.
  - *Given* không còn Recipe Post ở trạng thái `PUBLISHED` nào tham chiếu nguyên liệu chuẩn,
  - *When* Administrator yêu cầu chuyển nguyên liệu từ `ACTIVE` sang `INACTIVE`,
  - *Then* hệ thống cho phép ngừng sử dụng; nguyên liệu không còn được chọn cho liên kết mới và các liên kết lịch sử vẫn được bảo toàn.
  - Việc kiểm tra dựa trên trạng thái hiện tại của Recipe Post tại thời điểm yêu cầu; quy tắc này chỉ áp dụng cho nguyên liệu, không áp dụng cho đơn vị đo lường. Nguyên liệu đã `INACTIVE` trước khi quy tắc có hiệu lực được giữ nguyên hiện trạng.
- **AC-18.8 (Hiển thị danh mục đơn vị đo lường):**
  - *Given* Administrator mở danh mục `UNIT`,
  - *When* danh mục được hiển thị,
  - *Then* hệ thống trình bày tên, ký hiệu và thứ nguyên của các đơn vị đo lường.
- **AC-18.9 (Thêm đơn vị đo lường):**
  - *Given* Administrator nhập tên, ký hiệu và một thứ nguyên hợp lệ (`MASS`, `VOLUME` hoặc `COUNT`) cho đơn vị mới,
  - *When* Administrator lưu đơn vị,
  - *Then* đơn vị mới xuất hiện trong danh mục `UNIT`.
- **AC-18.10 (Chỉnh sửa đơn vị đo lường):**
  - *Given* Administrator chỉnh sửa tên, ký hiệu hoặc thứ nguyên của một đơn vị,
  - *When* Administrator lưu thay đổi,
  - *Then* danh mục `UNIT` hiển thị thông tin đã cập nhật.
- **AC-18.11 (Ngừng sử dụng đơn vị nhưng giữ liên kết lịch sử):**
  - *Given* đơn vị đo lường đang được tham chiếu bởi dữ liệu công thức,
  - *When* Administrator ngừng sử dụng đơn vị đó,
  - *Then* đơn vị không còn được chọn cho liên kết mới và các liên kết lịch sử vẫn được bảo toàn.
- **AC-18.12 (Từ chối tỷ lệ quy đổi không dương):**
  - *Given* Administrator cấu hình tỷ lệ quy đổi nguyên liệu–đơn vị với số gam tương đương bằng hoặc nhỏ hơn 0,
  - *When* Administrator lưu cấu hình,
  - *Then* hệ thống từ chối lưu cấu hình đó.
- **AC-18.13 (Từ chối cặp nguyên liệu–đơn vị quy đổi bị trùng):**
  - *Given* đã tồn tại một cấu hình quy đổi cho cùng cặp nguyên liệu và đơn vị,
  - *When* Administrator tạo thêm cấu hình quy đổi cho cặp đó,
  - *Then* hệ thống từ chối tạo bản ghi trùng.
- **AC-18.14 (Đồng bộ quy đổi thể tích):** Admin nhập gram trên 1 ml cho một nguyên liệu; Backend lưu đồng bộ gram trên đơn vị cho `ml`, `L`, `tsp`, `tbsp`, `cup` theo hệ số 1, 1000, 5, 15, 240.
- **AC-18.15 (Nhập hệ số quy đổi):** Admin nhập trực tiếp hệ số cho nguyên liệu; hệ thống không yêu cầu khai báo loại nguồn hay thông tin tham khảo. Admin có thể bật `is_approximate` để đánh dấu hệ số xấp xỉ.
- **AC-18.16 (Soft-disable và khôi phục):** Admin ngừng/khôi phục quy đổi theo nguyên liệu; đơn vị ngừng hỗ trợ không được trả trong danh sách lựa chọn công thức mới.

---

---

<a id="fr-19"></a>
### FR-19 — Chọn nguyên liệu từ danh mục chuẩn và khai báo định lượng

- **Mã yêu cầu:** FR-19
- **Module:** M03, M04

- **Use Case detail:** [Open interaction flows](../use-cases/recipe-contribution-and-community.md#fr-19).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Khi tạo hoặc sửa Recipe Post, Chuyên gia chỉ được chọn nguyên liệu trong catalog chuẩn (FR-18), đơn vị trong bộ 11 mã chuẩn và được hỗ trợ cho đúng nguyên liệu. `mg/g/kg` có quy đổi cố định; thể tích và số đếm cần conversion active riêng theo nguyên liệu. Đơn vị legacy được giữ để hiển thị bài cũ nhưng không được chọn cho bài mới; khi sửa bài cũ, tác giả phải chọn đơn vị đang hỗ trợ và kiểm tra lại số lượng. Mỗi bài có 1–50 dòng, số lượng dương; cấm "vừa đủ". Backend xác thực lại khi lưu. Thiếu hồ sơ dinh dưỡng không tự chặn chọn/công khai (FR-40, BR-50).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi bài Recipe Post tạo mới hoặc chỉnh sửa trong hệ thống (FR-04, FR-16, FR-44).
  - Số lượng: Tối thiểu 1 dòng nguyên liệu, tối đa 50 dòng nguyên liệu (BR-19).
  - Định lượng bắt buộc: 100% dòng nguyên liệu phải có số lượng số học dương (`quantity > 0`) và đơn vị đo hợp lệ; cấm tuyệt đối chữ "vừa đủ" (BR-14).
  - Ràng buộc quy đổi: Bắt buộc có tỷ lệ quy đổi về gam trong `INGREDIENT_UNIT_CONVERSION` đối với các đơn vị ngoài `MASS` g/kg để phục vụ tính dinh dưỡng (BR-73).
- **Phân loại Actor:**
  - Primary Actor: `Chuyên gia` (tác giả bài viết).
  - Supporting Actor: `Danh mục nguyên liệu chuẩn do Administrator quản lý`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-19.1`: Tìm kiếm và chọn nguyên liệu từ danh mục chuẩn khi tạo hoặc sửa công thức.
  - `UC-19.2`: Xử lý khi không tìm thấy nguyên liệu trong danh mục chuẩn.
  - `UC-19.3`: Quản lý danh sách 1–50 dòng nguyên liệu trong bài công thức (thêm, sửa, xóa, sắp xếp dòng).
- **User Stories:**
  - *Là một Chuyên gia*, tôi muốn chọn nguyên liệu từ danh mục chuẩn rồi khai báo định lượng và đơn vị rõ ràng để các công thức dùng thống nhất cùng một danh mục.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chuyên gia tạo hoặc sửa Recipe Post chỉ được chọn nguyên liệu từ danh mục chuẩn; chỉ Administrator quản lý catalog theo FR-18.
- **Ràng buộc nghiệp vụ:** Bắt buộc tham chiếu nguyên liệu chuẩn (BR-12); cấm tên tự do và luồng đề xuất nguyên liệu; cấm tuyệt đối định lượng "vừa đủ" (BR-14); bắt buộc có tỷ lệ quy đổi sang gam khi xuất bản (BR-73); đúng giới hạn 1–50 dòng (BR-19). Nguyên liệu chuẩn thiếu hồ sơ dinh dưỡng vẫn được chọn và xử lý theo FR-40/BR-50.

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-12](BUSINESS-RULES.md#br-12): Bắt buộc liên kết tới nguyên liệu chuẩn.
  - [BR-14](BUSINESS-RULES.md#br-14): Quy tắc định lượng nguyên liệu số học và tổng hợp Shopping List an toàn.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post (1–50 nguyên liệu có định lượng số học và tỷ lệ quy đổi).
  - [BR-48](BUSINESS-RULES.md#br-48): Xử lý nguyên liệu thiếu số liệu dinh dưỡng và chặn công khai khi thiếu quy đổi đơn vị.
  - [BR-50](BUSINESS-RULES.md#br-50): Cho phép công khai bài thiếu dữ liệu dinh dưỡng.
  - [BR-73](BUSINESS-RULES.md#br-73): Chuẩn hóa đơn vị đo lường và chặn công khai khi thiếu quy đổi.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến (chống XSS trong tên nguyên liệu).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-19.1 (Chỉ chọn nguyên liệu có trong danh mục chuẩn):**
  - *Given* Chuyên gia đang tạo hoặc sửa Recipe Post,
  - *When* tìm kiếm nguyên liệu không có trong danh mục chuẩn,
  - *Then* hệ thống không cho thêm/lưu nguyên liệu bằng tên tự do, không tạo luồng đề xuất và hướng dẫn liên hệ Admin để quản lý catalog.
- **AC-19.2 (Bắt buộc định lượng số học dương và từ chối 'vừa đủ'):**
  - *Given* tác giả nhập dòng nguyên liệu "Muối tiêu",
  - *When* tác giả cố tình nhập chuỗi "vừa đủ" hoặc nhập số lượng $\le 0$,
  - *Then* hệ thống từ chối lưu và yêu cầu nhập số lượng số học dương (`quantity > 0`) kèm đơn vị đo hợp lệ (BR-14).
- **AC-19.3 (Ràng buộc số dòng nguyên liệu từ 1 đến 50 dòng):**
  - *Given* tác giả tạo bài viết có 0 dòng nguyên liệu hoặc có 51 dòng nguyên liệu,
  - *When* tác giả gửi yêu cầu lưu bài viết,
  - *Then* hệ thống từ chối lưu và hiển thị thông báo số lượng nguyên liệu phải từ 1 đến 50 dòng.
- **AC-19.4 (Bắt buộc lưu tham chiếu và tên catalog chuẩn):**
  - *Given* Chuyên gia chọn nguyên liệu "Đậu phụ" trong danh mục,
  - *When* bài viết được lưu thành công,
  - *Then* dòng nguyên liệu lưu tham chiếu tới đúng mục catalog và hiển thị tên chuẩn "Đậu phụ"; không thể thay bằng tên tự nhập hoặc lưu thiếu tham chiếu.
- **AC-19.5 (Chặn xuất bản khi đơn vị thiếu tỷ lệ quy đổi sang gam):**
  - *Given* nguyên liệu chọn đơn vị đếm "quả" hoặc "muỗng" nhưng nguyên liệu đó chưa có tỷ lệ quy đổi sang gam trong `INGREDIENT_UNIT_CONVERSION`,
  - *When* tác giả nhấn công khai bài viết,
  - *Then* hệ thống từ chối xuất bản và hiển thị lỗi validation chặn xuất bản theo BR-19 và BR-73.
- **AC-19.6 (Lọc đơn vị theo nguyên liệu và giữ số lượng):** Khi chọn nguyên liệu, hệ thống chỉ liệt kê `mg/g/kg` cùng các đơn vị volume/count có conversion active của nguyên liệu đó. Đổi nguyên liệu giữ unit nếu còn hỗ trợ, nếu không thì chọn `g`; số lượng người dùng nhập được giữ nguyên.
- **AC-19.7 (Bài cũ và dữ liệu AI):** Bài cũ dùng đơn vị legacy vẫn hiển thị tên và lượng. Khi sửa, tác giả phải chọn unit mới trước khi lưu. Đơn vị AI không hỗ trợ phải được báo rõ và không tự động thay thế.

---

---

<a id="fr-20"></a>
### FR-20 — Thống nhất nguồn hiển thị thẻ món, chi tiết và thực đơn

- **Mã yêu cầu:** FR-20
- **Module:** M01, M04, M05
- **Điểm tích hợp AI Chatbot:** Trang chi tiết công thức (Recipe Detail) có thể cung cấp Recipe Post hiện tại làm context cho AI Chatbot [FR-51](FUNCTIONAL-REQUIREMENTS.md#fr-51) khi người dùng chủ động chọn chức năng hỏi AI về công thức đang xem. Toàn bộ luồng tương tác hội thoại, phân quyền và trừ hạn ngạch thuộc phạm vi sở hữu của FR-51 (áp dụng cho cả Member và Guest theo [FR-02](FUNCTIONAL-REQUIREMENTS.md#fr-02)).

- **Use Case detail:** [Open interaction flows](../use-cases/discovery-and-public-content.md#fr-20).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Áp dụng kiến trúc nguồn dữ liệu duy nhất (Single Source of Truth - SSOT) cho toàn bộ các bài công thức nấu ăn trong hệ thống: trang chi tiết bài công thức (Recipe Detail View) và thẻ món ăn (Recipe Card theo FR-17) được kết xuất trực tiếp từ cùng một bản thể Recipe Post duy nhất; các phân hệ Lịch ăn tuần (Meal Plan - FR-09, FR-33) và Danh sách mua sắm (Shopping List - FR-53) tham chiếu trực tiếp đến bản thể này bằng khóa ngoại; hệ thống TUYỆT ĐỐI KHÔNG sao chép (duplicate) dữ liệu bài viết hay tạo các bài nguồn/website riêng biệt; khi bài viết được chỉnh sửa, toàn bộ các điểm hiển thị đều được cập nhật nhất quán; khi bài viết bị xóa hoặc ẩn, các tham chiếu lịch sử được giữ an toàn dưới dạng bản ghi không khả dụng (Tombstone) (BR-35, BR-64).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Toàn bộ Recipe Post công khai trong hệ thống.
  - Điểm kết nối dữ liệu: Thẻ món Khám phá (FR-17), Trang chi tiết công thức, Danh sách công thức đã lưu (FR-32), Lịch ăn tuần 3 bữa (FR-09, FR-33), Danh sách mua sắm (FR-53).
  - Không áp dụng: Hệ thống không tạo bài viết ảo hay nhân bản dữ liệu để phục vụ menu.
- **Phân loại Actor:**
  - Primary Actor: `Guest`, `Member` (người xem nội dung), `Member` (tác giả chỉnh sửa/xóa bài).
  - Supporting Actor: `Hệ thống quản lý dữ liệu hợp nhất (Data SSOT Subsystem)`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-20.1`: Xem trang chi tiết đầy đủ của bài công thức được kết xuất từ nguồn dữ liệu duy nhất.
  - `UC-20.2`: Xem các tham chiếu công thức trong Lịch ăn và Danh sách đã lưu tự động cập nhật đồng bộ khi bài nguồn thay đổi.
  - `UC-20.3`: Xem hiển thị trạng thái Tombstone an toàn trong Lịch ăn khi bài công thức nguồn bị xóa hoặc tạm ẩn.
  - `UC-20.4`: Xuất bản in/tệp tin công thức chi tiết dạng PDF hoặc TXT (Export recipe detail).
- **User Stories:**
  - *Là một người sử dụng hệ thống*, tôi muốn khi tác giả cập nhật công thức nấu ăn thì các món tôi đã lưu trong lịch ăn cũng tự động cập nhật theo, và nếu bài viết có bị xóa thì lịch ăn của tôi vẫn còn ghi nhớ tên món chứ không bị mất trắng.
  - *Là một người nội trợ*, tôi muốn có nút xuất công thức ra tệp PDF hoặc TXT để dễ dàng in ấn mang vào bếp hoặc lưu trữ ngoại tuyến khi nấu ăn.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Mọi người dùng đều có quyền xem chi tiết công thức công khai và xuất tệp PDF/TXT. Member được bình chọn Like/Dislike (BR-69), Guest chỉ xem tỷ lệ %.
- **Ràng buộc kiến trúc:** Single Source of Truth; không tạo bản sao độc lập; áp dụng Tombstone pattern (BR-35, BR-64).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của mô tả giới thiệu và ảnh đại diện.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn.
  - [BR-33](BUSINESS-RULES.md#br-33): Thao tác lưu công thức không gọi Gemini.
  - [BR-35](BUSINESS-RULES.md#br-35): Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn (Tombstone).
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
  - [BR-69](BUSINESS-RULES.md#br-69): Tương tác Like / Dislike và tỷ lệ hài lòng (% Like) của bài công thức.
  - [BR-70](BUSINESS-RULES.md#br-70): Ghi nhận lượt xem và khử trùng lặp theo phiên (`RECIPE_VIEW`).
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian tải trang hiển thị chi tiết bài viết $\le 2$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-20.1 (Hiển thị trang chi tiết công thức từ nguồn dữ liệu duy nhất):**
  - *Given* bài công thức công khai tồn tại trong hệ thống,
  - *When* người dùng mở xem trang chi tiết,
  - *Then* hệ thống kết xuất đầy đủ thông tin chuẩn (tiêu đề, nguyên liệu định lượng số, khẩu phần, thời gian, dinh dưỡng, thể loại món `dish_category`, hướng dẫn chế biến `instructions`, thư viện ảnh `RECIPE_MEDIA`, video YouTube nhúng, tỷ lệ % Like và số lượt bình chọn `RECIPE_REACTION`, lượt xem `RECIPE_VIEW`) từ bản thể dữ liệu duy nhất.
- **AC-20.2 (Đồng bộ tức thì khi tác giả chỉnh sửa bài viết):**
  - *Given* tác giả vừa cập nhật thành công tiêu đề, hướng dẫn chế biến hoặc nguyên liệu của bài công thức,
  - *When* một người dùng khác mở xem bài viết đó hoặc xem trong Lịch ăn tuần,
  - *Then* thông tin mới cập nhật được phản ánh chính xác ngay lập tức mà không cần đồng bộ thủ công.
- **AC-20.3 (Bảo toàn Tombstone trong Lịch ăn khi bài nguồn bị xóa):**
  - *Given* một bài công thức đang được lưu trong Lịch ăn của Member B,
  - *When* tác giả Member A thực hiện xóa bài công thức đó,
  - *Then* ô tương ứng trong Lịch ăn của Member B hiển thị trạng thái "Công thức không còn khả dụng" và không gây lỗi sập trang.
- **AC-20.4 (Xuất bản tệp tin công thức chi tiết sang PDF / TXT):**
  - *Given* người dùng đang xem trang chi tiết bài công thức hợp lệ,
  - *When* người dùng nhấn nút "Xuất công thức" và chọn định dạng PDF hoặc TXT,
  - *Then* hệ thống tạo và tải xuống tệp tin chứa đầy đủ tiêu đề, tác giả, thể loại món, khẩu phần, định lượng nguyên liệu và hướng dẫn chế biến với định dạng chuẩn trang in (A4 đối với PDF).
- **AC-20.5 (Thời gian tải trang chi tiết đáp ứng chuẩn NFR):**
  - *Given* bài công thức có ảnh đại diện và nội dung hướng dẫn chế biến dài 5.000 ký tự,
  - *When* người dùng truy cập trang chi tiết,
  - *Then* toàn bộ nội dung được tải và hiển thị hoàn tất trong thời gian không vượt quá 2 giây.

---

---

<a id="fr-21"></a>
### FR-21 — AI hỗ trợ tạo giới thiệu hoặc hướng dẫn chế biến không lưu nháp server

- **Mã yêu cầu:** FR-21
- **Module:** M03, M06

- **Use Case detail:** [Open interaction flows](../use-cases/recipe-contribution-and-community.md#fr-21).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp tính năng trợ lý AI tương tác thông minh hỗ trợ Chuyên gia trong quá trình tạo hoặc chỉnh sửa bài công thức nấu ăn: AI hỗ trợ gợi ý đoạn văn bản giới thiệu món ăn hấp dẫn (Description $\le 2.000$ ký tự) hoặc đề xuất nội dung hướng dẫn chế biến (văn bản từ 10 đến 5.000 ký tự theo FR-16, BR-19) dựa trên thông tin tác giả đã cung cấp (tên món, loại ăn chay, thể loại món `dish_category`, danh sách nguyên liệu và khẩu phần); AI tuyệt đối không tự ý thêm bất kỳ nguyên liệu mới nào ngoài danh sách tác giả đã nhập; kết quả do AI sinh ra được đưa trực tiếp vào các ô nhập liệu của biểu mẫu ở phía client (ô mô tả `description` hoặc ô hướng dẫn chế biến `instructions`) ở dạng có thể chỉnh sửa tự do và chỉ được công khai khi người dùng chủ động xem xét, xác nhận (BR-15); hệ thống không tự động công khai hoặc lưu persistent server-side Recipe Post draft trong baseline hiện tại; tính năng thuộc gói Pro (FR-10, BR-02); nếu AI gặp sự cố kỹ thuật hoặc tài khoản không thuộc gói Pro, tác giả vẫn có toàn quyền tiếp tục tự viết hướng dẫn chế biến và xuất bản bài viết bình thường (BR-16).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Chuyên gia đã đăng nhập và đang đăng ký gói Pro còn hiệu lực (FR-03, FR-10, BR-02).
  - Khả năng hỗ trợ:
    1. Gợi ý đoạn giới thiệu món ăn (Description $\le 2.000$ ký tự).
    2. Đề xuất nội dung hướng dẫn chế biến linh hoạt (`instructions`, từ 10 đến 5.000 ký tự).
  - Không hỗ trợ: AI không tự thêm nguyên liệu, không tự động công khai và không lưu persistent server-side Recipe Post draft trong baseline hiện tại.
- **Phân loại Actor:**
  - Primary Actor: `Chuyên gia (Role = EXPERT)` (tác giả bài viết).
  - Supporting Actor: `Google Gemini AI` (trợ lý sinh nội dung), `Hệ thống xác thực quyền tính năng (Feature Entitlement)`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-21.1`: Yêu cầu AI gợi ý đoạn văn bản giới thiệu món ăn dựa trên nguyên liệu và tên món đã nhập.
  - `UC-21.2`: Yêu cầu AI đề xuất nội dung hướng dẫn chế biến linh hoạt (10–5.000 ký tự) dựa trên danh sách nguyên liệu.
- **User Stories:**
  - *Là một Chuyên gia ẩm thực*, tôi muốn nhờ AI gợi ý giúp một đoạn mô tả món ăn hấp dẫn và hướng dẫn chế biến rõ ràng từ những nguyên liệu tôi đã chọn, để tôi có thể chỉnh sửa lại cho đúng bí quyết của mình và chủ động xác nhận đăng bài nhanh chóng hơn.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chỉ Chuyên gia đã đăng nhập và đang sử dụng gói Pro còn hiệu lực mới được sử dụng.
- **Ràng buộc an toàn:** AI không tự thêm nguyên liệu; kết quả AI phải ở dạng chỉnh sửa được; AI không tự ý công khai và không lưu persistent server-side Recipe Post draft trong baseline hiện tại; không chặn đăng bài khi AI lỗi (BR-16).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Quyền sử dụng AI Chat cho Guest và Member Free.
  - [BR-02](BUSINESS-RULES.md#br-02): Phân quyền tính năng AI cho gói Plus và Pro.
  - [BR-03](BUSINESS-RULES.md#br-03): Xác thực quyền tính năng trước khi gọi dịch vụ AI.
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider, timeout AI và lưu trữ telemetry.
  - [BR-06](BUSINESS-RULES.md#br-06): Bảo mật API Key của AI Provider.
  - [BR-07](BUSINESS-RULES.md#br-07): Quyền đăng và công khai Recipe Post dành riêng cho Chuyên gia (Role = EXPERT).
  - [BR-15](BUSINESS-RULES.md#br-15): Đồng nhất chính sách bài viết tự soạn và bài có AI hỗ trợ.
  - [BR-16](BUSINESS-RULES.md#br-16): Không chặn công khai bài viết khi AI lỗi hoặc gói không hỗ trợ tính năng AI.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post (hướng dẫn chế biến linh hoạt, 0..5 ảnh với 1 ảnh bìa, tỷ lệ quy đổi đơn vị).
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03): Thời gian phản hồi của tính năng AI Chatbot.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-21.1 (AI sinh nội dung không tự ý thêm nguyên liệu mới):**
  - *Given* tác giả khai báo danh sách nguyên liệu chỉ gồm "Đậu phụ, Cà chua, Hành boa-rô",
  - *When* AI sinh nội dung hướng dẫn chế biến,
  - *Then* toàn bộ nội dung hướng dẫn chỉ sử dụng các nguyên liệu đã khai báo và tuyệt đối không xuất hiện nguyên liệu mới (như nấm, ớt chuông...).
- **AC-21.2 (Đưa nội dung AI sinh vào form soạn thảo dưới dạng có thể chỉnh sửa):**
  - *Given* AI phản hồi thành công nội dung hướng dẫn chế biến (10–5.000 ký tự),
  - *When* dữ liệu trả về client,
  - *Then* nội dung được tự động điền vào ô nhập liệu `instructions` trên form để tác giả có thể chỉnh sửa trực tiếp và chỉ được công khai khi tác giả chủ động bấm đăng.
- **AC-21.3 (Tuyệt đối không lưu trữ bản nháp trên server):**
  - *Given* tác giả sử dụng AI hỗ trợ soạn bài nhưng chưa nhấn nút "Đăng công thức",
  - *When* kiểm tra cơ sở dữ liệu hệ thống trên máy chủ,
  - *Then* không có bất kỳ bản ghi bài viết nháp nào được lưu trên server.
- **AC-21.4 (Không chặn xuất bản bài viết khi AI lỗi hoặc tài khoản gói Free hoặc Plus):**
  - *Given* tài khoản thuộc gói Free hoặc dịch vụ AI gặp sự cố,
  - *When* tác giả tự gõ hướng dẫn chế biến vào biểu mẫu và nhấn đăng bài đạt chuẩn,
  - *Then* hệ thống công khai bài viết thành công mà không có bất kỳ trở ngại nào (BR-16).
- **AC-21.5 (Xác thực quyền gói Pro trước khi gọi AI):**
  - *Given* Chuyên gia thuộc gói Pro còn hiệu lực,
  - *When* tác giả nhấn yêu cầu AI hỗ trợ soạn bài,
  - *Then* hệ thống xác thực quyền tính năng thành công và chuyển ngữ cảnh yêu cầu tới dịch vụ Google Gemini; đối với tài khoản gói Free, hệ thống chặn gọi AI và hướng dẫn nâng cấp gói (FR-10, BR-02, BR-03).

---

---

<a id="fr-23"></a>
### FR-23 — Hiển thị thông tin tác giả gắn liền với tài khoản

- **Mã yêu cầu:** FR-23
- **Module:** M01, M02, M03
- **Tóm tắt yêu cầu:** Hệ thống định danh tác giả gắn chặt với tài khoản đã đăng ký; hiển thị thông tin tác giả trên đầu bài công thức và trang hồ sơ công khai gồm tên, avatar, giới thiệu ngắn, bài đăng, ngày tham gia và tổng lượt thích/lượt xem của toàn bộ bài công khai; phân định nghiêm ngặt quyền xem dữ liệu công khai và dữ liệu riêng tư; bảo vệ quyền sở hữu nội dung và ngăn chặn lộ lọt thông tin nhạy cảm.

- **Use Case detail:** [Open interaction flows](../use-cases/discovery-and-public-content.md#fr-23).

#### Mục đích
Xác lập mối liên kết định danh bất biến giữa nội dung bài công thức (`Recipe Post`) với tài khoản thành viên (`Member`) đã tạo ra bài viết; cung cấp không gian hồ sơ công khai để cộng đồng nhận diện tác giả và các bài công thức do tác giả chia sẻ; đồng thời thiết lập ranh giới bảo mật nghiêm ngặt nhằm bảo vệ thông tin riêng tư (email, hồ sơ ăn uống, dinh dưỡng, lịch ăn) của người dùng.

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-23.1`: Hiển thị thông tin tác giả trên bài công thức (Display author card on Recipe Post).
  - `UC-23.2`: Xem trang hồ sơ công khai của thành viên (View public user profile).
  - `UC-23.3`: Xem và chỉnh sửa hồ sơ cá nhân của chính mình (View and edit own profile).
  *(Ghi chú: UC-23.4 trước đây về kiểm soát ranh giới dữ liệu riêng tư không còn là Use Case độc lập vì đây là cơ chế an ninh hệ thống chứ không phải mục tiêu của Actor; hành vi này được chuẩn hóa thành Quy tắc phân quyền & bảo mật, Luồng an ninh/ngoại lệ, và Tiêu chí nghiệm thu).*
- **User Stories:**
  - `US-23.1`: Là một người đọc, tôi muốn thấy tên, ảnh đại diện và liên kết hồ sơ của tác giả ở đầu bài công thức để biết ai là người sáng tạo món ăn.
  - `US-23.2`: Là một người đọc, tôi muốn xem trang hồ sơ công khai của tác giả để biết ngày tham gia, giới thiệu ngắn và danh sách các món ăn khác của tác giả.
  - `US-23.3`: Là một thành viên, tôi muốn cập nhật tên hiển thị, ảnh đại diện và phần giới thiệu ngắn để cá nhân hóa hình ảnh của mình trong cộng đồng.
  - `US-23.4`: Là một thành viên, tôi muốn địa chỉ email, thông tin xác thực, hồ sơ dị ứng/ăn chay riêng tư và dữ liệu dinh dưỡng/lịch ăn của tôi được bảo mật tuyệt đối, không bị lộ ra ngoài trang công khai.

#### Ranh giới dữ liệu công khai và dữ liệu riêng tư (Data Visibility Boundary)

| Nhóm dữ liệu | Phạm vi hiển thị | Danh mục trường dữ liệu | Ràng buộc bảo mật & hiển thị |
|---|---|---|---|
| **Hồ sơ công khai (Public Profile)** | Công khai cho toàn bộ người dùng (Guest, Member, Admin) | - Tên hiển thị (3–50 ký tự)<br>- Ảnh đại diện (avatar)<br>- Giới thiệu ngắn (bio, tối đa 500 ký tự)<br>- Thời điểm tham gia (định dạng Tháng/Năm)<br>- Danh sách Recipe Post đang công khai | - Nếu chưa có avatar: hiển thị ảnh mặc định hệ thống.<br>- Nếu bio trống: cho phép để trống/ẩn.<br>- Tuyệt đối KHÔNG gán nhãn "Chuyên gia", "Bác sĩ" hoặc "Đã xác minh danh tính ngoài đời" (BR-18).<br>- Không hiển thị bài viết đang bị ẩn do vi phạm. |
| **Dữ liệu tài khoản & hồ sơ riêng tư (Private Account & Profile Data)** | Mặc định độc quyền chủ sở hữu (`Owner-only by default`); Administrator chỉ được tiếp cận các thông tin định danh và quản trị tối thiểu được BR/NFR cho phép rõ ràng | - Địa chỉ email đăng ký<br>- Trạng thái xác minh email<br>- Trạng thái tài khoản (`ACTIVE`, `LOCKED`)<br>- Hồ sơ sở thích ăn chay (FR-31)<br>- Hồ sơ dinh dưỡng & chỉ số sức khỏe (FR-35, FR-38)<br>- Công thức đã lưu (FR-32)<br>- Lịch ăn tuần (FR-09, FR-33)<br>- Danh sách mua sắm (FR-53)<br>- Báo cáo vi phạm đã gửi (FR-26) | - Máy chủ bắt buộc kiểm tra quyền sở hữu (`Ownership Check`): Dữ liệu cá nhân hóa (hồ sơ dinh dưỡng, chỉ số sức khỏe/BMI, sở thích ăn uống, công thức đã lưu, lịch ăn, danh sách mua sắm) thuộc quyền ĐỘC QUYỀN của chính chủ tài khoản (`Resource Owner`).<br>- Administrator TUYỆT ĐỐI KHÔNG tự động có quyền truy cập dữ liệu cá nhân hóa (nghiêm cấm quy tắc generic `isOwner || isAdmin`).<br>- Quyền của Administrator là riêng biệt theo từng tài nguyên (resource-specific) và giới hạn ở mức tối thiểu cần thiết để thực thi nhiệm vụ quản trị (account ID, email quản trị, trạng thái tài khoản, lịch sử kiểm duyệt/báo cáo) theo BR/NFR.<br>- Tuyệt đối không trả các trường riêng tư trong giao diện hay dữ liệu public profile. |
| **Dữ liệu an ninh / Thông tin định danh nhạy cảm nội bộ (Security / Credential Internals)** | Xử lý nội bộ độc quyền bởi các thành phần an ninh/xác thực tin cậy của hệ thống (System Security Components Only) | - Mật khẩu / Password hash<br>- Password reset token<br>- Email verification token<br>- Authentication secrets / API keys | - **Tuyệt đối KHÔNG BAO GIỜ** để lộ qua Public Profile.<br>- **Tuyệt đối KHÔNG BAO GIỜ** trả về qua Member UI/API thông thường.<br>- **Tuyệt đối KHÔNG BAO GIỜ** trả về qua Administrator UI/API thông thường.<br>- Administrator không được phép truy cập hoặc xem các bí mật xác thực này chỉ vì có vai trò Admin (tuân thủ nguyên tắc đặc quyền tối thiểu - Least Privilege).<br>- Chỉ được xử lý bởi các thành phần an ninh/xác thực tin cậy của hệ thống khi có yêu cầu kỹ thuật hợp lệ. |

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Mọi người dùng (Guest, Member, Admin) đều có quyền xem thông tin công khai của tác giả và trang hồ sơ công khai của thành viên.
- Quy tắc phân quyền tài nguyên riêng tư: Mặc định là ĐỘC QUYỀN CHỦ SỞ HỮU (`Owner-only by default`). Chỉ chính chủ sở hữu tài khoản (`Resource Owner`) mới có quyền xem, truy xuất và chỉnh sửa dữ liệu cá nhân hóa của mình (hồ sơ dinh dưỡng, chỉ số BMI/sức khỏe, sở thích ăn uống, công thức đã lưu, lịch ăn tuần, danh sách mua sắm).
- Administrator tuyệt đối KHÔNG tự động được cấp quyền truy cập vào các dữ liệu cá nhân hóa trên (nghiêm cấm sử dụng logic phân quyền generic `isOwner || isAdmin`).
- Quyền của Administrator là quyền hạn riêng biệt theo từng tài nguyên (resource-specific) và phải được quy định rõ ràng trong các BR/NFR: Administrator chỉ được tiếp cận các thông tin quản trị tối thiểu phục vụ nhiệm vụ (account ID, email quản trị, trạng thái tài khoản, thông tin báo cáo/kiểm duyệt vi phạm).
- Tách biệt rõ ràng ranh giới Security / Credential Internals (mật khẩu băm, token bí mật): các thông tin này tuyệt đối không trả về qua bất kỳ UI/API người dùng hay quản trị viên nào, tuân thủ nguyên tắc đặc quyền tối thiểu (Least Privilege).
- Máy chủ bắt buộc thực thi kiểm tra quyền sở hữu (Ownership Check) ở tầng nghiệp vụ máy chủ trước khi truy xuất hoặc thay đổi bất kỳ tài nguyên cá nhân nào; việc ẩn nút trên giao diện máy khách không thay thế được kiểm tra bảo mật phía máy chủ (NFR-08, NFR-09).
- Không cấp nhãn chuyên gia, bác sĩ dinh dưỡng hoặc cam kết xác minh danh tính thật trên hồ sơ công khai (BR-18).
- Ràng buộc kỹ thuật được phê duyệt: Lưu trữ ảnh đại diện trên dịch vụ Azure Blob Storage; bảo vệ kênh truyền qua HTTPS/TLS 1.2+ (NFR-08).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản đăng bài (không được chọn tài khoản khác, AI không thay thế tác giả).
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai (không lộ email, thông tin đăng nhập, hồ sơ ăn uống/sức khỏe riêng tư, không nhãn chuyên gia).
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân & chỉ số sức khỏe, HTTPS/TLS 1.2+, kiểm tra quyền sở hữu ở Backend.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn truy cập trái quyền.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt, tương thích đa độ phân giải.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-23.1 — Hiển thị đầy đủ thông tin tác giả trên trang bài công thức:**
  - **Given:** Một Recipe Post hợp lệ đang ở trạng thái công khai được tạo bởi tác giả `NguyenVanA`.
  - **When:** Người dùng (Guest hoặc Member) truy cập vào trang chi tiết bài công thức đó.
  - **Then:** Đầu trang hiển thị rõ ràng tên hiển thị `NguyenVanA`, ảnh đại diện (hoặc ảnh mặc định), ngày đăng bài, và liên kết dẫn tới trang hồ sơ công khai của tác giả.

- **AC-23.2 — Xem trang hồ sơ công khai chỉ chứa thông tin được phép:**
  - **Given:** Người dùng truy cập trang hồ sơ công khai của tác giả `NguyenVanA`.
  - **When:** Trang hồ sơ tải hoàn tất.
  - **Then:** Giao diện hiển thị: tên hiển thị, ảnh đại diện, giới thiệu ngắn (nếu có), thời điểm tham gia (Tháng/Năm), danh sách các bài công thức đang công khai của tác giả, cùng tổng số lượt thích và lượt xem của tất cả bài công khai bất kể phân trang; tuyệt đối KHÔNG có email, mật khẩu/password hash, token xác thực, hồ sơ dị ứng, dữ liệu dinh dưỡng, lịch ăn hay danh sách đã lưu.

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

---

<a id="fr-25"></a>
### FR-25 — Công khai Recipe Post ngay sau khi validation đạt

- **Mã yêu cầu:** FR-25
- **Module:** M02 (Identity & Access), M03 (Community Recipes & Social)
- **Tóm tắt yêu cầu:** Khi Chuyên gia đã đăng nhập nhấn "Công khai bài viết" và toàn bộ các điều kiện kiểm tra tính hợp lệ (validation rules theo SRS 3.9), gồm tham chiếu catalog bắt buộc cho mọi dòng nguyên liệu theo FR-19/BR-12, đều thỏa mãn, hệ thống lập tức chuyển trạng thái bài viết thành công khai (`PUBLISHED`) và hiển thị ngay trên toàn hệ thống; không áp dụng luồng duyệt trước từng bài hay trạng thái Pending Review ở cấp bài viết (BR-07).

- **Use Case detail:** [Open interaction flows](../use-cases/identity-and-access.md#fr-25).

#### Mục đích
Triển khai cơ chế xuất bản bài viết công khai trực tiếp (Direct Publishing); giảm thiểu tối đa độ trễ trong việc chia sẻ công thức; tạo sự hứng khởi cho người dùng tham gia đóng góp nội dung; đồng thời phân định ranh giới rõ ràng giữa việc xuất bản nội dung và việc kiểm soát vi phạm qua cơ chế hậu kiểm (BR-07, FR-06).

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-25.1`: Xuất bản trực tiếp bài công thức sau khi kiểm tra hợp lệ (Publish recipe post directly).
- **User Stories:**
  - `US-25.1`: Là một Chuyên gia ẩm thực chay vừa hoàn thiện công thức, tôi muốn bài viết của mình xuất hiện ngay trên trang khám phá để cộng đồng có thể xem ngay mà không phải chờ Quản trị viên duyệt thủ công từng bài.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Chỉ Chuyên gia đã xác thực mới có quyền xuất bản bài công thức (BR-05, BR-07, NFR-09).
- Hệ thống tuyệt đối không cho phép client gửi trường `status` tùy ý; trạng thái `PUBLISHED` chỉ do máy chủ thiết lập sau khi toàn bộ điều kiện kiểm tra đã vượt qua thành công.

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Quyền đăng và công khai Recipe Post dành riêng cho Chuyên gia (Role = EXPERT).
  - [BR-14](BUSINESS-RULES.md#br-14): Quy tắc định lượng và tổng hợp Shopping List an toàn.
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản đăng bài.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post (hướng dẫn chế biến linh hoạt, 0..5 ảnh với 1 ảnh bìa, tỷ lệ quy đổi đơn vị).
  - [BR-20](BUSINESS-RULES.md#br-20): Tính tùy chọn của mô tả giới thiệu và ảnh đại diện.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
  - [BR-73](BUSINESS-RULES.md#br-73): Quy tắc chuẩn hóa đơn vị đo và chuyển đổi đơn vị nguyên liệu.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và toàn vẹn giao dịch.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-25.1 — Công khai bài viết thành công ngay lập tức khi validation đạt:**
  - **Given:** Chuyên gia đã nhập đầy đủ và chính xác toàn bộ các trường bắt buộc của bài viết thỏa mãn Recipe Validation Profile của FR-16 (bao gồm thể loại món `dish_category`, hướng dẫn chế biến `instructions` 10–5.000 ký tự, nguyên liệu có số lượng số dương và đơn vị chuẩn có quy tắc quy đổi khả dụng, 0–5 ảnh với đúng 1 ảnh bìa nếu có ảnh).
  - **When:** Chuyên gia nhấn "Công khai bài viết".
  - **Then:** Hệ thống chuyển trạng thái bài viết thành `PUBLISHED` ngay lập tức mà không cần phê duyệt trước (BR-07), gán đúng tác giả (BR-17) và chuyển hướng tới trang chi tiết bài viết.

- **AC-25.2 — Bài viết vừa công khai xuất hiện ngay trong kết quả tìm kiếm:**
  - **Given:** Bài công thức vừa được xuất bản thành công với tiêu đề "Nấm đùi gà kho tiêu".
  - **When:** Người dùng khác tìm kiếm từ khóa "Nấm đùi gà kho tiêu".
  - **Then:** Bài viết xuất hiện ngay trong kết quả tìm kiếm công khai.

- **AC-25.3 — Chặn xuất bản và hiển thị lỗi cụ thể khi có trường vi phạm validation:**
  - **Given:** Member đang ở biểu mẫu công thức nhưng bỏ trống danh sách nguyên liệu hoặc không có bước chế biến nào.
  - **When:** Member nhấn "Công khai bài viết".
  - **Then:** Hệ thống từ chối xuất bản, giữ nguyên dữ liệu đã nhập và hiển thị thông báo lỗi yêu cầu hoàn thiện trường vi phạm.

- **AC-25.4 — Gán quyền tác giả bất biến từ phiên đăng nhập khi công khai bài:**
  - **Given:** Member `UserA` đang đăng nhập với phiên xác thực hợp lệ.
  - **When:** `UserA` công khai một bài công thức mới.
  - **Then:** Bài viết được gán cố định tác giả là `UserA`; không có bất kỳ cách nào để gán sang tài khoản khác từ client (BR-17).

- **AC-25.5 — Chặn xuất bản khi nguyên liệu thiếu quy tắc quy đổi đơn vị cần thiết:**
  - **Given:** Tác giả sử dụng nguyên liệu "Bí đỏ" với đơn vị "miếng" (thuộc nhóm cần quy đổi sang gram để tính dinh dưỡng) nhưng hệ thống chưa có quy tắc chuyển đổi trong `INGREDIENT_UNIT_CONVERSION`,
  - **When:** Tác giả nhấn "Công khai bài viết",
  - **Then:** Hệ thống từ chối xuất bản và hiển thị thông báo lỗi validation: *"Nguyên liệu 'Bí đỏ' với đơn vị 'miếng' chưa có dữ liệu quy đổi trọng lượng. Vui lòng chọn đơn vị đo chuẩn (gram/kg) để xuất bản"*.

- **AC-25.6 — Kiểm tra cấu hình ảnh đại diện duy nhất khi có ảnh minh họa:**
  - **Given:** Tác giả tải lên 3 ảnh minh họa vào bài viết nhưng không chọn ảnh nào làm ảnh đại diện (`is_cover`),
  - **When:** Tác giả nhấn "Công khai bài viết",
  - **Then:** Hệ thống từ chối xuất bản và hiển thị thông báo yêu cầu chỉ định chính xác 1 ảnh đại diện cho bài viết (FR-14, BR-19).

---

---

<a id="fr-26"></a>
### FR-26 — Gửi báo cáo bài công thức có vấn đề

- **Mã yêu cầu:** FR-26
- **Module:** M02 (Identity & Access), M03 (Community Recipes & Social), M09 (Administration & Moderation)
- **Tóm tắt yêu cầu:** Hệ thống cho phép tài khoản đã đăng nhập gửi báo cáo bài công thức có vấn đề; Guest được yêu cầu đăng nhập khi bấm "Báo cáo" (BR-24); Administrator tiếp nhận, xem xét và xử lý báo cáo theo quy trình hậu kiểm; ngăn chặn cùng một tài khoản tạo báo cáo mở trùng lặp trên cùng một bài viết (FR-30).

- **Use Case detail:** [Open interaction flows](../use-cases/identity-and-access.md#fr-26).

#### Mục đích
Thiết lập cơ chế giám sát cộng đồng (crowdsourced moderation) nhằm phát hiện kịp thời các nội dung bài viết vi phạm tiêu chuẩn món chay, gây nguy hiểm an toàn thực phẩm, phản cảm hoặc vi phạm bản quyền; cung cấp thông tin đầu vào chuẩn xác phục vụ quy trình xử lý hậu kiểm của Quản trị viên (FR-06, BR-26).

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-26.1`: Gửi báo cáo bài công thức có vấn đề (Report inappropriate recipe post).
- **User Stories:**
  - `US-26.1`: Là một thành viên, khi tôi thấy một bài viết ghi "Ăn chay" nhưng lại hướng dẫn dùng nước mắm cá hoặc mỡ lợn, tôi muốn gửi báo cáo để Ban quản trị xử lý nhằm bảo vệ người ăn chay khác.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Bắt buộc xác thực tài khoản Member khi gửi báo cáo; tuyệt đối không chấp nhận báo cáo ẩn danh từ Guest nhằm chống quấy rối hoặc spam giả mạo (BR-24, NFR-09).
- Danh tính của người báo cáo được bảo mật nghiêm ngặt và không bao giờ tiết lộ cho tác giả bài viết bị báo cáo (FR-29, BR-18).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
  - [BR-26](BUSINESS-RULES.md#br-26): Các hình thức chế tài khi xử lý vi phạm.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân & truyền tải an toàn.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-26.1 — Member gửi báo cáo bài công thức thành công:**
  - **Given:** Member đang đăng nhập và chưa từng có báo cáo đang mở trên bài viết A.
  - **When:** Member chọn lý do báo cáo hợp lệ và nhấn "Gửi báo cáo".
  - **Then:** Hệ thống lưu báo cáo ở trạng thái `OPEN` và phản hồi thông báo đã tiếp nhận thành công.

- **AC-26.2 — Ngăn chặn tạo báo cáo mới trùng lặp từ cùng tài khoản (FR-30):**
  - **Given:** Member đã có một báo cáo ở trạng thái `OPEN` hoặc `IN_REVIEW` đối với bài viết A.
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

---

<a id="fr-27"></a>
### FR-27 — Biểu mẫu báo cáo bài công thức theo 6 nhóm lý do

- **Mã yêu cầu:** FR-27
- **Module:** M03 (Community Recipes & Social)
- **Tóm tắt yêu cầu:** Biểu mẫu báo cáo bài công thức cho phép chọn đúng một trong sáu nhóm lý do chuẩn hóa tại SRS 3.11 và nhập mô tả bổ sung; mô tả bổ sung là bắt buộc khi chọn nhóm lý do "Khác" (10–500 ký tự sau khi trim) và là tùy chọn, tối đa 500 ký tự, đối với các nhóm lý do còn lại.

- **Use Case detail:** [Open interaction flows](../use-cases/recipe-contribution-and-community.md#fr-27).

#### Mục đích
Chuẩn hóa dữ liệu đầu vào của các báo cáo vi phạm nội dung; giúp người báo cáo dễ dàng chỉ rõ bản chất vi phạm; đồng thời hỗ trợ Quản trị viên nhanh chóng phân loại, đánh giá mức độ khẩn cấp (như nguy cơ an toàn thực phẩm) và xử lý chính xác theo quy chuẩn nền tảng.

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-27.1`: Chọn lý do và gửi biểu mẫu báo cáo bài công thức (Submit recipe report form).
- **User Stories:**
  - `US-27.1`: Là một người báo cáo, tôi muốn chọn đúng lý do "Sai thông tin công thức hoặc nhãn ăn chay" từ danh sách có sẵn để Ban quản trị hiểu ngay vấn đề mà tôi không cần phải gõ giải thích dài dòng nếu không cần thiết.

#### Cấu trúc 6 nhóm lý do báo cáo chuẩn hóa (SRS 3.11)

| Nhóm lý do | Mã phân loại | Tính chất mô tả bổ sung | Mô tả chi tiết phạm vi vi phạm |
|---|---|---|---|
| **1. Sai thông tin công thức hoặc nhãn ăn chay** | `RECIPE_INFO_OR_DIET_LABEL` | Tùy chọn (Optional) | Thông tin công thức sai hoặc nhãn/chỉ dẫn chế độ ăn chay không chính xác. |
| **2. Nguy cơ an toàn thực phẩm** | `FOOD_SAFETY_HAZARD` | Tùy chọn (Optional) | Hướng dẫn kết hợp nguyên liệu gây ngộ độc, liều lượng nguy hiểm hoặc phương pháp nấu có hại cho sức khỏe. |
| **3. Spam / Quảng cáo không phù hợp** | `SPAM_ADVERTISING` | Tùy chọn (Optional) | Chèn nội dung spam, quảng cáo không phù hợp hoặc thương mại không liên quan. |
| **4. Nội dung xúc phạm / Không phù hợp** | `INAPPROPRIATE_CONTENT` | Tùy chọn (Optional) | Nội dung xúc phạm hoặc không phù hợp. |
| **5. Sao chép / Nghi vi phạm bản quyền** | `COPYRIGHT_VIOLATION` | Tùy chọn (Optional) | Sao chép công thức, hình ảnh hoặc nội dung có dấu hiệu vi phạm bản quyền. |
| **6. Khác** | `OTHER` | **Bắt buộc (Mandatory)** | Các vấn đề khác; **bắt buộc nhập từ 10 đến 500 ký tự** để giải thích rõ lý do báo cáo. |

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Làm sạch dữ liệu mô tả chống các tấn công chèn mã độc XSS (NFR-10).
- Nhóm lý do gửi lên máy chủ phải khớp chính xác với 1 trong 6 mã phân loại chuẩn hóa.

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống lỗ hổng OWASP Top 10 (XSS).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-27.1 — Hiển thị đầy đủ 6 nhóm lý do chuẩn hóa:**
  - **Given:** Member mở biểu mẫu báo cáo bài viết.
  - **When:** Biểu mẫu tải hoàn tất.
  - **Then:** Giao diện hiển thị đúng và đủ 6 nhóm lý do theo quy định tại SRS 3.11.

- **AC-27.2 — Gửi thành công báo cáo với mô tả để trống cho 5 nhóm lý do đầu:**
  - **Given:** Member chọn nhóm lý do "Sai thông tin công thức hoặc nhãn ăn chay".
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

- **AC-27.5 — Giới hạn mô tả bổ sung:**
  - **Given:** Member chọn một lý do hợp lệ.
  - **When:** Mô tả sau khi trim vượt quá 500 ký tự.
  - **Then:** Hệ thống từ chối và không tạo báo cáo; mô tả của năm lý do đầu vẫn có thể để trống.

---

---

<a id="fr-28"></a>
### FR-28 — Administrator xử lý báo cáo qua các trạng thái

- **Mã yêu cầu:** FR-28
- **Module:** M02 (Identity & Access), M03 (Community Recipes & Social), M09 (Administration & Moderation)
- **Tóm tắt yêu cầu:** Administrator quyết định thủ công khi xử lý báo cáo qua các trạng thái: Chờ xử lý (`OPEN`), Đang xem xét (`IN_REVIEW`), Đã giải quyết (`RESOLVED`); bắt buộc ghi kết luận và lý do xử lý; duy trì lịch sử kiểm toán (audit history); có thể cảnh báo tác giả, ẩn/xóa Recipe Post hoặc khóa/mở khóa tài khoản (BR-26); MVP không có ma trận chế tài số tự động.

- **Use Case detail:** [Open interaction flows](../use-cases/identity-and-access.md#fr-28).

#### Mục đích
Thiết lập quy trình nghiệp vụ rõ ràng, chuẩn hóa và có trách nhiệm giải trình cho Quản trị viên trong việc xử lý các phản ánh vi phạm; đảm bảo mọi phán quyết chế tài đều được lưu vết minh bạch, đồng thời tránh xung đột khi nhiều quản trị viên cùng thao tác xử lý nội dung.

#### Vòng đời trạng thái báo cáo vi phạm
Hệ thống quản lý trạng thái báo cáo theo luồng tuần tự:
```
[OPEN] (Chờ xử lý) ──> [IN_REVIEW] (Đang xem xét) ──> [RESOLVED] (Đã giải quyết)
```
- `OPEN` (Chờ xử lý): Báo cáo mới được gửi từ người dùng, chưa có Admin nào tiếp nhận.
- `IN_REVIEW` (Đang xem xét): Một Admin đã tiếp nhận và đang thẩm tra bài viết; giúp các Admin khác không xử lý trùng.
- `RESOLVED` (Đã giải quyết): Admin đã ra quyết định kết luận (bác bỏ hoặc áp dụng chế tài cụ thể).

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-28.1`: Xem danh sách và tiếp nhận xử lý báo cáo (Review report queue).
  - `UC-28.2`: Ra quyết định xử lý báo cáo và áp dụng chế tài (Resolve report and apply sanctions).
- **User Stories:**
  - `US-28.1`: Là một Administrator, tôi muốn chuyển báo cáo sang trạng thái "Đang xem xét" khi bắt đầu thẩm tra để đồng nghiệp biết và không thao tác trùng.
  - `US-28.2`: Là một Administrator, khi quyết định ẩn bài viết vi phạm, tôi muốn hệ thống yêu cầu tôi nhập lý do kết luận rõ ràng để làm căn cứ thông báo cho tác giả và phục vụ kiểm tra lại sau này.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Chỉ tài khoản có vai trò Administrator mới có quyền truy cập và thay đổi trạng thái xử lý báo cáo (NFR-09).
- Bắt buộc lưu vết kiểm toán (audit trail) không thể xóa sửa đối với mọi quyết định xử lý của Admin (NFR-08).
- Không áp dụng chế tài tự động bằng thuật toán trong phiên bản MVP; mọi quyết định đều do Quản trị viên con người đánh giá thủ công (BR-26).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
  - [BR-26](BUSINESS-RULES.md#br-26): Các hình thức chế tài khi xử lý vi phạm.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và toàn vẹn lịch sử kiểm toán.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

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

---

<a id="fr-29"></a>
### FR-29 — Phân quyền hiển thị thông tin báo cáo

- **Mã yêu cầu:** FR-29
- **Module:** M02 (Identity & Access), M03 (Community Recipes & Social), M09 (Administration & Moderation)
- **Tóm tắt yêu cầu:** Hệ thống phân quyền hiển thị thông tin báo cáo theo mục 3.13: Administrator thấy đầy đủ thông tin tài khoản người gửi và người bị báo cáo; tác giả thấy thông tin xử lý nhưng không thấy danh tính người báo cáo; người gửi xem trạng thái và kết quả báo cáo của mình; báo cáo vi phạm không được hiển thị công khai cho bên thứ ba.

- **Use Case detail:** [Open interaction flows](../use-cases/identity-and-access.md#fr-29).

#### Mục đích
Bảo vệ an toàn và quyền riêng tư cho người dùng tham gia đóng góp phản ánh vi phạm; ngăn chặn triệt để các hành vi đe dọa, quấy rối hoặc trả đũa cá nhân giữa các thành viên cộng đồng; đồng thời đảm bảo tính minh bạch đối với kết quả xử lý của Ban quản trị.

#### Danh mục Use Cases
- `UC-29.1`: Người gửi xem lịch sử, trạng thái và kết quả các báo cáo của chính mình.
- `UC-29.2`: Tác giả xem thông tin xử lý nội dung của mình mà không thấy danh tính người báo cáo.

#### Bảng Ma trận phân quyền hiển thị thông tin báo cáo (SRS 3.13)

| Trường thông tin báo cáo | Administrator | Người gửi báo cáo | Tác giả bài viết bị báo cáo | Người dùng khác / Guest |
|---|---|---|---|---|
| **Danh tính người gửi báo cáo** | Có (Hiển thị đầy đủ) | Có (Xem của chính mình) | **KHÔNG (Ẩn tuyệt đối)** | **KHÔNG** |
| **Nội dung bài viết bị báo cáo** | Có | Có | Có | Có (nếu bài chưa bị ẩn) |
| **Lý do và mô tả báo cáo đã gửi**| Có | Có | Có (nội dung kết luận tóm tắt) | **KHÔNG** |
| **Trạng thái xử lý (`OPEN`,...)**| Có | Có | Có (khi có quyết định) | **KHÔNG** |
| **Ghi chú kết luận của Admin** | Có | Có (kết luận chung) | Có (lý do chế tài) | **KHÔNG** |

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Việc ẩn danh tính người báo cáo đối với tác giả phải được thực thi ở tầng máy chủ (Data Transfer Object không chứa trường `reporterInfo` khi trả về cho tác giả) (NFR-08, NFR-09).
- Báo cáo vi phạm là dữ liệu riêng tư nhạy cảm, tuyệt đối không tạo endpoint public hay để lộ ra bên ngoài (NFR-08).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân & truyền tải an toàn HTTPS.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

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

---

<a id="fr-30"></a>
### FR-30 — Bổ sung thông tin và ngăn trùng lặp báo cáo mở

- **Mã yêu cầu:** FR-30
- **Module:** M03 (Community Recipes & Social), M09 (Administration & Moderation)
- **Tóm tắt yêu cầu:** Người gửi được bổ sung mô tả cho báo cáo đang mở; hệ thống ngăn cùng tài khoản tạo báo cáo mở trùng trên một bài công thức; Admin xem báo cáo nhóm theo bài; báo cáo lại sau khi giải quyết cần mô tả vấn đề mới.

- **Use Case detail:** [Open interaction flows](../use-cases/recipe-contribution-and-community.md#fr-30).

#### Mục đích
Tối ưu hóa quy trình tiếp nhận thông tin phản ánh; chống hành vi spam gửi nhiều báo cáo trùng lặp gây quá tải hệ thống quản trị; hỗ trợ Quản trị viên theo dõi bức tranh toàn cảnh về toàn bộ các phản ánh của cộng đồng trên từng bài viết cụ thể.

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-30.1`: Bổ sung thông tin cho báo cáo đang mở (Append information to open report).
  - `UC-30.2`: Xem danh sách báo cáo được nhóm theo bài viết (View reports grouped by recipe).
- **User Stories:**
  - `US-30.1`: Là một người đã gửi báo cáo, khi tôi phát hiện thêm bằng chứng vi phạm, tôi muốn bổ sung thêm vào báo cáo cũ đang chờ duyệt mà không cần tạo một báo cáo mới.
  - `US-30.2`: Là một Administrator, tôi muốn các báo cáo về cùng một bài viết được gom lại một chỗ để tôi nắm được toàn bộ phản ánh trước khi ra quyết định.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Kiểm tra tính duy nhất của báo cáo đang mở theo cặp `(reporterId, recipeId, status IN ['OPEN', 'IN_REVIEW'])` ở tầng cơ sở dữ liệu / nghiệp vụ máy chủ (NFR-08).
- Chỉ người tạo báo cáo mới có quyền bổ sung thông tin cho báo cáo đó (NFR-09).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-24](BUSINESS-RULES.md#br-24): Xác thực tài khoản khi gửi báo cáo vi phạm.
  - [BR-26](BUSINESS-RULES.md#br-26): Các hình thức chế tài khi xử lý vi phạm.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và toàn vẹn giao dịch.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

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

---

<a id="fr-31"></a>
### FR-31 — Điều kiện thông tin tối thiểu trước khi dùng AI cá nhân hóa

- **Mã yêu cầu:** FR-31
- **Module:** M02, M05, M06
- **Tóm tắt yêu cầu:** Sau lần xác thực thành công đầu tiên, hệ thống mời Member vừa đăng ký thực hiện bảng câu hỏi Onboarding một lần; lời mời không tự xuất hiện lại sau Skip hoặc ở các lần đăng nhập sau, nhưng Member có thể mở mục sở thích ăn uống trong Cài đặt để hoàn tất/cập nhật hồ sơ. Tài khoản đã tồn tại không tự động bị hỏi. Skip không khóa tính năng thông thường. Trước khi gọi AI gợi ý món hoặc tạo thực đơn cá nhân hóa, hồ sơ phải hoàn tất ba nhóm thông tin tối thiểu (loại ăn chay, nguyên liệu cần tránh do dị ứng/kiêng, món hoặc nguyên liệu không thích); chặn gọi Gemini khi thiếu thông tin; không áp dụng quota lượt/ngày.

- **Use Case detail:** [Open interaction flows](../use-cases/identity-and-access.md#fr-31).

#### Mục đích
Thu thập và chuẩn hóa dữ liệu sở thích, thói quen và các ràng buộc ăn kiêng của thành viên nhằm phục vụ các tính năng gợi ý món ăn và lập thực đơn cá nhân hóa của AI; bảo đảm quyền tự do trải nghiệm của người dùng qua cơ chế Onboarding tùy chọn (cho phép bỏ qua); đồng thời thiết lập cổng kiểm soát dữ liệu đầu vào nghiêm ngặt để đảm bảo an toàn dị ứng và tránh gửi yêu cầu AI tới provider khi thiếu thông tin cần thiết.

#### Cấu trúc hồ sơ sở thích ăn uống (Dietary Preferences Profile)

| Nhóm thông tin | Tính chất ràng buộc | Quy tắc giá trị & Validation |
|---|---|---|
| **1. Loại ăn chay (`dietaryType`)** | Ràng buộc cứng, bắt buộc phải chọn | Bắt buộc chọn đúng **1 trong 4 loại** đã được chuẩn hóa (SRS 3.20):<br>- `Vegan` (Ăn chay thuần — không thịt, cá, trứng, sữa, mật ong)<br>- `Lacto Vegetarian` (Ăn chay có dùng sữa và chế phẩm từ sữa)<br>- `Ovo Vegetarian` (Ăn chay có dùng trứng)<br>- `Lacto-Ovo Vegetarian` (Ăn chay có dùng cả trứng và sữa) |
| **2. Nguyên liệu cần tránh (`allergiesAndRestrictions`)** | Ràng buộc cứng, bắt buộc phải xác nhận | - Danh sách nguyên liệu cần tránh (chọn từ danh mục nguyên liệu chuẩn hoặc nhập văn bản tự do, ví dụ: đậu phộng, gluten, nấm, đậu nành,...).<br>- **HOẶC** người dùng phải chủ động tích chọn xác nhận: `"Không có"`.<br>- Không được để trạng thái chưa xác nhận (trống/unspecified). Bỏ trống KHÔNG được tự hiểu là không có dị ứng (BR-31). |
| **3. Món hoặc nguyên liệu không thích (`dislikedIngredients`)** | Ràng buộc cứng, bắt buộc phải xác nhận | - Danh sách món hoặc nguyên liệu không thích (chọn từ danh mục chuẩn hoặc nhập tự do, ví dụ: mướp đắng, rau mùi,...).<br>- **HOẶC** người dùng phải chủ động tích chọn xác nhận: `"Không có"`.<br>- Không được để trạng thái chưa xác nhận (trống/unspecified). Bỏ trống KHÔNG được tự hiểu là không có món không thích (BR-31). |
| **4. Khẩu vị ẩm thực (`cuisinePreference`)** | Sở thích mềm, tùy chọn | Tùy chọn khẩu vị ưa thích (ví dụ: món chay truyền thống Việt Nam, món chay phong cách Thái, Âu,...). Cho phép để trống. |
| **5. Thời gian nấu tối đa (`maxCookingTimeMinutes`)** | Sở thích mềm, tùy chọn | Số nguyên dương biểu diễn số phút mong muốn (ví dụ: $\le 30$ phút, $\le 60$ phút). Cho phép để trống. |
| **6. Độ khó nấu ăn (`cookingDifficulty`)** | Sở thích mềm, tùy chọn | Chọn một trong các mức: `Easy` (Dễ), `Medium` (Trung bình), `Advanced` (Nâng cao). Cho phép để trống. |

#### Danh mục Use Cases & User Stories
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
  - `US-31.5`: Là một thành viên, khi tôi yêu cầu AI gợi ý món hoặc tạo thực đơn cá nhân hóa mà chưa hoàn tất 3 thông tin tối thiểu, tôi muốn hệ thống giải thích rõ lý do, hướng dẫn tôi điền thông tin và không gọi Gemini khi thông tin chưa đầy đủ.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Khách vãng lai (Guest) không có hồ sơ cá nhân và không thể gọi AI cá nhân hóa (BR-05).
- Chỉ Member đã đăng nhập mới có quyền tạo và chỉnh sửa hồ sơ sở thích của chính mình.
- Dữ liệu sở thích ăn uống và dị ứng là thông tin riêng tư, được bảo vệ theo NFR-08 và NFR-20; không bao giờ được hiển thị trong hồ sơ công khai hay tiết lộ cho người dùng khác.
- Việc hoàn thành Onboarding và khai báo dị ứng là hỗ trợ cá nhân hóa ẩm thực, không cấu thành cam kết an toàn y tế tuyệt đối hay thay thế kiểm tra an toàn thực phẩm ngoài đời thực (SRS 3.15).
- Ràng buộc kỹ thuật được phê duyệt: Tích hợp Google Gemini cho các chức năng AI; kiểm soát quyền tính năng AI (Feature Entitlement) theo gói.

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest (Guest không có hồ sơ cá nhân hóa).
  - [BR-30](BUSINESS-RULES.md#br-30): Ranh giới chức năng khi bỏ qua Onboarding (không khóa các chức năng không cá nhân hóa).
  - [BR-31](BUSINESS-RULES.md#br-31): Không gọi AI khi chặn do thiếu hồ sơ; để trống không tự hiểu là "Không có".
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-04](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-04): Thời gian tạo thực đơn tuần theo hồ sơ và ràng buộc ăn uống $\le 8$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân & sở thích ăn uống, HTTPS/TLS 1.2+, kiểm soát quyền sở hữu.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt, thích ứng đa thiết bị.
  - [NFR-20](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20): Cơ chế đồng ý (Consent) và bảo vệ dữ liệu cá nhân thói quen ăn uống, dị ứng; người dùng có quyền tự xem và sửa.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

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
  - **Then:** Hệ thống ghi nhận trạng thái chưa hoàn tất Onboarding; Member vẫn có thể thực hiện tìm kiếm bài viết, lưu công thức, xếp lịch ăn thủ công và bình luận; riêng Chuyên gia vẫn có thể tạo Recipe Post bình thường mà không bị ngăn cản (BR-30).

- **AC-31.4 — Chặn gọi AI cá nhân hóa khi chưa chọn loại ăn chay:**
  - **Given:** Member chưa chọn loại ăn chay trong hồ sơ sở thích và thuộc gói có quyền sử dụng tính năng tương ứng.
  - **When:** Member nhấn yêu cầu "AI gợi ý món cho tôi" hoặc "AI tạo thực đơn tuần".
  - **Then:** Hệ thống kiểm tra và chặn yêu cầu; tuyệt đối KHÔNG gửi yêu cầu tới dịch vụ AI; hệ thống hiển thị thông báo yêu cầu chọn loại ăn chay trước (BR-31).

- **AC-31.5 — Chặn gọi AI cá nhân hóa khi chưa xác nhận thông tin dị ứng/kiêng cử:**
  - **Given:** Member đã chọn loại ăn chay nhưng chưa khai báo danh sách dị ứng và cũng chưa tích xác nhận "Không có dị ứng/kiêng cử".
  - **When:** Member nhấn yêu cầu chức năng AI cá nhân hóa.
  - **Then:** Hệ thống kiểm tra và chặn yêu cầu; tuyệt đối KHÔNG gửi yêu cầu tới dịch vụ AI; hệ thống thông báo yêu cầu xác nhận thông tin dị ứng trước (BR-31).

- **AC-31.6 — Chặn gọi AI cá nhân hóa khi chưa xác nhận món hoặc nguyên liệu không thích:**
  - **Given:** Member đã chọn loại ăn chay và xác nhận dị ứng, nhưng chưa khai báo món không thích và chưa tích xác nhận "Không có món không thích".
  - **When:** Member nhấn yêu cầu chức năng AI cá nhân hóa.
  - **Then:** Hệ thống kiểm tra và chặn yêu cầu; tuyệt đối KHÔNG gửi yêu cầu tới dịch vụ AI; hệ thống thông báo yêu cầu xác nhận thông tin món không thích trước (BR-31).

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

- **AC-31.10 — Chỉ mời Member mới một lần và không tự hỏi lại sau Skip:**
  - **Given:** Member vừa đăng ký và hoàn tất xác thực thành công lần đầu; hoặc Member đã chọn Skip tại lời mời Onboarding.
  - **When:** Member mới đăng nhập lần đầu; hoặc Member đăng nhập lại sau khi đã chọn Skip.
  - **Then:** Hệ thống chỉ hiển thị lời mời/questionnaire ở lần xác thực thành công đầu tiên của tài khoản mới; sau Skip không hiển thị lại tự động ở lần đăng nhập sau. Tài khoản đã tồn tại không tự động bị hỏi. Member vẫn dùng được các tính năng thông thường và có thể mở mục sở thích ăn uống trong Cài đặt để hoàn tất hoặc chỉnh sửa hồ sơ (UC-31.2, UC-31.3; BR-30).

---

---

<a id="fr-32"></a>
### FR-32 — Lưu, bỏ lưu và quản lý danh sách công thức đã lưu

- **Mã yêu cầu:** FR-32
- **Module:** M02 (Identity & Access), M04 (Recipe Catalog & Search), M05 (Meal Planning & Shopping List)
- **Tóm tắt yêu cầu:** Hệ thống cho Member lưu/bỏ lưu bài công thức và xem, tìm/lọc danh sách công thức đã lưu; mỗi bài chỉ xuất hiện một lần trong danh sách của cùng Member (idempotent); tham chiếu đã lưu được giữ bằng unavailable/tombstone nếu bài bị ẩn hoặc xóa vi phạm, nhưng bài không còn được khám phá công khai hoặc dùng bởi AI.

- **Use Case detail:** [Open interaction flows](../use-cases/identity-and-access.md#fr-32).

#### Mục đích
Hỗ trợ thành viên lưu trữ nhanh các bài công thức yêu thích thành một sổ tay nấu ăn cá nhân; giúp người dùng dễ dàng tìm lại và nấu lại các món ăn tâm đắc mà không phải tìm kiếm lại từ đầu; đồng thời cung cấp danh mục món ăn sẵn sàng để đưa nhanh vào Lịch ăn tuần.

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-32.1`: Lưu bài công thức vào danh sách đã lưu (Save recipe to saved list).
  - `UC-32.2`: Bỏ lưu bài công thức khỏi danh sách (Unsave recipe).
  - `UC-32.3`: Xem và tìm kiếm trong danh sách công thức đã lưu (View and search saved recipes).
- **User Stories:**
  - `US-32.1`: Là một thành viên đang lướt xem công thức, tôi muốn nhấn Lưu bài để gom các món ngon vào danh sách riêng để cuối tuần nấu thử.
  - `US-32.2`: Là một thành viên có nhiều công thức đã lưu, tôi muốn tìm kiếm nhanh theo tên món trong danh sách của mình.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Guest không có quyền lưu hay sở hữu danh sách công thức đã lưu (BR-05, BR-32).
- Máy chủ bắt buộc kiểm tra quyền sở hữu tài nguyên (Ownership Check) ở tầng nghiệp vụ; không người dùng nào được xem hay sửa danh sách đã lưu của người khác (NFR-08, NFR-09).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Công thức đã lưu và Lịch ăn.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu riêng tư cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

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

---

<a id="fr-33"></a>
### FR-33 — Thêm và quản lý bài công thức trong lịch ăn 3 bữa

- **Mã yêu cầu:** FR-33
- **Module:** M02 (Identity & Access), M04 (Recipe Catalog & Search), M05 (Meal Planning & Shopping List)
- **Tóm tắt yêu cầu:** Hệ thống cho phép Member thêm bài công thức vào ngày và một trong ba loại Bữa sáng / Bữa trưa / Bữa tối, chuyển ngày/bữa, thay công thức và xóa mục khỏi lịch ăn; mỗi bữa được có nhiều công thức nhưng không có cùng công thức trùng trong cùng ngày/bữa; tham chiếu hiện có được giữ bằng unavailable/tombstone khi bài nguồn không còn khả dụng.

- **Giới hạn phạm vi:** FR-33 quản lý việc sắp xếp món trong lịch ăn, không cung cấp chức năng xuất lịch ăn riêng. Báo cáo PDF dinh dưỡng ngày/tuần kèm thực đơn dự kiến thuộc FR-37.

- **Use Case detail:** [Open interaction flows](../use-cases/identity-and-access.md#fr-33).

#### Mục đích
Cung cấp công cụ thao tác linh hoạt, chi tiết và trực quan để thành viên tự do thiết kế từng bữa ăn cụ thể trong Lịch ăn tuần; bảo đảm dữ liệu bữa ăn chính xác, không bị trùng lặp công thức trong cùng một bữa; làm cơ sở vững chắc cho chức năng kiểm tra dinh dưỡng ngày (FR-37) và tổng hợp danh sách đi chợ tự động (FR-53).

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-33.1`: Thêm bài công thức vào bữa ăn cụ thể trong lịch (Add recipe to meal slot).
  - `UC-33.2`: Chuyển món ăn sang bữa hoặc ngày khác (Move recipe to another meal slot).
  - `UC-33.3`: Xóa món ăn khỏi bữa trong Lịch ăn (Remove recipe from meal slot).
- **User Stories:**
  - `US-33.1`: Là một thành viên, tôi muốn thêm món Canh nấm vào Bữa trưa Thứ Ba để chuẩn bị thực đơn cho gia đình.
  - `US-33.2`: Là một thành viên, tôi muốn có thể chuyển món ăn từ Bữa trưa sang Bữa tối khi tôi bận không kịp nấu bữa trưa.
  - `US-33.3`: Là một thành viên, tôi muốn thêm cả món xào và món canh vào cùng một Bữa trưa mà không bị giới hạn chỉ 1 món.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Dữ liệu các món trong Lịch ăn là thông tin riêng tư của Member (FR-23).
- Toàn bộ các thao tác thêm, chuyển, xóa đều bắt buộc kiểm tra quyền sở hữu ở tầng máy chủ (NFR-08, NFR-09).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Lịch ăn.
  - [BR-33](BUSINESS-RULES.md#br-33): Xử lý tham chiếu công thức trong Lịch ăn.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và toàn vẹn giao dịch.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

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

---

<a id="fr-34"></a>
### FR-34 — AI gợi ý món và lập menu từ công thức công khai có sẵn

- **Mã yêu cầu:** FR-34
- **Module:** M04 (Recipe Catalog & Search), M05 (Meal Planning & Shopping List), M06 (AI Assistant & Personalization)
- **Tóm tắt yêu cầu:** AI gợi ý món, lập menu và đề xuất thay thế chỉ từ Recipe Post đang công khai (`PUBLISHED`), không hidden/deleted/unavailable, dựa trên hồ sơ sở thích (FR-31) và nguyên liệu người dùng cung cấp; mọi kết quả dẫn tới bài nguồn và chỉ được lưu sau khi người dùng xác nhận (SRS 3.17, BR-31, BR-38). Member có thể chủ động yêu cầu AI tạo hướng dẫn chế biến 6–10 bước dựa trên chính Recipe Post công khai; hướng dẫn không thêm nguyên liệu, không tạo món khác và không sửa/lưu bài nguồn.

- **Use Case detail:** [Open interaction flows](../use-cases/recipe-discovery-and-classification.md#fr-34).

#### Mục đích
Tận dụng năng lực phân tích thông minh của AI để giải quyết bài toán gợi ý món ăn phù hợp với nguyên liệu thực tế và sở thích riêng của từng gia đình; bảo đảm 100% các món ăn được gợi ý đều là các công thức thực tế, có thể nấu được ngay từ kho công thức cộng đồng mà không bị ảo giác sinh ra công thức giả mạo.

#### Ranh giới an toàn của tính năng AI gợi ý món (SRS 3.17, BR-38)
- **Nguyên tắc "Không bịa công thức" (No Hallucination):** AI không tự sinh món hoặc danh sách nguyên liệu mới. Món được gợi ý phải là Recipe Post hợp lệ đang `PUBLISHED`; hướng dẫn cho Member chỉ được diễn giải và mở rộng thao tác dựa trên Recipe Post đó, không thêm nguyên liệu hoặc đổi món.
- **Minh bạch dữ kiện:** Mỗi hướng dẫn dẫn tới bài nguồn. Khi bài không nêu thời gian/nhiệt độ, AI phải nói rõ và đánh dấu các con số ước tính là `Ước tính AI`; không trình bày ước tính như dữ kiện của tác giả. Hướng dẫn chỉ hiển thị trong phiên và không sửa hoặc lưu Recipe Post.
- **Nguyên tắc dẫn nguồn bài viết:** Mỗi món ăn được gợi ý phải đi kèm đường dẫn xem chi tiết công thức gốc của tác giả trong hệ thống.
- **Nguyên tắc xác nhận trước khi lưu:** Kết quả gợi ý thực đơn tuần của AI chỉ mang tính đề xuất; hệ thống TUYỆT ĐỐI KHÔNG tự động ghi đè vào Lịch ăn của Member nếu người dùng chưa bấm nút "Xác nhận áp dụng vào Lịch ăn".

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-34.1`: Yêu cầu AI gợi ý món ăn theo nguyên liệu sẵn có (Request AI recipe suggestions by ingredients).
  - `UC-34.2`: Yêu cầu AI lập thực đơn tuần từ kho công thức (Request AI weekly meal plan generation).
  - `UC-34.3`: Xác nhận lưu thực đơn AI đề xuất vào Lịch ăn (Confirm and apply AI meal plan).
  - `UC-34.4`: Yêu cầu AI tạo hướng dẫn chế biến chi tiết cho Recipe Post công khai (Request a source-based detailed cooking guide).
- **User Stories:**
  - `US-34.1`: Là một thành viên đang có sẵn nấm và đậu phụ, tôi muốn nhờ AI tìm giúp các món ngon nấu được từ hai nguyên liệu này từ kho bài viết của cộng đồng.
  - `US-34.2`: Là một thành viên bận rộn, tôi muốn AI lên giúp tôi thực đơn 7 ngày hợp khẩu vị, tôi xem qua thấy hài lòng rồi mới bấm lưu vào Lịch ăn của mình.
  - `US-34.3`: Là một Member muốn tự tin nấu món được AI gợi ý, tôi muốn xem hướng dẫn theo từng bước từ sơ chế đến thành phẩm dựa trên bài công thức nguồn, để làm theo mà không nhầm phần AI bổ sung với nội dung tác giả.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Guest không được phép sử dụng AI cá nhân hóa gợi ý món (BR-05).
- Theo FR-10, Member Free được dùng gợi ý theo nguyên liệu ở UC-34.1 và hướng dẫn chế biến từ công thức công khai ở UC-34.4; các UC-34.2 và UC-34.3 về preview và áp dụng thực đơn tuần thông thường yêu cầu gói Plus hoặc Pro. Phạm vi này khác FR-36, vốn tạo thực đơn tuần theo dữ liệu dinh dưỡng tin cậy.
- Ràng buộc chặt chẽ trong System Prompt và kiểm tra chéo ở Backend: chỉ chấp nhận ID bài viết hợp lệ đang ở trạng thái `PUBLISHED` từ cơ sở dữ liệu, loại bỏ bất kỳ món ăn nào không có trong hệ thống (NFR-08, NFR-10).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-30](BUSINESS-RULES.md#br-30): Ranh giới chức năng khi bỏ qua Onboarding.
  - [BR-31](BUSINESS-RULES.md#br-31): Không gọi AI khi thiếu hồ sơ.
  - [BR-38](BUSINESS-RULES.md#br-38): AI chỉ dùng công thức công khai có sẵn và không tự tạo công thức mới.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-04](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-04): Thời gian tạo thực đơn tuần theo hồ sơ $\le 8$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu và toàn vẹn giao dịch.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC, chặn trái quyền.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-34.1 — AI chỉ gợi ý các món ăn có công thức nguồn công khai thực tế:**
  - **Given:** Member yêu cầu AI gợi ý món từ nguyên liệu sẵn có.
  - **When:** AI trả về kết quả gợi ý.
  - **Then:** 100% các món ăn được đề xuất đều gắn liền với Recipe Post đang công khai (`PUBLISHED`) và có đường dẫn hợp lệ dẫn tới bài viết đó (BR-38).

- **AC-34.2 — AI tuân thủ nghiêm ngặt loại ăn chay và danh sách dị ứng:**
  - **Given:** Member khai báo loại ăn chay `Vegan` và dị ứng `Đậu phộng` tại FR-31.
  - **When:** AI tạo thực đơn gợi ý.
  - **Then:** Toàn bộ các món ăn được chọn tuyệt đối không chứa trứng, sữa, mật ong hay đậu phộng.

- **AC-34.3 — Không tự động lưu thực đơn vào Lịch ăn khi chưa được người dùng xác nhận:**
  - **Given:** AI vừa tạo xong bản xem trước thực đơn tuần đề xuất.
  - **When:** Member đóng màn hình xem trước hoặc chưa bấm nút xác nhận.
  - **Then:** Lịch ăn tuần của Member giữ nguyên trạng thái cũ, không bị ghi đè hay thay đổi dữ liệu.

- **AC-34.4 — Lưu thành công thực đơn AI vào Lịch ăn sau khi người dùng xác nhận:**
  - **Given:** Member đang ở màn hình xem trước thực đơn tuần do AI đề xuất.
  - **When:** Member nhấn nút "Xác nhận áp dụng vào Lịch ăn".
  - **Then:** Hệ thống lưu toàn bộ các món ăn vào Lịch ăn tuần của Member và hiển thị thông báo thành công.

- **AC-34.5 — Chặn yêu cầu AI khi thiếu hồ sơ sở thích:**
  - **Given:** Member chưa hoàn thành 3 thông tin tối thiểu trong hồ sơ sở thích theo FR-31.
  - **When:** Member nhấn yêu cầu AI gợi ý món.
  - **Then:** Hệ thống dừng xử lý trước khi gọi Gemini và hiển thị hướng dẫn bổ sung hồ sơ (BR-31).
- **AC-34.6 — Gợi ý theo nguyên liệu vẫn tuân thủ trường phái ăn chay và dị ứng:**
  - **Given:** Member đã khai báo trường phái ăn chay và dị ứng trong hồ sơ FR-31, rồi yêu cầu gợi ý món theo nguyên liệu sẵn có.
  - **When:** Hệ thống trả về các Recipe Post được gợi ý.
  - **Then:** Các công thức được gợi ý phù hợp với trường phái ăn chay của Member và không chứa nguyên liệu gây dị ứng đã khai báo.

---

---

- **AC-34.7 — Hướng dẫn chi tiết theo đúng trình tự chế biến:**
  - **Given:** Member đã nhận gợi ý một Recipe Post công khai phù hợp và chủ động yêu cầu hướng dẫn nấu.
  - **When:** AI tạo hướng dẫn.
  - **Then:** Hướng dẫn có 6–10 bước theo thứ tự, bao gồm chuẩn bị, sơ chế, làm nóng nồi/chảo, thứ tự cho nguyên liệu vào, thao tác nấu/dấu hiệu chín, nêm nếm và hoàn thiện thành phẩm.
- **AC-34.8 — Hướng dẫn chỉ dựa trên công thức nguồn và không ghi dữ liệu nghiệp vụ:**
  - **Given:** Member yêu cầu hướng dẫn cho Recipe Post công khai.
  - **When:** Backend xử lý và trả hướng dẫn.
  - **Then:** Response dẫn đúng Recipe Post; hướng dẫn chỉ sử dụng nguyên liệu của bài nguồn, áp dụng hồ sơ ăn chay và nguyên liệu cần tránh của Member, không tạo hoặc sửa Recipe Post và không ghi Meal Plan.
- **AC-34.9 — Phân biệt ước tính AI với nội dung nguồn:**
  - **Given:** Bài nguồn không nêu thời gian hoặc nhiệt độ ở một công đoạn.
  - **When:** AI bổ sung gợi ý thao tác.
  - **Then:** AI nêu rõ bài nguồn không cung cấp dữ kiện đó và gắn nhãn `Ước tính AI` cho mọi con số ước tính.
<a id="fr-35"></a>
### FR-35 — Khai báo hồ sơ dinh dưỡng và xem chỉ số tham khảo cá nhân

- **Mã yêu cầu:** FR-35
- **Module:** M02, M10

- **Use Case detail:** [Open interaction flows](../use-cases/nutrition.md#fr-35).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cho phép Member đã đăng nhập và đủ điều kiện sức khỏe khai báo/cập nhật thông số nhân trắc học (`date_of_birth`, giới tính, chiều cao, cân nặng, mức độ vận động) cùng mục tiêu dinh dưỡng chung tùy chọn (`MAINTAIN_WEIGHT`, `LOSE_WEIGHT`, `GAIN_WEIGHT`, `BUILD_MUSCLE`, `IMPROVE_HEALTH`, `SUPPORT_TRAINING`); service tính tuổi từ ngày sinh để kiểm tra điều kiện 18–120, không lưu tuổi cố định; tính toán chỉ số BMI tham khảo và hiển thị nhu cầu năng lượng/dinh dưỡng ước tính dựa trên dữ liệu chuẩn USDA/NIH. Mục tiêu dinh dưỡng chung không bắt buộc và không tự thay đổi các con số FR-35; nó được dùng làm mục tiêu đã lưu cho chế độ AI dựa trên hồ sơ của FR-36. Bắt buộc hiển thị tuyên bố từ chối trách nhiệm y tế (Medical Safety Disclaimer); hệ thống tuyệt đối không tự chẩn đoán, kê đơn hay áp đặt chế độ ăn điều trị.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập, từ đủ 18 tuổi, không mang thai, không cho con bú và không có bệnh lý mạn tính cần chế độ ăn điều trị đặc biệt (SRS 3.18, BR-41, BR-42).
  - Không áp dụng: Guest (BR-05), người dùng dưới 18 tuổi, phụ nữ có thai/cho con bú, người cần can thiệp y tế (BR-42).
- **Phân loại Actor:**
  - Primary Actor: `Member` (đã đăng nhập tài khoản hợp lệ).
  - Supporting Actor: `Hệ thống tính toán dinh dưỡng nội bộ` (tham chiếu dữ liệu USDA/NIH).

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-35.1`: Khai báo và cập nhật thông số hồ sơ dinh dưỡng cá nhân.
  - `UC-35.2`: Xem chỉ số BMI và phân loại thể trạng tham khảo kèm thông điệp từ chối trách nhiệm y tế.
  - `UC-35.3`: Xem bảng nhu cầu 9 chỉ tiêu dinh dưỡng hàng ngày tham khảo.
- **User Stories:**
  - *Là một Member ăn chay*, tôi muốn khai báo chiều cao, cân nặng và mức độ vận động để hệ thống cung cấp các chỉ số tham khảo phù hợp với thể trạng của tôi, giúp tôi theo dõi dinh dưỡng một cách an toàn mà không bị nhầm lẫn với chẩn đoán y khoa.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chỉ Member sở hữu tài khoản mới có quyền xem và cập nhật hồ sơ dinh dưỡng của mình. Guest và Member khác không có quyền truy cập.
- **Ràng buộc an toàn:** Tuyệt đối không quy đổi BMI thành chế độ ăn cắt giảm calorie cực đoan; hệ thống không cung cấp chức năng kê đơn thuốc hay thực phẩm chức năng.

#### Ma trận truy vết (Traceability Matrix)
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

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-35.1 (Hiển thị Disclaimer y tế):**
  - *Given* Member đã đăng nhập và truy cập giao diện Hồ sơ dinh dưỡng,
  - *When* trang được hiển thị,
  - *Then* hệ thống trình bày nổi bật văn bản Tuyên bố từ chối trách nhiệm y tế nêu rõ số liệu chỉ mang tính chất tham khảo dựa trên chuẩn USDA/NIH và không thay thế tư vấn y khoa chuyên nghiệp.
- **AC-35.2 (Kiểm tra ngày sinh và chặn đối tượng ngoài phạm vi hỗ trợ):**
  - *Given* Member nhập `date_of_birth` trong tương lai hoặc trước ngày 01/01/1900, hoặc ngày sinh cho thấy tuổi do service tính nằm ngoài khoảng 18–120; Member cũng có thể khai báo đang mang thai, cho con bú hoặc có bệnh lý cần điều trị,
  - *When* Member gửi yêu cầu lưu hồ sơ hoặc tính nhu cầu dinh dưỡng,
  - *Then* hệ thống từ chối ngày sinh không hợp lệ; với người ngoài khoảng tuổi hoặc thuộc nhóm loại trừ, hệ thống không tính nhu cầu cá nhân hóa và hiển thị hướng dẫn tham vấn bác sĩ/chuyên gia y tế. Tuổi được tính từ `date_of_birth`, không lưu thành thuộc tính tuổi cố định (Q7).
- **AC-35.3 (Xác thực dữ liệu nhập liệu nhân trắc học):**
  - *Given* Member nhập thông số chiều cao hoặc cân nặng ngoài ngưỡng sinh lý hợp lệ ($100 \le \text{chiều cao} \le 250$ cm, $30 \le \text{cân nặng} \le 300$ kg),
  - *When* Member nhấn nút lưu hồ sơ,
  - *Then* hệ thống ngăn chặn việc lưu và hiển thị thông báo lỗi cụ thể tại từng ô nhập liệu tương ứng.
- **AC-35.4 (Tính toán BMI tham khảo chính xác):**
  - *Given* Member lưu chiều cao 170 cm và cân nặng 65 kg hợp lệ,
  - *When* hệ thống lưu hồ sơ và sau đó Member mở lại hồ sơ,
  - *Then* hệ thống lưu BMI chính xác (22.4913494810 trước khi định dạng hiển thị), đọc lại BMI đã lưu mà không tính lại khi mở trang, hiển thị 22.5 cùng phân loại "Bình thường" và ghi chú đây là chỉ số sàng lọc tham khảo. Khi chiều cao hoặc cân nặng được cập nhật, hệ thống tính và lưu BMI mới cùng hồ sơ.
- **AC-35.5 (Tính toán bảng 9 chỉ tiêu tham khảo):**
  - *Given* Member có thông số nhân trắc học và mức độ vận động hợp lệ đã được lưu,
  - *When* hệ thống hiển thị hồ sơ dinh dưỡng,
  - *Then* bảng kết quả trình bày đầy đủ định lượng tham khảo hàng ngày cho 9 chỉ tiêu cốt lõi: Năng lượng (kcal), Chất đạm (g), Carbohydrate (g), Chất béo (g), Chất xơ (g), Canxi (mg), Sắt (mg), Vitamin B12 (mcg), Kẽm (mg).
- **AC-35.6 (Bảo mật dữ liệu dinh dưỡng cá nhân):**
  - *Given* một người dùng khác hoặc Guest xem trang hồ sơ công khai của Member,
  - *When* trang hồ sơ công khai được tải,
  - *Then* các thông số chiều cao, cân nặng, BMI và bảng dinh dưỡng cá nhân hoàn toàn không xuất hiện trên giao diện công khai.

---

---

<a id="fr-36"></a>
### FR-36 — AI lập menu theo nhu cầu dinh dưỡng từ công thức tin cậy

- **Mã yêu cầu:** FR-36
- **Module:** M04, M05, M06, M10

- **Use Case detail:** [Open interaction flows](../use-cases/nutrition.md#fr-36).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp trong trang Lịch ăn hai chế độ tạo thực đơn tham khảo bằng AI cho các ngày từ hôm nay đến hết tuần hiện tại. Ngày trước hôm nay được hiển thị là đã qua và không có đề xuất; ngày thuộc tuần khác không nằm trong phạm vi AI đề xuất. Chế độ “Dựa trên hồ sơ” đọc mục tiêu chung đã lưu trong FR-35 cùng BMI, dữ liệu nhân trắc học/dinh dưỡng và các ràng buộc ăn uống đã khai báo; nếu mục tiêu hoặc câu trả lời sở thích chưa có, giữ là `null`, không bắt hoàn tất onboarding và không suy đoán mục tiêu cân nặng. Chế độ “Theo mong muốn tuần này” nhận mục tiêu riêng cho lần tạo lịch hiện tại cùng nguyên liệu muốn có/cần tránh; mọi câu trả lời đều tùy chọn, chỉ có hiệu lực cho lần tạo lịch này, không lưu vào hồ sơ và không tự động áp dụng về sau. Nguyên liệu muốn có chỉ là ưu tiên mềm trong tập công thức hợp lệ; mọi công thức chứa nguyên liệu người dùng chọn tránh bị loại khỏi tập ứng viên và không thể được ưu tiên hoặc chọn, kể cả khi có nguyên liệu người dùng muốn có. Với cả hai chế độ, AI chỉ được lựa chọn từ các Recipe Post đang công khai trong hệ thống có dữ liệu định lượng dinh dưỡng tin cậy (100% nguyên liệu ánh xạ gram và có số liệu đầy đủ); AI không được tự tạo món ngoài cơ sở dữ liệu. Mỗi ngày trong phạm vi đề xuất có ba bữa cố định; mỗi bữa có thể gồm nhiều công thức, mỗi công thức một khẩu phần. Tổng kcal hướng tới phân bổ 25% cho sáng, 37,5% cho trưa và 37,5% cho tối theo tổng mục tiêu năng lượng ngày FR-35; mỗi tỷ lệ có dung sai ±5 điểm phần trăm, không cần trùng chính xác tỷ lệ trung tâm. Hệ thống phối hợp công thức để hướng tới phân bổ này và tối ưu các chỉ tiêu còn lại trong bộ chín chỉ tiêu cá nhân. BMI góp phần cá nhân hóa lựa chọn nhưng không tự thay đổi mục tiêu số FR-35. Member phải xem trước và xác nhận trước khi áp dụng; các bữa trước hôm nay được giữ nguyên. Preview chỉ trình bày tổng dinh dưỡng dự kiến, không phân loại thiếu/thừa. Mục tiêu tăng/giảm cân trong yêu cầu menu là ưu tiên lựa chọn món, không phải cam kết thay đổi cân nặng hay mục tiêu y tế; không tự suy ra mức thâm hụt/thặng dư calorie.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập, có hồ sơ nhân trắc học hợp lệ và xác nhận đủ điều kiện (FR-35/FR-38), và có gói Plus hoặc Pro hợp lệ cho AI lập thực đơn dinh dưỡng (FR-10). Onboarding sở thích FR-31 và mục tiêu chung là tùy chọn cho FR-36; câu trả lời chưa khai báo là `null`, không đồng nghĩa xác nhận “không có”. Nếu đã khai báo trường phái ăn chay hoặc nguyên liệu dị ứng/cần tránh, các dữ liệu đó là ràng buộc an toàn bắt buộc. BMI được dùng cùng dữ liệu hồ sơ để cá nhân hóa lựa chọn, không tự đặt hoặc thay đổi mục tiêu tham khảo. Câu trả lời của “Theo mong muốn tuần này” chỉ có trong request hiện tại.
  - Giới hạn: Quá trình tạo thực đơn chỉ mang tính tham khảo và hỗ trợ lên kế hoạch ăn uống; việc tối ưu hóa vi chất chuyên sâu theo bệnh lý nằm ngoài phạm vi MVP.
- **Phân loại Actor:**
  - Primary Actor: `Member` (đã đăng nhập và đủ điều kiện).
  - Supporting Actor: `Google Gemini AI` (đóng vai trò tổng hợp và đề xuất món dựa trên prompt giới hạn và kho dữ liệu công thức tin cậy).

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-36.1`: Yêu cầu AI tạo thực đơn tham khảo theo hồ sơ đã lưu hoặc theo mong muốn riêng của tuần hiện tại.
  - `UC-36.2`: Xem danh sách món được đề xuất kèm giải trình dinh dưỡng và cảnh báo tham khảo.
  - `UC-36.3`: Xác nhận áp dụng thực đơn gợi ý vào các bữa ăn trong Lịch ăn tuần.
- **User Stories:**
  - *Là một Member bận rộn*, tôi muốn nhờ AI đề xuất thực đơn các món chay từ hôm nay đến hết tuần hiện tại dựa trên nhu cầu dinh dưỡng của tôi và chỉ chọn từ những công thức đã có sẵn công thức nấu chuẩn, giúp tôi lên lịch cho các ngày còn lại trong tuần.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member đã đăng nhập có gói Plus hoặc Pro còn hiệu lực mới có quyền yêu cầu AI lập thực đơn dinh dưỡng. Guest và Free không có quyền này (FR-10, BR-02, BR-03).
- **Ràng buộc nghiệp vụ:** AI chỉ đóng vai trò ghép nối công thức có sẵn; hệ thống cấm tuyệt đối việc tạo công thức ảo hoặc ghi đè số liệu dinh dưỡng không qua kiểm chứng (BR-38, BR-40, BR-41).
- **Hai chế độ yêu cầu:** `PROFILE` dùng mục tiêu dinh dưỡng chung trong FR-35 nếu có; `THIS_WEEK` nhận `goal`, `preferredIngredientIds` và `avoidedIngredientIds` chỉ trong request hiện tại. Nguyên liệu dị ứng/cần tránh từ FR-31 và mọi nguyên liệu trong `avoidedIngredientIds` là hard exclusion; nguyên liệu trong `preferredIngredientIds` chỉ là ưu tiên mềm trong số công thức còn hợp lệ. Nếu cùng một nguyên liệu nằm trong cả hai danh sách, điều kiện tránh luôn thắng. Giá trị của chế độ `THIS_WEEK` không được lưu hoặc tái sử dụng cho request khác.
- **Mức cá nhân hóa:** Mục tiêu được chuyển thành tiêu chí ưu tiên chọn công thức trong pool tin cậy. Tổng năng lượng và 9 chỉ tiêu của món/menu được tính từ dữ liệu RecipePost đã xác thực; mục tiêu tuần không làm thay đổi công thức mục tiêu FR-35, không yêu cầu cân nặng/thời hạn mục tiêu và không phải cam kết kết quả cân nặng.

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Quyền AI cơ bản cho Guest và Free.
  - [BR-02](BUSINESS-RULES.md#br-02): Quyền AI nâng cao theo gói Plus và Pro.
  - [BR-03](BUSINESS-RULES.md#br-03): Kiểm tra quyền tính năng trước khi gọi Gemini.
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider và timeout AI.
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-30](BUSINESS-RULES.md#br-30): Ranh giới chức năng khi bỏ qua Onboarding.
  - [BR-31](BUSINESS-RULES.md#br-31): Không gọi AI khi thiếu hồ sơ.
  - [BR-35](BUSINESS-RULES.md#br-35): Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn.
  - [BR-36](BUSINESS-RULES.md#br-36): Quy tắc 3 loại bữa ăn cố định trong Lịch ăn MVP.
  - [BR-37](BUSINESS-RULES.md#br-37): Tính duy nhất của công thức trong cùng một bữa ăn ngày.
  - [BR-38](BUSINESS-RULES.md#br-38): AI không tự tạo công thức mới ngoài hệ thống.
  - [BR-40](BUSINESS-RULES.md#br-40): Điều kiện dữ liệu dinh dưỡng tin cậy cho AI menu.
  - [BR-41](BUSINESS-RULES.md#br-41): Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia.
  - [BR-50](BUSINESS-RULES.md#br-50): Cho phép công khai bài thiếu dữ liệu dinh dưỡng (nhưng bị loại khỏi pool AI menu).
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-04](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-04): Thời gian tạo thực đơn tuần theo hồ sơ $\le 8$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân và chỉ số sức khỏe.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-20](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20): Cơ chế bảo vệ dữ liệu sức khỏe và ranh giới thông tin.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
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
- **AC-36.4 (Xác nhận của Member trước khi cập nhật Lịch ăn):**
  - *Given* AI đã sinh bản xem trước thực đơn thành công,
  - *When* Member chưa nhấn nút "Lưu vào Lịch ăn",
  - *Then* hệ thống không tự động thay đổi bất kỳ mục nào trong Lịch ăn tuần hiện tại của Member; khi Member xác nhận, hệ thống chỉ thay các mục từ hôm nay đến hết Chủ nhật bằng các món được chọn trong preview trong một giao dịch nguyên tử, giữ nguyên mọi mục trước hôm nay. Nếu bất kỳ món nào không còn hợp lệ, toàn bộ thao tác rollback và lịch cũ được giữ nguyên.
- **AC-36.5 (Kiểm tra quyền gói Plus hoặc Pro trước khi gọi AI):**
  - *Given* Member chỉ có gói Free,
  - *When* gửi yêu cầu tạo thực đơn dinh dưỡng bằng AI,
  - *Then* Backend từ chối trước khi gọi Gemini và hướng dẫn nâng cấp lên Plus; không kiểm tra số dư lượt/ngày.
- **AC-36.6 (Lỗi kỹ thuật không làm thay đổi quyền tính năng):**
  - *Given* Member có gói Plus hoặc Pro hợp lệ gửi yêu cầu tạo thực đơn AI,
  - *When* dịch vụ AI gặp lỗi kết nối hoặc thời gian phản hồi vượt quá 8 giây,
  - *Then* hệ thống thông báo lỗi tới người dùng và quyền gói Plus hoặc Pro không đổi.
- **AC-36.7 (Hai chế độ gợi ý trên trang Lịch ăn):** Member đủ điều kiện và có Plus hoặc Pro chọn được “Dựa trên hồ sơ” hoặc “Theo mong muốn tuần này”; cả hai đều hiển thị bản xem trước trước khi áp dụng.
- **AC-36.8 (Mục tiêu hồ sơ có thể để trống):** Ở chế độ hồ sơ, mục tiêu đã lưu được dùng làm tiêu chí ưu tiên; nếu null, hệ thống vẫn tạo menu cân bằng theo dữ liệu hợp lệ khác, không buộc trả lời mục tiêu và không tự gán mục tiêu cân nặng.
- **AC-36.9 (Câu trả lời tuần này chỉ có hiệu lực một lần):** Chế độ tuần này dùng mục tiêu cùng nguyên liệu muốn có/cần tránh chỉ cho request hiện tại; không ghi vào hồ sơ hoặc áp dụng sang tuần sau nếu Member không chọn lại. Ràng buộc an toàn đã khai báo từ FR-31 vẫn luôn được áp dụng. Mọi công thức có chứa nguyên liệu trong danh sách cần tránh của tuần bị loại khỏi candidate pool; quy tắc này luôn thắng nguyên liệu muốn có. Nguyên liệu muốn có chỉ xếp ưu tiên mềm trong các công thức còn lại.
- **AC-36.10 (Không suy diễn mục tiêu định lượng hoặc hứa kết quả):** Mục tiêu giảm/tăng cân hoặc tăng cơ chỉ điều hướng lựa chọn công thức; năng lượng và 9 chỉ tiêu vẫn là dữ liệu tính được từ hồ sơ/RecipePost có nguồn, không tự thêm thâm hụt/thặng dư calorie hay cam kết cân nặng.
- **AC-36.11 (Không ép hoàn tất hồ sơ sở thích):** Member đủ điều kiện FR-35/FR-38 và quyền gói Plus hoặc Pro nhưng chưa hoàn tất một hoặc nhiều câu hỏi FR-31 vẫn có thể tạo menu; dữ liệu chưa khai báo là `null`, mọi ràng buộc đã biết được áp dụng và UI giải thích rằng mức cá nhân hóa phụ thuộc thông tin đã cung cấp.
- **AC-36.12 (Ba bữa trong các ngày còn lại và lặp món có kiểm soát):** Preview thành công bao gồm từng ngày từ hôm nay đến hết Chủ nhật với ba bữa cố định; các ngày trước hôm nay được ghi rõ là đã qua và AI không đề xuất món. Ngày thuộc tuần khác không có đề xuất. Mỗi bữa có thể có một hoặc nhiều công thức. Hệ thống được lặp công thức giữa các ngày/bữa khi pool không đủ đa dạng nhưng không lặp cùng công thức trong một bữa; ưu tiên tránh lặp ở các bữa liền kề. Member có thể bỏ chọn từng món trước khi xác nhận; các mục trước hôm nay được giữ nguyên khi áp dụng.
- **AC-36.13 (Phối hợp nhiều món, mỗi món một khẩu phần):** Given Member yêu cầu AI tạo menu tuần, When xem preview hoặc áp dụng menu, Then mỗi ngày trong phạm vi có ba bữa cố định và mỗi bữa có thể chứa nhiều công thức công khai; mỗi công thức được đề xuất ở một khẩu phần. Hệ thống phối hợp nhiều công thức để hướng tới phân bổ kcal và tối ưu chín chỉ tiêu. Một công thức không bị lặp trong cùng một bữa.
- **AC-36.14 (Tối ưu và trình bày tổng dinh dưỡng theo ngày):** Given hồ sơ có BMI và chín mức tham khảo cá nhân từ FR-35, When AI tạo menu, Then hệ thống tổng hợp dinh dưỡng dự kiến của toàn bộ công thức trong từng ngày, ưu tiên đạt mục tiêu kcal ngày và phân bổ ba bữa theo AC-36.15; trong các phương án có mức đáp ứng phân bổ năng lượng tương đương, tối đa hóa số trong tám chỉ tiêu dinh dưỡng còn lại đạt mức tham khảo, rồi ưu tiên phương án có tổng chênh lệch tương đối nhỏ hơn. BMI được dùng cùng hồ sơ để cá nhân hóa lựa chọn, không tự thay đổi các mức tham khảo. Preview chỉ hiển thị tổng dinh dưỡng ngày, không phân loại hoặc mô tả chỉ tiêu là thiếu hay thừa.
- **AC-36.15 (Phân bổ năng lượng giữa ba bữa):** Given mục tiêu năng lượng tham khảo ngày FR-35 là `E` kcal, When AI lập menu, Then tổng kcal dự kiến của bữa sáng hướng tới `25% × E`, bữa trưa `37,5% × E`, bữa tối `37,5% × E`; mỗi bữa đạt yêu cầu khi tỷ lệ nằm trong khoảng ±5 điểm phần trăm quanh tỷ lệ mục tiêu tương ứng. Hệ thống tính từ toàn bộ món và khẩu phần dự kiến của bữa, hiển thị kcal/tỷ lệ từng bữa và không yêu cầu khớp đúng tỷ lệ trung tâm. Nếu pool công thức tin cậy không đáp ứng được các khoảng này, preview nêu rõ chênh lệch.
- **AC-36.16 (Giới hạn đề xuất trong tuần hiện tại):** Given Member mở Lịch ăn trong một ngày bất kỳ, When yêu cầu AI tạo thực đơn, Then chỉ các ngày từ ngày hiện tại đến hết Chủ nhật của tuần đó được đề xuất. Các ngày trước hôm nay phải hiển thị rõ là đã qua và AI không đề xuất; các ngày sau Chủ nhật hiện tại không được đề xuất. Nếu tuần đang xem không giao với khoảng này, preview không có món và không gọi bước xếp hạng AI. Khi áp dụng menu, hệ thống chỉ xóa/thay các mục từ hôm nay đến hết tuần hiện tại, giữ nguyên các mục của ngày đã qua.

---

---

<a id="fr-37"></a>
### FR-37 — Khai báo khẩu phần, kiểm tra dinh dưỡng menu ngày/tuần và xuất báo cáo PDF

- **Mã yêu cầu:** FR-37
- **Module:** M05, M06, M10

- **Use Case detail:** [Open interaction flows](../use-cases/nutrition.md#fr-37).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp chức năng cho phép Member tùy chỉnh số khẩu phần ăn cho từng món trong Lịch ăn (Sáng, Trưa, Tối); hệ thống tự động tính toán dinh dưỡng theo 9 chỉ tiêu cốt lõi theo ngày đơn lẻ hoặc tổng hợp cả tuần dựa trên định lượng nguyên liệu và số khẩu phần; đối chiếu với mức nhu cầu tham khảo cá nhân (nếu đã khai báo hồ sơ) để hiển thị trạng thái đạt, chưa đạt hoặc vượt mức kèm chênh lệch định lượng; hỗ trợ phân tích tổng thể tuần (tính trung bình cộng 7 ngày, thống kê tỷ lệ ngày đạt chuẩn vi chất); hỗ trợ xuất báo cáo dinh dưỡng ngày hoặc tuần dưới định dạng file PDF phục vụ theo dõi cá nhân hoặc tham khảo ý kiến chuyên gia; hiển thị cảnh báo minh bạch nếu có món ăn chứa nguyên liệu chưa có dữ liệu dinh dưỡng; nghiêm cấm hiển thị điểm tổng hợp (health score) hoặc nhãn phán xét đơn giản hóa; xác lập rõ ranh giới thông tin là "Dinh dưỡng dự kiến theo thực đơn (Planned Intake)", không khẳng định là nhật ký thực tế đã hấp thụ.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập và quản lý Lịch ăn cá nhân (FR-09, FR-33).
  - Không áp dụng: Guest không có quyền lưu lịch ăn (BR-05, BR-32). Chức năng không bao gồm ghi nhận nhật ký ăn thực tế (Food Intake Log / Meal Check-in) trong phạm vi MVP.
- **Phân loại Actor:**
  - Primary Actor: `Member` (đã đăng nhập).
  - Supporting Actor: `Hệ thống tính toán dinh dưỡng nội bộ`, `Google Gemini AI` (tùy chọn khi Member yêu cầu giải thích).

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-37.1`: Khai báo và điều chỉnh số lượng khẩu phần cho từng món ăn trong Lịch ăn ngày.
  - `UC-37.2`: Xem bảng tổng hợp dinh dưỡng ngày theo 9 chỉ tiêu cốt lõi so sánh với mức tham khảo cá nhân.
  - `UC-37.3`: Xem cảnh báo chi tiết các món ăn bị thiếu dữ liệu dinh dưỡng trong ngày.
  - `UC-37.4`: Yêu cầu AI phân tích chênh lệch dinh dưỡng và gợi ý món điều chỉnh từ kho công thức có sẵn.
  - `UC-37.5`: Xem bảng phân tích tổng thể dinh dưỡng tuần (Overall weekly nutrition summary) với giá trị trung bình 7 ngày và nhận diện vi chất đặc thù ăn chay.
  - `UC-37.6`: Xuất báo cáo dinh dưỡng ngày hoặc tuần dưới định dạng file PDF (Export nutrition report).
- **User Stories:**
  - *Là một Member đang lập kế hoạch ăn uống*, tôi muốn điều chỉnh số khẩu phần dự kiến trong thực đơn và kiểm tra tổng dinh dưỡng trong ngày cũng như phân tích cả tuần so với mức khuyến nghị tham khảo, để đánh giá thực đơn kế hoạch mà không coi đó là nhật ký lượng thực tế đã ăn.
  - *Là một Member đang chuẩn bị tham vấn chuyên gia dinh dưỡng hoặc bác sĩ*, tôi muốn xuất báo cáo dinh dưỡng thực đơn dạng PDF (ngày hoặc tuần) kèm đầy đủ chỉ tiêu, danh sách món và ghi chú minh bạch, để tiện in ấn và chia sẻ.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chỉ Member sở hữu Lịch ăn mới có quyền điều chỉnh khẩu phần, xem bảng kiểm tra dinh dưỡng và xuất báo cáo PDF.
- **Ràng buộc an toàn:**
  - Không được phép tổng hợp thành một chỉ số điểm số duy nhất (Health Score) để đánh giá ngày hay tuần ăn (BR-45).
  - Luôn gắn kèm cảnh báo khi dữ liệu chưa trọn vẹn (BR-48).
  - Bắt buộc gắn kèm Tuyên bố từ chối trách nhiệm y tế (BR-41) trên cả giao diện xem trực tiếp và file PDF xuất ra.
  - Xác định rõ bản chất thông tin là ước tính theo thực đơn kế hoạch (Planned Intake), không thay thế hồ sơ bệnh án hay chẩn đoán lâm sàng.

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Lịch ăn.
  - [BR-36](BUSINESS-RULES.md#br-36): Quy tắc 3 loại bữa ăn cố định trong Lịch ăn MVP.
  - [BR-37](BUSINESS-RULES.md#br-37): Tính duy nhất của công thức trong cùng một bữa ăn ngày.
  - [BR-39](BUSINESS-RULES.md#br-39): Vai trò tham khảo của chỉ số BMI.
  - [BR-40](BUSINESS-RULES.md#br-40): Điều kiện dữ liệu dinh dưỡng tin cậy cho AI menu.
  - [BR-41](BUSINESS-RULES.md#br-41): Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia.
  - [BR-43](BUSINESS-RULES.md#br-43): Gắn số liệu dinh dưỡng với một khẩu phần.
  - [BR-44](BUSINESS-RULES.md#br-44): Quy tắc diễn giải mức tham khảo theo từng chỉ tiêu dinh dưỡng.
  - [BR-45](BUSINESS-RULES.md#br-45): Cấm hiển thị điểm tổng hợp hoặc nhãn đơn giản hóa.
  - [BR-46](BUSINESS-RULES.md#br-46): Tính dinh dưỡng từ các chỉ tiêu hiện có trong catalog.
  - [BR-47](BUSINESS-RULES.md#br-47): Phân bổ dinh dưỡng theo số khẩu phần và cộng dồn ngày.
  - [BR-48](BUSINESS-RULES.md#br-48): Xử lý nguyên liệu thiếu định lượng hoặc thiếu số liệu dinh dưỡng.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân và chỉ số sức khỏe.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.
  - [NFR-20](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20): Cơ chế bảo vệ dữ liệu sức khỏe cá nhân.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
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
- **AC-37.6 (Nút So sánh và Thống kê mỗi ngày trong tuần):**
  - *Given* Member đã xếp thực đơn cho các ngày trong tuần trên Lịch ăn,
  - *When* Member bấm nút "So sánh & Thống kê 7 ngày",
  - *Then* hệ thống hiển thị bảng ma trận trực quan gồm 7 cột ngày (Thứ Hai đến Chủ Nhật) chi tiết 9 chỉ tiêu, kèm cột trung bình tuần và cột so sánh chênh lệch với DRI cá nhân (thanh tiến trình/màu sắc trực quan cho từng ngày đạt/chưa đạt), không tạo điểm số tổng hợp (Health Score) theo BR-45.
- **AC-37.7 (Xuất báo cáo PDF tuần hoàn chỉnh A4 2 trang):**
  - *Given* Member nhấn nút "Xuất báo cáo PDF" và chọn phạm vi tuần,
  - *When* file PDF được tạo thành công,
  - *Then* tệp PDF đạt chuẩn in ấn A4 (2 trang), chứa đầy đủ: Trang 1 gồm thông tin chung, bảng ma trận thống kê 7 ngày x 9 chỉ tiêu so sánh với DRI cá nhân; Trang 2 gồm tóm tắt thực đơn 3 bữa/ngày, cảnh báo nguyên liệu thiếu dữ liệu (BR-48) và tuyên bố từ chối trách nhiệm y tế chuẩn (BR-41).
- **AC-37.8 (Kiểm soát quyền riêng tư khi xuất PDF):**
  - *Given* Member A đang đăng nhập,
  - *When* yêu cầu xuất báo cáo PDF,
  - *Then* hệ thống chỉ xuất báo cáo từ dữ liệu thực đơn của chính Member A, không thể xem hoặc xuất báo cáo của tài khoản khác (SF-37.1, NFR-08, NFR-09).

- **AC-37.9 (Xuất báo cáo PDF ngày một trang):**
  - *Given* Member chọn một ngày có ít nhất một món trong Lịch ăn và nhấn "Xuất báo cáo PDF" với phạm vi ngày,
  - *When* file được tạo thành công,
  - *Then* hệ thống tải file PDF A4 một trang gồm thông tin ngày, ba bữa, tên món, số khẩu phần dự kiến, bảng chín chỉ tiêu, mức tham khảo, cảnh báo thiếu dữ liệu, timestamp và tuyên bố miễn trừ y tế.

---

---

<a id="fr-38"></a>
### FR-38 — Xác nhận phạm vi hỗ trợ trước khi dùng chức năng dinh dưỡng

- **Mã yêu cầu:** FR-38
- **Module:** M02, M10
- **Tóm tắt yêu cầu:** Trước khi truy cập hoặc sử dụng các chức năng dinh dưỡng cá nhân (hồ sơ dinh dưỡng, AI menu theo dinh dưỡng, kiểm tra menu ngày), hệ thống bắt buộc Member xác nhận thuộc phạm vi đối tượng được hỗ trợ trong MVP (từ đủ 18 tuổi, không mang thai/cho con bú, không cần chế độ ăn điều trị bệnh); người không đủ điều kiện được giải thích rõ ranh giới an toàn y tế, bị từ chối chức năng dinh dưỡng cá nhân nhưng vẫn được sử dụng bình thường toàn bộ các chức năng không-dinh-dưỡng khác. Eligibility confirmation tách biệt với đồng thuận lưu/xử lý dữ liệu hồ sơ do FR-35 quản lý.

- **Use Case detail:** [Open interaction flows](../use-cases/nutrition.md#fr-38).

#### Mục đích
Thiết lập cổng kiểm soát an toàn y tế và sức khỏe bắt buộc trước khi cung cấp các chức năng liên quan đến dữ liệu dinh dưỡng cá nhân; yêu cầu Member chủ động xác nhận mình thuộc nhóm đối tượng áp dụng an toàn của MVP; giải thích minh bạch bản chất thông tin dinh dưỡng là ước tính tham khảo lập kế hoạch; đồng thời đảm bảo việc không đủ điều kiện dinh dưỡng không gây phân biệt đối xử hoặc khóa các chức năng thông thường của người dùng.

#### Tiêu chí đối tượng hỗ trợ hợp lệ trong MVP (Eligible Target Audience)
Theo quy định an toàn tại [BR-42](BUSINESS-RULES.md#br-42), chức năng dinh dưỡng MVP **CHỈ ÁP DỤNG** cho người dùng đồng thời thỏa mãn cả ba điều kiện:
1. **Độ tuổi:** Người trưởng thành từ đủ 18 tuổi trở lên.
2. **Tình trạng thai kỳ & nuôi con:** Không đang trong giai đoạn mang thai hoặc cho con bú.
3. **Tình trạng bệnh lý:** Không mắc các bệnh lý đòi hỏi chế độ ăn điều trị chuyên biệt theo chỉ định của bác sĩ (ví dụ: suy thận mạn tính, đái tháo đường phụ thuộc insulin, bệnh gan giai đoạn nặng, gout cấp tính, suy tim, rối loạn chuyển hóa nặng,...).

#### Ranh giới thông tin y tế và vai trò của chỉ số BMI
- **Ranh giới tham khảo (BR-41):** Mọi số liệu dinh dưỡng, ước tính năng lượng (kcal), 9 chỉ tiêu dưỡng chất và kết quả kiểm tra menu ngày trong hệ thống chỉ là thông tin ước tính hỗ trợ lập kế hoạch ăn uống cá nhân dựa trên các hướng dẫn dinh dưỡng tham khảo của USDA/NIH; hệ thống KHÔNG đưa ra chẩn đoán y khoa, không thay thế bác sĩ chuyên khoa hoặc chuyên gia dinh dưỡng lâm sàng, và không đưa ra chế độ ăn điều trị bệnh.
- **Vai trò tham khảo của BMI (BR-39):** Chỉ số BMI (Body Mass Index) chỉ được tính toán và hiển thị như một chỉ số sàng lọc tham khảo trong hồ sơ cá nhân rộng hơn (gồm tuổi, giới tính, chiều cao, cân nặng, mức vận động); BMI hoặc mục tiêu cân nặng TUYỆT ĐỐI KHÔNG được sử dụng độc lập để tự động sinh ra các mục tiêu calorie hay macro mang tính áp đặt/kê đơn y tế.

#### Danh mục Use Cases & User Stories
- **Các Use Case con:**
  - `UC-38.1`: Xác nhận đủ điều kiện và phạm vi sử dụng chức năng dinh dưỡng (Confirm nutrition eligibility and scope). *(Bao gồm nhánh xác nhận INELIGIBLE; Hủy/rời màn hình không phải xác nhận)*
  - `UC-38.2`: Xem lại tuyên bố miễn trừ y tế và phạm vi hỗ trợ dinh dưỡng (View nutrition disclaimer and eligibility scope).
  - `UC-38.3`: Cập nhật lại trạng thái điều kiện sức khỏe dinh dưỡng (Update nutrition eligibility status).
  *(Ghi chú: Xử lý trường hợp người dùng không thuộc đối tượng hỗ trợ không còn là Use Case độc lập vì đây không phải là mục tiêu chủ động của Actor mà là Luồng rẽ nhánh an toàn thuộc UC-38.1).*
- **User Stories:**
  - `US-38.1`: Là một thành viên muốn theo dõi dinh dưỡng món ăn, tôi muốn thấy rõ điều kiện sử dụng và ranh giới y tế trước khi khai báo dữ liệu sức khỏe để có thể đưa ra quyết định phù hợp.
  - `US-38.2`: Là một thành viên đủ điều kiện (từ 18 tuổi, không mang thai/cho con bú, không ăn kiêng bệnh lý), tôi muốn xác nhận nhanh chóng để bắt đầu thiết lập hồ sơ dinh dưỡng và nhận phân tích thực đơn.
  - `US-38.3`: Là một thành viên không thuộc nhóm hỗ trợ (dưới 18 tuổi, đang mang thai, hoặc có bệnh lý điều trị), tôi muốn hệ thống giải thích rõ lý do an toàn sức khỏe và vẫn cho phép tôi sử dụng bình thường các tính năng khác của ứng dụng.
  - `US-38.4`: Là một thành viên có tình trạng sức khỏe thay đổi (ví dụ: bắt đầu mang thai), tôi muốn có thể cập nhật lại xác nhận này để hệ thống tạm dừng các khuyến nghị dinh dưỡng không còn an toàn.

#### Quy tắc phân quyền và bảo mật (Permissions & Security)
- Khách chưa đăng nhập (Guest) không có quyền truy cập chức năng dinh dưỡng (BR-05).
- Việc xác nhận đủ điều kiện là bắt buộc đối với mọi Member trước khi hệ thống lưu trữ hoặc xử lý bất kỳ dữ liệu nhân trắc học hay phân tích dinh dưỡng nào (NFR-20).
- Trạng thái hiện tại gồm `NOT_CONFIRMED`, `ELIGIBLE`, `INELIGIBLE`. Ban đầu là `NOT_CONFIRMED`, thời điểm xác nhận là `NULL`. Chỉ `ELIGIBLE` được truy cập dữ liệu dinh dưỡng cá nhân; `NOT_CONFIRMED` và `INELIGIBLE` bị chặn ở Backend, kể cả khi gọi API trực tiếp. Endpoint xác nhận vẫn cho phép Member `INELIGIBLE` xác nhận lại; không cần Admin phê duyệt.
- Máy chủ bắt buộc kiểm tra trạng thái `ELIGIBLE` trước khi xử lý các chức năng dinh dưỡng cá nhân thuộc FR-35, FR-36 và FR-37. Khi `INELIGIBLE`, cả quyền đọc dữ liệu/lịch sử dinh dưỡng cá nhân cũng bị khóa; dữ liệu trong Database được giữ nguyên. Khi xác nhận lại thành `ELIGIBLE`, chỉ khôi phục quyền theo phân quyền hiện hành, không tự động tính lại dữ liệu cũ.
- Member chỉ cập nhật trạng thái qua thao tác xác nhận chủ động. Backend lưu trạng thái và thời điểm xác nhận thành công gần nhất trong cùng giao dịch. Hủy, quay lại, đóng trang/thoát mà chưa xác nhận, dữ liệu không hợp lệ hoặc lỗi xử lý không được thay đổi Database hay timestamp trước đó. Không lưu nháp, tự động lưu hoặc lịch sử xác nhận. Timestamp eligibility độc lập với `health_data_consent_at` của FR-35.
- Xác nhận eligibility là hợp đồng riêng với thao tác lưu hồ sơ FR-35; dữ liệu hồ sơ và đồng thuận xử lý dữ liệu sức khỏe tiếp tục theo FR-35.
- Ràng buộc kỹ thuật được phê duyệt: Dữ liệu tính toán lấy từ danh mục nội bộ; nguồn/cách ước tính được nhập tự do và có thể là AI-generated chưa xác minh. USDA FoodData Central và NIH DRI chỉ là nguồn tham khảo tùy chọn; tuyệt đối không gọi runtime API bên thứ ba khi tính toán dinh dưỡng (BR-49, BR-52).

#### Truy vết quy tắc nghiệp vụ và phi chức năng (Traceability)
- **Quy tắc nghiệp vụ liên quan:**
  - [BR-39](BUSINESS-RULES.md#br-39): Vai trò tham khảo của chỉ số BMI (không tự kê calorie/macro từ BMI hoặc mục tiêu cân nặng).
  - [BR-41](BUSINESS-RULES.md#br-41): Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia y tế.
  - [BR-42](BUSINESS-RULES.md#br-42): Đối tượng loại trừ khỏi tính toán nhu cầu dinh dưỡng MVP (người dưới 18 tuổi, mang thai/cho con bú, bệnh lý điều trị; không khóa chức năng thông thường).
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân & chỉ số sức khỏe, HTTPS/TLS 1.2+, kiểm tra quyền sở hữu.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.
  - [NFR-20](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20): Cơ chế đồng ý (Consent) trước khi thu thập/xử lý dữ liệu sức khỏe, ranh giới thông tin rõ ràng.

#### Tiêu chí nghiệm thu chi tiết (Acceptance Criteria)

- **AC-38.1 — Bắt buộc hiển thị xác nhận khi truy cập chức năng dinh dưỡng lần đầu:**
  - **Given:** Member đã đăng nhập và có trạng thái `NOT_CONFIRMED`.
  - **When:** Member nhấn vào tính năng "Hồ sơ dinh dưỡng" hoặc "Kiểm tra dinh dưỡng thực đơn ngày".
  - **Then:** Backend từ chối truy xuất hoặc xử lý dữ liệu dinh dưỡng cá nhân; giao diện bắt buộc hiển thị màn hình "Xác nhận phạm vi hỗ trợ dinh dưỡng" với ba điều kiện và tuyên bố ranh giới y tế tham khảo (BR-41, BR-42).

- **AC-38.2 — Xác nhận đủ điều kiện thành công mở khóa tính năng dinh dưỡng:**
  - **Given:** Member đang ở màn hình xác nhận phạm vi hỗ trợ dinh dưỡng.
  - **When:** Member tích chọn một ô cam kết đáp ứng đủ ba tiêu chí (từ 18 tuổi, không mang thai/cho con bú, không cần chế độ ăn điều trị) và nhấn "Xác nhận và tiếp tục".
  - **Then:** Backend lưu `ELIGIBLE` và thời điểm xác nhận gần nhất trong cùng giao dịch, trả thành công; giao diện chuyển Member sang màn hình Khai báo Hồ sơ dinh dưỡng cá nhân (FR-35). Thao tác này độc lập với lần lưu hồ sơ và `health_data_consent_at`.
  - **Quy tắc không lưu:** Chỉ gửi yêu cầu cập nhật khi Member chủ động nhấn xác nhận. Hủy, quay lại, đóng màn hình/thoát trang hoặc chỉ tick mà chưa xác nhận không lưu trạng thái nào; không có lưu nháp hay tự lưu. Validation thất bại hoặc Backend lỗi phải giữ nguyên trạng thái và timestamp cũ.

- **AC-38.3 — Xử lý an toàn khi người dùng xác nhận không đủ điều kiện:**
  - **Given:** Member đang ở màn hình xác nhận phạm vi hỗ trợ dinh dưỡng.
  - **When:** Member chủ động xác nhận lựa chọn không thuộc nhóm hỗ trợ.
  - **Then:** Backend lưu `INELIGIBLE` và thời điểm xác nhận trong cùng giao dịch; hệ thống giải thích lý do an toàn và chặn dinh dưỡng. Đây là trạng thái đã xác nhận, khác với Hủy/quay lại/đóng trang (không lưu). Member được xác nhận lại bất cứ lúc nào, không cần Admin; chức năng không-dinh-dưỡng vẫn dùng bình thường.

- **AC-38.4 — Bảo toàn 100% tính năng không-dinh-dưỡng cho người không đủ điều kiện:**
  - **Given:** Member đã được ghi nhận trạng thái không đủ điều kiện dinh dưỡng.
  - **When:** Member thực hiện các thao tác: tìm kiếm món chay, xem chi tiết bài công thức, lưu công thức vào `Saved Recipes`, thêm công thức vào Lịch ăn thủ công và bình luận; nếu là Chuyên gia thì tạo và công khai Recipe Post.
  - **Then:** Toàn bộ các thao tác trên được hệ thống xử lý thành công 100% mà không gặp bất kỳ thông báo lỗi hay sự hạn chế nào liên quan đến dinh dưỡng (BR-42).

- **AC-38.5 — Chặn ở tầng kiểm soát máy chủ đối với các chức năng dinh dưỡng khi chưa xác nhận:**
  - **Given:** Member có trạng thái `NOT_CONFIRMED` hoặc `INELIGIBLE`.
  - **When:** Ứng dụng hoặc client gọi trực tiếp API đọc/ghi/tính toán dữ liệu dinh dưỡng cá nhân thuộc FR-35, FR-36 hoặc FR-37.
  - **Then:** Backend từ chối trước khi trả hoặc xử lý dữ liệu cá nhân; chỉ `ELIGIBLE` được đi tiếp theo phân quyền/nghiệp vụ của FR sở hữu endpoint. API xác nhận eligibility vẫn truy cập được để Member `INELIGIBLE` xác nhận lại. Chỉ nghiệm thu guard cho endpoint đã tồn tại và được tích hợp; endpoint FR-36/FR-37 chưa có phải ghi dependency, chưa xác minh.

- **AC-38.6 — Cập nhật điều kiện sức khỏe sang không đủ điều kiện tạm dừng tính năng dinh dưỡng:**
  - **Given:** Member có trạng thái `ELIGIBLE`.
  - **When:** Member chủ động xác nhận cập nhật thành `INELIGIBLE` (ví dụ tình trạng sức khỏe thay đổi) và Backend xử lý thành công.
  - **Then:** Backend cập nhật trạng thái và timestamp gần nhất nguyên tử; mọi chức năng dinh dưỡng cá nhân bị khóa ngay, kể cả đọc hồ sơ/lịch sử. Dữ liệu lịch sử được giữ nguyên trong Database. Khi xác nhận lại thành `ELIGIBLE`, quyền đọc được khôi phục nhưng không tự động tính lại dữ liệu cũ. Nếu hủy, dữ liệu không hợp lệ hoặc xử lý lỗi, trạng thái và timestamp cũ không đổi.

- **AC-38.7 — Kiểm soát truy cập và xác minh ranh giới BMI khi tích hợp FR-35:**
  - **Given:** Member truy cập Hồ sơ dinh dưỡng FR-35 thông qua luồng FR-38.
  - **When:** Member `NOT_CONFIRMED`/`INELIGIBLE` gọi FR-35, Member xác nhận `ELIGIBLE`, hoặc Member `ELIGIBLE` truy cập theo phân quyền hiện hành.
  - **Then:** `NOT_CONFIRMED` và `INELIGIBLE` bị Backend chặn; `INELIGIBLE` vẫn được xác nhận lại; xác nhận `ELIGIBLE` thành công lưu trạng thái/timestamp và giao diện chuyển sang FR-35; `ELIGIBLE` được qua guard. Khi FR-35 sẵn sàng, kiểm thử tích hợp phải xác minh BMI có nhãn tham khảo và không tự sinh chỉ định calorie/macro điều trị chỉ từ BMI (BR-39). FR-38 không triển khai hoặc sửa UI, thuật toán BMI, công thức năng lượng hay macro của FR-35. Nếu chưa có bằng chứng FR-35, phần BMI ghi dependency/chưa xác minh, không đánh dấu AC-38.7 hoàn tất.

---

---

<a id="fr-39"></a>
### FR-39 — Tính toán ước tính 9 chỉ tiêu dinh dưỡng cho công thức

- **Mã yêu cầu:** FR-39
- **Module:** M03, M04, M10

- **Use Case detail:** [Open interaction flows](../use-cases/nutrition.md#fr-39).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống tính chín chỉ tiêu từ các giá trị hiện có trong danh mục nguyên liệu nội bộ theo trọng lượng gram thực tế của từng nguyên liệu thuộc catalog chuẩn FR-18, rồi quy đổi ra một khẩu phần theo số khẩu phần tác giả khai báo. USDA/NIH chỉ là nguồn tham khảo tùy chọn; dữ liệu do AI ước tính được chấp nhận nếu ghi rõ chưa xác minh. Mỗi chỉ tiêu `NULL` được bỏ riêng khỏi phép tính, không đổi thành `0`; khi thiếu nguyên liệu hoặc chỉ tiêu, hệ thống công khai phạm vi thiếu và đánh dấu kết quả chưa đầy đủ. Không gọi API dinh dưỡng ngoài theo thời gian thực hoặc tự suy diễn dữ liệu lúc người dùng thao tác.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Tất cả các bài Recipe Post được Chuyên gia tạo hoặc chỉnh sửa trong hệ thống (FR-04, FR-16, FR-19, FR-44).
  - 9 chỉ tiêu cốt lõi: Năng lượng (kcal), Chất đạm (g), Carbohydrate (g), Chất béo (g), Chất xơ (g), Canxi (mg), Sắt (mg), Vitamin B12 (mcg), Kẽm (mg) (SRS 3.18, BR-40, BR-46).
- **Phân loại Actor:**
  - Primary Actor: `Member / Tác giả bài viết` (khi tạo/sửa công thức), `Guest / Member` (khi xem chi tiết công thức).
  - Supporting Actor: `Cơ sở dữ liệu dinh dưỡng nội bộ` (lưu trữ 9 chỉ số chuẩn trên 100g).

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-39.1`: Xem bảng ước tính 9 chỉ tiêu dinh dưỡng của công thức (tính trên 1 khẩu phần và trên toàn bộ công thức).
  - `UC-39.2`: Xem cảnh báo danh sách các nguyên liệu chưa hỗ trợ tính dinh dưỡng trong công thức (nếu có).
- **User Stories:**
  - *Là một người tìm kiếm công thức nấu ăn chay*, tôi muốn biết rõ lượng dinh dưỡng ước tính trong mỗi phần ăn để cân đối khẩu phần cho bản thân, đồng thời biết rõ nguyên liệu nào chưa có dữ liệu dinh dưỡng để không bị lầm tưởng con số là chính xác tuyệt đối 100%.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Mọi người dùng (Guest, Member, Admin) đều có quyền xem bảng dinh dưỡng ước tính trên các công thức công khai.
- **Ràng buộc nghiệp vụ:** Không hiển thị điểm số tổng hợp (BR-45); ghi rõ nguồn/cách ước tính đã lưu, gắn nhãn AI chưa xác minh khi áp dụng, và hiển thị từ chối trách nhiệm y tế (BR-39, BR-41, BR-52).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-14](BUSINESS-RULES.md#br-14): Quy tắc định lượng và tổng hợp Shopping List an toàn.
  - [BR-39](BUSINESS-RULES.md#br-39): Vai trò tham khảo của chỉ số BMI.
  - [BR-40](BUSINESS-RULES.md#br-40): Điều kiện dữ liệu dinh dưỡng tin cậy cho AI menu.
  - [BR-41](BUSINESS-RULES.md#br-41): Ranh giới thông tin dinh dưỡng và không thay thế chuyên gia.
  - [BR-43](BUSINESS-RULES.md#br-43): Gắn số liệu dinh dưỡng với một khẩu phần.
  - [BR-44](BUSINESS-RULES.md#br-44): Quy tắc diễn giải mức tham khảo theo từng chỉ tiêu dinh dưỡng.
  - [BR-45](BUSINESS-RULES.md#br-45): Cấm hiển thị điểm tổng hợp hoặc nhãn đơn giản hóa.
  - [BR-46](BUSINESS-RULES.md#br-46): Tính dinh dưỡng từ các chỉ tiêu hiện có trong catalog.
  - [BR-48](BUSINESS-RULES.md#br-48): Xử lý nguyên liệu thiếu định lượng hoặc thiếu số liệu dinh dưỡng.
  - [BR-49](BUSINESS-RULES.md#br-49): Không gọi API dinh dưỡng ngoài khi chạy ứng dụng.
  - [BR-50](BUSINESS-RULES.md#br-50): Cho phép công khai bài thiếu dữ liệu dinh dưỡng.
  - [BR-73](BUSINESS-RULES.md#br-73): Quy tắc chuẩn hóa đơn vị đo và chuyển đổi đơn vị nguyên liệu.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian tải trang hiển thị chi tiết bài viết $\le 2$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Bảo vệ hệ thống khỏi lỗ hổng (không gọi API ngoài không kiểm soát).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-39.1 (Tính toán chính xác theo gram nguyên liệu và khẩu phần qua quy đổi đơn vị):**
  - *Given* công thức có khai báo 2 khẩu phần và nguyên liệu gồm "2 bìa Đậu phụ mơ", với quy tắc quy đổi trong `INGREDIENT_UNIT_CONVERSION` là 1 bìa $= 150\text{g}$ (tổng trọng lượng quy đổi $= 300\text{g}$) và chỉ số protein trong danh mục nội bộ là $8\text{g} / 100\text{g}$,
  - *When* hệ thống thực hiện tính toán dinh dưỡng cho công thức,
  - *Then* lượng protein trên 1 khẩu phần được hiển thị chính xác là $12\text{g}$ ($\frac{300 \times 8}{100} \div 2$).
- **AC-39.2 (Đầy đủ 9 chỉ tiêu cốt lõi):**
  - *Given* công thức có các nguyên liệu đã được ánh xạ đầy đủ,
  - *When* người dùng xem bảng dinh dưỡng công thức,
  - *Then* hệ thống hiển thị đủ 9 vị trí chỉ tiêu: Năng lượng (kcal), Chất đạm (g), Carbohydrate (g), Chất béo (g), Chất xơ (g), Canxi (mg), Sắt (mg), Vitamin B12 (mcg), Kẽm (mg); chỉ tiêu chưa tính được hiển thị `Chưa đủ dữ liệu`, không thay bằng `0`.
- **AC-39.3 (Minh bạch khi có nguyên liệu thiếu số liệu):**
  - *Given* công thức chứa nguyên liệu chưa có hồ sơ dinh dưỡng hoặc có một hay nhiều chỉ tiêu dinh dưỡng `NULL`,
  - *When* bảng dinh dưỡng công thức được hiển thị,
  - *Then* hệ thống gắn nhãn cảnh báo "Ước tính chưa đầy đủ", liệt kê nguyên liệu và chỉ tiêu còn thiếu, cộng riêng từng chỉ tiêu có dữ liệu và không gán giá trị thiếu bằng 0.
- **AC-39.4 (Cấm hiển thị điểm tổng hợp sức khỏe):**
  - *Given* kết quả tính toán dinh dưỡng công thức được kết xuất,
  - *When* hiển thị trên giao diện người dùng,
  - *Then* hệ thống tuyệt đối không hiển thị bất kỳ nhãn điểm tổng hợp (như "Điểm dinh dưỡng: 85/100") hay nhãn phân loại đơn giản ("Tốt / Xấu").
- **AC-39.5 (Hoạt động hoàn toàn bằng dữ liệu nội bộ, không gọi API ngoài):**
  - *Given* người dùng truy cập xem chi tiết công thức,
  - *When* hệ thống tải và tính toán dữ liệu dinh dưỡng,
  - *Then* toàn bộ dữ liệu được truy xuất trực tiếp từ cơ sở dữ liệu nội bộ mà không tạo bất kỳ HTTP request nào ra dịch vụ dinh dưỡng bên ngoài.

---

---

<a id="fr-40"></a>
### FR-40 — Công khai Recipe Post chứa nguyên liệu chuẩn thiếu dữ liệu dinh dưỡng

- **Mã yêu cầu:** FR-40
- **Module:** M03, M04, M06, M10

- **Use Case detail:** [Open interaction flows](../use-cases/nutrition.md#fr-40).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cho phép Chuyên gia đã đăng nhập công khai trực tiếp bài công thức có các nguyên liệu đã được chọn từ catalog chuẩn FR-18 nhưng còn thiếu hồ sơ trong danh mục dinh dưỡng nội bộ; hệ thống không được chặn quyền xuất bản chỉ vì lý do thiếu dữ liệu dinh dưỡng. Các nguyên liệu thiếu hồ sơ được gắn cờ "Chưa hỗ trợ tính dinh dưỡng"; bài viết hiển thị cảnh báo kết quả dinh dưỡng chưa đầy đủ và bị loại khỏi tập hợp ứng viên cho AI lập thực đơn theo dinh dưỡng (FR-36). Yêu cầu này không cho phép nhập nguyên liệu ngoài catalog chuẩn.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Chuyên gia tạo và xuất bản công thức trực tiếp (FR-04, FR-25).
  - Phân định ranh giới: Xuất bản bài viết cộng đồng không bị phụ thuộc vào tiến độ cập nhật danh mục dinh dưỡng của Administrator (BR-07, BR-50).
- **Phân loại Actor:**
  - Primary Actor: `Chuyên gia` (tác giả bài viết).
  - Secondary Actor: `Guest / Member` (người xem công thức).
  - Supporting Actor: `Hệ thống phân loại và gắn cờ dinh dưỡng`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-40.1`: Công khai trực tiếp Recipe Post có nguyên liệu chuẩn chưa có hồ sơ dinh dưỡng nội bộ.
  - `UC-40.2`: Xem chỉ dẫn và cảnh báo về nguyên liệu chưa hỗ trợ tính dinh dưỡng trên trang chi tiết công thức.
- **User Stories:**
  - *Là một Chuyên gia*, tôi muốn công khai công thức dùng nguyên liệu đã có trong catalog chuẩn dù nguyên liệu đó chưa có hồ sơ dinh dưỡng, để dữ liệu dinh dưỡng được cảnh báo minh bạch mà không tự đặt số liệu.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chuyên gia có toàn quyền công khai bài viết của mình mà không cần phê duyệt trước của Admin (BR-07, BR-59).
- **Ràng buộc nghiệp vụ:** Không được vì thiếu dữ liệu dinh dưỡng mà ngăn cản quyền chia sẻ công thức cộng đồng (BR-50).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post.
  - [BR-40](BUSINESS-RULES.md#br-40): Điều kiện dữ liệu dinh dưỡng tin cậy cho AI menu.
  - [BR-46](BUSINESS-RULES.md#br-46): Tính dinh dưỡng từ các chỉ tiêu hiện có trong catalog.
  - [BR-48](BUSINESS-RULES.md#br-48): Xử lý nguyên liệu thiếu định lượng hoặc thiếu số liệu dinh dưỡng.
  - [BR-50](BUSINESS-RULES.md#br-50): Cho phép công khai bài thiếu dữ liệu dinh dưỡng.
  - [BR-59](BUSINESS-RULES.md#br-59): Không có hàng đợi duyệt bài trước khi công khai.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-40.1 (Không chặn xuất bản khi thiếu hồ sơ dinh dưỡng):**
  - *Given* Chuyên gia đăng công thức hợp lệ có chứa nguyên liệu đã có trong catalog chuẩn nhưng chưa có hồ sơ dinh dưỡng nội bộ,
  - *When* Chuyên gia nhấn nút "Đăng công thức",
  - *Then* hệ thống xuất bản bài công thức thành công ở trạng thái công khai ngay lập tức mà không hiển thị lỗi chặn xuất bản.
- **AC-40.2 (Hiển thị nhãn chưa hỗ trợ tính dinh dưỡng cho nguyên liệu chuẩn):**
  - *Given* bài công thức đã công khai có chứa nguyên liệu chuẩn chưa có hồ sơ dinh dưỡng,
  - *When* người dùng xem danh sách nguyên liệu trên trang chi tiết công thức,
  - *Then* cạnh tên nguyên liệu đó có gắn nhãn hoặc chú thích rõ ràng "Chưa hỗ trợ tính dinh dưỡng".
- **AC-40.3 (Loại trừ bài viết khỏi pool gợi ý AI Menu dinh dưỡng):**
  - *Given* bài công thức được gắn trạng thái "Chưa đầy đủ dữ liệu dinh dưỡng",
  - *When* hệ thống thực hiện truy vấn trích xuất ứng viên cho chức năng AI lập thực đơn theo dinh dưỡng (FR-36),
  - *Then* bài công thức này bị loại hoàn toàn khỏi kết quả truy vấn và không bao giờ xuất hiện trong gợi ý thực đơn dinh dưỡng của AI.

---

---

<a id="fr-41"></a>
### FR-41 — Administrator quản lý danh mục nguyên liệu dinh dưỡng nội bộ

- **Mã yêu cầu:** FR-41
- **Module:** M09, M10

- **Use Case detail:** [Open interaction flows](../use-cases/nutrition.md#fr-41).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp giao diện quản trị danh mục nguyên liệu dinh dưỡng. Mỗi Ingredient có thể thiếu bất kỳ chỉ tiêu nào (`NULL` là chưa biết, không phải `0`). `source_name` là văn bản tự do để ghi nguồn/cách ước tính, chấp nhận cả dữ liệu do AI tạo; USDA/NIH, URL nguồn và ngày tham khảo không bắt buộc để dùng dữ liệu trong phép tính. Administrator có thể bật `nutrition_supported = 1` khi có ít nhất một trong chín chỉ tiêu, để hệ thống tính phần có dữ liệu và đánh dấu kết quả chưa đầy đủ nếu còn `NULL`. Bản ghi không có chỉ tiêu nào vẫn không được bật tính. Trạng thái danh mục `status` độc lập với `nutrition_supported`. Giữ nguyên quyền quản trị, xem công thức đang liên kết, cấm xóa vĩnh viễn dữ liệu đã tham chiếu và cấm AI tự động ghi catalog.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Người dùng có vai trò `Administrator` (M09, M10).
  - Bảo đảm tính toàn vẹn và thẩm quyền: Chỉ con người có thẩm quyền quản trị mới được điều chỉnh dữ liệu dinh dưỡng cốt lõi (BR-51, BR-52, BR-54).
- **Phân loại Actor:**
  - Primary Actor: `Administrator`.
  - Supporting Actor: `Hệ thống quản lý dữ liệu dinh dưỡng nội bộ`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-41.1`: Tìm kiếm, xem chi tiết và lọc danh mục nguyên liệu dinh dưỡng nội bộ.
  - `UC-41.2`: Thêm mới và lưu nguyên liệu dinh dưỡng; cho phép thiếu chỉ tiêu khi chưa bật hỗ trợ tính dinh dưỡng.
  - `UC-41.3`: Chỉnh sửa thông số dinh dưỡng và thông tin nguồn của nguyên liệu hiện có.
  - `UC-41.4`: Chuyển trạng thái danh mục (`ACTIVE`/`INACTIVE`) hoặc kích hoạt lại Ingredient; trạng thái này độc lập với `nutrition_supported`.
  - `UC-41.5`: Xem danh sách các bài công thức đang sử dụng nguyên liệu dinh dưỡng.
- **User Stories:**
  - *Là một Administrator*, tôi muốn tự do nhập số liệu và ghi rõ nguồn hoặc cách ước tính, kể cả dữ liệu AI chưa xác minh, để catalog linh hoạt và hệ thống vẫn tính được các chỉ tiêu hiện có.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Độc quyền cho vai trò `Administrator` (M09, M10). Member thông thường và Guest hoàn toàn không có quyền can thiệp.
- **Ràng buộc an toàn:** Chỉ Administrator được ghi dữ liệu; nhãn AI không được trình bày như dữ liệu đã xác minh. Cấm AI tự động cập nhật catalog và cấm xóa cứng dữ liệu đã tham chiếu (BR-51, BR-53, BR-54).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-46](BUSINESS-RULES.md#br-46): Tính dinh dưỡng từ các chỉ tiêu hiện có trong catalog.
  - [BR-49](BUSINESS-RULES.md#br-49): Không gọi API dinh dưỡng ngoài khi chạy ứng dụng.
  - [BR-51](BUSINESS-RULES.md#br-51): Thẩm quyền quản lý danh mục nguyên liệu dinh dưỡng (chỉ Administrator).
  - [BR-52](BUSINESS-RULES.md#br-52): Cho phép tính các chỉ tiêu có dữ liệu và cảnh báo phần còn thiếu.
  - [BR-53](BUSINESS-RULES.md#br-53): Cấm xóa vĩnh viễn nguyên liệu dinh dưỡng đã tham chiếu.
  - [BR-54](BUSINESS-RULES.md#br-54): Cấm nhập hàng loạt tự động vào danh mục dinh dưỡng.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật (chặn can thiệp trái phép).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-41.1 (Phân quyền quản trị danh mục dinh dưỡng):**
  - *Given* người dùng không có vai trò Administrator (Member hoặc Guest),
  - *When* người dùng cố gắng truy cập hoặc thực hiện thao tác trên danh mục nguyên liệu dinh dưỡng,
  - *Then* hệ thống từ chối truy cập và trả về thông báo lỗi không có quyền.
- **AC-41.2 (Lưu và tính một phần dữ liệu dinh dưỡng):
  - *Given* Administrator tạo mới hoặc cập nhật Ingredient với `nutrition_supported = 0`,
  - *When* một hoặc nhiều chỉ tiêu chưa có dữ liệu,
  - *Then* hệ thống lưu giá trị hiện có; `NULL` không bị thay bằng `0`. Tên nguồn/cách ước tính là văn bản tự do; USDA/NIH, URL tham khảo và ngày tham khảo không bắt buộc cho phép tính; dữ liệu gắn nhãn AI được chấp nhận.
  - *Given* Ingredient đang `nutrition_supported = 0`,
  - *When* Administrator yêu cầu bật `nutrition_supported = 1`,
  - *Then* hệ thống cho phép bật khi có ít nhất một chỉ tiêu khác `NULL`; chỉ tiêu có dữ liệu được tính và chỉ tiêu `NULL` hiển thị chưa có dữ liệu, không tính bằng `0`. Nếu cả chín chỉ tiêu đều `NULL`, hệ thống từ chối bật hỗ trợ (BR-52).
- **AC-41.3 (Cấm xóa vĩnh viễn nguyên liệu đã tham chiếu):**
  - *Given* một nguyên liệu dinh dưỡng đang được liên kết trong ít nhất một bài Recipe Post,
  - *When* Administrator thực hiện thao tác xóa vĩnh viễn nguyên liệu đó,
  - *Then* hệ thống từ chối xóa, giữ nguyên toàn vẹn dữ liệu tham chiếu và yêu cầu chuyển sang thao tác ngừng hỗ trợ.
- **AC-41.4 (Chuyển trạng thái ngừng hỗ trợ an toàn):**
  - *Given* một nguyên liệu dinh dưỡng đang ở trạng thái hoạt động,
  - *When* Administrator xác nhận chuyển trạng thái sang "Ngừng hỗ trợ",
  - *Then* nguyên liệu không còn xuất hiện trong danh sách gợi ý nhập nguyên liệu mới cho người dùng nhưng các bài viết cũ vẫn giữ nguyên liên kết lịch sử.
- **AC-41.5 (Chấp nhận nhãn AI nhưng cấm AI tự ghi catalog):
  - *Given* hệ thống nhận được request từ các module AI hoặc tiến trình nền tự động,
  - *When* request cố gắng chỉnh sửa bảng dữ liệu nguyên liệu dinh dưỡng,
  - *Then* hệ thống từ chối xử lý và ghi log cảnh báo an ninh. Administrator vẫn có thể nhập thủ công dữ liệu AI-generated và ghi rõ nguồn/cách ước tính.

---

---

<a id="fr-44"></a>
### FR-44 — Chuyên gia sửa hoặc xóa Recipe Post đã công khai của chính mình

- **Mã yêu cầu:** FR-44
- **Module:** M02, M03

- **Use Case detail:** [Open interaction flows](../use-cases/identity-and-access.md#fr-44).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cho phép Chuyên gia đã đăng nhập chỉnh sửa nội dung hoặc xóa bỏ bài công thức nấu ăn đã công khai do chính mình tạo ra; khi chỉnh sửa, các thay đổi phải vượt qua toàn bộ Recipe Validation Profile chuẩn theo FR-16 (tiêu đề, khẩu phần, thời gian, loại ăn chay, thể loại món `dish_category`, từng nguyên liệu tham chiếu catalog chuẩn bắt buộc theo FR-19/BR-12, định lượng số dương và chuyển đổi đơn vị khả dụng theo BR-73, hướng dẫn chế biến linh hoạt `instructions` 10–5.000 ký tự theo BR-19, 0–5 ảnh minh họa với đúng 1 ảnh đại diện theo FR-14, BR-19) và được cập nhật công khai ngay lập tức mà không cần Administrator phê duyệt lại; trường hợp bài viết đang bị Administrator tạm ẩn do vi phạm (BR-27), tác giả không được tự ý sửa để mở lại công khai; khi xóa bài công thức, hệ thống chuyển trạng thái bài viết sang xóa mềm (Tombstone), bài viết không còn xuất hiện trên danh mục công khai hay tìm kiếm, nhưng các tham chiếu lịch sử trong Lịch ăn tuần (Meal Plan), Công thức đã lưu (Saved Recipe) và Danh sách mua sắm hiện có của người dùng khác được bảo toàn ở trạng thái hiển thị "Nội dung không còn khả dụng" để tránh phá vỡ tính toàn vẹn dữ liệu.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Chuyên gia sở hữu bài công thức (`Role = EXPERT`, `Author`).
  - Phân quyền: Nghiêm cấm sửa hoặc xóa bài của tác giả khác (RBAC, chống IDOR theo NFR-09, NFR-10).
- **Phân loại Actor:**
  - Primary Actor: `Chuyên gia` (`Role = EXPERT`) (tác giả bài viết).
  - Supporting Actor: `Hệ thống kiểm tra tính toàn vẹn dữ liệu`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-44.1`: Chỉnh sửa nội dung bài công thức đã công khai của chính mình.
  - `UC-44.2`: Xóa bài công thức đã công khai của chính mình kèm cơ chế bảo toàn tham chiếu (Tombstone).
- **User Stories:**
  - *Là một Chuyên gia ẩm thực*, tôi muốn cập nhật lại định lượng gia vị hoặc hướng dẫn chế biến cho chính xác hơn sau khi nhận được phản hồi của bạn đọc, hoặc xóa bài viết khi không còn muốn chia sẻ mà không làm hỏng lịch ăn của những người đã lưu bài trước đó.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chỉ Chuyên gia là tác giả sở hữu bài công thức mới có quyền sửa/xóa nội dung bài viết. Administrator chỉ thực hiện hậu kiểm theo FR-06.
- **Ràng buộc an toàn:** Sửa bài công khai trực tiếp ngay (BR-07, BR-62); xóa bài bảo toàn tính toàn vẹn dữ liệu qua cơ chế Tombstone (BR-35, BR-64).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Quyền đăng và công khai Recipe Post dành riêng cho Chuyên gia (Role = EXPERT).
  - [BR-14](BUSINESS-RULES.md#br-14): Quy tắc định lượng và tổng hợp Shopping List an toàn.
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản đăng bài.
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-19](BUSINESS-RULES.md#br-19): Điều kiện bắt buộc để công khai Recipe Post (hướng dẫn chế biến linh hoạt, 0..5 ảnh với 1 ảnh bìa, tỷ lệ quy đổi đơn vị).
  - [BR-27](BUSINESS-RULES.md#br-27): Quy tắc phục hồi bài công thức bị ẩn.
  - [BR-35](BUSINESS-RULES.md#br-35): Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn.
  - [BR-59](BUSINESS-RULES.md#br-59): Không có hàng đợi duyệt bài trước khi công khai.
  - [BR-62](BUSINESS-RULES.md#br-62): Kiểm tra quyền tác giả và validation khi sửa bài công thức.
  - [BR-64](BUSINESS-RULES.md#br-64): Quyền sửa và xóa bài công thức của chính tác giả.
  - [BR-73](BUSINESS-RULES.md#br-73): Quy tắc chuẩn hóa đơn vị đo và chuyển đổi đơn vị nguyên liệu.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến (chống IDOR).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-44.1 (Quyền tác giả khi chỉnh sửa bài viết):**
  - *Given* người dùng đăng nhập không phải là tác giả của bài công thức,
  - *When* cố gắng truy cập trang chỉnh sửa hoặc gửi yêu cầu cập nhật bài công thức,
  - *Then* hệ thống từ chối thực thi và hiển thị thông báo lỗi không có quyền chỉnh sửa.
- **AC-44.2 (Công khai trực tiếp thay đổi sau validation):**
  - *Given* chính tác giả Chuyên gia chỉnh sửa và gửi nội dung cập nhật thỏa mãn đầy đủ điều kiện hợp lệ bắt buộc (bao gồm thể loại món `dish_category` và hướng dẫn chế biến `instructions` 10–5.000 ký tự),
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

---

<a id="fr-46"></a>
### FR-46 — Hiển thị và quản lý bình luận, reply lồng nhiều cấp

- **Mã yêu cầu:** FR-46
- **Module:** M01, M02, M03, M09

- **Use Case detail:** [Open interaction flows](../use-cases/discovery-and-public-content.md#fr-46).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp chức năng bình luận cộng đồng đa cấp trên bài công thức nấu ăn; cho phép Guest xem toàn bộ danh sách bình luận công khai; cho phép Member đã đăng nhập tạo bình luận gốc mới, gửi câu trả lời (Reply) lồng nhau theo cấu trúc cây phân cấp tối đa 5 cấp; Member có quyền chỉnh sửa hoặc xóa bình luận của chính mình; khi bình luận cha bị xóa nhưng vẫn có bình luận con bên dưới, hệ thống giữ nguyên vị trí phân cấp và hiển thị bản ghi thay thế (Tombstone: *"Bình luận này đã bị xóa bởi người dùng"*) để bảo toàn tính toàn vẹn của chuỗi thảo luận; Administrator có thẩm quyền quản trị ẩn hoặc xóa các bình luận vi phạm quy chuẩn cộng đồng.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi bài Recipe Post ở trạng thái công khai (`Public`).
  - Giới hạn phân cấp: Cấu trúc lồng nhau tối đa là 5 cấp (Depth $\le 5$ theo BR-66); mọi phản hồi ở cấp 5 sẽ trở thành câu trả lời đồng cấp với cấp 5 hoặc chỉ đích danh tài khoản được phản hồi.
- **Phân loại Actor:**
  - Primary Actor: `Member` (tạo, sửa, xóa bình luận/reply của chính mình).
  - Secondary Actor: `Guest` (xem bình luận công khai).
  - Administrative Actor: `Administrator` (kiểm duyệt và xử lý bình luận vi phạm).

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-46.1`: Xem danh sách bình luận và chuỗi phản hồi lồng nhau trên bài công thức.
  - `UC-46.2`: Tạo bình luận gốc mới trên bài công thức công khai.
  - `UC-46.3`: Phản hồi (Reply) bình luận hoặc reply của người khác trong giới hạn 5 cấp.
  - `UC-46.4`: Chỉnh sửa nội dung bình luận/reply của chính mình.
  - `UC-46.5`: Xóa bình luận/reply của chính mình kèm cơ chế bảo toàn Tombstone nếu có phản hồi con.
  - `UC-46.6`: Administrator xử lý gỡ bỏ bình luận vi phạm chuẩn mực cộng đồng.
- **User Stories:**
  - *Là một người nấu ăn*, tôi muốn đặt câu hỏi dưới bài công thức và trao đổi qua lại với tác giả hoặc những người khác để học hỏi kinh nghiệm nấu nướng thực tế.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member đăng bình luận trực tiếp; chỉ chính tác giả mới được sửa/xóa bình luận của mình; Admin có quyền kiểm duyệt hậu kiểm.
- **Ràng buộc cấu trúc:** Tuyệt đối không để độ sâu phân cấp vượt quá 5 cấp nhằm bảo vệ bố cục giao diện responsive trên thiết bị di động (BR-66, NFR-13).

#### Ma trận truy vết (Traceability Matrix)
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

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
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

---

<a id="fr-47"></a>
### FR-47 — Gợi ý bài công thức liên quan thông thường và tùy chọn Gemini

- **Mã yêu cầu:** FR-47
- **Module:** M04, M06

- **Use Case detail:** [Open interaction flows](../use-cases/recipe-discovery-and-classification.md#fr-47).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp tính năng gợi ý các bài công thức nấu ăn liên quan ở chân trang chi tiết Recipe Post; mặc định hệ thống hiển thị danh sách gợi ý liên quan thông thường dựa trên tiêu chí đối khớp thuộc tính tĩnh (cùng danh mục món ăn, trùng khớp ít nhất một nguyên liệu chính, cùng trường phái ăn chay) mà hoàn toàn không sử dụng trí tuệ nhân tạo (AI) và không yêu cầu gói trả phí; đồng thời cung cấp nút tùy chọn chuyên biệt "Nhờ Trợ lý AI gợi ý món ăn kết hợp hoặc biến tấu" sử dụng Google Gemini AI; chỉ khi Member chủ động bấm vào nút tùy chọn AI này, hệ thống mới gửi yêu cầu tới Gemini cho các tài khoản thuộc gói Pro (FR-10, BR-02, BR-03); các gợi ý của AI cũng chỉ được chọn lọc từ các Recipe Post đang công khai trong hệ thống, không tự bịa công thức mới.
- **Phạm vi nghiệp vụ:**
  - Gợi ý thông thường: Phục vụ cho mọi đối tượng người dùng (bao gồm Guest và Member) hoàn toàn miễn phí (BR-05).
  - Tùy chọn gọi AI: Độc quyền cho Member đã đăng nhập thuộc gói Pro còn hiệu lực (FR-10, BR-02, BR-03).
- **Phân loại Actor:**
  - Primary Actor: `Guest / Member` (xem gợi ý thông thường), `Member` (chủ động yêu cầu AI gợi ý).
  - Supporting Actor: `Hệ thống truy vấn công thức nội bộ`, `Google Gemini AI` (khi được gọi).

#### Quy tắc gợi ý và nguyên liệu chính
- Gợi ý thông thường cho phép khớp một phần: cùng `dish_category`, cùng `vegetarian_type` hoặc trùng ít nhất một nguyên liệu chính; chỉ cần một tiêu chí khớp để là ứng viên. Mỗi tiêu chí được một điểm; xếp tổng điểm giảm dần, tiếp theo là số nguyên liệu chuẩn trùng nhau giảm dần, thời điểm công khai mới nhất và ID tăng dần để ổn định kết quả.
- Nguyên liệu chính là nguyên liệu có khối lượng lớn nhất sau khi quy đổi định lượng về gram. g/kg dùng hệ số của đơn vị MASS; ml/quả và các đơn vị khác dùng bảng quy đổi gram hiện hành theo nguyên liệu/đơn vị. Nếu nhiều nguyên liệu cùng khối lượng lớn nhất, tất cả đều là nguyên liệu chính.
- Nếu một dòng thiếu khả năng quy đổi hoặc chỉ có nguyên liệu tùy chỉnh không có ID chuẩn, không suy đoán nguyên liệu chính của công thức đó; các tiêu chí thể loại và loại chay vẫn dùng được. Không so trực tiếp các số định lượng khác đơn vị và không mặc định thiếu dữ liệu bằng 0.
- Luôn loại bài nguồn, bài ẩn/xóa và kết quả trùng. Mặc định hiển thị tối đa 6 công thức; ít ứng viên thì trả số lượng thực tế, không điền món giả.
- Với tùy chọn AI, Backend chỉ gửi tập ứng viên công khai có giới hạn và cùng loại chay với bài nguồn. Gemini chọn ID trong tập này; Backend xác minh ID và đọc lại trạng thái/nội dung hiện tại trước khi hiển thị. Không có lựa chọn phù hợp thì hiển thị trạng thái rỗng.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-47.1`: Xem danh sách các bài công thức liên quan thông thường dựa trên danh mục và nguyên liệu.
  - `UC-47.2`: Yêu cầu Trợ lý AI phân tích và đề xuất món ăn biến tấu hoặc kết hợp phù hợp từ kho công thức có sẵn.
- **User Stories:**
  - *Là một người tìm kiếm món ăn*, tôi muốn xem nhanh các món ăn tương tự hoặc cùng nguyên liệu ở cuối bài để có thêm sự lựa chọn, và khi cần ý tưởng độc đáo thì có thể nhờ AI gợi ý kết hợp món ăn kèm hoàn hảo.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Gợi ý thông thường mở cho tất cả mọi người (Guest và Member). Gợi ý AI chỉ dành cho Member đã đăng nhập thuộc gói Pro.
- **Ràng buộc kỹ thuật:** Tách biệt rạch ròi giữa thuật toán lọc cơ sở dữ liệu thông thường và cuộc gọi AI; cấm tự động gọi AI khi chỉ xem trang để tránh lãng phí chi phí vận hành (BR-03).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Quyền sử dụng AI Chat cho Guest và Member Free.
  - [BR-02](BUSINESS-RULES.md#br-02): Phân quyền tính năng AI cho gói Plus và Pro.
  - [BR-03](BUSINESS-RULES.md#br-03): Xác thực quyền tính năng trước khi gọi dịch vụ AI.
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider, timeout AI và lưu trữ telemetry.
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-38](BUSINESS-RULES.md#br-38): AI không tự tạo công thức mới ngoài hệ thống.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian tải trang hiển thị chi tiết bài viết $\le 2$ giây.
  - [NFR-03](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03): Thời gian phản hồi của tính năng AI Chatbot.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-47.1 (Hiển thị gợi ý thông thường từ cơ sở dữ liệu nội bộ):**
  - *Given* người dùng truy cập trang chi tiết bài công thức nấu ăn,
  - *When* khối công thức liên quan thông thường được hiển thị,
  - *Then* hệ thống hiển thị danh sách bài viết từ cơ sở dữ liệu nội bộ mà không tạo request tới dịch vụ AI và không yêu cầu gói dịch vụ nâng cao.
- **AC-47.2 (Chỉ gọi AI khi có thao tác bấm chủ động của Member có quyền gói):**
  - *Given* Member có gói Pro đang ở trang chi tiết bài công thức,
  - *When* Member chủ động nhấn nút "Nhờ AI gợi ý món ăn kèm / biến tấu",
  - *Then* hệ thống mới bắt đầu kết nối tới Google Gemini và hiển thị kết quả gợi ý AI thành công.
- **AC-47.3 (AI chỉ đề xuất công thức có sẵn trong hệ thống):**
  - *Given* Member yêu cầu AI gợi ý món ăn kèm,
  - *When* kết quả gợi ý được trả về từ AI,
  - *Then* tất cả các món ăn được đề xuất đều có liên kết hợp lệ tới các Recipe Post đang công khai trong hệ thống, không chứa công thức giả định không tồn tại.
- **AC-47.4 (Xác thực quyền gói Pro trước khi gọi AI):**
  - *Given* Member đăng nhập bằng tài khoản gói Free hoặc Plus nhấn nút "Nhờ AI gợi ý món ăn kèm / biến tấu",
  - *When* hệ thống kiểm tra quyền tính năng (Feature Entitlement),
  - *Then* hệ thống từ chối gọi AI, hiển thị thông báo yêu cầu nâng cấp gói Pro (FR-10, BR-02, BR-03), và giữ nguyên trạng thái trang.

---

---

<a id="fr-48"></a>
### FR-48 — Báo cáo bình luận, reply và Administrator hậu kiểm

- **Mã yêu cầu:** FR-48
- **Module:** M03, M09

- **Use Case detail:** [Open interaction flows](../use-cases/recipe-contribution-and-community.md#fr-48).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp cơ chế cho phép Member đã đăng nhập gửi báo cáo vi phạm đối với các bình luận (Comment) hoặc phản hồi (Reply) trên bài công thức nấu ăn khi phát hiện nội dung vi phạm chuẩn mực cộng đồng; các lý do báo cáo được phân loại rõ ràng (ngôn từ thù ghét/xúc phạm, quảng cáo/spam, công kích cá nhân, thông tin sai lệch); bình luận và reply vẫn được công khai trực tiếp ngay khi tạo và hệ thống xử lý qua quy trình hậu kiểm (post-moderation); Administrator tiếp nhận báo cáo tại Trung tâm kiểm duyệt để thẩm định và ra quyết định xử lý; áp dụng nghiêm ngặt nguyên tắc bảo mật tuyệt đối danh tính người báo cáo (BR-28) và cơ chế chống gửi báo cáo trùng lặp đang mở trên cùng một nội dung (BR-29); việc gửi báo cáo hoàn toàn độc lập với quyết định xử lý cuối cùng của Administrator (BR-58).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Mọi bình luận và reply công khai trên hệ thống.
  - Phân quyền: Chỉ Member đã đăng nhập mới được gửi báo cáo (BR-24); Administrator có thẩm quyền duy nhất để xử lý báo cáo (BR-26).
- **Phân loại Actor:**
  - Primary Actor: `Member` (người gửi báo cáo vi phạm).
  - Administrative Actor: `Administrator` (người thẩm định và xử lý hậu kiểm).

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-48.1`: Gửi báo cáo vi phạm bình luận hoặc phản hồi với lý do và mô tả chi tiết.
  - `UC-48.2`: Administrator xem danh sách, thẩm định và đưa ra quyết định xử lý báo cáo bình luận.
- **User Stories:**
  - *Là một thành viên cộng đồng văn minh*, tôi muốn báo cáo những bình luận có lời lẽ xúc phạm hoặc quảng cáo rác để Ban quản trị xem xét và gỡ bỏ, giữ cho môi trường trao đổi ẩm thực luôn tích cực và an toàn.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Mọi Member đã đăng nhập đều có quyền gửi báo cáo. Quyền thẩm định và ra quyết định chế tài thuộc về Administrator (BR-26).
- **Ràng buộc nghiệp vụ:** Không tự ý xóa nội dung trước khi kiểm duyệt (BR-23, BR-58); bảo mật danh tính người báo cáo (BR-28).

#### Ma trận truy vết (Traceability Matrix)
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

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
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

---

<a id="fr-49"></a>
### FR-49 — Gửi thông báo trong app và email cho reply mới và kết quả báo cáo

- **Mã yêu cầu:** FR-49
- **Module:** M02, M03, M09

- **Use Case detail:** [Open interaction flows](../use-cases/identity-and-access.md#fr-49).

#### Mục đích & Phạm vi
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

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-49.1`: Nhận và xem danh sách thông báo sự kiện trong ứng dụng (In-app Notification Center).
  - `UC-49.2`: Đánh dấu thông báo là đã đọc hoặc đánh dấu đọc tất cả.
  - `UC-49.3`: Nhận email thông báo bất đồng bộ về phản hồi mới hoặc kết quả kiểm duyệt.
  - `UC-49.4`: Cấu hình tùy chọn bật/tắt nhận email thông báo trong trang quản lý tài khoản.
- **User Stories:**
  - *Là một thành viên tích cực*, tôi muốn nhận được thông báo ngay khi có ai đó trả lời bình luận của tôi để tôi có thể kịp thời phản hồi, và tôi muốn nhận email khi báo cáo vi phạm của tôi đã được Ban quản trị xử lý để biết kết quả.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member chỉ có quyền xem và thao tác trên danh sách thông báo của chính tài khoản mình (RBAC theo NFR-09).
- **Ràng buộc kỹ thuật:** Email gửi bất đồng bộ (asynchronous / best-effort); bảo mật danh tính người báo cáo là bắt buộc (BR-28).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ quyền riêng tư trong hồ sơ tác giả công khai.
  - [BR-28](BUSINESS-RULES.md#br-28): Bảo mật danh tính người báo cáo và tính riêng tư của báo cáo.
  - [BR-66](BUSINESS-RULES.md#br-66): Quy tắc liên kết và phân quyền với reply bình luận.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
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

---

<a id="fr-51"></a>
### FR-51 — AI Chatbot hỗ trợ hỏi đáp ẩm thực chay theo ngữ cảnh

- **Mã yêu cầu:** FR-51
- **Module:** M06 (AI Assistant & Personalization), M10 (Nutrition & Dietetics)

- **Use Case detail:** [Open interaction flows](../use-cases/ai-and-personalization.md#fr-51).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp **MỘT AI Chatbot duy nhất dành cho người dùng cuối** (Unified End-User AI Chatbot), tích hợp Google Gemini AI, hỗ trợ đa năng lực/hỏi đáp tùy theo câu hỏi và ngữ cảnh người dùng đang tương tác. Hệ thống không tạo nhiều chatbot riêng biệt (như Cooking Bot, Recipe Bot, Nutrition Bot, Ingredient Bot, BMI Bot); thay vào đó, cùng một AI Chatbot sẽ hỗ trợ linh hoạt dựa trên hai ngữ cảnh hoạt động chính:
  1. *Ngữ cảnh chung (General Context):* Người dùng mở chatbot từ giao diện chung của hệ thống để hỏi đáp về lối sống ăn chay, kỹ thuật nấu ăn và chế biến món chay, gợi ý nguyên liệu thay thế phù hợp với trường phái ăn chay, giải thích kiến thức dinh dưỡng thực vật, và giải thích ý nghĩa tham khảo của chỉ số BMI cùng mức năng lượng calorie (dựa trên hồ sơ dinh dưỡng nếu Member đã khai báo tại FR-35). Khi người dùng muốn tìm món hoặc công thức, chatbot ưu tiên chọn bài Recipe Post đang công khai trong danh mục và hiển thị thẻ dẫn tới bài thật; không bịa bài viết để thay kết quả tìm kiếm. Chỉ khi người dùng yêu cầu rõ ràng, chatbot mới có thể viết công thức mới hoặc biến tấu công thức.
  2. *Ngữ cảnh bài công thức (Recipe Context):* Khi người dùng đang xem một bài công thức nấu ăn cụ thể (trang Recipe Detail theo FR-20) và chủ động chọn chức năng "Hỏi AI về công thức này", hệ thống chuyển dữ liệu bài công thức hiện tại làm ngữ cảnh trực tiếp cho Chatbot FR-51. Ngữ cảnh bao gồm các dữ liệu Recipe hợp lệ: tiêu đề, thể loại món (`dish_category`), loại ăn chay, khẩu phần, thời gian chuẩn bị và nấu, danh sách nguyên liệu và định lượng (FR-19), nội dung hướng dẫn thực hiện trong trường `instructions` (10–5.000 ký tự theo FR-16), và dữ liệu dinh dưỡng khả dụng (FR-39). Người dùng có thể hỏi về bài hoặc yêu cầu rõ ràng để AI biến tấu công thức đó; nội dung biến tấu chỉ là bản nháp trong chat, không sửa bài gốc hoặc tạo bài đã đăng.
- **Người dùng và Quyền truy cập:**
  - Cả `Guest` và `Member` sử dụng **CÙNG MỘT AI Chatbot** này; không tạo bot riêng biệt theo phân quyền người dùng.
  - Sự khác biệt nằm ở cơ chế xác thực, giới hạn tần suất kỹ thuật và mức độ cá nhân hóa:
    - *Guest:* Trải nghiệm trực tiếp AI Chatbot cơ bản (FR-51) theo cơ chế giới hạn tần suất kỹ thuật (Technical Rate Limiting, tối đa 10 request / phút theo anonymous cookie + IP) để chống spam (FR-02, BR-01); được phép hỏi cả ngữ cảnh chung và ngữ cảnh bài công thức công khai; không có lịch sử hội thoại lưu theo tài khoản và không có hồ sơ dinh dưỡng cá nhân (BR-05). Guest không áp dụng daily quota theo số lượt/ngày và không có chu kỳ reset 00:00.
    - *Member (Free / Plus / Pro):* Được xác thực theo tài khoản người dùng đã đăng nhập; không áp dụng quota tính theo số lượt/ngày và không có chu kỳ reset 00:00; phân quyền tính năng tuân thủ mô hình Feature-based Entitlement (FR-10, BR-01, BR-02).
    - Dữ liệu đo lường kỹ thuật (telemetry) do provider trả về chỉ dùng để đối soát chi phí vận hành và lưu trữ tối đa 90 ngày; tuyệt đối không lưu trữ raw prompt (BR-04, FR-11).
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
    - AI rà soát và gắn cờ nội dung vi phạm nằm ngoài phạm vi baseline hiện tại của Chatbot.
    - Gợi ý bài viết liên quan trên trang chi tiết Recipe Post -> thuộc [FR-47](FUNCTIONAL-REQUIREMENTS.md#fr-47); tìm công thức trực tiếp trong hội thoại và trả thẻ bài viết thật thuộc FR-51.
- **Phân loại Actor:**
  - Primary Actor: `Guest` (người dùng chưa xác thực trải nghiệm theo FR-02), `Member` (người dùng đã đăng nhập theo FR-10).
  - Supporting Actor: `Google Gemini AI` (mô hình ngôn ngữ lớn hỗ trợ xử lý ngôn ngữ tự nhiên), `Hệ thống xác thực quyền tính năng và bảo mật rate limit`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-51.1 — Hỏi đáp AI trong ngữ cảnh chung`: Người dùng đặt câu hỏi với AI Chatbot từ giao diện chung của hệ thống. Các năng lực (capabilities) được hỗ trợ bao gồm:
    - Hỏi đáp về lối sống ăn chay, kiến thức ẩm thực và kỹ thuật chế biến món chay căn bản.
    - Khi người dùng muốn tìm món/công thức, ưu tiên truy vấn Recipe Post công khai và trả các thẻ bài viết thật từ Backend; không tạo nội dung mới để giả làm bài trong hệ thống. Nếu người dùng yêu cầu rõ viết công thức mới, AI có thể trả bản nháp trong hội thoại.
    - Giải thích ý nghĩa tham khảo của chỉ số BMI và mức phân bổ calorie theo ranh giới an toàn (kết hợp hồ sơ dinh dưỡng nếu Member đã khai báo tại FR-35).
    - Giải thích cách sử dụng, xử lý hoặc thay thế nguyên liệu mà người dùng chủ động hỏi trong phạm vi tư vấn ẩm thực; chỉ viết công thức mới khi người dùng yêu cầu rõ và nội dung chỉ là bản nháp trong chat.
  - `UC-51.2 — Hỏi đáp AI theo Recipe Post đang xem`: Người dùng đang xem một bài công thức cụ thể và chọn "Hỏi AI về công thức này". Các năng lực (capabilities) được hỗ trợ bao gồm:
    - Giải thích chi tiết các công đoạn chuẩn bị/chế biến trong nội dung hướng dẫn đang xem (kỹ thuật nấu, lưu ý khi làm, ước tính tham khảo khi công thức chưa nêu rõ).
    - Gợi ý thay thế nguyên liệu phù hợp với trường phái ăn chay của món ăn hoặc theo yêu cầu của người dùng (Vegan, Lacto, Ovo, Lacto-Ovo).
    - Giải thích thành phần nguyên liệu, thuật ngữ nấu ăn và dữ liệu dinh dưỡng sẵn có trong bài viết.
    - Chỉ biến tấu công thức khi người dùng yêu cầu rõ; nội dung biến tấu là bản nháp được gắn nhãn AI, không thay đổi Recipe Post gốc.
- **User Stories:**
  - `US-51.1` *(Hỏi đáp chung)*: Là một người tìm hiểu hoặc thực hành ăn chay, tôi muốn hỏi Chatbot về kỹ thuật nấu ăn và kiến thức dinh dưỡng thực vật căn bản để có chế độ ăn lành mạnh và tự tin vào bếp.
  - `US-51.2` *(Ngữ cảnh công thức)*: Là người đang xem một bài công thức nấu ăn cụ thể, tôi muốn chọn "Hỏi AI về công thức này" để được AI giải đáp nhanh các thắc mắc về món ăn đó (giải thích công đoạn làm, gợi ý thay thế nguyên liệu) dựa trên đúng nguyên liệu và dữ liệu của bài viết mà không phải tự gõ lại thông tin.
  - `US-51.3` *(Dinh dưỡng & BMI an toàn)*: Là một người theo dõi sức khỏe, tôi muốn hỏi AI để hiểu rõ ý nghĩa tham khảo của chỉ số BMI cá nhân và cách phân bổ calorie của bữa ăn chay mà không tiếp nhận các chỉ định y khoa sai lệch.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Cả Guest (theo FR-02) và Member (theo FR-10) đều có quyền sử dụng cùng một AI Chatbot này; Guest tuân thủ giới hạn tần suất kỹ thuật; Member được xác thực theo tài khoản và mô hình Feature-based Entitlement.
- **Ràng buộc an toàn & Toàn vẹn:** Ranh giới y tế tuyệt đối (BR-09, BR-41); cấm bịa đặt thông tin (NFR-25); AI chỉ tư vấn và không tự thực hiện thao tác ghi/sửa dữ liệu nghiệp vụ của người dùng; tuyệt đối không tạo hoặc thay đổi giá trị trong danh mục dinh dưỡng chính thức (BR-51); tuân thủ đúng `dietaryType` và `allergiesAndRestrictions`; tách biệt rõ nhãn AI với nội dung của tác giả.

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-01](BUSINESS-RULES.md#br-01): Quyền sử dụng AI Chat cho Guest và Member Free.
  - [BR-02](BUSINESS-RULES.md#br-02): Phân quyền tính năng AI cho gói Plus và Pro.
  - [BR-03](BUSINESS-RULES.md#br-03): Xác thực quyền tính năng trước khi gọi dịch vụ AI.
  - [BR-04](BUSINESS-RULES.md#br-04): Xử lý lỗi provider, timeout AI và lưu trữ telemetry.
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest (không lưu lịch sử theo tài khoản).
  - [BR-06](BUSINESS-RULES.md#br-06): Bảo mật Gemini API Key.
  - [BR-09](BUSINESS-RULES.md#br-09): Ranh giới y tế và sức khỏe của câu trả lời AI.
  - [BR-38](BUSINESS-RULES.md#br-38): Ưu tiên Recipe Post công khai trong chatbot; chỉ soạn mới/biến tấu khi người dùng yêu cầu rõ.
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

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-51.1 (Trả lời câu hỏi theo ngữ cảnh Recipe Post đang xem):**
  - *Given* người dùng (Guest hoặc Member) đang xem bài công thức công khai "Đậu hũ sốt cà chua" (khẩu phần 2 người, thời gian nấu 25 phút) và bấm "Hỏi AI về công thức này",
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
  - *Then* cả hai đều tương tác với cùng một mô hình AI Chatbot duy nhất (FR-51); Guest được bảo vệ bằng giới hạn tần suất kỹ thuật (FR-02, BR-01) và Member được xác thực theo tài khoản; hệ thống áp dụng mô hình Feature-based Entitlement không tính theo daily quota (FR-10, BR-01, BR-02).
- **AC-51.6 (Chatbot không tự ý sửa đổi Recipe Post hay dữ liệu hệ thống):**
  - *Given* người dùng trong phiên chat yêu cầu "Hãy sửa nội dung hướng dẫn của bài viết này ngắn lại" hoặc "Thêm món này vào lịch ăn của tôi",
  - *When* Chatbot xử lý,
  - *Then* Chatbot từ chối thao tác ghi dữ liệu, nêu rõ AI chỉ đóng vai trò tư vấn thông tin và hướng dẫn người dùng tự thao tác nếu họ là tác giả bài viết hoặc chủ sở hữu lịch ăn.
- **AC-51.7 (Xử lý khi dịch vụ AI gặp sự cố hoặc quá thời gian phản hồi):**
  - *Given* người dùng gửi câu hỏi nhưng dịch vụ AI bên ngoài gặp lỗi kết nối hoặc quá thời gian phản hồi quy định tại NFR-03,
  - *When* hệ thống xử lý ngoại lệ,
  - *Then* hệ thống hiển thị thông báo lỗi kỹ thuật thân thiện và ghi error log kỹ thuật (BR-04, NFR-18).
- **AC-51.8 (Hiển thị phản hồi AI cho câu hỏi hợp lệ trong ngữ cảnh chung):**
  - *Given* Guest hoặc Member hỏi Chatbot về ăn chay, kiến thức ẩm thực hoặc kỹ thuật chế biến trong ngữ cảnh chung,
  - *When* câu hỏi hợp lệ được xử lý thành công,
  - *Then* giao diện hiển thị phản hồi AI không rỗng cùng nhãn AI. Chất lượng ngữ nghĩa và độ chính xác của nội dung được đánh giá theo NFR-25; thời gian phản hồi được đo theo NFR-03.
- **AC-51.9 (Ưu tiên bài công thức thật khi người dùng tìm món):**
  - *Given* người dùng yêu cầu tìm hoặc gợi ý công thức, chẳng hạn món chay dễ nấu,
  - *When* hệ thống tạo phản hồi,
  - *Then* Backend chỉ trả về các Recipe Post đang công khai, đã lọc theo trường phái ăn chay và nguyên liệu cần tránh đã khai báo; mỗi kết quả là thẻ bài viết thật có thể mở trang công thức. Nếu không có bài phù hợp, chatbot nói rõ không tìm thấy thay vì bịa bài hoặc tự viết công thức.
- **AC-51.10 (Chỉ viết mới hoặc biến tấu khi người dùng yêu cầu rõ):**
  - *Given* người dùng hỏi tìm/gợi ý công thức mà không yêu cầu viết mới hoặc biến tấu,
  - *When* Gemini phản hồi,
  - *Then* chatbot ưu tiên bài công thức công khai; nội dung công thức mới hoặc biến tấu chỉ xuất hiện khi người dùng yêu cầu rõ, được gắn nhãn là nội dung AI và chỉ tồn tại trong hội thoại, không lưu, đăng hoặc sửa Recipe Post.

---

---

<a id="fr-53"></a>
### FR-53 — Tạo và quản lý Shopping List checklist tương tác

- **Mã yêu cầu:** FR-53
- **Module:** M05

- **Use Case detail:** [Open interaction flows](../use-cases/meal-planning-and-shopping.md#fr-53).

#### Mục đích & Phạm vi
  - **Tóm tắt yêu cầu:** Hệ thống cho phép Member đã đăng nhập tạo và quản lý Danh sách mua sắm (Shopping List) phục vụ đi chợ hoặc mua sắm nguyên liệu; Member có thể tạo danh sách rỗng độc lập với Lịch ăn và công thức, hoặc tạo nội dung từ Lịch ăn tuần (Meal Plan - FR-09), hoặc thêm từng bài công thức (Recipe Post) công khai vào một danh sách thuộc tài khoản hay tạo danh sách mới ngay trong thao tác; Member có thể đổi tên và xóa mọi danh sách thuộc tài khoản của mình; việc xóa loại bỏ checklist và dữ liệu đóng góp công thức của danh sách, không xóa Lịch ăn hoặc Recipe Post nguồn; mặt hàng catalog chỉ được hỗ trợ khi Member chọn một kết quả nguyên liệu chính xác; các phần thêm thủ công được lưu riêng với định lượng công thức để có thể gộp, xem, sửa hoặc xóa độc lập; xóa nguyên liệu gộp sẽ xóa toàn bộ đóng góp cho nguyên liệu đó khỏi danh sách; công thức chỉ được hiển thị trong danh sách khi mọi nguyên liệu và định lượng bắt buộc của từng nguồn còn đầy đủ; hiển thị checklist tương tác cho phép đánh dấu đã mua/chưa mua và lưu bền vững theo tài khoản (BR-32).
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập tài khoản hợp lệ (FR-03, BR-05, BR-32).
  - Nguồn tạo:
    1. Tạo danh sách rỗng có tên do Member nhập, độc lập với Lịch ăn và Recipe Post.
    2. Sinh từ Lịch ăn tuần (toàn bộ tuần hoặc các ngày được tích chọn trong FR-09).
      3. Thêm nguyên liệu từ một Recipe Post công khai tại mỗi lần thao tác từ menu hành động của thẻ công thức trong Khám phá (FR-17)/Công thức đã lưu (FR-32) hoặc nút trên trang chi tiết; Member chọn danh sách sở hữu hoặc tạo danh sách có tên mới cùng thao tác.
  - Tính năng quản lý danh sách: Member được đổi tên và xóa tất cả danh sách của chính mình, gồm danh sách `MANUAL`, danh sách tạo từ Lịch ăn và danh sách cá nhân từ Recipe Post. Tên mới của danh sách từ Lịch ăn được giữ nguyên, không tự đổi theo tuần.
  - Tính năng quản lý tương tác: Thêm mặt hàng thủ công ngoài công thức, sửa tổng lượng của nguyên liệu gộp nhưng giữ lượng bắt buộc tối thiểu từ công thức, sửa/xóa phần đã thêm thủ công, xóa toàn bộ một nguyên liệu gộp khỏi danh sách, tick checklist đánh dấu đã mua/chưa mua.
  - Danh sách mới lưu riêng từng lần xuất hiện của công thức, gồm công thức, khẩu phần và (nếu tạo từ Lịch ăn) ngày/bữa. Khi hiển thị, các lần cùng công thức được gom thành một thẻ và cộng tổng khẩu phần; Member có thể đổi tổng khẩu phần trong danh sách, hệ thống phân bổ lại giữa các nguồn và cập nhật nguyên liệu mà không sửa Lịch ăn hoặc bài công thức.
  - Danh sách cũ thiếu metadata nguồn tiếp tục hoạt động nhưng không suy diễn công thức/khẩu phần. Đóng góp công thức lịch sử vốn đã lưu có thể hiển thị tên món, nhưng không có điều khiển khẩu phần nếu thiếu khẩu phần gốc.
- **Phân loại Actor:**
  - Primary Actor: `Member` (người tạo và quản lý danh sách mua sắm).
  - Supporting Actor: `Hệ thống quản lý Danh sách mua sắm`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-53.1`: Tạo danh sách mua sắm rỗng, từ Lịch ăn tuần hoặc từ các công thức được chọn.
  - `UC-53.2`: Thêm mục hàng hóa mua sắm thủ công ngoài công thức, chỉnh sửa hoặc xóa mục.
  - `UC-53.3`: Đánh dấu đã mua hoặc bỏ đánh dấu các mục dạng checklist tương tác trên thiết bị.
  - `UC-53.4`: Đổi tên danh sách mua sắm thuộc tài khoản Member.
  - `UC-53.5`: Xóa danh sách mua sắm thuộc tài khoản Member và checklist của danh sách.
- **User Stories:**
  - *Là một người nội trợ*, tôi muốn tạo danh sách đi chợ rỗng theo tên mình hoặc tạo từ thực đơn tuần, tự thêm các đồ cần mua và tick chọn từng món ngay trên điện thoại.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Dành riêng cho Member đã đăng nhập (BR-05, BR-32).
- **Ràng buộc kỹ thuật:** Checklist tương tác cập nhật mượt mà; tối ưu hóa thao tác một tay trên màn hình điện thoại khi đi chợ (NFR-13).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-14](BUSINESS-RULES.md#br-14): Quy tắc định lượng và tổng hợp Shopping List an toàn.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Lịch ăn và Shopping List.
  - [BR-33](BUSINESS-RULES.md#br-33): Thao tác lưu công thức không gọi Gemini.
  - [BR-35](BUSINESS-RULES.md#br-35): Độc lập vòng đời giữa Công thức đã lưu và Lịch ăn.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến (chống XSS).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt, tối ưu trên thiết bị di động.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
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
  - *Given* một nguyên liệu gộp có thể gồm định lượng bắt buộc từ công thức đầy đủ, phần còn lại của công thức không còn đầy đủ (mất xác định) và phần Member thêm thủ công,
  - *When* Member sửa tổng số lượng hoặc xóa nguyên liệu gộp,
  - *Then* hệ thống theo dõi riêng ba loại lượng. Chỉ công thức còn đủ mọi nguyên liệu mới tạo lượng bắt buộc và đặt mức sàn; Member không thể sửa tổng thấp hơn mức này. Khi tăng tổng, phần tăng thuộc lượng Member thêm. Khi giảm tổng, hệ thống giảm lượng mất xác định trước, rồi mới giảm lượng Member thêm; không giảm lượng bắt buộc. Khi tổng nhập thấp hơn mức bắt buộc, giao diện cảnh báo ngay dưới ô số lượng và khóa thao tác lưu; không hiển thị thường trực phần phân rã lượng. Trước khi lưu, bước xác nhận nêu tên mặt hàng và phần chênh lệch tăng/giảm so với tổng hiện tại khi quy đổi được an toàn; nếu không thể quy đổi thì nêu rõ số cũ và số mới mà không đoán chênh lệch. Khi Member xác nhận, hệ thống mới thêm mặt hàng hoặc cập nhật số lượng/đơn vị, gồm cả sửa phần thêm thủ công; hủy hoặc quay lại không thay đổi dữ liệu. Xóa nguyên liệu gộp loại bỏ các lượng gắn với chính item đó; các lượng của cùng lần xuất hiện công thức ở những item còn lại chuyển thành mất xác định, không còn làm món xuất hiện hay làm tăng mức sàn. Meal Plan và Recipe Post nguồn không bị sửa.
- **AC-53.5 (Yêu cầu đăng nhập đối với danh sách mua sắm):**
  - *Given* người dùng là Guest chưa đăng nhập,
  - *When* cố gắng truy cập tính năng danh sách mua sắm,
  - *Then* hệ thống chặn truy cập và hiển thị thông báo yêu cầu đăng nhập.
  - **AC-53.6 (Thêm một Recipe Post vào danh sách mua sắm được chọn):**
    - *Given* Member mở menu hành động của một Recipe Post công khai trong Khám phá/Công thức đã lưu hoặc đang xem trang chi tiết công thức,
    - *When* Member chọn một danh sách thuộc tài khoản hoặc nhập tên hợp lệ để tạo danh sách mới, điều chỉnh khẩu phần từ 1–50, chọn các nguyên liệu muốn thêm, có thể nhập thêm mặt hàng thủ công với số lượng và đơn vị hợp lệ, rồi xác nhận,
    - *Then* chỉ các nguyên liệu được chọn được tính theo khẩu phần và các mặt hàng thủ công được thêm vào đúng danh sách; Member có thể bỏ chọn toàn bộ nguyên liệu công thức nếu đã nhập ít nhất một mặt hàng thủ công. Nếu tạo mới, danh sách và toàn bộ mặt hàng được lưu nguyên tử; thêm lại cùng công thức trong cùng danh sách không cộng trùng đóng góp nguyên liệu của công thức.
    - Guest được yêu cầu đăng nhập; Admin không có thao tác này. Công thức không còn công khai hoặc không sở hữu danh sách đích bị từ chối.
- **AC-53.7 (Đổi tên danh sách mua sắm):**
  - *Given* Member đang xem một danh sách thuộc tài khoản của mình, gồm danh sách từ Lịch ăn hoặc danh sách cá nhân,
  - *When* Member nhập tên mới không rỗng sau khi loại bỏ khoảng trắng đầu/cuối và lưu,
  - *Then* tên mới được lưu và hiển thị trong thanh danh sách cùng trang checklist; tên danh sách từ Lịch ăn không tự đổi lại theo tuần. Tên dài hơn 200 ký tự hoặc chỉ có khoảng trắng bị từ chối; danh sách của tài khoản khác không thể bị đổi tên.
- **AC-53.8 (Xóa danh sách mua sắm):**
  - *Given* Member đang xem một danh sách thuộc tài khoản của mình,
  - *When* Member xác nhận xóa danh sách,
  - *Then* danh sách, các item và dữ liệu đóng góp Recipe Post gắn với checklist bị xóa; Lịch ăn, Recipe Post nguồn và danh sách của Member khác được giữ nguyên. Nếu danh sách vừa xóa đang được chọn, hệ thống mở một danh sách còn lại hoặc hiển thị trạng thái chưa có danh sách.
- **AC-53.9 (Tạo danh sách đi chợ rỗng độc lập):**
  - *Given* Member đã đăng nhập và không có hoặc đang có các Shopping List khác,
  - *When* Member chọn tạo danh sách trống, nhập tên hợp lệ và xác nhận,
  - *Then* hệ thống tạo checklist mới thuộc Member với tên đã chuẩn hóa, không có item và không cần Meal Plan hoặc Recipe Post; Member có thể mở danh sách và thêm mặt hàng thủ công.
- **AC-53.10 (Lưu và hiển thị các món tạo danh sách):** Danh sách hiển thị các công thức nguồn thành một dãy thẻ cuộn ngang; mỗi thẻ chỉ gồm ảnh bìa, tên công thức và tổng khẩu phần, và chỉ xuất hiện khi toàn bộ nguyên liệu cùng định lượng bắt buộc của mọi lần xuất hiện còn trong danh sách. Nếu một dòng nguyên liệu bị xóa, mọi công thức phụ thuộc vào dòng đó bị ẩn; thêm cùng nguyên liệu thủ công không khôi phục liên kết công thức. Đóng góp còn lại từ lần xuất hiện không đầy đủ được phân loại riêng là mất xác định, không phải lượng Member thêm. Nếu cùng công thức xuất hiện nhiều lần trong Lịch ăn, các lần đó được gom thành một thẻ và khẩu phần được cộng lại; giao diện không hiển thị ngày hoặc bữa ăn. Chạm thẻ mở trang chi tiết công thức. Mỗi nguyên liệu có đóng góp từ công thức, mất xác định hoặc thêm thủ công có nút tròn xem chi tiết cạnh các thao tác; hộp thoại phân biệt ba loại lượng và gộp toàn bộ lượng mất xác định thành một tổng duy nhất, không liệt kê theo từng món hay lần xuất hiện. Sau khi xóa item, danh sách và thẻ món nguồn được đồng bộ ngay từ dữ liệu mới nhất. Thẻ công thức có menu ba chấm để chỉnh nhanh khẩu phần hoặc gỡ công thức khỏi danh sách.
- **AC-53.11 (Đổi tổng khẩu phần trong Shopping List):** Member có thể chỉnh tổng khẩu phần từ menu ba chấm trên thẻ món hoặc từ trang chi tiết công thức. Trước khi lưu, giao diện hiển thị xác nhận cùng danh sách từng nguyên liệu và lượng dự kiến tăng/giảm. Khi xác nhận, hệ thống phân bổ tổng mới giữa các lần xuất hiện theo tỷ lệ khẩu phần đã lưu (bước 0,1; mỗi lần xuất hiện tối thiểu 0,5), tính lại đóng góp nguyên liệu tương ứng và cập nhật lượng cần mua. Thay đổi chỉ áp dụng cho Shopping List hiện tại, không sửa Meal Plan hoặc Recipe Post. Phần Member tự thêm và lượng mất xác định không đổi; nếu một lần xuất hiện thiếu khẩu phần gốc, không cho chỉnh tổng và không suy diễn giá trị.
- **AC-53.12 (Chọn nguyên liệu catalog và gộp phần thêm thủ công):** Khi Member gõ tên mặt hàng, hệ thống hiển thị gợi ý catalog; chỉ chọn kết quả catalog mới tạo mặt hàng có ingredientId, ảnh và đơn vị được nguyên liệu hỗ trợ. Nếu Member không chọn kết quả, hệ thống lưu tên nhập như mặt hàng tùy chỉnh, không ảnh và cho chọn đơn vị chuẩn đang hoạt động. Trước khi thêm, giao diện hiển thị xác nhận với tên mặt hàng cụ thể và số lượng/đơn vị sắp thêm; chỉ xác nhận mới lưu. Khi thêm một nguyên liệu catalog đã có trong cùng danh sách với đơn vị có thể quy đổi an toàn, lượng được gộp vào dòng hiện có nhưng phần thêm được ghi riêng, hiển thị trong chi tiết và có thể sửa/xóa độc lập; nếu các đơn vị không thể quy đổi an toàn, giữ dòng riêng theo FR-54. Tên tự do trùng với tên catalog không được tự liên kết.
- **AC-53.13 (Ẩn công thức khi thiếu nguyên liệu bắt buộc):** Khi một nguyên liệu gộp bị xóa, mọi đóng góp gắn với chính item đó bị xóa và mọi công thức phụ thuộc bị ẩn khỏi khu vực món ăn. Các đóng góp ở item còn lại của cùng lần xuất hiện chuyển thành lượng mất xác định; chúng vẫn được tính vào tổng có thể mua, nhưng không còn là mức sàn bắt buộc và không khôi phục món. Khi giảm tổng nguyên liệu, hệ thống trừ lượng mất xác định trước lượng thêm thủ công; lượng bắt buộc chỉ đến từ các lần xuất hiện công thức đầy đủ. Nguyên liệu đã đánh dấu đã mua vẫn được tính là còn trong danh sách. Thêm lại nguyên liệu thủ công không tự liên kết với bất kỳ công thức nào.
- **AC-53.14 (Gỡ công thức khỏi Shopping List):** Từ menu ba chấm trên thẻ món, Member có thể gỡ toàn bộ các lần xuất hiện của công thức khỏi danh sách hiện tại. Trước khi gỡ, hộp thoại xác nhận liệt kê lượng của từng nguyên liệu sẽ giảm; nếu sau khi gỡ không còn đóng góp nào cho một nguyên liệu thì hộp thoại nêu rõ mặt hàng sẽ bị xóa khỏi danh sách. Chỉ sau khi Member xác nhận, Backend xóa các đóng góp của công thức và tính lại từng mặt hàng từ các nguồn còn lại; mặt hàng không còn nguồn bị xóa. Recipe Post, Lịch ăn và danh sách khác được giữ nguyên. Hủy xác nhận không thay đổi dữ liệu.

---

---

<a id="fr-54"></a>
### FR-54 — Tự động tổng hợp nguyên liệu trùng an toàn trong Shopping List

- **Mã yêu cầu:** FR-54
- **Module:** M05

- **Use Case detail:** [Open interaction flows](../use-cases/meal-planning-and-shopping.md#fr-54).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Shopping List chỉ tổng hợp cùng `ingredientId`. Chuẩn hóa tự động trong `MASS` (`mg/g/kg`) và `VOLUME` (`ml/L/tsp/tbsp/cup`) theo hệ số chuẩn; quy đổi khác dimension chỉ dùng conversion active của đúng nguyên liệu. Không có conversion thì giữ dòng riêng, không suy diễn. Lượng kết quả lưu đến 6 chữ số thập phân để tránh mất số lượng nhỏ. Mọi lượng phải dương; cấm "vừa đủ".
- **Phạm vi nghiệp vụ:**
  - Quy tắc tổng hợp nguyên liệu:
    1. **Quy tắc cùng bản thể:** Bắt buộc có cùng `ingredientId`.
    2. **Quy tắc quy đổi khối lượng (cùng dimension MASS):** $1.000\text{g} = 1\text{kg}$ (tự động cộng dồn và đưa về đơn vị phù hợp).
    3. **Quy tắc quy đổi thể tích (cùng dimension VOLUME):** $1.000\text{ml} = 1\text{l}$ (tự động cộng dồn và đưa về đơn vị phù hợp).
    4. **Quy tắc đơn vị đếm được (cùng dimension COUNT):** Chỉ gộp khi trùng khớp hoàn toàn tên đơn vị (ví dụ: *2 quả* + *3 quả* = *5 quả*).
    5. **Quy tắc quy đổi khác dimension:** Chỉ được phép quy đổi và gộp khi có bản ghi hợp lệ trong `INGREDIENT_UNIT_CONVERSION` đối với đúng `ingredientId`. Nếu không có bản ghi chuyển đổi, giữ tách dòng độc lập.
    6. **Cấm tuyệt đối suy diễn bất đối xứng:** Tuyệt đối không tự bịa đặt hệ số quy đổi giữa khối lượng và thể tích hoặc giữa số lượng đếm được và khối lượng khi chưa có cấu hình trong `INGREDIENT_UNIT_CONVERSION`.
    7. **Phân nhóm hiển thị:** Các dòng sau khi tổng hợp được hiển thị gom nhóm theo Danh mục thực phẩm (Rau củ, Nấm, Đậu & Chế phẩm, Ngũ cốc, Gia vị...).
- **Phân loại Actor:**
  - Primary Actor: `Member` (người cần danh sách nguyên liệu tổng hợp chính xác để đi chợ).
  - Supporting Mechanism / System: `Hệ thống tính toán và tổng hợp Shopping List` (cơ chế tính toán và gom gộp tự động).

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-54.1`: Xem danh sách nguyên liệu mua sắm được tổng hợp tự động an toàn theo đúng đơn vị đo chuẩn.
  - `UC-54.2`: Xem các dòng nguyên liệu có đơn vị không tương thích được tách dòng minh bạch và phân nhóm theo danh mục.
- **User Stories:**
  - *Là một người đi chợ*, tôi muốn danh sách mua sắm tự động cộng dồn 300g đậu phụ ở món này với 200g đậu phụ ở món khác thành 500g đậu phụ, hoặc tự gộp 1 bìa đậu phụ (150g) với 150g đậu phụ thành 300g đậu phụ theo tỷ lệ quy đổi chuẩn, nhưng không được tự ý bịa tỷ lệ đổi nếu chưa có dữ liệu chính xác để tôi mua đúng số lượng thực tế cần dùng.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Hệ thống thực thi tự động; phục vụ cho Member sở hữu danh sách mua sắm.
- **Ràng buộc an toàn:** Tuyệt đối cấm suy diễn mass $\leftrightarrow$ volume hoặc piece $\leftrightarrow$ mass/volume khi chưa có quy tắc quy đổi chính thức trong `INGREDIENT_UNIT_CONVERSION` (BR-14, BR-73).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-14](BUSINESS-RULES.md#br-14): Quy tắc định lượng và tổng hợp Shopping List an toàn.
  - [BR-48](BUSINESS-RULES.md#br-48): Xử lý nguyên liệu thiếu định lượng hoặc thiếu số liệu dinh dưỡng.
  - [BR-49](BUSINESS-RULES.md#br-49): Không gọi API dinh dưỡng ngoài khi chạy ứng dụng.
  - [BR-73](BUSINESS-RULES.md#br-73): Quy tắc chuẩn hóa đơn vị đo và chuyển đổi đơn vị nguyên liệu.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗi tính toán và lỗ hổng phần mềm.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-54.1 (Quy đổi và tổng hợp an toàn gam sang kilôgam):**
  - *Given* danh sách có Món 1 dùng 600g Đậu phụ và Món 2 dùng 800g Đậu phụ (cùng `ingredientId`),
  - *When* hệ thống thực hiện tổng hợp danh sách mua sắm,
  - *Then* hệ thống gộp thành một dòng duy nhất "Đậu phụ: 1,4 kg" (hoặc "1.400 g").
- **AC-54.2 (Quy đổi và tổng hợp an toàn mililít sang lít):**
  - *Given* danh sách có Món 1 dùng 500ml Nước cốt dừa và Món 2 dùng 750ml Nước cốt dừa,
  - *When* hệ thống thực hiện tổng hợp,
  - *Then* hệ thống gộp thành một dòng duy nhất "Nước cốt dừa: 1,25 l" (hoặc "1.250 ml").
- **AC-54.3 (Cấm tự ý quy đổi giữa khối lượng và thể tích khi không có quy tắc chuyển đổi):**
  - *Given* danh sách có Món A dùng "300g Nước tương" và Món B dùng "200ml Nước tương" và không có quy tắc chuyển đổi riêng,
  - *When* hệ thống thực hiện tổng hợp,
  - *Then* hệ thống giữ thành 2 dòng riêng biệt: "Nước tương: 300g" và "Nước tương: 200ml" mà không tự ý cộng dồn thành 500g hay 500ml.
- **AC-54.4 (Quy đổi và tổng hợp khác dimension khi có quy tắc chuyển đổi hợp lệ):**
  - *Given* danh sách có Món 1 dùng "2 bìa Đậu phụ" (với quy tắc $1\text{ bìa} = 150\text{g}$ trong `INGREDIENT_UNIT_CONVERSION`) và Món 2 dùng "300g Đậu phụ",
  - *When* hệ thống thực hiện tổng hợp,
  - *Then* hệ thống quy đổi 2 bìa thành 300g và gộp thành một dòng duy nhất "Đậu phụ: 600g".
- **AC-54.5 (Giữ tách dòng độc lập khi không có quy tắc chuyển đổi):**
  - *Given* danh sách có Món 1 dùng "2 quả Bơ" và Món 2 dùng "300g Bơ", và chưa có quy tắc quy đổi quả bơ sang gram trong `INGREDIENT_UNIT_CONVERSION`,
  - *When* hệ thống thực hiện tổng hợp danh sách mua sắm,
  - *Then* hệ thống giữ thành 2 dòng riêng biệt: "Bơ: 2 quả" và "Bơ: 300g" mà không tự ý gán tỷ lệ giả định.
- **AC-54.6 (Đổi đơn vị chuẩn khối lượng/thể tích):** `1.000 mg` tổng hợp thành `1 g`, và `2 tsp + 1 ml` được chuẩn hóa thành `11 ml`; `cup` luôn tương đương `240 ml`.
- **AC-54.7 (Giữ lượng rất nhỏ):** Khi tổng hợp `1 mg`, lượng được lưu/hiển thị không được làm tròn thành `0 g`.
- **AC-54.8 (Tổng hợp lại theo khẩu phần nguồn):** Khi khẩu phần một nguồn công thức thay đổi, chỉ đóng góp của nguồn đó được tỷ lệ lại trước khi tổng hợp; đóng góp món khác và lượng chỉnh thủ công được giữ.

---

---

<a id="fr-55"></a>
### FR-55 — Sao chép clipboard và xuất file text Shopping List

- **Mã yêu cầu:** FR-55
- **Module:** M05

- **Use Case detail:** [Open interaction flows](../use-cases/meal-planning-and-shopping.md#fr-55).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp hai chức năng kết xuất và chia sẻ dữ liệu nhanh cho Danh sách mua sắm (Shopping List) dành cho Member:
  1. **Sao chép vào Clipboard (Copy to Clipboard):** Cho phép người dùng nhấn một nút để sao chép toàn bộ danh sách mua sắm hiện tại vào khay nhớ tạm của thiết bị dưới dạng văn bản có cấu trúc phân nhóm rõ ràng, kèm trạng thái `[ ]` chưa mua hoặc `[x]` đã mua; hiển thị thông báo phản hồi thao tác thành công.
  2. **Xuất tệp văn bản thuần túy (Export as `.txt` Checklist):** Cho phép người dùng tải về tệp tin định dạng văn bản `.txt` chứa toàn bộ danh sách mua sắm dạng checklist để lưu trữ offline, gửi qua tin nhắn, hoặc in ấn ra giấy khi đi chợ.
- **Phạm vi nghiệp vụ:**
  - Áp dụng cho: Member đã đăng nhập và đang quản lý danh sách mua sắm (FR-03, FR-53).
  - Định dạng kết xuất: Văn bản UTF-8 rõ ràng, phân nhóm theo danh mục thực phẩm (Rau củ, Nấm, Đậu hạt, Gia vị...), mỗi dòng có ký hiệu trạng thái checklist (`[ ]` hoặc `[x]`), tên nguyên liệu và định lượng tổng hợp.
- **Phân loại Actor:**
  - Primary Actor: `Member` (người xuất hoặc sao chép danh sách mua sắm).
  - Supporting Actor: `Hệ thống kết xuất dữ liệu văn bản`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-55.1`: Sao chép nhanh toàn bộ nội dung danh sách mua sắm vào Clipboard thiết bị.
  - `UC-55.2`: Xuất và tải xuống tệp tin văn bản thuần túy `.txt` dạng checklist mua sắm.
- **User Stories:**
  - *Là một người nội trợ*, tôi muốn sao chép nhanh danh sách đi chợ vào Zalo gửi cho người thân hoặc tải về file text để in ra giấy khi đi siêu thị mà không cần phải mở ứng dụng liên tục.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Member sở hữu danh sách mua sắm mới có quyền xuất dữ liệu.
- **Ràng buộc kỹ thuật:** Định dạng văn bản UTF-8 chuẩn, hiển thị tốt tiếng Việt; thể hiện rõ ký hiệu `[ ]` và `[x]` (NFR-13).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
  - [BR-32](BUSINESS-RULES.md#br-32): Yêu cầu đăng nhập đối với Shopping List.
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập chức năng theo vai trò (RBAC).
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
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

---

<a id="fr-57"></a>
### FR-57 — Tương tác bình chọn Like / Dislike và hiển thị tỷ lệ % hài lòng bài công thức

- **Mã yêu cầu:** FR-57
- **Module:** M01, M02, M03

- **Use Case detail:** [Open interaction flows](../use-cases/discovery-and-public-content.md#fr-57).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Cung cấp tính năng tương tác bình chọn Thích / Không thích (Like / Dislike) độc quyền cho bài công thức nấu ăn (`RECIPE_REACTION`); chỉ cho phép Member đã đăng nhập thực hiện phản hồi (BR-05, BR-69); mỗi Member chỉ được có tối đa 1 bản ghi phản hồi hiệu lực trên một bài công thức (`UNIQUE(user_id, recipe_id)`) với cơ chế chuyển đổi qua lại giữa Like và Dislike (Switch reaction) hoặc bấm lại vào phản hồi đã chọn để hủy bỏ phản hồi (Toggle off); tác giả bài viết TUYỆT ĐỐI KHÔNG được tự bình chọn Like/Dislike cho bài viết của chính mình; khách vãng lai (Guest) chỉ có quyền xem tỷ lệ % Like và tổng số lượt bình chọn (khi Guest nhấp vào nút Like hoặc Dislike, hệ thống hiển thị thông báo yêu cầu đăng nhập theo BR-05, BR-69); hệ thống tự động tính toán tỷ lệ % Like: $\text{like\_percentage} = \operatorname{round}\left(\frac{\text{total\_likes}}{\text{total\_likes} + \text{total\_dislikes}} \times 100\right)$ và hiển thị huy hiệu `👍 {like_percentage}%` nổi bật ở góc trên bên trái ảnh bìa trên thẻ bài viết Recipe Card (theo phong cách Samsung Food); nếu bài chưa có lượt bình chọn nào thì hiển thị nhãn `Mới` thay vì `0%`.
- **Phạm vi nghiệp vụ:**
  - Loại tương tác: `LIKE` hoặc `DISLIKE` (thuộc thực thể `RECIPE_REACTION`).
  - Phân quyền: Độc quyền cho Member đã đăng nhập; Guest chỉ xem (View-only theo BR-05, BR-69).
  - Ràng buộc tác giả: Tác giả không được tự Like hoặc Dislike bài viết của chính mình (BR-69).
  - Chuyển đổi và hủy: Member có thể đổi giữa Like và Dislike, hoặc bấm lại để hủy phản hồi; hệ thống cập nhật hoặc xóa bản ghi hiệu lực, không tạo bản ghi mới.
  - Huy hiệu Samsung Food: Hiển thị `👍 {like_percentage}%` trên Recipe Card và thanh % tỷ lệ trên Recipe Detail; hiển thị nhãn `Mới` khi chưa có lượt bình chọn.
- **Phân loại Actor:**
  - Primary Actor: `Member` (người bình chọn Like/Dislike), `Guest` (người xem tỷ lệ % Like).
  - Supporting Actor: `Hệ thống tính toán tỷ lệ % và cập nhật bộ đếm`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-57.1`: Member gửi bình chọn Thích (Like) cho bài công thức.
  - `UC-57.2`: Member gửi bình chọn Không thích (Dislike) cho bài công thức.
  - `UC-57.3`: Member chuyển đổi loại phản hồi (từ Like sang Dislike hoặc ngược lại) hoặc hủy phản hồi đã chọn (Toggle off).
  - `UC-57.4`: Người dùng (Guest hoặc Member) xem tỷ lệ % Like và số lượng bình chọn trên thẻ bài viết (Samsung Food badge) và trang chi tiết công thức.
- **User Stories:**
  - *Là một Member sau khi nấu thử theo công thức*, tôi muốn nhấn Like để tán thành công thức ngon, giúp tăng tỷ lệ % hài lòng của món ăn trên thẻ bài viết cho cộng đồng thấy.
  - *Là một Guest đang tìm kiếm món ăn*, tôi muốn thấy rõ tỷ lệ % Like (ví dụ `👍 98%`) nổi bật trên ảnh thẻ món theo phong cách Samsung Food để nhanh chóng chọn được những món chay được cộng đồng đánh giá cao nhất.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Chỉ Member đã đăng nhập mới có quyền gửi bình chọn Like/Dislike (BR-05, BR-69); Guest chỉ xem tỷ lệ % (View-only).
- **Ràng buộc an toàn:** Tác giả không được tự bình chọn bài viết của chính mình; mỗi Member tối đa 1 phản hồi hiệu lực trên 1 công thức (được phép chuyển đổi hoặc hủy) (BR-69).

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest (Guest không được bình chọn).
  - [BR-17](BUSINESS-RULES.md#br-17): Gắn quyền tác giả với tài khoản đăng bài.
  - [BR-69](BUSINESS-RULES.md#br-69): Tương tác Like / Dislike và tỷ lệ hài lòng (% Like) của bài công thức.
  - [BR-71](BUSINESS-RULES.md#br-71): Quy tắc sắp xếp và xếp hạng Hoạt động sôi nổi nhất (Most Active).
  - [BR-72](BUSINESS-RULES.md#br-72): Quy tắc xếp hạng Xu hướng thịnh hành (Trending).
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian hiển thị chi tiết bài viết và tỷ lệ % Like $\le 2$ giây.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Phân quyền truy cập RBAC (chặn Guest và tác giả tự bình chọn).
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật phổ biến.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-57.1 (Member gửi phản hồi Like thành công):**
  - *Given* Member A (không phải tác giả) đang xem bài công thức công khai "Canh nấm hạt sen" có 9 lượt Like và 1 lượt Dislike (tỷ lệ 90%),
  - *When* Member A nhấp chọn nút "Like",
  - *Then* hệ thống lưu bản ghi `LIKE` của Member A vào cơ sở dữ liệu, tổng số Like tăng lên 10, tỷ lệ % Like cập nhật thành 91% (10/11 * 100 làm tròn), huy hiệu trên thẻ công thức cập nhật `👍 91%`.
- **AC-57.2 (Member chuyển đổi phản hồi từ Like sang Dislike - Switch):**
  - *Given* Member A đã từng Like bài viết "Canh nấm hạt sen",
  - *When* Member A nhấp vào nút "Dislike",
  - *Then* hệ thống cập nhật bản ghi phản hồi thành `DISLIKE`, giảm 1 Like, tăng 1 Dislike, tính toán lại tỷ lệ % Like, không tạo thêm bản ghi mới trong cơ sở dữ liệu.
- **AC-57.3 (Member bấm lại cùng nút để hủy phản hồi - Toggle off):**
  - *Given* Member A đang có phản hồi Like trên bài viết "Canh nấm hạt sen",
  - *When* Member A nhấp lại vào nút "Like",
  - *Then* hệ thống hủy phản hồi của Member A, xóa bản ghi khỏi trạng thái hiệu lực, giảm 1 Like, tính toán lại tỷ lệ % Like và đưa nút Like về trạng thái chưa kích hoạt.
- **AC-57.4 (Chặn tác giả tự bình chọn bài viết của chính mình):**
  - *Given* Member B là tác giả của bài công thức "Đậu hũ sốt cà chua",
  - *When* Member B cố gắng nhấp Like hoặc Dislike trên bài viết của chính mình,
  - *Then* hệ thống từ chối thực hiện với mã HTTP 403 Forbidden, giữ nguyên số liệu và hiển thị thông báo: "Tác giả không được tự bình chọn bài viết của chính mình" (BR-69).
- **AC-57.5 (Guest tương tác hiển thị yêu cầu đăng nhập và nhãn "Mới" cho bài chưa có vote):**
  - *Given* một bài công thức mới đăng chưa có lượt bình chọn nào (0 Like, 0 Dislike),
  - *When* người dùng xem thẻ bài viết và trang chi tiết,
  - *Then* thẻ bài viết hiển thị nhãn "Mới" (New) thay vì "0%"; khi Guest nhấn nút Like hoặc Dislike, hệ thống hiển thị thông báo yêu cầu đăng nhập tài khoản (BR-05, BR-69).

---

---

<a id="fr-58"></a>
### FR-58 — Ghi nhận lượt xem (Recipe Views) và thống kê tương tác đa chiều

- **Mã yêu cầu:** FR-58
- **Module:** M01, M03, M08

- **Use Case detail:** [Open interaction flows](../use-cases/discovery-and-public-content.md#fr-58).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống tự động ghi nhận sự kiện xem bài công thức nấu ăn (`RECIPE_VIEW`) khi người dùng (Guest hoặc Member) truy cập và tương tác trang chi tiết Recipe Post; áp dụng cơ chế khử trùng lặp (Deduplication Window) theo cửa sổ thời gian 30 phút cho mỗi phiên xem của cùng một tác nhân (dựa trên `userId` đối với Member đã đăng nhập, hoặc anonymous session cookie/fingerprint đối với Guest) để ngăn chặn hành vi spam reload/F5 tăng view ảo (BR-70); hỗ trợ tổng hợp và tính toán số lượt xem theo các khung thời gian: 24 giờ qua ($V_{24h}$), 7 ngày qua ($V_{7d}$), 30 ngày qua ($V_{30d}$), và toàn thời gian ($V_{all}$); cung cấp dữ liệu số liệu xem cho trang chi tiết công thức, bộ lọc sắp xếp Xem nhiều nhất (FR-08), thuật toán Hoạt động sôi nổi nhất (BR-71) và Xu hướng thịnh hành (BR-72).
- **Phạm vi nghiệp vụ:**
  - Ghi nhận lượt xem cho cả `Guest` và `Member`.
  - Cửa sổ khử trùng lặp (Deduplication Window): 30 phút cho mỗi cặp `(viewerId/sessionId, recipeId)` (BR-70).
  - Khung thời gian thống kê: 24 giờ qua, 7 ngày qua, 30 ngày qua, và toàn thời gian.
  - Hiển thị: Tổng số lượt xem toàn thời gian hiển thị trên Recipe Card (FR-17) và trang chi tiết (FR-20).
- **Phân loại Actor:**
  - Primary Actor: `Guest` / `Member` (người xem bài viết).
  - Supporting System: `Hệ thống ghi nhận sự kiện & tổng hợp số liệu thống kê`.

#### Use Cases & User Stories
- **Danh sách Use Cases:**
  - `UC-58.1`: Tự động ghi nhận lượt xem hợp lệ khi người dùng mở trang chi tiết bài công thức.
  - `UC-58.2`: Khử trùng lặp lượt xem trong cửa sổ 30 phút đối với cùng một người xem.
  - `UC-58.3`: Xem thống kê tổng số lượt xem toàn thời gian trên giao diện bài viết và thẻ công thức.
  - `UC-58.4`: Tổng hợp số lượt xem theo khung thời gian (24h, 7d, 30d, all-time) phục vụ sắp xếp và xếp hạng.
- **User Stories:**
  - *Là một tác giả công thức*, tôi muốn thấy số lượt xem bài viết của mình tăng lên một cách trung thực khi có bạn đọc ghé thăm để biết mức độ quan tâm của cộng đồng đối với món ăn của mình.
  - *Là một người tìm kiếm công thức*, tôi muốn lọc được những món ăn được xem nhiều nhất trong tuần qua để nấu thử các món đang được nhiều người ưa chuộng.

#### Phân quyền & Ràng buộc phê duyệt
- **Quyền hạn:** Cả Guest và Member đều được ghi nhận lượt xem khi duyệt bài.
  - **Ràng buộc an toàn:** Cửa sổ khử trùng lặp 30 phút là bắt buộc (BR-70); số lượt xem không được phép can thiệp thủ công từ phía client.

#### Ma trận truy vết (Traceability Matrix)
- **Business Rules liên quan:**
  - [BR-07](BUSINESS-RULES.md#br-07): Đăng và công khai Recipe Post trực tiếp.
  - [BR-70](BUSINESS-RULES.md#br-70): Quy tắc ghi nhận lượt xem (Recipe Views) và chống trùng lặp.
  - [BR-71](BUSINESS-RULES.md#br-71): Quy tắc sắp xếp và xếp hạng Hoạt động sôi nổi nhất (Most Active).
  - [BR-72](BUSINESS-RULES.md#br-72): Quy tắc xếp hạng Xu hướng thịnh hành (Trending).
- **Yêu cầu phi chức năng liên quan:**
  - [NFR-02](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-02): Thời gian tải và hiển thị chi tiết bài viết $\le 2$ giây.
  - [NFR-05](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-05): Khả năng chịu tải đồng thời và ghi nhận lượt xem cao tải.
  - [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
  - [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Phòng chống các lỗ hổng bảo mật và gian lận tương tác.
  - [NFR-13](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13): Giao diện Responsive tiếng Việt trên đa kích thước màn hình.

#### Tiêu chí chấp nhận nguyên tử (Acceptance Criteria)
- **AC-58.1 (Ghi nhận lượt xem hợp lệ đầu tiên):**
  - *Given* người dùng lần đầu tiên truy cập bài viết công khai "Canh chua chay",
  - *When* trang chi tiết tải hoàn tất,
  - *Then* hệ thống tạo bản ghi trong `RECIPE_VIEW`, tăng `view_count` của bài viết lên 1 đơn vị và hiển thị số lượt xem mới trên trang.
- **AC-58.2 (Khử trùng lặp lượt xem trong cửa sổ 30 phút):**
  - *Given* người dùng đã xem bài viết "Canh chua chay" vào lúc 10:00,
  - *When* người dùng tải lại trang lúc 10:15 (sau 15 phút, vẫn trong cửa sổ 30 phút),
  - *Then* hệ thống không tạo thêm bản ghi `RECIPE_VIEW` mới và giữ nguyên số `view_count` của bài viết.
- **AC-58.3 (Ghi nhận lượt xem mới sau khi hết cửa sổ 30 phút):**
  - *Given* người dùng đã xem bài viết lúc 10:00,
  - *When* người dùng quay lại xem lại bài viết lúc 10:35 (sau 35 phút, đã vượt qua ngưỡng 30 phút),
  - *Then* hệ thống ghi nhận một lượt xem mới vào `RECIPE_VIEW` và tăng `view_count` của bài viết lên 1 đơn vị.
- **AC-58.4 (Tổng hợp số lượt xem theo các khung thời gian 24h, 7d, 30d, all-time):**
  - *Given* bài công thức có nhiều lượt xem được ghi nhận rải rác theo thời gian,
  - *When* người dùng chọn bộ lọc sắp xếp "Xem nhiều nhất" theo khung thời gian (24h, 7 ngày, 30 ngày, toàn thời gian) tại FR-08,
  - *Then* hệ thống tính toán và sắp xếp danh sách bài viết dựa trên đúng tổng số lượt xem tương ứng với khung thời gian đã chọn.

---

---

<a id="fr-59"></a>
### FR-59 — Theo dõi, bỏ theo dõi và xem quan hệ theo dõi giữa các Member

- **Mã yêu cầu:** FR-59
- **Module:** M01, M02, M03

- **Use Case detail:** [Open interaction flows](../use-cases/discovery-and-public-content.md#fr-59).

#### Mục đích & Phạm vi
- **Tóm tắt yêu cầu:** Hệ thống cho phép Member đã đăng nhập theo dõi hoặc bỏ theo dõi một Member khác từ hồ sơ công khai; xem số lượng người theo dõi (`followers`) và số lượng tài khoản đang theo dõi (`following`); xem danh sách hai chiều có phân trang. Quan hệ có hướng được lưu trong `USER_FOLLOW`.
- **Trong phạm vi:** Follow, unfollow, kiểm tra trạng thái đang theo dõi, bộ đếm và danh sách followers/following.
- **Ngoài phạm vi:** News feed cá nhân hóa, tài khoản riêng tư/yêu cầu duyệt follow, block user, gợi ý tài khoản và thông báo follow.
- **Actors:** `Member` (`CUSTOMER` hoặc `EXPERT`) là tác nhân chính; `Guest` không được tạo quan hệ theo dõi. `Administrator` không dùng quyền quản trị để tạo quan hệ thay người khác.

#### Use Cases & User Stories
- `UC-59.1`: Member theo dõi một Member khác.
- `UC-59.2`: Member bỏ theo dõi một Member khác.
- `UC-59.3`: Xem trạng thái theo dõi và số lượng followers/following trên hồ sơ.
- `UC-59.4`: Member xem danh sách followers/following có phân trang.
- `US-59.1`: Là một Member, tôi muốn theo dõi người dùng khác để ghi nhận sự quan tâm tới hồ sơ và nội dung công khai của họ.
- `US-59.2`: Là một Member, tôi muốn bỏ theo dõi bất cứ lúc nào và thấy bộ đếm được cập nhật chính xác.

#### Phân quyền & Bảo mật
- Guest thao tác follow/unfollow nhận `401 Unauthorized`.
- Member tự theo dõi chính mình hoặc thao tác thay tài khoản khác bị từ chối với `403 Forbidden`.
- Tài khoản đích không tồn tại nhận `404 Not Found`; tài khoản không hoạt động không nhận quan hệ mới.
- Backend lấy `follower_user_id` từ phiên xác thực, không tin cậy ID người theo dõi do client cung cấp.

#### Traceability
- [BR-05](BUSINESS-RULES.md#br-05): Giới hạn tính năng đối với Guest.
- [BR-18](BUSINESS-RULES.md#br-18): Bảo vệ dữ liệu hồ sơ công khai.
- [BR-75](BUSINESS-RULES.md#br-75): Ràng buộc quan hệ theo dõi.
- [NFR-08](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08): Bảo vệ dữ liệu cá nhân.
- [NFR-09](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09): Authentication, authorization và chống IDOR.
- [NFR-10](NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10): Bảo vệ endpoint và kiểm tra dữ liệu đầu vào.

#### Acceptance Criteria
- **AC-59.1 — Theo dõi thành công:** Given Member A chưa theo dõi Member B và cả hai tài khoản hoạt động, When A nhấn `Theo dõi`, Then hệ thống tạo đúng một quan hệ A → B, trả `following = true` và tăng follower count của B một đơn vị.
- **AC-59.2 — Quan hệ có hướng:** Given A theo dõi B, Then B không tự động theo dõi A; B → A chỉ tồn tại nếu B chủ động theo dõi A.
- **AC-59.3 — Không tự theo dõi:** Given A đang xem hồ sơ của chính mình, When A gửi yêu cầu follow trực tiếp, Then Backend trả `403 Forbidden` và không tạo bản ghi.
- **AC-59.4 — Không trùng quan hệ:** Given A đã theo dõi B, When yêu cầu follow A → B được gửi lại, Then chỉ một bản ghi tồn tại và bộ đếm không tăng thêm.
- **AC-59.5 — Bỏ theo dõi idempotent:** Given A đang hoặc đã ngừng theo dõi B, When A gửi yêu cầu unfollow, Then hệ thống trả `following = false`, không còn quan hệ A → B và không giảm bộ đếm xuống dưới dữ liệu thực tế.
- **AC-59.6 — Danh sách và quyền riêng tư:** Given Member đã đăng nhập xem followers/following của một hồ sơ, When hệ thống trả danh sách có phân trang, Then chỉ các trường hồ sơ công khai được trả về; email, dữ liệu đăng nhập và hồ sơ dinh dưỡng không xuất hiện.
- **AC-59.7 — Tài khoản không hoạt động:** Given tài khoản B đã bị vô hiệu hóa, When A gửi yêu cầu follow B hoặc tải danh sách công khai, Then hệ thống không tạo quan hệ mới và không hiển thị B trong danh sách công khai.

---

---

<a id="fr-60"></a>
### FR-60 — So sánh hai Recipe Post công khai theo nguyên liệu và dinh dưỡng

- **Mã yêu cầu:** FR-60
- **Module:** M01, M04, M10

- **Use Case detail:** [Open interaction flows](../use-cases/discovery-and-public-content.md#fr-60).

#### Mục đích & Phạm vi

- **Tóm tắt yêu cầu:** Hệ thống cho phép Guest hoặc Member chọn đúng hai Recipe Post công khai khác nhau để xem bảng đối chiếu thông tin tổng quan, danh sách nguyên liệu và chín chỉ tiêu dinh dưỡng trên một khẩu phần. Kết quả chỉ trình bày dữ liệu và chênh lệch định lượng, không chấm điểm sức khỏe, không kết luận món nào tốt hơn và không thay thế tư vấn dinh dưỡng.
- **Trong phạm vi:** Chọn/thay hai công thức, đối chiếu thông tin tổng quan, nguyên liệu, dữ liệu dinh dưỡng trên một khẩu phần và cảnh báo thiếu dữ liệu.
- **Ngoài phạm vi:** So sánh theo 100 g thành phẩm, lưu lịch sử, xuất PDF, chia sẻ link kết quả, AI xếp hạng món tốt hơn và kết luận y tế.
- **Actors:** `Guest` và `Member` (`CUSTOMER` hoặc `EXPERT`). Administrator sử dụng chức năng như người xem dữ liệu công khai, không có quyền sửa dữ liệu từ màn hình so sánh.

#### Use Cases & User Stories

- `UC-60.1`: Bắt đầu so sánh từ Recipe Card, Recipe Detail hoặc danh sách công thức đã lưu của Member.
- `UC-60.2`: Tìm, chọn và thay Recipe Post ở một trong hai vị trí so sánh.
- `UC-60.3`: So sánh thông tin tổng quan và danh sách nguyên liệu.
- `UC-60.4`: So sánh chín chỉ tiêu dinh dưỡng trên một khẩu phần.
- `UC-60.5`: Xử lý công thức hoặc chỉ tiêu thiếu dữ liệu.
- `US-60.1`: Là một Guest đang khám phá món chay, tôi muốn đối chiếu hai công thức công khai để thấy sự khác nhau về nguyên liệu, khẩu phần và dinh dưỡng trước khi lựa chọn.
- `US-60.2`: Là một Member, tôi muốn thay nhanh một trong hai công thức đang so sánh mà không phải tạo hay lưu một lịch sử so sánh riêng.

#### Phân quyền, an toàn và traceability

- Guest và Member được xem vì chức năng chỉ sử dụng dữ liệu Recipe Post công khai; thao tác không mở rộng quyền truy cập dữ liệu riêng tư.
- Tuân thủ [BR-41](BUSINESS-RULES.md#br-41), [BR-43](BUSINESS-RULES.md#br-43), [BR-45](BUSINESS-RULES.md#br-45), [BR-46](BUSINESS-RULES.md#br-46), [BR-48](BUSINESS-RULES.md#br-48) và [BR-76](BUSINESS-RULES.md#br-76).
- Dữ liệu nguồn liên quan: FR-08 (tìm kiếm), FR-17 (Recipe Card), FR-19 (nguyên liệu), FR-20 (chi tiết thống nhất), FR-39 (chín chỉ tiêu dinh dưỡng), NFR-13 (responsive).

#### Acceptance Criteria

- **AC-60.1 — So sánh hai công thức công khai khác nhau:** Given Recipe A và Recipe B khác nhau và đều `PUBLISHED`, When người dùng chọn đủ hai bài, Then hệ thống hiển thị bảng so sánh tổng quan, nguyên liệu và dinh dưỡng.
- **AC-60.2 — Chặn chọn cùng một công thức:** Given Recipe A đã được chọn, When người dùng cố chọn lại Recipe A làm Recipe B, Then hệ thống không chấp nhận và yêu cầu chọn một bài khác.
- **AC-60.3 — So sánh theo một khẩu phần:** Given hai công thức có dữ liệu dinh dưỡng đầy đủ, When bảng dinh dưỡng hiển thị, Then chín chỉ tiêu của mỗi bên được lấy theo một khẩu phần, kèm số khẩu phần gốc và chênh lệch định lượng tuyệt đối theo cùng đơn vị.
- **AC-60.4 — Giữ nguyên nguyên liệu/đơn vị không thể quy đổi:** Given hai dòng nguyên liệu không cùng `ingredient_id` hoặc dùng đơn vị chưa có quy đổi hợp lệ, When danh sách nguyên liệu được đối chiếu, Then hệ thống không tự hợp nhất hoặc tự quy đổi và hiển thị số lượng/đơn vị nguyên bản ở từng bên.
- **AC-60.5 — Dữ liệu thiếu không trở thành số 0:** Given một chỉ tiêu của Recipe B chưa đủ dữ liệu, When bảng so sánh hiển thị, Then bên B ghi `Chưa đủ dữ liệu`, hệ thống không hiển thị `0` và không tính chênh lệch cho chỉ tiêu đó.
- **AC-60.6 — Không phán xét sức khỏe:** Given bảng so sánh đã được tạo, Then giao diện không có Natri/muối, Health Score, chênh lệch phần trăm hoặc nhãn kết luận công thức tốt hơn/lành mạnh hơn.
- **AC-60.7 — Không lưu lịch sử hoặc xuất PDF:** Given người dùng đóng giao diện so sánh, Then hệ thống không tạo bản ghi lịch sử; giao diện không cung cấp thao tác xuất kết quả so sánh ra PDF.
- **AC-60.8 — Responsive:** Given người dùng xem trên màn hình nhỏ, Then nội dung hai công thức được trình bày theo nhóm/khối dễ đối chiếu, không yêu cầu cuộn ngang toàn trang để đọc các trường bắt buộc.

---

<a id="fr-61"></a>
### FR-61 — Khám phá và theo dõi Chuyên gia ẩm thực chay

- **Mã yêu cầu:** FR-61
- **Module:** M01, M02
- **Use Case detail:** [Open interaction flows](../use-cases/discovery-and-public-content.md#fr-61).

#### Mục đích và phạm vi

Hệ thống cung cấp danh bạ công khai để Guest và Member tìm, xem hồ sơ và nội dung công khai của Chuyên gia. Danh sách chỉ bao gồm tài khoản có role `EXPERT` và trạng thái `ACTIVE`; hỗ trợ tìm theo tên hiển thị hoặc phần giới thiệu và phân trang. Thông tin danh bạ chỉ gồm tên hiển thị, avatar, bio, số người theo dõi đang hoạt động và trạng thái theo dõi của Member hiện tại.

Customer và Expert đã đăng nhập có thể theo dõi/bỏ theo dõi qua hành vi đã quy định ở FR-59. Guest chọn theo dõi được dẫn tới đăng nhập. Administrator chỉ xem và không có thao tác follow từ danh bạ. Chức năng không xếp hạng hoặc đề xuất cá nhân hóa chuyên gia.

#### Acceptance Criteria

- **AC-61.1 — Chỉ hiển thị Chuyên gia hoạt động:** Given danh sách có tài khoản Expert hoạt động, Customer và tài khoản không hoạt động, When người dùng mở danh bạ, Then chỉ Expert có trạng thái `ACTIVE` xuất hiện.
- **AC-61.2 — Tìm kiếm và phân trang:** Given danh bạ có nhiều Expert, When người dùng tìm theo chuỗi tên hiển thị/bio hoặc chuyển trang, Then kết quả được lọc, sắp xếp ổn định theo tên hiển thị và ID, đồng thời chỉ tải số bản ghi giới hạn của trang.
- **AC-61.3 — Chỉ trả thông tin công khai:** Given API trả danh sách chuyên gia, Then response chỉ chứa ID, tên hiển thị, avatar, bio, follower count và trạng thái following; không chứa email, thông tin đăng nhập, dữ liệu dinh dưỡng hoặc trường quản trị.
- **AC-61.4 — Theo dõi và bỏ theo dõi từ danh bạ:** Given Customer/Expert đã đăng nhập đang xem một Expert khác, When họ chọn Theo dõi hoặc Đang theo dõi, Then UI gọi API FR-59, hiển thị trạng thái và bộ đếm từ response Backend; lỗi không làm UI giả nhận thao tác thành công.
- **AC-61.5 — Guest và Administrator:** Given Guest hoặc Administrator xem danh bạ, When họ chọn một Expert, Then Guest được yêu cầu đăng nhập và Administrator không được thực hiện follow; Backend FR-59 vẫn là quyền quyết định cuối cùng.
- **AC-61.6 — Menu Khám phá:** Given người dùng mở điều hướng chính trên desktop hoặc mobile, Then mục Khám phá mở các lối vào Công thức, Danh mục nguyên liệu và Chuyên gia; trang Cộng đồng tiếp tục truy cập qua nút Trang chủ và URL trực tiếp.
- **AC-61.7 — Trạng thái giao diện:** Given danh bạ đang tải, lỗi, không có kết quả hoặc có kết quả, Then giao diện hiển thị trạng thái tương ứng và có thể thử lại khi lỗi; thao tác điều hướng hỗ trợ keyboard focus.

---

<a id="fr-62"></a>
### FR-62 — Xem chi tiết nguyên liệu và tìm công thức

- **Mã yêu cầu:** FR-62
- **Module:** M01, M04
- **Use Case detail:** [Open interaction flows](../use-cases/discovery-and-public-content.md#fr-62).

#### Mục đích và phạm vi

Hệ thống cung cấp trang công khai hiển thị toàn bộ nguyên liệu đang hoạt động trong danh mục chuẩn FR-18. Guest và Member có thể tìm nguyên liệu, mở trang chi tiết, xem ảnh minh họa và các chỉ tiêu dinh dưỡng có sẵn trên mỗi 100 g. Tại trang chi tiết, người dùng có thể quy đổi hai chiều giữa các đơn vị đang được hỗ trợ cho chính nguyên liệu đó; hệ thống dùng hệ số gram trên mỗi đơn vị và hiển thị cảnh báo khi dữ liệu là ước tính. Từ danh mục, người dùng chọn một hoặc nhiều nguyên liệu và chuyển tới trang Khám phá với các bộ lọc đã chọn; từ chi tiết, người dùng có thể tìm công thức theo riêng nguyên liệu đang xem. API công thức giữ nguyên quy tắc FR-08 hiện tại: kết quả phải có đủ tất cả nguyên liệu được chọn.

Trang này là cách duyệt trực tiếp từ điển nguyên liệu hiện có; không tạo taxonomy nhóm món ăn hoặc danh mục động mới. API công khai trả thông tin danh mục cần cho giao diện: ID, tên, nhóm, ảnh minh họa, các chỉ tiêu dinh dưỡng trên mỗi 100 g ở trang chi tiết và danh sách đơn vị quy đổi đang hỗ trợ gồm ID, mã, tên, nhóm đơn vị, gram trên đơn vị và cờ ước tính. API không trả tên/URL nguồn tham chiếu, ngày tham khảo, trạng thái danh mục, cờ hỗ trợ tính dinh dưỡng hay trường quản trị khác. Chỉ nguyên liệu đang hoạt động mới có thể được liệt kê và xem chi tiết.

#### Acceptance Criteria

- **AC-62.1 — Toàn bộ nguyên liệu đang hoạt động:** Given catalog có nguyên liệu ở trạng thái `ACTIVE` và `INACTIVE`, When Guest mở trang danh mục, Then tất cả và chỉ các nguyên liệu `ACTIVE` được tải để hiển thị thành thẻ nguyên liệu.
- **AC-62.2 — Tìm và chọn nhiều nguyên liệu:** Given danh mục nguyên liệu đã tải, When người dùng tìm theo tên và chọn hoặc bỏ chọn nguyên liệu từ thẻ, Then giao diện thể hiện đúng trạng thái đã chọn và hỗ trợ chọn nhiều nguyên liệu bằng chuột hoặc bàn phím.
- **AC-62.3 — Tìm công thức theo nhiều nguyên liệu:** Given người dùng chọn một hoặc nhiều nguyên liệu đang hoạt động, When chọn Tìm công thức, Then trang Khám phá mở với đúng các ingredient ID đã chọn; công thức phải thỏa tất cả nguyên liệu theo semantics AND của FR-08.
- **AC-62.4 — Duyệt danh sách nguyên liệu:** Given danh mục nguyên liệu được hiển thị, Then giao diện có thể cho biết nhóm của từng nguyên liệu nhưng không yêu cầu người dùng duyệt taxonomy nhóm món ăn để tìm hoặc chọn.
- **AC-62.5 — Hạn chế dữ liệu API công khai:** Given danh sách nhóm/nguyên liệu, chi tiết hoặc đơn vị quy đổi được trả về, Then API chỉ có group name/count; ingredient ID/name/group/image URL; dinh dưỡng trên 100 g; hoặc đơn vị được hỗ trợ gồm ID/code/name/dimension/gramsPerUnit/approximate. API không tiết lộ nguồn tham chiếu, ngày tham khảo, trạng thái nội bộ, cờ hỗ trợ tính dinh dưỡng hoặc trường quản trị khác.
- **AC-62.6 — Trạng thái giao diện:** Given danh mục đang tải, lỗi, rỗng hoặc có dữ liệu, Then UI hiển thị trạng thái tương ứng, có thể thử lại khi lỗi và các nhóm/nguyên liệu có thể thao tác bằng keyboard.
- **AC-62.7 — Xem chi tiết nguyên liệu hoạt động:** Given nguyên liệu đang `ACTIVE`, When Guest hoặc Member mở chi tiết từ danh mục, Then trang hiển thị tên, nhóm, ảnh minh họa nếu có và bảng dinh dưỡng trên mỗi 100 g; ảnh trống/tải lỗi được nhận biết là chưa có ảnh; nguyên liệu `INACTIVE` hoặc ID không tồn tại trả cùng phản hồi không tìm thấy.
- **AC-62.8 — Chỉ tiêu dinh dưỡng công khai:** Given chi tiết nguyên liệu được mở, Then bảng có thể hiển thị năng lượng, protein, carbohydrate, chất béo, chất xơ, canxi, sắt, vitamin B12 và kẽm; mỗi giá trị giữ đơn vị theo định nghĩa chỉ tiêu.
- **AC-62.9 — Dữ liệu thiếu không phải số không:** Given một hoặc nhiều chỉ tiêu dinh dưỡng là `NULL`, When bảng dinh dưỡng được hiển thị, Then từng chỉ tiêu thiếu được ghi là chưa có dữ liệu, không chuyển thành `0`; nếu tất cả đều thiếu, UI hiển thị trạng thái chưa có dữ liệu dinh dưỡng.
- **AC-62.10 — Tiếp tục tìm công thức:** Given người dùng đang xem chi tiết nguyên liệu, When chọn Tìm công thức, Then trang Khám phá mở với đúng ID nguyên liệu đó làm bộ lọc.
- **AC-62.11 — Bài công thức có nguyên liệu:** Given người dùng mở chi tiết nguyên liệu, Then trang tải và hiển thị ngay các bài công thức công khai có liên kết đúng ingredient ID đó, hỗ trợ Xem thêm, sắp xếp mới nhất/được yêu thích và mở từng bài; lỗi trang tiếp theo giữ các bài đã tải và cho thử lại đúng trang bị lỗi.
- **AC-62.12 — Nhãn lọc loại món:** Given trang chi tiết hiển thị bài công thức liên quan, Then các nhãn loại ăn chay lấy từ lựa chọn chuẩn của hệ thống và lọc đồng thời với ingredient ID; nhãn mô tả loại của công thức, không tự suy ra nguyên liệu phù hợp hoặc vi phạm một chế độ ăn khi catalog chưa có dữ liệu phân loại đó.
- **AC-62.13 — Quy đổi đơn vị theo nguyên liệu:** Given Guest hoặc Member đang xem chi tiết một nguyên liệu `ACTIVE`, When mở công cụ quy đổi, Then hai danh sách đơn vị chỉ gồm đơn vị API trả về cho nguyên liệu đó; nhập hoặc đổi đơn vị ở một phía sẽ cập nhật lượng tương đương phía kia bằng `gramsPerUnit`, giữ số lượng không âm và cho phép đổi giữa khối lượng, thể tích và số đếm khi cả hai đơn vị được hỗ trợ. Đơn vị ước tính được thông báo rõ; lỗi tải đơn vị cho phép thử lại mà không ảnh hưởng phần chi tiết nguyên liệu.

---
