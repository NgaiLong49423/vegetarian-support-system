package tech.mamxanh.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.admin.dto.AdminAiConfigurationResponse;
import tech.mamxanh.admin.dto.AdminAiConfigurationUpdateRequest;
import tech.mamxanh.admin.service.AdminAiConfigurationService;
import tech.mamxanh.common.config.OpenApiConfig;

@RestController
@RequestMapping("/api/v1/admin/ai-config")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED: Administrator session required.",
                content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED or ACCOUNT_LOCKED: active Administrator required.",
                content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ProblemDetail.class)))
})
public class AdminAiConfigurationController {
    private final AdminAiConfigurationService service;

    public AdminAiConfigurationController(AdminAiConfigurationService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Read Admin-managed AI plan, Gemini model and Credit configuration")
    @ApiResponse(responseCode = "200", description = "Current configuration, including a pending Free quota change.")
    public ResponseEntity<AdminAiConfigurationResponse> get() {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(service.getConfiguration());
    }

    @PutMapping
    @Operation(summary = "Update AI configuration (Administrator only)",
            description = "New Gemini IDs are verified against the provider before persistence. Free quota applies next month; paid-plan prices and quotas apply to future activation/purchase. Optional email broadcast is asynchronous.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saved configuration."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: invalid plan, model access/default, ratio or top-up range."),
            @ApiResponse(responseCode = "422", description = "AI_MODEL_NOT_AVAILABLE: Gemini does not expose the requested model."),
            @ApiResponse(responseCode = "503", description = "AI_MODEL_VALIDATION_UNAVAILABLE: provider validation failed; configuration was not changed.")
    })
    public ResponseEntity<AdminAiConfigurationResponse> update(
            @Valid @RequestBody AdminAiConfigurationUpdateRequest request) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(service.update(request));
    }
}
