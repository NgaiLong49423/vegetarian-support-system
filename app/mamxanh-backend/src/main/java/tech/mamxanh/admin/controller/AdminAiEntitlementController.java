package tech.mamxanh.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.mamxanh.admin.dto.AdminAiCreditGrantResponse;
import tech.mamxanh.admin.dto.AdminAiPackageResponse;
import tech.mamxanh.admin.dto.GiftAiPackageRequest;
import tech.mamxanh.admin.dto.GrantAiCreditRequest;
import tech.mamxanh.admin.service.AdminAiEntitlementService;
import tech.mamxanh.common.config.OpenApiConfig;

@RestController
@Validated
@RequestMapping("/api/v1/admin/ai-accounts")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminAiEntitlementController {
    private final AdminAiEntitlementService service;

    public AdminAiEntitlementController(AdminAiEntitlementService service) { this.service = service; }

    @GetMapping("/{userId}/tier")
    @Operation(summary = "Read an account's current AI tier", description = "Administrator only.")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    public ResponseEntity<AdminAiPackageResponse> getTier(@PathVariable @Min(1) long userId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.currentTier(userId));
    }

    @PostMapping("/{userId}/package-gifts")
    @Operation(summary = "Gift an AI package to a member", description = "Applies immediately. Paid tiers start a new 30-day period. Does not create a payment order or refund the previous period. Requires a reason, records the gift in account history, and sends an in-app notice. CUSTOMER and EXPERT accounts can receive gifts; ADMIN accounts cannot.")
    @ApiResponse(responseCode = "200", description = "Package gift applied or requested tier already active.")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or AI_PACKAGE_GIFT_MEMBER_ONLY")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    public ResponseEntity<AdminAiPackageResponse> giftPackage(@PathVariable @Min(1) long userId,
            @Valid @RequestBody GiftAiPackageRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.giftPackage(userId, request));
    }

    @PostMapping("/{userId}/credit-grants")
    @Operation(summary = "Grant AI Credit to an account", description = "Requires positive Credit and a 10–500 character reason. Updates the wallet and ledger atomically and sends an in-app notice.")
    @ApiResponse(responseCode = "200", description = "Credit grant recorded.")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    public ResponseEntity<AdminAiCreditGrantResponse> grantCredit(@PathVariable @Min(1) long userId,
            @Valid @RequestBody GrantAiCreditRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.grantCredit(userId, request));
    }
}
