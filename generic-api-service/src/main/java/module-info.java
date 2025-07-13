module dev.mars.generic.api {
    // Export only packages needed by other modules (integration-tests)
    exports dev.mars.generic;  // Main application class
    exports dev.mars.bootstrap;  // Bootstrap demo functionality

    // Export configuration and model packages for integration testing
    exports dev.mars.generic.config;
    exports dev.mars.generic.model;

    // Open packages to Guice for reflection and dependency injection
    opens dev.mars.config to com.google.guice;
    opens dev.mars.database to com.google.guice;
    opens dev.mars.database.loader to com.google.guice;
    opens dev.mars.database.repository to com.google.guice;
    opens dev.mars.generic to com.google.guice;
    opens dev.mars.generic.config to com.google.guice;
    opens dev.mars.generic.database to com.google.guice;
    opens dev.mars.generic.management to com.google.guice;
    opens dev.mars.generic.migration to com.google.guice;
    
    // Required modules
    requires dev.mars.common;  // Provides core framework dependencies

    // Transitive dependencies from common-library (needed for direct usage)
    requires transitive io.javalin;
    requires transitive com.fasterxml.jackson.databind;
    requires transitive com.fasterxml.jackson.datatype.jsr310;
    requires transitive com.fasterxml.jackson.dataformat.yaml;
    requires transitive com.h2database;
    requires transitive com.zaxxer.hikari;
    requires transitive com.google.guice;
    requires transitive javax.inject;
    requires transitive org.yaml.snakeyaml;
    requires transitive ch.qos.logback.classic;
    requires transitive org.slf4j;

    // Additional database support
    requires java.naming;  // Required for PostgreSQL JNDI support

    // API documentation
    requires swagger.ui;

    // Java platform modules
    requires java.sql;
    requires java.management;
    requires java.net.http;
}