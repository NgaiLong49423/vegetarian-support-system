package tech.mamxanh.admin.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tech.mamxanh.admin.dto.AdminReportResponse;
import tech.mamxanh.admin.dto.AdminReportStatusCountsResponse;

/** Read side of the FR-28 Administrator queue: Recipe Post reports joined with their accounts. */
@Repository
public class AdminReportQueryRepository {
    private static final String SELECT = """
            SELECT r.report_id, r.status, r.reason_code, r.description, r.created_at, r.updated_at,
                   r.decision, r.decision_reason, r.handled_at,
                   p.recipe_id, p.title, p.status AS recipe_status,
                   rep.user_id AS reporter_id, rep.display_name AS reporter_name, rep.email AS reporter_email,
                   rep.role AS reporter_role, rep.account_status AS reporter_status,
                   au.user_id AS author_id, au.display_name AS author_name, au.email AS author_email,
                   au.role AS author_role, au.account_status AS author_status,
                   h.user_id AS handler_id, h.display_name AS handler_name
            FROM [REPORT] r
            JOIN [RECIPE_POST] p ON p.recipe_id = r.recipe_id
            JOIN [USER] rep ON rep.user_id = r.reporter_id
            JOIN [USER] au ON au.user_id = p.author_id
            LEFT JOIN [USER] h ON h.user_id = r.handled_by
            """;
    private final JdbcTemplate jdbc;

    public AdminReportQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AdminReportResponse> page(String status, int page, int size) {
        StringBuilder sql = new StringBuilder(SELECT);
        List<Object> args = new ArrayList<>();
        if (status != null) {
            sql.append("WHERE r.status = ? ");
            args.add(status);
        }
        sql.append("ORDER BY r.created_at DESC, r.report_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        args.add((long) page * size);
        args.add(size);
        return jdbc.query(sql.toString(), (rs, row) -> map(rs), args.toArray());
    }

    public long count(String status) {
        if (status == null) {
            return jdbc.queryForObject("SELECT COUNT(*) FROM [REPORT] WHERE recipe_id IS NOT NULL", Long.class);
        }
        return jdbc.queryForObject("SELECT COUNT(*) FROM [REPORT] WHERE recipe_id IS NOT NULL AND status = ?",
                Long.class, status);
    }

    public AdminReportStatusCountsResponse countByStatus() {
        long[] counts = new long[3];
        jdbc.query("SELECT status, COUNT(*) AS total FROM [REPORT] WHERE recipe_id IS NOT NULL GROUP BY status", rs -> {
            switch (rs.getString("status")) {
                case "OPEN" -> counts[0] = rs.getLong("total");
                case "IN_REVIEW" -> counts[1] = rs.getLong("total");
                case "RESOLVED" -> counts[2] = rs.getLong("total");
                default -> { }
            }
        });
        return new AdminReportStatusCountsResponse(counts[0], counts[1], counts[2]);
    }

    public Optional<AdminReportResponse> findById(long reportId) {
        return jdbc.query(SELECT + "WHERE r.report_id = ?", (rs, row) -> map(rs), reportId).stream().findFirst();
    }

    private static AdminReportResponse map(ResultSet rs) throws SQLException {
        long handlerId = rs.getLong("handler_id");
        AdminReportResponse.ReportHandler handler = rs.wasNull() ? null
                : new AdminReportResponse.ReportHandler(handlerId, rs.getString("handler_name"));
        return new AdminReportResponse(rs.getLong("report_id"), rs.getString("status"), rs.getString("reason_code"),
                rs.getString("description"), time(rs.getTimestamp("created_at")), time(rs.getTimestamp("updated_at")),
                new AdminReportResponse.ReportedRecipe(rs.getLong("recipe_id"), rs.getString("title"), rs.getString("recipe_status")),
                account(rs, "reporter"), account(rs, "author"), handler, rs.getString("decision"),
                rs.getString("decision_reason"), time(rs.getTimestamp("handled_at")));
    }

    private static AdminReportResponse.ReportAccount account(ResultSet rs, String prefix) throws SQLException {
        return new AdminReportResponse.ReportAccount(rs.getLong(prefix + "_id"), rs.getString(prefix + "_name"),
                rs.getString(prefix + "_email"), rs.getString(prefix + "_role"), rs.getString(prefix + "_status"));
    }

    private static LocalDateTime time(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
