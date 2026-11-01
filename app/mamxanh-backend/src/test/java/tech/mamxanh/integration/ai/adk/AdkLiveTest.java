package tech.mamxanh.integration.ai.adk;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.mamxanh.integration.ai.adk.AdkSpikeTest.actor;
import static tech.mamxanh.integration.ai.adk.AdkSpikeTest.user;

import com.google.adk.models.Gemini;
import com.google.adk.tools.FunctionTool;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import java.time.Clock;
import java.util.List;
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
                .httpOptions(HttpOptions.builder().timeout(10_000)
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
                System.out.println("G1_LIVE model=" + AiTokenUsage.MODEL + " calls=" + ledger.calls.size()
                        + " usageEvents=" + ledger.usages.size() + " fixtureToolResults=" + tools.observedOwners.size());
            }
        }
    }
}
