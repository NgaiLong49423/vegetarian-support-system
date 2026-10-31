package tech.mamxanh.subscription.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.subscription.entity.SubscriptionEntity;
import tech.mamxanh.subscription.entity.SubscriptionStatus;
import tech.mamxanh.subscription.entity.SubscriptionTier;
import tech.mamxanh.subscription.repository.AiConfigurationRepository;
import tech.mamxanh.subscription.repository.SubscriptionRepository;

/** Admin-facing subscription mutation boundary; payment checkout remains in SubscriptionService. */
@Service
public class AdminSubscriptionService {
    private final UserRepository users;
    private final SubscriptionRepository subscriptions;
    private final AiConfigurationRepository configuration;
    private final Clock clock;

    public AdminSubscriptionService(UserRepository users, SubscriptionRepository subscriptions,
            AiConfigurationRepository configuration, Clock clock) {
        this.users = users;
        this.subscriptions = subscriptions;
        this.configuration = configuration;
        this.clock = clock;
    }

    @Transactional
    public TierChange giftPackage(long userId, String requestedTier) {
        var recipient = users.findByIdForUpdate(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND", "Không tìm thấy tài khoản."));
        if (recipient.getRole() != Role.CUSTOMER && recipient.getRole() != Role.EXPERT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI_PACKAGE_GIFT_MEMBER_ONLY",
                    "Chỉ có thể tặng gói AI cho tài khoản Thành viên hoặc Chuyên gia.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        SubscriptionEntity active = subscriptions.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE).orElse(null);
        String oldTier = active != null && active.isActiveAt(now) ? active.getTier().name() : "FREE";
        if (Objects.equals(oldTier, requestedTier)) {
            return new TierChange(userId, oldTier, requestedTier, active == null ? null : active.getStartsAt(),
                    active != null && active.isActiveAt(now) ? active.getEndsAt() : null,
                    quotaFor(requestedTier), false, now);
        }

        if (active != null) {
            active.setStatus(SubscriptionStatus.EXPIRED);
            active.setEndsAt(now);
            active.setUpdatedAt(now);
            subscriptions.saveAndFlush(active);
        }

        if ("FREE".equals(requestedTier)) {
            return new TierChange(userId, oldTier, requestedTier, null, null, quotaFor(requestedTier), true, now);
        }

        SubscriptionTier tier = SubscriptionTier.valueOf(requestedTier);
        LocalDateTime endsAt = now.plusDays(30);
        SubscriptionEntity next = new SubscriptionEntity(userId, tier, now, endsAt);
        long quota = quotaFor(requestedTier);
        next.setTokenQuota(quota);
        subscriptions.saveAndFlush(next);
        return new TierChange(userId, oldTier, requestedTier, now, endsAt, quota, true, now);
    }

    @Transactional(readOnly = true)
    public TierChange currentTier(long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        SubscriptionEntity active = subscriptions.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE).orElse(null);
        boolean currentlyActive = active != null && active.isActiveAt(now);
        String tier = currentlyActive ? active.getTier().name() : "FREE";
        return new TierChange(userId, tier, tier, currentlyActive ? active.getStartsAt() : null,
                currentlyActive ? active.getEndsAt() : null, quotaFor(tier), false, now);
    }

    private long quotaFor(String tier) {
        if ("FREE".equals(tier)) return configuration.currentFreeQuota(clock.instant());
        return configuration.findConfiguration().plans().stream()
                .filter(plan -> plan.tier().equals(tier)).findFirst()
                .orElseThrow(() -> new IllegalStateException("AI plan configuration missing: " + tier)).tokenQuota();
    }

    public record TierChange(long userId, String oldTier, String newTier, LocalDateTime startsAt,
            LocalDateTime endsAt, long tokenQuota, boolean changed, LocalDateTime changedAt) { }
}
