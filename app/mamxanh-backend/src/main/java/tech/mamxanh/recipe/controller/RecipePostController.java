package tech.mamxanh.recipe.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

@RestController
@RequestMapping("/api/v1/recipes")
@Validated
public class RecipePostController {
    private final RecipePostService service;

    public RecipePostController(RecipePostService service) {
        this.service = service;
    }

    @GetMapping
    public RecipePageResponse searchPublic(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.searchPublished(keyword, page, size);
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
