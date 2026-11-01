package tech.mamxanh.subscription.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Account subscription status and validity details (FR-13, AC-13.4, AC-13.7).
 */
public record MySubscriptionResponse(
        String tier,
        String status,
        boolean active,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}, format = "date-time")
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime startsAt,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}, format = "date-time")
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime endsAt,
        List<String> unlockedFeatures
) {
    public static MySubscriptionResponse free() {
        return new MySubscriptionResponse(
                "FREE",
                "ACTIVE",
                true,
                null,
                null,
                List.of("Các chức năng AI theo vai trò và quyền nghiệp vụ của tài khoản")
        );
    }
}
