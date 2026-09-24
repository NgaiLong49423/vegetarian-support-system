> **Document:** C4 Container Diagram Workspace Guide
> **File:** `docs/diagrams/C4 Container Diagram/README.md`
> **Version:** v1.4.0
> **Created:** 2026-09-17
> **Last Updated:** 2026-09-24
> **Status:** Under Review
> **Related Docs:** `docs/architecture/ARCHITECTURE.md`, `docs/architecture/TECHNOLOGY-STACK.md`, `docs/diagrams/ERD/README.md`

# C4 Container Diagram — Hệ thống Mâm Xanh

README này diễn giải nội dung đang được thể hiện trong:

- [C4 Container Diagram – Vegetarian Support System.drawio](./C4%20Container%20Diagram%20%E2%80%93%20Vegetarian%20Support%20System.drawio): nguồn Draw.io có thể chỉnh sửa.
- [C4 Container Diagram – Vegetarian Support System.drawio.png](./C4%20Container%20Diagram%20%E2%80%93%20Vegetarian%20Support%20System.drawio.png): ảnh xuất dùng để xem và nhúng vào tài liệu.

![C4 Container Diagram của hệ thống Mâm Xanh](./C4%20Container%20Diagram%20%E2%80%93%20Vegetarian%20Support%20System.drawio.png)

## 1. Phạm vi của sơ đồ

Đây là sơ đồ kiến trúc ở mức **C4 Container (Level 2)**. Thuật ngữ *container* trong C4 chỉ một ứng dụng, dịch vụ hoặc data store có trách nhiệm và ranh giới riêng; nó không mặc định đồng nghĩa với Docker container.

Sơ đồ hiện chia kiến trúc thành bốn khu vực lớn:

1. **Frontend** — ứng dụng web React và TypeScript.
2. **Backend** — REST API Java 21 và Spring Boot.
3. **Database / Data Stores** — Microsoft SQL Server và Azure Blob Storage.
4. **External Systems** — Brevo, payOS, Google Authentication và Google Gemini.

Các khối Vercel và Azure ở ngoài các khu vực trên thể hiện nền tảng triển khai. Khung Docker thể hiện tooling phục vụ môi trường phát triển, không phải một C4 container nghiệp vụ.

## 2. Các thành phần được thể hiện

### 2.1. Frontend

| Thành phần | Phân loại | Công nghệ | Trách nhiệm |
|---|---|---|---|
| Web Application | C4 Container | React, TypeScript | Hiển thị giao diện, xử lý tương tác phía trình duyệt và gọi Backend REST API. |
| Frontend Cloud Platform | Deployment platform | Vercel | Build và host Web Application; đây không phải container nghiệp vụ do nhóm tự xây dựng. |

Frontend là môi trường **không tin cậy (untrusted)**. Backend phải xác thực lại authentication, authorization, input và Business Rule; secret của Backend không được đưa vào mã chạy trên trình duyệt.

### 2.2. Backend

| Thành phần | Phân loại | Công nghệ | Trách nhiệm |
|---|---|---|---|
| Backend REST API | C4 Container | Java 21, Spring Boot | Cung cấp REST API, xác thực và phân quyền, thực thi nghiệp vụ, truy cập data stores và điều phối external services. |
| Backend Cloud Platform | Deployment platform | Azure | Chạy Backend trên hạ tầng Azure; dịch vụ Azure cụ thể phải tiếp tục bám tài liệu kiến trúc và công nghệ có thẩm quyền. |

Backend là **trusted server-side boundary** của hệ thống. Credential của database, Blob Storage, Gemini, payOS và Brevo chỉ được cấu hình phía Backend hoặc secret store phù hợp.

### 2.3. Database và lưu trữ ảnh

| Thành phần | Phân loại | Công nghệ | Trách nhiệm |
|---|---|---|---|
| Application Database | C4 Container / Data Store | Microsoft SQL Server | Lưu dữ liệu quan hệ và là relational source of truth của ứng dụng. |
| Recipe Image Storage | C4 Container / Data Store | Azure Blob Storage | Lưu tệp ảnh Recipe Post; SQL Server chỉ giữ metadata và reference cần thiết. |

Backend là thành phần duy nhất trực tiếp đọc/ghi dữ liệu nghiệp vụ và điều phối thao tác với Blob Storage. Frontend không kết nối trực tiếp tới SQL Server.

### 2.4. External Systems

| Hệ thống bên ngoài | Tên phân loại trên sơ đồ | Vai trò |
|---|---|---|
| Brevo Email Service | Transactional Email Delivery Service | Nhận yêu cầu gửi email giao dịch và trả kết quả chấp nhận hoặc lỗi gửi email. |
| payOS | Online Payment Gateway | Tạo payment link/VietQR, trả trạng thái khởi tạo và gửi webhook kết quả thanh toán về Backend. |
| Google Authentication | External Identity Provider | Xác thực tài khoản Google và cung cấp ID Token để hệ thống kiểm tra trước khi tạo phiên nội bộ. |
| Google Gemini | AI Recommendation and Assistance Service | Nhận yêu cầu AI đã được Backend kiểm soát và trả về kết quả gợi ý hoặc hỗ trợ có cấu trúc. |

YouTube không nằm trong khối External Systems của sơ đồ hiện tại. Việc nhúng video từ một URL không được mô hình hóa như luồng xử lý dữ liệu nghiệp vụ hai chiều trong view này.

## 3. Luồng đi và luồng về

Sơ đồ dùng hai mũi tên một chiều để trình bày rõ request và response giữa các khu vực. Ý nghĩa mong đợi của từng cặp như sau:

| Luồng đi | Luồng về | Giao thức / Công nghệ | Ý nghĩa |
|---|---|---|---|
| Frontend → Backend | Backend → Frontend | HTTPS / REST / JSON | Frontend gửi API request; Backend trả API response hoặc lỗi chuẩn HTTP. |
| Backend → Application Database | Application Database → Backend | JDBC / Microsoft SQL Server Driver | Backend thực thi query/transaction; database trả kết quả hoặc trạng thái thao tác. |
| Backend → Recipe Image Storage | Recipe Image Storage → Backend | HTTPS / Azure Blob SDK | Backend upload, đọc hoặc xóa ảnh; Blob Storage trả metadata, URL hoặc lỗi. |
| Backend → External Systems | External Systems → Backend | Tùy provider | Backend gửi yêu cầu tích hợp; provider trả response, callback hoặc webhook. |

Giao thức của từng external service không hoàn toàn giống nhau:

- Google Gemini và payOS sử dụng HTTPS API; payOS còn có webhook trả về Backend.
- Google Authentication bắt đầu từ thao tác đăng nhập phía Frontend, sau đó ID Token được chuyển về Backend để kiểm tra.
- Brevo sử dụng SMTP/TLS theo technology baseline, vì vậy không nên gắn nhãn mọi external flow chỉ là HTTPS.

Không có luồng Frontend → Application Database và không có luồng External Systems → Application Database trực tiếp.

## 4. Docker và deployment

Docker được chọn để đồng bộ môi trường phát triển giữa các thành viên:

- `app/mamxanh-frontend/Dockerfile` định nghĩa môi trường Frontend.
- `app/mamxanh-backend/Dockerfile` định nghĩa môi trường Backend.
- Không có Dockerfile dùng chung tại repository root.
- Microsoft SQL Server chưa được đưa vào Docker trong quyết định hiện tại.
- Docker Compose chưa được chốt.
- Docker development không buộc Vercel phải chạy Frontend bằng container và chưa xác nhận Backend image là production artifact.

Các đường `Deployment` trên sơ đồ biểu diễn Web Application được triển khai lên Vercel và Backend được triển khai lên Azure. Chúng không phải request/response runtime của người dùng.

## 5. Điểm cần hoàn thiện trước khi duyệt sơ đồ

README đã được đồng bộ với nội dung hiện có, nhưng sơ đồ vẫn ở trạng thái `Under Review` vì còn các điểm sau:

1. Nhãn `Blackend` và `Blackend Cloud Platform` cần sửa thành `Backend` và `Backend Cloud Platform`.
2. Khung Docker hiện bao cả khu vực Database, trong khi quyết định đã chốt chỉ có Dockerfile riêng cho Frontend và Backend; cách vẽ này có thể khiến người đọc hiểu nhầm SQL Server cũng chạy bằng Docker.
3. Các đường request/response đang dùng mô tả giống nhau cho cả hai chiều. Nhãn chiều về nên dùng động từ như `Returns API responses` hoặc `Returns query results`.
4. Quan hệ Backend ↔ Recipe Image Storage chưa có cặp đường riêng, nên luồng Blob Storage chưa được thể hiện rõ.
5. External Systems đang dùng một cặp HTTPS tổng quát, chưa thể hiện khác biệt giữa Gemini API, payOS API/webhook, Google sign-in/token verification và Brevo SMTP/TLS.
6. Nhiều connector trong XML đang dùng tọa độ rời thay vì gắn `source`/`target` vào block; khi di chuyển block, đường nối có thể không đi theo.
7. Sơ đồ dùng Vercel cho Frontend, trong khi `ARCHITECTURE.md` và bảng deployment trong `TECHNOLOGY-STACK.md` vẫn ghi Azure Static Web Apps. Quyết định nguồn có thẩm quyền cần được đồng bộ riêng trước khi coi view này là baseline chính thức.

## 6. Quy trình cập nhật

Khi thay đổi sơ đồ:

1. Chỉnh file `.drawio` nguồn, không chỉnh trực tiếp PNG.
2. Kiểm tra tên block, loại C4, công nghệ, chiều mũi tên và protocol label.
3. Gắn connector vào đúng source/target để đường nối đi theo block.
4. Xuất lại PNG từ file `.drawio` sau khi hoàn tất.
5. Cập nhật README này theo đúng nội dung được thể hiện, nhưng không dùng README để thay thế quyết định trong `ARCHITECTURE.md` hoặc `TECHNOLOGY-STACK.md`.
6. Cập nhật `Version`, `Last Updated` và `Status` dựa trên mức thay đổi và trạng thái review thực tế.
