package tech.mamxanh.admin.dto;

import java.time.LocalDateTime;

/**
 * Reports on one Recipe Post as one group for Administrators (FR-30, AC-30.4). {@code reportCount} and
 * {@code latestReportAt} follow the status filter; {@code statusCounts} always covers every report on the post.
 */
public record AdminReportGroupResponse(AdminReportResponse.ReportedRecipe recipe, AdminReportResponse.ReportAccount author,
        long reportCount, AdminReportStatusCountsResponse statusCounts, LocalDateTime latestReportAt) { }
