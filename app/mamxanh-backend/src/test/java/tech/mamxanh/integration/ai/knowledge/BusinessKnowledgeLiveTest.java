package tech.mamxanh.integration.ai.knowledge;

import com.google.adk.models.Gemini;
import com.google.adk.tools.FunctionTool;
import com.google.genai.Client;
import com.google.genai.types.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tech.mamxanh.integration.ai.AiPromptLoader;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.adk.*;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Explicitly authorized operational batch; never starts Spring or touches user SQL. */
@EnabledIfSystemProperty(named="business.live", matches="true")
class BusinessKnowledgeLiveTest {
    @Test
    void buildIndexAndExerciseNativeKnowledgeAgent() throws Exception {
        var json = new ObjectMapper();
        var catalog = KnowledgeCatalog.agent();
        var dir = Path.of("target/business-knowledge-live-20261101");
        Files.createDirectories(dir);
        if (Files.exists(dir.resolve("runtime-owner.json")))
            throw new IllegalStateException("Evaluation ledger is owned by the running acceptance stack; reconcile and transfer ownership before another CLI batch");
        var ledger = dir.resolve("budget.json");
        // CREATE_NEW: reruns continue the same ledger and vector checkpoint.
        if (!Files.exists(ledger)) Files.writeString(ledger,
                json.writeValueAsString(new PilotLiveBudget.State(0,0,0,PilotLiveBudget.PRICING_DATE)), StandardOpenOption.CREATE_NEW);
        int calls = catalog.chunks().size() + 15;
        assertThat(calls).isLessThan(2000);
        // Upper bound: maximum permitted bytes per embedding plus 18 maximum generation calls.
        assertThat(calls * 8024 * 2 * .20 / 1_000_000 + 12 * (128000 * 2 * .75 + 1024 * 3.75) / 1_000_000).isLessThan(5);
        var budget = new PilotLiveBudget(ledger, 2000);
        var config = new KnowledgeIndex.Config("gemini-embedding-2",768,"search-prefix-v1");
        String key = System.getenv("GEMINI_API_KEY");
        assertThat(key).as("Backend Gemini credential available").isNotBlank();
        try (var client = Client.builder().apiKey(key).httpOptions(HttpOptions.builder().timeout(10000)
                .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build()) {
            var embedder = new GeminiEmbeddingClient(client,config,budget);
            var checkpoint = dir.resolve("vectors.json");
            Map<String,List<Double>> vectors = new LinkedHashMap<>();
            if (Files.exists(checkpoint)) {
                var prior = json.readValue(Files.readString(checkpoint), KnowledgeIndex.class);
                assertThat(prior.corpusHash()).isEqualTo(catalog.fingerprint());
                assertThat(prior.config()).isEqualTo(config);
                vectors.putAll(prior.vectors());
            }
            var remaining = catalog.chunks().stream().filter(c -> !vectors.containsKey(c.id())).toList();
            try (var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
                for (int offset=0; offset<remaining.size(); offset+=4) {
                    var batch = remaining.subList(offset,Math.min(offset+4,remaining.size()));
                    var jobs = batch.stream().map(chunk -> executor.submit(() -> {
                        var vector = embedder.embed(chunk.text(),catalog.document(chunk.documentId()).metadata().title(),false);
                        synchronized (vectors) {
                            vectors.put(chunk.id(),vector);
                            Files.writeString(checkpoint,json.writeValueAsString(new KnowledgeIndex(1,catalog.revision(),catalog.fingerprint(),config,0,vectors)));
                        }
                        return true;
                    })).toList();
                    for (var job : jobs) job.get();
                }
            }
            double threshold = -1;
            for (String unsupported : List.of("Dự báo thời tiết ngày mai", "Tỷ giá ngoại tệ hôm nay", "Đặt vé máy bay")) {
                var query = embedder.embed(unsupported,"",true);
                threshold = Math.max(threshold,vectors.values().stream().mapToDouble(v -> KnowledgeIndex.cosine(query,v)).max().orElse(-1));
            }
            var index = new KnowledgeIndex(1,catalog.revision(),catalog.fingerprint(),config,
                    Math.min(1.000001,threshold+.000001),vectors).validate(catalog,config);
            // Atomic publish to a separate artifact: frozen G2/G3 corpus/index remain intact.
            index.publish(Path.of("src/main/resources/ai-knowledge/agent-index.json"),catalog);
            try (var retriever = new HybridKnowledgeRetriever(catalog,index,embedder,Clock.systemUTC())) {
                var query = retriever.search("Quên mật khẩu và khôi phục tài khoản");
                assertThat(query.status()).isEqualTo("hybrid");
                assertThat(query.hits()).extracting(HybridKnowledgeRetriever.Hit::knowledgeId).contains("password-recovery");
                var tools = new KnowledgeTools(retriever);
                var auth = new UsernamePasswordAuthenticationToken("synthetic-member","unused",List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
                var model = budget.wrap(new Gemini(AiTokenUsage.MODEL,client));
                var gateway = new AdkModelGateway(model,new AdkModelGateway.Policy(){
                    public Object beforeCall(AdkModelGateway.Call call) { return null; }
                    public void onUsableResponse(AdkModelGateway.Call call,Object context,AdkModelGateway.Usage usage) { }
                },Clock.systemUTC());
                try (var session = new AdkSpikeSession(auth,gateway,List.of(
                        new AdkSpikeSession.Tool(FunctionTool.create(tools,"search_business_knowledge"),args -> {}),
                        new AdkSpikeSession.Tool(FunctionTool.create(tools,"get_business_knowledge"),args -> {})),
                        Clock.systemUTC(),Duration.ofSeconds(30),new AiPromptLoader().loadSystemPrompt()+"\n"+KnowledgeTools.INSTRUCTION,1024)) {
                    var events = session.run(auth,Content.fromParts(Part.fromText("Tôi quên mật khẩu Mâm Xanh, tôi cần làm gì?"))).toList().blockingGet();
                    String trace = events.toString();
                    Files.writeString(dir.resolve("native-events.txt"),trace);
                    assertThat(trace).contains("search_business_knowledge","get_business_knowledge","password-recovery");
                }
            }
        }
    }
}
