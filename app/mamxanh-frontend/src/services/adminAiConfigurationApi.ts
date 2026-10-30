import { apiClient } from '../lib/apiClient';

export type AiPlanSetting = {
  tier: 'FREE' | 'PLUS' | 'PRO';
  priceVnd: number;
  tokenQuota: number;
  pendingTokenQuota: number | null;
  pendingEffectiveAt: string | null;
};

export type AiModelSetting = {
  modelId: string;
  displayName: string;
  enabled: boolean;
  freeAllowed: boolean;
  freeDefault: boolean;
  plusAllowed: boolean;
  plusDefault: boolean;
  proAllowed: boolean;
  proDefault: boolean;
};

export type AiCreditSetting = {
  creditsPer1000Vnd: number;
  creditsPer1000OverageTokens: number;
  minimumTopUpVnd: number;
  maximumTopUpVnd: number;
};

export type AdminAiConfiguration = {
  plans: AiPlanSetting[];
  models: AiModelSetting[];
  credit: AiCreditSetting;
};

export const adminAiConfigurationApi = {
  async get() {
    return (await apiClient.get<AdminAiConfiguration>('/admin/ai-config')).data;
  },
  async update(configuration: AdminAiConfiguration & { sendEmailNotification: boolean }) {
    return (await apiClient.put<AdminAiConfiguration>('/admin/ai-config', configuration)).data;
  },
};
