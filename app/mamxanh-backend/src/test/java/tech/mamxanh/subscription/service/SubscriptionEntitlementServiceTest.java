package tech.mamxanh.subscription.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.subscription.repository.SubscriptionRepository;

class SubscriptionEntitlementServiceTest {
    private final SubscriptionRepository repository = mock(SubscriptionRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T03:00:00Z"), ZoneOffset.UTC);
    private final SubscriptionEntitlementService service = new SubscriptionEntitlementService(repository, clock);

    @Test void deniedCallsExplainTheActualMinimumTierAndKeepTheStableErrorCode() {
        assertUpgrade(() -> service.requirePlusOrPro(7), "PLUS", "Plus");
        assertUpgrade(() -> service.requirePro(7), "PRO", "Pro");
    }

    @Test void everyCallRechecksCurrentSubscriptionWithoutADailyCounterOrCachedEntitlement() {
        var now = LocalDateTime.now(clock);
        when(repository.hasActivePaidSubscription(7, now)).thenReturn(true, false);
        when(repository.hasActiveProSubscription(7, now)).thenReturn(true, false);
        service.requirePlusOrPro(7);
        service.requirePro(7);
        assertUpgrade(() -> service.requirePlusOrPro(7), "PLUS", "Plus");
        assertUpgrade(() -> service.requirePro(7), "PRO", "Pro");
        verify(repository, times(2)).hasActivePaidSubscription(7, now);
        verify(repository, times(2)).hasActiveProSubscription(7, now);
        verifyNoMoreInteractions(repository);
    }

    private void assertUpgrade(Runnable call, String tier, String label) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(AppException.class, ex -> {
            assertThat(ex.errorCode()).isEqualTo(ErrorCode.AI_SUBSCRIPTION_REQUIRED);
            assertThat(ex.properties()).containsEntry("requiredTier", tier).containsEntry("upgradeUrl", "/goi-ai");
            assertThat(ex.getMessage()).contains("nâng cấp lên " + label);
        });
    }
}
