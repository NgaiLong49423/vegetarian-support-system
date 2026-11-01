package tech.mamxanh.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tech.mamxanh.admin.dto.AdminReportGroupResponse;
import tech.mamxanh.admin.dto.AdminReportResponse;
import tech.mamxanh.admin.dto.AdminReportStatusCountsResponse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/** Read side of the FR-28 Administrator queue: Recipe Post reports joined with their accounts, grouped by post for FR-30. */
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

    public List<AdminReportResponse> page(String status, Long recipeId, int page, int size) {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (status != null) {
            sql.append("AND r.status = ? ");
            args.add(status);
        }
        if (recipeId != null) {
            sql.append("AND r.recipe_id = ? ");
            args.add(recipeId);
        }
        sql.append("ORDER BY r.created_at DESC, r.report_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        args.add((long) page * size);
        args.add(size);
        return withSupplements(jdbc.query(sql.toString(), (rs, row) -> map(rs), args.toArray()));
    }

    public long count(String status, Long recipeId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM [REPORT] WHERE recipe_id IS NOT NULL ");
        List<Object> args = new ArrayList<>();
        if (status != null) {
            sql.append("AND status = ? ");
            args.add(status);
        }
        if (recipeId != null) {
            sql.append("AND recipe_id = ? ");
            args.add(recipeId);
        }
        return jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    }

    /**
     * AC-30.4 and UC-06.1: one row per reported Recipe Post with its most common matching reason, the post with the
     * newest matching report first, or with the most matching reports first. {@code search} keeps the posts whose
     * title or author name or email contains it.
     */
    public List<AdminReportGroupResponse> groups(String status, String search, boolean mostReported, int page, int size) {
        String matching = status == null ? "1 = 1" : "r.status = ?";
        String order = mostReported ? "g.report_count DESC, g.latest_report_at DESC, g.recipe_id DESC"
                : "g.latest_report_at DESC, g.recipe_id DESC";
        String sql = """
                SELECT g.recipe_id, g.report_count, g.latest_report_at, g.open_count, g.in_review_count, g.resolved_count,
                       p.title, p.status AS recipe_status,
                       au.user_id AS author_id, au.display_name AS author_name, au.email AS author_email,
                       au.role AS author_role, au.account_status AS author_status,
                       top_reason.reason_code AS top_reason_code, top_reason.reason_count AS top_reason_count
                FROM (
                    SELECT r.recipe_id,
                           SUM(CASE WHEN %1$s THEN 1 ELSE 0 END) AS report_count,
                           MAX(CASE WHEN %1$s THEN r.created_at END) AS latest_report_at,
                           SUM(CASE WHEN r.status = 'OPEN' THEN 1 ELSE 0 END) AS open_count,
                           SUM(CASE WHEN r.status = 'IN_REVIEW' THEN 1 ELSE 0 END) AS in_review_count,
                           SUM(CASE WHEN r.status = 'RESOLVED' THEN 1 ELSE 0 END) AS resolved_count
                    FROM [REPORT] r
                    WHERE r.recipe_id IS NOT NULL
                    GROUP BY r.recipe_id
                    HAVING SUM(CASE WHEN %1$s THEN 1 ELSE 0 END) > 0
                ) g
                JOIN [RECIPE_POST] p ON p.recipe_id = g.recipe_id
                JOIN [USER] au ON au.user_id = p.author_id
                CROSS APPLY (
                    SELECT TOP 1 r.reason_code, COUNT(*) AS reason_count
                    FROM [REPORT] r
                    WHERE r.recipe_id = g.recipe_id AND %1$s
                    GROUP BY r.reason_code
                    ORDER BY COUNT(*) DESC, MAX(r.created_at) DESC, MAX(r.report_id) DESC
                ) top_reason
                WHERE %2$s
                ORDER BY %3$s OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """.formatted(matching, searchClause(search), order);
        List<Object> args = new ArrayList<>();
        if (status != null) {
            args.add(status);
            args.add(status);
            args.add(status);
            args.add(status);
        }
        addSearchArgs(args, search);
        args.add((long) page * size);
        args.add(size);
        return jdbc.query(sql, (rs, row) -> new AdminReportGroupResponse(
                new AdminReportResponse.ReportedRecipe(rs.getLong("recipe_id"), rs.getString("title"), rs.getString("recipe_status")),
                account(rs, "author"), rs.getLong("report_count"),
                new AdminReportStatusCountsResponse(rs.getLong("open_count"), rs.getLong("in_review_count"),
                        rs.getLong("resolved_count")),
                time(rs.getTimestamp("latest_report_at")), rs.getString("top_reason_code"), rs.getLong("top_reason_count")),
                args.toArray());
    }

    public long countGroups(String status, String search) {
        String sql = """
                SELECT COUNT(*)
                FROM (SELECT DISTINCT r.recipe_id FROM [REPORT] r WHERE r.recipe_id IS NOT NULL AND %1$s) g
                JOIN [RECIPE_POST] p ON p.recipe_id = g.recipe_id
                JOIN [USER] au ON au.user_id = p.author_id
                WHERE %2$s
                """.formatted(status == null ? "1 = 1" : "r.status = ?", searchClause(search));
        List<Object> args = new ArrayList<>();
        if (status != null) {
            args.add(status);
        }
        addSearchArgs(args, search);
        return jdbc.queryForObject(sql, Long.class, args.toArray());
    }

    private static String searchClause(String search) {
        return search == null ? "1 = 1"
                : "(LOWER(p.title) LIKE ? ESCAPE '\\' OR LOWER(au.display_name) LIKE ? ESCAPE '\\' OR LOWER(au.email) LIKE ? ESCAPE '\\')";
    }

    /** The search text is matched literally: LIKE wildcards typed by the Administrator are escaped. */
    private static void addSearchArgs(List<Object> args, String search) {
        if (search == null) {
            return;
        }
        String pattern = "%" + search.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_").replace("[", "\\[") + "%";
        args.add(pattern);
        args.add(pattern);
        args.add(pattern);
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
        return withSupplements(jdbc.query(SELECT + "WHERE r.report_id = ?", (rs, row) -> map(rs), reportId)).stream().findFirst();
    }

    /** Loads the supplements of every listed report in one query, oldest first within each report. */
    private List<AdminReportResponse> withSupplements(List<AdminReportResponse> reports) {
        if (reports.isEmpty()) {
            return reports;
        }
        Map<Long, List<AdminReportResponse.AdminReportSupplement>> byReport = new HashMap<>();
        String ids = String.join(",", Collections.nCopies(reports.size(), "?"));
        jdbc.query("SELECT supplement_id, report_id, content, created_at FROM [REPORT_SUPPLEMENT] WHERE report_id IN ("
                + ids + ") ORDER BY created_at, supplement_id", rs -> {
                    byReport.computeIfAbsent(rs.getLong("report_id"), key -> new ArrayList<>())
                            .add(new AdminReportResponse.AdminReportSupplement(rs.getLong("supplement_id"),
                                    rs.getString("content"), time(rs.getTimestamp("created_at"))));
                }, reports.stream().map(AdminReportResponse::id).toArray());
        return reports.stream().map(report -> report.withSupplements(byReport.getOrDefault(report.id(), List.of()))).toList();
    }

    private static AdminReportResponse map(ResultSet rs) throws SQLException {
        long handlerId = rs.getLong("handler_id");
        AdminReportResponse.ReportHandler handler = rs.wasNull() ? null
                : new AdminReportResponse.ReportHandler(handlerId, rs.getString("handler_name"));
        return new AdminReportResponse(rs.getLong("report_id"), rs.getString("status"), rs.getString("reason_code"),
                rs.getString("description"), time(rs.getTimestamp("created_at")), time(rs.getTimestamp("updated_at")),
                new AdminReportResponse.ReportedRecipe(rs.getLong("recipe_id"), rs.getString("title"), rs.getString("recipe_status")),
                account(rs, "reporter"), account(rs, "author"), handler, rs.getString("decision"),
                rs.getString("decision_reason"), time(rs.getTimestamp("handled_at")), List.of());
    }

    private static AdminReportResponse.ReportAccount account(ResultSet rs, String prefix) throws SQLException {
        return new AdminReportResponse.ReportAccount(rs.getLong(prefix + "_id"), rs.getString(prefix + "_name"),
                rs.getString(prefix + "_email"), rs.getString(prefix + "_role"), rs.getString(prefix + "_status"));
    }

    private static LocalDateTime time(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
