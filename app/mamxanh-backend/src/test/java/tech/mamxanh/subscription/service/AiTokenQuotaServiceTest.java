package tech.mamxanh.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.subscription.entity.SubscriptionEntity;
import tech.mamxanh.subscription.entity.SubscriptionStatus;
import tech.mamxanh.subscription.entity.SubscriptionTier;
import tech.mamxanh.subscription.repository.AiTokenUsageLedgerRepository;
import tech.mamxanh.subscription.repository.SubscriptionRepository;

class AiTokenQuotaServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");
    private final AiTokenUsageLedgerRepository ledger = mock(AiTokenUsageLedgerRepository.class);
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AiTokenQuotaService service = new AiTokenQuotaService(ledger, subscriptions, users, clock);

    @BeforeEach
    void setUp() {
        when(subscriptions.findByUserIdAndStatus(anyLong(), eq(SubscriptionStatus.ACTIVE))).thenReturn(Optional.empty());
        when(ledger.sumMemberTokens(anyLong(), any(), any())).thenReturn(0L);
        when(ledger.sumGuestTokens(anyString())).thenReturn(0L);
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7L, Role.CUSTOMER));
    }

    @Test
    void appliesGuestLimitPerSessionAndStopsBeforeTheNextProviderCall() {
        when(ledger.sumGuestTokens("guest-session")).thenReturn(19_999L);
        service.requireAvailable(null, "guest-session");

        when(ledger.sumGuestTokens("guest-session")).thenReturn(20_000L);
        assertThatThrownBy(() -> service.requireAvailable(null, "guest-session"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_TOKEN_QUOTA_EXCEEDED);
        verify(ledger, never()).sumGuestTokens("another-session");
    }

    @Test
    void freeQuotaUsesCalendarMonthInHoChiMinhTime() {
        when(ledger.sumMemberTokens(eq(7L), any(), any())).thenReturn(49_999L);
        service.requireAvailable(7L, null);
        var usage = service.getMyUsage(7L);
        assertThat(usage.tier()).isEqualTo("FREE");
        assertThat(usage.quotaTokens()).isEqualTo(50_000);
        assertThat(usage.periodStartsAt()).isEqualTo(Instant.parse("2026-09-30T17:00:00Z"));
        assertThat(usage.resetsAt()).isEqualTo(Instant.parse("2026-10-31T17:00:00Z"));
    }

    @Test
    void paidQuotaUsesTheSubscriptionSnapshotAndPeriod() {
        var subscription = new SubscriptionEntity(7L, SubscriptionTier.PRO,
                LocalDateTime.of(2026, 10, 1, 10, 0), LocalDateTime.of(2026, 10, 31, 10, 0));
        when(subscriptions.findByUserIdAndStatus(7L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(subscription));
        when(ledger.sumMemberTokens(eq(7L), eq(subscription.getStartsAt()), eq(subscription.getEndsAt())))
                .thenReturn(599_999L);

        service.requireAvailable(7L, null);
        var usage = service.getMyUsage(7L);
        assertThat(usage.tier()).isEqualTo("PRO");
        assertThat(usage.quotaTokens()).isEqualTo(600_000);
        assertThat(usage.remainingTokens()).isEqualTo(1);
    }

    @Test
    void recordsProviderTotalOnceAndDoesNotDebitMissingOrInvalidMetadata() {
        when(ledger.existsById(anyString())).thenReturn(false);
        var complete = new AiTokenUsage("gemini-3.8-flash", "STANDARD", 100, 200, 310, 10, 0, 0);
        service.record(new AiUsageEvent(UUID.randomUUID(), 7L, null, "FR-51", NOW, complete));
        service.record(new AiUsageEvent(UUID.randomUUID(), 7L, null, "FR-51", NOW,
                AiTokenUsage.legacy(null, null, null)));
        var invalid = new AiTokenUsage("gemini-3.8-flash", "STANDARD", 100, 200, 250, 10, 0, 0);
        service.record(new AiUsageEvent(UUID.randomUUID(), 7L, null, "FR-51", NOW, invalid));

        var captor = org.mockito.ArgumentCaptor.forClass(
                tech.mamxanh.subscription.entity.AiTokenUsageLedgerEntity.class);
        verify(ledger, times(3)).saveAndFlush(captor.capture());
        assertThat(captor.getAllValues()).extracting("tokenCount").containsExactly(310L, null, null);
        assertThat(captor.getAllValues()).extracting("usageStatus").containsExactly("RECORDED", "MISSING", "INVALID");
    }

    @Test
    void usageReadIsScopedToTheAuthenticatedMember() {
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(8L, Role.CUSTOMER));
        assertThatThrownBy(() -> service.getMyUsage(7L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        verifyNoInteractions(ledger);
    }
}
