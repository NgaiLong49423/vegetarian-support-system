package tech.mamxanh.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** One entry of a followers/following list (AC-59.6): public profile fields only, no email or account data. */
public record FollowMemberResponse(
        long userId,
        String displayName,
        @Schema(description = "Ảnh đại diện; null thì Frontend hiển thị ảnh mặc định.")
        String avatarUrl) {
}
