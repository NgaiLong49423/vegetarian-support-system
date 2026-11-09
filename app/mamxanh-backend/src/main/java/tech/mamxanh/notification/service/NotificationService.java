package tech.mamxanh.notification.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.notification.dto.response.NotificationResponse;
import tech.mamxanh.notification.dto.response.NotificationPreferencesResponse;

/**
 * Public notification write boundary and the FR-49 inbox. Every read or update is scoped to the
 * owner's {@code user_id} (NFR-09), so another user's notification behaves as if it did not exist.
 */
@Service
public class NotificationService {
    static final String COMMENT_REPLY = "COMMENT_REPLY";

    private static final String COLUMNS = "notification_id, notification_type, title, message, target_path, is_read, "
            + "created_at, read_at";

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public NotificationService(JdbcTemplate jdbc, Clock clock, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.events = events;
    }

    public void expertApplicationDecision(long userId, long applicationId, boolean approved, String reason) {
        String type = approved ? "EXPERT_APPLICATION_APPROVED" : "EXPERT_APPLICATION_REJECTED";
        String title = approved ? "Đơn đăng ký Chuyên gia được duyệt" : "Đơn đăng ký Chuyên gia bị từ chối";
        String message = approved ? "Bạn đã trở thành Chuyên gia ẩm thực Mâm Xanh."
                : "Đơn đăng ký Chuyên gia bị từ chối. Lý do: " + reason;
        jdbc.update("INSERT INTO [NOTIFICATION](user_id,notification_type,title,message,target_path,created_at) VALUES(?,?,?,?,?,?)",
                userId, type, title, message, "/dang-ky-chuyen-gia?applicationId=" + applicationId, now());
    }

    /**
     * AC-49.1: called inside the reply transaction, so the notification commits or rolls back with the
     * reply. The reply email is only requested here and sent after commit (AC-49.2, AC-49.3).
     */
    public void commentReply(long recipientId, long replyCommentId, long recipeId, String recipeTitle, String replierName) {
        String targetPath = "/cong-thuc/id/" + recipeId + "#comment-" + replyCommentId;
        jdbc.update("""
                INSERT INTO [NOTIFICATION](user_id,comment_id,notification_type,title,message,target_path,created_at)
                VALUES(?,?,?,?,?,?,?)
                """, recipientId, replyCommentId, COMMENT_REPLY, "Có phản hồi mới cho bình luận của bạn",
                replierName + " đã trả lời bình luận của bạn trong công thức “" + recipeTitle + "”.", targetPath, now());
        jdbc.query("""
                SELECT email, display_name FROM [USER]
                WHERE user_id = ? AND reply_email_enabled = 1 AND account_status = 'ACTIVE' AND email_verified = 1
                """, rs -> {
                    events.publishEvent(new CommentReplyEmailRequested(rs.getString("email"), rs.getString("display_name"),
                            replierName, recipeTitle, targetPath));
                }, recipientId);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(long userId, int page, int size, boolean unreadOnly) {
        String filter = unreadOnly ? " AND is_read = 0" : "";
        List<NotificationResponse> content = jdbc.query("SELECT " + COLUMNS + " FROM [NOTIFICATION] WHERE user_id = ?"
                + filter + " ORDER BY created_at DESC, notification_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                (rs, row) -> map(rs), userId, (long) page * size, size);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM [NOTIFICATION] WHERE user_id = ?" + filter, Long.class, userId);
        return PageResponse.of(content, page, size, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public long unreadCount(long userId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM [NOTIFICATION] WHERE user_id = ? AND is_read = 0",
                Long.class, userId);
        return count == null ? 0 : count;
    }

    /** UC-49.2; idempotent, so a notification already read keeps its first {@code read_at}. */
    @Transactional
    public NotificationResponse markRead(long userId, long notificationId) {
        jdbc.update("UPDATE [NOTIFICATION] SET is_read = 1, read_at = ? WHERE user_id = ? AND notification_id = ? AND is_read = 0",
                now(), userId, notificationId);
        return jdbc.query("SELECT " + COLUMNS + " FROM [NOTIFICATION] WHERE user_id = ? AND notification_id = ?",
                        (rs, row) -> map(rs), userId, notificationId)
                .stream().findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Transactional
    public void markAllRead(long userId) {
        jdbc.update("UPDATE [NOTIFICATION] SET is_read = 1, read_at = ? WHERE user_id = ? AND is_read = 0", now(), userId);
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse preferences(long userId) {
        Boolean enabled = jdbc.queryForObject("SELECT reply_email_enabled FROM [USER] WHERE user_id = ?", Boolean.class, userId);
        return new NotificationPreferencesResponse(Boolean.TRUE.equals(enabled));
    }

    /** UC-49.4: only the email channel can be turned off; in-app notifications are always created. */
    @Transactional
    public NotificationPreferencesResponse updatePreferences(long userId, boolean replyEmailEnabled) {
        jdbc.update("UPDATE [USER] SET reply_email_enabled = ?, updated_at = ? WHERE user_id = ?", replyEmailEnabled, now(), userId);
        return preferences(userId);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private static NotificationResponse map(ResultSet rs) throws SQLException {
        Timestamp readAt = rs.getTimestamp("read_at");
        return new NotificationResponse(rs.getLong("notification_id"), rs.getString("notification_type"),
                rs.getString("title"), rs.getString("message"), rs.getString("target_path"), rs.getBoolean("is_read"),
                rs.getTimestamp("created_at").toLocalDateTime(), readAt == null ? null : readAt.toLocalDateTime());
    }

    public void expertApplicationCancelled(long userId, long applicationId) {
        jdbc.update("INSERT INTO [NOTIFICATION](user_id,notification_type,title,message,target_path,created_at) VALUES(?,?,?,?,?,?)",
                userId, "EXPERT_APPLICATION_CANCELLED", "Đơn đăng ký Chuyên gia đã kết thúc",
                "Đơn đang chờ của bạn được đóng vì tài khoản đã được cấp vai trò Chuyên gia trực tiếp.",
                "/dang-ky-chuyen-gia?applicationId=" + applicationId,
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }

    public void aiCreditGranted(long userId, BigDecimal credits, String reason) {
        jdbc.update("INSERT INTO [NOTIFICATION](user_id,notification_type,title,message,target_path,created_at) VALUES(?,?,?,?,?,?)",
                userId, "AI_CREDIT_GRANTED", "Bạn được cộng AI Credit",
                "Quản trị viên đã cộng " + credits.stripTrailingZeros().toPlainString()
                        + " AI Credit vào ví. Lý do: " + reason,
                "/goi-ai", LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }

    public void aiCreditTopUpNeedsReconciliation(long userId, String orderCode, int amountVnd) {
        jdbc.update("INSERT INTO [NOTIFICATION](user_id,notification_type,title,message,target_path,created_at) VALUES(?,?,?,?,?,?)",
                userId, "AI_CREDIT_RECONCILE", "Cần đối soát giao dịch nạp AI Credit",
                "Giao dịch nạp AI Credit đơn #" + orderCode + " cần đối soát vì thanh toán được ghi nhận sau hạn hoặc chưa xác minh được thời điểm. AI Credit chưa được cộng tự động; bộ phận hỗ trợ sẽ đối soát khoản "
                        + amountVnd + "đ.",
                "/giao-dich", LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }

    public void aiCreditTopUpReconciled(long userId, BigDecimal credits) {
        jdbc.update("INSERT INTO [NOTIFICATION](user_id,notification_type,title,message,target_path,created_at) VALUES(?,?,?,?,?,?)",
                userId, "AI_CREDIT_RECONCILE", "Giao dịch nạp AI Credit đã được xác minh",
                credits.stripTrailingZeros().toPlainString() + " AI Credit đã được cộng vào ví của bạn sau khi xác minh thời điểm thanh toán.",
                "/goi-ai", LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }

    public void aiPackageGifted(long userId, String tier, LocalDateTime endsAt, String reason) {
        String period = endsAt == null ? " theo quota Free hằng tháng" : " đến " + endsAt.toLocalDate();
        jdbc.update("INSERT INTO [NOTIFICATION](user_id,notification_type,title,message,target_path,created_at) VALUES(?,?,?,?,?,?)",
                userId, "AI_PACKAGE_GIFTED", "Bạn được tặng gói AI " + tier,
                "Quản trị viên đã tặng bạn gói " + tier + period + ". Lý do: " + reason,
                "/goi-ai", LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }

    /** Persists one in-app notice per changed AI setting for every active Customer and Expert. */
    public void aiConfigurationChanged(List<String> changes) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        for (String message : changes) {
            jdbc.update("""
                    INSERT INTO [NOTIFICATION]
                        (user_id,notification_type,title,message,target_path,created_at)
                    SELECT user_id,?,?,?,?,?
                    FROM [USER]
                    WHERE account_status = 'ACTIVE' AND role IN ('CUSTOMER','EXPERT')
                    """, "AI_CONFIG_CHANGED", "Cập nhật cấu hình AI", message, "/goi-ai", now);
        }
    }
}
