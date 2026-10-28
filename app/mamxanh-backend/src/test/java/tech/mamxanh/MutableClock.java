package tech.mamxanh;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Test clock that stays still until a test moves it, so expiry and cooldown rules are deterministic. */
public class MutableClock extends Clock {

    public static final Instant DEFAULT_START = Instant.parse("2026-09-28T03:00:00Z");

    private volatile Instant instant;
    private final ZoneId zone;

    public MutableClock() {
        this(DEFAULT_START, ZoneOffset.UTC);
    }

    private MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public void reset() {
        instant = DEFAULT_START;
    }

    public void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this.zone.equals(zone) ? this : new MutableClock(instant, zone);
    }
}
