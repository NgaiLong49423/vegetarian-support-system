package tech.mamxanh.integration.ai.knowledge;

/** Offline classpath-only verification entry point; no Spring, SQL, credentials or network. */
public final class KnowledgeSmoke {
    private KnowledgeSmoke() { }
    public static void main(String[] args) {
        var catalog = KnowledgeCatalog.packaged();
        try (var in = KnowledgeSmoke.class.getResourceAsStream("/knowledge-base/index.json")) {
            if (in == null) throw new IllegalStateException("Packaged index missing: /knowledge-base/index.json");
            var index = new tools.jackson.databind.ObjectMapper().readValue(in.readAllBytes(), KnowledgeIndex.class);
            index.validate(catalog, index.config());
            try (var retriever = new HybridKnowledgeRetriever(catalog, index, null, java.time.Clock.systemUTC())) {
                if (catalog.documents().size() != 43 || retriever.search("FR-53.9 danh sách rỗng").hits().isEmpty())
                    throw new IllegalStateException("Smoke failed: documents=" + catalog.documents().size());
                System.out.println("KNOWLEDGE_OFFLINE_OK revision=" + catalog.revision() + " documents=" + catalog.documents().size()
                        + " chunks=" + catalog.chunks().size() + " model=" + index.config().model() + " dimension=" + index.config().dimension());
            }
        } catch (java.io.IOException e) { throw new IllegalStateException("Packaged index unreadable", e); }
    }
}
