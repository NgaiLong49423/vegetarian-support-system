package tech.mamxanh.auth.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.mamxanh.auth.dto.response.FollowMemberResponse;
import tech.mamxanh.auth.dto.response.FollowStatusResponse;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.entity.UserFollowId;
import tech.mamxanh.auth.repository.UserFollowRepository;
import tech.mamxanh.auth.repository.UserFollowRepository.FollowMemberView;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.common.response.PageResponse;

/**
 * FR-59 member following (BR-75). The follower always comes from the session; only an ACTIVE
 * Customer or Expert may follow or unfollow. A target is any Member with a public profile (Q55),
 * but a LOCKED Member receives no new follower (AC-59.7).
 */
@Service
public class MemberFollowService {
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;

    public MemberFollowService(CurrentUserService currentUserService, UserRepository userRepository,
            UserFollowRepository followRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    @Transactional(readOnly = true)
    public FollowStatusResponse getStatus(long userId) {
        currentUserService.requirePublicMember(userId);
        boolean following = currentUserService.findAuthenticatedUserId()
                .map(viewerId -> followRepository.existsById(new UserFollowId(viewerId, userId)))
                .orElse(false);
        return status(userId, following);
    }

    @Transactional
    public FollowStatusResponse follow(long userId) {
        long followerId = requireActiveFollower(userId);
        currentUserService.requirePublicMember(userId);
        userRepository.findById(userId)
                .filter(target -> target.getAccountStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.FOLLOW_TARGET_INACTIVE));
        followRepository.insertIfAbsent(followerId, userId);
        return status(userId, true);
    }

    @Transactional
    public FollowStatusResponse unfollow(long userId) {
        long followerId = requireActiveFollower(userId);
        currentUserService.requirePublicMember(userId);
        followRepository.deleteFollow(followerId, userId);
        return status(userId, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowMemberResponse> followers(long userId, int page, int size) {
        currentUserService.requirePublicMember(userId);
        return page(followRepository.findActiveFollowers(userId, PageRequest.of(page, size)), page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowMemberResponse> following(long userId, int page, int size) {
        currentUserService.requirePublicMember(userId);
        return page(followRepository.findActiveFollowing(userId, PageRequest.of(page, size)), page, size);
    }

    /** BR-75: an ACTIVE Customer or Expert acting for itself, never on its own profile (AC-59.3). */
    private long requireActiveFollower(long targetUserId) {
        long followerId = currentUserService.requireAuthenticatedUserId();
        userRepository.findById(followerId)
                .filter(user -> user.getAccountStatus() == AccountStatus.ACTIVE && user.getRole() != Role.ADMIN)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_ACCESS_REQUIRED,
                        "Chỉ Member có tài khoản hoạt động mới theo dõi được thành viên khác."));
        if (followerId == targetUserId) {
            throw new AppException(ErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }
        return followerId;
    }

    private FollowStatusResponse status(long userId, boolean following) {
        return new FollowStatusResponse(userId, following, followRepository.countActiveFollowers(userId),
                followRepository.countActiveFollowing(userId));
    }

    private static PageResponse<FollowMemberResponse> page(Page<FollowMemberView> result, int page, int size) {
        return PageResponse.of(result.getContent().stream()
                .map(view -> new FollowMemberResponse(view.getUserId(), view.getDisplayName(), view.getAvatarUrl()))
                .toList(), page, size, result.getTotalElements());
    }
}
