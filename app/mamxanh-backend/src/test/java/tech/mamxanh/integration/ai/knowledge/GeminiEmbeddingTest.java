package tech.mamxanh.integration.ai.knowledge;

import static org.assertj.core.api.Assertions.*;
import com.google.genai.types.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GeminiEmbeddingTest {
    private static final GeminiEmbeddingClient.Observer NO_USAGE = new GeminiEmbeddingClient.Observer() {
        public void beforeCall(KnowledgeIndex.Config c,int bytes) { assertThat(bytes).isPositive(); }
        public void completed(KnowledgeIndex.Config c,long latency,Float tokens,Integer characters,String status) {
            assertThat(tokens).isNull(); assertThat(characters).isNull();
        }
    };
    @Test void sdkInputsSeparateTaskTypesFromSearchPrefixesAndNormalizeVectors() {
        for(String model:List.of("gemini-embedding-001","gemini-embedding-2")) {
            var config=new KnowledgeIndex.Config(model,768,model.endsWith("001")?"task-type-v1":"search-prefix-v1");
            var inputs=new ArrayList<String>(); var options=new ArrayList<EmbedContentConfig>();
            var client=new GeminiEmbeddingClient(config,NO_USAGE,(m,text,option)->{
                assertThat(m).isEqualTo(model); inputs.add(text); options.add(option);
                var values=new ArrayList<>(Collections.nCopies(768,0f));values.set(0,2f);
                return EmbedContentResponse.builder().embeddings(List.of(ContentEmbedding.builder().values(values).build())).build();
            },Duration.ofSeconds(10));
            assertThat(client.embed("Danh sách","Tiêu đề",false).getFirst()).isEqualTo(1);
            client.embed("Câu hỏi","",true);
            assertThat(options).allSatisfy(o->assertThat(o.outputDimensionality()).contains(768));
            if(model.endsWith("001")) {
                assertThat(inputs).containsExactly("Danh sách","Câu hỏi");
                assertThat(options.get(0).taskType()).contains("RETRIEVAL_DOCUMENT");
                assertThat(options.get(1).taskType()).contains("RETRIEVAL_QUERY");
            } else {
                assertThat(inputs).containsExactly("title: Tiêu đề | text: Danh sách","task: search result | query: Câu hỏi");
                assertThat(options).allSatisfy(o->assertThat(o.taskType()).isEmpty());
            }
        }
    }
    @Test void queryDeadlineDoesNotRetryAndSanitizesProviderFailure() {
        var attempts=new AtomicInteger();
        var client=new GeminiEmbeddingClient(KnowledgeTest.CONFIG,NO_USAGE,(m,text,options)->{
            attempts.incrementAndGet();
            try {Thread.sleep(1000);} catch(InterruptedException ignored) {Thread.currentThread().interrupt();}
            var values=new ArrayList<>(Collections.nCopies(768,0f)); values.set(0,1f);
            return EmbedContentResponse.builder().embeddings(List.of(ContentEmbedding.builder().values(values).build())).build();
        },Duration.ofMillis(40));
        assertThatThrownBy(()->client.embed("query","",true)).isInstanceOf(IllegalStateException.class).hasMessage("Embedding unavailable");
        assertThat(attempts).hasValue(1);
    }
    @Test void providerErrorsAreSanitizedWithoutRetry() {
        var attempts=new AtomicInteger();
        var client=new GeminiEmbeddingClient(KnowledgeTest.CONFIG,NO_USAGE,(m,text,options)->{
            attempts.incrementAndGet();throw new IllegalArgumentException("synthetic provider detail must stay private");
        },Duration.ofSeconds(10));
        assertThatThrownBy(()->client.embed("query","",true)).isInstanceOf(IllegalStateException.class).hasMessage("Embedding unavailable").hasNoCause();
        assertThat(attempts).hasValue(1);
    }
}
