package tech.mamxanh.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.ApiException;

@ExtendWith(MockitoExtension.class)
class AccountAdministrationServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 12, 0);
    @Mock private UserRepository users;
    private AccountAdministrationService service;

    @BeforeEach
    void setUp() {
        service = new AccountAdministrationService(users,
                Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void locksAndUnlocksAccountOnlyWhenStatusChanges() {
        User customer = customer();
        when(users.findByIdForUpdate(42L)).thenReturn(Optional.of(customer));

        var locked = service.changeStatus(42L, AccountStatus.LOCKED);
        assertThat(locked.statusChanged()).isTrue();
        assertThat(customer.getAccountStatus()).isEqualTo(AccountStatus.LOCKED);
        assertThat(customer.getUpdatedAt()).isEqualTo(NOW);

        var unchanged = service.changeStatus(42L, AccountStatus.LOCKED);
        assertThat(unchanged.statusChanged()).isFalse();
        service.changeStatus(42L, AccountStatus.ACTIVE);
        assertThat(customer.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(users, org.mockito.Mockito.times(2)).saveAndFlush(customer);
    }

    @Test
    void changesCustomerAndExpertRolesAndSkipsUnchangedRole() {
        User customer = customer();
        when(users.findByIdForUpdate(42L)).thenReturn(Optional.of(customer));

        assertThat(service.changeRole(42L, Role.EXPERT).roleChanged()).isTrue();
        assertThat(customer.getRole()).isEqualTo(Role.EXPERT);
        assertThat(service.changeRole(42L, Role.EXPERT).roleChanged()).isFalse();
        service.changeRole(42L, Role.CUSTOMER);
        assertThat(customer.getRole()).isEqualTo(Role.CUSTOMER);
        verify(users, org.mockito.Mockito.times(2)).saveAndFlush(customer);
    }

    @Test
    void rejectsMissingAndAdministratorAccountsWithoutSaving() {
        when(users.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changeStatus(99L, AccountStatus.LOCKED))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        User administrator = User.registerWithPassword("admin@example.test", "hash", "Admin", NOW);
        org.springframework.test.util.ReflectionTestUtils.setField(administrator, "id", 7L);
        org.springframework.test.util.ReflectionTestUtils.setField(administrator, "role", Role.ADMIN);
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(administrator));
        assertThatThrownBy(() -> service.changeRole(7L, Role.EXPERT))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(administrator.getRole()).isEqualTo(Role.ADMIN);
        verify(users, never()).saveAndFlush(administrator);
    }

    @Test
    void rejectsAdministratorAsRequestedRole() {
        User customer = customer();
        when(users.findByIdForUpdate(42L)).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> service.changeRole(42L, Role.ADMIN))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(users, never()).saveAndFlush(customer);
    }

    private static User customer() {
        User user = User.registerWithPassword("user@example.test", "hash", "Người dùng", NOW);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }
}
