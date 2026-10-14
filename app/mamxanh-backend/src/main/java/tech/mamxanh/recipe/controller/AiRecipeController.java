package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.recipe.dto.request.AiRecipeRecommendationRequest;
import tech.mamxanh.recipe.dto.response.AiRecipeRecommendationResponse;
import tech.mamxanh.recipe.dto.response.AiWeeklyMenuPreviewResponse;
import tech.mamxanh.recipe.service.AiRecipeRecommendationService;

@RestController
@RequestMapping("/api/v1/recipes/ai")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AiRecipeController {
    private final AiRecipeRecommendationService service;

    public AiRecipeController(AiRecipeRecommendationService service) {
        this.service = service;
    }

    @Operation(summary = "Recommend public recipes from available ingredients",
            description = "Requires an active Member and a complete FR-31 profile. Returns 3–5 currently published "
                    + "recipes selected from a safety-filtered catalog; more ingredient matches rank higher.")
    @ApiResponse(responseCode = "200", description = "Recipe suggestions, possibly empty when no suitable recipe exists.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AiRecipeRecommendationResponse.class)))
    @ApiResponse(responseCode = "409", description = "The FR-31 profile is incomplete.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "Gemini is unavailable.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/recommendations")
    public AiRecipeRecommendationResponse recommend(@Valid @RequestBody AiRecipeRecommendationRequest request) {
        return service.recommend(request.ingredientIds());
    }

    @Operation(summary = "Generate a 7-day AI meal-plan preview",
            description = "Requires an active Pro Member and complete FR-31 profile. Returns 21 slots without "
                    + "writing the member's Meal Plan. Recipe IDs may repeat, but adjacent slots cannot.")
    @ApiResponse(responseCode = "200", description = "The generated 21-slot preview.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AiWeeklyMenuPreviewResponse.class)))
    @ApiResponse(responseCode = "403", description = "An active Pro subscription is required.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Profile incomplete or not enough suitable recipes.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "Gemini is unavailable.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/weekly-menu")
    public AiWeeklyMenuPreviewResponse weeklyMenu(@Valid @RequestBody AiRecipeRecommendationRequest request) {
        return service.weeklyMenu(request.ingredientIds());
    }
}
