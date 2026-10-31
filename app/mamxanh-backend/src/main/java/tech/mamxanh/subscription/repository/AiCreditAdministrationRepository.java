package tech.mamxanh.subscription.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Atomic SQL Server wallet and append-only ledger writes for Admin grants. */
@Repository
public class AiCreditAdministrationRepository {
    private final JdbcTemplate jdbc;

    public AiCreditAdministrationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public BigDecimal grant(long userId, long administratorId, String referenceId, BigDecimal credits,
            String reason, LocalDateTime now) {
        jdbc.update("""
                MERGE INTO AI_CREDIT_WALLET WITH (HOLDLOCK) AS target
                USING (SELECT ? AS user_id, ? AS updated_at) AS source
                   ON target.user_id = source.user_id
                WHEN NOT MATCHED THEN
                    INSERT (user_id, balance, updated_at)
                    VALUES (source.user_id, CAST(0 AS DECIMAL(38,9)), source.updated_at);
                """, userId, now);

        BigDecimal balance = jdbc.queryForObject("""
                UPDATE AI_CREDIT_WALLET
                SET balance = balance + ?, updated_at = ?
                OUTPUT inserted.balance
                WHERE user_id = ?
                """, BigDecimal.class, credits, now, userId);
        jdbc.update("""
                INSERT INTO AI_CREDIT_LEDGER
                    (user_id, transaction_type, reference_id, credit_delta, balance_after,
                     granted_by_user_id, reason, created_at)
                VALUES (?, 'ADMIN_GRANT', ?, ?, ?, ?, ?, ?)
                """, userId, referenceId, credits, balance, administratorId, reason, now);
        return balance;
    }

    public BigDecimal topUp(long userId, String orderCode, BigDecimal credits, LocalDateTime now) {
        jdbc.update("""
                MERGE INTO AI_CREDIT_WALLET WITH (HOLDLOCK) AS target
                USING (SELECT ? AS user_id, ? AS updated_at) AS source
                   ON target.user_id = source.user_id
                WHEN NOT MATCHED THEN
                    INSERT (user_id, balance, updated_at)
                    VALUES (source.user_id, CAST(0 AS DECIMAL(38,9)), source.updated_at);
                """, userId, now);
        BigDecimal balance = jdbc.queryForObject("""
                UPDATE AI_CREDIT_WALLET
                SET balance = balance + ?, updated_at = ?
                OUTPUT inserted.balance
                WHERE user_id = ?
                """, BigDecimal.class, credits, now, userId);
        jdbc.update("""
                INSERT INTO AI_CREDIT_LEDGER
                    (user_id, transaction_type, reference_id, credit_delta, balance_after, created_at)
                VALUES (?, 'TOP_UP', ?, ?, ?, ?)
                """, userId, orderCode, credits, balance, now);
        return balance;
    }

    public BigDecimal balance(long userId) {
        return jdbc.query("SELECT balance FROM AI_CREDIT_WALLET WHERE user_id = ?", rs ->
                rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO, userId);
    }
}
