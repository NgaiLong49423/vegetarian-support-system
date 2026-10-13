package tech.mamxanh.subscription.dto.response;

import java.util.List;

/**
 * Details of an AI membership plan according to Phase 1 specification (FR-13, AC-13.1).
 */
public record PlanResponse(
        String tier,
        int priceVnd,
        String name,
        String description,
        List<String> features,
        String billingPeriod
) {
}
