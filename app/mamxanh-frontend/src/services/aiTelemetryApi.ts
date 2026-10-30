import { apiClient, asApiError } from '../lib/apiClient';

export type TelemetryTotals = {
  calls: number;
  promptTokens: number;
  responseTokens: number;
  totalTokens: number;
  thinkingTokens: number;
  cachedTokens: number;
  incompleteUsageCalls: number;
  unpricedCalls: number;
  knownEstimatedCostUsd: number;
  estimatedCostUsd: number | null;
};
export type TelemetryQuery = { from: string; to: string; granularity: 'DAY' | 'WEEK' | 'MONTH'; feature: string };
export type TelemetryReport = {
  from: string; to: string; granularity: string; timezone: string; retainedFrom: string;
  currency: string; costBasis: string; totals: TelemetryTotals;
  buckets: { periodStart: string; feature: string; totals: TelemetryTotals }[];
};

export async function getAiTelemetry(query: TelemetryQuery, signal?: AbortSignal): Promise<TelemetryReport> {
  try {
    const response = await apiClient.get<TelemetryReport>('/admin/ai-telemetry', {
      params: { ...query, feature: query.feature || undefined }, signal,
    });
    return response.data;
  } catch (error) { throw asApiError(error); }
}
