> **Document:** Vegetarian Support System Project Overview
> **File:** `README.md`
> **Version:** v0.7.1
> **Created:** 2026-06-14
> **Last Updated:** 2026-09-16
> **Status:** Under Review

# Vegetarian Support System

Ứng dụng web hỗ trợ người ăn chay khám phá bài công thức, lập thực đơn tuần và nhận gợi ý từ Gemini dựa trên nội dung đang công khai trong hệ thống. Đây là dự án môn SWP391 do nhóm 5 thành viên phát triển.

> Trạng thái hiện tại: đang chuẩn hóa yêu cầu và môi trường làm việc. SRS vẫn là bản nháp; source code, database schema và hướng dẫn chạy chưa được triển khai trong repository. Nội dung tài liệu không phải bằng chứng tính năng đã hoạt động.

## Phạm vi MVP

- Guest đọc/tìm bài công thức công khai, xem bình luận và dùng AI hỏi đáp cơ bản theo hạn mức.
- Member quản lý hồ sơ, lưu công thức, lập lịch ăn ba bữa, Shopping List và tương tác Like/bình luận. Nearby Restaurant Discovery/Google Maps (M11) là `OUT_OF_SCOPE` và không thuộc baseline triển khai hiện tại.
- Member đã đăng nhập có thể tạo và công khai Recipe Post trực tiếp; Administrator hậu kiểm nội dung theo báo cáo của người dùng.
- Gemini hỗ trợ hỏi đáp, gợi ý công thức có sẵn, lập/thay thực đơn và tạo nội dung có thể chỉnh sửa trong biểu mẫu; AI không tự tạo dữ liệu dinh dưỡng chính thức hoặc tự quyết định kiểm duyệt. Lưu nháp Recipe Post, lịch sử chat AI và AI quét/gắn cờ nội dung không thuộc MVP hiện tại.
- Free, Plus và Pro có cùng nhóm chức năng AI, với hạn mức 5/15/50 request thành công/ngày. Giá tháng cố định cho MVP là FREE 0 VND, PLUS 49,000 VND và PRO 99,000 VND; không tự động gia hạn hoặc hoàn tiền một phần, entitlement chỉ có hiệu lực sau thanh toán được xác minh và hết hạn cuối kỳ đã trả.

Chi tiết nghiệp vụ nằm trong [SRS](docs/requirements/SRS.md). Các đề xuất hoặc tài liệu nghiên cứu không tự trở thành yêu cầu nếu chưa được ghi nhận trong SRS.

## Công nghệ baseline

| Khu vực | Lựa chọn hiện tại |
|---|---|
| Frontend | React, TypeScript, Vite, npm, Axios |
| Backend | Java 21, Spring Boot, Maven, REST API/JSON |
| Data | Microsoft SQL Server, Spring Data JPA/Hibernate, Flyway |
| Security | Spring Security, short-lived JWT access token, rotating refresh token với server-side revocation, BCrypt, role-based authorization |
| External services | Azure Blob Storage, Google Gemini và YouTube embedding; payment/email provider cụ thể còn chọn khi tích hợp. Google Maps không phải dependency của baseline hiện tại vì M11 là `OUT_OF_SCOPE`. |
| Quality | JUnit 5, Mockito, JaCoCo, OpenAPI/Swagger UI, Bean Validation |

Model Gemini, AI architecture, frontend state management, CSS/UI library và deployment vẫn là `TBD`. Xem [Technology Stack](docs/architecture/TECHNOLOGY-STACK.md) trước khi thêm dependency và [System Architecture](docs/architecture/ARCHITECTURE.md) trước khi thay đổi ranh giới hệ thống.

## Cấu trúc repository

```text
.
├── app/
│   ├── frontend/
│   └── backend/
├── database/
├── docs/
│   ├── requirements/
│   ├── architecture/
│   ├── testing/
│   ├── decisions/
│   ├── diagrams/
│   └── research/
├── .github/
├── .agents/
├── AGENTS.md
├── CONTRIBUTING.md
└── CHANGELOG.md
```

`app/frontend`, `app/backend` và các SQL file hiện là điểm giữ chỗ có chủ đích. Không ghi hướng dẫn chạy giả định cho đến khi scaffold thật và các lệnh đã được kiểm tra.

## Nguồn tài liệu

| Nội dung | Nguồn chính | Trạng thái |
|---|---|---|
| Yêu cầu chi tiết | [SRS](docs/requirements/SRS.md) | Requirements Baseline v1.0.0 — Active |
| Yêu cầu sản phẩm cấp cao | [PRD](docs/requirements/PRD.md) | Requirements Baseline v1.0.0 — Active |
| Kiến trúc cấp cao | [System Architecture](docs/architecture/ARCHITECTURE.md) | Active; chưa phải bằng chứng implementation |
| Công nghệ | [Technology Stack](docs/architecture/TECHNOLOGY-STACK.md) | Active baseline |
| Chiến lược kiểm thử | [Test Strategy](docs/testing/TEST-STRATEGY.md) | Active; chưa khẳng định test đã tồn tại |
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
5. Mở PR vào `develop` bằng `Refs #<issue>`; tác giả không tự review/approve thay cho reviewer.
6. Issue ở `Review` cho đến khi release PR từ `develop` vào `main` được merge.

Tại lần kiểm tra local ngày 2026-09-12, remote-tracking refs chưa có `develop`; đây là bước thiết lập còn lại, không được coi là đã cấu hình chỉ vì tài liệu mô tả workflow.

## Trạng thái chạy và kiểm thử

Chưa có source code hoặc build manifest (`pom.xml`, `package.json`) để cung cấp lệnh cài đặt, build hay test đã được xác minh. Khi scaffold được thêm, README này và README của từng ứng dụng phải được cập nhật bằng các lệnh đã chạy thành công trên repository.

## Thành viên

Nhóm gồm 5 thành viên. Tên tài khoản GitHub, phạm vi chính và người backup được quản lý trong GitHub Project/Team, không hard-code khi chưa được nhóm xác nhận. Trách nhiệm chung và cơ chế ra quyết định nằm trong [ADR-002](docs/decisions/002-five-member-team-operating-agreement.md).

## License

Xem [LICENSE](LICENSE).
