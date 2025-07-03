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
    
    // Required external modules
    requires io.javalin;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.h2database;
    requires com.zaxxer.hikari;
    requires com.google.guice;
    requires javax.inject;
    requires org.yaml.snakeyaml;
    requires ch.qos.logback.classic;
    requires org.slf4j;
    requires java.sql;
    requires swagger.ui;
}