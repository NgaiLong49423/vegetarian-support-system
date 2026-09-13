> **Document:** Topic 03 AI Plan Decomposition  
> **File:** `docs/requirements/ai-plan-decomposition.md`  
> **Version:** v0.6.1  
> **Created:** 2026-09-11  
> **Last Updated:** 2026-09-12  
> **Status:** Under Review  

# Phân rã gói AI — Đề tài 03

## Trạng thái và ranh giới

- Ngày cập nhật: 11/09/2026.
- Nhà cung cấp AI khởi đầu đã chốt: **Gemini API**.
- Người chưa đăng nhập cũng dùng được gói Free; người có tài khoản dùng Free hoặc nâng cấp Plus/Pro.
- Đây là thiết kế sản phẩm và kỹ thuật. Đã chốt thanh toán thật; giá, chu kỳ, cổng, gia hạn và hoàn tiền chưa chốt.
- AI hỗ trợ tìm kiếm, gợi ý và hỏi đáp. Khi gợi ý món hoặc lập menu, AI chỉ chọn từ bài công thức đang công khai, không bị ẩn/xóa trong hệ thống và không tạo công thức mới. Với menu theo dinh dưỡng, AI dùng mức tham khảo do hệ thống cung cấp và công thức có đủ dữ liệu dinh dưỡng; không tự đặt mục tiêu hoặc tự ước lượng dữ liệu thiếu. AI không tự xuất bản/ẩn/xóa nội dung và không thay thế tư vấn y tế/dinh dưỡng.

## 1. Kết luận: dùng lượt gọi theo ngày ở giao diện, vẫn đo token thật ở backend

Nên dùng **số lượt gọi AI theo ngày** để áp dụng quyền cho người dùng. Ví dụ Free 5 lần/ngày, Plus 15, Pro 50. Đây là đơn vị dễ hiểu, dễ test, và người dùng biết ngay mình còn bao nhiêu lượt.

Nhưng không nên bỏ hoàn toàn token. Gemini trả `usageMetadata` với input/output/total token cho mỗi phản hồi; backend có thể lưu dữ liệu này để quan sát mức tiêu thụ thực tế. Như vậy:

| Mục đích | Đơn vị sử dụng |
| --- | --- |
| Quyền hiển thị và giới hạn người dùng | Lượt gọi AI/ngày |
| Chống spam/tăng đột biến trong thời gian ngắn | Requests per minute (RPM) nội bộ |
| Theo dõi chi phí/quota Gemini thật | Token input/output/total từ phản hồi Gemini, khi có |

Mỗi “lượt gọi” ở phiên bản đầu phải được chuẩn hóa là **một yêu cầu text-only, một phản hồi, giới hạn độ dài đầu ra**. Nếu sau này thêm nhận diện ảnh, tóm tắt video, Google Search grounding hoặc output dài, đó phải là loại request khác và không được âm thầm tính ngang một lượt text.

## 2. Sự thật cần giữ rõ về Gemini Free Tier

Gemini có Free Tier cho một số model, nhưng **không phải miễn phí vô hạn hay được bảo đảm mãi mãi**. Quota phụ thuộc model và project; Google áp dụng các giới hạn như RPM, TPM và RPD, đồng thời có thể thay đổi theo tier. Quota tính theo project, không theo API key; RPD của Google reset theo giờ Pacific. Xem tài liệu chính thức [rate limits](https://ai.google.dev/gemini-api/docs/rate-limits) và [pricing](https://ai.google.dev/gemini-api/docs/pricing) (đã kiểm tra 11/09/2026).

Hệ quả cho sản phẩm:

- Không cam kết “Max vô hạn” khi backend còn dùng Gemini Free Tier.
- Gói người dùng trong app **khác** usage tier/billing tier của Google. Free/Plus/Pro/Max là quyền trong app; Google vẫn có quota riêng ở project.
- Nếu Google trả lỗi quota/rate limit, app phải báo tạm thời không khả dụng, không hứa có câu trả lời ngay chỉ vì người dùng đang ở Pro/Max.
- Với quyết định thu tiền thật từ người dùng, nhóm phải đánh giá chi phí Gemini Paid Tier và điều khoản trước. Không nên bán quyền “không giới hạn” dựa duy nhất vào quota miễn phí.

## 3. Gói AI đã chốt theo Q19–Q26

| Gói | Đăng nhập | Lượt AI/ngày | Quyền |
| --- | --- | ---: | --- |
| Guest Free | Không | 5 | Hỏi đáp cơ bản; không cá nhân hóa/lịch sử tài khoản |
| Free | Có | 5 | Toàn bộ chức năng AI trong phạm vi |
| Plus | Có | 15 | Cùng chức năng Free, nhiều lượt hơn |
| Pro | Có | 50 | Cùng chức năng Free, nhiều lượt hơn |

Không triển khai Max/unlimited. Quyền đăng bài và điều kiện hồ sơ/dinh dưỡng vẫn áp dụng, không được bỏ qua vì mua gói.

## 4. Chức năng và cách tính lượt

- Mọi gói Member có hỏi đáp, gợi ý công thức hiện có, lập/thay menu tuần, giải thích dinh dưỡng, tùy chọn gợi ý bài liên quan và hỗ trợ tác giả soạn bài.
- AI không tạo công thức mới; hỗ trợ viết giới thiệu/bước nấu dựa trên đầu vào tác giả, không tự thêm nguyên liệu hoặc tự công khai.
- Mỗi yêu cầu AI thành công tính một lượt, kể cả đề xuất menu 7 ngày sáng/trưa/tối. Yêu cầu lỗi không trừ lượt; không tính theo số món hoặc token.
- Member xem/xóa lịch sử AI riêng. Lịch sử hội thoại khác log usage tối thiểu dùng đối soát; thời hạn lưu và quy tắc xóa log phải xác định ở thiết kế.
- Gợi ý bài liên quan thông thường không gọi Gemini/không tính lượt; AI chỉ được gọi khi người dùng yêu cầu.
- Đăng ký Plus/Pro qua thanh toán thật, backend xác minh giao dịch trước khi kích hoạt. Chưa chốt giá, cổng, chu kỳ và chính sách; mô phỏng chỉ phục vụ kiểm thử, không thay yêu cầu tích hợp thật.
- Hạn mức app không bảo đảm quota Gemini luôn đủ; lỗi dịch vụ phải được hiển thị rõ, không bịa kết quả.

## 5. Luồng nghiệp vụ

### A. Guest Free — chưa đăng nhập

1. Khách chọn “Dùng thử AI”.
2. Backend tạo/đọc một `AnonymousFreeId` ngẫu nhiên bằng cookie phiên; không dùng API key Gemini ở frontend.
3. Backend kiểm tra còn tối đa 5 lượt Free trong ngày, giới hạn RPM theo `AnonymousFreeId`/IP và áp dụng biện pháp chống bot phù hợp.
4. Nếu hợp lệ, backend gọi Gemini, trả câu trả lời có cảnh báo phạm vi và lưu usage ở dạng ẩn danh/tối thiểu.
5. Sau khi hết 5 lượt, khách được mời đăng ký hoặc nâng cấp; không tự tạo tài khoản và không cố nhận diện lại cá nhân chỉ để cấp thêm lượt.

### B. Tài khoản Free/Plus/Pro

1. Người dùng chọn tính năng AI.
2. Backend xác định gói đang hiệu lực và giới hạn hằng ngày của gói.
3. Backend kiểm tra usage count hôm nay và RPM của tài khoản.
4. Nếu còn quyền, backend gọi Gemini; sau phản hồi, lưu kết quả, tăng `requestCount`, và lưu token metadata nếu Gemini trả về.
5. Nếu hết lượt, không gọi Gemini; UI hiển thị “đã dùng X/Y lượt hôm nay”, mốc reset, và quyền lợi nâng cấp.

### C. Gemini lỗi hoặc hết quota project

1. Không tăng số lượt của người dùng nếu backend chưa nhận được câu trả lời hợp lệ.
2. Ghi nhận lỗi kỹ thuật có phân loại (timeout, `429`, safety block, provider error), không ghi API key hay dữ liệu nhạy cảm.
3. Hiển thị lỗi tạm thời; người dùng giữ nguyên số lượt.
4. Với `429`, backend áp dụng retry/backoff có giới hạn hoặc yêu cầu thử lại sau; không lặp vô hạn.

## 6. Quy tắc nghiệp vụ tối thiểu

| Mã | Quy tắc |
| --- | --- |
| AI-01 | Khách chưa đăng nhập có gói Guest Free gồm tối đa 5 lượt/ngày theo cookie/thiết bị, IP và chính sách chống lạm dụng; không dùng được tính năng cá nhân hóa. |
| AI-02 | Mỗi request AI phải được backend kiểm tra gói, lượt còn lại và RPM trước khi gọi Gemini. |
| AI-03 | API key Gemini chỉ nằm ở backend/biến môi trường; tuyệt đối không gửi xuống browser hay commit vào repository. |
| AI-04 | Một request được tính một lượt khi backend nhận câu trả lời hợp lệ từ Gemini và lưu kết quả thành công. |
| AI-05 | Request lỗi trước khi có câu trả lời hợp lệ không trừ lượt; event lỗi vẫn được ghi để đối soát. |
| AI-06 | Hạn mức ngày của app reset theo timezone đã công bố; đề xuất dùng `Asia/Ho_Chi_Minh` cho nhóm/người dùng mục tiêu. Đây tách biệt với thời điểm reset quota RPD của Google. |
| AI-07 | Max không được mô tả “unlimited” khi chưa có quota/ngân sách ràng buộc; mọi gói vẫn có RPM và fair-use protection. |
| AI-08 | `usageMetadata.totalTokenCount` của Gemini được lưu khi phản hồi có trường này, chỉ nhằm quan sát quota/chi phí; không dùng nó làm quyền hiển thị ở bản đầu. |
| AI-09 | AI không tự đăng, duyệt, ẩn hay xóa nội dung. Nội dung gắn cờ chuyển Administrator quyết định. |
| AI-10 | Câu trả lời AI phải hiển thị giới hạn: đây là gợi ý thông tin, không chẩn đoán/điều trị hay thay thế chuyên gia y tế. |
| AI-11 | AI gợi ý/lập menu chỉ trả các bài công thức đang công khai, không bị ẩn/xóa và phải dẫn tới bài nguồn; nếu không có lựa chọn phù hợp thì thông báo rõ, không tạo công thức mới. |
| AI-12 | AI không dùng BMI một mình để suy ra calorie/dưỡng chất và không gọi chênh lệch của một ngày là chẩn đoán thiếu chất; dữ liệu không đủ phải được nói rõ. |
| AI-13 | AI menu dinh dưỡng và giải thích kiểm tra ngày trong MVP chỉ phục vụ Member đủ 18 tuổi, không mang thai/cho con bú và không cần chế độ ăn điều trị; người ngoài phạm vi vẫn dùng AI không-dinh-dưỡng theo quyền gói. |

## 7. Dữ liệu tối thiểu để đo bằng dữ liệu thật

Đây chưa phải ERD, nhưng các dữ liệu sau là bắt buộc nếu muốn biết mức dùng thực tế thay vì đoán:

| Đối tượng | Dữ liệu cần lưu | Lý do |
| --- | --- | --- |
| `AIPlan` | code, dailyRequestLimit, feature permissions, trạng thái | Cấu hình quyền Free/Plus/Pro mà không hard-code. |
| `Subscription` | userId, planCode, start/end, status | Biết gói thực sự đang hiệu lực của tài khoản. |
| `AnonymousFreeUsage` | anonymous id đã băm, ngày sử dụng, usedCount, expiresAt | Đếm 5 lượt Free cho khách mà không lộ danh tính. |
| `AIUsageEvent` | requestId, userId *hoặc* anonymous id, feature, thời điểm, outcome, requestCountDelta | Đếm lượt thành công/thất bại có kiểm chứng. |
| `GeminiUsageMetadata` | promptTokenCount, candidatesTokenCount, totalTokenCount, model, requestId | Đo token thực tế do Gemini phản hồi; không tự ước lượng. |
| `AIProviderError` | requestId, loại lỗi, provider status, retryable | Phân biệt hết lượt gói với lỗi Gemini/quota project. |

Không ghi prompt đầy đủ hoặc dữ liệu sức khỏe nhạy cảm vào log vận hành mặc định. Nếu cần lưu hội thoại, phải có mục đích và thời hạn lưu rõ ràng.

## 8. Kiểm chứng bắt buộc trước khi tuyên bố hoạt động

1. Gọi Gemini qua backend với một API key nằm trong biến môi trường và đọc `usageMetadata` từ phản hồi thật.
2. Gửi một request thành công: xác nhận `AIUsageEvent.requestCountDelta = 1` và token metadata được lưu đúng request.
3. Gửi request lỗi/timeout giả lập: xác nhận không tăng số lượt.
4. Dùng Guest Free đủ 5 lượt: xác nhận request thứ 6 bị chặn trước khi gọi Gemini.
5. Dùng Free đủ 5 lượt trong cùng ngày: xác nhận request thứ 6 bị chặn; sau mốc reset app, quota được tính lại.
6. Vượt RPM nội bộ hoặc Gemini trả `429`: xác nhận thông báo đúng, không lộ secret, và không trừ lượt oan.

Các bước này chứng minh luồng đếm của app và metadata trả về trong môi trường thử. Chúng không chứng minh Gemini Free Tier đủ cho tải thật lâu dài.

## 9. Việc tiếp theo

Viết User Stories/Acceptance Criteria cho quyền gói 5/15/50, một yêu cầu thành công một lượt, lịch sử riêng và thanh toán thật. Giá/cổng/chu kỳ/gia hạn/hoàn tiền chưa chốt. Dinh dưỡng theo [SRS mục 3.18 và 3.20](SRS.md): ghi nguồn trước, nghiên cứu công thức khi triển khai Q31.
