---
id: plans-overview
title: Gói dịch vụ và hạn mức AI
category: subscriptions
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-10
  - FR-13
  - FR-63
  - BR-02
  - BR-78
source_use_cases:
  - UC-10.1
  - UC-10.2
  - UC-13.1
source_files:
  - app/mamxanh-frontend/src/pages/AiPlans.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/admin/service/AdminAiConfigurationService.java
  - app/mamxanh-backend/src/main/java/tech/mamxanh/subscription/service/AiTokenQuotaService.java
implementation_status: verified
last_verified: 2026-11-21
---

# Gói dịch vụ và hạn mức AI

## Gói AI dùng để làm gì?

Gói dịch vụ ảnh hưởng đến quota, model AI được phép sử dụng và một số tính năng có điều kiện theo gói. Gói AI không tự thay đổi vai trò tài khoản: nâng cấp gói không biến Customer thành Expert và không cấp quyền đăng công thức.

## Xem giá và quyền lợi hiện hành

Giá, quota, danh sách model và một số giới hạn được quản trị viên cấu hình. Vì vậy, không nên dựa vào con số giá hoặc token cố định trong một câu trả lời cũ. Hãy xem trang **Gói dịch vụ AI** (`/goi-ai`) và phần sử dụng AI của tài khoản để biết lựa chọn, thời hạn, quota và Credit đang áp dụng.

Nếu Trợ lý không có công cụ đọc cấu hình hiện hành, Trợ lý nên hướng dẫn bạn mở trang trên thay vì tự khẳng định giá, số token, model hoặc quota còn lại.

## Quota, AI Credit và model

- Mỗi yêu cầu AI chịu hạn mức của kỳ hiện tại. Quota theo gói và model được phép có thể khác nhau theo cấu hình tài khoản.
- AI Credit có thể được dùng theo quy tắc hiện hành khi quota gói không còn. Số dư thật là dữ liệu tài khoản động; cần đọc từ trang hoặc công cụ Backend phù hợp.
- Nếu hết quota hoặc không còn Credit, yêu cầu có thể bị từ chối cho đến khi kỳ quota được làm mới, có Credit khả dụng hoặc gói được thay đổi.
- Không kết luận rằng mọi tính năng AI đều có cùng điều kiện gói. Điều kiện truy cập thuộc nghiệp vụ của từng tính năng.

## Gói AI và thực đơn dinh dưỡng

FR-36 xác định gói dịch vụ không phải điều kiện nghiệp vụ để truy cập chức năng tạo thực đơn dinh dưỡng. Yêu cầu vẫn phải đáp ứng hồ sơ dinh dưỡng, eligibility và quota/model hiện hành. Xem [AI tạo thực đơn dinh dưỡng](../ai-features/ai-meal-recommendations.md) để biết luồng cụ thể.

## Vai trò tài khoản

- Free, Plus và Pro là cấp gói dịch vụ AI, không phải vai trò người dùng.
- Customer và Expert có quyền nghiệp vụ khác nhau; quyền đăng và quản lý công thức vẫn theo vai trò Expert.
- Giá và nội dung các gói có thể được cập nhật; nội dung trang gói đang hiển thị là thông tin cần dùng khi tư vấn người dùng.
