package tech.mamxanh.admin.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.mamxanh.admin.dto.AdminAppealResponse;
import tech.mamxanh.admin.dto.ResolveAppealRequest;
import tech.mamxanh.admin.service.AdminAppealService;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.response.PageResponse;

/** Administrator queue of appeals on hidden Recipe Posts (FR-28, BR-27). */
@RestController
@Validated
@RequestMapping("/api/v1/admin/appeals")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminAppealController {
    private final AdminAppealService service;

    public AdminAppealController(AdminAppealService service) {
        this.service = service;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of appeals, newest activity first; repeat status to "
                    + "keep several states, omit it for every appeal."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: unknown status or page/size outside its range.")
    })
    @GetMapping
    public PageResponse<AdminAppealResponse> listAppeals(
            @Parameter(description = "PENDING, EDIT_ALLOWED, RESUBMITTED, RESTORED or KEPT_HIDDEN; may repeat.")
            @RequestParam(required = false)
            List<@Pattern(regexp = "PENDING|EDIT_ALLOWED|RESUBMITTED|RESTORED|KEPT_HIDDEN") String> status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(status, page, size);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The appeal with its author and the hidden post's content."),
            @ApiResponse(responseCode = "404", description = "APPEAL_NOT_FOUND: no appeal on this report. "
                    + "RECIPE_NOT_FOUND: the author deleted the post.")
    })
    @GetMapping("/{reportId}")
    public AdminAppealResponse getAppeal(@PathVariable @Min(1) long reportId) {
        return service.detail(reportId);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The decided appeal; RESTORE has published the post again."),
            @ApiResponse(responseCode = "404", description = "APPEAL_NOT_FOUND: no appeal on this report."),
            @ApiResponse(responseCode = "409", description = "APPEAL_STATE_INVALID: the appeal is not awaiting this decision "
                    + "(ALLOW_EDIT only on PENDING) or the post is no longer hidden.")
    })
    @PostMapping("/{reportId}/resolve")
    public AdminAppealResponse resolveAppeal(@PathVariable @Min(1) long reportId,
            @Valid @RequestBody ResolveAppealRequest request) {
        return service.resolve(reportId, request);
    }
}
