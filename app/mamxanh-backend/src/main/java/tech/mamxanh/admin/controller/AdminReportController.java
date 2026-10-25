package tech.mamxanh.admin.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.admin.dto.AdminReportResponse;
import tech.mamxanh.admin.dto.AdminReportStatusCountsResponse;
import tech.mamxanh.admin.dto.ResolveReportRequest;
import tech.mamxanh.admin.service.AdminReportService;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.response.PageResponse;

/** FR-28 Administrator queue for Recipe Post reports; reports are never exposed outside this role. */
@RestController
@Validated
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminReportController {
    private final AdminReportService service;

    public AdminReportController(AdminReportService service) {
        this.service = service;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of Recipe Post reports, newest first."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: unknown status, or page/size outside its range.")
    })
    @GetMapping
    public PageResponse<AdminReportResponse> listReports(
            @RequestParam(required = false) @Pattern(regexp = "OPEN|IN_REVIEW|RESOLVED") String status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(status, page, size);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Number of Recipe Post reports in each status.")
    })
    @GetMapping("/status-counts")
    public AdminReportStatusCountsResponse countReportsByStatus() {
        return service.countByStatus();
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report detail with reporter, author and handling Administrator."),
            @ApiResponse(responseCode = "404", description = "REPORT_NOT_FOUND: no Recipe Post report has this id.")
    })
    @GetMapping("/{reportId}")
    public AdminReportResponse getReport(@PathVariable @Min(1) long reportId) {
        return service.detail(reportId);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The report, now IN_REVIEW and held by the caller."),
            @ApiResponse(responseCode = "404", description = "REPORT_NOT_FOUND: no Recipe Post report has this id."),
            @ApiResponse(responseCode = "409", description = "REPORT_STATUS_CONFLICT: the report is no longer OPEN.")
    })
    @PostMapping("/{reportId}/claim")
    public AdminReportResponse claimReport(@PathVariable @Min(1) long reportId) {
        return service.claim(reportId);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The RESOLVED report with its decision; the sanction is applied."),
            @ApiResponse(responseCode = "404", description = "REPORT_NOT_FOUND: no Recipe Post report has this id."),
            @ApiResponse(responseCode = "409", description = "REPORT_STATUS_CONFLICT: the report is not IN_REVIEW under the caller. "
                    + "REPORT_DECISION_NOT_APPLICABLE: the post was deleted (hide) or the author is an Administrator (lock).")
    })
    @PostMapping("/{reportId}/resolve")
    public AdminReportResponse resolveReport(@PathVariable @Min(1) long reportId,
            @Valid @RequestBody ResolveReportRequest request) {
        return service.resolve(reportId, request);
    }
}
