package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.MutableClock;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.repository.RecipeViewRepository;

@ExtendWith(MockitoExtension.class)
class RecipeViewServiceTest {
    private static final String GUEST = "abcdefab-1234-4567-89ab-123456789abc";
    @Mock RecipeViewRepository repository;
    private MutableClock clock;
    private RecipeViewService service;

    @BeforeEach void setup() {
        clock = new MutableClock();
        service = new RecipeViewService(repository, clock);
    }

    @Test void firstGuestViewUsesHashAndServerTime() {
        when(repository.publishedCount(1)).thenReturn(Optional.of(4L));
        when(repository.incrementPublishedCount(1)).thenReturn(Optional.of(5L));
        var result = service.record(1, null, GUEST);
        assertThat(result.counted()).isTrue();
        assertThat(result.viewCount()).isEqualTo(5);
        verify(repository).insert(eq(1L), isNull(), argThat(hash -> hash.matches("[a-f0-9]{64}")),
                eq(LocalDateTime.now(clock)));
    }

    @Test void memberUsesVerifiedUserAndDoesNotPersistGuestIdentity() {
        when(repository.publishedCount(1)).thenReturn(Optional.of(0L));
        when(repository.incrementPublishedCount(1)).thenReturn(Optional.of(1L));
        service.record(1, 7L, GUEST);
        verify(repository).lockViewer("recipe-view:1:user:7");
        verify(repository).insert(1, 7L, null, LocalDateTime.now(clock));
    }

    @ParameterizedTest @ValueSource(ints = {900, 1800, 1801, 2100})
    void windowUsesLastCountedEventAndIncludesExactlyThirtyMinutes(int seconds) {
        var first = LocalDateTime.now(clock);
        clock.advance(Duration.ofSeconds(seconds));
        when(repository.publishedCount(1)).thenReturn(Optional.of(1L));
        when(repository.latest(1, 7L, null)).thenReturn(Optional.of(first));
        if (seconds > 1800) when(repository.incrementPublishedCount(1)).thenReturn(Optional.of(2L));
        var result = service.record(1, 7L, null);
        assertThat(result.counted()).isEqualTo(seconds > 1800);
        assertThat(result.viewCount()).isEqualTo(seconds > 1800 ? 2 : 1);
        if (seconds <= 1800) {
            verify(repository, never()).insert(anyLong(), any(), any(), any());
            verify(repository, never()).incrementPublishedCount(anyLong());
        }
    }

    @Test void uuidCaseDoesNotChangeAnonymousIdentity() {
        when(repository.publishedCount(1)).thenReturn(Optional.of(1L));
        when(repository.latest(eq(1L), isNull(), anyString())).thenReturn(Optional.of(LocalDateTime.now(clock)));
        service.record(1, null, GUEST);
        service.record(1, null, GUEST.toUpperCase(java.util.Locale.ROOT));
        var keys = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository, times(2)).lockViewer(keys.capture());
        assertThat(keys.getAllValues().get(0)).isEqualTo(keys.getAllValues().get(1));
    }

    @Test void invalidIdsAndMissingGuestIdentityNeverReachDatabase() {
        for (String id : new String[]{null, "", "1-1-1-1-1", "not-a-uuid"}) {
            assertThatThrownBy(() -> service.record(1, null, id)).isInstanceOf(AppException.class);
        }
        assertThatThrownBy(() -> service.record(0, 7L, null)).isInstanceOf(AppException.class);
        verifyNoInteractions(repository);
    }

    @Test void lockFailureIsRetryableAndNeverWrites() {
        when(repository.publishedCount(1)).thenReturn(Optional.of(0L));
        when(repository.lockViewer(anyString())).thenReturn(-1);
        assertThatThrownBy(() -> service.record(1, 7L, null)).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.RECIPE_VIEW_UNAVAILABLE));
        verify(repository, never()).incrementPublishedCount(anyLong());
        verify(repository, never()).insert(anyLong(), any(), any(), any());
    }

    @Test void duplicateReturnsCounterRefreshedAfterWaitingForViewerLock() {
        when(repository.publishedCount(1)).thenReturn(Optional.of(0L), Optional.of(1L));
        when(repository.latest(1, 7L, null)).thenReturn(Optional.of(LocalDateTime.now(clock)));
        var result = service.record(1, 7L, null);
        assertThat(result.counted()).isFalse();
        assertThat(result.viewCount()).isEqualTo(1);
        verify(repository, never()).incrementPublishedCount(anyLong());
    }

    @Test void nonPublicRecipeDoesNotWriteEvents() {
        assertThatThrownBy(() -> service.record(1, 7L, null)).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.RECIPE_NOT_FOUND));
        verify(repository, never()).insert(anyLong(), any(), any(), any());
    }

    @Test void recipeHiddenBetweenReadAndWriteDoesNotInsert() {
        when(repository.publishedCount(1)).thenReturn(Optional.of(0L));
        assertThatThrownBy(() -> service.record(1, 7L, null)).isInstanceOf(AppException.class);
        verify(repository, never()).insert(anyLong(), any(), any(), any());
    }
}
