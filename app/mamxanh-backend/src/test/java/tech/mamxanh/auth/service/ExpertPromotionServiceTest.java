package tech.mamxanh.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.ApiException;

class ExpertPromotionServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ExpertPromotionService service = new ExpertPromotionService(users,
            Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void promotionRequiresActiveCustomerAccount() {
        var applicant = mock(User.class);
        when(users.findByIdForUpdate(17L)).thenReturn(Optional.of(applicant));
        when(applicant.getAccountStatus()).thenReturn(AccountStatus.LOCKED);
        when(applicant.getRole()).thenReturn(Role.CUSTOMER);

        assertThatThrownBy(() -> service.promoteActiveCustomer(17L)).isInstanceOf(ApiException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((ApiException) error).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void activeCustomerCanBePromotedAndSubmissionRequiresActiveCustomer() {
        var applicant = mock(User.class);
        when(users.findByIdForUpdate(17L)).thenReturn(Optional.of(applicant));
        when(applicant.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(applicant.getRole()).thenReturn(Role.CUSTOMER);

        service.promoteActiveCustomer(17L);
        service.requireActiveCustomer(17L);

        verify(applicant).promoteToExpert(java.time.LocalDateTime.of(2026, 10, 4, 12, 0));
        verify(users).findById(17L);
    }

    @Test
    void missingApplicantCannotBeApproved() {
        when(users.findByIdForUpdate(17L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.promoteActiveCustomer(17L)).isInstanceOf(ApiException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((ApiException) error).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
    }
}
