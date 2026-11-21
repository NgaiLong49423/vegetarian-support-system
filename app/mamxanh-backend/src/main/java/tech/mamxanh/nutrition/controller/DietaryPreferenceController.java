package tech.mamxanh.nutrition.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.nutrition.dto.request.SaveDietaryPreferencesRequest;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse;
import tech.mamxanh.nutrition.dto.response.IngredientSuggestionResponse;
import tech.mamxanh.nutrition.dto.response.OnboardingInvitationResponse;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.nutrition.service.DietaryConstraintEvaluator;
import tech.mamxanh.nutrition.service.DietarySafetyAssessmentService;

import java.util.List;

/** FR-31: the signed-in Member's own dietary preferences and Onboarding state. */
@RestController
@RequestMapping(path = "/api/v1/nutrition/dietary-preferences", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class DietaryPreferenceController {

    private final DietaryPreferenceService service;
    private final DietarySafetyAssessmentService safety;

    public DietaryPreferenceController(DietaryPreferenceService service, DietarySafetyAssessmentService safety) {
        this.service = service;
        this.safety = safety;
    }

    @GetMapping
    public DietaryPreferencesResponse getOwnPreferences() {
        return service.getOwnPreferences();
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public DietaryPreferencesResponse saveOwnPreferences(@Valid @RequestBody SaveDietaryPreferencesRequest request) {
        return service.saveOwnPreferences(request);
    }

    @PostMapping("/onboarding/skip")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void skipOnboarding() {
        service.skipOnboarding();
    }

    @PostMapping("/onboarding/invitation")
    public OnboardingInvitationResponse claimOnboardingInvitation() {
        return service.claimOnboardingInvitation();
    }

    @GetMapping("/ingredient-suggestions")
    public List<IngredientSuggestionResponse> suggestIngredients(@RequestParam(defaultValue = "") String query) {
        return service.suggestIngredients(query);
    }

    @GetMapping("/recipes/{recipeId}/safety-assessment")
    public DietaryConstraintEvaluator.Assessment assessRecipe(@PathVariable long recipeId) {
        return safety.assessCurrentRecipe(recipeId);
    }
}
