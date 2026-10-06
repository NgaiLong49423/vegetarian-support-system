package tech.mamxanh.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalDatabaseResetConfigurationTest {

    @Test
    @DisplayName("requireLocalDatabase validates loopback and MamXanhDB correctly")
    void testRequireLocalDatabase() {
        // Valid localhost
        ReflectionTestUtils.invokeMethod(
                LocalDatabaseResetConfiguration.class,
                "requireLocalDatabase",
                "jdbc:sqlserver://localhost:1433;databaseName=MamXanhDB;encrypt=true",
                "MamXanhDB"
        );

        // Valid 127.0.0.1
        ReflectionTestUtils.invokeMethod(
                LocalDatabaseResetConfiguration.class,
                "requireLocalDatabase",
                "jdbc:sqlserver://127.0.0.1:1433;databaseName=MamXanhDB",
                "MamXanhDB"
        );

        // Invalid remote host
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                LocalDatabaseResetConfiguration.class,
                "requireLocalDatabase",
                "jdbc:sqlserver://prod.database.windows.net:1433;databaseName=MamXanhDB",
                "MamXanhDB"
        )).isInstanceOf(IllegalStateException.class);

        // Invalid connected database name
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                LocalDatabaseResetConfiguration.class,
                "requireLocalDatabase",
                "jdbc:sqlserver://localhost:1433;databaseName=MamXanhDB",
                "OtherDB"
        )).isInstanceOf(IllegalStateException.class);

        // Invalid configured database name in properties
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                LocalDatabaseResetConfiguration.class,
                "requireLocalDatabase",
                "jdbc:sqlserver://localhost:1433;databaseName=OtherDB",
                "MamXanhDB"
        )).isInstanceOf(IllegalStateException.class);

        // Missing properties in URL
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                LocalDatabaseResetConfiguration.class,
                "requireLocalDatabase",
                "jdbc:sqlserver://localhost:1433",
                "MamXanhDB"
        )).isInstanceOf(IllegalStateException.class);

        // Invalid regex URL
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                LocalDatabaseResetConfiguration.class,
                "requireLocalDatabase",
                "invalid-url",
                "MamXanhDB"
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("requireDatabaseCreationPermissions checks permission query output")
    void testRequireDatabaseCreationPermissions() throws SQLException {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);

        // Success case: returns 1
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);
        ReflectionTestUtils.invokeMethod(
                LocalDatabaseResetConfiguration.class,
                "requireDatabaseCreationPermissions",
                connection
        );

        // Failure case 1: returns 0
        when(resultSet.getInt(1)).thenReturn(0);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                LocalDatabaseResetConfiguration.class,
                "requireDatabaseCreationPermissions",
                connection
        )).isInstanceOf(IllegalStateException.class);

        // Failure case 2: empty resultSet
        when(resultSet.next()).thenReturn(false);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                LocalDatabaseResetConfiguration.class,
                "requireDatabaseCreationPermissions",
                connection
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("recreateLocalDatabaseBeforeMigration bean creates strategy")
    void testRecreateLocalDatabaseBeforeMigrationBean() {
        LocalDatabaseResetConfiguration config = new LocalDatabaseResetConfiguration();
        assertThat(config.recreateLocalDatabaseBeforeMigration()).isNotNull();
    }
}
