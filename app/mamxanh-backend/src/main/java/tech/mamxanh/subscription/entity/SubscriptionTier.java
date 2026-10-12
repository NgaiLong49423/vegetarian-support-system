package tech.mamxanh.subscription.entity;

/**
 * Membership tiers for AI capabilities (FR-13, BR-02).
 * DB values allowed in SUBSCRIPTION table: 'PLUS', 'PRO'.
 * FREE tier is the default system entitlement when no active subscription exists.
 */
public enum SubscriptionTier {
    PLUS(49_000, "Gói PLUS", "Gói tháng cơ bản với AI soạn bài và gợi ý biến tấu"),
    PRO(99_000, "Gói PRO", "Gói tháng cao cấp với đầy đủ tính năng AI và lập thực đơn tuần theo dinh dưỡng");

    private final int priceVnd;
    private final String displayName;
    private final String description;

    SubscriptionTier(int priceVnd, String displayName, String description) {
        this.priceVnd = priceVnd;
        this.displayName = displayName;
        this.description = description;
    }

    public int priceVnd() {
        return priceVnd;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
