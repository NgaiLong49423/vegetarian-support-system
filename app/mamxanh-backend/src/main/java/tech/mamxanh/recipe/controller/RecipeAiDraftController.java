package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import tech.mamxanh.recipe.dto.request.RecipeAiDraftRequest;
import tech.mamxanh.recipe.dto.response.RecipeAiDraftResponse;
import tech.mamxanh.recipe.service.RecipeAiDraftService;

@RestController
@RequestMapping("/api/v1/recipes/ai/draft")
@SecurityRequirement(name = "bearerAuth")
public class RecipeAiDraftController {
    private final RecipeAiDraftService service;
    public RecipeAiDraftController(RecipeAiDraftService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasRole('EXPERT')")
    @Operation(summary = "Preview an AI recipe with proposed ingredient quantities",
            description = "Requires an active Expert with Pro. Resolves entered ingredient names against the active catalog, asks Gemini for gram quantities and text, and recalculates calories using catalog energy values. Target calories are a reference, not a guaranteed medical target. Does not save or publish recipes.")
    @ApiResponse(responseCode = "200", description = "Editable recipe preview with catalog IDs, gram units, actual calculated calories and sources.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RecipeAiDraftResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input, unknown/ambiguous/duplicate ingredient names, missing energy data, or a catalog entry changed.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Active Expert role and Pro entitlement are required.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "AI_RESPONSE_INVALID: incomplete or invalid provider output.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "AI_PROVIDER_UNAVAILABLE: provider configuration, quota or timeout failure.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<RecipeAiDraftResponse> draft(@Valid @RequestBody RecipeAiDraftRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.generate(request));
    }
}
