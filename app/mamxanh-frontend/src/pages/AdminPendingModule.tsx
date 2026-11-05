import { AdminUserManagement } from "./AdminUserManagement"

export type PendingModuleType = "users" | "transactions"

export function AdminPendingModule({ module }: { module: PendingModuleType }) {
  return <AdminUserManagement />
}
