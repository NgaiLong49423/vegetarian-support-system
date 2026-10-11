package tech.mamxanh.nutrition.dto.request;

import jakarta.validation.constraints.NotNull;

public record CatalogStatusUpdateRequest(@NotNull Boolean active) { }
