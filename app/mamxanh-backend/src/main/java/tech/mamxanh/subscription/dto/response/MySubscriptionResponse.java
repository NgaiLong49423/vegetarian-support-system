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
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        List<String> unlockedFeatures
) {
    public static MySubscriptionResponse free() {
        return new MySubscriptionResponse(
                "FREE",
                "ACTIVE",
                true,
                null,
                null,
                List.of(
                        "AI Chatbot hỗ trợ ẩm thực chay",
                        "Gợi ý món theo nguyên liệu sẵn có"
                )
        );
    }
}
