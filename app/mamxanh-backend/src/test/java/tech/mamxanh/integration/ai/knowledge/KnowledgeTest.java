package tech.mamxanh.integration.ai.knowledge;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

class KnowledgeTest {
    private final KnowledgeCatalog catalog = KnowledgeCatalog.packaged();
    static final KnowledgeIndex.Config CONFIG = new KnowledgeIndex.Config("gemini-embedding-001", 768, "task-type-v1");
    static List<Double> vector(int position) {
        var v = new ArrayList<>(Collections.nCopies(768, 0.0)); v.set(position, 1.0); return v;
    }
    KnowledgeIndex index() {
        var vectors = new LinkedHashMap<String, List<Double>>();
        for (int i = 0; i < catalog.chunks().size(); i++) vectors.put(catalog.chunks().get(i).id(), vector(i % 5));
        return new KnowledgeIndex(1, catalog.revision(), catalog.fingerprint(), CONFIG, .5, vectors);
    }
    @Test void catalogHasFiveDerivedAllowlistedPlaybooksAndStableChunks() {
        assertThat(catalog.documents()).hasSize(43);
        assertThat(catalog.chunks()).isNotEmpty().allSatisfy(c -> {
            assertThat(c.text().length()).isLessThanOrEqualTo(2000);
            assertThat(c.hash()).isEqualTo(KnowledgeCatalog.hash(c.text()));
        });
        assertThat(catalog.fingerprint()).isEqualTo(KnowledgeCatalog.packaged().fingerprint());
        assertThatThrownBy(() -> catalog.document("../application.properties")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void windowsCheckoutLineEndingsPreserveCorpusAndIndexCompatibility() {
        var manifest=new KnowledgeCatalog.Manifest(catalog.revision(),catalog.documents().stream().map(KnowledgeCatalog.Document::metadata).toList());
        var windows=KnowledgeCatalog.load(path->path.equals("catalog.json")?new ObjectMapper().writeValueAsString(manifest)
                :catalog.documents().stream().filter(d->d.metadata().file().equals(path)).findFirst().orElseThrow().content().replace("\n","\r\n"));
        assertThat(windows.fingerprint()).isEqualTo(catalog.fingerprint());
        assertThat(windows.chunks()).isEqualTo(catalog.chunks());
        assertThatCode(()->index().validate(windows,CONFIG)).doesNotThrowAnyException();
    }
    @Test void catalogRejectsTraversalDuplicateIdsAndUnregisteredStatuses() {
        String json = new ObjectMapper().writeValueAsString(new KnowledgeCatalog.Manifest("r", catalog.documents().stream().map(KnowledgeCatalog.Document::metadata).toList()));
        for (String bad : List.of(json.replace("assistant-scope.md", "../secret.md"), json.replace("execution-confirmation\"", "assistant-scope\""), json.replace("APPROVED", "UNKNOWN_STATUS"))) {
            assertThatThrownBy(() -> KnowledgeCatalog.load(p -> bad)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void retiredSourcesAreNotReadOrServed() {
        var entry = catalog.documents().getFirst().metadata();
        var retired = new KnowledgeCatalog.Entry(entry.id(), entry.title(), entry.file(), "2", "RETIRED", true, entry.sources(), entry.tools());
        String json = new ObjectMapper().writeValueAsString(new KnowledgeCatalog.Manifest("r2", List.of(retired)));
        var loaded = KnowledgeCatalog.load(p -> { assertThat(p).isEqualTo("catalog.json"); return json; });
        assertThat(loaded.documents()).isEmpty(); assertThat(loaded.chunks()).isEmpty();
    }
    @Test void chunkingRetainsHeadingAndUnicodeAcrossLongParagraphs() {
        String text = "# Tiếng Việt\n\n" + "Ăn chay 🥗 ".repeat(700);
        var chunks = KnowledgeCatalog.chunk(catalog.documents().getFirst().metadata(), text);
        assertThat(chunks).hasSizeGreaterThan(3).allSatisfy(c -> {
            assertThat(c.text()).startsWith("# Tiếng Việt");
            assertThat(c.text().length()).isLessThanOrEqualTo(2000);
            assertThat(Character.isHighSurrogate(c.text().charAt(c.text().length()-1))).isFalse();
        });
    }
    @Test void rejectsOversizedDocuments() {
        var manifest = new KnowledgeCatalog.Manifest("r", List.of(catalog.documents().getFirst().metadata()));
        assertThatThrownBy(() -> KnowledgeCatalog.load(p -> p.equals("catalog.json") ? new ObjectMapper().writeValueAsString(manifest) : "x".repeat(12001)))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsInvalidVectorsAndSpaces() {
        for (var values : List.of(List.of(1.0), Collections.nCopies(768, 0.0), Collections.nCopies(768, Double.NaN), Collections.nCopies(768, Double.POSITIVE_INFINITY)))
            assertThatThrownBy(() -> KnowledgeIndex.normalize(values, 768)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index().validate(catalog, new KnowledgeIndex.Config("gemini-embedding-2", 768, "search-prefix-v1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(KnowledgeIndex.cosine(vector(0), vector(0))).isEqualTo(1);
    }
    @Test void corruptOrStaleIndexCannotPublishAndAtomicRollbackRestoresSnapshot(@TempDir Path temp) throws Exception {
        Path path = temp.resolve("index.json"); var original = index(); original.publish(path, catalog);
        String before = Files.readString(path);
        var bad = new KnowledgeIndex(1, "stale", original.corpusHash(), CONFIG, .5, original.vectors());
        assertThatThrownBy(() -> bad.publish(path, catalog)).isInstanceOf(IllegalArgumentException.class);
        assertThat(Files.readString(path)).isEqualTo(before);
        var update = new KnowledgeIndex(1, original.revision(), original.corpusHash(), CONFIG, .7, original.vectors());
        update.publish(path, catalog); assertThat(Files.readString(path)).isNotEqualTo(before);
        original.publish(path, catalog); assertThat(Files.readString(path)).isEqualTo(before);
        assertThatThrownBy(() -> new ObjectMapper().readValue("broken", KnowledgeIndex.class)).isInstanceOf(RuntimeException.class);
    }
    @Test void bm25PreservesIdentifiersAndFoldsVietnameseAccents() {
        try (var retriever = new HybridKnowledgeRetriever(catalog, null, null, Clock.systemUTC())) {
            assertThat(retriever.search("FR-53.9").hits().getFirst().knowledgeId()).isEqualTo("shopping-create");
            assertThat(HybridKnowledgeRetriever.frequencies("ĐỌC FR-53.9 get_shopping_list")).containsKeys("doc", "fr-53.9", "get_shopping_list");
            assertThat(retriever.search("tao danh sach rong").status()).isEqualTo("keyword_degraded");
            assertThatThrownBy(() -> retriever.search("x".repeat(1001))).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void equalWeightRrfUsesRankNotRawScores() {
        var scores = new HashMap<String, Double>();
        HybridKnowledgeRetriever.addRrf(scores, List.of("a", "b"));
        HybridKnowledgeRetriever.addRrf(scores, List.of("b", "a"));
        assertThat(scores.get("a")).isEqualTo(scores.get("b")).isCloseTo(1.0/61+1.0/62, within(1e-10));
    }
    @Test void hybridDeduplicatesDocumentsAndCacheIsSessionScoped() {
        var calls = new AtomicInteger();
        try (var retriever = new HybridKnowledgeRetriever(catalog, index(), (q,t,b) -> { calls.incrementAndGet(); return vector(0); }, Clock.systemUTC())) {
            var result = retriever.search("FR-53.9");
            assertThat(result.status()).isEqualTo("hybrid");
            assertThat(result.hits()).hasSizeLessThanOrEqualTo(3).extracting(HybridKnowledgeRetriever.Hit::knowledgeId).doesNotHaveDuplicates();
            assertThat(result.hits()).allSatisfy(h -> assertThat(h.sources()).isNotEmpty());
            retriever.search("FR-53.9"); assertThat(calls).hasValue(1);
        }
    }
    @Test void errorsDegradeWithoutRetryOrChangingProvider() {
        var calls = new AtomicInteger();
        try (var retriever = new HybridKnowledgeRetriever(catalog, index(), (q,t,b) -> { calls.incrementAndGet(); throw new IllegalStateException("timeout"); }, Clock.systemUTC())) {
            var result = retriever.search("FR-53.9");
            assertThat(result.status()).isEqualTo("keyword_degraded");
            assertThat(result.reason()).isEqualTo("EMBEDDING_UNAVAILABLE"); assertThat(calls).hasValue(1);
        }
    }
    @Test void unsupportedSemanticQueryReturnsNoMatch() {
        try (var retriever = new HybridKnowledgeRetriever(catalog, index(), (q,t,b) -> vector(700), Clock.systemUTC())) {
            assertThat(retriever.search("weather").status()).isEqualTo("no_match");
        }
    }
    @Test void cacheExpiresAndCloseClearsAccess() {
        var now = new java.util.concurrent.atomic.AtomicReference<>(Instant.now());
        var clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; } public Clock withZone(ZoneId z) { return this; } public Instant instant() { return now.get(); }
        };
        var retriever = new HybridKnowledgeRetriever(catalog, index(), (q,t,b) -> vector(0), clock);
        retriever.search("FR-53.9"); now.set(now.get().plusSeconds(1801));
        assertThatThrownBy(() -> retriever.search("FR-53.9")).isInstanceOf(IllegalStateException.class);
        retriever.close(); assertThatThrownBy(() -> retriever.get("shopping-create")).isInstanceOf(IllegalStateException.class);
    }
    @Test void activeBackendSessionCanRenewKnowledgeIdleExpiryWithoutRevivingAnExpiredSession() {
        var now = new java.util.concurrent.atomic.AtomicReference<>(Instant.now());
        var clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return now.get(); }
        };
        try (var retriever = new HybridKnowledgeRetriever(catalog, null, null, clock)) {
            now.set(now.get().plusSeconds(1200));
            retriever.touch();
            now.set(now.get().plusSeconds(1200));
            assertThat(retriever.get("shopping-create")).isNotNull();
            now.set(now.get().plusSeconds(601));
            assertThatThrownBy(retriever::touch).isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void cumulativeRunBudgetRejectsOverflowAndResetsOnNextRun() {
        var tools = new KnowledgeTools(new HybridKnowledgeRetriever(catalog, null, null, Clock.systemUTC()));
        tools.charge("one", 24000);
        assertThatThrownBy(() -> tools.charge("one", 1)).isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> tools.charge("two", 24000)).doesNotThrowAnyException();
    }
    @Test void changedSourceInvalidatesOldIndexUntilExplicitRebuild(@TempDir Path temporary) throws Exception {
        var manifest=new KnowledgeCatalog.Manifest("g2-pilot-2",catalog.documents().stream().map(KnowledgeCatalog.Document::metadata).toList());
        var updated=KnowledgeCatalog.load(path->path.equals("catalog.json")?new ObjectMapper().writeValueAsString(manifest)
                :catalog.documents().stream().filter(d->d.metadata().file().equals(path)).findFirst().orElseThrow().content()+"\n\nNội dung cập nhật.");
        assertThatThrownBy(()->index().validate(updated,CONFIG)).isInstanceOf(IllegalArgumentException.class);
        var vectors=new LinkedHashMap<String,List<Double>>(); updated.chunks().forEach(c->vectors.put(c.id(),vector(0)));
        var rebuilt=new KnowledgeIndex(1,updated.revision(),updated.fingerprint(),CONFIG,.5,vectors);
        rebuilt.publish(temporary.resolve("updated.json"),updated);
        assertThat(rebuilt.indexRevision()).isNotEqualTo(index().indexRevision());
        try(var retriever=new HybridKnowledgeRetriever(updated,index(),(q,t,b)->{throw new AssertionError("Stale index must not call embedding");},Clock.systemUTC())) {
            assertThat(retriever.search("FR-53.9").status()).isEqualTo("keyword_degraded");
        }
    }
    @Test void embeddingConfigurationMismatchDisablesSemanticBeforeNetwork() {
        var wrongSpace=new HybridKnowledgeRetriever.Embedder() {
            public List<Double> embed(String q,String t,boolean query) {throw new AssertionError("Wrong space must not be called");}
            public Optional<KnowledgeIndex.Config> configuration() {return Optional.of(new KnowledgeIndex.Config("gemini-embedding-2",768,"search-prefix-v1"));}
        };
        try(var retriever=new HybridKnowledgeRetriever(catalog,index(),wrongSpace,Clock.systemUTC())) {
            assertThat(retriever.search("FR-53.9").status()).isEqualTo("keyword_degraded");
        }
    }
    @Test void missingOrUnnormalizedIndexVectorsAreRejected() {
        var original=index(); var missing=new LinkedHashMap<>(original.vectors()); missing.remove(missing.keySet().iterator().next());
        assertThatThrownBy(()->new KnowledgeIndex(1,catalog.revision(),catalog.fingerprint(),CONFIG,.5,missing).validate(catalog,CONFIG)).isInstanceOf(IllegalArgumentException.class);
        var invalid=new LinkedHashMap<>(original.vectors());invalid.put(invalid.keySet().iterator().next(),vector(0).stream().map(v->v*2).toList());
        assertThatThrownBy(()->new KnowledgeIndex(1,catalog.revision(),catalog.fingerprint(),CONFIG,.5,invalid).validate(catalog,CONFIG)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->original.vectors().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
