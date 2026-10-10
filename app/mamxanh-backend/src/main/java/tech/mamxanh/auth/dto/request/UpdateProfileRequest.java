package tech.mamxanh.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code PUT /api/v1/me/profile} (AC-23.4, AC-23.6, Q58): the same name rule as registration. */
public record UpdateProfileRequest(
        @NotNull(message = "Vui lòng nhập tên hiển thị.")
        @Size(min = 3, max = 50, message = "Tên hiển thị cần từ 3 đến 50 ký tự.")
        @Pattern(regexp = "[^\\p{Cntrl}]*", message = "Tên hiển thị không được chứa ký tự điều khiển.")
        String displayName,

        @Size(max = 500, message = "Giới thiệu ngắn tối đa 500 ký tự.")
        String bio) {

    public UpdateProfileRequest {
        displayName = displayName == null ? null : displayName.strip();
        bio = bio == null || bio.isBlank() ? null : bio.strip();
    }
}
