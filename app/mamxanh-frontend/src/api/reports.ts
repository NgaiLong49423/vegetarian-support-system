import { apiClient } from '../lib/apiClient';

export type ReportReasonCode =
  | 'RECIPE_INFO_OR_DIET_LABEL'
  | 'FOOD_SAFETY_HAZARD'
  | 'SPAM_ADVERTISING'
  | 'INAPPROPRIATE_CONTENT'
  | 'COPYRIGHT_VIOLATION'
  | 'OTHER';

export const reportReasons: Array<{ code: ReportReasonCode; label: string }> = [
  { code: 'RECIPE_INFO_OR_DIET_LABEL', label: 'Sai thông tin công thức hoặc nhãn ăn chay' },
  { code: 'FOOD_SAFETY_HAZARD', label: 'Nguy cơ an toàn thực phẩm' },
  { code: 'SPAM_ADVERTISING', label: 'Spam hoặc quảng cáo không phù hợp' },
  { code: 'INAPPROPRIATE_CONTENT', label: 'Nội dung xúc phạm hoặc không phù hợp' },
  { code: 'COPYRIGHT_VIOLATION', label: 'Sao chép hoặc nghi vi phạm bản quyền' },
  { code: 'OTHER', label: 'Khác' },
];

export type ReportSupplement = { supplementId: number; content: string; createdAt: string };
export type MyReport = {
  reportId: number;
  status: 'OPEN' | 'IN_REVIEW' | 'RESOLVED';
  reasonCode: ReportReasonCode;
  description: string | null;
  createdAt: string;
  recipe: { id: number; title: string };
  supplements: ReportSupplement[];
  supplementLimit: number;
};

export const reportsApi = {
  submit: async (recipeId: number, reasonCode: ReportReasonCode, description: string) =>
    (await apiClient.post<{ reportId: number; status: string; message: string }>(`/recipes/${recipeId}/reports`, {
      reasonCode,
      description: description || undefined,
    })).data,
  mine: async (reportId: number) => (await apiClient.get<MyReport>(`/me/reports/${reportId}`)).data,
  supplement: async (reportId: number, content: string) =>
    (await apiClient.post<ReportSupplement>(`/me/reports/${reportId}/supplements`, { content })).data,
};
