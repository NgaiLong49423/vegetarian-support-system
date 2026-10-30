package tech.mamxanh.notification.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 12, 0);
    @Mock private JdbcTemplate jdbc;

    @Test
    void notifiesApplicantWhenAnAdministratorEndsPendingApplicationAfterDirectGrant() {
        NotificationService service = new NotificationService(jdbc,
                Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"), ZoneOffset.UTC));

        service.expertApplicationCancelled(42L, 91L);

        verify(jdbc).update(eq("INSERT INTO [NOTIFICATION](user_id,notification_type,title,message,target_path,created_at) VALUES(?,?,?,?,?,?)"),
                eq(42L), eq("EXPERT_APPLICATION_CANCELLED"), eq("Đơn đăng ký Chuyên gia đã kết thúc"),
                eq("Đơn đang chờ của bạn được đóng vì tài khoản đã được cấp vai trò Chuyên gia trực tiếp."),
                eq("/dang-ky-chuyen-gia?applicationId=91"), eq(NOW));
    }
}
