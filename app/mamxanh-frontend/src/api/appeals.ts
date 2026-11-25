import { apiClient } from '../lib/apiClient';
import type { PageResponse } from './expertApplications';
import type { RecipePost } from './recipes';

/** FR-28 / BR-27: an Expert's appeal against the hide decision on their own Recipe Post. */
export type AppealStatus = 'PENDING' | 'EDIT_ALLOWED' | 'RESUBMITTED' | 'RESTORED' | 'KEPT_HIDDEN';
export type AppealDecision = 'RESTORE' | 'KEEP_HIDDEN' | 'ALLOW_EDIT';

export type RecipeAppeal = {
  reportId: number;
  recipeId: number;
  recipeTitle: string;
  hideReason: string;
  hiddenAt: string;
  appealStatus: AppealStatus | null;
  appealMessage: string | null;
  appealedAt: string | null;
  resubmittedAt: string | null;
  adminReason: string | null;
  decidedAt: string | null;
};

export type AdminAppeal = {
  appeal: RecipeAppeal;
  author: { id: number; displayName: string; email: string; accountStatus: string };
  recipe: RecipePost | null;
};

export const APPEAL_MESSAGE_MIN = 10;
export const APPEAL_MESSAGE_MAX = 1000;

export const recipeAppealsApi = {
  get: async (recipeId: number) => (await apiClient.get<RecipeAppeal>(`/recipes/${recipeId}/appeal`)).data,
  submit: async (recipeId: number, message: string) =>
    (await apiClient.post<RecipeAppeal>(`/recipes/${recipeId}/appeal`, { message })).data,
  resubmit: async (recipeId: number) => (await apiClient.post<RecipeAppeal>(`/recipes/${recipeId}/appeal/resubmit`)).data,
};

export const adminAppealsApi = {
  list: async (statuses: AppealStatus[], page: number) =>
    (await apiClient.get<PageResponse<AdminAppeal>>('/admin/appeals', {
      params: { status: statuses.length ? statuses : undefined, page, size: 20 },
      // Spring reads a repeated parameter (status=A&status=B), not axios' default status[]=A.
      paramsSerializer: { indexes: null },
    })).data,
  detail: async (reportId: number) => (await apiClient.get<AdminAppeal>(`/admin/appeals/${reportId}`)).data,
  resolve: async (reportId: number, decision: AppealDecision, reason: string) =>
    (await apiClient.post<AdminAppeal>(`/admin/appeals/${reportId}/resolve`, { decision, reason })).data,
};
