package tech.mamxanh.subscription.repository;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SubscriptionRepository {
    private final JdbcTemplate jdbcTemplate;

    public SubscriptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasActivePaidSubscription(long userId, LocalDateTime now) {
        Integer match = jdbcTemplate.queryForObject("""
                SELECT CASE WHEN EXISTS (
                    SELECT 1 FROM [SUBSCRIPTION]
                    WHERE user_id = ? AND status = 'ACTIVE' AND tier IN ('PLUS', 'PRO')
                      AND starts_at <= ? AND ends_at > ?
                ) THEN 1 ELSE 0 END
                """, Integer.class, userId, now, now);
        return Integer.valueOf(1).equals(match);
    }
}
