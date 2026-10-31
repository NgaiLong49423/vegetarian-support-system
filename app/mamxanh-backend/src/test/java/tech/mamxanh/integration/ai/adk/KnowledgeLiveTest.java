package tech.mamxanh.integration.ai.adk;

import static org.assertj.core.api.Assertions.*;
import static tech.mamxanh.integration.ai.adk.AdkSpikeTest.actor;
import static tech.mamxanh.integration.ai.adk.AdkSpikeTest.user;
import com.google.adk.models.*;
import com.google.adk.tools.FunctionTool;
import com.google.genai.Client;
import com.google.genai.types.*;
import io.reactivex.rxjava3.core.Flowable;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.knowledge.*;
import tools.jackson.databind.ObjectMapper;

/** Explicit live command only. All questions and identities are synthetic; no SQL/public chat. */
@EnabledIfEnvironmentVariable(named="MAMXANH_G2_LIVE", matches="true")
class KnowledgeLiveTest {
    record Case(String id,String split,String category,String query,List<String> expectedSources,String expectedKind) { }
    record Candidate(KnowledgeIndex index,double recall,double mrr,double estimatedIndexUsd) { }
    private final ObjectMapper json = new ObjectMapper();
    private final Path output = Path.of("target/g2");
    private final KnowledgeCatalog catalog=KnowledgeCatalog.packaged();
    private List<Case> dataset() throws Exception {
        try(var input=getClass().getResourceAsStream("/ai-knowledge/evaluation.json")) {
            return List.of(json.readValue(input.readAllBytes(),Case[].class));
        }
    }
    @Test void benchmarkLockIndexThenEvaluateNativeAgent() throws Exception {
        var budget=new G2Budget();
        String phase=System.getProperty("g2.phase","full");
        if(phase.equals("diagnostic")) budget.preflight(6,3);
        else if(phase.equals("validation")) budget.preflight(70,30,16000);
        else budget.preflight();
        var cases=dataset(); assertThat(cases).hasSize(60);
        var dev=cases.stream().filter(c->c.split().equals("development")).toList();
        assertThat(dev).hasSize(36);
        var reports=new ArrayList<Map<String,Object>>(); var candidates=new ArrayList<Candidate>();
        try(var client=Client.builder().apiKey(System.getenv("GEMINI_API_KEY"))
                .httpOptions(HttpOptions.builder().timeout(10000).retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build()) {
            if (Set.of("agents","diagnostic","validation").contains(phase)) {
                Path resource=Path.of("src/main/resources/ai-knowledge/index.json");
                var selected=json.readValue(Files.readString(resource),KnowledgeIndex.class);
                selected.validate(catalog,selected.config());
                var lock=json.readTree(Files.readString(output.resolve("selection-lock.json")));
                if (!lock.path("corpusHash").asText().equals(selected.corpusHash()) || lock.path("threshold").asDouble()!=selected.threshold()
                        || !json.treeToValue(lock.path("config"),KnowledgeIndex.Config.class).equals(selected.config()))
                    throw new IllegalStateException("Selection lock mismatch");
                evaluateAgents(client,budget,selected,new GeminiEmbeddingClient(client,selected.config(),budget),cases.stream().filter(c->c.split().equals("holdout")).toList());
                return;
            }
            for(String model:List.of("gemini-embedding-001","gemini-embedding-2")) for(int dimension:List.of(768,1536)) {
                var config=new KnowledgeIndex.Config(model,dimension,model.endsWith("001")?"task-type-v1":"search-prefix-v1");
                try {
                    var embedder=new GeminiEmbeddingClient(client,config,budget);
                    var vectors=new LinkedHashMap<String,List<Double>>();
                    long bytes=0;
                    for(var chunk:catalog.chunks()) {
                        String title=catalog.document(chunk.documentId()).metadata().title();
                        vectors.put(chunk.id(),embedder.embed(chunk.text(),title,false)); bytes+=chunk.text().getBytes(StandardCharsets.UTF_8).length;
                    }
                    var queryVectors=new LinkedHashMap<String,List<Double>>();
                    for(var c:dev) queryVectors.put(c.id(),embedder.embed(c.query(),"",true));
                    double threshold=-1;
                    for(var c:dev) if(c.category().equals("unsupported")) for(var vector:vectors.values())
                        threshold=Math.max(threshold,KnowledgeIndex.cosine(queryVectors.get(c.id()),vector));
                    threshold=Math.min(1.000001,threshold+0.000001);
                    var index=new KnowledgeIndex(1,catalog.revision(),catalog.fingerprint(),config,threshold,vectors).validate(catalog,config);
                    index.publish(output.resolve(model+"-"+dimension+".json"),catalog);
                    var metrics=evaluate(index,dev,queryVectors,reports,"development");
                    candidates.add(new Candidate(index,metrics[0],metrics[1],bytes*(model.endsWith("001")?.15:.20)/1_000_000));
                    reports.add(Map.of("model",model,"dimension",dimension,"status","VERIFIED","threshold",threshold));
                } catch(RuntimeException unavailable) {
                    reports.add(Map.of("model",model,"dimension",dimension,"status","NOT VERIFIED","reason",safeFailure(unavailable)));
                }
                Files.writeString(output.resolve("benchmark.json"),json.writeValueAsString(reports));
            }
            assertThat(candidates).as("At least one real embedding configuration must work").isNotEmpty();
            candidates.sort(Comparator.comparingDouble(Candidate::recall).reversed()
                    .thenComparing(Comparator.comparingDouble(Candidate::mrr).reversed())
                    .thenComparingDouble(Candidate::estimatedIndexUsd).thenComparingInt(c->c.index().config().dimension()));
            var selected=candidates.getFirst().index();
            // Lock before any holdout requests. Publishing is explicit in this opt-in command only.
            Files.writeString(output.resolve("selection-lock.json"),json.writeValueAsString(Map.of("config",selected.config(),"threshold",selected.threshold(),"corpusHash",selected.corpusHash(),"selection","development-only")));
            selected.publish(Path.of("src/main/resources/ai-knowledge/index.json"),catalog);
            var holdout=cases.stream().filter(c->c.split().equals("holdout")).toList();
            var embedder=new GeminiEmbeddingClient(client,selected.config(),budget);
            var holdoutVectors=new LinkedHashMap<String,List<Double>>();
            for(var c:holdout) holdoutVectors.put(c.id(),embedder.embed(c.query(),"",true));
            evaluate(selected,holdout,holdoutVectors,reports,"holdout");
            Files.writeString(output.resolve("benchmark.json"),json.writeValueAsString(reports));
            evaluateAgents(client,budget,selected,embedder,holdout);
        }
    }
    private double[] evaluate(KnowledgeIndex index,List<Case> cases,Map<String,List<Double>> queries,List<Map<String,Object>> reports,String split) {
        double hybridRecall=0,hybridMrr=0;
        for(String mode:List.of("keyword","semantic","hybrid")) {
            int supported=0,found=0,wrongUnsupported=0; double reciprocal=0; long latency=0;
            try(var retriever=new HybridKnowledgeRetriever(catalog,index,(q,t,b)->{
                var c=cases.stream().filter(row->row.query().equals(q)).findFirst().orElseThrow();return queries.get(c.id());
            },Clock.systemUTC())) {
                for(var c:cases) {
                    long start=System.nanoTime();
                    var result=mode.equals("hybrid")?retriever.search(c.query()):retriever.result(mode.equals("keyword")?retriever.keyword(c.query()):retriever.semantic(queries.get(c.id()),index.threshold()),mode,"BENCHMARK");
                    latency+=(System.nanoTime()-start)/1_000_000;
                    var ids=result.hits().stream().map(HybridKnowledgeRetriever.Hit::knowledgeId).toList();
                    if(c.category().equals("unsupported")&&!ids.isEmpty()) wrongUnsupported++;
                    if(!c.category().equals("supported")) continue;
                    supported++; for(int i=0;i<ids.size();i++) if(c.expectedSources().contains(ids.get(i))) {found++;reciprocal+=1.0/(i+1);break;}
                }
            }
            double recall=found/(double)supported,mrr=reciprocal/supported;
            reports.add(Map.of("model",index.config().model(),"dimension",index.config().dimension(),"split",split,"mode",mode,
                    "recallAt3",recall,"mrr",mrr,"unsupportedWithResults",wrongUnsupported,"rankingLatencyMillis",latency));
            if(mode.equals("hybrid")) {hybridRecall=recall;hybridMrr=mrr;}
        }
        return new double[]{hybridRecall,hybridMrr};
    }
    private void evaluateAgents(Client client,G2Budget budget,KnowledgeIndex index,GeminiEmbeddingClient embedder,List<Case> holdout) throws Exception {
        var chosen=new ArrayList<Case>();
        for (var doc:catalog.documents()) chosen.addAll(holdout.stream()
                .filter(c->c.category().equals("supported")&&c.expectedSources().contains(doc.metadata().id())).limit(2).toList());
        chosen.add(holdout.stream().filter(c->c.category().equals("supported")&&!chosen.contains(c)).findFirst().orElseThrow());
        chosen.addAll(holdout.stream().filter(c->!c.category().equals("supported")).toList());
        assertThat(chosen).hasSize(20);
        String phase=System.getProperty("g2.phase","full");
        var previous=new ArrayList<Map<String,Object>>();
        if(phase.equals("diagnostic")) {
            Path baseline=output.resolve("agent-evaluation-initial.json");
            if(!Files.exists(baseline)) Files.copy(output.resolve("agent-evaluation.json"),baseline);
            for(var node:json.readTree(Files.readString(baseline))) {
                if(node.path("pass").asBoolean()) previous.add(json.convertValue(node,Map.class));
            }
            chosen.removeIf(c->previous.stream().anyMatch(row->row.get("id").equals(c.id())));
            if(phase.equals("diagnostic")&&chosen.size()>1) chosen.subList(1,chosen.size()).clear();
        }
        var evidence=new ArrayList<Map<String,Object>>(); int passed=0;
        for(int i=0;i<chosen.size();i++) {
            var c=chosen.get(i); var owner=actor("g2-synthetic-"+i,i%2==0?"CUSTOMER":"EXPERT");
            var ledger=new AdkSpikeTest.Ledger();
            var diagnostics=new ArrayList<Map<String,Object>>();
            var bounded=new BaseLlm(AiTokenUsage.MODEL) {
                private final Gemini delegate=new Gemini(AiTokenUsage.MODEL,client);
                public Flowable<LlmResponse> generateContent(LlmRequest request,boolean stream) {
                    return Flowable.defer(()->{
                        int bytes=request.toJson().getBytes(StandardCharsets.UTF_8).length
                                +request.tools().values().stream().mapToInt(t->t.declaration().map(d->d.toJson().getBytes(StandardCharsets.UTF_8).length).orElse(0)).sum();
                        budget.generation(bytes);
                        return delegate.generateContent(request,stream).doOnNext(response->diagnostics.add(Map.of(
                                "finishReason",response.finishReason().map(Object::toString).orElse("MISSING"),
                                "errorCode",response.errorCode().map(Object::toString).orElse("NONE"),"partial",response.partial().orElse(false))));
                    });
                }
                public BaseLlmConnection connect(LlmRequest request) { throw new UnsupportedOperationException(); }
            };
            var row=new LinkedHashMap<String,Object>(); row.put("id",c.id());row.put("role",i%2==0?"CUSTOMER":"EXPERT");
            long start=System.nanoTime();
            try(var retriever=new HybridKnowledgeRetriever(catalog,index,embedder,Clock.systemUTC())) {
                var functions=new KnowledgeTools(retriever);
                var tools=List.of(new AdkSpikeSession.Tool(FunctionTool.create(functions,"search_business_knowledge"),args->{}),
                        new AdkSpikeSession.Tool(FunctionTool.create(functions,"get_business_knowledge"),args->{}));
                try(var session=new AdkSpikeSession(owner,new AdkModelGateway(bounded,ledger,Clock.systemUTC()),tools,Clock.systemUTC(),AdkSpikeSession.RUN_TIMEOUT,KnowledgeTools.INSTRUCTION,
                        Integer.getInteger("g2.outputTokens",phase.equals("diagnostic")?512:1024))) {
                    var events=session.run(owner,user(c.query())).toList().blockingGet();
                    var calls=events.stream().flatMap(e->e.functionCalls().stream()).toList();
                    var names=calls.stream().map(f->f.name().orElse("")).toList();
                    var readIds=calls.stream().filter(f->f.name().orElse("").equals("get_business_knowledge")).map(f->String.valueOf(f.args().orElse(Map.of()).get("knowledgeId"))).toList();
                    String answer=events.stream().filter(e->e.finalResponse()).map(e->e.stringifyContent()).reduce("",(a,b)->a+b);
                    row.put("syntheticFinal",answer);
                    int first=answer.indexOf('{'),last=answer.lastIndexOf('}');
                    var response=json.readTree(answer.substring(first,last+1));
                    var sources=new ArrayList<String>(); response.path("sourceIds").forEach(n->sources.add(n.asText()));
                    boolean safe=names.stream().allMatch(n->Set.of("search_business_knowledge","get_business_knowledge").contains(n))
                            && readIds.containsAll(sources) && !response.path("answer").asText().isBlank();
                    boolean correct=safe && names.contains("search_business_knowledge") && response.path("kind").asText().equals(c.expectedKind())
                            && (!c.category().equals("supported") || (names.contains("get_business_knowledge") && sources.stream().anyMatch(c.expectedSources()::contains)));
                    if(correct) passed++;
                    row.put("pass",correct);row.put("safety",safe);row.put("kind",response.path("kind").asText());
                    row.put("sourceIds",sources);row.put("toolNames",names);
                    // Synthetic output only, retained for manual rubric review. No prompts/user data.
                    row.put("syntheticAnswer",response.path("answer").asText());
                }
            } catch(Exception failure) {row.put("pass",false);row.put("reason",safeFailure(failure));}
            row.put("latencyMillis",(System.nanoTime()-start)/1_000_000);row.put("modelCalls",ledger.calls.size());
            row.put("responseDiagnostics",diagnostics);
            row.put("providerUsage",ledger.usages); evidence.add(row);
            Files.writeString(output.resolve("agent-evaluation"+(phase.equals("diagnostic")?"-diagnostic":"")+".json"),json.writeValueAsString(evidence));
        }
        if(phase.equals("diagnostic")) return;
        Files.writeString(output.resolve("agent-summary.json"),json.writeValueAsString(Map.of("total",20,"passed",passed,"minimum",16,"manualRubricReviewRequired",true)));
        assertThat(passed).as("NFR-25 structural/source constraints; human-readable synthetic answers require review too").isGreaterThanOrEqualTo(16);
    }
    private static String safeFailure(Throwable error) {
        for(int i=0;error.getCause()!=null&&i<8;i++) error=error.getCause();
        return error instanceof com.google.genai.errors.ApiException api ? "PROVIDER_"+api.code() : error.getClass().getSimpleName();
    }
}
