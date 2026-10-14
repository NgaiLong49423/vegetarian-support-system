package tech.mamxanh.mealplan.controller;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.mealplan.dto.response.MealPlanExportResponse;
import tech.mamxanh.mealplan.service.MealPlanExportService;

@RestController
@RequestMapping("/api/v1/meal-plans")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class MealPlanExportController {
    private final MealPlanExportService service;

    public MealPlanExportController(MealPlanExportService service) {
        this.service = service;
    }

    @Operation(summary = "Read the authenticated member's weekly export snapshot",
            description = "For PDF/TXT generation. No owner parameter. Missing nutrition remains null; unavailable sources disclose no content.")
    @ApiResponse(responseCode = "200", description = "Private export snapshot; Cache-Control: no-store.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MealPlanExportResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid Monday week start.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Active Member required; Administrator cannot export private schedules.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{weekStartDate}/export")
    public ResponseEntity<MealPlanExportResponse> getExport(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.getExport(weekStartDate));
    }
}
