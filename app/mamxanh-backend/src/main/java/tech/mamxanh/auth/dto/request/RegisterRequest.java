package tech.mamxanh.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tech.mamxanh.common.validation.EmailAddress;
import tech.mamxanh.common.validation.PasswordConfirmation;
import tech.mamxanh.common.validation.PasswordsMatch;
import tech.mamxanh.common.validation.ValidPassword;

/** {@code POST /api/v1/auth/register} (openapi.yaml RegisterRequest). */
@PasswordsMatch
public record RegisterRequest(
        @NotNull(message = "Vui lòng nhập tên hiển thị.")
        @Size(min = 3, max = 50, message = "Tên hiển thị cần từ 3 đến 50 ký tự.")
        String displayName,

        @NotNull(message = "Vui lòng nhập email.")
        @Size(max = EmailAddress.MAX_LENGTH, message = "Email tối đa 255 ký tự.")
        @Pattern(regexp = EmailAddress.PATTERN, message = "Vui lòng nhập địa chỉ email hợp lệ.")
        String email,

        @ValidPassword
        String password,

        @NotNull(message = "Vui lòng nhập lại mật khẩu để xác nhận.")
        String confirmPassword) implements PasswordConfirmation {

    public RegisterRequest {
        displayName = displayName == null ? null : displayName.strip();
        email = EmailAddress.normalize(email);
    }

    @Override
    public String toString() {
        return "RegisterRequest[redacted]";
    }
}
