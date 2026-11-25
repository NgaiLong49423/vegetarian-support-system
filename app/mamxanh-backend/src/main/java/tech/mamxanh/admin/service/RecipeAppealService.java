package tech.mamxanh.admin.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.admin.dto.RecipeAppealRequest;
import tech.mamxanh.admin.dto.RecipeAppealResponse;
import tech.mamxanh.admin.entity.RecipeReportEntity;
import tech.mamxanh.admin.entity.ReportDecision;
import tech.mamxanh.admin.repository.RecipeReportRepository;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.auth.service.CurrentUserService.CurrentUser;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.entity.RecipePostEntity;
import tech.mamxanh.recipe.repository.RecipePostRepository;

/** The author's side of an appeal against the hide decision on their own Recipe Post (FR-28, BR-27). */
@Service
public class RecipeAppealService {
    private final RecipeReportRepository reports;
    private final RecipePostRepository recipes;
    private final CurrentUserService currentUserService;
    private final EntityManager entityManager;
    private final Clock clock;

    public RecipeAppealService(RecipeReportRepository reports, RecipePostRepository recipes,
            CurrentUserService currentUserService, EntityManager entityManager, Clock clock) {
        this.reports = reports;
        this.recipes = recipes;
        this.currentUserService = currentUserService;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RecipeAppealResponse get(long recipeId) {
        RecipePostEntity recipe = requireOwnHiddenRecipe(recipeId);
        return RecipeAppealResponse.of(latestHideReport(recipeId), recipe.getTitle());
    }

    @Transactional
    public RecipeAppealResponse submit(long recipeId, RecipeAppealRequest request) {
        RecipePostEntity recipe = requireOwnHiddenRecipe(recipeId);
        RecipeReportEntity report = lock(latestHideReport(recipeId));
        report.submitAppeal(request.message(), now());
        reports.flush();
        return RecipeAppealResponse.of(report, recipe.getTitle());
    }

    @Transactional
    public RecipeAppealResponse resubmit(long recipeId) {
        RecipePostEntity recipe = requireOwnHiddenRecipe(recipeId);
        RecipeReportEntity report = lock(latestHideReport(recipeId));
        report.resubmitAppeal(now());
        reports.flush();
        return RecipeAppealResponse.of(report, recipe.getTitle());
    }

    /** Only the active Expert who wrote the post may appeal; a post that is not hidden has nothing to appeal. */
    private RecipePostEntity requireOwnHiddenRecipe(long recipeId) {
        CurrentUser author = currentUserService.requireActiveExpert();
        RecipePostEntity recipe = recipes.findById(recipeId)
                .filter(found -> !"DELETED".equals(found.getStatus()))
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        if (!recipe.getAuthorId().equals(author.id())) {
            throw new AppException(ErrorCode.RECIPE_EDIT_NOT_ALLOWED);
        }
        if (!"HIDDEN".equals(recipe.getStatus())) {
            throw new AppException(ErrorCode.APPEAL_NOT_FOUND);
        }
        return recipe;
    }

    private RecipeReportEntity latestHideReport(long recipeId) {
        return reports.findFirstByRecipeIdAndDecisionOrderByHandledAtDescIdDesc(recipeId, ReportDecision.HIDE_RECIPE)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_NOT_FOUND));
    }

    /**
     * Takes the row lock and re-reads the report under it. The report was already loaded without a lock, so a plain
     * locking query would hand back that stale copy; two simultaneous requests would then both pass the state check.
     */
    private RecipeReportEntity lock(RecipeReportEntity report) {
        entityManager.refresh(report, LockModeType.PESSIMISTIC_WRITE);
        return report;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
