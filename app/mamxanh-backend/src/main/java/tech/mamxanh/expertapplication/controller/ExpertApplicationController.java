package tech.mamxanh.expertapplication.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.expertapplication.dto.ExpertApplicationRequest;
import tech.mamxanh.expertapplication.dto.ExpertApplicationResponse;
import tech.mamxanh.expertapplication.dto.ApproveRequest;
import tech.mamxanh.expertapplication.dto.ReviewRequest;
import tech.mamxanh.expertapplication.service.ExpertApplicationService;

@RestController
@Validated
public class ExpertApplicationController {
    private final ExpertApplicationService service;
    public ExpertApplicationController(ExpertApplicationService service) { this.service = service; }

    @PostMapping("/api/v1/expert-applications")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ExpertApplicationResponse submit(@Valid @RequestBody ExpertApplicationRequest request) { return service.submit(request); }

    @GetMapping("/api/v1/expert-applications/me")
    @PreAuthorize("hasAnyRole('CUSTOMER','EXPERT')")
    public PageResponse<ExpertApplicationResponse> history(@RequestParam(defaultValue="0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.ownHistory(page, size); }

    @GetMapping("/api/v1/admin/expert-applications")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ExpertApplicationResponse> list(@RequestParam(required=false) String status,
            @RequestParam(defaultValue="0") @Min(0) @Max(1000000) int page, @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        if (status != null && !status.isBlank() && !status.matches("PENDING|APPROVED|REJECTED")) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST);
        return service.adminList(status, page, size);
    }

    @GetMapping("/api/v1/admin/expert-applications/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ExpertApplicationResponse detail(@PathVariable long id) { return service.adminDetail(id); }
    @PostMapping("/api/v1/admin/expert-applications/{id}/approve") @PreAuthorize("hasRole('ADMIN')")
    public ExpertApplicationResponse approve(@PathVariable long id, @Valid @RequestBody(required = false) ApproveRequest request) {
        return service.approve(id, request == null ? null : request.note());
    }
    @PostMapping("/api/v1/admin/expert-applications/{id}/reject") @PreAuthorize("hasRole('ADMIN')")
    public ExpertApplicationResponse reject(@PathVariable long id, @Valid @RequestBody ReviewRequest request) { return service.reject(id, request.reason()); }
}
