package tech.mamxanh.integration.ai.knowledge;

import com.google.genai.Client;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** One input per request, no automatic retry, no user quota or transcript dependency. */
public final class GeminiEmbeddingClient implements HybridKnowledgeRetriever.Embedder {
    public interface Observer {
        void beforeCall(KnowledgeIndex.Config config, int utf8Bytes);
        void completed(KnowledgeIndex.Config config, long latencyMillis, Float providerTokens,
                       Integer providerBillableCharacters, String status);
    }
    @FunctionalInterface interface Transport { EmbedContentResponse call(String model, String text, EmbedContentConfig options); }
    private final Transport transport;
    private final long timeoutMillis;
    private final KnowledgeIndex.Config config;
    private final Observer observer;
    public GeminiEmbeddingClient(Client client, KnowledgeIndex.Config config, Observer observer) {
        this(config, observer, (model, text, options) -> client.models.embedContent(model, text, options), java.time.Duration.ofSeconds(10));
    }
    GeminiEmbeddingClient(KnowledgeIndex.Config config, Observer observer, Transport transport, java.time.Duration timeout) {
        if (timeout.isNegative() || timeout.isZero() || timeout.compareTo(java.time.Duration.ofSeconds(10)) > 0)
            throw new IllegalArgumentException("Embedding deadline exceeds policy");
        this.transport = transport; this.config = config; this.observer = observer; this.timeoutMillis = timeout.toMillis();
    }
    @Override public java.util.Optional<KnowledgeIndex.Config> configuration() { return java.util.Optional.of(config); }
    @Override public List<Double> embed(String text, String title, boolean query) {
        if (text == null || text.length() > (query ? 1000 : 2000)) throw new IllegalArgumentException("Embedding input limit");
        String input = config.model().equals("gemini-embedding-2")
                ? (query ? "task: search result | query: " + text : "title: " + title + " | text: " + text) : text;
        var options = EmbedContentConfig.builder().outputDimensionality(config.dimension());
        if (config.model().equals("gemini-embedding-001")) {
            options.taskType(query ? "RETRIEVAL_QUERY" : "RETRIEVAL_DOCUMENT");
            if (!query) options.title(title);
        }
        observer.beforeCall(config, input.getBytes(StandardCharsets.UTF_8).length);
        long start = System.nanoTime();
        try {
            var response = Single.fromCallable(() -> transport.call(config.model(), input, options.build()))
                    .subscribeOn(Schedulers.io()).timeout(timeoutMillis, TimeUnit.MILLISECONDS).blockingGet();
            var embeddings = response.embeddings().orElseThrow();
            if (embeddings.size() != 1) throw new IllegalArgumentException("Expected single embedding");
            var embedding = embeddings.getFirst();
            if (embedding.statistics().flatMap(s -> s.truncated()).orElse(false)) throw new IllegalArgumentException("Truncated embedding");
            var result = KnowledgeIndex.normalize(embedding.values().orElseThrow().stream().map(Float::doubleValue).toList(), config.dimension());
            observer.completed(config, (System.nanoTime() - start) / 1_000_000,
                    embedding.statistics().flatMap(s -> s.tokenCount()).orElse(null),
                    response.metadata().flatMap(m -> m.billableCharacterCount()).orElse(null), "OK");
            return result;
        } catch (RuntimeException failure) {
            observer.completed(config, (System.nanoTime() - start) / 1_000_000, null, null, "ERROR");
            // Do not leak provider errors or input text into tools/telemetry.
            throw new IllegalStateException("Embedding unavailable");
        }
    }
}
