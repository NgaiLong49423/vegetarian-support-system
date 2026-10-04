import { apiClient, asApiError } from '../lib/apiClient';

export type SubscriptionTier = 'FREE' | 'PLUS' | 'PRO';
export type SubscriptionStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED';

export interface PlanResponse {
  tier: SubscriptionTier;
  name: string;
  priceVnd: number;
  billingCycle: string;
  description: string;
  features: string[];
  autoRenew: boolean;
}

export interface CheckoutRequest {
  tier: 'PLUS' | 'PRO';
  returnUrl?: string;
  cancelUrl?: string;
}

export interface CheckoutResponse {
  orderCode: number;
  checkoutUrl: string;
  amountVnd: number;
  tier: string;
  status: 'PENDING';
}

export interface MySubscriptionResponse {
  tier: SubscriptionTier;
  status: SubscriptionStatus;
  startsAt: string | null;
  endsAt: string | null;
  features: string[];
}

export interface TransactionHistoryResponse {
  orderCode: number;
  tier: 'PLUS' | 'PRO';
  amountVnd: number;
  status: PaymentStatus;
  createdAt: string;
  paidAt: string | null;
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

  /**
   * Creates a payOS payment link for upgrading to PLUS or PRO (AC-13.2).
   * Requires authenticated user.
   */
  createCheckout: (payload: CheckoutRequest) =>
    request(() => apiClient.post<CheckoutResponse>('/subscriptions/checkout', payload)),

  /**
   * Retrieves payment transaction history for current user (AC-13.6).
   * Requires authenticated user.
   */
  getTransactions: () => request(() => apiClient.get<TransactionHistoryResponse[]>('/subscriptions/transactions')),
};
