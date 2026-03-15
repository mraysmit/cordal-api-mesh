@SuppressWarnings("requires-automatic")
module dev.cordal.common {
    // Export all packages from common library
    exports dev.cordal.common.application;
    exports dev.cordal.common.cache;
    exports dev.cordal.common.config;
    exports dev.cordal.common.database;
    exports dev.cordal.common.dto;
    exports dev.cordal.common.exception;
    exports dev.cordal.common.metrics;
    exports dev.cordal.common.model;
    exports dev.cordal.common.util;

    // Core framework dependencies
    requires transitive io.javalin;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.dataformat.yaml;

    // Database dependencies
    requires com.h2database;
    requires transitive com.zaxxer.hikari;
    requires transitive java.sql;

    // Dependency injection
    requires transitive com.google.guice;
    requires jakarta.inject;

    // Configuration
    requires org.yaml.snakeyaml;

    // Logging
    requires ch.qos.logback.classic;
    requires org.slf4j;
}