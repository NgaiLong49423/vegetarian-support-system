package tech.mamxanh.admin.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.admin.dto.RecipeReportRequest;
import tech.mamxanh.admin.dto.RecipeReportResponse;
import tech.mamxanh.admin.service.RecipeReportService;
import tech.mamxanh.common.config.OpenApiConfig;

@RestController
@RequestMapping("/api/v1/recipes/{recipeId}/reports")
@Validated
public class RecipeReportController {
    private final RecipeReportService service;

    public RecipeReportController(RecipeReportService service) {
        this.service = service;
    }

    @PostMapping
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<RecipeReportResponse> submit(@PathVariable @Min(1) long recipeId,
            @Valid @RequestBody RecipeReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(recipeId, request));
    }
}
