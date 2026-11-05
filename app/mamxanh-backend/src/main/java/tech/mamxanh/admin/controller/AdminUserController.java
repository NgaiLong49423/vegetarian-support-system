package tech.mamxanh.admin.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.mamxanh.admin.dto.*;
import tech.mamxanh.admin.service.AdminUserService;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.response.PageResponse;

/** FR-06 account lookup and management for Administrators. */
@RestController
@Validated
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminUserController {
    private final AdminUserService service;
    public AdminUserController(AdminUserService service) { this.service = service; }

    @GetMapping
    @ApiResponses({@ApiResponse(responseCode = "200", description = "A bounded page of minimal account details."),
            @ApiResponse(responseCode = "400", description = "Invalid filters or pagination values.")})
    public PageResponse<AdminUserResponse> list(@RequestParam(required = false) @Size(max = 200) String query,
            @RequestParam(required = false) @Pattern(regexp = "CUSTOMER|EXPERT|ADMIN") String role,
            @RequestParam(required = false) @Pattern(regexp = "ACTIVE|LOCKED") String status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(query, role, status, page, size);
    }

    @GetMapping("/summary")
    @ApiResponse(responseCode = "200", description = "Global account counts, independent of list filters.")
    public AdminUserSummaryResponse summary() { return service.summary(); }

    @GetMapping("/{userId}")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Minimal account detail."),
            @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")})
    public AdminUserResponse detail(@PathVariable @Min(1) long userId) { return service.detail(userId); }

    @GetMapping("/{userId}/history")
    @ApiResponse(responseCode = "200", description = "Account role/status changes and historical report-based account locks.")
    public PageResponse<AdminUserAuditResponse> history(@PathVariable @Min(1) long userId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.history(userId, page, size);
    }

    @PatchMapping("/{userId}/status")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Updated account status."),
            @ApiResponse(responseCode = "400", description = "Reason must contain 10 to 500 characters."),
            @ApiResponse(responseCode = "409", description = "Administrator accounts are read-only here.")})
    public AdminUserResponse changeStatus(@PathVariable @Min(1) long userId,
            @Valid @RequestBody ChangeAdminUserStatusRequest request) {
        return service.changeStatus(userId, request);
    }

    @PatchMapping("/{userId}/role")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Updated account role."),
            @ApiResponse(responseCode = "400", description = "Reason must contain 10 to 500 characters."),
            @ApiResponse(responseCode = "409", description = "Only CUSTOMER/EXPERT targets are supported; ADMIN is read-only.")})
    public AdminUserResponse changeRole(@PathVariable @Min(1) long userId,
            @Valid @RequestBody ChangeAdminUserRoleRequest request) {
        return service.changeRole(userId, request);
    }
}
