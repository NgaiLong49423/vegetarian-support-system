package tech.mamxanh.integration.ai.adk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.BaseLlmConnection;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AdkSpikeTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest @ValueSource(strings = {"CUSTOMER", "EXPERT"})
    void nativeToolLoopPreservesIdentityArgumentsResultsAndHistory(String role) {
        var policy = new Ledger();
        var tools = new FixtureTools();
        var model = new FakeModel(request -> {
            if (request.contents().stream().flatMap(c -> c.parts().orElse(List.of()).stream())
                    .anyMatch(p -> p.functionResponse().isPresent())) return text("Đã đọc danh sách số 42.");
            return call("read_list", "read-1", Map.of("listId", 42));
        });
        Authentication owner = actor("7", role);
        try (var session = session(owner, model, policy, tools, false)) {
            var events = session.run(owner, user("Đọc danh sách 42")).toList().blockingGet();
            assertThat(events.stream().flatMap(e -> e.functionResponses().stream())).anySatisfy(response ->
                    assertThat(response.response().orElseThrow()).containsEntry("id", 42));
            assertThat(tools.observedOwners).containsExactly("7");
            assertThat(policy.calls).hasSize(2);
            assertThat(policy.usages).hasSize(2);
            assertThat(model.requests.get(1).contents().stream().flatMap(c -> c.parts().orElse(List.of()).stream()))
                    .anyMatch(p -> p.functionResponse().isPresent());
            session.run(owner, user("Cảm ơn")).toList().blockingGet();
            assertThat(model.requests.get(2).contents()).hasSizeGreaterThan(2);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Test void confirmationPausesAndResumesThroughNativeAdkWithoutReplayingWrite() {
        var tools = new FixtureTools();
        var model = new FakeModel(request -> request.contents().stream()
                .flatMap(c -> c.parts().orElse(List.of()).stream()).anyMatch(p -> p.functionResponse().isPresent())
                ? text("Xong") : call("create_list", "write-1", Map.of("name", "Cuối tuần")));
        var owner = actor("7", "CUSTOMER");
        try (var session = session(owner, model, new Ledger(), tools, true)) {
            var staged = session.run(owner, user("Tạo danh sách Cuối tuần")).toList().blockingGet();
            assertThat(tools.writes.get()).isZero();
            var confirmation = staged.stream().flatMap(e -> e.functionCalls().stream())
                    .filter(c -> c.name().orElse("").equals("adk_request_confirmation")).findFirst().orElseThrow();
            var approve = confirmationResponse(confirmation.id().orElseThrow(), true);
            session.run(owner, approve).toList().blockingGet();
            assertThat(tools.writes.get()).isEqualTo(1);
            // The boundary stops a consumed confirmation; it cannot authorize a second write.
            assertThatThrownBy(() -> session.run(owner, approve).toList().blockingGet())
                    .isInstanceOf(IllegalStateException.class).hasMessage("Run stopped");
            assertThat(tools.writes.get()).isEqualTo(1);
        }
    }

    @Test void rejectedConfirmationDoesNotWrite() {
        assertConfirmationBlocked(false, false);
    }

    @Test void revokedConsentBeforeApprovalDoesNotWrite() {
        assertConfirmationBlocked(true, true);
    }

    private void assertConfirmationBlocked(boolean approve, boolean revoke) {
        var tools = new FixtureTools();
        var allowed = new AtomicBoolean(true);
        var model = new FakeModel(r -> r.contents().stream().flatMap(c -> c.parts().orElse(List.of()).stream())
                .anyMatch(p -> p.functionResponse().isPresent()) ? text("Dừng")
                : call("create_list", "write-1", Map.of("name", "Cuối tuần")));
        var owner = actor("7", "EXPERT");
        var tool = new AdkSpikeSession.Tool(FunctionTool.create(tools, "create_list", true), args -> {
            if (!allowed.get()) throw new SecurityException("Consent revoked");
        });
        try (var session = new AdkSpikeSession(owner, new AdkModelGateway(model, new Ledger(), CLOCK), List.of(tool), CLOCK)) {
            var staged = session.run(owner, user("Tạo")).toList().blockingGet();
            var confirmation = staged.stream().flatMap(e -> e.functionCalls().stream())
                    .filter(c -> c.name().orElse("").equals("adk_request_confirmation")).findFirst().orElseThrow();
            if (revoke) allowed.set(false);
            try { session.run(owner, confirmationResponse(confirmation.id().orElseThrow(), approve)).toList().blockingGet(); }
            catch (RuntimeException expected) { assertThat(revoke).isTrue(); }
            assertThat(tools.writes.get()).isZero();
        }
    }

    @Test void wrongOwnerAndAdminCannotUseMemberSession() {
        var owner = actor("7", "CUSTOMER");
        try (var session = session(owner, new FakeModel(r -> text("OK")), new Ledger(), new FixtureTools(), false)) {
            assertThatThrownBy(() -> session.run(actor("8", "CUSTOMER"), user("x")).blockingLast()).isInstanceOf(SecurityException.class);
            assertThatThrownBy(() -> session.run(actor("7", "ADMIN"), user("x")).blockingLast()).isInstanceOf(SecurityException.class);
        }
    }

    @Test void expiredNativeConfirmationAndSessionCannotExecuteWrite() {
        var clock = new MutableClock();
        var tools = new FixtureTools();
        var owner = actor("7", "CUSTOMER");
        var model = new FakeModel(r -> r.contents().stream().flatMap(c -> c.parts().orElse(List.of()).stream())
                .anyMatch(p -> p.functionResponse().isPresent()) ? text("Dừng")
                : call("create_list", "write-expiry", Map.of("name", "Cuối tuần")));
        try (var session = new AdkSpikeSession(owner, new AdkModelGateway(model, new Ledger(), clock),
                List.of(new AdkSpikeSession.Tool(FunctionTool.create(tools, "create_list", true), a -> { })), clock)) {
            var events = session.run(owner, user("Tạo")).toList().blockingGet();
            var confirmation = events.stream().flatMap(e -> e.functionCalls().stream())
                    .filter(c -> c.name().orElse("").equals("adk_request_confirmation")).findFirst().orElseThrow();
            clock.now = clock.now.plus(Duration.ofMinutes(6));
            assertThatThrownBy(() -> session.run(owner, confirmationResponse(confirmation.id().orElseThrow(), true)).toList().blockingGet())
                    .isInstanceOf(IllegalStateException.class).hasMessage("Run stopped");
            assertThat(tools.writes.get()).isZero();
            clock.now = clock.now.plus(Duration.ofMinutes(31));
            assertThatThrownBy(() -> session.run(owner, user("x")).blockingLast()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test void wholeRunDeadlineAndCancellationStopBeforeAnyTool() {
        var model = new FakeModel(r -> text("unused")) {
            @Override public Flowable<LlmResponse> generateContent(LlmRequest r, boolean stream) { return Flowable.never(); }
        };
        var owner = actor("7", "EXPERT");
        var tools = new FixtureTools();
        try (var session = new AdkSpikeSession(owner, new AdkModelGateway(model, new Ledger(), CLOCK),
                List.of(new AdkSpikeSession.Tool(FunctionTool.create(tools, "create_list"), a -> { })), CLOCK, Duration.ofMillis(80))) {
            var subscriber = session.run(owner, user("x")).test();
            subscriber.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS).assertError(java.util.concurrent.TimeoutException.class);
            assertThat(tools.writes.get()).isZero();
            var cancel = session.run(owner, user("x")).test();
            cancel.cancel();
            assertThat(tools.writes.get()).isZero();
        }
    }

    @Test void concurrentSessionsKeepTheirOwnAuthentication() {
        var one = new FixtureTools();
        var two = new FixtureTools();
        Function<LlmRequest, LlmResponse> flow = r -> r.contents().stream()
                .flatMap(c -> c.parts().orElse(List.of()).stream()).anyMatch(p -> p.functionResponse().isPresent())
                ? text("OK") : call("read_list", "read-1", Map.of("listId", 42));
        var a = actor("7", "CUSTOMER"); var b = actor("8", "EXPERT");
        try (var s1 = session(a, new FakeModel(flow), new Ledger(), one, false);
             var s2 = session(b, new FakeModel(flow), new Ledger(), two, false)) {
            var f1 = java.util.concurrent.CompletableFuture.runAsync(() -> s1.run(a, user("x")).blockingLast());
            var f2 = java.util.concurrent.CompletableFuture.runAsync(() -> s2.run(b, user("x")).blockingLast());
            java.util.concurrent.CompletableFuture.allOf(f1, f2).join();
            assertThat(one.observedOwners).containsExactly("7");
            assertThat(two.observedOwners).containsExactly("8");
        }
    }

    @Test void providerErrorAfterSuccessfulToolDoesNotReplayItsWrite() {
        var count = new AtomicInteger(); var ledger = new Ledger(); var tools = new FixtureTools();
        var model = new FakeModel(r -> {
            if (count.getAndIncrement() == 0) return call("create_list", "write-1", Map.of("name", "Cuối tuần"));
            throw new com.google.genai.errors.ApiException(503, "fixture", "UNAVAILABLE");
        });
        var owner = actor("7", "CUSTOMER");
        try (var session = new AdkSpikeSession(owner, new AdkModelGateway(model, ledger, CLOCK),
                List.of(new AdkSpikeSession.Tool(FunctionTool.create(tools, "create_list"), a -> { })), CLOCK)) {
            assertThatThrownBy(() -> session.run(owner, user("x")).blockingLast()).isInstanceOf(RuntimeException.class);
            assertThat(tools.writes.get()).isEqualTo(1);
            assertThat(ledger.usages).hasSize(1);
            assertThat(count.get()).isEqualTo(3); // One successful call; two bounded attempts of the next call.
        }
    }

    @Test void throttlingAndPolicyDenialDoNotRetryOrDebit() {
        var ledger = new Ledger(); var attempts = new AtomicInteger();
        var model = new FakeModel(r -> { attempts.incrementAndGet(); throw new com.google.genai.errors.ApiException(429, "fixture", "RESOURCE_EXHAUSTED"); });
        var gateway = new AdkModelGateway(model, ledger, CLOCK);
        assertThatThrownBy(() -> gateway.generateContent(LlmRequest.builder().build(), false).blockingLast()).isInstanceOf(RuntimeException.class);
        assertThat(attempts.get()).isEqualTo(1); assertThat(ledger.usages).isEmpty();
        var denied = new AdkModelGateway(model, new Ledger() {
            @Override public Object beforeCall(AdkModelGateway.Call call) { throw new SecurityException("No budget"); }
        }, CLOCK);
        assertThatThrownBy(() -> denied.generateContent(LlmRequest.builder().build(), false).blockingLast()).isInstanceOf(SecurityException.class);
        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test void springContextCanConstructGatewayAndNativeRunnerWithoutDatabaseOrApplicationRoute() {
        new ApplicationContextRunner().withBean("model", FakeModel.class, () -> new FakeModel(r -> text("OK")))
                .withBean(AdkModelGateway.class, () -> new AdkModelGateway(new FakeModel(r -> text("OK")), new Ledger(), CLOCK))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    try (var session = new AdkSpikeSession(actor("7", "CUSTOMER"), context.getBean(AdkModelGateway.class), List.of(), CLOCK)) {
                        assertThat(session.run(actor("7", "CUSTOMER"), user("x")).toList().blockingGet()).isNotEmpty();
                    }
                });
    }

    @Test void policyHooksSeeTrustedIdentityAndDoNotLeakItToCallerThread() {
        var observed = new CopyOnWriteArrayList<String>();
        var policy = new Ledger() {
            @Override public Object beforeCall(AdkModelGateway.Call call) {
                observed.add(SecurityContextHolder.getContext().getAuthentication().getName());
                return super.beforeCall(call);
            }
            @Override public void onUsableResponse(AdkModelGateway.Call call, Object snapshot, AdkModelGateway.Usage usage) {
                observed.add(SecurityContextHolder.getContext().getAuthentication().getName());
                super.onUsableResponse(call, snapshot, usage);
            }
        };
        var callerContext = actor("999", "EXPERT");
        SecurityContextHolder.getContext().setAuthentication(callerContext);
        var owner = actor("7", "CUSTOMER");
        try (var session = session(owner, new FakeModel(r -> text("OK")), policy, new FixtureTools(), false)) {
            session.run(owner, user("x")).blockingLast();
            assertThat(observed).containsExactly("7", "7");
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(callerContext);
        }
    }

    @Test void modelUsesTheCreatedResourceIdInItsNextToolCall() {
        var tools = new FixtureTools(); var owner = actor("7", "EXPERT");
        var model = new FakeModel(request -> {
            var results = request.contents().stream().flatMap(c -> c.parts().orElse(List.of()).stream())
                    .flatMap(p -> p.functionResponse().stream()).toList();
            if (results.stream().anyMatch(r -> r.name().orElse("").equals("read_list"))) return text("Đã đọc 42");
            var created = results.stream().filter(r -> r.name().orElse("").equals("create_list")).findFirst();
            if (created.isPresent()) return call("read_list", "read-created", Map.of("listId", created.get().response().orElseThrow().get("id")));
            return call("create_list", "create-first", Map.of("name", "Cuối tuần"));
        });
        try (var session = new AdkSpikeSession(owner, new AdkModelGateway(model, new Ledger(), CLOCK), List.of(
                new AdkSpikeSession.Tool(FunctionTool.create(tools, "create_list"), a -> { }),
                new AdkSpikeSession.Tool(FunctionTool.create(tools, "read_list"), a -> { })), CLOCK)) {
            session.run(owner, user("Tạo rồi đọc")).blockingLast();
            assertThat(tools.writes.get()).isEqualTo(1);
            assertThat(tools.observedOwners).containsExactly("7");
            assertThat(model.requests).hasSize(3);
        }
    }

    @Test void boundedLlmCallsStopAnAgentThatKeepsRequestingTools() {
        var ledger = new Ledger(); var owner = actor("7", "CUSTOMER");
        var model = new FakeModel(r -> call("read_list", java.util.UUID.randomUUID().toString(), Map.of("listId", 42)));
        try (var session = session(owner, model, ledger, new FixtureTools(), false)) {
            assertThatThrownBy(() -> session.run(owner, user("x")).blockingLast()).isInstanceOf(RuntimeException.class);
            assertThat(ledger.calls.size()).isLessThanOrEqualTo(6);
        }
    }

    @Test void qaModeGuardAndTypedArgumentValidationStopBeforeMutation() {
        var owner = actor("7", "CUSTOMER"); var tools = new FixtureTools();
        try (var session = new AdkSpikeSession(owner,
                new AdkModelGateway(new FakeModel(r -> call("create_list", "write", Map.of("name", "X"))), new Ledger(), CLOCK),
                List.of(new AdkSpikeSession.Tool(FunctionTool.create(tools, "create_list"), args -> {
                    throw new SecurityException("QA cannot write");
                })), CLOCK)) {
            assertThatThrownBy(() -> session.run(owner, user("x")).blockingLast()).isInstanceOf(RuntimeException.class);
            assertThat(tools.writes.get()).isZero();
        }
        var model = new FakeModel(r -> call("read_list", "bad-id", Map.of("listId", 0)));
        try (var session = session(owner, model, new Ledger(), tools, false)) {
            assertThatThrownBy(() -> session.run(owner, user("x")).blockingLast()).isInstanceOf(RuntimeException.class);
            assertThat(tools.observedOwners).isEmpty();
        }
    }

    @Test void invalidResponseAndUnknownToolsAreNotCharged() {
        for (LlmResponse response : List.of(LlmResponse.builder().build(), call("unknown", "bad", Map.of()))) {
            var ledger = new Ledger();
            var gateway = new AdkModelGateway(new FakeModel(r -> response), ledger, CLOCK);
            assertThatThrownBy(() -> gateway.generateContent(LlmRequest.builder().contents(List.of(user("x"))).build(), false).blockingLast())
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(ledger.usages).isEmpty();
        }
    }

    @Test void usableResponseWithoutUsageDoesNotInventTokens() {
        var ledger = new Ledger();
        var gateway = new AdkModelGateway(new FakeModel(r -> text("OK").toBuilder().usageMetadata(null).build()), ledger, CLOCK);
        gateway.generateContent(LlmRequest.builder().contents(List.of(user("x"))).build(), false).blockingLast();
        assertThat(ledger.usages).singleElement().satisfies(usage -> {
            assertThat(usage.status()).isEqualTo(AdkModelGateway.UsageStatus.MISSING);
            assertThat(usage.metadata()).isNull();
        });
    }

    @Test void usableResponseWithInvalidUsageIsMarkedWithoutInventingOrChargingTokens() {
        var ledger = new Ledger();
        var gateway = new AdkModelGateway(new FakeModel(r -> text("OK").toBuilder().usageMetadata(
                GenerateContentResponseUsageMetadata.builder().promptTokenCount(20).candidatesTokenCount(5).totalTokenCount(3).build()).build()), ledger, CLOCK);
        assertThat(gateway.generateContent(LlmRequest.builder().build(), false).blockingLast().content()).isPresent();
        assertThat(ledger.usages).singleElement().satisfies(usage -> {
            assertThat(usage.status()).isEqualTo(AdkModelGateway.UsageStatus.INVALID);
            assertThat(usage.metadata().totalTokens()).isEqualTo(3);
        });
    }

    @Test void accountingFailureCannotTriggerAnotherProviderCall() {
        var model = new FakeModel(r -> text("OK"));
        var ledger = new Ledger() {
            @Override public void onUsableResponse(AdkModelGateway.Call call, Object context, AdkModelGateway.Usage usage) {
                throw new com.google.genai.errors.ApiException(503, "fixture accounting failure", "UNAVAILABLE");
            }
        };
        assertThatThrownBy(() -> new AdkModelGateway(model, ledger, CLOCK)
                .generateContent(LlmRequest.builder().build(), false).blockingLast()).isInstanceOf(RuntimeException.class);
        assertThat(model.requests).hasSize(1);
    }

    @Test void adkContentTracingIsDisabledAndOneSessionRejectsConcurrentRuns() throws Exception {
        var capture = com.google.adk.telemetry.Tracing.class.getDeclaredField("CAPTURE_MESSAGE_CONTENT_IN_SPANS");
        capture.setAccessible(true);
        assertThat(capture.getBoolean(null)).isFalse();
        var owner = actor("7", "CUSTOMER");
        var model = new FakeModel(r -> text("unused")) {
            @Override public Flowable<LlmResponse> generateContent(LlmRequest r, boolean stream) { return Flowable.never(); }
        };
        try (var session = session(owner, model, new Ledger(), new FixtureTools(), false)) {
            var first = session.run(owner, user("x")).test();
            assertThatThrownBy(() -> session.run(owner, user("x")).blockingLast()).hasMessage("Session busy");
            first.cancel();
        }
    }

    @Test void callStartSnapshotSurvivesClockChangeAndEachCallHasUniqueIdentity() {
        var clock = new MutableClock();
        var ledger = new Ledger();
        var model = new FakeModel(r -> { clock.now = clock.now.plus(Duration.ofDays(1)); return text("OK"); });
        var gateway = new AdkModelGateway(model, ledger, clock);
        var request = LlmRequest.builder().contents(List.of(user("x"))).build();
        gateway.generateContent(request, false).blockingLast();
        gateway.generateContent(request, false).blockingLast();
        assertThat(ledger.calls.getFirst().startedAt()).isEqualTo(Instant.parse("2026-11-01T00:00:00Z"));
        assertThat(ledger.calls.get(1).id()).isNotEqualTo(ledger.calls.getFirst().id());
        assertThat(ledger.completedContexts).containsExactly(ledger.calls.get(0).startedAt(), ledger.calls.get(1).startedAt());
    }

    @Test void providerDeadlineCancelsWithoutChargingOrFallback() {
        var ledger = new Ledger();
        var model = new FakeModel(r -> text("unused")) {
            @Override public Flowable<LlmResponse> generateContent(LlmRequest r, boolean stream) { return Flowable.never(); }
        };
        var gateway = new AdkModelGateway(model, ledger, CLOCK, Duration.ofMillis(40));
        assertThatThrownBy(() -> gateway.generateContent(LlmRequest.builder().build(), false).blockingLast())
                .hasCauseInstanceOf(java.util.concurrent.TimeoutException.class);
        assertThat(ledger.calls).hasSize(1);
        assertThat(ledger.usages).isEmpty();
    }

    static AdkSpikeSession session(Authentication owner, BaseLlm model, Ledger ledger, FixtureTools tools, boolean confirm) {
        String method = confirm ? "create_list" : "read_list";
        return new AdkSpikeSession(owner, new AdkModelGateway(model, ledger, CLOCK),
                List.of(new AdkSpikeSession.Tool(FunctionTool.create(tools, method, confirm), args -> { })), CLOCK);
    }

    static Authentication actor(String id, String role) {
        return UsernamePasswordAuthenticationToken.authenticated(id, null,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role)));
    }
    static Content user(String value) { return Content.fromParts(Part.fromText(value)); }
    static LlmResponse text(String value) { return response(Part.fromText(value)); }
    static LlmResponse call(String name, String id, Map<String, Object> args) {
        return response(Part.builder().functionCall(FunctionCall.builder().name(name).id(id).args(args).build()).build());
    }
    static LlmResponse response(Part part) {
        return LlmResponse.builder().content(Content.builder().role("model").parts(List.of(part)).build())
                .usageMetadata(GenerateContentResponseUsageMetadata.builder().promptTokenCount(2).candidatesTokenCount(1).totalTokenCount(3).build()).build();
    }
    static Content confirmationResponse(String id, boolean approved) {
        return Content.fromParts(Part.builder().functionResponse(FunctionResponse.builder().id(id)
                .name("adk_request_confirmation").response(Map.of("confirmed", approved)).build()).build());
    }

    public static class FixtureTools {
        final AtomicInteger writes = new AtomicInteger();
        final List<String> observedOwners = new CopyOnWriteArrayList<>();
        @Schema(description = "Read an existing fixture list by its real ID")
        public Map<String, Object> read_list(@Schema(name = "listId", description = "Positive list ID") int id) {
            if (id < 1) throw new IllegalArgumentException("Invalid list ID");
            observedOwners.add(SecurityContextHolder.getContext().getAuthentication().getName());
            return Map.of("id", id);
        }
        @Schema(description = "Create a fixture list; always requires confirmation")
        public Map<String, Object> create_list(@Schema(name = "name", description = "Nonblank name") String name) {
            if (name.isBlank()) throw new IllegalArgumentException("Missing list name");
            writes.incrementAndGet();
            return Map.of("id", 42, "name", name);
        }
    }
    static class FakeModel extends BaseLlm {
        final List<LlmRequest> requests = new CopyOnWriteArrayList<>();
        final Function<LlmRequest, LlmResponse> action;
        FakeModel(Function<LlmRequest, LlmResponse> action) { super("gemini-g1-fixture"); this.action = action; }
        @Override public Flowable<LlmResponse> generateContent(LlmRequest request, boolean stream) {
            return Flowable.fromCallable(() -> { requests.add(request); return action.apply(request); });
        }
        @Override public BaseLlmConnection connect(LlmRequest r) { throw new UnsupportedOperationException(); }
    }
    static class Ledger implements AdkModelGateway.Policy {
        final List<AdkModelGateway.Call> calls = new CopyOnWriteArrayList<>();
        final List<AdkModelGateway.Usage> usages = java.util.Collections.synchronizedList(new ArrayList<>());
        final List<Object> completedContexts = new CopyOnWriteArrayList<>();
        @Override public Object beforeCall(AdkModelGateway.Call call) { calls.add(call); return call.startedAt(); }
        @Override public void onUsableResponse(AdkModelGateway.Call call, Object context, AdkModelGateway.Usage usage) {
            completedContexts.add(context); usages.add(usage);
        }
    }
    static class MutableClock extends Clock {
        Instant now = CLOCK.instant();
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
