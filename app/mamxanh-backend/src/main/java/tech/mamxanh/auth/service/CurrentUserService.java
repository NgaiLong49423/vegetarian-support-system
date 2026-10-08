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

    public Map<Long, PublicProfile> getPublicProfiles(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return userRepository.findAllById(userIds).stream()
                .map(user -> new PublicProfile(user.getId(), user.getDisplayName(), user.getAvatarUrl()))
                .collect(Collectors.toMap(PublicProfile::id, Function.identity()));
    }

    private User requireActiveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        if (user.getAccountStatus() != AccountStatus.ACTIVE || !List.of(Role.CUSTOMER, Role.EXPERT).contains(user.getRole())) {
            throw new AppException(ErrorCode.RECIPE_EDIT_NOT_ALLOWED);
        }
        return user;
    }

    public record CurrentUser(long id, Role role) { }
    public record PublicProfile(long id, String displayName, String avatarUrl) { }
}
