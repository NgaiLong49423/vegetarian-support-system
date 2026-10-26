package tech.mamxanh.admin.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.admin.dto.MyReportResponse;
import tech.mamxanh.admin.dto.ReportSupplementRequest;
import tech.mamxanh.admin.dto.ReportSupplementResponse;
import tech.mamxanh.admin.entity.RecipeReportEntity;
import tech.mamxanh.admin.entity.ReportSupplementEntity;
import tech.mamxanh.admin.repository.RecipeReportRepository;
import tech.mamxanh.admin.repository.ReportSupplementRepository;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/**
 * FR-30: only the reporter reads their report and adds information while it is OPEN or IN_REVIEW
 * (SRS 3.14, NFR-09). At most {@value #SUPPLEMENT_LIMIT} supplements per report; the report row lock
 * keeps concurrent requests from passing the limit.
 */
@Service
public class ReportSupplementService {
    static final int SUPPLEMENT_LIMIT = 5;
    private static final List<String> OPEN_STATUSES = List.of("OPEN", "IN_REVIEW");

    private final RecipeReportRepository reports;
    private final ReportSupplementRepository supplements;
    private final CurrentUserService currentUserService;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public ReportSupplementService(RecipeReportRepository reports, ReportSupplementRepository supplements,
            CurrentUserService currentUserService, JdbcTemplate jdbc, Clock clock) {
        this.reports = reports;
        this.supplements = supplements;
        this.currentUserService = currentUserService;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MyReportResponse mine(long reportId) {
        RecipeReportEntity report = reports.findById(reportId)
                .filter(found -> found.getRecipeId() != null)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        requireReporter(report);
        String title = jdbc.queryForObject("SELECT title FROM [RECIPE_POST] WHERE recipe_id = ?", String.class,
                report.getRecipeId());
        List<ReportSupplementResponse> added = supplements.findAllByReportIdOrderByCreatedAtAscIdAsc(reportId).stream()
                .map(ReportSupplementService::response).toList();
        return new MyReportResponse(report.getId(), report.getStatus(), report.getReasonCode().name(),
                report.getDescription(), report.getCreatedAt(), new MyReportResponse.MyReportRecipe(report.getRecipeId(), title),
                added, SUPPLEMENT_LIMIT);
    }

    /** AC-30.1: stores the new information with its own time; the original description stays unchanged. */
    @Transactional
    public ReportSupplementResponse add(long reportId, ReportSupplementRequest request) {
        RecipeReportEntity report = reports.lockRecipeReportById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        requireReporter(report);
        if (!OPEN_STATUSES.contains(report.getStatus())) {
            throw new AppException(ErrorCode.REPORT_NOT_OPEN);
        }
        if (supplements.countByReportId(reportId) >= SUPPLEMENT_LIMIT) {
            throw new AppException(ErrorCode.REPORT_SUPPLEMENT_LIMIT);
        }
        ReportSupplementEntity saved = supplements.saveAndFlush(new ReportSupplementEntity(reportId, request.content(),
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)));
        return response(saved);
    }

    private void requireReporter(RecipeReportEntity report) {
        if (report.getReporterId() != currentUserService.requireAuthenticatedUserId()) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private static ReportSupplementResponse response(ReportSupplementEntity supplement) {
        return new ReportSupplementResponse(supplement.getId(), supplement.getContent(), supplement.getCreatedAt());
    }
}
