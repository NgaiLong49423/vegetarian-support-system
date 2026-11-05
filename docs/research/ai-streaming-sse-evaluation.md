> **Document:** AI Assistant SSE Streaming Architecture & Trade-off Evaluation  
> **File:** `docs/research/ai-streaming-sse-evaluation.md`  
> **Version:** v1.0.0  
> **Created:** 2026-11-03  
> **Last Updated:** 2026-11-03  
> **Status:** Active  

# Báo cáo đánh giá kỹ thuật: Streaming Server-Sent Events (SSE) cho Mâm Xanh AI Assistant

## 1. Bối cảnh và Mục tiêu

Trong kế hoạch tái cấu trúc AI Agent Mâm Xanh (PR #227), nhiệm vụ P0–P2 đã hoàn thành việc thống nhất runtime sang Google ADK Agentic Loop hai chiều, Role-Based Tool Filtering (28 domain tools), Saga Execution Journal và Execution Truthfulness Verifier.

Mục tiêu của báo cáo kỹ thuật này (Nhiệm vụ P3 / Phase 8) là đánh giá toàn diện tính khả thi, kiến trúc kỹ thuật và các trade-off khi bổ sung cơ chế truyền phát dữ liệu thời gian thực (Streaming) qua Server-Sent Events (SSE) từ Backend Spring Boot tới Chatbox Frontend React.

---

## 2. Khảo sát cơ chế Streaming trên Google ADK

Trong kiến trúc Google ADK Java SDK (`com.google.adk:google-adk:1.13.0`):
- `InMemoryRunner.runAsync(owner, sessionId, content, config)` trả về một `io.reactivex.rxjava3.core.Flowable<Event>`.
- Về bản chất, `Flowable<Event>` phát ra luồng sự kiện phản ứng (reactive event stream) tuần tự khi các bước trong Agentic Loop diễn ra:
  1. `Event` chứa model call request / raw candidate tokens.
  2. `Event` chứa `FunctionCall` (khi model quyết định gọi công cụ).
  3. `Event` chứa `FunctionResponse` (sau khi tool được thực thi bởi Backend).
  4. `Event` chứa các phần tử văn bản (`Part.fromText`) cuối cùng cho người dùng.

Về phía Spring Boot WebMVC, luồng này có thể chuyển tiếp tới client thông qua `org.springframework.web.servlet.mvc.method.annotation.SseEmitter` bằng cách subscribe vào `Flowable<Event>` của ADK.

---

## 3. Các thách thức kỹ thuật và Trade-off cốt lõi

### 3.1. Mâu thuẫn giữa Token Streaming tức thì và Execution Truthfulness Verification (P0)

- **Vấn đề**: Trong mô hình Agentic Loop hai chiều, Gemini có thể sinh ra văn bản trước hoặc sau khi gọi tool. Nếu hệ thống stream từng token (`text-delta`) ngay khi model vừa nhả chữ:
  - Nếu model sinh văn bản ảo giác (hallucination) như *"Tôi đã tạo thành công danh sách mua sắm..."* trước khi tool chạy xong hoặc khi tool gặp lỗi database/hết quota, các token này đã lập tức hiển thị trên màn hình người dùng.
  - Khi đó, `ExecutionTruthfulnessVerifier` không thể can thiệp hoặc thu hồi các token đã gửi qua giao thức SSE, dẫn đến việc người dùng nhận được thông tin sai lệch dù Backend ghi nhận thao tác thất bại.
- **Giải pháp kỹ thuật bắt buộc**:
  - Phải áp dụng mô hình **Dual-Phase Streaming / Buffer-then-Stream**:
    - *Phase Tool Calling & Execution*: Toàn bộ sự kiện gọi tool và kết quả thực thi backend được xử lý nội bộ, chỉ emit các SSE event kiểu `step_update` (ví dụ: `{"type":"STEP","tool":"search_public_recipes","status":"RUNNING"}`).
    - *Phase Final Synthesis*: Sau khi các tool hoàn tất và `ExecutionTruthfulnessVerifier` xác nhận tính trung thực của phản hồi, token văn bản cuối cùng mới được stream tới client.

### 3.2. Ràng buộc Hạn mức & Hạch toán Token (Token Quota Accounting - D3)

- Theo quy định `BR-09` và `MemberAccountingPolicy`: việc trừ token quota và tính phí tín dụng (Credit/Token) cần được hạch toán sau khi tổng số token input/output của lượt chạy được xác định chính xác từ `UsageMetadata` của nhà cung cấp.
- Nếu client ngắt kết nối giữa chừng (Client Drop / Cancel SSE):
  - Model có thể đã tiêu thụ token từ Gemini API.
  - Backend phải có cơ chế `onCompletion` / `onTimeout` trên `SseEmitter` để đảm bảo ghi nhận usage telemetry ngay cả khi người dùng đóng trình duyệt.

### 3.3. Tương thích với Human-in-the-Loop (AWAITING_CONFIRMATION)

- Ở chế độ `ExecutionMode.CONFIRM`: khi gặp thao tác ghi dữ liệu, hệ thống không thực thi ngay mà tạo `PendingConfirmation`.
- Khi stream qua SSE: Backend không stream văn bản tự do mà phải emit một event đặc thù `{"type":"CONFIRMATION_REQUIRED", "confirmationId":"...", "summary":"..."}` để Frontend hiển thị dialog/nút bấm "Xác nhận" / "Hủy".

---

## 4. Thiết kế Kiến trúc Mục tiêu cho Streaming (PoC)

Khi dự án quyết định kích hoạt Streaming ở giai đoạn tiếp theo, kiến trúc đề xuất bao gồm:

```
[Frontend Chatbox] 
      │
      │ 1. POST /recipes/ai/chat/stream (SSE)
      ▼
[Spring Boot SseEmitter]
      │
      │ 2. Subscribes to ADK Flowable<Event>
      ▼
[AdkAssistantEngine]
      ├── 3a. Event = ToolCall/ToolResponse ──> Emit SSE Event: "step"
      ├── 3b. Event = PendingConfirmation   ──> Emit SSE Event: "pending_confirmation"
      └── 3c. Event = Final Text Candidate  ──> Verify Truthfulness ──> Emit SSE Event: "delta"
```

### Cấu trúc SSE Payload Đề xuất:
- `event: step` — Cập nhật tiến trình từng bước: `{"index":1,"tool":"search_public_recipes","status":"SUCCEEDED"}`
- `event: confirmation` — Yêu cầu người dùng duyệt thao tác: `{"confirmationId":"...","proposedAction":"..."}`
- `event: delta` — Từng đoạn văn bản trả lời tự nhiên đã được kiểm chứng: `{"text":"Dưới đây là..."}`
- `event: complete` — Báo hiệu hoàn tất lượt chạy kèm metadata, sources và token usage: `{"status":"COMPLETED","sources":[...]}`

---

## 5. Kết luận và Khuyến nghị

1. **Khuyến nghị hiện tại**: Giữ nguyên cơ chế đồng bộ REST JSON (`POST /recipes/ai/chat`) làm mặc định cho PR #227. Cơ chế này đã được kiểm chứng qua 1,081 unit test, bảo toàn tuyệt đối 100% tính trung thực dữ liệu, cơ chế xác nhận CONFIRM, consent và multi-action journal.
2. **Lộ trình kích hoạt Streaming**: Triển khai SSE dưới dạng một feature độc lập sau khi runtime Google ADK trên main ổn định, tuân thủ mô hình Buffer-then-Stream đã phân tích ở trên để không làm suy giảm độ tin cậy của hệ thống.
