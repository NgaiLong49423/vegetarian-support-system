package tech.mamxanh.notification.dto.response;

/** Unread in-app notifications of the signed-in user, shown on the bell (AC-49.1). */
public record UnreadNotificationCountResponse(long unreadCount) {
}
