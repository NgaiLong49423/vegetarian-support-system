> **Document:** Data Dictionary & Traceability Matrix — Mâm Xanh
> **File:** `docs/diagrams/ERD/data-dictionary.md`
> **Version:** v0.7.1
> **Created:** 2026-09-23
> **Last Updated:** 2026-09-25
> **Status:** Under Review
> **Related Docs:** `docs/diagrams/ERD/README.md`, `docs/requirements/SRS.md`, `docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md`, `docs/requirements/srs/BUSINESS-RULES.md`

# Data Dictionary & Traceability Matrix

Tài liệu này là bảng tổng hợp bắt buộc của [Issue #63](https://github.com/NgaiLong49423/vegetarian-support-system/issues/63) mục D: **đúng một bảng, một dòng cho mỗi cột**, nhóm theo table để thành viên Backend tra cứu trực tiếp.

Nguồn đối chiếu: Conceptual ERD 22 thực thể / 36 connector trong [ERD README](README.md), requirement baseline trong [SRS](../../requirements/SRS.md) v1.10.0, và cấu trúc cột kế thừa từ bản nháp `logical-erd-v0.1.0.drawio`. Bản nháp này đã được xóa khỏi repo ngày 24/09/2026 theo yêu cầu của Tech Lead; tra lịch sử Git nếu cần đối chiếu.

## 1. Trạng thái và cách dùng

| Pha | Người phụ trách | Phạm vi trong tài liệu này | Trạng thái |
|---|---|---|---|
| Pha 1 — Logical | Nguyễn Hải Dương | `Table`, `Table purpose`, `Column`, `Business meaning`, `Logical type`, `Relationship/Cardinality`, `FR`, `BR`, `UC/AC`, `Security/Privacy note` | Hoàn thành (commit `cb404f7`); cập nhật theo review vòng 2 và quyết định `Q7`–`Q12` ngày 24/09/2026 |
| Pha 2 — Physical | Trương Văn Khải | `SQL Server type`, `Length/Precision`, `Nullable`, `Default`, `PK`, `FK reference`, `UNIQUE`, `CHECK`, `Index` | Hoàn thành (25/09/2026). Đã triển khai đầy đủ 33 hạng mục vào `V1__baseline_schema.sql` và `database/schema.sql`, kiểm thử đạt 66/66 test assertions PASS trên Microsoft SQL Server 2019 thật — xem mục 8 |

Toàn bộ 9 cột thuộc Pha 2 đã được Trương Văn Khải điền chi tiết cho toàn bộ 22 bảng. Toàn bộ các kiểu dữ liệu, độ dài, nullability, constraint và chỉ mục đã được triển khai hoàn chỉnh trong `V1__baseline_schema.sql` và `database/schema.sql`, kiểm thử trực tiếp đạt 100% PASS trên Microsoft SQL Server 2019 thật, tương thích với DDL hiện tại và không xảy ra xung đột khóa ngoại cascade (lỗi SQL Server Error 1785).

Ký hiệu trong tài liệu:

- `🆕` — cột hoặc bảng chưa có trong bản nháp `logical-erd-v0.1.0.drawio`, phải bổ sung khi dựng v1.0.0; hoặc cột bổ sung theo quyết định `Q7`–`Q12`.
- `🗑` — cột hoặc bảng phải xóa khỏi sơ đồ logical.
- `Technical design` — trường kỹ thuật không bắt nguồn trực tiếp từ requirement, có ghi lý do. Cột ghi `chỉ Physical ERD` không vẽ trên Logical ERD (quyết định `Q8`).

## 2. Quyết định đã chốt

Toàn bộ điểm mở ảnh hưởng tới table, column, key hoặc constraint đã được Tech Lead chốt ngày **23/09/2026** và **24/09/2026**. Pha 1 không còn `TBD`.

| ID | Quyết định | Người chốt | Ngày chốt |
|---|---|---|---|
| `Q1` | `RECIPE_POST.dish_category` dùng **danh sách cố định**, không nhập tự do và không tạo lại bảng `Category`. Lưu **technical code** trong cột có `CHECK` constraint, gồm **11 giá trị**: `NOODLE_SOUP`, `STIR_FRY`, `HOT_POT`, `BRAISED`, `SOUP`, `FRIED`, `STEAMED`, `SALAD`, `ROLL`, `GRILLED`, `DESSERT`. Frontend hiển thị nhãn tiếng Việt tương ứng. | Ngô Gia Long | 23/09/2026 |
| `Q2` | `RECIPE_POST` **lưu** `like_count`, `dislike_count` và `view_count`, mặc định `0` và không âm. `RECIPE_REACTION` / `RECIPE_VIEW` vẫn là **dữ liệu gốc**. Like/Dislike cập nhật bộ đếm **đồng bộ trong cùng transaction**; riêng `view_count` cập nhật **bất đồng bộ** theo ARCHITECTURE.md mục 6. `like_percentage` **tính khi đọc, không lưu thành cột**. | Ngô Gia Long | 23/09/2026 |
| `Q5` | **Chín cột dinh dưỡng của `INGREDIENT` cho phép `NULL`.** Phân biệt ba trạng thái: `NULL` = chưa có dữ liệu · `0` = giá trị thật bằng không · `nutrition_supported = 0` = nguyên liệu chưa được hỗ trợ tính dinh dưỡng. `nutrition_supported` chỉ được bật khi **đủ chín cột khác `NULL`** và có đủ `source_name`, `source_url`, `reference_date` (BR-52). Cho phép lưu bản ghi chưa hoàn thiện ở trạng thái chưa hỗ trợ. | Ngô Gia Long | 24/09/2026 |
| `Q6` | **`EXPERT_APPLICATION.reviewed_by` không có connector trên Conceptual ERD.** Giữ nguyên ma trận 36 connector; quan hệ này chỉ tồn tại ở mức khóa ngoại trong Data Dictionary và schema. Cách thể hiện trên Physical ERD do người thực hiện pha 2 quyết định. | Ngô Gia Long | 24/09/2026 |
| `Q7` | **`USER` lưu `date_of_birth`**, không lưu tuổi cố định. Điều kiện 18–120 tuổi do **service** kiểm tra; database chỉ chặn ngày sinh trong tương lai hoặc trước năm 1900. FR-35 sẽ được Tech Lead sửa từ "Năm sinh/Tuổi" thành "Ngày sinh". | Ngô Gia Long | 24/09/2026 |
| `Q8` | Ràng buộc liên dòng/liên bảng dùng **cột kỹ thuật + composite FK**: `COMMENT.parent_depth` ép `depth = depth cha + 1`; computed column `MEAL_PLAN_ENTRY.meal_week_start` ép `meal_date` nằm trong tuần của `MEAL_PLAN`. Hai cột này **chỉ thuộc Physical ERD**, không vẽ trên Logical ERD, và được ghi là `Technical design`. | Ngô Gia Long | 24/09/2026 |
| `Q9` | **Không lưu gói FREE trong `SUBSCRIPTION`.** User không có gói trả phí `ACTIVE` còn hạn thì entitlement mặc định là FREE. `tier` chỉ còn `PLUS` / `PRO`. | Ngô Gia Long | 24/09/2026 |
| `Q10` | **Mỗi user tối đa một gói trả phí `ACTIVE`.** Cho phép nâng cấp ngay PLUS → PRO: gói cũ chuyển `CANCELLED` và gói mới `ACTIVE` trong **cùng transaction**. MVP chưa hỗ trợ gia hạn sớm, hạ hạng hoặc nhiều gói chồng lấn. | Ngô Gia Long | 24/09/2026 |
| `Q11` | **`preference_type` chỉ còn `AVOID` và `DISLIKE`** — gộp dị ứng/kiêng vào `AVOID`, khớp hai danh sách của FR-31 mục 3. Mỗi nguyên liệu chỉ có **một loại hiệu lực** trên một user; đổi danh sách thì cập nhật `preference_type`, không tạo dòng trùng. | Ngô Gia Long | 24/09/2026 |
| `Q12` | Bổ sung ngay trong PR #66: trạng thái Onboarding, hai cờ xác nhận "Không có", `nutrition_goal`, `date_of_birth`; `activity_level` sửa về đúng **4 mức** của FR-35. | Ngô Gia Long | 24/09/2026 |

> [!WARNING]
> **Quyết định `Q7` đang chờ SRS cập nhật.** FR-35 Bước 2 hiện ghi *"Năm sinh/Tuổi"*. Tech Lead đã xác nhận ngày 24/09/2026 sẽ sửa thành "Ngày sinh". Không tự sửa SRS ở Task này.

> [!WARNING]
> **Quyết định `Q5` hiện mâu thuẫn với SRS và đang chờ Tech Lead cập nhật.**
> `AC-41.2` ghi: *"khi có bất kỳ chỉ tiêu nào trong 9 chỉ tiêu bị bỏ trống hoặc thiếu thông tin nguồn tham chiếu, hệ thống **ngăn chặn việc lưu**"*, và FR-41 lặp lại *"thiếu dù chỉ một giá trị chỉ tiêu dinh dưỡng hoặc thiếu nguồn trích dẫn, hệ thống chặn lưu"*.
> Tech Lead đã xác nhận ngày 24/09/2026 sẽ chốt lại nghiệp vụ theo hướng **cho phép lưu dữ liệu chưa đầy đủ ở trạng thái chưa hỗ trợ**. Cho tới khi `FR-41`/`AC-41.2` được sửa trong SRS, schema theo `Q5` sẽ không khớp một Acceptance Criteria đang `ACTIVE`. Không tự sửa SRS ở Task này — việc đó thuộc thẩm quyền Tech Lead.

### Các điểm đã có sẵn câu trả lời trong tài liệu

| Điểm | Kết luận | Nguồn |
|---|---|---|
| Các bước nấu lưu ở đâu | Không còn bảng `RECIPE_STEP`; hướng dẫn lưu tự do tại `RECIPE_POST.instructions`, bắt buộc 10–5.000 ký tự | FR-16, BR-19, AC-16.5 |
| `EXPERT_APPLICATION.status` | `PENDING` / `APPROVED` / `REJECTED` | FR-05 |
| `RECIPE_REACTION.reaction_type` | `LIKE` / `DISLIKE`; 1 bản ghi/user/bài; toggle được; tác giả không tự vote | FR-57, BR-69 |
| Chín chỉ tiêu dinh dưỡng | Năng lượng, Đạm, Carbohydrate, Béo, Xơ, Canxi, Sắt, Vitamin B12, **Kẽm**. Không có natri | BR-44, BR-52 |
| `MEAL_PLAN` theo ngày hay tuần | **Theo tuần** — lưới 7 ngày Thứ Hai đến Chủ Nhật, mỗi ngày đúng 3 bữa cố định. Cột `week_start_date` hiện tại là đúng. Cách diễn đạt "theo ngày hoặc theo tuần" ở ERD README mục 3 là lỏng, nên sửa cho khớp FR-09 | FR-09, BR-36 |
| `vegetarian_type` có 4 hay 5 giá trị | **Không mâu thuẫn — là ba cột khác nhau.** `RECIPE_POST.vegetarian_type` và `USER.vegetarian_type` dùng **4** giá trị (Vegan, Lacto, Ovo, Lacto-Ovo). Riêng `EXPERT_APPLICATION.vegetarian_type` là "trường phái chay chuyên sâu" của cá nhân, dùng **5** giá trị (thêm "Chay thực dưỡng dưỡng sinh") | FR-16 (4), FR-51 (4), FR-05 (5) |
| `MEAL_PLAN.week_start_date` có bắt buộc là Thứ Hai không | **Có.** Tuần luôn từ Thứ Hai đến Chủ Nhật; mỗi user một kế hoạch cho mỗi tuần. Chữ "mặc định" ở FR-09 Bước 1 nói về việc tải tuần hiện tại | FR-09 (Phạm vi nghiệp vụ), Q3 |
| `ingredient_id` và `custom_ingredient_name` là XOR hay "ít nhất một" | **Ít nhất một, không XOR.** Sau khi Admin liên kết nguyên liệu chuẩn, dòng vẫn giữ tên đã nhập nên có cả hai giá trị. Áp cho `RECIPE_INGREDIENT`, `SHOPPING_LIST_ITEM`, `USER_INGREDIENT_PREFERENCE`; tên tự nhập không được rỗng hoặc chỉ gồm khoảng trắng | SRS 3.4, BR-12, FR-54 AF-54.1 |
| `RECIPE_MEDIA.display_order` đánh số từ 0 hay 1 | **Từ 1**, miền 1..5 | FR-14 Bước 6 (`display_order` 1..N), BR-20 |
| Kiểm tra tuổi 18–120 ở database hay service | **Service.** SQL Server chỉ đánh giá `CHECK` khi cột được ghi, nên `CHECK` phụ thuộc thời gian không phải bất biến | FR-35 Bước 4, AC-35.2, BR-42, Q7 |

## 3. Delta từ `logical-erd-v0.1.0.drawio`

Mục này là ghi chép lịch sử. Bản nháp v0.1.0 đã được xóa khỏi repo ngày 24/09/2026. Đọc trực tiếp từ XML khi còn file: bản nháp có **23 bảng**, baseline cần **22**.

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

Theo đúng yêu cầu của [Issue #63](https://github.com/NgaiLong49423/vegetarian-support-system/issues/63) mục D, tài liệu này duy trì **đúng một bảng duy nhất cho mỗi thực thể, một dòng cho mỗi cột**. Cột `Physical` tạm thời của Pha 1 đã được phân rã thành **9 cột Physical cụ thể** (`SQL Server type` → `Index`) để kỹ sư Backend và Database tra cứu và ánh xạ trực tiếp sang Entity JPA / DDL:

- `SQL Server type`: Kiểu dữ liệu vật lý chuẩn trên Microsoft SQL Server (ví dụ: `BIGINT`, `NVARCHAR`, `VARCHAR`, `DECIMAL`, `BIT`, `INT`, `DATE`, `DATETIME2`).
- `Length/Precision`: Độ dài hoặc độ chính xác (ví dụ: `255`, `100`, `10,2`, `18,6`, `MAX`, `7`, hoặc `—` nếu không áp dụng).
- `Nullable`: Khả năng chấp nhận giá trị rỗng (`NOT NULL` hoặc `NULL`).
- `Default`: Ràng buộc giá trị mặc định (`DEFAULT`), kèm tên constraint chuẩn hóa `DF_...`.
- `PK`: Khóa chính (`PK (Identity 1,1)` hoặc `PK (Composite x/y)`).
- `FK reference`: Khóa ngoại tham chiếu bảng đích kèm hành vi cascade (`[CASCADE]`, `[SET NULL]`, `[NO ACTION]`).
- `UNIQUE`: Ràng buộc hoặc chỉ mục duy nhất (`UNIQUE` chuẩn hoặc `Filtered UNIQUE`).
- `CHECK`: Ràng buộc miền giá trị hợp lệ (`CHECK`), kèm danh sách enum/khoảng giá trị.
- `Index`: Chỉ mục hỗ trợ truy vấn hiệu năng cao (`Clustered`, `Nonclustered`, `Filtered Index`).

### 4.1 USER

**Mục đích:** Tài khoản, vai trò, hồ sơ cá nhân và chỉ số thể trạng của toàn bộ người dùng đã đăng ký. Đã gộp thực thể `User Profile`.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `user_id` | Định danh tài khoản | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_USER (Clustered) | PK; đầu `1` của 12 quan hệ xuất phát từ `USER` | FR-03 | — | UC-03.1 | — |
| `email` | Email đăng nhập, định danh duy nhất | Text, unique | VARCHAR | 255 | NOT NULL | — | — | — | UQ_USER_email | — | UQ_USER_email (Nonclustered) | — | FR-03 | BR-24 | UC-03.1 | Dữ liệu cá nhân |
| `password_hash` | Mật khẩu đã băm BCrypt | Text | VARCHAR | 255 | NULL | — | — | — | — | — | — | — | FR-03 | — | AC-03.1 | **Tuyệt đối không log**; NFR-06 |
| `google_subject` | `sub` từ Google ID Token, khóa định danh Google ổn định | Text, unique, optional | VARCHAR | 255 | NULL | — | — | — | UQ_USER_google_subject (Filtered) | — | UQ_USER_google_subject (Nonclustered, WHERE google_subject IS NOT NULL) | — | FR-03 | — | UC-03.8 | Dữ liệu cá nhân |
| `display_name` | Tên hiển thị công khai | Text | NVARCHAR | 100 | NOT NULL | — | — | — | — | — | — | — | FR-23 | BR-18 | — | Công khai theo BR-18 |
| `avatar_url` | Ảnh đại diện | Text, optional | VARCHAR | 2048 | NULL | — | — | — | — | — | — | — | FR-23 | BR-18 | — | Công khai |
| `bio` | Giới thiệu ngắn của tác giả | Text, optional | NVARCHAR | 500 | NULL | — | — | — | — | — | — | — | FR-23 | BR-18 | — | Công khai |
| `role` | Vai trò: `CUSTOMER` / `EXPERT` / `ADMIN` | Enum | VARCHAR | 20 | NOT NULL | 'CUSTOMER' (DF_USER_role) | — | — | — | CK_USER_role ('CUSTOMER', 'EXPERT', 'ADMIN') | — | Quyết định quyền đăng bài và quản trị | FR-03, FR-05 | BR-17, BR-26 | — | Quyết định phân quyền, NFR-09 |
| `account_status` | Trạng thái tài khoản (hoạt động / bị khóa) | Enum | VARCHAR | 20 | NOT NULL | 'ACTIVE' (DF_USER_account_status) | — | — | — | CK_USER_account_status ('ACTIVE', 'LOCKED') | — | — | FR-03 | BR-26 | — | Chỉ Admin đổi được |
| `email_verified` | Đã xác minh email hay chưa | Boolean | BIT | — | NOT NULL | 0 (DF_USER_email_verified) | — | — | — | — | — | — | FR-03 | — | UC-03.2 | — |
| `vegetarian_type` | Trường phái ăn chay của người dùng: Vegan / Lacto / Ovo / Lacto-Ovo (4 giá trị) | Enum | VARCHAR | 20 | NULL | — | — | — | — | CK_USER_vegetarian_type ('VEGAN', 'LACTO', 'OVO', 'LACTO_OVO') | — | — | FR-31 | BR-30 | — | — |
| `cuisine_preference` | Khẩu vị vùng miền ưa thích | Text, optional | NVARCHAR | 200 | NULL | — | — | — | — | — | — | — | FR-31 | BR-30 | — | — |
| `preferred_difficulty` | Độ khó món ăn ưa thích | Enum, optional | VARCHAR | 20 | NULL | — | — | — | — | CK_USER_preferred_difficulty ('EASY', 'MEDIUM', 'HARD') | — | — | FR-31 | BR-30 | — | — |
| `max_cooking_time_min` | Thời gian nấu tối đa chấp nhận được | Integer, optional | INT | — | NULL | — | — | — | — | — | — | — | FR-31 | BR-30 | — | — |
| `biological_sex` | Giới tính sinh học, dùng cho tính nhu cầu dinh dưỡng | Enum, optional | VARCHAR | 10 | NULL | — | — | — | — | CK_USER_biological_sex ('MALE', 'FEMALE') | — | — | FR-35 | BR-42 | — | **Dữ liệu sức khỏe**, NFR-08, NFR-20 |
| `height_cm` | Chiều cao, hợp lệ 100–250 | Decimal, optional | DECIMAL | 5,1 | NULL | — | — | — | — | CK_USER_height_cm (100.0..250.0) | — | — | FR-35 | BR-39 | AC-35.3 | **Dữ liệu sức khỏe** |
| `weight_kg` | Cân nặng, hợp lệ 30–300 | Decimal, optional | DECIMAL | 5,1 | NULL | — | — | — | — | CK_USER_weight_kg (30.0..300.0) | — | — | FR-35 | BR-39 | AC-35.3 | **Dữ liệu sức khỏe** |
| `activity_level` | Mức độ vận động, **đúng 4 mức** của FR-35 (Q12): `SEDENTARY` (Ít vận động) / `LIGHTLY_ACTIVE` (Vận động nhẹ) / `MODERATELY_ACTIVE` (Vận động vừa) / `VERY_ACTIVE` (Vận động nặng) | Enum, optional | VARCHAR | 30 | NULL | — | — | — | — | CK_USER_activity_level ('SEDENTARY', 'LIGHTLY_ACTIVE', 'MODERATELY_ACTIVE', 'VERY_ACTIVE') — bỏ `EXTRA_ACTIVE` | — | — | FR-35 | BR-39 | — | **Dữ liệu sức khỏe** |
| `pregnant` | Đang mang thai — thuộc nhóm loại trừ khỏi tính dinh dưỡng | Boolean | BIT | — | NOT NULL | 0 (DF_USER_pregnant) | — | — | — | — | — | — | FR-38 | BR-42 | — | **Dữ liệu sức khỏe nhạy cảm** |
| `breastfeeding` | Đang cho con bú — nhóm loại trừ | Boolean | BIT | — | NOT NULL | 0 (DF_USER_breastfeeding) | — | — | — | — | — | — | FR-38 | BR-42 | — | **Dữ liệu sức khỏe nhạy cảm** |
| `therapeutic_diet_required` | Cần chế độ ăn điều trị — nhóm loại trừ | Boolean | BIT | — | NOT NULL | 0 (DF_USER_therapeutic) | — | — | — | — | — | — | FR-38 | BR-42 | — | **Dữ liệu sức khỏe nhạy cảm** |
| `nutrition_scope_confirmed` | Đã xác nhận phạm vi hỗ trợ trước khi dùng chức năng dinh dưỡng | Boolean | BIT | — | NOT NULL | 0 (DF_USER_nutrition_scope) | — | — | — | — | — | — | FR-38 | BR-41, BR-42 | — | Bằng chứng consent, NFR-20 |
| `reply_email_enabled` | Bật nhận email khi có reply | Boolean | BIT | — | NOT NULL | 1 (DF_USER_reply_email) | — | — | — | — | — | — | FR-49 | — | — | — |
| `date_of_birth` 🆕 | Ngày sinh (Q7). Tuổi tính khi đọc, **không lưu cột tuổi**. Điều kiện 18–120 tuổi do **service** kiểm tra khi lưu hồ sơ dinh dưỡng | Date, optional | DATE | — | NULL | — | — | — | — | CK_USER_date_of_birth (date_of_birth >= '1900-01-01' AND date_of_birth <= CAST(GETDATE() AS DATE)) | — | — | FR-35 | BR-42 | AC-35.2 | **Dữ liệu sức khỏe**, NFR-08, NFR-20 |
| `nutrition_goal` 🆕 | Mục tiêu dinh dưỡng chung (Q12): `MAINTAIN_WEIGHT` (Duy trì cân nặng) / `IMPROVE_HEALTH` (Tăng cường sức khỏe) / `SUPPORT_TRAINING` (Hỗ trợ tập luyện). Frontend hiển thị nhãn tiếng Việt | Enum, optional | VARCHAR | 20 | NULL | — | — | — | — | CK_USER_nutrition_goal ('MAINTAIN_WEIGHT', 'IMPROVE_HEALTH', 'SUPPORT_TRAINING') | — | — | FR-35 | BR-42 | UC-35.1 | **Dữ liệu sức khỏe**, NFR-20 |
| `onboarding_status` 🆕 | Trạng thái Onboarding (Q12): `NOT_STARTED` (chưa hiển thị) / `SKIPPED` (đã bấm "Bỏ qua" — UC-31.2 ghi nhận "chưa hoàn tất") / `COMPLETED` (đã hoàn tất — UC-31.1). Chỉ phục vụ luồng hiển thị Onboarding; **cổng AI cá nhân hóa kiểm tra trực tiếp dữ liệu hồ sơ**, không dựa vào cột này (mục 5 #30) | Enum | VARCHAR | 20 | NOT NULL | 'NOT_STARTED' (DF_USER_onboarding_status) | — | — | — | CK_USER_onboarding_status ('NOT_STARTED', 'SKIPPED', 'COMPLETED') | — | — | FR-31 | BR-30 | UC-31.1, UC-31.2 | — |
| `avoid_none_confirmed` 🆕 | Người dùng đã **chủ động** xác nhận "Không có" nguyên liệu cần tránh (dị ứng/kiêng) (Q12). `0` = chưa xác nhận — **không** được hiểu là không dị ứng | Boolean | BIT | — | NOT NULL | 0 (DF_USER_avoid_none_confirmed) | — | — | — | — | — | Liên quan các dòng `USER_INGREDIENT_PREFERENCE` loại `AVOID` (mục 5 #30) | FR-31 | BR-31 | — | **Dữ liệu sức khỏe** — liên quan dị ứng |
| `dislike_none_confirmed` 🆕 | Người dùng đã **chủ động** xác nhận "Không có" món/nguyên liệu không thích (Q12). `0` = chưa xác nhận | Boolean | BIT | — | NOT NULL | 0 (DF_USER_dislike_none_confirmed) | — | — | — | — | — | Liên quan các dòng `USER_INGREDIENT_PREFERENCE` loại `DISLIKE` (mục 5 #30) | FR-31 | BR-31 | — | — |
| `created_at` | Thời điểm tạo tài khoản | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_USER_created_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_USER_updated_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |

### 4.2 USER_FOLLOW 🆕

**Mục đích:** Quan hệ theo dõi **có hướng** giữa hai Member. A theo dõi B không làm B tự động theo dõi A.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `follower_user_id` | Người thực hiện theo dõi | Identifier, FK | BIGINT | — | NOT NULL | — | PK (Composite 1/2) | FK_USER_FOLLOW_follower → USER(user_id) [NO ACTION] | — | CK_USER_FOLLOW_no_self (<> followed_user_id) | PK_USER_FOLLOW (Clustered) | `USER` 1 → `USER_FOLLOW` 0..* (vai trò follower) | FR-59 | BR-75 | — | — |
| `followed_user_id` | Người được theo dõi | Identifier, FK | BIGINT | — | NOT NULL | — | PK (Composite 2/2) | FK_USER_FOLLOW_followed → USER(user_id) [NO ACTION] | — | — | IX_USER_FOLLOW_followed (Nonclustered) | `USER` 1 → `USER_FOLLOW` 0..* (vai trò followed) | FR-59 | BR-75 | — | — |
| `created_at` | Thời điểm bắt đầu theo dõi | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_USER_FOLLOW_created_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |

**Ràng buộc không vẽ được bằng đường nối:** khóa chính kép `(follower_user_id, followed_user_id)` chống trùng cặp; điều kiện `follower_user_id ≠ followed_user_id` chống tự theo dõi. Cả hai FK cùng trỏ về `USER` nên **không được** dùng cascade (xem mục 5).

### 4.3 USER_INGREDIENT_PREFERENCE

**Mục đích:** Hai danh sách nguyên liệu của FR-31 mục 3: nguyên liệu **cần tránh** do dị ứng/kiêng (`AVOID`) và món/nguyên liệu **không thích** (`DISLIKE`).

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `preference_id` | Định danh bản ghi | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_USER_INGREDIENT_PREFERENCE (Clustered) | PK | FR-31 | — | — | — |
| `user_id` | Người sở hữu khai báo | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_UIP_USER → USER(user_id) [NO ACTION] | UQ_UIP_user_ingredient (1/2), UQ_UIP_user_custom_name (1/2) | — | IX_UIP_user (Nonclustered) | `USER` 1 → 0..* | FR-31 | BR-30 | — | Chỉ chủ sở hữu xem được |
| `ingredient_id` | Nguyên liệu chuẩn được tham chiếu | Identifier, FK, optional | BIGINT | — | NULL | — | — | FK_UIP_INGREDIENT → INGREDIENT(ingredient_id) [NO ACTION] | UQ_UIP_user_ingredient (Filtered, 2/2) | CK_UIP_target (ingredient_id IS NOT NULL OR custom_ingredient_name IS NOT NULL) | UQ_UIP_user_ingredient (Nonclustered, (user_id, ingredient_id) WHERE ingredient_id IS NOT NULL) | `INGREDIENT` 1 → 0..* | FR-31 | BR-13 | — | — |
| `custom_ingredient_name` | Tên nguyên liệu người dùng tự nhập khi không chọn từ danh mục chuẩn; không được rỗng hoặc chỉ gồm khoảng trắng | Text, optional | NVARCHAR | 200 | NULL | — | — | — | UQ_UIP_user_custom_name (Filtered, 2/2) | CK_UIP_target; CK_UIP_custom_name_not_blank (custom_ingredient_name IS NULL OR LEN(TRIM(NCHAR(9)+NCHAR(10)+NCHAR(13)+NCHAR(32)+NCHAR(160) FROM custom_ingredient_name)) > 0) | UQ_UIP_user_custom_name (Nonclustered, (user_id, custom_ingredient_name) WHERE ingredient_id IS NULL AND custom_ingredient_name IS NOT NULL) | — | FR-31 | BR-13 | — | — |
| `preference_type` | Loại hiệu lực của nguyên liệu (Q11): `AVOID` (cần tránh do dị ứng/kiêng) / `DISLIKE` (không thích). Mỗi nguyên liệu chỉ có **một** loại trên một user | Enum | VARCHAR | 20 | NOT NULL | — | — | — | — | CK_UIP_preference_type ('AVOID', 'DISLIKE') — bỏ `ALLERGY` | — | — | FR-31 | BR-13 | — | `AVOID` có thể chứa thông tin dị ứng — **dữ liệu sức khỏe** |
| `created_at` | Thời điểm khai báo | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_UIP_created_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |

**Ràng buộc không vẽ được bằng đường nối:**

- Mỗi dòng phải xác định được nguyên liệu: có `ingredient_id`, có `custom_ingredient_name`, hoặc có cả hai.
- Chống trùng theo `Q11`: một nguyên liệu chuẩn chỉ xuất hiện **một lần** trên một user (`UQ_UIP_user_ingredient`); tên tự nhập cũng vậy (`UQ_UIP_user_custom_name`). Hai index **không** chứa `preference_type`. Khi người dùng chuyển nguyên liệu sang danh sách khác, service **cập nhật** `preference_type` thay vì chèn dòng mới.
- Việc so trùng tên tự nhập phụ thuộc collation của database. Với `SQL_Latin1_General_CP1_CI_AS`: khác hoa/thường hoặc khác khoảng trắng cuối vẫn bị coi là trùng, nhưng khác khoảng trắng đầu thì **không**. Service phải trim tên trước khi lưu.

### 4.4 EXPERT_APPLICATION 🆕

**Mục đích:** Đơn đăng ký cấp quyền Chuyên gia của Customer, theo format văn bản. **Tuyệt đối không lưu tệp chứng chỉ vật lý.**

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `application_id` | Định danh đơn | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_EXPERT_APPLICATION (Clustered) | PK | FR-05 | BR-21 | — | — |
| `user_id` | Customer nộp đơn | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_EXPERT_APP_USER → USER(user_id) [NO ACTION] | UQ_EXPERT_APP_pending (Filtered) | — | UQ_EXPERT_APP_pending (Nonclustered, WHERE status='PENDING'), IX_EXPERT_APP_user | `USER` 1 → `EXPERT_APPLICATION` 0..* | FR-05 | BR-21 | — | — |
| `bio_experience` | Mô tả kinh nghiệm bản thân | Text | NVARCHAR | MAX | NOT NULL | — | — | — | — | — | — | — | FR-05 | BR-21 | — | — |
| `vegetarian_type` | Trường phái chay chuyên sâu của cá nhân: 4 giá trị trên **cộng** "Chay thực dưỡng dưỡng sinh" — tổng 5 giá trị | Enum | VARCHAR | 30 | NOT NULL | — | — | — | — | CK_EXPERT_APP_vegetarian_type ('VEGAN', 'LACTO', 'OVO', 'LACTO_OVO', 'MACROBIOTIC') | — | — | FR-05 | — | — | — |
| `sample_recipe_summary` | Tóm tắt công thức mẫu để chứng minh năng lực | Text | NVARCHAR | MAX | NOT NULL | — | — | — | — | — | — | — | FR-05 | BR-21 | — | — |
| `portfolio_url` | Liên kết hồ sơ ngoài | Text, optional | VARCHAR | 2048 | NULL | — | — | — | — | — | — | — | FR-05 | — | — | Không tải tệp lên hệ thống |
| `status` | `PENDING` / `APPROVED` / `REJECTED` | Enum | VARCHAR | 20 | NOT NULL | 'PENDING' (DF_EXPERT_APP_status) | — | — | — | CK_EXPERT_APP_status ('PENDING', 'APPROVED', 'REJECTED') | — | — | FR-05 | BR-21, BR-22 | — | Chỉ Admin đổi được |
| `admin_note` | Lý do duyệt hoặc từ chối | Text, optional | NVARCHAR | 1000 | NULL | — | — | — | — | — | — | — | FR-05 | BR-22 | — | Chỉ Admin xem |
| `reviewed_by` | Admin đã xử lý đơn | Identifier, FK, optional | BIGINT | — | NULL | — | — | FK_EXPERT_APP_REVIEWER → USER(user_id) [NO ACTION] | — | — | — | `USER` (vai trò ADMIN) | FR-05 | BR-26 | — | — |
| `reviewed_at` | Thời điểm xử lý | Timestamp, optional | DATETIME2 | 7 | NULL | — | — | — | — | — | — | — | FR-05 | — | — | — |
| `created_at` | Thời điểm nộp đơn | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_EXPERT_APP_created_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_EXPERT_APP_updated_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |

**Quyết định `Q6` (24/09/2026):** `reviewed_by` là khóa ngoại thật trỏ về `USER.user_id`, nhưng **không được vẽ thành connector trên Conceptual ERD** — ma trận giữ nguyên 36 connector. Quan hệ này chỉ tồn tại ở Data Dictionary và schema. Cách thể hiện trên Physical ERD do người thực hiện pha 2 quyết định.

**Ràng buộc nghiệp vụ:** mỗi Customer chỉ được có tối đa một đơn ở trạng thái `PENDING` tại một thời điểm. Khi `status` chuyển `APPROVED`, `USER.role` đổi thành `EXPERT`.

### 4.5 RECIPE_POST

**Mục đích:** Bài viết công thức nấu ăn chay do Chuyên gia làm tác giả. Không dùng bảng `Category`, không dùng bảng `Recipe Step`.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `recipe_id` | Định danh bài công thức | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_RECIPE_POST (Clustered) | PK; đầu `1` của 8 quan hệ xuất phát | FR-04 | — | — | — |
| `author_id` | Tác giả, gắn với tài khoản Chuyên gia | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_RECIPE_POST_USER → USER(user_id) [NO ACTION] | — | — | IX_RECIPE_POST_author (Nonclustered) | `USER` 1 → `RECIPE_POST` 0..*; bài bắt buộc thuộc đúng 1 tác giả | FR-04, FR-23 | BR-17 | — | — |
| `title` | Tiêu đề, 3–120 ký tự | Text | NVARCHAR | 120 | NOT NULL | — | — | — | — | CK_RECIPE_POST_title_len (LEN >= 3) | — | — | FR-16 | BR-19 | AC-16.1 | — |
| `description` | Mô tả giới thiệu, tối đa 2.000 ký tự, tùy chọn | Text, optional | NVARCHAR | 2000 | NULL | — | — | — | — | — | — | — | FR-16 | BR-20 | — | — |
| `instructions` 🆕 | Hướng dẫn chế biến dạng văn bản tự do, **bắt buộc** 10–5.000 ký tự sau trim; không ép phân rã theo bước | Text | NVARCHAR | MAX | NOT NULL | — | — | — | — | CK_RECIPE_POST_instructions_len (LEN(instructions) BETWEEN 10 AND 5000) — V1 hiện chỉ chặn `>= 10` | — | Thay thế bảng `RECIPE_STEP` đã bị loại bỏ | FR-16 | BR-19 | AC-16.5 | — |
| `dish_category` 🆕 | Thể loại món, **bắt buộc** khi công khai. Lưu technical code, 11 giá trị: `NOODLE_SOUP`, `STIR_FRY`, `HOT_POT`, `BRAISED`, `SOUP`, `FRIED`, `STEAMED`, `SALAD`, `ROLL`, `GRILLED`, `DESSERT`. Frontend hiển thị nhãn tiếng Việt | Enum, `CHECK` 11 giá trị | VARCHAR | 20 | NOT NULL | — | — | — | — | CK_RECIPE_POST_dish_category (11 codes) | IX_RECIPE_POST_dish_category (Nonclustered) | Thay thế bảng `CATEGORY` đã bị loại bỏ | FR-07, FR-08 | — | AC-07.3, AC-08.3 | — |
| `vegetarian_type` | Loại ăn chay của món: Vegan / Lacto / Ovo / Lacto-Ovo (4 giá trị) | Enum | VARCHAR | 20 | NOT NULL | — | — | — | — | CK_RECIPE_POST_vegetarian_type ('VEGAN', 'LACTO', 'OVO', 'LACTO_OVO') | — | — | FR-07, FR-08 | — | AC-07.3 | — |
| `difficulty` | Độ khó chế biến | Enum | VARCHAR | 10 | NOT NULL | — | — | — | — | CK_RECIPE_POST_difficulty ('EASY', 'MEDIUM', 'HARD') | — | — | FR-08 | — | — | — |
| `servings` | Số khẩu phần gốc, hợp lệ 1–50 | Integer | INT | — | NOT NULL | — | — | — | — | CK_RECIPE_POST_servings (1..50) | — | Cơ sở quy đổi dinh dưỡng theo khẩu phần | FR-16 | BR-43 | AC-16.2 | — |
| `prep_time_min` | Thời gian chuẩn bị, 0–1.440 phút | Integer | INT | — | NOT NULL | — | — | — | — | CK_RECIPE_POST_prep_time (0..1440) | — | — | FR-16 | BR-19 | — | — |
| `cook_time_min` | Thời gian nấu, 0–1.440 phút; tổng hai mốc phải > 0 | Integer | INT | — | NOT NULL | — | — | — | — | CK_RECIPE_POST_cook_time (0..1440); CK_RECIPE_POST_total_time (prep_time_min + cook_time_min > 0) | — | — | FR-16 | BR-19 | — | — |
| `youtube_url` | Liên kết YouTube để nhúng, tối đa 1 | Text, optional | VARCHAR | 2048 | NULL | — | — | — | — | — | — | 0..1 mỗi bài | FR-15 | BR-10 | — | Không tải tệp video lên |
| `status` | Trạng thái bài viết: `PUBLISHED` / `HIDDEN` / `DELETED`. **Không có `DRAFT`** — FR-24 (lưu nháp trên server) là `OUT_OF_SCOPE`; bài hợp lệ lưu thẳng `PUBLISHED`, `DELETED` phục vụ xóa mềm | Enum | VARCHAR | 20 | NOT NULL | 'PUBLISHED' (DF_RECIPE_POST_status) — V1 hiện là `DRAFT` | — | — | — | CK_RECIPE_POST_status ('PUBLISHED', 'HIDDEN', 'DELETED') | IX_RECIPE_POST_status_published (status, published_at DESC) | — | FR-25, FR-28 | BR-07, BR-27 | — | Chỉ Admin ẩn được |
| `published_at` | Thời điểm công khai | Timestamp, optional | DATETIME2 | 7 | NULL | — | — | — | — | — | IX_RECIPE_POST_status_published | — | FR-25 | BR-07 | — | — |
| `created_at` | Thời điểm tạo | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_RECIPE_POST_created_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm sửa gần nhất | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_RECIPE_POST_updated_at) | — | — | — | — | — | — | FR-44 | BR-62 | — | — |
| `like_count` 🆕 | Tổng lượt Like, mặc định `0`, **không âm**. Cập nhật **đồng bộ trong cùng transaction** với `RECIPE_REACTION` | Integer | INT | — | NOT NULL | 0 (DF_RECIPE_POST_like_count) | — | — | — | CK_RECIPE_POST_like_count (>= 0) | — | Bộ đếm dẫn xuất từ `RECIPE_REACTION` | FR-08, FR-17, FR-57 | BR-69, BR-71, BR-72 | — | — |
| `dislike_count` 🆕 | Tổng lượt Dislike, mặc định `0`, **không âm**. Cập nhật **đồng bộ trong cùng transaction** với `RECIPE_REACTION` | Integer | INT | — | NOT NULL | 0 (DF_RECIPE_POST_dislike_count) | — | — | — | CK_RECIPE_POST_dislike_count (>= 0) | — | Bộ đếm dẫn xuất từ `RECIPE_REACTION` | FR-08, FR-17, FR-57 | BR-69 | — | — |
| `view_count` 🆕 | Tổng lượt xem, mặc định `0`, **không âm**. Cập nhật **bất đồng bộ** theo ARCHITECTURE.md mục 6, không nghẽn luồng đọc | Integer | INT | — | NOT NULL | 0 (DF_RECIPE_POST_view_count) | — | — | — | CK_RECIPE_POST_view_count (>= 0) | — | Bộ đếm dẫn xuất từ `RECIPE_VIEW` | FR-08, FR-17, FR-58 | BR-70, BR-71, BR-72 | — | — |

**Ngoại lệ 3NF có chủ đích (Tech Lead chốt 23/09/2026):** ba cột `like_count`, `dislike_count`, `view_count` là dữ liệu **dẫn xuất** từ `RECIPE_REACTION` và `RECIPE_VIEW` — vi phạm 3NF một cách có kiểm soát để phục vụ 6 chế độ sắp xếp bằng truy vấn SQL thuần (BR-71, BR-72) và để hiển thị trên **mọi thẻ món** ở trang danh sách (FR-17). Hai bảng sự kiện vẫn là **nguồn dữ liệu gốc**; khi lệch phải tính lại từ chúng.

**`like_percentage` không phải là cột** — tính khi đọc từ `like_count` và `dislike_count`; hiển thị nhãn `Mới` khi chưa có lượt bình chọn nào (FR-17, BR-69).

### 4.6 RECIPE_MEDIA

**Mục đích:** 0–5 ảnh minh họa trên Azure Blob Storage, có thứ tự hiển thị và **đúng một** ảnh bìa.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `media_id` | Định danh ảnh | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_RECIPE_MEDIA (Clustered) | PK | FR-14 | — | — | — |
| `recipe_id` | Bài công thức chứa ảnh | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_RECIPE_MEDIA_RECIPE_POST → RECIPE_POST(recipe_id) [CASCADE] | UQ_RECIPE_MEDIA_cover (Filtered); UQ_RECIPE_MEDIA_order (1/2) | — | UQ_RECIPE_MEDIA_cover (Nonclustered, WHERE is_cover=1) | `RECIPE_POST` 1 → `RECIPE_MEDIA` 0..* | FR-14 | BR-20 | — | — |
| `blob_url` | Đường dẫn tệp trên Azure Blob Storage | Text | VARCHAR | 2048 | NOT NULL | — | — | — | — | — | — | — | FR-14 | BR-11 | — | **Không ghi SAS URL vào log** |
| `mime_type` | Định dạng ảnh: JPEG / PNG / WebP | Enum | VARCHAR | 20 | NOT NULL | — | — | — | — | CK_RECIPE_MEDIA_mime_type ('image/jpeg', 'image/png', 'image/webp') | — | — | FR-14 | BR-11 | — | — |
| `display_order` | Thứ tự hiển thị trong thư viện, **đánh số từ 1**, miền 1..5 | Integer | INT | — | NOT NULL | — | — | — | UQ_RECIPE_MEDIA_order (recipe_id, display_order) (2/2) | CK_RECIPE_MEDIA_display_order (display_order BETWEEN 1 AND 5) | — | — | FR-14 | BR-20 | — | — |
| `is_cover` | Cờ ảnh bìa | Boolean | BIT | — | NOT NULL | 0 (DF_RECIPE_MEDIA_is_cover) | — | — | — | — | — | — | FR-14, FR-17 | BR-20 | — | — |

**Ràng buộc không vẽ được bằng đường nối:**

| Quy tắc | Ép ở đâu |
|---|---|
| **Tối đa 5** ảnh mỗi bài | **Database**: `UQ_RECIPE_MEDIA_order` + `CK_RECIPE_MEDIA_display_order`. Chỉ có 5 giá trị `display_order` hợp lệ và không được trùng, nên không thể có ảnh thứ 6. Đã chạy thử trên SQL Server 2019 |
| **Không quá 1** ảnh bìa | **Database**: filtered unique index `UQ_RECIPE_MEDIA_cover` |
| **Phải có 1** ảnh bìa khi bài có ảnh | **Service** — constraint không ép được sự tồn tại. Kiểm tra khi lưu/công khai theo FR-14 EF-14.2 và AC-14.3 |

**Lưu ý cho Backend:** đổi thứ tự ảnh phải dùng **một** câu `UPDATE ... SET display_order = CASE ... END`, hoặc xóa rồi chèn lại trong cùng transaction. Cập nhật từng dòng một (kiểu Hibernate flush từng entity) sẽ vi phạm `UQ_RECIPE_MEDIA_order` ở bước giữa.

### 4.7 RECIPE_INGREDIENT

**Mục đích:** Dòng nguyên liệu trong một bài công thức, kèm định lượng số dương và đơn vị đo chuẩn.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `recipe_ingredient_id` | Định danh dòng nguyên liệu | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_RECIPE_INGREDIENT (Clustered) | PK | FR-19 | — | — | — |
| `recipe_id` | Bài công thức chứa dòng này | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_RECIPE_INGREDIENT_RECIPE → RECIPE_POST(recipe_id) [CASCADE] | — | — | — | `RECIPE_POST` 1 → `RECIPE_INGREDIENT` **1..\*** (bắt buộc ≥1, tối đa 50) | FR-16, FR-19 | BR-19 | AC-16.2 | — |
| `ingredient_id` | Liên kết tới nguyên liệu chuẩn | Identifier, FK, **optional** | BIGINT | — | NULL | — | — | FK_RECIPE_INGREDIENT_INGREDIENT → INGREDIENT(ingredient_id) [NO ACTION] | — | CK_RECIPE_INGREDIENT_target (ingredient_id IS NOT NULL OR custom_ingredient_name IS NOT NULL) | — | `INGREDIENT` 0..1 → 0..*; cho phép để trống để tác giả nhập tên tự do | FR-19 | BR-12, BR-13 | — | — |
| `unit_id` | Đơn vị đo, **bắt buộc** | Identifier, FK | INT | — | NOT NULL | — | — | FK_RECIPE_INGREDIENT_UNIT → UNIT(unit_id) [NO ACTION] | — | — | — | `RECIPE_INGREDIENT` 0..* → `UNIT` **1** | FR-19 | BR-14, BR-73 | — | — |
| `custom_ingredient_name` | Tên nguyên liệu tác giả tự nhập; **vẫn được giữ** sau khi Admin liên kết nguyên liệu chuẩn (SRS 3.4). Không được rỗng hoặc chỉ gồm khoảng trắng | Text, optional | NVARCHAR | 200 | NULL | — | — | — | — | CK_RECIPE_INGREDIENT_target; CK_RECIPE_INGREDIENT_custom_name_not_blank (custom_ingredient_name IS NULL OR LEN(TRIM(NCHAR(9)+NCHAR(10)+NCHAR(13)+NCHAR(32)+NCHAR(160) FROM custom_ingredient_name)) > 0) | — | — | FR-19 | BR-12 | — | — |
| `quantity` | Định lượng, **bắt buộc là số thực dương > 0**; cấm tuyệt đối giá trị phi số học như "vừa đủ" | Decimal | DECIMAL | 10,2 | NOT NULL | — | — | — | — | CK_RECIPE_INGREDIENT_quantity (> 0) | — | — | FR-19 | BR-14, BR-73 | — | — |

**Ít nhất một, không XOR:** mỗi dòng phải có `ingredient_id`, `custom_ingredient_name`, hoặc cả hai. SRS 3.4 yêu cầu *"khi chuẩn hóa sau vẫn giữ tên đã nhập"*, nên sau khi Admin liên kết, dòng có **cả hai** giá trị; ràng buộc XOR sẽ chặn đúng trạng thái này. Không dùng `LTRIM/RTRIM` để kiểm tra tên rỗng vì hai hàm này không bỏ tab hay xuống dòng. `TRIM(... FROM ...)` cần SQL Server 2017 trở lên.

**Cổng kiểm định xuất bản:** nếu tổ hợp nguyên liệu + đơn vị không quy đổi được sang gram qua `INGREDIENT_UNIT_CONVERSION` thì **chặn công khai** bài viết (BR-48, BR-73).

### 4.8 RECIPE_REACTION

**Mục đích:** Bình chọn Like / Dislike của Member đối với bài công thức, phục vụ tỷ lệ % hài lòng kiểu Samsung Food.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `user_id` | Người bình chọn | Identifier, FK | BIGINT | — | NOT NULL | — | PK (Composite 1/2) | FK_RECIPE_REACTION_USER → USER(user_id) [NO ACTION] | — | — | PK_RECIPE_REACTION (Clustered) | `USER` 1 → `RECIPE_REACTION` 0..* | FR-57 | BR-69 | — | — |
| `recipe_id` | Bài được bình chọn | Identifier, FK | BIGINT | — | NOT NULL | — | PK (Composite 2/2) | FK_RECIPE_REACTION_RECIPE → RECIPE_POST(recipe_id) [NO ACTION] | — | — | IX_RECIPE_REACTION_recipe (Nonclustered) | `RECIPE_POST` 1 → `RECIPE_REACTION` 0..* | FR-57 | BR-69 | — | — |
| `reaction_type` | `LIKE` hoặc `DISLIKE`; cho phép toggle và đổi chiều | Enum | VARCHAR | 10 | NOT NULL | — | — | — | — | CK_RECIPE_REACTION_type ('LIKE', 'DISLIKE') | — | — | FR-57 | BR-69 | — | — |
| `created_at` | Lần bình chọn đầu tiên | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_RECIPE_REACTION_created_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |
| `updated_at` | Lần đổi chiều gần nhất | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_RECIPE_REACTION_updated_at) | — | — | — | — | — | — | FR-57 | BR-69 | — | — |

**Cập nhật bộ đếm:** mọi thao tác thêm, đổi chiều hoặc gỡ bình chọn phải cập nhật `RECIPE_POST.like_count` / `dislike_count` **đồng bộ trong cùng transaction** (Tech Lead chốt 23/09/2026).

**Ràng buộc không vẽ được bằng đường nối:** khóa chính kép `(user_id, recipe_id)` bảo đảm **1 phản hồi / user / bài**; **tác giả không được tự bình chọn bài của mình** — ràng buộc này database không ép được bằng constraint thông thường, phải thực thi ở tầng service và ghi rõ trong pha 2.

### 4.9 RECIPE_VIEW

**Mục đích:** Sự kiện xem bài, phục vụ khử trùng lặp cửa sổ 30 phút và thống kê 24h / 7d / 30d / toàn thời gian.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `view_id` | Định danh lượt xem | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_RECIPE_VIEW (Clustered) | PK | FR-58 | — | — | — |
| `recipe_id` | Bài được xem | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_RECIPE_VIEW_RECIPE → RECIPE_POST(recipe_id) [NO ACTION] | — | — | IX_RECIPE_VIEW_recipe_time (recipe_id, viewed_at) | `RECIPE_POST` 1 → `RECIPE_VIEW` 0..* | FR-58 | BR-70 | — | — |
| `user_id` | Member đã đăng nhập, **để trống nếu là Guest** | Identifier, FK, optional | BIGINT | — | NULL | — | — | FK_RECIPE_VIEW_USER → USER(user_id) [SET NULL] | — | — | IX_RECIPE_VIEW_user (Nonclustered, WHERE user_id IS NOT NULL) | `USER` **0..1** → `RECIPE_VIEW` 0..*; cho phép ghi lượt xem của Guest mà không tạo bản ghi ảo trong `USER` | FR-58 | BR-70 | — | — |
| `anonymous_viewer_hash` | Định danh Guest qua IP hash / client session | Text, optional | VARCHAR | 128 | NULL | — | — | — | — | — | — | — | FR-58 | BR-70 | — | **Không lưu IP thô**; chỉ lưu giá trị đã băm |
| `viewed_at` | Thời điểm xem, cơ sở tính cửa sổ khử trùng lặp | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_RECIPE_VIEW_viewed_at) | — | — | — | — | IX_RECIPE_VIEW_recipe_time | — | FR-58 | BR-70 | — | — |

**Cập nhật bộ đếm:** `RECIPE_POST.view_count` cập nhật **bất đồng bộ** (`@Async` hoặc in-memory buffer định kỳ flush), khác với Like/Dislike — theo ARCHITECTURE.md mục 6.

**Ràng buộc không vẽ được bằng đường nối:** cửa sổ khử trùng lặp **30 phút** theo cặp `(user_id | anonymous_viewer_hash, recipe_id)` — logic tầng ứng dụng, không phải constraint.

### 4.10 COMMENT

**Mục đích:** Bình luận và phản hồi lồng nhiều cấp trên bài công thức.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `comment_id` | Định danh bình luận | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | UQ_COMMENT_id_recipe_depth (1/3) | — | PK_COMMENT (Clustered) | PK | FR-46 | — | — | — |
| `recipe_id` | Bài công thức chứa bình luận | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_COMMENT_RECIPE → RECIPE_POST(recipe_id) [NO ACTION]; FK_COMMENT_PARENT (2/3) | UQ_COMMENT_id_recipe_depth (2/3) | — | IX_COMMENT_recipe (recipe_id, created_at) | `RECIPE_POST` 1 → `COMMENT` 0..* | FR-46 | BR-66 | — | — |
| `user_id` | Người viết | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_COMMENT_USER → USER(user_id) [NO ACTION] | — | — | — | `USER` 1 → `COMMENT` 0..* | FR-46 | BR-66 | — | — |
| `parent_comment_id` | Bình luận cha; `NULL` nếu là bình luận gốc | Identifier, FK, optional | BIGINT | — | NULL | — | — | FK_COMMENT_PARENT (parent_comment_id, recipe_id, parent_depth) → COMMENT(comment_id, recipe_id, depth) [NO ACTION] — thay FK một cột hiện tại | — | CK_COMMENT_root_reply | IX_COMMENT_parent (Nonclustered, WHERE parent_comment_id IS NOT NULL) | **Tự tham chiếu:** `COMMENT` 0..1 → `COMMENT` 0..* | FR-46 | BR-66 | — | — |
| `content` | Nội dung bình luận (1–1.000 ký tự) | Text | NVARCHAR | 1000 | NOT NULL | — | — | — | — | CK_COMMENT_content_len (1..1000) | — | — | FR-46 | BR-66 | EF-46.1 | — |
| `depth` | Cấp lồng, 1–5 | Integer | INT | — | NOT NULL | 1 (DF_COMMENT_depth) | — | — | UQ_COMMENT_id_recipe_depth (3/3) | CK_COMMENT_depth (1..5); CK_COMMENT_root_reply | — | — | FR-46 | BR-66 | — | Technical design — cần thiết vì database không tự giới hạn độ sâu đệ quy |
| `parent_depth` 🆕 | Bản sao `depth` của bình luận cha, dùng cho FK kép ép `depth = parent_depth + 1`; `NULL` khi là bình luận gốc. **Technical design — chỉ Physical ERD (Q8)** | — (không thuộc Logical ERD) | INT | — | NULL | — | — | FK_COMMENT_PARENT (3/3) | — | CK_COMMENT_root_reply ((parent_comment_id IS NULL AND parent_depth IS NULL AND depth = 1) OR (parent_comment_id IS NOT NULL AND parent_depth IS NOT NULL AND depth = parent_depth + 1)) | — | — | FR-46 | BR-66 | — | — |
| `is_deleted` | Cờ tombstone khi bình luận cha bị xóa nhưng còn phản hồi | Boolean | BIT | — | NOT NULL | 0 (DF_COMMENT_is_deleted) | — | — | — | — | — | — | FR-46 | BR-66 | — | — |
| `created_at` | Thời điểm viết | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_COMMENT_created_at) | — | — | — | — | IX_COMMENT_recipe | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm sửa gần nhất | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_COMMENT_updated_at) | — | — | — | — | — | — | FR-46 | — | — | — |

**Ràng buộc BR-66 và nơi ép:**

| Quy tắc | Ép ở đâu |
|---|---|
| Reply thuộc cùng `recipe_id` với bình luận cha | **Database**: `FK_COMMENT_PARENT` gồm cả `recipe_id` |
| Bình luận gốc có `parent_comment_id IS NULL`; reply có `IS NOT NULL` | **Database**: `CK_COMMENT_root_reply` — gốc khi và chỉ khi `depth = 1` |
| `depth` của reply = `depth` cha + 1 | **Database**: `parent_depth` nằm trong `FK_COMMENT_PARENT` + `CK_COMMENT_root_reply` (Q8) |
| Cây không quá **5 cấp** | **Database**: suy ra từ ba quy tắc trên + `CK_COMMENT_depth`. Vì `depth` tăng nghiêm ngặt nên cũng không tạo được vòng lặp. Reply vào cấp 5 được service gắn vào **cha của cấp 5** (FR-46 Bước 4) |
| Xóa bình luận cha còn phản hồi thì chuyển **tombstone** (`is_deleted = 1`), không xóa cứng | **Service**. FK tự tham chiếu **không được** cascade — SQL Server từ chối tạo |

Các phương án trên đã chạy thử trên SQL Server 2019. Khi insert reply, service gán `parent_depth` bằng `depth` của cha và `depth = parent_depth + 1`. `FK_COMMENT_PARENT` từ chối mọi giá trị sai lệch với dòng cha thật.

### 4.11 REPORT

**Mục đích:** Báo cáo vi phạm từ người dùng, đã gộp kết quả và lý do xử lý của Admin (bỏ bảng `moderation_action`).

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `report_id` | Định danh báo cáo | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_REPORT (Clustered) | PK | FR-26 | — | — | — |
| `reporter_id` | Người gửi báo cáo | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_REPORT_USER → USER(user_id) [NO ACTION] | UQ_REPORT_open_recipe, UQ_REPORT_open_comment (Filtered) | — | UQ_REPORT_open_recipe, UQ_REPORT_open_comment | `USER` 1 → `REPORT` 0..* | FR-26 | BR-24 | — | **Danh tính người báo cáo phải được bảo vệ**, BR-28 |
| `recipe_id` | Bài công thức bị báo cáo | Identifier, FK, optional | BIGINT | — | NULL | — | — | FK_REPORT_RECIPE → RECIPE_POST(recipe_id) [NO ACTION] | UQ_REPORT_open_recipe (Filtered) | CK_REPORT_target_xor (XOR với comment_id) | UQ_REPORT_open_recipe | `RECIPE_POST` 0..1 → `REPORT` 0..* | FR-26 | BR-23 | — | — |
| `comment_id` | Bình luận bị báo cáo | Identifier, FK, optional | BIGINT | — | NULL | — | — | FK_REPORT_COMMENT → COMMENT(comment_id) [NO ACTION] | UQ_REPORT_open_comment (Filtered) | CK_REPORT_target_xor (XOR với recipe_id) | UQ_REPORT_open_comment | `REPORT` 0..* → `COMMENT` 0..1 | FR-48 | BR-23 | — | — |
| `reason_code` | Một trong 6 nhóm lý do: `NON_VEGAN`, `FOOD_SAFETY_HAZARD`, `INAPPROPRIATE_CONTENT`, `COPYRIGHT_VIOLATION`, `SPAM_ADVERTISING`, `OTHER` | Enum | VARCHAR | 30 | NOT NULL | — | — | — | — | CK_REPORT_reason_code (6 codes) | — | — | FR-27 | BR-25 | — | — |
| `description` | Mô tả bổ sung; **bắt buộc 10–500 ký tự** khi `reason_code = OTHER`, tùy chọn tối đa 500 ký tự với các lý do còn lại | Text | NVARCHAR | 500 | NULL | — | — | — | — | — | — | — | FR-27 | BR-25 | — | — |
| `status` | Trạng thái xử lý báo cáo | Enum | VARCHAR | 20 | NOT NULL | 'PENDING' (DF_REPORT_status) | — | — | — | CK_REPORT_status ('PENDING', 'PROCESSING', 'RESOLVED', 'REJECTED') | IX_REPORT_status (status, created_at) | — | FR-28 | BR-26 | — | Chỉ Admin xem và đổi, BR-29 |
| `decision` | Quyết định chế tài của Admin | Enum, optional | VARCHAR | 30 | NULL | — | — | — | — | — | — | — | FR-28 | BR-26 | — | Chỉ Admin |
| `decision_reason` | Lý do quyết định | Text, optional | NVARCHAR | 1000 | NULL | — | — | — | — | — | — | — | FR-28 | BR-26 | — | Chỉ Admin |
| `created_at` | Thời điểm gửi báo cáo | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_REPORT_created_at) | — | — | — | — | IX_REPORT_status | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_REPORT_updated_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |
| `handled_at` | Thời điểm Admin xử lý xong | Timestamp, optional | DATETIME2 | 7 | NULL | — | — | — | — | — | — | — | FR-28 | BR-26 | — | — |

**Ràng buộc XOR không vẽ được bằng đường nối:** một báo cáo trỏ tới **hoặc** `recipe_id` **hoặc** `comment_id`, không bao giờ cả hai và không bao giờ rỗng cả hai. Chống trùng: không cho tạo báo cáo mới khi đã tồn tại báo cáo đang mở của cùng người trên cùng đối tượng (BR-29).

### 4.12 NOTIFICATION

**Mục đích:** Thông báo trong ứng dụng gửi tới người dùng.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `notification_id` | Định danh thông báo | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_NOTIFICATION (Clustered) | PK | FR-49 | — | — | — |
| `user_id` | Người nhận | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_NOTIFICATION_USER → USER(user_id) [NO ACTION] | — | — | IX_NOTIFICATION_user_inbox (user_id, is_read, created_at DESC) | `USER` 1 → `NOTIFICATION` 0..* | FR-49 | — | — | Chỉ chủ sở hữu xem được |
| `comment_id` | Bình luận kích hoạt thông báo | Identifier, FK, optional | BIGINT | — | NULL | — | — | FK_NOTIFICATION_COMMENT → COMMENT(comment_id) [NO ACTION] | — | — | — | `COMMENT` 0..1 → `NOTIFICATION` 0..* | FR-49 | — | — | — |
| `report_id` | Báo cáo kích hoạt thông báo kết quả | Identifier, FK, optional | BIGINT | — | NULL | — | — | FK_NOTIFICATION_REPORT → REPORT(report_id) [NO ACTION] | — | — | — | `REPORT` 0..1 → `NOTIFICATION` 0..* | FR-49 | — | — | — |
| `notification_type` | Loại sự kiện sinh ra thông báo | Enum | VARCHAR | 30 | NOT NULL | — | — | — | — | — | — | — | FR-49 | — | — | — |
| `title` | Tiêu đề hiển thị | Text | NVARCHAR | 200 | NOT NULL | — | — | — | — | — | — | — | FR-49 | — | — | — |
| `message` | Nội dung hiển thị | Text | NVARCHAR | 1000 | NOT NULL | — | — | — | — | — | — | — | FR-49 | — | — | — |
| `is_read` | Đã đọc hay chưa | Boolean | BIT | — | NOT NULL | 0 (DF_NOTIFICATION_is_read) | — | — | — | — | IX_NOTIFICATION_user_inbox | — | FR-49 | — | — | — |
| `created_at` | Thời điểm sinh thông báo | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_NOTIFICATION_created_at) | — | — | — | — | IX_NOTIFICATION_user_inbox | — | Technical design — audit | — | — | — |
| `read_at` | Thời điểm đọc | Timestamp, optional | DATETIME2 | 7 | NULL | — | — | — | — | — | — | — | FR-49 | — | — | — |

### 4.13 SAVED_RECIPE

**Mục đích:** Đánh dấu lưu lại bài công thức yêu thích (bookmark).

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `user_id` | Người lưu | Identifier, FK | BIGINT | — | NOT NULL | — | PK (Composite 1/2) | FK_SAVED_RECIPE_USER → USER(user_id) [NO ACTION] | — | — | PK_SAVED_RECIPE (Clustered) | `USER` 1 → `SAVED_RECIPE` 0..* | FR-32 | BR-32, BR-34 | — | Chỉ chủ sở hữu xem được |
| `recipe_id` | Bài được lưu | Identifier, FK | BIGINT | — | NOT NULL | — | PK (Composite 2/2) | FK_SAVED_RECIPE_RECIPE → RECIPE_POST(recipe_id) [NO ACTION] | — | — | — | `RECIPE_POST` 1 → `SAVED_RECIPE` 0..* | FR-32 | BR-33, BR-34 | — | — |
| `saved_at` | Thời điểm lưu | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_SAVED_RECIPE_saved_at) | — | — | — | — | — | — | FR-32 | — | — | — |

**Ràng buộc:** khóa chính kép `(user_id, recipe_id)` bảo đảm **tính duy nhất của bản ghi lưu** (BR-34). Vòng đời độc lập với `MEAL_PLAN` (BR-35).

### 4.14 MEAL_PLAN

**Mục đích:** Kế hoạch thực đơn **theo tuần**: lưới 7 ngày Thứ Hai đến Chủ Nhật, mỗi ngày đúng 3 bữa cố định (FR-09, BR-36).

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `meal_plan_id` | Định danh kế hoạch | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | UQ_MEAL_PLAN_id_week (1/2) — đích của FK kép từ `MEAL_PLAN_ENTRY` | — | PK_MEAL_PLAN (Clustered) | PK | FR-09 | — | — | — |
| `user_id` | Chủ sở hữu kế hoạch | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_MEAL_PLAN_USER → USER(user_id) [NO ACTION] | UQ_MEAL_PLAN_user_week (1/2) | — | IX_MEAL_PLAN_user (Nonclustered) | `USER` 1 → `MEAL_PLAN` 0..*; tối đa 1 kế hoạch cho mỗi tuần | FR-09 | BR-32 | UC-09.1 | Chỉ chủ sở hữu xem được |
| `week_start_date` | Ngày Thứ Hai bắt đầu tuần kế hoạch; **bắt buộc là Thứ Hai** (tuần Thứ Hai–Chủ Nhật) | Date | DATE | — | NOT NULL | — | — | — | UQ_MEAL_PLAN_user_week (2/2), UQ_MEAL_PLAN_id_week (2/2) | CK_MEAL_PLAN_week_start_monday (DATEDIFF(DAY, CONVERT(DATE, '19000101', 112), week_start_date) % 7 = 0) — không dùng `DATEPART(weekday)` vì phụ thuộc `SET DATEFIRST` | — | Phạm vi tuần đã chốt tại FR-09 | FR-09 | BR-36 | — | — |
| `created_at` | Thời điểm tạo | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_MEAL_PLAN_created_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_MEAL_PLAN_updated_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |

### 4.15 MEAL_PLAN_ENTRY

**Mục đích:** Món ăn cụ thể được phân bổ vào từng ngày và từng bữa.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `meal_plan_entry_id` | Định danh mục lịch ăn | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_MEAL_PLAN_ENTRY (Clustered) | PK | FR-33 | — | — | — |
| `meal_plan_id` | Kế hoạch chứa mục này | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_MPE_MEAL_PLAN (meal_plan_id, meal_week_start) → MEAL_PLAN(meal_plan_id, week_start_date) [CASCADE] — thay FK một cột hiện tại | UQ_MPE_unique_slot (1/4) | — | UQ_MPE_unique_slot | `MEAL_PLAN` 1 → `MEAL_PLAN_ENTRY` 0..* | FR-33 | — | — | — |
| `recipe_id` | Món được xếp lịch | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_MPE_RECIPE → RECIPE_POST(recipe_id) [NO ACTION] | UQ_MPE_unique_slot (4/4) | — | UQ_MPE_unique_slot | `RECIPE_POST` 1 → `MEAL_PLAN_ENTRY` 0..* | FR-33 | BR-35 | — | — |
| `meal_date` | Ngày ăn | Date | DATE | — | NOT NULL | — | — | — | UQ_MPE_unique_slot (2/4) | — | UQ_MPE_unique_slot | — | FR-33 | BR-36 | — | — |
| `meal_type` | Bữa ăn: **3 loại cố định** Sáng / Trưa / Tối | Enum | VARCHAR | 10 | NOT NULL | — | — | — | UQ_MPE_unique_slot (3/4) | CK_MPE_meal_type ('BREAKFAST', 'LUNCH', 'DINNER') | UQ_MPE_unique_slot | — | FR-33 | BR-36 | — | — |
| `planned_servings` | Số khẩu phần dự kiến cho món trong bữa này | Decimal | DECIMAL | 5,1 | NOT NULL | — | — | — | — | CK_MPE_planned_servings (> 0) | — | Cơ sở cộng dồn dinh dưỡng theo ngày | FR-37 | BR-47 | — | — |
| `meal_week_start` 🆕 | Thứ Hai của tuần chứa `meal_date`, dùng cho FK kép ép `meal_date` nằm trong tuần của kế hoạch. **Technical design — chỉ Physical ERD (Q8)** | — (không thuộc Logical ERD) | DATE (computed, `PERSISTED`) | — | NOT NULL | `AS CONVERT(DATE, DATEADD(DAY, -(DATEDIFF(DAY, CONVERT(DATE, '19000101', 112), meal_date) % 7), meal_date)) PERSISTED` | — | FK_MPE_MEAL_PLAN (2/2) | — | — | — | — | FR-09 | BR-36 | — | — |

**Ràng buộc không vẽ được bằng đường nối:**

- Một món **không được trùng** trong cùng một bữa của cùng một ngày — duy nhất theo bộ `(meal_plan_id, meal_date, meal_type, recipe_id)` (BR-37).
- `meal_date` phải nằm trong 7 ngày từ `MEAL_PLAN.week_start_date`: database ép bằng `meal_week_start` + `FK_MPE_MEAL_PLAN` (Q8). Đã chạy thử trên SQL Server 2019: Chủ Nhật cùng tuần được chấp nhận; Chủ Nhật tuần trước và Thứ Hai tuần sau bị chặn; `ON DELETE CASCADE` vẫn hoạt động; đổi `week_start_date` của kế hoạch đã có mục bị chặn.
- Entity JPA phải khai báo `meal_week_start` là `insertable = false, updatable = false`. Computed column `PERSISTED` yêu cầu `QUOTED_IDENTIFIER ON` khi tạo và khi ghi dữ liệu. JDBC mặc định bật; `sqlcmd` cần cờ `-I`.

### 4.16 SHOPPING_LIST

**Mục đích:** Danh sách mua sắm nguyên liệu, tạo từ thực đơn, từ bài công thức hoặc lập thủ công.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `shopping_list_id` | Định danh danh sách | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_SHOPPING_LIST (Clustered) | PK | FR-53 | — | — | — |
| `user_id` | Chủ sở hữu | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_SHOPPING_LIST_USER → USER(user_id) [NO ACTION] | — | — | IX_SHOPPING_LIST_user (Nonclustered) | `USER` 1 → `SHOPPING_LIST` 0..* | FR-53 | BR-32 | — | Chỉ chủ sở hữu; không chia sẻ nhiều tài khoản |
| `name` | Tên danh sách | Text | NVARCHAR | 200 | NOT NULL | — | — | — | — | — | — | — | FR-53 | — | — | — |
| `created_at` | Thời điểm tạo | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_SHOPPING_LIST_created_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_SHOPPING_LIST_updated_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |

### 4.17 SHOPPING_LIST_ITEM

**Mục đích:** Từng mục nguyên liệu cần mua, kèm số lượng, đơn vị và trạng thái đã mua.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `shopping_list_item_id` | Định danh mục cần mua | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_SHOPPING_LIST_ITEM (Clustered) | PK | FR-53 | — | — | — |
| `shopping_list_id` | Danh sách chứa mục này | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_SLI_SHOPPING_LIST → SHOPPING_LIST(shopping_list_id) [CASCADE] | — | — | — | `SHOPPING_LIST` 1 → `SHOPPING_LIST_ITEM` 0..* | FR-53 | — | — | — |
| `ingredient_id` | Nguyên liệu chuẩn được tham chiếu | Identifier, FK, optional | BIGINT | — | NULL | — | — | FK_SLI_INGREDIENT → INGREDIENT(ingredient_id) [NO ACTION] | — | CK_SLI_target (ingredient_id IS NOT NULL OR custom_ingredient_name IS NOT NULL) | — | `INGREDIENT` 0..1 → 0..*; cho phép nhập tự do | FR-54 | BR-12 | — | — |
| `unit_id` | Đơn vị đo, **bắt buộc** | Identifier, FK | INT | — | NOT NULL | — | — | FK_SLI_UNIT → UNIT(unit_id) [NO ACTION] | — | — | — | `SHOPPING_LIST_ITEM` 0..* → `UNIT` **1** | FR-54 | BR-14, BR-73 | — | — |
| `custom_ingredient_name` | Tên nguyên liệu tự nhập, hoặc tên giữ lại từ dòng công thức nguồn; không được rỗng hoặc chỉ gồm khoảng trắng | Text, optional | NVARCHAR | 200 | NULL | — | — | — | — | CK_SLI_target; CK_SLI_custom_name_not_blank (cùng biểu thức `TRIM` với mục 4.7) | — | — | FR-53 | BR-12 | — | — |
| `quantity` | Số lượng cần mua, số thực dương | Decimal | DECIMAL | 10,2 | NOT NULL | — | — | — | — | CK_SLI_quantity (> 0) | — | — | FR-54 | BR-14 | — | — |
| `is_bought` | Đã mua hay chưa (tick checklist) | Boolean | BIT | — | NOT NULL | 0 (DF_SLI_is_bought) | — | — | — | — | — | — | FR-53 | — | — | — |

**Quy tắc gom an toàn:** chỉ gom các nguyên liệu **cùng chiều đo** (`MASS`, `VOLUME`, `COUNT`) hoặc gom được theo tỷ lệ trong `INGREDIENT_UNIT_CONVERSION` (BR-14, FR-54).

### 4.18 INGREDIENT

**Mục đích:** Từ điển nguyên liệu chuẩn kèm chín chỉ tiêu dinh dưỡng tham khảo trên 100 g.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `ingredient_id` | Định danh nguyên liệu | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_INGREDIENT (Clustered) | PK | FR-41 | — | — | — |
| `name` | Tên nguyên liệu chuẩn, duy nhất | Text, unique | NVARCHAR | 200 | NOT NULL | — | — | — | UQ_INGREDIENT_name | — | UQ_INGREDIENT_name (Nonclustered) | — | FR-41 | BR-51 | — | — |
| `energy_kcal_100g` | Chỉ tiêu 1 — Năng lượng — `NULL` nghĩa là chưa có dữ liệu, `0` là giá trị thật | Decimal, optional | DECIMAL | 10,2 | NULL | — | — | — | — | — | — | — | FR-39 | BR-52 | — | — |
| `protein_g_100g` | Chỉ tiêu 2 — Chất đạm — `NULL` nghĩa là chưa có dữ liệu, `0` là giá trị thật | Decimal, optional | DECIMAL | 10,2 | NULL | — | — | — | — | — | — | — | FR-39 | BR-52 | — | — |
| `carbohydrate_g_100g` | Chỉ tiêu 3 — Carbohydrate — `NULL` nghĩa là chưa có dữ liệu, `0` là giá trị thật | Decimal, optional | DECIMAL | 10,2 | NULL | — | — | — | — | — | — | — | FR-39 | BR-52 | — | — |
| `total_fat_g_100g` | Chỉ tiêu 4 — Chất béo — `NULL` nghĩa là chưa có dữ liệu, `0` là giá trị thật | Decimal, optional | DECIMAL | 10,2 | NULL | — | — | — | — | — | — | — | FR-39 | BR-52 | — | — |
| `fiber_g_100g` | Chỉ tiêu 5 — Chất xơ — `NULL` nghĩa là chưa có dữ liệu, `0` là giá trị thật | Decimal, optional | DECIMAL | 10,2 | NULL | — | — | — | — | — | — | — | FR-39 | BR-52 | — | — |
| `calcium_mg_100g` | Chỉ tiêu 6 — Canxi — `NULL` nghĩa là chưa có dữ liệu, `0` là giá trị thật | Decimal, optional | DECIMAL | 10,2 | NULL | — | — | — | — | — | — | — | FR-39 | BR-52 | — | — |
| `iron_mg_100g` | Chỉ tiêu 7 — Sắt — `NULL` nghĩa là chưa có dữ liệu, `0` là giá trị thật | Decimal, optional | DECIMAL | 10,2 | NULL | — | — | — | — | — | — | — | FR-39 | BR-52 | — | — |
| `vitamin_b12_mcg_100g` | Chỉ tiêu 8 — Vitamin B12 — `NULL` nghĩa là chưa có dữ liệu, `0` là giá trị thật | Decimal, optional | DECIMAL | 10,4 | NULL | — | — | — | — | — | — | — | FR-39 | BR-52 | — | — |
| `zinc_mg_100g` 🆕 | Chỉ tiêu 9 — Kẽm — `NULL` nghĩa là chưa có dữ liệu, `0` là giá trị thật | Decimal, optional | DECIMAL | 10,2 | NULL | — | — | — | — | — | — | — | FR-39 | BR-52 | — | — |
| ~~`sodium_mg_100g`~~ 🗑 | **Xóa** — natri không thuộc chín chỉ tiêu MVP | — | — | — | — | — | — | — | — | — | — | — | FR-39 | BR-44 | — | — |
| `source_name` | Nguồn số liệu, ví dụ USDA / NIH | Text | NVARCHAR | 200 | NOT NULL | — | — | — | — | — | — | — | FR-41 | BR-49 | — | — |
| `source_url` | Liên kết nguồn | Text, optional | VARCHAR | 2048 | NULL | — | — | — | — | — | — | — | FR-41 | BR-49 | — | — |
| `reference_date` | Ngày tham chiếu của số liệu | Date | DATE | — | NOT NULL | — | — | — | — | — | — | — | FR-41 | BR-49 | — | — |
| `nutrition_supported` | **Cổng kích hoạt (Q5):** chỉ được bật `1` khi đủ chín cột chỉ tiêu khác `NULL` **và** có đủ `source_name`, `source_url`, `reference_date`. `0` nghĩa là nguyên liệu chưa được hỗ trợ tính dinh dưỡng — khác với chỉ tiêu `NULL` (chưa có dữ liệu) | Boolean | BIT | — | NOT NULL | 0 (DF_INGREDIENT_nutrition_supported) | — | — | — | CK_INGREDIENT_nutrition_supported (nutrition_supported = 0 OR (9 cột chỉ tiêu IS NOT NULL AND source_url IS NOT NULL)) — `source_name`, `reference_date` đã `NOT NULL` | — | — | FR-40 | BR-50, BR-52 | — | — |
| `status` | Trạng thái hoạt động của mục từ điển | Enum | VARCHAR | 20 | NOT NULL | 'ACTIVE' (DF_INGREDIENT_status) | — | — | — | CK_INGREDIENT_status ('ACTIVE', 'INACTIVE') | — | — | FR-41 | BR-53 | — | — |
| `created_at` | Thời điểm tạo | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_INGREDIENT_created_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_INGREDIENT_updated_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |

**Ràng buộc:** **cấm xóa vĩnh viễn** nguyên liệu đã được tham chiếu (BR-53); cấm nhập hàng loạt tự động vào từ điển (BR-54).

### 4.19 UNIT

**Mục đích:** Từ điển đơn vị đo lường chuẩn hóa theo ba chiều `MASS`, `VOLUME`, `COUNT`.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `unit_id` | Định danh đơn vị | Identifier | INT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_UNIT (Clustered) | PK | FR-18 | BR-73 | — | — |
| `code` | Mã đơn vị, duy nhất — `g`, `kg`, `ml`, `L`, `quả`, `củ`, `bìa`… | Text, unique | VARCHAR | 20 | NOT NULL | — | — | — | UQ_UNIT_code | — | UQ_UNIT_code (Nonclustered) | — | FR-18 | BR-73 | — | — |
| `name` | Tên hiển thị | Text | NVARCHAR | 50 | NOT NULL | — | — | — | — | — | — | — | FR-18 | — | — | — |
| `dimension` | Chiều đo: `MASS` / `VOLUME` / `COUNT` | Enum | VARCHAR | 10 | NOT NULL | — | — | — | — | CK_UNIT_dimension ('MASS', 'VOLUME', 'COUNT') | — | Quyết định có gom an toàn được hay không | FR-18 | BR-14, BR-73 | — | — |
| `base_factor` | Hệ số quy về đơn vị cơ sở của cùng chiều | Decimal | DECIMAL | 18,6 | NOT NULL | — | — | — | — | — | — | — | FR-18 | BR-73 | — | — |
| `is_active` | Đơn vị còn dùng hay đã ngừng | Boolean | BIT | — | NOT NULL | 1 (DF_UNIT_is_active) | — | — | — | — | — | — | FR-18 | — | — | — |

Đây là **reference data bắt buộc** — pha 2 phải seed cùng migration, tách khỏi demo data.

### 4.20 INGREDIENT_UNIT_CONVERSION

**Mục đích:** Quy đổi đơn vị đặc thù (quả, củ, bìa, ml…) sang gram cho **từng nguyên liệu cụ thể**, phục vụ tính dinh dưỡng.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `ingredient_id` | Nguyên liệu áp dụng quy đổi | Identifier, FK | BIGINT | — | NOT NULL | — | PK (Composite 1/2) | FK_IUC_INGREDIENT → INGREDIENT(ingredient_id) [NO ACTION] | — | — | PK_INGREDIENT_UNIT_CONVERSION (Clustered) | `INGREDIENT_UNIT_CONVERSION` 0..* → `INGREDIENT` **1** | FR-18 | BR-73 | — | — |
| `unit_id` | Đơn vị nguồn cần quy đổi | Identifier, FK | INT | — | NOT NULL | — | PK (Composite 2/2) | FK_IUC_UNIT → UNIT(unit_id) [NO ACTION] | — | — | — | `INGREDIENT_UNIT_CONVERSION` 0..* → `UNIT` **1** | FR-18 | BR-73 | — | — |
| `grams_per_unit` | Số gram tương ứng một đơn vị | Decimal | DECIMAL | 10,2 | NOT NULL | — | — | — | — | CK_IUC_grams_per_unit (> 0) | — | — | FR-18 | BR-73 | — | — |
| `is_approximate` | Tỷ lệ là ước lượng hay chính xác | Boolean | BIT | — | NOT NULL | 0 (DF_IUC_is_approximate) | — | — | — | — | — | — | FR-18 | BR-48 | — | — |
| `is_active` | Quy tắc còn hiệu lực hay không | Boolean | BIT | — | NOT NULL | 1 (DF_IUC_is_active) | — | — | — | — | — | — | FR-18 | — | — | — |

**Cổng kiểm định xuất bản:** thiếu tỷ lệ quy đổi cho tổ hợp nguyên liệu + đơn vị đang dùng thì **chặn công khai** bài công thức (BR-48, BR-73).

### 4.21 SUBSCRIPTION

**Mục đích:** Gói trả phí PLUS / PRO, thời hạn hiệu lực và phân tầng tính năng AI. **Không lưu FREE (Q9):** user không có gói `ACTIVE` còn hạn thì entitlement mặc định là FREE.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `subscription_id` | Định danh gói | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_SUBSCRIPTION (Clustered) | PK | FR-13 | — | — | — |
| `user_id` | Chủ sở hữu gói | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_SUBSCRIPTION_USER → USER(user_id) [NO ACTION] | UQ_SUBSCRIPTION_active (Filtered) | — | IX_SUBSCRIPTION_user (user_id, status); UQ_SUBSCRIPTION_active (Nonclustered, (user_id) WHERE status = 'ACTIVE') | `USER` 1 → `SUBSCRIPTION` 0..* (lịch sử các gói trả phí); tối đa 1 gói `ACTIVE` (Q10) | FR-13 | BR-02 | UC-13.3 | — |
| `tier` | `PLUS` / `PRO` — không lưu FREE (Q9) | Enum | VARCHAR | 10 | NOT NULL | — | — | — | — | CK_SUBSCRIPTION_tier ('PLUS', 'PRO') — bỏ `FREE` | — | Quyết định quyền dùng tính năng AI | FR-10, FR-13 | BR-01, BR-02, BR-03 | — | — |
| `status` | Trạng thái hiệu lực; mỗi user tối đa **một** dòng `ACTIVE` (Q10) | Enum | VARCHAR | 20 | NOT NULL | 'ACTIVE' (DF_SUBSCRIPTION_status) | — | — | — | CK_SUBSCRIPTION_status ('ACTIVE', 'EXPIRED', 'CANCELLED') | IX_SUBSCRIPTION_user | — | FR-13 | BR-03 | — | — |
| `starts_at` | Thời điểm bắt đầu hiệu lực | Timestamp | DATETIME2 | 7 | NOT NULL | — | — | — | — | CK_SUBSCRIPTION_period | — | Chỉ kích hoạt **sau khi thanh toán được xác minh** | FR-13 | BR-03 | — | — |
| `ends_at` | Thời điểm hết hạn cuối kỳ đã trả | Timestamp | DATETIME2 | 7 | NOT NULL | — | — | — | — | CK_SUBSCRIPTION_period (ends_at > starts_at) | — | Không tự động gia hạn, không hoàn tiền một phần | FR-13 | BR-03 | — | — |
| `created_at` | Thời điểm tạo | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_SUBSCRIPTION_created_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |
| `updated_at` | Thời điểm cập nhật gần nhất | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_SUBSCRIPTION_updated_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |

**Ràng buộc (Q9, Q10):**

- Database: `CK_SUBSCRIPTION_period` và `UQ_SUBSCRIPTION_active`.
- Service: nâng cấp PLUS → PRO chuyển gói cũ sang `CANCELLED` và tạo gói mới `ACTIVE` trong **cùng transaction**, không hoàn tiền phần còn lại. Trước khi tạo gói mới, service phải chuyển gói đã quá `ends_at` nhưng còn `ACTIVE` sang `EXPIRED`, nếu không `UQ_SUBSCRIPTION_active` sẽ chặn. MVP không hỗ trợ gia hạn sớm, hạ hạng hoặc nhiều gói chồng lấn.

### 4.22 PAYMENT_TRANSACTION

**Mục đích:** Giao dịch thanh toán qua payOS để kích hoạt gói dịch vụ.

| Column | Business meaning | Logical type | SQL Server type | Length/Precision | Nullable | Default | PK | FK reference | UNIQUE | CHECK | Index | Relationship / Cardinality | FR | BR | UC/AC | Security / Privacy |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `payment_transaction_id` | Định danh giao dịch | Identifier | BIGINT | — | NOT NULL | — | PK (Identity 1,1) | — | — | — | PK_PAYMENT_TRANSACTION (Clustered) | PK | FR-13 | — | — | — |
| `user_id` | Người thanh toán | Identifier, FK | BIGINT | — | NOT NULL | — | — | FK_PAYMENT_USER → USER(user_id) [NO ACTION] | — | — | — | `USER` 1 → `PAYMENT_TRANSACTION` 0..* | FR-13 | — | — | — |
| `subscription_id` | Gói được kích hoạt bởi giao dịch này | Identifier, FK, optional | BIGINT | — | NULL | — | — | FK_PAYMENT_SUBSCRIPTION → SUBSCRIPTION(subscription_id) [NO ACTION] | — | — | — | `PAYMENT_TRANSACTION` 0..1 → `SUBSCRIPTION` 0..1 | FR-13 | BR-03 | — | — |
| `order_code` | Mã đơn hàng payOS, **duy nhất** | Text, unique | VARCHAR | 100 | NOT NULL | — | — | — | UQ_PAYMENT_order_code | — | UQ_PAYMENT_order_code (Nonclustered) | — | FR-13 | — | — | **Cơ chế idempotent cho webhook** — webhook gửi lại nhiều lần chỉ kích hoạt gói đúng một lần |
| `amount_vnd` | Số tiền: PLUS 49.000, PRO 99.000 VNĐ/tháng. FREE không phát sinh giao dịch (Q9) | Integer | INT | — | NOT NULL | — | — | — | — | CK_PAYMENT_amount (> 0) | — | — | FR-13 | — | — | — |
| `status` | Trạng thái giao dịch | Enum | VARCHAR | 20 | NOT NULL | 'PENDING' (DF_PAYMENT_status) | — | — | — | CK_PAYMENT_status ('PENDING', 'PAID', 'FAILED', 'CANCELLED') | — | — | FR-13 | — | — | — |
| `created_at` | Thời điểm tạo giao dịch | Timestamp | DATETIME2 | 7 | NOT NULL | SYSUTCDATETIME() (DF_PAYMENT_created_at) | — | — | — | — | — | — | Technical design — audit | — | — | — |
| `paid_at` | Thời điểm thanh toán thành công | Timestamp, optional | DATETIME2 | 7 | NULL | — | — | — | — | — | — | — | FR-13 | — | — | **Không lưu số thẻ**, NFR-21 (PCI-DSS) |

## 5. Ràng buộc không biểu diễn được bằng đường nối ERD

Issue #63 mục D yêu cầu ghi riêng các ràng buộc loại này. Trương Văn Khải chuyển chúng thành constraint hoặc ghi rõ là ràng buộc tầng service trong pha 2.

| # | Ràng buộc | Bảng | Ép được ở database? | Nguồn |
|---|---|---|---|---|
| 1 | `REPORT` trỏ tới **hoặc** `recipe_id` **hoặc** `comment_id`, không cả hai, không rỗng cả hai (XOR) | `REPORT` | Có — `CHECK` đếm cột khác `NULL` bằng 1 | BR-23 |
| 2 | **Không quá 1** ảnh `is_cover = true` mỗi bài | `RECIPE_MEDIA` | Có — filtered unique index `UQ_RECIPE_MEDIA_cover` | BR-20 |
| 2b | **Bắt buộc có đúng 1 cover khi bài có ảnh** | `RECIPE_MEDIA` | Không — filtered unique index chỉ chặn nhiều hơn một, không ép phải có. Tầng service kiểm tra khi publish | BR-20 |
| 3 | Mỗi bài công thức có **tối đa 5** ảnh | `RECIPE_MEDIA` | Có — `UQ_RECIPE_MEDIA_order` + `CK_RECIPE_MEDIA_display_order` (1..5). Đã triển khai ở V1 | BR-20, FR-14 |
| 4 | Bình luận lồng **tối đa 5 cấp**; `depth` = `depth` cha + 1; gốc khi và chỉ khi `parent_comment_id IS NULL` | `COMMENT` | Có — `CK_COMMENT_depth` + `CK_COMMENT_root_reply` + `FK_COMMENT_PARENT` gồm `parent_depth` (Q8) | BR-66 |
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
| 19 | `nutrition_supported = 1` chỉ khi đủ 9 chỉ tiêu khác `NULL` và đủ 3 trường nguồn | `INGREDIENT` | Có — `CK_INGREDIENT_nutrition_supported` | Q5, BR-52 |
| 20 | **Tổng** `prep_time_min + cook_time_min` phải **> 0** (mỗi vế có thể bằng 0) | `RECIPE_POST` | Có — `CK_RECIPE_POST_total_time` | FR-16, BR-19 |
| 21 | `instructions` dài **10–5.000** ký tự (V1 hiện chỉ chặn tối thiểu 10) | `RECIPE_POST` | Có — `CK_RECIPE_POST_instructions_len` | FR-16, AC-16.5 |
| 22 | `MEAL_PLAN_ENTRY.meal_date` phải nằm trong 7 ngày từ `MEAL_PLAN.week_start_date` | `MEAL_PLAN_ENTRY` | Có — computed column `meal_week_start` + `FK_MPE_MEAL_PLAN` kép (Q8) | FR-09, BR-36 |
| 23 | Reply thuộc cùng `recipe_id` với bình luận cha | `COMMENT` | Có — `FK_COMMENT_PARENT` gồm `recipe_id` | BR-66 |
| 24 | Mỗi user tối đa **một** kế hoạch cho mỗi tuần; `week_start_date` là **Thứ Hai** | `MEAL_PLAN` | Có — `UQ_MEAL_PLAN_user_week` + `CK_MEAL_PLAN_week_start_monday` | FR-09, Q3 |
| 25 | Dòng nguyên liệu phải có `ingredient_id` hoặc `custom_ingredient_name` (hoặc cả hai); tên tự nhập không rỗng, không chỉ gồm khoảng trắng | `RECIPE_INGREDIENT`, `SHOPPING_LIST_ITEM`, `USER_INGREDIENT_PREFERENCE` | Có — `CK_*_target` + `CK_*_custom_name_not_blank` | SRS 3.4, BR-12 |
| 26 | Mỗi nguyên liệu chỉ có **một** loại hiệu lực trên một user | `USER_INGREDIENT_PREFERENCE` | Có — `UQ_UIP_user_ingredient` + `UQ_UIP_user_custom_name`. Service trim tên và cập nhật `preference_type` khi đổi danh sách | Q11, FR-31 |
| 27 | `ends_at > starts_at`; mỗi user tối đa **một** gói `ACTIVE` | `SUBSCRIPTION` | Có — `CK_SUBSCRIPTION_period` + `UQ_SUBSCRIPTION_active` | Q10, FR-13 |
| 28 | Nâng cấp PLUS → PRO: gói cũ `CANCELLED`, gói mới `ACTIVE` trong cùng transaction; đánh dấu `EXPIRED` trước khi tạo gói mới | `SUBSCRIPTION` | Không — tầng service | Q10, FR-13 AF-13.2 |
| 29 | Tuổi tính từ `date_of_birth` nằm trong **18–120** khi lưu hồ sơ dinh dưỡng | `USER` | Không — tầng service. `CHECK` phụ thuộc thời gian chỉ chạy khi cột được ghi, nên không phải bất biến. Database chỉ chặn ngày trong tương lai hoặc trước 1900 | Q7, FR-35, BR-42 |
| 30 | Cổng AI cá nhân hóa: có `vegetarian_type`; có dòng `AVOID` **hoặc** `avoid_none_confirmed = 1`; có dòng `DISLIKE` **hoặc** `dislike_none_confirmed = 1`. Khi thêm dòng `AVOID`/`DISLIKE`, service đưa cờ "Không có" tương ứng về `0` | `USER`, `USER_INGREDIENT_PREFERENCE` | Không — tầng service (liên bảng) | Q12, FR-31, BR-31 |
| 31 | `content` bình luận dài **1–1.000** ký tự | `COMMENT` | Có — `CK_COMMENT_content_len` (1..1000) | FR-46, EF-46.1 |
| 32 | `amount_vnd` giao dịch thanh toán phải **> 0** (FREE không phát sinh giao dịch) | `PAYMENT_TRANSACTION` | Có — `CK_PAYMENT_amount` (> 0) | Q9, FR-13 |

## 6. Cảnh báo cho pha 2 — multiple cascade paths trên SQL Server

SQL Server từ chối tạo FK khi có nhiều đường cascade dẫn tới cùng một bảng (lỗi 1785). Mô hình này dính ở ít nhất ba chỗ:

- `COMMENT` tham chiếu cả `USER` lẫn `RECIPE_POST`, mà `RECIPE_POST` lại tham chiếu `USER`.
- `USER_FOLLOW` có **hai** FK cùng trỏ về `USER`.
- `REPORT` tham chiếu `USER` (reporter), `RECIPE_POST` và `COMMENT`.

Chỉ nên cascade ở quan hệ cha–con thật sự sở hữu, ví dụ `SHOPPING_LIST` → `SHOPPING_LIST_ITEM` và `MEAL_PLAN` → `MEAL_PLAN_ENTRY`. Các FK còn lại dùng `ON DELETE NO ACTION` và xử lý ở tầng service. Quyết định cho **từng** FK phải được ghi vào cột `FK reference` khi pha 2 hoàn thiện.

> [!NOTE]
> **Kết quả thực hiện Pha 2 (23/09/2026):**  
> Trương Văn Khải đã áp dụng triệt để chính sách trên vào `V1__baseline_schema.sql` và `database/schema.sql`:  
> - **CASCADE (4 quan hệ sở hữu thực sự):** `RECIPE_POST → RECIPE_MEDIA`, `RECIPE_POST → RECIPE_INGREDIENT`, `MEAL_PLAN → MEAL_PLAN_ENTRY`, `SHOPPING_LIST → SHOPPING_LIST_ITEM`.  
> - **SET NULL (1 quan hệ hỗ trợ Guest):** `RECIPE_VIEW.user_id → USER(user_id)`.  
> - **NO ACTION (33 quan hệ còn lại):** Toàn bộ các FK còn lại đều dùng `ON DELETE NO ACTION`, giải quyết triệt để lỗi SQL Server Error 1785.  
> Đã kiểm thử thực thi script thành công trên Microsoft SQL Server 2019 thật (`.\SQLEXPRESS`).

## 7. Việc còn lại trước khi chốt pha 1

- [x] ~~Trả lời câu hỏi chặn với Tech Lead~~ — `Q1` và `Q2` đã chốt ngày 23/09/2026, ghi tại mục 2.
- [x] ~~Dựng `logical-erd-v1.0.0.drawio` theo delta ở mục 3~~ — đã dựng ngày 23/09/2026: 22 bảng, XML đã kiểm tra hợp lệ.
- [x] ~~Export lại PNG cho `logical-erd-v1.0.0`~~ — đã xuất lại bằng draw.io ngày 24/09/2026 sau khi cập nhật sơ đồ (commit `827353e`).
- [x] ~~Đối chiếu hai chiều~~ — 24/09/2026: 22/22 bảng, 194/194 cột logical khớp giữa mục 4 và sơ đồ. Hai cột `chỉ Physical ERD` (`COMMENT.parent_depth`, `MEAL_PLAN_ENTRY.meal_week_start`) không vẽ trên Logical ERD theo `Q8`.
- [x] ~~Kiểm tra chuẩn hóa 3NF~~ — ngoại lệ có chủ đích đã ghi tại mục 4.5 sau quyết định `Q2`.
- [x] ~~Rà đủ 36 quan hệ trong ma trận của ERD README~~ — 24/09/2026:
  - Cả 36 quan hệ đều có trong cột `Relationship / Cardinality` ở trên, đúng bản số hai đầu.
  - Trên sơ đồ, cả 36 quan hệ khớp cặp bảng, nhãn và bản số. Logical ERD có **37 connector** vì quan hệ #36 tách hai vai trò `follower` / `followed` như ERD README yêu cầu.
  - Đã sửa trong lần rà này: gắn lại 9 connector có đầu nối không bám vào bảng (#3, #6, #8, #11, #15, #16, #19, #23, #31); vẽ bổ sung quan hệ #17 `RECIPE_POST` → `SAVED_RECIPE` vốn bị thiếu; đổi nhãn #11 thành `reacts` và #15 thành `receives_reactions` cho khớp ma trận.
- [x] ~~Cập nhật theo quyết định `Q7`–`Q12`~~ — 24/09/2026: thêm 5 cột vào `USER` trên sơ đồ và mục 4.1; cập nhật mục 4.3, 4.5–4.7, 4.10, 4.14–4.15, 4.17–4.18, 4.21–4.22 và mục 5.
- [x] ~~Xóa bản nháp `logical-erd-v0.1.0.drawio` và ảnh PNG~~ — 24/09/2026, theo yêu cầu Tech Lead trên PR #66.

## 8. Tiến độ thực hiện Pha 2 (Trương Văn Khải)

- [x] Viết kịch bản Flyway baseline migration: [V1__baseline_schema.sql](../../../app/mamxanh-backend/src/main/resources/db/migration/V1__baseline_schema.sql) (22 bảng, 196 cột, 38 FK, 57 CHECK, 55 DEFAULT, 55 index gồm 22 PK + 9 UNIQUE constraint + 24 index tạo riêng; trong đó 10 filtered index: 8 unique, 2 không unique; seed 15 dòng `UNIT`). Số liệu đếm bằng `sys.*` sau khi chạy `database/schema.sql` trên SQL Server 2019 ngày 25/09/2026.
- [x] Tạo snapshot bootstrap độc lập: [database/schema.sql](../../../database/schema.sql) (chuẩn T-SQL cho SSMS và `sqlcmd`, DDL khớp 100% với V1).
- [x] Khởi tạo và chạy thử trên database sạch Microsoft SQL Server 2019 thật (`.\SQLEXPRESS`): thành công 100%, 0 lỗi cú pháp hoặc cascade path.
- [x] Điền đầy đủ 9 cột Physical vào Bảng tổng hợp mục 4 của Data Dictionary này, cập nhật trạng thái sau review PR #66.
- [x] Dựng sơ đồ Physical ERD: [physical-erd-v1.0.0.drawio](./physical-erd-v1.0.0.drawio) (22 bảng, 37 connector, kiểu dữ liệu vật lý T-SQL SQL Server 2019; export PNG [physical-erd-v1.0.0.drawio.png](./physical-erd-v1.0.0.drawio.png) đã kiểm tra trực quan).
- [x] Soạn bộ kịch bản kiểm thử ràng buộc toàn vẹn dữ liệu và chẩn đoán: [database/queries.sql](../../../database/queries.sql) (35 automated test cases TC01–TC35, 66/66 test assertions đạt `PASS` 100% trên Microsoft SQL Server 2019 thật).

### 8.1 Kết quả thực hiện sau review PR #66 (hoàn thành 25/09/2026)

Toàn bộ đặc tả chi tiết đánh dấu `⏳` trước đây trong mục 4 và mục 5 đã được triển khai và kiểm thử hoàn tất:

- [x] `V1__baseline_schema.sql` và `database/schema.sql`: triển khai toàn bộ 33 hạng mục constraint, index, cột và giá trị mặc định theo yêu cầu review:
  - Vòng 1: `RECIPE_POST.status`; 9 cột dinh dưỡng `NULL`; `CK_INGREDIENT_nutrition_supported`; tổng thời gian > 0; `instructions` 10–5.000 ký tự; giới hạn tối đa 5 ảnh (`CK_RECIPE_MEDIA_display_order` + `UQ_RECIPE_MEDIA_order`); `meal_date` trong tuần (`meal_week_start` + `FK_MPE_MEAL_PLAN`).
  - Vòng 2: `Q7`–`Q12` (5 cột mới của `USER`, `onboarding_status`, `avoid_none_confirmed`, `dislike_none_confirmed`, `nutrition_goal`, `date_of_birth`), `[2]` (`CK_SUBSCRIPTION_tier`, `CK_SUBSCRIPTION_period`, `UQ_SUBSCRIPTION_active`), `[3]` (`UQ_UIP_user_ingredient`, `UQ_UIP_user_custom_name`, `CK_UIP_preference_type`), `[4]` (`CK_*_target`, `CK_*_custom_name_not_blank`), `[5]` (`FK_COMMENT_PARENT`, `CK_COMMENT_root_reply`), `[6]` (`CK_MEAL_PLAN_week_start_monday`, `UQ_MEAL_PLAN_user_week`), `[7]` (`CK_RECIPE_POST_status`, `DF_RECIPE_POST_status`).
- [x] Physical ERD:
  - Thể hiện đầy đủ `PK, ID`, `DEFAULT`, `NULL`/`NOT NULL`, `CHECK`, index và `ON DELETE` (`CASCADE`, `NO ACTION`, `SET NULL`) cho 196 cột physical (vòng 1 điểm 1).
  - Bổ sung 5 cột mới của `USER`, `COMMENT.parent_depth`, `MEAL_PLAN_ENTRY.meal_week_start`.
  - Kế thừa cấu trúc connector từ Logical ERD v1.0.0: 37 connector, không có đầu nối thả nổi ngoài bảng (fixed 9 floating ends), có đầy đủ quan hệ #17 `saved_recipe`.
  - Export PNG thành công, căn chỉnh container layout chuẩn xác.
- [x] `database/queries.sql`: nâng cấp inventory và bổ sung 20 test case mới (TC16–TC35) kiểm thử từng constraint mới, assert đúng tên constraint trong `ERROR_MESSAGE()`. 66/66 test assertions PASS.
- [x] Đồng bộ số liệu và cập nhật [database/README.md](../../../database/README.md).
- [x] Cập nhật `CHANGELOG.md` và chuẩn bị ma trận truy vết cho mô tả PR #66.
