package tech.mamxanh.recipe.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.mamxanh.nutrition.service.RecipeNutritionService;
import tech.mamxanh.recipe.entity.RecipeIngredientEntity;
import tech.mamxanh.recipe.entity.RecipeIngredientReferenceEntity;
import tech.mamxanh.recipe.entity.RecipeUnitReferenceEntity;
import tech.mamxanh.recipe.repository.RecipeIngredientReferenceRepository;
import tech.mamxanh.recipe.repository.RecipeIngredientRepository;
import tech.mamxanh.recipe.repository.RecipePostRepository;
import tech.mamxanh.recipe.repository.RecipeUnitReferenceRepository;

/**
 * FR-40 AC-40.3 (BR-40, BR-50): a Recipe Post may be published with ingredients that have no
 * nutrition data, but only published recipes whose nutrition is complete may enter the candidate
 * pool of the nutrition AI menu (FR-36). Completeness uses the same calculation as the public
 * recipe detail ({@link RecipeNutritionService}).
 */
@Service
public class RecipeNutritionCandidateService {
    private final RecipePostRepository recipeRepository;
    private final RecipeIngredientRepository ingredientRepository;
    private final RecipeIngredientReferenceRepository ingredientReferenceRepository;
    private final RecipeUnitReferenceRepository unitReferenceRepository;
    private final RecipeNutritionService nutritionService;

    public RecipeNutritionCandidateService(RecipePostRepository recipeRepository,
            RecipeIngredientRepository ingredientRepository,
            RecipeIngredientReferenceRepository ingredientReferenceRepository,
            RecipeUnitReferenceRepository unitReferenceRepository, RecipeNutritionService nutritionService) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.ingredientReferenceRepository = ingredientReferenceRepository;
        this.unitReferenceRepository = unitReferenceRepository;
        this.nutritionService = nutritionService;
    }

    /** The given ids that are published and have complete nutrition, in the given order. */
    @Transactional(readOnly = true)
    public List<Long> retainNutritionComplete(Collection<Long> recipeIds) {
        return recipeIds.stream().filter(this::isPublishedWithCompleteNutrition).toList();
    }

    private boolean isPublishedWithCompleteNutrition(long recipeId) {
        return recipeRepository.findByIdAndStatus(recipeId, "PUBLISHED").map(recipe -> {
            List<RecipeIngredientEntity> lines = ingredientRepository.findAllByRecipeIdOrderByIdAsc(recipeId);
            Map<Long, RecipeIngredientReferenceEntity> ingredients = ingredientReferenceRepository
                    .findAllById(lines.stream().map(RecipeIngredientEntity::getIngredientId).filter(Objects::nonNull).distinct().toList())
                    .stream().collect(Collectors.toMap(RecipeIngredientReferenceEntity::getId, Function.identity()));
            Map<Integer, RecipeUnitReferenceEntity> units = unitReferenceRepository
                    .findAllById(lines.stream().map(RecipeIngredientEntity::getUnitId).distinct().toList())
                    .stream().collect(Collectors.toMap(RecipeUnitReferenceEntity::getId, Function.identity()));
            return nutritionService.calculate(lines.stream().map(line -> {
                RecipeUnitReferenceEntity unit = units.get(line.getUnitId());
                RecipeIngredientReferenceEntity ingredient = line.getIngredientId() == null ? null : ingredients.get(line.getIngredientId());
                return new RecipeNutritionService.RecipeIngredient(line.getIngredientId(), line.getUnitId(),
                        unit.getDimension(), unit.getBaseFactor(), line.getQuantity(),
                        ingredient == null ? line.getCustomIngredientName() : ingredient.getName());
            }).toList(), recipe.getServings()).complete();
        }).orElse(false);
    }
}
