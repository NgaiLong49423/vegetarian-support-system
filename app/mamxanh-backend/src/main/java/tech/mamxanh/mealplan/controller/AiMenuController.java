package tech.mamxanh.mealplan.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.mealplan.dto.request.ApplyAiMenuRequest;
import tech.mamxanh.mealplan.dto.request.CreateAiMenuPreviewRequest;
import tech.mamxanh.mealplan.dto.response.AiMenuPreviewResponse;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.mealplan.service.AiMenuService;

@RestController
@RequestMapping("/api/v1/meal-plans/ai-menu")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AiMenuController {
    private final AiMenuService service;

    public AiMenuController(AiMenuService service) { this.service = service; }

    @Operation(summary = "Generate a nutrition-aware weekly meal-plan preview",
            description = "Requires an active Member, Plus or Pro entitlement, FR-38 eligibility and a valid FR-35 profile. "
                    + "PROFILE uses the saved goal; THIS_WEEK uses optional request-only preferences. Does not write Meal Plan entries.")
    @ApiResponse(responseCode = "200", description = "A 7-day preview with multiple trusted recipes per meal and calculated nutrition.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AiMenuPreviewResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Plus or Pro entitlement is required; nutrition eligibility must be confirmed (NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED) and the account must be eligible (NUTRITION_ELIGIBILITY_INELIGIBLE).", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A valid anthropometric profile is required.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "The AI provider is unavailable.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/previews")
    public AiMenuPreviewResponse preview(@Valid @RequestBody CreateAiMenuPreviewRequest request) {
        return service.createPreview(request);
    }

    @Operation(summary = "Replace a member's entire week with selected AI preview meals",
            description = "Revalidates Plus or Pro, eligibility and every selected published nutrition-complete recipe. "
                    + "The operation is atomic; any invalid entry preserves the old week.")
    @ApiResponse(responseCode = "200", description = "The replaced week.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanWeekResponse.class)))
    @ApiResponse(responseCode = "400", description = "The selected positions or recipes are invalid.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Plus or Pro entitlement or nutrition eligibility is required.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "A selected recipe is no longer available.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping
    public MealPlanWeekResponse apply(@Valid @RequestBody ApplyAiMenuRequest request) {
        return service.apply(request);
    }
}
