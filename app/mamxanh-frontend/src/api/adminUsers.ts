import { apiClient, asApiError } from '../lib/apiClient';

export type AdminUserRole = 'CUSTOMER' | 'EXPERT' | 'ADMIN';
export type AdminUserStatus = 'ACTIVE' | 'LOCKED';

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
  action: 'STATUS_CHANGED' | 'ROLE_CHANGED' | 'ACCOUNT_LOCKED';
  oldValue: string;
  newValue: string;
  reason: string;
  administratorId: number | null;
  administratorName: string | null;
  occurredAt: string;
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
