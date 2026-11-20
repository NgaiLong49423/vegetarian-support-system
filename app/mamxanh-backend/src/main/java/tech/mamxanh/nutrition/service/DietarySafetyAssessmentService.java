package tech.mamxanh.nutrition.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tech.mamxanh.recipe.service.RecipePostService;

import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/** Loads only the caller's profile and current published recipe before using the shared evaluator. */
@Service
public class DietarySafetyAssessmentService {
    private final DietaryPreferenceService preferences;
    private final IngredientCatalogLookupService catalog;
    private final RecipePostService recipes;
    private final DietaryConstraintEvaluator evaluator;

    public DietarySafetyAssessmentService(DietaryPreferenceService preferences, IngredientCatalogLookupService catalog,
            RecipePostService recipes, DietaryConstraintEvaluator evaluator) {
        this.preferences = preferences;
        this.catalog = catalog;
        this.recipes = recipes;
        this.evaluator = evaluator;
    }

    @Transactional(readOnly = true)
    public DietaryConstraintEvaluator.Assessment assessCurrentRecipe(long recipeId) {
        DietaryPreferencesResponse profile = preferences.getOwnPreferences();
        RecipePostResponse recipe = recipe(recipeId);
        LocalDateTime version = preferences.currentProfileVersion();
        var assessment = assess(recipe, profile);
        return assessment.withAssessmentToken(token(recipe, profile, version));
    }

    @Transactional(readOnly = true)
    public DietaryConstraintEvaluator.Assessment assess(RecipePostResponse recipe, DietaryPreferencesResponse profile) {
        Collection<Long> ids = recipe.ingredients().stream().map(RecipePostResponse.Ingredient::ingredientId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, IngredientResponse> details = ids.isEmpty() ? Map.of()
                : catalog.findIngredientReferences(ids).stream().filter(item -> item.id() != null)
                        .collect(Collectors.toMap(IngredientResponse::id, item -> item, (first, ignored) -> first));
        return evaluator.evaluate(recipe, profile, details);
    }

    @Transactional(readOnly = true)
    public void requireRiskAcknowledged(long recipeId, Boolean acknowledged, String assessmentToken) {
        var assessment = assessCurrentRecipe(recipeId);
        if (assessment.requiresRiskAcknowledgement() && (!Boolean.TRUE.equals(acknowledged)
                || assessment.assessmentToken() == null || !assessment.assessmentToken().equals(assessmentToken))) {
            throw new AppException(ErrorCode.DIETARY_RISK_ACKNOWLEDGEMENT_REQUIRED);
        }
    }

    private static String token(RecipePostResponse recipe, DietaryPreferencesResponse profile, LocalDateTime version) {
        String source = version + "|" + profile.vegetarianType() + "|" + recipe.id() + "|" + recipe.vegetarianType()
                + "|" + recipe.ingredients().stream().map(line -> line.ingredientId() + ":" + line.name())
                        .collect(Collectors.joining(";"))
                + "|A:" + profile.avoid().items().stream().map(item -> item.ingredientId() + ":" + item.name())
                        .collect(Collectors.joining(";"))
                + "|D:" + profile.dislike().items().stream().map(item -> item.ingredientId() + ":" + item.name())
                        .collect(Collectors.joining(";"));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private RecipePostResponse recipe(long recipeId) {
        List<RecipePostResponse> found = recipes.getPublishedCards(List.of(recipeId));
        if (found.isEmpty()) throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        return found.getFirst();
    }
}
