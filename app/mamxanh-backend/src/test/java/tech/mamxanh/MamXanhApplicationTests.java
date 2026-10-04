package tech.mamxanh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

/**
 * Starts the application through {@link MamXanhApplication#main} (see {@link AbstractIntegrationTest})
 * with Flyway V1→latest on a clean SQL Server and Hibernate {@code ddl-auto=validate}.
 */
class MamXanhApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MSSQLServerContainer sqlServer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void mainStartsApplicationContext() {
        assertTrue(context.isActive(), "The application entry point must start a working context");
    }

    /** Starting through main must not pick up the local profile, a local .env or another database. */
    @Test
    void mainUsesTheTestProfileAndOnlyTheSqlServerContainer() throws SQLException {
        assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("test");
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL())
                    .contains(sqlServer.getHost() + ":" + sqlServer.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT));
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL", Integer.class))
                .isGreaterThanOrEqualTo(4);
    }

}
