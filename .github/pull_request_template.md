> **Document:** Pull Request Template  
> **File:** `.github/pull_request_template.md`  
> **Version:** v1.0.0  
> **Created:** 2026-06-14  
> **Last Updated:** 2026-09-12  
> **Status:** Template  

# Pull Request

> **Pull Request (PR - Yêu cầu gộp code):** Là yêu cầu xem xét và gộp mã nguồn từ một nhánh phát triển riêng biệt vào nhánh chính của dự án. Hãy điền đầy đủ các thông tin dưới đây để người duyệt code (reviewer) dễ dàng kiểm tra và phê duyệt.

---

## 1. Mô Tả Thay Đổi

Mô tả ngắn gọn những thay đổi chính mà Pull Request này mang lại:

- _Mô tả thay đổi chính._

## 2. Lý Do Thay Đổi

Giải thích nguyên nhân hoặc mục đích của việc thực hiện những thay đổi này:

- _Giải thích lý do thay đổi._

## 3. Loại Thay Đổi

*Chọn loại thay đổi phù hợp bằng cách đánh dấu `x` vào ô vuông (ví dụ: `[x]`)*:

- [ ] `feat`: thêm tính năng mới
- [ ] `fix`: sửa lỗi
- [ ] `docs`: cập nhật tài liệu
- [ ] `style`: chỉnh định dạng code, không thay đổi logic
- [ ] `refactor`: tái cấu trúc code, không thay đổi chức năng
- [ ] `test`: thêm hoặc chỉnh sửa test
- [ ] `build`: thay đổi dependency, build tool hoặc cấu hình build
- [ ] `chore`: công việc phụ trợ như setup, cấu hình, dọn dẹp file
- [ ] `ops`: thay đổi liên quan đến deploy, CI/CD hoặc vận hành

## 4. Branch Liên Quan

Ghi rõ nhánh nguồn và nhánh đích:

```text
Source branch:
Target branch: develop | main
```

> **Giải thích thuật ngữ:**
> * **Branch (nhánh):** Nhánh mã nguồn song song dùng để phát triển các chức năng hoặc sửa lỗi riêng biệt trước khi gộp vào nhánh chính.
> * **Merge (gộp code):** Thao tác gộp tất cả các thay đổi từ nhánh này vào nhánh khác.

## 5. Issue Liên Quan

Feature PR vào `develop` chỉ liên kết Issue, chưa đóng Issue:

```text
Refs #<issue-number>
```

Release PR vào `main` liệt kê các Issue sẽ được đóng sau khi merge:

```text
Closes #<issue-number>
```

Nếu không có Issue liên quan, phải ghi rõ lý do.

> **Giải thích thuật ngữ:**
> * **Issue:** Một thẻ ghi nhận đầu việc, lỗi, hoặc yêu cầu thảo luận tính năng được quản lý trên GitHub.

## 6. Những File/Thư Mục Bị Ảnh Hưởng

Liệt kê các tập tin hoặc thư mục chính trực tiếp chịu ảnh hưởng của thay đổi:

- _Liệt kê file/thư mục bị ảnh hưởng._

*Ví dụ minh họa:*
* `- app/`
* `- database/schema.sql`
* `- README.md`

## 7. Cách Kiểm Tra

Mô tả các bước hoặc phương thức bạn đã kiểm tra (test) thay đổi này trước khi gửi PR:

- Các lệnh automated test/build đã chạy:
  - `...`
- Kết quả:
  - ...
- Các bước manual test và kết quả, nếu chưa có automated test:
  1. ...

*Nếu chưa kiểm tra được, hãy ghi rõ lý do:*
- _Ghi lý do chưa kiểm tra được, hoặc xóa dòng này khi đã kiểm tra._

## 8. Ảnh Chụp Màn Hình Hoặc Demo

Nếu thay đổi của bạn có tác động đến giao diện người dùng (UI), hãy đính kèm ảnh chụp màn hình hoặc cung cấp liên kết video/mô tả demo tại đây:

```text
Không có
```

Nếu có dùng AI, ghi phần AI hỗ trợ, phần người thực hiện đã kiểm tra và bằng chứng xác minh. Nếu không dùng, ghi `Không dùng AI`.

## 9. Ghi Chú Thêm

Cung cấp bất kỳ thông tin bổ sung nào cần người duyệt code lưu ý:

```text
Không có
```

## 10. Checklist Trước Khi Merge

*Đảm bảo bạn đã tích chọn đầy đủ các mục này trước khi yêu cầu gộp nhánh:*

- [ ] PR liên kết đúng Issue và không vượt ngoài scope đã thống nhất
- [ ] Tất cả Acceptance Criteria đã được kiểm tra
- [ ] Đã ghi lệnh/cách kiểm tra và kết quả thực tế
- [ ] Project build và chạy được; không còn blocker hoặc lỗi nghiêm trọng đã biết
- [ ] Không commit secret, `.env`, credential, file build hoặc file cá nhân
- [ ] Commit message tuân theo Conventional Commits
- [ ] Tên branch rõ ràng và đúng quy ước
- [ ] Đã cập nhật tài liệu, API docs, database script/schema và ERD nếu liên quan
- [ ] Phần chưa hoàn thành đã được tách thành Issue mới
- [ ] Mọi yêu cầu sửa đổi và review conversation quan trọng đã được giải quyết
- [ ] Có đủ số reviewer approval theo mức độ rủi ro
- [ ] Đã khai báo việc dùng AI và cách kiểm tra output, nếu có

## 11. Checklist Dành Cho Release PR Vào `main`

*Bỏ qua phần này nếu target branch là `develop`.*

- [ ] Source branch là `develop`, target branch là `main`
- [ ] Đã liệt kê từng Issue được phát hành bằng `Closes #<issue-number>`
- [ ] Tất cả Issue trong release đã qua cổng hoàn tất kỹ thuật và code nằm trong `develop`
- [ ] Không có thay đổi chức năng được thêm trực tiếp trong release PR
- [ ] Build và automated tests đạt trên commit mới nhất; hoặc đã ghi manual test và kết quả nếu chưa có automated tests
- [ ] Đã kiểm tra các luồng demo chính và sự kết hợp giữa những tính năng vừa tích hợp
- [ ] Không còn blocker hoặc lỗi nghiêm trọng; lỗi nhỏ được chấp nhận có Issue riêng và được ghi trong PR
- [ ] Database migration/schema đã được thử trên database sạch hoặc môi trường kiểm tra tương đương nếu liên quan
- [ ] README, tài liệu chạy, API docs và `CHANGELOG.md` đã cập nhật nếu liên quan
- [ ] Mọi review conversation quan trọng đã được giải quyết và required checks đã đạt
- [ ] Có ít nhất hai reviewer approval
- [ ] Không bypass required checks hoặc branch protection

### Xác Nhận Sau Khi Merge

- [ ] Đã chạy smoke test trên commit của `main`
- [ ] `main` build/chạy được và sẵn sàng demo
- [ ] Các Issue đã được đóng và chuyển sang `Done`

Nếu smoke test thất bại, mở lại Issue bị ảnh hưởng, chuyển về `Review`, tạo Bug Issue và dừng release tiếp theo cho đến khi nhóm quyết định `revert` hay `hotfix`.
