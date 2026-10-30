package tech.mamxanh.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.ApiException;

/** Auth-module boundary for Administrator changes to account role and status. */
@Service
public class AccountAdministrationService {
    private final UserRepository users;
    private final Clock clock;

    public AccountAdministrationService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public AccountChange changeStatus(long targetId, AccountStatus next) {
        var user = users.findByIdForUpdate(targetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy tài khoản."));
        requireManageable(user.getRole());
        AccountChange change = new AccountChange(user.getId(), user.getRole(), user.getRole(),
                user.getAccountStatus(), next);
        if (change.statusChanged()) {
            if (next == AccountStatus.LOCKED) user.lockByAdministrator(now());
            else user.unlockByAdministrator(now());
            users.saveAndFlush(user);
        }
        return change;
    }

    @Transactional
    public AccountChange changeRole(long targetId, Role next) {
        var user = users.findByIdForUpdate(targetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy tài khoản."));
        requireManageable(user.getRole());
        if (next == Role.ADMIN) throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN_ROLE_CHANGE_NOT_ALLOWED",
                "Trang này chỉ cho phép đổi vai trò giữa Thành viên và Chuyên gia.");
        AccountChange change = new AccountChange(user.getId(), user.getRole(), next,
                user.getAccountStatus(), user.getAccountStatus());
        if (change.roleChanged()) {
            if (next == Role.EXPERT) user.promoteToExpert(now());
            else user.demoteToCustomer(now());
            users.saveAndFlush(user);
        }
        return change;
    }

    private static void requireManageable(Role role) {
        if (role == Role.ADMIN) throw new ApiException(HttpStatus.CONFLICT, "ADMIN_ACCOUNT_READ_ONLY",
                "Không thể khóa, mở khóa hoặc đổi vai trò tài khoản Quản trị viên tại đây.");
    }

    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }

    public record AccountChange(long userId, Role oldRole, Role newRole,
            AccountStatus oldStatus, AccountStatus newStatus) {
        public boolean roleChanged() { return oldRole != newRole; }
        public boolean statusChanged() { return oldStatus != newStatus; }
    }
}
