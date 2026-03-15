@SuppressWarnings("requires-automatic")
module dev.cordal.generic.api {
    // Export only packages needed by other modules (integration-tests)
    exports dev.cordal.generic;  // Main application class
    exports dev.cordal.bootstrap;  // Bootstrap demo functionality

    // Export configuration and model packages for integration testing
    exports dev.cordal.config;
    exports dev.cordal.database;
    exports dev.cordal.database.loader;
    exports dev.cordal.database.repository;
    exports dev.cordal.dto;
    exports dev.cordal.generic.cache;
    exports dev.cordal.generic.config;
    exports dev.cordal.generic.database;
    exports dev.cordal.generic.dto;
    exports dev.cordal.generic.management;
    exports dev.cordal.generic.model;

    // Open packages to Guice for reflection and dependency injection
    opens dev.cordal.api;
    opens dev.cordal.config;
    opens dev.cordal.cache;
    opens dev.cordal.database;
    opens dev.cordal.database.loader to com.google.guice;
    opens dev.cordal.database.repository to com.google.guice;
    opens dev.cordal.dto;
    opens dev.cordal.generic;
    opens dev.cordal.generic.config;
    opens dev.cordal.generic.database to com.google.guice;
    opens dev.cordal.generic.dto;
    opens dev.cordal.generic.management;
    opens dev.cordal.generic.migration;
    opens dev.cordal.generic.model;
    opens dev.cordal.hotreload;
    opens dev.cordal.util;
    
    // Required modules
    requires transitive dev.cordal.common;  // Provides core framework dependencies

    // Transitive dependencies from common-library (needed for direct usage)
    requires transitive io.javalin;
    requires transitive com.fasterxml.jackson.databind;
    requires transitive com.fasterxml.jackson.datatype.jsr310;
    requires transitive com.fasterxml.jackson.dataformat.yaml;
    requires transitive com.h2database;
    requires transitive com.zaxxer.hikari;
    requires transitive com.google.guice;
    requires transitive jakarta.inject;
    requires transitive org.yaml.snakeyaml;
    requires transitive ch.qos.logback.classic;
    requires transitive org.slf4j;

    // Additional database support
    requires java.naming;  // Required for PostgreSQL JNDI support

    // Java platform modules
    requires transitive java.sql;
    requires java.management;
    requires java.net.http;
}