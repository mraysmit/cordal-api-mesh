module dev.mars.metrics {
    // Export packages
    exports dev.mars.config;
    exports dev.mars.controller;
    exports dev.mars.database;
    exports dev.mars.dto;
    exports dev.mars.metrics;
    exports dev.mars.model;
    exports dev.mars.repository;
    exports dev.mars.service;
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
    requires java.net.http;
}