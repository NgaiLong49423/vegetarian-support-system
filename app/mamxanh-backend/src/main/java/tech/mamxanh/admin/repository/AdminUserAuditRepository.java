package tech.mamxanh.admin.repository;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminUserAuditRepository {
    private final JdbcTemplate jdbc;
    public AdminUserAuditRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void recordStatus(long targetId, long administratorId, String oldStatus, String newStatus,
            String reason, LocalDateTime now) {
        jdbc.update("INSERT INTO [USER_ACCOUNT_AUDIT](target_user_id,administrator_id,action,old_account_status,new_account_status,reason,created_at) VALUES(?,?,'STATUS_CHANGED',?,?,?,?)",
                targetId, administratorId, oldStatus, newStatus, reason.strip(), now);
    }

    public void recordRole(long targetId, long administratorId, String oldRole, String newRole,
            String reason, Long cancelledApplicationId, LocalDateTime now) {
        jdbc.update("INSERT INTO [USER_ACCOUNT_AUDIT](target_user_id,administrator_id,action,old_role,new_role,reason,expert_application_id,created_at) VALUES(?,?,'ROLE_CHANGED',?,?,?,?,?)",
                targetId, administratorId, oldRole, newRole, reason.strip(), cancelledApplicationId, now);
    }

    public void recordAiPackageGift(long targetId, long administratorId, String oldTier, String newTier,
            LocalDateTime endsAt, String reason, LocalDateTime now) {
        jdbc.update("INSERT INTO [USER_ACCOUNT_AUDIT](target_user_id,administrator_id,action,old_tier,new_tier,new_tier_ends_at,reason,created_at) VALUES(?,?,'AI_PACKAGE_GIFTED',?,?,?,?,?)",
                targetId, administratorId, oldTier, newTier, endsAt, reason.strip(), now);
    }
}
