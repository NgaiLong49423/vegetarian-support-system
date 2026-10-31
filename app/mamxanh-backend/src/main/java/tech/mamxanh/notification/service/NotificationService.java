package tech.mamxanh.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
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
