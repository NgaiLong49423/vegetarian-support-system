package tech.mamxanh.recipe.service;

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
import tech.mamxanh.shopping.dto.request.CreateEmptyShoppingListRequest;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse;
import tech.mamxanh.shopping.service.ShoppingListService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** One create per request. ADK remains the executor; SQL only records its business write. */
@Service
public class AiShoppingExecutionJournal {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ShoppingListService shopping;
    private final java.time.LocalDateTime bootCutoff;

    public AiShoppingExecutionJournal(JdbcTemplate jdbc, PlatformTransactionManager manager, ShoppingListService shopping) {
        this.jdbc = jdbc;
        this.shopping = shopping;
        this.bootCutoff = jdbc.queryForObject("SELECT SYSUTCDATETIME()", java.time.LocalDateTime.class);
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transaction.setTimeout(10);
    }

    public record Result(UUID clientRequestId, UUID actionId, String state, Long shoppingListId) { }

    public Result find(long owner, UUID requestId) {
        List<Result> rows = jdbc.query("""
                SELECT client_request_id, state, shopping_list_id FROM AI_ASSISTANT_EXECUTION
                WHERE owner_id=? AND client_request_id=? AND expires_at > SYSUTCDATETIME()
                """, (rs, row) -> new Result(requestId, requestId, rs.getString("state"),
                rs.getObject("shopping_list_id", Long.class)), owner, requestId);
        return rows.isEmpty() ? new Result(requestId, requestId, "UNKNOWN", null) : rows.getFirst();
    }

    public ShoppingListResponse create(long owner, UUID sessionId, UUID requestId, String name, BooleanSupplier allowed) {
        if (!allowed.getAsBoolean()) throw stopped();
        String hash = hash(name);
        // Reservation must survive rollback of the following business transaction.
        try {
            transaction.executeWithoutResult(status -> jdbc.update("""
                    INSERT INTO AI_ASSISTANT_EXECUTION(owner_id,client_request_id,session_id,arguments_hash,state)
                    VALUES (?,?,?,?,'RESERVED')
                    """, owner, requestId, sessionId, hash));
        } catch (DuplicateKeyException duplicate) {
            // Read only. Never turn an abandoned RESERVED record into a retry.
            String previous = jdbc.queryForObject("SELECT arguments_hash FROM AI_ASSISTANT_EXECUTION WHERE owner_id=? AND client_request_id=?",
                    String.class, owner, requestId);
            if (!hash.equals(previous)) throw new AppException(ErrorCode.VALIDATION_FAILED, "Mã thao tác đã gắn với tham số khác.");
            Result result = find(owner, requestId);
            if ("SUCCEEDED".equals(result.state())) return shopping.get(result.shoppingListId());
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Thao tác đã được ghi nhận; hãy kiểm tra kết quả, không gửi lại thao tác ghi.");
        }
        try {
            return transaction.execute(status -> {
                // Conditional UPDATE takes the SQL row lock; cancel and write compete for this row.
                if (jdbc.update("""
                        UPDATE AI_ASSISTANT_EXECUTION SET state='WRITING'
                        WHERE owner_id=? AND client_request_id=? AND state='RESERVED'
                        """, owner, requestId) != 1) throw stopped();
                if (!allowed.getAsBoolean()) throw stopped();
                // REQUIRED public Spring service joins this transaction, on this thread.
                ShoppingListResponse created = shopping.createEmpty(new CreateEmptyShoppingListRequest(name));
                jdbc.update("""
                        UPDATE AI_ASSISTANT_EXECUTION SET state='SUCCEEDED', shopping_list_id=?
                        WHERE owner_id=? AND client_request_id=? AND state='WRITING'
                        """, created.id(), owner, requestId);
                return created;
            });
        } catch (RuntimeException failure) {
            Result outcome = find(owner, requestId);
            if ("SUCCEEDED".equals(outcome.state())) return shopping.get(outcome.shoppingListId());
            // Only after the business transaction has returned/rolled back. If outcome cannot be
            // read (e.g. SQL disconnected during commit), leave it UNKNOWN to the caller, never replay.
            transaction.executeWithoutResult(status -> jdbc.update("""
                    UPDATE AI_ASSISTANT_EXECUTION SET state=?
                    WHERE owner_id=? AND client_request_id=? AND state='RESERVED'
                    """, allowed.getAsBoolean() ? "FAILED" : "CANCELLED", owner, requestId));
            throw failure;
        }
    }

    public void cancel(long owner, UUID requestId) {
        transaction.executeWithoutResult(status -> jdbc.update("""
                UPDATE AI_ASSISTANT_EXECUTION SET state='CANCELLED'
                WHERE owner_id=? AND client_request_id=? AND state='RESERVED'
                """, owner, requestId));
    }

    /** Single Backend instance only: boot occurs before accepting new requests, no live owner remains. */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverAbandonedReservations() {
        transaction.executeWithoutResult(status -> {
            jdbc.update("UPDATE AI_ASSISTANT_EXECUTION SET state='FAILED' WHERE state='RESERVED' AND created_at < ?", bootCutoff);
            jdbc.update("DELETE FROM AI_ASSISTANT_EXECUTION WHERE expires_at <= SYSUTCDATETIME()");
        });
    }

    private static IllegalStateException stopped() { return new IllegalStateException("Thao tác đã dừng trước bước ghi."); }
    private static String hash(String name) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(name.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
