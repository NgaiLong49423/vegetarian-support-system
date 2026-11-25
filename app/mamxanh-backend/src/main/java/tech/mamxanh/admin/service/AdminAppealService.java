package tech.mamxanh.admin.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.admin.dto.AdminAppealResponse;
import tech.mamxanh.admin.dto.ResolveAppealRequest;
import tech.mamxanh.admin.entity.AppealDecision;
import tech.mamxanh.admin.entity.RecipeReportEntity;
import tech.mamxanh.admin.entity.ReportDecision;
import tech.mamxanh.admin.repository.AdminAppealQueryRepository;
import tech.mamxanh.admin.repository.RecipeReportRepository;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.notification.service.ModerationNotificationService;
import tech.mamxanh.recipe.repository.RecipePostRepository;
import tech.mamxanh.recipe.service.RecipeModerationService;
import tech.mamxanh.recipe.service.RecipePostService;

/** The Administrator reviews appeals on hidden Recipe Posts and decides them (FR-28, BR-26, BR-27). */
@Service
public class AdminAppealService {
    private static final String NO_LONGER_APPLIES =
            "Khiếu nại không còn hiệu lực: bài không còn bị ẩn hoặc đã có quyết định ẩn mới hơn.";
    private final AdminAppealQueryRepository queries;
    private final RecipeReportRepository reports;
    private final RecipePostRepository recipes;
    private final RecipeModerationService recipeModeration;
    private final RecipePostService recipePosts;
    private final ModerationNotificationService notifications;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public AdminAppealService(AdminAppealQueryRepository queries, RecipeReportRepository reports,
            RecipePostRepository recipes, RecipeModerationService recipeModeration, RecipePostService recipePosts,
            ModerationNotificationService notifications, CurrentUserService currentUserService, Clock clock) {
        this.queries = queries;
        this.reports = reports;
        this.recipes = recipes;
        this.recipeModeration = recipeModeration;
        this.recipePosts = recipePosts;
        this.notifications = notifications;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAppealResponse> list(List<String> statuses, int page, int size) {
        return PageResponse.of(queries.page(statuses, page, size), page, size, queries.count(statuses));
    }

    @Transactional(readOnly = true)
    public AdminAppealResponse detail(long reportId) {
        AdminAppealResponse summary = find(reportId);
        return new AdminAppealResponse(summary.appeal(), summary.author(),
                recipePosts.getForAdministrator(summary.appeal().recipeId()));
    }

    /**
     * Records the decision under a row lock, so two Administrators cannot both decide the same step, then applies
     * it: RESTORE publishes the post again; KEEP_HIDDEN and ALLOW_EDIT leave it hidden. The author is notified.
     */
    @Transactional
    public AdminAppealResponse resolve(long reportId, ResolveAppealRequest request) {
        long administratorId = currentUserService.requireAuthenticatedUserId();
        RecipeReportEntity report = reports.lockRecipeReportById(reportId)
                .filter(found -> found.getAppealStatus() != null)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_NOT_FOUND));
        requireAppealStillApplies(report);
        AppealDecision decision = AppealDecision.valueOf(request.decision());
        report.decideAppeal(decision, request.reason(), administratorId, LocalDateTime.now(clock));
        if (decision == AppealDecision.RESTORE && !recipeModeration.restore(report.getRecipeId())) {
            throw new AppException(ErrorCode.APPEAL_STATE_INVALID, NO_LONGER_APPLIES);
        }
        reports.flush();
        AdminAppealResponse updated = find(reportId);
        notifications.appealDecided(updated.author().id(), reportId, updated.appeal().recipeTitle(), decision,
                request.reason());
        return updated;
    }

    /**
     * BR-27: an appeal can be decided only while the post is still hidden and this report is the post's latest hide
     * decision. Locks the post after the report (the order every moderation path uses), so a concurrent hide,
     * delete or restore is seen.
     */
    private void requireAppealStillApplies(RecipeReportEntity report) {
        boolean hidden = recipes.lockById(report.getRecipeId())
                .map(recipe -> "HIDDEN".equals(recipe.getStatus()))
                .orElse(false);
        boolean latest = reports
                .findFirstByRecipeIdAndDecisionOrderByHandledAtDescIdDesc(report.getRecipeId(), ReportDecision.HIDE_RECIPE)
                .map(found -> found.getId().equals(report.getId()))
                .orElse(false);
        if (!hidden || !latest) {
            throw new AppException(ErrorCode.APPEAL_STATE_INVALID, NO_LONGER_APPLIES);
        }
    }

    private AdminAppealResponse find(long reportId) {
        return queries.find(reportId).orElseThrow(() -> new AppException(ErrorCode.APPEAL_NOT_FOUND));
    }
}
