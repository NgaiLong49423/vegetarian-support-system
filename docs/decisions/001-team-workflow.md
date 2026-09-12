> **Document:** Team Workflow Decision 001  
> **File:** `docs/decisions/001-team-workflow.md`  
> **Version:** v1.4.0  
> **Created:** 2026-09-07  
> **Last Updated:** 2026-09-09  
> **Status:** Active  

# Quyết Định 001: Workflow Làm Việc Nhóm

## Bối cảnh

Template cần một workflow dùng được cho nhóm sinh viên có lịch học khác nhau, trước khi biết chính xác ai sẽ giữ vai trò Team Lead, PM hay Tech Lead.

Nguồn tham khảo và ranh giới giữa hướng dẫn bên ngoài với quy ước của nhóm được ghi tại [`WORKFLOW-SOURCES.md`](WORKFLOW-SOURCES.md).

## Quyết định

### 1. Luồng nhánh

```text
Backlog -> Planning -> In Progress -> Review -> Done

feature branch -> Pull Request vào develop -> review, tích hợp và chờ release -> release Pull Request vào main -> Done + đóng Issue
```

* `main` luôn giữ bản ổn định có thể demo.
* `develop` dùng để tích hợp các task hoàn thành về mặt kỹ thuật và chạy thử.
* Không merge trực tiếp feature branch vào `main`.
* Board chỉ dùng năm trạng thái: `Backlog -> Planning -> In Progress -> Review -> Done`.
* `Planning` là lúc task đã được làm rõ và chuẩn bị để code, nhưng chưa lập trình.
* `In Progress` là lúc đang code hoặc tự kiểm tra trên feature branch tạo từ `develop`.
* `Review` bắt đầu khi Pull Request vào `develop` được mở. Sau khi PR đó merge vào `develop`, Issue vẫn ở `Review` để chờ release vào `main`.
* `Done` chỉ xảy ra sau khi release được merge vào `main`; Issue được đóng cùng lúc.
* Khi bị vướng, dùng label `Blocked`; không tạo thêm cột trạng thái.

### 2. Release

* Mặc định, nhóm tạo một release từ `develop` sang `main` mỗi tuần trước buổi demo.
* Có thể tạo thêm release cho mốc quan trọng.
* Không merge chỉ vì đến lịch. Release PR chỉ được merge khi đạt cổng hoàn tất dự án:
  * Source branch là `develop`, target branch là `main`; release PR liệt kê từng Issue được phát hành bằng `Closes #<issue-number>`.
  * Mỗi Issue trong release đã qua cổng hoàn tất kỹ thuật và thay đổi tương ứng đang nằm trong `develop`.
  * Không sửa chức năng trực tiếp trong release PR. Lỗi phát hiện trong lúc chuẩn bị release phải được sửa qua PR khác vào `develop`.
  * Build và automated tests chạy thành công trên commit mới nhất của `develop`. Nếu chưa có automated tests, release PR ghi rõ manual test và kết quả.
  * Các luồng demo chính và sự kết hợp giữa những tính năng vừa tích hợp đã được kiểm tra.
  * Không còn blocker hoặc lỗi nghiêm trọng đã biết. Lỗi nhỏ được chấp nhận phải có Issue riêng và được ghi trong release PR.
  * Nếu có thay đổi database, migration/schema đã được thử trên database sạch hoặc môi trường kiểm tra tương đương.
  * README, tài liệu chạy, API docs và `CHANGELOG.md` đã được cập nhật khi liên quan.
  * Mọi review conversation quan trọng đã được giải quyết, các required checks đã đạt và release PR có ít nhất hai approval.
  * Không bypass required checks hoặc branch protection để kịp lịch demo.
* Sau khi merge, nhóm chạy smoke test ngắn trên commit của `main`, xác nhận `main` build/chạy được; các Issue được đóng và chuyển sang `Done`.
* Nếu smoke test sau merge thất bại, mở lại Issue bị ảnh hưởng, chuyển về `Review`, tạo Bug Issue và dừng release tiếp theo cho đến khi nhóm quyết định `revert` hay `hotfix`.

### 3. Definition of Ready (`Planning` -> `In Progress`)

Definition of Ready (DoR) là checklist phối hợp do nhóm chọn, không phải artifact bắt buộc của Scrum. Một Issue chỉ chuyển từ `Planning` sang `In Progress` khi:

- [ ] Mục tiêu và kết quả mong đợi được mô tả rõ, có `Source Trace` đến yêu cầu hoặc quyết định nguồn.
- [ ] Phạm vi và phần ngoài phạm vi đã đủ rõ cho task; không còn câu hỏi nghiệp vụ quan trọng làm thay đổi hướng thực hiện.
- [ ] Acceptance Criteria có thể kiểm tra được.
- [ ] Có đúng một owner chịu trách nhiệm cập nhật tiến độ.
- [ ] Đã có Type, Priority và Story Points; Issue triển khai không vượt quá `5 SP`.
- [ ] Có Start Date và Target Date, với thời lượng dự kiến không quá 4–5 ngày lịch.
- [ ] Dependency và blocker đã được xác định; blocker ngăn việc bắt đầu đã được xử lý hoặc có kế hoạch xử lý rõ.
- [ ] Người dự kiến thực hiện đã tham gia hoặc xác nhận ước lượng.

DoR không yêu cầu đặc tả hoàn hảo. Nếu checklist tạo thêm thủ tục nhưng không giúp giảm hiểu sai hoặc blocker, nhóm phải rút gọn sau khi retrospective.

`Target Date` là hạn để owner hoàn tất cổng kỹ thuật và merge feature PR vào `develop`; đây không phải hạn release vào `main`. Sau khi PR đã merge vào `develop`, Issue tiếp tục ở `Review` để chờ release nhưng owner không bị tính là trễ. Nếu PR vẫn đang mở hoặc còn yêu cầu sửa, hạn vẫn còn hiệu lực cho đến khi đạt cổng kỹ thuật.

### 4. Task và trễ hạn

* Một task có một owner, đầu ra kiểm tra được, tiêu chí hoàn thành rõ ràng và thời hạn tối đa 4–5 ngày lịch.
* Owner cập nhật vào ngày thứ 2 hoặc 3. Nếu có nguy cơ trễ, phải báo trước hạn, nêu phần còn lại và ước lượng mới.
* Khi trễ vì scope lớn, tách phần chưa hoàn thành thành Issue mới. Khi bị blocker, gắn label `Blocked` và nêu rõ trợ giúp cần thiết. Không tiến độ hoặc không cập nhật thì đưa task lại backlog để phân công lại.

### 5. Review Pull Request

* PR chỉ được merge vào `develop` khi đạt cổng hoàn tất kỹ thuật:
  * Liên kết đúng Issue và thay đổi không vượt ngoài scope đã thống nhất.
  * Tất cả Acceptance Criteria đã được kiểm tra và PR ghi rõ cách kiểm tra cùng kết quả.
  * Dự án build/chạy được; automated test liên quan đã chạy nếu có. Khi chưa có automated test, PR phải ghi bằng chứng manual test.
  * Không còn lỗi blocker hoặc lỗi nghiêm trọng đã biết.
  * Không chứa secret, `.env`, credential, file build hoặc file cá nhân.
  * Mọi yêu cầu sửa đổi và review conversation quan trọng đã được giải quyết.
  * Nếu có thay đổi database, API, cấu hình hoặc cách chạy, tài liệu, migration/schema, script và ERD liên quan đã được cập nhật.
  * Phần chưa hoàn thành được tách thành Issue mới; không coi một phần implementation là toàn bộ scope.
* PR vào `develop` cần ít nhất một reviewer khác tác giả.
* PR liên quan database, authentication, cấu trúc dùng chung hoặc release vào `main` cần ít nhất hai người kiểm tra.
* Khi PR vào `develop` đạt cổng trên và được merge, Issue được xem là hoàn tất kỹ thuật nhưng vẫn ở `Review` để chờ release.
* Khi reviewer yêu cầu sửa, Issue vẫn ở `Review` trong lúc tác giả cập nhật PR. Chỉ đưa lại `In Progress` nếu PR bị đóng hoặc cần làm lại đáng kể.

### 6. Đồng bộ GitHub Projects và Issue

* Không bật workflow tổng quát `Pull request merged -> Done` nếu workflow đó không lọc được nhánh đích `main`.
* Chỉ bật automation đóng Issue khi trạng thái `Done` được kích hoạt sau merge vào `main`.

### 7. Story Points

* Dùng thang `1, 2, 3, 5, 8` để ước lượng độ lớn và cân tải khi lên kế hoạch.
* Task `8 SP` phải được bẻ nhỏ trước khi nhận.
* SP không dùng để xếp hạng, thưởng/phạt hay quy đổi trực tiếp từ review. SP đã hoàn thành chỉ là một tín hiệu cùng với Issue, PR, review, test, tài liệu và demo để nhìn đóng góp.

## Chưa quyết định

* Ai giữ Team Lead, PM, Tech Lead hoặc quyền merge cụ thể.
* Thời hạn phản hồi PR.
* Có triển khai bot nhắc task quá hạn hay không, và dùng kênh nào.

## Lý do

Workflow giữ `main` luôn ổn định để demo, cho phép tích hợp sớm trên `develop`, và có bằng chứng rõ ràng về công việc mà không biến nhóm thành hệ thống chấm điểm cá nhân.
