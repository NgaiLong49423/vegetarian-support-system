package tech.mamxanh.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Public notification write boundary; inbox/read workflows belong to FR-49. */
@Service
public class NotificationService {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public NotificationService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public void expertApplicationDecision(long userId, long applicationId, boolean approved, String reason) {
        String type = approved ? "EXPERT_APPLICATION_APPROVED" : "EXPERT_APPLICATION_REJECTED";
        String title = approved ? "Đơn đăng ký Chuyên gia được duyệt" : "Đơn đăng ký Chuyên gia bị từ chối";
        String message = approved ? "Bạn đã trở thành Chuyên gia ẩm thực Mâm Xanh."
                : "Đơn đăng ký Chuyên gia bị từ chối. Lý do: " + reason;
        jdbc.update("INSERT INTO [NOTIFICATION](user_id,notification_type,title,message,target_path,created_at) VALUES(?,?,?,?,?,?)",
                userId, type, title, message, "/dang-ky-chuyen-gia?applicationId=" + applicationId,
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }

    public void expertApplicationCancelled(long userId, long applicationId) {
        jdbc.update("INSERT INTO [NOTIFICATION](user_id,notification_type,title,message,target_path,created_at) VALUES(?,?,?,?,?,?)",
                userId, "EXPERT_APPLICATION_CANCELLED", "Đơn đăng ký Chuyên gia đã kết thúc",
                "Đơn đang chờ của bạn được đóng vì tài khoản đã được cấp vai trò Chuyên gia trực tiếp.",
                "/dang-ky-chuyen-gia?applicationId=" + applicationId,
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
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
