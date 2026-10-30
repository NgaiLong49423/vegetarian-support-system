package tech.mamxanh.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.admin.dto.AdminUserResponse;
import tech.mamxanh.admin.dto.AdminUserSummaryResponse;
import tech.mamxanh.admin.dto.ChangeAdminUserRoleRequest;
import tech.mamxanh.admin.dto.ChangeAdminUserStatusRequest;
import tech.mamxanh.admin.service.AdminUserService;
import tech.mamxanh.common.response.PageResponse;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {
    @Mock private AdminUserService service;
    private AdminUserController controller;

    @BeforeEach
    void setUp() { controller = new AdminUserController(service); }

    @Test
    void delegatesListingSummaryAndDetails() {
        PageResponse<AdminUserResponse> page = PageResponse.of(List.of(), 0, 20, 0);
        AdminUserResponse account = account();
        when(service.list("an", "CUSTOMER", "ACTIVE", 0, 20)).thenReturn(page);
        when(service.summary()).thenReturn(new AdminUserSummaryResponse(1, 1, 0, 0));
        when(service.detail(42L)).thenReturn(account);

        assertThat(controller.list("an", "CUSTOMER", "ACTIVE", 0, 20)).isSameAs(page);
        assertThat(controller.summary().totalUsers()).isEqualTo(1);
        assertThat(controller.detail(42L)).isSameAs(account);
    }

    @Test
    void delegatesHistoryAndAccountChanges() {
        PageResponse<tech.mamxanh.admin.dto.AdminUserAuditResponse> history = PageResponse.of(List.of(), 1, 10, 0);
        AdminUserResponse account = account();
        var status = new ChangeAdminUserStatusRequest("LOCKED", "Vi phạm quy định cộng đồng");
        var role = new ChangeAdminUserRoleRequest("EXPERT", "Đã xem xét hồ sơ chuyên gia");
        when(service.history(42L, 1, 10)).thenReturn(history);
        when(service.changeStatus(42L, status)).thenReturn(account);
        when(service.changeRole(42L, role)).thenReturn(account);

        assertThat(controller.history(42L, 1, 10)).isSameAs(history);
        assertThat(controller.changeStatus(42L, status)).isSameAs(account);
        assertThat(controller.changeRole(42L, role)).isSameAs(account);
        verify(service).history(42L, 1, 10);
    }

    private static AdminUserResponse account() {
        return new AdminUserResponse(42L, "Nguyễn An", "an@example.test", "CUSTOMER", "ACTIVE",
                LocalDateTime.of(2026, 10, 20, 8, 0), 3L);
    }
}
