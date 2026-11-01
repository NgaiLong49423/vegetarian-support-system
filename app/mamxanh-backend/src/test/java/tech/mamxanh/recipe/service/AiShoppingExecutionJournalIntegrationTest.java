package tech.mamxanh.recipe.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.shopping.service.ShoppingListService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiShoppingExecutionJournalIntegrationTest extends AbstractIntegrationTest {
    @Autowired AiShoppingExecutionJournal journal;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager manager;
    @MockitoSpyBean ShoppingListService shopping;
    private long owner;
    private final UUID session = UUID.randomUUID();

    @BeforeEach void member() {
        String email = "g4-" + UUID.randomUUID() + "@example.invalid";
        jdbc.update("INSERT INTO [USER](email,display_name,role,account_status,email_verified) VALUES (?,N'G4','CUSTOMER','ACTIVE',1)", email);
        owner = jdbc.queryForObject("SELECT user_id FROM [USER] WHERE email=?", Long.class, email);
        authenticate();
    }
    @AfterEach void cleanup() {
        reset(shopping);
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM AI_ASSISTANT_EXECUTION WHERE owner_id=?", owner);
        jdbc.update("DELETE FROM SHOPPING_LIST WHERE user_id=?", owner);
        jdbc.update("DELETE FROM [USER] WHERE user_id=?", owner);
    }
    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                Long.toString(owner), null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }
    private int lists() { return jdbc.queryForObject("SELECT COUNT(*) FROM SHOPPING_LIST WHERE user_id=?", Integer.class, owner); }

    @Test void responseLossAndRestartReadTheCommittedResultWithoutAnotherInsert() {
        UUID request = UUID.randomUUID();
        long id = journal.create(owner, session, request, "G4 response lost", () -> true).id();
        journal.recoverAbandonedReservations(); // same restart hook, no runner/session survives
        assertThat(journal.find(owner, request).shoppingListId()).isEqualTo(id);
        assertThat(journal.create(owner, session, request, "G4 response lost", () -> true).id()).isEqualTo(id);
        assertThat(lists()).isEqualTo(1);
        assertThat(journal.find(owner + 1, request).state()).isEqualTo("UNKNOWN");
        assertThatThrownBy(() -> journal.create(owner, session, request, "different", () -> true)).isInstanceOf(RuntimeException.class);
    }

    @Test void rollbackDoesNotLoseReservationAndNeverReplaysTheWrite() {
        UUID request = UUID.randomUUID();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("after insert, before commit"); })
                .when(shopping).createEmpty(any());
        assertThatThrownBy(() -> journal.create(owner, session, request, "G4 rollback", () -> true)).isInstanceOf(IllegalStateException.class);
        assertThat(lists()).isZero();
        assertThat(journal.find(owner, request).state()).isEqualTo("FAILED");
        reset(shopping);
        assertThatThrownBy(() -> journal.create(owner, session, request, "G4 rollback", () -> true)).isInstanceOf(RuntimeException.class);
        assertThat(lists()).isZero();
    }

    @Test void cancelAtTheWriteBoundaryRollsBackClaimButNotReservation() {
        UUID request = UUID.randomUUID();
        AtomicInteger checks = new AtomicInteger();
        assertThatThrownBy(() -> journal.create(owner, session, request, "G4 cancelled", () -> checks.incrementAndGet() == 1))
                .isInstanceOf(IllegalStateException.class);
        assertThat(lists()).isZero();
        assertThat(journal.find(owner, request).state()).isEqualTo("CANCELLED");
    }

    @Test void concurrentDuplicateWaitsForCommitAndCreatesOnlyOneList() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        doAnswer(call -> { entered.countDown(); assertThat(release.await(15, TimeUnit.SECONDS)).isTrue(); return call.callRealMethod(); })
                .when(shopping).createEmpty(any());
        UUID request = UUID.randomUUID();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> { authenticate(); return journal.create(owner, session, request, "G4 concurrent", () -> true).id(); });
            assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
            var second = pool.submit(() -> { authenticate(); return journal.create(owner, session, request, "G4 concurrent", () -> true).id(); });
            release.countDown();
            assertThat(second.get(20, TimeUnit.SECONDS)).isEqualTo(first.get(20, TimeUnit.SECONDS));
        } finally { release.countDown(); }
        assertThat(lists()).isEqualTo(1);
    }

    @Test void cancelDuringWriteWaitsForSqlLockAndCannotReplaceSuccess() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        doAnswer(call -> { entered.countDown(); assertThat(release.await(15, TimeUnit.SECONDS)).isTrue(); return call.callRealMethod(); })
                .when(shopping).createEmpty(any());
        UUID request = UUID.randomUUID();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var write = pool.submit(() -> { authenticate(); return journal.create(owner, session, request, "G4 cancel race", () -> true).id(); });
            assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
            var cancelStarted = new CountDownLatch(1);
            var cancel = pool.submit(() -> { cancelStarted.countDown(); journal.cancel(owner, request); });
            assertThat(cancelStarted.await(5, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            long id = write.get(20, TimeUnit.SECONDS); cancel.get(20, TimeUnit.SECONDS);
            assertThat(journal.find(owner, request).state()).isEqualTo("SUCCEEDED");
            assertThat(journal.find(owner, request).shoppingListId()).isEqualTo(id);
        } finally { release.countDown(); }
        assertThat(lists()).isEqualTo(1);
    }

    @Test void abandonedReservationStaysReservedUntilRestartRecovery() {
        UUID request = UUID.randomUUID();
        jdbc.update("INSERT INTO AI_ASSISTANT_EXECUTION(owner_id,client_request_id,session_id,arguments_hash,state,created_at) VALUES (?,?,?,?,'RESERVED',DATEADD(day,-1,SYSUTCDATETIME()))",
                owner, request, session, "a".repeat(64));
        assertThat(journal.find(owner, request).state()).isEqualTo("RESERVED");
        journal.recoverAbandonedReservations();
        assertThat(journal.find(owner, request).state()).isEqualTo("FAILED");
        assertThat(lists()).isZero();
    }

    @Test void startupRecoveryDoesNotFailReservationsCreatedByTheCurrentProcess() {
        UUID request = UUID.randomUUID();
        jdbc.update("INSERT INTO AI_ASSISTANT_EXECUTION(owner_id,client_request_id,session_id,arguments_hash,state) VALUES (?,?,?,?,'RESERVED')",
                owner, request, session, "a".repeat(64));
        journal.recoverAbandonedReservations();
        assertThat(journal.find(owner, request).state()).isEqualTo("RESERVED");
    }

    @Test void sqlLockTimeoutDoesNotMarkAnActiveReservationFailedOrReplayIt() throws Exception {
        UUID request = UUID.randomUUID();
        jdbc.update("INSERT INTO AI_ASSISTANT_EXECUTION(owner_id,client_request_id,session_id,arguments_hash,state) VALUES (?,?,?,?,'RESERVED')",
                owner, request, session, "a".repeat(64));
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var holder = pool.submit(() -> new org.springframework.transaction.support.TransactionTemplate(manager).execute(status -> {
                jdbc.update("UPDATE AI_ASSISTANT_EXECUTION SET state='RESERVED' WHERE owner_id=? AND client_request_id=?", owner, request);
                entered.countDown();
                try { assertThat(release.await(25, TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException error) { throw new IllegalStateException(error); }
                return null;
            }));
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> journal.cancel(owner, request)).isInstanceOf(org.springframework.dao.DataAccessException.class);
            } finally { release.countDown(); }
            holder.get(10, TimeUnit.SECONDS);
        } finally { release.countDown(); }
        assertThat(journal.find(owner, request).state()).isEqualTo("RESERVED");
        assertThat(lists()).isZero();
    }
}
