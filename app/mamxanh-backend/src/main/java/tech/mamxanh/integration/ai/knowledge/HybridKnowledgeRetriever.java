package tech.mamxanh.integration.ai.knowledge;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Session-scoped retriever. Query text exists only in RAM until expiry/close. */
public final class HybridKnowledgeRetriever implements AutoCloseable {
    @FunctionalInterface public interface Embedder {
        List<Double> embed(String text, String title, boolean query);
        default Optional<KnowledgeIndex.Config> configuration() { return Optional.empty(); }
    }
    public record Hit(String knowledgeId, String title, String summary, String excerpt,
                      String version, List<String> sources) { }
    public record Result(String status, String reason, List<Hit> hits) { }
    private record Cached(List<Double> vector, Instant expires) { }
    private final KnowledgeCatalog catalog;
    private final KnowledgeIndex index;
    private final Embedder embedder;
    private final java.util.function.Consumer<String> embeddingGuard;
    private final Clock clock;
    private Instant expires;
    private boolean closed;
    private final Map<String, Cached> cache = new HashMap<>();
    private final List<Map<String, Long>> terms;

    public HybridKnowledgeRetriever(KnowledgeCatalog catalog, KnowledgeIndex index, Embedder embedder, Clock clock) {
        this(catalog, index, embedder, query -> { }, clock);
    }

    public HybridKnowledgeRetriever(KnowledgeCatalog catalog, KnowledgeIndex index, Embedder embedder,
            java.util.function.Consumer<String> embeddingGuard, Clock clock) {
        this.catalog = catalog;
        this.embedder = embedder;
        this.embeddingGuard = embeddingGuard == null ? query -> { } : embeddingGuard;
        this.clock = clock;
        KnowledgeIndex valid = null;
        try { if (index != null) valid = index.validate(catalog,
                embedder == null ? index.config() : embedder.configuration().orElse(index.config())); }
        catch (IllegalArgumentException ignored) { /* Current corpus remains usable in degraded mode. */ }
        this.index = valid;
        expires = clock.instant().plus(Duration.ofMinutes(30));
        terms = catalog.chunks().stream().map(c -> frequencies(c.text())).toList();
    }

    public synchronized Result search(String query) {
        return search(query, false);
    }

    public synchronized Result search(String query, boolean userFacing) {
        checkSession();
        validateQuery(query);
        var keyword = keyword(query);
        if (index == null || embedder == null) return result(keyword, "keyword_degraded", "INDEX_UNAVAILABLE", userFacing);
        try {
            cache.entrySet().removeIf(e -> !e.getValue().expires().isAfter(clock.instant()));
            String key = KnowledgeCatalog.hash(query);
            Cached cached = cache.get(key);
            List<Double> vector;
            if (cached == null) {
                embeddingGuard.accept(query);
                vector = KnowledgeIndex.normalize(embedder.embed(query, "", true), index.config().dimension());
                if (cache.size() >= 128) cache.clear();
                cache.put(key, new Cached(vector, expires));
            } else vector = cached.vector();
            var semantic = semantic(vector, index.threshold());
            // Calibrated semantic gate rejects unsupported queries even if a generic word overlaps.
            if (semantic.isEmpty()) return new Result("no_match", "BELOW_SEMANTIC_THRESHOLD", List.of());
            var scores = new HashMap<String, Double>();
            addRrf(scores, keyword); addRrf(scores, semantic);
            return result(sorted(scores), "hybrid", "OK", userFacing);
        } catch (SecurityException unconsented) {
            throw unconsented;
        } catch (RuntimeException unavailable) {
            return result(keyword, "keyword_degraded", "EMBEDDING_UNAVAILABLE", userFacing);
        }
    }

    public List<String> keyword(String query) {
        validateQuery(query);
        var queryTerms = frequencies(query);
        var scores = new HashMap<String, Double>();
        double average = Math.max(1, terms.stream().mapToLong(t -> t.values().stream().mapToLong(Long::longValue).sum()).average().orElse(1));
        for (int i = 0; i < terms.size(); i++) {
            var doc = terms.get(i);
            double length = doc.values().stream().mapToLong(Long::longValue).sum(), score = 0;
            for (String term : queryTerms.keySet()) {
                long count = terms.stream().filter(t -> t.containsKey(term)).count();
                double tf = doc.getOrDefault(term, 0L);
                score += Math.log(1 + (terms.size() - count + .5) / (count + .5))
                        * tf * 2.2 / (tf + 1.2 * (.25 + .75 * length / average));
            }
            if (score > 0) scores.put(catalog.chunks().get(i).id(), score);
        }
        return sorted(scores);
    }
    public List<String> semantic(List<Double> vector, double threshold) {
        if (index == null) return List.of();
        var normalized = KnowledgeIndex.normalize(vector, index.config().dimension());
        var scores = new HashMap<String, Double>();
        index.vectors().forEach((id, value) -> {
            double score = KnowledgeIndex.cosine(normalized, value);
            if (score >= threshold) scores.put(id, score);
        });
        return sorted(scores);
    }
    public static void addRrf(Map<String, Double> scores, List<String> ranking) {
        for (int i = 0; i < ranking.size(); i++) scores.merge(ranking.get(i), 1.0 / (60 + i + 1), Double::sum);
    }
    private static List<String> sorted(Map<String, Double> scores) {
        return scores.keySet().stream().sorted(Comparator.<String>comparingDouble(scores::get).reversed().thenComparing(s -> s)).toList();
    }

    public Result result(List<String> ranking, String status, String reason) {
        return result(ranking, status, reason, false);
    }

    public Result result(List<String> ranking, String status, String reason, boolean userFacing) {
        var hits = new ArrayList<Hit>(); var seen = new HashSet<String>();
        for (String chunkId : ranking) {
            var chunk = catalog.chunks().stream().filter(c -> c.id().equals(chunkId)).findFirst().orElseThrow();
            if (!seen.add(chunk.documentId())) continue;
            var doc = catalog.document(chunk.documentId());
            if (userFacing && doc.metadata().isAgentOnly()) continue;
            String excerpt = chunk.text().substring(0, Math.min(650, chunk.text().length()));
            hits.add(new Hit(chunk.documentId(), doc.metadata().title(), doc.metadata().title(), excerpt,
                    doc.metadata().version(), doc.metadata().sources()));
            if (hits.size() == 3) break;
        }
        return new Result(hits.isEmpty() && !status.equals("keyword_degraded") ? "no_match" : status, reason, List.copyOf(hits));
    }

    public synchronized KnowledgeCatalog.Document get(String id) {
        return get(id, false);
    }

    public synchronized KnowledgeCatalog.Document get(String id, boolean userFacing) {
        checkSession();
        var doc = catalog.document(id);
        if (userFacing && doc.metadata().isAgentOnly()) {
            throw new SecurityException("Access to agent-only knowledge denied for user-facing request: " + id);
        }
        return doc;
    }

    /** Called only after the owning Backend session has validated activity and ownership. */
    public synchronized void touch() {
        checkSession();
        expires = clock.instant().plus(Duration.ofMinutes(30));
    }
    private void checkSession() {
        if (closed || !expires.isAfter(clock.instant())) { cache.clear(); throw new IllegalStateException("Knowledge session expired"); }
    }
    static void validateQuery(String query) {
        if (query == null || query.isBlank() || query.length() > 1000) throw new IllegalArgumentException("Query must contain 1..1000 characters");
    }
    static Map<String, Long> frequencies(String text) {
        String folded = Normalizer.normalize(text.toLowerCase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return Arrays.stream(folded.split("[^\\p{L}\\p{N}_.-]+"))
                .filter(t -> !t.isBlank()).collect(Collectors.groupingBy(t -> t, Collectors.counting()));
    }
    @Override public synchronized void close() { closed = true; cache.clear(); }
}
