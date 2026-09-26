> **Document:** Use Case Specifications — M09
> **File:** `docs/requirements/use-cases/administration.md`
> **Version:** v2.0.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-26
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M09

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns the required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-06"></a>
## FR-06 — Administrator xử lý báo cáo và quản lý nội dung hậu kiểm

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-06).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Administrator`: Quản trị viên hệ thống có toàn quyền thực hiện các chế tài hậu kiểm.

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

---
