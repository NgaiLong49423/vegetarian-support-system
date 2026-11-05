---
title: Mâm Xanh — Nguyên tắc triển khai kỹ thuật cho AI Coding Agent
version: 1.0.0
status: ACTIVE
scope: Toàn bộ repository Mâm Xanh (không giới hạn ở các Gate AI)
---

# MÂM XANH — AGENT ENGINEERING PRINCIPLES

> **Mục đích:** Đây là bộ tiêu chí mặc định để AI Coding Agent phân tích, lập kế hoạch, viết code, refactor, review và kiểm thử trong dự án Mâm Xanh (SWP391). Không yêu cầu người dùng phải nhắc lại ở mỗi tác vụ.
>
> **Ưu tiên bắt buộc, theo thứ tự:** **(1) Chức năng ổn định → (2) Hiệu năng → (3) Khả năng bảo trì → (4) Tái sử dụng framework/thư viện → (5) Kiểm thử và an toàn cơ bản.**
>
> Thứ tự trên là **thứ tự ưu tiên khi đánh đổi nguồn lực**, không phải lý do bỏ qua kiểm thử hay các kiểm tra tối thiểu để dữ liệu và chức năng không bị sai. Các yêu cầu SRS/Business Rules đã duyệt và ràng buộc trực tiếp của tác vụ vẫn phải được tuân thủ.

## 1. Bối cảnh và mục tiêu

- Đây là **dự án học thuật SWP391**, ưu tiên demo được các luồng nghiệp vụ thật từ giao diện đến Backend và Database.
- Tránh **over-engineering** (thiết kế phức tạp hơn nhu cầu), kiến trúc cấp doanh nghiệp không cần thiết, tối ưu hóa khi chưa có bằng chứng và mở rộng phạm vi ngoài yêu cầu.
- Ưu tiên **hoàn thành tính năng đang làm** thay vì xây hạ tầng tổng quát cho những tính năng tương lai chưa có.
- Tôn trọng công nghệ, code, cấu hình và hợp đồng API thực tế của repository; **đọc code trước khi đưa ra giải pháp**. Không giả định tên hàm, Tool, thư viện hoặc API đã tồn tại.

## 2. Tiêu chí 1 — Functionality & Stability (chức năng và độ ổn định)

**Mục tiêu:** Chức năng được yêu cầu phải chạy thật, trả kết quả đúng, duy trì trạng thái nhất quán và xử lý hợp lý những lỗi thông thường.

- Đối chiếu yêu cầu, SRS/Business Rules và mã nguồn đang chạy trước khi sửa.
- Bảo đảm luồng **UI → API → Service/Agent → Database** hoặc luồng tích hợp tương ứng hoạt động từ đầu đến cuối.
- Không coi việc viết xong code, mock pass hay build thành công là bằng chứng tính năng đã hoạt động trên hệ thống thật.
- Ưu tiên xử lý lỗi có thể gặp trong vận hành: thiếu tham số, mất kết nối, timeout, lỗi API/model, request trùng, cancel, trạng thái không nhất quán và kết quả ghi dữ liệu đã thành công nhưng phản hồi bị mất.
- Không báo thành công khi Backend chưa xác nhận. Không tự phát lại thao tác ghi có khả năng đã commit.
- Giữ nguyên nghiệp vụ hiện hành; chỉ thay đổi phạm vi người dùng yêu cầu. Không tự nâng cấp tính năng hoặc thêm business rule mới.

**Điều kiện đạt:** Có bằng chứng chức năng chính chạy đúng trên môi trường phù hợp; các lỗi blocker (lỗi ngăn chức năng hoạt động) đã được xử lý hoặc được báo rõ.

## 3. Tiêu chí 2 — Performance (hiệu năng)

**Mục tiêu:** Đủ nhanh và tiết kiệm tài nguyên cho quy mô dự án, không tối ưu hóa sớm.

- Tránh gọi thừa LLM, Embedding, API, SQL, đọc/ghi file và các vòng xử lý trùng lặp.
- Tái sử dụng kết quả/cache hiện có khi đúng ngữ cảnh và không làm sai dữ liệu.
- Tránh giữ transaction trong lúc gọi mạng/model; chỉ thực hiện công việc thực sự cần thiết bên trong transaction.
- Ưu tiên thay đổi đơn giản có tác động rõ. Chỉ thêm queue, distributed cache, vector database, reranker, microservice hoặc cơ chế tối ưu lớn khi có nhu cầu và bằng chứng đo lường.
- Đo hoặc ghi nhận độ trễ, số lần gọi và tài nguyên khi hiệu năng là vấn đề; không tuyên bố “nhanh hơn” nếu chưa đo.

**Điều kiện đạt:** Không có thao tác dư thừa rõ ràng hoặc độ trễ bất hợp lý; không đánh đổi tính đúng đắn lấy tốc độ.

## 4. Tiêu chí 3 — Maintainability (khả năng bảo trì)

**Mục tiêu:** Code dễ hiểu, sửa, mở rộng và bàn giao cho các thành viên nhóm.

- Tuân thủ kiến trúc và quy ước code đang có; tách trách nhiệm đúng chỗ, tránh lớp/hàm quá lớn.
- Ưu tiên cách triển khai **đơn giản nhất đáp ứng đầy đủ yêu cầu**; không tạo abstraction (lớp trừu tượng) trước khi có nhu cầu rõ.
- Tránh duplicate code (lặp code), hardcode cấu hình quan trọng và các lớp adapter/wrapper không mang lại giá trị thực.
- Thay đổi tối thiểu những file cần thiết; không refactor lan rộng hoặc đổi tên chỉ vì thẩm mỹ trong khi đang làm tính năng.
- Viết chú thích cho quyết định khó hiểu; không viết tài liệu, log hoặc bộ cấu hình đồ sộ nếu không cần.
- Cập nhật hợp đồng API/OpenAPI và tài liệu có liên quan khi behavior thực tế thay đổi.

**Điều kiện đạt:** Người khác trong nhóm có thể đọc, chạy và chỉnh sửa chức năng mà không phải hiểu một hạ tầng tùy biến phức tạp.

## 5. Tiêu chí 4 — Library/Framework First (ưu tiên giải pháp có sẵn)

**Mục tiêu:** Không tự phát minh lại tính năng mà hệ thống hoặc thư viện phù hợp đã cung cấp.

**Trước khi viết cơ chế mới, kiểm tra theo thứ tự:**

1. **Code hiện có của repository:** Service, utility, component, test fixture, integration và configuration đã giải quyết vấn đề chưa?
2. **Framework/SDK đang dùng:** Google ADK Java, Spring Boot, Spring Data JPA, Jackson, React và các dependency hiện tại có API/chức năng tương ứng không?
3. **Thư viện bên ngoài:** Chỉ cân nhắc thư viện uy tín, đang được duy trì, tương thích phiên bản và giấy phép, đem lại lợi ích rõ so với viết một đoạn code nhỏ.
4. **Tự triển khai:** Chỉ khi không có giải pháp phù hợp, hoặc dùng thư viện tạo thêm chi phí/độ phức tạp không đáng có.

- **Ưu tiên tái sử dụng, không phải sao chép code tùy tiện trên Internet.** Kiểm tra nguồn gốc, license (giấy phép), tính tương thích và hành vi thực tế.
- Không thêm dependency chỉ vì “có thể sẽ dùng”; không xây planner/executor, session engine, retry engine, workflow engine hoặc HTTP client thứ hai nếu framework hiện tại đáp ứng được.
- Nếu thư viện không có chức năng phù hợp, một giải pháp tự viết **nhỏ, rõ ràng, dễ test** được chấp nhận.
- Với công nghệ có phiên bản thay đổi nhanh, xác minh API ở **phiên bản đang sử dụng trong project**; không dựa vào API của phiên bản khác.

**Điều kiện đạt:** Trong báo cáo kỹ thuật nêu ngắn gọn thành phần đã tái sử dụng và lý do nếu phải tự viết cơ chế mới đáng kể.

## 6. Tiêu chí 5 — Pragmatic Testing & Basic Safety (kiểm thử và an toàn cơ bản)

**Mục tiêu:** Đủ bằng chứng để tin rằng chức năng chạy đúng; không xây chương trình kiểm thử/bảo mật cấp doanh nghiệp.

- Kiểm thử **happy path** (luồng thành công), lỗi thường gặp, trường hợp biên liên quan và đường tích hợp thực tế.
- Với tính năng có ghi dữ liệu: xác minh không ghi trùng, không ghi sai owner/tài khoản, trạng thái transaction phù hợp và không báo thành công giả.
- Với AI Agent: phân biệt mock/unit, integration, browser E2E (kiểm thử từ giao diện tới hệ thống thật) và live model. Không gọi mock test là live E2E.
- Giữ những kiểm tra thiết yếu hiện có: xác thực, phân quyền theo người dùng, validation (kiểm tra đầu vào), xác nhận các thao tác cần xác nhận, không lộ API key và không làm hỏng dữ liệu.
- Không tự thêm hệ thống chống tấn công nâng cao, compliance (tuân thủ tiêu chuẩn doanh nghiệp), telemetry/audit phức tạp, mô hình phân quyền mới hoặc test suite khổng lồ nếu tác vụ không đòi hỏi.
- Khi chạy API trả phí, kiểm tra ngân sách và điều kiện đã được duyệt trước khi gọi.

**Điều kiện đạt:** Các test trọng tâm pass, kết quả thực thi được ghi rõ; các mục chưa chạy hoặc lỗi tồn đọng được khai báo trung thực.

## 7. Quy tắc lựa chọn giải pháp khi có đánh đổi

- **Ổn định hơn nhưng thêm một ít code:** Chọn giải pháp ổn định, miễn không tạo hạ tầng quá mức.
- **Nhanh hơn nhưng dễ mất dữ liệu hoặc làm sai nghiệp vụ:** Không chọn.
- **Một thư viện lớn thay cho 15 dòng code đơn giản:** Có thể tự viết 15 dòng nếu dễ bảo trì hơn; Library-first không đồng nghĩa thêm dependency bằng mọi giá.
- **Tối ưu chỉ có lợi trên lý thuyết:** Hoãn, ưu tiên hoàn thiện chức năng thật.
- **Thêm framework riêng để “chuẩn enterprise”:** Từ chối nếu không giải quyết vấn đề cụ thể đang gặp.
- **Có thể giữ giới hạn của phiên bản hiện tại để hoàn thành gate:** Giữ, ghi rõ giới hạn; không mở rộng ngoài phạm vi.
- **Nếu tài liệu và runtime mâu thuẫn:** Báo sai khác, đối chiếu SRS/Business Rules và xác định nguồn đúng; không tự suy đoán.

## 8. Quy trình mặc định cho mọi tác vụ của Coding Agent

1. **Inspect:** Đọc yêu cầu, source liên quan, dependency và test hiện có; xác định phạm vi thực sự.
2. **Reuse check:** Liệt kê ngắn gọn những gì đã có thể dùng lại; chỉ đề xuất code mới cho khoảng trống cụ thể.
3. **Minimal implementation:** Sửa/tạo ít thành phần nhất để hoàn thành chức năng, tránh lan sang gate/module khác.
4. **Verification:** Chạy test phù hợp, build và kiểm thử tích hợp/thực tế khi có môi trường; phân biệt những gì đã thực sự chạy.
5. **Report:** Trình bày kết quả chức năng, bằng chứng test, hiệu năng đáng chú ý, code/framework được tái sử dụng, lỗi và phần còn lại.

Không vòng lặp lập kế hoạch vô hạn: khi yêu cầu đã rõ và không có blocker, chuyển sang triển khai. Không tuyên bố hoàn tất nếu chưa có bằng chứng.

## 9. Những nguyên tắc repository cần giữ

- Không tự ý sửa `docs/diagrams/`.
- Không push trực tiếp vào `main` hoặc `develop`; tuân thủ quy trình nhánh/PR đang áp dụng.
- Không tự ý xóa hoặc ghi đè thay đổi local chưa liên quan; kiểm tra Git status trước các thao tác có nguy cơ ảnh hưởng dữ liệu.
- Không tự commit/push khi người dùng hoặc phạm vi tác vụ chưa cho phép.
- Nếu thêm bảng/cột, API hoặc dependency, giải thích tại sao cần thiết cho tính năng hiện tại.

## 10. Mẫu báo cáo hoàn thành ngắn gọn

```markdown
### Kết quả
- Chức năng hoàn thành / chưa hoàn thành:
- Luồng chạy thực tế đã kiểm chứng:
- Test đã chạy (pass/fail/chưa chạy):
- Framework, thư viện và code sẵn có đã tái sử dụng:
- Thay đổi đáng kể và lý do:
- Hiệu năng / chi phí nếu có số đo:
- Lỗi còn tồn tại, giới hạn và bước tiếp theo:
```

---

**Quy tắc cuối cùng:** Ưu tiên **phần mềm chạy đúng và ổn định**, sau đó **đủ nhanh**, **dễ bảo trì**, **tận dụng công cụ có sẵn** và **được kiểm chứng vừa đủ**. Không đánh đổi sự đơn giản để lấy hình thức “kiến trúc chuyên nghiệp” nhưng chưa có giá trị thực tế cho SWP391.
