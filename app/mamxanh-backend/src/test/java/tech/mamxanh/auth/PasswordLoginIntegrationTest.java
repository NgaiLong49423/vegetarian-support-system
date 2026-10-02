package tech.mamxanh.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;

/**
 * FR-03-B (#6): UC-03.4, AC-03.6–AC-03.9, NFR-07 and the account-status check of NFR-09 against
 * a real SQL Server (Flyway V1→V4). Time is controlled through the shared {@code MutableClock}.
 */
@ExtendWith(OutputCaptureExtension.class)
class PasswordLoginIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "an@example.com";
    private static final String PASSWORD = "MatKhau123";
    private static final String WRONG_PASSWORD = "SaiMatKhau9";
    private static final String NEUTRAL_MESSAGE = "Email hoặc mật khẩu không chính xác.";
    private static final String PROTECTED_PATH = "/api/v1/some-protected-resource";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void deleteUsers() {
        userRepository.deleteAll();
    }

    // ---------------------------------------------------------------- AC-03.6

    @Test
    void loginWithCorrectCredentialsReturnsAStatelessAccessTokenAndTheAccount() throws Exception {
        User user = createVerifiedUser(EMAIL);

        login("  An@Example.com ", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.account.id").value(user.getId()))
                .andExpect(jsonPath("$.account.email").value(EMAIL))
                .andExpect(jsonPath("$.account.displayName").value("Nguyễn An"))
                .andExpect(jsonPath("$.account.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.account.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.account.emailVerified").value(true))
                .andExpect(jsonPath("$.account.passwordHash").doesNotExist());
    }

    @Test
    void accessTokenCarriesSubjectRoleIssuerAndConfiguredLifetimeWithoutSessionId() throws Exception {
        User user = createVerifiedUser(EMAIL);

        var jwt = jwtDecoder.decode(accessToken(EMAIL, PASSWORD));

        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CUSTOMER");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("mamxanh");
        assertThat(jwt.getIssuedAt()).isEqualTo(clock.instant());
        assertThat(jwt.getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(15)));
        assertThat(jwt.getClaims()).doesNotContainKeys("sid", "email");
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    void successfulLoginResetsPreviousFailures() throws Exception {
        createVerifiedUser(EMAIL);
        login(EMAIL, WRONG_PASSWORD).andExpect(status().isUnauthorized());
        login(EMAIL, WRONG_PASSWORD).andExpect(status().isUnauthorized());

        login(EMAIL, PASSWORD).andExpect(status().isOk());

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLoginBlockedUntil()).isNull();
    }

    // ---------------------------------------------------------------- AC-03.7

    @Test
    void wrongPasswordIsRejectedNeutrallyAndCounted() throws Exception {
        createVerifiedUser(EMAIL);

        expectInvalidCredentials(login(EMAIL, WRONG_PASSWORD));

        assertThat(userRepository.findByEmail(EMAIL).orElseThrow().getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void unknownEmailGetsTheSameAnswerAsAWrongPassword() throws Exception {
        createVerifiedUser(EMAIL);

        expectInvalidCredentials(login("khong-ton-tai@example.com", PASSWORD));
    }

    @Test
    void passwordLongerThanBcryptAcceptsIsAWrongPasswordNotAServerError() throws Exception {
        createVerifiedUser(EMAIL);

        expectInvalidCredentials(login(EMAIL, "Ă".repeat(37)));
        assertThat(userRepository.findByEmail(EMAIL).orElseThrow().getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void googleOnlyAccountCannotLogInWithAPassword() throws Exception {
        createVerifiedUser(EMAIL);
        jdbcTemplate.update("UPDATE [USER] SET password_hash = NULL, google_subject = 'google-123' WHERE email = ?", EMAIL);

        expectInvalidCredentials(login(EMAIL, PASSWORD));
    }

    @Test
    void loginValidatesTheRequestShape() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"khong-phai-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    // ---------------------------------------------------------------- account state

    @Test
    void unverifiedEmailIsRejectedOnlyAfterTheCorrectPassword() throws Exception {
        userRepository.save(User.registerWithPassword(EMAIL, passwordEncoder.encode(PASSWORD), "Nguyễn An", now()));

        expectInvalidCredentials(login(EMAIL, WRONG_PASSWORD));
        login(EMAIL, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void administrativelyLockedAccountIsRejectedOnlyAfterTheCorrectPassword() throws Exception {
        createVerifiedUser(EMAIL);
        lock(EMAIL);

        expectInvalidCredentials(login(EMAIL, WRONG_PASSWORD));
        login(EMAIL, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    // ---------------------------------------------------------------- AC-03.8

    @Test
    void fifthWrongPasswordBlocksLoginForTenMinutesWithoutLockingTheAccount() throws Exception {
        createVerifiedUser(EMAIL);
        for (int attempt = 1; attempt <= 4; attempt++) {
            expectInvalidCredentials(login(EMAIL, WRONG_PASSWORD));
        }

        login(EMAIL, WRONG_PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "600"))
                .andExpect(jsonPath("$.code").value("LOGIN_TEMPORARILY_BLOCKED"))
                .andExpect(jsonPath("$.detail").value("Bạn đã nhập sai mật khẩu 5 lần liên tiếp. Vui lòng thử lại sau 10 phút."));

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLoginBlockedUntil()).isEqualTo(now().plusMinutes(10));
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void blockedAccountIsRejectedBeforeThePasswordIsChecked() throws Exception {
        createVerifiedUser(EMAIL);
        blockByWrongPasswords(EMAIL);
        clock.advance(Duration.ofMinutes(4));

        login(EMAIL, PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "360"))
                .andExpect(jsonPath("$.detail").value("Bạn đã nhập sai mật khẩu 5 lần liên tiếp. Vui lòng thử lại sau 6 phút."));

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLoginBlockedUntil()).isEqualTo(now().plusMinutes(6));
    }

    @Test
    void blockOnOneAccountDoesNotAffectAnotherAccount() throws Exception {
        createVerifiedUser(EMAIL);
        createVerifiedUser("binh@example.com");
        blockByWrongPasswords(EMAIL);

        login("binh@example.com", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void concurrentWrongPasswordsAreAllCounted() throws Exception {
        createVerifiedUser(EMAIL);
        List<Callable<Integer>> attempts = new ArrayList<>();
        for (int attempt = 0; attempt < 5; attempt++) {
            attempts.add(() -> login(EMAIL, WRONG_PASSWORD).andReturn().getResponse().getStatus());
        }

        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(5)) {
            for (Future<Integer> result : pool.invokeAll(attempts)) {
                statuses.add(result.get());
            }
        }

        assertThat(statuses).containsOnly(401, 429).filteredOn(code -> code == 429).hasSize(1);
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLoginBlockedUntil()).isNotNull();
    }

    // ---------------------------------------------------------------- AC-03.9

    @Test
    void correctPasswordAfterTenMinutesLogsInAndClearsTheCounter() throws Exception {
        createVerifiedUser(EMAIL);
        blockByWrongPasswords(EMAIL);
        clock.advance(Duration.ofMinutes(10));

        login(EMAIL, PASSWORD).andExpect(status().isOk());

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLoginBlockedUntil()).isNull();
    }

    @Test
    void wrongPasswordAfterTheBlockExpiresStartsANewCount() throws Exception {
        createVerifiedUser(EMAIL);
        blockByWrongPasswords(EMAIL);
        clock.advance(Duration.ofMinutes(10));

        expectInvalidCredentials(login(EMAIL, WRONG_PASSWORD));

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getLoginBlockedUntil()).isNull();
    }

    // ---------------------------------------------------------------- Bearer token on protected requests

    @Test
    void validBearerTokenAuthenticatesProtectedRequests() throws Exception {
        createVerifiedUser(EMAIL);
        String token = accessToken(EMAIL, PASSWORD);

        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void administrativeLockBlocksAnAlreadyIssuedTokenImmediately() throws Exception {
        createVerifiedUser(EMAIL);
        String token = accessToken(EMAIL, PASSWORD);
        lock(EMAIL);

        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        createVerifiedUser(EMAIL);
        String token = accessToken(EMAIL, PASSWORD);

        clock.advance(Duration.ofMinutes(15).minusSeconds(1));
        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());

        // Spring's JwtTimestampValidator rejects a token once the clock is past exp.
        clock.advance(Duration.ofSeconds(2));
        expectUnauthenticated(token);
    }

    @Test
    void tamperedForeignAndOrphanTokensAreRejected() throws Exception {
        User user = createVerifiedUser(EMAIL);
        String token = accessToken(EMAIL, PASSWORD);
        String[] parts = token.split("\\.");
        String elevatedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                        .replace("CUSTOMER", "ADMIN").getBytes(StandardCharsets.UTF_8));

        expectUnauthenticated(parts[0] + "." + elevatedPayload + "." + parts[2]);
        expectUnauthenticated(signedWith("another-secret-that-is-at-least-32-bytes-long", "mamxanh", user.getId()));
        expectUnauthenticated(signedWith("integration-test-only-signing-secret-0123456789", "someone-else", user.getId()));

        userRepository.deleteAll();
        expectUnauthenticated(token);
    }

    @Test
    void logsNeverContainThePasswordOrTheAccessToken(CapturedOutput output) throws Exception {
        createVerifiedUser(EMAIL);
        String token = accessToken(EMAIL, PASSWORD);
        login(EMAIL, WRONG_PASSWORD).andExpect(status().isUnauthorized());
        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        assertThat(output.getAll()).doesNotContain(PASSWORD).doesNotContain(WRONG_PASSWORD).doesNotContain(token);
    }

    // ---------------------------------------------------------------- helpers

    private User createVerifiedUser(String email) {
        User user = User.registerWithPassword(email, passwordEncoder.encode(PASSWORD), "Nguyễn An", now());
        user.markEmailVerified(now());
        return userRepository.save(user);
    }

    private void lock(String email) {
        jdbcTemplate.update("UPDATE [USER] SET account_status = 'LOCKED' WHERE email = ?", email);
    }

    private void blockByWrongPasswords(String email) throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            login(email, WRONG_PASSWORD);
        }
        assertThat(userRepository.findByEmail(email).orElseThrow().getLoginBlockedUntil()).isNotNull();
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String accessToken(String email, String password) throws Exception {
        MvcResult result = login(email, password).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.accessToken");
    }

    private void expectInvalidCredentials(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value(NEUTRAL_MESSAGE))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    private void expectUnauthenticated(String token) throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    private String signedWith(String secret, String issuer, Long userId) {
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(userId.toString())
                .issuedAt(issuedAt).expiresAt(issuedAt.plus(Duration.ofMinutes(15))).claim("role", "ADMIN").build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
