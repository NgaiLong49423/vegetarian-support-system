import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { ImagePlus, LoaderCircle } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card, EmptyState } from '../components/ui';
import { MemberAvatar } from '../components/MemberAvatar';
import { useAuth } from '../components/AuthContext';
import { avatarFileError, AVATAR_TYPES, membersApi, type MemberProfile } from '../api/members';
import { asApiError } from '../lib/apiClient';

const inputClass = 'w-full rounded-xl border border-brand-200 bg-white px-3 py-2.5 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
const errorClass = 'mt-1 text-xs font-medium text-red-700';
const BIO_MAX = 500;

/** UC-23.3 own profile settings (`/ho-so/cai-dat`): name, short bio and avatar (AC-23.4–AC-23.7). */
export function ProfileSettingsPage() {
  const { isAuthenticated, account } = useAuth();
  if (!isAuthenticated) return <Navigate to="/dang-nhap" replace />;
  if (account?.role === 'ADMIN') {
    return <PageContainer className="py-12"><EmptyState title="Tài khoản quản trị không có hồ sơ thành viên" description="Hồ sơ công khai và phần cài đặt này chỉ dành cho Member." /></PageContainer>;
  }
  return <ProfileSettingsForm />;
}

function ProfileSettingsForm() {
  const { updateAccountProfile } = useAuth();
  const [profile, setProfile] = useState<MemberProfile | null>(null);
  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [loadError, setLoadError] = useState('');
  const [retry, setRetry] = useState(0);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [notice, setNotice] = useState('');
  const [saving, setSaving] = useState(false);
  const [avatarError, setAvatarError] = useState('');
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    let alive = true;
    setLoadError('');
    membersApi.getOwnProfile()
      .then((own) => {
        if (!alive) return;
        setProfile(own);
        setDisplayName(own.displayName);
        setBio(own.bio ?? '');
      })
      .catch((cause: unknown) => { if (alive) setLoadError(asApiError(cause).message); });
    return () => { alive = false; };
  }, [retry]);

  const applySaved = (saved: MemberProfile, message: string) => {
    setProfile(saved);
    setDisplayName(saved.displayName);
    setBio(saved.bio ?? '');
    updateAccountProfile(saved.displayName, saved.avatarUrl);
    setNotice(message);
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setNotice('');
    setFormError('');
    const trimmed = displayName.trim();
    if (trimmed.length < 3 || trimmed.length > 50) {
      setFieldErrors({ displayName: 'Tên hiển thị cần từ 3 đến 50 ký tự.' });
      return;
    }
    setFieldErrors({});
    setSaving(true);
    try {
      applySaved(await membersApi.updateOwnProfile({ displayName: trimmed, bio }), 'Đã lưu hồ sơ.');
    } catch (cause) {
      const failure = asApiError(cause);
      const byField: Record<string, string> = {};
      for (const item of failure.errors) byField[item.field] = byField[item.field] ? `${byField[item.field]} ${item.message}` : item.message;
      setFieldErrors(byField);
      if (!failure.errors.length) setFormError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  const chooseAvatar = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setNotice('');
    const problem = avatarFileError(file);
    if (problem) {
      setAvatarError(problem);
      return;
    }
    setAvatarError('');
    setUploading(true);
    try {
      applySaved(await membersApi.uploadAvatar(file), 'Đã cập nhật ảnh đại diện.');
    } catch (cause) {
      setAvatarError(asApiError(cause).message);
    } finally {
      setUploading(false);
    }
  };

  if (loadError) {
    return <PageContainer className="py-12"><EmptyState title="Không tải được hồ sơ" description={loadError} action={<Button variant="secondary" onClick={() => setRetry((current) => current + 1)}>Thử lại</Button>} /></PageContainer>;
  }
  if (!profile) {
    return <PageContainer className="py-12"><p role="status" className="text-center text-sm text-ink-muted">Đang tải hồ sơ...</p></PageContainer>;
  }

  return <PageContainer className="py-8">
    <div className="mx-auto max-w-2xl">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="text-2xl font-extrabold text-ink">Cài đặt hồ sơ</h1>
        <Link to={`/thanh-vien/${profile.userId}`} className="text-sm font-semibold text-brand-700 hover:underline">Xem hồ sơ công khai</Link>
      </div>
      {notice && <div role="status" className="mt-4 rounded-xl bg-leaf-50 p-3 text-sm text-leaf-800">{notice}</div>}

      <Card className="mt-5 p-5 sm:p-6">
        <h2 className="text-lg font-bold text-ink">Ảnh đại diện</h2>
        <div className="mt-4 flex flex-wrap items-center gap-4">
          <MemberAvatar name={profile.displayName} url={profile.avatarUrl} size="lg" />
          <div>
            <label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-brand-200 px-4 py-2.5 text-sm font-semibold text-brand-700 focus-within:ring-2 focus-within:ring-brand-100 hover:bg-brand-50">
              {uploading ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <ImagePlus className="h-4 w-4" />}
              {uploading ? 'Đang tải ảnh...' : 'Chọn ảnh mới'}
              <input type="file" accept={AVATAR_TYPES.join(',')} className="sr-only" disabled={uploading} onChange={(event) => void chooseAvatar(event)} aria-describedby="avatar-hint" />
            </label>
            <p id="avatar-hint" className="mt-2 text-xs text-ink-muted">JPEG, PNG hoặc WebP, tối đa 2 MB.</p>
            {avatarError && <p role="alert" className={errorClass}>{avatarError}</p>}
          </div>
        </div>
      </Card>

      <Card className="mt-5 p-5 sm:p-6">
        <form noValidate onSubmit={(event) => void submit(event)} className="space-y-5">
          <div>
            <label htmlFor="profile-name" className="block text-sm font-semibold text-ink">Tên hiển thị</label>
            <input id="profile-name" value={displayName} maxLength={50} onChange={(event) => setDisplayName(event.target.value)} aria-invalid={Boolean(fieldErrors.displayName)} aria-describedby={fieldErrors.displayName ? 'profile-name-error' : undefined} className={`mt-1.5 ${inputClass}`} />
            {fieldErrors.displayName && <p id="profile-name-error" role="alert" className={errorClass}>{fieldErrors.displayName}</p>}
          </div>
          <div>
            <label htmlFor="profile-bio" className="block text-sm font-semibold text-ink">Giới thiệu ngắn <span className="font-normal text-ink-muted">(không bắt buộc)</span></label>
            <textarea id="profile-bio" value={bio} maxLength={BIO_MAX} rows={4} onChange={(event) => setBio(event.target.value)} aria-invalid={Boolean(fieldErrors.bio)} aria-describedby="profile-bio-count" className={`mt-1.5 ${inputClass}`} />
            <p id="profile-bio-count" className="mt-1 text-right text-xs text-ink-muted">{bio.length}/{BIO_MAX}</p>
            {fieldErrors.bio && <p role="alert" className={errorClass}>{fieldErrors.bio}</p>}
          </div>
          {formError && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">{formError}</div>}
          <Button type="submit" disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu thay đổi'}</Button>
        </form>
      </Card>
    </div>
  </PageContainer>;
}
