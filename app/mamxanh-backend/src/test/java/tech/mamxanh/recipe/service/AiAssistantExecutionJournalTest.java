package tech.mamxanh.recipe.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.mamxanh.recipe.service.AiAssistantExecutionJournal.ActionState;
import tech.mamxanh.recipe.service.AiAssistantExecutionJournal.JournalEntry;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiAssistantExecutionJournalTest {

    private AiAssistantExecutionJournal journal;

    @BeforeEach
    void setUp() {
        journal = new AiAssistantExecutionJournal(null, null);
    }

    @Test
    void multiActionRecordingInSingleClientRequest() {
        long ownerId = 42L;
        UUID clientRequestId = UUID.randomUUID();

        String res1 = journal.executeWithJournal(ownerId, clientRequestId, "act_1", "search_public_recipes",
                () -> "Found 3 recipes");
        String res2 = journal.executeWithJournal(ownerId, clientRequestId, "act_2", "create_shopping_list",
                () -> "Created list #101");

        assertThat(res1).isEqualTo("Found 3 recipes");
        assertThat(res2).isEqualTo("Created list #101");

        List<JournalEntry> entries = journal.findByRequest(ownerId, clientRequestId);
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).actionId()).isEqualTo("act_1");
        assertThat(entries.get(0).state()).isEqualTo(ActionState.SUCCEEDED);
        assertThat(entries.get(1).actionId()).isEqualTo("act_2");
        assertThat(entries.get(1).state()).isEqualTo(ActionState.SUCCEEDED);
    }

    @Test
    void partialSuccessTrackingAcrossSteps() {
        long ownerId = 42L;
        UUID clientRequestId = UUID.randomUUID();

        // Step 1: succeeds
        journal.executeWithJournal(ownerId, clientRequestId, "act_1", "search_public_recipes",
                () -> "Found recipes");

        // Step 2: fails
        assertThatThrownBy(() -> journal.executeWithJournal(ownerId, clientRequestId, "act_2", "add_meal_plan_slot",
                () -> { throw new IllegalStateException("Duplicate meal plan slot"); }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate meal plan slot");

        List<JournalEntry> entries = journal.findByRequest(ownerId, clientRequestId);
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).state()).isEqualTo(ActionState.SUCCEEDED);
        assertThat(entries.get(1).state()).isEqualTo(ActionState.FAILED);
        assertThat(entries.get(1).errorMessage()).contains("Duplicate meal plan slot");
    }

    @Test
    void idempotentReplayReturnsCachedResultWithoutReInvokingAction() {
        long ownerId = 42L;
        UUID clientRequestId = UUID.randomUUID();
        String actionId = "create_shopping_list_1";
        AtomicInteger invocationCount = new AtomicInteger(0);

        // First attempt executes action
        String firstResult = journal.executeWithJournal(ownerId, clientRequestId, actionId, "create_shopping_list", () -> {
            invocationCount.incrementAndGet();
            return "ShoppingList#999";
        });
        assertThat(firstResult).isEqualTo("ShoppingList#999");
        assertThat(invocationCount.get()).isEqualTo(1);

        // Second attempt with exact same key returns cached result WITHOUT re-invoking action
        String replayResult = journal.executeWithJournal(ownerId, clientRequestId, actionId, "create_shopping_list", () -> {
            invocationCount.incrementAndGet();
            return "ShoppingList#WRONG";
        });
        assertThat(replayResult).isEqualTo("ShoppingList#999");
        assertThat(invocationCount.get()).isEqualTo(1); // Not incremented!
    }

    @Test
    void cancelTransitionsStateToCancelled() {
        long ownerId = 42L;
        UUID clientRequestId = UUID.randomUUID();
        String actionId = "pending_meal_plan_write";

        journal.recordReservation(ownerId, clientRequestId, actionId, "add_meal_plan_slot");
        journal.cancel(ownerId, clientRequestId, actionId);

        var entry = journal.find(ownerId, clientRequestId, actionId);
        assertThat(entry).isPresent();
        assertThat(entry.get().state()).isEqualTo(ActionState.CANCELLED);
        assertThat(entry.get().errorMessage()).contains("đã bị hủy");
    }
}
