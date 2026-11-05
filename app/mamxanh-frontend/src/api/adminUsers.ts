import { apiClient, asApiError } from '../lib/apiClient';

export type AdminUserRole = 'CUSTOMER' | 'EXPERT' | 'ADMIN';
export type AdminUserStatus = 'ACTIVE' | 'LOCKED';
export type AdminAiTier = 'FREE' | 'PLUS' | 'PRO';

export interface AdminUser {
  id: number;
  displayName: string;
  email: string;
  role: AdminUserRole;
  accountStatus: AdminUserStatus;
  joinedAt: string;
  publishedRecipeCount: number;
}

export interface AdminUserAudit {
  eventId: string;
  action: 'STATUS_CHANGED' | 'ROLE_CHANGED' | 'AI_PACKAGE_GIFTED' | 'AI_CREDIT_GRANTED' | 'ACCOUNT_LOCKED';
  oldValue: string;
  newValue: string;
  reason: string;
  administratorId: number | null;
  administratorName: string | null;
  occurredAt: string;
  effectiveUntil: string | null;
}

export interface AdminAiPackageState {
  userId: number;
  oldTier: AdminAiTier;
  newTier: AdminAiTier;
  startsAt: string | null;
  endsAt: string | null;
  tokenQuota: number;
  changed: boolean;
  effectiveAt: string;
}

export interface AdminAiCreditGrant {
  userId: number;
  credits: number;
  balance: number;
  grantedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminUserSummary {
  totalUsers: number;
  activeUsers: number;
  expertUsers: number;
  lockedUsers: number;
}

export async function listAdminUsers(params: { query?: string; role?: string; status?: string; page: number; size: number }) {
  try { return (await apiClient.get<PageResponse<AdminUser>>('/admin/users', { params })).data; }
  catch (error) { throw asApiError(error); }
}

export async function getAdminUserSummary() {
  try { return (await apiClient.get<AdminUserSummary>('/admin/users/summary')).data; }
  catch (error) { throw asApiError(error); }
}

export async function getAdminUser(userId: number) {
  try { return (await apiClient.get<AdminUser>(`/admin/users/${userId}`)).data; }
  catch (error) { throw asApiError(error); }
}

export async function getAdminUserHistory(userId: number, page = 0, size = 20) {
  try { return (await apiClient.get<PageResponse<AdminUserAudit>>(`/admin/users/${userId}/history`, { params: { page, size } })).data; }
  catch (error) { throw asApiError(error); }
}

export async function updateAdminUserStatus(userId: number, accountStatus: AdminUserStatus, reason: string) {
  try { return (await apiClient.patch<AdminUser>(`/admin/users/${userId}/status`, { accountStatus, reason })).data; }
  catch (error) { throw asApiError(error); }
}

export async function updateAdminUserRole(userId: number, role: Exclude<AdminUserRole, 'ADMIN'>, reason: string) {
  try { return (await apiClient.patch<AdminUser>(`/admin/users/${userId}/role`, { role, reason })).data; }
  catch (error) { throw asApiError(error); }
}

export async function getAdminAiTier(userId: number) {
  try { return (await apiClient.get<AdminAiPackageState>(`/admin/ai-accounts/${userId}/tier`)).data; }
  catch (error) { throw asApiError(error); }
}

export async function giftAdminAiPackage(userId: number, tier: AdminAiTier, reason: string) {
  try { return (await apiClient.post<AdminAiPackageState>(`/admin/ai-accounts/${userId}/package-gifts`, { tier, reason })).data; }
  catch (error) { throw asApiError(error); }
}

export async function grantAdminAiCredit(userId: number, credits: number, reason: string) {
  try { return (await apiClient.post<AdminAiCreditGrant>(`/admin/ai-accounts/${userId}/credit-grants`, { credits, reason })).data; }
  catch (error) { throw asApiError(error); }
}
