> **Document:** Use Case Specifications — M01
> **File:** `docs/requirements/use-cases/discovery-and-public-content.md`
> **Version:** v2.1.1
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-27
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M01

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns the required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

## Dedicated Actor-Goal Use Cases

<a id="uc-01-1"></a>
### UC-01.1 — Duyệt danh sách bài công thức công khai
- **Goal / Primary Actor:** Guest xem danh sách Recipe Post công khai; hệ thống nội dung là supporting actor.
- **Trigger / Preconditions:** Guest mở Trang chủ/Khám phá; chỉ bài `PUBLISHED` hợp lệ được truy xuất.
- **Main Flow:** Hệ thống tải trang kết quả, hiển thị Recipe Card và cho phép chuyển trang/sắp xếp.
- **Alternative / Security:** Danh sách rỗng hiển thị empty state; bài ẩn/xóa không được trả về.
- **Postconditions:** Không thay đổi dữ liệu người dùng; danh sách công khai được hiển thị.
- **Traceability / Acceptance Coverage:** FR-01; BR-05; NFR-02; [AC-01.1, AC-01.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-01).

<a id="uc-01-2"></a>
### UC-01.2 — Tìm kiếm công thức cơ bản theo từ khóa
- **Goal / Primary Actor:** Guest tìm Recipe Post công khai theo từ khóa.
- **Trigger / Preconditions:** Guest nhập từ khóa; kho nội dung có thể rỗng.
- **Main Flow:** Hệ thống chuẩn hóa truy vấn, tìm trong tiêu đề/mô tả và trả kết quả có phân trang.
- **Alternative / Security:** Không có kết quả thì hiển thị thông báo/gợi ý; nội dung không công khai bị loại.
- **Postconditions:** Kết quả phản ánh truy vấn, không phát sinh dữ liệu cá nhân.
- **Traceability / Acceptance Coverage:** FR-01; NFR-02; [AC-01.2, AC-01.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-01).

<a id="uc-01-3"></a>
### UC-01.3 — Xem chi tiết bài công thức công khai
- **Goal / Primary Actor:** Guest đọc đầy đủ một Recipe Post công khai.
- **Trigger / Preconditions:** Guest chọn Recipe Card; bài còn `PUBLISHED`.
- **Main Flow:** Hệ thống tải nội dung, media, nguyên liệu, hướng dẫn, tác giả và thống kê công khai.
- **Alternative / Security:** Bài không còn khả dụng trả trạng thái phù hợp; thao tác thành viên yêu cầu đăng nhập.
- **Postconditions:** Chi tiết được hiển thị; lượt xem hợp lệ có thể được ghi nhận theo BR-70.
- **Traceability / Acceptance Coverage:** FR-01; BR-05, BR-69, BR-70; [AC-01.3, AC-01.4, AC-01.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-01).

<a id="uc-15-1"></a>
### UC-15.1 — Gắn hoặc cập nhật liên kết YouTube
- **Goal / Primary Actor:** Expert gắn tối đa một video YouTube cho Recipe Post của mình; YouTube là supporting system.
- **Trigger / Preconditions:** Expert đang tạo/sửa bài thuộc quyền sở hữu.
- **Main Flow:** Expert nhập URL; hệ thống kiểm tra domain/format, trích Video ID và hiển thị preview trước khi lưu.
- **Alternative / Security:** Cho phép để trống/xóa link; URL ngoài YouTube hoặc payload nguy hiểm bị từ chối.
- **Postconditions:** Chỉ tham chiếu URL/Video ID hợp lệ được lưu, không lưu binary video.
- **Traceability / Acceptance Coverage:** FR-15; BR-10, BR-20; NFR-10; [AC-15.1–AC-15.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-15).

<a id="uc-15-2"></a>
### UC-15.2 — Xem video YouTube nhúng
- **Goal / Primary Actor:** Guest/Member phát video hướng dẫn trong trang chi tiết.
- **Trigger / Preconditions:** Recipe Post công khai có Video ID hợp lệ và video cho phép nhúng.
- **Main Flow:** Hệ thống dựng iframe an toàn; người xem chủ động bấm phát.
- **Alternative / Security:** Không có video thì không hiển thị khung; lỗi/không cho embed không làm hỏng trang.
- **Postconditions:** Video được phát từ YouTube; hệ thống không sao chép file video.
- **Traceability / Acceptance Coverage:** FR-15; BR-10, BR-20; NFR-10; [AC-15.2, AC-15.4, AC-15.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-15).

<a id="uc-20-1"></a>
### UC-20.1 — Xem chi tiết từ nguồn Recipe Post duy nhất
- **Goal / Primary Actor:** Guest/Member xem cùng một dữ liệu Recipe Post nhất quán ở trang chi tiết.
- **Trigger / Preconditions:** Người dùng mở Recipe Post công khai.
- **Main Flow:** Hệ thống đọc nguồn bài duy nhất và kết xuất nội dung, media, nguyên liệu, dinh dưỡng khả dụng.
- **Alternative / Security:** Bài ẩn/xóa không lộ nội dung; trạng thái không khả dụng được xử lý an toàn.
- **Postconditions:** Dữ liệu hiển thị nhất quán với bản ghi nguồn.
- **Traceability / Acceptance Coverage:** FR-20; BR-16; NFR-02; [AC-20.1, AC-20.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-20).

<a id="uc-20-2"></a>
### UC-20.2 — Xem tham chiếu tự đồng bộ sau chỉnh sửa
- **Goal / Primary Actor:** Member xem Meal Plan/Saved Recipe phản ánh nội dung Recipe Post mới nhất.
- **Trigger / Preconditions:** Bài nguồn đã được Expert chỉnh sửa; Member có tham chiếu hợp lệ.
- **Main Flow:** Hệ thống resolve tham chiếu tới bài nguồn và hiển thị dữ liệu hiện hành.
- **Alternative / Security:** Không sao chép nội dung thành bản độc lập; quyền dữ liệu cá nhân vẫn được kiểm tra.
- **Postconditions:** Không phát sinh bản sao lệch nguồn.
- **Traceability / Acceptance Coverage:** FR-20; BR-16; [AC-20.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-20).

<a id="uc-20-3"></a>
### UC-20.3 — Xem Tombstone khi bài nguồn không khả dụng
- **Goal / Primary Actor:** Member hiểu vì sao mục Meal Plan cũ không còn mở được.
- **Trigger / Preconditions:** Tham chiếu tồn tại nhưng Recipe Post nguồn đã bị xóa/ẩn.
- **Main Flow:** Hệ thống giữ tham chiếu lịch sử và hiển thị Tombstone thay cho nội dung công khai.
- **Alternative / Security:** Không để lộ nội dung đã ẩn; người dùng có thể gỡ/thay mục lịch ăn.
- **Postconditions:** Tính toàn vẹn Meal Plan được giữ mà không phục hồi nội dung nguồn.
- **Traceability / Acceptance Coverage:** FR-20; BR-16; [AC-20.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-20).

<a id="uc-20-4"></a>
### UC-20.4 — Xuất chi tiết công thức PDF hoặc TXT
- **Goal / Primary Actor:** Member tải bản Recipe Post chi tiết để sử dụng ngoại tuyến.
- **Trigger / Preconditions:** Member chọn Export trên bài đang khả dụng.
- **Main Flow:** Hệ thống dựng nội dung từ nguồn hiện hành và trả file PDF/TXT.
- **Alternative / Security:** Dữ liệu thiếu được ghi minh bạch; không đưa dữ liệu riêng tư ngoài phạm vi bài.
- **Postconditions:** File được tải; Recipe Post nguồn không bị thay đổi.
- **Traceability / Acceptance Coverage:** FR-20; [AC-20.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-20).

<a id="uc-23-1"></a>
### UC-23.1 — Hiển thị thông tin tác giả trên Recipe Post
- **Goal / Primary Actor:** Guest/Member nhận diện tác giả của bài công khai.
- **Trigger / Preconditions:** Bài có author liên kết hợp lệ.
- **Main Flow:** Hệ thống hiển thị tên, avatar và liên kết hồ sơ công khai từ tài khoản tác giả.
- **Alternative / Security:** Dùng avatar mặc định khi thiếu; không hiển thị dữ liệu riêng tư.
- **Postconditions:** Quyền tác giả hiển thị đúng tài khoản nguồn.
- **Traceability / Acceptance Coverage:** FR-23; BR-18; [AC-23.1, AC-23.3, AC-23.9](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-23).

<a id="uc-23-2"></a>
### UC-23.2 — Xem hồ sơ công khai của Member
- **Goal / Primary Actor:** Guest/Member xem thông tin và bài công khai của một Member.
- **Trigger / Preconditions:** Người xem chọn tên/avatar tác giả; hồ sơ còn hoạt động.
- **Main Flow:** Hệ thống tải các trường công khai và Recipe Post công khai của hồ sơ.
- **Alternative / Security:** Tài khoản không khả dụng trả trạng thái phù hợp; email/dữ liệu riêng không xuất hiện.
- **Postconditions:** Chỉ dữ liệu hồ sơ công khai được trình bày.
- **Traceability / Acceptance Coverage:** FR-23; BR-18; NFR-08; [AC-23.2, AC-23.3, AC-23.8](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-23).

<a id="uc-23-3"></a>
### UC-23.3 — Xem và chỉnh sửa hồ sơ cá nhân
- **Goal / Primary Actor:** Member cập nhật tên hiển thị, giới thiệu và avatar của mình; Blob Storage hỗ trợ avatar.
- **Trigger / Preconditions:** Member đã đăng nhập và mở hồ sơ cá nhân.
- **Main Flow:** Member chỉnh dữ liệu; hệ thống validate, lưu và phản ánh trên hồ sơ công khai.
- **Alternative / Security:** Tên/tệp không hợp lệ bị từ chối; owner-only enforcement chặn sửa hồ sơ khác.
- **Postconditions:** Hồ sơ hợp lệ được cập nhật, dữ liệu riêng vẫn được bảo vệ.
- **Traceability / Acceptance Coverage:** FR-23; BR-18; NFR-08, NFR-09; [AC-23.4–AC-23.8, AC-23.10](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-23).

<a id="uc-46-1"></a>
### UC-46.1 — Xem chuỗi bình luận lồng nhau
- **Goal / Primary Actor:** Guest/Member đọc bình luận và reply theo cây tối đa 5 cấp.
- **Trigger / Preconditions:** Người dùng mở Recipe Post công khai.
- **Main Flow:** Hệ thống tải chuỗi bình luận, quan hệ cha-con và Tombstone theo thứ tự hiển thị.
- **Alternative / Security:** Bình luận bị gỡ hiển thị Tombstone khi cần giữ con; không lộ nội dung đã gỡ.
- **Postconditions:** Cấu trúc hội thoại được bảo toàn.
- **Traceability / Acceptance Coverage:** FR-46; BR-66; [AC-46.1, AC-46.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-46).

<a id="uc-46-2"></a>
### UC-46.2 — Tạo bình luận gốc
- **Goal / Primary Actor:** Member đăng bình luận mới trên Recipe Post công khai.
- **Trigger / Preconditions:** Member đăng nhập; bài còn công khai.
- **Main Flow:** Member nhập nội dung; Backend validate/làm sạch và công khai bình luận.
- **Alternative / Security:** Guest bị yêu cầu đăng nhập; input rỗng/mã độc bị từ chối.
- **Postconditions:** Bình luận gốc hợp lệ được lưu.
- **Traceability / Acceptance Coverage:** FR-46; BR-66; NFR-10; [AC-46.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-46).

<a id="uc-46-3"></a>
### UC-46.3 — Reply bình luận trong giới hạn 5 cấp
- **Goal / Primary Actor:** Member trả lời một bình luận/reply.
- **Trigger / Preconditions:** Member chọn Reply; node cha thuộc cùng Recipe Post.
- **Main Flow:** Hệ thống kiểm tra depth, lưu reply và gắn đúng parent.
- **Alternative / Security:** Vượt cấp hoặc parent sai bài bị từ chối; input được làm sạch.
- **Postconditions:** Reply xuất hiện đúng vị trí trong cây.
- **Traceability / Acceptance Coverage:** FR-46; BR-66; [AC-46.1, AC-46.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-46).

<a id="uc-46-4"></a>
### UC-46.4 — Chỉnh sửa bình luận của chính mình
- **Goal / Primary Actor:** Member sửa nội dung bình luận/reply do mình tạo.
- **Trigger / Preconditions:** Member chọn Edit trên comment thuộc quyền sở hữu.
- **Main Flow:** Backend kiểm tra ownership, validate nội dung và lưu thay đổi.
- **Alternative / Security:** Sửa comment người khác bị chặn; nội dung không hợp lệ không được lưu.
- **Postconditions:** Comment hợp lệ được cập nhật mà không đổi cấu trúc cây.
- **Traceability / Acceptance Coverage:** FR-46; NFR-09, NFR-10; [AC-46.3, AC-46.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-46).

<a id="uc-46-5"></a>
### UC-46.5 — Xóa bình luận của chính mình
- **Goal / Primary Actor:** Member xóa comment/reply do mình tạo.
- **Trigger / Preconditions:** Member chọn Delete và xác nhận; ownership hợp lệ.
- **Main Flow:** Hệ thống xóa nội dung hoặc tạo Tombstone nếu còn reply con.
- **Alternative / Security:** Xóa comment người khác bị chặn; comment không con có thể được gỡ theo rule.
- **Postconditions:** Nội dung không còn công khai, cây reply vẫn toàn vẹn.
- **Traceability / Acceptance Coverage:** FR-46; BR-66; [AC-46.2, AC-46.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-46).

<a id="uc-46-6"></a>
### UC-46.6 — Administrator gỡ bình luận vi phạm
- **Goal / Primary Actor:** Administrator hậu kiểm comment/reply vi phạm.
- **Trigger / Preconditions:** Admin mở nội dung bị báo cáo/xác định vi phạm.
- **Main Flow:** Admin chọn chế tài; Backend kiểm tra quyền và gỡ nội dung, giữ Tombstone khi có con.
- **Alternative / Security:** Người không phải Admin bị chặn; quyết định phải theo audit rule liên quan.
- **Postconditions:** Nội dung vi phạm không còn hiển thị, cấu trúc reply được giữ.
- **Traceability / Acceptance Coverage:** FR-46; FR-48; BR-66; [AC-46.2, AC-46.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-46).

<a id="uc-57-1"></a>
### UC-57.1 — Gửi Like cho Recipe Post
- **Goal / Primary Actor:** Member bày tỏ Like cho bài công khai.
- **Trigger / Preconditions:** Member đăng nhập, không phải tác giả; bài `PUBLISHED`.
- **Main Flow:** Backend upsert `LIKE`, cập nhật kết quả hiển thị và trả trạng thái hiện hành.
- **Alternative / Security:** Guest/tác giả bị chặn; cùng lựa chọn lần nữa chuyển sang UC-57.3.
- **Postconditions:** Tối đa một reaction hiệu lực của Member trên bài.
- **Traceability / Acceptance Coverage:** FR-57; BR-69; [AC-57.1, AC-57.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-57).

<a id="uc-57-2"></a>
### UC-57.2 — Gửi Dislike cho Recipe Post
- **Goal / Primary Actor:** Member bày tỏ Dislike cho bài công khai.
- **Trigger / Preconditions:** Member đăng nhập, không phải tác giả; bài `PUBLISHED`.
- **Main Flow:** Backend upsert `DISLIKE` và trả số liệu/tỷ lệ mới.
- **Alternative / Security:** Guest/tác giả bị chặn; reaction trước đó được switch nguyên tử.
- **Postconditions:** Một reaction hiệu lực được lưu.
- **Traceability / Acceptance Coverage:** FR-57; BR-69; [AC-57.2, AC-57.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-57).

<a id="uc-57-3"></a>
### UC-57.3 — Chuyển hoặc hủy reaction
- **Goal / Primary Actor:** Member switch Like/Dislike hoặc toggle off lựa chọn hiện tại.
- **Trigger / Preconditions:** Member đã có reaction trên bài.
- **Main Flow:** Hệ thống đổi loại khi chọn nút đối diện hoặc xóa reaction khi chọn lại cùng nút.
- **Alternative / Security:** Request lặp/race được xử lý theo unique constraint và transaction.
- **Postconditions:** Trạng thái reaction và aggregate phản ánh đúng lựa chọn cuối.
- **Traceability / Acceptance Coverage:** FR-57; BR-69; [AC-57.2, AC-57.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-57).

<a id="uc-57-4"></a>
### UC-57.4 — Xem tỷ lệ Like và tổng bình chọn
- **Goal / Primary Actor:** Guest/Member xem mức phản hồi của cộng đồng.
- **Trigger / Preconditions:** Recipe Card hoặc Recipe Detail được hiển thị.
- **Main Flow:** Hệ thống hiển thị `like_percentage` và tổng Like/Dislike; chưa có vote thì hiển thị `Mới`.
- **Alternative / Security:** Guest bấm nút reaction được yêu cầu đăng nhập; số liệu không cho phép suy ra người bình chọn.
- **Postconditions:** Không thay đổi reaction khi chỉ xem.
- **Traceability / Acceptance Coverage:** FR-57; BR-69; [AC-57.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-57).

<a id="uc-58-3"></a>
### UC-58.3 — Xem tổng lượt xem Recipe Post
- **Goal / Primary Actor:** Guest/Member xem tổng lượt xem công khai.
- **Trigger / Preconditions:** Recipe Card/Detail được hiển thị; aggregate view khả dụng.
- **Main Flow:** Hệ thống trả và hiển thị tổng view toàn thời gian.
- **Alternative / Security:** Thiếu aggregate tạm thời không làm lỗi nội dung; không lộ viewer identity.
- **Postconditions:** Chỉ số được xem, không tạo thêm view chỉ do render aggregate.
- **Traceability / Acceptance Coverage:** FR-58; BR-70; [AC-17.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-17) verifies the Recipe Card count and [AC-20.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-20) verifies the Recipe Detail count.

`UC-58.1`, `UC-58.2` và `UC-58.4` là system/background mechanisms (capture, deduplication và aggregation), không phải actor-goal UC. [AC-58.1–AC-58.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-58) vẫn là FR-level/system acceptance.

<a id="uc-59-1"></a>
### UC-59.1 — Theo dõi Member khác
- **Goal / Primary Actor:** Member theo dõi một Member khác.
- **Trigger / Preconditions:** Member đăng nhập, target hoạt động và chưa được follow.
- **Main Flow:** Backend kiểm tra hai tài khoản, tạo quan hệ có hướng và trả trạng thái/count mới.
- **Alternative / Security:** Tự follow, target không hoạt động hoặc cặp trùng bị chặn.
- **Postconditions:** Quan hệ A → B duy nhất tồn tại.
- **Traceability / Acceptance Coverage:** FR-59; BR-75; [AC-59.1–AC-59.4, AC-59.7](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-59).

<a id="uc-59-2"></a>
### UC-59.2 — Bỏ theo dõi Member
- **Goal / Primary Actor:** Member ngừng theo dõi một Member khác.
- **Trigger / Preconditions:** Member bấm Unfollow; quan hệ có thể đã tồn tại hoặc đã được xóa.
- **Main Flow:** Backend xóa quan hệ nếu có và trả `following=false`.
- **Alternative / Security:** Yêu cầu lặp vẫn thành công về trạng thái; count không xuống dưới dữ liệu thực.
- **Postconditions:** Không còn quan hệ A → B.
- **Traceability / Acceptance Coverage:** FR-59; BR-75; [AC-59.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-59).

<a id="uc-59-3"></a>
### UC-59.3 — Xem trạng thái và số lượng follow
- **Goal / Primary Actor:** Guest/Member xem follower/following counts và trạng thái follow phù hợp.
- **Trigger / Preconditions:** Người dùng mở hồ sơ công khai.
- **Main Flow:** Hệ thống trả count và, với Member đăng nhập, quan hệ của chính người xem với hồ sơ.
- **Alternative / Security:** Tài khoản không hoạt động không xuất hiện; dữ liệu riêng tư không được trả.
- **Postconditions:** Chỉ dữ liệu follow công khai được hiển thị.
- **Traceability / Acceptance Coverage:** FR-59; BR-75; [AC-59.2, AC-59.6, AC-59.7](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-59).

<a id="uc-59-4"></a>
### UC-59.4 — Xem danh sách followers/following
- **Goal / Primary Actor:** Member duyệt danh sách followers/following có phân trang.
- **Trigger / Preconditions:** Member đăng nhập và chọn một danh sách trên hồ sơ.
- **Main Flow:** Hệ thống tải trang dữ liệu, chỉ trả trường hồ sơ công khai.
- **Alternative / Security:** Danh sách rỗng hiển thị empty state; tài khoản không hoạt động bị loại.
- **Postconditions:** Danh sách được xem, quan hệ không thay đổi.
- **Traceability / Acceptance Coverage:** FR-59; BR-75; [AC-59.6, AC-59.7](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-59).

<a id="uc-60-1"></a>
### UC-60.1 — Bắt đầu so sánh hai Recipe Post
- **Goal / Primary Actor:** Guest/Member mở so sánh từ card/detail/saved list.
- **Trigger / Preconditions:** Recipe Post khởi đầu còn công khai.
- **Main Flow:** Hệ thống đặt bài đã chọn vào một vế và yêu cầu chọn bài thứ hai.
- **Alternative / Security:** Bài không còn công khai bị từ chối; Saved list chỉ khả dụng cho Member.
- **Postconditions:** Phiên so sánh tạm thời có một lựa chọn.
- **Traceability / Acceptance Coverage:** FR-60; [AC-60.1, AC-60.7](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-60).

<a id="uc-60-2"></a>
### UC-60.2 — Tìm, chọn hoặc thay Recipe Post so sánh
- **Goal / Primary Actor:** Guest/Member hoàn thiện hoặc thay một vế so sánh.
- **Trigger / Preconditions:** Giao diện so sánh đang mở.
- **Main Flow:** Người dùng tìm/chọn bài; hệ thống xác minh công khai và khác bài còn lại.
- **Alternative / Security:** Chọn cùng bài hoặc bài không khả dụng bị từ chối.
- **Postconditions:** Hai vế chứa hai Recipe Post công khai khác nhau.
- **Traceability / Acceptance Coverage:** FR-60; [AC-60.1, AC-60.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-60).

<a id="uc-60-3"></a>
### UC-60.3 — So sánh tổng quan và nguyên liệu
- **Goal / Primary Actor:** Guest/Member đối chiếu thông tin và nguyên liệu hai công thức.
- **Trigger / Preconditions:** Hai bài hợp lệ đã được chọn.
- **Main Flow:** Hệ thống trình bày hai cột, ghép nguyên liệu chỉ khi identity/quy đổi hợp lệ và giữ giá trị gốc khi không thể quy đổi.
- **Alternative / Security:** Không tự hợp nhất khác nguyên liệu hoặc khác dimension.
- **Postconditions:** Bảng đối chiếu minh bạch, không thay dữ liệu nguồn.
- **Traceability / Acceptance Coverage:** FR-60; [AC-60.1, AC-60.4, AC-60.8](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-60).

<a id="uc-60-4"></a>
### UC-60.4 — So sánh chín chỉ tiêu dinh dưỡng
- **Goal / Primary Actor:** Guest/Member đối chiếu 9 chỉ tiêu trên một khẩu phần.
- **Trigger / Preconditions:** Hai bài đã chọn; dữ liệu dinh dưỡng được tính từ nguồn tin cậy khi khả dụng.
- **Main Flow:** Hệ thống hiển thị mỗi giá trị và chênh lệch tuyệt đối theo cùng đơn vị.
- **Alternative / Security:** Chỉ tiêu thiếu hiển thị `Chưa đủ dữ liệu`, không đổi thành 0 hoặc tính chênh lệch.
- **Postconditions:** Bảng chỉ cung cấp dữ liệu, không kết luận món tốt hơn.
- **Traceability / Acceptance Coverage:** FR-60; [AC-60.3, AC-60.5, AC-60.6](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-60).

<a id="uc-60-5"></a>
### UC-60.5 — Xử lý dữ liệu so sánh thiếu
- **Goal / Primary Actor:** Guest/Member hiểu phần dữ liệu nào chưa đủ.
- **Trigger / Preconditions:** Một Recipe Post/nguyên liệu/chỉ tiêu thiếu dữ liệu đáng tin cậy.
- **Main Flow:** Hệ thống đánh dấu đúng trường thiếu và vẫn hiển thị phần có căn cứ.
- **Alternative / Security:** Không suy diễn số 0, không tạo Health Score hay kết luận y tế.
- **Postconditions:** Kết quả so sánh giữ provenance và giới hạn dữ liệu.
- **Traceability / Acceptance Coverage:** FR-60; [AC-60.5–AC-60.8](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-60).

---

<a id="fr-01"></a>
## FR-01 — Hệ thống cho Guest xem và tìm kiếm nội dung đã công khai

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-01).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Guest`: Người dùng vãng lai chưa xác thực truy cập ứng dụng để tìm và xem công thức nấu ăn.
- **Secondary Actor / External System:**
  - `Hệ thống phân phối nội dung công khai`: Đảm bảo tải dữ liệu nhanh chóng và an toàn.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:** Bài công thức đang ở trạng thái công khai (`PUBLISHED`) và không bị ẩn hoặc xóa do vi phạm quy tắc cộng đồng.
- **Trigger:** Guest truy cập vào trang web, sử dụng ô tìm kiếm, hoặc nhấp vào một bài công thức từ danh sách.

#### 5. Luồng sự kiện (Flow of Events)

##### A. Luồng Duyệt danh sách công thức công khai (UC-01.1)
1. **Main Flow:**
   - Bước 1: Guest truy cập trang chủ hoặc trang Khám phá.
   - Bước 2: Hệ thống truy xuất và hiển thị danh sách các bài công thức công khai kèm phân trang theo các chế độ sắp xếp (mặc định Mới nhất, hoặc Được yêu thích nhất, Xem nhiều nhất, Bình luận nhiều nhất, Hoạt động sôi nổi nhất, Thịnh hành). Mỗi thẻ công thức hiển thị: ảnh bìa món ăn (từ `RECIPE_MEDIA` hoặc ảnh mặc định), tiêu đề, loại ăn chay chuẩn, thời gian nấu, huy hiệu tỷ lệ % Like (`👍 {like_percentage}%` theo phong cách Samsung Food hoặc nhãn "Mới" nếu chưa có bình chọn, `RECIPE_REACTION`), số lượt xem (`RECIPE_VIEW`), và thông tin tác giả.
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
   - Bước 2: Hệ thống hiển thị toàn bộ nội dung chi tiết của bài viết: tiêu đề, thư viện ảnh (tối đa 5 ảnh từ `RECIPE_MEDIA` với 1 ảnh bìa), video YouTube nhúng (nếu có), khẩu phần, thời gian chuẩn bị và nấu, danh sách nguyên liệu với số lượng số học và đơn vị chuẩn, hướng dẫn thực hiện chi tiết (`instructions`, 10–5.000 ký tự theo FR-16), tỷ lệ % Like và số lượng Like/Dislike (`RECIPE_REACTION`), số lượt xem (`RECIPE_VIEW`), và thông tin tác giả. Đồng thời, hệ thống ghi nhận lượt xem vào `RECIPE_VIEW` có khử trùng lặp theo phiên (BR-70).
2. **Security & Exception Flow (Yêu cầu đăng nhập khi thực hiện hành động thành viên):**
   - Bước 1: Khi đang ở trang chi tiết bài viết hoặc danh sách, Guest nhấp vào các nút tương tác thành viên: "Like", "Dislike" (`RECIPE_REACTION`), "Bình luận", "Lưu công thức", hoặc "Thêm vào lịch ăn".
   - Bước 2: Hệ thống nhận diện trạng thái chưa đăng nhập của Guest. Guest chỉ được xem tỷ lệ % Like chứ tuyệt đối không được gửi bình chọn Like hoặc Dislike (BR-69).
   - Bước 3: Hệ thống hiển thị hộp thoại thông báo: "Vui lòng đăng nhập hoặc tạo tài khoản để thực hiện chức năng này" kèm nút bấm điều hướng tới trang Đăng nhập / Đăng ký (BR-05, BR-32, BR-69).

#### 6. Hậu điều kiện (Postconditions)
- Nội dung công thức công khai được hiển thị đầy đủ cho Guest.
- Lượt xem hợp lệ được ghi nhận vào `RECIPE_VIEW` nếu ngoài cửa sổ khử trùng lặp 30 phút (BR-70).
- Không phát sinh phiên đăng nhập người dùng hay thay đổi dữ liệu đánh giá/bình luận trên máy chủ.

---

<a id="fr-15"></a>
## FR-15 — Nhúng trình phát YouTube trong bài công thức

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-15).

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

---

<a id="fr-20"></a>
## FR-20 — Thống nhất nguồn hiển thị thẻ món, chi tiết và thực đơn

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-20).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Bài công thức đã được Chuyên gia tạo và lưu trữ trong cơ sở dữ liệu hệ thống (FR-04).
- **Kích hoạt (Trigger):**
  - Người dùng truy cập trang chi tiết công thức, xem thẻ món, mở Lịch ăn tuần, hoặc nhấn nút xuất công thức.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Người dùng nhấp vào một thẻ món trong trang Khám phá hoặc trong Lịch ăn tuần.
  - Bước 2: Hệ thống truy vấn trực tiếp bản thể Recipe Post từ cơ sở dữ liệu dựa trên mã định danh công thức duy nhất (`recipeId`).
  - Bước 3: Hệ thống kết xuất Trang chi tiết công thức (Recipe Detail View) gồm đầy đủ các khối dữ liệu:
    - Tiêu đề, Tác giả (FR-23), Phân loại ăn chay, Thể loại món chuẩn hóa (`dish_category`), Ngày đăng/cập nhật.
    - Thời gian chuẩn bị, Thời gian nấu, Khẩu phần (FR-16).
    - Thư viện ảnh từ `RECIPE_MEDIA` (tối đa 5 ảnh với 1 ảnh bìa, FR-14) và Video YouTube nhúng (FR-15).
    - Tỷ lệ % Like (`like_percentage`) kèm tổng số lượt Like và Dislike theo BR-69; hiển thị hai nút tương tác Like / Dislike cho Member (hỗ trợ toggle off hoặc chuyển đổi phản hồi; Guest nhấn vào thì hiển thị yêu cầu đăng nhập theo BR-05; tác giả bài viết bị chặn tự bình chọn).
    - Tổng số lượt xem hợp lệ (`RECIPE_VIEW`) theo BR-70; ghi nhận lượt xem nếu ngoài cửa sổ khử trùng lặp 30 phút.
    - Mô tả giới thiệu món ăn.
    - Danh sách 1–50 nguyên liệu kèm định lượng số học và đơn vị chuẩn (FR-19).
    - Bảng ước tính 9 chỉ tiêu dinh dưỡng trên 1 khẩu phần (FR-39).
    - Nội dung hướng dẫn chuẩn bị/chế biến (`instructions`, 10–5.000 ký tự) rõ ràng, linh hoạt (FR-16).
    - Nút chức năng "Xuất công thức" hỗ trợ tải tệp PDF hoặc TXT (UC-20.4).
    - Khu vực bình luận và thảo luận cộng đồng nhiều cấp (FR-46).
  - Bước 4: Trang hiển thị hoàn tất trong thời gian $\le 2$ giây (NFR-02).
- **Luồng thay thế (Alternative Flows):**
  - *AF-20.1 (Tác giả cập nhật bài công thức):* Khi tác giả chỉnh sửa bài công thức (FR-44), hệ thống cập nhật trực tiếp trên bản ghi duy nhất đó. Ngay lập tức, trang chi tiết, thẻ món trên Khám phá và các mục trong Lịch ăn của mọi người dùng đều phản ánh thông tin mới mà không cần thao tác đồng bộ phụ nào khác.
  - *AF-20.2 (Bài công thức bị xóa hoặc ẩn - Cơ chế Tombstone):* Khi bài công thức bị xóa bởi tác giả (FR-44) hoặc bị Administrator ẩn (BR-26):
    - Trạng thái bài viết chuyển sang `DELETED` hoặc `HIDDEN`.
    - Bài viết lập tức biến mất khỏi trang Khám phá, Tìm kiếm và pool gợi ý AI.
    - Tại các ô Lịch ăn tuần (FR-09) và Danh sách đã lưu (FR-32) của người dùng khác, hệ thống giữ nguyên liên kết nhưng hiển thị dạng Tombstone: *"Công thức không còn khả dụng"* và vô hiệu hóa link mở chi tiết (BR-35, BR-64).
  - *AF-20.3 (Xuất tệp tin công thức PDF / TXT):* Người dùng nhấn nút "Xuất công thức", chọn định dạng PDF hoặc TXT. Trình duyệt tải xuống tệp tin bản in hoàn chỉnh định dạng trang A4 rõ nét bao gồm tên món, phân loại, nguyên liệu định lượng và hướng dẫn nấu.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-20.1 (Truy cập trực tiếp bài viết không tồn tại hoặc đã bị xóa):* Nếu người dùng truy cập trực tiếp qua URL của bài viết đã bị xóa hoàn toàn, hệ thống phản hồi trang thông báo lỗi 404 thân thiện nêu rõ nội dung không còn khả dụng.
  - *SF-20.1 (Ngăn chặn nhân bản dữ liệu bài viết):* Kiến trúc cơ sở dữ liệu sử dụng khóa ngoại chặt chẽ trỏ về bảng công thức gốc, ngăn ngừa việc nhân bản dữ liệu gây bất nhất quán dữ liệu giữa các phân hệ (NFR-08).

#### 5. Hậu điều kiện (Postconditions)
- Dữ liệu bài công thức luôn bảo đảm tính toàn vẹn và nhất quán trên toàn hệ thống.
- Lượt xem hợp lệ được ghi nhận vào `RECIPE_VIEW` (BR-70).
- Trải nghiệm người dùng không bị gián đoạn khi dữ liệu gốc biến động.

---

<a id="fr-23"></a>
## FR-23 — Hiển thị thông tin tác giả gắn liền với tài khoản

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-23).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Guest` / `Member`: Người xem duyệt xem bài công thức và trang hồ sơ công khai của tác giả.
  - `Chuyên gia` (`Role = EXPERT`) / `Member` (Tác giả / Chủ tài khoản): Người dùng xem và cập nhật thông tin hồ sơ của chính mình; tác giả bài công thức chỉ là Chuyên gia.
  - `Administrator`: Quản trị viên xem xét thông tin tài khoản phục vụ hậu kiểm nội dung vi phạm.
- **Secondary Actor / External System:**
  - `Dịch vụ lưu trữ tệp đám mây (Azure Blob Storage)`: Lưu trữ và phân phối tệp ảnh đại diện người dùng.

#### 4. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:**
  - Để xem thông tin tác giả / hồ sơ công khai: Bài công thức đang ở trạng thái công khai (`PUBLISHED`) hoặc tài khoản người dùng đang tồn tại trong hệ thống.
  - Để chỉnh sửa hồ sơ cá nhân: Người dùng đã đăng nhập với vai trò Member và là chủ sở hữu của tài khoản đó (`Resource Owner`).
- **Trigger:** Người dùng mở trang chi tiết bài công thức, nhấp vào liên kết hồ sơ tác giả, hoặc truy cập vào trang quản lý hồ sơ cá nhân.

#### 6. Luồng sự kiện (Flow of Events)

##### A. Luồng Hiển thị thông tin tác giả trên bài công thức (UC-23.1)
1. **Main Flow:**
   - Bước 1: Người dùng (Guest hoặc Member) mở trang chi tiết bài công thức hợp lệ đang công khai.
   - Bước 2: Tại đầu trang chi tiết, hệ thống hiển thị khối thông tin tác giả: ảnh đại diện, tên hiển thị công khai, và thời điểm công khai bài viết.
   - Bước 3: Người dùng có thể nhấp vào ảnh đại diện hoặc tên tác giả để điều hướng sang trang Hồ sơ công khai của tác giả.

##### B. Luồng Xem trang hồ sơ công khai của thành viên (UC-23.2)
1. **Main Flow:**
   - Bước 1: Người dùng truy cập đường dẫn hồ sơ công khai của một thành viên.
   - Bước 2: Ứng dụng gửi yêu cầu lấy thông tin hồ sơ công khai tới máy chủ.
   - Bước 3: Hệ thống truy xuất dữ liệu, tổng hợp các thông tin công khai: tên hiển thị, ảnh đại diện, giới thiệu ngắn, thời điểm tham gia, và danh sách các bài công thức đang công khai của thành viên.
   - Bước 4: Hệ thống phản hồi dữ liệu chỉ chứa thông tin công khai; tuyệt đối không chứa bất kỳ trường dữ liệu riêng tư nào.
   - Bước 5: Giao diện hiển thị trang hồ sơ công khai của tác giả kèm danh sách các bài công thức.
2. **Alternative Flows:**
   - *Tác giả chưa có bài viết nào:* Danh sách bài viết hiển thị trạng thái rỗng ("Tác giả chưa có bài công thức nào được công khai").
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

---

<a id="fr-46"></a>
## FR-46 — Hiển thị và quản lý bình luận, reply lồng nhiều cấp

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-46).

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

---

<a id="fr-57"></a>
## FR-57 — Tương tác bình chọn Like / Dislike và hiển thị tỷ lệ % hài lòng bài công thức

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-57).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Member đã đăng nhập tài khoản hoạt động (`ACTIVE`) và không phải là tác giả của bài công thức đang xem (FR-03, BR-17, BR-69).
  - Bài công thức ở trạng thái công khai (`PUBLISHED`) (FR-20, BR-07).
- **Kích hoạt (Trigger):**
  - Người dùng nhấp chọn nút Like hoặc Dislike trên khối tương tác của trang chi tiết bài công thức hoặc tương tác nhanh.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow - Member gửi phản hồi Like / Dislike):**
  - Bước 1: Member mở trang chi tiết bài viết công khai (FR-20) và xem khối tương tác Like / Dislike.
  - Bước 2: Member nhấp vào nút "Like" (hoặc "Dislike").
  - Bước 3: Client gửi request kèm `recipeId` và `reactionType` (`LIKE` hoặc `DISLIKE`) lên Backend.
  - Bước 4: Backend kiểm tra an toàn:
    - Xác thực phiên đăng nhập của Member (nếu chưa đăng nhập -> yêu cầu đăng nhập theo AF-57.1).
    - Kiểm tra quyền tác giả: Nếu `userId` trùng với `authorId` của bài viết -> từ chối yêu cầu với mã lỗi HTTP 403 Forbidden và thông báo: *"Tác giả không được tự bình chọn bài viết của chính mình"* (BR-69).
    - Kiểm tra giá trị hợp lệ: Bắt buộc `reactionType` thuộc `{"LIKE", "DISLIKE"}`.
  - Bước 5: Backend kiểm tra bản ghi phản hồi hiện có của Member trên bài viết trong bảng `RECIPE_REACTION`:
    - Nếu chưa từng có phản hồi: Tạo mới bản ghi `RECIPE_REACTION` (`user_id`, `recipe_id`, `reaction_type`, `created_at`, `updated_at`).
    - Nếu đã chọn cùng loại phản hồi trước đó (bấm lại nút Like khi đang Like, hoặc bấm lại Dislike khi đang Dislike): Hủy phản hồi (Toggle off), xóa bản ghi khỏi trạng thái hiệu lực.
    - Nếu đã chọn loại đối nghịch trước đó (đang Like bấm Dislike, hoặc đang Dislike bấm Like): Chuyển đổi trạng thái phản hồi (Switch), cập nhật `reaction_type` sang giá trị mới và cập nhật `updated_at`.
  - Bước 6: Backend tính toán lại số lượng Like, số lượng Dislike và tỷ lệ % Like của bài viết theo công thức:
    $$\text{like\_percentage} = \operatorname{round}\left(\frac{\text{total\_likes}}{\text{total\_likes} + \text{total\_dislikes}} \times 100\right)$$
  - Bước 7: Backend phản hồi kết quả thành công kèm số lượng và tỷ lệ % mới; giao diện cập nhật trạng thái nút bấm (active/inactive), bộ đếm và thanh tỷ lệ % Like của bài viết.
- **Luồng thay thế (Alternative Flows):**
  - *AF-57.1 (Guest tương tác khối bình chọn):* Khi Guest (chưa đăng nhập) nhấn vào nút Like hoặc Dislike, hệ thống chặn thao tác và hiển thị modal đăng nhập/đăng ký kèm thông điệp: *"Vui lòng đăng nhập tài khoản để bình chọn bài công thức này"* (BR-05, BR-69).
  - *AF-57.2 (Bài viết chưa có lượt bình chọn nào):* Khi $(\text{total\_likes} + \text{total\_dislikes}) = 0$, thẻ món hiển thị nhãn `Mới` (New) ở góc trên bên trái ảnh thay vì hiển thị `0%` để tránh gây hiểu lầm tiêu cực cho món ăn mới đăng (BR-69).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-57.1 (Giá trị phản hồi không hợp lệ):* Nếu request gửi giá trị không thuộc `LIKE` hoặc `DISLIKE`, Backend từ chối với mã HTTP 400 Bad Request (NFR-10).
  - *EF-57.2 (Tác giả cố tình tự bình chọn bài của mình):* Backend kiểm tra quyền sở hữu và từ chối với mã HTTP 403 Forbidden (BR-69, NFR-09).
  - *SF-57.1 (Khóa duy nhất chống duplicate reaction):* Ràng buộc duy nhất `UNIQUE(user_id, recipe_id)` trên bảng `RECIPE_REACTION` đảm bảo mỗi user chỉ có đúng 1 bản ghi phản hồi hiệu lực cho một bài viết, ngăn chặn lỗi ghi trùng khi người dùng click liên tiếp hoặc lỗi mạng.

#### 5. Hậu điều kiện (Postconditions)
- Bản ghi `RECIPE_REACTION` được thêm mới, cập nhật hoặc xóa an toàn trong cơ sở dữ liệu.
- Tỷ lệ % Like và các bộ đếm lượt Like, Dislike của `Recipe Post` được tính toán lại chính xác và hiển thị cho toàn bộ người xem.
- Dữ liệu phản hồi sẵn sàng phục vụ bộ lọc sắp xếp Được yêu thích nhất (FR-08) và các thuật toán xếp hạng tương tác (BR-71, BR-72).

---

<a id="fr-58"></a>
## FR-58 — Ghi nhận lượt xem (Recipe Views) và thống kê tương tác đa chiều

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-58).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Bài công thức ở trạng thái công khai (`PUBLISHED`) (FR-20, BR-07).
- **Kích hoạt (Trigger):**
  - Người dùng truy cập trang chi tiết công thức nấu ăn (Recipe Detail View).

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow - Ghi nhận lượt xem):**
  - Bước 1: Người dùng (Guest hoặc Member) truy cập trang chi tiết bài công thức công khai (FR-20).
  - Bước 2: Sau khi trang tải nội dung cơ bản, client gửi một sự kiện ghi nhận lượt xem (View Event) kèm `recipeId` và thông tin định danh người xem (`userId` nếu đã đăng nhập, hoặc `sessionId` lưu trong cookie ẩn danh đối với Guest).
  - Bước 3: Backend kiểm tra bản ghi xem gần nhất của cặp `(viewerId/sessionId, recipeId)`:
    - Nếu đã có bản ghi xem trong vòng 30 phút gần nhất: Hệ thống bỏ qua (không ghi nhận thêm lượt xem mới, bảo đảm tính trung thực theo BR-69).
    - Nếu chưa có bản ghi nào hoặc bản ghi xem gần nhất đã cách hơn 30 phút: Hệ thống tạo bản ghi mới trong bảng `RECIPE_VIEW` (`recipe_id`, `viewer_id`, `session_id`, `viewed_at`), đồng thời tăng giá trị trường `view_count` trên `Recipe Post` lên 1 đơn vị.
  - Bước 4: Backend trả về phản hồi thành công (hoặc xử lý ngầm bất đồng bộ mà không làm chậm việc render trang của người dùng).
  - Bước 5: Số lượt xem cập nhật được phản ánh trên giao diện chi tiết bài viết và các thẻ bài viết tương ứng.
  - Bước 6: Định kỳ (hoặc theo truy vấn tổng hợp), hệ thống tính toán số lượt xem trong 24 giờ qua ($V_{24h}$), 7 ngày qua ($V_{7d}$), 30 ngày qua ($V_{30d}$) từ bảng `RECIPE_VIEW` để phục vụ các bộ lọc sắp xếp của FR-08.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-58.1 (Người dùng tải lại trang liên tục - F5 spam):* Khi người dùng cố ý tải lại trang liên tục trong thời gian ngắn, hệ thống phát hiện tất cả các request này đều nằm trong cửa sổ 30 phút của cùng một session, do đó chỉ tính đúng 1 lượt xem duy nhất.
  - *SF-58.1 (Hiệu năng xử lý sự kiện xem cao tải):* Cơ chế ghi nhận lượt xem được thiết kế tối ưu (sử dụng chỉ mục phù hợp trên `RECIPE_VIEW` hoặc tác vụ nền), đảm bảo không gây khóa bảng (table locking) hoặc làm chậm thời gian tải trang chi tiết bài viết (đáp ứng NFR-02 $\le 2$ giây và NFR-05).

#### 5. Hậu điều kiện (Postconditions)
- Bản ghi `RECIPE_VIEW` mới được ghi nhận an toàn nếu thỏa mãn điều kiện khử trùng lặp 30 phút.
- Bộ đếm lượt xem toàn thời gian `view_count` của Recipe Post được tăng chính xác.
- Dữ liệu lượt xem theo các khung thời gian 24h, 7d, 30d sẵn sàng phục vụ các chức năng sắp xếp và xếp hạng của hệ thống.

---

<a id="fr-59"></a>
## FR-59 — Theo dõi, bỏ theo dõi và xem quan hệ theo dõi giữa các Member

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-59).

#### 3. Tiền điều kiện & Kích hoạt
- Member đang đăng nhập bằng tài khoản hoạt động.
- Tài khoản đích tồn tại, đang hoạt động và không phải chính tài khoản hiện tại.
- Member kích hoạt nút `Theo dõi` hoặc `Đang theo dõi` trên hồ sơ công khai.

#### 4. Luồng xử lý
- **Follow:** Backend xác thực Member, kiểm tra tài khoản nguồn/đích và BR-75; nếu quan hệ chưa tồn tại thì tạo `USER_FOLLOW`; trả về `following = true` cùng bộ đếm mới.
- **Follow lặp:** Nếu quan hệ đã tồn tại, hệ thống không tạo bản ghi thứ hai và trả về trạng thái hiện tại theo cơ chế idempotent.
- **Unfollow:** Backend xóa quan hệ của đúng cặp có hướng; nếu quan hệ đã không tồn tại, hệ thống vẫn trả về `following = false` mà không phát sinh lỗi nghiệp vụ.
- **Xem danh sách:** Hệ thống trả danh sách followers hoặc following có phân trang, chỉ gồm dữ liệu hồ sơ công khai theo BR-18.

#### 5. Hậu điều kiện
- Mỗi cặp có hướng `(follower_user_id, followed_user_id)` có tối đa một bản ghi hiệu lực.
- Bộ đếm followers/following phản ánh dữ liệu `USER_FOLLOW`; không dùng số do client tự gửi.
- Follow không cấp thêm quyền truy cập email, thông tin đăng nhập, hồ sơ dinh dưỡng hoặc dữ liệu riêng tư.

---

<a id="fr-60"></a>
## FR-60 — So sánh hai Recipe Post công khai theo nguyên liệu và dinh dưỡng

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-60).

#### 3. Tiền điều kiện & Kích hoạt

- Có ít nhất hai Recipe Post khác nhau đang ở trạng thái `PUBLISHED`.
- Dữ liệu tổng quan/nguyên liệu lấy từ cùng nguồn Recipe Post của FR-20; dữ liệu dinh dưỡng lấy từ kết quả FR-39.
- Người dùng nhấn `So sánh` trên Recipe Card/Recipe Detail hoặc mở giao diện so sánh và chọn công thức đầu tiên.

#### 4. Luồng xử lý

##### A. Chọn hai công thức

1. Người dùng chọn Recipe A từ thẻ, trang chi tiết hoặc công cụ tìm kiếm.
2. Hệ thống mở giao diện so sánh và yêu cầu chọn Recipe B.
3. Người dùng tìm/chọn Recipe B; hệ thống loại Recipe A khỏi lựa chọn hợp lệ để ngăn chọn trùng.
4. Hệ thống kiểm tra cả hai bài vẫn `PUBLISHED` trước khi hiển thị kết quả.

##### B. So sánh thông tin và nguyên liệu

1. Hệ thống hiển thị song song tên món, ảnh bìa, tác giả Chuyên gia, loại ăn chay, `dish_category`, khẩu phần gốc, thời gian chuẩn bị/nấu và tỷ lệ Like của mỗi công thức.
2. Hệ thống hiển thị danh sách nguyên liệu hai bên với số lượng và đơn vị nguyên bản.
3. Nguyên liệu liên kết cùng một `ingredient_id` có thể được đặt cùng hàng để đối chiếu. Nguyên liệu tự do hoặc chỉ giống tên không bị tự động hợp nhất.
4. Nếu đơn vị khác nhau và không có quy tắc quy đổi được duyệt, hệ thống giữ nguyên đơn vị và ghi rõ không thể đối chiếu định lượng trực tiếp.

##### C. So sánh dinh dưỡng trên một khẩu phần

1. Hệ thống lấy chín chỉ tiêu trên một khẩu phần của Recipe A và Recipe B từ FR-39.
2. Với mỗi chỉ tiêu có dữ liệu ở cả hai bên, hệ thống hiển thị giá trị A, giá trị B và chênh lệch tuyệt đối `A - B` theo cùng đơn vị.
3. Hệ thống hiển thị rõ số khẩu phần gốc của từng công thức và tuyên bố rằng khẩu phần của hai món có thể không có cùng khối lượng thành phẩm.
4. Hệ thống không hiển thị Natri/muối, chênh lệch phần trăm, Health Score hoặc kết luận món nào tốt hơn/lành mạnh hơn.

##### D. Thay lựa chọn và kết thúc

1. Người dùng có thể thay Recipe A hoặc Recipe B; hệ thống tính lại toàn bộ bảng từ lựa chọn mới.
2. Khi đóng hoặc rời giao diện, hệ thống không lưu bản ghi lịch sử so sánh.

#### 5. Luồng thay thế, ngoại lệ và dữ liệu thiếu

- Nếu một bài không còn công khai trong lúc so sánh, hệ thống gỡ bài khỏi kết quả, thông báo nội dung không còn khả dụng và yêu cầu chọn bài khác.
- Nếu một bên thiếu dữ liệu dinh dưỡng, hệ thống hiển thị `Chưa đủ dữ liệu` cho đúng chỉ tiêu/bên bị ảnh hưởng; không dùng `0` và không tính chênh lệch cho cặp đó.
- Nếu chỉ có một công thức được chọn, hệ thống hiển thị trạng thái chờ chọn công thức thứ hai và chưa trình bày kết luận so sánh.
- Nếu không còn công thức thứ hai phù hợp, hệ thống thông báo rõ và cho phép quay lại khám phá.

#### 6. Hậu điều kiện

- Người dùng xem được kết quả đối chiếu hiện thời mà không tạo dữ liệu nghiệp vụ mới trong database.
- Việc so sánh không thay đổi Recipe Post, danh sách đã lưu, Meal Plan, hồ sơ dinh dưỡng hoặc dữ liệu tương tác.

---
