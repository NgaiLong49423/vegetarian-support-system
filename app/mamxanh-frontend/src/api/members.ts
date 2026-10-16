import { apiClient, asApiError } from '../lib/apiClient';
import type { RecipeSearchResult } from './recipes';

/** FR-23 public profile (`GET /members/{userId}`); never contains email, role or private data. */
export interface MemberProfile {
  userId: number;
  displayName: string;
  avatarUrl: string | null;
  bio: string | null;
  /** Month the member joined, `yyyy-MM` in Vietnam time. */
  joinedMonth: string;
}

/** FR-59 follow state of a profile; counts include only active accounts (AC-59.7). */
export interface FollowStatus {
  userId: number;
  /** Whether the signed-in viewer follows this member; always false for a Guest. */
  following: boolean;
  followerCount: number;
  followingCount: number;
}

/** Entry of a followers/following list: public profile fields only (AC-59.6). */
export interface FollowMember {
  userId: number;
  displayName: string;
  avatarUrl: string | null;
}

export interface FollowMemberPage {
  content: FollowMember[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const FOLLOW_PAGE_SIZE = 20;

export interface UpdateProfilePayload {
  displayName: string;
  bio: string;
}

export const AVATAR_MAX_BYTES = 2 * 1024 * 1024;
export const AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

async function request<T>(operation: () => Promise<{ data: T }>): Promise<T> {
  try {
    return (await operation()).data;
  } catch (error) {
    throw asApiError(error);
  }
}

export const membersApi = {
  getProfile: (userId: number) => request(() => apiClient.get<MemberProfile>(`/members/${userId}`)),
  getRecipes: (userId: number, page: number, size = 12) => request(
    () => apiClient.get<RecipeSearchResult>(`/members/${userId}/recipes`, { params: { page, size } }),
  ),
  getFollowStatus: (userId: number) => request(() => apiClient.get<FollowStatus>(`/members/${userId}/follow`)),
  follow: (userId: number) => request(() => apiClient.put<FollowStatus>(`/members/${userId}/follow`)),
  unfollow: (userId: number) => request(() => apiClient.delete<FollowStatus>(`/members/${userId}/follow`)),
  getFollowList: (userId: number, kind: 'followers' | 'following', page: number) => request(
    () => apiClient.get<FollowMemberPage>(`/members/${userId}/${kind}`, { params: { page, size: FOLLOW_PAGE_SIZE } }),
  ),
  getOwnProfile: () => request(() => apiClient.get<MemberProfile>('/me/profile')),
  updateOwnProfile: (payload: UpdateProfilePayload) => request(() => apiClient.put<MemberProfile>('/me/profile', payload)),
  uploadAvatar: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return request(() => apiClient.post<MemberProfile>('/me/profile/avatar', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }));
  },
};

/** Client-side check before upload (AC-23.7); the Backend repeats it and also checks the file content. */
export function avatarFileError(file: File): string | null {
  if (!AVATAR_TYPES.includes(file.type)) return 'Ảnh đại diện phải là tệp JPEG, PNG hoặc WebP.';
  if (file.size > AVATAR_MAX_BYTES) return 'Ảnh đại diện tối đa 2 MB.';
  return null;
}

/** `2026-09` → `Tháng 9/2026` (AC-23.2: join time shown as month/year). */
export function joinedMonthLabel(joinedMonth: string): string {
  const [year, month] = joinedMonth.split('-');
  return `Tháng ${Number(month)}/${year}`;
}
