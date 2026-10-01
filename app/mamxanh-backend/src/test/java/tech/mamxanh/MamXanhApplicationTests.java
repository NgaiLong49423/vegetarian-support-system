package tech.mamxanh;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Starts the application through {@link MamXanhApplication#main} (see {@link AbstractIntegrationTest})
 * with Flyway V1→latest on a clean SQL Server and Hibernate {@code ddl-auto=validate}.
 */
class MamXanhApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void mainStartsApplicationContext() {
        assertTrue(context.isActive(), "The application entry point must start a working context");
    }

}
