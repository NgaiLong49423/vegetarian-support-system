> **Document:** Use Case Specifications — M06
> **File:** `docs/requirements/use-cases/ai-and-personalization.md`
> **Version:** v2.1.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-09-27
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M06

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-02"></a>
## FR-02 — Quyền Guest trải nghiệm AI Chatbot chung có giới hạn tần suất kỹ thuật

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-02).

<a id="uc-02-1"></a>
### UC-02.1 — Trải nghiệm dùng thử AI Chatbot chung cho Guest

#### Goal
Cho phép Guest dùng thử AI Chatbot ở ngữ cảnh chung hoặc ngữ cảnh Recipe Post công khai, đồng thời bảo vệ dịch vụ bằng giới hạn tần suất kỹ thuật.

#### Primary Actor
`Guest`.

#### Trigger
Guest gửi câu hỏi từ giao diện Chatbot chung hoặc chọn hỏi AI trên một Recipe Post công khai.

#### Preconditions
- Guest chưa đăng nhập; Guest không có hồ sơ dinh dưỡng hoặc lịch sử hội thoại gắn với tài khoản.
- Nếu chọn ngữ cảnh Recipe Post, bài viết đang công khai và Guest có quyền xem.
- Trình duyệt có thể sử dụng anonymous cookie để nhận diện phiên; hệ thống có thể áp dụng coarse IP protection.

#### Main Flow
1. Guest mở Chatbot và gửi câu hỏi hợp lệ.
2. Hệ thống nhận diện phiên Guest bằng anonymous cookie kết hợp bảo vệ theo IP và kiểm tra giới hạn tần suất kỹ thuật theo BR-01.
3. Khi yêu cầu nằm trong ngưỡng, hệ thống cho phép Guest tiếp tục luồng hội thoại do [FR-51](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-51) sở hữu; FR-02 không tạo daily quota hay lịch sử hội thoại theo tài khoản.

#### Alternative Flows
- **Vượt giới hạn tần suất:** Hệ thống không chuyển yêu cầu tới dịch vụ AI, trả HTTP 429, thông báo Guest tạm dừng thao tác và cung cấp gợi ý đăng ký theo BR-05.

#### Exception Flows
- **Dịch vụ AI lỗi hoặc timeout:** Phần phản hồi lỗi thân thiện thuộc luồng hội thoại dùng chung tại [FR-51](#fr-51), theo BR-04 và NFR-18.

#### Postconditions
Guest được chuyển tiếp tới hội thoại FR-51 khi yêu cầu hợp lệ và chưa vượt giới hạn; yêu cầu quá giới hạn bị chặn trước khi gọi dịch vụ AI.

#### Traceability
- **Parent FR:** [FR-02](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-02).
- **Relevant BR:** [BR-01](../srs/BUSINESS-RULES.md#br-01) (quyền Guest và giới hạn tần suất); [BR-05](../srs/BUSINESS-RULES.md#br-05) (giới hạn trải nghiệm Guest).
- **Relevant NFR:** [NFR-10](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10) (kiểm soát đầu vào và bảo vệ API).

---

<a id="fr-10"></a>
## FR-10 — Phân quyền tính năng AI theo gói tài khoản (Feature-based AI Entitlement)

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-10).

<a id="uc-10-1"></a>
### UC-10.1 — Xác thực quyền sử dụng tính năng AI trước khi kích hoạt

#### Goal
Chỉ cho phép yêu cầu gọi tính năng AI khi actor có quyền theo hạng gói hiện tại; kiểm tra được thực hiện ở Backend trước khi gọi provider.

#### Primary Actor
`Guest` hoặc `Member` thuộc gói Free, Plus hoặc Pro.

#### Supporting Actors
Hệ thống phân quyền tính năng AI (Feature Entitlement Guard).

#### Trigger
Backend nhận yêu cầu sử dụng một tính năng AI.

#### Preconditions
Yêu cầu xác định được actor, tính năng cần gọi và gói Subscription đang có hiệu lực nếu actor là Member.

#### Main Flow
1. Backend xác định actor và hạng gói hiện tại.
2. Feature Entitlement Guard đối chiếu tính năng được yêu cầu với ma trận quyền của FR-10: Guest chỉ dùng Chatbot; Free Member dùng Chatbot và gợi ý món cơ bản; Plus có thêm AI hỗ trợ soạn bài và gợi ý biến tấu; Pro có toàn bộ tính năng, gồm lập thực đơn tuần.
3. Với quyền hợp lệ, Backend cho phép chuyển yêu cầu tới luồng AI sở hữu tính năng tương ứng; các quyền không bị giới hạn theo số lượt/ngày.

#### Alternative Flows
- **AF-10.1 — Yêu cầu tính năng vượt quyền gói:** Member Free/Plus yêu cầu tính năng thuộc gói cao hơn; xử lý theo [UC-10.2](#uc-10-2) và không chuyển yêu cầu tới Gemini.

#### Exception Flows
- **EF-10.1 — Provider lỗi sau khi quyền đã được xác thực:** Lỗi/timeout được xử lý bởi FR sở hữu tính năng theo BR-04 và NFR-18; quyền hợp lệ không đồng nghĩa yêu cầu AI thành công.

#### Security Flows
- **SF-10.1 — Kiểm tra entitlement tại Backend:** Feature Entitlement Guard bắt buộc chạy tại Backend; thay đổi giao diện/client không thể vượt qua kiểm tra. Yêu cầu không đủ quyền bị chặn trước mọi HTTP request tới Google Gemini API.

#### Postconditions
Yêu cầu được chuyển tới đúng luồng tính năng khi quyền hợp lệ; nếu không hợp lệ, luồng UC-10.2 kết thúc mà không phát sinh lời gọi Gemini.

#### Traceability
- **Parent FR:** [FR-10](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-10).
- **Relevant BR:** [BR-01](../srs/BUSINESS-RULES.md#br-01) (quyền Chatbot cơ bản); [BR-02](../srs/BUSINESS-RULES.md#br-02) (entitlement Plus/Pro); [BR-03](../srs/BUSINESS-RULES.md#br-03) (kiểm tra quyền trước khi gọi AI).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (phân quyền); [NFR-10](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10) (chống vượt quyền và bảo mật request).

---

<a id="uc-10-2"></a>
### UC-10.2 — Nhận hướng dẫn nâng cấp khi tính năng vượt quyền gói hiện tại

#### Goal
Thông báo rõ quyền lợi/gói cần thiết khi Member yêu cầu tính năng AI cao hơn gói hiện tại.

#### Primary Actor
`Member` thuộc gói Free hoặc Plus.

#### Trigger
Member yêu cầu tính năng AI mà gói hiện tại không bao gồm.

#### Preconditions
Backend đã xác định gói hiện tại và tính năng yêu cầu; entitlement check tại UC-10.1 cho kết quả không đủ quyền.

#### Main Flow
1. Backend từ chối yêu cầu tại máy chủ với HTTP 403 trước khi gọi Gemini.
2. Giao diện thông báo gói tối thiểu cần có: Free Member được hướng dẫn nâng cấp Plus khi yêu cầu tính năng Plus; Plus Member được hướng dẫn nâng cấp Pro khi yêu cầu lập thực đơn tuần.
3. Giao diện cung cấp đường dẫn tới thông tin gói dịch vụ tại [FR-13](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-13).

#### Postconditions
Yêu cầu không được thực thi và không phát sinh chi phí gọi Gemini; trạng thái Subscription của Member không thay đổi.

#### Traceability
- **Parent FR:** [FR-10](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-10).
- **Relevant BR:** [BR-02](../srs/BUSINESS-RULES.md#br-02) (quyền theo gói Plus/Pro); [BR-03](../srs/BUSINESS-RULES.md#br-03) (chặn trước khi gọi AI).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (phân quyền Backend); [NFR-13](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-13) (giao diện tiếng Việt, responsive).

---

<a id="fr-11"></a>
## FR-11 — Ghi nhận dữ liệu đo lường kỹ thuật (Telemetry) sử dụng AI

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-11).

#### Shared system-triggered behavior — telemetry capture and retention
FR-11 xác định việc ghi telemetry và dọn bản ghi quá 90 ngày là cơ chế hệ thống, không phải UC độc lập. Không tạo UC-11.1 hoặc UC-11.3; chỉ [UC-11.2](#uc-11-2) là mục tiêu tương tác có actor.

- **Ghi telemetry:** Sau phản hồi Gemini thành công, hệ thống ghi metadata token do provider trả về (`promptTokenCount`, `candidatesTokenCount`, `totalTokenCount`) cùng tài khoản/anonymous session ID, loại tính năng và timestamp UTC. Nếu phản hồi không có `usageMetadata`, hệ thống ghi nhận 0 token và đánh dấu cờ kiểm toán kỹ thuật theo luồng hiện hành.
- **Lỗi provider:** Timeout hoặc lỗi 4xx/5xx không tạo bản ghi telemetry thành công; hệ thống ghi error log riêng theo BR-04.
- **Dọn dữ liệu:** Tiến trình nền chạy định kỳ xóa bản ghi telemetry quá 90 ngày.
- **AF-11.1 — Thiếu usageMetadata:** Phản hồi thành công không có metadata token vẫn được ghi nhận với số token bằng 0 và cờ kiểm toán, không làm gián đoạn trải nghiệm.
- **EF-11.1 — Gọi AI thất bại hoặc timeout:** Không ghi lượt thành công; chỉ ghi log lỗi kỹ thuật riêng.
- **SF-11.1 — Không lưu prompt thô:** Bản ghi telemetry không chứa câu hỏi hoặc câu trả lời thô.
- **SF-11.2 — Retention 90 ngày:** Bản ghi quá hạn được dọn bởi tiến trình nền định kỳ.

#### Traceability — shared system behavior
- **Parent FR:** [FR-11](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-11).
- **Relevant BR:** [BR-04](../srs/BUSINESS-RULES.md#br-04) (provider errors, telemetry và retention).
- **Relevant NFR:** [NFR-08](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08) (bảo vệ dữ liệu cá nhân); [NFR-20](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20) (quyền riêng tư dữ liệu sức khỏe); [NFR-22](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-22) (không lưu prompt thô, giới hạn retention).

<a id="uc-11-2"></a>
### UC-11.2 — Administrator xem thống kê lượng token và chi phí AI

#### Goal
Giúp Administrator theo dõi lượng token tiêu thụ và chi phí AI ước tính theo chu kỳ để phục vụ vận hành.

#### Primary Actor
`Administrator`.

#### Trigger
Administrator mở trang Thống kê kỹ thuật trong khu vực quản trị.

#### Preconditions
Administrator đã xác thực; dữ liệu telemetry hợp lệ sẵn có để tổng hợp.

#### Main Flow
1. Hệ thống truy vấn dữ liệu telemetry đã tổng hợp.
2. Hệ thống hiển thị lượng token theo ngày, tuần, tháng và phân bổ theo tính năng AI.
3. Administrator xem số liệu và chi phí ước tính để theo dõi mức sử dụng tài nguyên.

#### Postconditions
Administrator đã xem được báo cáo tổng hợp; nội dung prompt hoặc câu trả lời thô không được hiển thị qua báo cáo telemetry.

#### Traceability
- **Parent FR:** [FR-11](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-11).
- **Relevant BR:** [BR-04](../srs/BUSINESS-RULES.md#br-04) (bản chất dữ liệu telemetry và lỗi provider).
- **Relevant NFR:** [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09) (chỉ Administrator được xem dữ liệu quản trị).

---

<a id="fr-51"></a>
## FR-51 — AI Chatbot hỗ trợ hỏi đáp ẩm thực chay theo ngữ cảnh

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-51).

#### Shared chatbot processing behavior — UC-51.1 and UC-51.2
Hai Use Case bên dưới dùng chung quy trình xử lý hội thoại; các quyền Guest/Member vẫn do [FR-02](#fr-02) và [FR-10](#fr-10) sở hữu, không được biến thành entitlement mới của chatbot.

1. Hệ thống hiển thị thông báo AI chỉ cung cấp thông tin tham khảo, không chẩn đoán hoặc thay thế tư vấn y khoa.
2. Hệ thống kiểm tra đầu vào và quyền/tần suất theo FR-02 hoặc FR-10; request bị từ chối không được gửi tới Gemini.
3. Backend chuẩn bị ngữ cảnh phù hợp với UC đang thực hiện và gửi yêu cầu an toàn tới Google Gemini; API key không lộ ra client.
4. Hệ thống hiển thị câu trả lời có nhãn AI rõ ràng, tách biệt nội dung gốc của tác giả; telemetry tuân theo FR-11 và không lưu raw prompt.

#### Shared Alternative Flows
- **AF-51.2 — Guest vượt giới hạn tần suất:** Dừng nhận câu hỏi, thông báo thao tác quá nhanh và gợi ý đăng ký/đăng nhập theo FR-02, BR-01 và BR-05.
- **AF-51.3 — Member Free yêu cầu tính năng AI nâng cao:** Không xử lý như năng lực chatbot; chuyển quyền kiểm tra và hướng dẫn nâng cấp về [FR-10](#fr-10) / [UC-10.2](#uc-10-2) và FR sở hữu tính năng đó.

#### Shared Exception and Security Flows
- **EF-51.1 — Yêu cầu chẩn đoán hoặc điều trị y khoa:** Từ chối đưa kết luận chẩn đoán/phác đồ và khuyến nghị tham vấn bác sĩ hoặc chuyên gia y tế.
- **EF-51.2 — Gemini lỗi hoặc timeout:** Hiển thị thông báo sự cố thân thiện và ghi error log theo BR-04/NFR-18.
- **EF-51.3 — Yêu cầu ghi/sửa dữ liệu hệ thống:** Giải thích chatbot chỉ tư vấn, không tự sửa Recipe Post, Meal Plan, hồ sơ hoặc danh mục dinh dưỡng.
- **SF-51.1 — Bảo vệ API key và prompt:** API key được kiểm soát tại Backend; đầu vào được kiểm soát để phòng prompt injection.

#### Traceability — shared chatbot behavior
- **Parent FR:** [FR-51](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-51).
- **Related FR:** [FR-02](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-02) (Guest access); [FR-10](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-10) (Feature Entitlement); [FR-11](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-11) (telemetry).
- **Relevant BR:** [BR-04](../srs/BUSINESS-RULES.md#br-04), [BR-06](../srs/BUSINESS-RULES.md#br-06), [BR-09](../srs/BUSINESS-RULES.md#br-09), [BR-41](../srs/BUSINESS-RULES.md#br-41), [BR-51](../srs/BUSINESS-RULES.md#br-51).
- **Relevant NFR:** [NFR-03](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03), [NFR-10](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10), [NFR-18](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-18), [NFR-20](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20), [NFR-22](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-22), [NFR-25](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-25).

<a id="uc-51-1"></a>
### UC-51.1 — Hỏi đáp AI trong ngữ cảnh chung

#### Goal
Cho phép người dùng hỏi Chatbot về ăn chay, kỹ thuật chế biến và thông tin dinh dưỡng tham khảo trong ngữ cảnh chung.

#### Primary Actor
`Guest` hoặc `Member`.

#### Supporting Actors
Google Gemini AI.

#### Trigger
Người dùng mở Chatbot từ giao diện chung của hệ thống.

#### Preconditions
- Guest đáp ứng kiểm tra truy cập/tần suất tại FR-02; Member đã đăng nhập và được kiểm tra entitlement tại FR-10.
- Nếu Member hỏi về chỉ số sức khỏe cá nhân, Member phải xác nhận phạm vi hỗ trợ theo FR-38 trước khi Chatbot xử lý câu hỏi đó.

#### Main Flow
1. Người dùng mở ngữ cảnh chung và nhập câu hỏi về ăn chay, kiến thức ẩm thực hoặc kỹ thuật chế biến.
2. Hệ thống xử lý theo quy trình chatbot dùng chung ở cấp FR-51; nếu câu hỏi liên quan tới BMI/calorie cá nhân, hệ thống chỉ dùng thông tin hồ sơ phù hợp khi Member yêu cầu.
3. Chatbot trả lời theo dữ liệu ngữ cảnh chung và các ranh giới an toàn; nội dung AI được gắn nhãn riêng.

#### Alternative Flows
- **AF-51.4 — Member hỏi BMI nhưng chưa khai báo hồ sơ:** Giải thích công thức BMI ở mức tham khảo và cung cấp liên kết tới Hồ sơ dinh dưỡng theo FR-35; không suy diễn hồ sơ cá nhân chưa có.

#### Postconditions
Người dùng nhận được phản hồi cho câu hỏi chung; không có dữ liệu nghiệp vụ nào bị chatbot tự sửa.

#### Traceability
- **Parent FR:** [FR-51](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-51).
- **Relevant BR:** [BR-09](../srs/BUSINESS-RULES.md#br-09) (ranh giới y tế); [BR-39](../srs/BUSINESS-RULES.md#br-39) (BMI chỉ mang tính tham khảo); [BR-41](../srs/BUSINESS-RULES.md#br-41) (thông tin dinh dưỡng không thay thế chuyên gia).
- **Relevant NFR:** [NFR-03](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03), [NFR-12](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-12), [NFR-20](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20), [NFR-25](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-25).

---

<a id="uc-51-2"></a>
### UC-51.2 — Hỏi đáp AI theo Recipe Post đang xem

#### Goal
Giúp người đang xem Recipe Post công khai hỏi AI về chính công thức đó mà không phải nhập lại dữ liệu bài viết.

#### Primary Actor
`Guest` hoặc `Member`.

#### Supporting Actors
Google Gemini AI.

#### Trigger
Người dùng nhấn “Hỏi AI về công thức này” trên trang chi tiết Recipe Post.

#### Preconditions
Bài Recipe Post đang công khai (`PUBLISHED`) và người dùng có quyền xem; truy cập AI vẫn phải qua kiểm tra tại FR-02 hoặc FR-10.

#### Main Flow
1. Giao diện mở Chatbot với huy hiệu/tiêu đề của Recipe Post đang xem.
2. Người dùng nhập câu hỏi về công đoạn chế biến, nguyên liệu thay thế phù hợp hoặc thông tin có sẵn trong bài.
3. Hệ thống chuẩn bị context gồm dữ liệu công thức, trường phái ăn chay, khẩu phần, thời gian, nguyên liệu/định lượng, `instructions` và dữ liệu dinh dưỡng hiện có; sau đó xử lý theo quy trình chatbot dùng chung.
4. Chatbot trả lời dựa trên context bài; nếu công thức không ghi một chi tiết, câu trả lời phải phân biệt rõ phần gợi ý ước tính của AI với dữ kiện gốc.

#### Alternative Flows
- **AF-51.1 — Đóng hoặc chuyển ngữ cảnh:** Người dùng có thể đóng context công thức để về hội thoại chung hoặc chuyển sang Recipe Post khác; Chatbot cập nhật context theo bài mới.

#### Postconditions
Người dùng nhận được câu trả lời gắn với Recipe Post đã chọn; nội dung gốc và dữ liệu nghiệp vụ không bị chatbot thay đổi.

#### Traceability
- **Parent FR:** [FR-51](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-51).
- **Relevant BR:** [BR-09](../srs/BUSINESS-RULES.md#br-09) (ranh giới y tế); [BR-41](../srs/BUSINESS-RULES.md#br-41) (ranh giới tư vấn dinh dưỡng); [BR-51](../srs/BUSINESS-RULES.md#br-51) (AI không quản lý danh mục dinh dưỡng).
- **Relevant NFR:** [NFR-03](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-03), [NFR-10](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-10), [NFR-12](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-12), [NFR-18](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-18), [NFR-22](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-22), [NFR-25](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-25).

---
