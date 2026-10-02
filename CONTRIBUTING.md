> **Document:** Contribution Guide  
> **File:** `CONTRIBUTING.md`  
> **Version:** v3.9.0
> **Created:** 2026-06-14  
> **Last Updated:** 2026-10-02
> **Status:** Active  

# Hướng Dẫn Đóng Góp

This document is the source of truth for the repository contribution workflow. It applies to human contributors and AI agents. AGENTS.md contains additional agent-specific instructions; ADRs record related decisions and rationale.

Tài liệu này cung cấp các quy định và hướng dẫn chi tiết về cách đóng góp mã nguồn (code), cách viết thông điệp ghi nhận thay đổi (commit message), cách đặt tên nhánh (branch) và quy trình gửi yêu cầu gộp mã nguồn (pull request) cho dự án. Việc tuân thủ các quy tắc này giúp dự án luôn sạch sẽ, dễ bảo trì và làm việc nhóm hiệu quả hơn.

---

## Quy Tắc Chung

Để giữ cho kho lưu trữ mã nguồn của dự án luôn chuyên nghiệp, hãy tuân thủ các quy tắc cốt lõi sau:

* **Giữ code sạch, dễ đọc:** Viết code rõ ràng, tuân thủ các chuẩn định dạng (coding conventions) của ngôn ngữ lập trình được sử dụng.
* **Không commit file rác:** Tránh đưa các file tạm thời, file build, hoặc các file cấu hình cá nhân không cần thiết vào Git.
* **Viết commit message rõ ràng và có ý nghĩa:** Giúp các thành viên khác hiểu ngay bạn đã thay đổi những gì và tại sao.
* **Cập nhật tài liệu đầy đủ:** Nếu thay đổi của bạn ảnh hưởng đến cách cài đặt, cấu hình hoặc sử dụng dự án, hãy cập nhật lại tài liệu hướng dẫn liên quan.
* **Kiểm tra kỹ trước khi push code:** Hãy chắc chắn rằng mã nguồn được biên dịch thành công và chạy thử không gặp lỗi trước khi đẩy lên GitHub.

## Workflow Làm Việc Nhóm

Đây là Source of Truth cho quy trình vận hành. ADR-001 và ADR-002 giữ bối cảnh, lý do và lịch sử quyết định; không xác lập cổng merge riêng.

### Luồng nhánh và demo local

```text
Backlog -> Planning -> In Progress -> Review -> Done

branch làm việc -> PR vào develop -> kiểm tra tích hợp
                -> PR develop vào main -> kiểm tra demo local -> Done
```

- Thành viên phát triển và debug trên local; `develop` là current integration baseline của code, documentation, migration và common infrastructure đã merge; `main` là phiên bản ổn định để demo. Baseline này không thay thế authority theo concern như SRS, OpenAPI, Technology Stack hoặc architecture docs.
- Nhánh làm việc tách từ `develop`; thay đổi đi qua PR vào `develop`, rồi PR `develop -> main`.
- `Planning`: làm rõ scope, Acceptance Criteria, dependency và kế hoạch trước khi code. `In Progress`: triển khai và tự kiểm tra.
- `Review` bắt đầu khi mở PR vào `develop`; sau merge Issue vẫn ở `Review` chờ nghiệm thu trên `main`. Nếu PR bị đóng hoặc cần làm lại đáng kể, chuyển về `In Progress`.
- `Done` chỉ sau khi toàn bộ scope/Acceptance Criteria đạt trên `main` và kiểm tra demo local sau merge đạt. Merge vào `develop` hoặc `main` riêng lẻ chưa đủ.
- Không cần hoàn thành cả module SRS mới đưa code lên `main`. Phạm vi PR gồm những thay đổi ổn định và dependency đã được đáp ứng.
- Vercel là nền tảng hosting đã chốt cho Frontend; Azure App Service là nền tảng hosting đã chốt cho Backend. Bản UI hiện có trên Vercel vẫn được deploy thủ công và chưa cấu hình CI/CD hoặc Git auto-deploy. Deployment là công việc riêng, không phải điều kiện `Done` của từng FR ở giai đoạn demo local. Chi tiết và giới hạn bản hiện tại nằm trong [Frontend README](app/mamxanh-frontend/README.md#vercel-cho-frontend).
- Quản lý blocker trực tiếp trên GitHub Issue hoặc GitHub Project; ghi nguyên nhân, dependency liên quan, trợ giúp cần thiết và điều kiện để tiếp tục.

### Phân rã và điều kiện nhận Issue

- Mặc định mỗi FR có một Issue triển khai. Chỉ đề xuất sub-issue khi scope quá lớn cho một PR, thông thường trên 5 SP (ví dụ 8 SP); ghi rõ lý do và được Tech Lead duyệt, dùng template hiện có. Sub-issue truy về FR và Issue cha, tránh giao trùng scope giữa cha/con hoặc giữa các FR liên quan.
- Scope và Acceptance Criteria xuất phát từ SRS; không cắt yêu cầu cho vừa người nhận. Phân rã dựa trên công việc và dependency, không dựa trên việc muốn ghi nhận hoàn thành một phần.
- Trước khi nhận, Issue/sub-issue phải có Source Trace, scope, Acceptance Criteria kiểm tra được, đúng một owner, Type, Priority, SP, Start Date, Target Date cụ thể và dependency/blocker rõ ràng. Owner tham gia ước lượng và xác nhận cam kết. Khi chuẩn bị draft, agent phải đề xuất Story Points, Type và Priority theo options thật của Project, kèm lý do và trạng thái chờ Tech Lead duyệt; không để cả ba field TBD cho FR đã phân rã.
- Phân biệt dependency với blocker thực tế: ghi đầu ra cần có, trạng thái đã/chưa đáp ứng và điều kiện hết block. Công việc chưa rõ nghiệp vụ hoặc bị block ngăn triển khai chưa được giao như một cam kết triển khai trong tuần; có thể giao việc làm rõ/gỡ blocker trước.
- Khi có sub-issue triển khai, dùng SP của sub-issue để tính tải; SP ở Issue cha chỉ là ước lượng tổng thể tham khảo, không cộng thêm vào tải hoặc SP hoàn thành. Issue cha chỉ `Done` khi toàn bộ phạm vi FR được nghiệm thu.

### Nhịp tuần, Story Points và deadline

- Đầu tuần, nhóm chọn Issue và lượng việc phù hợp với khả năng, lịch học và thời gian của từng người; dùng thang SP `1, 2, 3, 5, 8` để ước lượng độ lớn, độ phức tạp và mức chưa chắc chắn. Không quy đổi cứng SP thành giờ hoặc yêu cầu mọi người nhận SP bằng nhau.
- Ưu tiên một Issue và một PR hoàn chỉnh cho FR ở mức 1–5 SP. FR khoảng 8 SP cần xem xét sub-issue theo các phần hành vi có thể nghiệm thu; không tách thành các Issue ngang hàng cùng FR chỉ để chia PR, không ép giảm SP để tránh phân rã. SP là ước lượng tương đối, không phải giới hạn tự động.
- Khi nhận Issue, owner cam kết hoàn thành toàn bộ scope và Acceptance Criteria đúng Target Date, có thời gian tự kiểm tra và sửa lỗi trước PR cuối tuần. Không tự giảm scope hoặc dời deadline.
- Cuối tuần là mốc xem xét đưa phần ổn định vào `main`, không phải yêu cầu merge bất kể chất lượng. Tech Lead chọn phạm vi và mở PR; lỗi nghiêm trọng có thể cần đợt sửa sớm.
- Owner cập nhật khi bắt đầu, khi có blocker, nguy cơ trễ hoặc thay đổi ảnh hưởng công việc. Không yêu cầu báo cáo repository riêng hoặc cập nhật cứng vào ngày thứ 2/3.
- Báo ngay khi dự kiến trễ: nguyên nhân, phần còn lại, blocker, trợ giúp cần thiết và dự kiến hoàn thành mới. Tech Lead quyết định hỗ trợ, điều chỉnh phân công hoặc duyệt deadline mới; báo trễ không tự động gia hạn hay miễn trách nhiệm.
- Ghi nhận trễ hạn so với deadline đã cam kết, kể cả khi được hỗ trợ hoặc duyệt hạn mới. Nếu owner đã bàn giao đầy đủ, đúng hạn và chỉ còn chờ review/đợt đưa lên `main`, ghi rõ thời điểm bàn giao và nguyên nhân chờ để phân biệt trách nhiệm owner với thời gian nghiệm thu.
- Issue chưa đạt giữ mở, chưa tính SP hoàn thành; ưu tiên hoàn tất việc tồn trước khi giao thêm cho owner. Không giảm scope hay tách phần thiếu sau deadline để coi Issue gốc đã xong.
- Nếu phần chưa hoàn thành trên `develop` ảnh hưởng bản demo, phải sửa hoặc cô lập trước PR vào `main`. SP hoàn thành chỉ tính khi work item đạt `Done`; nhóm dùng kết quả và nguyên nhân chênh lệch để điều chỉnh kế hoạch tuần sau, không chấm điểm cá nhân.

### Ownership, AI và API contract

- Owner chịu trách nhiệm toàn bộ luồng FR, gồm cả FE do AI tạo/sửa. Không mặc định chia sub-issue FE/BE; dùng checklist trong Issue. Không bắt buộc có thành viên FE riêng.
- Phân rã FR theo SRS trước khi triển khai. Theo [Engineering Autonomy Policy](#engineering-autonomy-policy), người thực hiện (developer hoặc AI coding agent) có quyền tự chủ thiết kế các endpoint, request/response DTOs, validation và error codes trong phạm vi Issue được giao.
- **Quy tắc contract API:** Generated OpenAPI tại `/v3/api-docs` từ source Spring Boot là runtime contract cho endpoint đã implement và là contract agent/CI phải tra cứu. Trong migration, `docs/api/openapi.yaml` chỉ giữ planned/reference contract cho endpoint chưa implement; reviewer đối chiếu semantic contract với runtime cho phần đã implement, không duy trì hai nguồn chuẩn song song. Cập nhật `docs/api/API.md` trong cùng work item khi thay đổi convention tích hợp chung; không yêu cầu sao chép mọi endpoint runtime vào YAML thủ công. FE và BE không được tự quyết contract khác nhau; conflict phải được nêu rõ trước merge.
- Không tạo các breaking API changes (xóa endpoint, đổi path/method, xóa/đổi tên trường response, đổi auth model) mà không có sự phối hợp, thống nhất trước với các bên tiêu thụ (consumer).
- Owner ghi nhận phần AI hỗ trợ và bằng chứng người thực hiện đã xác minh: kiểm tra API, thao tác từ UI, các test đã chạy và kết quả. AI báo hoàn thành không phải bằng chứng nghiệm thu.

### Template bắt buộc cho PR và Bug Issue

- Mọi PR vào `develop` hoặc `main`, kể cả PR do AI agent chuẩn bị hay tạo qua CLI/API, phải dùng [PR template của dự án](.github/pull_request_template.md).
- Mọi Bug Issue phải dùng [Bug Report form của dự án](.github/ISSUE_TEMPLATE/bug_report.yml). Khi tạo qua CLI/API, body phải giữ các mục và thông tin bắt buộc tương ứng với form hiện hành.
- Giữ cấu trúc, các mục và checklist của template; điền nội dung theo thay đổi thực tế. Mục không áp dụng ghi `Không áp dụng` kèm lý do hoặc bỏ qua theo chỉ dẫn sẵn trong template. Không tự thay bằng format mới hoặc lược bỏ thông tin bắt buộc.
- Quy tắc áp dụng cho cả thành viên và AI agent. Nếu template cần thay đổi, đề xuất để Tech Lead duyệt và cập nhật template chung trước khi sử dụng cấu trúc mới.

### PR vào `develop`

- PR dùng `Refs #<issue-number>`, ghi scope, thay đổi và kết quả tự kiểm tra; không chứa secret, `.env`, credential hoặc file build/cá nhân.
- Required validation checks phải pass trước merge; xem [Required checks và technical enforcement](#develop-required-checks). Ruleset hiện có yêu cầu một approval; giữ gate review hiện hành, không coi AI review là approval của thành viên.
- Owner tự kiểm tra phần thay đổi và ghi trung thực phần chưa kiểm tra/blocker. Merge vào `develop` là tích hợp, chưa xác nhận FR đã hoàn thành.
- Một Issue có thể có nhiều PR liên quan; không ép quan hệ một Issue/một branch/một PR.

<a id="develop-integration-baseline"></a>
#### Develop integration baseline và đồng bộ PR song song

- **Baseline:** `develop` là current integration baseline. Chỉ code, documentation, migration và common infrastructure thực sự đã merge vào `develop` mới thuộc baseline tích hợp mà các branch khác phải tương thích trước khi merge. Quy tắc này không thay thế source of truth theo concern của repository, gồm SRS/requirements, OpenAPI contract, Technology Stack, architecture và các authority được chỉ định khác.
- **Draft/Open work:** Draft PR, Open PR chưa merge, feature branch khác hoặc commit ngoài `develop` không tự trở thành integration baseline hoặc dependency bắt buộc, và không buộc branch khác đổi implementation chỉ vì chúng tồn tại. Dependency được xác định rõ trong GitHub Issue, bởi owner/Tech Lead, requirement, repository policy hoặc quyết định integration đã chốt vẫn phải được tôn trọng.
- **Baseline tiến lên:** Integration baseline chỉ thay đổi khi nội dung thực sự được merge vào `develop`, theo merge gates hiện hành. PR nào được merge trước tạo baseline mới; không suy thứ tự merge từ số Issue/PR, thời điểm mở PR, tạo branch hoặc commit. Không yêu cầu giữ compatibility với Draft/Open PR chưa merge, trừ dependency đã được xác nhận.
- **Thời điểm đồng bộ:** Owner kiểm tra/sync `develop` khi chuẩn bị Ready for Review, trước merge nếu baseline liên quan đã thay đổi kể từ lần sync gần nhất, hoặc khi owner yêu cầu. Không cần sync lặp lại cho mỗi commit không liên quan trên `develop`.

```text
PR được merge vào develop
        ↓
develop trở thành integration baseline mới
        ↓
PR liên quan còn mở sync baseline mới trên feature branch
        ↓
resolve Git conflict + inspect semantic conflict
        ↓
adapt implementation + kiểm tra migration nếu có
        ↓
chạy lại verification phù hợp với scope
        ↓
merge theo gates hiện hành
```

- **Git và semantic conflict:** Git conflict được resolve trên feature branch. Dù Git merge tự động thành công, owner vẫn kiểm tra semantic conflict: duplicate common infrastructure/service/helper; exception hierarchy hoặc API/error convention không tương thích; entity/schema assumptions thay đổi; DTO/client abstraction trùng; FE/BE contract drift; shared frontend client/component conventions và các xung đột architecture/behavior khác. “Git không báo conflict” không chứng minh branch an toàn để merge.
- **Common infrastructure:** Nếu `develop` đã có infrastructure dùng chung, PR còn mở phải reuse, extend hoặc adapt theo baseline đó thay vì giữ implementation song song không cần thiết. Bao gồm `GlobalExceptionHandler`, `ErrorCode`, exception hierarchy, auth/security abstraction, API response/error convention, shared DTO, common utility/service, repository conventions, shared frontend API client và design/component primitives. Nếu baseline thật sự không đáp ứng feature, owner/agent surface concern trong PR hoặc xin quyết định phù hợp; không tự tạo convention cạnh tranh.
- **Flyway:** Migration đã merge giữ nguyên version và content. Sau khi sync, migration chưa merge phải thích ứng với lịch sử mới và dùng version khả dụng tiếp theo khi collision. Không sửa migration đã baseline hóa để giải collision; thay đổi tiếp theo dùng append-only migration. Trước khi chọn version, kiểm tra migration history hiện hành trên `develop`.
- **Resolve an toàn và re-validation:** Không chọn `ours`, `theirs`, Accept Current hoặc Accept Incoming cho toàn file khi chưa hiểu intent. Xác định phần đã thành baseline và intent feature, kết hợp đúng hai phía; nếu semantic intent chưa rõ, dừng và hỏi owner/reviewer. Sau sync, chạy lại verification phù hợp với scope và repository rules (ví dụ backend build/tests/migration validation, frontend typecheck/build/tests hoặc cross-layer API/FE-BE contract checks). Kết quả trước sync không phải bằng chứng cuối nếu synchronization có thể ảnh hưởng feature; cập nhật PR evidence khi cần.

<a id="docker-development"></a>
#### Docker development và kiểm thử tích hợp

**Quy tắc chuẩn:** `docker-compose.yml` ở repository root là entry point chuẩn cho stack Docker development tích hợp FE/BE/SQL Server. Compose build các Dockerfile riêng của Frontend và Backend từ source hiện tại trong branch, khởi tạo SQL Server và chờ service phụ thuộc khỏe trước khi đưa stack lên. SQL Server image được pin bằng tag dễ đọc cộng repository manifest digest lấy từ MCR; Compose và Testcontainers dùng cùng reference. Khi nâng image, cập nhật cả hai reference trong PR, xác minh digest/architecture và tools trên MCR, chạy Backend integration tests và Docker Development gate trước merge. Không dùng `latest`, không dùng config/image ID làm digest, và không ghi nhãn image tag thành tên product release.

- Dockerfile tại `app/mamxanh-frontend/` và `app/mamxanh-backend/` vẫn thuộc từng component, định nghĩa cách build component đó; chúng không phải hai môi trường tích hợp độc lập và không thay thế Compose.
- Không yêu cầu hoặc tạo hai CI gate build riêng cho FE và BE chỉ để lặp lại việc build đã được `Docker Development` thực hiện. Có thể chạy riêng một Dockerfile để debug component; kiểm tra riêng không thay thế Compose gate khi PR ảnh hưởng stack hoặc tích hợp.
- Mọi thay đổi source FE/BE, Dockerfile, dependency/lockfile, cấu hình runtime được Compose sử dụng, database initialization hoặc `docker-compose.yml` phải được đánh giá theo toàn stack. Trước khi Ready for Review, chạy Compose local nếu Docker khả dụng; PR sau đó phải pass `Docker Development` trên commit mới nhất.
- Compose publish các port development chỉ trên loopback `127.0.0.1`; SQL host port `1433` được giữ để hỗ trợ Backend chạy trực tiếp trên máy. `name: mamxanh-dev` là project mặc định. Nếu dùng `-p <project>` để cô lập worktree, dùng chính project name đó cho mọi lệnh lifecycle của stack: `config`, `up`, `ps`, `run`, `down` và volume reset. Tên project không tránh xung đột host ports khi chạy nhiều stack song song.
- Docker chỉ phục vụ development và verification. Không suy ra thay đổi deployment/production từ Docker Compose; production baseline vẫn là Vercel cho Frontend, Azure App Service cho Backend và Azure SQL theo tài liệu công nghệ hiện hành.

**Thiết lập lần đầu (PowerShell tại repository root):**

```powershell
if (-not (Test-Path app/mamxanh-backend/.env)) {
    Copy-Item app/mamxanh-backend/.env.example app/mamxanh-backend/.env
} else {
    Write-Output 'Existing app/mamxanh-backend/.env preserved; edit it only if needed.'
}
```

Mở `app/mamxanh-backend/.env`, thay `MSSQL_SA_PASSWORD` bằng mật khẩu local mạnh đáp ứng yêu cầu SQL Server. File `.env` bị ignore và không được commit/chia sẻ; `.env.example` chỉ là hợp đồng biến môi trường an toàn. Compose override connection URL/user/password của Backend để kết nối service `sqlserver`; SMTP để trống thì không gửi email thật. Không đưa secret vào command line, workflow YAML, PR log hay tài liệu.

**Khởi động và xác minh:**

```powershell
$composeProject = 'mamxanh-dev'
docker compose -p $composeProject --env-file app/mamxanh-backend/.env config --quiet
docker compose -p $composeProject --env-file app/mamxanh-backend/.env up --build --detach --wait --wait-timeout 600
Invoke-WebRequest http://localhost:5173/ -UseBasicParsing
Invoke-WebRequest http://localhost:8080/v3/api-docs -UseBasicParsing
docker compose -p $composeProject --env-file app/mamxanh-backend/.env ps
```

`config --quiet` xác nhận Compose parse/interpolate được, không chứng minh image chạy. Sau khi `up` thành công, FE phải phản hồi tại `http://localhost:5173/`, Backend OpenAPI tại `http://localhost:8080/v3/api-docs`, và `ps` phải cho thấy các service dài hạn healthy. Compose kiểm tra khởi động và endpoint smoke; nó không thay unit, integration, authorization hoặc acceptance tests của feature. Khi xác minh xong, dừng stack:

```powershell
docker compose -p $composeProject --env-file app/mamxanh-backend/.env down
```

Lệnh `down` giữ named volumes, bao gồm dữ liệu SQL local và Frontend dependencies. Sửa `MSSQL_SA_PASSWORD` trong `.env` không tự đổi password đã khởi tạo trong SQL Server volume; volume còn giữ database state/credential cũ. Nếu cần reset database development nhưng giữ dependency Frontend, dừng project rồi xóa riêng SQL volume, sau đó khởi động lại:

```powershell
$composeProject = 'mamxanh-dev'
docker compose -p $composeProject --env-file app/mamxanh-backend/.env down
docker volume rm "$($composeProject)_sqlserver-data"
# Đặt password mong muốn trong app/mamxanh-backend/.env trước khi khởi động lại.
docker compose -p $composeProject --env-file app/mamxanh-backend/.env up --build --detach --wait --wait-timeout 600
```

> Xóa SQL volume sẽ xóa toàn bộ database development local của project đó; không chạy nếu cần giữ dữ liệu. Thay `mamxanh-dev` bằng cùng project name đã dùng cho stack nếu có override.

Sau khi `package.json` hoặc `package-lock.json` thay đổi, cập nhật dependency trong named volume từ lockfile bằng one-off container; lệnh này không khởi động dependencies và không chạm SQL volume:

```powershell
$composeProject = 'mamxanh-dev'
docker compose -p $composeProject --env-file app/mamxanh-backend/.env run --rm --no-deps frontend npm ci
```

Mọi lệnh sau đó (`up`, `down`, `run`, reset volume) phải tiếp tục dùng cùng `$composeProject`. Chỉ khi troubleshooting fallback không dùng được `run npm ci`, dừng stack và xóa riêng `<project>_frontend-node-modules`; không xóa SQL volume để làm mới Frontend dependencies.

`docker compose ... down --volumes --remove-orphans` là **full reset**: xóa cả SQL data và `frontend-node-modules`. Chỉ dùng khi chủ động chấp nhận mất toàn bộ named-volume data. CI được phép làm vậy vì runner là disposable.

**PR và GitHub gate:**

- Workflow `.github/workflows/ci.yml` chạy `Docker Development` trên PR hướng vào `develop`/`main` và push vào hai branch đó. Job setup Node.js `22.23.3`, dùng project `mamxanh-ci` thống nhất cho `up`/cleanup, build Compose, đợi health checks, capture `/v3/api-docs`, validate bằng Scalar CLI `2.5.2`, upload OpenAPI artifact riêng theo PR/run, rồi smoke-test route `/scalar`; CI dùng password tạm, dọn volume trên runner disposable sau job. HTTP `curl /scalar` chỉ chứng minh route/HTML shell trả về, không chứng minh Scalar JavaScript đã render contract hoặc request API chạy được.
- Scalar tại `http://localhost:8080/scalar` là giao diện chính thức để team đọc và manual-test API. Browser acceptance phải xác nhận JavaScript tải/render generated `/v3/api-docs`, kiểm tra operation và gửi request phù hợp trong giao diện; đây là bằng chứng riêng với CI route smoke và không thay automated regression/authorization tests.
- Ruleset `protect-develop` yêu cầu status context `Docker Development` và strict up-to-date. PR vào `develop` phải sync baseline theo phần trên; sau lần sync cuối có ảnh hưởng, chạy lại kiểm tra liên quan và đợi CI trên commit cập nhật.
- Nếu Docker không chạy được local, ghi rõ nguyên nhân và kết quả nào chưa xác minh trong PR; không ghi “Docker test passed”. Required CI check vẫn phải pass trước khi merge. Việc Docker daemon của máy cá nhân unavailable không tự cho phép bỏ qua gate.
- Khi sửa Compose, workflow hoặc Dockerfile, giữ cùng stack/entry point và cùng smoke criteria nhất quán; không tạo nhánh cấu hình local riêng hoặc yêu cầu thành viên pull image `latest` thủ công ngoài định nghĩa đã review trong Git.

<a id="develop-required-checks"></a>
#### Required checks và technical enforcement cho `develop`

Project Owner chốt hard gates: Frontend Playwright/Istanbul/NYC đạt **≥60% cho cả Lines, Statements, Functions và Branches**; Backend JaCoCo đạt **overall BUNDLE LINE ≥80%**. Command/check fail không được coi là pass. Xem [Test Strategy](docs/testing/TEST-STRATEGY.md#8-cách-hiểu-coverage) và app README cho lệnh, metric và report paths.

| Required check context | Phạm vi |
|---|---|
| `Frontend` | Typecheck, build và Playwright coverage hard gate |
| `Backend` | `clean verify`: JUnit, package và JaCoCo hard gate |
| `Docker Development` | Build và chạy smoke test toàn bộ development stack từ Docker Compose |
| `Sonar` | Đọc LCOV/JaCoCo qua artifacts, static analysis và chờ Sonar Quality Gate; scan/gate fail hoặc timeout làm check fail |
| `Analyze (javascript-typescript)` | CodeQL default setup: JavaScript/TypeScript validation |
| `Analyze (java-kotlin)` | CodeQL default setup: Java validation |
| `Analyze (actions)` | CodeQL default setup: GitHub Actions validation |

CodeQL là GitHub default setup, không có workflow YAML local. Ba tên CodeQL và `Frontend`/`Backend` đã được thấy trên GitHub check runs; `Sonar` và `Docker Development` là job riêng trong workflow và cần xác nhận context sau lần chạy đầu. Mọi PR validation liên quan phải pass; khi bổ sung validation job mới, cập nhật danh sách required contexts cùng workflow, không coi job mới là advisory mặc định.

Không yêu cầu `release-source` cho PR `develop`: workflow này chỉ kiểm tra PR vào `main`. Dependabot Updates là utility, deployment/manual/release jobs không chạy PR không phải merge gate của `develop`. `SonarCloud Code Analysis` là report từ Sonar app và có thể `neutral`; dùng job `Sonar` chờ Quality Gate làm gate. Kody/Kodus + Gemini chỉ advisory AI reviewer; chưa đưa thành required check trước khi đánh giá đủ PR, false positives và có quyết định riêng của owner.

**Trạng thái enforcement ngày 2026-10-02:** Ruleset `protect-develop` đang active, yêu cầu PR và một approval, chặn force push/xóa branch, bật strict up-to-date và có owner/admin bypass. Required status check `Docker Development` đã được cấu hình trực tiếp trên Ruleset. Các context khác trong bảng mô tả policy validation nhưng chưa được xác nhận là required trong Ruleset; cần kiểm tra sau khi các check tương ứng chạy trên PR. Workflow YAML riêng không tự bật GitHub settings.

Để duy trì/mở rộng technical merge blocking, owner cấu hình Ruleset/Branch Protection ngoài repository; trạng thái hiện tại được xác minh ở trên:

1. Target đúng `refs/heads/develop`; require PR before merging, giữ approval/review gate hiện hành.
2. Ruleset hiện yêu cầu `Docker Development`. Sau khi có PR run, xác minh context được GitHub nhận diện đúng và một lần chạy fail chặn merge. Bổ sung các contexts còn lại trong bảng từ PR check runs thực tế, với source GitHub Actions khi có tùy chọn; xác minh CodeQL checks chạy trên PR target `develop` trước khi require.
3. Bật require branches to be up to date (strict mode); sau sync phải chạy lại checks trên commit cập nhật.
4. Giữ block force pushes và branch deletion. Owner bypass là quyền quản trị hiện có, không làm member PR được bỏ qua gates.
5. Dùng PR kiểm tra để xác nhận mỗi check fail thực sự khóa merge; không ghi technical enforcement hoàn tất chỉ từ YAML hoặc checklist. Sonar secret/Quality Gate phải sẵn sàng; không skip Sonar và báo xanh khi secret/report thiếu.

### PR vào `main` và nghiệm thu

Tech Lead chịu trách nhiệm chọn phạm vi, mở PR `develop -> main`, tổng hợp bằng chứng và tổ chức nghiệm thu. Không sửa chức năng trực tiếp trong PR này; sửa qua branch/PR vào `develop`.

Checklist trước merge:

- [ ] Liệt kê FR/Issue thuộc phạm vi bằng `Refs #<issue-number>`; toàn bộ scope được đưa vào bản demo và dependency đã đáp ứng.
- [ ] Acceptance Criteria được bao phủ bằng test cases cho luồng chính, lỗi và quyền truy cập khi liên quan; ghi automated/manual tests và kết quả. Manual test bổ sung các phần chưa tự động hóa, không thay thế required automated checks.
- [ ] Build, automated tests và các required GitHub Actions checks đạt trên commit mới nhất. Check fail, skip hoặc chưa có automation không được ghi là pass; không bypass để kịp demo.
- [ ] Kiểm tra tích hợp và các luồng demo chính đạt; không còn lỗi cản trở demo. Lỗi được chấp nhận có Bug Issue và được công khai trong PR.
- [ ] Migration/schema đã kiểm tra trên database sạch hoặc môi trường tương đương khi có thay đổi DB; tài liệu/contract liên quan đã cập nhật; không có secrets/artifacts ngoài scope.
- [ ] Mọi yêu cầu sửa bắt buộc đã giải quyết; có ít nhất một approval từ người khác tác giả PR. Code do chính reviewer viết cần một thành viên khác kiểm tra phần đó.

Sau merge, owner cung cấp bằng chứng chức năng; Tech Lead tổ chức kiểm tra demo local trên commit mới nhất của `main`. Đạt thì xác nhận, đóng Issue và chuyển `Done`. Dùng `Refs` cả ở PR vào `main` để tránh đóng Issue bằng keyword trước khi kiểm tra sau merge.

Nếu kiểm tra thất bại, Issue bị ảnh hưởng chưa `Done`; nếu đã đóng sai trong chính đợt đó thì mở lại và đưa về `Review`, tạo Bug Issue liên kết. Issue đã nghiệm thu từ trước giữ lịch sử; lỗi mới có Bug Issue riêng. Lỗi nghiêm trọng làm bản demo không dùng được do Tech Lead quyết định sửa ngay hoặc revert, giữ cổng approval/checks cho PR vào `main`.

**Trạng thái công cụ:** `.github/workflows/ci.yml` chạy Frontend/Backend verification cho PR và push vào `develop`/`main`, cùng job Sonar nhận artifacts và chờ Quality Gate. `.github/workflows/release-source.yml` kiểm tra PR vào `main` phải có source `develop`. Build/test automation có trong repository; GitHub required checks và server-side Sonar settings phải được xác minh riêng, không suy ra từ workflow YAML.

### Review và phản hồi

- `Request changes`: bug, sai Acceptance Criteria, security, sai contract hoặc build/test fail; phải sửa trước merge vào `main`.
- `Comment`: hỏi hoặc trao đổi; reviewer ghi rõ nếu cần trả lời trước approval. `Suggestion`/`nit`: cải thiện nhỏ không bắt buộc, không chặn merge chỉ vì nit.
- Reviewer kiểm tra code, scope, logic, failure cases, security, test evidence và maintainability; không chỉ đọc checklist hoặc format.
- Tác giả sửa xong phản hồi và request kiểm tra lại; không chỉ tự resolve comment. Yêu cầu quan trọng được resolve khi người nêu xác nhận hoặc có quyết định thay thế được ghi rõ.
- Code thay đổi sau approval cần review lại phần thay đổi và required checks chạy trên commit mới nhất.
- Reviewer phản hồi trong khoảng 24 giờ khi lịch học cho phép; nếu bận thì báo Tech Lead để đổi reviewer.

### Báo và xử lý bug

- Lỗi riêng trên branch cá nhân do người làm tự xử lý. Bug trên `develop`/`main` tạo Bug Issue theo form hiện có; nếu lỗi thấy ở branch cá nhân thực chất tồn tại ở hai nhánh này hoặc chặn người khác, vẫn báo.
- Bug report gồm tiêu đề cụ thể, branch/commit đã kiểm tra, môi trường, các bước/dữ liệu tái hiện, expected theo yêu cầu, actual, bằng chứng không chứa secret, ảnh hưởng và FR/Issue/PR liên quan nếu biết. Không cần biết nguyên nhân hoặc tác giả code mới được báo.
- Tech Lead xác nhận bug, mức ưu tiên và owner sau khi xem nguyên nhân. Người phụ trách code liên quan là lựa chọn mặc định, nhưng lỗi có thể do tích hợp/cấu hình/yêu cầu; có thể giao người khác theo khả năng và thời gian. Giao sửa là trách nhiệm xử lý, không phải kết luận quy lỗi cá nhân.
- Owner sửa trên branch riêng từ `develop`, kiểm tra lại các bước tái hiện và chức năng liên quan, PR vào `develop`, sau đó qua cổng `main`.
- Bug chỉ `Done` sau khi kiểm tra bản `main` xác nhận hết lỗi. Bug nghiêm trọng trên `main` không phải chờ cuối tuần; Tech Lead tổ chức xử lý sớm.

### Đồng bộ GitHub Projects và Issue

- [GitHub Project #15](https://github.com/users/NgaiLong49423/projects/15) quản lý owner, SP, deadline, status và blocker; linked PR giữ bằng chứng kiểm tra/review.
- Không dùng automation `PR merged -> Done` hoặc `Issue closed -> Done` để bỏ qua nghiệm thu local sau merge. Không bật automation trong thay đổi tài liệu này.
---

## Engineering Autonomy Policy

Chính sách Tự chủ Kỹ thuật (Engineering Autonomy Policy) phân định rõ ràng giữa các quyết định kỹ thuật cục bộ thuộc quyền chủ động của người thực hiện (thành viên hoặc AI coding agent) và các thay đổi cấu trúc lớn cấp hệ thống đòi hỏi phải có Decision Issue cùng biểu quyết đồng thuận 3/5 từ nhóm. Chính sách này giúp loại bỏ nút thắt quy trình, tránh biến mọi điều chỉnh kỹ thuật thường nhật thành rào cản làm chậm tiến độ.

### 1. Database Schema Autonomy (Tự chủ tiến hóa Lược đồ Cơ sở dữ liệu)

- **Quyền tự chủ (Autonomous Evolution):** Developer và coding agent có toàn quyền tự chủ thực hiện các thay đổi schema cục bộ trên các bảng hiện có phục vụ trực tiếp cho Issue được giao:
  - Bổ sung cột mới trên bảng đã có.
  - Điều chỉnh nullability hoặc giá trị mặc định (DEFAULT) tương thích với yêu cầu nghiệp vụ.
  - Thêm index phục vụ tối ưu hóa hiệu năng truy vấn.
  - Thêm ràng buộc CHECK hoặc ràng buộc khóa ngoại (Foreign Key) **nhằm triển khai một mối quan hệ (relationship) đã được requirement hoặc schema baseline chấp thuận**.
  - *Thủ tục:* Không cần mở Decision Issue, không cần biểu quyết 3/5.
  - *Quy tắc bắt buộc:* Phải tạo file Flyway migration mới (append-only), cập nhật entity JPA, DTO, repository, unit/integration test tương ứng, và cập nhật snapshot `database/schema.sql`. Tuyệt đối không chỉnh sửa lịch sử migration đã chia sẻ (`V1`, `V2`).
  - *Ranh giới Diagram:* Quyền tự chủ schema **chỉ áp dụng** cho migration, JPA code, test và snapshot `database/schema.sql`. Nó **tuyệt đối không cấp quyền** chỉnh sửa ERD trong `docs/diagrams/ERD/`.
- **Ngưỡng bắt buộc Decision Issue & Biểu quyết 3/5 (Structural/Cross-module Redesign):**
  - Tạo bảng / thực thể mới trong cơ sở dữ liệu.
  - Xóa bảng hiện có.
  - Chia tách (split) hoặc gộp (merge) bảng.
  - **Tạo mối quan hệ (relationship) hoặc bản số (cardinality) mới chưa có trong baseline**, hoặc thay đổi mối quan hệ hiện tại.
  - Thay đổi kiểu dữ liệu phá vỡ tính tương thích (breaking data type change).
  - Thay đổi hệ quản trị cơ sở dữ liệu (DBMS) hoặc kiến trúc lưu trữ dữ liệu.

### 2. Dependency Autonomy (Tự chủ Thư viện và Phụ thuộc)

- **Baseline bắt buộc theo Technology Stack Authority:** Các technology, provider hoặc framework có trạng thái `Confirmed` trong [docs/architecture/TECHNOLOGY-STACK.md](docs/architecture/TECHNOLOGY-STACK.md) là baseline bắt buộc; thành viên và coding agent không được tự ý thay thế. Không hard-code technology hoặc version chưa được authority document chốt.
- **Quyền tự chủ (Auxiliary Implementation Dependencies):** Developer và coding agent có quyền tự chủ bổ sung các dependency phụ trợ khi có lý do kỹ thuật rõ ràng phục vụ cho Issue (ví dụ: thư viện kiểm thử như Testcontainers, công cụ mapping như MapStruct, utility helpers, client SDK phụ trợ, dev/build plugins).
  - *Quy tắc bắt buộc:* Khai báo tường minh trong `pom.xml` hoặc `package.json`, khóa phiên bản ổn định, và bắt buộc phải có mã nguồn hoặc test sử dụng thực tế trong dự án.
- **Ngưỡng bắt buộc Decision Issue & Biểu quyết 3/5 (Core/Project-wide Dependencies):**
  - Thêm một framework hoặc runtime mới thay thế hoặc cạnh tranh với Confirmed baseline.
  - Thay đổi ORM (Hibernate/JPA sang framework khác).
  - Thay đổi kiến trúc xác thực (Authentication/Security Framework).
  - Bổ sung Message Broker (Kafka, RabbitMQ) hoặc Distributed Cache (Redis).
  - Thay đổi database driver hoặc engine kết nối.

### 3. API Design Autonomy (Tự chủ Thiết kế API và Đồng bộ Hợp đồng)

- **Quyền tự chủ (Non-breaking API Additions):** Developer và coding agent có quyền tự chủ thiết kế các REST endpoint mới, request/response DTOs, cơ chế validation, và các stable business error codes cần thiết để hoàn thành nghiệp vụ của Issue.
  - *Quy tắc contract trong migration:* API implementation và OpenAPI annotations/DTOs phải làm generated `/v3/api-docs` phản ánh runtime contract. `docs/api/openapi.yaml` chỉ là planned/reference cho endpoint chưa implement; semantic đối chiếu endpoint đã implement thuộc review cho tới khi migration comparison tự động được thiết lập. Cập nhật `docs/api/API.md` khi convention tích hợp chung thay đổi. Không yêu cầu đồng bộ bản sao endpoint runtime vào manual YAML.
- **Ngưỡng yêu cầu phối hợp và phê duyệt (Breaking API Changes):**
  - Xóa bỏ một endpoint đang hoạt động.
  - Thay đổi URI path hoặc HTTP method của endpoint hiện có.
  - Xóa bỏ hoặc đổi tên các trường trong payload response mà client đang sử dụng.
  - Thay đổi kiểu dữ liệu các trường hiện có gây lỗi phân tích cú pháp (breaking type change).
  - Thay đổi mô hình xác thực/ủy quyền hoặc cấu trúc envelope lỗi dùng chung toàn hệ thống.
  - *Yêu cầu:* Phải mở trao đổi phối hợp với bên tiêu thụ (consumer), thống nhất phiên bản/kế hoạch chuyển đổi trước khi thực hiện.

### 4. Ranh giới bất biến: Bảo vệ Diagram Artifact Workspace

- `docs/diagrams/` là **human-maintained presentation workspace** (không gian trình diễn do con người duy trì và trình bày).
- **Engineering Autonomy tuyệt đối không mở rộng quyền ghi sang `docs/diagrams/`.** Toàn bộ thư mục này duy trì trạng thái **read-only** đối với AI coding agent theo mặc định.
- Việc thay đổi Flyway migration, `database/schema.sql`, JPA entity quan hệ, API contract hay tài liệu kỹ thuật **không tự động cấp quyền sửa, tạo mới, xóa, regenerate hoặc export diagram** trong `docs/diagrams/`.
- Coding agent chỉ được phép chỉnh sửa diagram khi và chỉ khi người dùng đưa ra **explicit authorization trong task hiện tại**, nêu rõ công việc diagram và định danh artifact cụ thể. Nếu không có ủy quyền cụ thể, agent chỉ được phép ghi nhận cảnh báo rằng diagram có thể đã cũ và cần con người cập nhật.

---

## Quy Tắc Đặt Tên Branch

**Branch** (nhánh) là một nhánh mã nguồn độc lập được tách ra từ nhánh chính (như `main` hoặc `master`) để phát triển tính năng hoặc sửa lỗi mà không làm ảnh hưởng trực tiếp đến mã nguồn hiện tại của dự án.

Khi làm việc, tạo nhánh từ `develop` và đặt tên theo cấu trúc:
`[loại-nhánh]/[issue-number]-[tên-ngắn-gọn]`

### Các tiền tố nhánh thông dụng:
* **`feature/`**: Sử dụng khi phát triển một tính năng mới.
* **`fix/`**: Sử dụng khi sửa lỗi (bug).
* **`docs/`**: Sử dụng khi cập nhật, sửa đổi tài liệu hướng dẫn hoặc tài liệu dự án.
* **`refactor/`**: Sử dụng khi tối ưu hóa, tái cấu trúc mã nguồn nhưng không làm thay đổi tính năng hệ thống.
* **`chore/`**: Sử dụng cho các công việc phụ trợ như thiết lập (setup) ban đầu, cấu hình dự án, cập nhật thư viện.

### Ví dụ cụ thể:
```text
feature/123-login-page
fix/124-database-connection
docs/125-update-readme
refactor/126-user-service
chore/127-project-setup
```

---

## Quy Tắc Viết Commit Message

Dự án này sử dụng tiêu chuẩn **Conventional Commits** để quản lý lịch sử commit. Định dạng chuẩn của một commit message như sau:

Toàn bộ commit message — subject/description, body và footer do contributor viết — **phải dùng tiếng Anh**, theo [Documentation Language Policy](docs/README.md#documentation-language-policy). Không dùng tiếng Việt trong commit message. Các token kỹ thuật như `type`, `scope`, `BREAKING CHANGE`, `Closes`, `Fixes` và `Refs` giữ đúng cú pháp quy định.

```text
<type>(<optional scope>): <description>

<optional body>

<optional footer>
```

**Giải thích chi tiết các thành phần:**
* **`type`** (bắt buộc): Loại thay đổi của commit (xem chi tiết ở phần bên dưới).
* **`scope`** (không bắt buộc): Phạm vi hoặc thành phần bị ảnh hưởng bởi thay đổi (ví dụ: `auth` - xác thực, `database` - cơ sở dữ liệu, `readme`).
* **`description`** (bắt buộc): Mô tả ngắn gọn về những gì đã thay đổi.
* **`body`** (không bắt buộc): Phần giải thích chi tiết hơn về nguyên nhân và cách thức thực hiện thay đổi.
* **`footer`** (không bắt buộc): Phần ghi chú cuối cùng, dùng để liên kết mã công việc (issue) hoặc đánh dấu thay đổi gây phá vỡ tương thích (breaking change).

---

## Các Loại Commit Type

Hãy chọn đúng loại `type` phù hợp với thay đổi của bạn:

* **`feat`** (feature): Thêm mới, cập nhật hoặc loại bỏ một chức năng/tính năng cho ứng dụng hoặc API/UI.
* **`fix`**: Sửa lỗi của một tính năng hay logic đã có trước đó.
* **`docs`**: Chỉ thực hiện thay đổi liên quan đến tài liệu (ví dụ: cập nhật file `README.md`, `CONTRIBUTING.md`).
* **`style`**: Chỉnh sửa định dạng hiển thị của code như khoảng trắng, xuống dòng, dấu chấm phẩy, thụt lề... mà không làm thay đổi logic hoạt động của chương trình.
* **`refactor`**: Tái cấu trúc mã nguồn nhằm tối ưu hóa, làm sạch code nhưng không làm thay đổi chức năng.
* **`perf`** (performance): Một dạng đặc biệt của `refactor` giúp tăng tốc độ xử lý hoặc tiết kiệm tài nguyên hệ thống.
* **`test`**: Thêm mới các bài kiểm tra tự động hoặc sửa đổi các bài kiểm tra (test) hiện có.
* **`build`**: Thay đổi liên quan đến công cụ xây dựng dự án (build tools), quản lý thư viện phụ thuộc (dependencies) hoặc phiên bản dự án (ví dụ: Maven, Gradle, npm).
* **`chore`**: Các tác vụ phụ trợ, không trực tiếp thay đổi code chạy của ứng dụng (ví dụ: sửa file `.gitignore`, cấu hình ban đầu).
* **`ops`**: Thay đổi liên quan đến vận hành, triển khai (deploy), hạ tầng, CI/CD (quy trình tự động xây dựng, kiểm thử và triển khai), giám sát hệ thống.

---

## Scope (Phạm vi)

* **`scope`** là phạm vi hoặc mô-đun bị tác động bởi commit đó.
* Việc điền `scope` là không bắt buộc, nhưng được khuyến khích sử dụng khi thay đổi chỉ nằm trong một khu vực cụ thể để dễ theo dõi.

### Quy tắc viết Scope:
* Không sử dụng mã công việc/lỗi (issue ID) để làm scope.
* Viết scope ngắn gọn, dễ hiểu và dùng chữ thường (lowercase).

### Ví dụ:
```text
feat(auth): add login form
fix(database): handle database connection failure
docs(readme): update usage guide
```

---

## Quy Tắc Viết Description

**`description`** là mô tả ngắn gọn về các thay đổi, được viết ngay sau dấu hai chấm và khoảng trắng `: `. Đây là phần bắt buộc của mỗi commit message.

### Quy tắc khi viết Description:
* Viết ngắn gọn, rõ nghĩa và đi thẳng vào vấn đề.
* Không viết hoa chữ cái đầu tiên của description.
* Không sử dụng dấu chấm (`.`) ở cuối câu.
* Dùng tiếng Anh ở dạng mệnh lệnh (ví dụ: `add`, `fix`, `remove`, `update`; không dùng dạng quá khứ như `added`, `fixed`).
* Tránh viết các commit mô tả chung chung, vô nghĩa.

### Ví dụ SAI:
```text
fix(auth): Fixed login bug.
update
aaa
```

### Ví dụ ĐÚNG:
```text
fix(auth): reject empty passwords during login
feat(cart): add cart persistence
docs: update README usage guide
```

---

## Commit Body (Nội dung chi tiết)

* **`body`** là phần không bắt buộc.
* Được sử dụng khi thay đổi phức tạp và bạn cần giải thích rõ **LÝ DO** thực hiện thay đổi đó hoặc cách giải quyết so với phiên bản trước.
* Giữa dòng tiêu đề và phần `body` cần có 1 dòng trống làm dấu phân cách.

### Ví dụ:
```text
fix(auth): validate password before login

Previously, an empty password could pass the initial validation step.
This change requires a non-empty password before authentication.
```

---

## Commit Footer (Chân trang)

* **`footer`** là phần cuối cùng của commit message, không bắt buộc.
* Thường dùng để liên kết mã công việc/lỗi hoặc ghi chú các thay đổi đột phá.
* Giữa phần `body` (hoặc tiêu đề nếu không có body) và phần `footer` cần có 1 dòng trống làm dấu phân cách.

**Giải thích thuật ngữ:**
* **Issue**: Các thẻ ghi nhận công việc, lỗi, hoặc yêu cầu tính năng được quản lý trên GitHub hoặc các công cụ quản lý dự án (như Jira, Trello).
* **Breaking Change**: Thay đổi lớn làm phá vỡ tính tương thích ngược, có khả năng làm mã nguồn cũ hoặc API cũ không hoạt động được nữa.

### Ví dụ:
```text
Closes #123
Fixes JIRA-456
BREAKING CHANGE: remove the legacy user profile endpoint
```

---

## Breaking Changes (Thay đổi đột phá)

Khi thay đổi của bạn làm ảnh hưởng trực tiếp đến khả năng chạy của hệ thống cũ (ví dụ: đổi tên hàm cốt lõi, đổi cấu trúc bảng database quan trọng):
* Đặt dấu chấm than `!` ngay trước dấu hai chấm ở tiêu đề commit: `<type>(<scope>)!: <description>`.
* Bắt buộc khai báo thông tin chi tiết bằng cụm từ `BREAKING CHANGE:` ở phần footer.

### Ví dụ:
```text
feat(api)!: remove the legacy user endpoint

BREAKING CHANGE: `/api/v1/users` has been removed and replaced by `/api/v2/users`.
```

---

## Ví Dụ Commit Chuyên Nghiệp

Dưới đây là một số ví dụ thực tế chuẩn hóa theo Conventional Commits:

```text
chore: init
feat(auth): add login page
fix(database): handle database connection failure
docs: update project description
refactor(user): simplify data validation logic
style: format Java files
test(auth): add login tests
build: update project dependencies
perf: reduce repeated database queries
chore: update .gitignore
```

---

## Quy Tắc Pull Request

**Pull Request** (yêu cầu gộp code / PR) là cách bạn yêu cầu những người quản lý dự án xem xét và gộp mã nguồn từ nhánh của bạn vào nhánh chính.

Để gửi một pull request thành công:
1. **Đặt tiêu đề rõ ràng:** Tiêu đề PR nên tuân theo định dạng tương tự commit message và dùng tiếng Anh (ví dụ: `feat(auth): add login page`).
2. **Mô tả chi tiết nội dung:** Điền đầy đủ thông tin vào mẫu PR, mô tả rõ các thay đổi bạn đã thực hiện và lý do thay đổi.
3. **Liên kết Issue:** PR vào `develop` và `main` dùng `Refs #123`. Tech Lead xác nhận và đóng Issue sau khi kiểm tra demo local trên `main` đạt; không dùng closing keywords để đóng trước nghiệm thu.
4. **Kiểm tra hoạt động:** Chắc chắn rằng dự án của bạn vẫn chạy được và không làm hỏng các tính năng cũ.
5. **Dọn dẹp code:** Đảm bảo không có code thừa, comment nháp hay các file rác trước khi gửi PR.

Các yêu cầu đồng bộ integration baseline cho PR vào `develop` được quy định tại [Develop integration baseline và đồng bộ PR song song](#develop-integration-baseline); không lặp lại checklist tại đây.

---

## Checklist Trước Khi Push Code

Trước khi thực hiện lệnh `git push` để đẩy code lên GitHub, hãy kiểm tra danh sách sau:

- [ ] Code đã biên dịch và chạy thành công trên máy cá nhân.
- [ ] Không có file rác, file build tạm hoặc file cấu hình cá nhân trong danh sách commit.
- [ ] Tất cả các commit message đều tuân thủ đúng định dạng Conventional Commits.
- [ ] Tài liệu hướng dẫn liên quan đã được cập nhật đầy đủ (nếu có thay đổi cách sử dụng).
- [ ] Các tập tin script cơ sở dữ liệu đã được cập nhật đầy đủ (nếu có thay đổi schema database).

## File Placement

Before creating or moving a file, use [the repository layout and document register](docs/README.md). Keep product notes out of the repository root. Register a new maintained document with its purpose and read trigger; scratch files are not registered or automatically read.

GitHub Issues/Projects manage progress, owners, dates and blockers; linked PRs hold review/test evidence. No individual or weekly report files are required. Audit findings stay in the conversation unless a saved export is explicitly requested.

## Shared Editing Rules

Preserve existing contributor changes and stay within the requested scope. Structural changes to architecture, new database tables or relationships, breaking API changes, or project-wide core dependencies require explicit authorization and decision records according to the [Engineering Autonomy Policy](#engineering-autonomy-policy). Local schema evolution, auxiliary dependencies, and non-breaking API additions needed by an assigned Issue are permitted under autonomous engineering conventions with appropriate tests, migrations, and contract documentation updates. Presentation diagrams under `docs/diagrams/` remain protected and read-only for agents unless explicitly authorized. Follow the [Documentation Language Policy](docs/README.md#documentation-language-policy). Update affected links and metadata, and record meaningful changes in the changelog. Verify against the actual repository and state any failed or unavailable checks.

## Changelog Format

Write all `CHANGELOG.md` headings and prose in English according to the [Documentation Language Policy](docs/README.md#documentation-language-policy), using dated topic entries in reverse chronological order. Do not add Vietnamese prose to a new or edited entry. Each entry explains what changed on that date and why it matters. Keep these three sections in this order: Added, Changed, Fixed. Use "None." when a category has no changes; record removals explicitly under Changed. Do not use a single accumulating [Unreleased] section or copy tutorial/example history into the changelog.

Each entry has:

1. A heading: `## YYYY-MM-DD — Specific Topic`. Append a linked PR reference when its association is verified; omit it when no PR is known.
2. `**Status:**` with Working tree (uncommitted changes) or Committed plus a verified commit hash. A commit alone does not prove a PR was merged or a release was deployed.
3. `**Scope:**` describing the coherent change, followed by Added, Changed and Fixed.

Several distinct topics may share a date. Update the same topic entry while the same work is in progress. When committed, update its status/reference in place instead of copying it into another entry. Keep committed work separate from later uncommitted changes.

Use the evidenced change date (project timezone: Asia/Saigon), not the date on which old history is reformatted. If only a commit/checkpoint date is known, say so in the entry. Never invent dates, PR numbers, commit hashes or release status. Historical document versions may be retained to explain successive decisions; the metadata Version tracks the changelog document, not the application.

Template (placeholders below are instructions, not real history):

```markdown
## YYYY-MM-DD — Specific Topic

**Status:** Working tree — not committed.

**Scope:** Explain the change and its purpose.

### Added

- Describe new capabilities or documents, or write None.

### Changed

- Describe updates or removals, or write None.

### Fixed

- Describe corrections, or write None.
```

When a real PR is verified, append `([PR #N](verified-PR-URL))` to the heading. A committed entry uses `**Status:** Committed — VERIFIED_HASH.` Keep links in the project repository and omit unknown references. Preparing the changelog does not itself commit, create a PR, merge or publish anything.
