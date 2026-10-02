package tech.mamxanh;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real Microsoft SQL Server for integration tests (decision Q17). Flyway applies every migration
 * to a clean database when the Spring context starts; the container lives as long as the cached
 * context, so all integration test classes share one container. Requires a running Docker daemon.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final DockerImageName SQL_SERVER_IMAGE = DockerImageName.parse("mcr.microsoft.com/mssql/server:2019-latest");

    @Bean
    @ServiceConnection
    MSSQLServerContainer sqlServerContainer() {
        return new MSSQLServerContainer(SQL_SERVER_IMAGE).acceptLicense();
    }

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock();
    }
}
