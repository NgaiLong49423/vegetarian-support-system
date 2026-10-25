import { apiClient } from '../lib/apiClient';
import type { PageResponse } from './expertApplications';

export type ReportStatus = 'OPEN' | 'IN_REVIEW' | 'RESOLVED';
export type ReportDecision = 'NO_VIOLATION' | 'WARN_AUTHOR' | 'HIDE_RECIPE' | 'LOCK_AUTHOR';
export type ReportAccount = {
  id: number;
  displayName: string;
  email: string;
  role: string;
  accountStatus: string;
};
export type AdminReport = {
  id: number;
  status: ReportStatus;
  reasonCode: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
  recipe: { id: number; title: string; status: string };
  reporter: ReportAccount;
  author: ReportAccount;
  handledBy: { id: number; displayName: string } | null;
  decision: ReportDecision | null;
  decisionReason: string | null;
  handledAt: string | null;
};
export type ReportStatusCounts = { open: number; inReview: number; resolved: number };

export const adminReportsApi = {
  list: async (status: ReportStatus | '', page: number) =>
    (await apiClient.get<PageResponse<AdminReport>>('/admin/reports', { params: { status: status || undefined, page, size: 20 } })).data,
  counts: async () => (await apiClient.get<ReportStatusCounts>('/admin/reports/status-counts')).data,
  detail: async (id: number) => (await apiClient.get<AdminReport>(`/admin/reports/${id}`)).data,
  claim: async (id: number) => (await apiClient.post<AdminReport>(`/admin/reports/${id}/claim`)).data,
  resolve: async (id: number, decision: ReportDecision, reason: string) =>
    (await apiClient.post<AdminReport>(`/admin/reports/${id}/resolve`, { decision, reason })).data,
};
