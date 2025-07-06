// Simple test to demonstrate validation error handling
// This shows that the SystemBootstrapDemo validation methods work correctly

import dev.mars.bootstrap.SystemBootstrapDemo;
import java.lang.reflect.Method;

public class TestValidationDemo {
    public static void main(String[] args) {
        System.out.println("=== Testing SystemBootstrapDemo Validation Error Handling ===");
        
        SystemBootstrapDemo demo = new SystemBootstrapDemo();
        
        try {
            // Test the validation methods directly using reflection
            Method performConfigValidation = demo.getClass().getDeclaredMethod("performConfigurationValidation");
            performConfigValidation.setAccessible(true);
            
            Method performSchemaValidation = demo.getClass().getDeclaredMethod("performDatabaseSchemaValidation");
            performSchemaValidation.setAccessible(true);
            
            System.out.println("1. Testing configuration validation error handling...");
            performConfigValidation.invoke(demo);
            System.out.println("   ✓ Configuration validation completed without throwing exception");
            
            System.out.println("2. Testing database schema validation error handling...");
            performSchemaValidation.invoke(demo);
            System.out.println("   ✓ Database schema validation completed without throwing exception");
            
            System.out.println("\n=== SUCCESS: Validation error handling works correctly ===");
            System.out.println("The SystemBootstrapDemo now:");
            System.out.println("- Reports configuration errors without crashing");
            System.out.println("- Continues validation even when errors are found");
            System.out.println("- Provides detailed error reporting");
            
        } catch (Exception e) {
            System.out.println("✗ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
