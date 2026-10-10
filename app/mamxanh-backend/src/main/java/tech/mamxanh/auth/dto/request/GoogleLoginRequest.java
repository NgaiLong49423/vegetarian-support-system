package tech.mamxanh.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Google ID Token returned by Google Identity Services on the Frontend (UC-03.5). */
public record GoogleLoginRequest(
        @NotBlank(message = "Thiếu Google ID Token.")
        @Size(max = 4096, message = "Google ID Token không hợp lệ.")
        String idToken) {

    @Override
    public String toString() {
        return "GoogleLoginRequest[redacted]";
    }
}
