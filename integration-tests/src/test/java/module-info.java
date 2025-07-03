module dev.mars.integration.tests {
    // This is a test module, so it doesn't export any packages

    // Required modules - only require common library to avoid package conflicts
    requires dev.mars.common;
    // Note: We don't directly require dev.mars.generic.api and dev.mars.metrics
    // to avoid package split issues with dev.mars.config

    // External dependencies
    requires okhttp3;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.junit.jupiter.api;
    requires org.assertj.core;
    requires java.sql;
    requires java.net.http;
    requires io.javalin;
    requires com.google.guice;
    requires javax.inject;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    // Awaitility is not modularized, so we rely on the automatic module system

    // Open packages for testing
    opens dev.mars.integration to org.junit.platform.commons;
}
