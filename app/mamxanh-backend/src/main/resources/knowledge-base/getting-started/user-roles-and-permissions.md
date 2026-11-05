---
id: user-roles-and-permissions
title: Các vai trò người dùng và phân quyền trên Mâm Xanh
category: getting-started
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-01
  - FR-03
  - FR-04
  - FR-05
  - FR-06
  - BR-05
  - BR-07
source_use_cases:
  - UC-01.1
  - UC-03.4
  - UC-04.1
  - UC-05.1
source_files:
  - docs/requirements/srs/FUNCTIONAL-REQUIREMENTS.md
  - docs/requirements/srs/BUSINESS-RULES.md
  - app/mamxanh-backend/src/main/java/tech/mamxanh/auth/entity/User.java
implementation_status: verified
last_verified: 2026-11-01
---

# Các vai trò người dùng và phân quyền trên Mâm Xanh

## Chức năng này dùng để làm gì?

Mâm Xanh phân định rõ quyền hạn của từng nhóm người dùng nhằm bảo đảm tính toàn vẹn chuyên môn của nội dung ẩm thực, đồng thời bảo vệ dữ liệu cá nhân của mỗi thành viên.

Hệ thống có 4 vai trò chính:
1. **Khách vãng lai (Guest)**
2. **Thành viên thông thường (Customer / Member)**
3. **Chuyên gia ẩm thực chay (Expert)**
4. **Quản trị viên (Administrator)**

## Bảng so sánh quyền hạn chi tiết

| Tính năng | Khách (Guest) | Thành viên (Customer) | Chuyên gia (Expert) |
|---|:---:|:---:|:---:|
| Xem công thức công khai, tỷ lệ % Like, lượt xem | Có | Có | Có |
| Tìm kiếm, lọc và so sánh 2 công thức | Có | Có | Có |
| Sử dụng AI Chatbot trải nghiệm | Có (tối đa 20.000 token/phiên) | Có (theo gói dịch vụ) | Có (theo gói dịch vụ) |
| Bình chọn Like / Dislike bài viết | Không | Có | Có (trừ bài của mình) |
| Viết bình luận, phản hồi thảo luận | Không | Có | Có |
| Lưu công thức yêu thích (`Saved Recipe`) | Không | Có | Có |
| Lập thực đơn tuần (`Meal Plan`) | Không | Có | Có |
| Tạo và quản lý Danh sách đi chợ | Không | Có | Có |
| Theo dõi dinh dưỡng cá nhân | Không | Có | Có |
| Theo dõi (`Follow`) thành viên khác | Không | Có | Có |
| Báo cáo nội dung có vấn đề | Không | Có | Có |
| Nộp đơn xin làm Chuyên gia | Không | Có | Đã là Chuyên gia |
| **Đăng và quản lý bài công thức công khai** | **Không** | **Không** | **Có** |
| Sử dụng AI hỗ trợ soạn bài công thức | Không | Không | Có |

## Chi tiết từng vai trò

### 1. Khách vãng lai (Guest)
- **Đặc điểm:** Người truy cập chưa đăng nhập tài khoản.
- **Quyền hạn:** Khám phá toàn bộ bài công thức đã công khai, xem danh bạ chuyên gia, xem danh mục nguyên liệu, tra cứu thông tin dinh dưỡng công thức, sử dụng tính năng so sánh 2 món ăn và trải nghiệm AI Chatbot với hạn mức kỹ thuật (20.000 token mỗi phiên trình duyệt).
- **Giới hạn:** Không có dữ liệu cá nhân lưu trữ; không thể lưu món, lên lịch ăn, đi chợ, bình luận hay bình chọn Like/Dislike.

### 2. Thành viên thông thường (Customer)
- **Đặc điểm:** Người dùng đã đăng ký tài khoản và xác minh email.
- **Quyền hạn:** Sở hữu toàn bộ tính năng quản lý cá nhân (Thực đơn tuần, Danh sách mua sắm, Công thức đã lưu, Theo dõi dinh dưỡng), bình chọn Like/Dislike, gửi bình luận nhiều cấp, theo dõi chuyên gia yêu thích và nộp đơn đăng ký nâng cấp thành Chuyên gia.
- **Quy tắc quan trọng:** Thành viên thông thường **không có quyền tự ý tạo hoặc đăng bài công thức lên hệ thống**. Quy định này nhằm giữ vững chất lượng chuyên môn cho toàn bộ công thức trên nền tảng.

### 3. Chuyên gia ẩm thực chay (Expert)
- **Đặc điểm:** Thành viên được Ban quản trị phê duyệt đơn đăng ký hoặc chỉ định trực tiếp nhờ uy tín và kinh nghiệm ẩm thực chay.
- **Quyền hạn đặc thù:** Là **chủ thể duy nhất được quyền tạo, chỉnh sửa, xóa và công khai bài công thức** trên Mâm Xanh. Có trang hồ sơ chuyên gia hiển thị huy hiệu xác nhận, danh sách công thức đã đóng góp và tổng lượt thích/xem. Được sử dụng AI để hỗ trợ soạn thảo lời giới thiệu và hướng dẫn chế biến.
- **Giới hạn:** Chuyên gia không được tự bình chọn Like hoặc Dislike cho bài viết do chính mình sáng tác.

### 4. Quản trị viên (Administrator)
- **Đặc điểm:** Nhân sự quản trị và vận hành hệ thống.
- **Quyền hạn:** Quản lý từ điển nguyên liệu, đơn vị đo lường và tỷ lệ quy đổi; thẩm định đơn đăng ký Chuyên gia; tiếp nhận và xử lý các báo cáo vi phạm nội dung; quản lý tài khoản thành viên; cấu hình gói dịch vụ AI và bảng giá.
- **Giới hạn:** Quản trị viên không tham gia viết hay đăng bài công thức; không sửa chữa trực tiếp nội dung bài viết của Chuyên gia.

## Những điều cần lưu ý

1. **Gói dịch vụ AI (Free, Plus, Pro) không thay đổi vai trò tài khoản:** Việc nâng cấp gói Plus hoặc Pro chỉ mở rộng hạn mức token và cho phép lựa chọn mô hình Gemini cao cấp hơn, không biến tài khoản Customer thành Expert và không cấp thêm quyền đăng bài.
2. **Quyền tác giả độc lập:** Mỗi Chuyên gia chỉ có quyền chỉnh sửa hoặc xóa bài viết do chính tài khoản của mình tạo ra.
