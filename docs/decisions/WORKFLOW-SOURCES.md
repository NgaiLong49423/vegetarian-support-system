> **Document:** Workflow Evidence Register  
> **File:** `docs/decisions/WORKFLOW-SOURCES.md`  
> **Version:** v1.4.2
> **Created:** 2026-09-08  
> **Last Updated:** 2026-09-14
> **Status:** Active  

# Nguồn và bằng chứng cho workflow

## Mục đích

Tài liệu này ghi lại phần nào trong workflow của nhóm đến từ hướng dẫn bên ngoài hoặc hành vi của platform, và phần nào là convention do chính nhóm lựa chọn. External source cung cấp căn cứ cho quyết định; chúng không tự động ghi đè các constraint mà nhóm đã ghi nhận.

## Phân loại bằng chứng

| Phân loại | Ý nghĩa |
|---|---|
| `Platform behavior` | Hành vi được vendor của công cụ ghi nhận và workflow của nhóm phải tính đến |
| `Framework guidance` | Hướng dẫn từ một framework được công nhận; có thể cần điều chỉnh theo bối cảnh dự án |
| `Complementary practice` | Practice hữu ích nhưng không phải thành phần bắt buộc của framework |
| `Team convention` | Quy ước local do nhóm sinh viên lựa chọn cho dự án này |

## Danh mục nguồn

| Nguồn | Phân loại bằng chứng | Nguồn này xác lập điều gì | Dự án sử dụng như thế nào |
|---|---|---|---|
| [The Scrum Guide (2020)](https://scrumguides.org/scrum-guide.html) | Framework guidance | Product Backlog refinement bổ sung chi tiết, thứ tự và kích thước; người thực hiện công việc chịu trách nhiệm sizing; công việc phải thỏa Definition of Done trước khi được xem là hoàn tất | Làm căn cứ cho refinement có nguồn, developer tham gia estimate, tiêu chí hoàn tất có thể kiểm tra và Definition of Done rõ ràng |
| [Scrum.org: Ready or Not? Demystifying the Definition of Ready in Scrum](https://www.scrum.org/resources/blog/ready-or-not-demystifying-definition-ready-scrum) | Complementary practice | Definition of Ready có thể giúp nhóm làm rõ công việc trước khi bắt đầu, nhưng không phải Scrum artifact chính thức và không nên trở thành gate nặng nề hoặc thay thế collaboration | Nhóm dùng readiness checklist ngắn trước khi chuyển Issue từ `Planning` sang `In Progress`, sau đó review và điều chỉnh |
| [GitHub Docs: About Projects](https://docs.github.com/en/issues/planning-and-tracking-with-projects/learning-about-projects/about-projects) | Platform behavior | GitHub Projects cung cấp view, field và automation có thể cấu hình nhưng không áp đặt một phương pháp quản lý dự án cụ thể | Hỗ trợ board năm trạng thái và các field do nhóm tự cấu hình; trạng thái cụ thể vẫn là Team convention |
| [GitHub Docs: Using the built-in automations](https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-built-in-automations) | Platform behavior | Built-in workflow có thể cập nhật trạng thái item khi Pull Request được merge hoặc Issue được đóng | Automation tổng quát `merged PR -> Done` vẫn tắt nếu không phân biệt được release merge vào `main` với feature merge vào `develop` |
| [GitHub Docs: Linking a pull request to an issue](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue) | Platform behavior | Closing keyword sẽ đóng Issue được liên kết khi Pull Request merge vào default branch của repository | Vì `main` là default branch, feature PR vào `develop` không đại diện cho `Done`; release PR vào `main` phải reference các Issue được phát hành |
| [GitHub Docs: About protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches) | Platform behavior | Branch protection có thể yêu cầu review và status check trước khi merge | Làm căn cứ bảo vệ `main` và `develop` sau khi role trong repository và required check được xác nhận; cấu hình protection cụ thể vẫn chưa chốt |
| [Google Engineering Practices: Code Review](https://google.github.io/eng-practices/review/) | Complementary practice | Reviewer nên đủ chuyên môn cho thay đổi liên quan và phản hồi kịp thời; nhiều reviewer có thể phụ trách các phần khác nhau | Mặc định một reviewer là đủ; thay đổi về database, authentication, cấu trúc dùng chung và release cần phạm vi review rộng hơn |
| [A successful Git branching model](https://nvie.com/posts/a-successful-git-branching-model/) | Complementary practice | Mô tả integration branch dài hạn và production branch ổn định; tác giả cũng lưu ý model này không phù hợp cho mọi dự án | Nhóm điều chỉnh `develop` làm integration branch và `main` làm stable demo branch vì dự kiến release demo theo tuần |

## Phân loại quyết định

| Quyết định workflow | Phân loại | Ghi chú |
|---|---|---|
| `Backlog -> Planning -> In Progress -> Review -> Done` | Team convention | GitHub Projects cho phép cấu hình như vậy nhưng không bắt buộc năm trạng thái này |
| `feature branch -> develop -> main` | Adapted team convention | Được chọn để tích hợp và demo theo tuần; nên xem xét lại nếu dự án chuyển sang continuous delivery |
| Giữ Issue ở `Review` sau khi merge vào `develop` | Team convention informed by platform behavior | Tránh nhầm integration merge với việc hoàn tất release |
| Chỉ `Done` sau release merge vào `main` và Issue được đóng | Team Definition of Done | Điều chỉnh khái niệm hoàn tất của Scrum cho release model của repository |
| Definition of Ready trước `In Progress` | Complementary team practice | Checklist collaboration gọn nhẹ, không phải Scrum artifact chính thức và không yêu cầu specification hoàn hảo |
| Thời lượng task 4–5 ngày lịch | Team convention | Được chọn phù hợp lịch sinh viên; không xuất phát trực tiếp từ Scrum hoặc GitHub |
| `Target Date` kết thúc khi hoàn tất kỹ thuật và merge vào `develop` | Team convention | Tách deadline giao việc của owner khỏi lịch release chung và tránh cảnh báo quá hạn sai khi Issue đã tích hợp nhưng còn ở `Review` chờ release |
| Thang Story Point `1, 2, 3, 5, 8`, trong đó `8` phải được phân rã | Team convention informed by relative estimation practice | Dùng cho planning và cân bằng tải, không dùng để chấm hiệu suất cá nhân |
| Mặc định một reviewer và hai reviewer cho thay đổi rủi ro cao đã chọn | Team convention informed by review guidance | Repository rule chỉ nên cấu hình sau khi role và permission được xác nhận |
| Một owner, một reviewer/backup và mặc định một Issue `In Progress` mỗi thành viên | Team convention | Chọn cho nhóm 5 người để giảm công việc ẩn và tình trạng owner quá tải; không phải quy định bắt buộc của Scrum/GitHub |
| Quyết định thay đổi requirement, API, schema, architecture, core dependency hoặc workflow cần ít nhất 3/5 đồng ý | Team convention | Tạo quy tắc đa số được ghi nhận nhưng vẫn giữ course requirement và security constraint là non-negotiable |
| Gate hoàn tất kỹ thuật trước khi merge vào `develop` | Team convention informed by Scrum quality guidance and GitHub merge controls | Yêu cầu traceability, bằng chứng verification, review approval, phản hồi blocking đã giải quyết và cập nhật tài liệu/database khi áp dụng |
| Release gate trước khi merge `develop` vào `main` | Team convention informed by Scrum Definition of Done and GitHub branch controls | Yêu cầu release candidate đã tích hợp và dùng được, verification trên commit mới nhất, hai approval, không bypass, đóng Issue có traceability và Smoke Test sau merge |

## Chính sách rà soát

- Xem xét lại convention khi quy mô nhóm, nhịp delivery, course requirement, permission của repository hoặc deployment model thay đổi.
- Ưu tiên primary source và vendor documentation khi cần xác định platform behavior.
- Ghi nguồn mới hoặc cách hiểu đã thay đổi tại đây trước khi sửa workflow rule đã ổn định.
- Xem blog post và ví dụ là guidance, không phải standard bắt buộc.
