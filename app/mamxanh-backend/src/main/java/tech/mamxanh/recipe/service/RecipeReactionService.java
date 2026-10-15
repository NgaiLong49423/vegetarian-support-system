package tech.mamxanh.recipe.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.dto.response.RecipeReactionResponse;
import tech.mamxanh.recipe.entity.RecipePostEntity;
import tech.mamxanh.recipe.entity.RecipeReactionEntity;
import tech.mamxanh.recipe.entity.RecipeReactionId;
import tech.mamxanh.recipe.entity.RecipeReactionType;
import tech.mamxanh.recipe.repository.RecipePostRepository;
import tech.mamxanh.recipe.repository.RecipeReactionRepository;
import tech.mamxanh.recipe.repository.RecipeStatisticsRepository;

@Service
public class RecipeReactionService {
    private final RecipePostRepository recipes;
    private final RecipeReactionRepository reactions;
    private final RecipeStatisticsRepository statistics;
    private final CurrentUserService currentUser;
    private final Clock clock;

    public RecipeReactionService(RecipePostRepository recipes, RecipeReactionRepository reactions,
            RecipeStatisticsRepository statistics, CurrentUserService currentUser, Clock clock) {
        this.recipes = recipes;
        this.reactions = reactions;
        this.statistics = statistics;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RecipeReactionResponse get(long recipeId) {
        var member = currentUser.requireActiveMember();
        recipes.findByIdAndStatus(recipeId, "PUBLISHED")
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        var reaction = reactions.findById(new RecipeReactionId(member.id(), recipeId));
        return response(recipeId, reaction.map(RecipeReactionEntity::getReactionType).orElse(null));
    }

    @Transactional
    public RecipeReactionResponse set(long recipeId, RecipeReactionType type) {
        var member = currentUser.requireActiveMember();
        if (type == null) throw new AppException(ErrorCode.VALIDATION_FAILED);
        requireWritable(recipeId, member.id());
        var id = new RecipeReactionId(member.id(), recipeId);
        var reaction = reactions.findById(id);
        var now = LocalDateTime.now(clock);
        if (reaction.isPresent()) {
            reaction.get().changeTo(type, now);
        } else {
            reactions.save(new RecipeReactionEntity(id, type, now));
        }
        reactions.flush();
        return response(recipeId, type);
    }

    @Transactional
    public RecipeReactionResponse remove(long recipeId) {
        var member = currentUser.requireActiveMember();
        requireWritable(recipeId, member.id());
        reactions.findById(new RecipeReactionId(member.id(), recipeId)).ifPresent(reactions::delete);
        reactions.flush();
        return response(recipeId, null);
    }

    private void requireWritable(long recipeId, long memberId) {
        // Lock the recipe even when this Member has no reaction yet. All mutations take this
        // lock, serializing insert/switch/delete and coordinating with recipe edits/deletion.
        RecipePostEntity recipe = recipes.lockById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        if (!"PUBLISHED".equals(recipe.getStatus())) throw new AppException(ErrorCode.RECIPE_NOT_FOUND);
        if (recipe.getAuthorId().equals(memberId)) throw new AppException(ErrorCode.RECIPE_SELF_REACTION_NOT_ALLOWED);
    }

    private RecipeReactionResponse response(long recipeId, RecipeReactionType currentReaction) {
        var stats = statistics.findStatistics(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        long count = stats.getLikes() + stats.getDislikes();
        BigDecimal percentage = RecipeReactionStatistics.likePercentage(stats.getLikes(), stats.getDislikes());
        return new RecipeReactionResponse(recipeId, currentReaction, stats.getLikes(), stats.getDislikes(), count, percentage);
    }
}
