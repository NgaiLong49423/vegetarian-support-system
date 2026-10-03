# Quy trình triển khai FR từ GitHub Issue

## Khi sử dụng

Dùng khi thành viên yêu cầu agent triển khai một GitHub Issue đã liên kết với FR. Câu gọi ngắn: “Làm Issue #<số> theo workflow triển khai FR.” Số Issue và yêu cầu hiện tại là đầu vào; workflow này không tự cấp quyền commit, push, tạo PR hay sửa GitHub.

## Nguồn và điều kiện đầu vào

- Đọc `AGENTS.md`, `CONTRIBUTING.md` và `.agents/POLICY.md`. `CONTRIBUTING.md` là nguồn quy định quy trình đóng góp và review.
- Đọc Issue thật, các Project field và công việc liên kết bằng phương thức truy cập GitHub hiện có. GitHub CLI là tùy chọn. Nếu không truy cập được, xin quyền truy cập hoặc nội dung Issue; không đoán scope.
- Kiểm tra Source Trace, Acceptance Criteria có thể kiểm chứng, đúng một owner, các field lập kế hoạch và dependency theo `CONTRIBUTING.md`. Xác nhận Issue có đúng một owner theo Project. Không suy đoán ownership từ người đang chat; nếu người yêu cầu không phải owner, chỉ triển khai khi yêu cầu hiện tại thể hiện rõ quyền thực hiện thay mặt owner hoặc Issue đã được giao lại theo quy trình. Báo phần thiếu hoặc mâu thuẫn trước khi code.
- Xác nhận FR có trong current registry `docs/requirements/SRS.md`, sau đó chỉ đọc định nghĩa FR/BR/NFR chi tiết và nguồn API/kiến trúc liên quan. SRS xác định yêu cầu; Issue theo dõi việc triển khai. Dùng `docs/architecture/ARCHITECTURE.md` và `app/mamxanh-backend/README.md` làm baseline kiến trúc Backend. Với API, tra generated OpenAPI runtime `/v3/api-docs` trước; nếu runtime không khả dụng, dùng artifact/spec sinh từ cùng branch commit. Trong migration, chỉ dùng `docs/api/openapi.yaml` làm planned contract cho endpoint chưa implement; dùng `docs/api/API.md` cho convention tích hợp. Dừng và báo rõ nếu generated contract mâu thuẫn với implementation, hoặc FR/Issue mâu thuẫn đáng kể với yêu cầu có thẩm quyền.

## Các bước

1. Xem `git status --short`, branch hiện tại, code và test liên quan. Giữ nguyên thay đổi local không thuộc nhiệm vụ. Tìm `AGENTS.md` lồng trong đường dẫn liên quan. Đọc README của app và hướng dẫn API/database khi phần việc cần đến.
2. Ánh xạ từng Acceptance Criterion với hành vi, thành phần cần sửa và cách kiểm chứng. Sau khi xác định file/surface thay đổi, route theo scope và nói rõ một dòng trước khi code:
   - `*.tsx`, `*.ts`, `*.css`, `src/components/`, `src/pages/`, route/UI/browser behavior -> đọc và áp dụng `.agents/skills/mamxanh-frontend-development/SKILL.md`.
   - `*.java`, Controller, Service, Repository, Entity, DTO, `*.sql`, Flyway, API/auth/database -> đọc và áp dụng `.agents/skills/mamxanh-backend-development/SKILL.md`.
   - Full-stack/API-backed UI -> đọc cả hai skill; workflow này vẫn sở hữu sequencing, gates và handoff.
   - Với công việc full-stack hoặc UI phụ thuộc API, đọc generated OpenAPI trước khi xác lập ranh giới và ghép hai phần triển khai. Nếu Backend chưa chạy, dùng generated artifact/spec của cùng branch commit. Trong migration, planned endpoint chỉ lấy từ `docs/api/openapi.yaml` và không được mô tả là runtime API.
   - Với endpoint hoặc DTO mới, không breaking và nằm trong scope Issue, Backend được quyền thiết kế và đồng bộ contract theo Engineering Autonomy trước khi Frontend phụ thuộc vào response shape mới.
   - Frontend không được tự suy đoán field, HTTP status, error shape hoặc hành vi authorization.
   Nếu scope còn mơ hồ, dùng Issue, file diff và task wording; không tạo orchestration framework mới. Triển khai theo kiến trúc Backend và baseline API đã được nhóm chấp nhận; không yêu cầu duyệt lại quyết định đã chốt. Với `TBD` làm thay đổi business meaning, externally visible behavior, Acceptance Criteria, structural schema relationship hoặc breaking contract, agent phải surface quyết định trước khi triển khai phần phụ thuộc. Không dùng `TBD` như lý do để xin duyệt các local implementation details thuộc Engineering Autonomy. Tuân thủ cổng duyệt contract trong `CONTRIBUTING.md` cho phần mới hoặc thay đổi; không tự ý đổi public API, schema, kiến trúc hoặc core dependency.
3. Khi Issue đủ điều kiện, tạo hoặc dùng branch phù hợp từ `develop` theo `CONTRIBUTING.md`. Không ghi đè thay đổi chưa commit hoặc branch của người khác. Triển khai trọn phạm vi Issue theo một luồng hoạt động hoàn chỉnh; đồng bộ FE, BE, validation, authorization, xử lý lỗi và test khi có liên quan. Không tự thu hẹp scope.
4. Khi chuẩn bị Ready for Review/merge hoặc owner yêu cầu sync, áp dụng [Develop integration baseline và đồng bộ PR](../../CONTRIBUTING.md#develop-integration-baseline). Kiểm tra `develop` hiện tại và các thay đổi liên quan kể từ lúc tạo branch/lần sync gần nhất; không cần sync sau mỗi commit không liên quan. Đồng bộ theo workflow Git được ủy quyền, resolve Git conflict trên feature branch, kiểm tra semantic conflict, adapt theo common infrastructure đã merge và kiểm tra lịch sử/version Flyway nếu có.
5. Sau synchronization có thể ảnh hưởng feature, chạy lại verification phù hợp với scope và repository rules; cập nhật evidence nếu kết quả trước đó không còn đại diện cho branch hiện tại. Đối chiếu từng Acceptance Criterion; phân biệt kiểm tra UI/mock với xác minh Backend/database thật. Ghi trung thực các kiểm tra lỗi, bỏ qua hoặc không chạy được.
   Trước merge PR vào `develop`, kiểm tra [required validation checks](../../CONTRIBUTING.md#develop-required-checks): Frontend 60/60/60/60, Backend overall JaCoCo line ≥80%, Sonar Quality Gate và CodeQL checks liên quan phải pass. Không hạ gates hoặc dùng exclusions/test vô nghĩa để pass. GitHub technical merge block cần Ruleset được xác minh; workflow không cho phép tự thay settings. Kody/Gemini chỉ advisory review.
6. Xem diff và status cuối cùng để phát hiện file ngoài scope, artifact sinh ra, secret hoặc thay đổi contract/yêu cầu ngoài ý muốn. Báo file đã đổi, bằng chứng cho Acceptance Criteria, lệnh và kết quả kiểm tra, rủi ro còn lại và quyết định nhóm cần chốt.

## Cổng chuyển bước

- Nếu Issue chưa đủ điều kiện, thiếu quyền truy cập, dependency đang chặn hoặc nguồn yêu cầu mâu thuẫn, dừng phần triển khai bị ảnh hưởng và nêu đúng thông tin/quyết định cần có để tiếp tục.
- Workflow cho phép triển khai local theo yêu cầu. Commit, push, tạo PR, cập nhật Issue/Project, merge và chuyển `Done` cần được người dùng cho phép trong nhiệm vụ hiện tại. Khi được phép, làm theo `CONTRIBUTING.md`, PR template hiện hành và `.agents/POLICY.md`; xem trước các thay đổi từ xa và xác minh kết quả.
- PR vào `develop` đưa Issue sang `Review`, chưa phải `Done`. Tech Lead phụ trách `develop -> main`; hoàn thành cần qua nghiệm thu demo local sau merge theo quy trình dự án.

## Kết quả bàn giao

Báo ngắn gọn bằng tiếng Việt: Issue/FR, branch, phạm vi đã làm, bằng chứng Acceptance Criteria, kết quả test, blocker và bước tiếp theo đã được cho phép. Không tạo file báo cáo tiến độ riêng.
