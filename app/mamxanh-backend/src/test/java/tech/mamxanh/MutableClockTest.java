package tech.mamxanh;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class MutableClockTest {
    @Test
    void vietnamViewSharesTimeChangesAndResetAcrossTheLocalDateBoundary() {
        var clock = new MutableClock();
        var vietnam = clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

        clock.advance(Duration.ofHours(15));
        assertThat(vietnam.instant()).isEqualTo(clock.instant());
        assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(LocalDate.now(vietnam)).isEqualTo(LocalDate.of(2026, 9, 29));
        assertThat(vietnam.withZone(ZoneOffset.UTC)).isSameAs(clock);

        clock.reset();
        assertThat(vietnam.instant()).isEqualTo(MutableClock.DEFAULT_START);
        assertThat(LocalDate.now(vietnam)).isEqualTo(LocalDate.of(2026, 9, 28));
    }
}
