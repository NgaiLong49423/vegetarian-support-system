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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

    @Test
    void returnsEmptyResponseWhenMemberHasNoSavedProfile() {
        when(repository.findById(42L)).thenReturn(Optional.of(member()));

        var response = service.getOwnProfile();

        assertFalse(response.hasProfile());
        assertEquals(null, response.profile());
        assertEquals(null, response.results());
    }

    @Test
    void returnsOutOfScopeReasonForSavedProfileOfUnderageMember() {
        NutritionProfileEntity member = completeMember();
        member.setDateOfBirth(LocalDate.of(2010, 7, 1));
        when(repository.findById(42L)).thenReturn(Optional.of(member));

        var response = service.getOwnProfile();

        assertTrue(response.hasProfile());
        assertFalse(response.eligible());
        assertEquals(List.of("AGE_OUT_OF_SCOPE"), response.outOfScopeReasons());
        assertEquals(null, response.profile());
    }

    @Test
    void requiresACompleteSavedProfileBeforeCalculating() {
        when(repository.findById(42L)).thenReturn(Optional.of(member()));

        var exception = assertThrows(NutritionProfileException.class, () -> service.calculateOwnResults(
                new tech.mamxanh.nutrition.dto.request.ConfirmNutritionEligibilityRequest(false, false, false)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("NUTRITION_PROFILE_REQUIRED", exception.getCode());
    }

    @Test
    void rejectsAllUnsupportedEligibilityAnswersWithoutPersisting() {
        NutritionProfileEntity member = completeMember();
        when(repository.findById(42L)).thenReturn(Optional.of(member));

        for (var confirmation : List.of(
                new tech.mamxanh.nutrition.dto.request.ConfirmNutritionEligibilityRequest(true, false, false),
                new tech.mamxanh.nutrition.dto.request.ConfirmNutritionEligibilityRequest(false, true, false),
                new tech.mamxanh.nutrition.dto.request.ConfirmNutritionEligibilityRequest(false, false, true))) {
            var exception = assertThrows(NutritionProfileException.class, () -> service.calculateOwnResults(confirmation));
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
            assertEquals("NUTRITION_PROFILE_OUT_OF_SCOPE", exception.getCode());
        }
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsInvalidProfileDatesMeasurementsConsentAndSelections() {
        when(repository.findById(42L)).thenReturn(Optional.of(member()));

        assertRejected(saveRequest(LocalDate.of(1899, 12, 31), "FEMALE", "170", "65", true));
        assertRejected(saveRequest(LocalDate.of(2026, 1, 2), "FEMALE", "170", "65", true));
        assertRejected(saveRequest(LocalDate.of(2000, 1, 1), "FEMALE", "99.9", "65", true));
        assertRejected(saveRequest(LocalDate.of(2000, 1, 1), "FEMALE", "250.1", "65", true));
        assertRejected(saveRequest(LocalDate.of(2000, 1, 1), "FEMALE", "170", "29.9", true));
        assertRejected(saveRequest(LocalDate.of(2000, 1, 1), "FEMALE", "170", "300.1", true));
        assertRejected(saveRequest(LocalDate.of(2000, 1, 1), "FEMALE", "170", "65", false));
        assertRejected(new SaveNutritionProfileRequest(LocalDate.of(2000, 1, 1), "OTHER", new BigDecimal("170"),
                new BigDecimal("65"), "SEDENTARY", "MAINTAIN_WEIGHT", false, false, false, true));
        assertRejected(new SaveNutritionProfileRequest(LocalDate.of(2000, 1, 1), "FEMALE", new BigDecimal("170"),
                new BigDecimal("65"), "UNKNOWN", "MAINTAIN_WEIGHT", false, false, false, true));
        assertRejected(new SaveNutritionProfileRequest(LocalDate.of(2000, 1, 1), "FEMALE", new BigDecimal("170"),
                new BigDecimal("65"), "SEDENTARY", "UNKNOWN", false, false, false, true));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsNonMemberAndInactiveAccountsAndMissingMembers() {
        NutritionProfileEntity inactive = member();
        inactive.setAccountStatus("SUSPENDED");
        when(repository.findById(43L)).thenReturn(Optional.of(inactive));
        NutritionProfileEntity administrator = member();
        administrator.setRole("ADMIN");
        when(repository.findById(45L)).thenReturn(Optional.of(administrator));
        when(repository.findById(44L)).thenReturn(Optional.empty());

        SecurityContextHolder.getContext().setAuthentication(authenticatedAs("43"));
        var inactiveException = assertThrows(NutritionProfileException.class, service::getOwnProfile);
        assertEquals(HttpStatus.FORBIDDEN, inactiveException.getStatus());

        SecurityContextHolder.getContext().setAuthentication(authenticatedAs("45"));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(NutritionProfileException.class, service::getOwnProfile).getStatus());

        SecurityContextHolder.getContext().setAuthentication(authenticatedAs("44"));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(NutritionProfileException.class, service::getOwnProfile).getStatus());

        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.unauthenticated("42", "test-only"));
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(NutritionProfileException.class, service::getOwnProfile).getStatus());

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "test-key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(NutritionProfileException.class, service::getOwnProfile).getStatus());
    }

    private void assertRejected(SaveNutritionProfileRequest request) {
        assertThrows(NutritionProfileException.class, () -> service.saveOwnProfile(request));
    }

    private static SaveNutritionProfileRequest saveRequest(LocalDate dob, String sex, String height, String weight, boolean consent) {
        return new SaveNutritionProfileRequest(dob, sex, new BigDecimal(height), new BigDecimal(weight),
                "SEDENTARY", "MAINTAIN_WEIGHT", false, false, false, consent);
    }

    private static UsernamePasswordAuthenticationToken authenticatedAs(String userId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                userId, "test-only", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private static NutritionProfileEntity completeMember() {
        NutritionProfileEntity member = member();
        member.setDateOfBirth(LocalDate.of(2000, 7, 1));
        member.setBiologicalSex("FEMALE");
        member.setHeightCm(new BigDecimal("170.0"));
        member.setWeightKg(new BigDecimal("65.0"));
        member.setActivityLevel("SEDENTARY");
        member.setNutritionGoal("MAINTAIN_WEIGHT");
        member.setHealthDataConsent(true);
        return member;
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
