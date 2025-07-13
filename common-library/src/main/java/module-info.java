module dev.mars.common {
    // Export all packages from common library
    exports dev.mars.common.application;
    exports dev.mars.common.config;
    exports dev.mars.common.database;
    exports dev.mars.common.dto;
    exports dev.mars.common.exception;
    exports dev.mars.common.metrics;
    exports dev.mars.common.model;
    exports dev.mars.common.util;

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