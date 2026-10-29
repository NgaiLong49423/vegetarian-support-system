package tech.mamxanh.nutrition.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.nutrition.dto.response.PublicIngredientGroupResponse;
import tech.mamxanh.nutrition.dto.response.PublicIngredientDetailResponse;
import tech.mamxanh.nutrition.dto.response.PublicIngredientResponse;
import tech.mamxanh.nutrition.dto.response.IngredientUnitOptionResponse;
import tech.mamxanh.nutrition.service.IngredientCatalogService;

/** Public read-only browsing of active ingredient groups and their catalog entries (FR-62). */
@RestController
@Validated
@RequestMapping("/api/v1/ingredients")
public class PublicIngredientBrowseController {
    private final IngredientCatalogService service;

    public PublicIngredientBrowseController(IngredientCatalogService service) { this.service = service; }

    @GetMapping("/{ingredientId}/units")
    @Operation(summary = "List recipe units supported by an active ingredient")
    public List<IngredientUnitOptionResponse> units(@PathVariable @Positive long ingredientId) {
        return service.listRecipeUnits(ingredientId);
    }

    @GetMapping("/groups")
    @Operation(summary = "List active ingredient groups")
    public List<PublicIngredientGroupResponse> groups() {
        return service.listPublicGroups();
    }

    @GetMapping("/{ingredientId}")
    @Operation(summary = "Get public details for an active ingredient")
    @ApiResponse(responseCode = "200", description = "Active ingredient with available per-100 g nutrition.")
    @ApiResponse(responseCode = "404", description = "INGREDIENT_NOT_FOUND: ingredient does not exist or is inactive.")
    public PublicIngredientDetailResponse ingredient(@PathVariable @Positive long ingredientId) {
        return service.getPublicIngredient(ingredientId);
    }

    @GetMapping
    @Operation(summary = "Search active public ingredients, optionally within a group")
    public PageResponse<PublicIngredientResponse> ingredients(
            @RequestParam(required = false) @Size(max = 100) String query,
            @RequestParam(required = false) @Size(max = 100) String group,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.searchPublicIngredients(query, group, page, size);
    }
}
