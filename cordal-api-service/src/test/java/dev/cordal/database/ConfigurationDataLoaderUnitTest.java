package dev.cordal.database;

import dev.cordal.config.GenericApiConfig;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.ConfigurationLoader;
import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.config.QueryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigurationDataLoaderUnitTest {

    private DatabaseManager databaseManager;
    private GenericApiConfig genericApiConfig;
    private ConfigurationLoader configurationLoader;
    private ConfigurationDataLoader dataLoader;

    @BeforeEach
    void setUp() {
        databaseManager = mock(DatabaseManager.class);
        genericApiConfig = mock(GenericApiConfig.class);
        configurationLoader = mock(ConfigurationLoader.class);
        dataLoader = new ConfigurationDataLoader(databaseManager, genericApiConfig, configurationLoader);
    }

    @Test
    void shouldSkipWhenConfigurationSourceIsNotDatabase() throws Exception {
        when(genericApiConfig.getConfigSource()).thenReturn("yaml");

        assertThatCode(() -> dataLoader.loadConfigurationDataIfNeeded()).doesNotThrowAnyException();

        verify(genericApiConfig, never()).isLoadConfigFromYaml();
        verify(databaseManager, never()).getConnection();
    }

    @Test
    void shouldSkipWhenYamlLoadingIsDisabled() throws Exception {
        when(genericApiConfig.getConfigSource()).thenReturn("database");
        when(genericApiConfig.isLoadConfigFromYaml()).thenReturn(false);

        assertThatCode(() -> dataLoader.loadConfigurationDataIfNeeded()).doesNotThrowAnyException();

        verify(databaseManager, never()).getConnection();
    }

    @Test
    void shouldSkipWhenConfigurationDataAlreadyExists() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(genericApiConfig.getConfigSource()).thenReturn("database");
        when(genericApiConfig.isLoadConfigFromYaml()).thenReturn(true);
        when(databaseManager.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(countStatement);
        when(countStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(2);

        dataLoader.loadConfigurationDataIfNeeded();

        verify(configurationLoader, never()).loadDatabaseConfigurations();
        verify(configurationLoader, never()).loadQueryConfigurations();
        verify(configurationLoader, never()).loadEndpointConfigurations();
        verify(connection, never()).commit();
    }

    @Test
    void shouldLoadConfigurationDataFromYamlIntoDatabase() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = mock(PreparedStatement.class);
        PreparedStatement databaseStatement = mock(PreparedStatement.class);
        PreparedStatement queryStatement = mock(PreparedStatement.class);
        PreparedStatement endpointStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        DatabaseConfig withPool = new DatabaseConfig();
        withPool.setDescription("Primary database");
        withPool.setUrl("jdbc:h2:mem:primary");
        withPool.setUsername("sa");
        withPool.setPassword("secret");
        withPool.setDriver("org.h2.Driver");
        DatabaseConfig.PoolConfig poolConfig = new DatabaseConfig.PoolConfig();
        poolConfig.setMaximumPoolSize(20);
        poolConfig.setMinimumIdle(5);
        poolConfig.setConnectionTimeout(1000);
        poolConfig.setIdleTimeout(2000);
        poolConfig.setMaxLifetime(3000);
        poolConfig.setLeakDetectionThreshold(4000);
        poolConfig.setConnectionTestQuery("SELECT 42");
        withPool.setPool(poolConfig);

        DatabaseConfig withoutPool = new DatabaseConfig();
        withoutPool.setDescription("Reporting database");
        withoutPool.setUrl("jdbc:h2:mem:reporting");
        withoutPool.setUsername("report");
        withoutPool.setPassword("pwd");
        withoutPool.setDriver("org.h2.Driver");

        QueryConfig query = new QueryConfig();
        query.setDescription("Orders query");
        query.setDatabase("primary-db");
        query.setSql("SELECT * FROM orders");

        ApiEndpointConfig endpoint = new ApiEndpointConfig();
        endpoint.setDescription("Orders endpoint");
        endpoint.setPath("/api/orders");
        endpoint.setMethod("GET");
        endpoint.setQuery("orders-query");

        when(genericApiConfig.getConfigSource()).thenReturn("database");
        when(genericApiConfig.isLoadConfigFromYaml()).thenReturn(true);
        when(databaseManager.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(countStatement, databaseStatement, queryStatement, endpointStatement);
        when(countStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);
        when(configurationLoader.loadDatabaseConfigurations()).thenReturn(Map.of("primary-db", withPool, "reporting-db", withoutPool));
        when(configurationLoader.loadQueryConfigurations()).thenReturn(Map.of("orders-query", query));
        when(configurationLoader.loadEndpointConfigurations()).thenReturn(Map.of("orders-endpoint", endpoint));

        dataLoader.loadConfigurationDataIfNeeded();

        verify(connection).setAutoCommit(false);
        verify(databaseStatement, times(2)).executeUpdate();
        verify(queryStatement, times(1)).executeUpdate();
        verify(endpointStatement, times(1)).executeUpdate();
        verify(connection).commit();
    }

    @Test
    void shouldWrapLoadingFailuresInRuntimeException() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(genericApiConfig.getConfigSource()).thenReturn("database");
        when(genericApiConfig.isLoadConfigFromYaml()).thenReturn(true);
        when(databaseManager.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(countStatement);
        when(countStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);
        when(configurationLoader.loadDatabaseConfigurations()).thenThrow(new RuntimeException("yaml parse failed"));

        assertThatThrownBy(() -> dataLoader.loadConfigurationDataIfNeeded())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to load configuration data from YAML files")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldReturnFalseWhenCountQueryReturnsNoRows() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement countStatement = mock(PreparedStatement.class);
        PreparedStatement databaseStatement = mock(PreparedStatement.class);
        PreparedStatement queryStatement = mock(PreparedStatement.class);
        PreparedStatement endpointStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(genericApiConfig.getConfigSource()).thenReturn("database");
        when(genericApiConfig.isLoadConfigFromYaml()).thenReturn(true);
        when(databaseManager.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(countStatement, databaseStatement, queryStatement, endpointStatement);
        when(countStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        when(configurationLoader.loadDatabaseConfigurations()).thenReturn(Map.of());
        when(configurationLoader.loadQueryConfigurations()).thenReturn(Map.of());
        when(configurationLoader.loadEndpointConfigurations()).thenReturn(Map.of());

        dataLoader.loadConfigurationDataIfNeeded();

        verify(connection).commit();
        assertThat(true).isTrue();
    }
}