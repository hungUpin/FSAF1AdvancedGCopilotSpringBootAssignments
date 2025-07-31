package com.example.copilot.controller;

import com.example.copilot.config.MaxMemoryHealthIndicator;
import com.example.copilot.config.PaymentGatewayHealthIndicator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health controller to expose custom health checks including memory and payment gateway monitoring.
 * This supplements the Spring Boot Actuator health endpoint with custom business logic checks.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final MaxMemoryHealthIndicator memoryHealthIndicator;
    private final PaymentGatewayHealthIndicator paymentGatewayHealthIndicator;

    /**
     * Custom memory health endpoint that shows detailed memory usage information.
     * Returns DOWN status if memory usage exceeds 90% threshold.
     */
    @GetMapping("/memory")
    public ResponseEntity<Map<String, Object>> getMemoryHealth() {
        MaxMemoryHealthIndicator.MemoryHealthStatus memoryStatus = memoryHealthIndicator.checkMemoryHealth();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", memoryStatus.getStatus());
        response.put("message", memoryStatus.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("maxMemory", memoryStatus.getMaxMemory());
        details.put("totalMemory", memoryStatus.getTotalMemory());
        details.put("usedMemory", memoryStatus.getUsedMemory());
        details.put("freeMemory", memoryStatus.getFreeMemory());
        details.put("memoryUsagePercentage", memoryStatus.getMemoryUsagePercentage());
        details.put("threshold", memoryStatus.getThreshold());
        
        response.put("details", details);
        
        // Return appropriate HTTP status based on health check
        if ("DOWN".equals(memoryStatus.getStatus())) {
            return ResponseEntity.status(503).body(response); // Service Unavailable
        } else {
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Simplified health check endpoint that returns basic system information.
     */
    @GetMapping("/simple")
    public ResponseEntity<Map<String, Object>> getSimpleHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("application", "Copilot E-Commerce API");
        
        // Add JVM info
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvmInfo = new HashMap<>();
        jvmInfo.put("availableProcessors", runtime.availableProcessors());
        jvmInfo.put("maxMemory", runtime.maxMemory());
        jvmInfo.put("totalMemory", runtime.totalMemory());
        jvmInfo.put("freeMemory", runtime.freeMemory());
        
        response.put("jvm", jvmInfo);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Payment gateway health endpoint that shows detailed payment service status.
     * Returns DOWN status if payment gateway is experiencing issues.
     */
    @GetMapping("/payment-gateway")
    public ResponseEntity<Map<String, Object>> getPaymentGatewayHealth() {
        PaymentGatewayHealthIndicator.PaymentGatewayStatus gatewayStatus = 
            paymentGatewayHealthIndicator.checkPaymentGatewayHealth();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", gatewayStatus.getStatus());
        response.put("message", gatewayStatus.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("gateway", gatewayStatus.getGatewayName());
        details.put("lastChecked", gatewayStatus.getLastChecked());
        details.put("responseTime", gatewayStatus.getResponseTime() + "ms");
        
        if (gatewayStatus.isHealthy()) {
            details.put("transactionCapacity", gatewayStatus.getTransactionCapacity());
            details.put("apiVersion", gatewayStatus.getApiVersion());
            details.put("webhookStatus", gatewayStatus.getWebhookStatus());
            details.put("lastSuccessfulTransaction", gatewayStatus.getLastSuccessfulTransaction());
        } else {
            details.put("errorCode", gatewayStatus.getErrorCode());
            details.put("retryAfter", gatewayStatus.getRetryAfter());
            details.put("webhookStatus", gatewayStatus.getWebhookStatus());
        }
        
        response.put("details", details);
        
        // Return appropriate HTTP status based on health check
        if ("DOWN".equals(gatewayStatus.getStatus())) {
            return ResponseEntity.status(503).body(response); // Service Unavailable
        } else {
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Comprehensive health check endpoint that combines all health indicators.
     */
    @GetMapping("/comprehensive")
    public ResponseEntity<Map<String, Object>> getComprehensiveHealth() {
        MaxMemoryHealthIndicator.MemoryHealthStatus memoryStatus = memoryHealthIndicator.checkMemoryHealth();
        PaymentGatewayHealthIndicator.PaymentGatewayStatus gatewayStatus = 
            paymentGatewayHealthIndicator.checkPaymentGatewayHealth();
        
        Map<String, Object> response = new HashMap<>();
        
        // Overall status - DOWN if any component is DOWN
        boolean isOverallHealthy = "UP".equals(memoryStatus.getStatus()) && "UP".equals(gatewayStatus.getStatus());
        response.put("status", isOverallHealthy ? "UP" : "DOWN");
        response.put("timestamp", System.currentTimeMillis());
        
        // Individual component statuses
        Map<String, Object> components = new HashMap<>();
        
        // Memory component
        Map<String, Object> memoryComponent = new HashMap<>();
        memoryComponent.put("status", memoryStatus.getStatus());
        memoryComponent.put("details", Map.of(
            "usage", memoryStatus.getMemoryUsagePercentage(),
            "threshold", memoryStatus.getThreshold(),
            "message", memoryStatus.getMessage()
        ));
        components.put("memory", memoryComponent);
        
        // Payment gateway component
        Map<String, Object> gatewayComponent = new HashMap<>();
        gatewayComponent.put("status", gatewayStatus.getStatus());
        gatewayComponent.put("details", Map.of(
            "gateway", gatewayStatus.getGatewayName(),
            "responseTime", gatewayStatus.getResponseTime() + "ms",
            "message", gatewayStatus.getMessage()
        ));
        components.put("paymentGateway", gatewayComponent);
        
        response.put("components", components);
        
        // Return appropriate HTTP status
        return isOverallHealthy ? ResponseEntity.ok(response) : ResponseEntity.status(503).body(response);
    }
}
