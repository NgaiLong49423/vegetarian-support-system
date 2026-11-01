package tech.mamxanh.integration.ai.knowledge;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Function;

/** Allowlisted, derived pilot material. Never scans the repository or accepts user paths. */
public record KnowledgeCatalog(String revision, List<Document> documents, List<Chunk> chunks) {
    public KnowledgeCatalog {
        documents = List.copyOf(documents); chunks = List.copyOf(chunks);
    }
    public record Entry(String id, String title, String file, String version, String status,
                        boolean enabledForPilot, List<String> sources, List<String> tools,
                        String retrievalPolicy, List<String> accessScopes) {
        public Entry {
            sources = sources == null ? null : List.copyOf(sources);
            tools = tools == null ? null : List.copyOf(tools);
            accessScopes = accessScopes == null ? List.of("CUSTOMER", "EXPERT") : List.copyOf(accessScopes);
            retrievalPolicy = retrievalPolicy == null ? "BOTH" : retrievalPolicy;
        }

        public Entry(String id, String title, String file, String version, String status,
                     boolean enabledForPilot, List<String> sources, List<String> tools) {
            this(id, title, file, version, status, enabledForPilot, sources, tools, "BOTH", List.of("CUSTOMER", "EXPERT"));
        }

        public boolean isAgentOnly() {
            return "AGENT_ONLY".equalsIgnoreCase(retrievalPolicy);
        }

        public boolean enabled() {
            return enabledForPilot;
        }
    }
    public record Manifest(String revision, List<Entry> documents) { }
    public record Document(Entry metadata, String content) { }
    public record Chunk(String id, String documentId, String version, String hash, String text) { }

    /** Load unified catalog from knowledge-base classpath resource. */
    public static KnowledgeCatalog packaged() {
        var mapper = new ObjectMapper();
        var manifest = mapper.readTree(businessResource("catalog.json"));
        String revision = manifest.path("revision").asText();
        if (revision == null || revision.isBlank()) {
            revision = "kb-v" + manifest.path("version").asText() + "-20261101";
        }
        String version = manifest.path("version").asText();
        if (version.isBlank()) throw new IllegalArgumentException("Missing catalog version");
        var entries = new ArrayList<Entry>();
        var content = new HashMap<String, String>();
        for (var node : manifest.path("documents")) {
            String id = node.path("id").asText();
            String path = node.path("resourcePath").asText();
            if (!path.matches("knowledge-base/[a-z-]+/[a-z-]+\\.md"))
                throw new IllegalArgumentException("Invalid resource path: " + path);
            String file = id + ".md";
            String rawDoc = businessResource(path.substring("knowledge-base/".length()));
            if (content.putIfAbsent(file, tech.mamxanh.integration.ai.AiPromptLoader.stripFrontMatter(rawDoc)) != null)
                throw new IllegalArgumentException("Duplicate knowledge ID: " + id);

            List<String> sources = new ArrayList<>();
            if (node.has("sources") && node.get("sources").isArray()) {
                node.get("sources").forEach(s -> sources.add(s.asText()));
            } else {
                sources.add(path);
            }
            List<String> tools = new ArrayList<>();
            if (node.has("tools") && node.get("tools").isArray()) {
                node.get("tools").forEach(t -> tools.add(t.asText()));
            }
            String retrievalPolicy = node.path("retrievalPolicy").asText("BOTH");
            List<String> accessScopes = new ArrayList<>();
            if (node.has("accessScopes") && node.get("accessScopes").isArray()) {
                node.get("accessScopes").forEach(a -> accessScopes.add(a.asText()));
            } else {
                accessScopes.addAll(List.of("CUSTOMER", "EXPERT"));
            }
            String status = node.path("status").asText("APPROVED");
            String docVersion = node.has("version") && !node.get("version").asText().isBlank() ? node.get("version").asText() : version;
            boolean enabled = node.has("enabled") ? node.get("enabled").asBoolean() : node.path("enabledForPilot").asBoolean(true);
            entries.add(new Entry(id, node.path("title").asText(), file, docVersion, status, enabled,
                    sources, tools, retrievalPolicy, accessScopes));
        }
        if (entries.isEmpty() || entries.size() != manifest.path("totalDocuments").asInt())
            throw new IllegalArgumentException("Catalog count mismatch: expected " + manifest.path("totalDocuments").asInt() + ", got " + entries.size());
        content.put("catalog.json", mapper.writeValueAsString(new Manifest(revision, entries)));
        return load(content::get);
    }

    /** Unified catalog serving business help. */
    public static KnowledgeCatalog business() {
        return packaged();
    }

    /** Unified catalog serving agent execution. */
    public static KnowledgeCatalog agent() {
        return packaged();
    }

    private static String businessResource(String path) {
        try (var stream = KnowledgeCatalog.class.getResourceAsStream("/knowledge-base/" + path)) {
            if (stream == null) throw new IllegalArgumentException("Missing business resource: " + path);
            return new String(stream.readNBytes(100_001), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new IllegalStateException("Cannot read business resource", e); }
    }

    public static KnowledgeCatalog packaged(String resourcePrefix) {
        String normalizedPrefix = resourcePrefix.endsWith("/") ? resourcePrefix : resourcePrefix + "/";
        return load(path -> {
            try (var in = KnowledgeCatalog.class.getResourceAsStream(normalizedPrefix + path)) {
                if (in == null) throw new IllegalArgumentException("Missing catalog resource");
                return new String(in.readNBytes(100_001), StandardCharsets.UTF_8);
            } catch (IOException e) { throw new IllegalArgumentException("Cannot read catalog", e); }
        });
    }

    public static KnowledgeCatalog load(Function<String, String> read) {
        var manifest = new ObjectMapper().readValue(read.apply("catalog.json"), Manifest.class);
        if (manifest.revision() == null || manifest.revision().isBlank()) throw new IllegalArgumentException("Missing revision");
        var ids = new HashSet<String>();
        var docs = new ArrayList<Document>();
        var chunks = new ArrayList<Chunk>();
        for (var entry : manifest.documents()) {
            if (entry.id() == null || !entry.id().matches("[a-z][a-z0-9-]{1,63}") || !ids.add(entry.id())
                    || entry.file() == null || !entry.file().matches("[a-z0-9-]+\\.md")
                    || entry.title() == null || entry.title().isBlank() || entry.title().length() > 200
                    || entry.version() == null || entry.version().isBlank()
                    || entry.sources() == null || entry.sources().isEmpty() || entry.tools() == null
                    || !Set.of("PILOT", "RETIRED", "DRAFT", "APPROVED", "ACTIVE").contains(entry.status())) {
                throw new IllegalArgumentException("Invalid catalog entry: " + (entry != null ? entry.id() : "null"));
            }
            if (!entry.enabledForPilot() || !(entry.status().equals("PILOT") || entry.status().equals("APPROVED") || entry.status().equals("ACTIVE"))) continue;
            String text = read.apply(entry.file());
            // Git may materialize Markdown as CRLF on Windows; hashes represent logical text.
            if (text != null) text = text.replace("\r\n", "\n");
            if (text == null || text.isBlank() || text.length() > 12_000) throw new IllegalArgumentException("Invalid document size: " + entry.id());
            docs.add(new Document(entry, text));
            chunks.addAll(chunk(entry, text));
        }
        return new KnowledgeCatalog(manifest.revision(), List.copyOf(docs), List.copyOf(chunks));
    }

    static List<Chunk> chunk(Entry entry, String text) {
        var result = new ArrayList<Chunk>();
        String heading = entry.title();
        for (String paragraph : text.replace("\r\n", "\n").split("\\n\\s*\\n")) {
            if (paragraph.isBlank()) continue;
            if (paragraph.startsWith("#")) heading = paragraph.lines().findFirst().orElse(entry.title()).substring(0,
                    Math.min(200, paragraph.lines().findFirst().orElse(entry.title()).length()));
            int room = 2000 - heading.length() - 2;
            for (int start = 0; start < paragraph.length();) {
                int end = Math.min(paragraph.length(), start + room);
                if (end < paragraph.length() && Character.isHighSurrogate(paragraph.charAt(end - 1))) end--;
                String body = heading + "\n\n" + paragraph.substring(start, end);
                result.add(new Chunk(entry.id() + ":" + result.size(), entry.id(), entry.version(), hash(body), body));
                start = end;
            }
        }
        return result;
    }

    public String fingerprint() {
        return hash(new ObjectMapper().writeValueAsString(this));
    }
    public Document document(String id) {
        return documents.stream().filter(d -> d.metadata().id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown knowledge ID: " + id));
    }
    public static String hash(String input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
