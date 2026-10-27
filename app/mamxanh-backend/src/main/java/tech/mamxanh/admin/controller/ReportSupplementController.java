package tech.mamxanh.admin.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.admin.dto.MyReportResponse;
import tech.mamxanh.admin.dto.ReportSupplementRequest;
import tech.mamxanh.admin.dto.ReportSupplementResponse;
import tech.mamxanh.admin.service.ReportSupplementService;
import tech.mamxanh.common.config.OpenApiConfig;

/** The reporter's own view of a Recipe Post report and its supplements (FR-30). */
@RestController
@Validated
@RequestMapping("/api/v1/me/reports")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ReportSupplementController {
    private final ReportSupplementService service;

    public ReportSupplementController(ReportSupplementService service) {
        this.service = service;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The caller's report with its supplements, oldest first."),
            @ApiResponse(responseCode = "403", description = "ACCESS_DENIED: the report belongs to another account."),
            @ApiResponse(responseCode = "404", description = "REPORT_NOT_FOUND: no Recipe Post report has this id.")
    })
    @GetMapping("/{reportId}")
    public MyReportResponse getMyReport(@PathVariable @Min(1) long reportId) {
        return service.mine(reportId);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "The stored supplement with its time."),
            @ApiResponse(responseCode = "403", description = "ACCESS_DENIED: the report belongs to another account."),
            @ApiResponse(responseCode = "404", description = "REPORT_NOT_FOUND: no Recipe Post report has this id."),
            @ApiResponse(responseCode = "409", description = "REPORT_NOT_OPEN: the report is RESOLVED. "
                    + "REPORT_SUPPLEMENT_LIMIT: the report already has 5 supplements.")
    })
    @PostMapping("/{reportId}/supplements")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportSupplementResponse addReportSupplement(@PathVariable @Min(1) long reportId,
            @Valid @RequestBody ReportSupplementRequest request) {
        return service.add(reportId, request);
    }
}
