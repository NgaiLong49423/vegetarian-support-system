> **Document:** Contribution Guide  
> **File:** `CONTRIBUTING.md`  
> **Version:** v2.0.1  
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-13  
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

Workflow này áp dụng cho Vegetarian Support System. Quy tắc owner, reviewer/backup, WIP, Release Coordinator luân phiên và cách ra quyết định cho nhóm 5 người nằm tại [`docs/decisions/002-five-member-team-operating-agreement.md`](docs/decisions/002-five-member-team-operating-agreement.md).

### Luồng nhánh và release

```text
Backlog -> Planning -> In Progress -> Review -> Done

feature branch -> Pull Request vào develop -> review, tích hợp và chờ release -> release Pull Request vào main -> Done + đóng Issue
```

* `main` luôn là bản ổn định có thể dùng để demo.
* `develop` là nơi tích hợp các task đã hoàn thành về mặt kỹ thuật trước khi phát hành bản demo.
* Mỗi task bắt đầu từ một nhánh tách từ `develop` và chỉ được đưa vào `develop` qua Pull Request (PR).
* Mặc định, nhóm tạo một release từ `develop` sang `main` mỗi tuần trước buổi demo. Không merge chỉ vì đến lịch: release phải đạt checklist bên dưới. Nhóm có thể tạo thêm release khi có mốc quan trọng.
* `Planning` là lúc đã làm rõ yêu cầu và chuẩn bị để code; chưa bắt đầu lập trình.
* `In Progress` là lúc đang code hoặc tự kiểm tra trên feature branch tạo từ `develop`.
* `Review` bắt đầu khi đã mở Pull Request vào `develop`. Sau khi PR được merge vào `develop`, Issue vẫn ở `Review` để chờ đợt release vào `main`.
* Chỉ sau khi release được merge vào `main`, Issue mới chuyển sang `Done` và được đóng.
* Không tạo thêm cột Blocked. Khi bị vướng, gắn label `⛔ Blocked` vào Issue và ghi rõ trợ giúp cần thiết.

### Definition of Ready: từ `Planning` sang `In Progress`

Definition of Ready là checklist phối hợp của nhóm, không phải trạng thái mới trên board. Trước khi bắt đầu code, Issue phải có:

- [ ] Mục tiêu và `Source Trace` rõ ràng.
- [ ] Phạm vi đủ rõ và Acceptance Criteria có thể kiểm tra.
- [ ] Đúng một owner; người thực hiện đã tham gia hoặc xác nhận ước lượng.
- [ ] Type, Priority và Story Points; Issue triển khai không vượt quá `5 SP`.
- [ ] Start Date và Target Date trong tối đa 4–5 ngày lịch.
- [ ] Dependency và blocker được ghi rõ; không còn câu hỏi nghiệp vụ quan trọng cản trở việc bắt đầu.

Không dùng checklist này để đòi tài liệu hoàn hảo hoặc trì hoãn trao đổi. Cơ sở của quy tắc và các phần chỉ là quy ước nhóm được ghi tại [`docs/decisions/WORKFLOW-SOURCES.md`](docs/decisions/WORKFLOW-SOURCES.md).

`Target Date` là hạn để owner hoàn tất cổng kỹ thuật và merge feature PR vào `develop`, không phải hạn release vào `main`. Sau khi PR đã merge vào `develop`, Issue vẫn ở `Review` để chờ release nhưng owner không bị tính là trễ. Nếu PR còn mở hoặc còn yêu cầu sửa, hạn vẫn tiếp tục có hiệu lực.

### Checklist release vào `main`

- [ ] Source branch là `develop`, target branch là `main`.
- [ ] Release PR liệt kê từng Issue được phát hành bằng `Closes #<issue-number>`.
- [ ] Mỗi Issue trong release đã qua cổng hoàn tất kỹ thuật và code tương ứng đang nằm trong `develop`.
- [ ] Không sửa chức năng trực tiếp trong release PR; mọi bản sửa đi qua PR khác vào `develop`.
- [ ] Build và automated tests đạt trên commit mới nhất; nếu chưa có automated tests, release PR ghi manual test và kết quả.
- [ ] Các luồng demo chính và sự kết hợp giữa những tính năng vừa tích hợp đã được kiểm tra.
- [ ] Không còn blocker hoặc lỗi nghiêm trọng đã biết; lỗi nhỏ được chấp nhận có Issue riêng và được ghi trong release PR.
- [ ] Database migration/schema đã được thử trên database sạch hoặc môi trường kiểm tra tương đương nếu có thay đổi database.
- [ ] README, tài liệu chạy, API docs và `CHANGELOG.md` đã cập nhật khi liên quan.
- [ ] Mọi review conversation quan trọng đã được giải quyết và required checks đã đạt.
- [ ] Release PR có ít nhất hai approval.
- [ ] Không bypass required checks hoặc branch protection để kịp lịch demo.

Sau khi merge, nhóm chạy smoke test ngắn trên commit của `main`. Nếu đạt, xác nhận các Issue đã đóng và chuyển sang `Done`. Nếu thất bại, mở lại Issue bị ảnh hưởng, chuyển về `Review`, tạo Bug Issue và dừng release tiếp theo cho đến khi nhóm quyết định `revert` hay `hotfix`.

### Task, tiến độ và Story Points

* Một task có đúng một owner, đầu ra có thể kiểm tra, tiêu chí hoàn thành rõ ràng, và thời hạn tối đa 4–5 ngày lịch.
* Đến ngày thứ 2 hoặc 3, owner cập nhật trên Issue: phần đã làm, phần còn lại và blocker (nếu có).
* Nếu dự kiến trễ, owner báo trước hạn và nêu phần còn lại cùng ước lượng mới. Không tự kéo dài hạn trong im lặng.
* Nếu task trễ vì scope quá lớn, chỉ merge phần đã hoàn thành; phần còn lại được tách thành Issue mới. Nếu bị blocker, gắn label `⛔ Blocked` và nêu rõ trợ giúp cần thiết. Khi không có tiến độ hoặc cập nhật, task được đưa lại vào backlog để nhóm phân công lại.
* Dùng Story Points (SP) theo thang `1, 2, 3, 5, 8` để ước lượng độ lớn và cân tải khi lập kế hoạch. Task `8 SP` phải được bẻ nhỏ trước khi nhận.
* SP không là điểm xếp hạng hay kỷ luật cá nhân. Chỉ tính SP đã hoàn thành khi Issue đạt Done; dùng SP cùng với lịch học, blocker và độ phù hợp để cân tải task mới.

### Pull Request và review

PR chỉ được merge vào `develop` khi toàn bộ checklist sau đạt:

- [ ] PR dùng `Refs #<issue-number>` để liên kết đúng Issue và thay đổi không vượt ngoài scope đã thống nhất.
- [ ] Tất cả Acceptance Criteria đã được kiểm tra.
- [ ] PR ghi rõ cách kiểm tra và kết quả; chạy automated tests liên quan nếu dự án có test, hoặc cung cấp bằng chứng manual test nếu chưa có.
- [ ] Dự án build và chạy được; không còn lỗi blocker hoặc lỗi nghiêm trọng đã biết.
- [ ] Không chứa secret, `.env`, credential, file build hoặc file cá nhân.
- [ ] Mọi yêu cầu sửa đổi và review conversation quan trọng đã được giải quyết.
- [ ] Tài liệu, API docs, database migration/schema, script và ERD được cập nhật khi thay đổi có liên quan.
- [ ] Phần chưa hoàn thành được tách thành Issue mới.

* Mỗi PR vào `develop` cần ít nhất một reviewer khác tác giả.
* PR liên quan database, authentication, cấu trúc dùng chung hoặc release vào `main` cần ít nhất hai người kiểm tra.
* Khi PR được merge vào `develop`, Issue vẫn ở `Review` và được xem là hoàn tất kỹ thuật, đang chờ release.
* Khi reviewer yêu cầu sửa, Issue vẫn ở `Review`. Chỉ đưa lại `In Progress` khi PR bị đóng hoặc cần làm lại đáng kể.
* Review không phải là một cuộc đua lấy điểm.
* Bằng chứng đóng góp gồm Issue, PR, review có nội dung, test, tài liệu và demo; không chỉ dựa vào số SP.

### Đồng bộ GitHub Projects và Issue

* Không bật workflow tổng quát `Pull request merged -> Done` nếu nó không lọc được nhánh đích `main`; merge feature vào `develop` sẽ làm Issue Done quá sớm.
* Chỉ dùng automation đóng Issue khi trạng thái `Done` đã được thiết kế để xảy ra sau merge vào `main`.

**Giải thích thuật ngữ:**
* **Commit** (lần lưu): Hành động lưu lại trạng thái thay đổi của các file mã nguồn vào lịch sử Git tại máy cá nhân.
* **Push** (đẩy code): Hành động gửi các commit từ máy tính cá nhân (local) lên kho lưu trữ trực tuyến trên GitHub.
* **Repository** (kho lưu trữ / repo): Nơi lưu trữ toàn bộ mã nguồn, tài liệu và lịch sử các phiên bản của dự án.

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
feat(auth): thêm form đăng nhập
fix(database): xử lý lỗi mất kết nối database
docs(readme): cập nhật hướng dẫn sử dụng
```

---

## Quy Tắc Viết Description

**`description`** là mô tả ngắn gọn về các thay đổi, được viết ngay sau dấu hai chấm và khoảng trắng `: `. Đây là phần bắt buộc của mỗi commit message.

### Quy tắc khi viết Description:
* Viết ngắn gọn, rõ nghĩa và đi thẳng vào vấn đề.
* Không viết hoa chữ cái đầu tiên của description.
* Không sử dụng dấu chấm (`.`) ở cuối câu.
* Nên viết theo dạng mệnh lệnh (ví dụ: dùng các từ như `thêm`, `sửa`, `xóa` thay vì `đã thêm`, `đã sửa`).
* Tránh viết các commit mô tả chung chung, vô nghĩa.

### Ví dụ SAI:
```text
fix(auth): Fixed login bug.
update
aaa
```

### Ví dụ ĐÚNG:
```text
fix(auth): sửa lỗi đăng nhập khi password rỗng
feat(cart): thêm chức năng lưu giỏ hàng
docs: cập nhật hướng dẫn sử dụng README
```

---

## Commit Body (Nội dung chi tiết)

* **`body`** là phần không bắt buộc.
* Được sử dụng khi thay đổi phức tạp và bạn cần giải thích rõ **LÝ DO** thực hiện thay đổi đó hoặc cách giải quyết so với phiên bản trước.
* Giữa dòng tiêu đề và phần `body` cần có 1 dòng trống làm dấu phân cách.

### Ví dụ:
```text
fix(auth): kiểm tra password trước khi đăng nhập

Trước đây hệ thống cho phép password rỗng đi qua bước kiểm tra.
Thay đổi này bổ sung điều kiện bắt buộc nhập password trước khi xác thực.
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
BREAKING CHANGE: xóa endpoint user profile cũ
```

---

## Breaking Changes (Thay đổi đột phá)

Khi thay đổi của bạn làm ảnh hưởng trực tiếp đến khả năng chạy của hệ thống cũ (ví dụ: đổi tên hàm cốt lõi, đổi cấu trúc bảng database quan trọng):
* Đặt dấu chấm than `!` ngay trước dấu hai chấm ở tiêu đề commit: `<type>(<scope>)!: <description>`.
* Bắt buộc khai báo thông tin chi tiết bằng cụm từ `BREAKING CHANGE:` ở phần footer.

### Ví dụ:
```text
feat(api)!: xóa endpoint user cũ

BREAKING CHANGE: `/api/v1/users` đã bị xóa và được thay bằng `/api/v2/users`.
```

---

## Ví Dụ Commit Chuyên Nghiệp

Dưới đây là một số ví dụ thực tế chuẩn hóa theo Conventional Commits:

```text
chore: init
feat(auth): thêm trang đăng nhập
fix(database): xử lý lỗi mất kết nối database
docs: cập nhật mô tả dự án
refactor(user): đơn giản hóa logic kiểm tra dữ liệu
style: định dạng lại các file Java
test(auth): thêm test kiểm tra đăng nhập
build: cập nhật dependency của dự án
perf: giảm số lần truy vấn database lặp lại
chore: cập nhật .gitignore
```

---

## Quy Tắc Pull Request

**Pull Request** (yêu cầu gộp code / PR) là cách bạn yêu cầu những người quản lý dự án xem xét và gộp mã nguồn từ nhánh của bạn vào nhánh chính.

Để gửi một pull request thành công:
1. **Đặt tiêu đề rõ ràng:** Tiêu đề PR nên tuân theo định dạng tương tự commit message (ví dụ: `feat(auth): thêm trang đăng nhập`).
2. **Mô tả chi tiết nội dung:** Điền đầy đủ thông tin vào mẫu PR, mô tả rõ các thay đổi bạn đã thực hiện và lý do thay đổi.
3. **Liên kết Issue:** Feature PR vào `develop` dùng `Refs #123` để liên kết mà chưa đóng Issue. Release PR vào `main` dùng `Closes #123` cho các Issue sẽ hoàn tất khi release được merge.
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

Preserve existing contributor changes and stay within the requested scope. Changes to architecture, database schema, public APIs or core dependencies require authorization. Write new governance documentation in professional English; preserve existing Vietnamese product documents unless translation is requested. Update affected links and metadata, and record meaningful changes in the changelog. Verify against the actual repository and state any failed or unavailable checks.

## Changelog Format

Write CHANGELOG.md in English, using dated topic entries in reverse chronological order. Each entry explains what changed on that date and why it matters. Keep these three sections in this order: Added, Changed, Fixed. Use "None." when a category has no changes; record removals explicitly under Changed. Do not use a single accumulating [Unreleased] section or copy tutorial/example history into the changelog.

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
