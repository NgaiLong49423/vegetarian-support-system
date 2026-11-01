package tech.mamxanh.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.admin.dto.AiTelemetryReport;
import tech.mamxanh.admin.service.AiTelemetryReportService;
import tech.mamxanh.common.config.OpenApiConfig;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/ai-telemetry")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AiTelemetryController {
    private final AiTelemetryReportService service;
    public AiTelemetryController(AiTelemetryReportService service) { this.service = service; }
    @GetMapping
    @Operation(summary = "Aggregate retained AI token usage and estimated Standard paid-equivalent USD cost",
            description = "Administrator only. Dates are inclusive in Asia/Ho_Chi_Minh; weeks start Monday. "
                    + "At most the latest 90 days are available; retainedFrom exposes partial first-day coverage. "
                    + "Granularity DAY/WEEK/MONTH. No raw prompts, responses or actor identifiers are returned. "
                    + "estimatedCostUsd is null when any call is unpriced; knownEstimatedCostUsd is the priced subtotal, not an invoice.")
    public AiTelemetryReport report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String granularity,
            @RequestParam(required = false) String feature) {
        return service.report(from, to, granularity, feature);
    }
}
