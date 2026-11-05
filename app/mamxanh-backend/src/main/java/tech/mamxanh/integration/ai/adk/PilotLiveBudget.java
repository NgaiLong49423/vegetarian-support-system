package tech.mamxanh.integration.ai.adk;

import com.google.adk.models.*;
import io.reactivex.rxjava3.core.Flowable;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.knowledge.*;
import tools.jackson.databind.ObjectMapper;

/** Opt-in operational evaluation ceiling. Never reserves or debits user quota/Credit. */
public final class PilotLiveBudget implements GeminiEmbeddingClient.Observer {
    public static final String PRICING_DATE="2026-11-01";
    private static final Object PROCESS_BUDGET_LOCK=new Object();
    private final Path path;
    private final int embeddingCeiling;
    private final ObjectMapper json=new ObjectMapper();
    public record State(double reservedUpperUsd,int embeddingAttempts,int generationAttempts,String pricingVerifiedOn) {
        public State {
            if(!Double.isFinite(reservedUpperUsd)||reservedUpperUsd<0||reservedUpperUsd>5||embeddingAttempts<0||generationAttempts<0)
                throw new IllegalArgumentException("Invalid G3 budget ledger");
        }
    }
    public PilotLiveBudget(Path path) { this(path, 150); }
    public PilotLiveBudget(Path path, int embeddingCeiling) {
        if (embeddingCeiling < 1 || embeddingCeiling > 2000) throw new IllegalArgumentException("Invalid embedding ceiling");
        this.path=path.toAbsolutePath(); this.embeddingCeiling=embeddingCeiling;
    }
    // The combined corpus has hundreds of paragraph chunks; the USD ceiling remains authoritative.
    public static PilotLiveBudget configured(String path) {return path==null||path.isBlank()?null:new PilotLiveBudget(Path.of(path),2000);}
    private synchronized void reserve(double dollars, boolean generation) {
        synchronized(PROCESS_BUDGET_LOCK) {
        if(!LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString().equals(PRICING_DATE))
            throw new IllegalStateException("Reverify G3 pricing before live calls on a later date");
        try {
            Files.createDirectories(path.getParent());
            try(var channel=FileChannel.open(path.resolveSibling("budget.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
                var lock=channel.lock()) {
                if(!Files.exists(path)) throw new IllegalStateException("G3 cumulative ledger missing; initialize only for an authorized evaluation batch");
                var previous=json.readValue(Files.readString(path),State.class);
                if(!PRICING_DATE.equals(previous.pricingVerifiedOn())) throw new IllegalStateException("Unverified pricing ledger");
                var next=new State(previous.reservedUpperUsd()+dollars,previous.embeddingAttempts()+(generation?0:1),previous.generationAttempts()+(generation?1:0),PRICING_DATE);
                // The authorized focused validation batch was preflighted against the existing spend.
                // State's USD ceiling remains authoritative for every call, including failed attempts.
                if(next.embeddingAttempts()>embeddingCeiling||next.generationAttempts()>140) throw new IllegalStateException("Evaluation call ceiling reached");
                Path temporary=Files.createTempFile(path.getParent(),"g3-budget-",".tmp");
                try {Files.writeString(temporary,json.writeValueAsString(next));Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                finally {Files.deleteIfExists(temporary);}
            }
        } catch(java.io.IOException e) {
            org.slf4j.LoggerFactory.getLogger(PilotLiveBudget.class).warn("Operational budget storage failed category={}",e.getClass().getSimpleName());
            throw new IllegalStateException("G3 operational budget unavailable",e);
        }
        }
    }
    public void generation(int bytes) {
        if(bytes<0||bytes>128000) throw new IllegalArgumentException("Model input cap exceeded");
        reserve((bytes*2*.75+1024*3.75)/1_000_000,true);
    }
    public void beforeCall(KnowledgeIndex.Config config,int bytes) {
        if(!config.model().equals("gemini-embedding-2")||bytes<0||bytes>7000) throw new IllegalArgumentException("Unpriced G3 embedding input");
        reserve((bytes+1024)*2*.20/1_000_000,false);
    }
    public void completed(KnowledgeIndex.Config config,long latency,Float tokens,Integer characters,String status) {
        var data=new LinkedHashMap<String,Object>();data.put("model",config.model());data.put("dimension",config.dimension());
        data.put("providerTokens",tokens);data.put("providerBillableCharacters",characters);data.put("status",status);data.put("latencyMillis",latency);
        append("embedding-usage.jsonl",data);
    }
    private synchronized void append(String file,Object value) {
        try {Files.writeString(path.resolveSibling(file),json.writeValueAsString(value)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
        catch(java.io.IOException e) {throw new IllegalStateException("G3 operational usage storage unavailable",e);}
    }
    public BaseLlm wrap(BaseLlm delegate) {
        if(!AiTokenUsage.MODEL.equals(delegate.model())) throw new IllegalArgumentException("Unpriced G3 generation model");
        return new BaseLlm(delegate.model()) {
            public Flowable<LlmResponse> generateContent(LlmRequest request,boolean stream) {
                return Flowable.defer(()->{
                    int cap=request.config().flatMap(c->c.maxOutputTokens()).orElse(1024);
                    if(cap>4096) throw new IllegalArgumentException("Output cap exceeded");
                    int bytes=request.toJson().getBytes(StandardCharsets.UTF_8).length+request.tools().values().stream()
                            .mapToInt(t->t.declaration().map(d->d.toJson().getBytes(StandardCharsets.UTF_8).length).orElse(0)).sum();
                    generation(bytes);
                    return delegate.generateContent(request,stream).doOnNext(r->{
                        var record=new LinkedHashMap<String,Object>();record.put("model",model());record.put("providerUsage",r.usageMetadata().orElse(null));
                        record.put("finishReason",r.finishReason().map(Object::toString).orElse("MISSING"));append("generation-usage.jsonl",record);
                    });
                });
            }
            public BaseLlmConnection connect(LlmRequest request) {throw new UnsupportedOperationException();}
        };
    }
}
