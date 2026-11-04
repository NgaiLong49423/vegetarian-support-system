package tech.mamxanh.recipe.service;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saga / Multi-action State Machine journal supporting:
 * - Multi-action identification: (ownerId, clientRequestId, actionId)
 * - Atomic transaction binding domain write and SUCCEEDED journal record
 * - Partial success tracking across multiple action steps
 * - Idempotent replay / commit timeout recovery without duplicate domain writes
 * - Restart recovery marking stale in-flight records as INDETERMINATE
 */
@Service
public class AiAssistantExecutionJournal {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantExecutionJournal.class);

    public enum ActionState {
        RESERVED,
        WRITING,
        SUCCEEDED,
        FAILED,
        CANCELLED,
        INDETERMINATE
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
    private final TransactionTemplate requiresNewTransaction;
    private final TransactionTemplate sharedTransaction;
    private final ObjectMapper mapper;
    private final LocalDateTime bootCutoff;

    public AiAssistantExecutionJournal() {
        this(null, null, null);
    }

    @Autowired
    public AiAssistantExecutionJournal(
            @Autowired(required = false) JdbcTemplate jdbc,
            @Autowired(required = false) PlatformTransactionManager manager,
            @Autowired(required = false) ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper != null ? mapper : new ObjectMapper();
        if (manager != null) {
            this.requiresNewTransaction = new TransactionTemplate(manager);
            this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.requiresNewTransaction.setTimeout(10);

            this.sharedTransaction = new TransactionTemplate(manager);
            this.sharedTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            this.sharedTransaction.setTimeout(15);
        } else {
            this.requiresNewTransaction = null;
            this.sharedTransaction = null;
        }

        LocalDateTime cutoff = null;
        if (jdbc != null) {
            try {
                cutoff = jdbc.queryForObject("SELECT SYSUTCDATETIME()", LocalDateTime.class);
            } catch (Exception ex) {
                log.warn("Could not capture SQL Server boot cutoff time: {}", ex.getMessage());
            }
        }
        this.bootCutoff = cutoff;
    }

    public AiAssistantExecutionJournal(
            JdbcTemplate jdbc,
            PlatformTransactionManager manager) {
        this(jdbc, manager, new ObjectMapper());
    }

    public JournalEntry recordReservation(long ownerId, UUID clientRequestId, String actionId, String toolName) {
        ActionKey key = new ActionKey(ownerId, clientRequestId, actionId);
        JournalEntry entry = new JournalEntry(
                ownerId, clientRequestId, actionId, toolName,
                ActionState.RESERVED, null, null, Instant.now()
        );
        entries.put(key, entry);

        if (jdbc != null && requiresNewTransaction != null) {
            String hash = hash(actionId + ":" + toolName);
            try {
                requiresNewTransaction.executeWithoutResult(status -> jdbc.update("""
                        INSERT INTO AI_ASSISTANT_ACTION_JOURNAL(owner_id, client_request_id, action_id, tool_name, arguments_hash, state)
                        VALUES (?, ?, ?, ?, ?, 'RESERVED')
                        """, ownerId, clientRequestId, actionId, toolName, hash));
            } catch (DuplicateKeyException duplicate) {
                log.debug("Reservation duplicate key detected for owner={}, req={}, action={}", ownerId, clientRequestId, actionId);
            } catch (Exception ex) {
                log.warn("Failed to persist reservation in SQL Server: {}", ex.getMessage());
            }
        }
        return entry;
    }

    public void updateState(long ownerId, UUID clientRequestId, String actionId, ActionState newState, Object resultData, String errorMessage) {
        ActionKey key = new ActionKey(ownerId, clientRequestId, actionId);
        entries.compute(key, (k, existing) -> {
            String tool = existing != null ? existing.toolName() : "unknown";
            Instant created = existing != null ? existing.createdAt() : Instant.now();
            return new JournalEntry(ownerId, clientRequestId, actionId, tool, newState, resultData, errorMessage, created);
        });

        if (jdbc != null) {
            String json = null;
            if (resultData != null) {
                try {
                    json = mapper.writeValueAsString(resultData);
                } catch (Exception ignored) {
                    json = String.valueOf(resultData);
                }
            }
            final String finalJson = json;
            try {
                jdbc.update("""
                        UPDATE AI_ASSISTANT_ACTION_JOURNAL
                        SET state = ?, result_json = COALESCE(?, result_json), error_message = COALESCE(?, error_message)
                        WHERE owner_id = ? AND client_request_id = ? AND action_id = ?
                        """, newState.name(), finalJson, errorMessage, ownerId, clientRequestId, actionId);
            } catch (Exception ex) {
                log.warn("Failed to update SQL journal state for key {}: {}", key, ex.getMessage());
            }
        }
    }

    public Optional<JournalEntry> find(long ownerId, UUID clientRequestId, String actionId) {
        ActionKey key = new ActionKey(ownerId, clientRequestId, actionId);
        JournalEntry mem = entries.get(key);
        if (mem != null) {
            return Optional.of(mem);
        }

        if (jdbc != null) {
            try {
                List<JournalEntry> rows = jdbc.query("""
                        SELECT owner_id, client_request_id, action_id, tool_name, state, result_json, error_message, created_at
                        FROM AI_ASSISTANT_ACTION_JOURNAL
                        WHERE owner_id = ? AND client_request_id = ? AND action_id = ? AND expires_at > SYSUTCDATETIME()
                        """, (rs, rowNum) -> {
                    String stateStr = rs.getString("state");
                    ActionState state;
                    try {
                        state = ActionState.valueOf(stateStr);
                    } catch (Exception e) {
                        state = ActionState.INDETERMINATE;
                    }
                    String resultJson = rs.getString("result_json");
                    return new JournalEntry(
                            rs.getLong("owner_id"),
                            (UUID) rs.getObject("client_request_id"),
                            rs.getString("action_id"),
                            rs.getString("tool_name"),
                            state,
                            resultJson,
                            rs.getString("error_message"),
                            rs.getTimestamp("created_at").toInstant()
                    );
                }, ownerId, clientRequestId, actionId);

                if (!rows.isEmpty()) {
                    JournalEntry fromDb = rows.getFirst();
                    entries.put(key, fromDb);
                    return Optional.of(fromDb);
                }
            } catch (Exception ex) {
                log.debug("SQL find failed for key {}: {}", key, ex.getMessage());
            }
        }
        return Optional.empty();
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
        Optional<JournalEntry> existing = find(ownerId, clientRequestId, actionId);
        if (existing.isPresent() && existing.get().state() == ActionState.SUCCEEDED) {
            @SuppressWarnings("unchecked")
            T cached = (T) existing.get().resultData();
            return cached;
        }

        // 2. Reserve Action
        recordReservation(ownerId, clientRequestId, actionId, toolName);

        // 3. Execute domain action (wrapped in shared transaction if manager available)
        try {
            if (sharedTransaction != null) {
                return sharedTransaction.execute(status -> {
                    updateState(ownerId, clientRequestId, actionId, ActionState.WRITING, null, null);
                    try {
                        T result = action.call();
                        updateState(ownerId, clientRequestId, actionId, ActionState.SUCCEEDED, result, null);
                        return result;
                    } catch (Exception failure) {
                        if (failure instanceof RuntimeException runtimeException) {
                            throw runtimeException;
                        }
                        throw new AppException(ErrorCode.AI_RESPONSE_INVALID, failure.getMessage());
                    }
                });
            } else {
                updateState(ownerId, clientRequestId, actionId, ActionState.WRITING, null, null);
                T result = action.call();
                updateState(ownerId, clientRequestId, actionId, ActionState.SUCCEEDED, result, null);
                return result;
            }
        } catch (Exception failure) {
            // Check if outcome actually succeeded (commit succeeded but network timeout occurred)
            Optional<JournalEntry> outcome = find(ownerId, clientRequestId, actionId);
            if (outcome.isPresent() && outcome.get().state() == ActionState.SUCCEEDED) {
                @SuppressWarnings("unchecked")
                T cached = (T) outcome.get().resultData();
                return cached;
            }

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

    /**
     * Startup recovery: marks any abandoned RESERVED or WRITING actions created before startup
     * as INDETERMINATE so they are never blindly retried without domain state verification.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverAbandonedReservations() {
        if (jdbc != null && requiresNewTransaction != null && bootCutoff != null) {
            try {
                requiresNewTransaction.executeWithoutResult(status -> {
                    jdbc.update("""
                            UPDATE AI_ASSISTANT_ACTION_JOURNAL
                            SET state = 'INDETERMINATE', error_message = 'Phiên xử lý bị gián đoạn do hệ thống khởi động lại'
                            WHERE state IN ('RESERVED', 'WRITING') AND created_at < ?
                            """, bootCutoff);
                    jdbc.update("DELETE FROM AI_ASSISTANT_ACTION_JOURNAL WHERE expires_at <= SYSUTCDATETIME()");
                });
                log.info("AI Action Journal startup recovery completed with cutoff: {}", bootCutoff);
            } catch (Exception ex) {
                log.warn("AI Action Journal startup recovery failed: {}", ex.getMessage());
            }
        }
    }

    private static String hash(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
