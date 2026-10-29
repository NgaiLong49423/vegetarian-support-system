> **Document:** API Integration Guide
> **File:** `docs/api/API.md`
> **Version:** v0.51.0
> **Created:** 2026-09-20
> **Last Updated:** 2026-10-29
> **Status:** Active

# API Integration Guide

## Giám sát giao dịch gói tài khoản

Phạm vi triển khai theo yêu cầu trực tiếp ngày 29/10/2026, chưa có Issue riêng; sử dụng dữ liệu FR-13 và giữ nguyên quy tắc kích hoạt gói qua payOS đã xác minh. Không bổ sung quyền sửa giao dịch hoặc xác nhận thanh toán thủ công.

- `GET /api/v1/admin/transactions`: chỉ Admin; trả `{ transactions: PageResponse<AdminTransactionResponse>, summary }`. Guest 401; Customer/Expert 403; tài khoản khóa 403 `ACCOUNT_LOCKED`. Các response thành công đặt `Cache-Control: no-store`.
- Query tùy chọn: `query` tối đa 200 ký tự, tìm chuỗi literal theo mã đơn payOS, `TX-<id>`, tên hoặc email; `tier=PLUS|PRO`; `status=PENDING|PAID|FAILED|CANCELLED`; `from`, `to` dạng `YYYY-MM-DD`, lọc ngày **tạo đơn**, bao gồm hai đầu theo `Asia/Ho_Chi_Minh`. Không nhận ngày tương lai, trước năm 1900 hoặc khoảng đảo ngược. Bỏ ngày để không giới hạn khoảng tương ứng; retention 90 ngày của AI telemetry không áp dụng cho lịch sử thanh toán.
- `page` từ 0, mặc định 0; `size` 1–100, mặc định 20. Sắp xếp mới nhất theo `created_at`, sau đó ID giảm dần. Thống kê `total`, `paid`, `pending`, `failed`, `cancelled`, `revenueVnd` sử dụng cùng bộ lọc trên mọi trang; doanh thu chỉ cộng PAID, tổng dùng BIGINT. Không coi PENDING là xác nhận link QR còn mở hoặc cổng thanh toán đang khỏe.
- Các timestamp trả UTC có `Z`. `tier` lấy chu kỳ đăng ký đã liên kết hoặc suy ra từ hai mức giá Phase 1 khi không có chu kỳ; số tiền lịch sử khác không bị gán nhầm Plus. Các trường chu kỳ/paidAt/tier có thể null; chu kỳ là lần mua tương ứng, không xác nhận quyền hiện tại.
- `GET /api/v1/admin/transactions/{id}`: cùng phân quyền, 404 `PAYMENT_TRANSACTION_NOT_FOUND` nếu không có đơn. Chỉ đọc local SQL, không gọi payOS hoặc tự cấp quyền.
- `GET /api/v1/admin/transactions/export`: cùng bộ lọc, xuất CSV UTF-8 có BOM, quote/escape ô và trung hòa chuỗi có thể thành công thức bảng tính. Không phân trang; hơn 10.000 dòng trả 400 `VALIDATION_FAILED` để người dùng thu hẹp bộ lọc, không cắt file âm thầm. Số tiền là VND, thời gian trong CSV là UTC. Khi mở trong phần mềm bảng tính, nhập cột mã đơn dưới dạng Text để giữ nguyên mã số dài. File chứa dữ liệu cá nhân chỉ Admin được truy cập; không phải hóa đơn hoặc kết quả đối soát từ provider.
- Checkout kiểm tra cấu hình merchant trước khi tạo đơn; thiếu cấu hình trả 503 `PAYMENT_UNAVAILABLE` và không tạo PENDING giả. Sau đó commit đơn trước provider bằng transaction riêng để callback sớm nhìn thấy đơn và lỗi provider không rollback dấu vết. Timeout mơ hồ giữ PENDING; nhánh thất bại rõ ràng chỉ chuyển PENDING sang FAILED, không ghi đè PAID. Signature HMAC-SHA256, amount, currency, idempotency và ownership vẫn thuộc luồng thanh toán hiện có. Đối chiếu protocol với [payOS API](https://payos.vn/docs/api/) và [hướng dẫn kiểm tra chữ ký](https://payos.vn/docs/tich-hop-webhook/kiem-tra-du-lieu-voi-signature/).

## Telemetry sử dụng AI — FR-11 / Issue #17

Chatbot request/response/history-turn che nội dung trong `toString()` để log DEBUG của Spring không ghi câu hỏi/câu trả lời. JSON contract không bị che hoặc đổi nội dung bởi cơ chế này; kiểm chứng BUG-076 dùng dữ liệu tổng hợp, không phải dữ liệu cá nhân thật.

- `GET /api/v1/admin/ai-telemetry` chỉ dành cho `ADMIN`, dùng Bearer JWT hiện có; Guest trả 401, Member trả 403. Tài khoản bị khóa bị chặn theo kiểm tra trạng thái hiện hành. Response là DTO tổng hợp trực tiếp, không có envelope và không trả định danh tài khoản/phiên.
- Query bắt buộc `from`, `to` theo `YYYY-MM-DD`, bao gồm cả hai ngày; tùy chọn `granularity=DAY|WEEK|MONTH` (mặc định DAY), `feature=FR-21|FR-34|FR-36|FR-37|FR-47|FR-51`. Không nhận ngày tương lai, khoảng đảo ngược hoặc ngày bắt đầu trước ngày hiện tại trừ 90 ngày. Validation sai trả 400 theo ProblemDetail hiện hành.
- Ngày báo cáo theo `Asia/Ho_Chi_Minh`, tuần bắt đầu Thứ Hai; timestamp lưu UTC. `retainedFrom` cho biết mốc giữ dữ liệu chính xác, nên ngày đầu khoảng retention có thể chỉ có một phần dữ liệu. SQL loại dữ liệu quá hạn ngay cả trước khi job xóa chạy.
- `totals` và từng `buckets[].totals` chứa lượt thành công, prompt/response/total/thinking/cached tokens, số lượt metadata thiếu/bất thường và số lượt chưa tính được chi phí. Total giữ số provider trả về; không cộng lại thinking hoặc cached vào total. Thiếu toàn bộ usageMetadata ghi ba bộ đếm chính bằng 0 và trạng thái MISSING; thiếu một phần là PARTIAL; số liệu bất thường là INVALID, các bộ đếm không dùng được được biểu diễn bằng 0 kèm cờ kiểm toán. Không coi các lượt này là dữ liệu token đầy đủ.
- Chi phí là USD quy đổi theo giá Gemini Standard trả phí, không phải hóa đơn hoặc phí gói Free/Plus/Pro. Lưu phiên bản giá và số tiền tại thời điểm ghi. Khi có lượt chưa tính được, `estimatedCostUsd` là null; `knownEstimatedCostUsd` chỉ là phần đã tính, `unpricedCalls` cho biết phần còn thiếu. Không hiển thị chi phí chưa biết thành 0.
- Bảng giá text `gemini-3.8-flash` kiểm tra lại ngày 28/10/2026 (giữ phiên bản giá hiệu lực 23/10/2026) từ [Google](https://ai.google.dev/gemini-api/docs/pricing#gemini-3.8-flash): input/output/cached lần lượt 0.75/3.75/0.075 USD trên một triệu token đến hết 2026; 1.50/7.50/0.15 từ 01/01/2027. Công thức dùng `(prompt-cached)*input + cached*cachedRate + (response+thinking)*output`, chia một triệu. Chỉ tính khi metadata đầy đủ và phù hợp phạm vi Standard text, không có tool-use prompt tokens. Không áp giá hồi tố trước 23/10/2026 theo múi giờ Việt Nam (22/10/2026 17:00 UTC); chi phí trước mốc đó chưa xác định.
- Sau phản hồi hợp lệ, listener force file metadata xuống disk rồi đưa vào executor riêng có 2 worker/queue 500. Queue đầy và DB lỗi không xóa file; replay khi khởi động và mỗi 5 giây. Writer ghi UUID idempotent bằng transaction riêng; chỉ xóa file sau khi SQL commit. Caller không chờ DB, tránh thiếu connection từ transaction đọc lồng nhau; buffer RAM không còn là bản sao duy nhất.
- Không thể ghi disk bền trả 503 `AI_TELEMETRY_UNAVAILABLE`; không trả 200 khi metadata chưa được lưu bền. Mất/hỏng chính volume không thể khôi phục bằng cơ chế này. Storage pending phải writable, riêng tư và được giữ khi restart; root Compose có named volume `backend-telemetry-data`. Không đổi API chat/recipe payload hoặc phân quyền FR-10.
- FR-21 và hướng dẫn nấu FR-34 dùng kết quả có token metadata thực từ provider. Provider lỗi/timeout/đầu ra không hợp lệ không phát sự kiện thành công. Thiếu usageMetadata vẫn ghi số 0 kèm cờ kiểm toán theo AF-11.1, không làm gián đoạn trải nghiệm khi storage hoạt động.
- Bảng `AI_USAGE_TELEMETRY` không chứa prompt, câu trả lời hoặc lịch sử hội thoại. Member dùng ID xác thực; Guest dùng anonymous UUID hiện có, không lưu IP. File pending quá hạn được xóa trong replay; job SQL xóa theo lô 1.000 bản ghi khi khởi động và mỗi phút, với điều kiện `occurred_at < nowUTC - 90 ngày`. Không giữ bảng tổng hợp lịch sử vượt retention.
- V26 là thay đổi cấu trúc thử nghiệm trên nhánh riêng theo chỉ dẫn người dùng; quyết định nhóm vẫn chờ biểu quyết, chưa phải schema đã được nhóm phê duyệt. Không chỉnh ERD. Contract chi tiết lấy từ generated OpenAPI/Scalar; xuất lại snapshot bằng script khi Backend chạy.


## Checklist mua sắm — FR-53

Các operation đã triển khai dưới `/api/v1/shopping-lists`:

| Method / đường dẫn tương đối | Hành vi |
|---|---|
| `POST /` | `/api/v1/shopping-lists` nhận `{name}` để tạo danh sách rỗng độc lập với Lịch ăn và Recipe Post; Backend tự xác định Member sở hữu, trim tên, giới hạn 200 ký tự, gán loại `MANUAL`; trả `201`, Location và checklist rỗng. |
| `POST /from-meal-plan` | Tạo danh sách mới từ Meal Plan của Member; `{weekStartDate, selectedDates?}`; trả `201`, Location và `ShoppingListCreationResponse`. |
| `POST /from-recipe` | `{name, recipeId, servings?, selectedIngredientIds?, manualItems?}` tạo danh sách `MANUAL` và lưu các nguyên liệu được chọn/mặt hàng thủ công trong cùng transaction; bỏ `selectedIngredientIds` để chọn tất cả, mảng rỗng chỉ hợp lệ khi có `manualItems`; trả `201 ShoppingListCreationResponse` và Location. |
| `POST /{listId}/recipes` | `{recipeId, servings?, selectedIngredientIds?, manualItems?}` thêm các nguyên liệu được chọn và mặt hàng thủ công vào danh sách thuộc Member; bỏ `selectedIngredientIds` để chọn tất cả, mảng rỗng chỉ hợp lệ khi có `manualItems`; trả `200 ShoppingListCreationResponse`. |
| `GET` | Danh sách riêng của Member; `page >= 0`, `size` 1–50, mặc định 20; `PageResponse` dùng `content`; sắp xếp `updatedAt DESC, id DESC`. |
| `GET /options` | Các đơn vị catalog đang hoạt động để thêm/sửa mục. |
| `GET /{listId}` | Chi tiết danh sách và các mục theo ID tăng dần. |
| `PATCH /{listId}` | Đổi tên danh sách `{name}`; trim khoảng trắng, tên không rỗng và tối đa 200 ký tự; trả `200 ShoppingListSummary`. |
| `DELETE /{listId}` | Xóa danh sách cùng checklist và provenance công thức; trả `204`. Lịch ăn và Recipe Post nguồn được giữ nguyên. |
| `POST /{listId}/items` | Thêm mục thủ công `{name, quantity, unitId}`; trả `201`, Location và mục đã lưu; mặc định `bought=false`. |
| `PUT /{listId}/items/{itemId}` | Đặt số lượng/đơn vị; giữ định danh nguyên liệu và trạng thái đã mua. |
| `PATCH /{listId}/items/{itemId}/bought` | Nhận `{bought: true/false}`; gửi lại cùng giá trị không đổi `updatedAt`. |
| `DELETE /{listId}/items/{itemId}` | Xóa đúng một mục, trả `204`. |
| `DELETE /{listId}/items?bought=true` | Chỉ dọn mục đã mua của danh sách đó, trả `204`; thiếu hoặc sai điều kiện không xóa hàng loạt. |

Mọi operation cần Bearer token của Member hoạt động (Customer/Expert); tài khoản lấy từ principal, không nhận `userId` từ request. Service kiểm tra chủ sở hữu trước khi đọc/thay đổi danh sách hoặc mục. Danh sách không tồn tại/khác chủ sở hữu hoặc mục không thuộc danh sách đều trả `404 NOT_FOUND`. Guest/token lỗi trả `401`; Admin và tài khoản không hoạt động bị từ chối theo auth baseline (`403`).

Tên thủ công là văn bản thuần, không rỗng sau chuẩn hóa khoảng trắng, tối đa 200 ký tự; không chấp nhận markup hoặc ký tự điều khiển. `quantity > 0`, tối đa 8 chữ số nguyên và 2 chữ số thập phân theo schema hiện tại. Đơn vị mới phải đang hoạt động. Đơn vị cũ đã ngừng sử dụng vẫn hiển thị và có thể giữ nguyên khi sửa số lượng. Không tự đổi lượng khi Member sửa đơn vị: request biểu diễn cặp số lượng/đơn vị mới do Member xác nhận. Validation trả `400 VALIDATION_FAILED`; response lỗi theo mục 4.

Checklist response giữ `ingredientId` (nullable cho mục thủ công), tên, nhóm, `imageUrl` của nguyên liệu trong catalog (nullable khi chưa có ảnh), số lượng, đơn vị và `bought`; tham chiếu catalog hiện có được phân giải kể cả đã ngừng sử dụng. Frontend hiển thị ảnh ở từng dòng; mục thủ công hoặc nguyên liệu chưa có ảnh trả `imageUrl: null` và dùng trạng thái thiếu ảnh. Các mutation khóa danh sách của chủ sở hữu trong transaction để tránh ghi đè giữa thao tác checklist và cleanup. `PATCH /{listId}` chỉ thay tên và cập nhật thời gian sửa; `DELETE /{listId}` xóa provenance trước các item tham chiếu, sau đó xóa danh sách trong cùng transaction. Đổi tên không làm tên danh sách từ Lịch ăn tự đổi lại; xóa không tác động tới Meal Plan hay Recipe Post nguồn.

Khi tạo từ Meal Plan, `weekStartDate` phải là thứ Hai; `selectedDates` bỏ qua hoặc `null` nghĩa là toàn tuần, còn mảng có từ 1–7 ngày không trùng trong tuần. Backend luôn đọc tuần của principal, không nhận owner hoặc entry ID từ client. Mỗi bữa đóng góp theo `plannedServings / servings`; cùng công thức ở nhiều bữa được tính nhiều lần, khác quy tắc chống thêm trùng Recipe Post cá nhân của #90. Nguồn không còn công khai không được thêm mới.

`ShoppingListCreationResponse` gồm `shoppingList`, `warnings` (`code`, `message`, `mealPlanEntryId`, `recipeId`, hai ID có thể `null`) và `incompatibleItemIds` của các dòng đã lưu riêng đơn vị. Lượng sau gộp mới làm tròn `HALF_UP` tới hai chữ số thập phân; thay đổi có `QUANTITY_ROUNDED`. Lượng quá nhỏ thành 0, vượt 99999999,99, tuần không có món, không còn nguyên liệu hợp lệ hoặc chọn ngày sai đều trả `400 VALIDATION_FAILED`; không tạo danh sách rỗng. Item mới `bought=false`; danh sách/item lưu nguyên tử. Đọc lại hoặc tick không lấy lại lượng từ Recipe Post, giữ số lượng và chỉnh sửa đã lưu. Cảnh báo tạo không phải lịch sử provenance của #90.

Frontend `/ke-hoach` cho Member chọn cả tuần hoặc ngày rồi gọi operation tạo; `/di-cho?listId=...` đọc checklist và thực hiện CRUD/trạng thái qua API. Không tổng hợp nguyên liệu lần nữa ở Frontend. Cảnh báo response tạo được hiển thị trong context điều hướng; không có endpoint lịch sử cảnh báo. Mọi lần đọc lại giữ các dòng đơn vị đã lưu riêng. Các operation đọc/sửa/đánh dấu có response `200` kèm schema trong generated OpenAPI.

### Nguồn Recipe Post được chọn — AC-53.6

`POST /{listId}/recipes` và `POST /from-recipe` nhận `selectedIngredientIds` để chỉ thêm nguyên liệu được chọn; bỏ trường này để giữ tương thích và thêm toàn bộ nguyên liệu. Có thể bỏ chọn tất cả nếu gửi ít nhất một `manualItems` dạng `{name, quantity, unitId}`; mặt hàng thủ công và phần công thức được lưu trong cùng transaction. Bỏ `servings` để dùng lượng gốc; nếu có (1–50), Backend tính `recipe quantity × servings / recipe.servings` cho các dòng đã chọn trước khi tổng hợp và lưu. Gửi lại công thức trong cùng danh sách không tạo đóng góp công thức trùng; mặt hàng thủ công vẫn được thêm như các thao tác POST thông thường. Nguồn không công khai/không tồn tại và danh sách không thuộc Member trả `404`; dữ liệu hoặc định lượng sai trả `400`. Thao tác khóa danh sách để tránh ghi đè checklist.

Mỗi item trả thêm `sources`: `recipeId`, `title` hiện hành (nullable), `available`, `originalQuantity`, `originalUnitId`, `originalUnitCode`. Đây là lượng/đơn vị được đóng góp tại thời điểm thêm (sau khi scale theo `servings` nếu có), không bị thay khi Member chỉnh lượng/đơn vị của item hoặc công thức được sửa. Công thức HIDDEN/DELETED giữ tham chiếu và checklist, nhưng không trả tiêu đề/nội dung riêng tư. UI chỉ mở link nguồn khi `available=true`.

Khi thêm công thức mới, FR-54 tổng hợp lượng mới với **tổng hiện đang lưu do Member chỉnh**, không tái tính từ các công thức cũ. Dòng nhận nguyên liệu mới chuyển về chưa mua; dòng không bị ảnh hưởng giữ lượng và trạng thái. Xóa/dọn item giữ dấu công thức đã thêm và lượng đóng góp, bỏ liên kết tới item bị xóa; thêm lại công thức đó không phục hồi item đã chủ động xóa. Đọc/tick hoặc sửa nguồn không tự tổng hợp lại.

Khám phá và Công thức đã lưu mở thao tác thêm từ menu dấu ba chấm trên từng công thức; Member chọn một danh sách đã có hoặc tạo mới, điều chỉnh khẩu phần, rồi được đưa tới `/di-cho?listId=...`. Thiết kế nguồn tương thích quyết định trong #90; triển khai local trong #53 không tự thay trạng thái/dependency hay xác nhận nghiệm thu Issue #90.

Schema/status chính xác lấy từ generated OpenAPI; không chỉnh endpoint trong YAML thủ công.
### Thời gian đăng ký gói và giao dịch — FR-13

- `createdAt`, `paidAt`, `startsAt`, `endsAt` trong response subscription/payment là ISO-8601 UTC có hậu tố `Z`; Frontend hiển thị theo `Asia/Ho_Chi_Minh` (UTC+7), không phụ thuộc múi giờ trình duyệt.
- Lịch sử giao dịch có `endsAt` nullable, lấy từ Subscription được liên kết với chính giao dịch đã thanh toán. Giao dịch chưa trả tiền hoặc thiếu liên kết trả `null`; không suy ra từ gói hiện tại hay tự cộng thời hạn ở Frontend.
- `paidAt` là thời điểm Backend xác nhận/kích hoạt giao dịch. Không thay đổi cách xác minh chữ ký, số tiền hoặc thời hạn chu kỳ.
- Dữ liệu cũ từng được tạo bằng giờ máy Windows cần đối chiếu trước khi sửa; không cộng/trừ 7 giờ hàng loạt cho dữ liệu UTC của Docker. Bản sửa không tự ghi lại các giao dịch lịch sử.

## 1. Mục đích và phạm vi

Tài liệu này hướng dẫn Frontend, Backend và tester tích hợp với API Mâm Xanh. Generated OpenAPI từ Spring Boot là runtime contract. File [`openapi.yaml`](openapi.yaml) là bản xuất được commit từ Springdoc để tiện tra cứu và phải khớp với runtime; không sửa contract endpoint thủ công trong file này.

Backend source trong branch gồm đăng ký, xác minh email, đăng nhập bằng mật khẩu và Google, đặt lại mật khẩu (FR-03), tìm kiếm/lọc RecipePost công khai (FR-08), danh mục quản trị nguyên liệu/đơn vị/quy đổi (FR-18), xác nhận eligibility dinh dưỡng (FR-38) và hồ sơ dinh dưỡng tham khảo (FR-35), đọc chi tiết RecipePost và Meal Plan (FR-20), AI gợi ý và menu tuần công thức (FR-34), AI preview/thay tuần Meal Plan theo dinh dưỡng (FR-36), chatbot Member theo ngữ cảnh chung hoặc RecipePost công khai (FR-51), danh sách RecipePost đã lưu (FR-20/FR-32), nộp báo cáo Recipe Post (FR-26/27), bình chọn (FR-57), lượt xem và thống kê (FR-58), theo dõi thành viên (FR-59), và bình luận lồng nhiều cấp (FR-46). Runtime spec là nguồn contract; YAML trong repo được sinh từ cùng runtime endpoint.

Nhóm đã chấp nhận baseline API hiện có để phân rã và chuẩn bị triển khai FR-03. Các thông số còn mở ở mục 8 phải được owner đề xuất và Tech Lead duyệt trước khi triển khai phần phụ thuộc vào chúng. Trạng thái tài liệu `Active` không phải bằng chứng endpoint đã được triển khai hoặc chạy thành công.

## 2. Contract và công cụ

- Runtime OpenAPI được sinh từ Controller/DTO/annotations bằng springdoc: `/v3/api-docs` (JSON) và `/v3/api-docs.yaml` (YAML).
- Bản OpenAPI YAML được commit tại [`openapi.yaml`](openapi.yaml) để tra cứu và tích hợp; CI so sánh file này với YAML runtime.
- Khi Backend đang chạy tại `http://localhost:8080`, tạo/cập nhật snapshot bằng `./scripts/export-openapi.ps1`. Script tải trực tiếp `/v3/api-docs.yaml` và thay file chỉ sau khi nhận được tài liệu OpenAPI hợp lệ.
- Operation cần đăng nhập khai báo `bearerAuth`; operation công khai thì không. Response lỗi dùng `application/problem+json` theo `ProblemDetail`; operation nhận dữ liệu khai báo `400`, operation cần đăng nhập khai báo `401`/`403`, và lỗi riêng được khai báo theo mã Backend thực sự trả về.
- `OpenApiContractIntegrationTest` kiểm tra security và response lỗi trong generated contract so với quy tắc runtime.
- Base path: `/api/v1`.
- Giao diện chính thức để team xem và thử API: `/scalar` trên cùng Backend host/port. Scalar tải generated contract từ `/v3/api-docs`.
- Swagger UI vẫn được giữ như giao diện tương thích trong giai đoạn đầu; Scalar là UI được khuyến nghị cho team.
- JSON property dùng `camelCase`.
- Timestamp biểu diễn instant dùng ISO-8601 UTC; calendar date dùng `YYYY-MM-DD` và không timezone-shift.
- Response lỗi dùng `application/problem+json` theo `ProblemDetail`, bổ sung `code` ổn định và `errors` cho lỗi theo field khi cần.

Sau khi thay đổi Controller, DTO hoặc OpenAPI annotations, chạy script xuất YAML rồi đưa bản cập nhật vào cùng thay đổi. Docker Development tải YAML runtime, validate bằng Scalar CLI, so sánh với file được commit và báo lỗi nếu snapshot chưa cập nhật. Artifact của CI vẫn cung cấp runtime spec của lần chạy để kiểm tra khi cần.

### 2.1 Scalar dành cho thành viên và tester

Mở `http://localhost:8080/scalar` khi Backend chạy local (bao gồm stack Docker Compose). Tại Scalar, thành viên có thể tìm operation, đọc request/response/schema/status/security, nhập Bearer/JWT token thủ công nếu generated runtime spec khai báo scheme phù hợp, và gửi request tới Backend. Không dùng JWT thật trong ảnh chụp, log hoặc tài liệu.

Scalar là giao diện xem và manual testing; request thử bằng Scalar không thay thế automated regression, authorization, integration hoặc acceptance tests. CI kiểm tra HTTP route và nội dung HTML entry point `/scalar` nhưng không chứng minh JavaScript đã tải/render, OpenAPI đã hiển thị trong browser, hoặc một API request đã chạy thành công. Runtime UI chỉ hiển thị auth schemes do generated OpenAPI khai báo; Bearer/JWT acceptance cần scheme và protected runtime endpoint thật.

## 3. Authentication flow

### 3.1 Access token

Đăng nhập email/password (`POST /auth/login`) và đăng nhập Google (`POST /auth/google`, mục 3.4) cùng trả Stateless JWT Access Token trong JSON body (`AuthResponse`). Frontend gửi token ở các API được bảo vệ:

```http
Authorization: Bearer <access-token>
```

Frontend không ghi access token vào log. Login phát JWT ký HS256 bằng secret cấu hình qua `MAMXANH_JWT_SECRET`; token chứa `iss`, `sub` (`USER.user_id`), `role`, `iat`, `exp`, không có `sid` hoặc refresh token. TTL là 60 phút (`expiresInSeconds = 3600`). Frontend lưu phiên trong `sessionStorage` của tab hiện tại; phiên bị xóa khi tab đóng và không đồng bộ giữa các tab. Request mang Bearer token được Backend kiểm tra tài khoản hiện hành; token không hợp lệ/hết hạn hoặc user không còn tồn tại trả `401`, tài khoản bị quản trị khóa trả `403 ACCOUNT_LOCKED`. Frontend xóa phiên khi nhận các lỗi kết thúc phiên này.

### 3.2 Client-side Logout

Hệ thống sử dụng Stateless JWT Access Token, không duy trì server-side session, cookie hay refresh token. Do đó không tồn tại endpoint `POST /auth/logout` trên máy chủ.

Khi người dùng chọn Đăng xuất, Frontend xóa phiên cục bộ và điều hướng về Đăng nhập; không gọi Backend. Frontend thực hiện:
1. Xóa Access Token khỏi nơi lưu trữ client.
2. Xóa thông tin người dùng trong AuthContext / state.
3. Điều hướng người dùng về trạng thái Guest hoặc màn hình Đăng nhập.

### 3.3 Candidate Follow-up: GET /auth/me

Endpoint `GET /auth/me` (tra cứu thông tin người dùng hiện tại từ token) là một ứng viên follow-up tiềm năng nhưng chưa thuộc planned contract hoặc runtime API. Việc thêm endpoint này sẽ được xem xét trong task riêng khi có yêu cầu cụ thể.

### 3.4 Google Login — FR-03-D

Frontend dùng Google Identity Services (`@react-oauth/google`) để lấy Google ID Token rồi gửi `POST /auth/google` với body `{ "idToken": "<Google ID Token>" }` (tối đa 4096 ký tự). Endpoint công khai, không cần Bearer token; response thành công là `200` với `AuthResponse` như đăng nhập mật khẩu. Không có refresh token hay session máy chủ.

Backend xác minh ID Token bằng `GoogleIdTokenVerifier` (`com.google.api-client:google-api-client`): chữ ký RS256 theo chứng chỉ Google công bố, `exp`, issuer `accounts.google.com`/`https://accounts.google.com` và audience bằng `MAMXANH_GOOGLE_CLIENT_ID`. Việc xác minh chạy trước transaction database. Email, tên và ảnh chỉ lấy từ claim đã xác minh; client không gửi các giá trị này.

Tài khoản được xử lý theo thứ tự (Q26, Q39–Q42):

1. Google `email_verified = false`, token không hợp lệ hoặc email Google không lưu được (không phải ASCII hợp lệ, dài hơn 255 ký tự): `401 GOOGLE_TOKEN_INVALID`, không tạo hay liên kết tài khoản.
2. Tìm theo `USER.google_subject` (`sub`) trước. Tìm thấy: tài khoản `LOCKED` trả `403 ACCOUNT_LOCKED`, còn lại đăng nhập; tên và ảnh không bị ghi đè.
3. Không thấy theo `google_subject` thì tìm theo email đã chuẩn hóa (trim, chữ thường):
   - Không có tài khoản: tạo Member mới `CUSTOMER`, `ACTIVE`, `email_verified = true`, không có mật khẩu; `display_name` và `avatar_url` lấy từ Google. Tên được trim, cắt còn 50 ký tự; nếu rỗng hoặc dưới 3 ký tự thì dùng phần trước `@` của email khi phần đó dài 3–50 ký tự, nếu không thì dùng `Thành viên Mâm Xanh`. Ảnh dài hơn 2048 ký tự bị bỏ qua.
   - Tài khoản `LOCKED`: `403 ACCOUNT_LOCKED`, không liên kết.
   - Email đã liên kết với `google_subject` khác: `409 GOOGLE_ACCOUNT_CONFLICT`.
   - Tài khoản mật khẩu chưa xác minh email: liên kết `google_subject`, đặt `email_verified = true`, xóa `password_hash` và mã xác minh email.
   - Tài khoản mật khẩu đã xác minh email: liên kết `google_subject` và giữ nguyên mật khẩu; người dùng đăng nhập được bằng cả hai cách.
4. Chặn tạm thời do sai mật khẩu (`login_blocked_until`) không ngăn Google Login, và Google Login thành công không đặt lại bộ đếm sai mật khẩu.

Không tải được chứng chỉ Google trả `503 GOOGLE_LOGIN_UNAVAILABLE`. Khi `MAMXANH_GOOGLE_CLIENT_ID` để trống, Backend từ chối mọi token (`401 GOOGLE_TOKEN_INVALID`) và Frontend không hiện nút Google (thiếu `VITE_GOOGLE_CLIENT_ID`). Client ID là giá trị công khai; Backend và Frontend phải dùng cùng một Client ID, và Google Cloud Console phải khai báo origin của Frontend (ví dụ `http://localhost:5173`).

Sau khi Google Login thành công, Frontend gọi `POST /nutrition/dietary-preferences/onboarding/invitation` như sau đăng nhập mật khẩu (mục 9).

### 3.5 Đặt lại mật khẩu — FR-03-E

Hai endpoint công khai, không cần Bearer token (UC-03.6, UC-03.7, AC-03.14, Q27):

- `POST /auth/password-resets` nhận `{ "email": "..." }` và luôn trả `202` với `{ "message": "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn." }`. Chỉ email sai định dạng trả `400 VALIDATION_FAILED`. Backend chỉ gửi email khi tài khoản tồn tại, đang `ACTIVE`, đã qua 60 giây kể từ email đặt lại trước và chưa gửi đủ 5 email trong khung 1 giờ của tài khoản đó; email không tồn tại, tài khoản `LOCKED` và yêu cầu vượt giới hạn đều nhận cùng phản hồi nhưng không có email, không có `429`. Không giới hạn theo IP. Tài khoản chưa xác minh email vẫn nhận email đặt lại.
- Email chứa liên kết `{MAMXANH_FRONTEND_BASE_URL}/dat-lai-mat-khau?token=<token>`. Token là 256 bit ngẫu nhiên (`SecureRandom`); `USER` chỉ lưu SHA-256 digest và hạn 15 phút. Token mới ghi đè token cũ nên chỉ liên kết mới nhất dùng được. Email được gửi bất đồng bộ sau khi transaction commit.
- `POST /auth/password-resets/confirm` nhận `{ "token": "...", "newPassword": "...", "confirmPassword": "..." }`. Mật khẩu mới theo cùng quy tắc đăng ký (8–64 ký tự, tối đa 72 bytes UTF-8, có chữ in hoa, chữ thường và chữ số) và phải khớp `confirmPassword`. Thành công trả `204`: lưu BCrypt hash mới, xóa token, đặt `email_verified = true` nếu email chưa xác minh và kết thúc khóa tạm do nhập sai mật khẩu (NFR-07). Tài khoản chỉ đăng nhập Google (chưa có mật khẩu) có thể đặt mật khẩu theo cách này và giữ liên kết Google (Q47).
- Lỗi của bước xác nhận: token không tồn tại, đã dùng, đã bị thay hoặc hết hạn trả `400 PASSWORD_RESET_TOKEN_INVALID`; mật khẩu mới trùng mật khẩu hiện tại trả `400 NEW_PASSWORD_SAME_AS_CURRENT` và token vẫn dùng được (Q48); tài khoản bị khóa sau khi nhận email trả `403 ACCOUNT_LOCKED`. Hai yêu cầu xác nhận đồng thời với cùng token chỉ có một yêu cầu thành công.
- Hệ thống không có session máy chủ nên không có phiên nào cần thu hồi; access token đã phát trước đó vẫn hợp lệ đến khi hết hạn (tối đa 60 phút, Q30).

Frontend: `/quen-mat-khau` gửi email và hiển thị nguyên văn `message` trung tính; `/dat-lai-mat-khau` đọc `token` từ URL, giữ trong bộ nhớ và xóa khỏi thanh địa chỉ trước khi người dùng nhập mật khẩu mới.

### Upload ảnh công thức — FR-14 / fix #158

`POST /api/v1/recipes/media/upload` nhận multipart trường `file`, yêu cầu Expert và trả `201` với envelope `data.blobUrl`, `data.mimeType`, `data.sizeBytes` sau khi lưu Azure thành công. Thiếu cấu hình, khởi tạo storage thất bại hoặc upload Azure lỗi trả `503 MEDIA_STORAGE_UNAVAILABLE` dưới dạng `application/problem+json`; không trả URL mock. Các tính năng không dùng storage vẫn khởi động. Storage dùng chung cho avatar cũng từ chối upload khi Azure không khả dụng.

Root Compose truyền `AZURE_STORAGE_CONNECTION_STRING` và `AZURE_STORAGE_CONTAINER_NAME` từ env file Backend. Container lưu ảnh công khai cần cho phép đọc Blob để trình duyệt mở URL ảnh; connection string chỉ nằm ở Backend. Ảnh cũ chỉ có URL mock cần tải lại vì chưa có nội dung tệp trên Azure.

### 3.6 Định danh lượt xem công thức — FR-58

Luồng FR-58 dùng `POST /api/v1/recipes/{recipeId}/views`. Gửi sự kiện sau khi nội dung chi tiết công thức công khai tải thành công; GET chi tiết và GET danh sách không tự tạo sự kiện. Schema/status chi tiết lấy từ generated OpenAPI. Transaction/Flyway và persistence phải được kiểm chứng bằng test SQL Server/Compose; kiểm thử với mock không thay bằng chứng này.

- Member gửi Bearer như các API hiện có; Backend lấy `user_id` từ JWT đã xác minh, không nhận danh tính tài khoản hoặc thời gian từ body. Guest không gửi Bearer và phải gửi `anonymousViewerId` dạng UUID chuẩn; Backend lưu SHA-256 của UUID đã chuẩn hóa chữ thường.
- Frontend giữ UUID của Guest qua reload bằng session cookie `mamxanh.recipe-viewer` trên chính origin Frontend (`Path=/`, `SameSite=Lax`, `Secure` khi HTTPS), rồi gửi trong JSON body. Đây là định danh ẩn danh, không phải auth cookie; không cần bật credentials xuyên origin hoặc sửa mô hình đăng nhập JWT. Khi cookie bị chặn, client báo lỗi và không gửi sự kiện bằng UUID tạm thay đổi qua reload.
- Trang chi tiết ở cả `/cong-thuc/id/:recipeId` và đường dẫn ID số `/cong-thuc/:slug` gửi sự kiện sau khi GET thành công và nội dung đã render. Lỗi GET hoặc phản hồi GET đến sau khi rời trang không ghi nhận lượt xem. Frontend dùng `statistics.viewCount` làm số ban đầu và `viewCount` trong response ghi nhận làm số mới, kể cả `counted=false`; không tự cộng số. Trang chủ và Khám phá dùng tổng `viewCount` từ danh sách. Lỗi ghi nhận vẫn cho phép đọc công thức và thử lại với cùng định danh; phản hồi muộn của trang trước không cập nhật trang hiện tại. Các luồng này đã được nối trong source; kiểm thử trình duyệt dùng API giả lập, chưa thay thế bằng chứng tích hợp SQL Server/Compose.
- Bearer sai/hết hạn không được hạ xuống Guest; tài khoản bị khóa giữ lỗi xác thực hiện hành. Không có loại trừ lượt xem của tác giả như quy tắc Like/Dislike.
- Response trực tiếp gồm `recipeId`, `counted`, `viewCount`, có `Cache-Control: no-store`. Request trùng vẫn thành công với `counted=false`; retry cùng định danh không làm tăng lượt đếm trong cửa sổ.
- Cửa sổ dựa trên lượt hợp lệ gần nhất: không quá 30 phút (kể cả đúng mốc 30 phút) thì bỏ qua; hơn 30 phút thì ghi mới. Request bị bỏ qua không cập nhật timestamp. Thời gian dùng Clock UTC của Backend.
- Sự kiện và bộ đếm được ghi trong cùng transaction. Khi lấy khóa người xem–công thức thất bại, API trả `503 RECIPE_VIEW_UNAVAILABLE`; client có thể thử lại cùng định danh. Không tăng số trên UI nếu chưa có phản hồi thành công; lỗi ghi nhận lượt xem không nên ngăn đọc nội dung công thức.
- Tổng công khai không tiết lộ định danh hoặc lịch sử người xem. Xóa cookie/đổi phiên tạo định danh Guest khác; khử trùng lặp theo phiên không phải cơ chế chống bot hoàn chỉnh.

## 4. Error convention

Frontend xử lý theo HTTP status và `code`, không parse câu chữ trong `detail`.

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request contains invalid data.",
  "instance": "/api/v1/auth/register",
  "code": "VALIDATION_FAILED",
  "errors": [
    {
      "field": "email",
      "message": "Email must be valid."
    }
  ]
}
```

Quy ước status chính:

| Status | Ý nghĩa |
|---|---|
| `200 OK` | Request thành công và có response body. |
| `201 Created` | Tài khoản đã được tạo. |
| `202 Accepted` | Yêu cầu email đã được tiếp nhận; không xác nhận email có tồn tại. |
| `204 No Content` | Thao tác thành công và không cần response body. |
| `400 Bad Request` | Payload, token xác minh hoặc token đặt lại mật khẩu không hợp lệ. |
| `401 Unauthorized` | Credential, access token hoặc Google ID Token không hợp lệ hay đã hết hạn. |
| `403 Forbidden` | Tài khoản chưa xác minh (`EMAIL_NOT_VERIFIED`), bị Administrator khóa (`ACCOUNT_LOCKED`) hoặc không phải Member khi dùng hồ sơ sở thích hay hồ sơ cá nhân (`MEMBER_ACCESS_REQUIRED`). |
| `409 Conflict` | Email đã được sử dụng, đã liên kết với tài khoản Google khác (`GOOGLE_ACCOUNT_CONFLICT`), hoặc hồ sơ sở thích chưa đủ để gọi AI cá nhân hóa (`DIETARY_PROFILE_INCOMPLETE`). |
| `429 Too Many Requests` | Chỉ áp dụng khi contract của endpoint quy định `429` (ví dụ login, resend verification hoặc AI rate limiting); client đọc `Retry-After` khi có. Password-reset request không trả `429`. |
| `503 Service Unavailable` | Dịch vụ phụ thuộc tạm thời không dùng được, ví dụ không tải được chứng chỉ Google (`GOOGLE_LOGIN_UNAVAILABLE`). |

Mã `code` đã triển khai (Issue #5, #6, #8, #9, #29, #36).

| `code` | Status | Khi nào |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Payload sai định dạng hoặc vi phạm validation; chi tiết theo field nằm trong `errors`. Mật khẩu yếu trả một phần tử `errors` cho mỗi tiêu chí còn thiếu. |
| `EMAIL_ALREADY_USED` | 409 | `POST /auth/register` với email đã có tài khoản (không phân biệt hoa/thường). |
| `VERIFICATION_TOKEN_INVALID` | 400 | `POST /auth/email-verifications` với mã không tồn tại, đã dùng, đã bị thay bằng mã mới hoặc hết hạn. |
| `RESEND_TOO_SOON` | 429 | `POST /auth/email-verifications/resend` trong vòng 60 giây kể từ email xác minh trước; kèm `Retry-After`. |
| `PASSWORD_RESET_TOKEN_INVALID` | 400 | `POST /auth/password-resets/confirm` với mã không tồn tại, đã dùng, đã bị thay bằng mã mới hoặc hết hạn (15 phút). |
| `NEW_PASSWORD_SAME_AS_CURRENT` | 400 | `POST /auth/password-resets/confirm` với mật khẩu mới trùng mật khẩu hiện tại; token chưa bị tiêu thụ. |
| `INVALID_CREDENTIALS` | 401 | `POST /auth/login` với email không tồn tại, sai mật khẩu hoặc tài khoản chỉ đăng nhập Google (không có mật khẩu); mọi trường hợp cùng `detail` trung tính "Email hoặc mật khẩu không chính xác." |
| `EMAIL_NOT_VERIFIED` | 403 | `POST /auth/login` đúng mật khẩu nhưng email chưa xác minh. |
| `ACCOUNT_LOCKED` | 403 | `POST /auth/login` đúng mật khẩu nhưng tài khoản bị Administrator khóa; `POST /auth/google` cho tài khoản đang `LOCKED`; `POST /auth/password-resets/confirm` với token hợp lệ của tài khoản đang `LOCKED`; hoặc request mang Bearer token hợp lệ của tài khoản đang `LOCKED`. |
| `LOGIN_TEMPORARILY_BLOCKED` | 429 | Lần sai mật khẩu thứ 5 liên tiếp và mọi lần thử `POST /auth/login` trong 10 phút sau đó (kiểm tra trước khi so mật khẩu); kèm `Retry-After` là số giây còn lại. Không đổi `account_status` và không áp dụng cho `POST /auth/google`. |
| `GOOGLE_TOKEN_INVALID` | 401 | `POST /auth/google` với ID Token sai định dạng, sai chữ ký, hết hạn, sai issuer/audience, thiếu `sub`/email, email Google chưa xác minh hoặc không lưu được; cũng trả khi Backend chưa cấu hình `MAMXANH_GOOGLE_CLIENT_ID`. |
| `GOOGLE_ACCOUNT_CONFLICT` | 409 | `POST /auth/google` khi email đã liên kết với một tài khoản Google khác. |
| `GOOGLE_LOGIN_UNAVAILABLE` | 503 | `POST /auth/google` khi Backend không tải được chứng chỉ ký của Google. |
| `MEMBER_ACCESS_REQUIRED` | 403 | Tài khoản Administrator gọi endpoint hồ sơ sở thích ăn uống (FR-31) hoặc hồ sơ cá nhân `/me/profile` (FR-23); chỉ Member (`CUSTOMER`, `EXPERT`) có các hồ sơ này. |
| `MEMBER_PROFILE_NOT_FOUND` | 404 | `GET /members/{userId}` hoặc `/members/{userId}/recipes` với ID không tồn tại hoặc thuộc tài khoản Administrator (FR-23, Q55). |
| `FILE_TOO_LARGE` | 413 | Ảnh đại diện vượt 2 MB, hoặc tệp tải lên vượt giới hạn 5 MB của máy chủ. |
| `UNSUPPORTED_IMAGE_TYPE` | 400 | Tệp ảnh không phải JPEG, PNG hoặc WebP; với ảnh đại diện, nội dung tệp (chữ ký đầu tệp) phải khớp `Content-Type`. |
| `INGREDIENT_PREFERENCE_CONFLICT` | 400 | `PUT /nutrition/dietary-preferences` có cùng một tên (không phân biệt hoa/thường) ở cả danh sách cần tránh và danh sách không thích. |
| `DIETARY_PROFILE_INCOMPLETE` | 409 | Cổng AI cá nhân hóa (BR-31) chặn yêu cầu vì hồ sơ thiếu nhóm thông tin tối thiểu; body có thêm `missing` (`VEGETARIAN_TYPE`, `AVOID_INGREDIENTS`, `DISLIKED_INGREDIENTS`). |
| `REPORT_ALREADY_OPEN` | 409 | `POST /recipes/{recipeId}/reports` khi tài khoản đã có báo cáo `OPEN` hoặc `IN_REVIEW` trên bài đó, kể cả khi gửi đồng thời (SRS 3.14); body có thêm `reportId` của báo cáo đang mở (AC-30.2). Xem mục 22. |
| `REPORT_NOT_OPEN` | 409 | `POST /me/reports/{reportId}/supplements` khi báo cáo đã `RESOLVED`. Xem mục 22. |
| `REPORT_SUPPLEMENT_LIMIT` | 409 | `POST /me/reports/{reportId}/supplements` khi báo cáo đã có 5 phần bổ sung. Xem mục 22. |
| `UNAUTHENTICATED` | 401 | Gọi endpoint cần đăng nhập mà không có Bearer token, hoặc token sai chữ ký, sai issuer, hết hạn hay thuộc tài khoản không còn tồn tại. |
| `ACCESS_DENIED` | 403 | Đã xác thực nhưng không đủ quyền. |
| `REPORT_NOT_FOUND` | 404 | `/me/reports/{reportId}` (mục 22) hoặc `/admin/reports/{reportId}` (mục 21) với ID không phải báo cáo Recipe Post. |
| `REPORT_STATUS_CONFLICT` | 409 | Tiếp nhận báo cáo không còn `OPEN`, hoặc ghi kết luận khi báo cáo chưa được chính Administrator đó tiếp nhận hay đã `RESOLVED`. Xem mục 21. |
| `REPORT_DECISION_NOT_APPLICABLE` | 409 | Kết luận `HIDE_RECIPE` cho bài tác giả đã xóa, hoặc `LOCK_AUTHOR` khi tác giả là Administrator. Báo cáo giữ `IN_REVIEW`. Xem mục 21. |
| `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE` | 404, 405, 415 | Lỗi định tuyến/định dạng do framework phát hiện. |
| `INTERNAL_ERROR` | 500 | Lỗi không mong đợi; `detail` không chứa thông tin nội bộ. |

`POST /auth/email-verifications/resend` trả cùng một phản hồi `202` cho email không tồn tại và email đã xác minh; hai trường hợp này không gửi email.

## 5. Quy tắc bảo mật của Auth slice

- Không trả password, password hash hoặc Google ID token trong response/log.
- Login sai dùng thông báo chung để tránh tiết lộ email có tồn tại.
- Quy tắc trên không che giấu hoàn toàn trạng thái khóa tạm: tài khoản đang bị chặn trả `429 LOGIN_TEMPORARILY_BLOCKED`, còn email không tồn tại/sai mật khẩu trả `401 INVALID_CREDENTIALS`. Đây là rủi ro account-enumeration đã được owner chấp nhận cho Issue #6.
- Password-reset request luôn trả cùng HTTP 202 trung tính; silent rate limiting vẫn enforce cooldown 60 giây và tối đa 5 email/giờ/tài khoản. Khi vượt limit, email không tồn tại hoặc tài khoản `LOCKED`, không gửi email; response không tiết lộ account existence hay trạng thái rate limit.
- Sau 5 lần đăng nhập sai liên tiếp, rate limit tạm thời 10 phút ở cấp tài khoản (lưu trên bảng `USER`); không rate limit IP; không đổi account status thành `LOCKED`.
- Giới hạn độ dài mật khẩu: 8–64 ký tự, tối đa 72 bytes UTF-8 (chuẩn BCrypt).
- Đăng xuất xử lý hoàn toàn phía client (không gọi backend API).
- Google ID token phải được Backend xác minh chữ ký, issuer, audience và expiry trước khi phát hành token; tài khoản được định danh bằng `google_subject`, không bằng email hay tên do client gửi.
- Liên kết Google vào tài khoản mật khẩu chưa xác minh sẽ xóa mật khẩu đó, vì mật khẩu có thể do người không sở hữu hộp thư đặt trước (Q26).

## 6. Nutrition eligibility — FR-38 và Nutrition Profile — FR-35

Eligibility và hồ sơ dinh dưỡng là hai thao tác Backend riêng. Client không truyền `userId`; các endpoint yêu cầu authenticated Member principal. Tích hợp JWT thật phụ thuộc authentication contract chung; không dùng fake authentication trong production.

- `GET /nutrition/profile/eligibility` trả trạng thái hiện tại (`NOT_CONFIRMED`, `ELIGIBLE`, `INELIGIBLE`) và `confirmedAt`. Trạng thái ban đầu là `NOT_CONFIRMED`, timestamp `null`.
- `PUT /nutrition/profile/eligibility` nhận trạng thái xác nhận `ELIGIBLE` hoặc `INELIGIBLE`. Chỉ request xác nhận chủ động mới cập nhật Database; Backend ghi trạng thái và thời điểm xác nhận thành công gần nhất trong cùng giao dịch. `NOT_CONFIRMED` không phải trạng thái được gửi để xác nhận. Lỗi validation/Backend không cập nhật giá trị cũ. Endpoint này tách biệt với lưu hồ sơ và `health_data_consent_at`; không có draft/autosave hoặc lịch sử xác nhận.
- `GET /nutrition/profile`, `PUT /nutrition/profile` và `POST /nutrition/profile/calculate` yêu cầu `ELIGIBLE`. `NOT_CONFIRMED` và `INELIGIBLE` bị chặn ở Backend trước khi đọc, lưu hay tính hồ sơ cá nhân; `INELIGIBLE` vẫn được gọi API eligibility để xác nhận lại. Khi trở lại `ELIGIBLE`, quyền truy cập được khôi phục theo phân quyền hiện hành.
- Guard trả `403 NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED` cho `NOT_CONFIRMED` và `403 NUTRITION_ELIGIBILITY_INELIGIBLE` cho `INELIGIBLE`.
- `PUT /nutrition/profile` lưu hồ sơ FR-35 và đồng thuận xử lý dữ liệu sức khỏe theo hợp đồng FR-35. `nutritionGoal` có thể là `null`; các giá trị được hỗ trợ gồm `MAINTAIN_WEIGHT`, `LOSE_WEIGHT`, `GAIN_WEIGHT`, `BUILD_MUSCLE`, `IMPROVE_HEALTH` và `SUPPORT_TRAINING`. Việc này không tự xác nhận hoặc đổi trạng thái eligibility.
- `PUT /nutrition/profile` tính và lưu BMI trong cùng hồ sơ khi Member lưu/cập nhật chiều cao hoặc cân nặng. `GET /nutrition/profile` trả lại BMI đã lưu và các mức tham khảo từ hồ sơ; mở lại trang không tính lại BMI. `POST /nutrition/profile/calculate` cũng dùng BMI đã lưu cùng 8 thành phần dinh dưỡng (9 chỉ tiêu khi tính cả năng lượng). Không lưu lịch sử tính toán.
- FR-36 dùng trạng thái `ELIGIBLE` đã xác nhận trước khi tạo hoặc áp dụng menu. Nếu chưa xác nhận, API trả `403 NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED`; người dùng có thể xác nhận chủ động qua `PUT /nutrition/profile/eligibility` trước khi thử lại.
- Guard dinh dưỡng cá nhân FR-37 cần được tích hợp tại API do FR đó sở hữu; chỉ endpoint đã tồn tại và được bảo vệ mới có bằng chứng nghiệm thu.
- Response hiển thị số dạng xấp xỉ. Đây là tham khảo, không phải chẩn đoán/điều trị/kê đơn, tư vấn y tế, chứng nhận hay giám sát liên tục.
- Lỗi dùng `application/problem+json` và mã ổn định; trường hợp ngoài phạm vi trả `422 NUTRITION_PROFILE_OUT_OF_SCOPE`.

## 7. Recipe detail, Meal Plan references and saved recipes — FR-20

Các endpoint detail và Meal Plan dưới đây trả projection đọc từ `RECIPE_POST` và bảng liên quan hiện có. Saved Recipes đọc và thay đổi tham chiếu riêng của tài khoản hiện tại trong `SAVED_RECIPE`, không tạo bản sao recipe. Generated `/v3/api-docs` là contract chi tiết của runtime.

| Acceptance criterion / integration | Runtime API hoặc owner | Hành vi |
|---|---|---|
| AC-20.1 — detail từ nguồn RecipePost duy nhất | `GET /api/v1/recipes/{recipeId}` (public) | Trả dữ liệu recipe hiện tại, hồ sơ tác giả công khai, nguyên liệu định lượng, media theo thứ tự, YouTube URL, 9 chỉ tiêu dinh dưỡng ước tính trên toàn công thức và mỗi khẩu phần kèm cảnh báo thiếu dữ liệu, thống kê Like/Dislike và lượt xem. Bài không công khai hoặc không tồn tại trả `404`. |
| AC-20.2 — cập nhật mới nhất ở detail và Meal Plan | `GET /api/v1/recipes/{recipeId}` và `GET /api/v1/meal-plans?weekStartDate=YYYY-MM-DD` (có xác thực) | Mỗi request đọc RecipePost và nguyên liệu hiện tại; không lưu snapshot recipe trùng lặp. |
| AC-20.3 — tombstone an toàn trong Meal Plan | `GET /api/v1/meal-plans?weekStartDate=YYYY-MM-DD` (có xác thực) | Tham chiếu không khả dụng vẫn ở đúng ô, kèm `recipeDeleted` và `unavailableMessage` chung `Công thức không còn khả dụng`; nội dung recipe bị lược bỏ. |
| UC-32.1/32.2 — lưu và bỏ lưu | `PUT /api/v1/saved-recipes/{recipeId}` và `DELETE /api/v1/saved-recipes/{recipeId}` (có xác thực) | Chỉ tài khoản Member hiện tại được xử lý. Lưu chỉ chấp nhận công thức công khai; gọi lặp không tạo mục trùng. Bỏ lưu chỉ xóa tham chiếu của tài khoản hiện tại và an toàn khi gọi lặp. |
| UC-32.3 — danh sách, tìm kiếm và phân trang công thức đã lưu | `GET /api/v1/saved-recipes?keyword=&page=0&size=20` (có xác thực) | Chỉ trả tham chiếu của tài khoản hiện tại, sắp xếp mới lưu gần đây trước. `size` giới hạn 1–50; `keyword` tối đa 120 ký tự, tìm trong tên và mô tả. Mục không khả dụng vẫn được giữ an toàn với `available=false`, thông báo chung và không có nội dung recipe, tác giả hay media. |
| AC-20.4 — export PDF/TXT | Frontend | Backend trả dữ liệu detail cần cho export. Frontend tạo TXT và PDF khổ A4; API này không tạo file. |
| AC-20.5 — tải toàn trang trong hai giây | Frontend xác minh acceptance | Backend trả đủ instructions trong cùng response detail. Frontend cần đo thời gian tải và render toàn trang trên môi trường mục tiêu. |
| AC-40.1/40.2 — công khai khi thiếu dữ liệu dinh dưỡng (FR-40, BR-50) | `POST /api/v1/recipes`, `GET /api/v1/recipes/{recipeId}` | Nguyên liệu trong catalog nhưng chưa bật tính không chặn công khai (nguyên liệu ngoài catalog vẫn bị từ chối theo BR-12). `nutritionSupported` biểu thị có tham gia tính phần dữ liệu hiện có; `nutritionComplete` và `ingredientsWithoutNutrition` cảnh báo chỉ tiêu còn thiếu. Các chỉ tiêu `NULL` không được xem là 0; số liệu AI-generated được giải thích là ước tính chưa xác minh. |
| AC-40.3 — loại khỏi ứng viên AI menu dinh dưỡng (BR-40, BR-50) | `POST /api/v1/meal-plans/ai-menu/previews`, `PUT /api/v1/meal-plans/ai-menu` | Pool ứng viên FR-36 (dùng cho prompt Gemini và cho bước kiểm lại khi áp dụng) chỉ gồm Recipe Post `PUBLISHED` có dinh dưỡng đầy đủ, cùng phép tính với detail. Áp dụng menu chứa bài thiếu dữ liệu trả `404 RECIPE_NOT_FOUND`, Lịch ăn giữ nguyên. Không có endpoint mới. |

Các key trong `nutrition.total` và `nutrition.perServing` lần lượt là `ENERGY_KCAL` (kcal), `PROTEIN_G` (g), `CARBOHYDRATE_G` (g), `TOTAL_FAT_G` (g), `FIBER_G` (g), `CALCIUM_MG` (mg), `IRON_MG` (mg), `VITAMIN_B12_MCG` (mcg) và `ZINC_MG` (mg). Nếu chưa có lượt bình chọn, `statistics.reactionCount` bằng `0` và `statistics.likePercentage` là `null`; Frontend hiển thị nhãn “Mới” theo BR-69.

Nutrition chỉ dùng danh mục nội bộ và quy đổi gram hiện có. `nutrition.complete=false` cùng `ingredientsMissingData` chỉ rõ nguyên liệu tùy chỉnh/thiếu dữ liệu hoặc thiếu quy đổi; không ngầm coi dữ liệu chưa biết là giá trị đo bằng không. Đây là số liệu tham khảo, không phải tư vấn y tế. Không gọi dịch vụ dinh dưỡng ngoài.

So sánh hai công thức (FR-60) không có API riêng: trang `/so-sanh?a={recipeId}&b={recipeId}` đọc `GET /api/v1/recipes/{recipeId}` cho từng bài; trang chọn món `/so-sanh/chon-mon` dùng `GET /api/v1/recipes/discovery` (cùng endpoint với trang Khám phá, trả `hasNext` thay vì tổng số) với cùng tham số tìm kiếm và lọc của trang Khám phá (`keyword`, `vegetarianType`, `dishCategory`, `ingredientIds`, `maxTotalTimeMinutes`, `sort`, `viewPeriod`, `page`, `size`) và loại bài đang ở bên còn lại ở phía Frontend. Bài trả `404` được gỡ khỏi so sánh và người dùng chọn bài khác. Bảng dinh dưỡng chỉ dùng `nutrition.perServing` khi `nutrition.complete=true`; nếu không, cả chín chỉ tiêu của bên đó hiển thị `Chưa đủ dữ liệu` và không tính chênh lệch. Trang chỉ gửi request `GET`, không lưu lịch sử so sánh (BR-76).

### 7.1. Tạo hoặc mở lịch tuần — FR-09

- `GET /api/v1/meal-plans?weekStartDate=YYYY-MM-DD` chỉ đọc lịch của Member hiện tại. Khi chưa có lịch, trả tuần với `entries=[]` và không tạo bản ghi.
- `PUT /api/v1/meal-plans?weekStartDate=YYYY-MM-DD` tạo lịch nếu chưa có, hoặc mở lại lịch đã có, trả `200` cùng `MealPlanWeekResponse`. Không cần request body. Gọi lặp giữ nguyên lịch và các món đã lưu.
- Cả hai thao tác yêu cầu JWT Bearer và `weekStartDate` theo định dạng ISO, là ngày Thứ Hai. Chủ lịch được lấy từ tài khoản đã xác thực; client không được chọn chủ lịch bằng `userId`. Guest/token không hợp lệ trả `401`; Admin hoặc tài khoản bị khóa trả `403`; ngày thiếu, sai định dạng hoặc không phải Thứ Hai trả `400 VALIDATION_FAILED`.
- Mỗi Member chỉ có một lịch cho mỗi tuần theo unique constraint hiện có. Thao tác tạo dùng transaction và khóa phạm vi khóa SQL Server để xử lý yêu cầu tạo đồng thời. Không gọi Gemini, không tạo món mặc định và không thay đổi danh sách công thức đã lưu.
- Contract chi tiết lấy từ generated `/v3/api-docs`; kiểm thử SQL Server và Compose cần được xác minh riêng với kiểm thử MVC dùng mock service.

### 7.2. Thêm, thay, chuyển và gỡ món — FR-09/FR-33

| Thao tác | API | Request và kết quả |
|---|---|---|
| Thêm món | `POST /api/v1/meal-plans/{weekStartDate}/entries` | JSON gồm `mealDate`, `mealType` (`BREAKFAST`, `LUNCH`, `DINNER`) và `recipeId` (số nguyên ≥1). Trả `201` cùng projection `MealPlanWeekResponse.Entry`; khẩu phần mặc định 1. Tạo tuần nếu chưa có. |
| Thay công thức | `PUT /api/v1/meal-plans/{weekStartDate}/entries/{entryId}` | JSON gồm `recipeId` thay thế. Trả `200` cùng projection của mục món; giữ `entryId`, ngày, loại bữa và khẩu phần hiện tại. |
| Chỉnh số phần cần nấu | `PATCH /api/v1/meal-plans/{weekStartDate}/entries/{entryId}/servings` | JSON gồm `plannedServings` (số nguyên 1–9.999). Trả `200` cùng projection đã lưu; giữ nguyên công thức, ngày và bữa. |
| Gỡ món | `DELETE /api/v1/meal-plans/{weekStartDate}/entries/{entryId}` | Trả `204`, không có response body. Chỉ xóa mục lịch; công thức nguồn và mục đã lưu không bị thay đổi. |
| Chuyển món | `PATCH /api/v1/meal-plans/{weekStartDate}/entries/{entryId}/position` | JSON gồm `mealDate` và `mealType` đích. Trả `200` cùng projection đã chuyển; giữ ID, công thức và khẩu phần, kể cả giá trị lẻ cũ. Chỉ chuyển trong cùng tuần; ngoài tuần trả `400`, trùng công thức ở bữa đích trả `409 MEAL_PLAN_RECIPE_DUPLICATE`. |

- `weekStartDate` phải là Thứ Hai; toàn bộ tuần phải nằm trong phạm vi ngày SQL Server (năm 1–9999). Ngày ăn phải nằm trong bảy ngày của tuần đã chọn; dữ liệu không hợp lệ trả `400 VALIDATION_FAILED`.
- Backend xác định chủ lịch từ tài khoản đã xác thực. Mục không tồn tại trong tuần của chủ tài khoản, kể cả ID thuộc người khác hoặc tuần khác, trả `403 ACCESS_DENIED`; không truy xuất hoặc sửa mục theo ID đơn lẻ.
- Chỉnh số phần dùng cùng transaction/khóa tuần và kiểm tra chủ sở hữu như thao tác thay món. Thiếu giá trị, số nhỏ hơn 1 hoặc lớn hơn 9.999, hay có phần thập phân khác 0 trả `400 VALIDATION_FAILED`; cột database vẫn là `DECIMAL(5,1)` để giữ dữ liệu cũ, không thay đổi schema. API ghi khẩu phần mới chỉ nhận số nguyên. Mục có công thức không còn khả dụng vẫn sửa được số phần, nhưng response giữ nhãn trung tính và không trả nội dung recipe bị ẩn/xóa.
- Trong luồng lập lịch hiện tại theo yêu cầu người dùng, `plannedServings` là số phần cần nấu (có thể cho cả gia đình). Dữ liệu này phục vụ tỷ lệ nguyên liệu đi chợ: lượng gốc × `plannedServings` ÷ số phần gốc của công thức. Bảng dinh dưỡng ngày/tuần trên Frontend thống kê riêng **1 người, mỗi món 1 phần**, không nhân theo lượng nấu cho gia đình. BR-47 hiện vẫn mô tả khẩu phần dự định ăn; hai ý nghĩa cần được nhóm phân biệt khi triển khai thống kê lượng đã ăn. Việc có API chỉnh số phần không xác nhận chức năng tổng hợp danh sách đi chợ đã triển khai.
- Một bữa chứa nhiều công thức khác nhau. Thêm hoặc thay thành công thức đã có trong cùng ngày/bữa trả `409 MEAL_PLAN_RECIPE_DUPLICATE`; thay lại cùng công thức của chính mục đó không bị xem là trùng mục khác.
- Chỉ công thức `PUBLISHED` được thêm hoặc dùng để thay thế; nguồn thiếu/ẩn/xóa/nháp trả `404 RECIPE_NOT_FOUND`. Kiểm tra trạng thái nguồn và ghi lịch nằm trong cùng transaction; các thay đổi của cùng lịch tuần được tuần tự hóa bằng khóa ở database và vẫn có unique constraint bảo vệ.
- Tombstone hiện có được thay hoặc gỡ như mục thông thường. Gỡ không yêu cầu công thức nguồn còn công khai; đọc lịch vẫn trả nhãn chung `Công thức không còn khả dụng` và lược bỏ nội dung bài không công khai.
- Frontend dùng dữ liệu response sau thêm/thay/chuyển và bỏ mục sau `204`; khi request thất bại giữ dữ liệu đang hiển thị và cho phép thử lại. Chuyển món khóa lịch tuần trước kiểm tra đích và cập nhật cùng một mục trong transaction; không xóa rồi thêm lại. Các thao tác này không gọi Gemini.

## 8. Thông số cần chốt trước khi triển khai phần phụ thuộc

| Quyết định | Trạng thái | Ảnh hưởng |
|---|---|---|
| Access-token lifetime | `Confirmed (Q30)` | JWT HS256 có hiệu lực 60 phút; MVP/demo chấp nhận access token không bị thu hồi trước hạn. |
| JWT Claims set | `Confirmed (Q31)` | Dùng `iss`, `sub` (`USER.user_id`), `role`, `iat`, `exp`; không dùng `jti`. |
| Frontend token storage | `Confirmed (Q33)` | Lưu phiên trong `sessionStorage`; không đồng bộ giữa các tab. |
| Candidate endpoint `GET /auth/me` | `PENDING` | Xem xét bổ sung sau nếu Frontend cần đồng bộ lại user profile. |
| Role freshness sau khi đổi role | `Confirmed (Q32)` | Backend lấy quyền từ `USER.role` khi xác thực từng request; claim `role` chỉ phục vụ hiển thị. |
| Public/development server URLs và CORS origins | `TBD` | Chốt theo môi trường thực tế trước khi cấu hình OpenAPI `servers` và CORS. |

Baseline API đã được nhóm chấp nhận; các mục `TBD` không tự có giá trị chỉ vì tài liệu chuyển sang `Active`. Owner FR-03 phân rã, đề xuất giá trị và cách kiểm thử; Tech Lead duyệt trước khi phần liên quan được coi là implementation-ready. Không suy diễn các giá trị này từ ví dụ hoặc cấu hình tạm.

## 9. Sở thích ăn uống và Onboarding — FR-31

Năm endpoint runtime thao tác hồ sơ của Member đang đăng nhập; client không truyền `userId`. Generated OpenAPI khai báo security scheme `bearerAuth` (HTTP bearer, JWT) cho cả năm operation; client gửi access token của `POST /auth/login` trong header `Authorization: Bearer`. Guest nhận `401 UNAUTHENTICATED`, Administrator nhận `403 MEMBER_ACCESS_REQUIRED`. Dữ liệu này riêng tư: không có endpoint nào trả sở thích của người khác.

- `GET /nutrition/dietary-preferences` trả `vegetarianType`, `avoid` và `dislike` (mỗi danh sách gồm `noneConfirmed` và `items` với `ingredientId`, `name`), ba sở thích tùy chọn, `onboardingStatus` và `aiPersonalization` (`eligible`, `missing`).
- `PUT /nutrition/dietary-preferences` lưu toàn bộ hồ sơ và chuyển `onboardingStatus` sang `COMPLETED` (dùng cho cả "Hoàn tất" Onboarding và "Lưu thay đổi" trong Cài đặt).
  - `vegetarianType` bắt buộc, một trong `VEGAN`, `LACTO`, `OVO`, `LACTO_OVO`.
  - `avoid` và `dislike` bắt buộc: có ít nhất một mục trong `items` hoặc `noneConfirmed = true`. Danh sách rỗng mà không xác nhận trả `400 VALIDATION_FAILED` với `errors[].field` là `avoid` hoặc `dislike` (BR-31). Thiếu `noneConfirmed` được hiểu là chưa xác nhận.
  - Mỗi danh sách tối đa 30 mục, mỗi tên 1–200 ký tự. Backend bỏ khoảng trắng thừa, gộp tên trùng (không phân biệt hoa/thường) và liên kết `ingredientId` khi tên trùng một nguyên liệu chuẩn đang hoạt động. Khi danh sách có mục, `noneConfirmed` được đưa về `false`.
  - Một tên ở cả hai danh sách trả `400 INGREDIENT_PREFERENCE_CONFLICT`.
  - Tùy chọn: `cuisinePreference` (tối đa 200 ký tự), `maxCookingTimeMinutes` (1–1440), `preferredDifficulty` (`EASY`, `MEDIUM`, `HARD`; giao diện hiển thị `HARD` là "Nâng cao").
  - Cập nhật hồ sơ không sửa Meal Plan đã lưu; chỉ các yêu cầu AI sau đó dùng dữ liệu mới.
- `POST /nutrition/dietary-preferences/onboarding/skip` trả `204`, chuyển `NOT_STARTED` sang `SKIPPED`; gọi lại hoặc gọi khi đã `COMPLETED` không đổi trạng thái.
- `POST /nutrition/dietary-preferences/onboarding/invitation` trả `200` với `{ "show": true | false }`. Chỉ trả `true` khi `onboardingStatus = NOT_STARTED` và lời mời chưa từng hiển thị; khi trả `true`, Backend ghi thời điểm vào `USER.onboarding_invited_at`. Backend khóa dòng `USER` của tài khoản trong lúc kiểm tra, nên các request song song chỉ có một request nhận `true`.
- `GET /nutrition/dietary-preferences/ingredient-suggestions?query=` trả tối đa 10 nguyên liệu chuẩn đang hoạt động (`id`, `name`, `ingredientGroup`); query rỗng trả danh sách rỗng.

Luồng Onboarding (AC-31.10): sau mỗi lần đăng nhập thành công của Member, Frontend gọi `POST .../onboarding/invitation` và chỉ chuyển tới trang Onboarding khi `show = true`. Vì vậy lời mời chỉ hiện một lần, kể cả khi Member rời questionnaire mà chưa "Hoàn tất" hay "Bỏ qua" (trạng thái vẫn `NOT_STARTED`). Trang Onboarding và trang Sở thích ăn uống trong Cài đặt vẫn mở thủ công được. Tài khoản tồn tại trước migration V7 được ghi `onboarding_invited_at` nên không bị hỏi và giữ nguyên `onboardingStatus`; tài khoản tạo sau đó bắt đầu ở `NOT_STARTED` với `onboarding_invited_at = NULL`. Luồng đăng nhập khác (ví dụ Google Login, Issue #8) cần gọi endpoint này sau khi đăng nhập thành công.

Cổng AI cá nhân hóa (AC-31.4–AC-31.6): endpoint AI gợi ý món hoặc tạo thực đơn tuần phải gọi `DietaryPreferenceService.requirePersonalizedAiEligible(userId)` trước mọi xử lý khác. Khi thiếu thông tin, request dừng với `409 DIETARY_PROFILE_INCOMPLETE` kèm `missing`; không gọi Gemini và không ghi Meal Plan. Cổng chỉ kiểm tra dữ liệu hồ sơ, không dựa vào `onboardingStatus`.

## 10. Tìm kiếm công thức công khai — FR-08

**Tích hợp thống kê FR-58:** `viewCount` trên danh sách và `statistics.viewCount` trên chi tiết là tổng số sự kiện xem đã lưu, không đổi ý nghĩa theo `viewPeriod`. Tham số `viewPeriod` chỉ chọn số lượt dùng để xếp hạng Most Viewed; card vẫn hiển thị tổng toàn thời gian.

Các khoảng là khoảng trượt theo Clock UTC của Backend, không phải ngày/tuần/tháng lịch: 24 giờ, 168 giờ, 720 giờ, hoặc lịch sử tới hiện tại. Mốc bắt đầu và kết thúc đều được tính; sự kiện sau hiện tại không tăng điểm Most Viewed. Nếu bằng số lượt trong khoảng, ưu tiên `published_at` mới hơn rồi `recipe_id` tăng dần để phân trang ổn định.

Most Active dùng tương tác 7 ngày; Trending dùng tương tác 72 giờ và suy giảm theo tuổi bài cùng bonus 48 giờ theo policy hiện có. Các truy vấn này dùng sự kiện `RECIPE_VIEW` của luồng ghi nhận mới, không lấy số do Frontend gửi. Kiểm chứng thứ tự, ranh giới thời gian và liên kết với luồng ghi nhận dùng các test SQL Server; test mapping với mock không chứng minh thứ tự truy vấn thực tế.

`GET /api/v1/recipes` là API đọc công khai có phân trang. Generated OpenAPI runtime là contract chi tiết. Endpoint nhận `keyword`, `page` (bắt đầu từ 0), `size` (1–50), `sort` (`NEWEST`, `MOST_LIKED`, `MOST_VIEWED`, `MOST_COMMENTED`, `MOST_ACTIVE` hoặc `TRENDING`) và `viewPeriod` (`ALL_TIME`, `LAST_24_HOURS`, `LAST_7_DAYS` hoặc `LAST_30_DAYS`).

Bộ lọc tùy chọn gồm `vegetarianType` (`VEGAN`, `LACTO`, `OVO`, `LACTO_OVO`), `dishCategory` (mã do API form-options trả về), `ingredientIds` lặp lại (tối đa 20 ID dương) và `maxTotalTimeMinutes` (số nguyên dương). Mọi tiêu chí đã gửi kết hợp theo AND. Nhiều `ingredientIds` cũng kết hợp theo AND: mỗi công thức phải chứa đủ mọi nguyên liệu chuẩn đã chọn. Tổng thời gian bằng thời gian chuẩn bị cộng thời gian nấu. Chỉ bài công thức công khai được trả về.

```http
GET /api/v1/recipes?keyword=canh&vegetarianType=VEGAN&dishCategory=SOUP&ingredientIds=12&ingredientIds=29&maxTotalTimeMinutes=30&sort=TRENDING&page=0&size=12
```

Dùng `GET /api/v1/recipes/form-options` để lấy mã/nhãn loại ăn chay và thể loại món hiện hành; dùng `GET /api/v1/recipes/ingredient-options?query=...` để tìm trong danh mục nguyên liệu đang hoạt động. Frontend không hard-code danh mục tùy chọn. Bộ lọc hoặc tham số phân trang ngoài giới hạn runtime trả `400` theo ProblemDetail chuẩn với `code` ổn định và lỗi field nếu có. Response danh sách giữ cấu trúc `items`, `page`, `size`, `totalElements`, `totalPages` hiện tại, đồng thời có `likes`, `dislikes`, `likePercentage` (`null` khi chưa có lượt bình chọn) và `viewCount` để hiển thị theo sort.

## 11. AI hỗ trợ soạn Recipe Post — FR-21

`POST /api/v1/recipes/ai-assistance` yêu cầu Bearer token của Expert đang hoạt động và gói Pro còn hiệu lực. Backend xác thực entitlement trước khi gọi Gemini. Generated OpenAPI runtime là hợp đồng chi tiết cho request/response và các lỗi.

Request chọn `contentType` là `DESCRIPTION` hoặc `INSTRUCTIONS`, gửi tiêu đề, mã danh mục, loại ăn chay, khẩu phần và các dòng nguyên liệu gồm `ingredientId`, `unitId`, `quantity`. Backend đối chiếu ingredient ID với danh mục đang hoạt động, yêu cầu Gemini trả các bước có cấu trúc và chỉ chấp nhận ingredient ID trong request. Nội dung không hợp lệ hoặc nhắc tên nguyên liệu có trong danh mục nhưng không được chọn bị từ chối.

Response trả `contentType` và `content` để Frontend điền trực tiếp vào trường có thể chỉnh sửa. API không tạo RecipePost hoặc bản nháp; chỉ thao tác đăng/lưu hiện có mới ghi công thức sau khi tác giả chủ động xác nhận. Free, Plus, hết hạn hoặc thiếu subscription nhận `403 AI_SUBSCRIPTION_REQUIRED` trước khi provider được gọi. Gemini thiếu cấu hình/lỗi nhận `503 AI_PROVIDER_UNAVAILABLE`; kết quả sai định dạng/giới hạn nhận `502 AI_RESPONSE_INVALID`. Các lỗi không ngăn tác giả tự nhập và đăng theo validation hiện hành.

Backend gọi model cố định `gemini-3.8-flash` qua Google Gen AI Java SDK; khóa chỉ được đọc từ `GEMINI_API_KEY` ở Backend. Không gửi API key xuống Frontend. Không ghi prompt thô vào log.

## 12. Hồ sơ thành viên — FR-23

Generated OpenAPI runtime là contract chi tiết; mục này ghi quy ước dùng chung (quyết định Q54–Q58).

- `GET /api/v1/members/{userId}` (công khai) trả `MemberProfileResponse`: `userId`, `displayName`, `avatarUrl` (null thì Frontend hiện ảnh mặc định), `bio` (null khi trống) và `joinedMonth` dạng `yyyy-MM` tính theo giờ Việt Nam từ `USER.created_at` (UTC). Không trả email, role, trạng thái tài khoản hay dữ liệu riêng tư (AC-23.2, BR-18). Hồ sơ của Member bị `LOCKED` vẫn công khai; ID không tồn tại hoặc của Administrator trả `404 MEMBER_PROFILE_NOT_FOUND` (Q55).
- `GET /api/v1/members/{userId}/recipes?page=0&size=12` (công khai) trả cùng dạng phân trang với `GET /api/v1/recipes`, chỉ gồm bài `PUBLISHED` của thành viên, mới nhất trước; `size` 1–50 (Q56).
- `GET /api/v1/members/{userId}/recipe-statistics` (công khai) trả `recipeCount`, `totalLikes` và `totalViews` tổng hợp trên tất cả bài `PUBLISHED` của tác giả, độc lập với phân trang. Không có bài thì các tổng bằng `0`; `totalLikes` chỉ đếm phản ứng `LIKE`.
- `GET /api/v1/me/profile` và `PUT /api/v1/me/profile` (cần Bearer token) luôn thao tác trên tài khoản trong phiên; không nhận `userId` (AC-23.8). Body `PUT`: `displayName` (bỏ khoảng trắng đầu cuối, 3–50 ký tự, không có ký tự điều khiển) và `bio` (bỏ khoảng trắng đầu cuối, tối đa 500 ký tự, rỗng lưu `NULL`) (Q58). Lỗi validation trả `400 VALIDATION_FAILED` kèm `errors[].field`. Tên mới hiện ngay trên hồ sơ và mọi bài của tác giả vì tác giả được đọc từ tài khoản hiện tại.
- `POST /api/v1/me/profile/avatar` (cần Bearer token, `multipart/form-data`, trường `file`) nhận JPEG, PNG hoặc WebP tối đa 2 MB; Backend đọc chữ ký đầu tệp và yêu cầu khớp `Content-Type` (AC-23.5, AC-23.7). Ảnh lưu qua `StorageClient` của FR-14 trong thư mục `avatars/` (Q54); ảnh đại diện cũ do hệ thống lưu bị xóa sau khi lưu thành công. Response là `MemberProfileResponse` mới.
- Administrator gọi `/me/profile` hoặc `/me/profile/avatar` nhận `403 MEMBER_ACCESS_REQUIRED`.
- Giới hạn multipart của máy chủ là 5 MB/tệp và 6 MB/request để ảnh công thức (≤ 5 MB) và ảnh đại diện (≤ 2 MB) tới được bước kiểm của service; tệp vượt giới hạn này trả `413 FILE_TOO_LARGE`.

## 13. Công thức liên quan — FR-47

Generated OpenAPI runtime là contract chi tiết. Hai API đều trả `{ "items": [...] }`, trong đó mỗi item dùng cùng DTO công thức công khai với `GET /api/v1/recipes`; thứ tự đã được Backend xác định, Frontend không xếp lại hoặc thay bằng dữ liệu mẫu.

- `GET /api/v1/recipes/{recipeId}/related?limit=6` công khai, không kiểm tra subscription và không gọi Gemini. `recipeId` dương; `limit` mặc định 6, từ 1 đến 12. Bài nguồn phải `PUBLISHED`; nguồn không khả dụng trả `404 RECIPE_NOT_FOUND`. Danh sách không chứa bài nguồn, bài ẩn/xóa hoặc ID trùng; ít ứng viên thì trả ít item, không có ứng viên trả `200` với `items: []`.
- Xếp hạng theo [FR-47](../requirements/srs/FUNCTIONAL-REQUIREMENTS.md#fr-47): khớp một phần, một điểm cho mỗi tiêu chí cùng thể loại/cùng loại chay/trùng nguyên liệu chính; tiếp theo là số nguyên liệu chuẩn trùng, ngày công khai và ID. Nguyên liệu chính có khối lượng gram lớn nhất; thiếu quy đổi hoặc nguyên liệu tùy chỉnh không có ID chuẩn thì bỏ tiêu chí nguyên liệu chính của công thức đó.
- `POST /api/v1/recipes/{recipeId}/related/ai?limit=6` không cần body; yêu cầu Bearer token của Customer/Expert đang hoạt động với Pro còn hiệu lực. Backend kiểm tra quyền trước khi gọi provider. Guest nhận `401`; Free/Plus/hết hạn nhận `403 AI_SUBSCRIPTION_REQUIRED`; Administrator không có quyền Member. Không áp quota lượt/ngày.
- Gemini nhận tối đa 50 ứng viên công khai cùng loại chay với nguồn, phân bổ theo thể loại, và chỉ trả ID. Backend từ chối cấu trúc sai, quá giới hạn hoặc ID ngoài tập bằng `502 AI_RESPONSE_INVALID`; loại ID trùng và đọc lại trạng thái/nội dung hiện tại sau khi provider trả về. Bài bị ẩn/xóa hoặc đổi sang loại chay khác không được gửi xuống FE. Không có ứng viên thì trả rỗng và không gọi Gemini.
- Lỗi cấu hình/provider/timeout trả `503 AI_PROVIDER_UNAVAILABLE`; API chỉ đọc, không tạo RecipePost hoặc ghi Lịch ăn. FE chỉ gọi POST sau thao tác chủ động, giữ gợi ý thường khi AI lỗi và bỏ phản hồi cũ khi chuyển bài. Khóa vẫn dùng `GEMINI_API_KEY` ở Backend và cơ chế Compose hiện có.
- Tích hợp FR-11: `AiUsageEvent` mang `userId`, `feature=FR-47`, `occurredAt` và các token count thực tế từ provider; thiếu metadata giữ `null`, không suy đoán bằng 0. Event chỉ phát sau khi phản hồi hợp lệ. Consumer lưu trữ tối đa 90 ngày/báo cáo do Issue #17 triển khai; FR-47 chưa cung cấp consumer lưu trữ. Không đưa prompt, nội dung công thức hoặc API key vào event/log.

## 14. Gợi ý món và menu tuần AI — FR-34

Các API yêu cầu Bearer token của Member đang hoạt động và hồ sơ FR-31 đủ ba nhóm thông tin. Backend lọc ứng viên công khai theo loại ăn chay trong hồ sơ và nguyên liệu cần tránh trước khi gửi catalog rút gọn tới Gemini. Lựa chọn Gemini phải thuộc ID trong catalog; Backend tải lại các bài công khai hiện hành trước khi trả dữ liệu. Không gửi nội dung hồ sơ riêng tư hoặc prompt người dùng vào telemetry/log.

| Thao tác | API | Quyền và kết quả |
|---|---|---|
| Gợi ý 3–5 món theo nguyên liệu | `POST /api/v1/recipes/ai/recommendations` | Body `{ "ingredientIds": [12, 29] }`; nguyên liệu được lấy từ `GET /api/v1/recipes/ingredient-options?query=...`. Trả `{ "items": [{ "recipe": RecipePostResponse, "reason": "..." }] }`. Công thức khớp nhiều nguyên liệu đã chọn được ưu tiên. Free Member được gọi theo FR-10. |
| Tạo menu tuần xem trước | `POST /api/v1/recipes/ai/weekly-menu` | `ingredientIds` tùy chọn (tối đa 20 ID). Bỏ field hoặc gửi mảng rỗng để AI chọn trên catalog công khai an toàn theo hồ sơ; khi có ID, candidate recipes phải chứa ít nhất một nguyên liệu đã chọn. Yêu cầu Plus hoặc Pro còn hiệu lực. Trả 21 `slots`, theo thứ tự ngày 0–6 và sáng/trưa/tối; có thể lặp công thức nếu catalog ít nhưng hai vị trí liên tiếp không trùng ID. Chỉ tạo preview, không lưu Meal Plan. |
| Xác nhận thêm menu vào Lịch ăn | `POST /api/v1/meal-plans/{weekStartDate}/ai-menu` | Yêu cầu Plus hoặc Pro còn hiệu lực, Bearer token và ngày Thứ Hai. Body gồm đúng 21 mục `slots`, mỗi mục `{ "dayOffset": 0, "mealType": "BREAKFAST", "recipeId": 123 }`. Backend giữ các mục hiện có và thêm menu trong một transaction. Trả tuần đã cập nhật. |

API gợi ý và preview trả `409 DIETARY_PROFILE_INCOMPLETE` khi hồ sơ chưa đủ; weekly preview và thao tác xác nhận trả `403 AI_SUBSCRIPTION_REQUIRED` nếu tài khoản không có Plus hoặc Pro còn hiệu lực. Không đủ ứng viên để tạo menu trả `409 AI_MENU_UNAVAILABLE`; lỗi provider trả `503 AI_PROVIDER_UNAVAILABLE`; phản hồi AI sai hoặc chứa ID ngoài catalog trả `502 AI_RESPONSE_INVALID`. Khi xác nhận, công thức đã ẩn/xóa trả `404 RECIPE_NOT_FOUND`; nếu cùng recipe đã có trong đúng ngày/bữa thì trả `409 MEAL_PLAN_RECIPE_DUPLICATE`. Hai trường hợp đều rollback toàn bộ, không ghi một phần. Chỉ gọi API xác nhận sau khi Member chủ động bấm áp dụng.

Gợi ý món, preview thực đơn và hướng dẫn nấu chi tiết phát `AiUsageEvent` với `feature=FR-34`. Đối với hướng dẫn nấu, interface hiện tại của Gemini không cung cấp token metadata nên các trường token được ghi `null`, không suy đoán thành `0`. Issue #17 tiếp tục sở hữu consumer lưu trữ/báo cáo telemetry. Không có bảng preview mới và không tạo công thức mới.

| Tạo hướng dẫn nấu chi tiết cho công thức công khai | `POST /api/v1/recipes/ai/cooking-guide` | Bearer token Member đang hoạt động; body `{ "recipeId": 123 }`. Yêu cầu hồ sơ sở thích FR-31 đủ; Backend tải lại Recipe Post đang công khai và kiểm tra loại ăn chay/nguyên liệu cần tránh trước khi gọi Gemini. Member Free/Plus/Pro được dùng như quyền gợi ý món UC-34.1. Trả `recipeId`, `recipeTitle`, 6–10 `steps` và `aiNotice`; không tạo/sửa Recipe Post hoặc Meal Plan, không lưu nội dung hướng dẫn. Công thức không khả dụng/không phù hợp trả `404`; thiếu hồ sơ trả `409`; AI trả sai ràng buộc `502 AI_RESPONSE_INVALID`; provider lỗi/timeout `503 AI_PROVIDER_UNAVAILABLE`.
## 15. AI lập thực đơn dinh dưỡng — FR-36

Generated `/v3/api-docs` là hợp đồng runtime chi tiết cho hai endpoint dưới đây. Cả hai yêu cầu Bearer token của Member đang hoạt động, gói Plus hoặc Pro còn hiệu lực, trạng thái dinh dưỡng `ELIGIBLE` và hồ sơ nhân trắc học FR-35 hợp lệ. FR-31 onboarding không bắt buộc; ràng buộc an toàn đã khai báo vẫn được áp dụng.

- `POST /api/v1/meal-plans/ai-menu/previews` nhận `weekStartDate` (Thứ Hai), `mode` (`PROFILE` hoặc `THIS_WEEK`), `goal` tùy chọn, `preferredIngredientIds` và `avoidedIngredientIds` tùy chọn. PROFILE dùng mục tiêu FR-35 đã lưu; THIS_WEEK dùng câu trả lời chỉ trong request hiện tại. Response chỉ có các ngày từ hôm nay đến Chủ nhật của tuần hiện tại; mỗi bữa có thể chứa nhiều công thức, mỗi công thức một khẩu phần, cùng cover, 9 giá trị dinh dưỡng mỗi khẩu phần và lý do chọn. Ngày đã qua không có món và UI ghi rõ AI không đề xuất; tuần trước hoặc tuần tương lai không sinh món. Preview không được lưu và không sửa Meal Plan.
- Backend chỉ chọn tối đa 50 công thức `PUBLISHED`, có mọi nguyên liệu ánh xạ gram và đủ 9 chỉ tiêu dinh dưỡng. Dị ứng/nguyên liệu cần tránh từ FR-31 và `avoidedIngredientIds` của tuần này là điều kiện loại trừ cứng trước khi gọi Gemini; mọi công thức chứa một trong các nguyên liệu đó không thuộc pool. `preferredIngredientIds` chỉ là ưu tiên mềm trong số công thức còn hợp lệ và không thể ghi đè điều kiện tránh. Gemini chỉ trả ID; Backend xác minh ID, tính lại dinh dưỡng và từ chối kết quả sai, ngoài pool hoặc lặp cùng món ở hai bữa liền kề. Pool có thể lặp công thức khác vị trí liền kề nếu không đủ 21 món khác nhau.
- `PUT /api/v1/meal-plans/ai-menu` nhận `weekStartDate`, mode và các giá trị tuần cần để tái xác minh, cùng danh sách món được chọn (`mealDate`, `mealType`, `recipeId`) trong khoảng hôm nay đến Chủ nhật hiện tại. Khi gọi API, Backend tái kiểm tra Plus hoặc Pro, eligibility, hồ sơ, công thức công khai, pool dinh dưỡng, ràng buộc an toàn và giới hạn ngày; trong một transaction, chỉ thay các entries từ hôm nay đến hết tuần hiện tại, giữ nguyên entries của ngày đã qua. Bất kỳ lỗi nào rollback và giữ nguyên lịch cũ. FE phải cảnh báo rõ phạm vi cập nhật trước khi gửi PUT.
- Backend không gọi bước xếp hạng Gemini và trả preview rỗng nếu khoảng tuần đang yêu cầu không giao với hôm nay đến Chủ nhật hiện tại; thao tác áp dụng các món ngoài khoảng ngày này bị từ chối.
- Mục tiêu chỉ điều hướng lựa chọn món, không làm thay đổi mục tiêu số FR-35, không tự suy ra thâm hụt/thặng dư năng lượng và không đảm bảo kết quả sức khỏe. Dữ liệu câu trả lời tuần không được lưu/tái sử dụng. Không gửi số đo nhân trắc học hoặc mục tiêu cá nhân chi tiết vào telemetry; không ghi prompt hay câu trả lời cá nhân vào log.
- Nếu chưa xác nhận trạng thái dinh dưỡng, API trả `403 NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED`; nếu người dùng đã xác nhận không đủ điều kiện, trả `403 NUTRITION_ELIGIBILITY_INELIGIBLE`. Hồ sơ nhân trắc học chưa lưu trả `409 NUTRITION_PROFILE_REQUIRED`.

## 16. Chatbot hỏi đáp theo ngữ cảnh — FR-51

`POST /api/v1/recipes/ai/chat` phục vụ cả Guest và Member đang hoạt động trên cùng một chatbot. Member gửi Bearer token; Guest gửi thêm `anonymousViewerId` dạng UUID lấy từ session cookie cùng origin Frontend (`mamxanh.recipe-viewer`, dùng chung định danh ẩn danh hiện có của FR-58). Không dùng cookie xác thực hoặc bật credentials xuyên origin. Endpoint nhận `contextType` (`GENERAL` hoặc `RECIPE`), `question` (tối đa 1.000 ký tự), tối đa 8 lượt `history` gần nhất (`speaker`: `USER` tối đa 1.000 ký tự, `ASSISTANT` tối đa 8.000 ký tự), và `recipeId` khi dùng ngữ cảnh `RECIPE`. Câu trả lời được Backend kiểm tra tối đa 8.000 ký tự trước khi trả thành công, để Frontend gửi lại đầy đủ ở lượt tiếp theo mà không cắt mất nội dung. Giới hạn này do Backend thực thi, không phụ thuộc provider có tuân thủ length constraint của schema hay không. `GENERAL` không nhận `recipeId`; `RECIPE` bắt buộc có ID dương.

Ví dụ body cho ngữ cảnh công thức:

```json
{
  "contextType": "RECIPE",
  "recipeId": 123,
  "question": "Có thể thay nấm bằng nguyên liệu nào?",
  "history": []
}
```

Backend tải RecipePost hiện hành theo ID và chỉ dùng bài đang công khai; không nhận nội dung công thức do Frontend gửi. Context gồm nội dung công thức, nguyên liệu/định lượng, dinh dưỡng hiện có và ràng buộc ăn chay/nguyên liệu cần tránh của Member nếu đã khai báo. Khi câu hỏi yêu cầu dữ liệu cá nhân về BMI hoặc năng lượng, chatbot không tự tải hồ sơ sức khỏe vào prompt; Member cần dùng luồng hồ sơ dinh dưỡng đã có.

Response gồm `contextType`, `recipeId` (null với ngữ cảnh chung), `answer` và `recommendations` (mảng rỗng nếu không tìm công thức). Khi người dùng tìm món/công thức, Backend tải tối đa 50 Recipe Post công khai, lọc trường phái ăn chay và nguyên liệu cần tránh đã khai báo rồi gửi danh sách giới hạn cho Gemini; Gemini chỉ được chọn tối đa 4 ID trong danh sách. Backend xác minh ID và trạng thái công khai lần nữa trước khi trả các thẻ bài viết gồm `id`, `title`, `description`, `coverUrl`, `totalTimeMinutes`, `difficulty` và `reason`. FE dùng `id` để mở trang `/cong-thuc/id/{id}`; không dùng URL do Gemini tạo. Nếu không tìm được bài phù hợp, `recommendations` rỗng và `answer` nói rõ không có kết quả. Nội dung công thức mới hoặc biến tấu chỉ được trả trong `answer` khi người dùng yêu cầu rõ; đây là bản nháp AI trong hội thoại, không lưu hoặc đăng.

Ví dụ phần response cho yêu cầu tìm món:

```json
{
  "contextType": "GENERAL",
  "recipeId": null,
  "answer": "Mình tìm được một số bài công thức phù hợp:",
  "recommendations": [
    {
      "id": 123,
      "title": "Đậu hũ sốt cà chua",
      "description": "Món chay dễ làm cho bữa cơm gia đình.",
      "coverUrl": "https://storage.example/recipe-cover.jpg",
      "totalTimeMinutes": 25,
      "difficulty": "EASY",
      "reason": "Cách làm đơn giản và thời gian chuẩn bị ngắn."
    }
  ]
}
```

Gemini phải phân biệt dữ kiện ghi trong RecipePost với gợi ý/ước tính của AI, không bịa thông tin còn thiếu, tôn trọng loại ăn chay và không chẩn đoán hoặc kê phác đồ điều trị. API chỉ đọc; không lưu lịch sử hội thoại hay thay đổi dữ liệu nghiệp vụ. Frontend giữ tối đa lịch sử cần gửi trong state của phiên hiện tại. Backend chỉ phát telemetry token `AiUsageEvent` sau phản hồi hợp lệ; không đưa câu hỏi, lịch sử, context công thức hoặc câu trả lời vào telemetry/log.

Lỗi request trả `400`; bài không tồn tại hoặc không còn công khai trả `404`; phản hồi Gemini sai/thiếu trả `502 AI_RESPONSE_INVALID`; Gemini lỗi hoặc timeout trả `503 AI_PROVIDER_UNAVAILABLE`. Backend giới hạn thời gian chờ Gemini theo NFR-18. Guest chịu giới hạn trượt 10 request/phút độc lập theo UUID và peer IP; đổi cookie không vượt được giới hạn IP, đổi IP không vượt được giới hạn cookie. Vượt ngưỡng trả `429 AI_GUEST_RATE_LIMITED` cùng `Retry-After` trước khi gọi chatbot/provider. Bộ đếm có giới hạn bộ nhớ, hết hiệu lực sau một phút, lưu hash SHA-256 và không tin `X-Forwarded-For`. Đây là bộ đếm trong một tiến trình cho baseline Backend hiện tại; không bảo đảm giới hạn chung khi chạy nhiều replica. Member không dùng bộ đếm Guest hoặc daily quota. Bearer sai/hết hạn trả `401`, tài khoản bị khóa trả `403`; không hạ request có Bearer lỗi thành Guest. Guest không đọc hồ sơ sở thích hoặc sức khỏe tài khoản; `AiUsageEvent.userId` là null cho Guest, chỉ chứa metadata token. Generated `/v3/api-docs` là contract chi tiết cho request, response, validation và security.

Phần Guest là prerequisite được tích hợp trong FR-10/Issue #16 để đáp ứng AC-10.1; không tự xác nhận Issue FR-02 đã được nghiệm thu. Khi tiếp tục FR-02, tái sử dụng endpoint và bộ đếm này, không tạo chatbot hoặc daily quota riêng.

SDK Gemini tắt retry nội bộ (một attempt mỗi lần gọi HTTP). Toàn thao tác dùng chung deadline 9 giây, bao gồm backoff và tối đa hai lần gọi HTTP. Chỉ thử lại một lần khi provider trả sớm HTTP 502/503/504, sau khoảng nghỉ 1–1,25 giây và khi còn ít nhất 2 giây cho lần gọi tiếp; không retry HTTP 429, lỗi validation/quyền, phản hồi không hợp lệ hoặc timeout. Đây là retry kỹ thuật có giới hạn thời gian, không thay quota hoặc entitlement. Model dùng chung hiện tại là `gemini-3.8-flash` theo baseline Confirmed; thử nghiệm Flash-Lite đã kết thúc ngày 27/10/2026 theo yêu cầu bàn giao kiểm thử. Provider lỗi/timeout trả `503 AI_PROVIDER_UNAVAILABLE`; Chatbot nhận phản hồi thiếu, bị cắt hoặc không hợp lệ trả `502 AI_RESPONSE_INVALID`. Nếu provider trả HTTP 429, Chatbot vẫn dùng 503 với hướng dẫn dịch vụ đang tạm giới hạn yêu cầu; không trả lỗi 429 dành cho bộ đếm Guest và không gợi ý đăng ký để vượt quota Google. Quota của project Google tách biệt với entitlement Free/Plus/Pro, không phải daily quota do ứng dụng áp dụng. Chỉ phản hồi đã qua validation mới phát usage event thành công. Log SDK chỉ chứa operation, model, nhóm lỗi, mã HTTP provider (nếu có) và thời gian; không ghi message/stack trace của SDK, raw prompt, câu trả lời hoặc API key. Cấu hình timeout không phải bằng chứng đạt thời gian trả lời thành công của NFR-03; hiệu năng provider cần kiểm tra riêng.

### Quy ước lỗi entitlement — FR-10

Chatbot áp dụng cùng ranh giới tính năng: yêu cầu rõ ràng lập thực đơn tuần kiểm tra Plus hoặc Pro; hỗ trợ viết mô tả/bước làm cho Recipe Post kiểm tra Expert cùng Pro. Việc điều hướng không gọi Gemini: Guest nhận 401, Member thiếu gói nhận 403 `AI_SUBSCRIPTION_REQUIRED` với `requiredTier` và `upgradeUrl`, tài khoản đủ quyền nhận response 200 cùng `workflowUrl` nội bộ (`/ke-hoach?view=for-you` hoặc `/dang-cong-thuc`). Đây chỉ là hướng dẫn tới chức năng sở hữu workflow, không phải kết quả AI hoặc bằng chứng đã kiểm tra xong hồ sơ/chấp nhận áp dụng. Hỏi đáp chung và yêu cầu viết/biến tấu bản nháp công thức trong phạm vi FR-51 vẫn được giữ.

`history[].recommendedRecipeIds` là field tùy chọn, tối đa bốn số nguyên dương, chỉ thuộc lượt ASSISTANT. FE gửi ID của các thẻ đã hiển thị, không tự thêm danh sách vào lượt USER. Backend xem đây là gợi ý loại trừ không đáng tin cậy khi yêu cầu món khác; không dùng ID này để cấp quyền, tải nội dung riêng hoặc xác thực công thức còn công khai. Tìm món tiếp nhận diện ngữ cảnh câu hỏi, lọc thể loại trước phân trang và kiểm tra tối đa bốn trang 50 bài để lấy tối đa 50 ứng viên hợp lệ. Thẻ kết quả được kiểm tra lại theo trạng thái hiện tại; thông báo rỗng chỉ nói về phần danh mục đã kiểm tra ở lượt này. Client cũ bỏ qua hai field tùy chọn vẫn gửi được request cũ, nhưng không có đủ ID thẻ để tránh lặp chính xác.

Các guard hiện có vẫn dùng `403 AI_SUBSCRIPTION_REQUIRED`. ProblemDetail bổ sung `requiredTier` (`PLUS` hoặc `PRO`) và `upgradeUrl` (`/goi-ai`); `detail` hướng dẫn đúng gói tối thiểu. Pro cho FR-21 vẫn đồng thời yêu cầu vai trò Expert. FR-47 (AI gợi ý món liên quan/biến tấu) và AI đề xuất điều chỉnh dinh dưỡng FR-37 tiếp tục yêu cầu Pro. Plus/Pro cho FR-36 vẫn yêu cầu eligibility và hồ sơ hợp lệ. Frontend giữ lại `requiredTier` khi chuẩn hóa lỗi và hiển thị đúng tên gói AI PLUS hoặc PRO trong cả thông báo lẫn liên kết nâng cấp. Không dùng lời nhắc “Plus hoặc Pro” ở lỗi thiếu quyền tính năng. Nếu response cũ thiếu metadata, giao diện dùng mức tối thiểu đã xác định của chức năng. Backend quyết định quyền từ Subscription đang hiệu lực, không từ thông tin gói do client khai. Preview và apply menu tuần kiểm tra lại Plus hoặc Pro ở từng bước; lỗi không đổi Subscription hoặc gọi Gemini. Nội dung tính toán dinh dưỡng/gợi ý thông thường không bị đưa vào paywall bởi thay đổi này.

`GET /api/v1/subscriptions/my-subscription` hiển thị quyền trả phí theo cùng cửa sổ `starts_at <= now < ends_at` với guard AI. Gói bắt đầu trong tương lai chưa mở tính năng: response dùng Free hiện tại và giữ nguyên bản ghi gói tương lai; không tự hủy hoặc đánh dấu gói tương lai là hết hạn.

## 17. Theo dõi thành viên — FR-59

Generated OpenAPI runtime là contract chi tiết; mục này ghi quy ước dùng chung. Quan hệ có hướng lưu trong `USER_FOLLOW` (có từ V1), người theo dõi luôn là tài khoản trong phiên (BR-75).

- `GET /api/v1/members/{userId}/follow` (công khai) trả `FollowStatusResponse`: `userId`, `following` (người xem có đang theo dõi không; Guest luôn `false`), `followerCount`, `followingCount`. Hai bộ đếm chỉ tính tài khoản `ACTIVE` (AC-59.7). Hồ sơ Member bị `LOCKED` vẫn đọc được (Q55).
- `PUT /api/v1/members/{userId}/follow` và `DELETE /api/v1/members/{userId}/follow` (cần Bearer token) đều idempotent và trả `FollowStatusResponse` mới: gọi lặp không tạo bản ghi trùng hay làm sai bộ đếm (AC-59.4, AC-59.5). Chỉ Customer/Expert đang hoạt động được gọi; Administrator nhận `403 MEMBER_ACCESS_REQUIRED`, tự theo dõi nhận `403 FOLLOW_SELF_NOT_ALLOWED`, theo dõi Member `LOCKED` nhận `409 FOLLOW_TARGET_INACTIVE`.
- `GET /api/v1/members/{userId}/followers` và `GET /api/v1/members/{userId}/following` (cần Bearer token) trả `PageResponse` gồm `userId`, `displayName`, `avatarUrl`; không có email hay dữ liệu đăng nhập (AC-59.6). Chỉ gồm tài khoản `ACTIVE`, mới theo dõi gần đây trước; `page` từ 0, `size` 1–50 (mặc định 20).
## 18. Bình luận và reply lồng nhiều cấp — FR-46

Generated OpenAPI runtime là contract chi tiết; mục này ghi quy ước dùng chung. Dữ liệu lưu trong `COMMENT` (có từ V1); `CK_COMMENT_depth` và `FK_COMMENT_PARENT` giữ cây tối đa 5 cấp và reply cùng bài với cha (BR-66).

- `GET /api/v1/recipes/{recipeId}/comments?page=0&size=10` (công khai) chỉ cho bài `PUBLISHED` (khác thì `404 RECIPE_NOT_FOUND`). Trả `RecipeCommentPageResponse`: `content` là các luồng gốc, mới nhất trước; mỗi `CommentResponse` có `commentId`, `parentCommentId`, `depth`, `content`, `author` (`userId`, `displayName`, `avatarUrl`), `deleted`, `edited`, `createdAt` và `replies` (cũ trước, đúng thứ tự hội thoại). `totalElements` đếm luồng gốc còn hiển thị, `commentCount` đếm bình luận và reply chưa xóa; `size` 1–50.
- Bình luận đã xóa còn reply hiển thị được trả về với `deleted = true`, `content` và `author` là `null` (tombstone, AC-46.2); đã xóa và không còn reply nào hiển thị thì không trả về (AF-46.3).
- `POST /api/v1/recipes/{recipeId}/comments` (cần Bearer token) body `{ "content", "parentCommentId"? }`, trả `201` với bình luận mới. Chỉ Customer/Expert đang hoạt động; Administrator nhận `403 MEMBER_ACCESS_REQUIRED`. `content` bỏ khoảng trắng đầu cuối, 1–1.000 ký tự, sai thì `400 VALIDATION_FAILED` với `errors[].field = content`. Trả lời bình luận cấp 5 được gắn vào cha cấp 4 nên vẫn ở cấp 5 (AC-46.1); Frontend chèn `@Tên` vào đầu nội dung.
- `PUT /api/v1/comments/{commentId}` body `{ "content" }` và `DELETE /api/v1/comments/{commentId}` (cần Bearer token): chỉ chủ bình luận; người khác nhận `403 COMMENT_EDIT_NOT_ALLOWED`. Sửa thì `edited = true`. Xóa trả `204`, gọi lặp an toàn, luôn là xóa mềm (`is_deleted`) vì `REPORT`/`NOTIFICATION` có thể tham chiếu bình luận.
- Nội dung là văn bản thuần: Backend lưu nguyên văn, Frontend hiển thị như text nên mã HTML/JavaScript không được thực thi (AC-46.3).


## 19. Kết quả thanh toán đã xác minh — FR-13 / Issue #142

Generated OpenAPI là contract chi tiết; endpoint mới không thay đổi các route thanh toán hiện có.

| Endpoint | Quyền và hành vi |
| --- | --- |
| `POST /api/v1/subscriptions/checkout` | Member đang hoạt động; body chỉ có `tier: PLUS` hoặc `PRO`; Backend quyết định giá. HTTP 201 trả `orderCode`, `amountVnd`, `tier`, `checkoutUrl`, `qrCode`. |
| `GET /api/v1/subscriptions/transactions/{orderCode}` | Chỉ chủ đơn; 200 trả trạng thái đã xác minh và subscription, `Cache-Control: no-store`. Mã đơn là chuỗi số dương trong giới hạn integer an toàn của JavaScript. |
| `POST /api/v1/payments/webhook` | Không yêu cầu Bearer; bắt buộc chữ ký HMAC hợp lệ và dữ liệu tiền tệ/số tiền hợp lệ trước khi xử lý. |

Response kết quả đơn gồm `orderCode` (string), `tier`, `amountVnd`, `status`, `paidAt`, `activated`, `subscription`. `activated` chỉ đúng khi subscription liên kết với chính đơn còn hiệu lực và gói hiện tại tương ứng; `PAID` là lịch sử thanh toán, không đồng nghĩa subscription còn hiệu lực mãi. Subscription gồm `tier`, `status`, `active`, `startsAt`, `endsAt`, `unlockedFeatures`.

- Đơn còn `PENDING` được Backend đối soát qua API payOS có credential server; kiểm tra chữ ký response, mã đơn, số tiền và số tiền đã trả trước khi kích hoạt. Webhook và đối soát dùng chung transaction/locking, xử lý lặp không cộng thêm kỳ.
- `404 PAYMENT_TRANSACTION_NOT_FOUND` dùng chung cho đơn không tồn tại và đơn của người khác; `403` cho role không phù hợp hoặc tài khoản LOCKED. Guest nhận 401.
- `503 PAYMENT_UNAVAILABLE` khi thiếu cấu hình hoặc provider lỗi; `502 PAYMENT_RESPONSE_INVALID` khi dữ liệu provider không xác minh được; webhook sai chữ ký/số tiền/currency trả 400. Các lỗi không được tạo quyền trả phí.
- Lịch sử giao dịch giữ các trường cũ và bổ sung `tier` (nullable với giao dịch cũ có số tiền không khớp giá gói). Checkout không nhận return/cancel URL tùy ý từ trình duyệt; các URL thuộc cấu hình Backend.
- Trang kết quả phải hỏi Backend, không suy luận thành công từ URL quay về. Frontend chờ có giới hạn và cho kiểm tra lại; tài khoản giữ quyền hiện tại khi đơn còn chờ hoặc bị hủy. Không chấp nhận mock checkout/chữ ký giả ở runtime.

Nguồn kỹ thuật payOS: [Payment API](https://payos.vn/docs/api/), [xác minh chữ ký](https://payos.vn/docs/tich-hop-webhook/kiem-tra-du-lieu-voi-signature/), [Return URL](https://payos.vn/docs/du-lieu-tra-ve/return-url/).

## 20. Danh mục và chi tiết nguyên liệu công khai — FR-62

Generated OpenAPI là contract chi tiết. Guest và Member có thể đọc danh mục và chi tiết mà không cần đăng nhập; request đã mang Bearer token vẫn tuân theo kiểm tra token hiện hành.

| Endpoint | Cách tích hợp |
| --- | --- |
| `GET /api/v1/ingredients` | Danh sách phân trang chỉ gồm catalog `ACTIVE`, có tìm kiếm và lọc nhóm; mỗi mục trả `ingredientId`, `name`, `ingredientGroup`, `imageUrl`. FE tải các trang để duyệt/tìm/chọn nhiều nguyên liệu. |
| `GET /api/v1/ingredients/groups` | Tên và số nguyên liệu đang hoạt động trong từng nhóm. |
| `GET /api/v1/ingredients/{ingredientId}` | Chi tiết nguyên liệu đang hoạt động, bổ sung `nutrition` với giá trị trên 100 g. ID không tồn tại và nguyên liệu `INACTIVE` đều trả `404 INGREDIENT_NOT_FOUND`; ID không hợp lệ trả `400 VALIDATION_FAILED`. |
| `GET /api/v1/ingredients/{ingredientId}/units` | Danh sách đơn vị công thức đang hỗ trợ cho nguyên liệu active: `mg/g/kg` luôn có; `ml/L/tsp/tbsp/cup` và `quả/củ/miếng` chỉ có khi conversion active. Trả `id`, `code`, `name`, `dimension`, `gramsPerUnit`, `approximate`. Dùng dữ liệu này cho bộ chọn đơn vị trong công thức và công cụ quy đổi hai chiều ở `/nguyen-lieu/:ingredientId`; Backend vẫn xác thực lại khi lưu công thức. |

`nutrition` giữ các chỉ tiêu và đơn vị sau; không nhân theo khẩu phần món hoặc tự suy diễn tỷ lệ quy đổi:

| Trường | Đơn vị trên 100 g |
| --- | --- |
| `energyKcalPer100g` | kcal |
| `proteinGramsPer100g`, `carbohydrateGramsPer100g`, `totalFatGramsPer100g`, `fiberGramsPer100g` | g |
| `calciumMgPer100g`, `ironMgPer100g`, `zincMgPer100g` | mg |
| `vitaminB12MicrogramsPer100g` | µg, giữ tối đa bốn chữ số thập phân |

- Từng chỉ tiêu có thể là `null`, nghĩa là chưa biết. FE ghi rõ chưa có dữ liệu; số `0` là giá trị đã có và phải hiển thị 0. Khi tất cả chỉ tiêu là `null`, hiển thị trạng thái chưa có dinh dưỡng.
- `imageUrl` lấy từ catalog. URL trống hoặc tải lỗi dùng trạng thái chưa có ảnh minh họa, không gán ảnh nguyên liệu khác.
- Response công khai không có tên/URL nguồn, ngày tham khảo, trạng thái catalog hoặc cờ hỗ trợ tính dinh dưỡng. Các trường đó tiếp tục thuộc API quản trị.
- Trang `/nguyen-lieu/:ingredientId` dùng API chi tiết; từ trang này mở `/kham-pha` với một ingredient ID. Danh mục vẫn chọn nhiều ID và tìm công thức theo semantics AND của FR-08.
- Lỗi dịch vụ/kết nối cho phép thử lại; `404` cho biết nguyên liệu không còn khả dụng và cung cấp đường về danh mục.
- Lưới bài viết trên chi tiết gọi `GET /api/v1/recipes/discovery` với `ingredientIds` bằng đúng ID đang xem, `size=9`, `page` tăng từ 0 và `sort=MOST_LIKED` hoặc `NEWEST`; Xem thêm theo `hasNext`, không tự hiển thị tổng số bài khi response không cung cấp tổng.
- Các nhãn loại ăn chay dùng `vegetarianTypes` của `GET /api/v1/recipes/form-options`; chọn nhãn gửi thêm `vegetarianType` nhưng vẫn giữ `ingredientIds`. Đây là nhãn của công thức; API không khai báo độ phù hợp chế độ ăn của riêng nguyên liệu.

## 21. Xử lý báo cáo của Administrator — FR-28

Generated OpenAPI runtime là contract chi tiết; mục này ghi quy ước dùng chung. Mọi endpoint dưới `/api/v1/admin/reports` cần Bearer token của Administrator; Guest nhận `401`, Customer/Expert nhận `403 ACCESS_DENIED` (NFR-09). Chỉ báo cáo Recipe Post (`REPORT.recipe_id`) nằm trong hàng đợi này; báo cáo bình luận thuộc FR-48.

| Endpoint | Cách tích hợp |
| --- | --- |
| `GET /api/v1/admin/reports?status=&recipeId=&page=&size=` | `PageResponse<AdminReportResponse>`, mới nhất trước. `status` là `OPEN`, `IN_REVIEW`, `RESOLVED` hoặc bỏ trống. `recipeId` (từ 1, tùy chọn) chỉ lấy báo cáo của một bài để xem từng phản ánh trong nhóm (AC-30.4). `page` từ 0, `size` 1–100 (mặc định 20). |
| `GET /api/v1/admin/reports/groups?status=&query=&sort=&page=&size=` | `PageResponse<AdminReportGroupResponse>`: mỗi bài có báo cáo là một nhóm (AC-30.4). Có cùng `status`, `page`, `size` như danh sách; phân trang đếm theo bài, không theo báo cáo. `query` (tối đa 200 ký tự, bỏ khoảng trắng đầu cuối, không phân biệt hoa thường) giữ các bài có tiêu đề, tên hoặc email tác giả chứa chuỗi này; ký tự `%`, `_`, `[` được so khớp nguyên văn. `sort` là `NEWEST` (mặc định, bài có báo cáo khớp bộ lọc mới nhất đứng trước) hoặc `MOST_REPORTED` (bài có nhiều báo cáo khớp bộ lọc nhất đứng trước, bằng nhau thì bài có báo cáo mới hơn đứng trước) (UC-06.1). Sai `sort` hoặc `query` quá dài trả `400 VALIDATION_FAILED`. |
| `GET /api/v1/admin/reports/status-counts` | Số báo cáo ở từng trạng thái: `open`, `inReview`, `resolved`. |
| `GET /api/v1/admin/reports/{reportId}` | Chi tiết một báo cáo, `404 REPORT_NOT_FOUND` khi không có. |
| `POST /api/v1/admin/reports/{reportId}/claim` | Không có body. Chuyển `OPEN` sang `IN_REVIEW` và ghi người gọi vào `handledBy` (AC-28.1). Báo cáo đã có người tiếp nhận hoặc đã giải quyết trả `409 REPORT_STATUS_CONFLICT`. Hai Administrator tiếp nhận cùng lúc thì đúng một người thành công vì Backend khóa dòng `REPORT`. |
| `POST /api/v1/admin/reports/{reportId}/resolve` | Body `{ "decision", "reason" }`. Chỉ Administrator đang giữ báo cáo `IN_REVIEW` được gọi; trả báo cáo `RESOLVED`. |

`AdminReportResponse` gồm `id`, `status`, `reasonCode`, `description`, `createdAt`, `updatedAt`, `recipe` (`id`, `title`, `status`), `reporter` và `author` (`id`, `displayName`, `email`, `role`, `accountStatus`), `handledBy` (`id`, `displayName`, `null` khi chưa ai tiếp nhận), `decision`, `decisionReason`, `handledAt`, `supplements` (các lần người gửi bổ sung, cũ trước: `supplementId`, `content`, `createdAt`, mảng rỗng khi chưa có, FR-30). Response này chỉ dành cho Administrator vì có danh tính người báo cáo (FR-29, BR-28). Thời gian là UTC không kèm múi giờ, Frontend hiển thị theo giờ Việt Nam.

- `decision` là một trong `NO_VIOLATION` (bài giữ nguyên), `WARN_AUTHOR` (bài giữ nguyên, tác giả nhận cảnh báo), `HIDE_RECIPE` (bài chuyển `HIDDEN`, không còn ở API công khai, tìm kiếm và gợi ý) hoặc `LOCK_AUTHOR` (tài khoản tác giả chuyển `LOCKED`, mọi Bearer token đã cấp nhận `403 ACCOUNT_LOCKED` ngay ở request kế tiếp).
- `AdminReportGroupResponse` gồm `recipe` (`id`, `title`, `status`), `author`, `reportCount` (số báo cáo khớp `status`, hoặc mọi báo cáo khi bỏ trống), `statusCounts` (`open`, `inReview`, `resolved` của mọi báo cáo trên bài), `latestReportAt` (báo cáo khớp bộ lọc mới nhất), `topReasonCode` và `topReasonCount` (lý do xuất hiện nhiều nhất trong các báo cáo khớp bộ lọc và số báo cáo có lý do đó; bằng nhau thì lấy lý do được báo cáo gần nhất, UC-06.1). Nhóm không thay báo cáo: mỗi báo cáo vẫn giữ người gửi, trạng thái và kết luận riêng (SRS 3.14), xem qua `recipeId` hoặc `GET /api/v1/admin/reports/{reportId}`.
- `reason` bỏ khoảng trắng đầu cuối, bắt buộc 10–500 ký tự với mọi kết luận (AC-28.5, BR-26). Sai `decision` hoặc `reason` trả `400 VALIDATION_FAILED` với `errors[].field` là `decision` hoặc `reason`, báo cáo giữ `IN_REVIEW`.
- Báo cáo `RESOLVED` không đổi được nữa: không có endpoint sửa, `claim` và `resolve` đều trả `409`. Bản ghi báo cáo (`decision`, `decision_reason`, `handled_by`, `handled_at`) là audit của quyết định (SRS 3.12, NFR-08). Báo cáo `IN_REVIEW` cũ chưa có `handledBy` (đổi từ `PROCESSING` ở V9) vẫn tiếp nhận được.
- Số lượng báo cáo không tự tạo chế tài; mỗi báo cáo cần một quyết định riêng (BR-26, BR-58). Kết luận của một báo cáo không tự đổi các báo cáo khác trên cùng bài.
- Khi giải quyết, Backend ghi `NOTIFICATION` trong cùng transaction: người báo cáo nhận `REPORT_RESULT` (kết quả chung, không nêu chế tài cụ thể), tác giả nhận `MODERATION_DECISION` với lý do của Administrator khi kết luận là cảnh báo, ẩn bài hoặc khóa tài khoản. Nội dung thông báo không chứa tên, email hay mô tả của người báo cáo (AC-49.4). Email kết quả kiểm duyệt chưa gửi trong bản này.

## 22. Báo cáo công thức — FR-26/27, FR-30

Generated OpenAPI runtime là contract chi tiết; mục này ghi quy ước dùng chung. Báo cáo lưu trong `REPORT` (V1, V9); filtered unique index `UQ_REPORT_open_recipe` giữ tối đa một báo cáo `OPEN`/`IN_REVIEW` cho mỗi cặp người gửi – bài.

- `POST /api/v1/recipes/{recipeId}/reports` (cần Bearer token) body `{ "reasonCode", "description"? }`, trả `201` `RecipeReportResponse` (`reportId`, `status = OPEN`, `message`). Chỉ bài `PUBLISHED`, khác thì `404 RECIPE_NOT_FOUND`. `reasonCode` là một trong `RECIPE_INFO_OR_DIET_LABEL`, `FOOD_SAFETY_HAZARD`, `SPAM_ADVERTISING`, `INAPPROPRIATE_CONTENT`, `COPYRIGHT_VIOLATION`, `OTHER`. `description` bỏ khoảng trắng đầu cuối, tối đa 500 ký tự; với `OTHER` bắt buộc 10–500 ký tự.
- Tài khoản đã có báo cáo `OPEN`/`IN_REVIEW` trên bài thì nhận `409 REPORT_ALREADY_OPEN`, body có `reportId` của báo cáo đó để Frontend dẫn người gửi tới báo cáo hiện có (AC-30.2). Request đồng thời của cùng tài khoản trên cùng bài được tuần tự hóa bằng khóa dòng `RECIPE_POST`: đúng một request tạo báo cáo, các request còn lại nhận `409` kèm `reportId` (SRS 3.14).
- Báo cáo lại sau khi báo cáo trước của chính tài khoản trên bài đó đã `RESOLVED` bắt buộc có `description` 10–500 ký tự với mọi `reasonCode` (AC-30.3, SRS 3.14); thiếu hoặc quá ngắn thì `400 VALIDATION_FAILED` với `field = description` và không tạo báo cáo.
- `GET /api/v1/me/reports/{reportId}` (cần Bearer token) trả `MyReportResponse` cho chính người gửi: `reportId`, `status`, `reasonCode`, `description`, `createdAt`, `recipe` (`id`, `title`), `supplements` (cũ trước) và `supplementLimit` (5). Không có danh tính người gửi hay ghi chú xử lý. Tài khoản khác nhận `403 ACCESS_DENIED` (NFR-09), ID không phải báo cáo Recipe Post nhận `404 REPORT_NOT_FOUND`. Frontend gọi endpoint này với `reportId` trong body `409 REPORT_ALREADY_OPEN` để mở báo cáo hiện có (AC-30.2).
- `POST /api/v1/me/reports/{reportId}/supplements` body `{ "content" }` trả `201` `ReportSupplementResponse` (`supplementId`, `content`, `createdAt`) (AC-30.1). `content` bỏ khoảng trắng đầu cuối, 10–500 ký tự, sai thì `400 VALIDATION_FAILED` với `errors[].field = content`. Chỉ người gửi (`403 ACCESS_DENIED` với tài khoản khác), chỉ khi báo cáo còn `OPEN`/`IN_REVIEW` (`409 REPORT_NOT_OPEN`), tối đa 5 lần mỗi báo cáo (`409 REPORT_SUPPLEMENT_LIMIT`). Request đồng thời được tuần tự hóa bằng khóa dòng `REPORT` nên không vượt giới hạn. Phần bổ sung lưu trong `REPORT_SUPPLEMENT` (V22), chỉ thêm, không sửa hay xóa, `REPORT.description` giữ nguyên.
- Administrator xem báo cáo gom theo bài qua `GET /api/v1/admin/reports/groups` và từng phản ánh của một bài qua `GET /api/v1/admin/reports?recipeId=` (AC-30.4); `AdminReportResponse.supplements` cho Administrator thấy các lần bổ sung. Xem mục 21.
