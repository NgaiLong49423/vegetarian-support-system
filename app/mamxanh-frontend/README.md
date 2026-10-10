> **Document:** Frontend Workspace Guide (Mâm Xanh)  
> **File:** `app/mamxanh-frontend/README.md`  
> **Version:** v1.17.0
> **Created:** 2026-09-18  
> **Last Updated:** 2026-10-10
> **Status:** Active  

# Mâm Xanh Frontend

Thư mục mã nguồn giao diện ứng dụng Mâm Xanh được xây dựng bằng **React 19**, **TypeScript**, **Vite 8**, **Tailwind CSS v4** và xuất phát từ thiết kế Figma Make.

Dự án đã được cấu hình tối ưu để mở, chỉnh sửa, chạy và gỡ lỗi trực tiếp trên **IntelliJ IDEA** (hoặc WebStorm/VS Code).

## Giao diện demo và giới hạn hiện tại

Bản demo Vercel hiện tại: [Mâm Xanh trên Vercel](https://mamxanh-frontend.vercel.app/) vẫn là bản UI cũ dùng dữ liệu mẫu và chưa có đăng nhập/phân quyền thật. Bản chạy local dưới đây dùng Backend và SQL Server thật cho các luồng đã tích hợp; thay đổi hiện tại chưa được deploy lên Vercel.

### Đăng ký, đăng nhập và khôi phục tài khoản (UI FR-03)

- Header mặc định dành cho Guest có Đăng nhập/Đăng ký. Các trang `/dang-nhap`, `/dang-ky`, `/quen-mat-khau`, `/xac-minh-email`, `/dat-lai-mat-khau` dùng bố cục responsive riêng cùng nhận diện dự án.
- **Đăng ký và xác minh email đã gọi Backend thật (Issue #5):** form đăng ký kiểm tra tên 3–50 ký tự, email và mật khẩu 8–64 ký tự (tối đa 72 byte UTF-8) có chữ in hoa, chữ thường, chữ số, rồi gửi `POST /auth/register`. Liên kết trong email mở `/xac-minh-email?token=...`; trang gửi mã một lần, xóa mã khỏi thanh địa chỉ và báo kết quả. Mã sai/hết hạn cho phép yêu cầu gửi lại email; gửi lại trong 60 giây hiển thị số giây chờ từ header `Retry-After`.
- **Đăng nhập bằng email/mật khẩu đã gọi Backend thật (Issue #6):** gửi `POST /auth/login`; thành công thì lưu phiên (access token, thời điểm hết hạn, `account`) trong `sessionStorage` key `mamxanh.auth` của tab hiện tại và hiển thị tên tài khoản thật ở header. Lỗi hiển thị theo `code`: sai email/mật khẩu (thông báo trung tính), email chưa xác minh (kèm liên kết gửi lại email), tài khoản bị khóa, và khóa đăng nhập tạm 10 phút (số phút lấy từ `Retry-After`). Mật khẩu luôn được xóa khỏi state sau mỗi lần gửi.
- Nơi lưu token nằm trong một hàm duy nhất (`src/lib/authStorage.ts`). Axios tự gắn `Authorization: Bearer` cho request cần đăng nhập (không gắn cho `/auth/*`); Backend trả `401` hoặc `403 ACCOUNT_LOCKED` thì Frontend xóa phiên và đưa về trang Đăng nhập. Phiên tự kết thúc khi token hết hạn (không có refresh token), bị xóa khi tab đóng và không đồng bộ giữa các tab.
- Docker Development chạy thêm kiểm thử Playwright tích hợp: trình duyệt đăng nhập qua Frontend/Backend đang chạy và truy vấn SQL Server Compose để xác minh bộ đếm login sai, khóa lần thứ năm và reset sau khi hết hạn.
- **Onboarding và Sở thích ăn uống đã gọi Backend thật (Issue #36):** sau khi đăng nhập, Frontend gọi `POST /nutrition/dietary-preferences/onboarding/invitation`; chỉ khi Backend trả `show = true` (lần đầu của tài khoản mới chưa trả lời) Member mới được chuyển tới `/khoi-tao-so-thich` để chọn loại ăn chay, nguyên liệu cần tránh, món không thích (hoặc xác nhận "Không có") và sở thích tùy chọn. Rời trang mà chưa trả lời hoặc bấm "Bỏ qua" (ghi nhận `SKIPPED`) thì các lần đăng nhập sau không hỏi lại; trang vẫn mở thủ công được. Trang `/ho-so/so-thich-an-uong` (menu "Sở thích ăn uống") cho xem, cập nhật hồ sơ và cho biết còn thiếu thông tin nào trước khi dùng AI cá nhân hóa. Gợi ý tên lấy từ danh mục nguyên liệu chuẩn đang hoạt động.
- **Đăng nhập Google đã gọi Backend thật (Issue #8):** trang Đăng nhập và Đăng ký hiện nút "Tiếp tục với Google" của Google Identity Services (`@react-oauth/google`) khi có `VITE_GOOGLE_CLIENT_ID`; script Google chỉ được tải trên hai trang này. Google trả ID Token, Frontend gửi `POST /auth/google` rồi lưu phiên và kiểm tra lời mời Onboarding như khi đăng nhập bằng mật khẩu. Lỗi hiển thị theo `code`: token Google không hợp lệ (`GOOGLE_TOKEN_INVALID`), tài khoản bị khóa (`ACCOUNT_LOCKED`), email đã liên kết Google khác (`GOOGLE_ACCOUNT_CONFLICT`) và Google tạm không khả dụng (`GOOGLE_LOGIN_UNAVAILABLE`). Thiếu Client ID thì thay nút bằng thông báo; script Google không tải được thì vẫn đăng nhập bằng email được.
- **Đăng xuất** chỉ xóa token và trạng thái đăng nhập trên thiết bị, không gọi Backend (AC-03.13). Quên và đặt lại mật khẩu vẫn là biểu mẫu demo cho tới Issue #9.
- Không còn chế độ đổi vai trò/tài khoản giả trên Frontend. Dùng tài khoản local do Backend seed tạo sẵn; danh sách email và cách đặt chung mật khẩu nằm trong [Backend Workspace Guide](../mamxanh-backend/README.md#tài-khoản-demo-local). Mỗi vai trò phải đăng nhập bằng tài khoản riêng; quyền được Backend xác thực.
- Các luồng Backend hiện hiển thị từ API/SQL thật: khám phá và chi tiết công thức; tạo/sửa/quản lý công thức; lịch ăn tuần; hồ sơ dinh dưỡng; sở thích/onboarding; nộp và duyệt đơn Chuyên gia; danh mục nguyên liệu/đơn vị Admin. Trang khám phá chỉ cung cấp tìm kiếm từ khóa và phân trang vì đó là các tham số API hiện hỗ trợ.
- Lỗi từ API được đọc theo HTTP status và `code` của ProblemDetail ([API Guide](../../docs/api/API.md) mục 4), không phân tích câu chữ trong `detail`.

### Các chức năng demo khác

| Khu vực | Người dùng có thể thử | Giới hạn hiện tại |
|---|---|---|
| Trang chủ và thanh điều hướng | Logo dự án, menu desktop trên một hàng, tiêu đề hai dòng; menu thu gọn trên màn hình nhỏ | Danh mục, bài cộng đồng và banner AI vẫn là nội dung UI mẫu; công thức nổi bật lấy từ Backend |
| Bình luận công thức | Viết, trả lời, sửa và xóa bình luận; tối đa 5 cấp; xóa bình luận cha vẫn giữ trả lời | Chỉ giữ trong bộ nhớ khi trang còn mở, chưa gửi Backend |
| Khẩu phần tại công thức | Chọn 1–50 phần; nguyên liệu = lượng gốc / số phần gốc × số phần muốn nấu | Làm tròn tối đa 2 chữ số thập phân; lượng không đọc được thành số giữ nguyên; chưa đồng bộ Shopping List |
| Kế hoạch bữa ăn | Chọn 0,5–10 phần cho từng món, bước 0,5 | Chỉ cập nhật trạng thái UI; chưa lưu hoặc tổng hợp dinh dưỡng thật |
| Báo cáo công thức | Mở nút Báo cáo, chọn 1 trong 6 lý do; lý do Khác cần mô tả 10–500 ký tự | Nút chỉ kiểm tra biểu mẫu, chưa gửi cho quản trị viên |
| Dinh dưỡng công thức | Đọc rõ số liệu minh họa cho **1 khẩu phần** | 8 chỉ tiêu tĩnh, chưa tính từ nguyên liệu; còn thiếu so với FR-39, xem BUG-002 |
| Theo dõi dinh dưỡng, danh sách mua sắm, bài cộng đồng/bình luận/báo cáo, lưu/yêu thích công thức, gói AI và giao dịch | Tương tác UI minh họa theo từng màn hình | Các chức năng này chưa có API tương ứng; không ghi hoặc trình bày như dữ liệu Backend |

Các trang chưa có API được giữ làm prototype để minh họa phạm vi giao diện; chúng được ghi nhãn demo và không đại diện cho dữ liệu đã lưu trên Backend.

Các màn hình này chuẩn bị trải nghiệm cho FR-13, FR-20, FR-26/27, FR-35/38, FR-37/39 và FR-46; không xác nhận đã hoàn thành toàn bộ Acceptance Criteria của các FR. SRS vẫn là nguồn yêu cầu chính thức.

## Vercel cho Frontend

- Vercel là nền tảng hosting đã chốt cho Frontend production. Backend được triển khai riêng trên Azure App Service; Frontend gọi Backend qua HTTPS/REST API.
- Bản UI hiện tại được triển khai thủ công từ thư mục Frontend. Vercel gắn nhãn môi trường `Production` cho link demo, nhưng bản này vẫn dùng dữ liệu mẫu và chưa phải bằng chứng toàn hệ thống đã được triển khai.
- `vercel.json` chuyển các đường dẫn SPA về `index.html`, giúp mở trực tiếp hoặc tải lại trang con bằng React Router.
- Build dùng `npm run build`, đầu ra `dist/`. `.vercel/` là thông tin liên kết tài khoản/project local và được bỏ qua trong Git.
- Chưa thiết lập Vercel Git auto-deploy hoặc deployment workflow. GitHub test CI chạy verification; push code không tự cập nhật link Vercel, mỗi lần cập nhật demo vẫn cần deploy thủ công.
- Dockerfile Frontend dùng để đồng bộ môi trường development giữa các thành viên, không dùng để deploy Frontend lên Vercel.

---

## 1. Yêu cầu môi trường

- **Node.js**: Phiên bản `>= 20.x` (khuyến nghị `v24.x` hoặc `v22.x LTS`).
- **Package Manager**: `npm` (tiêu chuẩn của dự án).
- **IDE**: IntelliJ IDEA (Ultimate hoặc Community với Node.js/Web plugin) / WebStorm.

Nếu chạy bằng Docker, thành viên chỉ cần Docker Desktop đang hoạt động; không cần cài Node.js trực tiếp trên máy.

## Khởi động nhanh

Nếu cần mở cả hệ thống, dùng hai cửa sổ Terminal và khởi động theo thứ tự:

1. Microsoft SQL Server và database `MamXanhDB`.
2. [Backend](../mamxanh-backend/README.md) tại port `8080`.
3. Frontend tại port `5173`.

Frontend gọi Backend tại `VITE_API_BASE_URL`. Khi chạy trực tiếp, `/api/v1` được Vite proxy tới `http://localhost:8080`; trong Docker Compose, proxy dùng service `backend`. Để đổi base path, sao chép `.env.example` thành `.env.local` (đã được Git bỏ qua) rồi sửa giá trị; không đặt secret trong biến `VITE_` vì chúng nằm trong bundle công khai. Backend phải cho phép origin của Frontend qua `MAMXANH_CORS_ALLOWED_ORIGINS`.

Đăng nhập Google cần `VITE_GOOGLE_CLIENT_ID` (Client ID công khai, cùng giá trị với `MAMXANH_GOOGLE_CLIENT_ID` của Backend) và origin của Frontend (ví dụ `http://localhost:5173`) phải có trong mục Authorized JavaScript origins của OAuth client trên Google Cloud Console. Biến được gắn vào bundle lúc build. Bản build kiểm thử E2E (`npm run test:e2e:coverage`) tự đặt một Client ID giả và thay Google Identity Services bằng script stub, nên test không gọi Google; khi tự build rồi chạy Playwright, đặt `VITE_GOOGLE_CLIENT_ID` trước lệnh build.

Chỉ các luồng được liệt kê là đã kết nối mới là bằng chứng hiển thị dữ liệu Backend; các UI demo khác không chứng minh Backend hoặc database đã kết nối.

### Cách 1 — Chạy trực tiếp bằng Node.js

Mở Terminal tại `app/mamxanh-frontend` và chạy:

```powershell
npm ci
npm run dev
```

Mở <http://localhost:5173>. Vite theo dõi source và tự cập nhật trình duyệt khi code thay đổi.

Sau lần cài đầu tiên, các lần mở dự án tiếp theo chỉ cần:

```powershell
npm run dev
```

Luôn dùng `npm ci` khi cài mới từ `package-lock.json` hoặc khi dependency bị lệch. Không chia sẻ thư mục `node_modules` giữa các thành viên.

### Cách 2 — Chạy bằng Docker

Để chạy đồng bộ toàn bộ ứng dụng, dùng Docker Compose từ root repository theo hướng dẫn tại [Backend README](../mamxanh-backend/README.md#cách-2--chạy-bằng-docker). Cách chạy riêng Frontend bên dưới chỉ dành cho debug component; không thay thế kiểm thử tích hợp hoặc Docker Development gate. Quy tắc chung do [CONTRIBUTING.md](../../CONTRIBUTING.md#docker-development) quản lý.

Đảm bảo Docker Desktop đã khởi động, sau đó mở PowerShell tại `app/mamxanh-frontend`:

```powershell
docker build -t mamxanh-frontend-dev .

docker run --rm --name mamxanh-frontend `
  -p 127.0.0.1:5173:5173 `
  --mount "type=bind,source=$($PWD.Path),target=/workspace" `
  --mount "type=volume,source=mamxanh-frontend-node-modules,target=/workspace/node_modules" `
  mamxanh-frontend-dev
```

Mở <http://localhost:5173>. Bind mount đồng bộ source vào container; named volume giữ `node_modules` Linux tách khỏi máy Windows để tránh lỗi dependency native khác hệ điều hành.

Dừng bằng `Ctrl+C`. Container tự xóa vì dùng `--rm`; volume dependency được giữ lại để lần chạy sau nhanh hơn.

Với stack Compose chuẩn, khi `package.json` hoặc `package-lock.json` thay đổi, mở PowerShell tại root repository rồi refresh đúng named volume theo lockfile bằng one-off command (xem [hướng dẫn chuẩn](../../CONTRIBUTING.md#docker-development)); dùng cùng `-p <project>` với stack đang chạy:

```powershell
$composeProject = 'mamxanh-dev'
docker compose -p $composeProject --env-file app/mamxanh-backend/.env run --rm --no-deps frontend npm ci
```

Lệnh này không xóa hay reset SQL volume. Với lệnh standalone `docker run` ở trên, volume riêng tên `mamxanh-frontend-node-modules` không được Compose quản lý: dừng container, xóa riêng volume đó bằng `docker volume rm mamxanh-frontend-node-modules`, build lại image rồi chạy lại để khởi tạo dependency từ `package-lock.json` mới. Không dùng lệnh này để reset dữ liệu SQL của stack Compose.

Để chạy cả Frontend, Backend và SQL Server trong Compose chuẩn, dùng phần **Docker Compose** trong [Backend README](../mamxanh-backend/README.md). Compose nạp seed mẫu sau Flyway và giữ dữ liệu qua `down`/`up`; reset riêng SQL volume được thực hiện bằng script có xác nhận.

### Kiểm tra trước khi bàn giao code

```powershell
npm run lint
npm run build
```

Nếu dùng Docker và không cài Node.js trên máy, chạy kiểm tra trong image:

```powershell
docker run --rm mamxanh-frontend-dev npm run lint
docker run --rm mamxanh-frontend-dev npm run build
```

### Lỗi khởi động thường gặp

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `npm` không được nhận diện | Chưa cài Node.js hoặc Terminal chưa nạp lại `PATH` | Cài Node.js 22 LTS rồi mở Terminal mới, hoặc dùng Docker. |
| Port `5173` đã được sử dụng | Một Vite/container khác đang chạy | Dừng tiến trình cũ; không tự đổi port nếu nhóm đang dùng URL chuẩn `5173`. |
| Dependency/native binary lỗi sau khi đổi máy | `node_modules` được sao chép từ máy hoặc hệ điều hành khác hoặc dependency volume cũ sau khi lockfile đổi | Cài bằng `npm ci`; với Compose dùng one-off `run --rm --no-deps frontend npm ci`; chỉ troubleshooting mới xóa riêng Frontend dependency volume. |
| Docker không nhận lệnh | Docker Desktop chưa cài, chưa chạy hoặc chưa có trong `PATH` | Mở Docker Desktop và xác minh bằng `docker version`. |

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

Để chạy E2E cùng coverage hard gate, dùng `npm run test:e2e:coverage`. Playwright + Istanbul/NYC làm sạch coverage cũ, tạo build instrumentation riêng, chạy cùng suite E2E rồi xuất text summary, HTML, `coverage/lcov.info` và summary theo từng metric. **Lines, Statements, Functions và Branches đều phải >80%**; đúng 80% vẫn fail. Checker so sánh số đếm covered/total chính xác, không quyết định bằng phần trăm đã làm tròn. `npm run test:e2e:coverage:gate-tests` chạy regression tests cho ngưỡng strict. Nếu browser test fail thì command vẫn fail dù coverage đạt. Không hạ threshold theo baseline, exclude production source hoặc thêm test vô nghĩa để pass. Cài Chromium lần đầu bằng `npx playwright install chromium`; GitHub Actions cài thêm Linux dependencies.

Job `Sonar` chỉ đọc LCOV/JaCoCo XML sau khi tải artifacts từ Frontend/Backend; Sonar không tạo coverage. Policy yêu cầu các required checks pass trước merge vào `develop`, còn technical merge block phải được cấu hình ở [Ruleset](../../CONTRIBUTING.md#develop-required-checks). Tests hiện tại kiểm chứng demo UI/browser behavior, không chứng minh Backend/database E2E.

HTML report được tạo trong `playwright-report/`; screenshot và trace lỗi nằm trong `test-results/`. Hai thư mục này là generated evidence và không được commit mặc định.

Playwright hiện kiểm tra các tương tác UI prototype bằng API giả, các luồng API-backed với response được intercept, và tích hợp đăng nhập thật qua Docker Development trên Compose. Test file `backend-demo-flows.spec.ts` dùng `page.route` để kiểm tra session, dữ liệu công thức, loading, lỗi và empty state; đây là kiểm thử Frontend, không chứng minh SQL seed hoặc xác thực Backend. Chỉ Docker Development chạy trình duyệt qua Backend/SQL Server thật và có thể làm bằng chứng tích hợp FE–BE.
