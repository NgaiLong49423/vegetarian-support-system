package tech.mamxanh.admin.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.admin.dto.RecipeReportRequest;
import tech.mamxanh.admin.dto.RecipeReportResponse;
import tech.mamxanh.admin.entity.RecipeReportEntity;
import tech.mamxanh.admin.entity.RecipeReportReason;
import tech.mamxanh.admin.repository.RecipeReportRepository;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.service.RecipePostService;

@Service
public class RecipeReportService {
    private static final List<String> ACTIVE_STATUSES = List.of("OPEN", "IN_REVIEW");
    private final RecipeReportRepository reportRepository;
    private final CurrentUserService currentUserService;
    private final RecipePostService recipePostService;
    private final Clock clock;

    public RecipeReportService(RecipeReportRepository reportRepository, CurrentUserService currentUserService,
            RecipePostService recipePostService, Clock clock) {
        this.reportRepository = reportRepository;
        this.currentUserService = currentUserService;
        this.recipePostService = recipePostService;
        this.clock = clock;
    }

    @Transactional
    public RecipeReportResponse submit(long recipeId, RecipeReportRequest request) {
        var member = currentUserService.requireActiveMember();
        recipePostService.requirePublishedForReport(recipeId);
        reportRepository.findFirstByReporterIdAndRecipeIdAndStatusInOrderByIdAsc(
                member.id(), recipeId, ACTIVE_STATUSES).ifPresent(existing -> {
                    throw new AppException(ErrorCode.REPORT_ALREADY_OPEN);
                });

        RecipeReportReason reason;
        try {
            reason = RecipeReportReason.valueOf(request.reasonCode() == null ? "" : request.reasonCode());
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Chọn một trong sáu lý do báo cáo hợp lệ.").withProperty("field", "reasonCode");
        }
        String description = request.description() == null ? null : request.description().trim();
        if (description != null && description.isEmpty()) description = null;
        if (reason == RecipeReportReason.OTHER && (description == null || description.length() < 10)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Mô tả cho lý do Khác phải có từ 10 đến 500 ký tự.").withProperty("field", "description");
        }

        RecipeReportEntity report = reportRepository.saveAndFlush(new RecipeReportEntity(
                member.id(), recipeId, reason, description, LocalDateTime.now(clock)));
        return new RecipeReportResponse(report.getId(), report.getStatus(), "Báo cáo đã được tiếp nhận.");
    }
}
