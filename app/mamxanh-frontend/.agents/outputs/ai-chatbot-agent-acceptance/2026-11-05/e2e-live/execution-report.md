# Báo cáo Thực thi Kịch bản Kiểm thử E2E (Execution Report)

> **Thời điểm:** 2026-11-04T21:33:16.716Z
> **Commit:** 94191353 | **Mô hình:** gemini-3.8-flash | **Cost Guard:** $0.0015 / $2.00

| Test Case ID | Capability | Tiêu đề Kịch bản | Trạng thái | Ghi chú bằng chứng |
|---|---|---|:---:|---|
| TC-G1-01 | CAP-01, CAP-02 | Tư vấn đạm thực vật | **FAIL** | [2mexpect([22m[31mlocator[39m[2m).[22mtoContainText[2m([22m[32mexpected[39m[2m)[22m failed

Locator: getByRole('dialog').locator('.ai-message-markdown').last()
Expected pattern: [32m/đạm|protein|đậu|hạt|nấm/i[39m
Timeout: 5000ms
Error: element(s) not found

Call log:
[2m  - Expect "toContainText" getByRole('dialog').locator('.ai-message-markdown').last() with timeout 5000ms[22m
[2m  - waiting for getByRole('dialog').locator('.ai-message-markdown').last()[22m
[2m    5 × locator resolved to <div class="ai-message-markdown min-w-0 [overflow-wrap:anywhere]">…</div>[22m
[2m      - unexpected value "Để gửi nội dung có dữ liệu cá nhân tới Google Gemini, vui lòng đồng ý đúng phạm vi trước."[22m
[2m    - waiting for "http://localhost:5173/trang-chu" navigation to finish...[22m
[2m    - navigated to "http://localhost:5173/trang-chu"[22m
 |
| TC-G1-04 | CAP-03 | Scope Policy từ chối món bò | **FAIL** | [2mexpect([22m[31mlocator[39m[2m).[22mtoBeEnabled[2m([22m[2m)[22m failed

Locator: getByRole('dialog').locator('#ai-chat-question')
Expected: enabled
Timeout: 15000ms
Error: element(s) not found

Call log:
[2m  - Expect "toBeEnabled" getByRole('dialog').locator('#ai-chat-question') with timeout 15000ms[22m
[2m  - waiting for getByRole('dialog').locator('#ai-chat-question')[22m
[2m    - waiting for "http://localhost:5173/trang-chu" navigation to finish...[22m
[2m    - navigated to "http://localhost:5173/trang-chu"[22m
 |
| TC-G1-05 | CAP-04 | Safety Policy từ chối y khoa | **FAIL** | [2mexpect([22m[31mlocator[39m[2m).[22mtoBeEnabled[2m([22m[2m)[22m failed

Locator: getByRole('dialog').locator('#ai-chat-question')
Expected: enabled
Timeout: 15000ms
Error: element(s) not found

Call log:
[2m  - Expect "toBeEnabled" getByRole('dialog').locator('#ai-chat-question') with timeout 15000ms[22m
[2m  - waiting for getByRole('dialog').locator('#ai-chat-question')[22m
[2m    - waiting for "http://localhost:5173/trang-chu" navigation to finish...[22m
[2m    - navigated to "http://localhost:5173/trang-chu"[22m
 |
| TC-G2-01 | CAP-06, CAP-07, CAP-08 | RAG tri thức nội bộ | **FAIL** | [2mexpect([22m[31mlocator[39m[2m).[22mtoBeEnabled[2m([22m[2m)[22m failed

Locator: getByRole('dialog').locator('#ai-chat-question')
Expected: enabled
Timeout: 15000ms
Error: element(s) not found

Call log:
[2m  - Expect "toBeEnabled" getByRole('dialog').locator('#ai-chat-question') with timeout 15000ms[22m
[2m  - waiting for getByRole('dialog').locator('#ai-chat-question')[22m
[2m    - waiting for "http://localhost:5173/trang-chu" navigation to finish...[22m
[2m    - navigated to "http://localhost:5173/trang-chu"[22m
 |
| TC-G3-01 | CAP-09 | Hỏi lại tên khi thiếu tham số | **FAIL** | [2mexpect([22m[31mlocator[39m[2m).[22mtoBeEnabled[2m([22m[2m)[22m failed

Locator: getByRole('dialog').locator('#ai-chat-question')
Expected: enabled
Timeout: 15000ms
Error: element(s) not found

Call log:
[2m  - Expect "toBeEnabled" getByRole('dialog').locator('#ai-chat-question') with timeout 15000ms[22m
[2m  - waiting for getByRole('dialog').locator('#ai-chat-question')[22m
[2m    - waiting for "http://localhost:5173/trang-chu" navigation to finish...[22m
[2m    - navigated to "http://localhost:5173/trang-chu"[22m
 |
| TC-G3-02 | CHATBOT | CHATBOT không thực thi thao tác ghi | **FAIL** | locator.click: Test timeout of 450000ms exceeded.
Call log:
[2m  - waiting for getByRole('dialog').locator('button[aria-label^="Chế độ AI:"]')[22m
[2m    - waiting for "http://localhost:5173/trang-chu" navigation to finish...[22m
[2m    - navigated to "http://localhost:5173/trang-chu"[22m
 |
| TC-G3-03 | AI Agent | AI Agent tôn trọng từ chối | **FAIL** | locator.click: Target page, context or browser has been closed |
