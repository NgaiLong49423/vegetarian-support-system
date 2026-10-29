package tech.mamxanh.admin.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tech.mamxanh.admin.repository.AiTelemetryRepository;
import tech.mamxanh.integration.ai.*;

class AiTelemetryServiceTest {
    private final Instant now = Instant.parse("2026-10-23T03:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final AiTelemetryRepository repository = mock(AiTelemetryRepository.class);
    private final AiTelemetryPricing pricing = new AiTelemetryPricing();
    private final AiTelemetryWriter writer = new AiTelemetryWriter(repository,pricing,clock);
    private AiTokenUsage usage(Integer p, Integer r, Integer t, Integer thinking, Integer cached, Integer tools) {
        return new AiTokenUsage(AiTokenUsage.MODEL,"STANDARD",p,r,t,thinking,cached,tools);
    }
    @ParameterizedTest
    @CsvSource({"150,300,450,0,0,0,COMPLETE", "150,300,500,50,20,0,COMPLETE",
            "150,300,450,0,151,0,INVALID", "150,300,449,0,0,0,INVALID",
            "150,300,451,0,0,0,INVALID", "-1,300,450,0,0,0,INVALID",
            "150,300,450,-1,0,0,INVALID"})
    void validatesProviderMetadataWithoutRecalculatingTotal(int p,int r,int t,int thoughts,int cached,int tools,String expected) {
        assertThat(AiTelemetryWriter.status(usage(p,r,t,thoughts,cached,tools))).isEqualTo(expected);
    }
    @Test void missingAndPartialAreExplicitAndNotPriced() {
        assertThat(AiTelemetryWriter.status(usage(null,null,null,null,null,null))).isEqualTo("MISSING");
        assertThat(AiTelemetryWriter.status(usage(1,null,5,null,null,null))).isEqualTo("PARTIAL");
        assertThat(AiTelemetryWriter.status(usage(1,2,5,null,null,null))).isEqualTo("PARTIAL");
        assertThat(pricing.estimate(usage(1,2,3,0,0,0),now,"PARTIAL")).isNull();
    }
    @Test void pricingIncludesThinkingAndCachedInputAndUsesHistoricalVersion() {
        // Metadata returned by the synthetic live Gemini 3.8 check on 2026-10-28.
        assertThat(pricing.estimate(usage(13,1,129,115,0,0),now,"COMPLETE").usd())
                .isEqualByComparingTo("0.00044475");
        var value = usage(150,300,500,50,20,0);
        assertThat(pricing.estimate(value,now,"COMPLETE").usd()).isEqualByComparingTo("0.0014115");
        var next = pricing.estimate(value,Instant.parse("2027-01-01T00:00:00Z"),"COMPLETE");
        assertThat(next.usd()).isEqualByComparingTo("0.002823");
        assertThat(next.version()).isEqualTo("gemini-standard-2027-01-01");
        assertThat(pricing.estimate(value,Instant.parse("2026-10-22T16:59:59Z"),"COMPLETE")).isNull();
        assertThat(pricing.estimate(value,Instant.parse("2026-10-22T17:00:00Z"),"COMPLETE").usd())
                .isEqualByComparingTo("0.0014115");
    }
    @Test void unsupportedPricingNeverInventsZeroCost() {
        for (var value : List.of(new AiTokenUsage("other","STANDARD",1,2,3,0,0,0),
                new AiTokenUsage(AiTokenUsage.MODEL,"BATCH",1,2,3,0,0,0), usage(1,2,3,null,0,0),
                usage(1,2,3,0,null,0), usage(1,2,3,0,0,null), usage(1,2,4,0,0,1))) {
            assertThat(pricing.estimate(value,now,"COMPLETE")).isNull();
        }
    }
    @Test void validatesEnvelopeAndSkipsExpiredReplay() {
        var good = usage(1,2,3,0,0,0);
        for (var event : List.of(new AiUsageEvent(null,1L,null,"FR-51",now,good),
                new AiUsageEvent(1L,"UNKNOWN",now,good), new AiUsageEvent(1L,"FR-51",null,good),
                new AiUsageEvent(1L,"FR-51",now.plusSeconds(1),good), new AiUsageEvent(0L,"FR-51",now,good),
                new AiUsageEvent(1L,"FR-51",now,(AiTokenUsage)null), new AiUsageEvent(null,"FR-51",now,good),
                new AiUsageEvent(1L,UUID.randomUUID().toString(),"FR-51",now,good),
                new AiUsageEvent(null,"not-a-uuid","FR-51",now,good),
                new AiUsageEvent(1L,"FR-51",now,new AiTokenUsage("other","STANDARD",1,2,3,0,0,0)))) {
            assertThatThrownBy(() -> writer.record(event)).isInstanceOf(IllegalArgumentException.class);
        }
        writer.record(new AiUsageEvent(1L,"FR-51",now.minus(Duration.ofDays(91)),good));
        verifyNoInteractions(repository);
    }
    @Test void auditFlagsAccompanyNormalizedUnusableCountersAndListenerDoesNotBreakAnswer() {
        var raw = usage(-1,2,3,null,null,null);
        var event = new AiUsageEvent(1L,"FR-51",now,raw);
        writer.record(event);
        verify(repository).insert(eq(event),eq(usage(0,2,3,null,null,null)),eq("INVALID"),isNull());
        var failing = mock(AiTelemetryWriter.class);
        doThrow(new IllegalStateException("private payload must not be logged")).when(failing).record(event);
        var spool = mock(AiTelemetrySpool.class);
        when(spool.isPending(event.eventId())).thenReturn(true);
        var listener = new AiTelemetryListener(failing, spool);
        try { assertThatCode(() -> listener.onUsage(event)).doesNotThrowAnyException(); }
        finally { listener.close(); }
        verify(failing).record(event);
        // Rejection after shutdown must not turn a completed AI request into an application error.
        assertThatCode(() -> listener.onUsage(event)).doesNotThrowAnyException();
    }
    @Test void retentionDeletesInBatchesAndSurvivesUnavailableDatabase() {
        when(repository.deleteBatchBefore(now.minus(Duration.ofDays(90)))).thenReturn(1000,1);
        var retention = new AiTelemetryRetention(repository,clock); retention.purge();
        verify(repository,times(2)).deleteBatchBefore(any());
        when(repository.deleteBatchBefore(any())).thenThrow(new IllegalStateException("db unavailable"));
        assertThatCode(retention::purge).doesNotThrowAnyException();
    }
    @Test void emptyReportHasKnownZeroCostAndClipsPartialRetentionDay() {
        when(repository.daily(any(),any(),isNull())).thenReturn(List.of());
        var report = new AiTelemetryReportService(repository,clock).report(LocalDate.of(2026,10,23),LocalDate.of(2026,10,23),"DAY",null);
        assertThat(report.totals().calls()).isZero();
        assertThat(report.totals().estimatedCostUsd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.buckets()).isEmpty();
        new AiTelemetryReportService(repository,clock).report(LocalDate.of(2026,7,25),LocalDate.of(2026,10,23),"DAY",null);
        verify(repository).daily(eq(now.minus(Duration.ofDays(90))),any(),isNull());
    }
}
