package tech.mamxanh.recipe.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.recipe.dto.request.CreateRecipeRequest;
import tech.mamxanh.recipe.dto.response.CreateRecipeResponse;
import tech.mamxanh.recipe.dto.response.IngredientOptionResponse;
import tech.mamxanh.recipe.dto.response.RecipeFormOptionsResponse;
import tech.mamxanh.recipe.dto.response.RecipeDetailResponse;
import tech.mamxanh.recipe.service.RecipeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "/api/v1/recipes", produces = MediaType.APPLICATION_JSON_VALUE)
public class RecipeController {
    private final RecipeService service;

    public RecipeController(RecipeService service) { this.service = service; }

    @GetMapping("/form-options")
    public RecipeFormOptionsResponse formOptions() { return service.formOptions(); }

    @GetMapping("/ingredient-options")
    public List<IngredientOptionResponse> ingredientOptions(@RequestParam(defaultValue = "") String query) {
        return service.findIngredients(query);
    }

    @GetMapping("/{recipeId}")
    public RecipeDetailResponse recipeDetail(@PathVariable Long recipeId) {
        return service.getPublishedRecipe(recipeId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateRecipeResponse> publish(@Valid @RequestBody CreateRecipeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.publish(request));
    }
}
