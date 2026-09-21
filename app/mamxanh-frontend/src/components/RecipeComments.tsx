import { useState, type ReactNode } from 'react';
import { MessageCircle } from 'lucide-react';
import { currentUser } from '../data/mockData';
import { Button, Card } from './ui';

type Comment = {
  id: string;
  parentId: string | null;
  author: string;
  avatar: string;
  body: string;
  depth: number;
  edited?: boolean;
  deleted?: boolean;
};

export function RecipeComments() {
  const [comments, setComments] = useState<Comment[]>([]);
  const [body, setBody] = useState('');
  const [replyTo, setReplyTo] = useState<string | null>(null);
  const [editing, setEditing] = useState<string | null>(null);
  const [error, setError] = useState('');

  const submit = () => {
    const content = body.trim();
    if (!content || content.length > 1000) {
      setError('Bình luận cần từ 1 đến 1.000 ký tự.');
      return;
    }
    if (editing) {
      setComments((items) => items.map((item) => item.id === editing && item.author === currentUser.name
        ? { ...item, body: content, edited: true } : item));
    } else {
      const target = comments.find((item) => item.id === replyTo);
      const parentId = target && target.depth >= 5 ? target.parentId : replyTo;
      const depth = target ? Math.min(target.depth + 1, 5) : 1;
      setComments((items) => [...items, {
        id: crypto.randomUUID(), parentId, depth, author: currentUser.name,
        avatar: currentUser.avatar, body: target && target.depth >= 5 ? `@${target.author} ${content}` : content,
      }]);
    }
    setBody('');
    setReplyTo(null);
    setEditing(null);
    setError('');
  };

  const remove = (id: string) => {
    setComments((items) => items.some((item) => item.parentId === id)
      ? items.map((item) => item.id === id ? { ...item, body: '', deleted: true } : item)
      : items.filter((item) => item.id !== id));
  };

  const render = (parentId: string | null): ReactNode => comments
    .filter((item) => item.parentId === parentId)
    .map((item) => (
      <div key={item.id} className={`border-l border-brand-100 py-3 pl-3 ${item.depth > 1 ? 'ml-3 sm:ml-6' : ''}`}>
        <div className="flex gap-3">
          <img src={item.avatar} alt="" className="h-8 w-8 rounded-full object-cover" />
          <div className="min-w-0 flex-1">
            <p className="text-sm font-bold text-ink">{item.author} {item.edited && <span className="font-normal text-ink-muted">(Đã chỉnh sửa)</span>}</p>
            <p className="mt-1 whitespace-pre-wrap break-words text-sm text-ink-soft">{item.deleted ? 'Bình luận này đã bị xóa bởi người dùng' : item.body}</p>
            {!item.deleted && <div className="mt-2 flex gap-3 text-xs font-semibold text-brand-600">
              <button onClick={() => { setReplyTo(item.id); setEditing(null); setBody(''); }}>Trả lời</button>
              {item.author === currentUser.name && <>
                <button onClick={() => { setEditing(item.id); setReplyTo(null); setBody(item.body); }}>Sửa</button>
                <button onClick={() => remove(item.id)}>Xóa</button>
              </>}
            </div>}
          </div>
        </div>
        {render(item.id)}
      </div>
    ));

  return <Card className="mb-10 p-6">
    <h2 className="flex items-center gap-2 text-xl font-extrabold text-ink"><MessageCircle className="h-5 w-5 text-brand-600" /> Bình luận ({comments.length})</h2>
    <p className="mt-1 text-xs text-ink-muted">Bản xem trước giao diện: bình luận chỉ tồn tại trong phiên này, chưa gửi tới máy chủ.</p>
    <div className="mt-5">
      {comments.length ? render(null) : <p className="rounded-xl bg-brand-50 p-4 text-sm text-ink-muted">Chưa có bình luận. Hãy bắt đầu cuộc trò chuyện.</p>}
    </div>
    {(replyTo || editing) && <div className="mt-4 flex items-center justify-between text-sm text-brand-600">
      <span>{editing ? 'Đang sửa bình luận' : `Đang trả lời ${comments.find((item) => item.id === replyTo)?.author}`}</span>
      <button onClick={() => { setReplyTo(null); setEditing(null); setBody(''); }}>Hủy</button>
    </div>}
    <label htmlFor="recipe-comment" className="mt-5 block text-sm font-semibold text-ink">Viết bình luận</label>
    <textarea id="recipe-comment" value={body} onChange={(event) => { setBody(event.target.value); setError(''); }} maxLength={1000}
      rows={3} placeholder="Chia sẻ câu hỏi hoặc kinh nghiệm nấu món này..."
      className="mt-2 w-full rounded-xl border border-brand-200 bg-white p-3 text-sm outline-none focus:border-brand-500" />
    <div className="mt-2 flex items-center justify-between gap-3">
      <span className="text-xs text-ink-muted">{body.length}/1.000 ký tự</span>
      <Button onClick={submit}>{editing ? 'Lưu chỉnh sửa' : replyTo ? 'Gửi trả lời' : 'Gửi bình luận'}</Button>
    </div>
    {error && <p role="alert" className="mt-2 text-sm text-red-600">{error}</p>}
  </Card>;
}
