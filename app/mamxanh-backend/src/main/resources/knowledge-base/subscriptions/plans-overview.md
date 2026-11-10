---
id: plans-overview
title: "So sánh các gói dịch vụ AI: Free, Plus và Pro"
category: subscriptions
audience:
  - guest
  - customer
  - expert
source_requirements:
  - FR-10
  - FR-13
  - BR-02
  - BR-78
source_use_cases:
  - UC-10.1
  - UC-10.2
  - UC-13.1
source_files:
  - app/mamxanh-frontend/src/pages/AiPlans.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/subscription/controller/SubscriptionController.java
implementation_status: verified
last_verified: 2026-11-01
---

# So sánh các gói dịch vụ AI: Free, Plus và Pro

## Chức năng này dùng để làm gì?

Mâm Xanh cung cấp các gói dịch vụ hội viên (**Subscription Tiers**) nhằm phục vụ nhu cầu tương tác và hỗ trợ từ Trợ lý AI ở các cấp độ khác nhau. Bạn có thể xem bảng so sánh chi tiết và lựa chọn nâng cấp tại trang **Gói dịch vụ AI** (`/goi-ai`).

Gói dịch vụ AI quyết định **hạn mức token**. Các gói thành viên dùng chung danh mục model AI khả dụng và phạm vi chức năng theo vai trò, điều kiện nghiệp vụ.

## Bảng so sánh chi tiết các gói

| Tiêu chí | Gói Miễn Phí (Free) | Gói Nâng Cao (Plus) | Gói Chuyên Nghiệp (Pro) |
|---|:---:|:---:|:---:|
| **Giá dịch vụ** | **0 đ** | **99.000 đ** / tháng (30 ngày) | **199.000 đ** / tháng (30 ngày) |
| **Hạn mức token** | **50.000 token / tháng** | **200.000 token / chu kỳ** | **500.000 token / chu kỳ** |
| **Quyền chọn mô hình AI** | Danh mục chung | Danh mục chung | Danh mục chung |
| **Khả năng nạp AI Credit** | Có | Có | Có |
| **Lưu công thức, Lịch ăn, Đi chợ** | Miễn phí trọn đời | Miễn phí trọn đời | Miễn phí trọn đời |
| **Xem phân tích dinh dưỡng** | Đầy đủ | Đầy đủ | Đầy đủ |
| **Tự động gia hạn** | Không | Không tự động trừ tiền | Không tự động trừ tiền |

## Chi tiết từng gói dịch vụ

### 1. Gói Miễn Phí (Free Tier)
- **Đối tượng:** Mọi thành viên vừa đăng ký tài khoản Mâm Xanh.
- **Quyền lợi:** Nhận ngay 50.000 token miễn phí mỗi tháng dương lịch (được làm mới vào ngày mùng 1 hàng tháng theo giờ Việt Nam).
- **Mô hình AI:** Chọn từ danh mục model thành viên đang khả dụng trong phần Cài đặt chat.

### 2. Gói Nâng Cao (Plus Tier)
- **Giá:** 99.000 đ / chu kỳ 30 ngày.
- **Quyền lợi:** Nhận 200.000 token trong chu kỳ.
- **Mô hình AI:** Chọn từ cùng danh mục model thành viên.

### 3. Gói Chuyên Nghiệp (Pro Tier)
- **Giá:** 199.000 đ / chu kỳ 30 ngày.
- **Quyền lợi:** Nhận 500.000 token trong chu kỳ.
- **Mô hình AI:** Chọn từ cùng danh mục model thành viên.

## Những điều cần lưu ý

1. **Gói AI KHÔNG khóa tính năng cơ bản của website:**
   - Việc bạn ở gói Free, Plus hay Pro **hoàn toàn không làm hạn chế** các tính năng cốt lõi như: Tìm kiếm món, xem công thức, bình luận, bình chọn Like/Dislike, lập thực đơn tuần thủ công hay tạo danh sách đi chợ. Mọi tính năng nghiệp vụ này đều mở miễn phí trọn đời cho mọi thành viên.
2. **Gói AI KHÔNG thay đổi vai trò Chuyên gia:**
   - Việc mua gói Pro không tự biến tài khoản người dùng thành Chuyên gia (Expert). Quyền đăng bài công thức vẫn tuân theo quy trình xét duyệt đơn Chuyên gia độc lập.
3. **Không tự động gia hạn (Không lo trừ tiền âm thầm):**
   - Mâm Xanh tôn trọng người dùng và **hoàn toàn không áp dụng cơ chế tự động trừ tiền trong tài khoản ngân hàng** khi hết chu kỳ 30 ngày. Khi gói hết hạn, tài khoản của bạn sẽ tự động quay trở về gói Free bình thường; bạn chỉ thanh toán khi chủ động muốn gia hạn tiếp.
4. **Token trong gói không cộng dồn:**
   - Hạn mức token của gói theo tháng không được cộng dồn sang chu kỳ tiếp theo nếu chưa dùng hết. Tuy nhiên, nếu bạn nạp thêm **AI Credit**, số dư Credit sẽ được bảo lưu vĩnh viễn không bao giờ hết hạn.
