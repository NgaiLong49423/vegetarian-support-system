package tech.mamxanh.recipe.service;

import org.springframework.stereotype.Service;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

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

/** FR-10 Guest prerequisite: rolling minute limits for cookie and peer IP independently.
 * Bounded, process-local state for the current single Backend; never stores prompts or raw IPs.
 * Forwarded headers are deliberately not trusted as client identities.
 */
@Service
public class GuestChatRateLimiter {
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int LIMIT = 10;
    private static final int MAX_BUCKETS = 20_000;
    private final Map<String, ArrayDeque<Instant>> buckets = new HashMap<>();
    private final Clock clock;

    public GuestChatRateLimiter(Clock clock) { this.clock = clock; }

    public synchronized void check(String cookieId, String peerAddress) {
        if (cookieId == null || !cookieId.matches("(?i)[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Hãy cho phép cookie của trang để sử dụng Chatbot dành cho khách.");
        }
        Instant now = clock.instant();
        Instant cutoff = now.minus(WINDOW);
        buckets.values().forEach(queue -> {
            while (!queue.isEmpty() && !queue.peekFirst().isAfter(cutoff)) queue.removeFirst();
        });
        buckets.values().removeIf(ArrayDeque::isEmpty);
        String cookie = key("cookie:", cookieId.toLowerCase(java.util.Locale.ROOT));
        String ip = key("ip:", peerAddress == null ? "unknown" : peerAddress);
        Instant next = null;
        for (String key : new String[]{cookie, ip}) {
            var queue = buckets.get(key);
            if (queue != null && queue.size() >= LIMIT) {
                Instant allowedAt = queue.peekFirst().plus(WINDOW);
                if (next == null || allowedAt.isAfter(next)) next = allowedAt;
            }
        }
        if (next != null) throw AppException.retryAfter(ErrorCode.AI_GUEST_RATE_LIMITED, Duration.between(now, next));
        int needed = (buckets.containsKey(cookie) ? 0 : 1) + (buckets.containsKey(ip) ? 0 : 1);
        if (buckets.size() + needed > MAX_BUCKETS) {
            throw AppException.retryAfter(ErrorCode.AI_GUEST_RATE_LIMITED, WINDOW);
        }
        buckets.computeIfAbsent(cookie, key -> new ArrayDeque<>()).addLast(now);
        buckets.computeIfAbsent(ip, key -> new ArrayDeque<>()).addLast(now);
    }

    private static String key(String prefix, String value) {
        try {
            return prefix + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
