# Changelog

`CHANGELOG.md` (nhật ký thay đổi) là file dùng để ghi lại lịch sử tất cả những thay đổi quan trọng của dự án qua từng mốc thời gian hoặc phiên bản.

* File này giúp người đọc (thành viên trong team, giảng viên hoặc người đóng góp) nhanh chóng nắm bắt dự án đã được thêm mới, chỉnh sửa, loại bỏ hoặc khắc phục những vấn đề gì mà không cần phải duyệt qua từng commit trong lịch sử Git.
* Không cần thiết phải ghi lại mọi thay đổi nhỏ nhặt (như sửa lỗi chính tả hay định dạng code), chỉ tập trung vào các cập nhật có ý nghĩa đối với người dùng hoặc hệ thống.

---

## [Unreleased]

### Added
- 2026-09-12: Thêm `docs/requirements/SRS-decision-questionnaire.md` v0.1.0 gồm 38 câu hỏi chốt phạm vi sản phẩm; mỗi câu có phương án khuyến nghị để xử lý các câu người dùng bỏ trống.
- 2026-09-12: Chuyển bộ 5 tài liệu phân rã Đề tài 03 vào repository; SRS v0.19.0 ghi nhận quy tắc chống báo cáo trùng. Giữ SRS mẫu cũ tại `docs/requirements/SRS-template-before-topic03.md` để tham khảo, không dùng làm yêu cầu hiện hành.
- Ghi nhận workflow nhóm mặc định: nhánh `develop`/`main`, release theo checklist, task 4–5 ngày, review PR và dùng Story Points để cân tải.
- Thêm bản ghi quyết định workflow tại `docs/decisions/001-team-workflow.md`.
- Thêm sổ nguồn `docs/decisions/WORKFLOW-SOURCES.md` để phân biệt platform behavior, framework guidance, complementary practice và team convention.
- Thêm Definition of Ready cho bước chuyển từ `Planning` sang `In Progress`.

### Changed
- 2026-09-12: SRS v0.43.0 đồng bộ xác nhận Q01–Q38, bổ sung FR-47–FR-51 và bảng truy vết; Free/Plus/Pro cùng chức năng, thanh toán thật, báo cáo/Like bình luận, email/in-app notification và bản đồ với khoảng cách đường bộ. Q31 chỉ ghi nguồn, nghiên cứu công thức khi triển khai; Q35 là phạm vi nhóm tự chọn, bỏ điều kiện chờ giảng viên xác nhận. Đồng bộ phiếu trả lời, actor, định hướng, gói AI và benchmark.
- 2026-09-12: Thay `docs/decisions/SWP-Technology-Stack-v1.0.0.txt` bằng v2.0.0 để đồng bộ quyết định location: bỏ Browser Geolocation và danh mục restaurant/store trong SQL Server, chuyển sang địa chỉ do Member nhập cùng Google Geocoding/Places.
- 2026-09-12: SRS v0.42.0 chốt bốn bán kính tìm nhà hàng chay cho Member: 500 m, 1 km, 5 km và 10 km; giá trị ngoài danh sách bị chặn trước khi gọi Google Maps Platform.
- 2026-09-12: SRS v0.41.0 chốt chỉ Member đã đăng nhập được tìm nhà hàng chay quanh địa chỉ nhập; yêu cầu Guest bị chặn trước khi gọi Google Maps Platform để không tiêu thụ quota dịch vụ ngoài.
- 2026-09-12: SRS v0.40.0 chốt bình luận hỗ trợ reply lồng nhiều cấp; Guest được đọc, Member quản lý bình luận của mình và trả lời bình luận/reply khác, Administrator quản lý nội dung vi phạm.
- 2026-09-12: SRS v0.39.0 chốt “bình chọn” thành Like/Upvote: mỗi Member tối đa một Like trên mỗi bài và có thể bỏ Like; Guest chỉ xem tổng, MVP không triển khai đánh giá 1–5 sao.
- 2026-09-12: SRS v0.38.0 chốt quyền tác giả theo mô hình mạng xã hội thu nhỏ ở mức chức năng: người có quyền đăng được tạo/xem/sửa/xóa bài công thức của mình, không duyệt từng bài/lần sửa; Admin vẫn hậu kiểm và tác giả không tự khôi phục bài bị ẩn.
- 2026-09-12: SRS v0.37.0 chốt tác giả được sửa bài công thức đang công khai mà không cần duyệt lại; đồng thời giới hạn module địa điểm thành đề xuất nhà hàng chay độc lập theo địa chỉ/bán kính từ Google Maps, không liên kết món đã tìm và không quản lý hồ sơ nhà hàng.
- 2026-09-12: SRS v0.36.0 chốt đơn xin quyền đăng bị từ chối phải có lý do và được sửa/gửi lại; mỗi Member chỉ có một đơn đang chờ, MVP không đặt thời gian chờ cố định và quyết định hạn chế do lạm dụng phải lưu lý do.
- 2026-09-12: SRS v0.35.0 chốt đơn xin quyền đăng phải có xác nhận chính sách, lý do đóng góp, xác nhận trách nhiệm và hiểu chế tài; MVP không yêu cầu chứng chỉ hoặc kinh nghiệm chuyên môn.
- 2026-09-12: SRS v0.34.0 thay duyệt từng công thức bằng duyệt đơn xin quyền đăng: Member được cấp quyền tự công khai bài hợp lệ; Admin hậu kiểm khi có báo cáo và có thể áp dụng chế tài có lý do. Đồng bộ tài liệu định hướng, actor, gói AI và benchmark sang nguồn gợi ý là bài đang công khai, không bị ẩn/xóa.
- 2026-09-12: SRS v0.33.0 đưa tìm nhà hàng chay quanh địa chỉ Google Maps do người dùng nhập vào MVP, không dùng GPS hoặc Gemini; ghi nhận phụ thuộc Geocoding/Places, trạng thái lỗi và các quyết định UX/chi phí còn mở.
- 2026-09-12: SRS v0.32.0 chốt thao tác quản lý danh mục dinh dưỡng của Administrator: xem/tìm, thêm, sửa, bật/ngừng hỗ trợ, xem công thức liên quan và lưu nguồn; MVP không xóa vĩnh viễn, nhập hàng loạt, gọi USDA hoặc dùng AI tự điền.
- 2026-09-12: SRS v0.31.0 chốt Administrator có quyền quản lý danh mục nguyên liệu dinh dưỡng của ứng dụng; phạm vi thao tác và ảnh hưởng đến công thức cũ sẽ được phân rã tiếp.
- 2026-09-12: SRS v0.30.0 chốt MVP dùng danh mục dinh dưỡng cố định trong database với USDA FoodData Central làm nguồn tham khảo chính; không gọi API/AI matching khi sử dụng, và nguyên liệu chưa hỗ trợ không chặn đăng bài nhưng làm kết quả dinh dưỡng chưa đầy đủ.
- 2026-09-12: SRS v0.29.0 chốt dinh dưỡng công thức được tính từ nguyên liệu và định lượng; khẩu phần chỉ dùng để phân bổ tổng công thức và ghi nhận lượng dự định ăn, còn kiểm tra ngày cộng tất cả món trong ba bữa.
- 2026-09-12: SRS v0.28.0 chốt chín chỉ tiêu dinh dưỡng MVP và cách trình bày theo khẩu phần; kiểm tra menu ngày dùng mức tham khảo cá nhân, không có điểm cân bằng tổng hợp hoặc Glycemic Index/Glycemic Load trong MVP.
- 2026-09-12: SRS v0.27.0 giới hạn các chức năng nhu cầu/AI menu/kiểm tra dinh dưỡng cho Member đủ 18 tuổi, không mang thai/cho con bú và không cần chế độ ăn điều trị; các chức năng thông thường vẫn khả dụng.
- 2026-09-12: SRS v0.26.0 chốt BMI chỉ là chỉ số tham khảo trong Hồ sơ nhu cầu dinh dưỡng; bổ sung AI menu theo dữ liệu dinh dưỡng và kiểm tra menu ngày với trạng thái/chênh lệch nhưng không chẩn đoán.
- 2026-09-12: SRS v0.25.0 chốt AI gợi ý và lập menu chỉ từ bài công thức đã duyệt trong hệ thống, phải dẫn tới bài nguồn và không được tạo công thức mới.
- 2026-09-12: SRS v0.24.0 cho phép nhiều công thức trong mỗi bữa, không đặt giới hạn cứng và ngăn cùng công thức bị thêm trùng vào cùng ngày/bữa của một Member.
- 2026-09-12: SRS v0.23.0 chốt lịch ăn MVP chỉ có ba loại cố định: Bữa sáng, Bữa trưa và Bữa tối; không có Bữa phụ hoặc loại bữa tùy chỉnh.
- 2026-09-12: SRS v0.22.0 tách Công thức đã lưu khỏi Lịch ăn, giới hạn dữ liệu cá nhân cho Member, loại Queue khỏi MVP và ghi nhận các thao tác lịch cấp cao.
- 2026-09-12: SRS v0.21.0 bỏ Blog tổng quát, thống nhất một loại nội dung là bài công thức (`Recipe Post`) và chuyển hướng dẫn nấu từng bước thành trường tùy chọn.
- 2026-09-12: SRS v0.20.0 chốt Onboarding được bỏ qua nhưng yêu cầu ba nhóm hồ sơ tối thiểu trước khi dùng AI gợi ý món hoặc tạo thực đơn cá nhân hóa.
- Quy ước `Target Date` là hạn hoàn tất kỹ thuật và merge feature PR vào `develop`; thời gian Issue ở `Review` chờ release vào `main` không tính là trễ của owner.
- Liên kết README và CONTRIBUTING với workflow đã chốt; board dùng năm trạng thái và `Review` giữ cả phần đang chờ release vào `main`.
- Thống nhất đường dẫn agent, skill và output theo thư mục `.agents/` trong toàn bộ template.
- Đồng bộ skill tạo Issue với thang Story Points `1, 2, 3, 5, 8`; bỏ trường Size và yêu cầu phân rã mọi Issue 8 SP trước khi giao.
- Đồng bộ sáu Project Type với primary labels và các GitHub Issue Forms; NFR dùng `📋 Task` làm Type chính và `📐 NFR` làm nhãn phụ.
- Rút gọn issue body thành bảy phần, tách `Draft State` khỏi Project `Status`, và cho phép owner/ngày để `TBD` cho đến trước khi bắt đầu thực hiện.
- Chuẩn hóa cổng hoàn tất kỹ thuật trước khi merge PR vào `develop`, gồm traceability, Acceptance Criteria, bằng chứng kiểm tra, review approval và cập nhật tài liệu/database khi liên quan.
- Phân biệt `Refs #...` cho feature PR vào `develop` với `Closes #...` cho release PR vào `main` trong Pull Request template.
- Chuẩn hóa cổng release từ `develop` vào `main`: kiểm tra release scope, build/test, luồng demo tích hợp, database, tài liệu, hai approval và không bypass branch protection.
- Bổ sung smoke test sau merge và quy tắc mở lại Issue, tạo Bug, dừng release khi bản trên `main` không đạt.

---

## Cách Sử Dụng File Này

* Mỗi khi dự án có các thay đổi đáng chú ý hoặc khi phát hành một phiên bản mới, hãy cập nhật thông tin tương ứng vào phần trên cùng của nhật ký thay đổi (theo thứ tự thời gian mới nhất ở trên).
* Việc phân chia lịch sử thay đổi nên được ghi nhận rõ ràng theo ngày tháng (YYYY-MM-DD) hoặc số phiên bản (version).

**Giải thích ý nghĩa phiên bản (version):**
* **`0.1.0`**: Phiên bản khởi tạo ban đầu, đang trong quá trình xây dựng hoặc thử nghiệm.
* **`1.0.0`**: Phiên bản đầu tiên hoàn chỉnh, có đầy đủ các chức năng cơ bản ổn định và sẵn sàng sử dụng.
* Quy tắc tăng phiên bản: Hãy tăng số phiên bản khi dự án có các nâng cấp lớn, thêm chức năng quan trọng hoặc sửa đổi lớn cấu trúc database/hệ thống.

---

## Các Nhóm Thay Đổi Thường Dùng

Để dễ theo dõi, các thay đổi trong mỗi phiên bản thường được phân loại vào các nhóm dưới đây:

* **`Added`** (Thêm mới): Ghi nhận các tính năng, tài liệu hoặc thành phần mới được tích hợp vào dự án.
* **`Changed`** (Thay đổi): Ghi nhận các thay đổi, tối ưu hóa hoặc nâng cấp trên các tính năng hiện có.
* **`Fixed`** (Sửa lỗi): Ghi nhận các lỗi lập trình hoặc sự cố đã được khắc phục.
* **`Removed`** (Loại bỏ): Ghi nhận các chức năng hoặc thành phần đã bị xóa khỏi dự án.
* **`Docs`** (Tài liệu): Ghi nhận các thay đổi hoặc bổ sung liên quan đến tài liệu dự án.
* **`Security`** (Bảo mật): Ghi nhận các bản vá hoặc cập nhật giúp khắc phục các lỗ hổng bảo mật.

---

## Mẫu Ghi Changelog

Bạn có thể sao chép đoạn mã Markdown dưới đây để bắt đầu viết nhật ký thay đổi cho phiên bản mới:

```md
## [version] - YYYY-MM-DD

### Added
- 

### Changed
- 

### Fixed
- 

### Removed
- 
```

---

## Ví Dụ

Dưới đây là một ví dụ minh họa về cách ghi chép nhật ký thay đổi:

```md
## [0.1.0] - 2026-06-15

### Added
- Khởi tạo cấu trúc GitHub template repository.
- Thêm thư mục `App/`, `docs/`, `database/` và `.github/`.
- Thêm file `CONTRIBUTING.md` để ghi quy tắc đóng góp và quy tắc commit.
- Thêm file `LICENSE` để xác định giấy phép sử dụng mã nguồn.
```

---

## Lưu Ý Khi Cập Nhật

* **Ngắn gọn và rõ ràng:** Diễn đạt súc tích, dễ hiểu về thay đổi cốt lõi.
* **Mỗi dòng một thay đổi:** Tránh viết gộp nhiều thay đổi khác nhau vào cùng một dòng.
* **Lọc bỏ chi tiết thừa:** Tránh ghi chép các thay đổi quá nhỏ không tác động đến cách thức vận hành của dự án (ví dụ: đổi tên biến nội bộ, sửa khoảng trắng...).
* **Cập nhật đúng thời điểm:** Nên cập nhật changelog ngay trước khi tạo một release (phiên bản mới), nộp dự án hoặc gộp nhánh chính.
* **Ghi nhận thay đổi quan trọng:** Bất kỳ thay đổi nào liên quan trực tiếp đến cấu trúc thư mục, tài liệu, hay tập tin script cơ sở dữ liệu (`database/`) đều cần được ghi lại cẩn thận.

---
