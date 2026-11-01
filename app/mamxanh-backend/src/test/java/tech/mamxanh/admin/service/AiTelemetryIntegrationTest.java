package tech.mamxanh.admin.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.admin.repository.AiTelemetryRepository;
import tech.mamxanh.integration.ai.*;

/** AC-11.1..5: real SQL Server/Flyway, event listener, transactions and HTTP security.
 * Only the external provider is controlled; no live Google billing is asserted.
 */
@ExtendWith(OutputCaptureExtension.class)
class AiTelemetryIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AiTelemetryWriter writer;
    @Autowired AiTelemetryRepository repository;
    @Autowired AiTelemetryRetention retention;
    @Autowired AiTelemetryListener listener;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean AiTelemetrySpool spool;
    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean AiClient ai;
    private long member;
    private String guest;

    @BeforeEach void fixture() {
        var inScope = new AiClient.ChatResult(null,
                "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 150, 10, 160);
        when(ai.classifyChatScope(anyString(), anyString())).thenReturn(inScope);
        when(ai.classifyChatScope(anyString(), anyString(), anyString())).thenReturn(inScope);
        clock.advance(Duration.between(clock.instant(), Instant.parse("2026-10-23T03:00:00Z")));
        guest = UUID.randomUUID().toString();
        String email = "fr11-" + guest + "@example.invalid";
        jdbc.update("INSERT INTO [USER](email,display_name,role,account_status,email_verified) VALUES (?,N'FR11','CUSTOMER','ACTIVE',1)", email);
        member = jdbc.queryForObject("SELECT user_id FROM [USER] WHERE email=?", Long.class, email);
    }
    @AfterEach void cleanup() throws Exception {
        var executor = (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor)
                org.springframework.test.util.ReflectionTestUtils.getField(listener,"executor");
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline && (executor.getActiveCount() > 0
                || !executor.getThreadPoolExecutor().getQueue().isEmpty()
                || spool.pending(500).stream().anyMatch(event -> java.util.Objects.equals(event.userId(), member)
                        || guest.equals(event.anonymousSessionId())))) {
            listener.replay();
            Thread.sleep(10);
        }
        jdbc.update("DELETE FROM AI_USAGE_TELEMETRY WHERE user_id=? OR anonymous_session_id=?", member, guest);
        jdbc.update("DELETE FROM [USER] WHERE user_id=?", member);
    }
    private AiTokenUsage usage() { return new AiTokenUsage(AiTokenUsage.MODEL,"STANDARD",150,300,450,0,0,0); }
    private AiUsageEvent event(String feature, Instant at) { return new AiUsageEvent(member,feature,at,usage()); }
    private long count() { return jdbc.queryForObject("SELECT COUNT_BIG(*) FROM AI_USAGE_TELEMETRY WHERE user_id=? OR anonymous_session_id=?", Long.class, member, guest); }
    private void awaitCount(long expected) throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (count() != expected && System.nanoTime() < deadline) Thread.sleep(10);
        assertThat(count()).isEqualTo(expected);
    }

    @Test void durableMetadataReplaysIntoRealSqlAndAcknowledgesOnlyAfterCommit() throws Exception {
        var event = event("FR-51", clock.instant());
        spool.store(event);
        assertThat(count()).isZero();
        listener.replay();
        awaitCount(1);
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (spool.pending(500).contains(event) && System.nanoTime() < deadline) Thread.sleep(10);
        assertThat(spool.pending(500)).doesNotContain(event);
        spool.store(event); // SQL commit may succeed before a process can remove its durable file.
        listener.replay();
        awaitCount(1);
    }

    @Test void unavailableDurableStorageReturns503InsteadOfAnUnaccountedSuccessfulAnswer() throws Exception {
        when(ai.answerChat(anyString(), anyString())).thenReturn(new AiClient.ChatResult("Synthetic answer", 150, 300, 450));
        doThrow(new tech.mamxanh.common.exception.AppException(tech.mamxanh.common.exception.ErrorCode.AI_TELEMETRY_UNAVAILABLE))
                .when(spool).store(any());
        mockMvc.perform(post("/api/v1/recipes/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contextType\":\"GENERAL\",\"question\":\"Gợi ý món canh chua chay\",\"anonymousViewerId\":\"" + guest + "\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_TELEMETRY_UNAVAILABLE"));
        assertThat(count()).isZero();
    }

    @Test void exactMetadataAndCostArePersistedOnceWithoutPromptColumns() throws Exception {
        var event = event("FR-51",clock.instant());
        events.publishEvent(event); events.publishEvent(event);
        awaitCount(1);
        var row = jdbc.queryForMap("SELECT * FROM AI_USAGE_TELEMETRY WHERE event_id=?",event.eventId().toString());
        assertThat(row.get("prompt_tokens")).isEqualTo(150L);
        assertThat(row.get("response_tokens")).isEqualTo(300L);
        assertThat(row.get("total_tokens")).isEqualTo(450L);
        assertThat(row.get("estimated_cost_usd").toString()).isEqualTo("0.001237500000");
        assertThat(row.keySet()).doesNotContain("prompt", "answer", "content", "history", "email");
        mockMvc.perform(get("/api/v1/admin/ai-telemetry?from=2026-10-23&to=2026-10-23").with(user("1").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totals.calls").value(1))
                .andExpect(jsonPath("$.totals.totalTokens").value(450))
                .andExpect(jsonPath("$.totals.estimatedCostUsd").value(0.0012375))
                .andExpect(jsonPath("$.buckets[0].feature").value("FR-51"))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test void missingMetadataStillRecordsSuccessfulCallWithAuditFlagAndUnknownCost() throws Exception {
        events.publishEvent(new AiUsageEvent(member,"FR-21",clock.instant(),AiTokenUsage.legacy(null,null,null)));
        awaitCount(1);
        var row = jdbc.queryForMap("SELECT * FROM AI_USAGE_TELEMETRY WHERE user_id=?",member);
        assertThat(row.get("usage_status")).isEqualTo("MISSING");
        assertThat(row.get("total_tokens")).isEqualTo(0L);
        assertThat(row.get("estimated_cost_usd")).isNull();
        mockMvc.perform(get("/api/v1/admin/ai-telemetry?from=2026-10-23&to=2026-10-23").with(user("1").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totals.incompleteUsageCalls").value(1))
                .andExpect(jsonPath("$.totals.unpricedCalls").value(1))
                .andExpect(jsonPath("$.totals.estimatedCostUsd").doesNotExist());
    }

    @Test void guestChatPersistsAnonymousMetadataAndProviderFailureDoesNotCreateSuccess(CapturedOutput output) throws Exception {
        var answer = new AiClient.ChatResult("Chao ban",null,150,300,450,usage());
        when(ai.answerChat(anyString(),anyString())).thenReturn(answer);
        when(ai.answerChat(anyString(),anyString(),anyString())).thenReturn(answer);
        String body = "{\"contextType\":\"GENERAL\",\"question\":\"Hướng dẫn nấu canh chua chay FR11 marker\",\"anonymousViewerId\":\""+guest+"\"}";
        mockMvc.perform(post("/api/v1/recipes/ai/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.answer").value("Chao ban"));
        awaitCount(2); // scope classification and answer are separate successful Gemini calls.
        assertThat(output.getAll()).doesNotContain("Hướng dẫn nấu canh chua chay FR11 marker", "Chao ban", "private provider failure");
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT anonymous_session_id) FROM AI_USAGE_TELEMETRY WHERE anonymous_session_id=?",Integer.class,guest)).isEqualTo(1);
        when(ai.answerChat(anyString(),anyString())).thenThrow(new GeminiAiClient.AiProviderException("private provider failure"));
        when(ai.answerChat(anyString(),anyString(),anyString())).thenThrow(new GeminiAiClient.AiProviderException("private provider failure"));
        mockMvc.perform(post("/api/v1/recipes/ai/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isServiceUnavailable());
        assertThat(count()).isEqualTo(3); // the failed answer still follows a successful recorded classification.
        assertThat(output.getAll()).doesNotContain("Hướng dẫn nấu canh chua chay FR11 marker", "Chao ban", "private provider failure");
    }

    @Test void independentWriterWorksInsideReadOnlyAndSurvivesCallerRollback() throws Exception {
        var tx = new TransactionTemplate(transactions); tx.setReadOnly(true);
        tx.executeWithoutResult(state -> { events.publishEvent(event("FR-36",clock.instant())); state.setRollbackOnly(); });
        awaitCount(1);
    }

    @Test void simultaneousDeliveryOfSameEventIsIdempotentInSqlServer() throws Exception {
        var event = event("FR-51",clock.instant());
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var calls = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 4; i++) calls.add(executor.submit(() -> writer.record(event)));
            for (var call : calls) call.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(count()).isEqualTo(1);
    }

    @Test void retentionDeletesOnlyOlderThanNinetyDaysAndWriterRejectsExpiredReplay() {
        var boundary = clock.instant().minus(Duration.ofDays(90));
        var old = event("FR-34",boundary.minusNanos(100));
        repository.insert(old,usage(),"COMPLETE",null);
        repository.insert(event("FR-47",boundary),usage(),"COMPLETE",null);
        repository.insert(event("FR-37",clock.instant()),usage(),"COMPLETE",null);
        assertThat(count()).isEqualTo(3);
        retention.purge();
        assertThat(count()).isEqualTo(2);
        writer.record(old);
        assertThat(count()).isEqualTo(2);
    }

    @Test void allFeaturesAndLocalMidnightAreGroupedWithoutLosingTotals() throws Exception {
        for (String feature : AiTelemetryWriter.FEATURES) events.publishEvent(event(feature,clock.instant()));
        // 17:00 UTC starts the next local calendar day.
        events.publishEvent(event("FR-51",Instant.parse("2026-10-22T17:00:00Z")));
        awaitCount(7);
        for (String granularity : new String[]{"DAY","WEEK","MONTH"}) {
            mockMvc.perform(get("/api/v1/admin/ai-telemetry?from=2026-10-23&to=2026-10-23&granularity="+granularity)
                    .with(user("1").roles("ADMIN")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totals.calls").value(7))
                    .andExpect(jsonPath("$.buckets.length()").value(6));
        }
        mockMvc.perform(get("/api/v1/admin/ai-telemetry?from=2026-10-23&to=2026-10-23&feature=FR-51")
                .with(user("1").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.totals.calls").value(2));
    }

    @Test void storedTimeIsUtcAndLocalMidnightSplitsDaysCorrectly() throws Exception {
        var before = event("FR-51",Instant.parse("2026-10-22T16:59:59Z"));
        events.publishEvent(before);
        events.publishEvent(event("FR-51",Instant.parse("2026-10-22T17:00:00Z")));
        awaitCount(2);
        assertThat(jdbc.queryForObject("SELECT occurred_at FROM AI_USAGE_TELEMETRY WHERE event_id=?",
                java.time.LocalDateTime.class,before.eventId().toString()))
                .isEqualTo(java.time.LocalDateTime.of(2026,10,22,16,59,59));
        mockMvc.perform(get("/api/v1/admin/ai-telemetry?from=2026-10-22&to=2026-10-23&feature=FR-51")
                .with(user("1").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.calls").value(2))
                .andExpect(jsonPath("$.buckets[0].periodStart").value("2026-10-22"))
                .andExpect(jsonPath("$.buckets[1].periodStart").value("2026-10-23"));
    }

    @Test void fiftyReadTransactionsDoNotStarveTelemetryConnections() throws Exception {
        var firstWave = new java.util.concurrent.CountDownLatch(10);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var calls = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 50; i++) calls.add(executor.submit(() -> {
                var tx = new TransactionTemplate(transactions); tx.setReadOnly(true);
                tx.executeWithoutResult(state -> {
                    jdbc.queryForObject("SELECT 1",Integer.class);
                    firstWave.countDown();
                    try {
                        if (!firstWave.await(10,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Source pool did not fill");
                    } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); }
                    events.publishEvent(event("FR-36",clock.instant()));
                });
            }));
            for (var call : calls) call.get(20,java.util.concurrent.TimeUnit.SECONDS);
        }
        awaitCount(50);
    }

    @Test void guestAndMemberCannotReadAndInvalidQueriesAreRejected() throws Exception {
        String url = "/api/v1/admin/ai-telemetry?from=2026-10-23&to=2026-10-23";
        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(url).with(user("1").roles("CUSTOMER"))).andExpect(status().isForbidden());
        for (String query : new String[]{"from=bad&to=2026-10-23", "from=2026-10-24&to=2026-10-23",
                "from=2026-01-01&to=2026-10-23", "from=2026-10-23&to=2026-10-24",
                "from=2026-10-23&to=2026-10-23&granularity=YEAR", "from=2026-10-23&to=2026-10-23&feature=UNKNOWN"}) {
            mockMvc.perform(get("/api/v1/admin/ai-telemetry?"+query).with(user("1").roles("ADMIN")))
                    .andExpect(status().isBadRequest());
        }
    }
}
