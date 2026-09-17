> **Document:** ERD Workspace Guide  
> **File:** `docs/diagrams/ERD/README.md`  
> **Version:** v1.2.0  
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-17  
> **Status:** Active  
> **Related Docs:** `docs/architecture/ARCHITECTURE.md`, `docs/requirements/SRS.md`, `database/README.md`

# ERD Workspace Guide — Hướng Dẫn Sơ Đồ Quan Hệ Thực Thể

## 1. Mục Đích và Tác Dụng của Sơ Đồ ERD

`ERD` (Entity Relationship Diagram — Sơ đồ quan hệ thực thể) trong thư mục này là mô hình dữ liệu cấp cao ở mức **Khái niệm (Conceptual Level)** của hệ thống **Mâm Xanh (Vegetarian Support System)**. 

Trong quy trình phát triển phần mềm của dự án, sơ đồ này có các tác dụng cốt lõi sau:

1. **Định hình bức tranh tổng thể về dữ liệu nghiệp vụ:**
   * Giúp toàn bộ 5 thành viên trong nhóm phát triển, giảng viên và các bên liên quan có cùng một góc nhìn thống nhất về các đối tượng dữ liệu mà hệ thống cần quản trị và vận hành.
2. **Xác lập và bảo vệ ranh giới phạm vi dữ liệu MVP:**
   * Khóa chặt phạm vi mô hình dữ liệu ở đúng **17 thực thể cốt lõi** đã được phê duyệt.
   * Ngăn ngừa tình trạng phình to phạm vi (scope creep) hoặc việc các thành viên tự tiện phát sinh bảng mới ngoài các quyết định kiến trúc đã chốt.
3. **Làm cầu nối giữa Yêu cầu nghiệp vụ (SRS) và Thiết kế kỹ thuật (Database Design):**
   * Chuyển hóa các yêu cầu chức năng (FR) và quy tắc nghiệp vụ (BR) từ tài liệu đặc tả thành các khái niệm thực thể dữ liệu rõ ràng trước khi bước vào lập trình chi tiết.
   * Là cơ sở định hướng để xây dựng mô hình dữ liệu vật lý (Physical Data Model), thiết kế khóa chính (PK), khóa ngoại (FK), các chỉ mục (Indexes) và ràng buộc toàn vẹn dữ liệu.
4. **Định hướng triển khai mã nguồn Backend và Migration:**
   * Làm kim chỉ nam cho các thành viên phụ trách Backend khi thiết kế các lớp Entity trong Java/Spring Data JPA và viết các kịch bản chuyển đổi dữ liệu (Flyway migration scripts).
   * Là căn cứ đối chiếu để duy trì ảnh chụp lược đồ cơ sở dữ liệu trong [database/schema.sql](../../../database/schema.sql).
5. **Giảm thiểu rủi ro thiết kế cơ sở dữ liệu:**
   * Giúp phát hiện sớm các điểm thiếu sót về cấu trúc thông tin ngay từ giai đoạn thiết kế kiến trúc, hạn chế tối đa việc phải tái cấu trúc cơ sở dữ liệu lớn khi hệ thống đã đi vào giai đoạn viết code.

---

## 2. Hình Ảnh và Tệp Nguồn Sơ Đồ

Dưới đây là sơ đồ quan hệ thực thể mức khái niệm của hệ thống Mâm Xanh:

![Conceptual ERD - Mâm Xanh](./conceptual-erd-v1.0.0.drawio.png)

* File nguồn Draw.io có thể mở và chỉnh sửa trực tiếp: [conceptual-erd-v1.0.0.drawio](./conceptual-erd-v1.0.0.drawio)
* Để xem trực quan cấu trúc sơ đồ, các thành viên có thể mở file ảnh PNG trên hoặc mở file `.drawio` bằng công cụ [Draw.io](https://app.diagrams.net/) hoặc extension Draw.io trên VS Code / IDE.

---

## 3. Ranh Giới 17 Thực Thể Khái Niệm Cốt Lõi (Conceptual Baseline)

Căn cứ theo quyết định kiến trúc ngày 17/09/2026, mô hình dữ liệu của MVP bao gồm chính xác **17 thực thể cốt lõi**:

| # | Thực thể (Entity) | Vai trò khái niệm trong hệ thống |
|---|---|---|
| 1 | **User** | Quản lý thông tin tài khoản, hồ sơ cá nhân và chỉ số dinh dưỡng/thể trạng (đã gộp từ `User Profile`). |
| 2 | **User Ingredient Preference** | Lưu trữ sở thích, kiêng kỵ và dị ứng nguyên liệu của người dùng (`ALLERGY`, `AVOID`, `DISLIKE`). |
| 3 | **Recipe Post** | Bài viết công thức nấu ăn chay (chứa thông tin cấu trúc, trường nội dung tự do `instructions`, tối đa 1 ảnh bìa và 0..1 link YouTube). |
| 4 | **Category** | Danh mục bài viết / món ăn do Administrator quản trị. |
| 5 | **Recipe Category** | Thực thể liên kết nhiều–nhiều (N:M) giữa bài công thức và danh mục. |
| 6 | **Ingredient** | Từ điển nguyên liệu chuẩn (tên, calo và dinh dưỡng tham khảo). |
| 7 | **Recipe Ingredient** | Nguyên liệu cụ thể trong một bài công thức kèm theo định lượng và đơn vị. |
| 8 | **Saved Recipe** | Thực thể đánh dấu lưu lại các bài công thức yêu thích của Member (Bookmark). |
| 9 | **Comment** | Bình luận và phản hồi trên bài viết công thức. |
| 10 | **Report** | Báo cáo vi phạm nội dung từ người dùng, tích hợp trực tiếp kết quả và lý do xử lý của Admin (đã gộp từ `Moderation Action`). |
| 11 | **Meal Plan** | Kế hoạch thực đơn bữa ăn theo tuần (7 ngày) của người dùng. |
| 12 | **Meal Plan Entry** | Món ăn cụ thể được phân bổ vào từng ngày và từng bữa (Sáng, Trưa, Tối). |
| 13 | **Shopping List** | Danh sách mua sắm nguyên liệu (tạo từ thực đơn, bài công thức hoặc lập thủ công). |
| 14 | **Shopping List Item** | Từng mục nguyên liệu cần mua kèm số lượng, đơn vị và trạng thái đã mua (`is_bought`). |
| 15 | **Subscription** | Quản lý gói dịch vụ hội viên (FREE, PLUS, PRO), thời hạn hiệu lực và phân tầng tính năng AI. |
| 16 | **Payment Transaction** | Lịch sử giao dịch thanh toán trực tuyến qua payOS để kích hoạt gói dịch vụ. |
| 17 | **Notification** | Thông báo trong ứng dụng gửi tới người dùng. |

### Các thành phần đã loại bỏ hoặc sáp nhập để tối ưu hóa phạm vi MVP:
* ❌ **`recipe_step`**: Loại bỏ thực thể riêng; hướng dẫn nấu ăn lưu ở trường văn bản tự do `instructions` trên `Recipe Post`.
* ❌ **`recipe_media`**: Loại bỏ thực thể riêng; lưu trực tiếp tối đa 1 ảnh đại diện (`cover_image_url`) và 0..1 link YouTube (`youtube_url`) trên `Recipe Post`.
* ❌ **`recipe_like` & `comment_like`**: Loại bỏ hoàn toàn tương tác Thích/Bỏ thích trên toàn hệ thống.
* ❌ **`moderation_action`**: Loại bỏ bảng riêng; kết quả và lý do kiểm duyệt lưu trực tiếp trên `Report`.
* ❌ **`user_profile`**: Gộp trực tiếp vào thực thể `User`.
* ❌ **`ai_usage_record`**: Loại bỏ bảng đếm lượt; phân quyền AI theo gói `Subscription` và đo lường kỹ thuật qua log hạ tầng.

---

## 4. Mối Liên Hệ Với Triển Khai Cơ Sở Dữ Liệu (Database Implementation)

Sơ đồ ERD trong thư mục này dừng ở mức **Khái niệm (Conceptual)** để xác lập thực thể và ranh giới nghiệp vụ. Khi chuyển sang giai đoạn phát triển:

1. **Thiết kế Lược đồ Chi tiết (Physical Schema):**
   * Các bảng vật lý, kiểu dữ liệu cụ thể (`BIGINT`, `NVARCHAR`, `VARCHAR`, `DATETIME2`), chỉ mục (Indexes) và ràng buộc khóa ngoại (Foreign Keys) sẽ được đặc tả chi tiết trong tài liệu thiết kế cơ sở dữ liệu và hiện thực hóa trong [database/schema.sql](../../../database/schema.sql).
2. **Quản lý Phiên bản Cơ sở dữ liệu bằng Flyway:**
   * Mọi thay đổi cấu trúc bảng trong quá trình lập trình Backend phải được tạo thành các file migration tuần tự trong thư mục `app/mamxanh-backend/src/main/resources/db/migration/` (theo chuẩn `V1__...`, `V2__...`).
   * Tuyệt đối không chỉnh sửa trực tiếp database trên môi trường triển khai mà không qua migration scripts.

---

## 5. Quy Ước Đặt Tên và Quản Lý Tệp

* **Vị trí thư mục:** `docs/diagrams/ERD/`
* **File sơ đồ Draw.io gốc:** `conceptual-erd-v[version].drawio` (Ví dụ: `conceptual-erd-v1.0.0.drawio`)
* **File hình ảnh xuất ra:** `conceptual-erd-v[version].drawio.png` (Ví dụ: `conceptual-erd-v1.0.0.drawio.png`)
* **Quy trình cập nhật:** Khi có điều chỉnh về danh mục thực thể theo quyết định kiến trúc mới, cần cập nhật file Draw.io, xuất lại ảnh PNG tương ứng, và cập nhật số phiên bản trong file `README.md` này.
