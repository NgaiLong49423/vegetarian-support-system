package tech.mamxanh.subscription.dto.response;

import tech.mamxanh.common.response.PageResponse;

public record AdminTransactionReport(PageResponse<AdminTransactionResponse> transactions, Summary summary) {
    /** Every value uses the same filters as the list, across all matching pages. Revenue includes PAID only. */
    @io.swagger.v3.oas.annotations.media.Schema(name = "AdminTransactionSummary")
    public record Summary(long total, long paid, long pending, long failed, long cancelled, long revenueVnd) { }
}
