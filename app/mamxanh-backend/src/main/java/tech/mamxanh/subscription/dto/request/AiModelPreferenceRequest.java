package tech.mamxanh.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiModelPreferenceRequest(@NotBlank @Size(max = 128) String modelId) { }
