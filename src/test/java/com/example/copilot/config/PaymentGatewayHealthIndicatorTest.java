package com.example.copilot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PaymentGatewayHealthIndicator.
 * Demonstrates how to test custom health indicators.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "logging.level.com.example.copilot.config.PaymentGatewayHealthIndicator=DEBUG"
})
class PaymentGatewayHealthIndicatorTest {

    @Test
    void testPaymentGatewayHealthCheck() {
        // Given
        PaymentGatewayHealthIndicator healthIndicator = new PaymentGatewayHealthIndicator();
        
        // When
        PaymentGatewayHealthIndicator.PaymentGatewayStatus status = 
            healthIndicator.checkPaymentGatewayHealth();
        
        // Then
        assertNotNull(status);
        assertNotNull(status.getStatus());
        assertNotNull(status.getMessage());
        assertNotNull(status.getLastChecked());
        assertNotNull(status.getGatewayName());
        
        // Status should be either UP or DOWN
        assertTrue(status.getStatus().equals("UP") || status.getStatus().equals("DOWN"));
        
        // Gateway name should be one of the simulated gateways
        assertTrue(java.util.Arrays.asList("Stripe", "PayPal", "Square", "Adyen")
            .contains(status.getGatewayName()));
        
        // Response time should be positive
        if (status.getResponseTime() != null) {
            assertTrue(status.getResponseTime() > 0);
        }
        
        // Verify healthy status consistency
        if (status.getStatus().equals("UP")) {
            assertTrue(status.isHealthy());
            assertNotNull(status.getTransactionCapacity());
            assertNotNull(status.getApiVersion());
        } else {
            assertFalse(status.isHealthy());
            assertNotNull(status.getErrorCode());
        }
    }
    
    @Test
    void testMultipleHealthChecks() {
        // Given
        PaymentGatewayHealthIndicator healthIndicator = new PaymentGatewayHealthIndicator();
        
        // When - perform multiple checks
        PaymentGatewayHealthIndicator.PaymentGatewayStatus status1 = 
            healthIndicator.checkPaymentGatewayHealth();
        PaymentGatewayHealthIndicator.PaymentGatewayStatus status2 = 
            healthIndicator.checkPaymentGatewayHealth();
        
        // Then - both should be valid (but may have different results due to randomness)
        assertNotNull(status1);
        assertNotNull(status2);
        
        // Timestamps should be different (assuming some time passes)
        assertNotEquals(status1.getLastChecked(), status2.getLastChecked());
    }
    
    @Test
    void testHealthStatusToString() {
        // Given
        PaymentGatewayHealthIndicator healthIndicator = new PaymentGatewayHealthIndicator();
        
        // When
        PaymentGatewayHealthIndicator.PaymentGatewayStatus status = 
            healthIndicator.checkPaymentGatewayHealth();
        
        // Then
        String statusString = status.toString();
        assertNotNull(statusString);
        assertTrue(statusString.contains("PaymentGatewayStatus"));
        assertTrue(statusString.contains(status.getGatewayName()));
        assertTrue(statusString.contains(status.getStatus()));
    }
}
