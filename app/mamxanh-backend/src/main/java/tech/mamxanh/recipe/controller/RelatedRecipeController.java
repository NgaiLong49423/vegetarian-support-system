package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.recipe.dto.response.RelatedRecipesResponse;
import tech.mamxanh.recipe.service.RelatedRecipeService;

@RestController
@RequestMapping("/api/v1/recipes/{recipeId}/related")
@Validated
public class RelatedRecipeController {
    private final RelatedRecipeService service;

    public RelatedRecipeController(RelatedRecipeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Find related public recipes without AI",
            description = "Partial attribute matches ranked by similarity; main ingredients have the largest converted gram weight.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Related recipes, possibly empty."),
            @ApiResponse(responseCode = "400", description = "Invalid ID or limit."),
            @ApiResponse(responseCode = "404", description = "Source recipe is unavailable.")})
    public RelatedRecipesResponse related(@PathVariable @Min(1) long recipeId,
            @RequestParam(defaultValue = "6") @Min(1) @Max(12) int limit) {
        return service.related(recipeId, limit);
    }

    @PostMapping("/ai")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Ask Gemini to select existing pairing or variation recipes",
            description = "Active Customer/Expert with Plus/Pro required before calling Gemini. No recipe or meal plan is written.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Validated public recipes, possibly empty."),
            @ApiResponse(responseCode = "400", description = "Invalid ID or limit."),
            @ApiResponse(responseCode = "401", description = "Authentication required."),
            @ApiResponse(responseCode = "403", description = "Member role and active Plus/Pro required."),
            @ApiResponse(responseCode = "404", description = "Source recipe is unavailable."),
            @ApiResponse(responseCode = "502", description = "Invalid Gemini selection."),
            @ApiResponse(responseCode = "503", description = "Gemini unavailable.")})
    public RelatedRecipesResponse recommend(@PathVariable @Min(1) long recipeId,
            @RequestParam(defaultValue = "6") @Min(1) @Max(12) int limit) {
        return service.recommend(recipeId, limit);
    }
}
