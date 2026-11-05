# Kho Tri Thức Nghiệp Vụ Mâm Xanh — Knowledge Base cho AI Assistant

> **Phiên bản:** v1.0.0  
> **Ngày thiết lập:** 2026-11-01  
> **Trạng thái:** Sẵn sàng cho AI Assistant và RAG  
> **Ngôn ngữ:** Tiếng Việt  

---

## 1. Giới thiệu và Mục đích

**Mâm Xanh Knowledge Base** là kho tri thức nghiệp vụ có cấu trúc, chính xác và thân thiện với người dùng, được chuyển đổi từ toàn bộ hệ thống tài liệu đặc tả kỹ thuật (SRS, FR, BR, Use Cases, NFR) và mã nguồn thực tế của dự án Mâm Xanh (Vegetarian Support System).

Kho tri thức này được thiết kế chuyên biệt để:
- Phục vụ **Trợ lý ảo AI Assistant** của Mâm Xanh tra cứu, giải thích và hướng dẫn người dùng sử dụng hệ thống như một nhân viên hỗ trợ am hiểu sản phẩm.
- Giải thích các tính năng, quyền hạn, chính sách và cách xử lý sự cố bằng **ngôn ngữ tự nhiên, dễ hiểu**, loại bỏ hoàn toàn các thuật ngữ kỹ thuật nội bộ (như FR, BR, UC, AC, Validation, Entitlement).
- Cung cấp nguồn dữ liệu chuẩn hóa, sẵn sàng tích hợp với Google ADK hoặc các hệ thống RAG (Retrieval-Augmented Generation) và Vector Search trong tương lai.

---

## 2. Cấu trúc thư mục

Kho tri thức được tổ chức theo chức năng và hành trình trải nghiệm của người dùng:

```text
knowledge-base/
├── README.md                                  # Mục lục và hướng dẫn quản lý kho tri thức
├── KNOWLEDGE-BASE-AUDIT.md                    # Báo cáo kiểm định chất lượng toàn diện
│
├── getting-started/                           # Bắt đầu với Mâm Xanh
│   ├── introduction.md                        # Giới thiệu Mâm Xanh, sứ mệnh và giá trị cốt lõi
│   ├── user-roles-and-permissions.md          # Các vai trò (Guest, Customer, Expert, Admin) và phân quyền
│   └── onboarding-guide.md                    # Khởi tạo sở thích ăn uống ban đầu (Onboarding)
│
├── accounts/                                  # Tài khoản & Hồ sơ cá nhân
│   ├── registration-and-login.md              # Đăng ký, đăng nhập và bảo mật tài khoản
│   ├── password-recovery.md                   # Quên mật khẩu và khôi phục tài khoản
│   ├── profile-management.md                  # Quản lý thông tin hồ sơ và cài đặt tài khoản
│   ├── following-members.md                   # Theo dõi Chuyên gia và thành viên khác
│   └── expert-registration.md                 # Quy trình nộp đơn đăng ký làm Chuyên gia ẩm thực chay
│
├── recipes/                                   # Khám phá & Công thức món chay
│   ├── search-and-filter.md                   # Tìm kiếm và lọc công thức món chay
│   ├── sorting-modes.md                       # 6 chế độ sắp xếp công thức (Mới nhất, % Like, Trending...)
│   ├── recipe-details-and-ratings.md          # Chi tiết công thức và đánh giá Like / Dislike (% Like)
│   ├── recipe-comparison.md                   # So sánh đối đầu 2 công thức món chay
│   ├── saved-recipes.md                       # Lưu và quản lý danh sách công thức yêu thích
│   ├── recipe-comments.md                     # Bình luận và phản hồi nhiều cấp (tối đa 5 cấp)
│   ├── expert-recipe-creation.md              # Dành cho Chuyên gia: Hướng dẫn đăng và chỉnh sửa công thức
│   └── ingredient-directory.md                # Tra cứu danh mục nguyên liệu và dinh dưỡng tham khảo
│
├── meal-planning/                             # Kế hoạch bữa ăn & Dinh dưỡng
│   ├── weekly-meal-planner.md                 # Lập kế hoạch thực đơn tuần 3 bữa cố định
│   ├── nutrition-tracking.md                  # Theo dõi và phân tích 9 chỉ tiêu dinh dưỡng thực đơn
│   └── exporting-meal-plan-and-nutrition.md   # Xuất file công thức và báo cáo dinh dưỡng TXT / PDF
│
├── shopping-list/                             # Danh sách đi chợ thông minh
│   ├── shopping-list-overview.md              # Tạo và quản lý danh sách mua sắm
│   ├── smart-ingredient-aggregation.md        # Cơ chế tự động tổng hợp nguyên liệu an toàn
│   └── shopping-list-actions.md               # Thao tác khi đi chợ và xuất file danh sách mua sắm
│
├── ai-features/                               # Trợ lý ảo AI Mâm Xanh
│   ├── ai-assistant-overview.md               # Tổng quan năng lực, giới hạn và an toàn của AI
│   ├── ai-chat-cooking-guidance.md            # Hỏi đáp ẩm thực chay và tư vấn nấu nướng cùng AI
│   ├── ai-meal-recommendations.md             # AI gợi ý món ăn và tạo thực đơn tuần tự động
│   ├── ai-recipe-drafting-assistance.md       # Dành cho Chuyên gia: AI hỗ trợ soạn bản nháp công thức
│   └── ai-execution-modes.md                  # 3 chế độ thực thi (QA, CONFIRM, AUTO) và bảo vệ dữ liệu
│
├── subscriptions/                             # Gói dịch vụ & Ngân sách AI
│   ├── plans-overview.md                      # So sánh chi tiết các gói: Free (0đ), Plus (99k), Pro (199k)
│   ├── ai-credit-and-top-up.md                # AI Credit, tỷ lệ nạp và thanh toán vượt mức (Overage)
│   └── managing-subscription.md               # Quản lý gói, kiểm tra hạn mức token và lịch sử giao dịch
│
├── policies/                                  # Chính sách & Tiêu chuẩn vận hành
│   ├── health-and-medical-disclaimer.md       # Tuyên bố miễn trừ trách nhiệm y tế và dinh dưỡng
│   ├── content-and-community-guidelines.md    # Quy chuẩn nội dung và tiêu chuẩn cộng đồng
│   ├── reporting-and-moderation.md            # Báo cáo vi phạm và quy trình xử lý của Quản trị viên
│   └── privacy-and-data-protection.md         # Chính sách bảo vệ dữ liệu và quyền riêng tư
│
└── troubleshooting/                           # Hướng dẫn xử lý sự cố thường gặp
    ├── account-and-login-issues.md            # Khắc phục lỗi đăng ký, đăng nhập và khóa tài khoản
    ├── recipe-creation-errors.md              # Khắc phục lỗi khi đăng bài công thức (quy đổi đơn vị, ảnh bìa...)
    ├── ai-limits-and-errors.md                # Xử lý khi AI từ chối trả lời hoặc hết hạn mức token
    └── subscription-and-payment-issues.md     # Xử lý sự cố thanh toán và nạp tiền
```

---

## 3. Quy chuẩn Metadata và Truy vết (Traceability)

Mỗi tài liệu trong kho tri thức đều bắt đầu bằng phần **YAML Front Matter** chuẩn hóa để máy tính và hệ thống tìm kiếm có thể phân loại, lập chỉ mục và truy vết nguồn gốc:

```yaml
---
id: weekly-meal-planner                      # Định danh duy nhất (kebab-case)
title: Lập kế hoạch thực đơn tuần 3 bữa       # Tiêu đề tài liệu
category: meal-planning                      # Thư mục chuyên đề
audience:                                    # Đối tượng hướng tới
  - customer
  - expert
source_requirements:                         # FR và BR liên quan trong SRS
  - FR-09
  - FR-33
  - BR-32
  - BR-36
source_use_cases:                            # Use Cases tương ứng
  - UC-09.1
  - UC-33.1
source_files:                                # Tệp nguồn xác minh (Frontend / Backend)
  - app/mamxanh-frontend/src/pages/MealPlanner.tsx
  - app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/controller/MealPlanController.java
implementation_status: verified              # Trạng thái: verified | partially-verified | planned
last_verified: 2026-11-01                    # Ngày xác minh gần nhất
---
```

### Bộ giá trị trạng thái triển khai (`implementation_status`):
- `verified`: Nghiệp vụ đã được xác minh đầy đủ qua cả SRS, mã nguồn Backend và giao diện Frontend.
- `partially-verified`: Nghiệp vụ đã có đặc tả và một phần mã nguồn, nhưng còn một số điểm đang hoàn thiện.
- `planned`: Tính năng đã được phê duyệt trong kế hoạch tương lai nhưng chưa triển khai thực tế.

---

## 4. Nguyên tắc phục vụ truy xuất RAG (RAG-Readiness)

1. **Tính độc lập ngữ cảnh (Self-contained Chunks):**
   - Mỗi phần nội dung (heading `##`) đều có thể đứng độc lập khi được hệ thống cắt đoạn (chunking).
   - Tuyệt đối không dùng các cụm từ phụ thuộc ngữ cảnh trước như *"như đã nêu ở trên"*, *"xem mục trước"*, giúp AI khi nhận một đoạn tài liệu trích xuất vẫn hiểu trọn vẹn ý nghĩa.
2. **Ngôn ngữ người dùng tự nhiên (User-First Phrasing):**
   - Không đưa mã kỹ thuật (FR-01, BR-19) vào thân bài viết. Khi AI trích dẫn nội dung cho người dùng, câu trả lời sẽ tự nhiên và lịch sự như một nhân viên hỗ trợ chăm sóc khách hàng.
3. **Phân tách tri thức tĩnh và dữ liệu động:**
   - Kho tri thức **chỉ chứa tri thức nghiệp vụ tĩnh** (quy tắc, cách sử dụng, điều kiện, chính sách).
   - Kho tri thức **không lưu trữ dữ liệu cá nhân động** (như số dư tiền thực tế của user A, món ăn trưa nay của user B). Những dữ liệu động này sẽ được AI truy vấn thông qua các công cụ API phân quyền riêng biệt khi cần.

---

## 5. Đọc tài liệu qua Spring Classpath Resources (JAR & Docker Compatibility)

Kho tri thức được định vị tại:
`app/mamxanh-backend/src/main/resources/knowledge-base/` (tương đương đường dẫn classpath `classpath:knowledge-base/`).

### Thiết kế tương thích môi trường JAR và Docker:
- **Không phụ thuộc thư mục vật lý:** Khi ứng dụng Spring Boot được đóng gói thành file `.jar` và triển khai trong container Docker, toàn bộ các tệp tin trong `src/main/resources` nằm bên trong tệp nén JAR, không tồn tại dưới dạng thư mục tệp thông thường trên hệ điều hành.
- **Cơ chế đọc chuẩn Spring Classpath:**
  - Danh mục `catalog.json` tại gốc thư mục chứa bảng chỉ mục đường dẫn tương đối của toàn bộ 38 tài liệu chuyên đề.
  - Các service Backend có thể đọc tài liệu thông qua `ResourceLoader`, `ClassPathResource("knowledge-base/...")` hoặc `ClassLoader.getResourceAsStream("knowledge-base/catalog.json")`.
  - Không sử dụng `java.io.File` hay đường dẫn tuyệt đối dạng `C:\...` hoặc `/app/...`.
  - Hỗ trợ nạp động bằng `PathMatchingResourcePatternResolver.getResources("classpath*:knowledge-base/**/*.md")`.

