package tech.mamxanh.subscription.entity;

/**
 * Persisted subscription tiers (FR-13, BR-02). Current price/quota/model rules live in AI configuration.
 * DB values allowed in SUBSCRIPTION table: 'PLUS', 'PRO'.
 * FREE tier is the default system entitlement when no active subscription exists.
 * The enum price values only recognize payment rows created before V34 stored their tier snapshot.
 */
public enum SubscriptionTier {
    PLUS(49_000, 300_000, "Gói PLUS", "Gói tháng với hạn mức token và model Gemini theo cấu hình hiện hành"),
    PRO(99_000, 600_000, "Gói PRO", "Gói tháng với hạn mức token và model Gemini theo cấu hình hiện hành");

    private final int priceVnd;
    private final long tokenQuota;
    private final String displayName;
    private final String description;

    SubscriptionTier(int priceVnd, long tokenQuota, String displayName, String description) {
        this.priceVnd = priceVnd;
        this.tokenQuota = tokenQuota;
        this.displayName = displayName;
        this.description = description;
    }

    public int priceVnd() {
        return priceVnd;
    }

    public long tokenQuota() { return tokenQuota; }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
