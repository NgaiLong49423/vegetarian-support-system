package tech.mamxanh.admin.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.admin.dto.*;
import tech.mamxanh.admin.repository.AdminUserAuditRepository;
import tech.mamxanh.admin.repository.AdminUserQueryRepository;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.AccountAdministrationService;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.expertapplication.service.ExpertApplicationService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class AdminUserService {
    private final AdminUserQueryRepository queries;
    private final AdminUserAuditRepository audits;
    private final AccountAdministrationService accounts;
    private final ExpertApplicationService applications;
    private final CurrentUserService currentUser;
    private final Clock clock;

    public AdminUserService(AdminUserQueryRepository queries, AdminUserAuditRepository audits,
            AccountAdministrationService accounts, ExpertApplicationService applications,
            CurrentUserService currentUser, Clock clock) {
        this.queries = queries;
        this.audits = audits;
        this.accounts = accounts;
        this.applications = applications;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> list(String query, String role, String status, int page, int size) {
        String normalized = query == null || query.isBlank() ? null : query.strip();
        return PageResponse.of(queries.page(normalized, role, status, page, size), page, size,
                queries.count(normalized, role, status));
    }

    @Transactional(readOnly = true)
    public AdminUserSummaryResponse summary() { return queries.summary(); }

    @Transactional(readOnly = true)
    public AdminUserResponse detail(long userId) { return requireUser(userId); }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserAuditResponse> history(long userId, int page, int size) {
        requireUser(userId);
        return PageResponse.of(queries.history(userId, page, size), page, size, queries.historyCount(userId));
    }

    @Transactional
    public AdminUserResponse changeStatus(long userId, ChangeAdminUserStatusRequest request) {
        String reason = normalizedReason(request.reason());
        AccountStatus target = AccountStatus.valueOf(request.accountStatus());
        var change = accounts.changeStatus(userId, target);
        if (change.statusChanged()) {
            audits.recordStatus(userId, currentUser.requireAuthenticatedUserId(),
                    change.oldStatus().name(), change.newStatus().name(), reason, now());
        }
        return requireUser(userId);
    }

    @Transactional
    public AdminUserResponse changeRole(long userId, ChangeAdminUserRoleRequest request) {
        String reason = normalizedReason(request.reason());
        Role target = Role.valueOf(request.role());
        if (target == Role.ADMIN) throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN_ROLE_CHANGE_NOT_ALLOWED",
                "Trang này chỉ cho phép đổi vai trò giữa Thành viên và Chuyên gia.");
        var change = accounts.changeRole(userId, target);
        Long cancelledApplicationId = null;
        long administratorId = currentUser.requireAuthenticatedUserId();
        if (change.roleChanged() && change.oldRole() == Role.CUSTOMER && change.newRole() == Role.EXPERT) {
            cancelledApplicationId = applications.cancelPendingForDirectRoleGrant(userId, administratorId, reason);
        }
        if (change.roleChanged()) {
            audits.recordRole(userId, administratorId, change.oldRole().name(), change.newRole().name(),
                    reason, cancelledApplicationId, now());
        }
        return requireUser(userId);
    }

    private AdminUserResponse requireUser(long userId) {
        return queries.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND", "Không tìm thấy tài khoản."));
    }

    private static String normalizedReason(String reason) {
        String value = reason == null ? "" : reason.strip();
        if (value.length() < 10 || value.length() > 500) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Lý do phải có từ 10 đến 500 ký tự.");
        }
        return value;
    }

    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
}
