package tech.mamxanh.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminAiConfigurationUpdateRequestValidationTest {
    @Test
    void acceptsDottedGeminiVersionInExistingModelId() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var request = new AdminAiConfigurationUpdateRequest(
                List.of(new AdminAiConfigurationUpdateRequest.PlanUpdate("FREE", 0, 50_000),
                        new AdminAiConfigurationUpdateRequest.PlanUpdate("PLUS", 99_000, 300_000),
                        new AdminAiConfigurationUpdateRequest.PlanUpdate("PRO", 199_000, 600_000)),
                List.of(new AdminAiConfigurationUpdateRequest.ModelUpdate("gemini-3.5-flash-lite", "Flash-Lite",
                                true, true, true, true, false, true, false),
                        new AdminAiConfigurationUpdateRequest.ModelUpdate("gemini-3.8-flash", "Flash",
                                true, false, false, true, true, true, true)),
                new AdminAiConfigurationUpdateRequest.CreditUpdate(BigDecimal.valueOf(1000), 400, 10_000, 500_000),
                false);

        assertThat(validator.validate(request)).isEmpty();
    }
}
