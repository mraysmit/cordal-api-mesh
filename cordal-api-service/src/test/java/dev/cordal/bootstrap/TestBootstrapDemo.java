package dev.cordal.bootstrap;

import dev.cordal.bootstrap.SystemBootstrapDemo;

public class TestBootstrapDemo {
    public static void main(String[] args) {
        System.out.println("Testing SystemBootstrapDemo with validation error handling...");
        SystemBootstrapDemo demo = new SystemBootstrapDemo();
        try {
            demo.runBootstrapDemo();
        } catch (Exception e) {
            System.out.println("Demo completed with error: " + e.getMessage());
        }
    }
}
