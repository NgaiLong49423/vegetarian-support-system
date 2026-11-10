package tech.mamxanh.recipe.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.service.AiAssistantExecutionJournal.ActionState;
import tech.mamxanh.recipe.service.AiAssistantExecutionJournal.JournalEntry;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiAssistantExecutionJournalTest {

    private AiAssistantExecutionJournal journal;

    @BeforeEach
    void setUp() {
        journal = new AiAssistantExecutionJournal(null, null);
    }

    @Test
    void persistentLookupFailureDoesNotPretendRequestHasNoActions() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var persistent = new AiAssistantExecutionJournal(jdbc, null);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("disconnected"))
                .when(jdbc).query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any());
        assertThatThrownBy(() -> persistent.findByRequest(42L, UUID.randomUUID()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Không thể xác minh");
    }

    @Test
    void indeterminateActionIsNeverReplayed() {
        UUID requestId = UUID.randomUUID();
        journal.recordReservation(42L, requestId, "action", "create_recipe_post");
        journal.updateState(42L, requestId, "action", ActionState.INDETERMINATE, null, null);
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> journal.executeWithJournal(42L, requestId, "action", "create_recipe_post", () -> {
            calls.incrementAndGet();
            return "created";
        })).isInstanceOf(AppException.class);
        assertThat(calls).hasValue(0);
    }

    @Test
    void failedPersistentStateUpdateDoesNotReportSuccess() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var persistent = new AiAssistantExecutionJournal(jdbc, null);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("disconnected"))
                .when(jdbc).update(contains("UPDATE AI_ASSISTANT_ACTION_JOURNAL"), any(), any(), any(), any(), any(), any());
        assertThatThrownBy(() -> persistent.updateState(42L, UUID.randomUUID(), "action", ActionState.SUCCEEDED, null, null))
                .isInstanceOf(AppException.class);
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
    void committedResultIsReturnedWhenTheActionResponseIsLost() {
        long ownerId = 42L;
        UUID clientRequestId = UUID.randomUUID();
        String actionId = "create_recipe_post_1";
        AtomicInteger invocationCount = new AtomicInteger();
        AiAssistantToolRegistry.ToolResult committed = new AiAssistantToolRegistry.ToolResult(
                "SUCCEEDED", "Recipe published", Map.of("recipeId", 901L), List.of());

        var recovered = journal.executeWithJournal(ownerId, clientRequestId, actionId, "create_recipe_post", () -> {
            invocationCount.incrementAndGet();
            // Simulate domain data and the journal success row committing before the HTTP/SSE response is lost.
            journal.updateState(ownerId, clientRequestId, actionId, ActionState.SUCCEEDED, committed, null);
            throw new IllegalStateException("client disconnected after commit");
        });

        assertThat(recovered).isEqualTo(committed);
        assertThat(invocationCount).hasValue(1);
        var replayed = journal.executeWithJournal(ownerId, clientRequestId, actionId, "create_recipe_post", () -> {
            invocationCount.incrementAndGet();
            return new AiAssistantToolRegistry.ToolResult("SUCCEEDED", "duplicate", Map.of("recipeId", 902L), List.of());
        });
        assertThat(replayed).isEqualTo(committed);
        assertThat(invocationCount).hasValue(1);
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

    @Test
    void duplicateInFlightReservationThrowsValidationFailed() {
        long ownerId = 42L;
        UUID clientRequestId = UUID.randomUUID();
        String actionId = "delete_shopping_list_1";

        // First reservation succeeds
        journal.recordReservation(ownerId, clientRequestId, actionId, "delete_shopping_list");

        // Concurrent duplicate reservation with the same key throws validation failed
        assertThatThrownBy(() -> journal.recordReservation(ownerId, clientRequestId, actionId, "delete_shopping_list"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Thao tác đang được xử lý trong một yêu cầu khác");
    }

    @Test
    void mismatchedArgumentsHashThrowsValidationFailed() {
        long ownerId = 42L;
        UUID clientRequestId = UUID.randomUUID();
        String actionId = "apply_weekly_menu_1";

        journal.executeWithJournal(ownerId, clientRequestId, actionId, "apply_weekly_menu", "hash_params_version_1",
                () -> "Applied menu v1");

        // Attempting replay with SAME key but DIFFERENT argumentsHash must be rejected
        assertThatThrownBy(() -> journal.executeWithJournal(ownerId, clientRequestId, actionId, "apply_weekly_menu", "hash_params_version_2",
                () -> "Applied menu v2"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Tham số của yêu cầu không khớp với bản ghi đã tồn tại");
    }

    @Test
    void concurrentThreadsExecutingSameActionOnlyExecuteOnce() throws Exception {
        long ownerId = 42L;
        UUID clientRequestId = UUID.randomUUID();
        String actionId = "delete_shopping_item_1";
        AtomicInteger invocationCount = new AtomicInteger(0);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> f1 = executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                return journal.executeWithJournal(ownerId, clientRequestId, actionId, "delete_shopping_item", () -> {
                    invocationCount.incrementAndGet();
                    Thread.sleep(100);
                    return "Deleted item";
                });
            });

            Future<String> f2 = executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                return journal.executeWithJournal(ownerId, clientRequestId, actionId, "delete_shopping_item", () -> {
                    invocationCount.incrementAndGet();
                    Thread.sleep(100);
                    return "Deleted item";
                });
            });

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();

            int successCount = 0;
            int conflictCount = 0;

            for (Future<String> f : List.of(f1, f2)) {
                try {
                    String res = f.get(5, TimeUnit.SECONDS);
                    if ("Deleted item".equals(res)) {
                        successCount++;
                    }
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof AppException appEx && appEx.errorCode() == ErrorCode.VALIDATION_FAILED) {
                        conflictCount++;
                    } else {
                        throw ex;
                    }
                }
            }

            // Exactly ONE thread executed the action
            assertThat(invocationCount.get()).isEqualTo(1);
            assertThat(successCount).isEqualTo(1);
            assertThat(conflictCount).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void sqlDuplicateKeyHandlingRejectsConcurrentInFlightReservation() {
        JdbcTemplate mockJdbc = mock(JdbcTemplate.class);
        PlatformTransactionManager mockTxManager = mock(PlatformTransactionManager.class);
        AiAssistantExecutionJournal dbJournal = new AiAssistantExecutionJournal(mockJdbc, mockTxManager);

        long ownerId = 100L;
        UUID clientRequestId = UUID.randomUUID();
        String actionId = "delete_shopping_list_1";
        String toolName = "delete_shopping_list";

        // Mock insert throwing DuplicateKeyException
        doThrow(new DuplicateKeyException("Primary key violation"))
                .when(mockJdbc).update(contains("INSERT INTO AI_ASSISTANT_ACTION_JOURNAL"), any(), any(), any(), any(), any());

        // Mock select query finding existing RESERVED row
        when(mockJdbc.queryForList(contains("SELECT arguments_hash"), eq(ownerId), eq(clientRequestId), eq(actionId)))
                .thenReturn(List.of(Map.of(
                        "arguments_hash", "some_hash",
                        "state", "RESERVED"
                )));

        assertThatThrownBy(() -> dbJournal.recordReservation(ownerId, clientRequestId, actionId, toolName, "some_hash"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Thao tác đang được xử lý trong một yêu cầu khác");
    }
}
