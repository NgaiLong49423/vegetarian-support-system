package tech.mamxanh.admin.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tech.mamxanh.admin.dto.AdminUserAuditResponse;
import tech.mamxanh.admin.dto.AdminUserResponse;
import tech.mamxanh.admin.dto.AdminUserSummaryResponse;

/** Bounded read side of the Administrator account-management API. */
@Repository
public class AdminUserQueryRepository {
    private static final String USER_SELECT = """
            SELECT u.user_id, u.display_name, u.email, u.role, u.account_status, u.created_at,
                   (SELECT COUNT_BIG(*) FROM [RECIPE_POST] p WHERE p.author_id=u.user_id AND p.status='PUBLISHED') AS recipe_count
            FROM [USER] u
            """;
    private final JdbcTemplate jdbc;

    public AdminUserQueryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<AdminUserResponse> page(String query, String role, String status, int page, int size) {
        StringBuilder sql = new StringBuilder(USER_SELECT).append("WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, query, role, status);
        sql.append("ORDER BY u.created_at DESC,u.user_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        args.add((long) page * size);
        args.add(size);
        return jdbc.query(sql.toString(), AdminUserQueryRepository::mapUser, args.toArray());
    }

    public long count(String query, String role, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT_BIG(*) FROM [USER] u WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, query, role, status);
        return jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    }

    public AdminUserSummaryResponse summary() {
        return jdbc.queryForObject("""
                SELECT COUNT_BIG(*) AS total_users,
                       SUM(CASE WHEN account_status='ACTIVE' THEN CONVERT(BIGINT,1) ELSE CONVERT(BIGINT,0) END) AS active_users,
                       SUM(CASE WHEN role='EXPERT' THEN CONVERT(BIGINT,1) ELSE CONVERT(BIGINT,0) END) AS expert_users,
                       SUM(CASE WHEN account_status='LOCKED' THEN CONVERT(BIGINT,1) ELSE CONVERT(BIGINT,0) END) AS locked_users
                FROM [USER]
                """, (rs, row) -> new AdminUserSummaryResponse(rs.getLong("total_users"), rs.getLong("active_users"),
                rs.getLong("expert_users"), rs.getLong("locked_users")));
    }

    public Optional<AdminUserResponse> findById(long userId) {
        return jdbc.query(USER_SELECT + "WHERE u.user_id=?", rs -> rs.next() ? Optional.of(mapUser(rs, 0)) : Optional.empty(), userId);
    }

    public List<AdminUserAuditResponse> history(long userId, int page, int size) {
        return jdbc.query("""
                SELECT event_id, action, old_value, new_value, reason, administrator_id, administrator_name, occurred_at, effective_until
                FROM (
                    SELECT CONCAT('A-',a.user_account_audit_id) AS event_id, a.action,
                           COALESCE(a.old_role,a.old_account_status,a.old_tier) AS old_value,
                           COALESCE(a.new_role,a.new_account_status,a.new_tier) AS new_value, a.reason,
                           a.administrator_id, admin.display_name AS administrator_name, a.created_at AS occurred_at,
                           a.new_tier_ends_at AS effective_until
                    FROM [USER_ACCOUNT_AUDIT] a
                    JOIN [USER] admin ON admin.user_id=a.administrator_id
                    WHERE a.target_user_id=?
                    UNION ALL
                    SELECT CONCAT('C-',ledger.transaction_id) AS event_id, 'AI_CREDIT_GRANTED' AS action,
                           CONVERT(NVARCHAR(50),ledger.credit_delta) AS old_value,
                           CONVERT(NVARCHAR(50),ledger.balance_after) AS new_value, ledger.reason,
                           ledger.granted_by_user_id AS administrator_id, grantor.display_name AS administrator_name,
                           ledger.created_at AS occurred_at, CAST(NULL AS DATETIME2(7)) AS effective_until
                    FROM AI_CREDIT_LEDGER ledger
                    JOIN [USER] grantor ON grantor.user_id=ledger.granted_by_user_id
                    WHERE ledger.user_id=? AND ledger.transaction_type='ADMIN_GRANT'
                    UNION ALL
                    SELECT CONCAT('R-',r.report_id) AS event_id, 'ACCOUNT_LOCKED' AS action,
                           'ACTIVE' AS old_value, 'LOCKED' AS new_value, r.decision_reason AS reason,
                           r.handled_by AS administrator_id, admin.display_name AS administrator_name, r.handled_at AS occurred_at,
                           CAST(NULL AS DATETIME2(7)) AS effective_until
                    FROM [REPORT] r
                    LEFT JOIN [USER] admin ON admin.user_id=r.handled_by
                    JOIN [RECIPE_POST] p ON p.recipe_id=r.recipe_id
                    WHERE p.author_id=? AND r.decision='LOCK_AUTHOR' AND r.handled_at IS NOT NULL
                ) events
                ORDER BY occurred_at DESC,event_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, (rs, row) -> mapHistoryEvent(rs), userId, userId, userId, (long) page * size, size);
    }

    private static AdminUserAuditResponse mapHistoryEvent(ResultSet rs) throws SQLException {
        String action = rs.getString("action");
        String oldValue = rs.getString("old_value");
        String newValue = rs.getString("new_value");
        if ("AI_CREDIT_GRANTED".equals(action)) {
            oldValue = compactDecimal(oldValue);
            newValue = compactDecimal(newValue);
        }
        return new AdminUserAuditResponse(rs.getString("event_id"), action, oldValue, newValue,
                rs.getString("reason"), rs.getObject("administrator_id", Long.class),
                rs.getString("administrator_name"), rs.getTimestamp("occurred_at").toLocalDateTime(),
                rs.getTimestamp("effective_until") == null ? null
                        : rs.getTimestamp("effective_until").toLocalDateTime());
    }

    private static String compactDecimal(String value) {
        return value == null ? null : new BigDecimal(value).stripTrailingZeros().toPlainString();
    }

    public long historyCount(long userId) {
        return jdbc.queryForObject("""
                SELECT (SELECT COUNT_BIG(*) FROM [USER_ACCOUNT_AUDIT] WHERE target_user_id=?) +
                       (SELECT COUNT_BIG(*) FROM AI_CREDIT_LEDGER WHERE user_id=? AND transaction_type='ADMIN_GRANT') +
                       (SELECT COUNT_BIG(*) FROM [REPORT] r JOIN [RECIPE_POST] p ON p.recipe_id=r.recipe_id
                        WHERE p.author_id=? AND r.decision='LOCK_AUTHOR' AND r.handled_at IS NOT NULL)
                """, Long.class, userId, userId, userId);
    }

    private static void appendFilters(StringBuilder sql, List<Object> args, String query, String role, String status) {
        if (role != null) { sql.append("AND u.role=? "); args.add(role); }
        if (status != null) { sql.append("AND u.account_status=? "); args.add(status); }
        if (query != null && !query.isBlank()) {
            sql.append("AND (u.display_name LIKE ? OR u.email LIKE ? OR CONVERT(NVARCHAR(30),u.user_id) LIKE ?) ");
            String pattern = "%" + query.strip().replace("[", "[[]").replace("%", "[%]").replace("_", "[_]") + "%";
            args.add(pattern); args.add(pattern); args.add(pattern);
        }
    }

    private static AdminUserResponse mapUser(ResultSet rs, int row) throws SQLException {
        return new AdminUserResponse(rs.getLong("user_id"), rs.getString("display_name"), rs.getString("email"),
                rs.getString("role"), rs.getString("account_status"), rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getLong("recipe_count"));
    }
}
