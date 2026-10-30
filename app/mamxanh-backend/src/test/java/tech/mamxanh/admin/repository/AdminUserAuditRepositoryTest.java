package tech.mamxanh.admin.repository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AdminUserAuditRepositoryTest {
    @Mock private JdbcTemplate jdbc;
    @InjectMocks private AdminUserAuditRepository repository;

    @Test
    void recordsStatusChangeWithCleanReasonAndTimestamp() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 29, 12, 0);
        repository.recordStatus(42L, 7L, "ACTIVE", "LOCKED", "  Community violation  ", now);

        verify(jdbc).update(eq("INSERT INTO [USER_ACCOUNT_AUDIT](target_user_id,administrator_id,action,old_account_status,new_account_status,reason,created_at) VALUES(?,?,'STATUS_CHANGED',?,?,?,?)"),
                eq(42L), eq(7L), eq("ACTIVE"), eq("LOCKED"), eq("Community violation"), eq(now));
    }

    @Test
    void recordsRoleChangeAndOptionalCancelledApplication() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 29, 12, 0);
        repository.recordRole(42L, 7L, "CUSTOMER", "EXPERT", "  Direct grant  ", 91L, now);

        verify(jdbc).update(eq("INSERT INTO [USER_ACCOUNT_AUDIT](target_user_id,administrator_id,action,old_role,new_role,reason,expert_application_id,created_at) VALUES(?,?,'ROLE_CHANGED',?,?,?,?,?)"),
                eq(42L), eq(7L), eq("CUSTOMER"), eq("EXPERT"), eq("Direct grant"), eq(91L), eq(now));
    }
}
