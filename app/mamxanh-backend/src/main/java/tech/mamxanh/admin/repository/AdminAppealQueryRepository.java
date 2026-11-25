package tech.mamxanh.admin.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tech.mamxanh.admin.dto.AdminAppealResponse;
import tech.mamxanh.admin.dto.AdminAppealResponse.AppealAuthor;
import tech.mamxanh.admin.dto.RecipeAppealResponse;
import tech.mamxanh.admin.entity.AppealStatus;

/** Read side of the appeal queue: hide reports that carry an appeal, joined with the post and its author. */
@Repository
public class AdminAppealQueryRepository {
    /**
     * An open appeal stays in the queue only while it still applies: the post is hidden and this report is its
     * latest hide decision. Superseded appeals and appeals on deleted posts cannot be decided (FR-28, BR-27).
     */
    private static final String CURRENT_OR_CLOSED = """
            AND (r.appeal_status IN ('RESTORED', 'KEPT_HIDDEN')
                 OR (EXISTS (SELECT 1 FROM [RECIPE_POST] cp WHERE cp.recipe_id = r.recipe_id AND cp.status = 'HIDDEN')
                     AND r.report_id = (SELECT TOP 1 h.report_id FROM [REPORT] h
                                        WHERE h.recipe_id = r.recipe_id AND h.decision = 'HIDE_RECIPE'
                                        ORDER BY h.handled_at DESC, h.report_id DESC)))
            """;
    private static final String SELECT = """
            SELECT r.report_id, r.recipe_id, p.title, r.decision_reason, r.handled_at,
                   r.appeal_status, r.appeal_message, r.appealed_at, r.appeal_resubmitted_at,
                   r.appeal_decision_reason, r.appeal_handled_at,
                   au.user_id AS author_id, au.display_name AS author_name, au.email AS author_email,
                   au.account_status AS author_status
            FROM [REPORT] r
            JOIN [RECIPE_POST] p ON p.recipe_id = r.recipe_id
            JOIN [USER] au ON au.user_id = p.author_id
            WHERE r.appeal_status IS NOT NULL
            """;
    private final JdbcTemplate jdbc;

    public AdminAppealQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Newest activity first: the resubmission time when there is one, otherwise the appeal time. */
    public List<AdminAppealResponse> page(Collection<String> statuses, int page, int size) {
        List<Object> args = new ArrayList<>();
        String sql = SELECT + CURRENT_OR_CLOSED + statusFilter(statuses, args)
                + "ORDER BY COALESCE(r.appeal_resubmitted_at, r.appealed_at) DESC, r.report_id DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        args.add((long) page * size);
        args.add(size);
        return jdbc.query(sql, (rs, row) -> map(rs), args.toArray());
    }

    public long count(Collection<String> statuses) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM [REPORT] r WHERE r.appeal_status IS NOT NULL " + CURRENT_OR_CLOSED
                + statusFilter(statuses, args);
        return jdbc.queryForObject(sql, Long.class, args.toArray());
    }

    public Optional<AdminAppealResponse> find(long reportId) {
        return jdbc.query(SELECT + "AND r.report_id = ?", (rs, row) -> map(rs), reportId).stream().findFirst();
    }

    private static String statusFilter(Collection<String> statuses, List<Object> args) {
        if (statuses == null || statuses.isEmpty()) {
            return "";
        }
        args.addAll(statuses);
        return "AND r.appeal_status IN (" + String.join(",", statuses.stream().map(s -> "?").toList()) + ") ";
    }

    private static AdminAppealResponse map(ResultSet rs) throws SQLException {
        RecipeAppealResponse appeal = new RecipeAppealResponse(rs.getLong("report_id"), rs.getLong("recipe_id"),
                rs.getString("title"), rs.getString("decision_reason"), time(rs, "handled_at"),
                AppealStatus.valueOf(rs.getString("appeal_status")), rs.getString("appeal_message"),
                time(rs, "appealed_at"), time(rs, "appeal_resubmitted_at"), rs.getString("appeal_decision_reason"),
                time(rs, "appeal_handled_at"));
        AppealAuthor author = new AppealAuthor(rs.getLong("author_id"), rs.getString("author_name"),
                rs.getString("author_email"), rs.getString("author_status"));
        return new AdminAppealResponse(appeal, author, null);
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
