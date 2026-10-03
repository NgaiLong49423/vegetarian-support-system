package tech.mamxanh.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Single-use token taken from an email link; described by generated runtime OpenAPI. */
public record TokenRequest(
        @NotBlank(message = "Thiếu mã xác minh.")
        @Size(max = 128, message = "Mã xác minh không hợp lệ.")
        String token) {

    @Override
    public String toString() {
        return "TokenRequest[redacted]";
    }
}
