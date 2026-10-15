package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.entity.*;
import tech.mamxanh.recipe.repository.*;

class RecipeReactionServiceTest {
    private final RecipePostRepository recipes = mock(RecipePostRepository.class);
    private final RecipeReactionRepository reactions = mock(RecipeReactionRepository.class);
    private final RecipeStatisticsRepository statistics = mock(RecipeStatisticsRepository.class);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-09T00:00:00Z"), ZoneOffset.UTC);
    private final RecipeReactionService service = new RecipeReactionService(recipes, reactions, statistics, users, clock);
    private final RecipeReactionId id = new RecipeReactionId(2L, 10L);
    private RecipePostEntity recipe;

    @BeforeEach
    void prepare() {
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(2L, Role.CUSTOMER));
        recipe = new RecipePostEntity();
        recipe.setId(10L);
        recipe.setAuthorId(1L);
        recipe.setStatus("PUBLISHED");
        when(recipes.lockById(10L)).thenReturn(Optional.of(recipe));
        when(recipes.findByIdAndStatus(10L, "PUBLISHED")).thenReturn(Optional.of(recipe));
        when(reactions.findById(id)).thenReturn(Optional.empty());
    }

    private void stats(long likes, long dislikes) {
        var projection = mock(RecipeStatisticsRepository.StatisticsProjection.class);
        when(projection.getLikes()).thenReturn(likes);
        when(projection.getDislikes()).thenReturn(dislikes);
        when(statistics.findStatistics(10L)).thenReturn(Optional.of(projection));
    }

    @Test
    void createsLikeForAuthenticatedMemberAndReturnsRoundedStatistics() {
        stats(10, 1);
        var response = service.set(10L, RecipeReactionType.LIKE);
        assertThat(response.currentReaction()).isEqualTo(RecipeReactionType.LIKE);
        assertThat(response.likePercentage()).isEqualByComparingTo("91");
        assertThat(response.reactionCount()).isEqualTo(11);
        var saved = ArgumentCaptor.forClass(RecipeReactionEntity.class);
        var order = inOrder(recipes, reactions, statistics);
        order.verify(recipes).lockById(10L);
        order.verify(reactions).findById(id);
        order.verify(reactions).save(saved.capture());
        order.verify(reactions).flush();
        order.verify(statistics).findStatistics(10L);
        assertThat(saved.getValue().getId()).isEqualTo(id);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @ParameterizedTest
    @CsvSource({"LIKE,DISLIKE", "DISLIKE,LIKE", "LIKE,LIKE", "DISLIKE,DISLIKE"})
    void updatesExistingReactionWithoutAddingRowsAndPreservesTimestampOnRetry(RecipeReactionType before, RecipeReactionType after) {
        var created = LocalDateTime.now(clock).minusDays(1);
        var existing = new RecipeReactionEntity(id, before, created);
        when(reactions.findById(id)).thenReturn(Optional.of(existing));
        stats(after == RecipeReactionType.LIKE ? 1 : 0, after == RecipeReactionType.DISLIKE ? 1 : 0);
        assertThat(service.set(10L, after).currentReaction()).isEqualTo(after);
        assertThat(existing.getReactionType()).isEqualTo(after);
        assertThat(existing.getCreatedAt()).isEqualTo(created);
        assertThat(existing.getUpdatedAt()).isEqualTo(before == after ? created : LocalDateTime.now(clock));
        verify(reactions, never()).save(any());
    }

    @Test
    void removesReactionAndReturnsNoDataWhenLastVoteIsRemoved() {
        var existing = new RecipeReactionEntity(id, RecipeReactionType.LIKE, LocalDateTime.now(clock));
        when(reactions.findById(id)).thenReturn(Optional.of(existing));
        stats(0, 0);
        var response = service.remove(10L);
        assertThat(response.currentReaction()).isNull();
        assertThat(response.likePercentage()).isNull();
        verify(reactions).delete(existing);
        verify(reactions).flush();
    }

    @Test
    void repeatedRemovalIsHarmlessAndZeroPercentIsDifferentFromNoVotes() {
        stats(0, 5);
        var response = service.remove(10L);
        assertThat(response.likePercentage()).isEqualByComparingTo("0");
        assertThat(response.reactionCount()).isEqualTo(5);
        verify(reactions, never()).delete(any());
    }

    @Test
    void readsOnlyCurrentMembersReactionWithoutMutationOrAuthorRestriction() {
        recipe.setAuthorId(2L);
        when(reactions.findById(id)).thenReturn(Optional.of(new RecipeReactionEntity(id, RecipeReactionType.DISLIKE, LocalDateTime.now(clock))));
        stats(0, 1);
        assertThat(service.get(10L).currentReaction()).isEqualTo(RecipeReactionType.DISLIKE);
        verify(recipes, never()).lockById(anyLong());
        verify(reactions, never()).flush();
    }

    @Test
    void blocksAuthorSetAndRemovalWithoutTouchingReactions() {
        recipe.setAuthorId(2L);
        assertError(() -> service.set(10L, RecipeReactionType.LIKE), ErrorCode.RECIPE_SELF_REACTION_NOT_ALLOWED);
        assertError(() -> service.remove(10L), ErrorCode.RECIPE_SELF_REACTION_NOT_ALLOWED);
        verifyNoInteractions(reactions, statistics);
    }

    @ParameterizedTest
    @CsvSource({"HIDDEN", "DELETED"})
    void blocksUnavailableRecipeMutation(String status) {
        recipe.setStatus(status);
        assertError(() -> service.set(10L, RecipeReactionType.DISLIKE), ErrorCode.RECIPE_NOT_FOUND);
        assertError(() -> service.remove(10L), ErrorCode.RECIPE_NOT_FOUND);
        verifyNoInteractions(reactions, statistics);
    }

    @Test
    void rejectsMissingRecipeAndInvalidTypeBeforeWriting() {
        when(recipes.lockById(10L)).thenReturn(Optional.empty());
        when(recipes.findByIdAndStatus(10L, "PUBLISHED")).thenReturn(Optional.empty());
        assertError(() -> service.set(10L, RecipeReactionType.LIKE), ErrorCode.RECIPE_NOT_FOUND);
        assertError(() -> service.remove(10L), ErrorCode.RECIPE_NOT_FOUND);
        assertError(() -> service.get(10L), ErrorCode.RECIPE_NOT_FOUND);
        assertError(() -> service.set(10L, null), ErrorCode.VALIDATION_FAILED);
        verifyNoInteractions(reactions, statistics);
    }

    @Test
    void propagatesAuthenticationFailureBeforeReadingRecipe() {
        when(users.requireActiveMember()).thenThrow(new AppException(ErrorCode.UNAUTHENTICATED));
        assertError(() -> service.set(10L, RecipeReactionType.LIKE), ErrorCode.UNAUTHENTICATED);
        assertError(() -> service.remove(10L), ErrorCode.UNAUTHENTICATED);
        assertError(() -> service.get(10L), ErrorCode.UNAUTHENTICATED);
        verifyNoInteractions(recipes, reactions, statistics);
    }

    private void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(AppException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(code));
    }
}
