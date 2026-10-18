import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { UserCheck, UserPlus } from 'lucide-react';
import { membersApi, type FollowMember, type FollowStatus } from '../api/members';
import { asApiError } from '../lib/apiClient';
import { useAuth } from './AuthContext';
import { MemberAvatar } from './MemberAvatar';
import { Button } from './ui';

type ListKind = 'followers' | 'following';
const LIST_TITLES: Record<ListKind, string> = { followers: 'Người theo dõi', following: 'Đang theo dõi' };
const EMPTY_LIST: Record<ListKind, string> = {
  followers: 'Chưa có người theo dõi.',
  following: 'Chưa theo dõi thành viên nào.',
};

/**
 * FR-59 on a public profile: follow counts (UC-59.3), follow/unfollow (UC-59.1, UC-59.2) and the
 * follower/following lists (UC-59.4). The Backend decides who may follow; this only hides the
 * button where it can never succeed (own profile, Administrator) and sends a Guest to sign in.
 */
export function MemberFollowPanel({ userId }: { userId: number }) {
  const { account, isAuthenticated } = useAuth();
  const [status, setStatus] = useState<FollowStatus | null>(null);
  const [summaryFailed, setSummaryFailed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [openList, setOpenList] = useState<ListKind | null>(null);
  const [members, setMembers] = useState<FollowMember[]>([]);
  const [listPage, setListPage] = useState(0);
  const [listPages, setListPages] = useState(0);
  const [listState, setListState] = useState<'idle' | 'loading' | 'error'>('idle');
  const canFollow = isAuthenticated && account?.role !== 'ADMIN' && account?.id !== userId;

  const loadSummary = useCallback(() => {
    setSummaryFailed(false);
    membersApi.getFollowStatus(userId).then(setStatus).catch(() => setSummaryFailed(true));
  }, [userId]);

  useEffect(() => { loadSummary(); }, [loadSummary]);

  const loadList = useCallback(async (kind: ListKind, page: number) => {
    setListState('loading');
    try {
      const result = await membersApi.getFollowList(userId, kind, page);
      setMembers((current) => page === 0 ? result.content : [...current, ...result.content]);
      setListPage(result.page);
      setListPages(result.totalPages);
      setListState('idle');
    } catch {
      setListState('error');
    }
  }, [userId]);

  const showList = (kind: ListKind) => {
    setOpenList(kind);
    setMembers([]);
    if (isAuthenticated) void loadList(kind, 0);
  };

  const toggleFollow = async () => {
    if (!status) return;
    setBusy(true);
    setError('');
    try {
      setStatus(status.following ? await membersApi.unfollow(userId) : await membersApi.follow(userId));
    } catch (failure) {
      setError(asApiError(failure).message);
    } finally {
      setBusy(false);
    }
  };

  if (summaryFailed) {
    return <p className="mt-4 flex flex-wrap items-center gap-3 text-sm text-ink-muted">
      Không tải được thông tin theo dõi.
      <Button variant="outline" size="sm" onClick={loadSummary}>Tải lại</Button>
    </p>;
  }
  if (!status) return null;

  return <div className="mt-4">
    <div className="flex flex-wrap items-center justify-center gap-3 sm:justify-start">
      <button type="button" onClick={() => showList('followers')} aria-expanded={openList === 'followers'}
        className="text-sm text-ink-soft hover:text-brand-700"><strong className="text-ink">{status.followerCount}</strong> người theo dõi</button>
      <button type="button" onClick={() => showList('following')} aria-expanded={openList === 'following'}
        className="text-sm text-ink-soft hover:text-brand-700">Đang theo dõi <strong className="text-ink">{status.followingCount}</strong></button>
      {!isAuthenticated && <Link to="/dang-nhap" className="inline-flex items-center gap-1.5 rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-on-brand hover:bg-brand-700"><UserPlus className="h-4 w-4" />Theo dõi</Link>}
      {canFollow && <Button size="sm" variant={status.following ? 'outline' : 'primary'} aria-pressed={status.following} disabled={busy} onClick={() => void toggleFollow()}>
        {status.following ? <><UserCheck className="h-4 w-4" />Đang theo dõi</> : <><UserPlus className="h-4 w-4" />Theo dõi</>}
      </Button>}
    </div>
    {error && <p role="alert" className="mt-2 text-sm text-red-700">{error}</p>}

    {openList && <section aria-label={LIST_TITLES[openList]} className="mt-4 rounded-2xl border border-brand-100 bg-paper p-4 text-left">
      <h2 className="text-base font-bold text-ink">{LIST_TITLES[openList]}</h2>
      {!isAuthenticated
        ? <p className="mt-2 text-sm text-ink-muted"><Link to="/dang-nhap" className="font-semibold text-brand-700">Đăng nhập</Link> để xem danh sách {openList === 'followers' ? 'người theo dõi' : 'đang theo dõi'}.</p>
        : <>
          {members.length > 0 && <ul className="mt-3 divide-y divide-brand-100">
            {members.map((item) => <li key={item.userId} className="py-2">
              <Link to={`/thanh-vien/${item.userId}`} className="flex items-center gap-3 text-sm font-semibold text-ink hover:text-brand-700">
                <span aria-hidden="true"><MemberAvatar name={item.displayName} url={item.avatarUrl} size="sm" /></span>{item.displayName}
              </Link>
            </li>)}
          </ul>}
          {listState === 'idle' && members.length === 0 && <p className="mt-2 text-sm text-ink-muted">{EMPTY_LIST[openList]}</p>}
          {listState === 'loading' && <p role="status" className="mt-2 text-sm text-ink-muted">Đang tải danh sách…</p>}
          {listState === 'error' && <p className="mt-2 flex items-center gap-3 text-sm text-ink-muted">Không tải được danh sách.
            <Button variant="outline" size="sm" onClick={() => void loadList(openList, members.length ? listPage + 1 : 0)}>Thử lại</Button></p>}
          {listState === 'idle' && listPage + 1 < listPages && <Button variant="outline" size="sm" className="mt-3" onClick={() => void loadList(openList, listPage + 1)}>Xem thêm</Button>}
        </>}
    </section>}
  </div>;
}
