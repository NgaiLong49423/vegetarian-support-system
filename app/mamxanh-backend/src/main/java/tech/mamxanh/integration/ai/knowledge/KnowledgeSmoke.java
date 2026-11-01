package tech.mamxanh.integration.ai.knowledge;

/** Offline classpath-only verification entry point; no Spring, SQL, credentials or network. */
public final class KnowledgeSmoke {
    private KnowledgeSmoke() { }
    public static void main(String[] args) {
        boolean g3 = args.length == 1 && "g3".equals(args[0]);
        boolean agent = args.length == 1 && "agent".equals(args[0]);
        if (args.length > 0 && !g3 && !agent) throw new IllegalArgumentException("Expected no argument, g3 or agent");
        var catalog = agent ? KnowledgeCatalog.agent() : g3 ? KnowledgeCatalog.packagedG3() : KnowledgeCatalog.packaged();
        try (var in = KnowledgeSmoke.class.getResourceAsStream(agent ? "/ai-knowledge/agent-index.json" : g3 ? "/ai-knowledge/g3/index.json" : "/ai-knowledge/index.json")) {
            if (in == null) throw new IllegalStateException("Packaged index missing");
            var index = new tools.jackson.databind.ObjectMapper().readValue(in.readAllBytes(), KnowledgeIndex.class);
            index.validate(catalog, index.config());
            try (var retriever = new HybridKnowledgeRetriever(catalog, index, null, java.time.Clock.systemUTC())) {
                if (catalog.documents().size() != (agent ? 43 : 5) || retriever.search("FR-53.9 danh sách rỗng").hits().isEmpty()) throw new IllegalStateException("Smoke failed");
                System.out.println((agent ? "AGENT" : g3 ? "G3" : "G2") + "_OFFLINE_OK revision=" + catalog.revision() + " documents=" + catalog.documents().size()
                        + " chunks=" + catalog.chunks().size() + " model=" + index.config().model() + " dimension=" + index.config().dimension());
            }
        } catch (java.io.IOException e) { throw new IllegalStateException("Packaged index unreadable", e); }
    }
}
