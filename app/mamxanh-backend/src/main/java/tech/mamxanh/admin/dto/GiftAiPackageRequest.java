package tech.mamxanh.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GiftAiPackageRequest(
        @NotBlank @Pattern(regexp = "FREE|PLUS|PRO") String tier,
        @NotBlank @Size(min = 10, max = 500) String reason) { }
