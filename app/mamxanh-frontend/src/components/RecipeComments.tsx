import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { LoaderCircle, MessageCircle } from 'lucide-react';
import {
  commentsApi,
  MAX_COMMENT_DEPTH,
  type RecipeComment,
} from '../api/comments';
import { asApiError } from '../lib/apiClient';
import { useAuth } from './AuthContext';
import { Button, Card } from './ui';

const TOMBSTONE = 'Bình luận này đã bị xóa bởi người dùng';

/** Adds a new reply at the end of its parent's replies. */
function withReply(nodes: RecipeComment[], reply: RecipeComment): RecipeComment[] {
  return nodes.map((node) => node.commentId === reply.parentCommentId
    ? { ...node, replies: [...node.replies, reply] }
    : { ...node, replies: withReply(node.replies, reply) });
}

function withEdit(nodes: RecipeComment[], edited: RecipeComment): RecipeComment[] {
  return nodes.map((node) => node.commentId === edited.commentId
    ? { ...node, content: edited.content, edited: edited.edited }
    : { ...node, replies: withEdit(node.replies, edited) });
}

/** Same rule as the Backend: a deleted comment stays as a tombstone only while it has visible replies. */
function withoutComment(nodes: RecipeComment[], commentId: number): RecipeComment[] {
  return nodes.flatMap((node) => {
    const replies = withoutComment(node.replies, commentId);
    if (node.commentId === commentId) {
      return replies.length ? [{ ...node, deleted: true, content: null, author: null, edited: false, replies }] : [];
    }
    if (node.deleted && replies.length === 0) return [];
    return [{ ...node, replies }];
  });
}

function formatTime(createdAt: string) {
  return new Date(`${createdAt}Z`).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

export function RecipeComments({ recipeId, className = 'w-full' }: { recipeId: number; className?: string }) {
  const { account, isAuthenticated } = useAuth();
  const canWrite = isAuthenticated && account?.role !== 'ADMIN';
  const [threads, setThreads] = useState<RecipeComment[]>([]);
  const [commentCount, setCommentCount] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loadState, setLoadState] = useState<'loading' | 'ready' | 'error'>('loading');
  const [loadingMore, setLoadingMore] = useState(false);
  const [body, setBody] = useState('');
  const [replyTo, setReplyTo] = useState<RecipeComment | null>(null);
  const [editing, setEditing] = useState<RecipeComment | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const composer = useRef<HTMLTextAreaElement>(null);

  const load = useCallback(async (nextPage: number) => {
    try {
      const result = await commentsApi.list(recipeId, nextPage);
      setThreads((current) => nextPage === 0 ? (result?.content ?? []) : [
        ...current,
        ...(result?.content ?? []).filter((thread) => !current.some((item) => item.commentId === thread.commentId)),
      ]);
      setCommentCount(result?.commentCount ?? result?.totalElements ?? 0);
      setPage(result?.page ?? 0);
      setTotalPages(result?.totalPages ?? 0);
      setLoadState('ready');
    } catch (err: unknown) {
      const apiErr = asApiError(err);
      if (apiErr.status === 404) {
        setThreads([]);
        setCommentCount(0);
        setPage(0);
        setTotalPages(0);
        setLoadState('ready');
      } else {
        setLoadState('error');
      }
    }
  }, [recipeId]);

  const loadFirstPage = useCallback(() => {
    setLoadState('loading');
    void load(0);
  }, [load]);

  useEffect(() => { loadFirstPage(); }, [loadFirstPage]);

  const loadMore = () => {
    setLoadingMore(true);
    load(page + 1).catch(() => setError('Không tải thêm được bình luận. Vui lòng thử lại.')).finally(() => setLoadingMore(false));
  };

  const resetComposer = () => {
    setBody('');
    setReplyTo(null);
    setEditing(null);
    setError('');
  };

  const startReply = (comment: RecipeComment) => {
    setEditing(null);
    setReplyTo(comment);
    setError('');
    setBody(comment.depth >= MAX_COMMENT_DEPTH && comment.author ? `@${comment.author.displayName} ` : '');
    composer.current?.focus();
  };

  const startEdit = (comment: RecipeComment) => {
    setReplyTo(null);
    setEditing(comment);
    setError('');
    setBody(comment.content ?? '');
    composer.current?.focus();
  };

  const submit = async () => {
    const content = body.trim();
    if (!content) return;
    setSubmitting(true);
    setError('');
    try {
      if (editing) {
        const edited = await commentsApi.update(editing.commentId, content);
        setThreads((current) => withEdit(current, edited));
      } else {
        const created = await commentsApi.create(recipeId, content, replyTo?.commentId);
        setThreads((current) => created.parentCommentId === null ? [created, ...current] : withReply(current, created));
        setCommentCount((count) => count + 1);
      }
      resetComposer();
    } catch (failure) {
      const problem = asApiError(failure);
      setError(problem.errors[0]?.message ?? problem.message);
      if (problem.code === 'COMMENT_NOT_FOUND') loadFirstPage();
    } finally {
      setSubmitting(false);
    }
  };

  const remove = async (comment: RecipeComment) => {
    setConfirmDelete(null);
    try {
      await commentsApi.remove(comment.commentId);
      setThreads((current) => withoutComment(current, comment.commentId));
      setCommentCount((count) => Math.max(0, count - 1));
      if (editing?.commentId === comment.commentId || replyTo?.commentId === comment.commentId) resetComposer();
    } catch (failure) {
      setError(asApiError(failure).message);
    }
  };

  const renderComment = (comment: RecipeComment) => {
    const own = !comment.deleted && comment.author?.userId === account?.id;
    return (
      <article
        key={comment.commentId}
        aria-label={comment.deleted ? 'Bình luận đã xóa' : `Bình luận của ${comment.author?.displayName ?? ''}`}
        className={`border-l border-brand-100 py-3 pl-3 ${comment.depth > 1 ? 'ml-2 sm:ml-6' : ''}`}
      >
        <div className="flex gap-3">
          {comment.author?.avatarUrl
            ? <img src={comment.author.avatarUrl} alt="" className="h-8 w-8 shrink-0 rounded-full object-cover" />
            : <span aria-hidden="true" className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-100 text-xs font-bold text-brand-700">{comment.author?.displayName.charAt(0).toUpperCase() ?? '·'}</span>}
          <div className="min-w-0 flex-1">
            {comment.deleted
              ? <p className="text-sm italic text-ink-muted">{TOMBSTONE}</p>
              : (
                <>
                  <p className="text-sm font-bold text-ink">
                    {comment.author?.displayName}{' '}
                    <span className="font-normal text-ink-muted">· {formatTime(comment.createdAt)}</span>{' '}
                    {comment.edited && <span className="font-normal text-ink-muted">(Đã chỉnh sửa)</span>}
                  </p>
                  <p className="mt-1 whitespace-pre-wrap break-words text-sm text-ink-soft">{comment.content}</p>
                </>
              )}
            {canWrite && !comment.deleted && (
              confirmDelete === comment.commentId
                ? (
                  <div className="mt-2 flex flex-wrap items-center gap-3 text-xs font-semibold">
                    <span className="text-ink">Xóa bình luận này?</span>
                    <button type="button" className="text-red-700" onClick={() => void remove(comment)}>Xác nhận xóa</button>
                    <button type="button" className="text-brand-700" onClick={() => setConfirmDelete(null)}>Không xóa</button>
                  </div>
                )
                : (
                  <div className="mt-2 flex gap-3 text-xs font-semibold text-brand-700">
                    <button type="button" onClick={() => startReply(comment)}>Trả lời</button>
                    {own && <button type="button" onClick={() => startEdit(comment)}>Sửa</button>}
                    {own && <button type="button" onClick={() => setConfirmDelete(comment.commentId)}>Xóa</button>}
                  </div>
                )
            )}
          </div>
        </div>
        {comment.replies.map(renderComment)}
      </article>
    );
  };

  return (
    <section aria-labelledby="recipe-comments-heading" className={className}>
      <Card className="p-5 sm:p-6">
        <h2 id="recipe-comments-heading" className="flex items-center gap-2 text-xl font-extrabold text-ink">
          <MessageCircle className="h-5 w-5 text-brand-600" /> Bình luận ({commentCount})
        </h2>
        <div className="mt-5">
          {loadState === 'loading' && (
            <p role="status" className="flex items-center gap-2 text-sm text-ink-muted"><LoaderCircle className="h-4 w-4 animate-spin" />Đang tải bình luận…</p>
          )}
          {loadState === 'error' && (
            <div role="alert" className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-red-50 p-4 text-sm text-red-800">
              <span>Không tải được bình luận. Vui lòng thử lại.</span>
              <Button size="sm" variant="outline" onClick={loadFirstPage}>Thử lại</Button>
            </div>
          )}
          {loadState === 'ready' && (threads.length
            ? threads.map(renderComment)
            : <p className="rounded-xl bg-brand-50 p-4 text-sm text-ink-muted">Chưa có bình luận. Hãy bắt đầu cuộc trò chuyện.</p>)}
          {loadState === 'ready' && page + 1 < totalPages && (
            <Button variant="outline" size="sm" className="mt-3" disabled={loadingMore} onClick={loadMore}>
              {loadingMore ? 'Đang tải…' : 'Xem thêm bình luận'}
            </Button>
          )}
        </div>

        {!isAuthenticated && (
          <p className="mt-5 text-sm text-ink-muted">
            <Link to="/dang-nhap" className="font-bold text-brand-700">Đăng nhập để bình luận</Link> và trao đổi với cộng đồng.
          </p>
        )}
        {isAuthenticated && !canWrite && (
          <p className="mt-5 text-sm text-ink-muted">Tài khoản quản trị viên không đăng bình luận.</p>
        )}
        {canWrite && (
          <div className="mt-5">
            {(replyTo || editing) && (
              <div className="mb-2 flex items-center justify-between text-sm text-brand-700">
                <span>{editing ? 'Đang sửa bình luận' : `Đang trả lời ${replyTo?.author?.displayName ?? ''}`}</span>
                <button type="button" onClick={resetComposer}>Hủy</button>
              </div>
            )}
            <label htmlFor="recipe-comment" className="block text-sm font-semibold text-ink">Viết bình luận</label>
            <textarea
              id="recipe-comment"
              ref={composer}
              value={body}
              onChange={(event) => { setBody(event.target.value); setError(''); }}
              maxLength={1000}
              rows={3}
              placeholder="Chia sẻ câu hỏi hoặc kinh nghiệm nấu món này..."
              className="mt-2 w-full rounded-xl border border-brand-200 bg-paper p-3 text-sm outline-none focus:border-brand-500"
            />
            <div className="mt-2 flex items-center justify-between gap-3">
              <span className="text-xs text-ink-muted">{body.length}/1.000 ký tự</span>
              <Button onClick={() => void submit()} disabled={submitting || !body.trim()}>
                {editing ? 'Lưu chỉnh sửa' : replyTo ? 'Gửi trả lời' : 'Gửi bình luận'}
              </Button>
            </div>
          </div>
        )}
        {error && <p role="alert" className="mt-2 text-sm text-red-700">{error}</p>}
      </Card>
    </section>
  );
}
