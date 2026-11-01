package tech.mamxanh.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Metadata-only write-ahead files. SQL remains the reporting source of truth. */
@Component
public class AiTelemetrySpool {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AiTelemetrySpool.class);
    private static final int MAX_EVENT_BYTES = 8192;
    private final Path directory;
    private final ObjectMapper mapper;
    private final Clock clock;

    public AiTelemetrySpool(@Value("${mamxanh.ai.telemetry.spool-directory:./data/ai-telemetry}") String directory,
            ObjectMapper mapper, Clock clock) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.mapper = mapper;
        this.clock = clock;
    }

    public void store(AiUsageEvent event) {
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            if (Files.getFileStore(directory).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
            }
            byte[] bytes = mapper.writeValueAsBytes(event);
            if (bytes.length > MAX_EVENT_BYTES) throw new IOException("Telemetry envelope too large");
            temporary = Files.createTempFile(directory, ".pending-", ".tmp");
            try (var file = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                var buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) file.write(buffer);
                file.force(true);
            }
            Files.move(temporary, path(event.eventId()), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            forceDirectory();
        } catch (IOException | RuntimeException failure) {
            // Do not acknowledge an AI answer whose metadata could not be saved durably.
            LOG.error("AI telemetry durable enqueue failed category={}", failure.getClass().getSimpleName());
            throw new AppException(ErrorCode.AI_TELEMETRY_UNAVAILABLE);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* Retention retries cleanup. */ }
            }
        }
    }

    public List<AiUsageEvent> pending(int limit) throws IOException {
        var result = new ArrayList<AiUsageEvent>();
        if (!Files.exists(directory)) return result;
        var cutoff = clock.instant().minus(Duration.ofDays(90));
        try (var files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                if (!Files.isRegularFile(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) continue;
                String name = file.getFileName().toString();
                if (name.startsWith(".pending-") && name.endsWith(".tmp")) {
                    if (Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) Files.deleteIfExists(file);
                    continue;
                }
                if (!name.matches("[0-9a-f-]{36}\\.json")) continue;
                try {
                    if (Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(file);
                        continue;
                    }
                    if (Files.size(file) > MAX_EVENT_BYTES) throw new IOException("Invalid telemetry file size");
                    var event = mapper.readValue(Files.readAllBytes(file), AiUsageEvent.class);
                    if (!path(event.eventId()).equals(file)) throw new IOException("Invalid telemetry identity");
                    if (event.occurredAt().isBefore(cutoff)) {
                        Files.deleteIfExists(file);
                    } else if (result.size() < limit) {
                        result.add(event);
                    }
                } catch (java.nio.file.NoSuchFileException ignored) {
                    // Another worker has already committed and acknowledged this event.
                } catch (RuntimeException | IOException invalid) {
                    // An unreadable file must not prevent later valid events from being replayed.
                    LOG.error("AI telemetry pending metadata unreadable category={}", invalid.getClass().getSimpleName());
                    if (Files.exists(file) && Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(file);
                    }
                }
            }
        }
        return result;
    }

    public void acknowledge(UUID eventId) throws IOException {
        Files.deleteIfExists(path(eventId));
        forceDirectory();
    }

    public boolean isPending(UUID eventId) {
        return Files.isRegularFile(path(eventId), java.nio.file.LinkOption.NOFOLLOW_LINKS);
    }

    private Path path(UUID eventId) { return directory.resolve(java.util.Objects.requireNonNull(eventId) + ".json"); }

    private void forceDirectory() throws IOException {
        try (var channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (AccessDeniedException | UnsupportedOperationException unsupported) {
            // Windows does not expose directory fsync through FileChannel; the file itself was forced.
            if (!System.getProperty("os.name").startsWith("Windows")) throw unsupported;
        }
    }
}
