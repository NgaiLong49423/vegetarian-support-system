package tech.mamxanh.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tech.mamxanh.admin.dto.AdminUserResponse;
import tech.mamxanh.admin.dto.ChangeAdminUserRoleRequest;
import tech.mamxanh.admin.dto.ChangeAdminUserStatusRequest;
import tech.mamxanh.admin.repository.AdminUserAuditRepository;
import tech.mamxanh.admin.repository.AdminUserQueryRepository;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.AccountAdministrationService;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.expertapplication.service.ExpertApplicationService;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 12, 0);
    @Mock private AdminUserQueryRepository queries;
    @Mock private AdminUserAuditRepository audits;
    @Mock private AccountAdministrationService accounts;
    @Mock private ExpertApplicationService applications;
    @Mock private CurrentUserService currentUser;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(queries, audits, accounts, applications, currentUser,
                Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void directExpertGrantCancelsPendingApplicationAndAuditsInSameOperation() {
        when(currentUser.requireAuthenticatedUserId()).thenReturn(7L);
        when(queries.findById(42L)).thenReturn(Optional.of(user(Role.EXPERT, AccountStatus.ACTIVE)));
        when(accounts.changeRole(42L, Role.EXPERT)).thenReturn(change(Role.CUSTOMER, Role.EXPERT,
                AccountStatus.ACTIVE, AccountStatus.ACTIVE));
        when(applications.cancelPendingForDirectRoleGrant(42L, 7L, "Đủ điều kiện chia sẻ công thức"))
                .thenReturn(91L);

        assertThat(service.changeRole(42L,
                new ChangeAdminUserRoleRequest("EXPERT", "Đủ điều kiện chia sẻ công thức")).role()).isEqualTo("EXPERT");

        verify(applications).cancelPendingForDirectRoleGrant(42L, 7L, "Đủ điều kiện chia sẻ công thức");
        verify(audits).recordRole(42L, 7L, "CUSTOMER", "EXPERT", "Đủ điều kiện chia sẻ công thức", 91L, NOW);
    }

    @Test
    void directExpertGrantWithoutPendingApplicationStillAuditsRoleChange() {
        when(currentUser.requireAuthenticatedUserId()).thenReturn(7L);
        when(queries.findById(42L)).thenReturn(Optional.of(user(Role.EXPERT, AccountStatus.ACTIVE)));
        when(accounts.changeRole(42L, Role.EXPERT)).thenReturn(change(Role.CUSTOMER, Role.EXPERT,
                AccountStatus.ACTIVE, AccountStatus.ACTIVE));
        when(applications.cancelPendingForDirectRoleGrant(42L, 7L, "Đủ điều kiện chia sẻ công thức"))
                .thenReturn(null);

        service.changeRole(42L, new ChangeAdminUserRoleRequest("EXPERT", "Đủ điều kiện chia sẻ công thức"));

        verify(audits).recordRole(42L, 7L, "CUSTOMER", "EXPERT", "Đủ điều kiện chia sẻ công thức", null, NOW);
    }

    @Test
    void unchangedStatusDoesNotCreateAudit() {
        when(accounts.changeStatus(42L, AccountStatus.ACTIVE)).thenReturn(change(Role.CUSTOMER, Role.CUSTOMER,
                AccountStatus.ACTIVE, AccountStatus.ACTIVE));
        when(queries.findById(42L)).thenReturn(Optional.of(user(Role.CUSTOMER, AccountStatus.ACTIVE)));

        service.changeStatus(42L, new ChangeAdminUserStatusRequest("ACTIVE", "Xác nhận tài khoản hợp lệ"));

        verify(audits, never()).recordStatus(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void trimsAndValidatesReasonBeforeChangingAccount() {
        assertThatThrownBy(() -> service.changeRole(42L,
                new ChangeAdminUserRoleRequest("EXPERT", "   ngắn   ")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(accounts, never()).changeRole(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    private static AccountAdministrationService.AccountChange change(Role oldRole, Role newRole,
            AccountStatus oldStatus, AccountStatus newStatus) {
        return new AccountAdministrationService.AccountChange(42L, oldRole, newRole, oldStatus, newStatus);
    }

    private static AdminUserResponse user(Role role, AccountStatus status) {
        return new AdminUserResponse(42L, "Người dùng", "user@example.test", role.name(), status.name(), NOW, 0);
    }
}
