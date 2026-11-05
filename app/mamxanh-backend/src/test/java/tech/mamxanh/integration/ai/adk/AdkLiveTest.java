package tech.mamxanh.integration.ai.adk;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.mamxanh.integration.ai.adk.AdkSpikeTest.actor;
import static tech.mamxanh.integration.ai.adk.AdkSpikeTest.user;

import com.google.adk.models.Gemini;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.adk.tools.FunctionTool;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.Part;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tech.mamxanh.integration.ai.AiTokenUsage;

/** Explicitly opt in. Fixture data only; no account, SQL, wallet or existing chat is touched. */
@EnabledIfEnvironmentVariable(named = "MAMXANH_G1_LIVE", matches = "true")
class AdkLiveTest {
    @Test void geminiUsesTheNativeToolLoopAndKeepsTheSession() {
        String key = System.getenv("GEMINI_API_KEY");
        assertThat(key != null && !key.isBlank()).as("Backend Gemini credential must be configured").isTrue();
        var owner = actor("7", "CUSTOMER");
        var ledger = new AdkSpikeTest.Ledger();
        var tools = new AdkSpikeTest.FixtureTools();
        try (var client = Client.builder().apiKey(key)
                .httpOptions(HttpOptions.builder().timeout(30_000)
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build()) {
            var gateway = new AdkModelGateway(new Gemini(AiTokenUsage.MODEL, client), ledger, Clock.systemUTC());
            try (var session = new AdkSpikeSession(owner, gateway,
                    List.of(new AdkSpikeSession.Tool(FunctionTool.create(tools, "read_list"), args -> { })), Clock.systemUTC())) {
                var result = session.run(owner, user("Call read_list with listId 42 and report the returned ID. This is synthetic test data."))
                        .toList().blockingGet();
                assertThat(tools.observedOwners).contains("7");
                assertThat(result.stream().anyMatch(event -> !event.functionResponses().isEmpty())).isTrue();
                assertThat(ledger.calls.size()).isBetween(2, 6);
                assertThat(ledger.usages).hasSize(ledger.calls.size());
                var followup = session.run(owner, user("Which list ID did the previous tool return? Reply briefly."))
                        .toList().blockingGet();
                assertThat(followup.stream().filter(event -> event.finalResponse()).map(event -> event.stringifyContent()))
                        .anyMatch(answer -> answer.contains("42"));
                // Only aggregate non-sensitive metrics are printed, never prompts/provider payloads.
                long inputTokens = ledger.usages.stream().map(AdkModelGateway.Usage::metadata)
                        .filter(java.util.Objects::nonNull).map(AiTokenUsage::promptTokens)
                        .filter(java.util.Objects::nonNull).mapToLong(Integer::longValue).sum();
                long outputTokens = ledger.usages.stream().map(AdkModelGateway.Usage::metadata)
                        .filter(java.util.Objects::nonNull).map(AiTokenUsage::responseTokens)
                        .filter(java.util.Objects::nonNull).mapToLong(Integer::longValue).sum();
                System.out.println("G1_LIVE model=" + AiTokenUsage.MODEL + " calls=" + ledger.calls.size()
                        + " usageEvents=" + ledger.usages.size() + " inputTokens=" + inputTokens
                        + " outputTokens=" + outputTokens + " fixtureToolResults=" + tools.observedOwners.size());
            }
        }
    }

    @Test void geminiSdkProducesRealPartialAndFinalResponsesWithinOneAbsoluteDeadline() {
        String key = System.getenv("GEMINI_API_KEY");
        assertThat(key != null && !key.isBlank()).as("Backend Gemini credential must be configured").isTrue();
        var ledger = new AdkSpikeTest.Ledger();
        long started = System.nanoTime();
        AtomicLong firstPartialNanos = new AtomicLong();
        try (var client = Client.builder().apiKey(key)
                .httpOptions(HttpOptions.builder().timeout(30_000)
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build()) {
            var gateway = new AdkModelGateway(new Gemini(AiTokenUsage.MODEL, client), ledger, Clock.systemUTC());
            var request = LlmRequest.builder().model(AiTokenUsage.MODEL)
                    .contents(List.of(Content.fromParts(Part.fromText(
                            "In two short sentences, explain one vegan egg substitute for cake and a typical ratio. Synthetic test only."))))
                    // Gemini 3.8 Flash may spend part of this budget on thinking before emitting
                    // its final response; 256 tokens can terminate with MAX_TOKENS after a partial.
                    .config(GenerateContentConfig.builder().candidateCount(1).maxOutputTokens(2048).build()).build();
            List<LlmResponse> responses = gateway.generateContent(request, true)
                    .doOnNext(response -> {
                        if (response.partial().orElse(false)) firstPartialNanos.compareAndSet(0, System.nanoTime());
                    }).toList().blockingGet();
            assertThat(responses).anyMatch(response -> response.partial().orElse(false));
            assertThat(responses.getLast().partial()).isEmpty();
            assertThat(ledger.usages).hasSize(1);
            long inputTokens = ledger.usages.stream().map(AdkModelGateway.Usage::metadata)
                    .filter(java.util.Objects::nonNull).map(AiTokenUsage::promptTokens)
                    .filter(java.util.Objects::nonNull).mapToLong(Integer::longValue).sum();
            long outputTokens = ledger.usages.stream().map(AdkModelGateway.Usage::metadata)
                    .filter(java.util.Objects::nonNull).map(AiTokenUsage::responseTokens)
                    .filter(java.util.Objects::nonNull).mapToLong(Integer::longValue).sum();
            long ttftMs = firstPartialNanos.get() == 0 ? -1
                    : TimeUnit.NANOSECONDS.toMillis(firstPartialNanos.get() - started);
            System.out.println("GEMINI_STREAM_LIVE model=" + AiTokenUsage.MODEL + " calls=1 ttftMs=" + ttftMs
                    + " totalModelMs=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
                    + " inputTokens=" + inputTokens + " outputTokens=" + outputTokens
                    + " partialResponses=" + responses.stream().filter(r -> r.partial().orElse(false)).count());
        }
    }
}
