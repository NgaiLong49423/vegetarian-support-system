package tech.mamxanh.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.Json;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.client.util.Clock;

/**
 * FR-03-D (#8): the production adapter around {@link GoogleIdTokenVerifier}. Google's signing
 * certificate is replaced by a self-signed one served through a mock transport, so signature,
 * expiry, issuer and audience are checked for real without calling Google.
 */
class GoogleIdTokenVerifierAdapterTest {

    private static final String CLIENT_ID = "mamxanh-test.apps.googleusercontent.com";
    private static final String KEY_ID = "test-key";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final long NOW_SECONDS = 1_790_000_000L;
    private static final JsonFactory JSON = GsonFactory.getDefaultInstance();
    private static final Clock FIXED_CLOCK = () -> TimeUnit.SECONDS.toMillis(NOW_SECONDS);

    @TempDir
    static Path tempDir;

    private static PrivateKey googleSigningKey;
    private static String googleCertificatesJson;

    @BeforeAll
    static void createGoogleSigningCertificate() throws Exception {
        Path keyStoreFile = tempDir.resolve("google-signing.p12");
        String password = UUID.randomUUID().toString();
        String keytool = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "keytool.exe" : "keytool")
                .toString();
        Process process = new ProcessBuilder(keytool, "-genkeypair", "-alias", KEY_ID, "-keyalg", "RSA",
                "-keysize", "2048", "-validity", "3650", "-dname", "CN=Google Test", "-storetype", "PKCS12",
                "-keystore", keyStoreFile.toString(), "-storepass", password, "-keypass", password)
                .redirectErrorStream(true).start();
        process.getInputStream().readAllBytes();
        assertThat(process.waitFor()).isZero();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(keyStoreFile)) {
            keyStore.load(in, password.toCharArray());
        }
        googleSigningKey = (PrivateKey) keyStore.getKey(KEY_ID, password.toCharArray());
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(KEY_ID);
        String pem = "-----BEGIN CERTIFICATE-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(certificate.getEncoded())
                + "\n-----END CERTIFICATE-----\n";
        googleCertificatesJson = JSON.toString(Map.of(KEY_ID, pem));
    }

    @Test
    void acceptsATokenSignedByGoogleForThisClientAndReturnsTheVerifiedClaims() throws Exception {
        String token = sign(payload(), googleSigningKey);

        Optional<GoogleIdentity> identity = adapter(certificates()).verify(token);

        assertThat(identity).contains(new GoogleIdentity("google-sub-123", "an.nguyen@gmail.com", true,
                "Nguyễn Văn An", "https://lh3.googleusercontent.com/a/photo"));
    }

    @Test
    void keepsAnUnverifiedEmailFlagSoTheServiceCanRejectIt() throws Exception {
        GoogleIdToken.Payload payload = payload().setEmailVerified(false);

        Optional<GoogleIdentity> identity = adapter(certificates()).verify(sign(payload, googleSigningKey));

        assertThat(identity).hasValueSatisfying(value -> assertThat(value.emailVerified()).isFalse());
    }

    @Test
    void rejectsAnExpiredToken() throws Exception {
        GoogleIdToken.Payload payload = payload()
                .setIssuedAtTimeSeconds(NOW_SECONDS - 7_200)
                .setExpirationTimeSeconds(NOW_SECONDS - 3_600);

        assertThat(adapter(certificates()).verify(sign(payload, googleSigningKey))).isEmpty();
    }

    @Test
    void rejectsATokenIssuedForAnotherClient() throws Exception {
        GoogleIdToken.Payload payload = payload().setAudience("another-app.apps.googleusercontent.com");

        assertThat(adapter(certificates()).verify(sign(payload, googleSigningKey))).isEmpty();
    }

    @Test
    void rejectsATokenFromAnotherIssuer() throws Exception {
        GoogleIdToken.Payload payload = payload().setIssuer("https://accounts.example.com");

        assertThat(adapter(certificates()).verify(sign(payload, googleSigningKey))).isEmpty();
    }

    @Test
    void rejectsATokenNotSignedByGoogle() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        PrivateKey attackerKey = generator.generateKeyPair().getPrivate();

        assertThat(adapter(certificates()).verify(sign(payload(), attackerKey))).isEmpty();
    }

    @Test
    void rejectsATokenWithoutEmail() throws Exception {
        GoogleIdToken.Payload payload = payload().setEmail(null);

        assertThat(adapter(certificates()).verify(sign(payload, googleSigningKey))).isEmpty();
    }

    @Test
    void rejectsAMalformedToken() {
        assertThat(adapter(certificates()).verify("not-a-google-id-token")).isEmpty();
        assertThat(adapter(certificates()).verify("aaa.bbb.ccc")).isEmpty();
    }

    @Test
    void reportsUnavailableWhenGoogleCertificatesCannotBeLoaded() throws Exception {
        MockHttpTransport failingTransport = new MockHttpTransport.Builder()
                .setLowLevelHttpResponse(new MockLowLevelHttpResponse().setStatusCode(503))
                .build();
        String token = sign(payload(), googleSigningKey);

        assertThatThrownBy(() -> adapter(failingTransport).verify(token))
                .isInstanceOf(GoogleVerificationUnavailableException.class);
    }

    private static GoogleIdTokenVerifierAdapter adapter(MockHttpTransport transport) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, JSON)
                .setAudience(List.of(CLIENT_ID))
                .setClock(FIXED_CLOCK)
                .build();
        return new GoogleIdTokenVerifierAdapter(verifier);
    }

    private static MockHttpTransport certificates() {
        return new MockHttpTransport.Builder()
                .setLowLevelHttpResponse(new MockLowLevelHttpResponse()
                        .setContentType(Json.MEDIA_TYPE)
                        .setContent(googleCertificatesJson))
                .build();
    }

    private static GoogleIdToken.Payload payload() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload()
                .setIssuer(GOOGLE_ISSUER)
                .setAudience(CLIENT_ID)
                .setSubject("google-sub-123")
                .setEmail("an.nguyen@gmail.com")
                .setEmailVerified(true)
                .setIssuedAtTimeSeconds(NOW_SECONDS - 60)
                .setExpirationTimeSeconds(NOW_SECONDS + 3_540);
        payload.set("name", "Nguyễn Văn An");
        payload.set("picture", "https://lh3.googleusercontent.com/a/photo");
        return payload;
    }

    private static String sign(GoogleIdToken.Payload payload, PrivateKey key) throws Exception {
        JsonWebSignature.Header header = new JsonWebSignature.Header().setAlgorithm("RS256").setKeyId(KEY_ID);
        return JsonWebSignature.signUsingRsaSha256(key, JSON, header, payload);
    }
}
