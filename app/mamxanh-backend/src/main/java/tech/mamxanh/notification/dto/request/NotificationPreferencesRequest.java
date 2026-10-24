package tech.mamxanh.notification.dto.request;

import jakarta.validation.constraints.NotNull;

/** UC-49.4: turn reply emails on or off. */
public record NotificationPreferencesRequest(
        @NotNull(message = "Vui lòng chọn bật hoặc tắt email thông báo.") Boolean replyEmailEnabled) {
}
