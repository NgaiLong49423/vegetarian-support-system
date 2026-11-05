package tech.mamxanh.recipe.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.recipe.entity.RecipePostEntity;
import tech.mamxanh.recipe.repository.RecipePostRepository;

import java.time.Clock;
import java.time.LocalDateTime;

/** Recipe Post status changes made by Administrator decisions (BR-26, SRS 3.12); not an approval step. */
@Service
public class RecipeModerationService {
    private final RecipePostRepository repository;
    private final Clock clock;

    public RecipeModerationService(RecipePostRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Hides a post from public reads, search and suggestions, which only return PUBLISHED posts
     * (AC-28.3). An already hidden post stays hidden. Returns {@code false} for a missing or deleted post.
     */
    @Transactional
    public boolean hide(long recipeId) {
        RecipePostEntity recipe = repository.lockById(recipeId).orElse(null);
        if (recipe == null || "DELETED".equals(recipe.getStatus())) {
            return false;
        }
        if (!"HIDDEN".equals(recipe.getStatus())) {
            recipe.setStatus("HIDDEN");
            recipe.setUpdatedAt(LocalDateTime.now(clock));
        }
        return true;
    }
}
