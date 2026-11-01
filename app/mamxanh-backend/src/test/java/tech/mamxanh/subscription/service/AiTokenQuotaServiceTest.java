package tech.mamxanh.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataRetrievalFailureException;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.integration.ai.adk.AdkModelGateway;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot;
import tech.mamxanh.subscription.entity.AiTokenUsageLedgerEntity;
import tech.mamxanh.subscription.entity.SubscriptionEntity;
import tech.mamxanh.subscription.entity.SubscriptionStatus;
import tech.mamxanh.subscription.entity.SubscriptionTier;
import tech.mamxanh.subscription.repository.AiConfigurationRepository;
import tech.mamxanh.subscription.repository.AiCreditAdministrationRepository;
import tech.mamxanh.subscription.repository.AiTokenUsageLedgerRepository;
import tech.mamxanh.subscription.repository.SubscriptionRepository;

class AiTokenQuotaServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");
    private final AiTokenUsageLedgerRepository ledger = mock(AiTokenUsageLedgerRepository.class);
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final AiConfigurationRepository configuration = mock(AiConfigurationRepository.class);
    private final AiCreditAdministrationRepository creditWallet = mock(AiCreditAdministrationRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AiTokenQuotaService service = new AiTokenQuotaService(
            ledger, subscriptions, users, configuration, creditWallet, clock);

    @BeforeEach
    void setUp() {
        when(configuration.findConfiguration()).thenReturn(new tech.mamxanh.subscription.entity.AiConfigurationSnapshot(List.of(),List.of(
                new tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Model("gemini-3.8-flash","Fixture",true,true,true,true,true,true,true)),
                new tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Credit(BigDecimal.valueOf(1000),400,10000,500000)));
        when(subscriptions.findByUserIdAndStatus(anyLong(), eq(SubscriptionStatus.ACTIVE))).thenReturn(Optional.empty());
        when(ledger.sumMemberTokens(anyLong(), any(), any())).thenReturn(0L);
        when(ledger.sumGuestTokens(anyString())).thenReturn(0L);
        when(configuration.currentFreeQuota(any())).thenReturn(50_000L);
        when(creditWallet.balance(anyLong())).thenReturn(BigDecimal.ZERO);
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
    void freeQuotaReadsTheAdminConfiguration() {
        when(configuration.currentFreeQuota(NOW)).thenReturn(42_000L);

        assertThat(service.getMyUsage(7L).quotaTokens()).isEqualTo(42_000L);
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

        var captor = ArgumentCaptor.forClass(AiTokenUsageLedgerEntity.class);
        verify(ledger, times(3)).saveAndFlush(captor.capture());
        assertThat(captor.getAllValues()).extracting("tokenCount").containsExactly(310L, null, null);
        assertThat(captor.getAllValues()).extracting("usageStatus").containsExactly("RECORDED", "MISSING", "INVALID");
        verify(creditWallet, never()).debitOverage(anyLong(), anyString(), any(), anyLong(), anyLong(), any());
    }

    @Test
    void usageReadIsScopedToTheAuthenticatedMember() {
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(8L, Role.CUSTOMER));
        assertThatThrownBy(() -> service.getMyUsage(7L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        verifyNoInteractions(ledger);
    }

    @Test
    void beginCallVerifiesModelAndAllowsOverageWhenQuotaExhaustedButCreditPositive() {
        when(ledger.sumMemberTokens(eq(7L), any(), any())).thenReturn(50_000L);
        when(creditWallet.balance(7L)).thenReturn(new BigDecimal("12.500000000"));
        when(configuration.findConfiguration()).thenReturn(new AiConfigurationSnapshot(
                List.of(),
                List.of(
                        new AiConfigurationSnapshot.Model("gemini-3.8-flash", "Flash", true, true, true, true, true, true, true),
                        new AiConfigurationSnapshot.Model("gemini-3.8-pro", "Pro", true, false, false, false, false, true, false)),
                new AiConfigurationSnapshot.Credit(new BigDecimal("10"), 400L, 20_000, 5_000_000)));

        assertThat(service.hasAvailableBudget(7L, null)).isTrue();
        var snapshot = service.beginCall(7L, null, "FR-51", "gemini-3.8-flash", NOW);
        assertThat(snapshot.tier()).isEqualTo("FREE");
        assertThat(snapshot.quotaLimit()).isEqualTo(50_000L);
        assertThat(snapshot.creditsPer1000OverageTokens()).isEqualTo(400L);

        assertThatThrownBy(() -> service.beginCall(7L, null, "FR-51", "gemini-3.8-pro", NOW))
                .isInstanceOf(ApiException.class);

        when(creditWallet.balance(7L)).thenReturn(BigDecimal.ZERO);
        assertThat(service.hasAvailableBudget(7L, null)).isFalse();
        assertThatThrownBy(() -> service.beginCall(7L, null, "FR-51", "gemini-3.8-flash", NOW))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_TOKEN_QUOTA_EXCEEDED);
    }

    @Test
    void completeCallUsesCallStartSnapshotAcrossMidCallResetOrTierUpgradeAndDebitsQuotaThenExactDecimalCredit() {
        Instant callStart = Instant.parse("2026-10-31T16:59:55Z");
        var snapshot = service.beginCall(7L, null, "FR-51", "gemini-3.8-flash", callStart);

        // Simulate a subscription upgrade and period change occurring while the Gemini HTTP call was in flight
        var upgraded = new SubscriptionEntity(7L, SubscriptionTier.PRO,
                LocalDateTime.of(2026, 10, 31, 17, 0), LocalDateTime.of(2026, 11, 30, 17, 0));
        when(subscriptions.findByUserIdAndStatus(7L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(upgraded));

        // 49,900 tokens already consumed in the call-start period; call uses 253 tokens -> 100 quota + 153 overage
        when(ledger.sumMemberTokens(7L, snapshot.periodStart(), snapshot.periodEnd())).thenReturn(49_900L);
        when(ledger.existsById(anyString())).thenReturn(false);

        UUID callId = UUID.randomUUID();
        var usage = new AiTokenUsage("gemini-3.8-flash", "STANDARD", 150, 93, 253, 10, 0, 0);
        service.completeCall(callId, snapshot, usage);

        verify(creditWallet).ensureAndLockWallet(eq(7L), eq(LocalDateTime.ofInstant(callStart, ZoneOffset.UTC)));
        var ledgerCaptor = ArgumentCaptor.forClass(AiTokenUsageLedgerEntity.class);
        verify(ledger).saveAndFlush(ledgerCaptor.capture());
        assertThat(ledgerCaptor.getValue().getPeriodStart()).isEqualTo(snapshot.periodStart());
        assertThat(ledgerCaptor.getValue().getPeriodEnd()).isEqualTo(snapshot.periodEnd());
        assertThat(ledgerCaptor.getValue().getQuotaLimit()).isEqualTo(50_000L);

        // 153 overage tokens * 400 / 1000 = 61.200000000 Credits
        verify(creditWallet).debitOverage(
                eq(7L),
                eq(callId.toString()),
                eq(new BigDecimal("61.200000000")),
                eq(153L),
                eq(400L),
                eq(LocalDateTime.ofInstant(callStart, ZoneOffset.UTC)));

        // Deduplication by callId on duplicate completion
        when(ledger.existsById(callId.toString())).thenReturn(true);
        service.completeCall(callId, snapshot, usage);
        verify(ledger, times(1)).saveAndFlush(any());
        verify(creditWallet, times(1)).debitOverage(anyLong(), anyString(), any(), anyLong(), anyLong(), any());
    }

    @Test
    void adkPolicyPropagatesPersistenceFailureWithoutRetryingModel() {
        var policy = new tech.mamxanh.integration.ai.adk.MemberAccountingPolicy(users, service, null);
        var call = new AdkModelGateway.Call(UUID.randomUUID(), "gemini-3.8-flash", NOW);
        Object context = policy.beforeCall(call);
        assertThat(context).isInstanceOf(AiTokenQuotaService.CallStartSnapshot.class);

        doThrow(new DataRetrievalFailureException("db down")).when(ledger).saveAndFlush(any());
        var usage = new AdkModelGateway.Usage(
                new AiTokenUsage("gemini-3.8-flash", "STANDARD", 10, 20, 30, 0, 0, 0),
                AdkModelGateway.UsageStatus.RECORDED);

        assertThatThrownBy(() -> policy.onUsableResponse(call, context, usage))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_TELEMETRY_UNAVAILABLE);
    }
}
