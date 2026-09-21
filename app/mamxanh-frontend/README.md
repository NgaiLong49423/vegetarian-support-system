> **Document:** Frontend Workspace Guide (Mâm Xanh)  
> **File:** `app/mamxanh-frontend/README.md`  
> **Version:** v1.1.0
> **Created:** 2026-09-18  
> **Last Updated:** 2026-09-20
> **Status:** Active  

# Mâm Xanh Frontend

Thư mục mã nguồn giao diện ứng dụng Mâm Xanh được xây dựng bằng **React 19**, **TypeScript**, **Vite 8**, **Tailwind CSS v4** và xuất phát từ thiết kế Figma Make.

Dự án đã được cấu hình tối ưu để mở, chỉnh sửa, chạy và gỡ lỗi trực tiếp trên **IntelliJ IDEA** (hoặc WebStorm/VS Code).

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

Test đầu tiên là **Frontend browser smoke test** trên giao diện hiện dùng mock data. Kết quả pass chỉ chứng minh application shell và navigation được kiểm tra hoạt động trong browser; nó không chứng minh Backend, database, authentication hoặc full FE–BE E2E đã hoạt động.
