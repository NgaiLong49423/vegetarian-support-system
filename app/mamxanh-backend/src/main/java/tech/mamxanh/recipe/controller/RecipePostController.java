package tech.mamxanh.recipe.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.recipe.dto.request.UpdateRecipePostRequest;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tech.mamxanh.recipe.service.RecipePostService;
import tech.mamxanh.recipe.service.RecipePostService.RecipePageResponse;
import tech.mamxanh.recipe.service.RecipePostService.RecipeReferenceData;
import tech.mamxanh.recipe.service.RecipeSortMode;
import tech.mamxanh.recipe.service.RecipeViewPeriod;

@RestController
@RequestMapping("/api/v1/recipes")
@Validated
public class RecipePostController {
    private final RecipePostService service;

    public RecipePostController(RecipePostService service) {
        this.service = service;
    }

    @GetMapping
    public RecipePageResponse searchPublic(
            @Parameter(description = "Tìm trong tiêu đề và mô tả ngắn công thức.")
            @RequestParam(defaultValue = "") String keyword,
            @Parameter(description = "Số trang bắt đầu từ 0.")
            @Schema(minimum = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số công thức mỗi trang (từ 1 đến 50).")
            @Schema(minimum = "1", maximum = "50")
            @RequestParam(defaultValue = "12") int size,
            @Parameter(description = "Chế độ sắp xếp công thức công khai.")
            @Schema(allowableValues = {"NEWEST", "MOST_LIKED", "MOST_VIEWED", "MOST_COMMENTED", "MOST_ACTIVE", "TRENDING"})
            @RequestParam(defaultValue = "NEWEST") String sort,
            @Parameter(description = "Khung thời gian cho MOST_VIEWED; mặc định ALL_TIME.")
            @Schema(allowableValues = {"ALL_TIME", "LAST_24_HOURS", "LAST_7_DAYS", "LAST_30_DAYS"})
            @RequestParam(defaultValue = "ALL_TIME") String viewPeriod,
            @Parameter(description = "Loại ăn chay chuẩn.")
            @Schema(allowableValues = {"VEGAN", "LACTO", "OVO", "LACTO_OVO"})
            @RequestParam(required = false) String vegetarianType,
            @Parameter(description = "Mã thể loại món chuẩn hóa.")
            @Schema(allowableValues = {"NOODLE_SOUP", "STIR_FRY", "HOT_POT", "BRAISED", "SOUP", "FRIED", "STEAMED", "SALAD", "ROLL", "GRILLED", "DESSERT"})
            @RequestParam(required = false) String dishCategory,
            @Parameter(description = "ID nguyên liệu; có thể lặp lại. Công thức phải chứa tất cả ID đã chọn.")
            @Size(max = 20)
            @RequestParam(required = false) java.util.List<@Min(1) Long> ingredientIds,
            @Parameter(description = "Tổng thời gian chuẩn bị và nấu tối đa, tính bằng phút.")
            @Min(1)
            @RequestParam(required = false) Integer maxTotalTimeMinutes) {
        return service.searchPublished(keyword, page, size,
                RecipeSortMode.from(sort), RecipeViewPeriod.from(viewPeriod), vegetarianType,
                dishCategory, ingredientIds, maxTotalTimeMinutes);
    }

    @GetMapping("/reference-data")
    public RecipeReferenceData referenceData(@RequestParam(defaultValue = "") String keyword) {
        return service.referenceData(keyword);
    }

    @GetMapping("/mine")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public RecipePageResponse listMine(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.listMine(page, size);
    }

    @GetMapping("/{recipeId}/manage")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public RecipePostResponse getForAuthor(@PathVariable @Min(1) long recipeId) {
        return service.getForAuthor(recipeId);
    }

    @PutMapping("/{recipeId}")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public RecipePostResponse update(@PathVariable @Min(1) long recipeId,
            @Valid @RequestBody UpdateRecipePostRequest request) {
        return service.update(recipeId, request);
    }

    @DeleteMapping("/{recipeId}")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<Void> delete(@PathVariable @Min(1) long recipeId) {
        service.delete(recipeId);
        return ResponseEntity.noContent().build();
    }
}
