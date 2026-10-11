package tech.mamxanh.auth.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.security.crypto.password.PasswordEncoder;

class LocalDemoCredentialsInitializerTest {

    @Test
    void hashesLocalPasswordAndAssignsItToExpectedDemoAccounts() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode("Valid-local-Password-123")).thenReturn("$2a$12$encoded");
        when(jdbcTemplate.update(anyString(), any(PreparedStatementSetter.class))).thenReturn(5);

        new LocalDemoCredentialsInitializer(jdbcTemplate, passwordEncoder, "Valid-local-Password-123")
                .run(null);

        var setter = org.mockito.ArgumentCaptor.forClass(PreparedStatementSetter.class);
        verify(jdbcTemplate).update(eq("UPDATE [USER] SET password_hash = ?, email_verified = 1 "
                + "WHERE email IN (?,?,?,?,?)"), setter.capture());
        PreparedStatement statement = mock(PreparedStatement.class);
        setter.getValue().setValues(statement);
        verify(statement).setString(1, "$2a$12$encoded");
        verify(statement).setString(2, "demo-customer@mamxanh.local");
        verify(statement).setString(3, "demo-new-member@mamxanh.local");
        verify(statement).setString(4, "demo-applicant@mamxanh.local");
        verify(statement).setString(5, "demo-expert@mamxanh.local");
        verify(statement).setString(6, "demo-admin@mamxanh.local");
    }

    @Test
    void rejectsPasswordOutsideBcryptByteLimitBeforeDatabaseWrite() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        assertThrows(IllegalStateException.class,
                () -> new LocalDemoCredentialsInitializer(jdbcTemplate, passwordEncoder, "short").run(null));
        verify(jdbcTemplate, org.mockito.Mockito.never()).update(anyString(), any(PreparedStatementSetter.class));
    }

    @Test
    void failsWhenRepeatableSeedDidNotCreateAllDemoAccounts() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");
        when(jdbcTemplate.update(anyString(), any(PreparedStatementSetter.class))).thenReturn(4);
        assertThrows(IllegalStateException.class,
                () -> new LocalDemoCredentialsInitializer(jdbcTemplate, passwordEncoder, "Valid-local-Password-123")
                        .run(null));
    }
}
