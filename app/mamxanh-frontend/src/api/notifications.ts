import { apiClient, asApiError } from '../lib/apiClient';

/** FR-49 in-app notification of the signed-in user; times are UTC ISO strings without an offset. */
export interface AppNotification {
  notificationId: number;
  type: string;
  title: string;
  message: string;
  targetPath: string | null;
  read: boolean;
  createdAt: string;
  readAt: string | null;
}

export interface NotificationPage {
  content: AppNotification[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface NotificationPreferences {
  replyEmailEnabled: boolean;
}

export const NOTIFICATION_PAGE_SIZE = 20;

/** Fired after the unread count may have changed, so the header bell reloads it. */
export const NOTIFICATIONS_CHANGED_EVENT = 'mamxanh:notifications-changed';

export function announceNotificationsChanged() {
  window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED_EVENT));
}

async function request<T>(operation: () => Promise<{ data: T }>): Promise<T> {
  try {
    return (await operation()).data;
  } catch (error) {
    throw asApiError(error);
  }
}

export const notificationsApi = {
  list: (page: number, unreadOnly: boolean) => request(
    () => apiClient.get<NotificationPage>('/me/notifications', { params: { page, size: NOTIFICATION_PAGE_SIZE, unreadOnly } }),
  ),
  unreadCount: () => request(() => apiClient.get<{ unreadCount: number }>('/me/notifications/unread-count'))
    .then((result) => result.unreadCount),
  markRead: (notificationId: number) => request(
    () => apiClient.put<AppNotification>(`/me/notifications/${notificationId}/read`),
  ),
  markAllRead: () => request(() => apiClient.put<void>('/me/notifications/read-all')),
  preferences: () => request(() => apiClient.get<NotificationPreferences>('/me/notification-preferences')),
  updatePreferences: (replyEmailEnabled: boolean) => request(
    () => apiClient.put<NotificationPreferences>('/me/notification-preferences', { replyEmailEnabled }),
  ),
};
