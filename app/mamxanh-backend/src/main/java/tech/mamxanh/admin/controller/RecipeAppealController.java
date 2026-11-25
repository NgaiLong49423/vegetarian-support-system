package tech.mamxanh.admin.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.mamxanh.admin.dto.RecipeAppealRequest;
import tech.mamxanh.admin.dto.RecipeAppealResponse;
import tech.mamxanh.admin.service.RecipeAppealService;
import tech.mamxanh.common.config.OpenApiConfig;

/** An Expert appeals the hide decision on their own Recipe Post (FR-28, BR-27). */
@RestController
@Validated
@RequestMapping("/api/v1/recipes/{recipeId}/appeal")
@PreAuthorize("hasRole('EXPERT')")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class RecipeAppealController {
    private final RecipeAppealService service;

    public RecipeAppealController(RecipeAppealService service) {
        this.service = service;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hide reason and appeal state; appealStatus is null before an appeal."),
            @ApiResponse(responseCode = "403", description = "RECIPE_EDIT_NOT_ALLOWED: another author's post or the caller is not an active Expert."),
            @ApiResponse(responseCode = "404", description = "RECIPE_NOT_FOUND: no such post. APPEAL_NOT_FOUND: the post is not hidden.")
    })
    @GetMapping
    public RecipeAppealResponse getRecipeAppeal(@PathVariable @Min(1) long recipeId) {
        return service.get(recipeId);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "The appeal is PENDING."),
            @ApiResponse(responseCode = "403", description = "RECIPE_EDIT_NOT_ALLOWED: another author's post or the caller is not an active Expert."),
            @ApiResponse(responseCode = "404", description = "RECIPE_NOT_FOUND or APPEAL_NOT_FOUND: the post is not hidden."),
            @ApiResponse(responseCode = "409", description = "APPEAL_NOT_ALLOWED: this hide decision was already appealed.")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeAppealResponse submitRecipeAppeal(@PathVariable @Min(1) long recipeId,
            @Valid @RequestBody RecipeAppealRequest request) {
        return service.submit(recipeId, request);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The edited post is RESUBMITTED for review and stays hidden."),
            @ApiResponse(responseCode = "403", description = "RECIPE_EDIT_NOT_ALLOWED: another author's post or the caller is not an active Expert."),
            @ApiResponse(responseCode = "404", description = "RECIPE_NOT_FOUND or APPEAL_NOT_FOUND: the post is not hidden."),
            @ApiResponse(responseCode = "409", description = "APPEAL_STATE_INVALID: the Administrator has not allowed an edit.")
    })
    @PostMapping("/resubmit")
    public RecipeAppealResponse resubmitRecipeAppeal(@PathVariable @Min(1) long recipeId) {
        return service.resubmit(recipeId);
    }
}
