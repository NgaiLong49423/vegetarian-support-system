package tech.mamxanh.subscription.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tech.mamxanh.subscription.dto.response.AdminTransactionResponse;
import tech.mamxanh.subscription.dto.response.AdminTransactionReport.Summary;
import tech.mamxanh.subscription.entity.SubscriptionTier;

/** Read-only payment accounting: no provider calls, settlement, or entitlement updates. */
@Repository
public class TransactionAuditRepository {
    private static final String FROM = """
            FROM PAYMENT_TRANSACTION p
            JOIN [USER] u ON u.user_id = p.user_id
            LEFT JOIN SUBSCRIPTION s ON s.subscription_id = p.subscription_id AND s.user_id = p.user_id
            """;
    private static final String TIER = "COALESCE(s.tier, p.subscription_tier, CASE p.amount_vnd WHEN "
            + SubscriptionTier.PLUS.priceVnd() + " THEN 'PLUS' WHEN " + SubscriptionTier.PRO.priceVnd() + " THEN 'PRO' END)";
    private static final String SELECT = """
            SELECT p.payment_transaction_id, p.order_code, p.user_id, u.display_name, u.email,
                   %s AS tier, p.amount_vnd, p.status, p.created_at, p.paid_at,
                   s.subscription_id, s.starts_at, s.ends_at, s.status AS subscription_status
            """.formatted(TIER);
    private final JdbcTemplate jdbc;

    public TransactionAuditRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Filter(String query, String tier, String status, LocalDateTime start, LocalDateTime end) { }
    private record Where(String sql, List<Object> args) { }

    private Where where(Filter filter) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (filter.query() != null) {
            // Treat SQL LIKE metacharacters as literal text, including SQL Server's bracket syntax.
            String value = "%" + filter.query().replace("!", "!!").replace("%", "!%")
                    .replace("_", "!_").replace("[", "![") + "%";
            sql.append(" AND (p.order_code LIKE ? ESCAPE '!' OR u.email LIKE ? ESCAPE '!'"
                    + " OR u.display_name LIKE ? ESCAPE '!' OR CONCAT('TX-', p.payment_transaction_id) LIKE ? ESCAPE '!')");
            for (int i = 0; i < 4; i++) args.add(value);
        }
        if (filter.tier() != null) { sql.append(" AND ").append(TIER).append(" = ?"); args.add(filter.tier()); }
        if (filter.status() != null) { sql.append(" AND p.status = ?"); args.add(filter.status()); }
        if (filter.start() != null) { sql.append(" AND p.created_at >= ?"); args.add(Timestamp.valueOf(filter.start())); }
        if (filter.end() != null) { sql.append(" AND p.created_at < ?"); args.add(Timestamp.valueOf(filter.end())); }
        return new Where(sql.toString(), args);
    }

    public Summary summary(Filter filter) {
        Where w = where(filter);
        return jdbc.queryForObject("""
                SELECT COUNT_BIG(*) AS total,
                  COALESCE(SUM(CAST(CASE WHEN p.status='PAID' THEN 1 ELSE 0 END AS BIGINT)),0) AS paid,
                  COALESCE(SUM(CAST(CASE WHEN p.status='PENDING' THEN 1 ELSE 0 END AS BIGINT)),0) AS pending,
                  COALESCE(SUM(CAST(CASE WHEN p.status='FAILED' THEN 1 ELSE 0 END AS BIGINT)),0) AS failed,
                  COALESCE(SUM(CAST(CASE WHEN p.status='CANCELLED' THEN 1 ELSE 0 END AS BIGINT)),0) AS cancelled,
                  COALESCE(SUM(CASE WHEN p.status='PAID' THEN CAST(p.amount_vnd AS BIGINT) ELSE 0 END),0) AS revenue
                """ + FROM + w.sql(), (rs, n) -> new Summary(rs.getLong("total"), rs.getLong("paid"),
                rs.getLong("pending"), rs.getLong("failed"), rs.getLong("cancelled"), rs.getLong("revenue")), w.args().toArray());
    }

    public List<AdminTransactionResponse> page(Filter filter, long offset, int limit) {
        Where w = where(filter);
        List<Object> args = new ArrayList<>(w.args());
        args.add(offset); args.add(limit);
        return jdbc.query(SELECT + FROM + w.sql()
                + " ORDER BY p.created_at DESC, p.payment_transaction_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                (rs, n) -> map(rs), args.toArray());
    }

    public Optional<AdminTransactionResponse> find(long id) {
        return jdbc.query(SELECT + FROM + " WHERE p.payment_transaction_id = ?", (rs, n) -> map(rs), id).stream().findFirst();
    }

    private AdminTransactionResponse map(ResultSet rs) throws SQLException {
        return new AdminTransactionResponse(rs.getLong("payment_transaction_id"), rs.getString("order_code"),
                rs.getLong("user_id"), rs.getString("display_name"), rs.getString("email"), rs.getString("tier"),
                rs.getInt("amount_vnd"), rs.getString("status"), instant(rs, "created_at"), instant(rs, "paid_at"),
                rs.getObject("subscription_id", Long.class), instant(rs, "starts_at"), instant(rs, "ends_at"),
                rs.getString("subscription_status"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime().toInstant(ZoneOffset.UTC);
    }
}
