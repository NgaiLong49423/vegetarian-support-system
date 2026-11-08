package tech.mamxanh.notification.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * In-app notifications written when an Administrator resolves a Recipe Post report (FR-49).
 * Messages carry the post title and the Administrator's reason only: never the reporter's name,
 * account or report text (BR-28, AC-49.4). Each one is also emailed after the decision commits (UC-49.3).
 */
@Service
public class ModerationNotificationService {
    private static final String REPORT_RESULT = "REPORT_RESULT";
    private static final String MODERATION_DECISION = "MODERATION_DECISION";
    private static final String AUTHOR_RECIPES_PATH = "/ho-so/chuyen-gia";

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public ModerationNotificationService(JdbcTemplate jdbc, Clock clock, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.events = events;
    }

    /** Links to the post only while it stays public: a hidden post would open on a "not found" page. */
    public void reportResult(long reporterId, long reportId, long recipeId, String recipeTitle, boolean violationFound,
            boolean postStillPublic) {
        String outcome = violationFound ? "đã xử lý nội dung vi phạm" : "chưa thấy nội dung vi phạm";
        insert(reporterId, reportId, REPORT_RESULT, "Báo cáo của bạn đã có kết quả",
                "Quản trị viên đã xem xét báo cáo của bạn về bài “%s” và %s. Cảm ơn bạn đã góp ý."
                        .formatted(recipeTitle, outcome),
                postStillPublic ? "/cong-thuc/id/" + recipeId : null);
    }

    public void authorWarned(long authorId, long reportId, String recipeTitle, String reason) {
        insert(authorId, reportId, MODERATION_DECISION, "Bài công thức của bạn nhận được cảnh báo",
                "Quản trị viên đã xem xét bài “%s” và gửi cảnh báo đến bạn. Lý do: %s".formatted(recipeTitle, reason),
                AUTHOR_RECIPES_PATH);
    }

    public void recipeHidden(long authorId, long reportId, String recipeTitle, String reason) {
        insert(authorId, reportId, MODERATION_DECISION, "Bài công thức của bạn đã bị ẩn",
                "Quản trị viên đã ẩn bài “%s” khỏi trang công khai. Lý do: %s".formatted(recipeTitle, reason),
                AUTHOR_RECIPES_PATH);
    }

    public void accountLocked(long authorId, long reportId, String recipeTitle, String reason) {
        insert(authorId, reportId, MODERATION_DECISION, "Tài khoản của bạn đã bị khóa",
                "Quản trị viên đã khóa tài khoản của bạn sau khi xem xét bài “%s”. Lý do: %s".formatted(recipeTitle, reason),
                null);
    }

    private void insert(long userId, long reportId, String type, String title, String message, String targetPath) {
        jdbc.update("INSERT INTO [NOTIFICATION](user_id,report_id,notification_type,title,message,target_path,created_at) VALUES(?,?,?,?,?,?,?)",
                userId, reportId, type, title, message, targetPath, LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        // FR-49 sends moderation outcomes regardless of the reply email preference, to verified addresses only.
        // A locked author is still told why by email, since the account can no longer sign in.
        jdbc.query("SELECT email, display_name FROM [USER] WHERE user_id = ? AND email_verified = 1", rs -> {
            events.publishEvent(new ModerationEmailRequested(rs.getString("email"), rs.getString("display_name"),
                    title, message, targetPath));
        }, userId);
    }
}
