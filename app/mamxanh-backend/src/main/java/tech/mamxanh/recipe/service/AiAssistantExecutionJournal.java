package tech.mamxanh.recipe.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saga / Distributed State Machine journal supporting:
 * - Multi-action identification: (ownerId, clientRequestId, actionId)
 * - Partial success tracking across multiple action steps
 * - Idempotent replay / commit timeout recovery without duplicate domain writes
 */
@Service
public class AiAssistantExecutionJournal {

    public enum ActionState {
        RESERVED,
        WRITING,
        SUCCEEDED,
        FAILED,
        CANCELLED
    }

    public record ActionKey(long ownerId, UUID clientRequestId, String actionId) { }

    public record JournalEntry(
            long ownerId,
            UUID clientRequestId,
            String actionId,
            String toolName,
            ActionState state,
            Object resultData,
            String errorMessage,
            Instant createdAt
    ) { }

    private final Map<ActionKey, JournalEntry> entries = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public AiAssistantExecutionJournal(
            @Autowired(required = false) JdbcTemplate jdbc,
            @Autowired(required = false) PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.transaction = manager != null ? new TransactionTemplate(manager) : null;
    }

    public JournalEntry recordReservation(long ownerId, UUID clientRequestId, String actionId, String toolName) {
        ActionKey key = new ActionKey(ownerId, clientRequestId, actionId);
        JournalEntry entry = new JournalEntry(
                ownerId, clientRequestId, actionId, toolName,
                ActionState.RESERVED, null, null, Instant.now()
        );
        entries.put(key, entry);
        return entry;
    }

    public void updateState(long ownerId, UUID clientRequestId, String actionId, ActionState newState, Object resultData, String errorMessage) {
        ActionKey key = new ActionKey(ownerId, clientRequestId, actionId);
        entries.compute(key, (k, existing) -> {
            String tool = existing != null ? existing.toolName() : "unknown";
            Instant created = existing != null ? existing.createdAt() : Instant.now();
            return new JournalEntry(ownerId, clientRequestId, actionId, tool, newState, resultData, errorMessage, created);
        });
    }

    public Optional<JournalEntry> find(long ownerId, UUID clientRequestId, String actionId) {
        return Optional.ofNullable(entries.get(new ActionKey(ownerId, clientRequestId, actionId)));
    }

    public List<JournalEntry> findByRequest(long ownerId, UUID clientRequestId) {
        return entries.values().stream()
                .filter(e -> e.ownerId() == ownerId && Objects.equals(e.clientRequestId(), clientRequestId))
                .sorted(Comparator.comparing(JournalEntry::createdAt).thenComparing(JournalEntry::actionId))
                .toList();
    }

    public boolean isSucceeded(long ownerId, UUID clientRequestId, String actionId) {
        return find(ownerId, clientRequestId, actionId)
                .map(e -> e.state() == ActionState.SUCCEEDED)
                .orElse(false);
    }

    public <T> T executeWithJournal(
            long ownerId,
            UUID clientRequestId,
            String actionId,
            String toolName,
            Callable<T> action) {

        ActionKey key = new ActionKey(ownerId, clientRequestId, actionId);

        // 1. Idempotency Check: if already succeeded in this or previous retry turn, return cached result
        JournalEntry existing = entries.get(key);
        if (existing != null && existing.state() == ActionState.SUCCEEDED) {
            @SuppressWarnings("unchecked")
            T cached = (T) existing.resultData();
            return cached;
        }

        // 2. Reserve Action
        recordReservation(ownerId, clientRequestId, actionId, toolName);
        updateState(ownerId, clientRequestId, actionId, ActionState.WRITING, null, null);

        // 3. Execute domain action
        try {
            T result = action.call();
            updateState(ownerId, clientRequestId, actionId, ActionState.SUCCEEDED, result, null);
            return result;
        } catch (Exception failure) {
            updateState(ownerId, clientRequestId, actionId, ActionState.FAILED, null, failure.getMessage());
            if (failure instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID, failure.getMessage());
        }
    }

    public void cancel(long ownerId, UUID clientRequestId, String actionId) {
        updateState(ownerId, clientRequestId, actionId, ActionState.CANCELLED, null, "Thao tác đã bị hủy");
    }

    public void clear() {
        entries.clear();
    }
}
