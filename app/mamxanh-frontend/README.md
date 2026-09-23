> **Document:** Frontend Workspace Guide (Mâm Xanh)  
> **File:** `app/mamxanh-frontend/README.md`  
> **Version:** v1.3.0
> **Created:** 2026-09-18  
> **Last Updated:** 2026-09-22
> **Status:** Active  

# Mâm Xanh Frontend

Thư mục mã nguồn giao diện ứng dụng Mâm Xanh được xây dựng bằng **React 19**, **TypeScript**, **Vite 8**, **Tailwind CSS v4** và xuất phát từ thiết kế Figma Make.

Dự án đã được cấu hình tối ưu để mở, chỉnh sửa, chạy và gỡ lỗi trực tiếp trên **IntelliJ IDEA** (hoặc WebStorm/VS Code).

## Giao diện demo và giới hạn hiện tại

Bản demo cho giảng viên: [Mâm Xanh trên Vercel](https://mamxanh-frontend.vercel.app/). Đây là bản Frontend dùng dữ liệu mẫu; tài khoản Lan Anh và gói FREE là dữ liệu demo, chưa có đăng nhập hoặc phân quyền thật. Các màn hình Auth bổ sung ngày 2026-09-22 mới có ở source local, chưa deploy lại Vercel.

### Đăng ký, đăng nhập và khôi phục tài khoản (UI FR-03)

- Header mặc định dành cho Guest có Đăng nhập/Đăng ký. Các trang `/dang-nhap`, `/dang-ky`, `/quen-mat-khau`, `/xac-minh-email`, `/dat-lai-mat-khau` dùng bố cục responsive riêng cùng nhận diện dự án.
- Đăng ký kiểm tra tên 3–50 ký tự, email, mật khẩu ít nhất 8 ký tự và xác nhận khớp; có nút hiện/ẩn mật khẩu. Chính sách độ phức tạp cần thống nhất theo BUG-004, chưa tuyên bố đầy đủ validation FR-03.
- Nút Google và các biểu mẫu hiện thông báo demo; không gọi API, không gửi email, không tạo tài khoản/phiên, không xác minh token và không lưu mật khẩu vào storage. Mật khẩu được xóa khỏi state sau khi biểu mẫu hợp lệ.
- Trang xác minh có yêu cầu gửi lại email; trang đặt lại mật khẩu cần liên kết email thật khi tích hợp Backend. Các giới hạn thời gian phía server chưa được mô phỏng thành cơ chế bảo mật FE.
- Tại trang đăng nhập, chọn **Khám phá tài khoản demo** để xem menu Lan Anh/FREE; chọn **Thoát tài khoản demo** để quay lại Guest. Đây chỉ là chuyển chế độ xem trong bộ nhớ, không phải authentication/authorization.
- API contract hiện `Active` nhưng chưa tích hợp trong UI này; xem [API Guide](../../docs/api/API.md) trước khi triển khai xác thực thật.

### Các chức năng demo khác

| Khu vực | Người dùng có thể thử | Giới hạn hiện tại |
|---|---|---|
| Trang chủ và thanh điều hướng | Logo dự án, menu desktop trên một hàng, tiêu đề hai dòng; menu thu gọn trên màn hình nhỏ | Logo ứng dụng/favicon dùng tài nguyên của dự án |
| Bình luận công thức | Viết, trả lời, sửa và xóa bình luận; tối đa 5 cấp; xóa bình luận cha vẫn giữ trả lời | Chỉ giữ trong bộ nhớ khi trang còn mở, chưa gửi Backend |
| Khẩu phần tại công thức | Chọn 1–50 phần; nguyên liệu = lượng gốc / số phần gốc × số phần muốn nấu | Làm tròn tối đa 2 chữ số thập phân; lượng không đọc được thành số giữ nguyên; chưa đồng bộ Shopping List |
| Kế hoạch bữa ăn | Chọn 0,5–10 phần cho từng món, bước 0,5 | Chỉ cập nhật trạng thái UI; chưa lưu hoặc tổng hợp dinh dưỡng thật |
| Báo cáo công thức | Mở nút Báo cáo, chọn 1 trong 6 lý do; lý do Khác cần mô tả 10–500 ký tự | Nút chỉ kiểm tra biểu mẫu, chưa gửi cho quản trị viên |
| Dinh dưỡng công thức | Đọc rõ số liệu minh họa cho **1 khẩu phần** | 8 chỉ tiêu tĩnh, chưa tính từ nguyên liệu; còn thiếu so với FR-39, xem BUG-002 |
| Hồ sơ dinh dưỡng và BMI | Nhập tuổi, chiều cao, cân nặng; xác nhận phạm vi hỗ trợ rồi tính BMI tham khảo | Chưa lưu hồ sơ, chưa tính nhu cầu calorie hoặc đề xuất điều trị |
| Gói AI | Xem FREE/PLUS/PRO và giá 0/49.000/99.000 VNĐ mỗi tháng | FREE hiện tại là mock; nút thanh toán bị vô hiệu hóa, chưa mua hoặc kích hoạt quyền thật |
| Lịch sử giao dịch | Mở trang lịch sử và xem trạng thái chưa có dữ liệu | Chưa kết nối API giao dịch; không tạo giao dịch giả |

Hồ sơ BMI, **Nâng cấp gói AI** và lịch sử giao dịch nằm trong menu avatar; gói `FREE · demo` hiện dưới tên tài khoản. Trên mobile, mở menu để truy cập các trang này. Đường dẫn trực tiếp: `/ho-so/dinh-duong`, `/goi-ai`, `/giao-dich`.

Các màn hình này chuẩn bị trải nghiệm cho FR-13, FR-20, FR-26/27, FR-35/38, FR-37/39 và FR-46; không xác nhận đã hoàn thành toàn bộ Acceptance Criteria của các FR. SRS vẫn là nguồn yêu cầu chính thức.

## Vercel cho buổi demo

- Bản UI được triển khai thủ công từ thư mục Frontend. Vercel gắn nhãn môi trường `Production` cho link demo; đây chưa phải sản phẩm cuối cùng.
- `vercel.json` chuyển các đường dẫn SPA về `index.html`, giúp mở trực tiếp hoặc tải lại trang con bằng React Router.
- Build dùng `npm run build`, đầu ra `dist/`. `.vercel/` là thông tin liên kết tài khoản/project local và được bỏ qua trong Git.
- Không thiết lập GitHub CI/CD hoặc Git auto-deploy cho bản demo này. Push code không tự cập nhật link Vercel; lần cập nhật demo sau cần deploy thủ công nếu được yêu cầu.
- Azure vẫn là baseline triển khai toàn hệ thống. Vercel chỉ phục vụ buổi giới thiệu UI, không thay thế quyết định kiến trúc.

---

## 1. Yêu cầu môi trường

- **Node.js**: Phiên bản `>= 20.x` (khuyến nghị `v24.x` hoặc `v22.x LTS`).
- **Package Manager**: `npm` (tiêu chuẩn của dự án).
- **IDE**: IntelliJ IDEA (Ultimate hoặc Community với Node.js/Web plugin) / WebStorm.

---

## 2. Cách mở và chạy trong IntelliJ IDEA

### Bước 1: Mở dự án trong IntelliJ IDEA
1. Khởi động **IntelliJ IDEA**.
2. Chọn **File** -> **Open...** (hoặc **Open Project** ở màn hình Welcome).
3. Trỏ tới thư mục:
   ```
   D:\Semester 5\SWP391\vegetarian-support-system\app\mamxanh-frontend
   ```
4. Chọn **Trust Project** nếu có thông báo tin cậy dự án.

### Bước 2: Chạy ứng dụng bằng nút Run (Play) trên thanh công cụ
Dự án đã được cấu hình sẵn các **Run Configurations** nằm trong `.idea/runConfigurations/` và `.run/`:
- **`dev`**: Khởi chạy dev server Vite (tự động mở trình duyệt tại `http://localhost:5173`).
- **`build`**: Đóng gói production build vào thư mục `dist/`.
- **`preview`**: Chạy preview bản build tại `http://localhost:4173`.

> **Thao tác**: Trên thanh công cụ trên cùng góc phải, chọn configuration **`dev`** và bấm nút **Play (▶️)** (hoặc `Shift + F10`). Trình duyệt sẽ tự động mở trang web Mâm Xanh.

### Bước 3: Sử dụng cửa sổ công cụ `npm` trong IntelliJ
Nếu bạn muốn chạy các lệnh nhanh từ giao diện đồ họa:
1. Vào menu **View** -> **Tool Windows** -> **npm** (hoặc click icon `npm` ở thanh bên).
2. Danh sách các script sẽ hiển thị: `dev`, `build`, `preview`, `lint`, `format`.
3. Nhấp đúp (Double-click) vào `dev` để chạy dev server.

---

## 3. Các lệnh dòng lệnh (Terminal)

Nếu sử dụng Terminal trong IntelliJ (`Alt + F12`) hoặc Command Prompt/PowerShell:

```bash
# 1. Cài đặt thư viện (nếu clone mới)
npm install

# 2. Khởi chạy môi trường phát triển (Hot reload)
npm run dev

# 3. Kiểm tra lỗi kiểu TypeScript
npm run lint

# 4. Đóng gói cho Production
npm run build

# 5. Xem trước bản đóng gói Production
npm run preview

# 6. Chạy Frontend browser smoke test bằng Playwright
# Lệnh này tự build trước khi khởi động preview server trên 127.0.0.1:4173.
npm run test:e2e

# 7. Mở Playwright UI hoặc xem HTML report của lần chạy gần nhất
npm run test:e2e:ui
npm run test:e2e:report
```

---

## 4. Cấu trúc thư mục mã nguồn

```
app/mamxanh-frontend/
├── .idea/                 # Cấu hình dự án IntelliJ IDEA (Modules, Run Configurations, Code Styles)
├── .run/                  # Shared Run Configurations (dev, build, preview)
├── .figma/                # Dữ liệu xuất và cấu hình từ Figma Make
├── public/                # Tài nguyên tĩnh (ảnh, favicon, robots)
├── tests/e2e/             # Playwright browser smoke và E2E tests có thể chạy lại
├── src/
│   ├── components/        # Các UI component dùng chung (Layout, Header, Footer, v.v.)
│   ├── pages/             # Các trang nghiệp vụ:
│   │   ├── Home.tsx             # Trang chủ giới thiệu
│   │   ├── Explore.tsx          # Khám phá công thức chay
│   │   ├── RecipeDetail.tsx     # Chi tiết công thức nấu ăn
│   │   ├── CreateRecipe.tsx     # Tạo và chia sẻ công thức mới
│   │   ├── MealPlanner.tsx      # Lập kế hoạch thực đơn
│   │   ├── ShoppingList.tsx     # Danh sách đi chợ thông minh
│   │   ├── NutritionTracker.tsx # Nhật ký theo dõi dinh dưỡng
│   │   ├── Community.tsx        # Diễn đàn cộng đồng
│   │   ├── PostDetail.tsx       # Chi tiết bài viết cộng đồng
│   │   ├── NutritionProfile.tsx # Hồ sơ BMI tham khảo
│   │   ├── AiPlans.tsx          # Gói AI và trạng thái gói demo
│   │   ├── TransactionHistory.tsx # Lịch sử giao dịch chưa kết nối API
│   │   └── Profile.tsx          # Hồ sơ cá nhân
│   ├── data/              # Dữ liệu mẫu (mock data cho UI)
│   ├── types.ts           # Kiểu dữ liệu TypeScript
│   ├── App.tsx            # Cấu hình định tuyến React Router
│   ├── main.tsx           # Entry point React 19
│   └── index.css          # Tailwind CSS styles
├── package.json           # Khai báo thư viện và npm scripts
├── playwright.config.ts   # Base URL, preview web server, Chromium và test evidence
├── tsconfig.json          # Cấu hình TypeScript compiler và alias path (@/*)
└── vite.config.ts         # Cấu hình build Vite (server port 5173, alias, plugins)
```

---

## 5. Các cấu hình đã được tối ưu cho IntelliJ IDEA

1. **Quản lý tài nguyên & Hiệu năng**:
   - `src/` được đánh dấu là **Source Root** giúp IntelliJ nhận diện auto-import và path alias `@/` chính xác.
   - `node_modules/`, `dist/`, `.figma/`, `out/` đã được **Exclude** khỏi quá trình Indexing, giúp IntelliJ không bị đơ giật hay chiếm dụng bộ nhớ RAM.
2. **Cổng Dev Server**:
   - Đã chuyển từ cổng mặc định của Figma (`8443` trên `0.0.0.0`) sang cổng tiêu chuẩn `5173` trên `localhost`.
   - Tự động mở trình duyệt khi ấn Run (`open: true`).
3. **Tiêu chuẩn mã nguồn**:
   - Tương thích 100% Vite 8 native ESM và TypeScript 5.7.
   - Xóa bỏ các cảnh báo deprecated của Vite liên quan đến import JSON và `__dirname`.

---

## 6. Playwright browser testing

Playwright có hai cách sử dụng khác nhau:

- Browser control của agent dùng cho exploratory verification, kiểm tra console/network và thu thập bằng chứng trong một task cụ thể. Cách này không tự tạo regression test.
- `@playwright/test` chạy các test có thể lặp lại trong `tests/e2e/` bằng `npm run test:e2e`.

Lần đầu chạy trên một máy mới, cài Chromium runtime bằng:

```bash
npx playwright install chromium
```

`npm run test:e2e` tự gọi `npm run build`, sau đó Playwright khởi động Vite preview tại `http://127.0.0.1:4173`, chờ URL sẵn sàng rồi chạy Chromium. Port được giữ cố định và không tái sử dụng một server có sẵn để tránh kiểm thử nhầm ứng dụng.

HTML report được tạo trong `playwright-report/`; screenshot và trace lỗi nằm trong `test-results/`. Hai thư mục này là generated evidence và không được commit mặc định.

Suite hiện có 11 test: 3 test Auth (đăng ký/xác minh, đăng nhập/chế độ demo, khôi phục/mobile) và 8 test trước đó: application shell/navigation; bình luận và giữ reply khi xóa cha; khẩu phần lẻ trong kế hoạch; nhân nguyên liệu theo khẩu phần; menu avatar/gói AI; điều hướng tài khoản trên mobile; validation báo cáo; BMI cùng trang gói AI/lịch sử giao dịch. Đây là kiểm thử Frontend với dữ liệu mẫu, không chứng minh Backend, database, authentication, thanh toán hoặc full FE–BE E2E đã hoạt động.
