package tech.mamxanh.integration.ai.knowledge;

import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** Immutable index snapshot; publication validates the complete revision before atomic replacement. */
public record KnowledgeIndex(int schema, String revision, String indexRevision, String corpusHash, Config config,
                             double threshold, Map<String, List<Double>> vectors) {
    public KnowledgeIndex(int schema, String revision, String corpusHash, Config config,
                          double threshold, Map<String, List<Double>> vectors) {
        this(schema, revision, java.util.UUID.randomUUID().toString(), corpusHash, config, threshold, vectors);
    }
    public KnowledgeIndex {
        Objects.requireNonNull(config, "Embedding configuration required");
        var copy = new LinkedHashMap<String, List<Double>>();
        vectors.forEach((id, vector) -> copy.put(id, List.copyOf(vector)));
        vectors = Collections.unmodifiableMap(copy);
    }
    public record Config(String model, int dimension, String inputFormat) {
        public Config {
            if (!Set.of("gemini-embedding-001", "gemini-embedding-2").contains(model)
                    || !Set.of(768, 1536).contains(dimension)
                    || !(model.equals("gemini-embedding-001") ? "task-type-v1" : "search-prefix-v1").equals(inputFormat))
                throw new IllegalArgumentException("Unsupported embedding configuration");
        }
    }
    public KnowledgeIndex validate(KnowledgeCatalog catalog, Config expected) {
        if (schema != 1 || indexRevision == null || indexRevision.isBlank()
                || !catalog.revision().equals(revision) || !catalog.fingerprint().equals(corpusHash)
                || !config.equals(expected) || !Double.isFinite(threshold) || threshold < -1 || threshold > 1.000001
                || !vectors.keySet().equals(new HashSet<>(catalog.chunks().stream().map(KnowledgeCatalog.Chunk::id).toList())))
            throw new IllegalArgumentException("Index/corpus/configuration mismatch");
        vectors.values().forEach(v -> {
            var normalized = normalize(v, config.dimension());
            for (int i = 0; i < v.size(); i++) if (Math.abs(normalized.get(i) - v.get(i)) > 1e-5)
                throw new IllegalArgumentException("Index vectors must be normalized");
        });
        return this;
    }
    public static List<Double> normalize(List<Double> vector, int dimension) {
        if (vector == null || vector.size() != dimension) throw new IllegalArgumentException("Vector dimension mismatch");
        double norm = 0;
        for (Double value : vector) {
            if (value == null || !Double.isFinite(value)) throw new IllegalArgumentException("Non-finite vector");
            norm += value * value;
        }
        if (!Double.isFinite(norm) || norm <= 0) throw new IllegalArgumentException("Invalid vector norm");
        double magnitude = Math.sqrt(norm);
        return vector.stream().map(value -> value / magnitude).toList();
    }
    public static double cosine(List<Double> left, List<Double> right) {
        if (left.size() != right.size()) throw new IllegalArgumentException("Vector dimension mismatch");
        double value = 0;
        for (int i = 0; i < left.size(); i++) value += left.get(i) * right.get(i);
        return value;
    }
    public static Optional<KnowledgeIndex> packaged(KnowledgeCatalog catalog, Config expected) {
        try (var input = KnowledgeIndex.class.getResourceAsStream("/ai-knowledge/index.json")) {
            if (input == null) return Optional.empty();
            return Optional.of(new ObjectMapper().readValue(input.readAllBytes(), KnowledgeIndex.class).validate(catalog, expected));
        } catch (Exception invalid) { return Optional.empty(); }
    }
    public void publish(Path destination, KnowledgeCatalog catalog) throws java.io.IOException {
        validate(catalog, config);
        Path parent = destination.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "knowledge-", ".json");
        try {
            Files.writeString(temporary, new ObjectMapper().writeValueAsString(this));
            new ObjectMapper().readValue(Files.readString(temporary), KnowledgeIndex.class).validate(catalog, config);
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
}
