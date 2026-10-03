package tech.mamxanh.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tech.mamxanh.common.validation.EmailAddress;

/** Email-only request payload exposed in generated runtime OpenAPI. */
public record EmailRequest(
        @NotNull(message = "Vui lòng nhập email.")
        @Size(max = EmailAddress.MAX_LENGTH, message = "Email tối đa 255 ký tự.")
        @Pattern(regexp = EmailAddress.PATTERN, message = "Vui lòng nhập địa chỉ email hợp lệ.")
        String email) {

    public EmailRequest {
        email = EmailAddress.normalize(email);
    }

    @Override
    public String toString() {
        return "EmailRequest[redacted]";
    }
}
