package tech.mamxanh.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A report as its own reporter sees it (FR-30, AC-30.2): what was sent and every supplement in
 * time order. Carries no reporter or Administrator identity and no internal decision note.
 */
public record MyReportResponse(long reportId, String status, String reasonCode, String description,
        LocalDateTime createdAt, MyReportRecipe recipe, List<ReportSupplementResponse> supplements,
        int supplementLimit) {

    public record MyReportRecipe(long id, String title) { }
}
