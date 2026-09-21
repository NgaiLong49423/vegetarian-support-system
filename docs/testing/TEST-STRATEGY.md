> **Document:** Test Strategy  
> **File:** `docs/testing/TEST-STRATEGY.md`  
> **Version:** v1.6.0
> **Created:** 2026-09-13  
> **Last Updated:** 2026-09-20
> **Status:** Active  
> **Related Docs:** `docs/requirements/SRS.md`, `docs/architecture/ARCHITECTURE.md`, `CONTRIBUTING.md`

# Test Strategy

## 1. Mục đích và ranh giới trạng thái hiện tại

Tài liệu này định nghĩa cách dự án dự kiến kiểm chứng requirement và chất lượng trên các ranh giới React, Spring Boot, SQL Server và các external service. Đây không phải danh mục Test Case và không khẳng định rằng test, framework, CI job, environment hoặc command có thể chạy hiện đã tồn tại.

Hành vi mong đợi chi tiết được xác định trong [SRS](../requirements/SRS.md). Công việc triển khai và bằng chứng liên quan được quản lý qua GitHub Issues và các Pull Request được liên kết theo [CONTRIBUTING.md](../../CONTRIBUTING.md).

## 2. Mục tiêu kiểm thử

- Chứng minh hành vi đã triển khai đáp ứng requirement, Business Rule và Acceptance Criteria liên quan.
- Phát hiện lỗi về authorization, ownership, validation, feature entitlement, persistence và integration trước khi release.
- Kiểm thử các tình huống thành công, thất bại, biên và phục hồi có ý nghĩa thay vì chỉ tối ưu một con số Coverage.
- Làm rõ giới hạn của external service và phân biệt bằng chứng local có tính xác định với bằng chứng từ live provider.
- Duy trì traceability từ requirement đến work item, quá trình review implementation và bằng chứng kiểm thử.

## 3. Phạm vi và các cấp độ kiểm thử

| Cấp độ | Phạm vi dự kiến | Bằng chứng điển hình | Ranh giới |
|---|---|---|---|
| Backend Unit Test | Business logic như subscription entitlement, phân quyền tính năng AI, quyết định ownership, điều phối validation và mapping kết quả | Assertion bằng JUnit 5; dùng Mockito khi cần cô lập dependency | Test dựa trên Mock không chứng minh Spring configuration, SQL hoặc provider thật hoạt động đúng |
| Backend Integration Test | Spring Security rule, persistence mapping, transaction, Flyway migration và hành vi của adapter | Test với cấu hình đại diện và hạ tầng được kiểm soát | Cách dùng test container/database cụ thể chỉ được chọn sau khi scaffold tồn tại |
| REST API | Validation request, authentication, authorization, hành vi status/error và response contract | Automated contract/API check trên endpoint thật kết hợp với OpenAPI đã được dự án áp dụng | Thử bằng Swagger UI chỉ là bằng chứng hỗ trợ, không phải regression suite |
| Frontend Component Test | Rendering, hành vi input và các trạng thái loading/error mà người dùng nhìn thấy ở component quan trọng | Tooling sẽ được chọn cùng React scaffold | Không tự chọn framework khi chưa có package evidence |
| Browser Smoke / End-to-End Test (E2E) | Playwright kiểm tra Frontend shell và các luồng browser quan trọng; khi vertical slice tồn tại, E2E đi xuyên từ browser đến Backend như khám phá → lập lịch, publish/report moderation và kiểm tra chặn quyền tính năng AI | `@playwright/test` cho test lặp lại; browser control cho exploratory verification; HTML report, screenshot và trace khi được cấu hình | Frontend-only hoặc mock-backed smoke test không chứng minh Backend/database; E2E không thay Unit Test hoặc Integration Test tập trung |
| Release Smoke Test | Các luồng demo cốt lõi trên candidate commit của `main` sau khi tích hợp release | Bằng chứng pass/fail được ghi nhận và liên kết với release workflow | Smoke Test pass không chứng minh Regression Coverage rộng |

## 4. Ưu tiên Coverage theo rủi ro

Việc kiểm thử nên ưu tiên:

- Ranh giới authorization giữa Guest/Member/Administrator và ownership của Member.
- Profile validation Recipe Post: title 3–120 ký tự, ingredients 1–50 mục, serving 1–50 người, prep/cook 0–1.440 phút và tổng > 0, description tối đa 2.000 ký tự, một YouTube link hợp lệ (tùy chọn); công khai trực tiếp và quyền sửa/xóa; không có bộ đếm Like.
- Kiểm thử cấu trúc các bước thực hiện (`RECIPE_STEP`): bắt buộc 1–30 bước, mỗi bước có nội dung 10–2.000 ký tự (tiêu đề tùy chọn $\le 120$ ký tự), thứ tự liên tục $1..N$; kiểm thử nghiệp vụ thêm, sửa, xóa và kéo thả / thay đổi vị trí (`step_order`) không để lại khoảng trống hoặc trùng lặp số thứ tự.
- Quản lý bộ sưu tập hình ảnh (`RECIPE_MEDIA`): kiểm thử tải lên 0–5 ảnh (JPEG/PNG/WebP/GIF, tối đa 5 MB/ảnh) qua Azure Blob Storage, bắt buộc chỉ định đúng 1 ảnh đại diện (`is_cover = true`), thứ tự hiển thị `media_order` 1..5; tự động dọn rác ảnh mồ côi khi hủy soạn thảo hoặc xóa ảnh.
- Cổng kiểm định xuất bản về nguyên liệu và đơn vị quy đổi (`UNIT`, `INGREDIENT_UNIT_CONVERSION`): số lượng bắt buộc là số thực dương > 0, loại bỏ hoàn toàn "vừa đủ"; nếu tổ hợp nguyên liệu + đơn vị cần quy đổi sang gram mà chưa có conversion trong hệ thống thì chặn publish (Validation Error với thông báo rõ ràng).
- Kiểm thử đánh giá công thức (`RECIPE_RATING`): thang điểm 1–5 sao, chỉ Member đã đăng nhập mới được đánh giá, mỗi Member chỉ được 1 đánh giá/công thức (có thể cập nhật), tác giả không được tự đánh giá bài của chính mình; Guest chỉ có quyền xem điểm TB và số lượt đánh giá; kiểm tra chống IDOR/tampering điểm số.
- Kiểm thử theo dõi lượt xem (`RECIPE_VIEW`): cơ chế chống trùng lặp trong cửa sổ 30 phút theo IP hash / Session ID / Member ID; cập nhật bộ đếm bất đồng bộ không nghẽn luồng đọc công thức; kiểm tra tính chính xác của dữ liệu tổng hợp 24h, 7 ngày, 30 ngày và toàn thời gian.
- Kiểm thử 6 chế độ khám phá/sắp xếp công thức: Mới nhất, Đánh giá cao nhất (kèm số lượt đánh giá), Xem nhiều nhất (theo 4 mốc thời gian), Bình luận nhiều nhất, Hoạt động sôi nổi nhất (BR-71, tương tác gần 7 ngày không phân rã), và Thịnh hành (BR-72, tương tác có phân rã thời gian theo công thức trọng số); đảm bảo truy vấn SQL không dùng AI, tối ưu chỉ mục và giới hạn độ trễ $\le 3$s.
- Quyền riêng tư của report, quy tắc chống report đang mở bị trùng và ghi trực tiếp kết quả moderation vào Report.
- Tính độc lập giữa Saved Recipe/Meal Planner/Shopping history, giữ unavailable/tombstone khi bài nguồn không khả dụng, các meal type và chống dữ liệu trùng.
- Phân quyền tính năng AI theo gói Subscription (Free: AI Chatbot, Plus: Soạn bài & Biến tấu, Pro: Lập thực đơn tuần), Guest dùng AI Chatbot có technical rate limit chống spam, telemetry token không raw prompt và retention 90 ngày.
- Điều kiện dùng chức năng dinh dưỡng, quy đổi khẩu phần qua `INGREDIENT_UNIT_CONVERSION`, công khai dữ liệu thiếu và cấm bịa hoặc diễn giải theo hướng chẩn đoán.
- Validation ảnh cover, tính nhất quán của reference, lỗi YouTube embed và xử lý error của external provider.
- M11/Google Maps là `OUT_OF_SCOPE`, không có test scope hoặc release gate trong baseline hiện tại.
- Xác minh payment trước khi kích hoạt feature entitlement, expiry cuối kỳ, FREE/PLUS/PRO ở 0/49,000/99,000 VND/tháng, no auto-renew/no partial refund và idempotency cho duplicate processing.
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

- Credible bug phát hiện thụ động trong development/verification hoặc qua bug audit do người dùng yêu cầu được lưu thành từng file riêng tại `.agents/outputs/bugs/`; `bugs-metadata.yaml` quản lý ID, đường dẫn và trạng thái `RECORDED`, `TRACKING` hoặc `RESOLVED`. Đây là agent output kỹ thuật, không thay GitHub Bug Issue, owner, priority hoặc progress state.
- Playwright chỉ là một trong nhiều nguồn bug signal. Một Playwright failure phải được phân biệt với test/locator sai, environment chưa sẵn sàng, tool chưa cài, expected failure hoặc lỗi tạm thời trước khi ghi product bug.
- Agent không tự chạy bug audit trước merge/release. Explicit audit và việc promote một bug record thành GitHub Bug Issue đều cần user authorization trong task hiện tại.
- Issue owner chịu trách nhiệm cung cấp bằng chứng verification phù hợp với thay đổi; reviewer độc lập kiểm tra implementation và bằng chứng.
- Check fail hoặc bị skip phải được ghi rõ. Blocker được ghi trên Issue theo workflow của repository.
- Defect nghiêm trọng đã biết chặn release gate liên quan. Defect nhỏ được chấp nhận phải có Issue riêng và được công khai rõ trong release.
- Không mô tả phần việc mới kiểm tra local và chưa commit là đã merge, release hoặc deploy.

## 10. Quan hệ với trạng thái hoàn tất và release

[CONTRIBUTING.md](../../CONTRIBUTING.md#workflow-làm-việc-nhóm) sở hữu cổng merge và Definition of Done. PR vào `develop` có review/approval và GitHub Actions tùy chọn; owner vẫn tự kiểm tra và ghi rõ giới hạn. Merge tích hợp chưa xác nhận FR hoàn thành.

PR `develop -> main` cần approval độc lập, build/automated tests và required GitHub Actions checks đạt trên commit mới nhất. Test cases phải bao phủ Acceptance Criteria và các luồng lỗi/quyền truy cập liên quan; manual tests bổ sung phần chưa tự động hóa, không thay thế required checks. Cổng này chưa có bằng chứng triển khai đầy đủ chỉ vì đã được ghi trong tài liệu.

Sau merge, Tech Lead tổ chức kiểm tra demo local trên `main` với bằng chứng chức năng từ owner; đạt mới xác nhận Issue `Done` và đóng. Defect sau nghiệm thu được theo dõi theo workflow bug, giữ lịch sử Issue đã hoàn thành. Tạm thời chưa deploy/CD; strategy này không yêu cầu deployment để nghiệm thu demo local.

## 11. Điều kiện áp dụng và open item

Frontend đã chọn Playwright cho browser smoke/E2E; persistent tests nằm tại `app/mamxanh-frontend/tests/e2e/`. Khi scaffold tích hợp thật xuất hiện, nhóm vẫn phải xác minh và tài liệu hóa command, test data và điều kiện environment cho từng full E2E flow. Các open technical items gồm Frontend component-test tooling, API test runner, chiến lược tích hợp SQL Server, tần suất live-provider test, curated AI dataset chi tiết, provider-specific timeout/retry, Playwright CI integration và mọi Coverage gate. NFR-01–25 vẫn `ACTIVE` dù một số chi tiết kiểm chứng cần tiếp tục phân rã.
