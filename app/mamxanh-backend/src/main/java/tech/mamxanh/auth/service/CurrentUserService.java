package tech.mamxanh.auth.service;

import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/** Public auth-module boundary for resolving the authenticated account from the security context. */
@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CurrentUser requireActiveExpert() {
        User user = requireActiveUser();
        if (user.getRole() != Role.EXPERT) {
            throw new AppException(ErrorCode.RECIPE_EDIT_NOT_ALLOWED);
        }
        return new CurrentUser(user.getId(), user.getRole());
    }

    public CurrentUser requireActiveMember() {
        User user = requireActiveUser();
        return new CurrentUser(user.getId(), user.getRole());
    }

    public PublicProfile getPublicProfile(long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        return new PublicProfile(user.getId(), user.getDisplayName(), user.getAvatarUrl());
    }

    /**
     * FR-23 (Q55): a public profile exists for every Member account, including a LOCKED one; an
     * Administrator account or an unknown id has none.
     */
    public void requirePublicMember(long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getRole() != Role.ADMIN)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_PROFILE_NOT_FOUND));
    }

    /** The account id carried by the authenticated session; never taken from the request. */
    public long requireAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    public Map<Long, PublicProfile> getPublicProfiles(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return userRepository.findAllById(userIds).stream()
                .map(user -> new PublicProfile(user.getId(), user.getDisplayName(), user.getAvatarUrl()))
                .collect(Collectors.toMap(PublicProfile::id, Function.identity()));
    }

    private User requireActiveUser() {
        long userId = requireAuthenticatedUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        if (user.getAccountStatus() != AccountStatus.ACTIVE || !List.of(Role.CUSTOMER, Role.EXPERT).contains(user.getRole())) {
            throw new AppException(ErrorCode.RECIPE_EDIT_NOT_ALLOWED);
        }
        return user;
    }

    public record CurrentUser(long id, Role role) { }
    public record PublicProfile(long id, String displayName, String avatarUrl) { }
}
