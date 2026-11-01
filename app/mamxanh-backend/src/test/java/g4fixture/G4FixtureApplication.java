package g4fixture;

import com.google.adk.models.*;
import com.google.genai.types.*;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.*;
import tech.mamxanh.MamXanhApplication;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.integration.ai.knowledge.KnowledgeCatalog;
import tech.mamxanh.recipe.service.*;
import tech.mamxanh.shopping.service.ShoppingListService;
import tech.mamxanh.subscription.service.*;
import java.util.*;

/** Test-only entry point packaged separately. Real ADK/Spring/SQL, deterministic model, no provider calls. */
public class G4FixtureApplication {
    public static void main(String[] args) {
        SpringApplication.run(new Class<?>[]{MamXanhApplication.class, Fixture.class}, args);
    }
    @Configuration(proxyBeanMethods = false)
    public static class Fixture {
        @Bean @Primary
        AdkShoppingPilotService fixturePilot(CurrentUserService users, ShoppingListService shopping,
                AiTokenQuotaService quota, AiChatSettingsService settings, AiAssistantSessionStore sessions,
                tools.jackson.databind.ObjectMapper mapper, org.springframework.context.ApplicationEventPublisher events,
                java.time.Clock clock) {
            return new AdkShoppingPilotService(users, shopping, quota, settings, sessions, mapper, events, clock,
                    KnowledgeCatalog.agent(), null, FixtureModel::new, null, true);
        }
    }
    static final class FixtureModel extends BaseLlm {
        FixtureModel(String model) { super(model); }
        @Override public Flowable<LlmResponse> generateContent(LlmRequest request, boolean stream) {
            if (request.contents().toString().contains("G4 trước"))
                return Flowable.timer(2, java.util.concurrent.TimeUnit.SECONDS).map(ignored -> respond(request));
            return Flowable.fromCallable(() -> respond(request));
        }
        @Override public BaseLlmConnection connect(LlmRequest request) { throw new UnsupportedOperationException(); }
        private LlmResponse respond(LlmRequest request) {
            List<Content> all = request.contents(); int start = 0;
            String name = "";
            for (int i = 0; i < all.size(); i++) {
                for (Part p : all.get(i).parts().orElse(List.of())) {
                    if ("user".equals(all.get(i).role().orElse("user")) && p.text().isPresent()) {
                        start = i;
                        var text = p.text().get(); int at = text.indexOf("tên là ");
                        if (at >= 0) name = text.substring(at + 7).strip();
                    }
                    if (p.functionResponse().filter(r -> "adk_request_confirmation".equals(r.name().orElse(""))).isPresent()) start = i;
                }
            }
            var responses = all.subList(start, all.size()).stream().flatMap(c -> c.parts().orElse(List.of()).stream())
                    .flatMap(p -> p.functionResponse().stream()).toList();
            var names = responses.stream().map(r -> r.name().orElse("")).toList();
            if (names.contains("get_shopping_list")) return text("Đã đọc kết quả thật từ Backend.");
            if (names.contains("create_shopping_list")) {
                Object id = responses.stream().filter(r -> "create_shopping_list".equals(r.name().orElse("")))
                        .map(r -> r.response().orElse(Map.of()).get("id")).filter(Objects::nonNull).reduce((a,b) -> b).orElse(null);
                return id instanceof Number ? call("get_shopping_list", Map.of("listId", id)) : text("Vui lòng xác nhận thay đổi đã xem trước.");
            }
            if (names.isEmpty()) return call("search_business_knowledge", Map.of("query", "tạo danh sách mua sắm rỗng"));
            if (!names.contains("get_business_knowledge")) return call("get_business_knowledge", Map.of("knowledgeId", "shopping-create"));
            return name.isBlank() ? text("Bạn muốn đặt tên danh sách là gì?") : call("create_shopping_list", Map.of("name", name));
        }
        private LlmResponse text(String text) { return response(Part.fromText(text)); }
        private LlmResponse call(String tool, Map<String,Object> arguments) {
            return response(Part.builder().functionCall(FunctionCall.builder().id(UUID.randomUUID().toString()).name(tool).args(arguments).build()).build());
        }
        private LlmResponse response(Part part) {
            return LlmResponse.builder().content(Content.builder().role("model").parts(List.of(part)).build())
                    .usageMetadata(GenerateContentResponseUsageMetadata.builder().promptTokenCount(20).candidatesTokenCount(10).totalTokenCount(30).build()).build();
        }
    }
}
