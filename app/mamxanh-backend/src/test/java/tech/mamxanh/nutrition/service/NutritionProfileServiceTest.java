package tech.mamxanh.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tech.mamxanh.nutrition.dto.request.SaveNutritionProfileRequest;
import tech.mamxanh.nutrition.entity.NutritionProfileEntity;
import tech.mamxanh.nutrition.repository.NutritionProfileRepository;

@ExtendWith(MockitoExtension.class)
class NutritionProfileServiceTest {
    @Mock private NutritionProfileRepository repository;

    private NutritionProfileService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
        service = new NutritionProfileService(repository, new NutritionCalculationService(), clock);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "42", "test-only", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void savesOnlyForAuthenticatedEligibleMemberAfterConsentWithoutReturningResults() {
        NutritionProfileEntity member = member();
        when(repository.findById(42L)).thenReturn(Optional.of(member));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.saveOwnProfile(new SaveNutritionProfileRequest(
                LocalDate.of(2000, 7, 1), "FEMALE", new BigDecimal("170.0"), new BigDecimal("65.0"),
                "SEDENTARY", "MAINTAIN_WEIGHT", false, false, false, true));

        assertFalse(response.eligible());
        assertEquals(null, response.results());
        assertTrue(response.hasProfile());
        assertTrue(member.isHealthDataConsent());
        assertEquals(localDateTimeUtc("2026-01-01T12:00:00"), member.getHealthDataConsentAt());
    }

    @Test
    void returnsResultsOnlyAfterSeparateEligibilityConfirmation() {
        NutritionProfileEntity member = member();
        member.setDateOfBirth(LocalDate.of(2000, 7, 1));
        member.setBiologicalSex("FEMALE");
        member.setHeightCm(new BigDecimal("170.0"));
        member.setWeightKg(new BigDecimal("65.0"));
        member.setActivityLevel("SEDENTARY");
        member.setNutritionGoal("MAINTAIN_WEIGHT");
        member.setHealthDataConsent(true);
        when(repository.findById(42L)).thenReturn(Optional.of(member));

        var response = service.calculateOwnResults(new tech.mamxanh.nutrition.dto.request.ConfirmNutritionEligibilityRequest(false, false, false));

        assertTrue(response.eligible());
        assertEquals("Bình thường", response.results().bmiCategory());
        assertEquals(8, response.results().dailyTargets().size()); // Eight nutrient targets plus energy = nine total.
    }

    @Test
    void profileReadReturnsSavedDataWithoutResultsOrCrossFeatureEligibilityFlags() {
        NutritionProfileEntity member = member();
        member.setDateOfBirth(LocalDate.of(2000, 7, 1));
        member.setBiologicalSex("FEMALE");
        member.setHeightCm(new BigDecimal("170.0"));
        member.setWeightKg(new BigDecimal("65.0"));
        member.setActivityLevel("SEDENTARY");
        member.setNutritionGoal("MAINTAIN_WEIGHT");
        member.setHealthDataConsent(true);
        member.setPregnant(true); // Another feature's stored flag does not control this FR's fresh declaration.
        when(repository.findById(42L)).thenReturn(Optional.of(member));

        var response = service.getOwnProfile();

        assertTrue(response.hasProfile());
        assertFalse(response.eligible());
        assertEquals(null, response.results());
        assertTrue(response.profile() != null);
    }

    @Test
    void blocksResultCalculationWhenAnyExclusionIsConfirmed() {
        NutritionProfileEntity member = member();
        member.setDateOfBirth(LocalDate.of(2000, 7, 1));
        member.setBiologicalSex("FEMALE");
        member.setHeightCm(new BigDecimal("170.0"));
        member.setWeightKg(new BigDecimal("65.0"));
        member.setActivityLevel("SEDENTARY");
        member.setNutritionGoal("MAINTAIN_WEIGHT");
        member.setHealthDataConsent(true);
        when(repository.findById(42L)).thenReturn(Optional.of(member));

        var exception = assertThrows(NutritionProfileException.class, () -> service.calculateOwnResults(
                new tech.mamxanh.nutrition.dto.request.ConfirmNutritionEligibilityRequest(true, false, false)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("NUTRITION_PROFILE_OUT_OF_SCOPE", exception.getCode());
    }

    @Test
    void rejectsIneligibleMemberWithoutSavingAnything() {
        when(repository.findById(42L)).thenReturn(Optional.of(member()));
        var exception = assertThrows(NutritionProfileException.class, () -> service.saveOwnProfile(new SaveNutritionProfileRequest(
                LocalDate.of(2000, 7, 1), "FEMALE", new BigDecimal("170"), new BigDecimal("65"),
                "SEDENTARY", "MAINTAIN_WEIGHT", true, false, false, true)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("NUTRITION_PROFILE_OUT_OF_SCOPE", exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void requiresNumericAuthenticatedMemberIdentity() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "not-a-user-id", "test-only", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        var exception = assertThrows(NutritionProfileException.class, service::getOwnProfile);
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertFalse(exception.getCode().isBlank());
        verify(repository, never()).findById(any());
    }

    @Test
    void rejectsUnauthenticatedRequestBeforeReadingAnyMemberProfile() {
        SecurityContextHolder.clearContext();

        var exception = assertThrows(NutritionProfileException.class, service::getOwnProfile);

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("AUTHENTICATION_REQUIRED", exception.getCode());
        verify(repository, never()).findById(any());
    }

    private static NutritionProfileEntity member() {
        NutritionProfileEntity member = new NutritionProfileEntity();
        member.setUserId(42L);
        member.setRole("CUSTOMER");
        member.setAccountStatus("ACTIVE");
        return member;
    }

    private static java.time.LocalDateTime localDateTimeUtc(String value) {
        return java.time.LocalDateTime.parse(value);
    }
}
