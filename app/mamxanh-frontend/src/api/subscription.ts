import { apiClient, asApiError } from '../lib/apiClient';

export type SubscriptionTier = 'FREE' | 'PLUS' | 'PRO';
export type SubscriptionStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED' | 'NEEDS_RECONCILIATION';
export type PaymentProductType = 'SUBSCRIPTION' | 'AI_CREDIT';

export interface PlanResponse {
  tier: SubscriptionTier;
  name: string;
  priceVnd: number;
  billingPeriod: string;
  description: string;
  features: string[];
}

export interface CheckoutRequest {
  tier: 'PLUS' | 'PRO';
}

export interface CheckoutResponse {
  orderCode: number;
  checkoutUrl: string;
  amountVnd: number;
  tier: string;
  qrCode: string;
}

export interface MySubscriptionResponse {
  tier: SubscriptionTier;
  status: SubscriptionStatus;
  active: boolean;
  startsAt: string | null;
  endsAt: string | null;
  unlockedFeatures: string[];
}

export interface MyAiUsageResponse {
  tier: SubscriptionTier;
  consumedTokens: number;
  quotaTokens: number;
  remainingTokens: number;
  periodStartsAt: string;
  resetsAt: string;
}

export interface AiChatSettingsResponse {
  tier: SubscriptionTier | 'GUEST';
  consumedTokens: number;
  quotaTokens: number;
  remainingTokens: number;
  resetsAt: string | null;
  selectedModelId: string | null;
  selectionRequired: boolean;
  models: Array<{ modelId: string; displayName: string; isDefault: boolean }>;
}

export interface TransactionHistoryResponse {
  paymentTransactionId: number;
  orderCode: string;
  tier: 'PLUS' | 'PRO' | null;
  amountVnd: number;
  status: PaymentStatus;
  createdAt: string;
  paidAt: string | null;
  endsAt: string | null;
  productType: PaymentProductType;
  creditAmount: number | null;
  creditsPer1000VndSnapshot: number | null;
  expiresAt: string | null;
}

export interface PaymentResultResponse {
  orderCode: string;
  tier: 'PLUS' | 'PRO' | 'AI_CREDIT';
  amountVnd: number;
  status: PaymentStatus;
  paidAt: string | null;
  activated: boolean;
  subscription: MySubscriptionResponse;
  productType: PaymentProductType;
  creditAmount: number | null;
  creditsPer1000VndSnapshot: number | null;
}

export interface MyAiCreditResponse {
  balance: number;
  creditsPer1000Vnd: number;
  minimumTopUpVnd: number;
  maximumTopUpVnd: number;
}

export interface CreditCheckoutResponse {
  orderCode: number;
  amountVnd: number;
  creditAmount: number;
  creditsPer1000VndSnapshot: number;
  expiresAt: string;
  checkoutUrl: string;
  qrCode: string;
}

async function request<T>(operation: () => Promise<{ data: T }>): Promise<T> {
  try {
    return (await operation()).data;
  } catch (error) {
    throw asApiError(error);
  }
}

export const subscriptionApi = {
  /**
   * Retrieves the 3 pricing tiers (AC-13.1).
   * Public endpoint, no authentication required.
   */
  getPlans: () => request(() => apiClient.get<PlanResponse[]>('/subscriptions/plans')),

  /**
   * Retrieves current user subscription info (AC-13.7, AC-13.4).
   * Requires authenticated user.
   */
  getMySubscription: () => request(() => apiClient.get<MySubscriptionResponse>('/subscriptions/my-subscription')),

  /** Retrieves the authenticated member's token usage for the current quota period. */
  getMyAiUsage: () => request(() => apiClient.get<MyAiUsageResponse>('/subscriptions/my-ai-usage')),

  getMyAiCredit: () => request(() => apiClient.get<MyAiCreditResponse>('/subscriptions/my-ai-credit')),

  getChatSettings: (anonymousViewerId?: string) => request(() => apiClient.get<AiChatSettingsResponse>(
    '/ai/chat-settings', { params: anonymousViewerId ? { anonymousViewerId } : undefined },
  )),

  selectChatModel: (modelId: string) => request(() => apiClient.put<AiChatSettingsResponse>(
    '/ai/chat-settings/model', { modelId },
  )),

  /**
   * Creates a payOS payment link for upgrading to PLUS or PRO (AC-13.2).
   * Requires authenticated user.
   */
  createCheckout: (payload: CheckoutRequest) =>
    request(() => apiClient.post<CheckoutResponse>('/subscriptions/checkout', payload)),

  createCreditCheckout: (amountVnd: number) => request(() => apiClient.post<CreditCheckoutResponse>(
    '/subscriptions/credit-checkout', { amountVnd },
  )),

  /**
   * Verifies an owned order and reconciles its payment before reporting activation.
   */
  getPaymentResult: (orderCode: string, signal?: AbortSignal) =>
    request(() => apiClient.get<PaymentResultResponse>(`/subscriptions/transactions/${encodeURIComponent(orderCode)}`, { signal })),

  /** Retrieves payment transaction history for the authenticated user (AC-13.6). */
  getTransactions: () => request(() => apiClient.get<TransactionHistoryResponse[]>('/subscriptions/transactions')),
};
