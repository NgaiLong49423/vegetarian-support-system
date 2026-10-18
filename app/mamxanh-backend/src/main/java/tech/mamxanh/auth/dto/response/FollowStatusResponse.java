package tech.mamxanh.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** FR-59 follow state of one Member profile (AC-59.1, AC-59.5). */
public record FollowStatusResponse(
        @Schema(description = "Member có hồ sơ đang xem.")
        long userId,
        @Schema(description = "Người xem đang theo dõi Member này; luôn false với Guest.")
        boolean following,
        @Schema(description = "Số người theo dõi Member này, chỉ tính tài khoản đang hoạt động.")
        long followerCount,
        @Schema(description = "Số tài khoản Member này đang theo dõi, chỉ tính tài khoản đang hoạt động.")
        long followingCount) {
}
