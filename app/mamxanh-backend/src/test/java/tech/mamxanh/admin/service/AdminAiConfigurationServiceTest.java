package tech.mamxanh.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import tech.mamxanh.admin.dto.AdminAiConfigurationResponse;
import tech.mamxanh.admin.dto.AdminAiConfigurationUpdateRequest;
import tech.mamxanh.admin.dto.AdminAiConfigurationUpdateRequest.CreditUpdate;
import tech.mamxanh.admin.dto.AdminAiConfigurationUpdateRequest.ModelUpdate;
import tech.mamxanh.admin.dto.AdminAiConfigurationUpdateRequest.PlanUpdate;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.integration.ai.AiProviderException;
import tech.mamxanh.integration.ai.AiProviderException.FailureKind;
import tech.mamxanh.notification.service.NotificationService;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot;
import tech.mamxanh.subscription.repository.AiConfigurationRepository;

@ExtendWith(MockitoExtension.class)
class AdminAiConfigurationServiceTest {
    @Mock private AiConfigurationRepository configurations;
    @Mock private GeminiModelValidator modelValidator;
    @Mock private NotificationService notifications;
    @Mock private org.springframework.context.ApplicationEventPublisher events;
    @Mock private PlatformTransactionManager transactions;

    private AdminAiConfigurationService createService() {
        return new AdminAiConfigurationService(configurations, modelValidator,
                notifications, events, transactions,
                Clock.fixed(Instant.parse("2026-10-31T04:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void rejectsTierWithoutExactlyOneEnabledDefaultBeforePersisting() {
        when(transactions.getTransaction(any(TransactionDefinition.class)))
                .thenAnswer(ignored -> new org.springframework.transaction.support.SimpleTransactionStatus());
        when(configurations.findConfiguration()).thenReturn(currentConfiguration());
        AdminAiConfigurationService service = createService();

        AdminAiConfigurationUpdateRequest invalid = new AdminAiConfigurationUpdateRequest(
                List.of(new PlanUpdate("FREE", 0, 50_000), new PlanUpdate("PLUS", 99_000, 300_000),
                        new PlanUpdate("PRO", 199_000, 600_000)),
                List.of(new ModelUpdate("gemini-3.5-flash-lite", "Flash-Lite", true,
                        true, false, true, false, true, false),
                        new ModelUpdate("gemini-3.8-flash", "Flash", true,
                                false, false, true, false, true, true)),
                new CreditUpdate(BigDecimal.valueOf(1000), 400, 10_000, 500_000), false);

        assertThatThrownBy(() -> service.update(invalid))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("đúng một model mặc định");
        verify(configurations, never()).updatePlan(any());
        verify(configurations, never()).replaceModels(any());
        verify(modelValidator, never()).verifyModel(any());
    }

    @Test
    void modelValidationSuccessAllowsConfigurationUpdate() {
        when(transactions.getTransaction(any(TransactionDefinition.class)))
                .thenAnswer(ignored -> new org.springframework.transaction.support.SimpleTransactionStatus());
        when(configurations.findConfiguration()).thenReturn(currentConfiguration());
        when(modelValidator.verifyModel("gemini-3.9-flash")).thenReturn(true);
        AdminAiConfigurationService service = createService();

        AdminAiConfigurationUpdateRequest valid = new AdminAiConfigurationUpdateRequest(
                List.of(new PlanUpdate("FREE", 0, 50_000), new PlanUpdate("PLUS", 99_000, 300_000),
                        new PlanUpdate("PRO", 199_000, 600_000)),
                List.of(new ModelUpdate("gemini-3.5-flash-lite", "Flash-Lite", true,
                                true, true, true, false, true, false),
                        new ModelUpdate("gemini-3.8-flash", "Flash", true,
                                false, false, true, true, true, false),
                        new ModelUpdate("gemini-3.9-flash", "Flash-Next", true,
                                false, false, false, false, true, true)),
                new CreditUpdate(BigDecimal.valueOf(1000), 400, 10_000, 500_000), false);

        AdminAiConfigurationResponse response = service.update(valid);

        assertThat(response).isNotNull();
        verify(modelValidator).verifyModel("gemini-3.9-flash");
        verify(configurations).replaceModels(org.mockito.ArgumentMatchers.argThat(models -> models.stream()
                .allMatch(model -> model.plusAllowed() == model.enabled() && model.proAllowed() == model.enabled()
                        && model.plusDefault() == model.freeDefault() && model.proDefault() == model.freeDefault())));
    }

    @Test
    void modelNotSupportingContentGenerationThrows422() {
        when(transactions.getTransaction(any(TransactionDefinition.class)))
                .thenAnswer(ignored -> new org.springframework.transaction.support.SimpleTransactionStatus());
        when(configurations.findConfiguration()).thenReturn(currentConfiguration());
        when(modelValidator.verifyModel("gemini-unsupported")).thenReturn(false);
        AdminAiConfigurationService service = createService();

        AdminAiConfigurationUpdateRequest request = new AdminAiConfigurationUpdateRequest(
                List.of(new PlanUpdate("FREE", 0, 50_000), new PlanUpdate("PLUS", 99_000, 300_000),
                        new PlanUpdate("PRO", 199_000, 600_000)),
                List.of(new ModelUpdate("gemini-3.5-flash-lite", "Flash-Lite", true,
                                true, true, true, false, true, false),
                        new ModelUpdate("gemini-3.8-flash", "Flash", true,
                                false, false, true, true, true, false),
                        new ModelUpdate("gemini-unsupported", "Unsupported", true,
                                false, false, false, false, true, true)),
                new CreditUpdate(BigDecimal.valueOf(1000), 400, 10_000, 500_000), false);

        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(api.getCode()).isEqualTo("AI_MODEL_NOT_AVAILABLE");
                });
    }

    @Test
    void providerNotFoundThrows422AiModelNotAvailable() {
        when(transactions.getTransaction(any(TransactionDefinition.class)))
                .thenAnswer(ignored -> new org.springframework.transaction.support.SimpleTransactionStatus());
        when(configurations.findConfiguration()).thenReturn(currentConfiguration());
        when(modelValidator.verifyModel("gemini-missing"))
                .thenThrow(new AiProviderException("Not found", null, FailureKind.HTTP, 404));
        AdminAiConfigurationService service = createService();

        AdminAiConfigurationUpdateRequest request = new AdminAiConfigurationUpdateRequest(
                List.of(new PlanUpdate("FREE", 0, 50_000), new PlanUpdate("PLUS", 99_000, 300_000),
                        new PlanUpdate("PRO", 199_000, 600_000)),
                List.of(new ModelUpdate("gemini-3.5-flash-lite", "Flash-Lite", true,
                                true, true, true, false, true, false),
                        new ModelUpdate("gemini-3.8-flash", "Flash", true,
                                false, false, true, true, true, false),
                        new ModelUpdate("gemini-missing", "Missing", true,
                                false, false, false, false, true, true)),
                new CreditUpdate(BigDecimal.valueOf(1000), 400, 10_000, 500_000), false);

        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(api.getCode()).isEqualTo("AI_MODEL_NOT_AVAILABLE");
                });
    }

    @Test
    void providerNetworkFailureThrows503ValidationUnavailable() {
        when(transactions.getTransaction(any(TransactionDefinition.class)))
                .thenAnswer(ignored -> new org.springframework.transaction.support.SimpleTransactionStatus());
        when(configurations.findConfiguration()).thenReturn(currentConfiguration());
        when(modelValidator.verifyModel("gemini-timeout"))
                .thenThrow(new AiProviderException("Timeout", null, FailureKind.TIMEOUT, null));
        AdminAiConfigurationService service = createService();

        AdminAiConfigurationUpdateRequest request = new AdminAiConfigurationUpdateRequest(
                List.of(new PlanUpdate("FREE", 0, 50_000), new PlanUpdate("PLUS", 99_000, 300_000),
                        new PlanUpdate("PRO", 199_000, 600_000)),
                List.of(new ModelUpdate("gemini-3.5-flash-lite", "Flash-Lite", true,
                                true, true, true, false, true, false),
                        new ModelUpdate("gemini-3.8-flash", "Flash", true,
                                false, false, true, true, true, false),
                        new ModelUpdate("gemini-timeout", "Timeout", true,
                                false, false, false, false, true, true)),
                new CreditUpdate(BigDecimal.valueOf(1000), 400, 10_000, 500_000), false);

        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(api.getCode()).isEqualTo("AI_MODEL_VALIDATION_UNAVAILABLE");
                });
    }

    private static AiConfigurationSnapshot currentConfiguration() {
        return new AiConfigurationSnapshot(List.of(
                new AiConfigurationSnapshot.Plan("FREE", 0, 50_000, null, null),
                new AiConfigurationSnapshot.Plan("PLUS", 99_000, 300_000, null, null),
                new AiConfigurationSnapshot.Plan("PRO", 199_000, 600_000, null, null)),
                List.of(
                        new AiConfigurationSnapshot.Model("gemini-3.5-flash-lite", "Flash-Lite", true,
                                true, true, true, false, true, false),
                        new AiConfigurationSnapshot.Model("gemini-3.8-flash", "Flash", true,
                                false, false, true, true, true, true)),
                new AiConfigurationSnapshot.Credit(BigDecimal.valueOf(1000), 400, 10_000, 500_000));
    }
}
