package tech.mamxanh.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A Recipe Post report as an Administrator sees it (FR-28, AC-29.3): reporter and author accounts,
 * the handling Administrator, the recorded decision and the reporter's supplements, oldest first
 * (FR-30). Never returned to authors or the public.
 */
public record AdminReportResponse(long id, String status, String reasonCode, String description,
        LocalDateTime createdAt, LocalDateTime updatedAt, ReportedRecipe recipe, ReportAccount reporter,
        ReportAccount author, ReportHandler handledBy, String decision, String decisionReason, LocalDateTime handledAt,
        List<AdminReportSupplement> supplements) {

    public AdminReportResponse withSupplements(List<AdminReportSupplement> values) {
        return new AdminReportResponse(id, status, reasonCode, description, createdAt, updatedAt, recipe, reporter,
                author, handledBy, decision, decisionReason, handledAt, List.copyOf(values));
    }

    public record ReportedRecipe(long id, String title, String status) { }

    public record ReportAccount(long id, String displayName, String email, String role, String accountStatus) { }

    public record ReportHandler(long id, String displayName) { }

    public record AdminReportSupplement(long supplementId, String content, LocalDateTime createdAt) { }
}
