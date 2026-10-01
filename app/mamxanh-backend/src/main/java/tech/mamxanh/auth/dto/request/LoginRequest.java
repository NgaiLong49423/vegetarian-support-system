package tech.mamxanh.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tech.mamxanh.common.validation.EmailAddress;

/**
 * {@code POST /api/v1/auth/login} (openapi.yaml LoginRequest). Only the shape is validated here;
 * the password strength rules apply at registration, and a wrong password is reported with the
 * neutral {@code INVALID_CREDENTIALS} answer instead of a validation error.
 */
public record LoginRequest(
        @NotNull(message = "Vui lòng nhập email.")
        @Size(max = EmailAddress.MAX_LENGTH, message = "Email tối đa 255 ký tự.")
        @Pattern(regexp = EmailAddress.PATTERN, message = "Vui lòng nhập địa chỉ email hợp lệ.")
        String email,

        @NotBlank(message = "Vui lòng nhập mật khẩu.")
        @Size(max = 64, message = "Mật khẩu tối đa 64 ký tự.")
        String password) {

    public LoginRequest {
        email = EmailAddress.normalize(email);
    }

    @Override
    public String toString() {
        return "LoginRequest[redacted]";
    }
}
