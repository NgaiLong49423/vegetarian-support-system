package tech.mamxanh.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import tech.mamxanh.auth.entity.MemberProfileEntity;
import tech.mamxanh.auth.repository.MemberProfileRepository;
import tech.mamxanh.auth.repository.UserFollowRepository;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.auth.service.ExpertDirectoryService;

@ExtendWith(MockitoExtension.class)
class ExpertDirectoryServiceTest {
    @Mock private MemberProfileRepository profileRepository;
    @Mock private UserFollowRepository followRepository;
    @Mock private CurrentUserService currentUserService;

    private ExpertDirectoryService service;

    @BeforeEach void setUp() {
        service = new ExpertDirectoryService(profileRepository, followRepository, currentUserService);
    }

    @Test void returnsOnlyPublicProfileFieldsAndActiveFollowerCountsForGuests() {
        MemberProfileEntity expert = org.mockito.Mockito.mock(MemberProfileEntity.class);
        when(expert.getId()).thenReturn(51L);
        when(expert.getDisplayName()).thenReturn("Bếp Xanh");
        when(expert.getAvatarUrl()).thenReturn("https://images.example.test/expert.webp");
        when(expert.getBio()).thenReturn("Món chay theo mùa");
        when(profileRepository.searchActiveExperts(eq("bếp"), any())).thenReturn(
                new PageImpl<>(List.of(expert), PageRequest.of(0, 12), 1));
        UserFollowRepository.ActiveFollowerCount count = org.mockito.Mockito.mock(UserFollowRepository.ActiveFollowerCount.class);
        when(count.getUserId()).thenReturn(51L);
        when(count.getFollowerCount()).thenReturn(7L);
        when(followRepository.findActiveFollowerCounts(List.of(51L))).thenReturn(List.of(count));
        when(currentUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());

        var response = service.search(" bếp ", 0, 12);

        assertThat(response.content()).singleElement().satisfies(item -> {
            assertThat(item.userId()).isEqualTo(51L);
            assertThat(item.displayName()).isEqualTo("Bếp Xanh");
            assertThat(item.bio()).isEqualTo("Món chay theo mùa");
            assertThat(item.followerCount()).isEqualTo(7L);
            assertThat(item.following()).isFalse();
        });
        verify(profileRepository).searchActiveExperts(eq("bếp"), any());
    }

    @Test void returnsFollowingStateForTheAuthenticatedViewer() {
        MemberProfileEntity expert = org.mockito.Mockito.mock(MemberProfileEntity.class);
        when(expert.getId()).thenReturn(51L);
        when(expert.getDisplayName()).thenReturn("Bếp Xanh");
        when(expert.getAvatarUrl()).thenReturn(null);
        when(expert.getBio()).thenReturn(null);
        when(profileRepository.searchActiveExperts(eq(null), any())).thenReturn(
                new PageImpl<>(List.of(expert), PageRequest.of(0, 12), 1));
        when(followRepository.findActiveFollowerCounts(List.of(51L))).thenReturn(List.of());
        when(currentUserService.findAuthenticatedUserId()).thenReturn(Optional.of(20L));
        when(followRepository.findFollowedIds(20L, List.of(51L))).thenReturn(List.of(51L));

        var response = service.search(" ", 0, 12);

        assertThat(response.content()).singleElement().satisfies(item -> {
            assertThat(item.following()).isTrue();
            assertThat(item.followerCount()).isZero();
        });
    }
}
