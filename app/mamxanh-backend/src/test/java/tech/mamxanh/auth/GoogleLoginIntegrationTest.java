package tech.mamxanh.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.auth.security.GoogleIdentity;
import tech.mamxanh.auth.security.GoogleVerificationUnavailableException;

/**
 * FR-03-D (#8): UC-03.5, AC-03.10, the five account cases of Q26 and decisions Q39–Q42 against a
 * real SQL Server. Google ID Token verification is mocked ({@code googleTokenVerifier}); the
 * verifier itself is covered by {@code GoogleIdTokenVerifierAdapterTest}.
 */
@ExtendWith(OutputCaptureExtension.class)
class GoogleLoginIntegrationTest extends AbstractIntegrationTest {

    private static final String ID_TOKEN = "header.google-id-token-payload.signature";
    private static final String SUBJECT = "109876543210987654321";
    private static final String OTHER_SUBJECT = "100000000000000000001";
    private static final String EMAIL = "an.nguyen@gmail.com";
    private static final String GOOGLE_NAME = "Nguyễn Văn An";
    private static final String PICTURE = "https://lh3.googleusercontent.com/a/an-nguyen";
    private static final String PASSWORD = "MatKhau123";
    private static final String WRONG_PASSWORD = "SaiMatKhau9";

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

    // ---------------------------------------------------------------- Q26: new account, AC-03.10

    @Test
    void createsAnActiveVerifiedCustomerFromTheGoogleProfileAndReturnsAnAccessToken() throws Exception {
        googleReturns(identity(SUBJECT, "An.Nguyen@Gmail.com", true, GOOGLE_NAME, PICTURE));

        googleLogin(ID_TOKEN)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.account.email").value(EMAIL))
                .andExpect(jsonPath("$.account.displayName").value(GOOGLE_NAME))
                .andExpect(jsonPath("$.account.avatarUrl").value(PICTURE))
                .andExpect(jsonPath("$.account.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.account.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.account.emailVerified").value(true));

        Map<String, Object> row = userRow(EMAIL);
        assertThat(row.get("google_subject")).isEqualTo(SUBJECT);
        assertThat(row.get("password_hash")).isNull();
        assertThat(row.get("email_verified")).isEqualTo(true);
        assertThat(row.get("account_status")).isEqualTo("ACTIVE");
        assertThat(row.get("role")).isEqualTo("CUSTOMER");
    }

    @Test
    void theReturnedAccessTokenOpensMemberEndpoints() throws Exception {
        googleReturns(identity(SUBJECT, EMAIL, true, GOOGLE_NAME, PICTURE));
        String accessToken = accessToken(googleLogin(ID_TOKEN));

        mockMvc.perform(get("/api/v1/nutrition/dietary-preferences")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void normalizesAnUnusableGoogleNameWhenCreatingTheAccount() throws Exception {
        googleReturns(identity(SUBJECT, EMAIL, true, "  A ", null));

        googleLogin(ID_TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.displayName").value("an.nguyen"))
                .andExpect(jsonPath("$.account.avatarUrl").doesNotExist());
    }

    @Test
    void doesNotStoreAnAvatarUrlLongerThanTheColumn() throws Exception {
        googleReturns(identity(SUBJECT, EMAIL, true, GOOGLE_NAME, "https://lh3.googleusercontent.com/" + "a".repeat(2100)));

        googleLogin(ID_TOKEN).andExpect(status().isOk()).andExpect(jsonPath("$.account.avatarUrl").doesNotExist());
    }

    // ---------------------------------------------------------------- Q26: linked account

    @Test
    void logsInALinkedAccountWithoutChangingItsProfile() throws Exception {
        googleReturns(identity(SUBJECT, EMAIL, true, GOOGLE_NAME, PICTURE));
        long accountId = accountId(googleLogin(ID_TOKEN));
        googleReturns(identity(SUBJECT, EMAIL, true, "Tên Google Mới", "https://lh3.googleusercontent.com/a/new"));

        googleLogin(ID_TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.id").value(accountId))
                .andExpect(jsonPath("$.account.displayName").value(GOOGLE_NAME))
                .andExpect(jsonPath("$.account.avatarUrl").value(PICTURE));
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void findsTheLinkedAccountByGoogleSubjectBeforeEmail() throws Exception {
        googleReturns(identity(SUBJECT, EMAIL, true, GOOGLE_NAME, PICTURE));
        long accountId = accountId(googleLogin(ID_TOKEN));
        googleReturns(identity(SUBJECT, "an.new.address@gmail.com", true, GOOGLE_NAME, PICTURE));

        googleLogin(ID_TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.id").value(accountId))
                .andExpect(jsonPath("$.account.email").value(EMAIL));
        assertThat(userRepository.count()).isEqualTo(1);
    }

    // ---------------------------------------------------------------- Q26: unverified password account

    @Test
    void linksAnUnverifiedPasswordAccountVerifiesItAndRemovesItsPassword() throws Exception {
        User user = User.registerWithPassword(EMAIL, passwordEncoder.encode(PASSWORD), "Tên Đã Đăng Ký", now());
        user.assignEmailVerificationToken("a".repeat(64), now().plusHours(24), now());
        userRepository.save(user);
        googleReturns(identity(SUBJECT, EMAIL, true, GOOGLE_NAME, PICTURE));

        googleLogin(ID_TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.id").value(user.getId()))
                .andExpect(jsonPath("$.account.displayName").value("Tên Đã Đăng Ký"))
                .andExpect(jsonPath("$.account.avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.account.emailVerified").value(true));

        Map<String, Object> row = userRow(EMAIL);
        assertThat(row.get("google_subject")).isEqualTo(SUBJECT);
        assertThat(row.get("email_verified")).isEqualTo(true);
        assertThat(row.get("password_hash")).isNull();
        assertThat(row.get("email_verification_token")).isNull();
        passwordLogin(EMAIL, PASSWORD).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- Q39: verified password account

    @Test
    void linksAVerifiedPasswordAccountAndKeepsItsPassword() throws Exception {
        User user = createVerifiedPasswordUser(EMAIL);
        String passwordHash = user.getPasswordHash();
        googleReturns(identity(SUBJECT, EMAIL, true, GOOGLE_NAME, PICTURE));

        googleLogin(ID_TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.id").value(user.getId()))
                .andExpect(jsonPath("$.account.displayName").value("Nguyễn An"));

        Map<String, Object> row = userRow(EMAIL);
        assertThat(row.get("google_subject")).isEqualTo(SUBJECT);
        assertThat(row.get("password_hash")).isEqualTo(passwordHash);
        passwordLogin(EMAIL, PASSWORD).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- Q26: LOCKED

    @Test
    void rejectsALockedPasswordAccountWithoutLinkingIt() throws Exception {
        createVerifiedPasswordUser(EMAIL);
        lock(EMAIL);
        googleReturns(identity(SUBJECT, EMAIL, true, GOOGLE_NAME, PICTURE));

        expectProblem(googleLogin(ID_TOKEN), 403, "ACCOUNT_LOCKED");

        assertThat(userRow(EMAIL).get("google_subject")).isNull();
    }

    @Test
    void rejectsALockedLinkedAccount() throws Exception {
        googleReturns(identity(SUBJECT, EMAIL, true, GOOGLE_NAME, PICTURE));
        googleLogin(ID_TOKEN).andExpect(status().isOk());
        lock(EMAIL);

        expectProblem(googleLogin(ID_TOKEN), 403, "ACCOUNT_LOCKED");
    }

    // ---------------------------------------------------------------- Q26: Google ID conflict

    @Test
    void rejectsAnEmailAlreadyLinkedToAnotherGoogleAccount() throws Exception {
        googleReturns(identity(SUBJECT, EMAIL, true, GOOGLE_NAME, PICTURE));
        googleLogin(ID_TOKEN).andExpect(status().isOk());
        googleReturns(identity(OTHER_SUBJECT, EMAIL, true, "Người Khác", null));

        expectProblem(googleLogin(ID_TOKEN), 409, "GOOGLE_ACCOUNT_CONFLICT");

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRow(EMAIL).get("google_subject")).isEqualTo(SUBJECT);
    }

    // ---------------------------------------------------------------- Q40 and invalid tokens

    @Test
    void rejectsAGoogleAccountWhoseEmailIsNotVerified() throws Exception {
        createUnverifiedPasswordUser(EMAIL);
        googleReturns(identity(SUBJECT, EMAIL, false, GOOGLE_NAME, PICTURE));

        expectProblem(googleLogin(ID_TOKEN), 401, "GOOGLE_TOKEN_INVALID");

        assertThat(userRow(EMAIL).get("google_subject")).isNull();
        assertThat(userRow(EMAIL).get("email_verified")).isEqualTo(false);
    }

    @Test
    void rejectsATokenTheVerifierDoesNotAcceptWithoutCreatingAnAccount() throws Exception {
        given(googleTokenVerifier.verify(anyString())).willReturn(Optional.empty());

        expectProblem(googleLogin(ID_TOKEN), 401, "GOOGLE_TOKEN_INVALID");

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void rejectsAGoogleEmailThatIsNotAValidAccountEmail() throws Exception {
        googleReturns(identity(SUBJECT, "an@ví-dụ.vn", true, GOOGLE_NAME, PICTURE));

        expectProblem(googleLogin(ID_TOKEN), 401, "GOOGLE_TOKEN_INVALID");

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void reportsUnavailableWhenGoogleCertificatesCannotBeLoaded() throws Exception {
        given(googleTokenVerifier.verify(anyString()))
                .willThrow(new GoogleVerificationUnavailableException(new java.io.IOException("timeout")));

        expectProblem(googleLogin(ID_TOKEN), 503, "GOOGLE_LOGIN_UNAVAILABLE");
    }

    @Test
    void rejectsAMissingOrOversizedIdToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("idToken"));
        googleLogin("x".repeat(4097))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    // ---------------------------------------------------------------- Q42: temporary password block

    @Test
    void allowsGoogleLoginDuringATemporaryPasswordBlockWithoutResettingTheCounter() throws Exception {
        createVerifiedPasswordUser(EMAIL);
        for (int attempt = 1; attempt <= 5; attempt++) {
            passwordLogin(EMAIL, WRONG_PASSWORD);
        }
        Map<String, Object> blocked = userRow(EMAIL);
        googleReturns(identity(SUBJECT, EMAIL, true, GOOGLE_NAME, PICTURE));

        googleLogin(ID_TOKEN).andExpect(status().isOk());

        Map<String, Object> after = userRow(EMAIL);
        assertThat(after.get("failed_login_attempts")).isEqualTo(blocked.get("failed_login_attempts")).isEqualTo(5);
        assertThat(after.get("login_blocked_until")).isEqualTo(blocked.get("login_blocked_until")).isNotNull();
        passwordLogin(EMAIL, PASSWORD).andExpect(status().isTooManyRequests());
    }

    // ---------------------------------------------------------------- robustness

    @Test
    void parallelFirstLoginsWithTheSameGoogleAccountCreateExactlyOneAccount() throws Exception {
        googleReturns(identity(SUBJECT, EMAIL, true, GOOGLE_NAME, PICTURE));
        List<Callable<MvcResult>> attempts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            attempts.add(() -> googleLogin(ID_TOKEN).andReturn());
        }

        List<MvcResult> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(5)) {
            for (Future<MvcResult> result : pool.invokeAll(attempts)) {
                results.add(result.get());
            }
        }

        assertThat(results).allSatisfy(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void logsNeverContainTheGoogleIdTokenOrTheAccessToken(CapturedOutput output) throws Exception {
        googleReturns(identity(SUBJECT, EMAIL, true, GOOGLE_NAME, PICTURE));

        String accessToken = accessToken(googleLogin(ID_TOKEN));
        given(googleTokenVerifier.verify(anyString())).willReturn(Optional.empty());
        googleLogin(ID_TOKEN);

        assertThat(output.getAll()).doesNotContain(ID_TOKEN).doesNotContain(accessToken);
    }

    @Test
    void generatedOpenApiPublishesThePublicGoogleLoginOperation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/google'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/google'].post.security").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.GoogleLoginRequest.required[0]").value("idToken"))
                .andExpect(jsonPath("$.components.schemas.GoogleLoginRequest.properties.idToken.maxLength").value(4096));
    }

    // ---------------------------------------------------------------- helpers

    private void googleReturns(GoogleIdentity identity) {
        given(googleTokenVerifier.verify(ID_TOKEN)).willReturn(Optional.of(identity));
    }

    private static GoogleIdentity identity(String subject, String email, boolean emailVerified, String name,
            String picture) {
        return new GoogleIdentity(subject, email, emailVerified, name, picture);
    }

    private ResultActions googleLogin(String idToken) throws Exception {
        String body = "{\"idToken\":\"%s\"}".formatted(idToken);
        return mockMvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions passwordLogin(String email, String password) throws Exception {
        String body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String accessToken(ResultActions result) throws Exception {
        String json = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(json, "$.accessToken");
    }

    private static long accountId(ResultActions result) throws Exception {
        String json = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(json, "$.account.id")).longValue();
    }

    private static void expectProblem(ResultActions result, int status, String code) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    private User createVerifiedPasswordUser(String email) {
        User user = User.registerWithPassword(email, passwordEncoder.encode(PASSWORD), "Nguyễn An", now());
        user.markEmailVerified(now());
        return userRepository.save(user);
    }

    private User createUnverifiedPasswordUser(String email) {
        return userRepository.save(User.registerWithPassword(email, passwordEncoder.encode(PASSWORD), "Nguyễn An", now()));
    }

    private void lock(String email) {
        jdbcTemplate.update("UPDATE [USER] SET account_status = 'LOCKED' WHERE email = ?", email);
    }

    private Map<String, Object> userRow(String email) {
        return jdbcTemplate.queryForMap("SELECT google_subject, password_hash, email_verified, account_status, role, "
                + "email_verification_token, failed_login_attempts, login_blocked_until FROM [USER] WHERE email = ?", email);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
