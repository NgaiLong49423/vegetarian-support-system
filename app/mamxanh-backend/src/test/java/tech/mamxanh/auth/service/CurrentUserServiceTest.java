package tech.mamxanh.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {
    @Mock private UserRepository userRepository;
    @InjectMocks private CurrentUserService service;

    @Test
    void bulkPublicProfileLookupReturnsOnlyPublicNameAndAvatar() {
        User user = User.registerWithPassword("private@example.test", "unused", "Public name",
                LocalDateTime.of(2026, 10, 8, 0, 0));
        ReflectionTestUtils.setField(user, "id", 7L);
        ReflectionTestUtils.setField(user, "avatarUrl", "https://img.test/avatar.png");
        when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(user));

        var profiles = service.getPublicProfiles(List.of(7L));

        assertThat(profiles).containsOnlyKeys(7L);
        assertThat(profiles.get(7L)).isEqualTo(new CurrentUserService.PublicProfile(
                7L, "Public name", "https://img.test/avatar.png"));
        verify(userRepository).findAllById(List.of(7L));
    }

    @Test
    void emptyBulkPublicProfileLookupAvoidsDatabaseAccess() {
        assertThat(service.getPublicProfiles(List.of())).isEmpty();
        assertThat(service.getPublicProfiles(null)).isEmpty();
        verify(userRepository, never()).findAllById(org.mockito.ArgumentMatchers.anyIterable());
    }

    @Test
    void optionalViewerIsEmptyForAGuestOnAPublicEndpoint() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(service.findAuthenticatedUserId()).isEmpty();

        SecurityContextHolder.clearContext();
        assertThat(service.findAuthenticatedUserId()).isEmpty();
    }

    @Test
    void optionalViewerIsTheSessionAccountWhenATokenIsPresent() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "42", null, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER")));

        assertThat(service.findAuthenticatedUserId()).contains(42L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
