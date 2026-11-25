package tech.mamxanh.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.admin.dto.AdminExpertDetailResponse;
import tech.mamxanh.admin.service.AdminExpertService;
import tech.mamxanh.common.config.OpenApiConfig;

/** Read-only current Expert profile lookup for Administrators. */
@RestController
@Validated
@RequestMapping("/api/v1/admin/experts")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminExpertController {
    private final AdminExpertService service;

    public AdminExpertController(AdminExpertService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get current Expert account, public profile, and latest approved application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current Expert detail; application fields are null when no approved application exists."),
            @ApiResponse(responseCode = "404", description = "EXPERT_NOT_FOUND: account is missing or no longer has the EXPERT role.")
    })
    public AdminExpertDetailResponse detail(@PathVariable @Min(1) long userId) {
        return service.detail(userId);
    }
}
