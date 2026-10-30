import type { ApiError } from './apiClient';

export type AiPaidTier = 'PLUS' | 'PRO';

/** Prefer the server's minimum tier; the fallback belongs to the requested feature. */
export function aiRequiredTier(error: Pick<ApiError, 'requiredTier'>, fallback: AiPaidTier): AiPaidTier {
  return error.requiredTier ?? fallback;
}

export function aiUpgradeMessage(error: Pick<ApiError, 'requiredTier'>, fallback: AiPaidTier, feature: string): string {
  const tier = aiRequiredTier(error, fallback);
  return `${feature} yêu cầu gói AI ${tier} còn hiệu lực. Hãy nâng cấp lên gói AI ${tier} để sử dụng.`;
}
