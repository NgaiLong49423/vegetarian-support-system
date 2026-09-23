# Quy trình triển khai FR từ GitHub Issue

## Khi sử dụng

Dùng khi thành viên yêu cầu agent triển khai một GitHub Issue đã liên kết với FR. Câu gọi ngắn: “Làm Issue #<số> theo workflow triển khai FR.” Số Issue và yêu cầu hiện tại là đầu vào; workflow này không tự cấp quyền commit, push, tạo PR hay sửa GitHub.

## Nguồn và điều kiện đầu vào

- Đọc `AGENTS.md`, `CONTRIBUTING.md` và `.agents/POLICY.md`. `CONTRIBUTING.md` là nguồn quy định quy trình đóng góp và review.
- Đọc Issue thật, các Project field và công việc liên kết bằng phương thức truy cập GitHub hiện có. GitHub CLI là tùy chọn. Nếu không truy cập được, xin quyền truy cập hoặc nội dung Issue; không đoán scope.
- Kiểm tra Source Trace, Acceptance Criteria có thể kiểm chứng, đúng một owner, các field lập kế hoạch và dependency theo `CONTRIBUTING.md`. Xác nhận người yêu cầu là owner hoặc được giao xử lý. Báo phần thiếu hoặc mâu thuẫn trước khi code.
- Kiểm tra FR và lifecycle trong `docs/requirements/SRS.md`, sau đó chỉ đọc định nghĩa FR/BR/NFR chi tiết và nguồn API/kiến trúc liên quan. SRS xác định yêu cầu; Issue theo dõi việc triển khai. Dùng `docs/architecture/ARCHITECTURE.md` và `app/mamxanh-backend/README.md` làm baseline kiến trúc Backend; dùng `docs/api/openapi.yaml` cho chi tiết contract hiện có và `docs/api/API.md` cho quy ước tích hợp. Dừng phần triển khai nếu FR không `ACTIVE` hoặc Issue mâu thuẫn đáng kể với yêu cầu có thẩm quyền.

## Các bước

1. Xem `git status --short`, branch hiện tại, code và test liên quan. Giữ nguyên thay đổi local không thuộc nhiệm vụ. Tìm `AGENTS.md` lồng trong đường dẫn liên quan. Đọc README của app và hướng dẫn API/database khi phần việc cần đến.
2. Ánh xạ từng Acceptance Criterion với hành vi, thành phần cần sửa và cách kiểm chứng. Triển khai theo kiến trúc Backend và baseline API đã được nhóm chấp nhận; không yêu cầu duyệt lại quyết định đã chốt. Với thông số `TBD` của FR đang làm, agent phân rã, đề xuất giá trị và cách kiểm thử để owner/Tech Lead duyệt trước khi triển khai phần phụ thuộc. Tuân thủ cổng duyệt contract trong `CONTRIBUTING.md` cho phần mới hoặc thay đổi; không tự ý đổi public API, schema, kiến trúc hoặc core dependency.
3. Khi Issue đủ điều kiện, tạo hoặc dùng branch phù hợp từ `develop` theo `CONTRIBUTING.md`. Không ghi đè thay đổi chưa commit hoặc branch của người khác. Triển khai trọn phạm vi Issue theo một luồng hoạt động hoàn chỉnh; đồng bộ FE, BE, validation, authorization, xử lý lỗi và test khi có liên quan. Không tự thu hẹp scope.
4. Chạy test gần phần thay đổi trước, rồi chạy build và kiểm tra hành vi rộng hơn khi khả thi. Đối chiếu từng Acceptance Criterion với bằng chứng. Phân biệt kiểm tra UI/mock với xác minh Backend/database thật. Ghi trung thực các kiểm tra lỗi, bỏ qua hoặc không chạy được.
5. Xem diff và status cuối cùng để phát hiện file ngoài scope, artifact sinh ra, secret hoặc thay đổi contract/yêu cầu ngoài ý muốn. Báo file đã đổi, bằng chứng cho Acceptance Criteria, lệnh và kết quả kiểm tra, rủi ro còn lại và quyết định nhóm cần chốt.

## Cổng chuyển bước

- Nếu Issue chưa đủ điều kiện, thiếu quyền truy cập, dependency đang chặn hoặc nguồn yêu cầu mâu thuẫn, dừng phần triển khai bị ảnh hưởng và nêu đúng thông tin/quyết định cần có để tiếp tục.
- Workflow cho phép triển khai local theo yêu cầu. Commit, push, tạo PR, cập nhật Issue/Project, merge và chuyển `Done` cần được người dùng cho phép trong nhiệm vụ hiện tại. Khi được phép, làm theo `CONTRIBUTING.md`, PR template hiện hành và `.agents/POLICY.md`; xem trước các thay đổi từ xa và xác minh kết quả.
- PR vào `develop` đưa Issue sang `Review`, chưa phải `Done`. Tech Lead phụ trách `develop -> main`; hoàn thành cần qua nghiệm thu demo local sau merge theo quy trình dự án.

## Kết quả bàn giao

Báo ngắn gọn bằng tiếng Việt: Issue/FR, branch, phạm vi đã làm, bằng chứng Acceptance Criteria, kết quả test, blocker và bước tiếp theo đã được cho phép. Không tạo file báo cáo tiến độ riêng.
