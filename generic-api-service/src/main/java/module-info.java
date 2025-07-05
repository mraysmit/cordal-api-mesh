module dev.mars.generic.api {
    // Export packages
    exports dev.mars.bootstrap;
    exports dev.mars.config;
    exports dev.mars.database;
    exports dev.mars.database.loader;
    exports dev.mars.database.repository;
    exports dev.mars.generic;
    exports dev.mars.generic.config;
    exports dev.mars.generic.database;
    exports dev.mars.generic.management;
    exports dev.mars.generic.migration;
    exports dev.mars.generic.model;
    exports dev.mars.util;

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
    requires dev.mars.common;
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
    requires java.management;
    requires java.net.http;
    requires swagger.ui;
}