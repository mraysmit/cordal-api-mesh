package dev.mars.common.database;

import com.zaxxer.hikari.HikariDataSource;
import dev.mars.common.config.DatabaseConfig;
import dev.mars.common.config.PoolConfig;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DatabaseConnectionFactory
 */
class DatabaseConnectionFactoryTest {

    @Test
    void shouldCreateDataSourceWithFullConfig() {
        PoolConfig poolConfig = new PoolConfig();
        poolConfig.setMaximumPoolSize(8);
        poolConfig.setMinimumIdle(2);
        poolConfig.setConnectionTimeout(25000);

        DatabaseConfig config = new DatabaseConfig();
        config.setName("test-factory-db");
        config.setDescription("Test factory database");
        config.setUrl("jdbc:h2:mem:factorytest;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriver("org.h2.Driver");
        config.setPool(poolConfig);

        HikariDataSource dataSource = DatabaseConnectionFactory.createDataSource(config);

        assertThat(dataSource).isNotNull();
        assertThat(dataSource.getJdbcUrl()).isEqualTo(config.getUrl());
        assertThat(dataSource.getUsername()).isEqualTo(config.getUsername());
        assertThat(dataSource.getDriverClassName()).isEqualTo(config.getDriver());
        assertThat(dataSource.getMaximumPoolSize()).isEqualTo(8);
        assertThat(dataSource.getMinimumIdle()).isEqualTo(2);
        assertThat(dataSource.getConnectionTimeout()).isEqualTo(25000);
        assertThat(dataSource.getPoolName()).isEqualTo("test-factory-db-pool");

        dataSource.close();
    }

    @Test
    void shouldCreateDataSourceWithDefaultPoolConfig() {
        DatabaseConfig config = new DatabaseConfig();
        config.setName("test-default-db");
        config.setUrl("jdbc:h2:mem:defaulttest;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriver("org.h2.Driver");
        // No pool config - should use defaults

        HikariDataSource dataSource = DatabaseConnectionFactory.createDataSource(config);

        assertThat(dataSource).isNotNull();
        assertThat(dataSource.getMaximumPoolSize()).isEqualTo(10); // Default
        assertThat(dataSource.getMinimumIdle()).isEqualTo(2); // Default
        assertThat(dataSource.getConnectionTimeout()).isEqualTo(30000); // Default

        dataSource.close();
    }

    @Test
    void shouldCreateDataSourceWithSimpleMethod() {
        HikariDataSource dataSource = DatabaseConnectionFactory.createDataSource(
            "jdbc:h2:mem:simpletest;DB_CLOSE_DELAY=-1",
            "sa",
            "",
            "org.h2.Driver"
        );

        assertThat(dataSource).isNotNull();
        assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:h2:mem:simpletest;DB_CLOSE_DELAY=-1");
        assertThat(dataSource.getUsername()).isEqualTo("sa");
        assertThat(dataSource.getDriverClassName()).isEqualTo("org.h2.Driver");
        assertThat(dataSource.getPoolName()).isEqualTo("default-pool");

        dataSource.close();
    }

    @Test
    void shouldCreateWorkingConnection() throws SQLException {
        DatabaseConfig config = new DatabaseConfig();
        config.setName("test-connection-db");
        config.setUrl("jdbc:h2:mem:connectiontest;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriver("org.h2.Driver");

        HikariDataSource dataSource = DatabaseConnectionFactory.createDataSource(config);

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isClosed()).isFalse();

            // Test that connection actually works
            try (var statement = connection.createStatement();
                 var resultSet = statement.executeQuery("SELECT 1")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1)).isEqualTo(1);
            }
        }

        dataSource.close();
    }
}
