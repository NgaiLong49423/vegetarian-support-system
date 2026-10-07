package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.recipe.dto.response.SavedRecipeResponse;
import tech.mamxanh.recipe.service.SavedRecipeReadService;

@RestController
@RequestMapping(path = "/api/v1/saved-recipes", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class SavedRecipeReadController {
    private final SavedRecipeReadService service;
    public SavedRecipeReadController(SavedRecipeReadService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "List the current member's saved recipes", description = "Read-only paginated projection. The user is derived from the authenticated principal; unavailable recipes are returned as safe tombstones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The current member's saved recipe page."),
            @ApiResponse(responseCode = "401", description = "Authentication is required."),
            @ApiResponse(responseCode = "400", description = "Page or size is outside its supported range.")
    })
    public PageResponse<SavedRecipeResponse> list(
            @Parameter(description = "Zero-based page index.", schema = @Schema(minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, from 1 through 50.", schema = @Schema(minimum = "1", maximum = "50"))
            @RequestParam(defaultValue = "20") int size) {
        return service.list(page, size);
    }
}
