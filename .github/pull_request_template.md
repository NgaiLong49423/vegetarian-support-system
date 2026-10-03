> **Document:** Pull Request Template  
> **File:** `.github/pull_request_template.md`  
> **Version:** v2.5.0
> **Created:** 2026-06-14  
> **Last Updated:** 2026-10-03
> **Status:** Template  

# Pull Request

Bắt buộc dùng template này, giữ các mục và checklist; không tự thay bằng format mới. Mục không áp dụng xử lý theo chỉ dẫn bên dưới hoặc ghi `Không áp dụng` kèm lý do.

Quy tắc áp dụng: [CONTRIBUTING.md](../CONTRIBUTING.md#workflow-làm-việc-nhóm). PR vào `develop` phải pass [required validation checks](../CONTRIBUTING.md#develop-required-checks) và review gate hiện hành; PR vào `main` phải đạt cổng bên dưới. Workflow YAML không tự bật GitHub required-check settings.

## Tóm tắt và phạm vi

- Thay đổi và lý do:
- FR/Issue liên quan: `Refs #<issue-number>` (bắt buộc cho implementation work item)
- Reason / Tech Lead authorization nếu dùng ngoại lệ `Refs: N/A`:
- Dependency/blocker hoặc phần chưa kiểm tra:

Dùng `Refs` cho cả hai nhánh; đóng Issue sau nghiệm thu local trên `main`, không dùng closing keywords để đóng ngay khi merge.

Tuân thủ [1 implementation Issue = 1 implementation PR](../CONTRIBUTING.md#implementation-issue-pr). Feature/FR, bug fix/refactor có work item, business behavior và implementation task khác phải có Issue. `Refs: N/A` chỉ dành cho repository setup/internal agent tooling/governance-configuration chore được Tech Lead cho phép; ghi `Reason` và căn cứ authorization. Đây không phải tùy chọn chung.

Ví dụ hợp lệ: `Refs: N/A` cùng `Reason: Repository internal agent tooling setup approved by Tech Lead.` Ví dụ không hợp lệ: “Implement Meal Planner — Refs: N/A”; feature này phải có Issue.

## Bằng chứng kiểm tra

- Commit được kiểm tra:
- Acceptance Criteria/test cases đã kiểm tra:
- Lệnh build/automated tests và kết quả thực tế:
- Manual tests (API, UI, tích hợp/demo) và kết quả:
- Checks chưa chạy/fail/skip và nguyên nhân:
- AI hỗ trợ phần nào; người thực hiện đã xác minh thế nào:

Thêm screenshot/video khi có thay đổi UI. Không ghi test pass nếu chưa chạy.

## Checklist chung

- [ ] Thay đổi truy về đúng FR/Issue và scope đã thống nhất; nếu là ngoại lệ repository-level chore, đã ghi Reason và Tech Lead authorization
- [ ] Đã ghi kết quả tự kiểm tra và phần còn chưa kiểm tra
- [ ] Không có secret, `.env`, credential, file build/cá nhân
- [ ] Tài liệu/contract/migration liên quan được cập nhật khi áp dụng

## Đồng bộ trước khi merge vào `develop`

Chỉ áp dụng cho PR hướng vào `develop`; không phải checklist cho PR `develop` → `main`. Xem [Develop integration baseline và đồng bộ PR](../CONTRIBUTING.md#develop-integration-baseline).

- [ ] Branch đã được kiểm tra/sync với integration baseline `develop` hiện hành trước merge.
- [ ] Git conflicts và semantic conflicts phát sinh sau sync đã được xử lý.
- [ ] Flyway migration version đã được kiểm tra lại nếu PR có migration.
- [ ] Verification phù hợp với scope đã chạy lại sau lần synchronization cuối có ảnh hưởng tới feature.
- [ ] Nếu thay đổi ảnh hưởng FE/BE, Dockerfile, Compose, runtime config hoặc database initialization: đã kiểm tra bằng root Docker Compose; nếu không chạy được local thì ghi rõ blocker và chưa xác minh.
- [ ] Required Frontend/Backend/Docker Development/Sonar và CodeQL checks liên quan pass trên commit cập nhật; thất bại/skip chưa được coi là pass. Review gate hiện hành đã đáp ứng.

## Cổng PR vào `main`

Bỏ qua phần này nếu target là `develop`. Tech Lead mở PR `develop -> main` và tổng hợp bằng chứng.

- [ ] Scope FR/Issue đủ để nghiệm thu; dependency đã đáp ứng, không thêm sửa chức năng trực tiếp trong PR này
- [ ] Acceptance Criteria được bao phủ bằng test cases cho luồng chính, lỗi và quyền truy cập khi liên quan
- [ ] Build, automated tests và required GitHub Actions checks đạt trên commit mới nhất; manual test không thay thế required checks
- [ ] Kiểm tra tích hợp/demo đạt; migration trên DB sạch hoặc tương đương đã kiểm tra nếu liên quan; lỗi được chấp nhận có Bug Issue
- [ ] Yêu cầu sửa bắt buộc đã giải quyết; có ít nhất một approval ngoài tác giả; phần code reviewer viết đã có người khác kiểm tra
- [ ] Nếu code đổi sau approval, phần thay đổi được review lại và required checks chạy trên commit mới nhất

## Nghiệm thu sau merge vào `main`

Đây là xác nhận sau merge, không phải checklist cần đánh dấu trước merge.

- [ ] Owner cung cấp bằng chứng chức năng và Tech Lead tổ chức kiểm tra demo local trên commit `main`
- [ ] Kết quả đạt; Tech Lead xác nhận, đóng Issue và chuyển `Done`

Nếu không đạt, Issue bị ảnh hưởng chưa `Done`; nếu đóng sai trong chính đợt này thì mở lại và chuyển `Review`. Lỗi mới ở Issue đã nghiệm thu từ trước được theo dõi bằng Bug Issue riêng. Tech Lead quyết định sửa sớm/revert khi lỗi cản trở demo.
