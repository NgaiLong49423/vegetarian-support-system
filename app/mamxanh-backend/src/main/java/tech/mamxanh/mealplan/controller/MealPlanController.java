package tech.mamxanh.mealplan.controller;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import tech.mamxanh.mealplan.dto.request.AddMealPlanEntryRequest;
import tech.mamxanh.mealplan.dto.request.ReplaceMealPlanEntryRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.mealplan.service.MealPlanService;

@RestController
@RequestMapping("/api/v1/meal-plans")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponse(responseCode = "400", description = "Invalid date, meal type or request fields.",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(responseCode = "401", description = "Authentication is required.",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(responseCode = "403", description = "An active Member and ownership of the entry are required.",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
public class MealPlanController {
    private final MealPlanService service;

    public MealPlanController(MealPlanService service) {
        this.service = service;
    }

    @Operation(summary = "Create or open the current member's weekly meal plan",
            description = "Idempotent for the authenticated member and Monday weekStartDate. "
                    + "Preserves existing entries. No request body is required; the owner is derived from authentication.")
    @ApiResponse(responseCode = "200", description = "The existing or newly created week.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanWeekResponse.class)))
    @ApiResponse(responseCode = "400", description = "The date is missing, malformed or is not Monday.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Authentication is required.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "An active Member account is required.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping
    public MealPlanWeekResponse openWeek(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate) {
        return service.openWeek(weekStartDate);
    }

    @Operation(summary = "Add a published recipe to the member's meal",
            description = "Creates the week if needed. Default plannedServings is 1. Different recipes may share a meal.")
    @ApiResponse(responseCode = "201", description = "The new meal-plan entry.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanWeekResponse.Entry.class)))
    @ApiResponse(responseCode = "404", description = "The recipe is unavailable.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "MEAL_PLAN_RECIPE_DUPLICATE: recipe already belongs to this day/meal.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{weekStartDate}/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public MealPlanWeekResponse.Entry addEntry(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @Valid @RequestBody AddMealPlanEntryRequest request) {
        return service.addEntry(weekStartDate, request);
    }

    @Operation(summary = "Replace the recipe in an owned meal-plan entry",
            description = "Preserves the entry's day, meal type and planned servings. Also supports replacing a tombstone.")
    @ApiResponse(responseCode = "200", description = "The updated entry.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanWeekResponse.Entry.class)))
    @ApiResponse(responseCode = "404", description = "The replacement recipe is unavailable.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "MEAL_PLAN_RECIPE_DUPLICATE: recipe already belongs to this day/meal.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{weekStartDate}/entries/{entryId}")
    public MealPlanWeekResponse.Entry replaceEntry(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @PathVariable @Min(1) long entryId, @Valid @RequestBody ReplaceMealPlanEntryRequest request) {
        return service.replaceEntry(weekStartDate, entryId, request.recipeId());
    }

    @Operation(summary = "Remove an owned entry, including an unavailable recipe reference",
            description = "Removes only the meal-plan entry. Does not delete the recipe or unsave it.")
    @ApiResponse(responseCode = "204", description = "Entry removed.", content = @Content)
    @DeleteMapping("/{weekStartDate}/entries/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEntry(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @PathVariable @Min(1) long entryId) {
        service.removeEntry(weekStartDate, entryId);
    }
}
