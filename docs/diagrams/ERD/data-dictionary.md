> **Document:** Data Dictionary & Traceability Matrix — Mâm Xanh
> **File:** `docs/diagrams/ERD/data-dictionary.md`
> **Version:** v0.3.0
> **Created:** 2026-09-23
> **Last Updated:** 2026-09-23
> **Status:** Draft
> **Related Docs:** `docs/diagrams/ERD/README.md`, `docs/requirements/SRS.md`, `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md`, `docs/requirements/srs/BUSINESS-RULES.md`

# Data Dictionary & Traceability Matrix

Tài liệu này là bảng tổng hợp bắt buộc của [Issue #63](https://github.com/NgaiLong49423/vegetarian-support-system/issues/63) mục D: **đúng một bảng, một dòng cho mỗi cột**, nhóm theo table để thành viên Backend tra cứu trực tiếp.

Nguồn đối chiếu: Conceptual ERD 22 thực thể / 36 connector trong [ERD README](README.md), requirement baseline trong [SRS](../../requirements/SRS.md) v1.10.0, và cấu trúc cột kế thừa từ `logical-erd-v0.1.0.drawio`.

## 1. Trạng thái và cách dùng

| Pha | Người phụ trách | Phạm vi trong tài liệu này |
|---|---|---|
| Pha 1 — Logical | Nguyễn Hải Dương | `Table`, `Table purpose`, `Column`, `Business meaning`, `Logical type`, `Relationship/Cardinality`, `FR`, `BR`, `UC/AC`, `Security/Privacy note` |
| Pha 2 — Physical | Trương Văn Khải | `SQL Server type`, `Length/Precision`, `Nullable`, `Default`, `PK`, `FK reference`, `UNIQUE`, `CHECK`, `Index` |

Các cột thuộc pha 2 hiện để `—` **có chủ đích**, không phải bỏ sót. Pha 2 chỉ bắt đầu sau khi pha 1 được chốt và các câu hỏi ở mục 2 có câu trả lời.

Ký hiệu trong tài liệu:

- `🆕` — cột hoặc bảng chưa có trong `logical-erd-v0.1.0.drawio`, phải bổ sung khi dựng v1.0.0.
- `🗑` — cột hoặc bảng phải xóa khỏi sơ đồ logical.
- `Technical design` — trường kỹ thuật không bắt nguồn trực tiếp từ requirement, có ghi lý do.

## 2. Quyết định đã chốt

Toàn bộ điểm mở ảnh hưởng tới table, column, key hoặc constraint đã được Tech Lead chốt ngày **23/09/2026**. Pha 1 không còn `TBD`.

| ID | Quyết định | Người chốt | Ngày chốt |
|---|---|---|---|
| `Q1` | `RECIPE_POST.dish_category` dùng **danh sách cố định**, không nhập tự do và không tạo lại bảng `Category`. Lưu **technical code** trong cột có `CHECK` constraint, gồm **11 giá trị**: `NOODLE_SOUP`, `STIR_FRY`, `HOT_POT`, `BRAISED`, `SOUP`, `FRIED`, `STEAMED`, `SALAD`, `ROLL`, `GRILLED`, `DESSERT`. Frontend hiển thị nhãn tiếng Việt tương ứng. | Ngô Gia Long | 23/09/2026 |
| `Q2` | `RECIPE_POST` **lưu** `like_count`, `dislike_count` và `view_count`, mặc định `0` và không âm. `RECIPE_REACTION` / `RECIPE_VIEW` vẫn là **dữ liệu gốc**. Like/Dislike cập nhật bộ đếm **đồng bộ trong cùng transaction**; riêng `view_count` cập nhật **bất đồng bộ** theo ARCHITECTURE.md mục 6. `like_percentage` **tính khi đọc, không lưu thành cột**. | Ngô Gia Long | 23/09/2026 |

### Các điểm đã có sẵn câu trả lời trong tài liệu

| Điểm | Kết luận | Nguồn |
|---|---|---|
| Các bước nấu lưu ở đâu | Không còn bảng `RECIPE_STEP`; hướng dẫn lưu tự do tại `RECIPE_POST.instructions`, bắt buộc 10–5.000 ký tự | FR-16, BR-19, AC-16.5 |
| `EXPERT_APPLICATION.status` | `PENDING` / `APPROVED` / `REJECTED` | FR-05 |
| `RECIPE_REACTION.reaction_type` | `LIKE` / `DISLIKE`; 1 bản ghi/user/bài; toggle được; tác giả không tự vote | FR-57, BR-69 |
| Chín chỉ tiêu dinh dưỡng | Năng lượng, Đạm, Carbohydrate, Béo, Xơ, Canxi, Sắt, Vitamin B12, **Kẽm**. Không có natri | BR-44, BR-52 |
| `MEAL_PLAN` theo ngày hay tuần | **Theo tuần** — lưới 7 ngày Thứ Hai đến Chủ Nhật, mỗi ngày đúng 3 bữa cố định. Cột `week_start_date` hiện tại là đúng. Cách diễn đạt "theo ngày hoặc theo tuần" ở ERD README mục 3 là lỏng, nên sửa cho khớp FR-09 | FR-09, BR-36 |
| `vegetarian_type` có 4 hay 5 giá trị | **Không mâu thuẫn — là ba cột khác nhau.** `RECIPE_POST.vegetarian_type` và `USER.vegetarian_type` dùng **4** giá trị (Vegan, Lacto, Ovo, Lacto-Ovo). Riêng `EXPERT_APPLICATION.vegetarian_type` là "trường phái chay chuyên sâu" của cá nhân, dùng **5** giá trị (thêm "Chay thực dưỡng dưỡng sinh") | FR-16 (4), FR-51 (4), FR-05 (5) |

## 3. Delta từ `logical-erd-v0.1.0.drawio`

Đọc trực tiếp từ XML: file hiện có **23 bảng**, baseline cần **22**.

| Hành động | Đối tượng | Căn cứ |
|---|---|---|
| 🗑 Xóa bảng | `RECIPE_STEP` | ERD README: "Loại bỏ bảng riêng theo Phương án B" |
| 🗑 Xóa bảng | `CATEGORY`, `RECIPE_CATEGORY` | ERD README: "Loại bỏ hoàn toàn bảng danh mục động và bảng liên kết" |
| 🆕 Thêm bảng | `EXPERT_APPLICATION` | Thực thể 21, quy trình xét duyệt Chuyên gia |
| 🆕 Thêm bảng | `USER_FOLLOW` | Thực thể 22, FR-59 / BR-75 `ACTIVE` 23/09/2026 |
| 🆕 Thêm cột | `RECIPE_POST.instructions` | Thay thế `RECIPE_STEP`; bắt buộc 10–5.000 ký tự |
| 🆕 Thêm cột | `RECIPE_POST.dish_category` | Thay thế bảng `CATEGORY`; bắt buộc, `CHECK` 11 technical code |
| 🆕 Thêm cột | `RECIPE_POST.like_count`, `dislike_count`, `view_count` | Quyết định Q2 ngày 23/09/2026; mặc định `0`, không âm |
| 🆕 Thêm cột | `INGREDIENT.zinc_mg_100g` | Chỉ tiêu thứ 9 |
| 🗑 Xóa cột | `INGREDIENT.sodium_mg_100g` | Natri không thuộc chín chỉ tiêu |
| ✅ Đã áp dụng | Toàn bộ delta trên đã được áp dụng vào `logical-erd-v1.0.0.drawio` | Dựng ngày 23/09/2026 từ XML của v0.1.0 |

`RECIPE_REACTION` đã được chuyển đổi đúng trong file `.drawio` (có cột `reaction_type`) — **không cần làm lại**, chỉ ảnh PNG gây hiểu nhầm.

## 4. Bảng tổng hợp

Cột pha 2 (`SQL Server type` → `Index`) được gộp thành một cột `Physical` để giữ bảng đọc được; Trương Văn Khải tách thành các cột riêng khi vào pha 2 theo đúng danh mục Issue #63 mục D.

### 4.1 USER

**Mục đích:** Tài khoản, vai trò, hồ sơ cá nhân và chỉ số thể trạng của toàn bộ người dùng đã đăng ký. Đã gộp thực thể `User Profile`.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `user_id` | Định danh tài khoản | Identifier | — | PK; đầu `1` của 12 quan hệ xuất phát từ `USER` | FR-03 | — | UC-03.1 | — |
| `email` | Email đăng nhập, định danh duy nhất | Text, unique | — | — | FR-03 | BR-24 | UC-03.1 | Dữ liệu cá nhân |
| `password_hash` | Mật khẩu đã băm BCrypt | Text | — | — | FR-03 | — | AC-03.1 | **Tuyệt đối không log**; NFR-06 |
| `google_subject` | `sub` từ Google ID Token, khóa định danh Google ổn định | Text, unique, optional | — | — | FR-03 | — | UC-03.8 | Dữ liệu cá nhân |
| `display_name` | Tên hiển thị công khai | Text | — | — | FR-23 | BR-18 | — | Công khai theo BR-18 |
| `avatar_url` | Ảnh đại diện | Text, optional | — | — | FR-23 | BR-18 | — | Công khai |
| `bio` | Giới thiệu ngắn của tác giả | Text, optional | — | — | FR-23 | BR-18 | — | Công khai |
| `role` | Vai trò: `CUSTOMER` / `EXPERT` / `ADMIN` | Enum | — | Quyết định quyền đăng bài và quản trị | FR-03, FR-05 | BR-17, BR-26 | — | Quyết định phân quyền, NFR-09 |
| `account_status` | Trạng thái tài khoản (hoạt động / bị khóa) | Enum | — | — | FR-03 | BR-26 | — | Chỉ Admin đổi được |
| `email_verified` | Đã xác minh email hay chưa | Boolean | — | — | FR-03 | — | UC-03.2 | — |
| `vegetarian_type` | Trường phái ăn chay của người dùng: Vegan / Lacto / Ovo / Lacto-Ovo (4 giá trị) | Enum | — | — | FR-31 | BR-30 | — | — |
| `cuisine_preference` | Khẩu vị vùng miền ưa thích | Text, optional | — | — | FR-31 | BR-30 | — | — |
| `preferred_difficulty` | Độ khó món ăn ưa thích | Enum, optional | — | — | FR-31 | BR-30 | — | — |
| `max_cooking_time_min` | Thời gian nấu tối đa chấp nhận được | Integer, optional | — | — | FR-31 | BR-30 | — | — |
| `biological_sex` | Giới tính sinh học, dùng cho tính nhu cầu dinh dưỡng | Enum, optional | — | — | FR-35 | BR-42 | — | **Dữ liệu sức khỏe**, NFR-08, NFR-20 |
| `height_cm` | Chiều cao, hợp lệ 100–250 | Decimal, optional | — | — | FR-35 | BR-39 | AC-35.3 | **Dữ liệu sức khỏe** |
| `weight_kg` | Cân nặng, hợp lệ 30–300 | Decimal, optional | — | — | FR-35 | BR-39 | AC-35.3 | **Dữ liệu sức khỏe** |
| `activity_level` | Mức độ vận động | Enum, optional | — | — | FR-35 | BR-39 | — | **Dữ liệu sức khỏe** |
| `pregnant` | Đang mang thai — thuộc nhóm loại trừ khỏi tính dinh dưỡng | Boolean | — | — | FR-38 | BR-42 | — | **Dữ liệu sức khỏe nhạy cảm** |
| `breastfeeding` | Đang cho con bú — nhóm loại trừ | Boolean | — | — | FR-38 | BR-42 | — | **Dữ liệu sức khỏe nhạy cảm** |
| `therapeutic_diet_required` | Cần chế độ ăn điều trị — nhóm loại trừ | Boolean | — | — | FR-38 | BR-42 | — | **Dữ liệu sức khỏe nhạy cảm** |
| `nutrition_scope_confirmed` | Đã xác nhận phạm vi hỗ trợ trước khi dùng chức năng dinh dưỡng | Boolean | — | — | FR-38 | BR-41, BR-42 | — | Bằng chứng consent, NFR-20 |
| `reply_email_enabled` | Bật nhận email khi có reply | Boolean | — | — | FR-49 | — | — | — |
| `created_at` | Thời điểm tạo tài khoản | Timestamp | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | — | — | Technical design — audit | — | — | — |

### 4.2 USER_FOLLOW 🆕

**Mục đích:** Quan hệ theo dõi **có hướng** giữa hai Member. A theo dõi B không làm B tự động theo dõi A.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `follower_user_id` | Người thực hiện theo dõi | Identifier, FK | — | `USER` 1 → `USER_FOLLOW` 0..* (vai trò follower) | FR-59 | BR-75 | — | — |
| `followed_user_id` | Người được theo dõi | Identifier, FK | — | `USER` 1 → `USER_FOLLOW` 0..* (vai trò followed) | FR-59 | BR-75 | — | — |
| `created_at` | Thời điểm bắt đầu theo dõi | Timestamp | — | — | Technical design — audit | — | — | — |

**Ràng buộc không vẽ được bằng đường nối:** khóa chính kép `(follower_user_id, followed_user_id)` chống trùng cặp; điều kiện `follower_user_id ≠ followed_user_id` chống tự theo dõi. Cả hai FK cùng trỏ về `USER` nên **không được** dùng cascade (xem mục 5).

### 4.3 USER_INGREDIENT_PREFERENCE

**Mục đích:** Sở thích, kiêng kỵ và dị ứng nguyên liệu của người dùng.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `preference_id` | Định danh bản ghi | Identifier | — | PK | FR-31 | — | — | — |
| `user_id` | Người sở hữu khai báo | Identifier, FK | — | `USER` 1 → 0..* | FR-31 | BR-30 | — | Chỉ chủ sở hữu xem được |
| `ingredient_id` | Nguyên liệu chuẩn được tham chiếu | Identifier, FK, optional | — | `INGREDIENT` 1 → 0..* | FR-31 | BR-13 | — | — |
| `custom_ingredient_name` | Tên nguyên liệu tự nhập khi chưa có trong từ điển | Text, optional | — | — | FR-19 | BR-13 | — | — |
| `preference_type` | `ALLERGY` / `AVOID` / `DISLIKE` | Enum | — | — | FR-31 | BR-13 | — | `ALLERGY` là **dữ liệu sức khỏe** |
| `created_at` | Thời điểm khai báo | Timestamp | — | — | Technical design — audit | — | — | — |

### 4.4 EXPERT_APPLICATION 🆕

**Mục đích:** Đơn đăng ký cấp quyền Chuyên gia của Customer, theo format văn bản. **Tuyệt đối không lưu tệp chứng chỉ vật lý.**

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `application_id` | Định danh đơn | Identifier | — | PK | FR-05 | BR-21 | — | — |
| `user_id` | Customer nộp đơn | Identifier, FK | — | `USER` 1 → `EXPERT_APPLICATION` 0..* | FR-05 | BR-21 | — | — |
| `bio_experience` | Mô tả kinh nghiệm bản thân | Text | — | — | FR-05 | BR-21 | — | — |
| `vegetarian_type` | Trường phái chay chuyên sâu của cá nhân: 4 giá trị trên **cộng** "Chay thực dưỡng dưỡng sinh" — tổng 5 giá trị | Enum | — | — | FR-05 | — | — | — |
| `sample_recipe_summary` | Tóm tắt công thức mẫu để chứng minh năng lực | Text | — | — | FR-05 | BR-21 | — | — |
| `portfolio_url` | Liên kết hồ sơ ngoài | Text, optional | — | — | FR-05 | — | — | Không tải tệp lên hệ thống |
| `status` | `PENDING` / `APPROVED` / `REJECTED` | Enum | — | — | FR-05 | BR-21, BR-22 | — | Chỉ Admin đổi được |
| `admin_note` | Lý do duyệt hoặc từ chối | Text, optional | — | — | FR-05 | BR-22 | — | Chỉ Admin xem |
| `reviewed_by` | Admin đã xử lý đơn | Identifier, FK, optional | — | `USER` (vai trò ADMIN) | FR-05 | BR-26 | — | — |
| `reviewed_at` | Thời điểm xử lý | Timestamp, optional | — | — | FR-05 | — | — | — |
| `created_at` | Thời điểm nộp đơn | Timestamp | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | — | — | Technical design — audit | — | — | — |

**Ràng buộc nghiệp vụ:** mỗi Customer chỉ được có tối đa một đơn ở trạng thái `PENDING` tại một thời điểm. Khi `status` chuyển `APPROVED`, `USER.role` đổi thành `EXPERT`.

### 4.5 RECIPE_POST

**Mục đích:** Bài viết công thức nấu ăn chay do Chuyên gia làm tác giả. Không dùng bảng `Category`, không dùng bảng `Recipe Step`.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `recipe_id` | Định danh bài công thức | Identifier | — | PK; đầu `1` của 8 quan hệ xuất phát | FR-04 | — | — | — |
| `author_id` | Tác giả, gắn với tài khoản Chuyên gia | Identifier, FK | — | `USER` 1 → `RECIPE_POST` 0..*; bài bắt buộc thuộc đúng 1 tác giả | FR-04, FR-23 | BR-17 | — | — |
| `title` | Tiêu đề, 3–120 ký tự | Text | — | — | FR-16 | BR-19 | AC-16.1 | — |
| `description` | Mô tả giới thiệu, tối đa 2.000 ký tự, tùy chọn | Text, optional | — | — | FR-16 | BR-20 | — | — |
| `instructions` 🆕 | Hướng dẫn chế biến dạng văn bản tự do, **bắt buộc** 10–5.000 ký tự sau trim; không ép phân rã theo bước | Text | — | Thay thế bảng `RECIPE_STEP` đã bị loại bỏ | FR-16 | BR-19 | AC-16.5 | — |
| `dish_category` 🆕 | Thể loại món, **bắt buộc** khi công khai. Lưu technical code, 11 giá trị: `NOODLE_SOUP`, `STIR_FRY`, `HOT_POT`, `BRAISED`, `SOUP`, `FRIED`, `STEAMED`, `SALAD`, `ROLL`, `GRILLED`, `DESSERT`. Frontend hiển thị nhãn tiếng Việt | Enum, `CHECK` 11 giá trị | — | Thay thế bảng `CATEGORY` đã bị loại bỏ | FR-07, FR-08 | — | AC-07.3, AC-08.3 | — |
| `vegetarian_type` | Loại ăn chay của món: Vegan / Lacto / Ovo / Lacto-Ovo (4 giá trị) | Enum | — | — | FR-07, FR-08 | — | AC-07.3 | — |
| `difficulty` | Độ khó chế biến | Enum | — | — | FR-08 | — | — | — |
| `servings` | Số khẩu phần gốc, hợp lệ 1–50 | Integer | — | Cơ sở quy đổi dinh dưỡng theo khẩu phần | FR-16 | BR-43 | AC-16.2 | — |
| `prep_time_min` | Thời gian chuẩn bị, 0–1.440 phút | Integer | — | — | FR-16 | BR-19 | — | — |
| `cook_time_min` | Thời gian nấu, 0–1.440 phút; tổng hai mốc phải > 0 | Integer | — | — | FR-16 | BR-19 | — | — |
| `youtube_url` | Liên kết YouTube để nhúng, tối đa 1 | Text, optional | — | 0..1 mỗi bài | FR-15 | BR-10 | — | Không tải tệp video lên |
| `status` | Trạng thái công khai / bị ẩn sau kiểm duyệt | Enum | — | — | FR-25, FR-28 | BR-07, BR-27 | — | Chỉ Admin ẩn được |
| `published_at` | Thời điểm công khai | Timestamp, optional | — | — | FR-25 | BR-07 | — | — |
| `created_at` | Thời điểm tạo | Timestamp | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm sửa gần nhất | Timestamp | — | — | FR-44 | BR-62 | — | — |
| `like_count` 🆕 | Tổng lượt Like, mặc định `0`, **không âm**. Cập nhật **đồng bộ trong cùng transaction** với `RECIPE_REACTION` | Integer | — | Bộ đếm dẫn xuất từ `RECIPE_REACTION` | FR-08, FR-17, FR-57 | BR-69, BR-71, BR-72 | — | — |
| `dislike_count` 🆕 | Tổng lượt Dislike, mặc định `0`, **không âm**. Cập nhật **đồng bộ trong cùng transaction** với `RECIPE_REACTION` | Integer | — | Bộ đếm dẫn xuất từ `RECIPE_REACTION` | FR-08, FR-17, FR-57 | BR-69 | — | — |
| `view_count` 🆕 | Tổng lượt xem, mặc định `0`, **không âm**. Cập nhật **bất đồng bộ** theo ARCHITECTURE.md mục 6, không nghẽn luồng đọc | Integer | — | Bộ đếm dẫn xuất từ `RECIPE_VIEW` | FR-08, FR-17, FR-58 | BR-70, BR-71, BR-72 | — | — |

**Ngoại lệ 3NF có chủ đích (Tech Lead chốt 23/09/2026):** ba cột `like_count`, `dislike_count`, `view_count` là dữ liệu **dẫn xuất** từ `RECIPE_REACTION` và `RECIPE_VIEW` — vi phạm 3NF một cách có kiểm soát để phục vụ 6 chế độ sắp xếp bằng truy vấn SQL thuần (BR-71, BR-72) và để hiển thị trên **mọi thẻ món** ở trang danh sách (FR-17). Hai bảng sự kiện vẫn là **nguồn dữ liệu gốc**; khi lệch phải tính lại từ chúng.

**`like_percentage` không phải là cột** — tính khi đọc từ `like_count` và `dislike_count`; hiển thị nhãn `Mới` khi chưa có lượt bình chọn nào (FR-17, BR-69).

### 4.6 RECIPE_MEDIA

**Mục đích:** 0–5 ảnh minh họa trên Azure Blob Storage, có thứ tự hiển thị và **đúng một** ảnh bìa.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `media_id` | Định danh ảnh | Identifier | — | PK | FR-14 | — | — | — |
| `recipe_id` | Bài công thức chứa ảnh | Identifier, FK | — | `RECIPE_POST` 1 → `RECIPE_MEDIA` 0..* | FR-14 | BR-20 | — | — |
| `blob_url` | Đường dẫn tệp trên Azure Blob Storage | Text | — | — | FR-14 | BR-11 | — | **Không ghi SAS URL vào log** |
| `mime_type` | Định dạng ảnh: JPEG / PNG / WebP | Enum | — | — | FR-14 | BR-11 | — | — |
| `display_order` | Thứ tự hiển thị trong thư viện | Integer | — | — | FR-14 | BR-20 | — | — |
| `is_cover` | Cờ ảnh bìa | Boolean | — | — | FR-14, FR-17 | BR-20 | — | — |

**Ràng buộc không vẽ được bằng đường nối:** mỗi bài có **tối đa 5** ảnh (ràng buộc tầng ứng dụng, database không ép được) và **đúng 1** ảnh `is_cover = true` khi có ảnh (ép được bằng filtered unique index — pha 2).

### 4.7 RECIPE_INGREDIENT

**Mục đích:** Dòng nguyên liệu trong một bài công thức, kèm định lượng số dương và đơn vị đo chuẩn.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `recipe_ingredient_id` | Định danh dòng nguyên liệu | Identifier | — | PK | FR-19 | — | — | — |
| `recipe_id` | Bài công thức chứa dòng này | Identifier, FK | — | `RECIPE_POST` 1 → `RECIPE_INGREDIENT` **1..\*** (bắt buộc ≥1, tối đa 50) | FR-16, FR-19 | BR-19 | AC-16.2 | — |
| `ingredient_id` | Liên kết tới nguyên liệu chuẩn | Identifier, FK, **optional** | — | `INGREDIENT` 0..1 → 0..*; cho phép để trống để tác giả nhập tên tự do | FR-19 | BR-12, BR-13 | — | — |
| `unit_id` | Đơn vị đo, **bắt buộc** | Identifier, FK | — | `RECIPE_INGREDIENT` 0..* → `UNIT` **1** | FR-19 | BR-14, BR-73 | — | — |
| `custom_ingredient_name` | Tên nguyên liệu tự nhập khi chưa có trong từ điển | Text, optional | — | — | FR-19 | BR-12 | — | — |
| `quantity` | Định lượng, **bắt buộc là số thực dương > 0**; cấm tuyệt đối giá trị phi số học như "vừa đủ" | Decimal | — | — | FR-19 | BR-14, BR-73 | — | — |

**Cổng kiểm định xuất bản:** nếu tổ hợp nguyên liệu + đơn vị không quy đổi được sang gram qua `INGREDIENT_UNIT_CONVERSION` thì **chặn công khai** bài viết (BR-48, BR-73).

### 4.8 RECIPE_REACTION

**Mục đích:** Bình chọn Like / Dislike của Member đối với bài công thức, phục vụ tỷ lệ % hài lòng kiểu Samsung Food.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `user_id` | Người bình chọn | Identifier, FK | — | `USER` 1 → `RECIPE_REACTION` 0..* | FR-57 | BR-69 | — | — |
| `recipe_id` | Bài được bình chọn | Identifier, FK | — | `RECIPE_POST` 1 → `RECIPE_REACTION` 0..* | FR-57 | BR-69 | — | — |
| `reaction_type` | `LIKE` hoặc `DISLIKE`; cho phép toggle và đổi chiều | Enum | — | — | FR-57 | BR-69 | — | — |
| `created_at` | Lần bình chọn đầu tiên | Timestamp | — | — | Technical design — audit | — | — | — |
| `updated_at` | Lần đổi chiều gần nhất | Timestamp | — | — | FR-57 | BR-69 | — | — |

**Cập nhật bộ đếm:** mọi thao tác thêm, đổi chiều hoặc gỡ bình chọn phải cập nhật `RECIPE_POST.like_count` / `dislike_count` **đồng bộ trong cùng transaction** (Tech Lead chốt 23/09/2026).

**Ràng buộc không vẽ được bằng đường nối:** khóa chính kép `(user_id, recipe_id)` bảo đảm **1 phản hồi / user / bài**; **tác giả không được tự bình chọn bài của mình** — ràng buộc này database không ép được bằng constraint thông thường, phải thực thi ở tầng service và ghi rõ trong pha 2.

### 4.9 RECIPE_VIEW

**Mục đích:** Sự kiện xem bài, phục vụ khử trùng lặp cửa sổ 30 phút và thống kê 24h / 7d / 30d / toàn thời gian.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `view_id` | Định danh lượt xem | Identifier | — | PK | FR-58 | — | — | — |
| `recipe_id` | Bài được xem | Identifier, FK | — | `RECIPE_POST` 1 → `RECIPE_VIEW` 0..* | FR-58 | BR-70 | — | — |
| `user_id` | Member đã đăng nhập, **để trống nếu là Guest** | Identifier, FK, optional | — | `USER` **0..1** → `RECIPE_VIEW` 0..*; cho phép ghi lượt xem của Guest mà không tạo bản ghi ảo trong `USER` | FR-58 | BR-70 | — | — |
| `anonymous_viewer_hash` | Định danh Guest qua IP hash / client session | Text, optional | — | — | FR-58 | BR-70 | — | **Không lưu IP thô**; chỉ lưu giá trị đã băm |
| `viewed_at` | Thời điểm xem, cơ sở tính cửa sổ khử trùng lặp | Timestamp | — | — | FR-58 | BR-70 | — | — |

**Cập nhật bộ đếm:** `RECIPE_POST.view_count` cập nhật **bất đồng bộ** (`@Async` hoặc in-memory buffer định kỳ flush), khác với Like/Dislike — theo ARCHITECTURE.md mục 6.

**Ràng buộc không vẽ được bằng đường nối:** cửa sổ khử trùng lặp **30 phút** theo cặp `(user_id | anonymous_viewer_hash, recipe_id)` — logic tầng ứng dụng, không phải constraint.

### 4.10 COMMENT

**Mục đích:** Bình luận và phản hồi lồng nhiều cấp trên bài công thức.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `comment_id` | Định danh bình luận | Identifier | — | PK | FR-46 | — | — | — |
| `recipe_id` | Bài công thức chứa bình luận | Identifier, FK | — | `RECIPE_POST` 1 → `COMMENT` 0..* | FR-46 | BR-66 | — | — |
| `user_id` | Người viết | Identifier, FK | — | `USER` 1 → `COMMENT` 0..* | FR-46 | BR-66 | — | — |
| `parent_comment_id` | Bình luận cha; `NULL` nếu là bình luận gốc | Identifier, FK, optional | — | **Tự tham chiếu:** `COMMENT` 0..1 → `COMMENT` 0..* | FR-46 | BR-66 | — | — |
| `content` | Nội dung bình luận | Text | — | — | FR-46 | BR-66 | — | — |
| `depth` | Cấp lồng, 1–5 | Integer | — | — | FR-46 | BR-66 | — | Technical design — cần thiết vì database không tự giới hạn độ sâu đệ quy |
| `is_deleted` | Cờ tombstone khi bình luận cha bị xóa nhưng còn phản hồi | Boolean | — | — | FR-46 | BR-66 | — | — |
| `created_at` | Thời điểm viết | Timestamp | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm sửa gần nhất | Timestamp | — | — | FR-46 | — | — | — |

**Ràng buộc không vẽ được bằng đường nối:** tối đa **5 cấp** lồng nhau; xóa bình luận cha còn phản hồi thì chuyển thành **tombstone** chứ không xóa cứng. FK tự tham chiếu **không được** cascade.

### 4.11 REPORT

**Mục đích:** Báo cáo vi phạm từ người dùng, đã gộp kết quả và lý do xử lý của Admin (bỏ bảng `moderation_action`).

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `report_id` | Định danh báo cáo | Identifier | — | PK | FR-26 | — | — | — |
| `reporter_id` | Người gửi báo cáo | Identifier, FK | — | `USER` 1 → `REPORT` 0..* | FR-26 | BR-24 | — | **Danh tính người báo cáo phải được bảo vệ**, BR-28 |
| `recipe_id` | Bài công thức bị báo cáo | Identifier, FK, optional | — | `RECIPE_POST` 0..1 → `REPORT` 0..* | FR-26 | BR-23 | — | — |
| `comment_id` | Bình luận bị báo cáo | Identifier, FK, optional | — | `REPORT` 0..* → `COMMENT` 0..1 | FR-48 | BR-23 | — | — |
| `reason_code` | Một trong 6 nhóm lý do: `NON_VEGAN`, `FOOD_SAFETY_HAZARD`, `INAPPROPRIATE_CONTENT`, `COPYRIGHT_VIOLATION`, `SPAM_ADVERTISING`, `OTHER` | Enum | — | — | FR-27 | BR-25 | — | — |
| `description` | Mô tả bổ sung; **bắt buộc 10–500 ký tự** khi `reason_code = OTHER`, tùy chọn tối đa 500 ký tự với các lý do còn lại | Text | — | — | FR-27 | BR-25 | — | — |
| `status` | Trạng thái xử lý báo cáo | Enum | — | — | FR-28 | BR-26 | — | Chỉ Admin xem và đổi, BR-29 |
| `decision` | Quyết định chế tài của Admin | Enum, optional | — | — | FR-28 | BR-26 | — | Chỉ Admin |
| `decision_reason` | Lý do quyết định | Text, optional | — | — | FR-28 | BR-26 | — | Chỉ Admin |
| `created_at` | Thời điểm gửi báo cáo | Timestamp | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | — | — | Technical design — audit | — | — | — |
| `handled_at` | Thời điểm Admin xử lý xong | Timestamp, optional | — | — | FR-28 | BR-26 | — | — |

**Ràng buộc XOR không vẽ được bằng đường nối:** một báo cáo trỏ tới **hoặc** `recipe_id` **hoặc** `comment_id`, không bao giờ cả hai và không bao giờ rỗng cả hai. Chống trùng: không cho tạo báo cáo mới khi đã tồn tại báo cáo đang mở của cùng người trên cùng đối tượng (BR-29).

### 4.12 NOTIFICATION

**Mục đích:** Thông báo trong ứng dụng gửi tới người dùng.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `notification_id` | Định danh thông báo | Identifier | — | PK | FR-49 | — | — | — |
| `user_id` | Người nhận | Identifier, FK | — | `USER` 1 → `NOTIFICATION` 0..* | FR-49 | — | — | Chỉ chủ sở hữu xem được |
| `comment_id` | Bình luận kích hoạt thông báo | Identifier, FK, optional | — | `COMMENT` 0..1 → `NOTIFICATION` 0..* | FR-49 | — | — | — |
| `report_id` | Báo cáo kích hoạt thông báo kết quả | Identifier, FK, optional | — | `REPORT` 0..1 → `NOTIFICATION` 0..* | FR-49 | — | — | — |
| `notification_type` | Loại sự kiện sinh ra thông báo | Enum | — | — | FR-49 | — | — | — |
| `title` | Tiêu đề hiển thị | Text | — | — | FR-49 | — | — | — |
| `message` | Nội dung hiển thị | Text | — | — | FR-49 | — | — | — |
| `is_read` | Đã đọc hay chưa | Boolean | — | — | FR-49 | — | — | — |
| `created_at` | Thời điểm sinh thông báo | Timestamp | — | — | Technical design — audit | — | — | — |
| `read_at` | Thời điểm đọc | Timestamp, optional | — | — | FR-49 | — | — | — |

### 4.13 SAVED_RECIPE

**Mục đích:** Đánh dấu lưu lại bài công thức yêu thích (bookmark).

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `user_id` | Người lưu | Identifier, FK | — | `USER` 1 → `SAVED_RECIPE` 0..* | FR-32 | BR-32, BR-34 | — | Chỉ chủ sở hữu xem được |
| `recipe_id` | Bài được lưu | Identifier, FK | — | `RECIPE_POST` 1 → `SAVED_RECIPE` 0..* | FR-32 | BR-33, BR-34 | — | — |
| `saved_at` | Thời điểm lưu | Timestamp | — | — | FR-32 | — | — | — |

**Ràng buộc:** khóa chính kép `(user_id, recipe_id)` bảo đảm **tính duy nhất của bản ghi lưu** (BR-34). Vòng đời độc lập với `MEAL_PLAN` (BR-35).

### 4.14 MEAL_PLAN

**Mục đích:** Kế hoạch thực đơn **theo tuần**: lưới 7 ngày Thứ Hai đến Chủ Nhật, mỗi ngày đúng 3 bữa cố định (FR-09, BR-36).

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `meal_plan_id` | Định danh kế hoạch | Identifier | — | PK | FR-09 | — | — | — |
| `user_id` | Chủ sở hữu kế hoạch | Identifier, FK | — | `USER` 1 → `MEAL_PLAN` 0..* | FR-09 | BR-32 | — | Chỉ chủ sở hữu xem được |
| `week_start_date` | Ngày bắt đầu tuần kế hoạch, mặc định Thứ Hai | Date | — | Phạm vi tuần đã chốt tại FR-09 | FR-09 | BR-36 | — | — |
| `created_at` | Thời điểm tạo | Timestamp | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | — | — | Technical design — audit | — | — | — |

### 4.15 MEAL_PLAN_ENTRY

**Mục đích:** Món ăn cụ thể được phân bổ vào từng ngày và từng bữa.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `meal_plan_entry_id` | Định danh mục lịch ăn | Identifier | — | PK | FR-33 | — | — | — |
| `meal_plan_id` | Kế hoạch chứa mục này | Identifier, FK | — | `MEAL_PLAN` 1 → `MEAL_PLAN_ENTRY` 0..* | FR-33 | — | — | — |
| `recipe_id` | Món được xếp lịch | Identifier, FK | — | `RECIPE_POST` 1 → `MEAL_PLAN_ENTRY` 0..* | FR-33 | BR-35 | — | — |
| `meal_date` | Ngày ăn | Date | — | — | FR-33 | BR-36 | — | — |
| `meal_type` | Bữa ăn: **3 loại cố định** Sáng / Trưa / Tối | Enum | — | — | FR-33 | BR-36 | — | — |
| `planned_servings` | Số khẩu phần dự kiến cho món trong bữa này | Decimal | — | Cơ sở cộng dồn dinh dưỡng theo ngày | FR-37 | BR-47 | — | — |

**Ràng buộc không vẽ được bằng đường nối:** một món **không được trùng** trong cùng một bữa của cùng một ngày — duy nhất theo bộ `(meal_plan_id, meal_date, meal_type, recipe_id)` (BR-37).

### 4.16 SHOPPING_LIST

**Mục đích:** Danh sách mua sắm nguyên liệu, tạo từ thực đơn, từ bài công thức hoặc lập thủ công.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `shopping_list_id` | Định danh danh sách | Identifier | — | PK | FR-53 | — | — | — |
| `user_id` | Chủ sở hữu | Identifier, FK | — | `USER` 1 → `SHOPPING_LIST` 0..* | FR-53 | BR-32 | — | Chỉ chủ sở hữu; không chia sẻ nhiều tài khoản |
| `name` | Tên danh sách | Text | — | — | FR-53 | — | — | — |
| `created_at` | Thời điểm tạo | Timestamp | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | — | — | Technical design — audit | — | — | — |

### 4.17 SHOPPING_LIST_ITEM

**Mục đích:** Từng mục nguyên liệu cần mua, kèm số lượng, đơn vị và trạng thái đã mua.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `shopping_list_item_id` | Định danh mục cần mua | Identifier | — | PK | FR-53 | — | — | — |
| `shopping_list_id` | Danh sách chứa mục này | Identifier, FK | — | `SHOPPING_LIST` 1 → `SHOPPING_LIST_ITEM` 0..* | FR-53 | — | — | — |
| `ingredient_id` | Nguyên liệu chuẩn được tham chiếu | Identifier, FK, optional | — | `INGREDIENT` 0..1 → 0..*; cho phép nhập tự do | FR-54 | BR-12 | — | — |
| `unit_id` | Đơn vị đo, **bắt buộc** | Identifier, FK | — | `SHOPPING_LIST_ITEM` 0..* → `UNIT` **1** | FR-54 | BR-14, BR-73 | — | — |
| `custom_ingredient_name` | Tên nguyên liệu tự nhập | Text, optional | — | — | FR-53 | BR-12 | — | — |
| `quantity` | Số lượng cần mua, số thực dương | Decimal | — | — | FR-54 | BR-14 | — | — |
| `is_bought` | Đã mua hay chưa (tick checklist) | Boolean | — | — | FR-53 | — | — | — |

**Quy tắc gom an toàn:** chỉ gom các nguyên liệu **cùng chiều đo** (`MASS`, `VOLUME`, `COUNT`) hoặc gom được theo tỷ lệ trong `INGREDIENT_UNIT_CONVERSION` (BR-14, FR-54).

### 4.18 INGREDIENT

**Mục đích:** Từ điển nguyên liệu chuẩn kèm chín chỉ tiêu dinh dưỡng tham khảo trên 100 g.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `ingredient_id` | Định danh nguyên liệu | Identifier | — | PK | FR-41 | — | — | — |
| `name` | Tên nguyên liệu chuẩn, duy nhất | Text, unique | — | — | FR-41 | BR-51 | — | — |
| `energy_kcal_100g` | Chỉ tiêu 1 — Năng lượng | Decimal | — | — | FR-39 | BR-52 | — | — |
| `protein_g_100g` | Chỉ tiêu 2 — Chất đạm | Decimal | — | — | FR-39 | BR-52 | — | — |
| `carbohydrate_g_100g` | Chỉ tiêu 3 — Carbohydrate | Decimal | — | — | FR-39 | BR-52 | — | — |
| `total_fat_g_100g` | Chỉ tiêu 4 — Chất béo | Decimal | — | — | FR-39 | BR-52 | — | — |
| `fiber_g_100g` | Chỉ tiêu 5 — Chất xơ | Decimal | — | — | FR-39 | BR-52 | — | — |
| `calcium_mg_100g` | Chỉ tiêu 6 — Canxi | Decimal | — | — | FR-39 | BR-52 | — | — |
| `iron_mg_100g` | Chỉ tiêu 7 — Sắt | Decimal | — | — | FR-39 | BR-52 | — | — |
| `vitamin_b12_mcg_100g` | Chỉ tiêu 8 — Vitamin B12 | Decimal | — | — | FR-39 | BR-52 | — | — |
| `zinc_mg_100g` 🆕 | Chỉ tiêu 9 — Kẽm | Decimal | — | — | FR-39 | BR-52 | — | — |
| ~~`sodium_mg_100g`~~ 🗑 | **Xóa** — natri không thuộc chín chỉ tiêu MVP | — | — | — | FR-39 | BR-44 | — | — |
| `source_name` | Nguồn số liệu, ví dụ USDA / NIH | Text | — | — | FR-41 | BR-49 | — | — |
| `source_url` | Liên kết nguồn | Text, optional | — | — | FR-41 | BR-49 | — | — |
| `reference_date` | Ngày tham chiếu của số liệu | Date | — | — | FR-41 | BR-49 | — | — |
| `nutrition_supported` | Đã có đủ chín chỉ tiêu và nguồn để kích hoạt tính dinh dưỡng hay chưa | Boolean | — | — | FR-40 | BR-50, BR-52 | — | — |
| `status` | Trạng thái hoạt động của mục từ điển | Enum | — | — | FR-41 | BR-53 | — | — |
| `created_at` | Thời điểm tạo | Timestamp | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | — | — | Technical design — audit | — | — | — |

**Ràng buộc:** **cấm xóa vĩnh viễn** nguyên liệu đã được tham chiếu (BR-53); cấm nhập hàng loạt tự động vào từ điển (BR-54).

### 4.19 UNIT

**Mục đích:** Từ điển đơn vị đo lường chuẩn hóa theo ba chiều `MASS`, `VOLUME`, `COUNT`.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `unit_id` | Định danh đơn vị | Identifier | — | PK | FR-18 | BR-73 | — | — |
| `code` | Mã đơn vị, duy nhất — `g`, `kg`, `ml`, `L`, `quả`, `củ`, `bìa`… | Text, unique | — | — | FR-18 | BR-73 | — | — |
| `name` | Tên hiển thị | Text | — | — | FR-18 | — | — | — |
| `dimension` | Chiều đo: `MASS` / `VOLUME` / `COUNT` | Enum | — | Quyết định có gom an toàn được hay không | FR-18 | BR-14, BR-73 | — | — |
| `base_factor` | Hệ số quy về đơn vị cơ sở của cùng chiều | Decimal | — | — | FR-18 | BR-73 | — | — |
| `is_active` | Đơn vị còn dùng hay đã ngừng | Boolean | — | — | FR-18 | — | — | — |

Đây là **reference data bắt buộc** — pha 2 phải seed cùng migration, tách khỏi demo data.

### 4.20 INGREDIENT_UNIT_CONVERSION

**Mục đích:** Quy đổi đơn vị đặc thù (quả, củ, bìa, ml…) sang gram cho **từng nguyên liệu cụ thể**, phục vụ tính dinh dưỡng.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `ingredient_id` | Nguyên liệu áp dụng quy đổi | Identifier, FK | — | `INGREDIENT_UNIT_CONVERSION` 0..* → `INGREDIENT` **1** | FR-18 | BR-73 | — | — |
| `unit_id` | Đơn vị nguồn cần quy đổi | Identifier, FK | — | `INGREDIENT_UNIT_CONVERSION` 0..* → `UNIT` **1** | FR-18 | BR-73 | — | — |
| `grams_per_unit` | Số gram tương ứng một đơn vị | Decimal | — | — | FR-18 | BR-73 | — | — |
| `is_approximate` | Tỷ lệ là ước lượng hay chính xác | Boolean | — | — | FR-18 | BR-48 | — | — |
| `is_active` | Quy tắc còn hiệu lực hay không | Boolean | — | — | FR-18 | — | — | — |

**Cổng kiểm định xuất bản:** thiếu tỷ lệ quy đổi cho tổ hợp nguyên liệu + đơn vị đang dùng thì **chặn công khai** bài công thức (BR-48, BR-73).

### 4.21 SUBSCRIPTION

**Mục đích:** Gói dịch vụ hội viên, thời hạn hiệu lực và phân tầng tính năng AI.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `subscription_id` | Định danh gói | Identifier | — | PK | FR-13 | — | — | — |
| `user_id` | Chủ sở hữu gói | Identifier, FK | — | `USER` 1 → `SUBSCRIPTION` 0..* (lịch sử các gói) | FR-13 | BR-02 | — | — |
| `tier` | `FREE` / `PLUS` / `PRO` | Enum | — | Quyết định quyền dùng tính năng AI | FR-10, FR-13 | BR-01, BR-02, BR-03 | — | — |
| `status` | Trạng thái hiệu lực | Enum | — | — | FR-13 | BR-03 | — | — |
| `starts_at` | Thời điểm bắt đầu hiệu lực | Timestamp | — | Chỉ kích hoạt **sau khi thanh toán được xác minh** | FR-13 | BR-03 | — | — |
| `ends_at` | Thời điểm hết hạn cuối kỳ đã trả | Timestamp | — | Không tự động gia hạn, không hoàn tiền một phần | FR-13 | BR-03 | — | — |
| `created_at` | Thời điểm tạo | Timestamp | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | — | — | Technical design — audit | — | — | — |

### 4.22 PAYMENT_TRANSACTION

**Mục đích:** Giao dịch thanh toán qua payOS để kích hoạt gói dịch vụ.

| Column | Business meaning | Logical type | Physical | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|
| `payment_transaction_id` | Định danh giao dịch | Identifier | — | PK | FR-13 | — | — | — |
| `user_id` | Người thanh toán | Identifier, FK | — | `USER` 1 → `PAYMENT_TRANSACTION` 0..* | FR-13 | — | — | — |
| `subscription_id` | Gói được kích hoạt bởi giao dịch này | Identifier, FK, optional | — | `PAYMENT_TRANSACTION` 0..1 → `SUBSCRIPTION` 0..1 | FR-13 | BR-03 | — | — |
| `order_code` | Mã đơn hàng payOS, **duy nhất** | Text, unique | — | — | FR-13 | — | — | **Cơ chế idempotent cho webhook** — webhook gửi lại nhiều lần chỉ kích hoạt gói đúng một lần |
| `amount_vnd` | Số tiền: FREE 0, PLUS 49.000, PRO 99.000 VNĐ/tháng | Integer | — | — | FR-13 | — | — | — |
| `status` | Trạng thái giao dịch | Enum | — | — | FR-13 | — | — | — |
| `created_at` | Thời điểm tạo giao dịch | Timestamp | — | — | Technical design — audit | — | — | — |
| `paid_at` | Thời điểm thanh toán thành công | Timestamp, optional | — | — | FR-13 | — | — | **Không lưu số thẻ**, NFR-21 (PCI-DSS) |

## 5. Ràng buộc không biểu diễn được bằng đường nối ERD

Issue #63 mục D yêu cầu ghi riêng các ràng buộc loại này. Trương Văn Khải chuyển chúng thành constraint hoặc ghi rõ là ràng buộc tầng service trong pha 2.

| # | Ràng buộc | Bảng | Ép được ở database? | Nguồn |
|---|---|---|---|---|
| 1 | `REPORT` trỏ tới **hoặc** `recipe_id` **hoặc** `comment_id`, không cả hai, không rỗng cả hai (XOR) | `REPORT` | Có — `CHECK` đếm cột khác `NULL` bằng 1 | BR-23 |
| 2 | Mỗi bài công thức có **đúng 1** ảnh `is_cover = true` | `RECIPE_MEDIA` | Có — filtered unique index | BR-20 |
| 3 | Mỗi bài công thức có **tối đa 5** ảnh | `RECIPE_MEDIA` | Không — tầng service | BR-20 |
| 4 | Bình luận lồng **tối đa 5 cấp** | `COMMENT` | Không — cột `depth` + `CHECK 1..5`, service duy trì | BR-66 |
| 5 | Xóa bình luận cha còn phản hồi thì chuyển **tombstone**, không xóa cứng | `COMMENT` | Không — tầng service | BR-66 |
| 6 | **1 phản hồi / user / bài** | `RECIPE_REACTION` | Có — khóa chính kép | BR-69 |
| 7 | **Tác giả không được tự bình chọn** bài của mình | `RECIPE_REACTION` | Không — tầng service | BR-69 |
| 8 | Khử trùng lặp lượt xem trong **cửa sổ 30 phút** | `RECIPE_VIEW` | Không — tầng service | BR-70 |
| 9 | Một món **không trùng** trong cùng bữa cùng ngày | `MEAL_PLAN_ENTRY` | Có — unique theo bộ 4 cột | BR-37 |
| 10 | Mỗi Customer tối đa **một đơn `PENDING`** | `EXPERT_APPLICATION` | Có — filtered unique index | BR-21 |
| 11 | **Không tự theo dõi chính mình**; không trùng cặp | `USER_FOLLOW` | Có — `CHECK` + khóa chính kép | BR-75 |
| 12 | `order_code` duy nhất — idempotency cho webhook payOS | `PAYMENT_TRANSACTION` | Có — unique | FR-13 |
| 13 | **Chặn công khai** bài công thức khi thiếu tỷ lệ quy đổi sang gram | `RECIPE_INGREDIENT` | Không — tầng service tại thời điểm publish | BR-48, BR-73 |
| 14 | **Cấm xóa vĩnh viễn** nguyên liệu đã được tham chiếu | `INGREDIENT` | Có — FK `NO ACTION` | BR-53 |
| 15 | Chống báo cáo trùng khi đã có báo cáo đang mở | `REPORT` | Có — filtered unique index | BR-29 |
| 16 | `dish_category` chỉ nhận **11 technical code** đã chốt | `RECIPE_POST` | Có — `CHECK` | Q1, 23/09/2026 |
| 17 | `like_count`, `dislike_count`, `view_count` **không âm**, mặc định `0` | `RECIPE_POST` | Có — `CHECK >= 0` + `DEFAULT 0` | Q2, 23/09/2026 |
| 18 | Bộ đếm Like/Dislike khớp `RECIPE_REACTION` — cập nhật **cùng transaction** | `RECIPE_POST` | Không — tầng service | Q2, 23/09/2026 |

## 6. Cảnh báo cho pha 2 — multiple cascade paths trên SQL Server

SQL Server từ chối tạo FK khi có nhiều đường cascade dẫn tới cùng một bảng (lỗi 1785). Mô hình này dính ở ít nhất ba chỗ:

- `COMMENT` tham chiếu cả `USER` lẫn `RECIPE_POST`, mà `RECIPE_POST` lại tham chiếu `USER`.
- `USER_FOLLOW` có **hai** FK cùng trỏ về `USER`.
- `REPORT` tham chiếu `USER` (reporter), `RECIPE_POST` và `COMMENT`.

Chỉ nên cascade ở quan hệ cha–con thật sự sở hữu, ví dụ `SHOPPING_LIST` → `SHOPPING_LIST_ITEM` và `MEAL_PLAN` → `MEAL_PLAN_ENTRY`. Các FK còn lại dùng `ON DELETE NO ACTION` và xử lý ở tầng service. Quyết định cho **từng** FK phải được ghi vào cột `FK reference` khi pha 2 hoàn thiện.

## 7. Việc còn lại trước khi chốt pha 1

- [x] ~~Trả lời câu hỏi chặn với Tech Lead~~ — `Q1` và `Q2` đã chốt ngày 23/09/2026, ghi tại mục 2.
- [x] ~~Dựng `logical-erd-v1.0.0.drawio` theo delta ở mục 3~~ — đã dựng ngày 23/09/2026: 22 bảng, 36 connector, XML đã kiểm tra hợp lệ.
- [ ] **Export PNG cho `logical-erd-v1.0.0`** — phải mở bằng draw.io và xuất thủ công; chưa làm.
- [ ] Đối chiếu hai chiều: mỗi dòng trong tài liệu này xuất hiện trên sơ đồ, và ngược lại.
- [x] ~~Kiểm tra chuẩn hóa 3NF~~ — ngoại lệ có chủ đích đã ghi tại mục 4.5 sau quyết định `Q2`.
- [ ] Rà đủ **36 connector** trong ma trận quan hệ của ERD README so với cột `Relationship / Cardinality` ở trên.
