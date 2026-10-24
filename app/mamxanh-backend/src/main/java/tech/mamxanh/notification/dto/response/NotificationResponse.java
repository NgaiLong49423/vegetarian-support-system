package tech.mamxanh.notification.dto.response;

import java.time.LocalDateTime;

/** One in-app notification of the signed-in user (UC-49.1); times are UTC. */
public record NotificationResponse(long notificationId, String type, String title, String message, String targetPath,
        boolean read, LocalDateTime createdAt, LocalDateTime readAt) {
}
