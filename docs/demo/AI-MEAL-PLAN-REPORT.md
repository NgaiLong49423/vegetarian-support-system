# Báo cáo nghiệp vụ và luồng hoạt động: AI tạo lịch ăn Mâm Xanh

- **Ngày đối chiếu mã nguồn:** 21/11/2026
- **Mục đích:** Tài liệu tham khảo để trình bày và trả lời câu hỏi về chức năng AI lập thực đơn tuần.
- **Loại tài liệu:** Báo cáo giải thích theo SRS và code hiện tại; không thay thế SRS/OpenAPI.
- **Phạm vi:** Luồng tạo preview trong trang Lịch ăn, luồng áp dụng vào lịch, và cách Backend gọi Gemini.

## 1. Tóm tắt để trình bày với giảng viên

Mâm Xanh không nhờ Gemini tự sáng tác món hoặc tự tính dinh dưỡng. Backend lấy tối đa 50 công thức đang công khai, loại những công thức vi phạm ràng buộc ăn chay/dị ứng hoặc thiếu dữ liệu dinh dưỡng tin cậy, rồi gửi Gemini danh sách công thức hợp lệ cùng các mục tiêu dinh dưỡng và sở thích trong request. Gemini chỉ xếp hạng ID công thức. Backend kiểm tra lại các ID và dữ liệu, sau đó bộ tối ưu của ứng dụng tự ghép món vào các ngày/bữa để hướng tới mục tiêu kcal và dưỡng chất. Người dùng xem trước, có thể bỏ chọn món, rồi xác nhận mới áp dụng vào Lịch ăn.

Trang Lịch ăn gọi Backend; Backend gọi Google Gemini qua Google ADK Java Runtime khi `GEMINI_API_KEY` có cấu hình. Nếu không có key, client AI được cấu hình thành trạng thái lỗi “Gemini is not configured” và request thất bại; không có nhánh giả lập thực đơn trong luồng này. Việc kiểm tra mã nguồn xác nhận tích hợp đã được viết như vậy, nhưng không tự xác nhận key có mặt hoặc một request Gemini đã chạy trong môi trường demo cụ thể.

## 2. Mục tiêu và quy tắc nghiệp vụ

Theo FR-36, chức năng tạo thực đơn tham khảo từ công thức công khai có sẵn cho các ngày **từ hôm nay đến hết tuần hiện tại**. Mỗi ngày có ba loại bữa: sáng, trưa và tối. Các bữa đã qua không được AI thay thế. Công thức chỉ được đưa vào tập ứng viên nếu có thể tính dinh dưỡng từ dữ liệu nguyên liệu đã xác thực.

Hệ thống dùng mục tiêu năng lượng từ hồ sơ dinh dưỡng để hướng tới phân bổ:

- Sáng: 25% năng lượng ngày.
- Trưa: 37,5% năng lượng ngày.
- Tối: 37,5% năng lượng ngày.
- Mỗi tỷ lệ có dung sai ±5 điểm phần trăm; kết quả là mục tiêu tối ưu, không phải bảo đảm trùng chính xác.

FR-36 yêu cầu AI chỉ chọn Recipe Post đang công khai; không được tự tạo món ngoài cơ sở dữ liệu. Preview chỉ mang tính tham khảo. Việc lưu thực đơn phải theo thao tác xác nhận của người dùng.

## 3. Dữ liệu đầu vào

### 3.1 Hồ sơ dinh dưỡng

Member phải đăng nhập, có hồ sơ nhân trắc học đã lưu và đồng ý sử dụng dữ liệu, đồng thời phải xác nhận đủ điều kiện dinh dưỡng. Hồ sơ cung cấp tuổi, giới tính sinh học, chiều cao, cân nặng, mức vận động và mục tiêu dinh dưỡng nếu có. Backend tính BMI, năng lượng ngày và các mục tiêu dinh dưỡng tham khảo trước khi tạo menu.

Mức vận động ảnh hưởng đến EER/kcal ngày được ước tính. Kcal và các dải mục tiêu dinh dưỡng này đi vào prompt/context và bộ tối ưu thực đơn. BMI được đưa vào phần cá nhân hóa lựa chọn, nhưng FR-36 không cho phép BMI tự đặt hoặc thay đổi mục tiêu kcal.

### 3.2 Sở thích ăn uống

Các ràng buộc đã khai báo về kiểu ăn chay và nguyên liệu cần tránh/dị ứng được áp dụng như điều kiện cứng. Món/nguyên liệu không thích là sở thích mềm và được giảm ưu tiên. Theo baseline FR-36, hoàn tất onboarding sở thích không phải điều kiện riêng để tạo menu; câu trả lời chưa khai báo vẫn là `null`, không được hiểu thành xác nhận “không có”. Ràng buộc nào đã khai báo thì vẫn được dùng.

**Cập nhật theo thay đổi đang chuẩn bị PR ngày 28/11/2026:** Backend so sánh kiểu ăn trong hồ sơ với kiểu do tác giả gắn cho công thức, không suy loại chay từ tên nguyên liệu. Nếu tác giả gắn sai loại, bước này không tự phát hiện sai lệch. Danh sách dị ứng/cần tránh và không thích vẫn được đối chiếu riêng theo nguyên liệu.

Trong chế độ `THIS_WEEK`, người dùng có thể gửi mục tiêu riêng cùng nguyên liệu muốn có và nguyên liệu cần tránh cho lần tạo hiện tại. Nguyên liệu muốn có chỉ là ưu tiên mềm; nguyên liệu cần tránh luôn thắng nếu cùng lúc xuất hiện ở cả hai danh sách. Các lựa chọn tuần này không được lưu thành hồ sơ lâu dài.

**Giới hạn đã xác minh:** ba trường trong hồ sơ FR-31 là khẩu vị ẩm thực, thời gian nấu tối đa và độ khó mong muốn hiện được lưu/đọc trong hồ sơ nhưng chưa được nối vào `AiMenuService` để lọc hoặc xếp hạng menu. Vì vậy không nên trình bày rằng menu hiện tại chắc chắn ưu tiên món Pháp, món dưới 30 phút hoặc món dễ chỉ vì người dùng lưu các giá trị đó trong hồ sơ. Finding đã được ghi tại `.agents/outputs/bugs/BUG-116.md`.

## 4. Luồng hoạt động từ giao diện đến lưu lịch

```mermaid
sequenceDiagram
    actor Member
    participant FE as Frontend: MealPlanWeekEditor
    participant API as Backend: AiMenuController
    participant Service as AiMenuService
    participant DB as SQL Server / catalog
    participant Gemini as Google Gemini qua ADK
    participant Opt as AiMenuOptimizer

    Member->>FE: Chọn chế độ và yêu cầu tạo thực đơn
    FE->>API: POST /api/v1/meal-plans/ai-menu/previews
    API->>Service: createPreview(request)
    Service->>DB: Đọc hồ sơ, eligibility, công thức và dữ liệu nguyên liệu
    Service->>Service: Lọc ràng buộc và tính dinh dưỡng từng khẩu phần
    Service->>Gemini: Gửi prompt + danh sách công thức hợp lệ
    Gemini-->>Service: JSON recipeIds được xếp hạng
    Service->>Service: Kiểm tra ID; đọc lại profile/công thức/ràng buộc
    Service->>Opt: Ghép recipe vào ngày/bữa theo kcal và nutrient targets
    Opt-->>Service: Các lựa chọn ngày, bữa, recipe ID
    Service-->>FE: Preview và số liệu dinh dưỡng dự kiến
    Member->>FE: Xem, bỏ chọn món nếu muốn, xác nhận áp dụng
    FE->>API: PUT /api/v1/meal-plans/ai-menu
    API->>Service: apply(request)
    Service->>DB: Kiểm tra lại rồi thay các mục từ hôm nay trở đi trong transaction
    Service-->>FE: Lịch tuần sau cập nhật
```

### Bước A — Frontend gửi yêu cầu preview

Trong `MealPlanWeekEditor.tsx`, Frontend gọi API `createAiMenuPreview()`:

- `weekStartDate`: ngày đầu tuần đang xem.
- `mode`: `PROFILE` hoặc `THIS_WEEK`.
- `goal`: chỉ gửi khi chế độ tuần này có mục tiêu riêng.
- `preferredIngredientIds`, `avoidedIngredientIds`: danh sách tùy chọn cho lần tạo tuần này.

Frontend đặt mặc định tất cả món trong preview ở trạng thái được chọn. Người dùng có thể bỏ chọn trước khi áp dụng. Bản preview được giữ tạm trong `sessionStorage` để tiếp tục xem trong phiên trình duyệt; đây chưa phải là dữ liệu đã ghi vào Meal Plan.

### Bước B — Backend xác thực hồ sơ và xác định ngày có thể đề xuất

`AiMenuController` nhận `POST /api/v1/meal-plans/ai-menu/previews` và chuyển request cho `AiMenuService.createPreview()`.

Service kiểm tra Member, hồ sơ dinh dưỡng và eligibility. Sau đó xác định ngày đầu tiên được phép đề xuất là ngày muộn hơn giữa ngày bắt đầu tuần và hôm nay; ngày cuối không vượt quá Chủ Nhật của tuần hiện tại. Nếu trong tuần không còn ngày có thể đề xuất, service trả preview rỗng thay vì tạo menu cho ngày đã qua.

Chế độ `PROFILE` dùng mục tiêu dinh dưỡng chung trong hồ sơ nếu đã có. Chế độ `THIS_WEEK` dùng mục tiêu được gửi trong request; lựa chọn này không được lưu lại vào hồ sơ.

### Bước C — Backend lập tập công thức đáng tin cậy

`AiMenuService.candidates()` lấy tối đa **50 công thức công khai** từ catalog, rồi áp dụng các bước:

1. Loại công thức có xung đột kiểu ăn chay hoặc nguyên liệu dị ứng/cần tránh.
2. Loại công thức có nguyên liệu chưa xác minh.
3. Giải đổi định lượng nguyên liệu sang gram: nguyên liệu dạng khối lượng dùng hệ số đơn vị; nguyên liệu theo chiếc/thể tích cần có quy đổi gram phù hợp.
4. Tính các chất dinh dưỡng của công thức bằng dữ liệu nguyên liệu trong catalog.
5. Chỉ giữ công thức có đủ dữ liệu dinh dưỡng cần thiết. Món có nguyên liệu người dùng không thích vẫn có thể nằm trong pool, nhưng bị xếp thấp hơn.

Việc tính dinh dưỡng dùng dữ liệu lưu trong catalog, không để Gemini tự tính hoặc thay thế dữ liệu nguồn. `RecipeNutritionService` cộng lượng từng chất theo gram nguyên liệu, rồi chia theo số khẩu phần để ra số liệu mỗi khẩu phần.

### Bước D — Gemini xếp hạng, không tự tạo món

`AiMenuService.prompt()` gửi đến model:

- Ngày cần đề xuất.
- Mục tiêu tuần nếu có.
- BMI, mục tiêu năng lượng kcal và dải tham chiếu dưỡng chất.
- Kiểu ăn chay, ID nguyên liệu muốn có, cần tránh và không thích.
- Từng ứng viên gồm ID, tên món, nguyên liệu và dinh dưỡng mỗi khẩu phần.

Prompt yêu cầu Gemini xếp hạng ứng viên và trả JSON dạng `{"recipeIds":[...]}`. Schema đầu ra cũng yêu cầu mảng ID. Backend kiểm tra phản hồi phải là JSON hợp lệ, ID phải nằm trong danh sách ứng viên, không được trùng và không được vượt giới hạn.

Gemini **không** trả công thức mới, không tạo nguyên liệu, không quyết định số liệu dinh dưỡng cuối và không ghi trực tiếp vào lịch. Nếu phản hồi có ID không được phép hoặc sai định dạng, Backend từ chối kết quả.

### Bước E — Backend tối ưu và tạo preview

Sau khi nhận thứ tự ưu tiên từ Gemini, Backend đọc lại hồ sơ, trạng thái công thức, catalog nguyên liệu và ràng buộc để kiểm tra các điều kiện an toàn còn đúng. Sau đó `AiMenuOptimizer` dùng dinh dưỡng tin cậy của các công thức để ghép menu:

- Bắt đầu bằng cách chọn một món cho mỗi loại bữa theo mức gần với tỷ lệ kcal mục tiêu.
- Thử thêm món thứ hai hoặc các món bổ sung vào bữa nếu cải thiện điểm tổng thể.
- Tính kcal thực tế từ recipe nutrition, so với tổng kcal ngày và mục tiêu cho từng bữa.
- Đếm số mục tiêu dưỡng chất đạt ngưỡng tham chiếu; giảm độ lệch khi thiếu/vượt ngưỡng và giảm điểm cho nguyên liệu không thích.
- Tránh lặp cùng món ở hai bữa liền kề khi còn ứng viên khác.

Điểm số ưu tiên trước hết sự phù hợp kcal/phân bổ bữa, sau đó số bữa nằm trong khoảng dung sai, số mục tiêu dinh dưỡng đạt, độ lệch dưỡng chất, mức không thích và thứ hạng Gemini. Vì đây là thuật toán tối ưu từ một pool hữu hạn, **không bảo đảm** menu đạt chính xác từng phần trăm hoặc đủ mọi chỉ tiêu mỗi ngày.

### Bước F — Người dùng duyệt và áp dụng

Backend trả preview gồm ngày, loại bữa, ID/tên món, dinh dưỡng mỗi khẩu phần, tổng mục tiêu và cảnh báo tham khảo. Giao diện cho người dùng xem và bỏ chọn món. Nút áp dụng yêu cầu thêm một bước xác nhận rõ ràng.

Frontend gửi `PUT /api/v1/meal-plans/ai-menu` với các bữa đã chọn. Backend lập lại danh sách công thức được phép và kiểm tra hồ sơ, điều kiện, ngày, loại bữa, công thức còn công khai, định lượng/dinh dưỡng đầy đủ và các ràng buộc. Nếu có mục không hợp lệ, thao tác bị từ chối.

Khi hợp lệ, thao tác ghi lịch nằm trong transaction: các mục hiện có từ hôm nay trở đi trong tuần được thay bằng các mục đã chọn; ngày đã qua được giữ nguyên. Mỗi mục được lưu với một khẩu phần. Vì vậy, preview chưa phải là lịch đã lưu; chỉ bước áp dụng mới ghi dữ liệu Meal Plan.

## 5. AI nào được gọi và điều kiện gọi thật

### Luồng trang Lịch ăn

```text
MealPlanWeekEditor
  -> POST /api/v1/meal-plans/ai-menu/previews
  -> AiMenuController.preview
  -> AiMenuService.createPreview
  -> AiClient (bean @Primary: AdkAiClient)
  -> AdkSingleStepTaskRunner, task RECOMMENDATIONS
  -> Google ADK Gemini model
  -> Google Gemini API khi GEMINI_API_KEY được cấu hình
```

`AdkAiClient` chọn model qua `AiChatSettingsService.resolve(...)`, sau đó gọi task `RECOMMENDATIONS` với system prompt, prompt nghiệp vụ và JSON response schema. Runner cấu hình một LLM call cho task này; kết quả usage được chuyển lại để Backend ghi nhận quota/usage. `AdkConfiguration` tạo Google ADK `Gemini` client bằng `GEMINI_API_KEY`.

Khi không có `GEMINI_API_KEY`, `AdkConfiguration` dùng `UnconfiguredLlm`; gọi model phát sinh lỗi `Gemini is not configured`, không trả một menu giả. Lỗi provider được chuyển thành lỗi AI provider không khả dụng. Không đọc/ghi hoặc đưa giá trị API key vào báo cáo.

### Luồng AI Chatbox

Chatbox có luồng ADK tools riêng: `get_ai_menu_candidates` lấy các ứng viên Backend đã lọc; model trong lượt ADK xếp hạng ID; `create_ai_menu_preview` chuyển các ID đó vào cùng quy trình preview/optimizer. Tool lấy ứng viên tự nó không gọi AI. Luồng Chatbox vẫn có thể gọi model để phân tích yêu cầu và xếp hạng, sau đó yêu cầu người dùng xác nhận trước khi ghi lịch. Báo cáo này tập trung chính vào nút tạo thực đơn trong trang Lịch ăn.

## 6. Quota, quyền và lỗi có thể gặp

- Endpoint yêu cầu Bearer authentication và Member đang hoạt động.
- Hồ sơ dinh dưỡng phải tồn tại, hợp lệ, có consent và trạng thái eligibility được xác nhận là đủ điều kiện.
- Backend kiểm tra quota trước khi gọi model; sau phản hồi usable, usage được ghi nhận.
- Có thể không tạo được preview nếu không còn ứng viên công khai đủ dữ liệu hoặc nếu AI provider không khả dụng.
- JSON sai, ID ngoài danh sách, công thức bị ẩn/xóa hoặc ràng buộc thay đổi trong khi tạo preview đều có thể khiến kết quả bị từ chối.

### Mismatch cần biết khi demo

Mô tả `@Operation` trong `AiMenuController` và snapshot `docs/api/openapi.yaml` hiện nói endpoint/apply yêu cầu Plus hoặc Pro. Nhưng FR-36 và BR-31 hiện hành ghi gói không phải điều kiện truy cập; trong luồng `AiMenuService.createPreview()`/`apply()`, code gọi kiểm tra hồ sơ và quota nhưng không gọi `requirePlusOrPro`. Mismatch này đã được ghi nhận ở `.agents/outputs/bugs/BUG-115.md`. Khi trình bày, nên nói chính xác: “nghiệp vụ hiện hành không khóa theo Plus/Pro; hệ thống vẫn kiểm tra quota/model”, và tránh dùng mô tả OpenAPI cũ làm bằng chứng rằng Plus/Pro là điều kiện.

## 7. Điểm cần nói rõ để không trình bày quá mức

1. AI chỉ chọn/xếp hạng từ công thức có sẵn; không sáng tác recipe.
2. Backend chịu trách nhiệm lọc an toàn, xác minh ID, tính dinh dưỡng và tối ưu ghép bữa.
3. 25% / 37,5% / 37,5% là tỷ lệ mục tiêu có dung sai; không cam kết chính xác.
4. Dinh dưỡng là ước tính từ dữ liệu catalog công thức; không phải tư vấn y khoa hoặc cam kết kết quả sức khỏe.
5. Sở thích “khẩu vị ẩm thực”, “thời gian nấu tối đa” và “độ khó” đang lưu trong hồ sơ nhưng chưa được dùng bởi luồng gợi ý thực đơn chuyên biệt; không nên nói AI hiện chắc chắn tối ưu theo ba trường này (BUG-116).
6. Source code chứng minh luồng tích hợp Gemini và nhánh không cấu hình; chỉ log/request thực tế trong môi trường demo mới chứng minh request cụ thể đã gọi provider thành công.

## 8. Kịch bản demo ngắn

1. Đăng nhập bằng tài khoản Member có hồ sơ dinh dưỡng và eligibility hợp lệ.
2. Mở Lịch ăn, chọn “Dựa trên hồ sơ” hoặc “Theo mong muốn tuần này”.
3. Nếu dùng chế độ tuần này, chọn mục tiêu và nguyên liệu muốn có/cần tránh; nói rõ đây là input tạm thời của lần tạo đó.
4. Nhấn **Tạo thực đơn tuần**. Giải thích Frontend gửi request đến Backend; Backend lọc ứng viên, gọi Gemini xếp hạng ID, rồi tự tối ưu kcal/dưỡng chất.
5. Cho xem các món, ngày/bữa và số liệu dinh dưỡng trong preview. Nêu rằng có thể bỏ chọn món và tỷ lệ kcal là mục tiêu có dung sai.
6. Nhấn **Áp dụng vào Lịch ăn**, xác nhận lần hai. Sau phản hồi thành công mới kết luận lịch đã được lưu; ngày đã qua được giữ nguyên.

## 9. Mẫu trả lời miệng khoảng một phút

“Chức năng AI tạo lịch ăn của Mâm Xanh hoạt động theo mô hình kết hợp AI và quy tắc Backend. Trước tiên Backend kiểm tra hồ sơ dinh dưỡng, eligibility, rồi lấy tối đa 50 công thức công khai có nguyên liệu và dữ liệu dinh dưỡng đầy đủ. Những món vi phạm kiểu ăn chay hoặc nguyên liệu cần tránh bị loại trước. Backend gửi danh sách hợp lệ, thông tin dinh dưỡng và mục tiêu của người dùng cho Google Gemini qua Google ADK. Gemini chỉ trả thứ tự ID công thức, không được tạo món mới. Backend kiểm tra ID, đọc lại ràng buộc rồi dùng thuật toán tối ưu để xếp các món vào sáng, trưa, tối theo mục tiêu năng lượng 25%, 37,5%, 37,5% với dung sai. Người dùng xem preview và xác nhận thì Backend mới lưu. Nếu Gemini chưa được cấu hình API key, request báo provider chưa khả dụng chứ không tạo menu giả.”

## 10. Bảng chứng cứ: hàm nào gọi, code nằm ở đâu

Bảng dưới đây là phần truy vết cụ thể để trả lời câu hỏi “bằng chứng code ở đâu?”. Mỗi liên kết mở đúng file và dòng khai báo/call quan trọng. Các con số dòng phản ánh checkout đã đối chiếu ngày 21/11/2026; nếu code đổi, mở link và tìm lại tên hàm.

| Bước nghiệp vụ | Hàm và vị trí code | Bằng chứng trong code |
|---|---|---|
| Yêu cầu tạo preview từ màn hình Lịch ăn | [`generate()` — MealPlanWeekEditor.tsx, dòng 1613](../../app/mamxanh-frontend/src/pages/MealPlanWeekEditor.tsx#L1613); [`createAiMenuPreview()` — recipes.ts, dòng 650](../../app/mamxanh-frontend/src/api/recipes.ts#L650) | `generate()` gửi `weekStartDate`, `mode`, goal và ingredient IDs bằng POST đến `/meal-plans/ai-menu/previews`. Khi thành công, FE đặt toàn bộ món preview vào trạng thái được chọn ban đầu. |
| API nhận yêu cầu | [`preview()` — AiMenuController.java, dòng 36](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/controller/AiMenuController.java#L36) | Controller nhận request đã validate và chuyển cho `service.createPreview(request)`. Đây là entry point Backend của nút tạo preview. |
| Kiểm tra hồ sơ và dựng ngữ cảnh | [`createPreview()` — AiMenuService.java, dòng 84](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L84); [`resolveMenuContext()` — cùng file, dòng 190](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L190); [`requireMenuNutritionContext()` — NutritionProfileService.java, dòng 179](../../app/mamxanh-backend/src/main/java/tech/mamxanh/nutrition/service/NutritionProfileService.java#L179) | Backend yêu cầu Member đang hoạt động, hồ sơ dinh dưỡng và eligibility; chọn ngày có thể đề xuất từ hôm nay đến hết tuần. `PROFILE` lấy mục tiêu hồ sơ; `THIS_WEEK` lấy goal/ingredient preferences của request hiện tại. |
| Lấy và lọc công thức ứng viên | [`candidates()` — AiMenuService.java, dòng 259](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L259); [`DietaryConstraintEvaluator.evaluate()` — dòng 17](../../app/mamxanh-backend/src/main/java/tech/mamxanh/nutrition/service/DietaryConstraintEvaluator.java#L17) | Backend lấy nhiều nhất `CANDIDATE_LIMIT = 50`; loại món có xung đột dị ứng/avoid, kiểu chay hoặc nguyên liệu chưa xác minh. Nguyên liệu không thích bị tính điểm mềm thay vì hard exclusion. |
| Kiểm tra dữ liệu dinh dưỡng và tính mỗi khẩu phần | [`RecipeNutritionService.calculate()` — dòng 30](../../app/mamxanh-backend/src/main/java/tech/mamxanh/nutrition/service/RecipeNutritionService.java#L30); [`grams()` — dòng 91](../../app/mamxanh-backend/src/main/java/tech/mamxanh/nutrition/service/RecipeNutritionService.java#L91); [`recipeNutrition.calculate()` — AiMenuService.java, dòng 286](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L286) | Lượng nguyên liệu được đổi sang gram, nhân với dinh dưỡng trong catalog, cộng theo công thức rồi chia số khẩu phần. Công thức thiếu quy đổi hoặc số liệu bị loại khỏi pool. |
| Soạn prompt | [`prompt()` — AiMenuService.java, dòng 364](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L364) | Prompt bắt đầu bằng “Rank the listed trusted vegetarian recipes”; kèm candidate IDs, tên, nguyên liệu, nutrition per serving, BMI, mục tiêu kcal, targets và các constraint. Prompt yêu cầu model trả ID, không tính/đặt lại nutrition. |
| Gọi AI từ nghiệp vụ | [`createPreview()` — AiMenuService.java, dòng 98](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L98); [`recommendRecipes()` — AdkAiClient.java, dòng 104](../../app/mamxanh-backend/src/main/java/tech/mamxanh/integration/ai/AdkAiClient.java#L104); [`recommendRecipeSequence()` — dòng 109](../../app/mamxanh-backend/src/main/java/tech/mamxanh/integration/ai/AdkAiClient.java#L109) | Backend kiểm tra quota rồi gọi `ai.recommendRecipes(...)`; bean `AdkAiClient` chuyển thành task `RECOMMENDATIONS` và nhận kết quả cùng token usage. |
| Chạy task ADK một lần | [`executeTaskWithResult()` — AdkSingleStepTaskRunner.java, dòng 61](../../app/mamxanh-backend/src/main/java/tech/mamxanh/integration/ai/adk/AdkSingleStepTaskRunner.java#L61); cấu hình một LLM call ở [dòng 130](../../app/mamxanh-backend/src/main/java/tech/mamxanh/integration/ai/adk/AdkSingleStepTaskRunner.java#L130) | Runner dựng agent, gửi prompt với JSON response schema và giới hạn task này ở một lần gọi model. |
| Khởi tạo Gemini thật hoặc báo chưa cấu hình | [`createLlm()` — AdkConfiguration.java, dòng 64](../../app/mamxanh-backend/src/main/java/tech/mamxanh/integration/ai/adk/AdkConfiguration.java#L64); tạo Gemini ở [dòng 75](../../app/mamxanh-backend/src/main/java/tech/mamxanh/integration/ai/adk/AdkConfiguration.java#L75); nhánh thiếu key lỗi ở [dòng 87](../../app/mamxanh-backend/src/main/java/tech/mamxanh/integration/ai/adk/AdkConfiguration.java#L87) | Có key thì tạo Google ADK `Gemini`; không có thì `UnconfiguredLlm` ném lỗi `Gemini is not configured`, không sinh menu giả. Model được resolve tại [`AiChatSettingsService.resolve()`](../../app/mamxanh-backend/src/main/java/tech/mamxanh/subscription/service/AiChatSettingsService.java#L50). |
| Kiểm tra kết quả Gemini | [`parseRecipeIds()` — AiMenuService.java, dòng 340](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L340) | JSON phải có mảng `recipeIds`; ID ngoài pool, ID trùng, kết quả rỗng hoặc sai định dạng bị từ chối. |
| Đọc lại ràng buộc và chạy optimizer | Đọc lại context tại [`AiMenuService.java`, dòng 111](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L111); gọi [`optimize()` — AiMenuOptimizer.java, dòng 18](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuOptimizer.java#L18) | Sau response, Backend nạp lại profile/công thức/constraints rồi chỉ dùng candidate còn an toàn. Optimizer đặt tỷ lệ tại [`MEAL_SHARES`, dòng 15](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuOptimizer.java#L15), chấm tổng kcal/dinh dưỡng và dung sai ở [dòng 86](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuOptimizer.java#L86) và [dòng 125](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuOptimizer.java#L125). |
| Trả preview về giao diện | [`preview()` — AiMenuService.java, dòng 293](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L293) | Backend tạo danh sách meal, nutrition per serving, daily targets, BMI và disclaimer; chưa ghi Meal Plan ở bước này. |
| Người dùng chọn và xác nhận | [`apply()` trên Frontend — MealPlanWeekEditor.tsx, dòng 1630](../../app/mamxanh-frontend/src/pages/MealPlanWeekEditor.tsx#L1630); checkbox chọn món ở [dòng 1787](../../app/mamxanh-frontend/src/pages/MealPlanWeekEditor.tsx#L1787); nút xác nhận ở [dòng 1805](../../app/mamxanh-frontend/src/pages/MealPlanWeekEditor.tsx#L1805); API `applyAiMenu()` — recipes.ts, dòng 654 | FE yêu cầu bấm xác nhận lần hai rồi gửi đúng các meal được chọn bằng PUT `/meal-plans/ai-menu`. |
| Backend xác nhận và ghi lịch | [`apply()` — AiMenuService.java, dòng 220](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L220); validate lựa chọn ở [dòng 314](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L314); xóa mục từ ngày hiện tại ở [dòng 243](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L243) | Service kiểm tra lại hồ sơ, ngày, meal type, trạng thái công khai, nutrition và constraint. Trong transaction, mục hiện tại/tương lai được thay bằng các món đã chọn; ngày quá khứ giữ nguyên. |
| Đối chiếu nghiệp vụ chuẩn | [FR-36 — FUNCTIONAL-REQUIREMENTS.md](../requirements/srs/FUNCTIONAL-REQUIREMENTS.md#fr-36), nhất là phần mục tiêu/đầu vào ở dòng 2484 và AC phân bổ kcal ở dòng 2561; [BR-31 — BUSINESS-RULES.md](../requirements/srs/BUSINESS-RULES.md#br-31) | SRS là bằng chứng yêu cầu; code ở các hàng trên là bằng chứng triển khai. Dùng cả hai để tách “app phải làm gì” khỏi “code hiện đang làm gì”. |
| Mismatch Plus/Pro đã biết | Mô tả operation ở [`AiMenuController.java`, dòng 26](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/controller/AiMenuController.java#L26); service preview bắt đầu ở [AiMenuService.java, dòng 84](../../app/mamxanh-backend/src/main/java/tech/mamxanh/mealplan/service/AiMenuService.java#L84); SRS quyền FR-36 ở [dòng 2501](../requirements/srs/FUNCTIONAL-REQUIREMENTS.md#L2501) | Annotation/OpenAPI nói Plus/Pro; SRS nói không yêu cầu Plus/Pro và service thực tế kiểm tra profile/quota mà không gọi `requirePlusOrPro`. Finding: `BUG-115`. |

### Cách sử dụng bảng khi thầy hỏi

- **“Frontend gọi ở đâu?”** Mở hàng 1: `MealPlanWeekEditor.generate()` gọi API wrapper `createAiMenuPreview()`.
- **“Hàm nào gọi Gemini?”** Mở hàng 7: `AiMenuService.createPreview()` gọi `AiClient`; `AdkAiClient.recommendRecipes()` chuyển sang task ADK `RECOMMENDATIONS`; hàng 9 cho biết cấu hình Gemini.
- **“AI có tự tính và xếp món vào bữa không?”** Mở hàng 6–11: Gemini trả thứ tự ID; Backend xác minh rồi `AiMenuOptimizer.optimize()` mới ghép ngày/bữa.
- **“Dữ liệu dinh dưỡng lấy từ đâu?”** Mở hàng 5: `RecipeNutritionService.calculate()` dùng số liệu nguyên liệu trong catalog và định lượng công thức.
- **“Khi nào lịch được lưu?”** Mở hàng 13–14: preview chỉ hiển thị; PUT apply sau xác nhận mới ghi dữ liệu.

**Lưu ý:** `L<number>` trong link là số dòng GitHub-style. Nếu mở file trong editor, có thể dùng tên hàm ở cột giữa để tìm trực tiếp. Dòng có thể dịch chuyển khi code được chỉnh sửa; nội dung hàm là định danh ổn định hơn số dòng.

## 11. Trạng thái kiểm chứng của báo cáo

Nội dung được đối chiếu tĩnh từ FR-36/BR-31, OpenAPI snapshot, component Frontend và các lớp Controller/Service/Optimizer/ADK nêu trên. Không chạy Backend, không gọi Gemini, không kiểm tra API key hoặc dữ liệu tài khoản thật và không chạy test trong quá trình viết báo cáo. Do đó tài liệu mô tả chính xác **đường đi trong source hiện tại**; xác nhận request Gemini thành công trong một môi trường cụ thể cần quan sát lần chạy thực tế ở môi trường đó.
