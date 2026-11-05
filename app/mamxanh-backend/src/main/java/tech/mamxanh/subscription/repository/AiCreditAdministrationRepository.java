package tech.mamxanh.subscription.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Atomic SQL Server wallet and append-only ledger writes for Admin grants, top-ups and overage debits. */
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

    public void ensureAndLockWallet(long userId, LocalDateTime now) {
        jdbc.update("""
                MERGE INTO AI_CREDIT_WALLET WITH (HOLDLOCK) AS target
                USING (SELECT ? AS user_id, ? AS updated_at) AS source
                   ON target.user_id = source.user_id
                WHEN NOT MATCHED THEN
                    INSERT (user_id, balance, updated_at)
                    VALUES (source.user_id, CAST(0 AS DECIMAL(38,9)), source.updated_at);
                """, userId, now);
        jdbc.queryForObject(
                "SELECT balance FROM AI_CREDIT_WALLET WITH (UPDLOCK, HOLDLOCK) WHERE user_id = ?",
                BigDecimal.class, userId);
    }

    public OverageDebit debitOverage(long userId, String referenceId, BigDecimal requiredCredits,
            long overageTokens, long creditsPer1000Tokens, LocalDateTime now) {
        OverageDebit existing = findOverageDebit(referenceId);
        if (existing != null) return existing;

        ensureAndLockWallet(userId, now);
        BigDecimal currentBalance = jdbc.queryForObject(
                "SELECT balance FROM AI_CREDIT_WALLET WITH (UPDLOCK, HOLDLOCK) WHERE user_id = ?",
                BigDecimal.class, userId);
        existing = findOverageDebit(referenceId);
        if (existing != null) return existing;

        BigDecimal walletBalance = currentBalance == null ? BigDecimal.ZERO : currentBalance;
        BigDecimal normalizedRequired = requiredCredits.max(BigDecimal.ZERO).setScale(9, RoundingMode.HALF_UP);
        BigDecimal debited = walletBalance.min(normalizedRequired).max(BigDecimal.ZERO).setScale(9, RoundingMode.HALF_UP);
        BigDecimal absorbedCredits = normalizedRequired.subtract(debited).max(BigDecimal.ZERO).setScale(9, RoundingMode.HALF_UP);
        BigDecimal balanceAfter = walletBalance.subtract(debited).setScale(9, RoundingMode.HALF_UP);
        Long absorbedTokens = calculateAbsorbedTokens(overageTokens, creditsPer1000Tokens, debited, absorbedCredits);

        jdbc.update("UPDATE AI_CREDIT_WALLET SET balance = ?, updated_at = ? WHERE user_id = ?",
                balanceAfter, now, userId);
        BigDecimal delta = debited.signum() == 0
                ? BigDecimal.ZERO.setScale(9, RoundingMode.HALF_UP)
                : debited.negate();
        jdbc.update("""
                INSERT INTO AI_CREDIT_LEDGER
                    (user_id, transaction_type, reference_id, credit_delta, balance_after,
                     overage_tokens, credits_per_1000_tokens, system_absorbed_tokens,
                     system_absorbed_credits, created_at)
                VALUES (?, 'OVERAGE', ?, ?, ?, ?, ?, ?, ?, ?)
                """, userId, referenceId, delta, balanceAfter, overageTokens, creditsPer1000Tokens,
                absorbedTokens, absorbedCredits, now);
        return new OverageDebit(debited, balanceAfter, overageTokens, creditsPer1000Tokens,
                absorbedTokens, absorbedCredits);
    }

    public OverageDebit findOverageDebit(String referenceId) {
        return jdbc.query("""
                SELECT credit_delta, balance_after, overage_tokens, credits_per_1000_tokens,
                       system_absorbed_tokens, system_absorbed_credits
                FROM AI_CREDIT_LEDGER
                WHERE transaction_type = 'OVERAGE' AND reference_id = ?
                """, rs -> {
            if (!rs.next()) return null;
            BigDecimal delta = rs.getBigDecimal("credit_delta");
            BigDecimal absorbedCredits = rs.getBigDecimal("system_absorbed_credits");
            return new OverageDebit(
                    delta == null ? BigDecimal.ZERO : delta.abs(),
                    rs.getBigDecimal("balance_after"),
                    rs.getLong("overage_tokens"),
                    rs.getLong("credits_per_1000_tokens"),
                    rs.getObject("system_absorbed_tokens", Long.class),
                    absorbedCredits == null ? BigDecimal.ZERO : absorbedCredits);
        }, referenceId);
    }

    public BigDecimal balance(long userId) {
        return jdbc.query("SELECT balance FROM AI_CREDIT_WALLET WHERE user_id = ?", rs ->
                rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO, userId);
    }

    private static Long calculateAbsorbedTokens(long overageTokens, long creditsPer1000Tokens,
            BigDecimal debited, BigDecimal absorbedCredits) {
        if (overageTokens <= 0 || absorbedCredits.signum() <= 0) return 0L;
        if (debited.signum() <= 0 || creditsPer1000Tokens <= 0) return overageTokens;
        BigDecimal exactCovered=debited.multiply(BigDecimal.valueOf(1000)).divide(BigDecimal.valueOf(creditsPer1000Tokens),9,RoundingMode.HALF_UP).stripTrailingZeros();
        if(exactCovered.scale()>0) return null; // Fractional coverage is represented exactly by absorbed Credits.
        long coveredTokens = debited.multiply(BigDecimal.valueOf(1_000))
                .divide(BigDecimal.valueOf(creditsPer1000Tokens), 0, RoundingMode.FLOOR)
                .longValue();
        return Math.max(0L, overageTokens - coveredTokens);
    }

    public record OverageDebit(BigDecimal debitedCredits, BigDecimal balanceAfter, long overageTokens,
            long creditsPer1000Tokens, Long systemAbsorbedTokens, BigDecimal systemAbsorbedCredits) { }
}
