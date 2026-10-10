package tech.mamxanh.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.nutrition.dto.request.ConfirmNutritionEligibilityRequest;
import tech.mamxanh.nutrition.dto.request.SaveNutritionProfileRequest;
import tech.mamxanh.nutrition.dto.request.UpdateNutritionEligibilityRequest;
import tech.mamxanh.nutrition.dto.request.NutritionEligibilityConfirmationStatus;
import tech.mamxanh.nutrition.dto.response.NutritionEligibilityResponse;
import tech.mamxanh.nutrition.entity.NutritionEligibilityStatus;
import tech.mamxanh.nutrition.dto.response.NutritionProfileResponse;
import tech.mamxanh.nutrition.service.NutritionProfileService;

@ExtendWith(MockitoExtension.class)
class NutritionControllerTest {
    @Mock private NutritionProfileService service;
    @InjectMocks private NutritionController controller;

    @Test
    void delegatesOwnProfileReadToService() {
        var expected = emptyResponse();
        when(service.getOwnProfile()).thenReturn(expected);

        assertSame(expected, controller.getOwnProfile());
        verify(service).getOwnProfile();
    }

    @Test
    void delegatesProfileSaveToService() {
        var request = new SaveNutritionProfileRequest(LocalDate.of(1990, 1, 1), "FEMALE",
                new BigDecimal("170"), new BigDecimal("65"), "SEDENTARY", "MAINTAIN_WEIGHT",
                false, false, false, true);
        var expected = emptyResponse();
        when(service.saveOwnProfile(request)).thenReturn(expected);

        assertSame(expected, controller.saveOwnProfile(request));
        verify(service).saveOwnProfile(request);
    }

    @Test
    void delegatesCalculationConfirmationToService() {
        var request = new ConfirmNutritionEligibilityRequest(false, false, false);
        var expected = emptyResponse();
        when(service.calculateOwnResults(request)).thenReturn(expected);

        assertSame(expected, controller.calculateOwnResults(request));
        verify(service).calculateOwnResults(request);
    }

    @Test
    void delegatesEligibilityReadAndExplicitUpdate() {
        var expected = new NutritionEligibilityResponse(NutritionEligibilityStatus.NOT_CONFIRMED, null);
        when(service.getOwnEligibility()).thenReturn(expected);
        assertSame(expected, controller.getOwnEligibility());
        verify(service).getOwnEligibility();

        var request = new UpdateNutritionEligibilityRequest(NutritionEligibilityConfirmationStatus.INELIGIBLE);
        when(service.updateOwnEligibility(request)).thenReturn(expected);
        assertSame(expected, controller.updateOwnEligibility(request));
        verify(service).updateOwnEligibility(request);
    }

    private static NutritionProfileResponse emptyResponse() {
        return new NutritionProfileResponse(false, false, List.of(), null, null);
    }
}
