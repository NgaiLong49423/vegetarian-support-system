package tech.mamxanh;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tech.mamxanh.auth.security.GoogleTokenVerifier;
import tech.mamxanh.integration.email.EmailSender;

/**
 * Base for integration tests: full application context started through the real {@code main}
 * entry point, MockMvc, SQL Server via Testcontainers and a controllable clock. Outgoing email and
 * Google ID Token verification are mocked here (not per test class) so every integration test
 * shares one cached context and one container, and no test calls Google.
 */
@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
        properties = "mamxanh.auth.jwt-secret=integration-test-only-signing-secret-0123456789")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MutableClock clock;

    @MockitoBean
    protected EmailSender emailSender;

    @MockitoBean
    protected GoogleTokenVerifier googleTokenVerifier;

    @BeforeEach
    void resetClock() {
        clock.reset();
    }
}
