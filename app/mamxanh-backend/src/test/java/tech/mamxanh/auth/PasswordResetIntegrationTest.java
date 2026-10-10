package tech.mamxanh.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;

/**
 * FR-03-E (#9): UC-03.6, UC-03.7, AC-03.14, Q27 and the follow-up decisions Q47/Q48 against a real
 * SQL Server. Time is controlled through the shared {@code MutableClock}; outgoing email is mocked.
 */
@ExtendWith(OutputCaptureExtension.class)
class PasswordResetIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "an@example.com";
    private static final String PASSWORD = "MatKhau123";
    private static final String NEW_PASSWORD = "MatKhauMoi456";
    private static final String RESET_SUBJECT = "Đặt lại mật khẩu tài khoản Mâm Xanh";
    private static final String NEUTRAL_MESSAGE =
            "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn.";
    private static final Pattern TOKEN_IN_LINK = Pattern.compile("/dat-lai-mat-khau\\?token=([A-Za-z0-9_-]+)");
    private static final Duration EMAIL_WAIT = Duration.ofSeconds(5);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void deleteUsers() {
        userRepository.deleteAll();
    }

    // ---------------------------------------------------------------- UC-03.6 request

    @Test
    void requestForAnAccountReturnsTheNeutral202AndEmailsAFifteenMinuteLink() throws Exception {
        createVerifiedUser(EMAIL);

        requestReset("  An@Example.com ")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(NEUTRAL_MESSAGE));

        String token = awaitResetToken(EMAIL);
        Map<String, Object> row = userRow(EMAIL);
        assertThat(row.get("password_reset_token")).isEqualTo(sha256(token));
        assertThat(row.get("reset_token_expires_at")).isEqualTo(java.sql.Timestamp.valueOf(now().plusMinutes(15)));
        assertThat(row.get("password_reset_window_count")).isEqualTo(1);
    }

    @Test
    void requestForAnUnknownEmailGetsTheSameAnswerAndNoEmail() throws Exception {
        requestReset("nobody@example.com")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(NEUTRAL_MESSAGE));

        verify(emailSender, after(500).never()).sendPlainText(anyString(), anyString(), anyString());
    }

    @Test
    void aLockedAccountGetsTheSameAnswerButNoEmailAndNoToken() throws Exception {
        createVerifiedUser(EMAIL);
        jdbcTemplate.update("UPDATE [USER] SET account_status = 'LOCKED' WHERE email = ?", EMAIL);

        requestReset(EMAIL).andExpect(status().isAccepted()).andExpect(jsonPath("$.message").value(NEUTRAL_MESSAGE));

        verify(emailSender, after(500).never()).sendPlainText(anyString(), anyString(), anyString());
        assertThat(userRow(EMAIL).get("password_reset_token")).isNull();
    }

    @Test
    void aSecondRequestWithinSixtySecondsSendsNoEmailAndKeepsTheFirstToken() throws Exception {
        createVerifiedUser(EMAIL);
        requestReset(EMAIL).andExpect(status().isAccepted());
        String first = awaitResetToken(EMAIL);
        clock.advance(Duration.ofSeconds(59));

        requestReset(EMAIL).andExpect(status().isAccepted()).andExpect(jsonPath("$.message").value(NEUTRAL_MESSAGE));

        verify(emailSender, after(500).times(1)).sendPlainText(eq(EMAIL), eq(RESET_SUBJECT), anyString());
        assertThat(userRow(EMAIL).get("password_reset_token")).isEqualTo(sha256(first));
    }

    @Test
    void anEmailAfterTheCooldownReplacesTheTokenSoTheOldLinkStopsWorking() throws Exception {
        createVerifiedUser(EMAIL);
        requestReset(EMAIL);
        String first = awaitResetToken(EMAIL);
        clock.advance(Duration.ofSeconds(60));

        requestReset(EMAIL).andExpect(status().isAccepted());
        String second = awaitResetBodies(EMAIL, 2).get(1);
        String secondToken = extractToken(second);

        assertThat(secondToken).isNotEqualTo(first);
        expectInvalidToken(confirm(first, NEW_PASSWORD, NEW_PASSWORD));
        confirm(secondToken, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void atMostFiveEmailsAreSentPerHourAndTheSixthRequestIsStillNeutral() throws Exception {
        createVerifiedUser(EMAIL);
        for (int request = 1; request <= 5; request++) {
            requestReset(EMAIL).andExpect(status().isAccepted());
            clock.advance(Duration.ofSeconds(61));
        }
        awaitResetBodies(EMAIL, 5);

        requestReset(EMAIL).andExpect(status().isAccepted()).andExpect(jsonPath("$.message").value(NEUTRAL_MESSAGE));
        verify(emailSender, after(500).times(5)).sendPlainText(eq(EMAIL), eq(RESET_SUBJECT), anyString());

        clock.advance(Duration.ofMinutes(55));
        requestReset(EMAIL).andExpect(status().isAccepted());
        awaitResetBodies(EMAIL, 6);
    }

    @Test
    void parallelRequestsForOneAccountSendOnlyOneEmail() throws Exception {
        createVerifiedUser(EMAIL);
        List<Callable<Integer>> requests = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            requests.add(() -> requestReset(EMAIL).andReturn().getResponse().getStatus());
        }

        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(5)) {
            for (Future<Integer> result : pool.invokeAll(requests)) {
                statuses.add(result.get());
            }
        }

        assertThat(statuses).containsOnly(202);
        verify(emailSender, after(1000).times(1)).sendPlainText(eq(EMAIL), eq(RESET_SUBJECT), anyString());
        assertThat(userRow(EMAIL).get("password_reset_window_count")).isEqualTo(1);
    }

    @Test
    void anInvalidEmailIsAValidationError() throws Exception {
        requestReset("khong-phai-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    // ---------------------------------------------------------------- UC-03.7 confirm, AC-03.14

    @Test
    void aValidTokenSetsTheNewPasswordAndIsUsedOnlyOnce() throws Exception {
        createVerifiedUser(EMAIL);
        String token = requestAndAwaitToken(EMAIL);

        confirm(token, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        Map<String, Object> row = userRow(EMAIL);
        assertThat(passwordEncoder.matches(NEW_PASSWORD, (String) row.get("password_hash"))).isTrue();
        assertThat(((String) row.get("password_hash"))).startsWith("$2");
        assertThat(row.get("password_reset_token")).isNull();
        assertThat(row.get("reset_token_expires_at")).isNull();
        login(EMAIL, NEW_PASSWORD).andExpect(status().isOk());
        login(EMAIL, PASSWORD).andExpect(status().isUnauthorized());
        expectInvalidToken(confirm(token, "MatKhauKhac789", "MatKhauKhac789"));
    }

    @Test
    void parallelConfirmationsWithOneTokenChangeThePasswordOnce() throws Exception {
        createVerifiedUser(EMAIL);
        String token = requestAndAwaitToken(EMAIL);
        List<Callable<Integer>> confirmations = List.of(
                () -> confirm(token, NEW_PASSWORD, NEW_PASSWORD).andReturn().getResponse().getStatus(),
                () -> confirm(token, "MatKhauKhac789", "MatKhauKhac789").andReturn().getResponse().getStatus());

        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            for (Future<Integer> result : pool.invokeAll(confirmations)) {
                statuses.add(result.get());
            }
        }

        assertThat(statuses).containsExactlyInAnyOrder(204, 400);
        assertThat(userRow(EMAIL).get("password_reset_token")).isNull();
    }

    @Test
    void anExpiredTokenIsRejectedAndThePasswordIsUnchanged() throws Exception {
        createVerifiedUser(EMAIL);
        String token = requestAndAwaitToken(EMAIL);
        clock.advance(Duration.ofMinutes(15));

        expectInvalidToken(confirm(token, NEW_PASSWORD, NEW_PASSWORD));

        login(EMAIL, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void anUnknownTokenIsRejected() throws Exception {
        createVerifiedUser(EMAIL);

        expectInvalidToken(confirm("khong-ton-tai", NEW_PASSWORD, NEW_PASSWORD));
    }

    @Test
    void resettingAnUnverifiedAccountVerifiesItsEmail() throws Exception {
        userRepository.save(User.registerWithPassword(EMAIL, passwordEncoder.encode(PASSWORD), "Nguyễn An", now()));
        String token = requestAndAwaitToken(EMAIL);

        confirm(token, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        assertThat(userRow(EMAIL).get("email_verified")).isEqualTo(true);
        login(EMAIL, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void aGoogleOnlyAccountCanSetAPasswordThroughReset() throws Exception {
        createVerifiedUser(EMAIL);
        jdbcTemplate.update("UPDATE [USER] SET password_hash = NULL, google_subject = 'google-sub-1' WHERE email = ?", EMAIL);
        String token = requestAndAwaitToken(EMAIL);

        confirm(token, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        login(EMAIL, NEW_PASSWORD).andExpect(status().isOk());
        assertThat(userRow(EMAIL).get("google_subject")).isEqualTo("google-sub-1");
    }

    @Test
    void anAccountLockedAfterTheEmailCannotUseTheLink() throws Exception {
        createVerifiedUser(EMAIL);
        String token = requestAndAwaitToken(EMAIL);
        jdbcTemplate.update("UPDATE [USER] SET account_status = 'LOCKED' WHERE email = ?", EMAIL);

        confirm(token, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

        Map<String, Object> row = userRow(EMAIL);
        assertThat(passwordEncoder.matches(PASSWORD, (String) row.get("password_hash"))).isTrue();
    }

    @Test
    void aResetEndsATemporaryLoginBlock() throws Exception {
        createVerifiedUser(EMAIL);
        for (int attempt = 0; attempt < 5; attempt++) {
            login(EMAIL, "SaiMatKhau999");
        }
        login(EMAIL, PASSWORD).andExpect(status().isTooManyRequests());
        String token = requestAndAwaitToken(EMAIL);

        confirm(token, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        login(EMAIL, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void theCurrentPasswordIsRejectedAsTheNewOneAndTheTokenStaysUsable() throws Exception {
        createVerifiedUser(EMAIL);
        String token = requestAndAwaitToken(EMAIL);

        confirm(token, PASSWORD, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NEW_PASSWORD_SAME_AS_CURRENT"));

        assertThat(userRow(EMAIL).get("password_reset_token")).isEqualTo(sha256(token));
        confirm(token, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void theNewPasswordMustFollowThePasswordRules() throws Exception {
        createVerifiedUser(EMAIL);
        String token = requestAndAwaitToken(EMAIL);

        expectFieldError(confirm(token, "matkhauyeu1", "matkhauyeu1"), "newPassword");
        expectFieldError(confirm(token, NEW_PASSWORD, "MatKhauKhac456"), "confirmPassword");
        String over72Bytes = "Ằ".repeat(25) + "Ab1";
        assertThat(over72Bytes.length()).isLessThanOrEqualTo(64);
        assertThat(over72Bytes.getBytes(StandardCharsets.UTF_8).length).isGreaterThan(72);
        expectFieldError(confirm(token, over72Bytes, over72Bytes), "newPassword");
        expectFieldError(confirm("", NEW_PASSWORD, NEW_PASSWORD), "token");

        assertThat(userRow(EMAIL).get("password_reset_token")).isEqualTo(sha256(token));
    }

    // ---------------------------------------------------------------- security

    @Test
    void logsNeverContainTheResetTokenOrThePasswords(CapturedOutput output) throws Exception {
        createVerifiedUser(EMAIL);
        String token = requestAndAwaitToken(EMAIL);
        confirm(token, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        assertThat(output.getAll()).doesNotContain(token).doesNotContain(NEW_PASSWORD).doesNotContain(PASSWORD);
    }

    @Test
    void aFailedEmailIsLoggedWithoutTheAddressOrTheLinkAndTheAnswerStaysNeutral(CapturedOutput output)
            throws Exception {
        createVerifiedUser(EMAIL);
        doThrow(new IllegalStateException("SMTP refused " + EMAIL)).when(emailSender)
                .sendPlainText(eq(EMAIL), eq(RESET_SUBJECT), anyString());

        requestReset(EMAIL).andExpect(status().isAccepted()).andExpect(jsonPath("$.message").value(NEUTRAL_MESSAGE));

        String token = awaitResetToken(EMAIL);
        long deadline = System.nanoTime() + EMAIL_WAIT.toNanos();
        while (!output.getAll().contains("Password reset email delivery failed") && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertThat(output.getAll()).contains("Password reset email delivery failed: IllegalStateException")
                .doesNotContain(EMAIL).doesNotContain(token);
    }

    @Test
    void generatedOpenApiPublishesBothPublicOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/password-resets'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/password-resets'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/auth/password-resets/confirm'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/password-resets/confirm'].post.security").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.PasswordResetConfirmRequest.required").isArray());
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions requestReset(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-resets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private ResultActions confirm(String token, String newPassword, String confirmPassword) throws Exception {
        String body = "{\"token\":\"%s\",\"newPassword\":\"%s\",\"confirmPassword\":\"%s\"}"
                .formatted(token, newPassword, confirmPassword);
        return mockMvc.perform(post("/api/v1/auth/password-resets/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private String requestAndAwaitToken(String email) throws Exception {
        requestReset(email).andExpect(status().isAccepted());
        return awaitResetToken(email);
    }

    private String awaitResetToken(String email) {
        return extractToken(awaitResetBodies(email, 1).get(0));
    }

    private List<String> awaitResetBodies(String email, int expectedCount) {
        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(emailSender, timeout(EMAIL_WAIT.toMillis()).times(expectedCount))
                .sendPlainText(eq(email), eq(RESET_SUBJECT), bodies.capture());
        return bodies.getAllValues();
    }

    private static String extractToken(String emailBody) {
        Matcher matcher = TOKEN_IN_LINK.matcher(emailBody);
        assertThat(matcher.find()).as("reset link in email body").isTrue();
        return matcher.group(1);
    }

    private static void expectInvalidToken(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("PASSWORD_RESET_TOKEN_INVALID"))
                .andExpect(header().doesNotExist("Retry-After"));
    }

    private static void expectFieldError(ResultActions result, String field) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field == '%s')]".formatted(field)).exists());
    }

    private User createVerifiedUser(String email) {
        User user = User.registerWithPassword(email, passwordEncoder.encode(PASSWORD), "Nguyễn An", now());
        user.markEmailVerified(now());
        return userRepository.save(user);
    }

    private Map<String, Object> userRow(String email) {
        return jdbcTemplate.queryForMap("SELECT password_hash, password_reset_token, reset_token_expires_at, "
                + "password_reset_window_count, email_verified, google_subject FROM [USER] WHERE email = ?", email);
    }

    private static String sha256(String token) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
