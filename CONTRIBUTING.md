> **Document:** Contribution Guide  
> **File:** `CONTRIBUTING.md`  
> **Version:** v3.2.1
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-19
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

- Thành viên phát triển và debug trên local; `develop` là nhánh tích hợp, `main` là phiên bản ổn định để demo.
- Nhánh làm việc tách từ `develop`; thay đổi đi qua PR vào `develop`, rồi PR `develop -> main`.
- `Planning`: làm rõ scope, Acceptance Criteria, dependency và kế hoạch trước khi code. `In Progress`: triển khai và tự kiểm tra.
- `Review` bắt đầu khi mở PR vào `develop`; sau merge Issue vẫn ở `Review` chờ nghiệm thu trên `main`. Nếu PR bị đóng hoặc cần làm lại đáng kể, chuyển về `In Progress`.
- `Done` chỉ sau khi toàn bộ scope/Acceptance Criteria đạt trên `main` và kiểm tra demo local sau merge đạt. Merge vào `develop` hoặc `main` riêng lẻ chưa đủ.
- Không cần hoàn thành cả module SRS mới đưa code lên `main`. Phạm vi PR gồm những thay đổi ổn định và dependency đã được đáp ứng.
- Hiện chưa deploy hoặc bật CD. Nhóm dự kiến deploy cả hệ thống lên Azure khi app đạt điều kiện ổn định; deployment là công việc riêng, không phải điều kiện `Done` của từng FR ở giai đoạn demo local. Điều này không thay đổi lựa chọn công nghệ Azure.
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
- Phân rã FR theo SRS trước, chốt API contract cho FR sắp làm trước khi tích hợp FE/BE. Owner phụ trách BE đề xuất endpoint, request/response, validation, lỗi và quyền truy cập; kiểm tra dữ liệu đủ phục vụ giao diện; Tech Lead review và duyệt.
- Contract chưa duyệt phải ghi rõ là đề xuất. Không tự quyết API khác nhau ở FE/BE. Bản duyệt có một nơi tham chiếu chung theo quy tắc placement; thay đổi phải được thống nhất, cập nhật và báo bên bị ảnh hưởng.
- Không cần thiết kế toàn bộ API hoặc tạo Issue/format riêng cho contract của mọi FR. FE có thể chuẩn bị giao diện/mock theo contract; phần tích hợp chờ contract được duyệt.
- Owner ghi phần AI hỗ trợ và bằng chứng người thực hiện đã xác minh: kiểm tra API, thao tác từ UI, các test đã chạy và kết quả. AI báo hoàn thành không phải bằng chứng nghiệm thu.

### Template bắt buộc cho PR và Bug Issue

- Mọi PR vào `develop` hoặc `main`, kể cả PR do AI agent chuẩn bị hay tạo qua CLI/API, phải dùng [PR template của dự án](.github/pull_request_template.md).
- Mọi Bug Issue phải dùng [Bug Report form của dự án](.github/ISSUE_TEMPLATE/bug_report.yml). Khi tạo qua CLI/API, body phải giữ các mục và thông tin bắt buộc tương ứng với form hiện hành.
- Giữ cấu trúc, các mục và checklist của template; điền nội dung theo thay đổi thực tế. Mục không áp dụng ghi `Không áp dụng` kèm lý do hoặc bỏ qua theo chỉ dẫn sẵn trong template. Không tự thay bằng format mới hoặc lược bỏ thông tin bắt buộc.
- Quy tắc áp dụng cho cả thành viên và AI agent. Nếu template cần thay đổi, đề xuất để Tech Lead duyệt và cập nhật template chung trước khi sử dụng cấu trúc mới.

### PR vào `develop`

- PR dùng `Refs #<issue-number>`, ghi scope, thay đổi và kết quả tự kiểm tra; không chứa secret, `.env`, credential hoặc file build/cá nhân.
- Review có thể được request nhưng không bắt buộc approval. GitHub Actions không phải cổng bắt buộc cho merge vào `develop`.
- Owner vẫn tự kiểm tra phần thay đổi và ghi trung thực phần chưa kiểm tra/blocker. Cổng nhẹ cho phép tích hợp sớm, không xác nhận FR đã hoàn thành.
- Một Issue có thể có nhiều PR liên quan; không ép quan hệ một Issue/một branch/một PR.

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

**Trạng thái công cụ:** tại kiểm tra repository ngày 2026-09-18, `.github/workflows/release-source.yml` chỉ kiểm tra source PR vào `main` phải là `develop`; chưa có workflow build/test trong thư mục này. Cổng build/test bắt buộc ở trên là quyết định đã chốt, chưa có bằng chứng được triển khai đầy đủ. Khi đưa code triển khai vào `main`, phải có CI build/test phù hợp và xác minh required checks; tài liệu này không tự cấu hình GitHub hoặc tạo pipeline.

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
- Source Trace và lifecycle requirement tuân theo SRS và quy tắc requirement-to-Issue trong AGENTS.md; workflow không tự tạo hoặc đồng bộ Issue thật.

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

Preserve existing contributor changes and stay within the requested scope. Changes to architecture, database schema, public APIs or core dependencies require authorization. Follow the [Documentation Language Policy](docs/README.md#documentation-language-policy). Update affected links and metadata, and record meaningful changes in the changelog. Verify against the actual repository and state any failed or unavailable checks.

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
