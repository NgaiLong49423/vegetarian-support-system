> **Document:** Use Case Specifications — M09
> **File:** `docs/requirements/use-cases/administration.md`
> **Version:** v2.0.1
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-27
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M09

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns the required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-06"></a>
## FR-06 — Administrator xử lý báo cáo và quản lý nội dung hậu kiểm

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-06).

### Use Case navigation

- [UC-06.1 — Quản lý danh sách nội dung bị báo cáo hậu kiểm](#uc-06-1)
- [UC-06.2 — Thực hiện hành động hậu kiểm](#uc-06-2)

---

<a id="uc-06-1"></a>
### UC-06.1 — Quản lý danh sách nội dung bị báo cáo hậu kiểm

#### Goal
Giúp Administrator xem và ưu tiên các báo cáo nội dung cần được hậu kiểm.

#### Primary Actor
`Administrator`.

#### Trigger
Administrator mở khu vực Quản lý hậu kiểm (Moderation Portal) trong trang Quản trị.

#### Preconditions
Administrator đã đăng nhập bằng tài khoản có vai trò `ADMIN`.

#### Main Flow
1. Administrator mở mục “Hậu kiểm nội dung”.
2. Hệ thống hiển thị hàng đợi báo cáo đối với thành viên, Recipe Post, bình luận và danh mục thuộc phạm vi FR-06, gồm đối tượng bị báo cáo, người bị báo cáo (nếu có), trạng thái xử lý, số lượt báo cáo tích lũy và lý do báo cáo phổ biến nhất.
3. Administrator có thể lọc theo loại nội dung hoặc mức độ ưu tiên, hoặc tìm kiếm theo tiêu đề bài viết/tên tài khoản.

#### Postconditions
Danh sách và thông tin báo cáo được hiển thị để Administrator lựa chọn nội dung cần xem xét; UC này chưa thay đổi trạng thái báo cáo, nội dung hoặc tài khoản.

#### Traceability
- **Parent FR:** [FR-06](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-06).
- **Relevant BR:** [BR-23](../srs/BUSINESS-RULES.md#br-23) (báo cáo là tín hiệu cần xem xét, không phải kết luận); [BR-28](../srs/BUSINESS-RULES.md#br-28) (quyền riêng tư của báo cáo).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (phân quyền truy cập khu vực quản trị).

---

<a id="uc-06-2"></a>
### UC-06.2 — Thực hiện hành động hậu kiểm

#### Goal
Cho phép Administrator xem xét báo cáo và ghi nhận quyết định hậu kiểm thủ công đối với nội dung hoặc tài khoản liên quan.

#### Primary Actor
`Administrator`.

#### Supporting Actors
Tác giả hoặc thành viên có nội dung/tài khoản bị xử lý là bên nhận thông báo khi quyết định hậu kiểm yêu cầu thông báo.

#### Trigger
Administrator chọn một mục trong hàng đợi báo cáo để xem xét.

#### Preconditions
Administrator đã đăng nhập bằng tài khoản có vai trò `ADMIN`; báo cáo được chọn để xử lý.

#### Main Flow
1. Administrator xem nội dung bị báo cáo và chi tiết phản ánh từ cộng đồng.
2. Administrator tự đưa ra quyết định thủ công phù hợp: ẩn nội dung vi phạm, cảnh cáo tác giả, khóa tài khoản khi tái phạm/vi phạm nghiêm trọng, hoặc bác bỏ báo cáo và giữ nguyên trạng thái nội dung.
3. Administrator nhập kết luận giải thích lý do xử lý.
4. Hệ thống cập nhật trạng thái nội dung, tài khoản và báo cáo theo quyết định; lưu vết kiểm toán và thông báo kết quả cho bên liên quan khi áp dụng.

#### Postconditions
Quyết định hậu kiểm và lý do được lưu lại trong lịch sử kiểm toán; trạng thái liên quan phản ánh quyết định của Administrator.

#### Traceability
- **Parent FR:** [FR-06](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-06).
- **Relevant BR:** [BR-23](../srs/BUSINESS-RULES.md#br-23) (báo cáo không tự quyết định vi phạm); [BR-26](../srs/BUSINESS-RULES.md#br-26) (thẩm quyền và lưu kết luận/lịch sử xử lý); [BR-28](../srs/BUSINESS-RULES.md#br-28) (quyền riêng tư của báo cáo).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (phân quyền truy cập và thao tác quản trị).

---
