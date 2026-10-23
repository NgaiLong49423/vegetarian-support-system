package tech.mamxanh.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tech.mamxanh.auth.dto.response.ExpertDirectoryEntryResponse;
import tech.mamxanh.auth.service.ExpertDirectoryService;
import tech.mamxanh.common.response.PageResponse;

/** Public expert discovery (FR-61); follow mutations remain on the FR-59 member endpoints. */
@RestController
@Validated
@RequestMapping("/api/v1/experts")
public class ExpertDirectoryController {
    private final ExpertDirectoryService service;

    public ExpertDirectoryController(ExpertDirectoryService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Search the public directory of active experts")
    public PageResponse<ExpertDirectoryEntryResponse> search(
            @RequestParam(defaultValue = "") @Size(max = 100) String query,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return service.search(query, page, size);
    }
}
