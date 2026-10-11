package tech.mamxanh.expertapplication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExpertApplicationRequest(
        @NotBlank @Size(min = 20, max = 2000) String experience,
        @NotBlank @Pattern(regexp = "VEGAN|LACTO|OVO|LACTO_OVO|MACROBIOTIC") String vegetarianType,
        @NotBlank @Size(min = 30, max = 2000) String sampleRecipeSummary,
        @Size(max = 500) @Pattern(regexp = "^$|https?://[^\\s]+", message = "Portfolio URL must use HTTP or HTTPS") String portfolioUrl) {
    public ExpertApplicationRequest {
        if (experience != null) experience = experience.trim();
        if (vegetarianType != null) vegetarianType = vegetarianType.trim();
        if (sampleRecipeSummary != null) sampleRecipeSummary = sampleRecipeSummary.trim();
        if (portfolioUrl != null) portfolioUrl = portfolioUrl.trim();
    }
}
