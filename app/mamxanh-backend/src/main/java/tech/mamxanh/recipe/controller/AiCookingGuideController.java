package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.recipe.dto.request.AiCookingGuideRequest;
import tech.mamxanh.recipe.dto.response.AiCookingGuideResponse;
import tech.mamxanh.recipe.service.AiCookingGuideService;

@RestController
@RequestMapping("/api/v1/recipes/ai")
public class AiCookingGuideController {
    private final AiCookingGuideService service;

    public AiCookingGuideController(AiCookingGuideService service) {
        this.service = service;
    }

    @PostMapping(path = "/cooking-guide", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Generate a detailed AI cooking guide from a published recipe",
            description = "Requires an active Member with an eligible dietary profile. Uses only the published source recipe and does not modify it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A detailed, source-linked cooking guide was generated."),
            @ApiResponse(responseCode = "404", description = "The recipe is unavailable or conflicts with the member's dietary restrictions."),
            @ApiResponse(responseCode = "409", description = "The member's required dietary profile is incomplete."),
            @ApiResponse(responseCode = "502", description = "The generated cooking guide did not satisfy the source constraints."),
            @ApiResponse(responseCode = "503", description = "Gemini is not configured or unavailable.")
    })
    public AiCookingGuideResponse generate(@Valid @RequestBody AiCookingGuideRequest request) {
        return service.generate(request.recipeId());
    }
}