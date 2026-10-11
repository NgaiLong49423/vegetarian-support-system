package tech.mamxanh.expertapplication.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.expertapplication.dto.ExpertApplicationRequest;
import tech.mamxanh.expertapplication.dto.ExpertApplicationResponse;

@Repository
public class ExpertApplicationRepository {
    private static final String SELECT = "SELECT a.application_id,a.user_id,u.display_name,u.email,a.bio_experience,a.vegetarian_type,a.sample_recipe_summary,a.portfolio_url,a.status,a.admin_note,a.created_at,a.reviewed_at FROM [EXPERT_APPLICATION] a JOIN [USER] u ON u.user_id=a.user_id ";
    private final JdbcTemplate jdbc;

    public ExpertApplicationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long insert(long userId, ExpertApplicationRequest request, LocalDateTime now) {
        return jdbc.queryForObject("INSERT INTO [EXPERT_APPLICATION](user_id,bio_experience,vegetarian_type,sample_recipe_summary,portfolio_url,status,created_at,updated_at) OUTPUT INSERTED.application_id VALUES(?,?,?,?,?,'PENDING',?,?)",
                Long.class, userId, request.experience().trim(), request.vegetarianType(), request.sampleRecipeSummary().trim(), blankToNull(request.portfolioUrl()), now, now);
    }

    public List<ExpertApplicationResponse> page(Long userId, String status, int page, int size) {
        StringBuilder sql = new StringBuilder(SELECT);
        var args = new java.util.ArrayList<Object>();
        if (userId != null) { sql.append("WHERE a.user_id=? "); args.add(userId); }
        if (status != null && !status.isBlank()) { sql.append(userId == null ? "WHERE " : "AND ").append("a.status=? "); args.add(status); }
        sql.append("ORDER BY a.created_at DESC,a.application_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        args.add(page * size); args.add(size);
        return jdbc.query(sql.toString(), (rs, row) -> map(rs), args.toArray());
    }

    public long count(Long userId, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM [EXPERT_APPLICATION]");
        var args = new java.util.ArrayList<Object>();
        if (userId != null) { sql.append(" WHERE user_id=?"); args.add(userId); }
        if (status != null && !status.isBlank()) { sql.append(userId == null ? " WHERE " : " AND ").append("status=?"); args.add(status); }
        return jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    }

    public ExpertApplicationResponse findById(long id) {
        var result = jdbc.query(SELECT + "WHERE a.application_id=?", (rs, row) -> map(rs), id);
        if (result.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy đơn đăng ký.");
        return result.get(0);
    }

    public int transitionPending(long id, String status, String note, long reviewer, LocalDateTime now) {
        return jdbc.update("UPDATE [EXPERT_APPLICATION] SET status=?,admin_note=?,reviewed_by=?,reviewed_at=?,updated_at=? WHERE application_id=? AND status='PENDING'",
                status, note, reviewer, now, now, id);
    }

    public long applicantId(long id) { return jdbc.queryForObject("SELECT user_id FROM [EXPERT_APPLICATION] WHERE application_id=?", Long.class, id); }

    private static ExpertApplicationResponse map(ResultSet rs) throws SQLException {
        return new ExpertApplicationResponse(rs.getLong("application_id"), rs.getLong("user_id"), rs.getString("display_name"), rs.getString("email"),
                rs.getString("bio_experience"), rs.getString("vegetarian_type"), rs.getString("sample_recipe_summary"), rs.getString("portfolio_url"),
                rs.getString("status"), rs.getString("admin_note"), rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("reviewed_at") == null ? null : rs.getTimestamp("reviewed_at").toLocalDateTime());
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
