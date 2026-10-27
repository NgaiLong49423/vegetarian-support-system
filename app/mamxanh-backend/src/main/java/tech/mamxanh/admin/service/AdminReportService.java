package tech.mamxanh.admin.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.admin.dto.AdminReportGroupResponse;
import tech.mamxanh.admin.dto.AdminReportResponse;
import tech.mamxanh.admin.dto.AdminReportStatusCountsResponse;
import tech.mamxanh.admin.dto.ResolveReportRequest;
import tech.mamxanh.admin.entity.RecipeReportEntity;
import tech.mamxanh.admin.entity.ReportDecision;
import tech.mamxanh.admin.repository.AdminReportQueryRepository;
import tech.mamxanh.admin.repository.RecipeReportRepository;
import tech.mamxanh.auth.service.AccountModerationService;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.notification.service.ModerationNotificationService;
import tech.mamxanh.recipe.service.RecipeModerationService;

/**
 * FR-28: Administrators take Recipe Post reports through OPEN -> IN_REVIEW -> RESOLVED. Only the
 * Administrator who took a report records its decision, and a RESOLVED report never changes again,
 * so the report row itself is the audit record (SRS 3.12, BR-26, NFR-08). Report counts never
 * trigger a sanction; every change comes from one of these calls.
 */
@Service
public class AdminReportService {
    private final RecipeReportRepository reports;
    private final AdminReportQueryRepository queries;
    private final CurrentUserService currentUserService;
    private final RecipeModerationService recipeModeration;
    private final AccountModerationService accountModeration;
    private final ModerationNotificationService notifications;
    private final Clock clock;

    public AdminReportService(RecipeReportRepository reports, AdminReportQueryRepository queries,
            CurrentUserService currentUserService, RecipeModerationService recipeModeration,
            AccountModerationService accountModeration, ModerationNotificationService notifications, Clock clock) {
        this.reports = reports;
        this.queries = queries;
        this.currentUserService = currentUserService;
        this.recipeModeration = recipeModeration;
        this.accountModeration = accountModeration;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminReportResponse> list(String status, Long recipeId, int page, int size) {
        return PageResponse.of(queries.page(status, recipeId, page, size), page, size, queries.count(status, recipeId));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminReportGroupResponse> listGroups(String status, int page, int size) {
        return PageResponse.of(queries.groups(status, page, size), page, size, queries.countGroups(status));
    }

    @Transactional(readOnly = true)
    public AdminReportStatusCountsResponse countByStatus() {
        return queries.countByStatus();
    }

    @Transactional(readOnly = true)
    public AdminReportResponse detail(long reportId) {
        return queries.findById(reportId).orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
    }

    /**
     * AC-28.1: the first Administrator to take an OPEN report holds it; a later claim gets 409. An
     * IN_REVIEW report without a handler (mapped from PROCESSING by V9) can still be taken.
     */
    @Transactional
    public AdminReportResponse claim(long reportId) {
        long administratorId = currentUserService.requireAuthenticatedUserId();
        RecipeReportEntity report = lock(reportId);
        boolean unheldReview = "IN_REVIEW".equals(report.getStatus()) && report.getHandledBy() == null;
        if (!"OPEN".equals(report.getStatus()) && !unheldReview) {
            throw new AppException(ErrorCode.REPORT_STATUS_CONFLICT,
                    "Báo cáo đã được tiếp nhận hoặc đã giải quyết. Hãy tải lại danh sách.");
        }
        report.claim(administratorId, now());
        reports.flush();
        return detail(reportId);
    }

    /**
     * AC-28.2–AC-28.5: records the decision with its reason, applies the chosen sanction and
     * notifies the reporter and, for a sanction, the author, without revealing the reporter.
     */
    @Transactional
    public AdminReportResponse resolve(long reportId, ResolveReportRequest request) {
        long administratorId = currentUserService.requireAuthenticatedUserId();
        RecipeReportEntity report = lock(reportId);
        requireHeldBy(report, administratorId);
        ReportDecision decision = ReportDecision.valueOf(request.decision());
        AdminReportResponse current = detail(reportId);
        long authorId = current.author().id();
        String title = current.recipe().title();

        switch (decision) {
            case HIDE_RECIPE -> {
                if (!recipeModeration.hide(current.recipe().id())) {
                    throw new AppException(ErrorCode.REPORT_DECISION_NOT_APPLICABLE,
                            "Tác giả đã xóa bài công thức này nên không thể ẩn bài.");
                }
            }
            case LOCK_AUTHOR -> {
                if ("ADMIN".equals(current.author().role())) {
                    throw new AppException(ErrorCode.REPORT_DECISION_NOT_APPLICABLE,
                            "Không thể khóa tài khoản Quản trị viên từ một báo cáo.");
                }
                accountModeration.lockAccount(authorId);
            }
            case NO_VIOLATION, WARN_AUTHOR -> { }
        }
        report.resolve(decision, request.reason(), now());

        notifications.reportResult(report.getReporterId(), reportId, current.recipe().id(), title,
                decision != ReportDecision.NO_VIOLATION);
        switch (decision) {
            case WARN_AUTHOR -> notifications.authorWarned(authorId, reportId, title, request.reason());
            case HIDE_RECIPE -> notifications.recipeHidden(authorId, reportId, title, request.reason());
            case LOCK_AUTHOR -> notifications.accountLocked(authorId, reportId, title, request.reason());
            case NO_VIOLATION -> { }
        }
        reports.flush();
        return detail(reportId);
    }

    private RecipeReportEntity lock(long reportId) {
        return reports.lockRecipeReportById(reportId).orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
    }

    private static void requireHeldBy(RecipeReportEntity report, long administratorId) {
        switch (report.getStatus()) {
            case "OPEN" -> throw new AppException(ErrorCode.REPORT_STATUS_CONFLICT,
                    "Hãy tiếp nhận báo cáo trước khi ghi kết luận.");
            case "RESOLVED" -> throw new AppException(ErrorCode.REPORT_STATUS_CONFLICT,
                    "Báo cáo đã được giải quyết nên không thể đổi kết luận.");
            default -> {
                if (report.getHandledBy() == null || report.getHandledBy() != administratorId) {
                    throw new AppException(ErrorCode.REPORT_STATUS_CONFLICT,
                            "Báo cáo đang được quản trị viên khác xem xét.");
                }
            }
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
