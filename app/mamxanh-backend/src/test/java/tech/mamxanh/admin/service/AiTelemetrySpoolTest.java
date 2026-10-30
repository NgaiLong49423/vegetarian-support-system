package tech.mamxanh.admin.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tools.jackson.databind.json.JsonMapper;

class AiTelemetrySpoolTest {
    @TempDir Path directory;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-28T03:00:00Z"), ZoneOffset.UTC);
    private AiTelemetrySpool spool() { return new AiTelemetrySpool(directory.toString(), new JsonMapper(), clock); }
    private AiUsageEvent event() {
        return new AiUsageEvent(null, UUID.randomUUID().toString(), "FR-51", clock.instant(),
                new AiTokenUsage(AiTokenUsage.MODEL, "STANDARD", 13, 1, 129, 115, 0, 0));
    }

    @Test
    void forcedMetadataCanBeRecoveredByANewInstanceWithoutPromptFields() throws Exception {
        var event = event();
        spool().store(event);
        String json = Files.readString(directory.resolve(event.eventId() + ".json"));
        assertThat(json).contains("promptTokens", "thinkingTokens")
                .doesNotContain("question", "answer", "history", "apiKey", "promptContent");
        var restarted = spool();
        assertThat(restarted.pending(10)).containsExactly(event);
        restarted.acknowledge(event.eventId());
        assertThat(restarted.pending(10)).isEmpty();
    }

    @Test
    void databaseFailureLeavesTheFileForReplayAfterRestart() throws Exception {
        var writer = mock(AiTelemetryWriter.class);
        var event = event();
        doThrow(new IllegalStateException("synthetic private failure")).when(writer).record(event);
        var first = new AiTelemetryListener(writer, spool());
        first.onUsage(event);
        first.close();
        assertThat(spool().pending(10)).containsExactly(event);

        doNothing().when(writer).record(event);
        var restarted = new AiTelemetryListener(writer, spool());
        restarted.replay();
        restarted.close();
        verify(writer, times(2)).record(event);
        assertThat(spool().pending(10)).isEmpty();
    }

    @Test
    void saturatedMemoryQueueKeepsRejectedEventsForTheNextProcess() throws Exception {
        var writer = mock(AiTelemetryWriter.class);
        var started = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test worker timed out");
            return null;
        }).when(writer).record(any());
        var listener = new AiTelemetryListener(writer, spool(), 1);
        try {
            listener.onUsage(event());
            listener.onUsage(event());
            assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();
            listener.onUsage(event());
            listener.onUsage(event());
        } finally {
            release.countDown();
            listener.close();
        }
        assertThat(spool().pending(10)).hasSize(1);
        var restarted = new AiTelemetryListener(writer, spool());
        restarted.replay();
        restarted.close();
        verify(writer, times(4)).record(any());
        assertThat(spool().pending(10)).isEmpty();
    }

    @Test
    void staleReplaySnapshotCannotResurrectAnAlreadyAcknowledgedEvent() throws Exception {
        var event = event();
        var spool = spy(spool());
        spool.store(event);
        spool.acknowledge(event.eventId());
        when(spool.pending(500)).thenReturn(java.util.List.of(event));
        var writer = mock(AiTelemetryWriter.class);
        var listener = new AiTelemetryListener(writer, spool);
        listener.replay();
        listener.close();
        verifyNoInteractions(writer);
        assertThat(spool.pending(10)).isEmpty();
    }

    @Test
    void unavailableDiskFailsBeforeSchedulingInsteadOfAcknowledgingAnUnrecordedSuccess() throws Exception {
        Path notDirectory = directory.resolve("file");
        Files.writeString(notDirectory, "occupied");
        var writer = mock(AiTelemetryWriter.class);
        var listener = new AiTelemetryListener(writer,
                new AiTelemetrySpool(notDirectory.toString(), new JsonMapper(), clock));
        try {
            assertThatThrownBy(() -> listener.onUsage(event())).isInstanceOfSatisfying(AppException.class,
                    failure -> assertThat(failure.errorCode()).isEqualTo(ErrorCode.AI_TELEMETRY_UNAVAILABLE));
        } finally { listener.close(); }
        verifyNoInteractions(writer);
    }

    @Test
    void expiredMetadataAndStaleTemporaryFilesAreRemovedWithoutBlockingValidEvents() throws Exception {
        var old = new AiUsageEvent(1L, "FR-51", clock.instant().minus(Duration.ofDays(91)), event().usage());
        spool().store(old);
        var current = event();
        spool().store(current);
        var temporary = directory.resolve(".pending-abandoned.tmp");
        Files.writeString(temporary, "{}");
        Files.setLastModifiedTime(temporary, java.nio.file.attribute.FileTime.from(old.occurredAt()));
        var corrupt = directory.resolve(UUID.randomUUID() + ".json");
        Files.writeString(corrupt, "invalid JSON");
        assertThat(spool().pending(10)).containsExactly(current);
        assertThat(temporary).doesNotExist();
        assertThat(directory.resolve(old.eventId() + ".json")).doesNotExist();
        Files.setLastModifiedTime(corrupt, java.nio.file.attribute.FileTime.from(old.occurredAt()));
        spool().pending(10);
        assertThat(corrupt).doesNotExist();
    }
}
