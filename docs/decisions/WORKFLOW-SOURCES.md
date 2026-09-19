> **Document:** Workflow Evidence Register  
> **File:** `docs/decisions/WORKFLOW-SOURCES.md`  
> **Version:** v2.0.0
> **Created:** 2026-09-08  
> **Last Updated:** 2026-09-19
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
| [GitHub Docs: Using the built-in automations](https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-built-in-automations) | Platform behavior | Built-in workflow có thể cập nhật trạng thái item khi Pull Request được merge hoặc Issue được đóng | Không dùng automation merge/close để bỏ qua nghiệm thu demo local sau merge |
| [GitHub Docs: Linking a pull request to an issue](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue) | Platform behavior | Closing keyword sẽ đóng Issue được liên kết khi Pull Request merge vào default branch của repository | PR dùng `Refs` ở cả hai nhánh; Tech Lead xác nhận và đóng Issue sau nghiệm thu, tránh closing keywords đóng sớm |
| [GitHub Docs: About protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches) | Platform behavior | Branch protection có thể yêu cầu review và status check trước khi merge | Cổng approval/checks bắt buộc ở `main`, tùy chọn ở `develop`; cấu hình thực tế phải được xác minh riêng, tài liệu không tự thay đổi protection |
| [Google Engineering Practices: Code Review](https://google.github.io/eng-practices/review/) | Complementary practice | Reviewer nên đủ chuyên môn cho thay đổi liên quan và phản hồi kịp thời; nhiều reviewer có thể phụ trách các phần khác nhau | Mặc định một reviewer là đủ; thay đổi về database, authentication, cấu trúc dùng chung và release cần phạm vi review rộng hơn |
| [A successful Git branching model](https://nvie.com/posts/a-successful-git-branching-model/) | Complementary practice | Mô tả integration branch dài hạn và production branch ổn định; tác giả cũng lưu ý model này không phù hợp cho mọi dự án | Nhóm điều chỉnh `develop` làm integration branch và `main` làm stable demo branch vì dự kiến release demo theo tuần |

## Phân loại quyết định

Ngày 2026-09-18, decision-maker xác nhận workflow nhẹ ở nhánh tích hợp và cổng demo bắt buộc; quy tắc thực thi nằm tại [CONTRIBUTING.md](../../CONTRIBUTING.md#workflow-làm-việc-nhóm). Các ngưỡng bên dưới là Team convention, không phải yêu cầu bắt buộc của GitHub/Scrum/Google.

| Quyết định | Phân loại | Căn cứ và đánh đổi |
|---|---|---|
| Board năm trạng thái, nhánh làm việc qua `develop` rồi `main` | Team convention | Tách tích hợp khỏi phiên bản demo; không cần xong cả module |
| Nhận việc đầu tuần, xem xét đưa bản ổn định lên `main` cuối tuần | Team convention | Giữ nhịp học; không merge nếu chưa đạt chất lượng |
| SP tương đối, owner cam kết scope/deadline, báo nguy cơ trễ ngay | Team convention | Điều chỉnh lượng việc, không cắt yêu cầu cho vừa thành viên hoặc tính SP cha/con hai lần |
| Review/checks tùy chọn ở `develop`; approval độc lập và CI bắt buộc ở `main` | Team convention informed by review/merge guidance | Tích hợp nhẹ nhưng phải kiểm tra kỹ trước demo; CI build/test chưa được triển khai đầy đủ tại lần đọc ngày 2026-09-18 |
| Tech Lead điều phối `main`, owner kiểm chứng cả FE do AI thực hiện | Team convention | Không cần coordinator luân phiên hoặc thành viên FE riêng; giữ kiểm tra độc lập |
| `Done` sau nghiệm thu demo local trên `main` | Team Definition of Done | Merge/pipeline xanh không đủ; `Refs` tránh đóng Issue trước nghiệm thu |
| Bug trên `develop`/`main` được báo theo form, Tech Lead triage/giao owner | Team convention | Tái hiện theo commit và xác định nguyên nhân trước khi giao sửa |
| Chưa deploy/CD, triển khai Azure khi nhóm sẵn sàng | Team convention | Demo local hiện tại; không thay đổi technology baseline |

Các ngưỡng 5 SP/4–5 ngày, cập nhật ngày thứ 2/3, reviewer/backup cố định, coordinator luân phiên và hai approval cho `main` thuộc baseline cũ; đã được thay thế như ghi trong ADR-001/002. Quy tắc 3/5 cho thay đổi requirement/schema/kiến trúc/core dependency vẫn được giữ tại ADR-002, không suy diễn quyền Tech Lead vượt scope.

## Chính sách rà soát

- Xem xét lại convention khi quy mô nhóm, nhịp delivery, course requirement, permission của repository hoặc deployment model thay đổi.
- Ưu tiên primary source và vendor documentation khi cần xác định platform behavior.
- Ghi nguồn mới hoặc cách hiểu đã thay đổi tại đây trước khi sửa workflow rule đã ổn định.
- Xem blog post và ví dụ là guidance, không phải standard bắt buộc.
