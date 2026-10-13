package tech.mamxanh.recipe.repository;

import java.time.LocalDateTime;
import java.util.List;
import tech.mamxanh.recipe.service.RecipeSortMode;

public interface RecipeBrowseRepository {
    BrowsePage findPublished(String keyword, String vegetarianType, String dishCategory,
            List<Long> ingredientIds, Integer maxTotalTimeMinutes, RecipeSortMode sortMode,
            LocalDateTime viewSince, LocalDateTime now, int page, int size);

    /** FR-23 (Q56): one author's published posts, newest first, with the same public metrics. */
    BrowsePage findPublishedByAuthor(long authorId, LocalDateTime now, int page, int size);

    record BrowsePage(List<BrowseRow> rows, long totalElements) { }

    record BrowseRow(long recipeId, long likes, long dislikes, long views, long comments,
            long periodViews) { }
}
