package tech.mamxanh.recipe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentScope;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Bounded, process-local state for consent and pending AI actions; no prompts are retained. */
@Component
public class AiAssistantSessionStore {
    private static final int MAX_SESSIONS = 2_000;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Clock clock;
    private final Duration sessionTtl;
    private final Duration confirmationTtl = Duration.ofMinutes(5);

    public AiAssistantSessionStore(Clock clock,
            @Value("${mamxanh.ai-chat.session-ttl:PT30M}") Duration sessionTtl) {
        this.clock = clock;
        this.sessionTtl = sessionTtl.isNegative() || sessionTtl.isZero() ? Duration.ofMinutes(30) : sessionTtl;
    }

    public static final String ENGINE_LEGACY = "LEGACY_ASSISTANT";
    public static final String ENGINE_G3_PILOT = "ADK_SHOPPING_PILOT";

    public synchronized SessionHandle open(UUID requestedId, String owner) {
        return open(requestedId, owner, ENGINE_LEGACY);
    }

    public synchronized SessionHandle open(UUID requestedId, String owner, String requestedEngine) {
        Instant now = clock.instant();
        removeExpired(now);
        String targetEngine = requestedEngine == null ? ENGINE_LEGACY : requestedEngine;
        if (requestedId != null) {
            Session existing = sessions.get(requestedId);
            if (existing == null || !existing.owner.equals(owner)) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Phiên trợ lý không hợp lệ hoặc đã hết hạn.");
            }
            if (!existing.engine.equals(targetEngine)) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Phiên trợ lý đã gắn cố định với bộ điều phối khác.");
            }
            existing.lastAccess = now;
            existing.expiresAt = now.plus(sessionTtl);
            return new SessionHandle(existing.id, Set.copyOf(existing.consents), existing.pending, existing.expiresAt, existing.engine);
        }
        if (sessions.size() >= MAX_SESSIONS) {
            sessions.values().stream().min(java.util.Comparator.comparing(item -> item.lastAccess))
                    .map(item -> item.id).ifPresent(sessions::remove);
        }
        UUID id = UUID.randomUUID();
        Session session = new Session(id, owner, targetEngine, now, sessionTtl);
        sessions.put(id, session);
        return new SessionHandle(id, Set.copyOf(session.consents), session.pending, session.expiresAt, session.engine);
    }

    public synchronized Optional<String> peekEngine(UUID sessionId, String owner) {
        if (sessionId == null) return Optional.empty();
        Instant now = clock.instant();
        removeExpired(now);
        Session session = sessions.get(sessionId);
        if (session == null || !session.owner.equals(owner)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Phiên trợ lý không hợp lệ hoặc đã hết hạn.");
        }
        return Optional.of(session.engine);
    }

    public synchronized SessionHandle applyConsent(UUID sessionId, String owner,
            boolean grant, Set<ConsentScope> scopes) {
        Session session = require(sessionId, owner);
        if (grant) session.consents.addAll(scopes);
        else if (scopes.isEmpty()) session.consents.clear();
        else session.consents.removeAll(scopes);
        Instant now = clock.instant();
        session.lastAccess = now;
        session.expiresAt = now.plus(sessionTtl);
        return new SessionHandle(session.id, Set.copyOf(session.consents), session.pending, session.expiresAt, session.engine);
    }

    public synchronized PendingPlan createPending(UUID sessionId, String owner,
            List<PendingAction> actions, boolean explicitConfirmation) {
        Session session = require(sessionId, owner);
        PendingPlan pending = new PendingPlan(UUID.randomUUID(), List.copyOf(actions),
                explicitConfirmation, clock.instant().plus(confirmationTtl));
        session.pending = pending;
        session.lastAccess = clock.instant();
        session.expiresAt = session.lastAccess.plus(sessionTtl);
        return pending;
    }

    public synchronized PendingPlan takePending(UUID sessionId, String owner, UUID confirmationId) {
        Session session = require(sessionId, owner);
        PendingPlan pending = session.pending;
        if (pending == null || !pending.id().equals(confirmationId) || !pending.expiresAt().isAfter(clock.instant())) {
            session.pending = null;
            return null;
        }
        session.pending = null;
        session.lastAccess = clock.instant();
        session.expiresAt = session.lastAccess.plus(sessionTtl);
        return pending;
    }

    public synchronized void discardPending(UUID sessionId, String owner) {
        Session session = require(sessionId, owner);
        session.pending = null;
    }

    public synchronized void delete(UUID sessionId, String owner) {
        Session session = sessions.get(sessionId);
        if (session == null) return;
        if (!session.owner.equals(owner)) throw new AppException(ErrorCode.VALIDATION_FAILED,
                "Phiên trợ lý không hợp lệ hoặc đã hết hạn.");
        sessions.remove(sessionId);
    }

    private Session require(UUID id, String owner) {
        Session session = sessions.get(id);
        Instant now = clock.instant();
        if (session == null || !session.expiresAt.isAfter(now)) {
            sessions.remove(id);
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Phiên trợ lý không hợp lệ hoặc đã hết hạn.");
        }
        if (!session.owner.equals(owner)) throw new AppException(ErrorCode.VALIDATION_FAILED,
                "Phiên trợ lý không hợp lệ hoặc đã hết hạn.");
        session.lastAccess = now;
        session.expiresAt = now.plus(sessionTtl);
        return session;
    }

    private void removeExpired(Instant now) {
        sessions.values().removeIf(session -> !session.expiresAt.isAfter(now));
    }

    public record SessionHandle(UUID id, Set<ConsentScope> consentScopes,
            PendingPlan pending, Instant expiresAt, String engine) {
        public SessionHandle(UUID id, Set<ConsentScope> consentScopes, PendingPlan pending, Instant expiresAt) {
            this(id, consentScopes, pending, expiresAt, ENGINE_LEGACY);
        }
    }

    public record PendingAction(String tool, Map<String, Object> arguments, boolean requiresExplicitConfirmation) {
        public PendingAction {
            arguments = Collections.unmodifiableMap(new HashMap<>(arguments));
        }
    }

    public record PendingPlan(UUID id, List<PendingAction> actions,
            boolean requiresExplicitConfirmation, Instant expiresAt) { }

    private static final class Session {
        private final UUID id;
        private final String owner;
        private final String engine;
        private final Set<ConsentScope> consents = new HashSet<>();
        private Instant lastAccess;
        private Instant expiresAt;
        private PendingPlan pending;

        private Session(UUID id, String owner, String engine, Instant createdAt, Duration ttl) {
            this.id = id;
            this.owner = owner;
            this.engine = engine;
            this.lastAccess = createdAt;
            this.expiresAt = createdAt.plus(ttl);
        }
    }
}
