package tech.mamxanh.notification.dto.response;

/** Email channel preference of the signed-in user (UC-49.4); in-app notifications are always kept. */
public record NotificationPreferencesResponse(boolean replyEmailEnabled) {
}
