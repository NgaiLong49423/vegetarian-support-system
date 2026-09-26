> **Document:** Use Case Specifications — M02
> **File:** `docs/requirements/use-cases/identity-and-access.md`
> **Version:** v2.0.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-26
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M02

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns the required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-03"></a>
## FR-03 — Đăng ký, đăng nhập và quản lý tài khoản cơ bản

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-03).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Guest`: Người dùng chưa xác thực thực hiện đăng ký tài khoản mới, xác minh email, đăng nhập hoặc yêu cầu đặt lại mật khẩu.
  - `Member` / `Administrator`: Người dùng đã đăng nhập thực hiện các thao tác trong phiên làm việc, làm mới phiên tự động hoặc đăng xuất khỏi hệ thống.
- **Secondary Actor / External System:**
  - `Google Identity Services (OAuth2/OIDC)`: Cung cấp dịch vụ xác thực tài khoản Google tập trung.
  - `Dịch vụ Email (SMTP/Email Provider)`: Chịu trách nhiệm gửi email xác minh và email đặt lại mật khẩu theo cơ chế bất đồng bộ / best-effort.

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

---

<a id="fr-04"></a>
## FR-04 — Chuyên gia tạo và công khai trực tiếp Recipe Post

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-04).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Chuyên gia` (`Role = EXPERT`): Người dùng đã được Administrator phê duyệt đơn đăng ký Chuyên gia, có toàn quyền tạo, sửa, xóa và công khai bài viết của mình.
- **Secondary Actor / External System:**
  - `Dịch vụ lưu trữ tệp đám mây (Azure Blob Storage)`: Lưu trữ ảnh minh họa bài viết.
  - `Administrator`: Quản trị viên xử lý báo cáo vi phạm sau khi bài đã công khai (hậu kiểm).
  - `Customer`: Người dùng thông thường chỉ có quyền đọc, bình chọn Like/Dislike, bình luận và lưu công thức; bị chặn khi cố gắng đăng bài.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Người dùng đã đăng nhập với tài khoản Chuyên gia (`Role = EXPERT`) ở trạng thái hoạt động (`ACTIVE`) đã được Administrator phê duyệt qua quy trình FR-05. Đối với sửa/xóa bài: người dùng phải là chính chủ tác giả của bài viết (`authenticatedUser.id == recipe.authorId`).
- **Trigger:** Chuyên gia nhấn nút "Đăng công thức" trên thanh điều hướng, hoặc nhấn "Chỉnh sửa" / "Xóa" trên trang chi tiết bài viết của mình.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Chuyên gia tạo và công khai bài công thức trực tiếp (UC-04.1)
1. **Main Flow:**
   - Bước 1: Chuyên gia nhấn "Đăng công thức". Hệ thống kiểm tra vai trò `Role == EXPERT`; giao diện hiển thị biểu mẫu tạo bài viết.
   - Bước 2: Chuyên gia nhập thông tin bắt buộc (cấu trúc và validation theo FR-16, BR-19: hướng dẫn chế biến dạng văn bản 10–5.000 ký tự hoặc chia đoạn tùy ý, người đăng không bắt buộc phải viết từng bước nấu ăn; nguyên liệu định lượng số theo FR-19 có tỷ lệ quy đổi hợp lệ theo BR-73, tải tối đa 5 ảnh lên Azure Blob Storage theo FR-14 với đúng 1 ảnh bìa, và gắn tối đa 1 link YouTube theo FR-15).
   - Bước 3: Chuyên gia nhấn "Công khai bài viết".
   - Bước 4: Hệ thống thực thi kiểm tra tính hợp lệ toàn bộ dữ liệu (validation rules theo BR-19, BR-73, FR-16, FR-25). Toàn bộ dữ liệu đạt chuẩn.
   - Bước 5: Hệ thống tự động gán mã định danh tác giả từ phiên đăng nhập (BR-17), gán nhãn Chuyên gia, lưu bài viết ở trạng thái công khai (`PUBLISHED`), và phản hồi thành công.
   - Bước 6: Bài viết lập tức hiển thị trên trang cá nhân của Chuyên gia, trang chủ và kết quả tìm kiếm cộng đồng.

##### B. Luồng Chỉnh sửa bài công thức của chính mình (UC-04.2)
1. **Main Flow:**
   - Bước 1: Chuyên gia mở bài công thức do mình sở hữu và nhấn "Chỉnh sửa bài viết".
   - Bước 2: Giao diện hiển thị biểu mẫu với toàn bộ thông tin hiện tại của bài viết (gồm hướng dẫn chế biến, ảnh media, nguyên liệu).
   - Bước 3: Tác giả thay đổi thông tin cần cập nhật và nhấn "Lưu thay đổi".
   - Bước 4: Hệ thống kiểm tra quyền sở hữu của người gọi ở tầng máy chủ (Ownership Check theo BR-64).
   - Bước 5: Hệ thống kiểm tra tính hợp lệ của dữ liệu mới (hướng dẫn 10–5.000 ký tự, ảnh có cover, nguyên liệu có tỷ lệ quy đổi), cập nhật bài viết trong cơ sở dữ liệu và phản hồi thành công.

##### C. Luồng Xóa bài công thức của chính mình (UC-04.3)
1. **Main Flow:**
   - Bước 1: Chuyên gia mở bài công thức do mình sở hữu và nhấn "Xóa bài viết".
   - Bước 2: Hệ thống hiển thị hộp thoại xác nhận an toàn: "Bạn có chắc chắn muốn xóa bài công thức này? Thao tác này không thể hoàn tác."
   - Bước 3: Chuyên gia xác nhận đồng ý xóa.
   - Bước 4: Hệ thống kiểm tra quyền sở hữu tác giả ở tầng máy chủ.
   - Bước 5: Hệ thống đánh dấu trạng thái bài viết là không khả dụng/đã xóa (tombstone pattern); gỡ bài viết khỏi danh sách công khai, kết quả tìm kiếm và các gợi ý AI; giữ lại bản ghi tham chiếu cho các Lịch ăn hoặc Danh sách đã lưu của người dùng khác ở trạng thái không khả dụng (BR-32, BR-33, BR-64).

##### D. Luồng An ninh & Xử lý ngoại lệ (Security Flow)
1. **Chặn tài khoản không có vai trò Chuyên gia tạo bài viết:**
   - Nếu tài khoản có vai trò `CUSTOMER` hoặc `ADMIN` cố tình truy cập URL đăng bài hoặc gửi request `POST /api/v1/recipes` tới máy chủ:
   - Bộ lọc phân quyền (Spring Security) phát hiện người gọi không sở hữu vai trò `ROLE_EXPERT`.
   - Máy chủ lập tức từ chối yêu cầu và phản hồi mã lỗi `HTTP 403 Forbidden` kèm thông điệp: "Chức năng đăng bài chỉ dành riêng cho Chuyên gia ẩm thực đã được xác minh".
2. **Chặn sửa/xóa bài viết của người khác:**
   - Nếu một Chuyên gia khác cố tình gửi yêu cầu sửa hoặc xóa bài viết không thuộc quyền sở hữu của mình:
   - Máy chủ so khớp định danh người gọi với định danh tác giả của bài viết.
   - Phát hiện không trùng khớp -> Máy chủ từ chối yêu cầu và phản hồi mã lỗi `HTTP 403 Forbidden` (BR-64).
3. **Validation Error Flow:**
   - Nếu bài viết thiếu hướng dẫn chế biến hoặc không đạt 10–5.000 ký tự, hoặc upload ảnh mà không chọn ảnh bìa, hoặc nguyên liệu chưa có tỷ lệ quy đổi sang gam trong `INGREDIENT_UNIT_CONVERSION`, hệ thống từ chối lưu bài viết, trả về thông báo lỗi cụ thể và giữ nguyên biểu mẫu cho tác giả chỉnh sửa (BR-19, BR-73).

#### 6. Hậu điều kiện (Postconditions)
- Bài viết mới của Chuyên gia được công khai trực tiếp ngay trên hệ thống (`PUBLISHED`).
- Khi sửa bài: Thông tin mới được cập nhật đồng bộ trên toàn hệ thống.
- Khi xóa bài: Bài viết không còn khả dụng công khai; các mục tham chiếu trong Saved Recipes và Meal Planner chuyển sang trạng thái đã xóa.

---

<a id="fr-05"></a>
## FR-05 — Cơ chế nộp đơn đăng ký Chuyên gia theo format và Administrator phê duyệt cấp quyền

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-05).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Customer`: Người dùng thông thường đã đăng nhập nộp đơn đăng ký Chuyên gia theo format quy định.
  - `Administrator`: Quản trị viên tiếp nhận, thẩm định nội dung format và ra quyết định phê duyệt hoặc từ chối đơn.
- **Secondary Actor / External System:**
  - `Hệ thống thông báo (Notification Service)`: Gửi thông báo in-app cho người dùng khi đơn được xử lý.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Người dùng đã đăng nhập với vai trò `CUSTOMER` ở trạng thái hoạt động (`ACTIVE`) và đã xác minh email. Đối với Administrator: tài khoản phải có vai trò `ADMIN`.
- **Trigger:** Customer nhấn nút "Đăng ký làm Chuyên gia" trong trang cá nhân / thông tin tài khoản; hoặc Administrator truy cập mục "Quản lý đơn Chuyên gia" trong trang Quản trị.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Nộp đơn đăng ký Chuyên gia (UC-05.1)
1. **Main Flow:**
   - Bước 1: Customer nhấn nút "Đăng ký làm Chuyên gia". Giao diện hiển thị biểu mẫu nộp đơn theo format văn bản có cấu trúc.
   - Bước 2: Customer nhập các trường thông tin bắt buộc theo format:
     - *Kinh nghiệm ẩm thực chay* (`bio_experience`): Văn bản từ 20 đến 2.000 ký tự mô tả số năm ăn chay, kinh nghiệm làm bếp hoặc triết lý nấu ăn.
     - *Trường phái chay chuyên sâu* (`vegetarian_type`): Chọn 1 trong các trường phái (Thuần chay / Vegan, Chay có sữa / Lacto, Chay có trứng / Ovo, Chay trứng sữa / Lacto-Ovo, Chay thực dưỡng dưỡng sinh).
     - *Tóm tắt công thức sở trường mẫu* (`sample_recipe_summary`): Văn bản từ 30 đến 2.000 ký tự tóm tắt 1 món chay tâm huyết (tên món, nguyên liệu chính, nét đặc sắc).
     - *Kênh ẩm thực / liên kết tham khảo* (`portfolio_url`): Tùy chọn đúng một URL HTTP/HTTPS hợp lệ, tối đa 500 ký tự (link Facebook, kênh YouTube/TikTok hoặc Blog cá nhân nếu có).
   - Bước 3: Customer tích chọn ô cam kết chia sẻ thông tin an toàn, đúng chuẩn và nhấn "Gửi đơn đăng ký".
   - Bước 4: Hệ thống kiểm tra hợp lệ dữ liệu và kiểm tra quy tắc chống nộp đơn trùng (tài khoản không được có đơn khác đang ở trạng thái `PENDING`).
   - Bước 5: Hệ thống lưu bản ghi mới vào thực thể `EXPERT_APPLICATION` ở trạng thái chờ duyệt (`PENDING`), ghi nhận thời điểm tạo và phản hồi thông báo nộp đơn thành công.
   - Bước 6: Giao diện chuyển sang trạng thái hiển thị "Đơn đăng ký của bạn đang được Administrator xem xét".

##### B. Luồng Administrator thẩm định và phê duyệt đơn (UC-05.2 & UC-05.3)
1. **Main Flow:**
   - Bước 1: Administrator mở mục "Quản lý đơn Chuyên gia" trên trang Quản trị.
   - Bước 2: Hệ thống hiển thị danh sách các đơn đang ở trạng thái `PENDING` kèm thông tin tài khoản nộp, ngày gửi và tóm tắt nội dung.
   - Bước 3: Administrator chọn một đơn để xem chi tiết toàn bộ nội dung format do người dùng khai báo.
   - Bước 4: Administrator đánh giá thủ công tính đầy đủ, liên quan, nhất quán và phù hợp chính sách của nội dung tự khai; hệ thống không tạo điểm Chuyên gia tự động và không khẳng định đã xác minh danh tính hoặc tính xác thực ngoài hệ thống. Nếu đạt, Administrator nhấn "Phê duyệt" (Approve).
   - Bước 5: Administrator có thể nhập lời nhắn/chúc mừng tùy chọn vào `admin_note`.
   - Bước 6: Hệ thống kiểm tra lại tài khoản vẫn `ACTIVE`, vai trò vẫn là `CUSTOMER` và đơn vẫn `PENDING`; sau đó cập nhật trạng thái bản ghi `EXPERT_APPLICATION` thành `APPROVED`, ghi nhận `reviewed_by` và `reviewed_at`.
   - Bước 7: Hệ thống lập tức cập nhật vai trò của tài khoản người dùng tương ứng trong thực thể `USER` thành `EXPERT`.
   - Bước 8: Hệ thống gửi thông báo in-app chúc mừng tới người dùng: "Chúc mừng bạn! Đơn đăng ký Chuyên gia của bạn đã được phê duyệt. Bạn hiện đã có quyền tạo và chia sẻ công thức trên Mâm Xanh."

##### C. Luồng Administrator từ chối đơn đăng ký (UC-05.4)
1. **Alternative Flow:**
   - Bước 1: Administrator xem chi tiết đơn và nhận thấy thông tin không đạt yêu cầu (nội dung qua loa, không có kinh nghiệm thực tế, hoặc vi phạm chính sách).
   - Bước 2: Administrator nhấn "Từ chối" (Reject).
   - Bước 3: Hệ thống hiển thị hộp thoại bắt buộc Administrator phải nhập lý do từ chối cụ thể vào ô `admin_note` (từ 10 đến 500 ký tự, không được để trống).
   - Bước 4: Administrator nhập lý do và xác nhận từ chối.
   - Bước 5: Hệ thống cập nhật trạng thái bản ghi `EXPERT_APPLICATION` thành `REJECTED`, lưu lý do từ chối, ghi nhận `reviewed_by` và `reviewed_at`. Tài khoản người dùng vẫn giữ nguyên vai trò `CUSTOMER`.
   - Bước 6: Hệ thống gửi thông báo in-app tới người dùng giải thích rõ lý do đơn bị từ chối và hướng dẫn hoàn thiện để có thể nộp lại.

##### D. Luồng Ngoại lệ & An ninh (Exception & Security Flow)
1. **Chặn nộp đơn trùng lặp khi đang có đơn chờ duyệt:**
   - Nếu Customer đang có một bản ghi đơn ở trạng thái `PENDING` mà tiếp tục gửi request nộp đơn mới:
   - Hệ thống từ chối tiếp nhận, trả về mã lỗi `HTTP 409 Conflict` kèm thông báo: "Bạn đã có một đơn đăng ký đang chờ xét duyệt. Vui lòng chờ phản hồi từ Quản trị viên."
2. **Cho phép nộp lại sau khi bị từ chối:**
   - Nếu đơn trước đó của Customer đã có kết quả `REJECTED`, hệ thống cho phép người dùng bấm "Nộp lại đơn đăng ký" để tạo một bản ghi `EXPERT_APPLICATION` mới với thông tin đã được chỉnh sửa bổ sung.
3. **Chặn người dùng đã là Chuyên gia nộp đơn:**
   - Nếu tài khoản đã có vai trò `Role = EXPERT` hoặc `Role = ADMIN`, hệ thống ẩn nút đăng ký và chặn request nộp đơn với mã lỗi `HTTP 400 Bad Request`.
4. **Không cho rút đơn đang chờ trong MVP:**
   - Customer được xem đơn `PENDING` nhưng không có thao tác rút/hủy đơn. Nếu cần dừng xử lý, Customer liên hệ Administrator theo kênh hỗ trợ ngoài phạm vi chức năng này.
5. **Chống xử lý đồng thời hoặc xử lý lại:**
   - Chỉ đơn còn `PENDING` mới được phê duyệt/từ chối. Nếu một Administrator khác đã xử lý đơn hoặc tài khoản không còn `ACTIVE`/`CUSTOMER`, hệ thống từ chối quyết định mới với `HTTP 409 Conflict`, không ghi đè kết quả trước đó và yêu cầu tải lại dữ liệu.
6. **Xem lịch sử đơn của chính mình:**
   - Customer xem danh sách các đơn đã gửi theo thứ tự mới nhất trước, gồm ngày gửi, trạng thái, thời điểm xử lý và `admin_note` khi có; không xem được đơn của tài khoản khác.

#### 6. Hậu điều kiện (Postconditions)
- Bản ghi `EXPERT_APPLICATION` được tạo với trạng thái `PENDING` hoặc cập nhật trạng thái `APPROVED` / `REJECTED`.
- Khi được phê duyệt: Vai trò của tài khoản trong cơ sở dữ liệu chuyển thành `EXPERT`, mở khóa toàn bộ quyền đăng bài tại FR-04.
- Khi bị từ chối: Lý do từ chối được lưu trữ và hiển thị cho người dùng; vai trò tài khoản vẫn là `CUSTOMER`.
- Lịch sử các lần nộp được bảo toàn; đơn mới sau khi bị từ chối không ghi đè bản ghi cũ.
- Sau khi phê duyệt, hệ thống làm mới trạng thái phân quyền để người dùng nhận quyền `EXPERT` trong phiên hợp lệ tiếp theo mà không phải nộp lại đơn.

---

<a id="fr-25"></a>
## FR-25 — Công khai Recipe Post ngay sau khi validation đạt

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-25).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Chuyên gia` (`Role = EXPERT`): Tác giả thực hiện công khai bài viết.
- **Secondary Actor / External System:**
  - `Hệ thống kiểm tra tính hợp lệ (Validation Engine)`: Tự động thẩm định dữ liệu bài viết trước khi xuất bản.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Người dùng đã đăng nhập với tài khoản Chuyên gia hoạt động (`ACTIVE`) và đã hoàn thiện nội dung bài công thức trên biểu mẫu.
- **Trigger:** Tác giả nhấn nút "Công khai bài viết".

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Xuất bản trực tiếp khi đạt validation (UC-25.1)
1. **Main Flow:**
   - Bước 1: Chuyên gia nhấn nút "Công khai bài viết" trên giao diện soạn thảo công thức.
   - Bước 2: Hệ thống kích hoạt quy trình thẩm định tính hợp lệ toàn diện ở tầng máy chủ (Server-side Validation) đối chiếu trực tiếp với bộ quy tắc Recipe Validation Profile chuẩn được định nghĩa tại FR-16 và các Business Rules liên quan:
     - Thẩm định cấu trúc và độ dài trường theo FR-16: tiêu đề (3–120 ký tự), thể loại món ăn (`dish_category` thuộc danh mục chuẩn), khẩu phần (1–50), thời gian chuẩn bị và nấu (mỗi giá trị 0–1.440 phút, tổng thời gian $> 0$), mô tả bài viết ($\le 2.000$ ký tự), chọn đúng 1 trong 4 loại ăn chay chuẩn (BR-07, BR-19).
     - Thẩm định nguyên liệu theo FR-19: danh sách từ 1 đến 50 dòng, bắt buộc có tên nguyên liệu, số lượng số dương ($> 0$) và đơn vị đo chuẩn thuộc bảng `UNIT`.
     - Thẩm định quy tắc chuyển đổi đơn vị (`INGREDIENT_UNIT_CONVERSION`): Đối với các nguyên liệu sử dụng đơn vị cần quy đổi sang gram để tính toán dinh dưỡng (như đơn vị thuộc chiều `COUNT` quả/củ/bìa hoặc chiều `VOLUME` khác), nếu trong cơ sở dữ liệu chưa có quy tắc quy đổi tương ứng cho nguyên liệu đó, hệ thống bắt buộc từ chối xuất bản (Validation Error) và yêu cầu tác giả chọn đơn vị đo khác (như gram) (BR-14, BR-19, BR-73).
     - Thẩm định nội dung hướng dẫn chuẩn bị/chế biến theo FR-16: bắt buộc có nội dung `instructions` từ 10 đến 5.000 ký tự không rỗng sau khi trim (BR-19); không ép buộc phân rã thành các bước độc lập.
     - Thẩm định tệp ảnh và video theo FR-14 và FR-15: từ 0 đến 5 hình ảnh minh họa (`RECIPE_MEDIA`, JPEG/PNG/WebP $\le 5$ MB), nếu có ảnh thì bắt buộc có đúng 1 ảnh được chỉ định làm ảnh đại diện (`is_cover = true`) và có thứ tự hiển thị `display_order`; tối đa 1 liên kết YouTube hợp lệ.
     - Xác thực quyền tác giả: Tác giả được trích xuất tự động và gắn cố định từ phiên đăng nhập hợp lệ của Chuyên gia theo FR-23 và BR-17.
   - Bước 3: Toàn bộ các điều kiện đều thỏa mãn.
   - Bước 4: Hệ thống cập nhật trạng thái bài viết thành `PUBLISHED`, lưu nội dung hướng dẫn `instructions` và ảnh `RECIPE_MEDIA`, lưu thời điểm công khai (`publishedAt`), và phản hồi thành công.
   - Bước 5: Bài viết xuất hiện ngay lập tức trên trang chủ, trang khám phá, kết quả tìm kiếm và trang hồ sơ cá nhân của tác giả.
   - Bước 6: Giao diện chuyển hướng tác giả đến trang chi tiết bài viết vừa xuất bản kèm thông báo chúc mừng.
2. **Exception Flow (Kiểm tra dữ liệu không đạt):**
   - Bước 1: Khi phát hiện dữ liệu vi phạm bộ quy tắc Recipe Validation Profile (ví dụ: tiêu đề dưới 3 ký tự, thiếu thể loại món, chưa có dòng nguyên liệu nào, nguyên liệu thiếu quy tắc quy đổi đơn vị, thiếu hướng dẫn chế biến hoặc hướng dẫn dưới 10 ký tự, thiếu ảnh bìa khi có upload ảnh, tổng thời gian bằng 0):
   - Bước 2 (Xử lý máy chủ): Máy chủ thực thi thẩm định độc lập, lập tức từ chối yêu cầu xuất bản không hợp lệ và dừng quy trình, hoàn toàn không tạo bản ghi nào trong cơ sở dữ liệu ở bất kỳ trạng thái nào (kể cả nháp hay công khai).
   - Bước 3 (Xử lý giao diện): Giao diện giữ nguyên nội dung tác giả đã nhập, không làm mất dữ liệu biểu mẫu và hiển thị thông báo lỗi cụ thể tương ứng với trường không hợp lệ để tác giả tiếp tục hoàn thiện.

#### 6. Hậu điều kiện (Postconditions)
- Bài viết được chuyển sang trạng thái `PUBLISHED` và có thể tiếp cận công khai bởi toàn bộ người dùng (Guest, Member, Admin).
- Không tạo ra bất kỳ trạng thái trung gian nào như `Draft`, `Pending Review`, hoặc `Waiting for Approval` ở cấp bài viết (BR-07).

---

<a id="fr-26"></a>
## FR-26 — Gửi báo cáo bài công thức có vấn đề

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-26).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member` (Người báo cáo): Gửi phản ánh về bài công thức có vấn đề.
  - `Guest`: Người dùng chưa đăng nhập được yêu cầu xác thực.
- **Secondary Actor / External System:**
  - `Administrator`: Quản trị viên tiếp nhận và xử lý báo cáo.

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

---

<a id="fr-28"></a>
## FR-28 — Administrator xử lý báo cáo qua các trạng thái

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-28).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Administrator`: Người thực hiện tiếp nhận, xem xét và ra quyết định giải quyết báo cáo.

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

---

<a id="fr-29"></a>
## FR-29 — Phân quyền hiển thị thông tin báo cáo

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-29).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Administrator`: Xem toàn bộ thông tin phục vụ quản trị.
  - `Member` (Người gửi báo cáo): Xem trạng thái báo cáo của mình.
  - `Member` (Tác giả bài viết bị xử lý): Xem thông báo kết quả chế tài.

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

---

<a id="fr-31"></a>
## FR-31 — Điều kiện thông tin tối thiểu trước khi dùng AI cá nhân hóa

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-31).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member`: Người dùng đã đăng nhập tham gia Onboarding, xem và cập nhật hồ sơ sở thích ăn uống cá nhân.
- **Secondary Actor / External System:**
  - `Dịch vụ AI (Google Gemini)`: Tiếp nhận dữ liệu hồ sơ sở thích ăn uống hợp lệ làm đầu vào (constraints/context) để thực hiện gợi ý món ăn và lập thực đơn cá nhân hóa.

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
      - Member VẪN ĐƯỢC: Duyệt, tìm kiếm và lọc các bài công thức công khai; Đọc và gửi bình luận; Chuyên gia được tạo, chỉnh sửa và công khai Recipe Post của chính mình; Lưu bài công thức vào `Saved Recipes`; Tự thêm bài công thức vào `Meal Planner` 3 bữa theo cách thủ công; Sử dụng Chatbot AI hỏi đáp kiến thức chay chung (FR-51).
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
   - Bước 4: Điều kiện tối thiểu thỏa mãn -> Hệ thống tiếp tục kiểm tra quyền gói tính năng AI (FR-10, BR-02), đưa hồ sơ sở thích vào ngữ cảnh xử lý và gửi yêu cầu tới dịch vụ Google Gemini.
2. **Luồng Rẽ nhánh An toàn (Thiếu thông tin tối thiểu — Chặn an toàn & Không gọi AI):**
   - Bước 1: Member (chưa hoàn thành Onboarding hoặc hồ sơ chưa đầy đủ) yêu cầu tính năng AI cá nhân hóa.
   - Bước 2: Hệ thống kiểm tra hồ sơ sở thích và phát hiện thiếu ít nhất một trong ba nhóm thông tin tối thiểu.
   - Bước 3: Hệ thống **NGAY LẬP TỨC DỪNG XỬ LÝ**:
     - **TUYỆT ĐỐI KHÔNG GỌI DỊCH VỤ GOOGLE GEMINI** (BR-31).
     - **TUYỆT ĐỐI KHÔNG TẠO BẢN GHI DỞ DANG VÀO LỊCH ĂN** (BR-31).
   - Bước 4: Hệ thống phản hồi từ chối kèm danh sách các trường thông tin còn thiếu.
   - Bước 5: Giao diện hiển thị thông báo thân thiện: "Bạn cần hoàn tất 3 thông tin cơ bản về chế độ ăn chay (Loại ăn chay, Nguyên liệu dị ứng/kiêng, Món không thích) để AI có thể gợi ý chính xác và an toàn." kèm nút bấm "Cập nhật hồ sơ ngay".
   - Bước 6: Member nhấp vào nút và được chuyển trực tiếp tới màn hình bổ sung các trường còn thiếu.

#### 7. Hậu điều kiện (Postconditions)
- Khi hoàn thành Onboarding hoặc cập nhật hồ sơ: Hồ sơ sở thích được lưu trữ gắn với tài khoản Member; ghi nhận trạng thái đã hoàn tất Onboarding.
- Khi gọi AI thành công: Yêu cầu AI cá nhân hóa sử dụng đúng các ràng buộc sở thích mới nhất.
- Khi bị chặn do thiếu hồ sơ: Không có yêu cầu nào gửi tới dịch vụ AI bên ngoài; không có dữ liệu sai lệch nào được ghi nhận vào Lịch ăn.

---

<a id="fr-32"></a>
## FR-32 — Lưu, bỏ lưu và quản lý danh sách công thức đã lưu

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-32).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member`: Người dùng đã đăng nhập thực hiện lưu, bỏ lưu và quản lý danh sách.
  - `Guest`: Người dùng chưa đăng nhập được yêu cầu đăng nhập khi muốn lưu bài.

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

---

<a id="fr-33"></a>
## FR-33 — Thêm và quản lý bài công thức trong lịch ăn 3 bữa

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-33).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member`: Người dùng đã đăng nhập thao tác sắp xếp món ăn vào các bữa.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Bài công thức đang ở trạng thái công khai (`PUBLISHED`) hoặc trong danh sách đã lưu của Member. Member đã đăng nhập với tài khoản hoạt động (`ACTIVE`).
- **Trigger:** Member nhấn nút "Thêm món vào bữa" trên Lịch ăn, nhấn nút "Thêm vào lịch ăn" trên trang chi tiết bài công thức, hoặc nhấn nút "Xuất lịch ăn tuần".

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

##### E. Luồng Xuất bản Lịch ăn tuần sang PDF / TXT (UC-33.4)
1. **Main Flow:**
   - Bước 1: Member nhấn nút "Xuất lịch ăn tuần" trên thanh công cụ Lịch ăn.
   - Bước 2: Member chọn định dạng xuất: PDF hoặc TXT.
   - Bước 3: Hệ thống tổng hợp toàn bộ các món ăn theo từng ngày (Thứ Hai đến Chủ Nhật) và từng bữa (Sáng, Trưa, Tối), kèm thông tin khẩu phần và năng lượng ước tính.
   - Bước 4: Trình duyệt tải xuống tệp tin hoàn chỉnh để in ấn (bố cục bảng A4 đối với PDF) hoặc lưu trữ ngoại tuyến.

#### 6. Hậu điều kiện (Postconditions)
- Cấu trúc các món ăn trong từng bữa được cập nhật chính xác.
- Bảo đảm tuyệt đối không có 2 bản ghi cùng bài công thức trong cùng một bữa của một ngày.

---

<a id="fr-44"></a>
## FR-44 — Chuyên gia sửa hoặc xóa Recipe Post đã công khai của chính mình

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-44).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập tài khoản Chuyên gia và là tác giả sở hữu bài công thức (BR-17, BR-64).
  - Bài công thức đang ở trạng thái công khai (`Public`) và không nằm trong trạng thái bị Administrator khóa/ẩn vi phạm (BR-27).
- **Kích hoạt (Trigger):**
  - Tác giả nhấn nút "Chỉnh sửa công thức" hoặc "Xóa công thức" trên giao diện bài viết hoặc trang quản lý bài viết cá nhân.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow - Chỉnh sửa công thức):**
  - Bước 1: Tác giả nhấn "Chỉnh sửa công thức". Hệ thống kiểm tra quyền tác giả (BR-62, BR-64); nếu trùng khớp, hiển thị biểu mẫu chỉnh sửa với toàn bộ dữ liệu hiện tại của bài viết.
  - Bước 2: Tác giả sửa đổi các thông tin (tiêu đề, thể loại món `dish_category`, thời gian nấu, khẩu phần, danh sách nguyên liệu và định lượng, nội dung hướng dẫn chế biến `instructions` 10–5.000 ký tự, thêm/xóa/sắp xếp ảnh minh họa và chọn ảnh đại diện theo FR-14).
  - Bước 3: Tác giả nhấn "Lưu thay đổi".
  - Bước 4: Hệ thống thực hiện kiểm tra hợp lệ toàn bộ các tiêu chí validation bắt buộc theo FR-16, BR-19 và BR-73 (bao gồm kiểm tra tính khả dụng của quy tắc chuyển đổi đơn vị nguyên liệu).
  - Bước 5: Kiểm tra thành công, hệ thống cập nhật nội dung bài viết, trường `instructions` và ảnh `RECIPE_MEDIA` vào cơ sở dữ liệu, tự động tính toán lại bảng ước tính 9 chỉ tiêu dinh dưỡng (FR-39), và xuất bản trực tiếp các thay đổi lên trang công khai ngay lập tức mà không qua kiểm duyệt trước của Admin (BR-07, BR-59, BR-62).
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

---

<a id="fr-49"></a>
## FR-49 — Gửi thông báo trong app và email cho reply mới và kết quả báo cáo

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-49).

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

---
