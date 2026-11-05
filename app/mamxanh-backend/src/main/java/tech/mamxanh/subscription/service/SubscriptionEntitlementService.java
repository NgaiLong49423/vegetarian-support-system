package tech.mamxanh.subscription.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.subscription.repository.SubscriptionRepository;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class SubscriptionEntitlementService {
    private final SubscriptionRepository repository;
    private final Clock clock;

    public SubscriptionEntitlementService(SubscriptionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public void requirePlusOrPro(long userId) {
        if (!repository.hasActivePaidSubscription(userId, LocalDateTime.now(clock))) {
            throw upgradeRequired("PLUS", "Tính năng AI này yêu cầu gói AI PLUS còn hiệu lực. Hãy nâng cấp lên gói AI PLUS để tiếp tục.");
        }
    }

    @Transactional(readOnly = true)
    public void requirePro(long userId) {
        if (!repository.hasActiveProSubscription(userId, LocalDateTime.now(clock))) {
            throw upgradeRequired("PRO", "Tính năng AI này yêu cầu gói AI PRO còn hiệu lực. Hãy nâng cấp lên gói AI PRO để tiếp tục.");
        }
    }

    private static AppException upgradeRequired(String tier, String detail) {
        return new AppException(ErrorCode.AI_SUBSCRIPTION_REQUIRED, detail)
                .withProperty("requiredTier", tier).withProperty("upgradeUrl", "/goi-ai");
    }
}
