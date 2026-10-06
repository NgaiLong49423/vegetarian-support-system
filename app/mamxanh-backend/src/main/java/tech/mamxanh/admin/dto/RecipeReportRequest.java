package tech.mamxanh.admin.dto;

import jakarta.validation.constraints.Size;

public record RecipeReportRequest(
        @Size(max = 30) String reasonCode,
        @Size(max = 500) String description) { }
