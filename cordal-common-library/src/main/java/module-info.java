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
    requires io.javalin;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.dataformat.yaml;

    // Database dependencies
    requires com.h2database;
    requires com.zaxxer.hikari;
    requires java.sql;

    // Dependency injection
    requires com.google.guice;
    requires javax.inject;

    // Configuration
    requires org.yaml.snakeyaml;

    // Logging
    requires ch.qos.logback.classic;
    requires org.slf4j;
}