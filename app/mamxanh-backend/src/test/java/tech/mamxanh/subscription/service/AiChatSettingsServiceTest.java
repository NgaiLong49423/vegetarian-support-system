package tech.mamxanh.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tech.mamxanh.subscription.dto.response.MyAiUsageResponse;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot;
import tech.mamxanh.subscription.repository.AiConfigurationRepository;

class AiChatSettingsServiceTest {
    @Test
    void guestSettingsRequireModelSelectionWhenNoDefaultIsConfigured() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AiConfigurationRepository configuration = mock(AiConfigurationRepository.class);
        AiTokenQuotaService quota = mock(AiTokenQuotaService.class);
        when(quota.getGuestUsage("guest-session")).thenReturn(
                new MyAiUsageResponse("GUEST", 0, 1000, 1000, Instant.EPOCH, Instant.EPOCH.plusSeconds(3600)));
        when(configuration.findConfiguration()).thenReturn(new AiConfigurationSnapshot(List.of(), List.of(
                new AiConfigurationSnapshot.Model("gemini-flash", "Gemini Flash", true,
                        true, false, true, false, true, false)),
                new AiConfigurationSnapshot.Credit(BigDecimal.ONE, 1, 1000, 10000)));

        var response = new AiChatSettingsService(jdbc, configuration, quota).get(null, "guest-session");

        assertThat(response.tier()).isEqualTo("GUEST");
        assertThat(response.selectionRequired()).isTrue();
        assertThat(response.selectedModelId()).isNull();
        assertThat(response.models()).singleElement().satisfies(model -> {
            assertThat(model.modelId()).isEqualTo("gemini-flash");
            assertThat(model.isDefault()).isFalse();
        });
    }
}
