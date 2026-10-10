package tech.mamxanh.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tech.mamxanh.common.validation.PasswordConfirmation;
import tech.mamxanh.common.validation.PasswordsMatch;
import tech.mamxanh.common.validation.ValidPassword;

/** {@code POST /api/v1/auth/password-resets/confirm}; springdoc exposes this DTO in runtime OpenAPI. */
@PasswordsMatch
public record PasswordResetConfirmRequest(
        @NotBlank(message = "Thiếu mã đặt lại mật khẩu.")
        @Size(max = 128, message = "Mã đặt lại mật khẩu không hợp lệ.")
        String token,

        @ValidPassword
        String newPassword,

        @NotNull(message = "Vui lòng nhập lại mật khẩu để xác nhận.")
        String confirmPassword) implements PasswordConfirmation {

    /** The password that {@code confirmPassword} must match. */
    @Override
    public String password() {
        return newPassword;
    }

    @Override
    public String toString() {
        return "PasswordResetConfirmRequest[redacted]";
    }
}
