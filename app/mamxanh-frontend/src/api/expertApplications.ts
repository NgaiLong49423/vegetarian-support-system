import { apiClient } from '../lib/apiClient';

export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type ExpertApplication = {
  id: number;
  userId: number;
  displayName: string;
  email: string;
  experience: string;
  vegetarianType: string;
  sampleRecipeSummary: string;
  portfolioUrl: string | null;
  status: ApplicationStatus;
  adminNote: string | null;
  submittedAt: string;
  reviewedAt: string | null;
};
export type PageResponse<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number };

export const expertApplicationsApi = {
  submit: async (payload: { experience: string; vegetarianType: string; sampleRecipeSummary: string; portfolioUrl?: string }) =>
    (await apiClient.post<ExpertApplication>('/expert-applications', payload)).data,
  history: async (page: number) => (await apiClient.get<PageResponse<ExpertApplication>>('/expert-applications/me', { params: { page, size: 20 } })).data,
  list: async (status: string, page: number) => (await apiClient.get<PageResponse<ExpertApplication>>('/admin/expert-applications', { params: { status: status || undefined, page, size: 20 } })).data,
  detail: async (id: number) => (await apiClient.get<ExpertApplication>(`/admin/expert-applications/${id}`)).data,
  approve: async (id: number, note?: string) => (await apiClient.post<ExpertApplication>(`/admin/expert-applications/${id}/approve`, { note })).data,
  reject: async (id: number, reason: string) => (await apiClient.post<ExpertApplication>(`/admin/expert-applications/${id}/reject`, { reason })).data,
};
