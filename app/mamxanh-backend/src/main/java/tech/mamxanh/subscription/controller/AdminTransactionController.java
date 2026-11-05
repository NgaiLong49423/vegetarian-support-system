package tech.mamxanh.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.subscription.dto.response.AdminTransactionReport;
import tech.mamxanh.subscription.dto.response.AdminTransactionResponse;
import tech.mamxanh.subscription.service.TransactionAuditService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/transactions")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: invalid filters, paging, date or export exceeds 10,000 rows.",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED: Administrator session required.",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED or ACCOUNT_LOCKED: current account is not an active Administrator.",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
public class AdminTransactionController {
    private final TransactionAuditService service;
    public AdminTransactionController(TransactionAuditService service) { this.service = service; }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "Filtered transactions and cross-page totals.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminTransactionReport.class)))
    @Operation(summary = "Read payment transactions and filtered totals (Administrator only)",
            description = "Read-only local SQL state; does not contact payOS or settle orders. Literal query (max 200) searches order code, TX-ID, name or email. "
                    + "tier PLUS/PRO; status PENDING/PAID/FAILED/CANCELLED. Optional dates filter creation time, inclusive in Asia/Ho_Chi_Minh. "
                    + "UTC timestamps; unknown historical tier is null. Newest first, ID tie-breaker. page >=0, size 1..100. Revenue counts PAID only across all filtered pages.")
    public ResponseEntity<AdminTransactionReport> list(@RequestParam(required = false) String query,
            @RequestParam(required = false) String tier, @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(query, tier, status, from, to, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read a payment and its linked purchased period (not an invoice)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Local payment and linked purchased period.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminTransactionResponse.class))),
        @ApiResponse(responseCode = "404", description = "PAYMENT_TRANSACTION_NOT_FOUND: no such local order.",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AdminTransactionResponse> detail(@PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.detail(id));
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @ApiResponse(responseCode = "200", description = "UTF-8 CSV accounting export with BOM.",
            content = @Content(mediaType = "text/csv", schema = @Schema(type = "string", format = "binary")))
    @Operation(summary = "Export all filtered payment transactions as UTF-8 CSV (up to 10,000)",
            description = "Same filters as list; ignores pagination. More than 10,000 returns 400, never silently truncates. "
                    + "Formula-safe cells, UTC timestamps and BOM for spreadsheets. Local accounting export, not an invoice or provider reconciliation.")
    public ResponseEntity<org.springframework.core.io.ByteArrayResource> export(@RequestParam(required = false) String query,
            @RequestParam(required = false) String tier, @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"MamXanh_giao_dich.csv\"")
                .body(new org.springframework.core.io.ByteArrayResource(
                        service.export(query, tier, status, from, to).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
