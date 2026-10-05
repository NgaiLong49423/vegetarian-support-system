package tech.mamxanh.expertapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tech.mamxanh.auth.service.ExpertPromotionService;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.expertapplication.dto.ExpertApplicationRequest;
import tech.mamxanh.expertapplication.dto.ExpertApplicationResponse;
import tech.mamxanh.expertapplication.repository.ExpertApplicationRepository;
import tech.mamxanh.expertapplication.service.ExpertApplicationService;
import tech.mamxanh.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class ExpertApplicationServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 12, 0);
    @Mock private ExpertApplicationRepository applications;
    @Mock private ExpertPromotionService promotion;
    @Mock private NotificationService notifications;
    private ExpertApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ExpertApplicationService(applications, promotion, notifications,
                Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("42", "test", List.of()));
    }

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void submitRequiresEligibleCustomerAndReturnsCreatedApplication() {
        var request = new ExpertApplicationRequest("  More than twenty chars experience  ", "VEGAN",
                "  A sample recipe summary with enough details.  ", "  https://example.org  ");
        var expected = response("PENDING", null);
        when(applications.insert(42, request, NOW)).thenReturn(9L);
        when(applications.findById(9L)).thenReturn(expected);

        assertThat(service.submit(request)).isSameAs(expected);

        verify(promotion).requireActiveCustomer(42);
        verify(applications).insert(42, request, NOW);
    }

    @Test
    void duplicatePendingApplicationBecomesConflict() {
        var request = new ExpertApplicationRequest("  More than twenty chars experience  ", "VEGAN",
                "  A sample recipe summary with enough details.  ", null);
        when(applications.insert(42, request, NOW)).thenThrow(new DataIntegrityViolationException("unique pending"));

        assertThatThrownBy(() -> service.submit(request)).isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void customerHistoryIsScopedToCallerAndIncludesCount() {
        var row = response("REJECTED", "Please add detail.");
        when(applications.page(42L, null, 1, 10)).thenReturn(List.of(row));
        when(applications.count(42L, null)).thenReturn(11L);

        var result = service.ownHistory(1, 10);

        assertThat(result.content()).containsExactly(row);
        assertThat(result.totalElements()).isEqualTo(11L);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void adminListUsesRequestedStatusAndPage() {
        var row = response("PENDING", null);
        when(applications.page(null, "PENDING", 0, 20)).thenReturn(List.of(row));
        when(applications.count(null, "PENDING")).thenReturn(1L);

        assertThat(service.adminList("PENDING", 0, 20).content()).containsExactly(row);
    }

    @Test
    void adminDetailDelegatesLookup() {
        var expected = response("PENDING", null);
        when(applications.findById(9L)).thenReturn(expected);

        assertThat(service.adminDetail(9L)).isSameAs(expected);
    }

    @Test
    void rejectPersistsDecisionAndNotifiesWithTrimmedReason() {
        var expected = response("REJECTED", "Insufficient practical detail.");
        when(applications.transitionPending(9L, "REJECTED", "Insufficient practical detail.", 42, NOW)).thenReturn(1);
        when(applications.applicantId(9L)).thenReturn(17L);
        when(applications.findById(9L)).thenReturn(expected);

        assertThat(service.reject(9L, "  Insufficient practical detail.  ")).isSameAs(expected);

        verify(notifications).expertApplicationDecision(17L, 9L, false, "Insufficient practical detail.");
    }

    @Test
    void staleRejectionDoesNotNotify() {
        when(applications.transitionPending(9L, "REJECTED", "Insufficient practical detail.", 42, NOW)).thenReturn(0);

        assertThatThrownBy(() -> service.reject(9L, "Insufficient practical detail."))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void missingAuthenticationIsUnauthorized() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("anonymousUser", ""));

        assertThatThrownBy(() -> service.ownHistory(0, 20)).isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private static ExpertApplicationResponse response(String status, String note) {
        return new ExpertApplicationResponse(9, 17, "Applicant", "applicant@example.org", "Experience text",
                "VEGAN", "Sample recipe summary", null, status, note, NOW, null);
    }
}
