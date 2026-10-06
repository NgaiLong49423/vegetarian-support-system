package tech.mamxanh.common.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Recreates the local database from Flyway migrations for an explicitly selected profile.
 * The regular {@code local} profile never enables this behavior.
 */
@Configuration
@Profile("local-reset")
public class LocalDatabaseResetConfiguration {

    private static final Pattern SQL_SERVER_URL =
            Pattern.compile("(?i)^jdbc:sqlserver://([^;]+)(?:;(.*))?$");
    private static final Pattern LOOPBACK_SERVER =
            Pattern.compile("(?i)^(localhost|127\\.0\\.0\\.1|\\[?::1\\]?)(?::\\d+)?$");
    private static final String DATABASE_NAME = "MamXanhDB";

    @Bean
    FlywayMigrationStrategy recreateLocalDatabaseBeforeMigration() {
        return flyway -> {
            dropAndRecreateLocalDatabase(flyway.getConfiguration().getDataSource());
            flyway.migrate();
        };
    }

    private static void dropAndRecreateLocalDatabase(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            requireLocalDatabase(connection.getMetaData().getURL(), connection.getCatalog());
            connection.setCatalog("master");
            requireDatabaseCreationPermissions(connection);

            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER DATABASE [MamXanhDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                statement.execute("DROP DATABASE [MamXanhDB]");
                statement.execute("CREATE DATABASE [MamXanhDB] COLLATE SQL_Latin1_General_CP1_CI_AS");
                statement.execute("ALTER DATABASE [MamXanhDB] SET COMPATIBILITY_LEVEL = 150");
            }

            connection.setCatalog(DATABASE_NAME);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Cannot drop and recreate the verified local MamXanhDB. "
                            + "Check local SQL Server availability and database creation permissions.",
                    exception);
        }
    }

    private static void requireLocalDatabase(String jdbcUrl, String connectedDatabase) {
        Matcher matcher = SQL_SERVER_URL.matcher(jdbcUrl);
        if (!matcher.matches() || !LOOPBACK_SERVER.matcher(matcher.group(1)).matches()) {
            throw new IllegalStateException(
                    "The local-reset profile only supports a SQL Server on localhost or loopback.");
        }
        if (!DATABASE_NAME.equalsIgnoreCase(connectedDatabase)) {
            throw new IllegalStateException(
                    "The local-reset profile only supports the MamXanhDB database.");
        }

        String properties = matcher.group(2);
        String configuredDatabase = properties == null
                ? null
                : Arrays.stream(properties.split(";"))
                        .map(property -> property.split("=", 2))
                        .filter(parts -> parts.length == 2)
                        .filter(parts -> parts[0].trim().equalsIgnoreCase("databaseName"))
                        .map(parts -> parts[1].trim())
                        .findFirst()
                        .orElse(null);

        if (!DATABASE_NAME.equalsIgnoreCase(configuredDatabase)) {
            throw new IllegalStateException(
                    "The local-reset profile only supports the MamXanhDB database.");
        }
    }

    private static void requireDatabaseCreationPermissions(Connection connection) throws SQLException {
        String permissionQuery = "SELECT CASE WHEN IS_SRVROLEMEMBER('sysadmin') = 1 "
                + "OR IS_SRVROLEMEMBER('dbcreator') = 1 "
                + "OR (HAS_PERMS_BY_NAME(NULL, NULL, 'CREATE ANY DATABASE') = 1 "
                + "AND HAS_PERMS_BY_NAME(NULL, NULL, 'ALTER ANY DATABASE') = 1) "
                + "THEN 1 ELSE 0 END";
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(permissionQuery)) {
            if (!resultSet.next() || resultSet.getInt(1) != 1) {
                throw new IllegalStateException(
                        "The local-reset profile requires SQL Server permission to drop and create databases.");
            }
        }
    }
}
