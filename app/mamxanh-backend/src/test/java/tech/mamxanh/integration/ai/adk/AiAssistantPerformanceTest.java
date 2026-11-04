package tech.mamxanh.integration.ai.adk;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.BaseLlmConnection;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.mamxanh.integration.ai.AiPromptLoader;
import tech.mamxanh.recipe.service.ApprovedAiKnowledgeRetriever;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class AiAssistantPerformanceTest {

    @Test
    @DisplayName("BENCHMARK 1: Đo lường Cold-start vs Warm-start của AiPromptLoader")
    void benchmarkPromptLoaderColdVsWarm() {
        AiPromptLoader loader = new AiPromptLoader();

        // Cold load
        long startCold = System.nanoTime();
        String sysPromptCold = loader.loadSystemPrompt();
        String scopeCold = loader.loadPolicy("scope-policy");
        String safetyCold = loader.loadPolicy("safety-policy");
        String secCold = loader.loadPolicy("security-policy");
        String styleCold = loader.loadPolicy("response-style");
        long coldDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startCold);

        assertThat(sysPromptCold).isNotBlank();
        assertThat(scopeCold).isNotBlank();
        assertThat(safetyCold).isNotBlank();
        assertThat(secCold).isNotBlank();
        assertThat(styleCold).isNotBlank();

        // Warm load (100 iterations)
        long startWarm = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            loader.loadSystemPrompt();
            loader.loadPolicy("scope-policy");
            loader.loadPolicy("safety-policy");
            loader.loadPolicy("security-policy");
            loader.loadPolicy("response-style");
        }
        long warmDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startWarm);
        double avgWarmMs = (double) warmDurationMs / 100.0;

        System.out.printf("[BENCHMARK-PROMPT] Cold load: %d ms | Warm 100 iterations: %d ms (Avg: %.3f ms/call)%n",
                coldDurationMs, warmDurationMs, avgWarmMs);

        assertThat(coldDurationMs).isLessThan(200);
        assertThat(avgWarmMs).isLessThan(0.1);
    }

    @Test
    @DisplayName("BENCHMARK 2: Đo lường độ trễ In-memory Knowledge Retrieval")
    void benchmarkKnowledgeRetrievalLatency() {
        ApprovedAiKnowledgeRetriever retriever = new ApprovedAiKnowledgeRetriever();

        List<String> testQueries = List.of(
                "consent hồ sơ sức khỏe Gemini xác nhận AI",
                "Quên mật khẩu và khôi phục tài khoản",
                "So sánh các gói dịch vụ AI Free Plus Pro",
                "Cách nấu phở chay nấm hương thanh đạm",
                "Quy trình tạo danh sách mua sắm rỗng và quản lý trạng thái",
                "Tiêu chuẩn dinh dưỡng protein cho người ăn chay",
                "Chính sách an toàn y tế và từ chối kê đơn thuốc",
                "Lên thực đơn tuần cho người giảm cân",
                "Hướng dẫn đổi mật khẩu và đăng nhập Google",
                "Quy định duyệt bài viết của Chuyên gia ẩm thực"
        );

        // Cold retrieval
        long startCold = System.nanoTime();
        for (String q : testQueries) {
            retriever.retrieve(q);
        }
        long coldDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startCold);

        // Warm retrieval (50 runs of 10 queries = 500 lookups)
        List<Long> latenciesMicros = new ArrayList<>();
        for (int run = 0; run < 50; run++) {
            for (String q : testQueries) {
                long t0 = System.nanoTime();
                var results = retriever.retrieve(q);
                long t1 = System.nanoTime();
                latenciesMicros.add(TimeUnit.NANOSECONDS.toMicros(t1 - t0));
                assertThat(results).isNotNull();
            }
        }

        latenciesMicros.sort(Long::compareTo);
        long p50Micros = latenciesMicros.get((int) (latenciesMicros.size() * 0.50));
        long p95Micros = latenciesMicros.get((int) (latenciesMicros.size() * 0.95));
        long maxMicros = latenciesMicros.getLast();

        System.out.printf("[BENCHMARK-RAG] Cold 10 queries: %d ms | Warm 500 lookups -> p50: %.2f ms | p95: %.2f ms | max: %.2f ms%n",
                coldDurationMs, p50Micros / 1000.0, p95Micros / 1000.0, maxMicros / 1000.0);

        assertThat(p95Micros / 1000.0).isLessThan(2000.0); // Guards against deadlock or hanging; actual p95 is printed for baseline metrics
    }

    @Test
    @DisplayName("BENCHMARK 3: Đo lường Overhead của ADK Single-Step Task Runner")
    void benchmarkAdkSingleStepRunnerOverhead() {
        Function<LlmRequest, LlmResponse> handler = req -> LlmResponse.builder()
                .content(Content.builder().role("model")
                        .parts(List.of(Part.fromText("{\"description\":\"Món chay mẫu\",\"steps\":[]}"))).build())
                .usageMetadata(GenerateContentResponseUsageMetadata.builder()
                        .promptTokenCount(50)
                        .candidatesTokenCount(25)
                        .totalTokenCount(75)
                        .build())
                .build();

        BaseLlm mockLlm = new BaseLlm("gemini-3.8-flash") {
            @Override
            public Flowable<LlmResponse> generateContent(LlmRequest request, boolean stream) {
                return Flowable.just(handler.apply(request));
            }

            @Override
            public BaseLlmConnection connect(LlmRequest request) {
                throw new UnsupportedOperationException();
            }
        };

        AdkModelGateway.Policy noopPolicy = new AdkModelGateway.Policy() {
            @Override public Object beforeCall(AdkModelGateway.Call call) { return null; }
            @Override public void onUsableResponse(AdkModelGateway.Call call, Object context, AdkModelGateway.Usage usage) {}
        };

        AdkModelGateway gateway = new AdkModelGateway(mockLlm, noopPolicy, Clock.systemUTC());
        AdkSingleStepTaskRunner runner = new AdkSingleStepTaskRunner(gateway, Clock.systemUTC(), m -> gateway);

        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("description", Schema.builder().type(Type.Known.STRING).build()))
                .required(List.of("description")).build();

        // Warm up JIT
        for (int i = 0; i < 20; i++) {
            runner.executeTaskWithResult(null, "gemini-3.8-flash", "BENCHMARK",
                    "System prompt", "User prompt", schema, 512, Duration.ofSeconds(5));
        }

        // Measure 100 runs
        List<Long> durationsMs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long t0 = System.nanoTime();
            var res = runner.executeTaskWithResult(null, "gemini-3.8-flash", "BENCHMARK",
                    "System prompt", "User prompt", schema, 512, Duration.ofSeconds(5));
            long t1 = System.nanoTime();
            durationsMs.add(TimeUnit.NANOSECONDS.toMillis(t1 - t0));
            assertThat(res.text()).contains("Món chay mẫu");
        }

        durationsMs.sort(Long::compareTo);
        long p50 = durationsMs.get((int) (durationsMs.size() * 0.50));
        long p95 = durationsMs.get((int) (durationsMs.size() * 0.95));

        System.out.printf("[BENCHMARK-ADK-OVERHEAD] 100 executions -> p50: %d ms | p95: %d ms%n", p50, p95);

        // ADK pipeline overhead without network should be tiny (< 50ms)
        assertThat(p50).isLessThan(30);
    }
}
