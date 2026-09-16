> **Document:** Test Strategy  
> **File:** `docs/testing/TEST-STRATEGY.md`  
> **Version:** v1.2.0
> **Created:** 2026-09-13  
> **Last Updated:** 2026-09-16
> **Status:** Active  
> **Related Docs:** `docs/requirements/SRS.md`, `docs/architecture/ARCHITECTURE.md`, `CONTRIBUTING.md`

# Test Strategy

## 1. Mục đích và ranh giới trạng thái hiện tại

Tài liệu này định nghĩa cách dự án dự kiến kiểm chứng requirement và chất lượng trên các ranh giới React, Spring Boot, SQL Server và các external service. Đây không phải danh mục Test Case và không khẳng định rằng test, framework, CI job, environment hoặc command có thể chạy hiện đã tồn tại.

Hành vi mong đợi chi tiết được xác định trong [SRS](../requirements/SRS.md). Công việc triển khai và bằng chứng liên quan được quản lý qua GitHub Issues và các Pull Request được liên kết theo [CONTRIBUTING.md](../../CONTRIBUTING.md).

## 2. Mục tiêu kiểm thử

- Chứng minh hành vi đã triển khai đáp ứng requirement, Business Rule và Acceptance Criteria liên quan.
- Phát hiện lỗi về authorization, ownership, validation, quota, persistence và integration trước khi release.
- Kiểm thử các tình huống thành công, thất bại, biên và phục hồi có ý nghĩa thay vì chỉ tối ưu một con số Coverage.
- Làm rõ giới hạn của external service và phân biệt bằng chứng local có tính xác định với bằng chứng từ live provider.
- Duy trì traceability từ requirement đến work item, quá trình review implementation và bằng chứng kiểm thử.

## 3. Phạm vi và các cấp độ kiểm thử

| Cấp độ | Phạm vi dự kiến | Bằng chứng điển hình | Ranh giới |
|---|---|---|---|
| Backend Unit Test | Business logic như entitlement, đếm quota, quyết định ownership, điều phối validation và mapping kết quả | Assertion bằng JUnit 5; dùng Mockito khi cần cô lập dependency | Test dựa trên Mock không chứng minh Spring configuration, SQL hoặc provider thật hoạt động đúng |
| Backend Integration Test | Spring Security rule, persistence mapping, transaction, Flyway migration và hành vi của adapter | Test với cấu hình đại diện và hạ tầng được kiểm soát | Cách dùng test container/database cụ thể chỉ được chọn sau khi scaffold tồn tại |
| REST API | Validation request, authentication, authorization, hành vi status/error và response contract | Automated contract/API check trên endpoint thật kết hợp với OpenAPI đã được dự án áp dụng | Thử bằng Swagger UI chỉ là bằng chứng hỗ trợ, không phải regression suite |
| Frontend Component Test | Rendering, hành vi input và các trạng thái loading/error/quota mà người dùng nhìn thấy ở component quan trọng | Tooling sẽ được chọn cùng React scaffold | Không tự chọn framework khi chưa có package evidence |
| End-to-End Test (E2E) | Một số ít luồng quan trọng xuyên từ browser đến Backend như khám phá → lập lịch, publish/report moderation và lỗi AI quota | Bằng chứng tự động hoặc manual có kiểm soát trong integration environment | E2E không thay thế Unit Test hoặc Integration Test tập trung để chẩn đoán lỗi |
| Release Smoke Test | Các luồng demo cốt lõi trên candidate commit của `main` sau khi tích hợp release | Bằng chứng pass/fail được ghi nhận và liên kết với release workflow | Smoke Test pass không chứng minh Regression Coverage rộng |

## 4. Ưu tiên Coverage theo rủi ro

Việc kiểm thử nên ưu tiên:

- Ranh giới authorization giữa Guest/Member/Administrator và ownership của Member.
- Profile validation Recipe Post: title 3–120, ingredients 1–50, serving 1–50, prep/cook 0–1.440 và tổng > 0, tối đa 30 steps, description 2.000, tối đa 5 ảnh JPEG/PNG/WebP 5 MB/ảnh và một YouTube link; công khai trực tiếp và quyền sửa/xóa.
- Quyền riêng tư của report, quy tắc chống report đang mở bị trùng và moderation action chỉ dành cho Administrator.
- Tính độc lập giữa Saved Recipe/Meal Planner/Shopping history, giữ unavailable/tombstone khi bài nguồn không khả dụng, các meal type và chống dữ liệu trùng.
- Điều kiện dùng AI, quota 5/15/50 reset 00:00 `Asia/Ho_Chi_Minh`, account/cookie+coarse-IP tracking, chỉ tính lượt thành công, telemetry không raw prompt và retention 90 ngày.
- Điều kiện dùng chức năng dinh dưỡng, quy đổi khẩu phần, công khai dữ liệu thiếu và cấm bịa hoặc diễn giải theo hướng chẩn đoán.
- Validation ảnh, tính nhất quán của reference, lỗi YouTube embed và xử lý error/quota của external provider.
- M11/Google Maps là `OUT_OF_SCOPE`, không có test scope hoặc release gate trong baseline hiện tại.
- Xác minh payment trước khi kích hoạt entitlement, expiry cuối kỳ, FREE/PLUS/PRO ở 0/49,000/99,000 VND/tháng, no auto-renew/no partial refund và idempotency cho duplicate processing.
- Authentication: rate limit account identifier + IP 10 phút sau 5 lần sai; access token ngắn hạn, rotating refresh, server-side revocation và logout thu hồi refresh session.
- Notification: in-app theo business event; email async/best-effort không rollback hành động gốc, moderation email phải được attempt.

Danh sách này dùng để ưu tiên. Test Case chi tiết phải được suy ra từ các requirement có lifecycle đã được phê duyệt và Acceptance Criteria tương ứng.

## 5. Chiến lược kiểm thử external service và AI

Sử dụng nhiều lớp bằng chứng cho Gemini, Azure Blob Storage, Google authentication, email, YouTube và payment provider trong tương lai:

1. Unit Test có tính xác định kiểm tra quyết định của ứng dụng bằng phản hồi thành công/thất bại được kiểm soát.
2. Adapter/Integration Test kiểm tra request/response mapping và phân loại lỗi mà không khiến mọi test phụ thuộc vào live provider.
3. Live check giới hạn kiểm tra credential, configuration và một tương tác thật tối thiểu trong non-production environment được phép khi integration đã được triển khai.

Live call phải dùng test configuration riêng, quota có giới hạn và dữ liệu không nhạy cảm. Secret không được xuất hiện trong Source Control, log hoặc test fixture. Test dùng Mock không chứng minh khả năng kết nối thật; một live call thành công cũng không chứng minh reliability, mức chi phí phù hợp hoặc quota dài hạn.

Riêng với AI, verification phải dùng curated evaluation set và đạt ít nhất 80% case thỏa các rule áp dụng về allowed Recipe Post sources, restrictions, non-fabrication và safety/business constraints. Không bắt buộc in-product satisfaction survey chỉ để đáp ứng NFR-25; exact Gemini model/version được chọn bằng technical evaluation. Test còn bao phủ không trừ lượt khi provider lỗi và nutrition không tự kê mục tiêu từ BMI/weight goal.

## 6. Test Data và environment

- Giữ fixture có tính xác định, nhỏ, có mục đích rõ ràng và không chứa dữ liệu riêng tư thật của người dùng.
- Đại diện đủ actor role, relationship về ownership, content state liên quan lifecycle, ranh giới quota và trường hợp thiếu dữ liệu dinh dưỡng cần cho hành vi được kiểm thử.
- Seed/reference data trở thành dữ liệu có thẩm quyền phải thuộc asset của Backend/database, không nằm trong Frontend Mock.
- Việc kiểm chứng database migration nên bắt đầu từ database state sạch và được hỗ trợ sau khi Flyway migration tồn tại.
- Tách cấu hình local/unit khỏi integration environment dùng chung và mọi live-provider environment. Deployment topology cụ thể cho các environment hiện chưa được phê duyệt.
- Test cleanup phải bảo đảm tính lặp lại và không được sửa production data hoặc dữ liệu cá nhân trên provider.

## 7. Traceability giữa requirement và công việc

Đối với công việc triển khai, duy trì đường bằng chứng:

```text
SRS requirement / BR
        -> lifecycle-approved GitHub Issue + Acceptance Criteria
        -> Pull Request và review evidence
        -> automated/manual test result
        -> release evidence trên main
```

Requirement ID phải ổn định. GitHub Issues quản lý tiến độ triển khai; chúng không được định nghĩa lại ý nghĩa của SRS. Issue synchronization phải chờ SRS lifecycle baseline được xác nhận rõ ràng. Test nên tham chiếu requirement/acceptance identifier nhỏ nhất nhưng đủ hữu ích đã được repository hỗ trợ, thay vì tạo thêm một nguồn requirement song song.

## 8. Cách hiểu Coverage

JaCoCo là công cụ Java Coverage đã được chọn. Coverage giúp phát hiện phần code chưa được thực thi bởi test, nhưng tỷ lệ cao không chứng minh assertion đúng, scenario đủ, security đúng hoặc integration hoạt động. Dự án chưa phê duyệt Coverage threshold dạng số, vì vậy tài liệu này không tự đặt một con số. Cần xem Coverage cùng rủi ro requirement, scenario nhánh/lỗi, thay đổi của outcome quan trọng và defect lọt qua kiểm thử.

Frontend Coverage tooling và mọi quality gate vẫn là `TBD` cho tới khi Frontend scaffold và workflow của nhóm cung cấp đủ bằng chứng để chọn.

## 9. Defect, bằng chứng và ownership

- Issue owner chịu trách nhiệm cung cấp bằng chứng verification phù hợp với thay đổi; reviewer độc lập kiểm tra implementation và bằng chứng.
- Check fail hoặc bị skip phải được ghi rõ. Blocker được ghi trên Issue theo workflow của repository.
- Defect nghiêm trọng đã biết chặn release gate liên quan. Defect nhỏ được chấp nhận phải có Issue riêng và được công khai rõ trong release.
- Không mô tả phần việc mới kiểm tra local và chưa commit là đã merge, release hoặc deploy.

## 10. Quan hệ với trạng thái hoàn tất và release

Một thay đổi chỉ được xem là hoàn tất kỹ thuật để review khi Acceptance Criteria áp dụng đã được kiểm tra, automated test liên quan pass (hoặc có bằng chứng manual trung thực khi automation chưa tồn tại), dự án build/run được khi đã có scaffold chạy được, các ảnh hưởng về security/data/documentation đã được xử lý và không còn review conversation quan trọng chưa giải quyết.

Release lên `main` còn phải tuân theo release checklist trong `CONTRIBUTING.md`, bao gồm verification tích hợp/demo, kiểm thử migration trên database sạch khi áp dụng và Smoke Test sau merge. Test Strategy này bổ sung cho workflow đó; nó không tạo board status mới và không làm yếu requirement phê duyệt hiện có.

## 11. Điều kiện áp dụng và open item

Khi scaffold thật xuất hiện, nhóm phải xác minh và tài liệu hóa command thực tế, framework version, vị trí test và điều kiện tiên quyết của environment. Các open technical items gồm Frontend test tooling, API test runner, chiến lược tích hợp SQL Server, tần suất live-provider test, curated AI dataset chi tiết, provider-specific timeout/retry và mọi Coverage gate. NFR-01–25 vẫn `ACTIVE` dù một số chi tiết kiểm chứng cần tiếp tục phân rã.
