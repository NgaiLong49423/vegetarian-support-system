import { FormEvent, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Bot, LoaderCircle, Send, ShieldAlert, UserRound } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { useAuth } from '../components/AuthContext';
import { recipesApi, type AiChatTurn } from '../api/recipes';
import { asApiError } from '../lib/apiClient';

interface ChatMessage extends AiChatTurn { id: number }

export function AiChatPage() {
  const { isAuthenticated } = useAuth();
  const [searchParams] = useSearchParams();
  const recipeId = Number(searchParams.get('recipeId'));
  const hasRecipeContext = Number.isSafeInteger(recipeId) && recipeId > 0;
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [question, setQuestion] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [retryQuestion, setRetryQuestion] = useState('');
  const [sequence, setSequence] = useState(0);
  const contextType = hasRecipeContext ? 'RECIPE' : 'GENERAL';

  const pageTitle = useMemo(() => hasRecipeContext ? 'Hỏi về công thức' : 'Hỏi đáp ẩm thực chay', [hasRecipeContext]);

  async function send(questionText: string, previous: ChatMessage[], addQuestion: boolean) {
    const normalized = questionText.trim();
    if (!normalized || loading) return;
    setLoading(true);
    setError('');
    setRetryQuestion(normalized);
    const currentId = sequence + 1;
    setSequence(currentId);
    if (addQuestion) setMessages((current) => [...current, { id: currentId, speaker: 'USER', content: normalized }]);
    try {
      const response = await recipesApi.chat({
        contextType,
        ...(hasRecipeContext ? { recipeId } : {}),
        question: normalized,
        history: previous.slice(-8).map(({ speaker, content }) => ({ speaker, content })),
      });
      setMessages((current) => [...current, { id: currentId + 1, speaker: 'ASSISTANT', content: response.answer }]);
      setSequence(currentId + 1);
      setRetryQuestion('');
    } catch (cause) {
      const apiError = asApiError(cause);
      setError(apiError.status === 429
        ? 'Bạn đã gửi nhiều câu hỏi trong thời gian ngắn. Hãy thử lại sau; bạn cũng có thể đăng ký tài khoản để tiếp tục.'
        : apiError.code === 'AI_PROVIDER_UNAVAILABLE' || apiError.status === 0
          ? 'Dịch vụ trả lời đang gặp sự cố. Vui lòng thử lại sau.'
          : apiError.message);
    } finally {
      setLoading(false);
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalized = question.trim();
    if (!normalized) return;
    const previous = messages;
    setQuestion('');
    void send(normalized, previous, true);
  }

  if (!isAuthenticated) {
    return <PageContainer className="py-12"><section className="mx-auto max-w-xl rounded-2xl border border-brand-100 bg-paper p-6 text-center shadow-sm">
      <h1 className="text-2xl font-extrabold text-ink">{pageTitle}</h1>
      <p className="mt-3 text-sm leading-6 text-ink-muted">Đăng nhập để bắt đầu cuộc trò chuyện. Hỗ trợ khách chưa bật vì giới hạn tần suất Guest của FR-02 chưa được tích hợp.</p>
      <Link to="/dang-nhap" className="mt-5 inline-flex rounded-full bg-brand-600 px-5 py-2.5 text-sm font-semibold text-on-brand">Đăng nhập</Link>
      <Link to="/dang-ky" className="ml-3 mt-5 inline-flex rounded-full border border-brand-200 px-5 py-2.5 text-sm font-semibold text-brand-700">Đăng ký</Link>
    </section></PageContainer>;
  }

  return <PageContainer className="py-7 sm:py-10">
    <section className="mx-auto flex min-h-[70vh] max-w-3xl flex-col overflow-hidden rounded-2xl border border-brand-100 bg-paper shadow-sm" aria-labelledby="chat-heading">
      <header className="border-b border-brand-100 p-5 sm:p-6">
        <div className="flex items-center gap-3"><span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-50 text-brand-700"><Bot className="h-5 w-5" aria-hidden="true" /></span>
          <div><h1 id="chat-heading" className="text-xl font-extrabold text-ink">{pageTitle}</h1><p className="mt-1 text-xs text-ink-muted">{hasRecipeContext ? `Đang dùng thông tin công thức #${recipeId}` : 'Bạn có thể hỏi về nấu ăn và dinh dưỡng thực vật.'}</p></div>
        </div>
        <p className="mt-4 flex gap-2 rounded-xl bg-brand-50 px-3 py-2.5 text-xs leading-5 text-ink-soft"><ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-brand-700" aria-hidden="true" />Câu trả lời do AI tạo, chỉ mang tính tham khảo. Nội dung câu hỏi được gửi tới Google Gemini; cuộc trò chuyện chỉ giữ trong phiên trình duyệt này.</p>
      </header>

      <div className="flex-1 space-y-4 overflow-y-auto p-4 sm:p-6" aria-live="polite" aria-label="Cuộc trò chuyện">
        {messages.length === 0 && <div className="mx-auto max-w-md py-12 text-center"><Bot className="mx-auto h-8 w-8 text-brand-600" aria-hidden="true" /><p className="mt-3 font-semibold text-ink">Bạn muốn hỏi điều gì?</p><p className="mt-1 text-sm text-ink-muted">{hasRecipeContext ? 'Hỏi về nguyên liệu, cách chế biến hoặc món thay thế trong công thức này.' : 'Hỏi về nguyên liệu, cách nấu hoặc lựa chọn món chay.'}</p></div>}
        {messages.map((message) => <div key={message.id} className={`flex gap-3 ${message.speaker === 'USER' ? 'justify-end' : 'justify-start'}`}>
          {message.speaker === 'ASSISTANT' && <span className="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-700"><Bot className="h-4 w-4" aria-hidden="true" /></span>}
          <div className={`max-w-[85%] rounded-2xl px-4 py-3 text-sm leading-6 ${message.speaker === 'USER' ? 'bg-brand-600 text-on-brand' : 'bg-raised text-ink'}`}>
            <p className="mb-1 text-[11px] font-bold opacity-75">{message.speaker === 'USER' ? 'Bạn' : 'AI'}</p><p className="whitespace-pre-wrap">{message.content}</p>
          </div>
          {message.speaker === 'USER' && <span className="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-stone-100 text-ink-muted"><UserRound className="h-4 w-4" aria-hidden="true" /></span>}
        </div>)}
        {loading && <p role="status" className="flex items-center gap-2 text-sm text-ink-muted"><LoaderCircle className="h-4 w-4 animate-spin" />Đang trả lời…</p>}
        {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-900">{error}{retryQuestion && <button type="button" onClick={() => void send(retryQuestion, messages.slice(0, -1), false)} disabled={loading} className="ml-2 font-bold underline">Thử lại</button>}{error.includes('đăng ký') && <Link to="/dang-ky" className="ml-2 font-bold underline">Đăng ký</Link>}</div>}
      </div>

      <form onSubmit={submit} className="border-t border-brand-100 p-4 sm:p-5">
        <label htmlFor="ai-chat-question" className="sr-only">Câu hỏi của bạn</label>
        <div className="flex items-end gap-2 rounded-2xl border border-brand-200 bg-cream-soft p-2 focus-within:ring-2 focus-within:ring-brand-300">
          <textarea id="ai-chat-question" value={question} onChange={(event) => setQuestion(event.target.value)} maxLength={1000} rows={2} placeholder="Nhập câu hỏi của bạn…" className="max-h-36 min-h-12 flex-1 resize-y bg-transparent px-2 py-2 text-sm text-ink outline-none placeholder:text-ink-muted" disabled={loading} />
          <button type="submit" aria-label="Gửi câu hỏi" disabled={loading || !question.trim()} className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-600 text-on-brand enabled:hover:bg-brand-500 disabled:cursor-not-allowed disabled:opacity-50"><Send className="h-4 w-4" aria-hidden="true" /></button>
        </div>
        <p className="mt-2 text-right text-[11px] text-ink-muted">{question.length}/1.000 ký tự</p>
      </form>
    </section>
  </PageContainer>;
}
