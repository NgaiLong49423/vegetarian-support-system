package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentCategory;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentPurpose;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentScope;

class AiAssistantSessionStoreTest {
    @Test
    void consentIsScopedAndCanBeRevoked() {
        var store = new AiAssistantSessionStore(Clock.systemUTC(), Duration.ofMinutes(30));
        var session = store.open(null, "user:7");
        var scope = new ConsentScope(ConsentCategory.HEALTH_PROFILE, ConsentPurpose.AI_CHAT);
        assertThat(store.applyConsent(session.id(), "user:7", true, Set.of(scope)).consentScopes()).containsExactly(scope);
        assertThat(store.applyConsent(session.id(), "user:7", false, Set.of(scope)).consentScopes()).isEmpty();
    }

    @Test
    void confirmationIsOwnerBoundAndSingleUse() {
        var store = new AiAssistantSessionStore(Clock.systemUTC(), Duration.ofMinutes(30));
        var session = store.open(null, "user:7");
        var plan = store.createPending(session.id(), "user:7",
                List.of(new AiAssistantSessionStore.PendingAction("delete_shopping_list", Map.of("listId", 9), true)), true);
        assertThatThrownBy(() -> store.takePending(session.id(), "user:8", plan.id())).isInstanceOf(AppException.class);
        assertThat(store.takePending(session.id(), "user:7", plan.id()).actions()).hasSize(1);
        assertThat(store.takePending(session.id(), "user:7", plan.id())).isNull();
    }

    @Test
    void expiredConfirmationCannotBeConsumed() {
        var clock = new MutableClock(Instant.parse("2026-10-31T00:00:00Z"));
        var store = new AiAssistantSessionStore(clock, Duration.ofMinutes(30));
        var session = store.open(null, "guest:viewer");
        var plan = store.createPending(session.id(), "guest:viewer", List.of(), false);
        clock.advance(Duration.ofMinutes(6));
        assertThat(store.takePending(session.id(), "guest:viewer", plan.id())).isNull();
    }

    @Test
    void deleteRemovesOnlyTheOwningTemporarySessionAndIsIdempotent() {
        var store = new AiAssistantSessionStore(Clock.systemUTC(), Duration.ofMinutes(30));
        var session = store.open(null, "user:7");
        store.applyConsent(session.id(), "user:7", true,
                Set.of(new ConsentScope(ConsentCategory.BASIC_PROFILE, ConsentPurpose.AI_CHAT)));

        assertThatThrownBy(() -> store.delete(session.id(), "user:8")).isInstanceOf(AppException.class);
        assertThat(store.open(session.id(), "user:7").consentScopes()).hasSize(1);
        store.delete(session.id(), "user:7");
        store.delete(session.id(), "user:7");
        assertThatThrownBy(() -> store.applyConsent(session.id(), "user:7", true, Set.of()))
                .isInstanceOf(AppException.class);
    }

    @Test
    void unknownOrExpiredSessionIdIsRejectedAndEngineRemainsPinned() {
        var clock = new MutableClock(Instant.parse("2026-10-31T00:00:00Z"));
        var store = new AiAssistantSessionStore(clock, Duration.ofMinutes(30));
        assertThatThrownBy(() -> store.open(java.util.UUID.randomUUID(), "user:7", AiAssistantSessionStore.ENGINE_G3_PILOT))
                .isInstanceOf(AppException.class);

        var session = store.open(null, "user:7", AiAssistantSessionStore.ENGINE_G3_PILOT);
        assertThat(session.engine()).isEqualTo(AiAssistantSessionStore.ENGINE_G3_PILOT);
        assertThat(store.peekEngine(session.id(), "user:7")).contains(AiAssistantSessionStore.ENGINE_G3_PILOT);
        assertThatThrownBy(() -> store.open(session.id(), "user:7", AiAssistantSessionStore.ENGINE_LEGACY))
                .isInstanceOf(AppException.class);

        clock.advance(Duration.ofMinutes(31));
        assertThatThrownBy(() -> store.open(session.id(), "user:7", AiAssistantSessionStore.ENGINE_G3_PILOT))
                .isInstanceOf(AppException.class);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        private void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
