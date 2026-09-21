> **Document:** Mâm Xanh — Vegetarian Support System Project Overview
> **File:** `README.md`
> **Version:** v0.10.0
> **Created:** 2026-06-14
> **Last Updated:** 2026-09-20
> **Status:** Active

# Mâm Xanh — Vegetarian Support System

<p align="center">
  <img src="image/logo.png" alt="Mâm Xanh Logo" width="200" />
</p>

**Mâm Xanh** (Vegetarian Support System) là ứng dụng web hỗ trợ người ăn chay khám phá bài công thức, lập thực đơn tuần và nhận gợi ý thông minh từ Gemini AI dựa trên nội dung đang công khai trong hệ thống. Đây là dự án môn SWP391 do nhóm 5 thành viên phát triển.

> **Trạng thái hiện tại:** Backend và Frontend đã được scaffold. Backend dùng Java 21 + Spring Boot + Maven và đã có cấu hình local SQL Server theo mẫu; Frontend dùng React + Vite + TypeScript và đã có Playwright Chromium smoke test. API Authentication và cấu trúc package Backend hiện mới là tài liệu đề xuất/contract dưới review, chưa phải bằng chứng chức năng đã được triển khai.

## Phạm vi MVP

- Guest đọc/tìm bài công thức công khai, xem bình luận và dùng AI hỏi đáp cơ bản theo hạn mức.
- Member quản lý hồ sơ, lưu công thức, lập lịch ăn ba bữa, Shopping List, đánh giá sao và bình luận/reply. Chức năng Like đã `RETIRED`; Nearby Restaurant Discovery/Google Maps (M11) là `OUT_OF_SCOPE` và không thuộc baseline triển khai hiện tại.
- Member đã đăng nhập có thể tạo và công khai Recipe Post trực tiếp; Administrator hậu kiểm nội dung theo báo cáo của người dùng.
- Gemini hỗ trợ hỏi đáp, gợi ý công thức có sẵn, lập/thay thực đơn và tạo nội dung có thể chỉnh sửa trong biểu mẫu; AI không tự tạo dữ liệu dinh dưỡng chính thức hoặc tự quyết định kiểm duyệt. Lưu nháp Recipe Post, lịch sử chat AI và AI quét/gắn cờ nội dung không thuộc MVP hiện tại.
- Free, Plus và Pro có cùng nhóm chức năng AI, với hạn mức 5/15/50 request thành công/ngày. Giá tháng cố định cho MVP là FREE 0 VND, PLUS 49,000 VND và PRO 99,000 VND; không tự động gia hạn hoặc hoàn tiền một phần, entitlement chỉ có hiệu lực sau thanh toán được xác minh và hết hạn cuối kỳ đã trả.
Chi tiết nghiệp vụ nằm trong [SRS](docs/requirements/SRS.md). Các đề xuất hoặc tài liệu nghiên cứu không tự trở thành yêu cầu nếu chưa được ghi nhận trong SRS.

## Công nghệ baseline

| Khu vực | Lựa chọn hiện tại (Confirmed) |
|---|---|
| Frontend | React, TypeScript, Vite, npm, Axios, Requestly Pro (FE dev mocking) |
| Backend | Java 21, Spring Boot, Maven, REST API/JSON, Spring Boot Actuator |
| Data | Microsoft SQL Server (Azure SQL Database Serverless), Spring Data JPA/Hibernate, Flyway |
| Security | Spring Security, Google Identity Services (GIS), `GoogleIdTokenVerifier`, short-lived JWT access token, rotating refresh token (HttpOnly cookie), BCrypt, role-based authorization |
| External services | Google Gemini (`gemini-3.8-flash` qua Google Gen AI Java SDK), payOS (VietQR Payment REST & Webhook), Brevo (Transactional Email SMTP), Azure Blob Storage, YouTube embedding |
| Quality & DevOps | JUnit 5, Mockito, Playwright, JaCoCo, Codecov (CI coverage), Testmail (Email E2E testing), OpenAPI/Swagger UI, Bean Validation, Azure Application Insights |
| Deployment | Azure Static Web Apps (FE) + Azure App Service (BE) + Azure SQL Serverless (DB) + Azure Blob (Media) + Custom Domain (.tech) |

Xem chi tiết trong [Technology Stack](docs/architecture/TECHNOLOGY-STACK.md) và [System Architecture](docs/architecture/ARCHITECTURE.md).

## Cấu trúc repository

```text
.
├── app/
│   ├── mamxanh-backend/
│   │   ├── src/main/java/tech/mamxanh/
│   │   ├── src/main/resources/
│   │   ├── src/test/java/tech/mamxanh/
│   │   ├── pom.xml
│   │   ├── mvnw
│   │   └── mvnw.cmd
│   └── mamxanh-frontend/
│       ├── src/
│       ├── public/
│       ├── tests/e2e/
│       ├── package.json
│       ├── package-lock.json
│       ├── playwright.config.ts
│       └── vite.config.ts
├── database/
│   ├── schema.sql
│   ├── sample-data.sql
│   └── queries.sql
├── docs/
│   ├── api/
│   ├── architecture/
│   ├── decisions/
│   ├── diagrams/
│   ├── requirements/
│   ├── research/
│   └── testing/
├── image/
├── .github/
├── .agents/
│   ├── skills/
│   └── outputs/
├── AGENTS.md
├── CONTRIBUTING.md
└── CHANGELOG.md
```

Các file SQL trong `database/` hiện là điểm giữ chỗ có chủ đích. Flyway migration executable sẽ nằm trong `app/mamxanh-backend/src/main/resources/db/migration/` sau khi physical schema được duyệt. `node_modules/`, `dist/`, `target/`, cấu hình local chứa credential và test report sinh ra không được trình bày trong cây trên vì không phải source được Git theo dõi.

## Nguồn tài liệu

| Nội dung | Nguồn chính | Trạng thái |
|---|---|---|
| Yêu cầu chi tiết | [SRS](docs/requirements/SRS.md) | Requirements Baseline v1.0.0 — Active |
| Yêu cầu sản phẩm cấp cao | [PRD](docs/requirements/PRD.md) | Requirements Baseline v1.0.0 — Active |
| Kiến trúc cấp cao | [System Architecture](docs/architecture/ARCHITECTURE.md) | Active; chưa phải bằng chứng implementation |
| Công nghệ | [Technology Stack](docs/architecture/TECHNOLOGY-STACK.md) | Active baseline |
| API integration | [API Guide](docs/api/API.md) và [OpenAPI](docs/api/openapi.yaml) | Authentication slice — Under Review; chưa phải bằng chứng implementation |
| Đề xuất package Backend | [Backend Package Structure Proposal](docs/architecture/BACKEND-PACKAGE-STRUCTURE-PROPOSAL.md) | Draft; chỉ dùng để định hướng vertical slice |
| Chiến lược kiểm thử | [Test Strategy](docs/testing/TEST-STRATEGY.md) | Active; Frontend đã có Playwright smoke test, phạm vi khác theo bằng chứng triển khai |
| Quy trình Git/PR/release | [CONTRIBUTING.md](CONTRIBUTING.md) | Active |
| Quyết định workflow | [ADR-001](docs/decisions/001-team-workflow.md) | Active |
| Luật vận hành nhóm 5 người | [ADR-002](docs/decisions/002-five-member-team-operating-agreement.md) | Active |
| Nhật ký thay đổi | [CHANGELOG.md](CHANGELOG.md) | Active |

Quy tắc đặt file và danh mục tài liệu được duy trì nằm trong [Repository Layout and Document Register](docs/README.md). Agent mới bắt đầu tại [AGENTS.md](AGENTS.md); chỉ đọc tài liệu theo nhiệm vụ. Tiến độ, owner và blocker được quản lý trên GitHub Issues/Projects.

## Bắt đầu làm việc

1. Đọc SRS, Technology Stack, tài liệu liên quan trực tiếp tới task, `CONTRIBUTING.md` và ADR-002.
2. Chọn Issue đã đạt Definition of Ready; mỗi Issue có đúng một owner.
3. Tạo branch từ `develop` theo dạng `<type>/<issue-number>-<short-name>`.
4. Thực hiện một phạm vi nhỏ, tự kiểm tra và cập nhật tài liệu liên quan.
5. Mở PR vào `develop` bằng `Refs #<issue>` và ghi kết quả tự kiểm tra; owner vẫn phải chạy kiểm tra phù hợp dù approval/GitHub Actions chưa phải cổng bắt buộc ở nhánh này.
6. Tech Lead chọn phần ổn định để mở PR `develop -> main` cuối tuần; cần approval độc lập và required CI checks. Issue chỉ `Done` sau kiểm tra demo local trên `main` đạt. Xem [workflow chính](CONTRIBUTING.md#workflow-làm-việc-nhóm).

Hiện phát triển/demo trên local; deployment Azure thực hiện riêng khi nhóm xác nhận ứng dụng đủ ổn định. Chưa bật CD trong giai đoạn này.

## Trạng thái chạy và kiểm thử

Hai workspace đã có build manifest và hướng dẫn riêng:

- Backend: [app/mamxanh-backend/README.md](app/mamxanh-backend/README.md)
- Frontend: [app/mamxanh-frontend/README.md](app/mamxanh-frontend/README.md)

Các lệnh kiểm tra chính trên Windows:

```powershell
# Backend
cd app/mamxanh-backend
.\mvnw.cmd test

# Frontend
cd ../mamxanh-frontend
npm ci
npm run lint
npm run build
npm run test:e2e
```

`npm run test:e2e` tự build Frontend, khởi động Vite preview trên port cố định `4173` và chạy Playwright bằng Chromium. Kết quả của một lần chạy local không thay thế CI hoặc bằng chứng nghiệm thu cho một FR cụ thể.

## Thành viên

Dự án được phát triển bởi nhóm 5 thành viên (SWP391):

<div align="center">
  <table>
    <tr>
      <td align="center" width="20%">
        <a href="https://github.com/NgaiLong49423">
          <img src="https://github.com/NgaiLong49423.png?size=100" width="80" height="80" alt="Ngo Gia Long" style="border-radius: 50%;" /><br />
          <sub><b>Ngo Gia Long</b></sub>
        </a><br />
        <sub><a href="https://github.com/NgaiLong49423">@NgaiLong49423</a></sub>
      </td>
      <td align="center" width="20%">
        <a href="https://github.com/TonyDuongg">
          <img src="https://github.com/TonyDuongg.png?size=100" width="80" height="80" alt="Nguyen Hai Duong" style="border-radius: 50%;" /><br />
          <sub><b>Nguyen Hai Duong</b></sub>
        </a><br />
        <sub><a href="https://github.com/TonyDuongg">@TonyDuongg</a></sub>
      </td>
      <td align="center" width="20%">
        <a href="https://github.com/ngocthom2212">
          <img src="https://github.com/ngocthom2212.png?size=100" width="80" height="80" alt="Dương Thị Ngọc Thơm" style="border-radius: 50%;" /><br />
          <sub><b>Dương Thị Ngọc Thơm</b></sub>
        </a><br />
        <sub><a href="https://github.com/ngocthom2212">@ngocthom2212</a></sub>
      </td>
      <td align="center" width="20%">
        <a href="https://github.com/Gao2511">
          <img src="https://github.com/Gao2511.png?size=100" width="80" height="80" alt="Gao2511" style="border-radius: 50%;" /><br />
          <sub><b>Gao2511</b></sub>
        </a><br />
        <sub><a href="https://github.com/Gao2511">@Gao2511</a></sub>
      </td>
      <td align="center" width="20%">
        <a href="https://github.com/onlyKaizz">
          <img src="https://github.com/onlyKaizz.png?size=100" width="80" height="80" alt="Truong Van Khai" style="border-radius: 50%;" /><br />
          <sub><b>Truong Van Khai</b></sub>
        </a><br />
        <sub><a href="https://github.com/onlyKaizz">@onlyKaizz</a></sub>
      </td>
    </tr>
  </table>
</div>

Phạm vi chính, phân công công việc và người backup được quản lý trong [GitHub Project #15 (Vegetarian Support System — SWP391)](https://github.com/users/NgaiLong49423/projects/15). Trách nhiệm chung và cơ chế ra quyết định nằm trong [ADR-002](docs/decisions/002-five-member-team-operating-agreement.md).

## License

Xem [LICENSE](LICENSE).
