package tech.mamxanh.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.MutableClock;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.auth.security.OneTimeTokenService;

/**
 * FR-03-A (#5): UC-03.1–UC-03.3, AC-03.1–AC-03.5 against a real SQL Server (Flyway V1→V3).
 * Outgoing email is mocked; the verification link is read from the captured email body.
 */
@ExtendWith(OutputCaptureExtension.class)
class EmailRegistrationIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "MatKhau123";
    private static final String VERIFY_SUBJECT = "Xác minh email tài khoản Mâm Xanh";
    private static final Pattern TOKEN_IN_LINK = Pattern.compile("/xac-minh-email\\?token=([A-Za-z0-9_-]+)");
    private static final Duration EMAIL_WAIT = Duration.ofSeconds(5);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OneTimeTokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void deleteUsers() {
        userRepository.deleteAll();
    }

    // ---------------------------------------------------------------- AC-03.1

    @Test
    void registerCreatesActiveUnverifiedAccountWithBcryptPasswordAndSendsVerificationLink() throws Exception {
        register("Nguyễn An", "  An.Nguyen@Example.com ", PASSWORD, PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());

        User user = userRepository.findByEmail("an.nguyen@example.com").orElseThrow();
        assertThat(user.getDisplayName()).isEqualTo("Nguyễn An");
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD).matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
        assertThat(Integer.parseInt(user.getPasswordHash().substring(4, 6))).isGreaterThanOrEqualTo(10);
        assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();

        String rawToken = awaitVerificationToken("an.nguyen@example.com");
        assertThat(user.getEmailVerificationToken())
                .isEqualTo(tokenService.hash(rawToken))
                .isNotEqualTo(rawToken);
        assertThat(user.getVerificationTokenExpiresAt()).isEqualTo(now().plusHours(24));
    }

    @Test
    void verificationLinkPointsToTheFrontendVerifyRoute() throws Exception {
        register("Nguyễn An", "an@example.com", PASSWORD, PASSWORD).andExpect(status().isCreated());

        assertThat(awaitEmailBodies("an@example.com", 1).get(0))
                .contains("http://localhost:5173/xac-minh-email?token=");
    }

    // ---------------------------------------------------------------- AC-03.2

    @Test
    void registerRejectsAnEmailThatIsAlreadyUsedIgnoringCase() throws Exception {
        register("Nguyễn An", "an@example.com", PASSWORD, PASSWORD).andExpect(status().isCreated());
        awaitEmailBodies("an@example.com", 1);

        register("Người Khác", "AN@EXAMPLE.COM", PASSWORD, PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"))
                .andExpect(jsonPath("$.status").value(409));

        assertThat(userRepository.count()).isEqualTo(1);
        verify(emailSender, after(500).times(1)).sendPlainText(anyString(), anyString(), anyString());
    }

    // ---------------------------------------------------------------- AC-03.3

    @ParameterizedTest
    @CsvSource({
            "Abcde1,        ít nhất 8 ký tự",
            "alllowercase1, chữ in hoa",
            "ALLUPPERCASE1, chữ thường",
            "NoDigitsHere,  chữ số",
    })
    void registerRejectsWeakPasswordNamingTheMissingCriterion(String password, String missingCriterion)
            throws Exception {
        register("Nguyễn An", "an@example.com", password, password)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].message",
                        hasItem(containsString(missingCriterion))));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void registerReportsEveryMissingPasswordCriterionAtOnce() throws Exception {
        register("Nguyễn An", "an@example.com", "abc", "abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].message", hasItems(
                        containsString("ít nhất 8 ký tự"), containsString("chữ in hoa"), containsString("chữ số"))));
    }

    @Test
    void registerRejectsPasswordLongerThan72Utf8Bytes() throws Exception {
        String accented = "Aa1" + "ệ".repeat(30);
        register("Nguyễn An", "an@example.com", accented, accented)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].message", hasItem(containsString("72 byte"))));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void registerValidatesDisplayNameEmailAndPasswordConfirmationTogether() throws Exception {
        register("ab", "not-an-email", PASSWORD, "Different123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field", hasItems("displayName", "email", "confirmPassword")));

        assertThat(userRepository.count()).isZero();
        verify(emailSender, after(300).never()).sendPlainText(anyString(), anyString(), anyString());
    }

    @Test
    void registerTrimsDisplayNameBeforeCheckingItsLength() throws Exception {
        register("   ab   ", "an@example.com", PASSWORD, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("displayName")));
    }

    // ---------------------------------------------------------------- AC-03.4

    @Test
    void verifyWithValidTokenMarksEmailVerifiedKeepsStatusAndConsumesToken() throws Exception {
        String token = registerAndAwaitToken("an@example.com");

        submitVerification(token).andExpect(status().isNoContent());

        User user = userRepository.findByEmail("an@example.com").orElseThrow();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getEmailVerificationToken()).isNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNull();

        submitVerification(token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_TOKEN_INVALID"));
    }

    @Test
    void verifyDoesNotUnlockAnAdministrativelyLockedAccount() throws Exception {
        String token = registerAndAwaitToken("an@example.com");
        jdbcTemplate.update("UPDATE [USER] SET account_status = 'LOCKED' WHERE email = ?", "an@example.com");

        submitVerification(token).andExpect(status().isNoContent());

        User user = userRepository.findByEmail("an@example.com").orElseThrow();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.LOCKED);
    }

    @Test
    void verifyAcceptsATokenOneSecondBeforeItExpires() throws Exception {
        String token = registerAndAwaitToken("an@example.com");
        clock.advance(Duration.ofHours(24).minusSeconds(1));

        submitVerification(token).andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------- AC-03.5

    @Test
    void verifyRejectsAnExpiredToken() throws Exception {
        String token = registerAndAwaitToken("an@example.com");
        clock.advance(Duration.ofHours(24).plusSeconds(1));

        submitVerification(token)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VERIFICATION_TOKEN_INVALID"));

        assertThat(userRepository.findByEmail("an@example.com").orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    void verifyRejectsUnknownAndMissingTokens() throws Exception {
        submitVerification(tokenService.issue().raw())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_TOKEN_INVALID"));

        submitVerification("")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void resendWithinSixtySecondsIsRejectedWithRetryAfter() throws Exception {
        registerAndAwaitToken("an@example.com");
        clock.advance(Duration.ofSeconds(30));

        resend("an@example.com")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
                .andExpect(jsonPath("$.code").value("RESEND_TOO_SOON"));
    }

    @Test
    void resendAfterCooldownIssuesANewTokenAndInvalidatesTheOldOne() throws Exception {
        String oldToken = registerAndAwaitToken("an@example.com");
        clock.advance(Duration.ofSeconds(61));

        resend("An@Example.com")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").isNotEmpty());

        List<String> bodies = awaitEmailBodies("an@example.com", 2);
        String newToken = extractToken(bodies.get(1));
        assertThat(newToken).isNotEqualTo(oldToken);

        submitVerification(oldToken).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_TOKEN_INVALID"));
        submitVerification(newToken).andExpect(status().isNoContent());
    }

    @Test
    void resendGivesTheSameNeutralAnswerForUnknownAndVerifiedEmailsWithoutSendingEmail() throws Exception {
        String token = registerAndAwaitToken("verified@example.com");
        submitVerification(token).andExpect(status().isNoContent());
        clearInvocations(emailSender);
        clock.advance(Duration.ofMinutes(5));

        String unknown = resend("nobody@example.com").andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String verified = resend("verified@example.com").andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(verified).isEqualTo(unknown);
        verify(emailSender, after(500).never()).sendPlainText(anyString(), anyString(), anyString());
    }

    @Test
    void resendRejectsAnInvalidEmail() throws Exception {
        resend("not-an-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")));
    }

    // ---------------------------------------------------------------- security baseline

    @Test
    void protectedEndpointsReturnProblemDetail401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/some-protected-resource"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void corsAllowsTheConfiguredFrontendOriginOnly() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));

        mockMvc.perform(options("/api/v1/auth/register")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logsNeverContainThePasswordOrTheVerificationToken(CapturedOutput output) throws Exception {
        String token = registerAndAwaitToken("an@example.com");
        submitVerification(token).andExpect(status().isNoContent());

        assertThat(output.getAll()).doesNotContain(PASSWORD).doesNotContain(token)
                .doesNotContain(tokenService.hash(token));
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions register(String displayName, String email, String password, String confirmPassword)
            throws Exception {
        String body = """
                {"displayName":"%s","email":"%s","password":"%s","confirmPassword":"%s"}
                """.formatted(displayName, email, password, confirmPassword);
        return mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions submitVerification(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/email-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(token)));
    }

    private ResultActions resend(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/email-verifications/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private String registerAndAwaitToken(String email) throws Exception {
        register("Nguyễn An", email, PASSWORD, PASSWORD).andExpect(status().isCreated());
        return awaitVerificationToken(email);
    }

    private String awaitVerificationToken(String email) {
        return extractToken(awaitEmailBodies(email, 1).get(0));
    }

    private List<String> awaitEmailBodies(String email, int expectedCount) {
        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(emailSender, timeout(EMAIL_WAIT.toMillis()).times(expectedCount))
                .sendPlainText(eq(email), eq(VERIFY_SUBJECT), bodies.capture());
        return bodies.getAllValues();
    }

    private static String extractToken(String emailBody) {
        Matcher matcher = TOKEN_IN_LINK.matcher(emailBody);
        assertThat(matcher.find()).as("verification link in email body").isTrue();
        return matcher.group(1);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(MutableClock.DEFAULT_START, ZoneOffset.UTC);
    }
}
