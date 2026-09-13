> **Document:** Similar Products Benchmark for Topic 03  
> **File:** `docs/research/similar-products-benchmark.md`  
> **Version:** v0.1.0  
> **Created:** 2026-09-12  
> **Last Updated:** 2026-09-12  
> **Status:** Under Review  

# Benchmark sản phẩm tương tự — Đề tài 03

## Trạng thái và cách sử dụng

- Ngày nghiên cứu: 11/09/2026.
- Phạm vi: sản phẩm có một phần nghiệp vụ tương tự cộng đồng ăn chay, nội dung/công thức, moderation, meal planning hoặc AI subscription.
- Đây là tài liệu tham khảo để phân rã yêu cầu; **không phải yêu cầu đã chốt của dự án**.
- Bằng chứng được phân biệt giữa mô tả từ nguồn chính thức và quan sát trực tiếp giao diện. Quan sát giao diện không chứng minh thuật toán hoặc xử lý backend. “Bài học cho dự án” là đề xuất, trừ quyết định đã được người dùng xác nhận và ghi trong SRS.

## 1. Kết luận nhanh

Không có một sản phẩm tham khảo duy nhất trùng hoàn toàn với đề tài. Bộ ba hữu ích nhất là:

1. **Samsung Food:** gần nhất với tổ hợp community, recipe discovery, meal planning, AI và subscription.
2. **HappyCow:** gần nhất với cộng đồng vegan/vegetarian, nội dung do thành viên đóng góp và quy trình con người kiểm tra dữ liệu trước khi công khai.
3. **Forks Over Knives:** gần nhất với nội dung plant-based được tuyển chọn và weekly meal planning.

Nhóm nên học theo từng nghiệp vụ, không sao chép toàn bộ feature set của bất kỳ app nào.

## 2. Ma trận so sánh

| Sản phẩm | Community/UGC | Moderation | Meal planning | AI/premium | Mức tương đồng với đề tài |
| --- | --- | --- | --- | --- | --- |
| Samsung Food | Có community, recipe sharing và content creators | Có community guidelines; kết hợp công nghệ và con người, có thể gỡ nội dung/khóa tài khoản | Có weekly planner, thay món, shopping list | Food+ có tailored 7-day plans và AI recipe personalization | Cao nhất về tổng thể, nhưng phạm vi lớn hơn nhiều |
| HappyCow | Thành viên đóng góp review, ảnh, listing và tham gia forum | Listing mới được đội ngũ con người kiểm tra; review có guidelines | Không phải trọng tâm chính | Không phải ví dụ chính cho AI subscription | Cao về cộng đồng ăn chay và human moderation |
| Forks Over Knives | Có comment/community ở mức hỗ trợ | Nội dung công thức do đội ngũ tuyển chọn | Weekly plan, recipe box, thay món, shopping list | Meal Planner là dịch vụ trả phí | Cao về nội dung đáng tin cậy và meal planning |

## 3. Samsung Food — tham khảo gần nhất về tổng thể

### Nghiệp vụ đã xác minh

- Hỗ trợ recipe discovery/personalization, meal planning và social sharing.
- Người dùng có thể tham gia community và chia sẻ nội dung công thức.
- Food+ cung cấp tailored 7-day meal plans và AI recipe personalization dưới dạng premium subscription.
- Community Guidelines yêu cầu nội dung liên quan đúng chủ đề; nội dung vi phạm có thể bị gỡ và tài khoản vi phạm nặng/lặp lại có thể bị cấm.
- Samsung mô tả moderation có thể kết hợp công nghệ và con người.

Nguồn:

- [Samsung Food — trang sản phẩm](https://samsungfood.com/)
- [Samsung Food+ — tính năng premium](https://samsungfood.com/food-plus/)
- [Samsung Food User Community Guidelines](https://support.samsungfood.com/hc/en-us/articles/18365059414804-Samsung-Food-User-Community-Guidelines)
- [Getting Started with Meal Planner](https://support.samsungfood.com/hc/en-us/articles/35369657798548-Getting-Started-with-Meal-Planner)

### Bài học cho dự án

- Tách rõ chức năng miễn phí và giá trị premium; premium cần khác biệt về capability, không chỉ đổi nhãn.
- AI meal planning là một giá trị nâng cấp dễ hiểu hơn chatbot “hỏi gì cũng được”.
- Content policy cần nêu nội dung phù hợp chủ đề ăn chay, spam/trùng lặp, nội dung nguy hiểm và chế tài.
- Dự án của nhóm chỉ có một loại bài là bài công thức (`Recipe Post`), không có Blog tổng quát tách riêng. Hướng dẫn từng bước thuộc bài nhưng là tùy chọn.

### Không nên đưa vào MVP 9 tuần

- Pantry/food inventory đầy đủ.
- Shopping/e-commerce integration.
- Smart appliance control.
- Vision AI nhận diện nguyên liệu.
- Health-device synchronization và nutrition tracking chi tiết.

### Khảo sát trực tiếp phiên web đã đăng nhập — 11/09/2026

- [Home](https://app.samsungfood.com/feed) hiển thị thẻ món có ảnh, tên, thời gian và số nguyên liệu; có các điểm vào Post, Recipe URL và New Recipe riêng.
- [Chi tiết món đã khảo sát](https://app.samsungfood.com/recipes/10702f6cbc2384c415daa20b51736e08a6b) có nguyên liệu, khẩu phần, thời gian chuẩn bị/nấu, liên kết hướng dẫn nguồn và hành động thêm vào Meal Plan.
- [Planner](https://app.samsungfood.com/meal-plan) có Plan, For you, Queue, Previous; khi chưa có kế hoạch có lựa chọn cá nhân hóa, thư viện kế hoạch hoặc tự lập.
- For you mở lời mời trả lời câu hỏi. Bước tiếp theo yêu cầu đồng ý sử dụng dữ liệu sức khỏe; không cấp đồng ý nên chưa kiểm chứng câu hỏi phía sau hay chất lượng AI cá nhân hóa.
- Khi khảo sát, Add to meal plan thêm món ngay vào Queue chưa gán ngày. Đã hoàn tác đúng món vừa thêm bằng Remove from plan và xác minh Queue trống. Không kiểm chứng luồng lưu lịch theo ngày hay thay món AI.

**Quyết định người dùng đã chốt sau khảo sát và cập nhật 12/09/2026:** lấy Samsung Food làm tham khảo chính cho khám phá → chi tiết món → thực đơn; hệ thống chỉ quản lý bài công thức, không có Blog tổng quát. Bài có nguyên liệu, khẩu phần, thời gian nấu và loại ăn chay; cách làm từng bước là tùy chọn. Tương tác bình chọn dùng Like/Upvote đơn giản, không sao chép đánh giá 1–5 sao. Xem SRS mục 3.3, FR-16, FR-17 và FR-45. Không mặc định nhận toàn bộ feature set Samsung Food vào MVP.

**Quyết định phạm vi Saved/Planner ngày 12/09/2026:** dự án tách Công thức đã lưu khỏi Lịch ăn. Lưu công thức không gắn ngày/bữa và không thay đổi hồ sơ sở thích; mục lịch ăn phải tham chiếu ngày/bữa và có vòng đời riêng. Không sao chép Queue hay một module Previous riêng của Samsung Food vào MVP; tuần cũ được xem bằng điều hướng lịch, còn `For You` nếu có dùng luồng Gemini hiện tại.

## 4. HappyCow — tham khảo community và kiểm duyệt con người

### Nghiệp vụ đã xác minh

- Thành viên có thể đóng góp review và ảnh về restaurant/store, tạo danh sách và tham gia community/forum.
- Listing doanh nghiệp mới được đội ngũ con người kiểm tra về vị trí, giờ hoạt động và menu trước khi phê duyệt.
- HappyCow phân loại rõ Vegan, Vegetarian, Options và Stores & More.
- Review cần tài khoản thành viên; listing và review là hai loại contribution có quy tắc xuất bản khác nhau.

Nguồn:

- [HappyCow Member Benefits](https://www.happycow.net/members/benefits)
- [HappyCow Business FAQ — submission và approval](https://www.happycow.net/business/faq)
- [HappyCow Members FAQ](https://server.happycow.net/members/faq)

### Bài học cho dự án

- Human moderation trước khi công khai làm nội dung đáng tin hơn nhưng tạo hàng đợi và thời gian chờ. Dự án đã chọn hướng khác: Admin duyệt đơn xin quyền đăng một lần, người được cấp quyền tự công khai bài và Admin hậu kiểm khi có báo cáo.
- Bài công thức và Comment có thể cần mức hậu kiểm khác nhau. Dự án đã chốt bình luận hỗ trợ reply lồng nhiều cấp; giới hạn độ sâu/hiển thị và chế tài chi tiết sẽ được thiết kế sau. Dự án không cần thêm quy tắc cho một loại Blog tổng quát vì loại nội dung đó đã bị loại khỏi phạm vi.
- Cần danh mục ăn chay rõ ràng và tiêu chí gắn nhãn, tránh chỉ dựa vào từ khóa.
- Địa điểm ăn chay quanh địa chỉ người dùng nhập đã được đưa vào MVP; HappyCow tiếp tục là nguồn tham khảo nghiệp vụ, còn dữ liệu địa điểm của app lấy từ Google Maps Platform theo SRS. Dự án không quản lý hồ sơ nhà hàng và không gắn đề xuất địa điểm với món/bài công thức người dùng vừa tìm.

## 5. Forks Over Knives — tham khảo nội dung được tuyển chọn và meal plan

### Nghiệp vụ đã xác minh

- Cung cấp kho công thức whole-food plant-based với hướng dẫn từng bước.
- Người dùng có thể đánh dấu yêu thích, ghi chú và đưa nguyên liệu vào shopping list.
- Meal Planner cung cấp weekly plans, recipe box, thay recipe trong tuần và grocery list.
- Nội dung được trình bày là do đội ngũ/chef xây dựng thay vì phụ thuộc chủ yếu vào bài đăng tự do.

Nguồn:

- [Forks Over Knives Recipe App](https://www.forksoverknives.com/recipe-app/)
- [Forks Meal Planner](https://forksmealplanner.forksoverknives.com/)

### Bài học cho dự án

- Nội dung được tuyển chọn có thể tạo niềm tin tốt hơn số lượng bài viết lớn nhưng thiếu kiểm chứng.
- Với bài công thức, nên bắt buộc đủ thông tin tối thiểu trước khi người có quyền đăng công khai; hướng dẫn từng bước có thể để trống theo quyết định dự án.
- Weekly meal plan nên bắt đầu bằng thao tác thêm/thay/xóa món rõ ràng; AI personalization là lớp hỗ trợ sau, không thay thế dữ liệu công thức hợp lệ.
- Không sao chép các tuyên bố sức khỏe hoặc điều trị nếu dự án không có nguồn và chuyên gia chịu trách nhiệm.

## 6. Tài liệu kỹ thuật hỗ trợ quyết định media

- YouTube cho phép nhúng video bằng iframe/IFrame Player API để xem trực tiếp trong app: [YouTube IFrame Player API](https://developers.google.com/youtube/iframe_api_reference).
- Video private, age-restricted hoặc bị chủ sở hữu tắt embedding có thể không phát trong app: [YouTube Help](https://support.google.com/youtube/answer/97363).
- Azure Blob Storage có thể nhận upload trực tiếp bằng SAS có phạm vi và thời hạn giới hạn; đây là hướng phù hợp nếu sau này mở upload video lớn: [Azure SAS overview](https://learn.microsoft.com/en-us/azure/storage/common/storage-sas-overview).

Quyết định hiện tại của dự án: Phase 1 lưu ảnh trên Azure Blob và nhúng link YouTube trong bài công thức; không upload file video trực tiếp.

## 7. Phạm vi học hỏi phù hợp với 9 tuần

| Nghiệp vụ cần học | Sản phẩm tham khảo chính | Cách áp dụng nhỏ nhất cho dự án |
| --- | --- | --- |
| Community content | Samsung Food | Bài công thức; ảnh Azure và link YouTube tùy chọn; bình luận là tương tác hỗ trợ |
| Human moderation | HappyCow + Samsung Food guidelines | Admin duyệt đơn xin quyền đăng; người dùng báo cáo bài công khai và Admin hậu kiểm, không có hàng đợi duyệt từng bài |
| Recipe content | Forks Over Knives | Bài công thức có dữ liệu tối thiểu để tìm/lọc; hướng dẫn từng bước tùy chọn |
| Meal planning | Forks Over Knives + Samsung Food | Tách Đã lưu và Lịch ăn; thêm/chuyển/thay/xóa món theo ngày/bữa; chưa thêm Queue hay shopping integration |
| AI premium | Samsung Food+ | Free/Plus/Pro cùng AI, khác hạn mức 5/15/50; thanh toán thật theo Q19/Q26, không sao chép cách khóa tính năng của đối thủ |

## 8. Kết quả chốt phạm vi — 12/09/2026

Các câu hỏi sản phẩm đã được giải quyết trong [SRS mục 3.20](../requirements/SRS.md): reply nhiều cấp, báo cáo cả bình luận, lịch ba bữa, cùng chức năng AI cho Free/Plus/Pro; nhà hàng dùng ngưỡng đường bộ và danh sách + bản đồ. Bài công thức có bước nấu tùy chọn. Chỉ học các luồng phù hợp; nội dung khảo sát đối thủ không phải yêu cầu bắt buộc của nhóm.

Q31 ghi nguồn dinh dưỡng, công thức nghiên cứu khi triển khai. Q35 cho phép nhóm tự chọn phạm vi nhà hàng độc lập món đã tìm, không có điều kiện chờ xác nhận giảng viên. Validation và chi tiết tích hợp vẫn cần thiết kế; benchmark không chứng minh tính năng dự án đã chạy.
