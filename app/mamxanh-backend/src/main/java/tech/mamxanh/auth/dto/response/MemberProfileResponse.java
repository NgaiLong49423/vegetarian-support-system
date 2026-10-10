package tech.mamxanh.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** FR-23 public profile (AC-23.2): no email, role, account status or private data. */
public record MemberProfileResponse(
        long userId,
        String displayName,
        @Schema(description = "Ảnh đại diện; null thì Frontend hiển thị ảnh mặc định (AC-23.3).")
        String avatarUrl,
        @Schema(description = "Giới thiệu ngắn, tối đa 500 ký tự; null khi để trống.")
        String bio,
        @Schema(description = "Tháng tham gia theo giờ Việt Nam, dạng yyyy-MM.", example = "2026-09")
        String joinedMonth) {
}
