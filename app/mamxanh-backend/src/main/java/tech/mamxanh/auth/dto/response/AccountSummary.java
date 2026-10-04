package tech.mamxanh.auth.dto.response;

import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.entity.User;

/** openapi.yaml AccountSummary: the signed-in account, never the password hash or tokens. */
public record AccountSummary(
        Long id,
        String displayName,
        String email,
        String avatarUrl,
        Role role,
        AccountStatus accountStatus,
        boolean emailVerified) {

    public static AccountSummary from(User user) {
        return new AccountSummary(user.getId(), user.getDisplayName(), user.getEmail(), user.getAvatarUrl(),
                user.getRole(), user.getAccountStatus(), user.isEmailVerified());
    }
}
