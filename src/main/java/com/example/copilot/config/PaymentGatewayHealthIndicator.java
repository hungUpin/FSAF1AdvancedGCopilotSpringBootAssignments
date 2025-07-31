package com.example.copilot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Custom Health Indicator that monitors Payment Gateway status.
 * 
 * In a real application, this would:
 * - Make HTTP calls to payment provider's health/status endpoints (e.g., Stripe, PayPal, Square)
 * - Check API key validity and rate limits
 * - Verify connectivity to payment services
 * - Monitor transaction processing capabilities
 * - Check webhook endpoint availability
 * 
 * For simulation purposes, this randomly reports UP/DOWN status.
 * 
 * Note: This is a simplified version without Spring Boot Actuator dependencies
 * for compilation compatibility. To fully enable this with actuator, ensure 
 * spring-boot-starter-actuator is properly included in your pom.xml dependencies.
 */
@Component
@Slf4j
public class PaymentGatewayHealthIndicator {

    private static final String[] SIMULATED_GATEWAYS = {"Stripe", "PayPal", "Square", "Adyen"};
    private static final Random random = new Random();
    
    // Simulate some stability - 80% chance of UP status
    private static final double UP_PROBABILITY = 0.8;

    /**
     * Manual health check method - can be called programmatically.
     * In a full Spring Boot Actuator setup, this would implement HealthIndicator interface.
     */
    public PaymentGatewayStatus checkPaymentGatewayHealth() {
        try {
            log.debug("Checking payment gateway health status...");
            PaymentGatewayStatus status = performHealthCheck();
            
            if (status.isHealthy()) {
                log.info("Payment gateway health check passed: {}", status.getMessage());
            } else {
                log.warn("Payment gateway health check failed: {} (Error: {})", 
                    status.getMessage(), status.getErrorCode());
            }
            
            return status;
            
        } catch (Exception e) {
            log.error("Error checking payment gateway health", e);
            return createErrorStatus("Failed to check payment gateway status: " + e.getMessage());
        }
    }

    /**
     * Simulates checking payment gateway status.
     * In a real implementation, this would make HTTP calls to actual payment providers.
     * 
     * Example real implementation would:
     * - RestTemplate or WebClient call to https://status.stripe.com/api/v2/status.json
     * - Parse JSON response and check operational status
     * - Measure response time and validate API credentials
     */
    private PaymentGatewayStatus performHealthCheck() {
        // Simulate network delay
        simulateNetworkDelay();
        
        boolean isHealthy = random.nextDouble() < UP_PROBABILITY;
        String gatewayName = SIMULATED_GATEWAYS[random.nextInt(SIMULATED_GATEWAYS.length)];
        
        PaymentGatewayStatus status = new PaymentGatewayStatus();
        status.setGatewayName(gatewayName);
        status.setLastChecked(getCurrentTimestamp());
        status.setHealthy(isHealthy);
        
        if (isHealthy) {
            populateHealthyStatus(status);
        } else {
            populateUnhealthyStatus(status);
        }
        
        return status;
    }

    /**
     * Populates status object for healthy gateway state.
     */
    private void populateHealthyStatus(PaymentGatewayStatus status) {
        status.setStatus("UP");
        status.setMessage("Payment gateway is operational and processing transactions");
        status.setResponseTime(ThreadLocalRandom.current().nextInt(50, 200));
        status.setTransactionCapacity("Normal");
        status.setApiVersion("v2.1");
        status.setWebhookStatus("Active");
        status.setLastSuccessfulTransaction(getCurrentTimestamp());
    }

    /**
     * Populates status object for unhealthy gateway state.
     */
    private void populateUnhealthyStatus(PaymentGatewayStatus status) {
        String[] errorCodes = {"GATEWAY_TIMEOUT", "SERVICE_UNAVAILABLE", "API_RATE_LIMIT_EXCEEDED", "AUTHENTICATION_FAILED"};
        String[] errorMessages = {
            "Payment gateway is experiencing high latency",
            "Payment service temporarily unavailable - maintenance in progress",
            "API rate limit exceeded - too many requests per minute",
            "Authentication failed - invalid API credentials or expired tokens"
        };
        
        int errorIndex = random.nextInt(errorCodes.length);
        status.setStatus("DOWN");
        status.setMessage(errorMessages[errorIndex]);
        status.setErrorCode(errorCodes[errorIndex]);
        status.setRetryAfter("5 minutes");
        status.setWebhookStatus("Disconnected");
        status.setResponseTime(ThreadLocalRandom.current().nextInt(1000, 5000)); // Higher response time when down
    }

    /**
     * Creates an error status for exception scenarios.
     */
    private PaymentGatewayStatus createErrorStatus(String errorMessage) {
        PaymentGatewayStatus status = new PaymentGatewayStatus();
        status.setStatus("DOWN");
        status.setHealthy(false);
        status.setMessage(errorMessage);
        status.setLastChecked(getCurrentTimestamp());
        status.setErrorCode("HEALTH_CHECK_FAILED");
        return status;
    }

    /**
     * Simulates network delay for realistic health check behavior.
     */
    private void simulateNetworkDelay() {
        try {
            // Simulate 10-100ms network delay
            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Health check interrupted");
        }
    }

    /**
     * Gets current timestamp in a formatted string.
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Data class to hold payment gateway status information.
     */
    public static class PaymentGatewayStatus {
        private String status;
        private String gatewayName;
        private boolean healthy;
        private String message;
        private String lastChecked;
        private Integer responseTime;
        private String transactionCapacity;
        private String apiVersion;
        private String errorCode;
        private String retryAfter;
        private String webhookStatus;
        private String lastSuccessfulTransaction;

        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getGatewayName() { return gatewayName; }
        public void setGatewayName(String gatewayName) { this.gatewayName = gatewayName; }

        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getLastChecked() { return lastChecked; }
        public void setLastChecked(String lastChecked) { this.lastChecked = lastChecked; }

        public Integer getResponseTime() { return responseTime; }
        public void setResponseTime(Integer responseTime) { this.responseTime = responseTime; }

        public String getTransactionCapacity() { return transactionCapacity; }
        public void setTransactionCapacity(String transactionCapacity) { this.transactionCapacity = transactionCapacity; }

        public String getApiVersion() { return apiVersion; }
        public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

        public String getRetryAfter() { return retryAfter; }
        public void setRetryAfter(String retryAfter) { this.retryAfter = retryAfter; }

        public String getWebhookStatus() { return webhookStatus; }
        public void setWebhookStatus(String webhookStatus) { this.webhookStatus = webhookStatus; }

        public String getLastSuccessfulTransaction() { return lastSuccessfulTransaction; }
        public void setLastSuccessfulTransaction(String lastSuccessfulTransaction) { this.lastSuccessfulTransaction = lastSuccessfulTransaction; }

        @Override
        public String toString() {
            return String.format("PaymentGatewayStatus{gateway='%s', status='%s', healthy=%s, message='%s', responseTime=%dms}", 
                gatewayName, status, healthy, message, responseTime);
        }
    }
}
