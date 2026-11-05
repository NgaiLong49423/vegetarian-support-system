package tech.mamxanh.expertapplication.repository;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.expertapplication.dto.ExpertApplicationAttachment;
import tech.mamxanh.expertapplication.dto.ExpertApplicationRequest;
import tech.mamxanh.expertapplication.dto.ExpertApplicationResponse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ExpertApplicationRepository {
    private static final String SELECT = "SELECT a.application_id,a.user_id,u.display_name,u.email,a.bio_experience,a.vegetarian_type,a.sample_recipe_summary,a.portfolio_url,a.certificate_blob_key,a.application_avatar_blob_key,a.status,a.admin_note,a.created_at,a.reviewed_at FROM [EXPERT_APPLICATION] a JOIN [USER] u ON u.user_id=a.user_id ";
    private final JdbcTemplate jdbc;

    public ExpertApplicationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long insert(long userId, ExpertApplicationRequest request, LocalDateTime now) {
        return insert(userId, request, null, null, null, null, now);
    }

    public long insert(long userId, ExpertApplicationRequest request, String certificateBlobKey,
            String certificateContentType, String avatarBlobKey, String avatarContentType, LocalDateTime now) {
        return jdbc.queryForObject("INSERT INTO [EXPERT_APPLICATION](user_id,bio_experience,vegetarian_type,sample_recipe_summary,portfolio_url,certificate_blob_key,certificate_content_type,application_avatar_blob_key,application_avatar_content_type,status,created_at,updated_at) OUTPUT INSERTED.application_id VALUES(?,?,?,?,?,?,?,?,?,'PENDING',?,?)",
                Long.class, userId, request.experience().trim(), request.vegetarianType(),
                request.sampleRecipeSummary().trim(), blankToNull(request.portfolioUrl()), certificateBlobKey,
                certificateContentType, avatarBlobKey, avatarContentType, now, now);
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

    public Long cancelPendingForDirectRoleGrant(long userId, long reviewer, String note, LocalDateTime now) {
        List<Long> ids = jdbc.query("UPDATE [EXPERT_APPLICATION] SET status='CANCELLED',admin_note=?,reviewed_by=?,reviewed_at=?,updated_at=? OUTPUT INSERTED.application_id WHERE user_id=? AND status='PENDING'",
                (rs, row) -> rs.getLong(1), note, reviewer, now, now, userId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public Long applicantId(long id) {
        List<Long> ids = jdbc.query("SELECT user_id FROM [EXPERT_APPLICATION] WHERE application_id=?", (rs, row) -> rs.getLong(1), id);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public ExpertApplicationAttachment attachment(long id, String kind) {
        String columns = switch (kind) {
            case "certificate" -> "certificate_blob_key,certificate_content_type";
            case "avatar" -> "application_avatar_blob_key,application_avatar_content_type";
            default -> throw new IllegalArgumentException("Unsupported expert application attachment type");
        };
        var results = jdbc.query("SELECT " + columns + " FROM [EXPERT_APPLICATION] WHERE application_id=?",
                (rs, row) -> new ExpertApplicationAttachment(rs.getString(1), rs.getString(2)), id);
        if (results.isEmpty() || results.get(0).blobKey() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy tệp đính kèm của đơn.");
        }
        return results.get(0);
    }

    private static ExpertApplicationResponse map(ResultSet rs) throws SQLException {
        return new ExpertApplicationResponse(rs.getLong("application_id"), rs.getLong("user_id"), rs.getString("display_name"), rs.getString("email"),
                rs.getString("bio_experience"), rs.getString("vegetarian_type"), rs.getString("sample_recipe_summary"), rs.getString("portfolio_url"),
                rs.getString("certificate_blob_key") != null, rs.getString("application_avatar_blob_key") != null,
                rs.getString("status"), rs.getString("admin_note"), rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("reviewed_at") == null ? null : rs.getTimestamp("reviewed_at").toLocalDateTime());
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
