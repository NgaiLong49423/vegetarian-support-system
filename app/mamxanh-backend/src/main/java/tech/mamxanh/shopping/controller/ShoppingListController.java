package tech.mamxanh.shopping.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.shopping.dto.request.*;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse;
import tech.mamxanh.shopping.service.ShoppingListService;
import tech.mamxanh.shopping.service.ShoppingListCreationService;
import tech.mamxanh.shopping.dto.response.ShoppingListCreationResponse;

@RestController
@RequestMapping("/api/v1/shopping-lists")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid ID, pagination or payload.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "A valid access token is required.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "An active Member account is required.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "List/item does not exist or does not belong to the current Member/list.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
public class ShoppingListController {
    private final ShoppingListService service;
    private final ShoppingListCreationService creation;
    private final tech.mamxanh.shopping.service.RecipeShoppingListService recipeLists;
    private final tech.mamxanh.shopping.service.ShoppingRecipeSourceService recipeSources;
    public ShoppingListController(ShoppingListService service, ShoppingListCreationService creation,
            tech.mamxanh.shopping.service.RecipeShoppingListService recipeLists,
            tech.mamxanh.shopping.service.ShoppingRecipeSourceService recipeSources) {
        this.service = service;
        this.creation = creation;
        this.recipeLists = recipeLists;
        this.recipeSources = recipeSources;
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @Operation(operationId = "createEmptyShoppingList", summary = "Create an empty shopping list",
            description = "Creates a standalone MANUAL checklist for the authenticated Member. The name is trimmed and limited to 200 characters. No meal plan or recipe is changed.")
    @ApiResponse(responseCode = "201", description = "The empty checklist was created.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShoppingListResponse.class)))
    public ResponseEntity<ShoppingListResponse> createEmpty(@Valid @RequestBody CreateEmptyShoppingListRequest request) {
        var result = service.createEmpty(request);
        return ResponseEntity.created(URI.create("/api/v1/shopping-lists/" + result.id())).body(result);
    }

    @PostMapping(value = "/from-recipe", consumes = "application/json", produces = "application/json")
    @Operation(operationId = "createShoppingListFromRecipe", summary = "Create a named checklist from one public recipe",
            description = "Creates a MANUAL checklist and adds the selected ingredients from one public recipe plus optional manual items in one transaction. Omit selectedIngredientIds to include all recipe ingredients; provide an empty array only when manualItems contains at least one item. Optional servings scales selected quantities from the published serving count (1–50). A failed addition rolls back the new list.")
    @ApiResponse(responseCode = "201", description = "The new checklist and recipe ingredients.", useReturnTypeSchema = true)
    public ResponseEntity<ShoppingListCreationResponse> fromRecipe(
            @Valid @RequestBody CreateShoppingListFromRecipeRequest request) {
        var result = recipeLists.createFromRecipe(request);
        return ResponseEntity.created(URI.create("/api/v1/shopping-lists/" + result.shoppingList().id())).body(result);
    }

    @PostMapping(value = "/{listId}/recipes", consumes = "application/json", produces = "application/json")
    @Operation(operationId = "addRecipeToShoppingList", summary = "Add one public recipe to an owned checklist",
            description = "Adds selected ingredients from one public recipe and optional manual items to any checklist owned by the Member. Omit selectedIngredientIds to include all recipe ingredients; provide an empty array only when manualItems contains at least one item. Repeated additions do not duplicate recipe contributions. Optional servings scales selected quantities from the published serving count (1–50). Saved edits are retained as the base for aggregation.")
    @ApiResponse(responseCode = "200", description = "The updated checklist and recipe diagnostics.", useReturnTypeSchema = true)
    public ShoppingListCreationResponse addRecipe(@PathVariable long listId,
            @Valid @RequestBody AddRecipeToShoppingListRequest request) {
        return recipeLists.addRecipe(listId, request);
    }

    @PutMapping("/{listId}/recipe-sources/{sourceId}/servings")
    @Operation(operationId = "updateShoppingRecipeSourceServings", summary = "Change recipe servings in one shopping list",
            description = "Changes only the selected shopping-list recipe source and recalculates linked quantities; the meal plan and recipe post are not changed.")
    @ApiResponse(responseCode = "200", description = "Updated shopping list", useReturnTypeSchema = true)
    public ShoppingListResponse updateRecipeSourceServings(@PathVariable long listId, @PathVariable UUID sourceId,
            @Valid @RequestBody UpdateShoppingRecipeSourceServingsRequest request) {
        return recipeSources.updateServings(listId, sourceId, request);
    }

    @PutMapping("/{listId}/recipes/{recipeId}/servings")
    @Operation(operationId = "updateShoppingRecipeServings", summary = "Change grouped servings of a recipe in one shopping list",
            description = "Changes the combined servings for all occurrences of one recipe in this checklist, recalculates only their linked ingredients, and leaves the meal plan and recipe post unchanged.")
    @ApiResponse(responseCode = "200", description = "Updated shopping list", useReturnTypeSchema = true)
    public ShoppingListResponse updateRecipeServings(@PathVariable long listId, @PathVariable long recipeId,
            @Valid @RequestBody UpdateShoppingRecipeSourceServingsRequest request) {
        return recipeSources.updateRecipeServings(listId, recipeId, request);
    }

    @DeleteMapping("/{listId}/recipes/{recipeId}")
    @Operation(operationId = "removeShoppingRecipe", summary = "Remove a recipe and its contributions from one shopping list",
            description = "Removes all complete occurrences of the recipe from the owned checklist. Recipe posts and meal-plan entries are unchanged; ingredient items are recalculated from their remaining sources and removed when no sources remain.")
    @ApiResponse(responseCode = "200", description = "The updated checklist.", useReturnTypeSchema = true)
    public ShoppingListResponse removeRecipe(@PathVariable long listId, @PathVariable long recipeId) {
        return recipeSources.removeRecipe(listId, recipeId);
    }

    @PutMapping("/{listId}/manual-sources/{sourceId}")
    @Operation(operationId = "updateManualShoppingSource", summary = "Edit a manually added amount in a shopping list")
    @ApiResponse(responseCode = "200", description = "The recalculated item.", useReturnTypeSchema = true)
    public ShoppingListResponse.Item updateManualSource(@PathVariable long listId, @PathVariable UUID sourceId,
            @Valid @RequestBody UpdateShoppingItemRequest request) {
        return service.updateManualSource(listId, sourceId, request);
    }

    @DeleteMapping("/{listId}/manual-sources/{sourceId}")
    @Operation(operationId = "deleteManualShoppingSource", summary = "Remove one manually added amount from a merged item")
    @ApiResponse(responseCode = "204", description = "The manual addition was removed.", content = @Content)
    public ResponseEntity<Void> deleteManualSource(@PathVariable long listId, @PathVariable UUID sourceId) {
        service.deleteManualSource(listId, sourceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/from-meal-plan")
    @Operation(operationId = "createShoppingListFromMealPlan", summary = "Create an owned checklist from a meal-plan week",
            description = "Omit selectedDates for the whole week. Quantities are scaled to planned servings and aggregated by FR-54. Unavailable recipes/invalid source lines produce warnings; an empty result or a quantity that cannot be stored safely returns 400 without creating a list. Creates a new independent list; does not update the personal Recipe Post list.")
    @ApiResponse(responseCode = "201", description = "Checklist persisted with unbought items and creation diagnostics.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShoppingListCreationResponse.class)))
    public ResponseEntity<ShoppingListCreationResponse> fromMealPlan(@Valid @RequestBody CreateMealPlanShoppingListRequest request) {
        var result = creation.createFromMealPlan(request);
        return ResponseEntity.created(URI.create("/api/v1/shopping-lists/" + result.shoppingList().id())).body(result);
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "The owned shopping lists page.", useReturnTypeSchema = true)
    @Operation(operationId = "listShoppingLists", summary = "List the current Member's shopping lists", description = "page >= 0; size 1–50, default 20. Ordered by updatedAt descending, then ID descending.")
    public PageResponse<ShoppingListResponse.Summary> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.list(page, size); }

    @GetMapping("/options")
    @ApiResponse(responseCode = "200", description = "Active units for manual items.", useReturnTypeSchema = true)
    @Operation(operationId = "getShoppingOptions", summary = "Read active shopping item units")
    public List<UnitResponse> options() { return service.options(); }

    @GetMapping("/{listId}")
    @ApiResponse(responseCode = "200", description = "The saved checklist.", useReturnTypeSchema = true)
    @Operation(operationId = "getShoppingList", summary = "Read an owned shopping checklist", description = "Existing inactive catalog references remain readable. The account is derived from authentication.")
    public ShoppingListResponse get(@PathVariable long listId) { return service.get(listId); }

    @PatchMapping("/{listId}")
    @ApiResponse(responseCode = "200", description = "The renamed owned shopping list.", useReturnTypeSchema = true)
    @Operation(operationId = "renameShoppingList", summary = "Rename an owned shopping list", description = "The account is derived from authentication. Names are trimmed, non-blank and limited to 200 characters.")
    public ShoppingListResponse.Summary rename(@PathVariable long listId,
            @Valid @RequestBody RenameShoppingListRequest request) {
        return service.rename(listId, request);
    }

    @DeleteMapping("/{listId}")
    @Operation(operationId = "deleteShoppingList", summary = "Delete an owned shopping list", description = "Deletes the checklist and its recipe contribution records. Meal plans and source recipes are preserved.")
    @ApiResponse(responseCode = "204", description = "The list was deleted.", content = @Content)
    public ResponseEntity<Void> deleteList(@PathVariable long listId) {
        service.deleteList(listId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{listId}/items")
    @Operation(operationId = "addShoppingItem", summary = "Add a manual item to an owned list", description = "Name is plain text. Quantity is positive, with at most 8 integer and 2 fractional digits; the unit must be active. New items start unbought. This operation does not perform FR-54 aggregation.")
    @ApiResponse(responseCode = "201", description = "The manual item was saved.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShoppingListResponse.Item.class)))
    public ResponseEntity<ShoppingListResponse.Item> add(@PathVariable long listId,
            @Valid @RequestBody CreateShoppingItemRequest request) {
        var item = service.add(listId, request);
        return ResponseEntity.created(URI.create("/api/v1/shopping-lists/" + listId + "/items/" + item.id())).body(item);
    }

    @PutMapping("/{listId}/items/{itemId}")
    @ApiResponse(responseCode = "200", description = "The updated item.", useReturnTypeSchema = true)
    @Operation(operationId = "updateShoppingItem", summary = "Edit a shopping item's quantity and unit", description = "Preserves ingredient identity and bought state. An existing inactive unit may be retained; a newly selected unit must be active.")
    public ShoppingListResponse.Item update(@PathVariable long listId, @PathVariable long itemId,
            @Valid @RequestBody UpdateShoppingItemRequest request) { return service.update(listId, itemId, request); }

    @PatchMapping("/{listId}/items/{itemId}/bought")
    @ApiResponse(responseCode = "200", description = "The saved bought state.", useReturnTypeSchema = true)
    @Operation(operationId = "setShoppingItemBought", summary = "Set a shopping item's bought state", description = "Explicit boolean assignment, not toggle; repeating the same state has no effect on updatedAt.")
    public ShoppingListResponse.Item setBought(@PathVariable long listId, @PathVariable long itemId,
            @Valid @RequestBody ShoppingItemBoughtRequest request) { return service.setBought(listId, itemId, request.bought()); }

    @DeleteMapping("/{listId}/items/{itemId}")
    @Operation(operationId = "deleteShoppingItem", summary = "Delete one item from an owned shopping list")
    @ApiResponse(responseCode = "204", description = "The item was deleted.", content = @Content)
    public ResponseEntity<Void> delete(@PathVariable long listId, @PathVariable long itemId) {
        service.delete(listId, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{listId}/items", params = "bought=true")
    @Operation(operationId = "clearBoughtShoppingItems", summary = "Remove bought items from an owned list", description = "Requires bought=true. Unbought items and other lists are preserved.")
    @ApiResponse(responseCode = "204", description = "Bought items were removed; repeating cleanup is safe.", content = @Content)
    public ResponseEntity<Void> clearBought(@PathVariable long listId) {
        service.clearBought(listId);
        return ResponseEntity.noContent().build();
    }
}
