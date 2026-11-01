package tech.mamxanh.expertapplication.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.expertapplication.dto.ApproveRequest;
import tech.mamxanh.expertapplication.dto.ExpertApplicationRequest;
import tech.mamxanh.expertapplication.dto.ExpertApplicationResponse;
import tech.mamxanh.expertapplication.dto.ReviewRequest;
import tech.mamxanh.expertapplication.service.ExpertApplicationService;

import java.nio.charset.StandardCharsets;

@RestController
@Validated
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ExpertApplicationController {
    private final ExpertApplicationService service;
    public ExpertApplicationController(ExpertApplicationService service) { this.service = service; }

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "The submitted application, PENDING."),
            @ApiResponse(responseCode = "409", description = "EXPERT_APPLICATION_PENDING_EXISTS: the customer already has a pending application.")
    })
    @PostMapping("/api/v1/expert-applications")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ExpertApplicationResponse submit(@Valid @RequestBody ExpertApplicationRequest request) { return service.submit(request); }

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "The submitted application with optional private attachments."),
            @ApiResponse(responseCode = "400", description = "Invalid application data or attachment format."),
            @ApiResponse(responseCode = "409", description = "EXPERT_APPLICATION_PENDING_EXISTS: the customer already has a pending application."),
            @ApiResponse(responseCode = "413", description = "An attachment exceeds its size limit.")
    })
    @PostMapping(path = "/api/v1/expert-applications/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ExpertApplicationResponse submitWithAttachments(
            @RequestPart("application") @Valid ExpertApplicationRequest request,
            @RequestPart(value = "certificate", required = false) MultipartFile certificate,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        return service.submitWithAttachments(request, certificate, avatar);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The caller's applications, newest first.")
    })
    @GetMapping("/api/v1/expert-applications/me")
    @PreAuthorize("hasAnyRole('CUSTOMER','EXPERT')")
    public PageResponse<ExpertApplicationResponse> history(@RequestParam(defaultValue="0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.ownHistory(page, size); }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of applications."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: unknown status, or page/size outside its range.")
    })
    @GetMapping("/api/v1/admin/expert-applications")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ExpertApplicationResponse> list(@RequestParam(required=false) String status,
            @RequestParam(defaultValue="0") @Min(0) @Max(1000000) int page, @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        if (status != null && !status.isBlank() && !status.matches("PENDING|APPROVED|REJECTED|CANCELLED")) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST);
        return service.adminList(status, page, size);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application detail."),
            @ApiResponse(responseCode = "404", description = "NOT_FOUND: no application has this id.")
    })
    @GetMapping("/api/v1/admin/expert-applications/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ExpertApplicationResponse detail(@PathVariable long id) { return service.adminDetail(id); }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Private application attachment content."),
            @ApiResponse(responseCode = "403", description = "Administrator role required."),
            @ApiResponse(responseCode = "404", description = "Application or attachment not found.")
    })
    @GetMapping("/api/v1/admin/expert-applications/{id}/attachments/{kind}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> attachment(@PathVariable long id, @PathVariable String kind) {
        var attachment = service.downloadAttachment(id, kind);
        var disposition = ContentDisposition.inline().filename(attachment.filename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(attachment.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(attachment.content());
    }
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The approved application; the account becomes EXPERT."),
            @ApiResponse(responseCode = "409", description = "EXPERT_APPLICATION_STALE: the application is no longer pending, or the account is not an active customer.")
    })
    @PostMapping("/api/v1/admin/expert-applications/{id}/approve") @PreAuthorize("hasRole('ADMIN')")
    public ExpertApplicationResponse approve(@PathVariable long id, @Valid @RequestBody(required = false) ApproveRequest request) {
        return service.approve(id, request == null ? null : request.note());
    }
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The rejected application."),
            @ApiResponse(responseCode = "409", description = "EXPERT_APPLICATION_STALE: the application is no longer pending.")
    })
    @PostMapping("/api/v1/admin/expert-applications/{id}/reject") @PreAuthorize("hasRole('ADMIN')")
    public ExpertApplicationResponse reject(@PathVariable long id, @Valid @RequestBody ReviewRequest request) { return service.reject(id, request.reason()); }
}
