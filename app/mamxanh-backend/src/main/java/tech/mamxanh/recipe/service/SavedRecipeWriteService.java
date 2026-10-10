package tech.mamxanh.recipe.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.repository.RecipePostRepository;
import tech.mamxanh.recipe.repository.SavedRecipeRepository;

@Service
public class SavedRecipeWriteService {
    private final CurrentUserService currentUserService;
    private final RecipePostRepository recipeRepository;
    private final SavedRecipeRepository savedRecipeRepository;

    public SavedRecipeWriteService(CurrentUserService currentUserService, RecipePostRepository recipeRepository,
            SavedRecipeRepository savedRecipeRepository) {
        this.currentUserService = currentUserService;
        this.recipeRepository = recipeRepository;
        this.savedRecipeRepository = savedRecipeRepository;
    }

    @Transactional
    public void save(long recipeId) {
        long userId = currentUserService.requireActiveMember().id();
        validateRecipeId(recipeId);
        var recipe = recipeRepository.lockById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        if (!"PUBLISHED".equals(recipe.getStatus())) {
            throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        }
        savedRecipeRepository.insertIfAbsent(userId, recipeId);
    }

    @Transactional
    public void unsave(long recipeId) {
        long userId = currentUserService.requireActiveMember().id();
        validateRecipeId(recipeId);
        savedRecipeRepository.deleteSavedRecipe(userId, recipeId);
    }

    private void validateRecipeId(long recipeId) {
        if (recipeId < 1) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "recipeId phải lớn hơn 0.");
        }
    }
}
