module dev.mars.generic.api {
    // Export packages
    exports dev.mars.bootstrap;
    exports dev.mars.config;
    exports dev.mars.generic;
    exports dev.mars.generic.config;
    exports dev.mars.generic.database;
    exports dev.mars.generic.management;
    exports dev.mars.generic.model;
    exports dev.mars.util;
    
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