import { apiClient } from "../lib/apiClient"
import type { PageResponse } from "./expertApplications"

export type TransactionState = "PENDING" | "PAID" | "FAILED" | "CANCELLED" | "NEEDS_RECONCILIATION"
export type TransactionFilters = {
  query: string
  tier: "" | "PLUS" | "PRO"
  status: "" | TransactionState
  from: string
  to: string
}
export type AdminTransaction = {
  id: number
  orderCode: string
  userId: number
  customerName: string
  customerEmail: string
  tier: string | null
  amountVnd: number
  status: TransactionState
  createdAt: string
  paidAt: string | null
  subscriptionId: number | null
  subscriptionStartsAt: string | null
  subscriptionEndsAt: string | null
  subscriptionStatus: string | null
  productType: "SUBSCRIPTION" | "AI_CREDIT"
  creditAmount: number | null
  creditsPer1000VndSnapshot: number | null
  expiresAt: string | null
}
export type TransactionReport = {
  transactions: PageResponse<AdminTransaction>
  summary: {
    total: number
    paid: number
    pending: number
    failed: number
    cancelled: number
    needsReconciliation: number
    revenueVnd: number
  }
}
const params = (filters: TransactionFilters) =>
  Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value !== ""),
  )
export const adminTransactionsApi = {
  list: async (
    filters: TransactionFilters,
    page: number,
    signal: AbortSignal,
  ) =>
    (
      await apiClient.get<TransactionReport>("/admin/transactions", {
        params: { ...params(filters), page, size: 20 },
        signal,
      })
    ).data,
  detail: async (id: number, signal: AbortSignal) =>
    (
      await apiClient.get<AdminTransaction>(`/admin/transactions/${id}`, {
        signal,
      })
    ).data,
  export: async (filters: TransactionFilters) =>
    (
      await apiClient.get<Blob>("/admin/transactions/export", {
        params: params(filters),
        responseType: "blob",
        headers: { Accept: "text/csv, application/problem+json" },
      })
    ).data,
}
