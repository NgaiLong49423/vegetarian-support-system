package tech.mamxanh.integration.ai.adk;

import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import tools.jackson.databind.ObjectMapper;
import tech.mamxanh.integration.ai.knowledge.*;

/** Persistent conservative upper bounds, not provider invoices or user quota reservations. */
final class G2Budget implements GeminiEmbeddingClient.Observer {
    private final Path directory = Path.of("target/g2");
    private final ObjectMapper json = new ObjectMapper();
    private int batchGenerationEnd=70;
    private int batchEmbeddingEnd=350;
    private int generationInputCap=32000;
    record State(double reservedUpperUsd, int embeddingAttempts, int generationAttempts) {
        State {
            if(!Double.isFinite(reservedUpperUsd)||reservedUpperUsd<0||reservedUpperUsd>5||embeddingAttempts<0||generationAttempts<0)
                throw new IllegalArgumentException("Invalid cumulative G2 budget ledger");
        }
    }
    G2Budget() throws Exception {
        if (!LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).equals(LocalDate.of(2026,11,1)))
            throw new IllegalStateException("Reverify official pricing and update budget bounds before a later live batch");
        Files.createDirectories(directory);
        if(!Files.exists(directory.resolve("budget.json")) && Files.exists(Path.of("src/main/resources/knowledge-base/index.json")))
            throw new IllegalStateException("Published G2 index exists but cumulative ledger is missing; restore it before live calls");
    }
    synchronized void reserve(double amount, boolean generation) {
        try (var file = FileChannel.open(directory.resolve("budget.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = file.lock()) {
            Path path = directory.resolve("budget.json");
            State previous = Files.exists(path) ? json.readValue(Files.readString(path), State.class) : new State(0,0,0);
            var next = new State(previous.reservedUpperUsd()+amount, previous.embeddingAttempts()+(generation?0:1), previous.generationAttempts()+(generation?1:0));
            if (!Double.isFinite(amount) || amount < 0 || next.reservedUpperUsd() > 5 || next.generationAttempts()>batchGenerationEnd || next.embeddingAttempts()>batchEmbeddingEnd)
                throw new IllegalStateException("G2 cumulative technical budget/call cap exceeded");
            Path temporary = Files.createTempFile(directory,"budget-",".tmp");
            try {
                Files.writeString(temporary,json.writeValueAsString(next));
                Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temporary); }
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot reserve G2 technical budget",e); }
    }
    @Override public void beforeCall(KnowledgeIndex.Config config,int bytes) {
        if (bytes > 7000) throw new IllegalStateException("Embedding byte cap exceeded");
        // UTF-8 byte count upper bounds token count; double it and include title/request overhead.
        reserve((bytes+1024)*2*.20/1_000_000,false);
    }
    void generation(int bytes) {
        if(bytes>generationInputCap) throw new IllegalStateException("G2 generation input cap exceeded");
        reserve((bytes*2*.75 + 1024*3.75)/1_000_000,true);
    }
    @Override public synchronized void completed(KnowledgeIndex.Config config,long latency,Float tokens,Integer characters,String status) {
        try {
            var entry = new LinkedHashMap<String,Object>();
            entry.put("model",config.model()); entry.put("dimension",config.dimension()); entry.put("status",status);
            entry.put("latencyMillis",latency); entry.put("providerTokens",tokens); entry.put("providerBillableCharacters",characters);
            Files.writeString(directory.resolve("embedding-usage.jsonl"),json.writeValueAsString(entry)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        } catch (java.io.IOException e) { throw new IllegalStateException("Operational ledger unavailable",e); }
    }
    void preflight() throws Exception {
        preflight(70,350);
    }
    void preflight(int generationCalls,int embeddingCalls) throws Exception {
        preflight(generationCalls,embeddingCalls,32000);
    }
    void preflight(int generationCalls,int embeddingCalls,int inputBytes) throws Exception {
        if(inputBytes<1||inputBytes>32000||generationCalls<1||embeddingCalls<0) throw new IllegalArgumentException("Invalid batch limits");
        generationInputCap=inputBytes;
        // Full batch maximum: 70 bounded generation attempts + 350 bounded embedding attempts.
        // Each embedding input is capped by the 2000-char chunks / 1000-char queries.
        double fullBatchUpper = generationCalls*((inputBytes*2*.75+1024*3.75)/1_000_000.0)+embeddingCalls*((7000+1024)*2*.20/1_000_000);
        // Per-call byte caps and the persistent $5 guard are authoritative; the batch stops before any excess call.
        if (fullBatchUpper > 5) throw new IllegalStateException("Batch bound exceeds G2 budget");
        if(Files.exists(directory.resolve("budget.json"))) {
            var state=json.readValue(Files.readString(directory.resolve("budget.json")),State.class);
            if(state.reservedUpperUsd()+fullBatchUpper>5) throw new IllegalStateException("Existing spend leaves insufficient room for a full rerun; run an explicitly smaller batch");
            batchGenerationEnd=state.generationAttempts()+generationCalls;
            batchEmbeddingEnd=state.embeddingAttempts()+embeddingCalls;
        }
    }
}
