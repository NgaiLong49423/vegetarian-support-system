> **Document:** Use Case Specifications — M10
> **File:** `docs/requirements/use-cases/nutrition.md`
> **Version:** v2.5.0
> **Created:** 2026-09-26
> **Last Updated:** 2026-10-09
> **Status:** Active
> **Baseline:** Requirements / Implementation Baseline v2.0.0

# Use Case Specifications — M10

Detailed interaction flows for current-baseline requirements. Stable UC IDs are preserved. The linked FR owns the required behavior and Acceptance Criteria; this document owns actor/system interaction detail.

<a id="fr-35"></a>
## FR-35 — Khai báo hồ sơ dinh dưỡng và xem chỉ số tham khảo cá nhân

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35).

<a id="uc-35-1"></a>
### UC-35.1 — Khai báo và cập nhật thông số hồ sơ dinh dưỡng cá nhân

#### Goal
Lưu các thông số sức khỏe do Member khai báo để hỗ trợ các phép tính tham khảo thuộc FR-35.

#### Primary Actor
`Member` đã đăng nhập.

#### Trigger
Member mở biểu mẫu hồ sơ dinh dưỡng hoặc chọn cập nhật thông số đã lưu.

#### Preconditions
Member đã đăng nhập và có trạng thái eligibility `ELIGIBLE` theo FR-38.

#### Main Flow
1. Member mở hoặc chỉnh sửa hồ sơ dinh dưỡng.
2. Hệ thống hiển thị thông báo từ chối trách nhiệm y tế.
3. Member nhập/cập nhật `date_of_birth`, giới tính sinh học, chiều cao, cân nặng, mức độ hoạt động và mục tiêu dinh dưỡng chung.
4. Member gửi biểu mẫu lưu.
5. Hệ thống kiểm tra ngày sinh hợp lệ; service tính tuổi từ `date_of_birth` để xác minh giới hạn 18–120, không lưu tuổi cố định. Hệ thống kiểm tra chiều cao và cân nặng trong các ngưỡng được FR-35 quy định.
6. Hệ thống lưu hồ sơ hợp lệ gắn với tài khoản Member.

#### Alternative Flows
- Member cập nhật cân nặng hoặc mức độ hoạt động; hệ thống lưu giá trị mới và thời điểm cập nhật.

#### Exception/Security Flows
- Ngày sinh sai định dạng/ngoài giới hạn hoặc chiều cao, cân nặng ngoài ngưỡng: từ chối lưu và chỉ rõ trường cần sửa.
- Dữ liệu hồ sơ chỉ được chính Member sở hữu xem hoặc cập nhật; không hiển thị trên hồ sơ công khai (NFR-08, NFR-09, NFR-20).
- Nếu Member có trạng thái `NOT_CONFIRMED` hoặc `INELIGIBLE`, xử lý theo [FR-38](#fr-38) và không thực hiện cá nhân hóa dinh dưỡng.

#### Postconditions
Hồ sơ hợp lệ được lưu với `date_of_birth`; tuổi không được lưu thành thuộc tính cố định.

#### Traceability
- **Parent FR:** [FR-35](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35).
- **Relevant BR:** [BR-42](../srs/BUSINESS-RULES.md#br-42) (đối tượng loại trừ); [BR-48](../srs/BUSINESS-RULES.md#br-48) (quyền sở hữu hồ sơ).
- **Relevant NFR:** [NFR-08](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08), [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09), [NFR-20](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20).

#### Acceptance Coverage
- [AC-35.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35) — Ngày sinh và điều kiện tuổi/phạm vi.
- [AC-35.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35) — Kiểm tra chiều cao và cân nặng.
- [AC-35.6](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35) — Bảo vệ dữ liệu hồ sơ.

<a id="uc-35-2"></a>
### UC-35.2 — Xem BMI và phân loại thể trạng tham khảo

#### Goal
Cho Member xem BMI tham khảo được tính từ hồ sơ đã lưu, không coi đó là chẩn đoán y tế.

#### Primary Actor
`Member` đã đăng nhập.

#### Supporting Actor
Hệ thống tính toán dinh dưỡng nội bộ.

#### Trigger
Member mở hồ sơ dinh dưỡng đã có thông số hợp lệ.

#### Preconditions
Member đã xác nhận đủ điều kiện; hồ sơ có chiều cao và cân nặng hợp lệ.

#### Main Flow
1. Member mở hồ sơ dinh dưỡng.
2. Hệ thống hiển thị tuyên bố từ chối trách nhiệm y tế.
3. Hệ thống tính BMI từ cân nặng và chiều cao, làm tròn một chữ số thập phân.
4. Hệ thống hiển thị BMI cùng phân loại thể trạng tham khảo và cảnh báo đây không phải chẩn đoán.

#### Postconditions
BMI và phân loại tham khảo được hiển thị riêng tư cho Member; dữ liệu hồ sơ không bị thay đổi.

#### Traceability
- **Parent FR:** [FR-35](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35).
- **Relevant BR:** [BR-39](../srs/BUSINESS-RULES.md#br-39) (BMI chỉ mang tính tham khảo); [BR-41](../srs/BUSINESS-RULES.md#br-41) (ranh giới thông tin y tế).
- **Relevant NFR:** [NFR-08](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08), [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09), [NFR-20](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20).

#### Acceptance Coverage
- [AC-35.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35) — Hiển thị disclaimer y tế.
- [AC-35.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35) — Tính và trình bày BMI tham khảo.
- [AC-35.6](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35) — Không lộ dữ liệu trên hồ sơ công khai.

<a id="uc-35-3"></a>
### UC-35.3 — Xem nhu cầu tham khảo hằng ngày cho 9 chỉ tiêu dinh dưỡng

#### Goal
Cho Member xem bảng nhu cầu dinh dưỡng hằng ngày ước tính từ hồ sơ hợp lệ.

#### Primary Actor
`Member` đã đăng nhập.

#### Supporting Actor
Hệ thống tính toán dinh dưỡng nội bộ và dữ liệu tham khảo USDA/NIH.

#### Trigger
Member mở phần nhu cầu dinh dưỡng trong hồ sơ cá nhân.

#### Preconditions
Member đã xác nhận đủ điều kiện và đã lưu các thông số hồ sơ cần thiết.

#### Main Flow
1. Member mở phần nhu cầu dinh dưỡng.
2. Hệ thống xác định thông số hồ sơ đã lưu và tính mức tham khảo theo phương pháp hiện hành trong FR-35.
3. Hệ thống hiển thị 9 chỉ tiêu cùng đơn vị đo và disclaimer y tế.

#### Exception Flows
- Nếu hồ sơ thiếu hoặc trạng thái eligibility không phải `ELIGIBLE`, hệ thống không tính nhu cầu cá nhân hóa và hướng dẫn Member cập nhật hồ sơ hoặc xác nhận phạm vi hỗ trợ theo FR-38.

#### Postconditions
Bảng 9 chỉ tiêu được hiển thị cho Member để tham khảo khi xem dinh dưỡng thực đơn hoặc yêu cầu gợi ý AI.

#### Traceability
- **Parent FR:** [FR-35](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35).
- **Relevant BR:** [BR-41](../srs/BUSINESS-RULES.md#br-41) (ranh giới thông tin y tế); [BR-43](../srs/BUSINESS-RULES.md#br-43) (đơn vị khẩu phần).
- **Relevant NFR:** [NFR-08](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08), [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09), [NFR-20](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20).

#### Acceptance Coverage
- [AC-35.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35) — Hiển thị disclaimer y tế.
- [AC-35.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35) — Trình bày đầy đủ 9 chỉ tiêu.
- [AC-35.6](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-35) — Không lộ dữ liệu trên hồ sơ công khai.

---

<a id="fr-36"></a>
## FR-36 — AI lập menu theo nhu cầu dinh dưỡng từ công thức tin cậy

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36).

<a id="uc-36-1"></a>
### UC-36.1 — Yêu cầu AI tạo thực đơn tham khảo

#### Goal
Yêu cầu AI đề xuất thực đơn dựa trên hồ sơ dinh dưỡng và ràng buộc ăn chay của Member.

#### Primary Actor
`Member` đã đăng nhập.

#### Supporting Actors
Google Gemini; hệ thống kiểm tra entitlement và kho công thức nội bộ.

#### Trigger
Member chọn yêu cầu tạo thực đơn dinh dưỡng từ Lịch ăn tuần hoặc khu vực dinh dưỡng.

#### Preconditions
- Member có hồ sơ dinh dưỡng hợp lệ (FR-35), trạng thái eligibility `ELIGIBLE` (FR-38), và có thông tin ăn chay/dị ứng cần thiết (FR-31).
- Member có gói Pro còn hiệu lực cho tính năng này (FR-10).

#### Main Flow
1. Member chọn tạo thực đơn và phạm vi ngày/tuần được FR-36 hỗ trợ.
2. Backend xác minh entitlement Pro trước khi gọi Gemini.
3. Hệ thống lấy các Recipe Post công khai có dữ liệu dinh dưỡng đáng tin cậy, phù hợp với loại ăn chay và dị ứng của Member.
4. Hệ thống gửi cho Gemini các ứng viên nội bộ cùng dữ liệu hồ sơ cần thiết để đề xuất thực đơn; yêu cầu chỉ chọn ID công thức có sẵn.
5. Hệ thống nhận kết quả để tiếp tục UC-36.2.

#### Alternative Flows
- Nếu kho công thức không đủ cho phạm vi đã chọn, hệ thống thông báo giới hạn dữ liệu và cho phép đề xuất phạm vi ngắn hơn hoặc lặp món ở ngày khác theo FR-36.

#### Exception/Security Flows
- Nếu entitlement không hợp lệ, Backend từ chối trước khi gọi Gemini và hướng dẫn nâng cấp.
- Nếu thiếu hồ sơ hoặc ràng buộc bắt buộc, không gọi Gemini và hướng dẫn Member hoàn thiện dữ liệu theo BR-31.
- Nếu provider lỗi hoặc vượt thời gian phản hồi theo NFR-04, hệ thống thông báo lỗi; entitlement không bị thay đổi.
- Candidate pool loại bỏ công thức không công khai, không phù hợp dị ứng/loại ăn chay hoặc thiếu dữ liệu dinh dưỡng đáng tin cậy.

#### Postconditions
Khi tạo thành công, có kết quả đề xuất cần được hệ thống xác thực và trình bày; chưa có thay đổi nào được ghi vào Meal Plan.

#### Traceability
- **Parent FR:** [FR-36](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36).
- **Relevant BR:** [BR-02](../srs/BUSINESS-RULES.md#br-02), [BR-03](../srs/BUSINESS-RULES.md#br-03), [BR-30](../srs/BUSINESS-RULES.md#br-30), [BR-31](../srs/BUSINESS-RULES.md#br-31), [BR-40](../srs/BUSINESS-RULES.md#br-40), [BR-50](../srs/BUSINESS-RULES.md#br-50).
- **Relevant NFR:** [NFR-04](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-04), [NFR-08](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-08), [NFR-09](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-09), [NFR-20](../srs/NON-FUNCTIONAL-REQUIREMENTS.md#nfr-20).

#### Acceptance Coverage
- [AC-36.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36) — Chỉ dùng ứng viên công khai, đủ dữ liệu dinh dưỡng.
- [AC-36.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36) — Tuân thủ loại ăn chay và dị ứng.
- [AC-36.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36) — Kiểm tra Pro trước khi gọi Gemini.
- [AC-36.6](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36) — Xử lý lỗi provider mà không đổi entitlement.

<a id="uc-36-2"></a>
### UC-36.2 — Xem thực đơn gợi ý và giải trình dinh dưỡng

#### Goal
Giúp Member xem, kiểm tra và chọn món từ kết quả AI trước khi áp dụng.

#### Primary Actor
`Member`.

#### Supporting Actor
Hệ thống xác thực kết quả AI và tính tổng dinh dưỡng tham khảo.

#### Trigger
Kết quả tạo thực đơn từ UC-36.1 đã sẵn sàng.

#### Preconditions
Hệ thống đã nhận được phản hồi AI cho yêu cầu hợp lệ.

#### Main Flow
1. Hệ thống kiểm tra mỗi món được đề xuất khớp với Recipe Post đang công khai trong candidate pool.
2. Hệ thống loại bỏ mục không hợp lệ.
3. Hệ thống trình bày danh sách món, giải trình ngắn và bảng tổng hợp dinh dưỡng tham khảo cùng disclaimer y tế.
4. Member xem, giữ lại hoặc bỏ từng món; nếu muốn lưu, Member tiếp tục UC-36.3.

#### Alternative Flows
- Member đóng bản xem trước hoặc hủy đề xuất; Meal Plan không thay đổi.

#### Exception Flows
- Kết quả không chứa công thức hợp lệ: hệ thống không trình bày món ngoài kho công thức và thông báo không có kết quả phù hợp.

#### Postconditions
Member đã xem kết quả; trạng thái Meal Plan không đổi cho tới khi xác nhận UC-36.3.

#### Traceability
- **Parent FR:** [FR-36](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36).
- **Relevant BR:** [BR-38](../srs/BUSINESS-RULES.md#br-38) (không tự tạo công thức); [BR-39](../srs/BUSINESS-RULES.md#br-39), [BR-41](../srs/BUSINESS-RULES.md#br-41) (thông tin dinh dưỡng tham khảo).

#### Acceptance Coverage
- [AC-36.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36) — Kết quả dựa trên ứng viên đủ điều kiện.
- [AC-36.2](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36) — Loại bỏ công thức ngoài hệ thống.
- [AC-36.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36) — Kết quả tuân thủ ràng buộc ăn chay/dị ứng.

<a id="uc-36-3"></a>
### UC-36.3 — Xác nhận áp dụng thực đơn gợi ý vào Lịch ăn tuần

#### Goal
Cho Member áp dụng các món đã chọn từ bản xem trước vào các ô bữa tương ứng trong Meal Plan tuần.

#### Primary Actor
`Member` đã đăng nhập.

#### Trigger
Member xác nhận lưu các món trong bản xem trước.

#### Preconditions
Member đang xem kết quả hợp lệ từ UC-36.2 và có quyền cập nhật Meal Plan của chính mình.

#### Main Flow
1. Member xác nhận áp dụng các món đã chọn.
2. Hệ thống chuyển các món đã xác nhận qua cùng ranh giới cập nhật Meal Plan tuần mà FR-09 sở hữu.
3. Hệ thống cập nhật các vị trí bữa tương ứng theo quy tắc hiện hành; không tạo pipeline ghi Meal Plan riêng cho FR-36.
4. Hệ thống thông báo việc áp dụng hoàn tất.

#### Alternative Flows
- Member chưa xác nhận hoặc hủy thao tác: không ghi hoặc thay đổi Meal Plan.

#### Postconditions
Chỉ các món được Member xác nhận mới được áp dụng; Meal Plan được cập nhật theo luồng chung FR-09.

#### Traceability
- **Parent FR:** [FR-36](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36).
- **Related FR:** [FR-09](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-09) (Meal Plan tuần và cập nhật các bữa).
- **Relevant BR:** [BR-35](../srs/BUSINESS-RULES.md#br-35), [BR-36](../srs/BUSINESS-RULES.md#br-36), [BR-37](../srs/BUSINESS-RULES.md#br-37).

#### Acceptance Coverage
- [AC-36.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-36) — Không ghi Meal Plan trước khi Member xác nhận.

---

## Remaining Dedicated Actor-Goal Use Cases

<a id="uc-37-1"></a>
### UC-37.1 — Điều chỉnh khẩu phần món trong Meal Plan
- **Goal / Primary Actor:** Member đặt số khẩu phần thực tế cho từng món trong ngày.
- **Trigger / Preconditions:** Member sở hữu Meal Plan và mở Nutrition Check.
- **Main Flow:** Actor nhập servings hợp lệ; hệ thống lưu và tính lại dữ liệu theo tỷ lệ.
- **Alternative / Security:** Giá trị ngoài phạm vi/plan không thuộc owner bị từ chối.
- **Postconditions:** Khẩu phần mới là đầu vào cho tổng hợp dinh dưỡng.
- **Traceability / Acceptance Coverage:** FR-37; [AC-37.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-37).

<a id="uc-37-2"></a>
### UC-37.2 — Xem tổng hợp dinh dưỡng ngày
- **Goal / Primary Actor:** Member đối chiếu 9 chỉ tiêu của một ngày với mức tham khảo cá nhân.
- **Trigger / Preconditions:** Ngày có Meal Plan; hồ sơ dinh dưỡng đủ và actor đã consent.
- **Main Flow:** Hệ thống cộng dữ liệu theo servings và hiển thị từng chỉ tiêu/đơn vị riêng.
- **Alternative / Security:** Không tạo điểm tổng hợp; thiếu dữ liệu được chuyển sang UC-37.3.
- **Postconditions:** Kết quả chỉ mang tính tham khảo và không thay Meal Plan.
- **Traceability / Acceptance Coverage:** FR-37; BR-39, BR-41; [AC-37.2, AC-37.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-37).

<a id="uc-37-3"></a>
### UC-37.3 — Xem cảnh báo món thiếu dữ liệu
- **Goal / Primary Actor:** Member biết món/chỉ tiêu nào làm kết quả chưa đầy đủ.
- **Trigger / Preconditions:** Ít nhất một Recipe Post/Ingredient thiếu dữ liệu dinh dưỡng tin cậy.
- **Main Flow:** Hệ thống liệt kê nguồn thiếu và đánh dấu tổng hợp là chưa đầy đủ.
- **Alternative / Security:** NULL không bị đổi thành 0; không suy diễn số liệu bằng AI.
- **Postconditions:** Người dùng hiểu giới hạn dữ liệu của kết quả.
- **Traceability / Acceptance Coverage:** FR-37; [AC-37.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-37).

<a id="uc-37-4"></a>
### UC-37.4 — Yêu cầu AI phân tích chênh lệch
- **Goal / Primary Actor:** Member có entitlement nhận giải thích/gợi ý món từ kho hiện có; Gemini hỗ trợ.
- **Trigger / Preconditions:** Actor chủ động yêu cầu; dữ liệu số đã được hệ thống tính và hồ sơ đủ điều kiện.
- **Main Flow:** Backend kiểm tra entitlement, gửi dữ liệu có nguồn và trả giải thích/gợi ý Recipe Post hiện có.
- **Alternative / Security:** Thiếu dữ liệu/provider lỗi được thông báo; AI không tự tạo số hoặc kê đơn.
- **Postconditions:** Kết quả chỉ là gợi ý, không tự sửa Meal Plan.
- **Traceability / Acceptance Coverage:** FR-37; BR-03, BR-41; [AC-37.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-37).

<a id="uc-37-5"></a>
### UC-37.5 — Xem phân tích tổng thể tuần
- **Goal / Primary Actor:** Member xem trung bình 7 ngày và vi chất cần lưu ý trong chế độ chay.
- **Trigger / Preconditions:** Meal Plan tuần thuộc owner và dữ liệu ngày được tính.
- **Main Flow:** Hệ thống tổng hợp 7 ngày, hiển thị từng chỉ tiêu và cho phép mở chi tiết ngày.
- **Alternative / Security:** Ngày thiếu dữ liệu được đánh dấu; không chấm Health Score.
- **Postconditions:** Tổng quan tuần được hiển thị mà không thay dữ liệu nguồn.
- **Traceability / Acceptance Coverage:** FR-37; [AC-37.3, AC-37.4, AC-37.6](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-37).

<a id="uc-37-6"></a>
### UC-37.6 — Xuất báo cáo dinh dưỡng PDF
- **Goal / Primary Actor:** Member tải báo cáo ngày hoặc tuần thuộc dữ liệu của mình.
- **Trigger / Preconditions:** Actor chọn Export sau khi Nutrition Check hoàn tất.
- **Main Flow:** Hệ thống dựng PDF ngày một trang hoặc tuần A4 hai trang với dữ liệu/cảnh báo hiện hành.
- **Alternative / Security:** Plan không thuộc owner bị chặn; thiếu dữ liệu vẫn ghi minh bạch, không điền số giả.
- **Postconditions:** File PDF được tải; dữ liệu nguồn không thay đổi.
- **Traceability / Acceptance Coverage:** FR-37; NFR-08; [AC-37.7–AC-37.9](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-37).

<a id="uc-38-1"></a>
### UC-38.1 — Xác nhận điều kiện và phạm vi dinh dưỡng
- **Goal / Primary Actor:** Member xác nhận mình thuộc phạm vi hỗ trợ và đồng ý disclaimer.
- **Trigger / Preconditions:** Actor truy cập capability dinh dưỡng lần đầu/chưa consent.
- **Main Flow:** Hệ thống trình bày ba tiêu chí và ranh giới y tế. Member chọn một checkbox cam kết đủ điều kiện, chủ động nhấn xác nhận; Backend lưu `ELIGIBLE` cùng timestamp gần nhất trong cùng giao dịch và trả kết quả. Giao diện sau đó chuyển sang Hồ sơ dinh dưỡng FR-35. Eligibility confirmation độc lập với `health_data_consent_at` của FR-35.
- **Alternative / Security:** Member chủ động xác nhận không đủ điều kiện thì Backend lưu `INELIGIBLE` cùng timestamp; Backend chặn mọi chức năng dinh dưỡng cá nhân, kể cả đọc lịch sử, nhưng giữ dữ liệu trong Database. Member có thể xác nhận lại bất cứ lúc nào. Hủy, quay lại, đóng/thoát trước khi xác nhận hoặc lỗi/validation không hợp lệ không lưu thay đổi; không có draft/autosave.
- **Postconditions:** `NOT_CONFIRMED` ban đầu có timestamp `NULL`; sau xác nhận thành công trạng thái là `ELIGIBLE` hoặc `INELIGIBLE` với timestamp lần xác nhận thành công gần nhất. Không lưu lịch sử xác nhận.
- **Traceability / Acceptance Coverage:** FR-38; BR-41; [AC-38.1–AC-38.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-38).

<a id="uc-38-2"></a>
### UC-38.2 — Xem lại disclaimer dinh dưỡng
- **Goal / Primary Actor:** Member xem phạm vi hỗ trợ và tuyên bố không thay thế tư vấn y tế.
- **Trigger / Preconditions:** Actor mở thông tin disclaimer.
- **Main Flow:** Hệ thống hiển thị nội dung hiện hành và giải thích vai trò tham khảo của BMI/dinh dưỡng.
- **Alternative / Security:** Không biến nội dung thành chẩn đoán hoặc khuyến nghị điều trị.
- **Postconditions:** Trạng thái dữ liệu không đổi.
- **Traceability / Acceptance Coverage:** FR-38; BR-39, BR-41; [AC-38.7](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-38).

<a id="uc-38-3"></a>
### UC-38.3 — Cập nhật điều kiện sức khỏe dinh dưỡng
- **Goal / Primary Actor:** Member cập nhật eligibility khi hoàn cảnh thay đổi.
- **Trigger / Preconditions:** Actor đăng nhập và mở nutrition settings.
- **Main Flow:** Member chọn trạng thái mới và chủ động xác nhận lưu; Backend cập nhật trạng thái cùng timestamp trong một giao dịch. Nếu trạng thái mới là `ELIGIBLE`, Backend khôi phục quyền truy cập theo phân quyền hiện hành.
- **Alternative / Security:** Chuyển sang `INELIGIBLE` lập tức chặn mọi truy cập/xử lý dinh dưỡng cá nhân, kể cả đọc dữ liệu lịch sử, sau khi Database cập nhật thành công; dữ liệu lịch sử không bị xóa. Không tự động tính lại khi xác nhận lại. Hủy/thoát, validation thất bại hoặc lỗi Backend giữ nguyên trạng thái và timestamp trước đó. Không có lưu nháp/tự lưu/lịch sử xác nhận.
- **Postconditions:** Backend dùng trạng thái hiện tại để kiểm soát request tiếp theo; xác nhận `ELIGIBLE` thành công chuyển giao diện tới FR-35.
- **Traceability / Acceptance Coverage:** FR-38; [AC-38.4–AC-38.7](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-38).

<a id="uc-39-1"></a>
### UC-39.1 — Xem ước tính 9 chỉ tiêu của Recipe Post
- **Goal / Primary Actor:** Guest/Member xem dữ liệu dinh dưỡng trên toàn công thức và một khẩu phần.
- **Trigger / Preconditions:** Bài công khai có ingredient amount/unit; dữ liệu nội bộ khả dụng.
- **Main Flow:** Hệ thống quy đổi gram, cộng 9 chỉ tiêu và chia theo servings.
- **Alternative / Security:** Không gọi API ngoài, không tạo điểm sức khỏe; thiếu dữ liệu chuyển sang UC-39.2.
- **Postconditions:** Ước tính có đơn vị/giới hạn được hiển thị.
- **Traceability / Acceptance Coverage:** FR-39; [AC-39.1, AC-39.2, AC-39.4, AC-39.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-39).

<a id="uc-39-2"></a>
### UC-39.2 — Xem cảnh báo Ingredient chưa hỗ trợ
- **Goal / Primary Actor:** Guest/Member biết thành phần nào làm ước tính chưa đầy đủ.
- **Trigger / Preconditions:** Recipe Post chứa Ingredient không có đủ dữ liệu hỗ trợ.
- **Main Flow:** Hệ thống liệt kê ingredient/chỉ tiêu thiếu và không tính phần không có căn cứ.
- **Alternative / Security:** NULL hiển thị `Chưa đủ dữ liệu`, không thành 0.
- **Postconditions:** Provenance/độ đầy đủ được trình bày minh bạch.
- **Traceability / Acceptance Coverage:** FR-39; [AC-39.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-39).

<a id="uc-40-1"></a>
### UC-40.1 — Công khai bài có Ingredient ngoài catalog dinh dưỡng
- **Goal / Primary Actor:** Expert công khai công thức hợp lệ dù một ingredient chưa hỗ trợ nutrition.
- **Trigger / Preconditions:** Recipe Post đạt validation/conversion publish; ingredient dinh dưỡng có thể chưa map.
- **Main Flow:** Backend không dùng thiếu nutrition để chặn publish và lưu bài `PUBLISHED`.
- **Alternative / Security:** Quy tắc amount/unit/conversion của publish vẫn bắt buộc.
- **Postconditions:** Bài công khai nhưng không tự được coi là nutrition-complete.
- **Traceability / Acceptance Coverage:** FR-40; [AC-40.1, AC-40.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-40).

<a id="uc-40-2"></a>
### UC-40.2 — Xem nhãn chưa hỗ trợ dinh dưỡng
- **Goal / Primary Actor:** Guest/Member hiểu hạn chế dữ liệu của Recipe Post.
- **Trigger / Preconditions:** Bài chứa ingredient chưa hỗ trợ nutrition.
- **Main Flow:** Hệ thống hiển thị nhãn/cảnh báo tại ingredient và phần nutrition.
- **Alternative / Security:** Không hiển thị 0 giả hoặc phán xét sức khỏe.
- **Postconditions:** Nội dung bài vẫn xem được, giới hạn dữ liệu rõ ràng.
- **Traceability / Acceptance Coverage:** FR-40; [AC-40.2, AC-40.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-40).

<a id="uc-41-1"></a>
### UC-41.1 — Tìm và lọc Ingredient dinh dưỡng
- **Goal / Primary Actor:** Administrator tra cứu catalog nội bộ.
- **Trigger / Preconditions:** Admin đăng nhập và mở Nutrition Ingredient Management.
- **Main Flow:** Hệ thống trả danh sách/chi tiết theo keyword, status và `nutrition_supported`.
- **Alternative / Security:** Người không phải Admin bị chặn.
- **Postconditions:** Dữ liệu chỉ được đọc.
- **Traceability / Acceptance Coverage:** FR-41; NFR-09; [AC-41.1](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-41).

<a id="uc-41-2"></a>
### UC-41.2 — Thêm Ingredient dinh dưỡng
- **Goal / Primary Actor:** Administrator tạo Ingredient có provenance, kể cả khi chưa đủ 9 chỉ tiêu.
- **Trigger / Preconditions:** Admin cung cấp tên, `source_name`, `reference_date` và dữ liệu hiện có.
- **Main Flow:** Backend lưu nullable nutrition fields với `nutrition_supported=0`; chỉ cho bật support khi đủ 9 chỉ tiêu và metadata nguồn theo schema.
- **Alternative / Security:** NULL khác 0; dữ liệu thiếu không được bật support; AI không được ghi catalog.
- **Postconditions:** Ingredient tồn tại với trạng thái hỗ trợ chính xác.
- **Traceability / Acceptance Coverage:** FR-41; BR-52; [AC-41.1, AC-41.2, AC-41.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-41).

<a id="uc-41-3"></a>
### UC-41.3 — Chỉnh dữ liệu và nguồn Ingredient
- **Goal / Primary Actor:** Administrator cập nhật số liệu/provenance đáng tin cậy.
- **Trigger / Preconditions:** Ingredient tồn tại; actor là Admin.
- **Main Flow:** Backend validate, lưu 9 field/source metadata và kiểm tra invariant khi support bật.
- **Alternative / Security:** Thiếu field bắt buộc khi support=1 bị từ chối; AI không tự sửa.
- **Postconditions:** Dữ liệu mới áp dụng cho phép tính sau, có audit phù hợp.
- **Traceability / Acceptance Coverage:** FR-41; BR-52; [AC-41.2, AC-41.5](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-41).

<a id="uc-41-4"></a>
### UC-41.4 — Đổi trạng thái Ingredient
- **Goal / Primary Actor:** Administrator ngừng/kích hoạt lại catalog item an toàn.
- **Trigger / Preconditions:** Ingredient tồn tại; actor chọn `ACTIVE`/`INACTIVE` hoặc support flag phù hợp.
- **Main Flow:** Backend kiểm tra invariant, cập nhật trạng thái mà không xóa tham chiếu.
- **Alternative / Security:** Bật nutrition support khi thiếu dữ liệu bị từ chối.
- **Postconditions:** Trạng thái mới có hiệu lực cho lookup/tính toán sau.
- **Traceability / Acceptance Coverage:** FR-41; BR-52, BR-53; [AC-41.2, AC-41.4](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-41).

<a id="uc-41-5"></a>
### UC-41.5 — Xem Recipe Post đang dùng Ingredient
- **Goal / Primary Actor:** Administrator đánh giá tác động trước khi thay trạng thái.
- **Trigger / Preconditions:** Ingredient tồn tại.
- **Main Flow:** Hệ thống truy vấn các Recipe Post tham chiếu và trả danh sách có phân trang.
- **Alternative / Security:** Ingredient đang được tham chiếu không được hard-delete.
- **Postconditions:** Admin có impact view; dữ liệu không đổi.
- **Traceability / Acceptance Coverage:** FR-41; BR-53; [AC-41.3](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-41).

---

<a id="fr-37"></a>
## FR-37 — Khai báo khẩu phần, kiểm tra dinh dưỡng menu ngày/tuần và xuất báo cáo PDF

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-37).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Member đã đăng nhập tài khoản hợp lệ (FR-03).
  - Khoảng thời gian được chọn trong Lịch ăn (ngày hoặc tuần) có ít nhất một món ăn được xếp vào bữa (Sáng, Trưa hoặc Tối) (FR-33).
- **Kích hoạt (Trigger):**
  - Member bấm nút "Kiểm tra dinh dưỡng ngày", "Phân tích tổng thể", hoặc "Xuất báo cáo PDF" trên giao diện Lịch ăn tuần / Theo dõi dinh dưỡng.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow — Kiểm tra dinh dưỡng ngày):**
  - Bước 1: Member mở xem chi tiết một ngày trong Lịch ăn tuần.
  - Bước 2: Tại mỗi món ăn đã xếp vào các bữa (Sáng, Trưa, Tối), hệ thống hiển thị số khẩu phần mặc định là 1. Member có thể điều chỉnh số khẩu phần (từ 0.5 đến 10.0, bước nhảy 0.5).
  - Bước 3: Member nhấn nút "Kiểm tra dinh dưỡng ngày".
  - Bước 4: Hệ thống duyệt qua tất cả các món ăn trong 3 bữa của ngày đó:
    - Với mỗi món, hệ thống lấy dữ liệu 9 chỉ tiêu trên 1 khẩu phần đã tính toán theo công thức chuẩn (FR-39).
    - Nhân giá trị dinh dưỡng của từng chỉ tiêu với số khẩu phần Member đã khai báo cho món đó (BR-47).
    - Nếu món có nguyên liệu chưa hỗ trợ tính dinh dưỡng, hệ thống ghi nhận cờ cảnh báo thiếu dữ liệu cho món đó (BR-48).
  - Bước 5: Hệ thống cộng dồn tổng giá trị của từng chỉ tiêu trong 9 chỉ tiêu cho toàn bộ các bữa ăn trong ngày:
    $$\text{Tổng ngày}(i) = \sum_{\text{món}} \left(\text{Chỉ tiêu}_i \text{ trên 1 khẩu phần} \times \text{Số khẩu phần}\right)$$
  - Bước 6: Nếu Member đã có hồ sơ dinh dưỡng tham khảo (FR-35), hệ thống so sánh Tổng ngày với Mức nhu cầu tham khảo cá nhân:
    - Tính chênh lệch định lượng ($\Delta = \text{Tổng ngày} - \text{Mức tham khảo}$).
    - Hiển thị thanh tiến trình trực quan biểu thị tỷ lệ % đạt được theo từng chỉ tiêu riêng biệt.
    - Tuyệt đối không tạo ra một "điểm số sức khỏe" (health score) tổng hợp hay gán nhãn "lành mạnh / không lành mạnh" đơn giản hóa (BR-45).
    - Hệ thống diễn giải từng chỉ tiêu theo mức tham khảo tương ứng, không dùng một tỷ lệ hoặc nhãn chung để thay thế ý nghĩa riêng của từng chỉ tiêu (BR-44).
  - Bước 7: Nếu có món ăn chứa nguyên liệu chưa có dữ liệu dinh dưỡng, hệ thống hiển thị thông báo cảnh báo màu vàng nổi bật: *"Một số món trong ngày chưa có dữ liệu dinh dưỡng đầy đủ; số liệu thực tế có thể cao hơn bảng tính toán"* kèm danh sách tên các món bị ảnh hưởng (BR-48).
  - Bước 8: Hệ thống hiển thị nhãn ghi chú minh bạch *"Dinh dưỡng dự kiến theo thực đơn"* và Tuyên bố từ chối trách nhiệm y tế (BR-39, BR-41).

- **Luồng phân tích tổng thể tuần (Sub-flow SBF-37.1 — Phân tích tổng thể tuần & So sánh 7 ngày):**
  - Bước 1: Member nhấn nút "So sánh & Thống kê 7 ngày" (hoặc "Phân tích tổng thể") tại màn hình Theo dõi dinh dưỡng.
  - Bước 2: Hệ thống truy xuất dữ liệu kế hoạch ăn của 7 ngày trong tuần được chọn (từ Thứ Hai đến Chủ Nhật).
  - Bước 3: Hệ thống kết xuất bảng ma trận so sánh trực quan đa chiều:
    - 7 cột tương ứng với 7 ngày trong tuần (Thứ Hai đến Chủ Nhật), hiển thị chi tiết số liệu của 9 chỉ tiêu cho từng ngày.
    - Cột "Trung bình/ngày" tính theo trung bình cộng 7 ngày:
      $$\text{Giá trị TB/ngày}(i) = \frac{\sum_{d=1}^{7} \text{Tổng ngày}_d(i)}{7}$$
    - Cột "Chuẩn tham khảo cá nhân (DRI)" và tỷ lệ % hoàn thành mục tiêu.
    - Thanh trạng thái / màu sắc trực quan (đạt / vượt / thiếu) cho từng ô chỉ tiêu theo ngày mà không tạo điểm số tổng hợp phán xét (BR-45).
  - Bước 4: Thống kê tỷ lệ ngày đạt chuẩn của từng chỉ tiêu (ví dụ: *6/7 ngày đạt nhu cầu Đạm*).
  - Bước 5: Phân tích các vi chất đặc thù ăn chay:
    - Phát hiện nguy cơ thiếu hụt tích lũy nếu chỉ tiêu quan trọng (như B12, Sắt non-heme, Canxi) thấp hơn ngưỡng khuyến nghị liên tiếp từ 3 ngày trở lên trong tuần.
    - Không phân tích Natri/muối vì chỉ tiêu này không thuộc bộ chín chỉ tiêu dinh dưỡng MVP.
  - Bước 6: Hiển thị kết quả phân tích trong giao diện chi tiết (Modal / Panel). Tuyệt đối không tạo ra một điểm số tổng hợp (Health Score) cho cả tuần (BR-45); hiển thị cảnh báo danh sách các ngày/món thiếu dữ liệu chuẩn (BR-48); hiển thị nhãn minh bạch "Dinh dưỡng dự kiến theo thực đơn tuần".

- **Luồng xuất báo cáo PDF (Sub-flow SBF-37.2 — Xuất báo cáo PDF tuần / ngày):**
  - Bước 1: Member nhấn nút "Xuất báo cáo PDF" (hỗ trợ nút xuất nhanh "Xuất PDF tuần này").
  - Bước 2: Hệ thống hiển thị hộp thoại chọn phạm vi xuất: "Báo cáo ngày hiện tại" hoặc "Báo cáo toàn bộ tuần này" (mặc định chọn Tuần hiện tại).
  - Bước 3: Hệ thống tổng hợp dữ liệu dinh dưỡng theo phạm vi được chọn (tính toán on-demand từ dữ liệu lịch ăn hiện hành, không truy vấn hay lưu trữ vào bảng báo cáo tĩnh).
  - Bước 4: Tạo tài liệu định dạng PDF chuẩn in A4: một trang đối với báo cáo ngày và hai trang đối với báo cáo tuần.
    - **Báo cáo ngày — 1 trang:** Logo/tiêu đề, thông tin Member và ngày được chọn, ba bữa cùng tên món/số khẩu phần dự kiến, bảng chín chỉ tiêu và mức tham khảo, cảnh báo thiếu dữ liệu, timestamp và tuyên bố miễn trừ y tế.
    - **Báo cáo tuần — 2 trang:**
      - **Trang 1 — Tổng quan & Bảng ma trận so sánh 7 ngày:**
        - Phần thông tin chung: Logo Mâm Xanh, Tiêu đề *"Báo Cáo Phân Tích Dinh Dưỡng Thực Đơn Tuần"*, Họ tên/Mã tài khoản Member, Thời điểm xuất dữ liệu (timestamp), Khoảng thời gian tuần (từ ngày... đến ngày...).
        - Thông tin thể trạng tham khảo (nếu có): Nhóm tuổi, giới tính, mức vận động, mức calorie mục tiêu (FR-35).
        - Bảng ma trận 9 chỉ tiêu x 7 ngày: hiển thị số liệu từng ngày (Thứ 2 đến Chủ nhật), cột trung bình tuần, cột chuẩn DRI cá nhân và tỷ lệ % đạt.
      - **Trang 2 — Chi tiết thực đơn, cảnh báo & miễn trừ y tế:**
        - Tóm tắt thực đơn tuần: Danh mục bữa ăn 3 bữa (Sáng, Trưa, Tối) từng ngày, tên món và số khẩu phần trong kỳ báo cáo.
        - Phân tích vi chất đặc thù & cảnh báo thiếu dữ liệu: Nhận diện vi chất đạt chuẩn, cảnh báo các món chứa nguyên liệu chưa có dữ liệu chuẩn (BR-48).
        - Tuyên bố miễn trừ y tế bắt buộc: In rõ ràng tuyên bố từ chối trách nhiệm y tế chuẩn theo BR-41.
  - Bước 5: Trình duyệt tải file PDF trực tiếp về thiết bị của Member.

- **Luồng thay thế (Alternative Flows):**
  - *AF-37.1 (Chưa khai báo hồ sơ dinh dưỡng cá nhân):* Nếu Member chưa khai báo hồ sơ dinh dưỡng theo FR-35, hệ thống vẫn hiển thị tổng 9 chỉ tiêu dinh dưỡng của thực đơn (theo ngày hoặc tuần), nhưng ở cột so sánh sẽ hiển thị giá trị khuyến nghị tiêu chuẩn của người trưởng thành theo USDA kèm liên kết gợi ý: *"Khai báo hồ sơ để xem chỉ số phù hợp với thể trạng cá nhân"*.
  - *AF-37.2 (Member nhờ AI phân tích chênh lệch):* Member bấm nút "Nhờ AI phân tích và đề xuất điều chỉnh". Hệ thống kiểm tra quyền dùng AI nâng cao của gói Pro (BR-02, BR-03). Nếu hợp lệ, hệ thống gửi bảng số liệu chênh lệch cho Gemini kèm yêu cầu đề xuất đổi một món trong bữa sang một công thức khác có sẵn trong hệ thống để cân bằng dinh dưỡng. AI trả về đề xuất; nếu Member đồng ý, hệ thống đổi món trong Lịch ăn.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-37.1 (Ngày hoặc tuần không có món ăn nào):* Nếu khoảng thời gian được chọn không có bất kỳ món ăn nào trong lịch ăn, các nút "Kiểm tra dinh dưỡng ngày", "So sánh & Thống kê 7 ngày" và "Xuất báo cáo PDF" bị vô hiệu hóa kèm thông báo *"Vui lòng thêm ít nhất một món ăn vào lịch ăn để phân tích/xuất báo cáo dinh dưỡng"*.
  - *EF-37.2 (Khẩu phần không hợp lệ):* Nếu người dùng nhập số khẩu phần nhỏ hơn 0.5 hoặc lớn hơn 10, hệ thống báo lỗi và khôi phục về giá trị hợp lệ gần nhất.
  - *EF-37.3 (Tạo PDF thất bại):* Nếu không thể tạo file PDF, hệ thống thông báo xuất báo cáo thất bại, không tạo file rỗng/hỏng và cho phép Member thử lại mà không thay đổi dữ liệu Lịch ăn.
  - *SF-37.1 (Kiểm soát quyền riêng tư):* Lịch ăn, kết quả kiểm tra dinh dưỡng và file PDF xuất ra thuộc quyền riêng tư của chính Member; tài khoản khác hoặc Guest không thể xem hoặc tải dữ liệu (RBAC theo NFR-08, NFR-09).

#### 5. Hậu điều kiện (Postconditions)
- Số khẩu phần đã điều chỉnh được lưu vào Lịch ăn ngày của Member.
- Kết quả kiểm tra 9 chỉ tiêu (ngày hoặc tuần) được hiển thị chi tiết, minh bạch đến từng chỉ tiêu thành phần.
- File báo cáo PDF được tạo on-demand và tải thành công về máy người dùng mà không tạo bản ghi dư thừa trong CSDL.

---

<a id="fr-38"></a>
## FR-38 — Xác nhận phạm vi hỗ trợ trước khi dùng chức năng dinh dưỡng

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-38).

#### 2. Tác nhân (Actors)
- **Primary Actor:**
  - `Member`: Người dùng đã đăng nhập có nhu cầu sử dụng các tính năng dinh dưỡng (thiết lập hồ sơ dinh dưỡng, kiểm tra menu ngày, nhận AI menu theo dinh dưỡng).
- **Secondary Actor / External System:**
  - `Cơ sở dữ liệu tham chiếu dinh dưỡng nội bộ`: Dữ liệu tham khảo dinh dưỡng được biên soạn chuẩn bị từ nguồn USDA FoodData Central và NIH DRI (không gọi runtime API bên ngoài theo BR-49).

#### 6. Tiền điều kiện (Preconditions) & Điều kiện kích hoạt (Trigger)
- **Preconditions:**
  - Người dùng đã đăng nhập với vai trò Member và tài khoản ở trạng thái `ACTIVE`.
- **Trigger:**
  - Member lần đầu tiên nhấp vào bất kỳ tính năng nào thuộc Module M10: menu "Hồ sơ dinh dưỡng" (FR-35), nút "Kiểm tra dinh dưỡng thực đơn ngày" (FR-37), hoặc yêu cầu "AI lập thực đơn theo nhu cầu dinh dưỡng" (FR-36).
  - Member chủ động truy cập mục Cài đặt dinh dưỡng để cập nhật lại điều kiện sức khỏe.

#### 7. Luồng sự kiện (Flow of Events)

##### A. Luồng Xác nhận đủ điều kiện sử dụng chức năng dinh dưỡng (UC-38.1)
1. **Main Flow (Xác nhận đủ điều kiện thành công):**
   - Bước 1: Member nhấp vào tính năng dinh dưỡng (ví dụ: "Hồ sơ dinh dưỡng").
   - Bước 2: Backend kiểm tra trạng thái eligibility. Chỉ `ELIGIBLE` được đi tiếp; `NOT_CONFIRMED` và `INELIGIBLE` bị từ chối truy cập dữ liệu dinh dưỡng cá nhân. API eligibility vẫn cho phép Member `INELIGIBLE` xác nhận lại.
   - Bước 3: Giao diện hiển thị màn hình "Xác nhận phạm vi hỗ trợ dinh dưỡng".
   - Bước 4: Màn hình trình bày rõ ràng:
     - Khối 1: Ba điều kiện bắt buộc (Từ đủ 18 tuổi; Không mang thai hoặc đang cho con bú; Không có bệnh lý yêu cầu chế độ ăn điều trị riêng) theo BR-42.
     - Khối 2: Tuyên bố ranh giới thông tin: Dữ liệu mang tính tham khảo lập kế hoạch theo chuẩn USDA/NIH, không phải chẩn đoán hay điều trị y khoa theo BR-41.
     - Khối 3: Hộp kiểm cam kết: *"Tôi xác nhận tôi từ đủ 18 tuổi trở lên, không mang thai/cho con bú, không có nhu cầu ăn kiêng điều trị bệnh, và hiểu rằng các khuyến nghị dinh dưỡng chỉ mang tính chất tham khảo."*
   - Bước 5: Member tích chọn hộp kiểm và nhấn nút "Xác nhận và tiếp tục".
   - Bước 6: Ứng dụng gửi yêu cầu xác nhận lên máy chủ hệ thống.
   - Bước 7: Backend chỉ khi nhận request xác nhận hợp lệ mới lưu `ELIGIBLE` và timestamp xác nhận thành công gần nhất trong cùng một giao dịch.
   - Bước 8: Hệ thống phản hồi xác nhận thành công.
   - Bước 9: Giao diện chuyển hướng Member sang màn hình Khai báo Hồ sơ dinh dưỡng cá nhân (`FR-35`) để nhập các chỉ số tuổi, giới tính, chiều cao, cân nặng, mức vận động.
2. **Alternative Flow (Member xác nhận không đủ điều kiện):**
   - Bước 1: Member chủ động chọn "Tôi không thuộc nhóm đối tượng trên" và xác nhận lựa chọn.
   - Bước 2: Backend lưu `INELIGIBLE` và timestamp xác nhận thành công gần nhất trong cùng giao dịch.
   - Bước 3: Hệ thống hiển thị thông báo giải thích rõ ràng và lịch sự:
     - *"Rất tiếc! Hệ thống Vegetarian Support Application hiện chỉ cung cấp tính toán dinh dưỡng mẫu cho người trưởng thành khỏe mạnh bình thường. Đối với người dưới 18 tuổi, phụ nữ mang thai/cho con bú hoặc người có bệnh lý nền, nhu cầu dinh dưỡng đòi hỏi phác đồ chuyên biệt từ bác sĩ chuyên khoa hoặc chuyên gia dinh dưỡng lâm sàng. Để đảm bảo an toàn tuyệt đối cho sức khỏe của bạn, hệ thống xin phép tạm dừng chức năng dinh dưỡng đối với tài khoản này."*
   - Bước 4: Hệ thống khóa quyền truy cập các chức năng dinh dưỡng (FR-35, FR-36, FR-37) đối với tài khoản.
   - Bước 5: Hệ thống điều hướng Member về trang Khám phá bài viết.
   - Bước 6: **Bảo toàn chức năng thông thường (BR-42):** Member VẪN SỬ DỤNG HOÀN TOÀN BÌNH THƯỜNG toàn bộ các chức năng khác của hệ thống: xem bài công thức, tìm kiếm, lọc theo nguyên liệu/loại ăn chay, lưu bài viết vào `Saved Recipes`, xếp lịch ăn 3 bữa thủ công trong `Meal Planner`, bình luận và hỏi đáp Chatbot AI kiến thức chay chung (FR-51); riêng Chuyên gia được tạo và công khai Recipe Post.
3. **Alternative Flow (Hủy hoặc rời màn hình trước khi xác nhận):**
   - Member chọn Hủy/quay lại, đóng màn hình/thoát trang hoặc mới chọn dữ liệu nhưng chưa nhấn xác nhận.
   - Ứng dụng không gửi request cập nhật; Backend không lưu thay đổi. Trạng thái/timestamp trước đó được giữ nguyên. Không có lưu nháp hoặc tự động lưu.
4. **Alternative Flow (Validation hoặc Backend lỗi):**
   - Request không hợp lệ hoặc xử lý Backend thất bại thì không cập nhật trạng thái/timestamp; trả lỗi để Member có thể thử lại.

##### B. Luồng Xem lại tuyên bố miễn trừ y tế và phạm vi hỗ trợ (UC-38.2)
1. **Main Flow:**
   - Bước 1: Member truy cập mục Thông tin dinh dưỡng hoặc nhấp vào liên kết "Phạm vi hỗ trợ & Tuyên bố y tế" tại các trang dinh dưỡng.
   - Bước 2: Hệ thống hiển thị đầy đủ văn bản tuyên bố ranh giới thông tin tham khảo, cơ sở dữ liệu nguồn USDA/NIH, và 3 điều kiện áp dụng an toàn của hệ thống.

##### C. Luồng Cập nhật lại trạng thái điều kiện sức khỏe (UC-38.3)
1. **Main Flow:**
   - Bước 1: Member đã từng xác nhận đủ điều kiện trước đây, nay tình trạng sức khỏe thay đổi (ví dụ: đang mang thai hoặc phát hiện bệnh lý cần ăn kiêng).
   - Bước 2: Member vào mục "Cài đặt dinh dưỡng", chọn "Cập nhật điều kiện sức khỏe".
   - Bước 3: Member chọn `INELIGIBLE` và chủ động xác nhận lưu.
   - Bước 4: Backend cập nhật trạng thái cùng timestamp thành công gần nhất trong một giao dịch; nếu validation/lỗi xử lý xảy ra, giữ nguyên trạng thái và timestamp cũ.
   - Bước 5: Sau khi Database cập nhật thành công, Backend chặn toàn bộ đọc/xử lý dữ liệu dinh dưỡng cá nhân (FR-35, FR-36, FR-37), bao gồm đọc lịch sử. Dữ liệu lịch sử được giữ nguyên; các chức năng không-dinh-dưỡng hoạt động bình thường.
   - Bước 6: Khi Member chủ động xác nhận lại `ELIGIBLE`, Backend khôi phục quyền theo phân quyền hiện hành; không tự động tính lại dữ liệu cũ.

##### D. Luồng An ninh — Kiểm soát truy cập chức năng dinh dưỡng ở tầng máy chủ
1. **Main Flow (Kiểm soát chặt chẽ phía máy chủ):**
   - Đối với mọi request đọc/ghi/xử lý dữ liệu dinh dưỡng cá nhân thuộc FR-35/36/37, Backend kiểm tra trạng thái eligibility trước khi truy xuất dữ liệu.
   - Chỉ `ELIGIBLE` được đi tiếp; `NOT_CONFIRMED` và `INELIGIBLE` bị từ chối, kể cả khi gọi API trực tiếp. Endpoint xác nhận vẫn mở để `INELIGIBLE` xác nhận lại. Guard cho FR-36/37 chỉ được nghiệm thu sau khi endpoint do các FR đó sở hữu tồn tại và đã tích hợp.

#### 8. Hậu điều kiện (Postconditions)
- Khi xác nhận đủ điều kiện thành công: `ELIGIBLE` và timestamp được lưu gắn với Member; quyền truy cập cá nhân được mở và giao diện chuyển đến FR-35.
- Khi Member xác nhận không đủ điều kiện: lưu `INELIGIBLE` và timestamp; khóa đọc/xử lý dữ liệu dinh dưỡng cá nhân, bao gồm lịch sử, nhưng giữ nguyên dữ liệu. Member được xác nhận lại bất cứ lúc nào.
- Khi chưa xác nhận: giữ `NOT_CONFIRMED`, timestamp `NULL`; Hủy/thoát không tương đương với `INELIGIBLE` và không lưu.
- FR-35 sở hữu hồ sơ/BMI/tính toán; FR-36 sở hữu AI menu; FR-37 sở hữu kiểm tra menu/lịch sử. FR-38 sở hữu trạng thái eligibility và hợp đồng guard, không thay đổi dữ liệu/thuật toán của các FR này. Các chức năng không-dinh-dưỡng vẫn hoạt động theo phân quyền hiện hành.

---

<a id="fr-39"></a>
## FR-39 — Tính toán ước tính 9 chỉ tiêu dinh dưỡng cho công thức

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-39).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Bài công thức đã khai báo số khẩu phần (servings $\ge 1$) và có ít nhất một nguyên liệu hợp lệ (FR-16, FR-19).
- **Kích hoạt (Trigger):**
  - Tác giả nhấn lưu bài công thức, hoặc người dùng truy cập trang xem chi tiết Recipe Post (FR-18).

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Hệ thống đọc danh sách nguyên liệu của bài công thức kèm số lượng số dương và đơn vị đo chuẩn thuộc bảng `UNIT` (FR-19, BR-73).
  - Bước 2: Với mỗi nguyên liệu, hệ thống quy đổi định lượng sang đơn vị gram chuẩn:
    - Nếu đơn vị thuộc chiều `MASS`: $1\text{ g} = 1\text{ g}$; $1\text{ kg} = 1.000\text{ g}$.
    - Nếu đơn vị thuộc chiều `VOLUME` (ml, l) hoặc `COUNT` (quả, củ, bìa...): hệ thống sử dụng hệ số quy đổi ($conversion\_factor$) từ bảng `INGREDIENT_UNIT_CONVERSION` đối với đúng `ingredientId` và `unitId` đó:
      $$\text{Trọng lượng (g)} = \text{Số lượng} \times conversion\_factor$$
    - (Lưu ý: Nếu một nguyên liệu thiếu quy tắc chuyển đổi cần thiết, việc xuất bản đã bị chặn từ bước thẩm định FR-25, BR-14, BR-73).
  - Bước 3: Hệ thống truy vấn danh mục nguyên liệu dinh dưỡng nội bộ (`Nutrition Profile`) để ánh xạ từng nguyên liệu với bản ghi dinh dưỡng tương ứng (BR-46).
  - Bước 4: Với mỗi nguyên liệu đã được ánh xạ thành công, hệ thống tính toán giá trị của từng chỉ tiêu trong 9 chỉ tiêu theo trọng lượng gram thực tế đã quy đổi:
    $$\text{Giá trị chỉ tiêu}_i = \frac{\text{Trọng lượng (g)}}{100} \times \text{Chỉ số chuẩn trên 100g}_i$$
  - Bước 5: Hệ thống cộng tổng giá trị của từng chỉ tiêu cho tất cả các nguyên liệu đã ánh xạ thành công để ra Tổng dinh dưỡng toàn bài công thức.
  - Bước 6: Hệ thống chia tổng dinh dưỡng của toàn bộ công thức cho số khẩu phần (servings) đã khai báo để tính ra Dinh dưỡng ước tính trên 1 khẩu phần (BR-43):
    $$\text{Dinh dưỡng trên 1 khẩu phần}_i = \frac{\text{Tổng dinh dưỡng}_i}{\text{Số khẩu phần}}$$
  - Bước 7: Nếu 100% nguyên liệu đều có dữ liệu dinh dưỡng đầy đủ, hệ thống đánh dấu trạng thái dinh dưỡng của công thức là `Đầy đủ` (Eligible cho pool AI menu theo BR-40).
  - Bước 8: Hệ thống hiển thị bảng dinh dưỡng 9 chỉ tiêu trên trang chi tiết công thức kèm Tuyên bố từ chối trách nhiệm y tế (BR-39, BR-41).
- **Luồng thay thế (Alternative Flows):**
  - *AF-39.1 (Công thức chứa nguyên liệu chưa có dữ liệu dinh dưỡng):* Nếu có một hoặc nhiều nguyên liệu chưa được ánh xạ trong danh mục nội bộ:
    - Hệ thống vẫn tính tổng của các nguyên liệu đã có số liệu.
    - Hệ thống đánh dấu trạng thái dinh dưỡng của công thức là `Chưa đầy đủ` (Incomplete).
    - Trên bảng dinh dưỡng, hệ thống hiển thị nhãn cảnh báo màu vàng: *"Ước tính chưa đầy đủ: công thức có X nguyên liệu chưa hỗ trợ tính dinh dưỡng"* kèm danh sách tên các nguyên liệu đó (BR-48).
    - Hệ thống tuyệt đối không tự gán số liệu dinh dưỡng của nguyên liệu thiếu bằng 0 trong các phép so sánh nghiêm ngặt và không cho AI tự bịa số liệu (BR-48).
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-39.1 (Không có nguyên liệu nào có hồ sơ dinh dưỡng):* Nếu tất cả nguyên liệu đã chọn từ catalog chuẩn đều chưa có hồ sơ trong danh mục dinh dưỡng nội bộ, hệ thống hiển thị thông báo: *"Công thức chưa hỗ trợ tính toán dinh dưỡng do chưa có dữ liệu nguyên liệu chuẩn"* thay vì hiển thị toàn bộ số 0.
  - *SF-39.1 (Cấm gọi API ngoài realtime):* Hệ thống xử lý tính toán 100% dựa trên cơ sở dữ liệu nội bộ đã được Administrator phê duyệt (BR-49, BR-51); không thực hiện cuộc gọi API ra ngoài mạng Internet trong lúc người dùng xem bài viết nhằm bảo vệ hiệu năng và tính ổn định (NFR-02, NFR-10).

#### 5. Hậu điều kiện (Postconditions)
- Bảng ước tính 9 chỉ tiêu dinh dưỡng của công thức được lưu kèm hoặc tính toán sẵn sàng hiển thị.
- Trạng thái dinh dưỡng (`Đầy đủ` hoặc `Chưa đầy đủ`) được xác định chính xác để phục vụ bộ lọc tìm kiếm (FR-08) và pool AI lập menu (FR-36).

---

<a id="fr-40"></a>
## FR-40 — Công khai Recipe Post chứa nguyên liệu chuẩn thiếu dữ liệu dinh dưỡng

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-40).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Tác giả là Chuyên gia đã đăng nhập và hoàn thành các trường thông tin bắt buộc của bài công thức (FR-04, FR-16, FR-19).
  - Bài công thức có ít nhất một nguyên liệu đã chọn từ catalog chuẩn FR-18 nhưng chưa có hồ sơ trong danh mục dinh dưỡng nội bộ.
- **Kích hoạt (Trigger):**
  - Tác giả nhấn nút "Đăng công thức" (Publish Recipe).

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Tác giả hoàn thành soạn thảo công thức; trong danh sách có một số nguyên liệu chuẩn chưa có hồ sơ trong danh mục dinh dưỡng nội bộ.
  - Bước 2: Tác giả nhấn "Đăng công thức".
  - Bước 3: Hệ thống thực hiện quy trình kiểm tra hợp lệ thông tin bài viết theo bộ quy tắc Recipe Validation Profile chuẩn của FR-16 (tiêu đề, khẩu phần, thời gian chuẩn bị và nấu, loại ăn chay; nguyên liệu theo FR-19; nội dung hướng dẫn chuẩn bị/chế biến từ 10 đến 5.000 ký tự theo FR-16, BR-19).
  - Bước 4: Kiểm tra hợp lệ thành công; hệ thống xác định có nguyên liệu chưa ánh xạ được với bảng dinh dưỡng nội bộ.
  - Bước 5: Hệ thống cho phép công khai trực tiếp bài viết lên nền tảng ngay lập tức mà không chặn và không đưa vào hàng đợi duyệt trước (BR-07, BR-50, BR-59).
  - Bước 6: Hệ thống đánh dấu trạng thái dinh dưỡng của bài viết là `Chưa đầy đủ` (Incomplete Nutrition Data) và đánh dấu cờ `Loại trừ khỏi AI Menu Dinh dưỡng` (BR-40).
  - Bước 7: Trên trang chi tiết công thức công khai, bên cạnh tên nguyên liệu chuẩn chưa có hồ sơ dinh dưỡng, hệ thống hiển thị biểu tượng ghi chú nhỏ: *"Chưa hỗ trợ tính dinh dưỡng"* (BR-48).
  - Bước 8: Bảng ước tính dinh dưỡng của bài viết hiển thị giá trị cộng dồn của các nguyên liệu đã biết kèm dòng ghi chú cảnh báo minh bạch (FR-39).
- **Luồng thay thế (Alternative Flows):**
  - *AF-40.1 (Sau này Admin bổ sung hồ sơ dinh dưỡng):* Khi Administrator bổ sung hồ sơ dinh dưỡng cho nguyên liệu đã có trong catalog chuẩn (FR-41), hệ thống tính lại dinh dưỡng của các công thức sử dụng mục đó; nếu 100% nguyên liệu đã có dữ liệu được hỗ trợ, cờ cảnh báo được gỡ bỏ và bài viết đủ điều kiện tham gia pool AI menu.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-40.1 (Tác giả bỏ trống tên hoặc định lượng nguyên liệu):* Nếu nguyên liệu không có tên hoặc không có định lượng (số lượng + đơn vị đo chuẩn hoặc "vừa đủ"), hệ thống chặn đăng theo quy tắc validation bắt buộc (FR-16, FR-19), không liên quan đến việc nguyên liệu có trong danh mục dinh dưỡng hay không.
  - *SF-40.1 (Ngăn chặn AI đưa bài thiếu số liệu vào menu dinh dưỡng):* Khi module AI Menu (FR-36) truy vấn danh sách công thức để lập thực đơn theo calo/macro, truy vấn cơ sở dữ liệu bắt buộc lọc bỏ các bài viết có cờ `Chưa đầy đủ dinh dưỡng` để đảm bảo an toàn cho người dùng (BR-40).

#### 5. Hậu điều kiện (Postconditions)
- Recipe Post được công khai ngay lập tức cho toàn bộ cộng đồng xem và tương tác.
- Nguyên liệu chuẩn chưa có hồ sơ dinh dưỡng được hiển thị minh bạch trạng thái.
- Bài viết bị loại trừ khỏi pool gợi ý AI Menu theo mục tiêu dinh dưỡng.

---

<a id="fr-41"></a>
## FR-41 — Administrator quản lý danh mục nguyên liệu dinh dưỡng nội bộ

Source: [Functional Requirements](../srs/FUNCTIONAL-REQUIREMENTS.md#fr-41).

#### 3. Tiền điều kiện & Kích hoạt (Preconditions & Triggers)
- **Tiền điều kiện:**
  - Người dùng đã đăng nhập với tài khoản có vai trò Administrator (FR-03, NFR-09).
- **Kích hoạt (Trigger):**
  - Administrator truy cập phân hệ "Quản lý dinh dưỡng" -> "Danh mục nguyên liệu dinh dưỡng" trên thanh điều hướng quản trị.

#### 4. Luồng xử lý chi tiết (Flows)
- **Luồng chính (Main Flow):**
  - Bước 1: Administrator truy cập danh sách nguyên liệu dinh dưỡng; hệ thống hiển thị danh sách dạng bảng phân trang gồm: Tên nguyên liệu, Năng lượng, Đạm, Carb, Chất béo, Nguồn tham chiếu (USDA/NIH), Ngày cập nhật, trạng thái danh mục (`status`: `ACTIVE`/`INACTIVE`), trạng thái hỗ trợ tính toán (`nutrition_supported`) và Số lượng công thức đang sử dụng.
  - Bước 2: Administrator nhấn nút "Thêm nguyên liệu mới".
  - Bước 3: Hệ thống hiển thị biểu mẫu yêu cầu nhập liệu:
    - Tên nguyên liệu tiếng Việt (chuẩn hóa, không trùng lặp); MVP không quản lý tên tiếng Anh hoặc đa ngôn ngữ.
    - Nhóm thực phẩm (Rau củ, Đậu & Chế phẩm, Ngũ cốc, Các loại hạt, Trái cây, Gia vị chay).
    - Giá trị của 9 chỉ tiêu cốt lõi trên 100g (Năng lượng kcal $\ge 0$, Protein g $\ge 0$, Carb g $\ge 0$, Fat g $\ge 0$, Fiber g $\ge 0$, Calcium mg $\ge 0$, Iron mg $\ge 0$, Vitamin B12 mcg $\ge 0$, Zinc mg $\ge 0$); chỉ tiêu chưa có dữ liệu có thể để trống khi Ingredient chưa được hỗ trợ tính toán (`nutrition_supported = 0`), còn `0` là giá trị thật.
    - Tên nguồn dữ liệu (`source_name`, chọn USDA FoodData Central hoặc NIH/Viện Dinh dưỡng) và ngày đối chiếu (`reference_date`) theo ràng buộc database; URL nguồn (`source_url`) có thể chưa có khi `nutrition_supported = 0`.
  - Bước 4: Administrator nhập dữ liệu hiện có và nhấn "Lưu nguyên liệu"; các chỉ tiêu dinh dưỡng chưa biết được để trống, không nhập `0` thay thế.
  - Bước 5: Hệ thống kiểm tra hợp lệ: không chấp nhận giá trị dinh dưỡng âm; tên không được trùng với nguyên liệu đang hoạt động; tuân thủ các ràng buộc nguồn hiện hành. Khi `nutrition_supported = 0`, thiếu chỉ tiêu không chặn lưu; khi bật `nutrition_supported = 1`, phải có đủ 9 chỉ tiêu khác `NULL` và đủ `source_name`, `source_url`, `reference_date` (BR-52).
  - Bước 6: Hệ thống lưu bản ghi mới với `nutrition_supported = 0` mặc định; trạng thái danh mục (`status`) được quản lý riêng và không có nghĩa rằng nguyên liệu đã đủ điều kiện tham gia tính toán.
  - Bước 7: Hệ thống cập nhật lại danh sách và thông báo thêm mới thành công.
- **Luồng thay thế (Alternative Flows):**
  - *AF-41.1 (Chỉnh sửa nguyên liệu hiện có):* Administrator chọn một nguyên liệu và chỉnh sửa số liệu. Sau khi lưu hợp lệ, hệ thống cập nhật bản ghi và đánh dấu các công thức đang liên kết để tự động cập nhật lại dinh dưỡng trong chu kỳ tính toán tiếp theo.
  - *AF-41.2 (Ngừng hỗ trợ nguyên liệu trong danh mục):* Administrator chuyển `status` của nguyên liệu sang `INACTIVE`. Các công thức cũ đã sử dụng nguyên liệu này vẫn giữ nguyên tham chiếu lịch sử, nhưng nguyên liệu sẽ không xuất hiện trong gợi ý chọn nguyên liệu mới cho người dùng (BR-53).
  - *AF-41.3 (Kích hoạt lại nguyên liệu trong danh mục):* Administrator có thể chuyển `status` về `ACTIVE` cho một nguyên liệu đã ngừng hoạt động. Thay đổi `status` không tự bật `nutrition_supported`.
  - *AF-41.4 (Xem công thức đang liên kết):* Administrator nhấn vào số lượng công thức đang sử dụng để xem danh sách chi tiết các Recipe Post đang tham chiếu đến nguyên liệu này.
  - *AF-41.5 (Bật hỗ trợ tính toán dinh dưỡng):* Với Ingredient đã lưu ở `nutrition_supported = 0`, Administrator yêu cầu bật hỗ trợ. Hệ thống chỉ đặt `nutrition_supported = 1` khi có đủ chín chỉ tiêu và `source_name`, `source_url`, `reference_date`; nếu thiếu dữ liệu, hệ thống từ chối bật và chỉ rõ phần còn thiếu (BR-52). Thao tác này tách biệt với việc lưu bản ghi và đổi `status`.
- **Luồng ngoại lệ & Bảo mật (Exception & Security Flows):**
  - *EF-41.1 (Cố gắng xóa vĩnh viễn nguyên liệu đã tham chiếu):* Nếu Administrator cố gắng thực hiện hành động xóa vĩnh viễn (hard delete) một nguyên liệu đã có ít nhất một bài công thức tham chiếu, hệ thống từ chối hành động, ngăn chặn xóa và hiển thị thông báo: *"Không thể xóa vĩnh viễn nguyên liệu đã được tham chiếu trong công thức. Vui lòng sử dụng tính năng Ngừng hỗ trợ"* (BR-53).
  - *EF-41.2 (Thiếu dữ liệu khi bật hỗ trợ dinh dưỡng):* Nếu thiếu một trong chín chỉ tiêu hoặc thiếu metadata nguồn cần thiết khi bật `nutrition_supported = 1`, hệ thống từ chối bật hỗ trợ và yêu cầu bổ sung dữ liệu (BR-52). Khi `nutrition_supported = 0`, việc thiếu chỉ tiêu được lưu dưới dạng `NULL` và không chặn lưu Ingredient.
  - *SF-41.1 (Chặn truy cập trái phép):* Người dùng không có quyền Administrator khi cố gắng gọi thao tác CRUD danh mục dinh dưỡng sẽ bị hệ thống từ chối với mã lỗi 403 Forbidden (NFR-09).
  - *SF-41.2 (Cấm tự động nhập hàng loạt và AI can thiệp):* Hệ thống không cung cấp API import tự động không qua kiểm duyệt và tuyệt đối không cấp quyền cho AI tự động sửa đổi hoặc chèn số liệu vào danh mục dinh dưỡng (BR-51, BR-54).

#### 5. Hậu điều kiện (Postconditions)
- Bản ghi nguyên liệu dinh dưỡng được thêm mới, cập nhật hoặc chuyển trạng thái an toàn trong danh mục nội bộ.
- Mọi thay đổi dữ liệu dinh dưỡng được lưu kèm vết kiểm toán (Audit Trail) gồm Administrator thực hiện và dấu thời gian.

---
