package tech.mamxanh.recipe.repository;

import java.time.LocalDateTime;
import java.util.List;
import tech.mamxanh.recipe.service.RecipeSortMode;

public interface RecipeBrowseRepository {
    BrowsePage findPublished(String keyword, String vegetarianType, String dishCategory,
            List<Long> ingredientIds, Integer maxTotalTimeMinutes, RecipeSortMode sortMode,
            LocalDateTime viewSince, LocalDateTime now, int page, int size);

    record BrowsePage(List<BrowseRow> rows, long totalElements) { }

    record BrowseRow(long recipeId, long likes, long dislikes, long views, long comments,
            long periodViews) { }
}
