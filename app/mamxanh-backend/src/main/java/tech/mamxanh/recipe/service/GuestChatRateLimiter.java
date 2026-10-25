package tech.mamxanh.recipe.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/** FR-02: bounded, per-process sliding windows; no Guest entity, raw IP, or prompt storage. */
@Service
public class GuestChatRateLimiter {
    public static final String ID_HEADER = "X-Guest-Chat-Id";
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int ID_LIMIT = 10;
    private static final int IP_LIMIT = 60;
    private static final int MAX_KEYS = 10_000;
    private final Map<String, ArrayDeque<Instant>> windows = new HashMap<>();
    private final String salt = UUID.randomUUID().toString();
    private final Clock clock;

    public GuestChatRateLimiter(Clock clock) { this.clock = clock; }

    public synchronized void acquire(String anonymousId, String remoteAddress) {
        if (anonymousId != null && !UUID_PATTERN.matcher(anonymousId).matches()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
        Instant now = clock.instant();
        Instant cutoff = now.minus(WINDOW);
        windows.values().forEach(queue -> {
            while (!queue.isEmpty() && !queue.peekFirst().isAfter(cutoff)) queue.removeFirst();
        });
        windows.values().removeIf(ArrayDeque::isEmpty);
        // Use only the servlet peer address; never trust client-supplied forwarding headers here.
        String address = digest("ip:" + remoteAddress);
        String identity = digest(anonymousId == null ? "no-cookie:" + remoteAddress : "id:" + anonymousId.toLowerCase(java.util.Locale.ROOT));
        ArrayDeque<Instant> byId = windows.get(identity);
        ArrayDeque<Instant> byIp = windows.get(address);
        long waitMillis = Math.max(waitMillis(byId, ID_LIMIT, now), waitMillis(byIp, IP_LIMIT, now));
        if (waitMillis > 0) throw limited(Duration.ofMillis(waitMillis));
        int newKeys = (byId == null ? 1 : 0) + (byIp == null ? 1 : 0);
        if (windows.size() + newKeys > MAX_KEYS) throw limited(WINDOW);
        windows.computeIfAbsent(identity, key -> new ArrayDeque<>()).addLast(now);
        windows.computeIfAbsent(address, key -> new ArrayDeque<>()).addLast(now);
    }

    private static long waitMillis(ArrayDeque<Instant> queue, int limit, Instant now) {
        return queue == null || queue.size() < limit ? 0
                : Math.max(1, Duration.between(now, queue.peekFirst().plus(WINDOW)).toMillis());
    }

    private static AppException limited(Duration retryAfter) {
        return AppException.retryAfter(ErrorCode.GUEST_CHAT_RATE_LIMITED, retryAfter);
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((salt + value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
