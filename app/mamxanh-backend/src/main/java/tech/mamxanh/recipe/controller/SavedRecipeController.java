package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.recipe.dto.response.SavedRecipeResponse;
import tech.mamxanh.recipe.service.SavedRecipeReadService;
import tech.mamxanh.recipe.service.SavedRecipeWriteService;

@RestController
@RequestMapping(path = "/api/v1/saved-recipes", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class SavedRecipeController {
    private final SavedRecipeReadService readService;
    private final SavedRecipeWriteService writeService;

    public SavedRecipeController(SavedRecipeReadService readService, SavedRecipeWriteService writeService) {
        this.readService = readService;
        this.writeService = writeService;
    }

    @GetMapping
    @Operation(summary = "List the current member's saved recipes",
            description = "Returns a private, paginated list ordered by most recently saved. Unavailable recipes are safe tombstones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The current member's saved recipe page."),
            @ApiResponse(responseCode = "401", description = "Authentication is required."),
            @ApiResponse(responseCode = "400", description = "Keyword, page or size is outside its supported range.")
    })
    public PageResponse<SavedRecipeResponse> list(
            @Parameter(description = "Search available saved recipes by title or description; maximum 120 characters.")
            @RequestParam(defaultValue = "") String keyword,
            @Parameter(description = "Zero-based page index.", schema = @Schema(minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, from 1 through 50.", schema = @Schema(minimum = "1", maximum = "50"))
            @RequestParam(defaultValue = "20") int size) {
        return readService.list(keyword, page, size);
    }

    @PutMapping("/{recipeId}")
    @Operation(summary = "Save a published recipe for the current member",
            description = "Idempotent. The account ID is derived from the authenticated principal.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "The recipe is saved or was already saved."),
            @ApiResponse(responseCode = "401", description = "Authentication is required."),
            @ApiResponse(responseCode = "403", description = "An active Member account is required."),
            @ApiResponse(responseCode = "404", description = "The recipe is not public and available.")
    })
    public ResponseEntity<Void> save(@PathVariable long recipeId) {
        writeService.save(recipeId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{recipeId}")
    @Operation(summary = "Remove a recipe from the current member's saved list",
            description = "Idempotent. Removes only the current member's reference, including an unavailable recipe.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "The saved reference is removed or did not exist."),
            @ApiResponse(responseCode = "401", description = "Authentication is required."),
            @ApiResponse(responseCode = "403", description = "An active Member account is required.")
    })
    public ResponseEntity<Void> unsave(@PathVariable long recipeId) {
        writeService.unsave(recipeId);
        return ResponseEntity.noContent().build();
    }
}
