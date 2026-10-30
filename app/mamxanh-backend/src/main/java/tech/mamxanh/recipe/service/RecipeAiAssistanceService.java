package tech.mamxanh.recipe.service;

import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.service.IngredientCatalogLookupService;
import tech.mamxanh.recipe.dto.request.RecipeAiAssistanceRequest;
import tech.mamxanh.recipe.dto.request.RecipeAiAssistanceRequest.ContentType;
import tech.mamxanh.recipe.dto.response.RecipeAiAssistanceResponse;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;

@Service
public class RecipeAiAssistanceService {
    private static final Logger log = LoggerFactory.getLogger(RecipeAiAssistanceService.class);

    private final CurrentUserService currentUserService;
    private final SubscriptionEntitlementService entitlementService;
    private final IngredientCatalogLookupService ingredientCatalog;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public RecipeAiAssistanceService(CurrentUserService currentUserService,
            SubscriptionEntitlementService entitlementService, IngredientCatalogLookupService ingredientCatalog,
            AiClient aiClient, ObjectMapper objectMapper) {
        this.currentUserService = currentUserService;
        this.entitlementService = entitlementService;
        this.ingredientCatalog = ingredientCatalog;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    public RecipeAiAssistanceResponse generate(RecipeAiAssistanceRequest request) {
        var expert = currentUserService.requireActiveExpert();
        entitlementService.requirePlusOrPro(expert.id());

        List<Long> ingredientIds = request.ingredients().stream().map(RecipeAiAssistanceRequest.Ingredient::ingredientId).toList();
        if (new HashSet<>(ingredientIds).size() != ingredientIds.size()) {
            throw new AppException(ErrorCode.RECIPE_DATA_INVALID, "Mỗi nguyên liệu chỉ được xuất hiện một lần.");
        }
        List<IngredientResponse> ingredients = ingredientCatalog.findActiveIngredientsByIds(ingredientIds);
        if (ingredients.size() != ingredientIds.size()) {
            throw new AppException(ErrorCode.RECIPE_DATA_INVALID, "Hãy chọn nguyên liệu còn hoạt động trong danh mục.");
        }

        String prompt = prompt(request, ingredients);
        String raw;
        try {
            raw = aiClient.generateRecipeText(prompt, ingredientIds);
        } catch (AiProviderException exception) {
            log.warn("Recipe AI provider is unavailable: {}", exception.getClass().getSimpleName());
            throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        } catch (RuntimeException exception) {
            log.warn("Recipe AI provider failed: {}", exception.getClass().getSimpleName());
            throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        }

        GeneratedContent generated = parse(raw);
        String content = request.contentType() == ContentType.DESCRIPTION
                ? validateDescription(generated.description())
                : validateInstructions(generated.steps(), ingredients);
        return new RecipeAiAssistanceResponse(request.contentType(), content);
    }

    private String prompt(RecipeAiAssistanceRequest request, List<IngredientResponse> ingredients) {
        String lines = ingredients.stream().map(item -> {
            var entry = request.ingredients().stream().filter(line -> line.ingredientId().equals(item.id())).findFirst().orElseThrow();
            return "- id=%d, tên=%s, số lượng=%s, unitId=%d".formatted(item.id(), item.name(), entry.quantity(), entry.unitId());
        }).collect(java.util.stream.Collectors.joining("\n"));
        return "Hỗ trợ tác giả viết nội dung công thức bằng tiếng Việt. Ưu tiên sử dụng các nguyên liệu và định lượng tác giả đã nhập. "
                + "Nếu phù hợp, có thể gợi ý thêm nguyên liệu phụ; hãy nêu rõ đó là gợi ý để tác giả rà soát và chỉnh sửa trước khi đăng. "
                + (request.contentType() == ContentType.INSTRUCTIONS
                        ? "Với hướng dẫn, tạo 6 đến 10 bước tuần tự: chuẩn bị dụng cụ và nguyên liệu, sơ chế, làm nóng nồi/chảo, thứ tự cho nguyên liệu vào, thao tác nấu và dấu hiệu chín, nêm nếm, hoàn thiện thành phẩm. Mỗi bước mô tả thao tác cụ thể; không bịa định lượng. Thời gian/nhiệt độ thiếu trong dữ liệu phải ghi là Ước tính AI. "
                        : "Với mô tả, viết một đoạn ngắn gọn. ")
                + "Với hướng dẫn, dùng ingredientIds để tham chiếu các nguyên liệu tác giả đã nhập; nếu gợi ý thêm nguyên liệu, ghi rõ tên trong text để tác giả xem xét. "
                + "Trả JSON theo schema: description là phần giới thiệu; steps là các bước {text, ingredientIds}. "
                + "Mô tả tối đa 2.000 ký tự; tổng hướng dẫn sau khi ghép tên nguyên liệu tối đa 5.000 ký tự. "
                + "Không tạo bản nháp hoặc nội dung đăng tự động.\n"
                + "Loại nội dung: " + request.contentType() + "\nTên món: " + request.title()
                + "\nLoại ăn chay: " + request.vegetarianType() + "\nDanh mục: " + request.dishCategory()
                + "\nKhẩu phần: " + request.servings() + "\nNguyên liệu được phép:\n" + lines;
    }

    private GeneratedContent parse(String raw) {
        try {
            GeneratedContent content = objectMapper.readValue(raw, GeneratedContent.class);
            if (content == null) throw invalid("missing JSON object");
            return content;
        } catch (JacksonException exception) {
            throw invalid("JSON decoding failed: " + exception.getClass().getSimpleName());
        }
    }

    private String validateDescription(String description) {
        String content = description == null ? "" : description.strip();
        if (content.isBlank() || content.length() > 2000) throw invalid("description length=" + content.length());
        return content;
    }

    private String validateInstructions(List<GeneratedStep> steps, List<IngredientResponse> ingredients) {
        if (steps == null || steps.size() < 6 || steps.size() > 10) {
            throw invalid("step count=" + (steps == null ? "null" : steps.size()));
        }
        var names = ingredients.stream().collect(java.util.stream.Collectors.toMap(IngredientResponse::id,
                IngredientResponse::name));
        String content = java.util.stream.IntStream.range(0, steps.size()).mapToObj(index -> {
            GeneratedStep step = steps.get(index);
            if (step == null || step.text() == null || step.text().isBlank()) {
                throw invalid("invalid step text");
            }
            List<Long> ingredientIds = step.ingredientIds() == null ? List.of() : step.ingredientIds();
            String used = ingredientIds.stream().filter(java.util.Objects::nonNull).distinct().map(names::get)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.joining(", "));
            return (index + 1) + ". " + step.text().strip() + (used.isBlank() ? "" : " (" + used + ")");
        }).collect(java.util.stream.Collectors.joining("\n"));
        if (content.length() < 10 || content.length() > 5000) throw invalid("instructions length=" + content.length());
        return content;
    }

    private AppException invalid(String reason) {
        log.warn("Recipe AI response rejected: {}", reason);
        return new AppException(ErrorCode.AI_RESPONSE_INVALID);
    }

    private record GeneratedContent(String description, List<GeneratedStep> steps) { }
    private record GeneratedStep(String text, List<Long> ingredientIds) { }
}
