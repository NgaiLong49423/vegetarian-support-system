package tech.mamxanh.admin.dto;

/** Number of Recipe Post reports in each status of the FR-28 lifecycle. */
public record AdminReportStatusCountsResponse(long open, long inReview, long resolved) { }
