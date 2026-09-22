> **Document:** C4 Container Diagram Workspace Guide  
> **File:** `docs/diagrams/C4 Container Diagram/README.md`  
> **Version:** v1.2.0  
> **Created:** 2026-09-17  
> **Last Updated:** 2026-09-22  
> **Status:** Draft (Chờ cập nhật bản vẽ sơ đồ C4)  
> **Related Docs:** `docs/architecture/ARCHITECTURE.md`, `docs/architecture/TECHNOLOGY-STACK.md`, `docs/diagrams/ERD/README.md`

# C4 Container Diagram — Sơ Đồ Container Hệ Thống Mâm Xanh

> [!WARNING]
> **TÀI LIỆU ĐANG Ở TRẠNG THÁI NHÁP (DRAFT) — CHỜ CẬP NHẬT BẢN VẼ SƠ ĐỒ C4:**  
> Tài liệu diễn giải và sơ đồ kiến trúc trong file này đang chờ vẽ lại và cập nhật **sơ đồ C4 Container** mới theo định vị hệ thống đã thay đổi (sân chơi Chuyên gia ẩm thực chay với baseline 21 thực thể cốt lõi, loại bỏ quản lý danh mục động `CATEGORY` / `RECIPE_CATEGORY` và loại bỏ chế độ nấu ăn từng bước `RECIPE_STEP`).  
> Do đó, tài liệu này được giữ ở trạng thái **Nháp (Draft)** cho đến khi bản vẽ sơ đồ C4 Container thực tế được hoàn thiện và bàn giao chính thức.

## 1. Mục Đích và Ý Nghĩa của Sơ Đồ C4 Container

Mô hình C4 (Context, Containers, Components, Code) là một tiêu chuẩn kiến trúc phần mềm trực quan giúp diễn giải hệ thống ở nhiều cấp độ trừu tượng khác nhau. Trong đó, **C4 Container Diagram (Mức 2)** thể hiện bức tranh kiến trúc cấp cao ở mức thực thi (runtime execution):

* **"Container" trong mô hình C4:** Không đồng nhất với khái niệm Docker container hay Kubernetes pod. Container ở đây đại diện cho bất kỳ một **đơn vị thực thi độc lập (executable/deployable unit)** hoặc kho lưu trữ dữ liệu nào cấu thành nên giải pháp phần mềm (ví dụ: một ứng dụng web Single-Page Application, một dịch vụ API backend, một hệ quản trị cơ sở dữ liệu quan hệ, hoặc một dịch vụ lưu trữ đối tượng dạng Blob).
* **Mục tiêu của sơ đồ đối với dự án Mâm Xanh:**
  1. Phân rã rõ ràng ranh giới trách nhiệm giữa các khối kiến trúc: giao diện người dùng (Frontend), khối xử lý logic nghiệp vụ và xác thực (Backend), kho lưu trữ dữ liệu quan hệ (Database) và kho lưu trữ tệp nhị phân tĩnh (Blob Storage).
  2. Xác định các công nghệ cốt lõi được lựa chọn cho từng container (React/TypeScript, Java/Spring Boot, SQL Server, Azure Blob Storage).
  3. Minh định các luồng giao tiếp, giao thức mạng (HTTPS, REST/JSON, JDBC) và ranh giới tin cậy (Trust Boundaries) giữa client và server.
  4. Xác định các điểm tích hợp với các dịch vụ bên ngoài (Google Gemini, YouTube, Google Identity Services).

Tài liệu này đóng vai trò là bản diễn giải chính thức cho file sơ đồ kiến trúc [C4 Container Diagram – Vegetarian Support System.drawio](./C4%20Container%20Diagram%20%E2%80%93%20Vegetarian%20Support%20System.drawio) và ảnh xuất trực quan [C4 Container Diagram – Vegetarian Support System.drawio.png](./C4%20Container%20Diagram%20%E2%80%93%20Vegetarian%20Support%20System.drawio.png).

---

## 2. Hình Ảnh Sơ Đồ Trực Quan

Dưới đây là sơ đồ kiến trúc C4 Container của hệ thống **Mâm Xanh (Vegetarian Support System)**:

![Sơ đồ C4 Container - Mâm Xanh](./C4%20Container%20Diagram%20%E2%80%93%20Vegetarian%20Support%20System.drawio.png)

*Mã nguồn sơ đồ Draw.io gốc:* [C4 Container Diagram – Vegetarian Support System.drawio](./C4%20Container%20Diagram%20%E2%80%93%20Vegetarian%20Support%20System.drawio)

---

## 3. Phân Tích Chi Tiết Các Thành Phần Kiến Trúc

### 3.1. Tác Nhân Người Dùng (Actors)

* **Tên khối:** `User`
* **Loại phân quyền:** `Guest / Member (Customer & Expert) / Administrator`
* **Mô tả:** Người dùng tương tác trực tiếp với hệ thống thông qua trình duyệt web trên máy tính hoặc thiết bị di động để sử dụng các tính năng hỗ trợ lối sống ăn chay:
  * **Khách vãng lai (Guest):** Xem danh sách và chi tiết công thức nấu ăn công khai, tìm kiếm công thức theo thể loại/nguyên liệu, và trải nghiệm AI Chatbot cơ bản dưới sự kiểm soát tốc độ kỹ thuật (`FR-51`, giới hạn tối đa 10 request/phút qua IP + cookie ẩn danh).
  * **Thành viên (Member):**
    * *Customer (`ROLE_CUSTOMER`):* Quản lý hồ sơ cá nhân và thể trạng/dinh dưỡng, xem chi tiết, bình luận và phản hồi bài viết, bình chọn Like/Dislike bài công thức (`RECIPE_REACTION`), lưu công thức yêu thích (`Saved Recipe`), lập kế hoạch bữa ăn cá nhân 7 ngày (`Meal Plan`), tạo danh sách mua sắm (`Shopping List`), nộp đơn đăng ký Chuyên gia (`FR-05`, `BR-74`), nâng cấp và sử dụng các tính năng AI mở rộng theo gói hội viên (FREE, PLUS, PRO).
    * *Chuyên gia (`ROLE_EXPERT`):* Kế thừa toàn bộ quyền Customer, sở hữu độc quyền quyền tạo, chỉnh sửa, xóa và tải ảnh cho các bài viết công thức (`Recipe Post` theo `BR-07`, `FR-04`).
  * **Quản trị viên (Administrator):** Quản lý từ điển nguyên liệu chuẩn, đơn vị và tỷ lệ quy đổi (`FR-18`), kiểm duyệt nội dung cộng đồng và xử lý báo cáo vi phạm (`Report`), xét duyệt đơn đăng ký Chuyên gia (`EXPERT_APPLICATION` theo `BR-74`), quản lý danh sách tài khoản người dùng và giám sát trạng thái hệ thống.

---

### 3.2. Ranh Giới Hệ Thống Mâm Xanh (Mâm Xanh System Boundary)

Khu vực đóng khung nét đứt thể hiện phạm vi phần mềm do đội ngũ phát triển Mâm Xanh trực tiếp xây dựng và duy trì. Phạm vi này bao gồm **4 Container** chính:

#### 1. Web Application (Frontend Container)
* **Loại container:** Client-side Web Application (Single-Page Application - SPA).
* **Công nghệ:** React 18, TypeScript, Vite, Tailwind CSS, Axios/Fetch.
* **Môi trường triển khai:** Azure Static Web Apps.
* **Trách nhiệm chính:**
  * Cung cấp giao diện đồ họa người dùng responsive, tối ưu hiển thị trên cả desktop và mobile.
  * Xử lý điều hướng phía client (Client-side Routing), quản lý trạng thái hiển thị (UI State), xử lý logic form (validation định dạng sơ bộ trước khi gửi request).
  * Nhúng trình phát video của YouTube trực tiếp trên trang chi tiết công thức (`FR-15`).
  * Tích hợp Google Identity Services (GIS) button để người dùng kích hoạt luồng đăng nhập nhanh bằng tài khoản Google (`FR-03`).
  * Đóng gói và gửi các yêu cầu API định dạng JSON qua HTTPS tới Backend REST API, đồng thời nhận và hiển thị phản hồi dữ liệu hoặc thông báo lỗi thân thiện.
* **Ranh giới tin cậy (Trust Boundary):** **Untrusted (Không tin cậy)**. Frontend chạy trên thiết bị cá nhân của người dùng, có thể bị can thiệp hoặc sửa đổi mã nguồn client. Mọi thẩm định phân quyền (Authorization), xác thực vai trò (Role-based Access Control), kiểm tra quyền tính năng AI (Feature Entitlement), và kiểm tra tính hợp lệ dữ liệu kinh doanh bắt buộc phải được tái thẩm định độc lập tại Backend. Không được lưu trữ các secret nhạy cảm (như Gemini API Key, Storage Access Key, Database Password) tại container này.

#### 2. Backend REST API (Backend Container)
* **Loại container:** Server-side Web API Application.
* **Công nghệ:** Java 21 LTS, Spring Boot 3 (Spring Web MVC, Spring Security, Spring Data JPA, Hibernate, Flyway).
* **Môi trường triển khai:** Azure App Service (Linux).
* **Trách nhiệm chính:**
  * Là **Ranh giới tin cậy cốt lõi (Core Trusted Boundary)** của toàn bộ hệ thống.
  * **Bảo mật & Phiên làm việc:** Xác thực danh tính người dùng bằng Access Token (JWT Bearer Token ngắn hạn) và Refresh Token lưu trữ trong HttpOnly Cookie; phân quyền truy cập theo vai trò (`ROLE_GUEST`, `ROLE_MEMBER`, `ROLE_ADMIN`).
  * **Kiểm soát Quyền Tính năng AI (Feature-based Entitlement):** Thẩm định gói hội viên của người dùng (FREE / PLUS / PRO) trước khi cho phép gọi các endpoint AI tương ứng (`FR-21`, `FR-47`, `FR-36`, `FR-51`), trả về mã lỗi `403 Forbidden` nếu chưa đủ thẩm quyền gói theo đúng Business Rule `BR-02` và `BR-03`.
  * **Kiểm soát Tốc độ Kỹ thuật (Technical Rate Limiting):** Áp dụng rate limiting ở tầng server đối với các request AI từ Guest và Member nhằm bảo vệ hạ tầng và hạn chế lạm dụng tài nguyên API đám mây.
  * **Thực thi Quy tắc Nghiệp vụ (Business Rules):** Thực thi toàn bộ các ràng buộc kinh doanh (ví dụ: công thức phải có 10–5.000 ký tự hướng dẫn tự do `instructions`, tối đa 1 ảnh đại diện $\le 5$ MB, Like/Dislike áp dụng độc quyền cho Recipe Post qua `RECIPE_REACTION` và bình luận không có Like theo `FR-45`, xử lý kiểm duyệt ghi trực tiếp vào Report).
  * **Cấu trúc phần mềm:** Áp dụng mô hình **Modular Monolith** theo business capability (`auth`, `recipe`, `mealplan`, `shopping`, `nutrition`, `subscription`, `admin`). Bên trong từng module tuân thủ cấu trúc phân lớp MVC (`Controller` $\rightarrow$ `Service` $\rightarrow$ `Repository` $\rightarrow$ `Entity`/`Model`).
  * **Tích hợp dịch vụ:** Tương tác với cơ sở dữ liệu quan hệ qua Spring Data JPA/Hibernate, tải tệp ảnh lên Azure Blob Storage qua Azure SDK, gửi prompt và ngữ cảnh tới Google Gemini API, và thẩm định chữ ký số ID Token với Google Identity Services.

#### 3. Application Database (Database Container)
* **Loại container:** Relational Database Management System (RDBMS).
* **Công nghệ:** Microsoft SQL Server (Azure SQL Database Serverless).
* **Môi trường triển khai:** Azure SQL Serverless (tự động điều chỉnh quy mô tính toán và tạm dừng khi không có truy vấn để tiết kiệm chi phí).
* **Trách nhiệm chính:**
  * Là **Nguồn sự thật quan hệ duy nhất (Relational Source of Truth)** của toàn hệ thống.
  * Lưu trữ bền vững dữ liệu nghiệp vụ của đúng **21 thực thể khái niệm chuẩn hóa (Conceptual ERD Baseline v1.7.0)**:
    1. `User` (Tài khoản, vai trò, hồ sơ cá nhân và chỉ số dinh dưỡng/thể trạng)
    2. `User Ingredient Preference` (Sở thích, kiêng kỵ và dị ứng nguyên liệu)
    3. `Recipe Post` (Bài công thức kèm trường `instructions` 10–5.000 ký tự, thể loại `dish_category`, `youtube_url`)
    4. `Ingredient` (Từ điển nguyên liệu chuẩn)
    5. `Recipe Ingredient` (Định lượng nguyên liệu theo bài công thức)
    6. `Saved Recipe` (Bộ sưu tập bài viết đã lưu)
    7. `Comment` (Bình luận và phản hồi lồng đa cấp)
    8. `Report` (Báo cáo vi phạm và lý do/kết quả kiểm duyệt)
    9. `Meal Plan` (Kế hoạch thực đơn 7 ngày)
    10. `Meal Plan Entry` (Chi tiết từng bữa sáng, trưa, tối trong thực đơn)
    11. `Shopping List` (Danh sách mua sắm nguyên liệu)
    12. `Shopping List Item` (Từng món hàng cần mua)
    13. `Subscription` (Gói hội viên: FREE, PLUS, PRO)
    14. `Payment Transaction` (Lịch sử giao dịch thanh toán qua payOS)
    15. `Notification` (Thông báo người dùng)
    16. `Recipe Media` (`RECIPE_MEDIA`: Quản lý 0–5 hình ảnh minh họa bài viết kèm cờ ảnh bìa `is_cover`)
    17. `Recipe Reaction` (`RECIPE_REACTION`: Bình chọn Like / Dislike của Member và tỷ lệ % Like)
    18. `Recipe View` (`RECIPE_VIEW`: Ghi nhận sự kiện xem bài viết khử trùng lặp cửa sổ 30 phút)
    19. `Unit` (`UNIT`: Từ điển đơn vị đo lường chuẩn hóa)
    20. `Ingredient Unit Conversion` (`INGREDIENT_UNIT_CONVERSION`: Bảng quy đổi đơn vị đặc thù sang gram)
    21. `Expert Application` (`EXPERT_APPLICATION`: Đơn đăng ký quyền Chuyên gia của Customer theo BR-74)
  * Lưu trữ siêu dữ liệu (metadata), trạng thái giao dịch, đường dẫn URL trỏ tới ảnh lưu trên Azure Blob Storage (`RECIPE_MEDIA`), và đường dẫn video YouTube (`youtube_url`).
  * Thực thi các ràng buộc toàn vẹn dữ liệu (Primary Key, Foreign Key, Unique Indexes, Check Constraints). Quản lý lược đồ database thông qua các migration scripts của Flyway.

#### 4. Recipe Image Storage (Storage Container)
* **Loại container:** Cloud Object / Binary Blob Storage.
* **Công nghệ:** Azure Blob Storage (Hot / Cool tier).
* **Trách nhiệm chính:**
  * Lưu trữ các tệp nhị phân hình ảnh minh họa của bài viết công thức (`Recipe Post` qua thực thể `RECIPE_MEDIA`).
  * Giới hạn kỹ thuật nghiêm ngặt: Mỗi bài công thức chỉ có tối đa **5 ảnh** (`RECIPE_MEDIA`), dung lượng tối đa $\le 5$ MB/ảnh, định dạng cho phép: JPEG, PNG, WebP; trong đó có đúng 1 ảnh đại diện (`is_cover = true` theo BR-20).
  * Tách biệt hoàn toàn việc lưu trữ dữ liệu nhị phân dung lượng lớn ra khỏi cơ sở dữ liệu quan hệ, giúp database duy trì kích thước nhỏ gọn, tốc độ sao lưu/phục hồi nhanh chóng và tối ưu hiệu suất truy vấn.
  * Cung cấp URL công khai hoặc qua CDN để trình duyệt người dùng có thể tải hình ảnh bài viết nhanh chóng với độ trễ thấp.

---

### 3.3. Các Hệ Thống Bên Ngoài (External Systems / Services)

Hệ thống Mâm Xanh tích hợp có chọn lọc với 3 dịch vụ đám mây bên thứ ba được thể hiện rõ ràng trong sơ đồ C4 Container:

#### 1. Google Gemini (AI Service)
* **Loại dịch vụ:** Large Language Model (LLM) Cloud API.
* **Công nghệ / Model:** Google Gemini API (model `gemini-1.5-flash` hoặc phiên bản ổn định được phê duyệt).
* **Trách nhiệm:**
  * Tiếp nhận yêu cầu xử lý ngôn ngữ tự nhiên từ Backend REST API và sinh nội dung phản hồi thông minh.
  * Phục vụ 4 tính năng AI trong hệ thống:
    1. AI Chatbot giải đáp thắc mắc ăn chay (`FR-51`).
    2. AI hỗ trợ tạo nội dung giới thiệu và hướng dẫn chế biến tự do cho bài viết công thức (`FR-21`).
    3. AI gợi ý biến thể món ăn dựa trên nguyên liệu thay thế (`FR-47`).
    4. AI sinh gợi ý thực đơn tuần 7 ngày tự động theo chỉ số dinh dưỡng (`FR-36`).
* **Cơ chế giao tiếp & Bảo mật:**
  * Backend REST API là điểm duy nhất được phép giao tiếp với Gemini API thông qua HTTPS / REST.
  * API Key của Google Gemini được bảo vệ bí mật trong biến môi trường của máy chủ Backend (Azure App Service Configuration / Azure Key Vault), tuyệt đối không để lộ về phía client/browser.
  * Backend thực hiện kỹ thuật *Prompt Minimization*: chỉ gửi các trường ngữ cảnh dữ liệu thực sự cần thiết cho tác vụ (ví dụ: tên món, định lượng nguyên liệu, hướng dẫn tự do; không bao giờ gửi thông tin người dùng hay toàn bộ database).
  * Đo lường kỹ thuật (Telemetry): Backend ghi vết số token tiêu thụ, thời gian phản hồi và mã trạng thái kỹ thuật vào log hệ thống để theo dõi tải, không ghi nhận nội dung prompt nhạy cảm.

#### 2. YouTube (Media Embed Service)
* **Loại dịch vụ:** Third-party Video Streaming Platform.
* **Công nghệ:** YouTube IFrame Player API / Standard Embed Player.
* **Trách nhiệm:**
  * Phát video hướng dẫn chế biến món ăn trực tiếp trên giao diện trang chi tiết công thức của Web Application.
* **Cơ chế giao tiếp & Quyết định kiến trúc:**
  * Người dùng khi tạo hoặc chỉnh sửa bài viết công thức chỉ cung cấp đường dẫn URL video YouTube hợp lệ (`youtube_url`).
  * Web Application sử dụng URL này để nhúng iframe của YouTube trên trình duyệt người dùng.
  * **Quyết định tối ưu hóa chi phí:** Hệ thống Mâm Xanh **hoàn toàn không lưu trữ tệp video** trên Azure Blob Storage hay máy chủ Backend, giúp loại bỏ chi phí lưu trữ khổng lồ và chi phí băng thông truyền phát video. Trình duyệt client kết nối trực tiếp tới máy chủ của YouTube qua HTTPS.

#### 3. Google Authentication (Identity Service)
* **Loại dịch vụ:** OAuth 2.0 / OpenID Connect Identity Provider.
* **Công nghệ:** Google Identity Services (GIS) for Web.
* **Trách nhiệm:**
  * Cung cấp cơ chế đăng nhập một chạm an toàn và tiện lợi cho người dùng thông qua tài khoản Google có sẵn (`FR-03`).
* **Cơ chế giao tiếp & Xác thực:**
  1. Người dùng nhấn nút đăng nhập Google trên `Web Application`.
  2. Google SDK trên trình duyệt xử lý đăng nhập và trả về một chuỗi `Google ID Token` (JWT đã được Google ký số).
  3. `Web Application` gửi chuỗi ID Token này về `Backend REST API`.
  4. `Backend REST API` kết nối với dịch vụ xác thực của Google qua HTTPS để thẩm định chữ ký số và giải mã thông tin tài khoản (Google Subject ID, email, họ tên).
  5. Nếu token hợp lệ, Backend tìm kiếm hoặc tự động khởi tạo tài khoản `User` tương ứng trong `Application Database`, sau đó phát hành cặp token JWT nội bộ của Mâm Xanh để duy trì phiên làm việc cho client.

#### 4. Ghi Chú Về Các Dịch Vụ Mở Rộng Trong Kiến Trúc Tổng Thể
Để duy trì tính tinh gọn và tập trung cho sơ đồ C4 Container hiện tại, hai dịch vụ bên ngoài khác đã được quy hoạch chính thức trong [ARCHITECTURE.md](../../architecture/ARCHITECTURE.md) nhưng chưa biểu diễn chi tiết trên sơ đồ này bao gồm:
* **payOS (VietQR Payment Gateway):** Cung cấp cổng thanh toán trực tuyến qua mã VietQR chuẩn NAPAS 247 để xử lý giao dịch kích hoạt gói hội viên PLUS/PRO (`FR-12`, `FR-13`). Backend giao tiếp với payOS qua HTTPS REST API và tiếp nhận xác nhận thanh toán qua Webhook bảo mật bằng chữ ký HMAC-SHA256.
* **Brevo (Transactional Email Service):** Cung cấp dịch vụ gửi email giao dịch qua SMTP/API phục vụ kích hoạt tài khoản và gửi mã đặt lại mật khẩu (`FR-03`, `FR-06`).

Hai dịch vụ này hoạt động theo nguyên tắc tích hợp tương tự (được quản lý hoàn toàn ở tầng Backend qua kết nối bảo mật HTTPS) và sẽ được thể hiện đầy đủ ở các phiên bản sơ đồ phân hệ chuyên sâu.

---

## 4. Ma Trận Luồng Dữ Liệu và Giao Thức Giao Tiếp

Bảng dưới đây tổng hợp đầy đủ các mối quan hệ liên kết (Relationships) được mô hình hóa trong sơ đồ C4 Container:

| # | Nguồn (Source) | Đích (Target) | Giao thức / Công nghệ | Mục đích & Nội dung truyền tải |
|---|---|---|---|---|
| **1** | `User` | `Web Application` | `HTTPS` | Người dùng mở trình duyệt, truy cập hệ thống Mâm Xanh và tương tác với các màn hình chức năng. |
| **2** | `Web Application` | `Backend REST API` | `HTTPS / REST / JSON` | Trình duyệt gửi các yêu cầu API (kèm JWT Bearer Token trong Authorization Header hoặc Refresh Cookie), nhận kết quả JSON từ server. |
| **3** | `Backend REST API` | `Application Database` | `JDBC / Microsoft SQL Server Driver (TLS Port 1433)` | Backend đọc/ghi dữ liệu quan hệ của 17 thực thể thông qua Spring Data JPA và Flyway migrations. |
| **4** | `Backend REST API` | `Recipe Image Storage` | `Azure Blob Storage API / HTTPS` | Backend kiểm tra hợp lệ tệp ảnh đại diện ($\le 5$ MB), tải ảnh lên Blob Storage và nhận URL tĩnh trả về. *(Trên sơ đồ C4 gốc, mối quan hệ logic lưu trữ tham chiếu ảnh được mô tả gắn kết với cơ sở dữ liệu).* |
| **5** | `Backend REST API` | `Google Gemini` | `Gemini API / HTTPS` | Backend gửi prompt và dữ liệu ngữ cảnh tối giản để nhận nội dung văn bản AI sinh ra (Chatbot, Recipe Authoring, Variations, Meal Planner). |
| **6** | `Web Application` | `YouTube` | `YouTube Embed / HTTPS` | Trình duyệt của người dùng tải trực tiếp iframe/player của YouTube để phát video từ liên kết `youtube_url`. |
| **7** | `Backend REST API` | `Google Authentication` | `Google Identity Services / HTTPS` | Backend kiểm tra tính hợp lệ và thẩm định chữ ký số của Google ID Token khi người dùng đăng nhập bằng Google. |

---

## 5. Ranh Giới Tin Cậy và Nguyên Tắc Bảo Mật (Trust Boundaries)

Kiến trúc C4 Container của Mâm Xanh thiết lập 3 ranh giới bảo mật nghiêm ngặt:

1. **Ranh giới Mạng Công Khai (Public Internet vs. Web Application):**
   * Người dùng tương tác qua mạng Internet thông qua giao thức `HTTPS` bắt buộc (TLS 1.2+).
   * Static assets của Web Application được nén và phục vụ từ mạng phân phối biên của Azure Static Web Apps.

2. **Ranh giới Client - Server (Web Application vs. Backend REST API):**
   * Đây là ranh giới phân tách giữa môi trường **Untrusted (Không tin cậy)** và **Trusted (Tin cậy)**.
   * Mọi yêu cầu từ Frontend đều phải được xác thực danh tính qua JWT.
   * Backend không tin tưởng bất kỳ giá trị nào do Frontend gửi lên; mọi tham số đều phải trải qua tầng validation (Bean Validation `@Valid`, custom validators).
   * Ngăn chặn các lỗ hổng phổ biến: SQL Injection (ngăn ngừa tuyệt đối qua Parameterized Queries/JPA), XSS (thoát mã hóa đầu ra), CSRF (bảo vệ qua cơ chế SameSite Cookie cho Refresh Token và Stateless JWT cho API).

3. **Ranh giới Nội bộ và Dịch vụ Bên ngoài (Backend REST API vs. Database / External Providers):**
   * Toàn bộ chuỗi kết nối (Connection String) của SQL Server, Access Key của Azure Blob Storage, và Gemini API Key **chỉ tồn tại ở môi trường máy chủ Backend** (được cấu hình qua biến môi trường an toàn trên Azure App Service).
   * Cơ sở dữ liệu Azure SQL được cấu hình tường lửa chỉ chấp nhận kết nối từ IP của Azure App Service hoặc các IP phát triển được cấp phép; kết nối dữ liệu bắt buộc mã hóa TLS trên cổng TCP 1433.
   * Lỗi từ các dịch vụ bên ngoài (Gemini quá tải, mạng chập chờn) được Backend bao bọc và xử lý bằng các khối Fallback/Circuit Breaker thích hợp, bảo đảm không bao giờ để lộ thông tin cấu hình nhạy cảm ra ngoài phản hồi API (`NFR-18`).

---

## 6. Quy Ước Đặt Tên File và Quản Lý Sơ Đồ

Để đảm bảo tính nhất quán trong kho lưu trữ mã nguồn và tài liệu của dự án:

1. **Vị trí lưu trữ:**
   * Thư mục chứa: `docs/diagrams/C4 Container Diagram/`
   * File mã nguồn sơ đồ Draw.io: `C4 Container Diagram – Vegetarian Support System.drawio`
   * File hình ảnh xuất ra (PNG) dùng nhúng vào tài liệu: `C4 Container Diagram – Vegetarian Support System.drawio.png`

2. **Quy trình cập nhật khi kiến trúc thay đổi:**
   * Khi có sự thay đổi về mặt kiến trúc thực thi (ví dụ: bổ sung container mới, thay đổi công nghệ cơ sở dữ liệu, hoặc bổ sung tích hợp dịch vụ thanh toán/email lên sơ đồ):
     1. Mở file mã nguồn `.drawio` bằng công cụ [Draw.io](https://app.diagrams.net/) hoặc extension Draw.io Integration trong IDE.
     2. Thực hiện chỉnh sửa các khối hình, nhãn công nghệ, và đường mũi tên tương ứng.
     3. Xuất file ảnh mới đè lên file `C4 Container Diagram – Vegetarian Support System.drawio.png` (độ phân giải tối thiểu 300 DPI, nền trong suốt hoặc trắng rõ ràng).
     4. Cập nhật nội dung giải thích tương ứng trong file `README.md` này.
     5. Tăng phiên bản tài liệu (`Version`) theo quy tắc phiên bản ngữ nghĩa (Semantic Versioning) và cập nhật trường `Last Updated`.
