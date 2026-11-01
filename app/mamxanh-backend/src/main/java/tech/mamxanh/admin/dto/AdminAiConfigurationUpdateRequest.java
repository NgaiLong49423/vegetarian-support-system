package tech.mamxanh.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record AdminAiConfigurationUpdateRequest(
        @NotNull @Size(min = 3, max = 3) List<@Valid PlanUpdate> plans,
        @NotEmpty @Size(max = 50) List<@Valid ModelUpdate> models,
        @NotNull @Valid CreditUpdate credit,
        boolean sendEmailNotification) {

    public record PlanUpdate(
            @NotBlank @Pattern(regexp = "FREE|PLUS|PRO") String tier,
            @Min(0) int priceVnd,
            @Min(0) long tokenQuota) { }

    public record ModelUpdate(
            // Gemini versioned model IDs contain dots, for example gemini-3.5-flash-lite.
            @NotBlank @Pattern(regexp = "gemini-[a-z0-9][a-z0-9.-]{0,119}") String modelId,
            @NotBlank @Size(max = 120) String displayName,
            boolean enabled,
            boolean freeAllowed,
            boolean freeDefault,
            boolean plusAllowed,
            boolean plusDefault,
            boolean proAllowed,
            boolean proDefault) { }

    public record CreditUpdate(
            @NotNull @DecimalMin(value = "0.000001") BigDecimal creditsPer1000Vnd,
            @Min(1) long creditsPer1000OverageTokens,
            @Min(1) int minimumTopUpVnd,
            @Min(1) int maximumTopUpVnd) { }
}
