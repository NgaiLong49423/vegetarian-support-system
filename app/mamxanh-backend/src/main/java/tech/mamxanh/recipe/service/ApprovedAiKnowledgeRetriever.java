package tech.mamxanh.recipe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.mamxanh.integration.ai.knowledge.HybridKnowledgeRetriever;
import tech.mamxanh.integration.ai.knowledge.KnowledgeCatalog;
import tech.mamxanh.integration.ai.knowledge.KnowledgeIndex;
import tech.mamxanh.recipe.dto.response.AiChatResponse.Source;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.text.Normalizer;
import java.time.Clock;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Retrieves allowlisted, versioned, source-attributed knowledge snippets approved for the AI assistant.
 * Powered by HybridKnowledgeRetriever (BM25 + Precomputed Vector Index with graceful fallback).
 */
@Component
public class ApprovedAiKnowledgeRetriever {
    private static final Logger LOG = LoggerFactory.getLogger(ApprovedAiKnowledgeRetriever.class);
    private static final Pattern WORDS = Pattern.compile("[\\p{L}\\p{N}]{3,}");

    private final KnowledgeCatalog catalog;
    private final KnowledgeIndex index;
    private final HybridKnowledgeRetriever.Embedder embedder;
    private final Clock clock;

    public ApprovedAiKnowledgeRetriever() {
        this(KnowledgeCatalog.packaged(), loadOptionalIndex(), null, Clock.systemUTC());
    }

    @Autowired
    public ApprovedAiKnowledgeRetriever(
            @Autowired(required = false) HybridKnowledgeRetriever.Embedder embedder,
            @Autowired(required = false) Clock clock) {
        this(KnowledgeCatalog.packaged(), loadOptionalIndex(), embedder, clock != null ? clock : Clock.systemUTC());
    }

    public ApprovedAiKnowledgeRetriever(KnowledgeCatalog catalog, KnowledgeIndex index,
                                        HybridKnowledgeRetriever.Embedder embedder, Clock clock) {
        this.catalog = catalog != null ? catalog : KnowledgeCatalog.packaged();
        this.index = index;
        this.embedder = embedder;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public List<Source> retrieve(String query) {
        if (query == null || query.isBlank() || query.length() > 1000) {
            return List.of();
        }
        try (var retriever = new HybridKnowledgeRetriever(catalog, index, embedder, clock)) {
            var searchResult = retriever.search(query.strip());
            if (searchResult.hits().isEmpty()) {
                return List.of();
            }
            List<String> ordered = WORDS.matcher(fold(query)).results().map(java.util.regex.MatchResult::group).toList();
            Set<String> queryTerms = Set.copyOf(ordered);
            if (queryTerms.isEmpty()) {
                return List.of();
            }

            var filteredHits = searchResult.hits().stream().filter(hit -> {
                String foldedDoc = fold(hit.title() + " " + hit.excerpt());
                List<String> evidenceWords = WORDS.matcher(foldedDoc).results().map(java.util.regex.MatchResult::group).toList();
                Set<String> evidenceTerms = Set.copyOf(evidenceWords);
                long overlap = queryTerms.stream().filter(evidenceTerms::contains).count();
                String evidence = String.join(" ", evidenceWords);
                boolean phrase = ordered.size() <= 1 || java.util.stream.IntStream.range(1, ordered.size())
                        .anyMatch(i -> evidence.contains(ordered.get(i - 1) + " " + ordered.get(i)));
                return phrase && overlap >= Math.max(2, Math.ceil(queryTerms.size() * 0.5));
            }).toList();

            if (filteredHits.isEmpty()) {
                return List.of();
            }

            return filteredHits.stream()
                    .limit(3)
                    .map(hit -> new Source(
                            "APPROVED_DOCUMENT",
                            hit.knowledgeId(),
                            hit.title(),
                            hit.excerpt(),
                            hit.sources() != null && !hit.sources().isEmpty() ? hit.sources().getFirst() : "knowledge-base/" + hit.knowledgeId() + ".md",
                            hit.version()))
                    .toList();
        } catch (Exception e) {
            LOG.warn("Knowledge retrieval failed for query: {}", e.getMessage());
            return List.of();
        }
    }

    private static String fold(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replace('đ', 'd');
    }

    private static KnowledgeIndex loadOptionalIndex() {
        try (InputStream stream = ApprovedAiKnowledgeRetriever.class.getResourceAsStream("/knowledge-base/index.json")) {
            if (stream == null) return null;
            KnowledgeIndex index = new ObjectMapper().readValue(stream.readAllBytes(), KnowledgeIndex.class);
            return index.validate(KnowledgeCatalog.packaged(), index.config());
        } catch (Exception e) {
            LOG.debug("Optional vector index unavailable or unvalidated; falling back to BM25: {}", e.getMessage());
            return null;
        }
    }
}
