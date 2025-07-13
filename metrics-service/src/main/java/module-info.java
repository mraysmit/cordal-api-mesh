module dev.mars.metrics {
    // Export only packages needed by other modules (integration-tests)
    exports dev.mars.metrics;  // Main application class

    // Export configuration and model packages for integration testing
    exports dev.mars.config;
    exports dev.mars.model;
    
    // Required modules
    requires dev.mars.common;  // Provides all core framework dependencies

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

    // Java platform modules
    requires java.sql;
    requires java.net.http;
}