package tech.mamxanh.recipe.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/**
 * FR-02 / FR-10: Daily quota of 3 requests for Guest, resetting at midnight Vietnam time;
 * bounded, process-local state; never stores prompts or raw IPs.
 */
@Service
public class GuestChatRateLimiter {
    public static final String ID_HEADER = "X-Guest-Chat-Id";
    public static final int DAILY_CALL_LIMIT = 3;
    public static final int DAILY_IP_LIMIT = 60;
    private static final int MAX_KEYS = 10_000;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    static final class UsageRecord {
        final LocalDate date;
        final int count;

        UsageRecord(LocalDate date, int count) {
            this.date = date;
            this.count = count;
        }
    }

    private final Map<String, UsageRecord> usage = new HashMap<>();
    private final String salt = UUID.randomUUID().toString();
    private final Clock clock;

    public GuestChatRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized void check(String cookieId, String peerAddress) {
        if (cookieId == null || !UUID_PATTERN.matcher(cookieId).matches()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Hãy cho phép cookie của trang để sử dụng Chatbot dành cho khách.");
        }
        Instant now = clock.instant();
        LocalDate today = now.atZone(VIETNAM).toLocalDate();

        // Evict expired daily records
        usage.values().removeIf(record -> record.date.isBefore(today));

        String cookie = key("cookie:", cookieId.toLowerCase(java.util.Locale.ROOT));
        String ip = key("ip:", peerAddress == null ? "unknown" : peerAddress);

        UsageRecord idRecord = usage.get(cookie);
        UsageRecord ipRecord = usage.get(ip);

        Instant nextMidnight = today.plusDays(1).atStartOfDay(VIETNAM).toInstant();
        Duration retryAfter = Duration.between(now, nextMidnight);
        if (retryAfter.isNegative() || retryAfter.isZero()) {
            retryAfter = Duration.ofSeconds(1);
        }

        if (idRecord != null && idRecord.date.equals(today) && idRecord.count >= DAILY_CALL_LIMIT) {
            throw limited(retryAfter);
        }
        if (ipRecord != null && ipRecord.date.equals(today) && ipRecord.count >= DAILY_IP_LIMIT) {
            throw limited(retryAfter);
        }

        int newKeys = (idRecord == null ? 1 : 0) + (ipRecord == null ? 1 : 0);
        if (usage.size() + newKeys > MAX_KEYS) {
            throw limited(retryAfter);
        }

        usage.compute(cookie, (k, existing) -> existing == null || !existing.date.equals(today)
                ? new UsageRecord(today, 1)
                : new UsageRecord(today, existing.count + 1));
        usage.compute(ip, (k, existing) -> existing == null || !existing.date.equals(today)
                ? new UsageRecord(today, 1)
                : new UsageRecord(today, existing.count + 1));
    }

    public synchronized void acquire(String anonymousId, String remoteAddress) {
        check(anonymousId, remoteAddress);
    }

    public synchronized void reset() {
        usage.clear();
    }

    private static AppException limited(Duration retryAfter) {
        return AppException.retryAfter(ErrorCode.AI_GUEST_RATE_LIMITED, retryAfter);
    }

    private String key(String prefix, String value) {
        try {
            return prefix + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((salt + value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
